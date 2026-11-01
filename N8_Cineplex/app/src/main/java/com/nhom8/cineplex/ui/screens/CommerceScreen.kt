package com.nhom8.cineplex.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.nhom8.cineplex.BuildConfig
import com.nhom8.cineplex.R
import com.nhom8.cineplex.data.ShowtimeDto
import com.nhom8.cineplex.data.isoMillis
import com.nhom8.cineplex.data.vietnamDate
import com.nhom8.cineplex.data.vietnamTime
import com.nhom8.cineplex.data.displayCalendarDate
import com.nhom8.cineplex.model.Movie
import com.nhom8.cineplex.ui.*
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale
import java.util.Date

private val catalog = listOf(
    ComboChoice("one", "Combo 1 người", "1 bắp vừa · 1 nước", 79_000, R.drawable.combo_two, hasOptions = true),
    ComboChoice("two", "Combo 2 người", "1 bắp lớn · 2 nước", 100_000, R.drawable.combo_two, hasOptions = true),
    ComboChoice("popcorn", "Bắp riêng", "Chọn vị và kích cỡ", 49_000, R.drawable.combo_popcorn, hasOptions = true),
    ComboChoice("drink", "Nước riêng", "Ly 500 ml", 35_000, R.drawable.combo_drink)
)
private object C {
    val Primary = Color(0xFF8C2945)
    val Text = Color(0xFF20222A)
    val Muted = Color(0xFF5D6470)
    val Line = Color(0xFFD9DDE4)
    val Error = Color(0xFF9C263D)
    val Success = Color(0xFF246345)
    val Soft = Color(0xFFF5E8EC)
}
private fun money(amount: Long) = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(amount) + "đ"
private fun clock(seconds: Int) = "%02d:%02d".format(seconds / 60, seconds % 60)

