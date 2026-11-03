package com.nhom8.cineplex.data

import com.nhom8.cineplex.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

@Serializable data class TicketSeat(val seatCode: String, val price: String)
@Serializable data class TicketSnapshot(
    val movieTitle: String = "", val posterUrl: String? = null, val startAt: String = "",
    val format: String = "", val auditorium: String = "", val seatCodes: List<String> = emptyList(),
    val seats: List<TicketSeat> = emptyList(), val comboItems: List<ServerComboLine> = emptyList(), val comboName: String? = null, val comboQuantity: Int = 0,
    val subtotal: String = "0", val discount: String = "0", val totalAmount: String = "0",
    val voucherCode: String? = null
)
@Serializable data class ServerRefund(val id: String, val status: String, val operationState: String = "",
    val amount: String, val accountMasked: String? = null, val reason: String? = null, val paidAt: String? = null)
@Serializable data class ServerTicket(val id: String, val orderId: String, val status: String,
    val bookingCode: String, val issuedAt: String, val expiresAt: String, val snapshot: TicketSnapshot,
    val refundEligible: Boolean = false, val refundReason: String? = null, val accountMasked: String? = null,
    val refund: ServerRefund? = null)
@Serializable data class ServerPayment(val id: String, val orderId: String, val status: String,
    val operationState: String = "", val amount: String, val expiresAt: String,
    val qrCode: String? = null, val checkoutUrl: String? = null, val description: String? = null,
    val accountName: String? = null, val accountNumber: String? = null, val bin: String? = null,
    val paymentLinkId: String? = null, val ticketId: String? = null)
@Serializable data class OrderTicketSummary(val id: String, val status: String)
@Serializable data class CustomerOrder(val id: String, val showtimeId: String, val status: String,
    val totalAmount: String, val expiresAt: String, val seats: List<OrderSeatDto> = emptyList(),
    val payment: ServerPayment? = null, val ticket: OrderTicketSummary? = null)
@Serializable data class TicketListResponse(val serverTime: String, val items: List<ServerTicket>, val nextCursor: String? = null)
@Serializable data class TicketResponse(val serverTime: String, val item: ServerTicket)
@Serializable data class OrderListResponse(val serverTime: String, val items: List<CustomerOrder>)
@Serializable data class PaymentResponse(val serverTime: String, val item: ServerPayment)
@Serializable data class RefundResponse(val item: ServerRefund)
@Serializable data class TicketQr(val payload: String)
@Serializable data class TicketQrResponse(val item: TicketQr)

@Serializable data class PaymentIssue(val id: String, val status: String, val operationState: String, val amount: String,
    val kind: String, val orderId: String? = null, val accountMasked: String? = null, val reason: String? = null,
    val providerTransactionId: String? = null, val providerObjectId: String? = null)
