package com.nhom8.cineplex
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionRefreshTest {
 @Before fun before(){Dispatchers.setMain(StandardTestDispatcher())}
 @After fun after(){Dispatchers.resetMain()}
 private val expiry="2030-01-01T00:00:00.000Z"
 private fun payment()=ServerPayment("p","order","PENDING","PROCESSING","50001",expiry,qrCode="signed-provider-qr")
 private fun ticket(status:String="VALID")=ServerTicket("ticket","order",status,"N8TEST",expiry,expiry,TicketSnapshot(totalAmount="50001"),refundEligible=status=="VALID")
 private open inner class Backend:TicketsBackend {
  override suspend fun orders()=emptyList<CustomerOrder>()
  override suspend fun tickets()=listOf(ticket())
  override suspend fun ticket(id:String)=ticket()
  override suspend fun qr(id:String)="N8T:signed"
  override suspend fun payment(id:String)=payment()
  override suspend fun paymentStatus(id:String)=payment()
  override suspend fun checkPayment(id:String):ServerPayment=error("automatic refresh must not invoke provider check")
  override suspend fun refund(id:String):ServerRefund=error("automatic refresh must not resubmit refund")
 }
 @Test fun paymentRefreshOnlyReadsAndPreservesOrderAmountDeadline()=runTest {
  var creates=0;var reads=0
  val state=TicketsState(object:Backend(){
   override suspend fun payment(id:String):ServerPayment {creates++;return payment()}
   override suspend fun paymentStatus(id:String):ServerPayment {reads++;return payment().copy(status="PAID",operationState="SUCCEEDED",ticketId="ticket")}
  })
  state.openPayment("order",expiry,"50001");advanceUntilIdle();state.refreshStatus()
  assertEquals(1,creates);assertEquals(1,reads);assertEquals("PAID",state.payment!!.status);assertEquals("ticket",state.payment!!.ticketId);assertEquals(expiry,state.payment!!.expiresAt);assertEquals("order",state.paymentOrderId)
 }
 @Test fun refundRefreshMovesPendingToSuccessWithoutResubmitting()=runTest {
  var status="VALID";var refunds=0
  val state=TicketsState(object:Backend(){
   override suspend fun ticket(id:String):ServerTicket=this@TransactionRefreshTest.ticket(status)
   override suspend fun refund(id:String):ServerRefund {refunds++;status="REFUND_PENDING";return ServerRefund("r","PENDING","REQUESTED","50001")}
  })
  state.openTicket("ticket");advanceUntilIdle();state.refund();advanceUntilIdle();assertNull(state.qr)
  status="REFUNDED";state.refreshStatus();assertEquals("REFUNDED",state.selected!!.status);assertNull(state.qr);assertEquals(1,refunds)
 }
 @Test fun networkFailureKeepsPaymentQrAndBacksOffThenRecovers()=runTest {
  var fail=true
  val state=TicketsState(object:Backend(){override suspend fun paymentStatus(id:String):ServerPayment {if(fail)throw java.io.IOException("offline");return payment()}})
  state.openPayment("order",expiry,"50001");advanceUntilIdle();state.refreshStatus()
  assertEquals("signed-provider-qr",state.payment!!.qrCode);assertEquals(6000L,state.refreshDelayMillis);assertTrue(state.refreshError.isNotBlank())
  fail=false;state.refreshStatus();assertEquals(3000L,state.refreshDelayMillis);assertEquals("",state.refreshError)
 }
 @Test fun logoutDuringRefreshCannotRestoreOldAccountData()=runTest {
  val deferred=CompletableDeferred<ServerPayment>()
  val state=TicketsState(object:Backend(){override suspend fun paymentStatus(id:String)=deferred.await()})
  state.openPayment("order",expiry,"50001");advanceUntilIdle();val job=launch{state.refreshStatus()};runCurrent();state.clear();deferred.complete(payment().copy(status="PAID",ticketId="old"));job.join()
  assertNull(state.payment);assertNull(state.paymentOrderId);assertNull(state.selected)
 }
 @Test fun staleQuietResponseCannotOverwriteManualPaidConfirmation()=runTest {
  val deferred=CompletableDeferred<ServerPayment>()
  val state=TicketsState(object:Backend(){
   override suspend fun paymentStatus(id:String)=deferred.await()
   override suspend fun checkPayment(id:String)=payment().copy(status="PAID",operationState="SUCCEEDED",ticketId="ticket")
  })
  state.openPayment("order",expiry,"50001");advanceUntilIdle();val quiet=launch{state.refreshStatus()};runCurrent()
  state.checkPayment();runCurrent();assertEquals("PAID",state.payment!!.status)
  deferred.complete(payment());quiet.join();assertEquals("PAID",state.payment!!.status)
 }
 @Test fun staleQuietTicketCannotRestoreQrAfterRefundStarted()=runTest {
  var calls=0;val deferred=CompletableDeferred<ServerTicket>()
  val state=TicketsState(object:Backend(){
   override suspend fun ticket(id:String):ServerTicket {calls++;return when(calls){1->this@TransactionRefreshTest.ticket();2->deferred.await();else->this@TransactionRefreshTest.ticket("REFUND_PENDING")}}
   override suspend fun refund(id:String)=ServerRefund("r","PENDING","REQUESTED","50001")
  })
  state.openTicket("ticket");advanceUntilIdle();val quiet=launch{state.refreshStatus()};runCurrent();state.refund();runCurrent()
  assertEquals("REFUND_PENDING",state.selected!!.status);deferred.complete(ticket());quiet.join();assertEquals("REFUND_PENDING",state.selected!!.status);assertNull(state.qr)
 }

 @Test fun confirmedAutomaticReadClearsAnEarlierManualCheckError()=runTest {
  val state=TicketsState(object:Backend(){
   override suspend fun checkPayment(id:String):ServerPayment=throw java.io.IOException("lost response")
   override suspend fun paymentStatus(id:String)=payment().copy(status="PAID",operationState="SUCCEEDED",ticketId="ticket")
  })
  state.openPayment("order",expiry,"50001");advanceUntilIdle();state.checkPayment();advanceUntilIdle();assertTrue(state.paymentError.isNotBlank())
  state.refreshStatus();assertEquals("PAID",state.payment!!.status);assertEquals("",state.paymentError)
 }

}
