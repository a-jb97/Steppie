package com.example.steppie.ui.guardian

import java.time.Instant
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RoutineSetDraftTest {
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun `buildRoutineSetFromDraft creates active set with consecutive steps`() {
        val routineSet = buildRoutineSetFromDraft(
            draft = RoutineSetDraft(
                name = "주말 루틴",
                steps = listOf(
                    RoutineDraft(
                        title = "아침 먹기",
                        iconName = "breakfast",
                        colorToken = "color.card.mint",
                        scheduledTime = "08:30",
                    ),
                    RoutineDraft(
                        title = "산책하기",
                        iconName = "walk",
                        colorToken = "color.card.peach",
                    ),
                ),
            ),
            now = now,
            localeTag = "ko",
        )

        assertEquals("주말 루틴", routineSet.name.values["ko"])
        assertEquals(true, routineSet.isActive)
        assertEquals(listOf(0, 1), routineSet.routines.map { it.order })
        assertEquals(listOf(routineSet.id, routineSet.id), routineSet.routines.map { it.routineSetId })
        assertEquals(LocalTime.of(8, 30), routineSet.routines.first().scheduledTime)
    }

    @Test
    fun `buildRoutineSetFromDraft requires at least one step`() {
        assertThrows(IllegalArgumentException::class.java) {
            buildRoutineSetFromDraft(
                draft = RoutineSetDraft(name = "빈 루틴"),
                now = now,
                localeTag = "ko",
            )
        }
    }

    @Test
    fun `buildRoutineSetFromDraft rejects invalid scheduled time`() {
        assertThrows(IllegalArgumentException::class.java) {
            buildRoutineSetFromDraft(
                draft = RoutineSetDraft(
                    name = "학교 루틴",
                    steps = listOf(RoutineDraft(title = "등교", scheduledTime = "8:00")),
                ),
                now = now,
                localeTag = "ko",
            )
        }
    }
}
