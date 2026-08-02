package com.example.steppie.domain.model

import java.time.Instant
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineCollectionRulesTest {
    @Test
    fun `visible ordering retains inactive routines but excludes soft deleted routines`() {
        val active = routine(idSuffix = 1, order = 2)
        val inactive = routine(idSuffix = 2, order = 1, isActive = false)
        val deleted = routine(idSuffix = 3, order = 0, deletedAt = Instant.parse("2026-06-18T00:00:00Z"))

        assertEquals(listOf(inactive, active), listOf(active, deleted, inactive).visibleRoutinesInOrder())
        assertEquals(listOf(active), listOf(active, deleted, inactive).childRoutinesInOrder())
        assertTrue(active.isAvailableToChild)
        assertFalse(inactive.isAvailableToChild)
        assertFalse(deleted.isVisible)
    }

    @Test
    fun `child routine sets place unscheduled first then sort scheduled start times`() {
        val unscheduled = routineSet(idSuffix = 1, startTime = null)
        val eight = routineSet(idSuffix = 2, startTime = LocalTime.of(8, 0))
        val seven = routineSet(idSuffix = 3, startTime = LocalTime.of(7, 0))
        val inactive = routineSet(idSuffix = 4, startTime = LocalTime.of(6, 0), isActive = false)
        val deleted = routineSet(
            idSuffix = 5,
            startTime = LocalTime.of(5, 0),
            isActive = false,
            deletedAt = Instant.parse("2026-06-18T00:00:00Z"),
        )

        assertEquals(
            listOf(unscheduled, seven, eight),
            listOf(eight, inactive, deleted, unscheduled, seven).childRoutineSetsInScheduleOrder(),
        )
    }

    private fun routine(
        idSuffix: Int,
        order: Int,
        isActive: Boolean = true,
        deletedAt: Instant? = null,
    ): Routine = Routine(
        id = uuid(idSuffix),
        routineSetId = uuid(100),
        title = LocalizedText(mapOf("ko" to "루틴 $idSuffix")),
        order = order,
        isActive = isActive,
        createdAt = Instant.parse("2026-06-17T00:00:00Z"),
        deletedAt = deletedAt,
    )

    private fun routineSet(
        idSuffix: Int,
        startTime: LocalTime?,
        isActive: Boolean = true,
        deletedAt: Instant? = null,
    ): RoutineSet = RoutineSet(
        id = uuid(idSuffix),
        name = LocalizedText(mapOf("ko" to "세트 $idSuffix")),
        isActive = isActive,
        startTime = startTime,
        createdAt = Instant.parse("2026-06-17T00:00:00Z").plusSeconds(idSuffix.toLong()),
        deletedAt = deletedAt,
    )

    private fun uuid(suffix: Int): String = "10000000-0000-4000-8000-${suffix.toString().padStart(12, '0')}"
}
