package com.example.steppie.notifications

import com.example.steppie.data.sample.RoutineSampleData
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.repository.AppSettingsRepository
import com.example.steppie.testing.TestClockProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineNotificationReschedulerTest {
    @Test
    fun `reconcile after system restart uses persisted routines logs and settings`() = runTest {
        val now = Instant.parse("2026-01-02T06:30:00Z")
        val date = LocalDate.parse("2026-01-02")
        val repository = RoutineSampleData.inMemoryRepository()
        val completedRoutine = RoutineSampleData.morning.routines.first()
        repository.completeRoutine(completedRoutine.id, date, now)
        val settings = AppSettings(notificationLeadTimes = listOf(5), locale = "ko")
        val scheduler = RecordingNotificationScheduler()
        val rescheduler = RoutineNotificationRescheduler(
            routineRepository = repository,
            appSettingsRepository = TestAppSettingsRepository(settings),
            scheduler = scheduler,
            clockProvider = TestClockProvider(now, ZoneOffset.UTC),
        )

        rescheduler.reconcileToday()

        val input = scheduler.lastInput
        assertEquals(RoutineSampleData.morning.routines.map { it.id }, input?.routines?.map { it.id })
        assertEquals(setOf(completedRoutine.id), input?.completedRoutineIds)
        assertEquals(settings, input?.settings)
        assertEquals(now, input?.now)
        assertEquals(date, input?.date)
    }
}

private class RecordingNotificationScheduler : RoutineNotificationScheduler {
    var lastInput: RecordedNotificationInput? = null

    override fun reconcileToday(
        routines: List<Routine>,
        completedRoutineIds: Set<String>,
        settings: AppSettings,
        now: Instant,
        date: LocalDate,
    ) {
        lastInput = RecordedNotificationInput(routines, completedRoutineIds, settings, now, date)
    }
}

private data class RecordedNotificationInput(
    val routines: List<Routine>,
    val completedRoutineIds: Set<String>,
    val settings: AppSettings,
    val now: Instant,
    val date: LocalDate,
)

private class TestAppSettingsRepository(initial: AppSettings) : AppSettingsRepository {
    private val state = MutableStateFlow(initial)

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

    private fun unsupported(): Nothing = error("PIN operations are not used by RoutineNotificationReschedulerTest.")
}
