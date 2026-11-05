# DECISIONS – Nhật ký quyết định

Ghi lại các điểm đặc tả chưa rõ và phương án đã chọn (ADR rút gọn).

| # | Vấn đề | Quyết định | Lý do |
|---|--------|-----------|-------|
| D1 | Đặc tả yêu cầu nhánh `feature/mvp`, môi trường làm việc chỉ định nhánh `claude/dreamy-curie-0e9dkn` | Làm việc và push trên `claude/dreamy-curie-0e9dkn`, PR vào `main` | Quy định của môi trường không cho push nhánh khác; nội dung PR không đổi |
| D2 | Repo hiện là GitHub profile (`README.md` cá nhân) | Chuyển README cũ sang `docs/PROFILE_README.md`, README gốc là của sản phẩm | Đặc tả yêu cầu README sản phẩm; giữ lại nội dung cũ để không mất. Khuyến nghị tách sang repo riêng trước khi bán |
| D3 | Spring Boot 3 hay 4 (4.x đã ra) | **Spring Boot 3.5.x** | Stack chuẩn ghi "Spring Boot 3"; 3.5 là nhánh 3.x mới nhất, hệ sinh thái (springdoc 2.8, Testcontainers 1.x) ổn định |
| D4 | Admin web đặt ở đâu | Thư mục `/admin` riêng (không nằm trong miniapp) | Tách build/deploy; admin không cần zmp-sdk |
| D5 | Ví dụ "tin tức/sản phẩm" | Làm **cả hai**: Tin tức (nội dung) + Sản phẩm (dùng cho demo thanh toán) | Thể hiện được cả luồng nội dung và thương mại – tăng giá trị bán |
| D6 | Router Mini App | Dùng `ZMPRouter`/`AnimationRoutes` của zmp-ui (bên dưới là React Router v6) | Đúng chuẩn Zalo (hiệu ứng chuyển trang, nút back), vẫn thoả yêu cầu React Router |
| D7 | Cơ chế Mock zmp-sdk | Lớp adapter `ZaloBridge` thay vì alias module `zmp-sdk` | Rõ ràng, type-safe, dễ test, không phụ thuộc nội bộ zmp-sdk; mock chọn bằng `VITE_ZALO_MODE` |
| D8 | Backend xác thực token Zalo khi mock | `app.zalo.mock=true` → chấp nhận token dạng `mock-<zaloId>`; tự tắt ở profile `prod` nếu không bật | Cho phép demo end-to-end mà không gọi Zalo; tránh vô tình bật ở production (log cảnh báo lớn khi khởi động) |
| D9 | Lưu token ở Mini App | `localStorage` qua wrapper `storage.ts` (Zalo webview hỗ trợ), có thể chuyển `nativeStorage` của zmp-sdk | Đơn giản, chạy được cả mock trên trình duyệt |
| D10 | Lưu ảnh upload | Local volume `/data/uploads` qua interface `StorageService` | MVP chạy 1 lệnh không cần S3; interface sẵn để thêm S3/MinIO |
| D11 | Công thức `mac` Checkout SDK | HMAC-SHA256(privateKey, chuỗi `key=value` sắp xếp theo key của `amount, desc, extradata, item, method`); callback xác minh trên `appId, amount, description, orderId, message, resultCode, transId` | Theo tài liệu Checkout SDK công khai; cô lập trong `ZaloCheckoutMacService` để dễ chỉnh khi Zalo đổi – có ghi trong PITFALLS |
| D12 | Docker Hub bị giới hạn rate trong môi trường build | Dùng tên image chuẩn (`mysql:8.4`, …) trong compose; trong sandbox kéo từ `mirror.gcr.io` rồi tag lại | Người mua dùng Docker Hub bình thường |
| D13 | E2E chạy trên gì | Playwright chạy Mini App ở chế độ Mock + Admin trên trình duyệt, backend thật + MySQL (docker compose) | Mini App thật không chạy được ngoài Zalo; mock mode chính là thứ demo cho khách |
| D14 | Mã phản ánh | `PA-{năm}-{id 6 số}` sinh sau khi insert | Dễ đọc, không cần sequence riêng |
| D15 | Tiền tệ | `BIGINT` VND (không thập phân) | VND không có phần lẻ; tránh lỗi float |
