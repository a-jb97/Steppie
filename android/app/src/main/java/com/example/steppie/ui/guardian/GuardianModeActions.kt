package com.example.steppie.ui.guardian

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.example.steppie.domain.model.FeedbackIntensity
import java.time.LocalDate

private val NoAction: () -> Unit = {}
private val NoBooleanAction: (Boolean) -> Unit = {}
private val NoDoubleAction: (Double) -> Unit = {}
private val NoIntAction: (Int) -> Unit = {}
private val NoStringAction: (String) -> Unit = {}

@Stable
interface GuardianModeActions {
    val onDigit: (Int) -> Unit
    val onDeletePinDigit: () -> Unit
    val onCloseToChild: () -> Unit
    val onInteraction: () -> Unit
    val onNavigateBack: () -> Unit
    val onOpenHome: () -> Unit
    val onShowOutOfScopeNotice: () -> Unit
    val onClearNotice: () -> Unit
}

@Immutable
data class GuardianModeActionCallbacks(
    override val onDigit: (Int) -> Unit = NoIntAction,
    override val onDeletePinDigit: () -> Unit = NoAction,
    override val onCloseToChild: () -> Unit = NoAction,
    override val onInteraction: () -> Unit = NoAction,
    override val onNavigateBack: () -> Unit = NoAction,
    override val onOpenHome: () -> Unit = NoAction,
    override val onShowOutOfScopeNotice: () -> Unit = NoAction,
    override val onClearNotice: () -> Unit = NoAction,
) : GuardianModeActions

@Stable
interface GuardianRoutineActions {
    val onOpenRoutineEdit: () -> Unit
    val onOpenNewRoutineEditor: () -> Unit
    val onOpenRoutineEditor: (String) -> Unit
    val onDraftTitleChange: (String) -> Unit
    val onDraftIconChange: (String) -> Unit
    val onDraftPhotoPick: () -> Unit
    val onDraftCameraCapture: () -> Unit
    val onDraftPhotoRemove: () -> Unit
    val onDraftColorChange: (String) -> Unit
    val onDraftScheduledTimeChange: (String) -> Unit
    val onSaveDraft: () -> Unit
    val onRequestDelete: (String) -> Unit
    val onCancelDelete: () -> Unit
    val onConfirmDelete: () -> Unit
    val onMoveRoutine: (String, Int) -> Unit
}

@Immutable
data class GuardianRoutineActionCallbacks(
    override val onOpenRoutineEdit: () -> Unit = NoAction,
    override val onOpenNewRoutineEditor: () -> Unit = NoAction,
    override val onOpenRoutineEditor: (String) -> Unit = NoStringAction,
    override val onDraftTitleChange: (String) -> Unit = NoStringAction,
    override val onDraftIconChange: (String) -> Unit = NoStringAction,
    override val onDraftPhotoPick: () -> Unit = NoAction,
    override val onDraftCameraCapture: () -> Unit = NoAction,
    override val onDraftPhotoRemove: () -> Unit = NoAction,
    override val onDraftColorChange: (String) -> Unit = NoStringAction,
    override val onDraftScheduledTimeChange: (String) -> Unit = NoStringAction,
    override val onSaveDraft: () -> Unit = NoAction,
    override val onRequestDelete: (String) -> Unit = NoStringAction,
    override val onCancelDelete: () -> Unit = NoAction,
    override val onConfirmDelete: () -> Unit = NoAction,
    override val onMoveRoutine: (String, Int) -> Unit = { _, _ -> },
) : GuardianRoutineActions

