package com.example.steppie.ui.child

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.steppie.data.sample.RoutineSampleData
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
                    modifier = Modifier.requiredSize(393.dp, 852.dp),
                )
            }
        }

        composeRule.onNodeWithTag("routine_${target.id}").performClick()
        composeRule.onNodeWithTag("phone_focus").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(target.id, selectedId) }
    }

    private fun sampleState(): ChildRoutineUiState = childRoutineState(
        routines = RoutineSampleData.morning.routines,
        selectedRoutineId = null,
        singlePane = ChildSinglePane.Focus,
    )
}
