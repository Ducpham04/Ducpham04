import { useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { api, errorMessage } from '../../lib/api'
import { useAuth } from '../../lib/auth'
import { formatVnd, hhmm, longDate, vnNow } from '../../lib/format'
import type { Booking, Today } from '../../lib/types'
import { Avatar, Button, EmptyState, ErrorState, Panel, Skeleton, StatusBadge, useToast } from '../../components/ui'
import { BookingDetailSheet, useInvalidateBookings } from './BookingDetailSheet'
import { NewBookingSheet } from './NewBookingSheet'

export default function TodayPage() {
  const { user } = useAuth()
  const [open, setOpen] = useState<number | null>(null)
  const [creating, setCreating] = useState(false)
  const q = useQuery({ queryKey: ['today'], queryFn: async () => (await api.get<Today>('/dashboard/today')).data, refetchInterval: 60_000 })
  const t = q.data
  const hour = Math.floor(vnNow().minutes / 60)
  const greeting = hour < 12 ? 'Chào buổi sáng' : hour < 18 ? 'Chào buổi chiều' : 'Chào buổi tối'

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-ink-2">{t ? longDate(t.date) : ' '}</p>
          <h1 className="text-[1.75rem] font-semibold">{greeting}, {user?.fullName.split(' ').slice(-1)[0]}</h1>
        </div>
        <Button icon={<Plus className="size-4" />} onClick={() => setCreating(true)}>Tạo lịch hẹn</Button>
      </div>

      {q.isLoading ? (
        <Skeleton className="h-24" />
      ) : q.isError ? (
        <ErrorState message={errorMessage(q.error)} onRetry={() => q.refetch()} />
      ) : t ? (
        <>
          <p className="mb-8 max-w-[60ch] text-lg leading-relaxed">
            Hôm nay có <strong className="font-semibold">{t.total} lịch hẹn</strong>
            {t.completed > 0 && <>, đã hoàn thành <strong className="font-semibold">{t.completed}</strong></>}
            {t.checkedIn > 0 && <>, <strong className="font-semibold">{t.checkedIn}</strong> khách đang được phục vụ</>}
            {t.pendingPayment > 0 && <>, <strong className="font-semibold text-[oklch(0.45_0.1_70)]">{t.pendingPayment} chờ thanh toán</strong></>}
            .{user?.role === 'OWNER' && t.expectedRevenue > 0 && <> Doanh thu dự kiến <strong className="tabular font-semibold">{formatVnd(t.expectedRevenue)}</strong>.</>}
          </p>

          <div className="grid gap-6 lg:grid-cols-[1fr_380px]">
            <Panel>
              <div className="flex items-center justify-between border-b border-line px-5 py-3.5">
                <h2 className="font-semibold">Sắp tới</h2>
                <Link to="/app/calendar" className="text-sm text-brand hover:underline">Xem lịch</Link>
              </div>
              {t.upcoming.length === 0 ? (
                <EmptyState title="Không còn lịch hẹn nào hôm nay" description="Chia sẻ trang đặt lịch lên Facebook, Zalo để khách tự đặt." />
              ) : (
                <ul className="divide-y divide-line">
                  {t.upcoming.map((b) => <UpcomingRow key={b.id} b={b} onOpen={() => setOpen(b.id)} />)}
                </ul>
              )}
            </Panel>
            <Panel className="self-start">
              <h2 className="border-b border-line px-5 py-3.5 font-semibold">Cần xử lý</h2>
              {t.needsAttention.length === 0 ? (
                <p className="px-5 py-6 text-ink-2">Mọi lịch hẹn đã được cập nhật trạng thái.</p>
              ) : (
                <ul className="divide-y divide-line">
                  {t.needsAttention.map((b) => (
                    <li key={b.id}>
                      <button onClick={() => setOpen(b.id)} className="flex w-full items-center gap-3 px-5 py-3 text-left hover:bg-sunken">
                        <span className="tabular w-12 shrink-0 font-medium">{hhmm(b.startAt)}</span>
                        <span className="min-w-0 flex-1">
                          <span className="block truncate">{b.customer.fullName}</span>
                          <span className="block truncate text-sm text-ink-2">{b.status === 'CONFIRMED' ? 'Chưa check-in' : 'Chưa đánh dấu hoàn thành'}</span>
                        </span>
                        <StatusBadge status={b.status} />
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </Panel>
          </div>
        </>
      ) : null}
      <BookingDetailSheet bookingId={open} onClose={() => setOpen(null)} />
      <NewBookingSheet open={creating} onClose={() => setCreating(false)} />
    </div>
  )
}

function UpcomingRow({ b, onOpen }: { b: Booking; onOpen: () => void }) {
  const toast = useToast()
  const invalidate = useInvalidateBookings()
  const checkIn = useMutation({
    mutationFn: () => api.post(`/bookings/${b.id}/status`, { status: 'CHECKED_IN' }),
    onSuccess: () => { invalidate(); toast(`${b.customer.fullName} đã check-in`) },
    onError: (e) => toast(errorMessage(e), 'error'),
  })
  return (
    <li className="flex items-center gap-3 px-5 py-3">
      <button onClick={onOpen} className="flex min-w-0 flex-1 items-center gap-3 text-left">
        <span className="tabular w-12 shrink-0 font-semibold">{hhmm(b.startAt)}</span>
        <Avatar name={b.staff.name} color={b.staff.color} size={32} />
        <span className="min-w-0">
          <span className="block truncate font-medium">{b.customer.fullName}</span>
          <span className="block truncate text-sm text-ink-2">{b.service.name} · {b.staff.name}</span>
        </span>
      </button>
      {b.status === 'CONFIRMED' ? (
        <Button size="sm" variant="secondary" loading={checkIn.isPending} onClick={() => checkIn.mutate()}>Check-in</Button>
      ) : (
        <StatusBadge status={b.status} />
      )}
    </li>
  )
}
