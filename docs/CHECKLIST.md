# CHECKLIST – Kế hoạch triển khai

> Vai trò: Project Manager. Thứ tự = ưu tiên. Đánh dấu `[x]` khi xong.

## P0 – Tài liệu & khung
- [x] PRD, DESIGN, ARCHITECTURE, CHECKLIST, DECISIONS
- [ ] Commit + push + mở Draft PR

## P1 – Backend lõi (luồng chính end-to-end)
- [ ] Khởi tạo Spring Boot 3.5 / Java 21, cấu hình profile, Flyway V1 schema
- [ ] Error handling chuẩn + PageResponse
- [ ] JWT (access/refresh rotation), SecurityConfig, phân quyền
- [ ] ZaloGraphClient (mock + http) – xác thực token, giải mã SĐT/vị trí
- [ ] Auth API: /auth/zalo, /auth/login, /auth/refresh, /auth/logout
- [ ] Me API: /me, /me/phone, /me/location
- [ ] Articles + Products public API
- [ ] Feedback API + upload ảnh (validate magic bytes)
- [ ] Seed data tiếng Việt + tài khoản demo
- [ ] Notification gateway (mock + ZNS) + log
- [ ] Orders + PaymentProvider (mock + Zalo Checkout mac) + callback
- [ ] Admin API: stats, CRUD tin/SP, xử lý phản ánh, users, orders, notifications, uploads
- [ ] springdoc OpenAPI

## P2 – Mini App
- [ ] Khởi tạo Vite + React + TS + zmp-sdk + zmp-ui + Tailwind + React Query
- [ ] ZaloBridge (real + mock) + MockPanel
- [ ] API client (JWT interceptor, single-flight refresh, error mapping)
- [ ] AuthProvider + Splash đăng nhập
- [ ] Trang chủ, Tin tức (list/detail), Sản phẩm (list/detail)
- [ ] Gửi phản ánh (form RHF + Zod, ảnh, vị trí, từ chối quyền)
- [ ] Lịch sử (phản ánh + đơn), chi tiết phản ánh/đơn
- [ ] Hồ sơ + liên kết SĐT
- [ ] Thanh toán Checkout (mock sheet)
- [ ] app-config.json, zmp-cli.json, script deploy

## P3 – Admin web
- [ ] Khởi tạo Vite + React + TS + Tailwind + React Query + RHF + Zod
- [ ] Đăng nhập + guard theo vai trò
- [ ] Dashboard
- [ ] CRUD Tin tức, Sản phẩm
- [ ] Phản ánh: danh sách, chi tiết, cập nhật trạng thái
- [ ] Người dùng, Đơn hàng, Nhật ký thông báo

## P4 – DevOps
- [ ] Dockerfile backend / miniapp / admin
- [ ] docker-compose.yml một lệnh + .env.example
- [ ] GitHub Actions CI (backend test, miniapp & admin build/test, e2e)

## P5 – QA
- [ ] TEST_PLAN.md
- [ ] Unit test backend (JwtService, mac, services)
- [ ] Integration test backend (Testcontainers MySQL)
- [ ] Unit test frontend (Vitest: api client refresh, mock bridge, schema)
- [ ] E2E Playwright luồng chính (miniapp mock + admin)
- [ ] Tất cả test pass

## P6 – Review & bảo mật
- [ ] Review bảo mật (auth, validation, injection, secret, upload, CORS) + sửa
- [ ] Review hiệu năng (N+1, index, phân trang) + sửa

## P7 – Tài liệu bán hàng
- [ ] README.md
- [ ] docs/SETUP.md, docs/CUSTOMIZE.md, docs/PITFALLS.md
- [ ] LICENSE (thương mại mẫu), SELLING.md
- [ ] docs/FINAL_REPORT.md
- [ ] PR Ready for review
