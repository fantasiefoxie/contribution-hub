import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { taskApi, type ValidationActionInput, type AppealInput, type AppealResolveInput } from "@/lib/task-api";

export function usePendingValidations() {
  return useQuery({
    queryKey: ["validations", "pending"],
    queryFn: () => taskApi.getPendingValidations(),
  });
}

export function useApproveSubmission() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ submissionId, input }: { submissionId: string; input?: ValidationActionInput }) =>
      taskApi.approveSubmission(submissionId, input),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["validations"] });
      qc.invalidateQueries({ queryKey: ["tasks"] });
    },
  });
}

export function useRejectSubmission() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ submissionId, input }: { submissionId: string; input: ValidationActionInput }) =>
      taskApi.rejectSubmission(submissionId, input),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["validations"] });
      qc.invalidateQueries({ queryKey: ["tasks"] });
    },
  });
}

export function useRequestClarification() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ submissionId, input }: { submissionId: string; input: ValidationActionInput }) =>
      taskApi.requestClarification(submissionId, input),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["validations"] });
    },
  });
}

export function useCreateAppeal() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: AppealInput) => taskApi.createAppeal(input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["validations"] }),
  });
}

export function useResolveAppeal() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ appealId, input }: { appealId: string; input: AppealResolveInput }) =>
      taskApi.resolveAppeal(appealId, input),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["validations"] });
      qc.invalidateQueries({ queryKey: ["tasks"] });
    },
  });
}
