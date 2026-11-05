import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useLocation, useParams } from 'react-router-dom'
import { Copy, Clock } from 'lucide-react'
import { api, errorMessage } from '../../lib/api'
import { formatVnd, vnNow } from '../../lib/format'
import type { PublicBooking } from '../../lib/types'
import { Button, ErrorState, Panel, Skeleton, StatusBadge, useToast } from '../../components/ui'
import { PublicShell } from './PublicShell'
import { usePublicBusiness } from './BookingPage'
import { BookingSummary } from './BookingSummary'
import { LookupForm } from './LookupPage'

function storedPhone(code: string): string | null {
  try {
    return sessionStorage.getItem(`bookeasy.phone.${code}`)
  } catch {
    return null
  }
}

export default function BookingResultPage() {
  const { slug = '', code = '' } = useParams()
  const location = useLocation()
  const state = location.state as { phone?: string; fresh?: boolean } | null
  const [phone, setPhone] = useState<string | null>(state?.phone ?? storedPhone(code))
  const biz = usePublicBusiness(slug)

  const q = useQuery({
    queryKey: ['public-booking', code, phone],
    enabled: !!phone,
    queryFn: async () => (await api.get<PublicBooking>(`/public/bookings/${code}`, { params: { phone } })).data,
    refetchInterval: (query) => (query.state.data?.status === 'PENDING_PAYMENT' ? 5000 : false),
  })

  return (
    <PublicShell business={biz.data}>
      {!phone ? (
        <LookupForm initialCode={code} onFound={(p) => setPhone(p)} />
      ) : q.isLoading ? (
        <Skeleton className="h-80" />
      ) : q.isError ? (
        <ErrorState message={errorMessage(q.error)} onRetry={() => q.refetch()} />
      ) : q.data ? (
        <Result b={q.data} phone={phone} fresh={!!state?.fresh} />
      ) : null}
    </PublicShell>
  )
}

function Result({ b, phone, fresh }: { b: PublicBooking; phone: string; fresh: boolean }) {
  const toast = useToast()
  const qc = useQueryClient()
  const simulate = useMutation({
    mutationFn: () => api.post(`/public/bookings/${b.code}/simulate-payment`, null, { params: { phone } }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['public-booking', b.code] }),
    onError: (e) => toast(errorMessage(e), 'error'),
  })
  const pending = b.status === 'PENDING_PAYMENT'
  const confirmed = b.status === 'CONFIRMED' || b.status === 'CHECKED_IN' || b.status === 'COMPLETED'

  function copy(text: string) {
    navigator.clipboard?.writeText(text).then(() => toast('Đã sao chép'), () => undefined)
  }

  return (
    <div className="anim-rise">
      <div className="mb-6 flex items-start gap-4">
        {confirmed && (
          <svg viewBox="0 0 48 48" className="size-12 shrink-0" aria-hidden>
            <circle cx="24" cy="24" r="22" fill="var(--color-brand-soft)" />
            <path className={fresh ? 'check-draw' : ''} d="M14 24.5l7 7 13-14" fill="none" stroke="var(--color-brand)" strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        )}
        <div>
          <h1 className="text-2xl font-semibold">
            {pending ? 'Hoàn tất thanh toán để giữ lịch' : confirmed ? 'Đặt lịch thành công' : b.status === 'CANCELLED' ? 'Lịch hẹn đã hủy' : 'Chi tiết lịch hẹn'}
          </h1>
          <p className="mt-1 text-ink-2">
            {pending
              ? 'Quét mã QR bằng ứng dụng ngân hàng. Hệ thống tự xác nhận khi nhận được tiền.'
              : confirmed
                ? 'Chúng tôi đã gửi xác nhận và sẽ nhắc bạn trước giờ hẹn.'
                : 'Bạn có thể đặt lịch mới bất cứ lúc nào.'}
          </p>
        </div>
      </div>

      <div className="mb-5 flex items-center justify-between gap-3 rounded-[var(--radius-panel)] bg-sunken px-4 py-3">
        <div>
          <p className="text-sm text-ink-2">Mã lịch hẹn</p>
          <p className="font-mono text-2xl font-semibold tracking-wider" data-testid="booking-code">{b.code}</p>
        </div>
        <div className="flex flex-col items-end gap-2">
          <StatusBadge status={b.status} />
          <button onClick={() => copy(b.code)} className="inline-flex items-center gap-1 text-sm text-ink-2 hover:text-ink">
            <Copy className="size-3.5" aria-hidden /> Sao chép
          </button>
        </div>
      </div>

      {pending && b.payment && (
        <Panel className="mb-5 p-5">
          <div className="flex flex-col items-center gap-4 sm:flex-row sm:items-start">
            <img src={b.payment.qrUrl} alt={`Mã VietQR chuyển ${formatVnd(b.payment.amount)}`} width={240} height={240} className="size-60 rounded-[10px] border border-line bg-white object-contain" />
            <dl className="w-full space-y-2.5 text-[0.9375rem]">
              <div>
                <dt className="text-sm text-ink-2">Số tiền</dt>
                <dd className="tabular text-xl font-semibold">{formatVnd(b.payment.amount)}</dd>
              </div>
              <div>
                <dt className="text-sm text-ink-2">Nội dung chuyển khoản</dt>
                <dd className="flex items-center gap-2 font-mono text-lg font-semibold">
                  {b.payment.content}
                  <button onClick={() => copy(b.payment!.content)} aria-label="Sao chép nội dung" className="text-ink-2 hover:text-ink"><Copy className="size-4" /></button>
                </dd>
              </div>
              <div>
                <dt className="text-sm text-ink-2">Tài khoản</dt>
                <dd>{b.payment.accountNo}{b.payment.accountName && ` · ${b.payment.accountName}`}</dd>
              </div>
              {b.paymentExpiresAt && <Countdown until={b.paymentExpiresAt} />}
            </dl>
          </div>
          {import.meta.env.VITE_DEMO !== 'false' && (
            <div className="mt-4 border-t border-line pt-4">
              <Button variant="secondary" size="sm" loading={simulate.isPending} onClick={() => simulate.mutate()}>
                Giả lập đã chuyển khoản (demo)
              </Button>
            </div>
          )}
        </Panel>
      )}

      <Panel className="px-5 py-2">
        <BookingSummary b={b} />
      </Panel>

      <div className="mt-6 flex flex-wrap gap-3">
        <Link to={`/b/${b.businessSlug}`}><Button variant="secondary">Đặt thêm lịch</Button></Link>
        <Link to={`/b/${b.businessSlug}/tra-cuu?code=${b.code}`}><Button variant="ghost">Quản lý / hủy lịch</Button></Link>
      </div>
    </div>
  )
}

function Countdown({ until }: { until: string }) {
  const [, tick] = useState(0)
  useEffect(() => {
    const id = setInterval(() => tick((x) => x + 1), 1000)
    return () => clearInterval(id)
  }, [])
  const now = vnNow()
  const end = new Date(until.slice(0, 19) + 'Z').getTime()
  const cur = new Date(now.iso + 'Z').getTime() + new Date().getSeconds() * 1000
  const left = Math.max(0, Math.floor((end - cur) / 1000))
  const mm = String(Math.floor(left / 60)).padStart(2, '0')
  const ss = String(left % 60).padStart(2, '0')
  return (
    <p className="flex items-center gap-1.5 text-sm text-ink-2">
      <Clock className="size-4" aria-hidden />
      {left > 0 ? <>Giữ chỗ còn <span className="tabular font-medium text-ink">{mm}:{ss}</span></> : 'Đã hết thời gian giữ chỗ'}
    </p>
  )
}
