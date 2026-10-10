package com.nhom8.cineplex.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.components.*
import com.nhom8.cineplex.ui.theme.CineplexColors as C

@Composable
fun AdminScreen(vm: CineplexViewModel) {
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
