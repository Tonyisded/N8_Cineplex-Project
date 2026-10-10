# N8 Cineplex — Master Plan

> **Mục tiêu:** đồ án môn Phát triển ứng dụng trên thiết bị di động, ưu tiên kiến trúc rõ ràng, dễ code, dễ demo, nhưng vẫn đủ chiều sâu kỹ thuật cho sinh viên CNTT.
>
> **Business timezone:** `Asia/Ho_Chi_Minh` (UTC+7). Database lưu thời gian theo UTC; backend quy đổi khi hiển thị và kiểm tra nghiệp vụ.
>
> **Nguyên tắc database:** đúng **10 bảng nghiệp vụ**, database quan hệ cơ bản, **không JSONB**, mỗi bảng khoảng **7–12 trường**.
>
> **Nguyên tắc kiến trúc:** một NestJS backend theo modular monolith; PostgreSQL là source of truth; REST dùng cho command/query, WebSocket/Socket.IO dùng để báo realtime sau khi transaction đã commit.

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

# 1. Tổng quan

N8 Cineplex là ứng dụng Android đặt vé xem phim theo mô hình client-server.

Các chức năng chính:

- đăng ký / đăng nhập bằng email và mật khẩu;
- xác minh email;
- quên / đổi mật khẩu;
- đăng nhập bằng Google;
- quản lý hồ sơ và avatar;
- xem phim và lịch chiếu;
- crawler tự lấy phim/lịch chiếu công khai từ CGV Vincom Center Landmark 81;
- N8 tự quản lý phòng và ghế, độc lập với inventory CGV;
- chọn ghế và giữ ghế 10 phút;
- chống double booking;
- cập nhật seat map realtime bằng WebSocket;
- chọn combo;
- áp dụng promotion đơn giản;
- dùng N8 Refund Credit;
- thanh toán thật bằng SePay + VietQR;
- cập nhật trạng thái thanh toán realtime;
- nhận vé QR;
- gửi FCM khi đặt vé thành công;
- gửi email/Gmail xác nhận đặt vé thành công kèm QR vé;
- Staff scan QR để check-in;
- Staff xác nhận giao combo;
- self-refund thành N8 Refund Credit;
- Admin quản lý Staff/combo và có thể hủy suất;
- Admin xử lý các payment issue hiếm gặp ở mức tối thiểu;
- bắt buộc cập nhật APK khi phiên bản quá cũ.

N8 Cineplex chỉ lấy dữ liệu phim/lịch chiếu công khai từ CGV. Vé, ghế, thanh toán, QR và Refund Credit đều do N8 quản lý độc lập.

---

# 2. Vai trò

```text
CUSTOMER
STAFF
ADMIN
```

## 2.1. CUSTOMER

- đăng ký / đăng nhập;
- xem phim và lịch chiếu;
- chọn ghế;
- chọn combo;
- thanh toán;
- xem vé;
- self-refund nếu đủ điều kiện;
- quản lý profile.

## 2.2. STAFF

Staff chỉ làm nghiệp vụ tại rạp:

- đăng nhập;
- scan QR và check-in vé;
- xác nhận giao combo.

Staff **không**:

- sửa crawler;
- review movie/showtime;
- xử lý payment sai;
- reconciliation;
- manual finalize payment;
- xử lý lỗi hệ thống.

## 2.3. ADMIN

Admin có thể:

- tạo / khóa / mở khóa Staff;
- CRUD combo;
- xem phim / suất chiếu / order;
- bật/tắt booking bằng config;
- hủy suất chiếu trước giờ chiếu;
- xem payment issue;
- đánh dấu payment issue đã được hoàn tiền ngân hàng hoặc đã xử lý.

Admin không cần thao tác thường xuyên vào các luồng bình thường.

---

# 3. Công nghệ

## 3.1. Android

- Kotlin;
- Jetpack Compose;
- Material 3;
- ViewModel + StateFlow;
- Coroutines;
- Navigation Compose;
- Retrofit + OkHttp;
- Kotlin Serialization;
- DataStore;
- Android Keystore;
- Coil;
- CameraX;
- ML Kit Barcode Scanning;
- Firebase Cloud Messaging;
- Socket.IO client;
- Gradle Kotlin DSL.

## 3.2. Backend

- Node.js;
- TypeScript;
- NestJS;
- Prisma;
- PostgreSQL;
- REST API;
- Socket.IO / WebSocket;
- Swagger/OpenAPI;
- `@nestjs/schedule` cho cron/background cleanup;
- Nodemailer cho email.

## 3.3. Crawler

- Playwright;
- chạy trong NestJS scheduled service;
- không BullMQ;
- không worker queue riêng.

## 3.4. Thanh toán

- SePay;
- VietQR;
- webhook backend xác nhận giao dịch thật.

## 3.5. Email

- Nodemailer;
- Gmail SMTP;
- Gmail App Password đặt trong environment;
- không lưu mật khẩu Gmail trong database hoặc source code.

## 3.6. Deployment

- Ubuntu/Linux;
- Docker Compose;
- Nginx hoặc Caddy;
- HTTPS.

---

# 4. Kiến trúc

```text
Android App
    |
    | HTTPS REST + Socket.IO
    v
NestJS API
    |
    +-- PostgreSQL
    +-- Playwright crawler
    +-- SePay/VietQR
    +-- Firebase FCM
    +-- Gmail SMTP / Nodemailer
    +-- APK hosting
```

Backend dùng **modular monolith**.

Module chính:

```text
auth/
users/
movies/
showtimes/
seats/
booking/
payments/
tickets/
notifications/
crawler/
staff/
admin/
app-config/
```

Không dùng:

- microservices;
- Kafka;
- Kubernetes;
- CQRS framework;
- event sourcing;
- Elasticsearch;
- Redis;
- BullMQ;
- outbox pattern;
- durable idempotency table.

## 4.1. REST và WebSocket

REST là đường chính để tạo/sửa dữ liệu.

WebSocket chỉ dùng để báo rằng trạng thái vừa thay đổi.

Ví dụ:

```text
Customer A hold A5
→ backend transaction update PostgreSQL
→ transaction commit
→ Socket.IO emit SEAT_HELD
→ các client đang xem showtime đó cập nhật A5
```

WebSocket **không** là source of truth.

Khi socket reconnect:

```text
reconnect
→ GET lại dữ liệu từ REST
→ dùng trạng thái mới nhất từ PostgreSQL
→ tiếp tục nghe WebSocket
```

Fallback nếu socket lỗi:

- seat map polling khoảng 10 giây;
- payment polling khoảng 5 giây.

---

# 5. Tài khoản và xác thực

## 5.1. Email/password

Hỗ trợ:

- đăng ký;
- xác minh email;
- đăng nhập;
- đăng xuất;
- quên mật khẩu;
- reset mật khẩu;
- đổi mật khẩu.

Password hash bằng Argon2id hoặc bcrypt.

Email luôn:

```text
trim
→ lowercase
```

và unique trong database.

## 5.2. Xác minh email

Không tạo bảng OTP/challenge riêng.

Dùng signed token có thời hạn ngắn:

```text
register
→ tạo user
→ email_verified_at = NULL
→ backend tạo verification token hết hạn
→ gửi link/token qua email
→ user verify
→ email_verified_at = now
```

Customer chưa verify vẫn được xem phim/profile nhưng không được tạo booking/payment.

## 5.3. Google Login

Android dùng Sign in with Google / Credential Manager.

Backend verify Google credential rồi dùng:

```text
google_id = Google sub
```

`google_id` unique khi không null.

Google user mới:

- tạo `users`;
- set email từ verified Google email;
- `email_verified_at = now`;
- `password_hash = NULL`.

Không tạo `user_identities` table.

## 5.4. Access token + refresh token

Dùng:

- access token ngắn hạn;
- refresh token dài hạn hơn.

Đơn giản hóa bằng cách lưu **một refresh token hash hiện tại** trong `users.refresh_token_hash`.

Flow:

```text
login
→ tạo access token + refresh token
→ hash refresh token
→ users.refresh_token_hash = hash
```

Refresh:

```text
client gửi refresh token
→ backend compare với hash
→ hợp lệ thì cấp access token mới
```

Logout:

```text
users.refresh_token_hash = NULL
```

Đây là mô hình một refresh session chính cho mỗi account, đủ cho đồ án.

Không làm token family/reuse detection phức tạp.

## 5.5. Profile

Profile hỗ trợ:

- full name;
- avatar;
- đổi email;
- đổi password.

Avatar:

- upload binary;
- validate MIME/type/size;
- đề xuất tối đa 5 MB;
- database chỉ lưu `avatar_url`.

---

# 6. Crawler phim và lịch chiếu

Playwright crawl dữ liệu công khai từ CGV Landmark 81.

## 6.1. Dữ liệu lấy về

Movie:

```text
title
poster_url
duration
synopsis
age_rating
genre
language
source_url
```

Showtime:

```text
movie
start_at
format
source_key
```

Format hỗ trợ:

```text
STANDARD_2D
STANDARD_3D
IMAX_2D
IMAX_3D
SCREENX
GOLD_CLASS
```

## 6.2. Flow

```text
NestJS Cron
→ Playwright mở trang CGV
→ extract movie/showtime
→ normalize
→ upsert movies
→ upsert showtimes
→ auto room assignment
→ materialize showtime_seats
```

Nếu một showtime thiếu duration hoặc format không nhận diện được:

```text
skip showtime đó
→ log lỗi
```

Không tạo `needs_review` và không bắt Staff xử lý.

Crawler lỗi:

```text
log lỗi
→ cron lần sau chạy lại
```

Không cần `crawler_runs` table.

---

# 7. Phòng chiếu

Khởi tạo 7 phòng logic cơ sở:

```text
Cinema 1 — IMAX
Cinema 2 — ScreenX
Cinema 3 — Standard Large
Cinema 4 — Standard Medium A
Cinema 5 — Standard Medium B
Cinema 6 — Standard Small
Cinema 7 — Gold Class
```

Capacity mô phỏng:

```text
Cinema 1: 496
Cinema 2: 180
Cinema 3: 160
Cinema 4: 145
Cinema 5: 130
Cinema 6: 112
Cinema 7: 32
```

Đây là mô hình N8, không phải sơ đồ ghế chính thức của CGV.

## 7.1. Auto room assignment

Showtime được xếp vào phòng đúng capability đang rảnh.

Quy tắc:

```text
STANDARD → Standard room
IMAX → IMAX room
SCREENX → ScreenX room
GOLD → Gold room
```

Phòng rảnh khi:

```text
next.start_at >= previous.end_at
```

Nếu tất cả phòng đúng loại đều bận:

```text
auto-create extra logical room cùng loại
```

Ví dụ:

```text
STANDARD_EXTRA_01
IMAX_EXTRA_01
```

Không yêu cầu Staff xử lý.

---

# 8. Seat template và seat inventory

Không có bảng `seats` riêng.

Seat layout được định nghĩa bằng template trong code theo loại/capacity auditorium.

Ví dụ:

```text
Standard template
IMAX template
ScreenX template
Gold template
```

Khi showtime được tạo và đã có auditorium:

```text
load seat template theo auditorium
→ tạo showtime_seats
```

Mỗi `showtime_seats` là một ghế vật lý của một showtime cụ thể.

Seat type:

```text
NORMAL
VIP
COUPLE
GOLD
```

Seat status:

```text
AVAILABLE
HELD
SOLD
BLOCKED
```

Couple seat dùng quy ước seat code/group ở service layer; backend chỉ cho chọn đủ cặp.

---

# 9. Giữ ghế 10 phút

Thời gian giữ ghế:

```text
600 seconds
```

Flow:

```text
user chọn ghế
→ backend transaction
→ lock các showtime_seats cần chọn
→ kiểm tra AVAILABLE
→ tạo order PENDING_PAYMENT
→ tạo order_seats HELD
→ showtime_seats = HELD
→ held_by_user_id = user
→ hold_expires_at = now + 10m
→ commit
→ emit SEAT_HELD
```

Nếu hai user giữ cùng ghế đồng thời, chỉ transaction đầu tiên được thành công.

PostgreSQL là source of truth.

## 9.1. Expire hold

NestJS cron chạy định kỳ:

```text
showtime_seats HELD
AND hold_expires_at <= now
```

thì:

```text
order PENDING_PAYMENT → EXPIRED
order_seats HELD → CANCELED
showtime_seats HELD → AVAILABLE
held_by_user_id = NULL
hold_expires_at = NULL
restore credit_used nếu đã trừ
```

Sau commit:

```text
emit SEAT_RELEASED
emit ORDER_EXPIRED tới user
```

Không reset 10 phút khi đổi màn hình.

---

# 10. Double booking và concurrency

Đây là phần bắt buộc giữ dù database đã đơn giản hóa.

## 10.1. Constraint

`showtime_seats`:

```text
UNIQUE(showtime_id, seat_code)
```

## 10.2. Transaction

Hold/payment/refund phải dùng transaction.

Hold ghế chỉ thành công khi trạng thái hiện tại là `AVAILABLE`.

Payment chỉ bán ghế thuộc đúng order đang giữ.

Refund chỉ release ghế thuộc đúng order/ticket đang refund.

Không để client tự gửi trạng thái cuối cùng đáng tin cậy.

---

# 11. Combo

Một combo là một sản phẩm hoàn chỉnh.

Ví dụ:

```text
Combo 1 — 1 Popcorn + 2 Pepsi
Combo 2 — 1 Popcorn Large + 2 Pepsi Large
```

Không tách:

- concession_products;
- combo_items;
- combo_options.

Một order chọn tối đa:

```text
1 loại combo
+ quantity
```

Nếu không chọn combo:

```text
combo_id = NULL
combo_quantity = 0
```

---

# 12. Pricing

Pricing tính ở backend.

Client không được gửi trusted final amount.

Công thức:

```text
subtotal
= tổng giá ghế
+ combo_price * combo_quantity
```

Sau đó:

```text
final_total = subtotal - discount
credit_used = MIN(user.credit_balance, final_total)
bank_amount = final_total - credit_used
```

`orders.total_amount` lưu `final_total`.

## 12.1. Promotion

Promotion viết trong backend code/config, không có bảng riêng.

Ví dụ:

```text
WELCOME_OFFER
HAPPY_MONDAY
COMBO_SAVER
```

Chỉ áp dụng một discount tốt nhất.

Lưu kết quả cuối vào:

```text
orders.discount
```

Không cần lưu campaign history phức tạp.

---

# 13. N8 Refund Credit

Refund Credit lưu trực tiếp trong:

```text
users.credit_balance
```

Không có wallet/ledger table.

Credit:

- thuộc riêng user;
- không chuyển user khác;
- không rút tiền mặt;
- không hết hạn.

## 13.1. Dùng credit khi checkout

Khi tạo order:

```text
credit_used = MIN(credit_balance, total_amount)
```

Backend transaction:

```text
lock user
→ trừ credit_used khỏi users.credit_balance
→ lưu orders.credit_used
```

Nếu order expire/cancel trước khi PAID:

```text
restore đúng orders.credit_used
```

State guard đảm bảo chỉ restore một lần.

## 13.2. Order 0đ ngân hàng

Nếu:

```text
bank_amount = 0
```

thì:

```text
không tạo payments row
→ order PAID
→ order_seats ACTIVE
→ showtime_seats SOLD
→ issue ticket
→ emit PAYMENT_PAID
→ FCM
→ email ticket
```

---

# 14. SePay + VietQR

Đây là **thanh toán tiền thật** của N8.

Flow:

```text
order PENDING_PAYMENT
→ backend tạo payment_code unique
→ tạo VietQR với bank_amount + payment_code
→ user chuyển khoản
→ SePay webhook
→ backend verify
→ finalize payment
```

## 14.1. Payment code

Ví dụ:

```text
N8A1B2C3D4
```

Phải unique toàn hệ thống.

## 14.2. Webhook

Endpoint:

```text
POST /api/v1/webhooks/sepay
```

Backend phải verify webhook theo cơ chế SePay hỗ trợ tại thời điểm triển khai và không tin dữ liệu do Android tự khai.

Kiểm tra tối thiểu:

```text
transaction ID
receiving account
incoming transfer
payment code
amount
order/payment còn hợp lệ
transaction chưa xử lý
```

`payments.provider_transaction_id` unique khi không null.

## 14.3. Payment hợp lệ

Nếu:

```text
payment_code đúng
received_amount = expected_amount
order còn PENDING_PAYMENT
hold chưa hết hạn
```

thì trong một transaction:

```text
payments → PAID
orders → PAID
order_seats HELD → ACTIVE
showtime_seats HELD → SOLD
issue ticket VALID
```

Sau commit:

```text
emit PAYMENT_PAID tới user
emit SEAT_SOLD tới showtime
send FCM
send ticket email
```

## 14.4. Payment issue

Không bắt Staff xử lý.

Các trường hợp như:

```text
WRONG_AMOUNT
WRONG_REFERENCE
LATE_PAYMENT
```

thì:

```text
payments.status = NEEDS_ADMIN
payments.issue_reason = ...
```

Order không được tự chuyển PAID.

Nếu hold hết hạn:

```text
order EXPIRED
seats RELEASED
credit RESTORED
```

Admin có màn hình đơn giản để xem payment issue.

Admin chỉ cần một trong các action:

```text
MARK_BANK_REFUNDED
MARK_RESOLVED
```

