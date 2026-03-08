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
      events: {
        Row: {
          actor_id: string | null
          created_at: string
          entity_id: string
          entity_type: string
          id: string
          payload: Json | null
          type: Database["public"]["Enums"]["event_type"]
        }
        Insert: {
          actor_id?: string | null
          created_at?: string
          entity_id: string
          entity_type: string
          id?: string
          payload?: Json | null
          type: Database["public"]["Enums"]["event_type"]
        }
        Update: {
          actor_id?: string | null
          created_at?: string
          entity_id?: string
          entity_type?: string
          id?: string
          payload?: Json | null
          type?: Database["public"]["Enums"]["event_type"]
        }
        Relationships: []
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
      initiatives: {
        Row: {
          created_at: string
          created_by: string | null
          description: string | null
          end_date: string | null
          id: string
          is_micro: boolean
          start_date: string | null
          status: Database["public"]["Enums"]["initiative_status"]
          success_metrics: Json | null
          title: string
          updated_at: string
        }
        Insert: {
          created_at?: string
          created_by?: string | null
          description?: string | null
          end_date?: string | null
          id?: string
          is_micro?: boolean
          start_date?: string | null
          status?: Database["public"]["Enums"]["initiative_status"]
          success_metrics?: Json | null
          title: string
          updated_at?: string
        }
        Update: {
          created_at?: string
          created_by?: string | null
          description?: string | null
          end_date?: string | null
          id?: string
          is_micro?: boolean
          start_date?: string | null
          status?: Database["public"]["Enums"]["initiative_status"]
          success_metrics?: Json | null
          title?: string
          updated_at?: string
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
          phone: string | null
          status: Database["public"]["Enums"]["user_status"]
          updated_at: string
        }
        Insert: {
          avatar_url?: string | null
          created_at?: string
          display_name?: string | null
          id: string
          phone?: string | null
          status?: Database["public"]["Enums"]["user_status"]
          updated_at?: string
        }
        Update: {
          avatar_url?: string | null
          created_at?: string
          display_name?: string | null
          id?: string
          phone?: string | null
          status?: Database["public"]["Enums"]["user_status"]
          updated_at?: string
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
          lock_duration_mins: number
          locked_by: string | null
          locked_until: string | null
          org_id: string | null
          reserved_by: string | null
          reserved_until: string | null
          status: Database["public"]["Enums"]["task_status"]
          tags: string[] | null
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
          lock_duration_mins?: number
          locked_by?: string | null
          locked_until?: string | null
          org_id?: string | null
          reserved_by?: string | null
          reserved_until?: string | null
          status?: Database["public"]["Enums"]["task_status"]
          tags?: string[] | null
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
          lock_duration_mins?: number
          locked_by?: string | null
          locked_until?: string | null
          org_id?: string | null
          reserved_by?: string | null
          reserved_until?: string | null
          status?: Database["public"]["Enums"]["task_status"]
          tags?: string[] | null
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
