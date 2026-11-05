import type { BookingStatus } from './types'

export const STATUS_LABEL: Record<BookingStatus, string> = {
  PENDING_PAYMENT: 'Chờ thanh toán',
  CONFIRMED: 'Đã xác nhận',
  CHECKED_IN: 'Đã đến',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
  NO_SHOW: 'Không đến',
}

export const STATUS_TONE: Record<BookingStatus, string> = {
  PENDING_PAYMENT: 'bg-warn-soft text-[oklch(0.45_0.1_70)]',
  CONFIRMED: 'bg-brand-soft text-brand-strong',
  CHECKED_IN: 'bg-info-soft text-info',
  COMPLETED: 'bg-ok-soft text-ok',
  CANCELLED: 'bg-danger-soft text-danger',
  NO_SHOW: 'bg-sunken text-ink-2',
}

/** Các bước chuyển hợp lệ – khớp BookingStatus.java */
export const NEXT_STATUS: Record<BookingStatus, BookingStatus[]> = {
  PENDING_PAYMENT: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['CHECKED_IN', 'NO_SHOW', 'CANCELLED'],
  CHECKED_IN: ['COMPLETED', 'CANCELLED'],
  COMPLETED: [],
  CANCELLED: [],
  NO_SHOW: [],
}

export const ACTION_LABEL: Partial<Record<BookingStatus, string>> = {
  CONFIRMED: 'Xác nhận',
  CHECKED_IN: 'Check-in',
  COMPLETED: 'Hoàn thành',
  NO_SHOW: 'Không đến',
  CANCELLED: 'Hủy lịch',
}

export const CATEGORY_LABEL = {
  SPA: 'Spa & massage',
  SALON: 'Salon tóc / nail',
  GYM: 'Phòng gym / PT',
  CLINIC: 'Phòng khám',
  OTHER: 'Khác',
} as const
