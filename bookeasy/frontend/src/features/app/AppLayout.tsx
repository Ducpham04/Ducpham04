import { NavLink, Outlet, Link } from 'react-router-dom'
import { clsx } from 'clsx'
import { BarChart3, CalendarDays, ClipboardList, ExternalLink, Home, LogOut, Scissors, Settings, Users, UserSquare2 } from 'lucide-react'
import { useAuth } from '../../lib/auth'
import { Logo } from '../landing/Logo'

const NAV = [
  { to: '/app', label: 'Hôm nay', icon: Home, end: true, roles: ['OWNER', 'STAFF'] },
  { to: '/app/calendar', label: 'Lịch', icon: CalendarDays, roles: ['OWNER', 'STAFF'] },
  { to: '/app/bookings', label: 'Lịch hẹn', icon: ClipboardList, roles: ['OWNER', 'STAFF'] },
  { to: '/app/customers', label: 'Khách hàng', icon: Users, roles: ['OWNER'] },
  { to: '/app/services', label: 'Dịch vụ', icon: Scissors, roles: ['OWNER'] },
  { to: '/app/staff', label: 'Nhân viên', icon: UserSquare2, roles: ['OWNER'] },
  { to: '/app/reports', label: 'Báo cáo', icon: BarChart3, roles: ['OWNER'] },
  { to: '/app/settings', label: 'Cài đặt', icon: Settings, roles: ['OWNER'] },
]

export default function AppLayout() {
  const { user, logout } = useAuth()
  const items = NAV.filter((n) => user && n.roles.includes(user.role))
  const mobile = items.slice(0, 4)
  const more = user?.role === 'OWNER' ? '/app/settings' : null

  return (
    <div className="min-h-dvh md:grid md:grid-cols-[240px_1fr]">
      <aside className="sticky top-0 hidden h-dvh flex-col border-r border-line bg-surface md:flex">
        <div className="px-5 py-5"><Logo /></div>
        <div className="mx-3 mb-3 rounded-[10px] bg-sunken px-3 py-2.5">
          <p className="truncate text-sm font-medium">{user?.businessName}</p>
          {user?.businessSlug && (
            <a href={`/b/${user.businessSlug}`} target="_blank" rel="noreferrer" className="mt-0.5 inline-flex items-center gap-1 text-[0.8125rem] text-ink-2 hover:text-brand">
              Trang đặt lịch <ExternalLink className="size-3" aria-hidden />
            </a>
          )}
        </div>
        <nav className="flex flex-1 flex-col gap-0.5 px-3" aria-label="Điều hướng chính">
          {items.map((n) => (
            <NavLink
              key={n.to}
              to={n.to}
              end={n.end}
              className={({ isActive }) =>
                clsx(
                  'flex h-10 items-center gap-3 rounded-[10px] px-3 text-[0.9375rem] transition-colors duration-150',
                  isActive ? 'bg-brand-soft font-medium text-brand-strong' : 'text-ink-2 hover:bg-sunken hover:text-ink',
                )
              }
            >
              <n.icon className="size-[18px]" strokeWidth={1.75} aria-hidden />
              {n.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-line px-3 py-3">
          <div className="flex items-center gap-2 px-2">
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium">{user?.fullName}</p>
              <p className="truncate text-[0.8125rem] text-ink-3">{user?.role === 'OWNER' ? 'Chủ cơ sở' : 'Nhân viên'}</p>
            </div>
            <button onClick={logout} className="inline-flex size-10 items-center justify-center rounded-full text-ink-2 hover:bg-sunken hover:text-ink" aria-label="Đăng xuất" title="Đăng xuất">
              <LogOut className="size-[18px]" />
            </button>
          </div>
        </div>
      </aside>

      <div className="min-w-0">
        <header className="sticky top-0 z-20 flex items-center justify-between border-b border-line bg-surface px-4 py-3 md:hidden">
          <Link to="/app"><Logo /></Link>
          <button onClick={logout} className="inline-flex size-11 items-center justify-center rounded-full text-ink-2" aria-label="Đăng xuất">
            <LogOut className="size-5" />
          </button>
        </header>
        <main className="mx-auto max-w-[1280px] px-4 pt-6 pb-28 md:px-8 md:pb-12">
          <Outlet />
        </main>
      </div>

      <nav className="fixed inset-x-0 bottom-0 z-30 grid border-t border-line bg-surface pb-[env(safe-area-inset-bottom)] md:hidden" style={{ gridTemplateColumns: `repeat(${mobile.length + (more ? 1 : 0)}, 1fr)` }} aria-label="Điều hướng">
        {[...mobile, ...(more ? [{ to: '/app/more', label: 'Thêm', icon: Settings, end: false }] : [])].map((n) => (
          <NavLink
            key={n.to}
            to={n.to}
            end={n.end}
            className={({ isActive }) => clsx('flex h-16 flex-col items-center justify-center gap-1 text-[0.75rem]', isActive ? 'text-brand-strong font-medium' : 'text-ink-2')}
          >
            <n.icon className="size-5" strokeWidth={1.75} aria-hidden />
            {n.label}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}

export function MorePage() {
  const { user } = useAuth()
  const rest = NAV.filter((n) => user && n.roles.includes(user.role)).slice(4)
  return (
    <div>
      <h1 className="mb-4 text-[1.75rem] font-semibold">Thêm</h1>
      <ul className="divide-y divide-line rounded-[var(--radius-panel)] border border-line bg-surface">
        {rest.map((n) => (
          <li key={n.to}>
            <Link to={n.to} className="flex h-14 items-center gap-3 px-4">
              <n.icon className="size-5 text-ink-2" strokeWidth={1.75} aria-hidden /> {n.label}
            </Link>
          </li>
        ))}
        {user?.businessSlug && (
          <li>
            <a href={`/b/${user.businessSlug}`} target="_blank" rel="noreferrer" className="flex h-14 items-center gap-3 px-4">
              <ExternalLink className="size-5 text-ink-2" strokeWidth={1.75} aria-hidden /> Trang đặt lịch công khai
            </a>
          </li>
        )}
      </ul>
    </div>
  )
}
