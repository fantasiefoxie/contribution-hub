import { useEffect, useState, useRef, useCallback } from "react";
import { useWalkthrough } from "@/contexts/WalkthroughContext";
import { X, ChevronLeft, ChevronRight, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function WalkthroughOverlay() {
  const { active, currentStep, steps, next, prev, skip } = useWalkthrough();
  const [rect, setRect] = useState<DOMRect | null>(null);
  const [tooltipStyle, setTooltipStyle] = useState<React.CSSProperties>({});
  const [arrowClass, setArrowClass] = useState("");
  const tooltipRef = useRef<HTMLDivElement>(null);
  const step = steps[currentStep];
  const isLast = currentStep === steps.length - 1;

  const positionTooltip = useCallback(() => {
    if (!step || !active) return;

    const el = document.querySelector(step.target);
    if (!el) {
      // If target not found, show centered tooltip
      setRect(null);
      setTooltipStyle({
        position: "fixed",
        top: "50%",
        left: "50%",
        transform: "translate(-50%, -50%)",
      });
      setArrowClass("");
      return;
    }

    const r = el.getBoundingClientRect();
    setRect(r);

    // Scroll element into view if needed
    el.scrollIntoView({ behavior: "smooth", block: "nearest" });

    // Calculate tooltip position after a small delay for scroll
    requestAnimationFrame(() => {
      const updatedRect = el.getBoundingClientRect();
      setRect(updatedRect);

      const placement = step.placement || "bottom";
      const padding = 12;
      const tooltipWidth = Math.min(320, window.innerWidth - 32);

      let style: React.CSSProperties = { position: "fixed", width: tooltipWidth };

      switch (placement) {
        case "bottom":
          style.top = updatedRect.bottom + padding;
          style.left = Math.max(16, Math.min(
            updatedRect.left + updatedRect.width / 2 - tooltipWidth / 2,
            window.innerWidth - tooltipWidth - 16
          ));
          setArrowClass("walkthrough-arrow-top");
          break;
        case "top":
          style.bottom = window.innerHeight - updatedRect.top + padding;
          style.left = Math.max(16, Math.min(
            updatedRect.left + updatedRect.width / 2 - tooltipWidth / 2,
            window.innerWidth - tooltipWidth - 16
          ));
          setArrowClass("walkthrough-arrow-bottom");
          break;
        case "left":
          style.top = updatedRect.top + updatedRect.height / 2 - 60;
          style.right = window.innerWidth - updatedRect.left + padding;
          setArrowClass("walkthrough-arrow-right");
          break;
        case "right":
          style.top = updatedRect.top + updatedRect.height / 2 - 60;
          style.left = updatedRect.right + padding;
          setArrowClass("walkthrough-arrow-left");
          break;
      }

      setTooltipStyle(style);
    });
  }, [step, active]);

  useEffect(() => {
    if (!active) return;
    // Small delay to allow DOM to settle
    const timer = setTimeout(positionTooltip, 100);
    window.addEventListener("resize", positionTooltip);
    window.addEventListener("scroll", positionTooltip, true);
    return () => {
      clearTimeout(timer);
      window.removeEventListener("resize", positionTooltip);
      window.removeEventListener("scroll", positionTooltip, true);
    };
  }, [active, currentStep, positionTooltip]);

  // Handle keyboard navigation
  useEffect(() => {
    if (!active) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === "Escape") skip();
      else if (e.key === "ArrowRight" || e.key === "Enter") next();
      else if (e.key === "ArrowLeft") prev();
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [active, next, prev, skip]);

  if (!active || !step) return null;

  const spotlightPadding = 8;
  const spotlightRadius = 16;

  return (
    <div className="fixed inset-0 z-[9999]" role="dialog" aria-modal="true" aria-label="Walkthrough">
      {/* Backdrop with spotlight cutout */}
      <svg className="fixed inset-0 w-full h-full pointer-events-none" style={{ zIndex: 1 }}>
        <defs>
          <mask id="walkthrough-mask">
            <rect width="100%" height="100%" fill="white" />
            {rect && (
              <rect
                x={rect.left - spotlightPadding}
                y={rect.top - spotlightPadding}
                width={rect.width + spotlightPadding * 2}
                height={rect.height + spotlightPadding * 2}
                rx={spotlightRadius}
                fill="black"
              />
            )}
          </mask>
        </defs>
        <rect
          width="100%"
          height="100%"
          fill="hsl(220 18% 8% / 0.7)"
          mask="url(#walkthrough-mask)"
        />
      </svg>

      {/* Spotlight ring highlight */}
      {rect && (
        <div
          className="fixed pointer-events-none rounded-2xl ring-2 ring-primary/60 ring-offset-2 ring-offset-transparent"
          style={{
            left: rect.left - spotlightPadding,
            top: rect.top - spotlightPadding,
            width: rect.width + spotlightPadding * 2,
            height: rect.height + spotlightPadding * 2,
            zIndex: 2,
            boxShadow: "0 0 0 4px hsl(var(--primary) / 0.15), 0 0 20px 4px hsl(var(--primary) / 0.1)",
            transition: "all 0.3s cubic-bezier(0.4, 0, 0.2, 1)",
          }}
        />
      )}

      {/* Click shield (prevents interaction with page except highlighted area) */}
      <div className="fixed inset-0" style={{ zIndex: 3 }} onClick={(e) => {
        // Allow click-through on highlighted area
        if (rect) {
          const { clientX, clientY } = e;
          if (
            clientX >= rect.left - spotlightPadding &&
            clientX <= rect.right + spotlightPadding &&
            clientY >= rect.top - spotlightPadding &&
            clientY <= rect.bottom + spotlightPadding
          ) return;
        }
        // Click outside = next step
        next();
      }} />

      {/* Tooltip */}
      <div
        ref={tooltipRef}
        className={`bg-card border border-border rounded-2xl shadow-float p-4 ${arrowClass}`}
        style={{ ...tooltipStyle, zIndex: 4 }}
      >
        {/* Step counter + skip */}
        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center gap-2">
            <Sparkles className="h-3.5 w-3.5 text-primary" />
            <span className="text-[10px] font-semibold text-primary uppercase tracking-wide">
              Step {currentStep + 1} of {steps.length}
            </span>
          </div>
          <button
            onClick={skip}
            className="text-muted-foreground hover:text-foreground transition-colors p-1 -m-1"
            aria-label="Skip walkthrough"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Content */}
        <h3 className="font-display font-bold text-sm mb-1">{step.title}</h3>
        <p className="text-xs text-muted-foreground leading-relaxed">{step.description}</p>

        {/* Progress dots */}
        <div className="flex items-center gap-1 mt-3 mb-3">
          {steps.map((_, i) => (
            <div
              key={i}
              className={`h-1.5 rounded-full transition-all duration-300 ${
                i === currentStep
                  ? "w-4 bg-primary"
                  : i < currentStep
                  ? "w-1.5 bg-primary/40"
                  : "w-1.5 bg-muted-foreground/20"
              }`}
            />
          ))}
        </div>

        {/* Navigation buttons */}
        <div className="flex items-center gap-2">
          {currentStep > 0 && (
            <Button variant="ghost" size="sm" onClick={prev} className="rounded-lg gap-1 text-xs h-8">
              <ChevronLeft className="h-3 w-3" />
              Back
            </Button>
          )}
          <div className="flex-1" />
          <Button
            size="sm"
            onClick={next}
            className="rounded-lg gap-1 text-xs h-8 px-4"
          >
            {isLast ? "Get Started!" : "Next"}
            {!isLast && <ChevronRight className="h-3 w-3" />}
          </Button>
        </div>
      </div>
    </div>
  );
}
