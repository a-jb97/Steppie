package com.example.steppie.ui.guardian

import androidx.annotation.DrawableRes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick as semanticOnClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.steppie.R
import com.example.steppie.domain.model.FeedbackIntensity
import com.example.steppie.domain.model.IconRef
import com.example.steppie.ui.components.SteppieButton
import com.example.steppie.ui.components.SteppieButtonStyle
import com.example.steppie.ui.theme.SteppieCornerRadius
import com.example.steppie.ui.theme.SteppieLayout
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.theme.SteppieStroke
import com.example.steppie.ui.theme.SteppieTheme
import com.example.steppie.ui.tutorial.TutorialTarget
import com.example.steppie.ui.tutorial.tutorialAnchor
import java.time.LocalDate

private val GuardianSplitMinimumWidth = 905.dp

@Composable
fun GuardianModeScreen(
    state: GuardianModeUiState,
    modeActions: GuardianModeActions,
    routineActions: GuardianRoutineActions,
    routineSetActions: GuardianRoutineSetActions,
    templateActions: GuardianTemplateActions,
    recordActions: GuardianRecordActions,
    environmentActions: GuardianEnvironmentActions,
    securityActions: GuardianSecurityActions,
    modifier: Modifier = Modifier,
) {
    GuardianModeContent(
        state = state,
        onDigit = modeActions.onDigit,
        onDeletePinDigit = modeActions.onDeletePinDigit,
        onCloseToChild = modeActions.onCloseToChild,
        onInteraction = modeActions.onInteraction,
        onNavigateBack = modeActions.onNavigateBack,
        onOpenHome = modeActions.onOpenHome,
        onOpenRoutineEdit = routineActions.onOpenRoutineEdit,
        onOpenEnvironmentSettings = environmentActions.onOpenEnvironmentSettings,
        onOpenRecords = recordActions.onOpenRecords,
        onOpenRecordsCalendar = recordActions.onOpenRecordsCalendar,
        onOpenSecurity = securityActions.onOpenSecurity,
        onOpenBackupRestore = securityActions.onOpenBackupRestore,
        onOpenPinChange = securityActions.onOpenPinChange,
        onOpenRecoveryCode = securityActions.onOpenRecoveryCode,
        onOpenRecoveryPinReset = securityActions.onOpenRecoveryPinReset,
        onOpenRoutineSetCreate = routineSetActions.onOpenRoutineSetCreate,
        onOpenTemplateSelect = templateActions.onOpenTemplateSelect,
        onOpenTemplateSelectFromHome = templateActions.onOpenTemplateSelectFromHome,
        onCloseTemplateSelect = templateActions.onCloseTemplateSelect,
        onPreviewTemplate = templateActions.onPreviewTemplate,
        onSaveTemplatePreview = templateActions.onSaveTemplatePreview,
        onOpenNewRoutineEditor = routineActions.onOpenNewRoutineEditor,
        onOpenRoutineEditor = routineActions.onOpenRoutineEditor,
        onToggleRoutineSetListEditing = routineSetActions.onToggleRoutineSetListEditing,
        onSelectRoutineSet = routineSetActions.onSelectRoutineSet,
        onSetRoutineSetForToday = routineSetActions.onSetRoutineSetForToday,
        onRoutineSetStartTimeChange = routineSetActions.onRoutineSetStartTimeChange,
        onDismissDailyRoutineSelectionPrompt = routineSetActions.onDismissDailyRoutineSelectionPrompt,
        onRequestEditRoutineSetName = routineSetActions.onRequestEditRoutineSetName,
        onEditingRoutineSetNameChange = routineSetActions.onEditingRoutineSetNameChange,
        onCancelEditRoutineSetName = routineSetActions.onCancelEditRoutineSetName,
        onSaveEditingRoutineSetName = routineSetActions.onSaveEditingRoutineSetName,
        onDraftTitleChange = routineActions.onDraftTitleChange,
        onDraftIconChange = routineActions.onDraftIconChange,
        onDraftPhotoPick = routineActions.onDraftPhotoPick,
        onDraftCameraCapture = routineActions.onDraftCameraCapture,
        onDraftPhotoRemove = routineActions.onDraftPhotoRemove,
        onDraftColorChange = routineActions.onDraftColorChange,
        onDraftScheduledTimeChange = routineActions.onDraftScheduledTimeChange,
        onSaveDraft = routineActions.onSaveDraft,
        onRoutineSetNameChange = routineSetActions.onRoutineSetNameChange,
        onRoutineSetStepTitleChange = routineSetActions.onRoutineSetStepTitleChange,
        onRoutineSetStepIconChange = routineSetActions.onRoutineSetStepIconChange,
        onRoutineSetStepPhotoPick = routineSetActions.onRoutineSetStepPhotoPick,
        onRoutineSetStepCameraCapture = routineSetActions.onRoutineSetStepCameraCapture,
        onRoutineSetStepPhotoRemove = routineSetActions.onRoutineSetStepPhotoRemove,
        onRoutineSetStepColorChange = routineSetActions.onRoutineSetStepColorChange,
        onRoutineSetStepScheduledTimeChange = routineSetActions.onRoutineSetStepScheduledTimeChange,
        onAddRoutineSetStep = routineSetActions.onAddRoutineSetStep,
        onEditRoutineSetStep = routineSetActions.onEditRoutineSetStep,
        onRemoveRoutineSetStep = routineSetActions.onRemoveRoutineSetStep,
        onSaveRoutineSetDraft = routineSetActions.onSaveRoutineSetDraft,
        onRequestDelete = routineActions.onRequestDelete,
        onRequestDeleteRoutineSet = routineSetActions.onRequestDeleteRoutineSet,
        onCancelDelete = routineActions.onCancelDelete,
        onConfirmDelete = routineActions.onConfirmDelete,
        onConfirmDeleteRoutineSet = routineSetActions.onConfirmDeleteRoutineSet,
        onMoveRoutine = routineActions.onMoveRoutine,
        onSelectRecordsDate = recordActions.onSelectRecordsDate,
        onSelectRecordsCalendarDate = recordActions.onSelectRecordsCalendarDate,
        onMoveRecordsCalendarMonth = recordActions.onMoveRecordsCalendarMonth,
        onShowOutOfScopeNotice = modeActions.onShowOutOfScopeNotice,
        onFeedbackIntensityChange = environmentActions.onFeedbackIntensityChange,
        onTtsEnabledChange = environmentActions.onTtsEnabledChange,
        onTtsRateChange = environmentActions.onTtsRateChange,
        onTtsVolumeChange = environmentActions.onTtsVolumeChange,
        onSoundEnabledChange = environmentActions.onSoundEnabledChange,
        onHapticEnabledChange = environmentActions.onHapticEnabledChange,
        onNotificationLeadTimeChange = environmentActions.onNotificationLeadTimeChange,
        onQuietHoursEnabledChange = environmentActions.onQuietHoursEnabledChange,
        onQuietHoursStartChange = environmentActions.onQuietHoursStartChange,
        onQuietHoursEndChange = environmentActions.onQuietHoursEndChange,
        onReplayTutorials = environmentActions.onReplayTutorials,
        onCreateBackupFile = securityActions.onCreateBackupFile,
        onOpenRestoreFile = securityActions.onOpenRestoreFile,
        onRestorePinDigit = securityActions.onRestorePinDigit,
        onDeleteRestorePinDigit = securityActions.onDeleteRestorePinDigit,
        onCancelRestore = securityActions.onCancelRestore,
        onRecoveryDigit = securityActions.onRecoveryDigit,
        onDeleteRecoveryDigit = securityActions.onDeleteRecoveryDigit,
        onRecoveryCodeChange = securityActions.onRecoveryCodeChange,
        onConfirmRecoveryCode = securityActions.onConfirmRecoveryCode,
        onCancelRecoveryPinReset = securityActions.onCancelRecoveryPinReset,
        onCloseRecoveryCode = securityActions.onCloseRecoveryCode,
        onClearNotice = modeActions.onClearNotice,
        modifier = modifier,
    )
}

