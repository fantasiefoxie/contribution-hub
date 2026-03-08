export type Json =
  | string
  | number
  | boolean
  | null
  | { [key: string]: Json | undefined }
  | Json[]

export type Database = {
  // Allows to automatically instantiate createClient with right options
  // instead of createClient<Database, { PostgrestVersion: 'XX' }>(URL, KEY)
  __InternalSupabase: {
    PostgrestVersion: "14.4"
  }
  public: {
    Tables: {
      api_keys: {
        Row: {
          created_at: string
          expires_at: string | null
          id: string
          is_active: boolean
          key_hash: string
          key_prefix: string
          last_used_at: string | null
          name: string
          org_id: string | null
          rate_limit_per_min: number
          scopes: string[]
          user_id: string
        }
        Insert: {
          created_at?: string
          expires_at?: string | null
          id?: string
          is_active?: boolean
          key_hash: string
          key_prefix: string
          last_used_at?: string | null
          name: string
          org_id?: string | null
          rate_limit_per_min?: number
          scopes?: string[]
          user_id: string
        }
        Update: {
          created_at?: string
          expires_at?: string | null
          id?: string
          is_active?: boolean
          key_hash?: string
          key_prefix?: string
          last_used_at?: string | null
          name?: string
          org_id?: string | null
          rate_limit_per_min?: number
          scopes?: string[]
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "api_keys_org_id_fkey"
            columns: ["org_id"]
            isOneToOne: false
            referencedRelation: "organizations"
            referencedColumns: ["id"]
          },
        ]
      }
      appeals: {
        Row: {
          appellant_id: string
          created_at: string
          id: string
          reason: string
          resolution: string | null
          resolved_at: string | null
          resolved_by: string | null
          status: string
          submission_id: string
          validation_id: string
        }
        Insert: {
          appellant_id: string
          created_at?: string
          id?: string
          reason: string
          resolution?: string | null
          resolved_at?: string | null
          resolved_by?: string | null
          status?: string
          submission_id: string
          validation_id: string
        }
        Update: {
          appellant_id?: string
          created_at?: string
          id?: string
          reason?: string
          resolution?: string | null
          resolved_at?: string | null
          resolved_by?: string | null
          status?: string
          submission_id?: string
          validation_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "appeals_submission_id_fkey"
            columns: ["submission_id"]
            isOneToOne: false
            referencedRelation: "task_submissions"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "appeals_validation_id_fkey"
            columns: ["validation_id"]
            isOneToOne: false
            referencedRelation: "validations"
            referencedColumns: ["id"]
          },
        ]
      }
      audit_logs: {
        Row: {
          action: string
          actor_id: string
          created_at: string
          details: Json | null
          entity_id: string
          entity_type: string
          id: string
        }
        Insert: {
          action: string
          actor_id: string
          created_at?: string
          details?: Json | null
          entity_id: string
          entity_type: string
          id?: string
        }
        Update: {
          action?: string
          actor_id?: string
          created_at?: string
          details?: Json | null
          entity_id?: string
          entity_type?: string
          id?: string
        }
        Relationships: []
      }
      claim_attempts: {
        Row: {
          attempted_at: string
          id: string
          user_id: string
        }
        Insert: {
          attempted_at?: string
          id?: string
          user_id: string
        }
        Update: {
          attempted_at?: string
          id?: string
          user_id?: string
        }
        Relationships: []
      }
      contributions: {
        Row: {
          created_at: string
          id: string
          impact_metrics: Json | null
          score: number
          task_id: string
          user_id: string
          validation_id: string | null
        }
        Insert: {
          created_at?: string
          id?: string
          impact_metrics?: Json | null
          score?: number
          task_id: string
          user_id: string
          validation_id?: string | null
        }
        Update: {
          created_at?: string
          id?: string
          impact_metrics?: Json | null
          score?: number
          task_id?: string
          user_id?: string
          validation_id?: string | null
        }
        Relationships: [
          {
            foreignKeyName: "contributions_task_id_fkey"
            columns: ["task_id"]
            isOneToOne: false
            referencedRelation: "tasks"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "contributions_validation_id_fkey"
            columns: ["validation_id"]
            isOneToOne: false
            referencedRelation: "validations"
            referencedColumns: ["id"]
          },
        ]
      }
      credential_metadata: {
        Row: {
          created_at: string
          credential_id: string
          id: string
          key: string
          value: string
        }
        Insert: {
          created_at?: string
          credential_id: string
          id?: string
          key: string
          value: string
        }
        Update: {
          created_at?: string
          credential_id?: string
          id?: string
          key?: string
          value?: string
        }
        Relationships: [
          {
            foreignKeyName: "credential_metadata_credential_id_fkey"
            columns: ["credential_id"]
            isOneToOne: false
            referencedRelation: "credentials"
            referencedColumns: ["id"]
          },
        ]
      }
      credentials: {
        Row: {
          description: string | null
          id: string
          initiative_id: string | null
          is_active: boolean
          issued_at: string
          issued_by: string | null
          metadata: Json | null
          org_id: string | null
          revoked_at: string | null
          revoked_by: string | null
          title: string
          type: string
          user_id: string
        }
        Insert: {
          description?: string | null
          id?: string
          initiative_id?: string | null
          is_active?: boolean
          issued_at?: string
          issued_by?: string | null
          metadata?: Json | null
          org_id?: string | null
          revoked_at?: string | null
          revoked_by?: string | null
          title: string
          type?: string
          user_id: string
        }
        Update: {
          description?: string | null
          id?: string
          initiative_id?: string | null
          is_active?: boolean
          issued_at?: string
          issued_by?: string | null
          metadata?: Json | null
          org_id?: string | null
          revoked_at?: string | null
          revoked_by?: string | null
          title?: string
          type?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "credentials_initiative_id_fkey"
            columns: ["initiative_id"]
            isOneToOne: false
            referencedRelation: "initiatives"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "credentials_org_id_fkey"
            columns: ["org_id"]
            isOneToOne: false
            referencedRelation: "organizations"
            referencedColumns: ["id"]
          },
        ]
      }
      events: {
        Row: {
          actor_id: string | null
          created_at: string
          entity_id: string
          entity_type: string
          id: string
          payload: Json | null
          type: Database["public"]["Enums"]["event_type"]
          version: number
        }
        Insert: {
          actor_id?: string | null
          created_at?: string
          entity_id: string
          entity_type: string
          id?: string
          payload?: Json | null
          type: Database["public"]["Enums"]["event_type"]
          version?: number
        }
        Update: {
          actor_id?: string | null
          created_at?: string
          entity_id?: string
          entity_type?: string
          id?: string
          payload?: Json | null
          type?: Database["public"]["Enums"]["event_type"]
          version?: number
        }
        Relationships: []
      }
      idempotency_keys: {
        Row: {
          created_at: string
          expires_at: string
          id: string
          key: string
          operation: string
          result: Json | null
          user_id: string
        }
        Insert: {
          created_at?: string
          expires_at?: string
          id?: string
          key: string
          operation: string
          result?: Json | null
          user_id: string
        }
        Update: {
          created_at?: string
          expires_at?: string
          id?: string
          key?: string
          operation?: string
          result?: Json | null
          user_id?: string
        }
        Relationships: []
      }
      initiative_milestones: {
        Row: {
          id: string
          initiative_id: string
          percentage: number
          reached_at: string
        }
        Insert: {
          id?: string
          initiative_id: string
          percentage: number
          reached_at?: string
        }
        Update: {
          id?: string
          initiative_id?: string
          percentage?: number
          reached_at?: string
        }
        Relationships: [
          {
            foreignKeyName: "initiative_milestones_initiative_id_fkey"
            columns: ["initiative_id"]
            isOneToOne: false
            referencedRelation: "initiatives"
            referencedColumns: ["id"]
          },
        ]
      }
      initiative_orgs: {
        Row: {
          id: string
          initiative_id: string
          org_id: string
        }
        Insert: {
          id?: string
          initiative_id: string
          org_id: string
        }
        Update: {
          id?: string
          initiative_id?: string
          org_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "initiative_orgs_initiative_id_fkey"
            columns: ["initiative_id"]
            isOneToOne: false
            referencedRelation: "initiatives"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "initiative_orgs_org_id_fkey"
            columns: ["org_id"]
            isOneToOne: false
            referencedRelation: "organizations"
            referencedColumns: ["id"]
          },
        ]
      }
      initiative_participants: {
        Row: {
          id: string
          initiative_id: string
          joined_at: string
          user_id: string
        }
        Insert: {
          id?: string
          initiative_id: string
          joined_at?: string
          user_id: string
        }
        Update: {
          id?: string
          initiative_id?: string
          joined_at?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "initiative_participants_initiative_id_fkey"
            columns: ["initiative_id"]
            isOneToOne: false
            referencedRelation: "initiatives"
            referencedColumns: ["id"]
          },
        ]
      }
      initiatives: {
        Row: {
          category: string | null
          created_at: string
          created_by: string | null
          description: string | null
          end_date: string | null
          featured_at: string | null
          id: string
          is_micro: boolean
          is_online: boolean
          latitude: number | null
          longitude: number | null
          max_tasks: number | null
          max_volunteers: number | null
          priority_score: number
          promoted_at: string | null
          promoted_by: string | null
          region_name: string | null
          start_date: string | null
          status: Database["public"]["Enums"]["initiative_status"]
          success_metrics: Json | null
          target_metrics: Json | null
          title: string
          updated_at: string
          volunteer_target: number | null
        }
        Insert: {
          category?: string | null
          created_at?: string
          created_by?: string | null
          description?: string | null
          end_date?: string | null
          featured_at?: string | null
          id?: string
          is_micro?: boolean
          is_online?: boolean
          latitude?: number | null
          longitude?: number | null
          max_tasks?: number | null
          max_volunteers?: number | null
          priority_score?: number
          promoted_at?: string | null
          promoted_by?: string | null
          region_name?: string | null
          start_date?: string | null
          status?: Database["public"]["Enums"]["initiative_status"]
          success_metrics?: Json | null
          target_metrics?: Json | null
          title: string
          updated_at?: string
          volunteer_target?: number | null
        }
        Update: {
          category?: string | null
          created_at?: string
          created_by?: string | null
          description?: string | null
          end_date?: string | null
          featured_at?: string | null
          id?: string
          is_micro?: boolean
          is_online?: boolean
          latitude?: number | null
          longitude?: number | null
          max_tasks?: number | null
          max_volunteers?: number | null
          priority_score?: number
          promoted_at?: string | null
          promoted_by?: string | null
          region_name?: string | null
          start_date?: string | null
          status?: Database["public"]["Enums"]["initiative_status"]
          success_metrics?: Json | null
          target_metrics?: Json | null
          title?: string
          updated_at?: string
          volunteer_target?: number | null
        }
        Relationships: []
      }
      notifications: {
        Row: {
          body: string | null
          created_at: string
          data: Json | null
          id: string
          read: boolean
          title: string
          type: string
          user_id: string
        }
        Insert: {
          body?: string | null
          created_at?: string
          data?: Json | null
          id?: string
          read?: boolean
          title: string
          type: string
          user_id: string
        }
        Update: {
          body?: string | null
          created_at?: string
          data?: Json | null
          id?: string
          read?: boolean
          title?: string
          type?: string
          user_id?: string
        }
        Relationships: []
      }
      org_members: {
        Row: {
          id: string
          joined_at: string
          org_id: string
          role: Database["public"]["Enums"]["org_role"]
          user_id: string
        }
        Insert: {
          id?: string
          joined_at?: string
          org_id: string
          role?: Database["public"]["Enums"]["org_role"]
          user_id: string
        }
        Update: {
          id?: string
          joined_at?: string
          org_id?: string
          role?: Database["public"]["Enums"]["org_role"]
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "org_members_org_id_fkey"
            columns: ["org_id"]
            isOneToOne: false
            referencedRelation: "organizations"
            referencedColumns: ["id"]
          },
        ]
      }
      organizations: {
        Row: {
          created_at: string
          description: string | null
          id: string
          logo_url: string | null
          name: string
          slug: string
          updated_at: string
        }
        Insert: {
          created_at?: string
          description?: string | null
          id?: string
          logo_url?: string | null
          name: string
          slug: string
          updated_at?: string
        }
        Update: {
          created_at?: string
          description?: string | null
          id?: string
          logo_url?: string | null
          name?: string
          slug?: string
          updated_at?: string
        }
        Relationships: []
      }
      profiles: {
        Row: {
          avatar_url: string | null
          created_at: string
          display_name: string | null
          id: string
          interests: string[] | null
          latitude: number | null
          longitude: number | null
          max_active_tasks: number
          phone: string | null
          status: Database["public"]["Enums"]["user_status"]
          updated_at: string
        }
        Insert: {
          avatar_url?: string | null
          created_at?: string
          display_name?: string | null
          id: string
          interests?: string[] | null
          latitude?: number | null
          longitude?: number | null
          max_active_tasks?: number
          phone?: string | null
          status?: Database["public"]["Enums"]["user_status"]
          updated_at?: string
        }
        Update: {
          avatar_url?: string | null
          created_at?: string
          display_name?: string | null
          id?: string
          interests?: string[] | null
          latitude?: number | null
          longitude?: number | null
          max_active_tasks?: number
          phone?: string | null
          status?: Database["public"]["Enums"]["user_status"]
          updated_at?: string
        }
        Relationships: []
      }
      projects: {
        Row: {
          created_at: string
          description: string | null
          id: string
          initiative_id: string
          sort_order: number
          title: string
        }
        Insert: {
          created_at?: string
          description?: string | null
          id?: string
          initiative_id: string
          sort_order?: number
          title: string
        }
        Update: {
          created_at?: string
          description?: string | null
          id?: string
          initiative_id?: string
          sort_order?: number
          title?: string
        }
        Relationships: [
          {
            foreignKeyName: "projects_initiative_id_fkey"
            columns: ["initiative_id"]
            isOneToOne: false
            referencedRelation: "initiatives"
            referencedColumns: ["id"]
          },
        ]
      }
      rate_limit_entries: {
        Row: {
          action: string
          count: number
          id: string
          identifier: string
          window_start: string
        }
        Insert: {
          action: string
          count?: number
          id?: string
          identifier: string
          window_start?: string
        }
        Update: {
          action?: string
          count?: number
          id?: string
          identifier?: string
          window_start?: string
        }
        Relationships: []
      }
      regions: {
        Row: {
          created_at: string
          description: string | null
          id: string
          initiative_id: string
          name: string
          sort_order: number
        }
        Insert: {
          created_at?: string
          description?: string | null
          id?: string
          initiative_id: string
          name: string
          sort_order?: number
        }
        Update: {
          created_at?: string
          description?: string | null
          id?: string
          initiative_id?: string
          name?: string
          sort_order?: number
        }
        Relationships: [
          {
            foreignKeyName: "regions_initiative_id_fkey"
            columns: ["initiative_id"]
            isOneToOne: false
            referencedRelation: "initiatives"
            referencedColumns: ["id"]
          },
        ]
      }
      role_permissions: {
        Row: {
          id: string
          role: string
          scope: string
        }
        Insert: {
          id?: string
          role: string
          scope: string
        }
        Update: {
          id?: string
          role?: string
          scope?: string
        }
        Relationships: []
      }
      task_claims: {
        Row: {
          claimed_at: string
          expires_at: string
          id: string
          status: string
          task_id: string
          user_id: string
        }
        Insert: {
          claimed_at?: string
          expires_at: string
          id?: string
          status?: string
          task_id: string
          user_id: string
        }
        Update: {
          claimed_at?: string
          expires_at?: string
          id?: string
          status?: string
          task_id?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "task_claims_task_id_fkey"
            columns: ["task_id"]
            isOneToOne: false
            referencedRelation: "tasks"
            referencedColumns: ["id"]
          },
        ]
      }
      task_submissions: {
        Row: {
          attachments: Json | null
          content: string
          id: string
          status: string
          submitted_at: string
          task_id: string
          user_id: string
        }
        Insert: {
          attachments?: Json | null
          content: string
          id?: string
          status?: string
          submitted_at?: string
          task_id: string
          user_id: string
        }
        Update: {
          attachments?: Json | null
          content?: string
          id?: string
          status?: string
          submitted_at?: string
          task_id?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "task_submissions_task_id_fkey"
            columns: ["task_id"]
            isOneToOne: false
            referencedRelation: "tasks"
            referencedColumns: ["id"]
          },
        ]
      }
      task_updates: {
        Row: {
          attachments: Json | null
          created_at: string
          id: string
          notes: string | null
          task_id: string
          user_id: string
        }
        Insert: {
          attachments?: Json | null
          created_at?: string
          id?: string
          notes?: string | null
          task_id: string
          user_id: string
        }
        Update: {
          attachments?: Json | null
          created_at?: string
          id?: string
          notes?: string | null
          task_id?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "task_updates_task_id_fkey"
            columns: ["task_id"]
            isOneToOne: false
            referencedRelation: "tasks"
            referencedColumns: ["id"]
          },
        ]
      }
      tasks: {
        Row: {
          created_at: string
          created_by: string
          description: string | null
          difficulty: number
          expires_at: string | null
          id: string
          initiative_id: string | null
          is_priority: boolean
          is_team: boolean
          latitude: number | null
          lock_duration_mins: number
          locked_by: string | null
          locked_until: string | null
          longitude: number | null
          org_id: string | null
          project_id: string | null
          region_id: string | null
          reserved_by: string | null
          reserved_until: string | null
          status: Database["public"]["Enums"]["task_status"]
          tags: string[] | null
          task_pool: string
          team_target: number | null
          title: string
          updated_at: string
          visibility: Database["public"]["Enums"]["task_visibility"]
        }
        Insert: {
          created_at?: string
          created_by: string
          description?: string | null
          difficulty?: number
          expires_at?: string | null
          id?: string
          initiative_id?: string | null
          is_priority?: boolean
          is_team?: boolean
          latitude?: number | null
          lock_duration_mins?: number
          locked_by?: string | null
          locked_until?: string | null
          longitude?: number | null
          org_id?: string | null
          project_id?: string | null
          region_id?: string | null
          reserved_by?: string | null
          reserved_until?: string | null
          status?: Database["public"]["Enums"]["task_status"]
          tags?: string[] | null
          task_pool?: string
          team_target?: number | null
          title: string
          updated_at?: string
          visibility?: Database["public"]["Enums"]["task_visibility"]
        }
        Update: {
          created_at?: string
          created_by?: string
          description?: string | null
          difficulty?: number
          expires_at?: string | null
          id?: string
          initiative_id?: string | null
          is_priority?: boolean
          is_team?: boolean
          latitude?: number | null
          lock_duration_mins?: number
          locked_by?: string | null
          locked_until?: string | null
          longitude?: number | null
          org_id?: string | null
          project_id?: string | null
          region_id?: string | null
          reserved_by?: string | null
          reserved_until?: string | null
          status?: Database["public"]["Enums"]["task_status"]
          tags?: string[] | null
          task_pool?: string
          team_target?: number | null
          title?: string
          updated_at?: string
          visibility?: Database["public"]["Enums"]["task_visibility"]
        }
        Relationships: [
          {
            foreignKeyName: "tasks_initiative_id_fkey"
            columns: ["initiative_id"]
            isOneToOne: false
            referencedRelation: "initiatives"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "tasks_org_id_fkey"
            columns: ["org_id"]
            isOneToOne: false
            referencedRelation: "organizations"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "tasks_project_id_fkey"
            columns: ["project_id"]
            isOneToOne: false
            referencedRelation: "projects"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "tasks_region_id_fkey"
            columns: ["region_id"]
            isOneToOne: false
            referencedRelation: "regions"
            referencedColumns: ["id"]
          },
        ]
      }
      team_members: {
        Row: {
          id: string
          invite_code: string | null
          joined_at: string
          region_id: string | null
          task_id: string
          user_id: string
        }
        Insert: {
          id?: string
          invite_code?: string | null
          joined_at?: string
          region_id?: string | null
          task_id: string
          user_id: string
        }
        Update: {
          id?: string
          invite_code?: string | null
          joined_at?: string
          region_id?: string | null
          task_id?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "team_members_region_id_fkey"
            columns: ["region_id"]
            isOneToOne: false
            referencedRelation: "regions"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "team_members_task_id_fkey"
            columns: ["task_id"]
            isOneToOne: false
            referencedRelation: "tasks"
            referencedColumns: ["id"]
          },
        ]
      }
      user_roles: {
        Row: {
          id: string
          role: Database["public"]["Enums"]["org_role"]
          user_id: string
        }
        Insert: {
          id?: string
          role: Database["public"]["Enums"]["org_role"]
          user_id: string
        }
        Update: {
          id?: string
          role?: Database["public"]["Enums"]["org_role"]
          user_id?: string
        }
        Relationships: []
      }
      validations: {
        Row: {
          decided_at: string
          decision: Database["public"]["Enums"]["validation_decision"]
          feedback: string | null
          id: string
          submission_id: string
          validator_id: string
        }
        Insert: {
          decided_at?: string
          decision: Database["public"]["Enums"]["validation_decision"]
          feedback?: string | null
          id?: string
          submission_id: string
          validator_id: string
        }
        Update: {
          decided_at?: string
          decision?: Database["public"]["Enums"]["validation_decision"]
          feedback?: string | null
          id?: string
          submission_id?: string
          validator_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "validations_submission_id_fkey"
            columns: ["submission_id"]
            isOneToOne: false
            referencedRelation: "task_submissions"
            referencedColumns: ["id"]
          },
        ]
      }
      webhook_subscriptions: {
        Row: {
          api_key_id: string
          created_at: string
          events: string[]
          failure_count: number
          id: string
          is_active: boolean
          last_triggered_at: string | null
          secret: string
          url: string
        }
        Insert: {
          api_key_id: string
          created_at?: string
          events?: string[]
          failure_count?: number
          id?: string
          is_active?: boolean
          last_triggered_at?: string | null
          secret: string
          url: string
        }
        Update: {
          api_key_id?: string
          created_at?: string
          events?: string[]
          failure_count?: number
          id?: string
          is_active?: boolean
          last_triggered_at?: string | null
          secret?: string
          url?: string
        }
        Relationships: [
          {
            foreignKeyName: "webhook_subscriptions_api_key_id_fkey"
            columns: ["api_key_id"]
            isOneToOne: false
            referencedRelation: "api_keys"
            referencedColumns: ["id"]
          },
        ]
      }
    }
    Views: {
      [_ in never]: never
    }
    Functions: {
      expire_reservations: { Args: never; Returns: undefined }
      has_role: {
        Args: {
          _role: Database["public"]["Enums"]["org_role"]
          _user_id: string
        }
        Returns: boolean
      }
      is_org_admin: {
        Args: { _org_id: string; _user_id: string }
        Returns: boolean
      }
      is_org_member: {
        Args: { _org_id: string; _user_id: string }
        Returns: boolean
      }
    }
    Enums: {
      badge_level: "bronze" | "silver" | "gold" | "platinum"
      event_type:
        | "TaskCreated"
        | "TaskReserved"
        | "TaskClaimed"
        | "TaskProgressUpdated"
        | "TaskSubmitted"
        | "ValidationApproved"
        | "ValidationRejected"
        | "ContributionRecorded"
        | "BadgeAwarded"
        | "CredentialIssued"
        | "CredentialRevoked"
        | "RoleGranted"
        | "RoleRevoked"
        | "InitiativeJoined"
        | "InitiativePriorityUpdated"
        | "InitiativeCreated"
      initiative_status: "draft" | "active" | "completed" | "cancelled"
      org_role: "member" | "validator" | "moderator" | "admin"
      task_status:
        | "open"
        | "reserved"
        | "claimed"
        | "in_progress"
        | "submitted"
        | "validation_pending"
        | "validated"
        | "rejected"
        | "expired"
      task_visibility: "private" | "organization" | "public"
      user_status: "anonymous" | "registered" | "trusted"
      validation_decision: "approved" | "rejected" | "clarification_requested"
    }
    CompositeTypes: {
      [_ in never]: never
    }
  }
}

