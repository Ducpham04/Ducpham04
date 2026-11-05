import { clsx } from 'clsx'
import { AlertCircle, CheckCircle2, CircleDashed, Clock, LogIn, UserX, X, XCircle } from 'lucide-react'
import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { STATUS_LABEL, STATUS_TONE } from '../../lib/status'
import type { BookingStatus } from '../../lib/types'
import { initials } from '../../lib/format'
import { Button } from './Button'

export { Button } from './Button'
export { Input, Select, Textarea, Checkbox } from './Field'

export function Panel({ className, children, as: Tag = 'section' }: { className?: string; children: ReactNode; as?: 'section' | 'div' }) {
  return <Tag className={clsx('rounded-[var(--radius-panel)] border border-line bg-surface', className)}>{children}</Tag>
}

export function PageHeader({ title, description, actions }: { title: string; description?: ReactNode; actions?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div className="min-w-0">
        <h1 className="text-[1.75rem] font-semibold">{title}</h1>
        {description && <p className="mt-1 max-w-[65ch] text-ink-2">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  )
}

const STATUS_ICON: Record<BookingStatus, typeof Clock> = {
  PENDING_PAYMENT: Clock,
  CONFIRMED: CircleDashed,
  CHECKED_IN: LogIn,
  COMPLETED: CheckCircle2,
  CANCELLED: XCircle,
  NO_SHOW: UserX,
}

export function StatusBadge({ status, className }: { status: BookingStatus; className?: string }) {
  const Icon = STATUS_ICON[status]
  return (
    <span className={clsx('inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[0.8125rem] font-medium whitespace-nowrap', STATUS_TONE[status], className)}>
      <Icon className="size-3.5" aria-hidden strokeWidth={2} />
      {STATUS_LABEL[status]}
    </span>
  )
}

export function StatusIcon({ status, className }: { status: BookingStatus; className?: string }) {
  const Icon = STATUS_ICON[status]
  return <Icon className={className} aria-label={STATUS_LABEL[status]} strokeWidth={2} />
}

export function Avatar({ name, color, size = 36 }: { name: string; color?: string; size?: number }) {
  return (
    <span
      aria-hidden
      className="inline-flex shrink-0 items-center justify-center rounded-full text-[0.8125rem] font-semibold"
      style={{
        width: size,
        height: size,
        background: color ? `color-mix(in oklch, ${color} 16%, white)` : 'var(--color-sunken)',
        color: color ? `color-mix(in oklch, ${color} 75%, black)` : 'var(--color-ink-2)',
      }}
    >
      {initials(name)}
    </span>
  )
}

export function Skeleton({ className }: { className?: string }) {
  return <div className={clsx('animate-pulse rounded-md bg-sunken', className)} />
}

export function EmptyState({ title, description, action }: { title: string; description?: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-start gap-2 px-5 py-10 md:items-center md:text-center">
      <p className="font-medium text-ink">{title}</p>
      {description && <p className="max-w-[48ch] text-ink-2">{description}</p>}
      {action && <div className="mt-2">{action}</div>}
    </div>
  )
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div role="alert" className="flex flex-col items-start gap-3 px-5 py-8 md:items-center md:text-center">
      <p className="flex items-center gap-2 text-danger">
        <AlertCircle className="size-5" aria-hidden /> {message}
      </p>
      {onRetry && (
        <Button variant="secondary" size="sm" onClick={onRetry}>
          Thử lại
        </Button>
      )}
    </div>
  )
}

export function Segmented<T extends string>({ value, onChange, options, label }: { value: T; onChange: (v: T) => void; options: { value: T; label: string }[]; label: string }) {
  return (
    <div role="radiogroup" aria-label={label} className="inline-flex rounded-[var(--radius-control)] border border-line-strong bg-surface p-0.5">
      {options.map((o) => (
        <button
          key={o.value}
          role="radio"
          aria-checked={value === o.value}
          onClick={() => onChange(o.value)}
          className={clsx(
            'h-9 rounded-[8px] px-3 text-sm font-medium transition-colors duration-150',
            value === o.value ? 'bg-ink text-white' : 'text-ink-2 hover:text-ink',
          )}
        >
          {o.label}
        </button>
      ))}
    </div>
  )
}

export function Tabs<T extends string>({ value, onChange, tabs }: { value: T; onChange: (v: T) => void; tabs: { value: T; label: string }[] }) {
  return (
    <div role="tablist" className="-mx-4 mb-6 flex gap-1 overflow-x-auto border-b border-line px-4 md:mx-0 md:px-0">
      {tabs.map((t) => (
        <button
          key={t.value}
          role="tab"
          aria-selected={value === t.value}
          onClick={() => onChange(t.value)}
          className={clsx(
            '-mb-px h-11 shrink-0 border-b-2 px-3 text-[0.9375rem] font-medium transition-colors duration-150',
            value === t.value ? 'border-brand text-ink' : 'border-transparent text-ink-2 hover:text-ink',
          )}
        >
          {t.label}
        </button>
      ))}
    </div>
  )
}

