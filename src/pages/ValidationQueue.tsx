import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ShieldCheck, Loader2, Clock, User, FileText, ChevronRight } from "lucide-react";
import AppShell from "@/components/layout/AppShell";
import { Badge } from "@/components/ui/badge";
import { usePendingValidations } from "@/hooks/use-validations";

interface MockSubmission {
  id: string;
  content: string;
  submitted_at: string;
  status: string;
  contributor: { display_name: string; avatar_url: string | null };
  tasks: { id: string; title: string; org_id: string; difficulty: number; tags: string[] };
}

const mockSubmissions: MockSubmission[] = [
  {
    id: "sub-1",
    content: "Completed garden cleanup. Removed debris from 3 plots and planted 15 seedlings.",
    submitted_at: new Date(Date.now() - 3600000).toISOString(),
    status: "pending",
    contributor: { display_name: "Maria Santos", avatar_url: null },
    tasks: { id: "t-1", title: "Community Garden Cleanup", org_id: "org-1", difficulty: 2, tags: ["environment"] },
  },
  {
    id: "sub-2",
    content: "Surveyed 48 businesses on Main Street. Data entered into shared spreadsheet.",
    submitted_at: new Date(Date.now() - 7200000).toISOString(),
    status: "pending",
    contributor: { display_name: "James Park", avatar_url: null },
    tasks: { id: "t-2", title: "Survey Local Businesses", org_id: "org-1", difficulty: 3, tags: ["research"] },
  },
  {
    id: "sub-3",
    content: "Collected 3 water samples from river stations A, B, and C. Lab results pending.",
    submitted_at: new Date(Date.now() - 14400000).toISOString(),
    status: "pending",
    contributor: { display_name: "Aisha Johnson", avatar_url: null },
    tasks: { id: "t-3", title: "River Water Quality Testing", org_id: "org-2", difficulty: 3, tags: ["environment", "research"] },
  },
];

function timeAgo(dateStr: string) {
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
}

export default function ValidationQueue() {
  const navigate = useNavigate();
  const { data, isLoading } = usePendingValidations();

  const apiSubs = data?.submissions ?? [];
  const submissions: MockSubmission[] = apiSubs.length > 0 ? apiSubs : mockSubmissions;

  return (
    <AppShell>
      <div className="px-4 pt-6 pb-3">
        <div className="flex items-center gap-3">
          <div className="h-10 w-10 rounded-full bg-accent/20 flex items-center justify-center">
            <ShieldCheck className="h-5 w-5 text-accent" />
          </div>
          <div>
            <h1 className="font-display text-2xl font-bold text-foreground">Validation Queue</h1>
            <p className="text-sm text-muted-foreground">{submissions.length} submissions awaiting review</p>
          </div>
        </div>
      </div>

      <div className="px-4 space-y-3 mt-2 pb-24">
        {isLoading ? (
          <div className="flex items-center justify-center h-40">
            <Loader2 className="h-6 w-6 animate-spin text-primary" />
          </div>
        ) : submissions.length === 0 ? (
          <div className="text-center py-16">
            <p className="text-4xl mb-3">✅</p>
            <p className="text-muted-foreground text-sm">All caught up! No submissions pending.</p>
          </div>
        ) : (
          submissions.map((sub) => (
            <button
              key={sub.id}
              onClick={() => navigate(`/validate/${sub.id}`)}
              className="w-full bg-card rounded-2xl border border-border p-4 text-left hover:border-primary/30 transition-colors"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1 min-w-0">
                  <p className="font-display font-semibold text-sm truncate">{sub.tasks.title}</p>
                  <div className="flex items-center gap-2 mt-1 text-xs text-muted-foreground">
                    <User className="h-3 w-3" />
                    <span>{sub.contributor.display_name}</span>
                    <span>·</span>
                    <Clock className="h-3 w-3" />
                    <span>{timeAgo(sub.submitted_at)}</span>
                  </div>
                </div>
                <ChevronRight className="h-4 w-4 text-muted-foreground shrink-0 mt-1" />
              </div>

              <p className="text-xs text-muted-foreground mt-2 line-clamp-2">{sub.content}</p>

              <div className="flex items-center gap-2 mt-3">
                <Badge variant="outline" className="text-[10px]">
                  Difficulty {sub.tasks.difficulty}/5
                </Badge>
                {sub.tasks.tags?.slice(0, 2).map((tag) => (
                  <Badge key={tag} variant="secondary" className="text-[10px]">
                    {tag}
                  </Badge>
                ))}
                <div className="ml-auto">
                  <FileText className="h-3.5 w-3.5 text-muted-foreground" />
                </div>
              </div>
            </button>
          ))
        )}
      </div>
    </AppShell>
  );
}
