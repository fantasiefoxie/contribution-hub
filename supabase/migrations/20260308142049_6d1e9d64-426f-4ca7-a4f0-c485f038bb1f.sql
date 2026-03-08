
-- Add discovery columns to initiatives
ALTER TABLE public.initiatives ADD COLUMN IF NOT EXISTS featured_at timestamptz;
ALTER TABLE public.initiatives ADD COLUMN IF NOT EXISTS priority_score numeric NOT NULL DEFAULT 0;
ALTER TABLE public.initiatives ADD COLUMN IF NOT EXISTS volunteer_target integer;
ALTER TABLE public.initiatives ADD COLUMN IF NOT EXISTS category text;
ALTER TABLE public.initiatives ADD COLUMN IF NOT EXISTS is_online boolean NOT NULL DEFAULT false;
ALTER TABLE public.initiatives ADD COLUMN IF NOT EXISTS latitude double precision;
ALTER TABLE public.initiatives ADD COLUMN IF NOT EXISTS longitude double precision;
ALTER TABLE public.initiatives ADD COLUMN IF NOT EXISTS region_name text;

-- Index for discovery queries
CREATE INDEX IF NOT EXISTS idx_initiatives_priority ON public.initiatives (priority_score DESC) WHERE status = 'active';
CREATE INDEX IF NOT EXISTS idx_initiatives_featured ON public.initiatives (featured_at DESC NULLS LAST) WHERE featured_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_initiatives_category ON public.initiatives (category) WHERE category IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_initiatives_location ON public.initiatives (latitude, longitude) WHERE latitude IS NOT NULL;
