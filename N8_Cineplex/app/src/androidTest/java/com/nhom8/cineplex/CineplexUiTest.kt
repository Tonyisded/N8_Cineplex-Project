package com.nhom8.cineplex

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nhom8.cineplex.data.MockAccountRepository
import com.nhom8.cineplex.model.Screen
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.theme.CineplexMockTheme
import org.junit.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CineplexUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var vm: CineplexViewModel
    @Before fun setup() {
        vm = CineplexViewModel(MockAccountRepository())
        compose.setContent { CineplexMockTheme { CineplexApp(vm) } }
    }
    private fun field(label: String) = compose.onNode(hasContentDescription(label) and hasSetTextAction())
    private fun login(email: String, password: String) {
        field("Email").performScrollTo().performTextReplacement(email)
        field("Mật khẩu").performScrollTo().performTextReplacement(password)
        compose.onNodeWithTag("auth-submit").performScrollTo().performClick()
        compose.waitUntil(5_000) { !vm.loading }; compose.waitForIdle()
    }
    private fun back() { compose.waitForIdle(); compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }; compose.waitForIdle() }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null),"visual_checks").apply { mkdirs() }
        File(directory,"$name.png").outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
    @Test fun customerSearchBookingDetailsBackAndLogout() {
        screenshot("login")
        login("user@cineplex.test","123456")
        compose.onNodeWithText("Hôm nay, bạn muốn xem gì?").assertIsDisplayed()
        screenshot("home")
        compose.onNode(hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).performScrollToNode(hasText("Sắp chiếu"))
        compose.onAllNodes(hasText("CGV Landmark 81 ·", substring = true)).assertCountEquals(0)
        compose.onNodeWithText("Làm mới").assertDoesNotExist()
        compose.onAllNodes(hasText(" phim · ", substring = true)).assertCountEquals(0)
        compose.onNodeWithText("Sắp chiếu").performClick()
        compose.onNodeWithContentDescription("Tìm kiếm phim").performScrollTo().performTextReplacement("linh hon")
        compose.onNode(hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).performScrollToNode(hasContentDescription("Đặt vé Vùng đất linh hồn"))
        compose.onNodeWithContentDescription("Đặt vé Vùng đất linh hồn").performClick()
        compose.waitUntil(5_000) { vm.screen == Screen.DETAIL && !vm.detailLoading }
        compose.onNodeWithText("Chi tiết phim").assertIsDisplayed()
        screenshot("detail")
        compose.onNodeWithText("Đặt vé").performClick()
        compose.onNodeWithText("Sắp ra mắt").assertIsDisplayed()
        compose.onNodeWithText("Đã hiểu").performClick(); back()
        Assert.assertEquals("linh hon",vm.query); Assert.assertTrue(vm.soon)
        compose.onNode(hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).performScrollToNode(hasText("Sắp chiếu"))
        compose.onNodeWithText("Sắp chiếu").assertIsSelected()
        compose.onNodeWithText("Tài khoản").performClick()
        compose.onNodeWithText("Thông tin cá nhân").assertExists()
        compose.onNodeWithTag("profile-logout").performScrollTo().performClick()
        compose.onNodeWithText("Hẹn bạn ở rạp").assertExists()
        compose.waitForIdle()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed(); Assert.assertNull(vm.session); Assert.assertEquals(Screen.LOGIN,vm.screen) }
        compose.onNodeWithText("Hôm nay, bạn muốn xem gì?").assertDoesNotExist()
    }
    @Test fun staffRolesAndAdminMenus() {
        compose.onNodeWithText("Nhân viên đăng nhập?").performScrollTo().performClick()
        screenshot("staff")
        login("user@cineplex.test","123456")
        compose.onNodeWithText("Tài khoản không có quyền truy cập").assertExists()
        login("admin@cineplex.test","123456")
        compose.onNodeWithText("ADMIN · Cineplex Staff").assertIsDisplayed()
        screenshot("admin")
        listOf("Quản lý phim","Quản lý suất chiếu","Quản lý đặt vé").forEach {
            compose.onNodeWithText(it).performScrollTo().performClick()
            compose.onNodeWithText("Mục $it đang được chuẩn bị. Hãy quay lại sau nhé.").assertIsDisplayed()
            compose.onNodeWithText("Đã hiểu").performClick()
        }
        compose.onNodeWithText("Đăng xuất").performScrollTo().performClick()
        compose.onNodeWithText("Hẹn bạn ở rạp").assertExists(); Assert.assertNull(vm.session)
    }
    @Test fun registrationTermsPrefillAndRetainedAccount() {
        compose.onNodeWithText("Đăng ký").performClick()
        screenshot("signup")
        field("Họ và tên").performScrollTo().performTextInput("Nguyen An")
        field("Email").performScrollTo().performTextInput(" NEW@test.com ")
        field("Mật khẩu").performScrollTo().performTextInput("abcdefgh")
        field("Xác nhận mật khẩu").performScrollTo().performTextInput("abcdefgh")
        compose.onNodeWithContentDescription("Tôi đồng ý với Điều khoản sử dụng và Chính sách bảo mật").performScrollTo().performClick()
        compose.onNodeWithText("Điều khoản sử dụng").performScrollTo().performClick()
        compose.onNodeWithText("Điều khoản sử dụng — mẫu").assertIsDisplayed()
        compose.onNodeWithText("Đã hiểu").performClick()
        compose.onNodeWithTag("auth-submit").performScrollTo().performClick()
        compose.waitUntil(5_000) { !vm.loading }; compose.waitForIdle()
        field("Email").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText,AnnotatedString("new@test.com")))
        field("Mật khẩu").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText,AnnotatedString("")))
        compose.onNodeWithText("Đăng ký thành công! Vui lòng đăng nhập").assertExists()
        login("new@test.com","abcdefgh")
        compose.onNodeWithText("Tài khoản").performClick(); compose.onNodeWithTag("profile-logout").performScrollTo().performClick()
        login("new@test.com","abcdefgh")
        compose.onNodeWithText("Xin chào, Nguyen An").assertIsDisplayed()
    }
    @Test fun loginErrorsVisibilityAndForgotPassword() {
        compose.onNodeWithTag("auth-submit").performScrollTo().performClick()
        compose.onNodeWithText("Vui lòng nhập email.").assertExists()
        login("wrong","123456")
        compose.onNodeWithText("Email chưa đúng định dạng. Hãy nhập dạng ten@mien.com.").assertExists()
        compose.onNodeWithContentDescription("Hiện mật khẩu").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Ẩn mật khẩu").assertExists()
        login("user@cineplex.test","wrong")
        compose.onNodeWithText("Sai tài khoản hoặc mật khẩu. Vui lòng kiểm tra và thử lại.").assertExists()
        compose.onNodeWithText("Quên mật khẩu?").performScrollTo().performClick()
        compose.onNodeWithText("Quên mật khẩu").assertIsDisplayed()
        compose.onNodeWithText("Gửi email đặt lại mật khẩu").assertIsDisplayed()
        compose.onNodeWithText("Đóng").performClick()
    }
    @Test fun adminGuidanceAndAuthBack() {
        login("admin@cineplex.test","123456")
        compose.onNodeWithText("Chuyển sang đăng nhập nhân viên").performScrollTo().performClick()
        compose.onNodeWithText("Đăng nhập nhân viên").assertExists(); back()
        compose.onNodeWithText("Đăng ký").performClick()
        compose.onNodeWithText("Bắt đầu trải nghiệm Cineplex").assertExists(); back()
        compose.onNodeWithText("Hẹn bạn ở rạp").assertExists()
    }
    @Test fun googlePlaceholderOnlyOnCustomerLogin() {
        compose.onNodeWithTag("google-signin").performScrollTo()
        screenshot("login-google")
        compose.onNodeWithTag("google-signin").performClick()
        compose.onNodeWithText("Chức năng sẽ được bổ sung").assertIsDisplayed()
        Assert.assertNull(vm.session); Assert.assertEquals(Screen.LOGIN, vm.screen)
        compose.onNodeWithText("Đã hiểu").performClick()
        compose.onNodeWithText("Đăng ký").performScrollTo().performClick()
        compose.onNodeWithTag("google-signin").assertDoesNotExist()
        back()
        compose.onNodeWithText("Nhân viên đăng nhập?").performScrollTo().performClick()
        compose.onNodeWithTag("google-signin").assertDoesNotExist()
    }
    @Test fun customerProfileActionsReturnAndLogout() {
        login("user@cineplex.test", "123456")
        compose.onNodeWithContentDescription("Mở tài khoản").performScrollTo().performClick()
        compose.onNodeWithText("Thông tin cá nhân").assertIsDisplayed()
        compose.onNodeWithText("Tài khoản").assertIsSelected()
        screenshot("profile-customer")
        compose.onNodeWithText("user@cineplex.test").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Chỉnh sửa thông tin").performScrollTo().performClick()
        compose.onNodeWithText("Chức năng sẽ được bổ sung").assertIsDisplayed(); back()
        Assert.assertEquals(Screen.PROFILE, vm.screen)
        listOf("Đổi mật khẩu").forEach {
            compose.onNodeWithText(it).performScrollTo().performClick()
            compose.onNodeWithText("Chức năng sẽ được bổ sung").assertIsDisplayed()
            compose.onNodeWithText("Đã hiểu").performClick()
        }
        compose.onNodeWithText("Trợ giúp & hỗ trợ").performScrollTo().performClick()
        compose.onNodeWithText("Trợ giúp").assertIsDisplayed()
        compose.onNodeWithText("Đã hiểu").performClick()
        compose.onNodeWithText("Chính sách bảo mật").performScrollTo().performClick()
        compose.onNodeWithText("Android Keystore", substring = true).assertIsDisplayed(); back()
        compose.onNodeWithText("Trang chủ").performClick()
        compose.onNodeWithText("Hôm nay, bạn muốn xem gì?").assertIsDisplayed()
        compose.runOnUiThread { vm.query = "Dune" }
        compose.onNode(hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).performScrollToNode(hasContentDescription("Đặt vé Dune: Phần Hai"))
        val grid = compose.onNode(hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
        val position = grid.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        compose.onNodeWithText("Tài khoản").performClick(); back()
        Assert.assertEquals("Dune", vm.query)
        Assert.assertEquals(position, grid.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value(), 0.01f)
        compose.onNodeWithText("Tài khoản").performClick()
        compose.onNodeWithTag("profile-logout").performScrollTo()
        screenshot("profile-actions")
        compose.onNodeWithTag("profile-logout").performClick()
        compose.runOnUiThread { vm.back(); vm.account() }
        compose.onNodeWithText("Hẹn bạn ở rạp").assertExists()
        Assert.assertNull(vm.session); Assert.assertEquals(Screen.LOGIN, vm.screen)
    }
    @Test fun adminProfileHasNoCustomerNavigationAndReturnsToDashboard() {
        compose.onNodeWithText("Nhân viên đăng nhập?").performScrollTo().performClick()
        login("admin@cineplex.test", "123456")
        compose.onNodeWithContentDescription("Mở tài khoản quản trị").performScrollTo().performClick()
        compose.onNodeWithText("Thông tin cá nhân").assertIsDisplayed()
        compose.onNodeWithText("Tài khoản").assertDoesNotExist()
        screenshot("profile-admin")
        compose.onNodeWithText("admin@cineplex.test").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Quay lại dashboard quản trị").performScrollTo().performClick()
        compose.onNodeWithText("ADMIN · Cineplex Staff").assertIsDisplayed()
        compose.onNodeWithContentDescription("Mở tài khoản quản trị").performScrollTo().performClick()
        compose.onNodeWithTag("profile-logout").performScrollTo().performClick()
        compose.runOnUiThread { vm.back(); vm.account() }
        Assert.assertNull(vm.session); Assert.assertEquals(Screen.LOGIN, vm.screen)
    }
    @Test fun signupInlineValidationAndIndependentToggles() {
        compose.onNodeWithText("Đăng ký").performClick()
        compose.onNodeWithTag("auth-submit").performScrollTo().performClick()
        compose.onNodeWithText("Vui lòng nhập họ và tên.").assertExists()
        field("Họ và tên").performScrollTo().performTextInput("An")
        field("Email").performScrollTo().performTextInput("USER@cineplex.test")
        field("Mật khẩu").performScrollTo().performTextInput("1234567")
        field("Xác nhận mật khẩu").performScrollTo().performTextInput("mismatch")
        compose.onNodeWithTag("auth-submit").performScrollTo().performClick()
        compose.onNodeWithText("Email đã được đăng ký. Hãy đăng nhập hoặc dùng email khác.").assertExists()
        compose.onNodeWithText("Mật khẩu phải có ít nhất 8 ký tự.").assertExists()
        compose.onNodeWithText("Xác nhận mật khẩu không khớp. Vui lòng nhập lại.").assertExists()
        compose.onNodeWithText("Vui lòng đồng ý với điều khoản và chính sách bảo mật.").assertExists()
        compose.onNodeWithContentDescription("Hiện mật khẩu").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Hiện xác nhận mật khẩu").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Ẩn mật khẩu").assertExists()
        compose.onNodeWithContentDescription("Ẩn xác nhận mật khẩu").assertExists()
    }
}
