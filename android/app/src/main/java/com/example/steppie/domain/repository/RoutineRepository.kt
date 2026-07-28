package com.example.steppie.domain.repository

import com.example.steppie.domain.model.DailyLog
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.model.RoutineSet
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    fun observeRoutineSets(): Flow<List<RoutineSet>>
    fun observeRoutineSetsForRecords(): Flow<List<RoutineSet>>
    fun observeRoutineSet(id: String): Flow<RoutineSet?>
    fun observeRoutineSetsForDate(date: LocalDate): Flow<List<RoutineSet>>
    fun observeRoutineSetForDate(date: LocalDate): Flow<RoutineSet?>
    fun observeSelectedRoutineSetId(date: LocalDate): Flow<String?>
    suspend fun getRoutineSet(id: String): RoutineSet?
    suspend fun selectRoutineSetForDate(
        date: LocalDate,
        routineSetId: String,
        selectedAt: Instant = Instant.now(),
    )
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

    fun observeDailyLogs(date: LocalDate): Flow<List<DailyLog>>
    fun observeDailyLogs(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyLog>>
    fun observeAllDailyLogs(): Flow<List<DailyLog>>
    suspend fun completeRoutine(
        routineId: String,
        date: LocalDate = LocalDate.now(),
        completedAt: Instant = Instant.now(),
    ): DailyLog
    suspend fun undoRoutine(
        routineId: String,
        date: LocalDate = LocalDate.now(),
        updatedAt: Instant = Instant.now(),
    ): DailyLog
}
