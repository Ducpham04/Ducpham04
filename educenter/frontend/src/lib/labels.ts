import type {
  AttendanceStatus, ClassStatus, EnrollmentStatus, InvoiceStatus, PaymentMethod, Role, SessionStatus, StudentStatus, TenantPlan,
} from "@/api/types";

export type Tone = "neutral" | "primary" | "ok" | "warn" | "danger" | "info";

export const ROLE_LABEL: Record<Role, string> = {
  SUPER_ADMIN: "Quản trị hệ thống",
  CENTER_ADMIN: "Chủ trung tâm",
  STAFF: "Giáo vụ / Thu ngân",
  TEACHER: "Giáo viên",
  STUDENT: "Phụ huynh / Học viên",
};

export const STUDENT_STATUS: Record<StudentStatus, { label: string; tone: Tone }> = {
  TRIAL: { label: "Học thử", tone: "info" },
  ACTIVE: { label: "Đang học", tone: "ok" },
  ON_HOLD: { label: "Bảo lưu", tone: "warn" },
  DROPPED: { label: "Đã nghỉ", tone: "neutral" },
};

export const CLASS_STATUS: Record<ClassStatus, { label: string; tone: Tone }> = {
  PLANNED: { label: "Sắp mở", tone: "info" },
  ONGOING: { label: "Đang học", tone: "ok" },
  FINISHED: { label: "Đã kết thúc", tone: "neutral" },
  CANCELLED: { label: "Đã hủy", tone: "danger" },
};

export const ENROLLMENT_STATUS: Record<EnrollmentStatus, { label: string; tone: Tone }> = {
  ACTIVE: { label: "Đang học", tone: "ok" },
  COMPLETED: { label: "Hoàn thành", tone: "primary" },
  DROPPED: { label: "Đã rời lớp", tone: "neutral" },
};

export const SESSION_STATUS: Record<SessionStatus, { label: string; tone: Tone }> = {
  SCHEDULED: { label: "Chưa dạy", tone: "neutral" },
  COMPLETED: { label: "Đã điểm danh", tone: "ok" },
  CANCELLED: { label: "Đã hủy", tone: "danger" },
};

export const ATTENDANCE_STATUS: Record<AttendanceStatus, { label: string; short: string; tone: Tone }> = {
  PRESENT: { label: "Có mặt", short: "C", tone: "ok" },
  LATE: { label: "Đi muộn", short: "M", tone: "warn" },
  EXCUSED: { label: "Vắng có phép", short: "P", tone: "info" },
  ABSENT: { label: "Vắng", short: "K", tone: "danger" },
};

export const INVOICE_STATUS: Record<InvoiceStatus | "OVERDUE", { label: string; tone: Tone }> = {
  UNPAID: { label: "Chưa thu", tone: "warn" },
  PARTIAL: { label: "Thu một phần", tone: "info" },
  PAID: { label: "Đã thu đủ", tone: "ok" },
  CANCELLED: { label: "Đã hủy", tone: "neutral" },
  OVERDUE: { label: "Quá hạn", tone: "danger" },
};

export const PAYMENT_METHOD: Record<PaymentMethod, string> = {
  CASH: "Tiền mặt",
  BANK_TRANSFER: "Chuyển khoản",
  CARD: "Thẻ",
  OTHER: "Khác",
};

export const PLAN_LABEL: Record<TenantPlan, string> = {
  TRIAL: "Dùng thử",
  BASIC: "Cơ bản",
  PRO: "Chuyên nghiệp",
  LIFETIME: "Bản quyền vĩnh viễn",
};

export const GENDER_LABEL = { MALE: "Nam", FEMALE: "Nữ", OTHER: "Khác" } as const;

export function invoiceStatusKey(i: { status: InvoiceStatus; overdue: boolean }): InvoiceStatus | "OVERDUE" {
  return i.overdue ? "OVERDUE" : i.status;
}
