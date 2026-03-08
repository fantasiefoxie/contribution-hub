import { User, Leaf, Users, MapPin } from "lucide-react";

interface ActivityItem {
  id: string;
  score: number;
  impact_metrics: Record<string, unknown>;
  created_at: string;
  task: { title: string; tags: string[] };
  contributor: { display_name: string; avatar_url: string | null };
}

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

interface Props {
  activities: ActivityItem[];
  isLoading?: boolean;
}

export default function InitiativeActivityFeed({ activities, isLoading }: Props) {
  if (isLoading) {
    return (
      <div className="space-y-2">
        {[1, 2, 3].map((i) => (
          <div key={i} className="h-16 rounded-xl bg-muted animate-pulse" />
        ))}
      </div>
    );
  }

  if (activities.length === 0) {
    return (
      <div className="text-center py-8">
        <p className="text-3xl mb-2">📭</p>
        <p className="text-xs text-muted-foreground">No activity yet. Be the first to contribute!</p>
      </div>
    );
  }

  return (
    <div className="space-y-2">
      {activities.map((item) => {
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
  );
}
