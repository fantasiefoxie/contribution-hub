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
function getUserSupabase(req: Request) {
  const authHeader = req.headers.get("Authorization") ?? "";
  return createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, {
    global: { headers: { Authorization: authHeader } },
  });
}
async function getUser(supabase: ReturnType<typeof createClient>) {
  const { data: { user } } = await supabase.auth.getUser();
  return user;
}

// ═══ Priority Score Calculation ═══
function calculatePriorityScore(initiative: any, taskStats: any, participantCount: number): number {
  let score = 0;

  // Task urgency: more open tasks = higher urgency
  const openTasks = taskStats.open ?? 0;
  const totalTasks = taskStats.total ?? 0;
  if (totalTasks > 0) {
    score += (openTasks / totalTasks) * 25; // up to 25 pts
  }

  // Volunteer participation gap
  const target = initiative.volunteer_target || 50;
  const gap = Math.max(0, target - participantCount);
  const gapRatio = gap / target;
  score += gapRatio * 25; // up to 25 pts for unfilled campaigns

  // Milestone proximity: close to next milestone = higher priority
  if (totalTasks > 0) {
    const completed = taskStats.validated ?? 0;
    const pct = (completed / totalTasks) * 100;
    const milestones = [25, 50, 75, 100];
    for (const m of milestones) {
      if (pct < m && pct >= m - 15) {
        score += 15; // near a milestone
        break;
      }
    }
  }

  // End date urgency
  if (initiative.end_date) {
    const daysLeft = (new Date(initiative.end_date).getTime() - Date.now()) / 86400000;
    if (daysLeft > 0 && daysLeft < 14) score += 20;
    else if (daysLeft >= 14 && daysLeft < 30) score += 10;
  }

  // Featured boost
  if (initiative.featured_at) score += 15;

  return Math.min(Math.round(score), 100);
}

// Haversine distance in km
function haversineDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

// ═══ GET /discover — Personalized discovery feed ═══
async function discover(req: Request) {
  const db = getServiceDb();
  const supabase = getUserSupabase(req);
  const user = await getUser(supabase);

  // Get user profile for personalization
  let userProfile: any = null;
  if (user) {
    const { data } = await db.from("profiles").select("*").eq("id", user.id).single();
    userProfile = data;
  }

  // Fetch all active initiatives with participant counts
  const { data: initiatives } = await db
    .from("initiatives")
    .select("*, initiative_participants(count)")
    .eq("status", "active")
    .order("created_at", { ascending: false })
    .limit(100);

  if (!initiatives || initiatives.length === 0) {
    return json({ sections: [], initiatives: [] });
  }

  // Fetch task stats for each initiative
  const initIds = initiatives.map((i: any) => i.id);
  const { data: allTasks } = await db
    .from("tasks")
    .select("initiative_id, status")
    .in("initiative_id", initIds);

  const taskStatsMap: Record<string, any> = {};
  for (const t of allTasks ?? []) {
    if (!taskStatsMap[t.initiative_id]) taskStatsMap[t.initiative_id] = { total: 0, open: 0, validated: 0 };
    taskStatsMap[t.initiative_id].total++;
    if (t.status === "open") taskStatsMap[t.initiative_id].open++;
    if (t.status === "validated") taskStatsMap[t.initiative_id].validated++;
  }

  // Calculate priority scores and enrich
  const enriched = initiatives.map((init: any) => {
    const participantCount = init.initiative_participants?.[0]?.count ?? 0;
    const stats = taskStatsMap[init.id] || { total: 0, open: 0, validated: 0 };
    const priority = calculatePriorityScore(init, stats, participantCount);
    const volunteerTarget = init.volunteer_target || Math.max(50, participantCount + 20);
    const completionPct = stats.total > 0 ? Math.round((stats.validated / stats.total) * 100) : 0;

    // Lifecycle state
    let lifecycle = "active";
    if (init.start_date && new Date(init.start_date) > new Date()) lifecycle = "upcoming";
    else if (completionPct >= 100) lifecycle = "completed";
    else if (completionPct >= 75) lifecycle = "nearing_completion";

    return {
      ...init,
      priority_score: priority,
      participant_count: participantCount,
      volunteer_target: volunteerTarget,
      volunteer_gap: Math.max(0, volunteerTarget - participantCount),
      task_stats: stats,
      completion_pct: completionPct,
      lifecycle,
      initiative_participants: undefined,
    };
  });

  // ── Build sections ──
  const sections: any[] = [];

  // 1. Featured
  const featured = enriched.filter((i: any) => i.featured_at).sort((a: any, b: any) => new Date(b.featured_at).getTime() - new Date(a.featured_at).getTime());
  if (featured.length > 0) {
    sections.push({ key: "featured", title: "Featured Campaigns", icon: "star", items: featured.slice(0, 5) });
  }

  // 2. Personalized recommendations
  if (userProfile) {
    const recommended = enriched
      .filter((i: any) => !i.featured_at)
      .map((i: any) => {
        let relevance = i.priority_score;

        // Interest match
        if (userProfile.interests?.length && i.category) {
          if (userProfile.interests.includes(i.category)) relevance += 20;
        }

        // Location proximity
        if (userProfile.latitude && userProfile.longitude && i.latitude && i.longitude) {
          const dist = haversineDistance(userProfile.latitude, userProfile.longitude, i.latitude, i.longitude);
          if (dist < 10) relevance += 25;
          else if (dist < 50) relevance += 15;
          else if (dist < 100) relevance += 5;
        }

        return { ...i, relevance };
      })
      .sort((a: any, b: any) => b.relevance - a.relevance);

    if (recommended.length > 0) {
      sections.push({ key: "recommended", title: "Recommended For You", icon: "sparkles", items: recommended.slice(0, 6) });
    }
  }

  // 3. Nearby (if user has location)
  if (userProfile?.latitude && userProfile?.longitude) {
    const nearby = enriched
      .filter((i: any) => i.latitude && i.longitude && !i.is_online)
      .map((i: any) => ({
        ...i,
        distance_km: haversineDistance(userProfile.latitude, userProfile.longitude, i.latitude, i.longitude),
      }))
      .filter((i: any) => i.distance_km < 100)
      .sort((a: any, b: any) => a.distance_km - b.distance_km);

    if (nearby.length > 0) {
      sections.push({ key: "nearby", title: "Nearby Campaigns", icon: "map-pin", items: nearby.slice(0, 6) });
    }
  }

  // 4. High priority
  const highPriority = [...enriched].sort((a: any, b: any) => b.priority_score - a.priority_score).slice(0, 6);
  if (highPriority.length > 0) {
    sections.push({ key: "high_priority", title: "High Priority", icon: "trending-up", items: highPriority });
  }

  // 5. Community campaigns (micro-initiatives)
  const community = enriched.filter((i: any) => i.is_micro).sort((a: any, b: any) => b.priority_score - a.priority_score);
  if (community.length > 0) {
    sections.push({ key: "community", title: "Community Campaigns", icon: "heart", items: community.slice(0, 6) });
  }

  // 6. Online missions
  const online = enriched.filter((i: any) => i.is_online).sort((a: any, b: any) => b.priority_score - a.priority_score);
  if (online.length > 0) {
    sections.push({ key: "online", title: "Online Missions", icon: "globe", items: online.slice(0, 6) });
  }

  return json({ sections, all: enriched.sort((a: any, b: any) => b.priority_score - a.priority_score) });
}

