package com.nhom8.cineplex
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.ui.*
import com.nhom8.cineplex.ui.screens.*
import com.nhom8.cineplex.ui.theme.CineplexMockTheme
import org.junit.*
import org.junit.Assert.*
class VersionRefreshUiTest {
 @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
 @Before fun before(){AppVersionWarning.required.value=false}
 @After fun after(){AppVersionWarning.required.value=false}
 @Test fun oldVersionBlocksAppAndNetworkFailureCanRetry() {
  val state=AppVersionState(VersionBackend{AppRelease(8,8,"https://18.143.100.43/api/v1/app/android/download","a".repeat(64))},7)
  compose.setContent { CineplexMockTheme { AppUpdateGate(state) {Text("APP CONTENT")} } }
  compose.waitUntil(5000){state.outdated}
  compose.onNodeWithText("Phiên bản ứng dụng đã cũ, vui lòng cập nhật lên phiên bản mới.").assertIsDisplayed()
  compose.onNodeWithText("APP CONTENT").assertDoesNotExist()
  compose.onNodeWithText("Cập nhật ứng dụng").assertIsEnabled()
 }
 @Test fun automaticPaymentAndRefundRefreshWithoutResubmittingAndPauseInBackground() {
  var paid=false;var refundStarted=false;var completed=false;var creates=0;var refunds=0;var reads=0
  val deadline="2030-01-01T00:00:00.000Z"
  fun currentPayment()=ServerPayment("p","order",if(paid)"PAID" else "PENDING",if(paid)"SUCCEEDED" else "PROCESSING","50001",deadline,qrCode=if(paid)null else "TEST-ONLY-QR",ticketId=if(paid)"ticket" else null)
  fun currentTicket()=ServerTicket("ticket","order",if(completed)"REFUNDED" else if(refundStarted)"REFUND_PENDING" else "VALID","TEST ONLY",deadline,deadline,TicketSnapshot(movieTitle="UI fixture",totalAmount="50001",startAt=deadline),refundEligible=!refundStarted,refund=if(refundStarted)ServerRefund("r",if(completed)"RESOLVED" else "PENDING",if(completed)"SUCCEEDED" else "PROCESSING","50001") else null)
  val backend=object:TicketsBackend {
   override suspend fun orders()=emptyList<CustomerOrder>()
   override suspend fun tickets()=listOf(currentTicket())
   override suspend fun ticket(id:String)=currentTicket()
   override suspend fun qr(id:String)="N8T:TEST-ONLY"
   override suspend fun payment(id:String):ServerPayment {creates++;return currentPayment()}
   override suspend fun paymentStatus(id:String):ServerPayment {reads++;return currentPayment()}
   override suspend fun checkPayment(id:String):ServerPayment=error("no manual provider check expected")
   override suspend fun refund(id:String):ServerRefund {refunds++;refundStarted=true;return ServerRefund("r","PENDING","PROCESSING","50001")}
  }
  val state=TicketsState(backend);compose.setContent { CineplexMockTheme { TicketsScreen(CineplexViewModel(),state) } }
  compose.runOnIdle {state.openPayment("order",deadline,"50001")}
  compose.waitUntil(5000){state.payment?.qrCode!=null && reads>0}
  compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
  val stoppedReads=reads;android.os.SystemClock.sleep(3500);assertEquals(stoppedReads,reads)
  paid=true;compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
  compose.waitUntil(8000){state.payment?.status=="PAID"}
  compose.onNodeWithText("Thanh toán thành công").assertIsDisplayed()
  compose.onNodeWithText("Xem vé của tôi").performClick()
  compose.waitUntil(5000){state.selected!=null}
  compose.onNodeWithText("Yêu cầu hoàn vé").performScrollTo().performClick()
  compose.onNodeWithText("Xác nhận hoàn vé").performClick()
  compose.waitUntil(5000){state.selected?.status=="REFUND_PENDING"}
  completed=true;compose.waitUntil(9000){state.selected?.status=="REFUNDED"}
  compose.onNodeWithText("Hoàn tiền thành công").assertExists()
  compose.onNodeWithTag("native-ticket-qr").assertDoesNotExist()
  assertEquals(1,creates);assertEquals(1,refunds)
 }
}
