
-- Appeals table for the validation appeals workflow
CREATE TABLE public.appeals (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  submission_id uuid NOT NULL REFERENCES public.task_submissions(id),
  validation_id uuid NOT NULL REFERENCES public.validations(id),
  appellant_id uuid NOT NULL,
  reason text NOT NULL,
  status text NOT NULL DEFAULT 'pending',
  resolved_by uuid,
  resolution text,
  resolved_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE public.appeals ENABLE ROW LEVEL SECURITY;

-- Appellant can view their own appeals
CREATE POLICY "Users can view own appeals" ON public.appeals
  FOR SELECT USING (auth.uid() = appellant_id);

-- Org admins/moderators can view appeals for their org's tasks
CREATE POLICY "Org moderators can view appeals" ON public.appeals
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM task_submissions ts
      JOIN tasks t ON t.id = ts.task_id
      JOIN org_members om ON om.org_id = t.org_id
      WHERE ts.id = appeals.submission_id
        AND om.user_id = auth.uid()
        AND om.role IN ('admin', 'moderator')
    )
  );

-- Authenticated users can create appeals on their own submissions
CREATE POLICY "Users can create own appeals" ON public.appeals
  FOR INSERT WITH CHECK (auth.uid() = appellant_id);

-- Audit log table for immutable governance actions
CREATE TABLE public.audit_logs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  action text NOT NULL,
  actor_id uuid NOT NULL,
  entity_type text NOT NULL,
  entity_id uuid NOT NULL,
  details jsonb DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

-- Admins can view audit logs
CREATE POLICY "Admins can view audit logs" ON public.audit_logs
  FOR SELECT USING (public.has_role(auth.uid(), 'admin'));

-- System can insert audit logs (any authenticated user via edge function)
CREATE POLICY "System can insert audit logs" ON public.audit_logs
  FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);
