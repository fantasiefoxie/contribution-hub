import { Badge } from "@/components/ui/badge";
import { AlertTriangle, CheckCircle, AlertCircle } from "lucide-react";

interface BottleneckItemProps {
  severity: "critical" | "warning" | "info";
  title: string;
  description: string;
  recommendation: string;
}

const severityStyles: Record<string, string> = {
  critical: "bg-destructive/10 text-destructive border-destructive/30",
  warning: "bg-secondary/10 text-secondary border-secondary/30",
  info: "bg-accent/10 text-accent border-accent/30",
};

const severityIcons = {
  critical: AlertTriangle,
  warning: AlertCircle,
  info: CheckCircle,
};

export default function BottleneckItem({ severity, title, description, recommendation }: BottleneckItemProps) {
  const Icon = severityIcons[severity];
  return (
    <div className={`p-4 rounded-lg border ${severityStyles[severity]}`}>
      <div className="flex items-center gap-2 mb-1">
        <Icon className="h-4 w-4 shrink-0" />
        <span className="font-semibold text-sm">{title}</span>
        <Badge variant="outline" className="ml-auto text-[10px] uppercase shrink-0">{severity}</Badge>
      </div>
      <p className="text-xs opacity-80 mb-2">{description}</p>
      <p className="text-xs font-medium">→ {recommendation}</p>
    </div>
  );
}
