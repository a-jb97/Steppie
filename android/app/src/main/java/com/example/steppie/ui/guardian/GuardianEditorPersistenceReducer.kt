package com.example.steppie.ui.guardian

internal object GuardianEditorPersistenceReducer {
    fun routineSetSaveFailed(
        state: GuardianModeUiState,
        message: String,
    ): GuardianModeUiState = state.copy(draftError = message)

    fun routineSetRequired(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        draftError = "먼저 루틴 세트를 생성해 주세요.",
    )

    fun routineTitleRequired(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        draftError = "활동 이름을 입력해 주세요.",
    )

    fun scheduledTimeInvalid(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        draftError = "예정 시각은 HH:mm 형식으로 입력해 주세요.",
    )

    fun saveSucceeded(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        destination = GuardianDestination.RoutineEdit,
        destinationBackStack = state.destinationBackStack.dropLastMatching(GuardianDestination.RoutineEdit),
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
}
