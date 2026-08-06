package com.example.steppie.ui.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardianRecoveryReducerTest {
    @Test
    fun `opening pin reset clears pin and recovery inputs`() {
        val result = GuardianRecoveryReducer.openPinReset(populatedState())

        assertEquals(GuardianDestination.RecoveryCode, result.destination)
        assertEquals(
            listOf(GuardianDestination.Home, GuardianDestination.Security),
            result.destinationBackStack,
        )
        assertEquals(GuardianRecoveryStep.EnterCodeForPinReset, result.recoveryStep)
        assertNull(result.recoveryCodeToShow)
        assertEquals("", result.recoveryDigits)
        assertNull(result.recoveryError)
        assertEquals("", result.pinDigits)
        assertNull(result.pinError)
        assertEquals(18L, result.interactionToken)
    }

    @Test
    fun `recovery input keeps only six digits and clears mismatch`() {
        val result = GuardianRecoveryReducer.updateCodeInput(populatedState(), "a1-23 4567")

        assertEquals("123456", result.recoveryDigits)
        assertNull(result.recoveryError)
        assertEquals(18L, result.interactionToken)
    }

    @Test
    fun `recovery input replaces existing value when text field deletes a digit`() {
        val result = GuardianRecoveryReducer.updateCodeInput(populatedState(), "12345")

        assertEquals("12345", result.recoveryDigits)
        assertNull(result.recoveryError)
        assertEquals(18L, result.interactionToken)
    }

    @Test
    fun `closing recovery returns to the recorded destination`() {
        val result = GuardianRecoveryReducer.close(
            populatedState().copy(recoveryReturnDestination = GuardianDestination.Home),
        )

        assertTrue(result.isAuthenticated)
        assertEquals(GuardianDestination.Home, result.destination)
        assertNull(result.recoveryStep)
        assertNull(result.recoveryCodeToShow)
        assertEquals("", result.recoveryDigits)
        assertNull(result.recoveryError)
        assertEquals("", result.pinDigits)
        assertNull(result.pinError)
        assertNull(result.notice)
        assertFalse(result.showDailyRoutineSelectionPrompt)
    }

    @Test
    fun `cancel returns authenticated users to security`() {
        val result = GuardianRecoveryReducer.cancelPinReset(
            populatedState().copy(isAuthenticated = true, hasGuardianPin = true),
        )

        assertEquals(GuardianDestination.Security, result.destination)
        assertEquals(GuardianPinMode.Enter, result.pinMode)
        assertNull(result.recoveryStep)
        assertEquals("", result.recoveryDigits)
    }

    @Test
    fun `cancel returns unauthenticated users without pin to setup`() {
        val result = GuardianRecoveryReducer.cancelPinReset(
            populatedState().copy(isAuthenticated = false, hasGuardianPin = false),
        )

        assertEquals(GuardianDestination.Pin, result.destination)
        assertEquals(GuardianPinMode.Setup, result.pinMode)
    }

    @Test
    fun `verification result routes success to new pin and failure back to code input`() {
        val success = GuardianRecoveryReducer.verificationSucceeded(populatedState())
        val failure = GuardianRecoveryReducer.verificationFailed(populatedState())

        assertEquals(GuardianDestination.Pin, success.destination)
        assertEquals(GuardianPinMode.RecoveryResetNew, success.pinMode)
        assertNull(success.recoveryStep)
        assertEquals("", success.recoveryDigits)
        assertNull(success.recoveryError)
        assertEquals("", failure.recoveryDigits)
        assertEquals(GuardianRecoveryError.CodeMismatch, failure.recoveryError)
    }

    private fun populatedState() = GuardianModeUiState(
        isAuthenticated = true,
        destination = GuardianDestination.Security,
        destinationBackStack = listOf(GuardianDestination.Home),
        pinMode = GuardianPinMode.Enter,
        pinDigits = "1234",
        pinError = "pin error",
        recoveryStep = GuardianRecoveryStep.ShowCode,
        recoveryCodeToShow = "654321",
        recoveryDigits = "123456",
        recoveryError = GuardianRecoveryError.CodeMismatch,
        notice = "notice",
        showDailyRoutineSelectionPrompt = true,
        interactionToken = 17L,
    )
}
