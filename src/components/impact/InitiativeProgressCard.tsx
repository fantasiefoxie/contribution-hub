import { useNavigate } from "react-router-dom";
import { ChevronRight } from "lucide-react";
import { Progress } from "@/components/ui/progress";

interface InitiativeProgressProps {
  title: string;
  slug?: string;
  progress: Record<string, { current: number; target: number; percentage: number }>;
  milestones: { percentage: number; reached_at: string }[];
}

const metricLabels: Record<string, string> = {
  households_reached: "Households Surveyed",
  trees_planted: "Trees Planted",
  waste_removed_kg: "Waste Removed (kg)",
  people_served: "People Served",
};

export default function InitiativeProgressCard({ title, slug, progress, milestones }: InitiativeProgressProps) {
  const navigate = useNavigate();
  const entries = Object.entries(progress);
  if (entries.length === 0) return null;

  const reachedSet = new Set(milestones.map((m) => m.percentage));

  const handleClick = () => {
    if (slug) navigate(`/initiative/${slug}`);
  };

  return (
    <button
      onClick={handleClick}
      className="w-full text-left bg-card rounded-2xl border border-border p-4 hover:border-primary/20 transition-colors active:scale-[0.99] group"
    >
      <div className="flex items-center justify-between mb-3">
        <p className="font-display font-semibold text-sm">{title}</p>
        <ChevronRight className="h-4 w-4 text-muted-foreground group-hover:text-primary transition-colors" />
      </div>
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
    </button>
  );
}
