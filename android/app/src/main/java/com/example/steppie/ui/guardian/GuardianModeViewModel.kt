package com.example.steppie.ui.guardian

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.steppie.domain.model.BuiltinIconNames
import com.example.steppie.domain.model.IconRef
import com.example.steppie.domain.model.LocalizedText
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.model.RoutineColorTokens
import com.example.steppie.domain.model.RoutineSet
import com.example.steppie.domain.repository.AppSettingsRepository
import com.example.steppie.domain.repository.RoutineRepository
import java.time.Instant
import java.time.LocalTime
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GuardianDestination { Pin, Home, RoutineEdit, CardEdit, Security }

enum class GuardianPinMode { Enter, Setup, ChangeCurrent, ChangeNew }

data class RoutineDraft(
    val routineId: String? = null,
    val title: String = "",
    val iconName: String = "star",
    val colorToken: String = RoutineColorTokens.DEFAULT,
    val scheduledTime: String = "",
) {
    val isNew: Boolean
        get() = routineId == null
}

data class GuardianModeUiState(
    val isActive: Boolean = false,
    val isAuthenticated: Boolean = false,
    val destination: GuardianDestination = GuardianDestination.Pin,
    val pinMode: GuardianPinMode = GuardianPinMode.Enter,
    val pinDigits: String = "",
    val pinError: String? = null,
    val hasGuardianPin: Boolean = false,
    val activeRoutineSet: RoutineSet? = null,
    val routines: List<Routine> = emptyList(),
    val draft: RoutineDraft? = null,
    val draftError: String? = null,
    val pendingDeleteRoutineId: String? = null,
    val notice: String? = null,
    val interactionToken: Long = 0L,
) {
    val title: String
        get() = activeRoutineSet?.name?.resolve(null, Locale.getDefault().toLanguageTag()).orEmpty()
}

