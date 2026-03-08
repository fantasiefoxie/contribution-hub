
-- ═══ AI Advisory Layer Tables ═══

-- AI suggestions: all AI outputs are advisory and require human confirmation
CREATE TABLE public.ai_suggestions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL,
  suggestion_type TEXT NOT NULL,
  context_entity_type TEXT,
  context_entity_id UUID,
  input_summary TEXT,
  output_content JSONB NOT NULL DEFAULT '{}'::jsonb,
  confidence_score NUMERIC(4,3) CHECK (confidence_score >= 0 AND confidence_score <= 1),
  generated_by TEXT NOT NULL DEFAULT 'ai',
  model_id TEXT,
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'rejected', 'expired')),
  accepted_by UUID,
  accepted_at TIMESTAMPTZ,
  rejected_reason TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at TIMESTAMPTZ DEFAULT (now() + interval '7 days')
);

-- AI usage logs for observability tracking
CREATE TABLE public.ai_usage_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID,
  model_id TEXT NOT NULL,
  operation TEXT NOT NULL,
  input_tokens INTEGER DEFAULT 0,
  output_tokens INTEGER DEFAULT 0,
  estimated_cost_usd NUMERIC(10,6) DEFAULT 0,
  latency_ms INTEGER,
  suggestion_id UUID REFERENCES public.ai_suggestions(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX idx_ai_suggestions_user ON public.ai_suggestions(user_id);
CREATE INDEX idx_ai_suggestions_status ON public.ai_suggestions(status);
CREATE INDEX idx_ai_suggestions_type ON public.ai_suggestions(suggestion_type);
CREATE INDEX idx_ai_suggestions_created ON public.ai_suggestions(created_at DESC);
CREATE INDEX idx_ai_usage_logs_created ON public.ai_usage_logs(created_at DESC);
CREATE INDEX idx_ai_usage_logs_model ON public.ai_usage_logs(model_id);
CREATE INDEX idx_ai_usage_logs_user ON public.ai_usage_logs(user_id);

-- RLS
ALTER TABLE public.ai_suggestions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ai_usage_logs ENABLE ROW LEVEL SECURITY;

-- AI suggestions: users see their own, admins see all
CREATE POLICY "Users can view own suggestions" ON public.ai_suggestions FOR SELECT USING (auth.uid() = user_id OR has_role(auth.uid(), 'admin'));
CREATE POLICY "System can create suggestions" ON public.ai_suggestions FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);
CREATE POLICY "Users can accept/reject own suggestions" ON public.ai_suggestions FOR UPDATE USING (auth.uid() = user_id OR has_role(auth.uid(), 'admin'));

-- AI usage logs: admins only
CREATE POLICY "Admins can view AI usage" ON public.ai_usage_logs FOR SELECT USING (has_role(auth.uid(), 'admin'));
CREATE POLICY "System can log AI usage" ON public.ai_usage_logs FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);