type DatabaseWithoutInternals = Omit<Database, "__InternalSupabase">

type DefaultSchema = DatabaseWithoutInternals[Extract<keyof Database, "public">]

export type Tables<
  DefaultSchemaTableNameOrOptions extends
    | keyof (DefaultSchema["Tables"] & DefaultSchema["Views"])
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
        DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])
    : never = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
      DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])[TableName] extends {
      Row: infer R
    }
    ? R
    : never
  : DefaultSchemaTableNameOrOptions extends keyof (DefaultSchema["Tables"] &
        DefaultSchema["Views"])
    ? (DefaultSchema["Tables"] &
        DefaultSchema["Views"])[DefaultSchemaTableNameOrOptions] extends {
        Row: infer R
      }
      ? R
      : never
    : never

export type TablesInsert<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Insert: infer I
    }
    ? I
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Insert: infer I
      }
      ? I
      : never
    : never

export type TablesUpdate<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Update: infer U
    }
    ? U
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Update: infer U
      }
      ? U
      : never
    : never

export type Enums<
  DefaultSchemaEnumNameOrOptions extends
    | keyof DefaultSchema["Enums"]
    | { schema: keyof DatabaseWithoutInternals },
  EnumName extends DefaultSchemaEnumNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"]
    : never = never,
> = DefaultSchemaEnumNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"][EnumName]
  : DefaultSchemaEnumNameOrOptions extends keyof DefaultSchema["Enums"]
    ? DefaultSchema["Enums"][DefaultSchemaEnumNameOrOptions]
    : never

