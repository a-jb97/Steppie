package com.example.steppie.data.backup

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRestoreTransactionTest {
    @Test
    fun `apply failure rolls back every started store in reverse order`() = runTest {
        var room = "old-room"
        var settings = "old-settings"
        var photos = "old-photos"
        val rollbackOrder = mutableListOf<String>()
        val failure = IllegalStateException("photo restore failed")

        val thrown = runCatching {
            runBackupRestoreTransaction(
                listOf(
                    step("room", { room = "new-room" }) {
                        rollbackOrder += "room"
                        room = "old-room"
                    },
                    step("settings", { settings = "new-settings" }) {
                        rollbackOrder += "settings"
                        settings = "old-settings"
                    },
                    step("photos", {
                        photos = "partially-restored-photos"
                        throw failure
                    }) {
                        rollbackOrder += "photos"
                        photos = "old-photos"
                    },
                ),
            )
        }.exceptionOrNull()

        assertSame(failure, thrown)
        assertEquals("old-room", room)
        assertEquals("old-settings", settings)
        assertEquals("old-photos", photos)
        assertEquals(listOf("photos", "settings", "room"), rollbackOrder)
        assertTrue(failure.suppressed.isEmpty())
    }

    @Test
    fun `rollback failure is attached while remaining stores still roll back`() = runTest {
        var room = "old-room"
        var settings = "old-settings"
        val rollbackOrder = mutableListOf<String>()
        val restoreFailure = IllegalStateException("settings restore failed")
        val rollbackFailure = IllegalStateException("settings rollback failed")

        val thrown = runCatching {
            runBackupRestoreTransaction(
                listOf(
                    step("room", { room = "new-room" }) {
                        rollbackOrder += "room"
                        room = "old-room"
                    },
                    step("settings", {
                        settings = "partially-restored-settings"
                        throw restoreFailure
                    }) {
                        rollbackOrder += "settings"
                        throw rollbackFailure
                    },
                ),
            )
        }.exceptionOrNull()

        assertSame(restoreFailure, thrown)
        assertEquals("old-room", room)
        assertEquals("partially-restored-settings", settings)
        assertEquals(listOf("settings", "room"), rollbackOrder)
        assertEquals(1, restoreFailure.suppressed.size)
        assertEquals("settings", restoreFailure.suppressed.single().message)
        assertSame(rollbackFailure, restoreFailure.suppressed.single().cause)
    }

    private fun step(
        name: String,
        apply: suspend () -> Unit,
        rollback: suspend () -> Unit,
    ) = BackupRestoreStep(name, apply, rollback)
}
