# AGENTS.md — N8 Cineplex

Source of truth: `N8_Cineplex_Plan.md`.

Mọi coding agent phải đọc phần liên quan trong plan trước khi thay đổi kiến trúc, database, API hoặc business rule.

Đây là đồ án môn học đã chủ động giới hạn scope. Không tự ý đưa lại các cơ chế enterprise đã bị loại bỏ.

## Quy tắc bắt buộc: cập nhật tài liệu và tiến độ

Áp dụng cho mọi thành viên và coding agent, trong mọi giai đoạn của đồ án.

1. **Mọi thay đổi hoặc bổ sung đều phải cập nhật cả `N8_Cineplex_Plan.md` và `AGENTS.md`:** bao gồm requirement, scope, kiến trúc, database, API, business rule, code, cấu hình, triển khai, kiểm thử và hướng dẫn. Thay đổi tiến độ cũng phải ghi vào cả hai file, kể cả khi spec không đổi.
2. **Đúng vai trò của từng file:** Plan là source of truth, ghi đặc tả/quyết định và tiến độ chi tiết; AGENTS ghi quy tắc thực hiện tương ứng và tóm tắt tiến độ để agent tiếp theo tiếp tục đúng trạng thái. Không cần sao chép toàn bộ Plan vào AGENTS, nhưng quyết định và trạng thái trong hai file phải nhất quán.
3. **Cập nhật ngay trong cùng đợt làm việc:** sửa mục liên quan và ghi tiến độ trước khi báo hoàn thành hoặc commit/push thay đổi. Khi commit/push, đưa cả hai file vào cùng đợt với thay đổi tương ứng; không để tài liệu lỗi thời hoặc chỉ cập nhật một file.
4. **Ghi rõ tiến độ:** ngày cập nhật, hạng mục, trạng thái (`Chưa làm`, `Đang làm`, `Bị chặn`, `Hoàn thành`), phần đã xong, phần còn lại và bước tiếp theo. Nếu bị chặn, ghi nguyên nhân; nếu đã hoàn thành, cập nhật trạng thái trong cả hai file.
5. **Chỉ ghi hoàn thành khi có bằng chứng:** nêu kiểm tra đã chạy và kết quả thực tế; thêm commit SHA/PR khi đã có, không bịa hoặc điền trước. Với backend, ghi riêng kết quả local và VPS; chưa deploy/kiểm tra VPS thì vẫn ghi phần đó chưa hoàn thành theo quy tắc triển khai hiện có. Chỉ sửa tài liệu, không đổi source thì không cần rebuild/deploy API.
6. **Giữ lịch sử và bảo mật:** giữ các kết quả đã xác minh kèm ngày; quyết định mới phải chỉ rõ phần thay thế nếu có. Không suy diễn tính năng trong plan là đã được triển khai. Không ghi secrets, private key hoặc dữ liệu nhạy cảm; bằng chứng chỉ ghi tóm tắt/kết quả và tham chiếu an toàn, không đưa selftest tạm hoặc log thô vào repo.
7. **Kiểm tra trước khi kết thúc:** đọc lại cả hai file, đối chiếu spec với thay đổi thực tế và đối chiếu tiến độ giữa hai file. Nếu chưa đồng bộ tài liệu thì nhiệm vụ chưa hoàn thành.

### Nhật ký cập nhật tài liệu và tiến độ

| Ngày | Hạng mục | Trạng thái | Kết quả / phần còn lại |
| --- | --- | --- | --- |
| 2026-10-05 | Bổ sung quy tắc cập nhật đồng bộ tài liệu và tiến độ | Hoàn thành phần tài liệu | Đã cập nhật cả hai file; kiểm tra nội dung đồng bộ, giữ nguyên nội dung cũ và rollback trên bản sao đạt. Chưa commit/push; người dùng sẽ push lên GitHub. Không đổi source backend, không chạy lại kiểm tra/deploy VPS. |


---

## 1. Mục tiêu

Ưu tiên:

```text
DỄ HIỂU
DỄ VẼ ERD
DỄ CODE
DỄ TEST
DỄ DEMO
```

Nhưng phải giữ các điểm kỹ thuật quan trọng:

- REST API;
- WebSocket realtime;
- relational database;
- transaction/concurrency;
- JWT + refresh token;
- Google Login;
- crawler Playwright;
- payment webhook thật;
- QR ticket;
- FCM;
- email ticket;
- Docker Compose;
- Swagger.

---

## 2. Stack

### Android

- Kotlin
- Jetpack Compose
- Material 3
- ViewModel + StateFlow
- Coroutines
- Navigation Compose
- Retrofit + OkHttp
- Kotlin Serialization
- DataStore
- Android Keystore
- Coil
- CameraX
- ML Kit Barcode Scanning
- Firebase Cloud Messaging
- Socket.IO client

### Backend

- Node.js
- TypeScript
- NestJS
- Prisma
- PostgreSQL
- REST
- Socket.IO/WebSocket
- Swagger/OpenAPI
- `@nestjs/schedule`
- Nodemailer

### Crawler

- Playwright
- chạy trong NestJS scheduled service

