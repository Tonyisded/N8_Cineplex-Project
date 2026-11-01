package com.nhom8.cineplex.data

import com.nhom8.cineplex.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

@Serializable data class ServerCombo(val id:String,val name:String,val description:String,val price:String,val imageUrl:String?=null,val status:String)
@Serializable data class ComboResponse(val serverTime:String,val items:List<ServerCombo>)
@Serializable data class QuoteBody(val combo_id:String?=null,val combo_quantity:Int=0,val voucher_code:String="")
@Serializable data class ServerQuote(val subtotal:String,val discount:String,val totalAmount:String,val bankAmount:String,val promotionCode:String,val expiresAt:String,val quoteToken:String)
@Serializable data class QuoteResponse(val serverTime:String,val item:ServerQuote)
@Serializable data class CheckoutBody(val quoteToken:String)
@Serializable data class ServerVoucher(val code:String,val title:String,val description:String,val minimum:String,val illustration:String)
@Serializable data class VoucherResponse(val items:List<ServerVoucher>)
private interface CommerceApi {
 @GET("combos") suspend fun combos():ComboResponse
 @GET("vouchers") suspend fun vouchers(@Header("Authorization") auth:String):VoucherResponse
 @POST("orders/{id}/quote") suspend fun quote(@Header("Authorization") auth:String,@Path("id") id:String,@Body body:QuoteBody):QuoteResponse
 @POST("orders/{id}/checkout") suspend fun checkout(@Header("Authorization") auth:String,@Path("id") id:String,@Body body:CheckoutBody):HoldResponse
}
interface CommerceBackend {
 suspend fun combos():List<ServerCombo>
 suspend fun vouchers():List<ServerVoucher>
 suspend fun quote(id:String,body:QuoteBody):ServerQuote
 suspend fun checkout(id:String,token:String):HoldResponse
}
class ApiCommerceRepository(private val account:ApiAccountRepository):CommerceBackend {
 private val api=Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL)
  .client(OkHttpClient.Builder().connectTimeout(15,TimeUnit.SECONDS).readTimeout(25,TimeUnit.SECONDS).build())
  .addConverterFactory(Json {ignoreUnknownKeys=true;encodeDefaults=true}.asConverterFactory("application/json".toMediaType())).build().create(CommerceApi::class.java)
 override suspend fun combos()=api.combos().items.also { rows -> check(rows.all { it.price.toLong()>=0 }) }
 override suspend fun vouchers()=account.withAuthorization { api.vouchers(it).items }
 override suspend fun quote(id:String,body:QuoteBody)=account.withAuthorization { api.quote(it,id,body).item.also { q->check(q.subtotal.toLong()>=0 && q.discount.toLong()>=0 && q.totalAmount.toLong()==q.subtotal.toLong()-q.discount.toLong() && q.bankAmount.toLong()==q.totalAmount.toLong() && q.bankAmount.toLong()>=0) } }
 override suspend fun checkout(id:String,token:String)=account.withAuthorization { api.checkout(it,id,CheckoutBody(token)) }
}
