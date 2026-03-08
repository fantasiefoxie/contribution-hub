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

function getServiceSupabase() {
  return createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
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
    type, actor_id: actorId, entity_type: entityType, entity_id: entityId, payload,
  });
}

async function auditLog(
  supabase: ReturnType<typeof createClient>,
  action: string,
  actorId: string,
  entityType: string,
  entityId: string,
  details: Record<string, unknown> = {}
) {
  await supabase.from("audit_logs").insert({
    action, actor_id: actorId, entity_type: entityType, entity_id: entityId, details,
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

  if (!title || typeof title !== "string" || title.trim().length === 0) return err("Title is required");
  if (difficulty !== undefined && (difficulty < 1 || difficulty > 5)) return err("Difficulty must be between 1 and 5");

  const { data, error } = await supabase
    .from("tasks")
    .insert({
      title: title.trim(), description: description?.trim() || null,
      difficulty: difficulty || 1, visibility: visibility || "public",
      tags: tags || [], org_id: org_id || null, initiative_id: initiative_id || null,
      lock_duration_mins: lock_duration_mins || 60, expires_at: expires_at || null,
      created_by: user.id, status: "open",
    })
    .select().single();

  if (error) return err(error.message, 500);
  await emitEvent(supabase, "TaskCreated", user.id, "task", data.id, { title: data.title });
  return json({ task: data }, 201);
}

// ── Route: POST /tasks/:id/reserve ──
async function reserveTask(req: Request, taskId: string) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  await supabase.rpc("expire_reservations");

  const { data: task, error: fetchErr } = await supabase.from("tasks").select("*").eq("id", taskId).single();
  if (fetchErr || !task) return err("Task not found", 404);
  if (task.status !== "open") return err(`Task is not open (current: ${task.status})`);
  if (task.created_by === user.id) return err("Cannot reserve your own task");

  const reservedUntil = new Date(Date.now() + 60 * 1000).toISOString();
  const { data: updated, error: updateErr } = await supabase
    .from("tasks")
    .update({ status: "reserved", reserved_by: user.id, reserved_until: reservedUntil })
    .eq("id", taskId).eq("status", "open").select().single();

  if (updateErr || !updated) return err("Task was claimed by someone else", 409);
  await emitEvent(supabase, "TaskReserved", user.id, "task", taskId, { reserved_until: reservedUntil });
  return json({ task: updated, reserved_until: reservedUntil });
}

// ── Route: POST /tasks/:id/confirm-claim ──
async function confirmClaim(req: Request, taskId: string) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const { data: task, error: fetchErr } = await supabase.from("tasks").select("*").eq("id", taskId).single();
  if (fetchErr || !task) return err("Task not found", 404);
  if (task.status !== "reserved") return err("Task is not reserved");
  if (task.reserved_by !== user.id) return err("Reservation belongs to another user", 403);

  if (new Date(task.reserved_until!) < new Date()) {
    await supabase.from("tasks").update({ status: "open", reserved_by: null, reserved_until: null }).eq("id", taskId);
    return err("Reservation expired");
  }

  const lockedUntil = new Date(Date.now() + task.lock_duration_mins * 60 * 1000).toISOString();
  const { data: updated, error: updateErr } = await supabase
    .from("tasks")
    .update({ status: "claimed", locked_by: user.id, locked_until: lockedUntil, reserved_by: null, reserved_until: null })
    .eq("id", taskId).eq("reserved_by", user.id).select().single();

  if (updateErr || !updated) return err("Failed to confirm claim", 500);
  await supabase.from("task_claims").insert({ task_id: taskId, user_id: user.id, expires_at: lockedUntil });
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
  if (!notes && (!attachments || attachments.length === 0)) return err("Notes or attachments required");

  const { data: update, error } = await supabase
    .from("task_updates")
    .insert({ task_id: taskId, user_id: user.id, notes: notes?.trim() || null, attachments: attachments || [] })
    .select().single();

  if (error) return err(error.message, 500);
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
  if (!content || typeof content !== "string" || content.trim().length === 0) return err("Submission content is required");

  const { data: submission, error } = await supabase
    .from("task_submissions")
    .insert({ task_id: taskId, user_id: user.id, content: content.trim(), attachments: attachments || [] })
    .select().single();

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
    .eq("id", taskId).single();

  if (error || !task) return err("Task not found", 404);
  return json({ task });
}

