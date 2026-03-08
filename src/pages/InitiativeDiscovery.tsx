import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Compass, Users, Sparkles, ChevronRight, Loader2, TrendingUp } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import AppShell from "@/components/layout/AppShell";
import { useInitiatives } from "@/hooks/use-initiatives";

interface InitiativeItem {
  id: string;
  title: string;
  description: string | null;
  status: string;
  is_micro: boolean;
  start_date: string | null;
  end_date: string | null;
  target_metrics: Record<string, number>;
  initiative_participants: { count: number }[];
}

const mockInitiatives: InitiativeItem[] = [
  {
    id: "init-1", title: "River Cleanup Campaign", description: "City-wide river cleanup initiative targeting 50 pollution hotspots.",
    status: "active", is_micro: false, start_date: "2026-02-01", end_date: "2026-05-31",
    target_metrics: { households_reached: 1000, waste_removed_kg: 500 },
    initiative_participants: [{ count: 127 }],
  },
  {
    id: "init-2", title: "Ward 3 Census Survey", description: "Community-driven census of Ward 3 households.",
    status: "active", is_micro: true, start_date: "2026-03-01", end_date: "2026-03-30",
    target_metrics: { households_reached: 200 },
    initiative_participants: [{ count: 18 }],
  },
  {
    id: "init-3", title: "Urban Reforestation Drive", description: "Plant 1000 trees across 15 neighborhoods.",
    status: "active", is_micro: false, start_date: "2026-01-15", end_date: "2026-06-30",
    target_metrics: { trees_planted: 1000 },
    initiative_participants: [{ count: 89 }],
  },
  {
    id: "init-4", title: "Food Distribution Network", description: "Weekly food distribution to underserved communities.",
    status: "active", is_micro: true, start_date: "2026-03-05", end_date: "2026-04-04",
    target_metrics: { people_served: 500 },
    initiative_participants: [{ count: 12 }],
  },
];

type FilterType = "all" | "active" | "micro";

export default function InitiativeDiscovery() {
  const navigate = useNavigate();
  const [filter, setFilter] = useState<FilterType>("all");
  const { data, isLoading } = useInitiatives({ status: "active" });

  const apiInitiatives = data?.initiatives ?? [];
  const initiatives: InitiativeItem[] = apiInitiatives.length > 0 ? apiInitiatives : mockInitiatives;

  const filtered = initiatives.filter((i) => {
    if (filter === "micro") return i.is_micro;
    if (filter === "active") return !i.is_micro;
    return true;
  });

  return (
    <AppShell>
      <div className="px-4 pt-6 pb-3">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="font-display text-2xl font-bold">Initiatives</h1>
            <p className="text-sm text-muted-foreground mt-0.5">Join campaigns making real impact</p>
          </div>
          <button
            onClick={() => navigate("/initiatives/create")}
            className="h-10 px-4 rounded-xl bg-primary text-primary-foreground text-xs font-semibold flex items-center gap-1.5"
          >
            <Sparkles className="h-3.5 w-3.5" /> Create
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className="flex gap-2 px-4 mb-4">
        {([
          { key: "all", label: "All", icon: Compass },
          { key: "active", label: "Campaigns", icon: TrendingUp },
          { key: "micro", label: "Micro", icon: Sparkles },
        ] as const).map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            onClick={() => setFilter(key)}
            className={`flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium transition-all ${
              filter === key
                ? "bg-primary text-primary-foreground"
                : "bg-muted text-muted-foreground hover:text-foreground"
            }`}
          >
            <Icon className="h-3.5 w-3.5" /> {label}
          </button>
        ))}
      </div>

      {/* List */}
      <div className="px-4 space-y-3 pb-24">
        {isLoading ? (
          <div className="flex justify-center py-16"><Loader2 className="h-6 w-6 animate-spin text-primary" /></div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-16">
            <p className="text-4xl mb-3">🏔️</p>
            <p className="text-muted-foreground text-sm">No initiatives found</p>
          </div>
        ) : (
          filtered.map((init) => {
            const participants = init.initiative_participants?.[0]?.count ?? 0;
            return (
              <button
                key={init.id}
                onClick={() => navigate(`/initiatives/${init.id}`)}
                className="w-full bg-card rounded-2xl border border-border p-4 text-left hover:border-primary/30 transition-colors"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="font-display font-semibold text-sm truncate">{init.title}</p>
                      {init.is_micro && (
                        <Badge className="bg-accent/10 text-accent border-0 text-[10px] shrink-0">Micro</Badge>
                      )}
                    </div>
                    <p className="text-xs text-muted-foreground mt-1 line-clamp-2">{init.description}</p>
                  </div>
                  <ChevronRight className="h-4 w-4 text-muted-foreground shrink-0 mt-1" />
                </div>

                {/* Metrics preview */}
                {init.target_metrics && Object.keys(init.target_metrics).length > 0 && (
                  <div className="mt-3 space-y-1.5">
                    {Object.entries(init.target_metrics).slice(0, 2).map(([key, target]) => (
                      <div key={key}>
                        <div className="flex justify-between text-[10px] text-muted-foreground mb-0.5">
                          <span className="capitalize">{key.replace(/_/g, " ")}</span>
                          <span>0 / {target}</span>
                        </div>
                        <Progress value={0} className="h-1" />
                      </div>
                    ))}
                  </div>
                )}

                <div className="flex items-center gap-3 mt-3 text-[10px] text-muted-foreground">
                  <div className="flex items-center gap-1">
                    <Users className="h-3 w-3" />
                    <span>{participants} volunteers</span>
                  </div>
                  {init.end_date && (
                    <span>Ends {new Date(init.end_date).toLocaleDateString()}</span>
                  )}
                </div>
              </button>
            );
          })
        )}
      </div>
    </AppShell>
  );
}
