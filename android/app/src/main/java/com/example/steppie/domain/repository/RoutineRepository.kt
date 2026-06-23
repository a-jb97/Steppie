package com.example.steppie.domain.repository

import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.model.RoutineSet
import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    fun observeRoutineSets(): Flow<List<RoutineSet>>
    fun observeRoutineSet(id: String): Flow<RoutineSet?>
    suspend fun getRoutineSet(id: String): RoutineSet?
    suspend fun createRoutineSet(routineSet: RoutineSet): RoutineSet
    suspend fun updateRoutineSet(routineSet: RoutineSet): RoutineSet
    suspend fun deleteRoutineSet(id: String, deletedAt: Instant = Instant.now())

    suspend fun getRoutine(id: String): Routine?
    suspend fun createRoutine(routine: Routine): Routine
    suspend fun updateRoutine(routine: Routine): Routine
    suspend fun deleteRoutine(id: String, deletedAt: Instant = Instant.now())
    suspend fun reorderRoutines(
        routineSetId: String,
        orderedRoutineIds: List<String>,
        updatedAt: Instant = Instant.now(),
    )
}
