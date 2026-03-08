import { Trophy, Star, Target, Calendar, Award, TrendingUp, Home, Droplets, Users, Leaf } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import AppShell from "@/components/layout/AppShell";
import InitiativeProgressCard from "@/components/impact/InitiativeProgressCard";
import { useMyImpact } from "@/hooks/use-impact";

const badges = [
  { name: "Community Outreach", level: "Gold", emoji: "🤝", domain: "community" },
  { name: "Environmental Work", level: "Silver", emoji: "🌿", domain: "environment" },
  { name: "Survey Research", level: "Bronze", emoji: "📊", domain: "research" },
  { name: "Event Logistics", level: "Platinum", emoji: "🎪", domain: "logistics" },
];

const levelColors: Record<string, string> = {
  Bronze: "bg-orange-100 text-orange-700",
  Silver: "bg-gray-100 text-gray-700",
  Gold: "bg-yellow-100 text-yellow-700",
  Platinum: "bg-violet-100 text-violet-700",
};

const metricIcons: Record<string, typeof Home> = {
  households_reached: Home,
  trees_planted: Leaf,
  waste_removed_kg: Droplets,
  people_served: Users,
};

// Mock initiative progress
const mockInitiatives = [
  {
    title: "River Cleanup Campaign",
    progress: {
      households_reached: { current: 480, target: 1000, percentage: 48 },
      waste_removed_kg: { current: 320, target: 500, percentage: 64 },
    },
    milestones: [{ percentage: 25, reached_at: "2026-02-15" }],
  },
  {
    title: "Urban Reforestation Drive",
    progress: {
      trees_planted: { current: 750, target: 1000, percentage: 75 },
    },
    milestones: [
      { percentage: 25, reached_at: "2026-01-10" },
      { percentage: 50, reached_at: "2026-02-01" },
      { percentage: 75, reached_at: "2026-03-05" },
    ],
  },
];

export default function Contributions() {
  const { data: impactData } = useMyImpact();

  const tasksCompleted = impactData?.tasks_completed ?? 24;
  const totalScore = impactData?.total_score ?? 1240;
  const initiativesCount = impactData?.initiatives_participated ?? 5;
  const impactMetrics = impactData?.impact_metrics ?? {
    households_reached: 156,
    trees_planted: 42,
    waste_removed_kg: 89,
    people_served: 210,
  };

  const stats = [
    { label: "Tasks Done", value: String(tasksCompleted), icon: Target, color: "text-primary" },
    { label: "Impact Score", value: totalScore.toLocaleString(), icon: Star, color: "text-secondary" },
    { label: "Initiatives", value: String(initiativesCount), icon: Calendar, color: "text-accent" },
    { label: "Badges", value: "8", icon: Award, color: "text-primary" },
  ];

  return (
    <AppShell>
      <div className="px-4 pt-6 pb-3">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="font-display text-2xl font-bold">My Impact</h1>
            <p className="text-sm text-muted-foreground mt-0.5">Your contribution history</p>
          </div>
          <div className="h-12 w-12 rounded-full bg-primary/10 flex items-center justify-center">
            <Trophy className="h-6 w-6 text-primary" />
          </div>
        </div>
      </div>

      {/* Stats grid */}
      <div className="grid grid-cols-2 gap-2 px-4 mt-2">
        {stats.map(({ label, value, icon: Icon, color }) => (
          <div key={label} className="bg-card rounded-2xl border border-border p-4">
            <Icon className={`h-5 w-5 ${color} mb-2`} />
            <p className="font-display text-2xl font-bold">{value}</p>
            <p className="text-xs text-muted-foreground mt-0.5">{label}</p>
          </div>
        ))}
      </div>

      {/* Cumulative Impact Metrics */}
      <div className="px-4 mt-6">
        <h2 className="font-display font-semibold text-base mb-3 flex items-center gap-2">
          <TrendingUp className="h-4 w-4 text-primary" />
          Cumulative Impact
        </h2>
        <div className="grid grid-cols-2 gap-2">
          {Object.entries(impactMetrics as Record<string, number>).map(([key, value]) => {
            const Icon = metricIcons[key] || Star;
            return (
              <div key={key} className="bg-gradient-to-br from-primary/5 to-secondary/5 rounded-xl border border-primary/10 p-3">
                <Icon className="h-4 w-4 text-primary mb-1" />
                <p className="font-display text-lg font-bold">{value.toLocaleString()}</p>
                <p className="text-[10px] text-muted-foreground capitalize">{key.replace(/_/g, " ")}</p>
              </div>
            );
          })}
        </div>
      </div>

      {/* Initiative Progress */}
      <div className="px-4 mt-6">
        <h2 className="font-display font-semibold text-base mb-3">Initiative Progress</h2>
        <div className="space-y-3">
          {mockInitiatives.map((init) => (
            <InitiativeProgressCard
              key={init.title}
              title={init.title}
              progress={init.progress}
              milestones={init.milestones}
            />
          ))}
        </div>
      </div>

      {/* Badges */}
      <div className="px-4 mt-6">
        <h2 className="font-display font-semibold text-base mb-3">Badges Earned</h2>
        <div className="grid grid-cols-2 gap-2">
          {badges.map((b) => (
            <div key={b.name} className="bg-card rounded-xl border border-border p-3 flex items-start gap-3">
              <span className="text-2xl">{b.emoji}</span>
              <div className="min-w-0">
                <p className="font-medium text-xs truncate">{b.name}</p>
                <Badge className={`${levelColors[b.level]} border-0 text-[10px] mt-1`}>{b.level}</Badge>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Spacer for bottom nav */}
      <div className="h-8" />
    </AppShell>
  );
}
