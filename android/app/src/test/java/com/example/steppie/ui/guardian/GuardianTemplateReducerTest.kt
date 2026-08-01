package com.example.steppie.ui.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GuardianTemplateReducerTest {
    private val morning = requireNotNull(RoutineTemplates.find(RoutineTemplateId.Morning))
    private val bedtime = requireNotNull(RoutineTemplates.find(RoutineTemplateId.Bedtime))

    @Test
    fun `opening template selection clears drafts and records return destination`() {
        val updated = GuardianTemplateReducer.open(
            populatedState(),
            GuardianDestination.Home,
            morning,
        )

        assertEquals(GuardianDestination.TemplateSelect, updated.destination)
        assertEquals(listOf(GuardianDestination.Home, GuardianDestination.RoutineEdit), updated.destinationBackStack)
        assertEquals(GuardianDestination.Home, updated.templateReturnDestination)
        assertEquals(morning, updated.selectedTemplate)
        assertNull(updated.draft)
        assertNull(updated.routineSetDraft)
        assertNull(updated.draftError)
        assertNull(updated.notice)
        assertEquals(24L, updated.interactionToken)
    }

    @Test
    fun `closing template selection returns through the stored destination`() {
        val state = populatedState().copy(
            destination = GuardianDestination.TemplateSelect,
            destinationBackStack = listOf(GuardianDestination.Home, GuardianDestination.RoutineEdit),
            templateReturnDestination = GuardianDestination.RoutineEdit,
            selectedTemplate = morning,
        )

        val updated = GuardianTemplateReducer.close(state, GuardianDestination.RoutineEdit)

        assertEquals(GuardianDestination.RoutineEdit, updated.destination)
        assertEquals(listOf(GuardianDestination.Home), updated.destinationBackStack)
        assertNull(updated.selectedTemplate)
        assertNull(updated.draftError)
        assertNull(updated.notice)
        assertEquals(24L, updated.interactionToken)
    }

    @Test
    fun `preview replaces selection and clears conflicting drafts`() {
        val updated = GuardianTemplateReducer.preview(populatedState(), bedtime)

        assertEquals(GuardianDestination.TemplateSelect, updated.destination)
        assertEquals(bedtime, updated.selectedTemplate)
        assertNull(updated.draft)
        assertNull(updated.routineSetDraft)
        assertNull(updated.draftError)
        assertNull(updated.notice)
        assertEquals(24L, updated.interactionToken)
    }

    @Test
    fun `successful save returns to routine edit and exposes notice`() {
        val state = populatedState().copy(
            destination = GuardianDestination.TemplateSelect,
            destinationBackStack = listOf(GuardianDestination.Home, GuardianDestination.RoutineEdit),
            selectedTemplate = morning,
        )

        val updated = GuardianTemplateReducer.saveSucceeded(state)

        assertEquals(GuardianDestination.RoutineEdit, updated.destination)
        assertEquals(listOf(GuardianDestination.Home), updated.destinationBackStack)
        assertNull(updated.selectedTemplate)
        assertNull(updated.draft)
        assertNull(updated.routineSetDraft)
        assertNull(updated.draftError)
        assertEquals("템플릿으로 새 루틴 세트를 저장했습니다.", updated.notice)
        assertEquals(24L, updated.interactionToken)
    }

    @Test
    fun `failed save keeps selection and exposes error`() {
        val state = populatedState().copy(selectedTemplate = morning)

        val updated = GuardianTemplateReducer.saveFailed(state, "failure")

        assertEquals(state.destination, updated.destination)
        assertEquals(morning, updated.selectedTemplate)
        assertEquals("failure", updated.draftError)
        assertEquals(state.notice, updated.notice)
        assertEquals(24L, updated.interactionToken)
    }

    private fun populatedState() = GuardianModeUiState(
        destination = GuardianDestination.RoutineEdit,
        destinationBackStack = listOf(GuardianDestination.Home),
        draft = RoutineDraft(title = "기존 활동"),
        routineSetDraft = RoutineSetDraft(name = "기존 루틴"),
        selectedTemplate = morning,
        draftError = "error",
        notice = "notice",
        interactionToken = 23L,
    )
}
