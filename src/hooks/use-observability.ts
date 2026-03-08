import { useQuery } from "@tanstack/react-query";
import { observabilityApi } from "@/lib/observability-api";

export function usePlatformHealth() {
  return useQuery({
    queryKey: ["platform-health"],
    queryFn: () => observabilityApi.platformHealth(),
    refetchInterval: 15000,
  });
}

export function useInitiativeHealth(id: string) {
  return useQuery({
    queryKey: ["initiative-health", id],
    queryFn: () => observabilityApi.initiativeHealth(id),
    enabled: !!id,
  });
}

export function useValidatorMonitoring() {
  return useQuery({
    queryKey: ["validator-monitoring"],
    queryFn: () => observabilityApi.validatorMonitoring(),
  });
}

export function useFraudSignals() {
  return useQuery({
    queryKey: ["fraud-signals"],
    queryFn: () => observabilityApi.fraudSignals(),
  });
}

export function useIntegrityScore(userId: string) {
  return useQuery({
    queryKey: ["integrity-score", userId],
    queryFn: () => observabilityApi.integrityScore(userId),
    enabled: !!userId,
  });
}

export function useEventStream(limit?: number) {
  return useQuery({
    queryKey: ["event-stream", limit],
    queryFn: () => observabilityApi.eventStream(limit),
    refetchInterval: 10000,
  });
}

export function useTransparency() {
  return useQuery({
    queryKey: ["transparency"],
    queryFn: () => observabilityApi.transparency(),
    refetchInterval: 60000,
  });
}
