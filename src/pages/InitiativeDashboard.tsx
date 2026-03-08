import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Users, Target, Loader2, Plus, Zap, FolderOpen, Sparkles, TrendingUp, BarChart3, ChevronRight, Building2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { toast } from "sonner";
import AppShell from "@/components/layout/AppShell";
import InitiativeActivityFeed from "@/components/initiative/InitiativeActivityFeed";
import { useInitiative, useJoinInitiative, usePromoteInitiative, useCreateProject, useGenerateTasks } from "@/hooks/use-initiatives";
import { useInitiativeProgress } from "@/hooks/use-impact";

// Mock data
const mockDashboard = {
  initiative: {
    id: "init-1", title: "River Cleanup Campaign",
    description: "City-wide river cleanup initiative targeting 50 pollution hotspots across 15 wards. Join hundreds of volunteers working to restore our waterways.",
    status: "active", is_micro: false,
    start_date: "2026-02-01", end_date: "2026-05-31",
    target_metrics: { households_reached: 1000, waste_removed_kg: 500 },
    created_by: "user-1",
  },
  projects: [
    { id: "p-1", title: "Survey pollution sites", description: "Map and document pollution hotspots", sort_order: 0 },
    { id: "p-2", title: "Community outreach", description: "Engage local communities", sort_order: 1 },
    { id: "p-3", title: "Cleanup operations", description: "Organize cleanup drives", sort_order: 2 },
  ],
  participant_count: 127,
  task_count: 34,
  task_status_counts: { open: 12, claimed: 5, in_progress: 8, validated: 9 },
  tasks: [
    { id: "t-1", title: "Survey 20 households in Ward 1", difficulty: 2, status: "open", tags: ["survey", "community"] },
    { id: "t-2", title: "Remove waste from River Section A", difficulty: 3, status: "open", tags: ["environment", "cleanup"] },
    { id: "t-3", title: "Document pollution hotspot #12", difficulty: 1, status: "claimed", tags: ["research", "environment"] },
    { id: "t-4", title: "Organize community meeting Ward 5", difficulty: 3, status: "in_progress", tags: ["community", "outreach"] },
  ],
  organizations: [
    { id: "org-1", name: "Green Earth Foundation", logo_url: null },
    { id: "org-2", name: "City Municipal Corp", logo_url: null },
  ],
  activity: [
    {
      id: "a-1", score: 20, impact_metrics: { waste_removed_kg: 12 },
      created_at: new Date(Date.now() - 1800000).toISOString(),
      task: { title: "River Section B cleanup", tags: ["environment"] },
      contributor: { display_name: "Aisha Khan", avatar_url: null },
    },
    {
      id: "a-2", score: 30, impact_metrics: { households_reached: 15 },
      created_at: new Date(Date.now() - 3600000).toISOString(),
      task: { title: "Ward 3 household survey", tags: ["community"] },
      contributor: { display_name: "Rahul Sharma", avatar_url: null },
    },
    {
      id: "a-3", score: 15, impact_metrics: { waste_removed_kg: 8 },
      created_at: new Date(Date.now() - 7200000).toISOString(),
      task: { title: "Hotspot #7 documentation", tags: ["research"] },
      contributor: { display_name: "Priya Patel", avatar_url: null },
    },
  ],
};

const difficultyLabels = ["", "Easy", "Moderate", "Advanced", "Organizer"];

type Tab = "overview" | "tasks" | "activity";

