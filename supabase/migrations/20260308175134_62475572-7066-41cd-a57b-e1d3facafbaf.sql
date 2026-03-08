
-- ============================================================
-- 1. ORGANIZATION SLUG AUTO-GENERATION TRIGGER
-- ============================================================
CREATE OR REPLACE FUNCTION public.auto_generate_org_slug()
RETURNS trigger
LANGUAGE plpgsql
SET search_path TO 'public'
AS $$
DECLARE
  _base_slug TEXT;
  _slug TEXT;
  _counter INT := 0;
BEGIN
  IF NEW.slug IS NULL OR NEW.slug = '' THEN
    _base_slug := public.generate_slug(NEW.name);
    _slug := _base_slug;
    LOOP
      EXIT WHEN NOT EXISTS (
        SELECT 1 FROM public.organizations WHERE slug = _slug AND id != NEW.id
      );
      _counter := _counter + 1;
      _slug := _base_slug || '-' || _counter;
    END LOOP;
    NEW.slug := _slug;
  END IF;

  INSERT INTO public.resource_aliases (resource_type, alias, canonical_id)
  VALUES ('organization', NEW.slug, NEW.id)
  ON CONFLICT (resource_type, alias) DO NOTHING;

  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_org_slug
  BEFORE INSERT ON public.organizations
  FOR EACH ROW
  EXECUTE FUNCTION public.auto_generate_org_slug();

-- ============================================================
-- 2. EVENT LEDGER IMMUTABILITY
-- ============================================================
CREATE OR REPLACE FUNCTION public.prevent_event_mutation()
RETURNS trigger
LANGUAGE plpgsql
SET search_path TO 'public'
AS $$
BEGIN
  RAISE EXCEPTION 'Events are immutable and cannot be modified';
END;
$$;

CREATE TRIGGER trg_prevent_event_update
  BEFORE UPDATE ON public.events
  FOR EACH ROW
  EXECUTE FUNCTION public.prevent_event_mutation();

CREATE TRIGGER trg_prevent_event_delete
  BEFORE DELETE ON public.events
  FOR EACH ROW
  EXECUTE FUNCTION public.prevent_event_mutation();

-- ============================================================
-- 3. EVENT LEDGER INDEXES
-- ============================================================
CREATE INDEX IF NOT EXISTS events_created_at_idx ON public.events (created_at);
CREATE INDEX IF NOT EXISTS events_entity_idx ON public.events (entity_id, created_at);
CREATE INDEX IF NOT EXISTS events_actor_idx ON public.events (actor_id, created_at);
CREATE INDEX IF NOT EXISTS events_type_idx ON public.events (type);

-- ============================================================
-- 4. SLUG INDEXES (ensure fast lookups)
-- ============================================================
CREATE UNIQUE INDEX IF NOT EXISTS initiatives_slug_idx ON public.initiatives (slug) WHERE slug IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS tasks_slug_idx ON public.tasks (slug) WHERE slug IS NOT NULL;
CREATE INDEX IF NOT EXISTS resource_aliases_lookup_idx ON public.resource_aliases (resource_type, alias);

-- ============================================================
-- 5. EVENT QUEUE TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS public.event_queue (
  id uuid PRIMARY KEY DEFAULT public.uuid_generate_v7(),
  event_id uuid NOT NULL,
  status text NOT NULL DEFAULT 'pending',
  attempts int NOT NULL DEFAULT 0,
  max_attempts int NOT NULL DEFAULT 5,
  last_error text,
  created_at timestamptz NOT NULL DEFAULT now(),
  processed_at timestamptz,
  next_retry_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS event_queue_pending_idx ON public.event_queue (status, next_retry_at) WHERE status = 'pending';
CREATE INDEX IF NOT EXISTS event_queue_event_id_idx ON public.event_queue (event_id);

ALTER TABLE public.event_queue ENABLE ROW LEVEL SECURITY;

-- ============================================================
-- 6. CONTRIBUTOR_STATS SUMMARY TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS public.contributor_stats (
  user_id uuid PRIMARY KEY,
  tasks_completed int NOT NULL DEFAULT 0,
  tasks_claimed int NOT NULL DEFAULT 0,
  validations_done int NOT NULL DEFAULT 0,
  contribution_score numeric NOT NULL DEFAULT 0,
  initiatives_joined int NOT NULL DEFAULT 0,
  last_activity_at timestamptz DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS contributor_score_idx ON public.contributor_stats (contribution_score DESC);

ALTER TABLE public.contributor_stats ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Contributor stats viewable by everyone"
  ON public.contributor_stats FOR SELECT
  TO authenticated
  USING (true);

CREATE POLICY "System can upsert contributor stats"
  ON public.contributor_stats FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "System can update contributor stats"
  ON public.contributor_stats FOR UPDATE
  TO authenticated
  USING (true);

-- ============================================================
-- 7. INITIATIVE_PROGRESS SUMMARY TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS public.initiative_progress (
  initiative_id uuid PRIMARY KEY,
  tasks_total int NOT NULL DEFAULT 0,
  tasks_completed int NOT NULL DEFAULT 0,
  tasks_open int NOT NULL DEFAULT 0,
  tasks_in_progress int NOT NULL DEFAULT 0,
  contributors_active int NOT NULL DEFAULT 0,
  validators_active int NOT NULL DEFAULT 0,
  completion_pct numeric NOT NULL DEFAULT 0,
  last_event_at timestamptz DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS initiative_progress_pct_idx ON public.initiative_progress (completion_pct DESC);

ALTER TABLE public.initiative_progress ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Initiative progress viewable by everyone"
  ON public.initiative_progress FOR SELECT
  TO authenticated
  USING (true);

CREATE POLICY "System can upsert initiative progress"
  ON public.initiative_progress FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "System can update initiative progress"
  ON public.initiative_progress FOR UPDATE
  TO authenticated
  USING (true);

-- ============================================================
-- 8. ORG_METRICS SUMMARY TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS public.org_metrics (
  org_id uuid PRIMARY KEY,
  total_tasks int NOT NULL DEFAULT 0,
  completed_tasks int NOT NULL DEFAULT 0,
  active_members int NOT NULL DEFAULT 0,
  total_contributions numeric NOT NULL DEFAULT 0,
  last_event_at timestamptz DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE public.org_metrics ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Org metrics viewable by everyone"
  ON public.org_metrics FOR SELECT
  TO authenticated
  USING (true);

CREATE POLICY "System can upsert org metrics"
  ON public.org_metrics FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "System can update org metrics"
  ON public.org_metrics FOR UPDATE
  TO authenticated
  USING (true);

-- ============================================================
-- 9. AUTO-ENQUEUE EVENTS TRIGGER
-- ============================================================
CREATE OR REPLACE FUNCTION public.enqueue_event()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
BEGIN
  INSERT INTO public.event_queue (event_id) VALUES (NEW.id);
  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_enqueue_event
  AFTER INSERT ON public.events
  FOR EACH ROW
  EXECUTE FUNCTION public.enqueue_event();

-- ============================================================
-- 10. ADDITIONAL PERFORMANCE INDEXES
-- ============================================================
CREATE INDEX IF NOT EXISTS tasks_status_idx ON public.tasks (status);
CREATE INDEX IF NOT EXISTS tasks_initiative_id_idx ON public.tasks (initiative_id) WHERE initiative_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS contributions_user_idx ON public.contributions (user_id, created_at);
CREATE INDEX IF NOT EXISTS validations_submission_idx ON public.validations (submission_id);
CREATE INDEX IF NOT EXISTS notifications_user_unread_idx ON public.notifications (user_id, created_at) WHERE read = false;
