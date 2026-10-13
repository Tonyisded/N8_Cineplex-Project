package com.nhom8.cineplex.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.ui.components.*
import com.nhom8.cineplex.ui.theme.CineplexColors as C
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun HomeScreen(vm: CineplexViewModel, grid: LazyListState) {
    val scope = rememberCoroutineScope()
    val filtered = vm.filteredMovies
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().imePadding()) {
            LazyColumn(state = grid,modifier = Modifier.weight(1f).testTag("movie-schedules"),contentPadding = PaddingValues(24.dp),verticalArrangement = Arrangement.spacedBy(24.dp)) {
                item(key = "header") {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.SpaceBetween) { CineplexLogo(); Avatar(vm::account, session = vm.session) }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Muted("Xin chào, ${vm.session?.name.orEmpty()}")
                            Text("Hôm nay, bạn muốn xem gì?",style = MaterialTheme.typography.headlineMedium)
                        }
                        SearchField(vm.query,{ vm.query = it })
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            PrototypeTabs(listOf("Đang chiếu","Sắp chiếu"),if(vm.soon) 1 else 0,{ vm.soon = it == 1 })
                        }
                    }
                }
                stickyHeader(key = "schedule-filters") {
                    Column(Modifier.fillMaxWidth().background(C.Background).padding(vertical = 8.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) { DateSelector(vm); TimeSelector(vm) }
                }
                item(key = "banner") {
                    CinemaBanner(browse = { scope.launch { grid.animateScrollToItem(3) } })
                }
                items(filtered,key = { it.id }) { movie -> MovieScheduleCard(movie,vm.slotsFor(movie.id),{ vm.openMovie(movie) },{ vm.openTrailer(movie) },vm::showSlot) }
                if(vm.soon && !vm.fixtureCatalog) item { Muted("Dữ liệu sắp chiếu chưa được tích hợp") }
                else if(vm.catalogLoading) item { Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { CircularProgressIndicator(); Muted("Đang tải phim…") } }
                else if(vm.catalogError.isNotBlank()) item { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { Muted(vm.catalogError); ActionButton("Thử lại", { vm.refreshCatalog() }) } }
                else if(filtered.isEmpty()) item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        val hasUpcoming = vm.movies.any { vm.slotsFor(it.id,false).isNotEmpty() }
                        val hasMatchingTime = vm.movies.any { vm.slotsFor(it.id).isNotEmpty() }
                        val message = when {
                            !hasUpcoming && vm.catalogDate == vm.todayDate -> "Hôm nay không còn suất chiếu sắp tới."
                            !hasUpcoming -> "Chưa có lịch chiếu cho ngày ${displayCalendarDate(vm.catalogDate.ifBlank { vm.todayDate })}"
                            !hasMatchingTime -> "Không có suất trong khung giờ đã chọn."
                            else -> "Chưa tìm thấy phim"
                        }
                        Text(message,style = MaterialTheme.typography.headlineMedium)
                        Muted("Thử tên phim khác, khung giờ khác hoặc chọn ngày khác.")
                        if(vm.query.isNotBlank()) ActionButton("Xóa tìm kiếm",{ vm.query = "" },secondary = true)
                    }
                }
            }
            CustomerNavigation(0, { scope.launch { grid.animateScrollToItem(0) } }, vm::account, vm::coming)
        }
    }
}

@Composable
private fun TimeSelector(vm: CineplexViewModel) {
    LazyRow(Modifier.fillMaxWidth().testTag("time-windows"),horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(timeWindows.size) { index ->
            FilterChip(vm.timeWindow == index,{ vm.selectTimeWindow(index) },label = { Text(timeWindows[index]) },colors = FilterChipDefaults.filterChipColors(selectedContainerColor = C.Primary,selectedLabelColor = androidx.compose.ui.graphics.Color.White),modifier = Modifier.heightIn(min = 48.dp).testTag("time-window-$index"))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelector(vm: CineplexViewModel) {
    val selected = vm.catalogDate.ifBlank { vm.todayDate }
    val dates = vm.catalogDateChoices
    val row = rememberLazyListState()
    var picker by remember { mutableStateOf(false) }
    LaunchedEffect(selected, dates) {
        val index = dates.indexOf(selected)
        if(index >= 0) row.animateScrollToItem(index)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Ngày xem: ${displayCalendarDate(selected)}",style = MaterialTheme.typography.titleSmall)
            TextButton({ picker = true },Modifier.heightIn(min = 48.dp).testTag("open-date-picker")) {
                CineplexIcon("calendar"); Spacer(Modifier.width(8.dp)); Text("Chọn ngày")
            }
        }
        LazyRow(state = row,modifier = Modifier.fillMaxWidth().testTag("catalog-dates"),horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dates,key = { it }) { date ->
                FilterChip(selected == date,{ vm.selectCatalogDate(date) },label = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if(date == vm.todayDate) "Hôm nay" else calendarWeekday(date))
                        Text(displayCalendarDate(date).take(5))
                    }
                },colors = FilterChipDefaults.filterChipColors(selectedContainerColor = C.Primary,selectedLabelColor = androidx.compose.ui.graphics.Color.White),modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 80.dp).testTag("catalog-date-$date").semantics { contentDescription = "Chọn ngày ${displayCalendarDate(date)}" })
            }
        }
    }
    if(picker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = calendarDateMillis(selected),selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = calendarDate(utcTimeMillis) >= vm.todayDate
            override fun isSelectableYear(year: Int) = year >= vm.todayDate.take(4).toInt()
        })
        DatePickerDialog(onDismissRequest = { picker = false },confirmButton = {
            TextButton({ state.selectedDateMillis?.let { vm.selectCatalogDate(calendarDate(it)) }; picker = false },enabled = state.selectedDateMillis?.let { calendarDate(it) >= vm.todayDate } == true) { Text("Xem lịch") }
        },dismissButton = { TextButton({ picker = false }) { Text("Hủy") } }) { DatePicker(state) }
    }
}

@Composable
private fun SearchField(value: String, change: (String) -> Unit) {
    BasicTextField(value,change,singleLine = true,modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Tìm kiếm phim" },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = C.Text),cursorBrush = SolidColor(C.Primary),keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        decorationBox = { inner ->
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).background(C.Surface,RoundedCornerShape(12.dp)).border(1.dp,C.Line,RoundedCornerShape(12.dp)).padding(start = 16.dp,end = 4.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CineplexIcon("search")
                Box(Modifier.weight(1f)) { if(value.isEmpty()) Text("Tìm tên phim bạn muốn xem",color = C.Muted); inner() }
                if(value.isNotEmpty()) IconButton(onClick = { change("") },modifier = Modifier.size(48.dp).semantics { contentDescription = "Xóa tìm kiếm" }) { CineplexIcon("close") }
                else Spacer(Modifier.width(12.dp))
            }
        }
    )
}
