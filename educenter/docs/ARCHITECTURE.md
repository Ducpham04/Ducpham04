# ARCHITECTURE – EduCenter

## 1. Tổng quan

```mermaid
flowchart LR
  subgraph Client
    W[React SPA<br/>Vite + TS + Tailwind]
  end
  subgraph Server[Spring Boot 3 · Java 21]
    F[JwtAuthFilter] --> C[REST Controllers]
    C --> S[Services<br/>tenant guard + business rules]
    S --> R[JPA Repositories]
    SCH[Schedulers<br/>nhắc lịch / học phí] --> S
    S --> N[Notification ports<br/>EmailSender · ZnsClient]
    S --> P[PDF (OpenPDF) · Excel (POI)]
  end
  W -- "HTTPS JSON /api/v1" --> F
  R --> DB[(MySQL 8<br/>Flyway)]
  N --> SMTP[(SMTP / Mailpit)]
  N -.mock.-> ZALO[(Zalo ZNS API)]
  W -- "ảnh QR" --> VQ[(img.vietqr.io)]
```

- **Modular monolith**: mỗi nghiệp vụ một package (`student`, `classes`, `billing`…) có controller → service → repository riêng; giao tiếp qua service, không gọi repository chéo module trừ khi chỉ đọc.
- **Stateless API** (JWT), SPA phục vụ bởi Nginx trong Docker, proxy `/api` sang backend.
- **Multi-tenant: shared database, shared schema, cột `tenant_id`** trên mọi bảng nghiệp vụ. Tenant lấy từ JWT (claim `tid`) → `CurrentUser` → mọi truy vấn repository đều có `tenantId` (`findByIdAndTenantId`, `…WhereTenantId`). Không bao giờ nhận `tenantId` từ request body/query (trừ API SUPER_ADMIN).

## 2. ERD

```mermaid
erDiagram
  TENANTS ||--o{ USERS : has
  TENANTS ||--o{ STUDENTS : has
  STUDENTS ||--o{ GUARDIANS : has
  STUDENTS ||--o| USERS : "portal account"
  TENANTS ||--o{ COURSES : has
  TENANTS ||--o{ ROOMS : has
  COURSES ||--o{ CLASSES : has
  USERS ||--o{ CLASSES : teaches
  ROOMS ||--o{ CLASSES : "default room"
  CLASSES ||--o{ CLASS_SCHEDULES : "weekly slots"
  CLASSES ||--o{ ENROLLMENTS : has
  STUDENTS ||--o{ ENROLLMENTS : has
  CLASSES ||--o{ CLASS_SESSIONS : generates
  CLASS_SESSIONS ||--o{ ATTENDANCES : has
  STUDENTS ||--o{ ATTENDANCES : has
  COURSES ||--o{ FEE_PLANS : "optional"
  STUDENTS ||--o{ INVOICES : billed
  FEE_PLANS ||--o{ INVOICES : "optional"
  INVOICES ||--o{ PAYMENTS : has
  USERS ||--o{ REFRESH_TOKENS : has
  TENANTS ||--o{ NOTIFICATION_LOGS : has

  TENANTS { bigint id PK; string name; string slug UK; string plan; string status; string bank_code; string bank_account_no; string bank_account_name }
  USERS { bigint id PK; bigint tenant_id FK "null = SUPER_ADMIN"; string email UK; string password_hash; string full_name; string role; bigint student_id FK; bool enabled }
  STUDENTS { bigint id PK; bigint tenant_id FK; string code; string full_name; date dob; string phone; string email; string status; date joined_at }
  GUARDIANS { bigint id PK; bigint tenant_id; bigint student_id FK; string full_name; string relationship; string phone; string email; bool is_primary }
  COURSES { bigint id PK; bigint tenant_id; string code; string name; string level; int total_sessions; decimal tuition_fee; bool active }
  ROOMS { bigint id PK; bigint tenant_id; string name; int capacity; bool active }
  CLASSES { bigint id PK; bigint tenant_id; bigint course_id FK; string code; string name; bigint teacher_id FK; bigint room_id FK; int max_students; date start_date; date end_date; string status }
  CLASS_SCHEDULES { bigint id PK; bigint tenant_id; bigint class_id FK; int day_of_week "1=T2..7=CN"; time start_time; time end_time }
  ENROLLMENTS { bigint id PK; bigint tenant_id; bigint class_id FK; bigint student_id FK; date start_date; date end_date; string status }
  CLASS_SESSIONS { bigint id PK; bigint tenant_id; bigint class_id FK; date session_date; time start_time; time end_time; bigint room_id; bigint teacher_id; string status; string note }
  ATTENDANCES { bigint id PK; bigint tenant_id; bigint session_id FK; bigint student_id FK; string status; string note; bigint marked_by }
  FEE_PLANS { bigint id PK; bigint tenant_id; string name; bigint course_id; decimal amount; int sessions; bool active }
  INVOICES { bigint id PK; bigint tenant_id; string code; bigint student_id FK; bigint class_id; bigint fee_plan_id; string description; decimal subtotal; decimal discount; decimal total; decimal paid_amount; date due_date; string status; datetime last_reminded_at }
  PAYMENTS { bigint id PK; bigint tenant_id; bigint invoice_id FK; string receipt_no; decimal amount; string method; datetime paid_at; bigint received_by }
  REFRESH_TOKENS { bigint id PK; bigint user_id FK; string token_hash UK; datetime expires_at; datetime revoked_at }
  NOTIFICATION_LOGS { bigint id PK; bigint tenant_id; string channel; string type; string recipient; string status; string ref_key; datetime created_at }
  CODE_SEQUENCES { bigint tenant_id PK; string seq_key PK; bigint next_value }
```

