package com.example.steppie.ui.guardian

import androidx.lifecycle.viewModelScope
import com.example.steppie.data.repository.InMemoryRoutineRepository
import com.example.steppie.data.sample.RoutineSampleData
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.FeedbackIntensity
import com.example.steppie.domain.model.IconRef
import com.example.steppie.domain.repository.AppSettingsRepository
import com.example.steppie.testing.MainDispatcherRule
import com.example.steppie.testing.TestClockProvider
import com.example.steppie.testing.TestLocaleProvider
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GuardianModeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `pin setup rejects a mismatched confirmation then shows the recovery code on success`() = runTest {
        val fixture = guardianFixture()
        val viewModel = fixture.viewModel

        try {
            viewModel.openInitialSetup()
            inputPin(viewModel, "1234")
            runCurrent()

            assertEquals(GuardianPinMode.SetupConfirm, viewModel.uiState.value.pinMode)
            assertEquals("", viewModel.uiState.value.pinDigits)

            inputPin(viewModel, "1111")
            runCurrent()

            assertEquals(GuardianPinMode.Setup, viewModel.uiState.value.pinMode)
            assertEquals("PIN이 일치하지 않아요. 처음부터 다시 입력해 주세요.", viewModel.uiState.value.pinError)
            assertTrue(fixture.settingsRepository.setPinCalls.isEmpty())

            inputPin(viewModel, "1234")
            inputPin(viewModel, "1234")
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(listOf("1234"), fixture.settingsRepository.setPinCalls)
            assertTrue(state.isActive)
            assertTrue(state.isAuthenticated)
            assertTrue(state.hasGuardianPin)
            assertEquals(GuardianDestination.Home, state.destination)
            assertEquals(GuardianRecoveryStep.ShowCode, state.recoveryStep)
            assertEquals(TestRecoveryCode, state.recoveryCodeToShow)
            assertEquals(GuardianDestination.Home, state.recoveryReturnDestination)
            assertEquals("", state.pinDigits)
            assertNull(state.pinError)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `existing pin keeps guardian locked after failure and opens home after success`() = runTest {
        val fixture = guardianFixture(
            settings = configuredSettings(),
            currentPin = "2468",
        )
        val viewModel = fixture.viewModel

        try {
            viewModel.openFromChild()
            assertEquals(GuardianPinMode.Enter, viewModel.uiState.value.pinMode)

            inputPin(viewModel, "1111")
            runCurrent()

            assertFalse(viewModel.uiState.value.isAuthenticated)
            assertEquals(GuardianDestination.Pin, viewModel.uiState.value.destination)
            assertEquals("PIN이 맞지 않아요. 다시 입력해 주세요.", viewModel.uiState.value.pinError)
            assertEquals("", viewModel.uiState.value.pinDigits)

            inputPin(viewModel, "2468")
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(listOf("1111", "2468"), fixture.settingsRepository.verifyPinCalls)
            assertTrue(state.isAuthenticated)
            assertEquals(GuardianDestination.Home, state.destination)
            assertTrue(state.destinationBackStack.isEmpty())
            assertNull(state.pinError)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `closing guardian resets session state while preserving observed routine data`() = runTest {
        val fixture = authenticatedGuardianFixture()
        val viewModel = fixture.viewModel

        try {
            val routineSets = viewModel.uiState.value.routineSets
            val selectedRoutineSetId = viewModel.uiState.value.selectedRoutineSetId
            viewModel.openRoutineEdit()
            viewModel.openNewRoutineEditor()
            viewModel.updateDraftTitle("임시 활동")

            viewModel.closeToChild()

            val state = viewModel.uiState.value
            assertFalse(state.isActive)
            assertFalse(state.isAuthenticated)
            assertEquals(GuardianDestination.Pin, state.destination)
            assertNull(state.draft)
            assertEquals(routineSets, state.routineSets)
            assertEquals(selectedRoutineSetId, state.selectedRoutineSetId)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `closing setup recovery code ends guardian session with pin configured`() = runTest {
        val fixture = guardianFixture()
        val viewModel = fixture.viewModel

        try {
            viewModel.openInitialSetup()
            inputPin(viewModel, "1234")
            inputPin(viewModel, "1234")
            runCurrent()

            assertEquals(GuardianRecoveryStep.ShowCode, viewModel.uiState.value.recoveryStep)

            viewModel.closeRecoveryCode()

            val state = viewModel.uiState.value
            assertFalse(state.isActive)
            assertFalse(state.isAuthenticated)
            assertTrue(state.hasGuardianPin)
            assertEquals(GuardianDestination.Pin, state.destination)
            assertNull(state.recoveryStep)
            assertNull(state.recoveryCodeToShow)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `guardian navigation returns through its back stack and clears transient drafts`() = runTest {
        val fixture = authenticatedGuardianFixture()
        val viewModel = fixture.viewModel

        try {
            viewModel.openRoutineEdit()
            viewModel.openNewRoutineEditor()
            viewModel.updateDraftTitle("새 활동")

            assertEquals(GuardianDestination.CardEdit, viewModel.uiState.value.destination)
            assertEquals(
                listOf(GuardianDestination.Home, GuardianDestination.RoutineEdit),
                viewModel.uiState.value.destinationBackStack,
            )
            assertEquals("새 활동", viewModel.uiState.value.draft?.title)

            viewModel.navigateBack()

            assertEquals(GuardianDestination.RoutineEdit, viewModel.uiState.value.destination)
            assertEquals(listOf(GuardianDestination.Home), viewModel.uiState.value.destinationBackStack)
            assertNull(viewModel.uiState.value.draft)

            viewModel.navigateBack()

            assertEquals(GuardianDestination.Home, viewModel.uiState.value.destination)
            assertTrue(viewModel.uiState.value.destinationBackStack.isEmpty())
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `new routine validation keeps the editor open and valid input persists then returns`() = runTest {
        val fixture = authenticatedGuardianFixture()
        val viewModel = fixture.viewModel

        try {
            val initialRoutineIds = viewModel.uiState.value.routines.map { it.id }.toSet()
            viewModel.openRoutineEdit()
            viewModel.openNewRoutineEditor()
            viewModel.saveDraft()

            assertEquals(GuardianDestination.CardEdit, viewModel.uiState.value.destination)
            assertEquals("활동 이름을 입력해 주세요.", viewModel.uiState.value.draftError)

            viewModel.updateDraftTitle("  새 활동  ")
            viewModel.updateDraftScheduledTime("08:15")
            viewModel.updateDraftIcon("book")
            viewModel.saveDraft()
            runCurrent()

            val savedSet = fixture.routineRepository.observeRoutineSets().first().single()
            val savedRoutine = savedSet.routines.single { it.id !in initialRoutineIds }
            val state = viewModel.uiState.value

            assertEquals("새 활동", savedRoutine.title.values.values.single())
            assertEquals(mapOf("ko" to "새 활동"), savedRoutine.title.values)
            assertEquals(TestInstant, savedRoutine.createdAt)
            assertEquals(TestInstant, savedRoutine.updatedAt)
            assertEquals("08:15", savedRoutine.scheduledTime.toString())
            assertEquals("book", (savedRoutine.icon as IconRef.Builtin).name)
            assertEquals(GuardianDestination.RoutineEdit, state.destination)
            assertNull(state.draft)
            assertNull(state.draftError)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `existing routine save preserves identity and ordering while updating editable fields`() = runTest {
        val fixture = authenticatedGuardianFixture()
        val viewModel = fixture.viewModel

        try {
            val existing = viewModel.uiState.value.routines.first()
            viewModel.openRoutineEdit()
            viewModel.openRoutineEditor(existing.id)
            viewModel.updateDraftTitle("  수정한 활동  ")
            viewModel.updateDraftScheduledTime("09:20")

            viewModel.saveDraft()
            runCurrent()

            val saved = requireNotNull(fixture.routineRepository.getRoutine(existing.id))
            assertEquals(existing.id, saved.id)
            assertEquals(existing.createdAt, saved.createdAt)
            assertEquals(existing.order, saved.order)
            assertEquals(mapOf("ko" to "수정한 활동"), saved.title.values)
            assertEquals("09:20", saved.scheduledTime.toString())
            assertEquals(TestInstant, saved.updatedAt)
            assertEquals(GuardianDestination.RoutineEdit, viewModel.uiState.value.destination)
            assertNull(viewModel.uiState.value.draft)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `routine set save persists its steps then clears the editor`() = runTest {
        val fixture = authenticatedGuardianFixture()
        val viewModel = fixture.viewModel

        try {
            viewModel.openRoutineEdit()
            viewModel.openRoutineSetCreate()
            viewModel.updateRoutineSetName("  외출 준비  ")
            viewModel.updateRoutineSetStepTitle("  가방 챙기기  ")
            viewModel.updateRoutineSetStepScheduledTime("10:30")
            viewModel.addRoutineSetStep()

            viewModel.saveRoutineSetDraft()
            runCurrent()

            val saved = fixture.routineRepository.observeRoutineSets().first()
                .single { it.name.values["ko"] == "외출 준비" }
            assertFalse(saved.isActive)
            assertEquals(1, saved.routines.size)
            assertEquals("가방 챙기기", saved.routines.single().title.values["ko"])
            assertEquals("10:30", saved.routines.single().scheduledTime.toString())
            assertEquals(GuardianDestination.RoutineEdit, viewModel.uiState.value.destination)
            assertNull(viewModel.uiState.value.routineSetDraft)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `routine move persists the requested adjacent ordering and ignores its boundary`() = runTest {
        val fixture = authenticatedGuardianFixture()
        val viewModel = fixture.viewModel

        try {
            val initialIds = viewModel.uiState.value.routines.map { it.id }
            assertTrue(initialIds.size >= 2)

            viewModel.moveRoutine(initialIds.first(), direction = -1)
            runCurrent()
            assertEquals(
                initialIds,
                fixture.routineRepository.observeRoutineSets().first().single().routines.map { it.id },
            )

            viewModel.moveRoutine(initialIds.first(), direction = 1)
            runCurrent()

            val reorderedIds = fixture.routineRepository.observeRoutineSets().first().single().routines.map { it.id }
            assertEquals(initialIds[1], reorderedIds[0])
            assertEquals(initialIds[0], reorderedIds[1])
            assertEquals(initialIds.drop(2), reorderedIds.drop(2))
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `template save invokes callback before success state and persists routine set`() = runTest {
        val fixture = authenticatedGuardianFixture()
        val viewModel = fixture.viewModel

        try {
            val callbackDestinations = mutableListOf<GuardianDestination>()
            viewModel.openRoutineEdit()
            viewModel.openTemplateSelect()

            viewModel.saveTemplatePreview {
                callbackDestinations += viewModel.uiState.value.destination
            }
            runCurrent()

            val routineSets = fixture.routineRepository.observeRoutineSets().first()
            val state = viewModel.uiState.value

            assertEquals(listOf(GuardianDestination.TemplateSelect), callbackDestinations)
            assertEquals(2, routineSets.size)
            assertEquals(1, routineSets.count { it.isActive })
            assertEquals(GuardianDestination.RoutineEdit, state.destination)
            assertNull(state.selectedTemplate)
            assertEquals("템플릿으로 새 루틴 세트를 저장했습니다.", state.notice)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `only active routine set cannot be removed from daily participation`() = runTest {
        val fixture = authenticatedGuardianFixture()
        val viewModel = fixture.viewModel

        try {
            val routineSet = viewModel.uiState.value.routineSets.single()

            viewModel.setRoutineSetForToday(routineSet.id)

            val persisted = fixture.routineRepository.observeRoutineSets().first().single()
            assertTrue(persisted.isActive)
            assertEquals("최소 한 개의 루틴 세트는 매일 진행해야 합니다.", viewModel.uiState.value.notice)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `routine set start time validates input then persists a valid time`() = runTest {
        val fixture = authenticatedGuardianFixture()
        val viewModel = fixture.viewModel

        try {
            val routineSetId = viewModel.uiState.value.routineSets.single().id

            viewModel.updateRoutineSetStartTime(routineSetId, "8시")

            assertEquals("시작 시각은 HH:mm 형식으로 입력해 주세요.", viewModel.uiState.value.draftError)
            assertNull(fixture.routineRepository.observeRoutineSets().first().single().startTime)

            viewModel.updateRoutineSetStartTime(routineSetId, "08:15")
            runCurrent()

            assertEquals("08:15", fixture.routineRepository.observeRoutineSets().first().single().startTime.toString())
            assertNull(viewModel.uiState.value.draftError)
            assertEquals("루틴 세트 시작 시간을 저장했습니다.", viewModel.uiState.value.notice)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `routine set name edit resolves locale and persists the trimmed name`() = runTest {
        val fixture = authenticatedGuardianFixture()
        val viewModel = fixture.viewModel

        try {
            val routineSet = viewModel.uiState.value.routineSets.single()

            viewModel.requestEditRoutineSetName(routineSet.id)
            assertEquals(routineSet.name.resolve(null, "ko"), viewModel.uiState.value.editingRoutineSetName)

            viewModel.updateEditingRoutineSetName("  새 루틴 이름  ")
            viewModel.saveEditingRoutineSetName()
            runCurrent()

            val persisted = fixture.routineRepository.observeRoutineSets().first().single()
            assertEquals(mapOf("ko" to "새 루틴 이름"), persisted.name.values)
            assertNull(viewModel.uiState.value.editingRoutineSetId)
            assertEquals("", viewModel.uiState.value.editingRoutineSetName)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `routine deletion clears the request after repository deletion`() = runTest {
        val fixture = authenticatedGuardianFixture()
        val viewModel = fixture.viewModel

        try {
            viewModel.openRoutineEdit()
            val routineId = requireNotNull(viewModel.uiState.value.routines.firstOrNull()).id

            viewModel.requestDelete(routineId)
            assertEquals(routineId, viewModel.uiState.value.pendingDeleteRoutineId)

            viewModel.confirmDelete()
            runCurrent()

            val routines = fixture.routineRepository.observeRoutineSets().first().single().routines
            assertTrue(routines.none { it.id == routineId })
            assertNull(viewModel.uiState.value.pendingDeleteRoutineId)
            assertEquals(GuardianDestination.RoutineEdit, viewModel.uiState.value.destination)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `last routine set deletion is rejected without repository mutation`() = runTest {
        val fixture = authenticatedGuardianFixture()
        val viewModel = fixture.viewModel

        try {
            val routineSetId = viewModel.uiState.value.routineSets.single().id
            viewModel.requestDeleteRoutineSet(routineSetId)
            viewModel.confirmDeleteRoutineSet()

            assertEquals(1, fixture.routineRepository.observeRoutineSets().first().size)
            assertNull(viewModel.uiState.value.pendingDeleteRoutineSetId)
            assertEquals("마지막 루틴 세트는 삭제할 수 없습니다.", viewModel.uiState.value.notice)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `deleting the only active routine set activates a replacement first`() = runTest {
        val fixture = authenticatedGuardianFixture()
        val viewModel = fixture.viewModel

        try {
            val activeId = viewModel.uiState.value.routineSets.single().id
            val replacement = requireNotNull(RoutineTemplates.find(RoutineTemplateId.Bedtime))
                .instantiate(TestInstant)
            fixture.routineRepository.createRoutineSet(replacement)
            runCurrent()

            viewModel.requestDeleteRoutineSet(activeId)
            viewModel.confirmDeleteRoutineSet()
            runCurrent()

            val remaining = fixture.routineRepository.observeRoutineSets().first().single()
            assertEquals(replacement.id, remaining.id)
            assertTrue(remaining.isActive)
            assertNull(viewModel.uiState.value.pendingDeleteRoutineSetId)
            assertTrue(viewModel.uiState.value.routineSetListEditing)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `environment changes update optimistic state and persist each resulting settings snapshot`() = runTest {
        val fixture = authenticatedGuardianFixture()
        val viewModel = fixture.viewModel

        try {
            viewModel.openEnvironmentSettings()
            viewModel.updateFeedbackIntensity(FeedbackIntensity.Strong)
            viewModel.updateTtsEnabled(false)
            viewModel.updateNotificationLeadTime(10, enabled = false)
            viewModel.updateQuietHoursEnabled(true)
            viewModel.updateQuietHoursStart("22:30")
            runCurrent()

            val state = viewModel.uiState.value
            val persisted = fixture.settingsRepository.updateCalls.last()

            assertEquals(GuardianDestination.EnvironmentSettings, state.destination)
            assertEquals(FeedbackIntensity.Strong, state.appSettings.feedbackIntensity)
            assertFalse(state.appSettings.ttsEnabled)
            assertEquals(listOf(5), state.appSettings.notificationLeadTimes)
            assertEquals("22:30", state.appSettings.quietHoursStart.toString())
            assertEquals("07:00", state.appSettings.quietHoursEnd.toString())
            assertEquals(5, fixture.settingsRepository.updateCalls.size)
            assertEquals(state.appSettings, persisted)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    private suspend fun authenticatedGuardianFixture(): GuardianFixture {
        val fixture = guardianFixture(
            settings = configuredSettings(),
            currentPin = "2468",
        )
        fixture.viewModel.openFromChild()
        inputPin(fixture.viewModel, "2468")
        return fixture
    }

    private fun guardianFixture(
        settings: AppSettings = AppSettings(),
        currentPin: String? = null,
    ): GuardianFixture {
        val routineRepository = InMemoryRoutineRepository(
            initialData = listOf(RoutineSampleData.morning.copy(isActive = true, startTime = null)),
        )
        val settingsRepository = FakeGuardianSettingsRepository(settings, currentPin)
        return GuardianFixture(
            viewModel = GuardianModeViewModel(
                routineRepository = routineRepository,
                appSettingsRepository = settingsRepository,
                clockProvider = TestClockProvider(TestInstant, ZoneOffset.UTC),
                localeProvider = TestLocaleProvider("ko"),
            ),
            routineRepository = routineRepository,
            settingsRepository = settingsRepository,
        )
    }

    private fun inputPin(viewModel: GuardianModeViewModel, pin: String) {
        pin.forEach { digit -> viewModel.inputPinDigit(digit.digitToInt()) }
    }
}

private val TestInstant = Instant.parse("2026-01-02T08:00:00Z")

private data class GuardianFixture(
    val viewModel: GuardianModeViewModel,
    val routineRepository: InMemoryRoutineRepository,
    val settingsRepository: FakeGuardianSettingsRepository,
)

private class FakeGuardianSettingsRepository(
    initialSettings: AppSettings,
    currentPin: String?,
) : AppSettingsRepository {
    private val settings = MutableStateFlow(initialSettings)
    private var currentPin = currentPin
    val setPinCalls = mutableListOf<String>()
    val verifyPinCalls = mutableListOf<String>()
    val updateCalls = mutableListOf<AppSettings>()

    override fun observeAppSettings(): Flow<AppSettings> = settings

    override suspend fun updateAppSettings(settings: AppSettings) {
        updateCalls += settings
        this.settings.value = settings
    }

    override suspend fun setGuardianPin(pin: String): String {
        setPinCalls += pin
        currentPin = pin
        settings.value = settings.value.copy(
            guardianPinHash = "configured-pin",
            recoveryCodeHash = "configured-recovery",
        )
        return TestRecoveryCode
    }

    override suspend fun verifyGuardianPin(pin: String): Boolean {
        verifyPinCalls += pin
        return pin == currentPin
    }

    override suspend fun verifyRecoveryCode(recoveryCode: String): Boolean = recoveryCode == TestRecoveryCode

    override suspend fun changeGuardianPin(currentPin: String, newPin: String): String? {
        if (currentPin != this.currentPin) return null
        this.currentPin = newPin
        return TestRecoveryCode
    }

    override suspend fun regenerateRecoveryCode(currentPin: String): String? =
        TestRecoveryCode.takeIf { currentPin == this.currentPin }

    override suspend fun resetGuardianPinWithRecoveryCode(recoveryCode: String, newPin: String): String? {
        if (recoveryCode != TestRecoveryCode) return null
        currentPin = newPin
        return TestRecoveryCode
    }
}

private fun configuredSettings(): AppSettings = AppSettings(
    guardianPinHash = "configured-pin",
    recoveryCodeHash = "configured-recovery",
)

private const val TestRecoveryCode = "654321"
