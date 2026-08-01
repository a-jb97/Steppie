package com.example.steppie.ui.child

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.steppie.R
import com.example.steppie.domain.model.FeedbackIntensity
import com.example.steppie.domain.model.Routine
import com.example.steppie.ui.components.RoutineCard
import com.example.steppie.ui.components.RoutineCardPresentation
import com.example.steppie.ui.components.RoutineCardState
import com.example.steppie.ui.components.rememberAnimationsEnabled
import com.example.steppie.ui.theme.SteppieLayout
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.tutorial.TutorialTarget
import com.example.steppie.ui.tutorial.tutorialAnchor

@Composable
internal fun PhoneFocusView(
    state: ChildRoutineUiState,
    onShowList: () -> Unit,
    onCompleteRoutine: () -> Unit,
    onAdvanceFromFeedback: () -> Unit,
    onUndoRoutine: () -> Unit,
) {
    val routine = state.selectedRoutine
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("phone_focus")
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onVerticalSwipe(onSwipeUp = onShowList)
            .verticalScroll(rememberScrollState())
            .padding(SteppieLayout.ChildScreenPadding),
        verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Large),
    ) {
        ChildHeader(
            title = if (state.feedbackRoutine != null) {
                stringResource(R.string.child_feedback_title)
            } else {
                stringResource(R.string.child_focus_title)
            },
            subtitle = if (state.feedbackRoutine != null) {
                stringResource(R.string.child_feedback_subtitle)
            } else if (state.isRoutineSetLocked) {
                stringResource(R.string.child_locked_set_subtitle, state.waitingUntil?.toString().orEmpty())
            } else {
                stringResource(R.string.child_focus_subtitle)
            },
        )
        ProgressIndicator(completed = state.progressCount, total = state.progressTotal)
        when {
            routine != null -> FocusRoutineContent(
                routine = routine,
                state = state,
                tablet = false,
                onCompleteRoutine = onCompleteRoutine,
            )
            state.isAllComplete -> AllCompleteContent(
                feedbackIntensity = state.feedbackIntensity,
                modifier = Modifier.weightlessFill(),
            )
            state.isWaiting -> WaitingRoutineContent(
                state = state,
                modifier = Modifier.weightlessFill(),
            )
            else -> EmptyRoutineContent(modifier = Modifier.weightlessFill())
        }
        if (state.undoRoutine != null) {
            UndoFeedbackButton(
                label = stringResource(R.string.child_undo_mistap),
                onClick = onUndoRoutine,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        if (state.feedbackRoutine != null) {
            val nextRoutine = state.nextIncompleteRoutine
            if (nextRoutine != null) {
                NextRoutinePreview(routine = nextRoutine, onClick = onAdvanceFromFeedback)
            } else if (state.hasRemainingSchedule) {
                ContinueSchedulePreview(onClick = onAdvanceFromFeedback)
            } else {
                AllCompletePreview(onClick = onAdvanceFromFeedback)
            }
        }
        FocusListNavigation(
            label = stringResource(R.string.child_list_title),
            directionUp = false,
            onClick = onShowList,
        )
    }
}

@Composable
internal fun FocusRoutineContent(
    routine: Routine,
    state: ChildRoutineUiState,
    tablet: Boolean,
    onCompleteRoutine: () -> Unit,
) {
    val title = routine.localizedTitle()
    val isFeedback = state.feedbackRoutineId == routine.id
    val isCompleted = routine.id in state.completedRoutineIds || isFeedback
    val isCurrent = state.currentRoutine?.id == routine.id
    val animationsEnabled = rememberAnimationsEnabled()
    val cardState = when {
        isCompleted -> RoutineCardState.Completed
        isCurrent -> RoutineCardState.Current
        else -> RoutineCardState.Upcoming
    }
    RoutineCard(
        title = if (isFeedback) stringResource(R.string.child_completed_card_title, title) else title,
        state = cardState,
        onClick = onCompleteRoutine,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("focus_routine_card")
            .tutorialAnchor(TutorialTarget.ChildCard)
            .then(if (tablet) Modifier.heightIn(min = 520.dp) else Modifier),
        presentation = RoutineCardPresentation.Focus,
        cardColor = routine.cardColor(),
        meta = if (isFeedback) {
            ""
        } else if (isCompleted) {
            null
        } else if (state.isRoutineSetLocked) {
            stringResource(R.string.child_locked_set_hint, state.waitingUntil?.toString().orEmpty())
        } else if (isCurrent) {
            stringResource(R.string.child_focus_tap_hint)
        } else {
            stringResource(R.string.child_focus_upcoming_hint)
        },
        focusMaxWidth = if (tablet) {
            SteppieLayout.FocusCardTabletMaxWidth
        } else {
            SteppieLayout.FocusCardPhoneMaxWidth
        },
    ) {
        if (isFeedback) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (animationsEnabled && state.feedbackIntensity == FeedbackIntensity.Strong) {
                    CheckParticleBurstLayer(
                        modifier = Modifier
                            .matchParentSize()
                            .testTag("check_particle_burst_layer"),
                    )
                }
                Image(
                    painter = painterResource(R.drawable.ic_feedback_check),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            RoutineIcon(icon = routine.icon, focus = true, modifier = Modifier.fillMaxSize())
        }
    }
}
