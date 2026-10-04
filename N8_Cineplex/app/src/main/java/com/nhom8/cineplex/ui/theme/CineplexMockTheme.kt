package com.nhom8.cineplex.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*

object CineplexColors {
    val Background = Color(0xFFF5F3F0)
    val Surface = Color.White
    val Raised = Color(0xFFECE7E9)
    val Primary = Color(0xFF7B263D)
    val Pressed = Color(0xFF5C1C2E)
    val Text = Color(0xFF25232A)
    val Muted = Color(0xFF625D65)
    val Line = Color(0xFFD9D3D6)
    val Error = Color(0xFFA12835)
    val Success = Color(0xFF246345)
}
object CineplexSpacing {
    val Small = 8.dp
    val Medium = 16.dp
    val Page = 24.dp
    val Large = 32.dp
    val Touch = 48.dp
    val Input = 56.dp
}
@Composable
fun CineplexMockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = CineplexColors.Primary, onPrimary = Color.White,
            background = CineplexColors.Background, surface = CineplexColors.Surface,
            surfaceVariant = CineplexColors.Raised, onBackground = CineplexColors.Text,
            onSurface = CineplexColors.Text, onSurfaceVariant = CineplexColors.Muted,
            outline = CineplexColors.Line, error = CineplexColors.Error),
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
            headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 29.sp),
            titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 25.sp),
            bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
            labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp)
        ), shapes = Shapes(small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(16.dp)),
        content = content
    )
}
