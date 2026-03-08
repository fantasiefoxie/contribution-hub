import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, Shield, Star, ChevronRight, Users, CheckCircle2, Clock, Loader2, Crown, Award, Eye, Gavel } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { toast } from "sonner";
import AppShell from "@/components/layout/AppShell";
import ProtectedRoute from "@/components/ProtectedRoute";
import { useMyProgression, useLeadershipOpportunities, useLeaderboard, useRequestPromotion } from "@/hooks/use-leadership";

const tierIcons: Record<string, typeof Shield> = {
  volunteer: Users,
  trusted: Star,
  validator: Eye,
  moderator: Gavel,
  admin: Crown,
};

const tierColors: Record<string, string> = {
  volunteer: "bg-muted text-muted-foreground",
  trusted: "bg-primary/10 text-primary",
  validator: "bg-accent/10 text-accent",
  moderator: "bg-secondary/10 text-secondary",
  admin: "bg-destructive/10 text-destructive",
};

// Mock data for when API isn't returning real data
const mockProgression = {
  current_tier: "volunteer",
  current_level: 0,
  stats: { validated_tasks: 7, approval_rate: 85, initiatives: 2, total_score: 580 },
  progression: [
    { key: "volunteer", label: "Volunteer", level: 0, current: true, achieved: true, eligible: false, progress: null },
    {
      key: "trusted", label: "Trusted Contributor", level: 1, current: false, achieved: false, eligible: false,
      criteria: { validated_tasks: 10, approval_rate: 90, initiatives: 1, description: "Complete 10+ validated tasks with ≥90% approval rate." },
      progress: {
        validated_tasks: { current: 7, required: 10, pct: 70 },
        approval_rate: { current: 85, required: 90, pct: 94 },
        initiatives: { current: 2, required: 1, pct: 100 },
        overall: 88,
      },
    },
    {
      key: "validator", label: "Validator", level: 2, current: false, achieved: false, eligible: false,
      criteria: { validated_tasks: 25, approval_rate: 95, initiatives: 3, description: "Complete 25+ validated tasks with ≥95% approval rate. Requires moderator recommendation." },
      progress: {
        validated_tasks: { current: 7, required: 25, pct: 28 },
        approval_rate: { current: 85, required: 95, pct: 89 },
        initiatives: { current: 2, required: 3, pct: 67 },
        overall: 61,
      },
    },
    {
      key: "moderator", label: "Moderator", level: 3, current: false, achieved: false, eligible: false,
      criteria: { validated_tasks: 50, approval_rate: 95, initiatives: 5, description: "Complete 50+ validated tasks. Requires admin confirmation." },
      progress: {
        validated_tasks: { current: 7, required: 50, pct: 14 },
        approval_rate: { current: 85, required: 95, pct: 89 },
        initiatives: { current: 2, required: 5, pct: 40 },
        overall: 48,
      },
    },
    {
      key: "admin", label: "Campaign Organizer", level: 4, current: false, achieved: false, eligible: false,
      criteria: { validated_tasks: 100, approval_rate: 95, initiatives: 10, description: "Complete 100+ validated tasks, 10+ initiatives." },
      progress: {
        validated_tasks: { current: 7, required: 100, pct: 7 },
        approval_rate: { current: 85, required: 95, pct: 89 },
        initiatives: { current: 2, required: 10, pct: 20 },
        overall: 39,
      },
    },
  ],
};

const mockOpportunities = {
  opportunities: [
    { type: "validator", initiative_id: "init-1", initiative_title: "River Cleanup Campaign", description: "Validator needed for River Cleanup Campaign", slots_available: 2 },
    { type: "moderator", initiative_id: "init-3", initiative_title: "Urban Reforestation Drive", description: "Moderator needed for Urban Reforestation Drive", slots_available: 1 },
    { type: "validator", initiative_id: "init-5", initiative_title: "Digital Literacy Program", description: "Validator needed for Digital Literacy Program", slots_available: 3 },
  ],
};

