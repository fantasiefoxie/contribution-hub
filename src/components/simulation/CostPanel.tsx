import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { DollarSign, TrendingDown, Lightbulb, Server, Cpu, Database, HardDrive, Globe, HardDriveDownload } from "lucide-react";
import type { CostBreakdown, CostOptimizationHint, ScenarioSummary } from "@/lib/load-simulation-engine";
import { estimateCost, getCostHints } from "@/lib/load-simulation-engine";
import { useMemo } from "react";
import { ResponsiveContainer, PieChart, Pie, Cell, Tooltip } from "recharts";

const PIE_COLORS = [
  "hsl(var(--primary))",
  "hsl(var(--accent))",
  "hsl(var(--secondary))",
  "hsl(var(--destructive))",
  "hsl(var(--muted-foreground))",
  "hsl(var(--primary) / 0.5)",
  "hsl(var(--accent) / 0.5)",
];

function fmt(v: number): string {
  return v >= 1000 ? `$${(v / 1000).toFixed(1)}K` : `$${Math.round(v)}`;
}

// ── Single Scenario Cost Panel ──
interface SingleCostPanelProps {
  breakdown: CostBreakdown;
  hints: CostOptimizationHint[];
  scenarioName: string;
}

export function SingleCostPanel({ breakdown, hints, scenarioName }: SingleCostPanelProps) {
  const pieData = useMemo(() => [
    { name: "API Servers", value: breakdown.apiServers },
    { name: "Workers", value: breakdown.workers },
    { name: "PostgreSQL Primary", value: breakdown.postgresPrimary },
    { name: "DB Replicas", value: breakdown.postgresReplicas },
    { name: "Redis", value: breakdown.redis },
    { name: "Storage", value: breakdown.storage },
    { name: "Bandwidth", value: breakdown.bandwidth },
  ].filter((d) => d.value > 0), [breakdown]);

  return (
    <div className="space-y-4">
      {/* Cost Summary */}
      <Card className="border-primary/30 bg-primary/5">
        <CardHeader className="pb-2">
          <CardTitle className="text-sm flex items-center gap-2 text-primary">
            <DollarSign className="h-4 w-4" /> Estimated Monthly Cost
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-4">
              <div>
                <p className="text-3xl font-bold font-mono text-foreground">{fmt(breakdown.total)}</p>
                <p className="text-xs text-muted-foreground">per month — {scenarioName}</p>
              </div>
              <div className="flex items-center gap-4">
                <div className="px-3 py-1.5 rounded-md bg-muted/50 border border-border/60">
                  <p className="text-xs text-muted-foreground">Per 1K Volunteers</p>
                  <p className="text-sm font-bold font-mono text-foreground">${breakdown.costPer1kVolunteers.toFixed(2)}</p>
                </div>
              </div>

              {/* Line-item breakdown */}
              <div className="space-y-1.5">
                {([
                  { icon: Server, label: "API Servers", val: breakdown.apiServers },
                  { icon: Cpu, label: "Workers", val: breakdown.workers },
                  { icon: Database, label: "PostgreSQL Primary", val: breakdown.postgresPrimary },
                  { icon: Database, label: "DB Replicas", val: breakdown.postgresReplicas },
                  { icon: HardDrive, label: "Redis Cache", val: breakdown.redis },
                  { icon: HardDriveDownload, label: "Storage", val: breakdown.storage },
                  { icon: Globe, label: "Bandwidth", val: breakdown.bandwidth },
                ] as const).filter((r) => r.val > 0).map(({ icon: I, label, val }) => (
                  <div key={label} className="flex items-center justify-between text-xs">
                    <span className="flex items-center gap-1.5 text-muted-foreground"><I className="h-3 w-3" />{label}</span>
                    <span className="font-mono text-foreground">{fmt(val)}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Pie Chart */}
            <div className="flex items-center justify-center">
              <ResponsiveContainer width="100%" height={200}>
                <PieChart>
                  <Pie data={pieData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} innerRadius={40} paddingAngle={2}>
                    {pieData.map((_, idx) => (
                      <Cell key={idx} fill={PIE_COLORS[idx % PIE_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(v: number) => fmt(v)} contentStyle={{ fontSize: 11, background: "hsl(var(--card))", border: "1px solid hsl(var(--border))" }} />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Optimization Hints */}
      {hints.length > 0 && (
        <Card className="border-border/60">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm flex items-center gap-2">
              <Lightbulb className="h-4 w-4 text-accent" /> Cost Optimization Hints
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            {hints.map((h) => (
              <div key={h.title} className="flex items-start gap-3 p-2.5 rounded-lg border border-border/60 bg-muted/20">
                <TrendingDown className="h-4 w-4 text-primary shrink-0 mt-0.5" />
                <div className="min-w-0">
                  <p className="text-xs font-semibold text-foreground">{h.title}</p>
                  <p className="text-xs text-muted-foreground">{h.description}</p>
                </div>
                <Badge variant="secondary" className="text-[10px] shrink-0">~{h.savingsPercent}% save</Badge>
              </div>
            ))}
          </CardContent>
        </Card>
      )}
    </div>
  );
}

// ── Comparison Cost Table ──
interface ComparisonCostTableProps {
  summaries: ScenarioSummary[];
}

export function ComparisonCostTable({ summaries }: ComparisonCostTableProps) {
  const rows = useMemo(() =>
    summaries.map((s) => {
      const cost = estimateCost(s.recommendedInfra, s.scenario);
      return { key: s.key, name: s.scenario.name, cost, infra: s.recommendedInfra };
    }), [summaries]);

  if (rows.length === 0) return null;

  return (
    <Card className="border-border/60">
      <CardHeader className="pb-2">
        <CardTitle className="text-sm flex items-center gap-2">
          <DollarSign className="h-4 w-4 text-primary" /> Cost Comparison
        </CardTitle>
      </CardHeader>
      <CardContent className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="text-xs">Scenario</TableHead>
              <TableHead className="text-xs text-right">Monthly Cost</TableHead>
              <TableHead className="text-xs text-right">API Servers</TableHead>
              <TableHead className="text-xs text-right">Workers</TableHead>
              <TableHead className="text-xs text-right">DB Replicas</TableHead>
              <TableHead className="text-xs text-right">Per 1K Vol.</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((r) => (
              <TableRow key={r.key}>
                <TableCell className="text-xs font-medium">{r.name}</TableCell>
                <TableCell className="text-xs text-right font-mono font-bold">{fmt(r.cost.total)}</TableCell>
                <TableCell className="text-xs text-right font-mono">{r.infra.api_servers}</TableCell>
                <TableCell className="text-xs text-right font-mono">{r.infra.bg_workers}</TableCell>
                <TableCell className="text-xs text-right font-mono">{r.infra.db_replicas}</TableCell>
                <TableCell className="text-xs text-right font-mono">${r.cost.costPer1kVolunteers.toFixed(2)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}
