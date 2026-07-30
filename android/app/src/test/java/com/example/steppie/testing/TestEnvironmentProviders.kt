package com.example.steppie.testing

import com.example.steppie.core.environment.ClockProvider
import com.example.steppie.core.environment.LocaleProvider
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

class TestClockProvider(
    var instant: Instant,
    override val zoneId: ZoneId,
) : ClockProvider {
    override fun now(): Instant = instant

    override fun today(): LocalDate = instant.atZone(zoneId).toLocalDate()

    override fun currentTime(): LocalTime = instant.atZone(zoneId).toLocalTime()

    override fun currentYearMonth(): YearMonth = YearMonth.from(today())
}

class TestLocaleProvider(
    private val tag: String,
) : LocaleProvider {
    override fun languageTag(): String = tag
}
