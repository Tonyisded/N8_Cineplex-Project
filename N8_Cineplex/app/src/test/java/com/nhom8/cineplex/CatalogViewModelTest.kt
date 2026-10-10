package com.nhom8.cineplex

import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.*
import com.nhom8.cineplex.ui.CineplexViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    private class Catalog : CatalogRepository {
        val films = listOf(MockMovies.all[0].copy(name = "Đường về", original = "", poster = 0), MockMovies.all[1])
        override suspend fun dates() = listOf(vietnamDate(),nextCalendarDate(vietnamDate()))
        val requestedDates = mutableListOf<String>()
        val detailDates = mutableListOf<String>()
        val waits = mutableMapOf<String,CompletableDeferred<Unit>>()
        val datedFilms = mutableMapOf<String,List<Movie>>()
        var calls = 0
        var failed = false
        var pending: CompletableDeferred<Unit>? = null
        override suspend fun movies(date: String): List<Movie> {
            calls++; requestedDates += date; waits[date]?.let { withContext(NonCancellable) { it.await() } }; pending?.let { withContext(NonCancellable) { it.await() } }
            if(failed) error("Offline")
            return datedFilms[date] ?: films
        }
        override suspend fun movie(id: String) = films.first { it.id == id }
        override suspend fun showtimes(id: String, date: String): List<ShowtimeDto> { detailDates += date; return emptyList() }
    }
    private fun login(catalog: Catalog): CineplexViewModel {
        val vm = CineplexViewModel(MockAccountAdapter(MockAccountRepository()), catalog)
        vm.update(AuthForm(email = "user@cineplex.test", password = "123456")); vm.submit()
        return vm
    }
    @Test fun realCatalogPathLoadingSearchRefreshAndNullMetadata() = runTest(dispatcher) {
        val catalog = Catalog(); val vm = login(catalog); advanceUntilIdle()
        assertEquals(2, vm.movies.size); assertFalse(vm.catalogLoading); assertEquals(1, catalog.calls)
        vm.query = "DUONG VE"; assertEquals("Đường về", vm.filteredMovies.single().name)
        vm.soon = true; assertTrue(vm.filteredMovies.isEmpty()); assertFalse(vm.fixtureCatalog)
        vm.refreshCatalog(force = false); advanceUntilIdle(); assertEquals(1, catalog.calls)
        vm.refreshCatalog(); advanceUntilIdle(); assertEquals(2, catalog.calls)
        vm.openMovie(catalog.films.first()); advanceUntilIdle(); assertFalse(vm.detailLoading); assertTrue(vm.showtimes.isEmpty())
        val dto = MovieDto("uuid", "Phim", 90, "K", sourceUrl = "https://www.cgv.vn/default/film.html").movie()
        assertEquals("", dto.original); assertEquals(0, dto.year); assertNull(dto.releaseDate); assertEquals(0, dto.poster)
    }
    @Test fun failureNeverFallsBackToMoviesAndRetryRecovers() = runTest(dispatcher) {
        val catalog = Catalog().apply { failed = true }; val vm = login(catalog); advanceUntilIdle()
        assertTrue(vm.movies.isEmpty()); assertTrue(vm.catalogError.isNotBlank()); assertNotNull(vm.session)
        catalog.failed = false; vm.refreshCatalog(); advanceUntilIdle(); assertEquals(2,vm.movies.size); assertEquals("",vm.catalogError)
    }
    @Test fun lateCatalogResponseAfterLogoutCannotRestoreMoviesOrSession() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>(); val catalog = Catalog().apply { pending = gate }; val vm = login(catalog)
        advanceTimeBy(1000); runCurrent(); assertTrue(vm.catalogLoading); vm.logout(); gate.complete(Unit); advanceUntilIdle()
        assertNull(vm.session); assertEquals(Screen.LOGIN,vm.screen); assertTrue(vm.movies.isEmpty()); assertFalse(vm.catalogLoading); assertNull(vm.movie)
    }
    @Test fun utcToVietnamAndBusinessDateDoNotDependOnDeviceTimezone() {
        assertEquals("2026-10-10",vietnamDate(Date(1791565200000L)))
        assertEquals("00:00",vietnamTime("2026-10-09T17:00:00.000Z"))
        assertEquals("23:30",vietnamTime("2026-10-10T16:30:00.000Z"))
    }

    @Test fun selectedDateIsUsedByHomeDetailBackAndResumeAndRejectsPast() = runTest(dispatcher) {
        val catalog = Catalog(); val first = vietnamDate(); val next = nextCalendarDate(first)
        val vm = CineplexViewModel(MockAccountAdapter(MockAccountRepository()),catalog)
        vm.update(AuthForm(email = "user@cineplex.test",password = "123456")); vm.submit(); advanceUntilIdle()
        vm.soon = true; vm.selectCatalogDate(next); advanceUntilIdle()
        assertFalse(vm.soon); assertEquals(next,vm.catalogDate); assertEquals(next,catalog.requestedDates.last())
        vm.openMovie(vm.movies.first()); advanceUntilIdle(); assertEquals(next,catalog.detailDates.last())
        vm.back(); vm.refreshCatalog(); advanceUntilIdle(); assertEquals(next,vm.catalogDate)
        val calls = catalog.calls; vm.refreshCatalog(force = false); advanceUntilIdle(); assertEquals(calls,catalog.calls)
        vm.selectCatalogDate("2000-01-01"); advanceUntilIdle(); assertEquals(next,vm.catalogDate)
        assertTrue(vm.catalogDateChoices.all { it >= first })
    }
    @Test fun oldDateResponseCannotReplaceNewSelectionAndEmptyDatesDoNotFallback() = runTest(dispatcher) {
        val catalog = Catalog(); val first = vietnamDate(); val next = nextCalendarDate(first); val later = nextCalendarDate(next)
        val gate = CompletableDeferred<Unit>(); catalog.waits[next] = gate; catalog.datedFilms[later] = emptyList()
        val vm = login(catalog); advanceUntilIdle()
        vm.selectCatalogDate(next); runCurrent(); assertTrue(vm.catalogLoading)
        vm.selectCatalogDate(later); runCurrent(); assertEquals(later,vm.catalogDate); assertFalse(vm.catalogLoading); assertTrue(vm.movies.isEmpty())
        gate.complete(Unit); advanceUntilIdle(); assertEquals(later,vm.catalogDate); assertTrue(vm.movies.isEmpty()); assertEquals("",vm.catalogError)
    }
    @Test fun vietnamDayRolloverClampsOldDayButPreservesFutureSelection() = runTest(dispatcher) {
        var today = "2026-10-31"; val catalog = Catalog()
        val vm = CineplexViewModel(MockAccountAdapter(MockAccountRepository()),catalog,{ today })
        vm.update(AuthForm(email = "user@cineplex.test",password = "123456")); vm.submit(); advanceUntilIdle()
        assertEquals(today,vm.catalogDate)
        today = "2026-11-01"; vm.refreshCatalog(force = false); advanceUntilIdle(); assertEquals(today,vm.catalogDate)
        vm.selectCatalogDate("2027-01-01"); advanceUntilIdle()
        today = "2026-12-31"; vm.refreshCatalog(force = false); advanceUntilIdle(); assertEquals("2027-01-01",vm.catalogDate)
    }
    @Test fun datePickerCalendarValuesAreUtcDatesAndValidateLeapDays() {
        assertEquals("2026-11-01",nextCalendarDate("2026-10-31"))
        assertEquals("2027-01-01",nextCalendarDate("2026-12-31"))
        assertEquals("2024-02-29",nextCalendarDate("2024-02-28"))
        assertEquals("2026-10-10",calendarDate(calendarDateMillis("2026-10-10")))
        assertEquals("10/10/2026",displayCalendarDate("2026-10-10"))
        for(value in listOf("2026-02-29","2026-13-01","2026-10-1","")) assertTrue(runCatching { calendarDateMillis(value) }.isFailure)
    }
}
