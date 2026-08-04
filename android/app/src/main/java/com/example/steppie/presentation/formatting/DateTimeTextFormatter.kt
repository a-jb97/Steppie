package com.example.steppie.presentation.formatting

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

internal fun formatLocalizedTime(time: LocalTime, locale: Locale): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(locale)
        .format(time)

internal fun orderedWeekdays(locale: Locale): List<DayOfWeek> {
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    return generateSequence(firstDay) { previous -> previous.plus(1) }
        .take(7)
        .toList()
}

internal fun formatWeekday(date: LocalDate, locale: Locale): String =
    formatWeekday(date.dayOfWeek, locale)

internal fun formatWeekday(dayOfWeek: DayOfWeek, locale: Locale): String =
    dayOfWeek.getDisplayName(TextStyle.SHORT, locale)

internal fun formatRecordDate(date: LocalDate): String = "${date.monthValue}/${date.dayOfMonth}"

internal fun formatFullRecordDate(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))

internal fun formatCalendarMonth(month: YearMonth, locale: Locale): String {
    val pattern = if (locale.language == "ko") "yyyy년 M월" else "MMMM yyyy"
    return month.format(DateTimeFormatter.ofPattern(pattern, locale))
}

internal fun formatCompletedTime(
    completedAt: Instant,
    locale: Locale,
    zoneId: ZoneId,
): String = completedAt.atZone(zoneId).format(
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale),
)

internal fun formatBackupFileName(now: Instant, zoneId: ZoneId): String {
    val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.US)
        .withZone(zoneId)
        .format(now)
    return "steppie-backup-$timestamp.zip"
}
