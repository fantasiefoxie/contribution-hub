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
  generateTimeline, DEFAULT_INFRA, DEFAULT_SCENARIO,
  type InfraParams, type SimPoint,
} from "@/lib/load-simulation-engine";
import InfraControls from "@/components/simulation/InfraControls";
import MetricCard from "@/components/simulation/MetricCard";
import BottleneckItem from "@/components/simulation/BottleneckItem";

const SCENARIO_META = {
  initiative: "Citywide Environmental Survey",
  region: "Mumbai Metropolitan Area",
  volunteers: 50_000,
  validators: 2_000,
  tasks: 5_000,
  target: "100K households",
  peak_window: "6 PM – 10 PM",
};

export default function LoadSimulation() {
  const [infra, setInfra] = useState<InfraParams>(DEFAULT_INFRA);
  const [elapsed, setElapsed] = useState(0);

  const timeline = useMemo(
    () => generateTimeline(infra, DEFAULT_SCENARIO),
    [infra]
  );

  // Animate elapsed
  useEffect(() => {
    setElapsed(0);
  }, [infra]);

  useEffect(() => {
    if (elapsed >= 120) return;
    const id = setTimeout(() => setElapsed((e) => Math.min(e + 5, 120)), 180);
    return () => clearTimeout(id);
  }, [elapsed]);

  const visiblePoints = timeline.filter((p) => p.minute <= elapsed);
  const peak = timeline.reduce((max, p) => (p.apiRps > (max?.apiRps ?? 0) ? p : max), timeline[0]);
  const totalApiRequests = timeline.reduce((s, p) => s + p.apiRps * 5 * 60, 0);
  const peakBacklog = Math.max(...timeline.map((p) => p.eventQueueBacklog));
  const peakWorkerUtil = Math.max(...timeline.map((p) => p.workerUtil));
  const peakError = Math.max(...timeline.map((p) => p.errorRate));

  // Dynamic bottlenecks based on simulation results
  const bottlenecks = useMemo(() => {
    const items: { severity: "critical" | "warning" | "info"; title: string; description: string; recommendation: string }[] = [];

    if (peakBacklog > 2000) {
      items.push({
        severity: "critical",
        title: "Event Queue Backlog Overflow",
        description: `Queue peaks at ${peakBacklog.toLocaleString()} events. ${infra.bg_workers} workers cannot drain fast enough during peak traffic.`,
        recommendation: `Scale workers to ${Math.ceil(infra.bg_workers * 2.5)} during peak. Add priority lanes and auto-scaling trigger at backlog > 500.`,
      });
    } else if (peakBacklog > 500) {
      items.push({
        severity: "warning",
        title: "Event Queue Backlog Elevated",
        description: `Queue reaches ${peakBacklog.toLocaleString()} events at peak. Processing delays of 5-15s expected.`,
        recommendation: `Increase workers from ${infra.bg_workers} to ${Math.ceil(infra.bg_workers * 1.5)} or raise events_per_worker_per_second.`,
      });
    }

    if (peakWorkerUtil >= 95) {
      items.push({
        severity: "critical",
        title: "Worker Saturation",
        description: `Workers hit ${peakWorkerUtil}% utilization — no headroom for bursts.`,
        recommendation: `Add ${Math.ceil(infra.bg_workers * 0.5)} more workers or optimize event processing throughput.`,
      });
    }

    const peakP99 = Math.max(...timeline.map((p) => p.p99));
    if (peakP99 > 800) {
      items.push({
        severity: "critical",
        title: "p99 Latency Exceeds SLA",
        description: `p99 latency peaks at ${peakP99}ms. User-facing requests will timeout at this level.`,
        recommendation: `Scale API servers from ${infra.api_servers} to ${infra.api_servers * 2}. Reduce base_latency via query optimization.`,
      });
    } else if (peakP99 > 400) {
      items.push({
        severity: "warning",
        title: "Elevated p99 Latency",
        description: `p99 latency reaches ${peakP99}ms during peak window.`,
        recommendation: `Add read replica for discovery queries. Enable 5s cache TTL on task browse endpoints.`,
      });
    }

    if (peakError > 0.05) {
      items.push({
        severity: "critical",
        title: "Error Rate Spike",
        description: `Error rate reaches ${(peakError * 100).toFixed(1)}% — cascading from latency exceeding 300ms threshold.`,
        recommendation: `Address latency bottlenecks first. Add circuit breakers and retry budgets.`,
      });
    }

    if (items.length === 0) {
      items.push({
        severity: "info",
        title: "System Within Capacity",
        description: "All metrics within acceptable thresholds. Infrastructure can handle this load configuration.",
        recommendation: "Monitor during actual deployment. Consider reducing resources to save cost.",
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
          <div className="flex items-center gap-2">
            <Activity className="h-6 w-6 text-primary" />
            <h1 className="text-2xl font-bold text-foreground font-['Space_Grotesk']">
              Load Simulation
            </h1>
            <Badge variant="secondary" className="ml-2">System Dynamics</Badge>
          </div>
          <p className="text-sm text-muted-foreground">
            Stateful simulation for <strong>{SCENARIO_META.initiative}</strong> — {SCENARIO_META.region}
          </p>
        </div>

        {/* Scenario Overview */}
        <Card className="border-border/60 bg-muted/30">
          <CardContent className="p-4">
            <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-6 gap-4 text-center">
              {([
                ["Volunteers", SCENARIO_META.volunteers.toLocaleString()],
                ["Validators", SCENARIO_META.validators.toLocaleString()],
                ["Tasks", SCENARIO_META.tasks.toLocaleString()],
                ["Peak Users", DEFAULT_SCENARIO.peak_users.toLocaleString()],
                ["Target", SCENARIO_META.target],
                ["Window", SCENARIO_META.peak_window],
              ] as const).map(([label, val]) => (
                <div key={label}>
                  <p className="text-xs text-muted-foreground">{label}</p>
                  <p className="text-sm font-bold text-foreground">{val}</p>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Infrastructure Controls */}
        <InfraControls infra={infra} onChange={setInfra} />

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
          <MetricCard icon={Layers} label="Peak Queue Backlog" value={peakBacklog.toLocaleString()} sub="event processing" />
          <MetricCard icon={BarChart3} label="Total API Requests" value={`${(totalApiRequests / 1_000_000).toFixed(1)}M`} sub="over 2h window" />
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
                <CardTitle className="text-sm">API Request Rate & DB Queries per Second</CardTitle>
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
                <CardTitle className="text-sm">Response Latency Percentiles (ms)</CardTitle>
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
                <CardTitle className="text-sm">Cascade: Queue Backlog → Worker Util → Error Rate</CardTitle>
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
                { label: "API Servers", count: infra.api_servers, icon: Server, status: peakWorkerUtil > 80 ? "saturated" : "healthy" },
                { label: "PostgreSQL", count: 1, icon: Database, status: peak?.dbConnPool > 85 ? "bottleneck" : "healthy" },
                { label: "Redis Cache", count: 1, icon: HardDrive, status: "healthy" },
                { label: "Workers", count: infra.bg_workers, icon: Cpu, status: peakWorkerUtil >= 95 ? "bottleneck" : peakWorkerUtil > 70 ? "saturated" : "healthy" },
                { label: "Object Store", count: 1, icon: Layers, status: "healthy" },
              ].map(({ label, count, icon: I, status }) => (
                <div key={label} className="flex flex-col items-center gap-1 p-3 rounded-lg border border-border/60 bg-card">
                  <I className="h-6 w-6 text-muted-foreground" />
                  <span className="text-xs font-semibold text-foreground">{count}× {label}</span>
                  <Badge
                    variant={status === "bottleneck" ? "destructive" : status === "saturated" ? "secondary" : "outline"}
                    className="text-[10px]"
                  >
                    {status}
                  </Badge>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Dynamic Bottlenecks */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm flex items-center gap-2">
              <TrendingUp className="h-4 w-4" /> Bottlenecks & Recommendations
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
