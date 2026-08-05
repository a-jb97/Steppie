package com.example.steppie.ui.child

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.steppie.core.environment.ClockProvider
import com.example.steppie.core.environment.LocaleProvider
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
import kotlinx.coroutines.CancellationException
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

data class ChildRoutineFeedbackEvent(
    val spokenText: String?,
    val vibrate: Boolean,
    val sound: Boolean,
    val ttsRate: Float,
    val ttsVolume: Float,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ChildRoutineViewModel(
    private val repository: RoutineRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val clockProvider: ClockProvider,
    private val localeProvider: LocaleProvider,
) : ViewModel() {
    private val currentDate = MutableStateFlow(clockProvider.today())
    private val _uiState = MutableStateFlow(ChildRoutineUiState())
    val uiState: StateFlow<ChildRoutineUiState> = _uiState.asStateFlow()
    private val _feedbackEvents = MutableSharedFlow<ChildRoutineFeedbackEvent>()
    val feedbackEvents: SharedFlow<ChildRoutineFeedbackEvent> = _feedbackEvents.asSharedFlow()
    private val settings = MutableStateFlow(AppSettings())
    private val currentTime = MutableStateFlow(clockProvider.currentTime().truncatedTo(ChronoUnit.MINUTES))
    private var latestRoutineSets: List<RoutineSet> = emptyList()
    private var latestCompletedIds: Set<String> = emptySet()
    private var lastGuidedRoutineId: String? = null
    private var routineWriteInProgress = false

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
                currentTime.value = clockProvider.currentTime().truncatedTo(ChronoUnit.MINUTES)
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
        if (routineWriteInProgress) return
        val previousDate = currentDate.value
        val actionDate = refreshCurrentDate()
        if (actionDate != previousDate) return
        val state = _uiState.value
        val routine = state.selectedRoutine ?: return
        if (!state.isSelectedRoutineCompletable || routine.id in state.completedRoutineIds) return

        val previousCompletedIds = latestCompletedIds
        routineWriteInProgress = true
        _uiState.value = state.copy(
            feedbackRoutineId = routine.id,
            undoRoutineId = routine.id,
            singlePane = ChildSinglePane.Focus,
        )
        latestCompletedIds = latestCompletedIds + routine.id

        viewModelScope.launch {
            try {
                repository.completeRoutine(routine.id, actionDate, clockProvider.now())
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
            } catch (error: CancellationException) {
                rollbackRoutineWrite(state, previousCompletedIds)
                throw error
            } catch (_: Exception) {
                rollbackRoutineWrite(state, previousCompletedIds)
            } finally {
                routineWriteInProgress = false
            }
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
        if (routineWriteInProgress) return
        val previousDate = currentDate.value
        val actionDate = refreshCurrentDate()
        if (actionDate != previousDate) return
        val state = _uiState.value
        val routineId = state.undoRoutineId ?: return
        val previousCompletedIds = latestCompletedIds
        routineWriteInProgress = true
        latestCompletedIds = latestCompletedIds - routineId
        _uiState.value = state.copy(
            selectedRoutineId = routineId,
            feedbackRoutineId = null,
            undoRoutineId = null,
            singlePane = ChildSinglePane.Focus,
        )
        viewModelScope.launch {
            try {
                repository.undoRoutine(routineId, actionDate, clockProvider.now())
            } catch (error: CancellationException) {
                rollbackRoutineWrite(state, previousCompletedIds)
                throw error
            } catch (_: Exception) {
                rollbackRoutineWrite(state, previousCompletedIds)
            } finally {
                routineWriteInProgress = false
            }
        }
    }

    fun onDataChanged() {
        refreshCurrentDate()
        currentTime.value = clockProvider.currentTime().truncatedTo(ChronoUnit.MINUTES)
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

    private fun rollbackRoutineWrite(
        previousState: ChildRoutineUiState,
        previousCompletedIds: Set<String>,
    ) {
        latestCompletedIds = previousCompletedIds
        _uiState.value = _uiState.value.copy(
            selectedRoutineId = previousState.selectedRoutineId,
            singlePane = previousState.singlePane,
            feedbackRoutineId = previousState.feedbackRoutineId,
            undoRoutineId = previousState.undoRoutineId,
        )
    }

    private fun Routine.localizedTitleForDevice(settings: AppSettings): String =
        title.resolve(appLocale = settings.locale, systemLocale = localeProvider.languageTag())

    private fun refreshCurrentDate(): LocalDate {
        val today = clockProvider.today()
        if (currentDate.value != today) {
            currentDate.value = today
            lastGuidedRoutineId = null
        }
        return today
    }

    companion object {
        fun factory(
            repository: RoutineRepository,
            appSettingsRepository: AppSettingsRepository,
            clockProvider: ClockProvider,
            localeProvider: LocaleProvider,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(ChildRoutineViewModel::class.java))
                    return ChildRoutineViewModel(
                        repository,
                        appSettingsRepository,
                        clockProvider,
                        localeProvider,
                    ) as T
                }
            }
    }
}

private data class ChildRoutineDateData(
    val routineSets: List<RoutineSet>,
    val logs: List<DailyLog>,
)

private fun FeedbackIntensity.allowsHaptic(): Boolean = this == FeedbackIntensity.Strong || this == FeedbackIntensity.Normal

private fun FeedbackIntensity.allowsSound(): Boolean = this == FeedbackIntensity.Strong || this == FeedbackIntensity.Normal
