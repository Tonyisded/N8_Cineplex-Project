package com.nhom8.cineplex

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.*
import com.nhom8.cineplex.ui.*
import com.nhom8.cineplex.ui.theme.CineplexMockTheme
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Read-only VPS inventory and geometry; never submits a booking. */
class SeatServerUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private fun show(type: String) {
        val catalog=ApiCatalogRepository()
        val days=runBlocking {catalog.dates().map {catalog.movies(it)}}
        val pair=days.firstNotNullOfOrNull {day -> day.showtimes.firstOrNull {it.auditorium.type==type && it.availableSeats>0}?.let {slot -> day.movies.first {it.id==slot.movieId} to slot}}
        assertNotNull("No live $type showtime available; do not substitute fake inventory",pair)
        val (movie,slot)=pair!!
        val connection=URL("${BuildConfig.API_BASE_URL}showtimes/${slot.id}/seats").openConnection() as HttpsURLConnection
        connection.connectTimeout=15000;connection.readTimeout=25000
        val snapshot=try {assertEquals(200,connection.responseCode);Json {ignoreUnknownKeys=true}.decodeFromString<SeatResponse>(connection.inputStream.bufferedReader().use {it.readText()})} finally {connection.disconnect()}
        assertEquals(slot.totalSeats,snapshot.items.size);assertTrue(snapshot.layout.key.endsWith("v1"))
        val readonly=object: SeatRepository {
            override suspend fun seats(showtimeId: String)=snapshot.also {check(it.showtimeId==showtimeId)}
            override suspend fun hold(request: HoldRequest): HoldResponse=error("Read-only test must not reserve inventory")
            override suspend fun order(id: String): HoldResponse=error("No order in read-only test")
            override suspend fun cancel(id: String): HoldResponse=error("No cancellation in read-only test")
        }
        val vm=CineplexViewModel(MockAccountAdapter(MockAccountRepository()),catalog)
        val state=SeatSelectionViewModel(readonly)
        compose.setContent {CineplexMockTheme {CineplexApp(vm,state)}}
        compose.runOnUiThread {vm.update(AuthForm(email="user@cineplex.test",password="123456"));vm.submit()}
        compose.waitUntil(30000) {vm.movies.isNotEmpty()}
        compose.runOnUiThread {vm.selectCatalogDate(vietnamDate(java.util.Date(isoMillis(slot.startAt))))}
        compose.waitUntil(30000) {!vm.catalogLoading && vm.movies.any {it.id==movie.id}}
        compose.runOnUiThread {vm.showSlot(slot)}
        compose.waitUntil(30000) {vm.screen==Screen.SEAT_SUMMARY && state.seats.isNotEmpty()}
        assertEquals(snapshot.items.size,state.seats.size)
        compose.onNodeWithTag("seat-countdown").assertDoesNotExist()
        compose.onNodeWithTag("seat-continue").assertIsNotEnabled()
        compose.waitForIdle()
        File(compose.activity.getExternalFilesDir(null),"live-seats-vps-$type.png").outputStream().use {
            checkNotNull(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()).compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }
    }
    @Test fun standardInventoryAndGeometryFromVps()=show("STANDARD")
    @Test fun imaxInventoryAndGeometryFromVps()=show("IMAX")
    @Test fun screenxInventoryAndGeometryFromVps()=show("SCREENX")
    @Test fun goldInventoryAndGeometryFromVps()=show("GOLD")
}
