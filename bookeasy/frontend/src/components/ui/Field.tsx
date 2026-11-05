import { clsx } from 'clsx'
import { forwardRef, useId, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react'

const control =
  'w-full rounded-[var(--radius-control)] border bg-surface px-3 text-ink placeholder:text-ink-3 transition-colors duration-150 ' +
  'focus:outline-none focus:border-brand focus:ring-2 focus:ring-brand/20 disabled:bg-sunken disabled:text-ink-3'

interface WrapProps {
  label?: string
  hint?: ReactNode
  error?: string
  className?: string
  children: (id: string, describedBy: string | undefined) => ReactNode
}

function Wrap({ label, hint, error, className, children }: WrapProps) {
  const id = useId()
  const descId = error || hint ? `${id}-desc` : undefined
  return (
    <div className={clsx('flex flex-col gap-1.5', className)}>
      {label && (
        <label htmlFor={id} className="text-sm font-medium text-ink">
          {label}
        </label>
      )}
      {children(id, descId)}
      {error ? (
        <p id={descId} className="text-sm text-danger">
          {error}
        </p>
      ) : hint ? (
        <p id={descId} className="text-sm text-ink-3">
          {hint}
        </p>
      ) : null}
    </div>
  )
}

type InputProps = InputHTMLAttributes<HTMLInputElement> & { label?: string; hint?: ReactNode; error?: string; wrapClassName?: string }

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input({ label, hint, error, wrapClassName, className, ...rest }, ref) {
  return (
    <Wrap label={label} hint={hint} error={error} className={wrapClassName}>
      {(id, desc) => (
        <input
          ref={ref}
          id={id}
          aria-invalid={error ? true : undefined}
          aria-describedby={desc}
          className={clsx(control, 'h-11 md:h-10', error ? 'border-danger' : 'border-line-strong', className)}
          {...rest}
        />
      )}
    </Wrap>
  )
})

type SelectProps = SelectHTMLAttributes<HTMLSelectElement> & { label?: string; hint?: ReactNode; error?: string; wrapClassName?: string }

export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select({ label, hint, error, wrapClassName, className, children, ...rest }, ref) {
  return (
    <Wrap label={label} hint={hint} error={error} className={wrapClassName}>
      {(id, desc) => (
        <select
          ref={ref}
          id={id}
          aria-invalid={error ? true : undefined}
          aria-describedby={desc}
          className={clsx(control, 'h-11 md:h-10 pr-8', error ? 'border-danger' : 'border-line-strong', className)}
          {...rest}
        >
          {children}
        </select>
      )}
    </Wrap>
  )
})

type TextareaProps = TextareaHTMLAttributes<HTMLTextAreaElement> & { label?: string; hint?: ReactNode; error?: string; wrapClassName?: string }

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea({ label, hint, error, wrapClassName, className, ...rest }, ref) {
  return (
    <Wrap label={label} hint={hint} error={error} className={wrapClassName}>
      {(id, desc) => (
        <textarea
          ref={ref}
          id={id}
          rows={3}
          aria-invalid={error ? true : undefined}
          aria-describedby={desc}
          className={clsx(control, 'py-2.5', error ? 'border-danger' : 'border-line-strong', className)}
          {...rest}
        />
      )}
    </Wrap>
  )
})

export function Checkbox({ label, className, ...rest }: InputHTMLAttributes<HTMLInputElement> & { label: ReactNode }) {
  return (
    <label className={clsx('inline-flex min-h-11 cursor-pointer items-center gap-2.5 text-[0.9375rem] md:min-h-9', className)}>
      <input type="checkbox" className="size-[18px] accent-[var(--color-brand)]" {...rest} />
      {label}
    </label>
  )
}
