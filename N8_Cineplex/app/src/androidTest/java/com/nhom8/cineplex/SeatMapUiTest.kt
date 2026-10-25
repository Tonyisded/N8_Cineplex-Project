package com.nhom8.cineplex

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.*
import com.nhom8.cineplex.ui.*
import com.nhom8.cineplex.ui.theme.CineplexMockTheme
import org.junit.*
import org.junit.Assert.*
import java.io.File

class SeatMapUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private lateinit var seats: SeatSelectionViewModel
    private lateinit var vm: CineplexViewModel
    private fun open(type: String) {
        val film=MockMovies.all.first().copy(id="seat-film",age="P",trailerYoutubeId=null)
        val format=when(type) {"IMAX"->"IMAX_2D";"SCREENX"->"SCREENX";"GOLD"->"GOLD_CLASS";else->"STANDARD_2D"}
        val capacity=when(type) {"IMAX"->496;"SCREENX"->180;"GOLD"->32;else->160}
        val slot=ShowtimeDto("seat-slot-$type",film.id,"fixture","2026-10-25T12:00:00.000Z","2026-10-25T14:00:00.000Z",format,"100000","ACTIVE",AuditoriumDto("room",type,type,capacity,false),null,capacity,capacity)
        val catalog=object: CatalogRepository {
            override suspend fun dates()=listOf("2026-10-25")
            override suspend fun movie(id: String)=film
            override suspend fun movies(date: String)=CatalogDay(listOf(film),listOf(slot),"2026-10-25T00:00:00.000Z")
            override suspend fun showtime(id: String)=ShowtimeResponse("2026-10-25T00:00:00.000Z",slot)
            override suspend fun showtimes(id: String,date: String)=ShowtimesResponse(date,listOf(slot),"2026-10-25T00:00:00.000Z")
        }
        vm=CineplexViewModel(MockAccountAdapter(MockAccountRepository()),catalog,{"2026-10-25"},{0L})
        seats=SeatSelectionViewModel(FakeSeatRepository(type))
        compose.setContent {CineplexMockTheme {CineplexApp(vm,seats)}}
        compose.runOnUiThread {vm.update(AuthForm(email="user@cineplex.test",password="123456"));vm.submit()}
        compose.waitUntil(10000) {vm.movies.isNotEmpty()}
        compose.runOnUiThread {vm.showSlot(slot)}
        compose.waitUntil(10000) {vm.screen==Screen.SEAT_SUMMARY && seats.seats.isNotEmpty()}
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        File(compose.activity.getExternalFilesDir(null),"live-seats-$name.png").outputStream().use {
            checkNotNull(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()).compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }
    }
    private fun visibleSeatWidth()=compose.onAllNodes(
        hasContentDescription("Còn trống",substring=true) or hasContentDescription("Đang chọn",substring=true),useUnmergedTree=true
    ).fetchSemanticsNodes().maxOf { it.boundsInRoot.width }
    @Test fun standardCoupleStartsCountdownOnlyAfterServerConfirmation() {
        open("STANDARD");compose.onNodeWithTag("seat-countdown").assertDoesNotExist()
        compose.onNodeWithTag("seat-L1").performSemanticsAction(SemanticsActions.OnClick) {it()};assertEquals(setOf("L1","L2"),seats.selected)
        compose.onNodeWithTag("seat-A1").performSemanticsAction(SemanticsActions.OnClick) {it()};assertEquals(3,seats.places)
        compose.onNodeWithTag("seat-countdown").assertDoesNotExist();screenshot("standard")
        compose.onNodeWithTag("seat-continue").performClick();compose.waitUntil(5000) {vm.screen==Screen.SEAT_HANDOFF}
        compose.onNodeWithText("Đang giữ ghế").assertIsDisplayed();compose.onNodeWithTag("seat-countdown").assertIsDisplayed()
        val deadline=seats.deadlineMillis;compose.onNodeWithText("Quay lại sơ đồ").performClick()
        compose.waitUntil(5000) {vm.screen==Screen.SEAT_SUMMARY};assertEquals(deadline,seats.deadlineMillis)
        compose.onNodeWithText("Hủy giữ chỗ để đổi ghế").performClick();compose.waitUntil(5000) {!seats.held}
    }
    @Test fun pinchPanNeverSelectOrResetSeatsAndHasNoZoomButtons() {
        open("STANDARD");compose.onNodeWithTag("seat-A1").performSemanticsAction(SemanticsActions.OnClick) {it()};val before=seats.selected
        compose.onNodeWithTag("seat-zoom-in").assertDoesNotExist()
        compose.onNodeWithTag("seat-zoom-out").assertDoesNotExist()
        val overviewSeatWidth=visibleSeatWidth()
        compose.onNodeWithTag("seat-map").performTouchInput {
            pinch(start0=center-Offset(30f,0f),end0=center-Offset(120f,0f),start1=center+Offset(30f,0f),end1=center+Offset(120f,0f),durationMillis=400)
        }
        assertTrue("Pinch outward must visibly enlarge the seats",visibleSeatWidth()>overviewSeatWidth)
        compose.onNodeWithTag("seat-minimap").assertExists();assertEquals(before,seats.selected)
        screenshot("zoom")
        compose.onNodeWithTag("seat-map").performTouchInput {swipeLeft()};assertEquals(before,seats.selected)
        val zoomedSeatWidth=visibleSeatWidth()
        compose.onNodeWithTag("seat-map").performTouchInput {
            pinch(start0=center-Offset(120f,0f),end0=center-Offset(30f,0f),start1=center+Offset(120f,0f),end1=center+Offset(30f,0f),durationMillis=400)
        }
        assertTrue("Pinch inward must visibly shrink the seats",visibleSeatWidth()<zoomedSeatWidth)
        assertEquals(before,seats.selected)
        compose.onNodeWithTag("seat-fit").performClick();compose.onNodeWithTag("seat-minimap").assertDoesNotExist()
        assertNull(seats.deadlineMillis)
    }
    @Test fun imaxHas496SeatsAndVipLegend() {
        open("IMAX");assertEquals(496,seats.seats.size);assertTrue(seats.seats.all {it.seatType=="VIP"})
        compose.onNodeWithText("VIP",useUnmergedTree=true).assertExists();screenshot("imax")
    }
    private fun assertFirstTapSelectsWithoutZoom() {
        val bounds=compose.onNodeWithTag("seat-A1").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("seat-minimap").assertDoesNotExist()
        compose.onNodeWithTag("seat-A1").performTouchInput {click()}
        compose.waitUntil(3000) {"A1" in seats.selected}
        assertEquals(setOf("A1"),seats.selected)
        assertEquals(bounds,compose.onNodeWithTag("seat-A1").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithTag("seat-minimap").assertDoesNotExist()
        compose.onNodeWithTag("seat-A1").performTouchInput {click()}
        compose.waitUntil(3000) {seats.selected.isEmpty()}
        compose.onNodeWithTag("seat-minimap").assertDoesNotExist()
        assertNull(seats.deadlineMillis)
    }
    @Test fun physicalTapSelectsOverviewImmediatelyWithoutZoom() {
        open("STANDARD")
        assertFirstTapSelectsWithoutZoom()
    }
    @Test fun denseImaxOverviewCanSelectWithoutAutomaticZoom() {
        open("IMAX")
        assertFirstTapSelectsWithoutZoom()
    }
    @Test fun screenxHas180SeatsAndThreeScreenSegments() {
        open("SCREENX");assertEquals(180,seats.seats.size);assertEquals(listOf("FRONT","LEFT","RIGHT"),seats.layout?.screens)
        compose.onNodeWithTag("seat-K1").performSemanticsAction(SemanticsActions.OnClick) {it()};assertEquals(2,seats.places);screenshot("screenx")
    }
    @Test fun goldHas32IndependentSeats() {
        open("GOLD");assertEquals(32,seats.seats.size);compose.onNodeWithTag("seat-A1").performSemanticsAction(SemanticsActions.OnClick) {it()};assertEquals(1,seats.places)
        compose.onNodeWithTag("seat-A2").performSemanticsAction(SemanticsActions.OnClick) {it()};assertEquals(2,seats.places);screenshot("gold")
    }
}
