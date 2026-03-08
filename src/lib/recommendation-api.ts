import { supabase } from "@/integrations/supabase/client";

const FUNCTION_NAME = "recommendation-engine";

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

export const recommendationApi = {
  feed: (params?: { passport?: boolean; limit?: number }) => {
    const query = new URLSearchParams();
    if (params?.passport) query.set("passport", "true");
    if (params?.limit) query.set("limit", String(params.limit));
    const qs = query.toString();
    return invoke(`feed${qs ? `?${qs}` : ""}`, "GET");
  },
  nextMission: (lastTaskId?: string) => {
    const qs = lastTaskId ? `?last_task_id=${lastTaskId}` : "";
    return invoke(`next-mission${qs}`, "GET");
  },
  getInterests: () => invoke("interests", "GET"),
  updateInterests: (interests: string[]) => invoke("interests", "PUT", { interests }),
};
