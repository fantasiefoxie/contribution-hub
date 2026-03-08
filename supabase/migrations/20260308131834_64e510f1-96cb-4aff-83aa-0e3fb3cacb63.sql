
-- Initiative milestones table
CREATE TABLE public.initiative_milestones (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  initiative_id uuid NOT NULL REFERENCES public.initiatives(id) ON DELETE CASCADE,
  percentage integer NOT NULL,
  reached_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(initiative_id, percentage)
);

ALTER TABLE public.initiative_milestones ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Milestones viewable by everyone" ON public.initiative_milestones
  FOR SELECT USING (true);

CREATE POLICY "System can insert milestones" ON public.initiative_milestones
  FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);

-- Notifications table for in-app notifications
CREATE TABLE public.notifications (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL,
  type text NOT NULL,
  title text NOT NULL,
  body text,
  data jsonb DEFAULT '{}'::jsonb,
  read boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own notifications" ON public.notifications
  FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can update own notifications" ON public.notifications
  FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "System can insert notifications" ON public.notifications
  FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);

-- Add target_metrics to initiatives for progress tracking
ALTER TABLE public.initiatives ADD COLUMN IF NOT EXISTS target_metrics jsonb DEFAULT '{}'::jsonb;

-- Enable realtime on contributions and events for live updates
ALTER PUBLICATION supabase_realtime ADD TABLE public.contributions;
ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
