import { useLocation } from "react-router-dom";
import { Clock, House, LayoutGrid, User, type LucideIcon } from "lucide-react";
import { BottomNavigation, useNavigate } from "zmp-ui";

const TABS = [
  { key: "/", label: "Trang chủ", icon: House },
  { key: "/explore", label: "Khám phá", icon: LayoutGrid },
  { key: "/history", label: "Lịch sử", icon: Clock },
  { key: "/profile", label: "Hồ sơ", icon: User },
] as { key: string; label: string; icon: LucideIcon }[];

export const NAV_PATHS: string[] = TABS.map((t) => t.key);

export function BottomNav() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  if (!NAV_PATHS.includes(pathname)) return null;
  return (
    <BottomNavigation fixed activeKey={pathname} onChange={(key) => navigate(key, { animate: false })}>
      {TABS.map((t) => (
        <BottomNavigation.Item key={t.key} label={t.label} icon={<t.icon size={22} />} activeIcon={<t.icon size={22} strokeWidth={2.5} />} />
      ))}
    </BottomNavigation>
  );
}
