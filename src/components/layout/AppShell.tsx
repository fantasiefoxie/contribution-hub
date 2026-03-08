import { ReactNode } from "react";
import BottomNav from "./BottomNav";

interface AppShellProps {
  children: ReactNode;
  /** Optional fixed bottom bar rendered above BottomNav */
  bottomBar?: ReactNode;
}

export default function AppShell({ children, bottomBar }: AppShellProps) {
  return (
    <div className="h-[100dvh] flex flex-col bg-background" style={{ overflowAnchor: "none" }}>
      <main
        className="flex-1 overflow-y-auto overflow-x-hidden overscroll-y-contain scroll-smooth max-w-lg mx-auto w-full custom-scrollbar scroll-container"
        style={{
          overflowAnchor: "auto",
          WebkitOverflowScrolling: "touch",
          willChange: "scroll-position",
          contain: "strict",
        }}
      >
        {/* Top fade mask */}
        <div className="scroll-edge-top pointer-events-none sticky top-0 z-20 h-3" />
        <div className={bottomBar ? "pb-36" : "pb-20"}>
          {children}
        </div>
        {/* Bottom fade mask */}
        <div className="scroll-edge-bottom pointer-events-none sticky bottom-0 z-20 h-4" />
      </main>
      {bottomBar && (
        <div className="fixed bottom-16 left-0 right-0 z-30">
          <div className="max-w-lg mx-auto">{bottomBar}</div>
        </div>
      )}
      <BottomNav />
    </div>
  );
}
