package com.nhom8.cineplex.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.nhom8.cineplex.data.MockMovies
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.components.*
import com.nhom8.cineplex.ui.theme.CineplexColors as C
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(vm: CineplexViewModel, grid: LazyGridState) {
    val scope = rememberCoroutineScope()
    val filtered = MockMovies.search(vm.query,vm.soon)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = if(maxWidth >= 1024.dp) 4 else if(maxWidth >= 768.dp) 3 else 2
        Column(Modifier.fillMaxSize().imePadding()) {
            LazyVerticalGrid(GridCells.Fixed(columns),state = grid,modifier = Modifier.weight(1f),contentPadding = PaddingValues(24.dp),horizontalArrangement = Arrangement.spacedBy(24.dp),verticalArrangement = Arrangement.spacedBy(24.dp)) {
                item(key = "header",span = { GridItemSpan(columns) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.SpaceBetween) { CineplexLogo(); Avatar(vm::account) }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Muted("Xin chào, ${vm.session?.name.orEmpty()}")
                            Text("Hôm nay, bạn muốn xem gì?",style = MaterialTheme.typography.headlineMedium)
                        }
                        CinemaBanner(browse = { scope.launch { grid.animateScrollToItem(1) } })
                    }
                }
                item(key = "search-tabs",span = { GridItemSpan(columns) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        SearchField(vm.query,{ vm.query = it })
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            PrototypeTabs(listOf("Đang chiếu","Sắp chiếu"),if(vm.soon) 1 else 0,{ vm.soon = it == 1 })
                            Muted("${filtered.size} phim · ${if(vm.soon) "Sắp chiếu" else "Đang chiếu"}")
                        }
                    }
                }
                items(filtered,key = { it.id }) { movie -> MovieCard(movie) { vm.openMovie(movie) } }
                if(filtered.isEmpty()) item(span = { GridItemSpan(columns) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Chưa tìm thấy phim",style = MaterialTheme.typography.headlineMedium)
                        Muted("Thử tên phim khác hoặc đổi mục phim.")
                        ActionButton("Xóa tìm kiếm",{ vm.query = "" },secondary = true)
                    }
                }
            }
            Column(Modifier.background(C.Surface)) {
                HorizontalDivider(color = C.Line)
                Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Trang chủ" to "home","Phim" to "film","Vé của tôi" to "ticket","Tài khoản" to "user").forEachIndexed { index,(label,icon) ->
                        Column(Modifier.weight(1f).heightIn(min = 64.dp).clip(RoundedCornerShape(12.dp)).background(if(index == 0) C.Raised else C.Surface)
                            .clickable(role = Role.Button) { when(index) { 0 -> scope.launch { grid.animateScrollToItem(0) }; 3 -> vm.account(); else -> vm.coming(label) } }
                            .padding(vertical = 4.dp).semantics { if(index == 0) selected = true },horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.spacedBy(4.dp,Alignment.CenterVertically)) {
                            CineplexIcon(icon,color = if(index == 0) C.Primary else C.Muted)
                            Text(label,color = if(index == 0) C.Primary else C.Muted)
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun SearchField(value: String, change: (String) -> Unit) {
    BasicTextField(value,change,singleLine = true,modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Tìm kiếm phim" },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = C.Text),cursorBrush = SolidColor(C.Primary),keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        decorationBox = { inner ->
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).background(C.Surface,RoundedCornerShape(12.dp)).border(1.dp,C.Line,RoundedCornerShape(12.dp)).padding(start = 16.dp,end = 4.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CineplexIcon("search")
                Box(Modifier.weight(1f)) { if(value.isEmpty()) Text("Tìm tên phim bạn muốn xem",color = C.Muted); inner() }
                if(value.isNotEmpty()) IconButton(onClick = { change("") },modifier = Modifier.size(48.dp).semantics { contentDescription = "Xóa tìm kiếm" }) { CineplexIcon("close") }
                else Spacer(Modifier.width(12.dp))
            }
        }
    )
}
