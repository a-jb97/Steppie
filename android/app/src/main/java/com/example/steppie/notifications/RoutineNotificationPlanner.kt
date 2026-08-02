package com.example.steppie.notifications

import com.example.steppie.core.environment.LocaleProvider
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.Routine
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class RoutineNotificationRequest(
    val routineId: String,
    val routineTitle: String,
    val leadMinutes: Int,
    val triggerAtMillis: Long,
) {
    val requestCode: Int
        get() = stableRequestCode(routineId, leadMinutes)
}

class RoutineNotificationPlanner(
    private val zoneId: ZoneId,
    private val localeProvider: LocaleProvider,
) {
    fun requestsForToday(
        routines: List<Routine>,
        completedRoutineIds: Set<String>,
        settings: AppSettings,
        now: Instant,
        date: LocalDate,
    ): List<RoutineNotificationRequest> {
        val localeTag = settings.locale ?: localeProvider.languageTag()
        return routines
            .filter { it.isActive && it.deletedAt == null && it.id !in completedRoutineIds }
            .flatMap { routine ->
                val scheduledTime = routine.scheduledTime ?: return@flatMap emptyList()
                settings.notificationLeadTimes.mapNotNull { leadMinutes ->
                    val triggerAt = date
                        .atTime(scheduledTime)
                        .atZone(zoneId)
                        .toInstant()
                        .minus(leadMinutes.toLong(), ChronoUnit.MINUTES)
                    if (!triggerAt.isAfter(now)) return@mapNotNull null
                    val localTriggerTime = triggerAt.atZone(zoneId).toLocalTime().truncatedTo(ChronoUnit.MINUTES)
                    if (settings.isQuietTime(localTriggerTime)) return@mapNotNull null
                    RoutineNotificationRequest(
                        routineId = routine.id,
                        routineTitle = routine.title.resolve(settings.locale, localeTag),
                        leadMinutes = leadMinutes,
                        triggerAtMillis = triggerAt.toEpochMilli(),
                    )
                }
            }
            .sortedWith(compareBy<RoutineNotificationRequest> { it.triggerAtMillis }.thenBy { it.routineTitle })
    }
}

fun stableRequestCode(routineId: String, leadMinutes: Int): Int {
    var result = 17
    result = 31 * result + routineId.hashCode()
    result = 31 * result + leadMinutes
    return result and Int.MAX_VALUE
}

private fun AppSettings.isQuietTime(time: LocalTime): Boolean {
    val start = quietHoursStart ?: return false
    val end = quietHoursEnd ?: return false
    if (start == end) return false
    return if (start < end) {
        time >= start && time < end
    } else {
        time >= start || time < end
    }
}