// ═══ GET /regions/:initiativeId — Regional board for an initiative ═══
async function getRegionalBoard(req: Request, initiativeId: string) {
  const db = getServiceDb();

  const { data: regions } = await db
    .from("regions")
    .select("*")
    .eq("initiative_id", initiativeId)
    .order("sort_order");

  if (!regions || regions.length === 0) return json({ regions: [], tasks_by_region: {} });

  const regionIds = regions.map((r: any) => r.id);
  const { data: tasks } = await db
    .from("tasks")
    .select("id, title, status, difficulty, tags, region_id")
    .eq("initiative_id", initiativeId)
    .in("region_id", regionIds);

  const tasksByRegion: Record<string, any[]> = {};
  for (const r of regions) tasksByRegion[r.id] = [];
  for (const t of tasks ?? []) {
    if (t.region_id && tasksByRegion[t.region_id]) {
      tasksByRegion[t.region_id].push(t);
    }
  }

  // Add stats per region
  const enrichedRegions = regions.map((r: any) => {
    const regionTasks = tasksByRegion[r.id] || [];
    return {
      ...r,
      task_count: regionTasks.length,
      open_tasks: regionTasks.filter((t: any) => t.status === "open").length,
      completed_tasks: regionTasks.filter((t: any) => t.status === "validated").length,
    };
  });

  return json({ regions: enrichedRegions, tasks_by_region: tasksByRegion });
}

// ═══ POST /recalculate — Recalculate priority scores for all active initiatives ═══
async function recalculateScores() {
  const db = getServiceDb();

  const { data: initiatives } = await db
    .from("initiatives")
    .select("*, initiative_participants(count)")
    .eq("status", "active");

  if (!initiatives) return json({ updated: 0 });

  const initIds = initiatives.map((i: any) => i.id);
  const { data: allTasks } = await db.from("tasks").select("initiative_id, status").in("initiative_id", initIds);

  const taskStatsMap: Record<string, any> = {};
  for (const t of allTasks ?? []) {
    if (!taskStatsMap[t.initiative_id]) taskStatsMap[t.initiative_id] = { total: 0, open: 0, validated: 0 };
    taskStatsMap[t.initiative_id].total++;
    if (t.status === "open") taskStatsMap[t.initiative_id].open++;
    if (t.status === "validated") taskStatsMap[t.initiative_id].validated++;
  }

  let updated = 0;
  for (const init of initiatives) {
    const participantCount = init.initiative_participants?.[0]?.count ?? 0;
    const stats = taskStatsMap[init.id] || { total: 0, open: 0, validated: 0 };
    const score = calculatePriorityScore(init, stats, participantCount);

    await db.from("initiatives").update({ priority_score: score }).eq("id", init.id);
    updated++;
  }

  return json({ updated });
}

// ═══ Router ═══
Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  const url = new URL(req.url);
  const parts = url.pathname.replace(/^\/campaign-discovery\/?/, "").split("/").filter(Boolean);

  try {
    if (req.method === "GET" && (parts.length === 0 || parts[0] === "discover")) return await discover(req);
    if (req.method === "GET" && parts[0] === "regions" && parts[1]) return await getRegionalBoard(req, parts[1]);
    if (req.method === "POST" && parts[0] === "recalculate") return await recalculateScores();

    return err("Not found", 404);
  } catch (e) {
    console.error("Campaign discovery error:", e);
    return err("Internal server error", 500);
  }
});
