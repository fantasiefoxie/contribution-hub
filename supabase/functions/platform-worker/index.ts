import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type, x-supabase-client-platform, x-supabase-client-platform-version, x-supabase-client-runtime, x-supabase-client-runtime-version",
};

function json(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

function getServiceDb() {
  return createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  );
}

type Db = ReturnType<typeof createClient>;

// ────────────────────────────────────────────────────────────
// 1. EXPIRE STALE RESERVATIONS & LOCKS
// ────────────────────────────────────────────────────────────
async function expireReservations(db: Db) {
  const now = new Date().toISOString();

  const { count: reservations } = await db
    .from("tasks")
    .update({ status: "open", reserved_by: null, reserved_until: null })
    .eq("status", "reserved")
    .lt("reserved_until", now)
    .select("*", { count: "exact", head: true });

  const { count: locks } = await db
    .from("tasks")
    .update({ status: "open", locked_by: null, locked_until: null })
    .eq("status", "claimed")
    .lt("locked_until", now)
    .select("*", { count: "exact", head: true });

  return { expired_reservations: reservations ?? 0, expired_locks: locks ?? 0 };
}

// ────────────────────────────────────────────────────────────
// 2. DETECT INITIATIVE MILESTONES
// ────────────────────────────────────────────────────────────
async function detectMilestones(db: Db) {
  const { data: initiatives } = await db
    .from("initiatives")
    .select("id, target_metrics")
    .eq("status", "active");

  if (!initiatives || initiatives.length === 0) return { milestones_detected: 0 };

  let detected = 0;
  const milestoneThresholds = [25, 50, 75, 100];

  for (const initiative of initiatives) {
    const { count: totalTasks } = await db
      .from("tasks")
      .select("*", { count: "exact", head: true })
      .eq("initiative_id", initiative.id);

    const { count: completedTasks } = await db
      .from("tasks")
      .select("*", { count: "exact", head: true })
      .eq("initiative_id", initiative.id)
      .eq("status", "validated");

    if (!totalTasks || totalTasks === 0) continue;
    const pct = Math.round(((completedTasks ?? 0) / totalTasks) * 100);

    const { data: existingMilestones } = await db
      .from("initiative_milestones")
      .select("percentage")
      .eq("initiative_id", initiative.id);

    const recorded = new Set(
      (existingMilestones ?? []).map((m: { percentage: number }) => m.percentage),
    );

    for (const threshold of milestoneThresholds) {
      if (pct >= threshold && !recorded.has(threshold)) {
        await db
          .from("initiative_milestones")
          .insert({ initiative_id: initiative.id, percentage: threshold });

        const { data: participants } = await db
          .from("initiative_participants")
          .select("user_id")
          .eq("initiative_id", initiative.id);

        if (participants && participants.length > 0) {
          await db.from("notifications").insert(
            participants.map((p: { user_id: string }) => ({
              user_id: p.user_id,
              type: "milestone",
              title: `Initiative reached ${threshold}%!`,
              body: `An initiative you joined has reached ${threshold}% completion.`,
              data: { initiative_id: initiative.id, percentage: threshold },
            })),
          );
        }
        detected++;
      }
    }

    if (pct >= 100) {
      await db
        .from("initiatives")
        .update({ status: "completed" })
        .eq("id", initiative.id)
        .eq("status", "active");
    }
  }

  return { milestones_detected: detected };
}

// ────────────────────────────────────────────────────────────
// 3. CLEANUP: IDEMPOTENCY KEYS & RATE LIMITS
// ────────────────────────────────────────────────────────────
async function cleanupExpiredData(db: Db) {
  const now = new Date().toISOString();
  const oneHourAgo = new Date(Date.now() - 3600000).toISOString();

  const [idempotency, rateLimits] = await Promise.all([
    db
      .from("idempotency_keys")
      .delete()
      .lt("expires_at", now)
      .select("*", { count: "exact", head: true }),
    db
      .from("rate_limit_entries")
      .delete()
      .lt("window_start", oneHourAgo)
      .select("*", { count: "exact", head: true }),
  ]);

  return {
    cleaned_keys: idempotency.count ?? 0,
    cleaned_rate_limits: rateLimits.count ?? 0,
  };
}

