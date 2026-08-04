package com.example.steppie.ui.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

private const val POST_NOTIFICATIONS_PERMISSION = "android.permission.POST_NOTIFICATIONS"

internal interface PhotoActivityActions {
    fun pick(target: PhotoTarget)

    fun capture(target: PhotoTarget)
}

internal interface BackupDocumentActions {
    fun create(fileName: String)

    fun open()
}

internal fun interface NotificationPermissionActions {
    fun requestIfNeeded()
}

@Composable
internal fun rememberPhotoActivityActions(
    onPhotoImport: (PhotoImportRequest) -> Unit,
): PhotoActivityActions {
    val context = LocalContext.current
    var pickerTargetName by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraTargetName by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val request = resolvePhotoPickerResult(
            targetName = pickerTargetName,
            uri = result.data?.data?.toString(),
        )
        pickerTargetName = null
        request?.let(onPhotoImport)
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { succeeded ->
        val request = resolveCameraResult(
            targetName = cameraTargetName,
            uri = cameraUriString,
            succeeded = succeeded,
        )
        cameraTargetName = null
        cameraUriString = null
        request?.let(onPhotoImport)
    }

    return remember(context, pickerLauncher, cameraLauncher) {
        object : PhotoActivityActions {
            override fun pick(target: PhotoTarget) {
                pickerTargetName = target.name
                pickerLauncher.launch(imageGalleryIntent())
            }

            override fun capture(target: PhotoTarget) {
                val uri = context.createCameraImageUri()
                cameraTargetName = target.name
                cameraUriString = uri.toString()
                cameraLauncher.launch(uri)
            }
        }
    }
}

@Composable
internal fun rememberBackupDocumentActions(
    onCreateDocument: (Uri) -> Unit,
    onOpenDocument: (Uri) -> Unit,
): BackupDocumentActions {
    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        uri?.let(onCreateDocument)
    }
    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(onOpenDocument)
    }

    return remember(createLauncher, openLauncher) {
        object : BackupDocumentActions {
            override fun create(fileName: String) {
                createLauncher.launch(fileName)
            }

            override fun open() {
                openLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
            }
        }
    }
}

@Composable
internal fun rememberNotificationPermissionActions(
    onPermissionResult: () -> Unit,
): NotificationPermissionActions {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        onPermissionResult()
    }

    return remember(context, launcher) {
        NotificationPermissionActions {
            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                POST_NOTIFICATIONS_PERMISSION,
            ) == PackageManager.PERMISSION_GRANTED
            if (shouldRequestNotificationPermission(Build.VERSION.SDK_INT, permissionGranted)) {
                launcher.launch(POST_NOTIFICATIONS_PERMISSION)
            }
        }
    }
}

private fun imageGalleryIntent(): Intent = Intent(Intent.ACTION_PICK).apply {
    setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
}

private fun Context.createCameraImageUri(): Uri {
    val directory = File(cacheDir, "camera_photos").apply { mkdirs() }
    val file = File.createTempFile("routine-photo-", ".jpg", directory)
    return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}
