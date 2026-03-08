import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Trophy, Star, Target, Calendar, Award, TrendingUp,
  Home, Droplets, Users, Leaf, ChevronDown, ChevronUp, Zap,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import AppShell from "@/components/layout/AppShell";
import ScreenHeader from "@/components/shared/ScreenHeader";
import QuickActionBar from "@/components/shared/QuickActionBar";
import { StatusDot, type UniversalStatus } from "@/components/shared/StatusIndicator";
import InitiativeProgressCard from "@/components/impact/InitiativeProgressCard";
import { useMyImpact } from "@/hooks/use-impact";

const badges = [
  { name: "Community Outreach", level: "Gold", icon: Users, domain: "community" },
  { name: "Environmental Work", level: "Silver", icon: Leaf, domain: "environment" },
  { name: "Survey Research", level: "Bronze", icon: TrendingUp, domain: "research" },
  { name: "Event Logistics", level: "Platinum", icon: Calendar, domain: "logistics" },
];

const levelColors: Record<string, string> = {
  Bronze: "bg-secondary/10 text-secondary",
  Silver: "bg-muted text-muted-foreground",
  Gold: "bg-secondary/15 text-secondary",
  Platinum: "bg-accent/10 text-accent",
};

const metricIcons: Record<string, typeof Home> = {
  households_reached: Home,
  trees_planted: Leaf,
  waste_removed_kg: Droplets,
  people_served: Users,
};

// Mock recent activity
const recentTasks = [
  { id: "1", title: "Planted 15 trees in Ward 3", status: "validated" as UniversalStatus, date: "Today" },
  { id: "2", title: "Surveyed 20 households", status: "validated" as UniversalStatus, date: "Yesterday" },
  { id: "3", title: "Cleaned river bank section B", status: "submitted" as UniversalStatus, date: "Mar 5" },
  { id: "4", title: "Updated volunteer spreadsheet", status: "in_progress" as UniversalStatus, date: "Mar 4" },
];

const mockInitiatives = [
  {
    title: "River Cleanup Campaign",
    progress: { households_reached: { current: 480, target: 1000, percentage: 48 }, waste_removed_kg: { current: 320, target: 500, percentage: 64 } },
    milestones: [{ percentage: 25, reached_at: "2026-02-15" }],
  },
  {
    title: "Urban Reforestation Drive",
    progress: { trees_planted: { current: 750, target: 1000, percentage: 75 } },
    milestones: [{ percentage: 25, reached_at: "2026-01-10" }, { percentage: 50, reached_at: "2026-02-01" }, { percentage: 75, reached_at: "2026-03-05" }],
  },
];

