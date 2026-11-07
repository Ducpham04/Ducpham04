import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Phone, Mail, Banknote } from 'lucide-react'
import { api, errorMessage } from '../../lib/api'
import { useAuth } from '../../lib/auth'
import { dateTimeLabel, formatVnd, hhmm } from '../../lib/format'
import { ACTION_LABEL, NEXT_STATUS } from '../../lib/status'
import type { Booking, BookingStatus } from '../../lib/types'
import { Button, ConfirmDialog, Input, Sheet, Skeleton, StatusBadge, Textarea, useToast } from '../../components/ui'

export function useInvalidateBookings() {
  const qc = useQueryClient()
  return () => {
    qc.invalidateQueries({ queryKey: ['bookings'] })
    qc.invalidateQueries({ queryKey: ['today'] })
    qc.invalidateQueries({ queryKey: ['booking'] })
  }
}

export function BookingDetailSheet({ bookingId, onClose }: { bookingId: number | null; onClose: () => void }) {
  const { user } = useAuth()
  const toast = useToast()
  const invalidate = useInvalidateBookings()
  const [confirmCancel, setConfirmCancel] = useState(false)
  const [reason, setReason] = useState('')
  const [note, setNote] = useState<string | null>(null)

  const q = useQuery({
    queryKey: ['booking', bookingId],
    enabled: bookingId !== null,
    queryFn: async () => (await api.get<Booking>(`/bookings/${bookingId}`)).data,
  })
  const status = useMutation({
    mutationFn: async (v: { status: BookingStatus; reason?: string }) => (await api.post<Booking>(`/bookings/${bookingId}/status`, v)).data,
    onSuccess: (b) => {
      invalidate()
      setConfirmCancel(false)
      toast(`Đã cập nhật: ${ACTION_LABEL[b.status] ?? b.status}`)
    },
    onError: (e) => toast(errorMessage(e), 'error'),
  })
  const paid = useMutation({
    mutationFn: async () => (await api.post<Booking>(`/bookings/${bookingId}/mark-paid`)).data,
    onSuccess: () => {
      invalidate()
      toast('Đã ghi nhận thanh toán')
    },
    onError: (e) => toast(errorMessage(e), 'error'),
  })
  const saveNote = useMutation({
    mutationFn: async () => (await api.patch<Booking>(`/bookings/${bookingId}/note`, { internalNote: note })).data,
    onSuccess: () => {
      invalidate()
      setNote(null)
      toast('Đã lưu ghi chú')
    },
    onError: (e) => toast(errorMessage(e), 'error'),
  })

  const b = q.data
  const isOwner = user?.role === 'OWNER'
  const actions = b
    ? NEXT_STATUS[b.status].filter((s) => s !== 'CANCELLED' && (isOwner || ['CHECKED_IN', 'COMPLETED', 'NO_SHOW'].includes(s)))
    : []
  const canCancel = b && isOwner && NEXT_STATUS[b.status].includes('CANCELLED')

  return (
    <Sheet
      open={bookingId !== null}
      onClose={onClose}
      title={b ? `Lịch hẹn ${b.code}` : 'Lịch hẹn'}
      footer={
        b && (actions.length > 0 || canCancel) ? (
          <div className="flex flex-wrap gap-2">
            {actions.map((s, i) => (
              <Button key={s} variant={i === 0 ? 'primary' : 'secondary'} loading={status.isPending && status.variables?.status === s} onClick={() => status.mutate({ status: s })}>
                {ACTION_LABEL[s]}
              </Button>
            ))}
            {canCancel && <Button variant="ghost" className="ml-auto text-danger" onClick={() => setConfirmCancel(true)}>Hủy lịch</Button>}
          </div>
        ) : undefined
      }
    >
      {!b ? (
        <div className="space-y-3"><Skeleton className="h-6 w-1/2" /><Skeleton className="h-32" /></div>
      ) : (
        <div className="flex flex-col gap-6">
          <div>
            <div className="mb-2 flex items-center gap-2"><StatusBadge status={b.status} /><span className="text-sm text-ink-3">{b.source === 'ONLINE' ? 'Đặt trực tuyến' : 'Tạo tại quầy'}</span></div>
            <p className="text-xl font-semibold">{b.service.name}</p>
            <p className="tabular text-ink-2">{dateTimeLabel(b.startAt)} – {hhmm(b.endAt)}</p>
            <p className="text-ink-2">{b.staff.name} · {b.branch.name}</p>
          </div>

          <section>
            <h3 className="mb-2 text-sm font-medium text-ink-2">Khách hàng</h3>
            <p className="font-medium">{b.customer.fullName}</p>
            <div className="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-[0.9375rem]">
              <a href={`tel:${b.customer.phone}`} className="inline-flex items-center gap-1.5 text-brand hover:underline"><Phone className="size-4" aria-hidden />{b.customer.phone}</a>
              {b.customer.email && <a href={`mailto:${b.customer.email}`} className="inline-flex items-center gap-1.5 text-ink-2 hover:text-ink"><Mail className="size-4" aria-hidden />{b.customer.email}</a>}
            </div>
            {b.customerNote && <p className="mt-2 rounded-[10px] bg-sunken px-3 py-2 text-[0.9375rem]">“{b.customerNote}”</p>}
          </section>

          <section>
            <h3 className="mb-2 text-sm font-medium text-ink-2">Thanh toán</h3>
            <dl className="grid grid-cols-2 gap-y-1.5 text-[0.9375rem]">
              <dt className="text-ink-2">Giá dịch vụ</dt><dd className="tabular text-right">{formatVnd(b.price)}</dd>
              {b.amountDue > 0 && <><dt className="text-ink-2">Cần thu trước</dt><dd className="tabular text-right">{formatVnd(b.amountDue)}</dd></>}
              {b.amountDue > 0 && <><dt className="text-ink-2">Đã nhận</dt><dd className="tabular text-right">{formatVnd(b.amountPaid)}</dd></>}
            </dl>
            {b.paymentStatus === 'PENDING' && isOwner && (
              <Button variant="secondary" size="sm" className="mt-3" icon={<Banknote className="size-4" />} loading={paid.isPending} onClick={() => paid.mutate()}>
                Đã nhận tiền chuyển khoản
              </Button>
            )}
          </section>

          <section>
            <Textarea
              label="Ghi chú nội bộ"
              placeholder="Chỉ nhân viên cơ sở nhìn thấy"
              value={note ?? b.internalNote ?? ''}
              onChange={(e) => setNote(e.target.value)}
            />
            {note !== null && note !== (b.internalNote ?? '') && (
              <Button size="sm" className="mt-2" loading={saveNote.isPending} onClick={() => saveNote.mutate()}>Lưu ghi chú</Button>
            )}
          </section>
          {b.cancelReason && <p className="text-sm text-ink-2">Lý do hủy: {b.cancelReason}</p>}
        </div>
      )}
      <ConfirmDialog
        open={confirmCancel}
        title="Hủy lịch hẹn?"
        message="Khung giờ sẽ được mở lại. Khách nhận được thông báo hủy."
        confirmLabel="Hủy lịch"
        danger
        loading={status.isPending}
        onConfirm={() => status.mutate({ status: 'CANCELLED', reason: reason || undefined })}
        onClose={() => setConfirmCancel(false)}
      >
        <Input wrapClassName="mt-4" label="Lý do (không bắt buộc)" value={reason} onChange={(e) => setReason(e.target.value)} />
      </ConfirmDialog>
    </Sheet>
  )
}
