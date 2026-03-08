import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { campaignOpsApi } from "@/lib/campaign-ops-api";

export function useCampaignDashboard(initiativeId: string) {
  return useQuery({
    queryKey: ["campaign-dashboard", initiativeId],
    queryFn: () => campaignOpsApi.dashboard(initiativeId),
    enabled: !!initiativeId,
    refetchInterval: 15000,
  });
}

export function useRegions(initiativeId: string) {
  return useQuery({
    queryKey: ["regions", initiativeId],
    queryFn: () => campaignOpsApi.listRegions(initiativeId),
    enabled: !!initiativeId,
  });
}

export function useCreateRegion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { initiative_id: string; name: string; description?: string; sort_order?: number }) =>
      campaignOpsApi.createRegion(input),
    onSuccess: (_, { initiative_id }) => {
      qc.invalidateQueries({ queryKey: ["regions", initiative_id] });
      qc.invalidateQueries({ queryKey: ["campaign-dashboard", initiative_id] });
    },
  });
}

export function useCapacityCheck() {
  return useQuery({
    queryKey: ["capacity-check"],
    queryFn: () => campaignOpsApi.capacityCheck(),
    staleTime: 10000,
  });
}

export function useRateCheck() {
  return useQuery({
    queryKey: ["rate-check"],
    queryFn: () => campaignOpsApi.rateCheck(),
    staleTime: 3000,
  });
}

export function useRegionalFeed(params?: { region_id?: string; pool?: string; initiative_id?: string }) {
  return useQuery({
    queryKey: ["regional-feed", params],
    queryFn: () => campaignOpsApi.feed(params),
    staleTime: 5000,
  });
}
