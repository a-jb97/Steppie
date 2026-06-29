package com.example.steppie.data.backup

import androidx.room.withTransaction
import com.example.steppie.data.local.DailyLogEntity
import com.example.steppie.data.local.RoutineDao
import com.example.steppie.data.local.RoutineEntity
import com.example.steppie.data.local.RoutineSetEntity
import com.example.steppie.data.local.SteppieDatabase
import com.example.steppie.data.repository.DataStoreAppSettingsRepository
import java.time.Instant

class BackupDataSource(
    private val database: SteppieDatabase,
    private val appSettingsRepository: DataStoreAppSettingsRepository,
    private val dao: RoutineDao = database.routineDao(),
) {
    suspend fun snapshot(exportedAt: Instant = Instant.now()): BackupSnapshot = BackupSnapshot(
        exportedAt = exportedAt,
        routineSets = dao.getAllRoutineSetEntities(),
        routines = dao.getAllRoutineEntities(),
        dailyLogs = dao.getAllDailyLogEntities(),
        appSettings = appSettingsRepository.getAppSettings(),
    )

    suspend fun replaceAll(newSnapshot: BackupSnapshot) {
        val previous = snapshot()
        try {
            replaceRoomData(
                routineSets = newSnapshot.routineSets,
                routines = newSnapshot.routines,
                dailyLogs = newSnapshot.dailyLogs,
            )
            appSettingsRepository.replaceAppSettings(newSnapshot.appSettings)
        } catch (error: Throwable) {
            runCatching {
                replaceRoomData(previous.routineSets, previous.routines, previous.dailyLogs)
                appSettingsRepository.replaceAppSettings(previous.appSettings)
            }
            throw error
        }
    }

    private suspend fun replaceRoomData(
        routineSets: List<RoutineSetEntity>,
        routines: List<RoutineEntity>,
        dailyLogs: List<DailyLogEntity>,
    ) = database.withTransaction {
        dao.deleteAllDailyLogs()
        dao.deleteAllRoutines()
        dao.deleteAllRoutineSets()
        dao.insertRoutineSets(routineSets)
        dao.insertRoutines(routines)
        dao.insertDailyLogs(dailyLogs)
    }
}
