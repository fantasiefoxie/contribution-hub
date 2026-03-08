import AppShell from "@/components/layout/AppShell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useState, useEffect, useMemo } from "react";
import {
  Activity, Server, Database, HardDrive, Cpu, Clock,
  TrendingUp, Users, Zap, BarChart3, Layers
} from "lucide-react";
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, AreaChart, Area, BarChart, Bar
} from "recharts";
import {
  generateTimeline, computeHealth, DEFAULT_INFRA, DEFAULT_SCENARIO, PRESETS,
  type InfraParams, type ScenarioConfig,
} from "@/lib/load-simulation-engine";
import InfraControls from "@/components/simulation/InfraControls";
import MetricCard from "@/components/simulation/MetricCard";
import BottleneckItem from "@/components/simulation/BottleneckItem";

export default function LoadSimulation() {
  const [infra, setInfra] = useState<InfraParams>(DEFAULT_INFRA);
  const [scenario, setScenario] = useState<ScenarioConfig>(DEFAULT_SCENARIO);
  const [activePreset, setActivePreset] = useState<string | null>("city_survey");
  const [elapsed, setElapsed] = useState(0);

  const timeline = useMemo(
    () => generateTimeline(infra, scenario),
    [infra, scenario]
  );

  const health = useMemo(() => computeHealth(timeline), [timeline]);

  // Reset animation on param change
  useEffect(() => { setElapsed(0); }, [infra, scenario]);

  useEffect(() => {
    if (elapsed >= 120) return;
    const id = setTimeout(() => setElapsed((e) => Math.min(e + 5, 120)), 150);
    return () => clearTimeout(id);
  }, [elapsed]);

  const handlePreset = (key: string) => {
    const p = PRESETS[key];
    if (!p) return;
    setInfra({ ...DEFAULT_INFRA, ...p.infra });
    setScenario(p.scenario);
    setActivePreset(key);
  };

  const handleInfraChange = (next: InfraParams) => {
    setInfra(next);
    setActivePreset(null); // custom config
  };

  const visiblePoints = timeline.filter((p) => p.minute <= elapsed);

  const peak = useMemo(
    () => timeline.length > 0 ? timeline.reduce((max, p) => (p.apiRps > max.apiRps ? p : max), timeline[0]) : null,
    [timeline]
  );
  const totalApiRequests = useMemo(
    () => timeline.reduce((s, p) => s + p.apiRps * 5 * 60, 0),
    [timeline]
  );
  const peakBacklog = useMemo(
    () => timeline.length > 0 ? Math.max(...timeline.map((p) => p.eventQueueBacklog)) : 0,
    [timeline]
  );
  const peakWorkerUtil = useMemo(
    () => timeline.length > 0 ? Math.max(...timeline.map((p) => p.workerUtil)) : 0,
    [timeline]
  );
  const peakError = useMemo(
    () => timeline.length > 0 ? Math.max(...timeline.map((p) => p.errorRate)) : 0,
    [timeline]
  );

  const bottlenecks = useMemo(() => {
    const items: { severity: "critical" | "warning" | "info"; title: string; description: string; recommendation: string }[] = [];

    if (peakBacklog > 2000) {
      items.push({
        severity: "critical",
        title: "Event Queue Backlog Overflow",
        description: `Queue peaks at ${peakBacklog.toLocaleString()} events. ${infra.bg_workers} workers cannot drain fast enough.`,
        recommendation: `Scale workers to ${Math.ceil(infra.bg_workers * 2.5)}. Add auto-scaling trigger at backlog > 500.`,
      });
    } else if (peakBacklog > 500) {
      items.push({
        severity: "warning",
        title: "Event Queue Backlog Elevated",
        description: `Queue reaches ${peakBacklog.toLocaleString()} events. 5-15s processing delays expected.`,
        recommendation: `Increase workers from ${infra.bg_workers} to ${Math.ceil(infra.bg_workers * 1.5)}.`,
      });
    }

    if (peakWorkerUtil >= 95) {
      items.push({
        severity: "critical",
        title: "Worker Saturation",
        description: `Workers hit ${peakWorkerUtil}% utilization — zero burst headroom.`,
        recommendation: `Add ${Math.ceil(infra.bg_workers * 0.5)} more workers or increase events_per_worker_per_second.`,
      });
    }

    const peakP95 = Math.max(...timeline.map((p) => p.p95));
    const peakP99 = Math.max(...timeline.map((p) => p.p99));
    if (peakP99 > 800) {
      items.push({
        severity: "critical",
        title: "p99 Latency Exceeds SLA",
        description: `p99 reaches ${peakP99}ms. Requests will timeout.`,
        recommendation: `Scale API servers from ${infra.api_servers} to ${infra.api_servers * 2}. Add read replica.`,
      });
    } else if (peakP95 > 300) {
      items.push({
        severity: "warning",
        title: "Elevated p95 Latency",
        description: `p95 latency reaches ${peakP95}ms during peak.`,
        recommendation: `Increase cache efficiency or add API servers. Current cache: ${Math.round(infra.cache_efficiency * 100)}%.`,
      });
    }

    if (peakError > 0.05) {
      items.push({
        severity: "critical",
        title: "Error Rate Spike",
        description: `Error rate reaches ${(peakError * 100).toFixed(1)}% — cascading from latency > 300ms.`,
        recommendation: `Address latency bottlenecks. Add circuit breakers and retry budgets.`,
      });
    }

    if (infra.cache_efficiency < 0.88) {
      items.push({
        severity: "warning",
        title: "Low Cache Efficiency",
        description: `Cache hit ratio at ${Math.round(infra.cache_efficiency * 100)}% causes excessive DB load.`,
        recommendation: `Increase Redis memory and TTL. Pre-warm discovery cache before peak window.`,
      });
    }

    if (items.length === 0) {
      items.push({
        severity: "info",
        title: "System Within Capacity",
        description: "All metrics within acceptable thresholds for this configuration.",
        recommendation: "Monitor during deployment. Consider reducing resources to save cost.",
      });
    }

    return items;
  }, [timeline, peakBacklog, peakWorkerUtil, peakError, infra]);

  const chartStyle = { fontSize: 12, background: "hsl(var(--card))", border: "1px solid hsl(var(--border))" };

  return (
    <AppShell>
      <div className="max-w-6xl mx-auto space-y-6 pb-24">
        {/* Header */}
        <div className="space-y-1">
          <div className="flex items-center gap-2 flex-wrap">
            <Activity className="h-6 w-6 text-primary" />
            <h1 className="text-2xl font-bold text-foreground font-['Space_Grotesk']">
              Capacity Planner
            </h1>
            <Badge variant="secondary" className="ml-2">{scenario.name}</Badge>
          </div>
          <p className="text-sm text-muted-foreground">
            Interactive infrastructure capacity simulator — adjust parameters to observe cascading system behavior
          </p>
        </div>

        {/* Scenario Overview */}
        <Card className="border-border/60 bg-muted/30">
          <CardContent className="p-4">
            <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-6 gap-4 text-center">
              {([
                ["Volunteers", scenario.volunteers.toLocaleString()],
                ["Validators", scenario.validators.toLocaleString()],
                ["Tasks", scenario.tasks.toLocaleString()],
                ["Peak Users", scenario.peak_users.toLocaleString()],
                ["Target", scenario.target],
                ["Spread", `σ=${scenario.spread}min`],
              ] as const).map(([label, val]) => (
                <div key={label}>
                  <p className="text-xs text-muted-foreground">{label}</p>
                  <p className="text-sm font-bold text-foreground">{val}</p>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Controls */}
        <InfraControls
          infra={infra}
          scenario={scenario}
          health={health}
          onChange={handleInfraChange}
          onScenarioChange={handlePreset}
          activePreset={activePreset}
        />

        {/* Progress */}
        <div className="flex items-center gap-3">
          <Clock className="h-4 w-4 text-muted-foreground" />
          <Progress value={(elapsed / 120) * 100} className="flex-1" />
          <span className="text-xs text-muted-foreground font-mono w-20 text-right">
            {Math.floor(elapsed / 60)}h {elapsed % 60}m / 2h
          </span>
        </div>

        {/* Summary Metrics */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <MetricCard icon={Zap} label="Peak API RPS" value={peak?.apiRps?.toLocaleString() ?? "—"} sub={`at ${peak?.time ?? ""}`} />
          <MetricCard icon={Database} label="Peak DB QPS" value={peak?.dbQps?.toLocaleString() ?? "—"} sub="PostgreSQL primary" />
          <MetricCard icon={Layers} label="Peak Queue" value={peakBacklog.toLocaleString()} sub="event backlog" />
          <MetricCard icon={BarChart3} label="Total Requests" value={`${(totalApiRequests / 1_000_000).toFixed(1)}M`} sub="2h window" />
        </div>

        {/* Charts */}
        <Tabs defaultValue="throughput" className="space-y-4">
          <TabsList className="bg-muted/50">
            <TabsTrigger value="throughput">Throughput</TabsTrigger>
            <TabsTrigger value="latency">Latency</TabsTrigger>
            <TabsTrigger value="cascade">Cascade</TabsTrigger>
            <TabsTrigger value="operations">Operations</TabsTrigger>
          </TabsList>

          <TabsContent value="throughput">
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm">API RPS & DB QPS</CardTitle>
              </CardHeader>
              <CardContent>
                <ResponsiveContainer width="100%" height={300}>
                  <AreaChart data={visiblePoints}>
                    <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                    <XAxis dataKey="time" tick={{ fontSize: 10 }} stroke="hsl(var(--muted-foreground))" />
                    <YAxis tick={{ fontSize: 10 }} stroke="hsl(var(--muted-foreground))" />
                    <Tooltip contentStyle={chartStyle} />
                    <Area type="monotone" dataKey="dbQps" name="DB QPS" fill="hsl(var(--accent) / 0.2)" stroke="hsl(var(--accent))" />
                    <Area type="monotone" dataKey="apiRps" name="API RPS" fill="hsl(var(--primary) / 0.3)" stroke="hsl(var(--primary))" />
                  </AreaChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="latency">
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm">Latency Percentiles (ms)</CardTitle>
              </CardHeader>
              <CardContent>
                <ResponsiveContainer width="100%" height={300}>
                  <LineChart data={visiblePoints}>
                    <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                    <XAxis dataKey="time" tick={{ fontSize: 10 }} stroke="hsl(var(--muted-foreground))" />
                    <YAxis tick={{ fontSize: 10 }} stroke="hsl(var(--muted-foreground))" />
                    <Tooltip contentStyle={chartStyle} />
                    <Line type="monotone" dataKey="p50" name="p50" stroke="hsl(var(--primary))" strokeWidth={2} dot={false} />
                    <Line type="monotone" dataKey="p95" name="p95" stroke="hsl(var(--accent))" strokeWidth={2} dot={false} />
                    <Line type="monotone" dataKey="p99" name="p99" stroke="hsl(var(--destructive))" strokeWidth={2} dot={false} />
                  </LineChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="cascade">
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm">Cascade: Queue → Worker Util → Errors</CardTitle>
              </CardHeader>
              <CardContent>
                <ResponsiveContainer width="100%" height={300}>
                  <LineChart data={visiblePoints}>
                    <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                    <XAxis dataKey="time" tick={{ fontSize: 10 }} stroke="hsl(var(--muted-foreground))" />
                    <YAxis yAxisId="left" tick={{ fontSize: 10 }} stroke="hsl(var(--muted-foreground))" />
                    <YAxis yAxisId="right" orientation="right" tick={{ fontSize: 10 }} stroke="hsl(var(--muted-foreground))" />
                    <Tooltip contentStyle={chartStyle} />
                    <Line yAxisId="left" type="monotone" dataKey="eventQueueBacklog" name="Queue Backlog" stroke="hsl(var(--destructive))" strokeWidth={2} dot={false} />
                    <Line yAxisId="right" type="monotone" dataKey="workerUtil" name="Worker %" stroke="hsl(var(--primary))" strokeWidth={2} dot={false} />
                    <Line yAxisId="right" type="monotone" dataKey="errorRate" name="Error Rate" stroke="hsl(var(--accent))" strokeWidth={2} dot={false} strokeDasharray="4 2" />
                  </LineChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="operations">
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm">Task Operations per 5-min Window</CardTitle>
              </CardHeader>
              <CardContent>
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart data={visiblePoints}>
                    <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                    <XAxis dataKey="time" tick={{ fontSize: 10 }} stroke="hsl(var(--muted-foreground))" />
                    <YAxis tick={{ fontSize: 10 }} stroke="hsl(var(--muted-foreground))" />
                    <Tooltip contentStyle={chartStyle} />
                    <Bar dataKey="taskClaims" name="Claims" fill="hsl(var(--primary))" radius={[2, 2, 0, 0]} />
                    <Bar dataKey="submissions" name="Submissions" fill="hsl(var(--accent))" radius={[2, 2, 0, 0]} />
                    <Bar dataKey="validations" name="Validations" fill="hsl(var(--secondary))" radius={[2, 2, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>

        {/* Infrastructure Topology */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm flex items-center gap-2">
              <Server className="h-4 w-4" /> Infrastructure Topology
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
              {[
                { label: "API Servers", count: infra.api_servers, icon: Server, health: health.p95Latency },
                { label: "PostgreSQL", count: 1, icon: Database, health: peak?.dbConnPool > 85 ? "critical" as const : peak?.dbConnPool > 60 ? "warning" as const : "healthy" as const },
                { label: "Redis Cache", count: 1, icon: HardDrive, health: infra.cache_efficiency < 0.88 ? "warning" as const : "healthy" as const },
                { label: "Workers", count: infra.bg_workers, icon: Cpu, health: health.workerUtil },
                { label: "Object Store", count: 1, icon: Layers, health: "healthy" as const },
              ].map(({ label, count, icon: I, health: h }) => (
                <div key={label} className="flex flex-col items-center gap-1.5 p-3 rounded-lg border border-border/60 bg-card">
                  <I className="h-6 w-6 text-muted-foreground" />
                  <span className="text-xs font-semibold text-foreground">{count}× {label}</span>
                  <Badge
                    variant={h === "critical" ? "destructive" : h === "warning" ? "secondary" : "outline"}
                    className="text-[10px]"
                  >
                    {h === "critical" ? "overloaded" : h === "warning" ? "saturated" : "healthy"}
                  </Badge>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Bottlenecks */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm flex items-center gap-2">
              <TrendingUp className="h-4 w-4" /> Scaling Recommendations
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {bottlenecks.map((b, i) => (
              <BottleneckItem key={i} {...b} />
            ))}
          </CardContent>
        </Card>
      </div>
    </AppShell>
  );
}