### Deployment

- Ubuntu/Linux
- Docker Compose
- Nginx/Caddy
- HTTPS

---

## 3. Không dùng

Không thêm lại nếu requirement chưa thay đổi rõ ràng:

- microservices
- Kafka
- Kubernetes
- CQRS framework
- event sourcing
- Elasticsearch
- Redis
- BullMQ
- outbox_events
- audit_logs table
- idempotency_records table
- token family/reuse detection phức tạp
- payment reconciliation phức tạp
- JSONB cho domain data

Không được xóa WebSocket/Socket.IO khỏi kiến trúc hiện tại.

---

## 4. Architecture

Use modular monolith.

```text
Android
  ├─ REST
  └─ Socket.IO
        ↓
      NestJS
        ↓
    PostgreSQL
```

Integrations:

```text
Playwright
SePay/VietQR
FCM
Gmail SMTP
APK hosting
```

PostgreSQL là source of truth.

WebSocket chỉ emit sau DB commit.

Reconnect socket phải fetch REST lại.

Không cần Redis adapter vì target deployment là một NestJS instance.

---

## 5. Roles

```text
CUSTOMER
STAFF
ADMIN
```

### STAFF

Staff chỉ:

- login
- scan ticket QR
- check-in
- redeem combo

Staff **không**:

- xử lý crawler
- review movie/showtime
- xử lý payment issue
- reconciliation
- manual finalize payment
- sửa order/payment state

### ADMIN

Admin:

- quản lý Staff
- CRUD combo
- xem movie/showtime/order
- cancel showtime
- xem payment issues
- mark payment issue bank-refunded/resolved

Không xây reconciliation workflow lớn.

---

## 6. Database — hard limit

Database phải có đúng **10 bảng nghiệp vụ**:

```text
1. users
2. movies
3. auditoriums
4. showtimes
5. showtime_seats
6. combos
7. orders
8. order_seats
9. payments
10. tickets
```

Rules:

- PostgreSQL relational database cơ bản;
- **không JSONB**;
- mỗi bảng khoảng 7–12 fields;
- không tạo bảng mới nếu chưa có thay đổi requirement rõ ràng;
- ưu tiên FK, UNIQUE, transaction.

Không tạo lại bảng `seats`.

Seat layout nằm trong code template; inventory thực tế nằm ở `showtime_seats`.

---

## 7. Canonical fields

### `users`

```text
id
email
password_hash nullable
google_id nullable
full_name
avatar_url nullable
role
is_active
email_verified_at nullable
credit_balance
refresh_token_hash nullable
fcm_token nullable
```

### `movies`

```text
id
title
poster_url nullable
duration_minutes
synopsis nullable
age_rating
genre nullable
language nullable
source_url
updated_at
```

### `auditoriums`

```text
id
name
type
capacity
is_extra
status
created_at
updated_at
```

### `showtimes`

```text
id
movie_id
auditorium_id
start_at
end_at
format
base_price
status
source_key
created_at
updated_at
```

### `showtime_seats`

```text
id
showtime_id
seat_code
row_name
seat_number
seat_type
price
status
held_by_user_id nullable
hold_expires_at nullable
updated_at
```

### `combos`

```text
id
name
description
price
image_url nullable
status
created_at
updated_at
```

### `orders`

```text
id
user_id
showtime_id
combo_id nullable
combo_quantity
subtotal
discount
credit_used
total_amount
status
expires_at
created_at
```

### `order_seats`

```text
id
order_id
showtime_seat_id
seat_code
price
status
created_at
```

### `payments`

```text
id
order_id
payment_code
expected_amount
received_amount nullable
provider_transaction_id nullable
transfer_content nullable
status
issue_reason nullable
expires_at
paid_at nullable
created_at
```

### `tickets`

```text
id
order_id
booking_code
qr_token_hash
status
issued_at
checked_in_at nullable
combo_redeemed_at nullable
refunded_at nullable
cancelled_at nullable
email_sent_at nullable
expires_at
```

Do not add fields casually. If a new field is needed, first check whether an existing field can satisfy the requirement without harming data integrity.

---

## 8. Auth

Support:

- register
- verify email
- login
- logout
- forgot/reset password
- change password
- Google login
- refresh token

Email normalize `trim + lowercase`.

Booking requires:

```text
is_active = true
AND email_verified_at IS NOT NULL
```

Refresh token model is intentionally simple:

```text
one current refresh_token_hash per user
```

Login replaces old refresh token hash.

Logout clears it.

Do not create `refresh_tokens` table.

---

## 9. Crawler

Use Playwright.

Flow:

```text
cron
→ crawl CGV Landmark 81
→ normalize
→ upsert movies/showtimes
→ assign room
→ materialize showtime_seats
```

If required movie/showtime data is missing:

```text
skip + log
```

Do not create `needs_review` workflow.

Do not require Staff intervention.

---

## 10. Room assignment

Assign compatible free room.

If all compatible rooms are busy:

```text
auto-create logical extra room
```

No Staff handling.

Seat layout comes from in-code template by auditorium type.

---

## 11. Seat hold

