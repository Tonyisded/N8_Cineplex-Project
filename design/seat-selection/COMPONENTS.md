# Cineplex — Hướng dẫn component Android

## Mở và chỉnh sửa

Mở `index.html` trong trình duyệt. HTML/CSS/JavaScript thuần, không cần cài dependency hoặc gọi API. Font, poster và ảnh diễn viên ở `assets/`. Trailer mở YouTube và cần Internet. Điểm vào mặc định là **chọn ghế** với suất mẫu. Có thể mở `#dashboard`, `#movie-detail`, `#showtimes` hoặc `#seat-handoff` để đến từng bước. Mở `preview/index.html` để dùng công cụ tình huống riêng, không có trên giao diện khách hàng.

## Hệ thống thiết kế

Hướng thiết kế được suy luận vì không có màn hình Cineplex tham chiếu: nền trắng lạnh `#F6F7F9`, surface `#FFFFFF`, chữ `#20222A`, chữ phụ `#5D6470`, nhấn `#8C2945`, nhấn nhẹ `#F5E8EC`, viền `#D9DDE4`. Chỉ ảnh phim có lớp chuyển màu để chữ dễ đọc.

Font Be Vietnam Pro cục bộ, 400/500/600/700. Body 16px, line-height 1.6; tiêu đề 20/24/28/32px; nhãn phụ 14px. Spacing 4/8/12/16/20/24/32/40/48/64px. Bo góc 8px cho badge, 12px cho control, 16px cho card, 24px cho sheet. Icon SVG outline cùng một bộ, nét 1.75. Vùng bấm tối thiểu 44px, nút chính 56px.

## Screen và luồng

| `data-screen` / route | Android đề xuất | Nội dung |
|---|---|---|
| `dashboard` | `DashboardEntryScreen` | Một thẻ phim để bắt đầu; không đại diện Dashboard đầy đủ |
| `movie-detail` | `MovieDetailScreen` | Backdrop, poster, thông tin phim, trailer, nội dung mở rộng, diễn viên, CTA |
| `showtimes` | `ShowtimesScreen` | Ngày, bộ lọc, nhóm rạp/định dạng, suất, trạng thái trống và CTA |
| `seat-handoff` / alias `seats` | `SeatSelectionScreen` | Sơ đồ ghế, giữ ghế, countdown, tổng tiền |
| `checkout-handoff` | `ConcessionsPaymentHandoffScreen` | Điểm bàn giao bắp nước/thanh toán |

Luồng: Dashboard → Chi tiết phim → Lịch chiếu → Chọn suất → Sheet T16 → Chọn ghế → Điểm bàn giao bắp nước/thanh toán. Quay lại giữ ngày, khu vực, rạp, suất còn hợp lệ và vị trí cuộn của màn hình trước. Đổi ngày/bộ lọc xóa suất đã chọn. Ngày 19/10/2026 có trạng thái trống. Ngày 18/10 có suất không khả dụng. Các ngày khác cũng có ô đã hết vé.

## Component dùng chung

| Tên trong nguồn | Android / Compose | Thuộc tính và trạng thái |
|---|---|---|
| `CineplexButton` | `Button`, `OutlinedButton`, `TextButton` | primary / secondary / text; default, pressed, focus, disabled |
| `AgeBadge` | `AgeBadge` composable | T16 nhất quán; nhãn accessible “Phân loại T16” |
| `DateCell` | `LazyRow` + selectable `Surface` | thứ, ngày/tháng, “Có suất”/“Chưa có”; selected có chữ và dấu chọn |
| `ShowtimeChip` | `FilterChip` / custom `Surface` | giờ, default / selected / unavailable; hết vé không thể chọn |
| `CinemaGroup` | `Column` | tên, địa chỉ, các nhóm định dạng và lưới suất |
| `MovieSummary` | `Row` | poster đầy đủ, tên phim, T16, thời lượng |
| `CastItem` | `LazyRow` item | ảnh đại diện crop có chủ đích; tên đầy đủ trong sheet khi chạm |
| `ActionBar` | `Scaffold.bottomBar` | đặt chỗ riêng trong layout; không che vùng cuộn |
| `CineplexSheet` | `ModalBottomSheet` | tiêu đề, nội dung, close/back; focus trap, Escape, trả focus |

