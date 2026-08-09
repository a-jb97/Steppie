package com.example.steppie.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutinePhotoStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = RoutinePhotoStore(context)

    @Test
    fun encodingExifRotatedPhotoRecyclesOwnedSourceBitmap() {
        val source = Bitmap.createBitmap(2, 3, Bitmap.Config.ARGB_8888)

        val bytes = store.encodeImportedPhoto(source, ExifInterface.ORIENTATION_ROTATE_90)

        assertTrue(source.isRecycled)
        val encoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        try {
            assertEquals(3, encoded.width)
            assertEquals(2, encoded.height)
        } finally {
            encoded.recycle()
        }
    }

    @Test
    fun encodingLargePhotoScalesLongestSideToMaximum() {
        val source = Bitmap.createBitmap(1600, 800, Bitmap.Config.ARGB_8888)

        val bytes = store.encodeImportedPhoto(source, ExifInterface.ORIENTATION_NORMAL)

        assertTrue(source.isRecycled)
        val encoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        try {
            assertEquals(1200, encoded.width)
            assertEquals(600, encoded.height)
        } finally {
            encoded.recycle()
        }
    }

    @Test
    fun importingCameraPhotoDeletesTemporarySourceAfterCopy() = runBlocking {
        val previousFiles = store.snapshotFiles()
        val cameraDirectory = File(context.cacheDir, "camera_photos").apply {
            deleteRecursively()
            mkdirs()
        }
        val source = File(cameraDirectory, "routine-photo-test.jpg")
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        try {
            source.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
        } finally {
            bitmap.recycle()
        }
        val sourceUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            source,
        )

        try {
            val imported = store.importPhoto(sourceUri)

            assertFalse(source.exists())
            assertTrue(store.fileFor(imported).isFile)
        } finally {
            store.replaceAllFiles(previousFiles)
            cameraDirectory.deleteRecursively()
        }
    }

    @Test
    fun importingExifRotatedCameraPhotoNormalizesOrientation() = runBlocking {
        val previousFiles = store.snapshotFiles()
        val cameraDirectory = File(context.cacheDir, "camera_photos").apply {
            deleteRecursively()
            mkdirs()
        }
        val source = File(cameraDirectory, "routine-photo-rotated.jpg")
        val bitmap = Bitmap.createBitmap(2, 3, Bitmap.Config.ARGB_8888)
        try {
            source.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
        } finally {
            bitmap.recycle()
        }
        ExifInterface(source).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
        val sourceUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            source,
        )

        try {
            val imported = store.importPhoto(sourceUri)
            val encoded = BitmapFactory.decodeFile(store.fileFor(imported).path)

            try {
                assertEquals(3, encoded.width)
                assertEquals(2, encoded.height)
            } finally {
                encoded.recycle()
            }
        } finally {
            store.replaceAllFiles(previousFiles)
            cameraDirectory.deleteRecursively()
        }
    }

    @Test
    fun failedCameraImportDeletesTemporarySourceWithoutLeavingPhotoAsset() = runBlocking {
        val previousFiles = store.snapshotFiles()
        val cameraDirectory = File(context.cacheDir, "camera_photos").apply {
            deleteRecursively()
            mkdirs()
        }
        val source = File(cameraDirectory, "routine-photo-invalid.jpg").apply {
            writeText("not an image")
        }
        val sourceUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            source,
        )

        try {
            val failure = runCatching { store.importPhoto(sourceUri) }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertFalse(source.exists())
            assertEquals(previousFiles.keys, store.snapshotFiles().keys)
        } finally {
            store.replaceAllFiles(previousFiles)
            cameraDirectory.deleteRecursively()
        }
    }

    @Test
    fun reconciliationDeletesOnlyAssetsWithoutPersistedReferences() = runBlocking {
        val previousFiles = store.snapshotFiles()
        val referencedId = "10000000-0000-4000-8000-000000000001"
        val orphanedId = "10000000-0000-4000-8000-000000000002"
        store.replaceAllFiles(
            mapOf(
                "$referencedId.jpg" to byteArrayOf(1),
                "$orphanedId.jpg" to byteArrayOf(2),
            ),
        )

        try {
            store.deleteUnreferencedAssets(setOf(referencedId))

            assertEquals(setOf("$referencedId.jpg"), store.snapshotFiles().keys)
        } finally {
            store.replaceAllFiles(previousFiles)
        }
    }
}
