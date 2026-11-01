package com.nhom8.cineplex.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import java.util.UUID

/** VND amounts are integers. This is a local demonstration until commerce APIs exist. */
data class ComboChoice(val id: String, val name: String, val description: String, val price: Long, val image: Int, val hasOptions: Boolean = false, val soldOut: Boolean = false)
data class ComboSelection(val product: ComboChoice, val quantity: Int, val variant: String = "", val surcharge: Long = 0)
data class PriceQuote(val seats: Long, val combo: Long, val fee: Long, val discount: Long) {
    val subtotal get() = seats + combo + fee
    val afterDiscount get() = (subtotal - discount).coerceAtLeast(0)
    val due get() = afterDiscount
}
fun priceQuote(seats: Long, combo: Long, promo: String): PriceQuote {
    require(seats >= 0 && combo >= 0)
    val subtotal = seats + combo
    val discount = if (promo == "FIRST50") subtotal / 2 else if (promo == "CINE20" && subtotal >= 300_000) 20_000L else 0L
    return PriceQuote(seats, combo, 0, discount)
}

enum class CommercePage { COMBOS, CHECKOUT, PROCESSING, SUCCESS, FAILED, UNKNOWN, CANCELLED, TICKET }
data class BookingSnapshot(val orderId: String, val attemptId: String, val movie: String, val showtime: String, val auditorium: String,
    val seats: List<String>, val products: List<ComboSelection>, val quote: PriceQuote, val name: String, val email: String, val phone: String, val method: String)

interface CommerceRepository { fun complete(snapshot: BookingSnapshot) }
class DemoCommerceRepository : CommerceRepository { override fun complete(snapshot: BookingSnapshot) {} }

class CommerceState(initialRepository: CommerceRepository = DemoCommerceRepository()) : ViewModel() {
    var live: LiveCommerceState? = null
    var repository by mutableStateOf(initialRepository); private set
    private var currentHoldId: String? = null
    var page by mutableStateOf(CommercePage.COMBOS)
    var cart by mutableStateOf<ComboSelection?>(null)
    var promoInput by mutableStateOf("")
    var promoApplied by mutableStateOf("")
    var promoMessage by mutableStateOf("")
    var promoChecking by mutableStateOf(false)
    var email by mutableStateOf("")
    var name by mutableStateOf("")
    var phone by mutableStateOf("")
    var emailTouched by mutableStateOf(false)
    var method by mutableStateOf("Ví điện tử")
    var snapshot by mutableStateOf<BookingSnapshot?>(null)
    var transactionMessage by mutableStateOf("")
    var nextOutcome by mutableStateOf("success")
    val comboTotal get() = cart?.let { (it.product.price + it.surcharge) * it.quantity } ?: 0L
    fun discount(seatTotal: Long): Long = if (promoApplied == "CINE20" && seatTotal + comboTotal >= 300_000) 20_000 else 0
    fun quote(seatTotal: Long): PriceQuote? = priceQuote(seatTotal, comboTotal, promoApplied)
    fun setCart(choice: ComboChoice, quantity: Int, variant: String? = null, surcharge: Long? = null) {
        val existing = cart?.takeIf { it.product.id == choice.id }
        cart = if (quantity <= 0) null else ComboSelection(choice, quantity.coerceAtMost(9), variant ?: existing?.variant.orEmpty(), surcharge ?: existing?.surcharge ?: 0)
    }
    fun reprice(seatTotal: Long) {
        if (promoApplied == "CINE20" && seatTotal + comboTotal < 300_000) {
            promoApplied = ""; promoMessage = "Mã CINE20 không còn đủ điều kiện sau khi thay đổi đơn."
        }
    }
    fun applyPromo(seatTotal: Long) {
        promoApplied = ""
        promoMessage = when {
            promoInput.isBlank() -> ""
            promoInput.trim().uppercase() !in listOf("CINE20", "FIRST50") -> "Mã ưu đãi không hợp lệ."
            promoInput == "CINE20" && seatTotal + comboTotal < 300_000 -> "CINE20 cần tổng trước giảm từ 300.000đ."
            else -> { promoApplied = promoInput; "Đã chọn voucher $promoApplied." }
        }
        reprice(seatTotal)
    }
    fun begin(orderId: String, movie: String, showtime: String, auditorium: String, seats: List<String>, seatTotal: Long): Boolean {
        val quote = quote(seatTotal) ?: return false
        if (name.trim().length !in 2..100 || email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() || !phone.replace(" ", "").matches(Regex("(0[0-9]{9}|\\+84[0-9]{9})"))) { emailTouched = true; return false }
        snapshot = BookingSnapshot(orderId, UUID.randomUUID().toString(), movie, showtime, auditorium,
            seats.toList(), listOfNotNull(cart), quote, name.trim(), email.trim(), phone, if (quote.due == 0L) "Voucher" else method)
        page = CommercePage.PROCESSING
        return true
    }
    fun finish(outcome: String) {
        val saved = snapshot ?: return
        when (outcome) {
            "success" -> { repository.complete(saved); page = CommercePage.SUCCESS }
            "failed" -> page = CommercePage.FAILED
            "cancelled" -> page = CommercePage.CANCELLED
            else -> page = CommercePage.UNKNOWN
        }
    }
    fun bindHold(id: String?) {
        if (id == null || id == currentHoldId) return
        currentHoldId = id; page = CommercePage.COMBOS; cart = null
        promoInput = ""; promoApplied = ""; promoMessage = ""; promoChecking = false
        snapshot = null; transactionMessage = ""; nextOutcome = "success"
    }
    fun clear() {
        live?.clear()
        repository = DemoCommerceRepository(); currentHoldId = null; page = CommercePage.COMBOS; cart = null
        promoInput = ""; promoApplied = ""; promoMessage = ""; promoChecking = false
        name = ""; email = ""; phone = ""; emailTouched = false
        method = "Ví điện tử"; snapshot = null; transactionMessage = ""; nextOutcome = "success"
    }
}
