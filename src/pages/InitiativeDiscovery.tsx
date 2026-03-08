import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Compass, Users, Sparkles, ChevronRight, Loader2, TrendingUp, Star, Globe, MapPin, Heart } from "lucide-react";
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
  promoted_at: string | null;
}

const mockInitiatives: InitiativeItem[] = [
  {
    id: "init-1", title: "River Cleanup Campaign", description: "City-wide river cleanup initiative targeting 50 pollution hotspots.",
    status: "active", is_micro: false, start_date: "2026-02-01", end_date: "2026-05-31",
    target_metrics: { households_reached: 1000, waste_removed_kg: 500 },
    initiative_participants: [{ count: 127 }], promoted_at: "2026-02-15T00:00:00Z",
  },
  {
    id: "init-2", title: "Ward 3 Census Survey", description: "Community-driven census of Ward 3 households.",
    status: "active", is_micro: true, start_date: "2026-03-01", end_date: "2026-03-30",
    target_metrics: { households_reached: 200 },
    initiative_participants: [{ count: 18 }], promoted_at: null,
  },
  {
    id: "init-3", title: "Urban Reforestation Drive", description: "Plant 1000 trees across 15 neighborhoods.",
    status: "active", is_micro: false, start_date: "2026-01-15", end_date: "2026-06-30",
    target_metrics: { trees_planted: 1000 },
    initiative_participants: [{ count: 89 }], promoted_at: "2026-01-20T00:00:00Z",
  },
  {
    id: "init-4", title: "Food Distribution Network", description: "Weekly food distribution to underserved communities.",
    status: "active", is_micro: true, start_date: "2026-03-05", end_date: "2026-04-04",
    target_metrics: { people_served: 500 },
    initiative_participants: [{ count: 12 }], promoted_at: null,
  },
  {
    id: "init-5", title: "Digital Literacy Program", description: "Online workshops teaching digital skills to seniors.",
    status: "active", is_micro: false, start_date: "2026-02-10", end_date: "2026-08-30",
    target_metrics: { people_served: 300 },
    initiative_participants: [{ count: 45 }], promoted_at: null,
  },
];

type FilterType = "all" | "featured" | "campaigns" | "micro" | "online" | "community";

const filters: { key: FilterType; label: string; icon: typeof Compass }[] = [
  { key: "all", label: "All", icon: Compass },
  { key: "featured", label: "Featured", icon: Star },
  { key: "campaigns", label: "Campaigns", icon: TrendingUp },
  { key: "micro", label: "Micro", icon: Sparkles },
  { key: "online", label: "Online", icon: Globe },
  { key: "community", label: "Community", icon: Heart },
];

function InitiativeCard({ init, onClick }: { init: InitiativeItem; onClick: () => void }) {
  const participants = init.initiative_participants?.[0]?.count ?? 0;
  const isFeatured = !!init.promoted_at;

  return (
    <button onClick={onClick} className="w-full bg-card rounded-2xl border border-border p-4 text-left hover:border-primary/30 transition-colors">
      <div className="flex items-start justify-between gap-3">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <p className="font-display font-semibold text-sm truncate">{init.title}</p>
            {isFeatured && (
              <Badge className="bg-secondary/10 text-secondary border-0 text-[10px] shrink-0 gap-0.5">
                <Star className="h-2.5 w-2.5 fill-current" /> Featured
              </Badge>
            )}
            {init.is_micro && (
              <Badge className="bg-accent/10 text-accent border-0 text-[10px] shrink-0">Micro</Badge>
            )}
          </div>
          <p className="text-xs text-muted-foreground mt-1 line-clamp-2">{init.description}</p>
        </div>
        <ChevronRight className="h-4 w-4 text-muted-foreground shrink-0 mt-1" />
      </div>

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
}

export default function InitiativeDiscovery() {
  const navigate = useNavigate();
  const [filter, setFilter] = useState<FilterType>("all");
  const [search, setSearch] = useState("");
  const { data, isLoading } = useInitiatives({ status: "active" });

  const apiInitiatives = data?.initiatives ?? [];
  const initiatives: InitiativeItem[] = apiInitiatives.length > 0 ? apiInitiatives : mockInitiatives;

  const featured = initiatives.filter((i) => !!i.promoted_at);

  const filtered = initiatives.filter((i) => {
    if (search) {
      const q = search.toLowerCase();
      if (!i.title.toLowerCase().includes(q) && !i.description?.toLowerCase().includes(q)) return false;
    }
    if (filter === "featured") return !!i.promoted_at;
    if (filter === "micro") return i.is_micro;
    if (filter === "campaigns") return !i.is_micro;
    if (filter === "community") return i.is_micro; // community-created
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

      {/* Search */}
      <div className="px-4 mb-3">
        <div className="relative">
          <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search initiatives..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-muted border-0 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/30 transition-all"
          />
        </div>
      </div>

      {/* Filters */}
      <div className="flex gap-2 px-4 mb-4 overflow-x-auto no-scrollbar">
        {filters.map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            onClick={() => setFilter(key)}
            className={`flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium whitespace-nowrap transition-all ${
              filter === key
                ? "bg-primary text-primary-foreground"
                : "bg-muted text-muted-foreground hover:text-foreground"
            }`}
          >
            <Icon className="h-3.5 w-3.5" /> {label}
          </button>
        ))}
      </div>

      {/* Featured Section */}
      {filter === "all" && featured.length > 0 && !search && (
        <div className="px-4 mb-4">
          <h2 className="font-display font-semibold text-sm mb-2 flex items-center gap-1.5">
            <Star className="h-4 w-4 text-secondary" /> Featured Initiatives
          </h2>
          <div className="flex gap-3 overflow-x-auto no-scrollbar pb-1">
            {featured.map((init) => {
              const participants = init.initiative_participants?.[0]?.count ?? 0;
              return (
                <button
                  key={init.id}
                  onClick={() => navigate(`/initiatives/${init.id}`)}
                  className="min-w-[240px] bg-gradient-to-br from-primary/5 to-secondary/5 rounded-2xl border border-primary/15 p-4 text-left shrink-0 hover:border-primary/30 transition-colors"
                >
                  <div className="flex items-center gap-1.5 mb-1">
                    <Star className="h-3 w-3 text-secondary fill-secondary" />
                    <span className="text-[10px] font-semibold text-secondary uppercase tracking-wide">Featured</span>
                  </div>
                  <p className="font-display font-semibold text-sm">{init.title}</p>
                  <p className="text-[11px] text-muted-foreground mt-0.5 line-clamp-1">{init.description}</p>
                  <div className="flex items-center gap-2 mt-2 text-[10px] text-muted-foreground">
                    <Users className="h-3 w-3" />
                    <span>{participants} volunteers</span>
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      )}

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
          filtered.map((init) => (
            <InitiativeCard key={init.id} init={init} onClick={() => navigate(`/initiatives/${init.id}`)} />
          ))
        )}
      </div>
    </AppShell>
  );
}