Hard duration:

```text
600 seconds
```

Creation transaction:

```text
lock selected showtime_seats
→ require AVAILABLE
→ create order PENDING_PAYMENT
→ create order_seats HELD
→ showtime_seats HELD
→ set held_by_user_id + hold_expires_at
```

After commit emit:

```text
SEAT_HELD
```

Expiry cron:

```text
order EXPIRED
order_seats CANCELED
showtime_seats AVAILABLE
restore order.credit_used once
```

After commit emit:

```text
SEAT_RELEASED
ORDER_EXPIRED
```

---

## 12. Double booking

PostgreSQL is authority.

Require:

```text
UNIQUE(showtime_id, seat_code)
```

Use transaction + row/state checks.

Client cannot decide seat status.

Do not remove transaction protection just because Redis is absent.

---

## 13. Pricing

Backend calculates all prices.

```text
subtotal = seats + combo
final_total = subtotal - discount
credit_used = min(user.credit_balance, final_total)
bank_amount = final_total - credit_used
```

Promotion is code/config, not table.

Only one best promotion.

---

## 14. Refund Credit

Store in:

```text
users.credit_balance
```

At order creation, deduct `credit_used` inside transaction.

If order expires/cancels before PAID, restore exactly once.

Self-refund adds `orders.total_amount` to credit.

No wallet/ledger tables.

---

## 15. Combo

One order supports:

```text
0 or 1 combo type
+ quantity
```

No product/item/option tables.

---

## 16. Payment

Real payment via SePay + VietQR.

Payment statuses:

```text
PENDING
PAID
EXPIRED
NEEDS_ADMIN
RESOLVED
```

Issue reasons:

```text
WRONG_AMOUNT
WRONG_REFERENCE
LATE_PAYMENT
```

Successful finalization transaction:

```text
payment PAID
order PAID
order_seats ACTIVE
showtime_seats SOLD
ticket VALID
```

After commit:

```text
PAYMENT_PAID
SEAT_SOLD
FCM
email ticket
```

Wrong/late payment:

```text
payment NEEDS_ADMIN
```

Staff never handles it.

Admin can only mark bank-refunded/resolved.

Do not revive an expired order.

### Idempotency without table

Use:

- unique `payment_code`
- unique `provider_transaction_id`
- transaction
- state guard

Duplicate webhook must not issue a second ticket or spend credit twice.

---

## 17. `order_seats` is mandatory

Do not remove `order_seats`.

Reason:

```text
seat A1 sold to Order 100
→ Order 100 refunded
→ A1 becomes AVAILABLE
→ A1 sold to Order 200
```

History must still preserve:

```text
Order 100 → A1 → REFUNDED
Order 200 → A1 → ACTIVE
```

`showtime_seats` represents current seat inventory.

`order_seats` represents booking history.

---

## 18. Ticket

One order = one ticket = one QR.

Status:

```text
VALID
USED
REFUNDED
CANCELLED
EXPIRED
```

Check-in window:

```text
T-90 to T+30
```

Staff can only:

```text
VALID → USED
```

Combo redeem is stored by `combo_redeemed_at`.

---

## 19. Self-refund

Eligibility:

- order PAID
- ticket VALID
- not checked in
- combo not redeemed
- >=45 min before showtime
- not already refunded

Transaction:

```text
order REFUNDED
ticket REFUNDED
order_seats REFUNDED
showtime_seats AVAILABLE
users.credit_balance += orders.total_amount
```

After commit emit FCM/realtime.

No monthly quota.

---

## 20. Showtime cancellation

Admin only.

Before showtime start.

Reject automatic cancellation if any ticket is already USED.

Pending orders:

```text
CANCELED + release seat + restore credit
```

Paid valid tickets:

```text
CANCELED/CANCELLED + release seat + add total to credit
```

Staff does not process any order manually.

---

## 21. WebSocket

Channels:

```text
showtime:{id}
user:{id}
```

Events:

```text
SEAT_HELD
SEAT_RELEASED
SEAT_SOLD
PAYMENT_PAID
ORDER_EXPIRED
TICKET_REFUNDED
SHOWTIME_CANCELLED
```

Rules:

- JWT handshake auth
- emit only after DB commit
- WebSocket is not source of truth
- reconnect → REST refetch
- polling fallback allowed

Do not require Redis for Socket.IO in single-instance deployment.

---

## 22. Email ticket

After ticket issue:

```text
send email to users.email
```

Provider:

```text
Nodemailer + Gmail SMTP
```

Email includes:

- movie
- showtime
- auditorium
- seats
- combo
- amounts
- booking code
- QR

If email fails:

```text
booking stays PAID
 ticket stays VALID
 email_sent_at stays NULL
```

Cron retries later.

Never rollback a paid booking because email failed.

---

## 23. FCM

Use `users.fcm_token` as one current token per user.

Push:

- payment success
- ticket issued
- T-1h reminder
- showtime cancelled
- refund completed

FCM is delivery only, not source of truth.

---

## 24. Required update

No app release table.

Read config from env:

```text
LATEST_VERSION_CODE
MIN_SUPPORTED_VERSION_CODE
APK_URL
APK_SHA256
```

