package com.example.steppie.ui.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardianBackupReducerTest {
    @Test
    fun `opening backup clears transient editing and restore state`() {
        val result = GuardianBackupReducer.open(populatedState())

        assertEquals(GuardianDestination.BackupRestore, result.destination)
        assertEquals(
            listOf(GuardianDestination.Home, GuardianDestination.Security),
            result.destinationBackStack,
        )
        assertNull(result.draft)
        assertNull(result.routineSetDraft)
        assertNull(result.editingRoutineSetId)
        assertNull(result.backupError)
        assertNull(result.backupMessage)
        assertEquals("", result.restorePinDigits)
        assertNull(result.notice)
        assertEquals(24L, result.interactionToken)
    }

    @Test
    fun `unavailable providers expose messages without incrementing interaction`() {
        val export = GuardianBackupReducer.exportUnavailable(populatedState())
        val restore = GuardianBackupReducer.restoreUnavailable(populatedState())

        assertEquals("백업 기능을 사용할 수 없습니다.", export.backupError)
        assertEquals("복원 기능을 사용할 수 없습니다.", restore.backupError)
        assertEquals(23L, export.interactionToken)
        assertEquals(23L, restore.interactionToken)
    }

    @Test
    fun `export lifecycle preserves existing success and failure messages`() {
        val started = GuardianBackupReducer.exportStarted(populatedState())
        val success = GuardianBackupReducer.exportSucceeded(started)
        val failure = GuardianBackupReducer.operationFailed(started, "failure")

        assertTrue(started.backupInProgress)
        assertNull(started.backupError)
        assertNull(started.backupMessage)
        assertFalse(success.backupInProgress)
        assertEquals("백업 파일을 저장했습니다.", success.backupMessage)
        assertFalse(failure.backupInProgress)
        assertEquals("failure", failure.backupError)
    }

    @Test
    fun `preview start clears prior restore input and messages`() {
        val result = GuardianBackupReducer.previewStarted(populatedState())

        assertTrue(result.backupInProgress)
        assertNull(result.backupError)
        assertNull(result.backupMessage)
        assertNull(result.pendingRestoreUri)
        assertNull(result.pendingRestorePreview)
        assertEquals("", result.restorePinDigits)
        assertEquals(24L, result.interactionToken)
    }

    @Test
    fun `restore pin input stops at four digits and delete clears errors`() {
        val first = GuardianBackupReducer.inputRestorePinDigit(
            populatedState().copy(restorePinDigits = "12"),
            3,
        )
        val full = GuardianBackupReducer.inputRestorePinDigit(first, 4)
        val ignored = GuardianBackupReducer.inputRestorePinDigit(full, 5)
        val deleted = GuardianBackupReducer.deleteRestorePinDigit(full.copy(backupError = "error"))

        assertEquals("123", first.restorePinDigits)
        assertNull(first.backupError)
        assertEquals("1234", full.restorePinDigits)
        assertSame(full, ignored)
        assertEquals("123", deleted.restorePinDigits)
        assertNull(deleted.backupError)
    }

    @Test
    fun `restore start rejection and failure preserve token rules`() {
        val started = GuardianBackupReducer.restoreStarted(populatedState())
        val rejected = GuardianBackupReducer.restorePinRejected(started)
        val failed = GuardianBackupReducer.restoreFailed(started, "restore failure")

        assertTrue(started.backupInProgress)
        assertNull(started.backupError)
        assertEquals(23L, started.interactionToken)
        assertFalse(rejected.backupInProgress)
        assertEquals("", rejected.restorePinDigits)
        assertEquals("PIN이 맞지 않아요. 다시 입력해 주세요.", rejected.backupError)
        assertFalse(failed.backupInProgress)
        assertEquals("restore failure", failed.backupError)
    }

    @Test
    fun `restore success resets state and increments completion token from previous state`() {
        val initial = GuardianModeUiState(interactionToken = 2L, restoreCompletedToken = 3L)
        val result = GuardianBackupReducer.restoreSucceeded(populatedState(), initial)

        assertEquals("백업 파일에서 복원했습니다.", result.backupMessage)
        assertEquals(24L, result.interactionToken)
        assertEquals(10L, result.restoreCompletedToken)
        assertEquals(GuardianDestination.Pin, result.destination)
    }

    private fun populatedState() = GuardianModeUiState(
        destination = GuardianDestination.Security,
        destinationBackStack = listOf(GuardianDestination.Home),
        draft = RoutineDraft(title = "활동"),
        routineSetDraft = RoutineSetDraft(name = "루틴"),
        editingRoutineSetId = "set-id",
        editingRoutineSetName = "수정 중",
        backupInProgress = true,
        backupMessage = "message",
        backupError = "error",
        restorePinDigits = "9876",
        notice = "notice",
        interactionToken = 23L,
        restoreCompletedToken = 9L,
    )
}
