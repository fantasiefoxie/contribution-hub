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

function err(message: string, status = 400) {
  return json({ error: message }, status);
}

function getSupabase(req: Request) {
  const authHeader = req.headers.get("Authorization") ?? "";
  return createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: authHeader } } }
  );
}

/** Extract authenticated user ID from JWT, or null if not authenticated */
async function getAuthUserId(req: Request): Promise<string | null> {
  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader?.startsWith("Bearer ")) return null;
    const supabase = getSupabase(req);
    const token = authHeader.replace("Bearer ", "");
    const { data, error } = await supabase.auth.getClaims(token);
    if (error || !data?.claims?.sub) return null;
    return data.claims.sub as string;
  } catch {
    return null;
  }
}

function getServiceDb() {
  return createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  );
}

/** Resolve any identifier (UUID, slug, alias) to a canonical UUID */
async function resolveId(resourceType: string, identifier: string): Promise<string | null> {
  const db = getServiceDb();
  const { data, error } = await db.rpc("resolve_resource", {
    _resource_type: resourceType,
    _identifier: identifier,
  });
  if (error || !data) return null;
  return data as string;
}

function notFound(identifier: string) {
  return json({ error: "Resource not found", identifier }, 404);
}

// ── GET /impact-worker/community-feed ──
// Recent public contributions for activity feed
async function communityFeed(req: Request) {
  const db = getServiceDb();
  const url = new URL(req.url);
  const limit = Math.min(parseInt(url.searchParams.get("limit") || "20"), 50);

  const { data: contributions, error } = await db
    .from("contributions")
    .select("id, score, impact_metrics, created_at, task_id, user_id")
    .order("created_at", { ascending: false })
    .limit(limit);

  if (error) return err(error.message, 500);

  // Enrich with task titles and user names
  const taskIds = [...new Set((contributions ?? []).map((c: any) => c.task_id))];
  const userIds = [...new Set((contributions ?? []).map((c: any) => c.user_id))];

  const [{ data: tasks }, { data: profiles }] = await Promise.all([
    db.from("tasks").select("id, title, tags, visibility").in("id", taskIds),
    db.from("profiles").select("id, display_name, avatar_url").in("id", userIds),
  ]);

  const taskMap = Object.fromEntries((tasks ?? []).map((t: any) => [t.id, t]));
  const profileMap = Object.fromEntries((profiles ?? []).map((p: any) => [p.id, p]));

  const feed = (contributions ?? [])
    .filter((c: any) => taskMap[c.task_id]?.visibility === "public")
    .map((c: any) => ({
      id: c.id,
      score: c.score,
      impact_metrics: c.impact_metrics,
      created_at: c.created_at,
      task: taskMap[c.task_id] || { title: "Unknown Task", tags: [] },
      contributor: profileMap[c.user_id] || { display_name: "Volunteer", avatar_url: null },
    }));

  return json({ feed });
}

// ── GET /impact-worker/my-impact ──
// Volunteer's cumulative impact
async function myImpact(req: Request) {
  const supabase = getSupabase(req);
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) return err("Unauthorized", 401);

  const db = getServiceDb();

  const { data: contributions } = await db
    .from("contributions")
    .select("score, impact_metrics, created_at, task_id")
    .eq("user_id", user.id);

  const items = contributions ?? [];
  const totalScore = items.reduce((sum: number, c: any) => sum + (c.score || 0), 0);

  // Aggregate impact metrics
  const aggregated: Record<string, number> = {};
  for (const c of items) {
    const metrics = c.impact_metrics || {};
    for (const [key, val] of Object.entries(metrics)) {
      if (typeof val === "number") {
        aggregated[key] = (aggregated[key] || 0) + val;
      }
    }
  }

  // Count distinct initiatives
  const taskIds = items.map((c: any) => c.task_id);
  const { data: tasks } = await db
    .from("tasks")
    .select("initiative_id")
    .in("id", taskIds)
    .not("initiative_id", "is", null);
  const initiativeCount = new Set((tasks ?? []).map((t: any) => t.initiative_id)).size;

  return json({
    tasks_completed: items.length,
    total_score: totalScore,
    impact_metrics: aggregated,
    initiatives_participated: initiativeCount,
    contributions: items,
  });
}