Old app gets HTTP 426.

Verify SHA-256 before installer.

---

## 25. Background jobs

Use NestJS Schedule only:

```text
crawl-cgv
expire-seat-holds
expire-tickets
send-ticket-reminders
retry-ticket-email
```

Do not add BullMQ unless requirements explicitly change.

---

## 26. API expectations

REST endpoints must follow the plan.

Important commands:

```text
POST /orders
POST /orders/:id/payment
POST /webhooks/sepay
POST /tickets/:id/refund
POST /staff/tickets/:id/check-in
POST /staff/tickets/:id/redeem-combo
POST /admin/showtimes/:id/cancel
POST /admin/payment-issues/:id/mark-bank-refunded
POST /admin/payment-issues/:id/resolve
```

Never expose generic endpoints that let clients directly set:

```text
order.status
payment.status
ticket.status
credit_balance
seat.status
role
```

---

## 27. Security

Required:

- HTTPS
- password hash
- JWT access token
- refresh token hash
- Android Keystore
- DTO validation
- role guard
- object ownership checks
- verified email before booking
- webhook validation
- unique payment transaction ID
- transaction for booking/payment/refund
- secrets in env
- opaque QR
- avatar validation
- auth rate limit
- crawler URL allowlist

Do not store bank password/OTP/PAN/CVV.

---

## 28. Testing priorities

Must cover:

### Auth

- register/verify/login
- refresh/logout
- Google login
- duplicate email
- unverified booking reject

### Crawler

- parse/upsert
- skip invalid showtime
- room assignment
- materialize seats

### Seat

- same-seat concurrency
- 10-minute expiry
- realtime hold/release
- couple pair rule

### Order history

- refund then resale preserves old `order_seats` record

### Payment

- exact payment → PAID
- duplicate webhook idempotent
- wrong/late → NEEDS_ADMIN
- expired order never revived

### WebSocket

- events after commit
- reconnect refetch
- polling fallback

### Ticket

- QR scan
- duplicate scan reject
- combo redeem once
- expire T+30

### Refund

- success >=45m
- reject <45m
- reject USED/redeemed
- seat release
- credit added once

### Email

- PAID → email
- QR in email
- email failure does not rollback
- retry updates email_sent_at

---

## 29. Coding rules

Before coding:

1. read relevant plan section;
2. preserve the 10-table limit;
3. preserve relational design and `order_seats` history;
4. preserve PostgreSQL transaction/state checks;
5. preserve WebSocket realtime + REST fallback;
6. keep Staff scope minimal;
7. do not reintroduce enterprise infrastructure without explicit requirement;
8. write/update tests;
9. run formatter/linter/tests.

Priority when instructions conflict:

1. newest explicit user instruction
2. `N8_Cineplex_Plan.md`
3. `AGENTS.md`
4. existing code

Existing code never overrides the plan.

---

## 30. Quyết định bổ sung cho tuần 4


## Tuần 4 — cấu hình triển khai đã chốt

Đây là quyết định cấu hình; chỉ ghi nhận triển khai thành công khi có kết quả kiểm tra thực tế.

