# BookEasy – Checklist triển khai

Cập nhật lần cuối: 05/11/2026 (bàn giao). `[x]` đã xong · `[~]` làm dở · `[ ]` chưa làm.

## 0. Tài liệu & hạ tầng dự án
- [x] PRD, DESIGN, ARCHITECTURE, DECISIONS, CHECKLIST
- [x] Commit + push + mở Draft PR (Ducpham04/Ducpham04#4)
- [x] Skeleton backend (Spring Boot 3.5, Flyway, profile mysql/h2)
- [x] Skeleton frontend (Vite, Tailwind v4, Router, React Query)

## 1. Backend – nền tảng
- [x] Migration V1 toàn bộ schema (tương thích MySQL 8 + H2)
- [x] Entity + repository
- [x] Xử lý lỗi chuẩn + error code tiếng Việt
- [x] Security: JWT access/refresh (xoay vòng, phát hiện tái sử dụng), login, register, logout, me
- [x] Seed dữ liệu demo tiếng Việt (3 cơ sở, tài khoản mỗi vai trò)

## 2. Backend – luồng chính
- [x] API công khai: thông tin cơ sở, nhân viên theo dịch vụ
- [x] Tính slot trống (ngày + danh sách ngày)
- [x] Tạo booking công khai, chống trùng bằng `booking_slots` UNIQUE
- [x] Tra cứu/hủy booking bằng mã + SĐT
- [x] Quản lý booking: danh sách, chi tiết, tạo tại quầy, đổi giờ, đổi trạng thái, ghi chú
- [x] VietQR URL + webhook (SePay, Casso, mock) + xác nhận tay + hết hạn 30'

## 3. Backend – cấu hình & mở rộng
- [x] Cơ sở (thông tin, thương hiệu, upload logo, thanh toán, quy tắc đặt lịch)
- [x] Chi nhánh + giờ mở cửa, ngày nghỉ
- [x] Danh mục + dịch vụ + nhân viên thực hiện
- [x] Nhân viên + ca + ngày nghỉ + tài khoản STAFF
- [x] Khách hàng (danh sách, chi tiết, ghi chú, thống kê)
- [x] Thông báo: email xác nhận, nhắc 24h/2h (scheduler), Zalo ZNS mock, nhật ký gửi
- [x] Báo cáo tổng hợp
- [x] Dashboard hôm nay
- [x] Super admin (danh sách, khóa/mở cơ sở)
- [x] Rate limit endpoint công khai (in-memory)

## 4. Frontend
- [x] Design tokens (OKLCH), font, component UI cơ bản
- [x] Trang đặt lịch công khai (wizard) + kết quả + VietQR + tra cứu/hủy
- [x] Đăng nhập / đăng ký, quản lý token (refresh tự động)
- [x] App shell (sidebar desktop / bottom nav mobile) theo vai trò
- [x] Tổng quan hôm nay
- [x] Lịch ngày/tuần theo nhân viên + kéo thả + side panel chi tiết
- [x] Danh sách booking + tạo booking tại quầy
- [x] Khách hàng
- [x] Dịch vụ & danh mục
- [x] Nhân viên, ca, ngày nghỉ, tài khoản
- [x] Báo cáo
- [x] Cài đặt (cơ sở, thương hiệu, chi nhánh & giờ, ngày nghỉ, thanh toán)
- [x] Super admin
- [x] Landing page BookEasy
- [ ] Code-split bundle chính (hiện ~594 kB, cảnh báo của Vite)

## 5. QA
- [x] TEST_PLAN.md
- [x] Unit test backend (slot, trạng thái, tiền cọc, SĐT/slug, parser webhook, JWT, PaymentService với Mockito)
- [x] Integration test (API + DB H2, chống trùng đồng thời 12 luồng, tenant isolation, thanh toán, nhắc lịch, phân quyền STAFF)
- [x] Tất cả test backend pass: **60/60**
- [~] Testcontainers MySQL: dependency đã có, **chưa viết test** (môi trường build không có Docker)
- [ ] Frontend unit test (Vitest đã cài + cấu hình, chưa có test; `npm test` dùng `--passWithNoTests`)
- [~] E2E: luồng đặt lịch công khai đã chạy thủ công bằng script Playwright khi phát triển, **chưa đưa vào repo**

## 6. DevOps
- [x] Dockerfile backend, frontend (nginx + CSP + proxy /api)
- [~] docker-compose.yml (mysql, mailpit, backend, frontend) + .env.example — **viết xong, chưa chạy thử được** (không có Docker daemon)
- [~] GitHub Actions (backend `mvn verify`, frontend lint/test/build, build docker) — viết xong, chưa xem kết quả chạy trên GitHub

## 7. Review & tài liệu
- [~] Security/code review: đã tự rà trong lúc code (xem FINAL_REPORT §4), chưa có lượt review riêng
- [x] README.md
- [x] FINAL_REPORT.md
- [ ] PR Ready for review — **để Draft** cho tới khi docker compose + CI được xác nhận

## Việc tiếp theo (theo thứ tự)
1. Chạy `docker compose up --build` trên máy có Docker, sửa lỗi nếu có (rủi ro chính: Flyway V1 trên MySQL 8.4, Mailpit).
2. Xem kết quả GitHub Actions trên PR, sửa nếu đỏ.
3. Viết `MySqlContainerTest` (Testcontainers) chạy Flyway + một luồng đặt lịch trên MySQL thật.
4. Đưa script Playwright E2E vào `bookeasy/e2e` + job CI.
5. Vitest cho `lib/format.ts` (normalizePhone, vnNow, addDays) và wizard đặt lịch.
6. Chuyển PR sang Ready for review.
