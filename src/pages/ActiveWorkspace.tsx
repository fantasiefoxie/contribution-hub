import { useState, useRef, useCallback } from "react";
import { Clock, Zap, Sparkles } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { useAddTaskUpdate, useSubmitTask } from "@/hooks/use-tasks";
import { toast } from "sonner";
import AppShell from "@/components/layout/AppShell";
import { StatusLegend } from "@/components/shared/StatusIndicator";
import MissionCompletionScreen from "@/components/mission/MissionCompletionScreen";
import FocusTaskCard, { type FocusTask } from "@/components/mission/FocusTaskCard";
import TaskFeedCard from "@/components/mission/TaskFeedCard";

// ── Types ──
type TaskStatus = "open" | "in_progress" | "submitted" | "validated";

interface MissionTask extends FocusTask {}

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

export default function ActiveWorkspace() {
  const [tasks, setTasks] = useState(mockMission.tasks);
  const [showCompletion, setShowCompletion] = useState(false);
  const [animateProgress, setAnimateProgress] = useState(false);
  const [successFlash, setSuccessFlash] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const addUpdate = useAddTaskUpdate();
  const submitTask = useSubmitTask();

  // ── Computed ──
  const completedCount = tasks.filter((t) => t.status === "validated" || t.status === "submitted").length;
  const totalCount = tasks.length;
  const progressPct = Math.round((completedCount / totalCount) * 100);
  const timeLeft = Math.max(0, Math.floor((new Date(mockMission.lockedUntil).getTime() - Date.now()) / 60000));
  const hours = Math.floor(timeLeft / 60);
  const mins = timeLeft % 60;

  // Focus task: first in_progress or first open that's mine, or first open
  const focusTask = tasks.find((t) => t.status === "in_progress" && t.isMine)
    || tasks.find((t) => t.status === "in_progress")
    || tasks.find((t) => t.status === "open" && t.isMine)
    || tasks.find((t) => t.status === "open");

  // Feed: everything except the focus task, sorted by status priority
  const statusOrder: Record<TaskStatus, number> = { in_progress: 0, open: 1, submitted: 2, validated: 3 };
  const feedTasks = tasks
    .filter((t) => t.id !== focusTask?.id)
    .sort((a, b) => statusOrder[a.status] - statusOrder[b.status]);

  // ── Animations ──
  const triggerProgress = useCallback(() => {
    setAnimateProgress(true);
    setTimeout(() => setAnimateProgress(false), 900);
  }, []);

  const flashSuccess = useCallback((msg: string) => {
    setSuccessFlash(msg);
    setTimeout(() => setSuccessFlash(null), 2000);
  }, []);

  // ── Actions ──
  const handleAddProgress = async (taskId: string, notes: string) => {
    try { await addUpdate.mutateAsync({ taskId, input: { notes } }); } catch {}
    setTasks((prev) => prev.map((t) => (t.id === taskId && t.status === "open" ? { ...t, status: "in_progress" as TaskStatus } : t)));
    toast.success("Progress added!");
    triggerProgress();
  };

  const handleSubmit = async (taskId: string, notes: string) => {
    try { await submitTask.mutateAsync({ taskId, input: { content: notes } }); } catch {}
    setTasks((prev) => prev.map((t) => (t.id === taskId ? { ...t, status: "submitted" as TaskStatus } : t)));
    flashSuccess("Task submitted! Next task loading...");
    triggerProgress();

    const updated = tasks.map((t) => (t.id === taskId ? { ...t, status: "submitted" as TaskStatus } : t));
    if (updated.every((t) => t.status === "submitted" || t.status === "validated")) {
      setTimeout(() => setShowCompletion(true), 800);
    }
  };

  const handleRelease = (taskId: string) => {
    setTasks((prev) => prev.map((t) => (t.id === taskId ? { ...t, status: "open" as TaskStatus, isMine: false } : t)));
    toast.info("Task released");
  };

  const handleClaim = (taskId: string) => {
    setTasks((prev) => prev.map((t) => (t.id === taskId ? { ...t, isMine: true, status: "in_progress" as TaskStatus } : t)));
    toast.success("Task claimed!");
    triggerProgress();
  };

  const handlePhotoUpload = () => {
    fileInputRef.current?.click();
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
      <input ref={fileInputRef} type="file" accept="image/*" capture="environment" className="hidden" />

      {/* ═══ SUCCESS FLASH ═══ */}
      {successFlash && (
        <div className="fixed top-0 left-0 right-0 z-50 pointer-events-none flex justify-center pt-4 animate-fade-in">
          <div className="bg-primary text-primary-foreground px-5 py-2.5 rounded-full shadow-lg flex items-center gap-2 text-sm font-semibold">
            <Sparkles className="h-4 w-4" /> {successFlash}
          </div>
        </div>
      )}

      {/* ═══ SECTION 1: Mission Header — compact ═══ */}
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

        {/* Progress bar with animation */}
        <div className="mt-3 bg-card rounded-xl border border-border p-3">
          <div className="flex items-center justify-between mb-1.5">
            <span className="text-xs font-medium text-muted-foreground">
              {completedCount} of {totalCount} tasks done
            </span>
            <span className={`font-display font-bold text-sm transition-all duration-500 ${animateProgress ? "text-primary scale-125" : "text-primary"}`}>
              {progressPct}%
            </span>
          </div>
          <div className={`transition-all duration-700 ${animateProgress ? "scale-[1.02]" : ""}`}>
            <div className="relative h-3 w-full overflow-hidden rounded-full bg-muted">
              <div
                className="h-full bg-gradient-to-r from-primary to-primary/80 transition-all duration-700 ease-out rounded-full"
                style={{ width: `${progressPct}%` }}
              />
            </div>
          </div>
          <div className="mt-2"><StatusLegend /></div>
        </div>
      </div>

      {/* ═══ SECTION 2: Focus Task ═══ */}
      <div className="px-4 mb-3">
        {focusTask ? (
          <FocusTaskCard
            task={focusTask}
            remaining={feedTasks.filter((t) => t.status === "open" || t.status === "in_progress").length}
            onAddProgress={handleAddProgress}
            onSubmit={handleSubmit}
            onRelease={handleRelease}
            onPhotoUpload={handlePhotoUpload}
          />
        ) : (
          <div className="bg-primary/5 border border-primary/10 rounded-2xl p-6 text-center animate-fade-in">
            <Sparkles className="h-8 w-8 text-primary mx-auto mb-2" />
            <p className="font-semibold text-primary">All tasks completed!</p>
            <p className="text-xs text-muted-foreground mt-1">Great work on this mission</p>
          </div>
        )}
      </div>

      {/* ═══ SECTION 3: Task Feed ═══ */}
      {feedTasks.length > 0 && (
        <div className="px-4 pb-24">
          <div className="flex items-center gap-2 mb-2">
            <h2 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Task Queue</h2>
            <span className="text-[10px] text-muted-foreground bg-muted px-1.5 py-0.5 rounded">{feedTasks.length}</span>
          </div>
          <div className="space-y-2">
            {feedTasks.map((task, i) => (
              <TaskFeedCard key={task.id} task={task} index={i} onClaim={handleClaim} />
            ))}
          </div>
        </div>
      )}
    </AppShell>
  );
}
