package com.example.steppie.ui.guardian

import com.example.steppie.domain.model.LocalizedText
import com.example.steppie.domain.model.RoutineSet
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardianRecordsReducerTest {
    private val today = LocalDate.parse("2026-08-02")
    private val yesterday = today.minusDays(1)

    @Test
    fun `repository data refresh replaces only record presentation state`() {
        val state = populatedState()
        val records = resultFor(yesterday)
        val calendarRecords = resultFor(today)

        val updated = GuardianRecordsReducer.repositoryDataChanged(
            state = state,
            records = records,
            calendarRecordDates = setOf(yesterday),
            calendarRecords = calendarRecords,
        )

        assertEquals(state.destination, updated.destination)
        assertEquals(records.days, updated.recordDays)
        assertEquals(records.routines, updated.selectedRecordRoutines)
        assertEquals(records.summary, updated.selectedRecordSummary)
        assertEquals(setOf(yesterday), updated.calendarRecordDates)
        assertEquals(calendarRecords.summary, updated.selectedCalendarRecordSummary)
        assertEquals(state.interactionToken, updated.interactionToken)
    }

    @Test
    fun `opening records resets transient editor state and selects today`() {
        val updated = GuardianRecordsReducer.open(populatedState(), today)

        assertEquals(GuardianDestination.Records, updated.destination)
        assertEquals(listOf(GuardianDestination.Home, GuardianDestination.Security), updated.destinationBackStack)
        assertEquals(today, updated.selectedRecordsDate)
        assertNull(updated.draft)
        assertNull(updated.routineSetDraft)
        assertNull(updated.pendingDeleteRoutineId)
        assertNull(updated.pendingDeleteRoutineSetId)
        assertEquals(24L, updated.interactionToken)
    }

    @Test
    fun `opening calendar selects today and applies calculated detail`() {
        val records = resultFor(today)

        val updated = GuardianRecordsReducer.openCalendar(populatedState(), today, records)

        assertEquals(GuardianDestination.RecordsCalendar, updated.destination)
        assertEquals(YearMonth.from(today), updated.recordsCalendarMonth)
        assertEquals(today, updated.selectedCalendarRecordsDate)
        assertEquals(records.routines, updated.selectedCalendarRecordRoutines)
        assertEquals(records.summary, updated.selectedCalendarRecordSummary)
        assertEquals(24L, updated.interactionToken)
    }

    @Test
    fun `record date is selectable only when it is in visible days`() {
        val state = populatedState().copy(recordDays = listOf(day(yesterday)))

        assertTrue(GuardianRecordsReducer.canSelectDate(state, yesterday))
        assertFalse(GuardianRecordsReducer.canSelectDate(state, today))
    }

    @Test
    fun `calendar accepts recorded dates and active today only`() {
        val recordedState = populatedState().copy(calendarRecordDates = setOf(yesterday))
        val activeTodayState = recordedState.copy(routineSets = listOf(activeRoutineSet()))

        assertTrue(GuardianRecordsReducer.canSelectCalendarDate(recordedState, yesterday, today))
        assertFalse(GuardianRecordsReducer.canSelectCalendarDate(recordedState, today, today))
        assertTrue(GuardianRecordsReducer.canSelectCalendarDate(activeTodayState, today, today))
        assertFalse(GuardianRecordsReducer.canSelectCalendarDate(activeTodayState, today.plusDays(1), today))
    }

    @Test
    fun `selecting record dates applies calculated result and increments interaction`() {
        val records = resultFor(yesterday)

        val selected = GuardianRecordsReducer.selectDate(populatedState(), yesterday, records)
        val calendarSelected = GuardianRecordsReducer.selectCalendarDate(
            populatedState(),
            yesterday,
            records,
        )

        assertEquals(yesterday, selected.selectedRecordsDate)
        assertEquals(records.days, selected.recordDays)
        assertEquals(records.summary, selected.selectedRecordSummary)
        assertEquals(YearMonth.from(yesterday), calendarSelected.recordsCalendarMonth)
        assertEquals(yesterday, calendarSelected.selectedCalendarRecordsDate)
        assertEquals(records.summary, calendarSelected.selectedCalendarRecordSummary)
        assertEquals(24L, selected.interactionToken)
        assertEquals(24L, calendarSelected.interactionToken)
    }

    @Test
    fun `moving calendar month preserves selection and increments interaction`() {
        val state = populatedState().copy(
            recordsCalendarMonth = YearMonth.of(2026, 8),
            selectedCalendarRecordsDate = today,
        )

        val updated = GuardianRecordsReducer.moveCalendarMonth(state, -2)

        assertEquals(YearMonth.of(2026, 6), updated.recordsCalendarMonth)
        assertEquals(today, updated.selectedCalendarRecordsDate)
        assertEquals(24L, updated.interactionToken)
    }

    private fun populatedState() = GuardianModeUiState(
        destination = GuardianDestination.Security,
        destinationBackStack = listOf(GuardianDestination.Home),
        draft = RoutineDraft(title = "활동"),
        routineSetDraft = RoutineSetDraft(name = "루틴"),
        routineSetListEditing = true,
        editingRoutineSetId = "set-id",
        editingRoutineSetName = "수정 중",
        pendingDeleteRoutineId = "routine-id",
        pendingDeleteRoutineSetId = "set-id",
        notice = "notice",
        interactionToken = 23L,
    )

    private fun resultFor(date: LocalDate) = GuardianRecordsResult(
        days = listOf(day(date)),
        routines = listOf(
            GuardianRecordRoutine(
                routineId = "routine-id",
                title = "양치하기",
                isCompleted = true,
                completedAt = null,
                isDeleted = false,
                isInactive = false,
                isMissing = false,
            ),
        ),
        summary = day(date),
    )

    private fun day(date: LocalDate) = GuardianRecordDay(
        date = date,
        completedCount = 1,
        totalCount = 1,
        hasRecords = true,
    )

    private fun activeRoutineSet() = RoutineSet(
        name = LocalizedText(mapOf("ko" to "아침 루틴")),
        isActive = true,
    )
}