// ────────────────────────────────────────────────────────────
// 4. RETRY FAILED WEBHOOKS
// ────────────────────────────────────────────────────────────
async function retryFailedWebhooks(db: Db) {
  const { data: subs } = await db
    .from("webhook_subscriptions")
    .select("id, url, failure_count")
    .eq("is_active", true)
    .gt("failure_count", 0)
    .lte("failure_count", 10);

  if (!subs || subs.length === 0) return { webhook_retries: 0 };

  let retried = 0;
  for (const sub of subs) {
    try {
      const res = await fetch(sub.url, {
        method: "POST",
        headers: { "Content-Type": "application/json", "X-Event-Type": "ping" },
        body: JSON.stringify({ event: "ping", timestamp: new Date().toISOString() }),
      });

      if (res.ok) {
        await db.from("webhook_subscriptions").update({ failure_count: 0 }).eq("id", sub.id);
        retried++;
      } else if (sub.failure_count >= 10) {
        await db.from("webhook_subscriptions").update({ is_active: false }).eq("id", sub.id);
      }
    } catch {
      // Leave as-is for next retry cycle
    }
  }

  return { webhook_retries: retried };
}

// ────────────────────────────────────────────────────────────
// 5. EVENT QUEUE PROCESSOR → SUMMARY TABLE UPDATER
// ────────────────────────────────────────────────────────────

interface QueueItem {
  id: string;
  event_id: string;
  attempts: number;
  max_attempts: number;
}

interface EventRow {
  id: string;
  type: string;
  entity_id: string;
  entity_type: string;
  actor_id: string | null;
  payload: Record<string, unknown> | null;
  created_at: string;
}

async function processEventQueue(db: Db) {
  // Pull pending items (batch of 50)
  const { data: items } = await db
    .from("event_queue")
    .select("id, event_id, attempts, max_attempts")
    .eq("status", "pending")
    .lte("next_retry_at", new Date().toISOString())
    .order("created_at", { ascending: true })
    .limit(50);

  if (!items || items.length === 0) return { events_processed: 0, events_failed: 0 };

  let processed = 0;
  let failed = 0;

  for (const item of items as QueueItem[]) {
    try {
      // Fetch the event
      const { data: event } = await db
        .from("events")
        .select("*")
        .eq("id", item.event_id)
        .single();

      if (!event) {
        // Event deleted or missing — mark as failed permanently
        await db
          .from("event_queue")
          .update({ status: "failed", last_error: "Event not found", processed_at: new Date().toISOString() })
          .eq("id", item.id);
        failed++;
        continue;
      }

      await handleEvent(db, event as EventRow);

      // Mark as processed
      await db
        .from("event_queue")
        .update({ status: "processed", processed_at: new Date().toISOString() })
        .eq("id", item.id);
      processed++;
    } catch (err) {
      const nextAttempt = item.attempts + 1;
      const backoffMs = Math.min(nextAttempt * 30000, 300000); // 30s increments, max 5min

      if (nextAttempt >= item.max_attempts) {
        await db
          .from("event_queue")
          .update({
            status: "failed",
            attempts: nextAttempt,
            last_error: String(err),
            processed_at: new Date().toISOString(),
          })
          .eq("id", item.id);
        failed++;
      } else {
        await db
          .from("event_queue")
          .update({
            attempts: nextAttempt,
            last_error: String(err),
            next_retry_at: new Date(Date.now() + backoffMs).toISOString(),
          })
          .eq("id", item.id);
        failed++;
      }
    }
  }

  return { events_processed: processed, events_failed: failed };
}

