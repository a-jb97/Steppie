package com.example.steppie.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.steppie.data.local.DailyRoutineSelectionEntity
import com.example.steppie.data.local.SteppieDatabase
import com.example.steppie.data.local.toEntity
import com.example.steppie.data.sample.RoutineSampleData
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.repository.AppSettingsRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupDataSourceTest {
    private lateinit var database: SteppieDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SteppieDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun snapshotUsesTheSuppliedExportTimestamp() = runBlocking {
        val exportedAt = Instant.parse("2026-06-17T00:00:00Z")
        val dataSource = BackupDataSource(
            database = database,
            appSettingsRepository = FixedAppSettingsRepository(AppSettings(locale = "ko")),
        )

        val snapshot = dataSource.snapshot(exportedAt)

        assertEquals(exportedAt, snapshot.exportedAt)
        assertEquals("ko", snapshot.appSettings.locale)
    }

    @Test
    fun settingsFailureRestoresPreviousRoomDataAndSettings() = runBlocking {
        val exportedAt = Instant.parse("2026-06-17T00:00:00Z")
        val previousSettings = AppSettings(locale = "ko")
        val settingsRepository = FixedAppSettingsRepository(previousSettings)
        val dataSource = BackupDataSource(database, settingsRepository)
        val previousSet = RoutineSampleData.morning
        database.routineDao().insertRoutineSet(previousSet.toEntity())
        database.routineDao().insertRoutines(previousSet.routines.map { it.toEntity() })
        val previousSelection = DailyRoutineSelectionEntity(
            date = "2026-06-17",
            routineSetId = previousSet.id,
            selectedAtEpochMillis = exportedAt.toEpochMilli(),
        )
        database.routineDao().upsertDailyRoutineSelection(previousSelection)
        val before = dataSource.snapshot(exportedAt)
        val replacementSet = RoutineSampleData.school
        val replacement = BackupSnapshot(
            exportedAt = exportedAt,
            routineSets = listOf(replacementSet.toEntity()),
            routines = replacementSet.routines.map { it.toEntity() },
            dailyLogs = emptyList(),
            appSettings = AppSettings(locale = "en"),
        )
        val failure = IllegalStateException("settings restore failed")
        settingsRepository.failNextUpdateAfterWrite = failure

        val thrown = runCatching { dataSource.replaceAll(replacement) }.exceptionOrNull()

        assertSame(failure, thrown)
        assertEquals(before, dataSource.snapshot(exportedAt))
        assertEquals(
            previousSelection,
            database.routineDao().getDailyRoutineSelection(previousSelection.date),
        )
    }
}

private class FixedAppSettingsRepository(
    settings: AppSettings,
) : AppSettingsRepository {
    private val state = MutableStateFlow(settings)
    var failNextUpdateAfterWrite: Throwable? = null

    override fun observeAppSettings(): Flow<AppSettings> = state

    override suspend fun updateAppSettings(settings: AppSettings) {
        state.value = settings
        failNextUpdateAfterWrite?.let { failure ->
            failNextUpdateAfterWrite = null
            throw failure
        }
    }

    override suspend fun setGuardianPin(pin: String): String = unsupported()

    override suspend fun verifyGuardianPin(pin: String): Boolean = unsupported()

    override suspend fun verifyRecoveryCode(recoveryCode: String): Boolean = unsupported()

    override suspend fun changeGuardianPin(currentPin: String, newPin: String): String? = unsupported()

    override suspend fun regenerateRecoveryCode(currentPin: String): String? = unsupported()

    override suspend fun resetGuardianPinWithRecoveryCode(recoveryCode: String, newPin: String): String? = unsupported()

    private fun unsupported(): Nothing = error("PIN operations are not used by BackupDataSourceTest.")
}
