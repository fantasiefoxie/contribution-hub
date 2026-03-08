import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { protocolApi } from "@/lib/protocol-api";

export function useProtocolDocs() {
  return useQuery({
    queryKey: ["protocol-docs"],
    queryFn: () => protocolApi.getDocs(),
    staleTime: 60 * 60 * 1000,
  });
}

export function useApiKeys() {
  return useQuery({
    queryKey: ["api-keys"],
    queryFn: () => protocolApi.listKeys(),
  });
}

export function useCreateApiKey() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { name: string; scopes?: string[]; org_id?: string }) =>
      protocolApi.createKey(input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["api-keys"] }),
  });
}

export function useRevokeApiKey() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (keyId: string) => protocolApi.revokeKey(keyId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["api-keys"] }),
  });
}

export function useWebhooks() {
  return useQuery({
    queryKey: ["webhooks"],
    queryFn: () => protocolApi.listWebhooks(),
  });
}

export function useCreateWebhook() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { api_key_id: string; url: string; events: string[] }) =>
      protocolApi.createWebhook(input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["webhooks"] }),
  });
}
