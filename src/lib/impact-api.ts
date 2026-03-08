import { supabase } from "@/integrations/supabase/client";

const FUNCTION_NAME = "impact-worker";

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

export const impactApi = {
  communityFeed: (limit?: number) => {
    const qs = limit ? `?limit=${limit}` : "";
    return invoke(`/community-feed${qs}`, "GET");
  },
  myImpact: () => invoke("/my-impact", "GET"),
  initiativeProgress: (id: string) => invoke(`/initiative/${id}/progress`, "GET"),
  nextMission: (currentTaskId?: string) => {
    const qs = currentTaskId ? `?current_task_id=${currentTaskId}` : "";
    return invoke(`/next-mission${qs}`, "GET");
  },
  processValidation: (contributionId: string) =>
    invoke("/process-validation", "POST", { contribution_id: contributionId }),
};
