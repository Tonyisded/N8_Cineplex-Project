package com.nhom8.cineplex.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.components.ActionButton
import com.nhom8.cineplex.ui.components.Muted
import com.nhom8.cineplex.ui.theme.CineplexColors

@Composable
fun AccountFormDialog(vm: CineplexViewModel, action: String) {
    var name by remember(action) { mutableStateOf(vm.session?.name.orEmpty()) }
    var email by remember(action) { mutableStateOf(vm.session?.email ?: vm.form.email) }
    var current by remember(action) { mutableStateOf("") }
    var replacement by remember(action) { mutableStateOf("") }
    var confirm by remember(action) { mutableStateOf("") }
    Dialog(onDismissRequest = vm::closeDialog) {
        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), color = CineplexColors.Surface) {
            Column(Modifier.fillMaxWidth().heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(24.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(when(action) { "forgot" -> "Quên mật khẩu"; "password" -> "Đổi mật khẩu"; else -> "Chỉnh sửa thông tin" }, style = MaterialTheme.typography.headlineSmall)
                if (action == "edit") {
                    OutlinedTextField(name, { name = it }, label = { Text("Họ và tên") }, enabled = !vm.loading, singleLine = true)
                    ActionButton("Lưu họ tên", { vm.saveName(name) }, Modifier.fillMaxWidth(), loading = vm.loading)
                    HorizontalDivider()
                }
                if (action != "password") OutlinedTextField(email, { email = it }, label = { Text(if(action == "forgot") "Email" else "Email mới") }, enabled = !vm.loading, singleLine = true)
                if (action == "password" || (action == "edit" && vm.session?.hasPassword == true)) OutlinedTextField(current, { current = it }, label = { Text("Mật khẩu hiện tại") }, enabled = !vm.loading, singleLine = true, visualTransformation = PasswordVisualTransformation())
                if (action == "password") {
                    OutlinedTextField(replacement, { replacement = it }, label = { Text("Mật khẩu mới") }, enabled = !vm.loading, singleLine = true, visualTransformation = PasswordVisualTransformation())
                    OutlinedTextField(confirm, { confirm = it }, label = { Text("Xác nhận mật khẩu mới") }, enabled = !vm.loading, singleLine = true, visualTransformation = PasswordVisualTransformation())
                }
                if (action == "edit") Muted("Email hiện tại chỉ thay đổi sau khi xác nhận thư gửi đến email mới.")
                if (vm.feedback.isNotBlank()) Text(vm.feedback, color = CineplexColors.Error)
                ActionButton(when(action) { "forgot" -> "Gửi email đặt lại mật khẩu"; "password" -> "Đổi mật khẩu"; else -> if(vm.session?.hasPassword == false) "Xác thực Google và đổi email" else "Gửi xác nhận đổi email" },
                    { when(action) { "forgot" -> vm.sendForgot(email); "password" -> vm.password(current, replacement, confirm); else -> vm.email(email, current) } }, Modifier.fillMaxWidth(), loading = vm.loading)
                ActionButton("Đóng", vm::closeDialog, Modifier.fillMaxWidth(), secondary = true, loading = vm.loading)
            }
        }
    }
}
