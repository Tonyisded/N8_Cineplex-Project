package com.nhom8.cineplex.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.Movie
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.SeatSelectionViewModel
import com.nhom8.cineplex.ui.components.MoviePoster
import com.nhom8.cineplex.ui.theme.CineplexColors as C
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private fun vnd(value: Long) = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value) + "đ"
private fun clockText(seconds: Int) = "%02d:%02d".format(Locale.ROOT, seconds / 60, seconds % 60)

@Composable
fun SeatSelectionScreen(vm: CineplexViewModel, state: SeatSelectionViewModel, movie: Movie, onBack: () -> Unit) {
    val slot = vm.selectedShowtime ?: return
    var zoom by remember(slot.id) { mutableFloatStateOf(1f) }
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    val scope = rememberCoroutineScope()
    Scaffold(containerColor = C.Surface,
        topBar = { SeatHeader("Chọn ghế", onBack) },
        bottomBar = {
            Surface(color = C.Surface, shadowElevation = 12.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.chosen.isEmpty()) Text("Chưa chọn ghế", color = C.Muted)
                        state.chosen.forEach { seat ->
                            Surface(color = C.Background, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, C.Line)) {
                                Text("${seat.kind.label} ${seat.label} · ${seat.places} chỗ · ${vnd(seat.price)}${if (seat.id == state.pending) " · Đang xử lý" else ""}",
                                    Modifier.padding(horizontal = 10.dp, vertical = 7.dp), fontSize = 12.sp)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Tổng tiền · ${state.places} chỗ", color = C.Muted, fontSize = 12.sp)
                            Text(vnd(state.total), color = C.Primary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("seat-total"))
                        }
                        Button({ state.continueToSummary(vm::seatHandoff) }, enabled = state.canContinue,
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.heightIn(min = 48.dp).testTag("seat-continue")) {
                            Text(if (state.confirming) "Đang kiểm tra…" else "Tiếp tục →")
                        }
                    }
                }
            }
        }) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState()).testTag("seat-selection-screen")) {
            BookingContext(movie, slot)
            Column(Modifier.fillMaxWidth().background(C.Background).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("MÔ PHỎNG CHỌN GHẾ", color = C.Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Sơ đồ, giá và lựa chọn chỉ minh họa trên thiết bị. Chưa giữ ghế trên máy chủ hoặc đồng bộ giữa người dùng.", color = C.Muted, fontSize = 12.sp)
                when {
                    !state.online -> Text("● Mất kết nối mô phỏng · Tạm khóa chọn ghế và tiếp tục", color = C.Error, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    state.syncing -> Text("● Đang đồng bộ ghế mô phỏng…", color = C.Primary, fontSize = 12.sp)
                    else -> Text("● Trạng thái ghế mô phỏng sẵn sàng", color = C.Success, fontSize = 12.sp)
                }
            }
            HoldTime(state)
            Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Chọn chỗ bạn thích", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton({ zoom = (zoom - .25f).coerceAtLeast(1f) }, enabled = zoom > 1f) { Text("−", fontSize = 20.sp) }
                    Text("${(zoom * 100).toInt()}%", fontSize = 12.sp)
                    TextButton({ zoom = (zoom + .25f).coerceAtMost(2f) }, enabled = zoom < 2f) { Text("+", fontSize = 20.sp) }
                    TextButton({ zoom = 1f; scope.launch { horizontal.scrollTo(0); vertical.scrollTo(0) } }) { Text("↺", fontSize = 20.sp) }
                }
                Text("Vuốt sơ đồ để xem các hàng ghế. Phóng to từ 100% đến 200%.", color = C.Muted, fontSize = 12.sp)
                Box(Modifier.fillMaxWidth().height(410.dp).clip(RoundedCornerShape(14.dp)).background(C.Background)
                    .horizontalScroll(horizontal).verticalScroll(vertical).testTag("seat-map")) {
                    Box(Modifier.width((356 * zoom).dp).height((682 * zoom).dp)) {
                        Column(Modifier.width(356.dp).graphicsLayer {
                            scaleX = zoom; scaleY = zoom; transformOrigin = TransformOrigin(0f, 0f)
                        }.padding(horizontal = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MÀN HÌNH", Modifier.fillMaxWidth().padding(top = 13.dp, bottom = 10.dp)
                                .background(C.Raised, RoundedCornerShape(topStart = 90.dp, topEnd = 90.dp)).padding(10.dp),
                                textAlign = TextAlign.Center, color = C.Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Spacer(Modifier.width(20.dp))
                                (1..6).forEachIndexed { index, number ->
                                    if (index == 3) Spacer(Modifier.width(14.dp))
                                    Text("$number", Modifier.width(44.dp), textAlign = TextAlign.Center, color = C.Muted, fontSize = 11.sp)
                                }
                            }
                            ('A'..'J').forEach { row ->
                                val rowSeats = state.seats.filter { it.row == row }
                                if (row == 'E' || row == 'I') Spacer(Modifier.height(12.dp))
                                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("$row", Modifier.width(20.dp), color = C.Muted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 12.sp)
                                    rowSeats.forEachIndexed { index, seat ->
                                        if (index == 3 && seat.kind != SeatKind.COUPLE) Spacer(Modifier.width(14.dp))
                                        SeatCell(seat, seat.id in state.selected, seat.id == state.pending, !state.online || state.syncing) { state.select(seat.id) }
                                    }
                                }
                            }
                        }
                    }
                }
                Text("Lối đi giữa ghế số 3 và 4 · Hàng I–J là ghế Couple", color = C.Muted, fontSize = 12.sp)
                SeatLegend()
                PriceLegend()
                if (state.message.isNotBlank()) Text(state.message, color = C.Primary, fontSize = 12.sp, modifier = Modifier.testTag("seat-message"))
            }
        }
    }
    if (state.leaveDialog) AlertDialog(onDismissRequest = state::stay, title = { Text("Rời màn hình chọn ghế?") },
        text = { Text("Các ghế đang chọn trong bản mô phỏng sẽ được giải phóng.") },
        confirmButton = { TextButton({ state.release(); vm.back() }) { Text("Rời màn hình") } },
        dismissButton = { TextButton(state::stay) { Text("Ở lại") } })
    if (state.expiredDialog) AlertDialog(onDismissRequest = {}, title = { Text("Đã hết thời gian giữ ghế mô phỏng") },
        text = { Text("Lựa chọn đã được xóa. Vui lòng chọn lại ghế để tiếp tục.") },
        confirmButton = { TextButton(state::dismissExpired) { Text("Chọn lại ghế") } },
        dismissButton = { TextButton({ state.release(); vm.back() }) { Text(if (vm.seatReturnScreen == com.nhom8.cineplex.model.Screen.HOME) "Về trang chủ" else "Về lịch chiếu") } })
}

