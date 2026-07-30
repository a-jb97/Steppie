package com.example.steppie.ui.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.steppie.data.sample.RoutineSampleData
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.FeedbackIntensity
import com.example.steppie.ui.tutorial.TutorialScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppEffectsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun notificationReconcileRunsInitiallyAndOncePerEffectiveInputChange() {
        val requests = mutableListOf<NotificationReconcileInput>()
        var input by mutableStateOf(notificationInput())
        var unrelatedRecomposition by mutableIntStateOf(0)

        composeRule.setContent {
            unrelatedRecomposition
            NotificationReconcileEffect(
                input = input,
                reconcile = requests::add,
            )
        }

        composeRule.runOnIdle {
            assertEquals(listOf(input), requests)
            unrelatedRecomposition += 1
        }
        composeRule.runOnIdle {
            assertEquals(1, requests.size)
            input = input.copy(completedRoutineIds = setOf(input.routines.first().id))
        }
        composeRule.runOnIdle {
            assertEquals(2, requests.size)
            input = input.copy(permissionRefresh = input.permissionRefresh + 1)
        }
        composeRule.runOnIdle {
            assertEquals(3, requests.size)
            input = input.copy(
                settings = input.settings.copy(feedbackIntensity = FeedbackIntensity.Strong),
            )
        }
        composeRule.runOnIdle {
            assertEquals(4, requests.size)
            input = input.copy(routines = input.routines.dropLast(1))
        }
        composeRule.runOnIdle {
            assertEquals(5, requests.size)
            assertEquals(input, requests.last())
        }
    }

    @Test
    fun tutorialResolutionClearsAnchorsBeforeShowingOnlyWhenScreenChanges() {
        val calls = mutableListOf<String>()
        var screen by mutableStateOf<TutorialScreen?>(null)
        var unrelatedRecomposition by mutableIntStateOf(0)

        composeRule.setContent {
            unrelatedRecomposition
            TutorialResolutionEffect(
                screen = screen,
                clearAnchors = { calls += "clear" },
                showFor = { calls += "show:$it" },
            )
        }

        composeRule.runOnIdle {
            assertEquals(listOf("clear", "show:null"), calls)
            unrelatedRecomposition += 1
        }
        composeRule.runOnIdle {
            assertEquals(2, calls.size)
            screen = TutorialScreen.ChildFocus
        }
        composeRule.runOnIdle {
            assertEquals(
                listOf("clear", "show:null", "clear", "show:ChildFocus"),
                calls,
            )
            unrelatedRecomposition += 1
        }
        composeRule.runOnIdle {
            assertEquals(4, calls.size)
            screen = TutorialScreen.GuardianHome
        }
        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    "clear",
                    "show:null",
                    "clear",
                    "show:ChildFocus",
                    "clear",
                    "show:GuardianHome",
                ),
                calls,
            )
        }
    }

    private fun notificationInput(): NotificationReconcileInput =
        NotificationReconcileInput(
            routines = RoutineSampleData.morning.routines,
            completedRoutineIds = emptySet(),
            settings = AppSettings(),
            permissionRefresh = 0,
        )
}
