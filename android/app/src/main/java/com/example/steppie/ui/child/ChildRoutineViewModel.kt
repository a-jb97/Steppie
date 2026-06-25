package com.example.steppie.ui.child

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class ChildSinglePane { Focus, List }

data class ChildRoutineUiState(
    val routines: List<Routine> = emptyList(),
    val selectedRoutineId: String? = null,
    val singlePane: ChildSinglePane = ChildSinglePane.Focus,
    val isLoading: Boolean = true,
) {
    val selectedRoutine: Routine?
        get() = routines.firstOrNull { it.id == selectedRoutineId }
}

internal fun childRoutineState(
    routines: List<Routine>,
    selectedRoutineId: String?,
    singlePane: ChildSinglePane,
): ChildRoutineUiState {
    val visibleRoutines = routines
        .filter { it.isActive && it.deletedAt == null }
        .sortedBy(Routine::order)
    val resolvedSelection = selectedRoutineId
        ?.takeIf { selectedId -> visibleRoutines.any { it.id == selectedId } }
        ?: visibleRoutines.firstOrNull()?.id

    return ChildRoutineUiState(
        routines = visibleRoutines,
        selectedRoutineId = resolvedSelection,
        singlePane = singlePane,
        isLoading = false,
    )
}

class ChildRoutineViewModel(
    private val repository: RoutineRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChildRoutineUiState())
    val uiState: StateFlow<ChildRoutineUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeRoutineSets().collectLatest { routineSets ->
                val activeSet = routineSets.firstOrNull { it.isActive && it.deletedAt == null }
                _uiState.value = childRoutineState(
                    routines = activeSet?.routines.orEmpty(),
                    selectedRoutineId = _uiState.value.selectedRoutineId,
                    singlePane = _uiState.value.singlePane,
                )
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
