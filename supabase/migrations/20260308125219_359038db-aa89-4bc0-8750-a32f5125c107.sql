
-- ==========================================================
-- ENUMS
-- ==========================================================
CREATE TYPE public.user_status AS ENUM ('anonymous', 'registered', 'trusted');
CREATE TYPE public.org_role AS ENUM ('member', 'validator', 'moderator', 'admin');
CREATE TYPE public.task_status AS ENUM (
  'open', 'reserved', 'claimed', 'in_progress',
  'submitted', 'validation_pending', 'validated', 'rejected', 'expired'
);
CREATE TYPE public.task_visibility AS ENUM ('private', 'organization', 'public');
CREATE TYPE public.validation_decision AS ENUM ('approved', 'rejected', 'clarification_requested');
CREATE TYPE public.initiative_status AS ENUM ('draft', 'active', 'completed', 'cancelled');
CREATE TYPE public.badge_level AS ENUM ('bronze', 'silver', 'gold', 'platinum');
CREATE TYPE public.event_type AS ENUM (
  'TaskCreated', 'TaskReserved', 'TaskClaimed',
  'TaskProgressUpdated', 'TaskSubmitted',
  'ValidationApproved', 'ValidationRejected',
  'ContributionRecorded', 'BadgeAwarded'
);

-- ==========================================================
-- ALL TABLES FIRST
-- ==========================================================

CREATE TABLE public.profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  phone TEXT,
  display_name TEXT,
  avatar_url TEXT,
  status user_status NOT NULL DEFAULT 'registered',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE public.user_roles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  role org_role NOT NULL,
  UNIQUE(user_id, role)
);

CREATE TABLE public.organizations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  slug TEXT NOT NULL UNIQUE,
  description TEXT,
  logo_url TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE public.org_members (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  org_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  role org_role NOT NULL DEFAULT 'member',
  joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(org_id, user_id)
);

CREATE TABLE public.initiatives (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title TEXT NOT NULL,
  description TEXT,
  start_date DATE,
  end_date DATE,
  success_metrics JSONB DEFAULT '{}',
  is_micro BOOLEAN NOT NULL DEFAULT false,
  created_by UUID REFERENCES auth.users(id),
  status initiative_status NOT NULL DEFAULT 'draft',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE public.initiative_orgs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  initiative_id UUID NOT NULL REFERENCES public.initiatives(id) ON DELETE CASCADE,
  org_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
  UNIQUE(initiative_id, org_id)
);

CREATE TABLE public.tasks (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  org_id UUID REFERENCES public.organizations(id) ON DELETE SET NULL,
  initiative_id UUID REFERENCES public.initiatives(id) ON DELETE SET NULL,
  created_by UUID NOT NULL REFERENCES auth.users(id),
  title TEXT NOT NULL,
  description TEXT,
  difficulty INT NOT NULL DEFAULT 1 CHECK (difficulty BETWEEN 1 AND 5),
  status task_status NOT NULL DEFAULT 'open',
  visibility task_visibility NOT NULL DEFAULT 'public',
  tags TEXT[] DEFAULT '{}',
  lock_duration_mins INT NOT NULL DEFAULT 60,
  locked_by UUID REFERENCES auth.users(id),
  locked_until TIMESTAMPTZ,
  reserved_by UUID REFERENCES auth.users(id),
  reserved_until TIMESTAMPTZ,
  expires_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE public.task_claims (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  task_id UUID NOT NULL REFERENCES public.tasks(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  claimed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at TIMESTAMPTZ NOT NULL,
  status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'released', 'expired'))
);

