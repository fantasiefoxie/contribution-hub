
-- Add team mission fields to tasks
ALTER TABLE public.tasks
  ADD COLUMN IF NOT EXISTS is_team boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS team_target integer DEFAULT NULL;

-- Create team_members table
CREATE TABLE public.team_members (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  task_id uuid NOT NULL REFERENCES public.tasks(id) ON DELETE CASCADE,
  user_id uuid NOT NULL,
  joined_at timestamptz NOT NULL DEFAULT now(),
  invite_code text,
  UNIQUE(task_id, user_id)
);

ALTER TABLE public.team_members ENABLE ROW LEVEL SECURITY;

-- Everyone can see team members for public tasks
CREATE POLICY "Team members viewable for public tasks" ON public.team_members
  FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.tasks t WHERE t.id = team_members.task_id AND t.visibility = 'public')
    OR auth.uid() = user_id
  );

-- Authenticated users can join teams
CREATE POLICY "Users can join teams" ON public.team_members
  FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Users can leave teams
CREATE POLICY "Users can leave teams" ON public.team_members
  FOR DELETE USING (auth.uid() = user_id);
