import type { CSSProperties, ReactNode } from 'react'
import { MapPin, Phone } from 'lucide-react'
import { Link } from 'react-router-dom'
import { onColor } from '../../lib/format'
import type { PublicBusiness } from '../../lib/types'

export function brandStyle(color?: string): CSSProperties {
  if (!color) return {}
  return { '--color-brand': color, '--color-on-brand': onColor(color) } as CSSProperties
}

export function PublicShell({ business, children }: { business?: PublicBusiness; children: ReactNode }) {
  return (
    <div className="brand-scope min-h-dvh bg-canvas" style={brandStyle(business?.brandColor)}>
      <header className="border-b border-line bg-surface">
        <div className="mx-auto flex max-w-2xl items-center gap-3 px-4 py-4">
          {business?.logoUrl ? (
            <img src={business.logoUrl} alt="" className="size-11 rounded-[10px] object-cover" />
          ) : (
            <span aria-hidden className="flex size-11 items-center justify-center rounded-[10px] bg-brand font-display text-xl text-on-brand">
              {business?.name.charAt(0) ?? ''}
            </span>
          )}
          <div className="min-w-0">
            <Link to={business ? `/b/${business.slug}` : '#'} className="block truncate font-display text-[1.375rem] leading-tight">
              {business?.name ?? ' '}
            </Link>
            {business && (
              <p className="mt-0.5 flex flex-wrap gap-x-3 text-sm text-ink-2">
                {business.address && (
                  <span className="inline-flex min-w-0 items-center gap-1">
                    <MapPin className="size-3.5 shrink-0" aria-hidden />
                    <span className="truncate">{business.address}</span>
                  </span>
                )}
                {business.phone && (
                  <a href={`tel:${business.phone.replace(/\s/g, '')}`} className="inline-flex items-center gap-1 hover:text-ink">
                    <Phone className="size-3.5" aria-hidden />
                    {business.phone}
                  </a>
                )}
              </p>
            )}
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-2xl px-4 pb-40 pt-5">{children}</main>
      <footer className="mx-auto max-w-2xl px-4 pb-32 text-center text-[0.8125rem] text-ink-3">
        Đặt lịch qua <Link to="/" className="underline underline-offset-2 hover:text-ink-2">BookEasy</Link>
      </footer>
    </div>
  )
}
