import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type, x-api-key, x-supabase-client-platform, x-supabase-client-platform-version, x-supabase-client-runtime, x-supabase-client-runtime-version",
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

// ── API Key Authentication ──
async function authenticateApiKey(req: Request): Promise<{ key: any; scopes: string[] } | null> {
  const apiKey = req.headers.get("x-api-key");
  if (!apiKey) return null;

  const db = getServiceDb();
  // Hash the key for lookup (simple prefix match + hash comparison)
  const prefix = apiKey.substring(0, 8);

  const { data: keys } = await db
    .from("api_keys")
    .select("*")
    .eq("key_prefix", prefix)
    .eq("is_active", true);

  if (!keys || keys.length === 0) return null;

  // Simple hash check using Web Crypto
  const encoder = new TextEncoder();
  const hashBuffer = await crypto.subtle.digest("SHA-256", encoder.encode(apiKey));
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  const hashHex = hashArray.map(b => b.toString(16).padStart(2, "0")).join("");

  const matched = keys.find((k: any) => k.key_hash === hashHex);
  if (!matched) return null;

  // Check expiry
  if (matched.expires_at && new Date(matched.expires_at) < new Date()) return null;

  // Update last_used_at
  await db.from("api_keys").update({ last_used_at: new Date().toISOString() }).eq("id", matched.id);

  return { key: matched, scopes: matched.scopes || [] };
}

// ── Auth: either API key or Bearer token ──
async function authenticate(req: Request): Promise<{ userId: string; scopes: string[]; isApiKey: boolean } | null> {
  // Try API key first
  const apiAuth = await authenticateApiKey(req);
  if (apiAuth) {
    return { userId: apiAuth.key.user_id, scopes: apiAuth.scopes, isApiKey: true };
  }

  // Try Bearer token
  const supabase = getUserSupabase(req);
  const { data: { user } } = await supabase.auth.getUser();
  if (user) {
    return { userId: user.id, scopes: ["users", "tasks", "initiatives", "contributions", "events", "webhooks"], isApiKey: false };
  }

  return null;
}

function hasScope(scopes: string[], required: string): boolean {
  return scopes.includes(required) || scopes.includes("*");
}

// ── Protocol: Users ──
async function getUsers(req: Request) {
  const db = getServiceDb();
  const url = new URL(req.url);
  const limit = Math.min(parseInt(url.searchParams.get("limit") || "20"), 100);
  const offset = parseInt(url.searchParams.get("offset") || "0");

  const { data, count } = await db
    .from("profiles")
    .select("id, display_name, avatar_url, status, interests, created_at", { count: "exact" })
    .order("created_at", { ascending: false })
    .range(offset, offset + limit - 1);

  return json({ users: data ?? [], total: count ?? 0, limit, offset });
}

async function getUser(_req: Request, userId: string) {
  const db = getServiceDb();
  const { data: profile } = await db.from("profiles").select("*").eq("id", userId).single();
  if (!profile) return err("User not found", 404);

  const [{ count: taskCount }, { count: contribCount }] = await Promise.all([
    db.from("tasks").select("*", { count: "exact", head: true }).eq("created_by", userId),
    db.from("contributions").select("*", { count: "exact", head: true }).eq("user_id", userId),
  ]);

  return json({
    user: {
      ...profile,
      stats: { tasks_created: taskCount ?? 0, contributions: contribCount ?? 0 },
    },
  });
}

// ── Protocol: Tasks ──
async function getTasks(req: Request) {
  const db = getServiceDb();
  const url = new URL(req.url);
  const status = url.searchParams.get("status");
  const initiative_id = url.searchParams.get("initiative_id");
  const limit = Math.min(parseInt(url.searchParams.get("limit") || "20"), 100);
  const offset = parseInt(url.searchParams.get("offset") || "0");

  let query = db
    .from("tasks")
    .select("id, title, description, difficulty, status, tags, is_team, team_target, initiative_id, org_id, created_at, visibility", { count: "exact" })
    .eq("visibility", "public")
    .order("created_at", { ascending: false })
    .range(offset, offset + limit - 1);

  if (status) query = query.eq("status", status);
  if (initiative_id) query = query.eq("initiative_id", initiative_id);

  const { data, count } = await query;
  return json({ tasks: data ?? [], total: count ?? 0, limit, offset });
}

