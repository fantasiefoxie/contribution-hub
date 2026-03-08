import { useState } from "react";
import { Camera, MapPin, CheckSquare, Clock, Zap, ChevronRight, Sparkles, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { toast } from "sonner";

// ── Types ──
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

// ── Inline completion overlay ──
function MicroMissionOverlay({
  mission,
  onComplete,
  onDismiss,
}: {
  mission: MicroMission;
  onComplete: () => void;
  onDismiss: () => void;
}) {
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

        {/* Simplified single-action area */}
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

        <Button
          onClick={onComplete}
          className="w-full h-14 rounded-2xl text-base font-semibold gap-2 bg-primary hover:bg-primary/90"
        >
          <Zap className="h-5 w-5" />
          {cfg.action}
        </Button>
      </div>
    </div>
  );
}

// ── Card ──
function MicroMissionCard({
  mission,
  onDoNow,
}: {
  mission: MicroMission;
  onDoNow: (m: MicroMission) => void;
}) {
  const cfg = typeConfig[mission.type];
  const Icon = cfg.icon;

  return (
    <div className="min-w-[200px] bg-card rounded-xl border border-border p-3 flex flex-col gap-2 shrink-0 snap-start">
      <div className="flex items-center gap-2">
        <div className={`h-7 w-7 rounded-lg ${cfg.bg} flex items-center justify-center`}>
          <Icon className={`h-3.5 w-3.5 ${cfg.color}`} />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-xs font-semibold text-foreground truncate">{mission.title}</p>
          <div className="flex items-center gap-1.5 mt-0.5">
            <Clock className="h-2.5 w-2.5 text-muted-foreground" />
            <span className="text-[9px] text-muted-foreground">{mission.estimatedMins} min</span>
            {mission.distance && (
              <>
                <MapPin className="h-2.5 w-2.5 text-muted-foreground" />
                <span className="text-[9px] text-muted-foreground">{mission.distance}</span>
              </>
            )}
          </div>
        </div>
      </div>
      <div className="flex items-center justify-between">
        <span className="text-[10px] font-semibold text-primary">+{mission.points} pts</span>
        <Button
          size="sm"
          onClick={() => onDoNow(mission)}
          className="h-7 px-3 rounded-lg text-[10px] font-semibold gap-1 bg-primary hover:bg-primary/90"
        >
          Do Now <ChevronRight className="h-3 w-3" />
        </Button>
      </div>
    </div>
  );
}

// ── Feed Section ──
interface QuickImpactFeedProps {
  missions: MicroMission[];
  onComplete: (missionId: string) => void;
}

export default function QuickImpactFeed({ missions, onComplete }: QuickImpactFeedProps) {
  const [activeMission, setActiveMission] = useState<MicroMission | null>(null);
  const [completedIds, setCompletedIds] = useState<Set<string>>(new Set());

  const available = missions.filter((m) => !completedIds.has(m.id));

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
      <div className="px-4 mb-3">
        <div className="flex items-center gap-2 mb-2">
          <Sparkles className="h-3.5 w-3.5 text-secondary" />
          <h2 className="text-xs font-semibold uppercase tracking-wider text-secondary">Quick Impact</h2>
          <span className="text-[9px] text-muted-foreground bg-muted px-1.5 py-0.5 rounded">{available.length}</span>
        </div>
        <div className="flex gap-2.5 overflow-x-auto pb-1 -mx-1 px-1 snap-x snap-mandatory scrollbar-hide">
          {available.map((m) => (
            <MicroMissionCard key={m.id} mission={m} onDoNow={setActiveMission} />
          ))}
        </div>
      </div>

      {activeMission && (
        <MicroMissionOverlay
          mission={activeMission}
          onComplete={handleComplete}
          onDismiss={() => setActiveMission(null)}
        />
      )}
    </>
  );
}
