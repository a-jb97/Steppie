package com.example.steppie.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.steppie.data.backup.BackupMaxAssetBytes
import com.example.steppie.domain.model.IconRef
import com.example.steppie.domain.model.newUuidV4
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoutinePhotoStore(context: Context) {
    private val appContext = context.applicationContext
    private val photoDirectory: File = File(appContext.filesDir, PhotoDirectoryName)

    suspend fun importPhoto(uri: Uri): IconRef.Photo = withContext(Dispatchers.IO) {
        val assetId = newUuidV4()
        val backupAssetName = backupAssetName(assetId)
        val outputFile = fileForAssetId(assetId)
        photoDirectory.mkdirs()

        val bytes = appContext.contentResolver.openInputStream(uri)?.use { input ->
            val original = BitmapFactory.decodeStream(input)
                ?: throw IllegalArgumentException("사진 파일을 읽을 수 없습니다.")
            original.toRoutinePhotoJpegBytes()
        } ?: throw IllegalArgumentException("사진 파일을 열 수 없습니다.")

        outputFile.writeBytes(bytes)
        IconRef.Photo(localAssetId = assetId, backupAssetName = backupAssetName)
    }

    fun fileFor(icon: IconRef.Photo): File = fileForAssetId(icon.localAssetId)

    fun readBackupAssets(photoRefs: List<IconRef.Photo>): Map<String, ByteArray> =
        photoRefs.mapNotNull { photo ->
            val name = photo.backupAssetName ?: return@mapNotNull null
            val file = fileFor(photo)
            if (file.isFile) name to file.readBytes() else null
        }.toMap()

    fun replaceAllFromBackup(assets: Map<String, ByteArray>) {
        photoDirectory.deleteRecursively()
        photoDirectory.mkdirs()
        assets.forEach { (name, bytes) ->
            val assetId = localAssetIdFromBackupName(name) ?: return@forEach
            fileForAssetId(assetId).writeBytes(bytes)
        }
    }

    fun snapshotFiles(): Map<String, ByteArray> {
        if (!photoDirectory.isDirectory) return emptyMap()
        return photoDirectory.listFiles()
            ?.filter { it.isFile }
            ?.associate { it.name to it.readBytes() }
            .orEmpty()
    }

    fun replaceAllFiles(files: Map<String, ByteArray>) {
        photoDirectory.deleteRecursively()
        photoDirectory.mkdirs()
        files.forEach { (name, bytes) ->
            File(photoDirectory, name).writeBytes(bytes)
        }
    }

    private fun fileForAssetId(assetId: String): File = File(photoDirectory, "$assetId.jpg")

    private fun Bitmap.toRoutinePhotoJpegBytes(): ByteArray {
        val maxSide = 1200
        val longest = maxOf(width, height)
        val initial = if (longest <= maxSide) {
            this
        } else {
            val scale = maxSide.toFloat() / longest.toFloat()
            Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
        }
        var current = initial
        var bytes = current.toJpegBytes()
        while (bytes.size > BackupMaxAssetBytes && maxOf(current.width, current.height) > MinPhotoSide) {
            current = Bitmap.createScaledBitmap(
                current,
                (current.width * 0.8f).toInt().coerceAtLeast(1),
                (current.height * 0.8f).toInt().coerceAtLeast(1),
                true,
            )
            bytes = current.toJpegBytes()
        }
        require(bytes.size <= BackupMaxAssetBytes) { "사진 파일은 5MB 이하여야 합니다." }
        return bytes
    }

    private fun Bitmap.toJpegBytes(): ByteArray {
        var quality = 88
        var bytes: ByteArray
        do {
            val output = ByteArrayOutputStream()
            compress(Bitmap.CompressFormat.JPEG, quality, output)
            bytes = output.toByteArray()
            quality -= 8
        } while (bytes.size > BackupMaxAssetBytes && quality >= 64)
        return bytes
    }

    companion object {
        private const val PhotoDirectoryName = "routine_photos"
        private const val MinPhotoSide = 320

        fun backupAssetName(assetId: String): String = "routine-photo-$assetId.jpg"

        private fun localAssetIdFromBackupName(name: String): String? =
            name.removePrefix("routine-photo-")
                .removeSuffix(".jpg")
                .takeIf { it.length == 36 }
    }
}
