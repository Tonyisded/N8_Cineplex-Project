package com.nhom8.cineplex.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import com.nhom8.cineplex.model.Movie
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
                DetailBody(movie,Modifier.weight(1f))
            } else Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 24.dp).padding(bottom = 24.dp),verticalArrangement = Arrangement.spacedBy(32.dp)) {
                MoviePoster(movie,Modifier.fillMaxWidth(.68f).widthIn(max = 300.dp).align(Alignment.CenterHorizontally))
                DetailBody(movie,Modifier.fillMaxWidth())
            }
        }
        Column(Modifier.background(C.Surface)) {
            HorizontalDivider(color = C.Line)
            Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp,vertical = 16.dp),contentAlignment = Alignment.Center) { ActionButton("Đặt vé",{ vm.coming("Đặt vé") },Modifier.widthIn(max = 640.dp).fillMaxWidth(),icon = "ticket") }
        }
    }
}
@Composable
private fun DetailBody(movie: Movie, modifier: Modifier) {
    Column(modifier,verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Muted("${movie.year} · ${movie.original}")
            Text(movie.name,style = MaterialTheme.typography.headlineLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AgeBadge(movie.age); Muted(movie.genre); Muted("${movie.minutes} phút")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Nội dung phim",style = MaterialTheme.typography.headlineMedium)
            Text(movie.description,lineHeight = 28.sp)
        }
        HorizontalDivider(color = C.Line)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Muted("Đạo diễn"); Text(movie.director) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Muted("Diễn viên"); Text(movie.cast) }
        }
        Muted("Lịch chiếu và giới hạn tuổi trong bản mẫu chỉ mang tính minh họa.")
    }
}
