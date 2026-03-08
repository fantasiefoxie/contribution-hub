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
  return createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, {
    global: { headers: { Authorization: authHeader } },
  });
}
function getServiceDb() {
  return createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
}
async function getUser(supabase: ReturnType<typeof createClient>) {
  const { data: { user }, error } = await supabase.auth.getUser();
  if (error || !user) return null;
  return user;
}

// Role tier definitions & criteria
const ROLE_TIERS = [
  { key: "volunteer", label: "Volunteer", level: 0 },
  { key: "trusted", label: "Trusted Contributor", level: 1 },
  { key: "validator", label: "Validator", level: 2 },
  { key: "moderator", label: "Moderator", level: 3 },
  { key: "admin", label: "Campaign Organizer", level: 4 },
];

const PROMOTION_CRITERIA: Record<string, { validated_tasks: number; approval_rate: number; initiatives: number; description: string }> = {
  trusted: {
    validated_tasks: 10,
    approval_rate: 90,
    initiatives: 1,
    description: "Complete 10+ validated tasks with ≥90% approval rate and participate in at least 1 initiative.",
  },
  validator: {
    validated_tasks: 25,
    approval_rate: 95,
    initiatives: 3,
    description: "Complete 25+ validated tasks with ≥95% approval rate and participate in 3+ initiatives. Requires moderator recommendation.",
  },
  moderator: {
    validated_tasks: 50,
    approval_rate: 95,
    initiatives: 5,
    description: "Complete 50+ validated tasks with ≥95% approval, 5+ initiatives. Requires admin confirmation.",
  },
  admin: {
    validated_tasks: 100,
    approval_rate: 95,
    initiatives: 10,
    description: "Complete 100+ validated tasks, 10+ initiatives. Campaign Organizer role granted by admins.",
  },
};

// Map role key to db values
function roleKeyToDbRole(key: string): string | null {
  const map: Record<string, string> = { validator: "validator", moderator: "moderator", admin: "admin" };
  return map[key] || null;
}

// ── GET /my-progression — get current user's role progression status ──
async function myProgression(req: Request) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const db = getServiceDb();

  // Get user profile, roles, and stats
  const [
    { data: profile },
    { data: roles },
    { data: contributions },
    { data: submissions },
    { data: participations },
  ] = await Promise.all([
    db.from("profiles").select("status, display_name, avatar_url").eq("id", user.id).single(),
    db.from("user_roles").select("role").eq("user_id", user.id),
    db.from("contributions").select("id, score").eq("user_id", user.id),
    db.from("task_submissions").select("id, status").eq("user_id", user.id),
    db.from("initiative_participants").select("initiative_id").eq("user_id", user.id),
  ]);

  const userRoles = (roles ?? []).map((r: any) => r.role);
  const validatedTasks = (contributions ?? []).length;
  const totalSubmissions = (submissions ?? []).length;
  const approvedSubmissions = (submissions ?? []).filter((s: any) => s.status === "approved" || s.status === "validated").length;
  const approvalRate = totalSubmissions > 0 ? Math.round((approvedSubmissions / totalSubmissions) * 100) : 0;
  const initiativeCount = new Set((participations ?? []).map((p: any) => p.initiative_id)).size;
  const totalScore = (contributions ?? []).reduce((s: number, c: any) => s + (c.score || 0), 0);

  // Determine current tier
  let currentTier = "volunteer";
  if (userRoles.includes("admin")) currentTier = "admin";
  else if (userRoles.includes("moderator")) currentTier = "moderator";
  else if (userRoles.includes("validator")) currentTier = "validator";
  else if (profile?.status === "trusted") currentTier = "trusted";

  const currentLevel = ROLE_TIERS.find((t) => t.key === currentTier)?.level ?? 0;

  // Calculate eligibility for each higher tier
  const stats = { validated_tasks: validatedTasks, approval_rate: approvalRate, initiatives: initiativeCount };
  const progression = ROLE_TIERS.map((tier) => {
    const criteria = PROMOTION_CRITERIA[tier.key];
    if (!criteria) return { ...tier, eligible: tier.level <= currentLevel, current: tier.key === currentTier, progress: null };

    const tasksPct = Math.min(100, Math.round((stats.validated_tasks / criteria.validated_tasks) * 100));
    const ratePct = Math.min(100, Math.round((stats.approval_rate / criteria.approval_rate) * 100));
    const initPct = Math.min(100, Math.round((stats.initiatives / criteria.initiatives) * 100));
    const overallPct = Math.round((tasksPct + ratePct + initPct) / 3);
    const meetsAll = tasksPct >= 100 && ratePct >= 100 && initPct >= 100;

    return {
      ...tier,
      eligible: meetsAll && tier.level === currentLevel + 1,
      current: tier.key === currentTier,
      achieved: tier.level <= currentLevel,
      criteria,
      progress: {
        validated_tasks: { current: stats.validated_tasks, required: criteria.validated_tasks, pct: tasksPct },
        approval_rate: { current: stats.approval_rate, required: criteria.approval_rate, pct: ratePct },
        initiatives: { current: stats.initiatives, required: criteria.initiatives, pct: initPct },
        overall: overallPct,
      },
    };
  });

  return json({
    user_id: user.id,
    display_name: profile?.display_name,
    current_tier: currentTier,
    current_level: currentLevel,
    roles: userRoles,
    stats: { ...stats, total_score: totalScore },
    progression,
    tiers: ROLE_TIERS,
  });
}

