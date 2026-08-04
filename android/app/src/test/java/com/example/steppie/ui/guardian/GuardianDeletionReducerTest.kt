package com.example.steppie.ui.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardianDeletionReducerTest {
    @Test
    fun `routine and routine set deletion requests keep independent pending ids`() {
        val routine = GuardianDeletionReducer.requestRoutine(populatedState(), "routine-id")
        val routineSet = GuardianDeletionReducer.requestRoutineSet(routine, "set-id")

        assertEquals("routine-id", routineSet.pendingDeleteRoutineId)
        assertEquals("set-id", routineSet.pendingDeleteRoutineSetId)
        assertEquals(25L, routineSet.interactionToken)
    }

    @Test
    fun `cancel clears both deletion requests`() {
        val updated = GuardianDeletionReducer.cancel(populatedState())

        assertNull(updated.pendingDeleteRoutineId)
        assertNull(updated.pendingDeleteRoutineSetId)
        assertEquals(24L, updated.interactionToken)
    }

    @Test
    fun `routine deletion result returns to routine edit and clears draft`() {
        val updated = GuardianDeletionReducer.routineDeleted(populatedState())

        assertEquals(GuardianDestination.RoutineEdit, updated.destination)
        assertEquals(listOf(GuardianDestination.Home), updated.destinationBackStack)
        assertNull(updated.draft)
        assertNull(updated.draftError)
        assertNull(updated.pendingDeleteRoutineId)
        assertEquals(24L, updated.interactionToken)
    }

    @Test
    fun `last routine set rejection clears request and exposes notice`() {
        val updated = GuardianDeletionReducer.lastRoutineSetRejected(populatedState())

        assertNull(updated.pendingDeleteRoutineSetId)
        assertEquals("마지막 루틴 세트는 삭제할 수 없습니다.", updated.notice)
        assertEquals(24L, updated.interactionToken)
    }

    @Test
    fun `routine set deletion result keeps list editing enabled`() {
        val updated = GuardianDeletionReducer.routineSetDeleted(
            populatedState().copy(routineSetListEditing = false),
        )

        assertNull(updated.pendingDeleteRoutineSetId)
        assertTrue(updated.routineSetListEditing)
        assertEquals(24L, updated.interactionToken)
    }

    private fun populatedState() = GuardianModeUiState(
        destination = GuardianDestination.CardEdit,
        destinationBackStack = listOf(GuardianDestination.Home, GuardianDestination.RoutineEdit),
        draft = RoutineDraft(title = "삭제 대상"),
        draftError = "error",
        pendingDeleteRoutineId = "routine-id",
        pendingDeleteRoutineSetId = "set-id",
        notice = "notice",
        interactionToken = 23L,
    )
}
