import { supabase } from "@/integrations/supabase/client";

const FUNCTION_NAME = "task-engine";

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

export interface CreateTaskInput {
  title: string;
  description?: string;
  difficulty?: number;
  visibility?: "private" | "organization" | "public";
  tags?: string[];
  org_id?: string;
  initiative_id?: string;
  lock_duration_mins?: number;
  expires_at?: string;
  is_team?: boolean;
  team_target?: number;
}

export interface TaskUpdateInput {
  notes?: string;
  attachments?: unknown[];
}

export interface TaskSubmissionInput {
  content: string;
  attachments?: unknown[];
}

export interface ValidationActionInput {
  feedback?: string;
}

export interface AppealInput {
  submission_id: string;
  validation_id: string;
  reason: string;
}

export interface AppealResolveInput {
  decision: "upheld" | "overridden";
  resolution?: string;
}

export const taskApi = {
  list: (params?: { status?: string; visibility?: string; limit?: number; offset?: number }) => {
    const query = new URLSearchParams();
    if (params?.status) query.set("status", params.status);
    if (params?.visibility) query.set("visibility", params.visibility);
    if (params?.limit) query.set("limit", String(params.limit));
    if (params?.offset) query.set("offset", String(params.offset));
    const qs = query.toString();
    return invoke(qs ? `?${qs}` : "", "GET");
  },

  get: (taskId: string) => invoke(`/${taskId}`, "GET"),
  create: (input: CreateTaskInput) => invoke("", "POST", input),
  reserve: (taskId: string) => invoke(`/${taskId}/reserve`, "POST"),
  confirmClaim: (taskId: string) => invoke(`/${taskId}/confirm-claim`, "POST"),
  addUpdate: (taskId: string, input: TaskUpdateInput) => invoke(`/${taskId}/updates`, "POST", input),
  submit: (taskId: string, input: TaskSubmissionInput) => invoke(`/${taskId}/submit`, "POST", input),
  getSubmissions: (taskId: string) => invoke(`/${taskId}/submissions`, "GET"),

  // Team missions
  joinTeam: (taskId: string) => invoke(`/${taskId}/team/join`, "POST"),
  leaveTeam: (taskId: string) => invoke(`/${taskId}/team/leave`, "POST"),
  getTeam: (taskId: string) => invoke(`/${taskId}/team`, "GET"),
  getInviteLink: (taskId: string) => invoke(`/${taskId}/team/invite`, "GET"),

  // Validation
  getPendingValidations: () => invoke("/validations/pending", "GET"),
  approveSubmission: (submissionId: string, input?: ValidationActionInput) =>
    invoke(`/submissions/${submissionId}/approve`, "POST", input || {}),
  rejectSubmission: (submissionId: string, input: ValidationActionInput) =>
    invoke(`/submissions/${submissionId}/reject`, "POST", input),
  requestClarification: (submissionId: string, input: ValidationActionInput) =>
    invoke(`/submissions/${submissionId}/request-clarification`, "POST", input),

  // Appeals
  createAppeal: (input: AppealInput) => invoke("/appeals", "POST", input),
  resolveAppeal: (appealId: string, input: AppealResolveInput) =>
    invoke(`/appeals/${appealId}/resolve`, "POST", input),
};