Không revive order cũ, không lấy lại ghế đã release.

Đây là đường xử lý tiền thật bất thường tối thiểu, không phải reconciliation module phức tạp.

## 14.5. Idempotency không cần bảng riêng

Không có `idempotency_records`.

Vẫn phải chống xử lý trùng bằng:

- unique `payment_code`;
- unique `provider_transaction_id`;
- transaction;
- kiểm tra current state trước update;
- duplicate webhook trả thành công nhưng không issue ticket lần hai.

---

# 15. Order

Order status:

```text
PENDING_PAYMENT
PAID
EXPIRED
CANCELED
REFUNDED
```

Flow chính:

```text
create order
→ PENDING_PAYMENT
→ PAID hoặc EXPIRED
```

Sau PAID:

```text
self-refund hợp lệ → REFUNDED
showtime bị Admin hủy → CANCELED
```

Một order thuộc một user và một showtime.

Một order có thể chọn một combo type + quantity.

---

# 16. Order Seat

`order_seats` lưu lịch sử ghế theo order.

Đây là bảng cần thiết để không mất lịch sử khi một ghế được refund rồi bán lại.

Ví dụ:

```text
Order 100 → A1 → REFUNDED
Order 200 → A1 → ACTIVE
```

Nếu chỉ lưu `order_id` trực tiếp trên `showtime_seats`, lịch sử Order 100 sẽ bị ghi đè khi A1 được bán lại.

Status:

```text
HELD
ACTIVE
CANCELED
REFUNDED
```

---

# 17. Ticket

MVP:

```text
1 order = 1 ticket
1 ticket = 1 QR
```

Ticket status:

```text
VALID
USED
REFUNDED
CANCELLED
EXPIRED
```

Ticket detail lấy từ join:

```text
ticket
→ order
→ order_seats
→ showtime
→ movie
→ auditorium
```

QR chứa opaque token/booking code, không chứa dữ liệu nhạy cảm.

---

# 18. Check-in

Staff scan QR bằng CameraX + ML Kit.

Check-in window:

```text
T-90 phút đến T+30 phút
```

Điều kiện:

```text
ticket.status = VALID
now nằm trong check-in window
```

Thành công:

```text
VALID → USED
checked_in_at = now
```

Scan lần hai bị reject.

Staff không được chỉnh trạng thái payment/order.

---

# 19. Combo redemption

Nếu order có combo:

Staff sau khi scan có thể xác nhận giao combo.

Điều kiện:

```text
order PAID
combo_id != NULL
combo_redeemed_at IS NULL
```

Thành công:

```text
tickets.combo_redeemed_at = now
```

Không giao lần hai.

---

# 20. Self-Refund

Điều kiện đơn giản:

- order `PAID`;
- ticket `VALID`;
- ticket chưa check-in;
- combo chưa redeemed;
- còn ít nhất 45 phút trước showtime;
- chưa refund trước đó.

Không dùng quota 2 lần/tháng để giảm độ phức tạp.

Transaction:

```text
lock order + ticket + user + seats
→ order REFUNDED
→ ticket REFUNDED
→ order_seats ACTIVE → REFUNDED
→ showtime_seats SOLD → AVAILABLE
→ users.credit_balance += orders.total_amount
```

Sau commit:

```text
emit TICKET_REFUNDED
emit SEAT_RELEASED
send FCM
```

Refund luôn thành N8 Refund Credit, không tự chuyển tiền lại ngân hàng.

---

# 21. Hủy suất chiếu

Chỉ ADMIN được hủy.

Chỉ cho hủy trước `showtime.start_at`.

Nếu đã có ticket `USED`, từ chối hủy tự động.

Flow:

```text
showtime → CANCELED
```

Với order `PENDING_PAYMENT`:

```text
order → CANCELED
order_seats HELD → CANCELED
seats → AVAILABLE
restore credit_used
```

Với order `PAID` và ticket `VALID`:

```text
order → CANCELED
ticket → CANCELLED
order_seats ACTIVE → CANCELED
seats → AVAILABLE
users.credit_balance += orders.total_amount
```

Sau commit:

```text
emit SHOWTIME_CANCELLED
emit SEAT_RELEASED
send FCM
```

Không cần Staff xử lý từng order.

---

# 22. Email vé sau khi mua thành công

Sau khi ticket được issue thành công, backend gửi email đến:

```text
users.email
```

Dùng:

```text
Nodemailer + Gmail SMTP
```

Email gồm:

- tên phim;
- ngày/giờ chiếu;
- format;
- phòng;
- ghế;
- combo nếu có;
- tổng tiền;
- Refund Credit đã dùng;
- số tiền chuyển khoản;
- booking code;
- QR vé.

Subject gợi ý:

```text
[N8 Cineplex] Đặt vé thành công - {booking_code}
```

## 22.1. Email không được làm hỏng booking

Payment/ticket transaction phải commit trước.

Sau đó mới gửi email.

Nếu email lỗi:

```text
ticket vẫn VALID
order vẫn PAID
email_sent_at vẫn NULL
```

Cron `retry-ticket-email` quét ticket mới có `email_sent_at IS NULL` rồi gửi lại.

Gửi thành công:

```text
tickets.email_sent_at = now
```

Không cần bảng email queue riêng.

---

# 23. FCM notification

Push:

- payment success;
- ticket issued;
- reminder trước showtime 1 giờ;
- showtime cancelled;
- self-refund completed.

Một user lưu một `fcm_token` hiện tại trong `users`.

Đây là simplification cho đồ án; không hỗ trợ nhiều installation token cùng lúc.

FCM chỉ là notification, không phải source of truth.

Khi tap notification:

```text
deep link
→ mở screen
→ GET server state
```

---

# 24. WebSocket / Socket.IO

## 24.1. Channels

```text
showtime:{showtimeId}
user:{userId}
```

Client authenticate socket bằng JWT handshake.

Backend tự suy ra `user:{userId}` từ JWT.

## 24.2. Events

Showtime channel:

```text
SEAT_HELD
SEAT_RELEASED
SEAT_SOLD
SHOWTIME_CANCELLED
```

User channel:

```text
PAYMENT_PAID
ORDER_EXPIRED
TICKET_REFUNDED
SHOWTIME_CANCELLED
```

## 24.3. Quy tắc

Chỉ emit sau DB commit.

Socket event chỉ là signal để UI cập nhật.

Client khi nhận event có thể update local state nhanh, nhưng khi reconnect phải fetch lại REST.

Một NestJS instance không cần Redis Socket.IO adapter.

---

# 25. Required APK Update

App self-host APK, không phụ thuộc Google Play.

Endpoint:

```text
GET /api/v1/app/version
```

Config lấy từ environment/config file:

```text
LATEST_VERSION_CODE
MIN_SUPPORTED_VERSION_CODE
APK_URL
APK_SHA256
```

Nếu app quá cũ:

```text
HTTP 426
APP_UPDATE_REQUIRED
```

UI:

```text
Update Now
Exit
```

Không có Skip/Later.

Flow:

```text
download APK
→ verify SHA-256
→ Android Package Installer
```

Không cần `app_releases` table.

---

# 26. Database — đúng 10 bảng

Database có đúng **10 bảng nghiệp vụ**:

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

Không JSONB.

Không tạo thêm bảng domain khác nếu requirement chưa thay đổi.

---

# 27. Chi tiết schema

## 27.1. `users` — 12 fields

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

Constraints:

```text
email UNIQUE
google_id UNIQUE when not null
credit_balance >= 0
```

## 27.2. `movies` — 14 fields (quyết định tuần 5, 2026-10-10)

```text
id
title
poster_url nullable
duration_minutes
synopsis nullable
age_rating
genre nullable
language nullable
source_url UNIQUE
original_title nullable
release_date nullable (DATE)
director nullable
cast nullable
updated_at
```

## 27.3. `auditoriums` — 8 fields

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

Type:

```text
STANDARD
IMAX
SCREENX
GOLD
```

## 27.4. `showtimes` — 11 fields

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

Status:

```text
ACTIVE
CANCELED
```

Constraints:

```text
source_key UNIQUE
```

## 27.5. `showtime_seats` — 11 fields

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

Constraint:

```text
UNIQUE(showtime_id, seat_code)
```

Status:

```text
AVAILABLE
HELD
SOLD
BLOCKED
```

## 27.6. `combos` — 8 fields

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

Status:

```text
AVAILABLE
UNAVAILABLE
```

## 27.7. `orders` — 12 fields

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

Status:

```text
PENDING_PAYMENT
PAID
EXPIRED
CANCELED
REFUNDED
```

## 27.8. `order_seats` — 7 fields

```text
id
order_id
showtime_seat_id
seat_code
price
status
created_at
```

Status:

```text
HELD
ACTIVE
CANCELED
REFUNDED
```

## 27.9. `payments` — 12 fields

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

Status:

```text
PENDING
PAID
EXPIRED
NEEDS_ADMIN
RESOLVED
```

Issue reason:

```text
WRONG_AMOUNT
WRONG_REFERENCE
LATE_PAYMENT
```

Constraints:

```text
payment_code UNIQUE
provider_transaction_id UNIQUE when not null
```

## 27.10. `tickets` — 12 fields

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

Constraints:

```text
order_id UNIQUE
booking_code UNIQUE
qr_token_hash UNIQUE
```

Status:

```text
VALID
USED
REFUNDED
CANCELLED
EXPIRED
```

---

# 28. Quan hệ ERD

```text
users
  |
  +---- orders ---- payments
  |        |
  |        +---- order_seats ---- showtime_seats
  |        |
  |        +---- tickets
  |        |
  |        +---- combos
  |
movies ---- showtimes ---- auditoriums
                 |
                 +---- showtime_seats
```

Foreign keys chính:

```text
showtimes.movie_id → movies.id
showtimes.auditorium_id → auditoriums.id
showtime_seats.showtime_id → showtimes.id
showtime_seats.held_by_user_id → users.id
orders.user_id → users.id
orders.showtime_id → showtimes.id
orders.combo_id → combos.id
order_seats.order_id → orders.id
order_seats.showtime_seat_id → showtime_seats.id
payments.order_id → orders.id
tickets.order_id → orders.id
```

---

# 29. API scope

Base:

```text
/api/v1
```

## 29.1. Auth

```text
POST /auth/register
POST /auth/verify-email
POST /auth/login
POST /auth/google
POST /auth/refresh
POST /auth/logout
POST /auth/forgot-password
POST /auth/reset-password
POST /auth/change-password
```

## 29.2. Profile

```text
GET   /me
PATCH /me
POST  /me/avatar
DELETE /me/avatar
POST  /me/change-email
DELETE /me
```

## 29.3. Catalog

```text
GET /movies
GET /movies/:id
GET /movies/:id/showtimes
GET /showtimes/:id
GET /showtimes/:id/seats
GET /combos
```

## 29.4. Booking

```text
POST /orders
GET  /orders/:id
POST /orders/:id/cancel
GET  /orders/:id/payment
```

`POST /orders` nhận:

```text
showtime_id
seat_ids[]
combo_id optional
combo_quantity
```

Backend tự tính giá.

## 29.5. Payment

```text
POST /orders/:id/payment
POST /webhooks/sepay
GET  /payments/:id
```

## 29.6. Ticket

```text
GET  /tickets
GET  /tickets/:id
POST /tickets/:id/refund
```

## 29.7. Staff

```text
POST /staff/tickets/scan
POST /staff/tickets/:id/check-in
POST /staff/tickets/:id/redeem-combo
```

## 29.8. Admin

```text
GET   /admin/staff
POST  /admin/staff
PATCH /admin/staff/:id

GET   /admin/combos
POST  /admin/combos
PATCH /admin/combos/:id
DELETE /admin/combos/:id

GET  /admin/orders
GET  /admin/showtimes
POST /admin/showtimes/:id/cancel

GET  /admin/payment-issues
GET  /admin/payment-issues/:id
POST /admin/payment-issues/:id/mark-bank-refunded
POST /admin/payment-issues/:id/resolve
```

## 29.9. App

```text
GET /app/version
GET /health
```

---

# 30. Android screens

## Customer

```text
Splash / Required Update
Login
Register
Email Verification
Forgot Password
Home
Search
Movie Detail
Showtime Selection
Seat Selection
Combo
Checkout
SePay VietQR Payment
Payment Result
My Tickets
Ticket Detail / QR
Self-Refund Confirm
Refund Credit
Profile
Edit Profile
Change Avatar
Change Email
Change Password
Booking History
```

## Staff

```text
Staff Login
QR Scanner
Ticket Result
Combo Redeem
```

## Admin

```text
Admin Home
Staff Accounts
Combos
Orders
Showtimes
Payment Issues
```

---

# 31. Background jobs bằng NestJS Cron

Không BullMQ.

Các cron job:

```text
crawl-cgv
expire-seat-holds
expire-tickets
send-ticket-reminders
retry-ticket-email
```

## `crawl-cgv`

- crawl dữ liệu;
- upsert;
- auto room assignment;
- materialize seats.

## `expire-seat-holds`

- tìm hold hết hạn;
- expire order;
- release seats;
- restore credit;
- emit realtime.

## `expire-tickets`

Sau T+30:

```text
VALID → EXPIRED
```

Không đổi `USED/REFUNDED/CANCELLED`.

## `send-ticket-reminders`

Gửi FCM khoảng 1 giờ trước suất nếu ticket vẫn `VALID`.

## `retry-ticket-email`

Gửi lại email cho ticket mới có:

```text
email_sent_at IS NULL
```

---

# 32. Security tối thiểu cần giữ

- HTTPS;
- password hash;
- JWT access token;
- refresh token hash;
- Android Keystore;
- DTO validation;
- role guard;
- ownership check;
- email verification trước booking;
- unique email/google ID;
- webhook authentication/validation;
- unique payment transaction ID;
- transaction cho booking/payment/refund;
- không tin giá/status từ client;
- secret chỉ trong environment;
- QR token opaque;
- rate limit cơ bản cho auth;
- validate avatar upload;
- crawler chỉ truy cập HTTP(S) host cho phép.

Không lưu:

- bank password;
- bank OTP;
- thẻ PAN/CVV.

---

# 33. Testing priorities

## Auth

- register;
- verify email;
- login/logout;
- refresh token;
- Google login;
- duplicate email;
- unverified user cannot book.

## Crawler

- parse movie;
- parse showtime;
- duplicate upsert;
- missing format/duration skipped;
- room assignment;
- seat materialization.

## Seat / Booking

- hold 10 phút;
- same-seat concurrency;
- expired hold release;
- couple seat completeness;
- order_seats history retained after refund/resale.

## Pricing

- ticket subtotal;
- combo quantity;
- promotion;
- Refund Credit;
- total never negative.

## Payment

- real webhook confirmation;
- exact code + amount → PAID;
- duplicate webhook does not double issue ticket;
- wrong amount/reference → NEEDS_ADMIN;
- late payment → NEEDS_ADMIN;
- expired order never revives;
- zero bank amount skips payment row.

## WebSocket

- seat hold emit;
- release emit;
- sold emit;
- payment paid emit;
- reconnect refetch REST;
- polling fallback works.

## Ticket

- QR valid;
- duplicate scan rejected;
- combo redeemed once;
- ticket expires after T+30.

## Refund

- >=45 min success;
- <45 min reject;
- used ticket reject;
- redeemed combo reject;
- seat becomes AVAILABLE;
- `order_seats` becomes REFUNDED;
- credit added exactly once.

## Email

- PAID → ticket → email;
- email contains booking data + QR;
- email failure does not rollback order;
- retry job sends later;
- email_sent_at updated on success.

## Admin cancellation

- pending order cancelled and credit restored;
- paid valid ticket gets credit;
- used ticket blocks automatic cancellation;
- realtime + FCM sent.

---

# 34. Deployment

Docker Compose services:

```text
api
postgres
reverse-proxy
```

Không cần:

```text
redis
bullmq-worker
kafka
```

Environment tối thiểu:

```text
DATABASE_URL
JWT_ACCESS_SECRET
JWT_REFRESH_SECRET
GOOGLE_CLIENT_ID
SEPAY_* secrets
GMAIL_USER
GMAIL_APP_PASSWORD
FCM credentials
APK_URL
APK_SHA256
LATEST_VERSION_CODE
MIN_SUPPORTED_VERSION_CODE
```

Public:

```text
443 → API / WebSocket / APK
```

---

# 35. Những gì cố ý không làm

Để giữ đồ án vừa sức, không triển khai:

- microservices;
- Kafka;
- Redis;
- BullMQ;
- Kubernetes;
- event sourcing;
- CQRS framework;
- Elasticsearch;
- payment receipt aggregation;
- underpaid multi-transaction matching;
- overpaid automation;
- outbox pattern;
- audit log database;
- idempotency database table;
- token family/reuse detection;
- nhiều device FCM token/user;
- promotion table phức tạp;
- Refund Credit ledger;
- seat template table;
- crawler review workflow;
- Staff payment reconciliation.

Những phần bị bỏ **không làm mất luồng chính của người dùng**.

---

# 36. Tiêu chí hoàn thành

Đồ án được xem là hoàn thành khi:

- Android app chạy được end-to-end;
- backend NestJS + PostgreSQL chạy ổn định;
- database đúng 10 bảng;
- crawler lấy được phim/lịch chiếu;
- user chọn/giữ ghế và realtime sync được;
- không double-book;
- combo/promotion/Refund Credit tính đúng;
- SePay/VietQR xác nhận được giao dịch thật;
- order đổi `PAID` tự động;
- ticket QR được tạo;
- FCM hoạt động;
- email vé gửi được;
- Staff scan/check-in và redeem combo được;
- self-refund hoạt động;
- Admin cancel showtime và xử lý payment issue tối thiểu được;
- required update hoạt động;
- Swagger có thể demo API;
- Docker Compose deploy được.

---

# 37. Quyết định bổ sung cho tuần 4


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


## Cập nhật Android 2026-10-09 — đăng nhập và profile từ ZIP mới

### Quyết định và phạm vi

Trạng thái: **Hoàn thành phần UI mock**. Bản thiết kế hiện hành là `design/signin-signup.zip` mới của người dùng, được giải nén tại `design/signin-signup/`. Dùng index.html, assets/cineplex.css, assets/cineplex.js và phần bổ sung cuối HANDOFF.md làm tham chiếu giao diện. Quyết định này thay cách mở dialog tài khoản trước đây bằng trang Profile; không thay đặc tả xác thực/profile thật ở mục 5 và 29.

- Đăng nhập khách hàng: thêm dòng “hoặc” và nút “Đăng nhập bằng Google” nền trắng, viền #D9D3D6, bo 12dp, cao tối thiểu 56dp; dùng logo gốc trong ZIP, rộng 20dp, giữ tỷ lệ 200:204. Khóa nút trong lúc submit; không xuất hiện ở đăng ký/đăng nhập nhân viên. Bấm chỉ mở “Chức năng sẽ được bổ sung”, không tạo phiên/OAuth giả.
- Profile: route PROFILE và ProfileScreen Compose riêng, mở qua avatar khách hàng/admin hoặc navigation Tài khoản. Session mock gồm name, email chuẩn hóa và role; không chứa password. Dùng dữ liệu của tài khoản đăng nhập/đăng ký, không thêm số điện thoại/ngày sinh/dữ liệu giả.
- Nội dung gồm tiêu đề “Thông tin cá nhân”, avatar initials từ hai từ cuối tên, tên/role, thẻ “Thông tin tài khoản” với họ tên/email/loại tài khoản, chỉnh sửa, “Bảo mật & hỗ trợ”, đổi mật khẩu/chính sách bảo mật/trợ giúp, đăng xuất.
- Giữ palette Cineplex, heading sans-serif hỗ trợ dấu tiếng Việt 32/24sp, avatar 80dp, padding 24dp trên mobile và 48dp trên tablet; nội dung cuộn được. Từ 768dp chia hai cột nhận diện/nội dung, giới hạn rộng 960dp. Dùng chung CustomerNavigation với Home; chọn Tài khoản ở Profile, admin không có navigation khách hàng.
- Back/Trang chủ khôi phục Home cùng query/tab/scroll của khách hàng; admin trở về dashboard. Back khi dialog đang mở chỉ đóng dialog. Logout xóa session, reset Home, về login; mở tài khoản và Back sau logout không khôi phục màn hình được bảo vệ.
- Chỉnh sửa/đổi mật khẩu/trợ giúp mở thông báo đúng export; chính sách bảo mật dùng nội dung mẫu hiện có. Các mục Phim/Vé của tôi giữ thông báo hiện có.
- Sửa lỗi lint có sẵn: tách Theme.Cineplex.Base và đặt windowLightNavigationBar trong values-v27 để giữ minSdk 24; logo Google ở drawable-nodpi. Không thêm dependency, backend/API/database hoặc business rule.

### Bằng chứng kiểm tra 2026-10-09

- Local Android: `assembleDebug testDebugUnitTest lintDebug --console=plain` thành công. 18 unit tests đạt, gồm email chuẩn hóa trong session, route/role/back/logout profile và Google không tạo phiên/khóa lúc gửi. Lint 0 lỗi, 23 cảnh báo còn lại; không tắt/bỏ qua lint hoặc test.
- Emulator Pixel_10_Pro_XL Android 17, 360×800dp: `connectedDebugAndroidTest` thành công, 10 tests (9 CineplexUiTest và 1 test có sẵn), 0 lỗi/thất bại/bỏ qua.
- APK cuối cùng sau sửa resource: 3 test Google/customer profile/admin profile chạy lại bằng AndroidJUnitRunner đạt cả ở 360×800dp và tablet 820×1180dp. Kiểm tra avatar và navigation, thông tin/email, dialog, chọn Tài khoản, Back/Trang chủ giữ query và vị trí scroll, admin về đúng dashboard, logout chặn mở lại profile.
- Đã xem ảnh chụp login Google, profile khách hàng/admin và phần thao tác; mobile cuộn và tablet hai cột hiển thị đúng. Ảnh chỉ ở `N8_Cineplex/app/build/visual-profile-360/visual_checks/` và `visual-profile-tablet/visual_checks/`, không đưa vào source. Emulator đã khôi phục kích thước gốc 1344×2992; APK mới được cài và MainActivity mở thành công.
- APK: `N8_Cineplex/app/build/outputs/apk/debug/app-debug.apk`. Không commit/push; không sửa backend nên không rebuild/deploy/kiểm tra VPS trong đợt này. Giữ lịch sử kiểm tra cũ và các thay đổi có sẵn của người dùng.

Phần còn lại ngoài scope đợt UI: Google OAuth và cập nhật profile/email/password/avatar qua backend thật theo các mục đã đặc tả, vẫn **Chưa làm** trong bản UI mock này. Bước tiếp theo: người dùng review giao diện, rồi tích hợp khi backend tương ứng sẵn sàng.
## Triển khai Auth/profile tuần 5 — 2026-10-10

Trạng thái: **Đang làm**. Scope: Auth và profile thật cho backend + Android, deadline 11/10/2026; catalog/Home/booking/payment/quản trị nghiệp vụ giữ nguyên cho các tuần sau.

Quyết định: hai cổng CUSTOMER và STAFF (STAFF/ADMIN); access 15 phút, refresh 7 ngày, một refresh hash/user và không rotation; logout không blacklist access token. Google không ghép tài khoản cùng email; Google-only không tạo/reset mật khẩu Cineplex, đổi email phải Google reauthentication. Signed email token: verify/change-email 30 phút, reset 15 phút; link fragment và xác nhận thủ công. Avatar JPEG/PNG/WebP <=5 MB, chuyển WebP <=512px, đầu vào <=16MP; storage riêng persistent. Không thay schema/10 bảng.

SMTP: Nodemailer + Gmail TLS; GMAIL_USER là group08.nt118.r14@gmail.com; GMAIL_APP_PASSWORD dùng credential người dùng cung cấp, chỉ trong cấu hình riêng, không ghi giá trị vào source/tài liệu/log. Email test anhnguyenfb2021@gmail.com và alias riêng theo run ID. Không dùng password Gmail làm password Cineplex.

E2E được phép tạo CUSTOMER tạm trong DB chung; runner ngoài source phải ghi ledger UUID/run ID/email chính xác, cleanup trong finally. Không xóa bằng email đơn độc. Kiểm tra role CUSTOMER/credit=0/không order hoặc held seat trước khi xóa; có dữ liệu không chứng minh được thì rollback và xin xác nhận. Không tác động CUSTOMER thật/STAFF/ADMIN; kiểm tra DB và ảnh sau cleanup. Fixtures khác luôn rollback; không reset/restore DB.

Key SSH cập nhật: D:\NT118\vpskey\shibakey.ppk, user ubuntu; giữ host-key validation. Google OAuth IDs còn thiếu, đang yêu cầu thông tin. Đã lưu baseline source bên ngoài source; backend typecheck baseline exit 0. Chưa triển khai Auth local/VPS, chưa tạo account test, chưa gửi SMTP; không ghi hoàn thành trước bằng chứng. Bước tiếp theo: backend/API/tests -> Android -> local -> giai đoạn VPS được cho phép -> E2E/cleanup. Mọi tiến độ mới phải cập nhật cả hai tài liệu.


### Tiến độ và đặc tả Auth/profile bổ sung — 2026-10-10

Trạng thái tổng thể: **Đang làm**; thay thế ghi nhận trước đó về việc chưa có Auth local/chưa gửi SMTP/chưa có OAuth IDs. Không thay đổi các kết quả lịch sử UI mock và bootstrap.

- Backend local đã triển khai POST `/auth/register`, `/auth/login`, `/auth/google`, `/auth/refresh`, `/auth/logout`, `/auth/verify-email`, `/auth/resend-verification`, `/auth/forgot-password`, `/auth/reset-password`, `/auth/change-password`; GET/PATCH `/me`, POST `/me/change-email`, POST/DELETE `/me/avatar`, GET `/assets/avatars/:userId/:name`. Prefix `/api/v1`. GET verify/reset chỉ phục vụ trang xác nhận, không thay đổi DB; token nằm ở fragment, không query URL. Password Argon2id, refresh JWT chỉ lưu SHA-256 hash. Access 15 phút, refresh 7 ngày; logout/đổi mật khẩu/đổi email hủy refresh, access đã cấp có thể còn hiệu lực đến hết 15 phút. Role và active luôn đọc từ DB; không cho client sửa role/credit/status.
- Google ID token xác minh bằng google-auth-library (signature/issuer/audience/expiry và email_verified). Không ghép tài khoản email/password cùng email. Google-only không tạo/reset mật khẩu Cineplex; đổi email phải đúng Google sub với ID token phát hành trong 5 phút gần nhất (cho phép lệch giờ tương lai tối đa 60 giây). Login Google sau không ghi đè tên/email/avatar đã sửa.
- Config riêng: DATABASE_URL, JWT_ACCESS_SECRET/JWT_REFRESH_SECRET/JWT_EMAIL_SECRET (ba giá trị khác nhau, >=32 ký tự), GMAIL_USER/GMAIL_APP_PASSWORD, GOOGLE_CLIENT_ID, PUBLIC_BASE_URL, AVATAR_UPLOAD_DIR; TRUST_PROXY=true chỉ ở sau proxy tin cậy. SMTP credential đã lưu trong backend/.env riêng, không nằm trong source/tài liệu/artifacts. SMTP authentication/TLS đã đạt. Không đổi schema/migration: giữ 10 bảng/103 cột canonical.
- Android MainActivity dùng ApiAccountRepository thật (không fallback mock khi lỗi mạng); Retrofit/OkHttp/Kotlin Serialization, Credential Manager, Coil. Refresh token mã hóa AES-GCM bằng Android Keystore trong DataStore, loại trừ backup; access chỉ RAM; refresh single-flight, retry 401 tối đa một lần. CUSTOMER/STAFF/ADMIN có route riêng; STAFF chỉ home/profile/logout ở scope tuần 5. Photo Picker có xem trước và giới hạn 5 MB. MockAccountAdapter chỉ được truyền tường minh trong tests cũ.
- Android debug mặc định API http://10.0.2.2:5000/api/v1/; backend vẫn bind localhost. Cleartext chỉ cho debug loopback/emulator, release cấm cleartext. `cineplexApiUrl` và `cineplexReleaseApiUrl` cấu hình Gradle theo môi trường; release chưa có endpoint tuần 5 đã kiểm tra nên mặc định https://localhost/api/v1/, cần cấu hình đúng khi triển khai. Không hardcode IP VPS trong backend.
- Google Cloud project đã tạo: `n8-cineplex-nt118-2026` (N8 Cineplex), owner tài khoản người dùng đã đăng nhập, không bật billing/free trial. OAuth External/Testing, support email thnguyen290106@gmail.com, contact group08.nt118.r14@gmail.com, test user anhnguyenfb2021@gmail.com; chỉ openid/email/profile, không sensitive/restricted scopes. Dùng Console hiện có; không cần cài Google Cloud/Firebase CLI hay Firebase Auth.
- Web Client ID: `802546348670-pm4d8tej6fd9e0bencenp6e7r5vskcos.apps.googleusercontent.com`; backend GOOGLE_CLIENT_ID và Android `googleWebClientId` ở gradle.properties dùng cùng ID công khai này. Android debug Client ID: `802546348670-4q37v415cp7c0uavuva1pgj14hacoo8r.apps.googleusercontent.com`, package com.nhom8.cineplex. Không cần client secret cho xác minh ID token; không tải/lưu client secret.
- signingReport đã chạy thành công: debug SHA-1 D6:C5:F9:15:7E:56:10:B4:70:0A:07:EE:02:12:61:25:EC:55:04:56; SHA-256 9A:20:42:E7:B2:7D:3B:05:15:E0:8F:1D:6A:5B:35:81:97:A6:4A:0D:5B:31:44:EF:42:6C:FA:A8:55:E2:9E:6C. Console Android OAuth chỉ yêu cầu SHA-1; SHA-256 đã lấy nhưng không có trường để nhập ở client này. Release chưa có signing config nên chưa tạo client release; máy/keystore khác phải có client fingerprint tương ứng.

#### Bằng chứng local hiện tại

- Backend build/typecheck/Prisma validate/Prettier đạt. `npm test`: 27 tests (14 Auth + 13 crawler), 0 fail/skip. Kiểm tra Google unit dùng fixture, không được coi là đăng nhập Google thật.
- Android assembleDebug + testDebugUnitTest + lintDebug đạt; 22 unit tests, 0 fail; lint 0 lỗi/41 cảnh báo. Connected UI lần đầu: 10 tests, 8 pass/2 fail do kỳ vọng placeholder quên mật khẩu/hỗ trợ/bảo mật cũ; đã cập nhật assertions theo luồng mới, chưa chạy lại sau sửa. Test Keystore/DataStore mới chưa chạy trên thiết bị. Không tắt lint/tests.
- E2E HTTP local dùng PostgreSQL chung và Gmail SMTP thật: register/duplicate/verify one-use/login/refresh/role injection reject/edit profile/upload-delete avatar/forgot-reset/change password/change email/logout đạt, runner exit 0. Token email được giữ trong RAM ở ranh giới SMTP để kiểm tra, không ghi log; chưa kiểm tra thư trong hộp Gmail của người nhận, nên chưa xác nhận thao tác qua link nhận trong inbox.
- CUSTOMER tạm có UUID do chính runner tạo và ledger ngoài source; cleanup finally đã xóa đúng UUID, refresh hash cùng row, avatar cục bộ; DB kiểm tra không còn user/order/held seat liên quan. Auth test không tạo booking/payment/ticket; gặp dữ liệu liên quan không chứng minh được thì từ chối cleanup. Snapshot hash các tài khoản khác trước/sau không đổi. Không xóa tài khoản chỉ theo email, không sửa/xóa STAFF/ADMIN/tài khoản thật.
- Transaction source: baseline typecheck exit 0; modified typecheck exit 0; executable ROLLBACK.sh đã chạy trên bản sao, hash khớp original và typecheck exit 0. Cùng probe POST /api/v1/auth/login {}: BASELINE 404, MODIFIED 400 (DTO validation), ROLLBACK 404, tất cả runner exit 0. DIFF áp dụng bằng patch --binary, tái tạo snapshot modified byte-for-byte. Bốn role và ledger nằm ngoài source tại D:\NT118\verification\week5-auth-profile-20261010; không chứa env/key/keystore.
- VPS hiện có health HTTPS 200/database up; chưa cập nhật hoặc kiểm tra API Auth tuần 5 trên VPS. Không dùng kết quả health bootstrap làm bằng chứng Auth đã triển khai VPS. Không chạy migration/reset/restore DB.

Phần còn lại / bước tiếp theo: người dùng đăng nhập Gmail test trên emulator (bước xác thực bắt buộc); chạy Google end-to-end qua Android/API thật với ledger UUID và cleanup tự động, kiểm tra lại DB; chạy lại connected UI + Keystore tests sau khi người dùng xong bước đăng nhập; kiểm tra inbox/link nhận thật; cập nhật/kiểm tra VPS theo quy trình được phép. Chưa báo hoàn thành tuần 5.

### Chuyển Android sang VPS theo yêu cầu — 2026-10-10

Trạng thái: **Đang làm**. Thay quyết định debug API dùng emulator-local và release localhost trước đó: cả hai mặc định API_BASE_URL=https://18.143.100.43/api/v1/. Các Gradle override vẫn dùng khi chủ động chọn môi trường khác. Không thay hàng loạt loopback: backend local HOST, DB SSH tunnel, PostgreSQL private bind và container healthcheck giữ địa chỉ nội bộ; API VPS bind 0.0.0.0 sau Nginx HTTPS 443. Không public 5000/5432 hoặc bỏ kiểm tra TLS.

Quan sát ban đầu: backend local port 5000 không chạy; VPS health 200 nhưng POST /api/v1/auth/login {} trả 404 (Auth tuần 5 chưa có trên VPS). Android build/unit/lint sau đổi URL đã đạt. Đang triển khai backend source snapshot đã kiểm tra bằng archive SHA-256/per-file manifest tới /opt/n8-cineplex-releases/week5-86462322a92a0794, không thay source checkout cũ; đây là ngoại lệ triển khai snapshot theo yêu cầu dùng VPS để thử ngay, nhận dạng bằng content hash thay quy trình version cũ. Cấu hình vận hành vẫn /etc/n8-cineplex; giữ runtime/config trước đó để rollback API, không restore DB. Không cần migration vì schema không đổi. Runtime phải giữ optional native packages cho sharp và kiểm tra Argon2/sharp trên Linux. Auth secrets riêng ở VPS, avatar volume persistent, TRUST_PROXY phía Nginx và giới hạn upload 6 MB ở proxy/5 MB tại API.

