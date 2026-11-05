import { useEffect, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import { api, errorMessage } from '../../lib/api'
import { durationLabel, formatVnd } from '../../lib/format'
import type { Category, Service } from '../../lib/types'
import { Button, Checkbox, ConfirmDialog, EmptyState, ErrorState, Input, PageHeader, Panel, Select, Sheet, Skeleton, Textarea, useToast } from '../../components/ui'
import { useCategories, useServices, useStaffList } from './queries'

const schema = z.object({
  name: z.string().trim().min(2, 'Nhập tên dịch vụ').max(150),
  categoryId: z.string(),
  description: z.string().max(1000),
  durationMinutes: z.coerce.number<string | number>().int().min(15, 'Tối thiểu 15 phút').max(480, 'Tối đa 8 giờ').refine((v) => v % 15 === 0, 'Bội số của 15 phút'),
  price: z.coerce.number<string | number>().int().min(0, 'Giá không hợp lệ'),
  active: z.boolean(),
  staffIds: z.array(z.number()),
})
type FormIn = z.input<typeof schema>
type FormOut = z.output<typeof schema>

export default function ServicesPage() {
  const services = useServices()
  const categories = useCategories()
  const [editing, setEditing] = useState<Service | 'new' | null>(null)
  const [catOpen, setCatOpen] = useState(false)

  const byCat = (cid: number | null) => (services.data ?? []).filter((s) => s.categoryId === cid)
  const groups: { id: number | null; name: string }[] = [...(categories.data ?? []).map((c) => ({ id: c.id as number | null, name: c.name })), { id: null, name: 'Chưa phân loại' }]

  return (
    <div>
      <PageHeader
        title="Dịch vụ"
        description="Dịch vụ đang hiển thị sẽ xuất hiện trên trang đặt lịch nếu có ít nhất một nhân viên thực hiện."
        actions={<><Button variant="secondary" onClick={() => setCatOpen(true)}>Danh mục</Button><Button icon={<Plus className="size-4" />} onClick={() => setEditing('new')}>Thêm dịch vụ</Button></>}
      />
      {services.isLoading ? <Skeleton className="h-64" /> : services.isError ? <ErrorState message={errorMessage(services.error)} onRetry={() => services.refetch()} /> : services.data?.length === 0 ? (
        <Panel><EmptyState title="Chưa có dịch vụ" description="Thêm dịch vụ đầu tiên để khách có thể đặt lịch." action={<Button onClick={() => setEditing('new')}>Thêm dịch vụ</Button>} /></Panel>
      ) : (
        <div className="flex flex-col gap-6">
          {groups.filter((g) => byCat(g.id).length).map((g) => (
            <section key={g.id ?? 'none'}>
              <h2 className="mb-2 text-lg font-semibold">{g.name}</h2>
              <Panel>
                <ul className="divide-y divide-line">
                  {byCat(g.id).map((s) => (
                    <li key={s.id} className="flex items-center gap-3 px-5 py-3">
                      <div className="min-w-0 flex-1">
                        <p className="truncate font-medium">{s.name} {!s.active && <span className="ml-1 rounded-full bg-sunken px-2 py-0.5 text-[0.75rem] font-normal text-ink-2">Đang ẩn</span>}</p>
                        <p className="truncate text-sm text-ink-2">{durationLabel(s.durationMinutes)} · {s.staffIds.length} nhân viên</p>
                      </div>
                      <span className="tabular font-medium">{formatVnd(s.price)}</span>
                      <button onClick={() => setEditing(s)} className="inline-flex size-10 items-center justify-center rounded-full text-ink-2 hover:bg-sunken hover:text-ink" aria-label={`Sửa ${s.name}`}><Pencil className="size-4" /></button>
                    </li>
                  ))}
                </ul>
              </Panel>
            </section>
          ))}
        </div>
      )}
      <ServiceSheet service={editing} categories={categories.data ?? []} onClose={() => setEditing(null)} />
      <CategorySheet open={catOpen} onClose={() => setCatOpen(false)} />
    </div>
  )
}

function ServiceSheet({ service, categories, onClose }: { service: Service | 'new' | null; categories: Category[]; onClose: () => void }) {
  const toast = useToast()
  const qc = useQueryClient()
  const staff = useStaffList()
  const [confirmDelete, setConfirmDelete] = useState(false)
  const form = useForm<FormIn, unknown, FormOut>({ resolver: zodResolver(schema) })
  const isNew = service === 'new'
  useEffect(() => {
    if (!service) return
    const s = service === 'new' ? null : service
    form.reset({
      name: s?.name ?? '',
      categoryId: s?.categoryId ? String(s.categoryId) : categories[0] ? String(categories[0].id) : '',
      description: s?.description ?? '',
      durationMinutes: s?.durationMinutes ?? 60,
      price: s?.price ?? 0,
      active: s?.active ?? true,
      staffIds: s?.staffIds ?? [],
    })
  }, [service, categories, form])

  const save = useMutation({
    mutationFn: async (v: FormOut) => {
      const body = { ...v, categoryId: v.categoryId ? Number(v.categoryId) : null, sortOrder: 0 }
      return isNew ? api.post('/services', body) : api.put(`/services/${(service as Service).id}`, body)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['services'] })
      qc.invalidateQueries({ queryKey: ['staff'] })
      toast(isNew ? 'Đã thêm dịch vụ' : 'Đã lưu dịch vụ')
      onClose()
    },
    onError: (e) => toast(errorMessage(e), 'error'),
  })
  const remove = useMutation({
    mutationFn: () => api.delete(`/services/${(service as Service).id}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['services'] })
      toast('Đã ẩn dịch vụ')
      setConfirmDelete(false)
      onClose()
    },
  })
  const staffIds = form.watch('staffIds') ?? []
  const { errors } = form.formState

  return (
    <Sheet
      open={service !== null}
      onClose={onClose}
      title={isNew ? 'Thêm dịch vụ' : 'Sửa dịch vụ'}
      footer={
        <div className="flex gap-2">
          <Button type="submit" form="svc-form" loading={save.isPending}>Lưu</Button>
          {!isNew && <Button variant="ghost" className="ml-auto text-danger" icon={<Trash2 className="size-4" />} onClick={() => setConfirmDelete(true)}>Ẩn dịch vụ</Button>}
        </div>
      }
    >
      <form id="svc-form" noValidate onSubmit={form.handleSubmit((v) => save.mutate(v))} className="flex flex-col gap-4">
        <Input label="Tên dịch vụ" error={errors.name?.message} {...form.register('name')} />
        <Select label="Danh mục" {...form.register('categoryId')}>
          <option value="">Chưa phân loại</option>
          {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </Select>
        <div className="grid grid-cols-2 gap-3">
          <Input label="Thời lượng (phút)" type="number" step={15} min={15} error={errors.durationMinutes?.message} {...form.register('durationMinutes')} />
          <Input label="Giá (₫)" type="number" step={1000} min={0} error={errors.price?.message} {...form.register('price')} />
        </div>
        <Textarea label="Mô tả" {...form.register('description')} />
        <Checkbox label="Hiển thị trên trang đặt lịch" {...form.register('active')} />
        <fieldset>
          <legend className="mb-1 text-sm font-medium">Nhân viên thực hiện được</legend>
          {staff.data?.filter((s) => s.active).map((s) => (
            <Checkbox
              key={s.id}
              className="flex"
              label={s.fullName}
              checked={staffIds.includes(s.id)}
              onChange={(e) => form.setValue('staffIds', e.target.checked ? [...staffIds, s.id] : staffIds.filter((x) => x !== s.id))}
            />
          ))}
        </fieldset>
      </form>
      <ConfirmDialog open={confirmDelete} title="Ẩn dịch vụ?" message="Dịch vụ không còn hiển thị cho khách. Lịch sử lịch hẹn vẫn được giữ nguyên." confirmLabel="Ẩn dịch vụ" danger loading={remove.isPending} onConfirm={() => remove.mutate()} onClose={() => setConfirmDelete(false)} />
    </Sheet>
  )
}

function CategorySheet({ open, onClose }: { open: boolean; onClose: () => void }) {
  const toast = useToast()
  const qc = useQueryClient()
  const categories = useCategories()
  const [name, setName] = useState('')
  const refresh = () => {
    qc.invalidateQueries({ queryKey: ['categories'] })
    qc.invalidateQueries({ queryKey: ['services'] })
  }
  const add = useMutation({
    mutationFn: () => api.post('/service-categories', { name, sortOrder: (categories.data?.length ?? 0) + 1 }),
    onSuccess: () => { setName(''); refresh() },
    onError: (e) => toast(errorMessage(e), 'error'),
  })
  const del = useMutation({
    mutationFn: (id: number) => api.delete(`/service-categories/${id}`),
    onSuccess: refresh,
    onError: (e) => toast(errorMessage(e), 'error'),
  })
  return (
    <Sheet open={open} onClose={onClose} title="Danh mục dịch vụ">
      <form onSubmit={(e) => { e.preventDefault(); if (name.trim()) add.mutate() }} className="mb-5 flex items-end gap-2">
        <Input wrapClassName="flex-1" label="Danh mục mới" value={name} onChange={(e) => setName(e.target.value)} placeholder="Ví dụ: Chăm sóc tóc" />
        <Button type="submit" loading={add.isPending}>Thêm</Button>
      </form>
      <ul className="divide-y divide-line rounded-[var(--radius-panel)] border border-line">
        {categories.data?.map((c) => (
          <li key={c.id} className="flex items-center justify-between px-4 py-2.5">
            {c.name}
            <button onClick={() => del.mutate(c.id)} className="inline-flex size-10 items-center justify-center rounded-full text-ink-2 hover:text-danger" aria-label={`Xóa ${c.name}`}><Trash2 className="size-4" /></button>
          </li>
        ))}
      </ul>
    </Sheet>
  )
}