// ── POST /request-promotion — request promotion to next tier ──
async function requestPromotion(req: Request) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const db = getServiceDb();
  const body = await req.json();
  const { target_role } = body;

  if (!target_role || !PROMOTION_CRITERIA[target_role]) {
    return err("Invalid target role");
  }

  // Check eligibility (same logic as myProgression)
  const [
    { data: contributions },
    { data: submissions },
    { data: participations },
  ] = await Promise.all([
    db.from("contributions").select("id").eq("user_id", user.id),
    db.from("task_submissions").select("id, status").eq("user_id", user.id),
    db.from("initiative_participants").select("initiative_id").eq("user_id", user.id),
  ]);

  const criteria = PROMOTION_CRITERIA[target_role];
  const validatedTasks = (contributions ?? []).length;
  const totalSubs = (submissions ?? []).length;
  const approvedSubs = (submissions ?? []).filter((s: any) => s.status === "approved" || s.status === "validated").length;
  const approvalRate = totalSubs > 0 ? Math.round((approvedSubs / totalSubs) * 100) : 0;
  const initCount = new Set((participations ?? []).map((p: any) => p.initiative_id)).size;

  if (validatedTasks < criteria.validated_tasks || approvalRate < criteria.approval_rate || initCount < criteria.initiatives) {
    return err("You don't meet the criteria for this role yet");
  }

  // For trusted: auto-promote by updating profile status
  if (target_role === "trusted") {
    await db.from("profiles").update({ status: "trusted" }).eq("id", user.id);

    await db.from("notifications").insert({
      user_id: user.id,
      type: "role_promotion",
      title: "🎉 You're now a Trusted Contributor!",
      body: "You can now create micro-initiatives and invite volunteers to team missions.",
      data: { role: "trusted" },
    });

    await db.from("events").insert({
      type: "BadgeAwarded",
      actor_id: user.id,
      entity_type: "user",
      entity_id: user.id,
      payload: { role: "trusted", promotion: true },
    });

    return json({ promoted: true, role: "trusted" });
  }

  // For higher roles: create a notification for admins/moderators to confirm
  // Find admins
  const { data: adminRoles } = await db.from("user_roles").select("user_id").in("role", ["admin", "moderator"]);
  const adminIds = (adminRoles ?? []).map((r: any) => r.user_id);

  if (adminIds.length > 0) {
    const { data: userProfile } = await db.from("profiles").select("display_name").eq("id", user.id).single();
    const notifications = adminIds.map((adminId: string) => ({
      user_id: adminId,
      type: "promotion_request",
      title: `📋 Role promotion request`,
      body: `${userProfile?.display_name || "A volunteer"} has requested promotion to ${ROLE_TIERS.find(t => t.key === target_role)?.label}. Tasks: ${validatedTasks}, Approval: ${approvalRate}%, Initiatives: ${initCount}.`,
      data: { candidate_id: user.id, target_role, stats: { validatedTasks, approvalRate, initCount } },
    }));
    await db.from("notifications").insert(notifications);
  }

  // Also notify the user
  await db.from("notifications").insert({
    user_id: user.id,
    type: "promotion_pending",
    title: "Promotion request submitted",
    body: `Your request to become a ${ROLE_TIERS.find(t => t.key === target_role)?.label} has been sent to moderators for review.`,
    data: { target_role },
  });

  return json({ requested: true, target_role });
}

