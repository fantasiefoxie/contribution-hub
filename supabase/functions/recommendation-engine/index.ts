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

// ── Simple in-memory cache ──
const cache = new Map<string, { data: unknown; expires: number }>();
function getCached(key: string): unknown | null {
  const entry = cache.get(key);
  if (!entry || Date.now() > entry.expires) { cache.delete(key); return null; }
  return entry.data;
}
function setCache(key: string, data: unknown, ttlMs = 5000) {
  cache.set(key, { data, expires: Date.now() + ttlMs });
}

// ── Haversine distance in km ──
function haversine(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

// ── Scoring ──
interface TaskRow {
  id: string;
  title: string;
  description: string | null;
  difficulty: number;
  tags: string[];
  status: string;
  latitude: number | null;
  longitude: number | null;
  is_priority: boolean;
  expires_at: string | null;
  initiative_id: string | null;
  created_at: string;
  visibility: string;
}

interface UserProfile {
  interests: string[];
  latitude: number | null;
  longitude: number | null;
  status: string;
}

function scoreTask(
  task: TaskRow,
  profile: UserProfile | null,
  completedCount: number,
): { score: number; distance: number | null; reasons: string[] } {
  let score = 0;
  let distance: number | null = null;
  const reasons: string[] = [];

  // Location proximity (0-30 pts)
  if (profile?.latitude && profile?.longitude && task.latitude && task.longitude) {
    distance = haversine(profile.latitude, profile.longitude, task.latitude, task.longitude);
    if (distance <= 5) { score += 30; reasons.push("nearby"); }
    else if (distance <= 20) { score += 15; reasons.push("medium_distance"); }
    else { score += 5; }
  } else if (!task.latitude && !task.longitude) {
    // Online/remote task
    score += 10;
    reasons.push("online");
  }

  // Interest match (0-25 pts)
  if (profile?.interests?.length && task.tags?.length) {
    const matches = task.tags.filter(t => profile.interests.includes(t)).length;
    const matchScore = Math.min(matches * 10, 25);
    score += matchScore;
    if (matches > 0) reasons.push("interest_match");
  }

  // Difficulty match (0-20 pts)
  const userLevel = completedCount >= 20 ? 4 : completedCount >= 10 ? 3 : completedCount >= 3 ? 2 : 1;
  const diffDelta = Math.abs(task.difficulty - userLevel);
  if (diffDelta === 0) { score += 20; reasons.push("difficulty_match"); }
  else if (diffDelta === 1) { score += 12; }
  else { score += 5; }

  // Initiative priority (0-15 pts)
  if (task.is_priority) { score += 15; reasons.push("priority"); }

  // Urgency (0-10 pts)
  if (task.expires_at) {
    const hoursLeft = (new Date(task.expires_at).getTime() - Date.now()) / 3600000;
    if (hoursLeft > 0 && hoursLeft < 24) { score += 10; reasons.push("urgent"); }
    else if (hoursLeft > 0 && hoursLeft < 72) { score += 5; reasons.push("expiring_soon"); }
  }

  return { score, distance, reasons };
}

// ── GET /feed — marketplace feed ──
async function getMarketplaceFeed(req: Request) {
  const url = new URL(req.url);
  const passport = url.searchParams.get("passport") === "true";
  const limit = Math.min(parseInt(url.searchParams.get("limit") || "40"), 100);

  const cacheKey = `feed:${passport}:${limit}`;
  const cached = getCached(cacheKey + ":anon");

  const supabase = getSupabase(req);
  const db = getServiceDb();
  const user = await getUser(supabase);

  // Get user profile + contribution history
  let profile: UserProfile | null = null;
  let completedCount = 0;

  if (user) {
    const [{ data: p }, { count }] = await Promise.all([
      db.from("profiles").select("interests, latitude, longitude, status").eq("id", user.id).single(),
      db.from("contributions").select("*", { count: "exact", head: true }).eq("user_id", user.id),
    ]);
    profile = p as UserProfile | null;
    completedCount = count ?? 0;
  }

  // Fetch open tasks
  let query = db
    .from("tasks")
    .select("id, title, description, difficulty, tags, status, latitude, longitude, is_priority, expires_at, initiative_id, created_at, visibility")
    .eq("status", "open")
    .eq("visibility", "public")
    .order("created_at", { ascending: false })
    .limit(limit);

  const { data: tasks, error } = await query;
  if (error) return err(error.message, 500);
  if (!tasks?.length) return json({ recommended: [], nearby: [], priority: [], online: [] });

  // Score and sort
  const scored = (tasks as TaskRow[]).map(task => {
    const { score, distance, reasons } = scoreTask(task, profile, completedCount);
    return { ...task, _score: score, _distance: distance, _reasons: reasons };
  });

  scored.sort((a, b) => b._score - a._score);

  // Categorize
  const recommended = scored.slice(0, 10);
  const nearby = scored.filter(t => t._distance !== null && t._distance <= 20).slice(0, 10);
  const priority = scored.filter(t => t.is_priority).slice(0, 10);
  const online = scored.filter(t => !t.latitude && !t.longitude).slice(0, 10);

  const result = { recommended, nearby, priority, online };

  if (!user) setCache(cacheKey + ":anon", result, 5000);

  return json(result);
}

// ── GET /next-mission — suggest next task after completion ──
async function getNextMission(req: Request) {
  const url = new URL(req.url);
  const lastTaskId = url.searchParams.get("last_task_id");

  const supabase = getSupabase(req);
  const db = getServiceDb();
  const user = await getUser(supabase);

  let lastTask: TaskRow | null = null;
  if (lastTaskId) {
    const { data } = await db.from("tasks").select("*").eq("id", lastTaskId).single();
    lastTask = data as TaskRow | null;
  }

  let profile: UserProfile | null = null;
  let completedCount = 0;
  if (user) {
    const [{ data: p }, { count }] = await Promise.all([
      db.from("profiles").select("interests, latitude, longitude, status").eq("id", user.id).single(),
      db.from("contributions").select("*", { count: "exact", head: true }).eq("user_id", user.id),
    ]);
    profile = p as UserProfile | null;
    completedCount = count ?? 0;
  }

  // Open tasks, preferring same initiative
  let query = db
    .from("tasks")
    .select("id, title, description, difficulty, tags, status, latitude, longitude, is_priority, expires_at, initiative_id, created_at, visibility")
    .eq("status", "open")
    .eq("visibility", "public")
    .limit(30);

  if (lastTask?.initiative_id) {
    query = query.eq("initiative_id", lastTask.initiative_id);
  }

  const { data: tasks } = await query;
  if (!tasks?.length) return json({ suggestion: null });

  const scored = (tasks as TaskRow[]).map(task => {
    const { score, distance } = scoreTask(task, profile, completedCount);
    // Bonus for tag overlap with last task
    let tagBonus = 0;
    if (lastTask?.tags?.length && task.tags?.length) {
      tagBonus = task.tags.filter(t => lastTask!.tags.includes(t)).length * 8;
    }
    return { ...task, _score: score + tagBonus, _distance: distance };
  });

  scored.sort((a, b) => b._score - a._score);

  return json({ suggestion: scored[0] || null });
}

// ── PUT /interests — update user interests ──
async function updateInterests(req: Request) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const body = await req.json();
  const { interests } = body;
  if (!Array.isArray(interests)) return err("interests must be an array");

  const cleaned = interests.map((i: string) => i.trim().toLowerCase()).filter(Boolean).slice(0, 20);

  const { error } = await supabase
    .from("profiles")
    .update({ interests: cleaned })
    .eq("id", user.id);

  if (error) return err(error.message, 500);
  return json({ interests: cleaned });
}

// ── GET /interests — get user interests ──
async function getInterests(req: Request) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const db = getServiceDb();
  const { data, error } = await db.from("profiles").select("interests").eq("id", user.id).single();
  if (error) return err(error.message, 500);
  return json({ interests: data?.interests ?? [] });
}

// ── Router ──
Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  const url = new URL(req.url);
  const path = url.pathname.replace(/^\/recommendation-engine\/?/, "").replace(/^\/+/, "");

  try {
    if (req.method === "GET" && (path === "feed" || path === "")) return await getMarketplaceFeed(req);
    if (req.method === "GET" && path === "next-mission") return await getNextMission(req);
    if (req.method === "GET" && path === "interests") return await getInterests(req);
    if (req.method === "PUT" && path === "interests") return await updateInterests(req);

    return err("Not found", 404);
  } catch (e) {
    console.error("Recommendation engine error:", e);
    return err("Internal server error", 500);
  }
});