async function getTask(_req: Request, taskId: string) {
  const db = getServiceDb();
  const { data } = await db
    .from("tasks")
    .select("*")
    .eq("id", taskId)
    .eq("visibility", "public")
    .single();
  if (!data) return err("Task not found", 404);
  return json({ task: data });
}

async function createTask(req: Request, userId: string) {
  const body = await req.json();
  const { title, description, difficulty, tags, initiative_id, is_team, team_target } = body;
  if (!title?.trim()) return err("title is required");

  const db = getServiceDb();
  const { data, error } = await db
    .from("tasks")
    .insert({
      title: title.trim(),
      description: description?.trim() || null,
      difficulty: difficulty || 1,
      tags: tags || [],
      initiative_id: initiative_id || null,
      is_team: is_team || false,
      team_target: team_target || null,
      created_by: userId,
      status: "open",
      visibility: "public",
    })
    .select()
    .single();

  if (error) return err(error.message, 500);

  // Fire event
  await db.from("events").insert({
    type: "TaskCreated",
    actor_id: userId,
    entity_type: "task",
    entity_id: data.id,
    payload: { title: data.title, source: "protocol_api" },
  });

  await dispatchWebhooks(db, "TaskCreated", { task: data });
  return json({ task: data }, 201);
}

// ── Protocol: Initiatives ──
async function getInitiatives(req: Request) {
  const db = getServiceDb();
  const url = new URL(req.url);
  const status = url.searchParams.get("status") || "active";
  const limit = Math.min(parseInt(url.searchParams.get("limit") || "20"), 100);
  const offset = parseInt(url.searchParams.get("offset") || "0");

  const { data, count } = await db
    .from("initiatives")
    .select("id, title, description, status, is_micro, start_date, end_date, target_metrics, created_at", { count: "exact" })
    .eq("status", status)
    .order("created_at", { ascending: false })
    .range(offset, offset + limit - 1);

  return json({ initiatives: data ?? [], total: count ?? 0, limit, offset });
}

async function getInitiative(_req: Request, id: string) {
  const db = getServiceDb();
  const { data } = await db.from("initiatives").select("*").eq("id", id).single();
  if (!data) return err("Initiative not found", 404);

  const [{ count: participantCount }, { count: taskCount }] = await Promise.all([
    db.from("initiative_participants").select("*", { count: "exact", head: true }).eq("initiative_id", id),
    db.from("tasks").select("*", { count: "exact", head: true }).eq("initiative_id", id),
  ]);

  return json({
    initiative: { ...data, participant_count: participantCount ?? 0, task_count: taskCount ?? 0 },
  });
}

async function createInitiative(req: Request, userId: string) {
  const body = await req.json();
  const { title, description, start_date, end_date, target_metrics, is_micro } = body;
  if (!title?.trim()) return err("title is required");

  const db = getServiceDb();
  const { data, error } = await db
    .from("initiatives")
    .insert({
      title: title.trim(),
      description: description?.trim() || null,
      start_date: start_date || null,
      end_date: end_date || null,
      target_metrics: target_metrics || {},
      is_micro: is_micro || false,
      max_tasks: is_micro ? 10 : null,
      max_volunteers: is_micro ? 25 : null,
      status: "draft",
      created_by: userId,
    })
    .select()
    .single();

  if (error) return err(error.message, 500);

  await db.from("initiative_participants").insert({ initiative_id: data.id, user_id: userId });
  await db.from("events").insert({
    type: "TaskCreated",
    actor_id: userId,
    entity_type: "initiative",
    entity_id: data.id,
    payload: { title: data.title, source: "protocol_api" },
  });

  await dispatchWebhooks(db, "InitiativeCreated", { initiative: data });
  return json({ initiative: data }, 201);
}

