package com.nhom8.cineplex

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.*
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.theme.CineplexMockTheme
import org.junit.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CatalogDateUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var vm: CineplexViewModel
    @Before fun setup() {
        val today = "2026-10-10"
        val catalog = object : CatalogRepository {
            val films = listOf(MockMovies.all[0].copy(name = "Phim hôm nay",soon = false),MockMovies.all[1].copy(name = "Phim ngày mai",soon = false))
            override suspend fun dates() = listOf(today,"2026-10-11")
            override suspend fun comingSoon() = emptyList<Movie>()
            override suspend fun movies(date: String): CatalogDay {
                val selected = when(date) { today -> listOf(films[0]); "2026-10-11" -> listOf(films[1]); else -> emptyList() }
                return CatalogDay(selected, selected.flatMap { showtimes(it.id,date).items }, "${today}T00:00:00.000Z")
            }
            override suspend fun movie(id: String) = films.first { it.id == id }
            override suspend fun showtime(id: String) = ShowtimeResponse("${today}T00:00:00.000Z", showtimes(films.first().id, id.removePrefix("slot-")).items.single())
            override suspend fun showtimes(id: String,date: String) = ShowtimesResponse(date,listOf(ShowtimeDto("slot-$date",id,"fixture:$date","${date}T03:00:00.000Z","${date}T05:00:00.000Z","STANDARD_2D","100000","ACTIVE",AuditoriumDto("room","Cinema 3","STANDARD",160,false),"Phụ đề Việt",160,160)),"${today}T00:00:00.000Z")
        }
        vm = CineplexViewModel(MockAccountAdapter(MockAccountRepository()),catalog,{ today })
        compose.setContent { CineplexMockTheme { CineplexApp(vm) } }
        compose.runOnUiThread { vm.update(AuthForm(email = "user@cineplex.test",password = "123456")); vm.submit() }
        compose.waitUntil(5_000) { !vm.catalogLoading && vm.movies.isNotEmpty() }
    }
    private fun grid() = compose.onNode(hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
    @Test fun homeDateChipsDriveDetailAndBackRetainsSelection() {
        compose.onNodeWithTag("catalog-date-2026-10-10").assertIsSelected()
        screenshot("dates-home")
        compose.onNodeWithTag("catalog-date-2026-10-11").performClick()
        compose.waitUntil(5_000) { vm.catalogDate == "2026-10-11" && !vm.catalogLoading }
        grid().performScrollToNode(hasContentDescription("Xem chi tiết Phim ngày mai"))
        compose.onNodeWithContentDescription("Xem chi tiết Phim ngày mai").performClick()
        compose.waitUntil(5_000) { !vm.detailLoading }
        Assert.assertEquals("2026-10-11T03:00:00.000Z",vm.showtimes.single().startAt)
        compose.onNodeWithText("Chọn suất chiếu").performClick()
        compose.waitUntil(5_000) { vm.screen == com.nhom8.cineplex.model.Screen.SHOWTIMES && !vm.detailLoading }
        compose.onNodeWithText("Ngày xem: 11/10/2026").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("schedule-slot-slot-2026-10-11").performScrollTo().assertIsDisplayed()
        screenshot("dates-detail")
        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.onNodeWithContentDescription("Quay lại").performClick()
        grid().performScrollToNode(hasTestTag("catalog-date-2026-10-11"))
        compose.onNodeWithTag("catalog-date-2026-10-11").assertIsSelected()
    }
    @Test fun emptyDayShowsMessageAndCalendarCanSelectFartherUnpublishedDay() {
        compose.onNodeWithTag("catalog-date-2026-10-12").performClick()
        compose.waitUntil(5_000) { vm.catalogDate == "2026-10-12" && !vm.catalogLoading }
        val empty = "Chưa có lịch chiếu cho ngày 12/10/2026"
        grid().performScrollToNode(hasText(empty)); compose.onNodeWithText(empty).assertIsDisplayed()
        screenshot("dates-empty")
        grid().performScrollToNode(hasTestTag("open-date-picker")); compose.onNodeWithTag("open-date-picker").performClick()
        compose.onNodeWithText("Xem lịch").assertIsDisplayed()
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("October 13",substring = true),useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        screenshot("dates-calendar")
        compose.onNode(hasText("October 13",substring = true),useUnmergedTree = true).performClick()
        compose.onNodeWithText("Xem lịch").performClick()
        compose.waitUntil(5_000) { vm.catalogDate == "2026-10-13" && !vm.catalogLoading }
        Assert.assertTrue(vm.movies.isEmpty()); Assert.assertEquals("",vm.catalogError)
        grid().performScrollToNode(hasText("Chưa có lịch chiếu cho ngày 13/10/2026"))
        compose.onNodeWithText("Chưa có lịch chiếu cho ngày 13/10/2026").assertIsDisplayed()
    }
    private fun screenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.getExternalFilesDir(null),"visual_checks").apply { mkdirs() }
        File(folder,"$name.png").outputStream().use { InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
}
