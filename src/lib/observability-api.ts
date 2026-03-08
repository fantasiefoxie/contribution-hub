import { supabase } from "@/integrations/supabase/client";

const FUNCTION_NAME = "observability";

async function invoke(path: string) {
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

  const res = await fetch(url, { method: "GET", headers });
  const json = await res.json();
  if (!res.ok) throw new Error(json.error || "Request failed");
  return json;
}

export const observabilityApi = {
  platformHealth: () => invoke("/platform-health"),
  initiativeHealth: (id: string) => invoke(`/initiative-health/${id}`),
  validatorMonitoring: () => invoke("/validator-monitoring"),
  fraudSignals: () => invoke("/fraud-signals"),
  integrityScore: (userId: string) => invoke(`/integrity-score/${userId}`),
  eventStream: (limit?: number) => invoke(`/event-stream${limit ? `?limit=${limit}` : ""}`),
  transparency: () => invoke("/transparency"),
  aiMetrics: () => invoke("/ai-metrics"),
};