// ══════════════════════════════════════════════════════
// ── VALIDATION ENDPOINTS ──
// ══════════════════════════════════════════════════════

// ── Route: GET /validations/pending ──
async function listPendingValidations(req: Request) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  // Get submissions for tasks in orgs where user is a validator/admin/moderator
  const serviceDb = getServiceSupabase();
  const { data: memberships } = await serviceDb
    .from("org_members")
    .select("org_id, role")
    .eq("user_id", user.id)
    .in("role", ["validator", "admin", "moderator"]);

  if (!memberships || memberships.length === 0) return json({ submissions: [] });

  const orgIds = memberships.map((m: any) => m.org_id);

  const { data: submissions, error } = await supabase
    .from("task_submissions")
    .select("*, tasks!inner(id, title, org_id, created_by, difficulty, tags)")
    .eq("status", "pending")
    .in("tasks.org_id", orgIds);

  if (error) return err(error.message, 500);

  // Filter out submissions where validator is creator or submitter
  const filtered = (submissions ?? []).filter((s: any) => {
    return s.user_id !== user.id && s.tasks?.created_by !== user.id;
  });

  // Enrich with profile info
  const userIds = [...new Set(filtered.map((s: any) => s.user_id))];
  const { data: profiles } = await serviceDb.from("profiles").select("id, display_name, avatar_url").in("id", userIds);
  const profileMap = Object.fromEntries((profiles ?? []).map((p: any) => [p.id, p]));

  const enriched = filtered.map((s: any) => ({
    ...s,
    contributor: profileMap[s.user_id] || { display_name: "Unknown", avatar_url: null },
  }));

  return json({ submissions: enriched });
}

// ── Route: POST /submissions/:id/approve ──
async function approveSubmission(req: Request, submissionId: string) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const serviceDb = getServiceSupabase();

  const { data: submission } = await serviceDb.from("task_submissions").select("*, tasks(*)").eq("id", submissionId).single();
  if (!submission) return err("Submission not found", 404);

  const task = (submission as any).tasks;
  if (!task) return err("Task not found", 404);

  // Verify validator is not creator or submitter
  if (submission.user_id === user.id) return err("Cannot validate your own submission", 403);
  if (task.created_by === user.id) return err("Cannot validate a task you created", 403);

  // Verify validator is org member with appropriate role
  if (task.org_id) {
    const { data: membership } = await serviceDb
      .from("org_members").select("role")
      .eq("user_id", user.id).eq("org_id", task.org_id)
      .in("role", ["validator", "admin", "moderator"]).single();
    if (!membership) return err("Not authorized to validate for this organization", 403);
  }

  const body = await req.json().catch(() => ({}));
  const feedback = body.feedback?.trim() || null;

  // Create validation record
  const { data: validation, error: valErr } = await supabase
    .from("validations")
    .insert({ submission_id: submissionId, validator_id: user.id, decision: "approved", feedback })
    .select().single();

  if (valErr) return err(valErr.message, 500);

  // Update submission status
  await serviceDb.from("task_submissions").update({ status: "approved" }).eq("id", submissionId);

  // Update task status to validated
  await serviceDb.from("tasks").update({ status: "validated" }).eq("id", task.id);

  // Create contribution record
  const { data: contribution } = await serviceDb.from("contributions").insert({
    task_id: task.id,
    user_id: submission.user_id,
    validation_id: validation.id,
    score: task.difficulty * 10,
    impact_metrics: { difficulty: task.difficulty, tags: task.tags },
  }).select().single();

  // Emit events
  await emitEvent(supabase, "ValidationApproved", user.id, "validation", validation.id, {
    submission_id: submissionId, task_id: task.id,
  });
  if (contribution) {
    await emitEvent(supabase, "ContributionRecorded", user.id, "contribution", contribution.id, {
      user_id: submission.user_id, score: contribution.score,
    });
  }

  // Audit log
  await auditLog(supabase, "submission_approved", user.id, "submission", submissionId, {
    task_id: task.id, validation_id: validation.id,
  });

  return json({ validation, contribution });
}