// ── Event handler: dispatch to summary updaters ──
async function handleEvent(db: Db, event: EventRow) {
  switch (event.type) {
    case "TaskCreated":
      await updateInitiativeProgressForTask(db, event.entity_id);
      await updateOrgMetricsForTask(db, event.entity_id, "created");
      break;

    case "TaskReserved":
    case "TaskClaimed":
      if (event.actor_id) {
        await upsertContributorStat(db, event.actor_id, { tasks_claimed_delta: 1 });
      }
      break;

    case "TaskSubmitted":
      break;

    case "ValidationApproved":
      await updateInitiativeProgressForTask(db, event.entity_id);
      await updateOrgMetricsForTask(db, event.entity_id, "completed");
      break;

    case "ValidationRejected":
      break;

    case "ContributionRecorded":
      if (event.actor_id) {
        const score = (event.payload as Record<string, unknown>)?.score;
        await upsertContributorStat(db, event.actor_id, {
          tasks_completed_delta: 1,
          score_delta: typeof score === "number" ? score : 0,
        });
      }
      break;

    case "InitiativeJoined":
      if (event.actor_id) {
        await upsertContributorStat(db, event.actor_id, { initiatives_joined_delta: 1 });
      }
      await refreshInitiativeProgress(db, event.entity_id);
      break;

    case "InitiativeCreated":
      await refreshInitiativeProgress(db, event.entity_id);
      break;

    default:
      // Unknown event type — skip silently
      break;
  }
}

// ── Summary updaters ──

interface ContributorDelta {
  tasks_completed_delta?: number;
  tasks_claimed_delta?: number;
  validations_done_delta?: number;
  score_delta?: number;
  initiatives_joined_delta?: number;
}

async function upsertContributorStat(db: Db, userId: string, delta: ContributorDelta) {
  const now = new Date().toISOString();

  // Try update first
  const { data: existing } = await db
    .from("contributor_stats")
    .select("user_id")
    .eq("user_id", userId)
    .single();

  if (existing) {
    // Fetch current values and increment
    const { data: current } = await db
      .from("contributor_stats")
      .select("*")
      .eq("user_id", userId)
      .single();

    if (current) {
      await db
        .from("contributor_stats")
        .update({
          tasks_completed: (current.tasks_completed ?? 0) + (delta.tasks_completed_delta ?? 0),
          tasks_claimed: (current.tasks_claimed ?? 0) + (delta.tasks_claimed_delta ?? 0),
          validations_done: (current.validations_done ?? 0) + (delta.validations_done_delta ?? 0),
          contribution_score: (current.contribution_score ?? 0) + (delta.score_delta ?? 0),
          initiatives_joined: (current.initiatives_joined ?? 0) + (delta.initiatives_joined_delta ?? 0),
          last_activity_at: now,
          updated_at: now,
        })
        .eq("user_id", userId);
    }
  } else {
    await db.from("contributor_stats").insert({
      user_id: userId,
      tasks_completed: delta.tasks_completed_delta ?? 0,
      tasks_claimed: delta.tasks_claimed_delta ?? 0,
      validations_done: delta.validations_done_delta ?? 0,
      contribution_score: delta.score_delta ?? 0,
      initiatives_joined: delta.initiatives_joined_delta ?? 0,
      last_activity_at: now,
      updated_at: now,
    });
  }
}

async function updateInitiativeProgressForTask(db: Db, taskId: string) {
  const { data: task } = await db
    .from("tasks")
    .select("initiative_id")
    .eq("id", taskId)
    .single();

  if (!task?.initiative_id) return;
  await refreshInitiativeProgress(db, task.initiative_id);
}

