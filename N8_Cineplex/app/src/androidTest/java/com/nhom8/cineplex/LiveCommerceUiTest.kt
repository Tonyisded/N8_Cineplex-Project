package com.nhom8.cineplex
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.ui.*
import com.nhom8.cineplex.ui.screens.LiveCommerceScreen
import com.nhom8.cineplex.ui.theme.CineplexMockTheme
import org.junit.*
import org.junit.Assert.*
class LiveCommerceUiTest {
 @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
 @Test fun serverPricingConfirmsPendingWithoutCancelingHold(){
  val seatsRepo=FakeSeatRepository();seatsRepo.items=seatsRepo.items.map{it.copy(price="240000")}
  val seats=SeatSelectionViewModel(seatsRepo,{0L});val vm=CineplexViewModel()
  var commits=0; var lastQuote:ServerQuote?=null
  val repo=object:CommerceBackend {
   override suspend fun combos()=listOf(ServerCombo("combo","Combo 2 người — Bơ","Bơ", "100000",status="AVAILABLE"), ServerCombo("popcorn","Bắp vừa — Bơ","Bắp", "49000",status="AVAILABLE"), ServerCombo("drink","Nước 500 ml","Nước", "35000",status="AVAILABLE"))
   override suspend fun vouchers()=listOf(ServerVoucher("CINE20","Voucher","", "300000", "cinema"), ServerVoucher("FIRST50","Chào bạn mới","", "0", "welcome"))
   override suspend fun quote(id:String,body:QuoteBody):ServerQuote {
    val subtotal=240000+100000*body.combo_quantity
    val discount=if(body.voucher_code=="CINE20"&&subtotal>=300000)20000 else 0
    return ServerQuote("$subtotal","$discount","${subtotal-discount}","${subtotal-discount}",if(discount>0)"CINE20" else "",seats.order!!.expiresAt,"server-token").also { lastQuote=it }
   }
   override suspend fun checkout(id:String,token:String):HoldResponse {assertEquals("server-token",token);commits++;val q=checkNotNull(lastQuote);return HoldResponse("2026-10-25T00:00:00.000Z",seats.order!!.copy(subtotal=q.subtotal,discount=q.discount,totalAmount=q.totalAmount,bankAmount=q.bankAmount))}
  }
  val state=LiveCommerceState(repo)
  compose.setContent{CineplexMockTheme{LiveCommerceScreen(vm,seats,state)}}
  compose.runOnUiThread{seats.bind("user","slot")}
  compose.waitUntil(5000){seats.seats.isNotEmpty()}
  compose.runOnUiThread{seats.select("A1");seats.continueToSummary{}}
  compose.waitUntil(5000){seats.held&&state.items.isNotEmpty()}
  val deadline=seats.deadlineMillis
  compose.onNodeWithTag("combo-image-combo").assertIsDisplayed()
  compose.onNodeWithText("Mã ưu đãi").assertDoesNotExist()
  compose.onNodeWithText("Lấy báo giá máy chủ").assertDoesNotExist()
  compose.onNodeWithTag("combo-plus-combo").performClick()
  compose.onNodeWithTag("combo-count-combo").assertTextEquals("1")
  compose.onNodeWithTag("commerce-total").assertTextEquals("340.000đ").assertIsDisplayed()
  compose.onNodeWithTag("combo-plus-combo").performClick()
  compose.onNodeWithTag("commerce-total").assertTextEquals("440.000đ")
  compose.onNodeWithTag("combo-minus-combo").performClick()
  compose.onNodeWithTag("combo-plus-popcorn").performScrollTo().performClick()
  compose.onNodeWithText("Đổi món đã chọn?").assertIsDisplayed()
  compose.onNodeWithText("Giữ món cũ").performClick()
  compose.onNodeWithTag("commerce-total").assertTextEquals("340.000đ")
  compose.onNodeWithTag("combo-plus-popcorn").performScrollTo().performClick()
  compose.onNodeWithText("Đổi món").performClick()
  compose.onNodeWithTag("commerce-total").assertTextEquals("289.000đ")
  compose.onNodeWithTag("combo-plus-combo").performScrollTo().performClick()
  compose.onNodeWithText("Đổi món").performClick()
  compose.onNodeWithTag("commerce-total").assertTextEquals("340.000đ")
  screenshot("combo-inline-quantity")
  compose.onNodeWithTag("commerce-next").performClick()
  compose.waitUntil(5000){state.quote!=null}
  compose.onNodeWithTag("commerce-total").assertTextEquals("340.000đ")
  compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
  compose.waitUntil(5000){state.step==LiveCommerceStep.COMBOS}
  assertEquals("combo",state.selected);assertEquals(deadline,seats.deadlineMillis)
  compose.onNodeWithTag("commerce-next").performClick()
  compose.waitUntil(5000){state.step==LiveCommerceStep.CHECKOUT&&state.quote!=null}
  assertEquals(0,commits)
  compose.onNodeWithText("−0đ").assertDoesNotExist()
  compose.onNodeWithTag("commerce-promo").performScrollTo().performClick()
  compose.onNodeWithText("Chọn CINE20").performClick()
  compose.waitUntil(5000){state.quote?.promotionCode=="CINE20"}
  compose.onNodeWithTag("commerce-total").assertTextEquals("320.000đ")
  screenshot("checkout-server-price")
  compose.onNodeWithTag("commerce-next").performClick()
  compose.waitUntil(5000){state.confirmed}
  compose.onNodeWithText("Đã xác nhận đơn — chờ thanh toán.",substring=true).assertExists()
  assertEquals(1,commits);assertTrue(seats.held);assertEquals(deadline,seats.deadlineMillis)
  compose.onNodeWithText("Mô phỏng thành công").assertDoesNotExist()
  compose.onNodeWithTag("commerce-next").assertIsNotEnabled()

 }

