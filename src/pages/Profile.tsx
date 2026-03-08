import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  User, Settings, LogOut, ChevronRight, ChevronDown, ChevronUp,
  Shield, MapPin, Loader2, Crown, Star, Bookmark, Building2,
  BellRing, Lock, HelpCircle, BarChart3, Globe, Code2, ShieldCheck,
  Target, Zap,
} from "lucide-react";
import ThemeToggle from "@/components/ThemeToggle";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { toast } from "sonner";
import AppShell from "@/components/layout/AppShell";
import ScreenHeader from "@/components/shared/ScreenHeader";
import QuickActionBar from "@/components/shared/QuickActionBar";
import InterestSelector from "@/components/recommendation/InterestSelector";
import { useUserInterests, useUpdateInterests } from "@/hooks/use-recommendations";

const profileData = {
  name: "Rahul D",
  phone: "+91 98XXX XXXXX",
  status: "Trusted Contributor",
  joined: "January 2025",
  location: "Navi Mumbai, MH",
};

const quickLinks = [
  { label: "Leadership & Roles", icon: Crown, href: "/leadership" },
  { label: "Validation Queue", icon: ShieldCheck, href: "/validate" },
  { label: "Developer Portal", icon: Code2, href: "/developers" },
];

const settingsMenu = [
  { label: "Platform Observability", icon: BarChart3, href: "/observability" },
  { label: "Transparency Report", icon: Globe, href: "/transparency" },
  { label: "Saved Tasks", icon: Bookmark, count: 3, href: "/saved" },
  { label: "My Organizations", icon: Building2, count: 2, href: "/organizations" },
  { label: "Notification Preferences", icon: BellRing, href: "/notifications" },
  { label: "Privacy & Security", icon: Lock, action: "privacy" },
  { label: "Help & Support", icon: HelpCircle, action: "help" },
];

