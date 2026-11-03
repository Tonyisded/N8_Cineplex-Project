# N8 Cineplex — Master Plan

## Quy định tên release — 2026-11-03

<!-- RELEASE_NUMERIC_NAMING_20261103 -->
Theo yêu cầu user: từ bản phát hành tiếp theo dùng tên release/tag chỉ gồm số phiên bản: v1.8, v1.9,...; không hậu tố compact-mobile/live-status hay mô tả khác. Khi tạo APK mới để phát hành, versionName tương ứng chỉ là1.8/1.9,... và tăng versionCode; không đổi tên release/tag cũ hoặc gắn nhãn1.8 lên APK1.7 cũ. Chỉ tạo release APK khi Android/APK thực sự cần cập nhật; thay đổi backend tương thích không cần APK mới thì không tạo release.

Bản sửa nhật ký an toàn/đối soát đã push source b290d1e0c40d6d240e660190a8d4f1571ae8690d trên fix/nguyen-refund-observability-20261103 và deploy VPS; gate150local/153Linux/21PostgreSQL, rollback147→150→147 là kết quả đợt sửa vừa qua, không phải test mới trong cập nhật quy định này. Android/APK không đổi nên không tạo release v1.8 trong đợt backend này. Không đổi min/latest trên VPS hoặc tag/release1.7. Nhận tiền thật vẫn cần đối soát người nhận; không suy luận lỗi cũ429 khi lịch sử không lưu.


## Nhật ký lỗi hoàn tiền an toàn và tiếp tục đối soát — 2026-11-03

Theo yêu cầu mới, đã sửa source root/candidate, kiểm thử và deploy VPS; không còn là phương án chỉ sửa riêng VPS. Chính sách snapshot.refundRetry v2 tách sendAttempts (tối đa4 lượt chuẩn bị/gửi HTTP, tính cả replay) khỏi cycles đối soát. Lỗi query không tiêu lượt gửi. Sau đủ4 lượt gửi, chuyển cần kiểm tra/reconcileOnly nhưng vẫn query theo lịch; tuyệt đối không có POST thứ5. Kết quả success đến muộn, ký/đúng reference/số tiền/bank/account, vẫn finalization transaction REFUNDED rồi dừng query. Sai dữ liệu/kết quả không hợp lệ vẫn dừng để kiểm tra, không tự chi lại. Key/body bền vững và quy tắc không đổi key khi chưa rõ kết quả giữ nguyên; mọi lượt vẫn dùng proxy Việt cố định, không đổi IP.

Marker v1 đang active được đọc tương thích và quy đổi budget bảo thủ; nghĩa vụ cũ đã NEEDS_REVIEW nhưng không có cờ reconcileOnly không tự requeue. Khoản50.000đ đã SUCCEEDED được kiểm tra đọc lại, không có thêm lệnh chi. Điều tra lỗi lịch sử trước nâng cấp vẫn không thể khôi phục mã lỗi đã bị bỏ qua; không kết luận lỗi cũ là429 chỉ từ suy đoán.

safeRefundError chỉ lấy trường cho phép: refund UUID, thời điểm, phase, cycle/sendAttempts, thời gian vòng xử lý, loại exception thuộc danh sách, HTTP status và provider code số/enum cho phép. Không ghi message/stack SDK, headers, payload, secret/token/key, số tài khoản hoặc thông tin người nhận. Lưu firstError bất biến, lastError và tối đa8 lỗi gần nhất trong snapshot, đồng thời console refund_failure để đối chiếu Docker logs. JSON notification_state không bị ghi đè. Public DTO/frontend không lộ counter/journal; không thay Android/APK, min7/latest8 và release1.7 giữ nguyên.

Gate mới: build/typecheck/format và150/150 local; Linux153/153 gồm3proxy; PostgreSQL cô lập21/21 với provider giả, có ca4 lần gửi rồi nhận kết quả success sau khởi động lại mà không gửi lần5. Test query lỗi10vòng vẫn không tiêu send budget; log/journal không chứa marker secret, message hay header thử nghiệm, bộ đệm giới hạn8. Thời gian giả/đẩy due chỉ trong fixture cô lập, không nhận là E2E ngân hàng thật. VPS đã recreate API sau tất cả gate thành công; health/Swagger/combos200, webhook unsigned400, proxy đọc số dư payOS đạt. Hai SHA256 JS runtime khớp source build root, healthy/restart0. Không migration hoặc đổi .env/secrets/Nginx/PostgreSQL/volume; giữ image trước-refund-log để phục hồi vận hành.

Cùng npm run build && npm test: BASELINE147 → MODIFIED150 → ROLLBACK147, exit0. ROLLBACK.sh --refund-log đã thực thi trên bản sao; REFUND_LOG_RECONCILE_BASELINE.zip giữ336publicfile pristine, root/candidate/VPS vẫn MODIFIED. Bốn role cũ ở D:\NT118\verification\week9-payos-ticket-refund được mở rộng, không thay bộ; tuyệt đối không restore DB để đảo chuyển khoản thật. Khi hạ image cũ phải ngăn tạo chi mới và giữ/đối soát metadata v2 vì parser cũ không hiểu v2. Lỗi unit đầu do fake lease chưa nhận diện reconcileOnly được giữ, sửa fake theo predicate thật và bổ sung regression PostgreSQL, không bỏ assertion.

Phần backend này được bàn giao để commit/push trên nhánh riêng fix/nguyen-refund-observability-20261103 theo yêu cầu; chỉ nhận công bố sau ls-remote khớp và ledger ngoài repo. Không thay tag/release1.7, không commit env/log/verification hay chạy chi thử tiền thật mới. Chưa xác nhận CI remote/máy vật lý; việc nhận tiền cuối vẫn cần người dùng đối chiếu ngân hàng.


## Proxy Việt cố định và retry hoàn tiền có giới hạn — 2026-11-03

<!-- FIXED_PAYOUT_PROXY_REFUND_RETRY_20261103 -->
Đã triển khai backend trên root/candidate và bật VPS sau gate; phần này thay trạng thái “proxy/retry chưa triển khai” của bước UI trước, không đổi lịch sử kết quả cũ. Dùng một cấu hình CONNECT proxy Việt đã dùng cho crawler, không luân phiên IP hoặc fallback trực tiếp khi lỗi. Chỉ payout channel (tạo/query/list/số dư) đi qua ProxyAgent riêng; thu tiền/webhook/email/FCM không bị đổi global dispatcher. Preflight quan sát countryVN và cùng egress ở2request; xác thực TLS giữ nguyên, redirect bị từ chối, SDK maxRetries0. Không hứa IP của nhà cung cấp proxy bất biến vĩnh viễn. Kết nối số dư payOS ký/xác thực thật qua adapter production đã đạt; không tạo payout thật để nghiệm thu.

Config mới: PAYOS_PAYOUT_PROXY_ENABLED, PAYOS_PAYOUT_PROXY_SERVER, PAYOS_PAYOUT_PROXY_USERNAME, PAYOS_PAYOUT_PROXY_PASSWORD, REFUND_RETRY_ENABLED. Mặc định local tắt; .env.example chỉ placeholder. Giá trị thật proxy chỉ ở env riêng bảo vệ trên VPS, sao chép từ cấu hình crawler hiện có bằng xử lý trong bộ nhớ; so sánh sau compose parse để tránh sai quoting, không log credential. Server URL chỉ http/https không userinfo/path/query; username/password đi cùng nhau, cấm tắt kiểm tra TLS. Undici8.12.3 được pin để dùng fetch và ProxyAgent cùng phiên bản; không cấu hình proxy toàn tiến trình. Local .env/JWT/provider keys gốc không đổi. Hai công tắc proxy/retry hiện bật VPS; giữ image/env trước triển khai (env backup600), không migrate/reset DB/Nginx/volume.

Chỉ các yêu cầu hoàn mới đủ dữ liệu nguồn thanh toán được gắn snapshot.refundRetry v1. Nghĩa vụ hoàn cũ/NEEDS_REVIEW và dữ liệu đã xóa không tự hồi sinh/requeue. Tối đa4vòng, thời gian DB và lease/CAS ngăn nhiều worker cùng gửi; nextAttemptAt tối thiểu180giây tính từ khi kết thúc vòng trước, tôn trọng Retry-After dài hơn. Lịch nền15giây có thể làm lần chạy muộn hơn180giây. Cycle/key/baseIds/dispatch budget được ghi bền vững trước HTTP, restart không reset. Metadata ở snapshot của chính refund để không ghi đè JSON retry thông báo đang được mail/FCM worker cập nhật. Success ký hợp lệ, đúng reference/số tiền/bank/account thì transaction REFUNDED và dừng; chưa thành công sau4vòng chuyển NEEDS_ADMIN/NEEDS_REVIEW, không hồi sinh vé/ghế. Email/FCM dùng retry hiện có, lỗi thông báo không đảo tiền/vé.

Retry an toàn không đồng nghĩa phát4khoản chi mới: mỗi vòng query authoritative trước; timeout/mất phản hồi chỉ replay đúng key/body đã ghi, hoặc nhận success từ query mà không gửi lại. Chỉ FAILED/PAYOUT_INSUFFICIENT_FUNDS đã ký, không reference ngân hàng và không transactionDatetime mới cho phép key của lần thử kế tiếp trên cùng business refund/reference/recipient. FAILED do ngân hàng có reference, trạng thái đang xử lý hoặc kết quả không rõ chỉ đối soát, không đổi key. Sai danh tính/số tiền/đích, nhiều success hoặc metadata hỏng chuyển kiểm tra ngay thay vì thử mù4lần. Chính sách an toàn này có chủ ý; không né429 bằng đổi IP.

Frontend/public refund DTO không có cycle/key/budget hoặc nhãn “lần x/4”; vẫn chỉ trạng thái đang xử lý rồi hoàn thành/cần kiểm tra. Không sửa Android trong bước backend này. APK8/1.7-compact-mobile đã kiểm chứng ở bước UI được công bố thêm trên download VPS, hashd06479570526552f5f6ea10cd7903a6dbe98fab5203c60ccf20ee72fa0a53491; latest8/minimum7 (không phá client7 tương thích). APK download HTTPS đã tải/đối chiếu, source Android Studio vẫn bản8. Kết quả88unit/6native+1font lớn thuộc bước UI trước, không nhận là chạy Android mới; Gradle output hiện có thể bị IDE clean, artifact bất biến và APK pulled-installed vẫn khớp.

Nghiệm thu mới: build/typecheck/format và147/147 local đạt; Linux150/150 gồm3proxy; PostgreSQL cô lập20/20, provider giả, gồm6worker tranh1refund chỉ chi/finalize1lần,4vòng giữ budget, HTTPtimeout sau success và restart không tạo lần chi thứ hai. Kiểm thử lịch180giây dùng clock giả và chỉ đẩy due fixture trong DB cô lập; không nhận là đã chờ9phút với ngân hàng thật. Proxy CONNECT fixture chứng minh credential chỉ ở CONNECT, không direct fallback; live proxy read-only payOS balance đạt. VPS health/Swagger/combos7&8 trả200; bản6 trả426, webhook unsigned400; API healthy/restart0. Ba hash JS adapter/service/policy đã đối chiếu runtime khớp root; bootstrap thành công, 0ERROR/FATAL, ledger lưu ngoài repo.

Cùng lệnh npm run build && npm test: BASELINE135 → MODIFIED147 → ROLLBACK135, exit0; ROLLBACK.sh --refund-retry thực thi trên bản sao, khôi phục333file public pristine theo REFUND_RETRY_BASELINE.zip; active root/candidate/VPS giữ modified. Bốn role cũ tiếp tục MODIFIED_FILE.zip/DIFF_FILE.patch/VERIFICATION.txt/ROLLBACK.sh ở D:\NT118\verification\week9-payos-ticket-refund, thêm mode/record chứ không thay bộ cũ. Rollback tiền thật không được thực hiện bằng restore DB; khi rollback vận hành phải dừng tạo chi mới và đối soát trước, giữ mọi metadata giao dịch. Các lỗi sync syntax/wrong-CWD dependency, fixture thiếu intent/reference, encoding thông báo, thiếu Gradle output và khác nơi cấp env proxy đã giữ literal evidence rồi sửa đúng nguyên nhân, không sửa assertion/production để né lỗi test.

Giới hạn: chưa tạo giao dịch hoàn tiền thật mới hoặc thử4lần với ngân hàng thật; chưa xác nhận CI remote. Backend đã công bố trên nhánh fix/nguyen-mobile-ui-20261103, source commit 923fc827841a9444b1369129c80d9331021febfa; tag v1.7-compact-mobile và GitHub Release APK debug đã xác minh hash công khai. Logic tài chính thể hiện rõ trong commit, không giấu dưới commit UI. Tag/release v1.6 giữ nguyên. Khi user thử thật cần đủ quỹ và xác nhận tiền đến tài khoản cuối; provider SUCCEEDED không thay thế đối soát ngân hàng.


## Giao diện điện thoại gọn hơn và bỏ tài khoản nhận hoàn — 2026-11-03

Đã xem ba ảnh n1/n2/n3 do user cung cấp. Thu gọn Home/Auth/Profile bằng padding/khoảng cách/logo và tiêu đề24sp thay32sp; body/label14sp, nhãn thanh điều hướng dùng labelSmall. Giữ vùng chạm48dp trở lên, trường nhập56dp và cỡ chữ hệ thống, không ép fontScale hay giảm density toàn app. Thanh toán vẫn giữ tài khoản nhận thu tiền; chỉ bỏ dòng tài khoản nhận trong tiến trình hoàn và số tài khoản trong hộp xác nhận hoàn, thay bằng nguồn thanh toán đã xác minh. Không đổi dữ liệu người nhận/backend hoặc gửi lệnh chi.

Đã đồng bộ candidate vào root Android Studio trước build/install: versionCode8/versionName1.7-compact-mobile. assembleDebug/assembleDebugAndroidTest/testDebugUnitTest/lintDebug/installDebug/installDebugAndroidTest đạt,88unit; native6/6 ở360dp (Compact1+Tickets5), thêm Compact1/1 ở fontScale1.3 đạt. Đã xem ảnh đăng nhập gọn ở360dp; chưa kiểm tra trực tiếp trên điện thoại vật lý của user hoặc mọi màn hình/fontScale. APK root/emulator hash khớp `d06479570526552f5f6ea10cd7903a6dbe98fab5203c60ccf20ee72fa0a53491`. Bản debug-signed cho nhóm thử, không uninstall/clear data; SDK/IDE/local.properties/.env giữ nguyên. Giữ README root rỗng.

Bộ bốn role cũ bổ sung MOBILE_UI_BASELINE.zip và mode ROLLBACK.sh --mobile-ui, không thay các mode trước. BASELINE source version7/còn dòng nhận tiền/88unit → MODIFIED version8/không dòng nhận tiền/88unit → ROLLBACK bản sao version7/còn dòng/88unit, cùng lệnh assembleDebug cleanTestDebugUnitTest testDebugUnitTest, exit0; snapshot10file khớp. Root/emulator giữ MODIFIED. Lượt root baseline ban đầu có thể chồng thời gian sửa nên không dùng làm bằng chứng baseline; đã chạy lại baseline bất biến trên bản sao. Lỗi Python encoding và install thiếu emulator được giữ ngoài Git, sửa lệnh/khởi động lại AVD không wipe rồi kiểm thử đạt.

Phạm vi commit UI lần này chỉ thu gọn giao diện/bỏ thông tin nhận tiền và kiểm thử/tài liệu tương ứng; không có code retry/proxy bị giấu trong commit. User chốt bước kế tiếp dùng một proxy Việt cố định (không luân phiên IP) và retry tối đa4lần cách180giây, dừng thành công, không hiện số lần frontend. Phần proxy/retry chưa triển khai hoặc deploy trong đợt UI này; cần kết nối proxy bảo vệ credential, kiểm tra chính sách provider, phân biệt query/dispatch và bảo đảm không tạo khoản chi mới khi kết quả còn unknown. Không thử chi tiền thật trong kiểm chứng UI. Không đổi tag/release1.6 hoặc minimum7 trên VPS trong bước này.


## Xác nhận công bố source và APK tuần 9 — 2026-11-02

Theo yêu cầu user: README.md ở root đã làm trống và vẫn giữ file, source commit `7e45158d8d6831894bb0866b2a7bc46f0c75b5e7` đã push lên nhánh mới `feat/nguyen-week9-payos-live-status`. Annotated tag `v1.6-live-status` đã push và peel đúng source 7e45158d8d6831894bb0866b2a7bc46f0c75b5e7; Author/Committer được GitHub xác nhận cùng tài khoản thnguyen290106, dùng noreply, không thêm đồng tác giả. Không main/force-push/PR/merge, không công bố .env/key/token hoặc evidence riêng tư.

GitHub Release thử nghiệm: https://github.com/Tonyisded/N8_Cineplex-Project/releases/tag/v1.6-live-status. Đã upload APK1.6-live-status/versionCode7 và SHA256SUMS.txt; tải lại qua URL công khai xác nhận SHA256 `7a67a2fe63b637e81be251c9c6ce3059d3c98ccda5669f33a7347d41dfcdc41b` khớp APK root/emulator/VPS. APK ký debug để nhóm test, không nhận là ký production/Play Store. Các kết quả135backend/138Linux/88unit Android/11native UI+5native HTTPS chỉ đọc và rollback111→135→111,77→88→77 ở mục bên dưới giữ nguyên; không coi công bố GitHub là lượt test tiền thật mới hoặc CI remote.

Mục này xác nhận kết quả đã quan sát và thay trạng thái “tag dự kiến/chưa công bố” bên dưới. Tag giữ nguyên source commit đã kiểm chứng; bản ghi xác nhận chỉ cập nhật hai MD trong commit tiếp theo trên cùng nhánh, không di chuyển tag. SHA cuối nhánh được đối chiếu sau push và lưu ngoài repo, không tự tham chiếu SHA trước commit. Khoản hoàn qua ví trung gian vẫn cần đối soát người nhận cuối; không nhận cả3 task tuần9 đã nghiệm thu tiền thật hoàn chỉnh.


## Phiên bản bắt buộc và tự cập nhật trạng thái — 2026-11-02

<!-- APP_COMPATIBILITY_AUTO_REFRESH_20261102 -->
Đã triển khai bản Android versionCode7/versionName1.6-live-status. Source Android và backend hiện đồng bộ vào project chính `D:\NT118\N8_Cineplex-Project`, không chỉ ở candidate. Build/install từ Gradle root `N8_Cineplex`; giữ session, local.properties, IDE settings, signing và .env, không uninstall/clear data. APK kéo từ emulator khớp SHA256 của APK build tại root: `7a67a2fe63b637e81be251c9c6ce3059d3c98ccda5669f33a7347d41dfcdc41b`. MainActivity đã qua cổng kiểm tra phiên bản của VPS và vào Trang chủ; ảnh kiểm chứng giữ ngoài Git.

Backend công bố GET /app/version và GET /app/android/download. Android gửi X-App-Version-Code qua toàn bộ năm API client; kiểm tra khi mở/quay lại app và mỗi60giây foreground. Dưới minimumVersionCode: chặn luồng app, hiện “Phiên bản ứng dụng đã cũ, vui lòng cập nhật lên phiên bản mới.”, nút tải APK và Kiểm tra lại. HTTP426 cũng kích hoạt cảnh báo. Mạng lỗi lần đầu có thông báo/thử lại, không tự nhận tương thích. URL cập nhật chỉ HTTPS cùng API host/path, không chấp nhận URL lạ. Người dùng tự xác nhận cài APK; không cài ngầm, không cần xóa app.

Quy trình deploy ghép API–APK: build/test source mới, tăng versionCode khi đổi hợp đồng API cần client mới, công bố APK có hash kiểm chứng trước, rồi mới tăng ANDROID_MIN_VERSION_CODE. ANDROID_LATEST_VERSION_CODE, ANDROID_APK_PATH và ANDROID_APK_SHA256 được validate cùng min; path tuyệt đối .apk, archive PK/hash được kiểm tra trước API startup. Bản mẫu .env chỉ có tên biến/public default và placeholder, không credential. Thay dữ liệu phim/lịch chiếu hoặc restart server không tự ép tải APK nếu hợp đồng vẫn tương thích. Min local mặc định0; VPS hiện min/latest7, APK content-addressed gắn read-only, manifest/download no-store. Giữ image/env/compose/APK trước để rollback vận hành; không hạ/xóa dữ liệu giao dịch.

Tự cập nhật thanh toán/vé/hoàn: vòng foreground theo lifecycle, đọc ngay khi quay lại từ ngân hàng; đang chờ/chi tiết khoảng3giây, danh sách10giây, lỗi tăng dần tối đa30giây. Dừng background và sau trạng thái terminal; vào lại vẫn đọc một lần. GET /orders/:id/payment xác thực ownership, chỉ đọc DB; auto-refresh không tạo order/payment, không gọi provider và không gửi lại yêu cầu hoàn. Giữ QR thanh toán khi lỗi mạng tạm thời, hiển thị lỗi nhẹ/thử lại; QR vé không còn hợp lệ được ẩn. Generation/identity guard bỏ phản hồi cũ khi logout, điều hướng, thanh toán thủ công hoặc hoàn, không để pending cũ đè PAID hay VALID cũ khôi phục QR sau hoàn. Khi đọc mới thành công xóa lỗi mạng cũ.

