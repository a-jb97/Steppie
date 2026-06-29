package com.example.steppie.data.backup

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.time.Instant
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal object BackupArchive {
    fun write(
        snapshot: BackupSnapshot,
        appVersion: String,
        output: OutputStream,
    ) {
        val dataJson = BackupJson.encodeData(snapshot)
        val checksum = sha256(dataJson.toByteArray(Charsets.UTF_8))
        val manifestJson = BackupJson.encodeManifest(
            createdAt = snapshot.exportedAt,
            appVersion = appVersion,
            dataChecksum = checksum,
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
        val snapshot = BackupJson.decodeData(dataBytes.toString(Charsets.UTF_8))
        return ReadBackup(manifest = manifest, snapshot = snapshot)
    }

    fun defaultFileName(now: Instant = Instant.now()): String {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withLocale(Locale.US)
            .withZone(java.time.ZoneId.systemDefault())
        return "steppie-backup-${formatter.format(now)}.zip"
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

internal data class ReadBackup(
    val manifest: BackupManifest,
    val snapshot: BackupSnapshot,
)