Tất cả tên component được đánh dấu bằng `data-component`; token nằm trên `:root`. Các function `Button`, `AgeBadge`, `DateCell`, `ShowtimeChip`, `MovieSummary`, `CinemaGroup` tạo nội dung dùng chung. `render()` là nơi nối màn hình; `state` là trạng thái giao diện duy nhất.

## Cảnh báo tuổi

Sheet hiển thị đúng hai câu:

> Phim dành cho khán giả từ đủ 16 tuổi
>
> Rạp có thể yêu cầu giấy tờ xác minh độ tuổi khi soát vé.

Hai nút chính: **Quay lại** và **Tôi từ đủ 16 tuổi — Tiếp tục**. Liên kết phụ **Tôi chưa đủ 16 tuổi** đưa tới thông báo không đủ tuổi, với **Chọn phim khác** và **Quay lại**. Không có xác minh thật; xác nhận chỉ minh họa chuyển bước. Sheet chưa đủ tuổi không được tiếp tục tới ghế.

## Dữ liệu và phạm vi

- Phim: Dune: Hành tinh cát – Phần hai; 166 phút; T16; Giả tưởng, Hành động. Ngày khởi chiếu tham chiếu lần phát hành lại: 25/02/2025 theo Galaxy Cinema.
- Điểm 8,6/10 và 1.280 lượt đánh giá là mẫu. Ngày chiếu 16–22/10/2026, các chi nhánh Cineplex, địa chỉ và suất chiếu là minh họa, không phải thông tin đặt vé thật.
- Định dạng lồng tiếng là dữ liệu minh họa của UI, không khẳng định bản phát hành thực tế.
- Không có Login/Signup, Dashboard đầy đủ, API, bắp nước đầy đủ hoặc thanh toán thật.

## Accessibility và nguồn tài sản

Control semantic, nhãn tiếng Việt, focus rõ, dialog quản lý focus và đóng bằng Escape. Trạng thái chọn/hết vé có chữ; thông báo dùng live region. Giảm chuyển động theo hệ thống, không khóa zoom. Rail ngày và diễn viên cuộn nội bộ; nội dung chính không bị thanh dưới che. Ảnh poster 780×1170, backdrop 1280×720; ảnh Timothée 500×676, Zendaya 500×751, Rebecca 500×750. Poster giữ toàn khung; avatar và backdrop là crop trang trí có chủ đích.

Nguồn chi tiết trong `assets/sources.json`; font có giấy phép tại `assets/FONT-LICENSE.txt`. Quyền hình phim thuộc chủ sở hữu; ảnh diễn viên có nguồn và giấy phép tại Wikimedia. Đây là tài sản tham khảo cho prototype.

Giao diện được xây dựng trực tiếp từ source theo yêu cầu; chưa mở preview hoặc chạy kiểm tra sau khi ghi deliverable.

Lệnh đọc hướng dẫn media của runtime trả về nội dung rỗng. Tài sản được chuẩn bị theo quy tắc nguồn ảnh thật và lưu cục bộ đã chốt trong kế hoạch; không dùng hướng dẫn media chưa đọc được.

## Bổ sung: Chọn ghế / SeatSelection

### Source và dữ liệu

`seats.js` quản lý component và state ghế; `seats.css` mở rộng token; `app.js` nối route, modal và dữ liệu suất. `index.html` tải module ghế trước app. Giữ `#seat-handoff` để liên kết xác nhận tuổi cũ vẫn hoạt động; `#seats` là alias. `#checkout-handoff` chỉ mở khi có ghế hợp lệ, online và còn thời gian.

