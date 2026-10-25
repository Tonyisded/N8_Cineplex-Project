package com.nhom8.cineplex.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import coil3.compose.AsyncImage
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nhom8.cineplex.model.Movie
import com.nhom8.cineplex.model.classifiedAgeRatings
import com.nhom8.cineplex.data.ShowtimeDto
import com.nhom8.cineplex.data.displayCalendarDate
import com.nhom8.cineplex.ui.theme.CineplexColors as C

@Composable
fun MoviePoster(movie: Movie, modifier: Modifier = Modifier) {
    val frame = modifier.aspectRatio(movie.ratio).shadow(2.dp,RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))
    if (movie.poster != 0) Image(painterResource(movie.poster), "Poster ${movie.name}", frame, contentScale = ContentScale.Fit)
    else Box(frame.background(C.Surface), contentAlignment = Alignment.Center) {
        Text("Poster ${movie.name}", Modifier.padding(12.dp), style = MaterialTheme.typography.labelSmall)
        AsyncImage(model = movie.posterUrl, contentDescription = "Poster ${movie.name}", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}
@Composable
fun AgeBadge(age: String) {
    if (age !in classifiedAgeRatings) return
    Surface(shape = RoundedCornerShape(8.dp),color = androidx.compose.ui.graphics.Color.Transparent,border = BorderStroke(1.dp,C.Line)) {
        Text(if(age in listOf("T13","T16","T18")) age.drop(1) + "+" else age,Modifier.heightIn(min = 32.dp).padding(horizontal = 8.dp,vertical = 4.dp),color = C.Primary,style = MaterialTheme.typography.labelLarge)
    }
}
@Composable
fun ComingSoonCard(movie: Movie, open: () -> Unit, trailer: () -> Unit) {
    val release = movie.releaseDate?.let { runCatching { displayCalendarDate(it) }.getOrNull() }
    Column(Modifier.fillMaxWidth().testTag("movie-${movie.id}"),verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.Top) {
            Text(movie.name,Modifier.weight(1f).clickable(role = Role.Button,onClick = open).padding(vertical = 12.dp),style = MaterialTheme.typography.titleMedium)
            TextButton(open,Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Xem chi tiết ${movie.name}" }) { Text("Chi tiết") }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AgeBadge(movie.age); if(movie.genre.isNotBlank()) Muted(movie.genre)
            Muted(if(release != null) "Khởi chiếu $release" else "Đang cập nhật")
            Muted(if(movie.minutes > 0) "${movie.minutes} phút" else "Đang cập nhật")
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.spacedBy(12.dp),verticalAlignment = Alignment.Top) {
            MoviePoster(movie,Modifier.width(96.dp).clickable(role = Role.Button,onClickLabel = "Xem chi tiết ${movie.name}",onClick = open))
            Column(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if(movie.original.isNotBlank() && movie.original != movie.name) Muted(movie.original)
                TrailerButton(movie,trailer)
            }
        }
        HorizontalDivider(color = C.Line)
    }
}
@Composable
fun MovieScheduleCard(movie: Movie, slots: List<ShowtimeDto>, open: () -> Unit, trailer: () -> Unit, showSlot: (ShowtimeDto) -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("movie-${movie.id}"),verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.Top) {
            Text(movie.name,Modifier.weight(1f).clickable(role = Role.Button,onClick = open).padding(vertical = 12.dp),style = MaterialTheme.typography.titleMedium)
            TextButton(open,Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Xem chi tiết ${movie.name}" }) { Text("Chi tiết") }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AgeBadge(movie.age); if(movie.genre.isNotBlank()) Muted(movie.genre); Muted("${movie.minutes} phút")
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.width(96.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MoviePoster(movie,Modifier.fillMaxWidth().clickable(role = Role.Button,onClickLabel = "Xem chi tiết ${movie.name}",onClick = open))
                TrailerButton(movie,trailer)
            }
            ShowtimeGroups(slots,showSlot,Modifier.weight(1f))
        }
        HorizontalDivider(color = C.Line)
    }
}
@Composable
fun TrailerButton(movie: Movie, open: () -> Unit) {
    if(movie.trailerYoutubeId?.matches(Regex("[A-Za-z0-9_-]{11}")) == true)
        TextButton(open,Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Xem trailer ${movie.name}" }) { Text("▷ Trailer") }
    else Text("Chưa có trailer",style = MaterialTheme.typography.labelSmall,color = C.Muted)
}
