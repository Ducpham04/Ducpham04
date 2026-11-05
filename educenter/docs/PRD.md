# PRD – EduCenter

> Phần mềm quản lý trung tâm tiếng Anh / giáo dục, mô hình SaaS multi-tenant cho trung tâm nhỏ & vừa tại Việt Nam.
> Phiên bản tài liệu: 1.0 (MVP) · Cập nhật: 2026-11-05

## 1. Bối cảnh & vấn đề

Trung tâm tiếng Anh quy mô 50–800 học viên ở Việt Nam đa phần quản lý bằng Excel + Zalo + sổ tay:

- Lịch lớp nằm rải rác, hay **trùng phòng / trùng giáo viên** khi mở lớp mới.
- Điểm danh giấy, cuối tháng mới tổng hợp → không biết học viên nghỉ nhiều để chăm sóc kịp.
- Học phí thu nhiều lần (đặt cọc, trả góp), **công nợ không rõ ai còn nợ bao nhiêu**; chuyển khoản sai nội dung khó đối soát.
- Phụ huynh liên tục nhắn hỏi lịch, hỏi con có đi học không, còn nợ bao nhiêu.
- Chủ trung tâm không có số liệu doanh thu / chuyên cần theo tháng để ra quyết định.

Code cũ (`english-class-manager`, Next.js + Supabase) đã chứng minh nhu cầu ở quy mô **1 giáo viên dạy thêm**. EduCenter mở rộng thành sản phẩm **bán được cho nhiều trung tâm** (multi-tenant, nhiều vai trò).

## 2. Mục tiêu

| Mục tiêu kinh doanh | Chỉ số |
| --- | --- |
| Bán theo gói tháng (SaaS) hoặc bán đứt (on-premise) | ≥ 10 trung tâm trả phí trong 6 tháng đầu |
| Giảm thời gian hành chính của giáo vụ | Điểm danh 1 lớp 25 HV < 1 phút; tra công nợ 1 HV < 15 giây |
| Giảm tin nhắn hỏi đáp của phụ huynh | ≥ 50% phụ huynh dùng cổng xem thông tin |
| Thu học phí đúng hạn hơn | Tỷ lệ hóa đơn quá hạn giảm 30% nhờ nhắc tự động + QR |

## 3. Persona

| Persona | Vai trò hệ thống | Mô tả | Nhu cầu chính |
| --- | --- | --- | --- |
| **Anh Minh** – chủ hệ thống | `SUPER_ADMIN` | Người vận hành EduCenter SaaS | Tạo/khóa trung tâm, xem số trung tâm, gói đăng ký |
| **Chị Lan** – chủ trung tâm "Anh ngữ Hoa Sen" (320 HV) | `CENTER_ADMIN` | Kiêm quản lý, ít thời gian, dùng laptop + điện thoại | Doanh thu, công nợ, lớp nào đông/vắng, quản lý nhân viên |
| **Thu** – giáo vụ kiêm thu ngân | `STAFF` | Ngồi quầy, thao tác nhiều nhất | Nhập học viên, xếp lớp, lập hóa đơn, thu tiền, in phiếu thu |
| **Thầy David / cô Hạnh** – giáo viên | `TEACHER` | Dạy 4–6 lớp, chỉ có điện thoại trong lớp | Xem lịch dạy tuần, điểm danh nhanh trên mobile |
| **Phụ huynh bé Gia Hân / học viên lớn** | `STUDENT` (tài khoản cổng) | Xem trên điện thoại | Lịch học, buổi có/vắng, học phí còn nợ + QR chuyển khoản |

> Quyết định: phụ huynh và học viên dùng **chung 1 tài khoản cổng** gắn với hồ sơ học viên (vai trò `STUDENT`). Xem `DECISIONS.md#D-07`.

## 4. User stories & Acceptance criteria (MVP)

Ký hiệu: **AC** = acceptance criteria. Tất cả dữ liệu luôn lọc theo tenant của người đăng nhập.

### E1. Xác thực & đa trung tâm

- **US-1.1** – Là người dùng, tôi đăng nhập bằng email + mật khẩu để vào đúng không gian trung tâm của mình.
  - AC: sai mật khẩu → thông báo chung "Email hoặc mật khẩu không đúng" (không lộ email tồn tại); đúng → nhận access token (15 phút) + refresh token (7 ngày), tự làm mới khi hết hạn.
  - AC: tài khoản bị khóa hoặc trung tâm bị khóa → không đăng nhập được.
- **US-1.2** – Là người dùng, tôi đổi mật khẩu của chính mình.
  - AC: phải nhập đúng mật khẩu cũ; mật khẩu mới ≥ 8 ký tự; đổi xong mọi refresh token cũ bị thu hồi.
