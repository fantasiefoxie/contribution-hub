import { useQuery } from "@tanstack/react-query";
import { useSession } from "@/hooks/use-session";
import { impactApi } from "@/lib/impact-api";

const defaultImpact = {
  tasks_completed: 0,
  total_score: 0,
  impact_metrics: {},
  initiatives_participated: 0,
  contributions: [],
};

export function useCommunityFeed(limit?: number) {
  return useQuery({
    queryKey: ["community-feed", limit],
    queryFn: async () => {
      try {
        return await impactApi.communityFeed(limit);
      } catch {
        return { feed: [] };
      }
    },
    refetchInterval: 30000,
  });
}

export function useMyImpact() {
  const userId = useSession();
  return useQuery({
    queryKey: ["my-impact", userId],
    queryFn: async () => {
      try {
        return await impactApi.myImpact();
      } catch {
        return defaultImpact;
      }
    },
    enabled: !!userId,
    retry: false,
    meta: { suppressError: true },
  });
}

export function useInitiativeProgress(id: string) {
  return useQuery({
    queryKey: ["initiative-progress", id],
    queryFn: () => impactApi.initiativeProgress(id),
    enabled: !!id,
    retry: false,
    meta: { suppressError: true },
  });
}

export function useNextMission(currentTaskId?: string) {
  const userId = useSession();
  return useQuery({
    queryKey: ["next-mission", currentTaskId],
    queryFn: async () => {
      try {
        return await impactApi.nextMission(currentTaskId);
      } catch {
        return { next_mission: null };
      }
    },
    enabled: !!userId,
  });
}
