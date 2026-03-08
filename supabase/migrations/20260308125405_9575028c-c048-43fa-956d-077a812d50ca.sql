
-- Fix org_members infinite recursion by using a security definer function
CREATE OR REPLACE FUNCTION public.is_org_member(_user_id UUID, _org_id UUID)
RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (SELECT 1 FROM public.org_members WHERE user_id = _user_id AND org_id = _org_id)
$$;

CREATE OR REPLACE FUNCTION public.is_org_admin(_user_id UUID, _org_id UUID)
RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (SELECT 1 FROM public.org_members WHERE user_id = _user_id AND org_id = _org_id AND role = 'admin')
$$;

-- Drop broken policies
DROP POLICY IF EXISTS "Members viewable by org members" ON public.org_members;
DROP POLICY IF EXISTS "Admins can manage members" ON public.org_members;

-- Recreate without recursion
CREATE POLICY "Members viewable by org members" ON public.org_members FOR SELECT USING (
  public.is_org_member(auth.uid(), org_id)
);
CREATE POLICY "Admins can insert members" ON public.org_members FOR INSERT WITH CHECK (
  public.is_org_admin(auth.uid(), org_id)
);
CREATE POLICY "Admins can update members" ON public.org_members FOR UPDATE USING (
  public.is_org_admin(auth.uid(), org_id)
);
CREATE POLICY "Admins can delete members" ON public.org_members FOR DELETE USING (
  public.is_org_admin(auth.uid(), org_id)
);

-- Also fix organizations update policy that references org_members
DROP POLICY IF EXISTS "Admins can update orgs" ON public.organizations;
CREATE POLICY "Admins can update orgs" ON public.organizations FOR UPDATE USING (
  public.is_org_admin(auth.uid(), id)
);
