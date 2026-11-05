# Nhật ký quyết định (ADR rút gọn)

| # | Quyết định | Lý do |
|---|---|---|
| D1 | Code đặt trong thư mục `bookeasy/` thay vì gốc repo | Repo `Ducpham04/Ducpham04` là repo profile GitHub; `README.md` gốc là trang giới thiệu cá nhân, không được ghi đè. |
| D2 | Làm việc trên nhánh `claude/modest-pasteur-vo53xg` thay vì `feature/mvp` | Môi trường chạy chỉ cho phép push lên nhánh được chỉ định này; nhánh đóng vai trò `feature/mvp`. |
| D3 | Multi-tenant shared schema với cột `business_id` | Đơn giản vận hành, chi phí thấp cho hàng trăm cơ sở nhỏ; businessId lấy từ JWT. |
| D4 | Chống trùng lịch bằng bảng `booking_slots` + `UNIQUE(staff_id, slot_start)` đơn vị 15 phút | MySQL không có exclusion constraint; cách này bảo đảm ở tầng DB, chạy được cả trên H2 cho test, không cần khóa bi quan. Hệ quả: thời lượng dịch vụ là bội số của 15 phút (đủ cho spa/salon/gym/phòng khám). |
| D5 | Múi giờ cố định Asia/Ho_Chi_Minh, lưu `DATETIME` địa phương | Thị trường mục tiêu chỉ VN, không DST; tránh lỗi chuyển đổi. Có cột `timezone` để mở rộng sau. |
| D6 | Khách không có tài khoản; xác thực sở hữu booking bằng mã booking + SĐT | Đúng yêu cầu "đặt không cần tài khoản"; OTP SMS tốn phí → để sau MVP (interface có thể thêm). |
| D7 | Môi trường build không có Docker daemon → test tích hợp chạy trên H2 (MODE=MySQL) + một bộ test Testcontainers MySQL tự bỏ qua khi không có Docker (chạy trên GitHub Actions) | Vẫn bảo đảm migration Flyway chạy thật trên MySQL 8 ở CI. Migration viết bằng SQL tương thích cả hai. |
| D8 | Seed dữ liệu demo bằng Java `DemoDataSeeder` (bật bằng `APP_SEED_DEMO=true`) thay vì SQL | Cần băm BCrypt và sinh lịch hẹn tương đối theo ngày hiện tại để demo luôn có dữ liệu "hôm nay". |
| D9 | Email dev dùng Mailpit trong docker-compose | Xem được email xác nhận/nhắc lịch thật tại http://localhost:8025 mà không cần SMTP thật. |
| D10 | Upload logo lưu ổ đĩa (volume) `/uploads`, phục vụ qua backend | Đủ cho MVP; interface lưu trữ có thể chuyển sang S3/R2 sau. Cho phép nhập URL logo trực tiếp. |
| D11 | Rate limit in-memory (token bucket) | Một instance trong MVP; khi scale ngang chuyển sang Redis/Bucket4j. |
| D12 | Kéo thả lịch dùng HTML5 Drag & Drop + Pointer tự viết, không dùng FullCalendar | FullCalendar resource view là bản trả phí; lưới tự vẽ nhẹ và kiểm soát được thiết kế. |
| D13 | Tailwind v4 (cấu hình `@theme` bằng OKLCH) | Bản Tailwind hiện hành, hỗ trợ OKLCH tự nhiên — khớp design system. |
| D14 | Font Be Vietnam Pro + Fraunces, icon Lucide | Theo hướng dẫn impeccable: tránh Inter-mọi-nơi và emoji; cả hai font hỗ trợ đầy đủ dấu tiếng Việt. |
| D15 | Doanh thu báo cáo = tổng `price` của booking `COMPLETED` trong kỳ | Phản ánh dịch vụ đã thực hiện; tiền cọc của booking chưa thực hiện không tính doanh thu. |
| D16 | Booking chờ thanh toán hết hạn sau 30 phút → tự hủy | Tránh giữ slot ảo; con số cấu hình được (`app.payment.hold-minutes`). |
| D17 | "Bất kỳ ai" gán nhân viên ít booking nhất trong ngày | Cân bằng tải đơn giản, dễ giải thích cho chủ cơ sở. |
| D18 | Giờ mở cửa theo chi nhánh; ngày nghỉ có thể toàn cơ sở hoặc theo chi nhánh | Chuỗi nhỏ thường có giờ khác nhau theo chi nhánh. |
| D19 | Refresh token lưu `localStorage` (MVP) | Đơn giản cho SPA + proxy. Giảm thiểu bằng CSP, access 15', xoay vòng, phát hiện tái sử dụng. Đề xuất chuyển sang cookie HttpOnly (xem FINAL_REPORT). |
| D20 | Để PR ở trạng thái Draft khi bàn giao | `docker compose` và CI chưa được xác nhận chạy; chuyển Ready sau khi xác nhận. |
