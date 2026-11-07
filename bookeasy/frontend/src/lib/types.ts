export type Role = 'SUPER_ADMIN' | 'OWNER' | 'STAFF'
export type BookingStatus = 'PENDING_PAYMENT' | 'CONFIRMED' | 'CHECKED_IN' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW'
export type PaymentStatus = 'NOT_REQUIRED' | 'PENDING' | 'PAID'
export type PaymentMode = 'NONE' | 'DEPOSIT' | 'FULL'
export type BusinessCategory = 'SPA' | 'SALON' | 'GYM' | 'CLINIC' | 'OTHER'
export type BusinessStatus = 'ACTIVE' | 'SUSPENDED'

export interface Me {
  id: number
  email: string
  fullName: string
  role: Role
  businessId: number | null
  staffId: number | null
  businessName: string | null
  businessSlug: string | null
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: Me
}

export interface ApiErrorBody {
  status: number
  code: string
  message: string
  fieldErrors?: Record<string, string>
}

export interface PaymentInfo {
  qrUrl: string
  bankBin: string
  accountNo: string
  accountName: string | null
  amount: number
  content: string
}

// ---------- Công khai ----------
export interface PublicBranch { id: number; name: string; address: string | null; phone: string | null }
export interface PublicService { id: number; name: string; description: string | null; durationMinutes: number; price: number; categoryId: number | null }
export interface PublicCategory { id: number; name: string }
export interface PublicStaff { id: number; fullName: string; title: string | null; avatarUrl: string | null; color: string }
export interface PublicBusiness {
  slug: string
  name: string
  description: string | null
  phone: string | null
  address: string | null
  logoUrl: string | null
  brandColor: string
  category: BusinessCategory
  paymentMode: PaymentMode
  depositPercent: number
  bookingWindowDays: number
  branches: PublicBranch[]
  categories: PublicCategory[]
  services: PublicService[]
}
export interface SlotOption { time: string; staffIds: number[] }
export interface DayAvailability { date: string; available: boolean; slotCount: number }
export interface PublicBooking {
  code: string
  status: BookingStatus
  businessName: string
  businessSlug: string
  branchName: string
  branchAddress: string | null
  serviceName: string
  durationMinutes: number
  staffName: string
  startAt: string
  endAt: string
  price: number
  amountDue: number
  amountPaid: number
  paymentStatus: PaymentStatus
  paymentExpiresAt: string | null
  payment: PaymentInfo | null
  customerName: string
  cancellable: boolean
}

// ---------- Quản trị ----------
export interface Booking {
  id: number
  code: string
  status: BookingStatus
  source: 'ONLINE' | 'ADMIN' | 'WALK_IN'
  startAt: string
  endAt: string
  price: number
  amountDue: number
  amountPaid: number
  paymentStatus: PaymentStatus
  paymentExpiresAt: string | null
  customer: { id: number; fullName: string; phone: string; email: string | null }
  staff: { id: number; name: string; color: string }
  service: { id: number; name: string; durationMinutes: number }
  branch: { id: number; name: string }
  customerNote: string | null
  internalNote: string | null
  cancelReason: string | null
  createdAt: string
  payment: PaymentInfo | null
}

export interface BranchHour { dayOfWeek: number; openTime: string; closeTime: string; closed: boolean }
export interface Branch { id: number; name: string; address: string | null; phone: string | null; active: boolean; hours: BranchHour[] }
export interface Holiday { id: number; date: string; name: string; branchId: number | null }
export interface Business {
  id: number
  slug: string
  name: string
  description: string | null
  phone: string | null
  email: string | null
  address: string | null
  logoUrl: string | null
  brandColor: string
  category: BusinessCategory
  status: BusinessStatus
  paymentMode: PaymentMode
  depositPercent: number
  bankBin: string | null
  bankAccountNo: string | null
  bankAccountName: string | null
  minLeadMinutes: number
  bookingWindowDays: number
  cancelBeforeMinutes: number
}
export interface Category { id: number; name: string; sortOrder: number }
export interface Service {
  id: number
  name: string
  categoryId: number | null
  description: string | null
  durationMinutes: number
  price: number
  active: boolean
  sortOrder: number
  staffIds: number[]
}
export interface Staff {
  id: number
  fullName: string
  title: string | null
  phone: string | null
  email: string | null
  avatarUrl: string | null
  color: string
  branchId: number
  active: boolean
  serviceIds: number[]
  hasAccount: boolean
  accountEmail: string | null
}
export interface Shift { dayOfWeek: number; startTime: string; endTime: string }
export interface TimeOff { id: number; startDate: string; endDate: string; reason: string | null }

export interface Page<T> { items: T[]; page: number; size: number; totalElements: number; totalPages: number }
export interface CustomerSummary {
  id: number
  fullName: string
  phone: string
  email: string | null
  totalBookings: number
  visits: number
  noShows: number
  cancelled: number
  totalSpent: number
  lastVisitAt: string | null
  createdAt: string
}
export interface CustomerDetail {
  customer: CustomerSummary
  notes: string | null
  bookings: { id: number; code: string; startAt: string; status: BookingStatus; serviceName: string; staffName: string; price: number }[]
}

export interface Today {
  date: string
  total: number
  pendingPayment: number
  checkedIn: number
  completed: number
  cancelled: number
  noShow: number
  expectedRevenue: number
  upcoming: Booking[]
  needsAttention: Booking[]
}

export interface ReportSummary {
  from: string
  to: string
  totalBookings: number
  completed: number
  cancelled: number
  noShow: number
  upcoming: number
  revenue: number
  averageTicket: number
  noShowRate: number
  cancelRate: number
  daily: { date: string; revenue: number; completed: number }[]
  topServices: { id: number; name: string; count: number; revenue: number }[]
  topStaff: { id: number; name: string; count: number; revenue: number }[]
}

export interface AdminBusiness {
  id: number
  slug: string
  name: string
  category: BusinessCategory
  status: BusinessStatus
  ownerName: string | null
  ownerEmail: string | null
  bookingCount: number
  createdAt: string
}