- VPS dùng chung: Ubuntu 24.04, IP `18.143.100.43`, user SSH **`ubuntu`**.
- Đăng nhập VPS bằng SSH key **file `.ppk`**, không dùng password. Key hiện tại của Trần Thanh Nguyên: `D:\ssh\shibakey.ppk`; dùng PuTTY/Plink trực tiếp với định dạng này.
- Theo quyết định của nhóm, Trần Thanh Nguyên sẽ chia sẻ file `.ppk` qua kênh riêng cho các thành viên được phép truy cập VPS bằng user `ubuntu`. Không đưa private key vào Git, source code, database backup hoặc tài liệu công khai. Dùng chung key đồng nghĩa cùng quyền truy cập và khó xác định người thao tác; khi key lộ hoặc thành viên rời nhóm phải thay/thu hồi key. Key riêng từng người là hướng nâng cấp sau này, không phải điều kiện chặn tuần 4.
- Một PostgreSQL 17 database `n8_cineplex` dùng chung cho cả development và testing; không tạo database test riêng từng thành viên. PostgreSQL là source of truth; giữ transaction/state checks.
- Code backend nằm trong `backend/`; schema, migration và seed ở `backend/prisma/`. Không cần folder `Database/` riêng. Git chứa cấu trúc/seed, không chứa live data, PGDATA, Docker volume, secrets hoặc dump thật.
- Source local mặc định `HOST=127.0.0.1`, `PORT=5000`. VPS truyền `HOST=0.0.0.0`, `PORT=5000` qua môi trường; không hardcode IP VPS trong source. Docker API listen `0.0.0.0:5000`, chỉ trong mạng container.
- Nginx public HTTPS **443** chuyển tiếp đến `api:5000`; địa chỉ API `https://18.143.100.43/api/v1`. Port 80 phục vụ ACME/redirect. Không public port 5000 hoặc 5432. PostgreSQL chỉ publish `127.0.0.1:5432` cho SSH tunnel; local tunnel `127.0.0.1:55432`.
- Docker Compose chạy `api`, `postgres`, `reverse-proxy`; chỉ một NestJS instance. Dockerfile/Compose/Nginx/systemd/secrets/script vận hành đặt ngoài repo trên VPS (`/etc/n8-cineplex`); source checkout đúng SHA ở `/opt/n8-cineplex`.
- HTTPS chứng chỉ IP thật qua Certbot webroot; tự gia hạn và reload Nginx. Không bỏ kiểm tra TLS. IP đổi thì cập nhật endpoint và chứng chỉ.
- Backup tự động **mỗi 24 giờ**, giữ **72 giờ (3 ngày)**, chỉ lưu tại `/var/backups/n8-cineplex` trên VPS. Lịch 02:00 Việt Nam = 19:00 UTC; systemd timer có chạy bù. `pg_dump -Fc`, chống chạy trùng, file tạm rồi đổi tên, kiểm tra danh mục và SHA-256. Chỉ dọn bản hết hạn sau khi backup mới thành công; backup lỗi giữ bản tốt và log lỗi.
- Backup thủ công tùy chọn. **Chỉ restore khi người dùng yêu cầu**; dừng API/job/phiên ghi trước khi thay dữ liệu của chính DB chung. Không tự restore khi test lỗi hoặc chạy restore trong setup chỉ để test. Backup cùng VPS không bảo vệ khi mất VPS/đĩa; có thể mất dữ liệu phát sinh sau snapshot.
- Tuần 4 chỉ bootstrap NestJS + Prisma + PostgreSQL + `@nestjs/schedule`, 10 bảng nghiệp vụ đúng canonical fields, seed 7 phòng, health query DB thật và Swagger. **Không Redis/BullMQ**, không triển khai Auth/crawler/payment/booking, không sửa Android. Socket.IO vẫn thuộc kiến trúc tương lai.
- `_prisma_migrations` là metadata kỹ thuật, không tính vào 10 bảng nghiệp vụ. UUID PK/FK, tiền VND Decimal(14,0), thời gian timestamptz UTC; enum/nullable/FK/UNIQUE theo spec. Không UNIQUE toàn cục `order_seats.showtime_seat_id`.
- Migration được review rồi `migrate deploy`; không `migrate dev`, reset, TRUNCATE, DROP schema hoặc `db push --force-reset` trên DB chung. Fixture dùng UUID riêng trong transaction/savepoint, luôn rollback; không sửa/xóa data có sẵn. Role API DML, role migration riêng.
- Seed idempotent, không ghi đè phòng đã được sửa: Cinema 1 IMAX 496; Cinema 2 SCREENX 180; Cinema 3 STANDARD 160; Cinema 4 STANDARD 145; Cinema 5 STANDARD 130; Cinema 6 STANDARD 112; Cinema 7 GOLD 32. UUID cố định, is_extra=false, ACTIVE. Chưa seed account/password/payment/showtime seats.
- API tuần 4: `GET /api/v1/health` query PostgreSQL, 200 `{"status":"ok","database":"up"}`, lỗi DB sau startup trả 503 `{"status":"error","database":"down"}`. Config/kết nối sai lúc startup phải thất bại. Swagger `/api/docs`, OpenAPI `/api/docs-json`.
- Repo chỉ source và cấu hình build cần thiết, không thêm docs/Infrastructure/README mới, script vận hành hoặc `.env.example`. Selftest tạm phải xóa sau khi test xong, kể cả khỏi commit; ghi kết quả ngoài repo. Không xóa test sẵn có của Android.
- Branch `feat/nguyen-week4-backend-bootstrap`; commit Author/Committer duy nhất Trần Thanh Nguyên `<thnguyen290106@gmail.com>`, không co-author/bot attribution, không commit/push main. Nhóm trưởng review/merge PR; không rewrite lịch sử/contributor đã có.
- Không dùng `docker compose down -v`. Rollback code không tự restore DB.


### Kết quả triển khai tuần 4 đã kiểm tra (2026-10-04T19:41:51+00:00)

- Source đã push branch `feat/nguyen-week4-backend-bootstrap`, SHA `aac59ef972be1a3021b5ac7c6ca8b1184ad0a239`; PR https://github.com/Tonyisded/N8_Cineplex-Project/pull/2. Author/Committer các commit mới đều Trần Thanh Nguyên `<thnguyen290106@gmail.com>`; main và Android không thay đổi.
- VPS chạy PostgreSQL 17, một API NestJS nội bộ port 5000 và Nginx HTTPS 443. Health `https://18.143.100.43/api/v1/health` trả 200 với database up; Swagger `/api/docs` trả 200, OpenAPI hợp lệ. Local mặc định 127.0.0.1:5000 đã kiểm tra qua SSH tunnel DB chung.
- Đúng 10 bảng nghiệp vụ/103 cột canonical và 7 phòng seed; seed lần hai INSERT 0 0. Schema diff đọc-only không có khác biệt. Lỗi migration ban đầu đã được đánh dấu rolled-back bằng Prisma rồi áp dụng migration đầy đủ, không reset database; không còn migration lỗi đang chờ xử lý.
- Backup service đã chạy thành công, hash/danh mục hợp lệ; timer mỗi 24 giờ lúc 19:00 UTC, giữ 72 giờ. Selftest xác nhận backup thất bại không dọn bản tốt, bản 73 giờ được dọn và bản 48 giờ được giữ. Chưa chạy restore thật và không tự restore.
- Certbot production IP certificate đã cấp; renewal dry-run + reload Nginx thành công. Reboot kiểm tra tự khởi động và bảo toàn database/seed đã đạt.
- 11 selftest core và kiểm tra constraint/history/role trên DB chung đạt; fixture SQL đã rollback. File selftest tạm đã xóa, không đưa test/docs/script vận hành/secrets vào commit. Không đồng nghĩa các tính năng Auth/crawler/booking/payment của các tuần sau đã hoàn thành.

