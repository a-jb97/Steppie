package com.example.steppie.ui.tutorial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialTest {
    @Test
    fun `every supported screen has at least one tutorial step`() {
        TutorialScreen.entries.forEach { screen ->
            assertTrue("Missing tutorial steps for $screen", tutorialStepsFor(screen).isNotEmpty())
        }
    }

    @Test
    fun `child screens explain core focus and read only list flows`() {
        assertEquals(4, tutorialStepsFor(TutorialScreen.ChildFocus).size)
        assertEquals(2, tutorialStepsFor(TutorialScreen.ChildList).size)
    }

    @Test
    fun `screen storage keys are unique and versions are positive`() {
        assertEquals(TutorialScreen.entries.size, TutorialScreen.entries.map { it.storageKey }.toSet().size)
        assertTrue(TutorialScreen.entries.all { it.version > 0 })
    }
}
