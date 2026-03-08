import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  Search, Flag, Sparkles, MapPin, AlertTriangle, Globe, Loader2,
  Crosshair, Zap, ChevronRight, Clock, PlayCircle,
} from "lucide-react";
import ThemeToggle from "@/components/ThemeToggle";
import AppShell from "@/components/layout/AppShell";
import ScreenHeader from "@/components/shared/ScreenHeader";
import FilterChipsBar from "@/components/shared/FilterChipsBar";
import QuickActionBar from "@/components/shared/QuickActionBar";
import { StatusDot, DifficultyBadge, type UniversalStatus } from "@/components/shared/StatusIndicator";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { useMarketplaceFeed } from "@/hooks/use-recommendations";
import { useWalkthrough, hasCompletedWalkthrough } from "@/contexts/WalkthroughContext";
import WalkthroughTrigger from "@/components/walkthrough/WalkthroughTrigger";
import FilterChipsBar from "@/components/shared/FilterChipsBar";
import QuickActionBar from "@/components/shared/QuickActionBar";
import { StatusDot, DifficultyBadge, type UniversalStatus } from "@/components/shared/StatusIndicator";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { useMarketplaceFeed } from "@/hooks/use-recommendations";

// ── Mock data ──
const mockFeed = {
  recommended: [
    { id: "demo-1", title: "Community Garden Cleanup", description: "Help maintain the local community garden", difficulty: 2, tags: ["environment", "outdoor"], is_priority: false, _score: 72, _distance: 1.2, _reasons: ["nearby", "interest_match"], expires_at: null },
    { id: "demo-2", title: "Survey Local Businesses", description: "Map and document local businesses for community directory", difficulty: 3, tags: ["research", "community"], is_priority: false, _score: 65, _distance: 0.8, _reasons: ["nearby", "difficulty_match"], expires_at: null },
    { id: "demo-3", title: "Youth Coding Workshop Helper", description: "Mentor students during a weekend coding workshop", difficulty: 4, tags: ["community", "education"], is_priority: true, _score: 58, _distance: 3.5, _reasons: ["priority", "interest_match"], expires_at: null },
  ],
  nearby: [
    { id: "demo-1", title: "Community Garden Cleanup", description: "Help maintain the local community garden", difficulty: 2, tags: ["environment", "outdoor"], is_priority: false, _score: 72, _distance: 1.2, _reasons: ["nearby"], expires_at: null },
    { id: "demo-4", title: "River Water Quality Testing", description: "Collect water samples for environmental analysis", difficulty: 3, tags: ["environment", "research"], is_priority: false, _score: 45, _distance: 5.1, _reasons: ["medium_distance"], expires_at: null },
  ],
  priority: [
    { id: "demo-3", title: "Youth Coding Workshop Helper", description: "Mentor students during a weekend coding workshop", difficulty: 4, tags: ["community", "education"], is_priority: true, _score: 58, _distance: 3.5, _reasons: ["priority"], expires_at: null },
  ],
  online: [
    { id: "demo-5", title: "Translate Health Flyers", description: "Translate community health flyers to local languages", difficulty: 1, tags: ["community", "online"], is_priority: false, _score: 40, _distance: null, _reasons: ["online"], expires_at: null },
  ],
};

type FeedFilter = "recommended" | "nearby" | "priority" | "online";

const feedFilters = [
  { key: "recommended" as FeedFilter, label: "For You", icon: Sparkles },
  { key: "nearby" as FeedFilter, label: "Nearby", icon: MapPin },
  { key: "priority" as FeedFilter, label: "Priority", icon: AlertTriangle },
  { key: "online" as FeedFilter, label: "Online", icon: Globe },
];

// ── Active mission mock ──
const activeMission = {
  id: "demo-1",
  title: "Community Garden Cleanup",
  progress: 33,
  tasksLeft: 4,
  timeLeft: "2h 30m",
};

