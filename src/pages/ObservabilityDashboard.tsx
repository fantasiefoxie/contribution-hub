import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, Activity, Users, CheckCircle2, AlertTriangle, Shield, Eye, Radio, Loader2, Award, TrendingUp, UserPlus, Brain, Sparkles } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import AppShell from "@/components/layout/AppShell";
import { usePlatformHealth, useValidatorMonitoring, useFraudSignals, useEventStream, useAiMetrics } from "@/hooks/use-observability";

// ── Mock data ──
const mockHealth = {
  total_volunteers: 342, active_volunteers_today: 47, total_tasks: 891,
  tasks_completed_today: 23, total_initiatives: 12, active_initiatives: 8,
  validation_backlog: 15,
  credentials_issued_today: 8, roles_granted_today: 3, initiative_joins_today: 14,
  event_breakdown_today: {
    TaskSubmitted: 31, ValidationApproved: 23, TaskClaimed: 18, ContributionRecorded: 23,
    CredentialIssued: 8, RoleGranted: 3, InitiativeJoined: 14, InitiativePriorityUpdated: 2,
  },
};

const mockValidators = {
  validators: [
    { validator_id: "v-1", display_name: "Sarah Chen", total_reviews: 89, approval_rate: 78, overturn_rate: 5, avg_review_hours: 2, flagged: false, flag_reasons: [] },
    { validator_id: "v-2", display_name: "David Kumar", total_reviews: 54, approval_rate: 99, overturn_rate: 22, avg_review_hours: 1, flagged: true, flag_reasons: ["rubber-stamping (>98% approval)", "high overturn rate (>20%)"] },
    { validator_id: "v-3", display_name: "Maria Okonjo", total_reviews: 67, approval_rate: 82, overturn_rate: 3, avg_review_hours: 4, flagged: false, flag_reasons: [] },
  ],
};

const mockFraud = {
  alerts: [
    { type: "repeated_pairing", severity: "high", description: "Validator and contributor paired 12 times", details: {} },
    { type: "fast_completion", severity: "medium", description: "Task completed in 45s", details: {} },
    { type: "low_evidence", severity: "low", description: "Submission with minimal evidence", details: {} },
  ],
  total_alerts: 3,
};

const mockEvents = {
  events: [
    { id: "e-1", type: "CredentialIssued", entity_type: "credential", actor_name: "System", created_at: new Date(Date.now() - 30000).toISOString() },
    { id: "e-2", type: "RoleGranted", entity_type: "user", actor_name: "Admin", created_at: new Date(Date.now() - 60000).toISOString() },
    { id: "e-3", type: "InitiativeJoined", entity_type: "initiative", actor_name: "Rahul S.", created_at: new Date(Date.now() - 90000).toISOString() },
    { id: "e-4", type: "TaskSubmitted", entity_type: "task", actor_name: "Rahul S.", created_at: new Date(Date.now() - 120000).toISOString() },
    { id: "e-5", type: "ValidationApproved", entity_type: "validation", actor_name: "Sarah C.", created_at: new Date(Date.now() - 180000).toISOString() },
    { id: "e-6", type: "ContributionRecorded", entity_type: "contribution", actor_name: "System", created_at: new Date(Date.now() - 240000).toISOString() },
    { id: "e-7", type: "InitiativePriorityUpdated", entity_type: "initiative", actor_name: "Admin", created_at: new Date(Date.now() - 300000).toISOString() },
    { id: "e-8", type: "TaskClaimed", entity_type: "task", actor_name: "Priya P.", created_at: new Date(Date.now() - 400000).toISOString() },
  ],
};

