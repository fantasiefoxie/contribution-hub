import { Progress } from "@/components/ui/progress";

interface InitiativeProgressProps {
  title: string;
  progress: Record<string, { current: number; target: number; percentage: number }>;
  milestones: { percentage: number; reached_at: string }[];
}

const metricLabels: Record<string, string> = {
  households_reached: "Households Surveyed",
  trees_planted: "Trees Planted",
  waste_removed_kg: "Waste Removed (kg)",
  people_served: "People Served",
};

export default function InitiativeProgressCard({ title, progress, milestones }: InitiativeProgressProps) {
  const entries = Object.entries(progress);
  if (entries.length === 0) return null;

  const reachedSet = new Set(milestones.map((m) => m.percentage));

  return (
    <div className="bg-card rounded-2xl border border-border p-4">
      <p className="font-display font-semibold text-sm mb-3">{title}</p>
      <div className="space-y-4">
        {entries.map(([key, { current, target, percentage }]) => (
          <div key={key}>
            <div className="flex justify-between items-baseline mb-1">
              <span className="text-xs text-muted-foreground">{metricLabels[key] || key.replace(/_/g, " ")}</span>
              <span className="text-xs font-display font-bold">
                {current.toLocaleString()} / {target.toLocaleString()}
              </span>
            </div>
            <Progress value={percentage} className="h-2" />
            {/* Milestone markers */}
            <div className="flex justify-between mt-1.5">
              {[25, 50, 75, 100].map((m) => (
                <div key={m} className="flex flex-col items-center">
                  <div className={`h-2 w-2 rounded-full ${reachedSet.has(m) ? "bg-primary" : percentage >= m ? "bg-primary/40" : "bg-muted"}`} />
                  <span className="text-[9px] text-muted-foreground mt-0.5">{m}%</span>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
