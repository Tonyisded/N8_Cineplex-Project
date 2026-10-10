package com.nhom8.cineplex.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.nhom8.cineplex.R
import com.nhom8.cineplex.model.Notice
import com.nhom8.cineplex.ui.theme.CineplexColors as C

@Composable
fun CineplexLogo(width: Dp = 112.dp, modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.cineplex_logo), "Cineplex", modifier.width(width).aspectRatio(1394f/1128f), contentScale = ContentScale.Fit)
}
@Composable
fun Muted(text: String, modifier: Modifier = Modifier) = Text(text, modifier, color = C.Muted)
@Composable
fun ActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, secondary: Boolean = false, loading: Boolean = false, icon: String? = null, compact: Boolean = false) {
    val color = if(secondary) C.Primary else androidx.compose.ui.graphics.Color.White
    Surface(onClick = onClick, enabled = !loading, modifier = modifier.heightIn(min = if(compact) 48.dp else 56.dp).semantics { role = Role.Button },
        color = if(secondary) androidx.compose.ui.graphics.Color.Transparent else if(loading) C.Primary.copy(alpha = .65f) else C.Primary,
        shape = RoundedCornerShape(12.dp), border = if(secondary) BorderStroke(1.dp,C.Primary) else null) {
        Row(Modifier.padding(horizontal = if(compact) 12.dp else 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            if(icon != null && icon != "arrow" && !loading) CineplexIcon(icon,color = color)
            Text(text,color = color,fontWeight = FontWeight.Bold)
            if(loading) CircularProgressIndicator(Modifier.size(16.dp),color = color,strokeWidth = 2.dp)
            else if(icon == "arrow") CineplexIcon("arrow",color = color)
        }
    }
}
@Composable
fun LinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: String? = null, enabled: Boolean = true) {
    TextButton(onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(8.dp)) {
        if(icon != null) { CineplexIcon(icon,color = C.Primary); Spacer(Modifier.width(8.dp)) }
        Text(text, color = C.Primary, fontWeight = FontWeight.SemiBold)
    }
}
@Composable
fun Avatar(onClick: () -> Unit, admin: Boolean = false, session: com.nhom8.cineplex.model.Session? = null) {
    Surface(onClick, shape = CircleShape, color = C.Raised, border = BorderStroke(1.dp,C.Line), modifier = Modifier.size(48.dp).semantics { contentDescription = if(admin) "Mở tài khoản quản trị" else "Mở tài khoản" }) {
        UserAvatar(session, Modifier.fillMaxSize())
    }
}
@Composable
fun CineplexDialog(notice: Notice, dismiss: () -> Unit, logout: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = dismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = C.Surface, border = BorderStroke(1.dp,C.Line)) {
            Column(Modifier.fillMaxWidth().heightIn(max = 600.dp).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text(notice.title,style = MaterialTheme.typography.headlineMedium)
                Text(notice.message)
                if(notice.account) ActionButton("Đăng xuất",logout,Modifier.fillMaxWidth())
                ActionButton(if(notice.account) "Đóng" else "Đã hiểu",dismiss,Modifier.fillMaxWidth(),secondary = notice.account)
            }
        }
    }
}
@Composable
fun CinemaBanner(staff: Boolean = false, browse: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Raised).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1.25f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if(staff) "CINEPLEX STAFF" else "HẸN BẠN Ở RẠP",color = C.Primary,fontWeight = FontWeight.Bold,letterSpacing = 1.2.sp)
            Text(if(staff) "Sẵn sàng cho\nsuất chiếu tiếp theo." else "Câu chuyện mới,\ntrên màn ảnh lớn.",style = MaterialTheme.typography.headlineMedium)
            Muted(if(staff) "Chọn một công việc để bắt đầu." else "Khám phá bộ phim dành cho bạn.")
            if(!staff) LinkButton("Khám phá phim",browse,icon = "arrow")
        }
        Image(painterResource(R.drawable.cinema_isometric),null,Modifier.weight(1f).aspectRatio(1.5f),contentScale = ContentScale.Fit)
    }
}