Bước tiếp theo: xác minh deploy healthy/health/Swagger/Auth, test HTTPS từ Android bằng OkHttp mặc định và chạy lại UI/Keystore, cập nhật kết quả thật vào cả hai tài liệu. Chưa ghi deploy hoàn thành khi build đang chạy.
### Kết quả chuyển endpoint và triển khai VPS — 2026-10-10

Trạng thái hạng mục kết nối: **Hoàn thành**. Trạng thái tổng thể Auth/profile tuần 5 vẫn **Đang làm** vì Google Login E2E và inbox thực tế còn chưa xác minh.

- Cả debug/release BuildConfig.API_BASE_URL mặc định https://18.143.100.43/api/v1/; đã đọc BuildConfig.java generated xác nhận. Override Gradle chỉ dùng khi chọn môi trường khác chủ động. Không thay loopback DB/tunnel/healthcheck bằng public IP; API container bind 0.0.0.0:5000, chỉ Nginx public HTTPS 443. Không bỏ kiểm tra TLS.
- VPS đã triển khai và kiểm tra source snapshot week5-86462322a92a0794 tại /opt/n8-cineplex-releases/week5-86462322a92a0794; archive SHA-256 86462322a92a0794548d12b948aaaa5d85e5102179d7fa18e8c86c652be2e174 và source-manifest.json kiểm tra per-file. APP_SHA của cấu hình vận hành đang là release label này; xác minh source bằng manifest/content hash, không suy diễn định danh version cũ. Cấu hình chạy vẫn ở /etc/n8-cineplex, Docker Compose mặc định đã trỏ context/image mới; source checkout cũ không thay đổi.
- Runtime image đã build trên VPS; Argon2/sharp native Linux self-check đạt. Dockerfile runtime không omit optional packages vì sharp cần binary theo platform. Container API n8-cineplex-api:week5-86462322a92a0794 healthy; log health/startup và HTTPS health 200/database up, Swagger 200, POST /auth/login {} 400, Google invalid token 401. VPS SMTP authentication/TLS đạt. Env auth root-only, secrets không nằm trong build context/source/tài liệu; avatar persistent /var/lib/n8-cineplex/avatars, TRUST_PROXY=true, Nginx upload 6 MB và API limit 5 MB.
- PostgreSQL container cũ f3282c84ca8c và volume giữ nguyên; không migration/reset/restore, không mở 5000/5432 public. Runtime/config trước đó còn tại /etc/n8-cineplex/rollback-week5-86462322a92a0794; chưa thực thi rollback VPS vì deploy/verification thành công. Source rollback vẫn kiểm tra trên bản sao, không restore DB.
- Android assembleDebug/testDebugUnitTest/lintDebug/connectedDebugAndroidTest đạt (exit 0); 22 unit tests và 12 instrumentation tests (0 fail/error/skip), gồm AuthServerConnectivityTest dùng OkHttp mặc định kiểm tra TLS từ emulator tới VPS và TokenVaultTest ciphertext roundtrip/clear. Đã sửa lỗi compile test nullable response body và tên tham số exact không có trong Compose; không tắt/bỏ qua test. Các kỳ vọng UI cũ đã cập nhật và tất cả kiểm tra lại đạt. Lint 0 lỗi/41 cảnh báo.
- APK mới cài qua adb install -r (Success), MainActivity mở lại; người dùng có thể Stop/Run từ Android Studio để dùng config mới. Không cần backend local port 5000 chạy khi app dùng VPS. Các unit/UI mock không chứng minh đăng nhập Google thật; kiểm tra kết nối HTTPS không được coi là toàn bộ nghiệp vụ Auth VPS E2E đã đạt.

Phần còn lại: Google credential thật trên thiết bị + backend VPS và kiểm tra inbox/link nhận thật. Runner Google loopback cũ chỉ dành kiểm thử local, không được chạy để kết luận E2E với APK mặc định VPS; cần điều chỉnh runner/ledger UUID theo VPS trước khi tự động tạo/xóa Google test account. Không xóa tài khoản do người dùng đang tự thử nếu chưa có chứng minh UUID ownership/ledger tương ứng. Tiếp tục cập nhật cả hai tài liệu và giữ cùng bốn artifact roles ngoài source.

### Xác nhận Google Login thủ công Android/VPS — 2026-10-10

Trạng thái Google Login: **Hoàn thành kiểm thử thủ công đăng nhập** (người dùng xác nhận đăng nhập thành công). Đối chiếu read-only DB chung đúng anhnguyenfb2021@gmail.com, UUID 2c03b2f3-baaf-400f-ae92-56384992c6dc: CUSTOMER, Google linked, password_hash NULL, email_verified_at có giá trị, active, current refresh hash có giá trị; 0 orders và 0 held seats. Không in hash/token/Google sub. Đây là kết quả thủ công và quan sát DB, không ghi thành automated Google E2E toàn bộ flows.

Tài khoản này do người dùng đang thử thủ công, không phải UUID fixture mà runner trước đã tạo (UUID fixture cũ vẫn đã dọn sạch). Không tự nhận là runner-owned để xóa. Đã hỏi người dùng giữ tài khoản để tiếp tục profile/avatar hay đã thử xong để cleanup đúng UUID. Cleanup tạm chờ thông tin kết thúc kiểm thử/xác nhận ownership; không xóa tài khoản hay dữ liệu lúc người dùng còn đang thử. Khi dọn phải row lock và kiểm tra lại cùng UUID/role/email/Google identity/credit/FK; có order/held seat hoặc dữ liệu không chứng minh được thì từ chối và yêu cầu xác nhận. Không chạm tài khoản thật/STAFF/ADMIN hoặc dữ liệu khác.

Kết quả kết nối đã hoàn thành: API_BASE_URL debug/release HTTPS VPS, runtime healthy, 22 unit + 12 device tests đạt. Tổng thể Auth/profile vẫn Đang làm: kiểm tra inbox/link nhận thật, các luồng VPS E2E còn chưa có bằng chứng và cleanup tài khoản test thủ công sau khi người dùng xong. Giữ cùng bốn role và mọi bằng chứng trước đó.
### Email HTML, kiểm thử qua VPS và dọn selftest — 2026-10-10

Trạng thái: **Đang làm**. Quyết định này thay hướng dẫn dùng API loopback cho kiểm thử tích hợp/E2E trước đó.

- Kiểm thử ứng dụng, API tích hợp/E2E và đường dẫn trong email dùng `https://18.143.100.43/api/v1`; Swagger dùng `https://18.143.100.43/api/docs`. Không dùng `127.0.0.1`, `localhost` hay `10.0.2.2` làm endpoint kiểm thử nghiệp vụ hoặc link gửi người nhận. Unit tests cô lập dùng fixture/in-memory vẫn được phép; loopback dành cho bind nội bộ, DB SSH tunnel và container healthcheck không phải endpoint public và giữ nguyên để không mở DB ra Internet.
- `PUBLIC_BASE_URL` cấu hình riêng local/VPS dùng HTTPS VPS; validation từ chối HTTP và localhost/IPv4/IPv6 loopback. Email verify/reset/change-email có HTML tiếng Việt, nhận diện N8 Cineplex, nút hành động, hạn dùng 30/15/30 phút, cảnh báo bảo mật và plain-text fallback. Token vẫn ở fragment; không log hoặc đưa token thật vào bằng chứng. Thư đã gửi trước đây không thể sửa; phải yêu cầu thư mới sau cập nhật.
- Sau khi kiểm thử kết thúc, **xóa toàn bộ file selftest tạm đã tạo**, cả trong source, ngoài source và staging VPS; không chỉ chuyển sang thư mục khác. Xóa runner/probe/script kiểm thử tạm sau lần sử dụng cuối; kiểm tra lại không còn file tương ứng. Chỉ giữ bằng chứng đã khử dữ liệu nhạy cảm, manifest/ledger kết quả và bốn vai trò MODIFIED_FILE/DIFF_FILE/VERIFICATION/ROLLBACK. `ROLLBACK.sh` là công cụ khôi phục bắt buộc, không phải selftest tạm. Không xóa unit/instrumentation/regression tests chính thức của dự án hoặc script vận hành đang dùng. Nếu gặp lỗi quyền/chính sách, ghi rõ file chưa xóa, không nhận đã dọn xong và không vượt qua kiểm soát.
- Người dùng đã xác nhận thử xong; cleanup tài khoản Google CUSTOMER test UUID `2c03b2f3-baaf-400f-ae92-56384992c6dc` đã chạy exit 0: đúng UUID/email/role/Google identity, row lock, không order/held seat; user và refresh hash đã xóa, avatar riêng không còn, snapshot tài khoản khác không đổi, schema vẫn 10 bảng/103 cột. Ghi nhận này thay trạng thái chờ cleanup ở mục trước; không chạy lại thao tác xóa trên UUID đã vắng mặt.

Bước tiếp theo: build/regression tests, cập nhật runtime VPS và xác minh template/HTTPS/SMTP thực tế; sau đó dọn selftest tạm, đồng bộ bằng chứng và mở lại cả hai tài liệu. Chưa xác minh giao diện thư trong inbox Gmail của người nhận; không ghi phần này đã hoàn thành.
### Kết quả email HTML và cleanup — 2026-10-10

Trạng thái hạng mục email: **Hoàn thành code, triển khai và kiểm tra SMTP/API VPS**. Thay trạng thái Đang làm của hạng mục email phía trên; tổng thể Auth/profile vẫn **Đang làm** ở phần inbox/link người nhận chưa xác minh.

- Đã sửa `MailService.send` để gửi HTML + plain-text fallback cho verify/reset/change-email; `validateEnvironment.PUBLIC_BASE_URL` từ chối HTTP và loopback; cấu hình riêng local/VPS dùng `https://18.143.100.43/api/v1`. Không đổi HOST/DB tunnel/internal healthcheck, không đổi schema.
- Backend build, 30 regression tests (17 Auth + 13 crawler), formatting đạt exit 0. Template cả ba mục đích có nút HTTPS VPS/token fragment và không có localhost; SMTP lỗi trả false, không lộ diagnostics. Preview HTML đã render/đọc ảnh ở mobile 390px và desktop 640px, không tràn ngang. Đây không phải bằng chứng Gmail inbox render.
- VPS đã chạy release `week5-0ec829634edc0a90`, archive SHA-256 `0ec829634edc0a90bdf585939720faba6ff62e4022ace0f67c841e0ba5d9e104`, per-file manifest khớp source. Runtime healthy; HTTPS health, verify page, reset page đều 200; kiểm tra template x3 và SMTP TLS/authentication đạt. Giữ nguyên production secrets và PostgreSQL/volume, không migration/reset/restore. Lần đầu đóng gói lỗi đọc BOM nên kiểm tra HTML trên snapshot cũ thất bại exit 1 và khôi phục config API cũ; đã sửa đọc UTF-8-sig, thêm chặn khi đóng gói lỗi, chạy lại source mới và đạt exit 0. Không dùng lần thất bại làm bằng chứng hoàn thành.
- Kiểm thử nghiệp vụ thật gọi API HTTPS VPS, không loopback: register HTTP 201/emailSent true; login CUSTOMER; change-email HTTP 200 xác nhận SMTP accepted; forgot-password HTTP 201 (response trung lập, không chứng minh inbox đã nhận). CUSTOMER fixture mới có UUID/run ID và alias do runner tạo; cleanup finally đã xóa đúng row/refresh hash, không order/held seat/avatar, snapshot tài khoản khác không đổi; token access cũ gọi /me bị từ chối 401. Các link test gửi trước cleanup không còn sử dụng được; thư cũ không tự đổi URL hay giao diện.
- Tài khoản Google test thủ công đã dọn đúng UUID theo xác nhận người dùng; không còn chờ cleanup. Script tạm trên staging VPS đã xóa và kiểm tra không còn runner .py/.mjs. Tiếp tục xóa selftest/runner/probe tạm local sau kiểm tra transaction cuối, chỉ giữ ledger/bằng chứng đã khử secrets và bốn role; regression tests chính thức giữ nguyên.

Phần chưa kiểm tra: render trong inbox Gmail thực tế/click link nhận; không tự ghi phần này đã đạt. Không cần rebuild/deploy lại khi chỉ đồng bộ tài liệu và bằng chứng sau bước này.
### Kết thúc dọn selftest tạm — 2026-10-10