- **US-1.3** – Là SUPER_ADMIN, tôi tạo trung tâm mới kèm tài khoản chủ trung tâm, khóa/mở khóa trung tâm.
  - AC: mã trung tâm (slug) duy nhất; dữ liệu trung tâm A **không bao giờ** xuất hiện ở trung tâm B (có test tự động).
- **US-1.4** – Là CENTER_ADMIN, tôi quản lý nhân viên (STAFF, TEACHER): tạo, sửa, khóa.

### E2. Học viên

- **US-2.1** – Là giáo vụ, tôi tạo/sửa hồ sơ học viên: họ tên, ngày sinh, giới tính, SĐT, email, trường, trình độ đầu vào, ghi chú.
  - AC: họ tên bắt buộc; SĐT đúng định dạng VN (10 số, đầu 0); mã học viên tự sinh `HV0001`…
- **US-2.2** – Tôi thêm 1–n phụ huynh cho học viên (họ tên, quan hệ, SĐT, email, Zalo, liên hệ chính).
  - AC: mỗi học viên có tối đa 1 phụ huynh "liên hệ chính"; email nhắc nhở gửi tới phụ huynh chính (fallback email học viên).
- **US-2.3** – Tôi đổi trạng thái học viên: **Đang học / Bảo lưu / Đã nghỉ** (+ Học thử).
  - AC: chuyển sang Đã nghỉ thì các ghi danh đang hoạt động chuyển sang "Đã rời lớp".
- **US-2.4** – Tôi tìm kiếm & lọc học viên theo tên/SĐT/mã, trạng thái; danh sách phân trang.
- **US-2.5** – Tôi xem lịch sử lớp của học viên (lớp, ngày vào, ngày ra, trạng thái), chuyên cần, hóa đơn.
- **US-2.6** – Tôi cấp tài khoản cổng cho học viên/phụ huynh (email + mật khẩu tạm).

### E3. Khóa học, lớp, phòng

- **US-3.1** – CENTER_ADMIN/STAFF quản lý **khóa học** (tên, mã, trình độ, số buổi, học phí niêm yết, mô tả).
- **US-3.2** – Quản lý **phòng học** (tên, sức chứa).
- **US-3.3** – Tạo **lớp** thuộc khóa: mã lớp, giáo viên, phòng, sĩ số tối đa, ngày bắt đầu/kết thúc, trạng thái (Sắp mở / Đang học / Đã kết thúc / Hủy).
- **US-3.4** – Đặt **lịch học lặp theo tuần** (VD: T2-T4-T6, 18:00–19:30).
  - AC: khi lưu, hệ thống **kiểm tra trùng phòng và trùng giáo viên** với các lớp khác đang hoạt động có khoảng ngày giao nhau; trùng → báo lỗi 409 nêu rõ lớp nào, thứ mấy, giờ nào.
  - AC: lưu lịch thành công → sinh các **buổi học** (session) từ ngày bắt đầu tới ngày kết thúc (tối đa 6 tháng).
- **US-3.5** – Ghi danh học viên vào lớp.
  - AC: không vượt sĩ số tối đa; không ghi danh trùng; học viên "Đã nghỉ" không ghi danh được.
- **US-3.6** – Chuyển học viên khỏi lớp (kết thúc ghi danh, giữ lịch sử).

### E4. Lịch

- **US-4.1** – Xem lịch dạy theo **tuần** và **tháng**, lọc theo giáo viên / phòng / lớp.
  - AC: TEACHER mặc định chỉ thấy lịch của mình.
- **US-4.2** – Hủy 1 buổi (có lý do) hoặc đổi giờ/phòng 1 buổi; đổi giờ cũng kiểm tra trùng.
- **US-4.3** – Endpoint kiểm tra trùng độc lập (dùng cho form) – trả danh sách xung đột.

### E5. Điểm danh

- **US-5.1** – Là giáo viên, trên điện thoại tôi mở "Buổi dạy hôm nay", chạm 1 lần để đánh **Có mặt / Đi muộn / Vắng có phép / Vắng không phép**, mặc định tất cả "Có mặt", lưu 1 lần.
  - AC: chỉ giáo viên của lớp (hoặc ADMIN/STAFF) được điểm danh; vùng chạm ≥ 44px; lưu lại được nhiều lần (upsert); buổi đã điểm danh chuyển trạng thái "Đã dạy".
- **US-5.2** – Xem thống kê chuyên cần theo lớp và theo học viên (tỷ lệ % = (có mặt + muộn) / tổng buổi đã điểm danh).

### E6. Học phí & công nợ

