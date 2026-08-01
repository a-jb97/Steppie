package com.example.steppie.ui.guardian

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.steppie.domain.model.FeedbackIntensity
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

    GuardianDialogHost(
        state = state,
        onCancelDelete = onCancelDelete,
        onConfirmDelete = onConfirmDelete,
        onConfirmDeleteRoutineSet = onConfirmDeleteRoutineSet,
        onEditingRoutineSetNameChange = onEditingRoutineSetNameChange,
        onCancelEditRoutineSetName = onCancelEditRoutineSetName,
        onSaveEditingRoutineSetName = onSaveEditingRoutineSetName,
        onRestorePinDigit = onRestorePinDigit,
        onDeleteRestorePinDigit = onDeleteRestorePinDigit,
        onCancelRestore = onCancelRestore,
        onClearNotice = onClearNotice,
    )
}
