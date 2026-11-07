package com.nhom8.cineplex.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.components.*
import com.nhom8.cineplex.ui.theme.CineplexColors as C

@Composable
fun EmailVerificationScreen(vm: CineplexViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp).testTag("email-verification"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CineplexLogo(96.dp)
        Spacer(Modifier.height(20.dp))
        Surface(
            modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth().testTag("required-email-form").semantics { paneTitle = "Xác minh email bắt buộc" },
            shape = RoundedCornerShape(20.dp),
            color = C.Surface,
            border = BorderStroke(1.dp, C.Line),
            shadowElevation = 2.dp,
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(shape = CircleShape, color = C.Raised) {
                    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) { CineplexIcon("mail", color = C.Primary) }
                }
                Text("Vui lòng xác minh email", fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Bạn cần xác minh email để tiếp tục sử dụng N8 Cineplex.", color = C.Muted, textAlign = TextAlign.Center)
                OutlinedTextField(
                    value = vm.verificationEmail, onValueChange = {}, readOnly = true, singleLine = true,
                    label = { Text("Email cần xác minh") }, modifier = Modifier.fillMaxWidth(),
                )
                Text("Vào hộp thư của bạn, mở email từ N8 Cineplex và bấm liên kết xác minh. Nếu chưa nhận được, hãy kiểm tra thư rác hoặc gửi lại email. Liên kết có hiệu lực 30 phút.", color = C.Muted)
                if (vm.feedback.isNotBlank()) Text(vm.feedback, Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, color = if (vm.success) C.Success else C.Muted)
                ActionButton("Gửi lại email xác minh", vm::resend, Modifier.fillMaxWidth().testTag("verification-resend"), loading = vm.loading, icon = "mail")
                ActionButton("Đã xác minh? Đăng nhập", vm::verificationLogin, Modifier.fillMaxWidth().testTag("verification-login"), secondary = true, loading = vm.loading)
                LinkButton("Dùng tài khoản khác", vm::verificationLogin, enabled = !vm.loading)
            }
        }
    }
}
