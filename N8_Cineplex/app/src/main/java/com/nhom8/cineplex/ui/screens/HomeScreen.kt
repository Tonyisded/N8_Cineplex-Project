package com.nhom8.cineplex.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
fun HomeScreen(vm: CineplexViewModel, grid: LazyGridState) {
    val scope = rememberCoroutineScope()
    val filtered = vm.filteredMovies
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = if(maxWidth >= 1024.dp) 4 else if(maxWidth >= 768.dp) 3 else 2
        Column(Modifier.fillMaxSize().imePadding()) {
            LazyVerticalGrid(GridCells.Fixed(columns),state = grid,modifier = Modifier.weight(1f),contentPadding = PaddingValues(24.dp),horizontalArrangement = Arrangement.spacedBy(24.dp),verticalArrangement = Arrangement.spacedBy(24.dp)) {
                item(key = "header",span = { GridItemSpan(columns) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.SpaceBetween) { CineplexLogo(); Avatar(vm::account, session = vm.session) }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Muted("Xin chào, ${vm.session?.name.orEmpty()}")
                            Text("Hôm nay, bạn muốn xem gì?",style = MaterialTheme.typography.headlineMedium)
                        }
                        SearchField(vm.query,{ vm.query = it })
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            PrototypeTabs(listOf("Đang chiếu","Sắp chiếu"),if(vm.soon) 1 else 0,{ vm.soon = it == 1 })
                            DateSelector(vm)
                        }
                    }
                }
                item(key = "banner",span = { GridItemSpan(columns) }) {
                    CinemaBanner(browse = { scope.launch { grid.animateScrollToItem(2) } })
                }
                items(filtered,key = { it.id }) { movie -> MovieCard(movie) { vm.openMovie(movie) } }
                if(vm.soon && !vm.fixtureCatalog) item(span = { GridItemSpan(columns) }) { Muted("Dữ liệu sắp chiếu chưa được tích hợp") }
                else if(vm.catalogLoading) item(span = { GridItemSpan(columns) }) { Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { CircularProgressIndicator(); Muted("Đang tải phim…") } }
                else if(vm.catalogError.isNotBlank()) item(span = { GridItemSpan(columns) }) { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { Muted(vm.catalogError); ActionButton("Thử lại", { vm.refreshCatalog() }) } }
                else if(filtered.isEmpty()) item(span = { GridItemSpan(columns) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(if(vm.movies.isEmpty()) "Chưa có lịch chiếu cho ngày ${displayCalendarDate(vm.catalogDate.ifBlank { vm.todayDate })}" else "Chưa tìm thấy phim",style = MaterialTheme.typography.headlineMedium)
                        Muted(if(vm.movies.isEmpty()) "Hãy chọn ngày khác hoặc quay lại sau." else "Thử tên phim khác hoặc đổi mục phim.")
                        if(vm.query.isNotBlank()) ActionButton("Xóa tìm kiếm",{ vm.query = "" },secondary = true)
                    }
                }
            }
            CustomerNavigation(0, { scope.launch { grid.animateScrollToItem(0) } }, vm::account, vm::coming)
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
                },modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 80.dp).testTag("catalog-date-$date").semantics { contentDescription = "Chọn ngày ${displayCalendarDate(date)}" })
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
