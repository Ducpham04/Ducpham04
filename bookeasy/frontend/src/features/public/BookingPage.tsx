import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { clsx } from 'clsx'
import { ArrowLeft, Check, Search, Users } from 'lucide-react'
import { api, errorBody, errorMessage } from '../../lib/api'
import { addDays, ddmmyyyy, durationLabel, formatVnd, hhmm, normalizePhone, vnNow, weekdayShort } from '../../lib/format'
import type { DayAvailability, PublicBooking, PublicBusiness, PublicStaff, SlotOption } from '../../lib/types'
import { Avatar, Button, ErrorState, Input, Skeleton, Textarea, useToast } from '../../components/ui'
import { PublicShell } from './PublicShell'

type Step = 'service' | 'staff' | 'time' | 'info'
const STEPS: { key: Step; label: string }[] = [
  { key: 'service', label: 'Dịch vụ' },
  { key: 'staff', label: 'Nhân viên' },
  { key: 'time', label: 'Thời gian' },
  { key: 'info', label: 'Thông tin' },
]

const infoSchema = z.object({
  fullName: z.string().trim().min(2, 'Vui lòng nhập họ tên').max(100, 'Họ tên quá dài'),
  phone: z.string().refine((v) => normalizePhone(v) !== null, 'Số điện thoại không hợp lệ (VD: 0912 345 678)'),
  email: z.union([z.literal(''), z.string().trim().email('Email không hợp lệ')]),
  note: z.string().max(500, 'Tối đa 500 ký tự'),
})
type InfoForm = z.infer<typeof infoSchema>

export function usePublicBusiness(slug: string) {
  return useQuery({
    queryKey: ['public', slug],
    queryFn: async () => (await api.get<PublicBusiness>(`/public/businesses/${slug}`)).data,
    retry: false,
  })
}

