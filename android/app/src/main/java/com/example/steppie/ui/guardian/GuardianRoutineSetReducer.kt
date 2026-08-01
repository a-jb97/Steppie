package com.example.steppie.ui.guardian

import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.model.RoutineSet

internal object GuardianRoutineSetReducer {
    fun toggleListEditing(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        routineSetListEditing = !state.routineSetListEditing,
        editingRoutineSetId = null,
        editingRoutineSetName = "",
        pendingDeleteRoutineSetId = null,
        draftError = null,
        interactionToken = state.interactionToken + 1,
    )

    fun select(
        state: GuardianModeUiState,
        routineSet: RoutineSet,
    ): GuardianModeUiState = state.copy(
        activeRoutineSet = routineSet,
        selectedRoutineSetId = routineSet.id,
        routines = routineSet.routines.sortedBy(Routine::order),
        draftError = null,
        interactionToken = state.interactionToken + 1,
    )

    fun lastDailyRoutineSetRejected(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        notice = "최소 한 개의 루틴 세트는 매일 진행해야 합니다.",
        interactionToken = state.interactionToken + 1,
    )

    fun missingStartTimeRejected(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        notice = "추가할 루틴 세트의 시작 시간을 먼저 설정해 주세요.",
        interactionToken = state.interactionToken + 1,
    )

    fun duplicateDailyStartTimeRejected(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        notice = "다른 루틴 세트와 시작 시간이 같아요.",
        interactionToken = state.interactionToken + 1,
    )

    fun dailyParticipationChanged(
        state: GuardianModeUiState,
        routineSetId: String,
        wasActive: Boolean,
    ): GuardianModeUiState = state.copy(
        selectedRoutineSetId = routineSetId,
        showDailyRoutineSelectionPrompt = false,
        notice = if (wasActive) {
            "매일 진행에서 제외했습니다."
        } else {
            "매일 진행에 추가했습니다."
        },
        interactionToken = state.interactionToken + 1,
    )

    fun invalidStartTime(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        draftError = "시작 시각은 HH:mm 형식으로 입력해 주세요.",
    )

    fun duplicateStartTime(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        draftError = "다른 루틴 세트와 시작 시간이 같아요.",
    )

    fun startTimeSaved(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        draftError = null,
        notice = "루틴 세트 시작 시간을 저장했습니다.",
        interactionToken = state.interactionToken + 1,
    )

    fun dismissDailySelectionPrompt(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        showDailyRoutineSelectionPrompt = false,
        interactionToken = state.interactionToken + 1,
    )

    fun beginNameEdit(
        state: GuardianModeUiState,
        routineSetId: String,
        resolvedName: String,
    ): GuardianModeUiState = state.copy(
        editingRoutineSetId = routineSetId,
        editingRoutineSetName = resolvedName,
        draftError = null,
        interactionToken = state.interactionToken + 1,
    )

    fun updateEditingName(
        state: GuardianModeUiState,
        name: String,
    ): GuardianModeUiState = state.copy(
        editingRoutineSetName = name,
        draftError = null,
        interactionToken = state.interactionToken + 1,
    )

    fun cancelNameEdit(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        editingRoutineSetId = null,
        editingRoutineSetName = "",
        draftError = null,
        interactionToken = state.interactionToken + 1,
    )

    fun blankNameRejected(state: GuardianModeUiState): GuardianModeUiState = state.copy(
        draftError = "루틴 세트 이름을 입력해 주세요.",
    )
}
