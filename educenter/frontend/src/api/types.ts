export type Role = "SUPER_ADMIN" | "CENTER_ADMIN" | "STAFF" | "TEACHER" | "STUDENT";

export interface Me {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  tenantId: number | null;
  tenantName: string | null;
  studentId: number | null;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: Me;
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export type StudentStatus = "TRIAL" | "ACTIVE" | "ON_HOLD" | "DROPPED";
export type Gender = "MALE" | "FEMALE" | "OTHER";

export interface StudentListItem {
  id: number;
  code: string;
  fullName: string;
  phone: string | null;
  status: StudentStatus;
  joinedAt: string;
  primaryGuardian: string | null;
  guardianPhone: string | null;
}

export interface Guardian {
  id: number;
  fullName: string;
  relationship: string | null;
  phone: string | null;
  email: string | null;
  zaloPhone: string | null;
  primary: boolean;
}

export interface Student {
  id: number;
  code: string;
  fullName: string;
  dob: string | null;
  gender: Gender | null;
  phone: string | null;
  email: string | null;
  school: string | null;
  level: string | null;
  address: string | null;
  status: StudentStatus;
  joinedAt: string;
  note: string | null;
  createdAt: string;
  guardians: Guardian[];
  portalEmail: string | null;
}

export interface Course {
  id: number;
  code: string;
  name: string;
  level: string | null;
  totalSessions: number;
  tuitionFee: number;
  description: string | null;
  active: boolean;
}

export interface Room {
  id: number;
  name: string;
  capacity: number;
  active: boolean;
}

export type ClassStatus = "PLANNED" | "ONGOING" | "FINISHED" | "CANCELLED";

export interface Slot {
  dayOfWeek: number;
  startTime: string;
  endTime: string;
}

export interface ClassListItem {
  id: number;
  code: string;
  name: string;
  courseId: number;
  courseName: string | null;
  teacherId: number | null;
  teacherName: string | null;
  roomId: number | null;
  roomName: string | null;
  maxStudents: number;
  enrolledCount: number;
  startDate: string;
  endDate: string | null;
  status: ClassStatus;
  schedules: Slot[];
}

export interface ClassDetail extends ClassListItem {
  note: string | null;
  totalSessions: number;
  completedSessions: number;
}

export type EnrollmentStatus = "ACTIVE" | "COMPLETED" | "DROPPED";

export interface ClassStudent {
  enrollmentId: number;
  studentId: number;
  studentCode: string;
  studentName: string;
  phone: string | null;
  studentStatus: StudentStatus;
  startDate: string;
  endDate: string | null;
  status: EnrollmentStatus;
  attendanceRate: number | null;
}

export interface StudentEnrollment {
  enrollmentId: number;
  classId: number;
  classCode: string;
  className: string;
  courseName: string | null;
  teacherName: string | null;
  startDate: string;
  endDate: string | null;
  status: EnrollmentStatus;
  classStatus: ClassStatus;
}

export type SessionStatus = "SCHEDULED" | "COMPLETED" | "CANCELLED";

export interface SessionView {
  id: number;
  classId: number;
  classCode: string;
  className: string;
  date: string;
  startTime: string;
  endTime: string;
  roomId: number | null;
  roomName: string | null;
  teacherId: number | null;
  teacherName: string | null;
  status: SessionStatus;
  note: string | null;
  attendanceCount: number;
  studentCount: number;
}

export interface Conflict {
  type: "ROOM" | "TEACHER";
  classId: number;
  classCode: string;
  className: string;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  message: string;
}

export type AttendanceStatus = "PRESENT" | "LATE" | "EXCUSED" | "ABSENT";

export interface RosterItem {
  studentId: number;
  studentCode: string;
  studentName: string;
  status: AttendanceStatus | null;
  note: string | null;
}

export interface SessionAttendance {
  session: SessionView;
  items: RosterItem[];
  taken: boolean;
}

export interface ClassRate {
  classId: number;
  classCode: string;
  className: string;
  present: number;
  late: number;
  excused: number;
  absent: number;
  total: number;
  rate: number;
}

export interface AttendanceHistoryItem {
  sessionId: number;
  classCode: string;
  className: string;
  date: string;
  startTime: string;
  status: AttendanceStatus;
  note: string | null;
}

export interface StudentAttendance {
  total: number;
  attended: number;
  rate: number;
  items: AttendanceHistoryItem[];
}

export interface FeePlan {
  id: number;
  name: string;
  courseId: number | null;
  courseName: string | null;
  amount: number;
  sessions: number | null;
  description: string | null;
  active: boolean;
}

export type InvoiceStatus = "UNPAID" | "PARTIAL" | "PAID" | "CANCELLED";
export type PaymentMethod = "CASH" | "BANK_TRANSFER" | "CARD" | "OTHER";

export interface Payment {
  id: number;
  receiptNo: string;
  amount: number;
  method: PaymentMethod;
  paidAt: string;
  note: string | null;
  receivedByName: string | null;
}

export interface Invoice {
  id: number;
  code: string;
  studentId: number;
  studentCode: string;
  studentName: string;
  classId: number | null;
  classCode: string | null;
  feePlanId: number | null;
  description: string;
  subtotal: number;
  discount: number;
  total: number;
  paidAmount: number;
  remaining: number;
  dueDate: string;
  status: InvoiceStatus;
  overdue: boolean;
  note: string | null;
  createdAt: string;
  payments: Payment[] | null;
}

export interface DebtItem {
  studentId: number;
  studentCode: string;
  studentName: string;
  phone: string | null;
  guardianPhone: string | null;
  invoiceCount: number;
  outstanding: number;
  overdueAmount: number;
  oldestDueDate: string;
}

export interface DebtSummary {
  totalOutstanding: number;
  totalOverdue: number;
  studentCount: number;
  items: DebtItem[];
}

export interface VietQr {
  imageUrl: string;
  bankCode: string;
  bankName: string;
  accountNo: string;
  accountName: string;
  amount: number;
  note: string;
}

export interface MonthValue {
  month: number;
  value: number;
}

export interface Dashboard {
  revenueThisMonth: number;
  revenueLastMonth: number;
  outstanding: number;
  overdue: number;
  newStudentsThisMonth: number;
  newStudentsLastMonth: number;
  activeStudents: number;
  attendanceRate30d: number;
  activeClasses: number;
  sessionsToday: SessionView[];
  topDebts: DebtItem[];
  revenue12Months: MonthValue[];
}

export interface StaffUser {
  id: number;
  email: string;
  fullName: string;
  phone: string | null;
  role: Role;
  enabled: boolean;
  lastLoginAt: string | null;
  createdAt: string;
}

export interface TeacherOption {
  id: number;
  fullName: string;
}

export type TenantPlan = "TRIAL" | "BASIC" | "PRO" | "LIFETIME";

export interface Tenant {
  id: number;
  name: string;
  slug: string;
  phone: string | null;
  email: string | null;
  address: string | null;
  plan: TenantPlan;
  status: "ACTIVE" | "SUSPENDED";
  bankCode: string | null;
  bankAccountNo: string | null;
  bankAccountName: string | null;
  createdAt: string;
  studentCount: number | null;
  userCount: number | null;
}

export interface NotificationLogItem {
  id: number;
  channel: "EMAIL" | "ZALO_ZNS";
  type: "SESSION_REMINDER" | "FEE_REMINDER";
  recipient: string;
  subject: string | null;
  status: "SENT" | "FAILED" | "MOCK" | "SKIPPED";
  error: string | null;
  createdAt: string;
}

export interface PortalMe {
  studentId: number;
  studentCode: string;
  fullName: string;
  status: StudentStatus;
  centerName: string;
  centerPhone: string | null;
  centerAddress: string | null;
  classes: ClassListItem[];
  nextSession: SessionView | null;
  attendanceRate: number;
  outstanding: number;
}
