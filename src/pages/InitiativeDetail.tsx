import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Users, Target, Calendar, MapPin, TrendingUp } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import AppShell from "@/components/layout/AppShell";
import { mockInitiatives, mockInitiativeProgress, mockTasks, mockOrganizations } from "@/lib/mock-data";

export default function InitiativeDetail() {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();

  const initiative = mockInitiatives.find((i) => i.slug === slug);
  const progress = initiative ? mockInitiativeProgress[initiative.id] : null;
  const tasks = initiative ? mockTasks.filter((t) => t.initiative_id === initiative.id) : [];

  if (!initiative) {
    return (
      <AppShell>
        <div className="flex flex-col items-center justify-center h-64 gap-3">
          <p className="text-muted-foreground text-sm">Initiative not found</p>
          <button onClick={() => navigate("/initiatives")} className="text-primary text-sm font-medium">
            Browse Initiatives →
          </button>
        </div>
      </AppShell>
    );
  }

  const completionPct = progress?.completion_pct ?? 0;

  return (
    <AppShell>
      {/* Header */}
      <div className="sticky top-0 z-10 bg-background/95 backdrop-blur-sm border-b border-border">
        <div className="flex items-center gap-3 px-4 h-14">
          <button onClick={() => navigate(-1)} className="h-8 w-8 rounded-full bg-muted flex items-center justify-center">
            <ArrowLeft className="h-4 w-4" />
          </button>
          <span className="font-display font-semibold text-sm truncate">Initiative</span>
        </div>
      </div>

      <div className="px-4 py-5 space-y-5">
        {/* Title & status */}
        <div>
          <Badge className="bg-primary/10 text-primary border-0 text-[10px] mb-2">{initiative.status.toUpperCase()}</Badge>
          <h1 className="font-display text-xl font-bold leading-tight">{initiative.title}</h1>
          <p className="text-sm text-muted-foreground mt-1.5">{initiative.description}</p>
        </div>

        {/* Meta */}
        <div className="grid grid-cols-3 gap-2">
          <div className="bg-muted rounded-xl p-3 text-center">
            <Users className="h-4 w-4 text-primary mx-auto mb-1" />
            <p className="text-[10px] text-muted-foreground">Volunteers</p>
            <p className="font-display font-semibold text-sm">{progress?.contributors_active ?? 0}</p>
          </div>
          <div className="bg-muted rounded-xl p-3 text-center">
            <Target className="h-4 w-4 text-secondary mx-auto mb-1" />
            <p className="text-[10px] text-muted-foreground">Tasks</p>
            <p className="font-display font-semibold text-sm">{progress?.tasks_total ?? 0}</p>
          </div>
          <div className="bg-muted rounded-xl p-3 text-center">
            <Calendar className="h-4 w-4 text-accent mx-auto mb-1" />
            <p className="text-[10px] text-muted-foreground">Ends</p>
            <p className="font-display font-semibold text-sm">{initiative.end_date ? new Date(initiative.end_date).toLocaleDateString("en-IN", { month: "short", day: "numeric" }) : "—"}</p>
          </div>
        </div>

        {/* Progress */}
        <div className="bg-primary/5 rounded-xl p-4 border border-primary/10">
          <div className="flex justify-between items-baseline mb-2">
            <span className="text-xs text-muted-foreground">Overall Progress</span>
            <span className="font-display font-bold text-sm text-primary">{completionPct}%</span>
          </div>
          <Progress value={completionPct} className="h-2.5" />
          <div className="grid grid-cols-3 gap-2 mt-3 text-center">
            <div>
              <p className="font-display font-bold text-sm text-primary">{progress?.tasks_completed ?? 0}</p>
              <p className="text-[9px] text-muted-foreground">Done</p>
            </div>
            <div>
              <p className="font-display font-bold text-sm text-secondary">{progress?.tasks_in_progress ?? 0}</p>
              <p className="text-[9px] text-muted-foreground">Active</p>
            </div>
            <div>
              <p className="font-display font-bold text-sm text-muted-foreground">{progress?.tasks_open ?? 0}</p>
              <p className="text-[9px] text-muted-foreground">Open</p>
            </div>
          </div>
        </div>

        {/* Location */}
        {initiative.region_name && (
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <MapPin className="h-3.5 w-3.5" />
            <span>{initiative.region_name}</span>
          </div>
        )}

        {/* Related tasks */}
        <div>
          <h2 className="font-display font-semibold text-sm mb-2 flex items-center gap-2">
            <TrendingUp className="h-4 w-4 text-primary" />
            Tasks in this Initiative
          </h2>
          <div className="space-y-1.5">
            {tasks.length === 0 && <p className="text-xs text-muted-foreground py-4 text-center">No tasks found</p>}
            {tasks.map((task) => (
              <button
                key={task.id}
                onClick={() => navigate(`/task/${task.id}`)}
                className="w-full text-left bg-card rounded-xl border border-border p-3 flex items-center gap-3 hover:border-primary/20 transition-colors active:scale-[0.99]"
              >
                <div className={`h-2 w-2 rounded-full shrink-0 ${task.status === "open" ? "bg-primary" : task.status === "in_progress" ? "bg-secondary" : "bg-muted-foreground"}`} />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate">{task.title}</p>
                  <p className="text-[10px] text-muted-foreground mt-0.5">Difficulty {task.difficulty}/5 · {task.status}</p>
                </div>
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="h-24" />
    </AppShell>
  );
}
