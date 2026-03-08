import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Compass, Users, Sparkles, ChevronRight, Loader2, TrendingUp, Star, Globe, MapPin, Heart, Clock, CheckCircle2, AlertTriangle, Zap } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import AppShell from "@/components/layout/AppShell";
import { useDiscoveryFeed } from "@/hooks/use-discovery";
import { useInitiatives } from "@/hooks/use-initiatives";

interface EnrichedInitiative {
  id: string;
  slug?: string;
  title: string;
  description: string | null;
  status: string;
  is_micro: boolean;
  is_online: boolean;
  start_date: string | null;
  end_date: string | null;
  target_metrics: Record<string, number>;
  featured_at: string | null;
  priority_score: number;
  participant_count: number;
  volunteer_target: number;
  volunteer_gap: number;
  task_stats: { total: number; open: number; validated: number };
  completion_pct: number;
  lifecycle: "upcoming" | "active" | "nearing_completion" | "completed";
  category: string | null;
  region_name: string | null;
  distance_km?: number;
  relevance?: number;
}

interface Section {
  key: string;
  title: string;
  icon: string;
  items: EnrichedInitiative[];
}

// Fallback mock data
const mockSections: Section[] = [
  {
    key: "featured", title: "Featured Campaigns", icon: "star",
    items: [
      { id: "init-1", title: "River Cleanup Campaign", description: "City-wide river cleanup targeting 50 pollution hotspots.", status: "active", is_micro: false, is_online: false, start_date: "2026-02-01", end_date: "2026-05-31", target_metrics: { waste_removed_kg: 500 }, featured_at: "2026-02-15", priority_score: 82, participant_count: 127, volunteer_target: 200, volunteer_gap: 73, task_stats: { total: 50, open: 18, validated: 24 }, completion_pct: 48, lifecycle: "active", category: "environment", region_name: "Downtown" },
      { id: "init-3", title: "Urban Reforestation Drive", description: "Plant 1000 trees across 15 neighborhoods.", status: "active", is_micro: false, is_online: false, start_date: "2026-01-15", end_date: "2026-06-30", target_metrics: { trees_planted: 1000 }, featured_at: "2026-01-20", priority_score: 71, participant_count: 89, volunteer_target: 150, volunteer_gap: 61, task_stats: { total: 40, open: 8, validated: 28 }, completion_pct: 70, lifecycle: "active", category: "environment", region_name: null },
    ],
  },
  {
    key: "high_priority", title: "High Priority", icon: "trending-up",
    items: [
      { id: "init-1", title: "River Cleanup Campaign", description: "City-wide river cleanup targeting 50 pollution hotspots.", status: "active", is_micro: false, is_online: false, start_date: "2026-02-01", end_date: "2026-05-31", target_metrics: { waste_removed_kg: 500 }, featured_at: "2026-02-15", priority_score: 82, participant_count: 127, volunteer_target: 200, volunteer_gap: 73, task_stats: { total: 50, open: 18, validated: 24 }, completion_pct: 48, lifecycle: "active", category: "environment", region_name: "Downtown" },
      { id: "init-4", title: "Food Distribution Network", description: "Weekly food distribution to underserved communities.", status: "active", is_micro: true, is_online: false, start_date: "2026-03-05", end_date: "2026-04-04", target_metrics: { people_served: 500 }, featured_at: null, priority_score: 76, participant_count: 12, volunteer_target: 60, volunteer_gap: 48, task_stats: { total: 15, open: 12, validated: 1 }, completion_pct: 7, lifecycle: "active", category: "welfare", region_name: null },
      { id: "init-2", title: "Ward 3 Census Survey", description: "Community-driven census of Ward 3 households.", status: "active", is_micro: true, is_online: false, start_date: "2026-03-01", end_date: "2026-03-30", target_metrics: { households_reached: 200 }, featured_at: null, priority_score: 68, participant_count: 18, volunteer_target: 40, volunteer_gap: 22, task_stats: { total: 20, open: 10, validated: 6 }, completion_pct: 30, lifecycle: "active", category: "governance", region_name: "Ward 3" },
    ],
  },
  {
    key: "community", title: "Community Campaigns", icon: "heart",
    items: [
      { id: "init-4", title: "Food Distribution Network", description: "Weekly food distribution to underserved communities.", status: "active", is_micro: true, is_online: false, start_date: "2026-03-05", end_date: "2026-04-04", target_metrics: { people_served: 500 }, featured_at: null, priority_score: 76, participant_count: 12, volunteer_target: 60, volunteer_gap: 48, task_stats: { total: 15, open: 12, validated: 1 }, completion_pct: 7, lifecycle: "active", category: "welfare", region_name: null },
      { id: "init-2", title: "Ward 3 Census Survey", description: "Community-driven census of Ward 3 households.", status: "active", is_micro: true, is_online: false, start_date: "2026-03-01", end_date: "2026-03-30", target_metrics: { households_reached: 200 }, featured_at: null, priority_score: 68, participant_count: 18, volunteer_target: 40, volunteer_gap: 22, task_stats: { total: 20, open: 10, validated: 6 }, completion_pct: 30, lifecycle: "active", category: "governance", region_name: "Ward 3" },
    ],
  },
  {
    key: "online", title: "Online Missions", icon: "globe",
    items: [
      { id: "init-5", title: "Digital Literacy Program", description: "Online workshops teaching digital skills to seniors.", status: "active", is_micro: false, is_online: true, start_date: "2026-02-10", end_date: "2026-08-30", target_metrics: { people_served: 300 }, featured_at: null, priority_score: 55, participant_count: 45, volunteer_target: 80, volunteer_gap: 35, task_stats: { total: 30, open: 15, validated: 10 }, completion_pct: 33, lifecycle: "active", category: "education", region_name: null },
    ],
  },
];

