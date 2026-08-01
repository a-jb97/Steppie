package com.example.steppie.ui.guardian

internal object GuardianDeletionReducer {
    fun requestRoutine(
        state: GuardianModeUiState,
        routineId: String,
    ): GuardianModeUiState = state.copy(
        pendingDeleteRoutineId = routineId,
        interactionToken = state.interactionToken + 1,
    )

    fun requestRoutineSet(
        state: GuardianModeUiState,
        routineSetId: String,
    ): GuardianModeUiState = state.copy(
        pendingDeleteRoutineSetId = routineSetId,
        interactionToken = state.interactionToken + 1,
    )

    fun cancel(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        pendingDeleteRoutineId = null,
        pendingDeleteRoutineSetId = null,
        interactionToken = state.interactionToken + 1,
    )

    fun routineDeleted(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        destination = GuardianDestination.RoutineEdit,
        destinationBackStack = state.destinationBackStack.dropLastMatching(GuardianDestination.RoutineEdit),
        draft = null,
        draftError = null,
        pendingDeleteRoutineId = null,
        interactionToken = state.interactionToken + 1,
    )

    fun lastRoutineSetRejected(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        pendingDeleteRoutineSetId = null,
        notice = "마지막 루틴 세트는 삭제할 수 없습니다.",
        interactionToken = state.interactionToken + 1,
    )

    fun routineSetDeleted(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        pendingDeleteRoutineSetId = null,
        routineSetListEditing = true,
        interactionToken = state.interactionToken + 1,
    )
}
