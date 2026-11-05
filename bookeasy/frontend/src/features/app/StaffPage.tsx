import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Plus, Trash2, X } from 'lucide-react'
import { api, errorMessage } from '../../lib/api'
import { DAY_NAMES, ddmmyyyy } from '../../lib/format'
import type { Shift, Staff, TimeOff } from '../../lib/types'
import { Avatar, Button, Checkbox, EmptyState, ErrorState, Input, PageHeader, Panel, Select, Sheet, Skeleton, Tabs, useToast } from '../../components/ui'
import { useBranches, useServices, useStaffList } from './queries'

const schema = z.object({
  fullName: z.string().trim().min(2, 'Nhập họ tên').max(100),
  title: z.string().max(100),
  phone: z.string().max(20),
  email: z.union([z.literal(''), z.string().email('Email không hợp lệ')]),
  color: z.string().regex(/^#[0-9a-fA-F]{6}$/),
  branchId: z.coerce.number<string | number>().min(1, 'Chọn chi nhánh'),
  active: z.boolean(),
  serviceIds: z.array(z.number()),
})
type FormIn = z.input<typeof schema>
type FormOut = z.output<typeof schema>

const PALETTE = ['#2f7d6b', '#b4573a', '#5a6fb0', '#8a5bb0', '#b08a2f', '#1f7a8c', '#a3476b', '#4f7a2f']

export default function StaffPage() {
  const staff = useStaffList()
  const branches = useBranches()
  const [editing, setEditing] = useState<Staff | 'new' | null>(null)
  return (
    <div>
      <PageHeader title="Nhân viên" description="Quản lý thông tin, ca làm việc, ngày nghỉ và tài khoản đăng nhập của nhân viên." actions={<Button icon={<Plus className="size-4" />} onClick={() => setEditing('new')}>Thêm nhân viên</Button>} />
      <Panel>
        {staff.isLoading ? <Skeleton className="m-4 h-40" /> : staff.isError ? <ErrorState message={errorMessage(staff.error)} onRetry={() => staff.refetch()} /> : staff.data?.length === 0 ? (
          <EmptyState title="Chưa có nhân viên" action={<Button onClick={() => setEditing('new')}>Thêm nhân viên</Button>} />
        ) : (
          <ul className="divide-y divide-line">
            {staff.data?.map((s) => (
              <li key={s.id}>
                <button onClick={() => setEditing(s)} className="flex w-full items-center gap-3 px-5 py-3 text-left hover:bg-sunken">
                  <Avatar name={s.fullName} color={s.color} />
                  <span className="min-w-0 flex-1">
                    <span className="block truncate font-medium">{s.fullName} {!s.active && <span className="ml-1 rounded-full bg-sunken px-2 py-0.5 text-[0.75rem] font-normal text-ink-2">Ngừng hoạt động</span>}</span>
                    <span className="block truncate text-sm text-ink-2">{[s.title, branches.data?.find((b) => b.id === s.branchId)?.name].filter(Boolean).join(' · ')}</span>
                  </span>
                  <span className="hidden text-sm text-ink-2 sm:block">{s.serviceIds.length} dịch vụ{s.hasAccount && ' · Có tài khoản'}</span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </Panel>
      <StaffSheet staff={editing} onClose={() => setEditing(null)} onCreated={(s) => setEditing(s)} />
    </div>
  )
}

function StaffSheet({ staff, onClose, onCreated }: { staff: Staff | 'new' | null; onClose: () => void; onCreated: (s: Staff) => void }) {
  const [tab, setTab] = useState<'info' | 'shifts' | 'off' | 'account'>('info')
  useEffect(() => setTab('info'), [staff === null || staff === 'new' ? staff : staff.id])
  const isNew = staff === 'new'
  return (
    <Sheet open={staff !== null} onClose={onClose} wide title={isNew ? 'Thêm nhân viên' : (staff as Staff | null)?.fullName ?? ''}>
      {!isNew && staff && (
        <Tabs value={tab} onChange={setTab} tabs={[{ value: 'info', label: 'Thông tin' }, { value: 'shifts', label: 'Ca làm việc' }, { value: 'off', label: 'Ngày nghỉ' }, { value: 'account', label: 'Tài khoản' }]} />
      )}
      {(isNew || tab === 'info') && <InfoForm staff={isNew ? null : (staff as Staff)} onSaved={(s) => (isNew ? onCreated(s) : undefined)} />}
      {!isNew && staff && tab === 'shifts' && <ShiftsEditor staffId={staff.id} />}
      {!isNew && staff && tab === 'off' && <TimeOffEditor staffId={staff.id} />}
      {!isNew && staff && tab === 'account' && <AccountForm staff={staff} />}
    </Sheet>
  )
}

function InfoForm({ staff, onSaved }: { staff: Staff | null; onSaved: (s: Staff) => void }) {
  const toast = useToast()
  const qc = useQueryClient()
  const branches = useBranches()
  const services = useServices()
  const form = useForm<FormIn, unknown, FormOut>({ resolver: zodResolver(schema) })
  useEffect(() => {
    form.reset({
      fullName: staff?.fullName ?? '',
      title: staff?.title ?? '',
      phone: staff?.phone ?? '',
      email: staff?.email ?? '',
      color: staff?.color ?? PALETTE[Math.floor(Math.random() * PALETTE.length)],
      branchId: staff?.branchId ?? branches.data?.[0]?.id ?? '',
      active: staff?.active ?? true,
      serviceIds: staff?.serviceIds ?? [],
    })
  }, [staff, branches.data, form])
  const save = useMutation({
    mutationFn: async (v: FormOut) => (staff ? await api.put<Staff>(`/staff/${staff.id}`, v) : await api.post<Staff>('/staff', v)).data,
    onSuccess: (s) => {
      qc.invalidateQueries({ queryKey: ['staff'] })
      qc.invalidateQueries({ queryKey: ['services'] })
      toast(staff ? 'Đã lưu' : 'Đã thêm nhân viên. Hãy thiết lập ca làm việc.')
      onSaved(s)
    },
    onError: (e) => toast(errorMessage(e), 'error'),
  })
  const color = form.watch('color')
  const serviceIds = form.watch('serviceIds') ?? []
  const { errors } = form.formState
  return (
    <form noValidate onSubmit={form.handleSubmit((v) => save.mutate(v))} className="flex flex-col gap-4">
      <Input label="Họ tên" error={errors.fullName?.message} {...form.register('fullName')} />
      <Input label="Chức danh" placeholder="Kỹ thuật viên, PT, Bác sĩ…" {...form.register('title')} />
      <div className="grid grid-cols-2 gap-3">
        <Input label="Số điện thoại" type="tel" {...form.register('phone')} />
        <Input label="Email" type="email" error={errors.email?.message} {...form.register('email')} />
      </div>
      <Select label="Chi nhánh" error={errors.branchId?.message} {...form.register('branchId')}>
        {branches.data?.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
      </Select>
      <fieldset>
        <legend className="mb-2 text-sm font-medium">Màu trên lịch</legend>
        <div className="flex flex-wrap gap-2">
          {PALETTE.map((c) => (
            <button type="button" key={c} onClick={() => form.setValue('color', c)} aria-label={`Màu ${c}`} aria-pressed={color === c}
              className="size-9 rounded-full ring-offset-2 aria-pressed:ring-2 aria-pressed:ring-ink" style={{ background: c }} />
          ))}
        </div>
      </fieldset>
      <fieldset>
        <legend className="mb-1 text-sm font-medium">Dịch vụ thực hiện được</legend>
        {services.data?.filter((s) => s.active).map((s) => (
          <Checkbox key={s.id} className="flex" label={s.name} checked={serviceIds.includes(s.id)}
            onChange={(e) => form.setValue('serviceIds', e.target.checked ? [...serviceIds, s.id] : serviceIds.filter((x) => x !== s.id))} />
        ))}
      </fieldset>
      {staff && <Checkbox label="Đang làm việc" {...form.register('active')} />}
      <Button type="submit" loading={save.isPending} className="self-start">{staff ? 'Lưu thay đổi' : 'Thêm nhân viên'}</Button>
    </form>
  )
}

function ShiftsEditor({ staffId }: { staffId: number }) {
  const toast = useToast()
  const q = useQuery({ queryKey: ['shifts', staffId], queryFn: async () => (await api.get<Shift[]>(`/staff/${staffId}/shifts`)).data })
  const [shifts, setShifts] = useState<Shift[] | null>(null)
  useEffect(() => { if (q.data) setShifts(q.data.map((s) => ({ ...s, startTime: s.startTime.slice(0, 5), endTime: s.endTime.slice(0, 5) }))) }, [q.data])
  const save = useMutation({
    mutationFn: () => api.put(`/staff/${staffId}/shifts`, { shifts }),
    onSuccess: () => { q.refetch(); toast('Đã lưu ca làm việc') },
    onError: (e) => toast(errorMessage(e), 'error'),
  })
  if (!shifts) return <Skeleton className="h-64" />
  const update = (i: number, patch: Partial<Shift>) => setShifts(shifts.map((s, j) => (j === i ? { ...s, ...patch } : s)))
  return (
    <div>
      <div className="flex flex-col divide-y divide-line">
        {DAY_NAMES.map((name, idx) => {
          const dow = idx + 1
          const items = shifts.map((s, i) => ({ s, i })).filter((x) => x.s.dayOfWeek === dow)
          return (
            <div key={dow} className="flex flex-wrap items-start gap-3 py-3">
              <span className="w-20 shrink-0 pt-2 text-[0.9375rem] font-medium">{name}</span>
              <div className="flex flex-1 flex-col gap-2">
                {items.length === 0 && <span className="pt-2 text-ink-3">Nghỉ</span>}
                {items.map(({ s, i }) => (
                  <div key={i} className="flex items-center gap-2">
                    <input type="time" step={900} value={s.startTime} onChange={(e) => update(i, { startTime: e.target.value })} aria-label={`${name} bắt đầu`} className="h-10 rounded-[var(--radius-control)] border border-line-strong px-2" />
                    <span className="text-ink-3">–</span>
                    <input type="time" step={900} value={s.endTime} onChange={(e) => update(i, { endTime: e.target.value })} aria-label={`${name} kết thúc`} className="h-10 rounded-[var(--radius-control)] border border-line-strong px-2" />
                    <button onClick={() => setShifts(shifts.filter((_, j) => j !== i))} aria-label="Xóa ca" className="inline-flex size-10 items-center justify-center rounded-full text-ink-2 hover:text-danger"><X className="size-4" /></button>
                  </div>
                ))}
              </div>
              <Button size="sm" variant="ghost" icon={<Plus className="size-4" />} onClick={() => setShifts([...shifts, { dayOfWeek: dow, startTime: items.length ? '14:00' : '09:00', endTime: items.length ? '20:00' : '12:00' }])}>Thêm ca</Button>
            </div>
          )
        })}
      </div>
      <div className="mt-4 flex gap-2">
        <Button loading={save.isPending} onClick={() => save.mutate()}>Lưu ca làm việc</Button>
        <Button variant="secondary" onClick={() => setShifts([1, 2, 3, 4, 5, 6].flatMap((d) => [{ dayOfWeek: d, startTime: '09:00', endTime: '12:00' }, { dayOfWeek: d, startTime: '13:30', endTime: '20:00' }]))}>Mẫu: T2–T7, 2 ca</Button>
      </div>
    </div>
  )
}

function TimeOffEditor({ staffId }: { staffId: number }) {
  const toast = useToast()
  const q = useQuery({ queryKey: ['timeoff', staffId], queryFn: async () => (await api.get<TimeOff[]>(`/staff/${staffId}/time-off`)).data })
  const [v, setV] = useState({ startDate: '', endDate: '', reason: '' })
  const add = useMutation({
    mutationFn: () => api.post(`/staff/${staffId}/time-off`, { ...v, endDate: v.endDate || v.startDate }),
    onSuccess: () => { q.refetch(); setV({ startDate: '', endDate: '', reason: '' }); toast('Đã thêm ngày nghỉ') },
    onError: (e) => toast(errorMessage(e), 'error'),
  })
  const del = useMutation({ mutationFn: (id: number) => api.delete(`/staff/${staffId}/time-off/${id}`), onSuccess: () => q.refetch() })
  return (
    <div>
      <form onSubmit={(e) => { e.preventDefault(); if (v.startDate) add.mutate() }} className="mb-5 grid grid-cols-2 gap-3">
        <Input label="Từ ngày" type="date" value={v.startDate} onChange={(e) => setV({ ...v, startDate: e.target.value })} />
        <Input label="Đến ngày" type="date" value={v.endDate} onChange={(e) => setV({ ...v, endDate: e.target.value })} />
        <Input wrapClassName="col-span-2" label="Lý do" value={v.reason} onChange={(e) => setV({ ...v, reason: e.target.value })} placeholder="Nghỉ phép, ốm, đào tạo…" />
        <Button type="submit" loading={add.isPending} className="col-span-2 justify-self-start" disabled={!v.startDate}>Thêm ngày nghỉ</Button>
      </form>
      {q.data?.length === 0 && <p className="text-ink-2">Chưa có ngày nghỉ nào.</p>}
      <ul className="divide-y divide-line">
        {q.data?.map((t) => (
          <li key={t.id} className="flex items-center justify-between py-2.5">
            <span><span className="tabular">{ddmmyyyy(t.startDate)}{t.endDate !== t.startDate && ` – ${ddmmyyyy(t.endDate)}`}</span>{t.reason && <span className="text-ink-2"> · {t.reason}</span>}</span>
            <button onClick={() => del.mutate(t.id)} aria-label="Xóa ngày nghỉ" className="inline-flex size-10 items-center justify-center rounded-full text-ink-2 hover:text-danger"><Trash2 className="size-4" /></button>
          </li>
        ))}
      </ul>
    </div>
  )
}

function AccountForm({ staff }: { staff: Staff }) {
  const toast = useToast()
  const qc = useQueryClient()
  const [v, setV] = useState({ email: staff.email ?? '', password: '' })
  const create = useMutation({
    mutationFn: () => api.post(`/staff/${staff.id}/account`, v),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['staff'] }); toast('Đã tạo tài khoản') },
    onError: (e) => toast(errorMessage(e), 'error'),
  })
  if (staff.hasAccount) return <p className="text-ink-2">Nhân viên đăng nhập bằng <strong className="text-ink">{staff.accountEmail}</strong>. Nhân viên chỉ xem lịch của mình và cập nhật check-in/hoàn thành.</p>
  return (
    <form onSubmit={(e) => { e.preventDefault(); create.mutate() }} className="flex flex-col gap-4">
      <p className="text-ink-2">Tạo tài khoản để nhân viên xem lịch của mình trên điện thoại.</p>
      <Input label="Email đăng nhập" type="email" value={v.email} onChange={(e) => setV({ ...v, email: e.target.value })} />
      <Input label="Mật khẩu tạm" type="text" hint="Tối thiểu 8 ký tự, gồm chữ và số" value={v.password} onChange={(e) => setV({ ...v, password: e.target.value })} />
      <Button type="submit" loading={create.isPending} className="self-start">Tạo tài khoản</Button>
    </form>
  )
}