const eventTypeColors: Record<string, string> = {
  TaskSubmitted: "bg-accent/10 text-accent",
  ValidationApproved: "bg-primary/10 text-primary",
  ValidationRejected: "bg-destructive/10 text-destructive",
  ContributionRecorded: "bg-secondary/10 text-secondary",
  TaskClaimed: "bg-muted text-muted-foreground",
  TaskCreated: "bg-muted text-muted-foreground",
  BadgeAwarded: "bg-secondary/10 text-secondary",
  CredentialIssued: "bg-primary/10 text-primary",
  CredentialRevoked: "bg-destructive/10 text-destructive",
  RoleGranted: "bg-accent/10 text-accent",
  RoleRevoked: "bg-destructive/10 text-destructive",
  InitiativeJoined: "bg-secondary/10 text-secondary",
  InitiativePriorityUpdated: "bg-muted text-muted-foreground",
  InitiativeCreated: "bg-accent/10 text-accent",
};

const severityColors: Record<string, string> = {
  high: "bg-destructive/10 text-destructive",
  medium: "bg-secondary/10 text-secondary",
  low: "bg-muted text-muted-foreground",
};

function timeAgo(dateStr: string) {
  const diff = Date.now() - new Date(dateStr).getTime();
  const secs = Math.floor(diff / 1000);
  if (secs < 60) return `${secs}s ago`;
  const mins = Math.floor(secs / 60);
  if (mins < 60) return `${mins}m ago`;
  return `${Math.floor(mins / 60)}h ago`;
}

type Tab = "health" | "validators" | "fraud" | "events" | "ai";

