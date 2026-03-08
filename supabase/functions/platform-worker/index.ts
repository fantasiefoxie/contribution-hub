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
  return createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
}

// ── Expire stale reservations ──
async function expireReservations(db: ReturnType<typeof createClient>) {
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

// ── Detect initiative milestones ──
async function detectMilestones(db: ReturnType<typeof createClient>) {
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

    // Check which milestones have been reached but not recorded
    const { data: existingMilestones } = await db
      .from("initiative_milestones")
      .select("percentage")
      .eq("initiative_id", initiative.id);

    const recorded = new Set((existingMilestones ?? []).map((m: any) => m.percentage));

    for (const threshold of milestoneThresholds) {
      if (pct >= threshold && !recorded.has(threshold)) {
        await db.from("initiative_milestones").insert({
          initiative_id: initiative.id,
          percentage: threshold,
        });

        // Notify participants
        const { data: participants } = await db
          .from("initiative_participants")
          .select("user_id")
          .eq("initiative_id", initiative.id);

        if (participants) {
          const notifications = participants.map((p: any) => ({
            user_id: p.user_id,
            type: "milestone",
            title: `Initiative reached ${threshold}%!`,
            body: `An initiative you joined has reached ${threshold}% completion.`,
            data: { initiative_id: initiative.id, percentage: threshold },
          }));
          if (notifications.length > 0) {
            await db.from("notifications").insert(notifications);
          }
        }

        detected++;
      }
    }

    // Auto-complete initiatives at 100%
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

// ── Clean up expired idempotency keys ──
async function cleanupIdempotencyKeys(db: ReturnType<typeof createClient>) {
  const { count } = await db
    .from("idempotency_keys")
    .delete()
    .lt("expires_at", new Date().toISOString())
    .select("*", { count: "exact", head: true });

  return { cleaned_keys: count ?? 0 };
}

// ── Clean up old rate limit entries ──
async function cleanupRateLimits(db: ReturnType<typeof createClient>) {
  const oneHourAgo = new Date(Date.now() - 3600000).toISOString();
  const { count } = await db
    .from("rate_limit_entries")
    .delete()
    .lt("window_start", oneHourAgo)
    .select("*", { count: "exact", head: true });

  return { cleaned_rate_limits: count ?? 0 };
}

// ── Dispatch webhook retries for failed deliveries ──
async function retryFailedWebhooks(db: ReturnType<typeof createClient>) {
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
      // Leave as-is
    }
  }

  return { webhook_retries: retried };
}

// ── Main handler ──
Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  const db = getServiceDb();
  const results: Record<string, unknown> = {};

  try {
    const [reservations, milestones, idempotency, rateLimits, webhooks] = await Promise.all([
      expireReservations(db),
      detectMilestones(db),
      cleanupIdempotencyKeys(db),
      cleanupRateLimits(db),
      retryFailedWebhooks(db),
    ]);

    Object.assign(results, reservations, milestones, idempotency, rateLimits, webhooks);
    results.processed_at = new Date().toISOString();

    console.log("Worker completed:", JSON.stringify(results));
    return json(results);
  } catch (e) {
    console.error("Worker error:", e);
    return json({ error: "Worker failed", details: String(e) }, 500);
  }
});
