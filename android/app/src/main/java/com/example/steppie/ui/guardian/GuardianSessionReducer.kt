package com.example.steppie.ui.guardian

internal object GuardianSessionReducer {
    fun closeToChild(
        state: GuardianModeUiState,
        initialState: GuardianModeUiState,
    ): GuardianModeUiState = initialState.withRepositorySnapshotFrom(state)

    fun closeAfterSetup(
        state: GuardianModeUiState,
        initialState: GuardianModeUiState,
    ): GuardianModeUiState = initialState.withRepositorySnapshotFrom(
        source = state,
        hasGuardianPin = true,
    )

    fun markInteraction(state: GuardianModeUiState): GuardianModeUiState = if (
        state.isActive && state.isAuthenticated
    ) {
        state.copy(interactionToken = state.interactionToken + 1)
    } else {
        state
    }

    fun showOutOfScopeNotice(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        notice = "이 기능은 이후 스프린트에서 구현합니다.",
        interactionToken = state.interactionToken + 1,
    )

    fun clearNotice(state: GuardianModeUiState): GuardianModeUiState = state.copy(notice = null)

    private fun GuardianModeUiState.withRepositorySnapshotFrom(
        source: GuardianModeUiState,
        hasGuardianPin: Boolean = source.hasGuardianPin,
    ): GuardianModeUiState = copy(
        hasGuardianPin = hasGuardianPin,
        appSettings = source.appSettings,
        routineSets = source.routineSets,
        activeRoutineSet = source.activeRoutineSet,
        todayRoutineSetId = source.todayRoutineSetId,
        selectedRoutineSetId = source.selectedRoutineSetId,
        routines = source.routines,
    )
}
