package com.example.steppie.ui.guardian

import com.example.steppie.data.sample.RoutineSampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardianRoutineSetReducerTest {
    @Test
    fun `list editing toggle clears transient edit state`() {
        val updated = GuardianRoutineSetReducer.toggleListEditing(populatedState())

        assertFalse(updated.routineSetListEditing)
        assertNull(updated.editingRoutineSetId)
        assertEquals("", updated.editingRoutineSetName)
        assertNull(updated.pendingDeleteRoutineSetId)
        assertNull(updated.draftError)
        assertEquals(24L, updated.interactionToken)
    }

    @Test
    fun `selection exposes the selected routine set with ordered routines`() {
        val routineSet = RoutineSampleData.morning.copy(
            routines = RoutineSampleData.morning.routines.reversed(),
        )

        val updated = GuardianRoutineSetReducer.select(populatedState(), routineSet)

        assertEquals(routineSet, updated.activeRoutineSet)
        assertEquals(routineSet.id, updated.selectedRoutineSetId)
        assertEquals(routineSet.routines.sortedBy { it.order }, updated.routines)
        assertNull(updated.draftError)
        assertEquals(24L, updated.interactionToken)
    }

    @Test
    fun `daily participation validation results preserve their notices`() {
        val state = populatedState()

        assertEquals(
            "최소 한 개의 루틴 세트는 매일 진행해야 합니다.",
            GuardianRoutineSetReducer.lastDailyRoutineSetRejected(state).notice,
        )
        assertEquals(
            "추가할 루틴 세트의 시작 시간을 먼저 설정해 주세요.",
            GuardianRoutineSetReducer.missingStartTimeRejected(state).notice,
        )
        assertEquals(
            "다른 루틴 세트와 시작 시간이 같아요.",
            GuardianRoutineSetReducer.duplicateDailyStartTimeRejected(state).notice,
        )
    }

    @Test
    fun `daily participation result selects the set and dismisses the prompt`() {
        val included = GuardianRoutineSetReducer.dailyParticipationChanged(
            populatedState(),
            routineSetId = "set-id",
            wasActive = false,
        )
        val excluded = GuardianRoutineSetReducer.dailyParticipationChanged(
            populatedState(),
            routineSetId = "set-id",
            wasActive = true,
        )

        assertEquals("set-id", included.selectedRoutineSetId)
        assertFalse(included.showDailyRoutineSelectionPrompt)
        assertEquals("매일 진행에 추가했습니다.", included.notice)
        assertEquals("매일 진행에서 제외했습니다.", excluded.notice)
        assertEquals(24L, included.interactionToken)
    }

    @Test
    fun `start time validation and save results preserve messages`() {
        val invalid = GuardianRoutineSetReducer.invalidStartTime(populatedState())
        val duplicate = GuardianRoutineSetReducer.duplicateStartTime(populatedState())
        val saved = GuardianRoutineSetReducer.startTimeSaved(populatedState())

        assertEquals("시작 시각은 HH:mm 형식으로 입력해 주세요.", invalid.draftError)
        assertEquals("다른 루틴 세트와 시작 시간이 같아요.", duplicate.draftError)
        assertEquals(23L, invalid.interactionToken)
        assertEquals(23L, duplicate.interactionToken)
        assertNull(saved.draftError)
        assertEquals("루틴 세트 시작 시간을 저장했습니다.", saved.notice)
        assertEquals(24L, saved.interactionToken)
    }

    @Test
    fun `daily selection prompt dismissal only changes prompt state`() {
        val updated = GuardianRoutineSetReducer.dismissDailySelectionPrompt(populatedState())

        assertFalse(updated.showDailyRoutineSelectionPrompt)
        assertEquals("notice", updated.notice)
        assertEquals(24L, updated.interactionToken)
    }

    @Test
    fun `name editing starts with the resolved name and accepts updates`() {
        val started = GuardianRoutineSetReducer.beginNameEdit(
            populatedState(),
            routineSetId = "set-id",
            resolvedName = "아침 준비",
        )
        val updated = GuardianRoutineSetReducer.updateEditingName(started, "저녁 준비")

        assertEquals("set-id", started.editingRoutineSetId)
        assertEquals("아침 준비", started.editingRoutineSetName)
        assertNull(started.draftError)
        assertEquals("저녁 준비", updated.editingRoutineSetName)
        assertEquals(25L, updated.interactionToken)
    }

    @Test
    fun `name edit cancellation clears edit fields`() {
        val updated = GuardianRoutineSetReducer.cancelNameEdit(populatedState())

        assertNull(updated.editingRoutineSetId)
        assertEquals("", updated.editingRoutineSetName)
        assertNull(updated.draftError)
        assertEquals(24L, updated.interactionToken)
    }

    @Test
    fun `blank name rejection preserves editing fields`() {
        val updated = GuardianRoutineSetReducer.blankNameRejected(populatedState())

        assertEquals("set-id", updated.editingRoutineSetId)
        assertEquals("이름", updated.editingRoutineSetName)
        assertEquals("루틴 세트 이름을 입력해 주세요.", updated.draftError)
        assertEquals(23L, updated.interactionToken)
    }

    private fun populatedState() = GuardianModeUiState(
        routineSetListEditing = true,
        selectedRoutineSetId = "previous-set-id",
        showDailyRoutineSelectionPrompt = true,
        editingRoutineSetId = "set-id",
        editingRoutineSetName = "이름",
        pendingDeleteRoutineSetId = "set-id",
        draftError = "error",
        notice = "notice",
        interactionToken = 23L,
    )
}
