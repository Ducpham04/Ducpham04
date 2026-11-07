# FINAL REPORT – EduCenter MVP

*Ngày: 07/11/2026 · Nhánh: `claude/gracious-goodall-l97dd4` · Thư mục: `educenter/`*

## 1. Đã làm

**Tài liệu**: PRD (persona, 9 epic, user story + acceptance criteria, phạm vi MVP/sau MVP), DESIGN (design system OKLCH, font Be Vietnam Pro, component, sitemap, 15 màn hình, áp dụng nguyên tắc *impeccable*), ARCHITECTURE (ERD mermaid, ~70 endpoint, ma trận phân quyền), DECISIONS (24 quyết định), CHECKLIST, TEST_PLAN, README.

**Backend (Spring Boot 3.5 / Java 21)** – 12 module:
- Xác thực JWT access/refresh xoay vòng + phát hiện dùng lại, BCrypt, giới hạn đăng nhập sai, lỗi chuẩn RFC 7807 tiếng Việt.
- Multi-tenant chung schema, mọi truy vấn lọc `tenantId` từ JWT; SUPER_ADMIN tạo/khóa trung tâm.
- Học viên + phụ huynh + trạng thái + lịch sử lớp + tài khoản cổng; khóa học, phòng; lớp, lịch tuần, **phát hiện trùng phòng/giáo viên**, sinh buổi học, ghi danh (sĩ số, trùng); lịch theo khoảng ngày, hủy/đổi buổi (kiểm tra trùng từng buổi).
- Điểm danh (quyền theo lớp của GV), thống kê chuyên cần.
- Gói học phí, hóa đơn, thu nhiều lần (khóa dòng chống thu vượt khi đồng thời), công nợ, **VietQR**, **phiếu thu PDF** có số tiền bằng chữ.
- Dashboard + 3 báo cáo Excel. Nhắc lịch & học phí qua email (scheduler) + Zalo ZNS (interface + mock) + nhật ký, chống gửi trùng.
- Cổng phụ huynh chỉ đọc dữ liệu của đúng học viên. Seed dữ liệu demo tiếng Việt 2 trung tâm.

**Frontend (React 19 + Vite + TS + Tailwind 4)**: 17 trang, mobile-first (sidebar desktop, thanh điều hướng đáy + sheet trên mobile), form RHF + Zod, React Query, toast, trạng thái loading/empty/error đầy đủ, điểm danh một chạm, lịch tuần/tháng, kiểm tra trùng lịch trực tiếp khi chọn giờ, QR có nút sao chép từng dòng.

**DevOps**: Dockerfile backend (multi-stage, non-root), frontend (Nginx + CSP + proxy), `docker-compose.yml` (MySQL, Mailpit, backend, frontend), `.env.example`, GitHub Actions (backend verify, frontend typecheck/test/build, E2E trên compose).

**QA**: 21 unit + 10 integration (Testcontainers MySQL) backend, 12 unit frontend, 5 E2E Playwright – **tất cả pass**. Test tìm ra và đã sửa 5 lỗi thật (xem TEST_PLAN §4).

**Review bảo mật/hiệu năng** (đã xử lý): không SQL nối chuỗi (JPQL tham số hóa), validate mọi DTO, không mass-assignment (record DTO), refresh token lưu SHA-256, secret JWT mặc định bị chặn ở prod, stack trace không lộ ra client, actuator chỉ mở `health`, header bảo mật + CSP ở Nginx, kiểm tra quyền 2 lớp (vai trò + sở hữu), truy vấn danh sách theo lô (tránh N+1), phân trang, index theo `tenant_id`. Excel ghi giá trị dạng chuỗi (không có formula injection).

## 2. Chưa làm / giới hạn
- `docker compose build` chưa chạy trọn trong sandbox do Docker Hub giới hạn tần suất (429). Các thành phần đã chạy riêng lẻ thành công; CI có job dựng compose.
- Zalo ZNS mới là mock (cần OA + template duyệt). Email chưa thử với SMTP thật.
- Chưa có: import học viên từ Excel, sổ điểm, CRM tuyển sinh, đa chi nhánh, audit log đầy đủ, 2FA, đối soát chuyển khoản tự động, app mobile, billing SaaS tự động.
- Refresh token lưu `localStorage` (đánh đổi cho SPA đơn giản, giảm rủi ro bằng CSP) – nên chuyển sang cookie HttpOnly.
- Giới hạn đăng nhập sai lưu trong bộ nhớ – chỉ đúng với 1 instance.
- Chưa test tải, chưa test Safari iOS thật; chưa có dark mode.

## 3. Rủi ro
| Rủi ro | Mức | Giảm thiểu |
| --- | --- | --- |
| Rò rỉ dữ liệu chéo trung tâm do quên lọc `tenantId` ở code mới | Cao | Quy ước `…AndTenantId`, integration test cô lập; nên thêm Hibernate filter hoặc ArchUnit kiểm tra |
| Nhắc nhở gửi trùng/sót khi chạy nhiều instance | Trung bình | Hiện chống trùng bằng `notification_logs`; khi scale cần ShedLock |
| Phụ thuộc img.vietqr.io (dịch vụ ngoài) | Thấp | Có thể tự sinh payload EMVCo VietQR + render QR nội bộ |
| Khách nhập dữ liệu cũ nhiều → thao tác tay lâu | Trung bình | Ưu tiên import Excel ở bản tiếp theo (bản cũ đã có code mẫu) |

## 4. Đề xuất phiên bản tiếp theo (v1.1 → v2)
1. **Import/Export Excel học viên** (port từ bản cũ) + onboarding checklist cho trung tâm mới.
2. **Đối soát chuyển khoản tự động** qua webhook Casso/SePay: tự ghi nhận thanh toán khi nội dung CK khớp mã hóa đơn – điểm bán hàng mạnh nhất.
3. **Zalo ZNS thật** + mẫu tin; SMS brandname dự phòng.
4. **Sổ điểm & nhận xét** gửi phụ huynh (bản cũ có `test_scores`, `student_comments`).
5. **CRM tuyển sinh**: lead, học thử, tỷ lệ chuyển đổi.
6. Lương giáo viên theo buổi, sổ chi phí, lãi/lỗ.
7. Billing SaaS tự động (giới hạn theo gói), audit log, 2FA cho chủ trung tâm, Zalo Mini App cho phụ huynh.

## 5. Gợi ý giá bán (thị trường Việt Nam 2026)
| Gói | Đối tượng | Giá tham khảo |
| --- | --- | --- |
| Dùng thử | Mọi trung tâm | Miễn phí 30 ngày, đủ tính năng |
| Cơ bản | ≤ 150 học viên, 1 cơ sở | **390.000 ₫/tháng** (3,9 tr/năm) |
| Chuyên nghiệp | ≤ 600 học viên, nhắc Zalo, báo cáo đầy đủ | **890.000 ₫/tháng** (8,9 tr/năm) |
| Doanh nghiệp | Không giới hạn, nhiều cơ sở, đối soát tự động | từ **1.990.000 ₫/tháng** |
| Bán đứt (on-premise) | Trung tâm muốn tự giữ dữ liệu | **25–45 triệu ₫** cài đặt + 15–20%/năm bảo trì |

Phí phát sinh: tin Zalo ZNS tính theo lượt gửi thực tế; phí khởi tạo/nhập liệu 1–3 triệu ₫ tùy số học viên. Mức giá đặt thấp hơn các phần mềm quản lý trung tâm phổ biến hiện có để vào nhóm trung tâm nhỏ đang dùng Excel.
