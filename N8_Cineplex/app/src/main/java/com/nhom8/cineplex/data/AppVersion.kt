package com.nhom8.cineplex.data

import com.nhom8.cineplex.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import java.net.URI
import java.util.concurrent.TimeUnit

object AppVersionWarning { val required = MutableStateFlow(false) }
fun cineplexHttpClient(): OkHttpClient = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS)
    .addInterceptor { chain ->
        val response = chain.proceed(chain.request().newBuilder().header("X-App-Version-Code", BuildConfig.VERSION_CODE.toString()).build())
        if(response.code == 426) AppVersionWarning.required.value = true
        response
    }.build()
@Serializable data class AppRelease(val minimumVersionCode: Int, val latestVersionCode: Int, val downloadUrl: String, val sha256: String = "", val message: String = "")
fun validateAppRelease(release: AppRelease, baseUrl: String = BuildConfig.API_BASE_URL) {
    check(release.minimumVersionCode >= 0 && release.latestVersionCode >= maxOf(1, release.minimumVersionCode))
    val base=URI(baseUrl);val url=URI(release.downloadUrl)
    check(url.scheme=="https" && url.userInfo==null && url.host==base.host && url.port==base.port && url.path==base.path.trimEnd('/')+"/app/android/download" && url.query==null && url.fragment==null)
    check(release.minimumVersionCode==0 || release.sha256.matches(Regex("[a-f0-9]{64}")))
}
fun interface VersionBackend { suspend fun release(): AppRelease }
private interface VersionApi { @GET("app/version") suspend fun release(): AppRelease }
class ApiVersionRepository: VersionBackend {
    private val api=Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).client(cineplexHttpClient())
        .addConverterFactory(Json { ignoreUnknownKeys=true }.asConverterFactory("application/json".toMediaType())).build().create(VersionApi::class.java)
    override suspend fun release()=api.release().also { validateAppRelease(it) }
}
