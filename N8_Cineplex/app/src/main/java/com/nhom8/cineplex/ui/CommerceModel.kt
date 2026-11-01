package com.nhom8.cineplex.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import java.util.UUID

/** VND amounts are integers. This is a local demonstration until commerce APIs exist. */
data class ComboChoice(val id: String, val name: String, val description: String, val price: Long, val image: Int, val hasOptions: Boolean = false, val soldOut: Boolean = false)
data class ComboSelection(val product: ComboChoice, val quantity: Int, val variant: String = "", val surcharge: Long = 0)
data class PriceQuote(val seats: Long, val combo: Long, val fee: Long, val discount: Long, val credit: Long) {
    val subtotal get() = seats + combo + fee
    val afterDiscount get() = (subtotal - discount).coerceAtLeast(0)
    val due get() = afterDiscount - credit
}
fun priceQuote(seats: Long, combo: Long, promo: String, creditBalance: Long, creditRequested: Long): PriceQuote {
    require(seats >= 0 && combo >= 0 && creditBalance >= 0)
    val subtotal = seats + combo
    val discount = if (promo == "CINE20" && subtotal >= 300_000) 20_000L else 0L
    val cap = minOf(creditBalance, subtotal - discount)
    require(creditRequested in 0..cap)
    return PriceQuote(seats, combo, 0, discount, creditRequested)
}

enum class CommercePage { COMBOS, CHECKOUT, PROCESSING, SUCCESS, FAILED, UNKNOWN, CANCELLED, TICKET }
enum class LedgerStatus { POSTED, PENDING, FAILED }
data class CreditEntry(val id: String, val title: String, val amount: Long, val status: LedgerStatus, val note: String,
    val orderId: String = "", val time: String = "", val requested: Long? = null, val deduction: Long? = null)
data class BookingSnapshot(val orderId: String, val attemptId: String, val movie: String, val showtime: String, val auditorium: String,
    val seats: List<String>, val products: List<ComboSelection>, val quote: PriceQuote, val name: String, val email: String, val phone: String, val method: String)

interface CommerceRepository {
    val entries: List<CreditEntry>
    val balance: Long get() = entries.filter { it.status == LedgerStatus.POSTED }.sumOf { it.amount }
    fun complete(snapshot: BookingSnapshot)
}

class DemoCommerceRepository(seed: List<CreditEntry> = listOf(
        CreditEntry("RC-101", "Hoàn vé đã cộng", 200_000, LedgerStatus.POSTED, "Điều kiện và khấu trừ là dữ liệu mẫu, không phải chính sách hoàn vé thật.", "CPX-1010-024", "10/10/2026 · 15:30", 210_000, 10_000),
        CreditEntry("RC-102", "Đã dùng Credit", -50_000, LedgerStatus.POSTED, "Sử dụng số dư khi đặt vé mẫu.", "CPX-1210-086", "12/10/2026 · 18:05"),
        CreditEntry("RC-104", "Hoàn vé đang xử lý", 80_000, LedgerStatus.PENDING, "Chưa cộng vào số dư khả dụng.", "CPX-1510-048", "15/10/2026 · 10:20", 80_000, 0),
        CreditEntry("RC-103", "Hoàn vé thất bại", 50_000, LedgerStatus.FAILED, "Không cộng vào số dư khả dụng.", "CPX-1410-019", "14/10/2026 · 17:40", 50_000, 0)
    )) : CommerceRepository {
    private val ledger = mutableStateListOf<CreditEntry>().apply { addAll(seed) }
    private val settled = mutableSetOf<String>()
    override val entries: List<CreditEntry> get() = ledger
    override fun complete(snapshot: BookingSnapshot) {
        if (settled.add(snapshot.orderId) && snapshot.quote.credit > 0)
            ledger.add(0, CreditEntry("use-${snapshot.orderId}", "Dùng Credit cho đơn mẫu", -snapshot.quote.credit,
                LedgerStatus.POSTED, "Sử dụng số dư khi đặt vé mẫu.", snapshot.orderId))
    }
}

