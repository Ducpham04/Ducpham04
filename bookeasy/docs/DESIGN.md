# BookEasy – Design System & UX

> Nguyên tắc thiết kế tham chiếu bộ quy tắc **[impeccable](https://github.com/pbakaus/impeccable)** để tránh giao diện "trông như AI làm": không gradient tím-xanh, không thẻ lồng thẻ, không ô icon bo tròn trên mọi tiêu đề, không chữ gradient, không eyebrow/kicker trên tiêu đề, không emoji làm icon, không viền trái màu dày trên card, không "ghost card" (vừa viền vừa bóng).

## 1. Định hướng
- **Tính cách**: điềm tĩnh, gọn, đáng tin – như sổ hẹn của một quầy lễ tân giỏi. Không trang trí thừa; dữ liệu (giờ, tên khách, giá) là nhân vật chính.
- **Hai bối cảnh**:
  - *Trang công khai* (khách hàng, điện thoại): ấm, có thương hiệu riêng của cơ sở, chữ tiêu đề serif, nhiều khoảng thở.
  - *Ứng dụng quản trị* (chủ/nhân viên): mật độ cao hơn, sans-serif, ưu tiên quét nhanh, số dạng tabular.
- Sáng là mặc định (dùng ở quầy lễ tân ban ngày). Dark mode không nằm trong MVP – quyết định theo bối cảnh sử dụng, không theo trào lưu.

## 2. Màu sắc (OKLCH, neutral ánh xanh rêu nhẹ)
Định nghĩa theo **vai trò**, không theo swatch. Neutral pha nhẹ hue 160 (hue thương hiệu) để gắn kết.

| Token | Giá trị | Dùng cho |
|---|---|---|
| `--canvas` | `oklch(0.985 0.004 160)` | Nền trang |
| `--surface` | `oklch(1 0 0)` | Bề mặt nổi (panel, popover) |
| `--sunken` | `oklch(0.965 0.006 160)` | Vùng lõm, hàng xen kẽ, input disabled |
| `--line` | `oklch(0.91 0.008 160)` | Đường kẻ 1px |
| `--ink` | `oklch(0.22 0.015 160)` | Chữ chính (≈ 15:1 trên canvas) |
| `--ink-2` | `oklch(0.45 0.015 160)` | Chữ phụ (≥ 7:1) |
| `--ink-3` | `oklch(0.56 0.012 160)` | Placeholder, chú thích (≥ 4.5:1) |
| `--brand` | `oklch(0.50 0.09 165)` | Hành động chính, focus (xanh ngọc bích "jade") |
| `--brand-strong` | `oklch(0.42 0.085 165)` | Hover của hành động chính |
| `--brand-soft` | `oklch(0.95 0.03 165)` | Nền chọn (selected), highlight |
| `--danger` | `oklch(0.55 0.18 25)` | Hủy, lỗi |
| `--warn` | `oklch(0.72 0.14 75)` | Chờ thanh toán |
| `--ok` | `oklch(0.58 0.12 150)` | Hoàn thành, đã thanh toán |

- Màu thương hiệu cơ sở (hex từ cấu hình) ghi đè `--brand` trên trang `/b/{slug}`; chữ trên nền brand tự chọn trắng/đen theo độ sáng.
- **Trạng thái booking không chỉ dựa vào màu**: luôn có nhãn chữ + icon.
  - Chờ thanh toán (warn, icon đồng hồ) · Đã xác nhận (brand) · Đã đến (xanh dương `oklch(0.55 0.12 250)`) · Hoàn thành (ok, dấu tick) · Hủy (danger, gạch ngang) · Không đến (xám, icon user-x).
- Không dùng màu chính cho trang trí.

## 3. Typography
- **Sans (UI)**: *Be Vietnam Pro* (400/500/600) – thiết kế cho tiếng Việt, dấu rõ, không phải Inter.
- **Serif (tiêu đề trang công khai)**: *Fraunces* (600) – tạo cảm giác "cửa hàng", hỗ trợ tiếng Việt.
- Thang chữ (rem): 0.8125 (chú thích) · **1 (body, sàn tối thiểu 16px)** · 1.125 · 1.375 · 1.75 · 2.25 · 3 (hero công khai, tối đa).
- Line-height: body 1.55; tiêu đề 1.15. Tracking tiêu đề −0.02em (không bao giờ < −0.04em).
- `text-wrap: balance` cho tiêu đề, `text-wrap: pretty` cho đoạn văn; độ dài dòng ≤ 70ch.
- Số tiền, giờ, thống kê: `font-variant-numeric: tabular-nums`.
- Monospace chỉ cho mã booking.

## 4. Spacing, bo góc, độ nổi
- Thang 4px: 4 · 8 · 12 · 16 · 20 · 24 · 32 · 40 · 56 · 80. Nhóm bằng khoảng cách trước khi thêm khung. Khoảng trên tiêu đề > khoảng dưới.
- Bo góc: input/nút 10px; panel 14px; chip/pill chỉ cho control nhỏ (bộ lọc, nhãn trạng thái).
- **Độ nổi khai báo một lần**: panel tĩnh dùng viền 1px `--line`, không bóng. Popover/dialog/drag preview dùng bóng có offset: `0 8px 24px -8px oklch(0.22 0.02 160 / 0.18)`, không viền. Không bao giờ cả hai.
- Không lồng card trong card: bên trong panel dùng divider/list.
- Touch target ≥ 44×44px.

## 5. Chuyển động
- 120ms phản hồi (hover/press), 200ms đổi trạng thái, 320ms mở sheet/dialog. Easing `cubic-bezier(0.16, 1, 0.3, 1)`; thoát nhanh hơn vào (≈ 70%).
- Chỉ animate `transform`/`opacity`. Một khoảnh khắc chủ đạo: màn hình xác nhận đặt lịch thành công (dấu tick vẽ nét).
- `prefers-reduced-motion`: bỏ dịch chuyển, giữ đổi màu/opacity.

## 6. Component
| Component | Quy tắc |
|---|---|
| Button | `primary` (nền brand, chữ trắng), `secondary` (nền surface, viền line), `ghost`, `danger`. Cao 44px (mobile) / 40px (desktop). Có trạng thái loading (spinner + giữ nguyên độ rộng), disabled, focus ring 2px brand offset 2px. |
| Input / Select / Textarea | Nhãn luôn hiển thị phía trên (không chỉ placeholder), lỗi tiếng Việt dưới ô, viền danger + `aria-invalid`. |
| Panel | Nền surface, viền 1px, bo 14px, padding 20–24px. |
| Status badge | Pill nhỏ, chữ + icon, nền soft của hue tương ứng. |
| Data list/table | Hàng cao 52px, divider 1px, số canh phải tabular. Mobile chuyển thành danh sách 2 dòng. |
| Sheet/Drawer | Trên mobile chi tiết booking mở bằng bottom sheet; desktop là side panel bên phải (không dùng modal cho tác vụ không cần chặn). |
| Dialog | Chỉ cho xác nhận phá hủy (hủy booking, xóa dịch vụ). |
| Empty state | 1 câu giải thích + 1 hành động chính. Không minh họa trang trí. |
| Toast | Góc dưới, tự ẩn 4s, có nút đóng; lỗi không tự ẩn. |
| Icon | Bộ **Lucide** duy nhất, stroke 1.75, 18–20px. Không emoji. |

## 7. Sitemap
```
Công khai
├── /                         Landing BookEasy (giới thiệu + đăng ký)
├── /b/{slug}                 Trang đặt lịch cơ sở (wizard 5 bước)
├── /b/{slug}/booking/{code}  Kết quả đặt lịch / thanh toán VietQR
└── /b/{slug}/tra-cuu         Tra cứu & hủy lịch (mã + SĐT)

Xác thực
├── /login
└── /register                 Đăng ký cơ sở mới

Quản trị cơ sở (OWNER/STAFF)   /app
├── /app                      Tổng quan hôm nay
├── /app/calendar             Lịch (ngày/tuần, theo nhân viên, kéo thả)
├── /app/bookings             Danh sách booking (lọc, tìm)
├── /app/customers            Khách hàng  → /app/customers/:id
├── /app/services             Dịch vụ & danh mục          (OWNER)
├── /app/staff                Nhân viên, ca, ngày nghỉ    (OWNER)
├── /app/reports              Báo cáo                     (OWNER)
└── /app/settings             Cơ sở, chi nhánh, giờ mở cửa, ngày nghỉ, thanh toán, thương hiệu (OWNER)

Super admin                   /admin
└── /admin                    Danh sách cơ sở, khóa/mở
```

## 8. Mô tả màn hình

### 8.1 Trang đặt lịch công khai `/b/{slug}` (mobile-first, 360–430px)
- **Header**: logo cơ sở (40px, bo 10px) + tên (Fraunces 1.375rem) + địa chỉ/SĐT dòng phụ. Không hero ảnh lớn – khách đến để đặt lịch.
- **Thanh tiến trình**: "Dịch vụ · Nhân viên · Thời gian · Thông tin" dạng chữ, bước hiện tại đậm + gạch dưới brand (không số 01/02).
- **Bước 1 – Dịch vụ**: chọn chi nhánh (segmented nếu ≤ 3). Danh mục là tiêu đề nhóm, dịch vụ là hàng danh sách: tên, mô tả 1 dòng, "60 phút · 350.000 ₫"; chạm để chọn (radio, viền brand + nền brand-soft).
- **Bước 2 – Nhân viên**: hàng đầu "Bất kỳ ai – hệ thống chọn người phù hợp"; sau đó avatar chữ cái (màu lịch của nhân viên), tên, chức danh.
- **Bước 3 – Thời gian**: dải ngày cuộn ngang 14 ngày (Th 2, 10/11), slot chia nhóm Sáng / Chiều / Tối dạng lưới 3–4 cột; ngày không còn slot hiển thị mờ + "Hết chỗ".
- **Bước 4 – Thông tin**: họ tên, SĐT, email (tùy chọn), ghi chú. Tóm tắt booking cố định phía trên nút.
- **Thanh hành động dính đáy**: tổng tiền + nút "Tiếp tục"/"Xác nhận đặt lịch" (full width).
- **Kết quả**: tick vẽ nét, mã booking monospace lớn có nút sao chép, chi tiết, nếu cần thanh toán: QR VietQR 240px, số tiền, ngân hàng, nội dung CK, đếm ngược 30 phút, tự kiểm tra trạng thái mỗi 5s.

### 8.2 Đăng nhập / Đăng ký
Bố cục 2 cột trên desktop (trái: form; phải: một đoạn văn ngắn về sản phẩm trên nền sunken, không ảnh minh họa), 1 cột trên mobile.

### 8.3 App shell quản trị
- Desktop: sidebar trái 240px (logo cơ sở, menu icon + chữ, người dùng ở đáy). Mobile: top bar + bottom nav 5 mục (Hôm nay, Lịch, Booking, Khách, Thêm).
- Nội dung tối đa 1280px, tiêu đề trang Be Vietnam Pro 600 1.75rem + hành động chính bên phải.

### 8.4 Tổng quan hôm nay `/app`
Câu tóm tắt dạng văn ("Hôm nay có **12 lịch hẹn**, 3 đang chờ thanh toán") thay cho dãy thẻ chỉ số; danh sách "Sắp tới" theo giờ với nút Check-in nhanh; khối "Cần xử lý" (chờ thanh toán quá hạn, chưa đánh dấu kết quả).

### 8.5 Lịch `/app/calendar`
- Toolbar: ← Hôm nay →, chọn ngày, Ngày/Tuần, lọc chi nhánh, nút "Tạo lịch hẹn".
- Lưới thời gian 07:00–22:00, 1 giờ = 64px, vạch 15 phút mờ; đường "bây giờ" màu danger.
- Chế độ Ngày: cột = nhân viên (header avatar + tên). Chế độ Tuần: cột = ngày, lọc 1 nhân viên hoặc tất cả.
- Khối booking: nền màu nhạt theo màu nhân viên + chữ ink; giờ, tên khách, dịch vụ; trạng thái bằng icon. Kéo thả để đổi giờ/nhân viên (bước 15 phút), xem trước bằng bóng. Mobile: chế độ danh sách theo giờ thay vì lưới, đổi giờ qua form.
- Chạm khối → side panel chi tiết: khách, SĐT (gọi được), dịch vụ, giá, thanh toán, ghi chú, hành động theo trạng thái.

### 8.6 Booking, Khách hàng, Dịch vụ, Nhân viên
Bảng dữ liệu + bộ lọc dạng chip; form trong side panel. Nhân viên: tab "Thông tin", "Ca làm việc" (lưới 7 ngày, thêm khoảng giờ), "Ngày nghỉ".

### 8.7 Báo cáo `/app/reports`
Chọn khoảng ngày (7 ngày, 30 ngày, tháng này, tùy chọn). Một dòng số liệu chính dạng văn bản có cấu trúc (doanh thu, booking, no-show %), biểu đồ cột doanh thu theo ngày (SVG tự vẽ, 1 màu brand), hai bảng xếp hạng dịch vụ & nhân viên.

### 8.8 Cài đặt `/app/settings`
Các mục theo tab: Thông tin cơ sở · Thương hiệu (logo, màu, xem trước trang công khai) · Chi nhánh & giờ mở cửa · Ngày nghỉ · Thanh toán · Đặt lịch (thời gian đặt trước tối thiểu, số ngày mở đặt).

### 8.9 Super admin `/admin`
Bảng cơ sở: tên, slug (link), chủ, số booking, ngày tạo, trạng thái; nút Tạm khóa/Kích hoạt có dialog xác nhận.

## 9. Kiểm tra chất lượng (trước khi merge UI)
- Tương phản chữ ≥ 4.5:1; focus nhìn thấy được; điều hướng bàn phím đầy đủ.
- Mỗi màn hình có trạng thái: loading (skeleton giữ bố cục), empty, error (có "Thử lại"), disabled.
- Kiểm tra 360px, 768px, 1280px; không cuộn ngang.
