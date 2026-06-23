package com.example.steppie.data.repository

import androidx.room.withTransaction
import com.example.steppie.data.local.RoutineDao
import com.example.steppie.data.local.SteppieDatabase
import com.example.steppie.data.local.toDomain
import com.example.steppie.data.local.toEntity
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.model.RoutineSet
import com.example.steppie.domain.model.requireUuidV4
import com.example.steppie.domain.repository.RoutineRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomRoutineRepository(
    private val database: SteppieDatabase,
    private val dao: RoutineDao = database.routineDao(),
) : RoutineRepository {
    override fun observeRoutineSets(): Flow<List<RoutineSet>> =
        dao.observeRoutineSets().map { sets -> sets.map { it.toDomain() } }

    override fun observeRoutineSet(id: String): Flow<RoutineSet?> {
        requireUuidV4(id, "RoutineSet.id")
        return dao.observeRoutineSet(id).map { it?.toDomain() }
    }

    override suspend fun getRoutineSet(id: String): RoutineSet? {
        requireUuidV4(id, "RoutineSet.id")
        return dao.getRoutineSet(id)?.toDomain()
    }

    override suspend fun createRoutineSet(routineSet: RoutineSet): RoutineSet = database.withTransaction {
        check(dao.getRoutineSetEntity(routineSet.id) == null) { "RoutineSet already exists: ${routineSet.id}" }
        if (routineSet.isActive) {
            dao.deactivateOtherRoutineSets(routineSet.id, routineSet.updatedAt.toEpochMilli())
        }
        dao.insertRoutineSet(routineSet.toEntity())
        routineSet.routines.sortedBy(Routine::order).forEach { dao.insertRoutine(it.toEntity()) }
        requireNotNull(dao.getRoutineSet(routineSet.id)).toDomain()
    }

    override suspend fun updateRoutineSet(routineSet: RoutineSet): RoutineSet = database.withTransaction {
        val existing = requireNotNull(dao.getRoutineSetEntity(routineSet.id)) {
            "RoutineSet not found: ${routineSet.id}"
        }
        check(existing.deletedAtEpochMillis == null) { "Deleted RoutineSet cannot be updated." }
        check(routineSet.createdAt.toEpochMilli() == existing.createdAtEpochMillis) {
            "RoutineSet.createdAt is immutable."
        }
        check(routineSet.updatedAt.toEpochMilli() >= existing.updatedAtEpochMillis) {
            "RoutineSet.updatedAt cannot move backwards."
        }
        check(!(existing.isActive && !routineSet.isActive && dao.getActiveRoutineSetId() == routineSet.id)) {
            "The active RoutineSet must be replaced by activating another set."
        }
        if (routineSet.isActive) {
            dao.deactivateOtherRoutineSets(routineSet.id, routineSet.updatedAt.toEpochMilli())
        }
        dao.updateRoutineSet(routineSet.copy(routines = emptyList()).toEntity())
        requireNotNull(dao.getRoutineSet(routineSet.id)).toDomain()
    }

    override suspend fun deleteRoutineSet(id: String, deletedAt: Instant) = database.withTransaction {
        requireUuidV4(id, "RoutineSet.id")
        val existing = requireNotNull(dao.getRoutineSetEntity(id)) { "RoutineSet not found: $id" }
        check(!existing.isActive) { "An active RoutineSet cannot be deleted." }
        if (existing.deletedAtEpochMillis == null) {
            check(deletedAt.toEpochMilli() >= existing.createdAtEpochMillis) {
                "deletedAt cannot precede createdAt."
            }
            dao.updateRoutineSet(
                existing.copy(
                    updatedAtEpochMillis = deletedAt.toEpochMilli(),
                    deletedAtEpochMillis = deletedAt.toEpochMilli(),
                ),
            )
        }
    }

    override suspend fun getRoutine(id: String): Routine? {
        requireUuidV4(id, "Routine.id")
        return dao.getVisibleRoutineEntity(id)?.toDomain()
    }

    override suspend fun createRoutine(routine: Routine): Routine = database.withTransaction {
        check(dao.getRoutineEntity(routine.id) == null) { "Routine already exists: ${routine.id}" }
        requireNotNull(dao.getRoutineSet(routine.routineSetId)) {
            "RoutineSet not found: ${routine.routineSetId}"
        }
        val saved = routine.copy(order = dao.nextRoutineOrder(routine.routineSetId))
        dao.insertRoutine(saved.toEntity())
        touchRoutineSet(saved.routineSetId, saved.updatedAt)
        saved.toEntity().toDomain()
    }

    override suspend fun updateRoutine(routine: Routine): Routine = database.withTransaction {
        val existing = requireNotNull(dao.getRoutineEntity(routine.id)) { "Routine not found: ${routine.id}" }
        check(existing.deletedAtEpochMillis == null) { "Deleted Routine cannot be updated." }
        requireNotNull(dao.getRoutineSet(routine.routineSetId)) {
            "A Routine in a deleted RoutineSet cannot be updated."
        }
        check(routine.routineSetId == existing.routineSetId) { "Routine.routineSetId is immutable." }
        check(routine.createdAt.toEpochMilli() == existing.createdAtEpochMillis) { "Routine.createdAt is immutable." }
        check(routine.order == existing.sortOrder) { "Use reorderRoutines to change Routine.order." }
        check(routine.updatedAt.toEpochMilli() >= existing.updatedAtEpochMillis) {
            "Routine.updatedAt cannot move backwards."
        }
        dao.updateRoutine(routine.toEntity())
        touchRoutineSet(routine.routineSetId, routine.updatedAt)
        requireNotNull(dao.getVisibleRoutineEntity(routine.id)).toDomain()
    }

    override suspend fun deleteRoutine(id: String, deletedAt: Instant) = database.withTransaction {
        requireUuidV4(id, "Routine.id")
        val existing = requireNotNull(dao.getRoutineEntity(id)) { "Routine not found: $id" }
        if (existing.deletedAtEpochMillis == null) {
            check(deletedAt.toEpochMilli() >= existing.createdAtEpochMillis) {
                "deletedAt cannot precede createdAt."
            }
            dao.updateRoutine(
                existing.copy(
                    isActive = false,
                    updatedAtEpochMillis = deletedAt.toEpochMilli(),
                    deletedAtEpochMillis = deletedAt.toEpochMilli(),
                ),
            )
            normalizeOrder(existing.routineSetId, deletedAt)
        }
    }

    override suspend fun reorderRoutines(
        routineSetId: String,
        orderedRoutineIds: List<String>,
        updatedAt: Instant,
    ) = database.withTransaction {
        requireUuidV4(routineSetId, "RoutineSet.id")
        requireNotNull(dao.getRoutineSet(routineSetId)) { "RoutineSet not found: $routineSetId" }
        val existing = dao.getVisibleRoutineEntities(routineSetId)
        check(orderedRoutineIds.size == orderedRoutineIds.distinct().size) {
            "orderedRoutineIds cannot contain duplicates."
        }
        check(orderedRoutineIds.toSet() == existing.map { it.id }.toSet()) {
            "orderedRoutineIds must contain every visible routine in the set exactly once."
        }
        check(existing.all { updatedAt.toEpochMilli() >= it.createdAtEpochMillis }) {
            "updatedAt cannot precede a routine's createdAt."
        }
        val byId = existing.associateBy { it.id }
        orderedRoutineIds.forEachIndexed { index, id ->
            dao.updateRoutine(
                requireNotNull(byId[id]).copy(
                    sortOrder = index,
                    updatedAtEpochMillis = updatedAt.toEpochMilli(),
                ),
            )
        }
        touchRoutineSet(routineSetId, updatedAt)
    }

    private suspend fun normalizeOrder(routineSetId: String, updatedAt: Instant) {
        dao.getVisibleRoutineEntities(routineSetId).forEachIndexed { index, entity ->
            dao.updateRoutine(
                entity.copy(sortOrder = index, updatedAtEpochMillis = updatedAt.toEpochMilli()),
            )
        }
        touchRoutineSet(routineSetId, updatedAt)
    }

    private suspend fun touchRoutineSet(routineSetId: String, updatedAt: Instant) {
        val set = requireNotNull(dao.getRoutineSetEntity(routineSetId))
        dao.updateRoutineSet(
            set.copy(updatedAtEpochMillis = maxOf(set.updatedAtEpochMillis, updatedAt.toEpochMilli())),
        )
    }
}
