package com.nhom8.cineplex.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.*
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.*
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nhom8.cineplex.model.Movie
import com.nhom8.cineplex.ui.components.CineplexIcon

fun trailerHtml(videoId: String, appId: String): String {
    require(Regex("[A-Za-z0-9_-]{11}").matches(videoId))
    require(Regex("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+").matches(appId))
    return """<!doctype html><html lang="vi"><head><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="referrer" content="strict-origin-when-cross-origin"><style>html,body{margin:0;height:100%;background:#000;color:#fff;font:14px sans-serif}#player{display:block;width:100%;height:100vh;min-height:200px}#status{padding:12px}#status:empty{display:none}</style></head><body><div id="status">Đang tải trailer…</div><div id="player"></div><script>
    var player;
    function onYouTubeIframeAPIReady(){player=new YT.Player('player',{width:innerWidth,height:innerHeight,videoId:'$videoId',playerVars:{origin:'https://$appId',autoplay:0,controls:1,playsinline:1,fs:0,rel:0},events:{onReady:function(e){e.target.setSize(innerWidth,innerHeight);e.target.getIframe().style.height=innerHeight+'px';window.trailerReady=true;document.getElementById('status').textContent='';},onError:function(e){window.trailerError=e.data;document.getElementById('status').textContent='Không phát được trailer trong app (mã '+e.data+'). Bạn có thể mở trên YouTube bên dưới.';}}});}
    </script><script src="https://www.youtube.com/iframe_api" onerror="document.getElementById('status').textContent='Không tải được trailer. Kiểm tra mạng rồi thử lại.'"></script></body></html>"""
}

@Composable
fun TrailerDialog(movie: Movie, close: () -> Unit) {
    val id = movie.trailerYoutubeId ?: return
    if(!Regex("[A-Za-z0-9_-]{11}").matches(id)) return
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val player = remember(id) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.mediaPlaybackRequiresUserGesture = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest) = request.isForMainFrame
            }
            loadDataWithBaseURL("https://${context.packageName}",trailerHtml(id,context.packageName),"text/html","UTF-8",null)
        }
    }
    DisposableEffect(player,lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if(event == Lifecycle.Event.ON_STOP) { player.evaluateJavascript("if(window.player){player.pauseVideo();}",null); player.onPause() }
            if(event == Lifecycle.Event.ON_RESUME) player.onResume()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); player.stopLoading(); player.loadUrl("about:blank"); player.destroy() }
    }
    Dialog(close,properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth().padding(16.dp),colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp),verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Trailer · ${movie.name}",Modifier.weight(1f),style = MaterialTheme.typography.titleMedium)
                    IconButton(close,Modifier.size(48.dp).semantics { contentDescription = "Đóng trailer" }) { CineplexIcon("close") }
                }
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val height = (maxWidth * 9f / 16f).coerceIn(200.dp,360.dp)
                    AndroidView(factory = { player },modifier = Modifier.fillMaxWidth().height(height).semantics { contentDescription = "Trình phát trailer YouTube" })
                }
                Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement = Arrangement.End) {
                    TextButton({ player.loadDataWithBaseURL("https://${context.packageName}",trailerHtml(id,context.packageName),"text/html","UTF-8",null) }) { Text("Thử lại") }
                    TextButton({
                        try { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.youtube.com/watch?v=$id"))) }
                        catch (_: ActivityNotFoundException) { Toast.makeText(context,"Không tìm thấy ứng dụng mở video.",Toast.LENGTH_SHORT).show() }
                    }) { Text("Mở trên YouTube") }
                }
            }
        }
    }
}
