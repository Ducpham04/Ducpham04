# DECISIONS – Nhật ký quyết định

Mỗi quyết định: bối cảnh → lựa chọn → lý do. Thực hiện khi yêu cầu chưa rõ, không hỏi lại.

## D-01. Đặt dự án ở thư mục `educenter/` trong repo `Ducpham04/Ducpham04`
- **Bối cảnh**: repo của phiên làm việc là repo hồ sơ GitHub (chỉ có README cá nhân). Code cũ thật sự nằm ở repo riêng `Ducpham04/english-class-manager` (Next.js + Supabase). Phiên làm việc chỉ được quyền ghi vào repo hồ sơ.
- **Lựa chọn**: đọc toàn bộ code cũ ở `english-class-manager` để tái sử dụng nghiệp vụ, viết hệ thống mới trong `educenter/` của repo được cấp quyền. Không đụng tới `README.md` gốc (hồ sơ cá nhân).
- **Lý do**: tránh phá hồ sơ GitHub khi merge; toàn bộ dự án tự chứa trong 1 thư mục, dễ tách sang repo riêng sau bằng `git subtree split -P educenter`.

## D-02. Nhánh làm việc
- Yêu cầu ghi `feature/mvp`, nhưng môi trường chỉ định nhánh `claude/gracious-goodall-l97dd4` và không cho đẩy sang nhánh khác → dùng nhánh chỉ định, PR Draft mở từ nhánh này. Có thể đổi tên nhánh khi merge.

## D-03. Viết lại backend & frontend theo stack chuẩn, giữ lại nghiệp vụ
- Code cũ: Next.js 16 + Supabase (Postgres, RLS tắt), **một trung tâm, không có tenant**, vai trò OWNER/TEACHER/ASSISTANT.
- **Giữ (port sang)**: mô hình học viên/lớp/lịch tuần/điểm danh/học phí, logic trạng thái học phí (`getStatusFromPayment`), sinh link VietQR (`buildVietQrImageUrl`, nội dung CK không dấu), danh sách ngân hàng, design tokens OKLCH xanh dương, văn phong tiếng Việt, ý tưởng cổng phụ huynh.
- **Viết lại**: toàn bộ data layer (Supabase → Spring Boot + JPA + MySQL), auth (Supabase auth → JWT access/refresh), UI (Next.js → React + Vite).
- **Bỏ (chưa port – sau MVP)**: nhận xét buổi học, bài tập, tin Zalo soạn sẵn, điểm kiểm tra, sổ chi phí, import Excel, trang landing bất động sản (không thuộc sản phẩm).

## D-04. Multi-tenant: shared schema + cột `tenant_id`
- Phương án: (a) DB riêng mỗi tenant, (b) schema riêng, (c) chung schema.
- Chọn (c): rẻ nhất để vận hành cho trung tâm nhỏ, 1 lần migrate, dễ báo cáo SaaS. Cô lập bằng tenantId lấy từ JWT, truyền tường minh vào mọi truy vấn repository (`…AndTenantId`), có integration test kiểm tra rò rỉ chéo tenant. Bản "bán đứt" = triển khai cùng image với 1 tenant.

## D-05. Email đăng nhập duy nhất toàn hệ thống
- Không bắt người dùng nhập "mã trung tâm" khi đăng nhập → UX đơn giản hơn. Hệ quả: 1 email chỉ thuộc 1 trung tâm (chấp nhận được với đối tượng khách hàng).

## D-06. Khóa chính BIGINT tự tăng thay vì UUID
- MySQL InnoDB tối ưu với khóa tăng dần; ID không lộ thông tin nhạy cảm vì mọi truy cập đều kiểm tra tenant + quyền.

## D-07. Phụ huynh và học viên dùng chung tài khoản cổng (vai trò `STUDENT`)
- Đề bài gộp "STUDENT/PARENT". Một tài khoản gắn với `student_id`, phụ huynh có nhiều con → nhiều tài khoản (sau MVP: 1 phụ huynh – nhiều học viên).

## D-08. Hóa đơn 1 dòng (không bảng invoice_items)
- Trung tâm nhỏ thường thu theo khóa/tháng, 1 dòng + giảm giá là đủ cho MVP; giảm độ phức tạp. Phụ phí (giáo trình) tạo hóa đơn riêng.

## D-09. Trạng thái "Quá hạn" là trạng thái suy diễn
- Lưu `UNPAID/PARTIAL/PAID/CANCELLED`; "quá hạn" = chưa đủ và `due_date < hôm nay` → luôn đúng theo thời gian, không cần job cập nhật. API trả thêm cờ `overdue`.

