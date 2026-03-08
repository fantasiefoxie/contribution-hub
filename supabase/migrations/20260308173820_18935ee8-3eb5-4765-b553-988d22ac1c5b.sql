
-- ============================================================
-- 1. UUIDv7 generator function (time-sortable UUIDs)
--    Uses current timestamp in the high bits for ordering,
--    random bytes for uniqueness. Compatible with UUID column type.
-- ============================================================
CREATE OR REPLACE FUNCTION public.uuid_generate_v7()
RETURNS UUID
LANGUAGE plpgsql
VOLATILE
SET search_path TO 'public'
AS $$
DECLARE
  _ts BIGINT;
  _bytes BYTEA;
  _hex TEXT;
BEGIN
  -- milliseconds since epoch
  _ts := (extract(epoch from clock_timestamp()) * 1000)::BIGINT;

  -- 6 bytes timestamp + 10 bytes random
  _bytes := decode(lpad(to_hex(_ts), 12, '0'), 'hex') || gen_random_bytes(10);

  -- Set version 7 (bits 48-51 = 0111) and variant (bits 64-65 = 10)
  _bytes := set_byte(_bytes, 6, (get_byte(_bytes, 6) & x'0F'::INT) | x'70'::INT);
  _bytes := set_byte(_bytes, 8, (get_byte(_bytes, 8) & x'3F'::INT) | x'80'::INT);

  _hex := encode(_bytes, 'hex');
  RETURN (
    substr(_hex, 1, 8) || '-' ||
    substr(_hex, 9, 4) || '-' ||
    substr(_hex, 13, 4) || '-' ||
    substr(_hex, 17, 4) || '-' ||
    substr(_hex, 21, 12)
  )::UUID;
END;
$$;

-- ============================================================
-- 2. Slug generation helper
-- ============================================================
CREATE OR REPLACE FUNCTION public.generate_slug(_title TEXT)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
SET search_path TO 'public'
AS $$
DECLARE
  _slug TEXT;
BEGIN
  _slug := lower(trim(_title));
  _slug := regexp_replace(_slug, '[^a-z0-9\s-]', '', 'g');
  _slug := regexp_replace(_slug, '[\s]+', '-', 'g');
  _slug := regexp_replace(_slug, '-+', '-', 'g');
  _slug := trim(BOTH '-' FROM _slug);
  -- Truncate to 80 chars
  _slug := left(_slug, 80);
  RETURN _slug;
END;
$$;

-- ============================================================
-- 3. Add slug columns to core tables
-- ============================================================
ALTER TABLE public.initiatives ADD COLUMN IF NOT EXISTS slug TEXT;
ALTER TABLE public.organizations ADD COLUMN IF NOT EXISTS slug_id TEXT;
ALTER TABLE public.tasks ADD COLUMN IF NOT EXISTS slug TEXT;

-- Create unique indexes for slug lookups
CREATE UNIQUE INDEX IF NOT EXISTS idx_initiatives_slug ON public.initiatives (slug) WHERE slug IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_tasks_slug ON public.tasks (slug) WHERE slug IS NOT NULL;

-- organizations already has a slug column, add unique index if missing
CREATE UNIQUE INDEX IF NOT EXISTS idx_organizations_slug ON public.organizations (slug) WHERE slug IS NOT NULL;

-- ============================================================
-- 4. Update default PK generation to UUIDv7 for core tables
-- ============================================================
ALTER TABLE public.initiatives ALTER COLUMN id SET DEFAULT public.uuid_generate_v7();
ALTER TABLE public.organizations ALTER COLUMN id SET DEFAULT public.uuid_generate_v7();
ALTER TABLE public.tasks ALTER COLUMN id SET DEFAULT public.uuid_generate_v7();
ALTER TABLE public.contributions ALTER COLUMN id SET DEFAULT public.uuid_generate_v7();
ALTER TABLE public.events ALTER COLUMN id SET DEFAULT public.uuid_generate_v7();
ALTER TABLE public.task_submissions ALTER COLUMN id SET DEFAULT public.uuid_generate_v7();
ALTER TABLE public.validations ALTER COLUMN id SET DEFAULT public.uuid_generate_v7();
ALTER TABLE public.profiles ALTER COLUMN id SET DEFAULT public.uuid_generate_v7();

