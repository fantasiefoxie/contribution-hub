import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/contexts/AuthContext";
import { impactApi } from "@/lib/impact-api";
import { mockMyImpact, mockCommunityFeed, mockNextMission as mockNext } from "@/lib/mock-data";

export function useCommunityFeed(limit?: number) {
  return useQuery({
    queryKey: ["community-feed", limit],
    queryFn: async () => {
      try {
        const data = await impactApi.communityFeed(limit);
        if (!data?.feed?.length) return mockCommunityFeed;
        return data;
      } catch {
        return mockCommunityFeed;
      }
    },
    refetchInterval: 30000,
    retry: false,
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
        return mockMyImpact;
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
    queryFn: async () => {
      try {
        return await impactApi.initiativeProgress(id);
      } catch {
        // Return a sensible default
        return { completion_pct: 0, tasks_total: 0, tasks_completed: 0, contributors_active: 0 };
      }
    },
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
        return mockNext;
      }
    },
    enabled: !!user,
    retry: false,
  });
}