export default function Contributions() {
  const navigate = useNavigate();
  const { data: impactData } = useMyImpact();
  const [showBadges, setShowBadges] = useState(false);
  const [showInitiatives, setShowInitiatives] = useState(false);

  const tasksCompleted = impactData?.tasks_completed ?? 24;
  const totalScore = impactData?.total_score ?? 1240;
  const initiativesCount = impactData?.initiatives_participated ?? 5;
  const impactMetrics = impactData?.impact_metrics ?? {
    households_reached: 156, trees_planted: 42, waste_removed_kg: 89, people_served: 210,
  };

  const stats = [
    { label: "Tasks Done", value: String(tasksCompleted), icon: Target, color: "text-primary" },
    { label: "Impact Score", value: totalScore.toLocaleString(), icon: Star, color: "text-secondary" },
    { label: "Initiatives", value: String(initiativesCount), icon: Calendar, color: "text-accent" },
    { label: "Badges", value: "8", icon: Award, color: "text-primary" },
  ];

  return (
    <AppShell>
      {/* ═══ HEADER — See: overall impact at a glance ═══ */}
      <ScreenHeader
        title="My Impact"
        subtitle="Your contribution journey"
        icon={Trophy}
        iconColor="text-primary"
      />

      {/* ── Stats grid (compact, scannable) ── */}
      <div className="grid grid-cols-4 gap-1.5 px-4">
        {stats.map(({ label, value, icon: Icon, color }) => (
          <div key={label} className="bg-card rounded-xl border border-border p-2.5 text-center">
            <Icon className={`h-4 w-4 ${color} mx-auto mb-1`} />
            <p className="font-display text-lg font-bold leading-none">{value}</p>
            <p className="text-[9px] text-muted-foreground mt-0.5">{label}</p>
          </div>
        ))}
      </div>

      {/* ═══ CONTENT — Decide: recent tasks as checklist ═══ */}
      <div className="px-4 mt-5">
        <h2 className="font-display font-semibold text-sm mb-2 flex items-center gap-2">
          <TrendingUp className="h-4 w-4 text-primary" />
          Recent Activity
        </h2>
        <div className="space-y-1.5">
          {recentTasks.map((task) => (
            <div key={task.id} className="bg-card rounded-xl border border-border p-3 flex items-center gap-3 min-h-[48px]">
              <StatusDot status={task.status} />
              <div className="flex-1 min-w-0">
                <p className={`text-sm font-medium leading-snug ${task.status === "validated" ? "text-muted-foreground" : "text-foreground"}`}>
                  {task.title}
                </p>
              </div>
              <span className="text-[10px] text-muted-foreground shrink-0">{task.date}</span>
            </div>
          ))}
        </div>
      </div>

      {/* ── Cumulative Impact (compact grid) ── */}
      <div className="px-4 mt-5">
        <h2 className="font-display font-semibold text-sm mb-2">Cumulative Impact</h2>
        <div className="grid grid-cols-2 gap-1.5">
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

      {/* ── Initiative Progress (progressive disclosure) ── */}
      <div className="px-4 mt-5">
        <button
          onClick={() => setShowInitiatives(!showInitiatives)}
          className="w-full flex items-center justify-between py-2"
        >
          <h2 className="font-display font-semibold text-sm">Initiative Progress</h2>
          {showInitiatives ? <ChevronUp className="h-4 w-4 text-muted-foreground" /> : <ChevronDown className="h-4 w-4 text-muted-foreground" />}
        </button>
        {showInitiatives && (
          <div className="space-y-3 animate-fade-in">
            {mockInitiatives.map((init) => (
              <InitiativeProgressCard key={init.title} title={init.title} progress={init.progress} milestones={init.milestones} />
            ))}
          </div>
        )}
      </div>

      {/* ── Badges (progressive disclosure) ── */}
      <div className="px-4 mt-4 pb-44">
        <button
          onClick={() => setShowBadges(!showBadges)}
          className="w-full flex items-center justify-between py-2"
        >
          <h2 className="font-display font-semibold text-sm">Badges Earned</h2>
          {showBadges ? <ChevronUp className="h-4 w-4 text-muted-foreground" /> : <ChevronDown className="h-4 w-4 text-muted-foreground" />}
        </button>
        {showBadges && (
          <div className="grid grid-cols-2 gap-2 animate-fade-in">
            {badges.map((b) => {
              const BIcon = b.icon;
              return (
                <div key={b.name} className="bg-card rounded-xl border border-border p-3 flex items-start gap-3">
                  <div className="h-9 w-9 rounded-full bg-primary/10 flex items-center justify-center shrink-0">
                    <BIcon className="h-4 w-4 text-primary" />
                  </div>
                  <div className="min-w-0">
                    <p className="font-medium text-xs truncate">{b.name}</p>
                    <Badge className={`${levelColors[b.level]} border-0 text-[10px] mt-1`}>{b.level}</Badge>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* ═══ BOTTOM — Act: quick actions ═══ */}
      <QuickActionBar
        actions={[
          { icon: Zap, label: "Resume Task", color: "text-secondary", bg: "bg-secondary/10", onClick: () => navigate("/active") },
          { icon: Target, label: "Find Task", color: "text-primary", bg: "bg-primary/10", onClick: () => navigate("/") },
          { icon: Award, label: "Badges", color: "text-accent", bg: "bg-accent/10", onClick: () => setShowBadges(true) },
          { icon: TrendingUp, label: "Initiatives", color: "text-muted-foreground", bg: "bg-muted", onClick: () => navigate("/initiatives") },
        ]}
      />
    </AppShell>
  );
}
