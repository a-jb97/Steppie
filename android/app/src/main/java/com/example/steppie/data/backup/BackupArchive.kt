package com.example.steppie.data.backup

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal object BackupArchive {
    fun write(
        snapshot: BackupSnapshot,
        appVersion: String,
        zoneId: ZoneId,
        output: OutputStream,
        assets: Map<String, ByteArray> = emptyMap(),
    ) {
        if (assets.values.any { it.size > BackupMaxAssetBytes }) {
            throw BackupValidationException("백업 사진 에셋이 5MB를 초과합니다.")
        }
        val dataJson = BackupJson.encodeData(snapshot, zoneId)
        val checksum = sha256(dataJson.toByteArray(Charsets.UTF_8))
        val manifestJson = BackupJson.encodeManifest(
            createdAt = snapshot.exportedAt,
            appVersion = appVersion,
            dataChecksum = checksum,
            zoneId = zoneId,
        )
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifestJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(BackupDataFileName))
            zip.write(dataJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("$BackupAssetDirectory/"))
            zip.closeEntry()

            assets.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry("$BackupAssetDirectory/$name"))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }

    fun read(input: InputStream): ReadBackup {
        val entries = mutableMapOf<String, ByteArray>()
        runCatching {
            ZipInputStream(input.buffered()).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    if (!entry.isDirectory) {
                        entries[entry.name] = zip.readEntryBytes()
                    }
                    zip.closeEntry()
                }
            }
        }.getOrElse {
            throw BackupValidationException("백업 zip 파일을 읽을 수 없습니다.")
        }
        val manifestBytes = entries["manifest.json"]
            ?: throw BackupValidationException("manifest.json이 없습니다.")
        val dataBytes = entries[BackupDataFileName]
            ?: throw BackupValidationException("data.json이 없습니다.")
        val manifest = BackupJson.decodeManifest(manifestBytes.toString(Charsets.UTF_8))
        val actualChecksum = sha256(dataBytes)
        if (!actualChecksum.equals(manifest.dataChecksum, ignoreCase = true)) {
            throw BackupValidationException("백업 checksum이 일치하지 않습니다.")
        }
        val snapshot = prepareSnapshotForRestore(
            snapshot = BackupJson.decodeData(dataBytes.toString(Charsets.UTF_8)),
            sourcePlatform = manifest.sourcePlatform,
        )
        val assets = entries
            .filterKeys { it.startsWith("$BackupAssetDirectory/") }
            .mapKeys { it.key.removePrefix("$BackupAssetDirectory/") }
            .filterKeys { it.isNotBlank() && '/' !in it && '\\' !in it }
        if (assets.values.any { it.size > BackupMaxAssetBytes }) {
            throw BackupValidationException("백업 사진 에셋이 5MB를 초과합니다.")
        }
        return ReadBackup(manifest = manifest, snapshot = snapshot, assets = assets)
    }

    private fun ZipInputStream.readEntryBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        copyTo(output)
        return output.toByteArray()
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}

internal fun prepareSnapshotForRestore(
    snapshot: BackupSnapshot,
    sourcePlatform: String,
): BackupSnapshot = if (sourcePlatform == AndroidBackupPlatform) {
    snapshot
} else {
    snapshot.copy(
        appSettings = snapshot.appSettings.copy(
            guardianPinHash = null,
            recoveryCodeHash = null,
        ),
    )
}

internal data class ReadBackup(
    val manifest: BackupManifest,
    val snapshot: BackupSnapshot,
    val assets: Map<String, ByteArray>,
) {
    val requiresGuardianPinSetup: Boolean
        get() = manifest.sourcePlatform != AndroidBackupPlatform
}
