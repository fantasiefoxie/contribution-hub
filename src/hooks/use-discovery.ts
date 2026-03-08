import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { discoveryApi } from "@/lib/discovery-api";
import { mockInitiatives } from "@/lib/mock-data";

const mockDiscoveryFeed = {
  featured: mockInitiatives.slice(0, 1),
  trending: mockInitiatives,
  nearby: mockInitiatives.slice(0, 2),
  micro: [],
};

export function useDiscoveryFeed() {
  return useQuery({
    queryKey: ["discovery-feed"],
    queryFn: async () => {
      try {
        const data = await discoveryApi.discover();
        if (!data?.featured?.length && !data?.trending?.length) return mockDiscoveryFeed;
        return data;
      } catch {
        return mockDiscoveryFeed;
      }
    },
    staleTime: 30000,
    retry: false,
  });
}

export function useRegionalBoard(initiativeId: string) {
  return useQuery({
    queryKey: ["regional-board", initiativeId],
    queryFn: async () => {
      try {
        return await discoveryApi.getRegionalBoard(initiativeId);
      } catch {
        return { regions: [], tasks_by_region: {} };
      }
    },
    enabled: !!initiativeId,
    retry: false,
  });
}

export function useRecalculateScores() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => discoveryApi.recalculateScores(),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["discovery-feed"] }),
  });
}
