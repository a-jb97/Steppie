package com.example.steppie.ui.child

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.steppie.ui.theme.SteppieLayout
import com.example.steppie.ui.tutorial.TutorialTarget
import com.example.steppie.ui.tutorial.tutorialAnchor
import kotlinx.coroutines.withTimeoutOrNull

private val SplitLayoutMinimumWidth = 905.dp

@Composable
internal fun ChildRoutineLayoutHost(
    state: ChildRoutineUiState,
    onShowList: () -> Unit,
    onShowFocus: () -> Unit,
    onSelectRoutine: (String) -> Unit,
    onCompleteRoutine: () -> Unit,
    onAdvanceFromFeedback: () -> Unit,
    onUndoRoutine: () -> Unit,
    onRequestGuardianMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        when {
            state.isLoading -> LoadingContent()
            maxWidth >= SplitLayoutMinimumWidth && maxWidth > maxHeight -> SplitRoutineLayout(
                state = state,
                onSelectRoutine = onSelectRoutine,
                onCompleteRoutine = onCompleteRoutine,
                onAdvanceFromFeedback = onAdvanceFromFeedback,
                onUndoRoutine = onUndoRoutine,
            )
            state.singlePane == ChildSinglePane.List -> PhoneRoutineList(
                state = state,
                onShowFocus = onShowFocus,
                onSelectRoutine = onSelectRoutine,
            )
            else -> PhoneFocusView(
                state = state,
                onShowList = onShowList,
                onCompleteRoutine = onCompleteRoutine,
                onAdvanceFromFeedback = onAdvanceFromFeedback,
                onUndoRoutine = onUndoRoutine,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(SteppieLayout.ChildMinimumTouchTarget)
                .tutorialAnchor(TutorialTarget.GuardianEntry)
                .testTag("guardian_hidden_entry")
                .pointerInput(onRequestGuardianMode) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val heldForThreeSeconds: Boolean = withTimeoutOrNull<Boolean>(3_000L) {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.none { it.id == down.id && it.pressed }) {
                                    return@withTimeoutOrNull false
                                }
                            }
                            false
                        } ?: true
                        if (heldForThreeSeconds) onRequestGuardianMode()
                    }
                },
        )
    }
}
