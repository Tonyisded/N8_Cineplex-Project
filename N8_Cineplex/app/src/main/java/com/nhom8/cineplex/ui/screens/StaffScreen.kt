package com.nhom8.cineplex.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.components.*

@Composable fun StaffScreen(vm: CineplexViewModel) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        CineplexLogo(); Text("CINEPLEX STAFF"); Text(vm.session?.name.orEmpty())
        Muted("Các thao tác quét vé/check-in sẽ được bổ sung theo tuần nghiệp vụ.")
        ActionButton("Tài khoản", vm::account); ActionButton("Đăng xuất", vm::logout, secondary = true)
    }
}
