import { Sparkles, MapPin, AlertTriangle, Globe, Loader2, ChevronRight } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Badge } from "@/components/ui/badge";

interface ScoredTask {
  id: string;
  title: string;
  description: string | null;
  difficulty: number;
  tags: string[];
  is_priority: boolean;
  _score: number;
  _distance: number | null;
  _reasons: string[];
  expires_at: string | null;
}

interface FeedSectionProps {
  title: string;
  icon: React.ReactNode;
  tasks: ScoredTask[];
  emptyMessage?: string;
}

const difficultyLabels = ["", "Easy", "Moderate", "Advanced", "Organizer"];
const difficultyColors = ["", "text-primary", "text-secondary", "text-accent", "text-destructive"];

function TaskRow({ task }: { task: ScoredTask }) {
  const navigate = useNavigate();
  return (
    <button
      onClick={() => navigate(`/task/${task.id}`)}
      className="w-full bg-card rounded-2xl border border-border p-4 text-left hover:border-primary/30 transition-colors"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <p className="font-display font-semibold text-sm truncate">{task.title}</p>
            {task.is_priority && (
              <Badge className="bg-destructive/10 text-destructive border-0 text-[10px] shrink-0">Priority</Badge>
            )}
          </div>
          {task.description && (
            <p className="text-xs text-muted-foreground mt-1 line-clamp-1">{task.description}</p>
          )}
          <div className="flex items-center gap-2 mt-2 flex-wrap">
            <span className={`text-[10px] font-medium ${difficultyColors[task.difficulty] || "text-muted-foreground"}`}>
              {difficultyLabels[task.difficulty] || `Lvl ${task.difficulty}`}
            </span>
            {task._distance !== null && (
              <span className="text-[10px] text-muted-foreground flex items-center gap-0.5">
                <MapPin className="h-2.5 w-2.5" />
                {task._distance < 1 ? `${Math.round(task._distance * 1000)}m` : `${task._distance.toFixed(1)}km`}
              </span>
            )}
            {task.tags?.slice(0, 3).map(tag => (
              <Badge key={tag} variant="secondary" className="text-[10px] px-1.5 py-0">{tag}</Badge>
            ))}
            {task._reasons.includes("urgent") && (
              <span className="text-[10px] text-destructive font-medium flex items-center gap-0.5">
                <AlertTriangle className="h-2.5 w-2.5" /> Urgent
              </span>
            )}
          </div>
        </div>
        <div className="flex flex-col items-end gap-1 shrink-0">
          <span className="text-[10px] text-muted-foreground bg-muted px-2 py-0.5 rounded-full">
            Score {task._score}
          </span>
          <ChevronRight className="h-4 w-4 text-muted-foreground" />
        </div>
      </div>
    </button>
  );
}

export default function FeedSection({ title, icon, tasks, emptyMessage }: FeedSectionProps) {
  if (!tasks.length && emptyMessage) {
    return (
      <div className="px-4 mb-4">
        <div className="flex items-center gap-2 mb-2">
          {icon}
          <h2 className="font-display font-semibold text-sm">{title}</h2>
        </div>
        <p className="text-xs text-muted-foreground text-center py-6">{emptyMessage}</p>
      </div>
    );
  }

  if (!tasks.length) return null;

  return (
    <div className="px-4 mb-5">
      <div className="flex items-center gap-2 mb-3">
        {icon}
        <h2 className="font-display font-semibold text-sm">{title}</h2>
        <span className="text-[10px] text-muted-foreground ml-auto">{tasks.length} tasks</span>
      </div>
      <div className="space-y-2">
        {tasks.map(task => <TaskRow key={task.id} task={task} />)}
      </div>
    </div>
  );
}