Nghiệm thu mới: backend build/typecheck/format đạt135/135; Linux138/138 gồm3proxy. Android assembleDebug/assembleDebugAndroidTest/testDebugUnitTest/lintDebug/installDebug đạt88/88 unit; native11/11 gồm VersionRefresh2, Tickets5, CheckoutPolish2, Voucher2 đạt. Kiểm thử auto status dùng fake transport, có pause/resume, lỗi mạng/backoff, không resend và cạnh tranh phản hồi; không coi là mua/hoàn tiền thật. VPS min7: thiếu header/bản6 trả426, bản7 /combos200; health/Swagger/email verification200; avatar public404 khi không tồn tại thay vì426; webhook unsigned400 (không426); payment read không JWT401. Manifest/download200 và APK hash khớp, API healthy/restart0. Không đổi schema/DB hoặc gọi giao dịch thu–chi thật trong đợt này; worker nghiệp vụ hiện có tiếp tục vận hành.

Rollback thực thi trên bản sao bằng ROLLBACK.sh --version-refresh: cùng npm run build && npm test cho BASELINE111 → MODIFIED135 → ROLLBACK111; cùng Gradle assembleDebug cleanTestDebugUnitTest testDebugUnitTest cho77/version6 →88/version7 →77/version6, tất cả exit0.307 pristine file khớp archive baseline; source root/emulator giữ modified. Bộ bốn role MODIFIED_FILE.zip/DIFF_FILE.patch/VERIFICATION.txt/ROLLBACK.sh vẫn ở D:\NT118\verification\week9-payos-ticket-refund, bổ sung mode/bằng chứng mới chứ không thay bộ cũ. Lỗi formatting ban đầu đã giữ literal log rồi sửa đúng style; không sửa logic để né test.

Theo yêu cầu công bố mới của user: chỉ làm trống README.md ở root (không xóa file, không đổi README con); baseline2090byte → modified0byte → rollback bản sao2090byte/hash gốc. Source bàn giao trên nhánh mới `feat/nguyen-week9-payos-live-status`, tag phát hành dự kiến `v1.6-live-status`; APK dùng đúng bytes root/emulator/VPS đã kiểm chứng, debug-signed cho kiểm thử, không nhận là bản ký production/Play Store. Chỉ commit/push source và hai MD, không đưa env/key/token/log/verification/APK vào Git; APK đính kèm GitHub Release riêng. Không force-push, không main, không tạo/merge PR. SHA/remote/tag/release asset chỉ ghi vào ledger sau khi lệnh thực sự thành công, không tự tham chiếu SHA trước commit.

Bổ sung kiểm thử native HTTPS chỉ đọc5/5: auth health/login validation và inventory STANDARD/IMAX/SCREENX/GOLD thật. Hai test dùng HTTP thô đã thêm version header; sửa giả định test cũ “hôm nay phải có phim” bằng chờ CUSTOMER rồi chọn ngày thực tế của suất. Lượt đầu1pass/4timeout và exit0 adb được giữ trong ledger; lượt retry runner OK(5tests), không sửa production/lịch chiếu/giờ hệ thống để né test. Năm hash dist runtime mới khớp root theo byte sau formatting; APK vẫn giữ nguyên hash đã công bố.

Giới hạn: chưa kiểm tra máy vật lý hoặc luồng tiền thật mới sau thay đổi auto-refresh. Một lần hoàn qua nguồn thanh toán trung gian vẫn cần chủ tài khoản đối soát người nhận cuối; provider SUCCEEDED không tự chứng minh khách nhận vào tài khoản cá nhân. Vì vậy chưa chốt toàn bộ3 task tuần9 là nghiệm thu tiền thật hoàn chỉnh. Không retry/xóa lần hoàn đó trong đợt này.


## Bắt buộc đồng bộ source Android khi code xong — 2026-11-02

<!-- ANDROID_SOURCE_SYNC_REQUIRED_20261102 -->
Theo yêu cầu người dùng: sau mỗi lần hoàn tất thay đổi Android, phải đồng bộ source và cấu hình public mới nhất vào đúng project Android Studio đang mở: `D:\NT118\N8_Cineplex-Project\N8_Cineplex`. Candidate ngoài repo chỉ là bản làm việc/kiểm chứng, không phải nơi bàn giao cuối. Không được chỉ cài APK từ candidate rồi báo hoàn thành trong khi project chính vẫn build bản cũ.

Trước đồng bộ phải giữ bản pristine/hash và kiểm tra xung đột với thay đổi của người dùng; không ghi đè âm thầm. Chỉ đồng bộ các file liên quan, giữ `local.properties`, SDK/IDE settings, signing và bí mật môi trường. Backend/provider secret không được chuyển vào Android/APK; Firebase client config public được giữ đúng theo triển khai. Sau đồng bộ, build/test từ chính Gradle root của project chính, cài APK được build tại đó, đối chiếu versionCode/versionName, hash APK và phiên bản trên thiết bị. Giữ tài khoản/dữ liệu ứng dụng khi cập nhật, không uninstall/clear data.

Chỉ kết thúc khi source project chính khớp bản đã kiểm chứng và lần Run tiếp theo có thể build source mới. AGENTS.md và N8_Cineplex_Plan.md phải cùng cập nhật trạng thái đồng bộ, lệnh/kết quả, phiên bản và giới hạn; không dùng kết quả cũ thay cho lượt kiểm tra mới. Không tự commit/push/deploy hay ghi đè `.env` khi đồng bộ Android.

## Chỉnh ghi chú QR và mũi tên Chi tiết vé — 2026-11-02

Theo ảnh người dùng: đã bỏ đúng dòng “QR chỉ có giá trị khi vé còn hợp lệ trên máy chủ.” trong TicketsScreen, không thay điều kiện hiệu lực/ẩn QR. NavigationIcon ở Chi tiết vé đổi từ ký tự nhỏ sang CineplexIcon(back)28dp trong IconButton48dp, mô tả trợ năng “Quay lại vé của tôi”, giữ callback closeTicket. Dùng lại icon có sẵn, không thêm dependency hay đổi các màn khác.

Đã đồng bộ hai file production+native regression vào project Android Studio chính, build/test/lint/install từ chính `D:\NT118\N8_Cineplex-Project\N8_Cineplex`, source khớp candidate. VersionCode6/versionName1.5-ticket-detail; 77unit đạt và native TicketsUiTest5/5 đạt, gồm kiểm tra không còn ghi chú, icon28dp/hitbox48dp, bấm quay lại, QR vé hoàn vẫn ẩn và các regression thanh toán/hoàn dùng fake. Native baseline mới trước sửa1fail do còn dòng ghi chú, sau sửa đạt; không coi exit0 của adb là kết quả runner.

Source probe cùng input: BASELINE ghi chú có/arrow mới chưa có → MODIFIED ghi chú không còn/28dp+48dp có → ROLLBACK trên bản sao trở lại baseline; restored SHA256 TicketsScreen=44ccf1946e0aa838219798e5bb369816589b548036b89e613e38f6cd5bed2f3b, bốn hash baseline trong TICKET_DETAIL_BASELINE.zip khớp. Mode ROLLBACK.sh --ticket-detail đã thực thi, giữ mọi mode/hồ sơ cũ. Project chính và APK trên emulator giữ modified, không rollback thiết bị hoặc dữ liệu thật.

Đã xem ảnh ticket-detail-back-polished.png từ emulator. APK build từ root và APK kéo ngược trên thiết bị khớp SHA256 8fc1dd7d4cdc921bc0e4308c9c50fdc1bd2a0fbf357fc800705d06e33da50f12. Không thay backend/.env/VPS/thanh toán/hoàn tiền, không giao dịch tài chính hoặc xóa dữ liệu trong lượt UI này. Không uninstall/clear data, không commit/push; chưa thử máy vật lý. Hai MD được đồng bộ sau thay đổi theo quy định bắt buộc.

## Trạng thái bàn giao Android Studio hiện tại — 2026-11-02

Đã sửa nguyên nhân Run mở bản cũ: project chính trước đây vẫn version1/1.0 trong khi APK mới build ở candidate. Theo yêu cầu mới, đã xuất bản 31 file Android thay đổi/mới vào `D:\NT118\N8_Cineplex-Project\N8_Cineplex`; toàn bộ137 file Android source/config public khớp bản mới đã kiểm chứng. Hai MD hiện cũng được đồng bộ trực tiếp vào repo chính. Các ghi chú lịch sử bên dưới nói Android chỉ ở candidate hoặc source gốc chưa đổi không còn mô tả trạng thái bàn giao hiện tại.

Đã build/test từ đúng Gradle root project chính bằng `gradlew.bat assembleDebug cleanTestDebugUnitTest testDebugUnitTest`: BASELINE version1/1.0 và68unit → MODIFIED version5/1.4-checkout-polish và77unit → ROLLBACK trên bản sao version1/1.0 và68unit, tất cả exit0. Bản sao rollback khớp mọi hash pristine Android+hai MD; source project chính giữ MODIFIED. Lần CLI đầu thiếu JAVA_HOME/exit9009 được lưu, đã sửa dùng Android Studio bundled JBR; không sửa IDE/JDK settings của người dùng.

`gradlew.bat installDebug` từ project chính cài thành công lên emulator, khởi động MainActivity và dumpsys xác nhận versionCode5/versionName1.4-checkout-polish. APK kéo ngược từ thiết bị có SHA256 khớp chính APK ở `N8_Cineplex/app/build/outputs/apk/debug/app-debug.apk`: f9a146bafcbf892b4027bbba8cc949adf1902db3b0d0f363ffed408e45ce3fc1. Đã giữ session/dữ liệu, local.properties, IDE settings và signing; không uninstall/clear data. IDE linkedExternalProjectPath là $PROJECT_DIR$, module app cùng root.

Bộ bốn role hiện hữu bổ sung ANDROID_STUDIO_BASELINE.zip, manifest xuất bản, mode ROLLBACK.sh --studio-source và literal commands/stdout/stderr. Bytes pristine được giữ trong archive bất biến; đồng bộ bản đã kiểm chứng vào đường dẫn source người dùng yêu cầu. Không sửa backend, .env, VPS, dữ liệu tiền thật hoặc commit/push trong đợt đồng bộ này. Chưa tự bấm nút Run trong IDE hoặc thử máy vật lý; build/install Gradle từ đúng root đã thực thi.

## Sửa bàn giao checkout và hoàn thiện UI/FCM — 2026-11-02

<!-- WEEK9_UI_FCM_FOLLOWUP_20261102 -->
Chưa chốt hoàn thành cả3 task tuần9 hoặc E2E tiền thật. User phát hiện checkout còn thông báo tuần8; đã sửa nút Thanh toán, bỏ thông báo xác nhận đơn cũ, mở payment QR trên cùng order. Native mới kiểm tra liên tục checkout→QR→PAID→Vé của tôi→hoàn→QR vô hiệu, transport giả, không chuyển tiền thật. Bộ native checkout/tickets/voucher10/10 đạt; Android74unit/build/lint đạt. Voucher đổi thành bottom sheet full-width, thẻ minh hoạ/mức giảm/điều kiện/chọn-bỏ; nhận diện N8, không logo MoMo. My Tickets có hero/tabs/thẻ poster/nhãn trạng thái/khung QR và tiến trình hoàn. Logo APK dùng nguyên PNG image-8.png do user cung cấp, adaptive+legacy; không đổi ảnh gốc. APK versionCode3/versionName1.2-week9-payments để phân biệt bản cũ.

Theo user yêu cầu Playwright tự thiết lập: đã tạo Firebase project n8-cineplex, gói Spark, tắt Analytics/Gemini/Developer Program/billing upgrade. Đã đăng ký Android com.nhom8.cineplex, service account riêng cineplex-fcm-sender chỉ role Firebase Cloud Messaging API Admin, cấu hình private key trong backend env bảo vệ. FCM_ENABLED đã bật VPS, OAuth thật đạt, gửi thông báo kiểm thử HTTP200 và thấy thông báo đúng package trên emulator; không dùng sự kiện này làm bằng chứng mua/hoàn tiền thật. Không đưa private key/device token vào Git, APK hoặc evidence; browser working files private/ignored, key chuyển ra thư mục private ngoài repo. Thu/chi/email vẫn bật theo user trước đó; không tạo payment/payout thật. Bản nguồn và APK mới chỉ ở candidate/evidence, không phải source cũ trong repo gốc. Chưa nghiệm thu ngân hàng hai đầu hoặc máy vật lý.

Bằng chứng backend và rollback trước giữ nguyên:111→129→111 exit0,296hash khớp; Linux132,PostgreSQL14 dùng provider giả. Bộ bốn role cũ được cập nhật, không thay bộ khác.


## Nghiệm thu code tuần 9 và bật thu–chi — 2026-11-02

<!-- WEEK9_PAYOS_VERIFIED_20261102 -->
Code được kiểm chứng trên candidate; VPS đã migrate và chạy `n8-cineplex-api:week9-payos-20261102`, healthy/restart0. Đã xóa cột Credit production, giữ10 bảng nghiệp vụ; không reset tài khoản/đơn. Backup trước migration đã restore thành công trên PostgreSQL cô lập rồi dọn container kiểm chứng. Health/Swagger200, tickets/vouchers không JWT401, OpenAPI có payment/ticket/refund. Bốn hash JS payment/provider/email/booking runtime khớp local; config riêng Linux giữ lớp proxy cũ.

Kết quả mới: backend129/129, typecheck/Prisma/format đạt; Linux132/132 (129 public+3proxy); PostgreSQL cô lập14/14, transport giả; Android74/74 unit, native7/7, build/lint đạt (0errors/54warnings/5hints). Đã xem ảnh UI vé/payment/refund và email desktop/mobile. Rollback bản sao cùng lệnh `npm run build && npm test`: BASELINE111 → MODIFIED129 → ROLLBACK111, exit0;296/296 hash pristine khớp, patch tái tạo đúng byte. Repo gốc giữ nguyên, source mới nằm trong candidate và MODIFIED_FILE.zip ngoài repo; không commit/push.

Theo yêu cầu tiếp theo của user, PAYMENT_LIVE_ENABLED/REFUND_PAYOUT_ENABLED/TRANSACTION_EMAIL_ENABLED đã bật trên VPS; webhook payOS đăng ký thành công. FCM vẫn tắt vì chưa có Firebase project/credentials. Ảnh ví Bảo Kim user cung cấp hiển thị0đ; đã cảnh báo cần nạp quỹ trước thử hoàn. Chưa tạo payment/payout/gửi email thật trong kiểm chứng, payments/tickets production vẫn0 tại postcheck; chưa nhận là E2E tiền thật đạt. User tự mua/hoàn và đối chiếu ngân hàng hai đầu.

Lỗi build hết đĩa đã đổi sang image delta tái sử dụng base cũ, chỉ dọn cache/image mới theo ID, không đụng volume/image dự phòng. Lỗi test Linux do overlay proxy đã sửa bằng giữ cả helper/config/test overlay cũ, không sửa production để né test. Lỗi interpreter rollback và newline patch đã sửa, giữ lỗi/lượt chạy trong evidence. Bằng chứng tại `D:\NT118\verification\week9-payos-ticket-refund`: MODIFIED_FILE.zip, DIFF_FILE.patch, VERIFICATION.txt, ROLLBACK.sh và APK N8-Cineplex-week9.apk. Chưa thử máy vật lý/Gmail/FCM thật; không scanner/CRUD Staff/reminder.


## Quy định cấu hình và bí mật `.env` — 2026-11-02

<!-- ENV_SECRETS_POLICY_20261102 -->
Từ giờ, khi thêm key, token, mật khẩu, chuỗi kết nối có thông tin xác thực hoặc cấu hình nhạy cảm cần giấu, phải đặt giá trị thật trong `backend/.env` riêng của môi trường và đọc qua biến môi trường/ConfigService; không hardcode trong source, test/fixture, tài liệu hoặc log. Nếu thêm biến mới, cập nhật đồng thời nơi đọc/kiểm tra cấu hình và `backend/.env.example` bằng tên biến, mô tả và placeholder hoặc giá trị rỗng, tuyệt đối không sao chép bí mật thật vào bản mẫu.

`backend/.env` và các `.env`/`.env.*` chứa bí mật phải được Git ignore, không commit/push GitHub. Chỉ `backend/.env.example` đã loại bỏ bí mật được phép track. Giữ nguyên credential/JWT secret đang dùng nếu không có yêu cầu đổi; không ghi đè `.env` hiện có bằng bản mẫu. Không đưa nội dung `.env` vào báo cáo hoặc artifact công khai; bản sao kiểm chứng/rollback có bí mật phải nằm ngoài repo, giới hạn quyền truy cập. Nếu bí mật từng bị commit/lộ, phải báo để thu hồi/đổi key; thêm ignore không xóa được lịch sử Git.

Android hiện dùng Gradle properties cho URL API và Google WEB client ID công khai, không tự đọc `.env`. Secret backend/provider phải ở server, không đóng gói vào `BuildConfig`, resource hoặc APK; chuyển sang `.env` khi build không làm secret trong APK trở nên an toàn. Chỉ thêm biến provider khi tích hợp được triển khai/duyệt, không bịa key hay coi kế hoạch payOS/MB/Bảo Kim là đã hoạt động.

Đã rà 13 biến backend hiện được sử dụng; giữ 9 giá trị có sẵn và bổ sung rõ `HOST`, `PORT`, `TRUST_PROXY`, `CRAWLER_ENABLED` với mặc định local. Bản mẫu chứa đủ 13 tên biến, bí mật để rỗng/placeholder; README nêu cách cấu hình backend và giới hạn Android. Kiểm tra offline BASELINE → MODIFIED → ROLLBACK gồm validate cấu hình, Git ignore và đồng bộ chính sách hai MD; rollback chỉ trên bản sao, source hiện tại giữ thay đổi. Không commit/push, deploy, ghi DB hoặc thử gửi mail/thanh toán thật trong đợt này.


## Tuần 9 — payOS, vé/hoàn tiền thật, bỏ Credit — 2026-11-02

<!-- WEEK9_PAYOS_TICKETS_REFUNDS_20261102 -->
**Đặc tả hiện hành được người dùng chốt; thay SePay/Refund Credit cũ. Trạng thái: đang triển khai và kiểm chứng trên candidate, chưa nghiệm thu tiền thật.** Thu qua payOS về MB; hoàn bằng kênh chi payOS từ quỹ Bảo Kim về đúng nguồn thanh toán đã xác minh. Có khóa/kênh hoạt động không chứng minh đã đủ quỹ hoặc chi tự động thành công. Không tự thu/chi, gửi mail thật, commit/push hoặc xác nhận E2E ngân hàng trong bước code/test.

- Bỏ sạch Credit N8: loại cột `users.credit_balance`, `orders.credit_used`, constraint, logic/DTO/profile/fixture đang dùng bằng migration mới; không sửa migration đã áp dụng, không reset DB/tài khoản/đơn. User xác nhận chưa sử dụng Credit. Backup bảo vệ ngoài repo trước drop; nếu thấy khoản tiền thật liên quan trái với tiền đề này thì dừng cutover để báo. Không xóa credit phí dịch vụ của payOS. Bản API cũ đọc cột Credit không dùng được làm image rollback sau drop.
- Voucher FIRST50/CINE20 hiện có giữ nguyên, mỗi đơn một voucher, không cộng dồn; `bankAmount=totalAmount`. Quote readonly, giá do server quyết định; token quote cũ yêu cầu báo giá mới. Giữ cùng order/deadline 600 giây khi back/retry, không fallback demo.
- QR thu do payOS sinh cho từng đơn, số tiền/nội dung điền sẵn; app không có ô sửa tiền. Không hứa kiểm soát thao tác của mọi app ngân hàng: backend chỉ phát vé khi tiền thực nhận đúng, chữ ký và trạng thái hợp lệ. Sai/thừa/thiếu/trễ hoặc chuyển thêm giữ vết và `NEEDS_ADMIN`, không hồi sinh đơn/ghế.
- Finalization chung cho webhook/query và ca 0đ; một transaction kiểm tra hold/ghế rồi PAID/ACTIVE/SOLD/phát một vé. Ca 0đ không gọi thu tiền hoặc tạo khoản thu giả. Return URL/socket không phải bằng chứng thanh toán.
- Một order/một ticket/một QR, có thể nhiều ghế. Snapshot giá/phim/poster/suất/phòng/ghế/combo/voucher/email giữ nguyên khi ghế được hoàn rồi bán lại. QR không chứa dữ liệu cá nhân; vé hoàn/hủy/hết hạn không dùng lại được.
- Self-refund toàn đơn: PAID, VALID, chưa check-in/chưa nhận combo, còn ít nhất45 phút theo DB. Hoàn100% tiền thực trả vé+combo sau voucher, không đổi voucher ra tiền; N8 chịu phí chi. Ghi nghĩa vụ/idempotency trước HTTP; REFUND_PENDING vô hiệu vé/nhả đúng ghế, chỉ REFUNDED khi chi thành công. Thiếu dữ liệu người trả/quỹ/phí hoặc timeout giữ nghĩa vụ và trạng thái kiểm tra/unknown; không đổi key để chi lại. Không cho nhập STK tùy ý. Admin xử lý issue tối thiểu có tham chiếu, không manual-finalize/revive, không chi tay khi payout còn unknown.
- Email mua có poster, suất/phòng/ghế, số ghế và combo/số lượng/đơn giá, voucher, tiền đã trả, booking code và QR CID. HTML responsive/inline CSS màu N8, plaintext, escape dữ liệu; ảnh bị chặn vẫn đọc được. Email tiếp nhận/hoàn thành/cần kiểm tra dùng cùng nhận diện; không báo tiền về trước kết quả cuối. Retry bền vững sau commit, lỗi mail/FCM không rollback vé/tiền. SMTP timeout có thể gửi trùng thư, không được tạo giao dịch lại. Nếu vé đã bị hoàn trước khi email mua gửi được, gửi biên nhận với cảnh báo vé không còn hiệu lực, không QR dùng được.
- FCM HTTPv1, một token hiện tại/user, sự kiện chứa ticketId; chạm thông báo refetch REST. Firebase chưa cấu hình thì tắt an toàn, không chặn payment/email. Private key chỉ backend `.env`, không APK; cấu hình Android chỉ là giá trị public.
- Giữ10 bảng nghiệp vụ; cho phép ngoại lệ field mới và JSON snapshot/notification retry có cấu trúc trong `payments`/`tickets` theo yêu cầu tuần9, không biến thành kho domain tùy ý. Không Redis/outbox/email queue/wallet. Dùng khóa DB, reference unique và trạng thái bền vững, không gọi provider dưới DB lock.
- Mọi khóa thật chỉ trong `.env` riêng được ignore, không source/test/MD/log/APK. Giữ bộ khóa theo yêu cầu user; đã cảnh báo rủi ro vì xuất hiện trong chat, không tự đổi. Thu/chi/email/FCM có công tắc mặc định tắt, test dùng transport giả; không có sandbox payOS riêng được coi là an toàn mặc định.
- Bàn giao gồm My Tickets thật (sắp tới/lịch sử/đơn pending), payment/refund status và UI trợ năng. Chưa kéo scanner/CRUD Staff/reminder tuần sau vào đợt này. Chỉ nói sẵn sàng UAT sau local/PostgreSQL/native/VPS gates; tiền thật do user tự mua/hoàn và kiểm tra ngân hàng hai đầu. Quỹ Bảo Kim/quyền chi, Firebase và ngân sách thử vẫn là gate bên ngoài.

