import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useSession } from "@/hooks/use-session";
import { recommendationApi } from "@/lib/recommendation-api";

export function useMarketplaceFeed(params?: { passport?: boolean }) {
  return useQuery({
    queryKey: ["marketplace-feed", params],
    queryFn: () => recommendationApi.feed(params),
    staleTime: 5000,
  });
}

export function useNextMission(lastTaskId?: string) {
  return useQuery({
    queryKey: ["next-mission", lastTaskId],
    queryFn: () => recommendationApi.nextMission(lastTaskId),
    enabled: !!lastTaskId,
  });
}

export function useUserInterests() {
  const userId = useSession();
  return useQuery({
    queryKey: ["user-interests", userId],
    queryFn: () => recommendationApi.getInterests(),
    enabled: !!userId,
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
