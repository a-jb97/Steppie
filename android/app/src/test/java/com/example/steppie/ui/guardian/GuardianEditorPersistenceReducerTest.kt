package com.example.steppie.ui.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GuardianEditorPersistenceReducerTest {
    @Test
    fun `routine set save failure exposes the supplied validation message`() {
        val updated = GuardianEditorPersistenceReducer.routineSetSaveFailed(
            populatedState(),
            "최소 1개 단계가 있어야 저장할 수 있습니다.",
        )

        assertEquals("최소 1개 단계가 있어야 저장할 수 있습니다.", updated.draftError)
        assertEquals(23L, updated.interactionToken)
    }

    @Test
    fun `routine validation failures preserve their messages without interaction changes`() {
        val state = populatedState()

        val missingSet = GuardianEditorPersistenceReducer.routineSetRequired(state)
        val missingTitle = GuardianEditorPersistenceReducer.routineTitleRequired(state)
        val invalidTime = GuardianEditorPersistenceReducer.scheduledTimeInvalid(state)

        assertEquals("먼저 루틴 세트를 생성해 주세요.", missingSet.draftError)
        assertEquals("활동 이름을 입력해 주세요.", missingTitle.draftError)
        assertEquals("예정 시각은 HH:mm 형식으로 입력해 주세요.", invalidTime.draftError)
        assertEquals(23L, missingSet.interactionToken)
        assertEquals(23L, missingTitle.interactionToken)
        assertEquals(23L, invalidTime.interactionToken)
    }

    @Test
    fun `save success returns to routine edit and clears all editor transients`() {
        val updated = GuardianEditorPersistenceReducer.saveSucceeded(populatedState())

        assertEquals(GuardianDestination.RoutineEdit, updated.destination)
        assertEquals(listOf(GuardianDestination.Home), updated.destinationBackStack)
        assertNull(updated.draft)
        assertNull(updated.routineSetDraft)
        assertNull(updated.selectedTemplate)
        assertEquals(GuardianDestination.RoutineEdit, updated.templateReturnDestination)
        assertNull(updated.draftError)
        assertNull(updated.pendingDeleteRoutineId)
        assertNull(updated.pendingDeleteRoutineSetId)
        assertNull(updated.notice)
        assertEquals(24L, updated.interactionToken)
    }

    private fun populatedState() = GuardianModeUiState(
        destination = GuardianDestination.CardEdit,
        destinationBackStack = listOf(GuardianDestination.Home, GuardianDestination.RoutineEdit),
        draft = RoutineDraft(title = "활동"),
        routineSetDraft = RoutineSetDraft(name = "루틴"),
        selectedTemplate = RoutineTemplates.all.first(),
        templateReturnDestination = GuardianDestination.Home,
        draftError = "error",
        pendingDeleteRoutineId = "routine-id",
        pendingDeleteRoutineSetId = "set-id",
        notice = "notice",
        interactionToken = 23L,
    )
}
