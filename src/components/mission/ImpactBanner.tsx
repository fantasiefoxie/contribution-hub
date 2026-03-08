import { Sparkles } from "lucide-react";

export default function ImpactBanner() {
  // Mock community-wide stats for today
  const tasksToday = 47;
  const volunteersActive = 128;

  return (
    <div className="mx-4 mb-2 bg-gradient-to-r from-primary/10 via-primary/5 to-secondary/10 rounded-2xl p-4 border border-primary/10">
      <div className="flex items-center gap-2 mb-2">
        <Sparkles className="h-4 w-4 text-primary" />
        <span className="text-xs font-semibold text-primary uppercase tracking-wide">Impact Today</span>
      </div>
      <div className="flex items-baseline gap-4">
        <div>
          <p className="font-display text-2xl font-bold text-foreground">{tasksToday}</p>
          <p className="text-[10px] text-muted-foreground">tasks completed</p>
        </div>
        <div className="h-8 w-px bg-border" />
        <div>
          <p className="font-display text-2xl font-bold text-foreground">{volunteersActive}</p>
          <p className="text-[10px] text-muted-foreground">volunteers active</p>
        </div>
      </div>
    </div>
  );
}