@Composable
private fun GuardianModeContent(
    state: GuardianModeUiState,
    onDigit: (Int) -> Unit,
    onDeletePinDigit: () -> Unit,
    onCloseToChild: () -> Unit,
    onInteraction: () -> Unit,
    onNavigateBack: () -> Unit = {},
    onOpenHome: () -> Unit,
    onOpenRoutineEdit: () -> Unit,
    onOpenEnvironmentSettings: () -> Unit = {},
    onOpenRecords: () -> Unit = {},
    onOpenRecordsCalendar: () -> Unit = {},
    onOpenSecurity: () -> Unit,
    onOpenBackupRestore: () -> Unit = {},
    onOpenPinChange: () -> Unit,
    onOpenRecoveryCode: () -> Unit = {},
    onOpenRecoveryPinReset: () -> Unit = {},
    onOpenRoutineSetCreate: () -> Unit,
    onOpenTemplateSelect: () -> Unit = {},
    onOpenTemplateSelectFromHome: () -> Unit = {},
    onCloseTemplateSelect: () -> Unit = {},
    onPreviewTemplate: (RoutineTemplateId) -> Unit = {},
    onSaveTemplatePreview: () -> Unit = {},
    onOpenNewRoutineEditor: () -> Unit,
    onOpenRoutineEditor: (String) -> Unit,
    onToggleRoutineSetListEditing: () -> Unit,
    onSelectRoutineSet: (String) -> Unit,
    onSetRoutineSetForToday: (String) -> Unit = {},
    onRoutineSetStartTimeChange: (String, String) -> Unit = { _, _ -> },
    onDismissDailyRoutineSelectionPrompt: () -> Unit = {},
    onRequestEditRoutineSetName: (String) -> Unit,
    onEditingRoutineSetNameChange: (String) -> Unit,
    onCancelEditRoutineSetName: () -> Unit,
    onSaveEditingRoutineSetName: () -> Unit,
    onDraftTitleChange: (String) -> Unit,
    onDraftIconChange: (String) -> Unit,
    onDraftPhotoPick: () -> Unit = {},
    onDraftCameraCapture: () -> Unit = {},
    onDraftPhotoRemove: () -> Unit = {},
    onDraftColorChange: (String) -> Unit,
    onDraftScheduledTimeChange: (String) -> Unit,
    onSaveDraft: () -> Unit,
    onRoutineSetNameChange: (String) -> Unit,
    onRoutineSetStepTitleChange: (String) -> Unit,
    onRoutineSetStepIconChange: (String) -> Unit,
    onRoutineSetStepPhotoPick: () -> Unit = {},
    onRoutineSetStepCameraCapture: () -> Unit = {},
    onRoutineSetStepPhotoRemove: () -> Unit = {},
    onRoutineSetStepColorChange: (String) -> Unit,
    onRoutineSetStepScheduledTimeChange: (String) -> Unit,
    onAddRoutineSetStep: () -> Unit,
    onEditRoutineSetStep: (Int) -> Unit,
    onRemoveRoutineSetStep: (Int) -> Unit,
    onSaveRoutineSetDraft: () -> Unit,
    onRequestDelete: (String) -> Unit,
    onRequestDeleteRoutineSet: (String) -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onConfirmDeleteRoutineSet: () -> Unit,
    onMoveRoutine: (String, Int) -> Unit,
    onSelectRecordsDate: (LocalDate) -> Unit = {},
    onSelectRecordsCalendarDate: (LocalDate) -> Unit = {},
    onMoveRecordsCalendarMonth: (Long) -> Unit = {},
    onShowOutOfScopeNotice: () -> Unit,
    onFeedbackIntensityChange: (FeedbackIntensity) -> Unit = {},
    onTtsEnabledChange: (Boolean) -> Unit = {},
    onTtsRateChange: (Double) -> Unit = {},
    onTtsVolumeChange: (Double) -> Unit = {},
    onSoundEnabledChange: (Boolean) -> Unit = {},
    onHapticEnabledChange: (Boolean) -> Unit = {},
    onNotificationLeadTimeChange: (Int, Boolean) -> Unit = { _, _ -> },
    onQuietHoursEnabledChange: (Boolean) -> Unit = {},
    onQuietHoursStartChange: (String) -> Unit = {},
    onQuietHoursEndChange: (String) -> Unit = {},
    onReplayTutorials: () -> Unit = {},
    onCreateBackupFile: () -> Unit = {},
    onOpenRestoreFile: () -> Unit = {},
    onRestorePinDigit: (Int) -> Unit = {},
    onDeleteRestorePinDigit: () -> Unit = {},
    onCancelRestore: () -> Unit = {},
    onRecoveryDigit: (Int) -> Unit = {},
    onDeleteRecoveryDigit: () -> Unit = {},
    onRecoveryCodeChange: (String) -> Unit = {},
    onConfirmRecoveryCode: () -> Unit = {},
    onCancelRecoveryPinReset: () -> Unit = {},
    onCloseRecoveryCode: () -> Unit = {},
    onClearNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .tutorialAnchor(TutorialTarget.GuardianContent)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .pointerInput(state.isActive, state.isAuthenticated) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        onInteraction()
                    }
                }
            },
    ) {
        BackHandler(
            enabled = true,
            onBack = {
                when (state.destination) {
                    GuardianDestination.Home -> onCloseToChild()
                    GuardianDestination.Pin -> {
                        if (state.pinMode == GuardianPinMode.Enter || state.destinationBackStack.isEmpty()) {
                            onCloseToChild()
                        } else {
                            onNavigateBack()
                        }
                    }
                    else -> onNavigateBack()
                }
            },
        )

        when (state.destination) {
            GuardianDestination.Pin -> GuardianPinScreen(
                state = state,
                onDigit = onDigit,
                onDeletePinDigit = onDeletePinDigit,
                onOpenRecoveryPinReset = onOpenRecoveryPinReset,
            )
            GuardianDestination.Home -> GuardianHomeScreen(
                onCloseToChild = onCloseToChild,
                onOpenRoutineSetCreate = onOpenRoutineSetCreate,
                onOpenRoutineEdit = onOpenRoutineEdit,
                onOpenTemplateSelect = onOpenTemplateSelectFromHome,
                onOpenEnvironmentSettings = onOpenEnvironmentSettings,
                onOpenRecords = onOpenRecords,
                onOpenSecurity = onOpenSecurity,
                onShowOutOfScopeNotice = onShowOutOfScopeNotice,
            )
            GuardianDestination.RoutineEdit -> {
                if (maxWidth >= GuardianSplitMinimumWidth && maxWidth > maxHeight) {
                    GuardianRoutineSplitScreen(
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
                } else {
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
            }
            GuardianDestination.TemplateSelect -> GuardianTemplateSelectScreen(
                state = state,
                onBack = onCloseTemplateSelect,
                onPreviewTemplate = onPreviewTemplate,
                onSaveTemplatePreview = onSaveTemplatePreview,
                useWideLayout = maxWidth >= GuardianSplitMinimumWidth && maxWidth > maxHeight,
            )
            GuardianDestination.CardEdit -> GuardianCardEditScreen(
                state = state,
                onOpenRoutineEdit = onOpenRoutineEdit,
                onRequestDelete = onRequestDelete,
                onDraftTitleChange = onDraftTitleChange,
                onDraftIconChange = onDraftIconChange,
                onDraftPhotoPick = onDraftPhotoPick,
                onDraftCameraCapture = onDraftCameraCapture,
                onDraftPhotoRemove = onDraftPhotoRemove,
                onDraftColorChange = onDraftColorChange,
                onDraftScheduledTimeChange = onDraftScheduledTimeChange,
                onSaveDraft = onSaveDraft,
            )
            GuardianDestination.RoutineSetCreate -> GuardianRoutineSetCreateScreen(
                state = state,
                useSplitLayout = maxWidth >= GuardianSplitMinimumWidth && maxWidth > maxHeight,
                onNavigateBack = onNavigateBack,
                onOpenHome = onOpenHome,
                onRoutineSetNameChange = onRoutineSetNameChange,
                onRoutineSetStepTitleChange = onRoutineSetStepTitleChange,
                onRoutineSetStepIconChange = onRoutineSetStepIconChange,
                onRoutineSetStepPhotoPick = onRoutineSetStepPhotoPick,
                onRoutineSetStepCameraCapture = onRoutineSetStepCameraCapture,
                onRoutineSetStepPhotoRemove = onRoutineSetStepPhotoRemove,
                onRoutineSetStepColorChange = onRoutineSetStepColorChange,
                onRoutineSetStepScheduledTimeChange = onRoutineSetStepScheduledTimeChange,
                onAddRoutineSetStep = onAddRoutineSetStep,
                onEditRoutineSetStep = onEditRoutineSetStep,
                onRemoveRoutineSetStep = onRemoveRoutineSetStep,
                onSaveRoutineSetDraft = onSaveRoutineSetDraft,
            )
            GuardianDestination.EnvironmentSettings -> GuardianEnvironmentSettingsScreen(
                settings = state.appSettings,
                useWideLayout = maxWidth >= GuardianSplitMinimumWidth && maxWidth > maxHeight,
                onNavigateBack = onNavigateBack,
                onFeedbackIntensityChange = onFeedbackIntensityChange,
                onTtsEnabledChange = onTtsEnabledChange,
                onTtsRateChange = onTtsRateChange,
                onTtsVolumeChange = onTtsVolumeChange,
                onSoundEnabledChange = onSoundEnabledChange,
                onHapticEnabledChange = onHapticEnabledChange,
                onNotificationLeadTimeChange = onNotificationLeadTimeChange,
                onQuietHoursEnabledChange = onQuietHoursEnabledChange,
                onQuietHoursStartChange = onQuietHoursStartChange,
                onQuietHoursEndChange = onQuietHoursEndChange,
                onReplayTutorials = onReplayTutorials,
            )
            GuardianDestination.Records -> GuardianRecordsScreen(
                state = state,
                useWideLayout = maxWidth >= GuardianSplitMinimumWidth && maxWidth > maxHeight,
                onNavigateBack = onNavigateBack,
                onSelectRecordsDate = onSelectRecordsDate,
                onOpenRecordsCalendar = onOpenRecordsCalendar,
            )
            GuardianDestination.RecordsCalendar -> GuardianRecordsCalendarScreen(
                state = state,
                useWideLayout = maxWidth >= GuardianSplitMinimumWidth && maxWidth > maxHeight,
                onNavigateBack = onNavigateBack,
                onSelectRecordsCalendarDate = onSelectRecordsCalendarDate,
                onMoveRecordsCalendarMonth = onMoveRecordsCalendarMonth,
            )
            GuardianDestination.Security -> GuardianSecurityScreen(
                onNavigateBack = onNavigateBack,
                onOpenPinChange = onOpenPinChange,
                onOpenRecoveryCode = onOpenRecoveryCode,
                onOpenBackupRestore = onOpenBackupRestore,
            )
            GuardianDestination.RecoveryCode -> GuardianRecoveryCodeScreen(
                state = state,
                onCancelRecoveryPinReset = onCancelRecoveryPinReset,
                onRecoveryCodeChange = onRecoveryCodeChange,
                onConfirmRecoveryCode = onConfirmRecoveryCode,
                onCloseRecoveryCode = onCloseRecoveryCode,
            )
            GuardianDestination.BackupRestore -> GuardianBackupRestoreScreen(
                state = state,
                onOpenSecurity = onOpenSecurity,
                onCreateBackupFile = onCreateBackupFile,
                onOpenRestoreFile = onOpenRestoreFile,
            )
        }

        if (state.recoveryStep == GuardianRecoveryStep.ShowCode && state.recoveryCodeToShow != null) {
            GuardianRecoveryCodeSheet(
                recoveryCode = state.recoveryCodeToShow,
                onCloseRecoveryCode = onCloseRecoveryCode,
            )
        }
        if (state.recoveryStep == GuardianRecoveryStep.EnterCodeForPinReset) {
            GuardianRecoveryCodeInputSheet(
                recoveryDigits = state.recoveryDigits,
                recoveryError = state.recoveryError,
                onRecoveryCodeChange = onRecoveryCodeChange,
                onConfirmRecoveryCode = onConfirmRecoveryCode,
                onCancelRecoveryPinReset = onCancelRecoveryPinReset,
            )
        }
        if (state.showDailyRoutineSelectionPrompt) {
            DailyRoutineSelectionDialog(
                routineSets = state.routineSets,
                onSelect = onSetRoutineSetForToday,
                onDismiss = onDismissDailyRoutineSelectionPrompt,
            )
        }
    }

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
            title = { Text(stringResource(R.string.guardian_restore_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small)) {
                    Text(
                        text = stringResource(
                            R.string.guardian_restore_confirm_body,
                            state.pendingRestorePreview.routineSetCount,
                            state.pendingRestorePreview.routineCount,
                            state.pendingRestorePreview.dailyLogCount,
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
                    CompactPinKeypad(onDigit = onRestorePinDigit, onDelete = onDeleteRestorePinDigit)
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

@Composable
private fun GuardianPinScreen(
    state: GuardianModeUiState,
    onDigit: (Int) -> Unit,
    onDeletePinDigit: () -> Unit,
    onOpenRecoveryPinReset: () -> Unit,
) {
    val pinProgressDescription = stringResource(R.string.a11y_pin_progress, state.pinDigits.length)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("guardian_pin")
            .verticalScroll(rememberScrollState())
            .padding(
                start = SteppieLayout.ChildScreenPadding,
                top = SteppieLayout.GuardianScreenPadding,
                end = SteppieLayout.ChildScreenPadding,
                bottom = SteppieLayout.ChildScreenPadding,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GuardianTopBar(
            title = when (state.pinMode) {
                GuardianPinMode.Enter -> stringResource(R.string.guardian_pin_title)
                GuardianPinMode.Setup -> stringResource(R.string.guardian_pin_setup_title)
                GuardianPinMode.SetupConfirm -> stringResource(R.string.guardian_pin_setup_confirm_title)
                GuardianPinMode.ChangeCurrent -> stringResource(R.string.guardian_pin_change_current_title)
                GuardianPinMode.ChangeNew -> stringResource(R.string.guardian_pin_change_new_title)
                GuardianPinMode.RecoveryRegenerateConfirm -> stringResource(R.string.guardian_recovery_confirm_pin_title)
                GuardianPinMode.RecoveryResetNew -> stringResource(R.string.guardian_recovery_new_pin_title)
            },
            subtitle = when (state.pinMode) {
                GuardianPinMode.Enter -> stringResource(R.string.guardian_pin_subtitle)
                GuardianPinMode.Setup -> stringResource(R.string.guardian_pin_setup_subtitle)
                GuardianPinMode.SetupConfirm -> stringResource(R.string.guardian_pin_setup_confirm_subtitle)
                GuardianPinMode.ChangeCurrent -> stringResource(R.string.guardian_pin_change_current_subtitle)
                GuardianPinMode.ChangeNew -> stringResource(R.string.guardian_pin_change_new_subtitle)
                GuardianPinMode.RecoveryRegenerateConfirm -> stringResource(R.string.guardian_recovery_confirm_pin_subtitle)
                GuardianPinMode.RecoveryResetNew -> stringResource(R.string.guardian_recovery_new_pin_subtitle)
            },
        )
        Spacer(Modifier.height(SteppieSpacing.Large))
        Row(
            modifier = Modifier.semantics {
                contentDescription = pinProgressDescription
            },
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < state.pinDigits.length) {
                                SteppieTheme.colors.progressComplete
                            } else {
                                SteppieTheme.colors.progressPending
                            },
                        ),
                )
            }
        }
        Spacer(Modifier.height(SteppieSpacing.Large))
        Box(Modifier.tutorialAnchor(TutorialTarget.GuardianMain)) {
            PinKeypad(onDigit = onDigit, onDelete = onDeletePinDigit)
        }
        if (state.pinError != null) {
            Spacer(Modifier.height(28.dp))
            ErrorMessage(state.pinError)
        }
        if (state.pinMode == GuardianPinMode.Enter) {
            Spacer(Modifier.height(SteppieSpacing.Small))
            Text(
                text = stringResource(R.string.guardian_recovery_reset_pin_action),
                modifier = Modifier
                    .heightIn(min = SteppieLayout.GuardianMinimumTouchTarget)
                    .clickable(role = Role.Button, onClick = onOpenRecoveryPinReset)
                    .padding(horizontal = SteppieSpacing.Small, vertical = SteppieSpacing.ExtraSmall),
                color = MaterialTheme.colorScheme.primary,
                style = SteppieTheme.typography.button,
            )
        }
    }
}

@Composable
private fun PinKeypad(onDigit: (Int) -> Unit, onDelete: () -> Unit) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫"))
    val deleteDescription = stringResource(R.string.a11y_pin_delete)
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { label ->
                    if (label.isBlank()) {
                        Spacer(Modifier.size(width = 90.dp, height = SteppieLayout.ChildMinimumTouchTarget))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(width = 90.dp, height = SteppieLayout.ChildMinimumTouchTarget)
                                .clip(RoundedCornerShape(SteppieCornerRadius.Card))
                                .background(MaterialTheme.colorScheme.surface)
                                .clearAndSetSemantics {
                                    contentDescription = if (label == "⌫") deleteDescription else label
                                    role = Role.Button
                                    semanticOnClick {
                                        if (label == "⌫") onDelete() else onDigit(label.toInt())
                                        true
                                    }
                                }
                                .clickable(role = Role.Button) {
                                    if (label == "⌫") onDelete() else onDigit(label.toInt())
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = SteppieTheme.typography.childCardTitle,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CompactPinKeypad(onDigit: (Int) -> Unit, onDelete: () -> Unit) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫"))
    val deleteDescription = stringResource(R.string.a11y_pin_delete)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SteppieSpacing.ExtraSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.ExtraSmall)) {
                row.forEach { label ->
                    if (label.isBlank()) {
                        Spacer(Modifier.size(width = 64.dp, height = 56.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(width = 64.dp, height = 56.dp)
                                .clip(RoundedCornerShape(SteppieCornerRadius.Control))
                                .background(MaterialTheme.colorScheme.surface)
                                .clearAndSetSemantics {
                                    contentDescription = if (label == "⌫") deleteDescription else label
                                    role = Role.Button
                                    semanticOnClick {
                                        if (label == "⌫") onDelete() else onDigit(label.toInt())
                                        true
                                    }
                                }
                                .clickable(role = Role.Button) {
                                    if (label == "⌫") onDelete() else onDigit(label.toInt())
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = SteppieTheme.typography.guardianSection,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuardianHomeScreen(
    onCloseToChild: () -> Unit,
    onOpenRoutineSetCreate: () -> Unit,
    onOpenRoutineEdit: () -> Unit,
    onOpenTemplateSelect: () -> Unit,
    onOpenEnvironmentSettings: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenSecurity: () -> Unit,
    onShowOutOfScopeNotice: () -> Unit,
) {
    GuardianScaffold(
        title = stringResource(R.string.guardian_home_title),
        subtitle = stringResource(R.string.guardian_home_subtitle),
        bottom = {
            SteppieButton(
                label = stringResource(R.string.guardian_done),
                onClick = onCloseToChild,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        GuardianMenuCard(R.drawable.ic_guardian_menu_routine_set_create, stringResource(R.string.guardian_menu_create_routine_set), stringResource(R.string.guardian_menu_create_routine_set_desc), onOpenRoutineSetCreate)
        GuardianMenuCard(R.drawable.ic_guardian_menu_template, stringResource(R.string.guardian_template_action), stringResource(R.string.guardian_template_home_desc), onOpenTemplateSelect)
        GuardianMenuCard(R.drawable.ic_guardian_menu_routine, stringResource(R.string.guardian_menu_routine), stringResource(R.string.guardian_menu_routine_desc), onOpenRoutineEdit)
        GuardianMenuCard(R.drawable.ic_guardian_menu_settings, stringResource(R.string.guardian_menu_feedback), stringResource(R.string.guardian_menu_feedback_desc), onOpenEnvironmentSettings)
        GuardianMenuCard(R.drawable.ic_guardian_menu_records, stringResource(R.string.guardian_menu_records), stringResource(R.string.guardian_menu_records_desc), onOpenRecords)
        GuardianMenuCard(R.drawable.ic_guardian_menu_security, stringResource(R.string.guardian_menu_security), stringResource(R.string.guardian_menu_security_desc), onOpenSecurity)
    }
}

@Composable
internal fun GuardianScaffold(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    topActionLabel: String? = null,
    @DrawableRes topActionIcon: Int? = null,
    topActionContentDescription: String? = null,
    onTopAction: (() -> Unit)? = null,
    bottom: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SteppieLayout.GuardianScreenPadding),
        verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium),
    ) {
        GuardianTopBar(
            title = title,
            subtitle = subtitle,
            onBack = onBack,
            topActionLabel = topActionLabel,
            topActionIcon = topActionIcon,
            topActionContentDescription = topActionContentDescription,
            onTopAction = onTopAction,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .tutorialAnchor(TutorialTarget.GuardianMain),
            verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
            content = content,
        )
        Spacer(Modifier.height(SteppieSpacing.Large))
        if (bottom != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .tutorialAnchor(TutorialTarget.GuardianBottom),
            ) { bottom() }
        }
    }
}

@Composable
internal fun GuardianTopBar(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    topActionLabel: String? = null,
    @DrawableRes topActionIcon: Int? = null,
    topActionContentDescription: String? = null,
    onTopAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SteppieSpacing.Medium, bottom = SteppieSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall),
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(SteppieLayout.GuardianMinimumTouchTarget),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.action_back),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = SteppieTheme.typography.guardianTitle,
            )
            if (topActionLabel != null && onTopAction != null) {
                Text(
                    text = topActionLabel,
                    modifier = Modifier
                        .heightIn(min = SteppieLayout.GuardianMinimumTouchTarget)
                        .clickable(role = Role.Button, onClick = onTopAction)
                        .padding(horizontal = SteppieSpacing.ExtraSmall),
                    color = MaterialTheme.colorScheme.primary,
                    style = SteppieTheme.typography.button,
                )
            } else if (topActionIcon != null && topActionContentDescription != null && onTopAction != null) {
                IconButton(
                    onClick = onTopAction,
                    modifier = Modifier.size(SteppieLayout.GuardianMinimumTouchTarget),
                ) {
                    Icon(
                        painter = painterResource(topActionIcon),
                        contentDescription = topActionContentDescription,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
        Text(subtitle, color = MaterialTheme.colorScheme.onSurface, style = SteppieTheme.typography.guardianCaption)
    }
}

@Composable
internal fun GuardianMenuCard(
    @DrawableRes icon: Int,
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    val description = stringResource(R.string.a11y_guardian_menu_card, title, body)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(SteppieCornerRadius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(SteppieStroke.Divider, MaterialTheme.colorScheme.outline, RoundedCornerShape(SteppieCornerRadius.Card))
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                semanticOnClick {
                    onClick()
                    true
                }
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(SteppieSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(SteppieCornerRadius.Control))
                .background(Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall),
        ) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = SteppieTheme.typography.guardianSection)
            Text(body, color = MaterialTheme.colorScheme.onSurface, style = SteppieTheme.typography.guardianCaption)
        }
    }
}

@Composable
internal fun GuardianPanel(
    title: String,
    body: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SteppieCornerRadius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(SteppieStroke.Divider, MaterialTheme.colorScheme.outline, RoundedCornerShape(SteppieCornerRadius.Card))
            .padding(SteppieSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(SteppieSpacing.ExtraSmall),
    ) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = SteppieTheme.typography.guardianSection)
        body?.let { Text(it, color = MaterialTheme.colorScheme.onSurface, style = SteppieTheme.typography.guardianCaption) }
        content()
    }
}

@Composable
internal fun ErrorMessage(message: String) {
    Text(
        text = "!  $message",
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SteppieCornerRadius.Control))
            .background(SteppieTheme.colors.cardRose)
            .border(SteppieStroke.Divider, MaterialTheme.colorScheme.error, RoundedCornerShape(SteppieCornerRadius.Control))
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(SteppieSpacing.Medium),
        color = MaterialTheme.colorScheme.error,
        style = SteppieTheme.typography.guardianCaption,
    )
}

@Composable
internal fun WarningMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SteppieCornerRadius.Control))
            .background(SteppieTheme.colors.cardLemon)
            .border(SteppieStroke.Divider, SteppieTheme.colors.warning, RoundedCornerShape(SteppieCornerRadius.Control))
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(SteppieSpacing.Medium),
        color = MaterialTheme.colorScheme.onSurface,
        style = SteppieTheme.typography.guardianCaption,
    )
}
