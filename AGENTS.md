# AGENTS.md — N8 Cineplex

Source of truth: `N8_Cineplex_Plan.md`.

## Công bố tài liệu triển khai PR #18 — 2026-10-26

Trạng thái: **Đang làm phần commit/push tài liệu theo yêu cầu người dùng**. Chỉ công bố `N8_Cineplex_Plan.md` và `AGENTS.md` trên nhánh `docs/manh-nhan-pr18-vps-deployment`; không push main, force push hoặc tự tạo/merge PR. Danh tính commit phần này: Đào Mạnh Nhân <24521227@gm.uit.edu.vn>. Đã đọc lại hai tài liệu, kiểm tra nội dung đồng bộ và diff whitespace; kết quả deploy PR #18 bên dưới giữ nguyên, không chạy lại build/deploy/API hoặc ghi DB vì chỉ thay tài liệu. Kết quả commit/SHA/push chỉ bổ sung sau khi quan sát thực tế; bước tiếp theo là commit cả hai file và kiểm tra remote khớp.


## Triển khai PR #18 lên VPS — 2026-10-26

Trạng thái: **Hoàn thành triển khai backend PR #18 và các kiểm tra trong phạm vi deploy**. Thay các trạng thái chưa build/test/deploy realtime/expiry ở nhật ký cũ phía dưới; giữ lịch sử, không coi toàn bộ nghiệp vụ tương lai đã hoàn thành. Source merge `f4d5c3b49c220a735f2f4d05c1ba5faa96bde42a`; không sửa thêm code nghiệp vụ/Android/schema/dependency, không commit/push main.

- Local public build/typecheck/format và 99/99 tests đạt; candidate giữ 4-file proxy overlay ngoài source public đạt 102/102. VPS Linux tools cùng candidate/network-none/theme readonly đạt 102/102 exit 0. Lỗi 101/102 trước do thiếu theme đã sửa bằng mount readonly, không sửa test/source để né lỗi; lượt bị ngắt không tính đạt và đã chạy lại gate thành công.
- API đã recreate đúng image f4d5c3b, ID `sha256:7356a36745b0ee1582b00456d68d873d8466ef43e93b0fdff4af2ab768324642`, StartedAt `2026-10-26T03:30:13.815253302Z`; healthy, restart unless-stopped, RestartCount 0, startup thành công, 0 ERROR/FATAL lúc kiểm tra. Checkout VPS đúng SHA; 5 hash dist runtime khớp tools đã test. PostgreSQL/Nginx/private env không đổi; chỉ recreate API, không public 5000/5432.
- Health/Swagger/OpenAPI HTTPS 200, đủ Orders/seats; coming-soon 25 item tại snapshot. Catalog/detail/seats thật 32 ghế đạt; order không JWT 401; WebSocket HTTPS/TLS và AUTH_REQUIRED cho JWT sai đạt. Crawler runtime qua proxy CGV HTTP200 lúc 03:31:32.012Z. Không import/fixture ghi DB; Prisma 4 migrations/schema up to date nên không migrate/reset/seed/restore. Giữ image cũ để rollback, không prune volume/network/cache.
- Giữ bốn role ngoài repo và supplemental records; chỉ đổi image/build.context của api/tools trong compose. BASELINE 2f2547b → MODIFIED f4d5c3b → ROLLBACK bản sao 2f2547b, config --images đều exit0; pristine hash và patch tái tạo bytes đạt; active/modified vẫn f4d5c3b. Đây là rollback bản sao cấu hình, không rollback API thật/DB. API trước deploy là f6af43b dù compose cũ tham chiếu 2f2547b; dùng supplemental live-image rollback nếu cần, không giả định hai trạng thái giống nhau.

Chưa chạy lại concurrency PostgreSQL/E2E600s trên DB chung hoặc Android vật lý; không gọi tests fixture là PostgreSQL concurrency thật, không tái dùng chấp thuận fixture run cũ. Hai MD cập nhật đồng bộ local, chưa commit/push tài liệu. Bước tiếp: review/công bố tiến độ trên branch riêng khi người dùng yêu cầu; fixture live mới cần chấp thuận riêng. Không còn bước bắt buộc đang treo trong phạm vi deploy PR #18.


## Đối chiếu GitHub và gói backend còn thiếu — 2026-10-26

CI của head cab9895 (run40216825084) đã đọc log: build/typecheck/format đạt, npm test82/83 và bài Auth HTTP thất bại vì JWT_ACCESS_SECRET được kiểm tra trước fixture env. Đây là suite backend cũ trên remote, chưa chứa7file bổ sung; file auth.test.mjs sửa thứ tự import vẫn còn local. Không dùng CI này làm bằng chứng bản sửa mới đã build/test đạt. Cần upload phần còn thiếu rồi xem CI mới trước khi review merge.

Trạng thái: **Hoàn thành đối chiếu source; chưa nghiệm thu toàn bộ backend/VPS**. GitHub main vẫn `e12b0e6afe83ac11ca50be9902d2db46e8a60346`. Commit mới nhất chạm backend trên main là `f6af43bf81e6a90b97fa25d48cd3c9a8c77f31d7`, tác giả/committer Trần Thanh Nguyên (`thnguyen290106`), thêm crawler/API coming-soon lúc16:52:02Việt Nam25/10. PR17 do cùng tài khoản merge lúc17:27:29. Đây là nguồn nền chung, không có nghĩa toàn bộ backend là công việc riêng của một người.

Người dùng đã tạo `feature/manh-nhan`: head `cab9895c672cd7722bd082fb4671447133febf6e`, tác giả Dao Manh Nhan/Manh-Nhan299, committer GitHub web-flow, lúc00:30:45Việt Nam26/10. Compare với main: ahead1/behind0, chỉ một file `.github/workflows/backend-ci.yml`; nội dung khớp local. Mục này thay kết luận chưa có branch ở lượt trước. Backend và hai MD chưa được upload. Không ghi nhận thao tác upload này là do agent thực hiện.

Đối chiếu Git blob của50file backend trên main:46file local khớp,4file sửa có chủ đích (gateway/service và Auth/booking tests),3file mới (subscription policy và hai realtime test files). Các file crawler/catalog/schema/package/lock không đổi; phần coming-soon của Nguyên còn nguyên. Gói còn thiếu gồm7file backend và2MD, loại workflow đã có. Chỉ đóng gói để review/upload vào feature/manh-nhan; không upload nguyên backend/verification hoặc ghi đè main. Chạy lại9/9policy và syntax checks đạt; full build/typecheck/format/transport/PostgreSQL concurrency/VPS vẫn chưa được kiểm chứng trong lượt này. Backend sửa đúng phạm vi so với main hiện tại, không được diễn giải thành đã đạt toàn bộ runtime acceptance.

## Yêu cầu xuất bản bằng Git CLI lên feature/manh-nhan — 2026-10-25

Trạng thái: **Chưa commit/push; clone Git CLI bị chặn kết nối mạng**. Người dùng yêu cầu dùng Git CLI, branch `feature/manh-nhan`, không push main/force push/reset/xóa code/tự merge. Tên branch này thay mục tiêu `feat/dao-manh-nhan-week7-realtime-expiry` của lượt công bố trước; trigger push CI đã đổi tương ứng, không thay logic nghiệp vụ.

Đã kiểm tra git status/branch/log/remote trong folder source: cả bốn đều báo not a git repository, không có `.git`, branch, commit hoặc remote local. Đã bảo toàn238file source bằng ZIP/hash trước cập nhật; bundle cũ giữ nguyên. Thử `git clone --no-checkout --single-branch --branch main` vào folder riêng thất bại kết nối GitHub443, trước xác thực. Không có clone hợp lệ để tạo commit/push an toàn; không git init thành root history mới hoặc giả nhận đã push. GitHub main được kiểm tra vẫn `e12b0e6afe83ac11ca50be9902d2db46e8a60346`.

Kết nối đọc xác nhận tài khoản Manh-Nhan299 và quyền user push=true; repo public, allow_forking=true. Endpoint repo cá nhân Manh-Nhan299/N8_Cineplex-Project trả404: chưa xác nhận fork tồn tại hoặc có thể truy cập. Phiên này chưa tạo fork/PR/commit/branch, không có URL branch mới. Lỗi Git CLI là mạng, khác lỗi403 của integration ở lượt trước; không suy ra user thiếu quyền push. Chuẩn bị quy trình ngoài source: clone đúng upstream, kiểm tra base SHA, copy đúng10file nhiệm vụ, kiểm thử, commit với Đào Mạnh Nhân/24521227@gm.uit.edu.vn, push chỉ feature/manh-nhan; tùy chọn remote fork sau khi fork đã tồn tại. Không tự tạo token hoặc đổi quyền ứng dụng. Full build/format/transport/VPS vẫn chưa được nghiệm thu;9/9policy là kết quả có phạm vi đã ghi ở dưới.

Bước tiếp theo cần kết nối Git CLI khả dụng: chạy quy trình đã chuẩn bị từ terminal của người dùng, xác nhận SHA/remote/CI rồi ghi kết quả thật vào cả hai MD. Nếu push403, kiểm tra tài khoản Git CLI/credential; fallback fork và PR vào Tonyisded/main, không merge tự động.

## Công bố phần tuần 7 của Đào Mạnh Nhân — 2026-10-25

**Cập nhật cuối phiên:** đã chuẩn bị source và bộ bằng chứng local; **chưa tạo được branch/commit GitHub**. API tạo blob và tạo branch đều trả HTTP403 `Resource not accessible by integration`; quyền push của tài khoản không đồng nghĩa kết nối ứng dụng có quyền ghi. Không có branch/commit mới hoặc CI mới để báo. Giữ nguyên main. Local policy hiện9/9đạt; chạy cùng9bài với thân join gốc trích nguyên từ baseline (fixture DB/socket) tái hiện6lỗi/3đạt, bản sửa9/9đạt. Đây là kiểm thử policy, không phải transport hay PostgreSQL concurrency. Đã siết bài overlapping join bằng một event-loop turn để thật sự tái hiện race ở baseline. Hai bài mới xác nhận kết nối khác không bị chặn bởi socket đang đợi và suất không tồn tại không làm mất subscription đang có. Syntax regressions đạt; full build/typecheck/format/transport và VPS vẫn chưa được chạy trong phiên này. Bước công bố chờ quyền ghi kết nối hoặc người dùng chạy quy trình kiểm thử/commit/push đã chuẩn bị bằng terminal riêng.

Trạng thái: **Đang kiểm chứng và công bố trên branch riêng theo yêu cầu người dùng**. Mục này cập nhật tiến độ phần tích hợp bên dưới. Base GitHub `main` đã đối chiếu là `e12b0e6afe83ac11ca50be9902d2db46e8a60346`; giữ lịch sử nhóm và chỉ đưa các file liên quan tuần 7 vào branch `feat/dao-manh-nhan-week7-realtime-expiry`. Kết nối GitHub xác nhận tài khoản `Manh-Nhan299`, email `24521227@gm.uit.edu.vn`, có quyền push. Không dùng danh tính Trần Thanh Nguyên cho commit mới của phần này.

- Giữ các sửa ShowtimeSubscriptions, clock PostgreSQL và regression booking/realtime hiện có; không thay Android, API contract, schema, dependencies hoặc crawler proxy overlay.
- Sửa test Auth nạp AppModule bằng dynamic import sau khi gán fixture env; đăng ký khôi phục env trước khi compile để cả lỗi setup cũng được cleanup. CI cũ `40184843274` đã build/typecheck/format thành công nhưng chỉ đạt82/83 tests vì ConfigModule kiểm tra JWT trước khi fixture được gán; đây là lỗi test setup, không phải bằng chứng lỗi đăng nhập production.
- Workflow backend chạy thêm khi push đúng branch tuần 7 này; giữ quyền contents:read và crawler live chỉ chạy khi được chọn thủ công. Khi format thất bại, in diff gợi ý trên runner; không tự commit code từ CI.
- Local:9/9 policy tests và syntax checks đạt; dependencies đầy đủ/build/transport tests còn chưa chạy local do mạng/cache. CI mới chưa được khởi chạy vì thao tác ghi GitHub bị403; chưa có commit SHA. VPS chưa truy cập/deploy, chưa chạy lại concurrency PostgreSQL hoặc chờ600giây trên VPS. Không dùng bằng chứng lịch sử làm kết quả mới và không đánh dấu Excel hoàn thành.

Bước tiếp theo: xác minh commit/branch/attribution thực tế, đọc kết quả CI và sửa lỗi có thể tái hiện, cập nhật cả hai MD cùng verification/rollback. Backend chỉ được ghi hoàn thành triển khai khi có kiểm chứng VPS tương ứng.

## Tích hợp phần tuần 7 của Đào Mạnh Nhân với API/UI mới — 2026-10-25

Trạng thái: **Đang làm; source và kiểm thử policy đã có, chưa hoàn thành build/tích hợp/VPS**. Đọc bản ZIP mới do người dùng cung cấp trong `N8_Cineplex-Project-main (1)`. Bản này đã có BookingService, BookingGateway, UI/API giữ ghế và lịch sử E2E600.113s của nhóm; không chép lại module realtime/expiry của bản nháp31ff7a4 vì khác path/event/lock order và sẽ tạo hai worker. Giữ duy nhất BookingService.expire hiện có, `/socket.io/`, `JOIN_SHOWTIME`, auto user room, deadline600s và khóa showtime → users UUID → orders/seats hiện hành. Các kết quả E2E lịch sử chưa được chạy lại trong phiên này.

