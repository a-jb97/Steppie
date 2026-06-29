package com.example.steppie.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Transaction
    @Query("SELECT * FROM routine_sets WHERE deletedAtEpochMillis IS NULL ORDER BY createdAtEpochMillis, id")
    fun observeRoutineSets(): Flow<List<RoutineSetWithRoutines>>

    @Transaction
    @Query("SELECT * FROM routine_sets WHERE id = :id AND deletedAtEpochMillis IS NULL")
    fun observeRoutineSet(id: String): Flow<RoutineSetWithRoutines?>

    @Transaction
    @Query("SELECT * FROM routine_sets WHERE id = :id AND deletedAtEpochMillis IS NULL")
    suspend fun getRoutineSet(id: String): RoutineSetWithRoutines?

    @Query("SELECT * FROM routine_sets WHERE id = :id")
    suspend fun getRoutineSetEntity(id: String): RoutineSetEntity?

    @Query("SELECT * FROM routine_sets ORDER BY createdAtEpochMillis, id")
    suspend fun getAllRoutineSetEntities(): List<RoutineSetEntity>

    @Query("SELECT id FROM routine_sets WHERE isActive = 1 AND deletedAtEpochMillis IS NULL LIMIT 1")
    suspend fun getActiveRoutineSetId(): String?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRoutineSet(entity: RoutineSetEntity)

    @Update
    suspend fun updateRoutineSet(entity: RoutineSetEntity)

    @Query("UPDATE routine_sets SET isActive = 0, updatedAtEpochMillis = :updatedAt WHERE isActive = 1 AND id != :exceptId")
    suspend fun deactivateOtherRoutineSets(exceptId: String, updatedAt: Long)

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutineEntity(id: String): RoutineEntity?

    @Query("SELECT * FROM routines ORDER BY routineSetId, sortOrder, id")
    suspend fun getAllRoutineEntities(): List<RoutineEntity>

    @Query(
        """
        SELECT routines.* FROM routines
        INNER JOIN routine_sets ON routine_sets.id = routines.routineSetId
        WHERE routines.id = :id
          AND routines.deletedAtEpochMillis IS NULL
          AND routine_sets.deletedAtEpochMillis IS NULL
        """,
    )
    suspend fun getVisibleRoutineEntity(id: String): RoutineEntity?

    @Query("SELECT * FROM routines WHERE routineSetId = :routineSetId AND deletedAtEpochMillis IS NULL ORDER BY sortOrder, id")
    suspend fun getVisibleRoutineEntities(routineSetId: String): List<RoutineEntity>

    @Query("SELECT COALESCE(MAX(sortOrder) + 1, 0) FROM routines WHERE routineSetId = :routineSetId AND deletedAtEpochMillis IS NULL")
    suspend fun nextRoutineOrder(routineSetId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRoutine(entity: RoutineEntity)

    @Update
    suspend fun updateRoutine(entity: RoutineEntity)

    @Query("SELECT * FROM daily_logs WHERE date = :date ORDER BY updatedAtEpochMillis, id")
    fun observeDailyLogs(date: String): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs ORDER BY date, updatedAtEpochMillis, id")
    suspend fun getAllDailyLogEntities(): List<DailyLogEntity>

    @Query("SELECT * FROM daily_logs WHERE date = :date AND routineId = :routineId LIMIT 1")
    suspend fun getDailyLog(date: String, routineId: String): DailyLogEntity?

    @Upsert
    suspend fun upsertDailyLog(entity: DailyLogEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRoutineSets(entities: List<RoutineSetEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRoutines(entities: List<RoutineEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDailyLogs(entities: List<DailyLogEntity>)

    @Query("DELETE FROM daily_logs")
    suspend fun deleteAllDailyLogs()

    @Query("DELETE FROM routines")
    suspend fun deleteAllRoutines()

    @Query("DELETE FROM routine_sets")
    suspend fun deleteAllRoutineSets()
}