## D-10. Seed dữ liệu demo bằng Java (`DemoDataSeeder`) thay vì SQL
- Cần BCrypt hash và ngày tương đối với hôm nay (buổi học tuần này, hóa đơn sắp hạn) → Java linh hoạt hơn. Flyway chỉ quản lý schema. Bật bằng `APP_SEED_DEMO=true` (mặc định bật trong docker-compose), chỉ chạy khi DB trống.

## D-11. Zalo ZNS: interface `ZaloZnsClient` + `MockZaloZnsClient`
- ZNS thật cần Zalo OA đã xác thực, template được duyệt và access token OA (refresh 90 ngày) → không thể có trong MVP. Mock ghi log + `notification_logs` trạng thái `MOCK`. Hướng dẫn tích hợp thật ở README.

## D-12. PDF phiếu thu bằng OpenPDF + font DejaVu Sans nhúng
- OpenPDF (LGPL/MPL) miễn phí, đủ cho phiếu thu A5. Font hệ thống PDF chuẩn không có dấu tiếng Việt → nhúng DejaVuSans (cài trong image qua `fonts-dejavu-core`; trong code có fallback đường dẫn).

## D-13. Excel bằng Apache POI (SXSSF không cần – dữ liệu nhỏ)

## D-14. Font giao diện "Be Vietnam Pro", icon lucide
- Theo nguyên tắc impeccable: tránh "Inter cho mọi thứ", chọn font có lý do – Be Vietnam Pro được thiết kế cho tiếng Việt. Xem DESIGN.md.

## D-15. Frontend gọi API cùng origin qua proxy
- Dev: Vite proxy `/api` → `localhost:8080`; Docker: Nginx proxy `/api` → `backend:8080`. Tránh CORS và cho phép lưu refresh token phía client đơn giản.
- Lưu token: access token trong bộ nhớ + refresh token trong `localStorage` (đánh đổi: dễ triển khai SPA; rủi ro XSS được giảm bằng CSP của Nginx và không render HTML người dùng). Sau MVP chuyển refresh token sang cookie HttpOnly.

## D-16. Lịch sinh buổi tối đa 180 ngày
- Tránh sinh hàng nghìn bản ghi với lớp không có ngày kết thúc; lớp dài hơn có thể "Sinh tiếp buổi" bằng cách lưu lại lịch.

## D-17. STAFF được xem báo cáo
- Giáo vụ thường lập báo cáo cho chủ. Có thể siết lại bằng cấu hình sau MVP.

## D-18. Kiểm thử tích hợp bằng Testcontainers MySQL
- Đúng stack chuẩn, bắt lỗi khác biệt SQL thật. Môi trường không có Docker có thể bỏ qua bằng `-DskipITs`.

## D-19. Tailwind CSS v4 (plugin `@tailwindcss/vite`), tokens qua `@theme`
- Đồng bộ với code cũ (đã dùng v4), cấu hình ít hơn.

## D-20. Testcontainers 2.0.5
- Docker Engine 29 yêu cầu API ≥ 1.40; Testcontainers 1.21 dùng API 1.32 nên không chạy được. Nâng lên 2.0.5 (artifact `testcontainers-mysql`, `testcontainers-junit-jupiter`). Container khởi động trong static block vì test dùng `PER_CLASS` (Spring tạo context trước `@BeforeAll`).

## D-21. Lần sinh buổi học đầu tiên bắt đầu từ ngày khai giảng
- Trung tâm thường nhập lớp đang chạy giữa khóa → cần điểm danh bù các buổi đã qua. Lần lưu lịch đầu sinh từ `start_date`; các lần đổi lịch sau chỉ thay buổi từ hôm nay, giữ nguyên buổi đã điểm danh/đã hủy.

## D-22. Dùng lại refresh token = thu hồi toàn bộ phiên
- Refresh token xoay vòng; nếu token đã thu hồi bị dùng lại (dấu hiệu bị đánh cắp) thì thu hồi mọi refresh token của user. `refresh` dùng `noRollbackFor = BusinessException` để việc thu hồi được ghi dù request trả 401.

## D-23. Secret JWT mặc định bị chặn ở profile `prod`
- Dev/demo chạy được ngay với secret mặc định (có log cảnh báo); `SPRING_PROFILES_ACTIVE=prod` mà chưa đổi `JWT_SECRET` thì app dừng khởi động.

## D-24. Nginx xóa header `Origin` khi proxy `/api`
- SPA và API cùng origin qua Nginx nên không cần CORS; xóa `Origin` tránh lỗi 403 khi domain thật chưa được khai trong `APP_CORS_ORIGINS`. Vẫn giữ CORS cho trường hợp gọi API trực tiếp (app mobile sau MVP).
