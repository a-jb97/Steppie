package com.example.steppie.ui.child

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.steppie.R
import com.example.steppie.domain.model.Routine
import com.example.steppie.presentation.environment.currentPresentationLocale
import com.example.steppie.ui.components.RoutineCard
import com.example.steppie.ui.components.RoutineCardState
import com.example.steppie.ui.theme.SteppieLayout
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.tutorial.TutorialTarget
import com.example.steppie.ui.tutorial.tutorialAnchor

@Composable
internal fun PhoneRoutineList(
    state: ChildRoutineUiState,
    onShowFocus: () -> Unit,
    onSelectRoutine: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("phone_list")
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        FocusListNavigation(
            label = stringResource(R.string.child_focus_title),
            directionUp = true,
            onClick = onShowFocus,
            modifier = Modifier.onVerticalSwipe(onSwipeDown = onShowFocus),
        )
        Column(
            modifier = Modifier.padding(horizontal = SteppieLayout.ChildScreenPadding),
            verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium),
        ) {
            ChildHeader(
                title = stringResource(R.string.child_list_title),
                subtitle = stringResource(R.string.child_list_subtitle),
            )
            ProgressIndicator(completed = state.progressCount, total = state.progressTotal)
        }
        RoutineList(
            state = state,
            onSelectRoutine = onSelectRoutine,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = SteppieLayout.ChildScreenPadding,
                    end = SteppieLayout.ChildScreenPadding,
                    top = SteppieSpacing.Large,
                ),
            contentBottomPadding = SteppieLayout.ChildScreenPadding,
        )
    }
}

@Composable
internal fun RoutineList(
    state: ChildRoutineUiState,
    onSelectRoutine: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentBottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
) {
    if (state.routines.isEmpty()) {
        EmptyRoutineContent(modifier = modifier)
        return
    }
    LazyColumn(
        modifier = modifier.tutorialAnchor(TutorialTarget.ChildList),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = contentBottomPadding),
    ) {
        items(state.routines, key = Routine::id) { routine ->
            val isCurrent = routine.id == state.currentRoutine?.id
            val isCompleted = routine.id in state.completedRoutineIds
            RoutineCard(
                title = routine.localizedTitle(),
                state = when {
                    isCompleted -> RoutineCardState.Completed
                    isCurrent -> RoutineCardState.Current
                    else -> RoutineCardState.Upcoming
                },
                onClick = { onSelectRoutine(routine.id) },
                modifier = Modifier.testTag("routine_${routine.id}"),
                cardColor = routine.cardColor(),
                meta = routine.listMeta(
                    isCurrent = isCurrent,
                    isCompleted = isCompleted,
                    lockedUntil = state.waitingUntil.takeIf { state.isRoutineSetLocked },
                ),
            ) {
                RoutineIcon(icon = routine.icon, focus = false, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
internal fun Routine.localizedTitle(): String {
    return title.resolve(
        appLocale = null,
        systemLocale = currentPresentationLocale().toLanguageTag(),
    )
}

@Composable
private fun Routine.listMeta(
    isCurrent: Boolean,
    isCompleted: Boolean,
    lockedUntil: java.time.LocalTime? = null,
): String {
    val orderLabel = order + 1
    return when {
        isCompleted -> stringResource(R.string.routine_state_completed)
        lockedUntil != null -> stringResource(R.string.child_locked_set_hint, lockedUntil.toString())
        isCurrent -> stringResource(R.string.routine_meta_current, orderLabel)
        scheduledTime != null -> stringResource(
            R.string.routine_meta_scheduled,
            orderLabel,
            scheduledTime.toString(),
        )
        else -> stringResource(R.string.routine_meta_order, orderLabel)
    }
}
