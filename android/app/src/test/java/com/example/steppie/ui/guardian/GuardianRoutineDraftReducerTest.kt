package com.example.steppie.ui.guardian

import com.example.steppie.domain.model.IconRef
import com.example.steppie.domain.model.LocalizedText
import com.example.steppie.domain.model.Routine
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class GuardianRoutineDraftReducerTest {
    @Test
    fun `opening new editor resets conflicting drafts and transient messages`() {
        val updated = GuardianRoutineDraftReducer.openNew(populatedState())

        assertEquals(GuardianDestination.CardEdit, updated.destination)
        assertEquals(listOf(GuardianDestination.Home, GuardianDestination.RoutineEdit), updated.destinationBackStack)
        assertEquals(RoutineDraft(), updated.draft)
        assertNull(updated.routineSetDraft)
        assertNull(updated.selectedTemplate)
        assertNull(updated.draftError)
        assertNull(updated.notice)
        assertEquals(24L, updated.interactionToken)
    }

    @Test
    fun `opening existing editor copies the resolved routine presentation values`() {
        val routine = routine()

        val updated = GuardianRoutineDraftReducer.openExisting(
            populatedState(),
            routine,
            "양치하기",
        )

        assertEquals(routine.id, updated.draft?.routineId)
        assertEquals("양치하기", updated.draft?.title)
        assertEquals(routine.icon, updated.draft?.icon)
        assertEquals(routine.colorToken, updated.draft?.colorToken)
        assertEquals("08:15", updated.draft?.scheduledTime)
        assertEquals(24L, updated.interactionToken)
    }

    @Test
    fun `title update clears error and preserves the single draft owner`() {
        val state = populatedState().copy(draft = RoutineDraft(title = "이전"))

        val updated = GuardianRoutineDraftReducer.updateTitle(state, "새 활동")

        assertEquals("새 활동", updated.draft?.title)
        assertNull(updated.draftError)
        assertEquals(24L, updated.interactionToken)
    }

    @Test
    fun `valid icon and color update while unsupported values leave state unchanged`() {
        val state = populatedState().copy(draft = RoutineDraft())

        val icon = GuardianRoutineDraftReducer.updateBuiltinIcon(state, "book")
        val invalidIcon = GuardianRoutineDraftReducer.updateBuiltinIcon(icon, "unknown")
        val color = GuardianRoutineDraftReducer.updateColor(icon, "color.card.mint")
        val invalidColor = GuardianRoutineDraftReducer.updateColor(color, "color.card.unknown")

        assertEquals(IconRef.Builtin("book"), icon.draft?.icon)
        assertSame(icon, invalidIcon)
        assertEquals("color.card.mint", color.draft?.colorToken)
        assertSame(color, invalidColor)
    }

    @Test
    fun `scheduled time keeps only supported characters and five positions`() {
        val state = populatedState().copy(draft = RoutineDraft())

        val updated = GuardianRoutineDraftReducer.updateScheduledTime(state, "08시:1a59")

        assertEquals("08:15", updated.draft?.scheduledTime)
        assertEquals(24L, updated.interactionToken)
    }

    @Test
    fun `photo lifecycle preserves existing token rules`() {
        val state = populatedState().copy(draft = RoutineDraft())
        val photo = IconRef.Photo("10000000-0000-4000-8000-000000000001")

        val unavailable = GuardianRoutineDraftReducer.photoUnavailable(state)
        val imported = GuardianRoutineDraftReducer.photoImported(state, photo)
        val failed = GuardianRoutineDraftReducer.photoImportFailed(state, "failure")
        val removed = GuardianRoutineDraftReducer.removePhoto(imported)

        assertEquals("사진 선택 기능을 사용할 수 없습니다.", unavailable.draftError)
        assertEquals(23L, unavailable.interactionToken)
        assertEquals(photo, imported.draft?.icon)
        assertEquals(24L, imported.interactionToken)
        assertEquals("failure", failed.draftError)
        assertEquals(24L, failed.interactionToken)
        assertEquals(IconRef.Builtin("star"), removed.draft?.icon)
    }

    @Test
    fun `draft input still increments interaction when no draft is open`() {
        val state = populatedState().copy(draft = null)

        val updated = GuardianRoutineDraftReducer.updateTitle(state, "ignored")

        assertNull(updated.draft)
        assertNull(updated.draftError)
        assertEquals(24L, updated.interactionToken)
    }

    private fun populatedState() = GuardianModeUiState(
        destination = GuardianDestination.RoutineEdit,
        destinationBackStack = listOf(GuardianDestination.Home),
        draft = RoutineDraft(title = "기존 활동"),
        routineSetDraft = RoutineSetDraft(name = "기존 루틴"),
        selectedTemplate = RoutineTemplates.find(RoutineTemplateId.Morning),
        draftError = "error",
        notice = "notice",
        interactionToken = 23L,
    )

    private fun routine() = Routine(
        id = "10000000-0000-4000-8000-000000000002",
        routineSetId = "10000000-0000-4000-8000-000000000003",
        title = LocalizedText(mapOf("ko" to "양치하기")),
        icon = IconRef.Builtin("brush-teeth"),
        colorToken = "color.card.mint",
        order = 0,
        scheduledTime = LocalTime.of(8, 15),
    )
}
