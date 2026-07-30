package com.example.steppie.ui.app

internal enum class AppMode {
    Bootstrap,
    Child,
    Guardian,
}

internal fun resolveAppMode(
    bootstrapComplete: Boolean,
    guardianActive: Boolean,
): AppMode = when {
    !bootstrapComplete -> AppMode.Bootstrap
    guardianActive -> AppMode.Guardian
    else -> AppMode.Child
}

internal data class NotificationRouteResolution(
    val routineIdToSelect: String? = null,
    val consumeRoute: Boolean = false,
)

internal fun resolveNotificationTarget(
    action: String?,
    routineId: String?,
    expectedAction: String,
): String? = routineId?.takeIf { action == expectedAction }

internal fun resolveNotificationRoute(
    targetRoutineId: String?,
    visibleRoutineIds: Set<String>,
): NotificationRouteResolution =
    if (targetRoutineId != null && targetRoutineId in visibleRoutineIds) {
        NotificationRouteResolution(
            routineIdToSelect = targetRoutineId,
            consumeRoute = true,
        )
    } else {
        NotificationRouteResolution()
    }

internal enum class PhotoTarget {
    RoutineDraft,
    RoutineSetStep,
}

internal data class PhotoImportRequest(
    val target: PhotoTarget,
    val uri: String,
)

internal fun resolvePhotoPickerResult(
    targetName: String?,
    uri: String?,
): PhotoImportRequest? = photoImportRequest(targetName, uri)

internal fun resolveCameraResult(
    targetName: String?,
    uri: String?,
    succeeded: Boolean,
): PhotoImportRequest? =
    if (succeeded) photoImportRequest(targetName, uri) else null

private fun photoImportRequest(
    targetName: String?,
    uri: String?,
): PhotoImportRequest? {
    val target = targetName?.let { name ->
        PhotoTarget.entries.firstOrNull { it.name == name }
    }
    return if (target != null && uri != null) {
        PhotoImportRequest(target = target, uri = uri)
    } else {
        null
    }
}