@Composable
private fun CommerceCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, C.Line)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable
private fun PriceLine(label: String, amount: Long, emphasized: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (emphasized) C.Text else C.Muted, fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal)
        Text(money(amount), color = if (emphasized) C.Primary else C.Text, fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommercePreviewScreen(vm: CineplexViewModel, seats: SeatSelectionViewModel, movie: Movie, slot: ShowtimeDto, state: CommerceState) {
    state.live?.let { LiveCommerceScreen(vm, seats, it); return }
    LaunchedEffect(seats.order?.id) { state.bindHold(seats.order?.id) }
    var options by remember { mutableStateOf<ComboChoice?>(null) }
    var filter by remember { mutableStateOf("all") }
    var confirmSkip by remember { mutableStateOf(false) }
    var flavour by remember { mutableStateOf("Bơ") }
    var large by remember { mutableStateOf(false) }
    LaunchedEffect(options?.id) {
        options?.let { product ->
            val current = state.cart?.takeIf { it.product.id == product.id }
            flavour = if (current?.variant?.startsWith("Caramel") == true) "Caramel" else "Bơ"
            large = product.id == "popcorn" && current?.surcharge == 10_000L
        }
    }
    val seatTotal = seats.order?.seats?.sumOf { it.price.toLongOrNull() ?: 0 } ?: 0L
    val showtimeLabel = "${displayCalendarDate(vietnamDate(Date(isoMillis(slot.startAt))))} · ${vietnamTime(slot.startAt)}"
    val active = seats.held && seats.secondsLeft > 0 && seats.online && !seats.syncing && !seats.confirming
    val quote = state.quote(seatTotal)
    val shownDue = quote?.due ?: (seatTotal + state.comboTotal - state.discount(seatTotal)).coerceAtLeast(0)
    val page = state.page
    LaunchedEffect(seatTotal, state.comboTotal) { state.reprice(seatTotal) }
    LaunchedEffect(page, seats.secondsLeft, seats.online) {
        if (page == CommercePage.PROCESSING && (!seats.online || seats.secondsLeft == 0)) {
            state.transactionMessage = "Chưa xác định kết quả. Kiểm tra lại cùng giao dịch."
            state.nextOutcome = "unknown"
            state.page = CommercePage.UNKNOWN
        }
    }
    LaunchedEffect(page, state.snapshot?.attemptId) {
        if (page == CommercePage.PROCESSING) {
            delay(1600)
            if (state.page == CommercePage.PROCESSING) state.finish(state.nextOutcome)
        }
    }
    LaunchedEffect(page) {
        if (page == CommercePage.SUCCESS && seats.held) seats.cancelHold()
    }
    Scaffold(containerColor = Color(0xFFF6F7F9), topBar = {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton({ when (state.page) { CommercePage.CHECKOUT -> state.page = CommercePage.COMBOS; CommercePage.PROCESSING, CommercePage.UNKNOWN -> state.transactionMessage = "Hãy kiểm tra kết quả giao dịch mẫu trước khi rời luồng."; else -> vm.back() } }) { Text("‹", fontSize = 30.sp) }
            Text(when (page) { CommercePage.COMBOS -> "Bắp nước & Combo"; CommercePage.CHECKOUT -> "Thanh toán"; CommercePage.TICKET -> "Vé mẫu"; else -> "Trạng thái giao dịch" }, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }, bottomBar = {
        Column(Modifier.fillMaxWidth().background(Color.White).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when (page) {
                CommercePage.COMBOS -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text("Tổng bắp nước", color = C.Muted, fontSize = 13.sp); Text(money(state.comboTotal), color = C.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                        TextButton({ if (state.cart == null) state.page = CommercePage.CHECKOUT else confirmSkip = true }, enabled = active) { Text("Bỏ qua bắp nước") }
                    }
                    Button({ state.page = CommercePage.CHECKOUT }, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = active) { Text("Tiếp tục") }
                }
                CommercePage.CHECKOUT -> {
                    PriceLine("Còn cần thanh toán", shownDue, true)
                    Button({
                        if (active && quote != null) seats.order?.let { state.begin(it.id, movie.name, showtimeLabel, slot.auditorium.name, it.seats.map { seat -> seat.seatCode }, seatTotal) }
                    }, Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("commerce-pay"), enabled = active && quote != null && !state.promoChecking) {
                        Text(if (quote?.due == 0L) "Xác nhận đặt vé mẫu" else "Mô phỏng thanh toán ${money(shownDue)}")
                    }
                }
                CommercePage.SUCCESS -> Button({ state.page = CommercePage.TICKET }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Xem vé mẫu") }
                CommercePage.FAILED, CommercePage.CANCELLED -> Button({ state.page = CommercePage.CHECKOUT }, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = active) { Text("Về checkout") }
                CommercePage.UNKNOWN -> Button({ state.finish(state.nextOutcome) }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Kiểm tra lại giao dịch mẫu") }
                CommercePage.TICKET -> Button(vm::back, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Quay lại chọn ghế") }
                CommercePage.PROCESSING -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    TextButton({ state.finish("cancelled") }, Modifier.fillMaxWidth()) { Text("Hủy giao dịch mẫu") }
                }
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(color = if (seats.secondsLeft < 120) C.Primary else Color(0xFF343434), shape = RoundedCornerShape(12.dp)) {
                Text(if (seats.held) "Thời gian giữ ghế · ${clock(seats.secondsLeft)}" else "Giữ chỗ đã kết thúc", Modifier.fillMaxWidth().padding(14.dp).testTag("seat-countdown"), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text("BẢN XEM TRƯỚC · Chưa thanh toán hoặc cộng/trừ Credit thật", color = C.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (page == CommercePage.COMBOS || page == CommercePage.CHECKOUT) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("✓ Chọn ghế", color = C.Success, fontSize = 13.sp)
                    Text(if (page == CommercePage.COMBOS) "● Bắp nước" else "✓ Bắp nước", color = if (page == CommercePage.COMBOS) C.Primary else C.Success, fontSize = 13.sp)
                    Text(if (page == CommercePage.CHECKOUT) "● Thanh toán" else "Thanh toán", color = if (page == CommercePage.CHECKOUT) C.Primary else C.Muted, fontSize = 13.sp)
                }
            }
            if (state.transactionMessage.isNotBlank()) Text(state.transactionMessage, color = C.Error)
            if (!active && page in listOf(CommercePage.COMBOS, CommercePage.CHECKOUT)) Text("Không thể tiếp tục. Kiểm tra kết nối và thời hạn giữ ghế, hoặc quay lại chọn ghế.", color = C.Error)
            when (page) {
                CommercePage.COMBOS -> {
                    Text("Đang giữ ghế", color = C.Success, fontWeight = FontWeight.Bold)
                    Column { Text("TRỌN VỊ ĐIỆN ẢNH", color = C.Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text("Phim hay, thêm bắp.", fontSize = 26.sp, fontWeight = FontWeight.Bold); Text("Chọn món bạn thích hoặc bỏ qua bước này.", color = C.Muted) }
                    Text("Chọn bắp nước", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("all" to "Tất cả", "combo" to "Combo", "single" to "Món lẻ").forEach { (id, label) ->
                            FilterChip(selected = filter == id, onClick = { filter = id }, label = { Text(label) })
                        }
                    }
                    TextButton(vm::back) { Text("Quay lại sơ đồ") }
                    catalog.filter { filter == "all" || (filter == "combo") == (it.id == "one" || it.id == "two") }.forEach { product -> CommerceCard {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(product.image), product.name, Modifier.size(84.dp).background(Color(0xFFF6F7F9)), contentScale = ContentScale.Fit)
                            Column(Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.Bold)
                                Text(product.description, color = C.Muted, fontSize = 13.sp)
                                Text(money(product.price), color = C.Primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (product.soldOut) Text("Hết hàng", color = C.Error)
                        else if (state.cart?.product?.id == product.id) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton({ state.setCart(product, state.cart!!.quantity - 1) }) { Text("−") }
                                Text("${state.cart!!.quantity}", Modifier.padding(16.dp))
                                OutlinedButton({ state.setCart(product, state.cart!!.quantity + 1) }, enabled = state.cart!!.quantity < 9) { Text("+") }
                                if (product.hasOptions) TextButton({ options = product }) { Text("Đổi tùy chọn") }
                            }
                            if (state.cart!!.variant.isNotBlank()) Text("${state.cart!!.variant} · phụ thu ${money(state.cart!!.surcharge)}", color = C.Muted)
                        } else OutlinedButton({ if (product.hasOptions) options = product else state.setCart(product, 1, "", 0) }) { Text("Chọn sản phẩm") }
                    } }
                    Text("Ảnh, giá và lựa chọn sản phẩm là dữ liệu mẫu.", color = C.Muted, fontSize = 13.sp)
                }
                CommercePage.CHECKOUT -> {
                    Text("Kiểm tra đơn đặt vé", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    CommerceCard {
                        Text(movie.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("N8 Cineplex · ${slot.auditorium.name} · ${slot.format.replace('_', ' ')}")
                        Text("Suất: $showtimeLabel · ${movie.age}", color = C.Muted)
                        val orderSeats = seats.order?.seats.orEmpty()
                        val seatMeta = seats.seats.associateBy { it.seatCode }
                        orderSeats.groupBy { seat -> seatMeta[seat.seatCode]?.takeIf { it.seatType == "COUPLE" }?.let { "${it.rowName}:${(it.seatNumber - 1) / 2}" } ?: seat.seatCode }.forEach { (_, group) ->
                            val couple = group.size == 2 && group.all { seatMeta[it.seatCode]?.seatType == "COUPLE" }
                            PriceLine(if (couple) "Couple ${group.joinToString("–") { it.seatCode }} · 2 chỗ" else "Ghế ${group.joinToString { it.seatCode }}", group.sumOf { it.price.toLongOrNull() ?: 0 })
                        }
                        state.cart?.let { PriceLine("${it.product.name} × ${it.quantity}${if (it.variant.isBlank()) "" else " · ${it.variant}"}", state.comboTotal) }
                    }
                    CommerceCard {
                        Text("Thông tin nhận vé mẫu", fontWeight = FontWeight.Bold)
                        OutlinedTextField(state.name, { state.name = it; state.emailTouched = true }, Modifier.fillMaxWidth(), label = { Text("Họ và tên") }, singleLine = true, isError = state.emailTouched && state.name.trim().length !in 2..100)
                        if (state.emailTouched && state.name.trim().length !in 2..100) Text("Họ tên cần 2–100 ký tự.", color = C.Error, fontSize = 13.sp)
                        OutlinedTextField(state.email, { state.email = it; state.emailTouched = true }, Modifier.fillMaxWidth(), label = { Text("Email nhận vé") }, singleLine = true, isError = state.emailTouched && !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches())
                        if (state.emailTouched && !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) Text("Nhập email hợp lệ.", color = C.Error, fontSize = 13.sp)
                        val phoneValid = state.phone.replace(" ", "").matches(Regex("(0[0-9]{9}|\\+84[0-9]{9})"))
                        OutlinedTextField(state.phone, { state.phone = it; state.emailTouched = true }, Modifier.fillMaxWidth(), label = { Text("Số điện thoại") }, singleLine = true, isError = state.emailTouched && !phoneValid)
                        if (state.emailTouched && !phoneValid) Text("Nhập số điện thoại 10 chữ số hoặc định dạng +84.", color = C.Error, fontSize = 13.sp)
                        Text("Không gửi email thật trong bản xem trước.", color = C.Muted, fontSize = 13.sp)
                    }
                    CommerceCard {
                        Text("Mã ưu đãi", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(state.promoInput, { state.promoInput = it }, Modifier.weight(1f), label = { Text("Nhập mã") }, singleLine = true)
                            Button({ state.promoChecking = true }, enabled = !state.promoChecking) { Text(if (state.promoChecking) "…" else "Áp dụng") }
                        }
                        if (state.promoChecking) LaunchedEffect(state.promoInput, seatTotal, state.comboTotal) { delay(700); state.applyPromo(seatTotal); state.promoChecking = false }
                        if (state.promoMessage.isNotBlank()) Text(state.promoMessage, color = if (state.promoApplied.isNotBlank()) C.Success else C.Error)
                    }
                    CommerceCard {
                        Text("Số dư hoàn vé", fontWeight = FontWeight.Bold)
                        PriceLine("Số dư khả dụng mẫu", state.repository.balance)
                        Row(verticalAlignment = Alignment.CenterVertically) { Text("Dùng Credit", Modifier.weight(1f)); Switch(state.useCredit, { state.useCredit = it }, enabled = state.repository.balance > 0) }
                        if (state.useCredit) {
                            OutlinedTextField(if (state.creditEdited) state.creditInput else state.creditCap(seatTotal).toString(), { state.creditEdited = true; state.creditInput = it }, Modifier.fillMaxWidth(), label = { Text("Credit sử dụng (đ)") }, singleLine = true, isError = state.creditError(seatTotal) != null)
                            state.creditError(seatTotal)?.let { Text(it, color = C.Error, fontSize = 13.sp) }
                        }
                        if (state.creditNotice.isNotBlank()) Text(state.creditNotice, color = C.Success, fontSize = 13.sp)
                        Text("Credit nội bộ dùng cho đơn sau, không rút về ngân hàng.", color = C.Muted, fontSize = 13.sp)
                    }
                    if ((quote?.due ?: 1) > 0) CommerceCard {
                        Text("Phương thức thanh toán mẫu", fontWeight = FontWeight.Bold)
                        listOf("Ví điện tử", "Thẻ ngân hàng").forEach { method -> Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(state.method == method, { state.method = method }); Text(method)
                        } }
                        Text("Không nhập thông tin thẻ hoặc tạo giao dịch tiền thật.", color = C.Muted, fontSize = 13.sp)
                    }
                    CommerceCard {
                        Text("Chi tiết tiền", fontWeight = FontWeight.Bold)
                        PriceLine("Tiền vé", seatTotal); PriceLine("Combo", state.comboTotal); PriceLine("Phí dịch vụ", 0)
                        PriceLine("Tổng trước giảm", seatTotal + state.comboTotal)
                        PriceLine("Giảm giá", -(quote?.discount ?: state.discount(seatTotal)))
                        PriceLine("Tổng sau giảm", (seatTotal + state.comboTotal - state.discount(seatTotal)).coerceAtLeast(0))
                        PriceLine("Credit sử dụng", -(quote?.credit ?: 0)); HorizontalDivider()
                        PriceLine("Còn thanh toán", shownDue, true)
                    }
                }
                CommercePage.PROCESSING -> ResultCard("Đang xử lý mô phỏng", "Đang chờ kết quả cho cùng một giao dịch. Không bấm thanh toán lại.", state.snapshot)
                CommercePage.SUCCESS -> ResultCard("Mô phỏng thành công", "Đã ghi Credit trong dữ liệu mẫu đúng một lần. Không có vé hoặc giao dịch thật.", state.snapshot)
                CommercePage.FAILED -> ResultCard("Mô phỏng thất bại", "Credit chưa bị trừ. Có thể thử lại khi phiên ghế còn hạn.", state.snapshot)
                CommercePage.CANCELLED -> ResultCard("Đã hủy mô phỏng", "Credit chưa bị trừ.", state.snapshot)
                CommercePage.UNKNOWN -> ResultCard("Chưa xác định kết quả", "Không tạo giao dịch mới. Kiểm tra lại cùng mã bên dưới.", state.snapshot)
                CommercePage.TICKET -> {
                    ResultCard("Vé mẫu · không có giá trị soát vé", "QR chỉ mã hóa mã đơn demo; backend chưa phát hành vé.", state.snapshot)
                    state.snapshot?.let { saved -> CommerceCard {
                        val bitmap = remember(saved.orderId) {
                            val matrix = QRCodeWriter().encode("CINEPLEX-DEMO:${saved.orderId}", BarcodeFormat.QR_CODE, 192, 192)
                            Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888).apply {
                                for (y in 0 until 192) for (x in 0 until 192) setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                            }.asImageBitmap()
                        }
                        Image(bitmap, "QR demo không có giá trị soát vé", Modifier.align(Alignment.CenterHorizontally).size(192.dp))
                        Text(saved.movie, fontWeight = FontWeight.Bold)
                        Text("Suất: ${saved.showtime}")
                        Text("Rạp: N8 Cineplex · ${saved.auditorium}")
                        Text("Ghế: ${saved.seats.joinToString()}")
                        PriceLine("Đã thanh toán trong mô phỏng", saved.quote.afterDiscount, true)
                        Text("Mã đơn mẫu: ${saved.orderId}", fontSize = 12.sp, color = C.Muted)
                    } }
                }
            }
            if (BuildConfig.DEBUG && page in listOf(CommercePage.COMBOS, CommercePage.CHECKOUT, CommercePage.UNKNOWN)) CommerceCard {
                Text("Tình huống thử · debug", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("success" to "Thành công", "failed" to "Thất bại", "unknown" to "Chưa rõ", "cancelled" to "Hủy").forEach { (value, label) ->
                        TextButton({ state.nextOutcome = value }) { Text(label, fontSize = 11.sp) }
                    }
                }
                if (page != CommercePage.UNKNOWN) TextButton({ state.zeroBalanceDemo() }) { Text("Số dư 0 · lịch sử trống") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    options?.let { product -> ModalBottomSheet(onDismissRequest = { options = null }) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Tùy chọn ${product.name}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Vị bơ và caramel cùng giá. Phô mai hiện hết hàng.")
            listOf("Bơ", "Caramel", "Phô mai").forEach { value -> Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(flavour == value, { flavour = value }, enabled = value != "Phô mai"); Text(value, color = if (value == "Phô mai") C.Muted else C.Text)
            } }
            if (product.id == "two") Text("Bắp lớn là cỡ chuẩn của combo này, không phụ thu.")
            else if (product.id == "popcorn") Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(large, { large = it }); Text("Cỡ lớn · phụ thu ${money(10_000)}") }
            val extra = if (large && product.id == "popcorn") 10_000L else 0L
            PriceLine("Giá một phần", product.price + extra, true)
            Button({ state.setCart(product, state.cart?.takeIf { it.product.id == product.id }?.quantity ?: 1, "$flavour · ${if (product.id == "two" || large) "Lớn" else "Vừa"}", extra); options = null }, Modifier.fillMaxWidth()) { Text("Xác nhận tùy chọn") }
        }
    } }
    if (confirmSkip) AlertDialog(onDismissRequest = { confirmSkip = false }, title = { Text("Bỏ qua bắp nước?") },
        text = { Text("Combo đã chọn sẽ được bỏ khỏi đơn. Ghế và thời hạn giữ ghế vẫn giữ nguyên.") },
        confirmButton = { TextButton({ state.cart = null; state.reprice(seatTotal); state.page = CommercePage.CHECKOUT; confirmSkip = false }) { Text("Bỏ combo và tiếp tục") } },
        dismissButton = { TextButton({ confirmSkip = false }) { Text("Giữ combo") } })
}

@Composable
private fun ResultCard(title: String, detail: String, snapshot: BookingSnapshot?) {
    CommerceCard {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = C.Primary)
        Text(detail)
        snapshot?.let { Text("Mã giao dịch mẫu: ${it.attemptId}", fontSize = 12.sp, color = C.Muted) }
    }
}

@Composable
fun CreditWalletScreen(state: CommerceState, back: () -> Unit) {
    state.live?.let { LiveCreditWallet(it, back); return }
    var detail by remember { mutableStateOf<CreditEntry?>(null) }
    Scaffold(topBar = { TextButton(back) { Text("‹  Số dư hoàn vé") } }, containerColor = Color(0xFFF6F7F9)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = C.Primary) { Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Text("Số dư khả dụng · dữ liệu mẫu", color = Color.White)
                Text(money(state.repository.balance), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("Credit dùng cho lần đặt vé tiếp theo. Không thể rút tiền.", color = Color.White)
            } }
            Text("Lịch sử Credit mẫu", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (state.repository.entries.isEmpty()) CommerceCard {
                Text("Chưa có giao dịch", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Lịch sử cộng và sử dụng số dư sẽ xuất hiện tại đây.", color = C.Muted)
            }
            state.repository.entries.forEach { entry -> Surface(onClick = { detail = entry }, shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, C.Line)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(entry.title, fontWeight = FontWeight.Bold)
                        Text(entry.time.ifBlank { entry.orderId }, color = C.Muted, fontSize = 13.sp)
                        Text(when(entry.status) { LedgerStatus.POSTED -> if (entry.amount < 0) "Đã dùng Credit" else "Đã cộng Credit"; LedgerStatus.PENDING -> "Đang xử lý · chưa cộng"; LedgerStatus.FAILED -> "Thất bại · chưa cộng" }, color = C.Muted, fontSize = 13.sp)
                    }
                    Text((if (entry.amount > 0) "+" else "") + money(entry.amount), color = if (entry.status == LedgerStatus.POSTED) C.Success else C.Muted)
                }
            } }
            if (BuildConfig.DEBUG) TextButton(state::restoreLedgerDemo) { Text("Nạp ví dụ Credit") }
            Text("Số dư và lịch sử này là mô phỏng trên thiết bị; backend hiện chưa cung cấp API Credit.", color = C.Muted)
        }
    }
    detail?.let { entry -> AlertDialog(onDismissRequest = { detail = null }, title = { Text("Chi tiết giao dịch") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(entry.title, fontWeight = FontWeight.Bold)
            Text("Mã giao dịch: ${entry.id}")
            if (entry.orderId.isNotBlank()) Text("Mã đơn: ${entry.orderId}")
            if (entry.time.isNotBlank()) Text("Thời gian: ${entry.time}")
            Text("Số tiền: ${money(entry.amount)}")
            entry.requested?.let { PriceLine("Đề nghị hoàn", it) }
            entry.deduction?.let { PriceLine("Khấu trừ mẫu", it) }
            Text(entry.note, color = C.Muted)
        } }, confirmButton = { TextButton({ detail = null }) { Text("Đóng") } }) }
}
