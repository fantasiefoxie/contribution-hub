
-- Initiative participants (volunteers who join an initiative)
CREATE TABLE public.initiative_participants (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  initiative_id uuid NOT NULL REFERENCES public.initiatives(id) ON DELETE CASCADE,
  user_id uuid NOT NULL,
  joined_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(initiative_id, user_id)
);

ALTER TABLE public.initiative_participants ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Participants viewable by everyone" ON public.initiative_participants
  FOR SELECT USING (true);

CREATE POLICY "Authenticated users can join" ON public.initiative_participants
  FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can leave" ON public.initiative_participants
  FOR DELETE USING (auth.uid() = user_id);

-- Projects table for grouping tasks within an initiative
CREATE TABLE public.projects (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  initiative_id uuid NOT NULL REFERENCES public.initiatives(id) ON DELETE CASCADE,
  title text NOT NULL,
  description text,
  sort_order integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE public.projects ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Projects viewable by everyone" ON public.projects
  FOR SELECT USING (true);

CREATE POLICY "Initiative creators can manage projects" ON public.projects
  FOR INSERT WITH CHECK (
    EXISTS (SELECT 1 FROM initiatives i WHERE i.id = projects.initiative_id AND i.created_by = auth.uid())
  );

CREATE POLICY "Initiative creators can update projects" ON public.projects
  FOR UPDATE USING (
    EXISTS (SELECT 1 FROM initiatives i WHERE i.id = projects.initiative_id AND i.created_by = auth.uid())
  );

-- Add project_id to tasks
ALTER TABLE public.tasks ADD COLUMN IF NOT EXISTS project_id uuid REFERENCES public.projects(id);

-- Add micro-initiative fields to initiatives
ALTER TABLE public.initiatives ADD COLUMN IF NOT EXISTS max_tasks integer;
ALTER TABLE public.initiatives ADD COLUMN IF NOT EXISTS max_volunteers integer;
ALTER TABLE public.initiatives ADD COLUMN IF NOT EXISTS promoted_by uuid;
ALTER TABLE public.initiatives ADD COLUMN IF NOT EXISTS promoted_at timestamptz;
