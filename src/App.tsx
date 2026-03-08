import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route } from "react-router-dom";
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
import NotFound from "./pages/NotFound";

const queryClient = new QueryClient();

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
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
          <Route path="*" element={<NotFound />} />
        </Routes>
      </BrowserRouter>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;
