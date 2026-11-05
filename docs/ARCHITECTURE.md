# ARCHITECTURE – Zalo Mini App Starter Kit

> Vai trò: Solution Architect

## 1. Tổng quan

```mermaid
flowchart LR
  subgraph Zalo["Ứng dụng Zalo"]
    MA["Mini App<br/>React + zmp-sdk + zmp-ui"]
  end
  BR["Trình duyệt (Mock mode)<br/>Mini App + zmp-sdk giả lập"]
  AD["Admin Web<br/>React + Vite"]
  subgraph Server
    BE["Backend API<br/>Spring Boot 3 / Java 21"]
    DB[(MySQL 8)]
    FS[("Uploads<br/>(local volume / S3)")]
  end
  ZG["Zalo Graph API<br/>graph.zalo.me"]
  ZN["ZNS / OA API<br/>business.openapi.zalo.me"]
  ZP["Zalo Checkout<br/>(payment callback)"]

  MA -- "HTTPS + JWT" --> BE
  BR -- "HTTPS + JWT" --> BE
  AD -- "HTTPS + JWT" --> BE
  BE --> DB
  BE --> FS
  BE -- "verify access token<br/>decode phone/location" --> ZG
  BE -- "gửi thông báo" --> ZN
  ZP -- "notify (mac)" --> BE
```

- **Monorepo**: `/miniapp`, `/admin`, `/backend`, `/e2e`, `/docs`, `docker-compose.yml`.
- **Backend** là *modular monolith* chia theo tính năng (package-by-feature), mỗi tích hợp bên ngoài nằm sau một **interface + 2 implementation (mock / real)** chọn bằng cấu hình:

| Interface | Mock | Real | Thuộc tính |
|-----------|------|------|-----------|
| `ZaloGraphClient` | `MockZaloGraphClient` | `HttpZaloGraphClient` | `app.zalo.mock` |
| `NotificationGateway` | `MockNotificationGateway` | `ZnsNotificationGateway` | `app.notification.provider=mock|zns` |
| `PaymentProvider` | `MockPaymentProvider` | `ZaloCheckoutPaymentProvider` | `app.payment.provider=mock|zalo` |
| `StorageService` | – | `LocalStorageService` (S3 sau MVP) | `app.storage.*` |

- **Mini App**: mọi lời gọi zmp-sdk đi qua `src/zalo/bridge.ts` (interface `ZaloBridge`), có `realBridge` (import động `zmp-sdk/apis`) và `mockBridge`. Chọn theo `VITE_ZALO_MODE`.

## 2. Luồng chính

### 2.1 Đăng nhập Zalo
```mermaid
sequenceDiagram
  participant U as Người dùng
  participant M as Mini App
  participant B as Backend
  participant Z as Zalo Graph API
  U->>M: Mở Mini App
  M->>M: zmp-sdk getAccessToken()
  M->>B: POST /api/v1/auth/zalo {accessToken}
  B->>Z: GET /v2.0/me?fields=id,name,picture (header access_token)
  Z-->>B: {id, name, picture}
  B->>B: upsert user theo zalo_id
  B-->>M: {accessToken (JWT 15'), refreshToken (30 ngày), user}
  M->>M: lưu token (nativeStorage / localStorage)
```

### 2.2 Số điện thoại / vị trí (token → backend giải mã)
```mermaid
sequenceDiagram
  M->>M: getPhoneNumber() → {token}
  M->>B: POST /api/v1/me/phone {token, zaloAccessToken}
  B->>Z: GET /v2.0/me/info (headers: access_token, code=token, secret_key=APP_SECRET)
  Z-->>B: {data:{number:"849xxxxxxxx"}}
  B-->>M: user (phone đã chuẩn hoá 09xxxxxxxx)
```
> `secret_key` (App Secret) **chỉ nằm ở backend**. Zalo access token được Mini App gửi kèm vì backend không lưu token Zalo.

### 2.3 Refresh token (client)
1 request 401 `TOKEN_EXPIRED` → lớp API khoá "refreshing" (single-flight promise), các request khác chờ → gọi `/auth/refresh` → phát lại. Refresh lỗi → `logout()` + đăng nhập lại.

### 2.4 Thanh toán Checkout SDK
```mermaid
sequenceDiagram
  M->>B: POST /api/v1/orders {items:[{productId, quantity}]}
  B->>B: tính amount từ DB, tạo order PENDING, ký mac=HMAC_SHA256(privateKey, "amount=..&desc=..&extradata=..&item=..&method=..")
  B-->>M: {order, checkout:{amount, desc, item, extradata, method, mac}}
  M->>M: Payment.createOrder(checkout) (mock: sheet giả)
  Z-->>B: POST /api/v1/payments/zalo/notify {data, mac}
  B->>B: verify mac, idempotent → PAID
  M->>B: GET /api/v1/orders/{id} (poll trạng thái)
```

