import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/contexts/AuthContext";
import { recommendationApi } from "@/lib/recommendation-api";
import { mockRecommendationFeed, mockUserInterests, mockNextMission } from "@/lib/mock-data";

export function useMarketplaceFeed(params?: { passport?: boolean }) {
  return useQuery({
    queryKey: ["marketplace-feed", params],
    queryFn: async () => {
      try {
        const data = await recommendationApi.feed(params);
        // If all arrays are empty, use mock data
        if (!data?.recommended?.length && !data?.nearby?.length && !data?.priority?.length && !data?.online?.length) {
          return mockRecommendationFeed;
        }
        return data;
      } catch {
        return mockRecommendationFeed;
      }
    },
    staleTime: 5000,
    retry: false,
  });
}

export function useNextMission(lastTaskId?: string) {
  return useQuery({
    queryKey: ["next-mission", lastTaskId],
    queryFn: async () => {
      try {
        return await recommendationApi.nextMission(lastTaskId);
      } catch {
        return mockNextMission;
      }
    },
    enabled: !!lastTaskId,
    retry: false,
  });
}

export function useUserInterests() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["user-interests", user?.id],
    queryFn: async () => {
      try {
        return await recommendationApi.getInterests();
      } catch {
        return mockUserInterests;
      }
    },
    enabled: !!user,
    retry: false,
    meta: { suppressError: true },
  });
}

export function useUpdateInterests() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (interests: string[]) => recommendationApi.updateInterests(interests),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["user-interests"] });
      qc.invalidateQueries({ queryKey: ["marketplace-feed"] });
    },
  });
}
