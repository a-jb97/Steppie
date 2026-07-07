package com.example.steppie.ui.child

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import com.example.steppie.domain.model.FeedbackIntensity
import com.example.steppie.domain.model.Routine
import com.example.steppie.ui.components.RoutineCard
import com.example.steppie.ui.components.RoutineCardColor
import com.example.steppie.ui.components.RoutineCardPresentation
import com.example.steppie.ui.components.RoutineCardState
import com.example.steppie.ui.components.rememberAnimationsEnabled
import com.example.steppie.ui.theme.SteppieCornerRadius
import com.example.steppie.ui.theme.SteppieLayout
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.theme.SteppieStroke
import com.example.steppie.ui.theme.SteppieTheme
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

private val SplitLayoutMinimumWidth = 905.dp

@Composable
fun ChildRoutineScreen(
    state: ChildRoutineUiState,
    onShowList: () -> Unit,
    onShowFocus: () -> Unit,
    onSelectRoutine: (String) -> Unit,
    onCompleteRoutine: () -> Unit,
    onAdvanceFromFeedback: () -> Unit,
    onUndoRoutine: () -> Unit,
    onRequestGuardianMode: () -> Unit = {},
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

@Composable
private fun LoadingContent() {
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
private fun PhoneFocusView(
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
private fun SplitRoutineLayout(
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
                        } else {
                            Spacer(Modifier.size(SteppieSpacing.Medium))
                            AllCompletePreview(onClick = onAdvanceFromFeedback)
                        }
                    }
                }
                state.isAllComplete -> AllCompleteContent(feedbackIntensity = state.feedbackIntensity)
                else -> EmptyRoutineContent()
            }
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
            color = if (title == stringResource(R.string.child_feedback_title)) {
                SteppieTheme.colors.success
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
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
private fun ProgressIndicator(completed: Int, total: Int) {
    val description = stringResource(R.string.a11y_progress, completed, total)
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = description
        },
        horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < completed) {
                            SteppieTheme.colors.progressComplete
                        } else {
                            SteppieTheme.colors.progressPending
                        },
                    ),
            )
        }
        Text(
            text = stringResource(R.string.child_progress_label, completed, total),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = SteppieTheme.typography.childProgress,
        )
    }
}

@Composable
private fun FocusRoutineContent(
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
            .then(if (tablet) Modifier.heightIn(min = 520.dp) else Modifier),
        presentation = RoutineCardPresentation.Focus,
        cardColor = routine.cardColor(),
        meta = if (isCompleted) {
            stringResource(R.string.routine_state_completed)
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
                meta = routine.listMeta(isCurrent, isCompleted),
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
private fun Routine.listMeta(isCurrent: Boolean, isCompleted: Boolean): String {
    val orderLabel = order + 1
    return when {
        isCompleted -> stringResource(R.string.routine_state_completed)
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
private fun UndoFeedbackButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .widthIn(min = 198.dp)
            .heightIn(min = SteppieLayout.ChildMinimumTouchTarget)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, Color(0xFF5D4037), RoundedCornerShape(18.dp))
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                semanticOnClick {
                    onClick()
                    true
                }
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = SteppieSpacing.Medium, vertical = SteppieSpacing.TwoExtraSmall),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_undo_arrow),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.size(SteppieSpacing.ExtraSmall))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = SteppieTheme.typography.button,
        )
    }
}

@Composable
private fun NextRoutinePreview(
    routine: Routine,
    onClick: () -> Unit,
) {
    val description = stringResource(R.string.child_next_routine, routine.localizedTitle())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SteppieLayout.ChildMinimumTouchTarget)
            .clip(RoundedCornerShape(SteppieCornerRadius.Card))
            .background(routine.previewColor())
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                semanticOnClick {
                    onClick()
                    true
                }
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = SteppieSpacing.Medium, vertical = SteppieSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
    ) {
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center,
        ) {
            RoutineIcon(icon = routine.icon, focus = false, modifier = Modifier.size(48.dp))
        }
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurface,
            style = SteppieTheme.typography.childListTitle,
            maxLines = 2,
        )
    }
}

