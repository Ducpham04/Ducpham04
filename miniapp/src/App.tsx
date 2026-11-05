import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Route } from "react-router-dom";
import { AnimationRoutes, App as ZApp, Button, SnackbarProvider, Spinner, ZMPRouter } from "zmp-ui";
import { ApiError } from "./api/client";
import { AuthProvider, useAuth } from "./auth/AuthProvider";
import { BottomNav } from "./components/BottomNav";
import { config } from "./config";
import ExplorePage from "./pages/ExplorePage";
import FeedbackDetailPage from "./pages/FeedbackDetailPage";
import FeedbackFormPage from "./pages/FeedbackFormPage";
import HistoryPage from "./pages/HistoryPage";
import HomePage from "./pages/HomePage";
import NewsDetailPage from "./pages/NewsDetailPage";
import OrderDetailPage from "./pages/OrderDetailPage";
import ProductDetailPage from "./pages/ProductDetailPage";
import ProfilePage from "./pages/ProfilePage";
import { MockCheckoutHost, MockPanel } from "./zalo/MockPanel";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: false,
      // Không retry lỗi 4xx (dữ liệu sai / không có quyền)
      retry: (count, err) => !(err instanceof ApiError && err.status >= 400 && err.status < 500) && count < 2,
    },
  },
});

function Shell() {
  const { status, error, login } = useAuth();
  if (status === "loading") {
    return (
      <div className="h-full flex flex-col items-center justify-center gap-4 bg-white" data-testid="splash">
        <div className="w-20 h-20 rounded-3xl bg-primary text-white flex items-center justify-center text-3xl font-bold">P</div>
        <div className="font-semibold text-lg">{config.appName}</div>
        <Spinner />
        <div className="text-sm text-ink-muted">Đang đăng nhập bằng Zalo…</div>
      </div>
    );
  }
  if (status === "error") {
    return (
      <div className="h-full flex flex-col items-center justify-center gap-3 px-8 text-center bg-white" role="alert">
        <div className="text-lg font-semibold">Không thể đăng nhập</div>
        <div className="text-sm text-ink-muted">{error}</div>
        <Button onClick={() => void login()}>Thử lại</Button>
      </div>
    );
  }
  return (
    <ZMPRouter>
      <AnimationRoutes>
        <Route path="/" element={<HomePage />} />
        <Route path="/explore" element={<ExplorePage />} />
        <Route path="/news/:id" element={<NewsDetailPage />} />
        <Route path="/products/:id" element={<ProductDetailPage />} />
        <Route path="/feedback/new" element={<FeedbackFormPage />} />
        <Route path="/feedback/:id" element={<FeedbackDetailPage />} />
        <Route path="/history" element={<HistoryPage />} />
        <Route path="/orders/:id" element={<OrderDetailPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="*" element={<HomePage />} />
      </AnimationRoutes>
      <BottomNav />
    </ZMPRouter>
  );
}

export default function App() {
  return (
    <ZApp>
      <QueryClientProvider client={queryClient}>
        <SnackbarProvider>
          <AuthProvider>
            <Shell />
            {config.zaloMode === "mock" && (
              <>
                <MockPanel />
                <MockCheckoutHost />
              </>
            )}
          </AuthProvider>
        </SnackbarProvider>
      </QueryClientProvider>
    </ZApp>
  );
}