export default function Profile() {
  const navigate = useNavigate();
  const { data: interestsData } = useUserInterests();
  const updateMutation = useUpdateInterests();

  const [interests, setInterests] = useState<string[]>([]);
  const [dirty, setDirty] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [showInterests, setShowInterests] = useState(false);

  useEffect(() => {
    if (interestsData?.interests) setInterests(interestsData.interests);
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
      {/* ═══ HEADER — See: identity at a glance ═══ */}
      <div className="px-4 pt-5 pb-3 flex items-center justify-between">
        <h1 className="font-display text-xl font-bold">Profile</h1>
        <ThemeToggle />
      </div>

      {/* Profile card (compact) */}
      <div className="mx-4 bg-card rounded-2xl border border-border p-4 flex items-center gap-3">
        <div className="h-14 w-14 rounded-full bg-primary/10 flex items-center justify-center shrink-0">
          <User className="h-6 w-6 text-primary" />
        </div>
        <div className="flex-1 min-w-0">
          <h2 className="font-display font-bold text-base">{profileData.name}</h2>
          <div className="flex items-center gap-1.5 mt-0.5">
            <Shield className="h-3 w-3 text-primary" />
            <Badge className="bg-primary/10 text-primary border-0 text-[10px]">{profileData.status}</Badge>
          </div>
          <div className="flex items-center gap-1 mt-1 text-[11px] text-muted-foreground">
            <MapPin className="h-3 w-3" /> {profileData.location}
          </div>
        </div>
      </div>

      {/* ── Quick stats row ── */}
      <div className="flex gap-1.5 mx-4 mt-3">
        <div className="flex-1 bg-card rounded-xl border border-border p-2.5 text-center">
          <p className="font-display text-lg font-bold text-primary">24</p>
          <p className="text-[9px] text-muted-foreground">Tasks</p>
        </div>
        <div className="flex-1 bg-card rounded-xl border border-border p-2.5 text-center">
          <p className="font-display text-lg font-bold text-secondary">1,240</p>
          <p className="text-[9px] text-muted-foreground">Score</p>
        </div>
        <div className="flex-1 bg-card rounded-xl border border-border p-2.5 text-center">
          <p className="font-display text-lg font-bold text-accent">87</p>
          <p className="text-[9px] text-muted-foreground">Integrity</p>
        </div>
      </div>

      {/* ═══ CONTENT — Decide: quick links (most-used actions) ═══ */}
      <div className="px-4 mt-4">
        <h2 className="font-display font-semibold text-xs text-muted-foreground uppercase tracking-wider mb-2">Quick Access</h2>
        <div className="grid grid-cols-3 gap-1.5">
          {quickLinks.map((item) => (
            <button
              key={item.label}
              onClick={() => navigate(item.href)}
              className="bg-card rounded-xl border border-border p-3 flex flex-col items-center gap-1.5 hover:border-primary/30 transition-colors active:scale-95"
            >
              <div className="h-9 w-9 rounded-lg bg-primary/10 flex items-center justify-center">
                <item.icon className="h-4 w-4 text-primary" />
              </div>
              <span className="text-[10px] font-medium text-center leading-tight">{item.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* ── Interests (progressive disclosure) ── */}
      <div className="mx-4 mt-4">
        <button
          onClick={() => setShowInterests(!showInterests)}
          className="w-full bg-card rounded-xl border border-border p-3 flex items-center justify-between"
        >
          <span className="font-display font-semibold text-sm">My Interests</span>
          {showInterests ? <ChevronUp className="h-4 w-4 text-muted-foreground" /> : <ChevronDown className="h-4 w-4 text-muted-foreground" />}
        </button>
        {showInterests && (
          <div className="bg-card border border-t-0 border-border rounded-b-xl p-3 animate-fade-in">
            <InterestSelector selected={interests} onChange={handleInterestChange} loading={updateMutation.isPending} />
            {dirty && (
              <Button onClick={saveInterests} disabled={updateMutation.isPending} size="sm" className="mt-3 rounded-xl">
                {updateMutation.isPending ? <Loader2 className="h-3 w-3 animate-spin" /> : "Save Interests"}
              </Button>
            )}
          </div>
        )}
      </div>

      {/* ── Settings menu (progressive disclosure) ── */}
      <div className="mx-4 mt-4">
        <button
          onClick={() => setShowSettings(!showSettings)}
          className="w-full flex items-center justify-between py-2"
        >
          <h2 className="font-display font-semibold text-xs text-muted-foreground uppercase tracking-wider">Settings & More</h2>
          {showSettings ? <ChevronUp className="h-4 w-4 text-muted-foreground" /> : <ChevronDown className="h-4 w-4 text-muted-foreground" />}
        </button>
        {showSettings && (
          <div className="bg-card rounded-2xl border border-border overflow-hidden animate-fade-in">
            {settingsMenu.map((item, i) => (
              <button
                key={item.label}
                onClick={() => item.href && navigate(item.href)}
                className={`w-full flex items-center gap-3 px-4 py-3 hover:bg-muted/50 transition-colors ${
                  i < settingsMenu.length - 1 ? "border-b border-border" : ""
                }`}
              >
                <div className="h-7 w-7 rounded-lg bg-muted flex items-center justify-center shrink-0">
                  <item.icon className="h-3.5 w-3.5 text-muted-foreground" />
                </div>
                <span className="text-sm font-medium flex-1 text-left">{item.label}</span>
                {item.count !== undefined && (
                  <span className="text-[10px] text-muted-foreground bg-muted px-2 py-0.5 rounded-full">{item.count}</span>
                )}
                <ChevronRight className="h-4 w-4 text-muted-foreground" />
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Sign out */}
      <div className="mx-4 mt-4 pb-44">
        <Button variant="outline" className="w-full rounded-xl h-11 text-destructive border-destructive/20 hover:bg-destructive/5 gap-2">
          <LogOut className="h-4 w-4" /> Sign Out
        </Button>
      </div>

      {/* ═══ BOTTOM — Act: persistent actions ═══ */}
      <QuickActionBar
        actions={[
          { icon: Zap, label: "Active Task", color: "text-secondary", bg: "bg-secondary/10", onClick: () => navigate("/active") },
          { icon: Target, label: "Find Task", color: "text-primary", bg: "bg-primary/10", onClick: () => navigate("/") },
          { icon: Settings, label: "Edit Profile", color: "text-muted-foreground", bg: "bg-muted", onClick: () => toast.info("Profile editing coming soon") },
        ]}
      />
    </AppShell>
  );
}
