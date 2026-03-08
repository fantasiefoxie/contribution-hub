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

function getServiceDb() {
  return createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
}
function getSupabase(req: Request) {
  const authHeader = req.headers.get("Authorization") ?? "";
  return createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, {
    global: { headers: { Authorization: authHeader } },
  });
}
async function getUser(supabase: ReturnType<typeof createClient>) {
  const { data: { user }, error } = await supabase.auth.getUser();
  if (error || !user) return null;
  return user;
}

// ── GET /platform-health — system-wide health metrics ──
async function platformHealth() {
  const db = getServiceDb();
  const todayStart = new Date();
  todayStart.setHours(0, 0, 0, 0);
  const todayISO = todayStart.toISOString();

  const [
    { count: totalVolunteers },
    { count: totalTasks },
    { count: totalInitiatives },
    { count: activeInitiatives },
    { count: tasksCompletedToday },
    { count: validationBacklog },
    { data: todayEvents },
    { data: recentContribs },
  ] = await Promise.all([
    db.from("profiles").select("*", { count: "exact", head: true }),
    db.from("tasks").select("*", { count: "exact", head: true }),
    db.from("initiatives").select("*", { count: "exact", head: true }),
    db.from("initiatives").select("*", { count: "exact", head: true }).eq("status", "active"),
    db.from("contributions").select("*", { count: "exact", head: true }).gte("created_at", todayISO),
    db.from("task_submissions").select("*", { count: "exact", head: true }).eq("status", "pending"),
    db.from("events").select("type, created_at").gte("created_at", todayISO).order("created_at", { ascending: false }).limit(100),
    db.from("contributions").select("user_id").gte("created_at", todayISO),
  ]);

  const activeVolunteersToday = new Set((recentContribs ?? []).map((c: any) => c.user_id)).size;

  // Event breakdown
  const eventCounts: Record<string, number> = {};
  for (const e of todayEvents ?? []) {
    eventCounts[e.type] = (eventCounts[e.type] || 0) + 1;
  }

  return json({
    total_volunteers: totalVolunteers ?? 0,
    active_volunteers_today: activeVolunteersToday,
    total_tasks: totalTasks ?? 0,
    tasks_completed_today: tasksCompletedToday ?? 0,
    total_initiatives: totalInitiatives ?? 0,
    active_initiatives: activeInitiatives ?? 0,
    validation_backlog: validationBacklog ?? 0,
    event_breakdown_today: eventCounts,
  });
}

// ── GET /initiative-health/:id — initiative-specific health ──
async function initiativeHealth(_req: Request, initiativeId: string) {
  const db = getServiceDb();

  const [
    { data: initiative },
    { data: allTasks },
    { data: participants },
    { data: validations },
  ] = await Promise.all([
    db.from("initiatives").select("*").eq("id", initiativeId).single(),
    db.from("tasks").select("id, status, created_at, updated_at").eq("initiative_id", initiativeId),
    db.from("initiative_participants").select("user_id, joined_at").eq("initiative_id", initiativeId),
    db.from("task_submissions").select("id, submitted_at, status, task_id")
      .in("task_id", (await db.from("tasks").select("id").eq("initiative_id", initiativeId)).data?.map((t: any) => t.id) ?? []),
  ]);

  if (!initiative) return err("Initiative not found", 404);

  const tasks = allTasks ?? [];
  const totalTasks = tasks.length;
  const completedTasks = tasks.filter((t: any) => t.status === "validated").length;
  const completionRate = totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0;

  // Validation turnaround (avg time from submission to validation)
  const pendingSubs = (validations ?? []).filter((s: any) => s.status === "pending");
  const resolvedSubs = (validations ?? []).filter((s: any) => s.status !== "pending");

  // Progress velocity (tasks completed per week)
  const oneWeekAgo = Date.now() - 7 * 24 * 3600000;
  const recentCompletions = tasks.filter((t: any) => t.status === "validated" && new Date(t.updated_at).getTime() > oneWeekAgo).length;

  return json({
    initiative_id: initiativeId,
    title: initiative.title,
    task_completion_rate: completionRate,
    total_tasks: totalTasks,
    completed_tasks: completedTasks,
    volunteer_count: (participants ?? []).length,
    validation_backlog: pendingSubs.length,
    validations_completed: resolvedSubs.length,
    progress_velocity_weekly: recentCompletions,
  });
}

