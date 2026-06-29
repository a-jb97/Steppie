package com.example.steppie.ui.guardian

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.steppie.data.sample.RoutineSampleData
import com.example.steppie.ui.theme.SteppieTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GuardianModeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pinScreenRequestsDigitsAndDelete() {
        val digits = mutableListOf<Int>()
        var deleted = 0

        composeRule.setContent {
            SteppieTheme {
                GuardianModeScreen(
                    state = GuardianModeUiState(isActive = true, destination = GuardianDestination.Pin),
                    onDigit = { digits += it },
                    onDeletePinDigit = { deleted += 1 },
                    onCloseToChild = {},
                    onInteraction = {},
                    onOpenHome = {},
                    onOpenRoutineEdit = {},
                    onOpenSecurity = {},
                    onOpenPinChange = {},
                    onOpenNewRoutineEditor = {},
                    onOpenRoutineEditor = {},
                    onDraftTitleChange = {},
                    onDraftIconChange = {},
                    onDraftColorChange = {},
                    onDraftScheduledTimeChange = {},
                    onSaveDraft = {},
                    onRequestDelete = {},
                    onCancelDelete = {},
                    onConfirmDelete = {},
                    onMoveRoutine = { _, _ -> },
                    onShowOutOfScopeNotice = {},
                    onClearNotice = {},
                )
            }
        }

        composeRule.onNodeWithTag("guardian_pin").assertIsDisplayed()
        composeRule.onNodeWithText("1").performClick()
        composeRule.onNodeWithText("⌫").performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(1), digits)
            assertEquals(1, deleted)
        }
    }

    @Test
    fun homeMenuRoutesRoutineAndSecurityCards() {
        var routineClicks = 0
        var securityClicks = 0

        composeRule.setContent {
            SteppieTheme {
                GuardianModeScreen(
                    state = GuardianModeUiState(
                        isActive = true,
                        isAuthenticated = true,
                        destination = GuardianDestination.Home,
                    ),
                    onDigit = {},
                    onDeletePinDigit = {},
                    onCloseToChild = {},
                    onInteraction = {},
                    onOpenHome = {},
                    onOpenRoutineEdit = { routineClicks += 1 },
                    onOpenSecurity = { securityClicks += 1 },
                    onOpenPinChange = {},
                    onOpenNewRoutineEditor = {},
                    onOpenRoutineEditor = {},
                    onDraftTitleChange = {},
                    onDraftIconChange = {},
                    onDraftColorChange = {},
                    onDraftScheduledTimeChange = {},
                    onSaveDraft = {},
                    onRequestDelete = {},
                    onCancelDelete = {},
                    onConfirmDelete = {},
                    onMoveRoutine = { _, _ -> },
                    onShowOutOfScopeNotice = {},
                    onClearNotice = {},
                )
            }
        }

        composeRule.onNodeWithText("루틴 관리").performClick()
        composeRule.onNodeWithText("보안").performClick()

        composeRule.runOnIdle {
            assertEquals(1, routineClicks)
            assertEquals(1, securityClicks)
        }
    }

    @Test
    fun routineEditUsesDragHandleInsteadOfArrowButtons() {
        composeRule.setContent {
            SteppieTheme {
                GuardianModeScreen(
                    state = GuardianModeUiState(
                        isActive = true,
                        isAuthenticated = true,
                        destination = GuardianDestination.RoutineEdit,
                        activeRoutineSet = RoutineSampleData.morning,
                        routines = RoutineSampleData.morning.routines,
                    ),
                    onDigit = {},
                    onDeletePinDigit = {},
                    onCloseToChild = {},
                    onInteraction = {},
                    onOpenHome = {},
                    onOpenRoutineEdit = {},
                    onOpenSecurity = {},
                    onOpenPinChange = {},
                    onOpenNewRoutineEditor = {},
                    onOpenRoutineEditor = {},
                    onDraftTitleChange = {},
                    onDraftIconChange = {},
                    onDraftColorChange = {},
                    onDraftScheduledTimeChange = {},
                    onSaveDraft = {},
                    onRequestDelete = {},
                    onCancelDelete = {},
                    onConfirmDelete = {},
                    onMoveRoutine = { _, _ -> },
                    onShowOutOfScopeNotice = {},
                    onClearNotice = {},
                )
            }
        }

        composeRule.onAllNodesWithText("↑").assertCountEquals(0)
        composeRule.onAllNodesWithText("↓").assertCountEquals(0)
        composeRule.onAllNodesWithText("×").assertCountEquals(0)
        composeRule.onNodeWithTag(
            "routine_drag_${RoutineSampleData.morning.routines.first().id}",
            useUnmergedTree = true,
        ).assertIsDisplayed()
    }
}