@Composable
fun SeatHandoffScreen(vm: CineplexViewModel, state: SeatSelectionViewModel, movie: Movie) {
    val slot = vm.selectedShowtime ?: return
    LaunchedEffect(state.expiredDialog) { if (state.expiredDialog) vm.back() }
    Scaffold(containerColor = C.Surface, topBar = { SeatHeader("Tóm tắt lựa chọn", vm::back) }, bottomBar = {
        Surface(color = C.Surface, shadowElevation = 10.dp) {
            Button(vm::back, Modifier.fillMaxWidth().padding(20.dp).heightIn(min = 48.dp)) { Text("Quay lại chọn ghế") }
        }
    }) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState()).padding(bottom = 20.dp)) {
            BookingContext(movie, slot)
            HoldTime(state)
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Ghế đã chọn", style = MaterialTheme.typography.headlineMedium)
                state.chosen.forEach { seat ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${seat.kind.label} ${seat.label} · ${seat.places} chỗ")
                        Text(vnd(seat.price), fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider(color = C.Line)
                Text("${state.places} chỗ · ${vnd(state.total)}", color = C.Primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Điểm chuyển tiếp mô phỏng. Chưa tạo đơn, giữ ghế thật hoặc thanh toán. Thời hạn phía trên tiếp tục đếm khi ở màn này.", color = C.Muted)
            }
        }
    }
}

