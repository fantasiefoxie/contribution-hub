import { supabase } from "@/integrations/supabase/client";

const FUNCTION_NAME = "leadership";

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

export const leadershipApi = {
  myProgression: () => invoke("/my-progression", "GET"),
  opportunities: () => invoke("/opportunities", "GET"),
  leaderboard: () => invoke("/leaderboard", "GET"),
  requestPromotion: (targetRole: string) => invoke("/request-promotion", "POST", { target_role: targetRole }),
  confirmPromotion: (candidateId: string, targetRole: string) =>
    invoke("/confirm-promotion", "POST", { candidate_id: candidateId, target_role: targetRole }),
};
