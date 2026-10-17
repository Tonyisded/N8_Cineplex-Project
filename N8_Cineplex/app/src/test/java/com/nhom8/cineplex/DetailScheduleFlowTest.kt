package com.nhom8.cineplex

import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.*
import com.nhom8.cineplex.ui.CineplexViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class DetailScheduleFlowTest {
    private val dispatcher = StandardTestDispatcher()
    private var today = "2026-10-17"
    private var clock = 0L
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    private inner class Catalog(age: String = "T16") : CatalogRepository {
        val film = MockMovies.all.first().copy(age = age)
        var serverTime = "2026-10-17T02:00:00.000Z"
        var fresh: ShowtimeDto? = null
        var calls = 0
        var failure = false
        var gate: CompletableDeferred<Unit>? = null
        val emptyDays = mutableSetOf<String>()
        override suspend fun dates() = listOf(today, nextCalendarDate(today))
        override suspend fun movie(id: String) = film
        override suspend fun movies(date: String) = CatalogDay(listOf(film), showtimes(film.id, date).items, serverTime)
        override suspend fun showtimes(id: String, date: String): ShowtimesResponse {
            val slot = ShowtimeDto("available-$date", id, "fixture", "${date}T03:00:00.000Z", "${date}T05:00:00.000Z", "IMAX_2D", "150000", "ACTIVE", AuditoriumDto("room", "Cinema 1", "IMAX", 496, false), "Phu de Viet", 3, 4)
            return ShowtimesResponse(date, if (date in emptyDays) emptyList() else listOf(slot, slot.copy(id = "sold-$date", availableSeats = 0), slot.copy(id = "missing-$date", availableSeats = 0, totalSeats = 0)), serverTime)
        }
        override suspend fun showtime(id: String): ShowtimeResponse {
            calls++; gate?.let { withContext(NonCancellable) { it.await() } }
            if (failure) error("Offline or removed showtime")
            return ShowtimeResponse(serverTime, fresh ?: showtimes(film.id, id.takeLast(10)).items.first { it.id == id })
        }
    }
    private suspend fun ready(api: Catalog): CineplexViewModel {
        val vm = CineplexViewModel(MockAccountAdapter(MockAccountRepository()), api, { today }, { clock })
        vm.update(AuthForm(email = "user@cineplex.test", password = "123456")); vm.submit(); dispatcher.scheduler.advanceUntilIdle()
        vm.openMovie(api.film); dispatcher.scheduler.advanceUntilIdle(); vm.openSchedule(); dispatcher.scheduler.advanceUntilIdle()
        return vm
    }
    @Test fun actualSlotsAgeBranchesSummaryAndSharedDateStayConsistent() = runTest(dispatcher) {
        val api = Catalog(); val vm = ready(api)
        assertEquals(Screen.SHOWTIMES, vm.screen); assertEquals(16, DetailSchedule.ageLimit(api.film)); assertEquals(3, vm.scheduleSlots.size)
        vm.chooseShowtime("sold-$today"); assertNull(vm.selectedShowtime)
        vm.chooseShowtime("missing-$today"); assertNull(vm.selectedShowtime)
        vm.chooseShowtime("available-$today"); vm.continueSchedule(); assertTrue(vm.ageSheet)
        vm.showUnderAge(); vm.confirmAge(); advanceUntilIdle(); assertEquals(0, api.calls); assertEquals(Screen.SHOWTIMES, vm.screen)
        vm.closeAgeSheet(); vm.continueSchedule(); vm.confirmAge(); advanceUntilIdle()
        assertEquals(Screen.SEAT_SUMMARY, vm.screen); assertEquals(1, api.calls); assertEquals("Cinema 1", vm.selectedShowtime?.auditorium?.name)
        vm.back(); assertEquals("available-$today", vm.selectedShowtime?.id)
        val next = nextCalendarDate(today); api.emptyDays += next; vm.selectCatalogDate(next); advanceUntilIdle()
        assertEquals(next, vm.catalogDate); assertNull(vm.selectedShowtime); assertTrue(vm.scheduleSlots.isEmpty())
        vm.back(); vm.back(); assertEquals(Screen.HOME, vm.screen); assertEquals(next, vm.catalogDate)
    }
    @Test fun allAgeLabelsUseMovieDataAndOnlyThresholdLabelsOpenTheSheet() = runTest(dispatcher) {
        for (age in listOf("P", "K", "T13", "T16", "T18")) {
            val api = Catalog(age); val vm = ready(api); vm.chooseShowtime("available-$today"); vm.continueSchedule(); advanceUntilIdle()
            if (age.startsWith("T")) { assertTrue(vm.ageSheet); assertEquals(age.drop(1).toInt(), DetailSchedule.ageLimit(api.film)); vm.confirmAge(); advanceUntilIdle() } else assertFalse(vm.ageSheet)
            assertEquals(Screen.SEAT_SUMMARY, vm.screen)
        }
    }
    @Test fun freshCanceledSoldMissingBegunOrRemovedShowtimesNeverReachSummary() = runTest(dispatcher) {
        for (scenario in listOf("canceled", "sold", "missing", "begun", "removed")) {
            val api = Catalog(); val vm = ready(api); val slot = vm.scheduleSlots.first()
            api.fresh = when (scenario) { "canceled" -> slot.copy(status = "CANCELED"); "sold" -> slot.copy(availableSeats = 0); "missing" -> slot.copy(availableSeats = 0, totalSeats = 0); else -> slot }
            if (scenario == "begun") api.serverTime = slot.startAt
            api.failure = scenario == "removed"
            vm.chooseShowtime(slot.id); vm.continueSchedule(); vm.confirmAge(); advanceUntilIdle()
            assertEquals(scenario, Screen.SHOWTIMES, vm.screen); assertNull(vm.selectedShowtime); assertNotNull(vm.notice); assertFalse(vm.scheduleChecking)
        }
    }
    @Test fun lateValidationCannotAdvanceAfterDateChangeLogoutOrClosingSheetAndDoubleTapReadsOnce() = runTest(dispatcher) {
        for (action in listOf("date", "logout", "close")) {
            val api = Catalog().apply { gate = CompletableDeferred() }; val vm = ready(api)
            vm.chooseShowtime("available-$today"); vm.continueSchedule(); vm.confirmAge(); runCurrent()
            vm.confirmAge(); runCurrent(); assertEquals(1, api.calls); assertTrue(vm.scheduleChecking)
            when (action) { "date" -> vm.selectCatalogDate(nextCalendarDate(today)); "logout" -> vm.logout(); else -> vm.closeAgeSheet() }
            api.gate!!.complete(Unit); advanceUntilIdle(); assertNotEquals(Screen.SEAT_SUMMARY, vm.screen); assertFalse(vm.scheduleChecking)
            if (action == "logout") { assertEquals(Screen.LOGIN, vm.screen); assertNull(vm.session) }
        }
    }
    @Test fun exactStartInvalidatesSelectionAndHomeTimeFilterDoesNotHideSchedule() = runTest(dispatcher) {
        val api = Catalog(); val vm = ready(api)
        vm.selectTimeWindow(6); assertTrue(vm.visibleShowtimes.isEmpty()); assertEquals(3, vm.scheduleSlots.size)
        vm.chooseShowtime("available-$today"); vm.continueSchedule(); assertTrue(vm.ageSheet)
        clock = 3_600_000L; vm.tickCatalogClock(); assertNull(vm.selectedShowtime); assertFalse(vm.ageSheet); assertEquals(0, api.calls); assertTrue(vm.scheduleSlots.isEmpty())
    }
    @Test fun dayRolloverFromSummaryClearsSelectionAndReturnsToSchedule() = runTest(dispatcher) {
        val api = Catalog("P"); val vm = ready(api)
        vm.chooseShowtime("available-$today"); vm.continueSchedule(); advanceUntilIdle()
        assertEquals(Screen.SEAT_SUMMARY, vm.screen)
        today = nextCalendarDate(today); api.serverTime = "${today}T02:00:00.000Z"
        vm.refreshCatalog(force = false); advanceUntilIdle()
        assertEquals(today, vm.catalogDate); assertNull(vm.selectedShowtime); assertEquals(Screen.SHOWTIMES, vm.screen)
    }
    @Test fun filmMetadataDoesNotInventRatingsReleaseDateOrTrailer() {
        val meta = DetailSchedule.filmMeta(MockMovies.all.first().copy(id = "dune", releaseDate = null, trailerYoutubeId = null))
        assertNull(meta.releaseDate); assertNull(meta.rating); assertNull(meta.reviewCount); assertNull(meta.trailerYoutubeId)
    }
}