- Bổ sung ShowtimeSubscriptions trong module booking: chuẩn hóa UUID lowercase, xếp hàng join/leave riêng từng socket để chỉ còn suất được yêu cầu cuối, kiểm tra connected trước/sau các await để tránh ghost rooms sau disconnect, giữ private room, từ chối room tùy ý/ID không tồn tại. Giới hạn30 JOIN_SHOWTIME/phút/socket để chặn hàng đợi/DB không giới hạn. Không đổi payload sự kiện, hợp đồng REST, UI hoặc dependencies/lockfile.
- Worker chọn ứng viên bằng cùng clock_timestamp của PostgreSQL như deadline/transition; không dùng giờ máy ứng dụng để bỏ sót hold khi app clock chậm. Không thay cutoff, 600 giây, lịch chạy1giây/startup, transaction/state guard/credit restoration hoặc thứ tự khóa.
- Kết quả phiên này: `node --test tests/realtime-policy.test.mjs` **7/7 đạt**; cú pháp hai file regression booking/realtime đạt. Đã viết thêm3 transport regressions (UUID case, overlapping join, disconnect trong lookup),2 clock-skew và2 expiry/cancel/credit-rollback regressions trong booking.test.mjs; **chưa thực thi** vì dependencies chưa cài đủ. Fixture transaction được bổ sung snapshot creditBalance để rollback trung thực khi lỗi sau bước cộng credit; không thêm credit reservation vào endpoint production. Không gọi7policy tests là concurrency PostgreSQL hoặc Socket.IO E2E.
- npm registry bị EACCES. Đã dùng cache hiện có nhưng npm ci --offline dừng ở thiếu xmlhttprequest-ssl2.1.2, không đổi lockfile; npm cleanup có EPERM và không nhận đã cài đủ. Người dùng đang được yêu cầu chạy npm.cmd ci --no-audit --no-fund trong PowerShell ngoài Codex tại backend folder mới. Không chạy npm install đồng thời trong cùng node_modules khi người dùng đang cài.
- SSH đến VPS bị Permission denied ngay ở kết nối TCP22, trước xác thực; chưa dùng được key người dùng cung cấp, chưa SSH/migrate/seed/ghi DB/deploy. Không gọi lỗi mạng là sai key; không log/đưa private key vào repo. Không tái sử dụng chấp thuận fixture commit của run lịch sử cho lượt mới. Khi truy cập được cần đọc-only preflight, giữ4 proxy overlay/PG/Nginx/secrets và nghiệm thu backend sau deploy theo quy tắc hiện có; chưa đạt thì ghi chưa hoàn thành.
- Chưa push/merge/commit source mới; bản cũ ngoài workspace đã không còn tại đường dẫn cũ. Lưu baseline/patch/verification/rollback ngoài repo trong thư mục verification của phiên mới; không đưa log, cache, key hoặc runner tạm vào Git. Bước tiếp theo: cài dependency → baseline/modified regressions, Prisma build/typecheck/format/full tests → kiểm tra tương thích Android → rollback copy → cập nhật kết quả cả hai MD. Chỉ công bố Git khi người dùng yêu cầu.

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
| 2026-10-25 | Phim Sắp Chiếu từ CGV (crawler/API/Android, migration duration) | Hoàn thành | Backend commit `f6af43b` và Android commit `03526a5` đã push; local 83/83 tests và bản proxy variant 86/86 đạt. VPS: backup trước migration, migrate `20261025000000_coming_soon_duration` đạt, 4-file proxy overlay 86/86 Linux tests, API mới healthy/Swagger 200, endpoint trả 24 phim; one-shot qua proxy tạo 24/33 phim, 2 omitted thiếu ngày, 7 skipped do proxy/trang chi tiết nên PARTIAL, không phải SUCCESS. Android 55 unit/34 connected (gồm live VPS) đạt, lint 0 lỗi. Đã prune image/cache (giữ 3 image đang chạy), xoá temp VPS; verification bundle/ROLLBACK.sh đã kiểm tra trên bản sao. Giới hạn: 7 phim chưa vào DB lượt đầu, chưa quan sát cron tick, chưa thử máy vật lý. |


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
source_url UNIQUE
original_title nullable
release_date nullable (DATE)
director nullable
cast nullable
trailer_youtube_id nullable (VARCHAR(11))
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
presentation_label nullable
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

Booking cutoff (chốt 2026-10-25): chỉ tạo hold mới khi DB `clock_timestamp() < showtimes.start_at`, suất/phòng ACTIVE; kiểm tra lại thời gian DB sau khi chờ khóa/kiểm tra ghế. Tới hoặc qua giờ bắt đầu trả `409 SHOWTIME_UNAVAILABLE`, không tạo đơn/giữ một phần ghế/phát sự kiện. Không tự thêm mốc dừng trước 10/15 phút, không cắt ngắn hạn600s của hold hợp lệ. Đọc/retry cùng Idempotency-Key/payload sau cutoff giữ trạng thái/deadline cũ; retry tới hạn chỉ expire/nhả một lần, không hồi sinh/gia hạn. Giữ ownership, transaction, thứ tự khóa và phạm vi payment/combo/credit hiện tại.

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


## Cập nhật Android 2026-10-09 — đăng nhập và profile từ ZIP mới

- Trạng thái: **Hoàn thành phần UI mock**. Thiết kế hiện hành là `design/signin-signup.zip`, giải nén tại `design/signin-signup/`; đối chiếu entry, cineplex.css/js và HANDOFF.md. Quy tắc nghiệp vụ trong Plan vẫn áp dụng.
- Đăng nhập khách hàng có nút Google với logo gốc, chỉ mở thông báo; không tạo phiên. Không có nút Google ở signup/staff, khóa khi form đang gửi.
- Profile mở từ avatar Home/admin hoặc Tài khoản; hiển thị initials/tên/email/role lấy từ session, không chứa mật khẩu. Mobile một cột, tablet từ 768dp hai cột; navigation khách hàng chọn Tài khoản, admin không có navigation khách hàng.
- Back/Trang chủ về Home/admin, giữ query/tab/scroll Home. Logout xóa phiên; Back hoặc mở tài khoản sau logout không vào lại profile. Chỉnh sửa/đổi mật khẩu/hỗ trợ mở thông báo; chính sách bảo mật mở mẫu hiện có.
- Sửa lỗi lint có sẵn bằng cách đặt windowLightNavigationBar trong values-v27, giữ minSdk 24. Không đổi backend/database/API; không cần deploy VPS cho đợt Android này.
- Bằng chứng 2026-10-09: assembleDebug và 18 unit tests đạt; lintDebug đạt (0 lỗi, 23 cảnh báo). Toàn bộ 10 instrumentation tests đạt ở 360×800dp; chạy lại 3 test Google/profile trên APK cuối cùng ở 360×800dp và 820×1180dp đều đạt. Đã đọc ảnh mobile/tablet tại app/build/visual-profile-360/visual_checks và visual-profile-tablet/visual_checks; emulator khôi phục kích thước gốc 1344×2992.
- APK: N8_Cineplex/app/build/outputs/apk/debug/app-debug.apk. Chưa commit/push; giữ các thay đổi sẵn có của người dùng. Còn lại ngoài scope UI: tích hợp Google/profile API thật theo Plan khi triển khai backend tương ứng; bước tiếp theo là người dùng review UI.
## Triển khai Auth/profile tuần 5 — 2026-10-10

Trạng thái: **Đang làm**. Scope: Auth và profile thật cho backend + Android, deadline 11/10/2026; catalog/Home/booking/payment/quản trị nghiệp vụ giữ nguyên cho các tuần sau.

Quyết định: hai cổng CUSTOMER và STAFF (STAFF/ADMIN); access 15 phút, refresh 7 ngày, một refresh hash/user và không rotation; logout không blacklist access token. Google không ghép tài khoản cùng email; Google-only không tạo/reset mật khẩu Cineplex, đổi email phải Google reauthentication. Signed email token: verify/change-email 30 phút, reset 15 phút; link fragment và xác nhận thủ công. Avatar JPEG/PNG/WebP <=5 MB, chuyển WebP <=512px, đầu vào <=16MP; storage riêng persistent. Không thay schema/10 bảng.

SMTP: Nodemailer + Gmail TLS; GMAIL_USER là group08.nt118.r14@gmail.com; GMAIL_APP_PASSWORD dùng credential người dùng cung cấp, chỉ trong cấu hình riêng, không ghi giá trị vào source/tài liệu/log. Email test anhnguyenfb2021@gmail.com và alias riêng theo run ID. Không dùng password Gmail làm password Cineplex.

E2E được phép tạo CUSTOMER tạm trong DB chung; runner ngoài source phải ghi ledger UUID/run ID/email chính xác, cleanup trong finally. Không xóa bằng email đơn độc. Kiểm tra role CUSTOMER/credit=0/không order hoặc held seat trước khi xóa; có dữ liệu không chứng minh được thì rollback và xin xác nhận. Không tác động CUSTOMER thật/STAFF/ADMIN; kiểm tra DB và ảnh sau cleanup. Fixtures khác luôn rollback; không reset/restore DB.

Key SSH cập nhật: D:\NT118\vpskey\shibakey.ppk, user ubuntu; giữ host-key validation. Google OAuth IDs còn thiếu, đang yêu cầu thông tin. Đã lưu baseline source bên ngoài source; backend typecheck baseline exit 0. Chưa triển khai Auth local/VPS, chưa tạo account test, chưa gửi SMTP; không ghi hoàn thành trước bằng chứng. Bước tiếp theo: backend/API/tests -> Android -> local -> giai đoạn VPS được cho phép -> E2E/cleanup. Mọi tiến độ mới phải cập nhật cả hai tài liệu.


### Tiến độ tiếp nối Auth/profile — 2026-10-10

Trạng thái: **Đang làm**, thay ghi nhận trước đó chưa có Auth local/SMTP/OAuth. Đã triển khai API Auth/profile/avatar và tích hợp Android API/Credential Manager/DataStore+Keystore; không fallback mock khi lỗi mạng, không thay schema/10 bảng/103 cột. Chi tiết API/config/TTL ở phần cập nhật tuần 5 của Plan. Google đổi email cần cùng sub và ID token iat trong 5 phút (future skew <=60s); không auto-link password account, không tạo password cho Google-only.

- SMTP credential đã lưu trong backend/.env riêng; authentication/TLS đạt; không ghi secret vào source, tài liệu, logs, archives.
- Google Cloud Console đã tạo project n8-cineplex-nt118-2026 và Web/Android Debug clients, External/Testing, chỉ openid/email/profile; test user anhnguyenfb2021@gmail.com. Web public ID trong backend private config và Android gradle.properties; package com.nhom8.cineplex/SHA-1 lấy từ signingReport. Plan lưu public IDs và SHA-1/SHA-256; chưa có client release/signing release. Không dùng Firebase Auth hay cài CLI không cần thiết.
- Backend build/typecheck/Prisma validate/Prettier đạt, 27 tests (14 Auth + 13 crawler) đạt. Android assembleDebug/unit/lint đạt, 22 unit tests, lint 0 lỗi/41 cảnh báo. Connected lần đầu 8/10 pass, 2 fail do kỳ vọng placeholder cũ; assertions đã sửa, còn chạy lại. Test Keystore mới chưa chạy trên device.
- HTTP E2E local trên DB chung + SMTP thật đạt register/duplicate/verify/login/refresh/role guard/profile/avatar/forgot-reset/change password/change email/logout, exit 0. Email token lấy trong RAM tại SMTP boundary; chưa đọc inbox/click link người nhận, không được gọi phần inbox đã đạt. Google unit không thay Google E2E thật.
- Cleanup finally đã xóa đúng UUID CUSTOMER do runner tạo, hash refresh và avatar; DB không còn user/order/held seat tương ứng, snapshot hash tài khoản khác không đổi. Không tạo booking/payment/ticket. Runner/ledger ngoài source; giữ điều kiện role/email/UUID/GoogleID/credit/ownership, row lock và FK checks; gặp dữ liệu không chứng minh được thì từ chối, báo lỗi/xin xác nhận. Không xóa theo email đơn độc.
- Giữ cùng bốn role D:\NT118\verification\week5-auth-profile-20261010\MODIFIED_FILE.zip, DIFF_FILE.patch, VERIFICATION.txt, ROLLBACK.sh. Rollback trên bản sao khớp hash original; matching typecheck cả ba exit 0; probe login {} BASELINE 404/MODIFIED 400/ROLLBACK 404, probe exits 0. DIFF tái tạo modified byte-for-byte; phải refresh/reopen role sau bổ sung tài liệu/source.
- VPS health hiện có 200/database up, chưa cập nhật/kiểm tra Auth tuần 5. Không coi health bootstrap là bằng chứng Auth VPS. Không migrate/reset/restore, không thay dịch vụ VPS trong đợt này.

Tiếp theo: chờ người dùng đăng nhập anhnguyenfb2021@gmail.com trên emulator Pixel_10_Pro_XL; chạy external google-e2e.mjs (API loopback 5000, timeout 300s, ledger creation UUID, cleanup finally) rồi Google Login bằng app thật. Không chạy app Google trước khi runner ledger đã khởi động. Chạy lại connected UI + Keystore tests sau bước người dùng đăng nhập, kiểm tra inbox thật và VPS còn thiếu, cập nhật cả hai tài liệu. Chỉ ghi hoàn thành khi tất cả bước tương ứng có bằng chứng. Giữ secrets ngoài source; không in browser client-secret dialog hoặc raw auth payload.

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


## Crawler/catalog tuần 5 — 2026-10-10

Trạng thái: **Đang làm**. Theo Plan mới: crawler Landmark81 và catalog Android trong đợt này, không crawler_runs table. Movies14 cột (bốn metadata nullable), tổng107 cột nghiệp vụ, đúng10 bảng; source_url UNIQUE, duplicate phải dừng migration, không merge/xóa.

Cron00:00/06:00/12:00/18:00 Việt Nam; businessDate ngày lịch UTC+7 chốt đầu run; logs thay bảng; local mặc định tắt, VPS chỉ bật sau xác minh. Giữ HTTPS/host allowlist/redirect refusal, parse tuổi/thời lượng/format theo film-screen, không công nghệ quảng cáo. SourceKey site071/dy/seq. Giá demo/room/seat/API contract theo Plan; transaction lock, import lặp không reset inventory/hồi sinh CANCELED, structural conflict giữ dữ liệu, không xóa suất nguồn đã gỡ. Không suy tên gốc/năm sản xuất.

Production Android API thật không fallback mock; Sắp chiếu chưa tích hợp; catalog jobs tách Auth, hủy logout, refresh resume/đổi ngày; booking/payment/VIP/couple zoning ngoài phạm vi. Baseline31 tests; parser local đủ9 phim/24 suất không skip, Android build/unit/lint đạt. Còn tests mới/VPS migration sau backup/triển khai/snapshot/API/Android thực/rollback. Chưa báo hoàn thành; giữ Auth users, secrets và dữ liệu hiện có. Đoạn Unicode mới đã sửa truyền UTF-8, lịch sử giữ nguyên.


### Kiểm chứng crawler và xử lý chặn backup — 2026-10-10

Trạng thái: **Đang làm**. Backend42 tests đạt, Android26 unit và12 instrumentation trên Pixel10 Pro XL emulator đạt; lint0 lỗi/41 cảnh báo cũ. Ngày11/10 live CGV12 phim/43 suất parse đủ sau hỗ trợ nhãn SCREENX-2D theo capability thực tế. Artifact rollback đã chạy trên bản sao: baseline31 → modified42 → restored31 tests đều exit0, hash restored khớp baseline; đã sửa lựa chọn Python để bỏ alias Microsoft Store không chạy được.

VPS image Chromium/Argon2/sharp đạt, nhưng backup trước migration thất bại với Invalid deployment SHA vì script cũ chỉ nhận40hex còn Auth dùng source snapshot week5-<16hex>. Migration đã dừng, DB chưa đổi. Sửa tối thiểu metadata backup ngoài source: lấy source_identity từ image API thực tế (hỗ trợ identity cũ và week5/crawler snapshot), không suy từ APP_SHA cũ; giữ dump/hash/list/lock/retention72 giờ chỉ dọn sau backup mới thành công, không restore. Còn backup mới/migration/VPS crawl/API/UI live và lịch tự động. Không coi image build là deploy thành công.

### Xác nhận localhost và kết quả tiếp nối crawler — 2026-10-10

