package com.nhom8.cineplex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nhom8.cineplex.ui.theme.CineplexColors as C

@Composable
fun CustomerNavigation(selectedIndex: Int, onHome: () -> Unit, onAccount: () -> Unit, onComing: (String) -> Unit) {
    Column(Modifier.background(C.Surface)) {
        HorizontalDivider(color = C.Line)
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Trang chủ" to "home", "Phim" to "film", "Vé của tôi" to "ticket", "Tài khoản" to "user").forEachIndexed { index, (label, icon) ->
                val active = index == selectedIndex
                val color = if (active) C.Primary else C.Muted
                Column(
                    Modifier.weight(1f).heightIn(min = 64.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (active) C.Raised else C.Surface)
                        .clickable(role = Role.Button) {
                            when (index) { 0 -> onHome(); 3 -> onAccount(); else -> onComing(label) }
                        }
                        .padding(vertical = 4.dp).semantics { selected = active },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                ) {
                    CineplexIcon(icon, color = color)
                    Text(label, color = color)
                }
            }
        }
    }
}
