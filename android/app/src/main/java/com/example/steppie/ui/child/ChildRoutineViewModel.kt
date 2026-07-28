package com.example.steppie.ui.child

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.DailyLog
import com.example.steppie.domain.model.FeedbackIntensity
import com.example.steppie.domain.model.LogStatus
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.model.RoutineSet
import com.example.steppie.domain.repository.AppSettingsRepository
import com.example.steppie.domain.repository.RoutineRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

enum class ChildSinglePane { Focus, List }

data class ChildRoutineUiState(
    val routines: List<Routine> = emptyList(),
    val completedRoutineIds: Set<String> = emptySet(),
    val selectedRoutineId: String? = null,
    val singlePane: ChildSinglePane = ChildSinglePane.Focus,
    val isLoading: Boolean = true,
    val feedbackRoutineId: String? = null,
    val undoRoutineId: String? = null,
    val feedbackIntensity: FeedbackIntensity = FeedbackIntensity.Normal,
    val currentRoutineSet: RoutineSet? = null,
    val waitingRoutineSet: RoutineSet? = null,
    val waitingUntil: LocalTime? = null,
    val dayProgressCount: Int? = null,
    val dayProgressTotal: Int? = null,
    val dayAllComplete: Boolean? = null,
    val scheduledRoutines: List<Routine> = emptyList(),
    val hasRemainingSchedule: Boolean = false,
    val isRoutineSetLocked: Boolean = false,
) {
    val selectedRoutine: Routine?
        get() = if (isAllComplete && feedbackRoutineId == null) {
            null
        } else {
            routines.firstOrNull { it.id == selectedRoutineId }
        }
    val feedbackRoutine: Routine?
        get() = routines.firstOrNull { it.id == feedbackRoutineId }
    val undoRoutine: Routine?
        get() = routines.firstOrNull { it.id == undoRoutineId }
    val currentRoutine: Routine?
        get() = if (isRoutineSetLocked) {
            null
        } else {
            routines.firstOrNull { it.id !in completedRoutineIds && it.id != feedbackRoutineId }
        }
    val nextIncompleteRoutine: Routine?
        get() = currentRoutine
    val isSelectedRoutineCompletable: Boolean
        get() = !isRoutineSetLocked &&
            feedbackRoutineId == null &&
            selectedRoutineId != null &&
            selectedRoutineId == currentRoutine?.id
    val progressCount: Int
        get() = dayProgressCount ?: routines.count { it.id in completedRoutineIds || it.id == feedbackRoutineId }
    val progressTotal: Int
        get() = dayProgressTotal ?: routines.size
    val isAllComplete: Boolean
        get() = dayAllComplete ?: (routines.isNotEmpty() && routines.all { it.id in completedRoutineIds })
    val isWaiting: Boolean
        get() = waitingRoutineSet != null
}

internal data class RoutineScheduleResolution(
    val currentSet: RoutineSet?,
    val waitingSet: RoutineSet?,
    val allComplete: Boolean,
)

internal fun resolveRoutineSchedule(
    routineSets: List<RoutineSet>,
    completedRoutineIds: Set<String>,
    now: LocalTime,
): RoutineScheduleResolution {
    val scheduledSets = routineSets
        .filter { it.isActive && it.deletedAt == null }
        .sortedWith(
            compareBy<RoutineSet> { it.startTime != null }
                .thenBy { it.startTime }
                .thenBy { it.createdAt }
                .thenBy { it.id },
        )
    val firstIncomplete = scheduledSets.firstOrNull { set ->
        set.routines.any { it.isActive && it.deletedAt == null && it.id !in completedRoutineIds }
    }
    if (firstIncomplete == null) {
        return RoutineScheduleResolution(
            currentSet = null,
            waitingSet = null,
            allComplete = scheduledSets.isNotEmpty(),
        )
    }
    return if (firstIncomplete.startTime == null || !now.isBefore(firstIncomplete.startTime)) {
        RoutineScheduleResolution(firstIncomplete, null, false)
    } else {
        RoutineScheduleResolution(null, firstIncomplete, false)
    }
}

