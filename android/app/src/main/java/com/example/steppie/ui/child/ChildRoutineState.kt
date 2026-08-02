package com.example.steppie.ui.child

import com.example.steppie.domain.model.FeedbackIntensity
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.model.RoutineSet
import com.example.steppie.domain.model.childRoutineSetsInScheduleOrder
import com.example.steppie.domain.model.childRoutinesInOrder
import com.example.steppie.domain.model.isAvailableToChild
import java.time.LocalTime

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
    val scheduledSets = routineSets.childRoutineSetsInScheduleOrder()
    val firstIncomplete = scheduledSets.firstOrNull { set ->
        set.routines.any { it.isAvailableToChild && it.id !in completedRoutineIds }
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

internal fun childRoutineState(
    routines: List<Routine>,
    completedRoutineIds: Set<String>,
    selectedRoutineId: String?,
    singlePane: ChildSinglePane,
    feedbackRoutineId: String? = null,
    undoRoutineId: String? = null,
    feedbackIntensity: FeedbackIntensity = FeedbackIntensity.Normal,
): ChildRoutineUiState {
    val visibleRoutines = routines.childRoutinesInOrder()
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
        .childRoutineSetsInScheduleOrder()
        .flatMap { set -> set.routines.childRoutinesInOrder() }
    val progressRoutines = when {
        displaySet != null -> displaySet.routines.childRoutinesInOrder()
        resolution.waitingSet != null -> resolution.waitingSet.routines.childRoutinesInOrder()
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
