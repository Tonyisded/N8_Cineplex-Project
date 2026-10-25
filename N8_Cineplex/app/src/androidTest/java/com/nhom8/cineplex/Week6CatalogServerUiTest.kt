package com.nhom8.cineplex

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.*
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.theme.CineplexMockTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File

class Week6CatalogServerUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun liveVpsSelectionUsesActualShowtimeRoomAndAgeBeforeSummary() = runBlocking {
        val api = ApiCatalogRepository()
        val dates = api.dates()
        assertTrue("VPS must publish upcoming dates", dates.isNotEmpty())
        val date = dates.firstOrNull { it > vietnamDate() } ?: dates.first()
        val day = api.movies(date)
        val expected = day.showtimes.first { it.availableSeats > 0 && it.totalSeats > 0 }
        val film = day.movies.first { it.id == expected.movieId }
        val vm = CineplexViewModel(MockAccountAdapter(MockAccountRepository()), api)
        compose.setContent { CineplexMockTheme { CineplexApp(vm) } }
        compose.runOnUiThread { vm.update(AuthForm(email = "user@cineplex.test", password = "123456")); vm.submit() }
        compose.waitUntil(30_000) { vm.movies.isNotEmpty() && !vm.catalogLoading }
        compose.runOnUiThread { vm.selectCatalogDate(date); vm.openMovie(film); vm.openSchedule() }
        compose.waitUntil(30_000) { !vm.detailLoading && vm.scheduleSlots.any { it.id == expected.id } }
        compose.onNodeWithTag("schedule-slot-${expected.id}").performScrollTo().performClick()
        compose.onNodeWithTag("continue-showtime").performClick()
        DetailSchedule.ageLimit(film)?.let { age -> compose.onNodeWithText("Tôi từ đủ $age tuổi — Tiếp tục").performScrollTo().performClick() }
        compose.waitUntil(30_000) { vm.screen == Screen.SEAT_SUMMARY || vm.notice != null }
        assertEquals("The live GET /showtimes/:id must allow the selected future slot", Screen.SEAT_SUMMARY, vm.screen)
        val detail = api.showtime(expected.id).item
        assertEquals(detail.id, vm.selectedShowtime?.id)
        assertEquals(detail.movieId, film.id)
        assertEquals(detail.format, vm.selectedShowtime?.format)
        assertEquals(detail.auditorium.id, vm.selectedShowtime?.auditorium?.id)
        compose.onNodeWithText("N8 Cineplex · ${detail.auditorium.name}").assertExists()
        compose.onNodeWithText("Landmark 81",substring = true).assertDoesNotExist()
        val file = File(compose.activity.getExternalFilesDir(null), "week6-live-vps-summary.png")
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        println("WEEK6_LIVE_VPS_OK movie=${film.id} showtime=${detail.id} format=${detail.format} room=${detail.auditorium.name}")
    }
}
