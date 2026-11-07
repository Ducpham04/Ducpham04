# BookEasy – Product Requirements Document (PRD)

> Phiên bản: 0.1 (MVP) · Ngày: 05/11/2026 · Chủ sở hữu: Product Owner (BookEasy)

## 1. Bối cảnh & mục tiêu

Các cơ sở dịch vụ nhỏ ở Việt Nam (spa, salon tóc/nail, phòng gym/PT, phòng khám nha/da liễu) phần lớn nhận lịch qua Zalo, Facebook Messenger và điện thoại, ghi tay vào sổ hoặc Google Sheet. Hệ quả: trùng lịch, quên lịch, khách bùng hẹn (no-show) 10–25%, chủ không biết dịch vụ/nhân viên nào mang lại doanh thu.

**BookEasy** là SaaS đặt lịch đa cơ sở (multi-tenant). Mỗi cơ sở có trang đặt lịch công khai riêng tại `/b/{slug}` để dán vào bio Facebook/Zalo/Instagram/Google Maps.

### Mục tiêu kinh doanh (12 tháng)
| Mục tiêu | Chỉ số |
|---|---|
| Có khách trả phí | 300 cơ sở trả phí |
| Giảm no-show cho cơ sở | No-show giảm ≥ 30% sau 2 tháng dùng nhắc lịch + đặt cọc |
| Đặt lịch tự phục vụ | ≥ 60% booking đến từ trang công khai thay vì nhập tay |
| Kích hoạt nhanh | Cơ sở mới cấu hình xong và nhận booking đầu tiên trong < 30 phút |

### Mục tiêu sản phẩm MVP
1. Khách đặt lịch trên điện thoại trong < 60 giây, không cần tạo tài khoản.
2. Không bao giờ có 2 booking trùng giờ cho cùng nhân viên (bảo đảm ở tầng DB).
3. Chủ cơ sở quản lý lịch trực quan (lịch ngày/tuần, cột theo nhân viên, kéo thả).
4. Thu cọc/trả trước bằng VietQR – kênh thanh toán phổ biến nhất ở VN, phí 0đ.
5. Nhắc lịch tự động (email; Zalo ZNS sẵn interface).

## 2. Persona

| Persona | Mô tả | Nỗi đau | Cần |
|---|---|---|---|
| **Chị Hạnh – Chủ spa (OWNER)** | 38 tuổi, 2 chi nhánh spa ở Q.3 & Q.7 TP.HCM, 9 kỹ thuật viên. Dùng điện thoại là chính. | Trùng lịch, khách bùng hẹn, không biết doanh thu theo nhân viên. | Lịch tập trung, đặt cọc, báo cáo đơn giản. |
| **Lan – Kỹ thuật viên (STAFF)** | 24 tuổi, làm ca. | Không biết hôm nay có khách nào, khách đến lúc nào. | Xem lịch của mình, check-in, đánh dấu hoàn thành. |
| **Minh – Khách hàng (CUSTOMER)** | 29 tuổi, nhân viên văn phòng, đặt lịch buổi tối qua điện thoại. | Nhắn tin chờ trả lời lâu, không biết giờ nào trống. | Xem giờ trống thật, đặt nhanh, được nhắc lịch. |
| **Admin BookEasy (SUPER_ADMIN)** | Đội vận hành BookEasy. | Cần quản lý các cơ sở đăng ký, khóa cơ sở vi phạm/không trả phí. | Danh sách cơ sở, kích hoạt/tạm khóa. |

## 3. Vai trò & quyền
- **SUPER_ADMIN**: quản lý toàn bộ cơ sở (tenant), xem thống kê tổng.
- **OWNER**: toàn quyền trong cơ sở của mình (cấu hình, dịch vụ, nhân viên, lịch, khách, báo cáo, thanh toán).
- **STAFF**: xem lịch của bản thân, đổi trạng thái booking của mình (check-in, hoàn thành, no-show), xem thông tin khách của booking mình phục vụ.
- **CUSTOMER**: không có tài khoản. Đặt lịch bằng SĐT (+ email tùy chọn). Tra cứu/hủy booking bằng **mã booking + SĐT** (xác thực sở hữu).

## 4. User stories & Acceptance criteria (MVP)

