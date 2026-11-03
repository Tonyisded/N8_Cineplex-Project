package com.nhom8.cineplex.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.ui.*
import com.nhom8.cineplex.ui.components.CustomerNavigation
import com.nhom8.cineplex.ui.components.CineplexIcon
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.nhom8.cineplex.ui.theme.CineplexColors as C
import kotlinx.coroutines.delay
import androidx.lifecycle.repeatOnLifecycle
import java.text.NumberFormat
import java.util.Locale

private fun money(value: String) = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value.toLongOrNull() ?: 0) + "đ"

@Composable
fun NativeTicketQr(payload: String, label: String) {
    val bitmap = remember(payload) {
        runCatching {
            val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 480, 480)
            Bitmap.createBitmap(480, 480, Bitmap.Config.ARGB_8888).apply {
                setPixels(IntArray(480 * 480) { i -> if (matrix[i % 480, i / 480]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }, 0, 480, 0, 0, 480, 480)
            }
        }.getOrNull()
    }
    if (bitmap != null) Image(bitmap.asImageBitmap(), label, Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 320.dp).testTag("native-ticket-qr"))
    else Text("Không hiển thị được QR. Hãy tải lại; không dùng mã thay thế.", color = C.Error)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(vm: CineplexViewModel, state: TicketsState, secondsLeft: Int? = null) {
    val payment = state.payment
    TransactionAutoRefresh(state)
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(payment?.expiresAt) { while (true) { now = System.currentTimeMillis(); delay(1_000) } }
    val seconds = secondsLeft ?: payment?.expiresAt?.let { ((isoMillis(it) - now) / 1000).coerceAtLeast(0).toInt() } ?: 0
    val pending = payment?.status == "PENDING" && payment.operationState !in listOf("UNKNOWN", "FAILED") && seconds > 0
    fun leave() { state.closePayment(); vm.tickets() }
    BackHandler { leave() }
    Scaffold(containerColor = C.Background, topBar = {
        TopAppBar(title = { Text("Thanh toán vé") }, navigationIcon = { TextButton(::leave) { Text("‹ Vé của tôi") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(payment?.let(::paymentStatus) ?: "Đang kiểm tra đơn thanh toán", style = MaterialTheme.typography.headlineSmall, color = C.Primary, fontWeight = FontWeight.Bold)
            payment?.let { p ->
                Text(money(p.amount), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = C.Primary, modifier = Modifier.testTag("payment-fixed-amount"))
                if (p.status == "PAID" && p.ticketId != null) {
                    Text("Vé đã được máy chủ xác nhận. Email sẽ được gửi tới tài khoản của bạn.")
                    Button({ state.closePayment(); vm.tickets(); state.openTicket(p.ticketId) }, Modifier.fillMaxWidth()) { Text("Xem vé của tôi") }
                } else if (pending) {
                    Text("Còn %02d:%02d để thanh toán".format(seconds / 60, seconds % 60))
                    p.qrCode?.takeIf { it.isNotBlank() }?.let { NativeTicketQr(it, "Mã QR thanh toán đúng số tiền của đơn") }
                    TicketPanel {
                        Text(p.accountName.orEmpty(), fontWeight = FontWeight.Bold)
                        Text("Tài khoản nhận: ${p.accountNumber.orEmpty()}")
                        Text("Nội dung: ${p.description.orEmpty()}")
                        Text("QR đã điền sẵn số tiền và nội dung. Không chỉnh sửa khi chuyển khoản.", color = C.Muted)
                    }

                } else Text(when {
                    p.status == "NEEDS_ADMIN" -> "Khoản tiền cần được kiểm tra. Không chuyển thêm tiền. Liên hệ hỗ trợ với mã đơn bên dưới."
                    p.status == "EXPIRED" || seconds <= 0 -> "Đã hết thời hạn giữ ghế. Không chuyển khoản bằng QR cũ."
                    else -> "Chưa xác định kết quả. Không chuyển lại hoặc tạo đơn mới; kiểm tra cùng đơn."
                })
            }
            if (state.refreshError.isNotBlank()) Text(state.refreshError, color=C.Muted, style=MaterialTheme.typography.bodySmall)
            if (state.paymentBusy) CircularProgressIndicator()
            if (state.paymentError.isNotBlank()) Text(state.paymentError, color = C.Error)
            Text("Mã đơn: ${state.paymentOrderId.orEmpty()}", style = MaterialTheme.typography.bodySmall)
            if (payment?.status != "PAID") Button(state::checkPayment, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = !state.paymentBusy) { Text("Kiểm tra thanh toán") }
            Text("Chỉ hiển thị thành công sau khi máy chủ xác nhận. Quay lại app không đồng nghĩa đã thanh toán.", color = C.Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketsScreen(vm: CineplexViewModel, state: TicketsState?) {
    if (state == null) { Column(Modifier.padding(24.dp)) { Text("Vé thật cần kết nối tài khoản máy chủ."); TextButton(vm::back) { Text("Quay lại") } }; return }
    if (state.paymentOrderId != null) { PaymentScreen(vm, state); return }
    var listNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1_000); listNow = System.currentTimeMillis() } }
    val pendingOrders = state.orders.filter { it.awaitingPaymentAt(listNow) }
    TransactionAutoRefresh(state)
    LaunchedEffect(vm.session?.id) { state.load() }
    val selected = state.selected
    var confirmRefund by remember(selected?.id) { mutableStateOf(false) }
    BackHandler { if (selected != null) state.closeTicket() else vm.back() }
    Scaffold(containerColor = C.Background, topBar = {
        TopAppBar(title = { Text(if (selected == null) "Vé của tôi" else "Chi tiết vé", fontWeight = FontWeight.Bold) },
            navigationIcon = { if (selected != null) IconButton(state::closeTicket, Modifier.size(48.dp).testTag("ticket-detail-back").semantics { contentDescription = "Quay lại vé của tôi" }) { CineplexIcon("back", Modifier.size(28.dp).testTag("ticket-detail-back-icon"), color = C.Primary) } },
            actions = { TextButton({ if (selected == null) state.load() else state.openTicket(selected.id) }, enabled = !state.busy) { Text("Tải lại") } })
    }, bottomBar = { CustomerNavigation(3, vm::back, vm::account, vm::coming, vm::tickets, vm::vouchers) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.error.isNotBlank()) Text(state.error, color = C.Error)
            if (state.refreshError.isNotBlank()) Text(state.refreshError, color=C.Muted, style=MaterialTheme.typography.bodySmall)
            if (selected == null) {
                Surface(shape=RoundedCornerShape(24.dp),color=C.Primary) {
                    Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(C.Primary,Color(0xFF4F182C)))).padding(22.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        Text("MỖI TẤM VÉ, MỘT CUỘC HẸN",color=Color(0xFFF5D6AD),fontSize=11.sp,fontWeight=FontWeight.Bold)
                        Text("Hẹn bạn ở rạp",color=Color.White,fontSize=28.sp,fontWeight=FontWeight.Bold)
                        Text("Vé điện tử và hành trình xem phim của bạn",color=Color.White.copy(alpha=.8f),fontSize=13.sp)
                        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            Surface(color=Color.White.copy(alpha=.12f),shape=RoundedCornerShape(12.dp)){Text("${state.tickets.count{it.status=="VALID"}} vé khả dụng",Modifier.padding(horizontal=12.dp,vertical=8.dp),color=Color.White,fontSize=12.sp)}
                            Surface(color=Color.White.copy(alpha=.12f),shape=RoundedCornerShape(12.dp)){Text("${pendingOrders.size} đơn chờ",Modifier.padding(horizontal=12.dp,vertical=8.dp),color=Color.White,fontSize=12.sp)}
                        }
                    }
                }
                pendingOrders.forEach { order -> TicketPanel {
                    Text("Chờ thanh toán", color = C.Primary, fontWeight = FontWeight.Bold)
                    Text("Ghế ${order.seats.joinToString { it.seatCode }} · ${money(order.totalAmount)}")
                    Text("Mã đơn: ${order.id}", style = MaterialTheme.typography.bodySmall)
                    Button({ state.openPayment(order.id, order.expiresAt, order.totalAmount) }, enabled = !state.busy) { Text("Tiếp tục thanh toán") }
                } }
                Row(Modifier.fillMaxWidth().selectableGroup().testTag("ticket-tabs"), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(false, true).forEach { history ->
                        val chosen = state.history == history
                        val count = state.tickets.count { if(history) it.status != "VALID" else it.status == "VALID" }
                        Surface(Modifier.weight(1f).testTag(if(history) "ticket-tab-history" else "ticket-tab-upcoming")
                            .selectable(chosen, role = Role.Tab) { state.showHistory(history) },
                            shape = RoundedCornerShape(20.dp), color = if(chosen) C.Primary else C.Surface,
                            border = BorderStroke(1.dp, if(chosen) C.Primary else Color(0xFFE4DCE1)), shadowElevation = if(chosen) 3.dp else 0.dp) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(if(history) "Lịch sử" else "Sắp tới", modifier = Modifier.weight(1f), color = if(chosen) Color.White else C.Primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Surface(color = if(chosen) Color.White.copy(alpha = .18f) else Color(0xFFF3E8ED), shape = RoundedCornerShape(9.dp)) {
                                        Text("$count", Modifier.padding(horizontal = 9.dp, vertical = 4.dp), color = if(chosen) Color.White else C.Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(if(history) "Đã dùng · Đã hoàn" else "Vé còn hiệu lực", color = if(chosen) Color.White.copy(alpha = .8f) else C.Muted, fontSize = 11.sp)
                                Box(Modifier.width(28.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(if(chosen) Color(0xFFF5D6AD) else Color(0xFFE7DCE2)))
                            }
                        }
                    }
                }
                if (!state.busy && state.error.isBlank() && state.shownTickets.isEmpty()) TicketPanel {
                    Column(Modifier.fillMaxWidth().padding(vertical=20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Image(painterResource(com.nhom8.cineplex.R.drawable.n8_launcher_logo),"N8 Cineplex",Modifier.size(100.dp))
                        Text(if(state.history) "Hành trình của bạn bắt đầu ở đây" else "Một buổi phim đang chờ bạn",fontWeight=FontWeight.Bold,fontSize=18.sp)
                        Text(if (state.history) "Bạn chưa có vé trong lịch sử." else "Bạn chưa có vé sắp tới.",color=C.Muted)
                    }
                }
                state.shownTickets.forEach { ticket -> TicketPanel {
                    TicketHeading(ticket.snapshot)
                    TicketStatusBadge(ticket.status)
                    TicketDivider()
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        Column {Text("Mã vé",color=C.Muted,fontSize=11.sp);Text(ticket.bookingCode,fontWeight=FontWeight.SemiBold,fontSize=12.sp)}
                        Column(horizontalAlignment=Alignment.End){Text("Đã thanh toán",color=C.Muted,fontSize=11.sp);Text(money(ticket.snapshot.totalAmount),color=C.Primary,fontWeight=FontWeight.Bold,fontSize=20.sp)}
                    }
                    Button({ state.openTicket(ticket.id) }, Modifier.fillMaxWidth(), enabled = !state.busy) { Text("Xem chi tiết vé") }
                } }
            } else {
                TicketPanel {
                    TicketHeading(selected.snapshot)
                    TicketStatusBadge(selected.status)
                    TicketDivider()
                    Surface(color=Color(0xFFFAF2F5),shape=RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {
                            Text("VÉ ĐIỆN TỬ N8",color=C.Primary,fontSize=11.sp,fontWeight=FontWeight.Bold)
                            Text("Mã vé: ${selected.bookingCode}", fontWeight = FontWeight.Bold,fontSize=14.sp)
                            if (selected.status == "VALID") state.qr?.let { NativeTicketQr(it, "QR vé ${selected.bookingCode}") }
                            Text(if(selected.status=="VALID") "Xuất trình QR khi sử dụng vé" else "QR đã ngừng hiệu lực",color=C.Primary,fontWeight=FontWeight.Medium,fontSize=12.sp)
                        }
                    }
                }
                TicketPanel {
                    val s = selected.snapshot
                    Text("Thông tin thanh toán", fontWeight = FontWeight.Bold,fontSize=19.sp)
                    Text("${s.seats.size} ghế · 1 vé điện tử")
                    s.seats.forEach { Text("Ghế ${it.seatCode} · ${money(it.price)}") }
                    if(s.comboItems.isNotEmpty())s.comboItems.forEach { Text("${it.name} × ${it.quantity} · ${money(it.totalPrice)}") }
                    else if (s.comboQuantity > 0) Text("${s.comboName.orEmpty()} × ${s.comboQuantity}")
                    AmountRow("Tạm tính", s.subtotal)
                    AmountRow("Giảm giá${s.voucherCode?.let { " · $it" }.orEmpty()}", if (s.discount.toLong() > 0) "-${s.discount}" else "0")
                    HorizontalDivider()
                    AmountRow("Đã thanh toán", s.totalAmount)
                }
                selected.refund?.let { refund -> TicketPanel {
                    Text(if (selected.status == "REFUNDED") "Hoàn tiền thành công" else "Tiền hoàn đang được xử lý", fontWeight = FontWeight.Bold)
                    Text("Số tiền: ${money(refund.amount)}",fontWeight=FontWeight.Bold,fontSize=24.sp,color=C.Primary)
                    TicketDivider()
                    Text("✓ Đã tiếp nhận yêu cầu hoàn",fontWeight=FontWeight.SemiBold)
                    Text(if(refund.operationState=="SUCCEEDED") "✓ Đã chuyển tiền hoàn" else "◉ Đang xác minh / xử lý chuyển tiền",color=C.Muted)
                    refund.reason?.let { Text(it) }
                    Text("Mã yêu cầu: ${refund.id}", style = MaterialTheme.typography.bodySmall)
                    Text(when (refund.operationState) {
                        "NEEDS_REVIEW" -> "Cần xác minh thông tin trước khi chuyển tiền."
                        "UNKNOWN" -> "Đang xác minh kết quả chuyển tiền; không tạo yêu cầu mới."
                        "FAILED" -> "Chưa chuyển được tiền. Yêu cầu vẫn được giữ để bộ phận hỗ trợ xử lý."
                        "SUCCEEDED" -> "Ngân hàng đã xác nhận chuyển tiền thành công."
                        else -> "Máy chủ đang xử lý yêu cầu hoàn tiền."
                    }, style = MaterialTheme.typography.bodySmall)
                } }
                if (selected.status == "VALID") {
                    OutlinedButton({ confirmRefund = true }, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = selected.refundEligible && !state.busy && state.error.isBlank()) { Text("Yêu cầu hoàn vé") }
                    Text(selected.refundReason ?: "Hoàn toàn đơn khi còn ít nhất 45 phút trước suất, chưa dùng vé/chưa nhận combo.", color = C.Muted)
                }
            }
        }
    }
    if (confirmRefund && selected != null) AlertDialog(onDismissRequest = { confirmRefund = false }, title = { Text("Hoàn toàn bộ đơn vé?") },
        text = { Text("Số tiền ${money(selected.snapshot.totalAmount)} sẽ được hoàn về ${"nguồn thanh toán đã xác minh"}. Vé sẽ mất hiệu lực ngay khi yêu cầu được tiếp nhận. Tiền hoàn cần thời gian xử lý; không phải hoàn tức thì.") },
        confirmButton = { TextButton({ confirmRefund = false; state.refund() }) { Text("Xác nhận hoàn vé") } }, dismissButton = { TextButton({ confirmRefund = false }) { Text("Giữ vé") } })
}

@Composable private fun TicketHeading(snapshot: TicketSnapshot) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.width(88.dp).height(128.dp).clip(RoundedCornerShape(12.dp)).background(C.Raised), contentAlignment = Alignment.Center) {
            Text("N8", color = C.Primary, fontWeight = FontWeight.Bold)
            snapshot.posterUrl?.let { AsyncImage(it, "Poster ${snapshot.movieTitle}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        }
        Column(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("N8 CINEPLEX · VÉ ĐIỆN TỬ",fontSize=10.sp,color=C.Primary,fontWeight=FontWeight.Bold)
            Text(snapshot.movieTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
            Text(runCatching { "${vietnamTime(snapshot.startAt)} · ${displayCalendarDate(vietnamDate(java.util.Date(isoMillis(snapshot.startAt))))}" }.getOrDefault("Đang tải suất chiếu"))
            Text("${snapshot.format} · ${snapshot.auditorium}")
            Text("Ghế ${snapshot.seatCodes.ifEmpty { snapshot.seats.map { it.seatCode } }.joinToString()}")
        }
    }
}
@Composable private fun TicketPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = C.Surface, border=BorderStroke(1.dp,Color(0xFFECE4E7)),shadowElevation=1.dp) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }
}
@Composable private fun AmountRow(label: String, amount: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, Modifier.weight(1f)); Text(money(amount), fontWeight = FontWeight.Bold) }
}

