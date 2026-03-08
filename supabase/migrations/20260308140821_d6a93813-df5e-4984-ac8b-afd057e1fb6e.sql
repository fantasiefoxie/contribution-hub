
-- API keys table for external app authentication
CREATE TABLE public.api_keys (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  org_id uuid REFERENCES public.organizations(id) ON DELETE CASCADE,
  user_id uuid NOT NULL,
  name text NOT NULL,
  key_hash text NOT NULL,
  key_prefix text NOT NULL,
  scopes text[] NOT NULL DEFAULT '{}',
  rate_limit_per_min integer NOT NULL DEFAULT 60,
  is_active boolean NOT NULL DEFAULT true,
  last_used_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  expires_at timestamptz
);

ALTER TABLE public.api_keys ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own API keys" ON public.api_keys
  FOR SELECT TO authenticated
  USING (auth.uid() = user_id);

CREATE POLICY "Users can create API keys" ON public.api_keys
  FOR INSERT TO authenticated
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own API keys" ON public.api_keys
  FOR UPDATE TO authenticated
  USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own API keys" ON public.api_keys
  FOR DELETE TO authenticated
  USING (auth.uid() = user_id);

-- Webhook subscriptions for event streaming
CREATE TABLE public.webhook_subscriptions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  api_key_id uuid REFERENCES public.api_keys(id) ON DELETE CASCADE NOT NULL,
  url text NOT NULL,
  events text[] NOT NULL DEFAULT '{}',
  secret text NOT NULL,
  is_active boolean NOT NULL DEFAULT true,
  failure_count integer NOT NULL DEFAULT 0,
  last_triggered_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE public.webhook_subscriptions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "API key owners can view subscriptions" ON public.webhook_subscriptions
  FOR SELECT TO authenticated
  USING (EXISTS (SELECT 1 FROM public.api_keys ak WHERE ak.id = webhook_subscriptions.api_key_id AND ak.user_id = auth.uid()));

CREATE POLICY "API key owners can create subscriptions" ON public.webhook_subscriptions
  FOR INSERT TO authenticated
  WITH CHECK (EXISTS (SELECT 1 FROM public.api_keys ak WHERE ak.id = webhook_subscriptions.api_key_id AND ak.user_id = auth.uid()));

CREATE POLICY "API key owners can update subscriptions" ON public.webhook_subscriptions
  FOR UPDATE TO authenticated
  USING (EXISTS (SELECT 1 FROM public.api_keys ak WHERE ak.id = webhook_subscriptions.api_key_id AND ak.user_id = auth.uid()));

CREATE POLICY "API key owners can delete subscriptions" ON public.webhook_subscriptions
  FOR DELETE TO authenticated
  USING (EXISTS (SELECT 1 FROM public.api_keys ak WHERE ak.id = webhook_subscriptions.api_key_id AND ak.user_id = auth.uid()));
