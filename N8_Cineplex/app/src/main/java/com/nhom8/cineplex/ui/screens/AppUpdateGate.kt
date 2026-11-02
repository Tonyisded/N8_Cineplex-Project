package com.nhom8.cineplex.ui.screens
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.nhom8.cineplex.data.AppVersionWarning
import com.nhom8.cineplex.ui.AppVersionState
import kotlinx.coroutines.delay
@Composable fun AppUpdateGate(state: AppVersionState, content: @Composable () -> Unit) {
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val forced by AppVersionWarning.required.collectAsState()
    val context=LocalContext.current
    LaunchedEffect(forced) { if(forced) state.check() }
    LaunchedEffect(lifecycle,state) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { while(true) {state.check();delay(60_000)} } }
    if(state.release!=null && !state.outdated && !forced) { content();return }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(28.dp),verticalArrangement=Arrangement.spacedBy(18.dp,Alignment.CenterVertically),horizontalAlignment=Alignment.CenterHorizontally) {
            Text("N8 CINEPLEX",style=MaterialTheme.typography.titleLarge)
            Text(if(state.outdated||forced) "Phiên bản ứng dụng đã cũ, vui lòng cập nhật lên phiên bản mới." else if(state.error.isNotBlank()) state.error else "Đang kiểm tra phiên bản ứng dụng…",style=MaterialTheme.typography.titleMedium)
            if(state.busy) CircularProgressIndicator()
            if(state.outdated||forced) state.release?.let { r ->
                Text("Phiên bản hiện tại: ${state.installedCode} · Phiên bản mới: ${r.latestVersionCode}")
                Button({ runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(r.downloadUrl))) } },Modifier.fillMaxWidth()) { Text("Cập nhật ứng dụng") }
                Text("Tải APK mới và xác nhận cài đặt trên thiết bị. Không cần xóa app hoặc dữ liệu tài khoản.",style=MaterialTheme.typography.bodySmall)
            }
            TextButton(state::check,enabled=!state.busy) { Text("Kiểm tra lại") }
        }
    }
}
