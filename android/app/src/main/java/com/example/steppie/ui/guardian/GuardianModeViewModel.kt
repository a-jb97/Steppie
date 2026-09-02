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
import com.example.steppie.domain.model.DailyLog
import com.example.steppie.domain.model.FeedbackIntensity
import com.example.steppie.domain.model.IconRef
import com.example.steppie.domain.model.LocalizedText
import com.example.steppie.domain.model.LogStatus
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.model.RoutineSet
import com.example.steppie.domain.model.isAvailableToChild
import com.example.steppie.domain.model.isVisible
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
    private var photoAssetsReconciled = false

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
                    visibleRoutineSets = routineSetsAndTodaySelection.routineSets.filter(RoutineSet::isVisible),
                    todayRoutineSetId = routineSetsAndTodaySelection.todayRoutineSetId,
                    recordRoutineSets = recordRoutineSets,
                    dailyLogs = dailyLogs,
                    allDailyLogs = allDailyLogs,
                    recordsEndDate = endDate,
                )
            }.collect { snapshot ->
                if (!photoAssetsReconciled) {
                    photoAssetsReconciled = true
                    val referencedPhotoIds = snapshot.recordRoutineSets
                        .flatMap(RoutineSet::routines)
                        .mapNotNull { (it.icon as? IconRef.Photo)?.localAssetId }
                        .toSet()
                    runCatching { routinePhotoStore?.deleteUnreferencedAssets(referencedPhotoIds) }
                }
                recordRoutineSetsCache = snapshot.recordRoutineSets
                dailyLogsCache = snapshot.dailyLogs
                allDailyLogsCache = snapshot.allDailyLogs
                _uiState.update { state ->
                    val records = buildGuardianRecords(
                        selectedDate = state.selectedRecordsDate,
                        endDate = snapshot.recordsEndDate,
                        routineSets = snapshot.recordRoutineSets,
                        dailyLogs = snapshot.dailyLogs,
                        routineSetsForToday = snapshot.visibleRoutineSets
                            .filter(RoutineSet::isAvailableToChild),
                        today = snapshot.today,
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
                            .filter(RoutineSet::isAvailableToChild)
                            .takeIf { selectedCalendarDate == snapshot.today }
                            .orEmpty(),
                        fallbackDate = snapshot.today,
                        localeTag = localeProvider.languageTag(),
                        zoneId = clockProvider.zoneId,
                    )
                    GuardianRepositoryReducer.dataChanged(
                        state = state,
                        settings = snapshot.settings,
                        visibleRoutineSets = snapshot.visibleRoutineSets,
                        todayRoutineSetId = snapshot.todayRoutineSetId,
                        records = records,
                        calendarRecordDates = calendarRecordDates,
                        calendarRecords = calendarRecords,
                    )
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
        val resetState = initialState()
        _uiState.update { GuardianSessionReducer.closeToChild(it, resetState) }
    }

    fun markInteraction() {
        _uiState.update(GuardianSessionReducer::markInteraction)
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
        _uiState.update { GuardianRecordsReducer.open(it, today) }
    }

    fun openRecordsCalendar() {
        val today = refreshCurrentDate()
        val current = _uiState.value
        val records = buildGuardianRecordDetail(
            selectedDate = today,
            routineSets = recordRoutineSetsCache,
            dailyLogs = allDailyLogsCache,
            routineSetsForSelectedDate = current.routineSets.filter(RoutineSet::isAvailableToChild),
            fallbackDate = today,
            localeTag = localeProvider.languageTag(),
            zoneId = clockProvider.zoneId,
        )
        _uiState.update { GuardianRecordsReducer.openCalendar(it, today, records) }
    }

    fun selectRecordsDate(date: LocalDate) {
        val current = _uiState.value
        if (!GuardianRecordsReducer.canSelectDate(current, date)) return
        val records = buildGuardianRecords(
            selectedDate = date,
            endDate = recordsEndDate.value,
            routineSets = recordRoutineSetsCache,
            dailyLogs = dailyLogsCache,
            routineSetsForToday = current.routineSets
                .filter(RoutineSet::isAvailableToChild),
            today = currentDate.value,
            localeTag = localeProvider.languageTag(),
            zoneId = clockProvider.zoneId,
        )
        _uiState.update { GuardianRecordsReducer.selectDate(it, date, records) }
    }

    fun selectRecordsCalendarDate(date: LocalDate) {
        val current = _uiState.value
        val today = currentDate.value
        if (!GuardianRecordsReducer.canSelectCalendarDate(current, date, today)) return
        val records = buildGuardianRecordDetail(
            selectedDate = date,
            routineSets = recordRoutineSetsCache,
            dailyLogs = allDailyLogsCache,
            routineSetsForSelectedDate = current.routineSets
                .filter(RoutineSet::isAvailableToChild)
                .takeIf { date == today }
                .orEmpty(),
            fallbackDate = today,
            localeTag = localeProvider.languageTag(),
            zoneId = clockProvider.zoneId,
        )
        _uiState.update { GuardianRecordsReducer.selectCalendarDate(it, date, records) }
    }

    fun moveRecordsCalendarMonth(monthDelta: Long) {
        _uiState.update { GuardianRecordsReducer.moveCalendarMonth(it, monthDelta) }
    }

    fun updateFeedbackIntensity(intensity: FeedbackIntensity) = updateSettings {
        GuardianSettingsReducer.updateFeedbackIntensity(it, intensity)
    }

    fun updateTtsEnabled(enabled: Boolean) = updateSettings {
        GuardianSettingsReducer.updateTtsEnabled(it, enabled)
    }

    fun updateTtsRate(rate: Double) = updateSettings {
        GuardianSettingsReducer.updateTtsRate(it, rate)
    }

    fun updateTtsVolume(volume: Double) = updateSettings {
        GuardianSettingsReducer.updateTtsVolume(it, volume)
    }

    fun updateSoundEnabled(enabled: Boolean) = updateSettings {
        GuardianSettingsReducer.updateSoundEnabled(it, enabled)
    }

    fun updateHapticEnabled(enabled: Boolean) = updateSettings {
        GuardianSettingsReducer.updateHapticEnabled(it, enabled)
    }

    fun updateNotificationLeadTime(leadMinutes: Int, enabled: Boolean) {
        updateSettings {
            GuardianSettingsReducer.updateNotificationLeadTime(it, leadMinutes, enabled)
        }
    }

    fun updateQuietHoursEnabled(enabled: Boolean) = updateSettings {
        GuardianSettingsReducer.updateQuietHoursEnabled(it, enabled)
    }

    fun updateQuietHoursStart(value: String) {
        val time = parseScheduledTime(value) ?: return
        updateSettings { GuardianSettingsReducer.updateQuietHoursStart(it, time) }
    }

    fun updateQuietHoursEnd(value: String) {
        val time = parseScheduledTime(value) ?: return
        updateSettings { GuardianSettingsReducer.updateQuietHoursEnd(it, time) }
    }

    fun openBackupRestore() {
        _uiState.update(GuardianBackupReducer::open)
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
        _uiState.update(GuardianRecoveryReducer::openPinReset)
    }

    fun closeRecoveryCode() {
        verifiedRecoveryCodeForReset = null
        if (_uiState.value.pinMode == GuardianPinMode.SetupConfirm) {
            pendingSetupPin = null
            val resetState = initialState()
            _uiState.update { GuardianSessionReducer.closeAfterSetup(it, resetState) }
        } else {
            _uiState.update(GuardianRecoveryReducer::close)
        }
    }

    fun updateRecoveryCodeInput(value: String) {
        _uiState.update { GuardianRecoveryReducer.updateCodeInput(it, value) }
    }

    fun confirmRecoveryCodeForPinReset() {
        val recoveryCode = _uiState.value.recoveryDigits
        if (recoveryCode.length != 6) return
        verifyRecoveryCodeForPinReset(recoveryCode)
    }

    fun cancelRecoveryPinReset() {
        verifiedRecoveryCodeForReset = null
        _uiState.update(GuardianRecoveryReducer::cancelPinReset)
    }

    fun openNewRoutineEditor() {
        _uiState.update(GuardianRoutineDraftReducer::openNew)
    }

    fun openRoutineEditor(routineId: String) {
        val routine = _uiState.value.routines.firstOrNull { it.id == routineId } ?: return
        val title = routine.title.resolve(null, localeProvider.languageTag())
        _uiState.update { GuardianRoutineDraftReducer.openExisting(it, routine, title) }
    }

    fun openRoutineSetCreate() {
        _uiState.update(GuardianRoutineSetDraftReducer::open)
    }

    fun openTemplateSelect(returnDestination: GuardianDestination = GuardianDestination.RoutineEdit) {
        val initialTemplate = RoutineTemplates.find(RoutineTemplateId.Morning)
        _uiState.update {
            GuardianTemplateReducer.open(it, returnDestination, initialTemplate)
        }
    }

    fun openTemplateSelectFromHome() {
        openTemplateSelect(GuardianDestination.Home)
    }

    fun closeTemplateSelect() {
        val destination = _uiState.value.templateReturnDestination
        _uiState.update { GuardianTemplateReducer.close(it, destination) }
    }

    fun previewTemplate(templateId: RoutineTemplateId) {
        val template = RoutineTemplates.find(templateId) ?: return
        _uiState.update { GuardianTemplateReducer.preview(it, template) }
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
                _uiState.update(GuardianTemplateReducer::saveSucceeded)
            }.onFailure { error ->
                _uiState.update { state ->
                    GuardianTemplateReducer.saveFailed(
                        state,
                        error.message ?: "템플릿을 저장할 수 없습니다.",
                    )
                }
            }
        }
    }

    fun toggleRoutineSetListEditing() {
        _uiState.update(GuardianRoutineSetReducer::toggleListEditing)
    }

    fun selectRoutineSet(routineSetId: String) {
        val routineSet = _uiState.value.routineSets.firstOrNull { it.id == routineSetId } ?: return
        _uiState.update { GuardianRoutineSetReducer.select(it, routineSet) }
    }

    fun setRoutineSetForToday(routineSetId: String) {
        val state = _uiState.value
        val target = state.routineSets.firstOrNull { it.id == routineSetId } ?: return
        if (target.isActive && state.routineSets.count(RoutineSet::isActive) <= 1) {
            _uiState.update(GuardianRoutineSetReducer::lastDailyRoutineSetRejected)
            return
        }
        if (!target.isActive && target.startTime == null) {
            _uiState.update(GuardianRoutineSetReducer::missingStartTimeRejected)
            return
        }
        if (
            !target.isActive &&
            state.routineSets.any { it.id != target.id && it.isActive && it.startTime == target.startTime }
        ) {
            _uiState.update(GuardianRoutineSetReducer::duplicateDailyStartTimeRejected)
            return
        }
        viewModelScope.launch {
            routineRepository.updateRoutineSet(
                target.copy(isActive = !target.isActive, updatedAt = clockProvider.now()),
            )
            _uiState.update {
                GuardianRoutineSetReducer.dailyParticipationChanged(it, routineSetId, target.isActive)
            }
        }
    }

    fun updateRoutineSetStartTime(routineSetId: String, value: String) {
        val target = _uiState.value.routineSets.firstOrNull { it.id == routineSetId } ?: return
        val parsed = parseDraftScheduledTime(value)
        if (value.isNotBlank() && parsed == null) {
            _uiState.update(GuardianRoutineSetReducer::invalidStartTime)
            return
        }
        if (
            parsed != null &&
            _uiState.value.routineSets.any { it.id != target.id && it.isActive && it.startTime == parsed }
        ) {
            _uiState.update(GuardianRoutineSetReducer::duplicateStartTime)
            return
        }
        viewModelScope.launch {
            routineRepository.updateRoutineSet(
                target.copy(startTime = parsed, updatedAt = clockProvider.now()),
            )
            _uiState.update(GuardianRoutineSetReducer::startTimeSaved)
        }
    }

    fun dismissDailyRoutineSelectionPrompt() {
        _uiState.update(GuardianRoutineSetReducer::dismissDailySelectionPrompt)
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
        val resolvedName = routineSet.name.resolve(null, localeProvider.languageTag())
        _uiState.update { GuardianRoutineSetReducer.beginNameEdit(it, routineSet.id, resolvedName) }
    }

    fun updateEditingRoutineSetName(name: String) {
        _uiState.update { GuardianRoutineSetReducer.updateEditingName(it, name) }
    }

    fun cancelEditRoutineSetName() {
        _uiState.update(GuardianRoutineSetReducer::cancelNameEdit)
    }

    fun saveEditingRoutineSetName() {
        val state = _uiState.value
        val routineSetId = state.editingRoutineSetId ?: return
        val routineSet = state.routineSets.firstOrNull { it.id == routineSetId } ?: return
        val trimmedName = state.editingRoutineSetName.trim()
        if (trimmedName.isBlank()) {
            _uiState.update(GuardianRoutineSetReducer::blankNameRejected)
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

    fun updateDraftTitle(title: String) {
        _uiState.update { GuardianRoutineDraftReducer.updateTitle(it, title) }
    }

    fun updateDraftIcon(iconName: String) {
        _uiState.update { GuardianRoutineDraftReducer.updateBuiltinIcon(it, iconName) }
    }

    fun importDraftPhoto(uri: Uri) {
        val store = routinePhotoStore ?: run {
            _uiState.update(GuardianRoutineDraftReducer::photoUnavailable)
            return
        }
        viewModelScope.launch {
            runCatching { store.importPhoto(uri) }
                .onSuccess { photo ->
                    _uiState.update { GuardianRoutineDraftReducer.photoImported(it, photo) }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        GuardianRoutineDraftReducer.photoImportFailed(
                            state,
                            error.message ?: "사진을 저장할 수 없습니다.",
                        )
                    }
                }
        }
    }

    fun removeDraftPhoto() {
        _uiState.update(GuardianRoutineDraftReducer::removePhoto)
    }

    fun updateDraftColor(colorToken: String) {
        _uiState.update { GuardianRoutineDraftReducer.updateColor(it, colorToken) }
    }

    fun updateDraftScheduledTime(value: String) {
        _uiState.update { GuardianRoutineDraftReducer.updateScheduledTime(it, value) }
    }

    fun updateRoutineSetName(name: String) {
        _uiState.update { GuardianRoutineSetDraftReducer.updateName(it, name) }
    }

    fun updateRoutineSetStepTitle(title: String) {
        _uiState.update { GuardianRoutineSetDraftReducer.updateStepTitle(it, title) }
    }

    fun updateRoutineSetStepIcon(iconName: String) {
        _uiState.update { GuardianRoutineSetDraftReducer.updateStepBuiltinIcon(it, iconName) }
    }

    fun importRoutineSetStepPhoto(uri: Uri) {
        val store = routinePhotoStore ?: run {
            _uiState.update(GuardianRoutineSetDraftReducer::photoUnavailable)
            return
        }
        viewModelScope.launch {
            runCatching { store.importPhoto(uri) }
                .onSuccess { photo ->
                    _uiState.update { GuardianRoutineSetDraftReducer.photoImported(it, photo) }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        GuardianRoutineSetDraftReducer.photoImportFailed(
                            state,
                            error.message ?: "사진을 저장할 수 없습니다.",
                        )
                    }
                }
        }
    }

    fun removeRoutineSetStepPhoto() {
        _uiState.update(GuardianRoutineSetDraftReducer::removeStepPhoto)
    }

    fun updateRoutineSetStepColor(colorToken: String) {
        _uiState.update { GuardianRoutineSetDraftReducer.updateStepColor(it, colorToken) }
    }

    fun updateRoutineSetStepScheduledTime(value: String) {
        _uiState.update { GuardianRoutineSetDraftReducer.updateStepScheduledTime(it, value) }
    }

    fun addRoutineSetStep() {
        val draft = _uiState.value.routineSetDraft ?: return
        val trimmedTitle = draft.stepDraft.title.trim()
        if (trimmedTitle.isBlank()) {
            _uiState.update {
                GuardianRoutineSetDraftReducer.showError(it, "단계 이름을 입력해 주세요.")
            }
            return
        }
        if (parseScheduledTime(draft.stepDraft.scheduledTime) == null && draft.stepDraft.scheduledTime.isNotBlank()) {
            _uiState.update {
                GuardianRoutineSetDraftReducer.showError(it, "예정 시각은 HH:mm 형식으로 입력해 주세요.")
            }
            return
        }
        _uiState.update { GuardianRoutineSetDraftReducer.addOrUpdateStep(it, trimmedTitle) }
    }

    fun editRoutineSetStep(index: Int) {
        _uiState.update { GuardianRoutineSetDraftReducer.editStep(it, index) }
    }

    fun removeRoutineSetStep(index: Int) {
        _uiState.update { GuardianRoutineSetDraftReducer.removeStep(it, index) }
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
            _uiState.update {
                GuardianEditorPersistenceReducer.routineSetSaveFailed(
                    it,
                    error.message ?: "루틴 세트를 저장할 수 없습니다.",
                )
            }
            return
        }

        viewModelScope.launch {
            routineRepository.createRoutineSet(
                routineSet.copy(isActive = _uiState.value.routineSets.none(RoutineSet::isActive)),
            )
            _uiState.update(GuardianEditorPersistenceReducer::saveSucceeded)
        }
    }

    fun saveDraft() {
        val state = _uiState.value
        val draft = state.draft ?: return
        val activeSet = state.activeRoutineSet
        if (activeSet == null) {
            _uiState.update(GuardianEditorPersistenceReducer::routineSetRequired)
            return
        }
        val trimmedTitle = draft.title.trim()
        if (trimmedTitle.isBlank()) {
            _uiState.update(GuardianEditorPersistenceReducer::routineTitleRequired)
            return
        }
        val scheduledTime = parseScheduledTime(draft.scheduledTime)
        if (scheduledTime == null && draft.scheduledTime.isNotBlank()) {
            _uiState.update(GuardianEditorPersistenceReducer::scheduledTimeInvalid)
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
            _uiState.update(GuardianEditorPersistenceReducer::saveSucceeded)
        }
    }

    fun requestDelete(routineId: String) {
        _uiState.update { GuardianDeletionReducer.requestRoutine(it, routineId) }
    }

    fun requestDeleteRoutineSet(routineSetId: String) {
        _uiState.update { GuardianDeletionReducer.requestRoutineSet(it, routineSetId) }
    }

    fun cancelDelete() {
        _uiState.update(GuardianDeletionReducer::cancel)
    }

    fun confirmDelete() {
        val routineId = _uiState.value.pendingDeleteRoutineId ?: return
        viewModelScope.launch {
            routineRepository.deleteRoutine(routineId, clockProvider.now())
            _uiState.update(GuardianDeletionReducer::routineDeleted)
        }
    }

    fun confirmDeleteRoutineSet() {
        val state = _uiState.value
        val routineSetId = state.pendingDeleteRoutineSetId ?: return
        val target = state.routineSets.firstOrNull { it.id == routineSetId } ?: return
        if (state.routineSets.size <= 1) {
            _uiState.update(GuardianDeletionReducer::lastRoutineSetRejected)
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
            routineRepository.deleteRoutineSet(target.id, clockProvider.now())
            _uiState.update(GuardianDeletionReducer::routineSetDeleted)
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
            routineRepository.reorderRoutines(setId, ids, clockProvider.now())
        }
    }

    fun showOutOfScopeNotice() {
        _uiState.update(GuardianSessionReducer::showOutOfScopeNotice)
    }

    fun clearNotice() {
        _uiState.update(GuardianSessionReducer::clearNotice)
    }

    fun navigateBack() {
        _uiState.update(GuardianNavigationReducer::navigateBack)
    }

    fun exportBackup(uri: Uri) {
        val provider = backupProvider ?: run {
            _uiState.update(GuardianBackupReducer::exportUnavailable)
            return
        }
        _uiState.update(GuardianBackupReducer::exportStarted)
        viewModelScope.launch {
            runCatching { provider.exportTo(uri) }
                .onSuccess {
                    _uiState.update(GuardianBackupReducer::exportSucceeded)
                }
                .onFailure { error ->
                    _uiState.update { GuardianBackupReducer.operationFailed(it, backupErrorMessage(error)) }
                }
        }
    }

    fun previewRestoreBackup(uri: Uri) {
        val provider = backupProvider ?: run {
            _uiState.update(GuardianBackupReducer::restoreUnavailable)
            return
        }
        _uiState.update(GuardianBackupReducer::previewStarted)
        viewModelScope.launch {
            runCatching { provider.previewImport(uri) }
                .onSuccess { preview ->
                    _uiState.update { GuardianBackupReducer.previewSucceeded(it, uri, preview) }
                }
                .onFailure { error ->
                    _uiState.update { GuardianBackupReducer.operationFailed(it, backupErrorMessage(error)) }
                }
        }
    }

    fun inputRestorePinDigit(digit: Int) {
        require(digit in 0..9)
        val current = _uiState.value
        if (current.restorePinDigits.length >= 4) return
        val nextDigits = current.restorePinDigits + digit.toString()
        _uiState.update { GuardianBackupReducer.inputRestorePinDigit(it, digit) }
        if (nextDigits.length == 4) restoreWithPin(nextDigits)
    }

    fun deleteRestorePinDigit() {
        _uiState.update(GuardianBackupReducer::deleteRestorePinDigit)
    }

    fun cancelRestore() {
        _uiState.update(GuardianBackupReducer::cancelRestore)
    }

    private fun restoreWithPin(pin: String) {
        val provider = backupProvider ?: return
        val uri = _uiState.value.pendingRestoreUri ?: return
        _uiState.update(GuardianBackupReducer::restoreStarted)
        viewModelScope.launch {
            if (!appSettingsRepository.verifyGuardianPin(pin)) {
                _uiState.update(GuardianBackupReducer::restorePinRejected)
                return@launch
            }
            runCatching { provider.restoreReplace(uri) }
                .onSuccess { result ->
                    _uiState.update {
                        GuardianBackupReducer.restoreSucceeded(
                            previousState = it,
                            initialState = initialState(),
                            requiresGuardianPinSetup = result.requiresGuardianPinSetup,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { GuardianBackupReducer.restoreFailed(it, backupErrorMessage(error)) }
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
                _uiState.update(GuardianRecoveryReducer::verificationSucceeded)
            } else {
                verifiedRecoveryCodeForReset = null
                _uiState.update(GuardianRecoveryReducer::verificationFailed)
            }
        }
    }

    private fun showPinError() {
        _uiState.update(GuardianPinReducer::showPinError)
    }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val nextSettings = transform(_uiState.value.appSettings)
        _uiState.update { GuardianSettingsReducer.settingsChanged(it, nextSettings) }
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

private fun buildGuardianRecords(
    selectedDate: LocalDate,
    endDate: LocalDate,
    routineSets: List<RoutineSet>,
    dailyLogs: List<DailyLog>,
    routineSetsForToday: List<RoutineSet>,
    today: LocalDate,
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
            fallbackRoutineSets = routineSetsForToday
                .takeIf { date == today }
                .orEmpty(),
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
    val logsByRoutineId = logs.associateBy(DailyLog::routineId)
    val fallbackRoutines = fallbackRoutineSets
        .sortedWith(
            compareBy<RoutineSet> { it.startTime != null }
                .thenBy { it.startTime }
                .thenBy { it.createdAt },
        )
        .flatMap(RoutineSet::routines)
        .filter { it.existedOn(date, zoneId) && it.isAvailableToChild }
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
    val routineRows = (fallbackRoutines + loggedRoutineSetRoutines + loggedRoutines)
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
