package com.example.steppie.ui.guardian

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick as semanticOnClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.steppie.R
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.model.RoutineSet
import com.example.steppie.presentation.formatting.formatLocalizedTime
import com.example.steppie.ui.child.RoutineIcon
import com.example.steppie.ui.components.SteppieButton
import com.example.steppie.ui.components.SteppieButtonStyle
import com.example.steppie.ui.theme.SteppieCornerRadius
import com.example.steppie.ui.theme.SteppieLayout
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.theme.SteppieStroke
import com.example.steppie.ui.theme.SteppieTheme
import java.util.Locale

@Composable
internal fun GuardianRoutineEditScreen(
    state: GuardianModeUiState,
    onNavigateBack: () -> Unit,
    onOpenRoutineSetCreate: () -> Unit,
    onOpenNewRoutineEditor: () -> Unit,
    onOpenRoutineEditor: (String) -> Unit,
    onToggleRoutineSetListEditing: () -> Unit,
    onSelectRoutineSet: (String) -> Unit,
    onSetRoutineSetForToday: (String) -> Unit,
    onRoutineSetStartTimeChange: (String, String) -> Unit,
    onRequestEditRoutineSetName: (String) -> Unit,
    onRequestDeleteRoutineSet: (String) -> Unit,
    onRequestDelete: (String) -> Unit,
    onMoveRoutine: (String, Int) -> Unit,
    onShowOutOfScopeNotice: () -> Unit,
) {
    GuardianScaffold(
        title = stringResource(R.string.guardian_menu_routine),
        subtitle = stringResource(R.string.guardian_routine_edit_subtitle),
        onBack = onNavigateBack,
        topActionLabel = stringResource(
            if (state.routineSetListEditing) R.string.action_done_editing else R.string.action_edit,
        ),
        onTopAction = onToggleRoutineSetListEditing,
        bottom = {
            if (state.activeRoutineSet == null) {
                Column(verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small)) {
                    SteppieButton(
                        label = stringResource(R.string.guardian_menu_create_routine_set),
                        onClick = onOpenRoutineSetCreate,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) {
        if (state.activeRoutineSet == null) {
            EmptyRoutineSetPanel(onOpenNewRoutineSet = onOpenRoutineSetCreate)
        }
        RoutineSetList(
            routineSets = state.routineSets,
            selectedRoutineSetId = state.selectedRoutineSetId,
            todayRoutineSetId = state.todayRoutineSetId,
            editing = state.routineSetListEditing,
            onSelectRoutineSet = onSelectRoutineSet,
            onSetRoutineSetForToday = onSetRoutineSetForToday,
            onRequestEditRoutineSetName = onRequestEditRoutineSetName,
            onRequestDeleteRoutineSet = onRequestDeleteRoutineSet,
        )
        state.activeRoutineSet?.let { selectedSet ->
            Column(
                modifier = Modifier.padding(top = SteppieSpacing.Small),
                verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
            ) {
                Text(
                    text = stringResource(R.string.guardian_routine_set_start_time_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = SteppieTheme.typography.guardianTitle,
                )
                ScheduledTimePicker(
                    scheduledTime = selectedSet.startTime?.toStorageString().orEmpty(),
                    onScheduledTimeChange = { value ->
                        onRoutineSetStartTimeChange(selectedSet.id, value)
                    },
                )
            }
        }
        if (state.activeRoutineSet != null) {
            Column(
                modifier = Modifier.padding(top = SteppieSpacing.Medium + SteppieSpacing.TwoExtraSmall),
                verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = SteppieLayout.GuardianMinimumTouchTarget),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            R.string.guardian_active_routine_steps_title,
                            state.activeRoutineSet?.name
                                ?.resolve(null, Locale.getDefault().toLanguageTag())
                                .orEmpty(),
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = SteppieTheme.typography.guardianTitle,
                    )
                }
                Text(
                    text = stringResource(R.string.guardian_active_routine_steps_desc),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = SteppieTheme.typography.guardianCaption,
                )
            }
        }
        state.routines.forEach { routine ->
            key(routine.id) {
                EditableRoutineRow(
                    routine = routine,
                    onClick = { onOpenRoutineEditor(routine.id) },
                    onDelete = { onRequestDelete(routine.id) },
                    onMove = { direction -> onMoveRoutine(routine.id, direction) },
                )
            }
        }
        if (state.activeRoutineSet != null) {
            SteppieButton(
                label = stringResource(R.string.guardian_add_routine),
                onClick = onOpenNewRoutineEditor,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RoutineSetList(
    routineSets: List<RoutineSet>,
    selectedRoutineSetId: String?,
    todayRoutineSetId: String?,
    editing: Boolean,
    onSelectRoutineSet: (String) -> Unit,
    onSetRoutineSetForToday: (String) -> Unit,
    onRequestEditRoutineSetName: (String) -> Unit,
    onRequestDeleteRoutineSet: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
    ) {
        routineSets.forEach { routineSet ->
            RoutineSetRow(
                routineSet = routineSet,
                selected = routineSet.id == selectedRoutineSetId,
                setForToday = routineSet.isActive,
                editing = editing,
                onSelect = { onSelectRoutineSet(routineSet.id) },
                onSetForToday = { onSetRoutineSetForToday(routineSet.id) },
                onEdit = { onRequestEditRoutineSetName(routineSet.id) },
                onDelete = { onRequestDeleteRoutineSet(routineSet.id) },
            )
        }
    }
}

@Composable
private fun RoutineSetRow(
    routineSet: RoutineSet,
    selected: Boolean,
    setForToday: Boolean,
    editing: Boolean,
    onSelect: () -> Unit,
    onSetForToday: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val borderColor = if (selected) SteppieTheme.colors.warning else MaterialTheme.colorScheme.outline
    val routineSetName = routineSet.name.resolve(null, Locale.getDefault().toLanguageTag())
    val meta = if (routineSet.isActive) {
        stringResource(
            R.string.guardian_routine_set_schedule_meta,
            routineSet.startTime?.let { formatLocalizedTime(it, Locale.getDefault()) }
                ?: stringResource(R.string.guardian_time_none),
            routineSet.routines.size,
        )
    } else {
        stringResource(R.string.guardian_routine_set_meta, routineSet.routines.size)
    }
    val selectionState = stringResource(if (selected) R.string.a11y_selected else R.string.a11y_not_selected)
    val accessibilityDescription = stringResource(R.string.a11y_routine_set_row, routineSetName, meta)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(SteppieCornerRadius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                if (selected) 2.dp else SteppieStroke.Divider,
                borderColor,
                RoundedCornerShape(SteppieCornerRadius.Card),
            )
            .then(
                if (editing) {
                    Modifier.semantics {
                        contentDescription = accessibilityDescription
                        stateDescription = selectionState
                    }
                } else {
                    Modifier.semantics {
                        contentDescription = accessibilityDescription
                        stateDescription = selectionState
                        role = Role.Button
                        semanticOnClick {
                            onSelect()
                            true
                        }
                    }
                },
            )
            .clickable(role = Role.Button, enabled = !editing, onClick = onSelect)
            .padding(horizontal = SteppieSpacing.Large, vertical = SteppieSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium),
    ) {
        RoutineSetSelectionMark(active = selected)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall)) {
            Text(
                text = routineSetName,
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.button,
                maxLines = 2,
            )
            Text(
                text = meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = SteppieTheme.typography.guardianCaption,
            )
        }
        if (editing) {
            IconTextButton(
                label = stringResource(R.string.a11y_item_action, routineSetName, stringResource(R.string.action_edit)),
                text = "✎",
                onClick = onEdit,
                danger = false,
            )
            IconTextButton(
                label = stringResource(R.string.a11y_item_action, routineSetName, stringResource(R.string.action_delete)),
                text = "-",
                onClick = onDelete,
                danger = true,
            )
        } else {
            SteppieButton(
                label = stringResource(
                    if (setForToday) {
                        R.string.guardian_routine_set_disable_daily
                    } else {
                        R.string.guardian_routine_set_enable_daily
                    },
                ),
                onClick = onSetForToday,
                style = SteppieButtonStyle.Secondary,
            )
        }
    }
}

@Composable
private fun RoutineSetSelectionMark(active: Boolean) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (active) SteppieTheme.colors.warning else Color.Transparent)
            .border(3.dp, SteppieTheme.colors.warning, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (active) {
            Text(
                text = "✓",
                color = MaterialTheme.colorScheme.onError,
                style = SteppieTheme.typography.button,
            )
        }
    }
}