// ── GET /impact-worker/initiative/:id/progress ──
async function initiativeProgress(req: Request, rawId: string) {
  const initiativeId = await resolveId("initiative", rawId);
  if (!initiativeId) return notFound(rawId);

  const db = getServiceDb();

  const { data: initiative } = await db
    .from("initiatives")
    .select("*")
    .eq("id", initiativeId)
    .single();

  if (!initiative) return notFound(rawId);

  // Get all contributions for tasks in this initiative
  const { data: tasks } = await db
    .from("tasks")
    .select("id")
    .eq("initiative_id", initiativeId)
    .eq("status", "validated");

  const taskIds = (tasks ?? []).map((t: any) => t.id);

  let aggregated: Record<string, number> = {};
  let totalContributions = 0;

  if (taskIds.length > 0) {
    const { data: contributions } = await db
      .from("contributions")
      .select("score, impact_metrics")
      .in("task_id", taskIds);

    totalContributions = (contributions ?? []).length;
    for (const c of contributions ?? []) {
      const metrics = c.impact_metrics || {};
      for (const [key, val] of Object.entries(metrics)) {
        if (typeof val === "number") {
          aggregated[key] = (aggregated[key] || 0) + val;
        }
      }
    }
  }

  // Calculate progress percentage against targets
  const targets = initiative.target_metrics || {};
  const progress: Record<string, { current: number; target: number; percentage: number }> = {};
  for (const [key, target] of Object.entries(targets as Record<string, number>)) {
    const current = aggregated[key] || 0;
    progress[key] = { current, target, percentage: target > 0 ? Math.min(100, Math.round((current / target) * 100)) : 0 };
  }

  // Get milestones
  const { data: milestones } = await db
    .from("initiative_milestones")
    .select("*")
    .eq("initiative_id", initiativeId)
    .order("percentage", { ascending: true });

  return json({
    initiative,
    total_contributions: totalContributions,
    validated_tasks: taskIds.length,
    aggregated_metrics: aggregated,
    progress,
    milestones: milestones ?? [],
  });
}

