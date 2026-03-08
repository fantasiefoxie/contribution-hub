import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/contexts/AuthContext";
import { protocolApi } from "@/lib/protocol-api";
import { mockCredentials } from "@/lib/mock-data";

const mockDocs = {
  version: "1.0.0",
  endpoints: [
    { method: "GET", path: "/tasks", description: "List tasks" },
    { method: "POST", path: "/tasks", description: "Create a task" },
    { method: "GET", path: "/tasks/:id", description: "Get task details" },
  ],
};

export function useProtocolDocs() {
  return useQuery({
    queryKey: ["protocol-docs"],
    queryFn: async () => {
      try {
        return await protocolApi.getDocs();
      } catch {
        return mockDocs;
      }
    },
    staleTime: 60 * 60 * 1000,
    retry: false,
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
    queryFn: async () => {
      try {
        return await protocolApi.listCredentials(params);
      } catch {
        return { credentials: mockCredentials };
      }
    },
    retry: false,
  });
}

export function useCredential(id: string) {
  return useQuery({
    queryKey: ["credential", id],
    queryFn: async () => {
      try {
        return await protocolApi.getCredential(id);
      } catch {
        const mock = mockCredentials.find((c) => c.id === id);
        return { credential: mock ?? null };
      }
    },
    enabled: !!id,
    retry: false,
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
    queryFn: async () => {
      try {
        return await protocolApi.listRoles(userId);
      } catch {
        return { roles: [] };
      }
    },
    retry: false,
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
    queryFn: async () => {
      try {
        return await protocolApi.getInitiativePriority(initiativeId);
      } catch {
        return { priority_score: 75, volunteer_target: 100, featured: false };
      }
    },
    enabled: !!initiativeId,
    retry: false,
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