Các mục/nghiệm thu trước ngày02/11 bên dưới là **lịch sử có phạm vi riêng**, không phải yêu cầu khôi phục Credit/SePay hoặc bằng chứng payOS đã chạy tiền thật. Giữ bằng chứng nguyên vẹn; trạng thái kiểm thử/deploy mới chỉ cập nhật sau kết quả quan sát.

## Nhật ký lịch sử trước02/11 (không phải đặc tả đang áp dụng)

## Thay đổi kế hoạch: bỏ Credit trong app, dùng voucher; tab Khám phá — 2026-11-01

<!-- VOUCHERS_DISCOVERY_REPLACES_CREDIT_20261101 -->
**Quyết định mới theo yêu cầu người dùng, thay kế hoạch Credit ban đầu:** từ mốc này app chỉ dùng voucher giảm giá, không còn ví hoàn tiền nội bộ, số dư, lịch sử Credit, công tắc/ô nhập Credit, tự khấu trừ hay nhãn hoàn Credit. Các mục Credit/hoàn vé nội bộ trong đặc tả và nhật ký cũ bên dưới chỉ là lịch sử, không còn là yêu cầu app đang áp dụng; không xoá bằng chứng nghiệm thu trước đây. Hoàn tiền thật/provider vẫn là nghiệp vụ riêng chưa triển khai, không biến voucher thành tiền hay số dư có thể rút.

Checkout bỏ hoàn toàn nhập tay mã ưu đãi: mở danh sách thẻ voucher có minh hoạ, điều kiện và nút chọn/bỏ; mỗi đơn một voucher, không cộng dồn. Tài khoản → **Voucher của tôi** hiển thị voucher khả dụng từ API xác thực, có tải lại và trạng thái rỗng/lỗi, không fallback dữ liệu demo khi mạng lỗi. Dùng minh hoạ vé ưu đãi riêng và ảnh combo sẵn có, không sao chép nhận diện MoMo. **FIRST50** có sẵn cho tài khoản chưa có đơn PAID/REFUNDED: giảm 50% toàn bộ vé + combo, không cap; giảm làm tròn xuống đến đồng (bill100001 → giảm50000 → trả50001). Giữ lượt dùng trên cùng order khi xác nhận; chặn đơn khác giữ FIRST50 bằng khóa user và kiểm tra trong transaction; hủy/hết hạn trả lượt, đơn đã mua/hoàn không được nhận lại. **CINE20** giảm20000 khi bill≥300000, dùng thay FIRST50, không cộng dồn. Chưa đặt hạn kết thúc cho hai ưu đãi này. Giá, điều kiện và số tiền cuối do server quyết định, không tin giá cache hoặc lựa chọn client. Giữ order/token/deadline600giây, quote chỉ đọc; retry không tự tạo đơn, không tự đánh dấu PAID/phát vé.

Tab điều hướng **Phim → Khám phá**. Bấm hiển thị “Mục Khám phá đang được chuẩn bị. Hãy quay lại sau nhé.”; chưa làm gợi ý theo thể loại/rating/hành vi, chưa thu thập hành vi hay bịa xếp hạng. Danh mục phim/Đang chiếu/Sắp chiếu ở Trang chủ giữ nguyên.

Backend thêm GET /vouchers và voucher_code cho quote, một cột orders.voucher_code/index bằng migration additive, không drop/reset hay xoá/chuyển đổi số dư cũ. Android không còn đọc/gửi/trừ Credit, kể cả DTO và demo. Cột/nhánh backend cũ chỉ giữ tương thích client/đơn cũ và hoàn đúng phần từng giữ; voucher checkout có creditUsed=0, bankAmount=totalAmount, hoàn reservation cũ nếu chuyển đơn cũ sang voucher. Không coi giữ dữ liệu lịch sử là còn ví trong app. Chưa thực hiện payOS/MB/Bảo Kim hay giao dịch tiền thật; ghi chú kế hoạch provider có sẵn tiếp tục giữ local, ngoài commit này.

Nghiệm thu mới: backend build/typecheck/format đạt,111/111 local; Linux114/114 gồm111 public+3proxy. Android assembleDebug/assembleDebugAndroidTest/testDebugUnitTest/lintDebug đạt,68/68 unit; native emulator7/7 gồm chọn/bỏ thẻ có ảnh, minimum, FIRST50 nửa bill server, giá server khác cache, back/retry không đổi deadline, demo flow/ví voucher và tab Khám phá/thông báo chuẩn bị. Lỗi assertion Unicode/selection cũ và capture dialog hai root đã sửa đúng test, giữ literal lỗi ngoài repo; không sửa production để né assertion. Đã xem ảnh emulator. Chưa chạy fixture voucher PostgreSQL/HTTPS ghi dữ liệu mới, chưa kiểm tra cạnh tranh voucher DB thật hoặc máy vật lý; các test server dịch vụ dùng mock, không nhận là E2E mua vé thật.

VPS đã backup archive hợp lệ trước migration, giữ image cũ/proxy overlay, chỉ migrate additive và recreate API thành n8-cineplex-api:vouchers-20261101: healthy/restart0; health/Swagger200, OpenAPI có /vouchers; không JWT401. Năm hash dist runtime khớp local;10bảng/110cột. Không sửa PostgreSQL/Nginx/secrets/volume, không deploy Android tự động lên máy người dùng, không tạo fixture/giao dịch thanh toán hoặc vé. Rollback chỉ trên bản sao: cùng npm test **BASELINE108 → MODIFIED111 → ROLLBACK108**, exit0;288hash baseline khớp, patch tái tạo chính xác. Probe UI baseline còn ví/ô nhập/tabPhim → modified không ví/không ô nhập/có selector/tabKhámphá → rollback trở về baseline; source active giữ modified. Bốn role/ledger và APK được kiểm tra ngoài repo. Bàn giao commit/push trên nhánh riêng feat/nguyen-vouchers-discovery theo yêu cầu; SHA/remote đối chiếu sau thực thi ngoài repo, không tự tham chiếu SHA trước commit, không main/force-push/PR/merge. Hai MD đồng bộ thay đổi kế hoạch này. Bước tiếp: nhóm review/cài APK mới; finalization/payment và kiểm thử fixture voucher thật cần gate riêng.

## Công bố checkout tích hợp tuần 8 — 2026-11-01

<!-- GROUP_WEEK8_CHECKOUT_PUBLICATION_20261101 -->
Trạng thái: **Hoàn thành công bố source checkout tích hợp tuần 8** trên nhánh `feat/manh-nhan-week8-checkout-integration`. Commit source `f0b0363a82842c927347997bd42982c2795ab377` đã push thành công (exit0), ls-remote khớp SHA; tài khoản xác thực và GitHub Author/Committer đều là Manh-Nhan299. Tên Đào Mạnh Nhân dùng noreply của chính tài khoản, không thêm đồng tác giả. Trạng thái này thay phần “chưa push” của mục bàn giao local bên dưới; giữ mục cũ làm lịch sử. Phạm vi source chỉ gồm hai file Android production, ba test files và hai MD; giữ lịch sử nền, không ghi main/force push/tự tạo hoặc merge PR.

Đợt push chỉ đối chiếu hash/source đã nghiệm thu và cập nhật hai MD; không sửa thêm logic, không chạy lại build/native/VPS/fixture DB hoặc giao dịch thanh toán. Kết quả68unit,15native regression +2native UI live và rollback64→68→64 là bằng chứng của đợt tích hợp bên dưới, không coi là lượt kiểm thử mới. Ghi chú payOS ngoài phạm vi vẫn giữ local; không đưa key/env/log/verification vào Git. Chưa xác nhận CI remote. SHA cuối nhánh sau commit xác nhận tài liệu được đối chiếu sau push và lưu ngoài repo, không tự ghi trước SHA của chính nó. Bước tiếp: nhóm trưởng review nhánh và quyết định tích hợp.

## Bàn giao source checkout tuần 8 trên nhánh riêng — 2026-11-01

<!-- GROUP_WEEK8_CHECKOUT_LOCAL_COMMIT_20261101 -->
Trạng thái: **Code và nghiệm thu checkout tuần 8 hoàn thành; bàn giao để review trên nhánh local `feat/manh-nhan-week8-checkout-integration`**. Phạm vi chỉ gồm hai file Android production, ba test files và hai tài liệu tiến độ. Danh tính commit Đào Mạnh Nhân dùng noreply của tài khoản Manh-Nhan299; không thêm đồng tác giả, giữ lịch sử nền của nhóm. Chỉ thực hiện commit cục bộ theo yêu cầu lần này, chưa push GitHub, không ghi main hoặc tạo/merge PR. Ghi chú kế hoạch payOS có sẵn vẫn giữ local, không đưa vào commit; key/env/log/verification không nằm trong phạm vi bàn giao. SHA commit được kiểm tra sau thao tác và ghi trong verification ngoài repo, không tự tham chiếu trước SHA của chính nó.

Source/test bytes khớp candidate đã nghiệm thu; không sửa thêm logic hoặc chạy lại fixture DB/VPS/native cho bước bàn giao. Giữ bằng chứng 68 unit, 15 native regression +2 native UI live và rollback64→68→64 ở mục tích hợp bên dưới, không coi là lượt chạy mới. Hai MD được đối chiếu và kiểm tra UTF-8/diff trước commit. Bước tiếp: review source; chỉ công bố remote khi người dùng yêu cầu.

## Cả nhóm tuần 8 — tích hợp checkout và ngân hàng 0đ — 2026-11-01

<!-- GROUP_WEEK8_CHECKOUT_INTEGRATION_20261101 -->
Trạng thái: **Hoàn thành tích hợp và kiểm chứng phạm vi checkout tuần 8; chưa có thanh toán/phát vé thật**. Giữ luồng live có sẵn: giữ ghế → chọn combo → quote server → checkout trên chính order, không fallback demo. Bổ sung LiveCommerceState lưu expiresAt gốc; từ chối quote đổi deadline, không đánh dấu confirmed khi checkout trả deadline hoặc subtotal/discount/totalAmount/creditUsed/bankAmount lệch quote. Phản hồi checkout không khớp giữ trạng thái unknown và token cũ để kiểm tra lại, không tạo order mới. Checkout chưa có quote hiển thị “—” cho giá bắp nước, không lấy giá cache làm breakdown cuối. Chỉ giá tạm tính bước combo được dùng cache danh mục. Ca bankAmount=0 hiển thị ngân hàng 0đ, đơn vẫn chờ xử lý/chưa có vé; không yêu cầu chuyển khoản hoặc giả báo PAID. Ẩn thông báo xác nhận cũ khi hold không còn active.

Bằng chứng mới local: Gradle assembleDebug/assembleDebugAndroidTest/testDebugUnitTest/lintDebug đạt; **68/68 unit**, không fail/error/skip. Trước sửa, thêm regression tái hiện hai lỗi deadline/tổng tiền (68 tests/2 fail), sau sửa 68/68 đạt; không sửa test để bỏ qua hành vi. Unit bao phủ giá cache 100000 nhưng quote 110000, tổng sau giảm/Credit/bank server, giữ original deadline qua back/quote/confirm và hết hạn đúng mốc600, cùng token khi mất phản hồi ở ca0đ. Native regression **15/15** gồm LiveCommerce3, SeatMap7, DetailSchedule3, commerce demo1 và đọc combos HTTPS1 đạt; hai native mới kiểm chứng server total khác cache, nhánh0đ/unknown retry và không reset countdown sau120giây giả lập. Lượt native đầu lỗi do assertTextContains mặc định so khớp toàn bộ; đã giữ lỗi và đổi đúng substring=true, không sửa production để né assertion.

Theo chấp thuận fixture mới cho riêng đợt này, đã chạy thêm **2/2 native UI HTTPS/PostgreSQL thật**, không chỉ repository: ca vé240000 + combo100000 − CINE20 20000 − Credit150000 = ngân hàng170000; ca vé100000 − Credit100000 = ngân hàng0, còn số dư50000. Cả hai kiểm tra UI từ quote server, back/tiếp tục/confirm/retry giữ nguyên order/expiresAt, TTL ban đầu xấp xỉ600giây theo response server, trạng thái vẫn PENDING_PAYMENT và ghế HELD; cancel hoàn đúng số dư150000. Trước cleanup payments0/tickets0; UUID tài khoản/phim/phòng/hai suất/hai ghế riêng đã dọn sạch ở cả lượt lỗi assertion và lượt cuối. Đã xem ảnh emulator thật cho checkout170000 và0đ; chưa so pixel nhiều viewport hoặc thử máy vật lý.

Rollback trên bản sao: **BASELINE64 → MODIFIED68 → ROLLBACK64 unit**, cùng lệnh/input, exit0; ROLLBACK.sh --group-checkout đã thực thi, bảy hash baseline khớp và file native live mới được bỏ đúng phạm vi. Native UI baseline sau rollback **1/1** đạt; emulator được cài lại APK modified sau kiểm tra rollback. Bốn role hiện có được mở rộng bằng GROUP_CHECKOUT_BASELINE.zip, mode rollback mới, patch, ảnh và literal outputs trong D:\NT118\verification\manh-nhan-week8-combo-20261101; không thay hoặc xóa kết quả/mode commerce trước đó.

Không sửa backend/API/schema/dependency, không deploy/restart VPS; kiểm tra đọc-only xác nhận image manh-nhan-week8-combo-20261101 vẫn healthy/restart0, StartedAt2026-11-01T14:26:39.692082193Z không đổi, health/Swagger/combos HTTPS200, 10 bảng/109 cột và9combo. Backend108/Linux111 và cron600 giây thật ở mục trước là lịch sử, không nhận là lượt chạy mới. Hai MD đồng bộ; không tự đánh dấu Excel hoặc công bố source mới. Giữ ghi chú payOS local ngoài phạm vi. Bước tiếp: nhóm review/trải nghiệm checkout tích hợp; payment/finalization (kể cả mua vé0đ) vẫn thuộc tuần9.

## Công bố Combo API tuần 8 của Đào Mạnh Nhân — 2026-11-01

<!-- MANH_NHAN_WEEK8_COMBO_PUBLICATION_20261101 -->
Trạng thái: **Hoàn thành công bố source Combo API/test tuần 8** trên nhánh `feat/manh-nhan-week8-combo-api`. Commit source `92066df1ddbc52d7e8baae349cd58aef569a02a5` đã push thành công (exit0), ls-remote khớp SHA; GitHub xác nhận Author/Committer đều thuộc `Manh-Nhan299`. Tên Đào Mạnh Nhân dùng noreply của chính tài khoản; không thêm đồng tác giả. Chỉ gồm hai file metadata API, ba test files và hai tài liệu tiến độ; giữ lịch sử nền của nhóm. Không ghi lên main, force push, tự tạo hoặc merge PR.

Gate công bố mới: local build/typecheck/format và **108/108 tests** đạt. Chỉ sửa encoding phần thông báo nhiệm vụ và cập nhật hai MD; không thay thêm source nghiệp vụ hoặc Android. Kế hoạch payOS có sẵn và ghi chú tương ứng được giữ local, không đưa vào nội dung công bố. Không đưa key/env/log/verification vào Git. Không chạy lại VPS/native/fixture DB hoặc giao dịch thanh toán trong đợt công bố này; kết quả Linux111, Android64/3 và E2E600 ở mục nghiệm thu bên dưới là bằng chứng của lượt trước, không phải lượt chạy mới. Chưa xác nhận CI remote. Commit xác nhận tài liệu không tự ghi trước SHA của chính nó; SHA cuối nhánh được đối chiếu sau push và lưu trong verification ngoài repo. Bước tiếp: nhóm trưởng review nhánh và quyết định tích hợp.

## Combo API tuần 8 của Đào Mạnh Nhân — 2026-11-01

<!-- MANH_NHAN_WEEK8_COMBO_20261101 -->
Trạng thái: **Hoàn thành phạm vi Combo API và kiểm thử tuần 8 trên local/VPS**. Kế thừa GET /combos, quote/checkout và transaction pricing/Credit đã có của Nguyên; không nhận phần nghiệp vụ kế thừa là phần viết mới. Bổ sung schema response danh mục trong Swagger và mô tả cặp combo_id/quantity. Thêm test HTTP danh mục public/read-only/rỗng/status/giá chuỗi/field projection/module wiring/OpenAPI; mở rộng HTTP quote cho quantity sai/biên 1,9/UUID uppercase/unknown/field injection; ma trận pricing/CINE20/Credit/giới hạn tiền; đổi quantity 1→9→1→0 chỉ điều chỉnh reservation delta trên cùng order/deadline. Không sửa thuật toán production, schema/migration/dependency, UI hoặc danh mục demo.

Bằng chứng mới: local build/typecheck/format/Prisma validate đạt; **108/108 tests**, không fail/error/skip. Linux tools candidate giữ proxy overlay, network-none và theme readonly: **111/111** (108 public +3 proxy) đạt. Lượt đầu 110/111 do TRUST_PROXY được nạp theo cách Docker env-file khác dotenv; sửa tham số TRUST_PROXY=false riêng gate fixture, không sửa test/source hoặc cấu hình API để né lỗi. Android assembleDebug/assembleDebugAndroidTest/testDebugUnitTest/lintDebug đạt; **64/64 unit**, native **3/3** gồm LiveCommerceUiTest, đọc combos HTTPS và checkout xác thực bằng fixture mới; runner đều OK, không dùng riêng exit của adb làm bằng chứng. Chưa chạy lại toàn bộ native suite hoặc máy vật lý.

VPS image `n8-cineplex-api:manh-nhan-week8-combo-20261101`, healthy/restart0/startup-success/0 ERROR-FATAL; health, Swagger và OpenAPI HTTPS 200, schema combo mới đã kiểm tra. **4/4 hash dist runtime** khớp local theo byte. So với release trước chỉ hai file production thay metadata; source nghiệp vụ và proxy overlay giữ nguyên. Đã kiểm tra archive backup hợp lệ, giữ image trước; chỉ recreate API, không thay PostgreSQL/Nginx/secrets/volume hoặc migrate/reset/restore. Key gốc không đổi, dùng bản sao ACL riêng ngoài source. Lỗi ACL key, encoding stdout, JAVA_HOME, interpreter rollback và cờ compose runner trước đó được giữ trong verification; lượt cuối đã sửa lệnh và kiểm chứng thực tế.

E2E PostgreSQL/HTTPS mới: 340000−20000−150000=170000; checkout lặp không trừ thêm, hủy đồng thời/lặp chỉ hoàn một lần; hai đơn khác suất tranh cùng Credit có một thành công/một 409, số dư không âm. Cron tự EXPIRED và hoàn Credit sau 600 giây thật, quan sát 2026-11-01T14:37:45.755Z; không sửa clock/expires_at hoặc GET order kích lazy expiry lúc chờ. Ca bank_amount=0 vẫn PENDING_PAYMENT, không thanh toán/phát vé thật. Fixture chỉ UUID/run ID riêng được chấp thuận cho đợt này, không nạp lại danh mục cố định; cleanup Node và Android đạt, số dư Android 150000 trước cleanup. Đối chiếu sau cleanup không còn user/movie/room/slot/seat/order/combo của lượt Node, vẫn **10 bảng/109 cột và 9 combo**.

Rollback bản sao: **BASELINE105 → MODIFIED108 → ROLLBACK105**, cùng npm test, exit0; script đã thực thi, sáu hash pristine khớp. Candidate mới vẫn giữ nguyên; patch tái tạo bảy file và ZIP đúng byte. Bốn role MODIFIED_FILE.zip/DIFF_FILE.patch/VERIFICATION.txt/ROLLBACK.sh được mở lại trong `D:\NT118\verification\manh-nhan-week8-combo-20261101`. Hai tài liệu đồng bộ, không tự đánh dấu Excel. Giới hạn: chưa kiểm tra máy vật lý/so pixel đa viewport; payment/finalization/vé/self-refund PAID và chuyển provider không thuộc đợt này. Bước tiếp: nhóm trưởng review phần Combo API/test; tiếp tục payment/finalization ở tuần 9 theo Plan.

