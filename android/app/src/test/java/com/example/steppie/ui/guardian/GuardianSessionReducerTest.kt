package com.example.steppie.ui.guardian

import com.example.steppie.data.sample.RoutineSampleData
import com.example.steppie.domain.model.AppSettings
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardianSessionReducerTest {
    @Test
    fun `close to child resets session transients and preserves repository snapshot`() {
        val state = populatedState()
        val initial = initialState()

        val updated = GuardianSessionReducer.closeToChild(state, initial)

        assertFalse(updated.isActive)
        assertFalse(updated.isAuthenticated)
        assertEquals(GuardianDestination.Pin, updated.destination)
        assertNull(updated.draft)
        assertNull(updated.notice)
        assertEquals(state.hasGuardianPin, updated.hasGuardianPin)
        assertEquals(state.appSettings, updated.appSettings)
        assertEquals(state.routineSets, updated.routineSets)
        assertEquals(state.activeRoutineSet, updated.activeRoutineSet)
        assertEquals(state.todayRoutineSetId, updated.todayRoutineSetId)
        assertEquals(state.selectedRoutineSetId, updated.selectedRoutineSetId)
        assertEquals(state.routines, updated.routines)
        assertEquals(initial.selectedRecordsDate, updated.selectedRecordsDate)
    }

    @Test
    fun `close after setup records pin availability even before repository refresh`() {
        val state = populatedState().copy(hasGuardianPin = false)

        val updated = GuardianSessionReducer.closeAfterSetup(state, initialState())

        assertTrue(updated.hasGuardianPin)
        assertFalse(updated.isActive)
        assertFalse(updated.isAuthenticated)
        assertEquals(state.routineSets, updated.routineSets)
    }

    @Test
    fun `interaction token advances only for an authenticated active session`() {
        val inactive = populatedState().copy(isActive = false, isAuthenticated = true)
        val locked = populatedState().copy(isActive = true, isAuthenticated = false)
        val authenticated = populatedState().copy(isActive = true, isAuthenticated = true)

        assertEquals(inactive, GuardianSessionReducer.markInteraction(inactive))
        assertEquals(locked, GuardianSessionReducer.markInteraction(locked))
        assertEquals(24L, GuardianSessionReducer.markInteraction(authenticated).interactionToken)
    }

    @Test
    fun `out of scope notice can be shown and cleared`() {
        val shown = GuardianSessionReducer.showOutOfScopeNotice(populatedState())
        val cleared = GuardianSessionReducer.clearNotice(shown)

        assertEquals("이 기능은 이후 스프린트에서 구현합니다.", shown.notice)
        assertEquals(24L, shown.interactionToken)
        assertNull(cleared.notice)
        assertEquals(shown.interactionToken, cleared.interactionToken)
    }

    private fun initialState() = GuardianModeUiState(
        selectedRecordsDate = LocalDate.of(2026, 2, 3),
    )

    private fun populatedState(): GuardianModeUiState {
        val routineSet = RoutineSampleData.morning
        return GuardianModeUiState(
            isActive = true,
            isAuthenticated = true,
            destination = GuardianDestination.CardEdit,
            hasGuardianPin = true,
            appSettings = AppSettings(guardianPinHash = "pin", recoveryCodeHash = "recovery"),
            routineSets = listOf(routineSet),
            activeRoutineSet = routineSet,
            todayRoutineSetId = routineSet.id,
            selectedRoutineSetId = routineSet.id,
            routines = routineSet.routines,
            draft = RoutineDraft(title = "활동"),
            notice = "notice",
            interactionToken = 23L,
        )
    }
}
