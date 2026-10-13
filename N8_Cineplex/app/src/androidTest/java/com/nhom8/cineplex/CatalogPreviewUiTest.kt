package com.nhom8.cineplex

import android.graphics.Bitmap
import android.os.Build
import android.view.*
import android.view.inspector.WindowInspector
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.*
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.theme.CineplexMockTheme
import org.junit.*
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class CatalogPreviewUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var vm: CineplexViewModel
    private var elapsed = 0L
    @Before fun setup() {
        val film = MockMovies.all[0].copy(id = "preview-film",name = "Phim có IMAX, SCREENX và GOLD CLASS",soon = false,trailerYoutubeId = "xGiimAKN0TM")
        val slots = listOf(
            Triple("first","07:10","IMAX_2D"),Triple("last","13:50","IMAX_2D"),Triple("screenx","11:40","SCREENX"),Triple("gold","09:00","GOLD_CLASS")
        ).map { (id,time,format) -> ShowtimeDto(id,film.id,"fixture:$id","2026-10-13T${time}:00.000Z","2026-10-13T16:30:00.000Z",format,"150000","ACTIVE",AuditoriumDto("room-$format",format,if(format == "GOLD_CLASS") "GOLD" else "IMAX",496,false),"Phụ đề Việt",if(id == "gold") 32 else 480,if(id == "gold") 32 else 496) }
        val catalog = object : CatalogRepository {
            override suspend fun dates() = listOf("2026-10-13")
            override suspend fun movies(date: String) = CatalogDay(listOf(film),slots,"2026-10-13T07:09:59.000Z")
            override suspend fun movie(id: String) = film
            override suspend fun showtimes(id: String,date: String) = ShowtimesResponse(date,slots,"2026-10-13T07:09:59.000Z")
        }
        vm = CineplexViewModel(MockAccountAdapter(MockAccountRepository()),catalog,{ "2026-10-13" },{ elapsed })
        compose.setContent { CineplexMockTheme { CineplexApp(vm) } }
        compose.runOnUiThread { vm.update(AuthForm(email = "user@cineplex.test",password = "123456")); vm.submit() }
        compose.waitUntil(5_000) { !vm.catalogLoading && vm.filteredMovies.isNotEmpty() }
    }
    private fun scrollTo(tag: String) { compose.onNodeWithTag("movie-schedules").performScrollToNode(hasTestTag(tag)); compose.onNodeWithTag(tag).performScrollTo() }
    @Test fun groupsCountsReadOnlyInteractionAndExactTimeRemoval() {
        scrollTo("showtime-first")
        compose.onNodeWithTag("format-IMAX").assertExists()
        compose.onAllNodesWithText("Còn 480/496").assertCountEquals(3)
        screenshot("preview-schedules")
        compose.onNodeWithTag("showtime-first").performClick()
        compose.onNodeWithText("Thông tin suất chiếu").assertIsDisplayed()
        compose.onNodeWithText("Chưa hỗ trợ đặt vé",substring = true).assertIsDisplayed()
        compose.onNodeWithText("Đã hiểu").performClick()
        compose.runOnUiThread { elapsed = 1000; vm.tickCatalogClock() }
        compose.onNodeWithTag("showtime-first").assertDoesNotExist()
        compose.runOnUiThread { vm.selectTimeWindow(5) }
        compose.onNodeWithTag("showtime-gold").assertDoesNotExist()
        compose.onNodeWithTag("showtime-screenx").assertExists()
        compose.runOnUiThread { elapsed = isoMillis("2026-10-13T13:50:00.000Z") - isoMillis("2026-10-13T07:09:59.000Z"); vm.tickCatalogClock() }
        compose.onNodeWithTag("movie-preview-film").assertDoesNotExist()
        scrollTo("time-window-0")
        compose.onNodeWithTag("movie-schedules").performScrollToNode(hasText("Hôm nay không còn suất chiếu sắp tới."))
        compose.onNodeWithText("Hôm nay không còn suất chiếu sắp tới.").assertIsDisplayed()
        screenshot("preview-expired")
        Assert.assertNotNull(vm.session)
    }
    @Test fun trailerDialogOpensAndClosesWithoutChangingCatalog() {
        compose.onNodeWithTag("movie-schedules").performScrollToNode(hasContentDescription("Xem trailer Phim có IMAX, SCREENX và GOLD CLASS"))
        compose.onNodeWithContentDescription("Xem trailer Phim có IMAX, SCREENX và GOLD CLASS").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Trình phát trailer YouTube").assertIsDisplayed()
        screenshot("preview-trailer")
        compose.onNodeWithContentDescription("Đóng trailer").performClick()
        compose.onNodeWithContentDescription("Trình phát trailer YouTube").assertDoesNotExist()
        Assert.assertEquals(4,vm.slotsFor("preview-film").size)
    }
    @Test fun actualYoutubeTrailerPlaysInsideWebViewAfterUserTap() {
        if(Build.VERSION.SDK_INT < 29) return
        compose.onNodeWithTag("movie-schedules").performScrollToNode(hasContentDescription("Xem trailer Phim có IMAX, SCREENX và GOLD CLASS"))
        compose.onNodeWithContentDescription("Xem trailer Phim có IMAX, SCREENX và GOLD CLASS").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Trình phát trailer YouTube").assertIsDisplayed()
        var web: WebView? = null
        fun find(view: View): WebView? = if(view is WebView) view else if(view is ViewGroup) (0 until view.childCount).firstNotNullOfOrNull { find(view.getChildAt(it)) } else null
        compose.runOnIdle { web = WindowInspector.getGlobalWindowViews().firstNotNullOfOrNull(::find) }
        Assert.assertNotNull(web)
        val result = AtomicReference("")
        compose.waitUntil(45_000) {
            compose.runOnUiThread { web!!.evaluateJavascript("Boolean(window.trailerReady || window.trailerError)") { result.set(it) } }
            result.get() == "true"
        }
        screenshot("preview-trailer-ready")
        compose.runOnIdle { web!!.evaluateJavascript("JSON.stringify({error:window.trailerError||null,state:player.getPlayerState(),referrer:document.referrer,url:location.href})") { result.set(it) } }
        compose.waitUntil(5_000) { result.get().startsWith("\"") }
        Assert.assertFalse("YT diagnostics: ${result.get()}",result.get().contains("\\\"error\\\":15"))
        compose.onNodeWithContentDescription("Trình phát trailer YouTube").performTouchInput { click(center) }
        result.set("")
        compose.waitUntil(30_000) {
            compose.runOnUiThread { web!!.evaluateJavascript("window.player ? player.getPlayerState() : -99") { result.set(it) } }
            result.get() == "1"
        }
        screenshot("preview-trailer-playing")
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        result.set("")
        compose.waitUntil(10_000) {
            compose.runOnUiThread { web!!.evaluateJavascript("window.player ? player.getPlayerState() : -99") { result.set(it) } }
            result.get() == "2"
        }
        compose.onNodeWithContentDescription("Đóng trailer").performClick()
        compose.onNodeWithContentDescription("Trình phát trailer YouTube").assertDoesNotExist()
    }
    private fun screenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.getExternalFilesDir(null),"visual_checks").apply { mkdirs() }
        File(folder,"$name.png").outputStream().use { InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
}
