import { useNavigate } from "react-router-dom";
import { MapPin, Clock, Flame, ArrowRight, Zap } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { useReserveTask } from "@/hooks/use-tasks";
import { toast } from "sonner";

export interface TaskCardData {
  id: string;
  title: string;
  organization?: string;
  difficulty: number;
  distance?: string;
  impact?: string;
  tags?: string[];
  status: string;
  estimatedTime?: string;
}

interface TaskCardProps {
  task: TaskCardData;
}

function DifficultyDots({ level }: { level: number }) {
  return (
    <div className="flex gap-0.5">
      {Array.from({ length: 5 }).map((_, i) => (
        <div
          key={i}
          className={`h-1.5 w-1.5 rounded-full ${
            i < level ? "bg-secondary" : "bg-muted"
          }`}
        />
      ))}
    </div>
  );
}

export default function TaskCard({ task }: TaskCardProps) {
  const navigate = useNavigate();
  const reserveMutation = useReserveTask();

  const handleQuickClaim = async (e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await reserveMutation.mutateAsync(task.id);
      toast.success("Task reserved! Tap to confirm claim.", {
        action: {
          label: "View",
          onClick: () => navigate(`/task/${task.id}`),
        },
      });
    } catch (err: any) {
      toast.error(err.message || "Could not reserve task");
    }
  };

  return (
    <button
      onClick={() => navigate(`/task/${task.id}`)}
      className="w-full text-left bg-card rounded-2xl border border-border p-4 shadow-sm hover:shadow-md hover:border-primary/20 transition-all duration-200 animate-slide-up group"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex-1 min-w-0">
          <h3 className="font-display font-semibold text-card-foreground text-[15px] leading-tight truncate">
            {task.title}
          </h3>
          {task.organization && (
            <p className="text-xs text-muted-foreground mt-0.5">{task.organization}</p>
          )}
        </div>
        <div className="shrink-0 h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center group-hover:bg-primary/20 transition-colors">
          <ArrowRight className="h-4 w-4 text-primary" />
        </div>
      </div>

      <div className="flex items-center gap-3 mt-3 text-xs text-muted-foreground">
        <span className="flex items-center gap-1">
          <Flame className="h-3 w-3 text-secondary" />
          <DifficultyDots level={task.difficulty} />
        </span>
        {task.distance && (
          <span className="flex items-center gap-1">
            <MapPin className="h-3 w-3" />
            {task.distance}
          </span>
        )}
        {task.estimatedTime && (
          <span className="flex items-center gap-1">
            <Clock className="h-3 w-3" />
            {task.estimatedTime}
          </span>
        )}
      </div>

      {task.impact && (
        <div className="mt-3">
          <Badge variant="secondary" className="text-[11px] font-medium bg-primary/10 text-primary border-0">
            {task.impact}
          </Badge>
        </div>
      )}

      <div className="flex items-center justify-between mt-3">
        {task.tags && task.tags.length > 0 && (
          <div className="flex gap-1.5 flex-wrap">
            {task.tags.slice(0, 3).map((tag) => (
              <span key={tag} className="text-[10px] px-2 py-0.5 rounded-full bg-muted text-muted-foreground">
                {tag}
              </span>
            ))}
          </div>
        )}
        {task.status === "open" && (
          <Button
            size="sm"
            onClick={handleQuickClaim}
            disabled={reserveMutation.isPending}
            className="rounded-lg text-[11px] h-7 px-3 bg-primary hover:bg-primary/90 gap-1 ml-auto shrink-0"
          >
            <Zap className="h-3 w-3" />
            Quick Claim
          </Button>
        )}
      </div>
    </button>
  );
}
