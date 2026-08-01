package com.example.steppie.ui.guardian

import com.example.steppie.domain.model.RoutineSet
import java.time.LocalDate
import java.time.YearMonth

internal object GuardianRecordsReducer {
    fun repositoryDataChanged(
        state: GuardianModeUiState,
        records: GuardianRecordsResult,
        calendarRecordDates: Set<LocalDate>,
        calendarRecords: GuardianRecordsResult,
    ): GuardianModeUiState = state.copy(
        recordDays = records.days,
        selectedRecordRoutines = records.routines,
        selectedRecordSummary = records.summary,
        calendarRecordDates = calendarRecordDates,
        selectedCalendarRecordRoutines = calendarRecords.routines,
        selectedCalendarRecordSummary = calendarRecords.summary,
    )

    fun open(
        state: GuardianModeUiState,
        today: LocalDate,
    ): GuardianModeUiState = state.copy(
        destination = GuardianDestination.Records,
        destinationBackStack = state.backStackFor(GuardianDestination.Records),
        selectedRecordsDate = today,
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

    fun openCalendar(
        state: GuardianModeUiState,
        today: LocalDate,
        records: GuardianRecordsResult,
    ): GuardianModeUiState = state.copy(
        destination = GuardianDestination.RecordsCalendar,
        destinationBackStack = state.backStackFor(GuardianDestination.RecordsCalendar),
        recordsCalendarMonth = YearMonth.from(today),
        selectedCalendarRecordsDate = today,
        selectedCalendarRecordRoutines = records.routines,
        selectedCalendarRecordSummary = records.summary,
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

    fun canSelectDate(
        state: GuardianModeUiState,
        date: LocalDate,
    ): Boolean = state.recordDays.any { it.date == date }

    fun selectDate(
        state: GuardianModeUiState,
        date: LocalDate,
        records: GuardianRecordsResult,
    ): GuardianModeUiState = state.copy(
        selectedRecordsDate = date,
        recordDays = records.days,
        selectedRecordRoutines = records.routines,
        selectedRecordSummary = records.summary,
        interactionToken = state.interactionToken + 1,
    )

    fun canSelectCalendarDate(
        state: GuardianModeUiState,
        date: LocalDate,
        today: LocalDate,
    ): Boolean = date in state.calendarRecordDates || (
        date == today && state.routineSets.any(RoutineSet::isActive)
    )

    fun selectCalendarDate(
        state: GuardianModeUiState,
        date: LocalDate,
        records: GuardianRecordsResult,
    ): GuardianModeUiState = state.copy(
        recordsCalendarMonth = YearMonth.from(date),
        selectedCalendarRecordsDate = date,
        selectedCalendarRecordRoutines = records.routines,
        selectedCalendarRecordSummary = records.summary,
        interactionToken = state.interactionToken + 1,
    )

    fun moveCalendarMonth(
        state: GuardianModeUiState,
        monthDelta: Long,
    ): GuardianModeUiState = state.copy(
        recordsCalendarMonth = state.recordsCalendarMonth.plusMonths(monthDelta),
        interactionToken = state.interactionToken + 1,
    )
}

internal data class GuardianRecordsResult(
    val days: List<GuardianRecordDay>,
    val routines: List<GuardianRecordRoutine>,
    val summary: GuardianRecordDay,
)
