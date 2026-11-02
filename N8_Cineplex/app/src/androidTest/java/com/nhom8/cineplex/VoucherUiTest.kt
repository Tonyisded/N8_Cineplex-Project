package com.nhom8.cineplex
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.nhom8.cineplex.data.ServerVoucher
import com.nhom8.cineplex.ui.screens.VoucherPicker
import com.nhom8.cineplex.ui.theme.CineplexMockTheme
import org.junit.*
import org.junit.Assert.*
class VoucherUiTest {
 @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
 @Test fun illustratedSelectionHasNoTextInputAndCanRemoveVoucher() {
  var selected=""
  val rows=listOf(ServerVoucher("FIRST50","Chào bạn mới · Giảm 50%","Giảm 50% tổng bill lần đầu", "0","welcome"),ServerVoucher("CINE20","Giảm 20.000đ","Từ 300.000đ", "300000","cinema"))
  compose.setContent { CineplexMockTheme { VoucherPicker(rows,"",100000,true){ selected=it } } }
  compose.onNodeWithTag("commerce-promo").performClick()
  compose.onNodeWithContentDescription("Minh hoạ Chào bạn mới · Giảm 50%").assertIsDisplayed()
  compose.onNodeWithText("Chưa đủ giá trị đơn").assertIsNotEnabled()
  val context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
  val file=java.io.File(context.getExternalFilesDir(null),"visual_checks/voucher-picker.png")
  file.parentFile!!.mkdirs()
  file.outputStream().use { androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
  compose.onNodeWithText("Chọn FIRST50").performClick();assertEquals("",selected);compose.onNodeWithText("Áp dụng").performClick();assertEquals("FIRST50",selected)
  compose.onNodeWithTag("commerce-promo").performClick();compose.onNodeWithText("Không dùng voucher").performClick();compose.onNodeWithText("Áp dụng").performClick();assertEquals("",selected)
 }
 @Test fun discoveryTabReplacesFilmAndAnnouncesPreparation() {
  val vm=com.nhom8.cineplex.ui.CineplexViewModel()
  compose.setContent { CineplexMockTheme { com.nhom8.cineplex.ui.components.CustomerNavigation(0,{}, {},vm::coming) } }
  compose.onNodeWithText("Phim").assertDoesNotExist()
  compose.onNodeWithText("Khám phá").performClick()
  assertEquals("Mục Khám phá đang được chuẩn bị. Hãy quay lại sau nhé.",vm.notice?.message)
 }
}
