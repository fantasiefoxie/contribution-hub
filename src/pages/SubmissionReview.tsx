import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Check, X, MessageCircle, Loader2, User, Clock, Flame, FileText, AlertTriangle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { toast } from "sonner";
import AppShell from "@/components/layout/AppShell";
import { useApproveSubmission, useRejectSubmission, useRequestClarification } from "@/hooks/use-validations";

// Mock data for display
const mockReview = {
  id: "sub-1",
  content: "Completed garden cleanup. Removed debris from 3 plots and planted 15 seedlings. Watered all beds and organized tools in the shed.",
  submitted_at: new Date(Date.now() - 3600000).toISOString(),
  attachments: [
    { type: "image", url: "/placeholder.svg", name: "before_cleanup.jpg" },
    { type: "image", url: "/placeholder.svg", name: "after_cleanup.jpg" },
  ],
  contributor: { display_name: "Maria Santos", avatar_url: null },
  tasks: {
    id: "t-1",
    title: "Community Garden Cleanup",
    description: "Help maintain our community garden by weeding, watering, and planting new seedlings.",
    difficulty: 2,
    tags: ["environment", "outdoor"],
  },
  updates: [
    { id: "u-1", notes: "Started clearing plot A. Lots of overgrowth.", created_at: new Date(Date.now() - 7200000).toISOString() },
    { id: "u-2", notes: "Plot A done. Moving to B and C now.", created_at: new Date(Date.now() - 5400000).toISOString() },
  ],
};

type ActionType = "approve" | "reject" | "clarify" | null;

