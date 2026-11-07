package com.nhom8.cineplex

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.*
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.theme.CineplexMockTheme
import org.junit.*
import org.junit.Assert.*
import java.io.File

class EmailVerificationUiTest {
 @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
 private class Account : AccountRepository {
  var resendCalls = 0
  override fun validate(form: AuthForm,signup:Boolean)=validateAuth(form,signup)
  override suspend fun register(form:AuthForm)=RegistrationResult(form.email,true,"Hãy kiểm tra hộp thư.")
  override suspend fun login(form:AuthForm,staff:Boolean):LoginResult=throw ApiFailure("EMAIL_NOT_VERIFIED","Hãy xác minh email.",email=form.email)
  override suspend fun resend(email:String):String {resendCalls++;return "Hãy kiểm tra hộp thư và thư rác."}
 }
 private fun show(signup:Boolean):Pair<CineplexViewModel,Account> {
  val repo=Account(); lateinit var vm:CineplexViewModel
  compose.runOnUiThread {vm=CineplexViewModel(repo)}
  compose.setContent {CineplexMockTheme {CineplexApp(vm)}}
  compose.runOnIdle {
   if(signup)vm.navigate(Screen.SIGNUP)
   vm.update(AuthForm("Test","fixture@example.test","password-8","password-8",true));vm.submit()
  }
  compose.waitUntil(10000){vm.screen==Screen.VERIFY_EMAIL&&!vm.loading}
  return vm to repo
 }
 private fun checkForm(vm:CineplexViewModel,repo:Account,name:String) {
  compose.onNodeWithTag("required-email-form").assertIsDisplayed()
  compose.onNodeWithText("Vui lòng xác minh email").assertIsDisplayed()
  compose.onNodeWithText("fixture@example.test").assertIsDisplayed()
  compose.onNodeWithText("Gửi lại email xác minh").assertIsDisplayed().performClick()
  compose.waitUntil(10000){!vm.loading}
  compose.waitForIdle()
  compose.runOnIdle {assertEquals(1,repo.resendCalls);assertNull(vm.session);assertTrue(vm.movies.isEmpty())}
  val instrumentation=InstrumentationRegistry.getInstrumentation()
  val image=instrumentation.uiAutomation.takeScreenshot()
  File(instrumentation.targetContext.getExternalFilesDir(null),"email-form-$name.png").outputStream().use {image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
  compose.onNodeWithTag("verification-login").performScrollTo().performClick()
  compose.runOnIdle {assertEquals(Screen.LOGIN,vm.screen);assertEquals("",vm.form.password)}
 }
 @Test fun registrationShowsRequiredVerificationForm() {val(vm,repo)=show(true);checkForm(vm,repo,"signup")}
 @Test fun unverifiedLoginShowsSameRequiredVerificationForm() {val(vm,repo)=show(false);checkForm(vm,repo,"login")}
}
