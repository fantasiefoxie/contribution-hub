import { useWalkthrough, hasCompletedWalkthrough } from "@/contexts/WalkthroughContext";
import { HelpCircle } from "lucide-react";
import { Button } from "@/components/ui/button";

/** Floating button to restart the walkthrough — always visible */
export default function WalkthroughTrigger() {
  const { start, active } = useWalkthrough();

  if (active) return null;

  return (
    <Button
      variant="outline"
      size="icon"
      onClick={start}
      className="fixed bottom-20 right-4 z-40 h-10 w-10 rounded-full shadow-elevated border-primary/20 bg-card hover:bg-primary/5 hover:border-primary/40 transition-all"
      title="Start walkthrough"
    >
      <HelpCircle className="h-5 w-5 text-primary" />
    </Button>
  );
}
