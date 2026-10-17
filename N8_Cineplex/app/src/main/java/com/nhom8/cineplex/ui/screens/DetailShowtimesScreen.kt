package com.nhom8.cineplex.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
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
    val dates = DetailSchedule.dates(vm.todayDate)
    val slots = vm.sampleSlots
    val chosen = vm.selectedSampleSlot?.takeIf { selected -> slots.any { it.id == selected.id && !it.soldOut } }
    Scaffold(containerColor = C.Surface,topBar = { DetailHeader("Chọn lịch chiếu",vm::back) },bottomBar = {
        DetailActionBar {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(chosen?.let { "${it.time} · ${displayCalendarDate(it.date)} · ${it.cinema.name}" } ?: "Chưa chọn suất chiếu",fontWeight = FontWeight.SemiBold)
                Button(vm::continueSchedule,Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("continue-showtime"),enabled = chosen != null,shape = RoundedCornerShape(12.dp)) { Text("Tiếp tục") }
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
            Column(Modifier.fillMaxWidth().background(C.Background).padding(vertical = 18.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Chọn ngày xem",Modifier.padding(horizontal = 20.dp),fontWeight = FontWeight.SemiBold)
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp),horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dates) { date ->
                        val selected = vm.scheduleDate == date
                        val empty = date == dates[3]
                        Surface(onClick = { vm.chooseScheduleDate(date) },color = if(selected) C.Primary else C.Surface,shape = RoundedCornerShape(12.dp),border = BorderStroke(1.dp,if(selected) C.Primary else C.Line),modifier = Modifier.width(83.dp).heightIn(min = 84.dp).testTag("schedule-date-$date")) {
                            Column(Modifier.padding(8.dp),horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(calendarWeekday(date),fontSize = 12.sp,color = if(selected) androidx.compose.ui.graphics.Color.White else C.Muted)
                                Text(displayCalendarDate(date).take(5),fontWeight = FontWeight.Bold,color = if(selected) androidx.compose.ui.graphics.Color.White else C.Text)
                                Text(if(empty) "Chưa có" else "Có suất",fontSize = 11.sp,color = if(selected) androidx.compose.ui.graphics.Color.White else C.Muted)
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = C.Line)
            Row(Modifier.fillMaxWidth().padding(20.dp),horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ScheduleDropDown("Khu vực",DetailSchedule.areas.getValue(vm.scheduleArea),DetailSchedule.areas.toList(),vm::chooseScheduleArea,Modifier.weight(1f))
                val cinemas = listOf("all" to "Tất cả rạp") + DetailSchedule.cinemas.filter { it.area == vm.scheduleArea }.map { it.id to it.name.removePrefix("Cineplex ") }
                ScheduleDropDown("Rạp chiếu",cinemas.firstOrNull { it.first == vm.scheduleCinema }?.second ?: "Tất cả rạp",cinemas,vm::chooseScheduleCinema,Modifier.weight(1f))
            }
            HorizontalDivider(color = C.Line)
            if (slots.isEmpty()) Column(Modifier.fillMaxWidth().padding(40.dp),horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("▦",fontSize = 32.sp,color = C.Primary)
                Text("Chưa có suất chiếu trong ngày này",style = MaterialTheme.typography.headlineMedium,textAlign = TextAlign.Center)
                Text("Hãy chọn một ngày hoặc rạp khác.",color = C.Muted,textAlign = TextAlign.Center)
                TextButton({ dates.firstOrNull { it != vm.scheduleDate && it != dates[3] }?.let(vm::chooseScheduleDate) }) { Text("Chọn ngày có suất") }
            }
            else DetailSchedule.cinemas.filter { cinema -> slots.any { it.cinema.id == cinema.id } }.forEach { cinema ->
                Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⌖ ${cinema.name}",style = MaterialTheme.typography.titleMedium)
                    Text(cinema.address,color = C.Muted,fontSize = 14.sp)
                    slots.filter { it.cinema.id == cinema.id }.groupBy { it.format }.forEach { (format, group) ->
                        Text(format,fontWeight = FontWeight.SemiBold,fontSize = 14.sp)
                        group.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { slot ->
                                    val selected = chosen?.id == slot.id
                                    Surface(onClick = { vm.chooseSampleSlot(slot.id) },enabled = !slot.soldOut,color = if(selected) C.Raised else if(slot.soldOut) C.Background else C.Surface,shape = RoundedCornerShape(8.dp),border = BorderStroke(if(selected) 2.dp else 1.dp,if(selected) C.Primary else C.Line),modifier = Modifier.weight(1f).heightIn(min = 68.dp).testTag("schedule-slot-${slot.id}")) {
                                        Column(Modifier.padding(7.dp),horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.Center) {
                                            Text(slot.time,fontWeight = FontWeight.SemiBold,color = if(selected) C.Primary else if(slot.soldOut) C.Muted else C.Text)
                                            Text(if(slot.soldOut) "Hết vé" else if(selected) "✓ Đã chọn" else "Còn vé",fontSize = 11.sp,color = if(selected) C.Primary else C.Muted)
                                        }
                                    }
                                }
                                repeat(3-row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                HorizontalDivider(color = C.Line)
            }
            Text("Lịch chiếu và rạp đang là dữ liệu minh họa.",Modifier.padding(20.dp),fontSize = 12.sp,color = C.Muted)
        }
    }
    if (vm.ageSheet) AgeConfirmationSheet(vm,movie)
}

@Composable
private fun ScheduleDropDown(label: String, value: String, options: List<Pair<String,String>>, choose: (String) -> Unit, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier,verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label,fontSize = 13.sp,fontWeight = FontWeight.Medium)
        Box {
            Surface(onClick = { expanded = true },shape = RoundedCornerShape(12.dp),border = BorderStroke(1.dp,C.Line),color = C.Surface,modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                Row(Modifier.padding(horizontal = 10.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.SpaceBetween) { Text(value,maxLines = 1,fontSize = 13.sp); Text("⌄",color = C.Primary) }
            }
            DropdownMenu(expanded,onDismissRequest = { expanded = false }) { options.forEach { (id,name) -> DropdownMenuItem(text = { Text(name) },onClick = { choose(id); expanded = false }) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgeConfirmationSheet(vm: CineplexViewModel, movie: Movie) {
    val age = DetailSchedule.ageLimit(movie)
    val selected = vm.selectedSampleSlot
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
                selected?.let { Text("${movie.name}\n${it.time} · ${displayCalendarDate(it.date)} · ${it.cinema.name}",Modifier.fillMaxWidth().background(C.Background,RoundedCornerShape(12.dp)).padding(16.dp)) }
                ActionButton(if(age != null) "Tôi từ đủ $age tuổi — Tiếp tục" else "Tôi đáp ứng phân loại tuổi — Tiếp tục",vm::confirmAge,Modifier.fillMaxWidth())
                ActionButton("Quay lại",vm::closeAgeSheet,Modifier.fillMaxWidth(),secondary = true)
                if(age != null) TextButton(vm::showUnderAge,Modifier.align(Alignment.CenterHorizontally)) { Text("Tôi chưa đủ $age tuổi") }
                Text("Xác nhận này chỉ minh họa luồng giao diện; chưa xác minh độ tuổi thực tế.",fontSize = 12.sp,color = C.Muted)
            }
        }
    }
}

@Composable
fun SeatSummaryScreen(vm: CineplexViewModel, movie: Movie) {
    val slot = vm.selectedSampleSlot
    Scaffold(containerColor = C.Surface,topBar = { DetailHeader("Tóm tắt suất chiếu",vm::back) },bottomBar = { DetailActionBar { ActionButton("Đổi suất chiếu",vm::back,Modifier.fillMaxWidth(),secondary = true) } }) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement = Arrangement.spacedBy(22.dp),horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✓",fontSize = 36.sp,color = C.Primary)
            Text("Sẵn sàng chọn ghế",style = MaterialTheme.typography.headlineMedium)
            Text("Bạn đã xác nhận điều kiện độ tuổi. Đây là thông tin suất chiếu đã chọn.",color = C.Muted,textAlign = TextAlign.Center)
            Surface(color = C.Background,shape = RoundedCornerShape(16.dp),border = BorderStroke(1.dp,C.Line),modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(movie.name,style = MaterialTheme.typography.titleMedium)
                    DetailAgeBadge(movie.age)
                    Text("Rạp chiếu",color = C.Muted)
                    Text(slot?.cinema?.name.orEmpty(),fontWeight = FontWeight.SemiBold)
                    Text(slot?.cinema?.address.orEmpty(),color = C.Muted)
                    Text("Suất chiếu",color = C.Muted)
                    Text(slot?.let { "${it.time} · ${displayCalendarDate(it.date)}" }.orEmpty(),fontWeight = FontWeight.SemiBold)
                    Text(slot?.format.orEmpty())
                }
            }
            Text("Chức năng chọn ghế sẽ có trong phiên bản sau.",color = C.Muted,textAlign = TextAlign.Center)
        }
    }
}
