package com.example.steppie.data.repository

import com.example.steppie.domain.model.DailyLog
import com.example.steppie.domain.model.LogStatus
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.model.RoutineSet
import com.example.steppie.domain.model.requireUuidV4
import com.example.steppie.domain.repository.RoutineRepository
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryRoutineRepository(
    initialData: List<RoutineSet> = emptyList(),
) : RoutineRepository {
    private val mutex = Mutex()
    private val state = MutableStateFlow(initialData.associateBy(RoutineSet::id))
    private val logs = MutableStateFlow<Map<Pair<LocalDate, String>, DailyLog>>(emptyMap())

    override fun observeRoutineSets(): Flow<List<RoutineSet>> = state
        .map { sets -> sets.values.filter { it.deletedAt == null }.sortedBy { it.createdAt }.map(::visible) }
        .distinctUntilChanged()

    override fun observeRoutineSetsForRecords(): Flow<List<RoutineSet>> = state
        .map { sets -> sets.values.sortedBy { it.createdAt } }
        .distinctUntilChanged()

    override fun observeRoutineSet(id: String): Flow<RoutineSet?> {
        requireUuidV4(id, "RoutineSet.id")
        return state.map { it[id]?.takeIf { set -> set.deletedAt == null }?.let(::visible) }.distinctUntilChanged()
    }

    override suspend fun getRoutineSet(id: String): RoutineSet? {
        requireUuidV4(id, "RoutineSet.id")
        return state.value[id]?.takeIf { it.deletedAt == null }?.let(::visible)
    }

    override suspend fun createRoutineSet(routineSet: RoutineSet): RoutineSet = mutex.withLock {
        check(routineSet.id !in state.value) { "RoutineSet already exists: ${routineSet.id}" }
        val updated = if (routineSet.isActive) deactivateOthers(state.value, routineSet.id, routineSet.updatedAt) else state.value
        state.value = updated + (routineSet.id to routineSet)
        visible(routineSet)
    }

    override suspend fun updateRoutineSet(routineSet: RoutineSet): RoutineSet = mutex.withLock {
        val existing = requireNotNull(state.value[routineSet.id]) { "RoutineSet not found: ${routineSet.id}" }
        check(existing.deletedAt == null) { "Deleted RoutineSet cannot be updated." }
        check(existing.createdAt == routineSet.createdAt) { "RoutineSet.createdAt is immutable." }
        check(routineSet.updatedAt >= existing.updatedAt) { "RoutineSet.updatedAt cannot move backwards." }
        check(!(existing.isActive && !routineSet.isActive)) {
            "The active RoutineSet must be replaced by activating another set."
        }
        val replacement = routineSet.copy(routines = existing.routines)
        val updated = if (replacement.isActive) {
            deactivateOthers(state.value, replacement.id, replacement.updatedAt)
        } else {
            state.value
        }
        state.value = updated + (replacement.id to replacement)
        visible(replacement)
    }

    override suspend fun deleteRoutineSet(id: String, deletedAt: Instant) = mutex.withLock {
        requireUuidV4(id, "RoutineSet.id")
        val existing = requireNotNull(state.value[id]) { "RoutineSet not found: $id" }
        check(!existing.isActive) { "An active RoutineSet cannot be deleted." }
        if (existing.deletedAt == null) {
            state.value = state.value + (id to existing.copy(updatedAt = deletedAt, deletedAt = deletedAt))
        }
    }

    override suspend fun getRoutine(id: String): Routine? {
        requireUuidV4(id, "Routine.id")
        return state.value.values.asSequence()
            .flatMap { it.routines.asSequence() }
            .firstOrNull { it.id == id && it.deletedAt == null }
    }

    override suspend fun createRoutine(routine: Routine): Routine = mutex.withLock {
        check(state.value.values.none { set -> set.routines.any { it.id == routine.id } }) {
            "Routine already exists: ${routine.id}"
        }
        val set = requireNotNull(state.value[routine.routineSetId]?.takeIf { it.deletedAt == null }) {
            "RoutineSet not found: ${routine.routineSetId}"
        }
        val saved = routine.copy(order = set.routines.count { it.deletedAt == null })
        replace(set.copy(updatedAt = maxOf(set.updatedAt, saved.updatedAt), routines = set.routines + saved))
        saved
    }

    override suspend fun updateRoutine(routine: Routine): Routine = mutex.withLock {
        val set = findSetForRoutine(routine.id)
        val existing = requireNotNull(set.routines.firstOrNull { it.id == routine.id })
        check(existing.deletedAt == null) { "Deleted Routine cannot be updated." }
        check(existing.routineSetId == routine.routineSetId) { "Routine.routineSetId is immutable." }
        check(existing.createdAt == routine.createdAt) { "Routine.createdAt is immutable." }
        check(existing.order == routine.order) { "Use reorderRoutines to change Routine.order." }
        check(routine.updatedAt >= existing.updatedAt) { "Routine.updatedAt cannot move backwards." }
        replace(
            set.copy(
                updatedAt = maxOf(set.updatedAt, routine.updatedAt),
                routines = set.routines.map { if (it.id == routine.id) routine else it },
            ),
        )
        routine
    }

    override suspend fun deleteRoutine(id: String, deletedAt: Instant) = mutex.withLock {
        requireUuidV4(id, "Routine.id")
        val set = findSetForRoutine(id)
        val target = requireNotNull(set.routines.firstOrNull { it.id == id })
        if (target.deletedAt == null) {
            val remaining = set.routines
                .map { if (it.id == id) it.copy(isActive = false, updatedAt = deletedAt, deletedAt = deletedAt) else it }
                .renumberVisible(deletedAt)
            replace(set.copy(updatedAt = deletedAt, routines = remaining))
        }
    }

    override suspend fun reorderRoutines(
        routineSetId: String,
        orderedRoutineIds: List<String>,
        updatedAt: Instant,
    ) = mutex.withLock {
        val set = requireNotNull(state.value[routineSetId]?.takeIf { it.deletedAt == null }) {
            "RoutineSet not found: $routineSetId"
        }
        val visible = set.routines.filter { it.deletedAt == null }
        check(orderedRoutineIds.size == orderedRoutineIds.distinct().size &&
            orderedRoutineIds.toSet() == visible.map { it.id }.toSet()) {
            "orderedRoutineIds must contain every visible routine in the set exactly once."
        }
        val orderById = orderedRoutineIds.withIndex().associate { it.value to it.index }
        replace(
            set.copy(
                updatedAt = updatedAt,
                routines = set.routines.map {
                    orderById[it.id]?.let { order -> it.copy(order = order, updatedAt = updatedAt) } ?: it
                },
            ),
        )
    }

    override fun observeDailyLogs(date: LocalDate): Flow<List<DailyLog>> = logs
        .map { source ->
            source.values
                .filter { it.date == date }
                .sortedWith(compareBy<DailyLog> { it.updatedAt }.thenBy { it.id })
        }
        .distinctUntilChanged()

    override fun observeDailyLogs(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyLog>> = logs
        .map { source ->
            source.values
                .filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }
                .sortedWith(compareByDescending<DailyLog> { it.date }.thenBy { it.updatedAt }.thenBy { it.id })
        }
        .distinctUntilChanged()

    override fun observeAllDailyLogs(): Flow<List<DailyLog>> = logs
        .map { source ->
            source.values.sortedWith(compareByDescending<DailyLog> { it.date }.thenBy { it.updatedAt }.thenBy { it.id })
        }
        .distinctUntilChanged()

    override suspend fun completeRoutine(
        routineId: String,
        date: LocalDate,
        completedAt: Instant,
    ): DailyLog = mutex.withLock {
        requireUuidV4(routineId, "Routine.id")
        val routine = requireNotNull(getRoutine(routineId)) { "Routine not found: $routineId" }
        val key = date to routineId
        val existing = logs.value[key]
        val saved = DailyLog(
            id = existing?.id ?: com.example.steppie.domain.model.newUuidV4(),
            date = date,
            routineId = routineId,
            routineSetId = routine.routineSetId,
            status = LogStatus.Completed,
            completedAt = completedAt,
            createdAt = existing?.createdAt ?: completedAt,
            updatedAt = completedAt,
        )
        logs.value = logs.value + (key to saved)
        saved
    }

    override suspend fun undoRoutine(
        routineId: String,
        date: LocalDate,
        updatedAt: Instant,
    ): DailyLog = mutex.withLock {
        requireUuidV4(routineId, "Routine.id")
        val routine = requireNotNull(getRoutine(routineId)) { "Routine not found: $routineId" }
        val key = date to routineId
        val existing = logs.value[key]
        val saved = DailyLog(
            id = existing?.id ?: com.example.steppie.domain.model.newUuidV4(),
            date = date,
            routineId = routineId,
            routineSetId = routine.routineSetId,
            status = LogStatus.Undone,
            completedAt = null,
            createdAt = existing?.createdAt ?: updatedAt,
            updatedAt = updatedAt,
        )
        logs.value = logs.value + (key to saved)
        saved
    }

    private fun replace(set: RoutineSet) {
        state.value = state.value + (set.id to set)
    }

    private fun findSetForRoutine(id: String): RoutineSet = requireNotNull(
        state.value.values.firstOrNull { set -> set.routines.any { it.id == id } },
    ) { "Routine not found: $id" }

    private fun visible(set: RoutineSet): RoutineSet = set.copy(
        routines = set.routines.filter { it.deletedAt == null }.sortedBy(Routine::order),
    )

    private fun List<Routine>.renumberVisible(updatedAt: Instant): List<Routine> {
        var next = 0
        return map { routine ->
            if (routine.deletedAt == null) routine.copy(order = next++, updatedAt = updatedAt) else routine
        }
    }

    private fun deactivateOthers(
        source: Map<String, RoutineSet>,
        exceptId: String,
        updatedAt: Instant,
    ): Map<String, RoutineSet> = source.mapValues { (id, set) ->
        if (id != exceptId && set.isActive) set.copy(isActive = false, updatedAt = updatedAt) else set
    }
}