data class ChildRoutineFeedbackEvent(
    val spokenText: String?,
    val vibrate: Boolean,
    val sound: Boolean,
    val ttsRate: Float,
    val ttsVolume: Float,
)

internal fun childRoutineState(
    routines: List<Routine>,
    completedRoutineIds: Set<String>,
    selectedRoutineId: String?,
    singlePane: ChildSinglePane,
    feedbackRoutineId: String? = null,
    undoRoutineId: String? = null,
    feedbackIntensity: FeedbackIntensity = FeedbackIntensity.Normal,
): ChildRoutineUiState {
    val visibleRoutines = routines
        .filter { it.isActive && it.deletedAt == null }
        .sortedBy(Routine::order)
    val visibleIds = visibleRoutines.mapTo(mutableSetOf(), Routine::id)
    val resolvedCompletedIds = completedRoutineIds.intersect(visibleIds)
    val resolvedFeedbackId = feedbackRoutineId?.takeIf { it in visibleIds }
    val resolvedSelection = resolvedFeedbackId ?: selectedRoutineId
        ?.takeIf { selectedId -> visibleRoutines.any { it.id == selectedId } }
        ?: visibleRoutines.firstOrNull { it.id !in resolvedCompletedIds }?.id
        ?: visibleRoutines.firstOrNull()?.id

    return ChildRoutineUiState(
        routines = visibleRoutines,
        completedRoutineIds = resolvedCompletedIds,
        selectedRoutineId = resolvedSelection,
        singlePane = singlePane,
        isLoading = false,
        feedbackRoutineId = resolvedFeedbackId,
        undoRoutineId = undoRoutineId?.takeIf { it in visibleIds },
        feedbackIntensity = feedbackIntensity,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChildRoutineViewModel(
    private val repository: RoutineRepository,
    private val appSettingsRepository: AppSettingsRepository,
) : ViewModel() {
    private val currentDate = MutableStateFlow(LocalDate.now())
    private val _uiState = MutableStateFlow(ChildRoutineUiState())
    val uiState: StateFlow<ChildRoutineUiState> = _uiState.asStateFlow()
    private val _feedbackEvents = MutableSharedFlow<ChildRoutineFeedbackEvent>()
    val feedbackEvents: SharedFlow<ChildRoutineFeedbackEvent> = _feedbackEvents.asSharedFlow()
    private val settings = MutableStateFlow(AppSettings())
    private val currentTime = MutableStateFlow(LocalTime.now().truncatedTo(ChronoUnit.MINUTES))
    private var latestRoutineSets: List<RoutineSet> = emptyList()
    private var latestCompletedIds: Set<String> = emptySet()
    private var lastGuidedRoutineId: String? = null

    init {
        val routineDataForDate = currentDate.flatMapLatest { date ->
            combine(
                repository.observeRoutineSetsForDate(date),
                repository.observeDailyLogs(date),
            ) { routineSets, logs ->
                ChildRoutineDateData(routineSets = routineSets, logs = logs)
            }
        }
        viewModelScope.launch {
            combine(
                appSettingsRepository.observeAppSettings(),
                routineDataForDate,
                currentTime,
            ) { appSettings, routineData, now ->
                settings.value = appSettings
                val completedIds = routineData.logs
                    .filter { it.status == LogStatus.Completed }
                    .mapTo(mutableSetOf()) { it.routineId }
                latestRoutineSets = routineData.routineSets
                latestCompletedIds = completedIds
                scheduledChildRoutineState(
                    routineSets = routineData.routineSets,
                    completedRoutineIds = completedIds,
                    selectedRoutineId = _uiState.value.selectedRoutineId,
                    singlePane = _uiState.value.singlePane,
                    feedbackRoutineId = _uiState.value.feedbackRoutineId,
                    undoRoutineId = _uiState.value.undoRoutineId,
                    feedbackIntensity = appSettings.feedbackIntensity,
                    now = now,
                )
            }.collectLatest { state ->
                _uiState.value = state
                announceSelectedRoutineIfNeeded(state)
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(30_000L)
                refreshCurrentDate()
                currentTime.value = LocalTime.now().truncatedTo(ChronoUnit.MINUTES)
            }
        }
    }

    fun showList() {
        _uiState.value = _uiState.value.copy(singlePane = ChildSinglePane.List)
    }

    fun showFocus() {
        val state = _uiState.value
        _uiState.value = state.copy(
            selectedRoutineId = if (state.feedbackRoutineId == null) {
                state.currentRoutine?.id
                    ?: state.selectedRoutineId
                    ?: state.routines.firstOrNull()?.id
            } else {
                state.selectedRoutineId
            },
            singlePane = ChildSinglePane.Focus,
        )
    }

    fun selectRoutine(routineId: String) {
        if (_uiState.value.routines.none { it.id == routineId }) return
        val nextState = _uiState.value.copy(
            selectedRoutineId = routineId,
            singlePane = ChildSinglePane.Focus,
        )
        _uiState.value = nextState
        announceSelectedRoutineIfNeeded(nextState)
    }

    fun completeSelectedRoutine() {
        val previousDate = currentDate.value
        val actionDate = refreshCurrentDate()
        if (actionDate != previousDate) return
        val state = _uiState.value
        val routine = state.selectedRoutine ?: return
        if (!state.isSelectedRoutineCompletable || routine.id in state.completedRoutineIds) return

        _uiState.value = state.copy(
            feedbackRoutineId = routine.id,
            undoRoutineId = routine.id,
            singlePane = ChildSinglePane.Focus,
        )
        latestCompletedIds = latestCompletedIds + routine.id

        viewModelScope.launch {
            repository.completeRoutine(routine.id, actionDate)
            val appSettings = settings.value
            _feedbackEvents.emit(
                ChildRoutineFeedbackEvent(
                    spokenText = if (appSettings.ttsEnabled) {
                        "${routine.localizedTitleForDevice(appSettings)} 완료! 잘했어요!"
                    } else {
                        null
                    },
                    vibrate = appSettings.hapticEnabled && appSettings.feedbackIntensity.allowsHaptic(),
                    sound = appSettings.soundEnabled && appSettings.feedbackIntensity.allowsSound(),
                    ttsRate = appSettings.ttsRate.toFloat(),
                    ttsVolume = appSettings.ttsVolume.toFloat(),
                ),
            )
        }
    }

    fun advanceFromFeedback() {
        val state = _uiState.value
        if (state.feedbackRoutineId == null) return
        _uiState.value = scheduledChildRoutineState(
            routineSets = latestRoutineSets,
            completedRoutineIds = latestCompletedIds,
            selectedRoutineId = null,
            singlePane = ChildSinglePane.Focus,
            feedbackRoutineId = null,
            undoRoutineId = null,
            feedbackIntensity = settings.value.feedbackIntensity,
            now = currentTime.value,
        )
        announceSelectedRoutineIfNeeded(_uiState.value)
    }

    fun undoLastCompletion() {
        val previousDate = currentDate.value
        val actionDate = refreshCurrentDate()
        if (actionDate != previousDate) return
        val routineId = _uiState.value.undoRoutineId ?: return
        latestCompletedIds = latestCompletedIds - routineId
        _uiState.value = _uiState.value.copy(
            selectedRoutineId = routineId,
            feedbackRoutineId = null,
            undoRoutineId = null,
            singlePane = ChildSinglePane.Focus,
        )
        viewModelScope.launch {
            repository.undoRoutine(routineId, actionDate)
        }
    }

    fun onDataChanged() {
        refreshCurrentDate()
        currentTime.value = LocalTime.now().truncatedTo(ChronoUnit.MINUTES)
        lastGuidedRoutineId = null
        _uiState.value = _uiState.value.copy(
            feedbackRoutineId = null,
            undoRoutineId = null,
            singlePane = ChildSinglePane.Focus,
        )
    }

    private fun announceSelectedRoutineIfNeeded(state: ChildRoutineUiState) {
        if (state.feedbackRoutineId != null || state.isRoutineSetLocked) return
        val routine = state.selectedRoutine ?: return
        if (routine.id == lastGuidedRoutineId) return
        val appSettings = settings.value
        if (!appSettings.ttsEnabled) return
        lastGuidedRoutineId = routine.id
        viewModelScope.launch {
            _feedbackEvents.emit(
                ChildRoutineFeedbackEvent(
                    spokenText = routine.localizedTitleForDevice(appSettings),
                    vibrate = false,
                    sound = false,
                    ttsRate = appSettings.ttsRate.toFloat(),
                    ttsVolume = appSettings.ttsVolume.toFloat(),
                ),
            )
        }
    }

    private fun Routine.localizedTitleForDevice(settings: AppSettings): String =
        title.resolve(appLocale = settings.locale, systemLocale = Locale.getDefault().toLanguageTag())

    private fun refreshCurrentDate(): LocalDate {
        val today = LocalDate.now()
        if (currentDate.value != today) {
            currentDate.value = today
            lastGuidedRoutineId = null
        }
        return today
    }

    companion object {
        fun factory(repository: RoutineRepository, appSettingsRepository: AppSettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(ChildRoutineViewModel::class.java))
                    return ChildRoutineViewModel(repository, appSettingsRepository) as T
                }
            }
    }
}