-- Also update supporting tables
ALTER TABLE public.projects ALTER COLUMN id SET DEFAULT public.uuid_generate_v7();
ALTER TABLE public.notifications ALTER COLUMN id SET DEFAULT public.uuid_generate_v7();
ALTER TABLE public.audit_logs ALTER COLUMN id SET DEFAULT public.uuid_generate_v7();
ALTER TABLE public.credentials ALTER COLUMN id SET DEFAULT public.uuid_generate_v7();
ALTER TABLE public.task_claims ALTER COLUMN id SET DEFAULT public.uuid_generate_v7();
ALTER TABLE public.task_updates ALTER COLUMN id SET DEFAULT public.uuid_generate_v7();
ALTER TABLE public.team_members ALTER COLUMN id SET DEFAULT public.uuid_generate_v7();
ALTER TABLE public.resource_aliases ALTER COLUMN id SET DEFAULT public.uuid_generate_v7();

-- ============================================================
-- 5. Auto-generate slugs on INSERT for initiatives and tasks
-- ============================================================
CREATE OR REPLACE FUNCTION public.auto_generate_initiative_slug()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path TO 'public'
AS $$
DECLARE
  _base_slug TEXT;
  _slug TEXT;
  _counter INT := 0;
BEGIN
  IF NEW.slug IS NULL OR NEW.slug = '' THEN
    _base_slug := public.generate_slug(NEW.title);
    _slug := _base_slug;
    -- Handle collisions by appending counter
    LOOP
      EXIT WHEN NOT EXISTS (
        SELECT 1 FROM public.initiatives WHERE slug = _slug AND id != NEW.id
      );
      _counter := _counter + 1;
      _slug := _base_slug || '-' || _counter;
    END LOOP;
    NEW.slug := _slug;
  END IF;

  -- Also register in alias table
  INSERT INTO public.resource_aliases (resource_type, alias, canonical_id)
  VALUES ('initiative', NEW.slug, NEW.id)
  ON CONFLICT (resource_type, alias) DO NOTHING;

  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_initiative_slug
  BEFORE INSERT ON public.initiatives
  FOR EACH ROW
  EXECUTE FUNCTION public.auto_generate_initiative_slug();

CREATE OR REPLACE FUNCTION public.auto_generate_task_slug()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path TO 'public'
AS $$
DECLARE
  _base_slug TEXT;
  _slug TEXT;
  _counter INT := 0;
BEGIN
  IF NEW.slug IS NULL OR NEW.slug = '' THEN
    _base_slug := public.generate_slug(NEW.title);
    _slug := _base_slug;
    LOOP
      EXIT WHEN NOT EXISTS (
        SELECT 1 FROM public.tasks WHERE slug = _slug AND id != NEW.id
      );
      _counter := _counter + 1;
      _slug := _base_slug || '-' || _counter;
    END LOOP;
    NEW.slug := _slug;
  END IF;

  INSERT INTO public.resource_aliases (resource_type, alias, canonical_id)
  VALUES ('task', NEW.slug, NEW.id)
  ON CONFLICT (resource_type, alias) DO NOTHING;

  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_task_slug
  BEFORE INSERT ON public.tasks
  FOR EACH ROW
  EXECUTE FUNCTION public.auto_generate_task_slug();

-- ============================================================
-- 6. Update resolve_resource to also check slug columns directly
-- ============================================================
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
  -- 1. Try direct UUID cast
  BEGIN
    _uuid := _identifier::UUID;
    RETURN _uuid;
  EXCEPTION WHEN invalid_text_representation THEN
    NULL; -- Not a UUID, continue
  END;

  -- 2. Try slug column on the source table
  IF _resource_type = 'initiative' THEN
    SELECT id INTO _uuid FROM public.initiatives WHERE slug = _identifier LIMIT 1;
    IF _uuid IS NOT NULL THEN RETURN _uuid; END IF;
  ELSIF _resource_type = 'task' THEN
    SELECT id INTO _uuid FROM public.tasks WHERE slug = _identifier LIMIT 1;
    IF _uuid IS NOT NULL THEN RETURN _uuid; END IF;
  ELSIF _resource_type = 'organization' THEN
    SELECT id INTO _uuid FROM public.organizations WHERE slug = _identifier LIMIT 1;
    IF _uuid IS NOT NULL THEN RETURN _uuid; END IF;
  END IF;

  -- 3. Fallback to alias table
  SELECT canonical_id INTO _uuid
  FROM public.resource_aliases
  WHERE resource_type = _resource_type AND alias = _identifier;

  RETURN _uuid;
END;
$$;

-- ============================================================
-- 7. Backfill slugs for any existing initiatives/tasks
-- ============================================================
UPDATE public.initiatives
SET slug = public.generate_slug(title) || '-' || left(id::TEXT, 8)
WHERE slug IS NULL;

UPDATE public.tasks
SET slug = public.generate_slug(title) || '-' || left(id::TEXT, 8)
WHERE slug IS NULL;