Kế thừa token baseline chi tiết phim và `detailui` trong ZIP. Source đăng nhập có token khác nên không đổi toàn ứng dụng. Không cần tải tài sản mới.

Đi từ lịch chiếu sẽ kế thừa suất thực sự chọn. Mở trực tiếp dùng Dune: Hành tinh cát – Phần hai, T16, Cineplex Đồng Khởi, Phòng 03, 19:30 · 16/10/2026, 2D · Phụ đề. Ngày này là dữ liệu mẫu kế thừa, không phải lịch chiếu hiện hành.

### Sơ đồ và giá

- Sáu vị trí mỗi hàng; A–D thường: 90.000đ/chỗ. E–H VIP: 120.000đ/chỗ. I–J Couple: ba cặp mỗi hàng, 240.000đ/cặp gồm hai chỗ.
- Lối đi giữa ghế 3/4 ở hàng đơn. Couple ghép 1–2, 3–4, 5–6; mỗi cặp là một entity, không có thao tác nửa cặp.
- Sơ đồ gốc 336px; target đơn 44px, gap 8px. VIP dùng viền đôi và V; Couple có hình đôi và hai mã. Trạng thái có viền, marker và accessible name, không chỉ màu.
- Zoom 100–200%, bước 25%, không giảm target dưới mức gốc. Pan bằng kéo chuột, cuộn/chạm hoặc phím mũi tên trên viewport. Ghế luôn có thể tiếp cận qua vùng cuộn. Reset trả về 100% và vị trí đầu.
- Ghế người khác giữ/đã bán dùng `aria-disabled` nhưng vẫn nhận chạm để giải thích. Mất mạng hoặc đang đồng bộ khóa chọn/bỏ và tiếp tục; vẫn xem và di chuyển sơ đồ được.

### Component Android

| Tên / `data-component` | Compose đề xuất | Thuộc tính chính |
|---|---|---|
| `SingleSeat` | `SeatButton` | id, thường/VIP, trạng thái, viền/marker, accessible name |
| `CoupleSeat` | `CoupleSeatButton` | hai mã, giá theo cặp, chọn/bỏ nguyên entity |
| `HoldCountdown` | `HoldCountdown` + ViewModel | deadline, idle/active/urgent/expired |
| `SeatLegend` | `SeatLegend` | hình ghế, marker và nhãn giải nghĩa |
| `ConnectionBanner` | `ConnectionBanner` | online/updating/offline; thông báo không lặp mỗi giây |
| `SeatSummaryBar` | `Scaffold.bottomBar` | danh sách cuộn ngang, tổng tiền, số chỗ, pending/disabled/loading |
| `SeatBookingSummary` | `BookingSummary` | phim, T16, rạp, phòng, ngày/giờ và định dạng |
| `CineplexSheet` | `ModalBottomSheet` | hết hạn/rời trang, focus trap và trả focus |

State gồm `available`, `selected`, `pending`, `held`, `sold`. `held` là người khác giữ, không có danh tính. Entity Couple có id `J1-J2`, hiển thị `Couple J1–J2 · 2 chỗ`. Giá là số nguyên VND, format tại lớp hiển thị.

### Giữ ghế và countdown

1. Chọn ghế tạo pending 650ms, cập nhật tạm tổng tiền và khóa bấm lặp. Giữ thành công đầu tiên mới bắt đầu 10:00. Lần giữ đầu thất bại không khởi động timer.
2. Deadline = thời điểm thành công đầu tiên + 600.000ms. Thêm, đổi hoặc bỏ hết ghế không reset phiên đang còn hạn. Tính thời gian từ deadline tuyệt đối; mất mạng hoặc tab ẩn không dừng timer.
3. Dưới 120 giây hiển thị “Thời gian giữ ghế sắp hết”. Không live-announce mỗi giây.
4. Đến 00:00 xóa lựa chọn và pending, hủy callback cũ bằng generation token, tổng tiền về 0, khóa tiếp tục và mở dialog đúng nội dung yêu cầu. Sau hết hạn, giữ thành công một ghế mới mở phiên 10 phút mới.
5. Tiếp tục có trạng thái xác nhận 800ms, kiểm tra lại ghế, thời gian và kết nối trước khi đến điểm bàn giao. Không tạo đơn hoặc thanh toán thật.

