import { CheckCircle2, MapPin, Award, Megaphone, Clock, Bell } from "lucide-react";
import AppShell from "@/components/layout/AppShell";

type NotifType = "validation" | "nearby" | "badge" | "announcement";

interface Notification {
  id: string;
  type: NotifType;
  title: string;
  message: string;
  time: string;
  read: boolean;
}

const mockNotifications: Notification[] = [
  {
    id: "n1",
    type: "validation",
    title: "Task Approved",
    message: "Your submission for 'Community Garden Cleanup' was approved! +85 points",
    time: "2h ago",
    read: false,
  },
  {
    id: "n2",
    type: "badge",
    title: "New Badge Earned",
    message: "You earned the Silver Environmental Work badge 🌿",
    time: "5h ago",
    read: false,
  },
  {
    id: "n3",
    type: "nearby",
    title: "New Task Nearby",
    message: "'River Water Quality Testing' is 0.5 km from you",
    time: "1d ago",
    read: true,
  },
  {
    id: "n4",
    type: "announcement",
    title: "Spring Campaign Launch",
    message: "Green City Initiative has launched a new spring cleanup campaign with 15 tasks",
    time: "2d ago",
    read: true,
  },
  {
    id: "n5",
    type: "validation",
    title: "Clarification Requested",
    message: "The validator for 'Census Survey' has asked for additional photos",
    time: "3d ago",
    read: true,
  },
];

const icons: Record<NotifType, typeof CheckCircle2> = {
  validation: CheckCircle2,
  nearby: MapPin,
  badge: Award,
  announcement: Megaphone,
};

const iconColors: Record<NotifType, string> = {
  validation: "bg-primary/10 text-primary",
  nearby: "bg-accent/10 text-accent",
  badge: "bg-secondary/10 text-secondary",
  announcement: "bg-muted text-muted-foreground",
};

export default function Notifications() {
  return (
    <AppShell>
      <div className="px-4 pt-6 pb-3">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="font-display text-2xl font-bold">Notifications</h1>
            <p className="text-sm text-muted-foreground mt-0.5">Stay updated on your missions</p>
          </div>
          <div className="h-10 w-10 rounded-full bg-muted flex items-center justify-center">
            <span className="text-lg">🔔</span>
          </div>
        </div>
      </div>

      <div className="px-4 space-y-2 mt-2">
        {mockNotifications.map((n) => {
          const Icon = icons[n.type];
          return (
            <div
              key={n.id}
              className={`bg-card rounded-xl border p-4 flex items-start gap-3 transition-all ${
                n.read ? "border-border" : "border-primary/20 shadow-sm"
              }`}
            >
              <div className={`h-9 w-9 rounded-full flex items-center justify-center shrink-0 ${iconColors[n.type]}`}>
                <Icon className="h-4 w-4" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <p className="font-medium text-sm">{n.title}</p>
                  {!n.read && <div className="h-2 w-2 rounded-full bg-primary shrink-0" />}
                </div>
                <p className="text-xs text-muted-foreground mt-0.5 leading-relaxed">{n.message}</p>
                <p className="text-[10px] text-muted-foreground mt-1.5 flex items-center gap-1">
                  <Clock className="h-2.5 w-2.5" />
                  {n.time}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    </AppShell>
  );
}
