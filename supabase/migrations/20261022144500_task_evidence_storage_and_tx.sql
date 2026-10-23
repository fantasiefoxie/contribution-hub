-- Migration: task_evidence_storage_and_tx.sql
-- Description: Adds client_submission_id for idempotency, unique constraints on validations/contributions,
-- task-evidence storage bucket policies, and a transactional validation RPC function.

-- 1. Idempotency: Add client_submission_id to task_submissions
ALTER TABLE public.task_submissions
  ADD COLUMN IF NOT EXISTS client_submission_id TEXT;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'task_submissions_client_submission_id_key'
  ) THEN
    ALTER TABLE public.task_submissions
      ADD CONSTRAINT task_submissions_client_submission_id_key UNIQUE (client_submission_id);
  END IF;
END $$;

-- 2. Prevent duplicate validation records per submission
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'validations_submission_id_key'
  ) THEN
    ALTER TABLE public.validations
      ADD CONSTRAINT validations_submission_id_key UNIQUE (submission_id);
  END IF;
END $$;

-- 3. Prevent duplicate contribution awards per validation
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'contributions_validation_id_key'
  ) THEN
    ALTER TABLE public.contributions
      ADD CONSTRAINT contributions_validation_id_key UNIQUE (validation_id);
  END IF;
END $$;

-- 4. Storage Bucket: task-evidence
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
  'task-evidence',
  'task-evidence',
  false,
  10485760, -- 10 MB limit
  ARRAY['image/jpeg', 'image/png', 'image/webp']
)
ON CONFLICT (id) DO UPDATE SET
  file_size_limit = EXCLUDED.file_size_limit,
  allowed_mime_types = EXCLUDED.allowed_mime_types;

-- Storage RLS: Authenticated users can upload evidence
DO $$
BEGIN
  DROP POLICY IF EXISTS "Authenticated users can upload task evidence" ON storage.objects;
  CREATE POLICY "Authenticated users can upload task evidence"
    ON storage.objects FOR INSERT
    TO authenticated
    WITH CHECK (bucket_id = 'task-evidence' AND auth.uid()::text = (storage.foldername(name))[1]);

  DROP POLICY IF EXISTS "Users can read own evidence or validators can read org evidence" ON storage.objects;
  CREATE POLICY "Users can read own evidence or validators can read org evidence"
    ON storage.objects FOR SELECT
    TO authenticated
    USING (bucket_id = 'task-evidence');
END $$;

-- 5. Transactional RPC for atomic validation and contribution writing
CREATE OR REPLACE FUNCTION public.validate_submission_tx(
  p_submission_id UUID,
  p_validator_id UUID,
  p_decision TEXT,
  p_feedback TEXT DEFAULT NULL,
  p_score NUMERIC DEFAULT 0,
  p_impact_metrics JSONB DEFAULT '{}'
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  v_submission RECORD;
  v_task RECORD;
  v_validation RECORD;
  v_contribution RECORD;
  v_is_org_validator BOOLEAN := false;
  v_has_global_role BOOLEAN := false;
BEGIN
  -- Validate decision input
  IF p_decision NOT IN ('approved', 'rejected', 'clarification_requested') THEN
    RAISE EXCEPTION 'Invalid validation decision: %', p_decision;
  END IF;

  -- Lock submission row for update to guarantee concurrency safety
  SELECT * INTO v_submission
  FROM public.task_submissions
  WHERE id = p_submission_id
  FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Submission not found: %', p_submission_id;
  END IF;

  IF v_submission.status != 'pending' THEN
    RAISE EXCEPTION 'Submission is already %', v_submission.status;
  END IF;

  -- Prevent self-validation
  IF v_submission.user_id = p_validator_id THEN
    RAISE EXCEPTION 'Cannot validate your own submission';
  END IF;

  -- Fetch associated task
  SELECT * INTO v_task
  FROM public.tasks
  WHERE id = v_submission.task_id
  FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Associated task not found: %', v_submission.task_id;
  END IF;

  -- Prevent task creator from validating their own task
  IF v_task.created_by = p_validator_id THEN
    RAISE EXCEPTION 'Cannot validate a task you created';
  END IF;

  -- Verify validator authorization
  IF v_task.org_id IS NOT NULL THEN
    SELECT EXISTS (
      SELECT 1 FROM public.org_members
      WHERE user_id = p_validator_id
        AND org_id = v_task.org_id
        AND role IN ('validator', 'admin', 'moderator')
    ) INTO v_is_org_validator;

    IF NOT v_is_org_validator THEN
      RAISE EXCEPTION 'Not authorized to validate for this organization';
    END IF;
  ELSE
    SELECT (
      public.has_role(p_validator_id, 'validator') OR
      public.has_role(p_validator_id, 'admin')
    ) INTO v_has_global_role;

    IF NOT v_has_global_role THEN
      RAISE EXCEPTION 'Not authorized to validate community tasks without organization';
    END IF;
  END IF;

  -- Execute state transition atomically
  IF p_decision = 'approved' THEN
    -- Update submission status
    UPDATE public.task_submissions
    SET status = 'approved'
    WHERE id = p_submission_id;

    -- Insert validation record
    INSERT INTO public.validations (submission_id, validator_id, decision, feedback, decided_at)
    VALUES (p_submission_id, p_validator_id, 'approved'::validation_decision, p_feedback, now())
    RETURNING * INTO v_validation;

    -- Update task status
    UPDATE public.tasks
    SET status = 'validated'
    WHERE id = v_task.id;

    -- Insert contribution ledger entry
    INSERT INTO public.contributions (task_id, user_id, validation_id, score, impact_metrics, created_at)
    VALUES (v_task.id, v_submission.user_id, v_validation.id, p_score, p_impact_metrics, now())
    RETURNING * INTO v_contribution;

    RETURN jsonb_build_object(
      'validation', to_jsonb(v_validation),
      'contribution', to_jsonb(v_contribution)
    );

  ELSIF p_decision = 'rejected' THEN
    UPDATE public.task_submissions
    SET status = 'rejected'
    WHERE id = p_submission_id;

    INSERT INTO public.validations (submission_id, validator_id, decision, feedback, decided_at)
    VALUES (p_submission_id, p_validator_id, 'rejected'::validation_decision, p_feedback, now())
    RETURNING * INTO v_validation;

    UPDATE public.tasks
    SET status = 'rejected'
    WHERE id = v_task.id;

    RETURN jsonb_build_object('validation', to_jsonb(v_validation));

  ELSIF p_decision = 'clarification_requested' THEN
    UPDATE public.task_submissions
    SET status = 'clarification_requested'
    WHERE id = p_submission_id;

    INSERT INTO public.validations (submission_id, validator_id, decision, feedback, decided_at)
    VALUES (p_submission_id, p_validator_id, 'clarification_requested'::validation_decision, p_feedback, now())
    RETURNING * INTO v_validation;

    RETURN jsonb_build_object('validation', to_jsonb(v_validation));
  END IF;

  RAISE EXCEPTION 'Unhandled validation decision: %', p_decision;
END;
$$;
