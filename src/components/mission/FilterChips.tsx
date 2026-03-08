import { MapPin, Signal, TreePine, Users, FlaskConical } from "lucide-react";

export type FilterCategory = "nearby" | "online" | "environment" | "community" | "research";

interface FilterChipsProps {
  active: FilterCategory;
  onChange: (cat: FilterCategory) => void;
}

const filters: { id: FilterCategory; label: string; icon: typeof MapPin }[] = [
  { id: "nearby", label: "Nearby", icon: MapPin },
  { id: "online", label: "Online", icon: Signal },
  { id: "environment", label: "Environment", icon: TreePine },
  { id: "community", label: "Community", icon: Users },
  { id: "research", label: "Research", icon: FlaskConical },
];

export default function FilterChips({ active, onChange }: FilterChipsProps) {
  return (
    <div className="flex gap-2 overflow-x-auto no-scrollbar px-4 py-2">
      {filters.map(({ id, label, icon: Icon }) => (
        <button
          key={id}
          onClick={() => onChange(id)}
          className={`flex items-center gap-1.5 px-3.5 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-all duration-200 ${
            active === id
              ? "bg-primary text-primary-foreground shadow-md shadow-primary/20"
              : "bg-muted text-muted-foreground hover:bg-muted/80"
          }`}
        >
          <Icon className="h-3.5 w-3.5" />
          {label}
        </button>
      ))}
    </div>
  );
}
