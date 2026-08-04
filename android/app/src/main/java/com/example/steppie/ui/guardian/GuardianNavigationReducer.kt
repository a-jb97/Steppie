package com.example.steppie.ui.guardian

internal object GuardianNavigationReducer {
    fun openHome(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        destination = GuardianDestination.Home,
        destinationBackStack = emptyList(),
        draft = null,
        routineSetDraft = null,
        selectedTemplate = null,
        templateReturnDestination = GuardianDestination.RoutineEdit,
        routineSetListEditing = false,
        editingRoutineSetId = null,
        editingRoutineSetName = "",
        draftError = null,
        pendingDeleteRoutineId = null,
        pendingDeleteRoutineSetId = null,
        notice = null,
        interactionToken = state.interactionToken + 1,
    )

    fun openRoutineEdit(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        destination = GuardianDestination.RoutineEdit,
        destinationBackStack = state.backStackFor(GuardianDestination.RoutineEdit),
        draft = null,
        routineSetDraft = null,
        selectedTemplate = null,
        templateReturnDestination = GuardianDestination.RoutineEdit,
        draftError = null,
        pendingDeleteRoutineId = null,
        pendingDeleteRoutineSetId = null,
        notice = null,
        interactionToken = state.interactionToken + 1,
    )

    fun openSecurity(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        destination = GuardianDestination.Security,
        destinationBackStack = state.backStackFor(GuardianDestination.Security),
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
        recoveryStep = null,
        recoveryCodeToShow = null,
        recoveryDigits = "",
        recoveryError = null,
        notice = null,
        interactionToken = state.interactionToken + 1,
    )

    fun openEnvironmentSettings(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        destination = GuardianDestination.EnvironmentSettings,
        destinationBackStack = state.backStackFor(GuardianDestination.EnvironmentSettings),
        draft = null,
        routineSetDraft = null,
        selectedTemplate = null,
        templateReturnDestination = GuardianDestination.RoutineEdit,
        routineSetListEditing = false,
        editingRoutineSetId = null,
        editingRoutineSetName = "",
        draftError = null,
        pendingDeleteRoutineId = null,
        pendingDeleteRoutineSetId = null,
        notice = null,
        interactionToken = state.interactionToken + 1,
    )

    fun navigateBack(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        destination = state.destinationBackStack.lastOrNull() ?: GuardianDestination.Home,
        destinationBackStack = state.destinationBackStack.dropLast(1),
        draft = null,
        routineSetDraft = null,
        selectedTemplate = null,
        templateReturnDestination = GuardianDestination.RoutineEdit,
        routineSetListEditing = false,
        editingRoutineSetId = null,
        editingRoutineSetName = "",
        draftError = null,
        pendingDeleteRoutineId = null,
        pendingDeleteRoutineSetId = null,
        backupError = null,
        backupMessage = null,
        pendingRestoreUri = null,
        pendingRestorePreview = null,
        restorePinDigits = "",
        recoveryStep = null,
        recoveryCodeToShow = null,
        recoveryDigits = "",
        recoveryError = null,
        notice = null,
        interactionToken = state.interactionToken + 1,
    )
}

internal fun GuardianModeUiState.backStackFor(destination: GuardianDestination): List<GuardianDestination> = when {
    this.destination == destination -> destinationBackStack
    this.destination == GuardianDestination.Pin && destination == GuardianDestination.Home -> emptyList()
    else -> destinationBackStack + this.destination
}

internal fun List<GuardianDestination>.dropLastMatching(destination: GuardianDestination): List<GuardianDestination> =
    if (lastOrNull() == destination) dropLast(1) else this
