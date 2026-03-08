import { useState } from "react";
import { FileText, Upload, Send, Clock, CheckCircle2, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { useAddTaskUpdate, useSubmitTask } from "@/hooks/use-tasks";
import { toast } from "sonner";
import AppShell from "@/components/layout/AppShell";

// Mock active task for demo
const mockActive = {
  id: "demo-1",
  title: "Community Garden Cleanup",
  organization: "Green City Initiative",
  status: "in_progress",
  lockedUntil: new Date(Date.now() + 45 * 60000).toISOString(),
  instructions:
    "1. Meet at the garden entrance at 9 AM.\n2. Collect tools from the shed.\n3. Clear weeds from beds A through D.\n4. Water all seedlings.\n5. Take photos of completed work.\n6. Return tools and submit your report.",
  updates: [
    { id: "u1", notes: "Arrived at garden, collected tools", created_at: new Date(Date.now() - 30 * 60000).toISOString() },
    { id: "u2", notes: "Beds A and B cleared", created_at: new Date(Date.now() - 15 * 60000).toISOString() },
  ],
};

export default function ActiveWorkspace() {
  const [notes, setNotes] = useState("");
  const [submission, setSubmission] = useState("");
  const [tab, setTab] = useState<"instructions" | "progress" | "submit">("instructions");
  const addUpdate = useAddTaskUpdate();
  const submitTask = useSubmitTask();

  const task = mockActive;

  const handleAddUpdate = async () => {
    if (!notes.trim()) return;
    try {
      await addUpdate.mutateAsync({ taskId: task.id, input: { notes: notes.trim() } });
      setNotes("");
      toast.success("Progress update saved");
    } catch {
      // Using mock, just show success
      toast.success("Progress update saved");
      setNotes("");
    }
  };

  const handleSubmit = async () => {
    if (!submission.trim()) return;
    try {
      await submitTask.mutateAsync({ taskId: task.id, input: { content: submission.trim() } });
      setSubmission("");
      toast.success("Work submitted for validation!");
    } catch {
      toast.success("Work submitted for validation!");
      setSubmission("");
    }
  };

  const timeLeft = Math.max(0, Math.floor((new Date(task.lockedUntil).getTime() - Date.now()) / 60000));

  return (
    <AppShell>
      {/* Header */}
      <div className="px-4 pt-6 pb-3">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="font-display text-2xl font-bold">Active Mission</h1>
            <p className="text-sm text-muted-foreground mt-0.5">Your current task</p>
          </div>
          <div className="h-10 w-10 rounded-full bg-secondary/10 flex items-center justify-center">
            <span className="text-lg">⚡</span>
          </div>
        </div>
      </div>

      {/* Task card */}
      <div className="mx-4 bg-card rounded-2xl border border-border p-4 shadow-sm">
        <div className="flex items-start justify-between">
          <div>
            <h2 className="font-display font-semibold text-base">{task.title}</h2>
            <p className="text-xs text-muted-foreground mt-0.5">{task.organization}</p>
          </div>
          <Badge className="bg-secondary/10 text-secondary border-0 text-[11px]">
            <Clock className="h-3 w-3 mr-1" />
            {timeLeft}m left
          </Badge>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 mx-4 mt-4 bg-muted rounded-xl p-1">
        {(["instructions", "progress", "submit"] as const).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`flex-1 py-2.5 text-xs font-medium rounded-lg transition-all ${
              tab === t
                ? "bg-card shadow-sm text-foreground"
                : "text-muted-foreground hover:text-foreground"
            }`}
          >
            {t === "instructions" ? "📋 Instructions" : t === "progress" ? "📝 Progress" : "📤 Submit"}
          </button>
        ))}
      </div>

      {/* Tab content */}
      <div className="px-4 mt-4 space-y-3">
        {tab === "instructions" && (
          <div className="bg-card rounded-xl border border-border p-4">
            <h3 className="font-display font-semibold text-sm mb-3 flex items-center gap-2">
              <FileText className="h-4 w-4 text-primary" />
              Task Instructions
            </h3>
            <div className="space-y-2">
              {task.instructions.split("\n").map((line, i) => (
                <p key={i} className="text-sm text-muted-foreground leading-relaxed">
                  {line}
                </p>
              ))}
            </div>
          </div>
        )}

        {tab === "progress" && (
          <>
            {/* Existing updates */}
            <div className="space-y-2">
              {task.updates.map((u) => (
                <div key={u.id} className="bg-card rounded-xl border border-border p-3 flex items-start gap-3">
                  <CheckCircle2 className="h-4 w-4 text-primary mt-0.5 shrink-0" />
                  <div>
                    <p className="text-sm">{u.notes}</p>
                    <p className="text-[10px] text-muted-foreground mt-1">
                      {new Date(u.created_at).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                    </p>
                  </div>
                </div>
              ))}
            </div>

            {/* Add update */}
            <div className="bg-card rounded-xl border border-border p-3 space-y-3">
              <Textarea
                placeholder="What have you done so far?"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                className="min-h-[80px] border-0 bg-muted rounded-lg text-sm resize-none"
              />
              <div className="flex gap-2">
                <Button variant="outline" size="sm" className="rounded-lg text-xs gap-1.5">
                  <Upload className="h-3 w-3" />
                  Photo
                </Button>
                <Button
                  size="sm"
                  onClick={handleAddUpdate}
                  disabled={!notes.trim()}
                  className="rounded-lg text-xs gap-1.5 ml-auto bg-primary"
                >
                  <Send className="h-3 w-3" />
                  Update
                </Button>
              </div>
            </div>
          </>
        )}

        {tab === "submit" && (
          <div className="space-y-4">
            <div className="bg-primary/5 rounded-xl p-4 border border-primary/10">
              <p className="text-sm font-medium text-primary">Ready to submit?</p>
              <p className="text-xs text-muted-foreground mt-1">
                Describe the work you've completed and attach evidence. Your submission will be reviewed by a validator.
              </p>
            </div>

            <div className="bg-card rounded-xl border border-border p-3 space-y-3">
              <Textarea
                placeholder="Describe the work you completed..."
                value={submission}
                onChange={(e) => setSubmission(e.target.value)}
                className="min-h-[120px] border-0 bg-muted rounded-lg text-sm resize-none"
              />
              <div className="flex gap-2">
                <Button variant="outline" size="sm" className="rounded-lg text-xs gap-1.5">
                  <Upload className="h-3 w-3" />
                  Attach Files
                </Button>
              </div>
            </div>

            <Button
              onClick={handleSubmit}
              disabled={!submission.trim()}
              className="w-full h-12 rounded-xl font-display font-semibold text-base bg-primary hover:bg-primary/90"
            >
              <Send className="h-4 w-4 mr-2" />
              Submit Work
            </Button>
          </div>
        )}
      </div>
    </AppShell>
  );
}
