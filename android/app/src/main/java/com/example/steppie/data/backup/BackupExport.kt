package com.example.steppie.data.backup

import java.io.File
import java.io.OutputStream

internal suspend fun stageAndCopyBackup(
    tempDirectory: File,
    writeStagedArchive: suspend (OutputStream) -> Unit,
    openDestination: () -> OutputStream?,
    deleteDestination: () -> Unit,
) {
    var stagedArchive: File? = null
    try {
        stagedArchive = File.createTempFile("steppie-backup-", ".zip", tempDirectory)
        stagedArchive.outputStream().use { output ->
            writeStagedArchive(output)
        }
        val destination = openDestination()
            ?: throw BackupValidationException("백업 파일을 만들 수 없습니다.")
        stagedArchive.inputStream().use { input ->
            destination.use { output -> input.copyTo(output) }
        }
    } catch (error: Throwable) {
        runCatching(deleteDestination)
        throw error
    } finally {
        stagedArchive?.let { runCatching(it::delete) }
    }
}
