package com.nhom8.cineplex.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import com.nhom8.cineplex.model.Screen
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.components.*
import com.nhom8.cineplex.ui.theme.CineplexColors as C

@Composable
fun AuthScreen(vm: CineplexViewModel) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 1024.dp
        val scroll = rememberScrollState()
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(scroll).padding(horizontal = if(wide) 48.dp else 24.dp,vertical = 32.dp),horizontalAlignment = Alignment.CenterHorizontally) {
            if(wide) Row(Modifier.widthIn(max = 1040.dp).fillMaxWidth(),horizontalArrangement = Arrangement.spacedBy(64.dp),verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f),horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.spacedBy(32.dp)) {
                    CineplexLogo(240.dp)
                    Text(if(vm.screen == Screen.STAFF) "Phía sau mỗi suất chiếu." else "Một chỗ ngồi.\nNhững câu chuyện mới.",fontSize = 24.sp,fontWeight = FontWeight.SemiBold)
                    Muted(if(vm.screen == Screen.STAFF) "Không gian làm việc dành cho đội ngũ vận hành Cineplex." else "Tìm bộ phim bạn yêu thích và chuẩn bị cho buổi xem phim tiếp theo.")
                }
                Surface(Modifier.weight(1.2f).widthIn(max = 480.dp),shape = RoundedCornerShape(16.dp),border = BorderStroke(1.dp,C.Line),shadowElevation = 2.dp) {
                    AuthPanel(vm,Modifier.padding(32.dp))
                }
            } else {
                CineplexLogo(160.dp)
                Spacer(Modifier.height(32.dp))
                AuthPanel(vm,Modifier.widthIn(max = 440.dp).fillMaxWidth())
            }
        }
    }
}
@Composable
private fun AuthPanel(vm: CineplexViewModel, modifier: Modifier) {
    val signup = vm.screen == Screen.SIGNUP
    val staff = vm.screen == Screen.STAFF
    val focus = remember(vm.screen) { listOf("name","email","password","confirm","consent").associateWith { FocusRequester() } }
    val manager = LocalFocusManager.current
    fun submit() { manager.clearFocus(); vm.submit(); vm.errors.keys.firstOrNull()?.let { focus[it]?.requestFocus() } }
    LaunchedEffect(vm.success) { if(vm.success) focus.getValue("password").requestFocus() }
    Column(modifier,verticalArrangement = Arrangement.spacedBy(24.dp)) {
        if(staff) {
            LinkButton("Đăng nhập khách hàng",{ vm.navigate(Screen.LOGIN) },icon = "back")
            Text("CINEPLEX STAFF",color = C.Primary,fontWeight = FontWeight.Bold,letterSpacing = 1.2.sp)
        } else PrototypeTabs(listOf("Đăng nhập","Đăng ký"),if(signup) 1 else 0,{ vm.navigate(if(it == 1) Screen.SIGNUP else Screen.LOGIN) },auth = true)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if(signup) "Bắt đầu trải nghiệm Cineplex" else if(staff) "Đăng nhập nhân viên" else "Hẹn bạn ở rạp",fontFamily = FontFamily.SansSerif,fontWeight = FontWeight.Bold,fontSize = 32.sp,lineHeight = 42.sp)
            Muted(if(signup) "Đăng ký để đặt vé và quản lý vé của bạn." else if(staff) "Đăng nhập để tiếp tục công việc tại rạp." else "Một bộ phim hay đang chờ bạn. Đăng nhập để tiếp tục.")
        }
        if(signup) LabeledField("name","Họ và tên","Nhập họ và tên của bạn",vm.form.name,{ vm.update(vm.form.copy(name = it)) },vm.errors["name"],!vm.loading,focus.getValue("name"),{ vm.validateField("name") })
        LabeledField("email","Email","Nhập email của bạn",vm.form.email,{ vm.update(vm.form.copy(email = it)) },vm.errors["email"],!vm.loading,focus.getValue("email"),{ vm.validateField("email") },email = true)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledField("password","Mật khẩu","Nhập mật khẩu của bạn",vm.form.password,{ vm.update(vm.form.copy(password = it)) },vm.errors["password"],!vm.loading,focus.getValue("password"),{ vm.validateField("password") },password = true,last = !signup,submit = ::submit)
            if(signup) Muted("Mật khẩu tối thiểu 8 ký tự.")
        }
        if(signup) {
            LabeledField("confirm","Xác nhận mật khẩu","Nhập lại mật khẩu",vm.form.confirm,{ vm.update(vm.form.copy(confirm = it)) },vm.errors["confirm"],!vm.loading,focus.getValue("confirm"),{ vm.validateField("confirm") },password = true,last = true,submit = ::submit)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.Top,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Checkbox(vm.form.consent,{ vm.update(vm.form.copy(consent = it)); vm.validateField("consent") },enabled = !vm.loading,modifier = Modifier.size(48.dp).focusRequester(focus.getValue("consent")).semantics { contentDescription = "Tôi đồng ý với Điều khoản sử dụng và Chính sách bảo mật" })
                    FlowRow(Modifier.weight(1f),horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tôi đồng ý với",Modifier.heightIn(min = 48.dp).wrapContentHeight(Alignment.CenterVertically))
                        TermsLink("Điều khoản sử dụng",!vm.loading) { vm.terms(false) }
                        Text("và",Modifier.heightIn(min = 48.dp).wrapContentHeight(Alignment.CenterVertically))
                        TermsLink("Chính sách bảo mật",!vm.loading) { vm.terms(true) }
                    }
                }
                vm.errors["consent"]?.let { Text(it,color = C.Error,modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            }
        } else if(!staff) LinkButton("Quên mật khẩu?",vm::forgot,Modifier.align(Alignment.End),enabled = !vm.loading)
        if(vm.feedback.isNotEmpty()) {
            if(vm.success) Surface(shape = RoundedCornerShape(8.dp),color = androidx.compose.ui.graphics.Color(0xFFEFF8F2),border = BorderStroke(1.dp,C.Success)) {
                Text(vm.feedback,Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },color = C.Success)
            } else Text(vm.feedback,color = C.Error,modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        if(vm.staffRequired) LinkButton("Chuyển sang đăng nhập nhân viên",{ vm.navigate(Screen.STAFF) },icon = "arrow")
        ActionButton(if(vm.loading) { if(signup) "Đang tạo tài khoản…" else "Đang đăng nhập…" } else if(signup) "Tạo tài khoản" else "Đăng nhập",::submit,Modifier.fillMaxWidth().testTag("auth-submit"),loading = vm.loading,icon = "arrow")
        if(vm.loading) Muted(if(signup) "Đang tạo tài khoản…" else "Đang xác thực thông tin…",Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        if(staff) Muted("Tài khoản do quản trị viên cấp.",Modifier.align(Alignment.CenterHorizontally))
        else {
            HorizontalDivider(color = C.Line)
            Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.Center,verticalAlignment = Alignment.CenterVertically) {
                LinkButton("Nhân viên đăng nhập?",{ vm.navigate(Screen.STAFF) },icon = "user")
                CineplexIcon("arrow",color = C.Primary)
            }
        }
    }
}
@Composable
private fun TermsLink(text: String, enabled: Boolean, action: () -> Unit) {
    TextButton(action,enabled = enabled,modifier = Modifier.heightIn(min = 48.dp),contentPadding = PaddingValues(0.dp),shape = RoundedCornerShape(8.dp)) {
        Text(text,color = C.Primary,textDecoration = TextDecoration.Underline,fontWeight = FontWeight.Normal)
    }
}
