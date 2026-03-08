import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { leadershipApi } from "@/lib/leadership-api";
import { useAuth } from "@/contexts/AuthContext";
import { mockLeadershipData, mockProfiles } from "@/lib/mock-data";

const mockProgression = {
  current_role: "member",
  tasks_completed: 24,
  validations_done: 8,
  contribution_score: 1240,
  next_role: "validator",
  requirements: { tasks_completed: 30, validations_done: 10, contribution_score: 1500 },
  progress_pct: 72,
};

const mockOpportunities = {
  opportunities: [
    { role: "validator", requirements_met: false, missing: ["6 more tasks", "2 more validations"] },
    { role: "moderator", requirements_met: false, missing: ["Become a validator first"] },
  ],
};

const mockLeaderboard = {
  leaderboard: mockProfiles.map((p, i) => ({
    user_id: p.id,
    display_name: p.display_name,
    avatar_url: p.avatar_url,
    contribution_score: 1500 - i * 200,
    tasks_completed: 30 - i * 5,
    rank: i + 1,
  })),
};

export function useMyProgression() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ["my-progression", user?.id],
    queryFn: async () => {
      try {
        return await leadershipApi.myProgression();
      } catch {
        return mockProgression;
      }
    },
    enabled: !!user,
    retry: false,
  });
}

export function useLeadershipOpportunities() {
  return useQuery({
    queryKey: ["leadership-opportunities"],
    queryFn: async () => {
      try {
        return await leadershipApi.opportunities();
      } catch {
        return mockOpportunities;
      }
    },
    retry: false,
  });
}

export function useLeaderboard() {
  return useQuery({
    queryKey: ["leaderboard"],
    queryFn: async () => {
      try {
        return await leadershipApi.leaderboard();
      } catch {
        return mockLeaderboard;
      }
    },
    retry: false,
  });
}

export function useRequestPromotion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (targetRole: string) => leadershipApi.requestPromotion(targetRole),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["my-progression"] }),
  });
}

export function useConfirmPromotion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ candidateId, targetRole }: { candidateId: string; targetRole: string }) =>
      leadershipApi.confirmPromotion(candidateId, targetRole),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["leaderboard"] });
      qc.invalidateQueries({ queryKey: ["leadership-opportunities"] });
    },
  });
}
