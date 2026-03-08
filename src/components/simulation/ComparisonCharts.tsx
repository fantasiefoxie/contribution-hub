import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, Legend,
} from "recharts";
import type { ScenarioSummary } from "@/lib/load-simulation-engine";
import { useMemo } from "react";

interface ComparisonChartsProps {
  summaries: ScenarioSummary[];
}

// Distinct scenario colors using HSL from design tokens where possible
const SCENARIO_COLORS = [
  "hsl(var(--primary))",
  "hsl(var(--accent))",
  "hsl(var(--destructive))",
];

const CHART_TOOLTIP_STYLE = {
  fontSize: 12,
  background: "hsl(var(--card))",
  border: "1px solid hsl(var(--border))",
};

interface MergedPoint {
  time: string;
  minute: number;
  [key: string]: string | number;
}

export default function ComparisonCharts({ summaries }: ComparisonChartsProps) {
  // Merge timelines by minute for overlay charts
  const mergedData = useMemo(() => {
    if (summaries.length === 0) return [];
    const base = summaries[0].timeline;
    const merged: MergedPoint[] = base.map((p) => ({
      time: p.time,
      minute: p.minute,
    }));

    summaries.forEach((s, idx) => {
      s.timeline.forEach((p, pIdx) => {
        if (pIdx < merged.length) {
          merged[pIdx][`apiRps_${idx}`] = p.apiRps;
          merged[pIdx][`workerUtil_${idx}`] = p.workerUtil;
          merged[pIdx][`eventQueueBacklog_${idx}`] = p.eventQueueBacklog;
          merged[pIdx][`p95_${idx}`] = p.p95;
        }
      });
    });

    return merged;
  }, [summaries]);

  if (summaries.length === 0 || mergedData.length === 0) {
    return <Skeleton className="w-full h-[300px] rounded-lg" />;
  }

  const charts: { title: string; dataKeyPrefix: string; unit: string }[] = [
    { title: "API Request Rate", dataKeyPrefix: "apiRps", unit: "RPS" },
    { title: "Worker Utilization", dataKeyPrefix: "workerUtil", unit: "%" },
    { title: "Queue Backlog", dataKeyPrefix: "eventQueueBacklog", unit: "events" },
    { title: "p95 Latency", dataKeyPrefix: "p95", unit: "ms" },
  ];

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
      {charts.map(({ title, dataKeyPrefix, unit }) => (
        <Card key={dataKeyPrefix} className="border-border/60">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm">{title} ({unit})</CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={250}>
              <LineChart data={mergedData}>
                <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                <XAxis dataKey="time" tick={{ fontSize: 10 }} stroke="hsl(var(--muted-foreground))" />
                <YAxis tick={{ fontSize: 10 }} stroke="hsl(var(--muted-foreground))" />
                <Tooltip contentStyle={CHART_TOOLTIP_STYLE} />
                <Legend wrapperStyle={{ fontSize: 11 }} />
                {summaries.map((s, idx) => (
                  <Line
                    key={s.key}
                    type="monotone"
                    dataKey={`${dataKeyPrefix}_${idx}`}
                    name={s.scenario.name}
                    stroke={SCENARIO_COLORS[idx % SCENARIO_COLORS.length]}
                    strokeWidth={2}
                    dot={false}
                    strokeDasharray={idx > 0 ? `${4 + idx * 2} ${2 + idx}` : undefined}
                  />
                ))}
              </LineChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
