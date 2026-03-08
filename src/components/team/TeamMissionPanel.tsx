import { Users, Loader2, UserPlus, LogOut, Share2, Copy, MessageCircle } from "lucide-react";
import { Progress } from "@/components/ui/progress";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { toast } from "sonner";
import { useTeamMembers, useJoinTeam, useLeaveTeam } from "@/hooks/use-tasks";
import { useState } from "react";

interface TeamMissionPanelProps {
  taskId: string;
  teamTarget: number;
  isTeam: boolean;
}

export default function TeamMissionPanel({ taskId, teamTarget, isTeam }: TeamMissionPanelProps) {
  const { data, isLoading } = useTeamMembers(taskId);
  const joinMutation = useJoinTeam();
  const leaveMutation = useLeaveTeam();
  const [showRoster, setShowRoster] = useState(false);
  const [showInvite, setShowInvite] = useState(false);

  if (!isTeam) return null;

  const members = data?.members ?? [];
  const memberCount = data?.member_count ?? 0;
  const target = data?.team_target ?? teamTarget;
  const progressPct = target > 0 ? Math.min(100, Math.round((memberCount / target) * 100)) : 0;
  const isFull = target > 0 && memberCount >= target;

  const isMock = taskId.startsWith("demo-");

  const handleJoin = async () => {
    if (isMock) {
      toast.success("You joined the team! 🎉");
      return;
    }
    try {
      await joinMutation.mutateAsync(taskId);
      toast.success("You joined the team! 🎉");
    } catch (e: any) {
      toast.error(e.message || "Failed to join");
    }
  };

  const handleLeave = async () => {
    if (isMock) {
      toast.success("Left the team");
      return;
    }
    try {
      await leaveMutation.mutateAsync(taskId);
      toast.success("Left the team");
    } catch (e: any) {
      toast.error(e.message || "Failed to leave");
    }
  };

  const handleShareWhatsApp = () => {
    const url = `${window.location.origin}/task/${taskId}?join=team`;
    const text = `Join my team mission! ${url}`;
    window.open(`https://wa.me/?text=${encodeURIComponent(text)}`, "_blank");
  };

  const handleShareSMS = () => {
    const url = `${window.location.origin}/task/${taskId}?join=team`;
    const text = `Join my team mission! ${url}`;
    window.open(`sms:?body=${encodeURIComponent(text)}`, "_blank");
  };

  const handleCopyLink = () => {
    const url = `${window.location.origin}/task/${taskId}?join=team`;
    navigator.clipboard.writeText(url).then(() => toast.success("Link copied!"));
  };

  if (isLoading) {
    return (
      <div className="bg-accent/5 rounded-2xl border border-accent/10 p-4 flex justify-center">
        <Loader2 className="h-5 w-5 animate-spin text-accent" />
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {/* Team Progress Card */}
      <div className="bg-accent/5 rounded-2xl border border-accent/10 p-4">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Users className="h-4 w-4 text-accent" />
            <span className="font-display font-semibold text-sm">Team Mission</span>
          </div>
          <Badge className={`text-[10px] border-0 ${isFull ? "bg-primary/10 text-primary" : "bg-accent/10 text-accent"}`}>
            {isFull ? "Team Full" : `${memberCount} / ${target}`}
          </Badge>
        </div>

        <div className="mb-2">
          <div className="flex justify-between text-[10px] text-muted-foreground mb-1">
            <span>Team Progress</span>
            <span className="font-display font-bold">{progressPct}%</span>
          </div>
          <Progress value={progressPct} className="h-2.5" />
        </div>

        <p className="text-[10px] text-muted-foreground">
          {memberCount} / {target} volunteers joined
        </p>
      </div>

      {/* Actions */}
      <div className="flex gap-2">
        <Button
          onClick={handleJoin}
          disabled={joinMutation.isPending || isFull}
          size="sm"
          className="flex-1 rounded-xl gap-1.5"
        >
          {joinMutation.isPending ? <Loader2 className="h-3 w-3 animate-spin" /> : <UserPlus className="h-3.5 w-3.5" />}
          Join Team
        </Button>
        <Button
          onClick={() => setShowInvite(!showInvite)}
          variant="outline"
          size="sm"
          className="rounded-xl gap-1.5"
        >
          <Share2 className="h-3.5 w-3.5" /> Invite
        </Button>
      </div>

      {/* Invite Options */}
      {showInvite && (
        <div className="bg-card rounded-xl border border-border p-3 space-y-2">
          <p className="text-xs font-semibold text-muted-foreground">Invite Teammates</p>
          <div className="flex gap-2">
            <Button onClick={handleShareWhatsApp} variant="outline" size="sm" className="flex-1 rounded-lg text-xs gap-1.5">
              <MessageCircle className="h-3.5 w-3.5 text-primary" /> WhatsApp
            </Button>
            <Button onClick={handleShareSMS} variant="outline" size="sm" className="flex-1 rounded-lg text-xs gap-1.5">
              💬 SMS
            </Button>
            <Button onClick={handleCopyLink} variant="outline" size="sm" className="flex-1 rounded-lg text-xs gap-1.5">
              <Copy className="h-3.5 w-3.5" /> Copy
            </Button>
          </div>
        </div>
      )}

      {/* Team Roster */}
      <button
        onClick={() => setShowRoster(!showRoster)}
        className="w-full text-xs text-primary font-medium text-center py-1"
      >
        {showRoster ? "Hide Roster" : "View Team Roster"}
      </button>

      {showRoster && (
        <div className="bg-card rounded-xl border border-border p-3 space-y-2">
          {members.length === 0 ? (
            <p className="text-xs text-muted-foreground text-center py-2">No members yet. Be the first!</p>
          ) : (
            members.map((m: any) => (
              <div key={m.id} className="flex items-center gap-3 py-1.5">
                <div className="h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center text-sm shrink-0">
                  {m.profile?.avatar_url ? (
                    <img src={m.profile.avatar_url} className="h-8 w-8 rounded-full object-cover" />
                  ) : (
                    "🧑"
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate">{m.profile?.display_name || "Volunteer"}</p>
                  <p className="text-[10px] text-muted-foreground">
                    Joined {new Date(m.joined_at).toLocaleDateString()}
                  </p>
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}
