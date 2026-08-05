package com.example.steppie.data.backup

import androidx.room.withTransaction
import com.example.steppie.data.local.DailyLogEntity
import com.example.steppie.data.local.RoutineDao
import com.example.steppie.data.local.RoutineEntity
import com.example.steppie.data.local.RoutineSetEntity
import com.example.steppie.data.local.SteppieDatabase
import com.example.steppie.data.photo.RoutinePhotoStore
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.repository.AppSettingsRepository
import java.time.Instant
import kotlinx.coroutines.flow.first

class BackupDataSource(
    private val database: SteppieDatabase,
    private val appSettingsRepository: AppSettingsRepository,
    private val routinePhotoStore: RoutinePhotoStore? = null,
    private val dao: RoutineDao = database.routineDao(),
) {
    suspend fun snapshot(exportedAt: Instant): BackupSnapshot {
        val current = currentData()
        return BackupSnapshot(
            exportedAt = exportedAt,
            routineSets = current.routineSets,
            routines = current.routines,
            dailyLogs = current.dailyLogs,
            appSettings = current.appSettings,
        )
    }

    fun photoBackupAssets(snapshot: BackupSnapshot): Map<String, ByteArray> {
        val store = routinePhotoStore ?: return emptyMap()
        val photos = snapshot.routines
            .filter { it.iconType == "photo" }
            .mapNotNull { entity ->
                val localAssetId = entity.localAssetId ?: return@mapNotNull null
                com.example.steppie.domain.model.IconRef.Photo(localAssetId, entity.backupAssetName)
            }
        return store.readBackupAssets(photos)
    }

    suspend fun replaceAll(newSnapshot: BackupSnapshot, assets: Map<String, ByteArray> = emptyMap()) {
        val previous = currentData()
        val steps = mutableListOf(
            BackupRestoreStep(
                name = "room",
                apply = {
                    replaceRoomData(
                        routineSets = newSnapshot.routineSets,
                        routines = newSnapshot.routines,
                        dailyLogs = newSnapshot.dailyLogs,
                    )
                },
                rollback = {
                    replaceRoomData(previous.routineSets, previous.routines, previous.dailyLogs)
                },
            ),
            BackupRestoreStep(
                name = "settings",
                apply = { appSettingsRepository.updateAppSettings(newSnapshot.appSettings) },
                rollback = { appSettingsRepository.updateAppSettings(previous.appSettings) },
            ),
        )
        routinePhotoStore?.let { store ->
            val previousAssets = store.snapshotFiles()
            steps += BackupRestoreStep(
                name = "photos",
                apply = { store.replaceAllFromBackup(assets) },
                rollback = { store.replaceAllFiles(previousAssets) },
            )
        }
        runBackupRestoreTransaction(steps)
    }

    private suspend fun replaceRoomData(
        routineSets: List<RoutineSetEntity>,
        routines: List<RoutineEntity>,
        dailyLogs: List<DailyLogEntity>,
    ) = database.withTransaction {
        dao.deleteAllDailyRoutineSelections()
        dao.deleteAllDailyLogs()
        dao.deleteAllRoutines()
        dao.deleteAllRoutineSets()
        dao.insertRoutineSets(routineSets)
        dao.insertRoutines(routines)
        dao.insertDailyLogs(dailyLogs)
    }

    private suspend fun currentData(): RestorableBackupData = RestorableBackupData(
        routineSets = dao.getAllRoutineSetEntities(),
        routines = dao.getAllRoutineEntities(),
        dailyLogs = dao.getAllDailyLogEntities(),
        appSettings = appSettingsRepository.observeAppSettings().first(),
    )
}

private data class RestorableBackupData(
    val routineSets: List<RoutineSetEntity>,
    val routines: List<RoutineEntity>,
    val dailyLogs: List<DailyLogEntity>,
    val appSettings: AppSettings,
)
