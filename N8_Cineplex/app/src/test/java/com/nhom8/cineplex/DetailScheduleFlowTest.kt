package com.nhom8.cineplex

import com.nhom8.cineplex.data.DetailSchedule
import com.nhom8.cineplex.data.MockAccountRepository
import com.nhom8.cineplex.data.MockMovies
import com.nhom8.cineplex.data.nextCalendarDate
import com.nhom8.cineplex.model.AuthForm
import com.nhom8.cineplex.model.Screen
import com.nhom8.cineplex.ui.CineplexViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailScheduleFlowTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun teardown() = Dispatchers.resetMain()

    @Test fun selectedFilmFiltersAndAgeBranchesStayConsistent() = runTest(dispatcher) {
        val vm = CineplexViewModel(MockAccountRepository())
        vm.update(AuthForm(email = "user@cineplex.test", password = "123456")); vm.submit(); advanceUntilIdle()
        val movie = MockMovies.all.first()
        vm.openMovie(movie); advanceUntilIdle(); vm.openSchedule()
        assertEquals(Screen.SHOWTIMES,vm.screen)
        assertEquals(movie.id,vm.movie?.id)
        assertEquals(16,DetailSchedule.ageLimit(movie))
        val future = nextCalendarDate(vm.todayDate)
        vm.chooseScheduleDate(future)
        val available = vm.sampleSlots.first { !it.soldOut }
        vm.chooseSampleSlot(available.id)
        assertEquals(available.id,vm.selectedSampleSlot?.id)
        vm.continueSchedule(); assertTrue(vm.ageSheet)
        vm.showUnderAge(); vm.confirmAge(); assertEquals(Screen.SHOWTIMES,vm.screen)
        vm.closeAgeSheet(); vm.continueSchedule(); vm.confirmAge(); assertEquals(Screen.SEAT_SUMMARY,vm.screen)
        vm.back(); assertEquals(Screen.SHOWTIMES,vm.screen)
        assertEquals(available.id,vm.selectedSampleSlot?.id)
        vm.chooseScheduleArea("hn"); assertNull(vm.selectedSampleSlot)
        vm.continueSchedule(); assertFalse(vm.ageSheet)
        vm.chooseScheduleDate(DetailSchedule.dates(vm.todayDate)[3]); assertTrue(vm.sampleSlots.isEmpty())
        vm.back(); assertEquals(Screen.DETAIL,vm.screen)
        vm.back(); assertEquals(Screen.HOME,vm.screen)
    }
}
