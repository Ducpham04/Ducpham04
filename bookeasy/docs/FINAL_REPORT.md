# BookEasy – Báo cáo bàn giao (FINAL REPORT / HANDOFF)

Ngày: 05/11/2026 · Nhánh: `claude/modest-pasteur-vo53xg` · PR: Ducpham04/Ducpham04#4 (Draft)

## 1. Tóm tắt
Bản MVP **chạy được end-to-end** ở chế độ dev (backend profile `h2` + frontend Vite): khách đặt lịch trên trang `/b/{slug}`, hệ thống tính giờ trống thật và chặn trùng lịch ở tầng DB, thu cọc VietQR (webhook SePay/Casso/mock), chủ cơ sở quản lý lịch kéo thả, khách hàng, dịch vụ, nhân viên, báo cáo; nhắc lịch qua email + Zalo ZNS (mock).
**Chưa xác nhận được** `docker compose up` và GitHub Actions vì môi trường phát triển không có Docker daemon.

## 2. Đã làm
### Backend (Java 21, Spring Boot 3.5, MySQL 8 / H2, Flyway)
- Multi-tenant theo `business_id`, tenant lấy từ JWT. JWT access 15' + refresh 7 ngày lưu SHA-256, xoay vòng, phát hiện tái sử dụng.
- **Chống đặt trùng**: bảng `booking_slots` + `UNIQUE(staff_id, slot_start)` theo đơn vị 15'. Test 12 luồng đồng thời: đúng 1 thành công.
- Thuật toán slot: giờ mở cửa chi nhánh ∩ ca nhân viên − ngày nghỉ − nghỉ phép − slot đã khóa − thời gian đặt trước tối thiểu. "Bất kỳ ai" gán người ít lịch nhất trong ngày, tự thử người kế tiếp khi xung đột.
- Vòng đời booking có kiểm tra chuyển trạng thái; đổi giờ/đổi nhân viên; hủy giải phóng slot.
- Thanh toán: dựng URL `img.vietqr.io`, `PaymentWebhookParser` (SePay, Casso v1/v2, mock), so khớp mã `BEXXXXXX` trong nội dung CK, idempotent theo `(provider, txn_id)`, so sánh secret hằng thời gian, booking chờ thanh toán quá 30' tự hủy.
- Thông báo: `NotificationChannel` (Email qua SMTP/Mailpit, Zalo ZNS mock kèm hướng dẫn tích hợp thật), gửi sau commit (async), scheduler nhắc 24h/2h, nhật ký gửi.
- CRM khách (số lần đến, no-show, chi tiêu), báo cáo (doanh thu, no-show %, hủy %, theo ngày, top dịch vụ/nhân viên), dashboard hôm nay, super admin, upload logo kiểm tra magic bytes, rate limit, Swagger `/swagger-ui.html`.
- Seed demo tiếng Việt: Lotus Spa (2 chi nhánh, đặt cọc 30%), Titan Fitness, Nha khoa An Tâm; khoảng 600 lịch hẹn (30 ngày qua + 7 ngày tới).

### Frontend (React 19, Vite, TypeScript, Tailwind v4, React Query, RHF + Zod)
- Design system theo impeccable: màu OKLCH theo vai trò, Be Vietnam Pro + Fraunces, icon Lucide, không gradient/thẻ lồng thẻ/emoji; trạng thái luôn kèm chữ + icon; mobile-first; hỗ trợ `prefers-reduced-motion`.
- Trang công khai: wizard 4 bước, thanh hành động dính đáy, màu thương hiệu từng cơ sở, trang VietQR có đếm ngược và tự cập nhật, tra cứu/hủy.
- App quản trị: Hôm nay, Lịch ngày/tuần (cột theo nhân viên, kéo thả đổi giờ/nhân viên, bấm ô trống để tạo lịch, dạng danh sách trên mobile), Lịch hẹn, Khách hàng, Dịch vụ, Nhân viên (ca, nghỉ phép, tài khoản), Báo cáo (biểu đồ SVG + bảng), Cài đặt, Super admin, Landing có bảng giá.

### DevOps & tài liệu
- Dockerfile backend/frontend (nginx: CSP, SPA fallback, proxy `/api`), `docker-compose.yml` (MySQL 8.4, Mailpit, backend, frontend), `.env.example`, workflow `.github/workflows/bookeasy-ci.yml`.
- Tài liệu: PRD, DESIGN, ARCHITECTURE (ERD mermaid, API, phân quyền), DECISIONS (18 quyết định), CHECKLIST, TEST_PLAN, README.

### Kiểm thử
- Backend: **60/60 test pass** (unit + integration). Chi tiết ở `TEST_PLAN.md`.
- Frontend: `tsc` sạch, `vite build` thành công, oxlint 0 lỗi (3 cảnh báo). Đã đi qua các màn hình chính bằng Playwright (thủ công) trên mobile và desktop.

