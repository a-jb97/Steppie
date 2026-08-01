package com.example.steppie.ui.guardian

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.steppie.core.environment.ClockProvider
import com.example.steppie.core.environment.LocaleProvider
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
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class GuardianModeViewModel(
    private val routineRepository: RoutineRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val clockProvider: ClockProvider,
    private val localeProvider: LocaleProvider,
    private val backupProvider: BackupProvider? = null,
    private val routinePhotoStore: RoutinePhotoStore? = null,
) : ViewModel() {
    private val recordsEndDate = MutableStateFlow(clockProvider.today())
    private val currentDate = MutableStateFlow(clockProvider.today())
    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<GuardianModeUiState> = _uiState.asStateFlow()
    private var verifiedPinForChange: String? = null
    private var verifiedRecoveryCodeForReset: String? = null
    private var pendingSetupPin: String? = null
    private var recordRoutineSetsCache: List<RoutineSet> = emptyList()
    private var dailyLogsCache: List<DailyLog> = emptyList()
    private var allDailyLogsCache: List<DailyLog> = emptyList()

    private fun initialState(): GuardianModeUiState {
        val today = clockProvider.today()
        return GuardianModeUiState(
            selectedRecordsDate = today,
            selectedRecordSummary = GuardianRecordDay(today, 0, 0, false),
            recordsCalendarMonth = YearMonth.from(today),
            selectedCalendarRecordSummary = GuardianRecordDay(today, 0, 0, false),
        )
    }

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
                        localeTag = localeProvider.languageTag(),
                        zoneId = clockProvider.zoneId,
                    )
                    val calendarRecordDates = snapshot.allDailyLogs.map(DailyLog::date).toSet()
                    val selectedCalendarDate = state.selectedCalendarRecordsDate
                    val calendarRecords = buildGuardianRecordDetail(
                        selectedDate = selectedCalendarDate,
                        routineSets = snapshot.recordRoutineSets,
                        dailyLogs = snapshot.allDailyLogs,
                        routineSetsForSelectedDate = snapshot.visibleRoutineSets
                            .filter(RoutineSet::isActive)
                            .takeIf { selectedCalendarDate == snapshot.today }
                            .orEmpty(),
                        fallbackDate = snapshot.today,
                        localeTag = localeProvider.languageTag(),
                        zoneId = clockProvider.zoneId,
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
        _uiState.update(GuardianPinReducer::openFromChild)
    }

    fun openInitialSetup() {
        if (_uiState.value.hasGuardianPin) return
        pendingSetupPin = null
        _uiState.update(GuardianPinReducer::openInitialSetup)
    }

    fun closeToChild() {
        verifiedPinForChange = null
        verifiedRecoveryCodeForReset = null
        _uiState.update {
            initialState().copy(
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
        _uiState.update { GuardianPinReducer.inputDigit(it, digit) }
        if (nextDigits.length == 4) handleCompletePin(nextDigits)
    }

    fun deletePinDigit() {
        _uiState.update(GuardianPinReducer::deleteDigit)
    }

    fun openHome() {
        _uiState.update(GuardianNavigationReducer::openHome)
    }

    fun openRoutineEdit() {
        _uiState.update(GuardianNavigationReducer::openRoutineEdit)
    }

    fun openSecurity() {
        verifiedRecoveryCodeForReset = null
        _uiState.update(GuardianNavigationReducer::openSecurity)
    }

    fun openEnvironmentSettings() {
        _uiState.update(GuardianNavigationReducer::openEnvironmentSettings)
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
            routineSetsForSelectedDate = current.routineSets.filter(RoutineSet::isActive),
            fallbackDate = today,
            localeTag = localeProvider.languageTag(),
            zoneId = clockProvider.zoneId,
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
            localeTag = localeProvider.languageTag(),
            zoneId = clockProvider.zoneId,
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
        val canSelectTodayRoutine = date == today && current.routineSets.any(RoutineSet::isActive)
        if (date !in current.calendarRecordDates && !canSelectTodayRoutine) return
        val records = buildGuardianRecordDetail(
            selectedDate = date,
            routineSets = recordRoutineSetsCache,
            dailyLogs = allDailyLogsCache,
            routineSetsForSelectedDate = current.routineSets
                .filter(RoutineSet::isActive)
                .takeIf { date == today }
                .orEmpty(),
            fallbackDate = today,
            localeTag = localeProvider.languageTag(),
            zoneId = clockProvider.zoneId,
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
        _uiState.update(GuardianPinReducer::openPinChange)
    }

    fun openRecoveryCodeRegeneration() {
        verifiedPinForChange = null
        verifiedRecoveryCodeForReset = null
        _uiState.update(GuardianPinReducer::openRecoveryCodeRegeneration)
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
                return@update initialState().copy(
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
        val title = routine.title.resolve(null, localeProvider.languageTag())
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
                val created = template.instantiate(clockProvider.now())
                routineRepository.createRoutineSet(
                    created.copy(isActive = _uiState.value.routineSets.none(RoutineSet::isActive)),
                )
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
        val state = _uiState.value
        val target = state.routineSets.firstOrNull { it.id == routineSetId } ?: return
        if (target.isActive && state.routineSets.count(RoutineSet::isActive) <= 1) {
            _uiState.update {
                it.copy(
                    notice = "최소 한 개의 루틴 세트는 매일 진행해야 합니다.",
                    interactionToken = it.interactionToken + 1,
                )
            }
            return
        }
        if (!target.isActive && target.startTime == null) {
            _uiState.update {
                it.copy(
                    notice = "추가할 루틴 세트의 시작 시간을 먼저 설정해 주세요.",
                    interactionToken = it.interactionToken + 1,
                )
            }
            return
        }
        if (
            !target.isActive &&
            state.routineSets.any { it.id != target.id && it.isActive && it.startTime == target.startTime }
        ) {
            _uiState.update {
                it.copy(
                    notice = "다른 루틴 세트와 시작 시간이 같아요.",
                    interactionToken = it.interactionToken + 1,
                )
            }
            return
        }
        viewModelScope.launch {
            routineRepository.updateRoutineSet(
                target.copy(isActive = !target.isActive, updatedAt = clockProvider.now()),
            )
            _uiState.update {
                it.copy(
                    selectedRoutineSetId = routineSetId,
                    showDailyRoutineSelectionPrompt = false,
                    notice = if (target.isActive) {
                        "매일 진행에서 제외했습니다."
                    } else {
                        "매일 진행에 추가했습니다."
                    },
                    interactionToken = it.interactionToken + 1,
                )
            }
        }
    }

    fun updateRoutineSetStartTime(routineSetId: String, value: String) {
        val target = _uiState.value.routineSets.firstOrNull { it.id == routineSetId } ?: return
        val parsed = parseDraftScheduledTime(value)
        if (value.isNotBlank() && parsed == null) {
            _uiState.update { it.copy(draftError = "시작 시각은 HH:mm 형식으로 입력해 주세요.") }
            return
        }
        if (
            parsed != null &&
            _uiState.value.routineSets.any { it.id != target.id && it.isActive && it.startTime == parsed }
        ) {
            _uiState.update { it.copy(draftError = "다른 루틴 세트와 시작 시간이 같아요.") }
            return
        }
        viewModelScope.launch {
            routineRepository.updateRoutineSet(
                target.copy(startTime = parsed, updatedAt = clockProvider.now()),
            )
            _uiState.update {
                it.copy(
                    draftError = null,
                    notice = "루틴 세트 시작 시간을 저장했습니다.",
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
        val today = clockProvider.today()
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
                editingRoutineSetName = routineSet.name.resolve(null, localeProvider.languageTag()),
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
                    name = LocalizedText(mapOf(localeProvider.languageTag() to trimmedName)),
                    updatedAt = clockProvider.now(),
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
        val now = clockProvider.now()
        val routineSet = runCatching {
            buildRoutineSetFromDraft(
                draft = draft,
                now = now,
                localeTag = localeProvider.languageTag(),
            )
        }.getOrElse { error ->
            _uiState.update { it.copy(draftError = error.message ?: "루틴 세트를 저장할 수 없습니다.") }
            return
        }

        viewModelScope.launch {
            routineRepository.createRoutineSet(
                routineSet.copy(isActive = _uiState.value.routineSets.none(RoutineSet::isActive)),
            )
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
            val now = clockProvider.now()
            val localizedTitle = LocalizedText(mapOf(localeProvider.languageTag() to trimmedTitle))
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
                if (state.routineSets.count(RoutineSet::isActive) <= 1) {
                    state.routineSets.firstOrNull { it.id != target.id }?.let { replacement ->
                        routineRepository.updateRoutineSet(
                            replacement.copy(isActive = true, updatedAt = clockProvider.now()),
                        )
                    }
                }
                routineRepository.updateRoutineSet(target.copy(isActive = false, updatedAt = clockProvider.now()))
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
        _uiState.update(GuardianNavigationReducer::navigateBack)
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
                        initialState().copy(
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
                        _uiState.update(GuardianPinReducer::authenticationSucceeded)
                    } else {
                        showPinError()
                    }
                }
                GuardianPinMode.Setup -> {
                    pendingSetupPin = pin
                    _uiState.update(GuardianPinReducer::awaitSetupConfirmation)
                }
                GuardianPinMode.SetupConfirm -> {
                    val firstPin = pendingSetupPin
                    if (firstPin == null || firstPin != pin) {
                        pendingSetupPin = null
                        _uiState.update(GuardianPinReducer::setupMismatch)
                        return@launch
                    }
                    val recoveryCode = appSettingsRepository.setGuardianPin(pin)
                    refreshCurrentDate()
                    _uiState.update { GuardianPinReducer.setupSucceeded(it, recoveryCode) }
                }
                GuardianPinMode.ChangeCurrent -> {
                    if (appSettingsRepository.verifyGuardianPin(pin)) {
                        verifiedPinForChange = pin
                        _uiState.update(GuardianPinReducer::currentPinVerified)
                    } else {
                        showPinError()
                    }
                }
                GuardianPinMode.ChangeNew -> {
                    val currentPin = verifiedPinForChange
                    val recoveryCode = currentPin?.let { appSettingsRepository.changeGuardianPin(it, pin) }
                    if (recoveryCode != null) {
                        verifiedPinForChange = null
                        _uiState.update { GuardianPinReducer.securityRecoveryCodeShown(it, recoveryCode) }
                    } else {
                        showPinError()
                    }
                }
                GuardianPinMode.RecoveryRegenerateConfirm -> {
                    val recoveryCode = appSettingsRepository.regenerateRecoveryCode(pin)
                    if (recoveryCode != null) {
                        _uiState.update { GuardianPinReducer.securityRecoveryCodeShown(it, recoveryCode) }
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
                        _uiState.update(GuardianPinReducer::recoveryResetSucceeded)
                    } else {
                        verifiedRecoveryCodeForReset = null
                        _uiState.update(GuardianPinReducer::recoveryResetFailed)
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
        _uiState.update(GuardianPinReducer::showPinError)
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
            clockProvider: ClockProvider,
            localeProvider: LocaleProvider,
            backupProvider: BackupProvider? = null,
            routinePhotoStore: RoutinePhotoStore? = null,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(GuardianModeViewModel::class.java))
                return GuardianModeViewModel(
                    routineRepository,
                    appSettingsRepository,
                    clockProvider,
                    localeProvider,
                    backupProvider,
                    routinePhotoStore,
                ) as T
            }
        }
    }
}

private fun GuardianModeUiState.shouldShowDailyRoutineSelectionPrompt(): Boolean =
    false

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
    localeTag: String,
    zoneId: ZoneId,
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
            localeTag = localeTag,
            zoneId = zoneId,
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
    routineSetsForSelectedDate: List<RoutineSet> = emptyList(),
    fallbackDate: LocalDate,
    localeTag: String,
    zoneId: ZoneId,
): GuardianRecordsResult {
    val date = selectedDate ?: fallbackDate
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
        fallbackRoutineSets = routineSetsForSelectedDate,
        localeTag = localeTag,
        zoneId = zoneId,
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
    fallbackRoutineSets: List<RoutineSet> = emptyList(),
    localeTag: String,
    zoneId: ZoneId,
): List<GuardianRecordRoutine> {
    if (logs.isEmpty()) {
        return fallbackRoutineSets
            .sortedWith(
                compareBy<RoutineSet> { it.startTime != null }
                    .thenBy { it.startTime }
                    .thenBy { it.createdAt },
            )
            .flatMap(RoutineSet::routines)
            .filter { it.existedOn(date, zoneId) && it.deletedAt == null && it.isActive }
            .sortedWith(compareBy<Routine> { it.order }.thenBy { it.createdAt }.thenBy { it.id })
            .map { routine ->
                GuardianRecordRoutine(
                    routineId = routine.id,
                    title = routine.title.resolve(null, localeTag),
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
        .filter { it.existedOn(date, zoneId) }
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
                title = routine.title.resolve(null, localeTag),
                isCompleted = log?.status == LogStatus.Completed,
                completedAt = log?.completedAt,
                isDeleted = routine.deletedAt != null,
                isInactive = !routine.isActive && routine.deletedAt == null,
                isMissing = false,
            )
        }
    return routineRows + missingLogRows
}

private fun Routine.existedOn(date: LocalDate, zoneId: ZoneId): Boolean =
    !createdAt.atZone(zoneId).toLocalDate().isAfter(date)

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
        isActive = false,
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
