# DESIGN – EduCenter

> Vai trò: UI/UX Designer. Nguyên tắc nền: **tác vụ trước, trang trí sau**, mobile-first, tiếng Việt.
> Tham chiếu tay nghề: [pbakaus/impeccable](https://github.com/pbakaus/impeccable) (craft-floor, typeset, colorize, layout, animate) – áp dụng như một "sàn chất lượng", không phải phong cách.
> Bản này **tinh chỉnh** bản sắc của code cũ (nền xanh dương rất nhạt, nhấn xanh dương, tương phản cao) chứ không làm lại từ đầu → giữ nhận diện quen thuộc cho người dùng cũ.

## 1. Định vị hình ảnh

- **Register**: sản phẩm vận hành (operational dashboard), không phải landing page.
- **Cảm giác**: rõ ràng · tin cậy · nhẹ đầu. Giống một sổ cái ngăn nắp hơn là một ứng dụng "công nghệ".
- **Bối cảnh dùng**: giáo vụ ngồi quầy (laptop, ánh sáng văn phòng), giáo viên đứng lớp (điện thoại, 1 tay), phụ huynh buổi tối (điện thoại). → **giao diện sáng** là mặc định (chọn theo bối cảnh dùng, không theo thể loại).

### Anti-references (không làm)
- Gradient tím-xanh, chữ gradient, glassmorphism trang trí, nền lưới/chấm.
- Card lồng card; lưới các card icon + tiêu đề + đoạn chữ giống hệt nhau làm bố cục chính.
- Nhãn "eyebrow" nhỏ viết hoa phía trên tiêu đề; đánh số 01/02/03 cho các mục không có thứ tự.
- Viền trái màu dày > 1px trên card/alert; bóng đổ lệch cứng kiểu brutalist.
- Emoji thay icon; mỗi tiêu đề một ô icon bo tròn.
- Modal cho việc không cần chặn ngữ cảnh (dùng trang hoặc panel trượt).
- Chỉ dùng màu để báo trạng thái (luôn kèm chữ + icon).

## 2. Design tokens

### 2.1 Màu (OKLCH, neutral ánh xanh để thống nhất với màu thương hiệu)

| Vai trò | Token | Giá trị | Dùng cho |
| --- | --- | --- | --- |
| Canvas | `--bg` | `oklch(0.984 0.005 250)` | Nền trang |
| Surface | `--surface` | `oklch(1 0 0)` | Card, bảng, form |
| Surface muted | `--surface-2` | `oklch(0.962 0.01 250)` | Header bảng, vùng phụ, hover dòng |
| Ink | `--ink` | `oklch(0.22 0.025 258)` | Chữ chính (≈ 15:1 trên surface) |
| Ink muted | `--ink-2` | `oklch(0.47 0.02 258)` | Chữ phụ (≥ 4.6:1) |
| Line | `--line` | `oklch(0.91 0.01 250)` | Viền 1px, divider |
| Primary | `--primary` | `oklch(0.52 0.15 258)` | Hành động chính, link, focus |
| Primary strong | `--primary-strong` | `oklch(0.43 0.15 258)` | Hover/pressed |
| Primary soft | `--primary-soft` | `oklch(0.955 0.025 258)` | Nav đang chọn, nền chip |
| Success | `--ok` / `--ok-soft` | `oklch(0.47 0.12 155)` / `oklch(0.955 0.035 155)` | Có mặt, đã thu đủ |
| Warning | `--warn` / `--warn-soft` | `oklch(0.56 0.13 70)` / `oklch(0.962 0.04 80)` | Đi muộn, thu một phần, bảo lưu |
| Danger | `--danger` / `--danger-soft` | `oklch(0.52 0.18 25)` / `oklch(0.962 0.03 25)` | Vắng, quá hạn, xóa |
| Info | `--info` / `--info-soft` | `oklch(0.5 0.13 235)` / `oklch(0.955 0.03 235)` | Học thử, sắp mở |

Quy tắc: màu nhấn mạnh nhất (primary) **sở hữu một vai trò** – hành động chính + điều hướng đang chọn – không rải lung tung. Dữ liệu biểu đồ: primary cho chuỗi chính, `--ink-2` 40% cho chuỗi so sánh.

### 2.2 Chữ

- **Một họ chữ: "Be Vietnam Pro"** (Google Fonts, 400/500/600/700). Lý do: thiết kế riêng cho tiếng Việt, dấu thanh không đè nhau ở cỡ nhỏ, không phải "Inter cho mọi thứ". Fallback: `system-ui, "Segoe UI", Roboto, sans-serif`.
- Số liệu dùng `font-variant-numeric: tabular-nums` (tiền, giờ, %) – không dùng monospace làm "trang phục".
- Thang cỡ cố định (không fluid trong app):

| Vai trò | Cỡ / line-height | Weight | Tracking |
| --- | --- | --- | --- |
| Page title | 24px mobile / 28px desktop · 1.2 | 700 | -0.02em |
| Section title | 18px · 1.35 | 600 | -0.01em |
| Body | 16px mobile / 15px dense desktop · 1.55 | 400 | 0 |
| Label / table head | 13px · 1.4 | 500 | 0 |
| Caption | 12.5px · 1.4 | 400 | 0 |
| KPI number | 26–30px · 1.1 | 700 | -0.02em |

- `text-wrap: balance` cho tiêu đề, `text-wrap: pretty` cho đoạn văn; đoạn văn dài tối đa 70ch.

### 2.3 Spacing, bo góc, độ nổi

- Thang 4px: `4 8 12 16 20 24 32 40 48 64`. Nhịp: **khoảng trên tiêu đề > khoảng dưới tiêu đề**; nhóm theo khoảng cách trước, thêm khung sau.
- Bo góc 2 bậc: control 8px (`rounded-lg`), container 12px (`rounded-xl`); pill chỉ cho chip trạng thái.
- Độ nổi khai báo **một lần**: card dùng viền 1px `--line`, **không** kèm bóng rộng. Chỉ phần tử nổi (dropdown, toast, bottom sheet) dùng `--shadow-pop: 0 8px 24px oklch(0.2 0.03 258 / 0.12)`.
- Vùng chạm tối thiểu 44×44px trên mobile (nút điểm danh 48px).

### 2.4 Chuyển động
- 120ms phản hồi (hover, nhấn); 200ms thay đổi trạng thái (panel, tab); 280ms sheet/drawer. Easing `cubic-bezier(0.16, 1, 0.3, 1)`; thoát nhanh hơn vào.
- Không có hiệu ứng khi tải trang. `prefers-reduced-motion` → bỏ dịch chuyển, giữ đổi màu.

### 2.5 Trạng thái tương tác bắt buộc
Mỗi control có: default, hover, focus-visible (ring 2px primary, offset 2px), active, disabled, loading. Mỗi danh sách có: loading (skeleton dòng), empty (1 câu nói rõ + hành động tiếp theo), error (nói lỗi gì + cách khắc phục).

## 3. Component

| Component | Mô tả |
| --- | --- |
| `Button` | Biến thể `primary` (nền primary, chữ trắng), `secondary` (viền line, nền surface), `ghost`, `danger`. Cao 40px desktop / 44px mobile. Nhãn là động từ: "Lưu học viên", "Ghi nhận thanh toán". |
| `Input / Select / Textarea` | Label luôn hiển thị phía trên (không dùng placeholder thay label), lỗi hiện dưới trường bằng chữ danger + icon. |
| `Badge (StatusChip)` | Pill nền `-soft`, chữ màu đậm, có chấm/biểu tượng hình dạng khác nhau. Map enum → tiếng Việt tập trung 1 file. |
| `Card` | Surface + viền 1px + bo 12px, padding 16/20px. Không lồng nhau. |
| `DataTable` | Desktop: bảng có header `--surface-2`, dòng cao 48px, divider 1px. Mobile: tự chuyển thành danh sách dòng (tên + 2 thông tin + chip) – không cuộn ngang. |
| `PageHeader` | Tiêu đề + mô tả 1 dòng + hành động chính bên phải (mobile: nút cố định đáy). |
| `Stat` | Con số + nhãn + so sánh (chữ, không sparkline trang trí). Dashboard dùng **một dải số liệu** trong 1 card chia cột bằng divider, không phải 4 card giống nhau. |
| `Sheet` | Panel trượt từ phải (desktop) / từ đáy (mobile) cho form tạo nhanh. |
| `Tabs` | Gạch chân 2px primary cho tab đang chọn. |
| `EmptyState` | Không minh họa; 1 dòng tiêu đề + 1 dòng hướng dẫn + nút. |
| `Toast` | Góc dưới, 4 giây, có nút đóng; lỗi không tự tắt. |
| `AttendanceRow` | Tên HV + 4 nút phân đoạn (C · M · P · K) cao 48px, nhãn đầy đủ trên ≥ 400px. |
| `QrPanel` | Ảnh VietQR 220px + ngân hàng, STK, chủ TK, số tiền, nội dung CK – có nút sao chép từng dòng. |

Icon: **lucide-react**, nét 1.75px, cỡ 18/20px, một bộ duy nhất.

## 4. Sitemap

```
/login
/portal                     (STUDENT) Tổng quan · Lịch · Điểm danh · Học phí
/app
├── /dashboard              Tổng quan (ADMIN, STAFF) | Lịch dạy hôm nay (TEACHER)
├── /students               Danh sách · /students/new · /students/:id (Hồ sơ, Phụ huynh, Lớp, Chuyên cần, Học phí)
├── /classes                Danh sách · /classes/:id (Thông tin, Lịch tuần, Học viên, Buổi học)
├── /schedule               Lịch tuần / tháng, lọc GV/phòng
├── /attendance             Buổi dạy hôm nay → /attendance/:sessionId (điểm danh mobile)
├── /billing                Hóa đơn · Công nợ · Gói học phí · /billing/invoices/:id
├── /reports                Doanh thu, công nợ, học viên mới, chuyên cần + Xuất Excel
├── /catalog                Khóa học · Phòng học
├── /staff                  Nhân viên (ADMIN)
├── /settings               Trung tâm, ngân hàng (VietQR), nhắc nhở & nhật ký gửi (ADMIN)
├── /account                Đổi mật khẩu
└── /tenants                Trung tâm (SUPER_ADMIN)
```

Điều hướng: desktop sidebar trái 248px (nhóm: Vận hành · Tài chính · Quản trị); mobile thanh dưới 5 mục theo vai trò (TEACHER: Hôm nay, Lịch, Điểm danh, Tài khoản) + menu "Thêm".

## 5. Mô tả màn hình

1. **Đăng nhập** – Cột trái (desktop) giới thiệu 1 câu + 3 lợi ích dạng danh sách chữ; cột phải form. Mobile chỉ form. Có khối "Tài khoản demo" bấm để điền nhanh.
2. **Tổng quan (ADMIN/STAFF)** – Dải 4 số liệu (Doanh thu tháng, Công nợ, HV mới, Chuyên cần 30 ngày) trong 1 card; dưới là biểu đồ cột doanh thu 12 tháng (2/3) + "Buổi học hôm nay" (1/3); cuối là "Hóa đơn quá hạn" 5 dòng.
3. **Hôm nay (TEACHER)** – Danh sách buổi dạy hôm nay theo giờ, mỗi dòng có nút "Điểm danh" lớn; dưới là lịch 7 ngày tới.
4. **Học viên** – Thanh tìm kiếm + lọc trạng thái dạng tab (Tất cả · Đang học · Bảo lưu · Đã nghỉ · Học thử). Bảng: Mã, Họ tên (+SĐT), Phụ huynh, Lớp đang học, Trạng thái.
5. **Hồ sơ học viên** – Header tên + chip trạng thái + hành động (Sửa, Đổi trạng thái, Lập hóa đơn). Tabs: Thông tin & phụ huynh · Lịch sử lớp · Chuyên cần · Học phí · Tài khoản cổng.
6. **Lớp học** – Danh sách lớp: mã, tên, khóa, GV, phòng, sĩ số `18/20` kèm thanh mảnh, lịch tóm tắt "T2·T4·T6 18:00".
7. **Chi tiết lớp** – Thông tin, **Lịch tuần** (thêm/xóa khung giờ, lỗi trùng hiển thị inline nêu rõ lớp bị trùng), Học viên (ghi danh từ ô tìm kiếm), Buổi học (danh sách + trạng thái + link điểm danh).
8. **Lịch** – Toggle Tuần/Tháng, mũi tên chuyển kỳ, lọc GV/phòng. Tuần: lưới 7 cột theo giờ (desktop), danh sách theo ngày (mobile). Tháng: ô ngày với tối đa 3 buổi + "+n".
9. **Điểm danh** – Header lớp, giờ, phòng; nút "Tất cả có mặt"; danh sách AttendanceRow; thanh lưu cố định đáy hiển thị đếm "22 có mặt · 1 muộn · 2 vắng".
10. **Học phí** – Tabs Hóa đơn / Công nợ / Gói học phí. Hóa đơn: lọc trạng thái, tháng. Công nợ: tổng nợ + quá hạn ở đầu, danh sách HV còn nợ.
11. **Chi tiết hóa đơn** – Trái: dòng hóa đơn, tổng, đã thu, còn lại, lịch sử thanh toán (mỗi dòng có "Phiếu thu PDF"). Phải: QrPanel + form ghi nhận thanh toán.
12. **Báo cáo** – Chọn năm; biểu đồ doanh thu, HV mới; bảng chuyên cần theo lớp; 3 nút xuất Excel.
13. **Cài đặt** – Thông tin trung tâm (tên, địa chỉ, SĐT, email), Ngân hàng VietQR (chọn ngân hàng, STK, chủ TK, xem thử QR), Nhắc nhở (nút chạy ngay, nhật ký gửi gần nhất, kênh Email/Zalo mock).
14. **Cổng phụ huynh** – Mobile-first một cột: chào + tên HV, thẻ "Buổi học tiếp theo", tabs Lịch / Điểm danh / Học phí; học phí còn nợ hiển thị QR ngay.
15. **Trung tâm (SUPER_ADMIN)** – Bảng trung tâm (tên, slug, gói, số HV, trạng thái) + tạo trung tâm (kèm tài khoản chủ).

## 6. Văn phong (UX writing)
- Xưng hô trung tính, ngắn: "Lưu", "Đã lưu điểm danh lớp IELTS 5.5 – K12".
- Lỗi nói rõ vấn đề + cách xử lý: "Phòng 201 đã có lớp TOEIC-A2 vào Thứ 4, 18:00–19:30. Chọn phòng khác hoặc đổi giờ."
- Tiền: `1.250.000 ₫`; ngày `05/11/2026`; thứ "T2…CN".
