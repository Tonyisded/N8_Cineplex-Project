package com.nhom8.cineplex
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.nhom8.cineplex.ui.components.CustomerNavigation
import com.nhom8.cineplex.ui.theme.CineplexMockTheme
import org.junit.*
import org.junit.Assert.*
class VoucherNavigationUiTest {
 @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
 @Test fun fiveDestinationsHaveWorkingVoucherAndRetainTicketsAndAccount() {
  var destination=-1
  compose.setContent {CineplexMockTheme {var selected by remember{mutableIntStateOf(0)};CustomerNavigation(selected,{selected=0;destination=0},{selected=4;destination=4},{},{selected=3;destination=3},{selected=2;destination=2})}}
  for(label in listOf("Trang chủ","Khám phá","Voucher","Vé của tôi","Tài khoản"))compose.onNodeWithText(label).assertIsDisplayed().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
  compose.onNodeWithText("Voucher").performClick();assertEquals(2,destination);compose.onNodeWithText("Voucher",useUnmergedTree=false).assertIsSelected()
  compose.waitForIdle();android.os.SystemClock.sleep(500)
  val instrumentation=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();val file=java.io.File(instrumentation.targetContext.getExternalFilesDir(null),"visual_checks/voucher-navigation.png");file.parentFile!!.mkdirs();file.outputStream().use {instrumentation.uiAutomation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
  compose.onNodeWithText("Vé của tôi").performClick();assertEquals(3,destination)
  compose.onNodeWithText("Tài khoản").performClick();assertEquals(4,destination)
 }
}
