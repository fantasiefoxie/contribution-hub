import { useState } from "react";
import { useNavigate } from "react-router-dom";
import AppShell from "@/components/layout/AppShell";
import ScreenHeader from "@/components/shared/ScreenHeader";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  ShieldCheck,
  Users,
  ClipboardCheck,
  Activity,
  TrendingUp,
  AlertTriangle,
  Search,
  ChevronRight,
  UserCog,
  Flag,
  Eye,
  CheckCircle2,
  XCircle,
  Clock,
  BarChart3,
  ArrowUpRight,
  ArrowDownRight,
} from "lucide-react";

/* ── mock data ── */

const platformStats = [
  { label: "Total Users", value: "2,847", change: "+12%", up: true, icon: Users },
  { label: "Active Tasks", value: "342", change: "+8%", up: true, icon: ClipboardCheck },
  { label: "Contributions", value: "1,206", change: "+23%", up: true, icon: TrendingUp },
  { label: "Pending Reviews", value: "47", change: "-5%", up: false, icon: Activity },
];

const systemHealth = [
  { label: "Edge Functions", status: "healthy" as const },
  { label: "Database", status: "healthy" as const },
  { label: "Auth Service", status: "healthy" as const },
  { label: "Webhook Delivery", status: "degraded" as const },
];

const mockUsers = [
  { id: "1", name: "Elena Vasquez", email: "elena@example.com", role: "admin", status: "trusted", tasks: 34, joined: "2025-08-12" },
  { id: "2", name: "Marcus Chen", email: "marcus@example.com", role: "validator", status: "trusted", tasks: 67, joined: "2025-09-03" },
  { id: "3", name: "Aisha Okonkwo", email: "aisha@example.com", role: "moderator", status: "trusted", tasks: 22, joined: "2025-10-18" },
  { id: "4", name: "James Park", email: "james@example.com", role: "member", status: "registered", tasks: 5, joined: "2026-01-07" },
  { id: "5", name: "Sofia Larsson", email: "sofia@example.com", role: "member", status: "registered", tasks: 12, joined: "2025-11-22" },
  { id: "6", name: "David Osei", email: "david@example.com", role: "validator", status: "trusted", tasks: 41, joined: "2025-09-15" },
];

const flaggedItems = [
  { id: "f1", type: "submission", title: "Duplicate content in river cleanup report", user: "James Park", severity: "medium", createdAt: "2h ago" },
  { id: "f2", type: "appeal", title: "Rejected validation appeal — tree planting photos", user: "Sofia Larsson", severity: "high", createdAt: "4h ago" },
  { id: "f3", type: "task", title: "Spam task: \"Buy crypto now\"", user: "Unknown", severity: "critical", createdAt: "6h ago" },
  { id: "f4", type: "submission", title: "Low-quality submission for data entry task", user: "David Osei", severity: "low", createdAt: "1d ago" },
];

const recentAudit = [
  { action: "Role granted", detail: "validator → Marcus Chen", actor: "Elena Vasquez", time: "12m ago" },
  { action: "Task removed", detail: "Spam task deleted", actor: "Aisha Okonkwo", time: "1h ago" },
  { action: "Appeal resolved", detail: "Approved re-review for #1284", actor: "Elena Vasquez", time: "3h ago" },
  { action: "Credential issued", detail: "Gold badge → Marcus Chen", actor: "System", time: "5h ago" },
];

/* ── helpers ── */

const roleColor: Record<string, string> = {
  admin: "bg-destructive/15 text-destructive",
  moderator: "bg-accent/15 text-accent-foreground",
  validator: "bg-primary/15 text-primary",
  member: "bg-muted text-muted-foreground",
};

const severityColor: Record<string, string> = {
  critical: "bg-destructive/15 text-destructive",
  high: "bg-secondary/15 text-secondary",
  medium: "bg-accent/15 text-accent-foreground",
  low: "bg-muted text-muted-foreground",
};

const healthDot: Record<string, string> = {
  healthy: "bg-primary",
  degraded: "bg-secondary",
  down: "bg-destructive",
};

/* ── component ── */

