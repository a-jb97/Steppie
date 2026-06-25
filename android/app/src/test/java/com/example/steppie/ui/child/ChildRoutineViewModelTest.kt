package com.example.steppie.ui.child

import com.example.steppie.data.sample.RoutineSampleData
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChildRoutineViewModelTest {
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
            selectedRoutineId = null,
            singlePane = ChildSinglePane.Focus,
        )

        assertEquals(listOf(routines[2].id, routines[3].id), state.routines.map { it.id })
        assertEquals(routines[2].id, state.selectedRoutineId)
    }

    @Test
    fun `state preserves a valid selection and falls back from an invalid selection`() {
        val routines = RoutineSampleData.morning.routines

        val selected = childRoutineState(routines, routines[3].id, ChildSinglePane.List)
        val fallback = childRoutineState(routines, "missing", ChildSinglePane.Focus)

        assertEquals(routines[3].id, selected.selectedRoutineId)
        assertEquals(ChildSinglePane.List, selected.singlePane)
        assertEquals(routines.first().id, fallback.selectedRoutineId)
    }

    @Test
    fun `empty routines produce an empty non-loading state`() {
        val state = childRoutineState(emptyList(), null, ChildSinglePane.Focus)

        assertEquals(emptyList<Any>(), state.routines)
        assertNull(state.selectedRoutineId)
        assertEquals(false, state.isLoading)
    }
}
