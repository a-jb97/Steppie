package com.example.steppie.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.Routine
import com.example.steppie.ui.tutorial.TutorialScreen

internal data class NotificationReconcileInput(
    val routines: List<Routine>,
    val completedRoutineIds: Set<String>,
    val settings: AppSettings,
    val permissionRefresh: Int,
)

@Composable
internal fun NotificationReconcileEffect(
    input: NotificationReconcileInput,
    reconcile: (NotificationReconcileInput) -> Unit,
) {
    LaunchedEffect(input) {
        reconcile(input)
    }
}

@Composable
internal fun TutorialResolutionEffect(
    screen: TutorialScreen?,
    clearAnchors: () -> Unit,
    showFor: (TutorialScreen?) -> Unit,
) {
    LaunchedEffect(screen) {
        clearAnchors()
        showFor(screen)
    }
}
