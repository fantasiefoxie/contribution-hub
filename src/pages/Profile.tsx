import { User, Settings, LogOut, ChevronRight, Shield, MapPin } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import AppShell from "@/components/layout/AppShell";

const profileData = {
  name: "Alex Rivera",
  phone: "+1 (555) 123-4567",
  status: "Trusted Contributor",
  joined: "January 2025",
  location: "San Francisco, CA",
  avatar: "🧑‍🌾",
};

const menuItems = [
  { label: "Saved Tasks", icon: "📌", count: 3 },
  { label: "My Organizations", icon: "🏢", count: 2 },
  { label: "Notification Preferences", icon: "🔔", count: undefined },
  { label: "Privacy & Security", icon: "🔒", count: undefined },
  { label: "Help & Support", icon: "❓", count: undefined },
];

export default function Profile() {
  return (
    <AppShell>
      <div className="px-4 pt-6 pb-3">
        <h1 className="font-display text-2xl font-bold">Profile</h1>
      </div>

      {/* Profile card */}
      <div className="mx-4 bg-card rounded-2xl border border-border p-5 flex items-center gap-4">
        <div className="h-16 w-16 rounded-full bg-primary/10 flex items-center justify-center text-3xl shrink-0">
          {profileData.avatar}
        </div>
        <div className="flex-1 min-w-0">
          <h2 className="font-display font-bold text-lg">{profileData.name}</h2>
          <div className="flex items-center gap-1.5 mt-1">
            <Shield className="h-3 w-3 text-primary" />
            <Badge className="bg-primary/10 text-primary border-0 text-[10px]">
              {profileData.status}
            </Badge>
          </div>
          <div className="flex items-center gap-1 mt-1.5 text-xs text-muted-foreground">
            <MapPin className="h-3 w-3" />
            {profileData.location}
          </div>
        </div>
      </div>

      {/* Stats row */}
      <div className="flex gap-2 mx-4 mt-4">
        <div className="flex-1 bg-card rounded-xl border border-border p-3 text-center">
          <p className="font-display text-xl font-bold text-primary">24</p>
          <p className="text-[10px] text-muted-foreground">Tasks</p>
        </div>
        <div className="flex-1 bg-card rounded-xl border border-border p-3 text-center">
          <p className="font-display text-xl font-bold text-secondary">1,240</p>
          <p className="text-[10px] text-muted-foreground">Score</p>
        </div>
        <div className="flex-1 bg-card rounded-xl border border-border p-3 text-center">
          <p className="font-display text-xl font-bold text-accent">8</p>
          <p className="text-[10px] text-muted-foreground">Badges</p>
        </div>
      </div>

      {/* Member info */}
      <div className="mx-4 mt-4 bg-muted/50 rounded-xl p-3">
        <p className="text-xs text-muted-foreground">
          Member since <span className="font-medium text-foreground">{profileData.joined}</span>
        </p>
      </div>

      {/* Menu */}
      <div className="mx-4 mt-4 bg-card rounded-2xl border border-border overflow-hidden">
        {menuItems.map((item, i) => (
          <button
            key={item.label}
            className={`w-full flex items-center gap-3 px-4 py-3.5 hover:bg-muted/50 transition-colors ${
              i < menuItems.length - 1 ? "border-b border-border" : ""
            }`}
          >
            <span className="text-lg">{item.icon}</span>
            <span className="text-sm font-medium flex-1 text-left">{item.label}</span>
            {item.count !== undefined && (
              <span className="text-xs text-muted-foreground bg-muted px-2 py-0.5 rounded-full">
                {item.count}
              </span>
            )}
            <ChevronRight className="h-4 w-4 text-muted-foreground" />
          </button>
        ))}
      </div>

      {/* Sign out */}
      <div className="mx-4 mt-4 mb-4">
        <Button variant="outline" className="w-full rounded-xl h-11 text-destructive border-destructive/20 hover:bg-destructive/5 gap-2">
          <LogOut className="h-4 w-4" />
          Sign Out
        </Button>
      </div>
    </AppShell>
  );
}
