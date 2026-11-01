package com.nhom8.cineplex.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.nhom8.cineplex.R
import com.nhom8.cineplex.data.ServerVoucher
import com.nhom8.cineplex.ui.CommerceState

private val previewVouchers = listOf(
    ServerVoucher("FIRST50", "Chào bạn mới · Giảm 50%", "Giảm 50% tổng bill lần mua đầu. Không giới hạn mức giảm; không cộng dồn.", "0", "welcome"),
    ServerVoucher("CINE20", "Hẹn hò màn ảnh · Giảm 20.000đ", "Tổng bill từ 300.000đ; không cộng dồn.", "300000", "cinema")
)
@Composable
private fun VoucherCard(v: ServerVoucher, action: String? = null, enabled: Boolean = true, onClick: () -> Unit = {}) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().testTag("voucher-${v.code}")) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(if (v.illustration == "welcome") R.drawable.voucher_welcome else R.drawable.combo_two),
                "Minh hoạ ${v.title}", Modifier.size(72.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(v.title, fontWeight = FontWeight.Bold)
                Text(v.description, style = MaterialTheme.typography.bodySmall)
                Text(v.code, color = Color(0xFF8C2945), style = MaterialTheme.typography.labelMedium)
                action?.let { TextButton(onClick, enabled = enabled) { Text(it) } }
            }
        }
    }
}
@Composable
fun VoucherPicker(items: List<ServerVoucher>, selected: String, subtotal: Long, enabled: Boolean, choose: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton({ open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth().testTag("commerce-promo")) {
        Text(if (selected.isEmpty()) "Chọn voucher" else "Đổi voucher · $selected")
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text("Chọn voucher") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Mỗi đơn dùng một voucher. Giá cuối do máy chủ xác nhận.")
                TextButton({ choose(""); open = false }) { Text("Không dùng voucher") }
                if (items.isEmpty()) Text("Bạn chưa có voucher khả dụng.")
                items.forEach { v ->
                    val eligible = subtotal >= v.minimum.toLong()
                    VoucherCard(v, if (eligible) "Chọn ${v.code}" else "Chưa đủ giá trị đơn", eligible) { choose(v.code); open = false }
                }
            }
        }, confirmButton = { TextButton({ open = false }) { Text("Đóng") } })
}
@Composable fun DemoVoucherPicker(choose: (String) -> Unit) = VoucherPicker(previewVouchers, "", Long.MAX_VALUE, true, choose)
@Composable
fun VoucherWalletScreen(state: CommerceState, back: () -> Unit) {
    val live = state.live
    LaunchedEffect(live) { live?.wallet() }
    Scaffold(topBar = { TextButton(back) { Text("‹ Voucher của tôi") } }, containerColor = Color(0xFFF6F7F9)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Ưu đãi dành cho bạn", style = MaterialTheme.typography.headlineSmall)
            Text("Chọn voucher ở bước kiểm tra đơn. Voucher lần đầu chỉ dành cho tài khoản chưa mua vé; hủy hoặc hết hạn giữ ghế sẽ trả lại lượt dùng.")
            if (live == null) Text("Bản xem trước · không phải voucher thật")
            val items = live?.vouchers ?: previewVouchers
            if (items.isEmpty()) Text("Chưa có voucher khả dụng. Thử tải lại.")
            items.forEach { VoucherCard(it) }
            live?.let {
                if (it.message.isNotBlank()) Text(it.message)
                TextButton(it::wallet) { Text("Tải lại voucher") }
            }
        }
    }
}
