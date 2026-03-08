import { useState } from "react";
import { Check, X } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

const ALL_INTERESTS = [
  { key: "environment", label: "Environment", emoji: "🌿" },
  { key: "community", label: "Community Outreach", emoji: "🤝" },
  { key: "survey", label: "Survey Research", emoji: "📊" },
  { key: "logistics", label: "Logistics", emoji: "📦" },
  { key: "digital", label: "Digital Tasks", emoji: "💻" },
  { key: "education", label: "Education", emoji: "🎓" },
  { key: "health", label: "Health", emoji: "🏥" },
  { key: "food", label: "Food & Nutrition", emoji: "🍲" },
  { key: "infrastructure", label: "Infrastructure", emoji: "🏗️" },
  { key: "arts", label: "Arts & Culture", emoji: "🎨" },
  { key: "sanitation", label: "Sanitation", emoji: "🚰" },
  { key: "advocacy", label: "Advocacy", emoji: "📢" },
];

interface InterestSelectorProps {
  selected: string[];
  onChange: (interests: string[]) => void;
  loading?: boolean;
}

export default function InterestSelector({ selected, onChange, loading }: InterestSelectorProps) {
  const toggle = (key: string) => {
    if (selected.includes(key)) {
      onChange(selected.filter(k => k !== key));
    } else {
      onChange([...selected, key]);
    }
  };

  return (
    <div className="space-y-3">
      <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">Your Interests</p>
      <div className="flex flex-wrap gap-2">
        {ALL_INTERESTS.map(({ key, label, emoji }) => {
          const active = selected.includes(key);
          return (
            <button
              key={key}
              onClick={() => toggle(key)}
              disabled={loading}
              className={`flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium transition-all border ${
                active
                  ? "bg-primary/10 border-primary/30 text-primary"
                  : "bg-card border-border text-muted-foreground hover:border-primary/20 hover:text-foreground"
              }`}
            >
              <span>{emoji}</span>
              {label}
              {active && <Check className="h-3 w-3" />}
            </button>
          );
        })}
      </div>
    </div>
  );
}

export { ALL_INTERESTS };