export default function BookingPage() {
  const { slug = '' } = useParams()
  const navigate = useNavigate()
  const toast = useToast()
  const qc = useQueryClient()
  const biz = usePublicBusiness(slug)

  const [step, setStep] = useState<Step>('service')
  const [branchId, setBranchId] = useState<number | null>(null)
  const [serviceId, setServiceId] = useState<number | null>(null)
  const [staffId, setStaffId] = useState<number | null>(null) // null = bất kỳ ai
  const [date, setDate] = useState<string>(() => vnNow().date)
  const [time, setTime] = useState<string | null>(null)

  const business = biz.data
  const branch = branchId ?? business?.branches[0]?.id ?? null
  const service = business?.services.find((s) => s.id === serviceId)

  const staff = useQuery({
    queryKey: ['public', slug, 'staff', serviceId, branch],
    enabled: !!serviceId && !!branch,
    queryFn: async () =>
      (await api.get<PublicStaff[]>(`/public/businesses/${slug}/staff`, { params: { serviceId, branchId: branch } })).data,
  })
  const days = useQuery({
    queryKey: ['public', slug, 'days', serviceId, branch, staffId],
    enabled: step === 'time' && !!serviceId && !!branch,
    queryFn: async () =>
      (await api.get<DayAvailability[]>(`/public/businesses/${slug}/availability/days`, {
        params: { serviceId, branchId: branch, staffId: staffId ?? undefined, days: business?.bookingWindowDays ?? 14 },
      })).data,
  })
  const slots = useQuery({
    queryKey: ['public', slug, 'slots', serviceId, branch, staffId, date],
    enabled: step === 'time' && !!serviceId && !!branch,
    queryFn: async () =>
      (await api.get<SlotOption[]>(`/public/businesses/${slug}/availability`, {
        params: { serviceId, branchId: branch, staffId: staffId ?? undefined, date },
      })).data,
  })

  const form = useForm<InfoForm>({ resolver: zodResolver(infoSchema), defaultValues: { fullName: '', phone: '', email: '', note: '' } })

  const book = useMutation({
    mutationFn: async (v: InfoForm) =>
      (await api.post<PublicBooking>(`/public/businesses/${slug}/bookings`, {
        branchId: branch,
        serviceId,
        staffId,
        date,
        time,
        fullName: v.fullName,
        phone: v.phone,
        email: v.email || null,
        note: v.note || null,
      })).data,
    onSuccess: (res, v) => {
      const phone = normalizePhone(v.phone)!
      try {
        sessionStorage.setItem(`bookeasy.phone.${res.code}`, phone)
      } catch {
        /* ignore */
      }
      navigate(`/b/${slug}/booking/${res.code}`, { state: { phone, fresh: true } })
    },
    onError: (e) => {
      const body = errorBody(e)
      if (body?.code === 'SLOT_TAKEN') {
        setTime(null)
        setStep('time')
        qc.invalidateQueries({ queryKey: ['public', slug] })
      }
      if (body?.fieldErrors) {
        Object.entries(body.fieldErrors).forEach(([k, m]) => {
          if (k in form.getValues()) form.setError(k as keyof InfoForm, { message: m })
        })
      }
      toast(errorMessage(e), 'error')
    },
  })

  const grouped = useMemo(() => {
    if (!business) return []
    const groups = business.categories
      .map((c) => ({ id: c.id, name: c.name, items: business.services.filter((s) => s.categoryId === c.id) }))
      .filter((g) => g.items.length)
    const other = business.services.filter((s) => !business.categories.some((c) => c.id === s.categoryId))
    if (other.length) groups.push({ id: 0, name: 'Dịch vụ khác', items: other })
    return groups
  }, [business])

  if (biz.isError) {
    const forbidden = errorBody(biz.error)?.status === 403
    return (
      <PublicShell>
        <ErrorState message={forbidden ? 'Cơ sở đang tạm ngưng nhận lịch trực tuyến.' : 'Không tìm thấy trang đặt lịch này.'} />
      </PublicShell>
    )
  }

  const stepIndex = STEPS.findIndex((s) => s.key === step)
  const chosenStaff = staff.data?.find((s) => s.id === staffId)
  const canNext = (step === 'service' && !!serviceId) || (step === 'staff') || (step === 'time' && !!time)

  function next() {
    if (step === 'service') setStep('staff')
    else if (step === 'staff') setStep('time')
    else if (step === 'time') setStep('info')
  }
  function back() {
    setStep(STEPS[Math.max(0, stepIndex - 1)].key)
  }

  return (
    <PublicShell business={business}>
      {business?.description && step === 'service' && <p className="mb-5 max-w-[65ch] text-ink-2">{business.description}</p>}

      <nav aria-label="Các bước đặt lịch" className="mb-6 flex items-center gap-4 overflow-x-auto border-b border-line text-[0.9375rem]">
        {STEPS.map((s, i) => (
          <button
            key={s.key}
            disabled={i > stepIndex}
            onClick={() => i < stepIndex && setStep(s.key)}
            aria-current={s.key === step ? 'step' : undefined}
            className={clsx(
              '-mb-px shrink-0 border-b-2 pb-2.5 font-medium transition-colors duration-150',
              s.key === step ? 'border-brand text-ink' : i < stepIndex ? 'border-transparent text-ink-2 hover:text-ink' : 'border-transparent text-ink-3',
            )}
          >
            {s.label}
          </button>
        ))}
        <Link to={`/b/${slug}/tra-cuu`} className="ml-auto inline-flex shrink-0 items-center gap-1 pb-2.5 text-sm text-ink-2 hover:text-ink">
          <Search className="size-4" aria-hidden /> Tra cứu lịch
        </Link>
      </nav>

      {!business ? (
        <div className="space-y-3">
          {[0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-16" />)}
        </div>
      ) : (
        <div key={step} className="anim-rise">
          {step === 'service' && (
            <section aria-labelledby="h-service">
              <h2 id="h-service" className="sr-only">Chọn dịch vụ</h2>
              {business.branches.length > 1 && (
                <div className="mb-6">
                  <p className="mb-2 text-sm font-medium">Chi nhánh</p>
                  <div className="grid gap-2 sm:grid-cols-2">
                    {business.branches.map((b) => (
                      <ChoiceRow key={b.id} selected={branch === b.id} onClick={() => { setBranchId(b.id); setStaffId(null); setTime(null) }}>
                        <span className="font-medium">{b.name}</span>
                        {b.address && <span className="block text-sm text-ink-2">{b.address}</span>}
                      </ChoiceRow>
                    ))}
                  </div>
                </div>
              )}
              {grouped.length === 0 && <p className="text-ink-2">Cơ sở chưa mở dịch vụ đặt lịch trực tuyến.</p>}
              {grouped.map((g) => (
                <div key={g.id} className="mb-7">
                  <h3 className="mb-2 text-lg font-semibold">{g.name}</h3>
                  <div className="flex flex-col gap-2">
                    {g.items.map((s) => (
                      <ChoiceRow key={s.id} selected={serviceId === s.id} onClick={() => { setServiceId(s.id); setStaffId(null); setTime(null) }}>
                        <span className="flex items-baseline justify-between gap-3">
                          <span className="font-medium">{s.name}</span>
                          <span className="tabular shrink-0 font-medium">{s.price > 0 ? formatVnd(s.price) : 'Miễn phí'}</span>
                        </span>
                        {s.description && <span className="mt-0.5 block text-sm text-ink-2">{s.description}</span>}
                        <span className="mt-1 block text-sm text-ink-3">{durationLabel(s.durationMinutes)}</span>
                      </ChoiceRow>
                    ))}
                  </div>
                </div>
              ))}
            </section>
          )}

          {step === 'staff' && (
            <section aria-labelledby="h-staff">
              <h2 id="h-staff" className="mb-3 text-lg font-semibold">Bạn muốn ai phục vụ?</h2>
              <div className="flex flex-col gap-2">
                <ChoiceRow selected={staffId === null} onClick={() => { setStaffId(null); setTime(null) }}>
                  <span className="flex items-center gap-3">
                    <span className="flex size-10 items-center justify-center rounded-full bg-sunken text-ink-2"><Users className="size-5" aria-hidden /></span>
                    <span>
                      <span className="block font-medium">Bất kỳ ai</span>
                      <span className="block text-sm text-ink-2">Hệ thống chọn người còn trống phù hợp nhất</span>
                    </span>
                  </span>
                </ChoiceRow>
                {staff.isLoading && <Skeleton className="h-16" />}
                {staff.data?.map((s) => (
                  <ChoiceRow key={s.id} selected={staffId === s.id} onClick={() => { setStaffId(s.id); setTime(null) }}>
                    <span className="flex items-center gap-3">
                      <Avatar name={s.fullName} color={s.color} size={40} />
                      <span>
                        <span className="block font-medium">{s.fullName}</span>
                        {s.title && <span className="block text-sm text-ink-2">{s.title}</span>}
                      </span>
                    </span>
                  </ChoiceRow>
                ))}
              </div>
            </section>
          )}

          {step === 'time' && (
            <section aria-labelledby="h-time">
              <h2 id="h-time" className="mb-3 text-lg font-semibold">Chọn ngày và giờ</h2>
              <div className="-mx-4 mb-5 flex gap-2 overflow-x-auto px-4 pb-1" role="listbox" aria-label="Chọn ngày">
                {(days.data ?? Array.from({ length: 14 }, (_, i) => ({ date: addDays(vnNow().date, i), available: true, slotCount: 0 }))).map((d) => (
                  <button
                    key={d.date}
                    role="option"
                    aria-selected={date === d.date}
                    disabled={days.isSuccess && !d.available}
                    onClick={() => { setDate(d.date); setTime(null) }}
                    className={clsx(
                      'flex w-[68px] shrink-0 flex-col items-center rounded-[12px] border py-2 transition-colors duration-150',
                      date === d.date ? 'border-brand bg-brand text-on-brand' : 'border-line-strong bg-surface hover:border-brand',
                      'disabled:cursor-not-allowed disabled:border-line disabled:bg-sunken disabled:text-ink-3',
                    )}
                  >
                    <span className="text-[0.8125rem]">{weekdayShort(d.date)}</span>
                    <span className="tabular text-lg font-semibold">{d.date.slice(8, 10)}</span>
                    <span className="text-[0.75rem] opacity-80">{days.isSuccess && !d.available ? 'Hết chỗ' : `thg ${Number(d.date.slice(5, 7))}`}</span>
                  </button>
                ))}
              </div>
              {slots.isLoading ? (
                <div className="grid grid-cols-4 gap-2">{Array.from({ length: 8 }, (_, i) => <Skeleton key={i} className="h-11" />)}</div>
              ) : slots.isError ? (
                <ErrorState message={errorMessage(slots.error)} onRetry={() => slots.refetch()} />
              ) : slots.data && slots.data.length === 0 ? (
                <p className="rounded-[var(--radius-panel)] bg-sunken px-4 py-5 text-ink-2">
                  Ngày {ddmmyyyy(date)} đã kín lịch hoặc cơ sở nghỉ. Vui lòng chọn ngày khác{staffId ? ' hoặc chọn “Bất kỳ ai”' : ''}.
                </p>
              ) : (
                <SlotGroups slots={slots.data ?? []} value={time} onChange={setTime} />
              )}
            </section>
          )}

          {step === 'info' && (
            <section aria-labelledby="h-info">
              <h2 id="h-info" className="mb-3 text-lg font-semibold">Thông tin của bạn</h2>
              <form id="info-form" noValidate onSubmit={form.handleSubmit((v) => book.mutate(v))} className="flex flex-col gap-4">
                <Input label="Họ và tên" autoComplete="name" placeholder="Nguyễn Văn A" error={form.formState.errors.fullName?.message} {...form.register('fullName')} />
                <Input label="Số điện thoại" type="tel" inputMode="tel" autoComplete="tel" placeholder="0912 345 678" hint="Dùng để tra cứu và nhận nhắc lịch" error={form.formState.errors.phone?.message} {...form.register('phone')} />
                <Input label="Email (không bắt buộc)" type="email" autoComplete="email" placeholder="ban@example.com" hint="Nhận email xác nhận và nhắc lịch trước 24 giờ" error={form.formState.errors.email?.message} {...form.register('email')} />
                <Textarea label="Ghi chú cho cơ sở (không bắt buộc)" placeholder="Ví dụ: da nhạy cảm, muốn phòng riêng…" error={form.formState.errors.note?.message} {...form.register('note')} />
                {business.paymentMode !== 'NONE' && service && service.price > 0 && (
                  <p className="rounded-[var(--radius-control)] bg-warn-soft px-4 py-3 text-[0.9375rem]">
                    Cơ sở yêu cầu {business.paymentMode === 'FULL' ? 'thanh toán trước' : `đặt cọc ${business.depositPercent}%`} qua chuyển khoản VietQR. Lịch hẹn được giữ 30 phút để bạn thanh toán.
                  </p>
                )}
              </form>
            </section>
          )}
        </div>
      )}

      {business && (
        <div className="fixed inset-x-0 bottom-0 z-30 border-t border-line bg-surface pb-[env(safe-area-inset-bottom)]">
          <div className="mx-auto flex max-w-2xl items-center gap-3 px-4 py-3">
            {stepIndex > 0 && (
              <button onClick={back} className="inline-flex size-11 shrink-0 items-center justify-center rounded-full border border-line-strong text-ink-2 hover:text-ink" aria-label="Quay lại">
                <ArrowLeft className="size-5" />
              </button>
            )}
            <div className="min-w-0 flex-1 text-sm">
              {service ? (
                <>
                  <p className="truncate font-medium text-ink">{service.name}</p>
                  <p className="truncate text-ink-2">
                    <span className="tabular">{formatVnd(service.price)}</span>
                    {step !== 'service' && ` · ${chosenStaff?.fullName ?? 'Bất kỳ ai'}`}
                    {time && ` · ${hhmm(time)} ${weekdayShort(date)} ${ddmmyyyy(date).slice(0, 5)}`}
                  </p>
                </>
              ) : (
                <p className="text-ink-2">Chọn một dịch vụ để bắt đầu</p>
              )}
            </div>
            {step === 'info' ? (
              <Button type="submit" form="info-form" loading={book.isPending} size="lg">
                Xác nhận đặt lịch
              </Button>
            ) : (
              <Button onClick={next} disabled={!canNext} size="lg">
                Tiếp tục
              </Button>
            )}
          </div>
        </div>
      )}
    </PublicShell>
  )
}

function ChoiceRow({ selected, onClick, children }: { selected: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      type="button"
      role="radio"
      aria-checked={selected}
      onClick={onClick}
      className={clsx(
        'relative w-full rounded-[12px] border px-4 py-3 pr-11 text-left transition-colors duration-150',
        selected ? 'border-brand bg-brand-soft' : 'border-line bg-surface hover:border-line-strong',
      )}
    >
      {children}
      <span
        aria-hidden
        className={clsx(
          'absolute right-3.5 top-3.5 flex size-5 items-center justify-center rounded-full border transition-colors duration-150',
          selected ? 'border-brand bg-brand text-on-brand' : 'border-line-strong',
        )}
      >
        {selected && <Check className="size-3.5" strokeWidth={3} />}
      </span>
    </button>
  )
}

function SlotGroups({ slots, value, onChange }: { slots: SlotOption[]; value: string | null; onChange: (t: string) => void }) {
  const groups = [
    { label: 'Buổi sáng', items: slots.filter((s) => s.time < '12:00') },
    { label: 'Buổi chiều', items: slots.filter((s) => s.time >= '12:00' && s.time < '17:00') },
    { label: 'Buổi tối', items: slots.filter((s) => s.time >= '17:00') },
  ].filter((g) => g.items.length)
  return (
    <div className="flex flex-col gap-5" role="radiogroup" aria-label="Chọn giờ">
      {groups.map((g) => (
        <div key={g.label}>
          <p className="mb-2 text-sm font-medium text-ink-2">{g.label}</p>
          <div className="grid grid-cols-4 gap-2 sm:grid-cols-6">
            {g.items.map((s) => (
              <button
                key={s.time}
                role="radio"
                aria-checked={value === s.time}
                onClick={() => onChange(s.time)}
                className={clsx(
                  'tabular h-11 rounded-[10px] border text-[0.9375rem] font-medium transition-colors duration-150',
                  value === s.time ? 'border-brand bg-brand text-on-brand' : 'border-line-strong bg-surface hover:border-brand',
                )}
              >
                {hhmm(s.time)}
              </button>
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}
