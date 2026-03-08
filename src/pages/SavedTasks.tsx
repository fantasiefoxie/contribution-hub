import { useNavigate } from "react-router-dom";
import { Bookmark, ArrowRight } from "lucide-react";
import AppShell from "@/components/layout/AppShell";
import ScreenHeader from "@/components/shared/ScreenHeader";
import { mockTasks } from "@/lib/mock-data";

const savedTasks = [mockTasks[0], mockTasks[2], mockTasks[4]];

export default function SavedTasks() {
  const navigate = useNavigate();

  return (
    <AppShell>
      <ScreenHeader title="Saved Tasks" subtitle="Your bookmarked tasks" icon={Bookmark} iconColor="text-primary" />

      <div className="px-4 space-y-2">
        {savedTasks.map((task) => (
          <button
            key={task.id}
            onClick={() => navigate(`/task/${task.id}`)}
            className="w-full text-left bg-card rounded-2xl border border-border p-4 flex items-center gap-3 hover:border-primary/20 transition-colors active:scale-[0.99] group"
          >
            <Bookmark className="h-4 w-4 text-primary shrink-0" />
            <div className="flex-1 min-w-0">
              <p className="font-display font-semibold text-sm truncate">{task.title}</p>
              <p className="text-[10px] text-muted-foreground mt-0.5">Difficulty {task.difficulty}/5 · {task.tags?.slice(0, 2).join(", ")}</p>
            </div>
            <ArrowRight className="h-4 w-4 text-muted-foreground group-hover:text-primary transition-colors shrink-0" />
          </button>
        ))}
      </div>

      <div className="h-24" />
    </AppShell>
  );
}
