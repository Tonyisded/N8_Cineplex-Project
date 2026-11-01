package com.nhom8.cineplex
import com.nhom8.cineplex.ui.*
import org.junit.Assert.*
import org.junit.Test
class CommerceModelTest {
 @Test fun firstPurchaseHalvesWholeBillIncludingCombos() {
  val q=priceQuote(240000,100000,"FIRST50")
  assertEquals(340000L,q.subtotal); assertEquals(170000L,q.discount);assertEquals(170000L,q.due)
  assertEquals(50001L,priceQuote(100001,0,"FIRST50").due)
 }
 @Test fun ordinaryVoucherHasMinimumAndDoesNotStack() {
  assertEquals(0L,priceQuote(299999,0,"CINE20").discount)
  assertEquals(20000L,priceQuote(300000,0,"CINE20").discount)
  assertThrows(IllegalArgumentException::class.java) { priceQuote(-1,0,"") }
 }
 @Test fun unknownAndFailureDoNotPretendSuccess() {
  val s=CommerceState();s.snapshot=BookingSnapshot("id","attempt","movie","slot","room",listOf("A1"),emptyList(),priceQuote(100000,0,"FIRST50"),"Name","a@example.com","0901234567","Bank")
  s.finish("unknown");assertEquals(CommercePage.UNKNOWN,s.page)
  s.finish("failed");assertEquals(CommercePage.FAILED,s.page)
  s.finish("success");s.finish("success");assertEquals(CommercePage.SUCCESS,s.page)
 }
 @Test fun newHoldResetsDraftButBackKeepsCart() {
  val s=CommerceState();s.bindHold("a");s.cart=ComboSelection(ComboChoice("one","Combo","",100000,0),1);s.page=CommercePage.CHECKOUT
  s.bindHold("a");assertNotNull(s.cart);assertEquals(CommercePage.CHECKOUT,s.page)
  s.bindHold("b");assertNull(s.cart);assertEquals(CommercePage.COMBOS,s.page)
 }
 @Test fun cartChangeRechecksVoucherMinimum() {
  val s=CommerceState();s.setCart(ComboChoice("one","Combo","",100000,0),1);s.promoInput="CINE20";s.applyPromo(240000)
  assertEquals("CINE20",s.promoApplied);s.setCart(s.cart!!.product,0);s.reprice(240000)
  assertEquals("",s.promoApplied);assertEquals(240000L,s.quote(240000)!!.due)
 }
 @Test fun quantitiesAreCappedAndZeroRemovesItem() {
  val s=CommerceState();val c=ComboChoice("one","Combo","",100000,0)
  s.setCart(c,99);assertEquals(9,s.cart!!.quantity);s.setCart(c,0);assertNull(s.cart)
 }
}
