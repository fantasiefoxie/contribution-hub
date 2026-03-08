import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { User, Settings, LogOut, ChevronRight, Shield, MapPin, Loader2, Crown, Star, Eye, Gavel, Users as UsersIcon, Bookmark, Building2, BellRing, Lock, HelpCircle, BarChart3, Globe, Code2, ShieldCheck } from "lucide-react";
import ThemeToggle from "@/components/ThemeToggle";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { toast } from "sonner";
import AppShell from "@/components/layout/AppShell";
import InterestSelector from "@/components/recommendation/InterestSelector";
import { useUserInterests, useUpdateInterests } from "@/hooks/use-recommendations";

const profileData = {
  name: "Alex Rivera",
  phone: "+1 (555) 123-4567",
  status: "Trusted Contributor",
  joined: "January 2025",
  location: "San Francisco, CA",
  avatar: null as null,
};

const menuItems = [
  { label: "Leadership & Roles", icon: "👑", count: undefined, href: "/leadership" },
  { label: "Platform Observability", icon: "📊", count: undefined, href: "/observability" },
  { label: "Transparency Report", icon: "🌍", count: undefined, href: "/transparency" },
  { label: "Developer Portal", icon: "🔌", count: undefined, href: "/developers" },
  { label: "Validation Queue", icon: "🛡️", count: undefined, href: "/validate" },
  { label: "Saved Tasks", icon: "📌", count: 3 },
  { label: "My Organizations", icon: "🏢", count: 2 },
  { label: "Notification Preferences", icon: "🔔", count: undefined },
  { label: "Privacy & Security", icon: "🔒", count: undefined },
  { label: "Help & Support", icon: "❓", count: undefined },
];

export default function Profile() {
  const navigate = useNavigate();
  const { data: interestsData, isLoading: loadingInterests } = useUserInterests();
  const updateMutation = useUpdateInterests();

  const [interests, setInterests] = useState<string[]>([]);
  const [dirty, setDirty] = useState(false);

  useEffect(() => {
    if (interestsData?.interests) {
      setInterests(interestsData.interests);
    }
  }, [interestsData]);

  const handleInterestChange = (newInterests: string[]) => {
    setInterests(newInterests);
    setDirty(true);
  };

  const saveInterests = async () => {
    try {
      await updateMutation.mutateAsync(interests);
      setDirty(false);
      toast.success("Interests updated!");
    } catch (e: any) {
      toast.error(e.message || "Failed to save");
    }
  };

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
          <div className="flex items-center gap-1.5 mt-1 flex-wrap">
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

      {/* Role capabilities hint */}
      <button
        onClick={() => navigate("/leadership")}
        className="mx-4 mt-3 bg-gradient-to-r from-primary/5 to-accent/5 rounded-xl p-3 border border-primary/10 flex items-center gap-3 hover:border-primary/20 transition-colors w-[calc(100%-2rem)]"
      >
        <Crown className="h-4 w-4 text-secondary" />
        <div className="flex-1 text-left">
          <p className="font-display font-semibold text-xs">Leadership Progression</p>
          <p className="text-[10px] text-muted-foreground">View your role, progress, and opportunities</p>
        </div>
        <ChevronRight className="h-4 w-4 text-muted-foreground" />
      </button>

      {/* Integrity & Stats row */}
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
          <p className="font-display text-xl font-bold text-accent">87</p>
          <p className="text-[10px] text-muted-foreground">Integrity</p>
        </div>
      </div>

      {/* Interests */}
      <div className="mx-4 mt-4 bg-card rounded-2xl border border-border p-4">
        <InterestSelector
          selected={interests}
          onChange={handleInterestChange}
          loading={updateMutation.isPending}
        />
        {dirty && (
          <Button
            onClick={saveInterests}
            disabled={updateMutation.isPending}
            size="sm"
            className="mt-3 rounded-xl"
          >
            {updateMutation.isPending ? <Loader2 className="h-3 w-3 animate-spin" /> : "Save Interests"}
          </Button>
        )}
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
            onClick={() => item.href && navigate(item.href)}
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