CREATE TABLE public.task_updates (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  task_id UUID NOT NULL REFERENCES public.tasks(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id),
  notes TEXT,
  attachments JSONB DEFAULT '[]',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE public.task_submissions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  task_id UUID NOT NULL REFERENCES public.tasks(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id),
  content TEXT NOT NULL,
  attachments JSONB DEFAULT '[]',
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
  submitted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE public.validations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  submission_id UUID NOT NULL REFERENCES public.task_submissions(id) ON DELETE CASCADE,
  validator_id UUID NOT NULL REFERENCES auth.users(id),
  decision validation_decision NOT NULL,
  feedback TEXT,
  decided_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE public.contributions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id),
  task_id UUID NOT NULL REFERENCES public.tasks(id),
  validation_id UUID REFERENCES public.validations(id),
  score NUMERIC NOT NULL DEFAULT 0,
  impact_metrics JSONB DEFAULT '{}',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE public.events (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  type event_type NOT NULL,
  actor_id UUID REFERENCES auth.users(id),
  entity_type TEXT NOT NULL,
  entity_id UUID NOT NULL,
  payload JSONB DEFAULT '{}',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ==========================================================
-- INDEXES
-- ==========================================================
CREATE INDEX idx_tasks_status ON public.tasks(status);
CREATE INDEX idx_tasks_org ON public.tasks(org_id);
CREATE INDEX idx_tasks_visibility ON public.tasks(visibility);
CREATE INDEX idx_tasks_tags ON public.tasks USING GIN(tags);
CREATE INDEX idx_events_type ON public.events(type);
CREATE INDEX idx_events_entity ON public.events(entity_type, entity_id);
CREATE INDEX idx_events_created ON public.events(created_at);

-- ==========================================================
-- ENABLE RLS ON ALL TABLES
-- ==========================================================
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.organizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.org_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.initiatives ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.initiative_orgs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.task_claims ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.task_updates ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.task_submissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.validations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.contributions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.events ENABLE ROW LEVEL SECURITY;

-- ==========================================================
-- RLS POLICIES
-- ==========================================================

-- Profiles
CREATE POLICY "Profiles viewable by everyone" ON public.profiles FOR SELECT USING (true);
CREATE POLICY "Users update own profile" ON public.profiles FOR UPDATE USING (auth.uid() = id);
CREATE POLICY "Users insert own profile" ON public.profiles FOR INSERT WITH CHECK (auth.uid() = id);

-- User Roles
CREATE POLICY "Users can view own roles" ON public.user_roles FOR SELECT USING (auth.uid() = user_id);

-- Organizations
CREATE POLICY "Orgs viewable by everyone" ON public.organizations FOR SELECT USING (true);
CREATE POLICY "Authenticated users can create orgs" ON public.organizations FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);
CREATE POLICY "Admins can update orgs" ON public.organizations FOR UPDATE USING (
  EXISTS (SELECT 1 FROM public.org_members WHERE org_id = organizations.id AND user_id = auth.uid() AND role = 'admin')
);

-- Org Members
CREATE POLICY "Members viewable by org members" ON public.org_members FOR SELECT USING (
  EXISTS (SELECT 1 FROM public.org_members om WHERE om.org_id = org_members.org_id AND om.user_id = auth.uid())
);
CREATE POLICY "Admins can manage members" ON public.org_members FOR ALL USING (
  EXISTS (SELECT 1 FROM public.org_members om WHERE om.org_id = org_members.org_id AND om.user_id = auth.uid() AND om.role = 'admin')
);

-- Initiatives
CREATE POLICY "Initiatives viewable by everyone" ON public.initiatives FOR SELECT USING (true);
CREATE POLICY "Authenticated users can create initiatives" ON public.initiatives FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);
CREATE POLICY "Creators can update initiatives" ON public.initiatives FOR UPDATE USING (auth.uid() = created_by);

-- Initiative Orgs
CREATE POLICY "Initiative orgs viewable" ON public.initiative_orgs FOR SELECT USING (true);

