package com.nhom8.cineplex
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.ui.AppVersionState
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.*
import org.junit.Assert.*
@OptIn(ExperimentalCoroutinesApi::class)
class AppVersionTest {
 @Before fun before(){Dispatchers.setMain(StandardTestDispatcher());AppVersionWarning.required.value=false}
 @After fun after(){Dispatchers.resetMain();AppVersionWarning.required.value=false}
 @Test fun oldApplicationRequiresUpdateButCompatibleApplicationRemainsUsable()=runTest {
  val r=AppRelease(8,8,"https://18.143.100.43/api/v1/app/android/download","a".repeat(64))
  val state=AppVersionState(VersionBackend{r},7);state.check();advanceUntilIdle();assertTrue(state.outdated)
  val ready=AppVersionState(VersionBackend{r},8);ready.check();advanceUntilIdle();assertFalse(ready.outdated)
 }
 @Test fun invalidOrForeignUpdateLinksAreRejected() {
  for(url in listOf("http://18.143.100.43/api/v1/app/android/download","https://evil.invalid/api/v1/app/android/download","https://user:pass@18.143.100.43/api/v1/app/android/download")) {
   assertThrows(IllegalStateException::class.java){validateAppRelease(AppRelease(7,7,url,"a".repeat(64)))}
  }
 }
 @Test fun everyApiClientAddsActualVersionAnd426RaisesUpdateSignal() {
  var header:String?=null
  val client=cineplexHttpClient().newBuilder().addInterceptor{ chain -> header=chain.request().header("X-App-Version-Code");Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(426).message("Upgrade Required").body("{}".toResponseBody()).build() }.build()
  client.newCall(Request.Builder().url("https://18.143.100.43/api/v1/me").build()).execute().use {assertEquals(426,it.code)}
  assertEquals(BuildConfig.VERSION_CODE.toString(),header);assertTrue(AppVersionWarning.required.value)
 }
 @Test fun initialVersionCheckNetworkFailureDoesNotPretendCompatibility()=runTest {
  val state=AppVersionState(VersionBackend{throw java.io.IOException("offline")});state.check();advanceUntilIdle();assertNull(state.release);assertTrue(state.error.isNotBlank());assertFalse(state.busy)
 }
}
