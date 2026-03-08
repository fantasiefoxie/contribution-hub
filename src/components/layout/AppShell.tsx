import { ReactNode } from "react";
import BottomNav from "./BottomNav";

interface AppShellProps {
  children: ReactNode;
  /** Optional fixed bottom bar rendered above BottomNav */
  bottomBar?: ReactNode;
}

export default function AppShell({ children, bottomBar }: AppShellProps) {
  return (
    <div className="h-[100dvh] flex flex-col bg-background">
      <main className="flex-1 overflow-y-auto overscroll-y-contain max-w-lg mx-auto w-full">
        <div className={bottomBar ? "pb-36" : "pb-20"}>
          {children}
        </div>
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