Kiểm tra mạng tiếp nối 10/10: CGV DNS `183.91.22.138`, host HTTPS timeout exit 28 trước TCP/TLS; trang kiểm soát playwright.dev HTTP200. Capture ens5 giới hạn 12 giây thấy 7 SYN tới CGV TCP443, không có phản hồi; chưa kết luận CGV chặn AWS. Chưa sửa firewall/SG/NACL hoặc source. Đang chờ ảnh SG Outbound và NACL Inbound/Outbound đúng subnet để kiểm tra TCP443/return ports/rule precedence; nếu AWS cho phép thì quản trị kiểm tra Flow Logs/đường mạng với AWS và CGV. Không thêm proxy/đổi IP hay tăng timeout để báo đã sửa. Crawler tự động vẫn Bị chặn, HOST local giữ 127.0.0.1.

Giữ source backend mặc định `HOST=127.0.0.1`, `PORT=5000`; địa chỉ VPS chỉ là endpoint kiểm thử, bind container truyền riêng qua môi trường ngoài source. Không sửa HOST/PORT hoặc triển khai lại chỉ để xác nhận. Kiểm tra `validateEnvironment` cùng fixture không truyền HOST/PORT trên BASELINE/MODIFIED/ROLLBACK đều in `HOST=127.0.0.1 PORT=5000 CRAWLER_ENABLED=false`, exit 0; bốn role giữ nguyên đường dẫn ngoài repo `D:\NT118\verification\week5-crawler-20261010`.

Thay trạng thái tạm thời trước đó: backup mới/migration đã đạt, API VPS đang chạy `crawler-514d0caff107cbdc`; local/VPS catalog đối chiếu snapshot ngày 10/10 đạt 9 phim/24 suất, catalog Android qua VPS đạt. Crawler tự động vẫn **Bị chặn** do runner thật trên VPS timeout tới CGV; cron vẫn tắt, không coi nhập snapshot từ Windows là VPS crawl thành công. VPS là EC2 Singapore theo người dùng, chưa kết luận nguyên nhân mạng. Tiếp theo xử lý outbound CGV, chạy runner thật, chỉ sau khi đạt mới bật/xác minh lịch 00/06/12/18 giờ Việt Nam. Giữ dữ liệu/Auth/secrets, không reset/restore DB.


### Proxy riêng cho crawler — 2026-10-10

Trạng thái: **Đang làm**, theo yêu cầu người dùng dùng proxy đã cấp; quyết định này thay hướng kiểm tra trực tiếp EC2 → CGV làm bước tiếp theo. Chỉ crawler browser context dùng proxy HTTP(S) có xác thực; không đổi HOST=127.0.0.1/PORT=5000 trong source, không áp proxy cho Auth/API/DB hoặc máy chủ toàn cục. Cấu hình qua CRAWLER_PROXY_SERVER, CRAWLER_PROXY_USERNAME, CRAWLER_PROXY_PASSWORD ngoài source; cả ba phải đủ hoặc cùng không có. Reject SOCKS/URL chứa credentials/path/query/fragment hoặc cấu hình thiếu bằng thông báo không chứa secrets. Khi đã cấu hình proxy, lỗi phải thất bại, không tự fallback direct. Giữ CGV HTTPS/TLS verification, allowlist và từ chối redirect; không thêm thư viện, bảng hoặc API token/đổi IP tự động. CLI smoke cũng nhận cùng cấu hình.

VPS kiểm tra endpoint thực: dạng https:// timeout TLS tới proxy, dạng http:// tạo CONNECT200 và CGV HTTP200, ssl_verify_result=0; dùng giao thức HTTP CONNECT đã quan sát, không suy từ nhãn thương mại HTTPS. Credentials lưu riêng với quyền hạn chế, VPS file root-owned mode600; không ghi vào tài liệu/source/bằng chứng. Baseline bổ sung trước proxy42 tests đạt; modified45 tests đạt, gồm validation/proxy browser context/TLS/fail-closed. Còn Playwright live local/VPS, triển khai source đúng hash, kiểm tra health/catalog/Auth không đổi và bật/xác minh cron sau live run thành công. Không migrate/reset/restore DB; chưa báo hoàn thành.


### Kết quả tích hợp proxy crawler — 2026-10-10

Proxy **Hoàn thành local và VPS**, thay trạng thái chặn timeout trước; chưa quan sát tick cron tương lai. Dùng3 biến CRAWLER_PROXY_SERVER/USERNAME/PASSWORD riêng ngoài source, validate đủ hoặc không có, HTTP(S) endpoint không chứa auth/path/query/fragment; proxy chỉ trong crawler browser context và smoke, fail-closed, không fallback khi proxy lỗi. HOST source127.0.0.1:5000, local cron tắt. Private env VPS root mode600, không lưu secrets vào source/MD/ledger. Endpoint đã đo dùng HTTP CONNECT (không TLS tới proxy); CGV vẫn HTTPS/TLS verified. Cần endpoint TLS hợp lệ nếu muốn bảo vệ cả kết nối tới proxy; credential đã xuất hiện trong chat nên đổi và cập nhật private env qua kênh riêng.

Build/typecheck/format và45 backend tests đạt, trước proxy42/baseline gốc31. Local Playwright live7 phim/19 suất; VPS source crawler-fa88ffd1a8f79543 đúng37 file manifest, image healthy/health DBup/Swagger200. VPS runner thật SUCCESS runId89cd3a56-0ad1-4c69-a163-1e443d8615a4 ngày2026-10-10, snapshot10:15:06.309Z:6 phim/18 suất, skip/conflict0, unchanged18/created0/seatsCreated0. Local/VPS API đối chiếu đủ6 metadata/18 suất đạt; DB giữ9/24/4137 từ snapshot trước do không xóa nguồn đã gỡ,10 bảng/107 cột, room overlap0/seat capacity mismatch0, Auth users hash không đổi. Không migration/reset/restore, không đổi Android.

VPS cron chỉ bật sau run thật đạt; runtime CRAWLER_ENABLED=true/proxy configured,00/06/12/18 Việt Nam, waitForCompletion=true; tick tiếp2026-10-10T18:00:00+07:00 **chưa quan sát**. Tiếp theo đọc log tick đó để xác nhận SUCCESS/runId/businessDate/counts, không tạo scheduler/API song song hoặc tuyên bố đã tick chỉ từ nextDate. Dọn selftest/import runner và archive upload tạm xong. Dùng lại bốn role tại D:\NT118\verification\week5-crawler-20261010; rollback source không restore DB hay tự đổi runtime.

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

#### Kết quả cuối và bàn giao — 2026-10-10

Trạng thái: **Hoàn thành** nhiều ngày + chọn ngày trên Home/chi tiết; thay các mốc Đang làm/chưa kiểm chứng phía trên. Quy tắc thực hiện theo mục cùng tên trong Plan: GET selecteddate đã validate, hôm nay riêng, tương lai tăng từng ngày/dừng trống đầu tiên, lỗi không giả làm rỗng; import/log mỗi businessDate, batchRunId, không xóa/reset lịch/ghế/history. API static movies/dates đứng trước :id; chọn ngày không quá khứ, giữ khi back/resume, chuyển ngày quá khứ về hôm nay khi qua nửa đêm. Không thêm bảng/dependency/migration hoặc mở rộng booking/Sắp chiếu.

- Local55/55 backend tests/build/typecheck/Prettier; Android30 unit/12 UI native (có catalog VPS thật), lint0 error/41 warning, APK build đạt. BASELINE42 chỉ hôm nay/không date feature → MODIFIED55 nhiều ngày đến fixture12-trống → ROLLBACK42 phục hồi cũ, exit0; bản sao157 file/hash pristine, source chính vẫn mới. Bằng chứng ngoài repo, giữ4 role và mở rộng --dates/PRE_DATES_BASELINE.zip, không thay mode cũ.
- VPS release crawler-dates-49365407a08485ce,37 file manifest/build nativeLinux đạt. Staging môi trường58 tests đạt trên Windows (không phải58 chạy Linux), giữ4 file proxy variant bằng patch /etc/n8-cineplex/dates-vps-only-proxy.patch ngoài repo. Source local vẫn127.0.0.1, không hỗ trợ proxy; **VPS CÓ PROXY** private root600/env nguyên hash,0.0.0.0:5000/HTTPS443/crontrue/restartunless-stopped. Không deploy direct local lên VPS. PostgreSQL/Nginx không restart,users không đổi,10 bảng/107 cột,không migrate/reset/restore.
- Lượt thật SUCCESS10–15/10; tương lai11–15 có12/43,11/37,10/42,8/19,8/22 phim/suất;16/10 trống dừng,0 skip/conflict,163 suất/29806 ghế mới. Hôm nay nguồn4/9 còn lại,DB giữ9/24 từ trước theo quy tắc không xóa. Chạy lạiSUCCESS created0/seatsCreated0; API local/VPS đối chiếu toàn metadata và suất snapshot mới nhất đạt, dates10–15/empty16/invalid400/healthSwagger200. ALWAYS LALISA canonical detailT13/98,không dùng badgeP để hạ tuổi. DB187 suất/33943 ghế,materialization0 lỗi.
- Dọn sau deploy đạt theo flock:3 image đang chạy/cache0,còn trống≈23.607GB; không xóa volume/backup/proxy/source. Checker Health.Log động đã sửa, không lặp thao tác xóa; probeCGV200 sau dọn,containerStartedAt/env giữ nguyên.
- Cron00/06/12/18 Việt Nam giữ nguyên; chưa quan sát tick00:00 ngày11/10 của bản mới. Giữ rủi ro nguồn thay đổi/proxy và quy tắc dừng tại gap đầu tiên; không hứa crawl ngày sau gap. Đã mở lại đủ4 role, đối chiếu JSON patch158 file/rollback157 file pristine/command SINGLE_DAY exit0 và hai MD nhất quán; source chính giữ chức năng mới, không thao tác DB production.


### Xác minh danh tính Git/GitHub theo yêu cầu — 2026-10-10

Trạng thái: **Hoàn thành kiểm tra tài khoản và cấu hình danh tính local**. GitHub API /user dùng credential hiện có xác nhận login Manh-Nhan299; credential username chọn Manh-Nhan299. Cấu hình riêng repo user.name=Đào Mạnh Nhân, user.email=24521227@gm.uit.edu.vn; git var xác nhận Author và Committer cùng danh tính này. Trước đổi, danh tính local là Trần Thanh Nguyên với email của Nguyên; không sửa cấu hình global hoặc xóa tài khoản được lưu của người khác. Không ghi credential/token vào tài liệu hoặc hồ sơ. Chưa tạo commit, push hay thay branch; branch hiện tại feat/nguyen-week5-auth-profile không phải branch riêng của Nhân, cần tạo branch riêng khi thực hiện bước tiếp theo theo yêu cầu người dùng. Không rewrite lịch sử/contributor cũ. Không sửa backend/Android hoặc cấu hình VPS nên không rebuild/deploy lại. Bộ4 role giữ nguyên vai trò; cập nhật bản source/tài liệu và bằng chứng ngoài repo. Rollback role chỉ phục hồi bản sao source/tài liệu, không đổi tài khoản GitHub, .git/config hoặc DB.


### Công bố branch crawler của Đào Mạnh Nhân — 2026-10-10

Trạng thái: **Đang làm bước công bố**, theo yêu cầu mới đã cho phép tạo branch/commit/push. Đã fetch và xác nhận main remote 4118310264c70bba8a0c00a2790ee3164e6f6ae2 đã chứa Auth/profile của Nguyên; tree main và HEAD cũ giống nhau nên tạo feat/dao-manh-nhan-week5-crawler-dates trực tiếp từ origin/main, giữ nguyên toàn bộ thay đổi crawler/catalog/date UI đã kiểm chứng. Không sửa main, không rewrite lịch sử/contributor cũ. Mọi commit mới chỉ Author/Committer Đào Mạnh Nhân <24521227@gm.uit.edu.vn>, tài khoản xác thực Manh-Nhan299; không thêm co-author/bot. Nội dung gồm crawler/import/catalog/metadata migration đã triển khai trước đó, giao diện ngày và tests, cùng hai MD; không có proxy support/secrets/env/log/artifact vận hành trong source gửi lên. Local55 backend/Android30 unit và12 UI native; VPS runtime nhiều ngày/proxy/idempotency đã đạt theo mục trên. Bước tiếp: kiểm tra staged files/secrets/diff, commit/push branch riêng, xác nhận SHA remote và GitHub attribution; chỉ ghi hoàn thành sau kết quả thật. Không deploy lại source direct lên VPS làm mất proxy private.


#### Kết quả công bố source đã xác minh — 2026-10-10

Trạng thái: **Hoàn thành tạo branch, commit và push source**, thay mốc Đang làm công bố phía trên. Branch feat/dao-manh-nhan-week5-crawler-dates tạo từ main remote4118310264c70bba8a0c00a2790ee3164e6f6ae2; commit source25f16de6f67266c6e3bf5cd658f07ff137c3a5c5 đã push thành công, remote branch SHA khớp và main giữ nguyên SHA. GitHub API xác nhận tài khoản đăng nhập, commit Author và Committer đều Manh-Nhan299; email Author/Committer24521227@gm.uit.edu.vn, tên Đào Mạnh Nhân, không co-author/bot. Source29 file thay đổi gồm cả hai MD; không đưa env/secrets/proxy variant/log/selftest tạm/artifact vận hành lên. Git diff --cached --check và secret-pattern scan đạt; chạy lại backend55/55 tests exit0 trước commit. Android30 unit/12 UI và runtime VPS giữ kết quả đã kiểm chứng vì source ứng dụng không đổi trong bước công bố.

Đối chiếu VPS sau push exit0:37 file runtime khớp manifest, public source của commit25f16de đối chiếu theo LF với bản môi trường chỉ khác đúng4 file proxy variant đã chốt. Release crawler-dates-49365407a08485ce vẫn healthy,StartedAt2026-10-10T13:17:47.939247555Z không đổi,health database up/Swagger200/dates10–15; proxy private root600 vẫn giữ. Đã ghi publication-reference.json ngoài repo tại release VPS để liên kết SHA public với manifest/overlay đang chạy. Không rebuild/recreate API hoặc migrate/restore DB chỉ vì publish cùng bytes source; không thay source VPS bằng bản direct mất proxy. Lịch cron và kết quả nhiều ngày giữ nguyên. PR chưa tạo/merge; nhóm trưởng có thể review branch này. Bản ghi tiến độ này được đưa vào commit tài liệu tiếp theo cùng cả hai MD, cũng chỉ danh tính Đào Mạnh Nhân; không bịa SHA cho commit tài liệu chưa tạo.

Source commit: https://github.com/Tonyisded/N8_Cineplex-Project/commit/25f16de6f67266c6e3bf5cd658f07ff137c3a5c5
Branch: https://github.com/Tonyisded/N8_Cineplex-Project/tree/feat/dao-manh-nhan-week5-crawler-dates


## Catalog preview — phim, suất, ghế và trailer (2026-10-13)

