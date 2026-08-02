package com.example.steppie.domain.model

import java.time.Instant
import java.time.LocalDate

object DailyLogTransition {
    fun complete(
        existing: DailyLog?,
        newLogId: String,
        date: LocalDate,
        routineId: String,
        routineSetId: String,
        completedAt: Instant,
    ): DailyLog = transition(
        existing = existing,
        newLogId = newLogId,
        date = date,
        routineId = routineId,
        routineSetId = routineSetId,
        status = LogStatus.Completed,
        completedAt = completedAt,
        updatedAt = completedAt,
    )

    fun undo(
        existing: DailyLog?,
        newLogId: String,
        date: LocalDate,
        routineId: String,
        routineSetId: String,
        updatedAt: Instant,
    ): DailyLog = transition(
        existing = existing,
        newLogId = newLogId,
        date = date,
        routineId = routineId,
        routineSetId = routineSetId,
        status = LogStatus.Undone,
        completedAt = null,
        updatedAt = updatedAt,
    )

    private fun transition(
        existing: DailyLog?,
        newLogId: String,
        date: LocalDate,
        routineId: String,
        routineSetId: String,
        status: LogStatus,
        completedAt: Instant?,
        updatedAt: Instant,
    ): DailyLog = DailyLog(
        id = existing?.id ?: newLogId,
        date = date,
        routineId = routineId,
        routineSetId = routineSetId,
        status = status,
        completedAt = completedAt,
        createdAt = existing?.createdAt ?: updatedAt,
        updatedAt = updatedAt,
    )
}
