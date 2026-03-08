
-- Add interests and location columns to profiles
ALTER TABLE public.profiles 
  ADD COLUMN IF NOT EXISTS interests text[] DEFAULT '{}',
  ADD COLUMN IF NOT EXISTS latitude double precision,
  ADD COLUMN IF NOT EXISTS longitude double precision;

-- Add location and priority columns to tasks
ALTER TABLE public.tasks
  ADD COLUMN IF NOT EXISTS latitude double precision,
  ADD COLUMN IF NOT EXISTS longitude double precision,
  ADD COLUMN IF NOT EXISTS is_priority boolean NOT NULL DEFAULT false;