// ── Route: POST /submissions/:id/reject ──
async function rejectSubmission(req: Request, submissionId: string) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const serviceDb = getServiceSupabase();

  const { data: submission } = await serviceDb.from("task_submissions").select("*, tasks(*)").eq("id", submissionId).single();
  if (!submission) return err("Submission not found", 404);

  const task = (submission as any).tasks;
  if (!task) return err("Task not found", 404);

  if (submission.user_id === user.id) return err("Cannot validate your own submission", 403);
  if (task.created_by === user.id) return err("Cannot validate a task you created", 403);

  if (task.org_id) {
    const { data: membership } = await serviceDb
      .from("org_members").select("role")
      .eq("user_id", user.id).eq("org_id", task.org_id)
      .in("role", ["validator", "admin", "moderator"]).single();
    if (!membership) return err("Not authorized to validate for this organization", 403);
  }

  const body = await req.json().catch(() => ({}));
  const feedback = body.feedback?.trim() || null;
  if (!feedback) return err("Feedback is required when rejecting");

  const { data: validation, error: valErr } = await supabase
    .from("validations")
    .insert({ submission_id: submissionId, validator_id: user.id, decision: "rejected", feedback })
    .select().single();

  if (valErr) return err(valErr.message, 500);

  await serviceDb.from("task_submissions").update({ status: "rejected" }).eq("id", submissionId);
  await serviceDb.from("tasks").update({ status: "rejected" }).eq("id", task.id);

  await emitEvent(supabase, "ValidationRejected", user.id, "validation", validation.id, {
    submission_id: submissionId, task_id: task.id, feedback,
  });

  await auditLog(supabase, "submission_rejected", user.id, "submission", submissionId, {
    task_id: task.id, validation_id: validation.id, feedback,
  });

  return json({ validation });
}

// ── Route: POST /submissions/:id/request-clarification ──
async function requestClarification(req: Request, submissionId: string) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const serviceDb = getServiceSupabase();

  const { data: submission } = await serviceDb.from("task_submissions").select("*, tasks(*)").eq("id", submissionId).single();
  if (!submission) return err("Submission not found", 404);

  const task = (submission as any).tasks;
  if (submission.user_id === user.id) return err("Cannot request clarification on your own submission", 403);

  const body = await req.json().catch(() => ({}));
  const feedback = body.feedback?.trim() || null;
  if (!feedback) return err("Feedback message is required");

  const { data: validation, error: valErr } = await supabase
    .from("validations")
    .insert({ submission_id: submissionId, validator_id: user.id, decision: "clarification_requested", feedback })
    .select().single();

  if (valErr) return err(valErr.message, 500);

  await serviceDb.from("task_submissions").update({ status: "clarification_requested" }).eq("id", submissionId);

  await auditLog(supabase, "clarification_requested", user.id, "submission", submissionId, {
    task_id: task?.id, feedback,
  });

  return json({ validation });
}

// ── Route: POST /appeals ──
async function createAppeal(req: Request) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const body = await req.json();
  const { submission_id, validation_id, reason } = body;

  if (!submission_id || !validation_id || !reason?.trim()) {
    return err("submission_id, validation_id, and reason are required");
  }

  // Verify submission belongs to user
  const serviceDb = getServiceSupabase();
  const { data: submission } = await serviceDb.from("task_submissions").select("user_id").eq("id", submission_id).single();
  if (!submission) return err("Submission not found", 404);
  if (submission.user_id !== user.id) return err("You can only appeal your own submissions", 403);

  const { data: appeal, error } = await supabase
    .from("appeals")
    .insert({ submission_id, validation_id, appellant_id: user.id, reason: reason.trim() })
    .select().single();

  if (error) return err(error.message, 500);

  await auditLog(supabase, "appeal_created", user.id, "appeal", appeal.id, {
    submission_id, validation_id,
  });

  return json({ appeal }, 201);
}

