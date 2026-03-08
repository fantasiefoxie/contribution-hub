import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Users, Target, MapPin, Loader2, Plus, BarChart3, Globe, UsersRound, Zap, Clock } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { toast } from "sonner";
import AppShell from "@/components/layout/AppShell";
import { useCampaignDashboard, useCreateRegion } from "@/hooks/use-campaign-ops";

// Mock data for when API returns empty
const mockDashboard = {
  initiative: {
    id: "init-1",
    title: "River Cleanup Campaign",
    description: "City-wide river cleanup initiative targeting 50 pollution hotspots across 15 wards.",
    status: "active",
  },
  active_volunteers: 127,
  total_tasks: 248,
  completed_today: 14,
  status_counts: { open: 89, claimed: 23, in_progress: 45, validated: 72, submitted: 19 },
  region_stats: [
    { id: "r1", name: "North Zone", total_tasks: 68, completed_tasks: 22, active_tasks: 15, open_tasks: 31, progress_pct: 32 },
    { id: "r2", name: "South Zone", total_tasks: 72, completed_tasks: 28, active_tasks: 12, open_tasks: 32, progress_pct: 39 },
    { id: "r3", name: "East Zone", total_tasks: 55, completed_tasks: 12, active_tasks: 10, open_tasks: 33, progress_pct: 22 },
    { id: "r4", name: "West Zone", total_tasks: 53, completed_tasks: 10, active_tasks: 8, open_tasks: 35, progress_pct: 19 },
  ],
  pool_counts: { regional: 156, team: 42, public: 38, online: 12 },
  team_pods: [
    { task_id: "t1", member_count: 8 },
    { task_id: "t2", member_count: 12 },
    { task_id: "t3", member_count: 5 },
    { task_id: "t4", member_count: 10 },
  ],
  unassigned_tasks: 12,
};