const SECTION_ICONS: Record<string, typeof Compass> = {
  star: Star,
  sparkles: Sparkles,
  "map-pin": MapPin,
  "trending-up": TrendingUp,
  heart: Heart,
  globe: Globe,
};

const LIFECYCLE_CONFIG: Record<string, { label: string; color: string; icon: typeof Clock }> = {
  upcoming: { label: "Upcoming", color: "bg-accent/10 text-accent border-accent/20", icon: Clock },
  active: { label: "Active", color: "bg-primary/10 text-primary border-primary/20", icon: Zap },
  nearing_completion: { label: "Almost Done", color: "bg-secondary/10 text-secondary border-secondary/20", icon: AlertTriangle },
  completed: { label: "Completed", color: "bg-muted text-muted-foreground border-border", icon: CheckCircle2 },
};

function LifecycleBadge({ lifecycle }: { lifecycle: string }) {
  const config = LIFECYCLE_CONFIG[lifecycle] || LIFECYCLE_CONFIG.active;
  const Icon = config.icon;
  return (
    <Badge className={`${config.color} border text-[9px] gap-0.5`}>
      <Icon className="h-2.5 w-2.5" /> {config.label}
    </Badge>
  );
}

function DemandBar({ current, target }: { current: number; target: number }) {
  const pct = Math.min(100, Math.round((current / target) * 100));
  const gap = target - current;
  return (
    <div className="mt-2">
      <div className="flex justify-between text-[10px] text-muted-foreground mb-0.5">
        <span className="flex items-center gap-1"><Users className="h-2.5 w-2.5" /> {current} / {target} volunteers</span>
        {gap > 0 && <span className="text-secondary font-medium">{gap} needed</span>}
      </div>
      <Progress value={pct} className="h-1.5" />
    </div>
  );
}

