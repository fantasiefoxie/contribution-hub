import { useQuery } from "@tanstack/react-query";
import { observabilityApi } from "@/lib/observability-api";

const mockPlatformHealth = {
  status: "healthy",
  uptime_pct: 99.8,
  active_users: 142,
  tasks_created_24h: 18,
  tasks_completed_24h: 12,
  avg_validation_time_mins: 22,
  event_queue_size: 3,
  error_rate_pct: 0.2,
};

const mockValidatorMonitoring = {
  validators: [
    { user_id: "user-3", display_name: "James Park", validations_24h: 8, avg_time_mins: 15, approval_rate: 0.88 },
    { user_id: "user-5", display_name: "Priya Sharma", validations_24h: 5, avg_time_mins: 20, approval_rate: 0.92 },
  ],
};

const mockFraudSignals = { signals: [], flagged_count: 0 };

const mockEventStream = {
  events: [
    { id: "ev-1", type: "TaskCreated", entity_type: "task", actor_id: "user-1", created_at: new Date(Date.now() - 300000).toISOString(), payload: { title: "Community Garden Cleanup" } },
    { id: "ev-2", type: "TaskClaimed", entity_type: "task", actor_id: "user-2", created_at: new Date(Date.now() - 600000).toISOString(), payload: { title: "Survey Local Businesses" } },
    { id: "ev-3", type: "ValidationApproved", entity_type: "submission", actor_id: "user-3", created_at: new Date(Date.now() - 1200000).toISOString(), payload: {} },
    { id: "ev-4", type: "InitiativeJoined", entity_type: "initiative", actor_id: "user-4", created_at: new Date(Date.now() - 1800000).toISOString(), payload: { title: "River Cleanup Campaign" } },
  ],
};

const mockTransparency = {
  total_tasks: 156,
  total_completed: 89,
  total_contributors: 142,
  total_organizations: 2,
  total_initiatives: 3,
  validation_accuracy: 0.94,
  avg_completion_time_hrs: 4.2,
  top_categories: [
    { category: "environment", count: 45 },
    { category: "community", count: 38 },
    { category: "education", count: 22 },
    { category: "research", count: 18 },
  ],
};

const mockAiMetrics = {
  total_suggestions: 48,
  accepted: 32,
  rejected: 10,
  pending: 6,
  acceptance_rate: 0.67,
  avg_confidence: 0.82,
  total_cost_usd: 1.24,
  models_used: [
    { model_id: "gemini-2.5-flash", invocations: 35, avg_latency_ms: 320 },
    { model_id: "gpt-5-mini", invocations: 13, avg_latency_ms: 480 },
  ],
};

export function usePlatformHealth() {
  return useQuery({
    queryKey: ["platform-health"],
    queryFn: async () => {
      try {
        return await observabilityApi.platformHealth();
      } catch {
        return mockPlatformHealth;
      }
    },
    refetchInterval: 15000,
    retry: false,
  });
}

export function useInitiativeHealth(id: string) {
  return useQuery({
    queryKey: ["initiative-health", id],
    queryFn: async () => {
      try {
        return await observabilityApi.initiativeHealth(id);
      } catch {
        return { status: "healthy", completion_pct: 42, bottlenecks: [] };
      }
    },
    enabled: !!id,
    retry: false,
  });
}

export function useValidatorMonitoring() {
  return useQuery({
    queryKey: ["validator-monitoring"],
    queryFn: async () => {
      try {
        return await observabilityApi.validatorMonitoring();
      } catch {
        return mockValidatorMonitoring;
      }
    },
    retry: false,
  });
}

export function useFraudSignals() {
  return useQuery({
    queryKey: ["fraud-signals"],
    queryFn: async () => {
      try {
        return await observabilityApi.fraudSignals();
      } catch {
        return mockFraudSignals;
      }
    },
    retry: false,
  });
}

export function useIntegrityScore(userId: string) {
  return useQuery({
    queryKey: ["integrity-score", userId],
    queryFn: async () => {
      try {
        return await observabilityApi.integrityScore(userId);
      } catch {
        return { score: 87, factors: { consistency: 0.92, timeliness: 0.85, quality: 0.88 } };
      }
    },
    enabled: !!userId,
    retry: false,
  });
}

export function useEventStream(limit?: number) {
  return useQuery({
    queryKey: ["event-stream", limit],
    queryFn: async () => {
      try {
        return await observabilityApi.eventStream(limit);
      } catch {
        return mockEventStream;
      }
    },
    refetchInterval: 10000,
    retry: false,
  });
}

export function useTransparency() {
  return useQuery({
    queryKey: ["transparency"],
    queryFn: async () => {
      try {
        return await observabilityApi.transparency();
      } catch {
        return mockTransparency;
      }
    },
    refetchInterval: 60000,
    retry: false,
  });
}

export function useAiMetrics() {
  return useQuery({
    queryKey: ["ai-metrics"],
    queryFn: async () => {
      try {
        return await observabilityApi.aiMetrics();
      } catch {
        return mockAiMetrics;
      }
    },
    refetchInterval: 30000,
    retry: false,
  });
}