## Công bố phần tuần 8 của Trần Thanh Nguyên — 2026-11-01

<!-- NGUYEN_WEEK8_PUBLICATION_20261101 -->
Trạng thái: **Hoàn thành công bố source tuần8** trên nhánh `feat/thnguyen-week8-commerce`. Commit source `cb633f5a4be19d6f8207e8837c9c5ca67425a32d` đã push thành công (exit0), ls-remote khớp SHA. GitHub xác thực `thnguyen290106`; Author/Committer Trần Thanh Nguyên dùng noreply cùng tài khoản, không thêm đồng tác giả. Phạm vi chỉ gồm pricing/order/Credit tuần8, Android live và UI combo tách checkout đã kiểm chứng, cùng hai tài liệu tiến độ. Các kế hoạch chuyển đổi chưa thực hiện và ghi chú tương ứng được giữ local, không đưa vào commit. Không ghi lên main, không tự tạo/merge PR.

Gate local mới: backend build/typecheck/format và105/105 tests đạt. Android assembleDebug/lint đạt; testDebugUnitTest đã thực thi lại,64/64 không fail/error/skip. Không chạy lại native/VPS/fixture DB chung hoặc giao dịch thanh toán trong đợt công bố; các kết quả VPS108/native trước đó vẫn là lịch sử có phạm vi riêng. Chưa xác nhận kết quả CI remote. Không đưa key/env/log/verification vào nhánh. Bản ghi xác nhận này chỉ cập nhật tài liệu và sửa encoding của thông báo mới; SHA cuối của nhánh được kiểm tra sau push và lưu ngoài repo. Bước tiếp: nhóm trưởng review nhánh và quyết định tích hợp.


## Kế hoạch payOS + MB + Bảo Kim — 2026-11-01

<!-- PAYOS_MB_BAOKIM_E2E_PLAN_20261101 -->
**Hoàn thành lập tài liệu; chưa triển khai chuyển đổi.** Theo yêu cầu người dùng, đã lập kế hoạch end-to-end tại `D:\NT118\ytuong\N8_Cineplex_PayOS_MB_BaoKim_E2E_Plan.md`: đăng ký/xác thực payOS–MB–Bảo Kim, quỹ hoàn/phí, gate duyệt tài khoản và thử thu–chi, voucher thay Credit, thiết kế API/dữ liệu đề xuất, kiểm thử local/Android/VPS, vận hành và rollback. Hướng đích là MB nhận tiền và Bảo Kim làm nguồn chi qua payOS; không phải tự động trích MB để hoàn. Chưa đăng ký/định danh/kích hoạt/chuyển tiền, chưa thay source/schema/API/runtime hoặc deploy trong đợt lập plan. Các yêu cầu provider và ngoại lệ field payments cần xác nhận/duyệt trước thực hiện; nội dung đề xuất chưa thay đặc tả đang chạy. Giữ lịch sử Credit và các kết quả cũ, không coi plan là nghiệm thu thanh toán. Đã kiểm tra cấu trúc Markdown, bản sao baseline/modified/rollback và hash tài liệu; không cần build lại cho đợt chỉ tài liệu. Bước tiếp: Nguyên xác nhận hồ sơ và quyền chi theo G0–G3; không tự mở giao dịch thật.


## UI chọn combo có ảnh và checkout tách bước — 2026-11-01

Trạng thái: **Hoàn thành chỉnh UI live theo phản hồi người dùng**. Thay danh sách kỹ thuật bằng thẻ full-width có ảnh, tên/vị/cỡ, giá VND có dấu phân cách và nút −/+ ngay từng thẻ; tổng vé + bắp nước cập nhật ngay theo danh mục server. Thanh tạm tính và Tiếp tục cố định dưới màn hình, không lẫn ưu đãi/Credit ở bước chọn món. Giảm số lượng về0 bỏ món, tối đa9; chọn loại khác có xác nhận thay, vẫn đúng giới hạn một loại/đơn. Không copy banner/giá quảng cáo thương hiệu khác; dùng lại ba ảnh minh hoạ đóng gói từ design đã có, ưu tiên imageUrl server nếu có và fallback khi thiếu/lỗi.

LiveCommerceStep.COMBOS → CHECKOUT: Tiếp tục tự lấy báo giá, không cần nút “Lấy báo giá máy chủ”; checkout mới hiển thị mã ưu đãi/Credit/tổng cuối từ server. Bắp nước ở breakdown lấy theo subtotal báo giá trừ giá ghế đã giữ khi có quote, không tin giá cache làm giá cuối. Ẩn bàn phím sau Áp dụng; quay lại giữ giỏ/order/deadline. Giá tạm tính không ghi DB/giữ Credit; chỉ xác nhận checkout mới gọi transaction hiện có. Unknown giữ token để retry, không tạo order/attempt mới. Nút số lượng có nhãn trợ năng; ảnh có mô tả; tiêu đề dài không tràn.

Kiểm chứng: Gradle assembleDebug/assembleDebugAndroidTest/testDebugUnitTest/lintDebug đạt; **64/64 unit**, lint0 errors/51 warnings/6 hints. Native regression **13/13** (LiveCommerceUiTest, demo cũ, SeatMap7, DetailSchedule3 và GET combos HTTPS thật) đạt; lượt chốt riêng LiveCommerceUiTest **1/1** sau tinh chỉnh nhãn trợ năng/format0/breakdown đạt. Đã xem hai ảnh chụp emulator cho chọn món/checkout. Native kiểm tra ảnh, −/+, tổng cập nhật, đổi món có xác nhận, quay lại không đổi deadline, quote tự động/read-only, CINE20 và không tự hủy hold sau xác nhận. Unit thêm cap9/giảm0, số tiền ghế không cộng combo cũ lần hai, không gọi mạng mỗi lần đổi số lượng, không ghi Credit trước xác nhận, giữ retry token cũ.

Rollback trên bản sao: baseline UI63 → modified64 → rollback63 unit; UI cũ native1/1 sau rollback, bảy hash trước sửa khớp. Emulator đã cài lại APK mới và mở MainActivity. Bộ bốn role cũ được mở rộng thêm UI baseline, ảnh và mode ROLLBACK.sh --combo-ui; mode rollback toàn bộ tuần8 và bằng chứng99→105→99 vẫn giữ. Đợt này đổi hai file Android production, hai test files và hai MD; 56 file backend trong candidate trước khớp byte. Không sửa API/backend/schema/dependency/dữ liệu hoặc deploy lại VPS. Kết quả backend105/VPS108, Android38 ở mục trước là lịch sử đã nghiệm thu, không phải lượt chạy mới của đợt UI này.

Giới hạn: ảnh là minh hoạ; chưa thử máy vật lý/so ảnh nhiều viewport. Thanh toán/vé thật vẫn ngoài phạm vi tuần8; xác nhận đơn không có nghĩa đã mua vé. Hai tài liệu đã đồng bộ; bước tiếp là người dùng trải nghiệm UI mới và phần payment tuần9 theo Plan.

## Backend pricing/order/Credit và Android thật tuần 8 — 2026-11-01

Trạng thái: **Hoàn thành phạm vi tuần 8 đã chốt; chưa có thanh toán/phát vé thật**. Giữ POST /orders là bước hold ghế chưa trừ Credit; thêm POST /orders/:id/quote (không ghi dữ liệu) và POST /orders/:id/checkout (xác nhận báo giá ký HMAC, điều chỉnh reservation Credit trong transaction trên cùng order/deadline). Tự dùng tối đa Credit theo số dư cộng phần đã giữ cho chính đơn; balance += creditUsed cũ − mới. Retry báo giá đã áp dụng không trừ thêm; đổi giá/số dư/snapshot trả 409 QUOTE_CHANGED; hủy/expiry hoàn đúng một lần. Giữ DB clock/cutoff, Couple, ownership và 600 giây. Không thêm bảng/cột/migration/dependency.

CINE20: trim/uppercase, giảm cố định 20.000đ khi subtotal >=300.000đ; total_amount là tổng sau promotion trước Credit. Ví dụ ghế 240.000 + combo 100.000 − 20.000 =320.000; số dư 150.000 → creditUsed150.000/bankAmount170.000. Thêm GET /combos và creditBalance ở GET /me. Theo lựa chọn người dùng, nạp 9 combo hoàn chỉnh theo tổ hợp vị/cỡ, UUID riêng cố định, 0–1 loại/đơn và quantity1–9 (không combo null/0); không bảng product/item/option hoặc CRUD Admin. Đây là phần tối thiểu giao nhau với Combo API của Đào Mạnh Nhân. Danh mục demo còn lại trên DB theo chấp thuận, khác fixture test phải cleanup.

Android dùng repository thật, danh mục/báo giá/số dư server, Credit tự áp dụng; demo chỉ còn ở nhánh preview/test, không fallback khi mạng lỗi. Nhánh thật không chạy timer thành công, không hiện QR demo hoặc tự hủy hold sau xác nhận. Timeout giữ token/đơn để kiểm tra lại; giỏ đổi phải báo giá lại. Ví thật chưa cung cấp lịch sử, không lấy số dư từ ledger mẫu.

Bằng chứng cuối: local backend build/typecheck/format và **105/105 tests** đạt. VPS tools Linux, proxy overlay giữ nguyên, network-none/theme readonly: **108/108** đạt (105 public +3 proxy). Android Gradle build/unit/lint đạt, **63/63 unit**, native toàn bộ **38/38** trên Pixel 10 Pro XL, không fail/skip; có repository Android gọi HTTPS thật, checkout/retry/cancel và số dư thật trên fixture. Lint 0 errors/51 warnings/5 hints. Lượt UI lỗi trước (assert nhãn cũ, 502 lúc recreate và lỗi không ổn định ở suite cũ) giữ lịch sử; sửa assertion đúng màn combo, sửa UTF-8 của pipeline, cài lại APK và chạy toàn bộ cuối đạt. Không dùng exit0 của adb thay cho kết quả runner: đã kiểm tra OK (38 tests).

VPS: image candidate `n8-cineplex-api:nguyen-week8-commerce-20261101-utf8`, 7/7 hash dist runtime khớp local theo byte; healthy/unless-stopped/restart0, startup-success, 0 ERROR/FATAL, health/Swagger200, OpenAPI đủ route mới. Backup trước deploy hợp lệ; chỉ recreate API, giữ PostgreSQL/Nginx/secrets/proxy/volume và image cũ, không migrate/reset/restore/prune. Vẫn 10 bảng/109 cột, danh mục9. Truy cập bằng key .pem người dùng chỉ định qua bản sao ACL riêng vì quyền key gốc quá rộng; không sửa key gốc hoặc đưa key/credentials vào source.

E2E HTTPS/PostgreSQL thật: ví dụ340000−20000−150000=170000; checkout lặp không trừ thêm, hủy đồng thời/lặp chỉ hoàn một lần; hai đơn khác suất tranh cùng số dư có một thành công/một409, không âm số dư. Cron tự expire và hoàn sau ít nhất600 giây thật, không sửa expires_at/clock hoặc đọc GET order để kích lazy expiry khi chờ; quan sát lúc09:42:06Z. Ca bank_amount0 vẫn pending, payments/tickets0; cleanup chính xác UUID của cả lượt Node và Android đạt, kiểm tra số dư Android150000 trước xóa. WebSocket HTTPS/TLS thật, JWT hợp lệ JOIN và từ chối room tùy ý đạt. Rollback bản sao: BASELINE99 → MODIFIED105 → ROLLBACK99; pristine hashes và patch tái tạo bytes được kiểm tra ngoài repo. Không rollback API/DB thật.

Giới hạn chủ động: cả bank_amount0 và >0 vẫn PENDING_PAYMENT; chưa bán ghế, payment, vé/QR thật, email/FCM hoặc self-refund vé PAID. Finalization mục13.2 là mục tiêu tuần9 chưa triển khai, không coi mua vé0đ hoàn tất. Chưa thử Android vật lý hoặc so pixel nhiều viewport. Bước tiếp: phần payment/finalization tuần9 theo Plan; không tự đánh dấu Excel.

## Đối chiếu source UI Combo / Checkout / Credit — 2026-11-01

Trạng thái: **Hoàn thành cập nhật Android trong phạm vi mô phỏng**. Đã đọc lần lượt `design/combo-checkout/CHECKOUT-COMPONENTS.md`, `index.html`, `checkout/checkout.js`, `checkout/checkout.css`, `checkout/assets/` làm nguồn; không dùng `ui.zip` hay `design.zip`. UI Compose đã điều chỉnh theo source: intro “Phim hay, thêm bắp.”, tiến trình đặt vé, tab lọc Tất cả/Combo/Món lẻ, bỏ combo có xác nhận, bảng màu commerce `#F6F7F9/#FFFFFF/#20222A/#5D6470/#8C2945/#F5E8EC/#D9DDE4`, ảnh giữ tỷ lệ Fit. Phô mai là lựa chọn vị hết hàng chứ không phải sản phẩm thứ năm. Chỉ bắp riêng cỡ lớn phụ thu 10.000đ; combo 2 người bắp lớn chuẩn giá 100.000đ. Đổi giỏ/ghế khiến CINE20 được xét lại và Credit đã chỉnh được hạ về cap mới; input sai vẫn giữ lỗi và khóa thanh toán. Hỗ trợ số điện thoại 0xxxxxxxxx hoặc +84xxxxxxxxx. Ví Credit có trạng thái rỗng, phân biệt pending/posted/failed/used, mã đơn/thời gian và chi tiết đề nghị/khấu trừ mẫu; nạp ví dụ chỉ trong debug. Ba ảnh Android khớp hash nguồn `checkout/assets/`. Vì Plan quy định một combo type + quantity cho một order, Android giữ một loại sản phẩm tối đa 9 phần dù prototype web hỗ trợ nhiều dòng giỏ; chưa đổi API/schema. Build Gradle Wrapper `assembleDebug testDebugUnitTest connectedDebugAndroidTest` đạt: 61 unit tests (tăng từ 60 ở mục dưới), CommerceFlowUiTest 1/1 và SeatMapUiTest 7/7 sau lần chỉnh UI trên emulator Pixel 10 Pro XL. Chưa so pixel đa viewport, chưa thử máy vật lý, chưa có payment/credit/vé backend thật. Giữ mọi thay đổi chưa commit; không commit/push/merge. Bước tiếp: khi backend commerce sẵn sàng, thay repository demo và chạy E2E có thanh toán/ledger thật.

## Tiến độ Android Combo / Checkout / Refund Credit UI — 2026-11-01

Trạng thái: **Hoàn thành UI trong phạm vi mô phỏng; chưa có giao dịch backend thật**. Thiết kế `design/combo-checkout/` được port sang Compose hiện có, không dùng ZIP lồng hoặc WebView. Luồng giữ ghế thật → chọn một combo/sản phẩm (0–9, tùy chọn vị/cỡ, hết hàng/bỏ qua) → checkout → xử lý/kết quả → vé mẫu. `SeatSelectionViewModel` là nguồn ghế, đơn và deadline 600 giây, không tạo lại hạn khi chuyển màn hình. `CommerceState` cùng `DemoCommerceRepository` tách UI khỏi dữ liệu mô phỏng. `PriceQuote` dùng VND nguyên tính vé + combo + phí 0đ − CINE20 hợp lệ − Credit; số dư từ ledger posted, pending/failed không cộng; giới hạn Credit theo số dư và tổng sau giảm. Ví dụ Couple 240.000đ + Combo 100.000đ − 20.000đ − Credit 100.000đ = còn trả 220.000đ; sau thành công mẫu còn 50.000đ. Snapshot giữ orderId thật của đơn giữ ghế, attemptId riêng, rạp/suất/ghế/combo/giá/liên hệ; kết quả unknown không tạo payment mới, ghi giảm Credit tối đa một lần/orderId. Vé dùng QR native ZXing với payload `CINEPLEX-DEMO:<orderId>`, ghi rõ không có giá trị soát vé. Backend hiện chỉ nhận đơn giữ ghế không combo/payment/credit; chưa gửi email, thanh toán, phát vé hoặc hoàn tiền thật. Demo thành công hủy đơn giữ ghế ở backend để không để ghế bị giữ vô ích; không đánh dấu order PAID. Chưa thay backend/schema/API. Gradle Wrapper `assembleDebug testDebugUnitTest` đạt, 60 unit tests trong đó 5 CommerceModelTest; emulator Pixel 10 Pro XL: CommerceFlowUiTest 1/1 đạt gồm luồng unknown → kiểm tra lại cùng attemptId/không trừ Credit trước kết quả, SeatMapUiTest 7/7 đạt. Chưa kiểm tra máy vật lý hoặc so pixel toàn bộ viewport. Chưa commit/push/merge. Bước tiếp: khi API commerce có sẵn, thay repository mẫu, nghiệm thu payment/ledger/vé thật và kiểm tra ảnh trên nhiều cỡ màn hình.

## Công bố tài liệu triển khai PR #18 — 2026-10-26

Trạng thái: **Hoàn thành công bố hai tài liệu theo yêu cầu người dùng**. Nội dung nghiệm thu deploy đã commit/push tại `8eb718bd89552320e316ee015131a0851f162950` trên nhánh `docs/manh-nhan-pr18-vps-deployment`; push exit 0 và ls-remote khớp SHA. Chỉ thay `N8_Cineplex_Plan.md` và `AGENTS.md`, danh tính commit Đào Mạnh Nhân <24521227@gm.uit.edu.vn>; không push main/force push/tự tạo hoặc merge PR. Hai tài liệu đã đọc lại, đồng bộ tiến độ và kiểm tra diff whitespace. Không build/deploy/API hoặc ghi DB thêm vì chỉ thay tài liệu; giữ nguyên bằng chứng runtime phía dưới. Commit chốt trạng thái này được kiểm tra SHA/remote và ghi trong ledger ngoài repo sau thực thi, không tự tham chiếu SHA trước. Bước tiếp theo: người dùng/nhóm review nhánh và quyết định tạo/merge PR.


## Triển khai PR #18 lên VPS — 2026-10-26

Trạng thái: **Hoàn thành triển khai backend PR #18 và các kiểm tra trong phạm vi deploy**. Mốc này thay trạng thái chưa build/test/deploy phần realtime/expiry trong các nhật ký cũ phía dưới; giữ lịch sử, không nhận toàn bộ nghiệp vụ tương lai đã hoàn thành. PR #18 đã merge; source chốt tại `f4d5c3b49c220a735f2f4d05c1ba5faa96bde42a`. Không tạo commit backend mới hoặc push main trong lượt này.

- Phạm vi: triển khai source đã merge, giữ ShowtimeSubscriptions và expiry dùng clock PostgreSQL; không sửa code nghiệp vụ/Android/schema/dependency. Source local vẫn mặc định localhost. VPS checkout `/opt/n8-cineplex` đúng SHA; release cùng SHA có đúng 4 file proxy overlay hiện hành ngoài source public (config, crawler service, smoke, crawler tests). Không thay secrets hoặc TLS/allowlist/fail-closed.
- Local: build/typecheck/format:check đạt; public **99/99 tests**, candidate proxy **102/102 tests**, exit 0. VPS Linux tools cùng candidate, network-none và theme Android mount readonly: **102/102 tests**, exit 0. Lượt Linux trước 101/102 do thiếu theme ngoài image đã sửa bằng mount fixture, không sửa test/source để né lỗi. Lượt bị ngắt không tính đạt; đã chạy lại gate và quan sát exit 0.
- VPS thực thi `compose up --no-deps --no-build --wait api` đạt. API image `n8-cineplex-api:f4d5c3b49c220a735f2f4d05c1ba5faa96bde42a`, image ID `sha256:7356a36745b0ee1582b00456d68d873d8466ef43e93b0fdff4af2ab768324642`, StartedAt `2026-10-26T03:30:13.815253302Z`; healthy, restart unless-stopped, RestartCount 0. Startup thành công, không có ERROR/FATAL trong khoảng kiểm tra. Hash 5 file dist gateway/service/subscriptions/config/crawler khớp tools đã test.
- PostgreSQL/Nginx giữ container ID/StartedAt, private env không đổi; 5000 không public, PostgreSQL chỉ localhost. Prisma xác nhận **4 migrations, schema up to date**; không migrate/seed/reset/restore hoặc tạo fixture booking. Giữ image cũ để rollback, không prune volume/network/cache; còn khoảng 15 GB trống lúc kiểm tra.
- HTTPS: health 200 `{"status":"ok","database":"up"}`, Swagger/OpenAPI 200 và đủ route Orders/seats; coming-soon 25 item tại snapshot. Catalog → detail → seats thật đạt với inventory GOLD 32, không lộ holder/deadline; order thiếu JWT trả 401. Socket.IO qua HTTPS WebSocket kiểm tra TLS bình thường, JWT sai trả AUTH_REQUIRED. Crawler smoke trong runtime qua proxy trả CGV HTTP200 lúc `2026-10-26T03:31:32.012Z`; không import ghi DB.
- Transaction cấu hình: chỉ đổi services.api.image/build.context và services.tools.image/build.context. Cùng lệnh `compose config --images`: BASELINE 2f2547b → MODIFIED f4d5c3b → ROLLBACK bản sao 2f2547b, cả ba exit 0; pristine hash khớp, patch tái tạo bytes đạt, active/modified vẫn f4d5c3b. Đây là rollback bản sao cấu hình, không hạ API production/restore DB. Đã mở lại bốn role ngoài repo. Có supplemental live-image rollback vì compose cũ tham chiếu 2f2547b nhưng API thực trước deploy chạy f6af43b.

