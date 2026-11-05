# BookEasy – Checklist triển khai

Thứ tự ưu tiên: luồng chính end-to-end (đặt lịch công khai → quản lý lịch) trước, mở rộng sau.

## 0. Tài liệu & hạ tầng dự án
- [x] PRD, DESIGN, ARCHITECTURE, DECISIONS, CHECKLIST
- [ ] Commit + push + mở Draft PR
- [ ] Skeleton backend (Spring Boot, Flyway, profile mysql/h2)
- [ ] Skeleton frontend (Vite, Tailwind v4, Router, React Query)

## 1. Backend – nền tảng
- [ ] Migration V1 toàn bộ schema
- [ ] Entity + repository
- [ ] Xử lý lỗi chuẩn + error code tiếng Việt
- [ ] Security: JWT access/refresh, login, register, logout, me
- [ ] Seed dữ liệu demo tiếng Việt (3 cơ sở, tài khoản mỗi vai trò)

## 2. Backend – luồng chính
- [ ] API công khai: thông tin cơ sở, nhân viên theo dịch vụ
- [ ] Tính slot trống (ngày + danh sách ngày)
- [ ] Tạo booking công khai, chống trùng bằng `booking_slots`
- [ ] Tra cứu/hủy booking bằng mã + SĐT
- [ ] Quản lý booking: danh sách, chi tiết, tạo tại quầy, đổi giờ, đổi trạng thái
- [ ] VietQR URL + webhook (SePay, Casso, mock) + xác nhận tay + hết hạn

## 3. Backend – cấu hình & mở rộng
- [ ] Cơ sở (thông tin, thương hiệu, logo upload, thanh toán, đặt lịch)
- [ ] Chi nhánh + giờ mở cửa, ngày nghỉ
- [ ] Danh mục + dịch vụ + nhân viên thực hiện
- [ ] Nhân viên + ca + ngày nghỉ + tài khoản STAFF
- [ ] Khách hàng (danh sách, chi tiết, ghi chú, thống kê)
- [ ] Thông báo: email xác nhận, nhắc 24h/2h (scheduler), Zalo ZNS mock, nhật ký
- [ ] Báo cáo tổng hợp
- [ ] Dashboard hôm nay
- [ ] Super admin
- [ ] Rate limit endpoint công khai

## 4. Frontend
- [ ] Design tokens, font, component UI cơ bản
- [ ] Trang đặt lịch công khai (wizard) + kết quả + VietQR + tra cứu
- [ ] Đăng nhập / đăng ký, quản lý token (refresh tự động)
- [ ] App shell (sidebar/bottom nav) theo vai trò
- [ ] Tổng quan hôm nay
- [ ] Lịch ngày/tuần theo nhân viên + kéo thả + side panel chi tiết
- [ ] Danh sách booking + tạo booking tại quầy
- [ ] Khách hàng
- [ ] Dịch vụ & danh mục
- [ ] Nhân viên, ca, ngày nghỉ
- [ ] Báo cáo
- [ ] Cài đặt (cơ sở, thương hiệu, chi nhánh & giờ, ngày nghỉ, thanh toán)
- [ ] Super admin
- [ ] Landing page BookEasy

## 5. QA
- [ ] TEST_PLAN.md
- [ ] Unit test (slot, trạng thái, parser webhook, JWT)
- [ ] Integration test (API + DB, chống trùng đồng thời, tenant isolation)
- [ ] Testcontainers MySQL (chạy ở CI)
- [ ] Frontend unit test (Vitest)
- [ ] E2E Playwright luồng chính
- [ ] Tất cả test pass

## 6. DevOps
- [ ] Dockerfile backend, frontend (nginx)
- [ ] docker-compose.yml (mysql, mailpit, backend, frontend) + .env.example
- [ ] GitHub Actions build + test

## 7. Review & tài liệu
- [ ] Security/code review + sửa lỗi
- [ ] README.md
- [ ] FINAL_REPORT.md
- [ ] PR Ready for review
