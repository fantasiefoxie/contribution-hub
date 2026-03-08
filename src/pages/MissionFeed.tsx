import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Search, Flag, Sparkles, MapPin, AlertTriangle, Globe, Loader2, Crosshair } from "lucide-react";
import ThemeToggle from "@/components/ThemeToggle";
import AppShell from "@/components/layout/AppShell";
import PassportToggle from "@/components/mission/PassportToggle";
import ImpactBanner from "@/components/mission/ImpactBanner";
import ActiveMissionBanner from "@/components/mission/ActiveMissionBanner";
import CommunityFeed from "@/components/impact/CommunityFeed";
import FeedSection from "@/components/recommendation/FeedSection";
import { useMarketplaceFeed } from "@/hooks/use-recommendations";

// Mock fallback data
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

export default function MissionFeed() {
  const navigate = useNavigate();
  const [passportMode, setPassportMode] = useState(false);
  const [search, setSearch] = useState("");

  const { data, isLoading } = useMarketplaceFeed({ passport: passportMode });
  const feed = (data?.recommended?.length > 0) ? data : mockFeed;

  const filterBySearch = (tasks: any[]) => {
    if (!search) return tasks;
    const q = search.toLowerCase();
    return tasks.filter((t: any) => t.title.toLowerCase().includes(q) || t.tags?.some((tag: string) => tag.includes(q)));
  };

  return (
    <AppShell>
      {/* Header */}
      <div className="px-4 pt-6 pb-2">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="font-display text-2xl font-bold text-foreground">Mission Board</h1>
            <p className="text-sm text-muted-foreground mt-0.5">
              {passportMode ? "Exploring global tasks" : "Smart recommendations for you"}
            </p>
          </div>
          <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
            <span className="text-lg">🎯</span>
          </div>
        </div>
      </div>

      {/* Active Mission Banner */}
      <ActiveMissionBanner
        taskId="demo-1"
        title="Community Garden Cleanup"
        lockedUntil={new Date(Date.now() + 2.5 * 3600000).toISOString()}
        updatesCount={2}
        totalSteps={6}
      />

      {/* Impact Banner */}
      <ImpactBanner />

      {/* Initiatives Quick Link */}
      <button
        onClick={() => navigate("/initiatives")}
        className="mx-4 mb-2 bg-gradient-to-r from-accent/10 to-primary/5 rounded-xl p-3 border border-accent/15 flex items-center gap-3 hover:border-accent/30 transition-colors"
      >
        <Flag className="h-4 w-4 text-accent" />
        <div className="flex-1">
          <p className="font-display font-semibold text-xs">Browse Initiatives</p>
          <p className="text-[10px] text-muted-foreground">Join campaigns making real impact</p>
        </div>
        <span className="text-xs text-primary font-medium">View →</span>
      </button>

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

      {/* Recommendation Sections */}
      {isLoading ? (
        <div className="flex justify-center py-12">
          <Loader2 className="h-6 w-6 animate-spin text-primary" />
        </div>
      ) : (
        <div className="mt-2">
          <FeedSection
            title="Recommended for You"
            icon={<Sparkles className="h-4 w-4 text-secondary" />}
            tasks={filterBySearch(feed.recommended ?? [])}
            emptyMessage="No recommendations yet — add interests in your profile!"
          />
          <FeedSection
            title="Nearby Tasks"
            icon={<MapPin className="h-4 w-4 text-primary" />}
            tasks={filterBySearch(feed.nearby ?? [])}
          />
          <FeedSection
            title="High Priority"
            icon={<AlertTriangle className="h-4 w-4 text-destructive" />}
            tasks={filterBySearch(feed.priority ?? [])}
          />
          <FeedSection
            title="Online Tasks"
            icon={<Globe className="h-4 w-4 text-accent" />}
            tasks={filterBySearch(feed.online ?? [])}
          />
        </div>
      )}

      {/* Community Activity Feed */}
      <CommunityFeed />

      <div className="h-8" />
    </AppShell>
  );
}