Trạng thái: **Đang làm** theo plan được người dùng yêu cầu triển khai. Thay phạm vi catalog cũ chỉ poster/chi tiết bằng danh sách phim kèm suất ngay trên Home, bố cục 001–005.jpg nhưng giữ màu/logo N8. Không chọn/giữ ghế, tạo order/combo/payment/ticket/QR; chạm suất chỉ thông tin. Không thêm bảng/dependency/hạ tầng enterprise.

- API chỉ trả suất ACTIVE thuộc ngày đang chọn và startAt > serverTime; đúng giờ bắt đầu ẩn suất, phim hết suất ẩn khỏi list. Không xóa lịch sử DB. Lọc khung giờ nửa mở và tìm kiếm áp dụng sau lọc thời gian; phim hết suất trong bộ lọc cũng ẩn. Suất tương lai hết ghế vẫn hiện 0/Y. UI tự loại suất ở mỗi ranh giới phút, dùng thời gian server + monotonic; resume/đổi ngày lấy dữ liệu mới, qua ngày chặn ngày quá khứ.
- /movies giữ businessDate/items, thêm serverTime và showtimes trong mỗi movie; /movies/:id/showtimes thêm serverTime/presentationLabel/availableSeats/totalSeats; /movies/dates chỉ ngày còn suất tương lai. Ghế AVAILABLE/tổng inventory N8 tính theo batch, không lưu counters; HELD/SOLD/BLOCKED không tính còn ghế. Inventory thiếu ghi chưa có dữ liệu, không giả số CGV. Đúng giờ/kết thúc DB UTC đổi Việt Nam; qua nửa đêm chỉ hiển thị giờ bắt đầu–kết thúc, không hậu tố ngày theo cập nhật giao diện 2026-10-13 bên dưới.
- Additive migration 20261013000000_catalog_preview: movies.trailer_youtube_id VARCHAR(11) nullable, showtimes.presentation_label TEXT nullable; đúng10 bảng/109 cột thay107 ở spec hiện hành (giữ kết quả107 lịch sử). Phim15/suất12 cột. Nhãn ngôn ngữ lấy đúng film-screen; thiếu không suy phụ đề. Trailer chỉ ID YouTube hợp lệ từ iframe nội dung phim, thiếu không skip phim. Import cũ chỉ cập nhật nhãn, không reset inventory/hồi sinh CANCELED/thay cấu trúc conflict.
- Home danh sách dọc poster trái/nhóm suất phải, Trailer/Chi tiết; badge IMAX xanh/SCREENX đen/GOLD CLASS cục bộ, theo format từng suất, không bịa Dolby/IMDb. Giữ search/tabs/banner/date/navigation; bỏ nút Đặt vé giả. Nhóm format+presentation, xếp theo giờ sớm nhất; lọc00–09/09–12/12–15/15–18/18–21/21–24; giữ state khi trở về. Sắp chiếu vẫn chưa tích hợp.
- Trailer trong dialog WebView hệ điều hành, HTML cố định + ID đã validate, YouTube iframe, baseURL HTTPS theo appID để Referer; không native JS bridge/JWT/file access/SSL bypass/autoplay. Pause khi nền, destroy khi đóng/logout. Thiếu ID ghi chưa có trailer, cấm nhúng/lỗi báo và chỉ mở YouTube khi người dùng chọn.
- Local giữ localhost, không mã/config proxy; VPS giữ overlay proxy ngoài repo và private env hiện có. Key được người dùng xác nhận D:\NT118\vpskey\shibakey.ppk, host key tiếp tục validate. Backup thành công trước migrate deploy; không reset/restore DB. Backend deploy trước APK, nghiệm thu health/Swagger/API/UI/crawler qua proxy rồi mới cleanup image/cache theo khóa.

Baseline đã lưu ngoài repo tại D:\NT118\verification\week5-crawler-20261010\PRE_PREVIEW_BASELINE.zip, 159 file, SHA25631bc6a46766f55ae8134841155ba5bb64927a08b1b72f987045471722b04cf10. Backend build và55/55 tests baseline exit0; source UI/API mới chưa kiểm chứng, VPS chưa thay đổi. Còn backend/Android tests, rollback bản sao, commit/push/deploy và nghiệm thu VPS. Giữ cùng bốn role MODIFIED_FILE.zip/DIFF_FILE.patch/VERIFICATION.txt/ROLLBACK.sh, mở rộng bằng evidence preview và --preview; không ghi hoàn thành trước bằng chứng.


### Catalog preview — kiểm chứng local trước triển khai VPS (2026-10-13)

Trạng thái tổng thể: **Đang làm**. Backend public build/59 tests/format đạt; bản staging giữ overlay proxy build và62 tests local đạt, không gọi đó là tests trong container Linux. Android34 unit, build APK/AndroidTest/lint đạt (0 error/43 warning); native14/14 trên360×800dp đạt, gồm Auth/UI cũ, chọn ngày, groups/counters, đúng giờ ẩn suất/phim và trailer YouTube thật (state1 khi nhấn Play, state2 sau nền/resume). Thanh ngày/giờ đặt trước banner, giữ màu N8.

Cùng preview-check.mjs input13/10 14:10 Việt Nam: BASELINE movies[past-only,future-film], showtimes[at-start,future], không counter/trailer/serverTime; MODIFIED chỉfuture-film/future, counter7/10, trailerxGiimAKN0TM/serverTime07:10Z; ROLLBACK trở lại đúng BASELINE. Cả3 exit0. ROLLBACK.sh --preview thực thi trên bản sao,159 file khớp pristine/hash aggregatee32161107203d68f094f3def534eff1bd151c9d63e567e976cbfe6f3dcbc226e; workspace vẫn giữ GOAL. Thêm --preview vào cùng bộ4 role cũ, không thay baseline/hủy các mode cũ.

Các lỗi build/fixture đã giữ ngoài repo: thiếu importclip/lintcontainer thừa; launchGradle relativepath và tham sốclass comma qua bat; DateSelector đổi vị trí làm selector cũ không thấy; testWebView chưa chờ onReady; staging thiếu env fixture/theme. Đã sửa command/fixture và rerun đạt, không bỏ test hay nới bảo vệ. VPS preflight dùng key được cấp, TLS/hostkey kiểm tra: API cũ healthy/health DBup/Swagger200, privateenv/proxy và PostgreSQL/Nginx còn nguyên; DB107 cột. Bản mới catalog-preview-1a04af45f85fa0a8 (39 file backend, manifest) đang build; chưa migrate/deploy, không báo hoàn thành. Còn tablet/font/liveAndroidVPS, commit/push, backup+migrate/deploy và cleanup theo khóa.


### Catalog preview — kết quả triển khai và nghiệm thu (2026-10-13)

Trạng thái tính năng: **Hoàn thành local/VPS/Android**, thay các mốc Đang làm của hạng mục preview phía trên. Chỉ hiển thị: Home poster+suất+ghế/nhóm format/ngôn ngữ, filter ngày/giờ/search, IMAX/SCREENX/GOLD CLASS, dialog thông tin và trailer nội bộ. startAt <= now bị loại; phim hết suất hoặc hết suất trong khung lọc bị loại; client tick theo phút không cần thoát app. Không tạo/giữ ghế/order/payment/ticket hoặc tự sửa trạng thái DB.

- Local backend build/59 tests/format đạt; staging có overlay proxy62 tests local đạt (không phải62 tests Linux). API thật local127.0.0.1:5000 dùng tunnel DB chung: health200/database up, payload phim/suất/count/trailer ngày13/14 khớp VPS; đã dừng hai process local/tunnel do phiên kiểm tra tạo.
- Android34 unit/build/lint đạt0 error/43 warning. Native15/15 cuối ở360×800dp gồm9 UI/Auth cũ,2 ngày,3 preview và1 catalog HTTPS VPS thật; tablet820×1180dp3/3 và393×852dp fontscale1.5 3/3 đạt. Trailer thật xGiimAKN0TM state1 khi chạm Play, state2 sau nền/resume và destroy khi đóng. Đã sửa sizing native gây phủ nút đóng và iframe SDK giữ200px trên tablet: setSize/height theo viewport khi ready; không thêm dependency/JS bridge/SSL bypass. Ảnh native đã lưu ngoài repo; chưa kiểm tra thiết bị vật lý. APK debug f1e30d2939be1125deb2f2b8c5e89c8f79ee625d4c8bb1473c753c7b122bdfc5 tại bộ verification cũ/CATALOG_PREVIEW.apk.
- Source tính năng đã push feat/catalog-showtimes-preview, SHA242f72dd13122db08d54f324c1071d806c72cdfe, Author/Committer Đào Mạnh Nhân <24521227@gm.uit.edu.vn>, không bot/co-author/main/rewriting. Bản fix tablet/tests và tiến độ cuối đang được đưa vào commit kế tiếp cùng cả hai MD; không bịa SHA trước commit. PR chưa tạo/merge.
- VPS đã build/manifest39 file/native Argon2/sharp/Chromium và deploy catalog-preview-1a04af45f85fa0a8; source identity1a04af45f85fa0a848f2425c21be58f7171016ef7b0e19bc9efa436dff946d32, archive0050a9943683b74e8e81c558a7620b8917cccc4e0fa92b101836cbc2a06547db. Public backend SHA242f72d chỉ khác đúng4 file proxy overlay hiện hành (config/service/smoke/crawler tests). API healthy/start10:41:05Z, health200/Swagger200, hostname/port/cron/proxy đúng; private3 env hash, PostgreSQL/Nginx ID/StartedAt giữ nguyên.
- Backup trước migration ban đầu dừng với Invalid deployment identity; DB còn107, API cũ không đổi. Phát hiện timer10–12/10 cũng lỗi do regex cũ không nhận crawler-dates. Chỉ sửa regex nhận thêm crawler-dates/catalog-preview trong backup.sh ngoài repo; giữ original backup.pre-preview.sh/hashff06a4ac44ecaefaba6e3781226b7a2d9cda069b721cebf292332ca3df173086. Cùng fixture label BASELINEexit1/MODIFIEDexit0/ROLLBACK copyexit1, hashcopy khớp; backup thật sau sửa đạt. Giữ pg_dump-Fc/list/hash/lock/retention72h và chỉ prune sau backup mới thành công, không restore.
- Backup pre-migration10:40:58Z SHA2b7be5ebf53afd2839d96c7d0586e4f7348671d89ff62c15ff887cae3e2cb6eb đạt rồi n8_migrator migrate deploy20261013000000_catalog_preview thành công:10 bảng/109 cột, users hash57c466fa2ca099356939bbd5e331a106/inventory hash14a81fbd119fff116ed2de24cc8e681f không đổi khi migrate. Không reset/drop/TRUNCATE/restore DB.
- Crawler thật qua proxy batchf5626f8e-8c9e-484c-90d1-d1612e1ed67a13/10 10:43:47–10:46:03Z: parse đủ nguồn13–18,19 trống thì dừng; skipped0, tạo12 suất/1660 ghế. Status PARTIAL/exit2 vì2 sourceKey cấu trúc đổi (13:15149585,14:15163316); giữ nguyên suất/inventory theo rule, không sửa để ép SUCCESS. Metadata trailer/nhãn đã cập nhật; không coi PARTIAL là thành công hoàn toàn của crawler.
- Audit read-only sau backfill:18 movies/344 showtimes/62015 seats,16 phim có trailer/197 suất có nhãn; users hash giữ nguyên. Sáu ngày API đều chỉ suất ACTIVE tương lai, phim không rỗng; list/detail và counter AVAILABLE/tổng khớp DB. Snapshot10:52 ngày13 có6 phim/19 suất/6 trailer,14 có12/44/11; past empty, date sai400; proxy HTTPS CGV200. Số suất tiếp tục giảm theo giờ, không ép count hiện tại bằng snapshot cũ.
- Sau health/Swagger/catalog/proxy đạt đã dùng khóa deploy-cleanup để dọn builder/image không dùng: giữ3 image đang chạy, thu hồi4.813GB cache +4.482GB image, còn22GB. API identity/StartedAt, env, volumes và dịch vụ khác không đổi; backup10:59:04Z trên release mới/hash2bb5d888841d596b8dd582976909fee19c1ddd42c431bcce7a7a9abc131ecaea đạt. Không prune volume/network hoặc reboot.
- BASELINE/MODIFIED/ROLLBACK cùng preview-check.mjs và input đã ghi ở mục trước, đềuexit0; rollback nguồn trên bản sao159 bytes/hash pristine, không restore DB. Cùng4 role ngoài repo được refresh/reopen/kiểm tra patch với source cuối, giữ mọi mode/baseline cũ.

Giới hạn đã chốt: số ghế là N8 không phải CGV; một số metadata nullable hoặc suất source-conflict giữ bản N8 cũ, không bịa nhãn/trailer. Video có thể cấm nhúng/lỗi mạng; hiển thị lỗi và cho mở YouTube khi người dùng chọn. Chưa đặt vé thật, chưa máy vật lý, không suy diễn các tuần booking/payment đã hoàn thành.


#### Công bố bản cuối catalog preview — 2026-10-13

Đã commit/push bản fix tablet và tiến độ đồng bộ trong cả hai MD:99e97acb46d27a661862e9b51c55830e94dbf103, remote branch feat/catalog-showtimes-preview khớp SHA, Author/Committer vẫn Đào Mạnh Nhân <24521227@gm.uit.edu.vn>. Đây là kết quả thay mốc đang đưa vào commit phía trên. Backend bytes không đổi so SHA242f72d nên không rebuild/recreate/migrate VPS lần nữa chỉ vì đổi Android/tài liệu. Native15/15 cuối qua VPS, tablet3/3, font1.5 3/3 và APK hash đã ghi phía trên. Cả hai MD đọc lại; bốn role/patch/rollback được chốt với source cuối và giữ nguyên baseline/lịch sử. PR chưa tạo/merge; không thay main.

### Tinh gọn giờ chiếu và chú thích Home — 2026-10-13

