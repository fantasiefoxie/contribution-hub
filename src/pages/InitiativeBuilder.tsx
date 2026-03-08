import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, Loader2, Sparkles, Calendar, Target } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import AppShell from "@/components/layout/AppShell";
import { useCreateInitiative } from "@/hooks/use-initiatives";

export default function InitiativeBuilder() {
  const navigate = useNavigate();
  const createMutation = useCreateInitiative();

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [isMicro, setIsMicro] = useState(false);

  // Success metrics
  const [metrics, setMetrics] = useState<{ key: string; target: number }[]>([
    { key: "households_reached", target: 0 },
  ]);

  const addMetric = () => {
    setMetrics([...metrics, { key: "", target: 0 }]);
  };

  const updateMetric = (i: number, field: "key" | "target", val: string | number) => {
    const updated = [...metrics];
    if (field === "key") updated[i].key = val as string;
    else updated[i].target = Number(val);
    setMetrics(updated);
  };

  const removeMetric = (i: number) => {
    setMetrics(metrics.filter((_, idx) => idx !== i));
  };

  const handleCreate = async () => {
    if (!title.trim()) { toast.error("Title is required"); return; }

    const target_metrics: Record<string, number> = {};
    for (const m of metrics) {
      if (m.key.trim() && m.target > 0) target_metrics[m.key.trim()] = m.target;
    }

    try {
      const result = await createMutation.mutateAsync({
        title: title.trim(),
        description: description.trim() || undefined,
        start_date: startDate || undefined,
        end_date: endDate || undefined,
        target_metrics,
        is_micro: isMicro,
      });
      toast.success("Initiative created!");
      navigate(`/initiatives/${result.initiative.id}`);
    } catch (e: any) {
      toast.error(e.message || "Failed to create");
    }
  };

  return (
    <AppShell>
      <div className="sticky top-0 z-10 bg-background/95 backdrop-blur-sm border-b border-border">
        <div className="flex items-center gap-3 px-4 h-14">
          <button onClick={() => navigate(-1)} className="h-8 w-8 rounded-full bg-muted flex items-center justify-center">
            <ArrowLeft className="h-4 w-4" />
          </button>
          <span className="font-display font-semibold text-sm">Create Initiative</span>
        </div>
      </div>

      <div className="px-4 py-5 space-y-5 pb-32">
        {/* Micro toggle */}
        <div className="bg-accent/5 rounded-xl border border-accent/10 p-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Sparkles className="h-5 w-5 text-accent" />
            <div>
              <p className="font-display font-semibold text-sm">Micro-Initiative</p>
              <p className="text-[10px] text-muted-foreground">Max 10 tasks · 25 volunteers · 30 days</p>
            </div>
          </div>
          <Switch checked={isMicro} onCheckedChange={setIsMicro} />
        </div>

        {/* Title */}
        <div className="space-y-2">
          <Label className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">Title</Label>
          <Input
            placeholder="e.g. River Cleanup Campaign"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="rounded-xl"
          />
        </div>

        {/* Description */}
        <div className="space-y-2">
          <Label className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">Description</Label>
          <Textarea
            placeholder="Describe the initiative goals and scope..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="rounded-xl min-h-[100px]"
          />
        </div>

        {/* Timeline */}
        <div className="space-y-2">
          <Label className="text-xs font-semibold text-muted-foreground uppercase tracking-wide flex items-center gap-1.5">
            <Calendar className="h-3.5 w-3.5" /> Timeline
          </Label>
          <div className="grid grid-cols-2 gap-2">
            <div>
              <p className="text-[10px] text-muted-foreground mb-1">Start Date</p>
              <Input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} className="rounded-xl text-sm" />
            </div>
            <div>
              <p className="text-[10px] text-muted-foreground mb-1">End Date</p>
              <Input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} className="rounded-xl text-sm" />
            </div>
          </div>
          {isMicro && endDate && startDate && (
            (() => {
              const days = Math.ceil((new Date(endDate).getTime() - new Date(startDate).getTime()) / 86400000);
              return days > 30 ? (
                <p className="text-[10px] text-destructive">Micro-initiatives cannot exceed 30 days ({days} days selected)</p>
              ) : null;
            })()
          )}
        </div>

        {/* Success Metrics */}
        <div className="space-y-3">
          <Label className="text-xs font-semibold text-muted-foreground uppercase tracking-wide flex items-center gap-1.5">
            <Target className="h-3.5 w-3.5" /> Success Metrics
          </Label>
          {metrics.map((m, i) => (
            <div key={i} className="flex gap-2 items-end">
              <div className="flex-1">
                <p className="text-[10px] text-muted-foreground mb-1">Metric Name</p>
                <Input
                  placeholder="e.g. households_reached"
                  value={m.key}
                  onChange={(e) => updateMetric(i, "key", e.target.value)}
                  className="rounded-xl text-sm"
                />
              </div>
              <div className="w-24">
                <p className="text-[10px] text-muted-foreground mb-1">Target</p>
                <Input
                  type="number"
                  placeholder="1000"
                  value={m.target || ""}
                  onChange={(e) => updateMetric(i, "target", e.target.value)}
                  className="rounded-xl text-sm"
                />
              </div>
              {metrics.length > 1 && (
                <button onClick={() => removeMetric(i)} className="text-destructive text-xs pb-2.5">✕</button>
              )}
            </div>
          ))}
          <button onClick={addMetric} className="text-xs text-primary font-medium">+ Add Metric</button>
        </div>
      </div>

      {/* Create button */}
      <div className="fixed bottom-20 left-0 right-0 px-4 pb-4">
        <div className="max-w-lg mx-auto">
          <Button
            onClick={handleCreate}
            disabled={createMutation.isPending || !title.trim()}
            className="w-full h-12 rounded-xl font-display font-semibold text-base bg-primary hover:bg-primary/90"
          >
            {createMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : "Create Initiative"}
          </Button>
        </div>
      </div>
    </AppShell>
  );
}
