package com.nhom8.cineplex
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
@OptIn(ExperimentalCoroutinesApi::class)
class LiveCommerceStateTest {
 @Before fun setup(){Dispatchers.setMain(StandardTestDispatcher())}
 @After fun cleanup(){Dispatchers.resetMain()}
 @Test fun serverQuoteIsAuthoritativeAndUnknownRetriesSameToken()=runTest {
  var calls=0;val tokens=mutableListOf<String>()
  val order=HoldOrder("order","slot","PENDING_PAYMENT","340000","20000","320000","320000","2030-01-01T00:00:00Z",emptyList())
  val repo=object:CommerceBackend {
   override suspend fun combos()=listOf(ServerCombo("combo","Combo","", "100000",status="AVAILABLE"))
   override suspend fun vouchers()=listOf(ServerVoucher("CINE20","Voucher","", "300000", "cinema"), ServerVoucher("FIRST50","Chào bạn mới","", "0", "welcome"))
   override suspend fun quote(id:String,body:QuoteBody)=ServerQuote("340000","20000","320000","320000","CINE20",order.expiresAt,"same-token")
   override suspend fun checkout(id:String,token:String):HoldResponse {tokens+=token;calls++;if(calls==1)throw ApiFailure("NETWORK_ERROR","timeout");return HoldResponse("2026-11-01T00:00:00Z",order)}
  }
  val vm=LiveCommerceState(repo);vm.bind(order);advanceUntilIdle();vm.select("combo");vm.promotion("CINE20");vm.price();advanceUntilIdle()
  assertEquals("320000",vm.quote!!.bankAmount)
  vm.confirm(SeatSelectionViewModel());advanceUntilIdle();assertTrue(vm.unknown);assertFalse(vm.confirmed)
  vm.confirm(SeatSelectionViewModel());advanceUntilIdle();assertTrue(vm.confirmed);assertFalse(vm.unknown);assertEquals(listOf("same-token","same-token"),tokens)
  vm.quantity(2);assertNull(vm.quote);vm.clear();assertTrue(vm.vouchers.isEmpty());assertNull(vm.selected)
 }

 @Test fun inlineQuantityUpdatesSubtotalWithoutNetworkAndCheckoutFetchesAutomatically()=runTest {
  var quotes=0;var commits=0;var sent:QuoteBody?=null
  val order=HoldOrder("order","slot","PENDING_PAYMENT","320000","20000","320000","320000","2030-01-01T00:00:00Z",listOf(OrderSeatDto("A1","A1","240000")))
  val repo=object:CommerceBackend {
   override suspend fun combos()=listOf(ServerCombo("a","Combo A","", "100000",status="AVAILABLE"),ServerCombo("b","Combo B","", "79000",status="AVAILABLE"),ServerCombo("sold","Hết hàng","","35000",status="UNAVAILABLE"))
   override suspend fun vouchers()=listOf(ServerVoucher("CINE20","Voucher","", "300000", "cinema"), ServerVoucher("FIRST50","Chào bạn mới","", "0", "welcome"))
   override suspend fun quote(id:String,body:QuoteBody):ServerQuote {quotes++;sent=body;return ServerQuote("340000","0","340000","340000","",order.expiresAt,"token")}
   override suspend fun checkout(id:String,token:String):HoldResponse {commits++;return HoldResponse("2026-11-01T00:00:00Z",order)}
  }
  val vm=LiveCommerceState(repo);vm.bind(order);advanceUntilIdle()
  assertEquals(240000L,vm.estimatedSubtotal);assertEquals(LiveCommerceStep.COMBOS,vm.step)
  vm.select("a");assertEquals(340000L,vm.estimatedSubtotal)
  vm.quantity(2);assertEquals(440000L,vm.estimatedSubtotal)
  vm.quantity(99);assertEquals(9,vm.quantity)
  vm.quantity(0);assertNull(vm.selected);assertEquals(240000L,vm.estimatedSubtotal)
  vm.select("sold");assertNull(vm.selected)
  vm.select("a");vm.select("b");assertEquals(1,vm.quantity);assertEquals(319000L,vm.estimatedSubtotal)
  vm.select("a");assertEquals(0,quotes);assertEquals(0,commits)
  vm.continueToCheckout();assertTrue(vm.busy);advanceUntilIdle()
  assertEquals(LiveCommerceStep.CHECKOUT,vm.step);assertEquals(1,quotes);assertEquals("a",sent!!.combo_id);assertEquals(0,commits)
  assertTrue(vm.backToCombos());assertEquals("a",vm.selected);assertEquals(1,vm.quantity)
  vm.quantity(2);assertNull(vm.quote);assertEquals(440000L,vm.estimatedSubtotal)
  vm.clear();assertEquals(LiveCommerceStep.COMBOS,vm.step);assertEquals(0L,vm.estimatedSubtotal)
 }

