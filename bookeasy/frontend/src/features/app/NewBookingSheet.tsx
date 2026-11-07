import { useEffect, useMemo } from 'react'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { api, errorMessage } from '../../lib/api'
import { useAuth } from '../../lib/auth'
import { formatVnd, normalizePhone, vnNow } from '../../lib/format'
import type { Booking } from '../../lib/types'
import { Button, Input, Select, Sheet, Textarea, useToast } from '../../components/ui'
import { useServices, useStaffList } from './queries'
import { useInvalidateBookings } from './BookingDetailSheet'

const schema = z.object({
  serviceId: z.coerce.number<string>().min(1, 'Chọn dịch vụ'),
  staffId: z.coerce.number<string>().min(1, 'Chọn nhân viên'),
  date: z.string().min(1, 'Chọn ngày'),
  time: z.string().regex(/^\d{2}:(00|15|30|45)$/, 'Giờ theo bước 15 phút'),
  fullName: z.string().trim().min(2, 'Nhập họ tên'),
  phone: z.string().refine((v) => normalizePhone(v) !== null, 'Số điện thoại không hợp lệ'),
  email: z.union([z.literal(''), z.string().email('Email không hợp lệ')]),
  note: z.string().max(500),
})
type FormIn = z.input<typeof schema>
type FormOut = z.output<typeof schema>

export function NewBookingSheet({ open, onClose, preset }: { open: boolean; onClose: () => void; preset?: { date?: string; time?: string; staffId?: number } }) {
  const { user } = useAuth()
  const toast = useToast()
  const invalidate = useInvalidateBookings()
  const services = useServices()
  const staff = useStaffList()
  const form = useForm<FormIn, unknown, FormOut>({
    resolver: zodResolver(schema),
    defaultValues: { serviceId: '', staffId: '', date: vnNow().date, time: '09:00', fullName: '', phone: '', email: '', note: '' },
  })

  useEffect(() => {
    if (!open) return
    form.reset({
      serviceId: '',
      staffId: preset?.staffId ? String(preset.staffId) : user?.role === 'STAFF' && user.staffId ? String(user.staffId) : '',
      date: preset?.date ?? vnNow().date,
      time: preset?.time ?? '09:00',
      fullName: '',
      phone: '',
      email: '',
      note: '',
    })
  }, [open, preset, form, user])

  const serviceId = Number(form.watch('serviceId'))
  const eligible = useMemo(() => {
    const svc = services.data?.find((s) => s.id === serviceId)
    return (staff.data ?? []).filter((s) => s.active && (!svc || svc.staffIds.includes(s.id)) && (user?.role !== 'STAFF' || s.id === user.staffId))
  }, [services.data, staff.data, serviceId, user])

  const create = useMutation({
    mutationFn: async (v: FormOut) =>
      (await api.post<Booking>('/bookings', {
        serviceId: v.serviceId,
        staffId: v.staffId,
        startAt: `${v.date}T${v.time}:00`,
        fullName: v.fullName,
        phone: v.phone,
        email: v.email || null,
        note: v.note || null,
        source: 'ADMIN',
      })).data,
    onSuccess: (b) => {
      invalidate()
      toast(`Đã tạo lịch hẹn ${b.code}`)
      onClose()
    },
    onError: (e) => toast(errorMessage(e), 'error'),
  })
  const { errors } = form.formState
  const times = useMemo(() => Array.from({ length: (23 - 6) * 4 }, (_, i) => {
    const m = 6 * 60 + i * 15
    return `${String(Math.floor(m / 60)).padStart(2, '0')}:${String(m % 60).padStart(2, '0')}`
  }), [])

  return (
    <Sheet
      open={open}
      onClose={onClose}
      title="Tạo lịch hẹn"
      footer={<Button type="submit" form="new-booking" loading={create.isPending} className="w-full">Tạo lịch hẹn</Button>}
    >
      <form id="new-booking" noValidate onSubmit={form.handleSubmit((v) => create.mutate(v))} className="flex flex-col gap-4">
        <Select label="Dịch vụ" error={errors.serviceId?.message} {...form.register('serviceId')}>
          <option value="">Chọn dịch vụ</option>
          {services.data?.filter((s) => s.active).map((s) => <option key={s.id} value={s.id}>{s.name} · {s.durationMinutes}′ · {formatVnd(s.price)}</option>)}
        </Select>
        <Select label="Nhân viên" error={errors.staffId?.message} {...form.register('staffId')}>
          <option value="">Chọn nhân viên</option>
          {eligible.map((s) => <option key={s.id} value={s.id}>{s.fullName}</option>)}
        </Select>
        <div className="grid grid-cols-2 gap-3">
          <Input label="Ngày" type="date" error={errors.date?.message} {...form.register('date')} />
          <Select label="Giờ" error={errors.time?.message} {...form.register('time')}>
            {times.map((t) => <option key={t} value={t}>{t}</option>)}
          </Select>
        </div>
        <Input label="Họ tên khách" error={errors.fullName?.message} {...form.register('fullName')} />
        <Input label="Số điện thoại" type="tel" inputMode="tel" error={errors.phone?.message} {...form.register('phone')} />
        <Input label="Email (không bắt buộc)" type="email" error={errors.email?.message} {...form.register('email')} />
        <Textarea label="Ghi chú" {...form.register('note')} />
        <p className="text-sm text-ink-3">Lịch tạo tại quầy được xác nhận ngay, không yêu cầu đặt cọc. Hệ thống vẫn chặn trùng giờ với lịch khác của nhân viên.</p>
      </form>
    </Sheet>
  )
}
