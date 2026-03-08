import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/contexts/AuthContext";
import { protocolApi } from "@/lib/protocol-api";

export function useProtocolDocs() {
  return useQuery({
    queryKey: ["protocol-docs"],
    queryFn: () => protocolApi.getDocs(),
    staleTime: 60 * 60 * 1000,
  });
}

// ── API Keys ──
export function useApiKeys() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["api-keys", user?.id],
    queryFn: async () => {
      try { return await protocolApi.listKeys(); }
      catch { return { api_keys: [] }; }
    },
    enabled: !!user,
    retry: false,
    meta: { suppressError: true },
  });
}

export function useCreateApiKey() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { name: string; scopes?: string[]; org_id?: string }) => protocolApi.createKey(input),
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

// ── Webhooks ──
export function useWebhooks() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["webhooks", user?.id],
    queryFn: async () => {
      try { return await protocolApi.listWebhooks(); }
      catch { return { webhooks: [] }; }
    },
    enabled: !!user,
    retry: false,
    meta: { suppressError: true },
  });
}

export function useCreateWebhook() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { api_key_id: string; url: string; events: string[] }) => protocolApi.createWebhook(input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["webhooks"] }),
  });
}

// ── Credentials ──
export function useCredentials(params?: { user_id?: string; type?: string }) {
  return useQuery({
    queryKey: ["credentials", params],
    queryFn: () => protocolApi.listCredentials(params),
  });
}

export function useCredential(id: string) {
  return useQuery({
    queryKey: ["credential", id],
    queryFn: () => protocolApi.getCredential(id),
    enabled: !!id,
  });
}

export function useIssueCredential() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { target_user_id: string; title: string; type?: string; initiative_id?: string; org_id?: string; description?: string }) =>
      protocolApi.issueCredential(input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["credentials"] }),
  });
}

export function useRevokeCredential() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => protocolApi.revokeCredential(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["credentials"] }),
  });
}

// ── Roles ──
export function useRoles(userId?: string) {
  return useQuery({
    queryKey: ["roles", userId],
    queryFn: () => protocolApi.listRoles(userId),
  });
}

export function useGrantRole() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { target_user_id: string; role: string }) => protocolApi.grantRole(input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["roles"] }),
  });
}

export function useRevokeRole() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { target_user_id: string; role: string }) => protocolApi.revokeRole(input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["roles"] }),
  });
}

// ── Initiative Join & Priority ──
export function useJoinInitiativeProtocol() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (initiativeId: string) => protocolApi.joinInitiative(initiativeId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["initiatives"] });
      qc.invalidateQueries({ queryKey: ["initiative"] });
    },
  });
}

export function useInitiativePriority(initiativeId: string) {
  return useQuery({
    queryKey: ["initiative-priority", initiativeId],
    queryFn: () => protocolApi.getInitiativePriority(initiativeId),
    enabled: !!initiativeId,
  });
}

export function useUpdateInitiativePriority() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: { priority_score?: number; volunteer_target?: number; featured?: boolean } }) =>
      protocolApi.updateInitiativePriority(id, input),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["initiative-priority"] });
      qc.invalidateQueries({ queryKey: ["initiatives"] });
    },
  });
}
