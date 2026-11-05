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
- [ ] Maven project Spring Boot 3 / Java 21, cấu hình profile, Dockerfile
- [ ] Flyway V1 schema đầy đủ
- [ ] Xử lý lỗi chung (ProblemDetail), PageResponse, CodeSequenceService
- [ ] Spring Security + JWT access/refresh, rate-limit đăng nhập
- [ ] Auth API: login / refresh / logout / me / change-password

## 2. Nghiệp vụ backend
- [ ] Tenants (SUPER_ADMIN) + thông tin trung tâm/ngân hàng
- [ ] Users / nhân viên
- [ ] Học viên + phụ huynh + trạng thái + lịch sử lớp + tài khoản cổng
- [ ] Khóa học, phòng học
- [ ] Lớp, lịch tuần, kiểm tra trùng phòng/GV, sinh buổi học, ghi danh
- [ ] Lịch theo khoảng ngày, đổi/hủy buổi
- [ ] Điểm danh + thống kê chuyên cần
- [ ] Gói học phí, hóa đơn, thanh toán nhiều lần, công nợ
- [ ] VietQR + phiếu thu PDF
- [ ] Báo cáo dashboard + xuất Excel
- [ ] Nhắc nhở email (scheduler) + Zalo ZNS mock + nhật ký
- [ ] Cổng học viên/phụ huynh
- [ ] Seed dữ liệu demo tiếng Việt

## 3. Frontend
- [ ] Vite + TS + Tailwind v4 + Router + React Query + RHF + Zod, design tokens
- [ ] API client có tự refresh token, AuthContext, route guard theo vai trò
- [ ] Layout: sidebar desktop + bottom nav mobile
- [ ] Đăng nhập (tài khoản demo)
- [ ] Dashboard (ADMIN/STAFF) + Hôm nay (TEACHER)
- [ ] Học viên: danh sách, tạo/sửa, chi tiết (phụ huynh, lớp, chuyên cần, học phí, cổng)
- [ ] Khóa học & phòng
- [ ] Lớp: danh sách, chi tiết, lịch tuần (hiện lỗi trùng), ghi danh, buổi học
- [ ] Lịch tuần/tháng
- [ ] Điểm danh mobile
- [ ] Học phí: hóa đơn, chi tiết + QR + thu tiền + PDF, công nợ, gói học phí
- [ ] Báo cáo + xuất Excel
- [ ] Nhân viên, Cài đặt (trung tâm, ngân hàng, nhắc nhở), Tài khoản
- [ ] Trung tâm (SUPER_ADMIN)
- [ ] Cổng phụ huynh

## 4. DevOps
- [ ] docker-compose.yml (mysql, mailpit, backend, frontend) – 1 lệnh
- [ ] .env.example
- [ ] GitHub Actions: build + test backend & frontend

## 5. QA
- [ ] TEST_PLAN.md
- [ ] Unit test backend (conflict, billing, VietQR, JWT…)
- [ ] Integration test (Testcontainers) luồng chính + cô lập tenant + phân quyền
- [ ] Unit test frontend (Vitest)
- [ ] E2E Playwright luồng chính
- [ ] Tất cả test pass

## 6. Review & tài liệu
- [ ] Review bảo mật/hiệu năng, sửa vấn đề
- [ ] README.md hoàn chỉnh
- [ ] FINAL_REPORT.md
- [ ] PR Ready for review
