import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, errorMessage } from '../../lib/api'
import { addDays, ddmmyyyy, formatVnd, percent, vnNow } from '../../lib/format'
import type { ReportSummary } from '../../lib/types'
import { ErrorState, Input, PageHeader, Panel, Skeleton } from '../../components/ui'
import { Chip } from './BookingsPage'

type Preset = '7' | '30' | 'month' | 'custom'

export default function ReportsPage() {
  const today = vnNow().date
  const [preset, setPreset] = useState<Preset>('30')
  const [custom, setCustom] = useState({ from: addDays(today, -29), to: today })
  const range = useMemo(() => {
    if (preset === '7') return { from: addDays(today, -6), to: today }
    if (preset === '30') return { from: addDays(today, -29), to: today }
    if (preset === 'month') return { from: today.slice(0, 8) + '01', to: today }
    return custom
  }, [preset, custom, today])

  const q = useQuery({
    queryKey: ['report', range.from, range.to],
    queryFn: async () => (await api.get<ReportSummary>('/reports/summary', { params: range })).data,
  })
  const r = q.data

  return (
    <div>
      <PageHeader title="Báo cáo" description={`Từ ${ddmmyyyy(range.from)} đến ${ddmmyyyy(range.to)}. Doanh thu tính theo lịch hẹn đã hoàn thành.`} />
      <div className="mb-6 flex flex-wrap items-end gap-2">
        <Chip active={preset === '7'} onClick={() => setPreset('7')}>7 ngày</Chip>
        <Chip active={preset === '30'} onClick={() => setPreset('30')}>30 ngày</Chip>
        <Chip active={preset === 'month'} onClick={() => setPreset('month')}>Tháng này</Chip>
        <Chip active={preset === 'custom'} onClick={() => setPreset('custom')}>Tùy chọn</Chip>
        {preset === 'custom' && (
          <div className="flex gap-2">
            <Input aria-label="Từ ngày" type="date" value={custom.from} onChange={(e) => setCustom((c) => ({ ...c, from: e.target.value }))} className="!h-9 text-sm" />
            <Input aria-label="Đến ngày" type="date" value={custom.to} onChange={(e) => setCustom((c) => ({ ...c, to: e.target.value }))} className="!h-9 text-sm" />
          </div>
        )}
      </div>

      {q.isLoading ? (
        <Skeleton className="h-96" />
      ) : q.isError ? (
        <ErrorState message={errorMessage(q.error)} onRetry={() => q.refetch()} />
      ) : r ? (
        <div className="flex flex-col gap-6">
          <dl className="grid grid-cols-2 gap-x-8 gap-y-5 border-y border-line py-5 md:grid-cols-4">
            <Figure label="Doanh thu" value={formatVnd(r.revenue)} sub={`TB ${formatVnd(r.averageTicket)}/lịch`} />
            <Figure label="Lịch hẹn" value={String(r.totalBookings)} sub={`${r.completed} hoàn thành · ${r.upcoming} sắp tới`} />
            <Figure label="Tỷ lệ không đến" value={percent(r.noShowRate)} sub={`${r.noShow} lịch không đến`} />
            <Figure label="Tỷ lệ hủy" value={percent(r.cancelRate)} sub={`${r.cancelled} lịch đã hủy`} />
          </dl>

          <Panel className="p-5">
            <h2 className="font-semibold">Doanh thu theo ngày</h2>
            <p className="mb-4 text-sm text-ink-2">Di chuột hoặc chạm vào cột để xem chi tiết</p>
            <RevenueChart data={r.daily} />
            <details className="mt-3 text-sm">
              <summary className="cursor-pointer text-ink-2 hover:text-ink">Xem dạng bảng</summary>
              <table className="mt-2 w-full max-w-md text-left">
                <thead className="text-ink-2"><tr><th className="py-1 font-medium">Ngày</th><th className="py-1 text-right font-medium">Lịch hoàn thành</th><th className="py-1 text-right font-medium">Doanh thu</th></tr></thead>
                <tbody className="tabular">
                  {r.daily.map((d) => <tr key={d.date} className="border-t border-line"><td className="py-1">{ddmmyyyy(d.date)}</td><td className="py-1 text-right">{d.completed}</td><td className="py-1 text-right">{formatVnd(d.revenue)}</td></tr>)}
                </tbody>
              </table>
            </details>
          </Panel>

          <div className="grid gap-6 lg:grid-cols-2">
            <RankTable title="Dịch vụ bán chạy" items={r.topServices} />
            <RankTable title="Nhân viên doanh thu cao" items={r.topStaff} />
          </div>
        </div>
      ) : null}
    </div>
  )
}

function Figure({ label, value, sub }: { label: string; value: string; sub: string }) {
  return (
    <div>
      <dt className="text-sm text-ink-2">{label}</dt>
      <dd className="tabular mt-0.5 text-2xl font-semibold">{value}</dd>
      <dd className="text-sm text-ink-3">{sub}</dd>
    </div>
  )
}