### Quy tắc bắt buộc khi bổ sung hoặc sửa backend: local và VPS

Áp dụng cho mọi lần bổ sung, sửa hoặc cập nhật backend về sau, không chỉ nhiệm vụ khởi tạo tuần 4.

1. **Code local và code commit lên Git luôn giữ cấu hình localhost:** backend trong `D:\NT118\N8_Cineplex-Project\backend` mặc định `HOST=127.0.0.1` (localhost), `PORT=5000`. Không hardcode IP VPS, mật khẩu, private key hoặc giá trị môi trường production vào source; cấu hình riêng đặt ngoài Git.
2. **Luôn có bước cập nhật/triển khai lên VPS:** sau khi sửa và kiểm tra local, commit/push trên branch riêng theo quy định, rồi triển khai đúng commit SHA đã chọn lên `ubuntu@18.143.100.43`. Không xem nhiệm vụ backend là hoàn thành chỉ vì local chạy được hoặc đã push Git. Nếu chưa triển khai/kiểm tra được VPS, phải ghi rõ bước chưa hoàn thành và nguyên nhân, không báo đã deploy.
3. **Cùng source, cấu hình theo môi trường:** trên VPS truyền `HOST=0.0.0.0`, `PORT=5000` cho API trong Docker; endpoint truy cập theo IP VPS là `https://18.143.100.43/api/v1`, Swagger `https://18.143.100.43/api/docs`. Không đổi source mặc định localhost thành IP VPS; IP VPS là địa chỉ truy cập qua Nginx HTTPS 443, không phải địa chỉ bind trực tiếp của container. Không public port 5000/5432.
4. **Bước VPS bắt buộc trong kế hoạch thực hiện:** SSH bằng user `ubuntu` và key `.ppk`; cập nhật source đúng SHA ở `/opt/n8-cineplex`, build lại image/backend và recreate/restart service API bằng cấu hình vận hành ngoài repo ở `/etc/n8-cineplex`. Giữ secrets phía VPS, PostgreSQL volume/dữ liệu và cấu hình tự khởi động Docker + `restart: unless-stopped`; không chạy thêm một `npm start` song song với API Docker.
5. **Nếu thay đổi schema:** review migration và tạo backup trước khi áp dụng migration bằng role migration trên DB chung; không reset/drop database, không dùng `docker compose down -v`, không tự restore. Không chạy migration nếu thay đổi không cần migration.
6. **Xác nhận sau deploy:** kiểm tra SHA trên VPS khớp phiên bản cần triển khai, container API chạy/healthy, log không có lỗi startup, health qua IP VPS trả HTTP 200 với database up và Swagger mở được; kiểm tra thêm hành vi/API vừa sửa. Ghi riêng kết quả local và VPS; chỉ báo hoàn thành khi bước VPS tương ứng đã đạt. Selftest tạm phải xóa sau khi kiểm tra; kết quả kiểm chứng lưu ngoài repo theo quy định hiện có.

### Hướng dẫn end-to-end chạy thử backend: Windows local và VPS Ubuntu

#### 0. Chọn cách chạy

- Chỉ muốn thử API/Swagger hoặc xem console backend đang chạy trên VPS: làm phần A, không cần Node.js hay database/tunnel local.
- Muốn chạy source trong folder `backend` trên Windows: làm phần B. Backend chạy local; PostgreSQL vẫn là DB chung trên VPS, không phải DB local.
- Muốn xem console local mà không kết nối DB: xem B.6. Backend hiện tại bắt buộc kết nối DB lúc startup.
- Đường dẫn `/` chưa có trang web, HTTP 404 ở đó là bình thường. Dùng `/api/v1/health` và `/api/docs`. Chạy thử bootstrap không đồng nghĩa toàn bộ nghiệp vụ đặt vé đã hoàn thành.

#### A. Thử API và xem console trên VPS

1. Mở trình duyệt Windows: health `https://18.143.100.43/api/v1/health`, Swagger `https://18.143.100.43/api/docs`. Không cần SSH nếu chỉ thử API. Trong Swagger chọn `GET /api/v1/health` → `Try it out` → `Execute`; phải trả HTTP 200 và `{"status":"ok","database":"up"}`.
2. Muốn xem console: cài PuTTY/Plink nếu chưa có, nhận key qua kênh riêng và bảo đảm mạng truy cập SSH port 22. Mở PowerShell Windows:

