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

async function getUser(supabase: ReturnType<typeof createClient>) {
  const { data: { user }, error } = await supabase.auth.getUser();
  if (error || !user) return null;
  return user;
}

async function emitEvent(
  supabase: ReturnType<typeof createClient>,
  type: string,
  actorId: string,
  entityType: string,
  entityId: string,
  payload: Record<string, unknown> = {}
) {
  await supabase.from("events").insert({
    type,
    actor_id: actorId,
    entity_type: entityType,
    entity_id: entityId,
    payload,
  });
}

// ── Route: GET /tasks ──
async function listTasks(req: Request) {
  const supabase = getSupabase(req);
  const url = new URL(req.url);
  const status = url.searchParams.get("status");
  const visibility = url.searchParams.get("visibility") || "public";
  const limit = Math.min(parseInt(url.searchParams.get("limit") || "50"), 100);
  const offset = parseInt(url.searchParams.get("offset") || "0");

  // Expire stale reservations first
  await supabase.rpc("expire_reservations");

  let query = supabase
    .from("tasks")
    .select("*")
    .eq("visibility", visibility)
    .order("created_at", { ascending: false })
    .range(offset, offset + limit - 1);

  if (status) query = query.eq("status", status);

  const { data, error } = await query;
  if (error) return err(error.message, 500);
  return json({ tasks: data, count: data?.length ?? 0 });
}

// ── Route: POST /tasks ──
async function createTask(req: Request) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const body = await req.json();
  const { title, description, difficulty, visibility, tags, org_id, initiative_id, lock_duration_mins, expires_at } = body;

  if (!title || typeof title !== "string" || title.trim().length === 0) {
    return err("Title is required");
  }
  if (difficulty !== undefined && (difficulty < 1 || difficulty > 5)) {
    return err("Difficulty must be between 1 and 5");
  }

  const { data, error } = await supabase
    .from("tasks")
    .insert({
      title: title.trim(),
      description: description?.trim() || null,
      difficulty: difficulty || 1,
      visibility: visibility || "public",
      tags: tags || [],
      org_id: org_id || null,
      initiative_id: initiative_id || null,
      lock_duration_mins: lock_duration_mins || 60,
      expires_at: expires_at || null,
      created_by: user.id,
      status: "open",
    })
    .select()
    .single();

  if (error) return err(error.message, 500);

  await emitEvent(supabase, "TaskCreated", user.id, "task", data.id, { title: data.title });
  return json({ task: data }, 201);
}

// ── Route: POST /tasks/:id/reserve ──
async function reserveTask(req: Request, taskId: string) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  // Expire stale reservations
  await supabase.rpc("expire_reservations");

  // Check task availability
  const { data: task, error: fetchErr } = await supabase
    .from("tasks")
    .select("*")
    .eq("id", taskId)
    .single();

  if (fetchErr || !task) return err("Task not found", 404);
  if (task.status !== "open") return err(`Task is not open (current: ${task.status})`);
  if (task.created_by === user.id) return err("Cannot reserve your own task");

  // 60-second reservation window
  const reservedUntil = new Date(Date.now() + 60 * 1000).toISOString();

  const { data: updated, error: updateErr } = await supabase
    .from("tasks")
    .update({
      status: "reserved",
      reserved_by: user.id,
      reserved_until: reservedUntil,
    })
    .eq("id", taskId)
    .eq("status", "open") // optimistic lock
    .select()
    .single();

  if (updateErr || !updated) return err("Task was claimed by someone else", 409);

  await emitEvent(supabase, "TaskReserved", user.id, "task", taskId, { reserved_until: reservedUntil });
  return json({ task: updated, reserved_until: reservedUntil });
}

// ── Route: POST /tasks/:id/confirm-claim ──
async function confirmClaim(req: Request, taskId: string) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const { data: task, error: fetchErr } = await supabase
    .from("tasks")
    .select("*")
    .eq("id", taskId)
    .single();

  if (fetchErr || !task) return err("Task not found", 404);
  if (task.status !== "reserved") return err("Task is not reserved");
  if (task.reserved_by !== user.id) return err("Reservation belongs to another user", 403);

  // Check reservation hasn't expired
  if (new Date(task.reserved_until!) < new Date()) {
    // Auto-expire
    await supabase.from("tasks").update({ status: "open", reserved_by: null, reserved_until: null }).eq("id", taskId);
    return err("Reservation expired");
  }

  const lockedUntil = new Date(Date.now() + task.lock_duration_mins * 60 * 1000).toISOString();

  const { data: updated, error: updateErr } = await supabase
    .from("tasks")
    .update({
      status: "claimed",
      locked_by: user.id,
      locked_until: lockedUntil,
      reserved_by: null,
      reserved_until: null,
    })
    .eq("id", taskId)
    .eq("reserved_by", user.id) // optimistic lock
    .select()
    .single();

  if (updateErr || !updated) return err("Failed to confirm claim", 500);

  // Create task_claims record
  await supabase.from("task_claims").insert({
    task_id: taskId,
    user_id: user.id,
    expires_at: lockedUntil,
  });

  await emitEvent(supabase, "TaskClaimed", user.id, "task", taskId, { locked_until: lockedUntil });
  return json({ task: updated });
}

