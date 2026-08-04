package com.example.steppie.ui.guardian

import com.example.steppie.data.sample.RoutineSampleData
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.RoutineSet
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class GuardianRepositoryReducerTest {
    private val today = LocalDate.parse("2026-08-02")
    private val morning = RoutineSampleData.morning.copy(isActive = true)
    private val bedtime = requireNotNull(RoutineTemplates.find(RoutineTemplateId.Bedtime))
        .instantiate(Instant.parse("2026-01-02T08:00:00Z"))

    @Test
    fun `refresh keeps a valid user selection ahead of todays routine set`() {
        val updated = reduce(
            state = GuardianModeUiState(selectedRoutineSetId = bedtime.id),
            visibleRoutineSets = listOf(morning, bedtime),
            todayRoutineSetId = morning.id,
        )

        assertEquals(bedtime.id, updated.selectedRoutineSetId)
        assertEquals(bedtime, updated.activeRoutineSet)
        assertEquals(bedtime.routines.sortedBy { it.order }, updated.routines)
        assertEquals(morning.id, updated.todayRoutineSetId)
    }

    @Test
    fun `refresh falls back through today active and first visible routine sets`() {
        val invalidSelection = GuardianModeUiState(selectedRoutineSetId = "missing-id")

        val todaySelected = reduce(
            state = invalidSelection,
            visibleRoutineSets = listOf(morning, bedtime),
            todayRoutineSetId = bedtime.id,
        )
        val activeSelected = reduce(
            state = invalidSelection,
            visibleRoutineSets = listOf(bedtime, morning),
            todayRoutineSetId = null,
        )
        val firstSelected = reduce(
            state = invalidSelection,
            visibleRoutineSets = listOf(bedtime, morning.copy(isActive = false)),
            todayRoutineSetId = null,
        )
        val empty = reduce(
            state = invalidSelection,
            visibleRoutineSets = emptyList(),
            todayRoutineSetId = morning.id,
        )

        assertEquals(bedtime.id, todaySelected.selectedRoutineSetId)
        assertEquals(morning.id, activeSelected.selectedRoutineSetId)
        assertEquals(bedtime.id, firstSelected.selectedRoutineSetId)
        assertNull(empty.selectedRoutineSetId)
        assertNull(empty.activeRoutineSet)
        assertNull(empty.todayRoutineSetId)
    }

    @Test
    fun `locked pin mode follows settings while setup confirmation remains stable`() {
        val lockedState = GuardianModeUiState(
            isActive = true,
            isAuthenticated = false,
            destination = GuardianDestination.Pin,
            pinMode = GuardianPinMode.SetupConfirm,
        )

        val setupConfirmation = reduce(state = lockedState, settings = AppSettings())
        val enter = reduce(
            state = lockedState,
            settings = AppSettings(guardianPinHash = "pin", recoveryCodeHash = "recovery"),
        )
        val authenticated = reduce(
            state = lockedState.copy(isAuthenticated = true, pinMode = GuardianPinMode.ChangeNew),
            settings = AppSettings(),
        )

        assertEquals(GuardianPinMode.SetupConfirm, setupConfirmation.pinMode)
        assertEquals(GuardianPinMode.Enter, enter.pinMode)
        assertEquals(GuardianPinMode.ChangeNew, authenticated.pinMode)
    }

    @Test
    fun `refresh applies settings and record presentation without interaction changes`() {
        val records = recordsFor(today.minusDays(1))
        val calendarRecords = recordsFor(today)
        val settings = AppSettings(ttsEnabled = false)
        val state = GuardianModeUiState(
            showDailyRoutineSelectionPrompt = true,
            interactionToken = 23L,
        )

        val updated = GuardianRepositoryReducer.dataChanged(
            state = state,
            settings = settings,
            visibleRoutineSets = listOf(morning),
            todayRoutineSetId = morning.id,
            records = records,
            calendarRecordDates = setOf(today.minusDays(1)),
            calendarRecords = calendarRecords,
        )

        assertEquals(settings, updated.appSettings)
        assertEquals(settings.hasGuardianPin, updated.hasGuardianPin)
        assertEquals(records.days, updated.recordDays)
        assertEquals(records.summary, updated.selectedRecordSummary)
        assertEquals(calendarRecords.summary, updated.selectedCalendarRecordSummary)
        assertEquals(setOf(today.minusDays(1)), updated.calendarRecordDates)
        assertFalse(updated.showDailyRoutineSelectionPrompt)
        assertEquals(23L, updated.interactionToken)
    }

    private fun reduce(
        state: GuardianModeUiState,
        settings: AppSettings = AppSettings(),
        visibleRoutineSets: List<RoutineSet> = listOf(morning),
        todayRoutineSetId: String? = morning.id,
    ): GuardianModeUiState = GuardianRepositoryReducer.dataChanged(
        state = state,
        settings = settings,
        visibleRoutineSets = visibleRoutineSets,
        todayRoutineSetId = todayRoutineSetId,
        records = recordsFor(today),
        calendarRecordDates = emptySet(),
        calendarRecords = recordsFor(today),
    )

    private fun recordsFor(date: LocalDate): GuardianRecordsResult {
        val day = GuardianRecordDay(date, completedCount = 0, totalCount = 0, hasRecords = false)
        return GuardianRecordsResult(days = listOf(day), routines = emptyList(), summary = day)
    }
}
