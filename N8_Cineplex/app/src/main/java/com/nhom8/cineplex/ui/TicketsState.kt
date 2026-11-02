package com.nhom8.cineplex.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhom8.cineplex.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TicketsState(private val repo: TicketsBackend): ViewModel() {
    var issues by mutableStateOf<List<PaymentIssue>>(emptyList()); private set
    var tickets by mutableStateOf<List<ServerTicket>>(emptyList()); private set
    var orders by mutableStateOf<List<CustomerOrder>>(emptyList()); private set
    var selected by mutableStateOf<ServerTicket?>(null); private set
    var qr by mutableStateOf<String?>(null); private set
    var payment by mutableStateOf<ServerPayment?>(null); private set
    var paymentOrderId by mutableStateOf<String?>(null); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf(""); private set
    var paymentError by mutableStateOf(""); private set
    var paymentBusy by mutableStateOf(false); private set
    var history by mutableStateOf(false); private set
    private var accountId = ""
    fun bindUser(id: String?) { if (accountId != id.orEmpty()) { clear(); accountId = id.orEmpty() } }
    private var generation = 0
    private var action: Job? = null
    private var paymentAction: Job? = null
    private var expectedExpiry: String? = null
    private var expectedAmount: String? = null
    var refreshError by mutableStateOf(""); private set
    private var refreshing = false
    private var refreshFailures = 0
    val refreshDelayMillis get() = if(paymentOrderId == null && selected == null && refreshFailures == 0) 10_000L else (3_000L * (1L shl refreshFailures.coerceAtMost(4))).coerceAtMost(30_000L)
    suspend fun refreshStatus() {
        if(refreshing || busy || paymentBusy) return
        val version=generation;val paymentId=paymentOrderId;val ticketId=selected?.id
        refreshing=true
        try {
            if(paymentId!=null) {
                val item=repo.paymentStatus(paymentId);validatePayment(item,paymentId)
                check(expectedExpiry==null || item.expiresAt==expectedExpiry)
                check(expectedAmount==null || item.amount==expectedAmount)
                if(version==generation && paymentOrderId==paymentId) payment=item
            } else if(ticketId!=null) {
                val item=repo.ticket(ticketId);check(item.id==ticketId)
                if(version!=generation || selected?.id!=ticketId || paymentOrderId!=null) return
                val payload=if(item.status=="VALID") repo.qr(ticketId).also {check(it.isNotBlank() && !it.startsWith("CINEPLEX-DEMO:"))} else null
                if(version==generation && paymentOrderId==null && selected?.id==ticketId) {selected=item;qr=payload;tickets=tickets.map {if(it.id==item.id) item else it}}
            } else {
                val currentOrders=repo.orders();val currentTickets=repo.tickets()
                if(version==generation && paymentOrderId==null && selected==null) {orders=currentOrders;tickets=currentTickets}
            }
            if(version==generation) {refreshFailures=0;refreshError="";if(paymentId!=null) paymentError="" else error=""}
        } catch(e: CancellationException) {throw e}
          catch(e: Exception) {if(version==generation) {refreshFailures++;refreshError="Chưa cập nhật được trạng thái. Đang tự thử lại…";if(ticketId!=null) qr=null;if(e is IllegalStateException && paymentId!=null) payment=null}}
        finally {if(version==generation) refreshing=false}
    }

    val pendingOrders get() = orders.filter { it.status == "PENDING_PAYMENT" }
    val shownTickets get() = tickets.filter { if (history) it.status != "VALID" else it.status == "VALID" }
    fun loadIssues() {
        if (busy) return
        val version = generation; busy = true; error = ""
        action = viewModelScope.launch {
            try { val rows = repo.issues(); if (version == generation) issues = rows
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { if (version == generation) error = e.message ?: "Không tải được vấn đề thanh toán."
            } finally { if (version == generation) busy = false }
        }
    }
    fun resolveIssue(issue: PaymentIssue, bankRefunded: Boolean, note: String) {
        if (busy || issue.operationState == "UNKNOWN" || note.trim().length < 8) return
        if (issue.kind == "REFUND" && !bankRefunded) return
        val version = generation; busy = true; error = ""
        action = viewModelScope.launch {
            try {
                repo.resolveIssue(issue.id, if (bankRefunded) "MARK_BANK_REFUNDED" else "MARK_RESOLVED", note.trim())
                val rows = repo.issues(); if (version == generation) issues = rows
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { if (version == generation) error = e.message ?: "Chưa cập nhật được kết quả. Tải lại trước khi thử."
            } finally { if (version == generation) busy = false }
        }
    }
    fun showHistory(value: Boolean) { history = value }
    fun clear() {
        generation++; action?.cancel(); paymentAction?.cancel(); tickets = emptyList(); orders = emptyList(); issues = emptyList()
        selected = null; qr = null; payment = null; paymentOrderId = null; busy = false; paymentBusy = false
        error = ""; paymentError = ""; refreshError="";refreshFailures=0;refreshing=false; expectedExpiry = null; expectedAmount = null; history = false
    }
    fun load() {
        if (busy) return
        val version = generation; busy = true; error = ""
        action = viewModelScope.launch {
            try {
                val newOrders = repo.orders(); val newTickets = repo.tickets()
                if (version == generation) { orders = newOrders; tickets = newTickets }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { if (version == generation) error = e.message ?: "Không tải được vé. Vui lòng thử lại."
            } finally { if (version == generation) busy = false }
        }
    }
    fun openTicket(id: String) {
        action?.cancel(); refreshing=false; val version = ++generation; busy = true; error = ""; selected = null; qr = null
        action = viewModelScope.launch {
            try {
                val item = repo.ticket(id)
                check(item.id == id)
                if (version == generation) selected = item
                if (item.status == "VALID") {
                    val payload = repo.qr(id)
                    check(payload.isNotBlank() && !payload.startsWith("CINEPLEX-DEMO:"))
                    if (version == generation) qr = payload
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { if (version == generation) { qr = null; error = e.message ?: "Không tải được vé mới nhất." }
            } finally { if (version == generation) busy = false }
        }
    }
    fun closeTicket() { generation++; refreshing=false; action?.cancel(); selected = null; qr = null; busy = false; error = ""; load() }
    fun openPayment(id: String, expiry: String? = null, amount: String? = null) {
        if (paymentOrderId == id && (paymentBusy || payment != null)) return
        paymentAction?.cancel(); paymentOrderId = id; payment = null; expectedExpiry = expiry; expectedAmount = amount
        requestPayment(create = true)
    }
    fun closePayment() { paymentAction?.cancel(); paymentOrderId = null; payment = null; paymentBusy = false; paymentError = "" }
    fun checkPayment() { if (!paymentBusy && paymentOrderId != null) requestPayment(create = false) }
    private fun requestPayment(create: Boolean) {
        val id = paymentOrderId ?: return
        val version = ++generation; refreshing=false; paymentBusy = true; paymentError = ""
        paymentAction = viewModelScope.launch {
            try {
                val item = if (create) repo.payment(id) else repo.checkPayment(id)
                validatePayment(item, id)
                check(expectedExpiry == null || item.expiresAt == expectedExpiry) { "Máy chủ trả thời hạn không khớp đơn. Không sử dụng mã QR này." }
                check(expectedAmount == null || item.amount == expectedAmount) { "Số tiền thanh toán không khớp báo giá. Không sử dụng mã QR này." }
                if (version == generation && paymentOrderId == id) payment = item
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { if (version == generation && paymentOrderId == id) { payment = null; paymentError = e.message ?: "Chưa xác định kết quả. Kiểm tra lại cùng đơn, không tạo đơn mới." }
            } finally { if (version == generation && paymentOrderId == id) paymentBusy = false }
        }
    }
    fun refund() {
        val ticket = selected ?: return
        if (busy || ticket.status != "VALID" || !ticket.refundEligible) return
        val version = ++generation; refreshing=false; busy = true; error = ""; qr = null
        action = viewModelScope.launch {
            try {
                val result = repo.refund(ticket.id)
                check(result.amount.toLong() == ticket.snapshot.totalAmount.toLong())
                val current = repo.ticket(ticket.id)
                if (version == generation) selected = current
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { if (version == generation) error = e.message ?: "Chưa xác định kết quả hoàn. Tải lại vé để kiểm tra; không tạo lần hoàn mới."
            } finally { if (version == generation) busy = false }
        }
    }
}

fun ticketStatus(status: String) = when (status) {
    "VALID" -> "Sẵn sàng sử dụng"
    "USED" -> "Đã sử dụng"
    "REFUND_PENDING" -> "Vé đã hủy · tiền hoàn đang xử lý"
    "REFUNDED" -> "Đã hoàn tiền"
    "CANCELLED", "CANCELED" -> "Đã hủy"
    "EXPIRED" -> "Đã hết hạn"
    else -> "Đang kiểm tra trạng thái"
}
fun paymentStatus(payment: ServerPayment): String = when {
    payment.status == "PAID" && payment.ticketId != null -> "Thanh toán thành công"
    payment.status == "NEEDS_ADMIN" -> "Giao dịch cần được kiểm tra"
    payment.status == "EXPIRED" -> "Đơn đã hết hạn"
    payment.operationState == "UNKNOWN" -> "Đang xác minh thanh toán"
    else -> "Chờ thanh toán"
}
