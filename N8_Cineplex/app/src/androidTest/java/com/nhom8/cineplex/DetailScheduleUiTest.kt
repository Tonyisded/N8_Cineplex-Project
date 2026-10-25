package com.nhom8.cineplex

import androidx.activity.ComponentActivity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.*
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.SeatSelectionViewModel
import com.nhom8.cineplex.ui.theme.CineplexMockTheme
import org.junit.*
import org.junit.Assert.*
import java.io.File

class DetailScheduleUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val today = "2026-10-17"
    private val film = MockMovies.all.first().copy(id = "week6-film", age = "T16", trailerYoutubeId = null)
    private val slot = ShowtimeDto("week6-slot", film.id, "fixture", "2026-10-17T15:00:00.000Z", "2026-10-17T17:00:00.000Z", "IMAX_2D", "150000", "ACTIVE", AuditoriumDto("room", "Cinema 1", "IMAX", 496, false), "Phụ đề Việt", 2, 4)
    private var canceled = false
    private var offline = false
    private lateinit var vm: CineplexViewModel
    private lateinit var seatVm: SeatSelectionViewModel
    private val api = object : CatalogRepository {
        override suspend fun dates() = listOf(today)
        override suspend fun movie(id: String) = film
        override suspend fun movies(date: String) = CatalogDay(listOf(film), listOf(slot), "2026-10-17T00:00:00.000Z")
        override suspend fun showtimes(id: String, date: String): ShowtimesResponse {
            if (offline) error("Offline")
            return ShowtimesResponse(date, if (date != today) emptyList() else listOf(slot, slot.copy(id = "sold", availableSeats = 0), slot.copy(id = "missing", totalSeats = 0, availableSeats = 0)), "2026-10-17T00:00:00.000Z")
        }
        override suspend fun showtime(id: String) = ShowtimeResponse("2026-10-17T00:00:00.000Z", if (canceled) slot.copy(status = "CANCELED") else slot)
    }
    @Before fun setup() {
        vm = CineplexViewModel(MockAccountAdapter(MockAccountRepository()), api, { today }, { 0L })
        seatVm = SeatSelectionViewModel()
        compose.setContent { CineplexMockTheme { CineplexApp(vm, seatVm) } }
        compose.runOnUiThread { vm.update(AuthForm(email = "user@cineplex.test", password = "123456")); vm.submit() }
        compose.waitUntil(10_000) { vm.movies.isNotEmpty() }
        compose.runOnUiThread { vm.openMovie(film) }
        compose.waitUntil(10_000) { !vm.detailLoading }
        compose.runOnUiThread { vm.openSchedule() }
        compose.waitUntil(10_000) { !vm.detailLoading }
    }
    private fun selectAndContinue() {
        compose.onNodeWithTag("schedule-slot-week6-slot").performScrollTo().performClick()
        compose.onNodeWithTag("continue-showtime").performClick()
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val file = File(compose.activity.getExternalFilesDir(null), "$name.png")
        file.outputStream().use { checkNotNull(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun landmarkRealSlotsAgeBranchesAndSummaryKeepTheSelectedRoom() {
        compose.onNodeWithText("Landmark 81").assertExists()
        compose.onNodeWithText("Cineplex Đồng Khởi").assertDoesNotExist()
        compose.onNodeWithTag("continue-showtime").assertIsNotEnabled()
        compose.onNodeWithTag("schedule-slot-sold").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("schedule-slot-missing").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("schedule-slot-week6-slot").assertWidthIsAtLeast((96 * compose.activity.resources.configuration.fontScale).dp)
        screenshot("week6-schedules")
        selectAndContinue()
        compose.onNodeWithText("Tôi chưa đủ 16 tuổi").performScrollTo().performClick()
        compose.onNodeWithText("Bạn chưa đủ tuổi xem phim này").assertExists()
        compose.onNodeWithText("Quay lại").performClick()
        compose.onNodeWithTag("continue-showtime").performClick()
        compose.onNodeWithText("Tôi từ đủ 16 tuổi — Tiếp tục").performScrollTo().performClick()
        compose.waitUntil(10_000) { vm.screen == Screen.SEAT_SUMMARY }
        compose.onNodeWithText("Landmark 81 · Cinema 1").assertExists()
        assertEquals(slot.id, vm.selectedShowtime?.id)
        screenshot("week6-summary")
        compose.onNodeWithTag("seat-A2").performClick()
        compose.waitUntil(5_000) { "A2" in seatVm.selected }
        compose.runOnUiThread { seatVm.select("J1-J2") }
        compose.waitUntil(5_000) { "J1-J2" in seatVm.selected }
        assertEquals(3, seatVm.places)
        assertEquals(330_000L, seatVm.total)
        compose.onNodeWithTag("seat-map").performTouchInput { swipeUp() }
        compose.onNodeWithTag("seat-map").performTouchInput { swipeUp() }
        screenshot("seat-couple")
        compose.onNodeWithTag("seat-continue").assertIsEnabled().performClick()
        compose.waitUntil(5_000) { vm.screen == Screen.SEAT_HANDOFF }
        compose.onNodeWithText("Tóm tắt lựa chọn").assertIsDisplayed()
        assertNotNull(seatVm.deadlineMillis)
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(5_000) { vm.screen == Screen.SEAT_SUMMARY }
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Rời màn hình chọn ghế?").assertIsDisplayed()
        compose.onNodeWithText("Ở lại").performClick()
        assertEquals(Screen.SEAT_SUMMARY, vm.screen)
    }
    @Test fun networkFailureRetryAndEmptyDayNeverGenerateSchedules() {
        compose.runOnUiThread { offline = true; vm.refreshMovie() }
        compose.waitUntil(10_000) { vm.detailError.isNotEmpty() }
        compose.onNodeWithTag("schedule-retry").performScrollTo()
        compose.runOnUiThread { offline = false }
        compose.onNodeWithTag("schedule-retry").performClick()
        compose.waitUntil(10_000) { !vm.detailLoading && vm.detailError.isEmpty() }
        compose.onNodeWithTag("schedule-slot-week6-slot").assertExists()
        compose.runOnUiThread { vm.selectCatalogDate(nextCalendarDate(today)) }
        compose.waitUntil(10_000) { !vm.detailLoading && vm.scheduleSlots.isEmpty() }
        compose.onNodeWithText("Chưa có suất chiếu trong ngày này").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("continue-showtime").assertIsNotEnabled()
        screenshot("week6-empty")
    }
    @Test fun canceledSelectionIsRejectedAfterAgeConfirmation() {
        selectAndContinue()
        compose.runOnUiThread { canceled = true }
        compose.onNodeWithText("Tôi từ đủ 16 tuổi — Tiếp tục").performScrollTo().performClick()
        compose.waitUntil(10_000) { vm.notice != null }
        assertEquals(Screen.SHOWTIMES, vm.screen)
        assertNull(vm.selectedShowtime)
        compose.onNodeWithText("Suất chiếu không còn khả dụng").assertExists()
        screenshot("week6-canceled")
    }
}
