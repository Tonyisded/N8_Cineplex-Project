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
import androidx.compose.ui.unit.dp
import com.nhom8.cineplex.model.Movie
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
    Surface(shape = RoundedCornerShape(8.dp),color = androidx.compose.ui.graphics.Color.Transparent,border = BorderStroke(1.dp,C.Line)) {
        Text(age,Modifier.heightIn(min = 32.dp).padding(horizontal = 8.dp,vertical = 4.dp),color = C.Primary,style = MaterialTheme.typography.labelLarge)
    }
}
@Composable
fun MovieCard(movie: Movie, open: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MoviePoster(movie,Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button,onClickLabel = "Xem chi tiết ${movie.name}",onClick = open))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(movie.name,Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button,onClick = open).wrapContentHeight(Alignment.CenterVertically),style = MaterialTheme.typography.titleMedium)
            if (movie.genre.isNotBlank()) Muted(movie.genre)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) { AgeBadge(movie.age); Muted("${movie.minutes} phút",Modifier.heightIn(min = 32.dp).wrapContentHeight(Alignment.CenterVertically)) }
            ActionButton("Đặt vé",open,Modifier.fillMaxWidth().padding(top = 8.dp).semantics { contentDescription = "Đặt vé ${movie.name}" },icon = "ticket",compact = true)
        }
    }
}
