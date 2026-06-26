package com.example.steppie

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.steppie.data.local.SteppieDatabase
import com.example.steppie.data.repository.DataStoreAppSettingsRepository
import com.example.steppie.data.repository.RoomRoutineRepository
import com.example.steppie.data.sample.RoutineSampleData
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.notifications.ACTION_OPEN_ROUTINE
import com.example.steppie.notifications.AndroidRoutineNotificationScheduler
import com.example.steppie.notifications.EXTRA_ROUTINE_ID
import com.example.steppie.ui.child.ChildRoutineFeedbackEvent
import com.example.steppie.ui.child.ChildRoutineScreen
import com.example.steppie.ui.child.ChildRoutineViewModel
import com.example.steppie.ui.guardian.GuardianModeScreen
import com.example.steppie.ui.guardian.GuardianModeViewModel
import com.example.steppie.ui.theme.SteppieTheme
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val routineRepository by lazy {
        RoomRoutineRepository(SteppieDatabase.getInstance(this))
    }
    private val appSettingsRepository by lazy { DataStoreAppSettingsRepository(this) }
    private val notificationScheduler by lazy { AndroidRoutineNotificationScheduler(this) }
    private val notificationRoutineId = MutableStateFlow<String?>(null)
    private lateinit var feedbackController: AndroidFeedbackController

    override fun onCreate(savedInstanceState: Bundle?) {
        enforceSupportedOrientation()
        super.onCreate(savedInstanceState)
        notificationRoutineId.value = intent.notificationRoutineId()
        feedbackController = AndroidFeedbackController(this)
        lifecycleScope.launch {
            RoutineSampleData.seedRepositoryIfEmpty(routineRepository)
        }
        enableEdgeToEdge()
        setContent {
            SteppieTheme {
                val childViewModel: ChildRoutineViewModel = viewModel(
                    factory = ChildRoutineViewModel.factory(routineRepository),
                )
                val guardianViewModel: GuardianModeViewModel = viewModel(
                    factory = GuardianModeViewModel.factory(routineRepository, appSettingsRepository),
                )
                val childState by childViewModel.uiState.collectAsStateWithLifecycle()
                val guardianState by guardianViewModel.uiState.collectAsStateWithLifecycle()
                val settings by appSettingsRepository.observeAppSettings()
                    .collectAsStateWithLifecycle(initialValue = AppSettings())
                val targetRoutineId by notificationRoutineId.collectAsStateWithLifecycle()
                var notificationPermissionRefresh by remember { mutableIntStateOf(0) }
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) {
                    notificationPermissionRefresh += 1
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                LaunchedEffect(childViewModel) {
                    childViewModel.feedbackEvents.collect(feedbackController::play)
                }
                LaunchedEffect(childState.routines, childState.completedRoutineIds, settings, notificationPermissionRefresh) {
                    notificationScheduler.reconcileToday(
                        routines = childState.routines,
                        completedRoutineIds = childState.completedRoutineIds,
                        settings = settings,
                    )
                }
                LaunchedEffect(targetRoutineId, childState.routines) {
                    val routineId = targetRoutineId ?: return@LaunchedEffect
                    if (childState.routines.any { it.id == routineId }) {
                        childViewModel.selectRoutine(routineId)
                        notificationRoutineId.value = null
                    }
                }
                LaunchedEffect(
                    guardianState.isActive,
                    guardianState.isAuthenticated,
                    guardianState.interactionToken,
                ) {
                    if (guardianState.isActive && guardianState.isAuthenticated) {
                        delay(180_000L)
                        guardianViewModel.closeToChild()
                    }
                }
                if (guardianState.isActive) {
                    GuardianModeScreen(
                        state = guardianState,
                        onDigit = guardianViewModel::inputPinDigit,
                        onDeletePinDigit = guardianViewModel::deletePinDigit,
                        onCloseToChild = guardianViewModel::closeToChild,
                        onInteraction = guardianViewModel::markInteraction,
                        onOpenHome = guardianViewModel::openHome,
                        onOpenRoutineEdit = guardianViewModel::openRoutineEdit,
                        onOpenSecurity = guardianViewModel::openSecurity,
                        onOpenPinChange = guardianViewModel::openPinChange,
                        onOpenNewRoutineEditor = guardianViewModel::openNewRoutineEditor,
                        onOpenRoutineEditor = guardianViewModel::openRoutineEditor,
                        onDraftTitleChange = guardianViewModel::updateDraftTitle,
                        onDraftIconChange = guardianViewModel::updateDraftIcon,
                        onDraftColorChange = guardianViewModel::updateDraftColor,
                        onDraftScheduledTimeChange = guardianViewModel::updateDraftScheduledTime,
                        onSaveDraft = guardianViewModel::saveDraft,
                        onRequestDelete = guardianViewModel::requestDelete,
                        onCancelDelete = guardianViewModel::cancelDelete,
                        onConfirmDelete = guardianViewModel::confirmDelete,
                        onMoveRoutine = guardianViewModel::moveRoutine,
                        onShowOutOfScopeNotice = guardianViewModel::showOutOfScopeNotice,
                        onClearNotice = guardianViewModel::clearNotice,
                    )
                } else {
                    ChildRoutineScreen(
                        state = childState,
                        onShowList = childViewModel::showList,
                        onShowFocus = childViewModel::showFocus,
                        onSelectRoutine = childViewModel::selectRoutine,
                        onCompleteRoutine = childViewModel::completeSelectedRoutine,
                        onAdvanceFromFeedback = childViewModel::advanceFromFeedback,
                        onUndoRoutine = childViewModel::undoLastCompletion,
                        onRequestGuardianMode = guardianViewModel::openFromChild,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationRoutineId.value = intent.notificationRoutineId()
    }

    override fun onDestroy() {
        feedbackController.shutdown()
        super.onDestroy()
    }

    private fun enforceSupportedOrientation() {
        requestedOrientation = if (resources.configuration.smallestScreenWidthDp < 600) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

private fun android.content.Intent?.notificationRoutineId(): String? =
    this?.takeIf { it.action == ACTION_OPEN_ROUTINE }?.getStringExtra(EXTRA_ROUTINE_ID)

private class AndroidFeedbackController(
    private val activity: ComponentActivity,
) {
    private var ttsReady = false
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(activity) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts?.language = Locale.getDefault()
                tts?.setSpeechRate(1.0f)
            }
        }
    }

    fun play(event: ChildRoutineFeedbackEvent) {
        if (event.vibrate) vibrate()
        if (ttsReady) {
            tts?.stop()
            tts?.speak(event.spokenText, TextToSpeech.QUEUE_FLUSH, null, "routine-complete")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            activity.getSystemService(Vibrator::class.java)
        }
        vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
