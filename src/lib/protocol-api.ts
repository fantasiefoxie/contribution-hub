import { supabase } from "@/integrations/supabase/client";

const FUNCTION_NAME = "protocol";

async function invoke(path: string, method: string, body?: unknown) {
  const { data: { session } } = await supabase.auth.getSession();
  const baseUrl = import.meta.env.VITE_SUPABASE_URL;
  const url = `${baseUrl}/functions/v1/${FUNCTION_NAME}${path}`;

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    "apikey": import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY,
  };
  if (session?.access_token) {
    headers["Authorization"] = `Bearer ${session.access_token}`;
  }

  const res = await fetch(url, { method, headers, body: body ? JSON.stringify(body) : undefined });
  const json = await res.json();
  if (!res.ok) throw new Error(json.error || "Request failed");
  return json;
}

export const protocolApi = {
  getDocs: () => invoke("/docs", "GET"),

  // API Keys
  listKeys: () => invoke("/keys", "GET"),
  createKey: (input: { name: string; scopes?: string[]; org_id?: string }) => invoke("/keys", "POST", input),
  revokeKey: (keyId: string) => invoke(`/keys/${keyId}`, "DELETE"),

  // Webhooks
  listWebhooks: () => invoke("/webhooks", "GET"),
  createWebhook: (input: { api_key_id: string; url: string; events: string[] }) =>
    invoke("/webhooks", "POST", input),

  // Credentials
  listCredentials: (params?: { user_id?: string; type?: string }) => {
    const qs = new URLSearchParams();
    if (params?.user_id) qs.set("user_id", params.user_id);
    if (params?.type) qs.set("type", params.type);
    const q = qs.toString();
    return invoke(`/credentials${q ? `?${q}` : ""}`, "GET");
  },
  getCredential: (id: string) => invoke(`/credentials/${id}`, "GET"),
  issueCredential: (input: { target_user_id: string; title: string; type?: string; initiative_id?: string; org_id?: string; description?: string; metadata?: Record<string, unknown>; metadata_entries?: { key: string; value: string }[] }) =>
    invoke("/credentials", "POST", input),
  revokeCredential: (id: string) => invoke(`/credentials/${id}`, "DELETE"),

  // Roles
  listRoles: (userId?: string) => invoke(`/roles${userId ? `?user_id=${userId}` : ""}`, "GET"),
  grantRole: (input: { target_user_id: string; role: string }) => invoke("/roles/grant", "POST", input),
  revokeRole: (input: { target_user_id: string; role: string }) => invoke("/roles/revoke", "POST", input),

  // Initiatives (extended)
  joinInitiative: (initiativeId: string) => invoke(`/initiatives/${initiativeId}/join`, "POST"),
  getInitiativePriority: (initiativeId: string) => invoke(`/initiatives/${initiativeId}/priority`, "GET"),
  updateInitiativePriority: (initiativeId: string, input: { priority_score?: number; volunteer_target?: number; featured?: boolean }) =>
    invoke(`/initiatives/${initiativeId}/priority`, "PUT", input),
};
