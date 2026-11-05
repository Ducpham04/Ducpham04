import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Plus, Search } from 'lucide-react'
import { clsx } from 'clsx'
import { api, errorMessage } from '../../lib/api'
import { addDays, dateTimeLabel, formatVnd, vnNow } from '../../lib/format'
import { STATUS_LABEL } from '../../lib/status'
import type { Booking, BookingStatus } from '../../lib/types'
import { Button, EmptyState, ErrorState, Input, PageHeader, Panel, Skeleton, StatusBadge } from '../../components/ui'
import { BookingDetailSheet } from './BookingDetailSheet'
import { NewBookingSheet } from './NewBookingSheet'

const RANGES = [
  { key: 'today', label: 'Hôm nay', from: 0, to: 1 },
  { key: 'next7', label: '7 ngày tới', from: 0, to: 7 },
  { key: 'past7', label: '7 ngày qua', from: -7, to: 0 },
  { key: 'past30', label: '30 ngày qua', from: -30, to: 1 },
] as const

export default function BookingsPage() {
  const [range, setRange] = useState<(typeof RANGES)[number]['key']>('next7')
  const [status, setStatus] = useState<BookingStatus | ''>('')
  const [q, setQ] = useState('')
  const [open, setOpen] = useState<number | null>(null)
  const [creating, setCreating] = useState(false)
  const r = RANGES.find((x) => x.key === range)!
  const today = vnNow().date
  const from = addDays(today, r.from)
  const to = addDays(today, r.to)

  const list = useQuery({
    queryKey: ['bookings', 'list', from, to, status, q],
    queryFn: async () =>
      (await api.get<Booking[]>('/bookings', { params: { from: `${from}T00:00:00`, to: `${to}T00:00:00`, status: status || undefined, q: q || undefined } })).data,
  })

  return (
    <div>
      <PageHeader title="Lịch hẹn" actions={<Button icon={<Plus className="size-4" />} onClick={() => setCreating(true)}>Tạo lịch hẹn</Button>} />
      <div className="mb-4 flex flex-col gap-3 md:flex-row md:items-center">
        <div className="relative md:w-72">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ink-3" aria-hidden />
          <Input aria-label="Tìm kiếm" placeholder="Tên, SĐT hoặc mã lịch hẹn" value={q} onChange={(e) => setQ(e.target.value)} className="pl-9" />
        </div>
        <div className="-mx-4 flex gap-2 overflow-x-auto px-4 md:mx-0 md:px-0">
          {RANGES.map((x) => <Chip key={x.key} active={range === x.key} onClick={() => setRange(x.key)}>{x.label}</Chip>)}
        </div>
        <select aria-label="Trạng thái" value={status} onChange={(e) => setStatus(e.target.value as BookingStatus | '')} className="h-10 rounded-[var(--radius-control)] border border-line-strong bg-surface px-3 text-sm md:ml-auto">
          <option value="">Mọi trạng thái</option>
          {Object.entries(STATUS_LABEL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
      </div>
      <Panel>
        {list.isLoading ? (
          <div className="space-y-2 p-4">{[0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-12" />)}</div>
        ) : list.isError ? (
          <ErrorState message={errorMessage(list.error)} onRetry={() => list.refetch()} />
        ) : list.data?.length === 0 ? (
          <EmptyState title="Không có lịch hẹn phù hợp" description="Thử đổi khoảng thời gian hoặc bộ lọc trạng thái." />
        ) : (
          <table className="w-full text-[0.9375rem]">
            <thead className="hidden border-b border-line text-left text-sm text-ink-2 md:table-header-group">
              <tr>
                <th className="px-5 py-3 font-medium">Thời gian</th>
                <th className="px-3 py-3 font-medium">Khách hàng</th>
                <th className="px-3 py-3 font-medium">Dịch vụ</th>
                <th className="px-3 py-3 font-medium">Nhân viên</th>
                <th className="px-3 py-3 text-right font-medium">Giá</th>
                <th className="px-5 py-3 font-medium">Trạng thái</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {list.data?.map((b) => (
                <tr key={b.id} onClick={() => setOpen(b.id)} className="grid cursor-pointer grid-cols-[1fr_auto] gap-x-3 px-4 py-3 hover:bg-sunken md:table-row md:p-0">
                  <td className="tabular md:px-5 md:py-3">{dateTimeLabel(b.startAt)}<span className="block font-mono text-[0.8125rem] text-ink-3">{b.code}</span></td>
                  <td className="row-start-2 md:px-3 md:py-3"><span className="font-medium">{b.customer.fullName}</span><span className="block text-sm text-ink-2">{b.customer.phone}</span></td>
                  <td className="col-span-2 text-ink-2 md:px-3 md:py-3 md:text-ink">{b.service.name}</td>
                  <td className="hidden md:table-cell md:px-3 md:py-3">{b.staff.name}</td>
                  <td className="tabular hidden text-right md:table-cell md:px-3 md:py-3">{formatVnd(b.price)}</td>
                  <td className="col-start-2 row-start-1 justify-self-end md:px-5 md:py-3"><StatusBadge status={b.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Panel>
      <BookingDetailSheet bookingId={open} onClose={() => setOpen(null)} />
      <NewBookingSheet open={creating} onClose={() => setCreating(false)} />
    </div>
  )
}

export function Chip({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      onClick={onClick}
      aria-pressed={active}
      className={clsx(
        'h-9 shrink-0 rounded-full border px-3.5 text-sm font-medium transition-colors duration-150',
        active ? 'border-ink bg-ink text-white' : 'border-line-strong bg-surface text-ink-2 hover:text-ink',
      )}
    >
      {children}
    </button>
  )
}
