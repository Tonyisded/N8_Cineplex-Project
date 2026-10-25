package com.nhom8.cineplex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.nhom8.cineplex.data.ApiCatalogRepository
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import com.nhom8.cineplex.data.ApiAccountRepository
import kotlinx.coroutines.CancellationException
import com.nhom8.cineplex.model.Screen
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.SeatSelectionViewModel
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
            override fun <T : ViewModel> create(modelClass: Class<T>): T = CineplexViewModel(ApiAccountRepository(applicationContext), ApiCatalogRepository()) as T
        })[CineplexViewModel::class.java]
        val seats = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SeatSelectionViewModel() as T
        })[SeatSelectionViewModel::class.java]
        setContent { CineplexMockTheme { CineplexApp(vm, seats) } }
    }
}
@Composable
fun CineplexApp(vm: CineplexViewModel, seats: SeatSelectionViewModel = remember { SeatSelectionViewModel() }) {
    // Keep movie filters/scroll across profile navigation; tokens are handled by the repository.
    val grid = rememberLazyListState()
    val signedIn = vm.session != null
    LaunchedEffect(signedIn) { if(!signedIn) grid.scrollToItem(0) }
    fun seatBack() {
        if (seats.selected.isNotEmpty() || seats.pending != null || seats.confirming) seats.requestLeave()
        else { seats.release(); vm.back() }
    }
    BackHandler(vm.trailer != null || vm.notice != null || vm.dialog != null || vm.ageSheet || seats.leaveDialog || seats.expiredDialog || vm.screen in listOf(Screen.SIGNUP,Screen.STAFF,Screen.DETAIL,Screen.SHOWTIMES,Screen.SEAT_SUMMARY,Screen.SEAT_HANDOFF,Screen.PROFILE)) {
        when { seats.leaveDialog -> seats.stay(); seats.expiredDialog -> seats.dismissExpired(); vm.screen == Screen.SEAT_SUMMARY -> seatBack(); else -> vm.back() }
    }
    LaunchedEffect(vm.session?.id, vm.selectedShowtime?.id, vm.screen) {
        if (vm.screen == Screen.SEAT_SUMMARY || vm.screen == Screen.SEAT_HANDOFF) vm.selectedShowtime?.let { seats.bind("${vm.session?.id}:${it.id}") }
        else seats.release()
    }
    val savedScreens = rememberSaveableStateHolder()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, vm) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) { seats.refreshClock(); vm.refreshProfile(); vm.refreshCatalog() } }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(lifecycle, vm) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { vm.tickCatalogClock(); vm.refreshCatalog(force = false); delay(60_000 - vm.nowMillis % 60_000) } }
    }
    LaunchedEffect(lifecycle, seats) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { seats.refreshClock(); delay(1_000) }
        }
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
                        Screen.DETAIL -> vm.movie?.let { selected -> savedScreens.SaveableStateProvider("detail-${selected.id}") { DetailExperienceScreen(vm,selected) } }
                        Screen.SHOWTIMES -> vm.movie?.let { selected -> savedScreens.SaveableStateProvider("showtimes-${selected.id}") { DetailShowtimesScreen(vm,selected) } }
                        Screen.SEAT_SUMMARY -> vm.movie?.let { selected -> SeatSelectionScreen(vm,seats,selected,::seatBack) }
                        Screen.SEAT_HANDOFF -> vm.movie?.let { selected -> SeatHandoffScreen(vm,seats,selected) }
                        Screen.ADMIN -> AdminScreen(vm)
                        Screen.STAFF_HOME -> StaffScreen(vm)
                        Screen.PROFILE -> ProfileScreen(vm)
                    }
                }
                vm.notice?.let { CineplexDialog(it,vm::dismissNotice,vm::logout) }
                vm.dialog?.let { AccountFormDialog(vm, it) }
                vm.trailer?.let { TrailerDialog(it, vm::closeTrailer) }
            }
        }
    }
}
