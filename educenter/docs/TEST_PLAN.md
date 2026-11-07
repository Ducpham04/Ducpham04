# TEST PLAN – EduCenter MVP

## 1. Phạm vi & chiến lược

| Tầng | Công cụ | Đối tượng | Chạy ở |
| --- | --- | --- | --- |
| Unit (backend) | JUnit 5 + AssertJ | Logic thuần: phát hiện trùng lịch, sinh buổi học, tiền & trạng thái hóa đơn, VietQR, chữ tiếng Việt (số tiền bằng chữ), JWT | `mvn test` |
| Integration (backend) | Spring Boot Test + MockMvc + **Testcontainers MySQL 8.4** | Toàn bộ API luồng chính trên DB thật, Flyway migration, phân quyền, cô lập tenant | `mvn verify` (cần Docker) |
| Unit (frontend) | Vitest + Testing Library + jsdom | Định dạng tiền/ngày/lịch tuần, API client (lỗi, tự refresh token), form đăng nhập | `npm test` |
| E2E | Playwright (desktop Chrome + Pixel 7) | Luồng người dùng thật trên stack đang chạy với dữ liệu demo | `npx playwright test` |

Nguyên tắc: logic nghiệp vụ dễ sai (trùng lịch, tiền) được tách thành hàm thuần để unit test dày; integration test đi đúng thứ tự một ngày làm việc của trung tâm; E2E chỉ bao luồng chính, không kiểm từng trường.

## 2. Ma trận yêu cầu ↔ test

| User story | Test |
| --- | --- |
| US-1.1 Đăng nhập, lỗi chung, refresh | `ApiFlowIT#superAdminCreatesTwoTenants`, `#refreshTokenRotatesAndReuseIsRejected`, `client.test.ts`, `LoginPage.test.tsx`, mọi E2E |
| US-1.3 Tạo/khóa trung tâm, cô lập dữ liệu | `ApiFlowIT#superAdminCreatesTwoTenants`, `#tenantsAreIsolated`, `#suspendedTenantCannotLogin` |
| US-1.4 Nhân viên | `ApiFlowIT#ownerSetsUpCatalogStaffAndBank` |
| US-2.x Học viên, phụ huynh, validate SĐT | `ApiFlowIT#enrollRespectsCapacityAndDuplicates`, E2E `staff-flow` |
| US-3.4 Lịch tuần, trùng phòng/GV, sinh buổi | `ConflictDetectorTest`, `SessionGeneratorTest`, `ApiFlowIT#classScheduleGeneratesSessionsAndDetectsConflicts`, E2E "phát hiện trùng phòng" |
| US-3.5 Ghi danh: sĩ số, trùng | `ApiFlowIT#enrollRespectsCapacityAndDuplicates`, E2E `staff-flow` |
| US-5.1 Điểm danh GV, quyền theo lớp | `ApiFlowIT#teacherTakesAttendanceOnlyForOwnClass`, E2E mobile "giáo viên điểm danh" |
| US-6.2/6.3 Hóa đơn, thu nhiều lần, không thu vượt | `InvoiceMathTest`, `ApiFlowIT#invoicePartialPaymentsReceiptAndQr`, E2E `staff-flow` |
| US-6.4 Công nợ | `ApiFlowIT#invoicePartialPaymentsReceiptAndQr`, `#tenantsAreIsolated` |
| US-6.5 VietQR | `VietQrServiceTest`, `ApiFlowIT` (amount = số còn lại) |
| US-6.6 Phiếu thu PDF | `ApiFlowIT` (content-type + chữ ký `%PDF-`), `VnTextTest` (số tiền bằng chữ) |
| US-8.x Dashboard, Excel | `ApiFlowIT` (dashboard, `.xlsx` là file zip), E2E "báo cáo & cài đặt" |
| US-9.x Cổng phụ huynh | `ApiFlowIT#portalSeesOnlyOwnData`, E2E mobile "phụ huynh" |

## 3. Kịch bản kiểm thử thủ công (smoke trước khi bàn giao)
1. `docker compose up -d --build` → mở http://localhost:3000, đăng nhập 5 tài khoản demo, mỗi vai trò thấy đúng menu.
2. Giáo vụ: tạo lớp, đặt lịch trùng phòng → thấy cảnh báo đỏ ngay khi chọn giờ; đổi giờ → "Không trùng lịch".
3. Giáo viên (điện thoại thật): Hôm nay → Điểm danh → lưu < 1 phút cho 10 HV.
4. Hóa đơn: thu 2 lần, in phiếu thu PDF, kiểm tra dấu tiếng Việt & số tiền bằng chữ; quét QR bằng app ngân hàng thật (tên chủ TK, số tiền, nội dung).
5. Cài đặt → "Gửi nhắc học phí ngay" → xem email trong Mailpit (http://localhost:8025), nhật ký Zalo hiện "Thử (mock)".
6. Xuất 3 file Excel, mở bằng Excel/LibreOffice.

## 4. Kết quả lần chạy gần nhất (2026-11-07)

| Bộ test | Kết quả |
| --- | --- |
| Backend unit (`mvn test`) | **21/21 pass** |
| Backend integration Testcontainers (`mvn verify`) | **10/10 pass** |
| Frontend unit (`npm test`) | **12/12 pass** |
| Typecheck (`tsc -b`) + build | pass |
| E2E Playwright (5 kịch bản, 2 thiết bị) | **5/5 pass**, chạy lặp 2 lần liên tiếp vẫn pass |

Lỗi tìm ra & đã sửa nhờ test: NPE khi lớp chưa có giáo viên/phòng (`Map.of().get(null)`); thu hồi phiên khi phát hiện dùng lại refresh token bị rollback; lớp nhập vào hệ thống khi đã khai giảng không điểm danh bù được; ô chọn ngân hàng mất giá trị do danh sách tải sau form; CORS chặn khi chạy bản preview.

## 5. Ngoài phạm vi / rủi ro còn lại
- Chưa có test tải (load test). Chưa test trên Safari iOS thật.
- Gửi email thật qua SMTP và Zalo ZNS thật chưa kiểm thử (dùng Mailpit + mock).
- `docker compose build` chưa chạy được trong sandbox do Docker Hub giới hạn tần suất (429); CI GitHub Actions có job E2E chạy toàn bộ stack bằng compose.
