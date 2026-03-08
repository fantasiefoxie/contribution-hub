import type { LucideIcon } from "lucide-react";

export interface FilterOption<T extends string = string> {
  key: T;
  label: string;
  icon?: LucideIcon;
  count?: number;
}

interface FilterChipsBarProps<T extends string> {
  options: FilterOption<T>[];
  active: T;
  onChange: (key: T) => void;
}

/** Horizontal scrollable filter chips */
export default function FilterChipsBar<T extends string>({ options, active, onChange }: FilterChipsBarProps<T>) {
  return (
    <div className="flex gap-1.5 overflow-x-auto pb-1 -mx-1 px-1 scrollbar-hide">
      {options.map((f) => (
        <button
          key={f.key}
          onClick={() => onChange(f.key)}
          className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-all ${
            active === f.key
              ? "bg-primary text-primary-foreground shadow-sm"
              : "bg-muted text-muted-foreground hover:bg-muted/80"
          }`}
        >
          {f.icon && <f.icon className="h-3 w-3" />}
          {f.label}
          {f.count !== undefined && (
            <span className="bg-primary-foreground/20 text-[10px] px-1 rounded">{f.count}</span>
          )}
        </button>
      ))}
    </div>
  );
}
