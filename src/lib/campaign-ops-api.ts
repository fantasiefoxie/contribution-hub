import { supabase } from "@/integrations/supabase/client";

const FUNCTION_NAME = "campaign-ops";

async function invoke(path: string, method: string, body?: unknown) {
  const { data: { session } } = await supabase.auth.getSession();
  const baseUrl = import.meta.env.VITE_SUPABASE_URL;
  const url = `${baseUrl}/functions/v1/${FUNCTION_NAME}/${path}`;

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

export const campaignOpsApi = {
  dashboard: (initiativeId: string) => invoke(`dashboard/${initiativeId}`, "GET"),
  listRegions: (initiativeId: string) => invoke(`regions/${initiativeId}`, "GET"),
  createRegion: (input: { initiative_id: string; name: string; description?: string; sort_order?: number }) =>
    invoke("regions", "POST", input),
  capacityCheck: () => invoke("capacity-check", "GET"),
  rateCheck: () => invoke("rate-check", "GET"),
  feed: (params?: { region_id?: string; pool?: string; initiative_id?: string; limit?: number }) => {
    const query = new URLSearchParams();
    if (params?.region_id) query.set("region_id", params.region_id);
    if (params?.pool) query.set("pool", params.pool);
    if (params?.initiative_id) query.set("initiative_id", params.initiative_id);
    if (params?.limit) query.set("limit", String(params.limit));
    const qs = query.toString();
    return invoke(`feed${qs ? `?${qs}` : ""}`, "GET");
  },
};
