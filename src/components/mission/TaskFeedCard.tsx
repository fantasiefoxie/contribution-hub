import { MapPin, Wifi, Zap } from "lucide-react";
import { Button } from "@/components/ui/button";
import { StatusDot, DifficultyBadge, type UniversalStatus } from "@/components/shared/StatusIndicator";

export interface FeedTask {
  id: string;
  title: string;
  difficulty: number;
  status: "open" | "in_progress" | "submitted" | "validated";
  isMine: boolean;
  isOnline: boolean;
  distance?: string;
}

const toUniversal = (s: FeedTask["status"]): UniversalStatus =>
  s === "open" ? "not_started" : s === "in_progress" ? "in_progress" : s === "submitted" ? "submitted" : "validated";

interface Props {
  task: FeedTask;
  index: number;
  onClaim: (taskId: string) => void;
}

export default function TaskFeedCard({ task, index, onClaim }: Props) {
  const uStatus = toUniversal(task.status);
  const canClaim = task.status === "open" && !task.isMine;

  return (
    <div
      className="bg-card rounded-xl border border-border p-3.5 flex items-center gap-3 animate-fade-in"
      style={{ animationDelay: `${index * 50}ms` }}
    >
      <StatusDot status={uStatus} />
      <div className="flex-1 min-w-0">
        <p className={`text-sm font-medium leading-snug ${task.status === "validated" ? "line-through text-muted-foreground" : "text-foreground"}`}>
          {task.title}
        </p>
        <div className="flex items-center gap-2 mt-0.5">
          <DifficultyBadge level={task.difficulty} />
          {task.isOnline && (
            <span className="flex items-center gap-0.5 text-[9px] text-accent"><Wifi className="h-2.5 w-2.5" /> Online</span>
          )}
          {task.distance && !task.isOnline && (
            <span className="flex items-center gap-0.5 text-[9px] text-muted-foreground"><MapPin className="h-2.5 w-2.5" /> {task.distance}</span>
          )}
        </div>
      </div>

      {canClaim && (
        <Button
          size="sm"
          onClick={() => onClaim(task.id)}
          className="h-9 px-3 rounded-xl text-[11px] font-semibold gap-1 bg-accent hover:bg-accent/90 text-accent-foreground shrink-0"
        >
          <Zap className="h-3 w-3" /> Claim
        </Button>
      )}

      {task.isMine && task.status !== "validated" && (
        <span className="text-[9px] font-medium text-primary bg-primary/10 px-2 py-0.5 rounded-full shrink-0">Yours</span>
      )}

      {task.status === "validated" && (
        <span className="text-[9px] font-medium text-primary">✓</span>
      )}
    </div>
  );
}
