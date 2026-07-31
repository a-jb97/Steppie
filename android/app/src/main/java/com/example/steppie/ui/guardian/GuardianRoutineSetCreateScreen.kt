package com.example.steppie.ui.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.steppie.R
import com.example.steppie.ui.child.RoutineIcon
import com.example.steppie.ui.components.SteppieButton
import com.example.steppie.ui.components.SteppieButtonState
import com.example.steppie.ui.components.SteppieButtonStyle
import com.example.steppie.ui.theme.SteppieCornerRadius
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.theme.SteppieTheme

@Composable
internal fun GuardianRoutineSetCreateScreen(
    state: GuardianModeUiState,
    useSplitLayout: Boolean,
    onNavigateBack: () -> Unit,
    onOpenHome: () -> Unit,
    onRoutineSetNameChange: (String) -> Unit,
    onRoutineSetStepTitleChange: (String) -> Unit,
    onRoutineSetStepIconChange: (String) -> Unit,
    onRoutineSetStepPhotoPick: () -> Unit,
    onRoutineSetStepCameraCapture: () -> Unit,
    onRoutineSetStepPhotoRemove: () -> Unit,
    onRoutineSetStepColorChange: (String) -> Unit,
    onRoutineSetStepScheduledTimeChange: (String) -> Unit,
    onAddRoutineSetStep: () -> Unit,
    onEditRoutineSetStep: (Int) -> Unit,
    onRemoveRoutineSetStep: (Int) -> Unit,
    onSaveRoutineSetDraft: () -> Unit,
) {
    val draft = state.routineSetDraft ?: return
    GuardianScaffold(
        title = stringResource(R.string.guardian_routine_set_create_title),
        subtitle = stringResource(R.string.guardian_routine_set_create_subtitle),
        onBack = onNavigateBack,
        bottom = {
            Row(horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium)) {
                SteppieButton(
                    label = stringResource(R.string.action_cancel),
                    onClick = onOpenHome,
                    modifier = Modifier.weight(1f),
                    style = SteppieButtonStyle.Secondary,
                )
                SteppieButton(
                    label = stringResource(R.string.action_save),
                    onClick = onSaveRoutineSetDraft,
                    modifier = Modifier.weight(1f),
                    state = if (draft.steps.isEmpty()) SteppieButtonState.Disabled else SteppieButtonState.Enabled,
                )
            }
        },
    ) {
        if (useSplitLayout) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("guardian_routine_set_create_split"),
                horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Large),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
                ) {
                    RoutineSetNameField(draft.name, onRoutineSetNameChange)
                    RoutineSetStepList(draft.steps, onEditRoutineSetStep, onRemoveRoutineSetStep)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
                ) {
                    RoutineSetStepEditor(
                        draft = draft.stepDraft,
                        onTitleChange = onRoutineSetStepTitleChange,
                        onIconChange = onRoutineSetStepIconChange,
                        onPhotoPick = onRoutineSetStepPhotoPick,
                        onCameraCapture = onRoutineSetStepCameraCapture,
                        onPhotoRemove = onRoutineSetStepPhotoRemove,
                        onColorChange = onRoutineSetStepColorChange,
                        onScheduledTimeChange = onRoutineSetStepScheduledTimeChange,
                        onAddStep = onAddRoutineSetStep,
                        isEditing = draft.editingStepIndex != null,
                    )
                    state.draftError?.let { ErrorMessage(it) }
                    WarningMessage(stringResource(R.string.guardian_unsaved_warning))
                }
            }
        } else {
            RoutineSetNameField(draft.name, onRoutineSetNameChange)
            RoutineSetStepList(draft.steps, onEditRoutineSetStep, onRemoveRoutineSetStep)
            RoutineSetStepEditor(
                draft = draft.stepDraft,
                onTitleChange = onRoutineSetStepTitleChange,
                onIconChange = onRoutineSetStepIconChange,
                onPhotoPick = onRoutineSetStepPhotoPick,
                onCameraCapture = onRoutineSetStepCameraCapture,
                onPhotoRemove = onRoutineSetStepPhotoRemove,
                onColorChange = onRoutineSetStepColorChange,
                onScheduledTimeChange = onRoutineSetStepScheduledTimeChange,
                onAddStep = onAddRoutineSetStep,
                isEditing = draft.editingStepIndex != null,
            )
            state.draftError?.let { ErrorMessage(it) }
            WarningMessage(stringResource(R.string.guardian_unsaved_warning))
        }
    }
}

@Composable
private fun RoutineSetNameField(
    name: String,
    onNameChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.guardian_field_routine_set_name)) },
        singleLine = true,
    )
}

@Composable
private fun RoutineSetStepEditor(
    draft: RoutineDraft,
    onTitleChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onPhotoPick: () -> Unit,
    onCameraCapture: () -> Unit,
    onPhotoRemove: () -> Unit,
    onColorChange: (String) -> Unit,
    onScheduledTimeChange: (String) -> Unit,
    onAddStep: () -> Unit,
    isEditing: Boolean,
) {
    GuardianPanel(title = stringResource(R.string.guardian_step_editor_title)) {
        OutlinedTextField(
            value = draft.title,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.guardian_field_step_title)) },
            singleLine = true,
        )
        IconPicker(
            selectedIcon = draft.icon,
            onSelected = onIconChange,
            onPhotoPick = onPhotoPick,
            onCameraCapture = onCameraCapture,
            onPhotoRemove = onPhotoRemove,
        )
        ColorPicker(selectedColorToken = draft.colorToken, onSelected = onColorChange)
        ScheduledTimePicker(
            scheduledTime = draft.scheduledTime,
            onScheduledTimeChange = onScheduledTimeChange,
        )
        SteppieButton(
            label = stringResource(if (isEditing) R.string.guardian_update_step else R.string.guardian_add_step),
            onClick = onAddStep,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RoutineSetStepList(
    steps: List<RoutineDraft>,
    onEditStep: (Int) -> Unit,
    onRemoveStep: (Int) -> Unit,
) {
    GuardianPanel(title = stringResource(R.string.guardian_step_list_title, steps.size)) {
        if (steps.isEmpty()) {
            Text(
                text = stringResource(R.string.guardian_step_list_empty),
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.guardianCaption,
            )
        } else {
            steps.forEachIndexed { index, step ->
                RoutineSetStepRow(
                    index = index,
                    step = step,
                    onEdit = { onEditStep(index) },
                    onRemove = { onRemoveStep(index) },
                )
            }
        }
    }
}

@Composable
private fun RoutineSetStepRow(
    index: Int,
    step: RoutineDraft,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(RoundedCornerShape(SteppieCornerRadius.Control))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(SteppieSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            RoutineIcon(step.icon, focus = false, modifier = Modifier.size(40.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.guardian_step_order_title, index + 1, step.title),
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.button,
                maxLines = 2,
            )
            Text(
                text = step.scheduledTime.ifBlank { stringResource(R.string.guardian_time_none) },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = SteppieTheme.typography.guardianCaption,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(SteppieSpacing.ExtraSmall)) {
            SteppieButton(
                label = stringResource(R.string.action_edit),
                onClick = onEdit,
                style = SteppieButtonStyle.Secondary,
            )
            SteppieButton(
                label = stringResource(R.string.action_delete),
                onClick = onRemove,
                style = SteppieButtonStyle.Danger,
            )
        }
    }
}
