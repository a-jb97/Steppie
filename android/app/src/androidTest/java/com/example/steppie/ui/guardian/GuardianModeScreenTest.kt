package com.example.steppie.ui.guardian

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.steppie.R
import com.example.steppie.data.sample.RoutineSampleData
import com.example.steppie.testing.testString
import com.example.steppie.testing.testText
import com.example.steppie.ui.theme.SteppieTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@Composable
private fun TestGuardianModeScreen(
    state: GuardianModeUiState,
    modeActions: GuardianModeActions = GuardianModeActionCallbacks(),
    routineActions: GuardianRoutineActions = GuardianRoutineActionCallbacks(),
    routineSetActions: GuardianRoutineSetActions = GuardianRoutineSetActionCallbacks(),
    templateActions: GuardianTemplateActions = GuardianTemplateActionCallbacks(),
    recordActions: GuardianRecordActions = GuardianRecordActionCallbacks(),
    environmentActions: GuardianEnvironmentActions = GuardianEnvironmentActionCallbacks(),
    securityActions: GuardianSecurityActions = GuardianSecurityActionCallbacks(),
) {
    GuardianModeScreen(
        state = state,
        modeActions = modeActions,
        routineActions = routineActions,
        routineSetActions = routineSetActions,
        templateActions = templateActions,
        recordActions = recordActions,
        environmentActions = environmentActions,
        securityActions = securityActions,
    )
}

private fun guardianMenuDescription(titleResourceId: Int, bodyResourceId: Int): String =
    testString(
        R.string.a11y_guardian_menu_card,
        testString(titleResourceId),
        testString(bodyResourceId),
    )

class GuardianModeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pinScreenRequestsDigitsAndDelete() {
        val digits = mutableListOf<Int>()
        var deleted = 0