Giới hạn: chưa chạy lại concurrency PostgreSQL/E2E600s trên DB chung hoặc Android vật lý; tests service dùng fixture không chứng minh concurrent commit PostgreSQL thật. Không tái dùng chấp thuận fixture run cũ. Hai MD cập nhật đồng bộ local, chưa commit/push tài liệu. Bước tiếp: review/công bố tiến độ trên branch riêng khi người dùng yêu cầu; nghiệm thu fixture live mới cần chấp thuận riêng. Không còn bước bắt buộc đang treo trong phạm vi deploy PR #18.


## Đối chiếu GitHub và gói backend còn thiếu — 2026-10-26

Đã đọc log CI head cab9895, run40216825084: npm ci/Prisma validate/build/typecheck/format đạt, npm test82/83. Bài Auth HTTP thất bại với JWT_ACCESS_SECRET vì AppModule static import chạy trước khi test gán fixture env. Nhánh remote hiện vẫn dùng backend cũ; auth.test.mjs dynamic import và các bổ sung còn local. CI này không kiểm chứng bản sửa mới. Bước tiếp theo phải upload9file còn thiếu rồi xem kết quả CI mới trước khi review merge; chưa ghi bản sửa đã build/test/VPS thành công.

Trạng thái: **Hoàn thành đối chiếu source; chưa nghiệm thu toàn bộ backend/VPS**. Đã đọc trực tiếp GitHub main, lịch sử path backend, PR17, commit feature/manh-nhan, compare và recursive trees; không dựa vào nhãn thời gian tương đối "10 hours".

### Người tạo các cập nhật và trạng thái nhánh

- Main hiện `e12b0e6afe83ac11ca50be9902d2db46e8a60346`. Commit mới nhất thay đổi backend là `f6af43bf81e6a90b97fa25d48cd3c9a8c77f31d7`, tác giả/committer Trần Thanh Nguyên (`thnguyen290106`), nội dung thêm CGV coming-soon crawler/API lúc16:52:02Việt Nam25/10/2026. PR17 được cùng tài khoản merge lúc17:27:29. Dữ liệu này xác nhận tác giả commit và người merge, không xác định riêng người thực hiện mọi lần push hoặc quyền sở hữu toàn bộ backend.
- Feature/manh-nhan đã xuất hiện, head `cab9895c672cd7722bd082fb4671447133febf6e`, tác giả Dao Manh Nhan/Manh-Nhan299, committer GitHub web-flow, lúc00:30:45Việt Nam26/10. Compare main: ahead1/behind0, chỉ sửa `.github/workflows/backend-ci.yml` với13dòng thêm; blob khớp local. Người dùng đã upload workflow; agent chưa thực hiện được thao tác ghi GitHub. Mốc này thay trạng thái chưa có branch ở các lượt trước.
- Backend trên feature vẫn giống main.7file backend nhiệm vụ và2MD chưa có trên feature, không kết luận toàn bộ phần Đào Mạnh Nhân đã được đưa lên.

### Tính đúng đắn của gói đang chuẩn bị

Trong50file backend hiện có trên main,46file local khớp Git blob,4file khác có chủ đích: booking.gateway.ts ủy quyền JOIN_SHOWTIME cho policy; booking.service.ts dùng clock PostgreSQL ở bước tìm đơn hết hạn; auth.test.mjs nạp fixture env trước AppModule; booking.test.mjs bổ sung regressions và snapshot rollback credit trong DB giả.3file mới là showtime-subscriptions.ts, realtime-policy.test.mjs và realtime.test.mjs. Không đổi crawler/catalog/schema/migrations/package/lock; giữ nguyên phần coming-soon mới nhất của Nguyên. Không bổ sung worker/gateway thứ hai hoặc thay API contract/Android.

Gói còn thiếu được tách thành9file (7backend+2MD), bỏ workflow đã có để tránh commit trùng. Gói/diff/hash và báo cáo nằm ngoài source, không ghi log/raw secrets vào Git. Chỉ đưa vào feature/manh-nhan sau review, không thay toàn bộ backend hoặc upload verification lên repository. Đã chạy lại9/9policy và syntax checks đạt. Full build/typecheck/format/Socket.IO transport/PostgreSQL concurrency/VPS vẫn chưa thực hiện; kết luận đúng phạm vi source không phải bảo đảm toàn bộ runtime đã đạt. Bước tiếp theo là upload9file còn thiếu vào branch, xem CI, xử lý lỗi thực tế rồi mới review/merge theo quyết định nhóm trưởng.

## Yêu cầu xuất bản bằng Git CLI lên feature/manh-nhan — 2026-10-25

Trạng thái: **Chưa commit/push; clone Git CLI bị chặn kết nối mạng**. Người dùng yêu cầu kiểm tra Git, đưa phần nhiệm vụ Đào Mạnh Nhân lên `feature/manh-nhan` bằng Git CLI, fallback fork nếu push403; không push main/force push/reset/xóa code/tự merge. Branch mới này thay mục tiêu `feat/dao-manh-nhan-week7-realtime-expiry` của lượt trước. Chỉ đổi trigger CI cho đúng branch, không đổi business rule hoặc Android.

### Kết quả kiểm tra và bảo toàn

- Folder source hiện tại không có `.git`; git status, branch --show-current, log -1 và remote -v đều báo not a git repository. Vì vậy chưa tồn tại branch/commit/remote local cho các thay đổi, không được gọi bản ZIP là bản clone hoặc tạo root commit mới để thay lịch sử nhóm.
- Trước cập nhật đã lưu238file source cùng hash ngoài repo, giữ nguyên bundle cũ và toàn bộ folder làm việc. Phần nhiệm vụ gồm10file: gateway/service/subscription policy, regressions booking/realtime/Auth, workflow và cả hai MD. Cache/dependencies/build/IDE/key/log không thuộc thay đổi đưa lên Git.
- Đã thử clone thật bằng Git CLI `git clone --no-checkout --single-branch --branch main` vào đích mới. Lệnh thất bại kết nối github.com443 trước xác thực; không phải HTTP403. Không có clone hợp lệ để chạy bước tạo feature branch/commit/push. Không thực hiện git init/reset/force push hoặc tự merge. GitHub main đọc lại vẫn e12b0e6afe83ac11ca50be9902d2db46e8a60346.
- GitHub xác nhận user Manh-Nhan299 có push=true, repo public/allow_forking=true. Repo cá nhân Manh-Nhan299/N8_Cineplex-Project trả404; chỉ kết luận chưa xác nhận fork tồn tại/có thể truy cập. Chưa tạo fork hoặc PR. Kết nối ứng dụng từng bị403 ở thao tác ghi, nhưng điều đó không chứng minh Git CLI thiếu quyền của user. Chưa có branch/commit/PR mới hay SHA xuất bản.

### Quy trình tiếp nối an toàn

Chuẩn bị script ngoài source để clone đúng upstream, kiểm tra base SHA đã review và dừng nếu main đổi, tạo feature/manh-nhan, copy đúng10file đã đối chiếu và chuẩn hóa LF, kiểm thử, cấu hình danh tính Đào Mạnh Nhân <24521227@gm.uit.edu.vn>, commit và push chỉ nhánh này. Nếu clone/test/push thất bại thì dừng và giữ nguyên checkout/thay đổi/commit đã tạo, không xóa hoặc reset. Hỗ trợ push sang remote fork đã tồn tại; nếu chưa có fork thì người dùng tạo trên GitHub trước và mở PR base Tonyisded/main, compare Manh-Nhan299/feature/manh-nhan. Không tự merge hay đổi quyền/token.

Bước tiếp theo phụ thuộc kết nối Git CLI: chạy quy trình trong terminal người dùng, gửi kết quả để xác nhận commit/branch/attribution/CI và cập nhật cả hai MD.9/9policy tests có phạm vi như nhật ký trước; full build/typecheck/format/transport/PostgreSQL concurrency/VPS chưa được ghi hoàn thành. Chưa sửa Excel.

## Công bố phần tuần 7 của Đào Mạnh Nhân — 2026-10-25

**Cập nhật cuối phiên:** source và bằng chứng local đã chuẩn bị; **công bố GitHub chưa thực hiện được**. API tạo blob và tạo branch đều trả HTTP403 `Resource not accessible by integration`; tài khoản có quyền push nhưng kết nối ứng dụng chưa cho phép các thao tác ghi này. Chưa tạo được branch/commit, không có SHA hoặc CI mới. Main giữ nguyên. Người dùng đã được đề nghị kiểm tra quyền ghi kết nối hoặc chọn thực thi quy trình kiểm thử/commit/push ở terminal riêng; không thay quyền ứng dụng hoặc tạo token thay người dùng.

Local policy9/9đạt. Cùng9bài kiểm tra chạy với thân JOIN_SHOWTIME gốc trích từ baseline, chỉ thay chỗ truy cập DB bằng callback fixture:3đạt/6lỗi; chạy bản sửa9/9đạt. Bài overlapping join được bổ sung event-loop turn để cả hai yêu cầu thực sự chồng lấn, tái hiện lỗi baseline thay vì tình cờ chạy tuần tự. Hai bài bổ sung kiểm tra socket khác vẫn tiến triển khi một socket đang chờ và suất không tồn tại giữ nguyên private/current room. Phép thử này chỉ kiểm chứng policy với DB/socket fixture; chưa phải Socket.IO transport, PostgreSQL cạnh tranh thật hoặc VPS. Syntax các regressions đạt; full build/typecheck/format và transport regressions vẫn chờ dependencies/quyền chạy CI. VPS chưa deploy hoặc nghiệm thu. Bộ bằng chứng và rollback ngoài repo được cập nhật theo source cuối; không sửa Excel hoặc tuyên bố toàn bộ tuần7đã hoàn tất.

Trạng thái: **Đang kiểm chứng và công bố trên branch riêng theo yêu cầu người dùng**. Mục này cập nhật tiến độ phần tích hợp bên dưới. Đã đối chiếu snapshot local với tree GitHub `main` tại `e12b0e6afe83ac11ca50be9902d2db46e8a60346`; các file source ngoài phần tuần 7 giữ nguyên, không đưa assets/build/cache/key vào thay đổi. Branch dự kiến `feat/dao-manh-nhan-week7-realtime-expiry`, tạo từ đúng base này. Kết nối GitHub xác nhận tài khoản `Manh-Nhan299`, email `24521227@gm.uit.edu.vn`, có quyền push; commit mới của phần này dùng danh tính Đào Mạnh Nhân và không thay lịch sử nhóm.

### Phạm vi bổ sung và sửa kiểm thử

- Giữ các sửa trực tiếp trên module booking hiện hành: ShowtimeSubscriptions chuẩn hóa UUID/tuần tự hóa join/leave/ngăn ghost rooms/giữ private room/giới hạn30yêu cầu mỗi phút; worker dùng giờ PostgreSQL cho bước chọn đơn hết hạn; các regressions clock-skew, expiry/cancel/credit rollback và Socket.IO. Không thêm gateway/worker thứ hai, không đổi600s/cutoff/lock order, REST/event payload, Android hoặc schema.
- Sửa test Auth vốn static-import AppModule trước khi gán fixture env. ConfigModule.forRoot được đánh giá lúc import, làm CI cũ `40184843274` fail JWT_ACCESS_SECRET dù build/typecheck/format đạt; log ghi82/83tests. Chuyển sang dynamic import sau khi gán fixture và đăng ký khôi phục env trước compile. Chỉ sửa test setup, không nới config validation hoặc dùng secret production.
- Thêm trigger push cho đúng branch tuần 7 vào workflow backend hiện có để thực thi build/typecheck/format/full tests trên GitHub Actions khi terminal local bị chặn mạng. Giữ pull_request/workflow_dispatch, quyền contents:read và mặc định không crawl live. Nếu format thất bại, runner in diff gợi ý, không commit hoặc cập nhật branch từ CI.

### Bằng chứng hiện tại và phần còn lại

Local9/9policy tests và kiểm tra cú pháp các file regression đạt. Full build/dependencies/transport tests chưa chạy local vì npm cache thiếu gói và phiên terminal bị chặn mạng; không tạo stub hoặc sửa lockfile để né lỗi. CI mới chưa được khởi chạy vì ghi GitHub bị403; SHA/remote/attribution chỉ ghi sau khi thực thi. Chưa truy cập/deploy VPS, chưa chạy lại PostgreSQL concurrency/E2E600s hoặc kiểm tra Android vật lý; giữ bằng chứng lịch sử có ngày ở các mục cũ, không nhận là lượt chạy mới. Không sửa Excel hoặc đánh dấu nghiệm thu backend/VPS đã hoàn tất.

Bước tiếp theo: công bố commit lên branch riêng, kiểm tra CI thực tế và sửa lỗi tái hiện được, xác minh remote/Author/Committer, cập nhật hai MD và cùng bộ baseline/modified/diff/verification/rollback ngoài repo. Giữ main và dữ liệu VPS nguyên trạng; khi kết nối VPS được mới triển khai theo quy tắc4-file proxy overlay, HTTPS và database dùng chung hiện hành.

## Tích hợp tuần 7 Đào Mạnh Nhân trên snapshot API/UI mới — 2026-10-25

Trạng thái: **Đang làm; chưa hoàn thành kiểm chứng tích hợp hoặc VPS**. Người dùng cung cấp folder `N8_Cineplex-Project-main (1)` và key VPS mới. Phần này ghi tiến độ phiên hiện tại; không biến các bằng chứng triển khai/E2E lịch sử phía dưới thành kết quả chạy mới.

### Đối chiếu phân công và nguồn mới

PCCV tuần7 giao Đào Mạnh Nhân realtime ghế, job expire hold và test concurrency. Snapshot mới đã có cả REST orders, Socket.IO BookingGateway, BookingService.expire và Android SeatRepository/ViewModel đang ghép thật. Lịch sử của nhóm ghi E2E600.113s đã chạy; phiên này chưa chạy lại phép thử đó. Bản nháp cũ31ff7a4 không được copy nguyên vào source mới: khác path `/api/v1/socket.io`/SUBSCRIBE_SHOWTIME và thứ tự khóa cũ có thể xung đột service mới. Giữ hợp đồng hiện hành `/socket.io/`, namespace/, JOIN_SHOWTIME nhận UUID string, user room từ JWT, REST refetch và polling10s foreground. Không thêm worker/module/dependency/schema mới hoặc sửa UI đã chốt.

### Bổ sung trực tiếp trên booking hiện có