/** Bottom sheet trên mobile, side panel bên phải trên desktop. */
export function Sheet({ open, onClose, title, children, footer, wide }: { open: boolean; onClose: () => void; title: string; children: ReactNode; footer?: ReactNode; wide?: boolean }) {
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (!open) return
    const prev = document.activeElement as HTMLElement | null
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    document.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    ref.current?.focus()
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = ''
      prev?.focus?.()
    }
  }, [open, onClose])
  if (!open) return null
  return createPortal(
    <div className="fixed inset-0 z-50">
      <div className="anim-fade absolute inset-0 bg-[oklch(0.22_0.02_160/0.32)]" onClick={onClose} />
      <div
        ref={ref}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        tabIndex={-1}
        className={clsx(
          'anim-sheet absolute inset-x-0 bottom-0 flex max-h-[92dvh] flex-col rounded-t-[18px] bg-surface shadow-[var(--shadow-float)] focus:outline-none',
          'md:inset-y-0 md:right-0 md:left-auto md:max-h-none md:rounded-none',
          wide ? 'md:w-[560px]' : 'md:w-[460px]',
        )}
      >
        <div className="flex items-center justify-between gap-3 border-b border-line px-5 py-3.5">
          <h2 className="text-lg font-semibold">{title}</h2>
          <button onClick={onClose} className="-mr-2 inline-flex size-11 items-center justify-center rounded-full text-ink-2 hover:bg-sunken" aria-label="Đóng">
            <X className="size-5" />
          </button>
        </div>
        <div className="flex-1 overflow-y-auto px-5 py-5">{children}</div>
        {footer && <div className="border-t border-line px-5 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))]">{footer}</div>}
      </div>
    </div>,
    document.body,
  )
}

export function ConfirmDialog({ open, title, message, confirmLabel, danger, loading, onConfirm, onClose, children }: {
  open: boolean
  title: string
  message: ReactNode
  confirmLabel: string
  danger?: boolean
  loading?: boolean
  onConfirm: () => void
  onClose: () => void
  children?: ReactNode
}) {
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, onClose])
  if (!open) return null
  return createPortal(
    <div className="fixed inset-0 z-[60] flex items-end justify-center p-4 md:items-center">
      <div className="anim-fade absolute inset-0 bg-[oklch(0.22_0.02_160/0.4)]" onClick={onClose} />
      <div role="alertdialog" aria-modal="true" aria-label={title} className="anim-rise relative w-full max-w-md rounded-[var(--radius-panel)] bg-surface p-5 shadow-[var(--shadow-float)]">
        <h2 className="text-lg font-semibold">{title}</h2>
        <div className="mt-2 text-ink-2">{message}</div>
        {children}
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose} autoFocus>
            Quay lại
          </Button>
          <Button variant={danger ? 'danger' : 'primary'} loading={loading} onClick={onConfirm}>
            {confirmLabel}
          </Button>
        </div>
      </div>
    </div>,
    document.body,
  )
}

// ---------------- Toast ----------------
interface ToastItem { id: number; message: string; tone: 'ok' | 'error' }
const ToastCtx = createContext<(message: string, tone?: 'ok' | 'error') => void>(() => undefined)

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])
  const remove = useCallback((id: number) => setItems((xs) => xs.filter((x) => x.id !== id)), [])
  const push = useCallback(
    (message: string, tone: 'ok' | 'error' = 'ok') => {
      const id = Date.now() + Math.random()
      setItems((xs) => [...xs.slice(-2), { id, message, tone }])
      if (tone === 'ok') setTimeout(() => remove(id), 4000)
    },
    [remove],
  )
  return (
    <ToastCtx.Provider value={push}>
      {children}
      <div aria-live="polite" className="pointer-events-none fixed inset-x-0 bottom-[calc(72px+env(safe-area-inset-bottom))] z-[70] flex flex-col items-center gap-2 px-4 md:bottom-6 md:items-end md:px-6">
        {items.map((t) => (
          <div
            key={t.id}
            role={t.tone === 'error' ? 'alert' : 'status'}
            className={clsx(
              'anim-rise pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-[12px] px-4 py-3 text-[0.9375rem] shadow-[var(--shadow-float)]',
              t.tone === 'error' ? 'bg-danger text-white' : 'bg-ink text-white',
            )}
          >
            <span className="flex-1">{t.message}</span>
            <button onClick={() => remove(t.id)} aria-label="Đóng thông báo" className="-m-1 p-1 opacity-80 hover:opacity-100">
              <X className="size-4" />
            </button>
          </div>
        ))}
      </div>
    </ToastCtx.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useToast() {
  return useContext(ToastCtx)
}

export function Spinner({ label = 'Đang tải' }: { label?: string }) {
  return (
    <div role="status" className="flex items-center gap-2 px-5 py-8 text-ink-2">
      <span className="size-4 animate-spin rounded-full border-2 border-line-strong border-t-brand" aria-hidden />
      {label}…
    </div>
  )
}
