package com.example.steppie.ui.guardian

internal object GuardianRecoveryReducer {
    fun openPinReset(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        destination = GuardianDestination.RecoveryCode,
        destinationBackStack = state.backStackFor(GuardianDestination.RecoveryCode),
        recoveryStep = GuardianRecoveryStep.EnterCodeForPinReset,
        recoveryCodeToShow = null,
        recoveryDigits = "",
        recoveryError = null,
        pinDigits = "",
        pinError = null,
        interactionToken = state.interactionToken + 1,
    )

    fun close(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        isAuthenticated = true,
        destination = state.recoveryReturnDestination,
        recoveryStep = null,
        recoveryCodeToShow = null,
        recoveryDigits = "",
        recoveryError = null,
        pinDigits = "",
        pinError = null,
        notice = null,
        showDailyRoutineSelectionPrompt = false,
        interactionToken = state.interactionToken + 1,
    )

    fun updateCodeInput(
        state: GuardianModeUiState,
        value: String,
    ): GuardianModeUiState = state.copy(
        recoveryDigits = value.filter(Char::isDigit).take(6),
        recoveryError = null,
        interactionToken = state.interactionToken + 1,
    )

    fun cancelPinReset(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        destination = if (state.isAuthenticated) GuardianDestination.Security else GuardianDestination.Pin,
        pinMode = if (state.hasGuardianPin) GuardianPinMode.Enter else GuardianPinMode.Setup,
        pinDigits = "",
        pinError = null,
        recoveryStep = null,
        recoveryCodeToShow = null,
        recoveryDigits = "",
        recoveryError = null,
        interactionToken = state.interactionToken + 1,
    )

    fun verificationSucceeded(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        destination = GuardianDestination.Pin,
        pinMode = GuardianPinMode.RecoveryResetNew,
        pinDigits = "",
        pinError = null,
        recoveryStep = null,
        recoveryCodeToShow = null,
        recoveryDigits = "",
        recoveryError = null,
        interactionToken = state.interactionToken + 1,
    )

    fun verificationFailed(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        recoveryDigits = "",
        recoveryError = GuardianRecoveryError.CodeMismatch,
        interactionToken = state.interactionToken + 1,
    )
}