export type CompositeTypes<
  PublicCompositeTypeNameOrOptions extends
    | keyof DefaultSchema["CompositeTypes"]
    | { schema: keyof DatabaseWithoutInternals },
  CompositeTypeName extends PublicCompositeTypeNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"]
    : never = never,
> = PublicCompositeTypeNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"][CompositeTypeName]
  : PublicCompositeTypeNameOrOptions extends keyof DefaultSchema["CompositeTypes"]
    ? DefaultSchema["CompositeTypes"][PublicCompositeTypeNameOrOptions]
    : never

export const Constants = {
  public: {
    Enums: {
      badge_level: ["bronze", "silver", "gold", "platinum"],
      event_type: [
        "TaskCreated",
        "TaskReserved",
        "TaskClaimed",
        "TaskProgressUpdated",
        "TaskSubmitted",
        "ValidationApproved",
        "ValidationRejected",
        "ContributionRecorded",
        "BadgeAwarded",
        "CredentialIssued",
        "CredentialRevoked",
        "RoleGranted",
        "RoleRevoked",
        "InitiativeJoined",
        "InitiativePriorityUpdated",
        "InitiativeCreated",
      ],
      initiative_status: ["draft", "active", "completed", "cancelled"],
      org_role: ["member", "validator", "moderator", "admin"],
      task_status: [
        "open",
        "reserved",
        "claimed",
        "in_progress",
        "submitted",
        "validation_pending",
        "validated",
        "rejected",
        "expired",
      ],
      task_visibility: ["private", "organization", "public"],
      user_status: ["anonymous", "registered", "trusted"],
      validation_decision: ["approved", "rejected", "clarification_requested"],
    },
  },
} as const
