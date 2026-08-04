package com.example.steppie.domain

import com.example.steppie.domain.model.IconRef
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.LocalizedText
import com.example.steppie.domain.model.Routine
import java.time.Instant
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RoutineModelsTest {
    @Test
    fun localizedText_usesContractFallbackOrder() {
        val text = LocalizedText(linkedMapOf("en" to "Brush teeth", "ko" to "양치하기"))

        assertEquals("Brush teeth", text.resolve("en-US", "ja-JP"))
        assertEquals("양치하기", text.resolve("ja-JP", "fr-FR"))
    }

    @Test
    fun routine_rejectsNonV4UuidAndUnknownColorToken() {
        assertThrows(IllegalArgumentException::class.java) {
            routine(id = "00000000-0000-1000-8000-000000000001")
        }
        assertThrows(IllegalArgumentException::class.java) {
            routine(colorToken = "#D8ECFF")
        }
    }

    @Test
    fun scheduledTime_isRepresentedAs24HourLocalTime() {
        val routine = routine(scheduledTime = LocalTime.parse("08:05"))

        assertEquals("08:05", routine.scheduledTime.toString())
        assertThrows(java.time.format.DateTimeParseException::class.java) {
            LocalTime.parse("8:05")
        }
        assertThrows(IllegalArgumentException::class.java) {
            routine(scheduledTime = LocalTime.of(8, 5, 1))
        }
    }

    @Test
    fun appSettings_allowsDisablingAllNotificationLeadTimes() {
        val settings = AppSettings(notificationLeadTimes = emptyList())

        assertEquals(emptyList<Int>(), settings.notificationLeadTimes)
        assertThrows(IllegalArgumentException::class.java) {
            AppSettings(notificationLeadTimes = listOf(10, 10))
        }
        assertThrows(IllegalArgumentException::class.java) {
            AppSettings(notificationLeadTimes = listOf(0))
        }
    }

    private fun routine(
        id: String = "50000000-0000-4000-8000-000000000001",
        colorToken: String = "color.card.sky",
        scheduledTime: LocalTime? = null,
    ): Routine = Routine(
        id = id,
        routineSetId = "50000000-0000-4000-8000-000000000002",
        title = LocalizedText(mapOf("ko" to "양치하기")),
        icon = IconRef.Builtin("brush-teeth"),
        colorToken = colorToken,
        order = 0,
        scheduledTime = scheduledTime,
        createdAt = Instant.parse("2026-01-02T00:00:00Z"),
    )
}