@Composable private fun TicketStatusBadge(status:String) {
    val active=status=="VALID"
    val pending=status=="REFUND_PENDING"
    Surface(color=if(active)Color(0xFFEAF4EE) else if(pending)Color(0xFFFFF3DB) else Color(0xFFF0EDF0),shape=RoundedCornerShape(50)) {
        Text(ticketStatus(status),Modifier.padding(horizontal=12.dp,vertical=7.dp),color=if(active)Color(0xFF276849) else if(pending)Color(0xFF865300) else C.Muted,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
    }
}
@Composable private fun TicketDivider() {
    Canvas(Modifier.fillMaxWidth().height(10.dp)) {
        drawLine(Color(0xFFE2D5DC),Offset(0f,size.height/2),Offset(size.width,size.height/2),strokeWidth=1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(),4.dp.toPx())))
    }
}

@Composable private fun TransactionAutoRefresh(state: TicketsState) {
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle,state,state.paymentOrderId,state.selected?.id) {
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while(true) {
                state.refreshStatus()
                val terminalPayment=state.paymentOrderId!=null && (state.payment?.let {it.status=="PAID" && it.ticketId!=null || it.status in setOf("EXPIRED","NEEDS_ADMIN","RESOLVED")}==true)
                val terminalTicket=state.paymentOrderId==null && state.selected?.let {it.status !in setOf("VALID","REFUND_PENDING")}==true
                if(terminalPayment||terminalTicket) kotlinx.coroutines.awaitCancellation()
                delay(state.refreshDelayMillis)
            }
        }
    }
}
