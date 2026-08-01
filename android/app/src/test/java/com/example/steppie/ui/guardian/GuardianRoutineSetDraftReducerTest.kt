package com.example.steppie.ui.guardian

import com.example.steppie.domain.model.IconRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class GuardianRoutineSetDraftReducerTest {
    @Test
    fun `opening routine set editor resets conflicting transient state`() {
        val updated = GuardianRoutineSetDraftReducer.open(populatedState())

        assertEquals(GuardianDestination.RoutineSetCreate, updated.destination)
        assertEquals(listOf(GuardianDestination.Home, GuardianDestination.RoutineEdit), updated.destinationBackStack)
        assertNull(updated.draft)
        assertEquals(RoutineSetDraft(), updated.routineSetDraft)
        assertNull(updated.selectedTemplate)
        assertNull(updated.pendingDeleteRoutineId)
        assertNull(updated.pendingDeleteRoutineSetId)
        assertNull(updated.draftError)
        assertNull(updated.notice)
        assertEquals(24L, updated.interactionToken)
    }

    @Test
    fun `name and step inputs update the single routine set draft owner`() {
        val state = populatedState().copy(routineSetDraft = RoutineSetDraft())

        val named = GuardianRoutineSetDraftReducer.updateName(state, "주말 루틴")
        val titled = GuardianRoutineSetDraftReducer.updateStepTitle(named, "산책하기")
        val icon = GuardianRoutineSetDraftReducer.updateStepBuiltinIcon(titled, "walk")
        val color = GuardianRoutineSetDraftReducer.updateStepColor(icon, "color.card.peach")
        val time = GuardianRoutineSetDraftReducer.updateStepScheduledTime(color, "09시:3a05")

        assertEquals("주말 루틴", time.routineSetDraft?.name)
        assertEquals("산책하기", time.routineSetDraft?.stepDraft?.title)
        assertEquals(IconRef.Builtin("walk"), time.routineSetDraft?.stepDraft?.icon)
        assertEquals("color.card.peach", time.routineSetDraft?.stepDraft?.colorToken)
        assertEquals("09:30", time.routineSetDraft?.stepDraft?.scheduledTime)
        assertNull(time.draftError)
        assertEquals(28L, time.interactionToken)
    }

    @Test
    fun `unsupported icon and color leave state unchanged`() {
        val state = populatedState().copy(routineSetDraft = RoutineSetDraft())

        assertSame(state, GuardianRoutineSetDraftReducer.updateStepBuiltinIcon(state, "unknown"))
        assertSame(state, GuardianRoutineSetDraftReducer.updateStepColor(state, "color.card.unknown"))
    }

    @Test
    fun `photo lifecycle preserves existing token rules`() {
        val state = populatedState().copy(routineSetDraft = RoutineSetDraft())
        val photo = IconRef.Photo("10000000-0000-4000-8000-000000000001")

        val unavailable = GuardianRoutineSetDraftReducer.photoUnavailable(state)
        val imported = GuardianRoutineSetDraftReducer.photoImported(state, photo)
        val failed = GuardianRoutineSetDraftReducer.photoImportFailed(state, "failure")
        val removed = GuardianRoutineSetDraftReducer.removeStepPhoto(imported)

        assertEquals("사진 선택 기능을 사용할 수 없습니다.", unavailable.draftError)
        assertEquals(23L, unavailable.interactionToken)
        assertEquals(photo, imported.routineSetDraft?.stepDraft?.icon)
        assertEquals(24L, imported.interactionToken)
        assertEquals("failure", failed.draftError)
        assertEquals(24L, failed.interactionToken)
        assertEquals(IconRef.Builtin("star"), removed.routineSetDraft?.stepDraft?.icon)
    }

    @Test
    fun `adding a step trims its title and resets the input draft`() {
        val state = populatedState().copy(
            routineSetDraft = RoutineSetDraft(
                name = "아침 루틴",
                stepDraft = RoutineDraft(title = "  양치하기  ", scheduledTime = "08:00"),
            ),
        )

        val updated = GuardianRoutineSetDraftReducer.addOrUpdateStep(state, "양치하기")

        assertEquals(listOf("양치하기"), updated.routineSetDraft?.steps?.map(RoutineDraft::title))
        assertEquals(RoutineDraft(), updated.routineSetDraft?.stepDraft)
        assertNull(updated.routineSetDraft?.editingStepIndex)
        assertEquals(24L, updated.interactionToken)
    }

    @Test
    fun `editing then applying a step replaces it at the same index`() {
        val steps = listOf(
            RoutineDraft(title = "첫 단계"),
            RoutineDraft(title = "둘째 단계"),
        )
        val state = populatedState().copy(routineSetDraft = RoutineSetDraft(steps = steps))

        val editing = GuardianRoutineSetDraftReducer.editStep(state, 1)
        val changed = GuardianRoutineSetDraftReducer.updateStepTitle(editing, "수정 단계")
        val applied = GuardianRoutineSetDraftReducer.addOrUpdateStep(changed, "수정 단계")

        assertEquals(listOf("첫 단계", "수정 단계"), applied.routineSetDraft?.steps?.map(RoutineDraft::title))
        assertEquals(RoutineDraft(), applied.routineSetDraft?.stepDraft)
        assertNull(applied.routineSetDraft?.editingStepIndex)
    }

    @Test
    fun `removing the edited step clears edit input and invalid index is ignored`() {
        val state = populatedState().copy(
            routineSetDraft = RoutineSetDraft(
                steps = listOf(RoutineDraft(title = "첫 단계"), RoutineDraft(title = "둘째 단계")),
                stepDraft = RoutineDraft(title = "둘째 단계"),
                editingStepIndex = 1,
            ),
        )

        val invalid = GuardianRoutineSetDraftReducer.removeStep(state, 3)
        val removed = GuardianRoutineSetDraftReducer.removeStep(state, 1)

        assertSame(state, invalid)
        assertEquals(listOf("첫 단계"), removed.routineSetDraft?.steps?.map(RoutineDraft::title))
        assertEquals(RoutineDraft(), removed.routineSetDraft?.stepDraft)
        assertNull(removed.routineSetDraft?.editingStepIndex)
        assertEquals(24L, removed.interactionToken)
    }

    @Test
    fun `validation error does not increment interaction`() {
        val state = populatedState()

        val updated = GuardianRoutineSetDraftReducer.showError(state, "단계 이름을 입력해 주세요.")

        assertEquals("단계 이름을 입력해 주세요.", updated.draftError)
        assertEquals(23L, updated.interactionToken)
    }

    @Test
    fun `draft input still increments interaction when no routine set draft is open`() {
        val state = populatedState().copy(routineSetDraft = null)

        val updated = GuardianRoutineSetDraftReducer.updateName(state, "ignored")

        assertNull(updated.routineSetDraft)
        assertNull(updated.draftError)
        assertEquals(24L, updated.interactionToken)
    }

    private fun populatedState() = GuardianModeUiState(
        destination = GuardianDestination.RoutineEdit,
        destinationBackStack = listOf(GuardianDestination.Home),
        draft = RoutineDraft(title = "기존 활동"),
        routineSetDraft = RoutineSetDraft(name = "기존 루틴"),
        selectedTemplate = RoutineTemplates.find(RoutineTemplateId.Morning),
        pendingDeleteRoutineId = "routine-id",
        pendingDeleteRoutineSetId = "set-id",
        draftError = "error",
        notice = "notice",
        interactionToken = 23L,
    )
}
