package com.example.steppie.ui.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GuardianPinReducerTest {
    @Test
    fun `opening from child selects enter or setup from persisted pin state`() {
        val enter = GuardianPinReducer.openFromChild(
            populatedState().copy(hasGuardianPin = true),
        )
        val setup = GuardianPinReducer.openFromChild(
            populatedState().copy(hasGuardianPin = false),
        )

        assertEquals(GuardianPinMode.Enter, enter.pinMode)
        assertEquals(GuardianPinMode.Setup, setup.pinMode)
        assertEquals(GuardianDestination.Pin, enter.destination)
        assertTrue(enter.isActive)
        assertFalse(enter.isAuthenticated)
        assertTrue(enter.destinationBackStack.isEmpty())
        assertEquals("", enter.pinDigits)
        assertNull(enter.pinError)
        assertEquals(12L, enter.interactionToken)
    }

    @Test
    fun `digit input clears errors increments interaction and stops at four digits`() {
        val first = GuardianPinReducer.inputDigit(populatedState().copy(pinDigits = "12"), 3)
        val full = GuardianPinReducer.inputDigit(first, 4)
        val ignored = GuardianPinReducer.inputDigit(full, 5)

        assertEquals("123", first.pinDigits)
        assertNull(first.pinError)
        assertEquals(12L, first.interactionToken)
        assertEquals("1234", full.pinDigits)
        assertSame(full, ignored)
    }

    @Test
    fun `invalid digit is rejected`() {
        try {
            GuardianPinReducer.inputDigit(GuardianModeUiState(), 10)
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    @Test
    fun `setup confirmation and mismatch preserve the existing messages`() {
        val confirmation = GuardianPinReducer.awaitSetupConfirmation(
            populatedState().copy(pinMode = GuardianPinMode.Setup),
        )
        val mismatch = GuardianPinReducer.setupMismatch(confirmation.copy(pinDigits = "9999"))

        assertEquals(GuardianPinMode.SetupConfirm, confirmation.pinMode)
        assertEquals("", confirmation.pinDigits)
        assertNull(confirmation.pinError)
        assertEquals(GuardianPinMode.Setup, mismatch.pinMode)
        assertEquals("", mismatch.pinDigits)
        assertEquals("PIN이 일치하지 않아요. 처음부터 다시 입력해 주세요.", mismatch.pinError)
    }

    @Test
    fun `successful setup opens home and exposes recovery code`() {
        val result = GuardianPinReducer.setupSucceeded(populatedState(), "654321")

        assertTrue(result.isAuthenticated)
        assertEquals(GuardianDestination.Home, result.destination)
        assertTrue(result.destinationBackStack.isEmpty())
        assertEquals(GuardianRecoveryStep.ShowCode, result.recoveryStep)
        assertEquals("654321", result.recoveryCodeToShow)
        assertEquals(GuardianDestination.Home, result.recoveryReturnDestination)
        assertEquals(12L, result.interactionToken)
    }

    @Test
    fun `successful pin change returns to security and exposes recovery code`() {
        val result = GuardianPinReducer.securityRecoveryCodeShown(
            populatedState().copy(
                destinationBackStack = listOf(GuardianDestination.Home, GuardianDestination.Security),
            ),
            recoveryCode = "123456",
        )

        assertTrue(result.isAuthenticated)
        assertEquals(GuardianDestination.Security, result.destination)
        assertEquals(listOf(GuardianDestination.Home), result.destinationBackStack)
        assertEquals(GuardianRecoveryStep.ShowCode, result.recoveryStep)
        assertEquals("123456", result.recoveryCodeToShow)
        assertEquals(GuardianDestination.Security, result.recoveryReturnDestination)
    }

    @Test
    fun `recovery reset success and failure route to their original destinations`() {
        val success = GuardianPinReducer.recoveryResetSucceeded(populatedState())
        val failure = GuardianPinReducer.recoveryResetFailed(populatedState())

        assertTrue(success.isAuthenticated)
        assertEquals(GuardianDestination.Home, success.destination)
        assertTrue(success.destinationBackStack.isEmpty())
        assertFalse(success.showDailyRoutineSelectionPrompt)
        assertEquals(GuardianDestination.RecoveryCode, failure.destination)
        assertEquals(GuardianRecoveryStep.EnterCodeForPinReset, failure.recoveryStep)
        assertEquals(GuardianRecoveryError.CodeMismatch, failure.recoveryError)
        assertEquals("", failure.pinDigits)
        assertNull(failure.pinError)
    }

    @Test
    fun `pin change and recovery regeneration use distinct pin modes`() {
        val pinChange = GuardianPinReducer.openPinChange(populatedState())
        val regeneration = GuardianPinReducer.openRecoveryCodeRegeneration(populatedState())

        assertEquals(GuardianDestination.Pin, pinChange.destination)
        assertEquals(GuardianPinMode.ChangeCurrent, pinChange.pinMode)
        assertEquals(GuardianPinMode.RecoveryRegenerateConfirm, regeneration.pinMode)
        assertEquals(
            listOf(GuardianDestination.Home, GuardianDestination.Security),
            pinChange.destinationBackStack,
        )
    }

    private fun populatedState() = GuardianModeUiState(
        isActive = true,
        isAuthenticated = true,
        destination = GuardianDestination.Security,
        destinationBackStack = listOf(GuardianDestination.Home),
        pinMode = GuardianPinMode.Enter,
        pinDigits = "9876",
        pinError = "error",
        recoveryStep = GuardianRecoveryStep.ShowCode,
        recoveryCodeToShow = "000000",
        recoveryDigits = "123456",
        recoveryError = GuardianRecoveryError.CodeMismatch,
        notice = "notice",
        showDailyRoutineSelectionPrompt = true,
        interactionToken = 11L,
    )
}
