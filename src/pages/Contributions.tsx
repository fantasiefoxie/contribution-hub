import { Trophy, Star, Target, Calendar, Award } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import AppShell from "@/components/layout/AppShell";

const stats = [
  { label: "Tasks Done", value: "24", icon: Target, color: "text-primary" },
  { label: "Impact Score", value: "1,240", icon: Star, color: "text-secondary" },
  { label: "Initiatives", value: "5", icon: Calendar, color: "text-accent" },
  { label: "Badges", value: "8", icon: Award, color: "text-primary" },
];

const badges = [
  { name: "Community Outreach", level: "Gold", emoji: "🤝", domain: "community" },
  { name: "Environmental Work", level: "Silver", emoji: "🌿", domain: "environment" },
  { name: "Survey Research", level: "Bronze", emoji: "📊", domain: "research" },
  { name: "Event Logistics", level: "Platinum", emoji: "🎪", domain: "logistics" },
];

const recentTasks = [
  { title: "Park Restoration", org: "Green City", date: "Mar 5", score: "+85" },
  { title: "Elderly Visits", org: "Community Care", date: "Mar 3", score: "+120" },
  { title: "Census Survey", org: "City Council", date: "Feb 28", score: "+95" },
  { title: "Beach Cleanup", org: "Ocean Watch", date: "Feb 25", score: "+110" },
];

const levelColors: Record<string, string> = {
  Bronze: "bg-orange-100 text-orange-700",
  Silver: "bg-gray-100 text-gray-700",
  Gold: "bg-yellow-100 text-yellow-700",
  Platinum: "bg-violet-100 text-violet-700",
};

export default function Contributions() {
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

      {/* Badges */}
      <div className="px-4 mt-6">
        <h2 className="font-display font-semibold text-base mb-3">Badges Earned</h2>
        <div className="grid grid-cols-2 gap-2">
          {badges.map((b) => (
            <div
              key={b.name}
              className="bg-card rounded-xl border border-border p-3 flex items-start gap-3"
            >
              <span className="text-2xl">{b.emoji}</span>
              <div className="min-w-0">
                <p className="font-medium text-xs truncate">{b.name}</p>
                <Badge className={`${levelColors[b.level]} border-0 text-[10px] mt-1`}>
                  {b.level}
                </Badge>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Recent tasks */}
      <div className="px-4 mt-6">
        <h2 className="font-display font-semibold text-base mb-3">Recent Contributions</h2>
        <div className="space-y-2">
          {recentTasks.map((t) => (
            <div
              key={t.title}
              className="bg-card rounded-xl border border-border p-3 flex items-center justify-between"
            >
              <div>
                <p className="font-medium text-sm">{t.title}</p>
                <p className="text-xs text-muted-foreground">
                  {t.org} · {t.date}
                </p>
              </div>
              <span className="text-sm font-display font-bold text-primary">{t.score}</span>
            </div>
          ))}
        </div>
      </div>
    </AppShell>
  );
}
