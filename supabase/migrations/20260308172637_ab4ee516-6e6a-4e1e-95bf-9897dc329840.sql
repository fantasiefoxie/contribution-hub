
-- Resource aliases table for canonical ID resolution
CREATE TABLE public.resource_aliases (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  resource_type TEXT NOT NULL,
  alias TEXT NOT NULL,
  canonical_id UUID NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Unique constraint: one alias per resource type
CREATE UNIQUE INDEX idx_resource_aliases_type_alias ON public.resource_aliases (resource_type, alias);

-- Index for fast lookups
CREATE INDEX idx_resource_aliases_canonical ON public.resource_aliases (resource_type, canonical_id);

-- RLS: publicly readable, only system can insert
ALTER TABLE public.resource_aliases ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Resource aliases viewable by everyone"
  ON public.resource_aliases FOR SELECT
  USING (true);

CREATE POLICY "System can insert resource aliases"
  ON public.resource_aliases FOR INSERT
  WITH CHECK (auth.uid() IS NOT NULL);

-- DB function to resolve an identifier to a canonical UUID
-- Accepts: UUID directly, or alias string
CREATE OR REPLACE FUNCTION public.resolve_resource(
  _resource_type TEXT,
  _identifier TEXT
)
RETURNS UUID
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
  _uuid UUID;
BEGIN
  -- Try direct UUID cast first
  BEGIN
    _uuid := _identifier::UUID;
    RETURN _uuid;
  EXCEPTION WHEN invalid_text_representation THEN
    -- Not a UUID, look up alias
    SELECT canonical_id INTO _uuid
    FROM public.resource_aliases
    WHERE resource_type = _resource_type
      AND alias = _identifier;
    RETURN _uuid;  -- NULL if not found
  END;
END;
$$;
