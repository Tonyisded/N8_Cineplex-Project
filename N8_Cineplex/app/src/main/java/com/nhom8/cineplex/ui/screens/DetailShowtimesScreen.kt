package com.nhom8.cineplex.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.Movie
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.components.*
import com.nhom8.cineplex.ui.theme.CineplexColors as C

@Composable
fun DetailShowtimesScreen(vm: CineplexViewModel, movie: Movie) {
    val slots = vm.scheduleSlots
    val chosen = vm.selectedShowtime?.takeIf { selected -> slots.any { it.id == selected.id && it.totalSeats > 0 && it.availableSeats > 0 } }
    Scaffold(containerColor = C.Surface,topBar = { DetailHeader("Chọn lịch chiếu",vm::back) },bottomBar = {
        DetailActionBar {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(chosen?.let { "${showtimeRange(it)} · ${displayCalendarDate(vm.catalogDate)} · ${it.auditorium.name}" } ?: "Chưa chọn suất chiếu",fontWeight = FontWeight.SemiBold)
                Button(vm::continueSchedule,Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("continue-showtime"),enabled = chosen != null && !vm.detailLoading && vm.detailError.isEmpty() && !vm.scheduleChecking,shape = RoundedCornerShape(12.dp)) { Text(if (vm.scheduleChecking) "Đang kiểm tra…" else "Tiếp tục") }
            }
        }
    }) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState()).testTag("showtimes-screen")) {
            Row(Modifier.fillMaxWidth().padding(20.dp),horizontalArrangement = Arrangement.spacedBy(14.dp),verticalAlignment = Alignment.CenterVertically) {
                MoviePoster(movie,Modifier.width(56.dp))
                Column(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(movie.name,style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),verticalAlignment = Alignment.CenterVertically) { DetailAgeBadge(movie.age); Text("${movie.minutes} phút",color = C.Muted,fontSize = 14.sp) }
                }
            }
            HorizontalDivider(color = C.Line)
            Column(Modifier.fillMaxWidth().background(C.Background).padding(20.dp)) { DateSelector(vm) }
            HorizontalDivider(color = C.Line)
            Row(Modifier.fillMaxWidth().padding(20.dp),horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(7.dp)) { Text("Khu vực",fontSize = 13.sp,fontWeight = FontWeight.Medium); Text("TP. Hồ Chí Minh",fontSize = 14.sp) }
                Column(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(7.dp)) { Text("Rạp chiếu",fontSize = 13.sp,fontWeight = FontWeight.Medium); Text("Landmark 81",fontSize = 14.sp) }
            }
            HorizontalDivider(color = C.Line)
            when {
                vm.detailLoading -> Box(Modifier.fillMaxWidth().padding(40.dp),contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.testTag("schedule-loading")) }
                vm.detailError.isNotEmpty() -> Column(Modifier.fillMaxWidth().padding(24.dp),horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(vm.detailError,color = C.Error,textAlign = TextAlign.Center)
                    TextButton(vm::refreshMovie,Modifier.testTag("schedule-retry")) { Text("Thử lại") }
                }
                slots.isEmpty() -> Column(Modifier.fillMaxWidth().padding(40.dp),horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("▦",fontSize = 32.sp,color = C.Primary)
                    Text("Chưa có suất chiếu trong ngày này",style = MaterialTheme.typography.headlineMedium,textAlign = TextAlign.Center)
                    Text("Hãy chọn một ngày khác.",color = C.Muted,textAlign = TextAlign.Center)
                }
                else -> slots.groupBy { it.format to it.presentationLabel.orEmpty() }.values.sortedBy { it.minOf { slot -> slot.startAt } }.forEach { group ->
                    Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(scheduleFormat(group.first()),fontWeight = FontWeight.SemiBold,fontSize = 14.sp)
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val columns = ((maxWidth + 8.dp) / (96.dp * LocalDensity.current.fontScale + 8.dp)).toInt().coerceIn(1,3)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                group.chunked(columns).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        row.forEach { slot ->
                                            val selected = chosen?.id == slot.id
                                            val available = slot.totalSeats > 0 && slot.availableSeats > 0
                                            Surface(onClick = { vm.chooseShowtime(slot.id) },enabled = available && !vm.scheduleChecking,color = if(selected) C.Raised else if(!available) C.Background else C.Surface,shape = RoundedCornerShape(8.dp),border = BorderStroke(if(selected) 2.dp else 1.dp,if(selected) C.Primary else C.Line),modifier = Modifier.weight(1f).heightIn(min = 104.dp).testTag("schedule-slot-${slot.id}").semantics { contentDescription = "Suất ${showtimeRange(slot)}, ${slot.auditorium.name}, ${seatSummary(slot)}" }) {
                                                Column(Modifier.padding(7.dp),horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.Center) {
                                                    Text(showtimeRange(slot),fontWeight = FontWeight.SemiBold,color = if(selected) C.Primary else if(!available) C.Muted else C.Text,fontSize = 13.sp)
                                                    Text(slot.auditorium.name,fontSize = 12.sp,color = C.Muted)
                                                    Text(if(slot.totalSeats == 0) "Chưa có dữ liệu ghế" else if(!available) "Hết vé" else if(selected) "✓ Đã chọn" else seatSummary(slot),fontSize = 11.sp,color = if(selected) C.Primary else C.Muted)
                                                }
                                            }
                                        }
                                        repeat(columns-row.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = C.Line)
                }
            }
        }
    }
    if (vm.ageSheet) AgeConfirmationSheet(vm,movie)
}