// ── Protocol: Contributions ──
async function getContributions(req: Request) {
  const db = getServiceDb();
  const url = new URL(req.url);
  const user_id = url.searchParams.get("user_id");
  const task_id = url.searchParams.get("task_id");
  const limit = Math.min(parseInt(url.searchParams.get("limit") || "20"), 100);
  const offset = parseInt(url.searchParams.get("offset") || "0");

  let query = db
    .from("contributions")
    .select("id, user_id, task_id, score, impact_metrics, created_at", { count: "exact" })
    .order("created_at", { ascending: false })
    .range(offset, offset + limit - 1);

  if (user_id) query = query.eq("user_id", user_id);
  if (task_id) query = query.eq("task_id", task_id);

  const { data, count } = await query;
  return json({ contributions: data ?? [], total: count ?? 0, limit, offset });
}

// ── Protocol: Events ──
async function getEvents(req: Request) {
  const db = getServiceDb();
  const url = new URL(req.url);
  const type = url.searchParams.get("type");
  const since = url.searchParams.get("since");
  const limit = Math.min(parseInt(url.searchParams.get("limit") || "50"), 200);

  let query = db
    .from("events")
    .select("id, type, entity_type, entity_id, actor_id, payload, created_at")
    .order("created_at", { ascending: false })
    .limit(limit);

  if (type) query = query.eq("type", type);
  if (since) query = query.gte("created_at", since);

  const { data } = await query;
  return json({ events: data ?? [] });
}

// ── Webhook Management ──
async function listWebhooks(req: Request, userId: string) {
  const db = getServiceDb();
  const { data: keys } = await db.from("api_keys").select("id").eq("user_id", userId);
  if (!keys || keys.length === 0) return json({ webhooks: [] });

  const keyIds = keys.map((k: any) => k.id);
  const { data } = await db.from("webhook_subscriptions").select("*").in("api_key_id", keyIds);
  return json({ webhooks: data ?? [] });
}

async function createWebhook(req: Request, userId: string) {
  const body = await req.json();
  const { api_key_id, url: webhookUrl, events } = body;
  if (!api_key_id || !webhookUrl || !events?.length) return err("api_key_id, url, and events are required");

  const db = getServiceDb();
  // Verify ownership
  const { data: key } = await db.from("api_keys").select("user_id").eq("id", api_key_id).single();
  if (!key || key.user_id !== userId) return err("API key not found", 404);

  // Generate webhook secret
  const secretBytes = crypto.getRandomValues(new Uint8Array(32));
  const secret = Array.from(secretBytes).map(b => b.toString(16).padStart(2, "0")).join("");

  const { data, error } = await db
    .from("webhook_subscriptions")
    .insert({ api_key_id, url: webhookUrl, events, secret, is_active: true })
    .select()
    .single();

  if (error) return err(error.message, 500);
  return json({ webhook: data, secret }, 201);
}

// ── API Key Management ──
async function listApiKeys(req: Request, userId: string) {
  const db = getServiceDb();
  const { data } = await db
    .from("api_keys")
    .select("id, name, key_prefix, scopes, rate_limit_per_min, is_active, last_used_at, created_at, expires_at")
    .eq("user_id", userId)
    .order("created_at", { ascending: false });
  return json({ api_keys: data ?? [] });
}

async function createApiKey(req: Request, userId: string) {
  const body = await req.json();
  const { name, scopes, org_id } = body;
  if (!name?.trim()) return err("name is required");

  const validScopes = ["users", "tasks", "initiatives", "contributions", "events", "webhooks", "*"];
  const requestedScopes = (scopes || ["tasks", "initiatives"]).filter((s: string) => validScopes.includes(s));

  // Generate API key
  const keyBytes = crypto.getRandomValues(new Uint8Array(32));
  const rawKey = "cos_" + Array.from(keyBytes).map(b => b.toString(16).padStart(2, "0")).join("");
  const prefix = rawKey.substring(0, 8);

  // Hash for storage
  const encoder = new TextEncoder();
  const hashBuffer = await crypto.subtle.digest("SHA-256", encoder.encode(rawKey));
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  const keyHash = hashArray.map(b => b.toString(16).padStart(2, "0")).join("");

  const db = getServiceDb();
  const { data, error } = await db
    .from("api_keys")
    .insert({
      user_id: userId,
      org_id: org_id || null,
      name: name.trim(),
      key_hash: keyHash,
      key_prefix: prefix,
      scopes: requestedScopes,
    })
    .select("id, name, key_prefix, scopes, rate_limit_per_min, created_at")
    .single();

  if (error) return err(error.message, 500);

  // Return raw key only once
  return json({ api_key: { ...data, key: rawKey } }, 201);
}

