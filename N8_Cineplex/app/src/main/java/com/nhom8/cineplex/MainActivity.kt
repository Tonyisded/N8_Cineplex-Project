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
        val vm = ViewModelProvider(this)[CineplexViewModel::class.java]
        setContent { CineplexMockTheme { CineplexApp(vm) } }
    }
}
@Composable
fun CineplexApp(vm: CineplexViewModel) {
    // Retained across detail navigation; no password or session is saved to disk.
    val grid = rememberLazyGridState()
    val signedIn = vm.session != null
    LaunchedEffect(signedIn) { if(!signedIn) grid.scrollToItem(0) }
    BackHandler(vm.notice != null || vm.screen in listOf(Screen.SIGNUP,Screen.STAFF,Screen.DETAIL)) { vm.back() }
    Surface(Modifier.fillMaxSize(),color = CineplexColors.Background) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = 1280.dp).fillMaxSize()) {
                key(vm.screen) {
                    when(vm.screen) {
                        Screen.LOGIN,Screen.SIGNUP,Screen.STAFF -> AuthScreen(vm)
                        Screen.HOME -> HomeScreen(vm,grid)
                        Screen.DETAIL -> vm.movie?.let { MovieDetailScreen(vm,it) }
                        Screen.ADMIN -> AdminScreen(vm)
                    }
                }
                vm.notice?.let { CineplexDialog(it,vm::dismissNotice,vm::logout) }
            }
        }
    }
}
