import type { LucideIcon } from "lucide-react";

export interface QuickActionItem {
  icon: LucideIcon;
  label: string;
  color: string;
  bg: string;
  onClick: () => void;
  disabled?: boolean;
}

/** Persistent bottom action bar — sits above BottomNav */
export default function QuickActionBar({ actions }: { actions: QuickActionItem[] }) {
  if (!actions.length) return null;

  return (
    <div className="fixed bottom-16 left-0 right-0 z-30 pointer-events-none">
      <div className="max-w-lg mx-auto px-4 pb-2 pointer-events-auto">
        <div className="bg-card/95 backdrop-blur-md border border-border rounded-2xl p-2.5 shadow-lg">
          <div className={`grid gap-1.5`} style={{ gridTemplateColumns: `repeat(${Math.min(actions.length, 4)}, 1fr)` }}>
            {actions.map((a) => (
              <button
                key={a.label}
                onClick={a.onClick}
                disabled={a.disabled}
                className="flex flex-col items-center gap-1 py-2 rounded-xl hover:bg-muted/60 transition-colors active:scale-95 disabled:opacity-40"
              >
                <div className={`h-9 w-9 rounded-lg ${a.bg} flex items-center justify-center`}>
                  <a.icon className={`h-4 w-4 ${a.color}`} />
                </div>
                <span className="text-[10px] font-medium text-muted-foreground leading-tight text-center">{a.label}</span>
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
