package com.nhom8.cineplex.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nhom8.cineplex.R
import com.nhom8.cineplex.data.ServerCombo
import com.nhom8.cineplex.ui.*
import java.text.NumberFormat
import java.util.Locale

private val commerceAccent = Color(0xFF8C2945)
private val commerceMuted = Color(0xFF5D6470)
private fun vnd(value: Long) = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value) + "đ"

// Presentation only: prices and sale availability always come from the server.
private fun comboPicture(item: ServerCombo) = when {
    item.name.startsWith("Bắp", ignoreCase = true) -> R.drawable.combo_popcorn
    item.name.startsWith("Nước", ignoreCase = true) -> R.drawable.combo_drink
    else -> R.drawable.combo_two
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveCommerceScreen(vm: CineplexViewModel, seats: SeatSelectionViewModel, state: LiveCommerceState) {
    LaunchedEffect(seats.order?.id) { state.bind(seats.order) }
    LaunchedEffect(seats.order?.status) { state.wallet() }
    val active = seats.held && seats.secondsLeft > 0 && seats.online
    val choosing = state.step == LiveCommerceStep.COMBOS
    val keyboard = LocalSoftwareKeyboardController.current
    var replacement by remember { mutableStateOf<ServerCombo?>(null) }
    BackHandler(!choosing) { state.backToCombos() }
    Scaffold(
        containerColor = Color(0xFFF6F7F9),
        topBar = {
            TopAppBar(
                title = { Text(if (choosing) "Bắp nước & Combo" else "Kiểm tra đơn", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                navigationIcon = {
                    TextButton({ if (choosing) vm.back() else state.backToCombos() }, enabled = !state.busy && !state.unknown, modifier = Modifier.semantics { contentDescription = if (choosing) "Quay lại chọn ghế" else "Quay lại bắp nước" }) { Text("‹", fontSize = 30.sp) }
                },
                actions = {
                    Surface(color = Color(0xFFF5E8EC), shape = RoundedCornerShape(24.dp), modifier = Modifier.padding(end = 12.dp)) {
                        Text("Giữ ghế %02d:%02d".format(seats.secondsLeft / 60, seats.secondsLeft % 60), Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = commerceAccent, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 12.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(if (choosing) "Tạm tính" else "Còn thanh toán", color = commerceMuted)
                            if (choosing) Text("Vé + bắp nước · chưa giảm giá", fontSize = 11.sp, color = commerceMuted)
                        }
                        Text(
                            if (choosing) vnd(state.estimatedSubtotal) else state.quote?.bankAmount?.toLong()?.let(::vnd) ?: "—",
                            Modifier.testTag("commerce-total"), fontSize = 25.sp, fontWeight = FontWeight.Bold, color = commerceAccent
                        )
                    }
                    Button(
                        onClick = {
                            when {
                                choosing -> state.continueToCheckout()
                                state.quote == null -> state.price()
                                else -> state.confirm(seats)
                            }
                        },
                        enabled = active && !state.busy && (choosing || !state.confirmed) && (!state.unknown || !choosing),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("commerce-next"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = commerceAccent)
                    ) {
                        if (state.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        else Text(when { choosing -> "Tiếp tục"; state.confirmed -> "Đã xác nhận đơn"; state.unknown -> "Kiểm tra lại đơn"; state.quote == null -> "Thử lại"; else -> "Xác nhận đơn" }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(if (choosing) "BƯỚC 2 / 3 · BẮP NƯỚC" else "BƯỚC 3 / 3 · CHECKOUT", color = commerceAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (!active) Text("Phiên giữ ghế đã hết hạn hoặc đang mất kết nối. Vui lòng kiểm tra lại.", color = MaterialTheme.colorScheme.error)
            if (state.message.isNotBlank()) Text(state.message, color = MaterialTheme.colorScheme.error)
            if (choosing) {
                Text("Thêm chút ngon, trọn buổi phim", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("Chọn số lượng ngay trên món bạn thích. Mỗi đơn chọn một loại; đổi món sẽ thay lựa chọn cũ.", color = commerceMuted, fontSize = 14.sp)
                if (state.items.isEmpty()) {
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    else TextButton(state::load) { Text("Tải lại danh sách bắp nước") }
                }
                state.items.sortedWith(compareBy<ServerCombo> { if (it.name.startsWith("Combo", true)) 0 else 1 }.thenBy { it.name }).forEach { item ->
                    ComboProductCard(
                        item = item,
                        count = if (state.selected == item.id) state.quantity else 0,
                        enabled = active && !state.busy && !state.unknown,
                        decrease = { state.quantity(state.quantity - 1) },
                        increase = {
                            when {
                                state.selected == item.id -> state.quantity(state.quantity + 1)
                                state.selected == null -> state.select(item.id)
                                else -> replacement = item
                            }
                        }
                    )
                }
                Text("Ảnh minh hoạ · Giá mỗi phần đã gồm lựa chọn vị/cỡ.", color = commerceMuted, fontSize = 12.sp)
                TextButton({ state.select(null) }, enabled = state.selected != null && !state.busy && !state.unknown) { Text("Bỏ bắp nước đã chọn") }
            } else {
                Text("Kiểm tra trước khi xác nhận", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                CheckoutCard {
                    Text("Đơn vé của bạn", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Ghế ${seats.order?.seats?.joinToString { it.seatCode }.orEmpty()}", color = commerceMuted)
                    CommerceAmount("Tiền vé", vnd(state.seatSubtotal))
                    state.chosen?.let { item ->
                        Text("${item.name} × ${state.quantity}", color = commerceMuted)
                        CommerceAmount("Bắp nước", state.quote?.subtotal?.toLong()?.minus(state.seatSubtotal)?.let(::vnd) ?: "—")
                    }
                    TextButton({ state.backToCombos() }, enabled = !state.busy && !state.unknown) { Text("Đổi bắp nước") }
                }
                CheckoutCard {
                    Text("Voucher của bạn", fontWeight = FontWeight.Bold)
                    VoucherPicker(state.vouchers, state.promo, state.quote?.subtotal?.toLong() ?: state.estimatedSubtotal,
                        active && !state.busy && !state.unknown && !state.confirmed) { code ->
                        state.promotion(code); state.price()
                    }

                }
                CheckoutCard {
                    Text("Chi tiết thanh toán", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    if (state.busy && state.quote == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                    state.quote?.let { q ->
                        CommerceAmount("Tạm tính", vnd(q.subtotal.toLong()))
                        CommerceAmount("Ưu đãi${if (q.promotionCode.isBlank()) "" else " · ${q.promotionCode}"}", vnd(-q.discount.toLong()))
                        CommerceAmount("Tổng sau giảm", vnd(q.totalAmount.toLong()))
                        HorizontalDivider(color = Color(0xFFE9E4E6))
                        CommerceAmount("Còn thanh toán", vnd(q.bankAmount.toLong()), emphasized = true)
                    } ?: Text("Đang chờ giá được kiểm tra. Chưa thể xác nhận đơn.", color = commerceMuted)
                }
                if (state.confirmed && active) Text(
                    if (state.quote?.bankAmount?.toLong() == 0L)
                        "Đã xác nhận đơn — ngân hàng 0đ. Đơn vẫn chờ xử lý, chưa có vé; không cần chuyển khoản ở bước này."
                    else "Đã xác nhận đơn — chờ thanh toán. Chưa có vé.",
                    color = commerceAccent, modifier = Modifier.testTag("commerce-confirmation")
                )
                Text("Bước này chỉ xác nhận đơn, chưa thực hiện thanh toán hoặc phát vé.", color = commerceMuted, fontSize = 12.sp)
                if (state.unknown) TextButton({ seats.refresh(); state.wallet() }) { Text("Đọc lại trạng thái đơn") }
            }
            TextButton({ seats.cancelHold { state.wallet(); vm.back() } }, enabled = active && !state.busy) { Text("Hủy giữ ghế", color = commerceMuted) }
        }
    }
    replacement?.let { item ->
        AlertDialog(
            onDismissRequest = { replacement = null },
            title = { Text("Đổi món đã chọn?") },
            text = { Text("Mỗi đơn chọn một loại bắp nước. Thay ${state.chosen?.name.orEmpty()} bằng ${item.name}?") },
            confirmButton = { TextButton({ state.select(item.id); replacement = null }) { Text("Đổi món") } },
            dismissButton = { TextButton({ replacement = null }) { Text("Giữ món cũ") } }
        )
    }
}

@Composable
private fun ComboProductCard(item: ServerCombo, count: Int, enabled: Boolean, decrease: () -> Unit, increase: () -> Unit) {
    val available = item.status == "AVAILABLE"
    val picture = painterResource(comboPicture(item))
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("combo-card-${item.id}"),
        color = Color.White, shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (count > 0) 1.5.dp else 1.dp, if (count > 0) commerceAccent else Color(0xFFE9E4E6))
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.imageUrl, placeholder = picture, error = picture, fallback = picture,
                contentDescription = "Ảnh minh hoạ ${item.name}", contentScale = ContentScale.Crop,
                modifier = Modifier.size(104.dp).clip(RoundedCornerShape(14.dp)).testTag("combo-image-${item.id}")
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(item.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(vnd(item.price.toLong()), fontWeight = FontWeight.Bold, color = commerceAccent, fontSize = 19.sp)
                if (!available) Text("Tạm hết hàng", color = commerceMuted, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedIconButton(
                        onClick = decrease, enabled = enabled && count > 0,
                        modifier = Modifier.size(48.dp).testTag("combo-minus-${item.id}").semantics { contentDescription = "Giảm số lượng ${item.name}" }, shape = CircleShape
                    ) { Text("−", fontSize = 24.sp) }
                    Text(count.toString(), Modifier.widthIn(min = 32.dp).testTag("combo-count-${item.id}"), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    FilledTonalIconButton(
                        onClick = increase, enabled = enabled && available && count < 9,
                        modifier = Modifier.size(48.dp).testTag("combo-plus-${item.id}").semantics { contentDescription = "Tăng số lượng ${item.name}" }, shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color(0xFFF5E8EC), contentColor = commerceAccent)
                    ) { Text("+", fontSize = 24.sp) }
                }
            }
        }
    }
}

@Composable
private fun CheckoutCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun CommerceAmount(label: String, value: String, emphasized: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = if (emphasized) commerceAccent else commerceMuted, fontSize = if (emphasized) 16.sp else 14.sp)
        Text(value, fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium, color = if (emphasized) commerceAccent else Color(0xFF20222A), fontSize = if (emphasized) 20.sp else 15.sp)
    }
}