async function revokeApiKey(req: Request, userId: string, keyId: string) {
  const db = getServiceDb();
  const { error } = await db
    .from("api_keys")
    .update({ is_active: false })
    .eq("id", keyId)
    .eq("user_id", userId);

  if (error) return err(error.message, 500);
  return json({ revoked: true });
}

// ── Webhook Dispatch ──
async function dispatchWebhooks(db: any, eventType: string, payload: any) {
  try {
    const { data: subs } = await db
      .from("webhook_subscriptions")
      .select("id, url, secret, events")
      .eq("is_active", true)
      .lte("failure_count", 10);

    if (!subs || subs.length === 0) return;

    for (const sub of subs) {
      if (!sub.events.includes(eventType) && !sub.events.includes("*")) continue;

      try {
        const body = JSON.stringify({ event: eventType, data: payload, timestamp: new Date().toISOString() });

        // HMAC signature
        const encoder = new TextEncoder();
        const keyData = await crypto.subtle.importKey(
          "raw", encoder.encode(sub.secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]
        );
        const sig = await crypto.subtle.sign("HMAC", keyData, encoder.encode(body));
        const sigHex = Array.from(new Uint8Array(sig)).map(b => b.toString(16).padStart(2, "0")).join("");

        const res = await fetch(sub.url, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-Webhook-Signature": sigHex,
            "X-Event-Type": eventType,
          },
          body,
        });

        if (res.ok) {
          await db.from("webhook_subscriptions").update({ last_triggered_at: new Date().toISOString(), failure_count: 0 }).eq("id", sub.id);
        } else {
          await db.from("webhook_subscriptions").update({ failure_count: sub.failure_count + 1 }).eq("id", sub.id);
        }
      } catch {
        await db.from("webhook_subscriptions").update({ failure_count: (sub.failure_count || 0) + 1 }).eq("id", sub.id);
      }
    }
  } catch { /* silent */ }
}

// ── Docs endpoint ──
function getDocs() {
  return json({
    name: "Contribution OS Protocol API",
    version: "1.0.0",
    base_url: "/functions/v1/protocol",
    authentication: {
      methods: ["API Key (x-api-key header)", "Bearer Token (Authorization header)"],
      api_key_format: "cos_<64 hex chars>",
      scopes: ["users", "tasks", "initiatives", "contributions", "events", "webhooks", "* (all)"],
    },
    rate_limits: { default: "60 requests/minute", configurable: true },
    endpoints: [
      { method: "GET", path: "/docs", description: "This documentation", auth: false },
      { method: "GET", path: "/users", description: "List volunteer profiles", scope: "users", params: ["limit", "offset"] },
      { method: "GET", path: "/users/:id", description: "Get volunteer profile with stats", scope: "users" },
      { method: "GET", path: "/tasks", description: "List public tasks", scope: "tasks", params: ["status", "initiative_id", "limit", "offset"] },
      { method: "GET", path: "/tasks/:id", description: "Get task details", scope: "tasks" },
      { method: "POST", path: "/tasks", description: "Create a new task", scope: "tasks" },
      { method: "GET", path: "/initiatives", description: "List initiatives", scope: "initiatives", params: ["status", "limit", "offset"] },
      { method: "GET", path: "/initiatives/:id", description: "Get initiative details", scope: "initiatives" },
      { method: "POST", path: "/initiatives", description: "Create an initiative", scope: "initiatives" },
      { method: "GET", path: "/contributions", description: "List contributions", scope: "contributions", params: ["user_id", "task_id", "limit", "offset"] },
      { method: "GET", path: "/events", description: "Query event stream", scope: "events", params: ["type", "since", "limit"] },
      { method: "GET", path: "/keys", description: "List your API keys", auth: "bearer" },
      { method: "POST", path: "/keys", description: "Create an API key", auth: "bearer" },
      { method: "DELETE", path: "/keys/:id", description: "Revoke an API key", auth: "bearer" },
      { method: "GET", path: "/webhooks", description: "List webhook subscriptions", auth: "bearer" },
      { method: "POST", path: "/webhooks", description: "Create a webhook subscription", auth: "bearer" },
    ],
    events: [
      { type: "TaskCreated", description: "A new task was created" },
      { type: "TaskReserved", description: "A volunteer reserved a task" },
      { type: "TaskClaimed", description: "A volunteer confirmed a task claim" },
      { type: "TaskSubmitted", description: "A task submission was made" },
      { type: "ValidationApproved", description: "A submission was approved" },
      { type: "ValidationRejected", description: "A submission was rejected" },
      { type: "ContributionRecorded", description: "A contribution was recorded" },
      { type: "InitiativeCreated", description: "A new initiative was created" },
    ],
    webhook_format: {
      headers: { "X-Webhook-Signature": "HMAC-SHA256 of body with webhook secret", "X-Event-Type": "Event type string" },
      body: { event: "string", data: "object", timestamp: "ISO 8601" },
    },
  });
}