export default function AdminDashboard() {
  const navigate = useNavigate();
  const [userSearch, setUserSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState("all");

  const filteredUsers = mockUsers.filter((u) => {
    const matchSearch = u.name.toLowerCase().includes(userSearch.toLowerCase()) || u.email.toLowerCase().includes(userSearch.toLowerCase());
    const matchRole = roleFilter === "all" || u.role === roleFilter;
    return matchSearch && matchRole;
  });

  return (
    <AppShell>
      <ScreenHeader title="Admin Panel" subtitle="Platform management" icon={ShieldCheck} />

      <div className="px-4 space-y-4 pb-4">
        <Tabs defaultValue="overview" className="w-full">
          <TabsList className="w-full grid grid-cols-3 h-9">
            <TabsTrigger value="overview" className="text-xs">
              <BarChart3 className="h-3.5 w-3.5 mr-1" />
              Overview
            </TabsTrigger>
            <TabsTrigger value="users" className="text-xs">
              <UserCog className="h-3.5 w-3.5 mr-1" />
              Users
            </TabsTrigger>
            <TabsTrigger value="moderation" className="text-xs">
              <Flag className="h-3.5 w-3.5 mr-1" />
              Moderation
            </TabsTrigger>
          </TabsList>

          {/* ─── OVERVIEW TAB ─── */}
          <TabsContent value="overview" className="mt-3 space-y-4">
            {/* Stat cards */}
            <div className="grid grid-cols-2 gap-3">
              {platformStats.map((s) => (
                <Card key={s.label} className="p-3 space-y-1.5">
                  <div className="flex items-center justify-between">
                    <span className="text-[11px] font-medium text-muted-foreground uppercase tracking-wide">{s.label}</span>
                    <s.icon className="h-3.5 w-3.5 text-muted-foreground" />
                  </div>
                  <p className="font-display text-xl font-bold">{s.value}</p>
                  <span className={`text-[11px] font-medium flex items-center gap-0.5 ${s.up ? "text-primary" : "text-destructive"}`}>
                    {s.up ? <ArrowUpRight className="h-3 w-3" /> : <ArrowDownRight className="h-3 w-3" />}
                    {s.change} this week
                  </span>
                </Card>
              ))}
            </div>

            {/* System health */}
            <Card className="p-3">
              <p className="text-[11px] font-semibold text-muted-foreground uppercase tracking-wide mb-2">System Health</p>
              <div className="space-y-2">
                {systemHealth.map((h) => (
                  <div key={h.label} className="flex items-center justify-between">
                    <span className="text-sm">{h.label}</span>
                    <div className="flex items-center gap-1.5">
                      <div className={`h-2 w-2 rounded-full ${healthDot[h.status]}`} />
                      <span className="text-xs text-muted-foreground capitalize">{h.status}</span>
                    </div>
                  </div>
                ))}
              </div>
            </Card>

            {/* Audit log */}
            <Card className="p-3">
              <p className="text-[11px] font-semibold text-muted-foreground uppercase tracking-wide mb-2">Recent Audit Log</p>
              <div className="space-y-2.5">
                {recentAudit.map((a, i) => (
                  <div key={i} className="flex items-start gap-2">
                    <div className="h-6 w-6 rounded-full bg-primary/10 flex items-center justify-center shrink-0 mt-0.5">
                      <Activity className="h-3 w-3 text-primary" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium leading-tight">{a.action}</p>
                      <p className="text-xs text-muted-foreground truncate">{a.detail} · {a.actor}</p>
                    </div>
                    <span className="text-[11px] text-muted-foreground whitespace-nowrap">{a.time}</span>
                  </div>
                ))}
              </div>
            </Card>
          </TabsContent>

          {/* ─── USERS TAB ─── */}
          <TabsContent value="users" className="mt-3 space-y-3">
            <div className="flex gap-2">
              <div className="relative flex-1">
                <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-muted-foreground" />
                <Input
                  placeholder="Search users…"
                  value={userSearch}
                  onChange={(e) => setUserSearch(e.target.value)}
                  className="pl-8 h-9 text-sm"
                />
              </div>
              <Select value={roleFilter} onValueChange={setRoleFilter}>
                <SelectTrigger className="w-28 h-9 text-xs">
                  <SelectValue placeholder="Role" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All roles</SelectItem>
                  <SelectItem value="admin">Admin</SelectItem>
                  <SelectItem value="moderator">Moderator</SelectItem>
                  <SelectItem value="validator">Validator</SelectItem>
                  <SelectItem value="member">Member</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <p className="text-xs text-muted-foreground">{filteredUsers.length} users</p>

            <div className="space-y-2">
              {filteredUsers.map((u) => (
                <Card key={u.id} className="p-3">
                  <div className="flex items-center gap-3">
                    <div className="h-9 w-9 rounded-full bg-muted flex items-center justify-center shrink-0">
                      <span className="font-display font-bold text-sm">{u.name.split(" ").map((n) => n[0]).join("")}</span>
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-1.5">
                        <p className="text-sm font-semibold truncate">{u.name}</p>
                        <Badge className={`text-[10px] py-0 px-1.5 ${roleColor[u.role]}`}>{u.role}</Badge>
                      </div>
                      <p className="text-xs text-muted-foreground truncate">{u.email}</p>
                    </div>
                    <div className="text-right shrink-0">
                      <p className="text-xs font-medium">{u.tasks} tasks</p>
                      <p className="text-[10px] text-muted-foreground">Since {new Date(u.joined).toLocaleDateString("en-US", { month: "short", year: "numeric" })}</p>
                    </div>
                  </div>

                  <div className="flex gap-1.5 mt-2.5 pt-2 border-t border-border">
                    <Button variant="ghost" size="sm" className="h-7 text-xs flex-1">
                      <Eye className="h-3 w-3 mr-1" />
                      View
                    </Button>
                    <Button variant="ghost" size="sm" className="h-7 text-xs flex-1">
                      <UserCog className="h-3 w-3 mr-1" />
                      Roles
                    </Button>
                  </div>
                </Card>
              ))}
            </div>
          </TabsContent>

          {/* ─── MODERATION TAB ─── */}
          <TabsContent value="moderation" className="mt-3 space-y-3">
            <div className="flex items-center justify-between">
              <p className="text-xs text-muted-foreground">{flaggedItems.length} flagged items</p>
              <Badge variant="outline" className="text-[10px]">
                <AlertTriangle className="h-3 w-3 mr-1 text-secondary" />
                {flaggedItems.filter((f) => f.severity === "critical" || f.severity === "high").length} urgent
              </Badge>
            </div>

            <div className="space-y-2">
              {flaggedItems.map((item) => (
                <Card key={item.id} className="p-3 group">
                  <div className="flex items-start gap-2.5">
                    <div className={`h-7 w-7 rounded-lg flex items-center justify-center shrink-0 ${
                      item.severity === "critical" ? "bg-destructive/15" : item.severity === "high" ? "bg-secondary/15" : "bg-muted"
                    }`}>
                      {item.severity === "critical" ? (
                        <XCircle className="h-3.5 w-3.5 text-destructive" />
                      ) : item.severity === "high" ? (
                        <AlertTriangle className="h-3.5 w-3.5 text-secondary" />
                      ) : (
                        <Clock className="h-3.5 w-3.5 text-muted-foreground" />
                      )}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium leading-tight">{item.title}</p>
                      <div className="flex items-center gap-2 mt-1">
                        <Badge className={`text-[10px] py-0 px-1.5 ${severityColor[item.severity]}`}>{item.severity}</Badge>
                        <span className="text-[11px] text-muted-foreground">{item.type} · {item.user}</span>
                      </div>
                    </div>
                    <span className="text-[10px] text-muted-foreground whitespace-nowrap">{item.createdAt}</span>
                  </div>

                  <div className="flex gap-1.5 mt-2.5 pt-2 border-t border-border">
                    <Button variant="ghost" size="sm" className="h-7 text-xs flex-1">
                      <Eye className="h-3 w-3 mr-1" />
                      Review
                    </Button>
                    <Button variant="ghost" size="sm" className="h-7 text-xs flex-1 text-primary">
                      <CheckCircle2 className="h-3 w-3 mr-1" />
                      Dismiss
                    </Button>
                    <Button variant="ghost" size="sm" className="h-7 text-xs flex-1 text-destructive">
                      <XCircle className="h-3 w-3 mr-1" />
                      Remove
                    </Button>
                  </div>
                </Card>
              ))}
            </div>
          </TabsContent>
        </Tabs>
      </div>
    </AppShell>
  );
}