// ── GET /validator-monitoring — track all validators ──
async function validatorMonitoring() {
  const db = getServiceDb();

  const { data: allValidations } = await db
    .from("validations")
    .select("id, validator_id, decision, decided_at, submission_id")
    .order("decided_at", { ascending: false })
    .limit(1000);

  if (!allValidations || allValidations.length === 0) return json({ validators: [] });

  // Group by validator
  const byValidator: Record<string, { approved: number; rejected: number; total: number; decisions: any[] }> = {};
  for (const v of allValidations) {
    if (!byValidator[v.validator_id]) byValidator[v.validator_id] = { approved: 0, rejected: 0, total: 0, decisions: [] };
    byValidator[v.validator_id].total += 1;
    if (v.decision === "approved") byValidator[v.validator_id].approved += 1;
    if (v.decision === "rejected") byValidator[v.validator_id].rejected += 1;
    byValidator[v.validator_id].decisions.push(v);
  }

  // Check appeal overturn rates
  const submissionIds = allValidations.map((v: any) => v.submission_id);
  const { data: appeals } = await db
    .from("appeals")
    .select("validation_id, status")
    .in("submission_id", submissionIds.slice(0, 500));

  const overturnByValidator: Record<string, number> = {};
  for (const a of appeals ?? []) {
    if (a.status === "resolved") {
      const validation = allValidations.find((v: any) => v.id === a.validation_id);
      if (validation) {
        overturnByValidator[validation.validator_id] = (overturnByValidator[validation.validator_id] || 0) + 1;
      }
    }
  }

  // Enrich with profiles
  const validatorIds = Object.keys(byValidator);
  const { data: profiles } = await db.from("profiles").select("id, display_name, avatar_url").in("id", validatorIds);
  const profileMap = Object.fromEntries((profiles ?? []).map((p: any) => [p.id, p]));

  const validators = validatorIds.map((vid) => {
    const stats = byValidator[vid];
    const approvalRate = stats.total > 0 ? Math.round((stats.approved / stats.total) * 100) : 0;
    const overturns = overturnByValidator[vid] || 0;
    const overturnRate = stats.total > 0 ? Math.round((overturns / stats.total) * 100) : 0;
    const flagged = approvalRate > 98 || approvalRate < 40 || overturnRate > 20;

    // Avg review speed (ms between submission and decision)
    const recentDecisions = stats.decisions.slice(0, 20);
    let avgSpeed = 0;
    if (recentDecisions.length > 0) {
      const speeds = recentDecisions.map((d: any) => new Date(d.decided_at).getTime());
      avgSpeed = speeds.length > 1 ? Math.round((speeds[0] - speeds[speeds.length - 1]) / speeds.length / 3600000) : 0;
    }

    return {
      validator_id: vid,
      display_name: profileMap[vid]?.display_name || "Unknown",
      avatar_url: profileMap[vid]?.avatar_url,
      total_reviews: stats.total,
      approval_rate: approvalRate,
      rejection_rate: 100 - approvalRate,
      overturns,
      overturn_rate: overturnRate,
      avg_review_hours: avgSpeed,
      flagged,
      flag_reasons: flagged ? [
        ...(approvalRate > 98 ? ["rubber-stamping (>98% approval)"] : []),
        ...(approvalRate < 40 ? ["unusually strict (<40% approval)"] : []),
        ...(overturnRate > 20 ? ["high overturn rate (>20%)"] : []),
      ] : [],
    };
  }).sort((a, b) => b.total_reviews - a.total_reviews);

  return json({ validators });
}

