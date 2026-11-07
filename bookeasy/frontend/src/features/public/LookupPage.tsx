import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { api, errorMessage } from '../../lib/api'
import { normalizePhone } from '../../lib/format'
import type { PublicBooking } from '../../lib/types'
import { Button, ConfirmDialog, Input, Panel, StatusBadge, useToast } from '../../components/ui'
import { PublicShell } from './PublicShell'
import { usePublicBusiness } from './BookingPage'
import { BookingSummary } from './BookingSummary'

const schema = z.object({
  code: z.string().trim().regex(/^BE[A-Za-z0-9]{6}$/i, 'Mã lịch hẹn gồm 8 ký tự, bắt đầu bằng BE'),
  phone: z.string().refine((v) => normalizePhone(v) !== null, 'Số điện thoại không hợp lệ'),
})
type Form = z.infer<typeof schema>

export function LookupForm({ initialCode = '', onFound }: { initialCode?: string; onFound: (phone: string, b: PublicBooking) => void }) {
  const form = useForm<Form>({ resolver: zodResolver(schema), defaultValues: { code: initialCode, phone: '' } })
  const lookup = useMutation({
    mutationFn: async (v: Form) => {
      const phone = normalizePhone(v.phone)!
      const res = await api.get<PublicBooking>(`/public/bookings/${v.code.toUpperCase()}`, { params: { phone } })
      return { phone, booking: res.data }
    },
    onSuccess: ({ phone, booking }) => onFound(phone, booking),
  })
  return (
    <section>
      <h1 className="text-2xl font-semibold">Tra cứu lịch hẹn</h1>
      <p className="mt-1 mb-5 text-ink-2">Nhập mã lịch hẹn và số điện thoại đã dùng khi đặt.</p>
      <form noValidate onSubmit={form.handleSubmit((v) => lookup.mutate(v))} className="flex flex-col gap-4">
        <Input label="Mã lịch hẹn" placeholder="BE7K2M9Q" className="font-mono uppercase" error={form.formState.errors.code?.message} {...form.register('code')} />
        <Input label="Số điện thoại" type="tel" inputMode="tel" placeholder="0912 345 678" error={form.formState.errors.phone?.message} {...form.register('phone')} />
        {lookup.isError && <p role="alert" className="text-danger">{errorMessage(lookup.error)}</p>}
        <Button type="submit" loading={lookup.isPending} className="self-start">Tra cứu</Button>
      </form>
    </section>
  )
}

export default function LookupPage() {
  const { slug = '' } = useParams()
  const [params] = useSearchParams()
  const biz = usePublicBusiness(slug)
  const toast = useToast()
  const qc = useQueryClient()
  const [found, setFound] = useState<{ phone: string; booking: PublicBooking } | null>(null)
  const [confirm, setConfirm] = useState(false)

  const cancel = useMutation({
    mutationFn: async () => (await api.post<PublicBooking>(`/public/bookings/${found!.booking.code}/cancel`, { phone: found!.phone })).data,
    onSuccess: (b) => {
      setFound((f) => (f ? { ...f, booking: b } : f))
      setConfirm(false)
      qc.invalidateQueries({ queryKey: ['public', slug] })
      toast('Đã hủy lịch hẹn')
    },
    onError: (e) => toast(errorMessage(e), 'error'),
  })

  return (
    <PublicShell business={biz.data}>
      {!found ? (
        <LookupForm initialCode={params.get('code') ?? ''} onFound={(phone, booking) => setFound({ phone, booking })} />
      ) : (
        <div className="anim-rise">
          <div className="mb-4 flex items-center justify-between gap-3">
            <div>
              <p className="text-sm text-ink-2">Mã lịch hẹn</p>
              <p className="font-mono text-2xl font-semibold tracking-wider">{found.booking.code}</p>
            </div>
            <StatusBadge status={found.booking.status} />
          </div>
          <Panel className="px-5 py-2">
            <BookingSummary b={found.booking} />
          </Panel>
          <div className="mt-5 flex flex-wrap gap-3">
            {found.booking.cancellable && (
              <Button variant="danger" onClick={() => setConfirm(true)}>Hủy lịch hẹn</Button>
            )}
            <Button variant="secondary" onClick={() => setFound(null)}>Tra cứu lịch khác</Button>
          </div>
          {!found.booking.cancellable && (found.booking.status === 'CONFIRMED' || found.booking.status === 'PENDING_PAYMENT') && (
            <p className="mt-3 text-sm text-ink-2">Đã quá thời hạn hủy trực tuyến. Vui lòng gọi trực tiếp cho cơ sở.</p>
          )}
        </div>
      )}
      <ConfirmDialog
        open={confirm}
        title="Hủy lịch hẹn này?"
        message="Khung giờ sẽ được mở lại cho khách khác. Tiền cọc (nếu có) được xử lý theo chính sách của cơ sở."
        confirmLabel="Hủy lịch"
        danger
        loading={cancel.isPending}
        onConfirm={() => cancel.mutate()}
        onClose={() => setConfirm(false)}
      />
    </PublicShell>
  )
}