const mockLeaderboard = {
  leaders: [
    { user_id: "u-1", display_name: "Aisha Khan", tier: "moderator", tier_label: "Moderator", total_score: 2450, task_count: 42 },
    { user_id: "u-2", display_name: "Rahul Sharma", tier: "validator", tier_label: "Validator", total_score: 1890, task_count: 31 },
    { user_id: "u-3", display_name: "Carlos Ruiz", tier: "trusted", tier_label: "Trusted Contributor", total_score: 1560, task_count: 24 },
    { user_id: "u-4", display_name: "Priya Patel", tier: "trusted", tier_label: "Trusted Contributor", total_score: 1240, task_count: 19 },
    { user_id: "u-5", display_name: "James Okonjo", tier: "volunteer", tier_label: "Volunteer", total_score: 890, task_count: 14 },
  ],
};

type Tab = "progression" | "opportunities" | "leaderboard";

export default function LeadershipDashboard() {
  const navigate = useNavigate();
  const [tab, setTab] = useState<Tab>("progression");
  const { data: progData, isLoading: loadingProg } = useMyProgression();
  const { data: oppsData, isLoading: loadingOpps } = useLeadershipOpportunities();
  const { data: boardData, isLoading: loadingBoard } = useLeaderboard();
  const requestMutation = useRequestPromotion();

  const prog = progData?.progression?.length > 0 ? progData : mockProgression;
  const opps = oppsData?.opportunities?.length > 0 ? oppsData : mockOpportunities;
  const board = boardData?.leaders?.length > 0 ? boardData : mockLeaderboard;

  const handleRequestPromotion = async (role: string) => {
    try {
      const result = await requestMutation.mutateAsync(role);
      if (result.promoted) {
        toast.success(`You've been promoted to ${role === "trusted" ? "Trusted Contributor" : role}!`);
      } else {
        toast.success("Promotion request submitted for review!");
      }
    } catch (e: any) {
      toast.error(e.message || "Failed to request promotion");
    }
  };

  return (
    <AppShell>
      {/* Header */}
      <div className="sticky top-0 z-10 bg-background/95 backdrop-blur-sm border-b border-border">
        <div className="flex items-center gap-3 px-4 h-14">
          <button onClick={() => navigate("/profile")} className="h-8 w-8 rounded-full bg-muted flex items-center justify-center">
            <ArrowLeft className="h-4 w-4" />
          </button>
          <span className="font-display font-semibold text-sm">Leadership</span>
        </div>
      </div>

      <ProtectedRoute>
      <div className="px-4 pt-5 pb-32">
        {/* Current Status */}
        <div className="bg-gradient-to-br from-primary/5 to-secondary/5 rounded-2xl border border-primary/10 p-5 mb-5">
          <div className="flex items-center gap-3 mb-3">
            {(() => {
              const Icon = tierIcons[prog.current_tier] || Shield;
              return (
                <div className="h-12 w-12 rounded-xl bg-primary/10 flex items-center justify-center">
                  <Icon className="h-6 w-6 text-primary" />
                </div>
              );
            })()}
            <div>
              <p className="text-[10px] text-muted-foreground uppercase tracking-wide font-semibold">Current Role</p>
              <p className="font-display font-bold text-lg capitalize">
                {prog.progression?.find((t: any) => t.current)?.label || "Volunteer"}
              </p>
            </div>
          </div>
          <div className="grid grid-cols-3 gap-2">
            <div className="bg-card/80 rounded-lg p-2 text-center">
              <p className="font-display font-bold text-sm text-primary">{prog.stats.validated_tasks}</p>
              <p className="text-[9px] text-muted-foreground">Tasks</p>
            </div>
            <div className="bg-card/80 rounded-lg p-2 text-center">
              <p className="font-display font-bold text-sm text-secondary">{prog.stats.approval_rate}%</p>
              <p className="text-[9px] text-muted-foreground">Approval</p>
            </div>
            <div className="bg-card/80 rounded-lg p-2 text-center">
              <p className="font-display font-bold text-sm text-accent">{prog.stats.total_score}</p>
              <p className="text-[9px] text-muted-foreground">Score</p>
            </div>
          </div>
        </div>

        {/* Tabs */}
        <div className="flex border-b border-border mb-4">
          {(["progression", "opportunities", "leaderboard"] as Tab[]).map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`flex-1 py-2.5 text-xs font-semibold capitalize transition-colors ${
                tab === t ? "text-primary border-b-2 border-primary" : "text-muted-foreground hover:text-foreground"
              }`}
            >
              {t}
            </button>
          ))}
        </div>

        {/* Progression Tab */}
        {tab === "progression" && (
          <div className="space-y-3">
            {loadingProg ? (
              <div className="flex justify-center py-12"><Loader2 className="h-6 w-6 animate-spin text-primary" /></div>
            ) : (
              (prog.progression ?? []).map((tier: any) => {
                const Icon = tierIcons[tier.key] || Shield;
                const isNext = !tier.achieved && !tier.current && tier.level === (prog.current_level ?? 0) + 1;

                return (
                  <div
                    key={tier.key}
                    className={`rounded-2xl border p-4 transition-colors ${
                      tier.current ? "border-primary bg-primary/5" :
                      tier.achieved ? "border-border bg-card" :
                      isNext ? "border-accent/30 bg-accent/5" :
                      "border-border/50 bg-muted/30"
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <div className={`h-10 w-10 rounded-xl flex items-center justify-center ${
                        tier.current || tier.achieved ? "bg-primary/10" : "bg-muted"
                      }`}>
                        {tier.achieved && !tier.current ? (
                          <CheckCircle2 className="h-5 w-5 text-primary" />
                        ) : (
                          <Icon className={`h-5 w-5 ${tier.current ? "text-primary" : "text-muted-foreground"}`} />
                        )}
                      </div>
                      <div className="flex-1">
                        <div className="flex items-center gap-2">
                          <p className="font-display font-semibold text-sm">{tier.label}</p>
                          {tier.current && <Badge className="bg-primary/10 text-primary border-0 text-[10px]">Current</Badge>}
                          {tier.achieved && !tier.current && <Badge className="bg-muted text-muted-foreground border-0 text-[10px]">Achieved</Badge>}
                        </div>
                        {tier.criteria && (
                          <p className="text-[10px] text-muted-foreground mt-0.5 leading-relaxed">{tier.criteria.description}</p>
                        )}
                      </div>
                    </div>

                    {/* Progress bars for unachieved tiers */}
                    {tier.progress && !tier.achieved && (
                      <div className="mt-3 space-y-2">
                        <div>
                          <div className="flex justify-between text-[10px] text-muted-foreground mb-0.5">
                            <span>Validated Tasks</span>
                            <span>{tier.progress.validated_tasks.current} / {tier.progress.validated_tasks.required}</span>
                          </div>
                          <Progress value={tier.progress.validated_tasks.pct} className="h-1.5" />
                        </div>
                        <div>
                          <div className="flex justify-between text-[10px] text-muted-foreground mb-0.5">
                            <span>Approval Rate</span>
                            <span>{tier.progress.approval_rate.current}% / {tier.progress.approval_rate.required}%</span>
                          </div>
                          <Progress value={tier.progress.approval_rate.pct} className="h-1.5" />
                        </div>
                        <div>
                          <div className="flex justify-between text-[10px] text-muted-foreground mb-0.5">
                            <span>Initiatives</span>
                            <span>{tier.progress.initiatives.current} / {tier.progress.initiatives.required}</span>
                          </div>
                          <Progress value={tier.progress.initiatives.pct} className="h-1.5" />
                        </div>

                        {tier.eligible && (
                          <Button
                            size="sm"
                            onClick={() => handleRequestPromotion(tier.key)}
                            disabled={requestMutation.isPending}
                            className="w-full mt-2 rounded-xl text-xs font-semibold"
                          >
                            {requestMutation.isPending ? (
                              <Loader2 className="h-3 w-3 animate-spin" />
                            ) : tier.key === "trusted" ? (
                              <>
                                <Star className="h-3.5 w-3.5 mr-1" /> Claim Trusted Status
                              </>
                            ) : (
                              <>
                                <Award className="h-3.5 w-3.5 mr-1" /> Request Promotion
                              </>
                            )}
                          </Button>
                        )}
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </div>
        )}

        {/* Opportunities Tab */}
        {tab === "opportunities" && (
          <div className="space-y-3">
            {loadingOpps ? (
              <div className="flex justify-center py-12"><Loader2 className="h-6 w-6 animate-spin text-primary" /></div>
            ) : (opps.opportunities ?? []).length === 0 ? (
              <div className="text-center py-12">
                <p className="text-3xl mb-2">🏔️</p>
                <p className="text-xs text-muted-foreground">No leadership opportunities right now. Keep contributing!</p>
              </div>
            ) : (
              (opps.opportunities ?? []).map((opp: any, i: number) => {
                const Icon = tierIcons[opp.type] || Shield;
                return (
                  <button
                    key={i}
                    onClick={() => navigate(`/initiatives/${opp.initiative_id}`)}
                    className="w-full bg-card rounded-2xl border border-border p-4 text-left hover:border-primary/20 transition-colors"
                  >
                    <div className="flex items-start gap-3">
                      <div className={`h-10 w-10 rounded-xl flex items-center justify-center shrink-0 ${tierColors[opp.type] || "bg-muted"}`}>
                        <Icon className="h-5 w-5" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="font-display font-semibold text-sm">{opp.description}</p>
                        <p className="text-[10px] text-muted-foreground mt-0.5">{opp.initiative_title}</p>
                        <div className="flex items-center gap-2 mt-1.5">
                          <Badge className={`border-0 text-[10px] capitalize ${tierColors[opp.type]}`}>
                            {opp.type}
                          </Badge>
                          <span className="text-[10px] text-muted-foreground">{opp.slots_available} slot{opp.slots_available !== 1 ? "s" : ""} available</span>
                        </div>
                      </div>
                      <ChevronRight className="h-4 w-4 text-muted-foreground shrink-0 mt-1" />
                    </div>
                  </button>
                );
              })
            )}
          </div>
        )}

        {/* Leaderboard Tab */}
        {tab === "leaderboard" && (
          <div className="space-y-2">
            {loadingBoard ? (
              <div className="flex justify-center py-12"><Loader2 className="h-6 w-6 animate-spin text-primary" /></div>
            ) : (board.leaders ?? []).length === 0 ? (
              <div className="text-center py-12">
                <p className="text-3xl mb-2">🏆</p>
                <p className="text-xs text-muted-foreground">No leaders yet. Be the first!</p>
              </div>
            ) : (
              (board.leaders ?? []).map((leader: any, i: number) => {
                const Icon = tierIcons[leader.tier] || Shield;
                const medals = ["🥇", "🥈", "🥉"];
                return (
                  <div key={leader.user_id} className="flex items-center gap-3 bg-card rounded-xl border border-border p-3">
                    <div className="w-6 text-center shrink-0">
                      {i < 3 ? (
                        <span className="text-lg">{medals[i]}</span>
                      ) : (
                        <span className="text-xs font-bold text-muted-foreground">{i + 1}</span>
                      )}
                    </div>
                    <div className="h-9 w-9 rounded-full bg-primary/10 flex items-center justify-center shrink-0">
                      <Icon className="h-4 w-4 text-primary" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-sm truncate">{leader.display_name}</p>
                      <div className="flex items-center gap-1.5 mt-0.5">
                        <Badge className={`border-0 text-[9px] ${tierColors[leader.tier]}`}>
                          {leader.tier_label}
                        </Badge>
                        <span className="text-[10px] text-muted-foreground">{leader.task_count} tasks</span>
                      </div>
                    </div>
                    <p className="font-display font-bold text-sm text-primary shrink-0">{leader.total_score.toLocaleString()}</p>
                  </div>
                );
              })
            )}
          </div>
        )}
      </div>}

    </AppShell>
  );
}