@Composable
internal fun DailyRoutineSelectionDialog(
    routineSets: List<RoutineSet>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.guardian_daily_routine_prompt_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
            ) {
                Text(
                    text = stringResource(R.string.guardian_daily_routine_prompt_body),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = SteppieTheme.typography.guardianCaption,
                )
                routineSets.forEach { routineSet ->
                    val routineSetName = routineSet.name.resolve(null, Locale.getDefault().toLanguageTag())
                    val meta = stringResource(R.string.guardian_routine_set_meta, routineSet.routines.size)
                    val accessibilityDescription = stringResource(R.string.a11y_routine_set_row, routineSetName, meta)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = SteppieLayout.GuardianMinimumTouchTarget)
                            .clip(RoundedCornerShape(SteppieCornerRadius.Card))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                SteppieStroke.Divider,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(SteppieCornerRadius.Card),
                            )
                            .clickable(role = Role.Button) { onSelect(routineSet.id) }
                            .semantics {
                                contentDescription = accessibilityDescription
                            }
                            .padding(horizontal = SteppieSpacing.Medium, vertical = SteppieSpacing.Small),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = routineSetName,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = SteppieTheme.typography.button,
                                maxLines = 2,
                            )
                            Text(
                                text = meta,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = SteppieTheme.typography.guardianCaption,
                            )
                        }
                        Text(
                            text = stringResource(R.string.guardian_routine_set_set_today),
                            color = MaterialTheme.colorScheme.primary,
                            style = SteppieTheme.typography.button,
                        )
                    }
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun IconTextButton(
    label: String,
    text: String,
    onClick: () -> Unit,
    danger: Boolean,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surface)
            .border(
                SteppieStroke.Divider,
                if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                CircleShape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (danger) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface,
            style = SteppieTheme.typography.guardianSection,
        )
    }
}

