import { useNavigate } from "react-router-dom";
import { Building2, ArrowRight, Users } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import AppShell from "@/components/layout/AppShell";
import ScreenHeader from "@/components/shared/ScreenHeader";
import { mockOrganizations } from "@/lib/mock-data";

export default function Organizations() {
  const navigate = useNavigate();

  return (
    <AppShell>
      <ScreenHeader title="My Organizations" subtitle="Groups you belong to" icon={Building2} iconColor="text-accent" />

      <div className="px-4 space-y-2">
        {mockOrganizations.map((org) => (
          <button
            key={org.id}
            onClick={() => navigate(`/initiatives`)}
            className="w-full text-left bg-card rounded-2xl border border-border p-4 flex items-center gap-3 hover:border-primary/20 transition-colors active:scale-[0.99] group"
          >
            <div className="h-10 w-10 rounded-xl bg-accent/10 flex items-center justify-center shrink-0">
              <Building2 className="h-5 w-5 text-accent" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="font-display font-semibold text-sm">{org.name}</p>
              <p className="text-[11px] text-muted-foreground mt-0.5">{org.description}</p>
              <Badge className="mt-1.5 bg-muted text-muted-foreground border-0 text-[10px]">
                <Users className="h-3 w-3 mr-1" /> Member
              </Badge>
            </div>
            <ArrowRight className="h-4 w-4 text-muted-foreground group-hover:text-primary transition-colors shrink-0" />
          </button>
        ))}
      </div>

      <div className="h-24" />
    </AppShell>
  );
}
