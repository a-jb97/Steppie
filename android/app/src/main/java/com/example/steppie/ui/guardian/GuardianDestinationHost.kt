package com.example.steppie.ui.guardian

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

@Composable
internal fun BoxScope.GuardianDestinationHost(
    state: GuardianModeUiState,
    useWideLayout: Boolean,
    modeActions: GuardianModeActions,
    routineActions: GuardianRoutineActions,
    routineSetActions: GuardianRoutineSetActions,
    templateActions: GuardianTemplateActions,
    recordActions: GuardianRecordActions,
    environmentActions: GuardianEnvironmentActions,
    securityActions: GuardianSecurityActions,
) {
    BackHandler(
        enabled = true,
        onBack = {
            when (state.destination) {
                GuardianDestination.Home -> modeActions.onCloseToChild()
                GuardianDestination.Pin -> {
                    if (state.pinMode == GuardianPinMode.Enter || state.destinationBackStack.isEmpty()) {
                        modeActions.onCloseToChild()
                    } else {
                        modeActions.onNavigateBack()
                    }
                }
                else -> modeActions.onNavigateBack()
            }
        },
    )

    when (state.destination) {
        GuardianDestination.Pin -> GuardianPinScreen(
            state = state,
            onDigit = modeActions.onDigit,
            onDeletePinDigit = modeActions.onDeletePinDigit,
            onOpenRecoveryPinReset = securityActions.onOpenRecoveryPinReset,
        )
        GuardianDestination.Home -> GuardianHomeScreen(
            onCloseToChild = modeActions.onCloseToChild,
            onOpenRoutineSetCreate = routineSetActions.onOpenRoutineSetCreate,
            onOpenRoutineEdit = routineActions.onOpenRoutineEdit,
            onOpenTemplateSelect = templateActions.onOpenTemplateSelectFromHome,
            onOpenEnvironmentSettings = environmentActions.onOpenEnvironmentSettings,
            onOpenRecords = recordActions.onOpenRecords,
            onOpenSecurity = securityActions.onOpenSecurity,
            onShowOutOfScopeNotice = modeActions.onShowOutOfScopeNotice,
        )
        GuardianDestination.RoutineEdit -> {
            if (useWideLayout) {
                GuardianRoutineSplitScreen(
                    state = state,
                    onNavigateBack = modeActions.onNavigateBack,
                    onOpenRoutineSetCreate = routineSetActions.onOpenRoutineSetCreate,
                    onOpenNewRoutineEditor = routineActions.onOpenNewRoutineEditor,
                    onOpenRoutineEditor = routineActions.onOpenRoutineEditor,
                    onToggleRoutineSetListEditing = routineSetActions.onToggleRoutineSetListEditing,
                    onSelectRoutineSet = routineSetActions.onSelectRoutineSet,
                    onSetRoutineSetForToday = routineSetActions.onSetRoutineSetForToday,
                    onRoutineSetStartTimeChange = routineSetActions.onRoutineSetStartTimeChange,
                    onRequestEditRoutineSetName = routineSetActions.onRequestEditRoutineSetName,
                    onRequestDeleteRoutineSet = routineSetActions.onRequestDeleteRoutineSet,
                    onRequestDelete = routineActions.onRequestDelete,
                    onMoveRoutine = routineActions.onMoveRoutine,
                    onShowOutOfScopeNotice = modeActions.onShowOutOfScopeNotice,
                )
            } else {
                GuardianRoutineEditScreen(
                    state = state,
                    onNavigateBack = modeActions.onNavigateBack,
                    onOpenRoutineSetCreate = routineSetActions.onOpenRoutineSetCreate,
                    onOpenNewRoutineEditor = routineActions.onOpenNewRoutineEditor,
                    onOpenRoutineEditor = routineActions.onOpenRoutineEditor,
                    onToggleRoutineSetListEditing = routineSetActions.onToggleRoutineSetListEditing,
                    onSelectRoutineSet = routineSetActions.onSelectRoutineSet,
                    onSetRoutineSetForToday = routineSetActions.onSetRoutineSetForToday,
                    onRoutineSetStartTimeChange = routineSetActions.onRoutineSetStartTimeChange,
                    onRequestEditRoutineSetName = routineSetActions.onRequestEditRoutineSetName,
                    onRequestDeleteRoutineSet = routineSetActions.onRequestDeleteRoutineSet,
                    onRequestDelete = routineActions.onRequestDelete,
                    onMoveRoutine = routineActions.onMoveRoutine,
                    onShowOutOfScopeNotice = modeActions.onShowOutOfScopeNotice,
                )
            }
        }
        GuardianDestination.TemplateSelect -> GuardianTemplateSelectScreen(
            state = state,
            onBack = templateActions.onCloseTemplateSelect,
            onPreviewTemplate = templateActions.onPreviewTemplate,
            onSaveTemplatePreview = templateActions.onSaveTemplatePreview,
            useWideLayout = useWideLayout,
        )
        GuardianDestination.CardEdit -> GuardianCardEditScreen(
            state = state,
            onOpenRoutineEdit = routineActions.onOpenRoutineEdit,
            onRequestDelete = routineActions.onRequestDelete,
            onDraftTitleChange = routineActions.onDraftTitleChange,
            onDraftIconChange = routineActions.onDraftIconChange,
            onDraftPhotoPick = routineActions.onDraftPhotoPick,
            onDraftCameraCapture = routineActions.onDraftCameraCapture,
            onDraftPhotoRemove = routineActions.onDraftPhotoRemove,
            onDraftColorChange = routineActions.onDraftColorChange,
            onDraftScheduledTimeChange = routineActions.onDraftScheduledTimeChange,
            onSaveDraft = routineActions.onSaveDraft,
        )
        GuardianDestination.RoutineSetCreate -> GuardianRoutineSetCreateScreen(
            state = state,
            useSplitLayout = useWideLayout,
            onNavigateBack = modeActions.onNavigateBack,
            onOpenHome = modeActions.onOpenHome,
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
        )
        GuardianDestination.EnvironmentSettings -> GuardianEnvironmentSettingsScreen(
            settings = state.appSettings,
            useWideLayout = useWideLayout,
            onNavigateBack = modeActions.onNavigateBack,
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
        )
        GuardianDestination.Records -> GuardianRecordsScreen(
            state = state,
            useWideLayout = useWideLayout,
            onNavigateBack = modeActions.onNavigateBack,
            onSelectRecordsDate = recordActions.onSelectRecordsDate,
            onOpenRecordsCalendar = recordActions.onOpenRecordsCalendar,
        )
        GuardianDestination.RecordsCalendar -> GuardianRecordsCalendarScreen(
            state = state,
            useWideLayout = useWideLayout,
            onNavigateBack = modeActions.onNavigateBack,
            onSelectRecordsCalendarDate = recordActions.onSelectRecordsCalendarDate,
            onMoveRecordsCalendarMonth = recordActions.onMoveRecordsCalendarMonth,
        )
        GuardianDestination.Security -> GuardianSecurityScreen(
            onNavigateBack = modeActions.onNavigateBack,
            onOpenPinChange = securityActions.onOpenPinChange,
            onOpenRecoveryCode = securityActions.onOpenRecoveryCode,
            onOpenBackupRestore = securityActions.onOpenBackupRestore,
        )
        GuardianDestination.RecoveryCode -> GuardianRecoveryCodeScreen(
            state = state,
            onCancelRecoveryPinReset = securityActions.onCancelRecoveryPinReset,
            onRecoveryCodeChange = securityActions.onRecoveryCodeChange,
            onConfirmRecoveryCode = securityActions.onConfirmRecoveryCode,
            onCloseRecoveryCode = securityActions.onCloseRecoveryCode,
        )
        GuardianDestination.BackupRestore -> GuardianBackupRestoreScreen(
            state = state,
            onOpenSecurity = securityActions.onOpenSecurity,
            onCreateBackupFile = securityActions.onCreateBackupFile,
            onOpenRestoreFile = securityActions.onOpenRestoreFile,
        )
    }

    if (state.recoveryStep == GuardianRecoveryStep.ShowCode && state.recoveryCodeToShow != null) {
        GuardianRecoveryCodeSheet(
            recoveryCode = state.recoveryCodeToShow,
            onCloseRecoveryCode = securityActions.onCloseRecoveryCode,
        )
    }
    if (state.recoveryStep == GuardianRecoveryStep.EnterCodeForPinReset) {
        GuardianRecoveryCodeInputSheet(
            recoveryDigits = state.recoveryDigits,
            recoveryError = state.recoveryError,
            onRecoveryCodeChange = securityActions.onRecoveryCodeChange,
            onConfirmRecoveryCode = securityActions.onConfirmRecoveryCode,
            onCancelRecoveryPinReset = securityActions.onCancelRecoveryPinReset,
        )
    }
    if (state.showDailyRoutineSelectionPrompt) {
        DailyRoutineSelectionDialog(
            routineSets = state.routineSets,
            onSelect = routineSetActions.onSetRoutineSetForToday,
            onDismiss = routineSetActions.onDismissDailyRoutineSelectionPrompt,
        )
    }
}
