
-- ═══ Credentials System ═══
CREATE TABLE public.credentials (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL,
  initiative_id UUID REFERENCES public.initiatives(id) ON DELETE SET NULL,
  org_id UUID REFERENCES public.organizations(id) ON DELETE SET NULL,
  type TEXT NOT NULL DEFAULT 'completion',
  title TEXT NOT NULL,
  description TEXT,
  metadata JSONB DEFAULT '{}'::jsonb,
  issued_by UUID,
  issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  revoked_at TIMESTAMPTZ,
  revoked_by UUID,
  is_active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE public.credential_metadata (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  credential_id UUID REFERENCES public.credentials(id) ON DELETE CASCADE NOT NULL,
  key TEXT NOT NULL,
  value TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX idx_credentials_user ON public.credentials(user_id);
CREATE INDEX idx_credentials_initiative ON public.credentials(initiative_id);
CREATE INDEX idx_credentials_type ON public.credentials(type);
CREATE INDEX idx_credentials_active ON public.credentials(is_active) WHERE is_active = true;
CREATE INDEX idx_credential_metadata_credential ON public.credential_metadata(credential_id);

-- RLS
ALTER TABLE public.credentials ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.credential_metadata ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Credentials viewable by everyone" ON public.credentials FOR SELECT USING (true);
CREATE POLICY "System can issue credentials" ON public.credentials FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);
CREATE POLICY "Issuers can revoke credentials" ON public.credentials FOR UPDATE USING (auth.uid() = issued_by OR has_role(auth.uid(), 'admin'));

CREATE POLICY "Credential metadata viewable" ON public.credential_metadata FOR SELECT USING (true);
CREATE POLICY "System can insert credential metadata" ON public.credential_metadata FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);

-- ═══ New Event Types ═══
ALTER TYPE public.event_type ADD VALUE IF NOT EXISTS 'CredentialIssued';
ALTER TYPE public.event_type ADD VALUE IF NOT EXISTS 'CredentialRevoked';
ALTER TYPE public.event_type ADD VALUE IF NOT EXISTS 'RoleGranted';
ALTER TYPE public.event_type ADD VALUE IF NOT EXISTS 'RoleRevoked';
ALTER TYPE public.event_type ADD VALUE IF NOT EXISTS 'InitiativeJoined';
ALTER TYPE public.event_type ADD VALUE IF NOT EXISTS 'InitiativePriorityUpdated';
ALTER TYPE public.event_type ADD VALUE IF NOT EXISTS 'InitiativeCreated';

-- ═══ New Permission Scopes ═══
INSERT INTO public.role_permissions (role, scope) VALUES
  ('admin', 'credentials:issue'),
  ('admin', 'credentials:revoke'),
  ('admin', 'roles:grant'),
  ('admin', 'roles:revoke'),
  ('admin', 'initiatives:join'),
  ('admin', 'initiatives:prioritize'),
  ('moderator', 'credentials:issue'),
  ('moderator', 'roles:grant'),
  ('moderator', 'initiatives:join'),
  ('moderator', 'initiatives:prioritize'),
  ('validator', 'credentials:issue'),
  ('validator', 'initiatives:join'),
  ('member', 'initiatives:join')
ON CONFLICT DO NOTHING;