## 3. ERD

```mermaid
erDiagram
  users ||--o{ refresh_tokens : has
  users ||--o{ feedbacks : sends
  users ||--o{ orders : places
  users ||--o{ notification_logs : receives
  feedbacks ||--o{ feedback_images : has
  orders ||--o{ order_items : contains
  products ||--o{ order_items : referenced

  users {
    bigint id PK
    varchar zalo_id UK "null với admin"
    varchar email UK "null với user Zalo"
    varchar password_hash
    varchar name
    varchar avatar_url
    varchar phone
    varchar role "USER|EDITOR|ADMIN"
    varchar status "ACTIVE|LOCKED"
    decimal last_lat
    decimal last_lng
    datetime created_at
    datetime updated_at
  }
  refresh_tokens {
    bigint id PK
    bigint user_id FK
    char token_hash UK "SHA-256"
    datetime expires_at
    datetime revoked_at
    datetime created_at
  }
  articles {
    bigint id PK
    varchar title
    varchar slug UK
    varchar category
    varchar summary
    text content
    varchar thumbnail_url
    boolean published
    datetime published_at
    datetime created_at
    datetime updated_at
  }
  products {
    bigint id PK
    varchar name
    text description
    bigint price "VND"
    varchar image_url
    int stock
    boolean active
    boolean featured
    datetime created_at
    datetime updated_at
  }
  feedbacks {
    bigint id PK
    varchar code UK "PA-2026-000123"
    bigint user_id FK
    varchar category
    varchar title
    text content
    varchar address
    decimal lat
    decimal lng
    varchar status "NEW|IN_PROGRESS|RESOLVED|REJECTED"
    text admin_reply
    datetime created_at
    datetime updated_at
  }
  feedback_images {
    bigint id PK
    bigint feedback_id FK
    varchar url
  }
  orders {
    bigint id PK
    varchar code UK
    bigint user_id FK
    bigint amount
    varchar status "PENDING|PAID|FAILED|CANCELLED"
    varchar payment_method
    varchar provider_order_id
    varchar provider_trans_id
    datetime paid_at
    datetime created_at
    datetime updated_at
  }
  order_items {
    bigint id PK
    bigint order_id FK
    bigint product_id FK
    varchar product_name
    bigint unit_price
    int quantity
  }
  notification_logs {
    bigint id PK
    bigint user_id FK
    varchar channel "ZNS|OA"
    varchar template_id
    varchar recipient
    text payload
    varchar status "SENT|FAILED|MOCKED"
    varchar error
    datetime created_at
  }
```

## 4. API (prefix `/api/v1`, JSON, OpenAPI tại `/swagger-ui.html`)

| Method | Path | Quyền | Mô tả |
|--------|------|-------|-------|
| POST | `/auth/zalo` | public | Đăng nhập bằng Zalo access token |
| POST | `/auth/login` | public | Đăng nhập admin (email/mật khẩu) |
| POST | `/auth/refresh` | public | Đổi refresh token lấy cặp token mới |
| POST | `/auth/logout` | public | Thu hồi refresh token |
| GET | `/me` | auth | Thông tin tài khoản |
| PATCH | `/me` | auth | Sửa tên hiển thị |
| POST | `/me/phone` | USER | Giải mã token SĐT |
| POST | `/me/location` | USER | Giải mã token vị trí |
| GET | `/articles` | public | `?q=&category=&page=&size=` |
| GET | `/articles/{id}` | public | Chỉ tin đã xuất bản |
| GET | `/articles/categories` | public | Danh sách chuyên mục |
| GET | `/products` | public | `?q=&featured=&page=&size=` |
| GET | `/products/{id}` | public | |
| POST | `/feedbacks` | USER | multipart: `data` (JSON) + `images[]` |
| GET | `/feedbacks/me` | USER | Lịch sử phản ánh của tôi |
| GET | `/feedbacks/{id}` | owner/ADMIN | Chi tiết |
| GET | `/feedbacks/categories` | public | Chuyên mục phản ánh |
| POST | `/orders` | USER | Tạo đơn + tham số checkout |
| GET | `/orders/me` | USER | Đơn của tôi |
| GET | `/orders/{id}` | owner/ADMIN | |
| POST | `/orders/{id}/mock-pay` | owner (chỉ khi provider=mock) | Giả lập thanh toán |
| POST | `/payments/zalo/notify` | public (xác minh mac) | Callback Checkout |
| POST | `/uploads` | EDITOR+ | Upload ảnh cho nội dung admin |
| GET | `/admin/stats` | EDITOR+ | Số liệu dashboard |
| GET/POST/PUT/DELETE | `/admin/articles[/{id}]` | EDITOR+ | CRUD tin |
| GET/POST/PUT/DELETE | `/admin/products[/{id}]` | EDITOR+ | CRUD sản phẩm |
| GET | `/admin/feedbacks` | ADMIN | `?status=&q=` |
| GET | `/admin/feedbacks/{id}` | ADMIN | |
| PATCH | `/admin/feedbacks/{id}` | ADMIN | `{status, adminReply}` → gửi thông báo |
| GET | `/admin/users` | ADMIN | |
| GET | `/admin/orders` | ADMIN | |
| GET | `/admin/notifications` | ADMIN | Nhật ký thông báo |
| GET | `/public/config` | public | Cờ cấu hình cho client (mock mode, payment provider) |

