package com.example.steppie.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
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
        var outputFile: File? = null
        try {
            val assetId = newUuidV4()
            val backupAssetName = backupAssetName(assetId)
            val destination = fileForAssetId(assetId)
            outputFile = destination
            photoDirectory.mkdirs()

            val exifOrientation = appContext.contentResolver.openInputStream(uri)?.use { input ->
                runCatching {
                    ExifInterface(input).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL,
                    )
                }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
            val bytes = appContext.contentResolver.openInputStream(uri)?.use { input ->
                val original = BitmapFactory.decodeStream(input)
                    ?: throw IllegalArgumentException("사진 파일을 읽을 수 없습니다.")
                encodeImportedPhoto(original, exifOrientation)
            } ?: throw IllegalArgumentException("사진 파일을 열 수 없습니다.")

            destination.writeBytes(bytes)
            IconRef.Photo(localAssetId = assetId, backupAssetName = backupAssetName)
        } catch (error: Exception) {
            outputFile?.delete()
            throw error
        } finally {
            deleteOwnedCameraSource(uri)
        }
    }

    fun fileFor(icon: IconRef.Photo): File = fileForAssetId(icon.localAssetId)

    suspend fun deleteUnreferencedAssets(referencedAssetIds: Set<String>) = withContext(Dispatchers.IO) {
        photoDirectory.listFiles()
            ?.filter { it.isFile && it.extension == "jpg" }
            ?.filter { it.nameWithoutExtension !in referencedAssetIds }
            ?.forEach(File::delete)
    }

    fun readBackupAssets(photoRefs: List<IconRef.Photo>): Map<String, ByteArray> =
        photoRefs.mapNotNull { photo ->
            val name = photo.backupAssetName ?: return@mapNotNull null
            val file = fileFor(photo)
            if (file.isFile) name to file.readBytes() else null
        }.toMap()

    fun replaceAllFromBackup(assets: Map<String, ByteArray>) {
        val stagingDirectory = File(photoDirectory.parentFile, "$PhotoDirectoryName-restore").apply {
            deleteRecursively()
            mkdirs()
        }
        assets.forEach { (name, bytes) ->
            val assetId = localAssetIdFromBackupName(name) ?: return@forEach
            File(stagingDirectory, "$assetId.jpg").writeBytes(bytes)
        }
        photoDirectory.deleteRecursively()
        check(stagingDirectory.renameTo(photoDirectory)) { "사진 에셋을 복원할 수 없습니다." }
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

    private fun deleteOwnedCameraSource(uri: Uri) {
        if (uri.authority != "${appContext.packageName}.fileprovider" ||
            uri.pathSegments.firstOrNull() != CameraPhotoPathName
        ) {
            return
        }
        runCatching { appContext.contentResolver.delete(uri, null, null) }
    }

    private fun Bitmap.applyExifOrientation(orientation: Int): Bitmap {
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    setRotate(90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    setRotate(-90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
                else -> return this@applyExifOrientation
            }
        }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    internal fun encodeImportedPhoto(original: Bitmap, exifOrientation: Int): ByteArray {
        var oriented: Bitmap? = null
        return try {
            val normalized = original.applyExifOrientation(exifOrientation)
            oriented = normalized
            normalized.toRoutinePhotoJpegBytes()
        } finally {
            oriented?.takeIf { it !== original }?.recycle()
            original.recycle()
        }
    }

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
        return try {
            var bytes = current.toJpegBytes()
            while (bytes.size > BackupMaxAssetBytes && maxOf(current.width, current.height) > MinPhotoSide) {
                val previous = current
                current = Bitmap.createScaledBitmap(
                    previous,
                    (previous.width * 0.8f).toInt().coerceAtLeast(1),
                    (previous.height * 0.8f).toInt().coerceAtLeast(1),
                    true,
                )
                if (previous !== this) previous.recycle()
                bytes = current.toJpegBytes()
            }
            require(bytes.size <= BackupMaxAssetBytes) { "사진 파일은 5MB 이하여야 합니다." }
            bytes
        } finally {
            if (current !== this) current.recycle()
        }
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
        private const val CameraPhotoPathName = "camera_photos"
        private const val MinPhotoSide = 320

        fun backupAssetName(assetId: String): String = "routine-photo-$assetId.jpg"

        private fun localAssetIdFromBackupName(name: String): String? =
            name.removePrefix("routine-photo-")
                .removeSuffix(".jpg")
                .takeIf { it.length == 36 }
    }
}
