package com.example.steppie.ui.app

import com.example.steppie.ui.child.ChildSinglePane
import com.example.steppie.ui.guardian.GuardianDestination
import com.example.steppie.ui.guardian.GuardianPinMode
import com.example.steppie.ui.tutorial.TutorialScreen

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

internal fun resolveTutorialScreen(
    guardianActive: Boolean,
    guardianDestination: GuardianDestination,
    guardianPinMode: GuardianPinMode,
    guardianOverlayBlocking: Boolean,
    childLoading: Boolean,
    childSinglePane: ChildSinglePane,
): TutorialScreen? {
    if (guardianActive) {
        if (guardianOverlayBlocking) return null
        return when (guardianDestination) {
            GuardianDestination.Pin ->
                TutorialScreen.GuardianPin.takeIf { guardianPinMode == GuardianPinMode.Enter }
            GuardianDestination.Home -> TutorialScreen.GuardianHome
            GuardianDestination.RoutineEdit -> TutorialScreen.RoutineManagement
            GuardianDestination.TemplateSelect -> TutorialScreen.TemplateSelect
            GuardianDestination.CardEdit -> TutorialScreen.CardEdit
            GuardianDestination.RoutineSetCreate -> TutorialScreen.RoutineSetCreate
            GuardianDestination.EnvironmentSettings -> TutorialScreen.EnvironmentSettings
            GuardianDestination.Records -> TutorialScreen.Records
            GuardianDestination.RecordsCalendar -> TutorialScreen.RecordsCalendar
            GuardianDestination.Security -> TutorialScreen.Security
            GuardianDestination.RecoveryCode -> TutorialScreen.RecoveryCode
            GuardianDestination.BackupRestore -> TutorialScreen.BackupRestore
        }
    }
    if (childLoading) return null
    return when (childSinglePane) {
        ChildSinglePane.Focus -> TutorialScreen.ChildFocus
        ChildSinglePane.List -> TutorialScreen.ChildList
    }
}

internal enum class PhotoTarget {
    RoutineDraft,
    RoutineSetStep,
}

internal data class PhotoImportRequest(
    val target: PhotoTarget,
    val uri: String,
)

internal fun shouldRequestNotificationPermission(
    apiLevel: Int,
    permissionGranted: Boolean,
): Boolean = apiLevel >= 33 && !permissionGranted

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
