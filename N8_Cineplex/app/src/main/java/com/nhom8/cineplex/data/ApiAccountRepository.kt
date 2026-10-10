package com.nhom8.cineplex.data

import android.content.Context
import com.nhom8.cineplex.BuildConfig
import com.nhom8.cineplex.model.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

@Serializable data class UserProfile(val id: String, val email: String, val fullName: String, val role: String, val avatarUrl: String? = null, val emailVerifiedAt: String? = null, val hasPassword: Boolean) {
    fun session() = Session(fullName, Role.valueOf(role), email, id, avatarUrl, emailVerifiedAt, hasPassword)
}
@Serializable data class TokenResponse(val accessToken: String, val refreshToken: String? = null, val expiresIn: Int, val user: UserProfile? = null) {
    override fun toString() = "TokenResponse(<redacted>)"
}
@Serializable data class MessageResponse(val message: String, val userId: String? = null, val emailSent: Boolean? = null)
interface AccountApi {
    @POST("auth/register") suspend fun register(@Body body: JsonObject): MessageResponse
    @POST("auth/login") suspend fun login(@Body body: JsonObject): TokenResponse
    @POST("auth/google") suspend fun google(@Body body: JsonObject): TokenResponse
    @POST("auth/refresh") suspend fun refresh(@Body body: JsonObject): TokenResponse
    @POST("auth/logout") suspend fun logout(@Body body: JsonObject): MessageResponse
    @POST("auth/forgot-password") suspend fun forgot(@Body body: JsonObject): MessageResponse
    @POST("auth/resend-verification") suspend fun resend(@Body body: JsonObject): MessageResponse
    @GET("me") suspend fun me(@Header("Authorization") auth: String): UserProfile
    @PATCH("me") suspend fun edit(@Header("Authorization") auth: String, @Body body: JsonObject): UserProfile
    @POST("me/change-email") suspend fun email(@Header("Authorization") auth: String, @Body body: JsonObject): MessageResponse
    @POST("auth/change-password") suspend fun password(@Header("Authorization") auth: String, @Body body: JsonObject): MessageResponse
    @Multipart @POST("me/avatar") suspend fun avatar(@Header("Authorization") auth: String, @Part file: MultipartBody.Part): UserProfile
    @DELETE("me/avatar") suspend fun deleteAvatar(@Header("Authorization") auth: String): UserProfile
}

class ApiAccountRepository(context: Context) : AccountRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val vault = TokenVault(context)
    private val mutex = Mutex()
    private var access: String? = null
    private var generation = 0
    private val api = Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL)
        .client(OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).build())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(AccountApi::class.java)
    private fun body(vararg values: Pair<String, String>) = buildJsonObject { values.forEach { (k, v) -> put(k, v) } }
    override fun validate(form: AuthForm, signup: Boolean) = validateAuth(form, signup)
    private suspend fun <T> call(block: suspend () -> T): T {
        try { return block() } catch (e: HttpException) {
            val obj = runCatching { json.parseToJsonElement(e.response()?.errorBody()?.string() ?: "{}").jsonObject }.getOrNull()
            val fields = obj?.get("fieldErrors")?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap()
            throw ApiFailure(obj?.get("code")?.jsonPrimitive?.content ?: "HTTP_${e.code()}", obj?.get("message")?.let { if (it is JsonPrimitive) it.content else "Dữ liệu không hợp lệ." } ?: "Không thực hiện được yêu cầu (${e.code()}).", fields)
        } catch (e: java.io.IOException) { throw ApiFailure("NETWORK_ERROR", "Không kết nối được máy chủ. Hãy kiểm tra mạng rồi thử lại.") }
    }
    private suspend fun establish(response: TokenResponse, expectedGeneration: Int): Session = mutex.withLock {
        if (expectedGeneration != generation) throw ApiFailure("SESSION_CANCELED", "Yêu cầu đã bị hủy.")
        val refresh = response.refreshToken ?: throw ApiFailure("INVALID_RESPONSE", "Máy chủ trả phiên không hợp lệ.")
        val user = response.user ?: throw ApiFailure("INVALID_RESPONSE", "Máy chủ không trả thông tin tài khoản.")
        vault.write(refresh); access = response.accessToken; user.session()
    }
    private suspend fun refreshLocked(expectedGeneration: Int) {
        val refresh = vault.read() ?: throw ApiFailure("SESSION_REVOKED", "Vui lòng đăng nhập lại.")
        try {
            val r = api.refresh(body("refreshToken" to refresh))
            if (expectedGeneration != generation) throw ApiFailure("SESSION_CANCELED", "Phiên đã kết thúc.")
            access = r.accessToken
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) { access = null; vault.write(null) }
            throw e
        }
    }
    private suspend fun <T> authorized(block: suspend (String) -> T): T = call {
        val expectedGeneration = generation
        val used = mutex.withLock { if (access == null) refreshLocked(expectedGeneration); access!! }
        try { block("Bearer $used") } catch (e: HttpException) {
            if (e.code() != 401) throw e
            val renewed = mutex.withLock { if (expectedGeneration != generation) throw ApiFailure("SESSION_CANCELED", "Phiên đã kết thúc."); if (access == used) refreshLocked(expectedGeneration); access!! }
            block("Bearer $renewed")
        }
    }
    override suspend fun register(form: AuthForm): String = call { api.register(buildJsonObject { put("fullName", form.name.trim()); put("email", normalizeEmail(form.email)); put("password", form.password); put("consent", form.consent) }).message }
    override suspend fun login(form: AuthForm, staff: Boolean): LoginResult = call {
        val expected = generation
        LoginResult.Success(establish(api.login(body("email" to normalizeEmail(form.email), "password" to form.password, "portal" to if (staff) "STAFF" else "CUSTOMER")), expected))
    }
    override suspend fun google(idToken: String): Session = call { val expected = generation; establish(api.google(body("idToken" to idToken)), expected) }
    override suspend fun restore(): Session? { if (vault.read() == null) return null; return me() }
    override suspend fun me(): Session = authorized { api.me(it).session() }
    override suspend fun edit(name: String): Session = authorized { api.edit(it, body("fullName" to name.trim())).session() }
    override suspend fun forgot(email: String): String = call { api.forgot(body("email" to normalizeEmail(email))).message }
    override suspend fun resend(email: String): String = call { api.resend(body("email" to normalizeEmail(email))).message }
    override suspend fun changePassword(current: String, replacement: String): String = authorized { api.password(it, body("currentPassword" to current, "newPassword" to replacement)).message }
    override suspend fun changeEmail(email: String, password: String?, idToken: String?): String = authorized {
        api.email(it, buildJsonObject { put("newEmail", normalizeEmail(email)); password?.let { p -> put("currentPassword", p) }; idToken?.let { t -> put("idToken", t) } }).message
    }
    override suspend fun avatar(bytes: ByteArray, mime: String): Session = authorized { api.avatar(it, MultipartBody.Part.createFormData("file", "avatar", bytes.toRequestBody(mime.toMediaType()))).session() }
    override suspend fun deleteAvatar(): Session = authorized { api.deleteAvatar(it).session() }
    override suspend fun logout() {
        val refresh = mutex.withLock { generation++; val old = vault.read(); access = null; vault.write(null); old }
        if (refresh != null) call { api.logout(body("refreshToken" to refresh)); Unit }
    }
}
