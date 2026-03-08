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
};
