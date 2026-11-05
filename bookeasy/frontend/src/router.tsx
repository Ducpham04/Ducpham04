import { lazy, Suspense, type ReactNode } from 'react'
import { createBrowserRouter, Navigate, Outlet, useLocation } from 'react-router-dom'
import { homeFor, useAuth } from './lib/auth'
import type { Role } from './lib/types'
import { Spinner } from './components/ui'
import LandingPage from './features/landing/LandingPage'
import BookingPage from './features/public/BookingPage'
import BookingResultPage from './features/public/BookingResultPage'
import LookupPage from './features/public/LookupPage'
import LoginPage from './features/auth/LoginPage'
import RegisterPage from './features/auth/RegisterPage'

const AppLayout = lazy(() => import('./features/app/AppLayout'))
const MorePage = lazy(() => import('./features/app/AppLayout').then((m) => ({ default: m.MorePage })))
const TodayPage = lazy(() => import('./features/app/TodayPage'))
const CalendarPage = lazy(() => import('./features/app/CalendarPage'))
const BookingsPage = lazy(() => import('./features/app/BookingsPage'))
const CustomersPage = lazy(() => import('./features/app/CustomersPage'))
const CustomerDetailPage = lazy(() => import('./features/app/CustomersPage').then((m) => ({ default: m.CustomerDetailPage })))
const ServicesPage = lazy(() => import('./features/app/ServicesPage'))
const StaffPage = lazy(() => import('./features/app/StaffPage'))
const ReportsPage = lazy(() => import('./features/app/ReportsPage'))
const SettingsPage = lazy(() => import('./features/app/SettingsPage'))
const AdminPage = lazy(() => import('./features/admin/AdminPage'))

function RequireRole({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const { user, ready } = useAuth()
  const location = useLocation()
  if (!ready && !user) return <Spinner />
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  if (!roles.includes(user.role)) return <Navigate to={homeFor(user)} replace />
  return <>{children}</>
}

const lazyEl = (el: ReactNode) => <Suspense fallback={<Spinner />}>{el}</Suspense>

function NotFound() {
  return (
    <div className="mx-auto max-w-md px-4 py-24 text-center">
      <h1 className="text-2xl font-semibold">Không tìm thấy trang</h1>
      <p className="mt-2 text-ink-2">Đường dẫn có thể đã thay đổi hoặc không tồn tại.</p>
      <a href="/" className="mt-6 inline-block text-brand underline">Về trang chủ</a>
    </div>
  )
}

export const router = createBrowserRouter([
  { path: '/', element: <LandingPage /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  { path: '/b/:slug', element: <BookingPage /> },
  { path: '/b/:slug/booking/:code', element: <BookingResultPage /> },
  { path: '/b/:slug/tra-cuu', element: <LookupPage /> },
  {
    path: '/app',
    element: <RequireRole roles={['OWNER', 'STAFF']}>{lazyEl(<AppLayout />)}</RequireRole>,
    children: [
      { index: true, element: lazyEl(<TodayPage />) },
      { path: 'calendar', element: lazyEl(<CalendarPage />) },
      { path: 'bookings', element: lazyEl(<BookingsPage />) },
      { path: 'more', element: lazyEl(<MorePage />) },
      {
        element: <RequireRole roles={['OWNER']}><Outlet /></RequireRole>,
        children: [
          { path: 'customers', element: lazyEl(<CustomersPage />) },
          { path: 'customers/:id', element: lazyEl(<CustomerDetailPage />) },
          { path: 'services', element: lazyEl(<ServicesPage />) },
          { path: 'staff', element: lazyEl(<StaffPage />) },
          { path: 'reports', element: lazyEl(<ReportsPage />) },
          { path: 'settings', element: lazyEl(<SettingsPage />) },
        ],
      },
    ],
  },
  { path: '/admin', element: <RequireRole roles={['SUPER_ADMIN']}>{lazyEl(<AdminPage />)}</RequireRole> },
  { path: '*', element: <NotFound /> },
])
