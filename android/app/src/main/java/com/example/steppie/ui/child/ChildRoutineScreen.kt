package com.example.steppie.ui.child

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick as semanticOnClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.steppie.R
import com.example.steppie.presentation.environment.currentPresentationLocale
import com.example.steppie.ui.theme.SteppieLayout
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.theme.SteppieStroke
import com.example.steppie.ui.theme.SteppieTheme
import com.example.steppie.ui.tutorial.TutorialTarget
import com.example.steppie.ui.tutorial.tutorialAnchor

@Composable
fun ChildRoutineScreen(
    state: ChildRoutineUiState,
    onShowList: () -> Unit,
    onShowFocus: () -> Unit,
    onSelectRoutine: (String) -> Unit,
    onCompleteRoutine: () -> Unit,
    onAdvanceFromFeedback: () -> Unit,
    onUndoRoutine: () -> Unit,
    modifier: Modifier = Modifier,
    onRequestGuardianMode: () -> Unit = {},
) {
    ChildRoutineLayoutHost(
        state = state,
        onShowList = onShowList,
        onShowFocus = onShowFocus,
        onSelectRoutine = onSelectRoutine,
        onCompleteRoutine = onCompleteRoutine,
        onAdvanceFromFeedback = onAdvanceFromFeedback,
        onUndoRoutine = onUndoRoutine,
        onRequestGuardianMode = onRequestGuardianMode,
        modifier = modifier,
    )
}

@Composable
internal fun LoadingContent() {
    val description = stringResource(R.string.state_loading)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun SplitRoutineLayout(
    state: ChildRoutineUiState,
    onSelectRoutine: (String) -> Unit,
    onCompleteRoutine: () -> Unit,
    onAdvanceFromFeedback: () -> Unit,
    onUndoRoutine: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .testTag("split_layout"),
    ) {
        Column(
            modifier = Modifier
                .width(SteppieLayout.SplitListWidth)
                .fillMaxHeight()
                .testTag("split_list_pane")
                .tutorialAnchor(TutorialTarget.ChildListNavigation)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(
                    horizontal = SteppieLayout.GuardianScreenPadding,
                    vertical = SteppieSpacing.ExtraLarge,
                ),
            verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium),
        ) {
            Text(
                text = stringResource(R.string.child_list_title),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = SteppieTheme.typography.guardianTitle,
            )
            ProgressIndicator(completed = state.progressCount, total = state.progressTotal)
            RoutineList(
                state = state,
                onSelectRoutine = onSelectRoutine,
                modifier = Modifier.fillMaxSize(),
            )
        }
        VerticalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .width(SteppieStroke.Divider),
            color = MaterialTheme.colorScheme.outline,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("split_focus_pane")
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(SteppieSpacing.TwoExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (state.feedbackRoutine != null) {
                    stringResource(R.string.child_feedback_title)
                } else {
                    stringResource(R.string.child_focus_title)
                },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = SteppieTheme.typography.guardianTitle,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(28.dp))
            val selectedRoutine = state.selectedRoutine
            when {
                selectedRoutine != null -> {
                    FocusRoutineContent(
                        routine = selectedRoutine,
                        state = state,
                        tablet = true,
                        onCompleteRoutine = onCompleteRoutine,
                    )
                    if (state.undoRoutine != null) {
                        Spacer(Modifier.size(SteppieSpacing.Medium))
                        UndoFeedbackButton(
                            label = stringResource(R.string.child_undo_mistap),
                            onClick = onUndoRoutine,
                        )
                    }
                    if (state.feedbackRoutine != null) {
                        val nextRoutine = state.nextIncompleteRoutine
                        if (nextRoutine != null) {
                            Spacer(Modifier.size(SteppieSpacing.Medium))
                            NextRoutinePreview(routine = nextRoutine, onClick = onAdvanceFromFeedback)
                        } else if (state.hasRemainingSchedule) {
                            Spacer(Modifier.size(SteppieSpacing.Medium))
                            ContinueSchedulePreview(onClick = onAdvanceFromFeedback)
                        } else {
                            Spacer(Modifier.size(SteppieSpacing.Medium))
                            AllCompletePreview(onClick = onAdvanceFromFeedback)
                        }
                    }
                }
                state.isAllComplete -> AllCompleteContent(feedbackIntensity = state.feedbackIntensity)
                state.isWaiting -> WaitingRoutineContent(state)
                else -> EmptyRoutineContent()
            }
        }
    }
}

@Composable
internal fun FocusListNavigation(
    label: String,
    directionUp: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionDescription = if (directionUp) {
        stringResource(R.string.a11y_show_focus)
    } else {
        stringResource(R.string.a11y_show_list)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(if (directionUp) "show_focus" else "show_list")
            .tutorialAnchor(TutorialTarget.ChildListNavigation)
            .heightIn(min = SteppieLayout.ChildMinimumTouchTarget)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clearAndSetSemantics {
                contentDescription = actionDescription
                role = Role.Button
                semanticOnClick {
                    onClick()
                    true
                }
            }
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (directionUp) "⌃" else "⌄",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = SteppieTheme.typography.childProgress,
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = SteppieTheme.typography.button,
        )
    }
}

@Composable
internal fun EmptyRoutineContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.child_empty_title),
            color = MaterialTheme.colorScheme.onSurface,
            style = SteppieTheme.typography.childListTitle,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun WaitingRoutineContent(
    state: ChildRoutineUiState,
    modifier: Modifier = Modifier,
) {
    val setName = state.waitingRoutineSet
        ?.name
        ?.resolve(null, currentPresentationLocale().toLanguageTag())
        .orEmpty()
    val startTime = state.waitingUntil?.toString().orEmpty()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp)
            .semantics {
                contentDescription = "$setName, $startTime"
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
        ) {
            Text(
                text = stringResource(R.string.child_waiting_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.childListTitle,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.child_waiting_message, startTime, setName),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = SteppieTheme.typography.childProgress,
                textAlign = TextAlign.Center,
            )
        }
    }
}

internal fun Modifier.onVerticalSwipe(
    onSwipeUp: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null,
): Modifier = pointerInput(onSwipeUp, onSwipeDown) {
    var distance = 0f
    detectVerticalDragGestures(
        onDragStart = { distance = 0f },
        onVerticalDrag = { _, dragAmount -> distance += dragAmount },
        onDragEnd = {
            when {
                distance <= -80f -> onSwipeUp?.invoke()
                distance >= 80f -> onSwipeDown?.invoke()
            }
        },
    )
}

internal fun Modifier.weightlessFill(): Modifier = fillMaxWidth().heightIn(min = 448.dp)
