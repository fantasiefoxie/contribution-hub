import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Clock, Flame, MapPin, Building2, Bookmark, Loader2, Users, Wifi } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { useTask, useReserveTask, useConfirmClaim } from "@/hooks/use-tasks";
import { useState, useEffect, useMemo } from "react";
import { toast } from "sonner";
import AppShell from "@/components/layout/AppShell";
import SocialSignals from "@/components/mission/SocialSignals";
import TeamMissionPanel from "@/components/team/TeamMissionPanel";
import { getMockTask, getMockOrg, getMockInitiative, getMockTaskImpact, getMockEstTime, getMockDistance } from "@/lib/mock-data";

function DifficultyBar({ level }: { level: number }) {
  return (
    <div className="flex gap-1">
      {Array.from({ length: 5 }).map((_, i) => (
        <div
          key={i}
          className={`h-2 flex-1 rounded-full ${i < level ? "bg-secondary" : "bg-muted"}`}
        />
      ))}
    </div>
  );
}

export default function TaskDetails() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data, isLoading } = useTask(id ?? "", { retry: false });
  const reserveMutation = useReserveTask();
  const claimMutation = useConfirmClaim();
  const [reservationTimer, setReservationTimer] = useState<number | null>(null);
  const [isReserved, setIsReserved] = useState(false);

  const task = data?.task;

  // Derive contextual data from mock lookups
  const ctx = useMemo(() => {
    const t = task ?? getMockTask(id ?? "");
    const org = getMockOrg(t?.org_id ?? null);
    const initiative = getMockInitiative(t?.initiative_id ?? null);
    const impact = getMockTaskImpact(id ?? "");
    const estTime = getMockEstTime(id ?? "");
    const distance = getMockDistance(id ?? "");
    const isOnline = !t?.latitude && !t?.longitude;
    return { org, initiative, impact, estTime, distance, isOnline };
  }, [task, id]);

  useEffect(() => {
    if (reservationTimer !== null && reservationTimer > 0) {
      const t = setTimeout(() => setReservationTimer((v) => (v ?? 1) - 1), 1000);
      return () => clearTimeout(t);
    }
    if (reservationTimer === 0) {
      setIsReserved(false);
      setReservationTimer(null);
      toast.error("Reservation expired");
    }
  }, [reservationTimer]);

  const isMock = id?.startsWith("demo-") ?? false;

  const handleReserve = async () => {
    if (!id) return;
    if (isMock) {
      setIsReserved(true);
      setReservationTimer(60);
      toast.success("Task reserved! Confirm within 60 seconds.");
      return;
    }
    try {
      await reserveMutation.mutateAsync(id);
      setIsReserved(true);
      setReservationTimer(60);
      toast.success("Task reserved! Confirm within 60 seconds.");
    } catch (e: any) {
      toast.error(e.message || "Failed to reserve");
    }
  };

  const handleConfirmClaim = async () => {
    if (!id) return;
    if (isMock) {
      setIsReserved(false);
      setReservationTimer(null);
      toast.success("Task claimed! Head to Active workspace.");
      navigate("/active");
      return;
    }
    try {
      await claimMutation.mutateAsync(id);
      setIsReserved(false);
      setReservationTimer(null);
      toast.success("Task claimed! Head to Active workspace.");
      navigate("/active");
    } catch (e: any) {
      toast.error(e.message || "Failed to claim");
    }
  };

  const t = task ?? {
    title: "Loading...",
    description: "",
    difficulty: 1,
    status: "open",
    tags: [],
    lock_duration_mins: 60,
    created_at: new Date().toISOString(),
    is_team: false,
    team_target: null,
  };

  if (isLoading) {
    return (
      <AppShell>
        <div className="flex items-center justify-center h-64">
          <Loader2 className="h-6 w-6 animate-spin text-primary" />
        </div>
      </AppShell>
    );
  }

  return (
    <AppShell>
      {/* Header */}
      <div className="sticky top-0 z-10 bg-background/95 backdrop-blur-sm border-b border-border">
        <div className="flex items-center gap-3 px-4 h-14">
          <button onClick={() => navigate(-1)} className="h-8 w-8 rounded-full bg-muted flex items-center justify-center">
            <ArrowLeft className="h-4 w-4" />
          </button>
          <span className="font-display font-semibold text-sm truncate">Task Details</span>
          {t.is_team && (
            <Badge className="bg-accent/10 text-accent border-0 text-[10px] ml-auto">
              <Users className="h-3 w-3 mr-1" /> Team
            </Badge>
          )}
        </div>
      </div>

      <div className="px-4 py-5 space-y-5">
        {/* Title + Org */}
        <div>
          <h1 className="font-display text-xl font-bold leading-tight">{t.title}</h1>
          <div className="flex items-center gap-2 mt-2 text-sm text-muted-foreground">
            <Building2 className="h-3.5 w-3.5" />
            <span>{ctx.org?.name ?? "Community Project"}</span>
          </div>
          {ctx.initiative && (
            <p className="text-xs text-muted-foreground mt-1">Part of <span className="font-medium text-foreground">{ctx.initiative.title}</span></p>
          )}
        </div>

        {/* Meta cards */}
        <div className="grid grid-cols-3 gap-2">
          <div className="bg-muted rounded-xl p-3 text-center">
            <Flame className="h-4 w-4 text-secondary mx-auto mb-1" />
            <p className="text-[10px] text-muted-foreground">Difficulty</p>
            <p className="font-display font-semibold text-sm">{t.difficulty}/5</p>
          </div>
          <div className="bg-muted rounded-xl p-3 text-center">
            <Clock className="h-4 w-4 text-accent mx-auto mb-1" />
            <p className="text-[10px] text-muted-foreground">Est. Time</p>
            <p className="font-display font-semibold text-sm">{ctx.estTime}</p>
          </div>
          <div className="bg-muted rounded-xl p-3 text-center">
            {ctx.isOnline ? (
              <>
                <Wifi className="h-4 w-4 text-primary mx-auto mb-1" />
                <p className="text-[10px] text-muted-foreground">Location</p>
                <p className="font-display font-semibold text-sm">Online</p>
              </>
            ) : (
              <>
                <MapPin className="h-4 w-4 text-primary mx-auto mb-1" />
                <p className="text-[10px] text-muted-foreground">Distance</p>
                <p className="font-display font-semibold text-sm">{ctx.distance ?? "—"}</p>
              </>
            )}
          </div>
        </div>

        {/* Difficulty bar */}
        <div>
          <p className="text-xs text-muted-foreground mb-1.5">Difficulty Level</p>
          <DifficultyBar level={t.difficulty} />
        </div>

        {/* Team Mission Panel */}
        {t.is_team && id && (
          <TeamMissionPanel
            taskId={id}
            teamTarget={t.team_target || 5}
            isTeam={t.is_team}
          />
        )}

        {/* Impact */}
        <div className="bg-primary/5 rounded-xl p-4 border border-primary/10">
          <p className="text-xs text-muted-foreground mb-1">Impact</p>
          <p className="font-display font-semibold text-primary text-lg">🌱 50kg waste removed</p>
        </div>

        {/* Description */}
        <div>
          <h2 className="font-display font-semibold text-sm mb-2">Description</h2>
          <p className="text-sm text-muted-foreground leading-relaxed">
            {t.description || "No description provided."}
          </p>
        </div>

        {/* Tags */}
        {t.tags && t.tags.length > 0 && (
          <div className="flex gap-2 flex-wrap">
            {t.tags.map((tag: string) => (
              <Badge key={tag} variant="outline" className="text-xs">
                {tag}
              </Badge>
            ))}
          </div>
        )}

        {/* Social Signals */}
        <SocialSignals volunteersJoined={42} tasksInProgress={7} />

        {/* Status */}
        <div className="bg-card rounded-xl border border-border p-4">
          <div className="flex items-center justify-between">
            <span className="text-xs text-muted-foreground">Status</span>
            <Badge
              className={`text-[11px] ${
                t.status === "open"
                  ? "bg-primary/10 text-primary border-0"
                  : "bg-muted text-muted-foreground border-0"
              }`}
            >
              {t.status.toUpperCase()}
            </Badge>
          </div>
        </div>
      </div>

      {/* Bottom action buttons */}
      <div className="sticky bottom-20 px-4 pb-4">
        <div className="bg-card/95 backdrop-blur-sm rounded-2xl border border-border p-3 shadow-lg space-y-2">
          {isReserved ? (
            <>
              <div className="text-center text-xs text-muted-foreground mb-1">
                Reserved — confirm within{" "}
                <span className="font-mono font-bold text-secondary">{reservationTimer}s</span>
              </div>
              <Button
                onClick={handleConfirmClaim}
                disabled={claimMutation.isPending}
                className="w-full h-12 rounded-xl font-display font-semibold text-base bg-primary hover:bg-primary/90"
              >
                {claimMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : "Confirm Claim"}
              </Button>
            </>
          ) : (
            <div className="flex gap-2">
              <Button
                onClick={handleReserve}
                disabled={reserveMutation.isPending || t.status !== "open"}
                className="flex-1 h-12 rounded-xl font-display font-semibold text-base bg-primary hover:bg-primary/90"
              >
                {reserveMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  "Claim Task"
                )}
              </Button>
              <Button
                variant="outline"
                className="h-12 w-12 rounded-xl shrink-0"
              >
                <Bookmark className="h-5 w-5" />
              </Button>
            </div>
          )}
        </div>
      </div>
    </AppShell>
  );
}
