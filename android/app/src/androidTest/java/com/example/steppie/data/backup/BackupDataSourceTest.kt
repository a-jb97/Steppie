package com.example.steppie.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.steppie.data.local.SteppieDatabase
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.repository.AppSettingsRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
}

private class FixedAppSettingsRepository(
    settings: AppSettings,
) : AppSettingsRepository {
    private val state = MutableStateFlow(settings)

    override fun observeAppSettings(): Flow<AppSettings> = state

    override suspend fun updateAppSettings(settings: AppSettings) {
        state.value = settings
    }

    override suspend fun setGuardianPin(pin: String): String = unsupported()

    override suspend fun verifyGuardianPin(pin: String): Boolean = unsupported()

    override suspend fun verifyRecoveryCode(recoveryCode: String): Boolean = unsupported()

    override suspend fun changeGuardianPin(currentPin: String, newPin: String): String? = unsupported()

    override suspend fun regenerateRecoveryCode(currentPin: String): String? = unsupported()

    override suspend fun resetGuardianPinWithRecoveryCode(recoveryCode: String, newPin: String): String? = unsupported()

    private fun unsupported(): Nothing = error("PIN operations are not used by BackupDataSourceTest.")
}
