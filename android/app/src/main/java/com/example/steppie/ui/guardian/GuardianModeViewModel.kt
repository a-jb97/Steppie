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
import com.example.steppie.domain.model.newUuidV4
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

enum class GuardianDestination { Pin, Home, RoutineEdit, CardEdit, RoutineSetCreate, Security }

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

data class RoutineSetDraft(
    val name: String = "",
    val stepDraft: RoutineDraft = RoutineDraft(),
    val steps: List<RoutineDraft> = emptyList(),
    val editingStepIndex: Int? = null,
)

data class GuardianModeUiState(
    val isActive: Boolean = false,
    val isAuthenticated: Boolean = false,
    val destination: GuardianDestination = GuardianDestination.Pin,
    val pinMode: GuardianPinMode = GuardianPinMode.Enter,
    val pinDigits: String = "",
    val pinError: String? = null,
    val hasGuardianPin: Boolean = false,
    val routineSets: List<RoutineSet> = emptyList(),
    val activeRoutineSet: RoutineSet? = null,
    val routines: List<Routine> = emptyList(),
    val draft: RoutineDraft? = null,
    val routineSetDraft: RoutineSetDraft? = null,
    val routineSetListEditing: Boolean = false,
    val editingRoutineSetId: String? = null,
    val editingRoutineSetName: String = "",
    val draftError: String? = null,
    val pendingDeleteRoutineId: String? = null,
    val pendingDeleteRoutineSetId: String? = null,
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
                settings.hasGuardianPin to routineSets.filter { it.deletedAt == null }
            }.collect { (hasPin, visibleRoutineSets) ->
                _uiState.update { state ->
                    val activeSet = visibleRoutineSets.firstOrNull { it.isActive }
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
                        routineSets = visibleRoutineSets,
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
                routineSetDraft = null,
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
                draft = null,
                routineSetDraft = null,
                draftError = null,
                pendingDeleteRoutineId = null,
                pendingDeleteRoutineSetId = null,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
        }
    }

    fun openSecurity() {
        _uiState.update {
            it.copy(
                destination = GuardianDestination.Security,
                draft = null,
                routineSetDraft = null,
                routineSetListEditing = false,
                editingRoutineSetId = null,
                editingRoutineSetName = "",
                draftError = null,
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
                routineSetDraft = null,
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
                routineSetDraft = null,
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

    fun openRoutineSetCreate() {
        _uiState.update {
            it.copy(
                destination = GuardianDestination.RoutineSetCreate,
                draft = null,
                routineSetDraft = RoutineSetDraft(),
                draftError = null,
                pendingDeleteRoutineId = null,
                pendingDeleteRoutineSetId = null,
                notice = null,
                interactionToken = it.interactionToken + 1,
            )
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
        if (routineSet.isActive) return
        viewModelScope.launch {
            routineRepository.updateRoutineSet(routineSet.copy(isActive = true, updatedAt = Instant.now()))
        }
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

    fun updateRoutineSetName(name: String) = updateRoutineSetDraft { it.copy(name = name) }

    fun updateRoutineSetStepTitle(title: String) = updateRoutineSetDraft {
        it.copy(stepDraft = it.stepDraft.copy(title = title))
    }

    fun updateRoutineSetStepIcon(iconName: String) {
        if (iconName !in BuiltinIconNames.all) return
        updateRoutineSetDraft { it.copy(stepDraft = it.stepDraft.copy(iconName = iconName)) }
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
            openRoutineEdit()
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

    private fun updateRoutineSetDraft(transform: (RoutineSetDraft) -> RoutineSetDraft) {
        _uiState.update {
            it.copy(
                routineSetDraft = it.routineSetDraft?.let(transform),
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
            icon = IconRef.Builtin(step.iconName),
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
