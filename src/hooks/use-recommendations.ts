import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { supabase } from "@/integrations/supabase/client";
import { recommendationApi } from "@/lib/recommendation-api";

function useSession() {
  const [userId, setUserId] = useState<string | null>(null);
  useEffect(() => {
    supabase.auth.getSession().then(({ data }) => {
      setUserId(data.session?.user?.id ?? null);
    });
    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      setUserId(session?.user?.id ?? null);
    });
    return () => subscription.unsubscribe();
  }, []);
  return userId;
}

export function useMarketplaceFeed(params?: { passport?: boolean }) {
  return useQuery({
    queryKey: ["marketplace-feed", params],
    queryFn: () => recommendationApi.feed(params),
    staleTime: 5000,
  });
}

export function useNextMission(lastTaskId?: string) {
  return useQuery({
    queryKey: ["next-mission", lastTaskId],
    queryFn: () => recommendationApi.nextMission(lastTaskId),
    enabled: !!lastTaskId,
  });
}

export function useUserInterests() {
  const userId = useSession();
  return useQuery({
    queryKey: ["user-interests", userId],
    queryFn: () => recommendationApi.getInterests(),
    enabled: !!userId,
    retry: false,
    meta: { suppressError: true },
  });
}

export function useUpdateInterests() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (interests: string[]) => recommendationApi.updateInterests(interests),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["user-interests"] });
      qc.invalidateQueries({ queryKey: ["marketplace-feed"] });
    },
  });
}
