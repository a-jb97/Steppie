package com.example.steppie.data.backup

import com.example.steppie.data.local.DailyLogEntity
import com.example.steppie.data.local.RoutineEntity
import com.example.steppie.data.local.RoutineSetEntity
import com.example.steppie.domain.model.AppSettings
import java.time.Instant

internal const val BackupAppName = "Steppie"
internal const val BackupSchemaVersion = 2
internal const val BackupDataFileName = "data.json"
internal const val BackupAssetDirectory = "assets"
internal const val BackupMaxAssetBytes = 5 * 1024 * 1024

data class BackupSnapshot(
    val exportedAt: Instant,
    val routineSets: List<RoutineSetEntity>,
    val routines: List<RoutineEntity>,
    val dailyLogs: List<DailyLogEntity>,
    val appSettings: AppSettings,
)

data class BackupImportPreview(
    val createdAt: Instant,
    val sourcePlatform: String,
    val routineSetCount: Int,
    val routineCount: Int,
    val dailyLogCount: Int,
)

class BackupValidationException(message: String) : Exception(message)
