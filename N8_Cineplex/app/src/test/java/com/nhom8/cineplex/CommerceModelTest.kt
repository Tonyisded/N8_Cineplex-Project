package com.nhom8.cineplex

import com.nhom8.cineplex.ui.*
import org.junit.Assert.*
import org.junit.Test

class CommerceModelTest {
    @Test fun exampleAndCreditLimits() {
        val quote = priceQuote(240_000, 100_000, "CINE20", 150_000, 100_000)
        assertEquals(340_000L, quote.subtotal)
        assertEquals(320_000L, quote.afterDiscount)
        assertEquals(220_000L, quote.due)
        assertThrows(IllegalArgumentException::class.java) { priceQuote(240_000, 100_000, "CINE20", 150_000, -1) }
        assertThrows(IllegalArgumentException::class.java) { priceQuote(240_000, 100_000, "CINE20", 150_000, 160_000) }
        assertEquals(0L, priceQuote(90_000, 0, "", 150_000, 90_000).due)
    }

    @Test fun coupleIsPricedOnceAndCreditPostsOnce() {
        val repository = DemoCommerceRepository()
        val quote = priceQuote(240_000, 100_000, "CINE20", repository.balance, 100_000)
        val snapshot = BookingSnapshot("order-1", "attempt-1", "Phim mẫu", "Suất mẫu", "Phòng 03", listOf("J1", "J2"), emptyList(), quote, "Nguyễn Văn A", "a@example.com", "0901234567", "VietQR")
        repository.complete(snapshot)
        repository.complete(snapshot)
        assertEquals(50_000L, repository.balance)
        assertEquals(1, repository.entries.count { it.id == "use-order-1" })
        assertEquals(240_000L, quote.seats)
    }

    @Test fun pendingAndFailedDoNotIncreaseBalance() {
        val repository = DemoCommerceRepository()
        assertEquals(150_000L, repository.balance)
        assertEquals(2, repository.entries.count { it.status != LedgerStatus.POSTED })
    }

    @Test fun unknownAndFailureKeepCreditUntilConfirmedSuccess() {
        val state = CommerceState()
        state.snapshot = BookingSnapshot("order-2", "attempt-2", "Phim", "Suất", "Phòng 03", listOf("A1"), emptyList(),
            priceQuote(90_000, 0, "", 150_000, 90_000), "Nguyễn Văn A", "a@example.com", "0901234567", "Credit")
        state.finish("unknown"); assertEquals(CommercePage.UNKNOWN, state.page); assertEquals(150_000L, state.repository.balance)
        state.finish("failed"); assertEquals(CommercePage.FAILED, state.page); assertEquals(150_000L, state.repository.balance)
        state.finish("success"); state.finish("success")
        assertEquals(CommercePage.SUCCESS, state.page); assertEquals(60_000L, state.repository.balance)
    }

    @Test fun newHoldResetsDraftButBackFromCheckoutKeepsCart() {
        val state = CommerceState()
        val product = ComboChoice("two", "Combo 2 người", "", 100_000, 0)
        state.bindHold("order-1"); state.cart = ComboSelection(product, 1); state.page = CommercePage.CHECKOUT
        state.bindHold("order-1"); assertEquals(CommercePage.CHECKOUT, state.page); assertNotNull(state.cart)
        state.bindHold("order-2"); assertEquals(CommercePage.COMBOS, state.page); assertNull(state.cart)
        state.zeroBalanceDemo(); assertEquals(0L, state.repository.balance)
    }

    @Test fun cartChangeRechecksPromoAndClampsCredit() {
        val state = CommerceState()
        val popcorn = ComboChoice("popcorn", "Bắp riêng", "", 49_000, 0)
        val drink = ComboChoice("drink", "Nước riêng", "", 35_000, 0)
        state.setCart(popcorn, 1, "Bơ · Lớn", 10_000)
        state.promoInput = "CINE20"; state.applyPromo(240_000)
        assertEquals("", state.promoApplied)
        state.setCart(drink, 1)
        assertEquals(0L, state.cart!!.surcharge)
        state.setCart(popcorn, 1, "Bơ · Lớn", 10_000)
        state.promoInput = "CINE20"; state.applyPromo(250_000)
        assertEquals("CINE20", state.promoApplied)
        state.creditEdited = true; state.creditInput = "150000"
        state.setCart(popcorn, 0); state.reprice(90_000)
        assertEquals("", state.promoApplied)
        assertEquals("90000", state.creditInput)
        assertEquals(0L, state.quote(90_000)!!.due)
    }
}