// ── POST /impact-worker/process-validation ──
// Called after validation to update aggregates, check milestones, send notifications
async function processValidation(req: Request) {
  const db = getServiceDb();
  const body = await req.json();
  const { contribution_id } = body;

  if (!contribution_id) return err("contribution_id required");

  const { data: contribution } = await db.from("contributions").select("*").eq("id", contribution_id).single();
  if (!contribution) return err("Contribution not found", 404);

  const { data: task } = await db.from("tasks").select("*").eq("id", contribution.task_id).single();
  if (!task) return err("Task not found", 404);

  // 1. Send impact feedback notification to contributor
  const metrics = contribution.impact_metrics || {};
  const metricLines = Object.entries(metrics)
    .filter(([k, v]) => typeof v === "number" && k !== "difficulty")
    .map(([k, v]) => `${v} ${k.replace(/_/g, " ")}`)
    .join(", ");

  await db.from("notifications").insert({
    user_id: contribution.user_id,
    type: "impact_feedback",
    title: "Your work made an impact!",
    body: metricLines
      ? `Your task "${task.title}" contributed: ${metricLines}.`
      : `Your task "${task.title}" was validated. Score: +${contribution.score}`,
    data: { task_id: task.id, contribution_id: contribution.id, metrics },
  });

  // 2. Check initiative milestones
  if (task.initiative_id) {
    const { data: initiative } = await db.from("initiatives").select("*").eq("id", task.initiative_id).single();
    if (initiative && initiative.target_metrics) {
      const targets = initiative.target_metrics as Record<string, number>;

      // Get all contributions for this initiative
      const { data: initTasks } = await db.from("tasks").select("id").eq("initiative_id", task.initiative_id).eq("status", "validated");
      const initTaskIds = (initTasks ?? []).map((t: any) => t.id);

      if (initTaskIds.length > 0) {
        const { data: allContribs } = await db.from("contributions").select("impact_metrics").in("task_id", initTaskIds);
        const totals: Record<string, number> = {};
        for (const c of allContribs ?? []) {
          for (const [k, v] of Object.entries(c.impact_metrics || {})) {
            if (typeof v === "number") totals[k] = (totals[k] || 0) + v;
          }
        }

        // Check overall progress (average across all target metrics)
        let totalPct = 0;
        let metricCount = 0;
        for (const [key, target] of Object.entries(targets)) {
          if (target > 0) {
            totalPct += Math.min(100, ((totals[key] || 0) / target) * 100);
            metricCount++;
          }
        }
        const overallPct = metricCount > 0 ? Math.round(totalPct / metricCount) : 0;

        // Check milestones: 25, 50, 75, 100
        for (const milestone of [25, 50, 75, 100]) {
          if (overallPct >= milestone) {
            const { error: insertErr } = await db
              .from("initiative_milestones")
              .insert({ initiative_id: task.initiative_id, percentage: milestone })
              .select().single();

            // If insert succeeds (not duplicate), emit notification
            if (!insertErr) {
              await db.from("events").insert({
                type: "BadgeAwarded", // Using closest available event type
                actor_id: contribution.user_id,
                entity_type: "initiative",
                entity_id: task.initiative_id,
                payload: { milestone, overall_percentage: overallPct },
              });

              // Notify initiative participants (get unique contributors)
              const { data: participantContribs } = await db
                .from("contributions")
                .select("user_id")
                .in("task_id", initTaskIds);
              const participants = [...new Set((participantContribs ?? []).map((c: any) => c.user_id))];

              const notifications = participants.map((uid: string) => ({
                user_id: uid,
                type: "milestone_reached",
                title: `🎉 ${initiative.title} reached ${milestone}%!`,
                body: `The initiative "${initiative.title}" has reached ${milestone}% of its goal.`,
                data: { initiative_id: task.initiative_id, milestone },
              }));

              if (notifications.length > 0) {
                await db.from("notifications").insert(notifications);
              }
            }
          }
        }
      }
    }
  }

  return json({ success: true });
}

// ── POST /impact-worker/weekly-summary ──
// Generate weekly summaries for all active volunteers (called by cron)
async function weeklyySummary(_req: Request) {
  const db = getServiceDb();
  const oneWeekAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString();

  const { data: recentContribs } = await db
    .from("contributions")
    .select("user_id, score, impact_metrics, task_id")
    .gte("created_at", oneWeekAgo);

  if (!recentContribs || recentContribs.length === 0) return json({ message: "No contributions this week" });

  // Group by user
  const byUser: Record<string, any[]> = {};
  for (const c of recentContribs) {
    if (!byUser[c.user_id]) byUser[c.user_id] = [];
    byUser[c.user_id].push(c);
  }

  const notifications = [];
  for (const [userId, contribs] of Object.entries(byUser)) {
    const taskCount = contribs.length;
    const totalScore = contribs.reduce((s, c) => s + (c.score || 0), 0);
    const allMetrics: Record<string, number> = {};
    for (const c of contribs) {
      for (const [k, v] of Object.entries(c.impact_metrics || {})) {
        if (typeof v === "number" && k !== "difficulty") allMetrics[k] = (allMetrics[k] || 0) + v;
      }
    }

    const metricLines = Object.entries(allMetrics)
      .map(([k, v]) => `${v} ${k.replace(/_/g, " ")}`)
      .join(", ");

    notifications.push({
      user_id: userId,
      type: "weekly_summary",
      title: "📊 Your Weekly Impact",
      body: `This week: ${taskCount} task${taskCount !== 1 ? "s" : ""} completed, +${totalScore} points.${metricLines ? ` Impact: ${metricLines}.` : ""}`,
      data: { task_count: taskCount, total_score: totalScore, metrics: allMetrics },
    });
  }

  if (notifications.length > 0) {
    await db.from("notifications").insert(notifications);
  }

  return json({ summaries_sent: notifications.length });
}

