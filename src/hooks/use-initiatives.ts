import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { initiativeApi, type CreateInitiativeInput, type GenerateTasksInput } from "@/lib/initiative-api";

export function useInitiatives(params?: { status?: string; is_micro?: string }) {
  return useQuery({
    queryKey: ["initiatives", params],
    queryFn: () => initiativeApi.list(params),
  });
}

export function useInitiative(id: string) {
  return useQuery({
    queryKey: ["initiative", id],
    queryFn: () => initiativeApi.get(id),
    enabled: !!id,
    retry: false,
    meta: { suppressError: true },
  });
}

export function useCreateInitiative() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateInitiativeInput) => initiativeApi.create(input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["initiatives"] }),
  });
}

export function useUpdateInitiative() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: Partial<CreateInitiativeInput & { status: string }> }) =>
      initiativeApi.update(id, input),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: ["initiatives"] });
      qc.invalidateQueries({ queryKey: ["initiative", id] });
    },
  });
}

export function useCreateProject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ initiativeId, input }: { initiativeId: string; input: { title: string; description?: string; sort_order?: number } }) =>
      initiativeApi.createProject(initiativeId, input),
    onSuccess: (_, { initiativeId }) => qc.invalidateQueries({ queryKey: ["initiative", initiativeId] }),
  });
}

export function useGenerateTasks() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ initiativeId, input }: { initiativeId: string; input: GenerateTasksInput }) =>
      initiativeApi.generateTasks(initiativeId, input),
    onSuccess: (_, { initiativeId }) => {
      qc.invalidateQueries({ queryKey: ["initiative", initiativeId] });
      qc.invalidateQueries({ queryKey: ["tasks"] });
    },
  });
}

export function useJoinInitiative() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => initiativeApi.join(id),
    onSuccess: (_, id) => qc.invalidateQueries({ queryKey: ["initiative", id] }),
  });
}

export function usePromoteInitiative() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => initiativeApi.promote(id),
    onSuccess: (_, id) => {
      qc.invalidateQueries({ queryKey: ["initiatives"] });
      qc.invalidateQueries({ queryKey: ["initiative", id] });
    },
  });
}
