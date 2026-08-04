package com.example.steppie.ui.guardian

import android.net.Uri
import com.example.steppie.data.backup.BackupImportPreview

internal object GuardianBackupReducer {
    fun open(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        destination = GuardianDestination.BackupRestore,
        destinationBackStack = state.backStackFor(GuardianDestination.BackupRestore),
        draft = null,
        routineSetDraft = null,
        selectedTemplate = null,
        templateReturnDestination = GuardianDestination.RoutineEdit,
        routineSetListEditing = false,
        editingRoutineSetId = null,
        editingRoutineSetName = "",
        draftError = null,
        backupError = null,
        backupMessage = null,
        pendingRestoreUri = null,
        pendingRestorePreview = null,
        restorePinDigits = "",
        notice = null,
        interactionToken = state.interactionToken + 1,
    )

    fun exportUnavailable(state: GuardianModeUiState): GuardianModeUiState =
        state.copy(backupError = "백업 기능을 사용할 수 없습니다.")

    fun restoreUnavailable(state: GuardianModeUiState): GuardianModeUiState =
        state.copy(backupError = "복원 기능을 사용할 수 없습니다.")

    fun exportStarted(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        backupInProgress = true,
        backupError = null,
        backupMessage = null,
        interactionToken = state.interactionToken + 1,
    )

    fun exportSucceeded(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        backupInProgress = false,
        backupMessage = "백업 파일을 저장했습니다.",
        interactionToken = state.interactionToken + 1,
    )

    fun operationFailed(
        state: GuardianModeUiState,
        message: String,
    ): GuardianModeUiState = state.copy(
        backupInProgress = false,
        backupError = message,
        interactionToken = state.interactionToken + 1,
    )

    fun previewStarted(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        backupInProgress = true,
        backupError = null,
        backupMessage = null,
        pendingRestoreUri = null,
        pendingRestorePreview = null,
        restorePinDigits = "",
        interactionToken = state.interactionToken + 1,
    )

    fun previewSucceeded(
        state: GuardianModeUiState,
        uri: Uri,
        preview: BackupImportPreview,
    ): GuardianModeUiState = state.copy(
        backupInProgress = false,
        pendingRestoreUri = uri,
        pendingRestorePreview = preview,
        restorePinDigits = "",
        interactionToken = state.interactionToken + 1,
    )

    fun inputRestorePinDigit(
        state: GuardianModeUiState,
        digit: Int,
    ): GuardianModeUiState {
        if (state.restorePinDigits.length >= 4) return state
        return state.copy(
            restorePinDigits = state.restorePinDigits + digit,
            backupError = null,
            interactionToken = state.interactionToken + 1,
        )
    }

    fun deleteRestorePinDigit(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        restorePinDigits = state.restorePinDigits.dropLast(1),
        backupError = null,
        interactionToken = state.interactionToken + 1,
    )

    fun cancelRestore(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        pendingRestoreUri = null,
        pendingRestorePreview = null,
        restorePinDigits = "",
        backupError = null,
        interactionToken = state.interactionToken + 1,
    )

    fun restoreStarted(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        backupInProgress = true,
        backupError = null,
    )

    fun restorePinRejected(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        backupInProgress = false,
        restorePinDigits = "",
        backupError = "PIN이 맞지 않아요. 다시 입력해 주세요.",
        interactionToken = state.interactionToken + 1,
    )

    fun restoreSucceeded(
        previousState: GuardianModeUiState,
        initialState: GuardianModeUiState,
    ): GuardianModeUiState = initialState.copy(
        backupMessage = "백업 파일에서 복원했습니다.",
        interactionToken = previousState.interactionToken + 1,
        restoreCompletedToken = previousState.restoreCompletedToken + 1,
    )

    fun restoreFailed(
        state: GuardianModeUiState,
        message: String,
    ): GuardianModeUiState = state.copy(
        backupInProgress = false,
        restorePinDigits = "",
        backupError = message,
        interactionToken = state.interactionToken + 1,
    )
}
