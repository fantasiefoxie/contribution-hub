import { useNavigate } from "react-router-dom";
import { ArrowRight, MapPin, Clock } from "lucide-react";

interface NextMissionCardProps {
  id: string;
  title: string;
  estimatedTime: string;
  distance?: string;
  tags?: string[];
}

export default function NextMissionCard({ id, title, estimatedTime, distance, tags }: NextMissionCardProps) {
  const navigate = useNavigate();

  return (
    <button
      onClick={() => navigate(`/task/${id}`)}
      className="w-full bg-gradient-to-r from-primary/5 to-accent/5 rounded-2xl border border-primary/10 p-4 text-left hover:border-primary/30 transition-colors"
    >
      <p className="text-[10px] uppercase tracking-wider text-primary font-semibold mb-1.5">Suggested Next Mission</p>
      <p className="font-display font-semibold text-sm">{title}</p>
      <div className="flex items-center gap-3 mt-2 text-xs text-muted-foreground">
        <div className="flex items-center gap-1">
          <Clock className="h-3 w-3" />
          <span>{estimatedTime}</span>
        </div>
        {distance && (
          <div className="flex items-center gap-1">
            <MapPin className="h-3 w-3" />
            <span>{distance}</span>
          </div>
        )}
      </div>
      <div className="flex items-center gap-1 text-xs text-primary font-medium mt-2">
        <span>View details</span>
        <ArrowRight className="h-3 w-3" />
      </div>
    </button>
  );
}
