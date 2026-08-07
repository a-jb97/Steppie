package com.example.steppie.data.backup

import android.content.Context
import android.net.Uri
import com.example.steppie.core.environment.ClockProvider

class AndroidBackupRepository(
    context: Context,
    private val dataSource: BackupDataSource,
    private val clockProvider: ClockProvider,
) : BackupProvider {
    private val appContext = context.applicationContext

    override suspend fun exportTo(uri: Uri) {
        stageAndCopyBackup(
            tempDirectory = appContext.cacheDir,
            writeStagedArchive = { output ->
                val snapshot = dataSource.snapshot(clockProvider.now())
                BackupArchive.write(
                    snapshot = snapshot,
                    appVersion = appVersionName(),
                    zoneId = clockProvider.zoneId,
                    output = output,
                    assets = dataSource.photoBackupAssets(snapshot),
                )
            },
            openDestination = { appContext.contentResolver.openOutputStream(uri, "w") },
            deleteDestination = { appContext.contentResolver.delete(uri, null, null) },
        )
    }

    override suspend fun previewImport(uri: Uri): BackupImportPreview {
        val read = readBackup(uri)
        return BackupImportPreview(
            createdAt = read.manifest.createdAt,
            sourcePlatform = read.manifest.sourcePlatform,
            routineSetCount = read.snapshot.routineSets.size,
            routineCount = read.snapshot.routines.size,
            dailyLogCount = read.snapshot.dailyLogs.size,
        )
    }

    override suspend fun restoreReplace(uri: Uri): BackupRestoreResult {
        val read = readBackup(uri)
        dataSource.replaceAll(read.snapshot, read.assets)
        return BackupRestoreResult(
            requiresGuardianPinSetup = read.requiresGuardianPinSetup,
        )
    }

    private fun readBackup(uri: Uri): ReadBackup {
        val input = appContext.contentResolver.openInputStream(uri)
            ?: throw BackupValidationException("백업 파일을 열 수 없습니다.")
        return input.use(BackupArchive::read)
    }

    private fun appVersionName(): String = runCatching {
        appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "1.0"
    }.getOrDefault("1.0")
}