## 3. Chưa làm / làm dở
| Hạng mục | Tình trạng |
|---|---|
| Chạy thử `docker compose up` | Viết xong, **chưa chạy** (không có Docker) |
| GitHub Actions | Viết xong, chưa xem kết quả |
| Testcontainers MySQL | Dependency có, chưa có test |
| E2E Playwright trong repo | Chưa có (mới chạy script thủ công) |
| Vitest frontend | Đã cấu hình, chưa có test |
| Code-split bundle | Bundle chính khoảng 594 kB (gzip 188 kB) |
| Sau MVP | Gói thành viên/liệu trình, voucher, đánh giá, widget nhúng — chưa làm (đúng phạm vi) |

## 4. Lỗi đã biết & rủi ro
1. **Docker/MySQL chưa kiểm chứng**: migration V1 mới chạy trên H2 (MODE=MySQL). Rủi ro thấp (SQL chuẩn) nhưng cần chạy thật.
2. **Refresh token lưu ở `localStorage`**: rủi ro nếu bị XSS. Đã giảm thiểu bằng CSP chặt, access token 15', xoay vòng refresh và phát hiện tái sử dụng. Nên chuyển sang cookie `HttpOnly; SameSite=Strict`.
3. **Rate limit trong bộ nhớ**: chỉ đúng khi chạy 1 instance. Khi scale cần Redis/Bucket4j.
4. **Validation chạy trước kiểm tra quyền** (Spring `@Valid` trước `@PreAuthorize`): gửi body sai tới endpoint không có quyền nhận 400 thay vì 403. Không lộ dữ liệu, nhưng nên sửa.
5. **Email gửi đồng bộ trong scheduler nhắc lịch**: SMTP chậm sẽ làm vòng scheduler chậm. Nên đưa qua hàng đợi/outbox.
6. **Webhook thanh toán dùng một secret chung** cho mọi cơ sở. Khi mỗi cơ sở có tài khoản SePay/Casso riêng cần secret theo tenant. Giao dịch không khớp được lưu với `business_id = 0`.
7. **"Giả lập đã chuyển khoản"** (`PAYMENT_MOCK_ENABLED`) bật mặc định cho demo. **Bắt buộc tắt ở production.**
8. Kéo thả lịch dùng HTML5 DnD nên không hoạt động trên màn hình cảm ứng. Mobile đổi giờ qua danh sách/form (chưa có form đổi giờ riêng trên mobile, chỉ đổi qua desktop).
9. Báo cáo gộp theo `start_at` trong Java; với hơn 100k booking/kỳ nên chuyển sang SQL group by.
10. Múi giờ cố định `Asia/Ho_Chi_Minh` (quyết định D5).

## 5. Bước tiếp theo cụ thể
1. Trên máy có Docker: `cd bookeasy && cp .env.example .env && docker compose up --build`, mở http://localhost:3000, đăng nhập `owner@lotusspa.vn / Demo@123`. Sửa lỗi nếu có.
2. Kiểm tra CI trên PR, sửa nếu đỏ, rồi chuyển PR sang **Ready for review**.
3. Thêm `MySqlContainerTest`, E2E Playwright (đặt lịch, kéo thả, check-in), Vitest cho `lib/format.ts`.
4. Bảo mật: refresh token qua cookie HttpOnly, secret webhook theo tenant, kiểm tra quyền trước validate, Redis rate limit.
5. Tích hợp thật SePay (webhook `Authorization: Apikey`) và Zalo ZNS (template đã duyệt, xem javadoc `ZaloZnsChannel`).
6. Sản phẩm v0.2: widget nhúng `<script>`, đánh giá sau dịch vụ, voucher, thẻ liệu trình, OTP SĐT, form đổi giờ trên mobile.

## 6. Gợi ý giá bán (thị trường VN, 2026)
| Gói | Giá/tháng | Nội dung |
|---|---|---|
| Dùng thử | 0đ, 14 ngày | Đầy đủ tính năng |
| Cơ bản | 199.000đ | 1 chi nhánh, ≤ 3 nhân viên, email nhắc lịch |
| Chuyên nghiệp | 449.000đ | ≤ 3 chi nhánh, ≤ 15 nhân viên, VietQR tự xác nhận, báo cáo |
| Chuỗi | Liên hệ (từ 1.200.000đ) | Không giới hạn, Zalo ZNS (tính phí theo tin), hỗ trợ ưu tiên |

Thanh toán năm giảm 2 tháng. Mức giá này thấp hơn phí một khách no-show trung bình mỗi tuần của một spa nhỏ (300–500k), nên dễ chứng minh ROI khi bán.
