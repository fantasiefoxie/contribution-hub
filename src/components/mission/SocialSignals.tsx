import { Users, Zap } from "lucide-react";

interface SocialSignalsProps {
  volunteersJoined: number;
  tasksInProgress: number;
}

export default function SocialSignals({ volunteersJoined, tasksInProgress }: SocialSignalsProps) {
  return (
    <div className="flex gap-3">
      <div className="flex items-center gap-1.5 bg-primary/5 rounded-lg px-3 py-1.5 border border-primary/10">
        <Users className="h-3.5 w-3.5 text-primary" />
        <span className="text-xs font-medium">{volunteersJoined} volunteers joined</span>
      </div>
      <div className="flex items-center gap-1.5 bg-secondary/5 rounded-lg px-3 py-1.5 border border-secondary/10">
        <Zap className="h-3.5 w-3.5 text-secondary" />
        <span className="text-xs font-medium">{tasksInProgress} in progress</span>
      </div>
    </div>
  );
}
