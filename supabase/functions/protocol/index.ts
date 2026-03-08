import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

// ═══════════════════════════════════════════════════════════════════
// Contribution OS — Coordination Protocol API v2
// Stabilized with: rate limiting, idempotency, granular permissions,
// versioned events, and compatibility layer
// ═══════════════════════════════════════════════════════════════════

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type, x-api-key, x-idempotency-key, x-supabase-client-platform, x-supabase-client-platform-version, x-supabase-client-runtime, x-supabase-client-runtime-version",
};

const PROTOCOL_VERSION = "2.0.0";

function json(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json", "X-Protocol-Version": PROTOCOL_VERSION },
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

// ═══ Rate Limiting ═══
async function checkRateLimit(db: any, identifier: string, action: string, maxPerMinute: number): Promise<boolean> {
  const windowStart = new Date();
  windowStart.setSeconds(0, 0);
  const windowKey = windowStart.toISOString();

  const { data } = await db
    .from("rate_limit_entries")
    .select("count")
    .eq("identifier", identifier)
    .eq("action", action)
    .eq("window_start", windowKey)
    .single();

  if (data) {
    if (data.count >= maxPerMinute) return false;
    await db
      .from("rate_limit_entries")
      .update({ count: data.count + 1 })
      .eq("identifier", identifier)
      .eq("action", action)
      .eq("window_start", windowKey);
  } else {
    await db
      .from("rate_limit_entries")
      .insert({ identifier, action, window_start: windowKey, count: 1 });
  }
  return true;
}

// ═══ Idempotency ═══
async function checkIdempotency(db: any, key: string, userId: string, operation: string): Promise<{ cached: boolean; result?: any }> {
  if (!key) return { cached: false };
  const { data } = await db
    .from("idempotency_keys")
    .select("result")
    .eq("key", key)
    .eq("user_id", userId)
    .eq("operation", operation)
    .single();
  if (data?.result) return { cached: true, result: data.result };
  return { cached: false };
}

async function storeIdempotency(db: any, key: string, userId: string, operation: string, result: any) {
  if (!key) return;
  await db.from("idempotency_keys").insert({ key, user_id: userId, operation, result }).onConflict("key,user_id,operation").merge();
}

// ═══ API Key Authentication ═══
async function authenticateApiKey(req: Request): Promise<{ key: any; scopes: string[] } | null> {
  const apiKey = req.headers.get("x-api-key");
  if (!apiKey) return null;

  const db = getServiceDb();
  const prefix = apiKey.substring(0, 8);
  const { data: keys } = await db.from("api_keys").select("*").eq("key_prefix", prefix).eq("is_active", true);
  if (!keys || keys.length === 0) return null;

  const encoder = new TextEncoder();
  const hashBuffer = await crypto.subtle.digest("SHA-256", encoder.encode(apiKey));
  const hashHex = Array.from(new Uint8Array(hashBuffer)).map(b => b.toString(16).padStart(2, "0")).join("");
  const matched = keys.find((k: any) => k.key_hash === hashHex);
  if (!matched) return null;
  if (matched.expires_at && new Date(matched.expires_at) < new Date()) return null;

  await db.from("api_keys").update({ last_used_at: new Date().toISOString() }).eq("id", matched.id);
  return { key: matched, scopes: matched.scopes || [] };
}

// ═══ Auth (API key or Bearer) ═══
async function authenticate(req: Request): Promise<{ userId: string; scopes: string[]; isApiKey: boolean; rateLimit: number } | null> {
  const apiAuth = await authenticateApiKey(req);
  if (apiAuth) {
    return { userId: apiAuth.key.user_id, scopes: apiAuth.scopes, isApiKey: true, rateLimit: apiAuth.key.rate_limit_per_min || 60 };
  }

  const supabase = getUserSupabase(req);
  const { data: { user } } = await supabase.auth.getUser();
  if (user) {
    // Load granular scopes from role_permissions based on user's roles
    const db = getServiceDb();
    const { data: roles } = await db.from("user_roles").select("role").eq("user_id", user.id);
    const userRoles = (roles ?? []).map((r: any) => r.role);

    let scopes: string[] = [];
    if (userRoles.length > 0) {
      const { data: perms } = await db.from("role_permissions").select("scope").in("role", userRoles);
      scopes = [...new Set((perms ?? []).map((p: any) => p.scope))];
    }

    // Default scopes for authenticated users (volunteer level)
    const defaultScopes = ["tasks:read", "tasks:claim", "initiatives:read", "contributions:read", "profiles:read",
      "users", "tasks", "initiatives", "contributions", "events", "webhooks"];
    scopes = [...new Set([...scopes, ...defaultScopes])];

    return { userId: user.id, scopes, isApiKey: false, rateLimit: 120 };
  }

  return null;
}

function hasScope(scopes: string[], required: string): boolean {
  return scopes.includes(required) || scopes.includes("*");
}

// ═══ Versioned Event Emitter ═══
async function emitEvent(db: any, type: string, actorId: string, entityType: string, entityId: string, payload: any = {}, version = 1) {
  await db.from("events").insert({ type, actor_id: actorId, entity_type: entityType, entity_id: entityId, payload, version });
  await dispatchWebhooks(db, type, { entity_type: entityType, entity_id: entityId, ...payload }, version);
}

// ═══ Protocol: Users ═══
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

  return json({ user: { ...profile, stats: { tasks_created: taskCount ?? 0, contributions: contribCount ?? 0 } } });
}

