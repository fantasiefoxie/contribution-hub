import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { taskApi, type CreateTaskInput, type TaskUpdateInput, type TaskSubmissionInput } from "@/lib/task-api";

export function useTasks(params?: { status?: string; visibility?: string }) {
  return useQuery({
    queryKey: ["tasks", params],
    queryFn: () => taskApi.list(params),
  });
}

export function useTask(taskId: string) {
  return useQuery({
    queryKey: ["task", taskId],
    queryFn: () => taskApi.get(taskId),
    enabled: !!taskId,
  });
}

export function useCreateTask() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateTaskInput) => taskApi.create(input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["tasks"] }),
  });
}

export function useReserveTask() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (taskId: string) => taskApi.reserve(taskId),
    onSuccess: (_, taskId) => {
      qc.invalidateQueries({ queryKey: ["tasks"] });
      qc.invalidateQueries({ queryKey: ["task", taskId] });
    },
  });
}

export function useConfirmClaim() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (taskId: string) => taskApi.confirmClaim(taskId),
    onSuccess: (_, taskId) => {
      qc.invalidateQueries({ queryKey: ["tasks"] });
      qc.invalidateQueries({ queryKey: ["task", taskId] });
    },
  });
}

export function useAddTaskUpdate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ taskId, input }: { taskId: string; input: TaskUpdateInput }) =>
      taskApi.addUpdate(taskId, input),
    onSuccess: (_, { taskId }) => qc.invalidateQueries({ queryKey: ["task", taskId] }),
  });
}

export function useSubmitTask() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ taskId, input }: { taskId: string; input: TaskSubmissionInput }) =>
      taskApi.submit(taskId, input),
    onSuccess: (_, { taskId }) => {
      qc.invalidateQueries({ queryKey: ["tasks"] });
      qc.invalidateQueries({ queryKey: ["task", taskId] });
    },
  });
}

export function useTaskSubmissions(taskId: string) {
  return useQuery({
    queryKey: ["task-submissions", taskId],
    queryFn: () => taskApi.getSubmissions(taskId),
    enabled: !!taskId,
  });
}

// Team hooks
export function useTeamMembers(taskId: string) {
  return useQuery({
    queryKey: ["team-members", taskId],
    queryFn: () => taskApi.getTeam(taskId),
    enabled: !!taskId,
  });
}

export function useJoinTeam() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (taskId: string) => taskApi.joinTeam(taskId),
    onSuccess: (_, taskId) => {
      qc.invalidateQueries({ queryKey: ["team-members", taskId] });
      qc.invalidateQueries({ queryKey: ["task", taskId] });
    },
  });
}

export function useLeaveTeam() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (taskId: string) => taskApi.leaveTeam(taskId),
    onSuccess: (_, taskId) => {
      qc.invalidateQueries({ queryKey: ["team-members", taskId] });
      qc.invalidateQueries({ queryKey: ["task", taskId] });
    },
  });
}

export function useTeamInviteLink(taskId: string) {
  return useQuery({
    queryKey: ["team-invite", taskId],
    queryFn: () => taskApi.getInviteLink(taskId),
    enabled: !!taskId,
  });
}
