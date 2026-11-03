package com.nhom8.cineplex.ui.screens

import androidx.compose.foundation.Image
import android.graphics.drawable.ColorDrawable
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
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
private fun VoucherCard(v: ServerVoucher, action: String? = null, enabled: Boolean = true, onClick: () -> Unit = {}, selected: Boolean = false) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(if(selected) 2.dp else 1.dp, if(selected) Color(0xFF8C2945) else Color(0xFFECE3E7)), tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth().testTag("voucher-${v.code}")) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.width(94.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFCEEF2)).padding(8.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Image(painterResource(if (v.illustration == "welcome") R.drawable.voucher_welcome else R.drawable.combo_two), "Minh hoạ ${v.title}", Modifier.size(64.dp))
                Text(when(v.code){"FIRST50"->"50%";"CINE20"->"20.000đ";else->"ƯU ĐÃI"}, color=Color(0xFF8C2945), fontWeight=FontWeight.ExtraBold, fontSize=20.sp)
            }
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
    var draft by remember { mutableStateOf("") }
    val primary = Color(0xFF8C2945)
    Surface(onClick = { draft = selected; open = true }, enabled = enabled,
        shape = RoundedCornerShape(22.dp), color = Color.White,
        modifier = Modifier.fillMaxWidth().testTag("commerce-promo")) {
        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFFFF3D6)), contentAlignment = Alignment.Center) {
                Text("%", color = Color(0xFFBA7400), fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Ưu đãi", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(if(selected.isBlank()) "Chọn ưu đãi cho đơn vé" else "Đã chọn · $selected", color = if(selected.isBlank()) Color(0xFF756C73) else primary, fontSize = 12.sp)
            }
            Text("›", color = primary, fontSize = 30.sp)
        }
    }
    if (open) Dialog(onDismissRequest = { open = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                window.setBackgroundDrawable(ColorDrawable(0xFFFADCE6.toInt()))
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
        }
        val valid = draft.isBlank() || items.any { it.code == draft && subtotal >= it.minimum.toLong() }
        Surface(Modifier.fillMaxSize(), color = Color(0xFFF5F3F7)) {
            Scaffold(Modifier.fillMaxSize().systemBarsPadding(), containerColor = Color.Transparent,
                topBar = {
                    Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFFFADCE6), Color(0xFFF5F3F7)))).padding(horizontal = 12.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton({ open = false }, Modifier.size(48.dp).testTag("voucher-close").semantics { contentDescription = "Quay lại thanh toán" }) { Text("‹", fontSize = 32.sp, color = primary) }
                            Text("Ưu đãi", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("N8 CINEPLEX", color = primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }, bottomBar = {
                    Surface(shadowElevation = 8.dp, color = Color.White) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Ưu đãi đã chọn", color = Color(0xFF756C73), fontSize = 13.sp)
                                Text(draft.ifBlank { "Không sử dụng" }, color = primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Button({ choose(draft); open = false }, enabled = enabled && valid,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = primary)) {
                                Text("Áp dụng", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Một ưu đãi, thêm niềm vui", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Chọn 1 voucher cho vé và bắp nước. Giá cuối cùng sẽ được máy chủ kiểm tra khi áp dụng.", color = Color(0xFF756C73), fontSize = 13.sp)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Voucher của bạn", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        TextButton({ draft = "" }) { Text("Không dùng voucher", color = primary, fontSize = 12.sp) }
                    }
                    if(items.isEmpty()) Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                        Text("Bạn chưa có voucher khả dụng cho tài khoản này.", Modifier.fillMaxWidth().padding(24.dp), color = Color(0xFF756C73))
                    }
                    items.forEach { v ->
                        val eligible = subtotal >= v.minimum.toLong()
                        val isSelected = draft == v.code
                        VoucherCard(v, if(!eligible) "Chưa đủ giá trị đơn" else if(isSelected) "Bỏ ${v.code}" else "Chọn ${v.code}", eligible,
                            onClick = { draft = if(isSelected) "" else v.code }, selected = isSelected)
                    }
                    Text("Mỗi đơn chỉ dùng một voucher · Không cộng dồn", color = Color(0xFF756C73), fontSize = 12.sp)
                }
            }
        }
    }
}
@Composable fun DemoVoucherPicker(choose: (String) -> Unit) = VoucherPicker(previewVouchers, "", Long.MAX_VALUE, true, choose)
@Composable
fun VoucherWalletScreen(state: CommerceState, back: () -> Unit, vm: com.nhom8.cineplex.ui.CineplexViewModel? = null) {
    val live = state.live
    LaunchedEffect(live) { live?.wallet() }
    Scaffold(bottomBar={vm?.let {com.nhom8.cineplex.ui.components.CustomerNavigation(2,it::customerHome,it::account,it::coming,it::tickets,it::vouchers)}},topBar = { TextButton(back) { Text("‹ Voucher của tôi") } }, containerColor = Color(0xFFF6F7F9)) { padding ->
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
