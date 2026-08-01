package com.example.steppie.ui.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardianNavigationReducerTest {
    @Test
    fun `home clears navigation editing and deletion state`() {
        val state = populatedState()

        val result = GuardianNavigationReducer.openHome(state)

        assertEquals(GuardianDestination.Home, result.destination)
        assertTrue(result.destinationBackStack.isEmpty())
        assertNull(result.draft)
        assertNull(result.routineSetDraft)
        assertNull(result.editingRoutineSetId)
        assertEquals("", result.editingRoutineSetName)
        assertNull(result.pendingDeleteRoutineId)
        assertNull(result.pendingDeleteRoutineSetId)
        assertNull(result.notice)
        assertEquals("message", result.backupMessage)
        assertEquals("error", result.backupError)
        assertEquals(42L, result.interactionToken)
    }

    @Test
    fun `routine editor pushes the current destination only once`() {
        val first = GuardianNavigationReducer.openRoutineEdit(
            populatedState().copy(
                destination = GuardianDestination.Home,
                destinationBackStack = emptyList(),
            ),
        )
        val second = GuardianNavigationReducer.openRoutineEdit(first)

        assertEquals(GuardianDestination.RoutineEdit, second.destination)
        assertEquals(listOf(GuardianDestination.Home), second.destinationBackStack)
        assertEquals(43L, second.interactionToken)
    }

    @Test
    fun `security clears backup and recovery messages`() {
        val result = GuardianNavigationReducer.openSecurity(populatedState())

        assertEquals(GuardianDestination.Security, result.destination)
        assertEquals(
            listOf(GuardianDestination.Home, GuardianDestination.CardEdit),
            result.destinationBackStack,
        )
        assertNull(result.backupError)
        assertNull(result.backupMessage)
        assertNull(result.recoveryStep)
        assertNull(result.recoveryCodeToShow)
        assertEquals("", result.recoveryDigits)
        assertNull(result.recoveryError)
        assertEquals("routine-id", result.pendingDeleteRoutineId)
        assertEquals("set-id", result.pendingDeleteRoutineSetId)
        assertEquals(42L, result.interactionToken)
    }

    @Test
    fun `environment settings clears editing and deletion state`() {
        val result = GuardianNavigationReducer.openEnvironmentSettings(populatedState())

        assertEquals(GuardianDestination.EnvironmentSettings, result.destination)
        assertNull(result.draft)
        assertNull(result.routineSetDraft)
        assertNull(result.editingRoutineSetId)
        assertNull(result.pendingDeleteRoutineId)
        assertNull(result.pendingDeleteRoutineSetId)
        assertEquals("message", result.backupMessage)
        assertEquals("error", result.backupError)
        assertEquals(42L, result.interactionToken)
    }

    @Test
    fun `back pops one destination and clears transient state`() {
        val result = GuardianNavigationReducer.navigateBack(populatedState())

        assertEquals(GuardianDestination.Home, result.destination)
        assertTrue(result.destinationBackStack.isEmpty())
        assertNull(result.draft)
        assertNull(result.pendingRestoreUri)
        assertNull(result.pendingRestorePreview)
        assertEquals("", result.restorePinDigits)
        assertNull(result.recoveryStep)
        assertNull(result.notice)
        assertEquals(42L, result.interactionToken)
    }

    @Test
    fun `back without history falls back to home`() {
        val result = GuardianNavigationReducer.navigateBack(
            GuardianModeUiState(
                destination = GuardianDestination.Security,
                interactionToken = 7L,
            ),
        )

        assertEquals(GuardianDestination.Home, result.destination)
        assertTrue(result.destinationBackStack.isEmpty())
        assertEquals(8L, result.interactionToken)
    }

    private fun populatedState() = GuardianModeUiState(
        destination = GuardianDestination.CardEdit,
        destinationBackStack = listOf(GuardianDestination.Home),
        draft = RoutineDraft(title = "활동"),
        routineSetDraft = RoutineSetDraft(name = "루틴"),
        routineSetListEditing = true,
        editingRoutineSetId = "set-id",
        editingRoutineSetName = "수정 중",
        draftError = "error",
        pendingDeleteRoutineId = "routine-id",
        pendingDeleteRoutineSetId = "set-id",
        backupMessage = "message",
        backupError = "error",
        restorePinDigits = "12",
        recoveryStep = GuardianRecoveryStep.ShowCode,
        recoveryCodeToShow = "123456",
        recoveryDigits = "12",
        recoveryError = GuardianRecoveryError.CodeMismatch,
        notice = "notice",
        interactionToken = 41L,
    )
}
