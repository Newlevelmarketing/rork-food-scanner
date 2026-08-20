import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route } from "react-router-dom";

import { Toaster } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { privacyPolicy, termsOfUse } from "@/lib/legal";
import { AppStoreProvider } from "@/store/AppStore";

import Index from "./pages/Index";
import { LegalPage } from "./pages/Legal";
import NotFound from "./pages/NotFound";

const queryClient = new QueryClient();

const App = () => (
  <QueryClientProvider client={queryClient}>
    <AppStoreProvider>
      <TooltipProvider>
        <Toaster />
        <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
          <Routes>
            <Route path="/" element={<Index />} />
            {/* Public legal documents. These URLs are submitted to App Store
                Connect and the Play Console, so their paths must stay stable. */}
            <Route path="/privacy" element={<LegalPage doc={privacyPolicy} />} />
            <Route path="/terms" element={<LegalPage doc={termsOfUse} />} />
            {/* ADD ALL CUSTOM ROUTES ABOVE THE CATCH-ALL "*" ROUTE */}
            <Route path="*" element={<NotFound />} />
          </Routes>
        </BrowserRouter>
      </TooltipProvider>
    </AppStoreProvider>
  </QueryClientProvider>
);

export default App;
