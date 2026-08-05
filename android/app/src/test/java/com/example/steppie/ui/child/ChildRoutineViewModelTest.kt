package com.example.steppie.ui.child

import androidx.lifecycle.viewModelScope
import com.example.steppie.data.repository.InMemoryRoutineRepository
import com.example.steppie.data.sample.RoutineSampleData
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.DailyLog
import com.example.steppie.domain.model.FeedbackIntensity
import com.example.steppie.domain.model.LogStatus
import com.example.steppie.domain.repository.AppSettingsRepository
import com.example.steppie.domain.repository.RoutineRepository
import com.example.steppie.testing.MainDispatcherRule
import com.example.steppie.testing.TestClockProvider
import com.example.steppie.testing.TestLocaleProvider
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChildRoutineViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `scheduled state exposes only the current routine set to the child screen`() {
        val morning = RoutineSampleData.morning.copy(isActive = true, startTime = null)
        val school = RoutineSampleData.school.copy(isActive = true, startTime = LocalTime.of(9, 0))

        val state = scheduledChildRoutineState(
            routineSets = listOf(morning, school),
            completedRoutineIds = emptySet(),
            selectedRoutineId = null,
            singlePane = ChildSinglePane.Focus,
            now = LocalTime.of(8, 0),
        )

        assertEquals(morning.id, state.currentRoutineSet?.id)
        assertEquals(morning.routines.map { it.id }, state.routines.map { it.id })
        assertEquals(morning.routines.size, state.progressTotal)
        assertEquals(
            (morning.routines + school.routines).map { it.id },
            state.scheduledRoutines.map { it.id },
        )
    }

    @Test
    fun `waiting state shows the next set but keeps completion locked until start time`() {
        val school = RoutineSampleData.school.copy(isActive = true, startTime = LocalTime.of(9, 0))

        val state = scheduledChildRoutineState(
            routineSets = listOf(school),
            completedRoutineIds = emptySet(),
            selectedRoutineId = null,
            singlePane = ChildSinglePane.Focus,
            now = LocalTime.of(8, 0),
        )

        assertEquals(school.routines.map { it.id }, state.routines.map { it.id })
        assertEquals(school.id, state.waitingRoutineSet?.id)
        assertEquals(school.routines.size, state.progressTotal)
        assertEquals(true, state.isRoutineSetLocked)
        assertEquals(null, state.currentRoutine)
        assertEquals(false, state.isSelectedRoutineCompletable)
    }

    @Test
    fun `waiting set becomes completable when its start time arrives`() {
        val school = RoutineSampleData.school.copy(isActive = true, startTime = LocalTime.of(9, 0))

        val state = scheduledChildRoutineState(
            routineSets = listOf(school),
            completedRoutineIds = emptySet(),
            selectedRoutineId = school.routines.first().id,
            singlePane = ChildSinglePane.Focus,
            now = LocalTime.of(9, 0),
        )

        assertEquals(false, state.isRoutineSetLocked)
        assertEquals(school.routines.first().id, state.currentRoutine?.id)
        assertEquals(true, state.isSelectedRoutineCompletable)
    }

    @Test
    fun `schedule waits until the first incomplete set start time`() {
        val morning = RoutineSampleData.morning.copy(
            isActive = true,
            startTime = LocalTime.of(8, 0),
        )

        val resolution = resolveRoutineSchedule(
            routineSets = listOf(morning),
            completedRoutineIds = emptySet(),
            now = LocalTime.of(7, 59),
        )

        assertEquals(null, resolution.currentSet)
        assertEquals(morning.id, resolution.waitingSet?.id)
        assertEquals(false, resolution.allComplete)
    }

    @Test
    fun `schedule starts a set at its exact start time`() {
        val morning = RoutineSampleData.morning.copy(
            isActive = true,
            startTime = LocalTime.of(8, 0),
        )

        val resolution = resolveRoutineSchedule(
            routineSets = listOf(morning),
            completedRoutineIds = emptySet(),
            now = LocalTime.of(8, 0),
        )

        assertEquals(morning.id, resolution.currentSet?.id)
        assertEquals(null, resolution.waitingSet)
    }

    @Test
    fun `schedule keeps an overdue incomplete set instead of interrupting it`() {
        val morning = RoutineSampleData.morning.copy(
            isActive = true,
            startTime = LocalTime.of(8, 0),
        )
        val school = RoutineSampleData.school.copy(
            isActive = true,
            startTime = LocalTime.of(9, 0),
        )

        val resolution = resolveRoutineSchedule(
            routineSets = listOf(morning, school),
            completedRoutineIds = emptySet(),
            now = LocalTime.of(9, 30),
        )

        assertEquals(morning.id, resolution.currentSet?.id)
    }

    @Test
    fun `schedule waits for next set after early completion then advances when due`() {
        val morning = RoutineSampleData.morning.copy(
            isActive = true,
            startTime = null,
        )
        val school = RoutineSampleData.school.copy(
            isActive = true,
            startTime = LocalTime.of(9, 0),
        )
        val morningCompleted = morning.routines.mapTo(mutableSetOf()) { it.id }

        val early = resolveRoutineSchedule(
            routineSets = listOf(morning, school),
            completedRoutineIds = morningCompleted,
            now = LocalTime.of(8, 30),
        )
        val due = resolveRoutineSchedule(
            routineSets = listOf(morning, school),
            completedRoutineIds = morningCompleted,
            now = LocalTime.of(9, 0),
        )

        assertEquals(school.id, early.waitingSet?.id)
        assertEquals(school.id, due.currentSet?.id)
    }

    @Test
    fun `schedule reports all complete only after every active set is done`() {
        val morning = RoutineSampleData.morning.copy(isActive = true)
        val school = RoutineSampleData.school.copy(isActive = true, startTime = LocalTime.of(9, 0))
        val completed = (morning.routines + school.routines).mapTo(mutableSetOf()) { it.id }

        val resolution = resolveRoutineSchedule(
            routineSets = listOf(morning, school),
            completedRoutineIds = completed,
            now = LocalTime.NOON,
        )

        assertEquals(true, resolution.allComplete)
        assertEquals(null, resolution.currentSet)
        assertEquals(null, resolution.waitingSet)
    }
    @Test
    fun `state keeps only active visible routines in order`() {
        val routines = RoutineSampleData.morning.routines
        val deleted = routines[0].copy(
            deletedAt = Instant.parse("2026-01-02T00:00:00Z"),
            isActive = false,
        )
        val inactive = routines[1].copy(isActive = false)

        val state = childRoutineState(
            routines = listOf(routines[3], inactive, routines[2], deleted),
            completedRoutineIds = emptySet(),
            selectedRoutineId = null,
            singlePane = ChildSinglePane.Focus,
        )

        assertEquals(listOf(routines[2].id, routines[3].id), state.routines.map { it.id })
        assertEquals(routines[2].id, state.selectedRoutineId)
    }

    @Test
    fun `state preserves a valid selection and falls back from an invalid selection`() {
        val routines = RoutineSampleData.morning.routines

        val selected = childRoutineState(routines, emptySet(), routines[3].id, ChildSinglePane.List)
        val fallback = childRoutineState(routines, emptySet(), "missing", ChildSinglePane.Focus)

        assertEquals(routines[3].id, selected.selectedRoutineId)
        assertEquals(ChildSinglePane.List, selected.singlePane)
        assertEquals(routines.first().id, fallback.selectedRoutineId)
    }

    @Test
    fun `empty routines produce an empty non-loading state`() {
        val state = childRoutineState(emptyList(), emptySet(), null, ChildSinglePane.Focus)

        assertEquals(emptyList<Any>(), state.routines)
        assertNull(state.selectedRoutineId)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `state derives progress and hides selected routine when all complete`() {
        val routines = RoutineSampleData.morning.routines

        val partial = childRoutineState(
            routines = routines,
            completedRoutineIds = setOf(routines[0].id, routines[1].id),
            selectedRoutineId = routines[1].id,
            singlePane = ChildSinglePane.Focus,
        )
        val complete = childRoutineState(
            routines = routines,
            completedRoutineIds = routines.mapTo(mutableSetOf()) { it.id },
            selectedRoutineId = routines.last().id,
            singlePane = ChildSinglePane.Focus,
        )

        assertEquals(2, partial.progressCount)
        assertEquals(routines.size, partial.progressTotal)
        assertEquals(routines[1].id, partial.selectedRoutineId)
        assertEquals(true, complete.isAllComplete)
        assertNull(complete.selectedRoutine)
    }

    @Test
    fun `feedback routine remains selected until it is explicitly cleared`() {
        val routines = RoutineSampleData.morning.routines

        val feedback = childRoutineState(
            routines = routines,
            completedRoutineIds = setOf(routines[0].id),
            selectedRoutineId = routines[0].id,
            singlePane = ChildSinglePane.Focus,
            feedbackRoutineId = routines[0].id,
            undoRoutineId = routines[0].id,
        )

        assertEquals(routines[0].id, feedback.selectedRoutineId)
        assertEquals(routines[0].id, feedback.feedbackRoutine?.id)
        assertEquals(routines[1].id, feedback.nextIncompleteRoutine?.id)
    }

    @Test
    fun `future selection is not completable until previous routines are done`() {
        val routines = RoutineSampleData.morning.routines

        val futureSelection = childRoutineState(
            routines = routines,
            completedRoutineIds = emptySet(),
            selectedRoutineId = routines[2].id,
            singlePane = ChildSinglePane.Focus,
        )
        val currentSelection = childRoutineState(
            routines = routines,
            completedRoutineIds = setOf(routines[0].id, routines[1].id),
            selectedRoutineId = routines[2].id,
            singlePane = ChildSinglePane.Focus,
        )

        assertEquals(routines[0].id, futureSelection.currentRoutine?.id)
        assertEquals(false, futureSelection.isSelectedRoutineCompletable)
        assertEquals(routines[2].id, currentSelection.currentRoutine?.id)
        assertEquals(true, currentSelection.isSelectedRoutineCompletable)
    }

    @Test
    fun `state keeps feedback intensity for presentation`() {
        val state = childRoutineState(
            routines = RoutineSampleData.morning.routines,
            completedRoutineIds = emptySet(),
            selectedRoutineId = null,
            singlePane = ChildSinglePane.Focus,
            feedbackIntensity = FeedbackIntensity.Strong,
        )

        assertEquals(FeedbackIntensity.Strong, state.feedbackIntensity)
    }

    @Test
    fun `completion enters feedback and persists the selected routine exactly once`() = runTest {
        val target = RoutineSampleData.morning.routines.first()
        val (viewModel, repository) = childViewModel()
        val feedback = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.feedbackEvents.first()
        }

        try {
            assertEquals(target.id, viewModel.uiState.value.selectedRoutineId)
            assertTrue(viewModel.uiState.value.isSelectedRoutineCompletable)

            viewModel.completeSelectedRoutine()
            viewModel.completeSelectedRoutine()
            runCurrent()

            val state = viewModel.uiState.value
            val log = repository.observeDailyLogs(TestDate).first().single()
            val event = feedback.await()

            assertEquals(listOf(target.id), repository.completeCalls.map { it.routineId })
            assertEquals(listOf(TestDate), repository.completeCalls.map { it.date })
            assertEquals(listOf(TestInstant), repository.completeCalls.map { it.at })
            assertTrue(repository.undoCalls.isEmpty())
            assertEquals(LogStatus.Completed, log.status)
            assertEquals(target.id, state.feedbackRoutineId)
            assertEquals(target.id, state.undoRoutineId)
            assertEquals(target.id, state.selectedRoutineId)
            assertTrue(target.id in state.completedRoutineIds)
            assertFalse(state.isSelectedRoutineCompletable)
            assertEquals("일어나기 완료! 잘했어요!", event.spokenText)
            assertTrue(event.vibrate)
            assertTrue(event.sound)
            assertEquals(1.0f, event.ttsRate)
            assertEquals(1.0f, event.ttsVolume)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `undo during feedback restores the routine and persists an undone log`() = runTest {
        val target = RoutineSampleData.morning.routines.first()
        val (viewModel, repository) = childViewModel()

        try {
            viewModel.completeSelectedRoutine()
            runCurrent()
            viewModel.undoLastCompletion()
            runCurrent()

            val state = viewModel.uiState.value
            val log = repository.observeDailyLogs(TestDate).first().single()

            assertEquals(listOf(target.id), repository.completeCalls.map { it.routineId })
            assertEquals(listOf(target.id), repository.undoCalls.map { it.routineId })
            assertEquals(listOf(TestInstant), repository.undoCalls.map { it.at })
            assertEquals(LogStatus.Undone, log.status)
            assertNull(log.completedAt)
            assertEquals(target.id, state.selectedRoutineId)
            assertNull(state.feedbackRoutineId)
            assertNull(state.undoRoutineId)
            assertFalse(target.id in state.completedRoutineIds)
            assertTrue(state.isSelectedRoutineCompletable)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `completion failure rolls back optimistic state and does not emit feedback`() = runTest {
        val target = RoutineSampleData.morning.routines.first()
        val (viewModel, repository) = childViewModel()
        val events = mutableListOf<ChildRoutineFeedbackEvent>()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.feedbackEvents.collect(events::add)
        }
        repository.completeFailure = IllegalStateException("complete failed")

        try {
            viewModel.completeSelectedRoutine()
            runCurrent()

            val state = viewModel.uiState.value
            val logs = repository.observeDailyLogs(TestDate).first()

            assertEquals(listOf(target.id), repository.completeCalls.map { it.routineId })
            assertTrue(logs.isEmpty())
            assertEquals(target.id, state.selectedRoutineId)
            assertNull(state.feedbackRoutineId)
            assertNull(state.undoRoutineId)
            assertFalse(target.id in state.completedRoutineIds)
            assertTrue(state.isSelectedRoutineCompletable)
            assertTrue(events.isEmpty())
        } finally {
            collector.cancel()
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `undo failure restores completed feedback state`() = runTest {
        val target = RoutineSampleData.morning.routines.first()
        val (viewModel, repository) = childViewModel()

        try {
            viewModel.completeSelectedRoutine()
            runCurrent()
            repository.undoFailure = IllegalStateException("undo failed")

            viewModel.undoLastCompletion()
            runCurrent()

            val state = viewModel.uiState.value
            val log = repository.observeDailyLogs(TestDate).first().single()

            assertEquals(listOf(target.id), repository.undoCalls.map { it.routineId })
            assertEquals(LogStatus.Completed, log.status)
            assertEquals(target.id, state.selectedRoutineId)
            assertEquals(target.id, state.feedbackRoutineId)
            assertEquals(target.id, state.undoRoutineId)
            assertTrue(target.id in state.completedRoutineIds)
            assertFalse(state.isSelectedRoutineCompletable)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `advancing feedback selects the next routine without another repository write`() = runTest {
        val routines = RoutineSampleData.morning.routines
        val (viewModel, repository) = childViewModel()

        try {
            viewModel.completeSelectedRoutine()
            runCurrent()
            viewModel.advanceFromFeedback()
            runCurrent()

            val state = viewModel.uiState.value

            assertEquals(listOf(routines.first().id), repository.completeCalls.map { it.routineId })
            assertTrue(repository.undoCalls.isEmpty())
            assertEquals(routines[1].id, state.selectedRoutineId)
            assertEquals(routines[1].id, state.currentRoutine?.id)
            assertNull(state.feedbackRoutineId)
            assertNull(state.undoRoutineId)
            assertTrue(routines.first().id in state.completedRoutineIds)
            assertTrue(state.isSelectedRoutineCompletable)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `completion feedback event follows intensity and device feedback settings`() = runTest {
        val cases = listOf(
            FeedbackCase(
                settings = AppSettings(feedbackIntensity = FeedbackIntensity.Strong, locale = "ko"),
                spokenText = "일어나기 완료! 잘했어요!",
                vibrate = true,
                sound = true,
            ),
            FeedbackCase(
                settings = AppSettings(feedbackIntensity = FeedbackIntensity.Normal, locale = "ko"),
                spokenText = "일어나기 완료! 잘했어요!",
                vibrate = true,
                sound = true,
            ),
            FeedbackCase(
                settings = AppSettings(feedbackIntensity = FeedbackIntensity.Quiet, locale = "ko"),
                spokenText = "일어나기 완료! 잘했어요!",
                vibrate = false,
                sound = false,
            ),
            FeedbackCase(
                settings = AppSettings(feedbackIntensity = FeedbackIntensity.Off, locale = "ko"),
                spokenText = "일어나기 완료! 잘했어요!",
                vibrate = false,
                sound = false,
            ),
            FeedbackCase(
                settings = AppSettings(
                    feedbackIntensity = FeedbackIntensity.Strong,
                    soundEnabled = false,
                    ttsEnabled = false,
                    ttsRate = 0.75,
                    ttsVolume = 0.4,
                    hapticEnabled = false,
                    locale = "ko",
                ),
                spokenText = null,
                vibrate = false,
                sound = false,
            ),
        )

        cases.forEach { case ->
            val (viewModel) = childViewModel(case.settings)
            val feedback = async(start = CoroutineStart.UNDISPATCHED) {
                viewModel.feedbackEvents.first()
            }

            try {
                viewModel.completeSelectedRoutine()
                runCurrent()

                val event = feedback.await()
                assertEquals(case.spokenText, event.spokenText)
                assertEquals(case.vibrate, event.vibrate)
                assertEquals(case.sound, event.sound)
                assertEquals(case.settings.ttsRate.toFloat(), event.ttsRate)
                assertEquals(case.settings.ttsVolume.toFloat(), event.ttsVolume)
            } finally {
                viewModel.viewModelScope.cancel()
            }
        }
    }

    private fun childViewModel(
        settings: AppSettings = AppSettings(locale = "ko"),
    ): Pair<ChildRoutineViewModel, RecordingRoutineRepository> {
        val source = InMemoryRoutineRepository(
            initialData = listOf(RoutineSampleData.morning.copy(isActive = true, startTime = null)),
        )
        val repository = RecordingRoutineRepository(source)
        return ChildRoutineViewModel(
            repository = repository,
            appSettingsRepository = FakeAppSettingsRepository(settings),
            clockProvider = TestClockProvider(TestInstant, ZoneOffset.UTC),
            localeProvider = TestLocaleProvider("ko"),
        ) to repository
    }
}

private val TestInstant = Instant.parse("2026-01-02T08:00:00Z")
private val TestDate = LocalDate.parse("2026-01-02")

private data class FeedbackCase(
    val settings: AppSettings,
    val spokenText: String?,
    val vibrate: Boolean,
    val sound: Boolean,
)

private data class RoutineWrite(
    val routineId: String,
    val date: LocalDate,
    val at: Instant,
)

private class RecordingRoutineRepository(
    private val delegate: RoutineRepository,
) : RoutineRepository by delegate {
    val completeCalls = mutableListOf<RoutineWrite>()
    val undoCalls = mutableListOf<RoutineWrite>()
    var completeFailure: Throwable? = null
    var undoFailure: Throwable? = null

    override suspend fun completeRoutine(
        routineId: String,
        date: LocalDate,
        completedAt: Instant,
    ): DailyLog {
        completeCalls += RoutineWrite(routineId, date, completedAt)
        completeFailure?.let { throw it }
        return delegate.completeRoutine(routineId, date, completedAt)
    }

    override suspend fun undoRoutine(
        routineId: String,
        date: LocalDate,
        updatedAt: Instant,
    ): DailyLog {
        undoCalls += RoutineWrite(routineId, date, updatedAt)
        undoFailure?.let { throw it }
        return delegate.undoRoutine(routineId, date, updatedAt)
    }
}

private class FakeAppSettingsRepository(
    initialSettings: AppSettings,
) : AppSettingsRepository {
    private val settings = MutableStateFlow(initialSettings)

    override fun observeAppSettings(): Flow<AppSettings> = settings

    override suspend fun updateAppSettings(settings: AppSettings) {
        this.settings.value = settings
    }

    override suspend fun setGuardianPin(pin: String): String = error("Not used in child tests")

    override suspend fun verifyGuardianPin(pin: String): Boolean = error("Not used in child tests")

    override suspend fun verifyRecoveryCode(recoveryCode: String): Boolean = error("Not used in child tests")

    override suspend fun changeGuardianPin(currentPin: String, newPin: String): String? =
        error("Not used in child tests")

    override suspend fun regenerateRecoveryCode(currentPin: String): String? = error("Not used in child tests")

    override suspend fun resetGuardianPinWithRecoveryCode(recoveryCode: String, newPin: String): String? =
        error("Not used in child tests")
}
