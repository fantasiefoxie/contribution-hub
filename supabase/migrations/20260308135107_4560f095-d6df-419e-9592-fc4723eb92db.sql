
-- Regions table for initiative task pools
CREATE TABLE public.regions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  initiative_id uuid NOT NULL REFERENCES public.initiatives(id) ON DELETE CASCADE,
  name text NOT NULL,
  description text,
  sort_order integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE public.regions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Regions viewable by everyone" ON public.regions FOR SELECT USING (true);
CREATE POLICY "Initiative creators can manage regions" ON public.regions FOR INSERT
  WITH CHECK (EXISTS (SELECT 1 FROM public.initiatives i WHERE i.id = regions.initiative_id AND i.created_by = auth.uid()));
CREATE POLICY "Initiative creators can update regions" ON public.regions FOR UPDATE
  USING (EXISTS (SELECT 1 FROM public.initiatives i WHERE i.id = regions.initiative_id AND i.created_by = auth.uid()));
CREATE POLICY "Initiative creators can delete regions" ON public.regions FOR DELETE
  USING (EXISTS (SELECT 1 FROM public.initiatives i WHERE i.id = regions.initiative_id AND i.created_by = auth.uid()));

-- Add region_id to tasks
ALTER TABLE public.tasks ADD COLUMN IF NOT EXISTS region_id uuid REFERENCES public.regions(id);

-- Add task_pool enum-like column
ALTER TABLE public.tasks ADD COLUMN IF NOT EXISTS task_pool text NOT NULL DEFAULT 'public';

-- Volunteer capacity: max active tasks
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS max_active_tasks integer NOT NULL DEFAULT 1;

-- Add region_id to team_members for regional team pods
ALTER TABLE public.team_members ADD COLUMN IF NOT EXISTS region_id uuid REFERENCES public.regions(id);

-- Rate limiting table
CREATE TABLE public.claim_attempts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL,
  attempted_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE public.claim_attempts ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users see own claims" ON public.claim_attempts FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert claims" ON public.claim_attempts FOR INSERT WITH CHECK (auth.uid() = user_id);
