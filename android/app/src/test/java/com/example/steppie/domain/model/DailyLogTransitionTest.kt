package com.example.steppie.domain.model

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailyLogTransitionTest {
    private val date = LocalDate.parse("2026-01-02")
    private val routineId = "40000000-0000-4000-8000-000000000001"
    private val routineSetId = "30000000-0000-4000-8000-000000000001"
    private val logId = "50000000-0000-4000-8000-000000000001"

    @Test
    fun `first completion creates a completed log with one timestamp`() {
        val completedAt = Instant.parse("2026-01-02T08:00:00Z")

        val completed = DailyLogTransition.complete(
            existing = null,
            newLogId = logId,
            date = date,
            routineId = routineId,
            routineSetId = routineSetId,
            completedAt = completedAt,
        )

        assertEquals(logId, completed.id)
        assertEquals(LogStatus.Completed, completed.status)
        assertEquals(completedAt, completed.completedAt)
        assertEquals(completedAt, completed.createdAt)
        assertEquals(completedAt, completed.updatedAt)
    }

    @Test
    fun `undo preserves identity and creation time while clearing completion`() {
        val existing = completedLog()
        val updatedAt = Instant.parse("2026-01-02T08:01:00Z")

        val undone = DailyLogTransition.undo(
            existing = existing,
            newLogId = "50000000-0000-4000-8000-000000000002",
            date = date,
            routineId = routineId,
            routineSetId = routineSetId,
            updatedAt = updatedAt,
        )

        assertEquals(existing.id, undone.id)
        assertEquals(existing.createdAt, undone.createdAt)
        assertEquals(LogStatus.Undone, undone.status)
        assertNull(undone.completedAt)
        assertEquals(updatedAt, undone.updatedAt)
    }

    @Test
    fun `recompletion preserves the original identity and creation time`() {
        val undone = DailyLogTransition.undo(
            existing = completedLog(),
            newLogId = logId,
            date = date,
            routineId = routineId,
            routineSetId = routineSetId,
            updatedAt = Instant.parse("2026-01-02T08:01:00Z"),
        )
        val recompletedAt = Instant.parse("2026-01-02T08:02:00Z")

        val recompleted = DailyLogTransition.complete(
            existing = undone,
            newLogId = "50000000-0000-4000-8000-000000000003",
            date = date,
            routineId = routineId,
            routineSetId = routineSetId,
            completedAt = recompletedAt,
        )

        assertEquals(logId, recompleted.id)
        assertEquals(completedLog().createdAt, recompleted.createdAt)
        assertEquals(LogStatus.Completed, recompleted.status)
        assertEquals(recompletedAt, recompleted.completedAt)
        assertEquals(recompletedAt, recompleted.updatedAt)
    }

    private fun completedLog(): DailyLog = DailyLog(
        id = logId,
        date = date,
        routineId = routineId,
        routineSetId = routineSetId,
        status = LogStatus.Completed,
        completedAt = Instant.parse("2026-01-02T08:00:00Z"),
        createdAt = Instant.parse("2026-01-02T08:00:00Z"),
        updatedAt = Instant.parse("2026-01-02T08:00:00Z"),
    )
}
