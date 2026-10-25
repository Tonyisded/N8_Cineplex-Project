package com.nhom8.cineplex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nhom8.cineplex.data.SeatAvailability
import com.nhom8.cineplex.ui.SeatSelectionViewModel
import com.nhom8.cineplex.ui.theme.CineplexMockTheme

/** Launch with: adb shell am start -n com.nhom8.cineplex/.SeatScenarioActivity */
class SeatScenarioActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CineplexMockTheme { ScenarioControls(SeatSelectionViewModel.debugCurrent?.get()) } }
    }
}

@Composable
private fun ScenarioControls(state: SeatSelectionViewModel?) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Điều khiển ghế mô phỏng · chỉ bản debug")
        if (state == null || state.showtimeId.isBlank()) {
            Text("Hãy mở màn chọn ghế trong ứng dụng trước.")
        } else {
            ScenarioButton("Người khác giữ A2") { state.debugChange("A2", SeatAvailability.HELD) }
            ScenarioButton("B4 đã bán") { state.debugChange("B4", SeatAvailability.SOLD) }
            ScenarioButton("Giải phóng A2, B4") {
                state.debugChange("A2", SeatAvailability.AVAILABLE)
                state.debugChange("B4", SeatAvailability.AVAILABLE)
            }
            ScenarioButton("Lần chọn F6 tiếp theo xung đột") { state.debugFailNext("F6") }
            ScenarioButton("Lần chọn J1–J2 tiếp theo xung đột") { state.debugFailNext("J1-J2") }
            ScenarioButton("Chỉ J1 bị giữ → khóa cả cặp J1–J2") { state.debugChangeCode("J1", SeatAvailability.HELD) }
            ScenarioButton("Giải phóng J1") { state.debugChangeCode("J1", SeatAvailability.AVAILABLE) }
            ScenarioButton("Mất kết nối") { state.debugOffline() }
            ScenarioButton("Kết nối lại và đồng bộ") { state.debugReconnect() }
            ScenarioButton("Kết nối lại, mất ghế đang chọn") {
                state.selected.firstOrNull()?.let { state.debugChange(it, SeatAvailability.HELD) }
                state.debugReconnect()
            }
            ScenarioButton("Còn 01:59") { state.debugSetRemaining(119_000) }
            ScenarioButton("Hết thời gian") { state.debugSetRemaining(-1) }
            ScenarioButton("Đặt lại phiên mô phỏng") { state.debugReset() }
            Text("Quay lại ứng dụng để xem kết quả. Các nút này không có trong bản release hoặc màn khách hàng.")
        }
    }
}

@Composable
private fun ScenarioButton(label: String, action: () -> Unit) {
    Button(action, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(label) }
}
