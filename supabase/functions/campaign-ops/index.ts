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

// ── Rate limiting check ──
async function checkRateLimit(db: ReturnType<typeof createClient>, userId: string): Promise<boolean> {
  const fiveSecondsAgo = new Date(Date.now() - 5000).toISOString();
  const { count } = await db
    .from("claim_attempts")
    .select("*", { count: "exact", head: true })
    .eq("user_id", userId)
    .gte("attempted_at", fiveSecondsAgo);
  return (count ?? 0) === 0;
}

async function recordClaimAttempt(supabase: ReturnType<typeof createClient>, userId: string) {
  await supabase.from("claim_attempts").insert({ user_id: userId });
}

// ── Capacity check ──
async function checkCapacity(db: ReturnType<typeof createClient>, userId: string): Promise<{ allowed: boolean; active: number; max: number }> {
  const [{ data: profile }, { count }] = await Promise.all([
    db.from("profiles").select("max_active_tasks").eq("id", userId).single(),
    db.from("tasks").select("*", { count: "exact", head: true })
      .eq("locked_by", userId)
      .in("status", ["claimed", "in_progress"]),
  ]);
  const max = profile?.max_active_tasks ?? 1;
  const active = count ?? 0;
  return { allowed: active < max, active, max };
}

// ── GET /dashboard/:initiativeId ──
async function getDashboard(_req: Request, initiativeId: string) {
  const db = getServiceDb();

  const [
    { data: initiative },
    { data: regions },
    { data: tasks },
    { data: participants },
    { data: teams },
  ] = await Promise.all([
    db.from("initiatives").select("*").eq("id", initiativeId).single(),
    db.from("regions").select("*").eq("initiative_id", initiativeId).order("sort_order"),
    db.from("tasks").select("id, status, region_id, task_pool, is_team, created_at").eq("initiative_id", initiativeId),
    db.from("initiative_participants").select("user_id, joined_at").eq("initiative_id", initiativeId),
    db.from("team_members").select("id, task_id, user_id, region_id")
      .in("task_id", (await db.from("tasks").select("id").eq("initiative_id", initiativeId)).data?.map((t: any) => t.id) ?? []),
  ]);

  if (!initiative) return err("Initiative not found", 404);

  const allTasks = tasks ?? [];
  const today = new Date().toISOString().split("T")[0];

  // Status counts
  const statusCounts: Record<string, number> = {};
  let completedToday = 0;
  for (const t of allTasks) {
    statusCounts[t.status] = (statusCounts[t.status] || 0) + 1;
    if (t.status === "validated" && t.created_at?.startsWith(today)) completedToday++;
  }

  // Regional breakdown
  const regionStats = (regions ?? []).map((r: any) => {
    const regionTasks = allTasks.filter(t => t.region_id === r.id);
    const completed = regionTasks.filter(t => t.status === "validated").length;
    return {
      ...r,
      total_tasks: regionTasks.length,
      completed_tasks: completed,
      active_tasks: regionTasks.filter(t => ["claimed", "in_progress"].includes(t.status)).length,
      open_tasks: regionTasks.filter(t => t.status === "open").length,
      progress_pct: regionTasks.length > 0 ? Math.round((completed / regionTasks.length) * 100) : 0,
    };
  });

  // Pool breakdown
  const poolCounts: Record<string, number> = {};
  for (const t of allTasks) {
    poolCounts[t.task_pool] = (poolCounts[t.task_pool] || 0) + 1;
  }

  // Team pods
  const teamTaskIds = [...new Set((teams ?? []).map((t: any) => t.task_id))];
  const teamPods = teamTaskIds.map(taskId => {
    const podMembers = (teams ?? []).filter((t: any) => t.task_id === taskId);
    return { task_id: taskId, member_count: podMembers.length };
  });

  return json({
    initiative,
    active_volunteers: (participants ?? []).length,
    total_tasks: allTasks.length,
    completed_today: completedToday,
    status_counts: statusCounts,
    region_stats: regionStats,
    pool_counts: poolCounts,
    team_pods: teamPods,
    unassigned_tasks: allTasks.filter(t => !t.region_id).length,
  });
}

// ── POST /regions ──
async function createRegion(req: Request) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const body = await req.json();
  const { initiative_id, name, description, sort_order } = body;
  if (!initiative_id || !name?.trim()) return err("initiative_id and name required");

  const { data, error } = await supabase
    .from("regions")
    .insert({ initiative_id, name: name.trim(), description: description?.trim() || null, sort_order: sort_order || 0 })
    .select()
    .single();

  if (error) return err(error.message, 500);
  return json({ region: data }, 201);
}

// ── GET /regions/:initiativeId ──
async function listRegions(_req: Request, initiativeId: string) {
  const db = getServiceDb();
  const { data, error } = await db.from("regions").select("*").eq("initiative_id", initiativeId).order("sort_order");
  if (error) return err(error.message, 500);
  return json({ regions: data });
}

// ── GET /capacity-check ──
async function capacityCheck(req: Request) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const db = getServiceDb();
  const result = await checkCapacity(db, user.id);
  return json(result);
}

// ── GET /rate-check ──
async function rateCheck(req: Request) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const db = getServiceDb();
  const allowed = await checkRateLimit(db, user.id);
  return json({ allowed, cooldown_seconds: allowed ? 0 : 5 });
}

// ── GET /regional-feed ──
async function regionalFeed(req: Request) {
  const url = new URL(req.url);
  const regionId = url.searchParams.get("region_id");
  const pool = url.searchParams.get("pool");
  const initiativeId = url.searchParams.get("initiative_id");
  const limit = Math.min(parseInt(url.searchParams.get("limit") || "30"), 100);

  const db = getServiceDb();
  let query = db
    .from("tasks")
    .select("id, title, description, difficulty, tags, status, region_id, task_pool, is_team, team_target, created_at")
    .eq("status", "open")
    .eq("visibility", "public")
    .order("created_at", { ascending: false })
    .limit(limit);

  if (regionId) query = query.eq("region_id", regionId);
  if (pool) query = query.eq("task_pool", pool);
  if (initiativeId) query = query.eq("initiative_id", initiativeId);

  const { data, error } = await query;
  if (error) return err(error.message, 500);

  // Group by pool
  const grouped: Record<string, any[]> = { regional: [], team: [], public: [], online: [] };
  for (const t of data ?? []) {
    const p = t.task_pool || "public";
    if (!grouped[p]) grouped[p] = [];
    grouped[p].push(t);
  }

  return json({ tasks: data, grouped, total: data?.length ?? 0 });
}

// ── Router ──
Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  const url = new URL(req.url);
  const path = url.pathname.replace(/^\/campaign-ops\/?/, "").replace(/^\/+/, "");
  const parts = path.split("/").filter(Boolean);

  try {
    if (req.method === "GET" && parts[0] === "dashboard" && parts[1]) return await getDashboard(req, parts[1]);
    if (req.method === "GET" && parts[0] === "regions" && parts[1]) return await listRegions(req, parts[1]);
    if (req.method === "POST" && parts[0] === "regions") return await createRegion(req);
    if (req.method === "GET" && parts[0] === "capacity-check") return await capacityCheck(req);
    if (req.method === "GET" && parts[0] === "rate-check") return await rateCheck(req);
    if (req.method === "GET" && parts[0] === "feed") return await regionalFeed(req);

    return err("Not found", 404);
  } catch (e) {
    console.error("Campaign ops error:", e);
    return err("Internal server error", 500);
  }
});
