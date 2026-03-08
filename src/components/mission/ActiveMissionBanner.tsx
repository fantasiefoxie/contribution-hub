import { useNavigate } from "react-router-dom";
import { Clock, ChevronRight, Zap } from "lucide-react";
import { Progress } from "@/components/ui/progress";
import { useState, useEffect } from "react";

interface ActiveMissionBannerProps {
  taskId: string;
  title: string;
  lockedUntil: string;
  updatesCount: number;
  totalSteps?: number;
}

export default function ActiveMissionBanner({ taskId, title, lockedUntil, updatesCount, totalSteps = 6 }: ActiveMissionBannerProps) {
  const navigate = useNavigate();
  const [now, setNow] = useState(Date.now());

  useEffect(() => {
    const interval = setInterval(() => setNow(Date.now()), 60000);
    return () => clearInterval(interval);
  }, []);

  const msLeft = Math.max(0, new Date(lockedUntil).getTime() - now);
  const hours = Math.floor(msLeft / 3600000);
  const mins = Math.floor((msLeft % 3600000) / 60000);
  const progressPct = Math.min(100, Math.round((updatesCount / totalSteps) * 100));

  if (msLeft <= 0) return null;

  return (
    <button
      onClick={() => navigate(`/active`)}
      className="mx-4 mb-3 bg-gradient-to-r from-secondary/15 via-secondary/10 to-primary/10 rounded-2xl p-3.5 border border-secondary/20 flex items-center gap-3 hover:border-secondary/40 transition-all"
    >
      <div className="h-10 w-10 rounded-full bg-secondary/20 flex items-center justify-center shrink-0">
        <Zap className="h-5 w-5 text-secondary" />
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-1.5">
          <p className="text-[10px] uppercase tracking-wider font-semibold text-secondary">Active Mission</p>
        </div>
        <p className="font-display font-semibold text-sm truncate mt-0.5">{title}</p>
        <div className="flex items-center gap-3 mt-1.5">
          <div className="flex items-center gap-1 text-[10px] text-muted-foreground">
            <Clock className="h-3 w-3" />
            <span>{hours}h {mins}m left</span>
          </div>
          <div className="flex-1 max-w-[80px]">
            <Progress value={progressPct} className="h-1.5" />
          </div>
          <span className="text-[10px] font-medium text-muted-foreground">{progressPct}%</span>
        </div>
      </div>
      <ChevronRight className="h-4 w-4 text-muted-foreground shrink-0" />
    </button>
  );
}
