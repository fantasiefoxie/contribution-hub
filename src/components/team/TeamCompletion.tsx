import { Users, PartyPopper, Trophy } from "lucide-react";
import { Button } from "@/components/ui/button";

interface TeamCompletionProps {
  taskTitle: string;
  memberCount: number;
  impactMetrics?: Record<string, number>;
  onClose: () => void;
  onNextMission?: () => void;
}

export default function TeamCompletion({ taskTitle, memberCount, impactMetrics, onClose, onNextMission }: TeamCompletionProps) {
  return (
    <div className="fixed inset-0 z-50 bg-background/95 backdrop-blur-sm flex items-center justify-center p-6">
      <div className="w-full max-w-sm text-center space-y-6">
        {/* Celebration */}
        <div className="space-y-2">
          <div className="text-6xl mb-4">🎉</div>
          <h1 className="font-display text-2xl font-bold">Team Mission Complete!</h1>
          <p className="text-sm text-muted-foreground">{taskTitle}</p>
        </div>

        {/* Team Badge */}
        <div className="bg-accent/10 rounded-2xl border border-accent/10 p-4 inline-flex items-center gap-3 mx-auto">
          <Users className="h-5 w-5 text-accent" />
          <div className="text-left">
            <p className="font-display font-bold text-lg text-accent">{memberCount} volunteers</p>
            <p className="text-[10px] text-muted-foreground">completed this together</p>
          </div>
        </div>

        {/* Impact */}
        {impactMetrics && Object.keys(impactMetrics).length > 0 && (
          <div className="bg-primary/5 rounded-2xl border border-primary/10 p-4 space-y-2">
            <p className="text-xs font-semibold text-primary uppercase tracking-wide flex items-center gap-1.5 justify-center">
              <Trophy className="h-3.5 w-3.5" /> Team Impact
            </p>
            {Object.entries(impactMetrics).map(([key, value]) => (
              <div key={key} className="flex items-center justify-between text-sm">
                <span className="text-muted-foreground capitalize">{key.replace(/_/g, " ")}</span>
                <span className="font-display font-bold">{value.toLocaleString()}</span>
              </div>
            ))}
          </div>
        )}

        <p className="text-xs text-muted-foreground">
          Each team member received equal contribution credit
        </p>

        {/* Actions */}
        <div className="space-y-2">
          {onNextMission && (
            <Button onClick={onNextMission} className="w-full h-12 rounded-xl font-display font-semibold">
              Next Mission →
            </Button>
          )}
          <Button onClick={onClose} variant="outline" className="w-full h-11 rounded-xl">
            Back to Feed
          </Button>
        </div>
      </div>
    </div>
  );
}
