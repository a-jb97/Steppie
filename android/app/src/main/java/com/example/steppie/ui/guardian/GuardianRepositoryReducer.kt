package com.example.steppie.ui.guardian

import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.RoutineSet
import com.example.steppie.domain.model.inRoutineOrder
import java.time.LocalDate

internal object GuardianRepositoryReducer {
    fun dataChanged(
        state: GuardianModeUiState,
        settings: AppSettings,
        visibleRoutineSets: List<RoutineSet>,
        todayRoutineSetId: String?,
        records: GuardianRecordsResult,
        calendarRecordDates: Set<LocalDate>,
        calendarRecords: GuardianRecordsResult,
    ): GuardianModeUiState {
        val todaySet = visibleRoutineSets.firstOrNull { it.id == todayRoutineSetId }
        val selectedSetId = state.selectedRoutineSetId
            ?.takeIf { selectedId -> visibleRoutineSets.any { it.id == selectedId } }
            ?: todaySet?.id
            ?: visibleRoutineSets.firstOrNull(RoutineSet::isActive)?.id
            ?: visibleRoutineSets.firstOrNull()?.id
        val selectedSet = visibleRoutineSets.firstOrNull { it.id == selectedSetId }
        val resolvedPinMode = resolvePinMode(state, settings)
        val repositoryState = state.copy(
            hasGuardianPin = settings.hasGuardianPin,
            appSettings = settings,
            pinMode = resolvedPinMode,
            routineSets = visibleRoutineSets,
            activeRoutineSet = selectedSet,
            todayRoutineSetId = todaySet?.id,
            selectedRoutineSetId = selectedSetId,
            routines = selectedSet?.routines.orEmpty().inRoutineOrder(),
        )
        return GuardianRecordsReducer.repositoryDataChanged(
            state = repositoryState,
            records = records,
            calendarRecordDates = calendarRecordDates,
            calendarRecords = calendarRecords,
        ).copy(showDailyRoutineSelectionPrompt = false)
    }

    private fun resolvePinMode(
        state: GuardianModeUiState,
        settings: AppSettings,
    ): GuardianPinMode {
        if (!state.isActive || state.isAuthenticated || state.destination != GuardianDestination.Pin) {
            return state.pinMode
        }
        return when {
            settings.hasGuardianPin -> GuardianPinMode.Enter
            state.pinMode == GuardianPinMode.SetupConfirm -> GuardianPinMode.SetupConfirm
            else -> GuardianPinMode.Setup
        }
    }
}
