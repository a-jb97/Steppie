package com.example.steppie.ui.guardian

import com.example.steppie.domain.model.BuiltinIconNames
import com.example.steppie.domain.model.IconRef
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.model.RoutineColorTokens

internal object GuardianRoutineDraftReducer {
    fun openNew(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        destination = GuardianDestination.CardEdit,
        destinationBackStack = state.backStackFor(GuardianDestination.CardEdit),
        draft = RoutineDraft(),
        routineSetDraft = null,
        selectedTemplate = null,
        templateReturnDestination = GuardianDestination.RoutineEdit,
        draftError = null,
        notice = null,
        interactionToken = state.interactionToken + 1,
    )

    fun openExisting(
        state: GuardianModeUiState,
        routine: Routine,
        resolvedTitle: String,
    ): GuardianModeUiState = state.copy(
        destination = GuardianDestination.CardEdit,
        destinationBackStack = state.backStackFor(GuardianDestination.CardEdit),
        routineSetDraft = null,
        selectedTemplate = null,
        templateReturnDestination = GuardianDestination.RoutineEdit,
        draft = RoutineDraft(
            routineId = routine.id,
            title = resolvedTitle,
            icon = routine.icon,
            colorToken = routine.colorToken,
            scheduledTime = routine.scheduledTime?.toString().orEmpty(),
        ),
        draftError = null,
        notice = null,
        interactionToken = state.interactionToken + 1,
    )

    fun updateTitle(
        state: GuardianModeUiState,
        title: String,
    ): GuardianModeUiState = updateDraft(state) { it.copy(title = title) }

    fun updateBuiltinIcon(
        state: GuardianModeUiState,
        iconName: String,
    ): GuardianModeUiState {
        if (iconName !in BuiltinIconNames.all) return state
        return updateDraft(state) { it.copy(icon = IconRef.Builtin(iconName)) }
    }

    fun photoUnavailable(state: GuardianModeUiState): GuardianModeUiState =
        state.copy(draftError = "사진 선택 기능을 사용할 수 없습니다.")

    fun photoImported(
        state: GuardianModeUiState,
        photo: IconRef,
    ): GuardianModeUiState = updateDraft(state) { it.copy(icon = photo) }

    fun photoImportFailed(
        state: GuardianModeUiState,
        message: String,
    ): GuardianModeUiState = state.copy(
        draftError = message,
        interactionToken = state.interactionToken + 1,
    )

    fun removePhoto(state: GuardianModeUiState): GuardianModeUiState =
        updateDraft(state) { it.copy(icon = IconRef.Builtin("star")) }

    fun updateColor(
        state: GuardianModeUiState,
        colorToken: String,
    ): GuardianModeUiState {
        if (colorToken !in RoutineColorTokens.all) return state
        return updateDraft(state) { it.copy(colorToken = colorToken) }
    }

    fun updateScheduledTime(
        state: GuardianModeUiState,
        value: String,
    ): GuardianModeUiState {
        val sanitized = value.filter { it.isDigit() || it == ':' }.take(5)
        return updateDraft(state) { it.copy(scheduledTime = sanitized) }
    }

    private fun updateDraft(
        state: GuardianModeUiState,
        transform: (RoutineDraft) -> RoutineDraft,
    ): GuardianModeUiState = state.copy(
        draft = state.draft?.let(transform),
        draftError = null,
        interactionToken = state.interactionToken + 1,
    )
}