 private fun integrationOrder() = HoldOrder("integration-order", "slot", "PENDING_PAYMENT", "350000", "20000", "330000", "330000", "2030-01-01T00:00:00Z", listOf(OrderSeatDto("A1", "A1", "240000")), "combo", 1)
 private fun integrationQuote(o: HoldOrder) = ServerQuote(o.subtotal, o.discount, o.totalAmount, o.bankAmount, "CINE20", o.expiresAt, "integration-token")
 private fun integrationRepo(o: HoldOrder) = object : CommerceBackend {
  override suspend fun combos() = listOf(ServerCombo("combo", "Combo", "", "100000", status = "AVAILABLE"))
  override suspend fun vouchers()=listOf(ServerVoucher("CINE20","Voucher","", "300000", "cinema"), ServerVoucher("FIRST50","Chào bạn mới","", "0", "welcome"))
  override suspend fun quote(id: String, body: QuoteBody) = integrationQuote(o)
  override suspend fun checkout(id: String, token: String) = HoldResponse("2026-11-01T00:00:00Z", o)
 }

 @Test fun quoteCannotChangeOriginalHoldDeadline() = runTest {
  val order = integrationOrder()
  val repo = object : CommerceBackend by integrationRepo(order) {
   override suspend fun quote(id: String, body: QuoteBody) = integrationQuote(order.copy(expiresAt = "2030-01-01T00:00:01Z"))
  }
  val vm = LiveCommerceState(repo); vm.bind(order); advanceUntilIdle()
  vm.continueToCheckout(); advanceUntilIdle()
  assertNull(vm.quote); assertFalse(vm.confirmed); assertTrue(vm.message.isNotBlank())
 }

 @Test fun checkoutCannotAcceptChangedDeadlineOrDifferentServerTotals() = runTest {
  val order = integrationOrder()
  for (response in listOf(order.copy(expiresAt = "2030-01-01T00:00:01Z"), order.copy(bankAmount = "1"))) {
   val repo = object : CommerceBackend by integrationRepo(order) {
    override suspend fun checkout(id: String, token: String) = HoldResponse("2026-11-01T00:00:00Z", response)
   }
   val vm = LiveCommerceState(repo); vm.bind(order); advanceUntilIdle(); vm.price(); advanceUntilIdle()
   vm.confirm(SeatSelectionViewModel()); advanceUntilIdle()
   assertFalse(vm.confirmed); assertTrue(vm.unknown); assertEquals("integration-token", vm.quote?.quoteToken)
   vm.clear()
  }
 }

 @Test fun zeroBankKeepsPendingOrderAndRetriesTheSameQuote() = runTest {
  val order = integrationOrder().copy(subtotal = "0", discount = "0", totalAmount = "0", bankAmount = "0")
  val tokens = mutableListOf<String>(); var applied = false
  val repo = object : CommerceBackend by integrationRepo(order) {
   override suspend fun vouchers()=listOf(ServerVoucher("CINE20","Voucher","", "300000", "cinema"), ServerVoucher("FIRST50","Chào bạn mới","", "0", "welcome"))
   override suspend fun checkout(id: String, token: String): HoldResponse {
    assertEquals(order.id, id); tokens += token
    if (!applied) { applied = true; throw ApiFailure("NETWORK_ERROR", "lost response") }
    return HoldResponse("2026-11-01T00:00:00Z", order)
   }
  }
  val vm = LiveCommerceState(repo); vm.bind(order); advanceUntilIdle(); vm.price(); advanceUntilIdle()
  vm.confirm(SeatSelectionViewModel()); advanceUntilIdle(); assertTrue(vm.unknown)
  vm.price(); vm.select(null); assertEquals("integration-token", vm.quote?.quoteToken)
  vm.confirm(SeatSelectionViewModel()); advanceUntilIdle()
  assertTrue(vm.confirmed); assertFalse(vm.unknown); assertEquals("0", vm.quote?.bankAmount)
  assertEquals(listOf("integration-token", "integration-token"), tokens)
 }

 @Test fun checkoutUsesServerTotalsWithoutRestartingSeatCountdown() = runTest {
  var now = 0L
  val seatRepo = FakeSeatRepository(clock = { now }); seatRepo.items = seatRepo.items.map { it.copy(price = "240000") }
  val seats = SeatSelectionViewModel(seatRepo) { now }; seats.bind("user", "slot"); advanceUntilIdle()
  seats.select("A1"); seats.continueToSummary {}; advanceUntilIdle()
  val hold = seats.order!!; val deadline = seats.deadlineMillis
  val priced = integrationOrder().copy(id = hold.id, expiresAt = hold.expiresAt)
  val vm = LiveCommerceState(integrationRepo(priced)); vm.bind(hold); advanceUntilIdle(); vm.select("combo"); vm.promotion("CINE20")
  assertEquals(340000L, vm.estimatedSubtotal)
  now = 120000L; seats.refreshClock(); assertEquals(480, seats.secondsLeft)
  vm.continueToCheckout(); advanceUntilIdle()
  assertEquals("350000", vm.quote?.subtotal); assertEquals("330000", vm.quote?.bankAmount)
  assertTrue(vm.backToCombos()); vm.continueToCheckout(); advanceUntilIdle()
  vm.confirm(seats); advanceUntilIdle()
  assertTrue(vm.confirmed); assertEquals(deadline, seats.deadlineMillis); assertEquals(480, seats.secondsLeft); assertEquals(1, seatRepo.holds)
  now = 600000L; seats.refreshClock(); advanceUntilIdle()
  assertFalse(seats.held); assertEquals(0, seats.secondsLeft); assertTrue(seats.expiredDialog)
 }
}
