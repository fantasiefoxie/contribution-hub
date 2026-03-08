import type { LucideIcon } from "lucide-react";
import { Progress } from "@/components/ui/progress";

interface ScreenHeaderProps {
  title: string;
  subtitle?: string;
  icon?: LucideIcon;
  iconColor?: string;
  /** Optional progress (0–100) */
  progress?: number;
  progressLabel?: string;
  rightSlot?: React.ReactNode;
}

/** Consistent compact header used across all screens */
export default function ScreenHeader({
  title,
  subtitle,
  icon: Icon,
  iconColor = "text-primary",
  progress,
  progressLabel,
  rightSlot,
}: ScreenHeaderProps) {
  return (
    <div className="px-4 pt-5 pb-3">
      <div className="flex items-center justify-between gap-3">
        <div className="flex-1 min-w-0">
          <h1 className="font-display text-xl font-bold leading-tight truncate">{title}</h1>
          {subtitle && <p className="text-xs text-muted-foreground mt-0.5">{subtitle}</p>}
        </div>
        {Icon && !rightSlot && (
          <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center shrink-0">
            <Icon className={`h-5 w-5 ${iconColor}`} />
          </div>
        )}
        {rightSlot}
      </div>

      {progress !== undefined && (
        <div className="mt-3 bg-card rounded-xl border border-border p-3">
          <div className="flex items-center justify-between mb-1.5">
            {progressLabel && <span className="text-xs font-medium text-muted-foreground">{progressLabel}</span>}
            <span className="font-display font-bold text-sm text-primary">{progress}%</span>
          </div>
          <Progress value={progress} className="h-2.5" />
        </div>
      )}
    </div>
  );
}
