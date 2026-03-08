import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { leadershipApi } from "@/lib/leadership-api";
import { useSession } from "@/hooks/use-session";

export function useMyProgression() {
  const userId = useSession();
  return useQuery({
    queryKey: ["my-progression", userId],
    queryFn: () => leadershipApi.myProgression(),
    enabled: !!userId,
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