// ── Router ──
Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  const url = new URL(req.url);
  const parts = url.pathname.replace(/^\/protocol\/?/, "").split("/").filter(Boolean);

  try {
    // Public endpoint: docs
    if (parts[0] === "docs" && req.method === "GET") return getDocs();

    // Authenticate
    const auth = await authenticate(req);

    // Protected read endpoints (API key or bearer)
    if (req.method === "GET") {
      switch (parts[0]) {
        case "users":
          if (!auth) return err("Unauthorized", 401);
          if (!hasScope(auth.scopes, "users")) return err("Insufficient scope: users", 403);
          return parts[1] ? getUser(req, parts[1]) : getUsers(req);
        case "tasks":
          if (!auth) return err("Unauthorized", 401);
          if (!hasScope(auth.scopes, "tasks")) return err("Insufficient scope: tasks", 403);
          return parts[1] ? getTask(req, parts[1]) : getTasks(req);
        case "initiatives":
          if (!auth) return err("Unauthorized", 401);
          if (!hasScope(auth.scopes, "initiatives")) return err("Insufficient scope: initiatives", 403);
          return parts[1] ? getInitiative(req, parts[1]) : getInitiatives(req);
        case "contributions":
          if (!auth) return err("Unauthorized", 401);
          if (!hasScope(auth.scopes, "contributions")) return err("Insufficient scope: contributions", 403);
          return getContributions(req);
        case "events":
          if (!auth) return err("Unauthorized", 401);
          if (!hasScope(auth.scopes, "events")) return err("Insufficient scope: events", 403);
          return getEvents(req);
        case "keys":
          if (!auth || auth.isApiKey) return err("Bearer auth required", 401);
          return listApiKeys(req, auth.userId);
        case "webhooks":
          if (!auth || auth.isApiKey) return err("Bearer auth required", 401);
          return listWebhooks(req, auth.userId);
      }
    }

    if (req.method === "POST") {
      switch (parts[0]) {
        case "tasks":
          if (!auth) return err("Unauthorized", 401);
          if (!hasScope(auth.scopes, "tasks")) return err("Insufficient scope: tasks", 403);
          return createTask(req, auth.userId);
        case "initiatives":
          if (!auth) return err("Unauthorized", 401);
          if (!hasScope(auth.scopes, "initiatives")) return err("Insufficient scope: initiatives", 403);
          return createInitiative(req, auth.userId);
        case "keys":
          if (!auth || auth.isApiKey) return err("Bearer auth required", 401);
          return createApiKey(req, auth.userId);
        case "webhooks":
          if (!auth || auth.isApiKey) return err("Bearer auth required", 401);
          return createWebhook(req, auth.userId);
      }
    }

    if (req.method === "DELETE") {
      if (parts[0] === "keys" && parts[1]) {
        if (!auth || auth.isApiKey) return err("Bearer auth required", 401);
        return revokeApiKey(req, auth.userId, parts[1]);
      }
    }

    return err("Not found", 404);
  } catch (e) {
    console.error("Protocol error:", e);
    return err("Internal server error", 500);
  }
});
