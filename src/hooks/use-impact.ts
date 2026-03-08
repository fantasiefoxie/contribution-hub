import { useQuery } from "@tanstack/react-query";
import { impactApi } from "@/lib/impact-api";

export function useCommunityFeed(limit?: number) {
  return useQuery({
    queryKey: ["community-feed", limit],
    queryFn: () => impactApi.communityFeed(limit),
    refetchInterval: 30000, // refresh every 30s
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
  });
}
