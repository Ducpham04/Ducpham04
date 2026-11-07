# CHECKLIST – Kế hoạch triển khai

Thứ tự ưu tiên: luồng chính chạy end-to-end trước (đăng nhập → học viên → lớp + lịch → điểm danh → hóa đơn/thu tiền → dashboard), mở rộng sau.

## 0. Tài liệu
- [x] PRD.md
- [x] DESIGN.md
- [x] ARCHITECTURE.md
- [x] DECISIONS.md
- [x] CHECKLIST.md
- [x] Commit, push, mở Draft PR

## 1. Nền tảng backend
- [x] Maven project Spring Boot 3 / Java 21, cấu hình profile, Dockerfile
- [x] Flyway V1 schema đầy đủ
- [x] Xử lý lỗi chung (ProblemDetail), PageResponse, CodeSequenceService
- [x] Spring Security + JWT access/refresh, rate-limit đăng nhập
- [x] Auth API: login / refresh / logout / me / change-password

## 2. Nghiệp vụ backend
- [x] Tenants (SUPER_ADMIN) + thông tin trung tâm/ngân hàng
- [x] Users / nhân viên
- [x] Học viên + phụ huynh + trạng thái + lịch sử lớp + tài khoản cổng
- [x] Khóa học, phòng học
- [x] Lớp, lịch tuần, kiểm tra trùng phòng/GV, sinh buổi học, ghi danh
- [x] Lịch theo khoảng ngày, đổi/hủy buổi
- [x] Điểm danh + thống kê chuyên cần
- [x] Gói học phí, hóa đơn, thanh toán nhiều lần, công nợ
- [x] VietQR + phiếu thu PDF
- [x] Báo cáo dashboard + xuất Excel
- [x] Nhắc nhở email (scheduler) + Zalo ZNS mock + nhật ký
- [x] Cổng học viên/phụ huynh
- [x] Seed dữ liệu demo tiếng Việt

## 3. Frontend
- [x] Vite + TS + Tailwind v4 + Router + React Query + RHF + Zod, design tokens
- [x] API client có tự refresh token, AuthContext, route guard theo vai trò
- [x] Layout: sidebar desktop + bottom nav mobile
- [x] Đăng nhập (tài khoản demo)
- [x] Dashboard (ADMIN/STAFF) + Hôm nay (TEACHER)
- [x] Học viên: danh sách, tạo/sửa, chi tiết (phụ huynh, lớp, chuyên cần, học phí, cổng)
- [x] Khóa học & phòng
- [x] Lớp: danh sách, chi tiết, lịch tuần (hiện lỗi trùng), ghi danh, buổi học
- [x] Lịch tuần/tháng
- [x] Điểm danh mobile
- [x] Học phí: hóa đơn, chi tiết + QR + thu tiền + PDF, công nợ, gói học phí
- [x] Báo cáo + xuất Excel
- [x] Nhân viên, Cài đặt (trung tâm, ngân hàng, nhắc nhở), Tài khoản
- [x] Trung tâm (SUPER_ADMIN)
- [x] Cổng phụ huynh

## 4. DevOps
- [x] docker-compose.yml (mysql, mailpit, backend, frontend) – 1 lệnh
- [x] .env.example
- [x] GitHub Actions: build + test backend & frontend

## 5. QA
- [x] TEST_PLAN.md
- [x] Unit test backend (conflict, billing, VietQR, JWT…)
- [x] Integration test (Testcontainers) luồng chính + cô lập tenant + phân quyền
- [x] Unit test frontend (Vitest)
- [x] E2E Playwright luồng chính
- [x] Tất cả test pass

## 6. Review & tài liệu
- [x] Review bảo mật/hiệu năng, sửa vấn đề
- [x] README.md hoàn chỉnh
- [x] FINAL_REPORT.md
- [x] PR Ready for review

## Ghi chú trạng thái
- `docker compose config` hợp lệ; `docker compose build` chưa chạy trọn trong sandbox vì Docker Hub trả 429 (giới hạn pull). Backend jar, frontend build, MySQL thật, toàn bộ test đã chạy được ngoài compose. Job `e2e` trong GitHub Actions dựng toàn bộ stack bằng compose.
