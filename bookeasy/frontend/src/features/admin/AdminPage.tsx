import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { LogOut } from 'lucide-react'
import { api, errorMessage } from '../../lib/api'
import { useAuth } from '../../lib/auth'
import { ddmmyyyy } from '../../lib/format'
import { CATEGORY_LABEL } from '../../lib/status'
import type { AdminBusiness } from '../../lib/types'
import { Button, ConfirmDialog, ErrorState, PageHeader, Panel, Skeleton, useToast } from '../../components/ui'
import { Logo } from '../landing/Logo'

export default function AdminPage() {
  const { logout } = useAuth()
  const toast = useToast()
  const qc = useQueryClient()
  const [target, setTarget] = useState<AdminBusiness | null>(null)
  const list = useQuery({ queryKey: ['admin', 'businesses'], queryFn: async () => (await api.get<AdminBusiness[]>('/admin/businesses')).data })
  const toggle = useMutation({
    mutationFn: (b: AdminBusiness) => api.patch(`/admin/businesses/${b.id}/status`, { status: b.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE' }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin'] }); setTarget(null); toast('Đã cập nhật trạng thái') },
    onError: (e) => toast(errorMessage(e), 'error'),
  })
  const active = list.data?.filter((b) => b.status === 'ACTIVE').length ?? 0
  return (
    <div className="min-h-dvh">
      <header className="flex items-center justify-between border-b border-line bg-surface px-4 py-3 md:px-8">
        <Logo />
        <Button variant="ghost" size="sm" icon={<LogOut className="size-4" />} onClick={logout}>Đăng xuất</Button>
      </header>
      <main className="mx-auto max-w-[1100px] px-4 py-6 md:px-8">
        <PageHeader title="Các cơ sở" description={list.data ? `${list.data.length} cơ sở, ${active} đang hoạt động.` : undefined} />
        <Panel>
          {list.isLoading ? <Skeleton className="m-4 h-40" /> : list.isError ? <ErrorState message={errorMessage(list.error)} onRetry={() => list.refetch()} /> : (
            <ul className="divide-y divide-line">
              {list.data?.map((b) => (
                <li key={b.id} className="flex flex-wrap items-center gap-3 px-5 py-3.5">
                  <div className="min-w-0 flex-1">
                    <p className="font-medium">{b.name} <a href={`/b/${b.slug}`} target="_blank" rel="noreferrer" className="ml-1 font-mono text-sm font-normal text-ink-2 hover:text-brand">/b/{b.slug}</a></p>
                    <p className="text-sm text-ink-2">{CATEGORY_LABEL[b.category]} · {b.ownerName ?? '—'} ({b.ownerEmail ?? '—'}) · {b.bookingCount} lịch hẹn · tạo {ddmmyyyy(b.createdAt)}</p>
                  </div>
                  <span className={b.status === 'ACTIVE' ? 'text-sm text-ok' : 'text-sm text-danger'}>{b.status === 'ACTIVE' ? 'Đang hoạt động' : 'Tạm khóa'}</span>
                  <Button size="sm" variant={b.status === 'ACTIVE' ? 'secondary' : 'primary'} onClick={() => setTarget(b)}>{b.status === 'ACTIVE' ? 'Tạm khóa' : 'Kích hoạt'}</Button>
                </li>
              ))}
            </ul>
          )}
        </Panel>
      </main>
      <ConfirmDialog
        open={!!target}
        title={target?.status === 'ACTIVE' ? `Tạm khóa ${target?.name}?` : `Kích hoạt ${target?.name}?`}
        message={target?.status === 'ACTIVE' ? 'Chủ và nhân viên không đăng nhập được, trang đặt lịch ngừng nhận lịch mới.' : 'Cơ sở sẽ hoạt động bình thường trở lại.'}
        confirmLabel={target?.status === 'ACTIVE' ? 'Tạm khóa' : 'Kích hoạt'}
        danger={target?.status === 'ACTIVE'}
        loading={toggle.isPending}
        onConfirm={() => target && toggle.mutate(target)}
        onClose={() => setTarget(null)}
      />
    </div>
  )
}
