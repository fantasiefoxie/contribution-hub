import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { discoveryApi } from "@/lib/discovery-api";

export function useDiscoveryFeed() {
  return useQuery({
    queryKey: ["discovery-feed"],
    queryFn: () => discoveryApi.discover(),
    staleTime: 30000,
  });
}

export function useRegionalBoard(initiativeId: string) {
  return useQuery({
    queryKey: ["regional-board", initiativeId],
    queryFn: () => discoveryApi.getRegionalBoard(initiativeId),
    enabled: !!initiativeId,
  });
}

export function useRecalculateScores() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => discoveryApi.recalculateScores(),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["discovery-feed"] }),
  });
}
