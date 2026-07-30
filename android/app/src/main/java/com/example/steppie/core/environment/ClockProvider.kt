package com.example.steppie.core.environment

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

interface ClockProvider {
    val zoneId: ZoneId

    fun now(): Instant

    fun today(): LocalDate

    fun currentTime(): LocalTime

    fun currentYearMonth(): YearMonth
}

object SystemClockProvider : ClockProvider {
    private val clock: Clock
        get() = Clock.systemDefaultZone()

    override val zoneId: ZoneId
        get() = clock.zone

    override fun now(): Instant = clock.instant()

    override fun today(): LocalDate = LocalDate.now(clock)

    override fun currentTime(): LocalTime = LocalTime.now(clock)

    override fun currentYearMonth(): YearMonth = YearMonth.now(clock)
}
