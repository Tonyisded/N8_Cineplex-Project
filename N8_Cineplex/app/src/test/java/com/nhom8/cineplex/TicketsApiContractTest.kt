package com.nhom8.cineplex

import com.nhom8.cineplex.data.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.Assert.*

class TicketsApiContractTest {
    private val json = Json { ignoreUnknownKeys = true }
    @Test fun orderTicketSummaryIsNotAFullTicket() {
        val response = json.decodeFromString<OrderListResponse>("""{
          "serverTime":"2026-11-02T13:27:00Z","items":[
            {"id":"pending","showtimeId":"slot","status":"PENDING_PAYMENT","totalAmount":"150000","expiresAt":"2026-11-02T13:30:00Z","ticket":null},
            {"id":"paid","showtimeId":"slot","status":"PAID","totalAmount":"150000","expiresAt":"2026-11-02T13:30:00Z","ticket":{"id":"ticket1","status":"VALID"}},
            {"id":"refund","showtimeId":"slot","status":"REFUND_PENDING","totalAmount":"150000","expiresAt":"2026-11-02T13:30:00Z","ticket":{"id":"ticket2","status":"REFUND_PENDING"}}
          ]}""")
        assertNull(response.items[0].ticket)
        assertEquals("ticket1", response.items[1].ticket!!.id)
        assertEquals("REFUND_PENDING", response.items[2].ticket!!.status)
    }
    @Test fun expiredUnpaidOrdersDisappearAtDeadlineWithoutHidingRefundDebt() {
        val order = CustomerOrder("order", "slot", "PENDING_PAYMENT", "150000", "2026-11-02T13:30:00.000Z")
        val deadline = isoMillis(order.expiresAt)
        assertTrue(order.awaitingPaymentAt(deadline - 1))
        assertFalse(order.awaitingPaymentAt(deadline))
        assertFalse(order.copy(expiresAt = "bad").awaitingPaymentAt(deadline))
        assertFalse(order.copy(status = "REFUND_PENDING").awaitingPaymentAt(deadline - 1))
    }
    @Test fun ticketDetailStillRequiresFinancialAndQrFields() {
        assertThrows(kotlinx.serialization.SerializationException::class.java) {
            json.decodeFromString<ServerTicket>("""{"id":"ticket1","status":"VALID"}""")
        }
    }
}
