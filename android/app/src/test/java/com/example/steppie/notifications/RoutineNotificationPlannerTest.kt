package com.example.steppie.notifications

import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.IconRef
import com.example.steppie.domain.model.LocalizedText
import com.example.steppie.domain.model.Routine
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineNotificationPlannerTest {
    private val date = LocalDate.parse("2026-01-02")
    private val zone = ZoneId.of("UTC")
    private val planner = RoutineNotificationPlanner(zoneId = zone)

    @Test
    fun `routines without scheduled time do not create notifications`() {
        val requests = planner.requestsForToday(
            routines = listOf(routine(scheduledTime = null)),
            completedRoutineIds = emptySet(),
            settings = AppSettings(),
            now = Instant.parse("2026-01-02T07:00:00Z"),
            date = date,
        )

        assertTrue(requests.isEmpty())
    }

    @Test
    fun `lead times in the past are skipped`() {
        val requests = planner.requestsForToday(
            routines = listOf(routine(scheduledTime = LocalTime.of(8, 0))),
            completedRoutineIds = emptySet(),
            settings = AppSettings(notificationLeadTimes = listOf(10, 5)),
            now = Instant.parse("2026-01-02T07:53:00Z"),
            date = date,
        )

        assertEquals(listOf(5), requests.map { it.leadMinutes })
    }

    @Test
    fun `completed routines are skipped and undone routines can be scheduled again`() {
        val target = routine(scheduledTime = LocalTime.of(8, 0))
        val settings = AppSettings(notificationLeadTimes = listOf(10))
        val now = Instant.parse("2026-01-02T07:40:00Z")

        val completed = planner.requestsForToday(
            routines = listOf(target),
            completedRoutineIds = setOf(target.id),
            settings = settings,
            now = now,
            date = date,
        )
        val undone = planner.requestsForToday(
            routines = listOf(target),
            completedRoutineIds = emptySet(),
            settings = settings,
            now = now,
            date = date,
        )

        assertTrue(completed.isEmpty())
        assertEquals(listOf(10), undone.map { it.leadMinutes })
    }

    @Test
    fun `empty lead times disable notification requests`() {
        val requests = planner.requestsForToday(
            routines = listOf(routine(scheduledTime = LocalTime.of(8, 0))),
            completedRoutineIds = emptySet(),
            settings = AppSettings(notificationLeadTimes = emptyList()),
            now = Instant.parse("2026-01-02T07:00:00Z"),
            date = date,
        )

        assertTrue(requests.isEmpty())
    }

    @Test
    fun `quiet hours suppress matching notification times`() {
        val requests = planner.requestsForToday(
            routines = listOf(routine(scheduledTime = LocalTime.of(8, 0))),
            completedRoutineIds = emptySet(),
            settings = AppSettings(
                notificationLeadTimes = listOf(10, 5),
                quietHoursStart = LocalTime.of(7, 45),
                quietHoursEnd = LocalTime.of(7, 55),
            ),
            now = Instant.parse("2026-01-02T07:00:00Z"),
            date = date,
        )

        assertEquals(listOf(5), requests.map { it.leadMinutes })
    }

    private fun routine(scheduledTime: LocalTime?): Routine = Routine(
        id = "50000000-0000-4000-8000-000000000001",
        routineSetId = "60000000-0000-4000-8000-000000000001",
        title = LocalizedText(mapOf("ko" to "양치하기", "en" to "Brush teeth")),
        icon = IconRef.Builtin("brush-teeth"),
        colorToken = "color.card.sky",
        order = 0,
        scheduledTime = scheduledTime,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
}
