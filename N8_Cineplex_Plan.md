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

## 27.2. `movies` — 10 fields

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
