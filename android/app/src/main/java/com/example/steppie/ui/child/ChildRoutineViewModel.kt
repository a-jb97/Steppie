package com.example.steppie.ui.child

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.steppie.domain.model.LogStatus
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.repository.RoutineRepository
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class ChildSinglePane { Focus, List }

data class ChildRoutineUiState(
    val routines: List<Routine> = emptyList(),
    val completedRoutineIds: Set<String> = emptySet(),
    val selectedRoutineId: String? = null,
    val singlePane: ChildSinglePane = ChildSinglePane.Focus,
    val isLoading: Boolean = true,
    val feedbackRoutineId: String? = null,
    val undoRoutineId: String? = null,
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
    val nextIncompleteRoutine: Routine?
        get() = routines.firstOrNull { it.id !in completedRoutineIds && it.id != feedbackRoutineId }
    val progressCount: Int
        get() = routines.count { it.id in completedRoutineIds || it.id == feedbackRoutineId }
    val progressTotal: Int
        get() = routines.size
    val isAllComplete: Boolean
        get() = routines.isNotEmpty() && routines.all { it.id in completedRoutineIds }
}

data class ChildRoutineFeedbackEvent(
    val spokenText: String,
    val vibrate: Boolean,
)

internal fun childRoutineState(
    routines: List<Routine>,
    completedRoutineIds: Set<String>,
    selectedRoutineId: String?,
    singlePane: ChildSinglePane,
    feedbackRoutineId: String? = null,
    undoRoutineId: String? = null,
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
    )
}

class ChildRoutineViewModel(
    private val repository: RoutineRepository,
) : ViewModel() {
    private val today = LocalDate.now()

    private val _uiState = MutableStateFlow(ChildRoutineUiState())
    val uiState: StateFlow<ChildRoutineUiState> = _uiState.asStateFlow()
    private val _feedbackEvents = MutableSharedFlow<ChildRoutineFeedbackEvent>()
    val feedbackEvents: SharedFlow<ChildRoutineFeedbackEvent> = _feedbackEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeRoutineSets(),
                repository.observeDailyLogs(today),
            ) { routineSets, logs ->
                val activeSet = routineSets.firstOrNull { it.isActive && it.deletedAt == null }
                val completedIds = logs
                    .filter { it.status == LogStatus.Completed }
                    .mapTo(mutableSetOf()) { it.routineId }
                childRoutineState(
                    routines = activeSet?.routines.orEmpty(),
                    completedRoutineIds = completedIds,
                    selectedRoutineId = _uiState.value.selectedRoutineId,
                    singlePane = _uiState.value.singlePane,
                    feedbackRoutineId = _uiState.value.feedbackRoutineId,
                    undoRoutineId = _uiState.value.undoRoutineId,
                )
            }.collectLatest { state ->
                _uiState.value = state
            }
        }
    }

    fun showList() {
        _uiState.value = _uiState.value.copy(singlePane = ChildSinglePane.List)
    }

    fun showFocus() {
        _uiState.value = _uiState.value.copy(singlePane = ChildSinglePane.Focus)
    }

    fun selectRoutine(routineId: String) {
        if (_uiState.value.routines.none { it.id == routineId }) return
        _uiState.value = _uiState.value.copy(
            selectedRoutineId = routineId,
            singlePane = ChildSinglePane.Focus,
        )
    }

    fun completeSelectedRoutine() {
        val state = _uiState.value
        val routine = state.selectedRoutine ?: return
        if (routine.id in state.completedRoutineIds || state.feedbackRoutineId != null) return

        _uiState.value = state.copy(
            feedbackRoutineId = routine.id,
            undoRoutineId = routine.id,
            singlePane = ChildSinglePane.Focus,
        )

        viewModelScope.launch {
            repository.completeRoutine(routine.id, today)
            _feedbackEvents.emit(
                ChildRoutineFeedbackEvent(
                    spokenText = "${routine.localizedTitleForDevice()} 완료! 잘했어요!",
                    vibrate = true,
                ),
            )
        }
    }

    fun advanceFromFeedback() {
        val state = _uiState.value
        if (state.feedbackRoutineId == null) return
        _uiState.value = state.copy(
            selectedRoutineId = state.nextIncompleteRoutine?.id,
            feedbackRoutineId = null,
            undoRoutineId = null,
            singlePane = ChildSinglePane.Focus,
        )
    }

    fun undoLastCompletion() {
        val routineId = _uiState.value.undoRoutineId ?: return
        _uiState.value = _uiState.value.copy(
            selectedRoutineId = routineId,
            feedbackRoutineId = null,
            undoRoutineId = null,
            singlePane = ChildSinglePane.Focus,
        )
        viewModelScope.launch {
            repository.undoRoutine(routineId, today)
        }
    }

    private fun Routine.localizedTitleForDevice(): String =
        title.resolve(appLocale = null, systemLocale = Locale.getDefault().toLanguageTag())

    companion object {
        fun factory(repository: RoutineRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(ChildRoutineViewModel::class.java))
                    return ChildRoutineViewModel(repository) as T
                }
            }
    }
}
