import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Server, Cpu, HardDrive, Database, AlertTriangle, CheckCircle, Shield } from "lucide-react";
import type { ScenarioSummary, HealthLevel } from "@/lib/load-simulation-engine";

interface ComparisonPanelProps {
  summaries: ScenarioSummary[];
}

function healthBadge(level: HealthLevel) {
  const variant = level === "critical" ? "destructive" : level === "warning" ? "secondary" : "outline";
  return <Badge variant={variant} className="text-[10px]">{level}</Badge>;
}

function worstHealth(s: ScenarioSummary): HealthLevel {
  const levels: HealthLevel[] = [s.health.workerUtil, s.health.queueBacklog, s.health.p95Latency, s.health.errorRate];
  if (levels.includes("critical")) return "critical";
  if (levels.includes("warning")) return "warning";
  return "healthy";
}

export default function ComparisonPanel({ summaries }: ComparisonPanelProps) {
  if (summaries.length === 0) return null;

  return (
    <div className="space-y-4">
      {/* Comparison Table */}
      <Card className="border-border/60">
        <CardHeader className="pb-2">
          <CardTitle className="text-sm flex items-center gap-2">
            <Server className="h-4 w-4 text-primary" /> Scenario Comparison
          </CardTitle>
        </CardHeader>
        <CardContent className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="text-xs">Metric</TableHead>
                {summaries.map((s) => (
                  <TableHead key={s.key} className="text-xs text-center">{s.scenario.name}</TableHead>
                ))}
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow>
                <TableCell className="text-xs font-medium text-muted-foreground">Volunteers</TableCell>
                {summaries.map((s) => (
                  <TableCell key={s.key} className="text-xs text-center font-mono">{s.scenario.volunteers.toLocaleString()}</TableCell>
                ))}
              </TableRow>
              <TableRow>
                <TableCell className="text-xs font-medium text-muted-foreground">Peak API RPS</TableCell>
                {summaries.map((s) => (
                  <TableCell key={s.key} className="text-xs text-center font-mono">{s.peakApiRps.toLocaleString()}</TableCell>
                ))}
              </TableRow>
              <TableRow>
                <TableCell className="text-xs font-medium text-muted-foreground">Peak DB QPS</TableCell>
                {summaries.map((s) => (
                  <TableCell key={s.key} className="text-xs text-center font-mono">{s.peakDbQps.toLocaleString()}</TableCell>
                ))}
              </TableRow>
              <TableRow>
                <TableCell className="text-xs font-medium text-muted-foreground">Worker Util</TableCell>
                {summaries.map((s) => (
                  <TableCell key={s.key} className="text-xs text-center font-mono">{s.peakWorkerUtil}%</TableCell>
                ))}
              </TableRow>
              <TableRow>
                <TableCell className="text-xs font-medium text-muted-foreground">Queue Peak</TableCell>
                {summaries.map((s) => (
                  <TableCell key={s.key} className="text-xs text-center font-mono">{s.peakBacklog.toLocaleString()}</TableCell>
                ))}
              </TableRow>
              <TableRow>
                <TableCell className="text-xs font-medium text-muted-foreground">p95 Latency</TableCell>
                {summaries.map((s) => (
                  <TableCell key={s.key} className="text-xs text-center font-mono">{s.peakP95}ms</TableCell>
                ))}
              </TableRow>
              <TableRow>
                <TableCell className="text-xs font-medium text-muted-foreground">Error Rate</TableCell>
                {summaries.map((s) => (
                  <TableCell key={s.key} className="text-xs text-center font-mono">{(s.peakErrorRate * 100).toFixed(1)}%</TableCell>
                ))}
              </TableRow>
              <TableRow>
                <TableCell className="text-xs font-medium text-muted-foreground">Overall Health</TableCell>
                {summaries.map((s) => (
                  <TableCell key={s.key} className="text-center">{healthBadge(worstHealth(s))}</TableCell>
                ))}
              </TableRow>
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {/* Infrastructure Estimates */}
      <Card className="border-border/60">
        <CardHeader className="pb-2">
          <CardTitle className="text-sm flex items-center gap-2">
            <Cpu className="h-4 w-4 text-primary" /> Recommended Infrastructure
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {summaries.map((s) => (
              <div key={s.key} className="p-3 rounded-lg border border-border/60 bg-muted/20 space-y-2">
                <p className="text-xs font-semibold text-foreground">{s.scenario.name}</p>
                <div className="space-y-1">
                  <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    <Server className="h-3 w-3" />
                    <span>API Servers: <span className="font-mono text-foreground">{s.recommendedInfra.api_servers}</span></span>
                  </div>
                  <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    <Cpu className="h-3 w-3" />
                    <span>Workers: <span className="font-mono text-foreground">{s.recommendedInfra.bg_workers}</span></span>
                  </div>
                  <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    <HardDrive className="h-3 w-3" />
                    <span>Redis: <span className="font-mono text-foreground">{s.recommendedInfra.redis_memory_gb}GB</span></span>
                  </div>
                  <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    <Database className="h-3 w-3" />
                    <span>DB Replicas: <span className="font-mono text-foreground">{s.recommendedInfra.db_replicas}</span></span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Breakpoints & Summary */}
      <Card className="border-border/60">
        <CardHeader className="pb-2">
          <CardTitle className="text-sm flex items-center gap-2">
            <Shield className="h-4 w-4 text-primary" /> Capacity Summary
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {summaries.map((s) => (
            <div key={s.key} className="p-3 rounded-lg border border-border/60 bg-card space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-foreground">{s.scenario.name}</span>
                {healthBadge(worstHealth(s))}
              </div>
              <div className="grid grid-cols-3 gap-2 text-xs">
                <div>
                  <p className="text-muted-foreground">Max Safe Volunteers</p>
                  <p className="font-mono font-bold text-foreground">{s.maxSafeVolunteers.toLocaleString()}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">Bottleneck</p>
                  <p className="font-mono font-bold text-foreground">{s.bottleneckComponent}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">Breakpoints</p>
                  <p className="font-mono font-bold text-foreground">{s.breakpoints.length}</p>
                </div>
              </div>
              {s.breakpoints.length > 0 && (
                <div className="space-y-1 pt-1">
                  {s.breakpoints.map((bp) => (
                    <div key={bp.metric} className="flex items-center gap-2 text-xs">
                      {bp.severity === "critical" ? (
                        <AlertTriangle className="h-3 w-3 text-destructive shrink-0" />
                      ) : (
                        <AlertTriangle className="h-3 w-3 text-secondary shrink-0" />
                      )}
                      <span className="text-muted-foreground">
                        {bp.metric}: <span className="font-mono text-foreground">{bp.actual}</span> (threshold: {bp.threshold})
                      </span>
                    </div>
                  ))}
                </div>
              )}
              {s.breakpoints.length === 0 && (
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <CheckCircle className="h-3 w-3 text-primary" />
                  <span>All metrics within safe thresholds</span>
                </div>
              )}
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