@Stable
interface GuardianRoutineSetActions {
    val onOpenRoutineSetCreate: () -> Unit
    val onToggleRoutineSetListEditing: () -> Unit
    val onSelectRoutineSet: (String) -> Unit
    val onSetRoutineSetForToday: (String) -> Unit
    val onRoutineSetStartTimeChange: (String, String) -> Unit
    val onDismissDailyRoutineSelectionPrompt: () -> Unit
    val onRequestEditRoutineSetName: (String) -> Unit
    val onEditingRoutineSetNameChange: (String) -> Unit
    val onCancelEditRoutineSetName: () -> Unit
    val onSaveEditingRoutineSetName: () -> Unit
    val onRoutineSetNameChange: (String) -> Unit
    val onRoutineSetStepTitleChange: (String) -> Unit
    val onRoutineSetStepIconChange: (String) -> Unit
    val onRoutineSetStepPhotoPick: () -> Unit
    val onRoutineSetStepCameraCapture: () -> Unit
    val onRoutineSetStepPhotoRemove: () -> Unit
    val onRoutineSetStepColorChange: (String) -> Unit
    val onRoutineSetStepScheduledTimeChange: (String) -> Unit
    val onAddRoutineSetStep: () -> Unit
    val onEditRoutineSetStep: (Int) -> Unit
    val onRemoveRoutineSetStep: (Int) -> Unit
    val onSaveRoutineSetDraft: () -> Unit
    val onRequestDeleteRoutineSet: (String) -> Unit
    val onConfirmDeleteRoutineSet: () -> Unit
}

@Immutable
data class GuardianRoutineSetActionCallbacks(
    override val onOpenRoutineSetCreate: () -> Unit = NoAction,
    override val onToggleRoutineSetListEditing: () -> Unit = NoAction,
    override val onSelectRoutineSet: (String) -> Unit = NoStringAction,
    override val onSetRoutineSetForToday: (String) -> Unit = NoStringAction,
    override val onRoutineSetStartTimeChange: (String, String) -> Unit = { _, _ -> },
    override val onDismissDailyRoutineSelectionPrompt: () -> Unit = NoAction,
    override val onRequestEditRoutineSetName: (String) -> Unit = NoStringAction,
    override val onEditingRoutineSetNameChange: (String) -> Unit = NoStringAction,
    override val onCancelEditRoutineSetName: () -> Unit = NoAction,
    override val onSaveEditingRoutineSetName: () -> Unit = NoAction,
    override val onRoutineSetNameChange: (String) -> Unit = NoStringAction,
    override val onRoutineSetStepTitleChange: (String) -> Unit = NoStringAction,
    override val onRoutineSetStepIconChange: (String) -> Unit = NoStringAction,
    override val onRoutineSetStepPhotoPick: () -> Unit = NoAction,
    override val onRoutineSetStepCameraCapture: () -> Unit = NoAction,
    override val onRoutineSetStepPhotoRemove: () -> Unit = NoAction,
    override val onRoutineSetStepColorChange: (String) -> Unit = NoStringAction,
    override val onRoutineSetStepScheduledTimeChange: (String) -> Unit = NoStringAction,
    override val onAddRoutineSetStep: () -> Unit = NoAction,
    override val onEditRoutineSetStep: (Int) -> Unit = NoIntAction,
    override val onRemoveRoutineSetStep: (Int) -> Unit = NoIntAction,
    override val onSaveRoutineSetDraft: () -> Unit = NoAction,
    override val onRequestDeleteRoutineSet: (String) -> Unit = NoStringAction,
    override val onConfirmDeleteRoutineSet: () -> Unit = NoAction,
) : GuardianRoutineSetActions

@Stable
interface GuardianTemplateActions {
    val onOpenTemplateSelect: () -> Unit
    val onOpenTemplateSelectFromHome: () -> Unit
    val onCloseTemplateSelect: () -> Unit
    val onPreviewTemplate: (RoutineTemplateId) -> Unit
    val onSaveTemplatePreview: () -> Unit
}

@Immutable
data class GuardianTemplateActionCallbacks(
    override val onOpenTemplateSelect: () -> Unit = NoAction,
    override val onOpenTemplateSelectFromHome: () -> Unit = NoAction,
    override val onCloseTemplateSelect: () -> Unit = NoAction,
    override val onPreviewTemplate: (RoutineTemplateId) -> Unit = {},
    override val onSaveTemplatePreview: () -> Unit = NoAction,
) : GuardianTemplateActions

@Stable
interface GuardianRecordActions {
    val onOpenRecords: () -> Unit
    val onOpenRecordsCalendar: () -> Unit
    val onSelectRecordsDate: (LocalDate) -> Unit
    val onSelectRecordsCalendarDate: (LocalDate) -> Unit
    val onMoveRecordsCalendarMonth: (Long) -> Unit
}

@Immutable
data class GuardianRecordActionCallbacks(
    override val onOpenRecords: () -> Unit = NoAction,
    override val onOpenRecordsCalendar: () -> Unit = NoAction,
    override val onSelectRecordsDate: (LocalDate) -> Unit = {},
    override val onSelectRecordsCalendarDate: (LocalDate) -> Unit = {},
    override val onMoveRecordsCalendarMonth: (Long) -> Unit = {},
) : GuardianRecordActions

