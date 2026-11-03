package com.nhom8.cineplex
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.screens.AuthScreen
import com.nhom8.cineplex.ui.theme.CineplexMockTheme
import org.junit.Rule
import org.junit.Test
class CompactMobileUiTest {
 @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
 @Test fun compactLoginKeepsReadableFieldsAndTouchTargets() {
  compose.setContent { CineplexMockTheme { AuthScreen(CineplexViewModel()) } }
  compose.onNodeWithText("Hẹn bạn ở rạp").assertIsDisplayed()
  compose.onNodeWithContentDescription("Email").assertHeightIsAtLeast(48.dp)
  compose.onNodeWithText("Đăng nhập bằng Google").performScrollTo().assertIsDisplayed()
 }
}
