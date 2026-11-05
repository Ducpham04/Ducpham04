# PRD – Zalo Mini App Starter Kit

> Phiên bản: 1.0 (MVP) · Ngày: 2026-11-05 · Vai trò: Product Owner / BA

## 1. Bối cảnh & mục tiêu

Zalo Mini App (ZMA) là kênh tiếp cận ~75 triệu người dùng Zalo, nhưng mỗi dự án mới đều phải làm lại cùng một khối việc: đăng nhập Zalo, xin quyền số điện thoại / vị trí, giải mã token phía server, lớp gọi API có refresh token, upload ảnh, gửi ZNS/OA, thanh toán Checkout SDK, trang quản trị… Thường mất **3–6 tuần** trước khi viết được dòng nghiệp vụ đầu tiên.

**Starter Kit** là một bộ khung *production-ready* để:

1. **Bán** như template thương mại cho dev/agency (gói Personal / Agency / Extended).
2. **Nhận dự án**: dev đổi cấu hình → có ngay Mini App + backend + admin chạy được, demo được cho khách **trên trình duyệt** kể cả khi chưa có App ID.

### Mục tiêu đo được

| # | Mục tiêu | Chỉ số |
|---|----------|--------|
| G1 | Chạy toàn bộ hệ thống bằng 1 lệnh | `docker compose up` → 3 URL hoạt động trong < 5 phút |
| G2 | Demo không cần App ID | Mini App chạy ở **chế độ Mock** trên Chrome, đủ luồng chính |
| G3 | Đổi thương hiệu nhanh | Đổi tên/màu/logo/App ID trong ≤ 5 file cấu hình, ≤ 30 phút |
| G4 | Chất lượng đủ để bán | CI xanh, test unit + integration + e2e cho luồng chính, tài liệu đầy đủ |

## 2. Persona

| Persona | Mô tả | Nhu cầu chính |
|---------|-------|---------------|
| **Anh Minh – Dev freelancer** (người mua) | 3 năm React/Java, nhận dự án Mini App cho SME | Có khung chạy ngay, code sạch, tài liệu rõ, ít "bẫy" Zalo |
| **Chị Lan – Chủ agency** (người mua) | Agency 10 người, làm Mini App cho chuỗi bán lẻ / phường xã | Demo cho khách nhanh, chuẩn hoá codebase giữa các dự án |
| **Bác Hùng – Người dân / khách hàng** (end-user) | Dùng Zalo hằng ngày, không rành công nghệ | Đọc tin, xem sản phẩm, gửi phản ánh có ảnh, xem lại lịch sử |
| **Cô Hạnh – Cán bộ quản trị** (end-user admin) | Nhân viên văn phòng / cán bộ phường | Đăng tin, quản lý sản phẩm, xử lý phản ánh, phản hồi người dân |
| **Tuấn – Biên tập viên** | Nhân viên nội dung | Chỉ quản lý tin tức & sản phẩm, không xem dữ liệu người dùng |

## 3. User stories & Acceptance Criteria

### EPIC A – Xác thực

**A1. Đăng nhập bằng Zalo** – *Là người dùng, tôi muốn mở Mini App là được đăng nhập ngay bằng tài khoản Zalo.*
- AC1: Mini App gọi `getAccessToken()` (zmp-sdk) → gửi `POST /api/v1/auth/zalo`.
- AC2: Backend gọi Zalo Graph API `/me` để xác thực token, **tạo mới hoặc ghép** user theo `zaloId`.
- AC3: Backend trả `accessToken` (JWT, 15 phút) + `refreshToken` (30 ngày, lưu hash, xoay vòng).
- AC4: Token Zalo không hợp lệ → HTTP 401, mã lỗi `ZALO_TOKEN_INVALID`, Mini App hiển thị màn hình lỗi có nút "Thử lại".
- AC5: Ở chế độ Mock, đăng nhập thành công với user giả "Nguyễn Văn An".

**A2. Tự làm mới phiên** – *Là người dùng, tôi không muốn bị đăng xuất giữa chừng.*
- AC1: Khi API trả 401 `TOKEN_EXPIRED`, lớp API tự gọi `/auth/refresh` **một lần** (các request đồng thời chờ chung), rồi gửi lại request.
- AC2: Refresh thất bại → xoá phiên, đăng nhập Zalo lại.
- AC3: Refresh token cũ bị thu hồi sau khi dùng (rotation); dùng lại token đã thu hồi → 401.

**A3. Đăng nhập admin** – *Là quản trị viên, tôi đăng nhập web admin bằng email/mật khẩu.*
- AC1: `POST /api/v1/auth/login`; sai thông tin → 401 thông điệp chung "Email hoặc mật khẩu không đúng".
- AC2: Tài khoản role `USER` không vào được admin (403).

### EPIC B – Quyền & thông tin

**B1. Hồ sơ Zalo** – AC: tên + ảnh đại diện lấy từ Zalo hiển thị trong màn Hồ sơ; có thể sửa tên hiển thị.

**B2. Số điện thoại** – *Là người dùng, tôi muốn chia sẻ SĐT để cán bộ liên hệ lại.*
- AC1: Bấm "Liên kết số điện thoại" → `getPhoneNumber()` → nhận `token` → gửi `POST /api/v1/me/phone`.
- AC2: Backend gọi Zalo `/me/info` với `access_token` + `code` + `secret_key` để giải mã, lưu SĐT dạng `0xxxxxxxxx`.
- AC3: Người dùng **từ chối** → hiển thị giải thích + nút "Thử lại", không chặn các chức năng khác.

