import {
  BarChart3, BookOpen, Building2, CalendarDays, ClipboardCheck, GraduationCap, LayoutDashboard, Settings, Sun, UserCog, Users, Wallet, type LucideIcon,
} from "lucide-react";
import type { Role } from "@/api/types";

export interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  roles: Role[];
  group: "Vận hành" | "Tài chính" | "Quản trị";
}

const M: Role[] = ["CENTER_ADMIN", "STAFF"];

export const NAV: NavItem[] = [
  { to: "/app/dashboard", label: "Tổng quan", icon: LayoutDashboard, roles: M, group: "Vận hành" },
  { to: "/app/today", label: "Hôm nay", icon: Sun, roles: ["TEACHER"], group: "Vận hành" },
  { to: "/app/students", label: "Học viên", icon: Users, roles: M, group: "Vận hành" },
  { to: "/app/classes", label: "Lớp học", icon: GraduationCap, roles: [...M, "TEACHER"], group: "Vận hành" },
  { to: "/app/schedule", label: "Lịch", icon: CalendarDays, roles: [...M, "TEACHER"], group: "Vận hành" },
  { to: "/app/attendance", label: "Điểm danh", icon: ClipboardCheck, roles: M, group: "Vận hành" },
  { to: "/app/billing", label: "Học phí", icon: Wallet, roles: M, group: "Tài chính" },
  { to: "/app/reports", label: "Báo cáo", icon: BarChart3, roles: M, group: "Tài chính" },
  { to: "/app/catalog", label: "Khóa học & phòng", icon: BookOpen, roles: M, group: "Quản trị" },
  { to: "/app/staff", label: "Nhân viên", icon: UserCog, roles: ["CENTER_ADMIN"], group: "Quản trị" },
  { to: "/app/settings", label: "Cài đặt", icon: Settings, roles: ["CENTER_ADMIN"], group: "Quản trị" },
  { to: "/app/tenants", label: "Trung tâm", icon: Building2, roles: ["SUPER_ADMIN"], group: "Quản trị" },
];

/** Mục ưu tiên trên thanh đáy mobile theo vai trò (tối đa 4, còn lại vào "Thêm"). */
export const MOBILE_PRIMARY: Partial<Record<Role, string[]>> = {
  CENTER_ADMIN: ["/app/dashboard", "/app/students", "/app/schedule", "/app/billing"],
  STAFF: ["/app/dashboard", "/app/students", "/app/attendance", "/app/billing"],
  TEACHER: ["/app/today", "/app/schedule", "/app/classes"],
  SUPER_ADMIN: ["/app/tenants"],
};

export function navFor(role: Role) {
  return NAV.filter((n) => n.roles.includes(role));
}
