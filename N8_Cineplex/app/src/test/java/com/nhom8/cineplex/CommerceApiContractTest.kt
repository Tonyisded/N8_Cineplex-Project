package com.nhom8.cineplex
import com.nhom8.cineplex.data.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
class CommerceApiContractTest {
 @Test fun zeroQuantityIsSentAndOrderAcceptsCheckoutFields(){
  val json=Json {ignoreUnknownKeys=true;encodeDefaults=true}
  assertTrue(json.encodeToString(QuoteBody()).contains("\"combo_quantity\":0"))
  val quote=json.decodeFromString<ServerQuote>("""{"subtotal":"340000","discount":"20000","totalAmount":"320000","bankAmount":"320000","promotionCode":"CINE20","expiresAt":"2030-01-01T00:00:00Z","quoteToken":"token"}""")
  assertEquals(quote.totalAmount,quote.bankAmount)
  assertTrue(json.encodeToString(QuoteBody()).contains("\"voucher_code\":\"\""))
 }
}