async function refreshInitiativeProgress(db: Db, initiativeId: string) {
  const now = new Date().toISOString();

  const [total, completed, open, inProgress, contributors, validators] = await Promise.all([
    db.from("tasks").select("*", { count: "exact", head: true }).eq("initiative_id", initiativeId),
    db.from("tasks").select("*", { count: "exact", head: true }).eq("initiative_id", initiativeId).eq("status", "validated"),
    db.from("tasks").select("*", { count: "exact", head: true }).eq("initiative_id", initiativeId).eq("status", "open"),
    db.from("tasks").select("*", { count: "exact", head: true }).eq("initiative_id", initiativeId).in("status", ["reserved", "claimed", "in_progress", "submitted"]),
    db.from("initiative_participants").select("*", { count: "exact", head: true }).eq("initiative_id", initiativeId),
    db.from("validations").select("validator_id", { count: "exact", head: true }),
  ]);

  const totalCount = total.count ?? 0;
  const completedCount = completed.count ?? 0;
  const pct = totalCount > 0 ? Math.round((completedCount / totalCount) * 100) : 0;

  const row = {
    initiative_id: initiativeId,
    tasks_total: totalCount,
    tasks_completed: completedCount,
    tasks_open: open.count ?? 0,
    tasks_in_progress: inProgress.count ?? 0,
    contributors_active: contributors.count ?? 0,
    validators_active: validators.count ?? 0,
    completion_pct: pct,
    last_event_at: now,
    updated_at: now,
  };

  // Upsert
  const { data: existing } = await db
    .from("initiative_progress")
    .select("initiative_id")
    .eq("initiative_id", initiativeId)
    .single();

  if (existing) {
    await db.from("initiative_progress").update(row).eq("initiative_id", initiativeId);
  } else {
    await db.from("initiative_progress").insert(row);
  }
}

async function updateOrgMetricsForTask(db: Db, taskId: string, action: "created" | "completed") {
  const { data: task } = await db
    .from("tasks")
    .select("org_id")
    .eq("id", taskId)
    .single();

  if (!task?.org_id) return;

  const orgId = task.org_id;
  const now = new Date().toISOString();

  const { data: existing } = await db
    .from("org_metrics")
    .select("*")
    .eq("org_id", orgId)
    .single();

  if (existing) {
    const updates: Record<string, unknown> = { last_event_at: now, updated_at: now };
    if (action === "created") updates.total_tasks = (existing.total_tasks ?? 0) + 1;
    if (action === "completed") updates.completed_tasks = (existing.completed_tasks ?? 0) + 1;
    await db.from("org_metrics").update(updates).eq("org_id", orgId);
  } else {
    await db.from("org_metrics").insert({
      org_id: orgId,
      total_tasks: action === "created" ? 1 : 0,
      completed_tasks: action === "completed" ? 1 : 0,
      last_event_at: now,
      updated_at: now,
    });
  }
}

// ── 6. CLEANUP PROCESSED QUEUE ITEMS ──
async function cleanupProcessedQueue(db: Db) {
  const oneDayAgo = new Date(Date.now() - 86400000).toISOString();
  const { count } = await db
    .from("event_queue")
    .delete()
    .eq("status", "processed")
    .lt("processed_at", oneDayAgo)
    .select("*", { count: "exact", head: true });

  return { cleaned_queue_items: count ?? 0 };
}

// ────────────────────────────────────────────────────────────
// MAIN HANDLER
// ────────────────────────────────────────────────────────────
Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  const db = getServiceDb();
  const results: Record<string, unknown> = {};

  try {
    const [reservations, milestones, cleanup, webhooks, queue, queueCleanup] =
      await Promise.all([
        expireReservations(db),
        detectMilestones(db),
        cleanupExpiredData(db),
        retryFailedWebhooks(db),
        processEventQueue(db),
        cleanupProcessedQueue(db),
      ]);

    Object.assign(results, reservations, milestones, cleanup, webhooks, queue, queueCleanup);
    results.processed_at = new Date().toISOString();

    console.log("Worker completed:", JSON.stringify(results));
    return json(results);
  } catch (e) {
    console.error("Worker error:", e);
    return json({ error: "Worker failed", details: String(e) }, 500);
  }
});