export default function MissionFeed() {
  const navigate = useNavigate();
  const [activeFilter, setActiveFilter] = useState<FeedFilter>("recommended");
  const [search, setSearch] = useState("");
  const { start: startWalkthrough, active: walkthroughActive } = useWalkthrough();

  // Auto-start walkthrough for first-time users
  useEffect(() => {
    if (!hasCompletedWalkthrough() && !walkthroughActive) {
      const timer = setTimeout(() => startWalkthrough(), 800);
      return () => clearTimeout(timer);
    }
  }, []);

  const { data, isLoading } = useMarketplaceFeed({ passport: false });
  const feed = (data?.recommended?.length > 0) ? data : mockFeed;

  const currentTasks = (feed[activeFilter] ?? []).filter((t: any) => {
    if (!search) return true;
    const q = search.toLowerCase();
    return t.title.toLowerCase().includes(q) || t.tags?.some((tag: string) => tag.includes(q));
  });

  return (
    <AppShell>
      {/* ═══ HEADER — See: context + progress ═══ */}
      <div className="px-4 pt-5 pb-2" data-walkthrough="welcome">
        <div className="flex items-center justify-between gap-3">
          <div className="flex-1 min-w-0">
            <h1 className="font-display text-xl font-bold leading-tight">Mission Board</h1>
            <p className="text-xs text-muted-foreground mt-0.5">Find your next task</p>
          </div>
          <div className="flex items-center gap-2">
            <ThemeToggle />
            <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
              <Crosshair className="h-5 w-5 text-primary" />
            </div>
          </div>
        </div>
      </div>

      {/* ── Active Mission Banner (one-tap resume) ── */}
      <button
        data-walkthrough="active-mission"
        onClick={() => navigate("/active")}
        className="mx-4 mb-3 bg-gradient-to-r from-secondary/15 via-secondary/10 to-primary/10 rounded-2xl p-3.5 border border-secondary/20 flex items-center gap-3 hover:border-secondary/40 transition-all"
      >
        <div className="h-10 w-10 rounded-full bg-secondary/20 flex items-center justify-center shrink-0">
          <Zap className="h-5 w-5 text-secondary" />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-[10px] uppercase tracking-wider font-semibold text-secondary">Active Mission</p>
          <p className="font-display font-semibold text-sm truncate mt-0.5">{activeMission.title}</p>
          <div className="flex items-center gap-3 mt-1.5">
            <span className="text-[10px] text-muted-foreground flex items-center gap-1">
              <Clock className="h-3 w-3" /> {activeMission.timeLeft}
            </span>
            <div className="flex-1 max-w-[80px]">
              <Progress value={activeMission.progress} className="h-1.5" />
            </div>
            <span className="text-[10px] font-medium text-primary">{activeMission.progress}%</span>
          </div>
        </div>
        <ChevronRight className="h-4 w-4 text-muted-foreground shrink-0" />
      </button>

      {/* ── Search (inline, fast) ── */}
      <div className="px-4 pb-2">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search tasks..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-muted border-0 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/30 transition-all"
          />
        </div>
      </div>

      {/* ═══ CONTENT — Decide: filter + scan ═══ */}
      <div className="px-4 mb-3">
        <FilterChipsBar<FeedFilter> options={feedFilters} active={activeFilter} onChange={setActiveFilter} />
      </div>

      {/* Task List */}
      {isLoading ? (
        <div className="flex justify-center py-12">
          <Loader2 className="h-6 w-6 animate-spin text-primary" />
        </div>
      ) : (
        <div className="px-4 space-y-2 pb-44">
          {currentTasks.length === 0 && (
            <div className="text-center py-10 text-muted-foreground">
              <Search className="h-8 w-8 mx-auto mb-2 opacity-40" />
              <p className="text-sm">No tasks match this filter</p>
            </div>
          )}
          {currentTasks.map((task: any, i: number) => (
            <MissionTaskCard key={task.id} task={task} index={i} />
          ))}
        </div>
      )}

      {/* ═══ BOTTOM — Act: persistent quick actions ═══ */}
      <QuickActionBar
        actions={[
          {
            icon: Zap,
            label: "Resume Mission",
            color: "text-secondary",
            bg: "bg-secondary/10",
            onClick: () => navigate("/active"),
          },
          {
            icon: Flag,
            label: "Initiatives",
            color: "text-accent",
            bg: "bg-accent/10",
            onClick: () => navigate("/initiatives"),
          },
          {
            icon: MapPin,
            label: "Near Me",
            color: "text-primary",
            bg: "bg-primary/10",
            onClick: () => setActiveFilter("nearby"),
          },
          {
            icon: Globe,
            label: "Online",
            color: "text-muted-foreground",
            bg: "bg-muted",
            onClick: () => setActiveFilter("online"),
          },
        ]}
      />
    </AppShell>
  );
}

// ── Task Card with See→Decide→Act pattern ──
function MissionTaskCard({ task, index }: { task: any; index: number }) {
  const navigate = useNavigate();
  const status: UniversalStatus = task.is_priority ? "in_progress" : "not_started";

  return (
    <button
      onClick={() => navigate(`/task/${task.id}`)}
      className="w-full bg-card rounded-xl border border-border p-3.5 text-left hover:border-primary/30 transition-all animate-fade-in min-h-[56px]"
      style={{ animationDelay: `${index * 30}ms` }}
    >
      <div className="flex items-center gap-3">
        <StatusDot status={status} />
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <p className="font-display font-semibold text-sm truncate">{task.title}</p>
            {task.is_priority && (
              <Badge className="bg-destructive/10 text-destructive border-0 text-[9px] shrink-0">Priority</Badge>
            )}
          </div>
          <div className="flex items-center gap-2 mt-0.5">
            <DifficultyBadge level={task.difficulty} />
            {task._distance !== null && (
              <span className="text-[10px] text-muted-foreground flex items-center gap-0.5">
                <MapPin className="h-2.5 w-2.5" />
                {task._distance < 1 ? `${Math.round(task._distance * 1000)}m` : `${task._distance.toFixed(1)}km`}
              </span>
            )}
            {task.tags?.slice(0, 2).map((tag: string) => (
              <span key={tag} className="text-[9px] text-muted-foreground bg-muted px-1.5 py-0 rounded">{tag}</span>
            ))}
          </div>
        </div>
        <ChevronRight className="h-4 w-4 text-muted-foreground shrink-0" />
      </div>
    </button>
  );
}
