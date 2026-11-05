import { Link } from 'react-router-dom'
import { BellRing, CalendarCheck2, QrCode, ShieldCheck } from 'lucide-react'
import { Button } from '../../components/ui'
import { useAuth, homeFor } from '../../lib/auth'
import { Logo } from './Logo'

const FEATURES = [
  { icon: CalendarCheck2, title: 'Trang đặt lịch riêng', text: 'Mỗi cơ sở có đường dẫn /b/ten-co-so mang logo và màu thương hiệu. Dán vào Facebook, Zalo, Google Maps.' },
  { icon: ShieldCheck, title: 'Không bao giờ trùng lịch', text: 'Giờ trống tính theo ca làm, giờ mở cửa và ngày nghỉ. Hai khách cùng bấm một giờ, chỉ một người giữ được chỗ.' },
  { icon: QrCode, title: 'Đặt cọc bằng VietQR', text: 'Khách quét mã, nội dung chuyển khoản là mã lịch hẹn. Kết nối SePay/Casso để tự xác nhận.' },
  { icon: BellRing, title: 'Nhắc lịch tự động', text: 'Email xác nhận, nhắc trước 24 giờ và 2 giờ. Sẵn sàng cho Zalo ZNS.' },
]

export default function LandingPage() {
  const { user } = useAuth()
  return (
    <div className="min-h-dvh">
      <header className="mx-auto flex max-w-6xl items-center justify-between px-4 py-5 md:px-8">
        <Logo />
        <nav className="flex items-center gap-2">
          {user ? (
            <Link to={homeFor(user)}><Button size="sm">Vào trang quản lý</Button></Link>
          ) : (
            <>
              <Link to="/login"><Button variant="ghost" size="sm">Đăng nhập</Button></Link>
              <Link to="/register"><Button size="sm">Dùng thử miễn phí</Button></Link>
            </>
          )}
        </nav>
      </header>
      <main>
        <section className="mx-auto grid max-w-6xl gap-10 px-4 pt-10 pb-16 md:grid-cols-[1.1fr_1fr] md:px-8 md:pt-20">
          <div>
            <h1 className="max-w-[16ch] font-display text-[2.5rem] leading-[1.08] md:text-[3.25rem]">Sổ hẹn thông minh cho spa, salon, gym và phòng khám</h1>
            <p className="mt-5 max-w-[52ch] text-lg text-ink-2">Khách tự đặt lịch 24/7 trên điện thoại. Bạn thấy toàn bộ lịch của nhân viên trên một màn hình, kéo thả để đổi giờ, và giảm khách bùng hẹn bằng đặt cọc.</p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to="/register"><Button size="lg">Tạo trang đặt lịch</Button></Link>
              <Link to="/b/lotus-spa"><Button size="lg" variant="secondary">Xem trang mẫu</Button></Link>
            </div>
            <p className="mt-4 text-sm text-ink-3">Miễn phí 14 ngày · Không cần thẻ · Cài đặt trong 15 phút</p>
          </div>
          <figure className="self-center rounded-[18px] border border-line bg-surface p-5">
            <p className="mb-3 text-sm text-ink-2">Lotus Spa · Thứ bảy</p>
            <ul className="space-y-2" aria-label="Ví dụ lịch hẹn">
              {[['09:00', 'Massage đá nóng', 'Chị Lan', '#2f7d6b'], ['10:30', 'Chăm sóc da cơ bản', 'Chị Mai', '#b4573a'], ['13:45', 'Gội đầu dưỡng sinh', 'Chị Hoa', '#5a6fb0'], ['15:00', 'Trị mụn chuyên sâu', 'Chị Mai', '#b4573a']].map(([t, s, p, c]) => (
                <li key={t} className="flex items-center gap-3 rounded-[10px] px-3 py-2.5" style={{ background: `color-mix(in oklch, ${c} 12%, white)` }}>
                  <span className="tabular font-semibold">{t}</span>
                  <span className="min-w-0 flex-1 truncate">{s}</span>
                  <span className="text-sm text-ink-2">{p}</span>
                </li>
              ))}
            </ul>
          </figure>
        </section>
        <section className="border-t border-line bg-surface">
          <div className="mx-auto grid max-w-6xl gap-x-10 gap-y-8 px-4 py-14 md:grid-cols-2 md:px-8">
            {FEATURES.map((f) => (
              <div key={f.title} className="flex gap-4">
                <f.icon className="mt-1 size-6 shrink-0 text-brand" strokeWidth={1.75} aria-hidden />
                <div>
                  <h2 className="text-lg font-semibold">{f.title}</h2>
                  <p className="mt-1 max-w-[50ch] text-ink-2">{f.text}</p>
                </div>
              </div>
            ))}
          </div>
        </section>
        <section className="mx-auto max-w-6xl px-4 py-14 md:px-8">
          <h2 className="text-2xl font-semibold">Bảng giá đơn giản</h2>
          <div className="mt-6 grid gap-4 md:grid-cols-3">
            {[['Cơ bản', '199.000 ₫', '1 chi nhánh, tối đa 3 nhân viên, email nhắc lịch'], ['Chuyên nghiệp', '449.000 ₫', '3 chi nhánh, 15 nhân viên, VietQR tự xác nhận, báo cáo'], ['Chuỗi', 'Liên hệ', 'Không giới hạn chi nhánh, Zalo ZNS, hỗ trợ ưu tiên']].map(([n, p, d], i) => (
              <div key={n} className={`rounded-[14px] border p-5 ${i === 1 ? 'border-brand' : 'border-line'} bg-surface`}>
                <p className="font-medium">{n}</p>
                <p className="tabular mt-2 text-2xl font-semibold">{p}<span className="text-base font-normal text-ink-2">{p !== 'Liên hệ' && ' /tháng'}</span></p>
                <p className="mt-2 text-ink-2">{d}</p>
              </div>
            ))}
          </div>
        </section>
      </main>
      <footer className="border-t border-line px-4 py-8 text-center text-sm text-ink-3">© {new Date().getFullYear()} BookEasy · Sản phẩm dành cho cơ sở dịch vụ Việt Nam</footer>
    </div>
  )
}