Trạng thái: **Hoàn thành local/Android**. Theo yêu cầu mới, thay quy tắc hiển thị suất qua nửa đêm có (+1 ngày) bằng chỉ giờ bắt đầu–kết thúc (ví dụ 23:00–01:00), áp dụng thẻ suất Home/chi tiết, nhãn trợ năng và dialog thông tin qua showtimeRange. Xóa dòng chú thích cuối danh sách Home “Lịch chiếu từ CGV · Phòng và ghế mô phỏng N8. Số ghế tại lần cập nhật API.” Không đổi định dạng HH:mm hiện hành, timestamp/ngày thực, lọc suất đã bắt đầu, API/database hay nguồn inventory N8; giữ thông báo chưa hỗ trợ đặt vé trong dialog. Không thêm dependency. Đã cập nhật test cùng ngày/qua nửa đêm và UI/nhãn trợ năng/không còn chú thích. BASELINE probe thực thi Kotlin với input 23:00–01:00: RANGE=23:00–01:00 (+1 ngày), CAPTION=true, exit0. Đã kiểm tra: Android34/34 unit, assembleDebug/assembleDebugAndroidTest và lint đạt (0 lỗi/43 cảnh báo); native4/4 trên giả lập360×800dp gồm test suất qua nửa đêm/nhãn trợ năng/không chú thích, ẩn suất đúng giờ và2 test ngày. Ảnh giao diện đã xem, chỉ có giờ bắt đầu–kết thúc và không còn dòng chú thích. MODIFIED cùng input: RANGE=23:00–01:00, CAPTION=false, exit0. ROLLBACK.sh --ui-text chạy trên bản sao riêng: trả lại RANGE=23:00–01:00 (+1 ngày), CAPTION=true, exit0;165 file khớp hash pristine, workspace vẫn giữ bản sửa. Cùng4 role MODIFIED_FILE.zip/DIFF_FILE.patch/VERIFICATION.txt/ROLLBACK.sh ngoài repo được mở lại; baseline và mode rollback cũ giữ nguyên. APK debug mới CATALOG_PREVIEW_CLEAN_UI.apk SHA256c77d16d0525d92f436219bdf4627dba36e4296d0585bed6e7a64c6deb013dee2, giữ APK cũ. Đã đối chiếu cả hai tài liệu. Chưa commit/push, chưa thử thiết bị vật lý; không cần deploy VPS vì không đổi backend/API/database. Không kiểm tra lại trailer/payment ngoài phạm vi sửa giao diện này.


#### Công bố bản tinh gọn giao diện — 2026-10-13

Trạng thái: **Hoàn thành commit/push source**. Branch feat/catalog-showtimes-preview, commit fe6eafc6b230aa50051888ab9298cff3c3cb423a, push exit0 và git ls-remote xác nhận SHA remote khớp. Commit gồm đúng7 file source/test/tài liệu, cả Plan và AGENTS cùng đợt; không main/co-author/bot/rewrite. Author/Committer đã kiểm tra: Đào Mạnh Nhân <24521227@gm.uit.edu.vn>. Kết quả34 unit/4 UI, build/lint và BASELINE/MODIFIED/ROLLBACK giữ nguyên vì chỉ công bố source đã kiểm chứng; không chạy lại build/native. Bản ghi xác nhận này đưa vào commit tài liệu kế tiếp cùng cả hai MD, không bịa SHA trước commit. Cùng4 role ngoài repo được refresh và mở lại, giữ baseline/mode rollback. Không deploy VPS vì backend/database không đổi; chưa kiểm tra máy vật lý. PR chưa tạo/merge.

### UI chi tiết phim và chọn lịch chiếu — 2026-10-17

Trạng thái **Hoàn thành phần source/build/unit; Bị chặn kiểm tra emulator**. Android Compose đã nối Dashboard khách hàng → chi tiết phim → lịch chiếu → sheet phân loại tuổi → tóm tắt suất; chưa có chọn ghế/đặt vé thật. Phim lấy từ lựa chọn Home; màn lịch dùng dữ liệu mẫu tập trung `DetailSchedule` vì API chưa hỗ trợ khu vực/nhiều rạp. Bộ lọc/ngày xóa suất không còn hợp lệ; không thể tiếp tục với suất hết vé; nhánh chưa đủ tuổi không mở tóm tắt. T16 và nội dung cảnh báo sinh từ `movie.age`, không rải hằng số trong UI. Dune có ảnh, trailer và rating/ngày phát hành mẫu tập trung; nguồn ảnh ghi trong Android raw JSON từ thiết kế. Giữ theme/navigation/TrailerDialog, safe insets và bottom bar Compose. Không đổi backend/API/DB nên không có deploy VPS.

Kiểm tra: Gradle Wrapper `assembleDebug testDebugUnitTest lintDebug` đạt, 35/35 unit, lint 0 lỗi/44 cảnh báo/2 hints. Sau sửa cuối đã chạy lại `assembleDebug testDebugUnitTest` đạt. Emulator khởi động nhưng ADB bị sandbox từ chối tạo `\\.android`, chưa chụp màn hình hay kiểm thử tương tác; không ghi visual đã đạt. Chưa commit/push. Cần kiểm tra emulator khi ADB hoạt động và cập nhật bằng chứng đồng thời ở Plan/AGENTS. Không sửa Login/Signup/Staff dashboard trong đợt này.


### Catalog/showtime và tích hợp lịch chiếu tuần 6 — 2026-10-17

Trạng thái: **Đang làm**. Theo phạm vi đã được xác nhận: bổ sung GET /api/v1/showtimes/:id và /showtimes/:id/seats; nối màn lịch chiếu Android vào API thật, chỉ Landmark 81, giữ bố cục của Nhân. Tái sử dụng format/ageRating và thuật toán phân phòng hiện có; không thêm bảng/migration/dependency, không làm giữ ghế/đơn/thanh toán. Backend local giữ localhost; VPS giữ overlay proxy ngoài repo. Đã lưu PRE_WEEK6_BASELINE.zip ngoài repo, SHA256 390131264373894706e159bbc1fbeca3c9cfb8de0601e67ca91a25f3b316be57; typecheck và 17/17 catalog tests baseline đạt exit0. Còn source/tests mới, kiểm chứng Android, rollback bản sao, triển khai/nghiệm thu VPS và cập nhật kết quả. Giữ bộ bốn role hiện hành tại D:\NT118\verification\week5-crawler-20261010, bổ sung baseline/mode --week6 mà không thay các mốc cũ.


### Kết quả hoàn thiện catalog/showtime tuần 6 — 2026-10-17

Trạng thái: **Hoàn thành local/VPS/Android trong phạm vi đã chốt**, thay mốc Đang làm catalog/showtime tuần6; xem hợp đồng/API và kết quả chi tiết trong Plan. Chỉ Landmark81; giữ bố cục của Nhân, dùng lịch/phòng/ghế/format/ageRating thật, không nhiều rạp hoặc dữ liệu lịch mẫu. GET /showtimes/:id trả {serverTime,item}; /showtimes/:id/seats trả {serverTime,showtimeId,items} và chỉ7 trường ghế công khai, không heldByUserId/holdExpiresAt. UUID sai400, unknown404; suất hủy/quá giờ vẫn đọc được200 nhưng không được tiếp tục. API đọc không giữ ghế/sửa inventory, không thêm bảng/migration/dependency. Giữ thuật toán phân phòng/transaction/advisory lock hiện có, kiểm thử đủ6 format/seed/inactive/extra/boundary/idempotency.

Android selectedShowtime thay SampleSlot; dùng chung ngày, trạng thái tải/lỗi/trống rõ, kiểm tra chi tiết suất trước tóm tắt, chặn hết ghế/hủy/quá giờ/thiếu inventory, cancel+epoch cho đổi ngày/back/logout. Qua ngày bỏ suất cũ và rời tóm tắt không hợp lệ; font lớn grid responsive, nhãn trợ năng back/suất. Tuổi từ movie.age, chỉ xác nhận tự khai. Không bịa điểm đánh giá/ngày/trailer.

Đã kiểm tra: local backend build/typecheck/format61/61 tests; API local127.0.0.1:5000 qua DB tunnel khớp VPS và inventory, đã dừng process/tunnel của phiên. Android41/41 unit, build APK/AndroidTest/lint0 lỗi/44 cảnh báo/2 hints; bản cuối phone21/21 (gồm catalog HTTPS VPS thật, Auth/date/preview/trailer), tablet3/3, font1.5 3/3, ảnh đã xem. APK WEEK6_CATALOG.apk SHA256 6acb8705ec34bf7cab86638d0b4d637ed3a637bdebbca4c5a519cdb7eb8a7036 ở bộ verification hiện hành. Chưa máy vật lý.

VPS release catalog-preview-780c6b76031e7acc, source identity 780c6b76031e7acce9ea401dbf0e7e9c5543576f1723116e0d0f7fbaf9409445, manifest42 file, giữ đúng4 overlay proxy ngoài repo. 64/64 tests thật trong tools container Linux network-none đạt, native modules/Chromium đạt; API healthy/start07:04:53.111260125Z ngày17/10, health200/database up, Swagger/OpenAPI mới đạt. DB10 bảng/109 cột, overlap/sai capability/thiếu inventory/trùng ghế đều0; users/inventory hash qua triển khai/nghiệm thu không đổi. Không migrate/reset/restore/backfill. Proxy Playwright CGV200 lúc07:07:15.266Z; env/PG/Nginx/mount không đổi. Backup timer và snap.certbot.renew.timer active, identity catalog-preview phù hợp backup.sh. Sau nghiệm thu đã dọn đúng cache/image không dùng theo khóa, giữ3 image đang chạy, còn22GB; không volume/network prune hay reboot.

Cùng probe UUID: BASELINE404 → MODIFIED200 (IMAX_2D,2/3 ghế,A1/A2/A10) → ROLLBACK copy404, exit0;138 file pristine/hash phục hồi, restored59/59 tests đạt với env fixture. Bốn role giữ tại D:\NT118\verification\week5-crawler-20261010, thêm PRE_WEEK6_BASELINE.zip/--week6/patch bổ sung, giữ toàn bộ baseline/mode cũ; source chính vẫn bản mới. Lỗi tool/fixture/emulator và các lần rerun được giữ ngoài repo, chi tiết trong Plan. Đã đồng bộ hai MD, không raw log/secrets/selftest tạm trong repo. Bước tiếp theo: chạy thử APK; giữ ghế/đơn/payment, VIP/couple, nhiều rạp và xác minh tuổi thật ngoài phạm vi. Source rollback không restore DB và không lấy lại image/cache đã dọn.


### Công bố phần tuần 6 trên nhánh riêng — 2026-10-17

Trạng thái: **Đang làm phần công bố**, không thay đổi kết quả hoàn thành local/VPS/Android ở trên. Người dùng đã yêu cầu tạo commit và push. GitHub API /user xác nhận tài khoản xác thực thnguyen290106; email thnguyen290106@gmail.com đã liên kết và verified. Repository dùng Author/Committer Trần Thanh Nguyên <thnguyen290106@gmail.com> cho các commit mới, không co-author/bot. Nhánh feat/nguyen-week6-catalog-showtimes tách từ main bcc666d6d5867b207d136fc8a1b17f1af78dc8a2; không sửa lịch sử hoặc đẩy lên main. Chỉ công bố 22 file của thay đổi tuần 6, không đưa secrets, log, APK hoặc selftest tạm vào repository.

Các source backend hiện tại đã đối chiếu byte/hash với manifest public của release VPS catalog-preview-780c6b76031e7acc đã nghiệm thu; bước công bố không thay đổi runtime, schema hoặc cấu hình VPS. Bằng chứng Git/định danh lưu ngoài repository tại bộ verification hiện hành; chưa ghi SHA/kết quả push trước khi thực thi. Bước tiếp theo: kiểm tra staged diff, tạo commit đúng danh tính, push nhánh, đối chiếu SHA remote và tài khoản tác giả trên GitHub rồi cập nhật cả hai tài liệu.


### Kết quả công bố source tuần 6 — 2026-10-17

Trạng thái: **Hoàn thành công bố source trên nhánh riêng**, thay mốc Đang làm phần công bố phía trên; giữ nguyên kết quả kiểm chứng local/VPS/Android đã ghi. Source commit `35f093d09766af3f78f0195bc1f69bf4b2c18c3f` đã push thành công lên `feat/nguyen-week6-catalog-showtimes`: https://github.com/Tonyisded/N8_Cineplex-Project/commit/35f093d09766af3f78f0195bc1f69bf4b2c18c3f. Git ls-remote xác nhận SHA remote khớp local; GitHub API xác nhận cả author.login và committer.login là `thnguyen290106`, Author/Committer Trần Thanh Nguyên <thnguyen290106@gmail.com>. Không có co-author/bot, không sửa contributor/lịch sử cũ, không push/merge main hoặc tạo PR thay nhóm trưởng.

Đã kiểm tra staged diff sạch lỗi whitespace và chỉ đúng22 file; không secrets, file build, selftest hoặc log tạm. Backend công bố khớp manifest public đã nghiệm thu của VPS catalog-preview-780c6b76031e7acc; bốn file proxy overlay môi trường vẫn ở ngoài repo. Lượt công bố không đổi code runtime hoặc schema so với bản đã triển khai, không build/deploy lại, không chạy lại toàn bộ Android/VPS tests, không thay dữ liệu DB. Các kết quả61/64 backend,41 unit,21/3/3 native và BASELINE404/MODIFIED200/ROLLBACK404 là bằng chứng thực thi đã ghi trong mốc hoàn thiện phía trên, không nhận là lượt chạy mới của thao tác Git.

Cả hai tài liệu được cập nhật cùng đợt. Bằng chứng đầy đủ và SHA của bản cập nhật tài liệu tiếp theo được lưu ngoài repo trong cùng bộ verification; giữ nguyên bốn role/baseline hiện hành. Bước tiếp theo: nhóm trưởng review nhánh và quyết định merge; chưa thực hiện merge.

### Nhật ký UI chọn ghế Android — 2026-10-25

| Ngày | Hạng mục | Trạng thái | Kết quả / phần còn lại |
| --- | --- | --- | --- |
| 2026-10-25 | UI chọn ghế native từ design/seat-selection | Hoàn thành phần Android mô phỏng; Chưa làm backend hold/realtime/order/payment thật | Nối sau lịch chiếu và xác nhận tuổi bằng đúng suất đã chọn; Compose có sơ đồ thường/VIP/Couple, trạng thái, zoom/cuộn, thanh tổng kết, deadline 600 giây, offline/reconnect mô phỏng, cảnh báo và dialog rời/hết hạn, điểm chuyển tiếp tóm tắt. Gradle assembleDebug/assembleDebugAndroidTest/testDebugUnitTest đạt, 43/43 unit; lint source ứng dụng 0 lỗi/44 cảnh báo/3 hints (lint đầy đủ phân tích test bị treo); emulator Pixel_10_Pro_XL 4/4 bài UI đạt (chọn ghế, khách hàng/back/logout, Signup, catalog), ảnh sơ đồ Couple đã xem. Chưa máy vật lý, chưa commit/push/merge. |

Quy tắc tiếp tục: SeatDemo và SeatSelectionViewModel chỉ là mô phỏng cục bộ, không phải giữ ghế hoặc realtime nhiều người dùng. Backend hiện chỉ có GET /api/v1/showtimes/:id/seats đọc inventory và chưa có zoning VIP/Couple hay API hold/order; không dùng sơ đồ demo để khẳng định tồn kho thật. Công cụ tình huống SeatScenarioActivity chỉ trong source set debug, khởi chạy bằng adb shell am start -n com.nhom8.cineplex/.SeatScenarioActivity, không đưa lên màn khách hàng. Khi làm booking thật, đọc lại mục seat hold/transaction/WebSocket trong Plan, giữ 10 bảng và emit sau commit. Lượt này chỉ sửa Android/tài liệu, không deploy VPS. Bước tiếp theo là backend hold/order/realtime theo plan rồi thay nguồn mô phỏng; chưa triển khai thanh toán.

### Nhật ký sửa ghế trống và điều hướng Home — 2026-10-25

