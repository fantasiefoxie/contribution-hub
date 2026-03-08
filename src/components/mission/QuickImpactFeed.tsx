import { useState } from "react";
import { Camera, MapPin, CheckSquare, Clock, Zap, ChevronRight, Sparkles, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { toast } from "sonner";

export interface MicroMission {
  id: string;
  title: string;
  type: "photo_verify" | "location_tag" | "quick_survey";
  estimatedMins: number;
  points: number;
  distance?: string;
  isOnline: boolean;
}

const typeConfig: Record<MicroMission["type"], { icon: typeof Camera; label: string; action: string; color: string; bg: string }> = {
  photo_verify: { icon: Camera, label: "Photo", action: "Take Photo", color: "text-secondary", bg: "bg-secondary/10" },
  location_tag: { icon: MapPin, label: "Location", action: "Confirm Location", color: "text-accent", bg: "bg-accent/10" },
  quick_survey: { icon: CheckSquare, label: "Survey", action: "Answer Now", color: "text-primary", bg: "bg-primary/10" },
};

function MicroMissionOverlay({ mission, onComplete, onDismiss }: { mission: MicroMission; onComplete: () => void; onDismiss: () => void }) {
  const cfg = typeConfig[mission.type];
  const Icon = cfg.icon;

  return (
    <div className="fixed inset-0 z-50 bg-background/80 backdrop-blur-sm flex items-end justify-center animate-fade-in">
      <div className="w-full max-w-lg bg-card border-t border-border rounded-t-3xl p-5 pb-8 space-y-4 animate-fade-in">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className={`h-8 w-8 rounded-lg ${cfg.bg} flex items-center justify-center`}>
              <Icon className={`h-4 w-4 ${cfg.color}`} />
            </div>
            <div>
              <p className="text-sm font-semibold text-foreground">{mission.title}</p>
              <p className="text-[10px] text-muted-foreground">{cfg.label} · {mission.estimatedMins} min</p>
            </div>
          </div>
          <button onClick={onDismiss} className="h-8 w-8 rounded-full bg-muted flex items-center justify-center">
            <X className="h-4 w-4 text-muted-foreground" />
          </button>
        </div>

        <div className="bg-muted rounded-2xl p-6 flex flex-col items-center gap-3">
          <div className={`h-16 w-16 rounded-2xl ${cfg.bg} flex items-center justify-center`}>
            <Icon className={`h-8 w-8 ${cfg.color}`} />
          </div>
          <p className="text-sm text-muted-foreground text-center">
            {mission.type === "photo_verify" && "Take a photo to verify this location or item"}
            {mission.type === "location_tag" && "Confirm you are at the correct location"}
            {mission.type === "quick_survey" && "Answer a quick yes/no question"}
          </p>
        </div>

        <Button onClick={onComplete} className="w-full h-14 rounded-2xl text-base font-semibold gap-2 bg-primary hover:bg-primary/90">
          <Zap className="h-5 w-5" /> {cfg.action}
        </Button>
      </div>
    </div>
  );
}

/** Inline micro-mission card — no horizontal scroll, stacks vertically */
function MicroMissionCard({ mission, onDoNow }: { mission: MicroMission; onDoNow: (m: MicroMission) => void }) {
  const cfg = typeConfig[mission.type];
  const Icon = cfg.icon;

  return (
    <div className="flex items-center gap-3 py-2.5">
      <div className={`h-9 w-9 rounded-lg ${cfg.bg} flex items-center justify-center shrink-0`}>
        <Icon className={`h-4 w-4 ${cfg.color}`} />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-foreground truncate">{mission.title}</p>
        <div className="flex items-center gap-2 mt-0.5">
          <span className="flex items-center gap-0.5 text-[10px] text-muted-foreground"><Clock className="h-2.5 w-2.5" />{mission.estimatedMins}m</span>
          <span className="text-[10px] font-semibold text-primary">+{mission.points} pts</span>
        </div>
      </div>
      <Button size="sm" onClick={() => onDoNow(mission)} className="h-8 px-3 rounded-lg text-[10px] font-semibold gap-1 bg-primary hover:bg-primary/90 shrink-0">
        Do Now <ChevronRight className="h-3 w-3" />
      </Button>
    </div>
  );
}

interface QuickImpactFeedProps {
  missions: MicroMission[];
  onComplete: (missionId: string) => void;
  /** Max items to show before "show more" */
  limit?: number;
}

export default function QuickImpactFeed({ missions, onComplete, limit = 3 }: QuickImpactFeedProps) {
  const [activeMission, setActiveMission] = useState<MicroMission | null>(null);
  const [completedIds, setCompletedIds] = useState<Set<string>>(new Set());
  const [showAll, setShowAll] = useState(false);

  const available = missions.filter((m) => !completedIds.has(m.id));
  const visible = showAll ? available : available.slice(0, limit);

  const handleComplete = () => {
    if (!activeMission) return;
    setCompletedIds((prev) => new Set(prev).add(activeMission.id));
    onComplete(activeMission.id);
    setActiveMission(null);
    toast.success(`+${activeMission.points} pts earned!`, { description: "Micro-mission completed" });
  };

  if (available.length === 0) return null;

  return (
    <>
      <div className="px-4 mb-2">
        <div className="flex items-center gap-2 mb-1">
          <Sparkles className="h-3.5 w-3.5 text-secondary" />
          <h2 className="text-xs font-semibold uppercase tracking-wider text-secondary">Quick Impact</h2>
          <span className="text-[9px] text-muted-foreground bg-muted px-1.5 py-0.5 rounded">{available.length}</span>
        </div>
        <div className="divide-y divide-border">
          {visible.map((m) => (
            <MicroMissionCard key={m.id} mission={m} onDoNow={setActiveMission} />
          ))}
        </div>
        {!showAll && available.length > limit && (
          <button onClick={() => setShowAll(true)} className="w-full py-2 text-xs font-medium text-primary hover:text-primary/80 transition-colors">
            Show {available.length - limit} more
          </button>
        )}
      </div>

      {activeMission && (
        <MicroMissionOverlay mission={activeMission} onComplete={handleComplete} onDismiss={() => setActiveMission(null)} />
      )}
    </>
  );
}
