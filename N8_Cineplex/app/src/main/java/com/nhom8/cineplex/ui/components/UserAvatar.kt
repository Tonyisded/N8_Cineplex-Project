package com.nhom8.cineplex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.nhom8.cineplex.model.Session
import com.nhom8.cineplex.ui.theme.CineplexColors
import java.util.Locale

@Composable fun UserAvatar(session: Session?, modifier: Modifier = Modifier) {
    var failed by remember(session?.avatarUrl) { mutableStateOf(false) }
    val initials = session?.name.orEmpty().trim().split(Regex("\\s+")).takeLast(2).filter(String::isNotEmpty).joinToString("") { String(Character.toChars(it.codePointAt(0))) }.uppercase(Locale.forLanguageTag("vi"))
    Box(modifier.clip(CircleShape).background(CineplexColors.Raised), contentAlignment = Alignment.Center) {
        if (session?.avatarUrl != null && !failed) AsyncImage(session.avatarUrl, "Ảnh đại diện", Modifier.fillMaxSize(), contentScale = ContentScale.Crop, onError = { failed = true })
        else Text(initials.ifBlank { "?" }, color = CineplexColors.Primary)
    }
}