// ═══ Protocol: Tasks ═══
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
  const { data } = await db.from("tasks").select("*").eq("id", taskId).eq("visibility", "public").single();
  if (!data) return err("Task not found", 404);
  return json({ task: data });
}

async function createTask(req: Request, userId: string, idempotencyKey: string | null) {
  const db = getServiceDb();

  if (idempotencyKey) {
    const { cached, result } = await checkIdempotency(db, idempotencyKey, userId, "create_task");
    if (cached) return json(result, 201);
  }

  const body = await req.json();
  const { title, description, difficulty, tags, initiative_id, is_team, team_target } = body;
  if (!title?.trim()) return err("title is required");

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

  await emitEvent(db, "TaskCreated", userId, "task", data.id, { title: data.title, source: "protocol_api" });

  const result = { task: data };
  if (idempotencyKey) await storeIdempotency(db, idempotencyKey, userId, "create_task", result);
  return json(result, 201);
}

// ═══ Protocol: Initiatives ═══
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

  return json({ initiative: { ...data, participant_count: participantCount ?? 0, task_count: taskCount ?? 0 } });
}

async function createInitiative(req: Request, userId: string, idempotencyKey: string | null) {
  const db = getServiceDb();

  if (idempotencyKey) {
    const { cached, result } = await checkIdempotency(db, idempotencyKey, userId, "create_initiative");
    if (cached) return json(result, 201);
  }

  const body = await req.json();
  const { title, description, start_date, end_date, target_metrics, is_micro } = body;
  if (!title?.trim()) return err("title is required");

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
  await emitEvent(db, "TaskCreated", userId, "initiative", data.id, { title: data.title, source: "protocol_api" });

  const result = { initiative: data };
  if (idempotencyKey) await storeIdempotency(db, idempotencyKey, userId, "create_initiative", result);
  return json(result, 201);
}

// ═══ Protocol: Contributions ═══
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

// ═══ Protocol: Events ═══
async function getEvents(req: Request) {
  const db = getServiceDb();
  const url = new URL(req.url);
  const type = url.searchParams.get("type");
  const since = url.searchParams.get("since");
  const version = url.searchParams.get("version");
  const limit = Math.min(parseInt(url.searchParams.get("limit") || "50"), 200);

  let query = db
    .from("events")
    .select("id, type, entity_type, entity_id, actor_id, payload, created_at, version")
    .order("created_at", { ascending: false })
    .limit(limit);

  if (type) query = query.eq("type", type);
  if (since) query = query.gte("created_at", since);
  if (version) query = query.eq("version", parseInt(version));

  const { data } = await query;
  return json({ events: data ?? [] });
}

// ═══ Protocol: Permissions ═══
async function getPermissions() {
  const db = getServiceDb();
  const { data } = await db.from("role_permissions").select("role, scope").order("role");

  const grouped: Record<string, string[]> = {};
  for (const p of data ?? []) {
    if (!grouped[p.role]) grouped[p.role] = [];
    grouped[p.role].push(p.scope);
  }

  return json({ roles: grouped });
}

