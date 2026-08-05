package com.example.steppie.notifications

import com.example.steppie.core.environment.ClockProvider
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.LogStatus
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.repository.AppSettingsRepository
import com.example.steppie.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class RoutineNotificationRescheduler(
    private val routineRepository: RoutineRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val scheduler: RoutineNotificationScheduler,
    private val clockProvider: ClockProvider,
) {
    suspend fun reconcileToday() {
        val date = clockProvider.today()
        val input = combine(
            routineRepository.observeRoutineSetsForDate(date),
            routineRepository.observeDailyLogs(date),
            appSettingsRepository.observeAppSettings(),
        ) { routineSets, logs, settings ->
            NotificationRescheduleInput(
                routines = routineSets.flatMap { it.routines },
                completedRoutineIds = logs
                    .filter { it.status == LogStatus.Completed }
                    .mapTo(mutableSetOf()) { it.routineId },
                settings = settings,
            )
        }.first()

        scheduler.reconcileToday(
            routines = input.routines,
            completedRoutineIds = input.completedRoutineIds,
            settings = input.settings,
            now = clockProvider.now(),
            date = date,
        )
    }
}

private data class NotificationRescheduleInput(
    val routines: List<Routine>,
    val completedRoutineIds: Set<String>,
    val settings: AppSettings,
)
