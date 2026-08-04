package com.example.steppie.ui.guardian

import com.example.steppie.domain.model.BuiltinIconNames
import com.example.steppie.domain.model.IconRef
import com.example.steppie.domain.model.RoutineColorTokens

internal object GuardianRoutineSetDraftReducer {
    fun open(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        destination = GuardianDestination.RoutineSetCreate,
        destinationBackStack = state.backStackFor(GuardianDestination.RoutineSetCreate),
        draft = null,
        routineSetDraft = RoutineSetDraft(),
        selectedTemplate = null,
        templateReturnDestination = GuardianDestination.RoutineEdit,
        draftError = null,
        pendingDeleteRoutineId = null,
        pendingDeleteRoutineSetId = null,
        notice = null,
        interactionToken = state.interactionToken + 1,
    )

    fun updateName(
        state: GuardianModeUiState,
        name: String,
    ): GuardianModeUiState = updateDraft(state) { it.copy(name = name) }

    fun updateStepTitle(
        state: GuardianModeUiState,
        title: String,
    ): GuardianModeUiState = updateDraft(state) {
        it.copy(stepDraft = it.stepDraft.copy(title = title))
    }

    fun updateStepBuiltinIcon(
        state: GuardianModeUiState,
        iconName: String,
    ): GuardianModeUiState {
        if (iconName !in BuiltinIconNames.all) return state
        return updateDraft(state) {
            it.copy(stepDraft = it.stepDraft.copy(icon = IconRef.Builtin(iconName)))
        }
    }

    fun photoUnavailable(state: GuardianModeUiState): GuardianModeUiState =
        state.copy(draftError = "사진 선택 기능을 사용할 수 없습니다.")

    fun photoImported(
        state: GuardianModeUiState,
        photo: IconRef,
    ): GuardianModeUiState = updateDraft(state) {
        it.copy(stepDraft = it.stepDraft.copy(icon = photo))
    }

    fun photoImportFailed(
        state: GuardianModeUiState,
        message: String,
    ): GuardianModeUiState = state.copy(
        draftError = message,
        interactionToken = state.interactionToken + 1,
    )

    fun removeStepPhoto(state: GuardianModeUiState): GuardianModeUiState = updateDraft(state) {
        it.copy(stepDraft = it.stepDraft.copy(icon = IconRef.Builtin("star")))
    }

    fun updateStepColor(
        state: GuardianModeUiState,
        colorToken: String,
    ): GuardianModeUiState {
        if (colorToken !in RoutineColorTokens.all) return state
        return updateDraft(state) {
            it.copy(stepDraft = it.stepDraft.copy(colorToken = colorToken))
        }
    }

    fun updateStepScheduledTime(
        state: GuardianModeUiState,
        value: String,
    ): GuardianModeUiState {
        val sanitized = value.filter { it.isDigit() || it == ':' }.take(5)
        return updateDraft(state) {
            it.copy(stepDraft = it.stepDraft.copy(scheduledTime = sanitized))
        }
    }

    fun showError(
        state: GuardianModeUiState,
        message: String,
    ): GuardianModeUiState = state.copy(draftError = message)

    fun addOrUpdateStep(
        state: GuardianModeUiState,
        trimmedTitle: String,
    ): GuardianModeUiState {
        val currentDraft = state.routineSetDraft ?: return state
        return state.copy(
            routineSetDraft = currentDraft.copy(
                stepDraft = RoutineDraft(),
                steps = currentDraft.editingStepIndex?.let { index ->
                    currentDraft.steps.mapIndexed { stepIndex, step ->
                        if (stepIndex == index) currentDraft.stepDraft.copy(title = trimmedTitle) else step
                    }
                } ?: (currentDraft.steps + currentDraft.stepDraft.copy(title = trimmedTitle)),
                editingStepIndex = null,
            ),
            draftError = null,
            interactionToken = state.interactionToken + 1,
        )
    }

    fun editStep(
        state: GuardianModeUiState,
        index: Int,
    ): GuardianModeUiState {
        val currentDraft = state.routineSetDraft ?: return state
        val step = currentDraft.steps.getOrNull(index) ?: return state
        return state.copy(
            routineSetDraft = currentDraft.copy(
                stepDraft = step,
                editingStepIndex = index,
            ),
            draftError = null,
            interactionToken = state.interactionToken + 1,
        )
    }

    fun removeStep(
        state: GuardianModeUiState,
        index: Int,
    ): GuardianModeUiState {
        val currentDraft = state.routineSetDraft ?: return state
        if (index !in currentDraft.steps.indices) return state
        return state.copy(
            routineSetDraft = currentDraft.copy(
                steps = currentDraft.steps.filterIndexed { stepIndex, _ -> stepIndex != index },
                stepDraft = if (currentDraft.editingStepIndex == index) {
                    RoutineDraft()
                } else {
                    currentDraft.stepDraft
                },
                editingStepIndex = null,
            ),
            draftError = null,
            interactionToken = state.interactionToken + 1,
        )
    }

    private fun updateDraft(
        state: GuardianModeUiState,
        transform: (RoutineSetDraft) -> RoutineSetDraft,
    ): GuardianModeUiState = state.copy(
        routineSetDraft = state.routineSetDraft?.let(transform),
        draftError = null,
        interactionToken = state.interactionToken + 1,
    )
}
