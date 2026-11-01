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
  val order=HoldOrder("order","slot","PENDING_PAYMENT","340000","20000","150000","320000","170000","2030-01-01T00:00:00Z",emptyList())
  val repo=object:CommerceBackend {
   override suspend fun combos()=listOf(ServerCombo("combo","Combo","", "100000",status="AVAILABLE"))
   override suspend fun balance()=if(calls>1)0L else 150000L
   override suspend fun quote(id:String,body:QuoteBody)=ServerQuote("340000","20000","150000","320000","170000","150000","CINE20",order.expiresAt,"same-token")
   override suspend fun checkout(id:String,token:String):HoldResponse {tokens+=token;calls++;if(calls==1)throw ApiFailure("NETWORK_ERROR","timeout");return HoldResponse("2026-11-01T00:00:00Z",order)}
  }
  val vm=LiveCommerceState(repo);vm.bind(order);advanceUntilIdle();vm.select("combo");vm.promotion("CINE20");vm.price();advanceUntilIdle()
  assertEquals("170000",vm.quote!!.bankAmount);assertEquals(150000L,vm.balance)
  vm.confirm(SeatSelectionViewModel());advanceUntilIdle();assertTrue(vm.unknown);assertFalse(vm.confirmed)
  vm.confirm(SeatSelectionViewModel());advanceUntilIdle();assertTrue(vm.confirmed);assertFalse(vm.unknown);assertEquals(listOf("same-token","same-token"),tokens)
  vm.quantity(2);assertNull(vm.quote);vm.clear();assertEquals(0L,vm.balance);assertNull(vm.selected)
 }

 @Test fun inlineQuantityUpdatesSubtotalWithoutNetworkAndCheckoutFetchesAutomatically()=runTest {
  var quotes=0;var commits=0;var sent:QuoteBody?=null
  val order=HoldOrder("order","slot","PENDING_PAYMENT","320000","20000","150000","320000","170000","2030-01-01T00:00:00Z",listOf(OrderSeatDto("A1","A1","240000")))
  val repo=object:CommerceBackend {
   override suspend fun combos()=listOf(ServerCombo("a","Combo A","", "100000",status="AVAILABLE"),ServerCombo("b","Combo B","", "79000",status="AVAILABLE"),ServerCombo("sold","Hết hàng","","35000",status="UNAVAILABLE"))
   override suspend fun balance()=150000L
   override suspend fun quote(id:String,body:QuoteBody):ServerQuote {quotes++;sent=body;return ServerQuote("340000","0","150000","340000","190000","150000","",order.expiresAt,"token")}
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
}
