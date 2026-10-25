package com.nhom8.cineplex

import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.*
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.screens.trailerHtml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogPreviewTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    private fun slot(id: String, time: String, available: Int = 7, total: Int = 10) = ShowtimeDto(id,"film","fixture","2026-10-13T${time}:00.000Z","2026-10-13T16:30:00.000Z","IMAX_2D","150000","ACTIVE",AuditoriumDto("room","Cinema 1","IMAX",496,false),"Phụ đề Việt",available,total)
    @Test fun tappingHomeShowtimeValidatesThenReturnsHomeAndRejectsUnavailable() = runTest(dispatcher) {
        val film = MockMovies.all[0].copy(id = "film", soon = false)
        val chosen = slot("home-slot", "07:10")
        var fresh = chosen
        val catalog = object : CatalogRepository {
            override suspend fun dates() = listOf("2026-10-13")
            override suspend fun movies(date: String) = CatalogDay(listOf(film), listOf(chosen), "2026-10-13T07:09:59.000Z")
            override suspend fun movie(id: String) = film
            override suspend fun showtime(id: String) = ShowtimeResponse("2026-10-13T07:09:59.000Z", fresh)
            override suspend fun showtimes(id: String, date: String) = ShowtimesResponse(date, listOf(chosen), "2026-10-13T07:09:59.000Z")
        }
        val vm = CineplexViewModel(MockAccountAdapter(MockAccountRepository()), catalog, { "2026-10-13" }, { 0L })
        vm.update(AuthForm(email = "user@cineplex.test", password = "123456")); vm.submit(); advanceUntilIdle()
        vm.showSlot(chosen); advanceUntilIdle()
        assertEquals(Screen.SEAT_SUMMARY, vm.screen)
        assertEquals(chosen.id, vm.selectedShowtime?.id)
        assertEquals(Screen.HOME, vm.seatReturnScreen)
        vm.back()
        assertEquals(Screen.HOME, vm.screen)
        assertNull(vm.selectedShowtime)
        fresh = chosen.copy(availableSeats = 0)
        vm.showSlot(chosen); advanceUntilIdle()
        assertEquals(Screen.HOME, vm.screen)
        assertNull(vm.selectedShowtime)
        assertEquals("Suất chiếu không còn khả dụng", vm.notice?.title)
    }
    @Test fun exactStartHidesSlotAndFinalStartHidesMovieWithoutNetworkAndTimeFilterWorks() = runTest(dispatcher) {
        var elapsed = 0L
        var calls = 0
        val film = MockMovies.all[0].copy(id = "film",soon = false,trailerYoutubeId = "xGiimAKN0TM")
        val slots = listOf(slot("first","07:10"),slot("last","13:50"))
        val catalog = object : CatalogRepository {
            override suspend fun dates() = listOf("2026-10-13")
            override suspend fun movies(date: String): CatalogDay { calls++; return CatalogDay(listOf(film),slots,"2026-10-13T07:09:59.000Z") }
            override suspend fun movie(id: String) = film
            override suspend fun showtime(id: String) = ShowtimeResponse("2026-10-13T07:09:59.000Z", slots.first { it.id == id })
            override suspend fun showtimes(id: String,date: String) = ShowtimesResponse(date,slots,"2026-10-13T07:09:59.000Z")
        }
        val vm = CineplexViewModel(MockAccountAdapter(MockAccountRepository()),catalog,{ "2026-10-13" },{ elapsed })
        vm.update(AuthForm(email = "user@cineplex.test",password = "123456")); vm.submit(); advanceUntilIdle()
        assertEquals(2,vm.slotsFor("film").size)
        elapsed = 1000; vm.tickCatalogClock()
        assertEquals("last",vm.slotsFor("film").single().id)
        vm.selectTimeWindow(4); assertTrue(vm.filteredMovies.isEmpty())
        vm.selectTimeWindow(5); assertEquals("film",vm.filteredMovies.single().id)
        elapsed = isoMillis("2026-10-13T13:50:00.000Z") - isoMillis("2026-10-13T07:09:59.000Z"); vm.tickCatalogClock()
        assertTrue(vm.filteredMovies.isEmpty()); assertEquals(1,calls)
        vm.openTrailer(film); assertNotNull(vm.trailer); vm.back(); assertNull(vm.trailer)
        vm.openTrailer(film); vm.logout(); advanceUntilIdle(); assertNull(vm.trailer); assertTrue(vm.catalogShowtimes.isEmpty())
    }
    @Test fun halfOpenTimeWindowsDoNotOverlapAndCanceledSlotsAreNeverVisible() {
        val noon = slot("noon","05:00")
        assertFalse(matchesTimeWindow(noon,2)); assertTrue(matchesTimeWindow(noon,3))
        assertTrue(upcomingSlots(listOf(noon.copy(status = "CANCELED")),0).isEmpty())
        assertEquals("Chưa có dữ liệu ghế",seatSummary(noon.copy(availableSeats = 0,totalSeats = 0)))
        assertEquals("Còn 0/10",seatSummary(noon.copy(availableSeats = 0)))
    }
    @Test fun overnightRangeAndUtcParsingAreStrict() {
        val late = slot("late","16:00").copy(endAt = "2026-10-13T18:00:00.000Z")
        assertEquals("23:00–01:00",showtimeRange(late))
        assertEquals("12:00–23:30",showtimeRange(slot("same-day","05:00")))
        assertTrue(runCatching { isoMillis("2026-02-30T00:00:00.000Z") }.isFailure)
    }
    @Test fun trailerUsesFixedTrustedTemplateRefererUserGestureAndNoNativeBridge() {
        val html = trailerHtml("xGiimAKN0TM","com.nhom8.cineplex")
        assertTrue(html.contains("autoplay:0")); assertTrue(html.contains("strict-origin-when-cross-origin")); assertTrue(html.contains("origin:'https://com.nhom8.cineplex'"))
        assertTrue(html.contains("onError")); assertFalse(html.contains("innerHTML")); assertFalse(html.contains("Android."))
        assertTrue(html.contains("height:100vh")); assertTrue(html.contains("width:innerWidth,height:innerHeight"))
        assertTrue(runCatching { trailerHtml("<script>","com.nhom8.cineplex") }.isFailure)
        assertTrue(runCatching { trailerHtml("xGiimAKN0TM","bad'id") }.isFailure)
    }
}
