import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { leadershipApi } from "@/lib/leadership-api";
import { useAuth } from "@/contexts/AuthContext";

export function useMyProgression() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["my-progression", user?.id],
    queryFn: () => leadershipApi.myProgression(),
    enabled: !!user,
    retry: (failureCount, error) => {
      if (error?.message?.includes("Unauthorized") || error?.message?.includes("401")) return false;
      return failureCount < 2;
    },
  });
}

export function useLeadershipOpportunities() {
  return useQuery({
    queryKey: ["leadership-opportunities"],
    queryFn: () => leadershipApi.opportunities(),
  });
}

export function useLeaderboard() {
  return useQuery({
    queryKey: ["leaderboard"],
    queryFn: () => leadershipApi.leaderboard(),
  });
}

export function useRequestPromotion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (targetRole: string) => leadershipApi.requestPromotion(targetRole),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["my-progression"] }),
  });
}

export function useConfirmPromotion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ candidateId, targetRole }: { candidateId: string; targetRole: string }) =>
      leadershipApi.confirmPromotion(candidateId, targetRole),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["leaderboard"] });
      qc.invalidateQueries({ queryKey: ["leadership-opportunities"] });
    },
  });
}
