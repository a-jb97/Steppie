package com.example.steppie.data.backup

import com.example.steppie.data.local.toEntity
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.DailyLog
import com.example.steppie.domain.model.IconRef
import com.example.steppie.domain.model.LocalizedText
import com.example.steppie.domain.model.LogStatus
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.model.RoutineSet
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupArchiveTest {
    private val now = Instant.parse("2026-06-17T00:00:00Z")
    private val routineSetId = "30000000-0000-4000-8000-000000000001"
    private val routineId = "40000000-0000-4000-8000-000000000001"

    @Test
    fun archive_roundTrip_preservesBackupContractData() {
        val snapshot = testSnapshot()
        val bytes = ByteArrayOutputStream()

        BackupArchive.write(snapshot, appVersion = "1.0", output = bytes)
        val read = BackupArchive.read(ByteArrayInputStream(bytes.toByteArray()))

        assertEquals("android", read.manifest.sourcePlatform)
        assertEquals(1, read.snapshot.routineSets.size)
        assertEquals(1, read.snapshot.routines.size)
        assertEquals(1, read.snapshot.dailyLogs.size)
        assertEquals("pin-hash", read.snapshot.appSettings.guardianPinHash)
    }

    @Test
    fun archive_includesRoutinePhotoAssets() {
        val assetName = "routine-photo-10000000-0000-4000-8000-000000000001.jpg"
        val photoRoutine = Routine(
            id = "40000000-0000-4000-8000-000000000002",
            routineSetId = routineSetId,
            title = LocalizedText(mapOf("ko" to "사진", "en" to "Photo")),
            icon = IconRef.Photo(
                localAssetId = "10000000-0000-4000-8000-000000000001",
                backupAssetName = assetName,
            ),
            colorToken = "color.card.sky",
            order = 1,
            createdAt = now,
            updatedAt = now,
        )
        val snapshot = testSnapshot().let { it.copy(routines = it.routines + photoRoutine.toEntity()) }
        val bytes = ByteArrayOutputStream()

        BackupArchive.write(
            snapshot = snapshot,
            appVersion = "1.0",
            output = bytes,
            assets = mapOf(assetName to byteArrayOf(1, 2, 3)),
        )
        val read = BackupArchive.read(ByteArrayInputStream(bytes.toByteArray()))

        assertTrue(read.assets.containsKey(assetName))
        assertEquals(3, read.assets.getValue(assetName).size)
    }

    @Test(expected = BackupValidationException::class)
    fun archive_rejectsOversizedRoutinePhotoAsset() {
        val bytes = ByteArrayOutputStream()

        BackupArchive.write(
            snapshot = testSnapshot(),
            appVersion = "1.0",
            output = bytes,
            assets = mapOf("routine-photo-10000000-0000-4000-8000-000000000001.jpg" to ByteArray(5 * 1024 * 1024 + 1)),
        )
    }

    @Test
    fun dataJson_doesNotContainRawPinFields() {
        val dataJson = BackupJson.encodeData(testSnapshot())

        assertTrue(dataJson.contains("guardianPinHash"))
        assertFalse(dataJson.contains("1234"))
        assertFalse(dataJson.contains("654321"))
    }

    @Test(expected = BackupValidationException::class)
    fun dataJson_rejectsDuplicateIds() {
        val snapshot = testSnapshot()
        val duplicate = snapshot.copy(routines = snapshot.routines + snapshot.routines.first())

        BackupJson.decodeData(BackupJson.encodeData(duplicate))
    }

    @Test(expected = BackupValidationException::class)
    fun dataJson_rejectsDuplicateDailyLogDateRoutinePair() {
        val snapshot = testSnapshot()
        val duplicateLog = snapshot.dailyLogs.first().copy(
            id = "50000000-0000-4000-8000-000000000002",
        )
        val duplicate = snapshot.copy(dailyLogs = snapshot.dailyLogs + duplicateLog)

        BackupJson.decodeData(BackupJson.encodeData(duplicate))
    }

    private fun testSnapshot(): BackupSnapshot {
        val routineSet = RoutineSet(
            id = routineSetId,
            name = LocalizedText(mapOf("ko" to "아침", "en" to "Morning")),
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        val routine = Routine(
            id = routineId,
            routineSetId = routineSetId,
            title = LocalizedText(mapOf("ko" to "양치", "en" to "Brush teeth")),
            icon = IconRef.Builtin("brush-teeth"),
            colorToken = "color.card.sky",
            order = 0,
            createdAt = now,
            updatedAt = now,
        )
        val dailyLog = DailyLog(
            id = "50000000-0000-4000-8000-000000000001",
            date = LocalDate.parse("2026-06-17"),
            routineId = routineId,
            routineSetId = routineSetId,
            status = LogStatus.Completed,
            completedAt = now,
            createdAt = now,
            updatedAt = now,
        )
        return BackupSnapshot(
            exportedAt = now,
            routineSets = listOf(routineSet.toEntity()),
            routines = listOf(routine.toEntity()),
            dailyLogs = listOf(dailyLog.toEntity()),
            appSettings = AppSettings(
                guardianPinHash = "pin-hash",
                recoveryCodeHash = "recovery-hash",
            ),
        )
    }
}
