
-- Fix: Replace overly permissive UPDATE policies with service-role-only patterns
-- and add restrictive policies to event_queue

-- Drop permissive UPDATE policies on summary tables
DROP POLICY IF EXISTS "System can update contributor stats" ON public.contributor_stats;
DROP POLICY IF EXISTS "System can update initiative progress" ON public.initiative_progress;
DROP POLICY IF EXISTS "System can update org metrics" ON public.org_metrics;

-- Replace with no-op UPDATE policies (only service role bypasses RLS)
CREATE POLICY "No direct updates to contributor stats"
  ON public.contributor_stats FOR UPDATE
  TO authenticated
  USING (false);

CREATE POLICY "No direct updates to initiative progress"
  ON public.initiative_progress FOR UPDATE
  TO authenticated
  USING (false);

CREATE POLICY "No direct updates to org metrics"
  ON public.org_metrics FOR UPDATE
  TO authenticated
  USING (false);

-- Drop permissive INSERT policies on summary tables (service role only)
DROP POLICY IF EXISTS "System can upsert contributor stats" ON public.contributor_stats;
DROP POLICY IF EXISTS "System can upsert initiative progress" ON public.initiative_progress;
DROP POLICY IF EXISTS "System can upsert org metrics" ON public.org_metrics;

CREATE POLICY "No direct inserts to contributor stats"
  ON public.contributor_stats FOR INSERT
  TO authenticated
  WITH CHECK (false);

CREATE POLICY "No direct inserts to initiative progress"
  ON public.initiative_progress FOR INSERT
  TO authenticated
  WITH CHECK (false);

CREATE POLICY "No direct inserts to org metrics"
  ON public.org_metrics FOR INSERT
  TO authenticated
  WITH CHECK (false);

-- Event queue: service-role only (no authenticated user access)
CREATE POLICY "No user access to event queue"
  ON public.event_queue FOR SELECT
  TO authenticated
  USING (false);