@Stable
interface GuardianEnvironmentActions {
    val onOpenEnvironmentSettings: () -> Unit
    val onFeedbackIntensityChange: (FeedbackIntensity) -> Unit
    val onTtsEnabledChange: (Boolean) -> Unit
    val onTtsRateChange: (Double) -> Unit
    val onTtsVolumeChange: (Double) -> Unit
    val onSoundEnabledChange: (Boolean) -> Unit
    val onHapticEnabledChange: (Boolean) -> Unit
    val onNotificationLeadTimeChange: (Int, Boolean) -> Unit
    val onQuietHoursEnabledChange: (Boolean) -> Unit
    val onQuietHoursStartChange: (String) -> Unit
    val onQuietHoursEndChange: (String) -> Unit
    val onReplayTutorials: () -> Unit
}

@Immutable
data class GuardianEnvironmentActionCallbacks(
    override val onOpenEnvironmentSettings: () -> Unit = NoAction,
    override val onFeedbackIntensityChange: (FeedbackIntensity) -> Unit = {},
    override val onTtsEnabledChange: (Boolean) -> Unit = NoBooleanAction,
    override val onTtsRateChange: (Double) -> Unit = NoDoubleAction,
    override val onTtsVolumeChange: (Double) -> Unit = NoDoubleAction,
    override val onSoundEnabledChange: (Boolean) -> Unit = NoBooleanAction,
    override val onHapticEnabledChange: (Boolean) -> Unit = NoBooleanAction,
    override val onNotificationLeadTimeChange: (Int, Boolean) -> Unit = { _, _ -> },
    override val onQuietHoursEnabledChange: (Boolean) -> Unit = NoBooleanAction,
    override val onQuietHoursStartChange: (String) -> Unit = NoStringAction,
    override val onQuietHoursEndChange: (String) -> Unit = NoStringAction,
    override val onReplayTutorials: () -> Unit = NoAction,
) : GuardianEnvironmentActions

@Stable
interface GuardianSecurityActions {
    val onOpenSecurity: () -> Unit
    val onOpenBackupRestore: () -> Unit
    val onOpenPinChange: () -> Unit
    val onOpenRecoveryCode: () -> Unit
    val onOpenRecoveryPinReset: () -> Unit
    val onCreateBackupFile: () -> Unit
    val onOpenRestoreFile: () -> Unit
    val onRestorePinDigit: (Int) -> Unit
    val onDeleteRestorePinDigit: () -> Unit
    val onCancelRestore: () -> Unit
    val onRecoveryDigit: (Int) -> Unit
    val onDeleteRecoveryDigit: () -> Unit
    val onRecoveryCodeChange: (String) -> Unit
    val onConfirmRecoveryCode: () -> Unit
    val onCancelRecoveryPinReset: () -> Unit
    val onCloseRecoveryCode: () -> Unit
}

@Immutable
data class GuardianSecurityActionCallbacks(
    override val onOpenSecurity: () -> Unit = NoAction,
    override val onOpenBackupRestore: () -> Unit = NoAction,
    override val onOpenPinChange: () -> Unit = NoAction,
    override val onOpenRecoveryCode: () -> Unit = NoAction,
    override val onOpenRecoveryPinReset: () -> Unit = NoAction,
    override val onCreateBackupFile: () -> Unit = NoAction,
    override val onOpenRestoreFile: () -> Unit = NoAction,
    override val onRestorePinDigit: (Int) -> Unit = NoIntAction,
    override val onDeleteRestorePinDigit: () -> Unit = NoAction,
    override val onCancelRestore: () -> Unit = NoAction,
    override val onRecoveryDigit: (Int) -> Unit = NoIntAction,
    override val onDeleteRecoveryDigit: () -> Unit = NoAction,
    override val onRecoveryCodeChange: (String) -> Unit = NoStringAction,
    override val onConfirmRecoveryCode: () -> Unit = NoAction,
    override val onCancelRecoveryPinReset: () -> Unit = NoAction,
    override val onCloseRecoveryCode: () -> Unit = NoAction,
) : GuardianSecurityActions