@Composable
private fun EmptyRoutineSetPanel(onOpenNewRoutineSet: () -> Unit) {
    GuardianPanel(
        title = stringResource(R.string.guardian_empty_routine_set_title),
        body = stringResource(R.string.guardian_empty_routine_set_body),
    ) {
        SteppieButton(
            label = stringResource(R.string.guardian_menu_create_routine_set),
            onClick = onOpenNewRoutineSet,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun GuardianRoutineSplitScreen(
    state: GuardianModeUiState,
    onNavigateBack: () -> Unit,
    onOpenRoutineSetCreate: () -> Unit,
    onOpenNewRoutineEditor: () -> Unit,
    onOpenRoutineEditor: (String) -> Unit,
    onToggleRoutineSetListEditing: () -> Unit,
    onSelectRoutineSet: (String) -> Unit,
    onSetRoutineSetForToday: (String) -> Unit,
    onRoutineSetStartTimeChange: (String, String) -> Unit,
    onRequestEditRoutineSetName: (String) -> Unit,
    onRequestDeleteRoutineSet: (String) -> Unit,
    onRequestDelete: (String) -> Unit,
    onMoveRoutine: (String, Int) -> Unit,
    onShowOutOfScopeNotice: () -> Unit,
) {
    Row(Modifier.fillMaxSize().testTag("guardian_split")) {
        Box(
            modifier = Modifier
                .width(SteppieLayout.SplitListWidth)
                .fillMaxHeight(),
        ) {
            GuardianRoutineEditScreen(
                state = state,
                onNavigateBack = onNavigateBack,
                onOpenRoutineSetCreate = onOpenRoutineSetCreate,
                onOpenNewRoutineEditor = onOpenNewRoutineEditor,
                onOpenRoutineEditor = onOpenRoutineEditor,
                onToggleRoutineSetListEditing = onToggleRoutineSetListEditing,
                onSelectRoutineSet = onSelectRoutineSet,
                onSetRoutineSetForToday = onSetRoutineSetForToday,
                onRoutineSetStartTimeChange = onRoutineSetStartTimeChange,
                onRequestEditRoutineSetName = onRequestEditRoutineSetName,
                onRequestDeleteRoutineSet = onRequestDeleteRoutineSet,
                onRequestDelete = onRequestDelete,
                onMoveRoutine = onMoveRoutine,
                onShowOutOfScopeNotice = onShowOutOfScopeNotice,
            )
        }
        Box(
            modifier = Modifier
                .width(SteppieStroke.Divider)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outline),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(SteppieSpacing.TwoExtraLarge),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.guardian_tablet_edit_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = SteppieTheme.typography.guardianTitle,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EditableRoutineRow(
    routine: Routine,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxRevealPx = with(LocalDensity.current) { 76.dp.toPx() }
    var swipeOffsetPx by remember(routine.id) { mutableFloatStateOf(0f) }
    var reorderOffsetPx by remember(routine.id) { mutableFloatStateOf(0f) }
    var isReordering by remember(routine.id) { mutableStateOf(false) }
    val routineTitle = routine.title.resolve(null, Locale.getDefault().toLanguageTag())
    val routineMeta = routine.scheduledTime?.toString() ?: stringResource(R.string.guardian_time_none)
    val rowDescription = stringResource(R.string.a11y_routine_edit_row, routineTitle, routineMeta)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 92.dp)
            .zIndex(if (isReordering) 1f else 0f),
    ) {
        if (swipeOffsetPx < -1f) {
            SwipeDeleteAction(
                label = stringResource(R.string.a11y_item_action, routineTitle, stringResource(R.string.action_delete)),
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = SteppieSpacing.ExtraSmall),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 92.dp)
                .graphicsLayer {
                    translationX = swipeOffsetPx
                    translationY = reorderOffsetPx
                    scaleX = if (isReordering) 1.02f else 1f
                    scaleY = if (isReordering) 1.02f else 1f
                    shadowElevation = if (isReordering) 12f else 0f
                }
                .clip(RoundedCornerShape(SteppieCornerRadius.Card))
                .background(MaterialTheme.colorScheme.surface)
                .border(SteppieStroke.Divider, MaterialTheme.colorScheme.outline, RoundedCornerShape(SteppieCornerRadius.Card))
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        swipeOffsetPx = (swipeOffsetPx + delta).coerceIn(-maxRevealPx, 0f)
                    },
                    onDragStopped = {
                        swipeOffsetPx = if (swipeOffsetPx <= -maxRevealPx / 2f) -maxRevealPx else 0f
                    },
                )
                .semantics {
                    contentDescription = rowDescription
                }
                .clickable(role = Role.Button, onClick = onClick)
                .padding(SteppieSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
        ) {
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colorForToken(routine.colorToken)),
            )
            Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                RoutineIcon(routine.icon, focus = false, modifier = Modifier.size(48.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall)) {
                Text(
                    text = routineTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = SteppieTheme.typography.button,
                    maxLines = 2,
                )
                Text(
                    text = routineMeta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = SteppieTheme.typography.guardianCaption,
                )
            }
            DragHandle(
                onMove = onMove,
                onDragStateChange = { dragging ->
                    isReordering = dragging
                    if (dragging) {
                        swipeOffsetPx = 0f
                    }
                },
                onDragOffsetChange = { offset ->
                    reorderOffsetPx = offset
                },
                contentDescription = stringResource(
                    R.string.a11y_item_action,
                    routineTitle,
                    stringResource(R.string.a11y_reorder_routine),
                ),
                modifier = Modifier.testTag("routine_drag_${routine.id}"),
            )
        }
    }
}

