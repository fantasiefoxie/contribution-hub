import { useNavigate } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { Loader2, LogIn } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { ReactNode } from "react";

interface Props {
  children: ReactNode;
  /** Optional: custom fallback when unauthenticated. Defaults to login prompt. */
  fallback?: ReactNode;
}

export default function ProtectedRoute({ children, fallback }: Props) {
  const { user, loading } = useAuth();
  const navigate = useNavigate();

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60dvh]">
        <Loader2 className="h-6 w-6 animate-spin text-primary" />
      </div>
    );
  }

  if (!user) {
    if (fallback) return <>{fallback}</>;
    return (
      <div className="flex flex-col items-center justify-center py-16 px-6 text-center gap-4">
        <div className="h-14 w-14 rounded-2xl bg-primary/10 flex items-center justify-center">
          <LogIn className="h-7 w-7 text-primary" />
        </div>
        <div>
          <p className="font-display font-semibold text-base">Sign in to continue</p>
          <p className="text-xs text-muted-foreground mt-1">
            Log in with your phone number to access this feature.
          </p>
        </div>
        <Button onClick={() => navigate("/")} className="rounded-xl">
          Sign In
        </Button>
      </div>
    );
  }

  return <>{children}</>;
}
