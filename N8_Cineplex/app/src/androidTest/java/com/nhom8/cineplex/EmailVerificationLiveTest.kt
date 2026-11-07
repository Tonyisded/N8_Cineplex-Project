package com.nhom8.cineplex
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.*
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.theme.CineplexMockTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
class EmailVerificationLiveTest {
 @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
 @Test fun realGmailVerificationRoundTrip() {
  Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("liveEmailGate")=="true")
  val email=requireNotNull(InstrumentationRegistry.getArguments().getString("liveEmailGateEmail")) { "Pass liveEmailGateEmail for the authorized test mailbox" };val password=java.util.UUID.randomUUID().toString()
  val repo=ApiAccountRepository(InstrumentationRegistry.getInstrumentation().targetContext)
  runBlocking {runCatching {repo.logout()}}
  lateinit var vm:CineplexViewModel
  compose.runOnUiThread {vm=CineplexViewModel(repo,ApiCatalogRepository())}
  compose.setContent {CineplexMockTheme {CineplexApp(vm)}}
  compose.waitUntil(30000){!vm.loading}
  compose.runOnIdle {vm.navigate(Screen.SIGNUP);vm.update(AuthForm("Email verification test",email,password,password,true));vm.submit()}
  compose.waitUntil(60000){vm.screen==Screen.VERIFY_EMAIL&&!vm.loading}
  assertNull(vm.session);assertTrue(vm.movies.isEmpty())
  compose.runOnIdle {vm.verificationLogin();vm.update(vm.form.copy(password=password));vm.submit()}
  compose.waitUntil(30000){vm.screen==Screen.VERIFY_EMAIL&&!vm.loading}
  assertNull(vm.session)
  compose.runOnIdle {vm.resend()};compose.waitUntil(60000){!vm.loading}
  android.util.Log.i("EmailGateTest","WAITING_FOR_GMAIL_CONFIRMATION registration+unverified-login+resend observed")
  val deadline=System.currentTimeMillis()+900000;var verified=false
  while(System.currentTimeMillis()<deadline&&!verified){Thread.sleep(20000);verified=runBlocking {try {val result=repo.login(AuthForm(email=email,password=password),false);result is LoginResult.Success&&result.session.emailVerifiedAt!=null}catch(e:ApiFailure){assertTrue(e.code in listOf("EMAIL_NOT_VERIFIED","HTTP_429","NETWORK_ERROR"));false}}}
  assertTrue("Gmail confirmation was not observed before deadline",verified)
  InstrumentationRegistry.getInstrumentation().runOnMainSync {vm.verificationLogin();vm.update(vm.form.copy(password=password));vm.submit()}
  val end=System.currentTimeMillis()+30000;while(System.currentTimeMillis()<end&&vm.screen!=Screen.HOME)Thread.sleep(200)
  assertEquals(Screen.HOME,vm.screen);assertNotNull(vm.session?.emailVerifiedAt)
  android.util.Log.i("EmailGateTest","LIVE_GMAIL_VERIFIED_AND_HOME no payment operations")
 }
}
