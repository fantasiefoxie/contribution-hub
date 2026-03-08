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

// ── GET / — list initiatives ──
async function listInitiatives(req: Request) {
  const supabase = getSupabase(req);
  const url = new URL(req.url);
  const status = url.searchParams.get("status") || "active";
  const isMicro = url.searchParams.get("is_micro");
  const limit = Math.min(parseInt(url.searchParams.get("limit") || "50"), 100);

  let query = supabase
    .from("initiatives")
    .select("*, initiative_participants(count)")
    .eq("status", status)
    .order("created_at", { ascending: false })
    .limit(limit);

  if (isMicro === "true") query = query.eq("is_micro", true);
  if (isMicro === "false") query = query.eq("is_micro", false);

  const { data, error } = await query;
  if (error) return err(error.message, 500);
  return json({ initiatives: data });
}

// ── GET /:id — get initiative with details ──
async function getInitiative(req: Request, id: string) {
  const supabase = getSupabase(req);
  const db = getServiceDb();

  const { data: initiative, error } = await supabase
    .from("initiatives")
    .select("*")
    .eq("id", id)
    .single();
  if (error || !initiative) return err("Initiative not found", 404);

  const [
    { data: projects },
    { data: participants },
    { count: taskCount },
    { data: taskStats },
  ] = await Promise.all([
    db.from("projects").select("*").eq("initiative_id", id).order("sort_order"),
    db.from("initiative_participants").select("user_id, joined_at").eq("initiative_id", id),
    db.from("tasks").select("*", { count: "exact", head: true }).eq("initiative_id", id),
    db.from("tasks").select("status").eq("initiative_id", id),
  ]);

  const statusCounts: Record<string, number> = {};
  for (const t of taskStats ?? []) {
    statusCounts[t.status] = (statusCounts[t.status] || 0) + 1;
  }

  return json({
    initiative,
    projects: projects ?? [],
    participants: participants ?? [],
    participant_count: (participants ?? []).length,
    task_count: taskCount ?? 0,
    task_status_counts: statusCounts,
  });
}

// ── POST / — create initiative ──
async function createInitiative(req: Request) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const body = await req.json();
  const { title, description, start_date, end_date, success_metrics, visibility, is_micro, target_metrics } = body;

  if (!title?.trim()) return err("Title is required");

  // Micro-initiative constraints
  if (is_micro) {
    if (end_date && start_date) {
      const days = (new Date(end_date).getTime() - new Date(start_date).getTime()) / 86400000;
      if (days > 30) return err("Micro-initiatives cannot exceed 30 days");
    }
  }

  const { data, error } = await supabase
    .from("initiatives")
    .insert({
      title: title.trim(),
      description: description?.trim() || null,
      start_date: start_date || null,
      end_date: end_date || null,
      success_metrics: success_metrics || {},
      target_metrics: target_metrics || {},
      status: "draft",
      is_micro: is_micro || false,
      max_tasks: is_micro ? 10 : null,
      max_volunteers: is_micro ? 25 : null,
      created_by: user.id,
    })
    .select()
    .single();

  if (error) return err(error.message, 500);

  // Auto-join creator
  await supabase.from("initiative_participants").insert({ initiative_id: data.id, user_id: user.id });

  await supabase.from("events").insert({
    type: "TaskCreated", // Closest available
    actor_id: user.id,
    entity_type: "initiative",
    entity_id: data.id,
    payload: { title: data.title, is_micro },
  });

  return json({ initiative: data }, 201);
}

// ── PUT /:id — update initiative ──
async function updateInitiative(req: Request, id: string) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const body = await req.json();
  const { data, error } = await supabase
    .from("initiatives")
    .update(body)
    .eq("id", id)
    .select()
    .single();

  if (error) return err(error.message, 500);
  return json({ initiative: data });
}

// ── POST /:id/projects — create project ──
async function createProject(req: Request, initiativeId: string) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const body = await req.json();
  const { title, description, sort_order } = body;
  if (!title?.trim()) return err("Title is required");

  const { data, error } = await supabase
    .from("projects")
    .insert({ initiative_id: initiativeId, title: title.trim(), description: description?.trim() || null, sort_order: sort_order || 0 })
    .select()
    .single();

  if (error) return err(error.message, 500);
  return json({ project: data }, 201);
}

