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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.nhom8.cineplex.data.ApiSeatRepository
import kotlinx.coroutines.CancellationException
import com.nhom8.cineplex.model.Screen
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.SeatSelectionViewModel
import com.nhom8.cineplex.ui.CommerceState
import com.nhom8.cineplex.ui.components.CineplexDialog
import com.nhom8.cineplex.ui.screens.*
import com.nhom8.cineplex.ui.theme.*

class MainActivity : ComponentActivity() {
    private var pendingTicketId by mutableStateOf<String?>(null)
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        pendingTicketId = com.nhom8.cineplex.data.TicketPush.ticketId(intent.getStringExtra("ticketId") ?: intent.getStringExtra("ticket_id"))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT,android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT,android.graphics.Color.TRANSPARENT))
        val account = ApiAccountRepository(applicationContext)
        pendingTicketId = com.nhom8.cineplex.data.TicketPush.ticketId(intent.getStringExtra("ticketId") ?: intent.getStringExtra("ticket_id"))
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) com.nhom8.cineplex.data.TicketPush.sync(applicationContext, account)
        })
        val vm = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = CineplexViewModel(account, ApiCatalogRepository()) as T
        })[CineplexViewModel::class.java]
        val seats = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SeatSelectionViewModel(ApiSeatRepository(applicationContext, account)) as T
        })[SeatSelectionViewModel::class.java]
        val commerce = ViewModelProvider(this)[CommerceState::class.java]
        val transactions = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = com.nhom8.cineplex.ui.TicketsState(com.nhom8.cineplex.data.ApiTicketsRepository(account)) as T
        })[com.nhom8.cineplex.ui.TicketsState::class.java]
        val live = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = com.nhom8.cineplex.ui.LiveCommerceState(com.nhom8.cineplex.data.ApiCommerceRepository(account), transactions) as T
        })[com.nhom8.cineplex.ui.LiveCommerceState::class.java]
        commerce.live = live
        val appVersion = ViewModelProvider(this, object: ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T: ViewModel> create(modelClass: Class<T>): T = com.nhom8.cineplex.ui.AppVersionState(com.nhom8.cineplex.data.ApiVersionRepository()) as T
        })[com.nhom8.cineplex.ui.AppVersionState::class.java]
        setContent {
            val permission = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) com.nhom8.cineplex.data.TicketPush.sync(applicationContext, account)
            }
            LaunchedEffect(vm.session?.id, pendingTicketId) {
                transactions.bindUser(vm.session?.id)
                if (vm.session?.role == com.nhom8.cineplex.model.Role.CUSTOMER) {
                    pendingTicketId?.let { id -> transactions.closePayment(); vm.tickets(); transactions.openTicket(id); pendingTicketId = null }
                    if (com.nhom8.cineplex.data.TicketPush.initialize(applicationContext)) {
                        val prefs = getSharedPreferences("ticket-notifications", MODE_PRIVATE)
                        if (android.os.Build.VERSION.SDK_INT >= 33 && !prefs.getBoolean("permission-requested", false)) {
                            prefs.edit().putBoolean("permission-requested", true).apply()
                            permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else com.nhom8.cineplex.data.TicketPush.sync(applicationContext, account)
                    }
                }
            }
            CineplexMockTheme { com.nhom8.cineplex.ui.screens.AppUpdateGate(appVersion) { CineplexApp(vm, seats, commerce) } }
        }
    }
}
@Composable
fun CineplexApp(vm: CineplexViewModel, seats: SeatSelectionViewModel = remember { SeatSelectionViewModel() }, commerce: CommerceState = remember { CommerceState() }) {
    // Keep movie filters/scroll across profile navigation; tokens are handled by the repository.
    val grid = rememberLazyListState()
    val signedIn = vm.session != null
    LaunchedEffect(vm.session?.id) { commerce.live?.transactions?.bindUser(vm.session?.id); if(!signedIn) { grid.scrollToItem(0); commerce.clear(); commerce.live?.transactions?.clear() } }
    fun seatBack() {
        if (seats.selected.isNotEmpty() || seats.pending != null || seats.confirming) seats.requestLeave()
        else { seats.release(); vm.back() }
    }
    BackHandler(vm.trailer != null || vm.notice != null || vm.dialog != null || vm.ageSheet || seats.leaveDialog || seats.expiredDialog || vm.screen in listOf(Screen.SIGNUP,Screen.STAFF,Screen.DETAIL,Screen.SHOWTIMES,Screen.SEAT_SUMMARY,Screen.SEAT_HANDOFF,Screen.PROFILE,Screen.VOUCHERS,Screen.TICKETS)) {
        when { seats.leaveDialog -> seats.stay(); seats.expiredDialog -> seats.dismissExpired(); vm.screen == Screen.SEAT_SUMMARY -> seatBack(); vm.screen == Screen.SEAT_HANDOFF && commerce.page in listOf(com.nhom8.cineplex.ui.CommercePage.PROCESSING, com.nhom8.cineplex.ui.CommercePage.UNKNOWN) -> commerce.transactionMessage = "Hãy kiểm tra kết quả giao dịch mẫu trước khi rời luồng."; vm.screen == Screen.SEAT_HANDOFF && commerce.page == com.nhom8.cineplex.ui.CommercePage.CHECKOUT -> commerce.page = com.nhom8.cineplex.ui.CommercePage.COMBOS; else -> vm.back() }
    }
    LaunchedEffect(vm.session?.id, vm.selectedShowtime?.id, vm.screen) {
        if(vm.session==null) seats.clearSession()
        if (vm.screen == Screen.SEAT_SUMMARY || vm.screen == Screen.SEAT_HANDOFF) vm.selectedShowtime?.let { seats.bind(vm.session?.id.orEmpty(), it.id) }
        else if(vm.screen==Screen.HOME) vm.session?.id?.let(seats::findResume)
    }
    val savedScreens = rememberSaveableStateHolder()
    val context = LocalContext.current
    val darkSeats = vm.screen == Screen.SEAT_SUMMARY
    SideEffect {
        (context as? android.app.Activity)?.let { activity ->
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                isAppearanceLightStatusBars = !darkSeats
                isAppearanceLightNavigationBars = !darkSeats
            }
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, vm) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) { seats.refreshClock(); vm.refreshProfile(); vm.refreshCatalog();  } }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(lifecycle, vm) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { vm.tickCatalogClock(); vm.refreshCatalog(force = false); delay(60_000 - vm.nowMillis % 60_000) } }
    }
    LaunchedEffect(lifecycle, seats, vm.screen) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try { seats.foreground(vm.screen in listOf(Screen.SEAT_SUMMARY, Screen.SEAT_HANDOFF)); while (true) { seats.refreshClock(); delay(1_000) } }
            finally { seats.foreground(false) }
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
    Surface(Modifier.fillMaxSize(),color = if(darkSeats) Color(0xFF111111) else CineplexColors.Background) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = 1280.dp).fillMaxSize()) {
                key(vm.screen) {
                    when(vm.screen) {
                        Screen.LOGIN,Screen.SIGNUP,Screen.STAFF -> AuthScreen(vm)
                        Screen.HOME -> Column(Modifier.fillMaxSize()) {
                            seats.resumeOrder?.let { order ->
                                Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
                                    Text("Bạn đang giữ ${order.seats.joinToString { it.seatCode }}",Modifier.weight(1f),maxLines=2)
                                    TextButton({vm.resumeHold(order.showtimeId)},enabled=!vm.scheduleChecking,modifier=Modifier.testTag("resume-hold")) {Text("Xem giữ chỗ")}
                                }
                            }
                            Box(Modifier.weight(1f)) {HomeScreen(vm,grid)}
                        }
                        Screen.DETAIL -> vm.movie?.let { selected -> savedScreens.SaveableStateProvider("detail-${selected.id}") { DetailExperienceScreen(vm,selected) } }
                        Screen.SHOWTIMES -> vm.movie?.let { selected -> savedScreens.SaveableStateProvider("showtimes-${selected.id}") { DetailShowtimesScreen(vm,selected) } }
                        Screen.SEAT_SUMMARY -> vm.movie?.let { selected -> savedScreens.SaveableStateProvider("seat-${vm.selectedShowtime?.id}") { SeatSelectionScreen(vm,seats,selected,::seatBack) } }
                        Screen.SEAT_HANDOFF -> vm.movie?.let { selected -> SeatHandoffScreen(vm,seats,selected,commerce) }
                        Screen.ADMIN -> AdminScreen(vm, commerce.live?.transactions)
                        Screen.STAFF_HOME -> StaffScreen(vm)
                        Screen.PROFILE -> ProfileScreen(vm)
                        Screen.VOUCHERS -> VoucherWalletScreen(commerce,vm::back,vm)
                        Screen.TICKETS -> TicketsScreen(vm,commerce.live?.transactions)
                    }
                }
                vm.notice?.let { CineplexDialog(it,vm::dismissNotice,vm::logout) }
                vm.dialog?.let { AccountFormDialog(vm, it) }
                vm.trailer?.let { TrailerDialog(it, vm::closeTrailer) }
            }
        }
    }
}