private data class ChildRoutineDateData(
    val routineSets: List<RoutineSet>,
    val logs: List<DailyLog>,
)

internal fun scheduledChildRoutineState(
    routineSets: List<RoutineSet>,
    completedRoutineIds: Set<String>,
    selectedRoutineId: String?,
    singlePane: ChildSinglePane,
    feedbackRoutineId: String? = null,
    undoRoutineId: String? = null,
    feedbackIntensity: FeedbackIntensity = FeedbackIntensity.Normal,
    now: LocalTime,
): ChildRoutineUiState {
    val feedbackSet = feedbackRoutineId?.let { id ->
        routineSets.firstOrNull { set -> set.routines.any { it.id == id } }
    }
    val resolution = resolveRoutineSchedule(routineSets, completedRoutineIds, now)
    val displaySet = feedbackSet ?: resolution.currentSet ?: resolution.waitingSet
    val base = childRoutineState(
        routines = displaySet?.routines.orEmpty(),
        completedRoutineIds = completedRoutineIds,
        selectedRoutineId = selectedRoutineId,
        singlePane = singlePane,
        feedbackRoutineId = feedbackRoutineId,
        undoRoutineId = undoRoutineId,
        feedbackIntensity = feedbackIntensity,
    )
    val allVisibleRoutines = routineSets
        .filter { it.isActive && it.deletedAt == null }
        .flatMap { set -> set.routines.filter { it.isActive && it.deletedAt == null } }
    val progressRoutines = when {
        displaySet != null -> displaySet.routines.filter { it.isActive && it.deletedAt == null }
        resolution.waitingSet != null -> resolution.waitingSet.routines.filter { it.isActive && it.deletedAt == null }
        resolution.allComplete -> allVisibleRoutines
        else -> emptyList()
    }
    return base.copy(
        currentRoutineSet = feedbackSet ?: resolution.currentSet,
        waitingRoutineSet = if (feedbackSet == null) resolution.waitingSet else null,
        waitingUntil = if (feedbackSet == null) resolution.waitingSet?.startTime else null,
        dayProgressCount = progressRoutines.count { it.id in completedRoutineIds || it.id == feedbackRoutineId },
        dayProgressTotal = progressRoutines.size,
        dayAllComplete = resolution.allComplete && feedbackRoutineId == null,
        scheduledRoutines = allVisibleRoutines,
        hasRemainingSchedule = !resolution.allComplete,
        isRoutineSetLocked = feedbackSet == null && resolution.waitingSet != null,
    )
}

private fun FeedbackIntensity.allowsHaptic(): Boolean = this == FeedbackIntensity.Strong || this == FeedbackIntensity.Normal

private fun FeedbackIntensity.allowsSound(): Boolean = this == FeedbackIntensity.Strong || this == FeedbackIntensity.Normal
