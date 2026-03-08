import { createContext, useContext, useState, useCallback, type ReactNode } from "react";

export interface WalkthroughStep {
  /** CSS selector or data-walkthrough attribute value to highlight */
  target: string;
  title: string;
  description: string;
  /** Which side to place the tooltip */
  placement?: "top" | "bottom" | "left" | "right";
  /** Optional route to navigate to before showing this step */
  route?: string;
}

interface WalkthroughState {
  active: boolean;
  currentStep: number;
  steps: WalkthroughStep[];
  start: () => void;
  next: () => void;
  prev: () => void;
  skip: () => void;
  goToStep: (index: number) => void;
}

const WalkthroughContext = createContext<WalkthroughState | undefined>(undefined);

const WALKTHROUGH_STEPS: WalkthroughStep[] = [
  {
    target: "[data-walkthrough='welcome']",
    title: "Welcome to Contribution OS! 🎯",
    description: "This is your Mission Board — the central hub where you discover tasks that need volunteers. Let's take a quick tour!",
    placement: "bottom",
  },
  {
    target: "[data-walkthrough='active-mission']",
    title: "Active Mission Banner",
    description: "When you're working on a task, it appears here for one-tap resume. Track your progress and remaining time at a glance.",
    placement: "bottom",
  },
  {
    target: "[data-walkthrough='search']",
    title: "Search Tasks",
    description: "Quickly find tasks by keyword or tag. Try searching for 'environment' or 'community' to narrow down your options.",
    placement: "bottom",
  },
  {
    target: "[data-walkthrough='filters']",
    title: "Smart Filters",
    description: "Switch between personalized recommendations, nearby tasks, high-priority missions, and remote-friendly online tasks.",
    placement: "bottom",
  },
  {
    target: "[data-walkthrough='task-card']",
    title: "Task Cards",
    description: "Each card shows the task title, difficulty level (1-5 dots), distance, and tags. Tap any card to see full details and claim it.",
    placement: "bottom",
  },
  {
    target: "[data-walkthrough='quick-actions']",
    title: "Quick Actions",
    description: "Jump to your active mission, browse initiatives, find nearby tasks, or switch to online-only — all from this floating bar.",
    placement: "top",
  },
  {
    target: "[data-walkthrough='nav-home']",
    title: "Navigation: Home",
    description: "You're here! The Mission Board is your starting point for discovering new tasks to contribute to.",
    placement: "top",
  },
  {
    target: "[data-walkthrough='nav-tasks']",
    title: "Navigation: Active Tasks",
    description: "View and manage your currently claimed tasks. Track progress, add updates, and submit your completed work here.",
    placement: "top",
  },
  {
    target: "[data-walkthrough='nav-impact']",
    title: "Navigation: Impact",
    description: "See your contribution history, earned credentials, and overall impact score. Your verified work lives here forever.",
    placement: "top",
  },
  {
    target: "[data-walkthrough='nav-alerts']",
    title: "Navigation: Alerts",
    description: "Get notified about validation results, team invites, initiative updates, and more. Never miss an important update.",
    placement: "top",
  },
  {
    target: "[data-walkthrough='nav-profile']",
    title: "Navigation: Profile",
    description: "Manage your profile, interests, and settings. Your credentials and contribution stats are showcased here.",
    placement: "top",
  },
];

const STORAGE_KEY = "cos_walkthrough_completed";

export function WalkthroughProvider({ children }: { children: ReactNode }) {
  const [active, setActive] = useState(false);
  const [currentStep, setCurrentStep] = useState(0);

  const start = useCallback(() => {
    setCurrentStep(0);
    setActive(true);
  }, []);

  const next = useCallback(() => {
    if (currentStep < WALKTHROUGH_STEPS.length - 1) {
      setCurrentStep(prev => prev + 1);
    } else {
      setActive(false);
      localStorage.setItem(STORAGE_KEY, "true");
    }
  }, [currentStep]);

  const prev = useCallback(() => {
    if (currentStep > 0) setCurrentStep(prev => prev - 1);
  }, [currentStep]);

  const skip = useCallback(() => {
    setActive(false);
    localStorage.setItem(STORAGE_KEY, "true");
  }, []);

  const goToStep = useCallback((index: number) => {
    if (index >= 0 && index < WALKTHROUGH_STEPS.length) setCurrentStep(index);
  }, []);

  return (
    <WalkthroughContext.Provider
      value={{ active, currentStep, steps: WALKTHROUGH_STEPS, start, next, prev, skip, goToStep }}
    >
      {children}
    </WalkthroughContext.Provider>
  );
}

export function useWalkthrough() {
  const ctx = useContext(WalkthroughContext);
  if (!ctx) throw new Error("useWalkthrough must be used within WalkthroughProvider");
  return ctx;
}

export function hasCompletedWalkthrough(): boolean {
  return localStorage.getItem(STORAGE_KEY) === "true";
}

export function resetWalkthrough(): void {
  localStorage.removeItem(STORAGE_KEY);
}
