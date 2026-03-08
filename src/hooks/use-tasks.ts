import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { taskApi, type CreateTaskInput, type TaskUpdateInput, type TaskSubmissionInput } from "@/lib/task-api";
import { getMockTask, getMockTeam, getMockSubmissions } from "@/lib/mock-data";

export function useTasks(params?: { status?: string; visibility?: string }) {
  return useQuery({
    queryKey: ["tasks", params],
    queryFn: async () => {
      try {
        return await taskApi.list(params);
      } catch {
        // Fallback handled in consuming components via mock data
        return { tasks: [] };
      }
    },
    retry: false,
  });
}

export function useTask(taskId: string, options?: { retry?: boolean | number }) {
  return useQuery({
    queryKey: ["task", taskId],
    queryFn: async () => {
      try {
        return await taskApi.get(taskId);
      } catch {
        // Graceful fallback to mock data
        const mock = getMockTask(taskId);
        if (mock) return { task: mock };
        return { task: null };
      }
    },
    enabled: !!taskId,
    retry: options?.retry ?? false,
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
    queryFn: async () => {
      try {
        return await taskApi.getSubmissions(taskId);
      } catch {
        return { submissions: getMockSubmissions(taskId) };
      }
    },
    enabled: !!taskId,
    retry: false,
  });
}

// Team hooks
export function useTeamMembers(taskId: string) {
  return useQuery({
    queryKey: ["team-members", taskId],
    queryFn: async () => {
      try {
        return await taskApi.getTeam(taskId);
      } catch {
        return getMockTeam(taskId);
      }
    },
    enabled: !!taskId,
    retry: false,
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
    queryFn: async () => {
      try {
        return await taskApi.getInviteLink(taskId);
      } catch {
        return { invite_url: `${window.location.origin}/task/${taskId}?join=team` };
      }
    },
    enabled: !!taskId,
    retry: false,
  });
}