### EPIC A – Tài khoản & đa cơ sở
- **A1. Đăng ký cơ sở** – *Là chủ cơ sở, tôi muốn tự đăng ký để dùng thử ngay.*
  - AC: nhập tên cơ sở, slug (tự gợi ý từ tên, bỏ dấu), họ tên, email, mật khẩu (≥ 8 ký tự, có chữ & số) → tạo tenant + OWNER + 1 chi nhánh mặc định + giờ mở cửa mặc định 08:00–20:00; slug trùng → báo lỗi tiếng Việt.
- **A2. Đăng nhập** – AC: email + mật khẩu → access token (15 phút) + refresh token (7 ngày, xoay vòng). Sai thông tin → "Email hoặc mật khẩu không đúng". Tài khoản/cơ sở bị khóa → không đăng nhập được.
- **A3. Super admin quản lý cơ sở** – AC: xem danh sách cơ sở (tên, slug, chủ, số booking, trạng thái), tạm khóa/kích hoạt. Cơ sở bị khóa → trang `/b/{slug}` báo "tạm ngưng nhận lịch".
- **A4. Cô lập dữ liệu** – AC: người dùng cơ sở A không đọc/ghi được bất kỳ dữ liệu nào của cơ sở B (trả 404).

### EPIC B – Cấu hình cơ sở
- **B1. Thông tin & thương hiệu** – AC: sửa tên, mô tả, SĐT, địa chỉ, logo (URL hoặc upload ảnh ≤ 2MB, png/jpg/webp), màu thương hiệu (hex). Trang công khai dùng logo + màu ngay.
- **B2. Chi nhánh** – AC: thêm/sửa/ngừng chi nhánh (tên, địa chỉ, SĐT). Mỗi chi nhánh có giờ mở cửa theo thứ trong tuần (có thể nghỉ cả ngày).
- **B3. Ngày nghỉ** – AC: thêm ngày nghỉ lễ (ví dụ Tết) cho toàn cơ sở hoặc 1 chi nhánh → không hiển thị slot ngày đó.
- **B4. Thanh toán** – AC: chọn chế độ: Không thu trước / Đặt cọc X% / Trả trước 100%; nhập ngân hàng (mã VietQR), số TK, tên chủ TK.

### EPIC C – Dịch vụ
- **C1. CRUD dịch vụ** – AC: tên, danh mục, mô tả, thời lượng (bội số 15 phút, 15–480), giá (VNĐ, ≥ 0), trạng thái hiển thị; chọn nhân viên thực hiện được. Dịch vụ ẩn không hiện trên trang công khai.
- **C2. Danh mục** – AC: thêm/sửa/xóa danh mục, sắp xếp thứ tự.

### EPIC D – Nhân viên
- **D1. CRUD nhân viên** – AC: tên, chức danh, SĐT, email, chi nhánh, ảnh, màu lịch; tùy chọn tạo tài khoản đăng nhập STAFF.
- **D2. Ca làm việc** – AC: đặt ca theo thứ (nhiều ca/ngày, ví dụ 08:00–12:00 và 14:00–20:00).
- **D3. Ngày nghỉ nhân viên** – AC: thêm khoảng nghỉ (từ ngày–đến ngày, lý do) → không có slot trong khoảng đó.

### EPIC E – Trang đặt lịch công khai `/b/{slug}`
- **E1. Chọn dịch vụ** – AC: hiển thị theo danh mục, có giá, thời lượng; chọn chi nhánh nếu > 1 chi nhánh.
- **E2. Chọn nhân viên** – AC: danh sách nhân viên làm được dịch vụ ở chi nhánh + lựa chọn "Bất kỳ ai".
- **E3. Chọn ngày giờ** – AC: 14 ngày tới; slot bước 15 phút trong giờ mở cửa ∩ ca nhân viên − ngày nghỉ − booking đã có; ngày hôm nay bỏ qua slot đã qua + thời gian tối thiểu đặt trước (mặc định 60 phút). "Bất kỳ ai" = hợp các nhân viên; khi đặt hệ thống tự gán nhân viên còn trống (ít booking nhất trong ngày).
- **E4. Thông tin khách** – AC: họ tên (bắt buộc), SĐT VN hợp lệ (bắt buộc), email (tùy chọn), ghi chú.
- **E5. Xác nhận** – AC: tạo booking mã dạng `BE` + 6 ký tự; nếu slot vừa bị người khác đặt → báo lỗi 409 "Khung giờ vừa có người đặt, vui lòng chọn giờ khác" và tải lại slot. Nếu cơ sở yêu cầu cọc/trả trước → hiện mã VietQR với số tiền và nội dung CK = mã booking, trạng thái "Chờ thanh toán".
- **E6. Tra cứu/hủy** – AC: nhập mã booking + SĐT → xem chi tiết, hủy nếu còn ≥ 2 giờ trước giờ hẹn.
- **E7. Chống đặt trùng** – AC: 2 request đồng thời cùng nhân viên/khung giờ → đúng 1 thành công (ràng buộc UNIQUE ở DB).

