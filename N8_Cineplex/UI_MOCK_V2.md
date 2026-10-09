# Cineplex Compose UI v2

## Các file triển khai
- `app/src/main/java/com/nhom8/cineplex/MainActivity.kt`: khởi động, điều hướng, Back và system bars.
- `model/Models.kt`: model tài khoản, phim, form và thông báo.
- `data/MockAccountRepository.kt`: tài khoản trong bộ nhớ, validation và kiểm tra vai trò.
- `data/MockMovies.kt`: sáu phim từ movies.js, tìm kiếm không phân biệt dấu.
- `ui/CineplexViewModel.kt`: trạng thái form, loading, phiên và điều hướng.
- `ui/screens/`: AuthScreen (login/signup/staff), HomeScreen, MovieDetailScreen, AdminScreen, ProfileScreen.
- `ui/components/`: icon, logo, banner, dialog, button, form, tab và thẻ phim.
- `ui/theme/CineplexMockTheme.kt`: palette, typography và shapes riêng của bản mới.
- `app/src/main/res/drawable/`: logo, minh họa rạp và sáu poster nguyên bản.
- `app/src/main/res/values/themes.xml`: nền khởi động và system bars sáng.
- `app/build.gradle.kts`: ViewModel KTX và coroutine test.
- `gradle.properties`: encoding COMPAT để Gradle test worker xử lý đường dẫn Windows có dấu; không tắt test.
- `app/src/test/.../MockAccountRepositoryTest.kt`, `CineplexViewModelTest.kt`: kiểm thử xác thực, đăng ký, vai trò và điều hướng.
- `app/src/androidTest/.../CineplexUiTest.kt`: kiểm thử các luồng Compose trên emulator.

Các thay đổi sẵn có của người dùng được giữ lại; không commit/push.

## Kiểm tra
Lệnh: `./gradlew.bat assembleDebug testDebugUnitTest connectedDebugAndroidTest --console=plain`.
Build thành công. 16 unit tests và 7 instrumentation tests (gồm test có sẵn) thành công, không bỏ qua/vô hiệu hóa test.
Đã kiểm tra sáu màn hình trên emulator ở 360dp và 430dp; 7 instrumentation tests đạt ở cả hai kích thước. Đã mở MainActivity thực tế để kiểm tra khởi động và system bars. Ảnh nằm ở `app/build/visual-360/visual_checks/` và `app/build/visual-430/visual_checks/`; ảnh khởi động ở `app/build/visual-430/startup.png`. Kích thước emulator đã được khôi phục sau kiểm tra.
APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Cách thử
- Mở app: đăng nhập khách hàng, không có tài khoản điền sẵn.
- Khách hàng: `user@cineplex.test` / `123456` → Home.
- Nhân viên: chọn “Nhân viên đăng nhập?”, dùng `admin@cineplex.test` / `123456` → admin.
- Dùng USER ở đăng nhập nhân viên hoặc ADMIN ở đăng nhập khách hàng để kiểm tra từ chối/hướng dẫn vai trò.
- Đăng ký email mới với mật khẩu từ 8 ký tự, xác nhận và đồng ý điều khoản. Email được trim, so sánh không phân biệt hoa/thường. Sau thành công, email được điền ở login, mật khẩu để trống.
- Tìm kiếm “vung dat” hoặc “DUNE”, đổi tab, mở chi tiết rồi Back để kiểm tra giữ bộ lọc.
- Chức năng đặt vé, quên mật khẩu và các mục chưa có mở dialog.
- Avatar trên Home/admin hoặc mục Tài khoản mở trang Thông tin cá nhân; trang profile có nút đăng xuất, admin vẫn có nút đăng xuất. Back sau đăng xuất không trở lại phiên cũ.
- Tài khoản đăng ký tồn tại qua đăng xuất nhưng mất khi tiến trình kết thúc. Không ghi mật khẩu vào file, preferences hay log; chưa kết nối backend.



## Cập nhật 2026-10-09 theo signin-signup.zip mới

- Hoàn thành UI mock Google và Profile theo ZIP mới; session thêm email chuẩn hóa, không chứa password. Google/chỉnh sửa/đổi mật khẩu/hỗ trợ chỉ mở thông báo; chưa tích hợp backend.
- Profile có dữ liệu phiên, avatar initials, thông tin tài khoản, bảo mật/hỗ trợ và logout. Khách hàng có navigation chọn Tài khoản; admin quay về dashboard. Back/Trang chủ giữ bộ lọc và vị trí scroll Home.
- Sửa lỗi lint NewApi sẵn có: light navigation bar nằm trong values-v27, minSdk vẫn 24.
- Build + 18 unit tests + lint đạt (0 lỗi, 23 cảnh báo). 10 instrumentation tests đạt ở 360×800dp; 3 tests mới đạt thêm trên APK cuối cùng ở 360×800dp và 820×1180dp.
- Đã xem ảnh tại app/build/visual-profile-360/visual_checks/ và app/build/visual-profile-tablet/visual_checks/. Emulator đã khôi phục kích thước gốc. APK hiện hành vẫn ở app/build/outputs/apk/debug/app-debug.apk.
- Chưa commit/push. Kết quả kiểm tra trước ở phần Kiểm tra phía trên được giữ như lịch sử.
