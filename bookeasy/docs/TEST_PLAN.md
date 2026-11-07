# BookEasy – Test plan

## Phạm vi & chiến lược
| Tầng | Công cụ | Trạng thái |
|---|---|---|
| Unit backend | JUnit 5, AssertJ, Mockito | ✅ có |
| Integration backend | SpringBootTest + MockMvc + H2 (MODE=MySQL) + Flyway thật, `MutableClock` để tua thời gian | ✅ có |
| DB thật | Testcontainers MySQL 8 | ⏳ chưa viết |
| Unit frontend | Vitest + Testing Library | ⏳ đã cấu hình, chưa có test |
| E2E | Playwright (Chromium) | ⏳ mới chạy thủ công |

Chạy: `cd bookeasy/backend && mvn test` → **60 test, 0 lỗi** (05/11/2026).

## Ma trận test đã tự động hóa
| Nhóm | Test | Kiểm tra |
|---|---|---|
| Thuật toán slot | `SlotCalculatorTest` | bước 15', vừa khung, bận chồng lấn, thời gian đặt trước tối thiểu, căn mốc, nhiều ca, giao giờ mở cửa |
| Vòng đời booking | `BookingStatusTest` | chuyển hợp lệ/không hợp lệ, trạng thái giữ chỗ |
| Tiền cọc | `AmountDueTest` | làm tròn lên 1.000đ, FULL/NONE, thiếu tài khoản ngân hàng |
| Dữ liệu VN | `TextUtilsTest` | chuẩn hóa SĐT (+84, dấu chấm/gạch), slug tiếng Việt |
| Thanh toán | `PaymentParsersTest`, `PaymentServiceTest` | payload SePay/Casso v1+v2, bỏ giao dịch chi, tách mã booking từ nội dung nhiễu, URL VietQR, idempotent, không khớp |
| JWT | `JwtServiceTest` | round-trip claims, token sửa/khác key/hết hạn, secret ngắn |
| Auth API | `AuthIntegrationTest` | đăng ký/đăng nhập/me, lỗi validate tiếng Việt, slug trùng, refresh xoay vòng + phát hiện tái sử dụng, logout, 401/403 |
| Luồng đặt lịch | `BookingFlowIntegrationTest` | trang công khai + 37 slot, trùng/chồng lấn → 409, "bất kỳ ai" gán người khác, ngoài giờ/quá khứ/giờ lệch/SĐT sai, ngày nghỉ + nghỉ phép, tra cứu/hủy giải phóng slot, đổi giờ + đổi nhân viên, luồng trạng thái + báo cáo, đặt cọc qua webhook + chống lặp + hết hạn, nhắc 24h/2h đúng 1 lần, STAFF chỉ thấy lịch của mình, tạo tại quầy |
| Đồng thời | `ConcurrentBookingIntegrationTest` | 12 request cùng slot → đúng 1 thành công (201), 11 bị từ chối (409) |
| Đa tenant | `TenantIsolationIntegrationTest` | chủ cơ sở B nhận 404 với booking/khách/dịch vụ/nhân viên/chi nhánh của A; cơ sở bị khóa không nhận lịch và không đăng nhập được |

## Kiểm thử thủ công đã làm (Playwright, Chromium)
- Mobile (iPhone 13): wizard dịch vụ → nhân viên → giờ → thông tin → trang VietQR, giả lập thanh toán.
- Desktop 1360px: đăng nhập demo, Hôm nay, Lịch ngày (căn cột, đường "bây giờ"), Báo cáo, Landing.
- Lỗi đã phát hiện và sửa: proxy Vite đổi Host gây 403 CORS; header lịch lệch cột; lời chào dùng múi giờ trình duyệt; nhãn tháng "Th 11" dễ nhầm với "Thứ".

## Việc còn lại
1. `MySqlContainerTest` (`@Testcontainers(disabledWithoutDocker = true)`): Flyway trên MySQL 8.4 + seed demo + đặt lịch.
2. E2E Playwright: đặt lịch công khai; chủ cơ sở kéo thả đổi giờ; STAFF check-in.
3. Vitest: `normalizePhone`, `vnNow`, `addDays/startOfWeek`, `onColor`; render wizard với MSW.
