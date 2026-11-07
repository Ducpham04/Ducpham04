import { lazy, Suspense, type ReactNode } from "react";
import { BrowserRouter, Navigate, Route, Routes, useLocation } from "react-router-dom";
import type { Role } from "@/api/types";
import { homePathFor, useAuth } from "@/lib/auth";
import { AppLayout } from "@/layouts/AppLayout";
import { LoginPage } from "@/pages/LoginPage";
import { Skeleton } from "@/components/ui";

const DashboardPage = lazy(() => import("@/pages/DashboardPage"));
const SessionsDayPage = lazy(() => import("@/pages/SessionsDayPage"));
const StudentsPage = lazy(() => import("@/pages/students/StudentsPage"));
const StudentDetailPage = lazy(() => import("@/pages/students/StudentDetailPage"));
const ClassesPage = lazy(() => import("@/pages/classes/ClassesPage"));
const ClassDetailPage = lazy(() => import("@/pages/classes/ClassDetailPage"));
const SchedulePage = lazy(() => import("@/pages/SchedulePage"));
const AttendanceTakePage = lazy(() => import("@/pages/AttendanceTakePage"));
const BillingPage = lazy(() => import("@/pages/billing/BillingPage"));
const InvoiceDetailPage = lazy(() => import("@/pages/billing/InvoiceDetailPage"));
const ReportsPage = lazy(() => import("@/pages/ReportsPage"));
const CatalogPage = lazy(() => import("@/pages/CatalogPage"));
const StaffPage = lazy(() => import("@/pages/StaffPage"));
const SettingsPage = lazy(() => import("@/pages/SettingsPage"));
const AccountPage = lazy(() => import("@/pages/AccountPage"));
const TenantsPage = lazy(() => import("@/pages/TenantsPage"));
const PortalPage = lazy(() => import("@/pages/portal/PortalPage"));

const MANAGERS: Role[] = ["CENTER_ADMIN", "STAFF"];

function PageFallback() {
  return (
    <div className="space-y-4" aria-busy="true" aria-label="Đang tải trang">
      <Skeleton className="h-8 w-56" />
      <Skeleton className="h-40 w-full" />
    </div>
  );
}

function RequireAuth({ roles, children }: { roles?: Role[]; children: ReactNode }) {
  const { user, ready } = useAuth();
  const location = useLocation();
  if (!ready) return <div className="p-8"><PageFallback /></div>;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (roles && !roles.includes(user.role)) return <Navigate to={homePathFor(user.role)} replace />;
  return <>{children}</>;
}

function HomeRedirect() {
  const { user, ready } = useAuth();
  if (!ready) return null;
  return <Navigate to={user ? homePathFor(user.role) : "/login"} replace />;
}

function NotFound() {
  return (
    <div className="grid min-h-[60dvh] place-items-center text-center">
      <div>
        <p className="text-5xl font-bold text-ink-3">404</p>
        <p className="mt-2 text-ink-2">Không tìm thấy trang bạn cần.</p>
        <a href="/" className="mt-4 inline-block font-semibold text-primary">Về trang chủ</a>
      </div>
    </div>
  );
}

const S = ({ children }: { children: ReactNode }) => <Suspense fallback={<PageFallback />}>{children}</Suspense>;

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomeRedirect />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/portal" element={<RequireAuth roles={["STUDENT"]}><S><PortalPage /></S></RequireAuth>} />
        <Route path="/app" element={<RequireAuth roles={["SUPER_ADMIN", "CENTER_ADMIN", "STAFF", "TEACHER"]}><AppLayout /></RequireAuth>}>
          <Route index element={<HomeRedirect />} />
          <Route path="dashboard" element={<RequireAuth roles={MANAGERS}><S><DashboardPage /></S></RequireAuth>} />
          <Route path="today" element={<RequireAuth roles={["TEACHER"]}><S><SessionsDayPage /></S></RequireAuth>} />
          <Route path="attendance" element={<RequireAuth roles={MANAGERS}><S><SessionsDayPage /></S></RequireAuth>} />
          <Route path="attendance/:sessionId" element={<RequireAuth roles={[...MANAGERS, "TEACHER"]}><S><AttendanceTakePage /></S></RequireAuth>} />
          <Route path="students" element={<RequireAuth roles={MANAGERS}><S><StudentsPage /></S></RequireAuth>} />
          <Route path="students/:id" element={<RequireAuth roles={MANAGERS}><S><StudentDetailPage /></S></RequireAuth>} />
          <Route path="classes" element={<RequireAuth roles={[...MANAGERS, "TEACHER"]}><S><ClassesPage /></S></RequireAuth>} />
          <Route path="classes/:id" element={<RequireAuth roles={[...MANAGERS, "TEACHER"]}><S><ClassDetailPage /></S></RequireAuth>} />
          <Route path="schedule" element={<RequireAuth roles={[...MANAGERS, "TEACHER"]}><S><SchedulePage /></S></RequireAuth>} />
          <Route path="billing" element={<RequireAuth roles={MANAGERS}><S><BillingPage /></S></RequireAuth>} />
          <Route path="billing/invoices/:id" element={<RequireAuth roles={MANAGERS}><S><InvoiceDetailPage /></S></RequireAuth>} />
          <Route path="reports" element={<RequireAuth roles={MANAGERS}><S><ReportsPage /></S></RequireAuth>} />
          <Route path="catalog" element={<RequireAuth roles={MANAGERS}><S><CatalogPage /></S></RequireAuth>} />
          <Route path="staff" element={<RequireAuth roles={["CENTER_ADMIN"]}><S><StaffPage /></S></RequireAuth>} />
          <Route path="settings" element={<RequireAuth roles={["CENTER_ADMIN"]}><S><SettingsPage /></S></RequireAuth>} />
          <Route path="account" element={<S><AccountPage /></S>} />
          <Route path="tenants" element={<RequireAuth roles={["SUPER_ADMIN"]}><S><TenantsPage /></S></RequireAuth>} />
          <Route path="*" element={<NotFound />} />
        </Route>
        <Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  );
}