function CampaignCard({ init, onClick }: { init: EnrichedInitiative; onClick: () => void }) {
  return (
    <button onClick={onClick} className="w-full bg-card rounded-2xl border border-border p-4 text-left hover:border-primary/30 transition-colors">
      <div className="flex items-start justify-between gap-2">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-1.5 flex-wrap mb-1">
            <p className="font-display font-semibold text-sm truncate">{init.title}</p>
            {init.featured_at && (
              <Badge className="bg-secondary/10 text-secondary border-0 text-[9px] gap-0.5">
                <Star className="h-2.5 w-2.5 fill-current" /> Featured
              </Badge>
            )}
          </div>
          <div className="flex items-center gap-1.5 flex-wrap">
            <LifecycleBadge lifecycle={init.lifecycle} />
            {init.is_micro && <Badge className="bg-accent/10 text-accent border-0 text-[9px]">Micro</Badge>}
            {init.is_online && <Badge className="bg-info/10 text-accent border-0 text-[9px]">Online</Badge>}
            {init.category && <Badge variant="outline" className="text-[9px]">{init.category}</Badge>}
            {init.region_name && (
              <Badge variant="outline" className="text-[9px] gap-0.5"><MapPin className="h-2 w-2" />{init.region_name}</Badge>
            )}
          </div>
          <p className="text-xs text-muted-foreground mt-1.5 line-clamp-2">{init.description}</p>
        </div>
        <div className="flex flex-col items-end gap-1 shrink-0">
          <div className="bg-primary/10 text-primary rounded-lg px-2 py-1 text-center">
            <p className="font-display font-bold text-sm leading-none">{init.priority_score}</p>
            <p className="text-[8px] mt-0.5">priority</p>
          </div>
          <ChevronRight className="h-4 w-4 text-muted-foreground" />
        </div>
      </div>

      {/* Task progress */}
      {init.task_stats.total > 0 && (
        <div className="mt-3">
          <div className="flex justify-between text-[10px] text-muted-foreground mb-0.5">
            <span>{init.task_stats.validated} / {init.task_stats.total} tasks completed</span>
            <span>{init.completion_pct}%</span>
          </div>
          <Progress value={init.completion_pct} className="h-1" />
        </div>
      )}

      {/* Volunteer demand */}
      <DemandBar current={init.participant_count} target={init.volunteer_target} />

      {/* Distance if available */}
      {init.distance_km !== undefined && (
        <div className="flex items-center gap-1 mt-2 text-[10px] text-muted-foreground">
          <MapPin className="h-2.5 w-2.5" />
          <span>{init.distance_km < 1 ? "< 1" : Math.round(init.distance_km)} km away</span>
        </div>
      )}
    </button>
  );
}

function FeaturedCarousel({ items, onSelect }: { items: EnrichedInitiative[]; onSelect: (id: string) => void }) {
  return (
    <div className="flex gap-3 overflow-x-auto no-scrollbar pb-1">
      {items.map((init) => (
        <button
          key={init.id}
          onClick={() => onSelect(init.id)}
          className="min-w-[260px] bg-gradient-to-br from-primary/5 to-secondary/5 rounded-2xl border border-primary/15 p-4 text-left shrink-0 hover:border-primary/30 transition-colors"
        >
          <div className="flex items-center justify-between mb-1">
            <div className="flex items-center gap-1.5">
              <Star className="h-3 w-3 text-secondary fill-secondary" />
              <span className="text-[10px] font-semibold text-secondary uppercase tracking-wide">Featured</span>
            </div>
            <div className="bg-primary/10 text-primary rounded-md px-1.5 py-0.5">
              <span className="font-display font-bold text-[11px]">{init.priority_score}</span>
            </div>
          </div>
          <p className="font-display font-semibold text-sm">{init.title}</p>
          <p className="text-[11px] text-muted-foreground mt-0.5 line-clamp-1">{init.description}</p>

          <div className="mt-2">
            <div className="flex justify-between text-[10px] text-muted-foreground mb-0.5">
              <span>{init.participant_count} / {init.volunteer_target} volunteers</span>
              <span className="text-secondary font-medium">{init.volunteer_gap} needed</span>
            </div>
            <Progress value={Math.round((init.participant_count / init.volunteer_target) * 100)} className="h-1" />
          </div>

          <div className="flex items-center gap-2 mt-2">
            <LifecycleBadge lifecycle={init.lifecycle} />
            {init.completion_pct > 0 && (
              <span className="text-[10px] text-muted-foreground">{init.completion_pct}% done</span>
            )}
          </div>
        </button>
      ))}
    </div>
  );
}

