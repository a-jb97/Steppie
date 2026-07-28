package com.example.steppie.ui.child

import com.example.steppie.data.sample.RoutineSampleData
import com.example.steppie.domain.model.FeedbackIntensity
import java.time.Instant
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChildRoutineViewModelTest {
    @Test
    fun `scheduled state exposes only the current routine set to the child screen`() {
        val morning = RoutineSampleData.morning.copy(isActive = true, startTime = null)
        val school = RoutineSampleData.school.copy(isActive = true, startTime = LocalTime.of(9, 0))

        val state = scheduledChildRoutineState(
            routineSets = listOf(morning, school),
            completedRoutineIds = emptySet(),
            selectedRoutineId = null,
            singlePane = ChildSinglePane.Focus,
            now = LocalTime.of(8, 0),
        )

        assertEquals(morning.id, state.currentRoutineSet?.id)
        assertEquals(morning.routines.map { it.id }, state.routines.map { it.id })
        assertEquals(morning.routines.size, state.progressTotal)
        assertEquals(
            (morning.routines + school.routines).map { it.id },
            state.scheduledRoutines.map { it.id },
        )
    }

    @Test
    fun `waiting state shows the next set but keeps completion locked until start time`() {
        val school = RoutineSampleData.school.copy(isActive = true, startTime = LocalTime.of(9, 0))

        val state = scheduledChildRoutineState(
            routineSets = listOf(school),
            completedRoutineIds = emptySet(),
            selectedRoutineId = null,
            singlePane = ChildSinglePane.Focus,
            now = LocalTime.of(8, 0),
        )

        assertEquals(school.routines.map { it.id }, state.routines.map { it.id })
        assertEquals(school.id, state.waitingRoutineSet?.id)
        assertEquals(school.routines.size, state.progressTotal)
        assertEquals(true, state.isRoutineSetLocked)
        assertEquals(null, state.currentRoutine)
        assertEquals(false, state.isSelectedRoutineCompletable)
    }

    @Test
    fun `waiting set becomes completable when its start time arrives`() {
        val school = RoutineSampleData.school.copy(isActive = true, startTime = LocalTime.of(9, 0))

        val state = scheduledChildRoutineState(
            routineSets = listOf(school),
            completedRoutineIds = emptySet(),
            selectedRoutineId = school.routines.first().id,
            singlePane = ChildSinglePane.Focus,
            now = LocalTime.of(9, 0),
        )

        assertEquals(false, state.isRoutineSetLocked)
        assertEquals(school.routines.first().id, state.currentRoutine?.id)
        assertEquals(true, state.isSelectedRoutineCompletable)
    }

    @Test
    fun `schedule waits until the first incomplete set start time`() {
        val morning = RoutineSampleData.morning.copy(
            isActive = true,
            startTime = LocalTime.of(8, 0),
        )

        val resolution = resolveRoutineSchedule(
            routineSets = listOf(morning),
            completedRoutineIds = emptySet(),
            now = LocalTime.of(7, 59),
        )

        assertEquals(null, resolution.currentSet)
        assertEquals(morning.id, resolution.waitingSet?.id)
        assertEquals(false, resolution.allComplete)
    }

    @Test
    fun `schedule starts a set at its exact start time`() {
        val morning = RoutineSampleData.morning.copy(
            isActive = true,
            startTime = LocalTime.of(8, 0),
        )

        val resolution = resolveRoutineSchedule(
            routineSets = listOf(morning),
            completedRoutineIds = emptySet(),
            now = LocalTime.of(8, 0),
        )

        assertEquals(morning.id, resolution.currentSet?.id)
        assertEquals(null, resolution.waitingSet)
    }

    @Test
    fun `schedule keeps an overdue incomplete set instead of interrupting it`() {
        val morning = RoutineSampleData.morning.copy(
            isActive = true,
            startTime = LocalTime.of(8, 0),
        )
        val school = RoutineSampleData.school.copy(
            isActive = true,
            startTime = LocalTime.of(9, 0),
        )

        val resolution = resolveRoutineSchedule(
            routineSets = listOf(morning, school),
            completedRoutineIds = emptySet(),
            now = LocalTime.of(9, 30),
        )

        assertEquals(morning.id, resolution.currentSet?.id)
    }

    @Test
    fun `schedule waits for next set after early completion then advances when due`() {
        val morning = RoutineSampleData.morning.copy(
            isActive = true,
            startTime = null,
        )
        val school = RoutineSampleData.school.copy(
            isActive = true,
            startTime = LocalTime.of(9, 0),
        )
        val morningCompleted = morning.routines.mapTo(mutableSetOf()) { it.id }

        val early = resolveRoutineSchedule(
            routineSets = listOf(morning, school),
            completedRoutineIds = morningCompleted,
            now = LocalTime.of(8, 30),
        )
        val due = resolveRoutineSchedule(
            routineSets = listOf(morning, school),
            completedRoutineIds = morningCompleted,
            now = LocalTime.of(9, 0),
        )

        assertEquals(school.id, early.waitingSet?.id)
        assertEquals(school.id, due.currentSet?.id)
    }

    @Test
    fun `schedule reports all complete only after every active set is done`() {
        val morning = RoutineSampleData.morning.copy(isActive = true)
        val school = RoutineSampleData.school.copy(isActive = true, startTime = LocalTime.of(9, 0))
        val completed = (morning.routines + school.routines).mapTo(mutableSetOf()) { it.id }

        val resolution = resolveRoutineSchedule(
            routineSets = listOf(morning, school),
            completedRoutineIds = completed,
            now = LocalTime.NOON,
        )

        assertEquals(true, resolution.allComplete)
        assertEquals(null, resolution.currentSet)
        assertEquals(null, resolution.waitingSet)
    }
    @Test
    fun `state keeps only active visible routines in order`() {
        val routines = RoutineSampleData.morning.routines
        val deleted = routines[0].copy(
            deletedAt = Instant.parse("2026-01-02T00:00:00Z"),
            isActive = false,
        )
        val inactive = routines[1].copy(isActive = false)

        val state = childRoutineState(
            routines = listOf(routines[3], inactive, routines[2], deleted),
            completedRoutineIds = emptySet(),
            selectedRoutineId = null,
            singlePane = ChildSinglePane.Focus,
        )

        assertEquals(listOf(routines[2].id, routines[3].id), state.routines.map { it.id })
        assertEquals(routines[2].id, state.selectedRoutineId)
    }

    @Test
    fun `state preserves a valid selection and falls back from an invalid selection`() {
        val routines = RoutineSampleData.morning.routines

        val selected = childRoutineState(routines, emptySet(), routines[3].id, ChildSinglePane.List)
        val fallback = childRoutineState(routines, emptySet(), "missing", ChildSinglePane.Focus)

        assertEquals(routines[3].id, selected.selectedRoutineId)
        assertEquals(ChildSinglePane.List, selected.singlePane)
        assertEquals(routines.first().id, fallback.selectedRoutineId)
    }

    @Test
    fun `empty routines produce an empty non-loading state`() {
        val state = childRoutineState(emptyList(), emptySet(), null, ChildSinglePane.Focus)

        assertEquals(emptyList<Any>(), state.routines)
        assertNull(state.selectedRoutineId)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `state derives progress and hides selected routine when all complete`() {
        val routines = RoutineSampleData.morning.routines

        val partial = childRoutineState(
            routines = routines,
            completedRoutineIds = setOf(routines[0].id, routines[1].id),
            selectedRoutineId = routines[1].id,
            singlePane = ChildSinglePane.Focus,
        )
        val complete = childRoutineState(
            routines = routines,
            completedRoutineIds = routines.mapTo(mutableSetOf()) { it.id },
            selectedRoutineId = routines.last().id,
            singlePane = ChildSinglePane.Focus,
        )

        assertEquals(2, partial.progressCount)
        assertEquals(routines.size, partial.progressTotal)
        assertEquals(routines[1].id, partial.selectedRoutineId)
        assertEquals(true, complete.isAllComplete)
        assertNull(complete.selectedRoutine)
    }

    @Test
    fun `feedback routine remains selected until it is explicitly cleared`() {
        val routines = RoutineSampleData.morning.routines

        val feedback = childRoutineState(
            routines = routines,
            completedRoutineIds = setOf(routines[0].id),
            selectedRoutineId = routines[0].id,
            singlePane = ChildSinglePane.Focus,
            feedbackRoutineId = routines[0].id,
            undoRoutineId = routines[0].id,
        )

        assertEquals(routines[0].id, feedback.selectedRoutineId)
        assertEquals(routines[0].id, feedback.feedbackRoutine?.id)
        assertEquals(routines[1].id, feedback.nextIncompleteRoutine?.id)
    }

    @Test
    fun `future selection is not completable until previous routines are done`() {
        val routines = RoutineSampleData.morning.routines

        val futureSelection = childRoutineState(
            routines = routines,
            completedRoutineIds = emptySet(),
            selectedRoutineId = routines[2].id,
            singlePane = ChildSinglePane.Focus,
        )
        val currentSelection = childRoutineState(
            routines = routines,
            completedRoutineIds = setOf(routines[0].id, routines[1].id),
            selectedRoutineId = routines[2].id,
            singlePane = ChildSinglePane.Focus,
        )

        assertEquals(routines[0].id, futureSelection.currentRoutine?.id)
        assertEquals(false, futureSelection.isSelectedRoutineCompletable)
        assertEquals(routines[2].id, currentSelection.currentRoutine?.id)
        assertEquals(true, currentSelection.isSelectedRoutineCompletable)
    }

    @Test
    fun `state keeps feedback intensity for presentation`() {
        val state = childRoutineState(
            routines = RoutineSampleData.morning.routines,
            completedRoutineIds = emptySet(),
            selectedRoutineId = null,
            singlePane = ChildSinglePane.Focus,
            feedbackIntensity = FeedbackIntensity.Strong,
        )

        assertEquals(FeedbackIntensity.Strong, state.feedbackIntensity)
    }
}
