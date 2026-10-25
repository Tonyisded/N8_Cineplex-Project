package com.nhom8.cineplex

import com.nhom8.cineplex.data.SeatAvailability
import com.nhom8.cineplex.data.SeatDemo
import com.nhom8.cineplex.ui.SeatSelectionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SeatSelectionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private var now = 1_000_000L
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun untouchedDemoShowtimeStartsWithEveryPlaceAvailable() {
        val seats = SeatDemo.initial()
        assertEquals(54, seats.size)
        assertEquals(60, seats.sumOf { it.places })
        assertTrue(seats.all { it.availability == SeatAvailability.AVAILABLE })
    }

    @Test fun coupleIsAtomicAndDeadlineDoesNotResetAfterChangingSeats() = runTest(dispatcher) {
        val state = SeatSelectionViewModel { now }
        state.bind("showtime-1")
        val pair = SeatDemo.initial().first { it.id == "J1-J2" }
        assertEquals(listOf("J1", "J2"), pair.codes)
        assertEquals(240_000L, pair.price)
        state.select(pair.id)
        assertEquals("J1-J2", state.pending)
        assertEquals(2, state.places)
        assertEquals(240_000L, state.total)
        assertNull(state.deadlineMillis)
        advanceTimeBy(650); runCurrent()
        assertEquals(setOf(pair.id), state.selected)
        assertEquals(now + 600_000, state.deadlineMillis)
        val deadline = state.deadlineMillis
        state.select(pair.id)
        assertTrue(state.selected.isEmpty())
        state.select("B1")
        advanceTimeBy(650); runCurrent()
        assertEquals(deadline, state.deadlineMillis)
        assertEquals(1, state.places)
        assertEquals(90_000L, state.total)
        now += 600_001
        state.refreshClock()
        assertTrue(state.expiredDialog)
        assertTrue(state.selected.isEmpty())
        assertEquals(0L, state.total)
        assertFalse(state.canContinue)
    }

    @Test fun conflictAndReconnectRemoveWholePairAndLockOffline() = runTest(dispatcher) {
        val state = SeatSelectionViewModel { now }
        state.bind("showtime-2")
        state.debugFailNext("J1-J2")
        state.select("J1-J2")
        advanceTimeBy(650); runCurrent()
        assertTrue(state.selected.isEmpty())
        assertNull(state.deadlineMillis)
        assertEquals(SeatAvailability.HELD, state.seats.first { it.id == "J1-J2" }.availability)
        state.select("J3-J4")
        advanceTimeBy(650); runCurrent()
        assertEquals(2, state.places)
        state.debugOffline()
        assertFalse(state.canContinue)
        state.debugChangeCode("J3", SeatAvailability.SOLD)
        assertEquals(2, state.places)
        state.debugReconnect()
        advanceTimeBy(900); runCurrent()
        assertTrue(state.selected.isEmpty())
        assertEquals(0L, state.total)
        assertEquals(SeatAvailability.SOLD, state.seats.first { it.id == "J3-J4" }.availability)
    }
}
