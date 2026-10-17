package com.nhom8.cineplex.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil3.compose.AsyncImage
import com.nhom8.cineplex.R
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.Movie
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.components.*
import com.nhom8.cineplex.ui.theme.CineplexColors as C

@Composable
fun DetailExperienceScreen(vm: CineplexViewModel, movie: Movie) {
    val meta = DetailSchedule.filmMeta(movie)
    var expanded by rememberSaveable(movie.id) { mutableStateOf(false) }
    var castCredit by remember { mutableStateOf<CastCredit?>(null) }
    Scaffold(containerColor = C.Surface, topBar = { DetailHeader("Chi tiết phim", vm::back) }, bottomBar = {
        DetailActionBar { ActionButton("Chọn suất chiếu", vm::openSchedule, Modifier.fillMaxWidth(), icon = "arrow") }
    }) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState()).testTag("movie-detail")) {
            Box(Modifier.fillMaxWidth().height(218.dp).background(C.Text)) {
                if (DetailSchedule.isDune(movie)) Image(painterResource(R.drawable.detail_backdrop),null,Modifier.fillMaxSize(),contentScale = ContentScale.Crop)
                else if (movie.poster != 0) Image(painterResource(movie.poster),null,Modifier.fillMaxSize(),contentScale = ContentScale.Crop)
                else AsyncImage(movie.posterUrl,null,Modifier.fillMaxSize(),contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xCC11141A)))))
                Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp),verticalAlignment = Alignment.Bottom,horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("ĐẠO DIỄN",color = Color.White.copy(alpha = .75f),fontSize = 12.sp); Text(movie.director.ifBlank { "Đang cập nhật" },color = Color.White,fontWeight = FontWeight.SemiBold) }
                    if (meta.trailerYoutubeId != null) OutlinedButton({ vm.openTrailer(movie) },colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),border = BorderStroke(1.dp,Color.White.copy(alpha = .7f))) { Text("▷ Xem trailer") }
                }
            }
            Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp),verticalAlignment = Alignment.Top) {
                    if (DetailSchedule.isDune(movie)) Image(painterResource(R.drawable.detail_poster),"Poster ${movie.name}",Modifier.width(76.dp).aspectRatio(2f/3f).clip(RoundedCornerShape(8.dp)),contentScale = ContentScale.Fit)
                    else MoviePoster(movie,Modifier.width(76.dp))
                    Column(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(8.dp)) { DetailAgeBadge(movie.age); Text(movie.genre,color = C.Muted,fontSize = 13.sp,maxLines = 2) }
                        Text(movie.name,style = MaterialTheme.typography.headlineMedium)
                        if (movie.original.isNotBlank() && movie.original != movie.name) Text(movie.original,color = C.Muted,fontSize = 13.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text("◷ ${movie.minutes} phút",color = C.Muted,fontSize = 14.sp)
                    Text("▦ Khởi chiếu ${meta.releaseDate?.let { runCatching { displayCalendarDate(it) }.getOrDefault(it) } ?: "đang cập nhật"}",color = C.Muted,fontSize = 14.sp)
                }
                if (meta.rating != null) Surface(color = C.Background,shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("★",color = Color(0xFF85500D),fontSize = 28.sp)
                        Column(Modifier.weight(1f)) { Text(meta.rating,fontWeight = FontWeight.Bold,fontSize = 23.sp); Text(meta.reviewCount.orEmpty(),color = C.Muted,fontSize = 13.sp) }
                        Text("Điểm mẫu",color = C.Muted,fontSize = 12.sp)
                    }
                }
                else Text("★ Chưa có đánh giá",Modifier.fillMaxWidth().background(C.Background,RoundedCornerShape(12.dp)).padding(16.dp),color = C.Muted)
                HorizontalDivider(color = C.Line)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nội dung phim",style = MaterialTheme.typography.headlineMedium)
                    Text(movie.description, maxLines = if (expanded) Int.MAX_VALUE else 3,overflow = TextOverflow.Ellipsis,lineHeight = 25.sp)
                    if (movie.description.isNotBlank()) TextButton({ expanded = !expanded },Modifier.heightIn(min = 48.dp).testTag("expand-synopsis")) { Text(if (expanded) "Thu gọn" else "Xem thêm") }
                }
            }
            HorizontalDivider(color = C.Line)
            Column(Modifier.fillMaxWidth().padding(vertical = 20.dp),verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Diễn viên",Modifier.padding(horizontal = 20.dp),style = MaterialTheme.typography.headlineMedium)
                if (DetailSchedule.isDune(movie)) LazyRow(contentPadding = PaddingValues(horizontal = 20.dp),horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(DetailSchedule.duneCast) { person -> Column(Modifier.width(96.dp).clickable { castCredit = person },horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(painterResource(person.portrait),person.name,Modifier.size(72.dp).clip(CircleShape),contentScale = ContentScale.Crop)
                        Text(person.name,maxLines = 2,overflow = TextOverflow.Ellipsis,fontSize = 13.sp,fontWeight = FontWeight.Medium)
                        Text(person.role,color = C.Muted,fontSize = 12.sp,maxLines = 1)
                    } }
                } else Text(movie.cast.ifBlank { "Đang cập nhật" },Modifier.padding(horizontal = 20.dp),lineHeight = 24.sp)
            }
        }
    }
    castCredit?.let { person -> AlertDialog(onDismissRequest = { castCredit = null },confirmButton = { TextButton({ castCredit = null }) { Text("Đóng") } },title = { Text(person.name) },text = { Column { Image(painterResource(person.portrait),null,Modifier.size(110.dp).clip(CircleShape),contentScale = ContentScale.Crop); Spacer(Modifier.height(12.dp)); Text("Vai ${person.role}") } }) }
}

@Composable internal fun DetailHeader(title: String, back: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(60.dp).background(C.Surface).padding(horizontal = 12.dp),verticalAlignment = Alignment.CenterVertically) {
        IconButton(back,Modifier.size(48.dp).semantics { contentDescription = "Quay lại" }) { CineplexIcon("back",color = C.Text) }
        Text(title,Modifier.padding(start = 8.dp),style = MaterialTheme.typography.titleMedium)
    }
}
@Composable internal fun DetailActionBar(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().background(C.Surface).border(BorderStroke(1.dp,C.Line)).padding(horizontal = 20.dp,vertical = 12.dp)) { content() }
}
@Composable internal fun DetailAgeBadge(age: String) {
    Surface(color = C.Raised,shape = RoundedCornerShape(8.dp),modifier = Modifier.semantics { contentDescription = "Phân loại $age" }) { Text(age,Modifier.padding(horizontal = 9.dp,vertical = 5.dp),color = C.Primary,fontWeight = FontWeight.Bold,fontSize = 13.sp) }
}
