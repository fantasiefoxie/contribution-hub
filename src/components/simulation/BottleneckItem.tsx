import { Badge } from "@/components/ui/badge";
import { AlertTriangle } from "lucide-react";

interface BottleneckItemProps {
  severity: "critical" | "warning" | "info";
  title: string;
  description: string;
  recommendation: string;
}

const colors = {
  critical: "bg-destructive/10 text-destructive border-destructive/30",
  warning: "bg-warning/10 text-warning border-warning/30",
  info: "bg-info/10 text-info border-info/30",
};

export default function BottleneckItem({ severity, title, description, recommendation }: BottleneckItemProps) {
  return (
    <div className={`p-4 rounded-lg border ${colors[severity]}`}>
      <div className="flex items-center gap-2 mb-1">
        <AlertTriangle className="h-4 w-4" />
        <span className="font-semibold text-sm">{title}</span>
        <Badge variant="outline" className="ml-auto text-[10px] uppercase">{severity}</Badge>
      </div>
      <p className="text-xs opacity-80 mb-2">{description}</p>
      <p className="text-xs font-medium">→ {recommendation}</p>
    </div>
  );
}