-- Tasks
CREATE POLICY "Public tasks viewable" ON public.tasks FOR SELECT USING (
  visibility = 'public' OR auth.uid() = created_by OR
  EXISTS (SELECT 1 FROM public.org_members WHERE org_id = tasks.org_id AND user_id = auth.uid())
);
CREATE POLICY "Authenticated users can create tasks" ON public.tasks FOR INSERT WITH CHECK (auth.uid() IS NOT NULL AND auth.uid() = created_by);
CREATE POLICY "Creators and admins can update tasks" ON public.tasks FOR UPDATE USING (
  auth.uid() = created_by OR
  EXISTS (SELECT 1 FROM public.org_members WHERE org_id = tasks.org_id AND user_id = auth.uid() AND role IN ('admin', 'moderator'))
);

-- Task Claims
CREATE POLICY "Users can view own claims" ON public.task_claims FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can create claims" ON public.task_claims FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Task Updates
CREATE POLICY "Task updates viewable" ON public.task_updates FOR SELECT USING (
  auth.uid() = user_id OR
  EXISTS (SELECT 1 FROM public.tasks t JOIN public.org_members om ON om.org_id = t.org_id WHERE t.id = task_updates.task_id AND om.user_id = auth.uid())
);
CREATE POLICY "Users can create own updates" ON public.task_updates FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Task Submissions
CREATE POLICY "Submissions viewable" ON public.task_submissions FOR SELECT USING (
  auth.uid() = user_id OR
  EXISTS (SELECT 1 FROM public.tasks t JOIN public.org_members om ON om.org_id = t.org_id WHERE t.id = task_submissions.task_id AND om.user_id = auth.uid())
);
CREATE POLICY "Users can create submissions" ON public.task_submissions FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Validations
CREATE POLICY "Validations viewable" ON public.validations FOR SELECT USING (
  auth.uid() = validator_id OR
  EXISTS (SELECT 1 FROM public.task_submissions ts WHERE ts.id = validations.submission_id AND ts.user_id = auth.uid())
);
CREATE POLICY "Validators can create validations" ON public.validations FOR INSERT WITH CHECK (
  auth.uid() = validator_id AND
  NOT EXISTS (
    SELECT 1 FROM public.task_submissions ts JOIN public.tasks t ON t.id = ts.task_id
    WHERE ts.id = validations.submission_id AND (ts.user_id = auth.uid() OR t.created_by = auth.uid())
  )
);

-- Contributions (immutable)
CREATE POLICY "Contributions viewable by owner" ON public.contributions FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "System can insert contributions" ON public.contributions FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);

-- Events (append-only)
CREATE POLICY "Events viewable by actor" ON public.events FOR SELECT USING (auth.uid() = actor_id);
CREATE POLICY "System can insert events" ON public.events FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);

-- ==========================================================
-- FUNCTIONS
-- ==========================================================

CREATE OR REPLACE FUNCTION public.has_role(_user_id UUID, _role org_role)
RETURNS BOOLEAN LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public
AS $$ SELECT EXISTS (SELECT 1 FROM public.user_roles WHERE user_id = _user_id AND role = _role) $$;

CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER AS $$ BEGIN NEW.updated_at = now(); RETURN NEW; END; $$ LANGUAGE plpgsql SET search_path = public;

CREATE TRIGGER update_profiles_updated_at BEFORE UPDATE ON public.profiles FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_organizations_updated_at BEFORE UPDATE ON public.organizations FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_initiatives_updated_at BEFORE UPDATE ON public.initiatives FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_tasks_updated_at BEFORE UPDATE ON public.tasks FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();

CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.profiles (id, phone, display_name)
  VALUES (NEW.id, NEW.phone, COALESCE(NEW.raw_user_meta_data->>'display_name', 'User'));
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

CREATE TRIGGER on_auth_user_created AFTER INSERT ON auth.users FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

CREATE OR REPLACE FUNCTION public.expire_reservations()
RETURNS void AS $$
BEGIN
  UPDATE public.tasks SET status = 'open', reserved_by = NULL, reserved_until = NULL WHERE status = 'reserved' AND reserved_until < now();
  UPDATE public.tasks SET status = 'open', locked_by = NULL, locked_until = NULL WHERE status = 'claimed' AND locked_until < now();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;
