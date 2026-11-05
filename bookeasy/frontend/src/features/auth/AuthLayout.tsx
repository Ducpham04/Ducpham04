import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Logo } from '../landing/Logo'

export function AuthLayout({ title, subtitle, children }: { title: string; subtitle: ReactNode; children: ReactNode }) {
  return (
    <div className="grid min-h-dvh md:grid-cols-[1fr_minmax(0,0.9fr)]">
      <div className="flex flex-col px-4 py-6 md:px-12">
        <Link to="/" className="self-start"><Logo /></Link>
        <div className="mx-auto flex w-full max-w-sm flex-1 flex-col justify-center py-10">
          <h1 className="text-[1.75rem] font-semibold">{title}</h1>
          <p className="mt-1 mb-7 text-ink-2">{subtitle}</p>
          {children}
        </div>
      </div>
      <aside className="hidden flex-col justify-end bg-sunken p-12 md:flex">
        <p className="max-w-[34ch] font-display text-[2rem] leading-tight">
          Khách tự đặt lịch, hệ thống tự nhắc, bạn chỉ việc phục vụ.
        </p>
        <p className="mt-4 max-w-[46ch] text-ink-2">
          BookEasy giúp spa, salon, phòng gym và phòng khám nhỏ nhận lịch 24/7, chặn trùng lịch, thu cọc VietQR và giảm khách bùng hẹn.
        </p>
      </aside>
    </div>
  )
}
