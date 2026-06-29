package com.example.steppie.ui.guardian

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
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
                    onOpenRoutineSetCreate = {},
                    onOpenNewRoutineEditor = {},
                    onOpenRoutineEditor = {},
                    onToggleRoutineSetListEditing = {},
                    onSelectRoutineSet = {},
                    onRequestEditRoutineSetName = {},
                    onEditingRoutineSetNameChange = {},
                    onCancelEditRoutineSetName = {},
                    onSaveEditingRoutineSetName = {},
                    onDraftTitleChange = {},
                    onDraftIconChange = {},
                    onDraftColorChange = {},
                    onDraftScheduledTimeChange = {},
                    onSaveDraft = {},
                    onRoutineSetNameChange = {},
                    onRoutineSetStepTitleChange = {},
                    onRoutineSetStepIconChange = {},
                    onRoutineSetStepColorChange = {},
                    onRoutineSetStepScheduledTimeChange = {},
                    onAddRoutineSetStep = {},
                    onEditRoutineSetStep = {},
                    onRemoveRoutineSetStep = {},
                    onSaveRoutineSetDraft = {},
                    onRequestDelete = {},
                    onRequestDeleteRoutineSet = {},
                    onCancelDelete = {},
                    onConfirmDelete = {},
                    onConfirmDeleteRoutineSet = {},
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
                    onOpenRoutineSetCreate = {},
                    onOpenNewRoutineEditor = {},
                    onOpenRoutineEditor = {},
                    onToggleRoutineSetListEditing = {},
                    onSelectRoutineSet = {},
                    onRequestEditRoutineSetName = {},
                    onEditingRoutineSetNameChange = {},
                    onCancelEditRoutineSetName = {},
                    onSaveEditingRoutineSetName = {},
                    onDraftTitleChange = {},
                    onDraftIconChange = {},
                    onDraftColorChange = {},
                    onDraftScheduledTimeChange = {},
                    onSaveDraft = {},
                    onRoutineSetNameChange = {},
                    onRoutineSetStepTitleChange = {},
                    onRoutineSetStepIconChange = {},
                    onRoutineSetStepColorChange = {},
                    onRoutineSetStepScheduledTimeChange = {},
                    onAddRoutineSetStep = {},
                    onEditRoutineSetStep = {},
                    onRemoveRoutineSetStep = {},
                    onSaveRoutineSetDraft = {},
                    onRequestDelete = {},
                    onRequestDeleteRoutineSet = {},
                    onCancelDelete = {},
                    onConfirmDelete = {},
                    onConfirmDeleteRoutineSet = {},
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
    fun homeMenuShowsCreateRoutineSetFirstAndRoutesIt() {
        var createClicks = 0

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
                    onOpenRoutineEdit = {},
                    onOpenSecurity = {},
                    onOpenPinChange = {},
                    onOpenRoutineSetCreate = { createClicks += 1 },
                    onOpenNewRoutineEditor = {},
                    onOpenRoutineEditor = {},
                    onToggleRoutineSetListEditing = {},
                    onSelectRoutineSet = {},
                    onRequestEditRoutineSetName = {},
                    onEditingRoutineSetNameChange = {},
                    onCancelEditRoutineSetName = {},
                    onSaveEditingRoutineSetName = {},
                    onDraftTitleChange = {},
                    onDraftIconChange = {},
                    onDraftColorChange = {},
                    onDraftScheduledTimeChange = {},
                    onSaveDraft = {},
                    onRoutineSetNameChange = {},
                    onRoutineSetStepTitleChange = {},
                    onRoutineSetStepIconChange = {},
                    onRoutineSetStepColorChange = {},
                    onRoutineSetStepScheduledTimeChange = {},
                    onAddRoutineSetStep = {},
                    onEditRoutineSetStep = {},
                    onRemoveRoutineSetStep = {},
                    onSaveRoutineSetDraft = {},
                    onRequestDelete = {},
                    onRequestDeleteRoutineSet = {},
                    onCancelDelete = {},
                    onConfirmDelete = {},
                    onConfirmDeleteRoutineSet = {},
                    onMoveRoutine = { _, _ -> },
                    onShowOutOfScopeNotice = {},
                    onClearNotice = {},
                )
            }
        }

        composeRule.onNodeWithText("루틴 세트 생성").performClick()

        composeRule.runOnIdle {
            assertEquals(1, createClicks)
        }
    }

    @Test
    fun routineEditWithoutActiveSetShowsCreateRoutineSetEmptyState() {
        composeRule.setContent {
            SteppieTheme {
                GuardianModeScreen(
                    state = GuardianModeUiState(
                        isActive = true,
                        isAuthenticated = true,
                        destination = GuardianDestination.RoutineEdit,
                    ),
                    onDigit = {},
                    onDeletePinDigit = {},
                    onCloseToChild = {},
                    onInteraction = {},
                    onOpenHome = {},
                    onOpenRoutineEdit = {},
                    onOpenSecurity = {},
                    onOpenPinChange = {},
                    onOpenRoutineSetCreate = {},
                    onOpenNewRoutineEditor = {},
                    onOpenRoutineEditor = {},
                    onToggleRoutineSetListEditing = {},
                    onSelectRoutineSet = {},
                    onRequestEditRoutineSetName = {},
                    onEditingRoutineSetNameChange = {},
                    onCancelEditRoutineSetName = {},
                    onSaveEditingRoutineSetName = {},
                    onDraftTitleChange = {},
                    onDraftIconChange = {},
                    onDraftColorChange = {},
                    onDraftScheduledTimeChange = {},
                    onSaveDraft = {},
                    onRoutineSetNameChange = {},
                    onRoutineSetStepTitleChange = {},
                    onRoutineSetStepIconChange = {},
                    onRoutineSetStepColorChange = {},
                    onRoutineSetStepScheduledTimeChange = {},
                    onAddRoutineSetStep = {},
                    onEditRoutineSetStep = {},
                    onRemoveRoutineSetStep = {},
                    onSaveRoutineSetDraft = {},
                    onRequestDelete = {},
                    onRequestDeleteRoutineSet = {},
                    onCancelDelete = {},
                    onConfirmDelete = {},
                    onConfirmDeleteRoutineSet = {},
                    onMoveRoutine = { _, _ -> },
                    onShowOutOfScopeNotice = {},
                    onClearNotice = {},
                )
            }
        }

        composeRule.onNodeWithText("아직 루틴 세트가 없어요").assertIsDisplayed()
        composeRule.onAllNodesWithText("루틴 세트 생성").assertCountEquals(2)
    }

    @Test
    fun routineEditShowsAllRoutineSetsAndSelectsOne() {
        var selectedRoutineSetId: String? = null

        composeRule.setContent {
            SteppieTheme {
                GuardianModeScreen(
                    state = GuardianModeUiState(
                        isActive = true,
                        isAuthenticated = true,
                        destination = GuardianDestination.RoutineEdit,
                        routineSets = RoutineSampleData.routineSets,
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
                    onOpenRoutineSetCreate = {},
                    onOpenNewRoutineEditor = {},
                    onOpenRoutineEditor = {},
                    onToggleRoutineSetListEditing = {},
                    onSelectRoutineSet = { selectedRoutineSetId = it },
                    onRequestEditRoutineSetName = {},
                    onEditingRoutineSetNameChange = {},
                    onCancelEditRoutineSetName = {},
                    onSaveEditingRoutineSetName = {},
                    onDraftTitleChange = {},
                    onDraftIconChange = {},
                    onDraftColorChange = {},
                    onDraftScheduledTimeChange = {},
                    onSaveDraft = {},
                    onRoutineSetNameChange = {},
                    onRoutineSetStepTitleChange = {},
                    onRoutineSetStepIconChange = {},
                    onRoutineSetStepColorChange = {},
                    onRoutineSetStepScheduledTimeChange = {},
                    onAddRoutineSetStep = {},
                    onEditRoutineSetStep = {},
                    onRemoveRoutineSetStep = {},
                    onSaveRoutineSetDraft = {},
                    onRequestDelete = {},
                    onRequestDeleteRoutineSet = {},
                    onCancelDelete = {},
                    onConfirmDelete = {},
                    onConfirmDeleteRoutineSet = {},
                    onMoveRoutine = { _, _ -> },
                    onShowOutOfScopeNotice = {},
                    onClearNotice = {},
                )
            }
        }

        composeRule.onNodeWithText("아침 루틴").assertIsDisplayed()
        composeRule.onNodeWithText("학교 루틴").assertIsDisplayed()
        composeRule.onNodeWithText("취침 루틴").assertIsDisplayed()
        composeRule.onNodeWithText("학교 루틴").performClick()

        composeRule.runOnIdle {
            assertEquals(RoutineSampleData.school.id, selectedRoutineSetId)
        }
    }

    @Test
    fun routineEditEditModeShowsRenameAndDeleteActionsForRoutineSets() {
        var editClicks = 0
        var deleteClicks = 0

        composeRule.setContent {
            SteppieTheme {
                GuardianModeScreen(
                    state = GuardianModeUiState(
                        isActive = true,
                        isAuthenticated = true,
                        destination = GuardianDestination.RoutineEdit,
                        routineSets = listOf(RoutineSampleData.morning),
                        activeRoutineSet = RoutineSampleData.morning,
                        routines = RoutineSampleData.morning.routines,
                        routineSetListEditing = true,
                    ),
                    onDigit = {},
                    onDeletePinDigit = {},
                    onCloseToChild = {},
                    onInteraction = {},
                    onOpenHome = {},
                    onOpenRoutineEdit = {},
                    onOpenSecurity = {},
                    onOpenPinChange = {},
                    onOpenRoutineSetCreate = {},
                    onOpenNewRoutineEditor = {},
                    onOpenRoutineEditor = {},
                    onToggleRoutineSetListEditing = {},
                    onSelectRoutineSet = {},
                    onRequestEditRoutineSetName = { editClicks += 1 },
                    onEditingRoutineSetNameChange = {},
                    onCancelEditRoutineSetName = {},
                    onSaveEditingRoutineSetName = {},
                    onDraftTitleChange = {},
                    onDraftIconChange = {},
                    onDraftColorChange = {},
                    onDraftScheduledTimeChange = {},
                    onSaveDraft = {},
                    onRoutineSetNameChange = {},
                    onRoutineSetStepTitleChange = {},
                    onRoutineSetStepIconChange = {},
                    onRoutineSetStepColorChange = {},
                    onRoutineSetStepScheduledTimeChange = {},
                    onAddRoutineSetStep = {},
                    onEditRoutineSetStep = {},
                    onRemoveRoutineSetStep = {},
                    onSaveRoutineSetDraft = {},
                    onRequestDelete = {},
                    onRequestDeleteRoutineSet = { deleteClicks += 1 },
                    onCancelDelete = {},
                    onConfirmDelete = {},
                    onConfirmDeleteRoutineSet = {},
                    onMoveRoutine = { _, _ -> },
                    onShowOutOfScopeNotice = {},
                    onClearNotice = {},
                )
            }
        }

        composeRule.onNodeWithText("✎").performClick()
        composeRule.onNodeWithText("-").performClick()

        composeRule.runOnIdle {
            assertEquals(1, editClicks)
            assertEquals(1, deleteClicks)
        }
    }

    @Test
    fun routineSetCreateDisablesSaveUntilAtLeastOneStepExists() {
        composeRule.setContent {
            SteppieTheme {
                GuardianModeScreen(
                    state = GuardianModeUiState(
                        isActive = true,
                        isAuthenticated = true,
                        destination = GuardianDestination.RoutineSetCreate,
                        routineSetDraft = RoutineSetDraft(name = "주말 루틴"),
                    ),
                    onDigit = {},
                    onDeletePinDigit = {},
                    onCloseToChild = {},
                    onInteraction = {},
                    onOpenHome = {},
                    onOpenRoutineEdit = {},
                    onOpenSecurity = {},
                    onOpenPinChange = {},
                    onOpenRoutineSetCreate = {},
                    onOpenNewRoutineEditor = {},
                    onOpenRoutineEditor = {},
                    onToggleRoutineSetListEditing = {},
                    onSelectRoutineSet = {},
                    onRequestEditRoutineSetName = {},
                    onEditingRoutineSetNameChange = {},
                    onCancelEditRoutineSetName = {},
                    onSaveEditingRoutineSetName = {},
                    onDraftTitleChange = {},
                    onDraftIconChange = {},
                    onDraftColorChange = {},
                    onDraftScheduledTimeChange = {},
                    onSaveDraft = {},
                    onRoutineSetNameChange = {},
                    onRoutineSetStepTitleChange = {},
                    onRoutineSetStepIconChange = {},
                    onRoutineSetStepColorChange = {},
                    onRoutineSetStepScheduledTimeChange = {},
                    onAddRoutineSetStep = {},
                    onEditRoutineSetStep = {},
                    onRemoveRoutineSetStep = {},
                    onSaveRoutineSetDraft = {},
                    onRequestDelete = {},
                    onRequestDeleteRoutineSet = {},
                    onCancelDelete = {},
                    onConfirmDelete = {},
                    onConfirmDeleteRoutineSet = {},
                    onMoveRoutine = { _, _ -> },
                    onShowOutOfScopeNotice = {},
                    onClearNotice = {},
                )
            }
        }

        composeRule.onNodeWithText("저장").assertIsNotEnabled()
        composeRule.onNodeWithText("저장하려면 최소 1개 단계가 필요합니다.").assertIsDisplayed()
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
                    onOpenRoutineSetCreate = {},
                    onOpenNewRoutineEditor = {},
                    onOpenRoutineEditor = {},
                    onToggleRoutineSetListEditing = {},
                    onSelectRoutineSet = {},
                    onRequestEditRoutineSetName = {},
                    onEditingRoutineSetNameChange = {},
                    onCancelEditRoutineSetName = {},
                    onSaveEditingRoutineSetName = {},
                    onDraftTitleChange = {},
                    onDraftIconChange = {},
                    onDraftColorChange = {},
                    onDraftScheduledTimeChange = {},
                    onSaveDraft = {},
                    onRoutineSetNameChange = {},
                    onRoutineSetStepTitleChange = {},
                    onRoutineSetStepIconChange = {},
                    onRoutineSetStepColorChange = {},
                    onRoutineSetStepScheduledTimeChange = {},
                    onAddRoutineSetStep = {},
                    onEditRoutineSetStep = {},
                    onRemoveRoutineSetStep = {},
                    onSaveRoutineSetDraft = {},
                    onRequestDelete = {},
                    onRequestDeleteRoutineSet = {},
                    onCancelDelete = {},
                    onConfirmDelete = {},
                    onConfirmDeleteRoutineSet = {},
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
