package com.example.steppie.ui.guardian

import android.view.HapticFeedbackConstants
import android.widget.FrameLayout
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.example.steppie.ui.theme.SteppieTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GuardianNumberKeypadTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun touchAndAccessibilityRequestOneHapticBeforeActionAndRespectSettingChanges() {
        val enabled = mutableStateOf(true)
        val events = mutableListOf<String>()
        composeRule.setContent {
            val context = LocalContext.current
            val view = object : FrameLayout(context) {
                override fun performHapticFeedback(feedbackConstant: Int): Boolean {
                    assertEquals(HapticFeedbackConstants.VIRTUAL_KEY, feedbackConstant)
                    events += "haptic"
                    // Unsupported hardware must not prevent the input action.
                    return false
                }
            }
            CompositionLocalProvider(LocalView provides view) {
                SteppieTheme {
                    GuardianNumberKeypad(
                        onDigit = { events += "digit:$it" },
                        onDelete = { events += "delete" },
                        hapticEnabled = enabled.value,
                        deleteDescription = "delete",
                    )
                }
            }
        }
        composeRule.onNodeWithContentDescription("1").performTouchInput { click() }
        composeRule.onNodeWithContentDescription("0").performClick()
        composeRule.onNodeWithContentDescription("delete").performClick()
        composeRule.runOnIdle {
            assertEquals(listOf("haptic", "digit:1", "haptic", "digit:0", "haptic", "delete"), events)
            events.clear()
            enabled.value = false
        }
        composeRule.onNodeWithContentDescription("2").performClick()
        composeRule.onNodeWithContentDescription("delete").performTouchInput { click() }
        composeRule.runOnIdle {
            assertEquals(listOf("digit:2", "delete"), events)
        }
    }
}
