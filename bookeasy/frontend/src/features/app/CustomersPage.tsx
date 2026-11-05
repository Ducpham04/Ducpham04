import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Phone, Search } from 'lucide-react'
import { api, errorMessage } from '../../lib/api'
import { dateTimeLabel, ddmmyyyy, formatVnd } from '../../lib/format'
import type { CustomerDetail, CustomerSummary, Page } from '../../lib/types'
import { Button, EmptyState, ErrorState, Input, PageHeader, Panel, Skeleton, StatusBadge, Textarea, useToast } from '../../components/ui'

export default function CustomersPage() {
  const [q, setQ] = useState('')
  const [debounced, setDebounced] = useState('')
  const [page, setPage] = useState(0)
  useEffect(() => {
    const t = setTimeout(() => { setDebounced(q); setPage(0) }, 300)
    return () => clearTimeout(t)
  }, [q])
  const list = useQuery({
    queryKey: ['customers', debounced, page],
    queryFn: async () => (await api.get<Page<CustomerSummary>>('/customers', { params: { q: debounced || undefined, page, size: 20 } })).data,
    placeholderData: (prev) => prev,
  })

  return (
    <div>
      <PageHeader title="Khách hàng" description="Khách được tự động lưu khi đặt lịch, nhận diện theo số điện thoại." />
      <div className="relative mb-4 md:w-80">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ink-3" aria-hidden />
        <Input aria-label="Tìm khách" placeholder="Tìm theo tên hoặc số điện thoại" value={q} onChange={(e) => setQ(e.target.value)} className="pl-9" />
      </div>
      <Panel>
        {list.isLoading ? (
          <div className="space-y-2 p-4">{[0, 1, 2].map((i) => <Skeleton key={i} className="h-12" />)}</div>
        ) : list.isError ? (
          <ErrorState message={errorMessage(list.error)} onRetry={() => list.refetch()} />
        ) : list.data?.items.length === 0 ? (
          <EmptyState title="Chưa có khách hàng" description="Khách sẽ xuất hiện ở đây sau lịch hẹn đầu tiên." />
        ) : (
          <table className="w-full text-[0.9375rem]">
            <thead className="hidden border-b border-line text-left text-sm text-ink-2 md:table-header-group">
              <tr>
                <th className="px-5 py-3 font-medium">Khách hàng</th>
                <th className="px-3 py-3 text-right font-medium">Số lần đến</th>
                <th className="px-3 py-3 text-right font-medium">Không đến</th>
                <th className="px-3 py-3 text-right font-medium">Tổng chi tiêu</th>
                <th className="px-5 py-3 font-medium">Lần gần nhất</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {list.data?.items.map((c) => (
                <tr key={c.id} className="relative grid grid-cols-[1fr_auto] px-4 py-3 hover:bg-sunken md:table-row md:p-0">
                  <td className="md:px-5 md:py-3">
                    <Link to={`/app/customers/${c.id}`} className="font-medium after:absolute after:inset-0">{c.fullName}</Link>
                    <span className="block text-sm text-ink-2">{c.phone}</span>
                  </td>
                  <td className="tabular text-right text-sm text-ink-2 md:px-3 md:py-3 md:text-[0.9375rem] md:text-ink"><span className="md:hidden">Đến </span>{c.visits}</td>
                  <td className="tabular hidden text-right md:table-cell md:px-3 md:py-3">{c.noShows}</td>
                  <td className="tabular hidden text-right md:table-cell md:px-3 md:py-3">{formatVnd(c.totalSpent)}</td>
                  <td className="hidden md:table-cell md:px-5 md:py-3">{c.lastVisitAt ? ddmmyyyy(c.lastVisitAt) : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {list.data && list.data.totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-line px-5 py-3 text-sm text-ink-2">
            <span>Trang {list.data.page + 1}/{list.data.totalPages} · {list.data.totalElements} khách</span>
            <div className="flex gap-2">
              <Button size="sm" variant="secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Trước</Button>
              <Button size="sm" variant="secondary" disabled={page + 1 >= list.data.totalPages} onClick={() => setPage((p) => p + 1)}>Sau</Button>
            </div>
          </div>
        )}
      </Panel>
    </div>
  )
}

export function CustomerDetailPage() {
  const { id } = useParams()
  const toast = useToast()
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['customer', id], queryFn: async () => (await api.get<CustomerDetail>(`/customers/${id}`)).data })
  const [notes, setNotes] = useState<string | null>(null)
  const save = useMutation({
    mutationFn: async () => (await api.patch<CustomerDetail>(`/customers/${id}`, { notes: notes ?? '' })).data,
    onSuccess: (d) => {
      qc.setQueryData(['customer', id], d)
      setNotes(null)
      toast('Đã lưu ghi chú')
    },
    onError: (e) => toast(errorMessage(e), 'error'),
  })
  if (q.isLoading) return <Skeleton className="h-96" />
  if (q.isError || !q.data) return <ErrorState message={errorMessage(q.error)} onRetry={() => q.refetch()} />
  const { customer: c, bookings } = q.data
  return (
    <div>
      <Link to="/app/customers" className="mb-3 inline-flex items-center gap-1 text-sm text-ink-2 hover:text-ink"><ArrowLeft className="size-4" aria-hidden /> Khách hàng</Link>
      <PageHeader
        title={c.fullName}
        description={<span className="inline-flex flex-wrap gap-x-4"><a href={`tel:${c.phone}`} className="inline-flex items-center gap-1 text-brand"><Phone className="size-4" aria-hidden />{c.phone}</a>{c.email && <span>{c.email}</span>}</span>}
      />
      <p className="mb-6 max-w-[65ch] text-lg">
        Đã đến <strong className="font-semibold">{c.visits} lần</strong>, chi tiêu <strong className="tabular font-semibold">{formatVnd(c.totalSpent)}</strong>
        {c.noShows > 0 && <>, <strong className="font-semibold text-danger">{c.noShows} lần không đến</strong></>}
        {c.lastVisitAt && <>. Lần gần nhất {ddmmyyyy(c.lastVisitAt)}</>}.
      </p>
      <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
        <Panel>
          <h2 className="border-b border-line px-5 py-3.5 font-semibold">Lịch sử lịch hẹn</h2>
          {bookings.length === 0 ? <EmptyState title="Chưa có lịch hẹn" /> : (
            <ul className="divide-y divide-line">
              {bookings.map((b) => (
                <li key={b.id} className="flex items-center gap-3 px-5 py-3">
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-medium">{b.serviceName}</p>
                    <p className="tabular truncate text-sm text-ink-2">{dateTimeLabel(b.startAt)} · {b.staffName}</p>
                  </div>
                  <span className="tabular hidden text-sm sm:block">{formatVnd(b.price)}</span>
                  <StatusBadge status={b.status} />
                </li>
              ))}
            </ul>
          )}
        </Panel>
        <Panel className="self-start p-5">
          <Textarea label="Ghi chú về khách" rows={5} placeholder="Sở thích, dị ứng, lưu ý khi phục vụ…" value={notes ?? q.data.notes ?? ''} onChange={(e) => setNotes(e.target.value)} />
          <Button className="mt-3" size="sm" disabled={notes === null} loading={save.isPending} onClick={() => save.mutate()}>Lưu ghi chú</Button>
        </Panel>
      </div>
    </div>
  )
}