**B3. Vị trí** – *Là người dùng, tôi muốn đính kèm vị trí khi gửi phản ánh.*
- AC1: Bấm "Lấy vị trí hiện tại" → `getLocation()` → token → backend giải mã → điền toạ độ.
- AC2: Từ chối → cho phép nhập địa chỉ thủ công; phản ánh vẫn gửi được.

### EPIC C – Nội dung

**C1. Trang chủ** – AC: lời chào theo tên, lối tắt (Tin tức, Sản phẩm, Gửi phản ánh, Lịch sử), 3 tin mới nhất, 4 sản phẩm nổi bật; có skeleton khi tải, trạng thái lỗi có nút thử lại.
**C2. Danh sách tin tức** – AC: phân trang (infinite "Xem thêm"), lọc theo chuyên mục, tìm kiếm theo tiêu đề; trạng thái rỗng có minh hoạ.
**C3. Chi tiết tin** – AC: ảnh bìa, tiêu đề, ngày đăng, nội dung; tin chưa xuất bản → 404.
**C4. Danh sách & chi tiết sản phẩm** – AC: giá định dạng `120.000 ₫`, nút "Mua ngay".

### EPIC D – Phản ánh

**D1. Gửi phản ánh** – AC:
- Form: chuyên mục (bắt buộc), tiêu đề (5–150 ký tự), nội dung (10–2000), địa chỉ (tuỳ chọn), vị trí (tuỳ chọn), tối đa **5 ảnh**, mỗi ảnh ≤ 5MB, chỉ JPG/PNG/WEBP.
- Validate phía client (Zod) và server (Bean Validation + kiểm tra magic bytes của ảnh).
- Thành công → mã phản ánh dạng `PA-2026-000123`, chuyển tới chi tiết.
**D2. Lịch sử của tôi** – AC: danh sách phản ánh + đơn hàng của chính tôi, có badge trạng thái; không xem được của người khác (403/404).
**D3. Xử lý phản ánh (admin)** – AC: lọc theo trạng thái, xem ảnh, đổi trạng thái `NEW → IN_PROGRESS → RESOLVED/REJECTED`, nhập phản hồi; khi đổi trạng thái hệ thống **gửi thông báo** (ZNS/OA – mock) cho người dân.

### EPIC E – Thông báo
**E1.** AC: `NotificationGateway` có 2 implementation: `mock` (ghi log + bảng `notification_logs`) và `zns` (gọi API ZNS thật); chọn bằng cấu hình `app.notification.provider`. Admin xem được nhật ký gửi.

### EPIC F – Thanh toán
**F1. Mua sản phẩm** – AC:
- Mini App tạo đơn `POST /api/v1/orders` → backend tính tiền từ DB (không tin giá client), trả tham số Checkout kèm `mac` (HMAC-SHA256).
- Mini App gọi `Payment.createOrder(...)` (Checkout SDK); ở Mock hiển thị sheet thanh toán giả.
- Backend nhận callback `POST /api/v1/payments/zalo/notify`, xác minh `mac`, cập nhật đơn `PAID` (idempotent).
- Mock: endpoint `POST /api/v1/orders/{id}/mock-pay` chỉ bật khi `app.payment.provider=mock`.

### EPIC G – Admin web
**G1.** Dashboard số liệu: số người dùng, phản ánh theo trạng thái, đơn hàng, doanh thu.
**G2.** CRUD tin tức, sản phẩm (ADMIN, EDITOR). **G3.** Xử lý phản ánh, xem người dùng, đơn hàng, nhật ký thông báo (chỉ ADMIN).

### EPIC H – Chế độ Mock
**H1.** AC: `VITE_ZALO_MODE=mock|real|auto`; `auto` tự phát hiện có đang chạy trong Zalo không. Mock giả lập: `getAccessToken`, `getUserInfo`, `getPhoneNumber`, `getLocation`, `chooseImage`, `Payment.createOrder`, `openWebview`, kèm khả năng mô phỏng **từ chối quyền** (bảng điều khiển Mock).

## 4. Phạm vi

### MVP (bản này)
Toàn bộ EPIC A–H ở trên; docker compose; CI; tài liệu SETUP/CUSTOMIZE/PITFALLS; LICENSE thương mại; SELLING.md.

### Sau MVP
- Đa ngôn ngữ (vi/en), đa tenant (nhiều Mini App/1 backend).
- Lưu trữ ảnh S3/MinIO (đã có interface `StorageService`), CDN.
- Tự động làm mới OA access token (refresh token OA), webhook OA.
- Push "Follow OA", chia sẻ (share), mã QR, mini game, tích điểm/voucher.
- Rate limit, audit log, 2FA cho admin, quản lý nhiều admin.
- Báo cáo nâng cao, xuất Excel.

## 5. Phi chức năng
- Bảo mật: JWT HS256 secret ≥ 32 byte từ biến môi trường; BCrypt; không log token/SĐT đầy đủ; CORS whitelist.
- Hiệu năng: API p95 < 300ms với dữ liệu mẫu; ảnh phục vụ qua static handler có cache.
- Khả dụng: health check `/actuator/health`; container restart policy.
- Trải nghiệm: mobile-first, tiếng Việt, mọi màn có loading / empty / error state.
