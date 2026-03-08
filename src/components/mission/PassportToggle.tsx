import { Globe } from "lucide-react";
import { Switch } from "@/components/ui/switch";

interface PassportToggleProps {
  enabled: boolean;
  onChange: (v: boolean) => void;
}

export default function PassportToggle({ enabled, onChange }: PassportToggleProps) {
  return (
    <div className="flex items-center gap-2 px-4">
      <div
        className={`flex items-center gap-2 px-3 py-2 rounded-xl transition-all duration-300 ${
          enabled
            ? "bg-accent/15 border border-accent/30"
            : "bg-muted border border-transparent"
        }`}
      >
        <Globe className={`h-4 w-4 transition-colors ${enabled ? "text-accent" : "text-muted-foreground"}`} />
        <span className="text-xs font-medium">Passport Mode</span>
        <Switch
          checked={enabled}
          onCheckedChange={onChange}
          className="data-[state=checked]:bg-accent scale-90"
        />
      </div>
    </div>
  );
}
