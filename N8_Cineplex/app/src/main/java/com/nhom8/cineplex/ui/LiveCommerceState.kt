package com.nhom8.cineplex.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhom8.cineplex.data.*
import kotlinx.coroutines.*

enum class LiveCommerceStep { COMBOS, CHECKOUT }

class LiveCommerceState(private val repo: CommerceBackend) : ViewModel() {
    var items by mutableStateOf<List<ServerCombo>>(emptyList()); private set
    var balance by mutableStateOf(0L); private set
    var selected by mutableStateOf<String?>(null); private set
    var quantity by mutableStateOf(0); private set
    var promo by mutableStateOf(""); private set
    var quote by mutableStateOf<ServerQuote?>(null); private set
    var busy by mutableStateOf(false); private set
    var confirmed by mutableStateOf(false); private set
    var unknown by mutableStateOf(false); private set
    var message by mutableStateOf(""); private set
    var step by mutableStateOf(LiveCommerceStep.COMBOS); private set
    var seatSubtotal by mutableStateOf(0L); private set
    val chosen get() = items.firstOrNull { it.id == selected }
    val comboSubtotal get() = (chosen?.price?.toLong() ?: 0L) * quantity
    val estimatedSubtotal get() = seatSubtotal + comboSubtotal
    private var orderId = ""
    private var expiresAt = ""
    private var generation = 0
    private var job: Job? = null

    fun clear() {
        generation++; job?.cancel(); orderId = ""; expiresAt = ""; quote = null
        items = emptyList(); balance = 0; selected = null; quantity = 0
        promo = ""; confirmed = false; unknown = false; busy = false; message = ""
        step = LiveCommerceStep.COMBOS; seatSubtotal = 0
    }
    fun bind(order: HoldOrder?) {
        if (order == null || order.id == orderId) return
        clear(); orderId = order.id; expiresAt = order.expiresAt
        selected = order.comboId; quantity = order.comboQuantity
        // totalAmount may already contain a combo; seat snapshots do not.
        seatSubtotal = order.seats.sumOf { it.price.toLong() }
        load()
    }
    fun load() {
        if (busy) return
        val version = generation
        busy = true; message = ""
        job = viewModelScope.launch {
            try {
                val rows = repo.combos(); val b = repo.balance()
                if (version == generation) { items = rows; balance = b }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                if (version == generation) message = e.message ?: "Không tải được danh mục. Vui lòng thử lại."
            } finally { if (version == generation) busy = false }
        }
    }
    private fun changed() { quote = null; confirmed = false; message = "" }
    fun select(id: String?) {
        if (busy || unknown) return
        if (id != null && items.none { it.id == id && it.status == "AVAILABLE" }) return
        selected = id; quantity = if (id == null) 0 else 1; changed()
    }
    fun quantity(n: Int) {
        if (busy || unknown || selected == null) return
        if (n <= 0) { selected = null; quantity = 0 } else quantity = n.coerceAtMost(9)
        changed()
    }
    fun promotion(value: String) {
        if (busy || unknown) return
        promo = value; changed()
    }
    fun continueToCheckout() {
        if (busy || unknown || (selected != null && chosen?.status != "AVAILABLE")) return
        step = LiveCommerceStep.CHECKOUT
        confirmed = false
        price()
    }
    fun backToCombos(): Boolean {
        if (step != LiveCommerceStep.CHECKOUT || busy || unknown) return false
        step = LiveCommerceStep.COMBOS
        return true
    }
    fun price() {
        if (busy || unknown) return
        val version = generation
        val input = QuoteBody(selected, quantity, promo)
        busy = true; message = ""; quote = null
        job = viewModelScope.launch {
            try {
                val q = repo.quote(orderId, input)
                if (version == generation) {
                    check(q.expiresAt == expiresAt) { "Báo giá không khớp thời hạn giữ ghế. Vui lòng kiểm tra lại đơn." }
                    quote = q; balance = q.creditBalance.toLong()
                    if (promo.isNotBlank() && q.promotionCode.isBlank()) {
                        promo = ""; message = "CINE20 chưa đủ điều kiện; đơn chưa được giảm giá."
                    }
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                if (version == generation) message = e.message ?: "Không cập nhật được giá. Vui lòng thử lại."
            } finally { if (version == generation) busy = false }
        }
    }
    fun confirm(seats: SeatSelectionViewModel) {
        val q = quote ?: return
        if (busy || confirmed) return
        val version = generation
        busy = true; message = ""
        job = viewModelScope.launch {
            try {
                val result = repo.checkout(orderId, q.quoteToken)
                check(result.item.id == orderId && result.item.status == "PENDING_PAYMENT")
                if (version == generation) {
                    val order = result.item
                    check(order.expiresAt == expiresAt && order.subtotal == q.subtotal &&
                        order.discount == q.discount && order.totalAmount == q.totalAmount &&
                        order.creditUsed == q.creditUsed && order.bankAmount == q.bankAmount)
                    confirmed = true; unknown = false; seats.refresh(); wallet()
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                if (version == generation) {
                    if (e is ApiFailure && e.code !in setOf("NETWORK_ERROR", "HTTP_502", "HTTP_503", "HTTP_504")) {
                        unknown = false; quote = null; message = e.message ?: "Hãy cập nhật lại giá."
                    } else {
                        unknown = true; message = "Chưa xác định kết quả. Kiểm tra lại cùng đơn, không tạo đơn mới."
                    }
                    seats.refresh()
                }
            } finally { if (version == generation) busy = false }
        }
    }
    fun wallet() {
        val version = generation
        viewModelScope.launch {
            try {
                val b = repo.balance()
                if (version == generation) balance = b
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                if (version == generation) message = "Chưa tải được số dư. Giá cuối sẽ được kiểm tra lại khi xác nhận."
            }
        }
    }
}