// ── Route: POST /tasks/:id/updates ──
async function addTaskUpdate(req: Request, taskId: string) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const { data: task } = await supabase.from("tasks").select("*").eq("id", taskId).single();
  if (!task) return err("Task not found", 404);
  if (task.locked_by !== user.id) return err("You are not the assigned volunteer", 403);
  if (!["claimed", "in_progress"].includes(task.status)) return err("Task must be claimed or in progress");

  const body = await req.json();
  const { notes, attachments } = body;

  if (!notes && (!attachments || attachments.length === 0)) {
    return err("Notes or attachments required");
  }

  const { data: update, error } = await supabase
    .from("task_updates")
    .insert({
      task_id: taskId,
      user_id: user.id,
      notes: notes?.trim() || null,
      attachments: attachments || [],
    })
    .select()
    .single();

  if (error) return err(error.message, 500);

  // Move task to in_progress if claimed
  if (task.status === "claimed") {
    await supabase.from("tasks").update({ status: "in_progress" }).eq("id", taskId);
  }

  await emitEvent(supabase, "TaskProgressUpdated", user.id, "task", taskId, { update_id: update.id });
  return json({ update }, 201);
}

// ── Route: POST /tasks/:id/submit ──
async function submitTask(req: Request, taskId: string) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const { data: task } = await supabase.from("tasks").select("*").eq("id", taskId).single();
  if (!task) return err("Task not found", 404);
  if (task.locked_by !== user.id) return err("You are not the assigned volunteer", 403);
  if (!["claimed", "in_progress"].includes(task.status)) return err("Task must be claimed or in progress");

  const body = await req.json();
  const { content, attachments } = body;

  if (!content || typeof content !== "string" || content.trim().length === 0) {
    return err("Submission content is required");
  }

  const { data: submission, error } = await supabase
    .from("task_submissions")
    .insert({
      task_id: taskId,
      user_id: user.id,
      content: content.trim(),
      attachments: attachments || [],
    })
    .select()
    .single();

  if (error) return err(error.message, 500);

  await supabase.from("tasks").update({ status: "validation_pending" }).eq("id", taskId);

  await emitEvent(supabase, "TaskSubmitted", user.id, "task", taskId, { submission_id: submission.id });
  return json({ submission }, 201);
}

// ── Route: GET /tasks/:id/submissions ──
async function getSubmissions(req: Request, taskId: string) {
  const supabase = getSupabase(req);

  const { data, error } = await supabase
    .from("task_submissions")
    .select("*, validations(*)")
    .eq("task_id", taskId)
    .order("submitted_at", { ascending: false });

  if (error) return err(error.message, 500);
  return json({ submissions: data });
}

// ── Route: GET /tasks/:id ──
async function getTask(req: Request, taskId: string) {
  const supabase = getSupabase(req);

  const { data: task, error } = await supabase
    .from("tasks")
    .select("*, task_updates(*), task_submissions(*)")
    .eq("id", taskId)
    .single();

  if (error || !task) return err("Task not found", 404);
  return json({ task });
}

// ── Router ──
Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  const url = new URL(req.url);
  // Path after /task-engine, e.g. /task-engine/abc-123/reserve
  const pathParts = url.pathname.replace(/^\/task-engine\/?/, "").split("/").filter(Boolean);

  try {
    // POST /task-engine (create task)
    if (req.method === "POST" && pathParts.length === 0) {
      return await createTask(req);
    }

    // GET /task-engine (list tasks)
    if (req.method === "GET" && pathParts.length === 0) {
      return await listTasks(req);
    }

    // GET /task-engine/:id
    if (req.method === "GET" && pathParts.length === 1) {
      return await getTask(req, pathParts[0]);
    }

    const taskId = pathParts[0];
    const action = pathParts[1];

    if (!taskId) return err("Task ID required", 400);

    if (req.method === "POST") {
      switch (action) {
        case "reserve":
          return await reserveTask(req, taskId);
        case "confirm-claim":
          return await confirmClaim(req, taskId);
        case "updates":
          return await addTaskUpdate(req, taskId);
        case "submit":
          return await submitTask(req, taskId);
        default:
          return err("Unknown action", 404);
      }
    }

    if (req.method === "GET") {
      switch (action) {
        case "submissions":
          return await getSubmissions(req, taskId);
        default:
          return err("Unknown action", 404);
      }
    }

    return err("Method not allowed", 405);
  } catch (e) {
    console.error("Task engine error:", e);
    return err("Internal server error", 500);
  }
});
