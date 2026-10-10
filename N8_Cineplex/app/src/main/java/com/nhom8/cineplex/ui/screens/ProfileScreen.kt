package com.nhom8.cineplex.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhom8.cineplex.model.Role
import com.nhom8.cineplex.model.Session
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.components.*
import com.nhom8.cineplex.ui.theme.CineplexColors as C

@Composable
fun ProfileScreen(vm: CineplexViewModel) {
    val session = vm.session ?: return
    val staff = session.role != Role.CUSTOMER
    val role = when(session.role) { Role.ADMIN -> "Quản trị viên"; Role.STAFF -> "Nhân viên"; Role.CUSTOMER -> "Khách hàng" }
    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            val tablet = maxWidth >= 768.dp
            Column(
                Modifier.widthIn(max = 960.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = if (tablet) 48.dp else 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(vm::back, modifier = Modifier.size(48.dp).semantics {
                        contentDescription = when(session.role) { Role.ADMIN -> "Quay lại dashboard quản trị"; Role.STAFF -> "Quay lại trang nhân viên"; Role.CUSTOMER -> "Quay lại Trang chủ" }
                    }) { CineplexIcon("back") }
                    Spacer(Modifier.weight(1f))
                    CineplexLogo()
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("TÀI KHOẢN CINEPLEX", color = C.Primary, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text("Thông tin cá nhân", Modifier.semantics { heading() }, fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 42.sp)
                    Muted("Thông tin của bạn, gọn trong một nơi.")
                }
                if (tablet) {
                    Row(horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.Top) {
                        ProfileIdentity(session, role, staff, Modifier.weight(1f))
                        ProfileContent(vm, session, role, Modifier.weight(1.6f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
                        ProfileIdentity(session, role, staff, Modifier.fillMaxWidth())
                        HorizontalDivider(color = C.Line)
                        ProfileContent(vm, session, role, Modifier.fillMaxWidth())
                    }
                }
            }
        }
        if (!staff) CustomerNavigation(3, vm::back, vm::account, vm::coming)
    }
}

@Composable
private fun ProfileIdentity(session: Session, role: String, staff: Boolean, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(Modifier.size(80.dp), shape = CircleShape, color = C.Raised, border = BorderStroke(1.dp, C.Line)) {
            Box(contentAlignment = Alignment.Center) {
                UserAvatar(session, Modifier.fillMaxSize())
            }
        }
        ProfileHeading(session.name)
        Text(if (staff) "$role · Cineplex Staff" else role, color = C.Primary, fontWeight = FontWeight.Bold)
        Muted("Cùng Cineplex tận hưởng những bộ phim bạn yêu thích.")
    }
}

@Composable
private fun ProfileContent(vm: CineplexViewModel, session: Session, role: String, modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<android.net.Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { selected = it }
    selected?.let { uri ->
        AlertDialog(onDismissRequest = { selected = null }, title = { Text("Xem trước ảnh đại diện") },
            text = { AsyncImage(uri, "Ảnh đã chọn", Modifier.fillMaxWidth().height(200.dp), contentScale = ContentScale.Fit) },
            confirmButton = { TextButton(onClick = {
                selected = null
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        runCatching {
                            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                            val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                                val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192); val limit = 5 * 1024 * 1024
                                while (output.size() <= limit) { val n = input.read(buffer, 0, minOf(buffer.size, limit + 1 - output.size())); if (n < 0) break; output.write(buffer, 0, n) }
                                output.toByteArray()
                            } ?: error("Image unavailable")
                            if (bytes.size > 5 * 1024 * 1024 || mime !in listOf("image/jpeg", "image/png", "image/webp")) error("Invalid image")
                            bytes to mime
                        }
                    }
                    result.fold({ (bytes, mime) -> vm.avatar(bytes, mime) }, { vm.avatarError() })
                }
            }) { Text("Upload") } }, dismissButton = { TextButton(onClick = { selected = null }) { Text("Hủy") } })
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(32.dp)) {
        Surface(shape = RoundedCornerShape(16.dp), color = C.Surface, border = BorderStroke(1.dp, C.Line)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                ProfileHeading("Thông tin tài khoản")
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ProfileData("Họ và tên", session.name)
                    ProfileData("Email", session.email)
                    ProfileData("Loại tài khoản", role)
                    ProfileData("Xác minh email", if(session.emailVerifiedAt != null) "Đã xác minh" else "Chưa xác minh")
                }
                if(session.emailVerifiedAt == null) ActionButton("Gửi lại email xác minh", vm::resend, Modifier.fillMaxWidth(), secondary = true, loading = vm.loading)
                ActionButton("Chỉnh sửa thông tin", { vm.profileAction("edit") }, Modifier.fillMaxWidth(), secondary = true, icon = "user", loading = vm.loading)
                ActionButton("Chọn ảnh đại diện", { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, Modifier.fillMaxWidth(), secondary = true, loading = vm.loading)
                if(session.avatarUrl != null) ActionButton("Xóa ảnh đại diện", vm::deleteAvatar, Modifier.fillMaxWidth(), secondary = true, loading = vm.loading)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            ProfileHeading("Bảo mật & hỗ trợ")
            Column {
                ProfileOption("lock", "Đổi mật khẩu", "Bảo vệ tài khoản của bạn") { vm.profileAction("password") }
                ProfileOption("lock", "Chính sách bảo mật", "Cách dữ liệu được sử dụng") { vm.terms(true) }
                ProfileOption("user", "Trợ giúp & hỗ trợ", "Giải đáp khi bạn cần") { vm.profileAction("help") }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionButton("Đăng xuất", vm::logout, Modifier.fillMaxWidth().testTag("profile-logout"), secondary = true, icon = "exit")
            Muted("Bạn sẽ quay về màn hình đăng nhập.", Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun ProfileHeading(text: String) {
    Text(text, Modifier.semantics { heading() }, fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp)
}

@Composable
private fun ProfileData(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Muted(label)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProfileOption(icon: String, title: String, description: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CineplexIcon(icon, color = C.Primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Muted(description)
            }
            CineplexIcon("arrow", Modifier.size(16.dp))
        }
        HorizontalDivider(color = C.Line)
    }
}
