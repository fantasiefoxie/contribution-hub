import { useState } from "react";
import {
  BookOpen, Users, Target, CheckSquare, Shield, Zap, Globe, Code2, FileText,
  ChevronRight, Search, ArrowRight, Layers, Eye, Award, Bell, BarChart3, Settings
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion";
import AppShell from "@/components/layout/AppShell";

/* ─── Section data ─── */

const GETTING_STARTED_STEPS = [
  { icon: Users, title: "Create an Account", desc: "Sign up with your email. You'll receive a verification link to confirm your identity." },
  { icon: Search, title: "Browse Missions", desc: "Explore the Mission Feed for open tasks. Filter by difficulty, tags, or initiative." },
  { icon: Target, title: "Claim a Task", desc: "Reserve a task (60s window), then confirm your claim. You'll have a time lock to work on it." },
  { icon: CheckSquare, title: "Submit Your Work", desc: "Add progress updates, then submit your completed work for peer validation." },
  { icon: Award, title: "Earn Credentials", desc: "Upon approval, you receive contribution points and verifiable credentials on your profile." },
];

const CORE_MODULES = [
  { icon: Users, name: "Identity", desc: "Profiles, roles (member / validator / moderator / admin), and contributor stats. Every user starts as 'registered' and can become 'trusted'." },
  { icon: Target, name: "Task Engine", desc: "Full task lifecycle: create → reserve → claim → progress → submit → validate. Supports solo and team missions with configurable lock durations." },
  { icon: Eye, name: "Validation", desc: "Peer review with three outcomes: approve, reject, or request clarification. Validators cannot review their own submissions or tasks they created." },
  { icon: FileText, name: "Contribution Ledger", desc: "Immutable record of approved contributions. Score = difficulty × 10. Impact metrics tracked per contribution." },
  { icon: Layers, name: "Initiative Engine", desc: "Group tasks under initiatives with projects, milestones, and priority scoring. Supports micro-initiatives (≤30 days, ≤10 tasks, ≤25 volunteers) that can be promoted to full." },
  { icon: Globe, name: "Campaign Ops", desc: "Operational tools for initiative managers: volunteer allocation, content scheduling, and region management." },
  { icon: Bell, name: "Notifications", desc: "Real-time notifications for task claims, validation results, appeals, and initiative updates." },
  { icon: BarChart3, name: "Analytics & Observability", desc: "Contributor stats, org metrics, initiative progress tracking, and system health monitoring." },
  { icon: Shield, name: "Governance", desc: "Appeals system for rejected submissions. Moderators/admins can override decisions. Full audit log for transparency." },
  { icon: Code2, name: "Protocol API", desc: "Public REST API with API key auth, granular scopes, idempotency, rate limiting, and HMAC-SHA256 signed webhooks." },
];

const TASK_STATUSES = [
  { status: "open", color: "bg-primary/10 text-primary", desc: "Available for volunteers to claim" },
  { status: "reserved", color: "bg-secondary/10 text-secondary", desc: "Soft-locked for 60 seconds while volunteer confirms" },
  { status: "claimed", color: "bg-accent/10 text-accent", desc: "Locked to a volunteer with a time-bounded work window" },
  { status: "in_progress", color: "bg-info/10 text-accent", desc: "Volunteer has posted at least one progress update" },
  { status: "submitted", color: "bg-secondary/10 text-secondary", desc: "Work submitted, awaiting validation assignment" },
  { status: "validation_pending", color: "bg-warning/10 text-secondary", desc: "Assigned for peer validation" },
  { status: "validated", color: "bg-primary/10 text-primary", desc: "Approved — contribution recorded to ledger" },
  { status: "rejected", color: "bg-destructive/10 text-destructive", desc: "Rejected by validator (can be appealed)" },
  { status: "expired", color: "bg-muted text-muted-foreground", desc: "Lock expired before submission" },
];

const ROLE_PERMISSIONS: Record<string, string[]> = {
  member: ["Browse & claim tasks", "Submit work", "Join initiatives", "View own contributions"],
  validator: ["All member permissions", "Review pending submissions", "Approve / reject / request clarification"],
  moderator: ["All validator permissions", "Resolve appeals", "Manage initiative tasks"],
  admin: ["All permissions", "Grant/revoke roles", "Issue/revoke credentials", "Update initiative priority", "Access audit logs & analytics"],
};

const FAQ: { q: string; a: string }[] = [
  { q: "How does the two-step claim process work?", a: "First you 'reserve' a task (60-second soft lock). During this time, others can't claim it. Then you 'confirm' to start the work timer. If you don't confirm in time, the task returns to the open pool." },
  { q: "What are micro-initiatives?", a: "Lightweight initiatives capped at 10 tasks, 25 volunteers, and 30 days. Perfect for quick community sprints. They can be promoted to full initiatives if they gain traction." },
  { q: "Can I appeal a rejected submission?", a: "Yes. You can create an appeal with a reason. Organization moderators or admins will review and can either uphold or override the original decision." },
  { q: "How are contribution scores calculated?", a: "Base score = task difficulty (1-5) × 10. Impact metrics like tags and difficulty are stored alongside for richer analytics." },
  { q: "What is the Zero-AI baseline?", a: "The core coordination engine runs entirely on deterministic logic — no AI dependencies. An optional intelligence layer provides advisory outputs (campaign planning, task template generation) that always require human confirmation." },
  { q: "How do webhooks work?", a: "Create an API key, then register a webhook URL with event filters. Events are delivered as signed POST requests using HMAC-SHA256. Failed deliveries retry up to 10 times." },
  { q: "What are verifiable credentials?", a: "On-platform digital certificates issued when you complete tasks or milestones. They include metadata, are tied to your profile, and can be independently verified via the API." },
  { q: "Is there a guided tour for new users?", a: "Yes! The interactive walkthrough starts automatically on your first visit. It highlights each part of the Mission Board, navigation, and key features with step-by-step tooltips. You can restart it anytime by tapping the help (?) button in the bottom-right corner of the home screen." },
];

const API_SECTIONS = [
  {
    title: "Authentication",
    content: `Two methods are supported:

**API Key** — Include \`x-api-key: cos_<64 hex chars>\` in headers. Rate limit: 60 req/min (configurable).

**Bearer Token** — Include \`Authorization: Bearer <jwt>\` from your session. Rate limit: 120 req/min.

All write operations support idempotency via the \`x-idempotency-key\` header (24h expiry).`,
  },
  {
    title: "Endpoints Overview",
    endpoints: [
      { method: "GET", path: "/users", desc: "List volunteer profiles" },
      { method: "GET", path: "/users/:id", desc: "Profile with stats, credentials, roles" },
      { method: "GET", path: "/tasks", desc: "List public tasks (?status, ?initiative_id)" },
      { method: "GET", path: "/tasks/:id", desc: "Task detail" },
      { method: "POST", path: "/tasks", desc: "Create a task" },
      { method: "GET", path: "/initiatives", desc: "List by priority (?status)" },
      { method: "GET", path: "/initiatives/:id", desc: "Detail with participants & tasks" },
      { method: "POST", path: "/initiatives", desc: "Create initiative" },
      { method: "POST", path: "/initiatives/:id/join", desc: "Join initiative" },
      { method: "GET", path: "/contributions", desc: "List contributions (?user_id, ?task_id)" },
      { method: "GET", path: "/credentials", desc: "List credentials (?user_id, ?type)" },
      { method: "POST", path: "/credentials", desc: "Issue credential" },
      { method: "GET", path: "/events", desc: "Query event stream (?type, ?since, ?version)" },
      { method: "GET", path: "/roles", desc: "List roles/permissions" },
      { method: "POST", path: "/roles/grant", desc: "Grant role to user" },
      { method: "GET/POST", path: "/keys", desc: "Manage API keys" },
      { method: "GET/POST", path: "/webhooks", desc: "Manage webhook subscriptions" },
    ],
  },
  {
    title: "Events & Webhooks",
    content: `All state changes emit versioned events. Current version: **1**.

**Event Types:** TaskCreated, TaskReserved, TaskClaimed, TaskSubmitted, ValidationApproved, ValidationRejected, ContributionRecorded, InitiativeCreated, CredentialIssued, CredentialRevoked, RoleGranted, RoleRevoked, InitiativeJoined, InitiativePriorityUpdated.

**Webhook Delivery:** POST to your URL with headers \`X-Webhook-Signature\` (HMAC-SHA256), \`X-Event-Type\`, and \`X-Protocol-Version\`. Body: \`{ event, data, timestamp, version }\`.`,
  },
  {
    title: "Permission Scopes",
    content: `**Legacy:** users, tasks, initiatives, contributions, events, webhooks, *

**Granular (v2):** tasks:read, tasks:create, tasks:claim, tasks:validate, tasks:manage, initiatives:read, initiatives:create, initiatives:manage, initiatives:join, initiatives:prioritize, contributions:read, profiles:read, submissions:review, credentials:read, credentials:issue, credentials:revoke, roles:read, roles:grant, roles:revoke`,
  },
];

const DB_TABLES = [
  { name: "profiles", desc: "User profiles with display name, interests, location, status" },
  { name: "user_roles", desc: "Separate role assignments (member, validator, moderator, admin)" },
  { name: "tasks", desc: "Core task records with status lifecycle, visibility, difficulty, tags" },
  { name: "task_claims", desc: "Claim history with expiry timestamps" },
  { name: "task_submissions", desc: "Submitted work with content and attachments" },
  { name: "task_updates", desc: "Progress updates (notes + attachments)" },
  { name: "validations", desc: "Peer review decisions (approved, rejected, clarification_requested)" },
  { name: "contributions", desc: "Immutable ledger of approved contributions with scores" },
  { name: "appeals", desc: "Appeal records for rejected submissions" },
  { name: "initiatives", desc: "Campaign/initiative definitions with priority scoring" },
  { name: "initiative_participants", desc: "Join records for initiative volunteers" },
  { name: "projects", desc: "Sub-groupings within initiatives" },
  { name: "credentials", desc: "Verifiable digital certificates" },
  { name: "organizations", desc: "Organization profiles with slug-based routing" },
  { name: "events", desc: "Immutable event stream for all state changes" },
  { name: "api_keys", desc: "Developer API keys with scoped permissions" },
  { name: "webhook_subscriptions", desc: "Registered webhook endpoints" },
  { name: "audit_logs", desc: "Immutable audit trail for governance" },
  { name: "notifications", desc: "Per-user notification records" },
];

export default function Documentation() {
  const [search, setSearch] = useState("");

  return (
    <AppShell>
      <div className="px-4 pt-6 pb-3">
        <div className="flex items-center gap-2">
          <BookOpen className="h-5 w-5 text-primary" />
          <h1 className="font-display text-2xl font-bold">Documentation</h1>
        </div>
        <p className="text-sm text-muted-foreground mt-1">
          Complete guide to Contribution OS — the open coordination protocol
        </p>
      </div>

      {/* Search */}
      <div className="px-4 mb-4">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search documentation..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="pl-9 rounded-xl bg-card border-border"
          />
        </div>
      </div>

      <Tabs defaultValue="overview" className="px-4 pb-24">
        <TabsList className="grid w-full grid-cols-5 mb-4 rounded-xl bg-muted/60">
          <TabsTrigger value="overview" className="rounded-lg text-[10px] sm:text-xs data-[state=active]:bg-card">Overview</TabsTrigger>
          <TabsTrigger value="guides" className="rounded-lg text-[10px] sm:text-xs data-[state=active]:bg-card">Guides</TabsTrigger>
          <TabsTrigger value="api" className="rounded-lg text-[10px] sm:text-xs data-[state=active]:bg-card">API</TabsTrigger>
          <TabsTrigger value="data" className="rounded-lg text-[10px] sm:text-xs data-[state=active]:bg-card">Data Model</TabsTrigger>
          <TabsTrigger value="faq" className="rounded-lg text-[10px] sm:text-xs data-[state=active]:bg-card">FAQ</TabsTrigger>
        </TabsList>

        {/* ═══ OVERVIEW TAB ═══ */}
        <TabsContent value="overview" className="space-y-5">
          {/* Hero */}
          <div className="bg-gradient-to-br from-primary/10 via-accent/5 to-secondary/10 rounded-2xl border border-primary/20 p-5">
            <h2 className="font-display text-lg font-bold mb-2">What is Contribution OS?</h2>
            <p className="text-sm text-muted-foreground leading-relaxed">
              A platform for coordinating volunteers to complete real-world tasks created by organizations.
              It provides contribution tracking, peer validation, analytics, verifiable credentials, and a
              public REST API — all built on a <span className="font-semibold text-foreground">Zero-AI baseline</span> where
              the core engine runs entirely on deterministic logic.
            </p>
          </div>

          {/* Architecture */}
          <div className="bg-card rounded-xl border border-border p-4">
            <h3 className="font-display font-semibold text-sm mb-3 flex items-center gap-2">
              <Layers className="h-4 w-4 text-primary" />
              Architecture — Two Layers
            </h3>
            <div className="space-y-3">
              <div className="bg-primary/5 rounded-lg p-3 border border-primary/10">
                <p className="text-[10px] font-semibold text-primary uppercase tracking-wide mb-1">Core Coordination Engine</p>
                <p className="text-xs text-muted-foreground">Deterministic logic only. No AI dependencies.</p>
                <div className="flex flex-wrap gap-1 mt-2">
                  {["Identity", "Tasks", "Validation", "Ledger", "Initiatives", "Campaigns", "Discovery", "Governance", "Observability", "Protocol API"].map(m => (
                    <Badge key={m} variant="outline" className="text-[8px]">{m}</Badge>
                  ))}
                </div>
              </div>
              <div className="bg-accent/5 rounded-lg p-3 border border-accent/10">
                <p className="text-[10px] font-semibold text-accent uppercase tracking-wide mb-1">Optional Intelligence Layer</p>
                <p className="text-xs text-muted-foreground">AI workers produce advisory outputs requiring human confirmation.</p>
                <div className="flex flex-wrap gap-1 mt-2">
                  {["Campaign planning", "Task templates", "Report drafting", "Analytics summaries"].map(m => (
                    <Badge key={m} variant="outline" className="text-[8px] border-accent/30">{m}</Badge>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* Core Modules */}
          <div>
            <h3 className="font-display font-semibold text-sm mb-3 px-1">Core Modules</h3>
            <div className="grid gap-2">
              {CORE_MODULES.filter(m => !search || m.name.toLowerCase().includes(search.toLowerCase()) || m.desc.toLowerCase().includes(search.toLowerCase())).map(mod => (
                <div key={mod.name} className="bg-card rounded-xl border border-border p-3 flex gap-3">
                  <mod.icon className="h-4 w-4 text-primary shrink-0 mt-0.5" />
                  <div>
                    <p className="text-sm font-medium">{mod.name}</p>
                    <p className="text-xs text-muted-foreground mt-0.5">{mod.desc}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </TabsContent>

        {/* ═══ GUIDES TAB ═══ */}
        <TabsContent value="guides" className="space-y-5">
          {/* Getting Started */}
          <div>
            <h3 className="font-display font-semibold text-sm mb-3 flex items-center gap-2">
              <Zap className="h-4 w-4 text-secondary" />
              Getting Started as a Contributor
            </h3>
            <div className="space-y-2">
              {GETTING_STARTED_STEPS.map((step, i) => (
                <div key={i} className="bg-card rounded-xl border border-border p-3 flex items-start gap-3">
                  <div className="h-6 w-6 rounded-full bg-primary/10 text-primary flex items-center justify-center text-xs font-bold shrink-0">{i + 1}</div>
                  <div>
                    <p className="text-sm font-medium flex items-center gap-1.5">
                      <step.icon className="h-3.5 w-3.5 text-muted-foreground" />
                      {step.title}
                    </p>
                    <p className="text-xs text-muted-foreground mt-0.5">{step.desc}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Task Lifecycle */}
          <div>
            <h3 className="font-display font-semibold text-sm mb-3 flex items-center gap-2">
              <Target className="h-4 w-4 text-accent" />
              Task Lifecycle
            </h3>
            <div className="bg-card rounded-xl border border-border p-4">
              <div className="flex flex-wrap gap-1.5 mb-3">
                {TASK_STATUSES.map((s, i) => (
                  <div key={s.status} className="flex items-center gap-1">
                    <Badge className={`text-[9px] font-mono ${s.color}`}>{s.status}</Badge>
                    {i < TASK_STATUSES.length - 1 && <ArrowRight className="h-2.5 w-2.5 text-muted-foreground/40" />}
                  </div>
                ))}
              </div>
              <div className="space-y-1.5">
                {TASK_STATUSES.map(s => (
                  <div key={s.status} className="flex gap-2 text-xs">
                    <code className="font-mono text-[10px] text-foreground w-28 shrink-0">{s.status}</code>
                    <span className="text-muted-foreground">{s.desc}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Roles & Permissions */}
          <div>
            <h3 className="font-display font-semibold text-sm mb-3 flex items-center gap-2">
              <Shield className="h-4 w-4 text-primary" />
              Roles & Permissions
            </h3>
            <div className="space-y-2">
              {Object.entries(ROLE_PERMISSIONS).map(([role, perms]) => (
                <div key={role} className="bg-card rounded-xl border border-border p-3">
                  <Badge variant="outline" className="text-[10px] font-mono mb-2 capitalize">{role}</Badge>
                  <ul className="space-y-1">
                    {perms.map((p, i) => (
                      <li key={i} className="flex items-start gap-1.5 text-xs text-muted-foreground">
                        <ChevronRight className="h-3 w-3 shrink-0 mt-0.5 text-primary/50" />
                        {p}
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>
          </div>

          {/* For Organization Admins */}
          <div>
            <h3 className="font-display font-semibold text-sm mb-3 flex items-center gap-2">
              <Settings className="h-4 w-4 text-secondary" />
              For Organization Admins
            </h3>
            <div className="bg-card rounded-xl border border-border p-4 space-y-3">
              <div>
                <p className="text-xs font-semibold text-foreground">Creating Initiatives</p>
                <p className="text-xs text-muted-foreground mt-0.5">Navigate to Initiatives → Create. Define title, description, dates, success metrics, and target metrics. Start as a micro-initiative for quick sprints, or go full-scale.</p>
              </div>
              <div>
                <p className="text-xs font-semibold text-foreground">Managing Tasks</p>
                <p className="text-xs text-muted-foreground mt-0.5">Use the task generator to bulk-create tasks from templates with variables (e.g., region names). Tasks can be organized into projects within an initiative.</p>
              </div>
              <div>
                <p className="text-xs font-semibold text-foreground">Validation Workflow</p>
                <p className="text-xs text-muted-foreground mt-0.5">Assign validator roles to trusted org members. Validators review submissions in the Validation Queue. Rejected submissions can be appealed by contributors.</p>
              </div>
              <div>
                <p className="text-xs font-semibold text-foreground">Issuing Credentials</p>
                <p className="text-xs text-muted-foreground mt-0.5">Issue verifiable credentials to volunteers via the Developer Portal or API. Credentials include type, title, description, and custom metadata entries.</p>
              </div>
            </div>
          </div>
        </TabsContent>

        {/* ═══ API TAB ═══ */}
        <TabsContent value="api" className="space-y-4">
          <div className="bg-primary/5 rounded-xl border border-primary/20 p-4">
            <p className="text-xs text-muted-foreground">
              Full API access is available at the <span className="font-medium text-foreground">Developer Portal</span> (/developers).
              Below is a reference summary.
            </p>
          </div>

          {API_SECTIONS.map((section, i) => (
            <div key={i} className="bg-card rounded-xl border border-border p-4">
              <h3 className="font-display font-semibold text-sm mb-3">{section.title}</h3>
              {section.content && (
                <div className="text-xs text-muted-foreground whitespace-pre-line leading-relaxed">
                  {section.content.split("\n").map((line, j) => {
                    if (line.startsWith("**") && line.endsWith("**")) {
                      return <p key={j} className="font-semibold text-foreground mt-2 mb-1">{line.replace(/\*\*/g, "")}</p>;
                    }
                    if (line.match(/^\*\*/)) {
                      const parts = line.split("**");
                      return <p key={j} className="mt-1"><span className="font-semibold text-foreground">{parts[1]}</span>{parts[2]}</p>;
                    }
                    return <p key={j}>{line}</p>;
                  })}
                </div>
              )}
              {section.endpoints && (
                <div className="space-y-1.5">
                  {section.endpoints.map((ep, j) => (
                    <div key={j} className="flex items-start gap-2 bg-muted/50 rounded-lg p-2">
                      <Badge className={`text-[9px] font-mono shrink-0 ${
                        ep.method === "GET" ? "bg-accent/10 text-accent border-accent/20" :
                        ep.method === "POST" ? "bg-primary/10 text-primary border-primary/20" :
                        ep.method === "DELETE" ? "bg-destructive/10 text-destructive border-destructive/20" :
                        "bg-secondary/10 text-secondary border-secondary/20"
                      }`}>{ep.method}</Badge>
                      <div>
                        <code className="text-[11px] font-mono text-foreground">{ep.path}</code>
                        <p className="text-[10px] text-muted-foreground">{ep.desc}</p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </TabsContent>

        {/* ═══ DATA MODEL TAB ═══ */}
        <TabsContent value="data" className="space-y-4">
          <div className="bg-card rounded-xl border border-border p-4">
            <h3 className="font-display font-semibold text-sm mb-1">Database Schema</h3>
            <p className="text-xs text-muted-foreground mb-4">
              {DB_TABLES.length} tables with Row-Level Security policies. All tables use UUID primary keys (v7 for ordering).
            </p>
            <div className="space-y-1.5">
              {DB_TABLES.filter(t => !search || t.name.includes(search.toLowerCase()) || t.desc.toLowerCase().includes(search.toLowerCase())).map(table => (
                <div key={table.name} className="flex items-start gap-2 bg-muted/50 rounded-lg p-2.5">
                  <code className="text-[11px] font-mono text-primary font-semibold w-36 shrink-0">{table.name}</code>
                  <p className="text-[10px] text-muted-foreground">{table.desc}</p>
                </div>
              ))}
            </div>
          </div>

          {/* Key relationships */}
          <div className="bg-card rounded-xl border border-border p-4">
            <h3 className="font-display font-semibold text-sm mb-3">Key Relationships</h3>
            <div className="space-y-2 text-xs text-muted-foreground">
              <p><code className="font-mono text-foreground">tasks</code> → belongs to <code className="font-mono text-foreground">initiatives</code>, <code className="font-mono text-foreground">organizations</code>, <code className="font-mono text-foreground">projects</code>, <code className="font-mono text-foreground">regions</code></p>
              <p><code className="font-mono text-foreground">task_submissions</code> → belongs to <code className="font-mono text-foreground">tasks</code>, references submitting user</p>
              <p><code className="font-mono text-foreground">validations</code> → belongs to <code className="font-mono text-foreground">task_submissions</code>, references validator</p>
              <p><code className="font-mono text-foreground">contributions</code> → belongs to <code className="font-mono text-foreground">tasks</code>, references <code className="font-mono text-foreground">validations</code></p>
              <p><code className="font-mono text-foreground">credentials</code> → belongs to user, optional <code className="font-mono text-foreground">initiatives</code> / <code className="font-mono text-foreground">organizations</code></p>
              <p><code className="font-mono text-foreground">webhook_subscriptions</code> → belongs to <code className="font-mono text-foreground">api_keys</code></p>
              <p><code className="font-mono text-foreground">events</code> → polymorphic entity references (entity_type + entity_id)</p>
            </div>
          </div>

          {/* Security model */}
          <div className="bg-card rounded-xl border border-border p-4">
            <h3 className="font-display font-semibold text-sm mb-3 flex items-center gap-2">
              <Shield className="h-4 w-4 text-primary" />
              Security Model
            </h3>
            <div className="space-y-2 text-xs text-muted-foreground">
              <p>• <span className="font-medium text-foreground">Row-Level Security</span> on all tables — users can only access data they're authorized for.</p>
              <p>• <span className="font-medium text-foreground">Roles stored separately</span> in <code className="font-mono">user_roles</code> table (never on profiles) to prevent privilege escalation.</p>
              <p>• <span className="font-medium text-foreground">Security-definer functions</span> (<code className="font-mono">has_role</code>, <code className="font-mono">is_org_member</code>) prevent recursive RLS checks.</p>
              <p>• <span className="font-medium text-foreground">Immutable tables</span>: contributions, audit_logs, events cannot be updated or deleted.</p>
              <p>• <span className="font-medium text-foreground">API key hashing</span>: Keys are SHA-256 hashed; only the prefix is stored in plaintext.</p>
              <p>• <span className="font-medium text-foreground">Webhook signatures</span>: HMAC-SHA256 for payload verification.</p>
            </div>
          </div>
        </TabsContent>

        {/* ═══ FAQ TAB ═══ */}
        <TabsContent value="faq" className="space-y-4">
          <Accordion type="single" collapsible className="space-y-2">
            {FAQ.filter(f => !search || f.q.toLowerCase().includes(search.toLowerCase()) || f.a.toLowerCase().includes(search.toLowerCase())).map((item, i) => (
              <AccordionItem key={i} value={`faq-${i}`} className="bg-card rounded-xl border border-border px-4">
                <AccordionTrigger className="text-sm font-medium py-3 hover:no-underline">
                  {item.q}
                </AccordionTrigger>
                <AccordionContent className="text-xs text-muted-foreground pb-3 leading-relaxed">
                  {item.a}
                </AccordionContent>
              </AccordionItem>
            ))}
          </Accordion>
        </TabsContent>
      </Tabs>
    </AppShell>
  );
}
