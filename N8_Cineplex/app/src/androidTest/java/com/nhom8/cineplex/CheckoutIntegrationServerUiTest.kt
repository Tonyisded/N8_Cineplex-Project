package com.nhom8.cineplex

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.AuthForm
import com.nhom8.cineplex.model.LoginResult
import com.nhom8.cineplex.ui.*
import com.nhom8.cineplex.ui.screens.LiveCommerceScreen
import com.nhom8.cineplex.ui.theme.CineplexMockTheme
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class CheckoutIntegrationServerUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun positiveBankCheckoutDisplaysHttpsQuote() = checkout(false)
    @Test fun zeroBankCheckoutStillHoldsSeatsWithoutClaimingTicket() = checkout(true)

    private fun checkout(zeroBank: Boolean) {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("A newly approved disposable fixture is required", args.containsKey("commerceZeroSlot"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val account = ApiAccountRepository(context)
        val apiSeats = ApiSeatRepository(context, account)
        val commerce = ApiCommerceRepository(account)
        var initial: HoldResponse? = null
        val repository = object : SeatRepository by apiSeats {
            override suspend fun hold(request: HoldRequest) = apiSeats.hold(request).also { initial = it }
        }
        val seats = SeatSelectionViewModel(repository)
        val state = LiveCommerceState(commerce)
        val vm = CineplexViewModel()
        val slot = args.getString(if (zeroBank) "commerceZeroSlot" else "commerceSlot")!!
        val seat = args.getString(if (zeroBank) "commerceZeroSeat" else "commerceSeat")!!
        var id: String? = null
        try {
            val session = runBlocking {
                (account.login(AuthForm(email = args.getString("commerceEmail")!!,
                    password = args.getString("commercePassword")!!), false) as LoginResult.Success).session
            }
            compose.setContent { CineplexMockTheme { LiveCommerceScreen(vm, seats, state) } }
            compose.runOnUiThread { seats.bind(session.id, slot) }
            compose.waitUntil(15000) { seats.seats.isNotEmpty() }
            compose.runOnUiThread { seats.select(seat); seats.continueToSummary {} }
            compose.waitUntil(15000) { seats.held && state.items.isNotEmpty() }
            val hold = checkNotNull(initial)
            id = hold.item.id
            val duration = isoMillis(hold.item.expiresAt) - isoMillis(hold.serverTime)
            assertTrue("Original hold must be 600 seconds, not a fresh checkout lease: $duration", duration in 590000L..600000L)
            val deadline = seats.deadlineMillis
            if (!zeroBank) {
                val combo = state.items.first { it.name.startsWith("Combo 2 người") && it.status == "AVAILABLE" }
                compose.onNodeWithTag("combo-plus-${combo.id}").performScrollTo().performClick()
            }
            compose.onNodeWithTag("commerce-next").performClick()
            compose.waitUntil(15000) { state.quote != null }
            if (!zeroBank) {
                compose.onNodeWithTag("commerce-promo").performScrollTo().performTextInput("CINE20")
                compose.onNodeWithText("Áp dụng").performClick()
                compose.waitUntil(15000) { state.quote?.promotionCode == "CINE20" }
            }
            val quote = checkNotNull(state.quote)
            assertEquals(if (zeroBank) "100000" else "340000", quote.subtotal)
            assertEquals(if (zeroBank) "0" else "20000", quote.discount)
            assertEquals(if (zeroBank) "100000" else "320000", quote.totalAmount)
            assertEquals(if (zeroBank) "100000" else "150000", quote.creditUsed)
            assertEquals(if (zeroBank) "0" else "170000", quote.bankAmount)
            assertEquals(hold.item.expiresAt, quote.expiresAt)
            compose.onNodeWithTag("commerce-total").assertTextEquals(if (zeroBank) "0đ" else "170.000đ")
            compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
            compose.waitUntil(5000) { state.step == LiveCommerceStep.COMBOS }
            assertEquals(deadline, seats.deadlineMillis)
            compose.onNodeWithTag("commerce-next").performClick()
            compose.waitUntil(15000) { state.quote != null && !state.busy }
            val token = checkNotNull(state.quote).quoteToken
            compose.onNodeWithTag("commerce-next").performClick()
            compose.waitUntil(15000) { state.confirmed && !state.busy }
            compose.onNodeWithTag("commerce-confirmation").performScrollTo()
                .assertTextContains(if (zeroBank) "ngân hàng 0đ" else "chờ thanh toán", substring = true)
            compose.onNodeWithTag("commerce-next").assertIsNotEnabled()
            compose.onNodeWithText("Thanh toán thành công", substring = true).assertDoesNotExist()
            compose.onNodeWithText("Mô phỏng thành công", substring = true).assertDoesNotExist()
            val image = File(context.getExternalFilesDir(null), "visual_checks/group-checkout-live-${if (zeroBank) "zero" else "positive"}.png")
            image.parentFile!!.mkdirs()
            image.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
            runBlocking {
                val retried = commerce.checkout(id, token)
                val current = apiSeats.order(id)
                assertEquals(hold.item.id, current.item.id)
                assertEquals(hold.item.expiresAt, retried.item.expiresAt)
                assertEquals(hold.item.expiresAt, current.item.expiresAt)
                assertEquals("PENDING_PAYMENT", current.item.status)
                assertEquals(quote.bankAmount, current.item.bankAmount)
                assertEquals(if (zeroBank) 50000L else 0L, commerce.balance())
                assertEquals("HELD", apiSeats.seats(slot).items.first { it.id == seat }.status)
            }
            compose.runOnUiThread { seats.refreshClock() }
            assertEquals(deadline, seats.deadlineMillis)
            assertTrue(seats.secondsLeft in 1..600)
            compose.onNodeWithText("Hủy giữ ghế").performScrollTo().performClick()
            compose.waitUntil(15000) { !seats.held && !seats.confirming }
            assertEquals(150000L, runBlocking { commerce.balance() })
        } finally {
            runBlocking { id?.let { apiSeats.cancel(it) } }
            compose.runOnUiThread { state.clear(); seats.clearSession() }
            runBlocking { account.logout() }
        }
    }
}
