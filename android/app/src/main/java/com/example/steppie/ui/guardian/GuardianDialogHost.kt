package com.example.steppie.ui.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.steppie.R
import com.example.steppie.ui.components.SteppieButton
import com.example.steppie.ui.components.SteppieButtonStyle
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.theme.SteppieTheme

@Composable
internal fun GuardianDialogHost(
    state: GuardianModeUiState,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onConfirmDeleteRoutineSet: () -> Unit,
    onEditingRoutineSetNameChange: (String) -> Unit,
    onCancelEditRoutineSetName: () -> Unit,
    onSaveEditingRoutineSetName: () -> Unit,
    onRestorePinDigit: (Int) -> Unit,
    onDeleteRestorePinDigit: () -> Unit,
    onCancelRestore: () -> Unit,
    onClearNotice: () -> Unit,
) {
    if (state.pendingDeleteRoutineId != null) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text(stringResource(R.string.guardian_delete_title)) },
            text = { Text(stringResource(R.string.guardian_delete_body)) },
            confirmButton = {
                SteppieButton(
                    label = stringResource(R.string.action_delete),
                    onClick = onConfirmDelete,
                    style = SteppieButtonStyle.Danger,
                )
            },
            dismissButton = {
                SteppieButton(
                    label = stringResource(R.string.action_cancel),
                    onClick = onCancelDelete,
                    style = SteppieButtonStyle.Secondary,
                )
            },
        )
    }

    if (state.pendingDeleteRoutineSetId != null) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text(stringResource(R.string.guardian_delete_routine_set_title)) },
            text = { Text(stringResource(R.string.guardian_delete_routine_set_body)) },
            confirmButton = {
                SteppieButton(
                    label = stringResource(R.string.action_delete),
                    onClick = onConfirmDeleteRoutineSet,
                    style = SteppieButtonStyle.Danger,
                )
            },
            dismissButton = {
                SteppieButton(
                    label = stringResource(R.string.action_cancel),
                    onClick = onCancelDelete,
                    style = SteppieButtonStyle.Secondary,
                )
            },
        )
    }

    if (state.editingRoutineSetId != null) {
        AlertDialog(
            onDismissRequest = onCancelEditRoutineSetName,
            title = { Text(stringResource(R.string.guardian_routine_set_rename_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small)) {
                    OutlinedTextField(
                        value = state.editingRoutineSetName,
                        onValueChange = onEditingRoutineSetNameChange,
                        label = { Text(stringResource(R.string.guardian_field_routine_set_name)) },
                        singleLine = true,
                    )
                    state.draftError?.let { ErrorMessage(it) }
                }
            },
            confirmButton = {
                SteppieButton(
                    label = stringResource(R.string.action_save),
                    onClick = onSaveEditingRoutineSetName,
                )
            },
            dismissButton = {
                SteppieButton(
                    label = stringResource(R.string.action_cancel),
                    onClick = onCancelEditRoutineSetName,
                    style = SteppieButtonStyle.Secondary,
                )
            },
        )
    }

    if (state.pendingRestorePreview != null) {
        AlertDialog(
            onDismissRequest = onCancelRestore,
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.padding(horizontal = SteppieSpacing.ExtraSmall).widthIn(max = 560.dp).fillMaxWidth(),
            title = { Text(stringResource(R.string.guardian_restore_confirm_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
                ) {
                    Text(
                        text = stringResource(
                            R.string.guardian_restore_confirm_body,
                            pluralStringResource(
                                R.plurals.guardian_restore_routine_set_count,
                                state.pendingRestorePreview.routineSetCount,
                                state.pendingRestorePreview.routineSetCount,
                            ),
                            pluralStringResource(
                                R.plurals.guardian_restore_routine_count,
                                state.pendingRestorePreview.routineCount,
                                state.pendingRestorePreview.routineCount,
                            ),
                            pluralStringResource(
                                R.plurals.guardian_restore_log_count,
                                state.pendingRestorePreview.dailyLogCount,
                                state.pendingRestorePreview.dailyLogCount,
                            ),
                        ),
                    )
                    Text(
                        text = stringResource(R.string.guardian_restore_pin_prompt),
                        style = SteppieTheme.typography.guardianBody,
                    )
                    val restorePinProgressDescription = stringResource(
                        R.string.a11y_pin_progress,
                        state.restorePinDigits.length,
                    )
                    Row(
                        modifier = Modifier.semantics {
                            contentDescription = restorePinProgressDescription
                        },
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        repeat(4) { index ->
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index < state.restorePinDigits.length) {
                                            SteppieTheme.colors.progressComplete
                                        } else {
                                            SteppieTheme.colors.progressPending
                                        },
                                    ),
                            )
                        }
                    }
                    GuardianNumberKeypad(
                        onDigit = onRestorePinDigit,
                        onDelete = onDeleteRestorePinDigit,
                        hapticEnabled = state.appSettings.hapticEnabled,
                    )
                    state.backupError?.let { ErrorMessage(it) }
                }
            },
            confirmButton = {},
            dismissButton = {
                SteppieButton(
                    label = stringResource(R.string.action_cancel),
                    onClick = onCancelRestore,
                    style = SteppieButtonStyle.Secondary,
                )
            },
        )
    }

    state.notice?.let { notice ->
        AlertDialog(
            onDismissRequest = onClearNotice,
            title = { Text(stringResource(R.string.guardian_notice_title)) },
            text = { Text(notice) },
            confirmButton = {
                SteppieButton(label = stringResource(R.string.action_ok), onClick = onClearNotice)
            },
        )
    }
}