```powershell
& "C:\Program Files\PuTTY\plink.exe" -t -noagent -i "D:\ssh\shibakey.ppk" -hostkey "SHA256:M2YHlZKG+3dwdNxEuy4S7LrTrE7fnANve6v1T8vlNvQ" ubuntu@18.143.100.43
```

Nếu lưu key ở nơi khác, thay đường dẫn `-i`. Đây là shell Ubuntu, không phải tunnel `-N`. Fingerprint là host key đã xác minh của VPS hiện tại; nếu bị báo khác, dừng và xác minh với quản trị, không bỏ kiểm tra host key. Không đưa `.ppk` lên Git.

3. Trong cửa sổ Ubuntu vừa mở, chạy:

```bash
sudo docker compose --env-file /etc/n8-cineplex/vps.env -f /etc/n8-cineplex/compose.yaml ps
sudo docker logs --tail 100 -f n8-cineplex-api-1
```

API/PostgreSQL phải running/healthy, reverse-proxy running. Console startup thành công có `Nest application successfully started`. `-f` hiện log mới liên tục; đứng yên khi không có log mới là bình thường. Nest chưa tự ghi access log cho mọi HTTP request. `Ctrl+C` chỉ thoát xem log, không dừng backend. Kiểm tra tiếp trong Ubuntu:

```bash
curl --fail --silent --show-error https://18.143.100.43/api/v1/health
curl --fail --silent --show-error -o /dev/null -w '%{http_code}\n' https://18.143.100.43/api/docs
```

Phải nhận JSON database up và mã `200`. Gõ `exit` để thoát SSH; backend vẫn chạy.

4. Nếu service đã bị dừng chủ động: kiểm tra nguyên nhân/log trước. Bật lại stack hiện có, không thay phiên bản source/image và không xóa data:

```bash
sudo docker compose --env-file /etc/n8-cineplex/vps.env -f /etc/n8-cineplex/compose.yaml up -d postgres api reverse-proxy
```

Kiểm tra lại A.3. Docker được enable khi boot, ba service có `restart: unless-stopped`; reboot thực tế đã được kiểm tra. Service bị `docker stop`/`compose down` phải bật lại chủ động. Đóng SSH không tắt backend. Không chạy `npm start` song song trên VPS, không dùng `docker compose down -v` để thử chạy.

#### B. Chạy backend local trên Windows

##### B.1. Điều kiện

- Source ở `D:\NT118\N8_Cineplex-Project\backend`, dùng branch/version được nhóm xác nhận; không tự ghi đè thay đổi local/main.
- Node.js 24.x, version `>=24.15.0` và `<25`, cùng npm; PuTTY/Plink và key `.ppk` hợp lệ. Chạy trong PowerShell/Windows Terminal (hoặc terminal Android Studio), không phải shell Ubuntu:

```powershell
node --version
npm.cmd --version
Test-Path "C:\Program Files\PuTTY\plink.exe"
Test-Path "D:\ssh\shibakey.ppk"
```

Hai `Test-Path` phải trả `True`. Không cần PostgreSQL/Docker local hoặc Redis/BullMQ.

##### B.2. Chuẩn bị `.env` riêng

Máy Trần Thanh Nguyên đã có `backend/.env`. Máy mới clone Git không có file này: nhận cấu hình DB của role `n8_app` từ quản trị qua kênh riêng, rồi tạo đúng file `backend/.env` (không phải `.env.txt`). Mẫu định dạng, không chứa mật khẩu thật:

```dotenv
HOST=127.0.0.1
PORT=5000
DATABASE_URL="postgresql://n8_app:<DB_PASSWORD>@127.0.0.1:55432/n8_cineplex"
```

Thay `<DB_PASSWORD>` bằng password thật, URL-encode nếu có ký tự đặc biệt. Không dùng placeholder để chạy. Không in/chụp/chia sẻ `.env` công khai, không copy `vps.env` hoặc dùng role migration/superuser cho API. `.env` đã được Git ignore. Nếu phiên PowerShell đã đặt `DATABASE_URL`, xóa riêng biến của phiên đó bằng `Remove-Item Env:DATABASE_URL -ErrorAction SilentlyContinue` để dùng giá trị trong `.env`. Source/Git luôn giữ localhost, không thay thành IP VPS.

##### B.3. PowerShell số 1: mở tunnel database

```powershell
& "C:\Program Files\PuTTY\plink.exe" -batch -noagent -i "D:\ssh\shibakey.ppk" -hostkey "SHA256:M2YHlZKG+3dwdNxEuy4S7LrTrE7fnANve6v1T8vlNvQ" -L "127.0.0.1:55432:127.0.0.1:5432" -N ubuntu@18.143.100.43
```

Giữ cửa sổ mở. `-N` không mở shell Ubuntu/chạy backend; chỉ chuyển kết nối `127.0.0.1:55432` trên Windows tới PostgreSQL `127.0.0.1:5432` trên VPS. Không có lỗi và đứng yên là bình thường. Nếu key có passphrase, bỏ `-batch` để nhập khi Plink hỏi, không ghi passphrase trong lệnh/file. Mỗi máy thành viên mở tunnel riêng.

