package com.nhom8.cineplex
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.runtime.*
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.ui.*
import com.nhom8.cineplex.ui.screens.*
import com.nhom8.cineplex.ui.theme.CineplexMockTheme
import org.junit.*
import org.junit.Assert.*
class CheckoutPolishUiTest {
 @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
 @Test fun voucherSelectionIsDraftUntilApplyAndBackDiscardsIt() {
  var selected=""; var calls=0
  val items=listOf(ServerVoucher("FIRST50","Chào bạn mới · Giảm 50%","Giảm 50% tổng vé và bắp nước cho lần mua đầu.","0","welcome"),ServerVoucher("CINE20","Hẹn hò màn ảnh · Giảm 20.000đ","Đơn từ 300.000đ, không cộng dồn.","300000","cinema"))
  compose.setContent { CineplexMockTheme { var current by remember { mutableStateOf("") }; VoucherPicker(items,current,100000,true){selected=it;current=it;calls++} } }
  compose.onNodeWithTag("commerce-promo").performClick()
  compose.onNodeWithText("Chọn FIRST50").performClick()
  assertEquals("",selected);assertEquals(0,calls)
  compose.onNodeWithText("Áp dụng").assertIsDisplayed().performClick()
  assertEquals("FIRST50",selected);assertEquals(1,calls)
  compose.onNodeWithTag("commerce-promo").performClick()
  compose.onNodeWithText("Không dùng voucher").performClick()
  compose.onNodeWithTag("voucher-close").performClick()
  assertEquals("FIRST50",selected);assertEquals(1,calls)
  compose.onNodeWithTag("commerce-promo").performClick()
  compose.onNodeWithText("Không dùng voucher").performClick()
  screenshot("voucher-polished.png")
  compose.onNodeWithText("Áp dụng").performClick();assertEquals("",selected)
 }
 @Test fun ticketTabsUseTheFullRowAndSwitchAccessibly() {
  val repo=object:TicketsBackend {
   override suspend fun orders()=emptyList<CustomerOrder>()
   override suspend fun tickets()=emptyList<ServerTicket>()
   override suspend fun ticket(id:String):ServerTicket=error("unused")
   override suspend fun qr(id:String):String=error("unused")
   override suspend fun payment(id:String):ServerPayment=error("unused")
   override suspend fun checkPayment(id:String):ServerPayment=error("unused")
   override suspend fun refund(id:String):ServerRefund=error("unused")
  }
  val state=TicketsState(repo)
  compose.setContent { CineplexMockTheme { TicketsScreen(CineplexViewModel(),state) } }
  compose.waitForIdle()
  val row=compose.onNodeWithTag("ticket-tabs").fetchSemanticsNode().boundsInRoot
  val upcoming=compose.onNodeWithTag("ticket-tab-upcoming").fetchSemanticsNode().boundsInRoot
  val history=compose.onNodeWithTag("ticket-tab-history").fetchSemanticsNode().boundsInRoot
  assertTrue(upcoming.width>row.width*.44f);assertTrue(history.width>row.width*.44f)
  assertEquals(upcoming.width,history.width,2f)
  compose.onNodeWithTag("ticket-tab-upcoming").assertIsSelected()
  compose.onNodeWithTag("ticket-tab-history").performClick().assertIsSelected()
  assertTrue(state.history)
  screenshot("ticket-tabs-polished.png")
 }
 private fun screenshot(name:String) {
  compose.waitForIdle()
  android.os.SystemClock.sleep(350)
  val i=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
  val file=java.io.File(i.targetContext.getExternalFilesDir(null),"visual_checks/$name");file.parentFile!!.mkdirs()
  file.outputStream().use { i.uiAutomation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
 }
}
