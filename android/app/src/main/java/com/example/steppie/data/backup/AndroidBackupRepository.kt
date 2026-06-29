package com.example.steppie.data.backup

import android.content.Context
import android.net.Uri

class AndroidBackupRepository(
    context: Context,
    private val dataSource: BackupDataSource,
) : BackupProvider {
    private val appContext = context.applicationContext

    override suspend fun exportTo(uri: Uri) {
        val snapshot = dataSource.snapshot()
        val output = appContext.contentResolver.openOutputStream(uri)
            ?: throw BackupValidationException("백업 파일을 만들 수 없습니다.")
        output.use {
            BackupArchive.write(
                snapshot = snapshot,
                appVersion = appVersionName(),
                output = it,
            )
        }
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

    override suspend fun restoreReplace(uri: Uri) {
        val read = readBackup(uri)
        dataSource.replaceAll(read.snapshot)
    }

    private fun readBackup(uri: Uri): ReadBackup {
        val input = appContext.contentResolver.openInputStream(uri)
            ?: throw BackupValidationException("백업 파일을 열 수 없습니다.")
        return input.use(BackupArchive::read)
    }

    private fun appVersionName(): String = runCatching {
        appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "1.0"
    }.getOrDefault("1.0")

    companion object {
        fun defaultFileName(): String = BackupArchive.defaultFileName()
    }
}