| Ngày | Hạng mục | Trạng thái | Kết quả / phần còn lại |
| --- | --- | --- | --- |
| 2026-10-25 | Sửa sơ đồ ghế ban đầu và thao tác chạm giờ chiếu Home | Hoàn thành Android mô phỏng/local/emulator; Chưa làm giữ ghế thật | Thay trạng thái HELD/SOLD đặt sẵn: 54 thực thể/60 chỗ SeatDemo ban đầu đều AVAILABLE, tình huống bị giữ/bán chỉ kích hoạt qua công cụ debug. Chạm giờ Home xác minh lại GET showtimes/:id rồi vào chọn ghế, Back về Home; suất không hợp lệ/lỗi vẫn ở Home. Luồng chi tiết/lịch/tuổi không đổi. Gradle build app + AndroidTest và 45/45 unit đạt; lint source 0 lỗi/44 cảnh báo/3 hints, lint đầy đủ phân tích test chưa đạt vì bị treo; emulator 3/3 bài UI liên quan đạt, ảnh sơ đồ ghế trống đã xem. Sơ đồ 60 chỗ vẫn là mẫu, chưa khớp phòng GOLD 32 chỗ/tồn kho thật; chưa commit/push/merge. |

Quy tắc tiếp tục: không thêm ghế HELD/SOLD mặc định cho suất mới trong SeatDemo; chỉ đưa trạng thái bận vào khi có dữ liệu server đúng ghế hoặc công cụ debug. Không gọi thao tác Home là giữ ghế thật. Khi triển khai inventory thực, sơ đồ phải theo room/seat API và trạng thái source of truth, không dùng số ghế 60 mẫu để suy ra 32/32 của phòng GOLD. Lượt này chỉ sửa Android/tài liệu, không sửa backend/VPS.


### Chọn ghế thật, bốn loại phòng và pinch zoom — 2026-10-25

Trạng thái: **Đang làm** trên nhánh `feat/nguyen-seat-selection-live`, theo plan đã được người dùng duyệt. Thay phạm vi SeatDemo/giữ ghế/realtime cục bộ của hai mốc Android cùng ngày. Không nhận kết quả mô phỏng cũ là kiểm chứng booking thật.

- Android nền đen cố định; Standard/IMAX/ScreenX/Gold có template riêng, số ghế đúng capacity N8. Ghế SOLD có dấu X; HELD/BLOCKED không chọn; Couple đủ cặp. Gold chọn độc lập, không ép cặp. Có pinch/pan/double-tap, fit, nút zoom và minimap; không đổi nền theo phim hoặc thêm FanC/ưu đãi giả.
- Chọn ghế là draft; chỉ bấm ĐẶT VÉ và server commit thành công mới giữ toàn nhóm 600 giây. Mở sơ đồ không chạy countdown. Deadline server không reset khi đổi màn/resume/khởi động app. Quay lại từ thông tin giữ chỗ vẫn giữ đơn; muốn đổi nhóm phải hủy giữ rõ ràng. Hủy lỗi mạng không được thông báo đã nhả.
- API POST /orders, GET /orders/:id, POST /orders/:id/cancel; GET seats thêm layout, giữ các trường ghế công khai cũ. JWT/CUSTOMER/email verified/ownership; backend tính giá; UUID Idempotency-Key dùng orders.id, retry trả cùng đơn/hạn, payload khác bị từ chối. Transaction/state guard/row lock; cron hết hạn mỗi giây và quét khi startup; chỉ emit sau commit SEAT_HELD/SEAT_RELEASED/ORDER_EXPIRED. REST refetch khi reconnect và poll 10 giây foreground, không coi socket là source of truth.
- Quyết định phạm vi mới được người dùng xác nhận: đợt này chỉ giữ ghế, chưa thanh toán/SePay/phát vé/combo/ưu đãi/credit. Orders mới có combo_id=NULL, combo_quantity=0, discount=0, credit_used=0, không trừ users.credit_balance. Đây là ngoại lệ theo giai đoạn thay quy tắc tự dùng credit khi tạo đơn tại mục Pricing/Refund Credit cho endpoint booking đợt này; không tuyên bố luồng thanh toán/credit 0đ đã hoàn thành. Giá từng chỗ từ server, Couple cộng hai chỗ; chưa thêm phụ thu VIP/Couple.
- Template code, không thêm bảng/cột/JSONB/migration. Hàng bỏ I, số ghế giảm từ trái sang phải. Standard: ba hàng đầu NORMAL, giữa VIP, cuối COUPLE; 160:[12,14,14,14,15,15,15,15,15,15,16],145:[13,14,14,14,14,14,14,16,16,16],130:[12,14,14,14,14,14,16,16,16],112:[12,12,14,14,14,14,16,16]. IMAX496:[18,22,26,30,32,34,36,36,38,38,38,38,38,36,36], toàn VIP/ba khối, giữa14. ScreenX180:10x18, ba khối6/6/6, ba hàng đầu NORMAL/sáu VIP/cuối COUPLE, biểu tượng màn trước/hai bên. Gold32:4x8 GOLD, bốn cụm hai nhưng chọn độc lập. Phòng extra cùng type/capacity tái dùng template. Không gọi đây là sơ đồ chính thức CGV.
- Inventory tương lai cũ chỉ chuyển sau backup khi toàn AVAILABLE và chưa có order history; transaction/ledger ngoài repo, giữ ID theo mã trùng. Không đổi/xóa inventory đã tham chiếu; fallback render đúng inventory legacy, không sinh giả theo capacity. Không backfill nếu chưa chứng minh điều kiện. Không reset/restore DB chung; fixture tự động rollback, chưa cho phép fixture booking commit trên DB chung.
- Local giữ localhost; VPS triển khai đúng SHA, giữ proxy overlay crawler ngoài repo và HTTPS443; Socket.IO /socket.io/ chung server/Nginx Upgrade. Không thêm Redis/BullMQ. Chỉ dọn image/cache cũ sau kiểm chứng đạt; rollback code không restore DB, phải drain hold trước hạ phiên bản thiếu expiry.

Tiến độ ban đầu: đã lưu PRE_LIVE_SEATS_BASELINE.zip bất biến ngoài repo và tạo nhánh riêng; đang triển khai source/tests. Chưa backfill, chưa commit/push/deploy/nghiệm thu VPS. Còn kiểm chứng local/emulator/rollback và VPS; không ghi hoàn thành trước bằng chứng. Giữ bộ MODIFIED_FILE.zip/DIFF_FILE.patch/VERIFICATION.txt/ROLLBACK.sh hiện có, bổ sung mode --live-seats, không thay lịch sử cũ. Người dùng đã yêu cầu commit/push nhánh riêng sau thực hiện.


#### Chọn ghế thật — kiểm chứng local trước triển khai VPS (2026-10-25)

Trạng thái: **Hoàn thành source và kiểm chứng local; Đang làm VPS/công bố**. Backend build/typecheck/format và 69/69 tests đạt, gồm HTTP/JWT/DTO/ownership, Couple, retry, expiry/history và Socket.IO hai client cô lập user room. Bài cạnh tranh service dùng transaction giả tuần tự, không gọi là kiểm chứng concurrent commit PostgreSQL trên VPS. Android50/50 unit đạt; APK/AndroidTest build và lintDebug đầy đủ (bao gồm phân tích unit/AndroidTest, không loại task) đạt 0 lỗi/44 cảnh báo/3 hints. Emulator Pixel10ProXL19/19 native đạt, gồm pinch/pan/double-tap, tap toàn cảnh phóng trước/chọn sau, bốn loại phòng, Couple/countdown sau xác nhận, chi tiết/tuổi/Home/back và Auth UI regression. Đã xem ảnh Standard/Gold/zoom; các ảnh này dùng fixture test, chưa nhận là ảnh inventory VPS mới.

- Local API127.0.0.1:5000 chạy source mới qua SSH tunnel DB chung: health200 database up, Swagger có Orders, đọc GOLD32/layout v1 đúng inventory, POST orders không JWT bị401. Không ghi dữ liệu thử; đã dừng đúng process API/tunnel của phiên. Không đổi private .env hoặc default localhost.
- Bổ sung khôi phục giữ chỗ từ Home bằng record lưu riêng theo user/suất và GET order có ownership, kể cả khi catalog hết ghế nên suất bị ẩn. Không cần ID ghép làm UUID backend; không tăng hạn khi mở lại. Bài unit kiểm tra resume khi availableSeats=0 đã đạt. Rời view/logout ngắt socket và xóa hiển thị riêng, không giả vờ nhả server khi mạng lỗi.
- Lỗi native ban đầu: pinch tạo tap ngoài ý muốn và test click từ overview kỳ vọng chọn ngay; đã thay hai detector tranh nhau bằng một vòng gesture, hủy tap khi multi-touch/drag, trì hoãn tap đơn để nhận double-tap, thêm kiểm tra tap vật lý; lượt19/19 sau sửa đạt. Giữ kết quả lỗi trong ledger ngoài repo.
- Audit production ban đầu báo6 lỗ hổng từ dependency có sẵn, không phải Socket.IO. Đã chọn bản cùng-major tương thích: override body-parser2.4.0/multer2.5.0/fast-uri3.1.9 và cập nhật Nodemailer trong range hiện có; không hạ NestJS. npm audit --omit=dev sau sửa:0 lỗ hổng. Socket.IO server4.8.4, Nest gateway/adapter12.1.2; Java client Android2.1.2. socket.io-client Node chỉ là devDependency cho test.
- Cùng probe HTTP/input IMAX496-all-AVAILABLE-price150000: BASELINE200/496 NORMAL/16 hàng/layout null → MODIFIED200/496 VIP/15 hàng/layout IMAX-496-v1 → ROLLBACK copy200/496 NORMAL/16 hàng/layout null, cả ba exit0. ROLLBACK.sh --live-seats chạy thật, byte nguồn gốc được xác minh; source chính vẫn GOAL mới. Giữ PRE_LIVE_SEATS_BASELINE.zip bất biến SHA256 de7eb3708ec18f25a698ea05a6f04d7c82cc291396b30dcf6f3cf9af3ea2a9a5 và mở rộng đủ4 role/patch có thể tái tạo byte, không restore DB.
- VPS mới chỉ preflight đọc-only: API cũ healthy, DB10 bảng/không có order,67 suất tương lai đủ điều kiện backfill tại snapshot. Chưa backup/backfill/deploy mới ở mốc này. Bộ fixture booking commit để nghiệm thu hai client/đủ600 giây đang hỏi chấp thuận riêng; khi chưa có trả lời chỉ test DB rollback, không tự tạo. Chưa có thiết bị vật lý.

Bước tiếp: commit/push nhánh riêng bằng thnguyen290106/Trần Thanh Nguyên, triển khai đúng SHA và giữ4 proxy overlay ngoài repo; chạy tests Linux/DB rollback, backup/backfill có guard, health/Swagger/realtime/VPS UI đọc-only, cập nhật kết quả thực tế và commit/push cả hai tài liệu lần cuối. Không tuyên bố các bước này đã thành công trước thực thi.


#### Kiểm chứng triển khai và tinh chỉnh mốc giữ — 2026-10-25

Trạng thái: **VPS bản đầu đã triển khai/kiểm chứng; Đang làm tinh chỉnh deadline và nghiệm thu cuối**. Commit66a4cd1fbbb98db690650cee5bf1919bde49eeef đã push nhánh riêng, VPS checkout đúng SHA, giữ4 proxy overlay. Build native/Chromium và72/72 tests Linux thật đạt; backup trước backfill hash0d82b3ca70105540f96462c770a5f4540b03746ccf611769e0a273cfad92c38e. PostgreSQL service/savepoint integration đã chạy với API DML role trong transaction ngoài, rollback toàn bộ fixture và kiểm tra0 bản ghi còn lại; đây không phải E2E commit nhiều client. Backfill51 suất,11 suất Gold không đổi,0 bị bỏ qua tại snapshot; giữ ID ghế có mã trùng, bảo toàn users/orders/payments/tickets. API healthy, health/Swagger200;62 suất tương lai được đọc/đối chiếu capacity/layout v1, đủ4 loại phòng. Không schema migration/reset/restore; private env và container PostgreSQL/Nginx giữ nguyên. Chưa dọn image/cache cũ ở mốc này vì nghiệm thu cuối còn chạy.

Code review thêm: mốc hết hạn phải lấy thời gian DB tươi sau khi đã chờ lock/kiểm tra ghế, không tính thời gian chờ lock vào600 giây. Bổ sung truy vấn mốc giữ ngay trước tạo đơn và kiểm tra lại suất chưa bắt đầu, kèm regression mô phỏng DB time tiến30 giây trong lúc chờ. Bản sửa này đang kiểm thử/commit/triển khai đúng SHA mới; không nhận kết quả bản66a là kết quả của bản sửa chưa deploy. Android source giữ nguyên so66a; thêm kiểm chứng font1.5:3/3 native đạt và đã phục hồi font thiết bị. E2E booking commit/đủ600 giây vẫn chờ chấp thuận riêng; chưa có thiết bị vật lý.


#### Chọn ghế thật — kết quả cuối đã quan sát (2026-10-25)

Trạng thái: **Hoàn thành source, công bố nhánh riêng, triển khai VPS và các kiểm chứng dưới đây; Chưa hoàn thành nghiệm thu E2E booking commit/đủ600 giây và thiết bị vật lý.** Mốc này thay các trạng thái Đang làm source/VPS phía trên, không tự nâng phần E2E chưa được phép thành hoàn thành. Tính năng payment/SePay/combo/ưu đãi/credit/phát vé vẫn ngoài phạm vi đã chốt, không có nút thanh toán hoặc vé giả.

