export function Logo({ className }: { className?: string }) {
  return (
    <span className={`inline-flex items-center gap-2 font-semibold ${className ?? ''}`}>
      <svg viewBox="0 0 32 32" className="size-7" aria-hidden>
        <rect width="32" height="32" rx="8" fill="var(--color-brand)" />
        <path d="M9 11h14M9 16h9M9 21h6" stroke="#fff" strokeWidth="2.5" strokeLinecap="round" />
        <circle cx="22" cy="21" r="3" fill="#f2c14e" />
      </svg>
      BookEasy
    </span>
  )
}
