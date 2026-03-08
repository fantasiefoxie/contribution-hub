import { useState } from "react";
import { Search } from "lucide-react";
import AppShell from "@/components/layout/AppShell";
import FilterChips, { type FilterCategory } from "@/components/mission/FilterChips";
import TaskCard, { type TaskCardData } from "@/components/mission/TaskCard";
import PassportToggle from "@/components/mission/PassportToggle";
import ImpactBanner from "@/components/mission/ImpactBanner";
import ActiveMissionBanner from "@/components/mission/ActiveMissionBanner";
import CommunityFeed from "@/components/impact/CommunityFeed";
import { useTasks } from "@/hooks/use-tasks";

// Mock data for display while API loads or returns empty
const mockTasks: TaskCardData[] = [
  {
    id: "demo-1",
    title: "Community Garden Cleanup",
    organization: "Green City Initiative",
    difficulty: 2,
    distance: "1.2 km",
    impact: "🌱 15 trees maintained",
    tags: ["environment", "outdoor"],
    status: "open",
    estimatedTime: "2h",
  },
  {
    id: "demo-2",
    title: "Survey Local Businesses",
    organization: "Downtown Alliance",
    difficulty: 3,
    distance: "0.8 km",
    impact: "📊 50 businesses mapped",
    tags: ["research", "community"],
    status: "open",
    estimatedTime: "3h",
  },
  {
    id: "demo-3",
    title: "Youth Coding Workshop Helper",
    organization: "TechBridge",
    difficulty: 4,
    distance: "3.5 km",
    impact: "🎓 20 students mentored",
    tags: ["community", "education"],
    status: "open",
    estimatedTime: "4h",
  },
  {
    id: "demo-4",
    title: "River Water Quality Testing",
    organization: "WaterWatch",
    difficulty: 3,
    distance: "5.1 km",
    impact: "💧 3 water samples collected",
    tags: ["environment", "research"],
    status: "open",
    estimatedTime: "1.5h",
  },
  {
    id: "demo-5",
    title: "Translate Health Flyers",
    organization: "Community Health Network",
    difficulty: 1,
    distance: undefined,
    impact: "📄 200 households reached",
    tags: ["community", "online"],
    status: "open",
    estimatedTime: "1h",
  },
];

export default function MissionFeed() {
  const [filter, setFilter] = useState<FilterCategory>("nearby");
  const [passportMode, setPassportMode] = useState(false);
  const [search, setSearch] = useState("");

  const { data, isLoading, isError } = useTasks({ status: "open" });
  const apiTasks: TaskCardData[] = (data?.tasks ?? []).map((t: any) => ({
    id: t.id,
    title: t.title,
    organization: undefined,
    difficulty: t.difficulty,
    distance: undefined,
    impact: undefined,
    tags: t.tags ?? [],
    status: t.status,
    estimatedTime: undefined,
  }));

  const showLoading = isLoading && !isError;
  const tasks = apiTasks.length > 0 ? apiTasks : mockTasks;

  const filtered = tasks.filter((t) => {
    if (search && !t.title.toLowerCase().includes(search.toLowerCase())) return false;
    if (filter === "online") return !t.distance || t.tags?.includes("online");
    if (filter === "environment") return t.tags?.includes("environment");
    if (filter === "community") return t.tags?.includes("community");
    if (filter === "research") return t.tags?.includes("research");
    return true;
  });

  return (
    <AppShell>
      {/* Header */}
      <div className="px-4 pt-6 pb-2">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="font-display text-2xl font-bold text-foreground">
              Mission Board
            </h1>
            <p className="text-sm text-muted-foreground mt-0.5">
              {passportMode ? "Exploring global tasks" : "Tasks near you"}
            </p>
          </div>
          <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
            <span className="text-lg">🎯</span>
          </div>
        </div>
      </div>

      {/* Impact Banner */}
      <ImpactBanner />

      {/* Search */}
      <div className="px-4 py-2">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search missions..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-muted border-0 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/30 transition-all"
          />
        </div>
      </div>

      {/* Passport Toggle */}
      <PassportToggle enabled={passportMode} onChange={setPassportMode} />

      {/* Filters */}
      <FilterChips active={filter} onChange={setFilter} />

      {/* Task List */}
      <div className="px-4 space-y-3 mt-2">
        {showLoading ? (
          Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="h-32 rounded-2xl bg-muted animate-pulse-soft" />
          ))
        ) : filtered.length === 0 ? (
          <div className="text-center py-16">
            <p className="text-4xl mb-3">🔍</p>
            <p className="text-muted-foreground text-sm">No missions found</p>
            <p className="text-muted-foreground text-xs mt-1">Try a different filter or enable Passport Mode</p>
          </div>
        ) : (
          filtered.map((task) => <TaskCard key={task.id} task={task} />)
        )}
      </div>

      {/* Community Activity Feed */}
      <CommunityFeed />

      {/* Spacer for bottom nav */}
      <div className="h-8" />
    </AppShell>
  );
}
