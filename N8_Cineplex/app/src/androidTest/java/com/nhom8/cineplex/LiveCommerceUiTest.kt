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
  var commits=0
  val repo=object:CommerceBackend {
   override suspend fun combos()=listOf(ServerCombo("combo","Combo 2 người — Bơ","Bơ", "100000",status="AVAILABLE"), ServerCombo("popcorn","Bắp vừa — Bơ","Bắp", "49000",status="AVAILABLE"), ServerCombo("drink","Nước 500 ml","Nước", "35000",status="AVAILABLE"))
   override suspend fun balance()=if(commits==0)150000L else 0L
   override suspend fun quote(id:String,body:QuoteBody):ServerQuote {
    val subtotal=240000+100000*body.combo_quantity
    val discount=if(body.promotion_code=="CINE20"&&subtotal>=300000)20000 else 0
    return ServerQuote("$subtotal","$discount","150000","${subtotal-discount}","${subtotal-discount-150000}","150000",if(discount>0)"CINE20" else "",seats.order!!.expiresAt,"server-token")
   }
   override suspend fun checkout(id:String,token:String):HoldResponse {assertEquals("server-token",token);commits++;return HoldResponse("2026-10-25T00:00:00.000Z",seats.order!!)}
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
  compose.onNodeWithTag("commerce-total").assertTextEquals("190.000đ")
  compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
  compose.waitUntil(5000){state.step==LiveCommerceStep.COMBOS}
  assertEquals("combo",state.selected);assertEquals(deadline,seats.deadlineMillis)
  compose.onNodeWithTag("commerce-next").performClick()
  compose.waitUntil(5000){state.step==LiveCommerceStep.CHECKOUT&&state.quote!=null}
  assertEquals(0,commits)
  compose.onNodeWithText("−0đ").assertDoesNotExist()
  compose.onNodeWithTag("commerce-promo").performScrollTo().performTextInput("CINE20")
  compose.onNodeWithText("Áp dụng").performClick()
  compose.waitUntil(5000){state.quote?.promotionCode=="CINE20"}
  compose.onNodeWithTag("commerce-total").assertTextEquals("170.000đ")
  screenshot("checkout-server-price")
  compose.onNodeWithTag("commerce-next").performClick()
  compose.waitUntil(5000){state.confirmed}
  compose.onNodeWithText("Đã xác nhận đơn — chờ thanh toán.",substring=true).assertExists()
  assertEquals(1,commits);assertTrue(seats.held);assertEquals(deadline,seats.deadlineMillis)
  compose.onNodeWithText("Mô phỏng thành công").assertDoesNotExist()
  compose.onNodeWithTag("commerce-next").assertIsNotEnabled()

 }
 private fun screenshot(name:String) {
  compose.waitForIdle()
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val directory=File(context.getExternalFilesDir(null),"visual_checks").apply{mkdirs()}
  File(directory,"$name.png").outputStream().use {compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
 }
}