private fun scheduleFormat(slot: ShowtimeDto): String {
    val format = when (slot.format) {
        "STANDARD_2D" -> "2D"
        "STANDARD_3D" -> "3D"
        "IMAX_2D" -> "IMAX 2D"
        "IMAX_3D" -> "IMAX 3D"
        "SCREENX" -> "SCREENX"
        "GOLD_CLASS" -> "GOLD CLASS"
        else -> slot.format
    }
    return format + slot.presentationLabel?.let { " · $it" }.orEmpty()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgeConfirmationSheet(vm: CineplexViewModel, movie: Movie) {
    val age = DetailSchedule.ageLimit(movie)
    val selected = vm.selectedShowtime
    ModalBottomSheet(onDismissRequest = vm::closeAgeSheet,containerColor = C.Surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(color = C.Raised,shape = RoundedCornerShape(16.dp)) { Text(if(vm.underAgeSheet) "!" else movie.age,Modifier.padding(16.dp),color = C.Primary,fontWeight = FontWeight.Bold,fontSize = 23.sp) }
            if (vm.underAgeSheet) {
                Text("Bạn chưa đủ tuổi xem phim này",style = MaterialTheme.typography.headlineMedium)
                Text("Phim ${movie.age} dành cho khán giả từ đủ $age tuổi. Hãy chọn một bộ phim phù hợp với độ tuổi của bạn.",color = C.Muted)
                ActionButton("Chọn phim khác",vm::chooseAnotherMovie,Modifier.fillMaxWidth())
                ActionButton("Quay lại",vm::closeAgeSheet,Modifier.fillMaxWidth(),secondary = true)
            } else {
                Text(if(age != null) "Phim dành cho khán giả từ đủ $age tuổi" else "Lưu ý phân loại độ tuổi ${movie.age}",style = MaterialTheme.typography.headlineMedium)
                Text("Rạp có thể yêu cầu giấy tờ xác minh độ tuổi khi soát vé.",color = C.Muted)
                selected?.let { Text("${movie.name}\n${showtimeRange(it)} · ${displayCalendarDate(vm.catalogDate)} · ${it.auditorium.name}",Modifier.fillMaxWidth().background(C.Background,RoundedCornerShape(12.dp)).padding(16.dp)) }
                ActionButton(if(age != null) "Tôi từ đủ $age tuổi — Tiếp tục" else "Tôi đáp ứng phân loại tuổi — Tiếp tục",vm::confirmAge,Modifier.fillMaxWidth(),loading = vm.scheduleChecking)
                ActionButton("Quay lại",vm::closeAgeSheet,Modifier.fillMaxWidth(),secondary = true)
                if(age != null) TextButton(vm::showUnderAge,Modifier.align(Alignment.CenterHorizontally)) { Text("Tôi chưa đủ $age tuổi") }
                Text("Đây là xác nhận tự khai; rạp có thể kiểm tra giấy tờ khi soát vé.",fontSize = 12.sp,color = C.Muted)
            }
        }
    }
}

@Composable
fun SeatSummaryScreen(vm: CineplexViewModel, movie: Movie) {
    val slot = vm.selectedShowtime
    Scaffold(containerColor = C.Surface,topBar = { DetailHeader("Tóm tắt suất chiếu",vm::back) },bottomBar = { DetailActionBar { ActionButton("Đổi suất chiếu",vm::back,Modifier.fillMaxWidth(),secondary = true) } }) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement = Arrangement.spacedBy(22.dp),horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✓",fontSize = 36.sp,color = C.Primary)
            Text("Thông tin suất chiếu",style = MaterialTheme.typography.headlineMedium)
            Text("Đây là thông tin suất chiếu đã chọn. Chưa giữ chỗ hoặc tạo đơn.",color = C.Muted,textAlign = TextAlign.Center)
            Surface(color = C.Background,shape = RoundedCornerShape(16.dp),border = BorderStroke(1.dp,C.Line),modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(movie.name,style = MaterialTheme.typography.titleMedium)
                    DetailAgeBadge(movie.age)
                    Text("Rạp chiếu",color = C.Muted)
                    Text("Landmark 81 · ${slot?.auditorium?.name.orEmpty()}",fontWeight = FontWeight.SemiBold)
                    Text("Phòng và ghế mô phỏng N8, không phải sơ đồ ghế CGV.",color = C.Muted)
                    Text("Suất chiếu",color = C.Muted)
                    Text(slot?.let { "${showtimeRange(it)} · ${displayCalendarDate(vm.catalogDate)}" }.orEmpty(),fontWeight = FontWeight.SemiBold)
                    Text(slot?.let(::scheduleFormat).orEmpty())
                    Text(slot?.let { "Giá vé cơ sở: ${it.basePrice} VND" }.orEmpty())
                }
            }
            Text("Chức năng chọn ghế sẽ có trong phiên bản sau.",color = C.Muted,textAlign = TextAlign.Center)
        }
    }
}