function RevenueChart({ data }: { data: ReportSummary['daily'] }) {
  const [hover, setHover] = useState<number | null>(null)
  const W = 720
  const H = 220
  const padL = 44
  const padB = 24
  const max = Math.max(1, ...data.map((d) => d.revenue))
  const nice = niceCeil(max)
  const plotW = W - padL
  const plotH = H - padB - 8
  const bw = plotW / data.length
  const barW = Math.max(2, bw - 2) // 2px khe giữa các cột
  const ticks = [0, nice / 2, nice]
  const labelEvery = Math.ceil(data.length / 8)
  const h = hover !== null ? data[hover] : null

  return (
    <div className="relative">
      <svg viewBox={`0 0 ${W} ${H}`} className="h-auto w-full" role="img" aria-label="Biểu đồ cột doanh thu theo ngày" onMouseLeave={() => setHover(null)}>
        {ticks.map((t) => {
          const y = 8 + plotH - (t / nice) * plotH
          return (
            <g key={t}>
              <line x1={padL} x2={W} y1={y} y2={y} stroke="var(--color-line)" strokeWidth={1} />
              <text x={padL - 6} y={y + 4} textAnchor="end" fontSize={11} fill="var(--color-ink-3)">{shortVnd(t)}</text>
            </g>
          )
        })}
        {data.map((d, i) => {
          const bh = (d.revenue / nice) * plotH
          const x = padL + i * bw + 1
          const y = 8 + plotH - bh
          return (
            <g key={d.date} onMouseEnter={() => setHover(i)} onClick={() => setHover(i)}>
              <rect x={padL + i * bw} y={8} width={bw} height={plotH} fill="transparent" />
              {bh > 0 && (
                <path
                  d={roundedTop(x, y, barW, bh, Math.min(4, barW / 2))}
                  fill="var(--color-brand)"
                  opacity={hover === null || hover === i ? 1 : 0.45}
                  style={{ transition: 'opacity 120ms ease-out' }}
                />
              )}
              {i % labelEvery === 0 && (
                <text x={x + barW / 2} y={H - 6} textAnchor="middle" fontSize={11} fill="var(--color-ink-3)">{d.date.slice(8, 10)}/{d.date.slice(5, 7)}</text>
              )}
            </g>
          )
        })}
      </svg>
      {h && hover !== null && (
        <div
          className="pointer-events-none absolute top-0 z-10 rounded-[10px] bg-surface px-3 py-2 text-sm shadow-[var(--shadow-float)]"
          style={{ left: `${Math.min(80, Math.max(0, ((padL + hover * bw) / W) * 100 - 5))}%` }}
        >
          <p className="font-medium">{ddmmyyyy(h.date)}</p>
          <p className="tabular text-ink-2">{formatVnd(h.revenue)} · {h.completed} lịch</p>
        </div>
      )}
    </div>
  )
}

function roundedTop(x: number, y: number, w: number, h: number, r: number) {
  const rr = Math.min(r, h)
  return `M${x},${y + h}V${y + rr}Q${x},${y} ${x + rr},${y}H${x + w - rr}Q${x + w},${y} ${x + w},${y + rr}V${y + h}Z`
}

function niceCeil(v: number) {
  const exp = Math.pow(10, Math.floor(Math.log10(v)))
  const f = v / exp
  return (f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10) * exp
}

function shortVnd(v: number) {
  if (v >= 1_000_000) return `${(v / 1_000_000).toLocaleString('vi-VN', { maximumFractionDigits: 1 })}tr`
  if (v >= 1000) return `${Math.round(v / 1000)}k`
  return String(v)
}

function RankTable({ title, items }: { title: string; items: ReportSummary['topServices'] }) {
  const max = Math.max(1, ...items.map((i) => i.revenue))
  return (
    <Panel>
      <h2 className="border-b border-line px-5 py-3.5 font-semibold">{title}</h2>
      {items.length === 0 ? (
        <p className="px-5 py-6 text-ink-2">Chưa có dữ liệu trong kỳ.</p>
      ) : (
        <ol className="divide-y divide-line">
          {items.map((it) => (
            <li key={it.id} className="px-5 py-3">
              <div className="flex items-baseline justify-between gap-3">
                <span className="min-w-0 truncate">{it.name}</span>
                <span className="tabular shrink-0 font-medium">{formatVnd(it.revenue)}</span>
              </div>
              <div className="mt-1.5 flex items-center gap-3">
                <div className="h-1.5 flex-1 rounded-full bg-sunken">
                  <div className="h-full rounded-full bg-brand" style={{ width: `${(it.revenue / max) * 100}%` }} />
                </div>
                <span className="tabular w-16 shrink-0 text-right text-sm text-ink-2">{it.count} lịch</span>
              </div>
            </li>
          ))}
        </ol>
      )}
    </Panel>
  )
}
