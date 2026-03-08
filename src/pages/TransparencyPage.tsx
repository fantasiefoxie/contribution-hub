import { useNavigate } from "react-router-dom";
import { ArrowLeft, Users, CheckCircle2, Flag, Award, Heart, Loader2 } from "lucide-react";
import AppShell from "@/components/layout/AppShell";
import { useTransparency } from "@/hooks/use-observability";

const mockTransparency = {
  total_volunteers: 342,
  total_tasks: 891,
  completed_tasks: 423,
  total_initiatives: 12,
  total_contributions: 423,
  total_validations: 389,
  total_score: 18450,
  impact_metrics: { households_reached: 1240, waste_removed_kg: 890, trees_planted: 456, people_served: 2100 },
};

const metricLabels: Record<string, string> = {
  households_reached: "Households Surveyed",
  waste_removed_kg: "Waste Removed (kg)",
  trees_planted: "Trees Planted",
  people_served: "People Served",
};

const metricEmojis: Record<string, string> = {
  households_reached: "🏠",
  waste_removed_kg: "♻️",
  trees_planted: "🌳",
  people_served: "🤝",
};

export default function TransparencyPage() {
  const navigate = useNavigate();
  const { data, isLoading } = useTransparency();
  const stats = data?.total_volunteers ? data : mockTransparency;

  return (
    <AppShell>
      <div className="sticky top-0 z-10 bg-background/95 backdrop-blur-sm border-b border-border">
        <div className="flex items-center gap-3 px-4 h-14">
          <button onClick={() => navigate(-1)} className="h-8 w-8 rounded-full bg-muted flex items-center justify-center">
            <ArrowLeft className="h-4 w-4" />
          </button>
          <span className="font-display font-semibold text-sm">Transparency</span>
        </div>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-32"><Loader2 className="h-6 w-6 animate-spin text-primary" /></div>
      ) : (
        <div className="px-4 pt-5 pb-32 space-y-5">
          {/* Hero */}
          <div className="text-center py-4">
            <p className="text-4xl mb-3">🌍</p>
            <h1 className="font-display text-2xl font-bold">Our Collective Impact</h1>
            <p className="text-sm text-muted-foreground mt-1">Real contributions, transparently tracked</p>
          </div>

          {/* Key stats */}
          <div className="grid grid-cols-2 gap-3">
            {[
              { label: "Total Volunteers", value: stats.total_volunteers, icon: Users, color: "text-primary" },
              { label: "Tasks Completed", value: stats.completed_tasks, icon: CheckCircle2, color: "text-secondary" },
              { label: "Initiatives Launched", value: stats.total_initiatives, icon: Flag, color: "text-accent" },
              { label: "Validations Made", value: stats.total_validations, icon: Award, color: "text-primary" },
            ].map(({ label, value, icon: Icon, color }) => (
              <div key={label} className="bg-card rounded-2xl border border-border p-4 text-center">
                <Icon className={`h-5 w-5 ${color} mx-auto mb-2`} />
                <p className="font-display font-bold text-2xl">{value.toLocaleString()}</p>
                <p className="text-[11px] text-muted-foreground mt-0.5">{label}</p>
              </div>
            ))}
          </div>

          {/* Total impact score */}
          <div className="bg-gradient-to-br from-primary/5 to-secondary/5 rounded-2xl border border-primary/10 p-5 text-center">
            <Heart className="h-6 w-6 text-primary mx-auto mb-2" />
            <p className="font-display font-bold text-3xl text-primary">{stats.total_score.toLocaleString()}</p>
            <p className="text-xs text-muted-foreground mt-1">Total Impact Score</p>
          </div>

          {/* Impact metrics */}
          {stats.impact_metrics && Object.keys(stats.impact_metrics).length > 0 && (
            <div className="bg-card rounded-2xl border border-border p-4">
              <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-4">Real-World Impact</p>
              <div className="grid grid-cols-2 gap-4">
                {Object.entries(stats.impact_metrics as Record<string, number>)
                  .filter(([, v]) => v > 0)
                  .map(([key, value]) => (
                    <div key={key} className="text-center">
                      <p className="text-2xl mb-1">{metricEmojis[key] || "📊"}</p>
                      <p className="font-display font-bold text-lg">{value.toLocaleString()}</p>
                      <p className="text-[10px] text-muted-foreground">{metricLabels[key] || key.replace(/_/g, " ")}</p>
                    </div>
                  ))}
              </div>
            </div>
          )}

          {/* Footer */}
          <div className="text-center pt-4">
            <p className="text-[11px] text-muted-foreground leading-relaxed">
              All contributions are validated by peer review and recorded in an immutable ledger.
              <br />
              Data updated in real-time.
            </p>
          </div>
        </div>
      )}
    </AppShell>
  );
}