export default function InitiativeDashboard() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data, isLoading } = useInitiative(id ?? "");
  const { data: progressData } = useInitiativeProgress(id ?? "");
  const joinMutation = useJoinInitiative();
  const promoteMutation = usePromoteInitiative();
  const createProjectMutation = useCreateProject();
  const generateTasksMutation = useGenerateTasks();

  const [tab, setTab] = useState<Tab>("overview");
  const [showTaskGen, setShowTaskGen] = useState(false);
  const [showProjectForm, setShowProjectForm] = useState(false);
  const [projectTitle, setProjectTitle] = useState("");
  const [templateTitle, setTemplateTitle] = useState("");
  const [taskDesc, setTaskDesc] = useState("");
  const [taskCount, setTaskCount] = useState(5);
  const [taskDifficulty, setTaskDifficulty] = useState(2);

  // Fallback to mock data when API returns error or no data (e.g. mock IDs like "init-1")
  const d = data && typeof data === 'object' && 'initiative' in data ? data : mockDashboard;
  const init = d.initiative;
  const projects = d.projects ?? [];
  const tasks = d.tasks ?? mockDashboard.tasks;
  const organizations = d.organizations ?? mockDashboard.organizations;
  const activity = d.activity ?? mockDashboard.activity;
  const totalTasks = d.task_count ?? 0;
  const statusCounts = d.task_status_counts ?? {};
  const completedTasks = (statusCounts.validated ?? 0);
  const progressPct = totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0;

  // Use real progress data if available
  const realProgress = progressData?.progress ?? {};

  const handleJoin = async () => {
    if (!id) return;
    try {
      await joinMutation.mutateAsync(id);
      toast.success("You joined this initiative! Tasks will now be prioritized for you.");
    } catch (e: any) {
      toast.error(e.message || "Failed to join");
    }
  };

  const handlePromote = async () => {
    if (!id) return;
    try {
      await promoteMutation.mutateAsync(id);
      toast.success("Initiative promoted to full campaign!");
    } catch (e: any) {
      toast.error(e.message || "Failed to promote");
    }
  };

  const handleCreateProject = async () => {
    if (!id || !projectTitle.trim()) return;
    try {
      await createProjectMutation.mutateAsync({ initiativeId: id, input: { title: projectTitle.trim() } });
      setProjectTitle("");
      setShowProjectForm(false);
      toast.success("Project created");
    } catch (e: any) {
      toast.error(e.message || "Failed");
    }
  };

  const handleGenerateTasks = async () => {
    if (!id || !templateTitle.trim()) return;
    try {
      await generateTasksMutation.mutateAsync({
        initiativeId: id,
        input: {
          template_title: templateTitle.trim(),
          description: taskDesc.trim() || undefined,
          difficulty: taskDifficulty,
          count: taskCount,
        },
      });
      setTemplateTitle("");
      setTaskDesc("");
      setShowTaskGen(false);
      toast.success(`${taskCount} tasks generated!`);
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
          <button onClick={() => navigate("/initiatives")} className="h-8 w-8 rounded-full bg-muted flex items-center justify-center">
            <ArrowLeft className="h-4 w-4" />
          </button>
          <span className="font-display font-semibold text-sm truncate">{init.title}</span>
        </div>
      </div>

      <div className="px-4 py-5 space-y-5 pb-32">
        {/* Title & Description */}
        <div>
          <div className="flex items-center gap-2 flex-wrap">
            <h1 className="font-display text-xl font-bold">{init.title}</h1>
            {init.is_micro && <Badge className="bg-accent/10 text-accent border-0 text-[10px]">Micro</Badge>}
            <Badge className="bg-primary/10 text-primary border-0 text-[10px] capitalize">{init.status}</Badge>
          </div>
          <p className="text-sm text-muted-foreground mt-2 leading-relaxed">{init.description}</p>
          {init.start_date && init.end_date && (
            <p className="text-[10px] text-muted-foreground mt-2">
              {new Date(init.start_date).toLocaleDateString()} — {new Date(init.end_date).toLocaleDateString()}
            </p>
          )}
        </div>

        {/* Participating Organizations */}
        {organizations.length > 0 && (
          <div className="flex gap-2 flex-wrap">
            {organizations.map((org: any) => (
              <div key={org.id} className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-muted text-xs">
                <Building2 className="h-3 w-3 text-muted-foreground" />
                <span className="font-medium">{org.name}</span>
              </div>
            ))}
          </div>
        )}

        {/* Stats */}
        <div className="grid grid-cols-3 gap-2">
          <div className="bg-card rounded-xl border border-border p-3 text-center">
            <Target className="h-4 w-4 text-primary mx-auto mb-1" />
            <p className="font-display font-bold text-lg">{totalTasks}</p>
            <p className="text-[10px] text-muted-foreground">Total Tasks</p>
          </div>
          <div className="bg-card rounded-xl border border-border p-3 text-center">
            <Zap className="h-4 w-4 text-secondary mx-auto mb-1" />
            <p className="font-display font-bold text-lg">{completedTasks}</p>
            <p className="text-[10px] text-muted-foreground">Completed</p>
          </div>
          <div className="bg-card rounded-xl border border-border p-3 text-center">
            <Users className="h-4 w-4 text-accent mx-auto mb-1" />
            <p className="font-display font-bold text-lg">{d.participant_count ?? 0}</p>
            <p className="text-[10px] text-muted-foreground">Volunteers</p>
          </div>
        </div>

        {/* Progress */}
        <div className="bg-card rounded-2xl border border-border p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wide flex items-center gap-1.5">
              <TrendingUp className="h-3.5 w-3.5" /> Overall Progress
            </span>
            <span className="font-display font-bold text-sm text-primary">{progressPct}%</span>
          </div>
          <Progress value={progressPct} className="h-2.5" />
          <div className="flex justify-between mt-2 text-[10px] text-muted-foreground">
            <span>{completedTasks} completed</span>
            <span>{(statusCounts.open ?? 0)} open · {(statusCounts.in_progress ?? 0) + (statusCounts.claimed ?? 0)} active</span>
          </div>
        </div>

        {/* Success Metrics with real progress */}
        {init.target_metrics && Object.keys(init.target_metrics).length > 0 && (
          <div className="bg-primary/5 rounded-2xl border border-primary/10 p-4 space-y-3">
            <p className="text-xs font-semibold text-primary uppercase tracking-wide">Success Metrics</p>
            {Object.entries(init.target_metrics as Record<string, number>).map(([key, target]) => {
              const real = realProgress[key];
              const current = real?.current ?? 0;
              const pct = real?.percentage ?? 0;
              return (
                <div key={key}>
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-muted-foreground capitalize">{key.replace(/_/g, " ")}</span>
                    <span className="font-display font-bold">{current.toLocaleString()} / {target.toLocaleString()}</span>
                  </div>
                  <Progress value={pct} className="h-1.5" />
                </div>
              );
            })}
          </div>
        )}

        {/* Join Button (prominent) */}
        <Button onClick={handleJoin} disabled={joinMutation.isPending} className="w-full h-12 rounded-xl font-display font-semibold text-base bg-primary hover:bg-primary/90">
          {joinMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <><Users className="h-5 w-5 mr-2" /> Join This Initiative</>}
        </Button>

        {/* Tabs */}
        <div className="flex border-b border-border">
          {(["overview", "tasks", "activity"] as Tab[]).map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`flex-1 py-2.5 text-xs font-semibold capitalize transition-colors ${
                tab === t
                  ? "text-primary border-b-2 border-primary"
                  : "text-muted-foreground hover:text-foreground"
              }`}
            >
              {t}
            </button>
          ))}
        </div>

        {/* Tab Content */}
        {tab === "overview" && (
          <div className="space-y-5">
            {/* Projects */}
            <div>
              <div className="flex items-center justify-between mb-3">
                <h2 className="font-display font-semibold text-sm flex items-center gap-1.5">
                  <FolderOpen className="h-4 w-4 text-primary" /> Projects
                </h2>
                <button onClick={() => setShowProjectForm(!showProjectForm)} className="text-xs text-primary font-medium flex items-center gap-1">
                  <Plus className="h-3 w-3" /> Add
                </button>
              </div>

              {showProjectForm && (
                <div className="bg-card rounded-xl border border-border p-3 mb-3 space-y-2">
                  <Input placeholder="Project name" value={projectTitle} onChange={(e) => setProjectTitle(e.target.value)} className="rounded-lg text-sm" />
                  <Button size="sm" onClick={handleCreateProject} disabled={!projectTitle.trim()} className="rounded-lg text-xs">
                    Create Project
                  </Button>
                </div>
              )}

              <div className="space-y-2">
                {projects.map((p: any, i: number) => (
                  <div key={p.id} className="bg-card rounded-xl border border-border p-3">
                    <div className="flex items-center gap-2">
                      <div className="h-6 w-6 rounded-lg bg-primary/10 flex items-center justify-center text-xs font-bold text-primary">
                        {i + 1}
                      </div>
                      <div>
                        <p className="font-medium text-sm">{p.title}</p>
                        {p.description && <p className="text-[10px] text-muted-foreground">{p.description}</p>}
                      </div>
                    </div>
                  </div>
                ))}
                {projects.length === 0 && !showProjectForm && (
                  <p className="text-xs text-muted-foreground text-center py-4">No projects yet.</p>
                )}
              </div>
            </div>

            {/* Task Generator */}
            <div>
              <div className="flex items-center justify-between mb-3">
                <h2 className="font-display font-semibold text-sm flex items-center gap-1.5">
                  <Sparkles className="h-4 w-4 text-secondary" /> Task Generator
                </h2>
                <button onClick={() => setShowTaskGen(!showTaskGen)} className="text-xs text-primary font-medium flex items-center gap-1">
                  <Plus className="h-3 w-3" /> Generate
                </button>
              </div>

              {showTaskGen && (
                <div className="bg-card rounded-2xl border border-border p-4 space-y-3">
                  <div>
                    <p className="text-[10px] text-muted-foreground mb-1">Title Template</p>
                    <Input
                      placeholder='e.g. Survey 20 households in Ward {{number}}'
                      value={templateTitle}
                      onChange={(e) => setTemplateTitle(e.target.value)}
                      className="rounded-xl text-sm"
                    />
                  </div>
                  <div>
                    <p className="text-[10px] text-muted-foreground mb-1">Description</p>
                    <Textarea
                      placeholder="Task description..."
                      value={taskDesc}
                      onChange={(e) => setTaskDesc(e.target.value)}
                      className="rounded-xl text-sm min-h-[60px]"
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <p className="text-[10px] text-muted-foreground mb-1">Count</p>
                      <Input type="number" value={taskCount} onChange={(e) => setTaskCount(Number(e.target.value))} className="rounded-xl text-sm" />
                    </div>
                    <div>
                      <p className="text-[10px] text-muted-foreground mb-1">Difficulty (1-4)</p>
                      <Input type="number" min={1} max={4} value={taskDifficulty} onChange={(e) => setTaskDifficulty(Number(e.target.value))} className="rounded-xl text-sm" />
                    </div>
                  </div>
                  <Button
                    onClick={handleGenerateTasks}
                    disabled={generateTasksMutation.isPending || !templateTitle.trim()}
                    className="w-full rounded-xl"
                  >
                    {generateTasksMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : `Generate ${taskCount} Tasks`}
                  </Button>
                </div>
              )}
            </div>

            {/* Ops & Promote */}
            <div className="space-y-2">
              <Button
                onClick={() => navigate(`/initiatives/${id}/ops`)}
                className="w-full h-11 rounded-xl font-display font-semibold bg-accent hover:bg-accent/90 text-accent-foreground gap-2"
              >
                <BarChart3 className="h-4 w-4" /> Campaign Operations
              </Button>
              {init.is_micro && (
                <Button onClick={handlePromote} disabled={promoteMutation.isPending} variant="outline" className="w-full h-11 rounded-xl font-display font-semibold">
                  {promoteMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <><TrendingUp className="h-4 w-4 mr-2" /> Promote to Full Campaign</>}
                </Button>
              )}
            </div>
          </div>
        )}

        {tab === "tasks" && (
          <div className="space-y-2">
            {tasks.length === 0 ? (
              <div className="text-center py-12">
                <p className="text-3xl mb-2">📋</p>
                <p className="text-xs text-muted-foreground">No tasks yet. Generate some from the overview tab!</p>
              </div>
            ) : (
              tasks.map((task: any) => (
                <button
                  key={task.id}
                  onClick={() => navigate(`/task/${task.id}`)}
                  className="w-full bg-card rounded-xl border border-border p-3 text-left hover:border-primary/20 transition-colors"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-sm truncate">{task.title}</p>
                      <div className="flex items-center gap-2 mt-1">
                        <Badge variant="outline" className="text-[10px] px-1.5 py-0">
                          {difficultyLabels[task.difficulty] || `Lvl ${task.difficulty}`}
                        </Badge>
                        <Badge
                          className={`text-[10px] px-1.5 py-0 border-0 ${
                            task.status === "open" ? "bg-primary/10 text-primary" :
                            task.status === "validated" ? "bg-secondary/10 text-secondary" :
                            "bg-muted text-muted-foreground"
                          }`}
                        >
                          {task.status}
                        </Badge>
                        {task.tags?.slice(0, 2).map((tag: string) => (
                          <span key={tag} className="text-[10px] text-muted-foreground">#{tag}</span>
                        ))}
                      </div>
                    </div>
                    <ChevronRight className="h-4 w-4 text-muted-foreground shrink-0" />
                  </div>
                </button>
              ))
            )}
          </div>
        )}

        {tab === "activity" && (
          <InitiativeActivityFeed activities={activity} />
        )}
      </div>
    </AppShell>
  );
}
