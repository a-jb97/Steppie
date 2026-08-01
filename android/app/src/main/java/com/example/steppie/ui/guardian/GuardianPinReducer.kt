package com.example.steppie.ui.guardian

internal object GuardianPinReducer {
    fun openFromChild(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        isActive = true,
        isAuthenticated = false,
        destination = GuardianDestination.Pin,
        destinationBackStack = emptyList(),
        pinMode = if (state.hasGuardianPin) GuardianPinMode.Enter else GuardianPinMode.Setup,
        pinDigits = "",
        pinError = null,
        recoveryStep = null,
        recoveryCodeToShow = null,
        recoveryDigits = "",
        recoveryError = null,
        notice = null,
        interactionToken = state.interactionToken + 1,
    )

    fun openInitialSetup(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        isActive = true,
        isAuthenticated = false,
        destination = GuardianDestination.Pin,
        destinationBackStack = emptyList(),
        pinMode = GuardianPinMode.Setup,
        pinDigits = "",
        pinError = null,
        recoveryStep = null,
        recoveryCodeToShow = null,
        interactionToken = state.interactionToken + 1,
    )

    fun inputDigit(state: GuardianModeUiState, digit: Int): GuardianModeUiState {
        require(digit in 0..9)
        if (state.pinDigits.length >= 4) return state
        return state.copy(
            pinDigits = state.pinDigits + digit,
            pinError = null,
            interactionToken = state.interactionToken + 1,
        )
    }

    fun deleteDigit(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        pinDigits = state.pinDigits.dropLast(1),
        pinError = null,
        interactionToken = state.interactionToken + 1,
    )

    fun authenticationSucceeded(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        isAuthenticated = true,
        destination = GuardianDestination.Home,
        destinationBackStack = emptyList(),
        pinDigits = "",
        pinError = null,
        showDailyRoutineSelectionPrompt = false,
        interactionToken = state.interactionToken + 1,
    )

    fun awaitSetupConfirmation(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        pinMode = GuardianPinMode.SetupConfirm,
        pinDigits = "",
        pinError = null,
        interactionToken = state.interactionToken + 1,
    )

    fun setupMismatch(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        pinMode = GuardianPinMode.Setup,
        pinDigits = "",
        pinError = "PIN이 일치하지 않아요. 처음부터 다시 입력해 주세요.",
        interactionToken = state.interactionToken + 1,
    )

    fun setupSucceeded(
        state: GuardianModeUiState,
        recoveryCode: String,
    ): GuardianModeUiState = state.copy(
        isAuthenticated = true,
        destination = GuardianDestination.Home,
        destinationBackStack = emptyList(),
        pinDigits = "",
        pinError = null,
        recoveryStep = GuardianRecoveryStep.ShowCode,
        recoveryCodeToShow = recoveryCode,
        recoveryDigits = "",
        recoveryError = null,
        recoveryReturnDestination = GuardianDestination.Home,
        interactionToken = state.interactionToken + 1,
    )

    fun currentPinVerified(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        pinMode = GuardianPinMode.ChangeNew,
        pinDigits = "",
        pinError = null,
        interactionToken = state.interactionToken + 1,
    )

    fun securityRecoveryCodeShown(
        state: GuardianModeUiState,
        recoveryCode: String,
    ): GuardianModeUiState = state.copy(
        isAuthenticated = true,
        destination = GuardianDestination.Security,
        destinationBackStack = state.destinationBackStack.dropLastMatching(GuardianDestination.Security),
        pinDigits = "",
        pinError = null,
        recoveryStep = GuardianRecoveryStep.ShowCode,
        recoveryCodeToShow = recoveryCode,
        recoveryDigits = "",
        recoveryError = null,
        recoveryReturnDestination = GuardianDestination.Security,
        interactionToken = state.interactionToken + 1,
    )

    fun recoveryResetSucceeded(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        isAuthenticated = true,
        destination = GuardianDestination.Home,
        destinationBackStack = emptyList(),
        pinDigits = "",
        pinError = null,
        recoveryStep = null,
        recoveryCodeToShow = null,
        recoveryDigits = "",
        recoveryError = null,
        recoveryReturnDestination = GuardianDestination.Home,
        showDailyRoutineSelectionPrompt = false,
        interactionToken = state.interactionToken + 1,
    )

    fun recoveryResetFailed(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        destination = GuardianDestination.RecoveryCode,
        recoveryStep = GuardianRecoveryStep.EnterCodeForPinReset,
        recoveryDigits = "",
        recoveryError = GuardianRecoveryError.CodeMismatch,
        pinDigits = "",
        pinError = null,
        interactionToken = state.interactionToken + 1,
    )

    fun showPinError(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        pinDigits = "",
        pinError = "PIN이 맞지 않아요. 다시 입력해 주세요.",
        interactionToken = state.interactionToken + 1,
    )

    fun openPinChange(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        destination = GuardianDestination.Pin,
        destinationBackStack = state.backStackFor(GuardianDestination.Pin),
        pinMode = GuardianPinMode.ChangeCurrent,
        pinDigits = "",
        pinError = null,
        recoveryStep = null,
        recoveryCodeToShow = null,
        recoveryDigits = "",
        recoveryError = null,
        interactionToken = state.interactionToken + 1,
    )

    fun openRecoveryCodeRegeneration(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        destination = GuardianDestination.Pin,
        destinationBackStack = state.backStackFor(GuardianDestination.Pin),
        pinMode = GuardianPinMode.RecoveryRegenerateConfirm,
        pinDigits = "",
        pinError = null,
        recoveryStep = null,
        recoveryCodeToShow = null,
        recoveryDigits = "",
        recoveryError = null,
        interactionToken = state.interactionToken + 1,
    )
}
