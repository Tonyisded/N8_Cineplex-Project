package com.nhom8.cineplex
import androidx.test.platform.app.InstrumentationRegistry
import com.nhom8.cineplex.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assume.assumeTrue
import java.util.UUID
import com.nhom8.cineplex.model.AuthForm
import org.junit.Assert.*
class CommerceServerUiTest {
 @Test fun androidRepositoryReadsCompleteCombosOverHttps()=runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val rows=ApiCommerceRepository(ApiAccountRepository(context)).combos()
  assertTrue(rows.isNotEmpty());assertEquals(rows.size,rows.map{it.id}.distinct().size)
  assertTrue(rows.all{it.price.toLong()>=0&&it.status in setOf("AVAILABLE","UNAVAILABLE")})
  assertTrue(rows.any{it.name.startsWith("Combo 2 người")&&it.price=="100000"})
  assertTrue(rows.any{it.name.startsWith("Bắp lớn")&&it.price=="59000"})
 }
 @Test fun authenticatedCheckoutKeepsDeadlineAndRestoresCreditOnce()=runBlocking {
  val args=InstrumentationRegistry.getArguments()
  assumeTrue("Dedicated disposable fixture required",args.containsKey("commerceEmail"))
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val account=ApiAccountRepository(context)
  val seats=ApiSeatRepository(context,account)
  val repo=ApiCommerceRepository(account)
  var id:String?=null
  try {
   account.login(AuthForm(email=args.getString("commerceEmail")!!,password=args.getString("commercePassword")!!),false)
   val hold=seats.hold(HoldRequest(UUID.randomUUID().toString(),args.getString("commerceSlot")!!,listOf(args.getString("commerceSeat")!!)));id=hold.item.id
   val combo=repo.combos().first{it.name.startsWith("Combo 2 người")&&it.status=="AVAILABLE"}
   val q=repo.quote(id,QuoteBody(combo.id,1,"CINE20"));assertEquals("170000",q.bankAmount)
   assertEquals(150000L,repo.balance())
   val confirmed=repo.checkout(id,q.quoteToken);assertEquals(hold.item.expiresAt,confirmed.item.expiresAt)
   repo.checkout(id,q.quoteToken);assertEquals(0L,repo.balance());assertEquals("PENDING_PAYMENT",seats.order(id).item.status)
   seats.cancel(id);seats.cancel(id);assertEquals(150000L,repo.balance())
  } finally {id?.let{runCatching{seats.cancel(it)}};account.logout()}
 }
}
