package com.nhom8.cineplex.ui
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhom8.cineplex.BuildConfig
import com.nhom8.cineplex.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
class AppVersionState(private val repo: VersionBackend, val installedCode: Int = BuildConfig.VERSION_CODE): ViewModel() {
    var release by mutableStateOf<AppRelease?>(null); private set
    var error by mutableStateOf(""); private set
    var busy by mutableStateOf(false); private set
    val outdated get() = release?.let { installedCode < it.minimumVersionCode } ?: false
    fun check() {
        if(busy) return
        busy=true
        viewModelScope.launch {
            try { val r=repo.release();release=r;error="";if(installedCode>=r.minimumVersionCode) AppVersionWarning.required.value=false }
            catch(e: CancellationException) { throw e }
            catch(_: Exception) { error="Không kiểm tra được phiên bản. Vui lòng kiểm tra mạng và thử lại." }
            finally { busy=false }
        }
    }
}