@Composable
private fun SwipeDeleteAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visualLabel = stringResource(R.string.action_delete)
    Column(
        modifier = modifier
            .width(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
                .clearAndSetSemantics {
                    contentDescription = label
                    role = Role.Button
                    semanticOnClick {
                        onClick()
                        true
                    }
                }
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_guardian_delete_trash),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(SteppieSpacing.TwoExtraSmall))
        Text(
            text = visualLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = SteppieTheme.typography.guardianCaption,
        )
    }
}

@Composable
private fun DragHandle(
    onMove: (Int) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    onDragOffsetChange: (Float) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val thresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(RoundedCornerShape(SteppieCornerRadius.Control))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clearAndSetSemantics { this.contentDescription = contentDescription }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    dragDistance += delta
                    onDragOffsetChange(dragDistance.coerceIn(-thresholdPx, thresholdPx))
                    when {
                        dragDistance <= -thresholdPx -> {
                            onMove(-1)
                            dragDistance += thresholdPx
                            onDragOffsetChange(dragDistance.coerceIn(-thresholdPx, thresholdPx))
                        }
                        dragDistance >= thresholdPx -> {
                            onMove(1)
                            dragDistance -= thresholdPx
                            onDragOffsetChange(dragDistance.coerceIn(-thresholdPx, thresholdPx))
                        }
                    }
                },
                onDragStarted = {
                    dragDistance = 0f
                    onDragStateChange(true)
                    onDragOffsetChange(0f)
                },
                onDragStopped = {
                    dragDistance = 0f
                    onDragOffsetChange(0f)
                    onDragStateChange(false)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text("☰", color = MaterialTheme.colorScheme.onSurfaceVariant, style = SteppieTheme.typography.guardianSection)
    }
}
