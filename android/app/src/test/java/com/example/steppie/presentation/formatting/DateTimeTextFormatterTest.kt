package com.example.steppie.presentation.formatting

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class DateTimeTextFormatterTest {
    @Test
    fun `localized date and time text follows the supplied locale`() {
        val date = LocalDate.of(2026, 1, 2)
        val time = LocalTime.of(8, 5)

        assertEquals("January 2, 2026", formatFullRecordDate(date, Locale.US))
        assertEquals("2026년 1월 2일", formatFullRecordDate(date, Locale.KOREA))
        assertEquals("8:05\u202FAM", formatLocalizedTime(time, Locale.US))
        assertEquals("Fri", formatWeekday(date, Locale.US))
        assertEquals("금", formatWeekday(date, Locale.KOREA))
        assertEquals("1/2", formatRecordDate(date))
    }

    @Test
    fun `calendar month keeps the existing Korean and English patterns`() {
        val month = YearMonth.of(2026, 1)

        assertEquals("2026년 1월", formatCalendarMonth(month, Locale.KOREA))
        assertEquals("January 2026", formatCalendarMonth(month, Locale.US))
    }

    @Test
    fun `calendar weekdays begin with the locale first day and cover one week`() {
        val usWeekdays = orderedWeekdays(Locale.US)
        val frenchWeekdays = orderedWeekdays(Locale.FRANCE)

        assertEquals(DayOfWeek.SUNDAY, usWeekdays.first())
        assertEquals(DayOfWeek.MONDAY, frenchWeekdays.first())
        assertEquals(7, usWeekdays.distinct().size)
        assertEquals(7, frenchWeekdays.distinct().size)
    }

    @Test
    fun `completed time applies the supplied zone before formatting`() {
        val completedAt = Instant.parse("2026-01-02T00:00:00Z")

        assertEquals("12:00\u202FAM", formatCompletedTime(completedAt, Locale.US, ZoneOffset.UTC))
        assertEquals("9:00\u202FAM", formatCompletedTime(completedAt, Locale.US, ZoneId.of("Asia/Seoul")))
    }

    @Test
    fun `backup file name keeps the contract pattern in the supplied local zone`() {
        val now = Instant.parse("2026-06-17T00:00:00Z")

        assertEquals("steppie-backup-20260617-000000.zip", formatBackupFileName(now, ZoneOffset.UTC))
        assertEquals(
            "steppie-backup-20260617-090000.zip",
            formatBackupFileName(now, ZoneId.of("Asia/Seoul")),
        )
    }
}