### Realtime và kết nối

`snapshot` là tồn kho mô phỏng, tách khỏi sơ đồ đang xem khi offline. Online cập nhật ngay trạng thái; ghế hoặc cặp không còn hợp lệ bị bỏ nguyên entity, cập nhật tổng tiền. Phản hồi pending đọc lại snapshot để xử lý xung đột. Generation token ngăn phản hồi cũ sau hết hạn, rời trang hoặc mất mạng.

Offline giữ góc nhìn và ghế đã xác nhận, xóa pending chưa xác nhận, khóa chọn/bỏ/tiếp tục. Kết nối lại mô phỏng đồng bộ 900ms, áp dụng snapshot, loại lựa chọn không hợp lệ, kiểm tra deadline và thông báo thay đổi. Timer vẫn chạy khi offline và kiểm tra lại khi tab hiển thị.

Back trong app và điều hướng hash khi giữ/pending mở dialog “Bạn muốn rời màn hình chọn ghế? Các ghế đang giữ sẽ được giải phóng.” với “Ở lại” và “Rời màn hình”. Rời hoặc tải lại tài liệu dùng `beforeunload`: trình duyệt quyết định có hiện cảnh báo hay không và dùng lời thoại mặc định; không thể ép nội dung tùy chỉnh trên mọi trình duyệt.

### Preview riêng

Mở `preview/index.html`; điều khiển nằm ngoài iframe ứng dụng. Iframe bật `?preview=1`, chỉ nhận lệnh từ đúng parent, cùng origin và kênh `cineplex-seat-preview-v1`, trong allowlist action. App bình thường bỏ qua lệnh mô phỏng.

| Tình huống | Thao tác preview | Kết quả |
|---|---|---|
| Giữ/bán | Người khác giữ A2 / B4 được mua | Sơ đồ đổi, chạm để xem lý do |
| Giải phóng | Giải phóng A2 và E2 | Trở về còn trống |
| Xung đột F6 | Xung đột ghế F6 | Tự thử chọn, pending rồi thất bại và báo đúng thông điệp |
| Couple thất bại | Xung đột Couple J1–J2 | Bỏ nguyên cặp và cập nhật tiền |
| Cập nhật | Đang cập nhật ghế… | Banner và khóa chọn tạm thời |
| Sắp hết | Giữ ghế rồi chọn Còn 01:59 | Hiện cảnh báo, không phải chờ 8 phút |
| Hết giờ | Giữ ghế rồi chọn Hết thời gian | Xóa lựa chọn, mở dialog |
| Offline | Mất kết nối | Sơ đồ giữ nguyên, khóa thao tác |
| Đồng bộ | Offline → cập nhật A2/B4 → Kết nối lại | Snapshot chỉ xuất hiện khi kết nối lại |
| Mất lựa chọn | Kết nối lại, một lựa chọn bị giữ | Bỏ một entity/cặp đang chọn, cập nhật giá và thông báo |
| Phiên mới | Đặt lại toàn bộ mô phỏng | Xóa lựa chọn, deadline, lỗi và trả sơ đồ ban đầu |

Không có thông tin cá nhân người giữ ghế. JavaScript chỉ mô phỏng cục bộ, không có backend/WebSocket. Tải lại mất phiên. Nguồn được viết trực tiếp theo yêu cầu; không mở preview hoặc chạy kiểm thử sau khi ghi trong quy trình bàn giao này.
