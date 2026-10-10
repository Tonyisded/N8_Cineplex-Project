package com.nhom8.cineplex.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import com.nhom8.cineplex.model.Movie
import com.nhom8.cineplex.data.vietnamTime
import com.nhom8.cineplex.data.displayCalendarDate
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.components.*
import com.nhom8.cineplex.ui.theme.CineplexColors as C

@Composable
fun MovieDetailScreen(vm: CineplexViewModel, movie: Movie) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 24.dp,vertical = 16.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(vm::back,Modifier.size(48.dp).semantics { contentDescription = "Quay lại Trang chủ" }) { CineplexIcon("back",color = C.Text) }
            Text("Chi tiết phim",style = MaterialTheme.typography.titleMedium)
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth >= 768.dp
            val scroll = rememberScrollState()
            if(wide) Row(Modifier.fillMaxSize().verticalScroll(scroll).padding(24.dp),horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                MoviePoster(movie,Modifier.width(300.dp))
                DetailBody(vm,movie,Modifier.weight(1f))
            } else Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 24.dp).padding(bottom = 24.dp),verticalArrangement = Arrangement.spacedBy(32.dp)) {
                MoviePoster(movie,Modifier.fillMaxWidth(.68f).widthIn(max = 300.dp).align(Alignment.CenterHorizontally))
                DetailBody(vm,movie,Modifier.fillMaxWidth())
            }
        }
        Column(Modifier.background(C.Surface)) {
            HorizontalDivider(color = C.Line)
            Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp,vertical = 16.dp),contentAlignment = Alignment.Center) { ActionButton("Đặt vé",{ vm.coming("Đặt vé") },Modifier.widthIn(max = 640.dp).fillMaxWidth(),icon = "ticket") }
        }
    }
}
@Composable
private fun DetailBody(vm: CineplexViewModel, movie: Movie, modifier: Modifier) {
    Column(modifier,verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if(movie.original.isNotBlank()) Muted(movie.original)
            movie.releaseDate?.let { Muted("Khởi chiếu: $it") }
            Text(movie.name,style = MaterialTheme.typography.headlineLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AgeBadge(movie.age); if(movie.genre.isNotBlank()) Muted(movie.genre); Muted("${movie.minutes} phút")
            }
        }
        if(movie.description.isNotBlank()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Nội dung phim",style = MaterialTheme.typography.headlineMedium)
            Text(movie.description,lineHeight = 28.sp)
        }
        HorizontalDivider(color = C.Line)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if(movie.director.isNotBlank()) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Muted("Đạo diễn"); Text(movie.director) }
            if(movie.cast.isNotBlank()) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Muted("Diễn viên"); Text(movie.cast) }
            movie.language?.let { Muted("Ngôn ngữ: $it") }
        }
        Text("Lịch chiếu ${displayCalendarDate(vm.catalogDate.ifBlank { vm.todayDate })} · CGV Landmark 81", style = MaterialTheme.typography.headlineMedium)
        if(vm.detailLoading) { CircularProgressIndicator(); Muted("Đang tải lịch chiếu…") }
        else if(vm.detailError.isNotBlank()) { Muted(vm.detailError); ActionButton("Thử lại", vm::refreshMovie) }
        else if(vm.showtimes.isEmpty()) Muted("Chưa có lịch chiếu cho phim này ngày ${displayCalendarDate(vm.catalogDate.ifBlank { vm.todayDate })}.")
        else vm.showtimes.forEach { slot ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${vietnamTime(slot.startAt)} · ${slot.format.replace('_', ' ')}", style = MaterialTheme.typography.titleMedium)
                Muted("${slot.auditorium.name} · ${slot.basePrice} VND")
            }
        }
        Muted("Giờ Việt Nam. Giá và phòng/ghế là mô phỏng N8, không phải giá hay sơ đồ ghế CGV.")
    }
}
