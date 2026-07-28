package com.example.steppie.ui.child

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.steppie.data.sample.RoutineSampleData
import com.example.steppie.domain.model.FeedbackIntensity
import com.example.steppie.ui.theme.SteppieTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChildRoutineScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun phoneCanOpenListAndReturnToFocus() {
        var state by mutableStateOf(sampleState())
        composeRule.setContent {
            SteppieTheme {
                ChildRoutineScreen(
                    state = state,
                    onShowList = { state = state.copy(singlePane = ChildSinglePane.List) },
                    onShowFocus = { state = state.copy(singlePane = ChildSinglePane.Focus) },
                    onSelectRoutine = {},
                    onCompleteRoutine = {},
                    onAdvanceFromFeedback = {},
                    onUndoRoutine = {},
                    modifier = Modifier.requiredSize(393.dp, 852.dp),
                )
            }
        }

        composeRule.onNodeWithTag("phone_focus").assertIsDisplayed()
        composeRule.onNodeWithTag("show_list").performClick()
        composeRule.onNodeWithTag("phone_list").assertIsDisplayed()
        composeRule.onNodeWithTag("show_focus").performClick()
        composeRule.onNodeWithTag("phone_focus").assertIsDisplayed()
    }

    @Test
    fun wideWindowUsesSplitLayout() {
        composeRule.setContent {
            SteppieTheme {
                ChildRoutineScreen(
                    state = sampleState(),
                    onShowList = {},
                    onShowFocus = {},
                    onSelectRoutine = {},
                    onCompleteRoutine = {},
                    onAdvanceFromFeedback = {},
                    onUndoRoutine = {},
                    modifier = Modifier.requiredSize(1000.dp, 800.dp),
                )
            }
        }

        composeRule.onNodeWithTag("split_layout").assertIsDisplayed()
        composeRule.onNodeWithTag("split_list_pane").assertIsDisplayed()
        composeRule.onNodeWithTag("split_focus_pane").assertIsDisplayed()
    }

    @Test
    fun tabletPortraitUsesSinglePane() {
        composeRule.setContent {
            SteppieTheme {
                ChildRoutineScreen(
                    state = sampleState(),
                    onShowList = {},
                    onShowFocus = {},
                    onSelectRoutine = {},
                    onCompleteRoutine = {},
                    onAdvanceFromFeedback = {},
                    onUndoRoutine = {},
                    modifier = Modifier.requiredSize(1000.dp, 1200.dp),
                )
            }
        }

        composeRule.onNodeWithTag("phone_focus").assertIsDisplayed()
    }

    @Test
    fun listCardSelectsRoutineWithoutCompletingIt() {
        val target = RoutineSampleData.morning.routines[1]
        var selectedId: String? = null
        var state by mutableStateOf(sampleState().copy(singlePane = ChildSinglePane.List))
        composeRule.setContent {
            SteppieTheme {
                ChildRoutineScreen(
                    state = state,
                    onShowList = {},
                    onShowFocus = {},
                    onSelectRoutine = { routineId ->
                        selectedId = routineId
                        state = state.copy(
                            selectedRoutineId = routineId,
                            singlePane = ChildSinglePane.Focus,
                        )
                    },
                    onCompleteRoutine = {},
                    onAdvanceFromFeedback = {},
                    onUndoRoutine = {},
                    modifier = Modifier.requiredSize(393.dp, 852.dp),
                )
            }
        }

        composeRule.onNodeWithTag("routine_${target.id}").performClick()
        composeRule.onNodeWithTag("phone_focus").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(target.id, selectedId) }
    }

    @Test
    fun futureRoutineSelectedFromListShowsUpcomingHintAndCannotComplete() {
        val target = RoutineSampleData.morning.routines[2]
        var completed = 0
        var state by mutableStateOf(sampleState().copy(singlePane = ChildSinglePane.List))
        composeRule.setContent {
            SteppieTheme {
                ChildRoutineScreen(
                    state = state,
                    onShowList = {},
                    onShowFocus = {},
                    onSelectRoutine = { routineId ->
                        state = state.copy(
                            selectedRoutineId = routineId,
                            singlePane = ChildSinglePane.Focus,
                        )
                    },
                    onCompleteRoutine = { completed += 1 },
                    onAdvanceFromFeedback = {},
                    onUndoRoutine = {},
                    modifier = Modifier.requiredSize(393.dp, 852.dp),
                )
            }
        }

        composeRule.onNodeWithTag("routine_${target.id}").performClick()

        composeRule.onNodeWithText("앞으로 할 일이에요").assertIsDisplayed()
        composeRule.onNodeWithTag("focus_routine_card").assertHasNoClickAction()
        composeRule.runOnIdle { assertEquals(0, completed) }
    }

    @Test
    fun showFocusFromListReturnsToCurrentRoutine() {
        val futureRoutine = RoutineSampleData.morning.routines[2]
        val currentRoutine = RoutineSampleData.morning.routines[0]
        var state by mutableStateOf(
            sampleState().copy(
                selectedRoutineId = futureRoutine.id,
                singlePane = ChildSinglePane.List,
            ),
        )
        composeRule.setContent {
            SteppieTheme {
                ChildRoutineScreen(
                    state = state,
                    onShowList = {},
                    onShowFocus = {
                        state = state.copy(
                            selectedRoutineId = state.currentRoutine?.id,
                            singlePane = ChildSinglePane.Focus,
                        )
                    },
                    onSelectRoutine = {},
                    onCompleteRoutine = {},
                    onAdvanceFromFeedback = {},
                    onUndoRoutine = {},
                    modifier = Modifier.requiredSize(393.dp, 852.dp),
                )
            }
        }

        composeRule.onNodeWithTag("show_focus").performClick()

        composeRule.onNodeWithText("일어나기").assertIsDisplayed()
        composeRule.onNodeWithText("카드를 누르면 완료").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(currentRoutine.id, state.selectedRoutineId) }
    }

    @Test
    fun focusCardRequestsCompletionAndUndoRequestsRevert() {
        val target = RoutineSampleData.morning.routines.first()
        var completed = 0
        var advanced = 0
        var undone = 0
        var state by mutableStateOf(sampleState())
        composeRule.setContent {
            SteppieTheme {
                ChildRoutineScreen(
                    state = state,
                    onShowList = {},
                    onShowFocus = {},
                    onSelectRoutine = {},
                    onCompleteRoutine = {
                        completed += 1
                        state = state.copy(
                            completedRoutineIds = setOf(target.id),
                            feedbackRoutineId = target.id,
                            undoRoutineId = target.id,
                        )
                    },
                    onAdvanceFromFeedback = {
                        advanced += 1
                        state = state.copy(
                            selectedRoutineId = RoutineSampleData.morning.routines[1].id,
                            feedbackRoutineId = null,
                            undoRoutineId = null,
                        )
                    },
                    onUndoRoutine = {
                        undone += 1
                        state = sampleState()
                    },
                    modifier = Modifier.requiredSize(393.dp, 852.dp),
                )
            }
        }

        composeRule.onNodeWithTag("focus_routine_card").performClick()
        composeRule.onNodeWithTag("phone_focus").assertIsDisplayed()
        composeRule.onNodeWithText("일어나기 완료!").assertIsDisplayed()
        composeRule.onNodeWithText("완료했어요").assertDoesNotExist()
        composeRule.onNodeWithText("다음 : 세수하기").assertIsDisplayed()
        composeRule.onNodeWithText("카드를 잘못 눌렀어요").performClick()
        composeRule.runOnIdle {
            assertEquals(1, completed)
            assertEquals(0, advanced)
            assertEquals(1, undone)
        }
    }

    @Test
    fun feedbackStaysUntilNextPreviewIsTapped() {
        val routines = RoutineSampleData.morning.routines
        var advanced = 0
        var state by mutableStateOf(
            sampleState().copy(
                completedRoutineIds = setOf(routines[0].id),
                selectedRoutineId = routines[0].id,
                feedbackRoutineId = routines[0].id,
                undoRoutineId = routines[0].id,
            ),
        )
        composeRule.setContent {
            SteppieTheme {
                ChildRoutineScreen(
                    state = state,
                    onShowList = {},
                    onShowFocus = {},
                    onSelectRoutine = {},
                    onCompleteRoutine = {},
                    onAdvanceFromFeedback = {
                        advanced += 1
                        state = state.copy(
                            selectedRoutineId = routines[1].id,
                            feedbackRoutineId = null,
                            undoRoutineId = null,
                        )
                    },
                    onUndoRoutine = {},
                    modifier = Modifier.requiredSize(393.dp, 852.dp),
                )
            }
        }

        composeRule.onNodeWithText("일어나기 완료!").assertIsDisplayed()
        composeRule.onNodeWithText("다음 : 세수하기").performClick()
        composeRule.runOnIdle { assertEquals(1, advanced) }
        composeRule.onNodeWithText("세수하기").assertIsDisplayed()
    }

    @Test
    fun finalFeedbackShowsAllCompletePreviewUntilTapped() {
        val routines = RoutineSampleData.morning.routines
        val finalRoutine = routines.last()
        var advanced = 0
        var state by mutableStateOf(
            sampleState().copy(
                completedRoutineIds = routines.mapTo(mutableSetOf()) { it.id },
                selectedRoutineId = finalRoutine.id,
                feedbackRoutineId = finalRoutine.id,
                undoRoutineId = finalRoutine.id,
            ),
        )
        composeRule.setContent {
            SteppieTheme {
                ChildRoutineScreen(
                    state = state,
                    onShowList = {},
                    onShowFocus = {},
                    onSelectRoutine = {},
                    onCompleteRoutine = {},
                    onAdvanceFromFeedback = {
                        advanced += 1
                        state = state.copy(
                            selectedRoutineId = null,
                            feedbackRoutineId = null,
                            undoRoutineId = null,
                        )
                    },
                    onUndoRoutine = {},
                    modifier = Modifier.requiredSize(393.dp, 852.dp),
                )
            }
        }

        composeRule.onNodeWithText("가방 챙기기 완료!").assertIsDisplayed()
        composeRule.onNodeWithText("오늘 할 일 완료!").performClick()
        composeRule.runOnIdle { assertEquals(1, advanced) }
        composeRule.onNodeWithText("오늘 할 일 완료!").assertIsDisplayed()
    }

    @Test
    fun strongRoutineFeedbackShowsCheckParticles() {
        composeRule.setContent {
            SteppieTheme {
                ChildRoutineScreen(
                    state = routineFeedbackState(FeedbackIntensity.Strong),
                    onShowList = {},
                    onShowFocus = {},
                    onSelectRoutine = {},
                    onCompleteRoutine = {},
                    onAdvanceFromFeedback = {},
                    onUndoRoutine = {},
                    modifier = Modifier.requiredSize(393.dp, 852.dp),
                )
            }
        }

        composeRule.onNodeWithText("일어나기 완료!").assertIsDisplayed()
        composeRule.onNodeWithTag("check_particle_burst_layer", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun nonStrongRoutineFeedbackDoesNotShowCheckParticles() {
        listOf(
            FeedbackIntensity.Normal,
            FeedbackIntensity.Quiet,
            FeedbackIntensity.Off,
        ).forEach { intensity ->
            composeRule.setContent {
                SteppieTheme {
                    ChildRoutineScreen(
                        state = routineFeedbackState(intensity),
                        onShowList = {},
                        onShowFocus = {},
                        onSelectRoutine = {},
                        onCompleteRoutine = {},
                        onAdvanceFromFeedback = {},
                        onUndoRoutine = {},
                        modifier = Modifier.requiredSize(393.dp, 852.dp),
                    )
                }
            }

            composeRule.onNodeWithText("일어나기 완료!").assertIsDisplayed()
            composeRule.onAllNodesWithTag("check_particle_burst_layer", useUnmergedTree = true).assertCountEquals(0)
        }
    }

    @Test
    fun strongAllCompleteShowsBirthdayFireworks() {
        composeRule.setContent {
            SteppieTheme {
                ChildRoutineScreen(
                    state = allCompleteState(FeedbackIntensity.Strong),
                    onShowList = {},
                    onShowFocus = {},
                    onSelectRoutine = {},
                    onCompleteRoutine = {},
                    onAdvanceFromFeedback = {},
                    onUndoRoutine = {},
                    modifier = Modifier.requiredSize(393.dp, 852.dp),
                )
            }
        }

        composeRule.onNodeWithText("오늘 할 일 완료!").assertIsDisplayed()
        composeRule.onNodeWithTag("birthday_fireworks_layer", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun nonStrongAllCompleteDoesNotShowBirthdayFireworks() {
        listOf(
            FeedbackIntensity.Normal,
            FeedbackIntensity.Quiet,
            FeedbackIntensity.Off,
        ).forEach { intensity ->
            composeRule.setContent {
                SteppieTheme {
                    ChildRoutineScreen(
                        state = allCompleteState(intensity),
                        onShowList = {},
                        onShowFocus = {},
                        onSelectRoutine = {},
                        onCompleteRoutine = {},
                        onAdvanceFromFeedback = {},
                        onUndoRoutine = {},
                        modifier = Modifier.requiredSize(393.dp, 852.dp),
                    )
                }
            }

            composeRule.onNodeWithText("오늘 할 일 완료!").assertIsDisplayed()
            composeRule.onAllNodesWithTag("birthday_fireworks_layer", useUnmergedTree = true).assertCountEquals(0)
        }
    }

    private fun sampleState(): ChildRoutineUiState = childRoutineState(
        routines = RoutineSampleData.morning.routines,
        completedRoutineIds = emptySet(),
        selectedRoutineId = null,
        singlePane = ChildSinglePane.Focus,
    )

    private fun allCompleteState(intensity: FeedbackIntensity): ChildRoutineUiState {
        val routines = RoutineSampleData.morning.routines
        return childRoutineState(
            routines = routines,
            completedRoutineIds = routines.mapTo(mutableSetOf()) { it.id },
            selectedRoutineId = null,
            singlePane = ChildSinglePane.Focus,
            feedbackIntensity = intensity,
        )
    }

    private fun routineFeedbackState(intensity: FeedbackIntensity): ChildRoutineUiState {
        val routines = RoutineSampleData.morning.routines
        val routine = routines.first()
        return childRoutineState(
            routines = routines,
            completedRoutineIds = setOf(routine.id),
            selectedRoutineId = routine.id,
            singlePane = ChildSinglePane.Focus,
            feedbackRoutineId = routine.id,
            undoRoutineId = routine.id,
            feedbackIntensity = intensity,
        )
    }
}