// ── POST /confirm-promotion — admin/mod confirms a promotion ──
async function confirmPromotion(req: Request) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const db = getServiceDb();

  // Check if requester is admin or moderator
  const { data: callerRoles } = await db.from("user_roles").select("role").eq("user_id", user.id);
  const hasAuthority = (callerRoles ?? []).some((r: any) => r.role === "admin" || r.role === "moderator");
  if (!hasAuthority) return err("Only moderators or admins can confirm promotions", 403);

  const body = await req.json();
  const { candidate_id, target_role } = body;
  if (!candidate_id || !target_role) return err("candidate_id and target_role required");

  const dbRole = roleKeyToDbRole(target_role);
  if (!dbRole) return err("Invalid target role");

  // Insert the role (upsert to avoid duplicates)
  const { error: insertErr } = await db
    .from("user_roles")
    .upsert({ user_id: candidate_id, role: dbRole }, { onConflict: "user_id,role" });

  if (insertErr) return err(insertErr.message, 500);

  // Also update profile status to trusted if not already
  await db.from("profiles").update({ status: "trusted" }).eq("id", candidate_id);

  // Notify the candidate
  const tierLabel = ROLE_TIERS.find(t => t.key === target_role)?.label || target_role;
  await db.from("notifications").insert({
    user_id: candidate_id,
    type: "role_promotion",
    title: `🎉 You've been promoted to ${tierLabel}!`,
    body: `A moderator has confirmed your promotion. You now have ${tierLabel} capabilities.`,
    data: { role: target_role },
  });

  await db.from("audit_logs").insert({
    action: "role_promoted",
    actor_id: user.id,
    entity_type: "user",
    entity_id: candidate_id,
    details: { target_role, confirmed_by: user.id },
  });

  return json({ confirmed: true, candidate_id, role: target_role });
}

// ── GET /opportunities — list leadership opportunities ──
async function opportunities(req: Request) {
  const db = getServiceDb();

  // Find active initiatives that need validators/moderators
  const { data: activeInitiatives } = await db
    .from("initiatives")
    .select("id, title, status")
    .eq("status", "active")
    .order("created_at", { ascending: false })
    .limit(20);

  const opps: any[] = [];

  for (const init of activeInitiatives ?? []) {
    // Check how many validators/mods are linked to this initiative's orgs
    const { data: initOrgs } = await db
      .from("initiative_orgs")
      .select("org_id")
      .eq("initiative_id", init.id);

    const orgIds = (initOrgs ?? []).map((o: any) => o.org_id);

    if (orgIds.length > 0) {
      const { data: members } = await db
        .from("org_members")
        .select("role")
        .in("org_id", orgIds)
        .in("role", ["validator", "moderator"]);

      const validatorCount = (members ?? []).filter((m: any) => m.role === "validator").length;
      const modCount = (members ?? []).filter((m: any) => m.role === "moderator").length;

      if (validatorCount < 3) {
        opps.push({
          type: "validator",
          initiative_id: init.id,
          initiative_title: init.title,
          description: `Validator needed for ${init.title}`,
          slots_available: 3 - validatorCount,
        });
      }
      if (modCount < 1) {
        opps.push({
          type: "moderator",
          initiative_id: init.id,
          initiative_title: init.title,
          description: `Moderator needed for ${init.title}`,
          slots_available: 1,
        });
      }
    } else {
      // No orgs linked — initiative needs validators
      opps.push({
        type: "validator",
        initiative_id: init.id,
        initiative_title: init.title,
        description: `Validator needed for ${init.title}`,
        slots_available: 3,
      });
    }
  }

  return json({ opportunities: opps.slice(0, 10) });
}