**Định dạng lỗi** thống nhất:
```json
{ "status": 400, "code": "VALIDATION_ERROR", "message": "Dữ liệu không hợp lệ", "errors": {"title": "Tiêu đề phải từ 5–150 ký tự"}, "path": "/api/v1/feedbacks", "timestamp": "..." }
```
**Phân trang**: `{ "items": [...], "page": 0, "size": 10, "totalElements": 42, "totalPages": 5 }`.

## 5. Phân quyền

| Vai trò | Mini App | Nội dung (tin/SP) | Phản ánh (admin) | Người dùng/Đơn/Thông báo |
|---------|----------|-------------------|------------------|--------------------------|
| `USER` | ✅ | đọc | chỉ của mình | ❌ |
| `EDITOR` | – | ✅ CRUD | ❌ | ❌ |
| `ADMIN` | – | ✅ CRUD | ✅ | ✅ |

Thực thi bằng `SecurityFilterChain` (theo path) + `@PreAuthorize` + kiểm tra *ownership* trong service.

## 6. Bảo mật
- JWT HS256, secret ≥ 256 bit lấy từ `APP_JWT_SECRET`; access 15', refresh 30 ngày, refresh lưu **SHA-256 hash**, rotation + thu hồi.
- BCrypt (strength 10) cho mật khẩu admin.
- Bean Validation cho mọi input; JPA tham số hoá (không nối chuỗi SQL); `LIKE` escape ký tự đặc biệt.
- Upload: whitelist MIME + kiểm tra magic bytes, tên file sinh ngẫu nhiên (UUID), giới hạn kích thước, phục vụ với `Content-Disposition` an toàn và `X-Content-Type-Options: nosniff`.
- CORS whitelist qua `APP_CORS_ALLOWED_ORIGINS` (mặc định có `h5.zdn.vn`, `zalo.me` cho Mini App thật).
- Callback thanh toán xác minh HMAC bằng so sánh constant-time.
- Không log token/secret; SĐT được che (`091****678`) trong log.

## 7. Cấu trúc thư mục

```
.
├── backend/                     # Spring Boot
│   ├── src/main/java/vn/starter/zma/
│   │   ├── ZmaApplication.java
│   │   ├── common/              # exception, dto chung, util, config
│   │   ├── security/            # JwtService, JwtAuthFilter, SecurityConfig
│   │   ├── auth/                # AuthController, AuthService, RefreshToken
│   │   ├── user/                # User, MeController, AdminUserController
│   │   ├── zalo/                # ZaloGraphClient (+mock/http), ZaloProperties
│   │   ├── content/             # Article, Product (+ admin controllers)
│   │   ├── feedback/            # Feedback, FeedbackImage
│   │   ├── order/               # Order, PaymentProvider (+mock/zalo checkout)
│   │   ├── notification/        # NotificationGateway (+mock/zns), NotificationLog
│   │   ├── storage/             # StorageService, LocalStorageService
│   │   └── admin/               # StatsController
│   ├── src/main/resources/db/migration/  # Flyway V1__init.sql, V2__seed.sql
│   └── src/test/java/...        # unit + integration (Testcontainers)
├── miniapp/                     # Zalo Mini App
│   ├── app-config.json          # cấu hình ZMA (title, header, màu)
│   ├── zmp-cli.json             # appId (không commit secret)
│   └── src/
│       ├── zalo/                # bridge.ts, realBridge.ts, mockBridge.ts, MockPanel
│       ├── api/                 # client.ts (fetch + JWT + refresh), endpoints
│       ├── auth/                # AuthProvider
│       ├── components/          # UI dùng chung
│       ├── pages/               # Home, News, Product, Feedback, History, Profile
│       └── theme/               # tokens.css
├── admin/                       # Admin web (React + Vite)
├── e2e/                         # Playwright
├── docs/
├── docker-compose.yml
└── .github/workflows/ci.yml
```
