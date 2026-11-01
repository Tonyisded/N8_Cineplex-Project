package com.nhom8.cineplex

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.*
import com.nhom8.cineplex.ui.*
import com.nhom8.cineplex.ui.theme.CineplexMockTheme
import org.junit.*
import org.junit.Assert.*

class CommerceFlowUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun heldCoupleFlowsIntoComboAndCheckoutWithoutNewDeadline() {
        val film = MockMovies.all.first().copy(id = "commerce-film", age = "P")
        val slot = ShowtimeDto("commerce-slot", film.id, "fixture", "2026-10-25T12:00:00.000Z", "2026-10-25T14:00:00.000Z",
            "STANDARD_2D", "100000", "ACTIVE", AuditoriumDto("room", "Phòng 03", "STANDARD", 160, false), null, 160, 160)
        val catalog = object : CatalogRepository {
            override suspend fun dates() = listOf("2026-10-25")
            override suspend fun comingSoon() = emptyList<Movie>()
            override suspend fun movie(id: String) = film
            override suspend fun movies(date: String) = CatalogDay(listOf(film), listOf(slot), "2026-10-25T00:00:00.000Z")
            override suspend fun showtime(id: String) = ShowtimeResponse("2026-10-25T00:00:00.000Z", slot)
            override suspend fun showtimes(id: String, date: String) = ShowtimesResponse(date, listOf(slot), "2026-10-25T00:00:00.000Z")
        }
        val repository = FakeSeatRepository()
        repository.items = repository.items.map { if (it.seatCode in listOf("L1", "L2")) it.copy(price = "120000") else it }
        val vm = CineplexViewModel(MockAccountAdapter(MockAccountRepository()), catalog, { "2026-10-25" }, { 0L })
        val seats = SeatSelectionViewModel(repository)
        val commerce = CommerceState()
        compose.setContent { CineplexMockTheme { CineplexApp(vm, seats, commerce) } }
        compose.runOnUiThread { vm.update(AuthForm(email = "user@cineplex.test", password = "123456")); vm.submit() }
        compose.waitUntil(10000) { vm.movies.isNotEmpty() }
        compose.runOnUiThread { vm.showSlot(slot) }
        compose.waitUntil(10000) { vm.screen == Screen.SEAT_SUMMARY && seats.seats.isNotEmpty() }
        compose.onNodeWithTag("seat-L1").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithTag("seat-continue").performClick()
        compose.waitUntil(10000) { vm.screen == Screen.SEAT_HANDOFF && seats.held }
        val deadline = seats.deadlineMillis
        compose.onNodeWithText("Chọn bắp nước").assertIsDisplayed()
        compose.onNodeWithTag("seat-countdown").assertIsDisplayed()
        compose.runOnUiThread { commerce.cart = ComboSelection(ComboChoice("two", "Combo 2 người", "1 bắp lớn · 2 nước", 100_000, R.drawable.combo_two), 1) }
        compose.onNodeWithText("Tiếp tục").performClick()
        compose.waitUntil(5000) { commerce.page == CommercePage.CHECKOUT }
        assertEquals(deadline, seats.deadlineMillis)
        compose.runOnUiThread {
            commerce.promoInput = "CINE20"; commerce.applyPromo(seats.total)
        }
        assertEquals(320_000L, commerce.quote(seats.total)!!.due)
        compose.onNodeWithText("Còn thanh toán").assertExists()
        compose.runOnUiThread {
            commerce.name = "Nguyễn Minh Anh"; commerce.email = "minh.anh@example.com"; commerce.phone = "0901234567"
            commerce.nextOutcome = "unknown"
        }
        compose.onNodeWithTag("commerce-pay").performClick()
        compose.waitUntil(10000) { commerce.page == CommercePage.UNKNOWN }
        val attempt = commerce.snapshot!!.attemptId
        compose.onNodeWithText("Chưa xác định kết quả").assertIsDisplayed()
        compose.runOnUiThread { commerce.nextOutcome = "success" }
        compose.onNodeWithText("Kiểm tra lại giao dịch mẫu").performClick()
        compose.waitUntil(10000) { commerce.page == CommercePage.SUCCESS }
        assertEquals(attempt, commerce.snapshot!!.attemptId)
        compose.onNodeWithText("Mô phỏng thành công").assertIsDisplayed()
        compose.onNodeWithText("Xem vé mẫu").performClick()
        compose.onNodeWithText("Vé mẫu · không có giá trị soát vé").assertIsDisplayed()
        compose.runOnUiThread { vm.account() }
        compose.onNodeWithText("Voucher của tôi").assertExists()
        compose.runOnUiThread { vm.vouchers() }
        compose.onNodeWithText("Ưu đãi dành cho bạn").assertIsDisplayed()
        compose.onNodeWithTag("voucher-FIRST50").assertExists()
    }
}
