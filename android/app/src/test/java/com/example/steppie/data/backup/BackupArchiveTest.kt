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
import java.time.LocalTime
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupArchiveTest {
    private val now = Instant.parse("2026-06-17T00:00:00Z")
    private val zoneId = ZoneId.of("Asia/Seoul")
    private val routineSetId = "30000000-0000-4000-8000-000000000001"
    private val routineId = "40000000-0000-4000-8000-000000000001"

    @Test
    fun archive_roundTrip_preservesBackupContractData() {
        val snapshot = testSnapshot()
        val bytes = ByteArrayOutputStream()

        BackupArchive.write(snapshot, appVersion = "1.0", zoneId = zoneId, output = bytes)
        val read = BackupArchive.read(ByteArrayInputStream(bytes.toByteArray()))

        assertEquals("android", read.manifest.sourcePlatform)
        assertEquals(1, read.snapshot.routineSets.size)
        assertEquals(1, read.snapshot.routines.size)
        assertEquals(1, read.snapshot.dailyLogs.size)
        assertEquals("pin-hash", read.snapshot.appSettings.guardianPinHash)
        assertEquals("08:00", read.snapshot.routineSets.single().startTime)
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
            zoneId = zoneId,
            output = bytes,
            assets = mapOf(assetName to byteArrayOf(1, 2, 3)),
        )
        val read = BackupArchive.read(ByteArrayInputStream(bytes.toByteArray()))

        assertTrue(read.assets.containsKey(assetName))
        assertEquals(3, read.assets.getValue(assetName).size)
    }

    @Test
    fun archive_semanticGolden_ignoresZipMetadataAndPreservesContractPayload() {
        val assetName = "routine-photo-10000000-0000-4000-8000-000000000001.jpg"
        val snapshot = testSnapshot()
        val original = writeArchive(snapshot, mapOf(assetName to byteArrayOf(1, 2, 3)))
        val archiveWithDifferentMetadata = rewriteZipEntryTimes(original, timeMillis = 0L)

        val originalRead = BackupArchive.read(ByteArrayInputStream(original))
        val rewrittenRead = BackupArchive.read(ByteArrayInputStream(archiveWithDifferentMetadata))

        assertEquals(snapshot, originalRead.snapshot)
        assertEquals(originalRead.manifest, rewrittenRead.manifest)
        assertEquals(originalRead.snapshot, rewrittenRead.snapshot)
        assertEquals(originalRead.assets.keys, rewrittenRead.assets.keys)
        originalRead.assets.forEach { (name, bytes) ->
            assertTrue(bytes.contentEquals(rewrittenRead.assets.getValue(name)))
        }
    }

    @Test
    fun jsonTimestampsUseTheSuppliedLocalZone() {
        val data = JSONObject(BackupJson.encodeData(testSnapshot(), zoneId))
        val manifest = JSONObject(
            BackupJson.encodeManifest(
                createdAt = now,
                appVersion = "1.0",
                dataChecksum = "checksum",
                zoneId = zoneId,
            ),
        )

        assertEquals("2026-06-17T09:00:00+09:00", data.getString("exportedAt"))
        assertEquals(
            "2026-06-17T09:00:00+09:00",
            data.getJSONArray("routineSets").getJSONObject(0).getString("createdAt"),
        )
        assertEquals("2026-06-17T09:00:00+09:00", manifest.getString("createdAt"))
    }

    @Test
    fun dataJson_version1MissingOptionalFields_usesCompatibleDefaults() {
        val version1Json = JSONObject(BackupJson.encodeData(testSnapshot(), zoneId)).apply {
            put("schemaVersion", 1)
            getJSONArray("routineSets").getJSONObject(0).remove("startTime")
            getJSONObject("appSettings").apply {
                remove("ttsRate")
                remove("ttsVolume")
                remove("notificationLeadTimes")
                remove("quietHoursStart")
                remove("quietHoursEnd")
                remove("locale")
            }
        }

        val restored = BackupJson.decodeData(version1Json.toString())

        assertNull(restored.routineSets.single().startTime)
        assertEquals(AppSettings().ttsRate, restored.appSettings.ttsRate, 0.0)
        assertEquals(AppSettings().ttsVolume, restored.appSettings.ttsVolume, 0.0)
        assertEquals(AppSettings().notificationLeadTimes, restored.appSettings.notificationLeadTimes)
        assertNull(restored.appSettings.quietHoursStart)
        assertNull(restored.appSettings.quietHoursEnd)
        assertNull(restored.appSettings.locale)
    }

    @Test(expected = BackupValidationException::class)
    fun archive_rejectsOversizedRoutinePhotoAsset() {
        val bytes = ByteArrayOutputStream()

        BackupArchive.write(
            snapshot = testSnapshot(),
            appVersion = "1.0",
            zoneId = zoneId,
            output = bytes,
            assets = mapOf("routine-photo-10000000-0000-4000-8000-000000000001.jpg" to ByteArray(5 * 1024 * 1024 + 1)),
        )
    }

    @Test
    fun dataJson_doesNotContainRawPinFields() {
        val dataJson = BackupJson.encodeData(testSnapshot(), zoneId)

        assertTrue(dataJson.contains("guardianPinHash"))
        assertFalse(dataJson.contains("1234"))
        assertFalse(dataJson.contains("654321"))
    }

    @Test(expected = BackupValidationException::class)
    fun dataJson_rejectsDuplicateIds() {
        val snapshot = testSnapshot()
        val duplicate = snapshot.copy(routines = snapshot.routines + snapshot.routines.first())

        BackupJson.decodeData(BackupJson.encodeData(duplicate, zoneId))
    }

    @Test(expected = BackupValidationException::class)
    fun dataJson_rejectsDuplicateDailyLogDateRoutinePair() {
        val snapshot = testSnapshot()
        val duplicateLog = snapshot.dailyLogs.first().copy(
            id = "50000000-0000-4000-8000-000000000002",
        )
        val duplicate = snapshot.copy(dailyLogs = snapshot.dailyLogs + duplicateLog)

        BackupJson.decodeData(BackupJson.encodeData(duplicate, zoneId))
    }

    private fun writeArchive(
        snapshot: BackupSnapshot,
        assets: Map<String, ByteArray>,
    ): ByteArray = ByteArrayOutputStream().also { output ->
        BackupArchive.write(
            snapshot = snapshot,
            appVersion = "1.0",
            zoneId = zoneId,
            output = output,
            assets = assets,
        )
    }.toByteArray()

    private fun rewriteZipEntryTimes(bytes: ByteArray, timeMillis: Long): ByteArray {
        val output = ByteArrayOutputStream()
        ZipInputStream(ByteArrayInputStream(bytes)).use { input ->
            ZipOutputStream(output).use { zip ->
                generateSequence { input.nextEntry }.forEach { source ->
                    zip.putNextEntry(ZipEntry(source.name).apply { time = timeMillis })
                    if (!source.isDirectory) input.copyTo(zip)
                    zip.closeEntry()
                    input.closeEntry()
                }
            }
        }
        return output.toByteArray()
    }

    private fun testSnapshot(): BackupSnapshot {
        val routineSet = RoutineSet(
            id = routineSetId,
            name = LocalizedText(mapOf("ko" to "아침", "en" to "Morning")),
            isActive = true,
            startTime = LocalTime.of(8, 0),
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
