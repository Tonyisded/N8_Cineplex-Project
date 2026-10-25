package com.nhom8.cineplex.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nhom8.cineplex.BuildConfig
import io.socket.client.IO
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*
import java.io.Closeable
import java.net.URI
import java.util.concurrent.TimeUnit

@Serializable data class SeatDto(val id: String, val seatCode: String, val rowName: String, val seatNumber: Int, val seatType: String, val price: String, val status: String) {
    val kindLabel get() = when(seatType) { "VIP" -> "VIP"; "COUPLE" -> "Couple"; "GOLD" -> "Gold Class"; else -> "Thường" }
}
@Serializable data class SeatPosition(val seatId: String, val x: Float, val y: Float, val pairKey: String? = null)
@Serializable data class SeatLayout(val key: String, val width: Float, val height: Float, val screens: List<String>, val seats: List<SeatPosition>)
@Serializable data class SeatResponse(val serverTime: String, val showtimeId: String, val items: List<SeatDto>, val layout: SeatLayout)
@Serializable data class OrderSeatDto(val id: String, val seatCode: String, val price: String)
@Serializable data class HoldOrder(val id: String, val showtimeId: String, val status: String, val subtotal: String, val discount: String, val creditUsed: String, val totalAmount: String, val bankAmount: String, val expiresAt: String, val seats: List<OrderSeatDto>)
@Serializable data class HoldResponse(val serverTime: String, val item: HoldOrder)
@Serializable data class HoldBody(val showtime_id: String, val seat_ids: List<String>)
@Serializable data class HoldRequest(val key: String, val showtimeId: String, val seatIds: List<String>)

interface SeatRepository {
    suspend fun seats(showtimeId: String): SeatResponse
    suspend fun hold(request: HoldRequest): HoldResponse
    suspend fun order(id: String): HoldResponse
    suspend fun cancel(id: String): HoldResponse
    suspend fun saved(userId: String, showtimeId: String): HoldRequest? = null
    suspend fun requests(userId: String): List<HoldRequest> = emptyList()
    suspend fun remember(userId: String, showtimeId: String, request: HoldRequest?) {}
    suspend fun watch(showtimeId: String, changed: () -> Unit, connection: (Boolean) -> Unit): Closeable = Closeable {}
}
private interface SeatApi {
    @GET("showtimes/{id}/seats") suspend fun seats(@Path("id") id: String): SeatResponse
    @POST("orders") suspend fun hold(@Header("Authorization") auth: String, @Header("Idempotency-Key") key: String, @Body body: HoldBody): HoldResponse
    @GET("orders/{id}") suspend fun order(@Header("Authorization") auth: String, @Path("id") id: String): HoldResponse
    @POST("orders/{id}/cancel") suspend fun cancel(@Header("Authorization") auth: String, @Path("id") id: String): HoldResponse
}
private val Context.holdStore by preferencesDataStore(name = "booking_holds")
class ApiSeatRepository(context: Context, private val account: ApiAccountRepository) : SeatRepository {
    private val store = context.applicationContext.holdStore
    private val json = Json { ignoreUnknownKeys = true }
    private val api = Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL)
        .client(OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).build())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(SeatApi::class.java)
    override suspend fun seats(showtimeId: String): SeatResponse = account.withAuthorization { api.seats(showtimeId) }.also { r ->
        check(r.showtimeId == showtimeId); isoMillis(r.serverTime)
        check(r.items.map { it.id }.distinct().size == r.items.size)
        check(r.items.all { it.seatType in setOf("NORMAL", "VIP", "COUPLE", "GOLD") && it.status in setOf("AVAILABLE", "HELD", "SOLD", "BLOCKED") && it.price.toLong() >= 0 })
        check(r.layout.width.isFinite() && r.layout.height.isFinite() && r.layout.width > 0 && r.layout.height > 0)
        check(r.layout.seats.map { it.seatId }.toSet() == r.items.map { it.id }.toSet() && r.layout.seats.size == r.items.size)
        check(r.layout.seats.all { it.x.isFinite() && it.y.isFinite() && it.x >= 0 && it.y >= 0 && it.x+0.9f <= r.layout.width && it.y+0.9f <= r.layout.height })
    }
    override suspend fun hold(request: HoldRequest) = account.withAuthorization { api.hold(it, request.key, HoldBody(request.showtimeId, request.seatIds)) }
    override suspend fun order(id: String) = account.withAuthorization { api.order(it, id) }
    override suspend fun cancel(id: String) = account.withAuthorization { api.cancel(it, id) }
    override suspend fun saved(userId: String, showtimeId: String): HoldRequest? = store.data.first()[stringPreferencesKey("$userId:$showtimeId")]?.let {
        runCatching { json.decodeFromString<HoldRequest>(it) }.getOrNull()?.takeIf { request -> request.showtimeId == showtimeId }
    }
    override suspend fun remember(userId: String, showtimeId: String, request: HoldRequest?) {
        store.edit { val key=stringPreferencesKey("$userId:$showtimeId"); if(request == null) it.remove(key) else it[key] = json.encodeToString(request) }
    }
    override suspend fun requests(userId: String): List<HoldRequest> = store.data.first().asMap().entries
        .filter { it.key.name.startsWith("$userId:") }.mapNotNull { (_,value) ->
            (value as? String)?.let { runCatching { json.decodeFromString<HoldRequest>(it) }.getOrNull() }
        }
    override suspend fun watch(showtimeId: String, changed: () -> Unit, connection: (Boolean) -> Unit): Closeable {
        val token = account.socketToken()
        val options = IO.Options().apply { auth = mapOf("token" to token); path = "/socket.io/"; reconnection = false }
        val socket = IO.socket(URI(BuildConfig.API_BASE_URL).resolve("/"), options)
        socket.on("connect") { socket.emit("JOIN_SHOWTIME", showtimeId); connection(true); changed() }
        socket.on("disconnect") { connection(false) }
        socket.on("connect_error") { connection(false) }
        listOf("SEAT_HELD", "SEAT_RELEASED", "SEAT_SOLD", "ORDER_EXPIRED", "SHOWTIME_CANCELLED", "PAYMENT_PAID").forEach { event -> socket.on(event) { changed() } }
        socket.connect()
        return Closeable { socket.off(); socket.disconnect(); socket.close() }
    }
}