Ràng buộc chính:
- `UNIQUE(tenant_id, code)` cho students/courses/classes/invoices; `UNIQUE(session_id, student_id)` cho attendances; `UNIQUE(class_id, student_id, status)` không dùng – kiểm tra ghi danh trùng ở service.
- Tiền dùng `DECIMAL(15,0)` (VND không có xu).
- Index: `(tenant_id, session_date)` trên class_sessions; `(tenant_id, status, due_date)` trên invoices; `(tenant_id, paid_at)` trên payments.

## 3. Phân quyền

| Tài nguyên | SUPER_ADMIN | CENTER_ADMIN | STAFF | TEACHER | STUDENT |
| --- | --- | --- | --- | --- | --- |
| Trung tâm (tenants) | CRUD | xem/sửa trung tâm mình | – | – | – |
| Nhân viên (users) | – | CRUD | xem GV | – | – |
| Học viên, phụ huynh | – | CRUD | CRUD | xem HV lớp mình | – |
| Khóa học, phòng | – | CRUD | CRUD | xem | – |
| Lớp, lịch tuần, ghi danh | – | CRUD | CRUD | xem lớp mình | – |
| Lịch / buổi học | – | CRUD | CRUD | xem lịch mình | xem (cổng) |
| Điểm danh | – | ghi/xem | ghi/xem | ghi/xem lớp mình | xem (cổng) |
| Học phí, hóa đơn, thanh toán | – | CRUD | CRUD | – | xem (cổng) |
| Báo cáo, Excel | – | ✔ | ✔ (trừ doanh thu năm*) | – | – |
| Cài đặt, nhắc nhở | – | ✔ | – | – | – |

\* MVP: STAFF xem được báo cáo (giáo vụ thường cần), ghi trong DECISIONS.

Thực thi 2 lớp: `@PreAuthorize` theo vai trò trên controller + kiểm tra **quyền sở hữu** trong service (TEACHER chỉ lớp mình, STUDENT chỉ `student_id` của mình, mọi vai trò chỉ `tenant_id` của mình).

## 4. API (prefix `/api/v1`, JSON, lỗi theo RFC 7807 `ProblemDetail`)

| Nhóm | Method & path | Vai trò |
| --- | --- | --- |
| Auth | `POST /auth/login` · `POST /auth/refresh` · `POST /auth/logout` · `GET /auth/me` · `POST /auth/change-password` | public / đã đăng nhập |
| Tenants | `GET/POST /admin/tenants` · `PUT /admin/tenants/{id}` · `PATCH /admin/tenants/{id}/status` | SUPER_ADMIN |
| Center | `GET/PUT /center` (thông tin + ngân hàng) | ADMIN (GET: mọi vai trò trong tenant) |
| Users | `GET/POST /users` · `PUT /users/{id}` · `PATCH /users/{id}/enabled` · `GET /users/teachers` | ADMIN (teachers: ADMIN, STAFF) |
| Students | `GET /students?q&status&page&size` · `POST /students` · `GET/PUT /students/{id}` · `PATCH /students/{id}/status` · `GET /students/{id}/enrollments` · `GET /students/{id}/attendance` · `POST /students/{id}/portal-account` | ADMIN, STAFF (GET: + TEACHER) |
| Guardians | `GET/POST /students/{id}/guardians` · `PUT/DELETE /guardians/{id}` | ADMIN, STAFF |
| Courses | `GET/POST /courses` · `PUT/DELETE /courses/{id}` | ADMIN, STAFF |
| Rooms | `GET/POST /rooms` · `PUT/DELETE /rooms/{id}` | ADMIN, STAFF |
| Classes | `GET /classes?q&status&teacherId` · `POST /classes` · `GET/PUT /classes/{id}` · `PUT /classes/{id}/schedules` · `GET /classes/{id}/students` · `POST /classes/{id}/enrollments` · `DELETE /enrollments/{id}` · `GET /classes/{id}/sessions` | ADMIN, STAFF (GET: + TEACHER lớp mình) |
| Schedule | `GET /sessions?from&to&teacherId&roomId&classId` · `GET /sessions/today` · `PATCH /sessions/{id}` (đổi giờ/phòng/hủy) · `POST /schedule/check-conflicts` | đã đăng nhập (TEACHER bị ép teacherId) |
| Attendance | `GET /sessions/{id}/attendance` · `PUT /sessions/{id}/attendance` · `GET /reports/attendance/classes` | ADMIN, STAFF, TEACHER (lớp mình) |
| Fee plans | `GET/POST /fee-plans` · `PUT/DELETE /fee-plans/{id}` | ADMIN, STAFF |
| Invoices | `GET /invoices?status&studentId&q&page` · `POST /invoices` · `GET /invoices/{id}` · `POST /invoices/{id}/cancel` · `GET /invoices/{id}/vietqr` · `GET /debts` | ADMIN, STAFF |
| Payments | `POST /invoices/{id}/payments` · `GET /payments/{id}/receipt.pdf` | ADMIN, STAFF |
| Reports | `GET /reports/dashboard` · `GET /reports/revenue?year` · `GET /reports/new-students?year` · `GET /reports/export/{revenue|debts|attendance}.xlsx` | ADMIN, STAFF |
| Notifications | `GET /notifications/logs` · `POST /notifications/run/{sessions|fees}` | ADMIN |
| Portal | `GET /portal/me` · `GET /portal/sessions` · `GET /portal/attendance` · `GET /portal/invoices` · `GET /portal/invoices/{id}/vietqr` | STUDENT |