- Source đã commit/push `feat/nguyen-seat-selection-live`:66a4cd1fbbb98db690650cee5bf1919bde49eeef (UI/backend),2f2547be285607f8c77d774862cfe090db71efae (deadline sau lock). Author/Committer Trần Thanh Nguyên <thnguyen290106@gmail.com>; GitHub đã xác nhận thnguyen290106 ở commit source, không co-author/bot, không push/merge main hoặc tạo PR. Hai MD cùng mỗi đợt source. Lượt chốt tiến độ này chỉ sửa hai MD, không đổi runtime và không rebuild/deploy lại API; SHA/kết quả push tài liệu được lưu trong cùng hồ sơ kiểm chứng ngoài repo sau thực thi, không bịa SHA tự tham chiếu trước commit.
- Backend local: build/typecheck/Prettier70/70 tests đạt; Android50/50 unit, APK/AndroidTest build và lintDebug đầy đủ đạt0 errors/44 warnings/3 hints. Local127.0.0.1:5000 qua tunnel DB chung health200/database up, Swagger Orders, đọc GOLD32/layout thật và POST khôngJWT401 đã đạt; process API/tunnel của phiên đã dừng. Source/private env vẫn localhost, không đổi IP bind thành VPS.
- Android emulator:19/19 native regression (6 seat/gesture,3 detail/age,1 Home/catalog,9 Auth/customer UI); font1.5 thêm3/3 đạt và đã phục hồi font1.0. Sau deploy cuối,4/4 SeatServerUiTest đọc HTTPS inventory/layout VPS thật đạt (không POST booking); đã xem ảnh cả4 loại, Gold bốn cụm/32 chỗ, IMAX496 hình mở rộng/hai lối đi, ScreenX180 ba khối/hai màn bên, Standard zoning/Couple. Pinch/pan/double-tap không chọn nhầm, tap overview phóng trước/chọn sau; chưa máy vật lý.
- Backend VPS: checkout/image đúng2f2547be285607f8c77d774862cfe090db71efae,52 file runtime khớp manifest, giữ4 proxy overlay ngoài repo. Native modules/Chromium và73/73 tests thật trong container Linux network-none đạt (70 public +3 proxy); PostgreSQL BookingService/savepoint integration bằng API DML role đạt, outer transaction rollback và kiểm tra0 fixture còn lại. Đó là kiểm tra SQL/service một connection, không phải bằng chứng concurrent commit hai client trên production. Service lấy thời gian DB mới sau lock/validation và kiểm tra lại suất chưa bắt đầu trước đặt expires_at+600s; regression mô phỏng chờ30s đạt. Khóa ngoài theo showtime, users theo UUID tăng dần, ghế theo UUID tăng dần; command tương lai phải giữ thứ tự khóa tương thích.
- Backup trước backfill đầu hash0d82b3ca70105540f96462c770a5f4540b03746ccf611769e0a273cfad92c38e; backup trước bản cuối hashfa2824fc0b02e05c749cce2a9c8131b5a06d1c2dd941090f98780bb6eb0dfcaf, catalog/hash hợp lệ. Backfill đầu51 suất thay/11 Gold không đổi/0 skip; lần cuối0 thay/60 không đổi tại snapshot. Chỉ ACTIVE tương lai, toàn AVAILABLE, không history; ID mã trùng/giá cũ giữ nguyên, ledger ngoài repo. Số suất giảm theo giờ, không ép snapshot62 thành60. Users/orders/payments/tickets không đổi qua backfill; không có fixture booking commit.
- VPS health/Swagger200, API healthy/không lỗi startup; inventory/layout đối chiếu toàn60 suất tương lai đủ4 loại/capacity;10 bảng/109 cột,0 order/0 HELD/0 trùng mã tại kiểm tra cuối. Socket.IO HTTPS WebSocket upgrade hoạt động, JWT sai bị AUTH_REQUIRED, giữ kiểm traTLS; user-room isolation với hai client đã kiểm tra trong test server, chưa nhận là E2E production JWT nhiều người. Probe Playwright qua proxy CGV200 lúc2026-10-25T07:25:37.726Z; HTTP fetch trực tiếp không dùng proxy đã timeout ở lượt kiểm tra thừa trước đó và không được dùng làm bằng chứng crawler, đã sửa runner về đúng smoke qua proxy, không thay transport/config hoặc bypassTLS. PostgreSQL/Nginx container/mount/StartedAt và private env giữ nguyên; timer backup/chứng chỉ active.
- Dọn sau nghiệm thu theo cùng flock đã chạy thật: buildx cache và image không dùng được dọn, chỉ còn3 image đang chạy; API/PostgreSQL healthy, Nginx running, health/Swagger vẫn200, còn22GB. Không prune volume/network, không down-v, không reboot hoặc restore DB. Hai selftest tạm trên VPS đã xóa, source/baseline/ledger an toàn ngoài repo giữ lại. Muốn quay phiên bản image đã dọn phải build lại; trước hạ về code thiếu expiry cần drain hold, không tự restore DB.
- Cùng HTTP probe/input IMAX496-all-AVAILABLE-price150000: BASELINE200/496 NORMAL/16 hàng/layout null → MODIFIED200/496 VIP/15 hàng/layout IMAX-496-v1 → ROLLBACK copy200/496 NORMAL/16 hàng/layout null, exit0. ROLLBACK.sh --live-seats thực thi,223 file gốc khớp hash; restored61/61 tests đạt sau bổ sung env fixture thiếu DATABASE_URL rồi AVATAR_UPLOAD_DIR (hai lượt lỗi được giữ), source chính vẫn mới. MODIFIED_FILE.zip/DIFF_FILE.patch/VERIFICATION.txt/ROLLBACK.sh cùng bộ cũ đã mở lại và patch tái tạo byte đạt; immutable baseline SHAde7eb3708ec18f25a698ea05a6f04d7c82cc291396b30dcf6f3cf9af3ea2a9a5 không đổi. Lỗi collision tên BASELINE/baseline trên Windows đã sửa bằng manifest riêng, kiểm lại ZIP gốc, không thay baseline.
- APK kiểm tra ZIP/hash: `[artifact path omitted per user request]`,20224615 bytes,SHA256 5d2195aa4174227cb883a974219499277a8c44c73ed6ea7071dee82c481dfdfb. Android source không đổi giữa66a và2f; giữ APK/ảnh cũ. Audit production sau cập nhật tương thích0 lỗ hổng được báo cáo; không downgrade NestJS.

Phần còn lại/bước tiếp: chờ người dùng chấp thuận riêng fixture CUSTOMER/suất/ghế/order đã commit để chạy E2E hai client và chờ đủ600 giây rồi cleanup đúng UUID/ledger; hiện câu hỏi chưa có trả lời nên tuyệt đối chưa tạo fixture đó, chưa tuyên bố cron đã nhả một đơn production sau10 phút thật. Chưa kiểm tra multi-touch trên thiết bị vật lý. Có thể tiếp tục kiểm tra đọc-only/rollback mà không tạo dữ liệu commit. Không đánh dấu toàn bộ acceptance plan hoàn tất khi hai phần này chưa được quan sát.


#### Đồng hồ countdown bao gồm deep sleep — 2026-10-25

Trạng thái: **Đang kiểm chứng bản tinh chỉnh Android cuối**. Code review sau deploy phát hiện System.nanoTime là clock uptime không tính deep sleep trên Android; đổi riêng SeatSelectionViewModel sang SystemClock.elapsedRealtime để thời gian giữ vẫn trôi khi thiết bị ngủ, không phụ thuộc chỉnh giờ hệ thống. Deadline UTC vẫn do server quyết định và được tải lại khi resume/restart; không sửa backend/API/database hoặc gia hạn order. Chạy lại unit/build/lint và native gesture/countdown sau thay đổi; chưa tuyên bố kết quả bản sửa trước thực thi. Backend đã deploy SHA2f2547b giữ nguyên, không rebuild/deploy API chỉ vì sửa Android. Hai MD cập nhật cùng commit; E2E commit600s và máy vật lý vẫn chưa được nghiệm thu.


#### Chốt Android countdown/realtime response — 2026-10-25