// ── Route: POST /appeals/:id/resolve ──
async function resolveAppeal(req: Request, appealId: string) {
  const supabase = getSupabase(req);
  const user = await getUser(supabase);
  if (!user) return err("Unauthorized", 401);

  const serviceDb = getServiceSupabase();

  const { data: appeal } = await serviceDb.from("appeals").select("*, task_submissions(*, tasks(*))").eq("id", appealId).single();
  if (!appeal) return err("Appeal not found", 404);
  if (appeal.status !== "pending") return err("Appeal already resolved");

  const task = (appeal as any).task_submissions?.tasks;
  if (!task) return err("Associated task not found", 404);

  // Check moderator/admin role in org
  if (task.org_id) {
    const { data: membership } = await serviceDb
      .from("org_members").select("role")
      .eq("user_id", user.id).eq("org_id", task.org_id)
      .in("role", ["admin", "moderator"]).single();
    if (!membership) return err("Not authorized to resolve appeals", 403);
  }

  const body = await req.json();
  const { decision, resolution } = body; // decision: "upheld" | "overridden"

  if (!["upheld", "overridden"].includes(decision)) return err("Decision must be 'upheld' or 'overridden'");

  await serviceDb.from("appeals").update({
    status: decision === "upheld" ? "rejected" : "approved",
    resolved_by: user.id,
    resolution: resolution?.trim() || null,
    resolved_at: new Date().toISOString(),
  }).eq("id", appealId);

  if (decision === "overridden") {
    const submission = (appeal as any).task_submissions;
    // Override: approve the submission
    await serviceDb.from("task_submissions").update({ status: "approved" }).eq("id", submission.id);
    await serviceDb.from("tasks").update({ status: "validated" }).eq("id", task.id);

    // Create contribution
    const { data: contribution } = await serviceDb.from("contributions").insert({
      task_id: task.id,
      user_id: submission.user_id,
      score: task.difficulty * 10,
      impact_metrics: { difficulty: task.difficulty, overridden: true },
    }).select().single();

    if (contribution) {
      await emitEvent(supabase, "ContributionRecorded", user.id, "contribution", contribution.id, {
        user_id: submission.user_id, score: contribution.score, appeal_override: true,
      });
    }

    await emitEvent(supabase, "ValidationApproved", user.id, "validation", appeal.validation_id, {
      appeal_id: appealId, overridden: true,
    });
  }

  await auditLog(supabase, `appeal_${decision}`, user.id, "appeal", appealId, {
    submission_id: appeal.submission_id, resolution,
  });

  return json({ success: true, decision });
}

// ── Router ──
Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  const url = new URL(req.url);
  const pathParts = url.pathname.replace(/^\/task-engine\/?/, "").split("/").filter(Boolean);

  try {
    // ── Validation routes ──
    // GET /task-engine/validations/pending
    if (req.method === "GET" && pathParts[0] === "validations" && pathParts[1] === "pending") {
      return await listPendingValidations(req);
    }

    // POST /task-engine/submissions/:id/approve|reject|request-clarification
    if (req.method === "POST" && pathParts[0] === "submissions" && pathParts.length === 3) {
      const subId = pathParts[1];
      switch (pathParts[2]) {
        case "approve": return await approveSubmission(req, subId);
        case "reject": return await rejectSubmission(req, subId);
        case "request-clarification": return await requestClarification(req, subId);
      }
    }

    // POST /task-engine/appeals
    if (req.method === "POST" && pathParts[0] === "appeals" && pathParts.length === 1) {
      return await createAppeal(req);
    }

    // POST /task-engine/appeals/:id/resolve
    if (req.method === "POST" && pathParts[0] === "appeals" && pathParts.length === 3 && pathParts[2] === "resolve") {
      return await resolveAppeal(req, pathParts[1]);
    }

    // ── Task routes ──
    if (req.method === "POST" && pathParts.length === 0) return await createTask(req);
    if (req.method === "GET" && pathParts.length === 0) return await listTasks(req);
    if (req.method === "GET" && pathParts.length === 1) return await getTask(req, pathParts[0]);

    const taskId = pathParts[0];
    const action = pathParts[1];
    if (!taskId) return err("Task ID required", 400);

    if (req.method === "POST") {
      switch (action) {
        case "reserve": return await reserveTask(req, taskId);
        case "confirm-claim": return await confirmClaim(req, taskId);
        case "updates": return await addTaskUpdate(req, taskId);
        case "submit": return await submitTask(req, taskId);
        default: return err("Unknown action", 404);
      }
    }

    if (req.method === "GET") {
      switch (action) {
        case "submissions": return await getSubmissions(req, taskId);
        default: return err("Unknown action", 404);
      }
    }

    return err("Method not allowed", 405);
  } catch (e) {
    console.error("Task engine error:", e);
    return err("Internal server error", 500);
  }
});
