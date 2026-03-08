import { CheckCircle2, Circle } from "lucide-react";

export type UniversalStatus = "not_started" | "in_progress" | "submitted" | "validated";

const statusConfig: Record<UniversalStatus, { dot: string; label: string; badge: string; text: string }> = {
  not_started: {
    dot: "border-muted-foreground/40 bg-transparent",
    label: "Not Started",
    badge: "bg-muted text-muted-foreground",
    text: "text-muted-foreground",
  },
  in_progress: {
    dot: "border-accent bg-accent",
    label: "In Progress",
    badge: "bg-accent/10 text-accent",
    text: "text-accent",
  },
  submitted: {
    dot: "border-secondary bg-secondary",
    label: "Submitted",
    badge: "bg-secondary/10 text-secondary",
    text: "text-secondary",
  },
  validated: {
    dot: "border-primary bg-primary",
    label: "Validated",
    badge: "bg-primary/10 text-primary",
    text: "text-primary",
  },
};

export function getStatusConfig(status: UniversalStatus) {
  return statusConfig[status];
}

/** Small colored dot — 20×20 with optional inner icon */
export function StatusDot({ status, size = "md" }: { status: UniversalStatus; size?: "sm" | "md" }) {
  const cfg = statusConfig[status];
  const sz = size === "sm" ? "h-3 w-3" : "h-5 w-5";
  const iconSz = size === "sm" ? "h-2 w-2" : "h-3 w-3";

  return (
    <div className={`${sz} rounded-full border-2 flex items-center justify-center shrink-0 transition-colors ${cfg.dot}`}>
      {status === "validated" && <CheckCircle2 className={`${iconSz} text-primary-foreground`} />}
      {status === "submitted" && <Circle className={`${size === "sm" ? "h-1 w-1" : "h-2 w-2"} text-secondary-foreground`} />}
    </div>
  );
}

/** Inline badge pill */
export function StatusBadge({ status }: { status: UniversalStatus }) {
  const cfg = statusConfig[status];
  return (
    <span className={`inline-flex items-center px-1.5 py-0 rounded text-[9px] font-medium ${cfg.badge}`}>
      {cfg.label}
    </span>
  );
}

/** Legend row showing all four statuses */
export function StatusLegend() {
  return (
    <div className="flex items-center gap-3">
      {(["not_started", "in_progress", "submitted", "validated"] as UniversalStatus[]).map((s) => (
        <div key={s} className="flex items-center gap-1">
          <StatusDot status={s} size="sm" />
          <span className="text-[9px] text-muted-foreground">{statusConfig[s].label}</span>
        </div>
      ))}
    </div>
  );
}

/** Difficulty label + color */
export function DifficultyBadge({ level }: { level: number }) {
  const label = level <= 1 ? "Easy" : level <= 2 ? "Medium" : level <= 3 ? "Hard" : "Expert";
  const color = level <= 1 ? "text-primary" : level <= 2 ? "text-secondary" : level <= 3 ? "text-destructive" : "text-destructive";
  return <span className={`text-[10px] font-medium ${color}`}>{label}</span>;
}
