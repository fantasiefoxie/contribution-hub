
-- ═══ EVENT CONTRACT: Add version column ═══
ALTER TABLE public.events ADD COLUMN IF NOT EXISTS version integer NOT NULL DEFAULT 1;

-- ═══ IDEMPOTENCY KEYS TABLE ═══
CREATE TABLE public.idempotency_keys (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  key text NOT NULL,
  user_id uuid NOT NULL,
  operation text NOT NULL,
  result jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  expires_at timestamptz NOT NULL DEFAULT (now() + interval '24 hours'),
  UNIQUE(key, user_id, operation)
);
ALTER TABLE public.idempotency_keys ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can view own idempotency keys" ON public.idempotency_keys
  FOR SELECT TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "Users can insert own idempotency keys" ON public.idempotency_keys
  FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

-- ═══ RATE LIMITING TABLE ═══
CREATE TABLE public.rate_limit_entries (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  identifier text NOT NULL,
  action text NOT NULL,
  window_start timestamptz NOT NULL DEFAULT now(),
  count integer NOT NULL DEFAULT 1,
  UNIQUE(identifier, action, window_start)
);
ALTER TABLE public.rate_limit_entries ENABLE ROW LEVEL SECURITY;
-- Service role only, no user access needed

-- ═══ PERMISSION SCOPES TABLE ═══
CREATE TABLE public.role_permissions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  role text NOT NULL,
  scope text NOT NULL,
  UNIQUE(role, scope)
);
ALTER TABLE public.role_permissions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone can read role permissions" ON public.role_permissions
  FOR SELECT USING (true);

-- ═══ UNIQUE CONSTRAINTS ═══
-- Prevent duplicate active claims on same task by same user
CREATE UNIQUE INDEX IF NOT EXISTS idx_task_claims_active_user_task
  ON public.task_claims (task_id, user_id) WHERE status = 'active';

-- Prevent duplicate org membership
CREATE UNIQUE INDEX IF NOT EXISTS idx_org_members_unique
  ON public.org_members (org_id, user_id);

-- Prevent duplicate initiative participation
CREATE UNIQUE INDEX IF NOT EXISTS idx_initiative_participants_unique
  ON public.initiative_participants (initiative_id, user_id);

-- ═══ PERFORMANCE INDEXES ═══
CREATE INDEX IF NOT EXISTS idx_events_type_created ON public.events (type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_events_entity ON public.events (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_events_actor ON public.events (actor_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_tasks_status ON public.tasks (status);
CREATE INDEX IF NOT EXISTS idx_tasks_initiative ON public.tasks (initiative_id) WHERE initiative_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_org ON public.tasks (org_id) WHERE org_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_created_by ON public.tasks (created_by);

CREATE INDEX IF NOT EXISTS idx_contributions_user ON public.contributions (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_contributions_task ON public.contributions (task_id);

CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON public.notifications (user_id, created_at DESC) WHERE read = false;

CREATE INDEX IF NOT EXISTS idx_validations_submission ON public.validations (submission_id);
CREATE INDEX IF NOT EXISTS idx_validations_validator ON public.validations (validator_id, decided_at DESC);

CREATE INDEX IF NOT EXISTS idx_task_submissions_task ON public.task_submissions (task_id, submitted_at DESC);
CREATE INDEX IF NOT EXISTS idx_task_submissions_user ON public.task_submissions (user_id);

CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON public.audit_logs (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON public.audit_logs (actor_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_api_keys_prefix ON public.api_keys (key_prefix) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_api_keys_user ON public.api_keys (user_id);

CREATE INDEX IF NOT EXISTS idx_webhook_subs_active ON public.webhook_subscriptions (is_active) WHERE is_active = true;

CREATE INDEX IF NOT EXISTS idx_rate_limit_lookup ON public.rate_limit_entries (identifier, action, window_start);

-- ═══ IMMUTABILITY: Prevent updates/deletes on contributions ═══
CREATE OR REPLACE FUNCTION public.prevent_contribution_mutation()
RETURNS trigger
LANGUAGE plpgsql
SET search_path TO 'public'
AS $$
BEGIN
  RAISE EXCEPTION 'Contributions are immutable and cannot be modified';
END;
$$;

DROP TRIGGER IF EXISTS contributions_immutable_update ON public.contributions;
CREATE TRIGGER contributions_immutable_update
  BEFORE UPDATE ON public.contributions
  FOR EACH ROW EXECUTE FUNCTION public.prevent_contribution_mutation();

DROP TRIGGER IF EXISTS contributions_immutable_delete ON public.contributions;
CREATE TRIGGER contributions_immutable_delete
  BEFORE DELETE ON public.contributions
  FOR EACH ROW EXECUTE FUNCTION public.prevent_contribution_mutation();

-- ═══ IMMUTABILITY: Prevent updates/deletes on audit_logs ═══
CREATE OR REPLACE FUNCTION public.prevent_audit_log_mutation()
RETURNS trigger
LANGUAGE plpgsql
SET search_path TO 'public'
AS $$
BEGIN
  RAISE EXCEPTION 'Audit logs are immutable and cannot be modified';
END;
$$;

DROP TRIGGER IF EXISTS audit_logs_immutable_update ON public.audit_logs;
CREATE TRIGGER audit_logs_immutable_update
  BEFORE UPDATE ON public.audit_logs
  FOR EACH ROW EXECUTE FUNCTION public.prevent_audit_log_mutation();

DROP TRIGGER IF EXISTS audit_logs_immutable_delete ON public.audit_logs;
CREATE TRIGGER audit_logs_immutable_delete
  BEFORE DELETE ON public.audit_logs
  FOR EACH ROW EXECUTE FUNCTION public.prevent_audit_log_mutation();
