import { useState } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { LogOut, Menu, X } from "lucide-react";
import { useAuth } from "@/lib/auth";
import { ROLE_LABEL } from "@/lib/labels";
import { initials } from "@/lib/format";
import { cn } from "@/lib/cn";
import { MOBILE_PRIMARY, navFor, type NavItem } from "./nav";

function Brand() {
  return (
    <Link to="/app" className="flex items-center gap-2.5 font-bold tracking-[-0.02em] text-ink">
      <img src="/favicon.svg" alt="" className="size-8" />
      <span className="text-[17px]">EduCenter</span>
    </Link>
  );
}

function SideLink({ item, onNavigate }: { item: NavItem; onNavigate?: () => void }) {
  return (
    <NavLink
      to={item.to}
      onClick={onNavigate}
      className={({ isActive }) =>
        cn(
          "flex h-10 items-center gap-3 rounded-lg px-3 text-sm font-medium transition-colors duration-[120ms]",
          isActive ? "bg-primary-soft text-primary-strong" : "text-ink-2 hover:bg-surface-2 hover:text-ink",
        )
      }
    >
      <item.icon className="size-[18px] shrink-0" strokeWidth={1.75} aria-hidden />
      {item.label}
    </NavLink>
  );
}

function NavGroups({ items, onNavigate }: { items: NavItem[]; onNavigate?: () => void }) {
  const groups = ["Vận hành", "Tài chính", "Quản trị"] as const;
  return (
    <nav aria-label="Điều hướng chính" className="flex flex-col gap-5">
      {groups.map((g) => {
        const list = items.filter((i) => i.group === g);
        if (!list.length) return null;
        return (
          <div key={g} className="flex flex-col gap-0.5">
            <p className="px-3 pb-1 text-[12px] font-medium text-ink-3">{g}</p>
            {list.map((i) => <SideLink key={i.to} item={i} onNavigate={onNavigate} />)}
          </div>
        );
      })}
    </nav>
  );
}

function UserBlock({ onNavigate }: { onNavigate?: () => void }) {
  const { user, logout } = useAuth();
  if (!user) return null;
  return (
    <div className="border-t border-line pt-3">
      <Link to="/app/account" onClick={onNavigate} className="flex items-center gap-3 rounded-lg px-2 py-2 hover:bg-surface-2">
        <span className="grid size-9 shrink-0 place-items-center rounded-full bg-primary-soft text-[13px] font-semibold text-primary-strong">{initials(user.fullName)}</span>
        <span className="min-w-0">
          <span className="block truncate text-sm font-medium text-ink">{user.fullName}</span>
          <span className="block truncate text-[12px] text-ink-2">{ROLE_LABEL[user.role]}</span>
        </span>
      </Link>
      <button onClick={logout} className="mt-1 flex h-10 w-full items-center gap-3 rounded-lg px-3 text-sm font-medium text-ink-2 hover:bg-surface-2 hover:text-danger">
        <LogOut className="size-[18px]" strokeWidth={1.75} aria-hidden />
        Đăng xuất
      </button>
    </div>
  );
}

export function AppLayout() {
  const { user } = useAuth();
  const [moreOpen, setMoreOpen] = useState(false);
  const location = useLocation();
  if (!user) return null;
  const items = navFor(user.role);
  const primaryPaths = MOBILE_PRIMARY[user.role] ?? [];
  const primary = primaryPaths.map((p) => items.find((i) => i.to === p)).filter(Boolean) as NavItem[];

  return (
    <div className="min-h-dvh lg:pl-[248px]">
      {/* Sidebar desktop */}
      <aside className="fixed inset-y-0 left-0 hidden w-[248px] flex-col border-r border-line bg-surface px-3 py-4 lg:flex">
        <div className="px-2 pb-1"><Brand /></div>
        {user.tenantName && <p className="truncate px-2 pb-5 text-[13px] text-ink-2">{user.tenantName}</p>}
        <div className="flex-1 overflow-y-auto"><NavGroups items={items} /></div>
        <UserBlock />
      </aside>

      {/* Top bar mobile */}
      <header className="sticky top-0 z-30 flex h-14 items-center justify-between border-b border-line bg-surface/95 px-4 backdrop-blur-sm lg:hidden">
        <Brand />
        <Link to="/app/account" className="grid size-9 place-items-center rounded-full bg-primary-soft text-[13px] font-semibold text-primary-strong" aria-label="Tài khoản">
          {initials(user.fullName)}
        </Link>
      </header>

      <main className="mx-auto w-full max-w-[1320px] px-4 pb-28 pt-5 sm:px-6 lg:px-8 lg:pb-12 lg:pt-8">
        <Outlet key={location.pathname.split("/")[2]} />
      </main>

      {/* Bottom nav mobile */}
      <nav aria-label="Điều hướng nhanh" className="fixed inset-x-0 bottom-0 z-30 border-t border-line bg-surface pb-[env(safe-area-inset-bottom)] lg:hidden">
        <div className="mx-auto flex max-w-lg">
          {primary.map((i) => (
            <NavLink key={i.to} to={i.to} className={({ isActive }) => cn("flex h-16 flex-1 flex-col items-center justify-center gap-1 text-[11.5px] font-medium", isActive ? "text-primary" : "text-ink-2")}>
              <i.icon className="size-[22px]" strokeWidth={1.75} aria-hidden />
              {i.label}
            </NavLink>
          ))}
          <button onClick={() => setMoreOpen(true)} className="flex h-16 flex-1 flex-col items-center justify-center gap-1 text-[11.5px] font-medium text-ink-2" aria-expanded={moreOpen}>
            <Menu className="size-[22px]" strokeWidth={1.75} aria-hidden />
            Thêm
          </button>
        </div>
      </nav>

      {moreOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 bg-ink/30 [animation:fade-in_200ms_ease-out]" onClick={() => setMoreOpen(false)} aria-hidden />
          <div role="dialog" aria-modal="true" aria-label="Menu" className="absolute inset-x-0 bottom-0 max-h-[85dvh] overflow-y-auto rounded-t-2xl bg-surface px-3 pb-[max(16px,env(safe-area-inset-bottom))] pt-3 [animation:sheet-in-up_280ms_var(--ease-out)]">
            <div className="mb-2 flex items-center justify-between px-2">
              <p className="text-[15px] font-semibold">{user.tenantName ?? "EduCenter"}</p>
              <button onClick={() => setMoreOpen(false)} className="grid size-10 place-items-center rounded-lg text-ink-2" aria-label="Đóng menu"><X className="size-5" /></button>
            </div>
            <NavGroups items={items} onNavigate={() => setMoreOpen(false)} />
            <div className="mt-4"><UserBlock onNavigate={() => setMoreOpen(false)} /></div>
          </div>
        </div>
      )}
    </div>
  );
}

