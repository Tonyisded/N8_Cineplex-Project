package com.nhom8.cineplex

import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class TicketsStateTest {
    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun cleanup() { Dispatchers.resetMain() }
    private val deadline = "2030-01-01T00:00:00.000Z"
    private fun payment() = ServerPayment("payment", "order", "PENDING", "READY", "50001", deadline,
        qrCode = "fixed-server-qr", checkoutUrl = "https://pay.payos.vn/web/link")
    private fun ticket(status: String = "VALID") = ServerTicket("ticket", "order", status, "N8TEST", "2026-11-02T00:00:00Z", deadline,
        TicketSnapshot(movieTitle = "Phim", totalAmount = "50001"), refundEligible = status == "VALID")
    private open inner class Backend: TicketsBackend {
        override suspend fun orders() = emptyList<CustomerOrder>()
        override suspend fun tickets() = listOf(ticket())
        override suspend fun ticket(id: String) = ticket()
        override suspend fun qr(id: String) = "N8T:signed-token"
        override suspend fun payment(id: String) = payment()
        override suspend fun checkPayment(id: String) = payment()
        override suspend fun refund(id: String) = ServerRefund("refund", "PENDING", "PROCESSING", "50001")
    }
    @Test fun unknownCheckRetainsOriginalOrderAndNeverCreatesAnotherPayment() = runTest {
        val calls = mutableListOf<String>()
        val repo = object: Backend() {
            override suspend fun payment(id: String): ServerPayment { calls += "create:$id"; throw ApiFailure("NETWORK_ERROR", "timeout") }
            override suspend fun checkPayment(id: String): ServerPayment { calls += "check:$id"; return payment() }
        }
        val state = TicketsState(repo); state.openPayment("order", deadline, "50001"); advanceUntilIdle()
        assertNull(state.payment); assertTrue(state.paymentError.isNotBlank())
        state.checkPayment(); advanceUntilIdle()
        assertEquals(listOf("create:order", "check:order"), calls)
        assertEquals(deadline, state.payment!!.expiresAt); assertEquals("50001", state.payment!!.amount)
    }
    @Test fun mismatchedMoneyOrDeadlineNeverExposesQr() = runTest {
        for (bad in listOf(payment().copy(amount = "1"), payment().copy(expiresAt = "2030-01-01T00:00:01.000Z"), payment().copy(orderId = "other"))) {
            val state = TicketsState(object: Backend() { override suspend fun payment(id: String) = bad })
            state.openPayment("order", deadline, "50001"); advanceUntilIdle()
            assertNull(state.payment); assertTrue(state.paymentError.isNotBlank())
        }
    }
    @Test fun invalidOrDemoQrCannotBeDisplayedAndRefundHidesOriginalQr() = runTest {
        val state = TicketsState(object: Backend() { override suspend fun qr(id: String) = "CINEPLEX-DEMO:order" })
        state.openTicket("ticket"); advanceUntilIdle()
        assertNull(state.qr); assertTrue(state.error.isNotBlank())
        val refunded = TicketsState(object: Backend() { override suspend fun ticket(id: String): ServerTicket = this@TicketsStateTest.ticket("REFUND_PENDING") })
        refunded.openTicket("ticket"); advanceUntilIdle(); assertNull(refunded.qr)
        assertEquals("Vé đã hủy · tiền hoàn đang xử lý", ticketStatus(refunded.selected!!.status))
    }
    @Test fun sessionClearDiscardsHistorySelectionAndPayment() = runTest {
        val state = TicketsState(Backend()); state.load(); advanceUntilIdle(); assertEquals(1, state.tickets.size)
        state.openPayment("order", deadline, "50001"); advanceUntilIdle(); state.clear()
        assertTrue(state.tickets.isEmpty()); assertNull(state.selected); assertNull(state.payment); assertNull(state.paymentOrderId)
    }
    @Test fun adminResolutionRequiresNoteAndNeverResolvesUnknownOrDiscardsRefundDebt() = runTest {
        val calls = mutableListOf<String>()
        val state = TicketsState(object: Backend() {
            override suspend fun issues() = emptyList<PaymentIssue>()
            override suspend fun resolveIssue(id: String, action: String, note: String) { calls += action }
        })
        val issue = PaymentIssue("issue", "NEEDS_ADMIN", "NEEDS_REVIEW", "50001", "REFUND")
        state.resolveIssue(issue, true, "short"); advanceUntilIdle()
        state.resolveIssue(issue.copy(operationState = "UNKNOWN"), true, "reference-1234"); advanceUntilIdle()
        state.resolveIssue(issue, false, "reference-1234"); advanceUntilIdle()
        assertTrue(calls.isEmpty())
        state.resolveIssue(issue, true, "reference-1234"); advanceUntilIdle()
        assertEquals(listOf("MARK_BANK_REFUNDED"), calls)
    }
    @Test fun checkoutLinkMustBeAnHttpsPayosLink() {
        for (url in listOf("http://pay.payos.vn/web/a", "https://pay.payos.vn.attacker.invalid/web/a", "https://user:password@pay.payos.vn/web/a")) {
            assertThrows(IllegalStateException::class.java) { validatePayment(payment().copy(checkoutUrl = url), "order") }
        }
    }
}
