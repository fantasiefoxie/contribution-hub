import { useState, useRef, useCallback } from "react";
import { Clock, Zap, Sparkles, Send, BarChart3, Camera } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { useAddTaskUpdate, useSubmitTask } from "@/hooks/use-tasks";
import { toast } from "sonner";
import AppShell from "@/components/layout/AppShell";
import { StatusLegend } from "@/components/shared/StatusIndicator";
import MissionCompletionScreen from "@/components/mission/MissionCompletionScreen";
import FocusTaskCard, { type FocusTask } from "@/components/mission/FocusTaskCard";
import TaskFeedCard from "@/components/mission/TaskFeedCard";
import QuickImpactFeed, { type MicroMission } from "@/components/mission/QuickImpactFeed";

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

const mockMicroMissions: MicroMission[] = [
  { id: "mm1", title: "Verify garden bed cleanup", type: "photo_verify", estimatedMins: 2, points: 15, distance: "0.1 km", isOnline: false },
  { id: "mm2", title: "Confirm tool shed location", type: "location_tag", estimatedMins: 1, points: 10, distance: "0.3 km", isOnline: false },
  { id: "mm3", title: "Rate volunteer experience", type: "quick_survey", estimatedMins: 2, points: 10, isOnline: true },
  { id: "mm4", title: "Photograph recycling station", type: "photo_verify", estimatedMins: 3, points: 20, distance: "0.5 km", isOnline: false },
];

const FEED_PAGE_SIZE = 5;

export default function ActiveWorkspace() {
  const [tasks, setTasks] = useState(mockMission.tasks);
  const [showCompletion, setShowCompletion] = useState(false);
  const [animateProgress, setAnimateProgress] = useState(false);
  const [successFlash, setSuccessFlash] = useState<string | null>(null);
  const [microPoints, setMicroPoints] = useState(0);
  const [feedLimit, setFeedLimit] = useState(FEED_PAGE_SIZE);
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

  const focusTask = tasks.find((t) => t.status === "in_progress" && t.isMine)
    || tasks.find((t) => t.status === "in_progress")
    || tasks.find((t) => t.status === "open" && t.isMine)
    || tasks.find((t) => t.status === "open");

  const statusOrder: Record<TaskStatus, number> = { in_progress: 0, open: 1, submitted: 2, validated: 3 };
  const allFeedTasks = tasks
    .filter((t) => t.id !== focusTask?.id)
    .sort((a, b) => statusOrder[a.status] - statusOrder[b.status]);
  const feedTasks = allFeedTasks.slice(0, feedLimit);
  const hasMore = allFeedTasks.length > feedLimit;

  // ── Animations ──
  const triggerProgress = useCallback(() => {
    setAnimateProgress(true);
    setTimeout(() => setAnimateProgress(false), 600);
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

      {/* Success flash */}
      {successFlash && (
        <div className="fixed top-0 left-0 right-0 z-50 pointer-events-none flex justify-center pt-4 animate-slide-up">
          <div className="bg-primary text-primary-foreground px-6 py-3 rounded-2xl shadow-float flex items-center gap-2 text-sm font-semibold">
            <Sparkles className="h-4 w-4" /> {successFlash}
          </div>
        </div>
      )}

      {/* ═══ Mission Header ═══ */}
      <div className="px-4 pt-4 pb-3">
        <div className="flex items-center justify-between gap-3 mb-3">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-1.5 mb-1">
              <Zap className="h-3.5 w-3.5 text-secondary shrink-0" />
              <span className="text-[10px] font-semibold uppercase tracking-wider text-secondary">Active Mission</span>
            </div>
            <h1 className="font-display text-base font-bold leading-tight truncate">{mockMission.title}</h1>
          </div>
          <Badge className="bg-card text-foreground text-[11px] gap-1.5 shrink-0 font-mono border-0 shadow-card rounded-xl px-3 py-1.5">
            <Clock className="h-3 w-3 text-secondary" />
            {hours > 0 ? `${hours}h ` : ""}{mins}m
          </Badge>
        </div>

        {/* Progress bar */}
        <div className="flex items-center justify-between mb-2">
          <span className="text-[11px] text-muted-foreground">{completedCount}/{totalCount} done</span>
          <span className={`font-display font-bold text-xs transition-all duration-200 ${animateProgress ? "text-primary scale-110" : "text-primary"}`}>
            {progressPct}%
          </span>
        </div>
        <div className={`transition-all duration-200 ${animateProgress ? "animate-task-complete" : ""}`}>
          <div className="h-2.5 w-full overflow-hidden rounded-full bg-muted/80">
            <div
              className="h-full bg-gradient-to-r from-primary to-primary/70 transition-all duration-500 ease-out rounded-full"
              style={{ width: `${progressPct}%` }}
            />
          </div>
        </div>
        <div className="mt-2"><StatusLegend /></div>
      </div>

      {/* ═══ Quick Impact ═══ */}
      <QuickImpactFeed
        missions={mockMicroMissions}
        limit={2}
        onComplete={(id) => {
          const m = mockMicroMissions.find((mm) => mm.id === id);
          if (m) setMicroPoints((p) => p + m.points);
          triggerProgress();
        }}
      />

      {/* ═══ Focus Task ═══ */}
      <div id="focus-task" className="px-4 mb-4">
        {focusTask ? (
          <FocusTaskCard
            task={focusTask}
            remaining={allFeedTasks.filter((t) => t.status === "open" || t.status === "in_progress").length}
            onAddProgress={handleAddProgress}
            onSubmit={handleSubmit}
            onRelease={handleRelease}
            onPhotoUpload={handlePhotoUpload}
          />
        ) : (
          <div className="bg-primary/5 rounded-2xl p-6 text-center animate-slide-up shadow-card">
            <Sparkles className="h-7 w-7 text-primary mx-auto mb-2" />
            <p className="font-semibold text-primary text-sm">All tasks completed!</p>
          </div>
        )}
      </div>

      {/* ═══ Task Queue ═══ */}
      {allFeedTasks.length > 0 && (
        <div className="px-4 pb-4">
          <div className="flex items-center gap-2 mb-3">
            <h2 className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Task Queue</h2>
            <span className="text-[10px] text-muted-foreground bg-muted px-2 py-0.5 rounded-full">{allFeedTasks.length}</span>
          </div>
          <div className="space-y-2">
            {feedTasks.map((task, i) => (
              <TaskFeedCard key={task.id} task={task} index={i} onClaim={handleClaim} />
            ))}
          </div>
          {hasMore && (
            <button
              onClick={() => setFeedLimit((l) => l + FEED_PAGE_SIZE)}
              className="w-full py-3 mt-2 text-xs font-medium text-primary hover:text-primary/80 transition-colors duration-200"
            >
              Load {Math.min(allFeedTasks.length - feedLimit, FEED_PAGE_SIZE)} more tasks
            </button>
          )}
        </div>
      )}
    </AppShell>
  );
}