##### B.4. PowerShell số 2: cài dependencies, build, chạy

```powershell
cd D:\NT118\N8_Cineplex-Project\backend
npm.cmd ci
npm.cmd run build
$env:HOST = "127.0.0.1"
$env:PORT = "5000"
npm.cmd start
```

Chỉ tiếp tục nếu bước trước thành công. `npm.cmd ci` cần khi clone mới/lần đầu hoặc package-lock đổi; máy đã có đúng dependencies có thể bỏ qua khi chạy lại. Dùng `npm.cmd` tránh PowerShell chặn `npm.ps1`. Build tự generate Prisma Client. Giữ cửa sổ này mở, đây là console local; chờ `Nest application successfully started`.

Không chạy migration/seed/reset chỉ để thử server: DB chung đã được khởi tạo. Migration thật tuân theo quy tắc deploy/migration, role `n8_app` không có quyền DDL.

##### B.5. Kiểm tra local

Mở health `http://127.0.0.1:5000/api/v1/health`, Swagger `http://127.0.0.1:5000/api/docs`, OpenAPI `http://127.0.0.1:5000/api/docs-json`. Dùng `Try it out` cho health trong Swagger. Có thể dùng `localhost`; nếu máy resolve IPv6 gây lỗi, dùng `127.0.0.1`. Hoặc mở PowerShell số 3:

```powershell
curl.exe --fail --silent --show-error http://127.0.0.1:5000/api/v1/health
curl.exe --fail --silent --show-error -o NUL -w "%{http_code}\n" http://127.0.0.1:5000/api/docs
```

Health phải HTTP 200/`{"status":"ok","database":"up"}`, Swagger phải `200`; OpenAPI có health route. Không dùng `-k` bỏ TLS khi thử VPS. API local vẫn dùng DB chung: không ghi/xóa data của nhóm tùy tiện; fixture cần transaction/savepoint rollback.

##### B.6. Sửa code, dừng và chỉ xem console

- Sửa code: `Ctrl+C` backend → `npm.cmd run build` → `npm.cmd start`, giữ tunnel. Tùy chọn watch: sau build đầu, chạy `npm.cmd run build:watch` và `npm.cmd run start:watch` ở hai cửa sổ riêng, vẫn giữ tunnel.
- Dừng local: `Ctrl+C` backend trước, rồi `Ctrl+C` tunnel; không dừng dịch vụ VPS. Lần sau mở tunnel rồi build/start, không cần `npm ci` nếu dependencies không đổi.
- Chỉ xem console local không mở tunnel: build/start vẫn in log nhưng sẽ báo `PostgreSQL connection failed during startup` và tự thoát, chưa phục vụ localhost. Không bỏ kiểm tra DB/giả lập health để che lỗi. Muốn xem console đang hoạt động mà không dùng DB/tunnel local thì làm phần A.
- Mọi thay đổi backend thật tiếp tục bước cập nhật VPS và kiểm tra sau deploy theo mục **Quy tắc bắt buộc khi bổ sung hoặc sửa backend: local và VPS**; local không thay thế VPS. Chỉ sửa hướng dẫn spec không cần rebuild API khi source không đổi.

#### C. Lỗi thường gặp và tiêu chí đạt

- Không nhận `node`/`npm.cmd`: cài đúng Node.js 24.x, mở lại PowerShell, kiểm tra PATH/version.
- `DATABASE_URL is required`: thiếu `.env`, sai tên `.env.txt` hoặc chạy ngoài `backend`; sửa cấu hình riêng, không đưa secrets vào source.
- Lỗi PostgreSQL startup: kiểm tra tunnel, `.env` trỏ `127.0.0.1:55432`, tài khoản/password và PostgreSQL healthy; không reset DB để chữa kết nối.
- SSH timeout/access denied: kiểm tra mạng/22, user `ubuntu`, key/passphrase; host key khác thì xác minh với quản trị, không bỏ xác thực.
- Port 5000/55432 bị chiếm: kiểm tra `Get-NetTCPConnection -State Listen -LocalPort 5000,55432`; chỉ dừng tiến trình/tunnel cũ của chính mình, không kill dịch vụ không rõ. Backend vẫn dùng port 5000.
- VPS API lỗi: kiểm tra `compose ps`, log API/proxy, Nginx/TLS và port 443. Xem proxy log: `sudo docker logs --tail 100 n8-cineplex-reverse-proxy-1`. Không public PostgreSQL/5000 hoặc bỏ TLS để che lỗi.
- Đạt khi startup thành công, health HTTP 200/database up, Swagger HTTP 200 và OpenAPI hợp lệ; ghi rõ kết quả local/VPS. Log startup cũ không chứng minh instance hiện tại hoạt động: phải kiểm tra HTTP và trạng thái service. Không coi startup fail hay 404 tại `/` là đã chạy thành công; không yêu cầu mọi request phải xuất hiện trong log.

