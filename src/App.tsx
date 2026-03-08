import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider, QueryCache } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { ThemeProvider } from "@/components/ThemeProvider";
import { AuthProvider } from "@/contexts/AuthContext";
import { WalkthroughProvider } from "@/contexts/WalkthroughContext";
import WalkthroughOverlay from "@/components/walkthrough/WalkthroughOverlay";
import ErrorBoundary from "@/components/ErrorBoundary";
import MissionFeed from "./pages/MissionFeed";
import TaskDetails from "./pages/TaskDetails";
import ActiveWorkspace from "./pages/ActiveWorkspace";
import Contributions from "./pages/Contributions";
import Notifications from "./pages/Notifications";
import Profile from "./pages/Profile";
import ValidationQueue from "./pages/ValidationQueue";
import SubmissionReview from "./pages/SubmissionReview";
import InitiativeDiscovery from "./pages/InitiativeDiscovery";
import InitiativeBuilder from "./pages/InitiativeBuilder";
import InitiativeDashboard from "./pages/InitiativeDashboard";
import CampaignOps from "./pages/CampaignOps";
import LeadershipDashboard from "./pages/LeadershipDashboard";
import ObservabilityDashboard from "./pages/ObservabilityDashboard";
import TransparencyPage from "./pages/TransparencyPage";
import DeveloperPortal from "./pages/DeveloperPortal";
import LoadSimulation from "./pages/LoadSimulation";
import Documentation from "./pages/Documentation";
import SavedTasks from "./pages/SavedTasks";
import Organizations from "./pages/Organizations";
import InitiativeDetail from "./pages/InitiativeDetail";
import NotFound from "./pages/NotFound";

const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: (_error, query) => {
      if (query.meta?.suppressError) return;
    },
  }),
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => {
        if (error instanceof Error && /40[13]/.test(error.message)) return false;
        return failureCount < 2;
      },
    },
  },
});

const App = () => (
  <ErrorBoundary>
    <ThemeProvider defaultTheme="system">
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <TooltipProvider>
            <WalkthroughProvider>
            <Toaster />
            <Sonner />
            <BrowserRouter>
              <Routes>
              <Route path="/" element={<MissionFeed />} />
              <Route path="/task/:id" element={<TaskDetails />} />
              <Route path="/active" element={<ActiveWorkspace />} />
              <Route path="/contributions" element={<Contributions />} />
              <Route path="/notifications" element={<Notifications />} />
              <Route path="/profile" element={<Profile />} />
              <Route path="/validate" element={<ValidationQueue />} />
              <Route path="/validate/:id" element={<SubmissionReview />} />
              <Route path="/initiatives" element={<InitiativeDiscovery />} />
              <Route path="/initiatives/create" element={<InitiativeBuilder />} />
              <Route path="/initiatives/:id" element={<InitiativeDashboard />} />
              <Route path="/initiatives/:id/ops" element={<CampaignOps />} />
              <Route path="/leadership" element={<LeadershipDashboard />} />
              <Route path="/observability" element={<ObservabilityDashboard />} />
              <Route path="/transparency" element={<TransparencyPage />} />
              <Route path="/developers" element={<DeveloperPortal />} />
              <Route path="/load-simulation" element={<LoadSimulation />} />
              <Route path="/saved" element={<SavedTasks />} />
              <Route path="/organizations" element={<Organizations />} />
              <Route path="/initiative/:slug" element={<InitiativeDetail />} />
              <Route path="/docs" element={<Documentation />} />
              <Route path="*" element={<NotFound />} />
              </Routes>
            </BrowserRouter>
            <WalkthroughOverlay />
            </WalkthroughProvider>
          </TooltipProvider>
        </AuthProvider>
      </QueryClientProvider>
    </ThemeProvider>
  </ErrorBoundary>
);

export default App;
