import { useState } from "react";
import { Send, Camera, Unlock, BarChart3, CheckCircle2, ChevronDown, ChevronUp, MapPin, Wifi } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { StatusDot, StatusBadge, DifficultyBadge, type UniversalStatus } from "@/components/shared/StatusIndicator";

export interface FocusTask {
  id: string;
  title: string;
  difficulty: number;
  status: "open" | "in_progress" | "submitted" | "validated";
  isMine: boolean;
  isOnline: boolean;
  distance?: string;
}

const toUniversal = (s: FocusTask["status"]): UniversalStatus =>
  s === "open" ? "not_started" : s === "in_progress" ? "in_progress" : s === "submitted" ? "submitted" : "validated";

interface Props {
  task: FocusTask;
  remaining: number;
  onAddProgress: (taskId: string, notes: string) => void;
  onSubmit: (taskId: string, notes: string) => void;
  onRelease: (taskId: string) => void;
  onPhotoUpload: (taskId: string) => void;
  isSubmitting?: boolean;
}

export default function FocusTaskCard({ task, remaining, onAddProgress, onSubmit, onRelease, onPhotoUpload, isSubmitting }: Props) {
  const [expanded, setExpanded] = useState(true);
  const [notes, setNotes] = useState("");
  const uStatus = toUniversal(task.status);

  const handleProgress = () => {
    if (!notes.trim()) return;
    onAddProgress(task.id, notes.trim());
    setNotes("");
  };

  const handleSubmit = () => {
    onSubmit(task.id, notes.trim() || "Task completed");
    setNotes("");
  };

  return (
    <div className="bg-card rounded-2xl shadow-elevated overflow-hidden animate-slide-up">
      {/* Label */}
      <div className="px-4 pt-3 pb-2 flex items-center justify-between">
        <span className="text-[10px] font-semibold uppercase tracking-wider text-primary">Current Task</span>
        <span className="text-[10px] text-muted-foreground">{remaining} more in queue</span>
      </div>

      {/* Header */}
      <button onClick={() => setExpanded(!expanded)} className="w-full flex items-center gap-3 px-4 pb-3 text-left">
        <StatusDot status={uStatus} />
        <div className="flex-1 min-w-0">
          <p className="text-[15px] font-semibold leading-snug text-foreground">{task.title}</p>
          <div className="flex items-center gap-2 mt-1.5">
            <DifficultyBadge level={task.difficulty} />
            <StatusBadge status={uStatus} />
            {task.isOnline && (
              <span className="flex items-center gap-1 text-[10px] text-accent"><Wifi className="h-3 w-3" /> Online</span>
            )}
            {task.distance && !task.isOnline && (
              <span className="flex items-center gap-1 text-[10px] text-muted-foreground"><MapPin className="h-3 w-3" /> {task.distance}</span>
            )}
          </div>
        </div>
        {expanded ? <ChevronUp className="h-4 w-4 text-muted-foreground shrink-0" /> : <ChevronDown className="h-4 w-4 text-muted-foreground shrink-0" />}
      </button>

      {/* Inline update panel */}
      {expanded && task.status !== "validated" && (
        <div className="px-4 pb-4 space-y-3 animate-slide-up">
          <div className="border-t border-border" />

          {task.status !== "submitted" && (
            <Textarea
              placeholder="Quick progress note..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="min-h-[52px] border-0 bg-muted/60 rounded-xl text-sm resize-none focus-visible:ring-1 focus-visible:ring-primary/30"
            />
          )}

          {/* Action buttons — large tap targets */}
          <div className="grid grid-cols-3 gap-2">
            {task.status !== "submitted" && (
              <Button
                onClick={handleProgress}
                disabled={!notes.trim()}
                variant="outline"
                className="h-12 rounded-xl text-xs gap-1 border-accent/30 text-accent hover:bg-accent/5 flex-col py-1 transition-all duration-200 active:scale-95"
              >
                <BarChart3 className="h-4 w-4" />
                Progress
              </Button>
            )}
            {(task.status === "open" || task.status === "in_progress") && (
              <Button
                onClick={handleSubmit}
                disabled={isSubmitting}
                className="h-12 rounded-xl text-xs gap-1 bg-primary hover:bg-primary/90 flex-col py-1 shadow-card transition-all duration-200 active:scale-95"
              >
                <Send className="h-4 w-4" />
                Submit
              </Button>
            )}
            <Button
              onClick={() => onPhotoUpload(task.id)}
              variant="outline"
              className="h-12 rounded-xl text-xs gap-1 border-secondary/30 text-secondary hover:bg-secondary/5 flex-col py-1 transition-all duration-200 active:scale-95"
            >
              <Camera className="h-4 w-4" />
              Photo
            </Button>
          </div>

          {task.isMine && task.status !== "submitted" && (
            <button onClick={() => onRelease(task.id)} className="flex items-center gap-1.5 text-[11px] text-muted-foreground hover:text-destructive transition-colors duration-200 mx-auto">
              <Unlock className="h-3 w-3" /> Release this task
            </button>
          )}
        </div>
      )}

      {expanded && task.status === "validated" && (
        <div className="px-4 pb-4">
          <div className="flex items-center gap-2 text-sm text-primary justify-center py-3 bg-primary/5 rounded-xl">
            <CheckCircle2 className="h-5 w-5" />
            <span className="font-semibold">Validated — great work!</span>
          </div>
        </div>
      )}
    </div>
  );
}