Trạng thái cleanup: **Hoàn thành**. Đã xóa 22 file runner/probe/script kiểm thử tạm và cache do đợt này tạo ở thư mục kiểm chứng ngoài source; kiểm tra lại còn **0** runner tạm .mjs/.py/*.remote.sh. Staging VPS cũng đã xóa các script tạm và xác nhận không còn runner. Source không có file tên selftest; giữ nguyên regression tests chính thức của Android/backend. Không coi việc chuyển file ra ngoài source là đã xóa. Chỉ giữ kết quả/ledger/manifest, preview đã khử token thật và bốn vai trò kiểm chứng; ROLLBACK.sh được giữ vì là công cụ khôi phục bắt buộc, không phải selftest.

Kiểm tra transaction cùng input POST /api/v1/auth/login {}: BASELINE 404, MODIFIED 400, ROLLBACK 404; cả ba runner/typecheck exit 0. Rollback trên bản sao khớp hash original, patch tái tạo modified byte-for-byte. Hai tài liệu đã đồng bộ quy tắc selftest, kiểm thử tích hợp/E2E qua HTTPS VPS và kết quả email/cleanup. Không có thay đổi source backend sau triển khai email đã xác minh; không cần triển khai lại chỉ vì cập nhật tài liệu. Inbox Gmail của người nhận vẫn chưa được kiểm tra trực tiếp.
### Đồng bộ giao diện email và trang xác nhận/reset với app — 2026-10-10

Trạng thái: **Đang làm**. Thay mẫu email tím và trang xác nhận/reset nền tối trước đó bằng theme đang dùng thực tế trong MainActivity: CineplexMockTheme/CineplexColors. Màu primary #7B263D, pressed #5C1C2E, background #F5F3F0, surface #FFFFFF, text #25232A, muted #625D65, outline #D9D3D6, error #A12835, success #246345; tiêu đề serif, nội dung sans-serif, bo góc 12/16px, input tối thiểu 56px và focus rõ ràng. Không dùng theme Material tím mặc định không được app hiện hành sử dụng.

Áp dụng cho cả email verify/reset/change-email và trang HTTPS xác minh/đặt lại mật khẩu; giữ nguyên TTL, token fragment, CSP nonce, no-store/no-referrer, POST xác nhận thủ công, validation và không log token/password. Endpoint kiểm thử thật tiếp tục dùng IP VPS HTTPS, không localhost. Người dùng yêu cầu kiểm thử đầy đủ gửi thư/xác nhận/quên-reset/đổi email: cần kiểm tra bằng mail thật, hành vi token một lần/lỗi/hết hạn và cleanup đúng CUSTOMER fixture. Chưa có kết quả mới thì chưa ghi hoàn thành. Xóa toàn bộ runner/selftest tạm sau test; giữ tests chính thức, ledger đã khử secrets và bốn role kiểm chứng.
### Kết quả đồng bộ theme và kiểm thử các email — 2026-10-10

Trạng thái: **Hoàn thành phần đồng bộ theme, triển khai và kiểm thử các luồng email qua VPS**. Thay trạng thái Đang làm của hạng mục theme phía trên. Tổng thể Auth/profile không tự chuyển Hoàn thành nếu các phần khác còn thiếu bằng chứng.

- MailService.send và emailPage dùng palette CineplexColors của app hiện hành: đỏ rượu #7B263D, nền kem #F5F3F0, text #25232A, muted #625D65, viền #D9D3D6; tiêu đề serif, nút/input bo 12px, card 16px. Trang xác minh/reset responsive, focus rõ, input 56px, trạng thái lỗi/thành công đúng màu app. Giữ CSP nonce/no-store/no-referrer, token fragment xóa khỏi URL trước POST thủ công; malformed fragment không làm crash trang. Không đổi API/schema/TTL hay logic phân quyền.
- Local build, 31 regression tests (18 Auth + 13 crawler) và format check đều exit 0; test so sánh trực tiếp palette HTML với CineplexMockTheme.kt, kiểm tra CSP/nonce/form/URL. Unit tests đã bao phủ token hết hạn; chưa chờ hết TTL thực tế trên VPS.
- VPS chạy source release `week5-3ad1c2f442b0d69e`, archive SHA-256 `3ad1c2f442b0d69e060496af6a10f1642a52cf4b9baeb4ba7f717e45086b668e`, per-file manifest khớp. API healthy, health DB up, trang verify/reset HTTPS 200 có đúng theme; template x3 và SMTP TLS/authentication đạt. PostgreSQL/volume và secrets giữ nguyên; không migrate/reset/restore.
- E2E thật gọi HTTPS `https://18.143.100.43/api/v1`: register gửi thư HTML+text thật; resend tạo thư thứ hai; lấy link từ Gmail **Sent của tài khoản gửi**, chỉ đọc đúng alias test/From, dùng read-only + BODY.PEEK và TLS xác thực. Kiểm đếm đúng verify2 (register+resend), reset1, change-email1; resend cho email đã verified không gửi thêm thư. Không đọc thư của người khác hoặc sửa/xóa/đánh dấu mailbox. Password/token chỉ giữ trong RAM, không đưa vào logs/ledger/screenshot/preview.
- Browser mở link verify thật: GET không đổi DB, fragment được xóa khỏi URL, bấm xác nhận thành công 200, replay 401. Forgot lấy link reset thật: form mismatch không POST/không sửa hash, xác nhận đúng thành công 200; replay và sai-purpose 401, password cũ/refresh cũ bị từ chối, password mới login 200. Change-email lấy thư thật đến alias mới: email chưa đổi trước click, xác nhận 200, replay 401, email cũ bị từ chối/email mới login 200, refresh cũ bị thu hồi. Missing/malformed fragment hiển thị lỗi an toàn, invalid token 401, forgot email không tồn tại trả response trung lập 201; logout/refresh bị từ chối đúng trạng thái.
- Đã render preview khử token của ba loại email ở 390/640px, không tràn ngang; đã đọc ảnh email reset và trang reset từ VPS. Preview giữ ngoài source, không có token thật. Đây là kiểm tra thư thật ở Sent và chức năng link, **không phải** xác nhận delivery hoặc render trong inbox Gmail của người nhận.
- CUSTOMER fixture có UUID/run ID/alias chính runner tạo, cleanup finally exit 0: row lock và kiểm tra UUID/email/role/name/Google identity/credit/FK, xóa đúng user và refresh hash, không order/held seat/avatar, snapshot tài khoản khác không đổi; token access cũ gọi /me bị từ chối 401. Không tái tạo hay xóa lại account Google thủ công đã dọn ở đợt trước.
- Đã xóa 3 file selftest/runner tạm local và runner staging VPS, kiểm tra còn 0 runner tạm; giữ regression tests chính thức, bốn role và ledger đã khử secrets. Không thêm selftest vào source. Thư cũ không tự đổi giao diện; link của fixture đã cleanup không còn sử dụng được.

Chưa kiểm tra: inbox/delivery/render phía người nhận và hết hạn TTL bằng chờ thời gian thực VPS. Bằng chứng và các kết quả trước đó vẫn được giữ, không suy diễn unit/mocked tests là Google Login mới; Google Login thủ công đã có bằng chứng ở đợt trước.
### Rà soát scope tuần 4–5 và xác nhận cleanup — 2026-10-10

Trạng thái hạng mục rà soát: **Hoàn thành**. Đã đọc lại workbook N8_Cineplex_PCCV.xlsx, sheet phân công rows 11–12 và source/tiến độ hiện tại. Không thay workbook. Tuần 4 backend bootstrap và UI nền tảng đã có bằng chứng; tuần 5 Auth/email/JWT-refresh/role/Google/profile/avatar có code và kết quả kiểm tra đã ghi phía trên. Tách phần code hoàn thành khỏi nghiệm thu chưa đủ bằng chứng; không suy diễn tất cả UI Android/API VPS/roles đã chạy đầy đủ end-to-end.

- Recheck DB chỉ đọc sau cleanup: 4 UUID test từ ledger đều không còn user/order/held seat; email test gốc anhnguyenfb2021@gmail.com không còn account. Schema vẫn 10 bảng/103 cột. Không tạo/xóa lại tài khoản trong lần rà soát này; các cleanup trước đã xóa refresh hash cùng user row, avatar thuộc fixture và đối chiếu dữ liệu người khác không đổi. Nếu lần sau đăng nhập Google lại tạo UUID mới thì phải ghi ownership mới và cleanup đúng UUID đó, không xóa rộng theo email/role.
- HTTPS VPS health trả 200/database up và Swagger 200 trong lượt rà soát. Không đổi source backend, không rebuild/deploy/migration/restore chỉ vì thêm tiến độ.
- Phần nghiệm thu tuần 5 chưa đủ bằng chứng: delivery/render trong inbox người nhận (đã kiểm tra Sent và thực thi link thật); toàn bộ ma trận thao tác Auth/profile/avatar trên Android qua VPS (đã có API/local, mail E2E VPS, tests Android và Google Login thủ công, chưa full matrix Android thực); STAFF/ADMIN login thật trên VPS với account được phép dùng (role guards đã unit test, không tự tạo/sửa tài khoản STAFF/ADMIN); Google OAuth cho APK release/keystore máy khác chưa cấu hình, chỉ debug fingerprint hiện tại đã kiểm tra. Release signing thuộc giai đoạn phát hành, không coi là feature backend bắt buộc còn thiếu của tuần 5.
- Token hết hạn đã unit test; chưa chờ hết TTL thời gian thực VPS là giới hạn kiểm chứng, không có nghĩa logic expiry chưa code. Không tự reset/restore hay tạo account quyền cao để hoàn thiện test.
- Workbook F11/F12 vẫn chưa đánh dấu hoàn thành (0); các nhãn Redis/BullMQ ở E11 và crawler_runs ở G12 là nội dung cũ đã bị Plan loại bỏ, không phải việc cần bổ sung. Bảng màu app và tài liệu mới nhất là trạng thái hiện hành; lịch sử cũ giữ nguyên và được thay bằng ghi nhận mới có bằng chứng.
- Crawler dành cho Đào Mạnh Nhân theo yêu cầu người dùng; không thay code/phạm vi crawler trong lượt này. Home/catalog/phim/lịch chiếu, ghế/order/combo/payment/ticket/refund, nghiệp vụ STAFF/ADMIN/FCM thuộc tuần sau; UI/mock chưa nối backend ở các phần đó không phải thiếu sót của tuần 4–5. Tích hợp auth với crawler thuộc công việc chung sau khi crawler của thành viên phụ trách sẵn sàng.

Bước tiếp theo trong phạm vi nghiệm thu: bổ sung bằng chứng inbox và full Android/VPS/role matrix bằng tài khoản được người dùng cho phép; không tự mở rộng scope. Hai tài liệu giữ nhất quán, dùng lại bốn role và xóa selftest tạm sau mỗi lượt.
### Chuẩn bị xuất bản branch Auth tuần 5 — 2026-10-10

Trạng thái xuất bản: **Đang làm**, theo yêu cầu mới của người dùng cho phép commit/push, thay yêu cầu trước đó chưa xuất bản. Branch mới `feat/nguyen-week5-auth-profile` tách từ main `65c12f9e15046d615eac3a6aacf2e4a96fd8cf3b`; không thay main/không rewrite lịch sử. Author và Committer của mọi commit mới duy nhất Trần Thanh Nguyên <thnguyen290106@gmail.com>, không co-author/bot attribution; identity cấu hình riêng repository, không đổi global của máy.

Gói thay đổi: Auth/profile/avatar backend, tích hợp Android API/Google/Keystore, mẫu email/trang verify-reset cùng theme app, regression tests và hai tài liệu đồng bộ. CI backend đổi bước `test:crawler` thành `npm test` để chạy cả Auth và crawler regression, không đổi code/phạm vi crawler của Đào Mạnh Nhân; credential/DB giả của tests là fixture không phải secrets. Không đưa private env, SSH key, APK/build/cache, live data, selftest tạm, log thô hay bốn artifact ngoài source vào commit. VPS vẫn chạy source snapshot đã kiểm chứng với content hash tương ứng; không đổi runtime/DB chỉ để xuất bản cùng source.

Kết quả trước xuất bản giữ nguyên: backend 31 tests/build/format, Android 22 unit +12 device/lint/build đã đạt; mail E2E VPS và Google Login thủ công đã đạt, bốn UUID test không còn và selftest tạm đã xóa. Nghiệm thu inbox người nhận, full Android/VPS/role matrix và release OAuth vẫn chưa đủ bằng chứng; không biến trạng thái đó thành Hoàn thành vì xuất bản branch. SHA và kết quả remote chỉ bổ sung sau khi quan sát thật.
### Kết quả xuất bản branch Auth tuần 5 — 2026-10-10

Trạng thái xuất bản source: **Hoàn thành**. Thay trạng thái Đang làm của mục chuẩn bị xuất bản phía trên. Đã tạo và push branch `feat/nguyen-week5-auth-profile` lên origin tại repository Tonyisded/N8_Cineplex-Project; commit tính năng `7127946459aba6493f6f9fb53c4a355aa9bc97e6`, subject `feat(auth): integrate week 5 auth and profile with Android`. Git push exit 0 và ls-remote xác nhận branch đúng SHA. Author và Committer đều Trần Thanh Nguyên <thnguyen290106@gmail.com>; không co-author/bot attribution. Main vẫn `65c12f9e15046d615eac3a6aacf2e4a96fd8cf3b`, không push/merge main hoặc rewrite lịch sử; contributor các commit lịch sử khác không bị thay đổi.

Đã kiểm tra 48 file staged chỉ chứa source/config build/tests chính thức/CI và hai MD; không có SMTP/JWT/DB secrets thật, env/key/APK/build/live data/selftest tạm/log thô/bốn role ngoài source. Trước xuất bản đã chạy lại typecheck +31 tests +format và diff-check, đều đạt exit 0. CI chỉ thay bước regression để chạy `npm test`; chưa tạo PR/chưa quan sát GitHub Actions chạy trên remote, không ghi CI remote đã đạt. Nhóm trưởng có thể review/merge branch qua PR; agent không tự merge.

VPS vẫn chạy backend snapshot `week5-3ad1c2f442b0d69e` đã kiểm chứng; các file backend hiện tại khớp per-file manifest. Việc xuất bản và ghi kết quả SHA không thay backend runtime/source/API/schema nên không recreate API/migrate/restore DB. Những phần nghiệm thu còn thiếu đã ghi ở mục rà soát tuần 4–5 vẫn giữ nguyên. Ghi nhận SHA thực tế này đi cùng một commit tài liệu kế tiếp trên chính branch để cả hai MD nhất quán; không suy diễn SHA của commit tài liệu khi chưa được tạo.

### Tài khoản demo cố định trên database chung — 2026-10-10

Trạng thái: **Hoàn thành tạo dữ liệu và kiểm tra API VPS** theo yêu cầu mới của người dùng. Đây là ngoại lệ được cho phép rõ ràng so với các ghi nhận trước đó chưa tạo STAFF/ADMIN; không mở endpoint cho client tự gán role.

- Đã thêm đúng hai tài khoản: `user@cineplex.test` → CUSTOMER (UUID `25e6a7bd-9ff1-4516-8ad0-fe5d780f11f5`), `admin@cineplex.test` → ADMIN (UUID `d60d33dc-7559-46bc-99ff-7851cc3b568c`). ADMIN đăng nhập qua portal STAFF/mục Nhân viên; không đồng nghĩa có thêm tài khoản STAFF riêng hoặc một role kết hợp STAFF/ADMIN.
- Cả hai active, mật khẩu được hash Argon2id, credit bằng 0, xác minh email sẵn bằng thao tác quản trị vì địa chỉ .test không nhận thư. Mật khẩu do người dùng chỉ định chỉ dùng trong RAM, không ghi vào source, tài liệu, log hay bằng chứng. Không sửa yêu cầu tối thiểu 8 ký tự của register/reset/change-password; login hiện có chấp nhận mật khẩu được cấp trực tiếp.
- Đây là dữ liệu demo cố định được người dùng yêu cầu giữ lại trong DB chính, **không phải CUSTOMER fixture tự động cleanup**. Không tự xóa/đặt lại/nâng quyền hai tài khoản này trong những selftest sau. Tài khoản test tạm mới vẫn phải có ownership/UUID riêng và cleanup đúng phạm vi; gặp email có sẵn thì dừng, không ghi đè password/role.
- Thực thi trực tiếp trong container API trên VPS qua SSH và Prisma hiện có: kiểm tra email chưa tồn tại, thử transaction insert hai row rồi rollback (count 0 → 2 → 0), sau đó tạo lại atomically (count cuối 2). Login cùng input trước tạo và sau dry-run rollback đều 401; sau tạo chính thức CUSTOMER/ADMIN đúng portal đều 200, GET /me 200 đúng UUID/role, sai portal 403. Logout 200, refresh kiểm thử sau logout 401; hash refresh cuối NULL. Đối chiếu snapshot tài khoản khác không đổi và số row của 9 bảng nghiệp vụ còn lại không đổi.
- Không đổi source Android/backend, schema, migration, seed, runtime hoặc secrets; không cần rebuild/deploy API. Chỉ cập nhật cả hai MD để đồng bộ quyết định/tiến độ. Không tạo file selftest tạm: runner qua stdin; ledger đã khử secrets nằm ngoài repo tại `D:\NT118\verification\week5-auth-profile-20261010\demo-accounts-result.json` cùng bốn role hiện có. Rollback nguồn không restore DB hoặc xóa hai tài khoản đã được yêu cầu giữ lại.
- Chưa thử hai tài khoản mới bằng thao tác UI Android thực tế; đã đọc validation login Android và xác nhận không chặn mật khẩu được cấp. Kiểm tra ADMIN API/VPS mới này bổ sung bằng chứng còn thiếu trước đây, không suy diễn đã kiểm tra STAFF riêng, full Android matrix hay nghiệp vụ quản trị của tuần sau.

Cảnh báo: mật khẩu demo yếu, đặc biệt với ADMIN trên API công khai. Chỉ dùng demo; cần đổi mật khẩu mạnh trước khi dùng thật. Địa chỉ .test không dùng được luồng quên mật khẩu qua email; nếu đổi sang email thật phải theo luồng xác minh hiện có. Bước tiếp theo: người dùng thử hai cổng đăng nhập trên app; không tự mở rộng scope hoặc publish thêm thay đổi tài liệu.


## Crawler/catalog tuần 5 — quyết định và tiến độ 2026-10-10

Trạng thái: **Đang làm**. Phụ trách: Đào Mạnh Nhân. Phạm vi đã chốt: CGV Landmark 81, phim/lịch chiếu của business date và catalog Android; thay phạm vi catalog để tuần sau trong ghi nhận Auth cũ, không thay Auth.

- Business date ngày lịch Asia/Ho_Chi_Minh chốt đầu run; DB UTC, không mốc cắt 06:00. Cron `0 0 */6 * * *`, timezone Asia/Ho_Chi_Minh, waitForCompletion: 00:00/06:00/12:00/18:00 Việt Nam. Local CRAWLER_ENABLED=false, VPS chỉ bật sau kiểm tra. Structured logs job/runId/businessDate/start/finish/counts/skipped/conflicts/status, không bảng crawler_runs.
- Đúng 10 bảng, không JSONB. Movies 14 cột: thêm original_title/release_date DATE/director/cast nullable; tổng 107 cột nghiệp vụ thay 103 ở spec hiện hành (kết quả 103 lịch sử giữ nguyên). UNIQUE movies.source_url, kiểm tra duplicate trước migration; trùng thì dừng, không tự merge/xóa. Original title chỉ khi nguồn ghi rõ, không suy slug; năm là năm khởi chiếu CGV, không phải năm sản xuất.
- Playwright HTTPS/host allowlist, từ chối redirect. Tuổi P/K/T13/T16/T18 và duration dương bắt buộc. Format từ đúng film-screen của suất (std/goldclass/screenx/imax), không từ icon quảng cáo công nghệ trên trang chi tiết. Thiếu/format lạ skip+log, không Staff review. source_key `cgv:071:<dy>:<seq>`; ngày/giờ từ cùng snapshot/link.
- Giá demo N8, không phải CGV: STANDARD_2D100000, STANDARD_3D120000, IMAX_2D150000, IMAX_3D180000, SCREENX160000, GOLD_CLASS250000 VND.
- Room assignment tương thích/rảnh [start,end), advisory transaction lock chung local/VPS, không network trong transaction. Ưu tiên 7 phòng seed, hết tạo logical extra capacities STANDARD160/IMAX496/SCREENX180/GOLD32. Template số cột16/31/12/8, ghế đúng capacity, hàng cuối có thể thiếu; GOLD seat GOLD, còn lại NORMAL. VIP/couple zoning ngoài phạm vi.
- Import lặp không trùng, không reset HELD/SOLD/BLOCKED hay hồi sinh CANCELED. Structural change slot cũ ghi conflict, giữ phòng/giờ/movie/format/inventory. Không xóa slot nguồn đã gỡ.
- REST public dùng rate limit hiện có: GET /api/v1/movies?date=YYYY-MM-DD, /movies/:id và /movies/:id/showtimes?date=YYYY-MM-DD. List/showtimes {businessDate,items}, detail metadata; date/UUID sai400, unknown404, empty200, camelCase/UTC ISO/releaseDate YYYY-MM-DD/money string. Không HTTP run-crawler endpoint.
- Android API thật/loading/error/retry/empty/search không dấu/Coil poster/null metadata ẩn/giờ Việt Nam/showtimes/giá demo, không mock fallback production. Sắp chiếu thông báo chưa tích hợp. Catalog job tách Auth, hủy logout; refresh resume/đổi ngày. Đặt vé chưa triển khai.
- Nghiệm thu tất cả hôm nay bằng cùng snapshot CGV công bố so DB/API/Android, không hồi phục suất đã gỡ trước snapshot. Migration additive sau backup mới thành công; nhận dạng source snapshot hash/manifest theo quy trình hiện có; giữ secrets/Auth users/avatar volume, không reset/restore DB.

Baseline backend typecheck và 31 tests đạt. Đã code parser/import/API/Android; snapshot local 2026-10-10T08:33:58.891Z có9 phim/24 suất, đã parse đủ không skip; Android assembleDebug/unit/lint exit0. Còn tests mới, migration/triển khai/nghiệm thu VPS và kiểm tra Android thực tế, rollback bản sao. Chưa xác nhận hoàn thành. Đã phát hiện lỗi truyền Unicode PowerShell trong đoạn mới và sửa bằng UTF-8, không thay nội dung lịch sử.


### Kiểm chứng crawler và xử lý chặn backup — 2026-10-10

Trạng thái: **Đang làm**. Backend42 tests đạt, Android26 unit và12 instrumentation trên Pixel10 Pro XL emulator đạt; lint0 lỗi/41 cảnh báo cũ. Ngày11/10 live CGV12 phim/43 suất parse đủ sau hỗ trợ nhãn SCREENX-2D theo capability thực tế. Artifact rollback đã chạy trên bản sao: baseline31 → modified42 → restored31 tests đều exit0, hash restored khớp baseline; đã sửa lựa chọn Python để bỏ alias Microsoft Store không chạy được.

VPS image Chromium/Argon2/sharp đạt, nhưng backup trước migration thất bại với Invalid deployment SHA vì script cũ chỉ nhận40hex còn Auth dùng source snapshot week5-<16hex>. Migration đã dừng, DB chưa đổi. Sửa tối thiểu metadata backup ngoài source: lấy source_identity từ image API thực tế (hỗ trợ identity cũ và week5/crawler snapshot), không suy từ APP_SHA cũ; giữ dump/hash/list/lock/retention72 giờ chỉ dọn sau backup mới thành công, không restore. Còn backup mới/migration/VPS crawl/API/UI live và lịch tự động. Không coi image build là deploy thành công.

### Xác nhận localhost và kết quả tiếp nối crawler — 2026-10-10


Kiểm tra mạng tiếp nối ngày 10/10: DNS CGV trả `183.91.22.138`; host curl HTTPS tới CGV exit 28, chưa mở được TCP/TLS, trong khi HTTPS tới playwright.dev trả 200. Capture giới hạn 12 giây trên ens5 chỉ lọc CGV TCP443 thấy 7 SYN gửi đi, không thấy phản hồi trong lượt đó; không chứng minh CGV chặn AWS hoặc xác định vị trí gói bị mất. Port tạm host hiện 32768–60999. Chưa sửa Security Group/NACL/firewall, chưa có quyền đọc cấu hình AWS qua console; đã yêu cầu người dùng cung cấp ảnh Outbound rules của SG và hai chiều NACL gắn subnet. Tiếp theo đối chiếu egress TCP443 và return ports/rule precedence; nếu AWS cho phép đầy đủ thì nhờ quản trị kiểm tra Flow Logs/đường mạng với AWS và phía CGV. Không tăng timeout, đổi IP hoặc thêm proxy để coi là đã xử lý. Trạng thái crawler tự động vẫn Bị chặn, source HOST giữ localhost.


Xác nhận theo yêu cầu mới: source backend vẫn mặc định `HOST=127.0.0.1`, `PORT=5000`; không hardcode địa chỉ VPS vào source. Địa chỉ VPS dùng để kiểm thử endpoint; cấu hình bind container `HOST=0.0.0.0` được truyền ngoài source qua môi trường, không thay mặc định local. Kiểm tra trực tiếp `validateEnvironment` trên BASELINE, MODIFIED và bản ROLLBACK với cùng fixture không truyền HOST/PORT đều in `HOST=127.0.0.1 PORT=5000 CRAWLER_ENABLED=false`, exit 0. Không sửa logic HOST/PORT, không cần triển khai lại cho xác nhận này.

Tiến độ tiếp nối thay trạng thái tạm thời trước đó: backup mới đã đạt, migration metadata đã áp dụng; API VPS chạy snapshot `crawler-514d0caff107cbdc`. API local và VPS đối chiếu snapshot ngày 10/10 (08:35:12.199Z) đạt 9 phim/24 suất, đủ metadata theo nguồn; kiểm thử catalog Android qua API VPS đạt. Toàn bộ crawler tự động vẫn **Bị chặn**: lệnh crawler thật trên VPS timeout tới CGV, trong khi Windows lấy được dữ liệu; cron VPS vẫn tắt. Người dùng xác nhận VPS là EC2 Singapore, chưa đủ bằng chứng kết luận nguyên nhân timeout. Bước tiếp theo: xử lý kết nối outbound tới CGV, chạy lại runner thật rồi mới bật và xác minh lịch 00/06/12/18 giờ Việt Nam. Không coi dữ liệu snapshot đã nhập là bằng chứng VPS crawl tự động thành công. Bằng chứng và bốn role tiếp tục lưu ngoài repo tại `D:\NT118\verification\week5-crawler-20261010`.


### Proxy riêng cho crawler — 2026-10-10

Trạng thái: **Đang làm**, theo yêu cầu người dùng dùng proxy đã cấp; quyết định này thay hướng kiểm tra trực tiếp EC2 → CGV làm bước tiếp theo. Chỉ crawler browser context dùng proxy HTTP(S) có xác thực; không đổi HOST=127.0.0.1/PORT=5000 trong source, không áp proxy cho Auth/API/DB hoặc máy chủ toàn cục. Cấu hình qua CRAWLER_PROXY_SERVER, CRAWLER_PROXY_USERNAME, CRAWLER_PROXY_PASSWORD ngoài source; cả ba phải đủ hoặc cùng không có. Reject SOCKS/URL chứa credentials/path/query/fragment hoặc cấu hình thiếu bằng thông báo không chứa secrets. Khi đã cấu hình proxy, lỗi phải thất bại, không tự fallback direct. Giữ CGV HTTPS/TLS verification, allowlist và từ chối redirect; không thêm thư viện, bảng hoặc API token/đổi IP tự động. CLI smoke cũng nhận cùng cấu hình.

VPS kiểm tra endpoint thực: dạng https:// timeout TLS tới proxy, dạng http:// tạo CONNECT200 và CGV HTTP200, ssl_verify_result=0; dùng giao thức HTTP CONNECT đã quan sát, không suy từ nhãn thương mại HTTPS. Credentials lưu riêng với quyền hạn chế, VPS file root-owned mode600; không ghi vào tài liệu/source/bằng chứng. Baseline bổ sung trước proxy42 tests đạt; modified45 tests đạt, gồm validation/proxy browser context/TLS/fail-closed. Còn Playwright live local/VPS, triển khai source đúng hash, kiểm tra health/catalog/Auth không đổi và bật/xác minh cron sau live run thành công. Không migrate/reset/restore DB; chưa báo hoàn thành.


### Kết quả tích hợp proxy crawler — 2026-10-10

Trạng thái riêng hạng mục proxy: **Hoàn thành local và VPS**; thay trạng thái Đang làm/Bị chặn timeout trực tiếp trước đó. Không đồng nghĩa đã quan sát tick cron tương lai. Source Config/crawler browser context và CLI smoke đã nhận proxy; private env local vẫn HOST127.0.0.1/PORT5000 và crawler local tắt, VPS env proxy root-owned mode600. Endpoint thực dùng HTTP CONNECT tới CGV HTTPS; TLS phía CGV vẫn kiểm tra, nhưng kết nối tới endpoint proxy HTTP không được TLS bảo vệ. Nếu cần TLS cả tới proxy, phải có endpoint TLS hợp lệ của nhà cung cấp, không bỏ kiểm tra chứng chỉ hoặc suy nhãn HTTPS là giao thức endpoint. Mật khẩu đã được người dùng gửi trong chat nên cần đổi qua kênh riêng rồi cập nhật private env; không ghi credential vào source/MD/ledger.

- Local: build/typecheck/format exit0, 45 tests đạt (trước proxy42, baseline gốc31). Playwright thật qua proxy ngày10/10 snapshot10:09:56.207Z:7 phim/19 suất, skipped=[]; riêng VPS curl HTTP CONNECT200/CGV200/ssl_verify_result0. Test cấu hình đủ/thiếu/endpoint không an toàn, TLS mặc định và fail-closed đạt.
- VPS: source snapshot crawler-fa88ffd1a8f79543, sourceHash fa88ffd1a8f79543883426bd7120e5ca586f084e887a122f971104815163afd8; 37 file backend khớp manifest. API image healthy, health DB up và Swagger200. Runner thật lúc10:15:00.535Z→10:15:37.657Z, runId89cd3a56-0ad1-4c69-a163-1e443d8615a4, businessDate2026-10-10, SUCCESS:6 phim/18 suất, không skip/conflict, unchanged18/created0/seatsCreated0. Không dùng snapshot Windows để thay crawl VPS trong lượt này.
- Local/VPS API cùng đối chiếu toàn bộ metadata6 phim và18 suất của chính snapshot VPS10:15:06.309Z đạt. API/DB vẫn giữ9 phim/24 suất/4137 ghế từ snapshot trước theo rule không xóa nguồn đã gỡ; số nguồn giảm theo thời điểm không phải parse thiếu. AUTH_USERS_UNCHANGED; đúng10 bảng/107 cột, mismatch capacity0/overlap room0. Không migrate/reset/restore DB, không đổi Android, secrets/Auth/avatar/volume giữ nguyên.
- Sau live run SUCCESS mới bật CRAWLER_ENABLED=true trên VPS và recreate đúng một API. Runtime xác nhận proxyConfigured=true, cron bật và spec0 0 */6 * * *, Asia/Ho_Chi_Minh, waitForCompletion=true; lịch00/06/12/18 giờ Việt Nam, next tick2026-10-10T18:00:00+07:00. **Chưa quan sát job tự chạy tại tick18:00**, bước tiếp theo chỉ đọc log để xác nhận runId/businessDate/status/counts của tick đó, không chạy thêm API hoặc scheduler song song.
- Selftest/import runner tạm đã dọn khỏi container; archive upload tạm đã xóa sau lưu source release/manifest. Bằng chứng và bốn role tiếp tục ở D:\NT118\verification\week5-crawler-20261010; rollback source không tự rollback cấu hình triển khai hoặc restore DB. Cấu hình trước proxy còn ở /etc/n8-cineplex/compose.before-crawler-proxy.yaml để quản trị chọn khi cần, không tự áp dụng rollback VPS.

Chốt kiểm chứng nguồn cùng fixture `python regression.py`: BASELINE31 → MODIFIED45 → ROLLBACK31 tests, tất cả exit0; ROLLBACK.sh chạy trên bản sao, mọi byte gốc phục hồi và hash aggregate1898f7b9942075ef1a5f6b01d1951eeb62fd15db86941f2693267d21ab3e2f51 khớp. Source hiện tại vẫn có GOAL proxy, không rollback workspace/VPS/DB. Bốn role sẽ được dựng lại từ source hiện tại, kiểm tra patch tái tạo và mở lại trước báo kết quả. Không chạy lại Android vì lượt proxy không đổi source Android; giữ bằng chứng Android trước đó.


### Dọn thông tin kỹ thuật khỏi Trang chủ khách — 2026-10-10

Trạng thái: **Đang làm**, theo ảnh tham chiếu D:\NT118\001.jpg. HomeScreen bỏ đúng3 thành phần: dòng CGV Landmark81/ngày/giờ Việt Nam, nút Làm mới và dòng số phim/Đang chiếu–Sắp chiếu. Giữ search, hai tab, danh sách phim và loading/error/empty, nút Thử lại khi lỗi; không bỏ refresh tự động resume/đổi ngày hoặc sửa ViewModel/backend/crawler/proxy. Không sửa file ảnh tham chiếu.

Đã ghi hash HomeScreen trước sửa a2db01c83a747542c5c276f9919e688ec35d12d59488453297063721d4e662c3 và lưu PRE_UI_BASELINE.zip ngoài source. Kiểm tra cùng ui-header-check.py: BASELINE cả3 thành phần có, MODIFIED cả3 không còn, search/tabs/retry vẫn có, exit0. Bổ sung assertions vào UI fixture/live catalog để tránh tái xuất hiện; đang chạy Android build/unit/lint/instrumentation/ảnh render. Không đổi backend nên không rebuild/deploy VPS. Bốn role giữ nguyên đường dẫn; ROLLBACK.sh thêm --ui để phục hồi snapshot trước lượt UI trên bản sao, mặc định vẫn phục hồi baseline gốc của crawler. Còn kết quả device/rollback/patch và đối chiếu tài liệu trước kết thúc.


#### Kết quả dọn Trang chủ khách — 2026-10-10

Trạng thái: **Hoàn thành UI local/máy giả lập**, thay trạng thái Đang làm của mục này. Trong HomeScreen.kt đã xóa đúng hai dòng tạo ba thành phần: nhánh if(!vm.soon) chứa nhãn CGV Landmark 81/ngày/giờ Việt Nam và nút Làm mới; dòng Muted đếm phim của cả hai tab. Giữ nguyên tìm kiếm, tab, poster, navigation, loading/error/empty và Thử lại. Không đổi ViewModel/MainActivity nên cơ chế refresh tự động vẫn giữ nguyên; không thay file ảnh tham chiếu.

- Build debug và AndroidTest, 26 unit tests, lintDebug exit0; lint có 0 error/41 warning, không tuyên bố đã sửa các warning ngoài phạm vi. Cùng selector CineplexUiTest + CatalogServerUiTest trên Pixel_10_Pro_XL Android17: BASELINE10 → MODIFIED10 → ROLLBACK10 tests đạt, exit0. Fixture Auth không ghi tài khoản DB chung; catalog đọc API HTTPS thật. Assertions mới xác nhận ba thành phần không tồn tại trong fixture và live catalog.
- Đã mở ảnh render trước/sau/khôi phục: BASELINE hiện đủ3 → MODIFIED không còn3, poster nằm ngay dưới tab → ROLLBACK hiện lại3. Cùng ui-header-check.py cho kết quả tương ứng true/false/true, search/tabs/retry luôn true, exit0. ROLLBACK.sh --ui thực thi trên bản sao, phục hồi đủ157 file từ PRE_UI_BASELINE.zip; aggregate hash da79fbd57b17100befd1262a1411cdc172b55522d5a0b653ba29952c8449d569 khớp. HomeScreen hash trước/rollback a2db01c83a747542c5c276f9919e688ec35d12d59488453297063721d4e662c3; sau sửa 82105196e072aa681cfc1b7fff10a3144ac532f7d5abbf41736df3fc274ddaa7. APK rollback là bản baseline đã build trước sửa, không phải APK mới sửa. Workspace vẫn giữ UI đã sửa; sẽ cài lại APK modified sau kiểm tra rollback.
- 37 file backend khớp manifest VPS đã xác minh trước đó; lượt này không đổi backend/DB/proxy nên không build/deploy VPS. JPEG tham chiếu hash c1467c54c3291b98cc3641fb8586af3c279b1d3912dabb9a15997e4664ba7b30 không đổi. Chưa thử máy Android vật lý. Bước theo dõi tick cron18:00 của crawler vẫn riêng biệt, chưa suy diễn đã chạy từ các UI test.
- Bốn role giữ nguyên: D:\NT118\verification\week5-crawler-20261010\MODIFIED_FILE.zip, DIFF_FILE.patch, VERIFICATION.txt, ROLLBACK.sh. Archive/patch sẽ đồng bộ source mới, kiểm tra tái tạo trên bản sao và mở lại đủ4 role trước báo kết quả; giữ baseline và bằng chứng crawler31/45/31 trước đó. Bản build UI mới: D:\NT118\verification\week5-crawler-20261010\UI_MODIFIED.apk, SHA256 5fdde4f7ba3df90bfbf53b89f902666adec81bb36e18c4f3d311d441b81330ed.

Chốt UI: patch đã tái tạo đủ157 file trên bản sao, baseline gốc không đổi; APK modified đã được cài lại thành công sau rollback. Đã mở ảnh ROLLBACK và xác nhận ba thành phần xuất hiện lại. Không đưa private password vào archive/patch/ledger. Nhiệm vụ UI đã có kết quả thực thi; không còn bước UI bị chặn.


### Kiểm tra tự khởi động VPS và proxy — 2026-10-10

Trạng thái: **Hoàn thành kiểm tra cấu hình/đường kết nối; Chưa làm reboot thực tế sau tích hợp proxy**. Thực thi restart-readiness.sh qua SSH, exit0, không restart/reboot service, không ghi DB. Docker systemctl enabled/active; API/PostgreSQL/reverse-proxy running và restart=unless-stopped (API/DB healthy). Private proxy env root-owned mode600 tồn tại bền vững; Compose nạp lại đủ ba biến proxy và CRAWLER_ENABLED=true, giá trị khớp runtime, không in secrets. API bắt đầu lúc2026-10-10T10:17:07.523Z sau lần recreate đã ghi trước đó; không coi việc đó là full VPS reboot.

Health200 database up, Swagger200; probe Playwright chỉ đọc CGV qua proxy trong container API tại2026-10-10T10:57:32.442Z trảHTTP200, đúng CGV Landmark81. Đây là bằng chứng hiện tại cộng với cấu hình persistence, không bảo đảm tuyệt đối nhà cung cấp proxy/CGV luôn sẵn sàng. Proxy là dịch vụ ngoài VPS; cần nạp cấu hình khi browser context được tạo, không có daemon proxy riêng cần chạy trong VPS. Sau reboot bình thường cron trở lại khi Nest startup và chờ mốc00/06/12/18 giờ Việt Nam; không có crawl tức thì hoặc chạy bù tick lỡ. Nếu container bị stop/compose down chủ động, phải bật lại; unless-stopped không thay thế thao tác đó. Docker giữ env của container và Compose tham chiếu private env khi recreate; không ghi secrets vào source.

Không tự reboot vì người dùng đang hỏi mức bảo đảm, chưa yêu cầu gây gián đoạn API chung. Bước còn lại để kiểm chứng end-to-end: chọn lúc phù hợp, reboot thật rồi xác nhận boot mới, containers/health/Swagger, proxy probe và tick cron tiếp theo. Tick18:00 vẫn chưa được quan sát trong lần kiểm tra này. Không đổi source backend/Android/DB; giữ kết quả UI BASELINE3 hiện → MODIFIED3 mất → ROLLBACK3 hiện, native10/10/10 và unit26 đã xác minh trước đó. Bốn role cùng đường dẫn D:\NT118\verification\week5-crawler-20261010 được mở rộng bằng tiến độ mới, không thay baseline/lịch sử.


### Reboot VPS theo yêu cầu và kiểm chứng phục hồi — 2026-10-10

Trạng thái: **Hoàn thành reboot và kiểm tra phục hồi VPS/proxy**; thay trạng thái Chưa làm reboot thực tế ở mục trước. Người dùng đã yêu cầu reboot; lệnh systemd-run gọi /usr/sbin/reboot thực thi thành công. Boot ID trước b3c1db98-2f12-4100-8b48-62279d5634f3 (boot2026-10-04T19:36:12Z), sau 0cba38ce-f2fc-442d-81e0-c1bed858b51c (boot2026-10-10T11:01:07Z). Không docker start/compose up/restart service bằng tay sau boot; các container tự chạy nhờ Docker enabled và restart=unless-stopped, startedAt khoảng11:01:19Z; API/PostgreSQL healthy, reverse-proxy running. Có thời gian khởi động: attempt1 Docker activating exit3; attempt2 health502/exit22; attempt3 tự phục hồi exit0. Giữ lỗi/exit trong bằng chứng, không che bằng thao tác bật tay.

Sau boot lúc11:01:38.381Z: health200 database up, Swagger200, Playwright probe qua proxy CGV200 đúng Landmark81; Compose/private proxy env đủ ba biến và khớp runtime, ownerroot/mode600, CRAWLER_ENABLED=true vẫn còn. Không in credential. Source release crawler-fa88ffd1a8f79543 khớp37 file, đúng10 bảng/107 cột,9 phim/24 suất/4137 ghế và7 phòng gốc còn nguyên. Timer backup và snap.certbot.renew đều enabled/active; health kiểm tra lần hai vẫn đạt. Không migrate/reset/restore DB hoặc rebuild/redeploy source trong lượt reboot này. Marker NO_REBOOT_NO_SERVICE_RESTART_NO_DB_WRITE trong đoạn readiness tái sử dụng chỉ mô tả thao tác của check sau boot; không phủ nhận reboot theo yêu cầu đã thực thi trước đó.

Đồng thời đã chạy đúng runner verify-first-cron.sh: cron18:00 tự chạy SUCCESS runId aff4be25-30d9-44ed-9b4d-aa6b3221b66a, businessDate2026-10-10, startedAt11:00:00.099Z/finishedAt11:00:37.970Z; snapshot11:00:07.503Z có6 phim/16 suất, skipped/conflicts0, unchanged16/created0/seatsCreated0. Đây là tick tự động **trước boot mới**, không phải tick sau reboot hay CLI crawl thay thế. Bước pending kiểm tra tick18:00 trước đó đã đạt, giữ kết quả6/18 của lần live trước như lịch sử. Sau reboot cấu hình cron được nạp lại, lịch tiếp theo2026-10-11T00:00:00+07:00; chưa chờ/quan sát tick tương lai đó, không tuyên bố nó đã chạy. Không có crawl tức thì/chạy bù lúc boot.

Đã xác nhận tự phục hồi trong lần reboot này, không bảo đảm tuyệt đối proxy bên ngoài/CGV luôn hoạt động, proxy hết hạn hoặc sự cố mạng vẫn có thể làm crawl thất bại. Container bị stop/compose down chủ động phải bật lại. Bốn role giữ nguyên D:\NT118\verification\week5-crawler-20261010, mở rộng bằng bằng chứng reboot; giữ baseline và các kết quả đã hoàn thành. UI vẫn chỉ bỏ nhánh nhãn CGV/nút Làm mới và dòng đếm phim trong HomeScreen; bằng chứng BASELINE3 hiện → MODIFIED3 mất → ROLLBACK3 hiện/native10/10/10/unit26 không bị thay thế.


### Source local không hỗ trợ proxy crawler — 2026-10-10

Trạng thái: **Đang làm**, theo yêu cầu mới của người dùng chỉ cần proxy trên VPS. Quyết định này thay quy tắc cùng source/cấu hình theo môi trường riêng cho phần proxy crawler: source local xóa hoàn toàn crawlerProxy, validation CRAWLER_PROXY_*, injection ConfigService/Optional của CrawlerService và option proxy trong browser.newContext; smoke gọi CrawlerService không tham số. Xóa3 biến proxy khỏi private backend/.env local, giữ các dòng khác; source mặc định HOST127.0.0.1/PORT5000 vẫn giữ nguyên. Không xóa TRUST_PROXY của Auth/Nginx vì đó không phải proxy outbound CGV. Xóa3 test proxy đã không còn áp dụng, thêm assertion options context trực tiếp vào fixture Chromium có sẵn; không bỏ kiểm tra TLS/allowlist/redirect hoặc error cleanup.

VPS đang chạy bản riêng crawler-fa88ffd1a8f79543 có proxy, giữ nguyên source/image/private env/container/cron; không deploy source direct lên VPS vì sẽ mất khả năng dùng proxy chỉ bằng env. Lần cập nhật VPS về sau phải xử lý riêng phần hỗ trợ proxy ngoài source local và kiểm tra lại; chưa thiết kế/thực thi cơ chế thay thế trong lượt này, không tự tạo overlay/sidecar/network-proxy. Đây là sai khác có chủ đích do yêu cầu mới, không tuyên bố local/VPS cùng37 hash nữa. Không đổi DB/Android/Auth/schedule.

Đã lưu PRE_DIRECT_BASELINE.zip bất biến ngoài source và hash TARGET trước sửa. Cùng direct-support-check.mjs trước sửa: export/forward proxy=true, TLS verification=true, probe fixture200/browserClosed=true;45 baseline tests đạt. Sau sửa export/forward=false, TLS/probe/browser cleanup vẫn đạt; build/typecheck exit0,42 tests đạt (bỏ3 test chức năng proxy bị xóa), format đạt sau sửa layout smoke.ts. Đang kiểm tra direct CGV smoke local và VPS hiện tại vẫn dùng proxy. Bốn role giữ nguyên; ROLLBACK.sh sẽ mở rộng --direct để phục hồi snapshot trước lượt này trên bản sao, giữ default baseline crawler và --ui; không tự phục hồi private env local/VPS hay DB. Các lỗi assert nhầm yêu cầu HOST/PORT phải có dòng explicit và lỗi quote node -e đã giữ trong ledger, sửa runner ngoài source; default localhost không bị đổi.


#### Chốt source local direct và ghi chú VPS có proxy — 2026-10-10

Trạng thái: **Hoàn thành yêu cầu source local và tài liệu; VPS giữ phiên bản riêng đã xác minh**. Theo nhắc lại mới nhất của người dùng, ghi rõ: **LOCAL không có mã hỗ trợ/config proxy crawler; VPS CÓ DÙNG PROXY cho crawler CGV**. Không ghi IP/port/account/password proxy vào hai file MD. VPS dùng private /etc/n8-cineplex/crawler-proxy.env root mode600 qua Compose, bản release crawler-fa88ffd1a8f79543 vẫn có hỗ trợ proxy và cron; không sửa hay redeploy bản direct local lên đó. Quyết định mới supersede yêu cầu cùng source đối với phần proxy crawler, không thay đổi yêu cầu deployment khác. Bốn file backend local khác bản VPS có chủ đích: config.ts, crawler.service.ts, smoke.ts, tests/crawler.test.mjs. Khi nâng cấp VPS từ source này phải xử lý riêng hỗ trợ proxy trước triển khai; chỉ thêm biến môi trường vào source direct không có tác dụng. Chưa tạo cơ chế nâng cấp/overlay, không tuyên bố deployment source direct lên VPS hoàn thành.

- Local: xóa export crawlerProxy/validation CRAWLER_PROXY_* trong config.ts; bỏ ConfigService/Optional constructor và browser.newContext.proxy khỏi CrawlerService; smoke không truyền config. Không còn crawlerProxy/CRAWLER_PROXY_* trong app source/test, private .env không còn3 biến proxy; source/default runtime127.0.0.1:5000, local cronfalse. Giữ TRUST_PROXY (Nginx/Auth), URL allowlist, TLS verification, từ chối redirect, browser cleanup, DB/state checks và cron giờ Việt Nam. Không dependency/schema/Android/Auth change.
- Build/typecheck/format exit0,42 tests đạt so với45 trước xóa (3 test của feature proxy bỏ theo feature, fixture Chromium được bổ sung assert exact options kết nối trực tiếp). Local crawler:smoke đọc CGV thật trực tiếp HTTP200 tại11:08:54.658Z, không dùng proxy và không ghi DB. VPS kiểm tra lại11:09:11.081Z: health200/DBup, Swagger200, proxy browser probeCGV200, container healthy giữ startedAt11:01:19Z, env proxy/cron vẫn nạp đúng; không restart/recreate/deploy hay ghi DB trong lượt này.
- Cùng direct-support-check.mjs/cùng fixture: BASELINE proxy export/forward=true → MODIFIED=false → ROLLBACK=true, TLS/probe200/browserClosed luôn đạt, exit0. Cùng python regression.py:45 →42 →45 tests, exit0. ROLLBACK.sh --direct đã thực thi trên bản sao,157 file match PRE_DIRECT_BASELINE.zip, aggregate7a54639fb53d6e4e350e3d138545b6162bf0e3193197bc05e27a5174cd5add7b; workspace vẫn giữ GOAL direct. Private env không nằm trong archive và rollback code không tự phục hồi env hay DB. Baseline/UI/reboot/cron18:00 đã xác minh vẫn giữ lịch sử; ảnh UI và26 unit/10 native test không chạy lại vì không đổi Android.
- Bốn role ở D:\NT118\verification\week5-crawler-20261010\MODIFIED_FILE.zip, DIFF_FILE.patch, VERIFICATION.txt, ROLLBACK.sh tiếp tục cùng vai trò; bổ sung PRE_DIRECT_BASELINE.zip và --direct, giữ default và --ui. Sẽ tái tạo/mở lại role từ source/tài liệu cuối, kiểm tra patch và cả hai MD trước kết thúc. Lần reboot vừa xác minh không phải kiểm chứng tick00:00 tương lai; không tuyên bố proxy ngoài VPS luôn hoạt động.


### Chính sách chỉ giữ image đang chạy và dọn đĩa VPS — 2026-10-10

Trạng thái: **Đang làm**, theo lựa chọn rõ ràng của người dùng: chỉ giữ image phiên bản mới đang chạy đã kiểm tra đạt, xóa hết image cũ không còn sử dụng sau mỗi lần triển khai thành công. Quy tắc mới thay chính sách giữ image cũ để rollback nhanh (nếu có); không thay chính sách backup72 giờ. Không tự nâng PostgreSQL/Nginx theo tag latest; bản mới nghĩa là release được nhóm chọn và đã xác minh, image API pin theo release identity. VPS vẫn dùng proxy private ngoài source; source local không có hỗ trợ proxy nên không dùng nguyên source local để thay bản VPS.

Bước bắt buộc cho mỗi lần triển khai về sau: khóa tuần tự deployment/cleanup bằng flock /var/tmp/n8-cineplex-deploy-cleanup.lock; build bản mới và recreate đúng service → chờ healthy → kiểm tra health database up/Swagger/catalog và crawler qua proxy → chỉ khi các kiểm tra đạt mới dọn cache builder default và docker image prune --all --force. Nếu bản mới lỗi thì chưa xóa image cũ. Không thêm cleanup cron hoặc nâng phiên bản dependency tự động. Không chạy cleanup đồng thời build/deploy; không prune volume/network/container đang chạy và không dùng docker compose down -v. Mất image/cache cũ đồng nghĩa lần build sau tải/build lại và không rollback image cũ ngay được; giữ source VPS/manifest/cấu hình cần tái dựng, không tự restore DB.

Đợt này preflight chỉ có3 container đang chạy; bảo vệ API n8-cineplex-api:crawler-fa88ffd1a8f79543, PostgreSQL17 và Nginx hiện được container tham chiếu. Trước dọn đo26 GB dùng/30 GB, Docker17 image/3 active, image reclaimable6.246GB/cache reclaimable2.314GB; dự kiến8–10GB, chưa coi là kết quả thực tế. PostgreSQLvolume67MB/backup204KB không phải nguyên nhân chính. Lệnh thực thi chỉ dọn build cache/image không được dùng, apt cache, và3 Snap revision nếu vẫn disabled: amazon-ssm-agent13009/core222411/snapd26865. Không xóa trực tiếp /var/lib/containerd, không đụng PGDATA/avatars/proxy-secrets/chứng chỉ/backup72h/source đang được Compose tham chiếu, không reboot/restart API. Đã ghi snapshot hash cấu hình/source, danh tính container/image/mount, users hash/count dữ liệu và backup hash trước thao tác. Còn kiểm tra sau dọn, đo GB thu hồi và đồng bộ/mở lại hai MD cùng4 role trước báo hoàn thành.


#### Kết quả dọn đĩa và quy tắc image đã chốt — 2026-10-10

Trạng thái: **Hoàn thành dọn đĩa VPS và ghi quy tắc triển khai vào hai MD**; thay trạng thái Đang làm của mục này. Bắt buộc chỉ giữ image của phiên bản đang chạy đã được kiểm tra thành công; mỗi lần triển khai bản mới xong phải xóa image cũ không còn dùng và cache build. Đây là bước bắt buộc của quy trình triển khai, **không phải cron dọn tự động mới**. Nếu bản mới chưa healthy hoặc health/Swagger/catalog/proxy chưa đạt, không dọn image cũ. Không tự nâng version PostgreSQL/Nginx hoặc thay tag release bằng floating latest. Không chạy cleanup song song build/deploy; dùng cùng flock /var/tmp/n8-cineplex-deploy-cleanup.lock cho cả phiên thao tác.

Trình tự sau khi bản mới đã đạt: sudo docker buildx prune --builder default --all --force → sudo docker image prune --all --force → đo df/docker system df và kiểm tra lại service. Chỉ dùng Docker quản lý layer; tuyệt đối không rm trực tiếp /var/lib/containerd hoặc /var/lib/docker, không prune volume/network, không down -v. Giữ image mà các container hiện tại tham chiếu, source/manifest VPS tương ứng, Compose/private env/proxy, PGDATA/avatars/chứng chỉ, backup72h. Nếu bản mới lỗi trước bước dọn thì giữ image cũ để xử lý; sau khi dọn muốn dùng lại image cũ phải build/tải lại, không giữ bản export image trên đĩa VPS chỉ để lách chính sách tối thiểu.

- Thực thi thật exit0: Docker cache51 →0; image17 →3, xóa14 image không còn dùng; apt-get clean; Snap disabled amazon-ssm-agent13009/core222411/snapd26865 đã xóa, mọi revision active và certbot giữ nguyên. API n8-cineplex-api:crawler-fa88ffd1a8f79543, PostgreSQL17 và Nginx đang chạy giữ nguyên. Không stop/restart/reboot container/VPS, không restore/migrate/reset DB.
- Đo statvfs trước: used26004226048 bytes, available4062773248; sau: used6451314688, available23615684608, total30083776512. Thực thu hồi19552911360 bytes = **19.553 GB (18.210 GiB)**, usage **87% →22%**, còn trống23.616 GB (khoảng22 GiB). Đây là kết quả đo filesystem, không cộng các số Docker cache/image do layer dùng chung; thay ước tính8–10GB trước đó bằng kết quả thực tế.
- Kiểm tra sau dọn exit0:3 image đều được dùng/3 container running, API/PostgreSQL healthy; id/image/mount/StartedAt container không đổi. Health200 database up, Swagger200; catalog2026-10-10 đúng envelope businessDate/items9; probe Playwright qua proxy CGV200 tại11:46:42.450Z. VPS **CÓ PROXY**, private proxy env còn nguyên owner/mode/hash; source local không có hỗ trợ proxy, không deploy source direct local lên VPS. Docker enabled, timer backup/chứng chỉ active.
- Đối chiếu snapshot:37 source file VPS/hash3 private cấu hình không đổi;15 file backup giữ nguyên hash; users hash không đổi, movies9/showtimes24/seats4137/auditoriums7 không đổi, PGDATAvolume67MB vẫn được mount đúng. Lỗi checker đầu tiên exit1 vì giả định key catalog data sai (HTTP200 thực là businessDate/items); đã giữ lỗi trong ledger và sửa đúng shape, không sửa server hay chạy lại thao tác xóa.
- Chỉ sửa tài liệu local; source/backend/Android và cấu hình vận hành không đổi nên không rebuild/deploy API. Ghi bằng chứng ngoài repo, giữ lịch sử test/source BASELINE45/MODIFIED42/ROLLBACK45. Sẽ mở rộng ROLLBACK.sh --disk để kiểm tra khôi phục hai MD trên bản sao từ PRE_DISK_BASELINE.zip, giữ default/--ui/--direct. Rollback role chỉ phục hồi source/tài liệu; **không khôi phục image/cache/Snap đã xóa hoặc restore DB**. Source hiện tại giữ quy tắc mới; chốt sau kiểm tra patch/rollback/mở lại đủ4 role và hai MD.

Chốt đợt dọn: patch đã tái tạo đủ157 file, rollback --disk trên bản sao phục hồi đủ157 file/hash hai MD trước đổi; cùng disk-doc-check.py BASELINE chưa có quy tắc mới → MODIFIED có → ROLLBACK chưa có, exit0, ghi chú VPS có proxy giữ nguyên cả3 trạng thái. Source chính vẫn giữ quy tắc mới. Snapshot bảo vệ trước/sau đã chuyển sang hồ sơ ngoài repo; hai file kiểm chứng tạm và thư mục riêng đã dọn khỏi /var/tmp trên VPS (không xóa lock triển khai). Không khôi phục image/cache cũ, không tạo cleanup cron; thao tác dọn sau các lần deploy thành công là bước bắt buộc trong quy trình đã ghi. Chỉ hai MD thay đổi so với PRE_DISK_BASELINE.zip, source/backend/Android vẫn nguyên.


### Lịch chiếu nhiều ngày và chọn ngày khách hàng — 2026-10-10

Trạng thái: **Hoàn thành triển khai và kiểm chứng local/VPS/Android**, kết quả cuối bên dưới. Trạng thái sau đây là mốc lúc bắt đầu.  Theo yêu cầu mới: hôm nay crawl riêng (trống vẫn tiếp tục ngày mai); từ ngày mai xử lý lần lượt ngày lịch Việt Nam đến ngày tương lai đầu tiên nguồn xác nhận trống rồi dừng. Không quét quá khứ, không bỏ qua khoảng trống để lấy lịch xa hơn; hết ngày công bố dừng với lý do riêng. Cron00:00/06:00/12:00/18:00 Việt Nam giữ nguyên; lượt sau kiểm tra lại. Timeout/HTTP/DOM lỗi hay toàn bộ suất invalid không đồng nghĩa ngày trống. Import theo ngày, giữ room/seat/state/history; không xóa lịch đã nhập vì nguồn gỡ. Không thêm bảng/migration/dependency.

Trang chủ: thanh ngày cuộn ngang + lịch chọn ngày tương lai, đặt cùng tìm kiếm/tabs trước banner; chọn ngày áp dụng danh sách phim và chi tiết suất, giữ khi resume/quay lại, ngày đã thành quá khứ chuyển về hôm nay. Empty200 hiển thị chưa có lịch cho ngày đã chọn, không lẫn lỗi mạng/search. API giữ date query, thêm GET /api/v1/movies/dates trả businessDate/dates từ ACTIVE showtimes trong DB. Sắp chiếu/booking không mở rộng. Local giữ localhost và không mã/config proxy; VPS giữ hỗ trợ proxy bằng bản vá môi trường ngoài repo từ phiên bản đã xác minh. Không deploy bản direct lên VPS khi chưa giữ proxy.

Mốc trước triển khai: đã lưu baseline157 file bất biến ngoài repo; cùng dates-check baseline chỉ collect hôm nay, chưa có dates endpoint/select date;42 backend tests đạt exit0. Đã sửa crawler/API/Android/tests;30 Android unit, lint0 error/41 warning, build đạt. Đang sửa kiểm chứng UI và crawler thật: phát hiện CGV AJAX có history.pushState trước response, và POST có thể trả trang Please enable JavaScript rồi frontend tự gắn No schedules available. Không nhận đó là ngày trống. GET công khai cùng trang với selecteddate=20261011 đã trả đúng12 phim/43 suất ngày11/10; dùng GET ngày đã validate thay POST, vẫn kiểm tra HTTPS/host/redirect/theater/ngày link và nguồn rỗng thật. Không thay transport, không thêm cơ chế né challenge. Unicode trong literal của helper PowerShell đã lỗi do output encoding ASCII; đã sửa UTF-8 phần mới, giữ nội dung lịch sử. Chưa xác minh phiên bản nhiều ngày trên VPS.

Các bước dự kiến trên đã thực hiện; xem kết quả cuối bên dưới.

Quyết định parser bổ sung từ nguồn thật: nhãn phụ đề tiếng Anh Eng Sub/Eng&Viet Sub/Viet Sub không thay format của film-screen. Nhãn tuổi duy nhất hợp lệ trên trang chi tiết phim là canonical; chỉ fallback badge lịch khi trang chi tiết không có tuổi hợp lệ, vẫn skip nếu thiếu/ambiguous. Điều này thay cách gom detail+listing thành một tập tuổi ở parser cũ: ALWAYS LALISA trang chi tiết T13/thời lượng98 phút nhưng badge lịch P, nay giữ T13 từ chi tiết thay vì bỏ phim hoặc hạ tuổi xuống P. Không suy diễn nhãn/thời lượng.

#### Kết quả cuối đã quan sát — 2026-10-10

Trạng thái: **Hoàn thành** chức năng nhiều ngày, API và giao diện khách; đây là trạng thái thay các mốc Đang làm/chưa xác minh VPS phía trên. Không thêm dependency/bảng/migration, không thay Auth hoặc reset dữ liệu. Thay đổi đúng các điểm: CrawlerService.collectUpcoming/collectPage (GET selecteddate); CrawlerImportService.runUpcoming (import/log từng businessDate + batchRunId); cron/CLI gọi batch; CatalogController.dates; CatalogRepository.dates, CineplexViewModel.catalogDate/selectCatalogDate và DateSelector trên Home/chi tiết.

- Local: backend55/55 tests, typecheck/build/Prettier đạt; command dates-check cùng input fixture10/11/12-trống/13-có-phim: BASELINE42 tests chỉ hôm nay, không endpoint dates/chọn ngày → MODIFIED55 tests đi10/11/12 và EMPTY_DAY, endpoint/chọn ngày có → ROLLBACK42 tests trở lại chỉ hôm nay/không endpoint/chọn ngày. Cả3 command exit0. ROLLBACK.sh --dates chạy trên bản sao, đủ157 file/hash trùng PRE_DATES_BASELINE.zip SHA25689879ea303bd63bb25ae49f6d0ba0e40927a0d031205c34a9f04cd2886f795dc; source chính giữ thay đổi mới. Rollback source không restore DB hoặc lấy lại image/cache đã dọn.
- Android:30 unit tests0 lỗi; lint0 error/41 warning có sẵn; APK và AndroidTest build đạt. Emulator12 UI tests đạt (9 luồng cũ,2 chọn ngày/calendar,1 catalog VPS thật): chọn11/10, danh sách/chi tiết đúng ngày, quay lại giữ chọn, ngày16/10 trống, calendar cho chọn ngày xa13/10 fixture. Đã xem screenshot native Home/date picker/future detail/empty; không hiện nút làm mới/count/nhãn kỹ thuật cũ. Emulator dùng locale English nên picker native theo locale hệ thống; nhãn ứng dụng tiếng Việt. APK SHA2560721863eea9be71b7dc75c4dcf2e00d94b690f35042d5982a38bb0157f1e6fb2. Lỗi selftest ban đầu do Unicode pipeline, nhiều root dialog và ngày Material3 có semantics tên đầy đủ đã sửa ở fixture/selector; không đổi yêu cầu hoặc bỏ kiểm tra.
- VPS: release crawler-dates-49365407a08485ce, source manifest37 file đã đối chiếu, image build Linux/native argon2/sharp/Chromium đạt. Bản môi trường58/58 tests đạt local trong staging, gồm3 test proxy; **không gọi đó là58 test chạy trong container Linux**. Chỉ4 file khác local do hỗ trợ proxy/private test; patch tại /etc/n8-cineplex/dates-vps-only-proxy.patch ngoài repo. Local không có CRAWLER_PROXY/crawlerProxy hoặc proxy credential; VPS CÓ PROXY, root600 private env nguyên hash. HOST local127.0.0.1:5000, VPS0.0.0.0:5000, HTTPS443, crontrue/restartunless-stopped giữ nguyên; chỉ recreate API, PostgreSQL/Nginx không restart.
- Lượt thật qua proxy runId40953f5f-5159-4dba-b7df-955cdd03180b tại13:17:57–13:20:19 UTC SUCCESS: ngày10/10 nguồn4 phim/9 suất còn lại;11/10 12/43;12/10 11/37;13/10 10/42;14/10 8/19;15/10 8/22;16/10 0/0, EMPTY_DAY dừng16/10. Mọi source movie/showtime parse đủ, skipped/conflicts0. Tạo163 suất tương lai và29806 ghế. Nguồn hôm nay giảm theo giờ; DB giữ24 suất/9 phim đã nhập từ trước, không xóa lịch cũ đã gỡ khỏi nguồn. Đối chiếu yêu cầu nguồn hiện tại là tập con DB hôm nay, không ép count DB bằng số suất còn lại.
- Lượt chạy lại runId2a09a73d-9130-4e8f-8164-ee5699e17a00 tại13:21:23–13:23:11 UTC SUCCESS; mọi ngày created0/seatsCreated0, cùng EMPTY_DAY16/10. HTTP checker local/VPS đối chiếu toàn bộ metadata và sourceKey/startAt/format/ACTIVE từ snapshot lượt mới nhất, đạt; CGV có thể đổi ngôn ngữ metadata giữa các lượt nên không so snapshot cũ với DB đã được lượt mới cập nhật. API dates10–15, future count khớp nguồn,16/10 items[], date invalid400, empty200, health/Swagger200.
- DB thật đọc-only:10 bảng nghiệp vụ/107 cột,187 suất/33943 ghế AVAILABLE, mọi suất đủ capacity, users hash trước/sau không đổi; ALWAYS LALISA T13/98 phút. Không migration/restore/đụng history/held/sold. State protections còn trong importer và regression fixture, không khẳng định có booking production vì inventory hiện đều AVAILABLE.
- Sau build/deploy/API/proxy đạt mới dọn cache/image theo khóa chung: giữ3 image đang chạy, cache0; thu hồi6646906880 bytes≈6.647GB sau build, còn trống23606841344 bytes≈23.607GB. Checker ban đầu so toàn bộ State gồm Health.Log động nên exit1 sau prune; sửa chỉ kiểm tra identity/StartedAt/config/hash, không chạy lại thao tác xóa. Health/Swagger/catalog và probe CGV200 sau dọn tại13:26:51 UTC đạt; proxy/env,PG/Nginx/backup giữ nguyên. Không prune volumes/network, không reboot/restore.
- Lịch tự động vẫn00:00/06:00/12:00/18:00 Việt Nam, mỗi lượt khám phá lại hôm nay và các ngày tương lai. Chưa quan sát tick00:00 ngày11/10 của phiên bản mới; không coi manual run là tick cron, không tuyên bố lịch tương lai bất biến. Rủi ro theo yêu cầu: dừng tại ngày trống đầu tiên nên nếu CGV có gap rồi lịch xa hơn sẽ chưa lấy lịch sau gap.

Bằng chứng đầy đủ nằm ngoài repo tại D:\NT118\verification\week5-crawler-20261010. Giữ cùng4 role MODIFIED_FILE.zip/DIFF_FILE.patch/VERIFICATION.txt/ROLLBACK.sh; bổ sung PRE_DATES_BASELINE.zip và --dates, giữ các mode cũ. Đã chốt: tái tạo và mở lại đủ4 role, JSON patch tái dựng đúng158 file bản sửa; rollback --dates trên bản sao phục hồi157 file pristine và cùng command trở về SINGLE_DAY, dates endpoint/chọn ngày không có, exit0. Hai MD đọc lại và đối chiếu nhất quán; source chính vẫn giữ chức năng mới, không thay dữ liệu production.


### Xác minh danh tính Git/GitHub theo yêu cầu — 2026-10-10

Trạng thái: **Hoàn thành kiểm tra tài khoản và cấu hình danh tính local**. GitHub API /user dùng credential hiện có xác nhận login Manh-Nhan299; credential username chọn Manh-Nhan299. Cấu hình riêng repo user.name=Đào Mạnh Nhân, user.email=24521227@gm.uit.edu.vn; git var xác nhận Author và Committer cùng danh tính này. Trước đổi, danh tính local là Trần Thanh Nguyên với email của Nguyên; không sửa cấu hình global hoặc xóa tài khoản được lưu của người khác. Không ghi credential/token vào tài liệu hoặc hồ sơ. Chưa tạo commit, push hay thay branch; branch hiện tại feat/nguyen-week5-auth-profile không phải branch riêng của Nhân, cần tạo branch riêng khi thực hiện bước tiếp theo theo yêu cầu người dùng. Không rewrite lịch sử/contributor cũ. Không sửa backend/Android hoặc cấu hình VPS nên không rebuild/deploy lại. Bộ4 role giữ nguyên vai trò; cập nhật bản source/tài liệu và bằng chứng ngoài repo. Rollback role chỉ phục hồi bản sao source/tài liệu, không đổi tài khoản GitHub, .git/config hoặc DB.


### Công bố branch crawler của Đào Mạnh Nhân — 2026-10-10

Trạng thái: **Đang làm bước công bố**, theo yêu cầu mới đã cho phép tạo branch/commit/push. Đã fetch và xác nhận main remote 4118310264c70bba8a0c00a2790ee3164e6f6ae2 đã chứa Auth/profile của Nguyên; tree main và HEAD cũ giống nhau nên tạo feat/dao-manh-nhan-week5-crawler-dates trực tiếp từ origin/main, giữ nguyên toàn bộ thay đổi crawler/catalog/date UI đã kiểm chứng. Không sửa main, không rewrite lịch sử/contributor cũ. Mọi commit mới chỉ Author/Committer Đào Mạnh Nhân <24521227@gm.uit.edu.vn>, tài khoản xác thực Manh-Nhan299; không thêm co-author/bot. Nội dung gồm crawler/import/catalog/metadata migration đã triển khai trước đó, giao diện ngày và tests, cùng hai MD; không có proxy support/secrets/env/log/artifact vận hành trong source gửi lên. Local55 backend/Android30 unit và12 UI native; VPS runtime nhiều ngày/proxy/idempotency đã đạt theo mục trên. Bước tiếp: kiểm tra staged files/secrets/diff, commit/push branch riêng, xác nhận SHA remote và GitHub attribution; chỉ ghi hoàn thành sau kết quả thật. Không deploy lại source direct lên VPS làm mất proxy private.


#### Kết quả công bố source đã xác minh — 2026-10-10

Trạng thái: **Hoàn thành tạo branch, commit và push source**, thay mốc Đang làm công bố phía trên. Branch feat/dao-manh-nhan-week5-crawler-dates tạo từ main remote4118310264c70bba8a0c00a2790ee3164e6f6ae2; commit source25f16de6f67266c6e3bf5cd658f07ff137c3a5c5 đã push thành công, remote branch SHA khớp và main giữ nguyên SHA. GitHub API xác nhận tài khoản đăng nhập, commit Author và Committer đều Manh-Nhan299; email Author/Committer24521227@gm.uit.edu.vn, tên Đào Mạnh Nhân, không co-author/bot. Source29 file thay đổi gồm cả hai MD; không đưa env/secrets/proxy variant/log/selftest tạm/artifact vận hành lên. Git diff --cached --check và secret-pattern scan đạt; chạy lại backend55/55 tests exit0 trước commit. Android30 unit/12 UI và runtime VPS giữ kết quả đã kiểm chứng vì source ứng dụng không đổi trong bước công bố.

Đối chiếu VPS sau push exit0:37 file runtime khớp manifest, public source của commit25f16de đối chiếu theo LF với bản môi trường chỉ khác đúng4 file proxy variant đã chốt. Release crawler-dates-49365407a08485ce vẫn healthy,StartedAt2026-10-10T13:17:47.939247555Z không đổi,health database up/Swagger200/dates10–15; proxy private root600 vẫn giữ. Đã ghi publication-reference.json ngoài repo tại release VPS để liên kết SHA public với manifest/overlay đang chạy. Không rebuild/recreate API hoặc migrate/restore DB chỉ vì publish cùng bytes source; không thay source VPS bằng bản direct mất proxy. Lịch cron và kết quả nhiều ngày giữ nguyên. PR chưa tạo/merge; nhóm trưởng có thể review branch này. Bản ghi tiến độ này được đưa vào commit tài liệu tiếp theo cùng cả hai MD, cũng chỉ danh tính Đào Mạnh Nhân; không bịa SHA cho commit tài liệu chưa tạo.

Source commit: https://github.com/Tonyisded/N8_Cineplex-Project/commit/25f16de6f67266c6e3bf5cd658f07ff137c3a5c5
Branch: https://github.com/Tonyisded/N8_Cineplex-Project/tree/feat/dao-manh-nhan-week5-crawler-dates
