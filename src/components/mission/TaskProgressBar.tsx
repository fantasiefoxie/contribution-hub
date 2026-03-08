import { Progress } from "@/components/ui/progress";

interface TaskProgressBarProps {
  updatesCount: number;
  totalSteps?: number;
}

export default function TaskProgressBar({ updatesCount, totalSteps = 6 }: TaskProgressBarProps) {
  const pct = Math.min(100, Math.round((updatesCount / totalSteps) * 100));

  return (
    <div className="mx-4 mb-4 bg-card rounded-2xl border border-border p-4">
      <div className="flex items-center justify-between mb-2">
        <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">Progress</span>
        <span className="font-display font-bold text-sm text-primary">{pct}%</span>
      </div>
      <Progress value={pct} className="h-2.5" />
      <p className="text-[10px] text-muted-foreground mt-2">{updatesCount} of {totalSteps} steps completed</p>
    </div>
  );
}
