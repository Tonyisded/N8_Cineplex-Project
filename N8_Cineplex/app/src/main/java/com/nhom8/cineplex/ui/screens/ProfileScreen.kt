package com.nhom8.cineplex.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import java.util.Locale

@Composable
fun ProfileScreen(vm: CineplexViewModel) {
    val session = vm.session ?: return
    val staff = session.role == Role.ADMIN
    val role = if (staff) "Quản trị viên" else "Khách hàng"
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
                        contentDescription = if (staff) "Quay lại dashboard quản trị" else "Quay lại Trang chủ"
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
    val initials = session.name.trim().split(Regex("\\s+")).takeLast(2)
        .filter { it.isNotEmpty() }.joinToString("") {
            String(Character.toChars(it.codePointAt(0)))
        }.uppercase(Locale.forLanguageTag("vi"))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(Modifier.size(80.dp), shape = CircleShape, color = C.Raised, border = BorderStroke(1.dp, C.Line)) {
            Box(contentAlignment = Alignment.Center) {
                Text(initials, color = C.Primary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
        }
        ProfileHeading(session.name)
        Text(if (staff) "$role · Cineplex Staff" else role, color = C.Primary, fontWeight = FontWeight.Bold)
        Muted("Cùng Cineplex tận hưởng những bộ phim bạn yêu thích.")
    }
}

@Composable
private fun ProfileContent(vm: CineplexViewModel, session: Session, role: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(32.dp)) {
        Surface(shape = RoundedCornerShape(16.dp), color = C.Surface, border = BorderStroke(1.dp, C.Line)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                ProfileHeading("Thông tin tài khoản")
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ProfileData("Họ và tên", session.name)
                    ProfileData("Email", session.email)
                    ProfileData("Loại tài khoản", role)
                }
                ActionButton("Chỉnh sửa thông tin", { vm.profileAction("edit") }, Modifier.fillMaxWidth(), secondary = true, icon = "user")
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