// ── GET /fraud-signals — detect suspicious patterns ──
async function fraudSignals() {
  const db = getServiceDb();

  const alerts: any[] = [];

  // 1. Repeated validator-contributor pairings
  const { data: validations } = await db
    .from("validations")
    .select("validator_id, submission_id, decision")
    .order("decided_at", { ascending: false })
    .limit(500);

  if (validations && validations.length > 0) {
    const subIds = validations.map((v: any) => v.submission_id);
    const { data: submissions } = await db
      .from("task_submissions")
      .select("id, user_id")
      .in("id", subIds);

    const subMap = Object.fromEntries((submissions ?? []).map((s: any) => [s.id, s.user_id]));
    const pairCounts: Record<string, number> = {};

    for (const v of validations) {
      const contributorId = subMap[v.submission_id];
      if (contributorId && contributorId !== v.validator_id) {
        const pair = `${v.validator_id}:${contributorId}`;
        pairCounts[pair] = (pairCounts[pair] || 0) + 1;
      }
    }

    for (const [pair, count] of Object.entries(pairCounts)) {
      if (count >= 5) {
        const [validatorId, contributorId] = pair.split(":");
        alerts.push({
          type: "repeated_pairing",
          severity: count >= 10 ? "high" : "medium",
          description: `Validator and contributor paired ${count} times`,
          details: { validator_id: validatorId, contributor_id: contributorId, count },
        });
      }
    }
  }

  // 2. Unrealistically fast completions (claimed and validated in < 2 min)
  const { data: recentClaims } = await db
    .from("task_claims")
    .select("task_id, user_id, claimed_at")
    .order("claimed_at", { ascending: false })
    .limit(200);

  if (recentClaims && recentClaims.length > 0) {
    const claimTaskIds = recentClaims.map((c: any) => c.task_id);
    const { data: fastSubs } = await db
      .from("task_submissions")
      .select("task_id, user_id, submitted_at")
      .in("task_id", claimTaskIds);

    const claimMap = Object.fromEntries(recentClaims.map((c: any) => [`${c.task_id}:${c.user_id}`, c.claimed_at]));

    for (const sub of fastSubs ?? []) {
      const claimTime = claimMap[`${sub.task_id}:${sub.user_id}`];
      if (claimTime) {
        const durationMs = new Date(sub.submitted_at).getTime() - new Date(claimTime).getTime();
        if (durationMs > 0 && durationMs < 120000) { // < 2 minutes
          alerts.push({
            type: "fast_completion",
            severity: durationMs < 30000 ? "high" : "medium",
            description: `Task completed in ${Math.round(durationMs / 1000)}s`,
            details: { task_id: sub.task_id, user_id: sub.user_id, duration_seconds: Math.round(durationMs / 1000) },
          });
        }
      }
    }
  }

  // 3. Submissions lacking evidence (empty or very short content)
  const { data: recentSubs } = await db
    .from("task_submissions")
    .select("id, user_id, content, attachments, submitted_at")
    .order("submitted_at", { ascending: false })
    .limit(100);

  for (const sub of recentSubs ?? []) {
    const hasContent = sub.content && sub.content.trim().length > 10;
    const hasAttachments = sub.attachments && Array.isArray(sub.attachments) && sub.attachments.length > 0;
    if (!hasContent && !hasAttachments) {
      alerts.push({
        type: "low_evidence",
        severity: "low",
        description: "Submission with minimal evidence",
        details: { submission_id: sub.id, user_id: sub.user_id, content_length: (sub.content || "").length },
      });
    }
  }

  // Sort by severity
  const severityOrder: Record<string, number> = { high: 0, medium: 1, low: 2 };
  alerts.sort((a, b) => (severityOrder[a.severity] ?? 3) - (severityOrder[b.severity] ?? 3));

  return json({ alerts: alerts.slice(0, 50), total_alerts: alerts.length });
}

// ── GET /integrity-score/:userId — contribution integrity score ──
async function integrityScore(_req: Request, userId: string) {
  const db = getServiceDb();

  const [
    { data: contributions },
    { data: submissions },
    { data: appeals },
    { data: profile },
  ] = await Promise.all([
    db.from("contributions").select("id, score").eq("user_id", userId),
    db.from("task_submissions").select("id, status").eq("user_id", userId),
    db.from("appeals").select("id, status").eq("appellant_id", userId),
    db.from("profiles").select("display_name, avatar_url, status").eq("id", userId).single(),
  ]);

  const totalSubs = (submissions ?? []).length;
  const approvedSubs = (submissions ?? []).filter((s: any) => s.status === "approved" || s.status === "validated").length;
  const rejectedSubs = (submissions ?? []).filter((s: any) => s.status === "rejected").length;
  const completionRate = totalSubs > 0 ? Math.round((approvedSubs / totalSubs) * 100) : 0;

  const totalAppeals = (appeals ?? []).length;
  const wonAppeals = (appeals ?? []).filter((a: any) => a.status === "resolved").length;

  // Integrity score: weighted combination
  const taskScore = Math.min(100, (contributions ?? []).length * 2); // More tasks = higher base
  const qualityScore = completionRate; // Approval rate
  const appealPenalty = totalAppeals > 0 ? Math.max(0, 100 - (rejectedSubs * 10)) : 100;

  const integrityPct = Math.round((taskScore * 0.3 + qualityScore * 0.5 + appealPenalty * 0.2));
  const clampedScore = Math.max(0, Math.min(100, integrityPct));

  let tier: string;
  if (clampedScore >= 90) tier = "excellent";
  else if (clampedScore >= 70) tier = "good";
  else if (clampedScore >= 50) tier = "moderate";
  else tier = "needs_improvement";

  return json({
    user_id: userId,
    display_name: profile?.display_name,
    integrity_score: clampedScore,
    tier,
    breakdown: {
      task_volume: taskScore,
      quality: qualityScore,
      appeal_history: appealPenalty,
    },
    stats: {
      total_contributions: (contributions ?? []).length,
      total_submissions: totalSubs,
      approved: approvedSubs,
      rejected: rejectedSubs,
      completion_rate: completionRate,
      appeals_filed: totalAppeals,
      appeals_won: wonAppeals,
    },
  });
}