class CommerceState(initialRepository: CommerceRepository = DemoCommerceRepository()) : ViewModel() {
    var repository by mutableStateOf(initialRepository); private set
    private var currentHoldId: String? = null
    var page by mutableStateOf(CommercePage.COMBOS)
    var cart by mutableStateOf<ComboSelection?>(null)
    var promoInput by mutableStateOf("")
    var promoApplied by mutableStateOf("")
    var promoMessage by mutableStateOf("")
    var creditNotice by mutableStateOf("")
    var promoChecking by mutableStateOf(false)
    var useCredit by mutableStateOf(true)
    var creditInput by mutableStateOf("")
    var creditEdited by mutableStateOf(false)
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
    fun creditCap(seatTotal: Long): Long = minOf(repository.balance, (seatTotal + comboTotal - discount(seatTotal)).coerceAtLeast(0))
    fun creditAmount(seatTotal: Long): Long = if (!useCredit) 0 else if (!creditEdited) creditCap(seatTotal) else creditInput.toLongOrNull() ?: -1
    fun creditError(seatTotal: Long): String? = if (!useCredit) null else when {
        creditInput.isNotBlank() && creditInput.toLongOrNull() == null -> "Nhập số Credit nguyên không âm."
        creditAmount(seatTotal) < 0 -> "Credit không được âm."
        creditAmount(seatTotal) > creditCap(seatTotal) -> "Tối đa ${creditCap(seatTotal)}đ theo số dư và tổng đơn."
        else -> null
    }
    fun quote(seatTotal: Long): PriceQuote? = if (creditError(seatTotal) == null)
        priceQuote(seatTotal, comboTotal, promoApplied, repository.balance, creditAmount(seatTotal)) else null
    fun setCart(choice: ComboChoice, quantity: Int, variant: String? = null, surcharge: Long? = null) {
        val existing = cart?.takeIf { it.product.id == choice.id }
        cart = if (quantity <= 0) null else ComboSelection(choice, quantity.coerceAtMost(9), variant ?: existing?.variant.orEmpty(), surcharge ?: existing?.surcharge ?: 0)
    }
    fun reprice(seatTotal: Long) {
        if (promoApplied == "CINE20" && seatTotal + comboTotal < 300_000) {
            promoApplied = ""; promoMessage = "Mã CINE20 không còn đủ điều kiện sau khi thay đổi đơn."
        }
        val cap = creditCap(seatTotal)
        val chosen = creditInput.toLongOrNull()
        if (useCredit && creditEdited && chosen != null && chosen > cap) {
            creditInput = cap.toString(); creditNotice = "Credit đã điều chỉnh theo tổng đơn mới."
        }
    }
    fun applyPromo(seatTotal: Long) {
        promoApplied = ""
        promoMessage = when {
            promoInput.isBlank() -> ""
            promoInput.trim().uppercase() != "CINE20" -> "Mã ưu đãi không hợp lệ."
            seatTotal + comboTotal < 300_000 -> "CINE20 cần tổng trước giảm từ 300.000đ."
            else -> { promoApplied = "CINE20"; "Đã giảm 20.000đ." }
        }
        reprice(seatTotal)
        reprice(seatTotal)
    }
    fun begin(orderId: String, movie: String, showtime: String, auditorium: String, seats: List<String>, seatTotal: Long): Boolean {
        val quote = quote(seatTotal) ?: return false
        if (name.trim().length !in 2..100 || email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() || !phone.replace(" ", "").matches(Regex("(0[0-9]{9}|\\+84[0-9]{9})"))) { emailTouched = true; return false }
        snapshot = BookingSnapshot(orderId, UUID.randomUUID().toString(), movie, showtime, auditorium,
            seats.toList(), listOfNotNull(cart), quote, name.trim(), email.trim(), phone, if (quote.due == 0L) "Credit" else method)
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
        promoInput = ""; promoApplied = ""; promoMessage = ""; promoChecking = false; creditNotice = ""
        creditInput = ""; creditEdited = false; useCredit = repository.balance > 0
        snapshot = null; transactionMessage = ""; nextOutcome = "success"
    }
    fun clear() {
        repository = DemoCommerceRepository(); currentHoldId = null; page = CommercePage.COMBOS; cart = null
        promoInput = ""; promoApplied = ""; promoMessage = ""; promoChecking = false; creditNotice = ""
        useCredit = true; creditInput = ""; creditEdited = false; name = ""; email = ""; phone = ""; emailTouched = false
        method = "Ví điện tử"; snapshot = null; transactionMessage = ""; nextOutcome = "success"
    }
    fun zeroBalanceDemo() {
        repository = DemoCommerceRepository(emptyList()); useCredit = false; creditInput = ""; creditEdited = false
    }
    fun restoreLedgerDemo() {
        repository = DemoCommerceRepository(); useCredit = true; creditInput = ""; creditEdited = false
    }
}
