import { useQuery } from "@tanstack/react-query";
import { impactApi } from "@/lib/impact-api";

export function useCommunityFeed(limit?: number) {
  return useQuery({
    queryKey: ["community-feed", limit],
    queryFn: () => impactApi.communityFeed(limit),
    refetchInterval: 30000,
  });
}

export function useMyImpact() {
  return useQuery({
    queryKey: ["my-impact"],
    queryFn: () => impactApi.myImpact(),
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
  return useQuery({
    queryKey: ["next-mission", currentTaskId],
    queryFn: () => impactApi.nextMission(currentTaskId),
    enabled: true,
  });
}