// ── GET /impact-worker/next-mission ──
// Recommend a related or nearby task after completion
async function nextMission(req: Request) {
  const supabase = getSupabase(req);
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) return err("Unauthorized", 401);

  const db = getServiceDb();
  const url = new URL(req.url);
  const currentTaskId = url.searchParams.get("current_task_id");

  // Get current task tags for relevance
  let tags: string[] = [];
  if (currentTaskId) {
    const { data: task } = await db.from("tasks").select("tags").eq("id", currentTaskId).single();
    tags = task?.tags ?? [];
  }

  // Find open tasks the user hasn't created
  let query = db
    .from("tasks")
    .select("id, title, tags, difficulty, lock_duration_mins")
    .eq("status", "open")
    .eq("visibility", "public")
    .neq("created_by", user.id)
    .limit(10)
    .order("created_at", { ascending: false });

  const { data: candidates } = await query;
  if (!candidates || candidates.length === 0) return json({ next_mission: null });

  // Score by tag overlap
  const scored = candidates.map((t: any) => {
    const overlap = (t.tags ?? []).filter((tag: string) => tags.includes(tag)).length;
    return { ...t, score: overlap };
  });
  scored.sort((a: any, b: any) => b.score - a.score);

  const pick = scored[0];
  return json({
    next_mission: {
      id: pick.id,
      title: pick.title,
      estimatedTime: `${pick.lock_duration_mins}min`,
      tags: pick.tags,
    },
  });
}

// ── POST /impact-worker/send-reminders ──
// Send reminders for claimed tasks with low activity (called by cron)
async function sendReminders(_req: Request) {
  const db = getServiceDb();

  // Find claimed/in_progress tasks
  const { data: activeTasks } = await db
    .from("tasks")
    .select("id, title, locked_by, locked_until, status")
    .in("status", ["claimed", "in_progress"])
    .not("locked_by", "is", null)
    .not("locked_until", "is", null);

  if (!activeTasks || activeTasks.length === 0) return json({ reminders_sent: 0 });

  const notifications = [];
  const now = Date.now();

  for (const task of activeTasks) {
    const lockEnd = new Date(task.locked_until).getTime();
    const msLeft = lockEnd - now;
    const hoursLeft = Math.floor(msLeft / 3600000);

    // Send reminder when ~50% of lock time has passed and < 4h remaining
    if (msLeft > 0 && msLeft < 4 * 3600000) {
      // Check if we already sent a reminder recently
      const { data: existing } = await db
        .from("notifications")
        .select("id")
        .eq("user_id", task.locked_by)
        .eq("type", "mission_reminder")
        .gte("created_at", new Date(now - 2 * 3600000).toISOString())
        .limit(1);

      if (!existing || existing.length === 0) {
        notifications.push({
          user_id: task.locked_by,
          type: "mission_reminder",
          title: "⏰ Mission Reminder",
          body: `Your mission "${task.title}" is still active. ${hoursLeft}h remaining.`,
          data: { task_id: task.id, hours_remaining: hoursLeft },
        });
      }
    }
  }

  if (notifications.length > 0) {
    await db.from("notifications").insert(notifications);
  }

  return json({ reminders_sent: notifications.length });
}

// ── Router ──
Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  const url = new URL(req.url);
  const pathParts = url.pathname.replace(/^\/impact-worker\/?/, "").split("/").filter(Boolean);

  try {
    if (req.method === "GET" && pathParts[0] === "community-feed") return await communityFeed(req);
    if (req.method === "GET" && pathParts[0] === "my-impact") return await myImpact(req);
    if (req.method === "GET" && pathParts[0] === "next-mission") return await nextMission(req);
    if (req.method === "GET" && pathParts[0] === "initiative" && pathParts.length === 3 && pathParts[2] === "progress") {
      return await initiativeProgress(req, pathParts[1]);
    }
    if (req.method === "POST" && pathParts[0] === "process-validation") return await processValidation(req);
    if (req.method === "POST" && pathParts[0] === "weekly-summary") return await weeklyySummary(req);
    if (req.method === "POST" && pathParts[0] === "send-reminders") return await sendReminders(req);

    return err("Not found", 404);
  } catch (e) {
    console.error("Impact worker error:", e);
    return err("Internal server error", 500);
  }
});
