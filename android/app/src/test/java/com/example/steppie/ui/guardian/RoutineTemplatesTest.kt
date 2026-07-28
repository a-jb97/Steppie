package com.example.steppie.ui.guardian

import com.example.steppie.domain.model.IconRef
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineTemplatesTest {
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun `templates provide Korean and English localized labels`() {
        RoutineTemplates.all.forEach { template ->
            assertTrue(template.name.values.containsKey("ko"))
            assertTrue(template.name.values.containsKey("en"))
            template.steps.forEach { step ->
                assertTrue(step.title.values.containsKey("ko"))
                assertTrue(step.title.values.containsKey("en"))
            }
        }
    }

    @Test
    fun `instantiating template creates new active routine set with consecutive routine order`() {
        val routineSet = requireNotNull(RoutineTemplates.find(RoutineTemplateId.Morning)).instantiate(now)

        assertEquals(false, routineSet.isActive)
        assertEquals(listOf(0, 1, 2, 3, 4, 5), routineSet.routines.map { it.order })
        assertEquals(List(routineSet.routines.size) { routineSet.id }, routineSet.routines.map { it.routineSetId })
        assertEquals(routineSet.routines.size, routineSet.routines.map { it.id }.distinct().size)
    }

    @Test
    fun `instantiating same template twice does not reuse ids`() {
        val template = requireNotNull(RoutineTemplates.find(RoutineTemplateId.Bedtime))
        val first = template.instantiate(now)
        val second = template.instantiate(now)

        assertNotEquals(first.id, second.id)
        first.routines.zip(second.routines).forEach { (firstRoutine, secondRoutine) ->
            assertNotEquals(firstRoutine.id, secondRoutine.id)
        }
    }

    @Test
    fun `school template uses bus icon for going to school`() {
        val school = requireNotNull(RoutineTemplates.find(RoutineTemplateId.School))
        val firstStep = school.instantiate(now).routines.first()

        assertEquals("routine.goSchool", firstStep.titleKey)
        assertEquals(IconRef.Builtin("bus"), firstStep.icon)
    }
}
