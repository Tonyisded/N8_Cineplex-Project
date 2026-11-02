package com.nhom8.cineplex.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.nhom8.cineplex.ui.TicketsState
import com.nhom8.cineplex.data.PaymentIssue
import com.nhom8.cineplex.model.Role
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.components.*
import com.nhom8.cineplex.ui.theme.CineplexColors as C

@Composable
fun AdminScreen(vm: CineplexViewModel, payments: TicketsState? = null) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween,verticalAlignment = Alignment.CenterVertically) { CineplexLogo(); Avatar(vm::account,admin = true, session = vm.session) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp),verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = androidx.compose.foundation.shape.CircleShape,color = C.Raised,border = BorderStroke(1.dp,C.Line),modifier = Modifier.size(48.dp)) {
                vm.session?.let { UserAvatar(it, Modifier.fillMaxSize()) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("ADMIN · Cineplex Staff",color = C.Primary,fontWeight = FontWeight.Bold); Text(vm.session?.name.orEmpty(),fontWeight = FontWeight.Bold) }
        }
        Text("Chào mừng trở lại.",style = MaterialTheme.typography.headlineMedium)
        CinemaBanner(staff = true)
        if (vm.session?.role == Role.ADMIN && payments != null) PaymentIssuesPanel(payments)
        Column(Modifier.padding(vertical = 8.dp),verticalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(Triple("film","Quản lý phim","Danh mục và thông tin phim"),Triple("clock","Quản lý suất chiếu","Lịch chiếu và phòng chiếu"),Triple("ticket","Quản lý đặt vé","Theo dõi các lượt đặt vé")).forEach { (icon,title,description) ->
                Column(Modifier.fillMaxWidth().clickable(role = androidx.compose.ui.semantics.Role.Button) { vm.coming(title) }) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 104.dp).padding(horizontal = 16.dp,vertical = 24.dp),horizontalArrangement = Arrangement.spacedBy(16.dp),verticalAlignment = Alignment.CenterVertically) {
                        CineplexIcon(icon,Modifier.size(32.dp),C.Primary)
                        Column(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title,style = MaterialTheme.typography.titleMedium); Muted(description) }
                        CineplexIcon("arrow",color = C.Primary)
                    }
                    HorizontalDivider(color = C.Line)
                }
            }
        }
        ActionButton("Đăng xuất",vm::logout,Modifier.widthIn(max = 440.dp).fillMaxWidth(),secondary = true,icon = "exit")
    }
}

@Composable
fun PaymentIssuesPanel(state: TicketsState) {
    LaunchedEffect(state) { state.loadIssues() }
    var selected by remember { mutableStateOf<PaymentIssue?>(null) }
    var note by remember { mutableStateOf("") }
    var bankRefunded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Vấn đề thanh toán / hoàn tiền", style = MaterialTheme.typography.titleLarge)
        TextButton(state::loadIssues, enabled = !state.busy) { Text("Tải lại giao dịch cần kiểm tra") }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.error.isNotBlank()) Text(state.error, color = C.Error)
        if (!state.busy && state.error.isBlank() && state.issues.isEmpty()) Text("Không có vấn đề thanh toán đang chờ xử lý.")
        state.issues.forEach { issue ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${issue.kind} · ${issue.amount}đ", fontWeight = FontWeight.Bold)
                    Text(issue.reason ?: "Cần kiểm tra giao dịch")
                    Text("Đơn: ${issue.orderId ?: "Chưa xác định"}")
                    Text("Trạng thái: ${issue.operationState}")
                    TextButton({ selected = issue; note = ""; bankRefunded = issue.kind == "REFUND" }, enabled = !state.busy) { Text("Xem và ghi nhận xử lý") }
                }
            }
        }
    }
    selected?.let { issue ->
        AlertDialog(onDismissRequest = { selected = null }, title = { Text("Xử lý vấn đề thanh toán") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Mã vấn đề: ${issue.id}")
                Text("Mã giao dịch: ${issue.providerTransactionId ?: issue.providerObjectId ?: "Chưa có"}")
                Text("Tài khoản: ${issue.accountMasked ?: "Chưa xác minh"}")
                Text("Thao tác này chỉ ghi nhận kết quả xử lý thực tế, không chuyển tiền hoặc phát vé. Không xác nhận đã hoàn nếu khách chưa được trả tiền.")
                if (issue.kind != "REFUND") Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(bankRefunded, { bankRefunded = it }); Text("Đã hoàn bằng ngân hàng")
                }
                if (issue.operationState == "UNKNOWN") Text("Lệnh chi chưa rõ kết quả. Phải tra soát trước; không được ghi nhận hoàn thủ công.", color = C.Error)
                OutlinedTextField(note, { note = it.take(1000) }, label = { Text("Tham chiếu ngân hàng / ghi chú xử lý") }, minLines = 2)
            } },
            confirmButton = { TextButton({ state.resolveIssue(issue, bankRefunded, note); selected = null }, enabled = !state.busy && issue.operationState != "UNKNOWN" && note.trim().length >= 8) { Text(if (bankRefunded) "Xác nhận đã hoàn ngân hàng" else "Ghi nhận đã xử lý") } },
            dismissButton = { TextButton({ selected = null }) { Text("Đóng") } })
    }
}
