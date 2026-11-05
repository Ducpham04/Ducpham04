import { useEffect, useMemo, useRef, useState, type DragEvent } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { clsx } from 'clsx'
import { ChevronLeft, ChevronRight, Plus } from 'lucide-react'
import { api, errorMessage } from '../../lib/api'
import { useAuth } from '../../lib/auth'
import { addDays, ddmmyyyy, hhmm, longDate, minutesToHHMM, parts, startOfWeek, vnNow, weekdayShort } from '../../lib/format'
import type { Booking, Staff } from '../../lib/types'
import { Avatar, Button, EmptyState, ErrorState, Panel, Segmented, Select, Skeleton, StatusIcon, useToast } from '../../components/ui'
import { useBranches, useStaffList } from './queries'
import { BookingDetailSheet, useInvalidateBookings } from './BookingDetailSheet'
import { NewBookingSheet } from './NewBookingSheet'

const START_HOUR = 6
const END_HOUR = 23
const HOUR_PX = 64
const PPM = HOUR_PX / 60
const SNAP = 15

type View = 'day' | 'week'
interface Column { key: string; label: React.ReactNode; date: string; staffId: number | null }

export default function CalendarPage() {
  const { user } = useAuth()
  const toast = useToast()
  const invalidate = useInvalidateBookings()
  const [view, setView] = useState<View>('day')
  const [date, setDate] = useState(vnNow().date)
  const [branchId, setBranchId] = useState<number | ''>('')
  const [weekStaff, setWeekStaff] = useState<number | ''>('')
  const [open, setOpen] = useState<number | null>(null)
  const [preset, setPreset] = useState<{ date: string; time: string; staffId?: number } | null>(null)
  const dragRef = useRef<{ id: number; grabOffsetMin: number } | null>(null)
  const scrollRef = useRef<HTMLDivElement>(null)
  const scrolled = useRef(false)
  useEffect(() => {
    // Lần đầu mở: cuộn tới khoảng 1 giờ trước hiện tại (hoặc 8:00).
    const el = scrollRef.current
    if (!el || scrolled.current) return
    scrolled.current = true
    const n = vnNow().minutes
    const target = n > 9 * 60 && n < 21 * 60 ? n - 60 : 8 * 60
    el.scrollTop = (target - START_HOUR * 60) * PPM
  })

  const branches = useBranches()
  const staff = useStaffList()
  const from = view === 'day' ? date : startOfWeek(date)
  const to = view === 'day' ? addDays(date, 1) : addDays(from, 7)

  const q = useQuery({
    queryKey: ['bookings', 'calendar', from, to, branchId],
    queryFn: async () =>
      (await api.get<Booking[]>('/bookings', { params: { from: `${from}T00:00:00`, to: `${to}T00:00:00`, branchId: branchId || undefined } })).data,
  })

  const visibleStaff = useMemo<Staff[]>(() => {
    const list = (staff.data ?? []).filter((s) => s.active && (!branchId || s.branchId === branchId))
    return user?.role === 'STAFF' ? list.filter((s) => s.id === user.staffId) : list
  }, [staff.data, branchId, user])

  const columns: Column[] = useMemo(() => {
    if (view === 'day') {
      return visibleStaff.map((s) => ({
        key: `s${s.id}`,
        date,
        staffId: s.id,
        label: (
          <span className="flex min-w-0 items-center gap-2">
            <Avatar name={s.fullName} color={s.color} size={28} />
            <span className="truncate">{s.fullName}</span>
          </span>
        ),
      }))
    }
    return Array.from({ length: 7 }, (_, i) => {
      const d = addDays(from, i)
      return {
        key: d,
        date: d,
        staffId: weekStaff || null,
        label: (
          <span className={clsx('flex flex-col leading-tight', d === vnNow().date && 'text-brand-strong')}>
            <span className="text-[0.8125rem] text-ink-2">{weekdayShort(d)}</span>
            <span className="tabular font-semibold">{ddmmyyyy(d).slice(0, 5)}</span>
          </span>
        ),
      }
    })
  }, [view, visibleStaff, date, from, weekStaff])

  const bookings = useMemo(
    () => (q.data ?? []).filter((b) => b.status !== 'CANCELLED' && (view === 'day' || !weekStaff || b.staff.id === weekStaff)),
    [q.data, view, weekStaff],
  )

  const reschedule = useMutation({
    mutationFn: async (v: { id: number; startAt: string; staffId?: number }) =>
      (await api.patch<Booking>(`/bookings/${v.id}/reschedule`, { startAt: v.startAt, staffId: v.staffId })).data,
    onSuccess: (b) => {
      invalidate()
      toast(`Đã đổi ${b.customer.fullName} sang ${hhmm(b.startAt)} ${ddmmyyyy(parts(b.startAt).date).slice(0, 5)} · ${b.staff.name}`)
    },
    onError: (e) => {
      invalidate()
      toast(errorMessage(e), 'error')
    },
  })

  function onDrop(e: DragEvent<HTMLDivElement>, col: Column) {
    e.preventDefault()
    const drag = dragRef.current
    dragRef.current = null
    if (!drag) return
    const rect = e.currentTarget.getBoundingClientRect()
    const rawMin = (e.clientY - rect.top) / PPM - drag.grabOffsetMin
    const minutes = Math.max(0, Math.round(rawMin / SNAP) * SNAP) + START_HOUR * 60
    const b = bookings.find((x) => x.id === drag.id)
    if (!b) return
    const startAt = `${col.date}T${minutesToHHMM(Math.min(minutes, END_HOUR * 60 - SNAP))}:00`
    const staffId = col.staffId ?? undefined
    if (startAt === b.startAt && (!staffId || staffId === b.staff.id)) return
    reschedule.mutate({ id: b.id, startAt, staffId })
  }

  function onEmptyClick(e: React.MouseEvent<HTMLDivElement>, col: Column) {
    if (e.target !== e.currentTarget) return
    const rect = e.currentTarget.getBoundingClientRect()
    const minutes = Math.floor((e.clientY - rect.top) / PPM / SNAP) * SNAP + START_HOUR * 60
    setPreset({ date: col.date, time: minutesToHHMM(minutes), staffId: col.staffId ?? undefined })
  }

  const now = vnNow()
  const hours = Array.from({ length: END_HOUR - START_HOUR }, (_, i) => START_HOUR + i)
  const step = view === 'day' ? 1 : 7
  const canDrag = (b: Booking) => ['CONFIRMED', 'PENDING_PAYMENT'].includes(b.status)

  return (
    <div>
      <div className="mb-5 flex flex-wrap items-center gap-3">
        <h1 className="mr-auto text-[1.75rem] font-semibold">Lịch</h1>
        <Button icon={<Plus className="size-4" />} onClick={() => setPreset({ date, time: '09:00' })}>Tạo lịch hẹn</Button>
      </div>
      <div className="mb-4 flex flex-wrap items-center gap-2">
        <div className="flex items-center gap-1">
          <Button variant="secondary" size="sm" aria-label="Trước" onClick={() => setDate(addDays(date, -step))}><ChevronLeft className="size-4" /></Button>
          <Button variant="secondary" size="sm" onClick={() => setDate(vnNow().date)}>Hôm nay</Button>
          <Button variant="secondary" size="sm" aria-label="Sau" onClick={() => setDate(addDays(date, step))}><ChevronRight className="size-4" /></Button>
        </div>
        <input type="date" value={date} onChange={(e) => e.target.value && setDate(e.target.value)} aria-label="Chọn ngày" className="h-9 rounded-[var(--radius-control)] border border-line-strong bg-surface px-2 text-sm" />
        <p className="font-medium">{view === 'day' ? longDate(date) : `${ddmmyyyy(from)} – ${ddmmyyyy(addDays(from, 6))}`}</p>
        <div className="ml-auto flex flex-wrap items-center gap-2">
          {(branches.data?.length ?? 0) > 1 && (
            <Select aria-label="Chi nhánh" value={branchId} onChange={(e) => setBranchId(e.target.value ? Number(e.target.value) : '')} className="!h-9 text-sm">
              <option value="">Tất cả chi nhánh</option>
              {branches.data?.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
            </Select>
          )}
          {view === 'week' && user?.role === 'OWNER' && (
            <Select aria-label="Nhân viên" value={weekStaff} onChange={(e) => setWeekStaff(e.target.value ? Number(e.target.value) : '')} className="!h-9 text-sm">
              <option value="">Tất cả nhân viên</option>
              {visibleStaff.map((s) => <option key={s.id} value={s.id}>{s.fullName}</option>)}
            </Select>
          )}
          <Segmented label="Chế độ xem" value={view} onChange={setView} options={[{ value: 'day', label: 'Ngày' }, { value: 'week', label: 'Tuần' }]} />
        </div>
      </div>

      {q.isError ? (
        <ErrorState message={errorMessage(q.error)} onRetry={() => q.refetch()} />
      ) : q.isLoading || staff.isLoading ? (
        <Skeleton className="h-[480px]" />
      ) : columns.length === 0 ? (
        <Panel><EmptyState title="Chưa có nhân viên" description="Thêm nhân viên và ca làm việc để bắt đầu nhận lịch." /></Panel>
      ) : (
        <>
          {/* Mobile: danh sách theo giờ */}
          <div className="md:hidden">
            <MobileAgenda bookings={bookings} onOpen={setOpen} />
          </div>
          {/* Desktop: lưới thời gian */}
          <Panel className="hidden overflow-hidden md:block">
            <div ref={scrollRef} className="max-h-[calc(100dvh-250px)] overflow-auto">
              <div style={{ minWidth: 64 + columns.length * 150 }}>
                <div className="sticky top-0 z-10 flex border-b border-line bg-surface">
                  <div className="w-16 shrink-0" />
                  {columns.map((c) => (
                    <div key={c.key} className="min-w-0 flex-1 border-l border-line px-3 py-2.5 text-sm font-medium">{c.label}</div>
                  ))}
                </div>
                <div className="relative flex pt-2">
                  <div className="w-16 shrink-0">
                    {hours.map((h) => (
                      <div key={h} style={{ height: HOUR_PX }} className="tabular -translate-y-2 pr-2 text-right text-[0.75rem] text-ink-3">{h === START_HOUR ? '' : `${h}:00`}</div>
                    ))}
                  </div>
                  {columns.map((col) => {
                    const items = bookings.filter((b) => parts(b.startAt).date === col.date && (view === 'week' || b.staff.id === col.staffId))
                    return (
                      <div
                        key={col.key}
                        data-testid={`col-${col.key}`}
                        className="relative min-w-0 flex-1 cursor-cell border-l border-line"
                        style={{
                          height: hours.length * HOUR_PX,
                          backgroundImage: `repeating-linear-gradient(to bottom, var(--color-line) 0 1px, transparent 1px ${HOUR_PX}px), repeating-linear-gradient(to bottom, oklch(0.95 0.005 160) 0 1px, transparent 1px ${HOUR_PX / 4}px)`,
                        }}
                        onDragOver={(e) => e.preventDefault()}
                        onDrop={(e) => onDrop(e, col)}
                        onClick={(e) => onEmptyClick(e, col)}
                      >
                        {col.date === now.date && now.minutes >= START_HOUR * 60 && now.minutes < END_HOUR * 60 && (
                          <div aria-hidden className="pointer-events-none absolute inset-x-0 z-[5] border-t-2 border-danger" style={{ top: (now.minutes - START_HOUR * 60) * PPM }} />
                        )}
                        {items.map((b) => {
                          const s = parts(b.startAt)
                          const e2 = parts(b.endAt)
                          const top = (s.minutes - START_HOUR * 60) * PPM
                          const height = Math.max((e2.minutes - s.minutes) * PPM - 2, 22)
                          const draggable = canDrag(b)
                          return (
                            <button
                              key={b.id}
                              draggable={draggable}
                              onDragStart={(ev) => {
                                const rect = (ev.currentTarget as HTMLElement).getBoundingClientRect()
                                dragRef.current = { id: b.id, grabOffsetMin: (ev.clientY - rect.top) / PPM }
                                ev.dataTransfer.effectAllowed = 'move'
                                ev.dataTransfer.setData('text/plain', String(b.id))
                              }}
                              onClick={() => setOpen(b.id)}
                              data-testid={`booking-${b.code}`}
                              className={clsx(
                                'absolute inset-x-1 overflow-hidden rounded-[8px] px-2 py-1 text-left text-[0.8125rem] leading-snug transition-shadow duration-150 hover:shadow-[var(--shadow-float)]',
                                draggable ? 'cursor-grab active:cursor-grabbing' : 'cursor-pointer',
                                b.status === 'COMPLETED' || b.status === 'NO_SHOW' ? 'opacity-60' : '',
                              )}
                              style={{
                                top,
                                height,
                                background: `color-mix(in oklch, ${b.staff.color} 14%, white)`,
                                color: `color-mix(in oklch, ${b.staff.color} 60%, black)`,
                                boxShadow: `inset 0 0 0 1px color-mix(in oklch, ${b.staff.color} 30%, white)`,
                              }}
                            >
                              <span className="flex items-center gap-1 font-semibold">
                                <StatusIcon status={b.status} className="size-3.5 shrink-0" />
                                <span className="tabular">{hhmm(b.startAt)}</span>
                                <span className="truncate">{b.customer.fullName}</span>
                              </span>
                              {height > 34 && <span className="block truncate">{b.service.name}</span>}
                              {view === 'week' && height > 52 && <span className="block truncate opacity-80">{b.staff.name}</span>}
                            </button>
                          )
                        })}
                      </div>
                    )
                  })}
                </div>
              </div>
            </div>
          </Panel>
          <p className="mt-2 hidden text-sm text-ink-3 md:block">Kéo thả lịch hẹn để đổi giờ{view === 'day' ? ' hoặc chuyển nhân viên' : ''}. Bấm vào ô trống để tạo lịch hẹn.</p>
        </>
      )}
      <BookingDetailSheet bookingId={open} onClose={() => setOpen(null)} />
      <NewBookingSheet open={preset !== null} preset={preset ?? undefined} onClose={() => setPreset(null)} />
    </div>
  )
}

function MobileAgenda({ bookings, onOpen }: { bookings: Booking[]; onOpen: (id: number) => void }) {
  if (bookings.length === 0) return <Panel><EmptyState title="Không có lịch hẹn" /></Panel>
  const byDate = bookings.reduce<Record<string, Booking[]>>((acc, b) => {
    const d = parts(b.startAt).date
    ;(acc[d] ??= []).push(b)
    return acc
  }, {})
  return (
    <div className="flex flex-col gap-5">
      {Object.entries(byDate).map(([d, list]) => (
        <section key={d}>
          <h2 className="mb-2 text-sm font-medium text-ink-2">{longDate(d)}</h2>
          <Panel>
            <ul className="divide-y divide-line">
              {list.map((b) => (
                <li key={b.id}>
                  <button onClick={() => onOpen(b.id)} className="flex w-full items-center gap-3 px-4 py-3 text-left">
                    <span className="size-2.5 shrink-0 rounded-full" style={{ background: b.staff.color }} aria-hidden />
                    <span className="tabular w-11 shrink-0 font-semibold">{hhmm(b.startAt)}</span>
                    <span className="min-w-0 flex-1">
                      <span className="block truncate font-medium">{b.customer.fullName}</span>
                      <span className="block truncate text-sm text-ink-2">{b.service.name} · {b.staff.name}</span>
                    </span>
                    <StatusIcon status={b.status} className="size-4 shrink-0 text-ink-2" />
                  </button>
                </li>
              ))}
            </ul>
          </Panel>
        </section>
      ))}
    </div>
  )
}