OpenAPI UI: `/swagger-ui.html` (tắt ở profile `prod` bằng biến `SPRINGDOC_ENABLED=false`).

## 5. Luồng chính

**Lưu lịch tuần → sinh buổi học**
1. `PUT /classes/{id}/schedules` với danh sách slot.
2. `ScheduleConflictService` so từng slot với slot của lớp khác *cùng tenant, trạng thái chưa kết thúc/hủy, khoảng ngày giao nhau, cùng thứ, giờ giao nhau* và *cùng phòng hoặc cùng giáo viên* → có xung đột ném `ConflictException` (409) kèm danh sách.
3. Xóa các buổi tương lai `SCHEDULED` chưa điểm danh, sinh lại buổi từ `max(today, start_date)` đến `end_date` (giới hạn 180 ngày).

**Thanh toán** – `PaymentService.record` khóa hóa đơn (`PESSIMISTIC_WRITE`), kiểm tra số tiền ≤ còn lại, tăng `paid_amount`, tính lại trạng thái, sinh số phiếu thu `PT2611-0001` từ `code_sequences`.

**Nhắc nhở** – `ReminderScheduler` (`@Scheduled(cron, zone=Asia/Ho_Chi_Minh)`) → `ReminderService` duyệt từng tenant ACTIVE → gửi qua các `NotificationChannel` (Email luôn bật, Zalo ZNS mock) → ghi `notification_logs` (khóa `ref_key` chống gửi trùng trong ngày).

## 6. Bảo mật
- BCrypt (strength 10); access JWT HS256 15 phút (claims: `sub`=userId, `tid`, `role`, `sid`=studentId); refresh token ngẫu nhiên 256-bit, lưu **SHA-256 hash**, xoay vòng mỗi lần refresh, thu hồi khi logout/đổi mật khẩu.
- Secret qua biến môi trường `JWT_SECRET` (≥ 32 byte), app từ chối khởi động nếu thiếu ở profile `prod`.
- Validation Jakarta Bean Validation trên mọi DTO; JPA tham số hóa → không SQL injection; không dùng native query nối chuỗi.
- CORS chỉ cho origin cấu hình; Lỗi trả `ProblemDetail` không lộ stack trace; rate-limit đăng nhập đơn giản theo IP (bucket trong bộ nhớ).

## 7. Cấu trúc thư mục

```
educenter/
├── backend/                       Spring Boot (Maven)
│   └── src/main/java/vn/educenter/
│       ├── EduCenterApplication.java
│       ├── common/                ApiError, PageResponse, exceptions, CodeSequenceService, VnText
│       ├── config/                SecurityConfig, OpenApiConfig, AppProperties
│       ├── security/              JwtService, JwtAuthFilter, CurrentUser, LoginRateLimiter
│       ├── auth/  tenant/  user/  student/  catalog/  classes/
│       ├── attendance/  billing/  notification/  report/  portal/
│       └── seed/                  DemoDataSeeder (dữ liệu demo tiếng Việt)
│   └── src/main/resources/db/migration/V1__init.sql
├── frontend/                      React + Vite
│   └── src/{api,components,features,hooks,lib,layouts,routes}
├── e2e/                           Playwright (luồng chính)
├── docs/
├── docker-compose.yml  .env.example  README.md
└── ../.github/workflows/educenter-ci.yml
```