- **US-6.1** – Quản lý **gói học phí** (tên, số tiền, số buổi/tháng, gắn khóa học tùy chọn).
- **US-6.2** – Lập **hóa đơn** cho học viên: chọn gói hoặc nhập dòng tự do, giảm giá, hạn thanh toán. Mã hóa đơn `HD2611-0001`.
- **US-6.3** – Ghi nhận **thanh toán nhiều lần** (tiền mặt / chuyển khoản / thẻ / khác).
  - AC: không thu vượt số còn lại; trạng thái tự tính: Chưa thu → Thu một phần → Đã thu đủ; quá hạn khi qua hạn mà chưa đủ; có thể Hủy hóa đơn (chưa có thanh toán).
- **US-6.4** – Xem **công nợ**: danh sách học viên còn nợ, tổng nợ, nợ quá hạn.
- **US-6.5** – Hiển thị **mã QR VietQR** cho số tiền còn lại (ảnh từ `img.vietqr.io`, nội dung CK không dấu `HD2611-0001 NGUYEN GIA HAN`).
  - AC: chỉ hiện khi trung tâm đã khai báo ngân hàng + số tài khoản.
- **US-6.6** – Xuất **phiếu thu PDF** cho từng lần thanh toán (tiếng Việt có dấu, thông tin trung tâm, số tiền bằng chữ).

### E7. Nhắc nhở

- **US-7.1** – Hệ thống tự gửi **email nhắc lịch học** cho buổi ngày mai (chạy 19:00 hằng ngày).
- **US-7.2** – Tự gửi **email nhắc học phí** cho hóa đơn sắp đến hạn (≤ 3 ngày) và quá hạn (chạy 08:00 hằng ngày, mỗi hóa đơn tối đa 1 lần/ngày).
- **US-7.3** – Kênh **Zalo ZNS**: có interface + bản mock ghi log; tài liệu cách tích hợp thật.
- **US-7.4** – CENTER_ADMIN xem **nhật ký gửi** và bấm "Chạy nhắc ngay" để thử.

### E8. Báo cáo

- **US-8.1** – Dashboard: doanh thu tháng này, công nợ hiện tại, học viên mới tháng này, tỷ lệ chuyên cần 30 ngày, buổi học hôm nay.
- **US-8.2** – Biểu đồ doanh thu 12 tháng; học viên mới theo tháng.
- **US-8.3** – Xuất Excel: doanh thu theo tháng, danh sách công nợ, chuyên cần theo lớp.

### E9. Cổng phụ huynh / học viên

- **US-9.1** – Xem lịch học sắp tới của các lớp đang học.
- **US-9.2** – Xem lịch sử điểm danh và tỷ lệ chuyên cần.
- **US-9.3** – Xem hóa đơn, số còn nợ, mã QR chuyển khoản.
  - AC: tài khoản cổng **chỉ** đọc dữ liệu của đúng học viên được gắn; không gọi được API quản trị (403).

## 5. Phạm vi

### MVP (bản này)
E1 → E9 như trên, web responsive mobile-first, tiếng Việt, docker compose 1 lệnh, seed dữ liệu demo.

### Sau MVP (không làm trong bản này)
- **Sổ điểm** (bài kiểm tra, nhận xét định kỳ, báo cáo học tập gửi phụ huynh) – code cũ có `test_scores`, `student_comments` có thể port.
- **CRM tuyển sinh**: lead, nguồn, lịch học thử, chuyển đổi lead → học viên.
- **App mobile** (React Native / Zalo Mini App) cho giáo viên & phụ huynh, push notification.
- **Gói đăng ký tự động**: billing SaaS cho từng trung tâm (VNPay/MoMo recurring), giới hạn theo gói.
- Zalo ZNS thật (cần OA + template duyệt), SMS brandname.
- Đối soát chuyển khoản tự động (webhook Casso/SePay).
- Lương giáo viên theo buổi, sổ chi phí (code cũ có `expenses`).
- Import Excel học viên (code cũ có, ưu tiên port sớm).
- Đa chi nhánh trong 1 tenant, audit log đầy đủ, 2FA.

## 6. Yêu cầu phi chức năng
- Bảo mật: JWT + BCrypt, phân quyền theo vai trò ở API, cô lập tenant ở tầng service/repository, validate mọi input, không lộ stack trace.
- Hiệu năng: danh sách phân trang; truy vấn báo cáo dùng aggregate SQL; p95 API < 300ms với 1.000 HV/tenant.
- Khả dụng: chạy được trên VPS 2GB RAM; 1 lệnh `docker compose up`.
- Ngôn ngữ: giao diện tiếng Việt, tiền tệ VND, múi giờ `Asia/Ho_Chi_Minh`.
