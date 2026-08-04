package com.example.steppie

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.steppie.di.AppContainer
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.notifications.ACTION_OPEN_ROUTINE
import com.example.steppie.notifications.EXTRA_ROUTINE_ID
import com.example.steppie.presentation.environment.LocalPresentationClockProvider
import com.example.steppie.presentation.environment.LocalPresentationLocaleProvider
import com.example.steppie.presentation.formatting.formatBackupFileName
import com.example.steppie.ui.app.AndroidFeedbackController
import com.example.steppie.ui.app.AppMode
import com.example.steppie.ui.app.NotificationReconcileEffect
import com.example.steppie.ui.app.NotificationReconcileInput
import com.example.steppie.ui.app.PhotoTarget
import com.example.steppie.ui.app.TutorialResolutionEffect
import com.example.steppie.ui.app.resolveAppMode
import com.example.steppie.ui.app.resolveNotificationRoute
import com.example.steppie.ui.app.resolveNotificationTarget
import com.example.steppie.ui.app.resolveTutorialScreen
import com.example.steppie.ui.app.rememberBackupDocumentActions
import com.example.steppie.ui.app.rememberNotificationPermissionActions
import com.example.steppie.ui.app.rememberPhotoActivityActions
import com.example.steppie.ui.child.ChildRoutineScreen
import com.example.steppie.ui.child.ChildRoutineViewModel
import com.example.steppie.ui.guardian.GuardianModeScreen
import com.example.steppie.ui.guardian.GuardianEnvironmentActionCallbacks
import com.example.steppie.ui.guardian.GuardianModeActionCallbacks
import com.example.steppie.ui.guardian.GuardianRecordActionCallbacks
import com.example.steppie.ui.guardian.GuardianRoutineActionCallbacks
import com.example.steppie.ui.guardian.GuardianRoutineSetActionCallbacks
import com.example.steppie.ui.guardian.GuardianSecurityActionCallbacks
import com.example.steppie.ui.guardian.GuardianTemplateActionCallbacks
import com.example.steppie.ui.guardian.GuardianModeViewModel
import com.example.steppie.ui.guardian.GuardianDestination
import com.example.steppie.ui.guardian.GuardianPinMode
import com.example.steppie.ui.theme.SteppieTheme
import com.example.steppie.ui.tutorial.TutorialCoordinatorViewModel
import com.example.steppie.ui.tutorial.TutorialOverlay
import com.example.steppie.ui.tutorial.LocalTutorialAnchorRegistry
import com.example.steppie.ui.tutorial.rememberTutorialAnchorRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { AppContainer(applicationContext) }
    private val notificationRoutineId = MutableStateFlow<String?>(null)
    private lateinit var feedbackController: AndroidFeedbackController

    override fun onCreate(savedInstanceState: Bundle?) {
        enforceSupportedOrientation()
        super.onCreate(savedInstanceState)
        notificationRoutineId.value = intent.notificationRoutineId()
        feedbackController = AndroidFeedbackController(this, appContainer.localeProvider)
        enableEdgeToEdge()
        setContent {
            SteppieTheme {
                val childViewModel: ChildRoutineViewModel = viewModel(
                    factory = ChildRoutineViewModel.factory(
                        appContainer.routineRepository,
                        appContainer.appSettingsRepository,
                        appContainer.clockProvider,
                        appContainer.localeProvider,
                    ),
                )
                val guardianViewModel: GuardianModeViewModel = viewModel(
                    factory = GuardianModeViewModel.factory(
                        appContainer.routineRepository,
                        appContainer.appSettingsRepository,
                        appContainer.clockProvider,
                        appContainer.localeProvider,
                        appContainer.backupProvider,
                        appContainer.routinePhotoStore,
                    ),
                )
                val tutorialViewModel: TutorialCoordinatorViewModel = viewModel(
                    factory = TutorialCoordinatorViewModel.factory(appContainer.tutorialProgressRepository),
                )
                val childState by childViewModel.uiState.collectAsStateWithLifecycle()
                val guardianState by guardianViewModel.uiState.collectAsStateWithLifecycle()
                val tutorialState by tutorialViewModel.uiState.collectAsStateWithLifecycle()
                val tutorialAnchorRegistry = rememberTutorialAnchorRegistry()
                val settings by appContainer.appSettingsRepository.observeAppSettings()
                    .collectAsStateWithLifecycle(initialValue = AppSettings())
                val targetRoutineId by notificationRoutineId.collectAsStateWithLifecycle()
                var notificationPermissionRefresh by remember { mutableIntStateOf(0) }
                var appBootstrapComplete by rememberSaveable { mutableStateOf(false) }
                val notificationPermissionActions = rememberNotificationPermissionActions {
                    notificationPermissionRefresh += 1
                }
                val photoActivityActions = rememberPhotoActivityActions { request ->
                    when (request.target) {
                        PhotoTarget.RoutineDraft -> guardianViewModel.importDraftPhoto(request.uri.toUri())
                        PhotoTarget.RoutineSetStep -> guardianViewModel.importRoutineSetStepPhoto(request.uri.toUri())
                    }
                }
                val backupDocumentActions = rememberBackupDocumentActions(
                    onCreateDocument = guardianViewModel::exportBackup,
                    onOpenDocument = guardianViewModel::previewRestoreBackup,
                )

                LaunchedEffect(childViewModel) {
                    childViewModel.feedbackEvents.collect(feedbackController::play)
                }
                LaunchedEffect(Unit) {
                    val persistedSettings = appContainer.appSettingsRepository.observeAppSettings().first()
                    if (!persistedSettings.hasGuardianPin) guardianViewModel.openInitialSetup()
                    appBootstrapComplete = true
                }
                LaunchedEffect(guardianState.restoreCompletedToken) {
                    if (guardianState.restoreCompletedToken > 0L) {
                        childViewModel.onDataChanged()
                        notificationPermissionRefresh += 1
                    }
                }
                NotificationReconcileEffect(
                    input = NotificationReconcileInput(
                        routines = childState.scheduledRoutines,
                        completedRoutineIds = childState.completedRoutineIds,
                        settings = settings,
                        permissionRefresh = notificationPermissionRefresh,
                    ),
                ) { input ->
                    appContainer.notificationScheduler.reconcileToday(
                        routines = input.routines,
                        completedRoutineIds = input.completedRoutineIds,
                        settings = input.settings,
                        now = appContainer.clockProvider.now(),
                        date = appContainer.clockProvider.today(),
                    )
                }
                LaunchedEffect(targetRoutineId, childState.routines) {
                    val route = resolveNotificationRoute(
                        targetRoutineId = targetRoutineId,
                        visibleRoutineIds = childState.routines.mapTo(mutableSetOf()) { it.id },
                    )
                    route.routineIdToSelect?.let(childViewModel::selectRoutine)
                    if (route.consumeRoute) {
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
                val tutorialScreen = resolveTutorialScreen(
                    guardianActive = guardianState.isActive,
                    guardianDestination = guardianState.destination,
                    guardianPinMode = guardianState.pinMode,
                    guardianOverlayBlocking =
                        guardianState.recoveryStep != null || guardianState.showDailyRoutineSelectionPrompt,
                    childLoading = childState.isLoading,
                    childSinglePane = childState.singlePane,
                )
                TutorialResolutionEffect(
                    screen = tutorialScreen,
                    clearAnchors = tutorialAnchorRegistry::clear,
                    showFor = tutorialViewModel::showFor,
                )

                CompositionLocalProvider(
                    LocalPresentationClockProvider provides appContainer.clockProvider,
                    LocalPresentationLocaleProvider provides appContainer.localeProvider,
                    LocalTutorialAnchorRegistry provides tutorialAnchorRegistry,
                ) {
                val appMode = resolveAppMode(
                    bootstrapComplete = appBootstrapComplete,
                    guardianActive = guardianState.isActive,
                )
                if (appMode != AppMode.Bootstrap) Box(Modifier.fillMaxSize()) {
                if (appMode == AppMode.Guardian) {
                    GuardianModeScreen(
                        state = guardianState,
                        modeActions = GuardianModeActionCallbacks(
                            onDigit = guardianViewModel::inputPinDigit,
                            onDeletePinDigit = guardianViewModel::deletePinDigit,
                            onCloseToChild = guardianViewModel::closeToChild,
                            onInteraction = guardianViewModel::markInteraction,
                            onNavigateBack = guardianViewModel::navigateBack,
                            onOpenHome = guardianViewModel::openHome,
                            onShowOutOfScopeNotice = guardianViewModel::showOutOfScopeNotice,
                            onClearNotice = guardianViewModel::clearNotice,
                        ),
                        routineActions = GuardianRoutineActionCallbacks(
                            onOpenRoutineEdit = guardianViewModel::openRoutineEdit,
                            onOpenNewRoutineEditor = guardianViewModel::openNewRoutineEditor,
                            onOpenRoutineEditor = guardianViewModel::openRoutineEditor,
                            onDraftTitleChange = guardianViewModel::updateDraftTitle,
                            onDraftIconChange = guardianViewModel::updateDraftIcon,
                            onDraftPhotoPick = {
                                photoActivityActions.pick(PhotoTarget.RoutineDraft)
                            },
                            onDraftCameraCapture = {
                                photoActivityActions.capture(PhotoTarget.RoutineDraft)
                            },
                            onDraftPhotoRemove = guardianViewModel::removeDraftPhoto,
                            onDraftColorChange = guardianViewModel::updateDraftColor,
                            onDraftScheduledTimeChange = guardianViewModel::updateDraftScheduledTime,
                            onSaveDraft = guardianViewModel::saveDraft,
                            onRequestDelete = guardianViewModel::requestDelete,
                            onCancelDelete = guardianViewModel::cancelDelete,
                            onConfirmDelete = guardianViewModel::confirmDelete,
                            onMoveRoutine = guardianViewModel::moveRoutine,
                        ),
                        routineSetActions = GuardianRoutineSetActionCallbacks(
                            onOpenRoutineSetCreate = guardianViewModel::openRoutineSetCreate,
                            onToggleRoutineSetListEditing = guardianViewModel::toggleRoutineSetListEditing,
                            onSelectRoutineSet = guardianViewModel::selectRoutineSet,
                            onSetRoutineSetForToday = guardianViewModel::setRoutineSetForToday,
                            onRoutineSetStartTimeChange = guardianViewModel::updateRoutineSetStartTime,
                            onDismissDailyRoutineSelectionPrompt = guardianViewModel::dismissDailyRoutineSelectionPrompt,
                            onRequestEditRoutineSetName = guardianViewModel::requestEditRoutineSetName,
                            onEditingRoutineSetNameChange = guardianViewModel::updateEditingRoutineSetName,
                            onCancelEditRoutineSetName = guardianViewModel::cancelEditRoutineSetName,
                            onSaveEditingRoutineSetName = guardianViewModel::saveEditingRoutineSetName,
                            onRoutineSetNameChange = guardianViewModel::updateRoutineSetName,
                            onRoutineSetStepTitleChange = guardianViewModel::updateRoutineSetStepTitle,
                            onRoutineSetStepIconChange = guardianViewModel::updateRoutineSetStepIcon,
                            onRoutineSetStepPhotoPick = {
                                photoActivityActions.pick(PhotoTarget.RoutineSetStep)
                            },
                            onRoutineSetStepCameraCapture = {
                                photoActivityActions.capture(PhotoTarget.RoutineSetStep)
                            },
                            onRoutineSetStepPhotoRemove = guardianViewModel::removeRoutineSetStepPhoto,
                            onRoutineSetStepColorChange = guardianViewModel::updateRoutineSetStepColor,
                            onRoutineSetStepScheduledTimeChange = guardianViewModel::updateRoutineSetStepScheduledTime,
                            onAddRoutineSetStep = guardianViewModel::addRoutineSetStep,
                            onEditRoutineSetStep = guardianViewModel::editRoutineSetStep,
                            onRemoveRoutineSetStep = guardianViewModel::removeRoutineSetStep,
                            onSaveRoutineSetDraft = guardianViewModel::saveRoutineSetDraft,
                            onRequestDeleteRoutineSet = guardianViewModel::requestDeleteRoutineSet,
                            onConfirmDeleteRoutineSet = guardianViewModel::confirmDeleteRoutineSet,
                        ),
                        templateActions = GuardianTemplateActionCallbacks(
                            onOpenTemplateSelect = guardianViewModel::openTemplateSelect,
                            onOpenTemplateSelectFromHome = guardianViewModel::openTemplateSelectFromHome,
                            onCloseTemplateSelect = guardianViewModel::closeTemplateSelect,
                            onPreviewTemplate = guardianViewModel::previewTemplate,
                            onSaveTemplatePreview = {
                                guardianViewModel.saveTemplatePreview(childViewModel::onDataChanged)
                            },
                        ),
                        recordActions = GuardianRecordActionCallbacks(
                            onOpenRecords = guardianViewModel::openRecords,
                            onOpenRecordsCalendar = guardianViewModel::openRecordsCalendar,
                            onSelectRecordsDate = guardianViewModel::selectRecordsDate,
                            onSelectRecordsCalendarDate = guardianViewModel::selectRecordsCalendarDate,
                            onMoveRecordsCalendarMonth = guardianViewModel::moveRecordsCalendarMonth,
                        ),
                        environmentActions = GuardianEnvironmentActionCallbacks(
                            onOpenEnvironmentSettings = guardianViewModel::openEnvironmentSettings,
                            onFeedbackIntensityChange = guardianViewModel::updateFeedbackIntensity,
                            onTtsEnabledChange = guardianViewModel::updateTtsEnabled,
                            onTtsRateChange = guardianViewModel::updateTtsRate,
                            onTtsVolumeChange = guardianViewModel::updateTtsVolume,
                            onSoundEnabledChange = guardianViewModel::updateSoundEnabled,
                            onHapticEnabledChange = guardianViewModel::updateHapticEnabled,
                            onNotificationLeadTimeChange = { leadMinutes, enabled ->
                                guardianViewModel.updateNotificationLeadTime(leadMinutes, enabled)
                                if (enabled) notificationPermissionActions.requestIfNeeded()
                            },
                            onQuietHoursEnabledChange = guardianViewModel::updateQuietHoursEnabled,
                            onQuietHoursStartChange = guardianViewModel::updateQuietHoursStart,
                            onQuietHoursEndChange = guardianViewModel::updateQuietHoursEnd,
                            onReplayTutorials = tutorialViewModel::resetAll,
                        ),
                        securityActions = GuardianSecurityActionCallbacks(
                            onOpenSecurity = guardianViewModel::openSecurity,
                            onOpenBackupRestore = guardianViewModel::openBackupRestore,
                            onOpenPinChange = guardianViewModel::openPinChange,
                            onOpenRecoveryCode = guardianViewModel::openRecoveryCodeRegeneration,
                            onOpenRecoveryPinReset = guardianViewModel::openRecoveryPinReset,
                            onCreateBackupFile = {
                                backupDocumentActions.create(
                                    formatBackupFileName(
                                        now = appContainer.clockProvider.now(),
                                        zoneId = appContainer.clockProvider.zoneId,
                                    ),
                                )
                            },
                            onOpenRestoreFile = {
                                backupDocumentActions.open()
                            },
                            onRestorePinDigit = guardianViewModel::inputRestorePinDigit,
                            onDeleteRestorePinDigit = guardianViewModel::deleteRestorePinDigit,
                            onCancelRestore = guardianViewModel::cancelRestore,
                            onRecoveryDigit = guardianViewModel::inputRecoveryDigit,
                            onDeleteRecoveryDigit = guardianViewModel::deleteRecoveryDigit,
                            onRecoveryCodeChange = guardianViewModel::updateRecoveryCodeInput,
                            onConfirmRecoveryCode = guardianViewModel::confirmRecoveryCodeForPinReset,
                            onCancelRecoveryPinReset = guardianViewModel::cancelRecoveryPinReset,
                            onCloseRecoveryCode = guardianViewModel::closeRecoveryCode,
                        ),
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
                    if (tutorialState.visible) {
                        TutorialOverlay(
                            state = tutorialState,
                            anchorRegistry = tutorialAnchorRegistry,
                            onPrevious = tutorialViewModel::previous,
                            onNext = tutorialViewModel::next,
                            onSkip = tutorialViewModel::finishCurrent,
                        )
                    }
                }
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
    resolveNotificationTarget(
        action = this?.action,
        routineId = this?.getStringExtra(EXTRA_ROUTINE_ID),
        expectedAction = ACTION_OPEN_ROUTINE,
    )
