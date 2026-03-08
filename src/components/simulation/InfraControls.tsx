import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Slider } from "@/components/ui/slider";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Settings, Zap, Building2, Globe, AlertTriangle } from "lucide-react";
import { PRESETS, type InfraParams, type ScenarioConfig, type HealthStatus } from "@/lib/load-simulation-engine";

interface InfraControlsProps {
  infra: InfraParams;
  scenario: ScenarioConfig;
  health: HealthStatus;
  onChange: (infra: InfraParams) => void;
  onScenarioChange: (key: string) => void;
  activePreset: string | null;
}

function Param({ label, value, displayValue, unit, min, max, step, onChange }: {
  label: string; value: number; displayValue?: string; unit: string;
  min: number; max: number; step: number;
  onChange: (v: number) => void;
}) {
  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <span className="text-xs text-muted-foreground font-medium">{label}</span>
        <Badge variant="outline" className="text-xs font-mono">
          {displayValue ?? value} {unit}
        </Badge>
      </div>
      <Slider
        min={min} max={max} step={step}
        value={[value]}
        onValueChange={([v]) => onChange(v)}
      />
    </div>
  );
}

function HealthDot({ status }: { status: "healthy" | "warning" | "critical" }) {
  const colors = {
    healthy: "bg-emerald-500",
    warning: "bg-amber-500 animate-pulse",
    critical: "bg-destructive animate-pulse",
  };
  return <span className={`inline-block w-2.5 h-2.5 rounded-full ${colors[status]}`} />;
}

const presetMeta: { key: string; label: string; icon: typeof Zap }[] = [
  { key: "small_ngo", label: "Small NGO", icon: Building2 },
  { key: "city_survey_10k", label: "City 10K", icon: Zap },
  { key: "city_survey", label: "City 50K", icon: Zap },
  { key: "disaster_response", label: "Disaster Surge", icon: AlertTriangle },
  { key: "national", label: "National 200K", icon: Globe },
  { key: "global_network", label: "Global 1M", icon: Globe },
];

export default function InfraControls({
  infra, scenario, health, onChange, onScenarioChange, activePreset,
}: InfraControlsProps) {
  const set = (key: keyof InfraParams) => (v: number) =>
    onChange({ ...infra, [key]: v });

  return (
    <div className="space-y-4">
      {/* Preset Scenarios */}
      <Card className="border-border/60 bg-muted/20">
        <CardHeader className="pb-2">
          <CardTitle className="text-sm flex items-center gap-2 text-foreground">
            <Zap className="h-4 w-4 text-primary" /> Scenario Presets
          </CardTitle>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-2">
          {presetMeta.map(({ key, label, icon: Icon }) => (
            <Button
              key={key}
              size="sm"
              variant={activePreset === key ? "default" : "outline"}
              className="gap-1.5 text-xs"
              onClick={() => onScenarioChange(key)}
            >
              <Icon className="h-3.5 w-3.5" />
              {label}
            </Button>
          ))}
        </CardContent>
      </Card>

      {/* Health Indicators + Controls */}
      <Card className="border-primary/30 bg-primary/5">
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <CardTitle className="text-sm flex items-center gap-2 text-primary">
              <Settings className="h-4 w-4" /> Infrastructure Parameters
            </CardTitle>
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-1.5">
                <HealthDot status={health.workerUtil} />
                <span className="text-[10px] text-muted-foreground">Workers</span>
              </div>
              <div className="flex items-center gap-1.5">
                <HealthDot status={health.queueBacklog} />
                <span className="text-[10px] text-muted-foreground">Queue</span>
              </div>
              <div className="flex items-center gap-1.5">
                <HealthDot status={health.p95Latency} />
                <span className="text-[10px] text-muted-foreground">p95</span>
              </div>
              <div className="flex items-center gap-1.5">
                <HealthDot status={health.errorRate} />
                <span className="text-[10px] text-muted-foreground">Errors</span>
              </div>
            </div>
          </div>
          <p className="text-xs text-muted-foreground">
            Adjust parameters to see cascading effects — health dots update in real time
          </p>
        </CardHeader>
        <CardContent className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          <Param label="API Servers" value={infra.api_servers} unit="nodes" min={2} max={20} step={1} onChange={set("api_servers")} />
          <Param label="Background Workers" value={infra.bg_workers} unit="workers" min={2} max={50} step={1} onChange={set("bg_workers")} />
          <Param label="Validators" value={infra.validators} unit="people" min={50} max={5000} step={50} onChange={set("validators")} />
          <Param label="Events / Worker / Sec" value={infra.events_per_worker_per_second} unit="evt/s" min={50} max={500} step={10} onChange={set("events_per_worker_per_second")} />
          <Param
            label="Base Latency" value={infra.base_latency} unit="ms"
            min={50} max={200} step={5} onChange={set("base_latency")}
          />
          <Param
            label="Cache Efficiency" value={infra.cache_efficiency}
            displayValue={`${Math.round(infra.cache_efficiency * 100)}%`}
            unit="" min={0.80} max={0.99} step={0.01}
            onChange={set("cache_efficiency")}
          />
        </CardContent>
      </Card>
    </div>
  );
}