// ── POST /:id/generate-tasks — bulk task generation ──
async function generateTasks(req: Request, initiativeId: string) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const db = getServiceDb();

  // Check initiative ownership
  const { data: initiative } = await db.from("initiatives").select("*").eq("id", initiativeId).single();
  if (!initiative) return err("Initiative not found", 404);
  if (initiative.created_by !== user.id) return err("Only the creator can generate tasks", 403);

  // Check micro limits
  if (initiative.is_micro && initiative.max_tasks) {
    const { count } = await db.from("tasks").select("*", { count: "exact", head: true }).eq("initiative_id", initiativeId);
    if ((count ?? 0) >= initiative.max_tasks) return err(`Micro-initiative limit: max ${initiative.max_tasks} tasks`);
  }

  const body = await req.json();
  const { template_title, description, difficulty, tags, project_id, count, variables } = body;

  if (!template_title?.trim()) return err("template_title is required");
  const taskCount = Math.min(count || 1, initiative.is_micro ? Math.max(0, (initiative.max_tasks || 10) - 0) : 50);

  const tasks = [];
  for (let i = 1; i <= taskCount; i++) {
    const vars = variables?.[i - 1] || {};
    let title = template_title;
    for (const [key, val] of Object.entries(vars)) {
      title = title.replace(`{{${key}}}`, String(val));
    }
    // Also replace {{number}} with index
    title = title.replace("{{number}}", String(i));

    tasks.push({
      title: title.trim(),
      description: description?.trim() || null,
      difficulty: difficulty || 1,
      tags: tags || [],
      initiative_id: initiativeId,
      project_id: project_id || null,
      org_id: initiative.created_by ? null : null, // Could link org
      created_by: user.id,
      status: "open",
      visibility: "public",
    });
  }

  const { data, error } = await supabase.from("tasks").insert(tasks).select();
  if (error) return err(error.message, 500);

  return json({ tasks: data, count: data?.length ?? 0 }, 201);
}

// ── POST /:id/join — join initiative ──
async function joinInitiative(req: Request, initiativeId: string) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const db = getServiceDb();

  // Check volunteer cap for micro-initiatives
  const { data: initiative } = await db.from("initiatives").select("max_volunteers, is_micro").eq("id", initiativeId).single();
  if (!initiative) return err("Initiative not found", 404);

  if (initiative.is_micro && initiative.max_volunteers) {
    const { count } = await db.from("initiative_participants").select("*", { count: "exact", head: true }).eq("initiative_id", initiativeId);
    if ((count ?? 0) >= initiative.max_volunteers) return err("This micro-initiative has reached its volunteer limit");
  }

  const { error } = await supabase
    .from("initiative_participants")
    .insert({ initiative_id: initiativeId, user_id: user.id });

  if (error) {
    if (error.code === "23505") return err("Already joined");
    return err(error.message, 500);
  }

  return json({ joined: true });
}

// ── POST /:id/promote — promote micro-initiative to full ──
async function promoteInitiative(req: Request, initiativeId: string) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const db = getServiceDb();
  const { data: initiative } = await db.from("initiatives").select("*").eq("id", initiativeId).single();
  if (!initiative) return err("Initiative not found", 404);
  if (!initiative.is_micro) return err("Only micro-initiatives can be promoted");

  const { error } = await db
    .from("initiatives")
    .update({
      is_micro: false,
      max_tasks: null,
      max_volunteers: null,
      promoted_by: user.id,
      promoted_at: new Date().toISOString(),
    })
    .eq("id", initiativeId);

  if (error) return err(error.message, 500);

  await supabase.from("audit_logs").insert({
    action: "initiative_promoted",
    actor_id: user.id,
    entity_type: "initiative",
    entity_id: initiativeId,
    details: { from: "micro", to: "full" },
  });

  return json({ promoted: true });
}

// ── Router ──
Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  const url = new URL(req.url);
  const pathParts = url.pathname.replace(/^\/initiative-engine\/?/, "").split("/").filter(Boolean);

  try {
    if (req.method === "GET" && pathParts.length === 0) return await listInitiatives(req);
    if (req.method === "POST" && pathParts.length === 0) return await createInitiative(req);

    if (pathParts.length >= 1) {
      const id = pathParts[0];

      if (req.method === "GET" && pathParts.length === 1) return await getInitiative(req, id);
      if (req.method === "PUT" && pathParts.length === 1) return await updateInitiative(req, id);

      if (req.method === "POST") {
        switch (pathParts[1]) {
          case "projects": return await createProject(req, id);
          case "generate-tasks": return await generateTasks(req, id);
          case "join": return await joinInitiative(req, id);
          case "promote": return await promoteInitiative(req, id);
        }
      }
    }

    return err("Not found", 404);
  } catch (e) {
    console.error("Initiative engine error:", e);
    return err("Internal server error", 500);
  }
});
