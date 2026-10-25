package com.nhom8.cineplex

import com.nhom8.cineplex.ui.SeatSelectionViewModel
import com.nhom8.cineplex.ui.SeatViewport
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SeatSelectionViewModelTest {
    private val dispatcher=StandardTestDispatcher()
    private var now=0L
    private val repo=FakeSeatRepository(clock={now})
    private fun vm()=SeatSelectionViewModel(repo) {now}.also {it.bind("user","showtime")}
    @Before fun setup() {Dispatchers.setMain(dispatcher)}
    @After fun teardown() {Dispatchers.resetMain()}
    @Test fun untouchedShowtimeLoadsInventoryWithoutStartingTime()=runTest(dispatcher) {
        val vm=vm();runCurrent();assertEquals(160,vm.seats.size);assertTrue(vm.selected.isEmpty());assertNull(vm.deadlineMillis)
        now+=100000;vm.refreshClock();assertEquals(0,vm.secondsLeft);assertFalse(vm.canContinue)
    }
    @Test fun coupleIsAtomicAndCountdownStartsOnlyAfterConfirming()=runTest(dispatcher) {
        val vm=vm();runCurrent();vm.select("L1");assertEquals(setOf("L1","L2"),vm.selected);assertEquals(200000L,vm.total)
        now+=20000;vm.select("B1");assertNull(vm.deadlineMillis);assertEquals(3,vm.places)
        vm.continueToSummary {};runCurrent();assertTrue(vm.held);assertEquals(600,vm.secondsLeft);assertEquals(1,repo.holds)
        val deadline=vm.deadlineMillis;now+=120000;vm.refreshClock();assertEquals(480,vm.secondsLeft)
        vm.bind("user","showtime");vm.refresh();runCurrent();assertEquals(deadline,vm.deadlineMillis);assertEquals(480,vm.secondsLeft)
        vm.select("B2");assertEquals(3,vm.places)
        now+=480001;vm.refreshClock();runCurrent();assertTrue(vm.expiredDialog);assertFalse(vm.held);assertFalse(vm.canContinue);assertEquals(0L,vm.total)
    }
    @Test fun conflictAndReconnectRemoveWholePairAndOfflineNeverFakesRelease()=runTest(dispatcher) {
        val vm=vm();runCurrent();vm.select("L1");repo.items=repo.items.map {if(it.id=="L2") it.copy(status="HELD") else it}
        vm.refresh();runCurrent();assertTrue(vm.selected.isEmpty());vm.select("L1");assertTrue(vm.selected.isEmpty())
        vm.select("B1");repo.offline=true;vm.refresh();runCurrent();assertFalse(vm.online);assertFalse(vm.canContinue)
        repo.offline=false;vm.refresh();runCurrent();vm.continueToSummary {};runCurrent();assertTrue(vm.held)
        repo.offline=true;vm.cancelHold();runCurrent();assertTrue(vm.held);assertTrue(vm.message.contains("Chưa xác nhận"))
        repo.offline=false;vm.cancelHold();runCurrent();assertFalse(vm.held);assertEquals(0,vm.places)
    }
    @Test fun lostResponseRecoversSameOrderAndProcessRestartPreservesDeadline()=runTest(dispatcher) {
        val first=vm();runCurrent();first.select("A1");repo.loseResponse=true;first.continueToSummary {};runCurrent();assertEquals(1,repo.holds)
        now+=30000;val restored=vm();runCurrent();assertTrue(restored.held);assertEquals(570,restored.secondsLeft);assertEquals(1,repo.holds)
        restored.cancelHold();runCurrent();assertFalse(restored.held)
    }
    @Test fun goldIsNotCoupleAndNewUserNeverInheritsAnotherSelection()=runTest(dispatcher) {
        val gold=SeatSelectionViewModel(FakeSeatRepository("GOLD")) {now};gold.bind("one","gold");runCurrent();gold.select("A1");assertEquals(1,gold.places)
        gold.bind("two","gold");runCurrent();assertTrue(gold.selected.isEmpty());assertNull(gold.deadlineMillis)
    }
    @Test fun homeCanRecoverOwnedHoldWithoutDependingOnCatalogAvailability()=runTest(dispatcher) {
        val first=vm();runCurrent();first.select("A1");first.continueToSummary {};runCurrent()
        val fresh=SeatSelectionViewModel(repo) {now};fresh.findResume("user");runCurrent()
        assertNotNull(fresh.resumeOrder);assertEquals("showtime",fresh.resumeOrder?.showtimeId)
        fresh.clearSession();assertNull(fresh.resumeOrder)
        fresh.findResume("different-user");runCurrent();assertNull(fresh.resumeOrder)
    }
    @Test fun zoomKeepsFocalPointClampsBoundsAndFitsLargeImax() {
        val fit=SeatViewport.fit(360f,400f,41f,22f)
        val zoom=fit.zoom(180f,200f,4f,0f,0f,fit.scale,72f,360f,400f,41f,22f)
        assertEquals((180-fit.x)/fit.scale,(180-zoom.x)/zoom.scale,.001f)
        val panned=zoom.zoom(180f,200f,1f,99999f,-99999f,fit.scale,72f,360f,400f,41f,22f)
        assertEquals(0f,panned.x,.001f);assertEquals(400-22*panned.scale,panned.y,.001f)
        val back=zoom.zoom(180f,200f,.0001f,0f,0f,fit.scale,72f,360f,400f,41f,22f)
        assertEquals(fit.scale,back.scale,.001f);assertEquals(fit.x,back.x,.001f)
    }
    @Test fun resumeNavigationDoesNotRejectAnOwnedHoldWhenCatalogIsSoldOut()=runTest(dispatcher) {
        val today="2026-10-25";val film=MockMovies.all.first()
        val slot=ShowtimeDto("owned-hold",film.id,"fixture","2026-10-25T12:00:00.000Z","2026-10-25T14:00:00.000Z","STANDARD_2D","100000","ACTIVE",AuditoriumDto("room","Cinema 3","STANDARD",160,false),null,0,160)
        val api=object: CatalogRepository by MockCatalogRepository {
            override suspend fun showtime(id: String)=ShowtimeResponse("2026-10-25T00:00:00.000Z",slot)
            override suspend fun movie(id: String)=film
        }
        val vm=CineplexViewModel(MockAccountAdapter(MockAccountRepository()),api,{today},{0L})
        vm.update(AuthForm(email="user@cineplex.test",password="123456"));vm.submit();advanceTimeBy(1000);runCurrent()
        assertEquals(Screen.HOME,vm.screen);vm.resumeHold(slot.id);runCurrent()
        assertEquals(Screen.SEAT_HANDOFF,vm.screen);assertEquals(0,vm.selectedShowtime?.availableSeats)
    }
    @Test fun socketRefetchFinishingCancelBeforeHttpResponseDoesNotLoseSuccess()=runTest(dispatcher) {
        lateinit var vm: SeatSelectionViewModel
        val events=object: SeatRepository by repo {
            override suspend fun cancel(id: String): HoldResponse {
                val result=repo.cancel(id)
                vm.refresh();yield()
                return result
            }
        }
        vm=SeatSelectionViewModel(events) {now};vm.bind("user","showtime");runCurrent()
        vm.select("A1");vm.continueToSummary {};runCurrent();assertTrue(vm.held)
        var completed=false;vm.cancelHold {completed=true};runCurrent()
        assertTrue(completed);assertFalse(vm.held);assertTrue(vm.online);assertEquals("Đã hủy giữ chỗ.",vm.message)
    }
}