type FilterType = "all" | "featured" | "high_priority" | "community" | "online" | "nearby";
const filters: { key: FilterType; label: string; icon: typeof Compass }[] = [
  { key: "all", label: "All", icon: Compass },
  { key: "featured", label: "Featured", icon: Star },
  { key: "high_priority", label: "Priority", icon: TrendingUp },
  { key: "community", label: "Community", icon: Heart },
  { key: "online", label: "Online", icon: Globe },
  { key: "nearby", label: "Nearby", icon: MapPin },
];

export default function InitiativeDiscovery() {
  const navigate = useNavigate();
  const [filter, setFilter] = useState<FilterType>("all");
  const [search, setSearch] = useState("");
  const { data: discoveryData, isLoading } = useDiscoveryFeed();

  const sections: Section[] = discoveryData?.sections?.length > 0 ? discoveryData.sections : mockSections;
  const allInitiatives: EnrichedInitiative[] = discoveryData?.all ?? sections.flatMap((s: Section) => s.items).filter((v: any, i: number, a: any[]) => a.findIndex((x: any) => x.id === v.id) === i);

  const filtered = allInitiatives.filter((i) => {
    if (search) {
      const q = search.toLowerCase();
      if (!i.title.toLowerCase().includes(q) && !i.description?.toLowerCase().includes(q)) return false;
    }
    if (filter === "featured") return !!i.featured_at;
    if (filter === "high_priority") return i.priority_score >= 60;
    if (filter === "community") return i.is_micro;
    if (filter === "online") return i.is_online;
    if (filter === "nearby") return i.distance_km !== undefined;
    return true;
  });

  const showSections = filter === "all" && !search;

  return (
    <AppShell>
      <div className="px-4 pt-6 pb-3">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="font-display text-2xl font-bold">Campaigns</h1>
            <p className="text-sm text-muted-foreground mt-0.5">Discover & join initiatives making impact</p>
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
          <Compass className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search campaigns..."
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

      {isLoading ? (
        <div className="flex justify-center py-16"><Loader2 className="h-6 w-6 animate-spin text-primary" /></div>
      ) : showSections ? (
        <div className="space-y-5 pb-24">
          {sections.map((section) => {
            const SectionIcon = SECTION_ICONS[section.icon] || Compass;
            return (
              <div key={section.key}>
                <h2 className="px-4 font-display font-semibold text-sm mb-2 flex items-center gap-1.5">
                  <SectionIcon className="h-4 w-4 text-primary" /> {section.title}
                </h2>
                {section.key === "featured" ? (
                  <div className="px-4">
                    <FeaturedCarousel items={section.items} onSelect={(id) => navigate(`/initiatives/${id}`)} />
                  </div>
                ) : (
                  <div className="px-4 space-y-2">
                    {section.items.slice(0, 3).map((init) => (
                      <CampaignCard key={init.id} init={init} onClick={() => navigate(`/initiatives/${init.id}`)} />
                    ))}
                    {section.items.length > 3 && (
                      <button
                        onClick={() => setFilter(section.key as FilterType)}
                        className="w-full text-center text-xs text-primary font-medium py-2 hover:underline"
                      >
                        View all {section.items.length} →
                      </button>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      ) : (
        <div className="px-4 space-y-3 pb-24">
          {filtered.length === 0 ? (
            <div className="text-center py-16">
              <p className="text-4xl mb-3">🏔️</p>
              <p className="text-muted-foreground text-sm">No campaigns found</p>
            </div>
          ) : (
            filtered.map((init) => (
              <CampaignCard key={init.id} init={init} onClick={() => navigate(`/initiatives/${init.id}`)} />
            ))
          )}
        </div>
      )}
    </AppShell>
  );
}
