# Cineplex — bàn giao giao diện sáng / isometric

Mở `index.html` để bắt đầu ở đăng nhập khách hàng. Sáu màn hình: login khách hàng, signup, login nhân viên, Home, chi tiết phim và dashboard admin. Không có khung điện thoại, chưa kết nối backend; nội dung và lịch chiếu là dữ liệu mock.

## Nguồn

- `assets/cineplex.css`: token và layout responsive dùng chung.
- `assets/cineplex.js`: UI, biểu mẫu, điều hướng và xác thực mock.
- `assets/movies.js`: giữ nguyên sáu bản ghi phim, mô tả, đạo diễn và diễn viên.
- `assets/cinema-isometric.png`: minh họa hư cấu dùng chung, 1536×1024, tạo bằng imagegen tích hợp. Hiển thị full frame, không cắt hình. Poster thật giữ nguyên tại assets/*.jpg và nguồn tại assets/sources.txt; chưa xác lập quyền thương mại.
- Các nguồn app.*, auth.* và auth-cinema.css cũ giữ để tham khảo, không còn được entry tải.

Hướng burgundy trên xám ấm sáng được agent chọn theo yêu cầu và bố cục ảnh mẫu. Prompt imagegen: “Airy isometric miniature cinema, light warm gray #F5F3F0, burgundy #7B263D seats, projection screen with abstract cinema shapes, person booking on a mobile device, tickets and striped popcorn carton; matte materials, subtle shadows, dusty rose/cream/charcoal palette, full scene inside frame, no lettering/logos/neon/glow.”

## Design token

| Token | Giá trị | Compose |
| --- | --- | --- |
| --bg | #F5F3F0 | background |
| --surface | #FFFFFF | surface |
| --raised | #ECE7E9 | surfaceVariant |
| --primary | #7B263D | primary |
| --pressed | #5C1C2E | pressed |
| onPrimary | #FFFFFF | onPrimary |
| --text | #25232A | onSurface |
| --muted | #625D65 | onSurfaceVariant |
| --line | #D9D3D6 | outline bề mặt |
| --error | #A12835 | error |
| --success | #246345 | success tùy chỉnh |

Typography: Georgia serif cho logo/heading, system-ui cho body/control. Scale 16/18/24/32/40px, body line-height 1.5, heading 1.2. Compose dùng serif có đầy đủ tiếng Việt và body Android. Font không phụ thuộc mạng.

Spacing 4/8/12/16/24/32/48/64px tương ứng dp; radius 8/12/16dp. Shadow 0 8px 24px rgba(37,35,42,0.08), chuyển sang elevation nhẹ. Vùng chạm 48dp, input và submit 56dp. Icon SVG outline nét 2px, đầu nét bo, kích thước 24px; field 20px. Focus outline 3px burgundy; error có chữ và viền, success có thông báo. Enter 180ms, pressed/exit 120ms; chỉ transform/opacity; reduced-motion tắt hiệu ứng không thiết yếu.

## Thành phần và responsive

CineplexBrand, CinemaIllustration, AuthTabs, AuthHeading, LabeledTextField, PasswordField, ErrorSummary, PrimaryButton, SecondaryButton, ConsentRow, NoticeDialog, AccountDialog, CinemaBanner, MovieTabs, MovieCard, BottomNavigation, MovieDetails, AdminMenuItem.

- Mobile 360–430px: một cột, padding ngang 24dp, minh họa login tối đa 300px rộng, signup 180px, staff 240px. Form ở normal flow, cuộn được; không ép hai cột.
- Tablet từ 768px: padding 48dp, grid phim 3 cột, detail poster trái/nội dung phải. Auth vẫn có form tối đa 440px.
- Wide từ 1024px: auth illustration trái/form phải; panel tối đa 480px gồm padding 32px, form bên trong 416px; grid phim 4 cột.
- Poster full frame: Dune 259×384, năm poster còn lại 500×750; width 100%, height auto. Banner Home nhỏ hơn khu vực phim.
- Home có vùng dự phòng bottom navigation 104px + safe-area; detail booking 112px + safe-area. Dùng env(safe-area-inset-*), 100dvh, interactive-widget=resizes-content; VisualViewport cuộn trường đang focus vào vùng nhìn thấy khi bàn phím mở. Hành vi bàn phím phụ thuộc trình duyệt/WebView.
- Tài khoản Home mở qua avatar hoặc mục Tài khoản; có đăng xuất. Admin có nút đăng xuất trực tiếp. Native dialog hỗ trợ focus trap, Escape, nút đóng và trả focus.

## Tài khoản thử

| Email | Mật khẩu | Role | Đăng nhập |
| --- | --- | --- | --- |
| user@cineplex.test | 123456 | USER | Khách hàng |
| admin@cineplex.test | 123456 | ADMIN | Nhân viên |

Không hiển thị hoặc điền sẵn thông tin trên trong UI. Tên hiển thị mặc định là Khách hàng / Quản trị viên; người đăng ký mới dùng họ tên đã nhập. Quy định 8 ký tự chỉ áp dụng đăng ký mới.

## Luồng và trạng thái

1. Login validate required/email khi blur và submit. Loading 900ms có nhãn xử lý, khóa gửi lặp. USER/customer → Home; ADMIN/staff → admin. USER/staff báo “Tài khoản không có quyền truy cập”; ADMIN/customer có hướng dẫn và liên kết chuyển staff. Sai thông tin báo sai tài khoản hoặc mật khẩu.
2. Signup kiểm tra tên trống/khoảng trắng, email trim/lowercase/định dạng/trùng, password tối thiểu 8 ký tự, confirm khớp và consent. Lỗi chỉ hiển thị dưới trường tương ứng; gửi sai sẽ focus trường lỗi đầu tiên. Hai password có hiện/ẩn độc lập. Hai điều khoản mở nội dung mẫu.
3. Thành công tạo USER trong bộ nhớ, replace về customer login, điền email vừa tạo, để password trống và hiện “Đăng ký thành công! Vui lòng đăng nhập”. Tài khoản mới dùng được sau logout trong cùng lần mở trang, mất khi reload.
4. Tab và link chuyển login/signup/staff. Back signup/staff về customer login. Chuyển form xóa password; generation token hủy callback đăng nhập/đăng ký cũ khi điều hướng.
5. Home tìm theo tên tiếng Việt không dấu và tên gốc, lọc theo tab, có empty state. Nút Đặt vé trên thẻ mở đúng detail; nút Đặt vé tại detail báo chưa triển khai. Back detail giữ query/tab/scroll và focus poster khi có.
6. Logout xóa session, tăng epoch, replace login và reset trạng thái Home. Popstate kiểm tra phiên/epoch để Back không mở màn hình được bảo vệ. Không thể xóa toàn bộ lịch sử trình duyệt; các entry cũ bị route guard vô hiệu hóa.
7. Quên mật khẩu báo “Chức năng sẽ được bổ sung”. Ba mục admin và các mục chưa triển khai báo thông báo hiện có. Không có biểu đồ, số liệu, social login hay chức năng ngoài phạm vi.

Không dùng localStorage/sessionStorage, không ghi mật khẩu vào log. Credentials có trong source để mô phỏng, không phải cơ chế bảo mật sản phẩm. Lịch chiếu và giới hạn tuổi chỉ minh họa.

## Kotlin + Jetpack Compose

- Đưa token vào CineplexTheme, ColorScheme, Typography, Shapes và spacing object.
- AuthScreen dùng BoxWithConstraints: Row trên wide, Column trên mobile; verticalScroll, imePadding, navigationBarsPadding, statusBarsPadding và BringIntoViewRequester cho field focus. Submit nằm trong luồng cuộn.
- OutlinedTextField có label, leadingIcon, trailingIcon, supportingText và isError; visibility state riêng từng password. Checkbox target 48dp; điều khoản dùng TextButton hoặc AnnotatedString có liên kết riêng.
- Scaffold cho Home/detail với innerPadding và bottomBar, không mang hằng số padding HTML sang Compose. LazyVerticalGrid adaptive; poster ContentScale.Fit với tỷ lệ thật. Detail wrap đầy đủ nội dung.
- AlertDialog cho thông báo/điều khoản. State loading/disabled/error/success có semantics và nhãn rõ.
- NavHost: login, signup, staff-login, home, movie/{id}, admin. Logout popUpTo graph inclusive; bảo vệ route bằng ViewModel session/role. Back signup/staff về login khách hàng.
- ViewModel giữ session, query, tab, scroll và mock accounts trong bộ nhớ; repository có delay 900ms. Không lưu password qua SavedStateHandle, persistent storage hoặc log. Sản phẩm thật cần backend xác thực riêng.

Nguồn được hoàn thiện theo yêu cầu; không thực hiện preview, screenshot hay kiểm thử sau khi giao entry.

## Điều chỉnh footer xác thực
Đã bỏ hai dòng Chưa có tài khoản? / Đã có tài khoản? và nút phụ tương ứng. Khách hàng chuyển form bằng hai tab Đăng nhập / Đăng ký phía trên. Liên kết Nhân viên đăng nhập? được giữ nguyên.


## Gợi ý và lỗi trong form
Các ô họ tên, email, mật khẩu và xác nhận mật khẩu có placeholder màu muted bên cạnh icon hiện có. Nhãn luôn hiển thị. Bỏ khung tổng hợp lỗi phía trên; lỗi kiểm tra dữ liệu hiện dưới từng ô, có aria-live. Compose dùng placeholder và supportingText/isError tương ứng.

Tiêu đề đăng ký dùng font system-ui hỗ trợ dấu tiếng Việt, weight 700, line-height 1.3, letter-spacing normal. Không hiện viền focus cho heading không tương tác; control vẫn giữ focus rõ.


Checkbox điều khoản căn giữa với dòng đầu của nội dung đồng ý. Nhãn và liên kết dùng line-height 48px, bỏ padding dọc riêng của liên kết để không lệch baseline; giữ vùng chạm 48px và xuống dòng tự nhiên. Compose dùng Row với checkbox 48dp và nội dung có dòng đầu căn giữa cùng hàng.

## Logo và lời chào cập nhật
Dùng ảnh logo người dùng cung cấp tại assets/cineplex-logo.png, kích thước gốc 1394×1128, giữ đầy đủ hình và tỷ lệ. Logo rộng 160px ở xác thực, 112px ở header; không lặp tên thương hiệu bên cạnh. Đăng nhập khách hàng dùng tiêu đề Hẹn bạn ở rạp và giữ nguyên Một bộ phim hay đang chờ bạn. Đăng nhập để tiếp tục. Tiêu đề dùng system-ui hỗ trợ dấu tiếng Việt. Trên mobile, minh họa đăng nhập có max-width 240px để ưu tiên form. Compose dùng Image với ContentScale.Fit và kích thước thích ứng. Công cụ đọc hướng dẫn media không trả nội dung; logo được sao chép nguyên bản, không xử lý hình ảnh.

## Xác thực tinh gọn
Bỏ banner minh họa khỏi ba trang xác thực, giữ banner Home/admin. Mobile dùng logo trung tâm rộng 160px, khối form tối đa 440px, cách logo và form 32px; tab chọn có nền nhẹ và gạch burgundy. Desktop từ 1024px dùng cột logo/lời dẫn và cột form với gap 64px; logo rộng 240px. Tiêu đề xác thực dùng system-ui, line-height 1.3. Giữ câu Một bộ phim hay đang chờ bạn và toàn bộ luồng mock. Compose dùng Column trên mobile, Row chia cột ở màn hình rộng.

## Nút Google
Đăng nhập khách hàng có nút Đăng nhập bằng Google dưới nút chính và dòng hoặc. Nút nền trắng, viền line, cao tối thiểu 56px, bo 12px, có focus/pressed/disabled và khóa khi gửi form. Bấm mở thông báo Chức năng sẽ được bổ sung; chưa tích hợp OAuth, không tạo phiên. Trang nhân viên không có nút này. Compose dùng OutlinedButton và AlertDialog.

Icon Google chính thức lấy từ https://developers.google.com/identity/images/g-logo.png, lưu tại assets/google-logo.png (200×204). Hiển thị rộng 20px, giữ tỷ lệ tự nhiên, cách nhãn 12px. Icon trang trí có alt rỗng; tên nút được đọc từ nhãn. Hướng dẫn media không trả nội dung; dùng ảnh gốc từ Google. Hành vi nút không đổi.

## Trang Thông tin cá nhân
Mở từ avatar trên Home/dashboard quản trị hoặc mục Tài khoản. Hiển thị avatar chữ cái, họ tên, email và loại tài khoản lấy từ phiên đăng nhập. Không tự thêm số điện thoại, ngày sinh hoặc dữ liệu cá nhân giả. Session chỉ chứa name, email, role; không chứa mật khẩu.

Khách hàng có thanh điều hướng với Tài khoản được chọn. Trang chủ/nút quay lại khôi phục tìm kiếm, tab và vị trí cuộn Home. Nhân viên/admin quay về dashboard quản trị và không có thanh điều hướng khách hàng. Trang cá nhân được bảo vệ theo phiên; đăng xuất quay về đăng nhập, Back không mở lại màn hình được bảo vệ. Tài khoản mới vẫn ở trong bộ nhớ sau đăng xuất và mất khi tải lại.

Chỉnh sửa thông tin, Đổi mật khẩu, Trợ giúp & hỗ trợ mở “Chức năng sẽ được bổ sung”. Chính sách bảo mật mở dialog mẫu hiện có. Chưa kết nối backend.

Token: nền #F5F3F0, surface #FFFFFF, primary #7B263D, chữ #25232A, muted #625D65, viền #D9D3D6. System-ui: tiêu đề 32px, mục 24px, nội dung 16px. Avatar 80px; spacing 8/12/16/24/32/48px, khối thông tin bo 16px, nút bo 12px, vùng chạm >=48px. Mobile xếp dọc, tablet >=768px chia cột nhận diện/nội dung, rộng tối đa 960px. Có vùng an toàn, chừa khoảng trống bottom navigation, focus/pressed và reduced-motion theo token chung.

Jetpack Compose: ProfileScreen dùng Scaffold, TopAppBar, NavigationBar cho USER, Column.verticalScroll trên mobile và Row trên tablet. Surface tròn chứa initials, thông tin dùng Column/Text, các thao tác dùng ListItem clickable, đăng xuất OutlinedButton, phản hồi AlertDialog. ViewModel giữ AccountUiState(name,email,role); navigation popUpTo xóa các màn hình được bảo vệ khi đăng xuất. Tái sử dụng token Cineplex.
