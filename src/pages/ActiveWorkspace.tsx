import { useState, useRef, useCallback } from "react";
import {
  Clock, CheckCircle2, Circle, Send, Camera, X,
  Zap, Filter, ChevronDown, ChevronUp, MapPin, Wifi,
  AlertTriangle, Loader2, Unlock, BarChart3,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { useAddTaskUpdate, useSubmitTask } from "@/hooks/use-tasks";
import { toast } from "sonner";
import AppShell from "@/components/layout/AppShell";
import ScreenHeader from "@/components/shared/ScreenHeader";
import FilterChipsBar, { type FilterOption } from "@/components/shared/FilterChipsBar";
import QuickActionBar from "@/components/shared/QuickActionBar";
import { StatusDot, StatusBadge, StatusLegend, DifficultyBadge, type UniversalStatus } from "@/components/shared/StatusIndicator";
import MissionCompletionScreen from "@/components/mission/MissionCompletionScreen";

// ── Types ──
type TaskStatus = "open" | "in_progress" | "submitted" | "validated";
type FilterKey = "all" | "mine" | "nearby" | "online";

interface MissionTask {
  id: string;
  title: string;
  difficulty: number;
  status: TaskStatus;
  isMine: boolean;
  isOnline: boolean;
  distance?: string;
}

// ── Mock data ──
const mockMission = {
  title: "Spring Community Cleanup Campaign",
  organization: "Green City Initiative",
  lockedUntil: new Date(Date.now() + 45 * 60000).toISOString(),
  tasks: [
    { id: "t1", title: "Clear weeds from garden beds A-D", difficulty: 2, status: "validated" as TaskStatus, isMine: true, isOnline: false, distance: "0.2 km" },
    { id: "t2", title: "Water all seedlings in section B", difficulty: 1, status: "submitted" as TaskStatus, isMine: true, isOnline: false, distance: "0.2 km" },
    { id: "t3", title: "Photograph completed work areas", difficulty: 1, status: "in_progress" as TaskStatus, isMine: true, isOnline: false, distance: "0.2 km" },
    { id: "t4", title: "Update volunteer spreadsheet", difficulty: 1, status: "open" as TaskStatus, isMine: false, isOnline: true },
    { id: "t5", title: "Return tools to garden shed", difficulty: 1, status: "open" as TaskStatus, isMine: false, isOnline: false, distance: "0.3 km" },
    { id: "t6", title: "Submit final cleanup report", difficulty: 3, status: "open" as TaskStatus, isMine: false, isOnline: true },
  ] as MissionTask[],
};

const toUniversalStatus = (s: TaskStatus): UniversalStatus =>
  s === "open" ? "not_started" : s === "in_progress" ? "in_progress" : s === "submitted" ? "submitted" : "validated";

// ── Filters ──
const filters: FilterOption<FilterKey>[] = [
  { key: "all", label: "All", icon: Filter },
  { key: "mine", label: "My Tasks", icon: CheckCircle2 },
  { key: "nearby", label: "Nearby", icon: MapPin },
  { key: "online", label: "Online", icon: Wifi },
];

export default function ActiveWorkspace() {
  const [activeFilter, setActiveFilter] = useState<FilterKey>("all");
  const [expandedTask, setExpandedTask] = useState<string | null>(null);
  const [inlineNotes, setInlineNotes] = useState<Record<string, string>>({});
  const [tasks, setTasks] = useState(mockMission.tasks);
  const [showCompletion, setShowCompletion] = useState(false);
  const [animateProgress, setAnimateProgress] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const addUpdate = useAddTaskUpdate();
  const submitTask = useSubmitTask();

  // ── Computed ──
  const completedCount = tasks.filter((t) => t.status === "validated").length;
  const totalCount = tasks.length;
  const progressPct = Math.round((completedCount / totalCount) * 100);
  const timeLeft = Math.max(0, Math.floor((new Date(mockMission.lockedUntil).getTime() - Date.now()) / 60000));
  const hours = Math.floor(timeLeft / 60);
  const mins = timeLeft % 60;

  const filtered = tasks.filter((t) => {
    if (activeFilter === "mine") return t.isMine;
    if (activeFilter === "nearby") return !t.isOnline;
    if (activeFilter === "online") return t.isOnline;
    return true;
  });

  // ── Actions ──
  const triggerProgressAnimation = useCallback(() => {
    setAnimateProgress(true);
    setTimeout(() => setAnimateProgress(false), 800);
  }, []);

  const handleAddProgress = async (taskId: string) => {
    const notes = inlineNotes[taskId]?.trim();
    if (!notes) return;
    try {
      await addUpdate.mutateAsync({ taskId, input: { notes } });
    } catch {
      // Demo fallback
    }
    setTasks((prev) =>
      prev.map((t) => (t.id === taskId && t.status === "open" ? { ...t, status: "in_progress" as TaskStatus } : t))
    );
    setInlineNotes((prev) => ({ ...prev, [taskId]: "" }));
    toast.success("Progress added!");
    triggerProgressAnimation();
  };

  const handleSubmitTask = async (taskId: string) => {
    const notes = inlineNotes[taskId]?.trim() || "Task completed";
    try {
      await submitTask.mutateAsync({ taskId, input: { content: notes } });
    } catch {
      // Demo fallback
    }
    setTasks((prev) =>
      prev.map((t) => (t.id === taskId ? { ...t, status: "submitted" as TaskStatus } : t))
    );
    setInlineNotes((prev) => ({ ...prev, [taskId]: "" }));
    setExpandedTask(null);
    toast.success("Task submitted for validation!");
    triggerProgressAnimation();

    // Check if all tasks done
    const updated = tasks.map((t) => (t.id === taskId ? { ...t, status: "submitted" as TaskStatus } : t));
    if (updated.every((t) => t.status === "submitted" || t.status === "validated")) {
      setTimeout(() => setShowCompletion(true), 600);
    }
  };

  const handleReleaseTask = (taskId: string) => {
    setTasks((prev) =>
      prev.map((t) => (t.id === taskId ? { ...t, status: "open" as TaskStatus, isMine: false } : t))
    );
    setExpandedTask(null);
    toast.info("Task released");
  };

  const handlePhotoUpload = (taskId: string) => {
    fileInputRef.current?.click();
    // In real app, link to task
    toast.success("Photo attached");
  };

  if (showCompletion) {
    return (
      <MissionCompletionScreen
        taskTitle={mockMission.title}
        impactMetrics={{ trees_maintained: 15, waste_removed_kg: 8 }}
        initiativeTitle="Spring Cleanup Campaign"
        initiativeProgress={43}
        nextMission={{ id: "demo-3", title: "Survey 15 households in Ward 3", estimatedTime: "30 min", distance: "1.2 km" }}
        onDismiss={() => setShowCompletion(false)}
      />
    );
  }

  return (
    <AppShell>
      {/* Hidden file input */}
      <input ref={fileInputRef} type="file" accept="image/*" capture="environment" className="hidden" />

      {/* ═══ SECTION 1: Mission Header ═══ */}
      <div className="px-4 pt-5 pb-3">
        <div className="flex items-start justify-between gap-3">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <Zap className="h-4 w-4 text-secondary shrink-0" />
              <span className="text-[10px] font-semibold uppercase tracking-wider text-secondary">Active Mission</span>
            </div>
            <h1 className="font-display text-lg font-bold leading-tight truncate">{mockMission.title}</h1>
            <p className="text-xs text-muted-foreground mt-0.5">{mockMission.organization}</p>
          </div>
          <Badge className="bg-card border border-border text-foreground text-[11px] gap-1 shrink-0 font-mono">
            <Clock className="h-3 w-3 text-secondary" />
            {hours > 0 ? `${hours}h ` : ""}{mins}m
          </Badge>
        </div>

        {/* Progress bar */}
        <div className="mt-3 bg-card rounded-xl border border-border p-3">
          <div className="flex items-center justify-between mb-1.5">
            <span className="text-xs font-medium text-muted-foreground">
              {completedCount} of {totalCount} tasks completed
            </span>
            <span className={`font-display font-bold text-sm transition-all duration-500 ${animateProgress ? "text-primary scale-110" : "text-primary"}`}>
              {progressPct}%
            </span>
          </div>
          <div className={`transition-all duration-700 ${animateProgress ? "scale-[1.01]" : ""}`}>
            <div className="relative h-2.5 w-full overflow-hidden rounded-full bg-muted">
              <div className="h-full bg-primary transition-all duration-500" style={{ width: `${progressPct}%` }} />
            </div>
          </div>
          <div className="mt-2.5">
            <StatusLegend />
          </div>
        </div>
      </div>

      {/* ═══ SECTION 2: Task Checklist ═══ */}
      <div className="px-4 mb-2">
        <FilterChipsBar<FilterKey>
          options={filters.map(f => ({ ...f, count: f.key === "all" ? totalCount : undefined }))}
          active={activeFilter}
          onChange={setActiveFilter}
        />
      </div>

      {/* Task List */}
      <div className="px-4 space-y-2 pb-44">
        {filtered.map((task, i) => {
          const isExpanded = expandedTask === task.id;
          const notes = inlineNotes[task.id] || "";
          const uStatus = toUniversalStatus(task.status);

          return (
            <div
              key={task.id}
              className={`bg-card rounded-xl border transition-all duration-200 animate-fade-in ${
                isExpanded ? "border-primary/30 shadow-md" : "border-border"
              }`}
              style={{ animationDelay: `${i * 40}ms` }}
            >
              {/* Task Row — Large tap target */}
              <button
                onClick={() => setExpandedTask(isExpanded ? null : task.id)}
                className="w-full flex items-center gap-3 p-3.5 min-h-[56px] text-left"
              >
                <StatusDot status={uStatus} />
                <div className="flex-1 min-w-0">
                  <p className={`text-sm font-medium leading-snug ${task.status === "validated" ? "line-through text-muted-foreground" : "text-foreground"}`}>
                    {task.title}
                  </p>
                  <div className="flex items-center gap-2 mt-0.5">
                    <DifficultyBadge level={task.difficulty} />
                    <StatusBadge status={uStatus} />
                    {task.isOnline && (
                      <span className="flex items-center gap-0.5 text-[9px] text-accent">
                        <Wifi className="h-2.5 w-2.5" /> Online
                      </span>
                    )}
                    {task.distance && !task.isOnline && (
                      <span className="flex items-center gap-0.5 text-[9px] text-muted-foreground">
                        <MapPin className="h-2.5 w-2.5" /> {task.distance}
                      </span>
                    )}
                  </div>
                </div>
                {isExpanded ? (
                  <ChevronUp className="h-4 w-4 text-muted-foreground shrink-0" />
                ) : (
                  <ChevronDown className="h-4 w-4 text-muted-foreground shrink-0" />
                )}
              </button>

              {/* ── Inline Expanded Panel ── */}
              {isExpanded && (
                <div className="px-3.5 pb-3.5 pt-0 space-y-2.5 animate-fade-in">
                  <div className="border-t border-border pt-2.5" />
                  {task.status !== "validated" && (
                    <>
                      <Textarea
                        placeholder="Quick progress note..."
                        value={notes}
                        onChange={(e) => setInlineNotes((prev) => ({ ...prev, [task.id]: e.target.value }))}
                        className="min-h-[60px] border-0 bg-muted rounded-lg text-sm resize-none"
                      />
                      <div className="flex items-center gap-2">
                        <Button variant="outline" size="sm" onClick={() => handlePhotoUpload(task.id)} className="rounded-lg text-[11px] gap-1 h-8">
                          <Camera className="h-3 w-3" /> Photo
                        </Button>
                        <div className="flex-1" />
                        {task.status !== "submitted" && (
                          <Button size="sm" variant="outline" onClick={() => handleAddProgress(task.id)} disabled={!notes.trim()} className="rounded-lg text-[11px] gap-1 h-8 border-accent text-accent hover:bg-accent/10">
                            <BarChart3 className="h-3 w-3" /> Add Progress
                          </Button>
                        )}
                        {(task.status === "in_progress" || task.status === "open") && (
                          <Button size="sm" onClick={() => handleSubmitTask(task.id)} className="rounded-lg text-[11px] gap-1 h-8 bg-primary hover:bg-primary/90">
                            <Send className="h-3 w-3" /> Submit
                          </Button>
                        )}
                      </div>
                    </>
                  )}
                  {task.status === "validated" && (
                    <div className="flex items-center gap-2 text-sm text-primary">
                      <CheckCircle2 className="h-4 w-4" />
                      <span className="font-medium">Validated — great work!</span>
                    </div>
                  )}
                  {task.isMine && task.status !== "validated" && task.status !== "submitted" && (
                    <button onClick={() => handleReleaseTask(task.id)} className="flex items-center gap-1.5 text-[11px] text-destructive/70 hover:text-destructive transition-colors pt-1">
                      <Unlock className="h-3 w-3" /> Release this task
                    </button>
                  )}
                </div>
              )}
            </div>
          );
        })}

        {filtered.length === 0 && (
          <div className="text-center py-10 text-muted-foreground">
            <Filter className="h-8 w-8 mx-auto mb-2 opacity-40" />
            <p className="text-sm">No tasks match this filter</p>
          </div>
        )}
      </div>

      {/* ═══ SECTION 3: Quick Action Panel ═══ */}
      <QuickActionBar
        actions={[
          {
            icon: BarChart3, label: "Add Progress", color: "text-accent", bg: "bg-accent/10",
            onClick: () => {
              const t = tasks.find((t) => t.status === "in_progress") || tasks.find((t) => t.status === "open");
              if (t) setExpandedTask(t.id); else toast.info("No open tasks available");
            },
          },
          {
            icon: Send, label: "Submit Task", color: "text-primary", bg: "bg-primary/10",
            onClick: () => {
              const t = tasks.find((t) => t.status === "in_progress");
              if (t) setExpandedTask(t.id); else toast.info("No tasks ready to submit");
            },
          },
          {
            icon: Camera, label: "Upload Proof", color: "text-secondary", bg: "bg-secondary/10",
            onClick: () => fileInputRef.current?.click(),
          },
          {
            icon: Unlock, label: "Release Task", color: "text-destructive", bg: "bg-destructive/10",
            onClick: () => {
              const t = tasks.find((t) => t.isMine && t.status !== "validated" && t.status !== "submitted");
              if (t) handleReleaseTask(t.id); else toast.info("No tasks to release");
            },
          },
        ]}
      />
    </AppShell>
  );
}