export default function SubmissionReview() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [action, setAction] = useState<ActionType>(null);
  const [feedback, setFeedback] = useState("");

  const approveMutation = useApproveSubmission();
  const rejectMutation = useRejectSubmission();
  const clarifyMutation = useRequestClarification();

  const sub = mockReview; // Will be replaced with API data when auth is wired

  const handleSubmitAction = async () => {
    if (!id) return;
    try {
      if (action === "approve") {
        await approveMutation.mutateAsync({ submissionId: id, input: { feedback: feedback || undefined } });
        toast.success("Submission approved! Contribution recorded.");
      } else if (action === "reject") {
        if (!feedback.trim()) { toast.error("Feedback is required when rejecting"); return; }
        await rejectMutation.mutateAsync({ submissionId: id, input: { feedback } });
        toast.success("Submission rejected. Contributor notified.");
      } else if (action === "clarify") {
        if (!feedback.trim()) { toast.error("Please provide your question"); return; }
        await clarifyMutation.mutateAsync({ submissionId: id, input: { feedback } });
        toast.success("Clarification requested.");
      }
      navigate("/validate");
    } catch (e: any) {
      toast.error(e.message || "Action failed");
    }
  };

  const isPending = approveMutation.isPending || rejectMutation.isPending || clarifyMutation.isPending;

  return (
    <AppShell>
      {/* Header */}
      <div className="sticky top-0 z-10 bg-background/95 backdrop-blur-sm border-b border-border">
        <div className="flex items-center gap-3 px-4 h-14">
          <button onClick={() => navigate("/validate")} className="h-8 w-8 rounded-full bg-muted flex items-center justify-center">
            <ArrowLeft className="h-4 w-4" />
          </button>
          <span className="font-display font-semibold text-sm">Review Submission</span>
        </div>
      </div>

      <div className="px-4 py-5 space-y-5 pb-48">
        {/* Task Info */}
        <div className="bg-card rounded-2xl border border-border p-4">
          <p className="text-[10px] uppercase tracking-wider text-muted-foreground font-medium mb-1">Task</p>
          <h2 className="font-display font-bold text-lg">{sub.tasks.title}</h2>
          <p className="text-sm text-muted-foreground mt-1">{sub.tasks.description}</p>
          <div className="flex items-center gap-3 mt-3">
            <div className="flex items-center gap-1 text-xs text-muted-foreground">
              <Flame className="h-3 w-3 text-secondary" />
              <span>Difficulty {sub.tasks.difficulty}/5</span>
            </div>
            {sub.tasks.tags.map((tag) => (
              <Badge key={tag} variant="secondary" className="text-[10px]">{tag}</Badge>
            ))}
          </div>
        </div>

        {/* Contributor */}
        <div className="flex items-center gap-3 bg-muted/50 rounded-xl p-3">
          <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
            <User className="h-5 w-5 text-primary" />
          </div>
          <div>
            <p className="font-display font-semibold text-sm">{sub.contributor.display_name}</p>
            <div className="flex items-center gap-1 text-xs text-muted-foreground">
              <Clock className="h-3 w-3" />
              <span>Submitted {new Date(sub.submitted_at).toLocaleString()}</span>
            </div>
          </div>
        </div>

        {/* Submission Content */}
        <div>
          <p className="text-[10px] uppercase tracking-wider text-muted-foreground font-medium mb-2">Submission</p>
          <div className="bg-card rounded-xl border border-border p-4">
            <p className="text-sm leading-relaxed">{sub.content}</p>
          </div>
        </div>

        {/* Evidence/Attachments */}
        {sub.attachments && sub.attachments.length > 0 && (
          <div>
            <p className="text-[10px] uppercase tracking-wider text-muted-foreground font-medium mb-2">Evidence</p>
            <div className="grid grid-cols-2 gap-2">
              {sub.attachments.map((att, i) => (
                <div key={i} className="bg-muted rounded-xl aspect-video flex items-center justify-center border border-border overflow-hidden">
                  <div className="text-center p-3">
                    <FileText className="h-6 w-6 text-muted-foreground mx-auto mb-1" />
                    <p className="text-[10px] text-muted-foreground truncate">{att.name}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Progress Updates History */}
        <div>
          <p className="text-[10px] uppercase tracking-wider text-muted-foreground font-medium mb-2">Progress Updates</p>
          <div className="space-y-2">
            {sub.updates.map((u) => (
              <div key={u.id} className="bg-muted/50 rounded-xl p-3 border border-border/50">
                <p className="text-sm">{u.notes}</p>
                <p className="text-[10px] text-muted-foreground mt-1">{new Date(u.created_at).toLocaleString()}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Bottom Actions */}
      <div className="fixed bottom-20 left-0 right-0 px-4 pb-4">
        <div className="bg-card/95 backdrop-blur-sm rounded-2xl border border-border p-4 shadow-lg max-w-lg mx-auto">
          {action ? (
            <div className="space-y-3">
              <div className="flex items-center gap-2 mb-1">
                {action === "approve" && <Check className="h-4 w-4 text-primary" />}
                {action === "reject" && <X className="h-4 w-4 text-destructive" />}
                {action === "clarify" && <MessageCircle className="h-4 w-4 text-accent" />}
                <span className="font-display font-semibold text-sm capitalize">{action === "clarify" ? "Request Clarification" : action}</span>
                <button onClick={() => { setAction(null); setFeedback(""); }} className="ml-auto text-xs text-muted-foreground underline">
                  Cancel
                </button>
              </div>
              <Textarea
                placeholder={
                  action === "approve" ? "Optional feedback for the contributor..."
                    : action === "reject" ? "Explain why this was rejected (required)..."
                    : "What do you need clarified? (required)..."
                }
                value={feedback}
                onChange={(e) => setFeedback(e.target.value)}
                className="min-h-[80px] text-sm rounded-xl"
              />
              <Button
                onClick={handleSubmitAction}
                disabled={isPending}
                className={`w-full h-11 rounded-xl font-display font-semibold ${
                  action === "reject" ? "bg-destructive hover:bg-destructive/90" :
                  action === "clarify" ? "bg-accent hover:bg-accent/90 text-accent-foreground" :
                  "bg-primary hover:bg-primary/90"
                }`}
              >
                {isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : `Confirm ${action === "clarify" ? "Request" : action}`}
              </Button>
            </div>
          ) : (
            <div className="flex gap-2">
              <Button
                onClick={() => setAction("approve")}
                className="flex-1 h-11 rounded-xl font-display font-semibold bg-primary hover:bg-primary/90"
              >
                <Check className="h-4 w-4 mr-1" /> Approve
              </Button>
              <Button
                onClick={() => setAction("reject")}
                variant="destructive"
                className="flex-1 h-11 rounded-xl font-display font-semibold"
              >
                <X className="h-4 w-4 mr-1" /> Reject
              </Button>
              <Button
                onClick={() => setAction("clarify")}
                variant="outline"
                className="h-11 rounded-xl px-3"
              >
                <MessageCircle className="h-4 w-4" />
              </Button>
            </div>
          )}
        </div>
      </div>
    </AppShell>
  );
}
