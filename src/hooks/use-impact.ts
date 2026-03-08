import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/contexts/AuthContext";
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
  const { user } = useAuth();
  return useQuery({
    queryKey: ["my-impact", user?.id],
    queryFn: async () => {
      try {
        return await impactApi.myImpact();
      } catch {
        return defaultImpact;
      }
    },
    enabled: !!user,
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
  const { user } = useAuth();
  return useQuery({
    queryKey: ["next-mission", currentTaskId],
    queryFn: async () => {
      try {
        return await impactApi.nextMission(currentTaskId);
      } catch {
        return { next_mission: null };
      }
    },
    enabled: !!user,
  });
}
