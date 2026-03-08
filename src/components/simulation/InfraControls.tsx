import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Slider } from "@/components/ui/slider";
import { Badge } from "@/components/ui/badge";
import { Settings } from "lucide-react";
import type { InfraParams } from "@/lib/load-simulation-engine";

interface InfraControlsProps {
  infra: InfraParams;
  onChange: (infra: InfraParams) => void;
}

function Param({ label, value, unit, min, max, step, onChange }: {
  label: string; value: number; unit: string; min: number; max: number; step: number;
  onChange: (v: number) => void;
}) {
  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <span className="text-xs text-muted-foreground font-medium">{label}</span>
        <Badge variant="outline" className="text-xs font-mono">{value} {unit}</Badge>
      </div>
      <Slider
        min={min} max={max} step={step}
        value={[value]}
        onValueChange={([v]) => onChange(v)}
      />
    </div>
  );
}

export default function InfraControls({ infra, onChange }: InfraControlsProps) {
  const set = (key: keyof InfraParams) => (v: number) =>
    onChange({ ...infra, [key]: v });

  return (
    <Card className="border-primary/30 bg-primary/5">
      <CardHeader className="pb-3">
        <CardTitle className="text-sm flex items-center gap-2 text-primary">
          <Settings className="h-4 w-4" /> Infrastructure Parameters
        </CardTitle>
        <p className="text-xs text-muted-foreground">
          Adjust to see cascading effects on queue, latency, and errors
        </p>
      </CardHeader>
      <CardContent className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <Param label="API Servers" value={infra.api_servers} unit="nodes" min={1} max={16} step={1} onChange={set("api_servers")} />
        <Param label="Background Workers" value={infra.bg_workers} unit="workers" min={1} max={50} step={1} onChange={set("bg_workers")} />
        <Param label="Events / Worker / Sec" value={infra.events_per_worker_per_second} unit="evt/s" min={1} max={30} step={1} onChange={set("events_per_worker_per_second")} />
        <Param label="Base Latency" value={infra.base_latency} unit="ms" min={5} max={100} step={5} onChange={set("base_latency")} />
      </CardContent>
    </Card>
  );
}
