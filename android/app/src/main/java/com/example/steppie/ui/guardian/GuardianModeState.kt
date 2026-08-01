package com.example.steppie.ui.guardian

import android.net.Uri
import com.example.steppie.core.environment.SystemClockProvider
import com.example.steppie.data.backup.BackupImportPreview
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.IconRef
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.model.RoutineColorTokens
import com.example.steppie.domain.model.RoutineSet
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

enum class GuardianDestination {
    Pin,
    Home,
    RoutineEdit,
    TemplateSelect,
    CardEdit,
    RoutineSetCreate,
    EnvironmentSettings,
    Records,
    RecordsCalendar,
    Security,
    RecoveryCode,
    BackupRestore,
}

enum class GuardianPinMode { Enter, Setup, SetupConfirm, ChangeCurrent, ChangeNew, RecoveryRegenerateConfirm, RecoveryResetNew }

enum class GuardianRecoveryStep { ShowCode, EnterCodeForPinReset }

enum class GuardianRecoveryError { CodeMismatch }

data class RoutineDraft(
    val routineId: String? = null,
    val title: String = "",
    val icon: IconRef = IconRef.Builtin("star"),
    val colorToken: String = RoutineColorTokens.DEFAULT,
    val scheduledTime: String = "",
) {
    val isNew: Boolean
        get() = routineId == null

    val builtinIconName: String
        get() = (icon as? IconRef.Builtin)?.name ?: "star"
}

data class RoutineSetDraft(
    val name: String = "",
    val stepDraft: RoutineDraft = RoutineDraft(),
    val steps: List<RoutineDraft> = emptyList(),
    val editingStepIndex: Int? = null,
)

data class GuardianRecordDay(
    val date: LocalDate,
    val completedCount: Int,
    val totalCount: Int,
    val hasRecords: Boolean,
) {
    val completionPercent: Int
        get() = if (totalCount == 0) 0 else (completedCount * 100) / totalCount

    val remainingCount: Int
        get() = (totalCount - completedCount).coerceAtLeast(0)
}

data class GuardianRecordRoutine(
    val routineId: String,
    val title: String?,
    val isCompleted: Boolean,
    val completedAt: Instant?,
    val isDeleted: Boolean,
    val isInactive: Boolean,
    val isMissing: Boolean,
)

data class GuardianModeUiState(
    val isActive: Boolean = false,
    val isAuthenticated: Boolean = false,
    val destination: GuardianDestination = GuardianDestination.Pin,
    val destinationBackStack: List<GuardianDestination> = emptyList(),
    val pinMode: GuardianPinMode = GuardianPinMode.Enter,
    val pinDigits: String = "",
    val pinError: String? = null,
    val hasGuardianPin: Boolean = false,
    val appSettings: AppSettings = AppSettings(),
    val routineSets: List<RoutineSet> = emptyList(),
    val activeRoutineSet: RoutineSet? = null,
    val todayRoutineSetId: String? = null,
    val selectedRoutineSetId: String? = null,
    val showDailyRoutineSelectionPrompt: Boolean = false,
    val routines: List<Routine> = emptyList(),
    val draft: RoutineDraft? = null,
    val routineSetDraft: RoutineSetDraft? = null,
    val selectedTemplate: RoutineTemplate? = null,
    val templateReturnDestination: GuardianDestination = GuardianDestination.RoutineEdit,
    val routineSetListEditing: Boolean = false,
    val selectedRecordsDate: LocalDate = SystemClockProvider.today(),
    val recordDays: List<GuardianRecordDay> = emptyList(),
    val selectedRecordRoutines: List<GuardianRecordRoutine> = emptyList(),
    val selectedRecordSummary: GuardianRecordDay = GuardianRecordDay(SystemClockProvider.today(), 0, 0, false),
    val recordsCalendarMonth: YearMonth = SystemClockProvider.currentYearMonth(),
    val selectedCalendarRecordsDate: LocalDate? = null,
    val calendarRecordDates: Set<LocalDate> = emptySet(),
    val selectedCalendarRecordRoutines: List<GuardianRecordRoutine> = emptyList(),
    val selectedCalendarRecordSummary: GuardianRecordDay = GuardianRecordDay(SystemClockProvider.today(), 0, 0, false),
    val editingRoutineSetId: String? = null,
    val editingRoutineSetName: String = "",
    val draftError: String? = null,
    val pendingDeleteRoutineId: String? = null,
    val pendingDeleteRoutineSetId: String? = null,
    val backupInProgress: Boolean = false,
    val backupMessage: String? = null,
    val backupError: String? = null,
    val pendingRestoreUri: Uri? = null,
    val pendingRestorePreview: BackupImportPreview? = null,
    val restorePinDigits: String = "",
    val recoveryStep: GuardianRecoveryStep? = null,
    val recoveryCodeToShow: String? = null,
    val recoveryDigits: String = "",
    val recoveryError: GuardianRecoveryError? = null,
    val recoveryReturnDestination: GuardianDestination = GuardianDestination.Home,
    val notice: String? = null,
    val interactionToken: Long = 0L,
    val restoreCompletedToken: Long = 0L,
)
