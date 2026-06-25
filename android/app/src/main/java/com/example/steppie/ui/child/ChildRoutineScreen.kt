package com.example.steppie.ui.child

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.steppie.R
import com.example.steppie.domain.model.Routine
import com.example.steppie.ui.components.RoutineCard
import com.example.steppie.ui.components.RoutineCardPresentation
import com.example.steppie.ui.components.RoutineCardState
import com.example.steppie.ui.theme.SteppieLayout
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.theme.SteppieStroke
import com.example.steppie.ui.theme.SteppieTheme
import java.util.Locale

private val SplitLayoutMinimumWidth = 905.dp

@Composable
fun ChildRoutineScreen(
    state: ChildRoutineUiState,
    onShowList: () -> Unit,
    onShowFocus: () -> Unit,
    onSelectRoutine: (String) -> Unit,
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
            )
            state.singlePane == ChildSinglePane.List -> PhoneRoutineList(
                state = state,
                onShowFocus = onShowFocus,
                onSelectRoutine = onSelectRoutine,
            )
            else -> PhoneFocusView(
                state = state,
                onShowList = onShowList,
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PhoneFocusView(
    state: ChildRoutineUiState,
    onShowList: () -> Unit,
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
            title = stringResource(R.string.child_focus_title),
            subtitle = stringResource(R.string.child_focus_subtitle),
        )
        ProgressIndicator(total = state.routines.size)
        if (routine == null) {
            EmptyRoutineContent(modifier = Modifier.weightlessFill())
        } else {
            FocusRoutineContent(
                routine = routine,
                tablet = false,
            )
        }
        FocusListNavigation(
            label = stringResource(R.string.child_list_title),
            directionUp = false,
            onClick = onShowList,
        )
    }
}

@Composable
private fun PhoneRoutineList(
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
            ProgressIndicator(total = state.routines.size)
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
private fun SplitRoutineLayout(
    state: ChildRoutineUiState,
    onSelectRoutine: (String) -> Unit,
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
                text = stringResource(R.string.child_focus_title),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = SteppieTheme.typography.guardianTitle,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(28.dp))
            state.selectedRoutine?.let { routine ->
                FocusRoutineContent(routine = routine, tablet = true)
            } ?: EmptyRoutineContent()
        }
    }
}

@Composable
private fun ChildHeader(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = SteppieTheme.typography.childCardTitle,
        )
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurface,
            style = SteppieTheme.typography.guardianCaption,
        )
    }
}

@Composable
private fun ProgressIndicator(total: Int) {
    val description = stringResource(R.string.a11y_progress, 0, total)
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = description
        },
        horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline),
            )
        }
        Text(
            text = stringResource(R.string.child_progress_label, 0, total),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = SteppieTheme.typography.childProgress,
        )
    }
}

@Composable
private fun FocusRoutineContent(
    routine: Routine,
    tablet: Boolean,
) {
    val title = routine.localizedTitle()
    RoutineCard(
        title = title,
        state = RoutineCardState.Current,
        onClick = {},
        modifier = Modifier
            .fillMaxWidth()
            .then(if (tablet) Modifier.heightIn(min = 520.dp) else Modifier),
        presentation = RoutineCardPresentation.Focus,
        cardColor = routine.cardColor(),
        meta = stringResource(R.string.child_focus_tap_hint),
        focusMaxWidth = if (tablet) {
            SteppieLayout.FocusCardTabletMaxWidth
        } else {
            SteppieLayout.FocusCardPhoneMaxWidth
        },
    ) {
        RoutineIcon(icon = routine.icon, focus = true, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun RoutineList(
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
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = contentBottomPadding),
    ) {
        items(state.routines, key = Routine::id) { routine ->
            val isCurrent = routine.id == state.selectedRoutineId
            RoutineCard(
                title = routine.localizedTitle(),
                state = if (isCurrent) RoutineCardState.Current else RoutineCardState.Upcoming,
                onClick = { onSelectRoutine(routine.id) },
                modifier = Modifier.testTag("routine_${routine.id}"),
                cardColor = routine.cardColor(),
                meta = routine.listMeta(isCurrent),
            ) {
                RoutineIcon(icon = routine.icon, focus = false, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun Routine.localizedTitle(): String {
    val configuration = LocalConfiguration.current
    val systemLocale = configuration.locales[0]?.toLanguageTag()
        ?: Locale.getDefault().toLanguageTag()
    return title.resolve(appLocale = null, systemLocale = systemLocale)
}

@Composable
private fun Routine.listMeta(isCurrent: Boolean): String {
    val orderLabel = order + 1
    return when {
        isCurrent -> stringResource(R.string.routine_meta_current, orderLabel)
        scheduledTime != null -> stringResource(
            R.string.routine_meta_scheduled,
            orderLabel,
            scheduledTime.toString(),
        )
        else -> stringResource(R.string.routine_meta_order, orderLabel)
    }
}

@Composable
private fun FocusListNavigation(
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
            .heightIn(min = SteppieLayout.ChildMinimumTouchTarget)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics(mergeDescendants = true) {
                contentDescription = actionDescription
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
private fun EmptyRoutineContent(modifier: Modifier = Modifier) {
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

private fun Modifier.onVerticalSwipe(
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

private fun Modifier.weightlessFill(): Modifier = fillMaxWidth().heightIn(min = 448.dp)
