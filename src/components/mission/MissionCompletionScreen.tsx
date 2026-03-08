import { useNavigate } from "react-router-dom";
import { PartyPopper, ArrowRight, TrendingUp } from "lucide-react";
import { Button } from "@/components/ui/button";

interface MissionCompletionProps {
  taskTitle: string;
  impactMetrics: Record<string, number>;
  initiativeTitle?: string;
  initiativeProgress?: number;
  nextMission?: { id: string; title: string; estimatedTime: string; distance?: string };
  onDismiss: () => void;
}

export default function MissionCompletionScreen({
  taskTitle,
  impactMetrics,
  initiativeTitle,
  initiativeProgress,
  nextMission,
  onDismiss,
}: MissionCompletionProps) {
  const navigate = useNavigate();

  const metricLines = Object.entries(impactMetrics)
    .filter(([k]) => k !== "difficulty")
    .map(([k, v]) => ({ label: k.replace(/_/g, " "), value: v }));

  return (
    <div className="fixed inset-0 z-50 bg-background/95 backdrop-blur-sm flex items-center justify-center p-6">
      <div className="max-w-sm w-full text-center space-y-6">
        {/* Celebration */}
        <div className="space-y-3">
          <div className="h-20 w-20 rounded-full bg-primary/10 flex items-center justify-center mx-auto">
            <PartyPopper className="h-10 w-10 text-primary" />
          </div>
          <h1 className="font-display text-2xl font-bold">Mission Completed 🎉</h1>
          <p className="text-sm text-muted-foreground">{taskTitle}</p>
        </div>

        {/* Impact Metrics */}
        {metricLines.length > 0 && (
          <div className="bg-card rounded-2xl border border-border p-4 space-y-3">
            {metricLines.map(({ label, value }) => (
              <div key={label} className="flex items-center justify-between">
                <span className="text-sm text-muted-foreground capitalize">{label}</span>
                <span className="font-display font-bold text-primary text-lg">{value}</span>
              </div>
            ))}
          </div>
        )}

        {/* Initiative Progress */}
        {initiativeTitle && initiativeProgress !== undefined && (
          <div className="bg-primary/5 rounded-xl border border-primary/10 p-4">
            <div className="flex items-center gap-2 justify-center mb-1">
              <TrendingUp className="h-4 w-4 text-primary" />
              <span className="text-xs font-semibold text-primary">Initiative Progress</span>
            </div>
            <p className="text-sm">{initiativeTitle}</p>
            <p className="font-display font-bold text-lg text-primary mt-1">{initiativeProgress}% complete</p>
          </div>
        )}

        {/* Next Mission */}
        {nextMission && (
          <div className="bg-card rounded-2xl border border-border p-4">
            <p className="text-[10px] uppercase tracking-wider text-muted-foreground font-medium mb-2">Next Mission Near You</p>
            <p className="font-display font-semibold text-sm">{nextMission.title}</p>
            <p className="text-xs text-muted-foreground mt-1">
              Est. {nextMission.estimatedTime}{nextMission.distance ? ` · ${nextMission.distance}` : ""}
            </p>
            <Button
              onClick={() => { onDismiss(); navigate(`/task/${nextMission.id}`); }}
              className="w-full mt-3 h-10 rounded-xl font-display font-semibold text-sm bg-primary hover:bg-primary/90"
            >
              View Mission <ArrowRight className="h-4 w-4 ml-1" />
            </Button>
          </div>
        )}

        <Button
          variant="ghost"
          onClick={onDismiss}
          className="text-muted-foreground text-sm"
        >
          Back to Home
        </Button>
      </div>
    </div>
  );
}