// ── GET /event-stream — recent platform events ──
async function eventStream(req: Request) {
  const db = getServiceDb();
  const url = new URL(req.url);
  const limit = Math.min(parseInt(url.searchParams.get("limit") || "50"), 100);

  const { data: events } = await db
    .from("events")
    .select("id, type, entity_type, entity_id, actor_id, payload, created_at")
    .order("created_at", { ascending: false })
    .limit(limit);

  // Enrich with actor names
  const actorIds = [...new Set((events ?? []).map((e: any) => e.actor_id).filter(Boolean))];
  let profileMap: Record<string, any> = {};
  if (actorIds.length > 0) {
    const { data: profiles } = await db.from("profiles").select("id, display_name").in("id", actorIds);
    profileMap = Object.fromEntries((profiles ?? []).map((p: any) => [p.id, p]));
  }

  const enriched = (events ?? []).map((e: any) => ({
    ...e,
    actor_name: profileMap[e.actor_id]?.display_name || "System",
  }));

  return json({ events: enriched });
}

// ── GET /transparency — public aggregated stats ──
async function transparency() {
  const db = getServiceDb();

  const [
    { count: totalVolunteers },
    { count: totalTasks },
    { count: completedTasks },
    { count: totalInitiatives },
    { count: totalContributions },
    { count: totalValidations },
    { data: recentContribs },
  ] = await Promise.all([
    db.from("profiles").select("*", { count: "exact", head: true }),
    db.from("tasks").select("*", { count: "exact", head: true }),
    db.from("tasks").select("*", { count: "exact", head: true }).eq("status", "validated"),
    db.from("initiatives").select("*", { count: "exact", head: true }),
    db.from("contributions").select("*", { count: "exact", head: true }),
    db.from("validations").select("*", { count: "exact", head: true }),
    db.from("contributions").select("score, impact_metrics").limit(1000),
  ]);

  // Aggregate impact metrics
  const aggregated: Record<string, number> = {};
  let totalScore = 0;
  for (const c of recentContribs ?? []) {
    totalScore += c.score || 0;
    for (const [k, v] of Object.entries(c.impact_metrics || {})) {
      if (typeof v === "number" && k !== "difficulty") aggregated[k] = (aggregated[k] || 0) + v;
    }
  }

  return json({
    total_volunteers: totalVolunteers ?? 0,
    total_tasks: totalTasks ?? 0,
    completed_tasks: completedTasks ?? 0,
    total_initiatives: totalInitiatives ?? 0,
    total_contributions: totalContributions ?? 0,
    total_validations: totalValidations ?? 0,
    total_score: totalScore,
    impact_metrics: aggregated,
  });
}

// ── Router ──
Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  const url = new URL(req.url);
  const pathParts = url.pathname.replace(/^\/observability\/?/, "").split("/").filter(Boolean);

  try {
    if (req.method !== "GET") return err("Method not allowed", 405);

    const route = pathParts[0] || "";

    if (route === "platform-health") return await platformHealth();
    if (route === "initiative-health" && pathParts[1]) return await initiativeHealth(req, pathParts[1]);
    if (route === "validator-monitoring") return await validatorMonitoring();
    if (route === "fraud-signals") return await fraudSignals();
    if (route === "integrity-score" && pathParts[1]) return await integrityScore(req, pathParts[1]);
    if (route === "event-stream") return await eventStream(req);
    if (route === "transparency") return await transparency();

    return err("Not found", 404);
  } catch (e) {
    console.error("Observability error:", e);
    return err("Internal server error", 500);
  }
});
