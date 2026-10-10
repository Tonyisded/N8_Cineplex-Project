package com.nhom8.cineplex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.*
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.nhom8.cineplex.data.ApiAccountRepository
import kotlinx.coroutines.CancellationException
import com.nhom8.cineplex.model.Screen
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.components.CineplexDialog
import com.nhom8.cineplex.ui.screens.*
import com.nhom8.cineplex.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT,android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT,android.graphics.Color.TRANSPARENT))
        val vm = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = CineplexViewModel(ApiAccountRepository(applicationContext)) as T
        })[CineplexViewModel::class.java]
        setContent { CineplexMockTheme { CineplexApp(vm) } }
    }
}
@Composable
fun CineplexApp(vm: CineplexViewModel) {
    // Keep movie filters/scroll across profile navigation; tokens are handled by the repository.
    val grid = rememberLazyGridState()
    val signedIn = vm.session != null
    LaunchedEffect(signedIn) { if(!signedIn) grid.scrollToItem(0) }
    BackHandler(vm.notice != null || vm.dialog != null || vm.screen in listOf(Screen.SIGNUP,Screen.STAFF,Screen.DETAIL,Screen.PROFILE)) { vm.back() }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, vm) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) vm.refreshProfile() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(vm.googleRequested) {
        if (vm.googleRequested) {
            if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) vm.googleFailed(false)
            else try {
                val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
                val result = CredentialManager.create(context).getCredential(context, GetCredentialRequest.Builder().addCredentialOption(option).build())
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) vm.googleSuccess(GoogleIdTokenCredential.createFrom(credential.data).idToken)
                else vm.googleFailed(false)
            } catch (_: GetCredentialCancellationException) { vm.googleFailed(true) }
              catch (e: CancellationException) { throw e }
              catch (_: GetCredentialException) { vm.googleFailed(false) }
              catch (_: Exception) { vm.googleFailed(false) }
        }
    }
    Surface(Modifier.fillMaxSize(),color = CineplexColors.Background) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = 1280.dp).fillMaxSize()) {
                key(vm.screen) {
                    when(vm.screen) {
                        Screen.LOGIN,Screen.SIGNUP,Screen.STAFF -> AuthScreen(vm)
                        Screen.HOME -> HomeScreen(vm,grid)
                        Screen.DETAIL -> vm.movie?.let { MovieDetailScreen(vm,it) }
                        Screen.ADMIN -> AdminScreen(vm)
                        Screen.STAFF_HOME -> StaffScreen(vm)
                        Screen.PROFILE -> ProfileScreen(vm)
                    }
                }
                vm.notice?.let { CineplexDialog(it,vm::dismissNotice,vm::logout) }
                vm.dialog?.let { AccountFormDialog(vm, it) }
            }
        }
    }
}
