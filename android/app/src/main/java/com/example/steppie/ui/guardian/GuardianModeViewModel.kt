package com.example.steppie.ui.guardian

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.steppie.data.backup.BackupImportPreview
import com.example.steppie.data.backup.BackupProvider
import com.example.steppie.data.backup.BackupValidationException
import com.example.steppie.data.photo.RoutinePhotoStore
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.BuiltinIconNames
import com.example.steppie.domain.model.DailyLog
import com.example.steppie.domain.model.FeedbackIntensity
import com.example.steppie.domain.model.IconRef
import com.example.steppie.domain.model.LocalizedText
import com.example.steppie.domain.model.LogStatus
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.model.RoutineColorTokens
import com.example.steppie.domain.model.RoutineSet
import com.example.steppie.domain.model.newUuidV4
import com.example.steppie.domain.repository.AppSettingsRepository
import com.example.steppie.domain.repository.RoutineRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GuardianDestination {
    Pin,
    Home,
    RoutineEdit,
    TemplateSelect,
    CardEdit,
    RoutineSetCreate,
    EnvironmentSettings,
    Records,
    RecordsCalendar,
    Security,
    RecoveryCode,
    BackupRestore,
}

enum class GuardianPinMode { Enter, Setup, SetupConfirm, ChangeCurrent, ChangeNew, RecoveryRegenerateConfirm, RecoveryResetNew }

enum class GuardianRecoveryStep { ShowCode, EnterCodeForPinReset }

enum class GuardianRecoveryError { CodeMismatch }

data class RoutineDraft(
    val routineId: String? = null,
    val title: String = "",
    val icon: IconRef = IconRef.Builtin("star"),
    val colorToken: String = RoutineColorTokens.DEFAULT,
    val scheduledTime: String = "",
) {
    val isNew: Boolean
        get() = routineId == null

    val builtinIconName: String
        get() = (icon as? IconRef.Builtin)?.name ?: "star"
}

data class RoutineSetDraft(
    val name: String = "",
    val stepDraft: RoutineDraft = RoutineDraft(),
    val steps: List<RoutineDraft> = emptyList(),
    val editingStepIndex: Int? = null,
)

data class GuardianRecordDay(
    val date: LocalDate,
    val completedCount: Int,
    val totalCount: Int,
    val hasRecords: Boolean,
) {
    val completionPercent: Int
        get() = if (totalCount == 0) 0 else (completedCount * 100) / totalCount

    val remainingCount: Int
        get() = (totalCount - completedCount).coerceAtLeast(0)
}

data class GuardianRecordRoutine(
    val routineId: String,
    val title: String?,
    val isCompleted: Boolean,
    val completedAt: Instant?,
    val isDeleted: Boolean,
    val isInactive: Boolean,
    val isMissing: Boolean,
)