// ── GET /leaderboard — top contributors eligible for promotion ──
async function leaderboard(_req: Request) {
  const db = getServiceDb();

  const { data: topContributors } = await db
    .from("contributions")
    .select("user_id, score")
    .order("created_at", { ascending: false })
    .limit(500);

  // Aggregate by user
  const byUser: Record<string, { total: number; count: number }> = {};
  for (const c of topContributors ?? []) {
    if (!byUser[c.user_id]) byUser[c.user_id] = { total: 0, count: 0 };
    byUser[c.user_id].total += c.score || 0;
    byUser[c.user_id].count += 1;
  }

  const sorted = Object.entries(byUser)
    .map(([uid, stats]) => ({ user_id: uid, ...stats }))
    .sort((a, b) => b.total - a.total)
    .slice(0, 20);

  // Enrich with profiles
  const userIds = sorted.map((s) => s.user_id);
  if (userIds.length === 0) return json({ leaders: [] });

  const [{ data: profiles }, { data: roles }] = await Promise.all([
    db.from("profiles").select("id, display_name, avatar_url, status").in("id", userIds),
    db.from("user_roles").select("user_id, role").in("user_id", userIds),
  ]);

  const profileMap = Object.fromEntries((profiles ?? []).map((p: any) => [p.id, p]));
  const roleMap: Record<string, string[]> = {};
  for (const r of roles ?? []) {
    if (!roleMap[r.user_id]) roleMap[r.user_id] = [];
    roleMap[r.user_id].push(r.role);
  }

  const leaders = sorted.map((s) => {
    const profile = profileMap[s.user_id] || {};
    const userRoles = roleMap[s.user_id] || [];
    let tier = "volunteer";
    if (userRoles.includes("admin")) tier = "admin";
    else if (userRoles.includes("moderator")) tier = "moderator";
    else if (userRoles.includes("validator")) tier = "validator";
    else if (profile.status === "trusted") tier = "trusted";

    return {
      user_id: s.user_id,
      display_name: profile.display_name || "Volunteer",
      avatar_url: profile.avatar_url,
      tier,
      tier_label: ROLE_TIERS.find((t) => t.key === tier)?.label || "Volunteer",
      total_score: s.total,
      task_count: s.count,
    };
  });

  return json({ leaders });
}

// ── Router ──
Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  const url = new URL(req.url);
  const pathParts = url.pathname.replace(/^\/leadership\/?/, "").split("/").filter(Boolean);

  try {
    const route = pathParts[0] || "";

    if (req.method === "GET") {
      if (route === "my-progression") return await myProgression(req);
      if (route === "opportunities") return await opportunities(req);
      if (route === "leaderboard") return await leaderboard(req);
    }

    if (req.method === "POST") {
      if (route === "request-promotion") return await requestPromotion(req);
      if (route === "confirm-promotion") return await confirmPromotion(req);
    }

    return err("Not found", 404);
  } catch (e) {
    console.error("Leadership error:", e);
    return err("Internal server error", 500);
  }
});