@Composable
private fun SeatHeader(title: String, back: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(C.Surface).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(back, modifier = Modifier.semantics { contentDescription = "Quay lại" }) { Text("‹", fontSize = 28.sp, color = C.Primary) }
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun BookingContext(movie: Movie, slot: ShowtimeDto) {
    Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        MoviePoster(movie, Modifier.width(62.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Thông tin suất chiếu", color = C.Primary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(movie.name, style = MaterialTheme.typography.titleMedium)
            Text(movie.age, color = C.Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("Landmark 81 · ${slot.auditorium.name}", color = C.Muted, fontSize = 12.sp)
            Text("${vietnamTime(slot.startAt)} · ${displayCalendarDate(vietnamDate(Date(isoMillis(slot.startAt))))}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("${slot.format.replace('_', ' ')}${slot.presentationLabel?.let { " · $it" }.orEmpty()}", color = C.Muted, fontSize = 12.sp)
            Text("Mã suất: ${slot.id}", color = C.Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun HoldTime(state: SeatSelectionViewModel) {
    val active = state.deadlineMillis != null
    val urgent = active && state.secondsLeft < 120
    Text(if (!active) "Thời gian 10:00 bắt đầu sau lần chọn thành công đầu tiên (mô phỏng)"
        else if (urgent) "⚠ Thời gian giữ ghế mô phỏng sắp hết · ${clockText(state.secondsLeft)}"
        else "Thời gian còn lại · ${clockText(state.secondsLeft)}",
        Modifier.fillMaxWidth().background(if (urgent) Color(0xFFFFE8DC) else C.Raised).padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("seat-countdown"), color = if (urgent) C.Error else C.Primary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
}

@Composable
private fun SeatCell(seat: DemoSeat, selected: Boolean, pending: Boolean, locked: Boolean, onClick: () -> Unit) {
    val status = when { pending -> "Đang xử lý"; selected -> "Đang chọn"; seat.availability == SeatAvailability.HELD -> "Người khác giữ";
        seat.availability == SeatAvailability.SOLD -> "Đã bán"; else -> "Còn trống" }
    val color = when { pending -> Color(0xFFFFE5B8); selected -> C.Primary; seat.availability == SeatAvailability.HELD -> C.Raised;
        seat.availability == SeatAvailability.SOLD -> Color(0xFFD5CED2); else -> C.Surface }
    val ink = if (selected) Color.White else C.Text
    Surface(onClick = onClick, color = color, shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (seat.kind == SeatKind.VIP) 2.dp else 1.dp, if (selected) C.Primary else C.Line),
        modifier = Modifier.width(if (seat.kind == SeatKind.COUPLE) 96.dp else 44.dp).height(44.dp)
            .graphicsLayer { alpha = if (locked) .6f else 1f }
            .testTag("seat-${seat.id}").semantics {
                contentDescription = "${seat.kind.label} ${seat.label}, ${seat.places} chỗ, ${vnd(seat.price)}${if (seat.places == 2) " mỗi cặp" else ""}, $status"
            }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(if (seat.kind == SeatKind.COUPLE) seat.codes.joinToString("  ") else seat.id, color = ink, fontSize = if (seat.kind == SeatKind.COUPLE) 12.sp else 11.sp, fontWeight = FontWeight.Bold)
            Text(when { pending -> "…"; selected -> "✓"; seat.availability == SeatAvailability.HELD -> "⌕";
                seat.availability == SeatAvailability.SOLD -> "×"; seat.kind == SeatKind.VIP -> "V"; seat.kind == SeatKind.COUPLE -> "2 chỗ"; else -> "+" },
                color = ink, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SeatLegend() {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("Chú thích ghế", fontWeight = FontWeight.Bold)
        listOf("+  Còn trống", "✓  Đang chọn", "…  Đang xử lý", "⌕  Người khác giữ", "×  Đã bán", "V  VIP", "2 chỗ  Couple").chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { Text(it, Modifier.weight(1f), fontSize = 12.sp, color = C.Muted) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PriceLegend() {
    Column(Modifier.fillMaxWidth().background(C.Background, RoundedCornerShape(12.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Giá vé mô phỏng", fontWeight = FontWeight.Bold)
        Text("Thường 90.000đ/chỗ · VIP 120.000đ/chỗ", fontSize = 12.sp)
        Text("Couple 240.000đ/cặp · gồm 2 chỗ", fontSize = 12.sp)
    }
}