data class GuardianModeUiState(
    val isActive: Boolean = false,
    val isAuthenticated: Boolean = false,
    val destination: GuardianDestination = GuardianDestination.Pin,
    val destinationBackStack: List<GuardianDestination> = emptyList(),
    val pinMode: GuardianPinMode = GuardianPinMode.Enter,
    val pinDigits: String = "",
    val pinError: String? = null,
    val hasGuardianPin: Boolean = false,
    val appSettings: AppSettings = AppSettings(),
    val routineSets: List<RoutineSet> = emptyList(),
    val activeRoutineSet: RoutineSet? = null,
    val todayRoutineSetId: String? = null,
    val selectedRoutineSetId: String? = null,
    val showDailyRoutineSelectionPrompt: Boolean = false,
    val routines: List<Routine> = emptyList(),
    val draft: RoutineDraft? = null,
    val routineSetDraft: RoutineSetDraft? = null,
    val selectedTemplate: RoutineTemplate? = null,
    val templateReturnDestination: GuardianDestination = GuardianDestination.RoutineEdit,
    val routineSetListEditing: Boolean = false,
    val selectedRecordsDate: LocalDate = LocalDate.now(),
    val recordDays: List<GuardianRecordDay> = emptyList(),
    val selectedRecordRoutines: List<GuardianRecordRoutine> = emptyList(),
    val selectedRecordSummary: GuardianRecordDay = GuardianRecordDay(LocalDate.now(), 0, 0, false),
    val recordsCalendarMonth: YearMonth = YearMonth.now(),
    val selectedCalendarRecordsDate: LocalDate? = null,
    val calendarRecordDates: Set<LocalDate> = emptySet(),
    val selectedCalendarRecordRoutines: List<GuardianRecordRoutine> = emptyList(),
    val selectedCalendarRecordSummary: GuardianRecordDay = GuardianRecordDay(LocalDate.now(), 0, 0, false),
    val editingRoutineSetId: String? = null,
    val editingRoutineSetName: String = "",
    val draftError: String? = null,
    val pendingDeleteRoutineId: String? = null,
    val pendingDeleteRoutineSetId: String? = null,
    val backupInProgress: Boolean = false,
    val backupMessage: String? = null,
    val backupError: String? = null,
    val pendingRestoreUri: Uri? = null,
    val pendingRestorePreview: BackupImportPreview? = null,
    val restorePinDigits: String = "",
    val recoveryStep: GuardianRecoveryStep? = null,
    val recoveryCodeToShow: String? = null,
    val recoveryDigits: String = "",
    val recoveryError: GuardianRecoveryError? = null,
    val recoveryReturnDestination: GuardianDestination = GuardianDestination.Home,
    val notice: String? = null,
    val interactionToken: Long = 0L,
    val restoreCompletedToken: Long = 0L,
) {
    val title: String
        get() = activeRoutineSet?.name?.resolve(null, Locale.getDefault().toLanguageTag()).orEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
class GuardianModeViewModel(
    private val routineRepository: RoutineRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val backupProvider: BackupProvider? = null,
    private val routinePhotoStore: RoutinePhotoStore? = null,
) : ViewModel() {
    private val recordsEndDate = MutableStateFlow(LocalDate.now())
    private val currentDate = MutableStateFlow(LocalDate.now())
    private val _uiState = MutableStateFlow(GuardianModeUiState())
    val uiState: StateFlow<GuardianModeUiState> = _uiState.asStateFlow()
    private var verifiedPinForChange: String? = null
    private var verifiedRecoveryCodeForReset: String? = null
    private var pendingSetupPin: String? = null
    private var recordRoutineSetsCache: List<RoutineSet> = emptyList()
    private var dailyLogsCache: List<DailyLog> = emptyList()
    private var allDailyLogsCache: List<DailyLog> = emptyList()

    init {
        val routineSetsWithTodaySelection = currentDate.flatMapLatest { date ->
            combine(
                routineRepository.observeRoutineSets(),
                routineRepository.observeSelectedRoutineSetId(date),
            ) { routineSets, todayRoutineSetId ->
                TodayRoutineSelectionSnapshot(
                    date = date,
                    routineSets = routineSets,
                    todayRoutineSetId = todayRoutineSetId,
                )
            }
        }
        viewModelScope.launch {
            combine(
                appSettingsRepository.observeAppSettings(),
                routineSetsWithTodaySelection,
                routineRepository.observeRoutineSetsForRecords(),
                recordsEndDate.flatMapLatest { endDate ->
                    routineRepository.observeDailyLogs(endDate.minusDays(6), endDate)
                        .map { dailyLogs -> endDate to dailyLogs }
                },
                routineRepository.observeAllDailyLogs(),
            ) { settings, routineSetsAndTodaySelection, recordRoutineSets, records, allDailyLogs ->
                val (endDate, dailyLogs) = records
                GuardianRepositorySnapshot(
                    settings = settings,
                    today = routineSetsAndTodaySelection.date,
                    visibleRoutineSets = routineSetsAndTodaySelection.routineSets.filter { it.deletedAt == null },
                    todayRoutineSetId = routineSetsAndTodaySelection.todayRoutineSetId,
                    recordRoutineSets = recordRoutineSets,
                    dailyLogs = dailyLogs,
                    allDailyLogs = allDailyLogs,
                    recordsEndDate = endDate,
                )
            }.collect { snapshot ->
                recordRoutineSetsCache = snapshot.recordRoutineSets
                dailyLogsCache = snapshot.dailyLogs
                allDailyLogsCache = snapshot.allDailyLogs
                _uiState.update { state ->
                    val todaySet = snapshot.visibleRoutineSets.firstOrNull { it.id == snapshot.todayRoutineSetId }
                    val selectedSetId = state.selectedRoutineSetId
                        ?.takeIf { selectedId -> snapshot.visibleRoutineSets.any { it.id == selectedId } }
                        ?: todaySet?.id
                        ?: snapshot.visibleRoutineSets.firstOrNull { it.isActive }?.id
                        ?: snapshot.visibleRoutineSets.firstOrNull()?.id
                    val selectedSet = snapshot.visibleRoutineSets.firstOrNull { it.id == selectedSetId }
                    val records = buildGuardianRecords(
                        selectedDate = state.selectedRecordsDate,
                        endDate = snapshot.recordsEndDate,
                        routineSets = snapshot.recordRoutineSets,
                        dailyLogs = snapshot.dailyLogs,
                    )
                    val calendarRecordDates = snapshot.allDailyLogs.map(DailyLog::date).toSet()
                    val selectedCalendarDate = state.selectedCalendarRecordsDate
                    val calendarRecords = buildGuardianRecordDetail(
                        selectedDate = selectedCalendarDate,
                        routineSets = snapshot.recordRoutineSets,
                        dailyLogs = snapshot.allDailyLogs,
                        routineSetForSelectedDate = todaySet.takeIf { selectedCalendarDate == snapshot.today },
                    )
                    val resolvedPinMode = if (
                        state.isActive &&
                        !state.isAuthenticated &&
                        state.destination == GuardianDestination.Pin
                    ) {
                        if (snapshot.settings.hasGuardianPin) {
                            GuardianPinMode.Enter
                        } else if (state.pinMode == GuardianPinMode.SetupConfirm) {
                            GuardianPinMode.SetupConfirm
                        } else {
                            GuardianPinMode.Setup
                        }
                    } else {
                        state.pinMode
                    }
                    val nextState = state.copy(
                        hasGuardianPin = snapshot.settings.hasGuardianPin,
                        appSettings = snapshot.settings,
                        pinMode = resolvedPinMode,
                        routineSets = snapshot.visibleRoutineSets,
                        activeRoutineSet = selectedSet,
                        todayRoutineSetId = todaySet?.id,
                        selectedRoutineSetId = selectedSetId,
                        routines = selectedSet?.routines.orEmpty().sortedBy(Routine::order),
                        recordDays = records.days,
                        selectedRecordRoutines = records.routines,
                        selectedRecordSummary = records.summary,
                        selectedCalendarRecordsDate = selectedCalendarDate,
                        calendarRecordDates = calendarRecordDates,
                        selectedCalendarRecordRoutines = calendarRecords.routines,
                        selectedCalendarRecordSummary = calendarRecords.summary,
                    )
                    nextState.copy(showDailyRoutineSelectionPrompt = nextState.shouldShowDailyRoutineSelectionPrompt())
                }
            }
        }
    }

    fun openFromChild() {
        refreshCurrentDate()
        verifiedPinForChange = null
        verifiedRecoveryCodeForReset = null
        _uiState.update { state ->
            state.copy(
                isActive = true,
                isAuthenticated = false,
                destination = GuardianDestination.Pin,
                destinationBackStack = emptyList(),
                pinMode = if (state.hasGuardianPin) GuardianPinMode.Enter else GuardianPinMode.Setup,
                pinDigits = "",
                pinError = null,
                recoveryStep = null,
                recoveryCodeToShow = null,
                recoveryDigits = "",
                recoveryError = null,
                notice = null,
                interactionToken = state.interactionToken + 1,
            )
        }
    }

    fun openInitialSetup() {
        if (_uiState.value.hasGuardianPin) return
        pendingSetupPin = null
        _uiState.update {
            it.copy(
                isActive = true,
                isAuthenticated = false,
                destination = GuardianDestination.Pin,
                destinationBackStack = emptyList(),
                pinMode = GuardianPinMode.Setup,
                pinDigits = "",
                pinError = null,
                recoveryStep = null,
                recoveryCodeToShow = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun closeToChild() {
        verifiedPinForChange = null
        verifiedRecoveryCodeForReset = null
        _uiState.update {
            GuardianModeUiState(
                hasGuardianPin = it.hasGuardianPin,
                appSettings = it.appSettings,
                routineSets = it.routineSets,
                activeRoutineSet = it.activeRoutineSet,
                todayRoutineSetId = it.todayRoutineSetId,
                selectedRoutineSetId = it.selectedRoutineSetId,
                routines = it.routines,
            )
        }
    }

    fun markInteraction() {
        if (_uiState.value.isActive && _uiState.value.isAuthenticated) {
            _uiState.update { it.copy(interactionToken = it.interactionToken + 1) }
        }
    }

    fun inputPinDigit(digit: Int) {
        require(digit in 0..9)
        val current = _uiState.value
        if (current.pinDigits.length >= 4) return
        val nextDigits = current.pinDigits + digit.toString()
        _uiState.update { it.copy(pinDigits = nextDigits, pinError = null, interactionToken = it.interactionToken + 1) }
        if (nextDigits.length == 4) handleCompletePin(nextDigits)
    }

    fun deletePinDigit() {
        _uiState.update {
            it.copy(
                pinDigits = it.pinDigits.dropLast(1),
                pinError = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun openHome() {
        _uiState.update {
            it.copy(
                destination = GuardianDestination.Home,
                destinationBackStack = emptyList(),
                draft = null,
                routineSetDraft = null,
                selectedTemplate = null,
                templateReturnDestination = GuardianDestination.RoutineEdit,
                routineSetListEditing = false,
                editingRoutineSetId = null,
                editingRoutineSetName = "",
                draftError = null,
                pendingDeleteRoutineId = null,
                pendingDeleteRoutineSetId = null,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun openRoutineEdit() {
        _uiState.update {
            it.copy(
                destination = GuardianDestination.RoutineEdit,
                destinationBackStack = it.backStackFor(GuardianDestination.RoutineEdit),
                draft = null,
                routineSetDraft = null,
                selectedTemplate = null,
                templateReturnDestination = GuardianDestination.RoutineEdit,
                draftError = null,
                pendingDeleteRoutineId = null,
                pendingDeleteRoutineSetId = null,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun openSecurity() {
        verifiedRecoveryCodeForReset = null
        _uiState.update {
            it.copy(
                destination = GuardianDestination.Security,
                destinationBackStack = it.backStackFor(GuardianDestination.Security),
                draft = null,
                routineSetDraft = null,
                selectedTemplate = null,
                templateReturnDestination = GuardianDestination.RoutineEdit,
                routineSetListEditing = false,
                editingRoutineSetId = null,
                editingRoutineSetName = "",
                draftError = null,
                backupError = null,
                backupMessage = null,
                recoveryStep = null,
                recoveryCodeToShow = null,
                recoveryDigits = "",
                recoveryError = null,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun openEnvironmentSettings() {
        _uiState.update {
            it.copy(
                destination = GuardianDestination.EnvironmentSettings,
                destinationBackStack = it.backStackFor(GuardianDestination.EnvironmentSettings),
                draft = null,
                routineSetDraft = null,
                selectedTemplate = null,
                templateReturnDestination = GuardianDestination.RoutineEdit,
                routineSetListEditing = false,
                editingRoutineSetId = null,
                editingRoutineSetName = "",
                draftError = null,
                pendingDeleteRoutineId = null,
                pendingDeleteRoutineSetId = null,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun openRecords() {
        val today = refreshCurrentDate()
        recordsEndDate.value = today
        _uiState.update {
            it.copy(
                destination = GuardianDestination.Records,
                destinationBackStack = it.backStackFor(GuardianDestination.Records),
                selectedRecordsDate = today,
                draft = null,
                routineSetDraft = null,
                selectedTemplate = null,
                templateReturnDestination = GuardianDestination.RoutineEdit,
                routineSetListEditing = false,
                editingRoutineSetId = null,
                editingRoutineSetName = "",
                draftError = null,
                pendingDeleteRoutineId = null,
                pendingDeleteRoutineSetId = null,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun openRecordsCalendar() {
        val today = refreshCurrentDate()
        val current = _uiState.value
        val records = buildGuardianRecordDetail(
            selectedDate = today,
            routineSets = recordRoutineSetsCache,
            dailyLogs = allDailyLogsCache,
            routineSetForSelectedDate = current.activeRoutineSet.takeIf { current.todayRoutineSetId == it?.id },
        )
        _uiState.update {
            it.copy(
                destination = GuardianDestination.RecordsCalendar,
                destinationBackStack = it.backStackFor(GuardianDestination.RecordsCalendar),
                recordsCalendarMonth = YearMonth.from(today),
                selectedCalendarRecordsDate = today,
                selectedCalendarRecordRoutines = records.routines,
                selectedCalendarRecordSummary = records.summary,
                draft = null,
                routineSetDraft = null,
                selectedTemplate = null,
                templateReturnDestination = GuardianDestination.RoutineEdit,
                routineSetListEditing = false,
                editingRoutineSetId = null,
                editingRoutineSetName = "",
                draftError = null,
                pendingDeleteRoutineId = null,
                pendingDeleteRoutineSetId = null,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun selectRecordsDate(date: LocalDate) {
        val current = _uiState.value
        if (current.recordDays.none { it.date == date }) return
        val records = buildGuardianRecords(
            selectedDate = date,
            endDate = recordsEndDate.value,
            routineSets = recordRoutineSetsCache,
            dailyLogs = dailyLogsCache,
        )
        _uiState.update {
            it.copy(
                selectedRecordsDate = date,
                recordDays = records.days,
                selectedRecordRoutines = records.routines,
                selectedRecordSummary = records.summary,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun selectRecordsCalendarDate(date: LocalDate) {
        val current = _uiState.value
        val today = currentDate.value
        val canSelectTodayRoutine = date == today && current.todayRoutineSetId != null
        if (date !in current.calendarRecordDates && !canSelectTodayRoutine) return
        val records = buildGuardianRecordDetail(
            selectedDate = date,
            routineSets = recordRoutineSetsCache,
            dailyLogs = allDailyLogsCache,
            routineSetForSelectedDate = current.activeRoutineSet.takeIf {
                date == today && current.todayRoutineSetId == it?.id
            },
        )
        _uiState.update {
            it.copy(
                recordsCalendarMonth = YearMonth.from(date),
                selectedCalendarRecordsDate = date,
                selectedCalendarRecordRoutines = records.routines,
                selectedCalendarRecordSummary = records.summary,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun moveRecordsCalendarMonth(monthDelta: Long) {
        _uiState.update {
            it.copy(
                recordsCalendarMonth = it.recordsCalendarMonth.plusMonths(monthDelta),
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun updateFeedbackIntensity(intensity: FeedbackIntensity) = updateSettings {
        it.copy(feedbackIntensity = intensity)
    }

    fun updateTtsEnabled(enabled: Boolean) = updateSettings {
        it.copy(ttsEnabled = enabled)
    }

    fun updateTtsRate(rate: Double) = updateSettings {
        it.copy(ttsRate = rate)
    }

    fun updateTtsVolume(volume: Double) = updateSettings {
        it.copy(ttsVolume = volume)
    }

    fun updateSoundEnabled(enabled: Boolean) = updateSettings {
        it.copy(soundEnabled = enabled)
    }

    fun updateHapticEnabled(enabled: Boolean) = updateSettings {
        it.copy(hapticEnabled = enabled)
    }

    fun updateNotificationLeadTime(leadMinutes: Int, enabled: Boolean) {
        require(leadMinutes in setOf(10, 5))
        updateSettings { settings ->
            val leadTimes = if (enabled) {
                (settings.notificationLeadTimes + leadMinutes).distinct()
            } else {
                settings.notificationLeadTimes.filterNot { it == leadMinutes }
            }.sortedDescending()
            settings.copy(notificationLeadTimes = leadTimes)
        }
    }

    fun updateQuietHoursEnabled(enabled: Boolean) = updateSettings {
        if (enabled) {
            it.copy(
                quietHoursStart = it.quietHoursStart ?: LocalTime.of(21, 0),
                quietHoursEnd = it.quietHoursEnd ?: LocalTime.of(7, 0),
            )
        } else {
            it.copy(quietHoursStart = null, quietHoursEnd = null)
        }
    }

    fun updateQuietHoursStart(value: String) {
        val time = parseScheduledTime(value) ?: return
        updateSettings { it.copy(quietHoursStart = time) }
    }

    fun updateQuietHoursEnd(value: String) {
        val time = parseScheduledTime(value) ?: return
        updateSettings { it.copy(quietHoursEnd = time) }
    }

    fun openBackupRestore() {
        _uiState.update {
            it.copy(
                destination = GuardianDestination.BackupRestore,
                destinationBackStack = it.backStackFor(GuardianDestination.BackupRestore),
                draft = null,
                routineSetDraft = null,
                selectedTemplate = null,
                templateReturnDestination = GuardianDestination.RoutineEdit,
                routineSetListEditing = false,
                editingRoutineSetId = null,
                editingRoutineSetName = "",
                draftError = null,
                backupError = null,
                backupMessage = null,
                pendingRestoreUri = null,
                pendingRestorePreview = null,
                restorePinDigits = "",
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun openPinChange() {
        verifiedPinForChange = null
        verifiedRecoveryCodeForReset = null
        _uiState.update {
            it.copy(
                destination = GuardianDestination.Pin,
                destinationBackStack = it.backStackFor(GuardianDestination.Pin),
                pinMode = GuardianPinMode.ChangeCurrent,
                pinDigits = "",
                pinError = null,
                recoveryStep = null,
                recoveryCodeToShow = null,
                recoveryDigits = "",
                recoveryError = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun openRecoveryCodeRegeneration() {
        verifiedPinForChange = null
        verifiedRecoveryCodeForReset = null
        _uiState.update {
            it.copy(
                destination = GuardianDestination.Pin,
                destinationBackStack = it.backStackFor(GuardianDestination.Pin),
                pinMode = GuardianPinMode.RecoveryRegenerateConfirm,
                pinDigits = "",
                pinError = null,
                recoveryStep = null,
                recoveryCodeToShow = null,
                recoveryDigits = "",
                recoveryError = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun openRecoveryPinReset() {
        verifiedPinForChange = null
        verifiedRecoveryCodeForReset = null
        _uiState.update {
            it.copy(
                destination = GuardianDestination.RecoveryCode,
                destinationBackStack = it.backStackFor(GuardianDestination.RecoveryCode),
                recoveryStep = GuardianRecoveryStep.EnterCodeForPinReset,
                recoveryCodeToShow = null,
                recoveryDigits = "",
                recoveryError = null,
                pinDigits = "",
                pinError = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun closeRecoveryCode() {
        verifiedRecoveryCodeForReset = null
        _uiState.update {
            if (it.pinMode == GuardianPinMode.SetupConfirm) {
                pendingSetupPin = null
                return@update GuardianModeUiState(
                    hasGuardianPin = true,
                    appSettings = it.appSettings,
                    routineSets = it.routineSets,
                    activeRoutineSet = it.activeRoutineSet,
                    todayRoutineSetId = it.todayRoutineSetId,
                    selectedRoutineSetId = it.selectedRoutineSetId,
                    routines = it.routines,
                )
            }
            val destination = it.recoveryReturnDestination
            val nextState = it.copy(
                isAuthenticated = true,
                destination = destination,
                recoveryStep = null,
                recoveryCodeToShow = null,
                recoveryDigits = "",
                recoveryError = null,
                pinDigits = "",
                pinError = null,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
            nextState.copy(showDailyRoutineSelectionPrompt = nextState.shouldShowDailyRoutineSelectionPrompt())
        }
    }

    fun inputRecoveryDigit(digit: Int) {
        require(digit in 0..9)
        val current = _uiState.value
        if (current.recoveryStep != GuardianRecoveryStep.EnterCodeForPinReset) return
        if (current.recoveryDigits.length >= 6) return
        updateRecoveryCodeInput(current.recoveryDigits + digit.toString())
    }

    fun updateRecoveryCodeInput(value: String) {
        val digits = value.filter(Char::isDigit).take(6)
        _uiState.update {
            it.copy(
                recoveryDigits = digits,
                recoveryError = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun deleteRecoveryDigit() {
        updateRecoveryCodeInput(_uiState.value.recoveryDigits.dropLast(1))
    }

    fun confirmRecoveryCodeForPinReset() {
        val recoveryCode = _uiState.value.recoveryDigits
        if (recoveryCode.length != 6) return
        verifyRecoveryCodeForPinReset(recoveryCode)
    }

    fun cancelRecoveryPinReset() {
        verifiedRecoveryCodeForReset = null
        _uiState.update {
            it.copy(
                destination = if (it.isAuthenticated) GuardianDestination.Security else GuardianDestination.Pin,
                pinMode = if (it.hasGuardianPin) GuardianPinMode.Enter else GuardianPinMode.Setup,
                pinDigits = "",
                pinError = null,
                recoveryStep = null,
                recoveryCodeToShow = null,
                recoveryDigits = "",
                recoveryError = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun openNewRoutineEditor() {
        _uiState.update {
            it.copy(
                destination = GuardianDestination.CardEdit,
                destinationBackStack = it.backStackFor(GuardianDestination.CardEdit),
                draft = RoutineDraft(),
                routineSetDraft = null,
                selectedTemplate = null,
                templateReturnDestination = GuardianDestination.RoutineEdit,
                draftError = null,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun openRoutineEditor(routineId: String) {
        val routine = _uiState.value.routines.firstOrNull { it.id == routineId } ?: return
        val title = routine.title.resolve(null, Locale.getDefault().toLanguageTag())
        _uiState.update {
            it.copy(
                destination = GuardianDestination.CardEdit,
                destinationBackStack = it.backStackFor(GuardianDestination.CardEdit),
                routineSetDraft = null,
                selectedTemplate = null,
                templateReturnDestination = GuardianDestination.RoutineEdit,
                draft = RoutineDraft(
                    routineId = routine.id,
                    title = title,
                    icon = routine.icon,
                    colorToken = routine.colorToken,
                    scheduledTime = routine.scheduledTime?.toString().orEmpty(),
                ),
                draftError = null,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun openRoutineSetCreate() {
        _uiState.update {
            it.copy(
                destination = GuardianDestination.RoutineSetCreate,
                destinationBackStack = it.backStackFor(GuardianDestination.RoutineSetCreate),
                draft = null,
                routineSetDraft = RoutineSetDraft(),
                selectedTemplate = null,
                templateReturnDestination = GuardianDestination.RoutineEdit,
                draftError = null,
                pendingDeleteRoutineId = null,
                pendingDeleteRoutineSetId = null,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun openTemplateSelect(returnDestination: GuardianDestination = GuardianDestination.RoutineEdit) {
        _uiState.update {
            it.copy(
                destination = GuardianDestination.TemplateSelect,
                destinationBackStack = it.backStackFor(GuardianDestination.TemplateSelect),
                draft = null,
                routineSetDraft = null,
                selectedTemplate = RoutineTemplates.find(RoutineTemplateId.Morning),
                templateReturnDestination = returnDestination,
                draftError = null,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun openTemplateSelectFromHome() {
        openTemplateSelect(GuardianDestination.Home)
    }

    fun closeTemplateSelect() {
        val destination = _uiState.value.templateReturnDestination
        _uiState.update {
            it.copy(
                destination = destination,
                destinationBackStack = it.destinationBackStack.dropLastMatching(destination),
                selectedTemplate = null,
                draftError = null,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun previewTemplate(templateId: RoutineTemplateId) {
        val template = RoutineTemplates.find(templateId) ?: return
        _uiState.update {
            it.copy(
                destination = GuardianDestination.TemplateSelect,
                selectedTemplate = template,
                draft = null,
                routineSetDraft = null,
                draftError = null,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun saveTemplatePreview(onDataChanged: () -> Unit) {
        val template = _uiState.value.selectedTemplate ?: return
        viewModelScope.launch {
            runCatching {
                routineRepository.createRoutineSet(template.instantiate(Instant.now()))
            }.onSuccess {
                onDataChanged()
                _uiState.update { state ->
                    state.copy(
                        destination = GuardianDestination.RoutineEdit,
                        destinationBackStack = state.destinationBackStack.dropLastMatching(GuardianDestination.RoutineEdit),
                        selectedTemplate = null,
                        draft = null,
                        routineSetDraft = null,
                        draftError = null,
                        notice = "템플릿으로 새 루틴 세트를 저장했습니다.",
                        interactionToken = state.interactionToken + 1,
                    )
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        draftError = error.message ?: "템플릿을 저장할 수 없습니다.",
                        interactionToken = state.interactionToken + 1,
                    )
                }
            }
        }
    }

    fun toggleRoutineSetListEditing() {
        _uiState.update {
            it.copy(
                routineSetListEditing = !it.routineSetListEditing,
                editingRoutineSetId = null,
                editingRoutineSetName = "",
                pendingDeleteRoutineSetId = null,
                draftError = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun selectRoutineSet(routineSetId: String) {
        val routineSet = _uiState.value.routineSets.firstOrNull { it.id == routineSetId } ?: return
        _uiState.update {
            it.copy(
                activeRoutineSet = routineSet,
                selectedRoutineSetId = routineSet.id,
                routines = routineSet.routines.sortedBy(Routine::order),
                draftError = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun setRoutineSetForToday(routineSetId: String) {
        if (_uiState.value.routineSets.none { it.id == routineSetId }) return
        viewModelScope.launch {
            val today = refreshCurrentDate()
            routineRepository.selectRoutineSetForDate(today, routineSetId)
            _uiState.update {
                it.copy(
                    todayRoutineSetId = routineSetId,
                    selectedRoutineSetId = routineSetId,
                    showDailyRoutineSelectionPrompt = false,
                    notice = "오늘의 루틴으로 설정했습니다.",
                    interactionToken = it.interactionToken + 1,
                )
            }
        }
    }

    fun dismissDailyRoutineSelectionPrompt() {
        _uiState.update {
            it.copy(
                showDailyRoutineSelectionPrompt = false,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    private fun refreshCurrentDate(): LocalDate {
        val today = LocalDate.now()
        if (currentDate.value != today) {
            currentDate.value = today
        }
        return today
    }

    fun requestEditRoutineSetName(routineSetId: String) {
        val routineSet = _uiState.value.routineSets.firstOrNull { it.id == routineSetId } ?: return
        _uiState.update {
            it.copy(
                editingRoutineSetId = routineSet.id,
                editingRoutineSetName = routineSet.name.resolve(null, Locale.getDefault().toLanguageTag()),
                draftError = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun updateEditingRoutineSetName(name: String) {
        _uiState.update {
            it.copy(
                editingRoutineSetName = name,
                draftError = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun cancelEditRoutineSetName() {
        _uiState.update {
            it.copy(
                editingRoutineSetId = null,
                editingRoutineSetName = "",
                draftError = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun saveEditingRoutineSetName() {
        val state = _uiState.value
        val routineSetId = state.editingRoutineSetId ?: return
        val routineSet = state.routineSets.firstOrNull { it.id == routineSetId } ?: return
        val trimmedName = state.editingRoutineSetName.trim()
        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(draftError = "루틴 세트 이름을 입력해 주세요.") }
            return
        }
        viewModelScope.launch {
            routineRepository.updateRoutineSet(
                routineSet.copy(
                    name = LocalizedText(mapOf(Locale.getDefault().toLanguageTag() to trimmedName)),
                    updatedAt = Instant.now(),
                ),
            )
            cancelEditRoutineSetName()
        }
    }

    fun updateDraftTitle(title: String) = updateDraft { it.copy(title = title) }

    fun updateDraftIcon(iconName: String) {
        if (iconName !in BuiltinIconNames.all) return
        updateDraft { it.copy(icon = IconRef.Builtin(iconName)) }
    }

    fun importDraftPhoto(uri: Uri) {
        val store = routinePhotoStore ?: run {
            _uiState.update { it.copy(draftError = "사진 선택 기능을 사용할 수 없습니다.") }
            return
        }
        viewModelScope.launch {
            runCatching { store.importPhoto(uri) }
                .onSuccess { photo -> updateDraft { it.copy(icon = photo) } }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            draftError = error.message ?: "사진을 저장할 수 없습니다.",
                            interactionToken = it.interactionToken + 1,
                        )
                    }
                }
        }
    }

    fun removeDraftPhoto() {
        updateDraft { it.copy(icon = IconRef.Builtin("star")) }
    }

    fun updateDraftColor(colorToken: String) {
        if (colorToken !in RoutineColorTokens.all) return
        updateDraft { it.copy(colorToken = colorToken) }
    }

    fun updateDraftScheduledTime(value: String) {
        val sanitized = value.filter { it.isDigit() || it == ':' }.take(5)
        updateDraft { it.copy(scheduledTime = sanitized) }
    }

    fun updateRoutineSetName(name: String) = updateRoutineSetDraft { it.copy(name = name) }

    fun updateRoutineSetStepTitle(title: String) = updateRoutineSetDraft {
        it.copy(stepDraft = it.stepDraft.copy(title = title))
    }

    fun updateRoutineSetStepIcon(iconName: String) {
        if (iconName !in BuiltinIconNames.all) return
        updateRoutineSetDraft { it.copy(stepDraft = it.stepDraft.copy(icon = IconRef.Builtin(iconName))) }
    }

    fun importRoutineSetStepPhoto(uri: Uri) {
        val store = routinePhotoStore ?: run {
            _uiState.update { it.copy(draftError = "사진 선택 기능을 사용할 수 없습니다.") }
            return
        }
        viewModelScope.launch {
            runCatching { store.importPhoto(uri) }
                .onSuccess { photo -> updateRoutineSetDraft { it.copy(stepDraft = it.stepDraft.copy(icon = photo)) } }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            draftError = error.message ?: "사진을 저장할 수 없습니다.",
                            interactionToken = it.interactionToken + 1,
                        )
                    }
                }
        }
    }

    fun removeRoutineSetStepPhoto() {
        updateRoutineSetDraft { it.copy(stepDraft = it.stepDraft.copy(icon = IconRef.Builtin("star"))) }
    }

    fun updateRoutineSetStepColor(colorToken: String) {
        if (colorToken !in RoutineColorTokens.all) return
        updateRoutineSetDraft { it.copy(stepDraft = it.stepDraft.copy(colorToken = colorToken)) }
    }

    fun updateRoutineSetStepScheduledTime(value: String) {
        val sanitized = value.filter { it.isDigit() || it == ':' }.take(5)
        updateRoutineSetDraft { it.copy(stepDraft = it.stepDraft.copy(scheduledTime = sanitized)) }
    }

    fun addRoutineSetStep() {
        val draft = _uiState.value.routineSetDraft ?: return
        val trimmedTitle = draft.stepDraft.title.trim()
        if (trimmedTitle.isBlank()) {
            _uiState.update { it.copy(draftError = "단계 이름을 입력해 주세요.") }
            return
        }
        if (parseScheduledTime(draft.stepDraft.scheduledTime) == null && draft.stepDraft.scheduledTime.isNotBlank()) {
            _uiState.update { it.copy(draftError = "예정 시각은 HH:mm 형식으로 입력해 주세요.") }
            return
        }
        _uiState.update {
            val currentDraft = it.routineSetDraft ?: return@update it
            it.copy(
                routineSetDraft = currentDraft.copy(
                    stepDraft = RoutineDraft(),
                    steps = currentDraft.editingStepIndex?.let { index ->
                        currentDraft.steps.mapIndexed { stepIndex, step ->
                            if (stepIndex == index) currentDraft.stepDraft.copy(title = trimmedTitle) else step
                        }
                    } ?: (currentDraft.steps + currentDraft.stepDraft.copy(title = trimmedTitle)),
                    editingStepIndex = null,
                ),
                draftError = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun editRoutineSetStep(index: Int) {
        _uiState.update {
            val currentDraft = it.routineSetDraft ?: return@update it
            val step = currentDraft.steps.getOrNull(index) ?: return@update it
            it.copy(
                routineSetDraft = currentDraft.copy(
                    stepDraft = step,
                    editingStepIndex = index,
                ),
                draftError = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun removeRoutineSetStep(index: Int) {
        _uiState.update {
            val currentDraft = it.routineSetDraft ?: return@update it
            if (index !in currentDraft.steps.indices) return@update it
            it.copy(
                routineSetDraft = currentDraft.copy(
                    steps = currentDraft.steps.filterIndexed { stepIndex, _ -> stepIndex != index },
                    stepDraft = if (currentDraft.editingStepIndex == index) RoutineDraft() else currentDraft.stepDraft,
                    editingStepIndex = null,
                ),
                draftError = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun saveRoutineSetDraft() {
        val draft = _uiState.value.routineSetDraft ?: return
        val now = Instant.now()
        val routineSet = runCatching {
            buildRoutineSetFromDraft(
                draft = draft,
                now = now,
                localeTag = Locale.getDefault().toLanguageTag(),
            )
        }.getOrElse { error ->
            _uiState.update { it.copy(draftError = error.message ?: "루틴 세트를 저장할 수 없습니다.") }
            return
        }

        viewModelScope.launch {
            routineRepository.createRoutineSet(routineSet)
            returnToRoutineEdit()
        }
    }

    fun saveDraft() {
        val state = _uiState.value
        val draft = state.draft ?: return
        val activeSet = state.activeRoutineSet
        if (activeSet == null) {
            _uiState.update { it.copy(draftError = "먼저 루틴 세트를 생성해 주세요.") }
            return
        }
        val trimmedTitle = draft.title.trim()
        if (trimmedTitle.isBlank()) {
            _uiState.update { it.copy(draftError = "활동 이름을 입력해 주세요.") }
            return
        }
        val scheduledTime = parseScheduledTime(draft.scheduledTime)
        if (scheduledTime == null && draft.scheduledTime.isNotBlank()) {
            _uiState.update { it.copy(draftError = "예정 시각은 HH:mm 형식으로 입력해 주세요.") }
            return
        }

        viewModelScope.launch {
            val now = Instant.now()
            val localizedTitle = LocalizedText(mapOf(Locale.getDefault().toLanguageTag() to trimmedTitle))
            if (draft.isNew) {
                routineRepository.createRoutine(
                    Routine(
                        routineSetId = activeSet.id,
                        title = localizedTitle,
                        icon = draft.icon,
                        colorToken = draft.colorToken,
                        order = state.routines.size,
                        scheduledTime = scheduledTime,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            } else {
                val existing = state.routines.firstOrNull { it.id == draft.routineId } ?: return@launch
                routineRepository.updateRoutine(
                    existing.copy(
                        title = localizedTitle,
                        icon = draft.icon,
                        colorToken = draft.colorToken,
                        scheduledTime = scheduledTime,
                        updatedAt = now,
                    ),
                )
            }
            returnToRoutineEdit()
        }
    }

    private fun returnToRoutineEdit() {
        _uiState.update {
            it.copy(
                destination = GuardianDestination.RoutineEdit,
                destinationBackStack = it.destinationBackStack.dropLastMatching(GuardianDestination.RoutineEdit),
                draft = null,
                routineSetDraft = null,
                selectedTemplate = null,
                templateReturnDestination = GuardianDestination.RoutineEdit,
                draftError = null,
                pendingDeleteRoutineId = null,
                pendingDeleteRoutineSetId = null,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun requestDelete(routineId: String) {
        _uiState.update {
            it.copy(
                pendingDeleteRoutineId = routineId,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun requestDeleteRoutineSet(routineSetId: String) {
        _uiState.update {
            it.copy(
                pendingDeleteRoutineSetId = routineSetId,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun cancelDelete() {
        _uiState.update {
            it.copy(
                pendingDeleteRoutineId = null,
                pendingDeleteRoutineSetId = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun confirmDelete() {
        val routineId = _uiState.value.pendingDeleteRoutineId ?: return
        viewModelScope.launch {
            routineRepository.deleteRoutine(routineId)
            _uiState.update {
                it.copy(
                    destination = GuardianDestination.RoutineEdit,
                    destinationBackStack = it.destinationBackStack.dropLastMatching(GuardianDestination.RoutineEdit),
                    draft = null,
                    draftError = null,
                    pendingDeleteRoutineId = null,
                    interactionToken = it.interactionToken + 1,
                )
            }
        }
    }

    fun confirmDeleteRoutineSet() {
        val state = _uiState.value
        val routineSetId = state.pendingDeleteRoutineSetId ?: return
        val target = state.routineSets.firstOrNull { it.id == routineSetId } ?: return
        if (state.routineSets.size <= 1) {
            _uiState.update {
                it.copy(
                    pendingDeleteRoutineSetId = null,
                    notice = "마지막 루틴 세트는 삭제할 수 없습니다.",
                    interactionToken = it.interactionToken + 1,
                )
            }
            return
        }
        viewModelScope.launch {
            if (target.isActive) {
                state.routineSets.firstOrNull { it.id != target.id }?.let { replacement ->
                    routineRepository.updateRoutineSet(replacement.copy(isActive = true, updatedAt = Instant.now()))
                }
            }
            routineRepository.deleteRoutineSet(target.id)
            _uiState.update {
                it.copy(
                    pendingDeleteRoutineSetId = null,
                    routineSetListEditing = true,
                    interactionToken = it.interactionToken + 1,
                )
            }
        }
    }

    fun moveRoutine(routineId: String, direction: Int) {
        val state = _uiState.value
        val setId = state.activeRoutineSet?.id ?: return
        val index = state.routines.indexOfFirst { it.id == routineId }
        val target = index + direction
        if (index !in state.routines.indices || target !in state.routines.indices) return
        val ids = state.routines.map { it.id }.toMutableList()
        val moved = ids.removeAt(index)
        ids.add(target, moved)
        viewModelScope.launch {
            routineRepository.reorderRoutines(setId, ids)
        }
    }

    fun showOutOfScopeNotice() {
        _uiState.update {
            it.copy(
                notice = "이 기능은 이후 스프린트에서 구현합니다.",
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    fun navigateBack() {
        _uiState.update {
            val destination = it.destinationBackStack.lastOrNull() ?: GuardianDestination.Home
            it.copy(
                destination = destination,
                destinationBackStack = it.destinationBackStack.dropLast(1),
                draft = null,
                routineSetDraft = null,
                selectedTemplate = null,
                templateReturnDestination = GuardianDestination.RoutineEdit,
                routineSetListEditing = false,
                editingRoutineSetId = null,
                editingRoutineSetName = "",
                draftError = null,
                pendingDeleteRoutineId = null,
                pendingDeleteRoutineSetId = null,
                backupError = null,
                backupMessage = null,
                pendingRestoreUri = null,
                pendingRestorePreview = null,
                restorePinDigits = "",
                recoveryStep = null,
                recoveryCodeToShow = null,
                recoveryDigits = "",
                recoveryError = null,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun exportBackup(uri: Uri) {
        val provider = backupProvider ?: run {
            _uiState.update { it.copy(backupError = "백업 기능을 사용할 수 없습니다.") }
            return
        }
        _uiState.update {
            it.copy(
                backupInProgress = true,
                backupError = null,
                backupMessage = null,
                interactionToken = it.interactionToken + 1,
            )
        }
        viewModelScope.launch {
            runCatching { provider.exportTo(uri) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            backupInProgress = false,
                            backupMessage = "백업 파일을 저장했습니다.",
                            interactionToken = it.interactionToken + 1,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            backupInProgress = false,
                            backupError = backupErrorMessage(error),
                            interactionToken = it.interactionToken + 1,
                        )
                    }
                }
        }
    }

    fun previewRestoreBackup(uri: Uri) {
        val provider = backupProvider ?: run {
            _uiState.update { it.copy(backupError = "복원 기능을 사용할 수 없습니다.") }
            return
        }
        _uiState.update {
            it.copy(
                backupInProgress = true,
                backupError = null,
                backupMessage = null,
                pendingRestoreUri = null,
                pendingRestorePreview = null,
                restorePinDigits = "",
                interactionToken = it.interactionToken + 1,
            )
        }
        viewModelScope.launch {
            runCatching { provider.previewImport(uri) }
                .onSuccess { preview ->
                    _uiState.update {
                        it.copy(
                            backupInProgress = false,
                            pendingRestoreUri = uri,
                            pendingRestorePreview = preview,
                            restorePinDigits = "",
                            interactionToken = it.interactionToken + 1,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            backupInProgress = false,
                            backupError = backupErrorMessage(error),
                            interactionToken = it.interactionToken + 1,
                        )
                    }
                }
        }
    }

    fun inputRestorePinDigit(digit: Int) {
        require(digit in 0..9)
        val current = _uiState.value
        if (current.restorePinDigits.length >= 4) return
        val nextDigits = current.restorePinDigits + digit.toString()
        _uiState.update {
            it.copy(
                restorePinDigits = nextDigits,
                backupError = null,
                interactionToken = it.interactionToken + 1,
            )
        }
        if (nextDigits.length == 4) restoreWithPin(nextDigits)
    }

    fun deleteRestorePinDigit() {
        _uiState.update {
            it.copy(
                restorePinDigits = it.restorePinDigits.dropLast(1),
                backupError = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun cancelRestore() {
        _uiState.update {
            it.copy(
                pendingRestoreUri = null,
                pendingRestorePreview = null,
                restorePinDigits = "",
                backupError = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    private fun restoreWithPin(pin: String) {
        val provider = backupProvider ?: return
        val uri = _uiState.value.pendingRestoreUri ?: return
        _uiState.update { it.copy(backupInProgress = true, backupError = null) }
        viewModelScope.launch {
            if (!appSettingsRepository.verifyGuardianPin(pin)) {
                _uiState.update {
                    it.copy(
                        backupInProgress = false,
                        restorePinDigits = "",
                        backupError = "PIN이 맞지 않아요. 다시 입력해 주세요.",
                        interactionToken = it.interactionToken + 1,
                    )
                }
                return@launch
            }
            runCatching { provider.restoreReplace(uri) }
                .onSuccess {
                    _uiState.update {
                        GuardianModeUiState(
                            backupMessage = "백업 파일에서 복원했습니다.",
                            interactionToken = it.interactionToken + 1,
                            restoreCompletedToken = it.restoreCompletedToken + 1,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            backupInProgress = false,
                            restorePinDigits = "",
                            backupError = backupErrorMessage(error),
                            interactionToken = it.interactionToken + 1,
                        )
                    }
                }
        }
    }

    private fun handleCompletePin(pin: String) {
        viewModelScope.launch {
            when (_uiState.value.pinMode) {
                GuardianPinMode.Enter -> {
                    if (appSettingsRepository.verifyGuardianPin(pin)) {
                        refreshCurrentDate()
                        _uiState.update {
                            val nextState = it.copy(
                                isAuthenticated = true,
                                destination = GuardianDestination.Home,
                                destinationBackStack = emptyList(),
                                pinDigits = "",
                                pinError = null,
                                interactionToken = it.interactionToken + 1,
                            )
                            nextState.copy(showDailyRoutineSelectionPrompt = nextState.shouldShowDailyRoutineSelectionPrompt())
                        }
                    } else {
                        showPinError()
                    }
                }
                GuardianPinMode.Setup -> {
                    pendingSetupPin = pin
                    _uiState.update {
                        it.copy(
                            pinMode = GuardianPinMode.SetupConfirm,
                            pinDigits = "",
                            pinError = null,
                            interactionToken = it.interactionToken + 1,
                        )
                    }
                }
                GuardianPinMode.SetupConfirm -> {
                    val firstPin = pendingSetupPin
                    if (firstPin == null || firstPin != pin) {
                        pendingSetupPin = null
                        _uiState.update {
                            it.copy(
                                pinMode = GuardianPinMode.Setup,
                                pinDigits = "",
                                pinError = "PIN이 일치하지 않아요. 처음부터 다시 입력해 주세요.",
                                interactionToken = it.interactionToken + 1,
                            )
                        }
                        return@launch
                    }
                    val recoveryCode = appSettingsRepository.setGuardianPin(pin)
                    refreshCurrentDate()
                    _uiState.update {
                        it.copy(
                            isAuthenticated = true,
                            destination = GuardianDestination.Home,
                            destinationBackStack = emptyList(),
                            pinDigits = "",
                            pinError = null,
                            recoveryStep = GuardianRecoveryStep.ShowCode,
                            recoveryCodeToShow = recoveryCode,
                            recoveryDigits = "",
                            recoveryError = null,
                            recoveryReturnDestination = GuardianDestination.Home,
                            interactionToken = it.interactionToken + 1,
                        )
                    }
                }
                GuardianPinMode.ChangeCurrent -> {
                    if (appSettingsRepository.verifyGuardianPin(pin)) {
                        verifiedPinForChange = pin
                        _uiState.update {
                            it.copy(
                                pinMode = GuardianPinMode.ChangeNew,
                                pinDigits = "",
                                pinError = null,
                                interactionToken = it.interactionToken + 1,
                            )
                        }
                    } else {
                        showPinError()
                    }
                }
                GuardianPinMode.ChangeNew -> {
                    val currentPin = verifiedPinForChange
                    val recoveryCode = currentPin?.let { appSettingsRepository.changeGuardianPin(it, pin) }
                    if (recoveryCode != null) {
                        verifiedPinForChange = null
                        _uiState.update {
                            it.copy(
                                isAuthenticated = true,
                                destination = GuardianDestination.Security,
                                destinationBackStack = it.destinationBackStack.dropLastMatching(GuardianDestination.Security),
                                pinDigits = "",
                                pinError = null,
                                recoveryStep = GuardianRecoveryStep.ShowCode,
                                recoveryCodeToShow = recoveryCode,
                                recoveryDigits = "",
                                recoveryError = null,
                                recoveryReturnDestination = GuardianDestination.Security,
                                interactionToken = it.interactionToken + 1,
                            )
                        }
                    } else {
                        showPinError()
                    }
                }
                GuardianPinMode.RecoveryRegenerateConfirm -> {
                    val recoveryCode = appSettingsRepository.regenerateRecoveryCode(pin)
                    if (recoveryCode != null) {
                        _uiState.update {
                            it.copy(
                                isAuthenticated = true,
                                destination = GuardianDestination.Security,
                                destinationBackStack = it.destinationBackStack.dropLastMatching(GuardianDestination.Security),
                                pinDigits = "",
                                pinError = null,
                                recoveryStep = GuardianRecoveryStep.ShowCode,
                                recoveryCodeToShow = recoveryCode,
                                recoveryDigits = "",
                                recoveryError = null,
                                recoveryReturnDestination = GuardianDestination.Security,
                                interactionToken = it.interactionToken + 1,
                            )
                        }
                    } else {
                        showPinError()
                    }
                }
                GuardianPinMode.RecoveryResetNew -> {
                    val recoveryCode = verifiedRecoveryCodeForReset
                    val newRecoveryCode = recoveryCode?.let {
                        appSettingsRepository.resetGuardianPinWithRecoveryCode(it, pin)
                    }
                    if (newRecoveryCode != null) {
                        verifiedRecoveryCodeForReset = null
                        _uiState.update {
                            val nextState = it.copy(
                                isAuthenticated = true,
                                destination = GuardianDestination.Home,
                                destinationBackStack = emptyList(),
                                pinDigits = "",
                                pinError = null,
                                recoveryStep = null,
                                recoveryCodeToShow = null,
                                recoveryDigits = "",
                                recoveryError = null,
                                recoveryReturnDestination = GuardianDestination.Home,
                                interactionToken = it.interactionToken + 1,
                            )
                            nextState.copy(showDailyRoutineSelectionPrompt = nextState.shouldShowDailyRoutineSelectionPrompt())
                        }
                    } else {
                        verifiedRecoveryCodeForReset = null
                        _uiState.update {
                            it.copy(
                                destination = GuardianDestination.RecoveryCode,
                                recoveryStep = GuardianRecoveryStep.EnterCodeForPinReset,
                                recoveryDigits = "",
                                recoveryError = GuardianRecoveryError.CodeMismatch,
                                pinDigits = "",
                                pinError = null,
                                interactionToken = it.interactionToken + 1,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun verifyRecoveryCodeForPinReset(recoveryCode: String) {
        viewModelScope.launch {
            if (appSettingsRepository.verifyRecoveryCode(recoveryCode)) {
                verifiedRecoveryCodeForReset = recoveryCode
                _uiState.update {
                    it.copy(
                        destination = GuardianDestination.Pin,
                        pinMode = GuardianPinMode.RecoveryResetNew,
                        pinDigits = "",
                        pinError = null,
                        recoveryStep = null,
                        recoveryCodeToShow = null,
                        recoveryDigits = "",
                        recoveryError = null,
                        interactionToken = it.interactionToken + 1,
                    )
                }
            } else {
                verifiedRecoveryCodeForReset = null
                _uiState.update {
                    it.copy(
                        recoveryDigits = "",
                        recoveryError = GuardianRecoveryError.CodeMismatch,
                        interactionToken = it.interactionToken + 1,
                    )
                }
            }
        }
    }

    private fun showPinError() {
        _uiState.update {
            it.copy(
                pinDigits = "",
                pinError = "PIN이 맞지 않아요. 다시 입력해 주세요.",
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    private fun updateDraft(transform: (RoutineDraft) -> RoutineDraft) {
        _uiState.update {
            it.copy(
                draft = it.draft?.let(transform),
                draftError = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    private fun updateRoutineSetDraft(transform: (RoutineSetDraft) -> RoutineSetDraft) {
        _uiState.update {
            it.copy(
                routineSetDraft = it.routineSetDraft?.let(transform),
                draftError = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val nextSettings = transform(_uiState.value.appSettings)
        _uiState.update {
            it.copy(
                appSettings = nextSettings,
                interactionToken = it.interactionToken + 1,
            )
        }
        viewModelScope.launch {
            appSettingsRepository.updateAppSettings(nextSettings)
        }
    }

    private fun parseScheduledTime(value: String): LocalTime? {
        if (value.isBlank()) return null
        return runCatching { LocalTime.parse(value) }.getOrNull()
            ?.takeIf { it.second == 0 && it.nano == 0 }
    }

    companion object {
        fun factory(
            routineRepository: RoutineRepository,
            appSettingsRepository: AppSettingsRepository,
            backupProvider: BackupProvider? = null,
            routinePhotoStore: RoutinePhotoStore? = null,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(GuardianModeViewModel::class.java))
                return GuardianModeViewModel(
                    routineRepository,
                    appSettingsRepository,
                    backupProvider,
                    routinePhotoStore,
                ) as T
            }
        }
    }
}

private fun GuardianModeUiState.backStackFor(destination: GuardianDestination): List<GuardianDestination> = when {
    this.destination == destination -> destinationBackStack
    this.destination == GuardianDestination.Pin && destination == GuardianDestination.Home -> emptyList()
    else -> destinationBackStack + this.destination
}

private fun List<GuardianDestination>.dropLastMatching(destination: GuardianDestination): List<GuardianDestination> =
    if (lastOrNull() == destination) dropLast(1) else this

private fun GuardianModeUiState.shouldShowDailyRoutineSelectionPrompt(): Boolean =
    isActive &&
        isAuthenticated &&
        recoveryStep == null &&
        todayRoutineSetId == null &&
        routineSets.isNotEmpty()

private fun backupErrorMessage(error: Throwable): String = when (error) {
    is BackupValidationException -> error.message ?: "백업 파일을 확인할 수 없습니다."
    else -> error.message ?: "백업/복원 중 오류가 발생했습니다."
}

private data class GuardianRepositorySnapshot(
    val settings: AppSettings,
    val today: LocalDate,
    val visibleRoutineSets: List<RoutineSet>,
    val todayRoutineSetId: String?,
    val recordRoutineSets: List<RoutineSet>,
    val dailyLogs: List<DailyLog>,
    val allDailyLogs: List<DailyLog>,
    val recordsEndDate: LocalDate,
)

private data class TodayRoutineSelectionSnapshot(
    val date: LocalDate,
    val routineSets: List<RoutineSet>,
    val todayRoutineSetId: String?,
)

private data class GuardianRecordsResult(
    val days: List<GuardianRecordDay>,
    val routines: List<GuardianRecordRoutine>,
    val summary: GuardianRecordDay,
)

private fun buildGuardianRecords(
    selectedDate: LocalDate,
    endDate: LocalDate,
    routineSets: List<RoutineSet>,
    dailyLogs: List<DailyLog>,
): GuardianRecordsResult {
    val startDate = endDate.minusDays(6)
    val logsByDate = dailyLogs.groupBy(DailyLog::date)
    val routinesById = routineSets
        .flatMap(RoutineSet::routines)
        .associateBy(Routine::id)
    val routineSetsById = routineSets.associateBy(RoutineSet::id)
    val dates = generateSequence(endDate) { previous ->
        previous.minusDays(1).takeIf { !it.isBefore(startDate) }
    }.toList()
    val detailsByDate = dates.associateWith { date ->
        buildGuardianRecordRoutines(
            date = date,
            logs = logsByDate[date].orEmpty(),
            routineSetsById = routineSetsById,
            routinesById = routinesById,
        )
    }
    val days = dates.map { date ->
        val logs = logsByDate[date].orEmpty()
        val routines = detailsByDate[date].orEmpty()
        GuardianRecordDay(
            date = date,
            completedCount = routines.count(GuardianRecordRoutine::isCompleted),
            totalCount = routines.size,
            hasRecords = logs.isNotEmpty(),
        )
    }
    val summary = days.firstOrNull { it.date == selectedDate }
        ?: GuardianRecordDay(selectedDate, 0, 0, false)
    return GuardianRecordsResult(
        days = days,
        routines = detailsByDate[selectedDate].orEmpty(),
        summary = summary,
    )
}

private fun buildGuardianRecordDetail(
    selectedDate: LocalDate?,
    routineSets: List<RoutineSet>,
    dailyLogs: List<DailyLog>,
    routineSetForSelectedDate: RoutineSet? = null,
): GuardianRecordsResult {
    val date = selectedDate ?: LocalDate.now()
    val logs = if (selectedDate == null) emptyList() else dailyLogs.filter { it.date == selectedDate }
    val routinesById = routineSets
        .flatMap(RoutineSet::routines)
        .associateBy(Routine::id)
    val routineSetsById = routineSets.associateBy(RoutineSet::id)
    val routines = buildGuardianRecordRoutines(
        date = date,
        logs = logs,
        routineSetsById = routineSetsById,
        routinesById = routinesById,
        fallbackRoutineSet = routineSetForSelectedDate,
    )
    val summary = GuardianRecordDay(
        date = date,
        completedCount = routines.count(GuardianRecordRoutine::isCompleted),
        totalCount = routines.size,
        hasRecords = logs.isNotEmpty(),
    )
    return GuardianRecordsResult(days = emptyList(), routines = routines, summary = summary)
}

private fun buildGuardianRecordRoutines(
    date: LocalDate,
    logs: List<DailyLog>,
    routineSetsById: Map<String, RoutineSet>,
    routinesById: Map<String, Routine>,
    fallbackRoutineSet: RoutineSet? = null,
): List<GuardianRecordRoutine> {
    if (logs.isEmpty()) {
        return fallbackRoutineSet
            ?.routines
            .orEmpty()
            .filter { it.existedOn(date) && it.deletedAt == null && it.isActive }
            .sortedWith(compareBy<Routine> { it.order }.thenBy { it.createdAt }.thenBy { it.id })
            .map { routine ->
                GuardianRecordRoutine(
                    routineId = routine.id,
                    title = routine.title.resolve(null, Locale.getDefault().toLanguageTag()),
                    isCompleted = false,
                    completedAt = null,
                    isDeleted = false,
                    isInactive = false,
                    isMissing = false,
                )
            }
    }
    val logsByRoutineId = logs.associateBy(DailyLog::routineId)
    val loggedRoutineSetRoutines = logs
        .mapNotNull { routineSetsById[it.routineSetId] }
        .flatMap(RoutineSet::routines)
        .filter { it.existedOn(date) }
    val loggedRoutines = logs.mapNotNull { routinesById[it.routineId] }
    val missingLogRows = logs
        .filter { it.routineId !in routinesById }
        .map { log ->
            GuardianRecordRoutine(
                routineId = log.routineId,
                title = null,
                isCompleted = log.status == LogStatus.Completed,
                completedAt = log.completedAt,
                isDeleted = true,
                isInactive = false,
                isMissing = true,
            )
        }
    val routineRows = (loggedRoutineSetRoutines + loggedRoutines)
        .distinctBy(Routine::id)
        .sortedWith(compareBy<Routine> { it.order }.thenBy { it.createdAt }.thenBy { it.id })
        .map { routine ->
            val log = logsByRoutineId[routine.id]
            GuardianRecordRoutine(
                routineId = routine.id,
                title = routine.title.resolve(null, Locale.getDefault().toLanguageTag()),
                isCompleted = log?.status == LogStatus.Completed,
                completedAt = log?.completedAt,
                isDeleted = routine.deletedAt != null,
                isInactive = !routine.isActive && routine.deletedAt == null,
                isMissing = false,
            )
        }
    return routineRows + missingLogRows
}

private fun Routine.existedOn(date: LocalDate): Boolean {
    val zone = java.time.ZoneId.systemDefault()
    return !createdAt.atZone(zone).toLocalDate().isAfter(date)
}

internal fun buildRoutineSetFromDraft(
    draft: RoutineSetDraft,
    now: Instant,
    localeTag: String,
): RoutineSet {
    val trimmedName = draft.name.trim()
    require(trimmedName.isNotBlank()) { "루틴 제목을 입력해 주세요." }
    require(draft.steps.isNotEmpty()) { "최소 1개 단계가 있어야 저장할 수 있습니다." }

    val routineSetId = newUuidV4()
    val routines = draft.steps.mapIndexed { index, step ->
        val trimmedTitle = step.title.trim()
        require(trimmedTitle.isNotBlank()) { "단계 이름을 입력해 주세요." }
        val scheduledTime = parseDraftScheduledTime(step.scheduledTime)
        require(scheduledTime != null || step.scheduledTime.isBlank()) {
            "예정 시각은 HH:mm 형식으로 입력해 주세요."
        }
        Routine(
            routineSetId = routineSetId,
            title = LocalizedText(mapOf(localeTag to trimmedTitle)),
            icon = step.icon,
            colorToken = step.colorToken,
            order = index,
            scheduledTime = scheduledTime,
            createdAt = now,
            updatedAt = now,
        )
    }
    return RoutineSet(
        id = routineSetId,
        name = LocalizedText(mapOf(localeTag to trimmedName)),
        isActive = true,
        createdAt = now,
        updatedAt = now,
        routines = routines,
    )
}

private fun parseDraftScheduledTime(value: String): LocalTime? {
    if (value.isBlank()) return null
    return runCatching { LocalTime.parse(value) }.getOrNull()
        ?.takeIf { it.second == 0 && it.nano == 0 }
}
