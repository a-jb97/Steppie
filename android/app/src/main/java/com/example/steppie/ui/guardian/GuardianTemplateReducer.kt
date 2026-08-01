package com.example.steppie.ui.guardian

internal object GuardianTemplateReducer {
    fun open(
        state: GuardianModeUiState,
        returnDestination: GuardianDestination,
        initialTemplate: RoutineTemplate?,
    ): GuardianModeUiState = state.copy(
        destination = GuardianDestination.TemplateSelect,
        destinationBackStack = state.backStackFor(GuardianDestination.TemplateSelect),
        draft = null,
        routineSetDraft = null,
        selectedTemplate = initialTemplate,
        templateReturnDestination = returnDestination,
        draftError = null,
        notice = null,
        interactionToken = state.interactionToken + 1,
    )

    fun close(
        state: GuardianModeUiState,
        destination: GuardianDestination,
    ): GuardianModeUiState = state.copy(
        destination = destination,
        destinationBackStack = state.destinationBackStack.dropLastMatching(destination),
        selectedTemplate = null,
        draftError = null,
        notice = null,
        interactionToken = state.interactionToken + 1,
    )

    fun preview(
        state: GuardianModeUiState,
        template: RoutineTemplate,
    ): GuardianModeUiState = state.copy(
        destination = GuardianDestination.TemplateSelect,
        selectedTemplate = template,
        draft = null,
        routineSetDraft = null,
        draftError = null,
        notice = null,
        interactionToken = state.interactionToken + 1,
    )

    fun saveSucceeded(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        destination = GuardianDestination.RoutineEdit,
        destinationBackStack = state.destinationBackStack.dropLastMatching(GuardianDestination.RoutineEdit),
        selectedTemplate = null,
        draft = null,
        routineSetDraft = null,
        draftError = null,
        notice = "템플릿으로 새 루틴 세트를 저장했습니다.",
        interactionToken = state.interactionToken + 1,
    )

    fun saveFailed(
        state: GuardianModeUiState,
        message: String,
    ): GuardianModeUiState = state.copy(
        draftError = message,
        interactionToken = state.interactionToken + 1,
    )
}
