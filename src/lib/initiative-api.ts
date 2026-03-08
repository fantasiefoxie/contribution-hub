import { supabase } from "@/integrations/supabase/client";

const FUNCTION_NAME = "initiative-engine";

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

export interface CreateInitiativeInput {
  title: string;
  description?: string;
  start_date?: string;
  end_date?: string;
  success_metrics?: Record<string, unknown>;
  target_metrics?: Record<string, number>;
  is_micro?: boolean;
}

export interface GenerateTasksInput {
  template_title: string;
  description?: string;
  difficulty?: number;
  tags?: string[];
  project_id?: string;
  count: number;
  variables?: Record<string, unknown>[];
}

export const initiativeApi = {
  list: (params?: { status?: string; is_micro?: string }) => {
    const query = new URLSearchParams();
    if (params?.status) query.set("status", params.status);
    if (params?.is_micro) query.set("is_micro", params.is_micro);
    const qs = query.toString();
    return invoke(qs ? `?${qs}` : "", "GET");
  },
  get: (id: string) => invoke(`/${id}`, "GET"),
  create: (input: CreateInitiativeInput) => invoke("", "POST", input),
  update: (id: string, input: Partial<CreateInitiativeInput & { status: string }>) => invoke(`/${id}`, "PUT", input),
  createProject: (id: string, input: { title: string; description?: string; sort_order?: number }) =>
    invoke(`/${id}/projects`, "POST", input),
  generateTasks: (id: string, input: GenerateTasksInput) =>
    invoke(`/${id}/generate-tasks`, "POST", input),
  join: (id: string) => invoke(`/${id}/join`, "POST"),
  promote: (id: string) => invoke(`/${id}/promote`, "POST"),
};
