import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { campaignOpsApi } from "@/lib/campaign-ops-api";
import { mockTasks, mockInitiativeProgress } from "@/lib/mock-data";

const mockDashboard = (initId: string) => ({
  initiative_id: initId,
  progress: mockInitiativeProgress[initId] ?? { tasks_total: 0, tasks_completed: 0, completion_pct: 0 },
  regions: [
    { id: "reg-1", name: "North Zone", initiative_id: initId, sort_order: 0, task_count: 12 },
    { id: "reg-2", name: "South Zone", initiative_id: initId, sort_order: 1, task_count: 8 },
  ],
  recent_activity: [],
});

const mockRegions = [
  { id: "reg-1", name: "North Zone", description: "Northern area of the city", sort_order: 0 },
  { id: "reg-2", name: "South Zone", description: "Southern area of the city", sort_order: 1 },
];

export function useCampaignDashboard(initiativeId: string) {
  return useQuery({
    queryKey: ["campaign-dashboard", initiativeId],
    queryFn: async () => {
      try {
        return await campaignOpsApi.dashboard(initiativeId);
      } catch {
        return mockDashboard(initiativeId);
      }
    },
    enabled: !!initiativeId,
    refetchInterval: 15000,
    retry: false,
  });
}

export function useRegions(initiativeId: string) {
  return useQuery({
    queryKey: ["regions", initiativeId],
    queryFn: async () => {
      try {
        return await campaignOpsApi.listRegions(initiativeId);
      } catch {
        return { regions: mockRegions };
      }
    },
    enabled: !!initiativeId,
    retry: false,
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
    queryFn: async () => {
      try {
        return await campaignOpsApi.capacityCheck();
      } catch {
        return { can_claim: true, active_tasks: 0, max_tasks: 3, remaining: 3 };
      }
    },
    staleTime: 10000,
    retry: false,
  });
}

export function useRateCheck() {
  return useQuery({
    queryKey: ["rate-check"],
    queryFn: async () => {
      try {
        return await campaignOpsApi.rateCheck();
      } catch {
        return { allowed: true, remaining: 10, limit: 10, window_seconds: 60 };
      }
    },
    staleTime: 3000,
    retry: false,
  });
}

export function useRegionalFeed(params?: { region_id?: string; pool?: string; initiative_id?: string }) {
  return useQuery({
    queryKey: ["regional-feed", params],
    queryFn: async () => {
      try {
        return await campaignOpsApi.feed(params);
      } catch {
        return { tasks: mockTasks.slice(0, 5) };
      }
    },
    staleTime: 5000,
    retry: false,
  });
}