1. BookingGateway ủy quyền JOIN_SHOWTIME cho ShowtimeSubscriptions. UUID hợp lệ được chuẩn hóa lowercase trước query và join để PostgreSQL UUID và tên phòng Socket.IO cùng một định danh. Không cho người dùng nhập tên room/user tùy ý.
2. Các yêu cầu join/leave của cùng socket chạy tuần tự; yêu cầu A rồi B không để lại cả phòngA/B khi adapter hoặc DB chậm. Chỉ bỏ showtime room, giữ socket/user room. Kiểm tra connected sau lookup, sau leave và sau join; nếu adapter hoàn tất join sau disconnect thì bỏ membership vừa tạo. Query lỗi trả ack lỗi chung và không làm hỏng các yêu cầu tiếp theo. Ngân sách30 JOIN_SHOWTIME/phút/socket, tính trước khi xếp hàng, giới hạn DB work/queue khi client gửi dồn. Ack cũ `{ok:true}` được giữ; ack lỗi có thể thêm code, không đổi các signal/payload hoặc nguồn state REST.
3. BookingService.expire lấy clock_timestamp từ PostgreSQL để tìm ứng viên thay new Date của process. Deadline/transition vốn dùng giờ DB; nếu process clock chậm, worker cũ có thể không nhìn thấy đơn đã đến hạn. Chuyển clock cho bước chọn ứng viên, vẫn recheck sau lock trước transition. Giữ deadline600s, cutoff tại start_at, quét startup/mỗi giây, running guard, batch100, giá/credit/history và transaction hiện có. Không thay khóa showtime → users UUID → orders/seats bằng thuật toán order-first của bản nháp cũ. [PostgreSQL clock_timestamp](https://www.postgresql.org/docs/17/functions-datetime.html) là giờ thực tại thời điểm gọi, khác mốc bắt đầu transaction.

### Kiểm chứng và giới hạn hiện tại

- Backup baseline đã tạo ngoài repo trước thay đổi, không chứa `.git`, node_modules, generated/build/dist hoặc private env/key. Tất cả sửa hiện tại nằm trong folder mới; không cố sử dụng đường dẫn project cũ đã bị người dùng thay thế.
- Native Node24 policy tests: **7/7 đạt**, gồm canonical UUID/private room, payload sai/unknown suất, hai join bất đồng bộ, disconnect lúc lookup/join, recovery sau lỗi và rate limit. Đây là socket/DB fixture in-memory, không phải test lock PostgreSQL hoặc transport production.
- Đã thêm3 tests Socket.IO thật trong tests/realtime.test.mjs (Auth/DB fixture),2 tests DB/app clock skew và2 tests expiry/cancel/credit-rollback trong booking.test.mjs. Hai bài bổ sung kiểm tra10 lệnh hủy tranh với expiry chỉ hoàn credit/phát signal một lần, không trả nhầm ghế đã bán lại; và lỗi cập nhật order rollback ghế/credit/events rồi tick sau retry thành công. Fixture transaction nay snapshot/restore cả creditBalance, giữ hành vi production vốn có, không triển khai credit reservation mới. Đã kiểm tra cú pháp, **chưa chạy** các bài này/build/typecheck/format/full suite vì thiếu dependencies. Không nhận các lỗi mới là đã tái hiện bằng regression trước khi baseline runner chạy được; chúng được xác định từ code review.
- npm registry EACCES trong sandbox. Đã đối chiếu cache347 package, thiếu39 entries phần lớn optional platform khác; npm ci --offline thực tế dừng ở xmlhttprequest-ssl2.1.2 và báo một số cleanup EPERM. Không sửa dependency/lock hoặc tạo stub để ép tests pass. Đang chờ người dùng chạy npm.cmd ci --no-audit --no-fund trong backend folder mới bằng PowerShell ngoài Codex.
- SSH `ubuntu@18.143.100.43` bị Permission denied khi mở TCP22, trước bước xác thực key. Đây không chứng minh key sai. Chưa vào VPS, chưa deploy/backup/migrate/seed/ghi dữ liệu hoặc phát sự kiện tới khách hàng thật. Key người dùng đưa giữ ngoài source và không bị in nội dung. Không tự dùng lại ngoại lệ fixture commit của run cũ cho phiên mới.
- Chưa commit/push/merge hoặc đánh dấu Excel hoàn thành. Khi có dependencies, chạy baseline từ archive với cùng regressions, chạy modified, verify rollback copy; sau đó build/typecheck/format/full tests và kiểm tra Android hiện hành. Backend thay đổi cần nghiệm thu VPS tương ứng khi truy cập được; giữ đúng4 proxy overlay/secrets/PG/Nginx hiện hành, không deploy source direct làm mất proxy. Nếu chưa deploy thì không báo tính năng hoàn thành VPS.

### Bước tiếp theo

Trong backend folder mới, hoàn tất npm ci theo lockfile. Generate/build dùng DATABASE_URL fixture chỉ để compile, không khởi động API giả hoặc kết nối DB chung. Core policy chạy bằng `node --test tests/realtime-policy.test.mjs`; transport regressions chạy `node --test tests/realtime.test.mjs`; booking regressions chạy `node --test tests/booking.test.mjs`; full suite vẫn npm test, không thay hoặc loại tests có sẵn. Cài Chromium Playwright hoặc dùng Chrome đã xác minh cho fixture theo quy tắc test của dự án; không gọi fixtures là CGV/VPS live. Sau khi pass local và VPS, cập nhật cả hai MD bằng số lượng/exit thực tế và commit SHA khi có. Bộ verification mới ở ngoài repo phải refresh với source cuối; rollback không restore DB hoặc tác động runtime đang chạy.

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
- dùng voucher giảm giá;
- thanh toán thật bằng payOS + VietQR;
- cập nhật trạng thái thanh toán realtime;
- nhận vé QR;
- gửi FCM khi đặt vé thành công;
- gửi email/Gmail xác nhận đặt vé thành công kèm QR vé;
- Staff scan QR để check-in;
- Staff xác nhận giao combo;
- self-refund về đúng tài khoản nguồn qua payOS/Bảo Kim;
- Admin quản lý Staff/combo và có thể hủy suất;
- Admin xử lý các payment issue hiếm gặp ở mức tối thiểu;
- bắt buộc cập nhật APK khi phiên bản quá cũ.

N8 Cineplex chỉ lấy dữ liệu phim/lịch chiếu công khai từ CGV. Vé, ghế, thanh toán, QR và hoàn tiền ngân hàng đều do N8 quản lý độc lập.

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

- payOS;
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
    +-- payOS/VietQR
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
```

Sau commit:

```text
emit SEAT_RELEASED
emit ORDER_EXPIRED tới user
```

Không reset 10 phút khi đổi màn hình.

## 9.2. Booking cutoff — chốt ngày 2026-10-25

- Không nhận **đơn giữ ghế mới** khi thời gian database `clock_timestamp() >= showtimes.start_at`; không áp dụng mốc dừng trước giờ chiếu 10 hoặc 15 phút.
- Suất và phòng phải ACTIVE. Kiểm tra thời gian trong transaction và kiểm tra lại bằng thời gian DB mới sau khi chờ khóa/kiểm tra ghế, ngay trước tạo đơn; nếu đã tới cutoff thì trả `409 SHOWTIME_UNAVAILABLE`, không tạo đơn/giữ một phần ghế/phát sự kiện.
- Hold hợp lệ trước cutoff có đủ 600 giây từ mốc DB sau khi chờ khóa; không cắt ngắn deadline theo giờ bắt đầu suất. Đọc đơn hoặc retry đúng Idempotency-Key/payload sau cutoff vẫn trả trạng thái/deadline của chính đơn đó, không tạo đơn mới hoặc gia hạn.
- Retry khi đơn tới hạn chuyển EXPIRED và nhả ghế theo state guard; không hồi sinh đơn cũ. UUID mới vẫn phải qua cutoff. Không đổi quyền ownership, hợp đồng API, schema hoặc phạm vi payment/combo/voucher đã triển khai theo gate.

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

Giá do backend quyết định, VND nguyên: `subtotal = seats + comboUnitPrice × quantity`, `totalAmount = subtotal − discount`, `bankAmount = totalAmount`. Giữ FIRST50/CINE20, một voucher/đơn, không cộng dồn. Quote không ghi tiền, checkout giữ deadline gốc; sau tạo payment khóa snapshot. Không còn số dư nội bộ hoặc nhánh dùng Credit.


---

# 13. hoàn tiền ngân hàng

Voucher thay toàn bộ ví cũ. User xác nhận Credit chưa dùng và yêu cầu xóa dữ liệu/cột/logic, không giữ nhánh tương thích. Drop bằng migration mới sau backup/preflight, không reset DB hoặc sửa migration đã áp dụng.

Ca bankAmount0 dùng chung finalizer, không tạo collection giả, vẫn có vé và email.


---

# 14. payOS + VietQR

payOS là provider hiện hành. Thu về MB; hoàn bằng quỹ Bảo Kim, không tự trích tiền MB. Dùng SDK/API chính thức với hai client thu/chi, idempotency/reference bền vững. QR có amount/description do server; app không sửa. Backend kiểm tra chữ ký/reference/currency/tài khoản/số tiền/hold/ghế, finalizer chung transaction PAID/ACTIVE/SOLD/một ticket. Ca0đ không gọi thu, vẫn finalization/vé/email thật theo trạng thái. Webhook trùng/query không phát vé lần hai. Sai tiền/reference/trễ/chuyển thêm lưu receipt và NEEDS_ADMIN; không revive. Không coi return URL hoặc HTTP200 là bằng chứng tiền vào/ra. Unknown được query mã cũ, không tạo giao dịch mới.

```text
POST /orders/:id/payment
POST /orders/:id/payment/check
POST /webhooks/payos
GET /orders
GET /orders/:id
GET /tickets
GET /tickets/:id
GET /tickets/:id/qr
POST /tickets/:id/refund
GET /tickets/:id/refund
PUT /me/fcm-token
DELETE /me/fcm-token
```
JWT+ownership cho API khách, role riêng cho Admin issues; webhook public chỉ chấp nhận chữ ký provider hợp lệ. Client không quyết định số tiền thu/hoàn hoặc người nhận. Mẫu webhook kiểm tra cấu hình không được phát vé.


---

# 15. Order

Order status:

```text
PENDING_PAYMENT
PAID
EXPIRED
CANCELED
REFUND_PENDING
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
self-refund hợp lệ → REFUND_PENDING
REFUNDED
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
REFUND_PENDING
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

Chỉ nhận self-refund khi order PAID, ticket VALID, chưa check-in/chưa redeem combo, còn ít nhất45 phút theo DB. Hoàn toàn đơn,100% tiền thực trả sau voucher, N8 chịu phí; không hoàn voucher thành tiền. TX ngắn ghi nghĩa vụ REFUND, unique/idempotency và REFUND_PENDING; vô hiệu QR, cập nhật lịch sử ghế và nhả đúng ghế. HTTP payout ngoài transaction; timeout/query giữ reference cũ. SUCCEEDED mới chuyển REFUNDED; thiếu dữ liệu nguồn/quỹ/phí/lỗi giữ nghĩa vụ và đưa kiểm tra. Không cho chọn STK khác, không revive vé nếu chi lỗi. Admin không chi tay khi payout unknown. Email/FCM chạy sau commit, không đảo ngược giao dịch.


---

# 21. Hủy suất chiếu

Mục tiêu tuần10/11: Admin hủy suất sử dụng chung nghĩa vụ hoàn tiền của khoản thu gốc, không cộng ví nội bộ; từng payout có trạng thái bền vững. Tuần9 không tự nhận đã có full UI/CRUD hủy suất.


---

# 22. Email vé sau khi mua thành công

Nodemailer/Gmail SMTP hiện có; không đổi credential. Email gửi tới email snapshot tài khoản, sau commit. Mua thành công có poster HTTPS tin cậy (không server-fetch URL tùy ý), tên phim/suất/phòng/ghế/số ghế, một QR cho cả đơn, combo số lượng/đơn giá, subtotal/voucher/discount/total, booking code và QR PNG CID. HTML dạng bảng responsive600px, inline CSS nhận diện N8, có plaintext/alt và escape. QR/thư cũ không làm vé hoàn/hủy/hết hạn hợp lệ lại. Nếu receipt mua gửi muộn khi vé không còn VALID thì cảnh báo trạng thái và không nhúng QR sử dụng.

Hoàn vé có email tiếp nhận, hoàn thành và cần kiểm tra; chỉ thông báo hoàn thành khi giao dịch chi thành công. Trạng thái gửi/lease/attempt/nextAttempt lưu trong ticket/payment, worker retry có backoff, tối đa10 lần mỗi sự kiện trước khi cần vận hành kiểm tra. Lỗi email/FCM không rollback booking/refund. SMTP có thể lặp thư khi mất phản hồi; Message-ID ổn định, không bao giờ dùng retry mail để chi hoặc phát vé lại. TRANSACTION_EMAIL_ENABLED mặc địnhfalse; test không gửi mail thật.


---

# 23. FCM notification

FCM HTTPv1 dùng google-auth-library hiện có. Một fcmToken hiện tại/user, token chuyển login phải gỡ liên kết tài khoản cũ; logout gỡ token. Thông báo mua/hoàn gồm ticketId, không gửi QR/token ngân hàng. Tap mở vé rồi refetch REST. FCM_ENABLED mặc địnhfalse khi chưa có Firebase; private key chỉ server .env. Android public FirebaseOptions qua Gradle properties, không Analytics. Permission Android13+ không được chặn mua/xem vé. Reminder/scanner hoàn thiện ở tuần sau.


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

Chỉ cho phép JSON snapshot/notification-state có cấu trúc trong payments/tickets theo ngoại lệ tuần9; không domain JSON tùy ý.

Không tạo thêm bảng domain khác nếu requirement chưa thay đổi.

---

# 27. Chi tiết schema

Schema chuẩn là `backend/prisma/schema.prisma`; các field/quan hệ Prisma hiện hành dưới đây thay giới hạn7–12 field cũ. Không có cột số dư/khấu trừ nội bộ. Migrations mới giữ lịch sử bootstrap rồi drop các cột cũ; không sửa checksum migration đã chạy.

### `User`

`id`, `email`, `passwordHash`, `googleId`, `fullName`, `avatarUrl`, `role`, `isActive`, `emailVerifiedAt`, `refreshTokenHash`, `fcmToken`, `orders`, `heldSeats`.

### `Movie`

`id`, `title`, `posterUrl`, `durationMinutes`, `synopsis`, `ageRating`, `genre`, `language`, `sourceUrl`, `originalTitle`, `releaseDate`, `director`, `cast`, `trailerYoutubeId`, `updatedAt`, `showtimes`.

### `Auditorium`

`id`, `name`, `type`, `capacity`, `isExtra`, `status`, `createdAt`, `updatedAt`, `showtimes`.

### `Showtime`

`id`, `movieId`, `auditoriumId`, `startAt`, `endAt`, `format`, `presentationLabel`, `basePrice`, `status`, `sourceKey`, `createdAt`, `updatedAt`, `movie`, `auditorium`, `seats`, `orders`.

### `ShowtimeSeat`

`id`, `showtimeId`, `seatCode`, `rowName`, `seatNumber`, `seatType`, `price`, `status`, `heldByUserId`, `holdExpiresAt`, `updatedAt`, `showtime`, `heldByUser`, `orderSeats`.

### `Combo`

`id`, `name`, `description`, `price`, `imageUrl`, `status`, `createdAt`, `updatedAt`, `orders`.

### `Order`

`id`, `userId`, `showtimeId`, `comboId`, `comboQuantity`, `checkoutRevision`, `checkoutConfirmedAt`, `voucherCode`, `subtotal`, `discount`, `totalAmount`, `status`, `expiresAt`, `createdAt`, `user`, `showtime`, `combo`, `seats`, `payments`, `ticket`.

### `OrderSeat`

`id`, `orderId`, `showtimeSeatId`, `seatCode`, `price`, `status`, `createdAt`, `order`, `showtimeSeat`.

### `Payment`

`id`, `orderId`, `paymentCode`, `expectedAmount`, `receivedAmount`, `providerTransactionId`, `transferContent`, `status`, `issueReason`, `expiresAt`, `paidAt`, `createdAt`, `order`, `kind`, `intentOrderId`, `refundOrderId`, `parentId`, `parent`, `children`, `providerOrderCode`, `providerObjectId`, `operationState`, `recipientCipher`, `snapshot`, `notificationState`, `attempts`, `nextAttemptAt`, `leaseUntil`, `updatedAt`, `resolvedBy`, `resolutionNote`.

### `Ticket`

`id`, `orderId`, `bookingCode`, `qrTokenHash`, `status`, `issuedAt`, `checkedInAt`, `comboRedeemedAt`, `refundedAt`, `cancelledAt`, `snapshot`, `emailAttempts`, `emailNextAttemptAt`, `emailLeaseUntil`, `fcmSentAt`, `emailSentAt`, `expiresAt`, `order`.

Constraints bắt buộc: một ticket/order; intentOrderId/refundOrderId unique; provider reference/orderCode/objectId unique khi có; số tiền không âm; lịch sử order_seats không bị ghi đè khi bán lại. JSON chỉ dùng snapshot bất biến và trạng thái retry có cấu trúc.


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


**Hợp đồng triển khai catalog tuần 6 — 2026-10-17:**

- `GET /api/v1/showtimes/:id` trả `{serverTime,item}`; `item` cùng cấu trúc suất trong `/movies/:id/showtimes`, gồm `id/movieId/sourceKey/startAt/endAt/format/presentationLabel/basePrice/status/auditorium/availableSeats/totalSeats`.
- `GET /api/v1/showtimes/:id/seats` trả `{serverTime,showtimeId,items}`; ghế chỉ có `id/seatCode/rowName/seatNumber/seatType/price/status`, sắp theo hàng rồi số ghế. Không trả người giữ ghế hoặc hạn giữ ghế. Inventory thiếu trả `[]`, không sinh ghế từ capacity.
- UUID sai400, suất không tồn tại404 `SHOWTIME_NOT_FOUND`. Chi tiết suất tồn tại vẫn200 khi đã bắt đầu hoặc CANCELED; client phải kiểm tra trạng thái/thời gian/số ghế trước bước tiếp tục. GET chỉ đọc, không giữ ghế hay sửa trạng thái. Tiền là chuỗi VND, thời gian UTC ISO và serverTime có milliseconds.
- Giữ các envelope/list/date và lọc ACTIVE/tương lai hiện có; không thêm endpoint format/rating riêng. Rating trong scope này là ageRating P/K/T13/T16/T18, không phải điểm đánh giá phim. Một địa điểm Landmark 81, phòng và inventory logic N8; không hỗ trợ nhiều rạp/khu vực.


## 29.4. Booking

```text
POST /orders
POST /orders/:id/quote
POST /orders/:id/checkout
GET  /orders/:id
POST /orders/:id/cancel
GET  /orders/:id/payment
```

`POST /orders` nhận:

```text
showtime_id
seat_ids[]
```

Hold tạo trước màn combo. Quote nhận combo_id/quantity/voucher_code, chỉ đọc, giá server; checkout nhận quoteToken đã ký, giữ deadline gốc, không nhận giá client. Snapshot thay đổi trả409 QUOTE_CHANGED; token trước chuyển đổi bị từ chối.

Backend tự tính giá.

## 29.5. Payment

```text
POST /orders/:id/payment
POST /orders/:id/payment/check
POST /webhooks/payos
GET /orders
GET /orders/:id
GET /tickets
GET /tickets/:id
GET /tickets/:id/qr
POST /tickets/:id/refund
GET /tickets/:id/refund
PUT /me/fcm-token
DELETE /me/fcm-token
```
JWT+ownership cho API khách, role riêng cho Admin issues; webhook public chỉ chấp nhận chữ ký provider hợp lệ. Client không quyết định số tiền thu/hoàn hoặc người nhận. Mẫu webhook kiểm tra cấu hình không được phát vé.


---

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
payOS VietQR Payment
Payment Result
My Tickets
Ticket Detail / QR
Self-Refund Confirm
hoàn tiền ngân hàng
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
- release hold once;
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
- hoàn tiền ngân hàng;
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
- payout obligation created once; unknown never paid again.

## Email

- PAID → ticket → email;
- email contains booking data + QR;
- email failure does not rollback order;
- retry job sends later;
- email_sent_at updated on success.

## Admin cancellation

- pending order cancelled and seats released once;
- paid valid ticket creates one bank-refund obligation;
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
PAYOS_COLLECT_*/PAYOS_PAYOUT_* secrets in private backend .env
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
- hoàn tiền ngân hàng ledger;
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
- combo/promotion/hoàn tiền ngân hàng tính đúng;
- payOS/VietQR xác nhận được giao dịch thật;
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

### UI chi tiết phim và chọn lịch chiếu từ OpenDesign — 2026-10-17

Trạng thái: **Hoàn thành phần source/build/unit; Bị chặn kiểm tra emulator**. Android native Compose nối Dashboard khách hàng → chi tiết phim → lịch chiếu → xác nhận phân loại tuổi → tóm tắt suất. Màn tóm tắt là điểm dừng hiện tại vì chưa có chọn ghế; không tạo đơn, giữ ghế hoặc thanh toán. Dùng phim đã chọn từ catalog/Home và trailer dialog hiện có. Ảnh Dune/diễn viên từ `design/detailui/assets` được đưa vào `drawable-nodpi`; `raw/detail_asset_sources.json` giữ nguồn tài sản, font Be Vietnam Pro không đưa vào vì app tiếp tục dùng typography Cineplex hiện có. Điểm 8,6/10, số lượt đánh giá, ngày phát hành lại và trailer Dune là dữ liệu mẫu tập trung trong `DetailSchedule`, chỉ hiển thị cho đúng phim. Phân loại tuổi lấy `movie.age` (T13/T16/T18), sinh nội dung xác nhận từ số tuổi; phim không có ngưỡng T đi thẳng tới tóm tắt. Xác nhận là minh họa, không xác minh tuổi thực tế.

API hiện tại chưa có chọn khu vực/nhiều rạp và endpoint đặt vé. `DetailSchedule` chứa ba rạp/hai khu vực và suất 7 ngày tương đối theo ngày hiện hành, gồm suất hết vé và một ngày trống; toàn bộ màn lịch chiếu ghi rõ dữ liệu minh họa. Đổi ngày/khu vực/rạp xóa suất đã chọn; nút tiếp tục chỉ mở khi suất hợp lệ. Back giữ state/scroll của màn chi tiết/lịch qua `SaveableStateHolder`, Home giữ LazyListState sẵn có. `Scaffold` dành riêng bottom bar, app dùng safe drawing insets hiện có.

Kiểm tra: Gradle Wrapper `assembleDebug testDebugUnitTest lintDebug` đạt khi dùng Gradle/Android user home trong vùng build ghi được; 35/35 unit tests đạt, gồm test mới chọn suất/nhánh tuổi; lint 0 lỗi, 44 cảnh báo, 2 hints. Sau sửa cuối về state/semantics, chạy lại `assembleDebug testDebugUnitTest` đạt. Emulator đã khởi động nhưng ADB bị sandbox chặn tạo `\\.android`; chưa có ảnh chụp/kiểm tra tương tác trên emulator và không suy diễn visual đã đạt. Không đổi backend/API/DB nên không triển khai VPS. Chưa commit/push. Bước tiếp theo: kiểm tra trực quan luồng trên emulator khi ADB truy cập được, rồi cập nhật bằng chứng vào cả hai file.


### Catalog/showtime và tích hợp lịch chiếu tuần 6 — 2026-10-17

Trạng thái: **Đang làm**. Theo phạm vi đã được xác nhận: bổ sung GET /api/v1/showtimes/:id và /showtimes/:id/seats; nối màn lịch chiếu Android vào API thật, chỉ Landmark 81, giữ bố cục của Nhân. Tái sử dụng format/ageRating và thuật toán phân phòng hiện có; không thêm bảng/migration/dependency, không làm giữ ghế/đơn/thanh toán. Backend local giữ localhost; VPS giữ overlay proxy ngoài repo. Đã lưu PRE_WEEK6_BASELINE.zip ngoài repo, SHA256 390131264373894706e159bbc1fbeca3c9cfb8de0601e67ca91a25f3b316be57; typecheck và 17/17 catalog tests baseline đạt exit0. Còn source/tests mới, kiểm chứng Android, rollback bản sao, triển khai/nghiệm thu VPS và cập nhật kết quả. Giữ bộ bốn role hiện hành tại D:\NT118\verification\week5-crawler-20261010, bổ sung baseline/mode --week6 mà không thay các mốc cũ.


### Kết quả hoàn thiện catalog/showtime tuần 6 — 2026-10-17

Trạng thái: **Hoàn thành local/VPS/Android trong phạm vi đã chốt**, thay mốc Đang làm của catalog/showtime tuần 6 phía trên. Phạm vi là API đọc catalog/suất/ghế và tích hợp màn chọn suất thật cho Landmark 81, không phải hoàn thành đặt vé. Source của màn chi tiết/lịch chiếu được giữ bố cục, thay dữ liệu mẫu bằng DTO/repository thật; bộ lọc nhiều rạp mẫu bỏ theo spec một địa điểm.

- Backend: ShowtimesController.detail/seats và serializer dùng chung; không đổi hợp đồng API cũ, không thêm bảng/field/migration/dependency. Format sáu loại và ageRating tiếp tục dùng chuẩn hiện có. Tái sử dụng room assignment, bổ sung kiểm thử phòng inactive, ưu tiên seed, tạo/tái sử dụng extra và ranh giới [start,end); không đổi importer để ép kết quả.
- Android: selectedShowtime thay SampleSlot; lịch/ngày dùng chung Home/chi tiết/lịch, nhóm format+presentationLabel, phòng/counter/giá từ API. Loading/error/retry/empty tách biệt, không fallback lịch/rạp/điểm đánh giá/ngày/trailer mẫu. API chi tiết kiểm tra lại trước tóm tắt, chặn suất hủy/quá giờ/hết ghế/thiếu inventory; epoch/cancel chống phản hồi muộn sau đổi ngày/back/logout. Cảnh báo tuổi dùng movie.age; xác nhận tự khai không xác minh tuổi thật. Qua ngày Việt Nam xóa lựa chọn và trở lại lịch thay vì để tóm tắt rỗng. Grid dùng min width theo fontScale, có nhãn trợ năng cho back/suất.
- Local: build/typecheck/format backend đạt; 61/61 tests đạt. Local127.0.0.1:5000 qua tunnel DB chung health200/database up, payload ngày17/18 khớp VPS cũ; chi tiết/ghế mới khớp inventory32 ghế GOLD. Process API/tunnel do phiên tạo đã dừng. Android41/41 unit, assembleDebug/AndroidTest/lint đạt, lint0 lỗi/44 cảnh báo/2 hints có sẵn.
- Android bản cuối: native21/21 trên360×800dp gồm UI/Auth/date/preview/trailer thật và hai catalog tests qua HTTPS VPS, thêm Auth connectivity; tablet820×1180dp3/3 và393×852dp font1.5 3/3 đạt. Ảnh native đã mở kiểm tra; giờ không bị ngắt dòng ở font lớn sau grid responsive. APK debug WEEK6_CATALOG.apk, SHA256 6acb8705ec34bf7cab86638d0b4d637ed3a637bdebbca4c5a519cdb7eb8a7036, lưu tại bộ verification hiện hành. Chưa thử máy vật lý.
- VPS: đã build và triển khai catalog-preview-780c6b76031e7acc, source identity 780c6b76031e7acce9ea401dbf0e7e9c5543576f1723116e0d0f7fbaf9409445, manifest42 file; đúng4 file overlay proxy môi trường so với public backend, không đưa proxy/secrets vào source local. 64/64 tests chạy thật trong tools container Linux network-none với cấu hình fixture và theme readonly, không gọi đó là tests local. Native Argon2/sharp/Chromium đạt. API healthy/start2026-10-17T07:04:53.111260125Z, health200/database up, Swagger200/OpenAPI có hai endpoint mới; HOST0.0.0.0:5000 nội bộ, TLS/host key kiểm tra bình thường.
- DB đọc-only:10 bảng/109 cột, overlap phòng ACTIVE0, sai capability0, thiếu inventory so capacity0, trùng seat_code0. Snapshot ngày18 tại07:07UTC có11 phim/43 suất; đã đối chiếu chi tiết/ghế cho GOLD_CLASS/STANDARD_2D/IMAX_2D/SCREENX đang có; các format3D được kiểm thử bằng fixture, không bịa suất thật. Dữ liệu/counter có thể thay đổi theo thời gian. Hash users/inventory trước triển khai và qua nghiệm thu giữ nguyên; không chạy migration/reset/restore hay backfill.
- Proxy crawler Playwright thật trên release mới CGV HTTP200 lúc07:07:15.266Z. Cấu hình riêng3 env/hash/quyền giữ nguyên, PostgreSQL/Nginx ID/StartedAt/mount giữ nguyên. Backup timer và snap.certbot.renew.timer active; release dùng family catalog-preview được backup.sh chấp nhận. Không thay lịch cron00/06/12/18 giờ Việt Nam.
- Sau nghiệm thu API/UI qua VPS đã dọn image/cache theo khóa deploy-cleanup: giữ đúng3 image đang chạy, thu hồi4.813GB builder +4.482GB image, còn22GB. API/PG/Nginx trạng thái/startup/mount/env giữ nguyên, health/Swagger vẫn200. Giữ source release/manifest và cấu hình dựng lại, không prune volume/network, không reboot.
- Bằng chứng: cùng probe với UUID fixture cố định: BASELINE hai endpoint404 → MODIFIED200, IMAX_2D/counter2/3 và ghế A1,A2,A10 → ROLLBACK bản sao404; exit0. Rollback --week6 phục hồi138 file/hash pristine từ PRE_WEEK6_BASELINE.zip SHA256 390131264373894706e159bbc1fbeca3c9cfb8de0601e67ca91a25f3b316be57; regression restored59/59 đạt với env fixture không-secret. Source chính giữ bản mới. Cùng4 role tại D:\NT118\verification\week5-crawler-20261010 được cập nhật, giữ baseline/mode cũ và bổ sung patch tuần6; không tự rollback VPS hoặc DB.

Các lỗi kiểm chứng đã giữ ngoài repo và sửa/rerun: patch tool từ chối hai thao tác cùng file (chưa chạy sửa đó); emulator ban đầu bị LOW_MEMORY, tăng RAM phiên test lên4096MB; capture Compose có2 root khi dialog mở chuyển sang UiAutomation; hai UI selectors cũ cập nhật để đi qua màn chọn suất mới, giữ kiểm tra ngày/search/logout; rollback copy không có .env dùng cấu hình fixture rồi59/59 đạt. Query certbot.timer không phải unit đang dùng; xác nhận đúng snap.certbot.renew.timer active, không sửa dịch vụ chứng chỉ. Không bỏ tests hoặc nới TLS/state guard để ép đạt.

Giới hạn: chưa chọn/giữ ghế, tạo đơn, combo/payment/ticket; đọc số ghế không bảo đảm chỗ. Không triển khai nhiều rạp, VIP/couple zoning hay xác minh tuổi thực tế. Không coi probe CGV là một lượt import hoặc tick cron. Bước tiếp theo ngoài nhiệm vụ hiện tại là người dùng chạy thử APK; nghiệp vụ đặt vé thuộc các tuần sau.


### Công bố phần tuần 6 trên nhánh riêng — 2026-10-17

Trạng thái: **Đang làm phần công bố**, không thay đổi kết quả hoàn thành local/VPS/Android ở trên. Người dùng đã yêu cầu tạo commit và push. GitHub API /user xác nhận tài khoản xác thực thnguyen290106; email thnguyen290106@gmail.com đã liên kết và verified. Repository dùng Author/Committer Trần Thanh Nguyên <thnguyen290106@gmail.com> cho các commit mới, không co-author/bot. Nhánh feat/nguyen-week6-catalog-showtimes tách từ main bcc666d6d5867b207d136fc8a1b17f1af78dc8a2; không sửa lịch sử hoặc đẩy lên main. Chỉ công bố 22 file của thay đổi tuần 6, không đưa secrets, log, APK hoặc selftest tạm vào repository.

Các source backend hiện tại đã đối chiếu byte/hash với manifest public của release VPS catalog-preview-780c6b76031e7acc đã nghiệm thu; bước công bố không thay đổi runtime, schema hoặc cấu hình VPS. Bằng chứng Git/định danh lưu ngoài repository tại bộ verification hiện hành; chưa ghi SHA/kết quả push trước khi thực thi. Bước tiếp theo: kiểm tra staged diff, tạo commit đúng danh tính, push nhánh, đối chiếu SHA remote và tài khoản tác giả trên GitHub rồi cập nhật cả hai tài liệu.


### Kết quả công bố source tuần 6 — 2026-10-17

Trạng thái: **Hoàn thành công bố source trên nhánh riêng**, thay mốc Đang làm phần công bố phía trên; giữ nguyên kết quả kiểm chứng local/VPS/Android đã ghi. Source commit `35f093d09766af3f78f0195bc1f69bf4b2c18c3f` đã push thành công lên `feat/nguyen-week6-catalog-showtimes`: https://github.com/Tonyisded/N8_Cineplex-Project/commit/35f093d09766af3f78f0195bc1f69bf4b2c18c3f. Git ls-remote xác nhận SHA remote khớp local; GitHub API xác nhận cả author.login và committer.login là `thnguyen290106`, Author/Committer Trần Thanh Nguyên <thnguyen290106@gmail.com>. Không có co-author/bot, không sửa contributor/lịch sử cũ, không push/merge main hoặc tạo PR thay nhóm trưởng.

Đã kiểm tra staged diff sạch lỗi whitespace và chỉ đúng22 file; không secrets, file build, selftest hoặc log tạm. Backend công bố khớp manifest public đã nghiệm thu của VPS catalog-preview-780c6b76031e7acc; bốn file proxy overlay môi trường vẫn ở ngoài repo. Lượt công bố không đổi code runtime hoặc schema so với bản đã triển khai, không build/deploy lại, không chạy lại toàn bộ Android/VPS tests, không thay dữ liệu DB. Các kết quả61/64 backend,41 unit,21/3/3 native và BASELINE404/MODIFIED200/ROLLBACK404 là bằng chứng thực thi đã ghi trong mốc hoàn thiện phía trên, không nhận là lượt chạy mới của thao tác Git.

Cả hai tài liệu được cập nhật cùng đợt. Bằng chứng đầy đủ và SHA của bản cập nhật tài liệu tiếp theo được lưu ngoài repo trong cùng bộ verification; giữ nguyên bốn role/baseline hiện hành. Bước tiếp theo: nhóm trưởng review nhánh và quyết định merge; chưa thực hiện merge.

### UI chọn ghế Android native từ design/seat-selection — 2026-10-25

Trạng thái: **Hoàn thành phần giao diện và tương tác mô phỏng trên Android; Chưa làm giữ ghế, realtime đa người dùng, đơn hàng hoặc thanh toán thật.** Mốc này bổ sung màn chọn ghế sau lịch chiếu/xác nhận tuổi hiện có, thay điểm dừng tóm tắt suất trước đây. Hai bộ thiết kế cũ chỉ còn là tham chiếu cho các màn đã triển khai. Không đổi Login, Signup, Dashboard, API, database hoặc backend.

Android Kotlin/Jetpack Compose tái dùng CineplexColors, typography, MoviePoster, selectedShowtime và điều hướng ViewModel hiện tại. Màn chọn ghế nhận đúng phim, phân loại tuổi, Landmark 81, phòng, ngày/giờ, format/nhãn trình chiếu và ID suất từ suất được chọn; mã suất hiển thị nguyên vẹn. Sơ đồ mô phỏng 10 hàng A–J có màn chiếu, số ghế, lối đi 3/4, ghế thường 90.000đ/chỗ, VIP 120.000đ/chỗ và Couple 240.000đ/cặp hai chỗ. Chọn/bỏ Couple nguyên cặp; một mã con không khả dụng khóa cả cặp. Trạng thái trống/chọn/đang xử lý/người khác giữ/đã bán có marker và nhãn trợ năng. Sơ đồ cuộn hai chiều, zoom 100–200%, có reset vị trí. Thanh cố định dưới cùng hiển thị mã ghế, số chỗ, giá và tổng tiền.

SeatSelectionViewModel giữ lựa chọn, trạng thái kết nối và deadline qua xoay màn hình; ticker duy nhất theo lifecycle STARTED, khi resume tính lại từ deadline tuyệt đối. Thời hạn 600 giây bắt đầu sau chọn mô phỏng thành công đầu tiên, không reset khi đổi/bỏ ghế hoặc sang màn tóm tắt; dưới 120 giây cảnh báo, hết hạn xóa lựa chọn/pending, khóa tiếp tục và mở dialog chọn lại/về lịch. Rời màn đang chọn mở xác nhận ở lại hoặc rời và xóa lựa chọn. Tiếp tục chỉ khi ghế hợp lệ, online mô phỏng và còn hạn; điểm chuyển tiếp tóm tắt chỉ hiển thị lựa chọn, không tạo đơn/thanh toán.

Backend hiện có GET /api/v1/showtimes/:id/seats chỉ đọc inventory, không có API giữ/giải phóng ghế hoặc Socket.IO booking phù hợp; backend hiện chưa có zoning VIP/Couple. Vì vậy nguồn SeatDemo tách riêng và toàn bộ chọn/giữ giả lập, xung đột, cập nhật giữ/bán/giải phóng, offline/reconnect là mô phỏng cục bộ trên thiết bị, không dùng tồn kho thật và không đồng bộ giữa người dùng. UI ghi rõ điều đó. Khi offline, sơ đồ giữ góc nhìn nhưng khóa chọn/tiếp tục; reconnect áp dụng snapshot, loại cặp/ghế mất hiệu lực và cập nhật tiền/deadline. Điều khiển tình huống nằm trong SeatScenarioActivity của source set debug, mở bằng adb shell am start -n com.nhom8.cineplex/.SeatScenarioActivity, không có trên màn khách hàng hoặc bản release.

Kiểm chứng local: Gradle Wrapper assembleDebug, assembleDebugAndroidTest, testDebugUnitTest đạt; 43/43 unit tests, gồm 2 test mới về Couple/deadline/xung đột/reconnect. Lint source ứng dụng đạt 0 errors, 44 warnings, 3 hints bằng lintDebug với hai task phân tích test được loại vì lượt lint đầy đủ bị treo ở phân tích test; không gọi lượt lint đầy đủ là đã đạt. Emulator Pixel_10_Pro_XL: 4/4 UI tests đạt (chọn ghế, khách hàng/back/logout, Signup, catalog). Bài chọn ghế đi theo luồng lịch chiếu → tuổi → chọn A2 và J1–J2 → 3 chỗ/330.000đ → tóm tắt → quay lại/xác nhận rời đạt; ảnh chụp được xem cho sơ đồ VIP/Couple, chú thích, thanh tổng kết. Chưa kiểm tra máy vật lý. Không cần deploy VPS vì không thay backend; chưa commit/push/merge. Bước tiếp theo khi triển khai nghiệp vụ thật: hợp đồng API hold/order và realtime sau DB commit, thay SeatDemo bằng nguồn server, giữ nguyên ràng buộc 10 bảng và transaction trong Plan.

### Điều chỉnh ghế trống ban đầu và lối vào từ Home — 2026-10-25

Trạng thái: **Hoàn thành phần Android mô phỏng và kiểm chứng local/emulator.** Điều chỉnh này thay thế trạng thái ban đầu có ghế HELD/SOLD đặt sẵn trong SeatDemo của mốc UI chọn ghế cùng ngày: khi mở một suất mới, toàn bộ 54 thực thể/60 chỗ của sơ đồ mẫu đều AVAILABLE. Chỉ thao tác mô phỏng trong debug mới tạo giữ/bán/giải phóng hoặc xung đột; lựa chọn đang xử lý/đang chọn vẫn hoạt động như trước. Trường hợp 32/32 trong ảnh người dùng vì vậy không còn các ghế mẫu bị khóa sẵn. Sơ đồ 60 chỗ vẫn là template minh họa, chưa phải sơ đồ/capacity hay tồn kho thực 32 chỗ của GOLD; không được suy diễn là giữ ghế thật.

Chạm giờ suất chiếu trên Home nay dùng phim và suất vừa chạm, kiểm tra lại GET /api/v1/showtimes/:id qua CatalogRepository trước khi mở Seat Selection; không còn dialog thông tin suất cho lượt chạm hợp lệ. Nếu API báo suất bắt đầu/hủy/hết ghế hoặc lỗi mạng, ở Home và thông báo lỗi; trong lúc kiểm tra có thanh tiến trình. Back từ chọn ghế đến Home, giữ bộ lọc/cuộn Home; nếu đang chọn thì dialog xác nhận rời vẫn áp dụng. Luồng chi tiết phim → chọn lịch chiếu → xác nhận tuổi → chọn ghế và Back về lịch chiếu giữ nguyên. Lối vào Home đi thẳng vào chọn ghế sau kiểm tra API, còn xác nhận tuổi vẫn nằm trong luồng lịch chiếu chi tiết; chưa có booking/payment thật.

Kiểm chứng: Gradle Wrapper assembleDebug, assembleDebugAndroidTest, testDebugUnitTest thành công; 45/45 unit tests đạt, gồm kiểm tra toàn bộ chỗ mẫu AVAILABLE và Home xác minh suất/trở về Home/từ chối suất hết ghế. Lint source ứng dụng đạt 0 errors, 44 warnings, 3 hints (hai task lint phân tích test vẫn loại như mốc trước vì lượt lint đầy đủ bị treo). Emulator Pixel_10_Pro_XL: 3/3 bài UI liên quan đạt (Home giờ → chọn ghế → Home; chi tiết/lịch/tuổi → chọn ghế; Dashboard/back/logout). Ảnh home-seat-all-available.png trong app/build đã xem, A1/A6/B2/B4 hiển thị trống; UI test kiểm tra A1 và I3–I4 là Còn trống. Không đổi backend/API/database, không deploy VPS, chưa commit/push/merge. Bước tiếp theo của nghiệp vụ thật vẫn là API hold/order/realtime và sơ đồ theo inventory/room thực.


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

Trạng thái: **Hoàn thành bổ sung kiểm thử biên, tài liệu và xác nhận VPS chỉ đọc**. Người dùng đã chốt cutoff tại đúng giờ bắt đầu suất chiếu; không thay runtime/API/database/Android hiện có. Bổ sung sáu bài regression với thời gian DB cố định: trước giờ chiếu 1 ms được giữ đủ600s; đúng/sau giờ chiếu bị từ chối; chờ khóa tới đúng cutoff bị từ chối; đọc/retry sau cutoff giữ nguyên đơn/deadline; retry tại expiry không hồi sinh đơn; phòng INACTIVE bị từ chối. Bài HTTP hiện có bổ sung kiểm tra409 SHOWTIME_UNAVAILABLE sau cutoff. Fixture hiện có nhận clock tùy chọn, mặc định giữ hành vi cũ; các bài service dùng DB giả, không phải phép thử cạnh tranh PostgreSQL thật.

- Local: build, typecheck, toàn bộ76/76 backend tests và format:check đạt exit0; trong đó15/15 booking tests đạt. Cùng lệnh node --test tests/booking.test.mjs: BASELINE9/9 → MODIFIED15/15 → ROLLBACK9/9, cả ba exit0. ROLLBACK.sh --week7-cutoff chạy thật trên bản sao riêng, cả3file khớp hash gốc; workspace vẫn giữ test/quy tắc mới. Giữ nguyên43file source/config/schema backend được ghi hash trước sửa; chỉ thay booking.test.mjs và hai tài liệu. Cùng bốn role ngoài repo được mở rộng, không thay baseline/mode rollback cũ.
- VPS: SSH bằng key người dùng chỉ định và host fingerprint đã ghim; kiểm tra chỉ đọc lúc2026-10-25T09:09:55Z đạt exit0. API/PostgreSQL healthy, reverse-proxy running; API StartedAt vẫn2026-10-25T07:20:42.139035548Z và restart unless-stopped. Health HTTPS trả status ok/database up, Swagger200, OpenAPI đủ3route Orders. Có startup-success,0dòng ERROR/FATAL trước/khi startup hoàn tất. Chín file JavaScript/package runtime khớp local theo hash byte hoặc LF chuẩn hóa. Socket.IO HTTPS WebSocket upgrade đạt, JWT sai bị AUTH_REQUIRED; không bỏ kiểm traTLS. Port API5000 không public, PostgreSQL chỉ127.0.0.1:5432. Không restart/rebuild/deploy API, migrate hoặc tạo fixture/ghi DB chung vì runtime không đổi.
- Kiểm tra VPS đầu exit1 tại bước đọc src/booking/booking.service.ts do image production không chứa source TypeScript; health/Swagger đã đạt ở lượt đó. Đã sửa checker đọc dist JavaScript thực chạy, giữ nguyên stdout/stderr/exit của lượt lỗi trong ledger ngoài repo. Không coi lỗi đường dẫn checker là lỗi runtime.

Giữ kết quả E2E600.113s đã nghiệm thu ở mục trước, không nhận là lượt E2E mới hoặc76tests chạy trên VPS. Chưa kiểm tra Android vật lý/deep sleep trong lượt này; không sửa Excel, không mở rộng payment/combo/credit. Bước tiếp theo: bàn giao kết quả kiểm thử; chỉ mở lại runtime nếu có lỗi tái hiện được hoặc requirement mới. Hai tài liệu đã được đối chiếu theo cùng kết quả local/VPS.

### Công bố phần chốt booking tuần 7 trên branch mới — 2026-10-25

Trạng thái: **Hoàn thành tạo branch, commit và push phần kiểm thử**. Đã tạo branch feat/nguyen-week7-seat-hold-cutoff và push commit93a9e953cc71d1de8e1e6d09c24e53d032800a6a, subject test(booking): cover week 7 booking cutoff boundaries. Commit chỉ gồm backend/tests/booking.test.mjs và cả hai tài liệu. Push exit0, ls-remote xác nhận branch đúng SHA; GitHub API xác nhận Author/Committer đều thnguyen290106, tên Trần Thanh Nguyên <thnguyen290106@gmail.com>, không co-author/bot attribution.

Kiểm tra trước xuất bản: chạy lại76/76 backend tests đạt exit0, diff/index whitespace check và quét credential trong nội dung thêm mới đạt. Giữ kết quả15/15 booking, BASELINE9/9 → MODIFIED15/15 → ROLLBACK9/9 và hash rollback gốc. Main remote vẫn6f717a611509aa5945a5726c723387255af6ac57; không push/merge main hoặc rewrite contributor lịch sử. Runtime/API/DB không đổi nên không rebuild/deploy VPS trong lượt công bố này; giữ kết quả VPS chỉ đọc đã ghi ở mục trước.

Bản ghi kết quả thực tế này được cập nhật đồng thời ở Plan và AGENTS trên cùng branch riêng; SHA cuối của commit tài liệu và xác nhận remote/attribution được lưu trong ledger ngoài repo sau thực thi, không điền trước SHA tự tham chiếu. Bước tiếp theo: nhóm trưởng review branch và tạo/merge PR theo quy trình nhóm; lượt này không tự tạo hoặc merge PR. Chưa kiểm tra thiết bị Android vật lý hoặc chạy lại E2E600s.

## Phim Sắp Chiếu từ CGV — crawler, API, Android (2026-10-25)

Trạng thái: **Hoàn thành code/crawler/API/migration, triển khai VPS, kiểm chứng Android và verification bundle/rollback**. Backend commit `f6af43bf81e6a90b97fa25d48cd3c9a8c77f31d7` và Android commit `03526a50493fee4398ae6b4f1fb0ba25c80580b8` trên nhánh `feat/nguyen-week8-coming-soon`; remote khớp tại lúc push. Mục này thay trạng thái "Sắp chiếu chưa tích hợp" ở các mốc trước; không thay lịch sử các mốc đó.

Phạm vi: bổ sung dữ liệu cho tab **Sắp chiếu** của app khách hàng từ nguồn CGV "Phim Sắp Chiếu"; không đặt vé/bán vé/giữ ghế cho phim sắp chiếu, không thêm bảng/cột/dependency. Trang chủ tab Đang chiếu, lịch chiếu, giá/ghế/booking hiện hành giữ nguyên.

Nguồn và quy tắc dữ liệu:
- URL nguồn cố định `https://www.cgv.vn/default/movies/coming-soon-1.html` (Magento render server-side; Playwright trực tiếp đọc HTTP 200). Không dùng trang "Phim Đang Chiếu" catalog và không phân trang.
- Selector nguồn: mỗi phim `li.film-lists.item`; tên/link `.product-name a`; tuổi `.nmovie-rating` với class `nmovie-rating-{t18|t16|t13|k|p|no}` (text `NO` = chưa phân loại); poster `.product-images img`; thông tin `.cgv-movie-info` với cặp `.cgv-info-bold`/`.cgv-info-normal` (Thể loại/Thời lượng/Khởi chiếu dạng `DD-MM-YYYY`). Trang chi tiết `/default/<slug>.html` dùng đúng selector parser hiện có.
- Snapshot nguồn quan sát 2026-10-25: 33 phim; 29 phim có ngày khởi chiếu (30-10-2026 → 05-03-2027), 4 phim thiếu ngày; chỉ 4/33 có thời lượng, 3/33 có phân loại tuổi (2×T18, 1×K), còn lại `NO`. Số lượng thay đổi theo nguồn, không hardcode.
- Phân loại sắp chiếu **không thêm cột/status**: `movies.release_date > hôm nay theo Asia/Ho_Chi_Minh`. Phim đã có suất ACTIVE bắt đầu trước `release_date` (đã chiếu sớm/premiere) bị loại khỏi danh sách; phim presale có suất từ ngày khởi chiếu vẫn giữ. Không dùng sự tồn tại của showtime để loại toàn bộ vì sẽ mất phim đã mở bán trước ngày chiếu.
- Thiếu metadata: `movies.duration_minutes = 0` nghĩa là chưa công bố thời lượng; `movies.age_rating = "NO"` nghĩa là chưa phân loại (giá trị nguồn). UI ẩn badge tuổi và ghi "Đang cập nhật" cho thời lượng; không bịa số.
- Parser `parseComingSoonMovie`: bắt buộc title và ngày khởi chiếu; duration/tuổi thiếu dùng sentinel; ưu tiên dữ liệu trang chi tiết, fallback listing; thiếu ngày khởi chiếu trả `null` → caller ghi `omitted`, không làm hỏng cả lượt.
- Collector `collectComingSoon`: xác thực URL/trang bằng body class `category-coming-soon`, container `.category-products.cgv-movies` và danh sách không rỗng; listing rỗng/sai trang ném lỗi (không coi là nguồn rỗng); lỗi từng trang chi tiết → `skipped` và tiếp tục.
- Import `importComingSoon`: transaction + `pg_advisory_xact_lock(810710)` dùng chung với import lịch; create/update theo `source_url`; update **non-degrading** (duration >0 hoặc tuổi khác `NO` mới ghi đè; field nullable chỉ cập nhật khi nguồn có giá trị); không tạo showtime/showtime_seat, không xoá phim/suất, không reset lịch sử. Status `SUCCESS` nếu không có `skipped` (omitted không tính PARTIAL).
- Cron: cùng tick `0 0 */6 * * *` Asia/Ho_Chi_Minh; chạy `runUpcoming()` rồi `runComingSoon()`, mỗi bước try/catch riêng để một nguồn lỗi không chặn nguồn kia. CLI one-shot: `node dist/crawler/run.js --coming-soon`.
- API additive `GET /api/v1/movies/coming-soon` (khai báo trước `/movies/:id`): `{businessDate, serverTime, items}` với `movieJson`, không kèm `showtimes`; chỉ phim `releaseDate > hôm nay`, loại phim đã chiếu trước ngày khởi chiếu; sort `releaseDate, title, id`. Contract ghi rõ `durationMinutes = 0` và `ageRating = "NO"` là sentinel.
- Android: repository gọi endpoint mới, validate `serverTime`, id không trùng, `releaseDate` bắt buộc/parse được; ViewModel giữ danh sách/loading/error riêng, tải cùng catalog lúc login/resume/force, lỗi sắp chiếu không làm hỏng tab Đang chiếu; tab Sắp chiếu ẩn thanh ngày/giờ (không áp dụng), giữ search/tabs/banner/nav; card riêng có ngày khởi chiếu, ẩn badge `NO`/thời lượng 0; mở chi tiết/trailer hiện có; back giữ tab, logout xoá dữ liệu.

Migration bắt buộc: DB hiện có CHECK `movies_duration_positive` (`duration_minutes > 0`), chặn sentinel 0. Migration `20261025000000_coming_soon_duration` thay bằng `movies_duration_nonnegative` (`>= 0`), **không đổi cột/bảng** (vẫn 10 bảng/109 cột), không rewrite dữ liệu. Phải backup trước, review SQL, áp dụng bằng `prisma migrate deploy` với role migration trên DB chung; không reset/restore. Rollback code không cần rollback DB vì code cũ không ghi 0 và chưa có endpoint đọc sentinel.

Giới hạn/giới hạn đã chốt: danh sách cập nhật tối đa 6 giờ/lần; phim bị gỡ khỏi nguồn vẫn còn tới khi qua ngày khởi chiếu; phim thiếu ngày khởi chiếu chưa hiển thị; phim đang chiếu sớm nhưng `release_date > hôm nay` nằm trong danh sách loại trừ theo suất trước release; không hỗ trợ nhiều rạp hay đặt vé cho phim sắp chiếu.

Tiến độ/kiểm chứng local:
- Backend `npm run build`, `npm run typecheck`, `npm run format:check` exit 0; **83/83 tests** đạt exit 0, gồm coming-soon parser (sentinel/ngày), collector (identity/omitted/skipped), import (non-degrading/không tạo suất-ghế/advisory lock), API (biên ngày VN/loại phim chiếu sớm/route trước `:id`), job (cùng tick/lỗi độc lập).
- Playwright live 2026-10-25: CGV coming-soon HTTP 200, 33 item và các selector nêu trên đọc được; đây là khảo sát nguồn, không phải lượt import.
- Lượt import thật local đầu tiên lúc 2026-10-25T09:47Z **thất bại** với `movies_duration_positive`; transaction rollback nên không ghi phim mới. Không dùng lượt lỗi này làm bằng chứng hoàn thành; migration được bổ sung để xử lý đúng nguyên nhân.
- Chưa áp dụng migration tại thời điểm ghi local; đã bổ sung migration để xử lý đúng CHECK cũ (xem mục kết quả bên dưới). Bước tiếp theo lúc đó: commit/push nhánh riêng, backup + migrate deploy, deploy API có proxy overlay, chạy one-shot qua proxy, kiểm tra API/Android thật.

### Kết quả triển khai và nghiệm thu phim Sắp Chiếu — 2026-10-25

Trạng thái: **Hoàn thành code/migration/deploy/kiểm chứng** trong phạm vi tab Sắp chiếu; không mở rộng booking/payment. Android không sửa backend; VPS chạy đúng commit backend `f6af43bf81e6a90b97fa25d48cd3c9a8c77f31d7` với 4-file proxy overlay ngoài repo.

- Local backend: `npm run build`, `npm run typecheck`, `npm run format:check` exit 0; **83/83 tests** đạt exit 0. Bản mô phỏng môi trường VPS (overlay proxy + fixture env + theme readonly) build và **86/86 tests** đạt (83 public + 3 proxy).
- Android: build debug/AndroidTest, **55/55 unit tests** và **34/34 connected tests** (11 lớp, gồm `CatalogServerUiTest` gọi HTTPS VPS thật và candidate coming-soon, Auth connectivity, SeatServer, UI fixture) đạt 0 fail/error/skip; lint **0 errors/44 warnings/3 hints**. Đã chạy riêng `CatalogServerUiTest` trên emulator Pixel_10_Pro_XL: 1/1 đạt; ảnh `catalog-live-upcoming`, `catalog-live-upcoming-detail`, `catalog-live-upcoming-schedule` cho thấy danh sách thật, ngày khởi chiếu, ẩn thanh ngày/giờ, thông báo "Phim khởi chiếu ... — lịch chiếu sẽ được cập nhật", không còn câu "chưa tích hợp". Một poster CLAYFACE chưa kịp hiển thị trong ảnh chụp; URL poster trả HTTP 200 nên ghi nhận là tải ảnh tạm thời, không phải lỗi dữ liệu/API.
- VPS release: archive SHA-256 `d98fab6e2669b516bd4a746079700873a9c9d3afc03e9dda6c99225617809e08`, 235 file, `sha256sum -c` đạt trước overlay; overlay đúng 4 file config/crawler.service/smoke/crawler tests, patch ngoài repo 1305 dòng, sau overlay chỉ đúng 4 file khác public và không có proxy/secrets trong source. Image `n8-cineplex-api: f6af43b...` healthy; tools tests Linux thật trong container `--network none` với fixture env/theme mount: **86/86** đạt.
- Migration/DB: backup trước migration `n8_cineplex-20261025T100129Z-390666.dump`, SHA-256 `6af515e30e96df71c6fc661bc3c4116bd3bc39622fae61aa0b61a9066a1fd1f7`, `pg_restore --list` hợp lệ; `prisma migrate deploy` áp dụng `20261025000000_coming_soon_duration`, `db:status` up to date; constraint `movies_duration_nonnegative (>=0)`; vẫn **10 bảng/109 cột**, không reset/restore, users hash `1626b515aac1ac4b639e41e3ba8fefd3` giữ nguyên.
- API VPS: health 200/database up, Swagger 200, OpenAPI có `/api/v1/movies/coming-soon`; endpoint trả **24 item**, tất cả `releaseDate > 2026-10-25`, không có `showtimes`, ngày tăng dần, 20 item duration 0 và 21 item age `NO`; SQL dự phòng đếm đúng 24. DB sau crawl: movies 47, showtimes 788, showtime_seats 134308 không đổi; không phát sinh showtime/ghế từ bước coming-soon.
- Lượt one-shot qua proxy thật trên VPS: runId `dc09cba8-2247-41ba-9436-784bbd711659`, 33 phim nguồn, tạo **24** phim, `omitted` 2 (thiếu ngày khởi chiếu), `skipped` 7 (1 timeout 30s + 6 trang chi tiết trả lỗi điều hướng qua proxy) nên status **PARTIAL**, exit 2; dữ liệu đã commit và hiển thị 24 phim. Không gọi lượt này là SUCCESS; cron 6h sẽ thử lại các trang còn thiếu. Chưa quan sát tick cron tương lai.
- Dọn dẹp: prune builder/image cũ theo khóa `n8-cineplex-deploy-cleanup.lock` sau khi health/Swagger/catalog/crawler đạt; chỉ giữ 3 image đang chạy (api mới, nginx, postgres), dung lượng còn trống tăng từ 15GB lên 22GB; tools image bị prune theo chính sách và có thể rebuild từ release nếu cần. Temp script/archive trên VPS đã xoá; health và API vẫn 200/24 item sau dọn. Không prune volume/network, không `compose down -v`, không restore DB.

Giới hạn còn lại: 7 phim nguồn chưa vào DB trong lượt one-shot đầu do proxy/trang chi tiết (sẽ thử lại theo cron); 2 phim thiếu ngày khởi chiếu; chưa quan sát cron tick thật; chưa kiểm tra thiết bị vật lý — kết quả Android là emulator. Verification bundle ngoài repo đã chốt: baseline/modified/diff/verification/rollback; `ROLLBACK.sh` chạy trên bản sao cho baseline hashes OK, route coming-soon vắng mặt, migration vắng mặt, backend baseline build đạt, catalog tests 14/14 và probe route metadata absent; ảnh live VPS lưu ngoài repo. Không ghi đường dẫn artifact vào tài liệu theo yêu cầu trước đó.

Công bố nhánh mới (2026-10-25): theo yêu cầu người dùng, đã tạo và push nhánh `feat/nguyen-week8-coming-soon-publish` trỏ tới đúng 3 commit đã kiểm chứng (`f6af43b`, `03526a5`, `c2455f9`; cùng tree, không tạo commit rỗng) rồi thêm commit tài liệu công bố này. `git ls-remote` khớp SHA remote và GitHub API xác nhận author/committer của commit đầu/cuối là `thnguyen290106`, email `thnguyen290106@gmail.com`; không viết lại lịch sử, không đụng `main` hay nhánh cũ. Base `5987ce8` đã nằm trong `origin/main`, nên nhánh publish dùng được cho PR mà main tree không khác baseline của đợt này.

## Sửa lỗi QR payOS thật — 2026-11-02

Đã tái hiện lỗi production `42501 permission denied for sequence payos_order_code_seq`: migration dùng role quản trị nhưng API dùng role giới hạn. Migration additive `20261102010000_payos_sequence_privileges` chỉ cấp USAGE sequence cho các role đã có INSERT payments, không nâng API thành admin; đã áp dụng bằng Prisma deploy trên VPS.

Lỗi kế tiếp: phản hồi tạo link có chữ ký hợp lệ dùng tài khoản định danh MB khác nhau cho từng đơn, không phải STK vật lý cấu hình. Đã giữ đối chiếu orderCode/amount/currency và chữ ký SDK, lưu tài khoản định danh từ phản hồi ký trên từng intent; receipt chỉ chấp nhận STK vật lý cấu hình hoặc đúng tài khoản định danh của intent đó. Không chấp nhận tài khoản định danh của đơn khác. Check khi transaction trước chưa tạo intent gọi lại start idempotent trên cùng order/deadline. Sửa đăng ký FCM dùng executeRaw cho hàm khóa advisory trả void, tránh Prisma UnsupportedNativeDataType.

Đã thao tác app thật trên emulator tới QR payOS 150000đ của order 5adb1a5e-171b-48ba-91dc-8f5e1dc71c7d, mã payOS N81000003; ảnh real-payment-qr.png ngoài repo. Đọc ngược QR từ chính ảnh: amount150000/currency704/countryVN/CRC16 hợp lệ. Không tự chuyển tiền, không đánh dấu PAID. Giữ deadline thật; QR hết hạn không được dùng lại hay kéo dài. Thu/chi/email/FCM đang bật ở VPS nhưng không suy diễn thành giao dịch tiền thật đã nghiệm thu.

Regression PostgreSQL bằng role không superuser: quyền sequence BASELINE=false → MODIFIED=true → ROLLBACK=false trên DB dùng một lần; 15/15 gồm start/check, QR VA, thanh toán, hoàn/resale, duplicate, timeout và FCM register. Provider của regression là giả lập, không coi là hoàn tiền thật. API quỹ chi payOS thật gọi chỉ đọc thành công; tại lúc kiểm tra chưa đủ hoàn150000đ. Cần bổ sung quỹ phù hợp trước tự test hoàn; chưa nhận hoàn tiền về ngân hàng thật là đạt. Không rollback production hoặc xóa đơn/vé/giao dịch.

Source thay đổi và bốn role vẫn ở D:\NT118\verification\week9-payos-ticket-refund; source gốc giữ nguyên. Không sửa Android/build APK trong đợt sửa backend này. Toàn bộ 3 task tuần9 vẫn chưa được đánh dấu hoàn thành trước nghiệm thu thu/hoàn thật và email tương ứng.

## Sửa DTO hoàn vé, hoàn thật và dọn đơn hết hạn — 2026-11-02

Đã tái hiện Android MissingFieldException tại $.items[1].ticket. GET /orders trả ticket tóm tắt id/status, không phải ServerTicket chi tiết. Đã tách OrderTicketSummary cho CustomerOrder.ticket; vẫn bắt buộc field tài chính/QR ở ServerTicket thật, không thêm default giả để né validation. Regression trước sửa 2 test/1 fail, sau sửa đạt; APK version4/1.3-week9-refund-fix đã cài lại giữ session. My Tickets đọc HTTPS thật không còn lỗi parse.

Nguồn trả tiền đã có đủ số tiền: API payOS lúc20:34:24 trả150000VND, khớp app Bảo Kim. Ảnh payOS0đ là ảnh thời điểm cũ/chưa đồng bộ; không suy diễn thành hai ví riêng hay yêu cầu nạp thêm. Hoàn bị review thực tế vì counterAccountBankId là CITAD01203001, không phải NAPAS BIN6chữ số. Mapping Vietcombank01203001→970436 được đối chiếu VSDC (https://vsdc.vn/vi/thong-tin-ngan-hang-thanh-vien-thanh-toan) và VietQR (https://api.vietqr.io/v2/banks). Mã CITAD khác chưa xác minh vẫn phải review; không đoán ngân hàng hay lấy tài khoản từ client.

Đã xác minh signed getLink/reference/amount/payer của receipt thật, số dư quỹ và không có payout; chỉ bổ sung recipient mã hóa vào cùng receipt/refund attempts0 và đưa lại đúng refund cũ vào REQUESTED, giữ id/reference. Worker tự chi đúng một lần: refund bff8756b-e0de-400b-8091-74157a29984a RESOLVED/SUCCEEDED/attempts1; provider query đúng1 SUCCEEDED. Email completed đã gửi. Người dùng xác nhận nhận đủ150000đ về Vietcombank và yêu cầu xóa sau đối soát.

Sau khi kiểm tra lại provider SUCCEEDED/amount/destination và xác nhận người dùng, đã xóa đúng vé thử/order5adb1a5e-171b-48ba-91dc-8f5e1dc71c7d: orders0/tickets0/order_seats0/payments liên kết0. Chỉ giữ receipt tombstone vô danh chứa reference chống callback trùng; không order/email/account/snapshot/tổng tiền. Backup trước xóa nằm /var/backups/n8-cineplex-private/reconciled-test-5adb1a5e-20261102.json quyền600 ngoài source/artifact; không restore vào production sau hoàn tiền.

Dọn tự động mỗi15giây: chỉ xóa vật lý đơn EXPIRED không có payment/ticket/reference và toàn bộ order_seats đã CANCELED, trong transaction khóa order. Đơn đã tạo link provider còn trace cần xử lý tiền đến trễ được giữ server nhưng không hiện ở danh sách. UI tự bỏ đơn chờ đúng deadline, không chờ người dùng tải lại. Không xóa vé đang chờ hoàn hoặc nghĩa vụ tài chính chưa đối soát.

Bằng chứng mới: backend131 unit đạt; PostgreSQL16/16 bằng role không superuser, gồm xóa hold rỗng/giữ trace/chống lặp và tiền hoàn; Android77 unit đạt cùng assembleDebug/assembleDebugAndroidTest/lintDebug. Test ISO fixture từng sai định dạng thiếu milliseconds đã giữ literal lỗi và sửa fixture đúng response server, không thay production để né test. Bốn role cũ mở rộng, source gốc giữ nguyên; không commit/push.

### Chốt lượt sửa và đối soát 2026-11-02
Sau khi người dùng xác nhận nhận đủ tiền, dữ liệu vé thử đã xóa đúng phạm vi như trên. API quỹ chi lúc20:48:15 trả0VND sau khoản hoàn150000đ, không phải báo mất tiền hay yêu cầu nạp lại giao dịch vừa hoàn. Các đơn hết hạn không có liên kết tài chính còn0; trace provider được giữ server nhưng không hiện giao diện.

Bổ sung nhánh retry: getLink đã xác minh thành công đưa intent còn hạn từ UNKNOWN về PROCESSING để dùng hosted checkout cũ; link provider đã EXPIRED/CANCELLED dừng poll/cancel lặp; không đổi NEEDS_ADMIN thành EXPIRED. Linux PostgreSQL role giới hạn17/17 đạt, fake provider. Native mới6/6, Android77unit; ba hash runtime payments.service/payment-domain/fcm.controller khớp local. APK version4 hash20aa573c9dd3b8eceea201d35d41826b4e97fc1b42dc170a143c9b6807ad04d9 đã cài.

Rollback bổ sung --refund-dto: cùng lệnh, cùng2test/input trên copy: BASELINE1fail MissingFieldException/exit1 → MODIFIED2pass/exit0 → ROLLBACK1fail MissingFieldException/exit1. Lỗi quay lại đúng theo baseline, không rollback APK/VPS hay tiền thật. Source active giữ MODIFIED. ROLLBACK.sh giữ mode toàn bộ source từ BASELINE.zip và mode DTO từ REFUND_DTO_BASELINE.zip; bằng chứng/ảnh/literal logs cùng bốn role hiện hữu.

## UI ưu đãi tham khảo ảnh người dùng và tab vé toàn hàng — 2026-11-02

Đã chỉnh VoucherPicker thành entry Ưu đãi dạng hàng thẻ trắng, minh họa %, chevron và mã đã chọn; bỏ nhãn/container trùng trong LiveCommerceScreen. Mở màn dialog toàn màn hình với header gradient hồng nhạt theo màu N8, voucher có ảnh/điều kiện/trạng thái, thanh Áp dụng cố định dưới cùng. Chọn/Bỏ chỉ đổi draft; Áp dụng mới gọi callback quote server; Back/đóng bỏ draft. Không nhập mã tay, không Xu/Credit, không ngân hàng/số dư giả. Giữ kiểm tra minimum, một voucher, guard busy/unknown/confirmed, original order và deadline. Thẻ không đủ điều kiện không bấm được. Dialog có trạng thái lỗi/rỗng theo danh sách thật, cuộn nội dung và system-bar contrast đúng.

TicketsScreen thay hai FilterChip nhỏ bằng hai thẻ selectable Role.Tab chia đều toàn chiều ngang, màu đậm cho mục chọn, số lượng, mô tả và vạch nhấn; nhãn có weight để không đè badge. Không thay nguồn dữ liệu, phân loại vé hay quyền hoàn. Cập nhật các caller/test tương tác mới, không sửa backend/schema/provider/FCM/secret hoặc deploy VPS trong đợt UI; 79 hash backend khớp trước sửa.

Kiểm chứng: assembleDebug/assembleDebugAndroidTest/testDebugUnitTest/lintDebug đạt, 77unit không fail/error. Native 8/8 gồm voucher apply/remove/cancel, disabled minimum, tab equal width>44% mỗi tab, selected semantics, cùng regression QR/payment/refund fakes. Hai test mới quan sát BASELINE2fail (chọn lập tức/thiếu full-width tabs) → MODIFIED2pass → ROLLBACK2fail; cùng lệnh instrument và input hành vi. adb đều exit0, riêng kết quả runner mới là bằng chứng pass/fail; không coi adb exit0 là tests đạt. ROLLBACK.sh --checkout-ui chạy trên checkout-ui-rollback, ba file restored khớp CHECKOUT_UI_BASELINE.zip chính xác; source active giữ modified, emulator cài lại APK mới. Bốn role cũ mở rộng, không thay lịch sử các mode all/--refund-dto.

Đã mở xem ảnh voucher-polished.png và ticket-tabs-polished.png từ emulator; ảnh đầu chụp giữa animation/systembar chưa chuẩn được giữ, ảnh cuối sau đợi350ms + sửa riêng dialog window background/contrast đã xem lại. Không phát sinh thanh toán/hoàn/fixture DB thật trong lượt UI này. APK versionCode5/versionName1.4-checkout-polish, SHA256 67576f0282da8e9ba70642146708fc22e265552b7f094da2cfe4b4990a391595. Source nằm candidate ngoài repo gốc; chưa commit/push. Chưa thử máy vật lý, font scale rất lớn hoặc nhiều viewport.