        composeRule.setContent {
            SteppieTheme {
                TestGuardianModeScreen(
                    state = GuardianModeUiState(isActive = true, destination = GuardianDestination.Pin),
                    modeActions = GuardianModeActionCallbacks(
                        onDigit = { digits += it },
                        onDeletePinDigit = { deleted += 1 },
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("guardian_pin").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("1").performClick()
        composeRule.onNodeWithContentDescription(testString(R.string.a11y_pin_delete)).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(1), digits)
            assertEquals(1, deleted)
        }
    }

    @Test
    fun homeMenuRoutesRoutineAndSecurityCards() {
        var routineClicks = 0
        var environmentClicks = 0
        var recordsClicks = 0
        var securityClicks = 0

        composeRule.setContent {
            SteppieTheme {
                TestGuardianModeScreen(
                    state = GuardianModeUiState(
                        isActive = true,
                        isAuthenticated = true,
                        destination = GuardianDestination.Home,
                    ),
                    routineActions = GuardianRoutineActionCallbacks(
                        onOpenRoutineEdit = { routineClicks += 1 },
                    ),
                    environmentActions = GuardianEnvironmentActionCallbacks(
                        onOpenEnvironmentSettings = { environmentClicks += 1 },
                    ),
                    recordActions = GuardianRecordActionCallbacks(
                        onOpenRecords = { recordsClicks += 1 },
                    ),
                    securityActions = GuardianSecurityActionCallbacks(
                        onOpenSecurity = { securityClicks += 1 },
                    ),
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            guardianMenuDescription(R.string.guardian_menu_routine, R.string.guardian_menu_routine_desc),
        ).performScrollTo().performClick()
        composeRule.onNodeWithContentDescription(
            guardianMenuDescription(R.string.guardian_menu_feedback, R.string.guardian_menu_feedback_desc),
        ).performScrollTo().performClick()
        composeRule.onNodeWithContentDescription(
            guardianMenuDescription(R.string.guardian_menu_records, R.string.guardian_menu_records_desc),
        ).performScrollTo().performClick()
        composeRule.onNodeWithContentDescription(
            guardianMenuDescription(R.string.guardian_menu_security, R.string.guardian_menu_security_desc),
        ).performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(1, routineClicks)
            assertEquals(1, environmentClicks)
            assertEquals(1, recordsClicks)
            assertEquals(1, securityClicks)
        }
    }

    @Test
    fun securityScreenRoutesRecoveryCodeCard() {
        var recoveryClicks = 0

        composeRule.setContent {
            SteppieTheme {
                TestGuardianModeScreen(
                    state = GuardianModeUiState(
                        isActive = true,
                        isAuthenticated = true,
                        destination = GuardianDestination.Security,
                    ),
                    securityActions = GuardianSecurityActionCallbacks(
                        onOpenRecoveryCode = { recoveryClicks += 1 },
                    ),
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            guardianMenuDescription(R.string.guardian_recovery_code, R.string.guardian_recovery_code_desc),
        ).performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(1, recoveryClicks)
        }
    }

    @Test
    fun recoveryCodeScreenShowsOneTimeCodeAndAcknowledges() {
        var closeClicks = 0

        composeRule.setContent {
            SteppieTheme {
                TestGuardianModeScreen(
                    state = GuardianModeUiState(
                        isActive = true,
                        isAuthenticated = true,
                        destination = GuardianDestination.RecoveryCode,
                        recoveryStep = GuardianRecoveryStep.ShowCode,
                        recoveryCodeToShow = "654321",
                    ),
                    securityActions = GuardianSecurityActionCallbacks(
                        onCloseRecoveryCode = { closeClicks += 1 },
                    ),
                )
            }
        }

        composeRule.onNodeWithText(testString(R.string.guardian_recovery_show_title)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            testString(R.string.a11y_recovery_code_value, "654321"),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(testString(R.string.guardian_recovery_acknowledge)).performClick()

        composeRule.runOnIdle {
            assertEquals(1, closeClicks)
        }
    }

    @Test
    fun recordsScreenShowsSummaryAndRoutineStatus() {
        val date = LocalDate.parse("2026-06-30")
        val selectedDay = GuardianRecordDay(
            date = date,
            completedCount = 2,
            totalCount = 3,
            hasRecords = true,
        )

        composeRule.setContent {
            SteppieTheme {
                TestGuardianModeScreen(
                    state = GuardianModeUiState(
                        isActive = true,
                        isAuthenticated = true,
                        destination = GuardianDestination.Records,
                        selectedRecordsDate = date,
                        recordDays = listOf(selectedDay),
                        selectedRecordSummary = selectedDay,
                        selectedRecordRoutines = listOf(
                            GuardianRecordRoutine(
                                routineId = "40000000-0000-4000-8000-000000000001",
                                title = "양치하기",
                                isCompleted = true,
                                completedAt = null,
                                isDeleted = false,
                                isInactive = false,
                                isMissing = false,
                            ),
                            GuardianRecordRoutine(
                                routineId = "40000000-0000-4000-8000-000000000002",
                                title = "가방 챙기기",
                                isCompleted = false,
                                completedAt = null,
                                isDeleted = true,
                                isInactive = false,
                                isMissing = false,
                            ),
                        ),
                    ),
                )
            }
        }

        composeRule.onNodeWithText(testString(R.string.guardian_records_title)).assertIsDisplayed()
        composeRule.onNodeWithText(testString(R.string.guardian_records_summary, 2, 3, 66)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            testString(
                R.string.a11y_guardian_record_routine,
                "양치하기",
                testString(R.string.guardian_records_completed),
            ),
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            testString(
                R.string.a11y_guardian_record_routine_with_lifecycle,
                "가방 챙기기",
                testString(R.string.guardian_records_not_completed),
                testString(R.string.guardian_records_deleted_routine),
            ),
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun recordsScreenRoutesCalendarButton() {
        var calendarClicks = 0

        composeRule.setContent {
            SteppieTheme {
                TestGuardianModeScreen(
                    state = GuardianModeUiState(
                        isActive = true,
                        isAuthenticated = true,
                        destination = GuardianDestination.Records,
                    ),
                    recordActions = GuardianRecordActionCallbacks(
                        onOpenRecordsCalendar = { calendarClicks += 1 },
                    ),
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            testString(R.string.a11y_guardian_records_calendar),
        ).performClick()

        composeRule.runOnIdle {
            assertEquals(1, calendarClicks)
        }
    }

    @Test
    fun recordsCalendarSelectsOnlyDatesWithRecords() {
        val dateWithRecords = LocalDate.parse("2026-06-30")
        val emptyDate = LocalDate.parse("2026-06-29")
        var selectedDate: LocalDate? = null

        composeRule.setContent {
            SteppieTheme {
                TestGuardianModeScreen(
                    state = GuardianModeUiState(
                        isActive = true,
                        isAuthenticated = true,
                        destination = GuardianDestination.RecordsCalendar,
                        recordsCalendarMonth = java.time.YearMonth.of(2026, 6),
                        calendarRecordDates = setOf(dateWithRecords),
                        selectedCalendarRecordsDate = dateWithRecords,
                        selectedCalendarRecordSummary = GuardianRecordDay(dateWithRecords, 1, 1, true),
                        selectedCalendarRecordRoutines = listOf(
                            GuardianRecordRoutine(
                                routineId = "40000000-0000-4000-8000-000000000001",
                                title = "양치하기",
                                isCompleted = true,
                                completedAt = null,
                                isDeleted = false,
                                isInactive = false,
                                isMissing = false,
                            ),
                        ),
                    ),
                    recordActions = GuardianRecordActionCallbacks(
                        onSelectRecordsCalendarDate = { selectedDate = it },
                    ),
                )
            }
        }

        composeRule.onNodeWithText(testString(R.string.guardian_records_calendar_title)).assertIsDisplayed()
        composeRule.onNodeWithTag("guardian_records_calendar_date_$dateWithRecords").performClick()
        composeRule.onNodeWithTag("guardian_records_calendar_date_$emptyDate")
            .assertIsDisplayed()
            .assertHasNoClickAction()
        composeRule.onNodeWithContentDescription(
            testString(
                R.string.a11y_guardian_record_routine,
                "양치하기",
                testString(R.string.guardian_records_completed),
            ),
        ).performScrollTo().assertIsDisplayed()

        composeRule.runOnIdle {
            assertEquals(dateWithRecords, selectedDate)
        }
    }

    @Test
    fun environmentSettingsShowsFigmaAndSprintControls() {
        composeRule.setContent {
            SteppieTheme {
                TestGuardianModeScreen(
                    state = GuardianModeUiState(
                        isActive = true,
                        isAuthenticated = true,
                        destination = GuardianDestination.EnvironmentSettings,
                    ),
                )
            }
        }

        composeRule.onNodeWithText(testString(R.string.guardian_setting_feedback_intensity)).assertIsDisplayed()
        composeRule.onNodeWithText(testString(R.string.guardian_setting_tts_enabled)).assertIsDisplayed()
        composeRule.onNodeWithText(testString(R.string.guardian_setting_sound_enabled)).assertIsDisplayed()
        composeRule.onNodeWithText(testString(R.string.guardian_setting_haptic_enabled)).assertIsDisplayed()
        composeRule.onNodeWithText(testString(R.string.guardian_setting_tts_rate)).assertIsDisplayed()
        composeRule.onNodeWithText(testString(R.string.guardian_setting_tts_volume)).assertIsDisplayed()
        composeRule.onNodeWithText(testString(R.string.guardian_setting_notification_lead_times)).assertIsDisplayed()
        composeRule.onNodeWithText(testString(R.string.guardian_setting_quiet_hours))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun homeMenuShowsCreateRoutineSetFirstAndRoutesIt() {
        var createClicks = 0

        composeRule.setContent {
            SteppieTheme {
                TestGuardianModeScreen(
                    state = GuardianModeUiState(
                        isActive = true,
                        isAuthenticated = true,
                        destination = GuardianDestination.Home,
                    ),
                    routineSetActions = GuardianRoutineSetActionCallbacks(
                        onOpenRoutineSetCreate = { createClicks += 1 },
                    ),
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            guardianMenuDescription(
                R.string.guardian_menu_create_routine_set,
                R.string.guardian_menu_create_routine_set_desc,
            ),
        ).performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(1, createClicks)
        }
    }

    @Test
    fun routineEditWithoutActiveSetShowsCreateRoutineSetEmptyState() {
        composeRule.setContent {
            SteppieTheme {
                TestGuardianModeScreen(
                    state = GuardianModeUiState(
                        isActive = true,
                        isAuthenticated = true,
                        destination = GuardianDestination.RoutineEdit,
                    ),
                )
            }
        }

        composeRule.onNodeWithText(testString(R.string.guardian_empty_routine_set_title)).assertIsDisplayed()
        composeRule.onAllNodesWithText(testString(R.string.guardian_menu_create_routine_set)).assertCountEquals(2)
    }

    @Test
    fun routineEditShowsDailyScheduleActionsAndSelectsAnEditingTarget() {
        var selectedRoutineSetId: String? = null

        composeRule.setContent {
            SteppieTheme {
                TestGuardianModeScreen(
                    state = GuardianModeUiState(
                        isActive = true,
                        isAuthenticated = true,
                        destination = GuardianDestination.RoutineEdit,
                        routineSets = RoutineSampleData.routineSets,
                        activeRoutineSet = RoutineSampleData.morning,
                        routines = RoutineSampleData.morning.routines,
                        selectedRoutineSetId = RoutineSampleData.morning.id,
                        todayRoutineSetId = RoutineSampleData.morning.id,
                    ),
                    routineSetActions = GuardianRoutineSetActionCallbacks(
                        onSelectRoutineSet = { selectedRoutineSetId = it },
                    ),
                )
            }
        }

        composeRule.onAllNodesWithText(RoutineSampleData.morning.name.testText()).assertCountEquals(2)
        composeRule.onNodeWithText(RoutineSampleData.school.name.testText()).assertIsDisplayed()
        composeRule.onNodeWithText(RoutineSampleData.bedtime.name.testText()).assertIsDisplayed()
        composeRule.onAllNodesWithText(testString(R.string.guardian_routine_set_disable_daily)).assertCountEquals(1)
        composeRule.onAllNodesWithText(testString(R.string.guardian_routine_set_enable_daily)).assertCountEquals(2)
        composeRule.onNodeWithText(RoutineSampleData.school.name.testText()).performClick()

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
                TestGuardianModeScreen(
                    state = GuardianModeUiState(
                        isActive = true,
                        isAuthenticated = true,
                        destination = GuardianDestination.RoutineEdit,
                        routineSets = listOf(RoutineSampleData.morning),
                        activeRoutineSet = RoutineSampleData.morning,
                        routines = RoutineSampleData.morning.routines,
                        routineSetListEditing = true,
                    ),
                    routineSetActions = GuardianRoutineSetActionCallbacks(
                        onRequestEditRoutineSetName = { editClicks += 1 },
                        onRequestDeleteRoutineSet = { deleteClicks += 1 },
                    ),
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
                TestGuardianModeScreen(
                    state = GuardianModeUiState(
                        isActive = true,
                        isAuthenticated = true,
                        destination = GuardianDestination.RoutineSetCreate,
                        routineSetDraft = RoutineSetDraft(name = "주말 루틴"),
                    ),
                )
            }
        }

        composeRule.onNodeWithText(testString(R.string.action_save))
            .performScrollTo()
            .assertIsNotEnabled()
        composeRule.onNodeWithText(testString(R.string.guardian_step_list_empty))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun routineEditUsesDragHandleInsteadOfArrowButtons() {
        composeRule.setContent {
            SteppieTheme {
                TestGuardianModeScreen(
                    state = GuardianModeUiState(
                        isActive = true,
                        isAuthenticated = true,
                        destination = GuardianDestination.RoutineEdit,
                        activeRoutineSet = RoutineSampleData.morning,
                        routines = RoutineSampleData.morning.routines,
                    ),
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
