import { ddmmyyyy, durationLabel, formatVnd, hhmm, parts, weekdayLong } from '../../lib/format'
import type { PublicBooking } from '../../lib/types'

export function BookingSummary({ b }: { b: PublicBooking }) {
  const p = parts(b.startAt)
  const rows: [string, React.ReactNode][] = [
    ['Dịch vụ', `${b.serviceName} · ${durationLabel(b.durationMinutes)}`],
    ['Thời gian', <span className="tabular">{hhmm(b.startAt)}–{hhmm(b.endAt)}, {weekdayLong(p.date)} {ddmmyyyy(p.date)}</span>],
    ['Nhân viên', b.staffName],
    ['Địa điểm', <>{b.branchName}{b.branchAddress && <span className="block text-ink-2">{b.branchAddress}</span>}</>],
    ['Khách hàng', b.customerName],
    ['Giá dịch vụ', <span className="tabular">{formatVnd(b.price)}</span>],
  ]
  if (b.amountDue > 0) rows.push(['Đã thanh toán', <span className="tabular">{formatVnd(b.amountPaid)} / {formatVnd(b.amountDue)}</span>])
  return (
    <dl className="divide-y divide-line">
      {rows.map(([k, v]) => (
        <div key={k} className="grid grid-cols-[110px_1fr] gap-3 py-2.5 text-[0.9375rem]">
          <dt className="text-ink-2">{k}</dt>
          <dd>{v}</dd>
        </div>
      ))}
    </dl>
  )
}
