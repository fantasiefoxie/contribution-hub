import { useCommunityFeed } from "@/hooks/use-impact";
import { User, Leaf, Droplets, Users, MapPin } from "lucide-react";

interface FeedItem {
  id: string;
  score: number;
  impact_metrics: Record<string, unknown>;
  created_at: string;
  task: { title: string; tags: string[] };
  contributor: { display_name: string; avatar_url: string | null };
}

const mockFeed: FeedItem[] = [
  {
    id: "f-1", score: 20,
    impact_metrics: { waste_removed_kg: 15 },
    created_at: new Date(Date.now() - 1800000).toISOString(),
    task: { title: "Park cleanup drive", tags: ["environment"] },
    contributor: { display_name: "Aisha Khan", avatar_url: null },
  },
  {
    id: "f-2", score: 30,
    impact_metrics: { households_reached: 12 },
    created_at: new Date(Date.now() - 3600000).toISOString(),
    task: { title: "Census household survey", tags: ["research", "community"] },
    contributor: { display_name: "Ravi Sharma", avatar_url: null },
  },
  {
    id: "f-3", score: 40,
    impact_metrics: { trees_planted: 8 },
    created_at: new Date(Date.now() - 7200000).toISOString(),
    task: { title: "Urban reforestation", tags: ["environment"] },
    contributor: { display_name: "Carlos Ruiz", avatar_url: null },
  },
  {
    id: "f-4", score: 25,
    impact_metrics: { people_served: 30 },
    created_at: new Date(Date.now() - 10800000).toISOString(),
    task: { title: "Food distribution event", tags: ["community"] },
    contributor: { display_name: "Priya Patel", avatar_url: null },
  },
];

const tagIcons: Record<string, typeof Leaf> = {
  environment: Leaf,
  research: MapPin,
  community: Users,
};

function formatMetric(metrics: Record<string, unknown>): string {
  const entries = Object.entries(metrics).filter(([k, v]) => typeof v === "number" && k !== "difficulty");
  if (entries.length === 0) return "";
  const [key, val] = entries[0];
  return `${val} ${key.replace(/_/g, " ")}`;
}

function timeAgo(dateStr: string) {
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
}

export default function CommunityFeed() {
  const { data } = useCommunityFeed(10);
  const feed: FeedItem[] = data?.feed?.length > 0 ? data.feed : mockFeed;

  return (
    <div className="px-4 mt-4">
      <h2 className="font-display font-semibold text-sm mb-3 flex items-center gap-2">
        <Users className="h-4 w-4 text-primary" />
        Community Activity
      </h2>
      <div className="space-y-2">
        {feed.map((item) => {
          const metric = formatMetric(item.impact_metrics);
          const tag = item.task.tags?.[0];
          const Icon = tag ? tagIcons[tag] || User : User;

          return (
            <div key={item.id} className="flex items-start gap-3 bg-card/60 rounded-xl p-3 border border-border/50">
              <div className="h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center shrink-0 mt-0.5">
                <Icon className="h-4 w-4 text-primary" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs leading-relaxed">
                  <span className="font-semibold">{item.contributor.display_name}</span>
                  {" completed "}
                  <span className="font-medium">{item.task.title}</span>
                </p>
                {metric && (
                  <p className="text-[11px] text-primary font-medium mt-0.5">{metric}</p>
                )}
                <p className="text-[10px] text-muted-foreground mt-0.5">{timeAgo(item.created_at)}</p>
              </div>
              <span className="text-[10px] font-display font-bold text-secondary shrink-0">+{item.score}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