Trạng thái: **Hoàn thành tinh chỉnh Android và kiểm chứng local/emulator; E2E commit600s/máy thật vẫn chưa làm**. Mốc này thay Đang kiểm chứng của mục deep sleep ngay phía trên. SeatSelectionViewModel dùng SystemClock.elapsedRealtime, clock nền tảng tính cả deep sleep theo [Android SystemClock](https://developer.android.com/reference/android/os/SystemClock); chưa nhận là đã thử deep sleep trên phần cứng thật. Không dùng giờ điện thoại để tự gia hạn; vẫn đối chiếu expiresAt/serverTime và REST khi resume.

Sửa thêm race thường gặp khi socket refetch nhận CANCELED trước response HTTP cancel: accept(response,expectedRequest) kiểm tra response theo đúng request đã gửi, bỏ response đã lỗi thời thay vì dựa vào request toàn cục vừa bị xóa. Không báo hủy thất bại sau khi server đã xác nhận, không làm sống lại deadline cũ. Regression ép refetch hoàn tất trước HTTP đạt. Bản chốt: Android51/51 unit, assembleDebug/AndroidTest và lintDebug đầy đủ đạt0 errors/44 warnings/3 hints; cài APK mới và chạy lại23/23 native đạt (19 UI/Auth/gesture/detail +4 đọc inventory VPS thật). Font1.5 3/3 trước đó vẫn giữ lịch sử, không nhận là lượt font mới của bản clock. Backend không đổi so2f2547b, VPS vẫn đúng bản đã nghiệm thu73 Linux tests; không rebuild API hoặc ghi dữ liệu test commit vì sửa Android.

APK bản chốt ở `[artifact path omitted per user request]`,20224996 bytes,SHA256 5afa281bf044b1170859f8883626d5bc3c228ce62ea381e0de3500098c4e7069, ZIP kiểm tra đạt. Thay APK cùng tên/hash5d2195... đã ghi ở mốc trước; bytes bản đó được giữ tại `CINEPLEX_LIVE_SEATS_PRE_SLEEP_CLOCK.apk`, không xóa artifact cũ. Bản chốt source Android/test/cả hai MD được công bố cùng đợt trên nhánh riêng; SHA và remote-match sẽ được ghi trong verification sau commit/push thực tế, không tự tham chiếu SHA trước commit. Cập nhật lại cùng4 role, kiểm tra patch/rollback trên bản sao và mở lại trước bàn giao. Bước tiếp theo duy nhất của acceptance còn thiếu là E2E fixture commit/đủ600s khi có chấp thuận và thiết bị vật lý; chưa có trả lời cho câu hỏi fixture nên chưa chạy.


#### E2E booking fixture được chấp thuận — 2026-10-25

Trạng thái: **Đang làm**. Người dùng đã cho phép tạo CUSTOMER/movie/auditorium/showtime/seats/orders kiểm thử riêng có commit và cleanup đúng UUID, không sửa/xóa dữ liệu có sẵn. Ngoại lệ chỉ áp dụng lượt E2E có ledger/run ID ngoài repo này, không thay quy tắc rollback fixture mặc định cho lượt khác. SSH dùng key được chỉ định D:\NT118\vpskey\shibakey.ppk và host fingerprint đã xác minh; không ghi private key/password/token vào repo hoặc ledger. Hai client HTTPS/Socket.IO thật sẽ tranh Couple, retry/ownership/cancel/history, chờ đủ600 giây không sửa expires_at, gồm owner đóng kết nối; cleanup finally chỉ theo UUID/ownership/FK/credit. Chưa có kết quả trước thực thi. Source backend2f2547b/Android3ace5e4 giữ nguyên; không rebuild/deploy API.


#### Nghiệm thu E2E giữ ghế thật và cleanup — 2026-10-25

Trạng thái: **Hoàn thành E2E backend/VPS; chưa kiểm tra thiết bị Android vật lý**. Mốc này thay trạng thái chờ cấp phép/chưa chạy E2E600s ở các mục trước, không xóa lịch sử. Người dùng đã chấp thuận fixture commit riêng và cleanup đúng UUID. Run `1c2e3ab9-c108-477b-a3b4-bacf1fbfde9e` dùng 3 CUSTOMER, một movie/phòng STANDARD160/suất +48h và160 ghế UUID riêng; SSH key D:\NT118\vpskey\shibakey.ppk cùng host fingerprint đã xác minh. Không đưa password/token/private key/log thô/selftest vào repo.

Hai client HTTPS + WebSocket độc lập trên VPS đã tranh cùng Couple L1–L2: đúng một201 và một409 SEAT_UNAVAILABLE; cả cặp giữ nguyên tử, giá do DB tính200000, creditUsed0; client còn lại thấy HELD realtime. Retry cùng UUID giữ nguyên order/deadline; đọc/hủy đơn người khác404; hủy lặp an toàn. Chặn email chưa xác minh403, Couple thiếu một ghế409 và chèn status/giá400. A giữ L3–L4 rồi đóng socket; B giữ A1 và tiếp tục quan sát. Không sửa expires_at/clock, không GET order hoặc POST trong lúc chờ; chỉ GET inventory mỗi30s. Cron tự nhả AVAILABLE và emit SEAT_RELEASED/ORDER_EXPIRED sau **600.113 giây thực**, B không nhận sự kiện private của A. Hai order EXPIRED; retry UUID cũ không làm sống lại/gia hạn; đặt lại bằng UUID mới được. Lịch sử order_seats CANCELED còn nguyên trước cleanup; credit cả3user vẫn1000, payments/tickets0.

Cleanup finally đã kiểm tra ownership/UUID/FK/state/credit trước khi xóa đúng fixture: 3 users, 4 orders, 160 seats, 7 order_seats và movie/phòng/suất riêng; còn0UUID fixture. Fingerprint/count toàn bộ10bảng trước tạo và sau cleanup giống hệt, không sửa/xóa dữ liệu có sẵn. JWT fixture sau xóa bị401 và GET seats fixture404. Các lượt script lỗi trước đó được giữ nguyên stderr/exit ngoài repo; fixture của lượt login kỳ vọng sai201 (API thực trả200) đã cleanup3users/160seats/0orders,0còn lại; lỗi cú pháp đã preflight/fix, không ghi DB ở lượt parse lỗi. Không restore/reset/migration/backup-restore hoặc đổi dữ liệu nghiệp vụ khác.

Local sau khi trở lại đúng nhánh feat/nguyen-seat-selection-live: rebuild backend và70/70 public tests đạt (exit0). Giữ kết quả đã xác minh73/73 Linux/VPS,51/51 Android unit,23/23 native/emulator và lint0errors; không nhận là đã chạy lại các bộ Android/Linux trong lượt chỉ cập nhật tài liệu này. Backend vẫn2f2547be285607f8c77d774862cfe090db71efae, Android/APK vẫn3ace5e4e751947eac5ac1cf6304c442567ce5e14; chỉ hai MD đổi nên không rebuild/deploy API. Commit/push tài liệu lên cùng branch riêng với đúng Author/Committer; SHA thực tế và remote-match ghi trong verification sau push. Không push main. Cập nhật/reopen cùng4role và kiểm lại baseline→modified→rollback trên bản sao. Chưa có máy vật lý để kiểm tra multi-touch/deep sleep; payment/SePay/ticket/combo vẫn ngoài scope phase này.

Sau E2E, kiểm tra đọc-only VPS lúc 2026-10-25T08:23:37Z đạt exit0: source/image đúng2f2547b, API healthy, health database up, Swagger200,10bảng/109cột, orders0/HELD0/duplicate seat codes0; smoke Playwright qua proxy CGV200, Socket.IO handshake hoạt động, backup/certbot timers active. Không đổi source/runtime hoặc tạo thêm fixture.


#### Chạm ghế trực tiếp, pinch hai ngón và nhãn N8 — 2026-10-25

Trạng thái: **Đang làm**. Theo yêu cầu mới, thay quy tắc tap toàn cảnh phóng trước/chọn sau và double-tap zoom/nút +/− trong mốc chọn ghế trước: chạm trúng ghế chọn/bỏ ngay ở mọi tỷ lệ, không tự đổi zoom hoặc chờ double-tap; chỉ pinch hai ngón thay đổi zoom, một ngón kéo sơ đồ. Giữ fit ban đầu, nút Vừa màn hình để reset theo yêu cầu chủ động, minimap và semantics trợ năng; pinch/pan không chọn ghế, Couple vẫn nguyên cặp. Gỡ nhãn CGV/Landmark81/TP.HCM và khối Khu vực khỏi UI lịch/ghế/chi tiết, dùng N8 Cineplex; không thay nguồn crawler/URL metadata nội bộ, timezone, API/DB/giữ ghế600s. Phạm vi sửa SeatMap/tap, nhãn lịch/summary/legacy detail và assertions native liên quan; không thêm dependency. Baseline đã lưu ngoài repo PRE_SEAT_GESTURE_BASELINE.zip (234file,SHA351f7867a09f0e800ca5d90b213e8ca08031a73ffa4ff5135bdd78028492221b). Còn build/unit/lint/native, rollback bản sao và commit/push cùng nhánh riêng; không rebuild/deploy backend vì chỉ đổi Android/tài liệu.


#### Nghiệm thu chạm trực tiếp/pinch/N8 — 2026-10-25

Trạng thái: **Hoàn thành Android local/emulator, chưa kiểm tra máy vật lý**. Thay trạng thái Đang làm của mục thao tác/nhãn N8 ngay trên. SeatMap.tap gọi state.select ngay khi hit ghế, xóa ngưỡng readable48dp, tapJob/delay và double-tap zoom; xóa nút +/−. Pinch hai ngón phóng/thu theo tâm ngón, kéo một ngón pan, không phát sinh chọn nhầm; giữ fit ban đầu, reset Vừa màn hình chủ động và minimap. Chạm lần đầu chọn được ngay cả Standard/IMAX toàn cảnh; chạm lại bỏ chọn, bounds/tỷ lệ không đổi; Couple/ghế bận/countdown/giữ chỗ vẫn theo quy tắc đã chốt. DetailShowtimesScreen bỏ Khu vực/TP.HCM, dùng Rạp chiếu N8 Cineplex; summary/legacy MovieDetailScreen dùng N8, gỡ lời mô phỏng/CGV. Quét toàn main không còn CGV/Landmark/TP.HCM hiển thị; không xóa nguồn crawler hoặc dữ liệu phim/lịch chiếu nội bộ.

Kiểm chứng thực tế: build debug/AndroidTest,51/51 unit và lint đầy đủ gồm test analysis đạt exit0,0errors/44warnings/3hints (cảnh báo cũ không thuộc lượt sửa). Native BASELINE9/9 → MODIFIED25/25 (20 fixture UI/Auth/gesture/detail +5 đọc-only VPS) → ROLLBACK9/9 đạt. Hai lượt siết assert pinch sau đó9/10 thất bại: đối chiếu [Compose TouchInjectionScope.pinch](https://developer.android.com/reference/kotlin/androidx/compose/ui/test/TouchInjectionScope) phát hiện test truyền sai thứ tự start0/end0/start1/end1 khiến ngón cắt nhau. Đổi sang named arguments với quỹ đạo tách/chụm đúng, đo kích thước ghế tăng/giảm thật, không sửa logic app để né assert; giữ stderr/adb exit0 và runner exit1 của cả hai lượt lỗi ngoài repo. Rebuild test/lint và cài lại MODIFIED:1/1 pinch riêng và25/25 toàn suite cuối đạt; không coi lượt25tests ban đầu là kiểm chứng pinch chuẩn thay cho suite cuối. Không tạo booking fixture/ghi DB hoặc chạy lại600s E2E trong lượt này; giữ bằng chứng E2E600.113s đã nghiệm thu trước. Đã xem ảnh lịch/ghế N8, không nút +/−. Probe cùng input seat-touch-and-visible-venue-labels: BASELINE autozoom/deferredTap=true,+/−2,CGV/Landmark/city=true → MODIFIED tất cả false,buttons0,N8=true,pinch=true → ROLLBACK trở lại đúng BASELINE; cả3exit0. ROLLBACK.sh --seat-gesture đã chạy trên bản sao riêng;234hash gốc khớp, archive trước sửa SHA351f7867a09f0e800ca5d90b213e8ca08031a73ffa4ff5135bdd78028492221b không đổi. Bản sao restored được build và chạy9native thật, không chỉ kiểm tra text. Source chính giữ GOAL mới và emulator đã cài lại bản sửa.

Cùng4role được mở rộng bằng seatGesture patch/mode rollback, giữ lịch sử/mode cũ; kiểm tra byte patch và mở lại trước bàn giao. Không đổi backend/API/schema/config nên không rebuild/deploy VPS, runtime2f2547b giữ nguyên. Cả hai MD và source/tests thuộc cùng đợt commit/push nhánh feat/nguyen-seat-selection-live, đúng Author/Committer Trần Thanh Nguyên; SHA/remote/attribution thực tế ghi trong verification sau push. Không push main. Phần chưa kiểm tra: pinch/deep-sleep trên Android vật lý; không phát sinh tính năng ngoài yêu cầu.

#### Đã lược đường dẫn artifact theo yêu cầu ? 2026-10-26

Trạng thái: **Hoàn thành**. Theo yêu cầu người dùng, đã bỏ đường dẫn tuyệt đối và metadata nhận diện bản mới khỏi Plan và AGENTS; các mốc kiểm thử/kết quả vẫn được giữ. Artifact ngoài repo không bị xóa. Source không đổi. Đã mở lại hai file và xác nhận không còn tên/path/hash bản mới; commit/push lại trên nhánh riêng.

### Chốt Seat hold, booking cutoff và chống double booking tuần 7 — 2026-10-25

Trạng thái: **Hoàn thành bổ sung kiểm thử biên, tài liệu và xác nhận VPS chỉ đọc**. Cutoff giữ tại đúng start_at theo quyết định người dùng. Chỉ bổ sung sáu bài regression thời gian DB cố định và assert HTTP409 trong bài hiện có; fixture clock mặc định không đổi. Giữ nguyên43file source/config/schema backend đã ghi hash; không sửa runtime/API/database/Android hoặc ghi DB chung. Quy tắc chi tiết ở mục9.2 và nhật ký tương ứng trong Plan.

- Local: build/typecheck/format:check và76/76 backend tests đạt exit0, gồm15/15 booking tests. Cùng lệnh booking: BASELINE9/9 → MODIFIED15/15 → ROLLBACK9/9, cả ba exit0; rollback --week7-cutoff trên bản sao đạt3/3hash gốc, workspace giữ bản mới. Các bài service dùng DB giả, không nhận là kiểm thử PostgreSQL cạnh tranh thật. Giữ cùng bốn role/ledger ngoài repo và các baseline/mode cũ.
- VPS: kiểm tra chỉ đọc qua key người dùng chỉ định/host fingerprint đã ghim lúc2026-10-25T09:09:55Z đạt exit0: API/PostgreSQL healthy, Nginx running, health database up/Swagger200, OpenAPI đủ3route Orders, startup-success và0dòng ERROR/FATAL lúc startup. Chín file JavaScript/package runtime khớp local theo hash byte hoặc LF chuẩn hóa; WebSocket HTTPS upgrade đạt và JWT sai bị AUTH_REQUIRED. API StartedAt không đổi, restart unless-stopped,5000không public/5432chỉlocalhost. Không rebuild/deploy/restart API, migrate hoặc tạo fixture vì runtime không đổi. Checker đầu exit1 do tìm source TypeScript không có trong image production; đã sửa sang dist JavaScript và giữ lỗi/exit trong ledger.

Giữ bằng chứng E2E600.113s trước đó, không gọi là lượt chạy mới hoặc76tests trên VPS. Chưa kiểm tra Android vật lý/deep sleep, không sửa Excel hoặc mở rộng payment/combo/credit. Bước tiếp theo: bàn giao kết quả, chỉ sửa runtime nếu có lỗi tái hiện được hoặc requirement mới; cả hai tài liệu thống nhất kết quả local/VPS.

### Công bố phần chốt booking tuần 7 trên branch mới — 2026-10-25

Trạng thái: **Hoàn thành tạo branch, commit và push phần kiểm thử**. Branch feat/nguyen-week7-seat-hold-cutoff đã push commit93a9e953cc71d1de8e1e6d09c24e53d032800a6a gồm booking.test.mjs và cả hai MD; push exit0/ls-remote đúng SHA. GitHub API xác nhận Author/Committer đều thnguyen290106, tên Trần Thanh Nguyên <thnguyen290106@gmail.com>, không co-author/bot. Không sửa contributor lịch sử; main remote vẫn6f717a611509aa5945a5726c723387255af6ac57, không push/merge main.

Chạy lại76/76 backend tests exit0 trước xuất bản; diff/index whitespace check và quét credential trong nội dung thêm mới đạt. Giữ15/15 booking, BASELINE9/9 → MODIFIED15/15 → ROLLBACK9/9 và hash rollback gốc. Runtime/API/DB không đổi, không rebuild/deploy VPS; giữ kết quả VPS chỉ đọc trước đó, không nhận là lượt nghiệm thu runtime mới. Hai tài liệu đồng bộ kết quả thực tế; SHA commit tài liệu cuối/remote/attribution lưu ledger ngoài repo sau thực thi, không tự tham chiếu trước. Bước tiếp theo: nhóm trưởng review và tạo/merge PR; không tự tạo/merge PR trong lượt này. Chưa kiểm tra Android vật lý hoặc chạy lại E2E600s.

### Phim Sắp Chiếu từ CGV — quy tắc thực hiện (2026-10-25)

Áp dụng cho đợt bổ sung tab Sắp chiếu. Chi tiết đặc tả, API contract, migration và tiến độ ở mục cùng tên trong Plan.

- Nguồn cố định `https://www.cgv.vn/default/movies/coming-soon-1.html`; đọc listing `li.film-lists.item`, `.product-name a`, `.nmovie-rating`, `.cgv-movie-info` (Thể loại/Thời lượng/Khởi chiếu) rồi mở trang chi tiết `/default/<slug>.html` bằng selector parser hiện có. Không thêm dependency/browser mới, không bỏ allowlist/TLS/refuse-redirect.
- Không thêm bảng/cột/status cho sắp chiếu. Phân loại bằng `movies.release_date > hôm nay (Asia/Ho_Chi_Minh)`; loại phim đã có suất ACTIVE bắt đầu trước ngày khởi chiếu; không loại phim presale có suất từ ngày khởi chiếu. Không xoá dữ liệu nguồn đã gỡ.
- Sentinels: `duration_minutes = 0` = chưa công bố, `age_rating = "NO"` = chưa phân loại. Đây là contract của API/Android; UI phải ghi "Đang cập nhật"/ẩn badge, không bịa số. Parser bắt buộc title + release date; thiếu ngày khởi chiếu là `omitted` (không PARTIAL), lỗi trang chi tiết là `skipped` (PARTIAL), listing rỗng/sai trang phải throw.
- Import dùng transaction + advisory lock `810710`, create/update theo `source_url`, non-degrading (không ghi đè duration >0/tuổi hợp lệ/nullable đã có bằng 0/`NO`/null), không tạo showtime hoặc showtime_seat trong bước coming-soon.
- Cron giữ nguyên `0 0 */6 * * *` Asia/Ho_Chi_Minh; mỗi tick chạy lịch đang chiếu rồi coming-soon, try/catch riêng. CLI one-shot `--coming-soon`; không tạo scheduler thứ hai.
- API additive `GET /api/v1/movies/coming-soon` trước `/movies/:id`, envelope `{businessDate, serverTime, items}` không kèm showtimes. Android giữ state/loading/error riêng cho tab, ẩn thanh ngày/giờ ở tab Sắp chiếu, giữ search/tab state/back/logout; lỗi tab này không được làm hỏng tab Đang chiếu hoặc fallback dữ liệu giả.
- Migration `20261025000000_coming_soon_duration` hạ CHECK `movies_duration_positive` thành `movies_duration_nonnegative`; không đổi số bảng/cột. Bắt buộc review SQL, backup trước, áp dụng bằng role migration `prisma migrate deploy`; không reset/restore/TRUNCATE/drop. Rollback code không cần rollback DB.
- VPS vẫn dùng source local không proxy + 4-file proxy overlay ngoài repo. Vì `crawler.service.ts` đã đổi cấu trúc, **phải tái tạo overlay** (config/crawler.service/smoke/crawler tests), xác nhận chỉ 4 file khác public và diff chỉ proxy; nếu patch không áp dụng an toàn thì dừng, không deploy bản direct làm mất proxy. Local giữ `127.0.0.1:5000`, không đưa proxy/secrets vào source/MD/log.
- Kiểm chứng tối thiểu: backend build/typecheck/format/tests; Linux tests trong container có proxy variant; one-shot coming-soon qua proxy trên VPS; API HTTPS trả danh sách đúng biên ngày/không showtimes; Android build/unit/lint + fixture UI + live VPS UI; DB đọc-only xác nhận 10 bảng/109 cột, không phát sinh showtime/ghế, users/inventory không đổi. Không tuyên bố tick cron tương lai nếu chưa quan sát; không nhận lượt lỗi đầu tiên là thành công.
- Trạng thái hiện tại: **Hoàn thành code/crawler/API/migration, deploy VPS, kiểm chứng Android và verification bundle/rollback**. Backend commit `f6af43bf81e6a90b97fa25d48cd3c9a8c77f31d7` và Android commit `03526a50493fee4398ae6b4f1fb0ba25c80580b8` trên `feat/nguyen-week8-coming-soon`, remote khớp; local 83/83 tests, bản proxy overlay 86/86, VPS Linux container 86/86. Migration đã backup `n8_cineplex-20261025T100129Z-390666.dump` (SHA-256 `6af515e3...d1f7`), apply đạt, 10 bảng/109 cột, users hash không đổi. API VPS health/Swagger 200, `/movies/coming-soon` trả 24 item toàn ngày tương lai/không showtimes; one-shot proxy runId `dc09cba8-...` tạo 24 phim, 2 omitted, 7 skipped nên PARTIAL (không gọi là SUCCESS; cron sẽ thử lại). Android 55 unit/34 connected (live VPS đạt), lint 0 lỗi. Đã prune builder/image theo khóa và xoá temp VPS, giữ 3 image đang chạy; không prune volume/network/DB. Verification bundle ngoài repo đã chốt; ROLLBACK.sh trên bản sao: baseline hashes OK, route/migration vắng mặt, baseline build + 14/14 catalog tests + probe route absent đạt. Giới hạn: 7 phim chưa vào DB lượt đầu, chưa quan sát cron tick, chưa thử máy vật lý; poster CLAYFACE tải chậm trong ảnh nhưng URL 200.
- Công bố nhánh mới (2026-10-25): đã tạo/push `feat/nguyen-week8-coming-soon-publish` trỏ 3 commit đã kiểm chứng (`f6af43b`, `03526a5`, `c2455f9`) rồi thêm commit tài liệu công bố trên nhánh này; remote khớp và GitHub API xác nhận author/committer `thnguyen290106`; không tạo commit rỗng, không đụng `main`/nhánh cũ, base `5987ce8` đã có trong `origin/main`.
