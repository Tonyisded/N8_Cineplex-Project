package com.nhom8.cineplex

import android.graphics.Bitmap
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.ui.*
import com.nhom8.cineplex.ui.screens.*
import com.nhom8.cineplex.ui.theme.CineplexMockTheme
import org.junit.*
import org.junit.Assert.*
import java.io.File

class TicketsUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val deadline = "2030-01-01T00:00:00.000Z"
    private fun fixtureTicket(status: String = "VALID", refund: ServerRefund? = null) = ServerTicket("ticket", "order", status, "N8-DEMO-TEST-ONLY", "2026-11-02T00:00:00.000Z", deadline,
        TicketSnapshot(movieTitle = "Phim kiểm thử tuần 9", startAt = "2030-01-01T12:00:00.000Z", format = "2D", auditorium = "Phòng N8", seatCodes = listOf("A1", "A2"), seats = listOf(TicketSeat("A1", "50000"), TicketSeat("A2", "50001")), subtotal = "100001", discount = "50000", totalAmount = "50001", voucherCode = "FIRST50"),
        refundEligible = status == "VALID", accountMasked = "MB ••••1234", refund = refund)
    private open inner class Backend: TicketsBackend {
        override suspend fun orders() = emptyList<CustomerOrder>()
        override suspend fun tickets() = listOf(fixtureTicket())
        override suspend fun ticket(id: String) = fixtureTicket()
        override suspend fun qr(id: String) = "N8T:TEST-ONLY-NOT-A-REAL-TICKET"
        override suspend fun payment(id: String) = ServerPayment("p", id, "PENDING", "READY", "50001", deadline, qrCode = "TEST-ONLY-NOT-A-REAL-PAYMENT", description = "N8 TEST", accountName = "N8 TEST", accountNumber = "0000000000")
        override suspend fun checkPayment(id: String) = payment(id)
        override suspend fun refund(id: String) = ServerRefund("refund", "PENDING", "PROCESSING", "50001", "MB ••••1234")
    }
    @Test fun ticketDetailHasClearBackWithoutServerNote() {
        val state = TicketsState(object: Backend() { override suspend fun ticket(id: String) = fixtureTicket("REFUNDED") })
        compose.setContent { CineplexMockTheme { TicketsScreen(CineplexViewModel(), state) } }
        compose.runOnIdle { state.openTicket("ticket") }
        compose.waitUntil(5000) { state.selected != null }
        compose.onNodeWithText("QR chỉ có giá trị khi vé còn hợp lệ trên máy chủ.").assertDoesNotExist()
        compose.onNodeWithTag("native-ticket-qr").assertDoesNotExist()
        compose.onNodeWithContentDescription("Quay lại vé của tôi").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("ticket-detail-back-icon", useUnmergedTree = true).assertWidthIsEqualTo(28.dp).assertHeightIsEqualTo(28.dp)
        screenshot("ticket-detail-back-polished")
        compose.onNodeWithContentDescription("Quay lại vé của tôi").performClick()
        compose.waitUntil(5000) { state.selected == null }
    }
    @Test fun qrAmountIsFixedAndUnknownNeverClaimsSuccess() {
        var checked: String? = null
        val state = TicketsState(object: Backend() { override suspend fun checkPayment(id: String): ServerPayment { checked = id; return payment(id).copy(operationState = "UNKNOWN", qrCode = null) } })
        compose.setContent { CineplexMockTheme { PaymentScreen(CineplexViewModel(), state, 590) } }
        compose.runOnUiThread { state.openPayment("order", deadline, "50001") }
        compose.waitUntil(5000) { state.payment != null }
        compose.onNodeWithTag("payment-fixed-amount").assertTextEquals("50.001đ")
        compose.onNodeWithTag("native-ticket-qr").assertExists()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        screenshot("week9-payment-fixed-amount")
        compose.onNodeWithText("Kiểm tra thanh toán").performScrollTo().performClick()
        compose.waitUntil(5000) { state.payment?.operationState == "UNKNOWN" }
        compose.onNodeWithTag("native-ticket-qr").assertDoesNotExist()
        compose.onNodeWithText("Thanh toán thành công").assertDoesNotExist()
        assertEquals("order", checked)
    }
    @Test fun ticketDetailRefundKeepsHistoryAndRevokesQr() {
        var refunded = false
        val refund = ServerRefund("refund", "PENDING", "PROCESSING", "50001", "MB ••••1234")
        val state = TicketsState(object: Backend() {
            override suspend fun ticket(id: String) = fixtureTicket(if (refunded) "REFUND_PENDING" else "VALID", if (refunded) refund else null)
            override suspend fun refund(id: String): ServerRefund { refunded = true; return refund }
        })
        compose.setContent { CineplexMockTheme { TicketsScreen(CineplexViewModel(), state) } }
        compose.waitUntil(5000) { state.tickets.isNotEmpty() }
        compose.onNodeWithText("Xem chi tiết vé").performScrollTo().performClick()
        compose.waitUntil(5000) { state.qr != null }
        compose.onNodeWithTag("native-ticket-qr").assertExists()
        screenshot("week9-ticket-detail")
        compose.onNodeWithText("Yêu cầu hoàn vé").performScrollTo().performClick()
        compose.onNodeWithText("Hoàn toàn bộ đơn vé?").assertIsDisplayed()
        compose.onNodeWithText("Xác nhận hoàn vé").performClick()
        compose.waitUntil(5000) { state.selected?.status == "REFUND_PENDING" }
        compose.onNodeWithTag("native-ticket-qr").assertDoesNotExist()
        compose.onNodeWithText("Vé đã hủy · tiền hoàn đang xử lý").assertExists()
        assertEquals("N8-DEMO-TEST-ONLY", state.selected!!.bookingCode)
        compose.onNodeWithText("Tài khoản nhận:", substring = true).assertDoesNotExist()
        compose.onNodeWithText("MB ••••1234", substring = true).assertDoesNotExist()
        screenshot("week9-refund-pending")
    }
    @Test fun adminCannotResolveAnUnknownTransferOrSubmitEmptyReference() {
        var calls = 0
        val issue = PaymentIssue("issue", "NEEDS_ADMIN", "UNKNOWN", "50001", "REFUND")
        val state = TicketsState(object: Backend() {
            override suspend fun issues() = listOf(issue)
            override suspend fun resolveIssue(id: String, action: String, note: String) { calls++ }
        })
        compose.setContent { CineplexMockTheme { PaymentIssuesPanel(state) } }
        compose.waitUntil(5000) { state.issues.isNotEmpty() }
        compose.onNodeWithText("Xem và ghi nhận xử lý").performClick()
        compose.onNodeWithText("Xác nhận đã hoàn ngân hàng").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("BANK-REFERENCE-1234")
        compose.onNodeWithText("Xác nhận đã hoàn ngân hàng").assertIsNotEnabled()
        assertEquals(0, calls)
    }
    @Test fun checkoutPaymentQrThenTicketAndRefundIsOneContinuousFlow() {
        val seatsRepo = FakeSeatRepository()
        val seats = SeatSelectionViewModel(seatsRepo, { 0L })
        val vm = CineplexViewModel()
        var refunded = false
        var starts = 0
        var confirms = 0
        val refund = ServerRefund("refund", "PENDING", "PROCESSING", "100000", "MB ••••1234")
        fun currentTicket(): ServerTicket = fixtureTicket(if (refunded) "REFUND_PENDING" else "VALID", if (refunded) refund else null).copy(
            orderId = checkNotNull(seats.order).id,
            snapshot = fixtureTicket().snapshot.copy(seatCodes=listOf("A1"), seats=listOf(TicketSeat("A1","100000")),subtotal="100000",discount="0",totalAmount="100000",voucherCode=null))
        val transactions = TicketsState(object: Backend() {
            override suspend fun payment(id:String):ServerPayment {
                starts++; return ServerPayment("payment",id,"PENDING","PROCESSING","100000",checkNotNull(seats.order).expiresAt,qrCode="TEST-ONLY-NO-MONEY",accountNumber="0000000000")
            }
            override suspend fun checkPayment(id:String)=ServerPayment("payment",id,"PAID","SUCCEEDED","100000",checkNotNull(seats.order).expiresAt,ticketId="ticket")
            override suspend fun tickets()=listOf(currentTicket())
            override suspend fun ticket(id:String)=currentTicket()
            override suspend fun refund(id:String):ServerRefund { refunded=true;return refund }
        })
        val commerce = object: CommerceBackend {
            override suspend fun combos()=emptyList<ServerCombo>()
            override suspend fun vouchers()=emptyList<ServerVoucher>()
            override suspend fun quote(id:String,body:QuoteBody)=ServerQuote("100000","0","100000","100000","",checkNotNull(seats.order).expiresAt,"quote")
            override suspend fun checkout(id:String,token:String):HoldResponse { confirms++;return HoldResponse("2026-10-25T00:00:00Z",checkNotNull(seats.order)) }
        }
        val live=LiveCommerceState(commerce,transactions)
        compose.setContent { CineplexMockTheme {
            if(transactions.selected != null) TicketsScreen(vm,transactions) else LiveCommerceScreen(vm,seats,live)
        } }
        compose.runOnUiThread { seats.bind("owner","slot") }
        compose.waitUntil(5000){seats.seats.isNotEmpty()}
        compose.runOnUiThread {seats.select("A1");seats.continueToSummary{}}
        compose.waitUntil(5000){seats.held && !live.busy}
        compose.onNodeWithTag("commerce-next").performClick()
        compose.waitUntil(5000){live.quote != null}
        compose.onNodeWithTag("commerce-next").assertTextEquals("Thanh toán").performClick()
        compose.waitUntil(5000){transactions.payment != null}
        compose.onNodeWithTag("payment-fixed-amount").assertTextEquals("100.000đ")
        compose.onNodeWithTag("native-ticket-qr").assertExists()
        compose.onNodeWithText("Đã xác nhận đơn",substring=true).assertDoesNotExist()
        screenshot("checkout-to-real-payment-screen")
        compose.onNodeWithText("Kiểm tra thanh toán").performScrollTo().performClick()
        compose.waitUntil(5000){transactions.payment?.status=="PAID"}
        compose.onNodeWithText("Xem vé của tôi").performScrollTo().performClick()
        compose.waitUntil(5000){transactions.selected != null && transactions.qr != null}
        compose.onNodeWithText("Yêu cầu hoàn vé").performScrollTo().performClick()
        compose.onNodeWithText("Xác nhận hoàn vé").performClick()
        compose.waitUntil(5000){transactions.selected?.status=="REFUND_PENDING"}
        compose.onNodeWithTag("native-ticket-qr").assertDoesNotExist()
        compose.onNodeWithText("Vé đã hủy · tiền hoàn đang xử lý").assertExists()
        assertEquals(1,confirms);assertEquals(1,starts);assertEquals(1,seatsRepo.holds)
        screenshot("checkout-to-ticket-refund-complete-flow")
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "visual_checks").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