export default function ObservabilityDashboard() {
  const navigate = useNavigate();
  const [tab, setTab] = useState<Tab>("health");
  const { data: healthData, isLoading: loadingHealth } = usePlatformHealth();
  const { data: valData, isLoading: loadingVal } = useValidatorMonitoring();
  const { data: fraudData, isLoading: loadingFraud } = useFraudSignals();
  const { data: eventData, isLoading: loadingEvents } = useEventStream(30);
  const { data: aiData, isLoading: loadingAi } = useAiMetrics();

  const health = healthData?.total_volunteers ? healthData : mockHealth;
  const validators = valData?.validators?.length > 0 ? valData : mockValidators;
  const fraud = fraudData?.alerts?.length >= 0 ? fraudData : mockFraud;
  const events = eventData?.events?.length > 0 ? eventData : mockEvents;

  return (
    <AppShell>
      <div className="sticky top-0 z-10 bg-background/95 backdrop-blur-sm border-b border-border">
        <div className="flex items-center gap-3 px-4 h-14">
          <button onClick={() => navigate("/profile")} className="h-8 w-8 rounded-full bg-muted flex items-center justify-center">
            <ArrowLeft className="h-4 w-4" />
          </button>
          <span className="font-display font-semibold text-sm">Platform Observability</span>
          <div className="ml-auto flex items-center gap-1.5">
            <div className="h-2 w-2 rounded-full bg-primary animate-pulse" />
            <span className="text-[10px] text-muted-foreground">Live</span>
          </div>
        </div>
      </div>

      <div className="px-4 pt-5 pb-32">
        {/* Tabs */}
        <div className="flex border-b border-border mb-4 overflow-x-auto">
          {([
            { key: "health" as Tab, label: "Health", icon: Activity },
            { key: "validators" as Tab, label: "Validators", icon: Shield },
            { key: "fraud" as Tab, label: "Fraud", icon: AlertTriangle },
            { key: "events" as Tab, label: "Events", icon: Radio },
            { key: "ai" as Tab, label: "AI Layer", icon: Brain },
          ]).map(({ key, label, icon: Icon }) => (
            <button
              key={key}
              onClick={() => setTab(key)}
              className={`flex-1 py-2.5 text-[11px] font-semibold flex items-center justify-center gap-1 transition-colors whitespace-nowrap ${
                tab === key ? "text-primary border-b-2 border-primary" : "text-muted-foreground hover:text-foreground"
              }`}
            >
              <Icon className="h-3.5 w-3.5" /> {label}
            </button>
          ))}
        </div>

        {/* Health Tab */}
        {tab === "health" && (
          <div className="space-y-4">
            {loadingHealth ? (
              <div className="flex justify-center py-12"><Loader2 className="h-6 w-6 animate-spin text-primary" /></div>
            ) : (
              <>
                {/* Key stats */}
                <div className="grid grid-cols-2 gap-2">
                  {[
                    { label: "Active Volunteers Today", value: health.active_volunteers_today, icon: Users, color: "text-primary" },
                    { label: "Tasks Completed Today", value: health.tasks_completed_today, icon: CheckCircle2, color: "text-secondary" },
                    { label: "Active Initiatives", value: health.active_initiatives, icon: Activity, color: "text-accent" },
                    { label: "Validation Backlog", value: health.validation_backlog, icon: Eye, color: health.validation_backlog > 20 ? "text-destructive" : "text-muted-foreground" },
                  ].map(({ label, value, icon: Icon, color }) => (
                    <div key={label} className="bg-card rounded-xl border border-border p-3">
                      <Icon className={`h-4 w-4 ${color} mb-1`} />
                      <p className="font-display font-bold text-xl">{value}</p>
                      <p className="text-[10px] text-muted-foreground">{label}</p>
                    </div>
                  ))}
                </div>

                {/* New consolidated metrics */}
                <div className="bg-card rounded-2xl border border-border p-4">
                  <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-3">Consolidated Metrics</p>
                  <div className="grid grid-cols-3 gap-3 text-center">
                    <div>
                      <Award className="h-4 w-4 text-primary mx-auto mb-1" />
                      <p className="font-display font-bold text-lg text-primary">{health.credentials_issued_today ?? 0}</p>
                      <p className="text-[10px] text-muted-foreground">Credentials Today</p>
                    </div>
                    <div>
                      <TrendingUp className="h-4 w-4 text-accent mx-auto mb-1" />
                      <p className="font-display font-bold text-lg text-accent">{health.roles_granted_today ?? 0}</p>
                      <p className="text-[10px] text-muted-foreground">Promotions Today</p>
                    </div>
                    <div>
                      <UserPlus className="h-4 w-4 text-secondary mx-auto mb-1" />
                      <p className="font-display font-bold text-lg text-secondary">{health.initiative_joins_today ?? 0}</p>
                      <p className="text-[10px] text-muted-foreground">Joins Today</p>
                    </div>
                  </div>
                </div>

                {/* Platform totals */}
                <div className="bg-card rounded-2xl border border-border p-4">
                  <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-3">Platform Totals</p>
                  <div className="grid grid-cols-3 gap-3 text-center">
                    <div>
                      <p className="font-display font-bold text-lg text-primary">{health.total_volunteers}</p>
                      <p className="text-[10px] text-muted-foreground">Volunteers</p>
                    </div>
                    <div>
                      <p className="font-display font-bold text-lg text-secondary">{health.total_tasks}</p>
                      <p className="text-[10px] text-muted-foreground">Tasks</p>
                    </div>
                    <div>
                      <p className="font-display font-bold text-lg text-accent">{health.total_initiatives}</p>
                      <p className="text-[10px] text-muted-foreground">Initiatives</p>
                    </div>
                  </div>
                </div>

                {/* Today's events breakdown */}
                {health.event_breakdown_today && Object.keys(health.event_breakdown_today).length > 0 && (
                  <div className="bg-card rounded-2xl border border-border p-4">
                    <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-3">Today's Activity</p>
                    <div className="space-y-2">
                      {Object.entries(health.event_breakdown_today as Record<string, number>)
                        .sort(([, a], [, b]) => b - a)
                        .map(([type, count]) => (
                          <div key={type} className="flex items-center justify-between">
                            <Badge className={`border-0 text-[10px] ${eventTypeColors[type] || "bg-muted text-muted-foreground"}`}>
                              {type}
                            </Badge>
                            <span className="font-display font-bold text-sm">{count}</span>
                          </div>
                        ))}
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
        )}

        {/* Validators Tab */}
        {tab === "validators" && (
          <div className="space-y-3">
            {loadingVal ? (
              <div className="flex justify-center py-12"><Loader2 className="h-6 w-6 animate-spin text-primary" /></div>
            ) : (validators.validators ?? []).length === 0 ? (
              <div className="text-center py-12">
                <p className="text-3xl mb-2">🛡️</p>
                <p className="text-xs text-muted-foreground">No validator activity yet.</p>
              </div>
            ) : (
              (validators.validators ?? []).map((v: any) => (
                <div key={v.validator_id} className={`bg-card rounded-2xl border p-4 ${v.flagged ? "border-destructive/30" : "border-border"}`}>
                  <div className="flex items-center gap-3 mb-3">
                    <div className={`h-10 w-10 rounded-full flex items-center justify-center ${v.flagged ? "bg-destructive/10" : "bg-primary/10"}`}>
                      <Shield className={`h-5 w-5 ${v.flagged ? "text-destructive" : "text-primary"}`} />
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <p className="font-display font-semibold text-sm">{v.display_name}</p>
                        {v.flagged && <Badge className="bg-destructive/10 text-destructive border-0 text-[10px]">⚠ Flagged</Badge>}
                      </div>
                      <p className="text-[10px] text-muted-foreground">{v.total_reviews} reviews</p>
                    </div>
                  </div>

                  <div className="grid grid-cols-3 gap-2 mb-2">
                    <div className="text-center">
                      <p className="font-display font-bold text-sm">{v.approval_rate}%</p>
                      <p className="text-[9px] text-muted-foreground">Approval</p>
                    </div>
                    <div className="text-center">
                      <p className="font-display font-bold text-sm">{v.overturn_rate}%</p>
                      <p className="text-[9px] text-muted-foreground">Overturn</p>
                    </div>
                    <div className="text-center">
                      <p className="font-display font-bold text-sm">{v.avg_review_hours}h</p>
                      <p className="text-[9px] text-muted-foreground">Avg Speed</p>
                    </div>
                  </div>

                  <Progress value={v.approval_rate} className="h-1.5" />

                  {v.flagged && v.flag_reasons.length > 0 && (
                    <div className="mt-2 space-y-1">
                      {v.flag_reasons.map((reason: string, i: number) => (
                        <p key={i} className="text-[10px] text-destructive flex items-center gap-1">
                          <AlertTriangle className="h-3 w-3 shrink-0" /> {reason}
                        </p>
                      ))}
                    </div>
                  )}
                </div>
              ))
            )}
          </div>
        )}

        {/* Fraud Tab */}
        {tab === "fraud" && (
          <div className="space-y-3">
            {loadingFraud ? (
              <div className="flex justify-center py-12"><Loader2 className="h-6 w-6 animate-spin text-primary" /></div>
            ) : (fraud.alerts ?? []).length === 0 ? (
              <div className="text-center py-12">
                <p className="text-3xl mb-2">✅</p>
                <p className="text-xs text-muted-foreground">No fraud signals detected. Platform looks clean!</p>
              </div>
            ) : (
              <>
                <div className="bg-card rounded-xl border border-border p-3 flex items-center justify-between">
                  <span className="text-xs text-muted-foreground">{fraud.total_alerts} total alert{fraud.total_alerts !== 1 ? "s" : ""}</span>
                  <div className="flex gap-1.5">
                    {["high", "medium", "low"].map((sev) => {
                      const count = (fraud.alerts ?? []).filter((a: any) => a.severity === sev).length;
                      return count > 0 ? (
                        <Badge key={sev} className={`border-0 text-[10px] ${severityColors[sev]}`}>
                          {count} {sev}
                        </Badge>
                      ) : null;
                    })}
                  </div>
                </div>

                {(fraud.alerts ?? []).map((alert: any, i: number) => (
                  <div key={i} className={`bg-card rounded-xl border p-3 ${
                    alert.severity === "high" ? "border-destructive/30" : "border-border"
                  }`}>
                    <div className="flex items-start gap-3">
                      <div className={`h-8 w-8 rounded-lg flex items-center justify-center shrink-0 ${severityColors[alert.severity]}`}>
                        <AlertTriangle className="h-4 w-4" />
                      </div>
                      <div className="flex-1">
                        <div className="flex items-center gap-2">
                          <Badge className={`border-0 text-[10px] capitalize ${severityColors[alert.severity]}`}>
                            {alert.severity}
                          </Badge>
                          <Badge className="bg-muted text-muted-foreground border-0 text-[10px]">
                            {alert.type.replace(/_/g, " ")}
                          </Badge>
                        </div>
                        <p className="text-xs mt-1">{alert.description}</p>
                      </div>
                    </div>
                  </div>
                ))}
              </>
            )}
          </div>
        )}

        {/* Events Tab */}
        {tab === "events" && (
          <div className="space-y-1.5">
            {loadingEvents ? (
              <div className="flex justify-center py-12"><Loader2 className="h-6 w-6 animate-spin text-primary" /></div>
            ) : (events.events ?? []).length === 0 ? (
              <div className="text-center py-12">
                <p className="text-3xl mb-2">📡</p>
                <p className="text-xs text-muted-foreground">No events yet.</p>
              </div>
            ) : (
              (events.events ?? []).map((event: any) => (
                <div key={event.id} className="flex items-center gap-3 bg-card/60 rounded-lg p-2.5 border border-border/50">
                  <div className="h-2 w-2 rounded-full bg-primary shrink-0" />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-1.5">
                      <Badge className={`border-0 text-[9px] px-1.5 ${eventTypeColors[event.type] || "bg-muted text-muted-foreground"}`}>
                        {event.type}
                      </Badge>
                      <span className="text-[10px] text-muted-foreground truncate">{event.actor_name}</span>
                    </div>
                  </div>
                  <span className="text-[10px] text-muted-foreground shrink-0">{timeAgo(event.created_at)}</span>
                </div>
              ))
            )}
          </div>
        )}

        {/* AI Layer Tab */}
        {tab === "ai" && (
          <div className="space-y-4">
            {loadingAi ? (
              <div className="flex justify-center py-12"><Loader2 className="h-6 w-6 animate-spin text-primary" /></div>
            ) : (
              <>
                {/* Architecture Rule Banner */}
                <div className="bg-primary/5 rounded-2xl border border-primary/20 p-4">
                  <div className="flex items-center gap-2 mb-2">
                    <Shield className="h-4 w-4 text-primary" />
                    <p className="text-xs font-semibold text-primary">Zero-AI Baseline Architecture</p>
                  </div>
                  <p className="text-[11px] text-muted-foreground leading-relaxed">
                    All core operations (task creation, claims, validation, contribution recording, governance) run on deterministic backend logic.
                    AI exists only in the <span className="font-medium text-foreground">Optional Intelligence Layer</span> — outputs are advisory and require human confirmation.
                  </p>
                </div>

                {/* AI Usage Stats */}
                <div className="grid grid-cols-2 gap-2">
                  {[
                    { label: "Total Suggestions", value: aiData?.total_suggestions ?? 0, icon: Sparkles, color: "text-accent" },
                    { label: "Acceptance Rate", value: `${aiData?.acceptance_rate ?? 0}%`, icon: CheckCircle2, color: "text-secondary" },
                    { label: "AI Requests Today", value: aiData?.ai_requests_today ?? 0, icon: Brain, color: "text-primary" },
                    { label: "Est. Cost Today", value: `$${(aiData?.estimated_cost_today_usd ?? 0).toFixed(4)}`, icon: Activity, color: "text-muted-foreground" },
                  ].map(({ label, value, icon: Icon, color }) => (
                    <div key={label} className="bg-card rounded-xl border border-border p-3">
                      <Icon className={`h-4 w-4 ${color} mb-1`} />
                      <p className="font-display font-bold text-xl">{value}</p>
                      <p className="text-[10px] text-muted-foreground">{label}</p>
                    </div>
                  ))}
                </div>

                {/* Suggestion Status Breakdown */}
                <div className="bg-card rounded-2xl border border-border p-4">
                  <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-3">Suggestion Pipeline</p>
                  <div className="grid grid-cols-3 gap-3 text-center">
                    <div>
                      <p className="font-display font-bold text-lg text-secondary">{aiData?.accepted ?? 0}</p>
                      <p className="text-[10px] text-muted-foreground">Accepted</p>
                    </div>
                    <div>
                      <p className="font-display font-bold text-lg text-destructive">{aiData?.rejected ?? 0}</p>
                      <p className="text-[10px] text-muted-foreground">Rejected</p>
                    </div>
                    <div>
                      <p className="font-display font-bold text-lg text-accent">{aiData?.pending ?? 0}</p>
                      <p className="text-[10px] text-muted-foreground">Pending</p>
                    </div>
                  </div>
                </div>

                {/* Usage by Model */}
                {aiData?.usage_by_model && Object.keys(aiData.usage_by_model).length > 0 && (
                  <div className="bg-card rounded-2xl border border-border p-4">
                    <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-3">Usage by Model</p>
                    <div className="space-y-2">
                      {Object.entries(aiData.usage_by_model as Record<string, any>).map(([model, stats]: [string, any]) => (
                        <div key={model} className="flex items-center justify-between bg-muted/50 rounded-lg p-2">
                          <div>
                            <p className="text-xs font-medium">{model}</p>
                            <p className="text-[10px] text-muted-foreground">{stats.tokens} tokens · {stats.avgLatency}ms avg</p>
                          </div>
                          <div className="text-right">
                            <p className="text-xs font-bold">{stats.requests} req</p>
                            <p className="text-[10px] text-muted-foreground">${stats.cost.toFixed(4)}</p>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Recent Suggestions */}
                {(aiData?.recent_suggestions ?? []).length > 0 && (
                  <div className="bg-card rounded-2xl border border-border p-4">
                    <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-3">Recent Suggestions</p>
                    <div className="space-y-1.5">
                      {(aiData?.recent_suggestions ?? []).map((s: any) => (
                        <div key={s.id} className="flex items-center gap-2 bg-muted/50 rounded-lg p-2">
                          <Badge className={`border-0 text-[9px] ${
                            s.status === "accepted" ? "bg-secondary/10 text-secondary" :
                            s.status === "rejected" ? "bg-destructive/10 text-destructive" :
                            "bg-accent/10 text-accent"
                          }`}>{s.status}</Badge>
                          <span className="text-[11px] flex-1 truncate">{s.suggestion_type}</span>
                          {s.confidence_score != null && (
                            <span className="text-[10px] text-muted-foreground">{Math.round(s.confidence_score * 100)}%</span>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Isolation Rules */}
                <div className="bg-card rounded-2xl border border-border p-4">
                  <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-3">AI Isolation Rules</p>
                  <div className="space-y-2 text-[11px]">
                    {[
                      "AI cannot write directly to core tables (tasks, contributions, validations)",
                      "All AI outputs marked with generated_by: 'ai' and confidence_score",
                      "AI workers read from event stream only — no direct DB mutations",
                      "Human confirmation required before suggestions become real data",
                      "Core modules operate independently with zero AI dependencies",
                    ].map((rule, i) => (
                      <div key={i} className="flex items-start gap-2">
                        <CheckCircle2 className="h-3.5 w-3.5 text-primary shrink-0 mt-0.5" />
                        <span className="text-muted-foreground">{rule}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </>
            )}
          </div>
        )}
      </div>
    </AppShell>
  );
}