 @Test fun checkoutDisplaysServerPricesAndKeepsDeadline() = checkoutIntegrationCase(false)
 @Test fun lostResponseRetriesWithoutClaimingTicketOrExtendingDeadline() = checkoutIntegrationCase(true)

 @Test fun firstPurchaseVoucherHalvesServerBill() = checkoutIntegrationCase(false, "FIRST50")

 private fun checkoutIntegrationCase(loseFirstResponse:Boolean, code:String = "CINE20") {
  val clock=java.util.concurrent.atomic.AtomicLong(0)
  val seatRepo=FakeSeatRepository(clock=clock::get);seatRepo.items=seatRepo.items.map { it.copy(price="240000") }
  val seats=SeatSelectionViewModel(seatRepo,clock::get);val vm=CineplexViewModel()
  var calls=0;var lastQuote:ServerQuote?=null
  val tokens=mutableListOf<String>()
  val repo=object:CommerceBackend {
   override suspend fun combos()=listOf(ServerCombo("combo","Combo kiểm thử","Giá danh mục khác báo giá", "100000",status="AVAILABLE"))
   override suspend fun vouchers()=listOf(ServerVoucher("CINE20","Voucher","", "300000", "cinema"), ServerVoucher("FIRST50","Chào bạn mới","", "0", "welcome"))
   override suspend fun quote(id:String,body:QuoteBody):ServerQuote {
    val subtotal=240000L+110000L*body.combo_quantity
    val discount=if(body.voucher_code=="FIRST50")subtotal/2 else if(body.voucher_code=="CINE20")20000L else 0L
    return ServerQuote("$subtotal","$discount","${subtotal-discount}","${subtotal-discount}",if(discount>0)body.voucher_code else "",seats.order!!.expiresAt,"fixed-integration-token").also {lastQuote=it}
   }
   override suspend fun checkout(id:String,token:String):HoldResponse {
    assertEquals(seats.order!!.id,id);tokens+=token;calls++
    if(loseFirstResponse&&calls==1)throw ApiFailure("NETWORK_ERROR","lost response")
    val q=checkNotNull(lastQuote);val current=seatRepo.order(id)
    return current.copy(item=current.item.copy(subtotal=q.subtotal,discount=q.discount,totalAmount=q.totalAmount,bankAmount=q.bankAmount,comboId="combo",comboQuantity=1))
   }
  }
  val state=LiveCommerceState(repo)
  compose.setContent { CineplexMockTheme { LiveCommerceScreen(vm,seats,state) } }
  compose.runOnUiThread { seats.bind("user","slot") }
  compose.waitUntil(5000) { seats.seats.isNotEmpty() }
  compose.runOnUiThread { seats.select("A1");seats.continueToSummary {} }
  compose.waitUntil(5000) { seats.held&&state.items.isNotEmpty() }
  val deadline=seats.deadlineMillis
  compose.onNodeWithTag("combo-plus-combo").performClick()
  compose.onNodeWithTag("commerce-total").assertTextEquals("340.000đ")
  compose.runOnUiThread {clock.set(120000);seats.refreshClock()}
  compose.onNodeWithTag("commerce-next").performClick()
  compose.waitUntil(5000) {state.quote!=null}
  compose.onNodeWithTag("commerce-promo").performScrollTo().performClick()
  compose.onNodeWithText("Chọn $code").performClick()
  compose.waitUntil(5000) {state.quote?.promotionCode==code}
  compose.onNodeWithText("110.000đ").assertExists()
  compose.onNodeWithText("350.000đ").assertExists()
  compose.onAllNodesWithText(if(code=="FIRST50")"175.000đ" else "330.000đ").onFirst().assertExists()
  compose.onNodeWithText("100.000đ").assertDoesNotExist()
  compose.onNodeWithTag("commerce-total").assertTextEquals(if(code=="FIRST50")"175.000đ" else "330.000đ")
  assertEquals(deadline,seats.deadlineMillis);assertEquals(480,seats.secondsLeft)
  compose.runOnUiThread {compose.activity.onBackPressedDispatcher.onBackPressed()}
  compose.waitUntil(5000) {state.step==LiveCommerceStep.COMBOS}
  compose.onNodeWithTag("commerce-next").performClick()
  compose.waitUntil(5000) {state.quote!=null&&state.step==LiveCommerceStep.CHECKOUT}
  compose.onNodeWithTag("commerce-next").performClick()
  if(loseFirstResponse) {
   compose.waitUntil(5000) {state.unknown}
   compose.onNodeWithTag("commerce-next").assertTextContains("Kiểm tra lại đơn")
   compose.onNodeWithTag("commerce-next").performClick()
  }
  compose.waitUntil(5000) {state.confirmed&&!state.busy}
  compose.onNodeWithTag("commerce-confirmation").assertTextContains("chờ thanh toán", substring=true)
  compose.onNodeWithText("Thanh toán thành công",substring=true).assertDoesNotExist()
  compose.onNodeWithText("Mô phỏng thành công",substring=true).assertDoesNotExist()
  compose.onNodeWithTag("commerce-next").assertIsNotEnabled()
  assertEquals(List(if(loseFirstResponse)2 else 1){"fixed-integration-token"},tokens)
  assertEquals(deadline,seats.deadlineMillis);assertEquals(480,seats.secondsLeft);assertEquals(1,seatRepo.holds)
  screenshot(if(code=="FIRST50")"voucher-first50" else if(loseFirstResponse)"voucher-checkout-retry" else "voucher-checkout-server-total")
  compose.runOnUiThread {clock.set(600000);seats.refreshClock()}
  compose.waitUntil(5000) {!seats.held}
  compose.onNodeWithTag("commerce-next").assertIsNotEnabled()
  compose.onNodeWithTag("commerce-confirmation").assertDoesNotExist()
 }
 private fun screenshot(name:String) {
  compose.waitForIdle()
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val directory=File(context.getExternalFilesDir(null),"visual_checks").apply{mkdirs()}
  File(directory,"$name.png").outputStream().use {compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
 }
}