// ═══ Webhook Management ═══
async function listWebhooks(_req: Request, userId: string) {
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
  const { data: key } = await db.from("api_keys").select("user_id").eq("id", api_key_id).single();
  if (!key || key.user_id !== userId) return err("API key not found", 404);

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

// ═══ API Key Management ═══
async function listApiKeys(_req: Request, userId: string) {
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

  const validScopes = ["users", "tasks", "initiatives", "contributions", "events", "webhooks", "*",
    "tasks:read", "tasks:create", "tasks:claim", "tasks:validate", "initiatives:read", "initiatives:create",
    "initiatives:manage", "contributions:read", "profiles:read", "submissions:review"];
  const requestedScopes = (scopes || ["tasks", "initiatives"]).filter((s: string) => validScopes.includes(s));

  const keyBytes = crypto.getRandomValues(new Uint8Array(32));
  const rawKey = "cos_" + Array.from(keyBytes).map(b => b.toString(16).padStart(2, "0")).join("");
  const prefix = rawKey.substring(0, 8);

  const encoder = new TextEncoder();
  const hashBuffer = await crypto.subtle.digest("SHA-256", encoder.encode(rawKey));
  const keyHash = Array.from(new Uint8Array(hashBuffer)).map(b => b.toString(16).padStart(2, "0")).join("");

  const db = getServiceDb();
  const { data, error } = await db
    .from("api_keys")
    .insert({ user_id: userId, org_id: org_id || null, name: name.trim(), key_hash: keyHash, key_prefix: prefix, scopes: requestedScopes })
    .select("id, name, key_prefix, scopes, rate_limit_per_min, created_at")
    .single();

  if (error) return err(error.message, 500);
  return json({ api_key: { ...data, key: rawKey } }, 201);
}

async function revokeApiKey(_req: Request, userId: string, keyId: string) {
  const db = getServiceDb();
  const { error } = await db.from("api_keys").update({ is_active: false }).eq("id", keyId).eq("user_id", userId);
  if (error) return err(error.message, 500);
  return json({ revoked: true });
}

// ═══ Webhook Dispatch ═══
async function dispatchWebhooks(db: any, eventType: string, payload: any, version = 1) {
  try {
    const { data: subs } = await db
      .from("webhook_subscriptions")
      .select("id, url, secret, events, failure_count")
      .eq("is_active", true)
      .lte("failure_count", 10);

    if (!subs || subs.length === 0) return;

    for (const sub of subs) {
      if (!sub.events.includes(eventType) && !sub.events.includes("*")) continue;
      try {
        const body = JSON.stringify({ event: eventType, data: payload, timestamp: new Date().toISOString(), version });
        const encoder = new TextEncoder();
        const keyData = await crypto.subtle.importKey("raw", encoder.encode(sub.secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
        const sig = await crypto.subtle.sign("HMAC", keyData, encoder.encode(body));
        const sigHex = Array.from(new Uint8Array(sig)).map(b => b.toString(16).padStart(2, "0")).join("");

        const res = await fetch(sub.url, {
          method: "POST",
          headers: { "Content-Type": "application/json", "X-Webhook-Signature": sigHex, "X-Event-Type": eventType, "X-Protocol-Version": PROTOCOL_VERSION },
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

// ═══ Docs ═══
function getDocs() {
  return json({
    name: "Contribution OS Coordination Protocol",
    version: PROTOCOL_VERSION,
    base_url: "/functions/v1/protocol",
    authentication: {
      methods: ["API Key (x-api-key header)", "Bearer Token (Authorization header)"],
      api_key_format: "cos_<64 hex chars>",
      scopes: {
        legacy: ["users", "tasks", "initiatives", "contributions", "events", "webhooks", "*"],
        granular: ["tasks:read", "tasks:create", "tasks:claim", "tasks:validate", "tasks:manage",
          "initiatives:read", "initiatives:create", "initiatives:manage",
          "contributions:read", "profiles:read", "submissions:review",
          "appeals:resolve", "validators:assign", "governance:audit", "protocol:manage"],
      },
    },
    rate_limits: { default: "60 req/min (API key)", authenticated: "120 req/min (bearer)", configurable: true },
    idempotency: { header: "x-idempotency-key", description: "Pass a unique key for POST operations to ensure at-most-once delivery. Keys expire after 24h." },
    event_versioning: { current: 1, description: "Events include a version field. Filter with ?version=1." },
    endpoints: [
      { method: "GET", path: "/docs", description: "This documentation", auth: false },
      { method: "GET", path: "/permissions", description: "List role-to-scope mappings", auth: false },
      { method: "GET", path: "/users", description: "List volunteer profiles", scope: "users" },
      { method: "GET", path: "/users/:id", description: "Get volunteer profile with stats", scope: "users" },
      { method: "GET", path: "/tasks", description: "List public tasks", scope: "tasks" },
      { method: "GET", path: "/tasks/:id", description: "Get task details", scope: "tasks" },
      { method: "POST", path: "/tasks", description: "Create a task (idempotent)", scope: "tasks" },
      { method: "GET", path: "/initiatives", description: "List initiatives", scope: "initiatives" },
      { method: "GET", path: "/initiatives/:id", description: "Get initiative details", scope: "initiatives" },
      { method: "POST", path: "/initiatives", description: "Create an initiative (idempotent)", scope: "initiatives" },
      { method: "GET", path: "/contributions", description: "List contributions", scope: "contributions" },
      { method: "GET", path: "/events", description: "Query event stream", scope: "events", params: ["type", "since", "version", "limit"] },
      { method: "GET", path: "/keys", description: "List your API keys", auth: "bearer" },
      { method: "POST", path: "/keys", description: "Create an API key", auth: "bearer" },
      { method: "DELETE", path: "/keys/:id", description: "Revoke an API key", auth: "bearer" },
      { method: "GET", path: "/webhooks", description: "List webhook subscriptions", auth: "bearer" },
      { method: "POST", path: "/webhooks", description: "Create webhook subscription", auth: "bearer" },
    ],
    events: [
      { type: "TaskCreated", version: 1 }, { type: "TaskReserved", version: 1 },
      { type: "TaskClaimed", version: 1 }, { type: "TaskSubmitted", version: 1 },
      { type: "ValidationApproved", version: 1 }, { type: "ValidationRejected", version: 1 },
      { type: "ContributionRecorded", version: 1 }, { type: "InitiativeCreated", version: 1 },
    ],
    webhook_format: {
      headers: { "X-Webhook-Signature": "HMAC-SHA256", "X-Event-Type": "string", "X-Protocol-Version": PROTOCOL_VERSION },
      body: { event: "string", data: "object", timestamp: "ISO 8601", version: "number" },
    },
    modules: ["Identity", "Task Engine", "Initiative Engine", "Contribution Ledger", "Event Stream", "Governance", "Notifications"],
  });
}

// ═══ Router ═══
Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  const url = new URL(req.url);
  const parts = url.pathname.replace(/^\/protocol\/?/, "").split("/").filter(Boolean);
  const idempotencyKey = req.headers.get("x-idempotency-key");

  try {
    // Public endpoints
    if (parts[0] === "docs" && req.method === "GET") return getDocs();
    if (parts[0] === "permissions" && req.method === "GET") return getPermissions();

    // Authenticate
    const auth = await authenticate(req);
    if (!auth) return err("Unauthorized", 401);

    // Rate limit check
    const db = getServiceDb();
    const rateLimitId = auth.isApiKey ? `apikey:${auth.userId}` : `user:${auth.userId}`;
    const allowed = await checkRateLimit(db, rateLimitId, "protocol_request", auth.rateLimit);
    if (!allowed) return err("Rate limit exceeded", 429);

    if (req.method === "GET") {
      switch (parts[0]) {
        case "users":
          if (!hasScope(auth.scopes, "users") && !hasScope(auth.scopes, "profiles:read")) return err("Insufficient scope", 403);
          return parts[1] ? getUser(req, parts[1]) : getUsers(req);
        case "tasks":
          if (!hasScope(auth.scopes, "tasks") && !hasScope(auth.scopes, "tasks:read")) return err("Insufficient scope", 403);
          return parts[1] ? getTask(req, parts[1]) : getTasks(req);
        case "initiatives":
          if (!hasScope(auth.scopes, "initiatives") && !hasScope(auth.scopes, "initiatives:read")) return err("Insufficient scope", 403);
          return parts[1] ? getInitiative(req, parts[1]) : getInitiatives(req);
        case "contributions":
          if (!hasScope(auth.scopes, "contributions") && !hasScope(auth.scopes, "contributions:read")) return err("Insufficient scope", 403);
          return getContributions(req);
        case "events":
          if (!hasScope(auth.scopes, "events")) return err("Insufficient scope", 403);
          return getEvents(req);
        case "keys":
          if (auth.isApiKey) return err("Bearer auth required", 401);
          return listApiKeys(req, auth.userId);
        case "webhooks":
          if (auth.isApiKey) return err("Bearer auth required", 401);
          return listWebhooks(req, auth.userId);
      }
    }

    if (req.method === "POST") {
      switch (parts[0]) {
        case "tasks":
          if (!hasScope(auth.scopes, "tasks") && !hasScope(auth.scopes, "tasks:create")) return err("Insufficient scope", 403);
          return createTask(req, auth.userId, idempotencyKey);
        case "initiatives":
          if (!hasScope(auth.scopes, "initiatives") && !hasScope(auth.scopes, "initiatives:create")) return err("Insufficient scope", 403);
          return createInitiative(req, auth.userId, idempotencyKey);
        case "keys":
          if (auth.isApiKey) return err("Bearer auth required", 401);
          return createApiKey(req, auth.userId);
        case "webhooks":
          if (auth.isApiKey) return err("Bearer auth required", 401);
          return createWebhook(req, auth.userId);
      }
    }

    if (req.method === "DELETE") {
      if (parts[0] === "keys" && parts[1]) {
        if (auth.isApiKey) return err("Bearer auth required", 401);
        return revokeApiKey(req, auth.userId, parts[1]);
      }
    }

    return err("Not found", 404);
  } catch (e) {
    console.error("Protocol error:", e);
    return err("Internal server error", 500);
  }
});