export default function CampaignOps() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data, isLoading } = useCampaignDashboard(id ?? "");
  const createRegionMutation = useCreateRegion();

  const [showRegionForm, setShowRegionForm] = useState(false);
  const [regionName, setRegionName] = useState("");

  const d = data?.initiative ? data : mockDashboard;
  const init = d.initiative;
  const regionStats = d.region_stats ?? [];
  const poolCounts = d.pool_counts ?? {};
  const teamPods = d.team_pods ?? [];

  const handleCreateRegion = async () => {
    if (!id || !regionName.trim()) return;
    try {
      await createRegionMutation.mutateAsync({ initiative_id: id, name: regionName.trim() });
      setRegionName("");
      setShowRegionForm(false);
      toast.success("Region created");
    } catch (e: any) {
      toast.error(e.message || "Failed");
    }
  };

  if (isLoading) {
    return <AppShell><div className="flex justify-center py-32"><Loader2 className="h-6 w-6 animate-spin text-primary" /></div></AppShell>;
  }

  return (
    <AppShell>
      {/* Header */}
      <div className="sticky top-0 z-10 bg-background/95 backdrop-blur-sm border-b border-border">
        <div className="flex items-center gap-3 px-4 h-14">
          <button onClick={() => navigate(-1)} className="h-8 w-8 rounded-full bg-muted flex items-center justify-center">
            <ArrowLeft className="h-4 w-4" />
          </button>
          <span className="font-display font-semibold text-sm truncate">Campaign Operations</span>
          <Badge className="bg-primary/10 text-primary border-0 text-[10px] ml-auto">Live</Badge>
        </div>
      </div>

      <div className="px-4 py-5 space-y-5 pb-32">
        {/* Title */}
        <div>
          <h1 className="font-display text-xl font-bold">{init.title}</h1>
          <p className="text-xs text-muted-foreground mt-1">{init.description}</p>
        </div>

        {/* Live Stats */}
        <div className="grid grid-cols-2 gap-2">
          <div className="bg-card rounded-xl border border-border p-3">
            <div className="flex items-center gap-2 mb-1">
              <Users className="h-4 w-4 text-accent" />
              <span className="text-[10px] text-muted-foreground">Active Volunteers</span>
            </div>
            <p className="font-display font-bold text-2xl">{d.active_volunteers}</p>
          </div>
          <div className="bg-card rounded-xl border border-border p-3">
            <div className="flex items-center gap-2 mb-1">
              <Target className="h-4 w-4 text-primary" />
              <span className="text-[10px] text-muted-foreground">Total Tasks</span>
            </div>
            <p className="font-display font-bold text-2xl">{d.total_tasks}</p>
          </div>
          <div className="bg-card rounded-xl border border-border p-3">
            <div className="flex items-center gap-2 mb-1">
              <Zap className="h-4 w-4 text-secondary" />
              <span className="text-[10px] text-muted-foreground">Completed Today</span>
            </div>
            <p className="font-display font-bold text-2xl text-secondary">{d.completed_today}</p>
          </div>
          <div className="bg-card rounded-xl border border-border p-3">
            <div className="flex items-center gap-2 mb-1">
              <Clock className="h-4 w-4 text-muted-foreground" />
              <span className="text-[10px] text-muted-foreground">In Progress</span>
            </div>
            <p className="font-display font-bold text-2xl">{(d.status_counts?.claimed ?? 0) + (d.status_counts?.in_progress ?? 0)}</p>
          </div>
        </div>

        {/* Task Distribution Pools */}
        <div className="bg-card rounded-2xl border border-border p-4">
          <h2 className="font-display font-semibold text-sm flex items-center gap-1.5 mb-3">
            <BarChart3 className="h-4 w-4 text-primary" /> Task Distribution
          </h2>
          <div className="grid grid-cols-2 gap-2">
            {[
              { key: "regional", label: "Regional", icon: MapPin, color: "text-primary" },
              { key: "team", label: "Team", icon: UsersRound, color: "text-accent" },
              { key: "public", label: "Public", icon: Globe, color: "text-secondary" },
              { key: "online", label: "Online", icon: Zap, color: "text-muted-foreground" },
            ].map(({ key, label, icon: Icon, color }) => (
              <div key={key} className="flex items-center gap-2 p-2 rounded-lg bg-muted/50">
                <Icon className={`h-3.5 w-3.5 ${color}`} />
                <span className="text-xs flex-1">{label}</span>
                <span className="font-display font-bold text-sm">{poolCounts[key] ?? 0}</span>
              </div>
            ))}
          </div>
          {d.unassigned_tasks > 0 && (
            <p className="text-[10px] text-destructive mt-2">⚠ {d.unassigned_tasks} tasks unassigned to regions</p>
          )}
        </div>

        {/* Regional Progress */}
        <div>
          <div className="flex items-center justify-between mb-3">
            <h2 className="font-display font-semibold text-sm flex items-center gap-1.5">
              <MapPin className="h-4 w-4 text-primary" /> Regional Task Pools
            </h2>
            <button onClick={() => setShowRegionForm(!showRegionForm)} className="text-xs text-primary font-medium flex items-center gap-1">
              <Plus className="h-3 w-3" /> Add Region
            </button>
          </div>

          {showRegionForm && (
            <div className="bg-card rounded-xl border border-border p-3 mb-3 space-y-2">
              <Input
                placeholder="Region name (e.g. North Zone)"
                value={regionName}
                onChange={(e) => setRegionName(e.target.value)}
                className="rounded-lg text-sm"
              />
              <Button size="sm" onClick={handleCreateRegion} disabled={!regionName.trim()} className="rounded-lg text-xs">
                Create Region
              </Button>
            </div>
          )}

          <div className="space-y-3">
            {regionStats.map((r: any) => (
              <div key={r.id} className="bg-card rounded-xl border border-border p-4">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <div className="h-7 w-7 rounded-lg bg-primary/10 flex items-center justify-center">
                      <MapPin className="h-3.5 w-3.5 text-primary" />
                    </div>
                    <span className="font-display font-semibold text-sm">{r.name}</span>
                  </div>
                  <span className="font-display font-bold text-sm text-primary">{r.progress_pct}%</span>
                </div>
                <Progress value={r.progress_pct} className="h-2 mb-2" />
                <div className="flex gap-3 text-[10px] text-muted-foreground">
                  <span>{r.completed_tasks} completed</span>
                  <span>{r.active_tasks} active</span>
                  <span>{r.open_tasks} open</span>
                </div>
              </div>
            ))}
            {regionStats.length === 0 && !showRegionForm && (
              <p className="text-xs text-muted-foreground text-center py-6">No regions defined. Add regions to divide tasks geographically.</p>
            )}
          </div>
        </div>

        {/* Team Pods */}
        <div>
          <h2 className="font-display font-semibold text-sm flex items-center gap-1.5 mb-3">
            <UsersRound className="h-4 w-4 text-accent" /> Team Pods
          </h2>
          {teamPods.length > 0 ? (
            <div className="grid grid-cols-2 gap-2">
              {teamPods.map((pod: any, i: number) => (
                <div key={pod.task_id} className="bg-card rounded-xl border border-border p-3">
                  <div className="flex items-center gap-2 mb-1.5">
                    <div className="h-6 w-6 rounded-full bg-accent/10 flex items-center justify-center text-[10px] font-bold text-accent">
                      {i + 1}
                    </div>
                    <span className="text-xs font-medium">Team Pod</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <Users className="h-3 w-3 text-muted-foreground" />
                    <span className="text-xs text-muted-foreground">{pod.member_count} members</span>
                  </div>
                  <Progress value={Math.min(100, (pod.member_count / 15) * 100)} className="h-1.5 mt-2" />
                </div>
              ))}
            </div>
          ) : (
            <p className="text-xs text-muted-foreground text-center py-6">No active team pods. Mark tasks as team-enabled to create pods.</p>
          )}
        </div>

        {/* Capacity Info */}
        <div className="bg-muted/50 rounded-xl p-4 border border-border">
          <h2 className="font-display font-semibold text-xs text-muted-foreground uppercase tracking-wide mb-2">Volunteer Guardrails</h2>
          <div className="space-y-1.5 text-xs text-muted-foreground">
            <div className="flex items-center justify-between">
              <span>Max active tasks per volunteer</span>
              <span className="font-display font-bold text-foreground">1</span>
            </div>
            <div className="flex items-center justify-between">
              <span>Claim cooldown</span>
              <span className="font-display font-bold text-foreground">5 seconds</span>
            </div>
            <div className="flex items-center justify-between">
              <span>Team pod max size</span>
              <span className="font-display font-bold text-foreground">15 volunteers</span>
            </div>
          </div>
        </div>
      </div>
    </AppShell>
  );
}