### EPIC F – Thanh toán
- **F1. VietQR** – AC: ảnh QR từ `img.vietqr.io` với `amount`, `addInfo={mã booking}`, `accountName`.
- **F2. Webhook** – AC: `POST /api/webhooks/payments/{provider}` (sepay | casso | mock) có kiểm tra secret; tìm mã booking trong nội dung CK; số tiền ≥ số cần thu → booking chuyển "Đã thanh toán/Đã cọc", trạng thái booking `PENDING_PAYMENT` → `CONFIRMED`; idempotent theo mã giao dịch.
- **F3. Xác nhận thủ công** – AC: chủ cơ sở bấm "Đã nhận tiền" trên booking.
- **F4. Hết hạn** – AC: booking chờ thanh toán quá 30 phút tự hủy, giải phóng slot.

### EPIC G – Quản lý lịch
- **G1. Calendar** – AC: xem theo ngày (cột = nhân viên) và tuần (cột = ngày); lọc chi nhánh/nhân viên; màu theo trạng thái.
- **G2. Kéo thả đổi giờ** – AC: kéo booking sang giờ/nhân viên khác → kiểm tra trùng → lưu; trùng thì hoàn tác + báo lỗi.
- **G3. Trạng thái** – AC: `PENDING_PAYMENT → CONFIRMED → CHECKED_IN → COMPLETED`; `CANCELLED`, `NO_SHOW` từ trạng thái phù hợp; chuyển sai luồng → 400.
- **G4. Tạo booking tại quầy** – AC: chủ/nhân viên tạo booking thủ công cho khách gọi điện.

### EPIC H – Nhắc lịch
- **H1.** Email xác nhận ngay khi booking được xác nhận.
- **H2.** Scheduler mỗi phút: gửi nhắc trước 24h và 2h (mỗi loại tối đa 1 lần/booking), chỉ cho booking `CONFIRMED`.
- **H3.** Zalo ZNS: interface `NotificationChannel` + mock ghi log; lưu nhật ký gửi.

### EPIC I – Khách hàng (CRM nhẹ)
- **I1.** Danh sách khách theo cơ sở (tìm theo tên/SĐT), số lần đến (COMPLETED), số lần no-show, tổng chi tiêu, lần đến gần nhất.
- **I2.** Chi tiết khách: lịch sử booking, ghi chú nội bộ (sửa được).

### EPIC J – Báo cáo
- **J1.** Theo khoảng ngày: doanh thu (booking hoàn thành), số booking, tỷ lệ no-show, tỷ lệ hủy, doanh thu theo ngày (biểu đồ), top dịch vụ, top nhân viên.

## 5. Yêu cầu phi chức năng
- Mobile-first, tiếng Việt, định dạng tiền `350.000 ₫`, ngày `dd/MM/yyyy`, múi giờ `Asia/Ho_Chi_Minh`.
- API p95 < 300ms với 10k booking/cơ sở; tính slot < 200ms.
- Bảo mật: BCrypt, JWT HS256 secret từ env, CORS giới hạn, validate mọi input, không lộ stacktrace, rate-limit endpoint công khai tạo booking.
- Một lệnh `docker compose up` chạy toàn bộ.

## 6. Phạm vi
### MVP (bản này)
EPIC A–J như trên.
### Sau MVP
- Gói thành viên / thẻ liệu trình (buổi tập, liệu trình 10 buổi).
- Voucher / mã giảm giá.
- Đánh giá sau dịch vụ (gửi link sau khi COMPLETED).
- Widget nhúng đặt lịch vào website khác (`<script>`/iframe).
- Tích hợp thật SePay/Casso, Zalo ZNS, SMS brandname; OTP SĐT.
- Thanh toán gói SaaS, đa ngôn ngữ, app mobile cho nhân viên.