@Serializable data class PaymentIssuesResponse(val items: List<PaymentIssue>)
@Serializable data class ResolveIssueBody(val action: String, val note: String)
interface TicketsBackend {
    suspend fun issues(): List<PaymentIssue> = throw ApiFailure("UNAVAILABLE", "Không có kết nối quản trị.")
    suspend fun resolveIssue(id: String, action: String, note: String) { throw ApiFailure("UNAVAILABLE", "Không có kết nối quản trị.") }
    suspend fun orders(): List<CustomerOrder>
    suspend fun tickets(): List<ServerTicket>
    suspend fun ticket(id: String): ServerTicket
    suspend fun qr(id: String): String
    suspend fun payment(id: String): ServerPayment
    suspend fun paymentStatus(id: String): ServerPayment = throw ApiFailure("UNAVAILABLE", "Chưa đọc được trạng thái thanh toán.")
    suspend fun checkPayment(id: String): ServerPayment
    suspend fun refund(id: String): ServerRefund
}
private interface TicketsApi {
    @GET("admin/payment-issues") suspend fun issues(@Header("Authorization") auth: String): PaymentIssuesResponse
    @POST("admin/payment-issues/{id}/resolve") suspend fun resolveIssue(@Header("Authorization") auth: String, @Path("id") id: String, @Body body: ResolveIssueBody): Unit
    @GET("orders") suspend fun orders(@Header("Authorization") auth: String): OrderListResponse
    @GET("tickets") suspend fun tickets(@Header("Authorization") auth: String, @Query("cursor") cursor: String? = null): TicketListResponse
    @GET("tickets/{id}") suspend fun ticket(@Header("Authorization") auth: String, @Path("id") id: String): TicketResponse
    @GET("tickets/{id}/qr") suspend fun qr(@Header("Authorization") auth: String, @Path("id") id: String): TicketQrResponse
    @POST("orders/{id}/payment") suspend fun payment(@Header("Authorization") auth: String, @Path("id") id: String): PaymentResponse
    @GET("orders/{id}/payment") suspend fun paymentStatus(@Header("Authorization") auth: String, @Path("id") id: String): PaymentResponse
    @POST("orders/{id}/payment/check") suspend fun checkPayment(@Header("Authorization") auth: String, @Path("id") id: String): PaymentResponse
    @POST("tickets/{id}/refund") suspend fun refund(@Header("Authorization") auth: String, @Path("id") id: String): RefundResponse
}
class ApiTicketsRepository(private val account: ApiAccountRepository): TicketsBackend {
    private val api = Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL)
        .client(cineplexHttpClient())
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
        .build().create(TicketsApi::class.java)
    override suspend fun issues() = account.withAuthorization { api.issues(it).items }
    override suspend fun resolveIssue(id: String, action: String, note: String) { account.withAuthorization { api.resolveIssue(it, id, ResolveIssueBody(action, note)) } }
    override suspend fun orders() = account.withAuthorization { api.orders(it).items }
    override suspend fun tickets(): List<ServerTicket> = account.withAuthorization { auth ->
        val items = mutableListOf<ServerTicket>()
        val cursors = mutableSetOf<String>()
        var cursor: String? = null
        do {
            val page = api.tickets(auth, cursor)
            items += page.items
            cursor = page.nextCursor
            check(cursor == null || cursors.add(cursor)) { "Phân trang vé không hợp lệ. Vui lòng tải lại." }
        } while (cursor != null)
        items.distinctBy { it.id }
    }
    override suspend fun ticket(id: String) = account.withAuthorization { api.ticket(it, id).item.also { t -> check(t.id == id) } }
    override suspend fun qr(id: String) = account.withAuthorization { api.qr(it, id).item.payload.also { p -> check(p.isNotBlank() && !p.startsWith("CINEPLEX-DEMO:")) } }
    override suspend fun payment(id: String) = account.withAuthorization { api.payment(it, id).item.also { p -> validatePayment(p, id) } }
    override suspend fun paymentStatus(id: String) = account.withAuthorization { api.paymentStatus(it, id).item.also { p -> validatePayment(p, id) } }
    override suspend fun checkPayment(id: String) = account.withAuthorization { api.checkPayment(it, id).item.also { p -> validatePayment(p, id) } }
    override suspend fun refund(id: String) = account.withAuthorization { api.refund(it, id).item.also { r -> check(r.amount.toLong() >= 0) } }
}
fun validatePayment(payment: ServerPayment, orderId: String) {
    check(payment.orderId == orderId && payment.amount.toLong() >= 0)
    isoMillis(payment.expiresAt)
    payment.checkoutUrl?.let { check(java.net.URI(it).let { u -> u.scheme == "https" && u.userInfo == null && u.host == "pay.payos.vn" }) }
}

fun CustomerOrder.awaitingPaymentAt(nowMillis: Long) = status == "PENDING_PAYMENT" &&
    runCatching { isoMillis(expiresAt) > nowMillis }.getOrDefault(false)