@Composable
private fun AllCompletePreview(onClick: () -> Unit) {
    val description = stringResource(R.string.a11y_all_complete)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SteppieLayout.ChildMinimumTouchTarget)
            .clip(RoundedCornerShape(SteppieCornerRadius.Card))
            .background(SteppieTheme.colors.cardSky)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                semanticOnClick {
                    onClick()
                    true
                }
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = SteppieSpacing.Medium, vertical = SteppieSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
    ) {
        Image(
            painter = painterResource(R.drawable.complete_stamp),
            contentDescription = null,
            modifier = Modifier.size(52.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall),
        ) {
            Text(
                text = stringResource(R.string.child_all_complete_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.button,
            )
            Text(
                text = stringResource(R.string.child_all_complete_subtitle),
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.guardianCaption,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun AllCompleteContent(
    feedbackIntensity: FeedbackIntensity,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.a11y_all_complete)
    val animationsEnabled = rememberAnimationsEnabled()
    val shouldPulseStamp =
        animationsEnabled &&
        feedbackIntensity in setOf(FeedbackIntensity.Strong, FeedbackIntensity.Normal)
    val stampPulse = remember { Animatable(0f) }
    LaunchedEffect(shouldPulseStamp) {
        if (shouldPulseStamp) {
            stampPulse.snapTo(0f)
            stampPulse.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
            )
        } else {
            stampPulse.snapTo(0f)
        }
    }
    val stampScale = if (shouldPulseStamp) {
        1f + 0.10f * kotlin.math.sin(stampPulse.value * Math.PI).toFloat()
    } else {
        1f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 620.dp)
            .clip(RoundedCornerShape(SteppieCornerRadius.Sheet))
            .background(SteppieTheme.colors.cardSky)
            .semantics(mergeDescendants = true) {
                contentDescription = description
            }
            .padding(SteppieSpacing.ExtraLarge),
        contentAlignment = Alignment.Center,
    ) {
        if (animationsEnabled && feedbackIntensity == FeedbackIntensity.Strong) {
            BirthdayFireworksLayer(
                modifier = Modifier
                    .matchParentSize()
                    .testTag("birthday_fireworks_layer"),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.complete_stamp),
                contentDescription = null,
                modifier = Modifier
                    .size(width = 168.dp, height = 164.dp)
                    .scale(stampScale),
            )
            Spacer(Modifier.size(SteppieSpacing.Large))
            Text(
                text = stringResource(R.string.child_all_complete_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.childCardTitle,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(SteppieSpacing.Medium))
            Text(
                text = stringResource(R.string.child_all_complete_subtitle),
                color = MaterialTheme.colorScheme.primary,
                style = SteppieTheme.typography.guardianSection,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CheckParticleBurstLayer(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 860, easing = FastOutSlowInEasing),
        )
    }
    val colors = listOf(
        Color(0xFFFFD54F),
        Color(0xFF80CBC4),
        Color(0xFFF48FB1),
        Color(0xFF64B5F6),
        Color(0xFFFF8A65),
        Color(0xFFA5D6A7),
        Color(0xFFC77A4A),
        Color(0xFFFFF176),
    )

    Canvas(modifier = modifier) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val eased = 1f - (1f - progress.value) * (1f - progress.value)
        val alpha = (1f - progress.value).coerceIn(0f, 1f)
        val primaryRadius = size.minDimension * 0.52f
        val secondaryRadius = size.minDimension * 0.34f
        repeat(24) { particle ->
            val angle = Math.PI * 2.0 * particle / 24.0
            val stagger = if (particle % 2 == 0) 1f else 0.78f
            val distance = primaryRadius * eased * stagger
            val particleCenter = androidx.compose.ui.geometry.Offset(
                x = center.x + kotlin.math.cos(angle).toFloat() * distance,
                y = center.y + kotlin.math.sin(angle).toFloat() * distance,
            )
            val trailStart = androidx.compose.ui.geometry.Offset(
                x = center.x + kotlin.math.cos(angle).toFloat() * distance * 0.58f,
                y = center.y + kotlin.math.sin(angle).toFloat() * distance * 0.58f,
            )
            val color = colors[particle % colors.size].copy(alpha = alpha)
            drawLine(
                color = color.copy(alpha = alpha * 0.65f),
                start = trailStart,
                end = particleCenter,
                strokeWidth = 3.5f * (1f - progress.value) + 1f,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = color,
                radius = 3.5f + 5.5f * (1f - progress.value),
                center = particleCenter,
            )
        }
        repeat(12) { particle ->
            val angle = (Math.PI * 2.0 * particle / 12.0) + 0.26
            val distance = secondaryRadius * eased
            drawCircle(
                color = colors[(particle + 3) % colors.size].copy(alpha = alpha * 0.80f),
                radius = 2.5f + 3.5f * (1f - progress.value),
                center = androidx.compose.ui.geometry.Offset(
                    x = center.x + kotlin.math.cos(angle).toFloat() * distance,
                    y = center.y + kotlin.math.sin(angle).toFloat() * distance,
                ),
            )
        }
    }
}

@Composable
private fun BirthdayFireworksLayer(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(80)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1_360, easing = LinearEasing),
        )
    }
    val colors = listOf(
        Color(0xFFFFD54F),
        Color(0xFF80CBC4),
        Color(0xFFF48FB1),
        Color(0xFF64B5F6),
        Color(0xFFFFAB91),
    )
    val bursts = remember {
        listOf(
            FireworkBurst(x = 0.22f, y = 0.26f, delay = 0.00f, radius = 0.20f),
            FireworkBurst(x = 0.76f, y = 0.22f, delay = 0.10f, radius = 0.22f),
            FireworkBurst(x = 0.50f, y = 0.34f, delay = 0.22f, radius = 0.18f),
            FireworkBurst(x = 0.32f, y = 0.52f, delay = 0.34f, radius = 0.16f),
            FireworkBurst(x = 0.70f, y = 0.50f, delay = 0.42f, radius = 0.16f),
        )
    }

    Canvas(modifier = modifier) {
        bursts.forEachIndexed { burstIndex, burst ->
            val localProgress = ((progress.value - burst.delay) / 0.58f).coerceIn(0f, 1f)
            if (localProgress <= 0f || localProgress >= 1f) return@forEachIndexed
            val eased = 1f - (1f - localProgress) * (1f - localProgress)
            val alpha = (1f - localProgress).coerceIn(0f, 1f)
            val center = androidx.compose.ui.geometry.Offset(
                x = size.width * burst.x,
                y = size.height * burst.y,
            )
            val maxRadius = size.minDimension * burst.radius
            repeat(12) { particle ->
                val angle = (Math.PI * 2.0 * particle / 12.0) + (burstIndex * 0.18)
                val distance = maxRadius * eased
                val particleCenter = androidx.compose.ui.geometry.Offset(
                    x = center.x + kotlin.math.cos(angle).toFloat() * distance,
                    y = center.y + kotlin.math.sin(angle).toFloat() * distance,
                )
                drawCircle(
                    color = colors[(particle + burstIndex) % colors.size].copy(alpha = alpha),
                    radius = 5f + 4f * (1f - localProgress),
                    center = particleCenter,
                )
            }
        }
    }
}

private data class FireworkBurst(
    val x: Float,
    val y: Float,
    val delay: Float,
    val radius: Float,
)

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

@Composable
private fun Routine.previewColor(): Color = when (cardColor()) {
    RoutineCardColor.Sky -> SteppieTheme.colors.cardSky
    RoutineCardColor.Mint -> SteppieTheme.colors.cardMint
    RoutineCardColor.Lemon -> SteppieTheme.colors.cardLemon
    RoutineCardColor.Peach -> SteppieTheme.colors.cardPeach
    RoutineCardColor.Lavender -> SteppieTheme.colors.cardLavender
    RoutineCardColor.Rose -> SteppieTheme.colors.cardRose
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