class GuardianModeViewModel(
    private val routineRepository: RoutineRepository,
    private val appSettingsRepository: AppSettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GuardianModeUiState())
    val uiState: StateFlow<GuardianModeUiState> = _uiState.asStateFlow()
    private var verifiedPinForChange: String? = null

    init {
        viewModelScope.launch {
            combine(
                appSettingsRepository.observeAppSettings(),
                routineRepository.observeRoutineSets(),
            ) { settings, routineSets ->
                settings.hasGuardianPin to routineSets.firstOrNull { it.isActive && it.deletedAt == null }
            }.collect { (hasPin, activeSet) ->
                _uiState.update { state ->
                    val resolvedPinMode = if (
                        state.isActive &&
                        !state.isAuthenticated &&
                        state.destination == GuardianDestination.Pin
                    ) {
                        if (hasPin) GuardianPinMode.Enter else GuardianPinMode.Setup
                    } else {
                        state.pinMode
                    }
                    state.copy(
                        hasGuardianPin = hasPin,
                        pinMode = resolvedPinMode,
                        activeRoutineSet = activeSet,
                        routines = activeSet?.routines.orEmpty().sortedBy(Routine::order),
                    )
                }
            }
        }
    }

    fun openFromChild() {
        verifiedPinForChange = null
        _uiState.update { state ->
            state.copy(
                isActive = true,
                isAuthenticated = false,
                destination = GuardianDestination.Pin,
                pinMode = if (state.hasGuardianPin) GuardianPinMode.Enter else GuardianPinMode.Setup,
                pinDigits = "",
                pinError = null,
                notice = null,
                interactionToken = state.interactionToken + 1,
            )
        }
    }

    fun closeToChild() {
        verifiedPinForChange = null
        _uiState.update { GuardianModeUiState(hasGuardianPin = it.hasGuardianPin) }
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
                draft = null,
                draftError = null,
                pendingDeleteRoutineId = null,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun openRoutineEdit() {
        _uiState.update {
            it.copy(
                destination = GuardianDestination.RoutineEdit,
                draft = null,
                draftError = null,
                pendingDeleteRoutineId = null,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun openSecurity() {
        _uiState.update {
            it.copy(
                destination = GuardianDestination.Security,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun openPinChange() {
        verifiedPinForChange = null
        _uiState.update {
            it.copy(
                destination = GuardianDestination.Pin,
                pinMode = GuardianPinMode.ChangeCurrent,
                pinDigits = "",
                pinError = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun openNewRoutineEditor() {
        _uiState.update {
            it.copy(
                destination = GuardianDestination.CardEdit,
                draft = RoutineDraft(),
                draftError = null,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun openRoutineEditor(routineId: String) {
        val routine = _uiState.value.routines.firstOrNull { it.id == routineId } ?: return
        val title = routine.title.resolve(null, Locale.getDefault().toLanguageTag())
        val icon = routine.icon as? IconRef.Builtin
        _uiState.update {
            it.copy(
                destination = GuardianDestination.CardEdit,
                draft = RoutineDraft(
                    routineId = routine.id,
                    title = title,
                    iconName = icon?.name ?: "star",
                    colorToken = routine.colorToken,
                    scheduledTime = routine.scheduledTime?.toString().orEmpty(),
                ),
                draftError = null,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun updateDraftTitle(title: String) = updateDraft { it.copy(title = title) }

    fun updateDraftIcon(iconName: String) {
        if (iconName !in BuiltinIconNames.all) return
        updateDraft { it.copy(iconName = iconName) }
    }

    fun updateDraftColor(colorToken: String) {
        if (colorToken !in RoutineColorTokens.all) return
        updateDraft { it.copy(colorToken = colorToken) }
    }

    fun updateDraftScheduledTime(value: String) {
        val sanitized = value.filter { it.isDigit() || it == ':' }.take(5)
        updateDraft { it.copy(scheduledTime = sanitized) }
    }

    fun saveDraft() {
        val state = _uiState.value
        val draft = state.draft ?: return
        val activeSet = state.activeRoutineSet ?: return
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
                        icon = IconRef.Builtin(draft.iconName),
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
                        icon = IconRef.Builtin(draft.iconName),
                        colorToken = draft.colorToken,
                        scheduledTime = scheduledTime,
                        updatedAt = now,
                    ),
                )
            }
            openRoutineEdit()
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

    fun cancelDelete() {
        _uiState.update { it.copy(pendingDeleteRoutineId = null, interactionToken = it.interactionToken + 1) }
    }

    fun confirmDelete() {
        val routineId = _uiState.value.pendingDeleteRoutineId ?: return
        viewModelScope.launch {
            routineRepository.deleteRoutine(routineId)
            _uiState.update {
                it.copy(
                    destination = GuardianDestination.RoutineEdit,
                    draft = null,
                    draftError = null,
                    pendingDeleteRoutineId = null,
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

    private fun handleCompletePin(pin: String) {
        viewModelScope.launch {
            when (_uiState.value.pinMode) {
                GuardianPinMode.Enter -> {
                    if (appSettingsRepository.verifyGuardianPin(pin)) {
                        _uiState.update {
                            it.copy(
                                isAuthenticated = true,
                                destination = GuardianDestination.Home,
                                pinDigits = "",
                                pinError = null,
                                interactionToken = it.interactionToken + 1,
                            )
                        }
                    } else {
                        showPinError()
                    }
                }
                GuardianPinMode.Setup -> {
                    appSettingsRepository.setGuardianPin(pin)
                    _uiState.update {
                        it.copy(
                            isAuthenticated = true,
                            destination = GuardianDestination.Home,
                            pinDigits = "",
                            pinError = null,
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
                    if (currentPin != null && appSettingsRepository.changeGuardianPin(currentPin, pin)) {
                        verifiedPinForChange = null
                        _uiState.update {
                            it.copy(
                                isAuthenticated = true,
                                destination = GuardianDestination.Security,
                                pinDigits = "",
                                pinError = null,
                                notice = "PIN이 변경되었습니다.",
                                interactionToken = it.interactionToken + 1,
                            )
                        }
                    } else {
                        showPinError()
                    }
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

    private fun parseScheduledTime(value: String): LocalTime? {
        if (value.isBlank()) return null
        return runCatching { LocalTime.parse(value) }.getOrNull()
            ?.takeIf { it.second == 0 && it.nano == 0 }
    }

    companion object {
        fun factory(
            routineRepository: RoutineRepository,
            appSettingsRepository: AppSettingsRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(GuardianModeViewModel::class.java))
                return GuardianModeViewModel(routineRepository, appSettingsRepository) as T
            }
        }
    }
}
