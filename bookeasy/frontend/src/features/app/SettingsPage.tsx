import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { ExternalLink, Plus, Trash2, Upload } from 'lucide-react'
import { api, errorMessage } from '../../lib/api'
import { DAY_NAMES, ddmmyyyy, onColor } from '../../lib/format'
import { CATEGORY_LABEL } from '../../lib/status'
import type { Branch, BranchHour, Business, Holiday } from '../../lib/types'
import { Button, Checkbox, Input, PageHeader, Panel, Select, Skeleton, Tabs, Textarea, useToast } from '../../components/ui'
import { useBranches, useBusiness } from './queries'

type Tab = 'info' | 'brand' | 'branches' | 'holidays' | 'payment'

// Một số ngân hàng phổ biến – mã BIN theo danh sách NAPAS/VietQR.
const BANKS = [
  ['970436', 'Vietcombank'], ['970415', 'VietinBank'], ['970418', 'BIDV'], ['970405', 'Agribank'], ['970407', 'Techcombank'],
  ['970422', 'MB Bank'], ['970416', 'ACB'], ['970432', 'VPBank'], ['970423', 'TPBank'], ['970403', 'Sacombank'], ['970443', 'SHB'], ['970441', 'VIB'],
]

const schema = z.object({
  name: z.string().trim().min(2, 'Nhập tên cơ sở').max(150),
  description: z.string().max(1000),
  phone: z.string().max(20),
  email: z.union([z.literal(''), z.string().email('Email không hợp lệ')]),
  address: z.string().max(255),
  logoUrl: z.union([z.literal(''), z.string().regex(/^(https?:\/\/|\/uploads\/).+/, 'URL phải bắt đầu bằng http(s)')]),
  brandColor: z.string().regex(/^#[0-9a-fA-F]{6}$/, 'Màu dạng #RRGGBB'),
  category: z.enum(['SPA', 'SALON', 'GYM', 'CLINIC', 'OTHER']),
  paymentMode: z.enum(['NONE', 'DEPOSIT', 'FULL']),
  depositPercent: z.coerce.number<string | number>().int().min(5).max(100),
  bankBin: z.string(),
  bankAccountNo: z.string().regex(/^\d*$/, 'Chỉ gồm chữ số'),
  bankAccountName: z.string().max(100),
  minLeadMinutes: z.coerce.number<string | number>().int().min(0).max(10080),
  bookingWindowDays: z.coerce.number<string | number>().int().min(1).max(90),
  cancelBeforeMinutes: z.coerce.number<string | number>().int().min(0).max(10080),
}).refine((v) => v.paymentMode === 'NONE' || (v.bankBin && v.bankAccountNo), { message: 'Nhập ngân hàng và số tài khoản', path: ['bankAccountNo'] })
type FormIn = z.input<typeof schema>
type FormOut = z.output<typeof schema>

export default function SettingsPage() {
  const [params] = useSearchParams()
  const [tab, setTab] = useState<Tab>('info')
  const biz = useBusiness()
  return (
    <div>
      <PageHeader
        title="Cài đặt"
        description={params.get('welcome') ? 'Chào mừng bạn! Hoàn thiện thông tin, thêm dịch vụ và nhân viên để bắt đầu nhận lịch.' : undefined}
        actions={biz.data && <a href={`/b/${biz.data.slug}`} target="_blank" rel="noreferrer"><Button variant="secondary" icon={<ExternalLink className="size-4" />}>Xem trang đặt lịch</Button></a>}
      />
      <Tabs value={tab} onChange={setTab} tabs={[
        { value: 'info', label: 'Thông tin' }, { value: 'brand', label: 'Thương hiệu' }, { value: 'branches', label: 'Chi nhánh & giờ mở cửa' },
        { value: 'holidays', label: 'Ngày nghỉ' }, { value: 'payment', label: 'Thanh toán & đặt lịch' },
      ]} />
      {!biz.data ? <Skeleton className="h-80" /> : (
        <>
          {(tab === 'info' || tab === 'brand' || tab === 'payment') && <BusinessForm business={biz.data} tab={tab} />}
          {tab === 'branches' && <BranchesEditor />}
          {tab === 'holidays' && <HolidaysEditor />}
        </>
      )}
    </div>
  )
}

function BusinessForm({ business, tab }: { business: Business; tab: Tab }) {
  const toast = useToast()
  const qc = useQueryClient()
  const fileRef = useRef<HTMLInputElement>(null)
  const form = useForm<FormIn, unknown, FormOut>({ resolver: zodResolver(schema) })
  useEffect(() => {
    form.reset({
      name: business.name, description: business.description ?? '', phone: business.phone ?? '', email: business.email ?? '',
      address: business.address ?? '', logoUrl: business.logoUrl ?? '', brandColor: business.brandColor, category: business.category,
      paymentMode: business.paymentMode, depositPercent: business.depositPercent, bankBin: business.bankBin ?? '',
      bankAccountNo: business.bankAccountNo ?? '', bankAccountName: business.bankAccountName ?? '', minLeadMinutes: business.minLeadMinutes,
      bookingWindowDays: business.bookingWindowDays, cancelBeforeMinutes: business.cancelBeforeMinutes,
    })
  }, [business, form])
  const save = useMutation({
    mutationFn: async (v: FormOut) => (await api.put<Business>('/business', v)).data,
    onSuccess: (b) => { qc.setQueryData(['business'], b); toast('Đã lưu cài đặt') },
    onError: (e) => toast(errorMessage(e), 'error'),
  })
  const upload = useMutation({
    mutationFn: async (file: File) => {
      const fd = new FormData()
      fd.append('file', file)
      return (await api.post<Business>('/business/logo', fd)).data
    },
    onSuccess: (b) => { qc.setQueryData(['business'], b); form.setValue('logoUrl', b.logoUrl ?? ''); toast('Đã cập nhật logo') },
    onError: (e) => toast(errorMessage(e), 'error'),
  })
  const { errors } = form.formState
  const color = form.watch('brandColor') ?? business.brandColor
  const logo = form.watch('logoUrl')
  const mode = form.watch('paymentMode')

  return (
    <form noValidate onSubmit={form.handleSubmit((v) => save.mutate(v))} className="max-w-2xl">
      <Panel className="flex flex-col gap-4 p-5">
        {tab === 'info' && (
          <>
            <Input label="Tên cơ sở" error={errors.name?.message} {...form.register('name')} />
            <Select label="Loại hình" {...form.register('category')}>
              {Object.entries(CATEGORY_LABEL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </Select>
            <Textarea label="Giới thiệu ngắn" hint="Hiển thị trên trang đặt lịch" {...form.register('description')} />
            <div className="grid gap-4 sm:grid-cols-2">
              <Input label="Số điện thoại" {...form.register('phone')} />
              <Input label="Email liên hệ" error={errors.email?.message} {...form.register('email')} />
            </div>
            <Input label="Địa chỉ" {...form.register('address')} />
            <p className="text-sm text-ink-2">Đường dẫn đặt lịch: <span className="font-mono">/b/{business.slug}</span></p>
          </>
        )}
        {tab === 'brand' && (
          <>
            <div className="flex items-center gap-4">
              {logo ? <img src={logo} alt="Logo" className="size-16 rounded-[12px] border border-line object-cover" /> : <span className="flex size-16 items-center justify-center rounded-[12px] font-display text-2xl" style={{ background: color, color: onColor(color) }}>{business.name.charAt(0)}</span>}
              <div className="flex flex-col gap-2">
                <input ref={fileRef} type="file" accept="image/png,image/jpeg,image/webp" className="hidden" onChange={(e) => e.target.files?.[0] && upload.mutate(e.target.files[0])} />
                <Button type="button" variant="secondary" size="sm" icon={<Upload className="size-4" />} loading={upload.isPending} onClick={() => fileRef.current?.click()}>Tải logo lên</Button>
                <span className="text-sm text-ink-3">PNG, JPG hoặc WEBP, tối đa 2MB</span>
              </div>
            </div>
            <Input label="Hoặc dán URL logo" placeholder="https://…" error={errors.logoUrl?.message} {...form.register('logoUrl')} />
            <div className="flex items-end gap-3">
              <Input label="Màu thương hiệu" error={errors.brandColor?.message} wrapClassName="flex-1" {...form.register('brandColor')} />
              <input type="color" aria-label="Chọn màu" value={color} onChange={(e) => form.setValue('brandColor', e.target.value)} className="h-11 w-14 cursor-pointer rounded-[var(--radius-control)] border border-line-strong bg-surface p-1 md:h-10" />
            </div>
            <div>
              <p className="mb-2 text-sm font-medium">Xem trước nút đặt lịch</p>
              <span className="inline-flex h-11 items-center rounded-[var(--radius-control)] px-5 font-medium" style={{ background: color, color: onColor(color) }}>Xác nhận đặt lịch</span>
            </div>
          </>
        )}
        {tab === 'payment' && (
          <>
            <Select label="Thu tiền trước khi xác nhận lịch" {...form.register('paymentMode')}>
              <option value="NONE">Không thu trước – trả tại cơ sở</option>
              <option value="DEPOSIT">Đặt cọc theo %</option>
              <option value="FULL">Trả trước 100%</option>
            </Select>
            {mode === 'DEPOSIT' && <Input label="Tỷ lệ đặt cọc (%)" type="number" min={5} max={100} {...form.register('depositPercent')} />}
            {mode !== 'NONE' && (
              <>
                <Select label="Ngân hàng nhận tiền" {...form.register('bankBin')}>
                  <option value="">Chọn ngân hàng</option>
                  {BANKS.map(([bin, n]) => <option key={bin} value={bin}>{n}</option>)}
                </Select>
                <Input label="Số tài khoản" inputMode="numeric" error={errors.bankAccountNo?.message} {...form.register('bankAccountNo')} />
                <Input label="Tên chủ tài khoản" hint="Viết hoa không dấu, đúng như trên ngân hàng" {...form.register('bankAccountName')} />
                <p className="text-sm text-ink-2">Khách quét mã VietQR với nội dung là mã lịch hẹn. Kết nối SePay hoặc Casso để tự động xác nhận (xem tài liệu), hoặc bấm “Đã nhận tiền” trên lịch hẹn.</p>
              </>
            )}
            <div className="grid gap-4 border-t border-line pt-4 sm:grid-cols-3">
              <Input label="Đặt trước tối thiểu (phút)" type="number" {...form.register('minLeadMinutes')} />
              <Input label="Mở lịch trước (ngày)" type="number" {...form.register('bookingWindowDays')} />
              <Input label="Cho hủy trước (phút)" type="number" {...form.register('cancelBeforeMinutes')} />
            </div>
          </>
        )}
        <Button type="submit" loading={save.isPending} className="self-start">Lưu thay đổi</Button>
      </Panel>
    </form>
  )
}

function BranchesEditor() {
  const toast = useToast()
  const qc = useQueryClient()
  const branches = useBranches()
  const [newName, setNewName] = useState('')
  const add = useMutation({
    mutationFn: () => api.post('/branches', { name: newName }),
    onSuccess: () => { setNewName(''); qc.invalidateQueries({ queryKey: ['branches'] }); toast('Đã thêm chi nhánh') },
    onError: (e) => toast(errorMessage(e), 'error'),
  })
  return (
    <div className="flex max-w-3xl flex-col gap-6">
      {branches.data?.map((b) => <BranchCard key={b.id} branch={b} />)}
      <form onSubmit={(e) => { e.preventDefault(); if (newName.trim()) add.mutate() }} className="flex items-end gap-2">
        <Input wrapClassName="flex-1" label="Thêm chi nhánh" placeholder="Tên chi nhánh" value={newName} onChange={(e) => setNewName(e.target.value)} />
        <Button type="submit" icon={<Plus className="size-4" />} loading={add.isPending}>Thêm</Button>
      </form>
    </div>
  )
}

function BranchCard({ branch }: { branch: Branch }) {
  const toast = useToast()
  const qc = useQueryClient()
  const [info, setInfo] = useState({ name: branch.name, address: branch.address ?? '', phone: branch.phone ?? '', active: branch.active })
  const [hours, setHours] = useState<BranchHour[]>(() =>
    Array.from({ length: 7 }, (_, i) => {
      const h = branch.hours.find((x) => x.dayOfWeek === i + 1)
      return h ? { ...h, openTime: h.openTime.slice(0, 5), closeTime: h.closeTime.slice(0, 5) } : { dayOfWeek: i + 1, openTime: '08:00', closeTime: '20:00', closed: true }
    }),
  )
  const save = useMutation({
    mutationFn: async () => {
      await api.put(`/branches/${branch.id}`, info)
      await api.put(`/branches/${branch.id}/hours`, { hours })
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['branches'] }); toast('Đã lưu chi nhánh') },
    onError: (e) => toast(errorMessage(e), 'error'),
  })
  const set = (i: number, patch: Partial<BranchHour>) => setHours(hours.map((h, j) => (j === i ? { ...h, ...patch } : h)))
  return (
    <Panel className="p-5">
      <div className="grid gap-4 sm:grid-cols-2">
        <Input label="Tên chi nhánh" value={info.name} onChange={(e) => setInfo({ ...info, name: e.target.value })} />
        <Input label="Số điện thoại" value={info.phone} onChange={(e) => setInfo({ ...info, phone: e.target.value })} />
        <Input wrapClassName="sm:col-span-2" label="Địa chỉ" value={info.address} onChange={(e) => setInfo({ ...info, address: e.target.value })} />
      </div>
      <h3 className="mt-6 mb-2 font-medium">Giờ mở cửa</h3>
      <div className="divide-y divide-line">
        {hours.map((h, i) => (
          <div key={h.dayOfWeek} className="flex flex-wrap items-center gap-3 py-2">
            <span className="w-20 text-[0.9375rem]">{DAY_NAMES[i]}</span>
            <Checkbox label="Mở cửa" checked={!h.closed} onChange={(e) => set(i, { closed: !e.target.checked })} />
            {!h.closed && (
              <span className="flex items-center gap-2">
                <input type="time" step={900} aria-label={`${DAY_NAMES[i]} mở`} value={h.openTime} onChange={(e) => set(i, { openTime: e.target.value })} className="h-10 rounded-[var(--radius-control)] border border-line-strong px-2" />
                <span className="text-ink-3">–</span>
                <input type="time" step={900} aria-label={`${DAY_NAMES[i]} đóng`} value={h.closeTime} onChange={(e) => set(i, { closeTime: e.target.value })} className="h-10 rounded-[var(--radius-control)] border border-line-strong px-2" />
              </span>
            )}
          </div>
        ))}
      </div>
      <div className="mt-4 flex flex-wrap items-center gap-4">
        <Button loading={save.isPending} onClick={() => save.mutate()}>Lưu chi nhánh</Button>
        <Checkbox label="Đang hoạt động" checked={info.active} onChange={(e) => setInfo({ ...info, active: e.target.checked })} />
      </div>
    </Panel>
  )
}

function HolidaysEditor() {
  const toast = useToast()
  const branches = useBranches()
  const q = useQuery({ queryKey: ['holidays'], queryFn: async () => (await api.get<Holiday[]>('/holidays')).data })
  const [v, setV] = useState({ date: '', name: '', branchId: '' })
  const add = useMutation({
    mutationFn: () => api.post('/holidays', { date: v.date, name: v.name, branchId: v.branchId ? Number(v.branchId) : null }),
    onSuccess: () => { q.refetch(); setV({ date: '', name: '', branchId: '' }); toast('Đã thêm ngày nghỉ') },
    onError: (e) => toast(errorMessage(e), 'error'),
  })
  const del = useMutation({ mutationFn: (id: number) => api.delete(`/holidays/${id}`), onSuccess: () => q.refetch() })
  return (
    <div className="max-w-2xl">
      <Panel className="mb-6 p-5">
        <form onSubmit={(e) => { e.preventDefault(); if (v.date && v.name) add.mutate() }} className="grid gap-4 sm:grid-cols-2">
          <Input label="Ngày" type="date" value={v.date} onChange={(e) => setV({ ...v, date: e.target.value })} />
          <Input label="Tên ngày nghỉ" placeholder="Tết Nguyên đán" value={v.name} onChange={(e) => setV({ ...v, name: e.target.value })} />
          <Select label="Áp dụng cho" value={v.branchId} onChange={(e) => setV({ ...v, branchId: e.target.value })}>
            <option value="">Toàn bộ cơ sở</option>
            {branches.data?.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
          </Select>
          <Button type="submit" className="self-end justify-self-start" loading={add.isPending} disabled={!v.date || !v.name}>Thêm ngày nghỉ</Button>
        </form>
      </Panel>
      {q.data?.length === 0 ? <p className="text-ink-2">Chưa có ngày nghỉ sắp tới.</p> : (
        <ul className="divide-y divide-line rounded-[var(--radius-panel)] border border-line bg-surface">
          {q.data?.map((h) => (
            <li key={h.id} className="flex items-center justify-between px-5 py-3">
              <span><span className="tabular font-medium">{ddmmyyyy(h.date)}</span> · {h.name}<span className="text-ink-2"> · {h.branchId ? branches.data?.find((b) => b.id === h.branchId)?.name : 'Toàn cơ sở'}</span></span>
              <button onClick={() => del.mutate(h.id)} aria-label={`Xóa ${h.name}`} className="inline-flex size-10 items-center justify-center rounded-full text-ink-2 hover:text-danger"><Trash2 className="size-4" /></button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
