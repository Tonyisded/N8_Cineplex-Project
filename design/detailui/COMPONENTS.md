# Cineplex — Hướng dẫn component Android

## Mở và chỉnh sửa

Mở `index.html` trong trình duyệt. HTML/CSS/JavaScript thuần, không cần cài dependency hoặc gọi API. Font, poster và ảnh diễn viên ở `assets/`. Trailer mở YouTube và cần Internet. Điểm vào mặc định là chi tiết phim; nút quay lại đưa đến thẻ phim Dashboard tối thiểu. Có thể mở `#dashboard`, `#movie-detail`, `#showtimes` để đến từng điểm vào.

## Hệ thống thiết kế

Hướng thiết kế được suy luận vì không có màn hình Cineplex tham chiếu: nền trắng lạnh `#F6F7F9`, surface `#FFFFFF`, chữ `#20222A`, chữ phụ `#5D6470`, nhấn `#8C2945`, nhấn nhẹ `#F5E8EC`, viền `#D9DDE4`. Chỉ ảnh phim có lớp chuyển màu để chữ dễ đọc.

Font Be Vietnam Pro cục bộ, 400/500/600/700. Body 16px, line-height 1.6; tiêu đề 20/24/28/32px; nhãn phụ 14px. Spacing 4/8/12/16/20/24/32/40/48/64px. Bo góc 8px cho badge, 12px cho control, 16px cho card, 24px cho sheet. Icon SVG outline cùng một bộ, nét 1.75. Vùng bấm tối thiểu 44px, nút chính 56px.

## Screen và luồng

| `data-screen` / route | Android đề xuất | Nội dung |
|---|---|---|
| `dashboard` | `DashboardEntryScreen` | Một thẻ phim để bắt đầu; không đại diện Dashboard đầy đủ |
| `movie-detail` | `MovieDetailScreen` | Backdrop, poster, thông tin phim, trailer, nội dung mở rộng, diễn viên, CTA |
| `showtimes` | `ShowtimesScreen` | Ngày, bộ lọc, nhóm rạp/định dạng, suất, trạng thái trống và CTA |
| `seat-handoff` | `SeatSelectionHandoffScreen` | Điểm chuyển tiếp có thông tin suất; không có sơ đồ ghế |

Luồng: Dashboard → Chi tiết phim → Lịch chiếu → Chọn suất → Sheet T16 → Điểm chọn ghế. Quay lại giữ ngày, khu vực, rạp, suất còn hợp lệ và vị trí cuộn của màn hình trước. Đổi ngày/bộ lọc xóa suất đã chọn. Ngày 19/10/2026 có trạng thái trống. Ngày 18/10 có suất không khả dụng. Các ngày khác cũng có ô đã hết vé.

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
- Không có Login/Signup, Dashboard đầy đủ, API, thanh toán hoặc chọn ghế.

## Accessibility và nguồn tài sản

Control semantic, nhãn tiếng Việt, focus rõ, dialog quản lý focus và đóng bằng Escape. Trạng thái chọn/hết vé có chữ; thông báo dùng live region. Giảm chuyển động theo hệ thống, không khóa zoom. Rail ngày và diễn viên cuộn nội bộ; nội dung chính không bị thanh dưới che. Ảnh poster 780×1170, backdrop 1280×720; ảnh Timothée 500×676, Zendaya 500×751, Rebecca 500×750. Poster giữ toàn khung; avatar và backdrop là crop trang trí có chủ đích.

Nguồn chi tiết trong `assets/sources.json`; font có giấy phép tại `assets/FONT-LICENSE.txt`. Quyền hình phim thuộc chủ sở hữu; ảnh diễn viên có nguồn và giấy phép tại Wikimedia. Đây là tài sản tham khảo cho prototype.

Giao diện được xây dựng trực tiếp từ source theo yêu cầu; chưa mở preview hoặc chạy kiểm tra sau khi ghi deliverable.

Lệnh đọc hướng dẫn media của runtime trả về nội dung rỗng. Tài sản được chuẩn bị theo quy tắc nguồn ảnh thật và lưu cục bộ đã chốt trong kế hoạch; không dùng hướng dẫn media chưa đọc được.
