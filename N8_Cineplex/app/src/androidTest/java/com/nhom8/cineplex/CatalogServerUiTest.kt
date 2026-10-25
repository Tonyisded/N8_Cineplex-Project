package com.nhom8.cineplex

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.AuthForm
import com.nhom8.cineplex.model.Screen
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.theme.CineplexMockTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CatalogServerUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun liveVpsCatalogMetadataShowtimesSearchAndUpcomingState() = runBlocking {
        // Fixture session only; catalog uses real HTTPS VPS and never writes shared Auth users.
        val api = ApiCatalogRepository()
        val films = api.movies(vietnamDate()).movies
        Assert.assertTrue("VPS catalog must have today's imported movies", films.isNotEmpty())
        val vm = CineplexViewModel(MockAccountAdapter(MockAccountRepository()), api)
        compose.setContent { CineplexMockTheme { CineplexApp(vm) } }
        compose.runOnUiThread { vm.update(AuthForm(email = "user@cineplex.test", password = "123456")); vm.submit() }
        compose.waitUntil(30_000) { vm.movies.isNotEmpty() && !vm.catalogLoading }
        Assert.assertEquals(films.map { it.id }.toSet(), vm.movies.map { it.id }.toSet())
        compose.onNode(hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).performScrollToNode(hasText("Đang chiếu"))
        compose.onAllNodes(hasText("CGV Landmark 81 ·", substring = true)).assertCountEquals(0)
        compose.onNodeWithText("Làm mới").assertDoesNotExist()
        compose.onAllNodes(hasText(" phim · ", substring = true)).assertCountEquals(0)
        val selected = films.first()
        val homeSlot = vm.slotsFor(selected.id).first()
        compose.onNodeWithTag("movie-schedules").performScrollToNode(hasTestTag("showtime-${homeSlot.id}"))
        compose.onNodeWithTag("showtime-${homeSlot.id}").performScrollTo().assertIsDisplayed()
        Assert.assertTrue(compose.onAllNodesWithText(seatSummary(homeSlot)).fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithText("Đặt vé").assertDoesNotExist()
        screenshot("catalog-live-schedules")
        compose.runOnUiThread { vm.query = selected.name; vm.openMovie(selected) }
        compose.waitUntil(30_000) { !vm.detailLoading }
        Assert.assertEquals("",vm.detailError)
        val slots = api.showtimes(selected.id,vietnamDate()).items
        Assert.assertEquals(slots.map { it.id }.toSet(),vm.showtimes.map { it.id }.toSet())
        compose.onNodeWithText("Chi tiết phim").assertIsDisplayed()
        compose.onNodeWithText(selected.name).assertExists()
        screenshot("catalog-live-detail")
        val soon = api.comingSoon()
        Assert.assertTrue("VPS coming-soon must publish at least one movie", soon.isNotEmpty())
        val upcoming = soon.first()
        val upcomingDate = upcoming.releaseDate
        Assert.assertNotNull("Coming-soon movies must expose a release date", upcomingDate)
        val upcomingRelease = displayCalendarDate(upcomingDate!!)
        compose.runOnUiThread { vm.back(); vm.query = ""; vm.soon = true }
        compose.waitUntil(30_000) { !vm.soonLoading }
        if (vm.soonError.isNotBlank()) compose.runOnUiThread { vm.refreshComingSoon() }
        compose.waitUntil(30_000) { !vm.soonLoading && vm.soonError.isEmpty() }
        Assert.assertEquals(soon.map { it.id }.toSet(), vm.comingSoonMovies.map { it.id }.toSet())
        compose.onNodeWithTag("catalog-dates").assertDoesNotExist()
        compose.onNodeWithTag("time-windows").assertDoesNotExist()
        val grid = compose.onNode(hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
        grid.performScrollToNode(hasContentDescription("Xem chi tiết ${upcoming.name}"))
        compose.onNodeWithContentDescription("Xem chi tiết ${upcoming.name}").assertIsDisplayed()
        Assert.assertTrue("Coming-soon card must show its Vietnamese release date", compose.onAllNodesWithText("Khởi chiếu $upcomingRelease").fetchSemanticsNodes().isNotEmpty())
        screenshot("catalog-live-upcoming")
        compose.runOnUiThread { vm.openMovie(upcoming) }
        compose.waitUntil(30_000) { !vm.detailLoading }
        Assert.assertEquals("", vm.detailError)
        compose.onNodeWithText("Chi tiết phim").assertIsDisplayed()
        compose.onNodeWithText(upcoming.name).assertExists()
        if (vm.scheduleSlots.isEmpty()) compose.onNodeWithText("Phim khởi chiếu $upcomingRelease — lịch chiếu sẽ được cập nhật.").performScrollTo().assertIsDisplayed()
        screenshot("catalog-live-upcoming-detail")
        compose.runOnUiThread { vm.openSchedule() }
        compose.waitUntil(30_000) { !vm.detailLoading && vm.screen == Screen.SHOWTIMES }
        if (vm.scheduleSlots.isEmpty()) compose.onNodeWithText("Phim khởi chiếu $upcomingRelease — lịch chiếu sẽ được cập nhật.").assertIsDisplayed()
        screenshot("catalog-live-upcoming-schedule")
        compose.runOnUiThread { vm.back(); vm.back(); vm.soon = false }
        compose.onNode(hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).performScrollToNode(hasText("Đang chiếu"))
        screenshot("catalog-live-home")
        val dates = api.dates()
        val future = dates.first { it > vietnamDate() }
        compose.onNodeWithTag("catalog-dates").performScrollToNode(hasTestTag("catalog-date-$future"))
        compose.onNodeWithTag("catalog-date-$future").performClick()
        compose.waitUntil(30_000) { vm.catalogDate == future && !vm.catalogLoading }
        Assert.assertEquals(api.movies(future).movies.map { it.id }.toSet(),vm.movies.map { it.id }.toSet())
        Assert.assertTrue(vm.movies.isNotEmpty())
        compose.runOnUiThread { vm.openMovie(vm.movies.first()) }
        compose.waitUntil(30_000) { !vm.detailLoading }
        Assert.assertEquals(api.showtimes(vm.movie!!.id,future).items.map { it.id }.toSet(),vm.showtimes.map { it.id }.toSet())
        screenshot("catalog-live-future-detail")
        compose.runOnUiThread { vm.back() }
        Assert.assertEquals(future,vm.catalogDate)
        val empty = nextCalendarDate(dates.last())
        compose.runOnUiThread { vm.selectCatalogDate(empty) }
        compose.waitUntil(30_000) { vm.catalogDate == empty && !vm.catalogLoading }
        Assert.assertEquals("",vm.catalogError)
        Assert.assertTrue(vm.movies.isEmpty())
        compose.onNode(hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).performScrollToNode(hasText("Chưa có lịch chiếu cho ngày ${displayCalendarDate(empty)}"))
        compose.onNodeWithText("Chưa có lịch chiếu cho ngày ${displayCalendarDate(empty)}").assertIsDisplayed()
        screenshot("catalog-live-empty")
    }
    private fun screenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.getExternalFilesDir(null),"visual_checks").apply { mkdirs() }
        File(folder,"$name.png").outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
}
