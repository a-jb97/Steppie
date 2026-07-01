package com.example.steppie.ui.guardian

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick as semanticOnClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.steppie.R
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.BuiltinIconNames
import com.example.steppie.domain.model.FeedbackIntensity
import com.example.steppie.domain.model.IconRef
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.model.RoutineColorTokens
import com.example.steppie.domain.model.RoutineSet
import com.example.steppie.ui.child.RoutineIcon
import com.example.steppie.ui.components.SteppieButton
import com.example.steppie.ui.components.SteppieButtonState
import com.example.steppie.ui.components.SteppieButtonStyle
import com.example.steppie.ui.theme.SteppieCornerRadius
import com.example.steppie.ui.theme.SteppieLayout
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.theme.SteppieStroke
import com.example.steppie.ui.theme.SteppieTheme
import java.util.Locale
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

private val GuardianSplitMinimumWidth = 905.dp

@Composable
fun GuardianModeScreen(
    state: GuardianModeUiState,
    onDigit: (Int) -> Unit,
    onDeletePinDigit: () -> Unit,
    onCloseToChild: () -> Unit,
    onInteraction: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenRoutineEdit: () -> Unit,
    onOpenEnvironmentSettings: () -> Unit = {},
    onOpenRecords: () -> Unit = {},
    onOpenSecurity: () -> Unit,
    onOpenBackupRestore: () -> Unit = {},
    onOpenPinChange: () -> Unit,
    onOpenRoutineSetCreate: () -> Unit,
    onOpenTemplateSelect: () -> Unit,
    onOpenTemplateSelectFromHome: () -> Unit,
    onCloseTemplateSelect: () -> Unit,
    onPreviewTemplate: (RoutineTemplateId) -> Unit,
    onSaveTemplatePreview: () -> Unit,
    onOpenNewRoutineEditor: () -> Unit,
    onOpenRoutineEditor: (String) -> Unit,
    onToggleRoutineSetListEditing: () -> Unit,
    onSelectRoutineSet: (String) -> Unit,
    onRequestEditRoutineSetName: (String) -> Unit,
    onEditingRoutineSetNameChange: (String) -> Unit,
    onCancelEditRoutineSetName: () -> Unit,
    onSaveEditingRoutineSetName: () -> Unit,
    onDraftTitleChange: (String) -> Unit,
    onDraftIconChange: (String) -> Unit,
    onDraftColorChange: (String) -> Unit,
    onDraftScheduledTimeChange: (String) -> Unit,
    onSaveDraft: () -> Unit,
    onRoutineSetNameChange: (String) -> Unit,
    onRoutineSetStepTitleChange: (String) -> Unit,
    onRoutineSetStepIconChange: (String) -> Unit,
    onRoutineSetStepColorChange: (String) -> Unit,
    onRoutineSetStepScheduledTimeChange: (String) -> Unit,
    onAddRoutineSetStep: () -> Unit,
    onEditRoutineSetStep: (Int) -> Unit,
    onRemoveRoutineSetStep: (Int) -> Unit,
    onSaveRoutineSetDraft: () -> Unit,
    onRequestDelete: (String) -> Unit,
    onRequestDeleteRoutineSet: (String) -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onConfirmDeleteRoutineSet: () -> Unit,
    onMoveRoutine: (String, Int) -> Unit,
    onSelectRecordsDate: (LocalDate) -> Unit = {},
    onShowOutOfScopeNotice: () -> Unit,
    onFeedbackIntensityChange: (FeedbackIntensity) -> Unit = {},
    onTtsEnabledChange: (Boolean) -> Unit = {},
    onTtsRateChange: (Double) -> Unit = {},
    onTtsVolumeChange: (Double) -> Unit = {},
    onSoundEnabledChange: (Boolean) -> Unit = {},
    onHapticEnabledChange: (Boolean) -> Unit = {},
    onNotificationLeadTimeChange: (Int, Boolean) -> Unit = { _, _ -> },
    onQuietHoursEnabledChange: (Boolean) -> Unit = {},
    onQuietHoursStartChange: (String) -> Unit = {},
    onQuietHoursEndChange: (String) -> Unit = {},
    onCreateBackupFile: () -> Unit = {},
    onOpenRestoreFile: () -> Unit = {},
    onRestorePinDigit: (Int) -> Unit = {},
    onDeleteRestorePinDigit: () -> Unit = {},
    onCancelRestore: () -> Unit = {},
    onClearNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .pointerInput(state.isActive, state.isAuthenticated) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        onInteraction()
                    }
                }
            },
    ) {
        when (state.destination) {
            GuardianDestination.Pin -> GuardianPinScreen(
                state = state,
                onDigit = onDigit,
                onDeletePinDigit = onDeletePinDigit,
            )
            GuardianDestination.Home -> GuardianHomeScreen(
                onCloseToChild = onCloseToChild,
                onOpenRoutineSetCreate = onOpenRoutineSetCreate,
                onOpenRoutineEdit = onOpenRoutineEdit,
                onOpenTemplateSelect = onOpenTemplateSelectFromHome,
                onOpenEnvironmentSettings = onOpenEnvironmentSettings,
                onOpenRecords = onOpenRecords,
                onOpenSecurity = onOpenSecurity,
                onShowOutOfScopeNotice = onShowOutOfScopeNotice,
            )
            GuardianDestination.RoutineEdit -> {
                if (maxWidth >= GuardianSplitMinimumWidth && maxWidth > maxHeight) {
                    GuardianRoutineSplitScreen(
                        state = state,
                        onOpenHome = onOpenHome,
                        onOpenRoutineSetCreate = onOpenRoutineSetCreate,
                        onOpenTemplateSelect = onOpenTemplateSelect,
                        onOpenNewRoutineEditor = onOpenNewRoutineEditor,
                        onOpenRoutineEditor = onOpenRoutineEditor,
                        onToggleRoutineSetListEditing = onToggleRoutineSetListEditing,
                        onSelectRoutineSet = onSelectRoutineSet,
                        onRequestEditRoutineSetName = onRequestEditRoutineSetName,
                        onRequestDeleteRoutineSet = onRequestDeleteRoutineSet,
                        onRequestDelete = onRequestDelete,
                        onMoveRoutine = onMoveRoutine,
                        onShowOutOfScopeNotice = onShowOutOfScopeNotice,
                    )
                } else {
                    GuardianRoutineEditScreen(
                        state = state,
                        onOpenHome = onOpenHome,
                        onOpenRoutineSetCreate = onOpenRoutineSetCreate,
                        onOpenTemplateSelect = onOpenTemplateSelect,
                        onOpenNewRoutineEditor = onOpenNewRoutineEditor,
                        onOpenRoutineEditor = onOpenRoutineEditor,
                        onToggleRoutineSetListEditing = onToggleRoutineSetListEditing,
                        onSelectRoutineSet = onSelectRoutineSet,
                        onRequestEditRoutineSetName = onRequestEditRoutineSetName,
                        onRequestDeleteRoutineSet = onRequestDeleteRoutineSet,
                        onRequestDelete = onRequestDelete,
                        onMoveRoutine = onMoveRoutine,
                        onShowOutOfScopeNotice = onShowOutOfScopeNotice,
                    )
                }
            }
            GuardianDestination.TemplateSelect -> GuardianTemplateSelectScreen(
                state = state,
                onBack = onCloseTemplateSelect,
                onPreviewTemplate = onPreviewTemplate,
                onSaveTemplatePreview = onSaveTemplatePreview,
                useWideLayout = maxWidth >= GuardianSplitMinimumWidth && maxWidth > maxHeight,
            )
            GuardianDestination.CardEdit -> GuardianCardEditScreen(
                state = state,
                onOpenRoutineEdit = onOpenRoutineEdit,
                onRequestDelete = onRequestDelete,
                onDraftTitleChange = onDraftTitleChange,
                onDraftIconChange = onDraftIconChange,
                onDraftColorChange = onDraftColorChange,
                onDraftScheduledTimeChange = onDraftScheduledTimeChange,
                onSaveDraft = onSaveDraft,
            )
            GuardianDestination.RoutineSetCreate -> GuardianRoutineSetCreateScreen(
                state = state,
                useSplitLayout = maxWidth >= GuardianSplitMinimumWidth && maxWidth > maxHeight,
                onOpenHome = onOpenHome,
                onRoutineSetNameChange = onRoutineSetNameChange,
                onRoutineSetStepTitleChange = onRoutineSetStepTitleChange,
                onRoutineSetStepIconChange = onRoutineSetStepIconChange,
                onRoutineSetStepColorChange = onRoutineSetStepColorChange,
                onRoutineSetStepScheduledTimeChange = onRoutineSetStepScheduledTimeChange,
                onAddRoutineSetStep = onAddRoutineSetStep,
                onEditRoutineSetStep = onEditRoutineSetStep,
                onRemoveRoutineSetStep = onRemoveRoutineSetStep,
                onSaveRoutineSetDraft = onSaveRoutineSetDraft,
            )
            GuardianDestination.EnvironmentSettings -> GuardianEnvironmentSettingsScreen(
                settings = state.appSettings,
                useWideLayout = maxWidth >= GuardianSplitMinimumWidth && maxWidth > maxHeight,
                onOpenHome = onOpenHome,
                onFeedbackIntensityChange = onFeedbackIntensityChange,
                onTtsEnabledChange = onTtsEnabledChange,
                onTtsRateChange = onTtsRateChange,
                onTtsVolumeChange = onTtsVolumeChange,
                onSoundEnabledChange = onSoundEnabledChange,
                onHapticEnabledChange = onHapticEnabledChange,
                onNotificationLeadTimeChange = onNotificationLeadTimeChange,
                onQuietHoursEnabledChange = onQuietHoursEnabledChange,
                onQuietHoursStartChange = onQuietHoursStartChange,
                onQuietHoursEndChange = onQuietHoursEndChange,
            )
            GuardianDestination.Records -> GuardianRecordsScreen(
                state = state,
                useWideLayout = maxWidth >= GuardianSplitMinimumWidth && maxWidth > maxHeight,
                onOpenHome = onOpenHome,
                onSelectRecordsDate = onSelectRecordsDate,
            )
            GuardianDestination.Security -> GuardianSecurityScreen(
                onOpenHome = onOpenHome,
                onOpenPinChange = onOpenPinChange,
                onOpenBackupRestore = onOpenBackupRestore,
                onShowOutOfScopeNotice = onShowOutOfScopeNotice,
            )
            GuardianDestination.BackupRestore -> GuardianBackupRestoreScreen(
                state = state,
                onOpenSecurity = onOpenSecurity,
                onCreateBackupFile = onCreateBackupFile,
                onOpenRestoreFile = onOpenRestoreFile,
            )
        }
    }

    if (state.pendingDeleteRoutineId != null) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text(stringResource(R.string.guardian_delete_title)) },
            text = { Text(stringResource(R.string.guardian_delete_body)) },
            confirmButton = {
                SteppieButton(
                    label = stringResource(R.string.action_delete),
                    onClick = onConfirmDelete,
                    style = SteppieButtonStyle.Danger,
                )
            },
            dismissButton = {
                SteppieButton(
                    label = stringResource(R.string.action_cancel),
                    onClick = onCancelDelete,
                    style = SteppieButtonStyle.Secondary,
                )
            },
        )
    }

    if (state.pendingDeleteRoutineSetId != null) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text(stringResource(R.string.guardian_delete_routine_set_title)) },
            text = { Text(stringResource(R.string.guardian_delete_routine_set_body)) },
            confirmButton = {
                SteppieButton(
                    label = stringResource(R.string.action_delete),
                    onClick = onConfirmDeleteRoutineSet,
                    style = SteppieButtonStyle.Danger,
                )
            },
            dismissButton = {
                SteppieButton(
                    label = stringResource(R.string.action_cancel),
                    onClick = onCancelDelete,
                    style = SteppieButtonStyle.Secondary,
                )
            },
        )
    }

    if (state.editingRoutineSetId != null) {
        AlertDialog(
            onDismissRequest = onCancelEditRoutineSetName,
            title = { Text(stringResource(R.string.guardian_routine_set_rename_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small)) {
                    OutlinedTextField(
                        value = state.editingRoutineSetName,
                        onValueChange = onEditingRoutineSetNameChange,
                        label = { Text(stringResource(R.string.guardian_field_routine_set_name)) },
                        singleLine = true,
                    )
                    state.draftError?.let { ErrorMessage(it) }
                }
            },
            confirmButton = {
                SteppieButton(
                    label = stringResource(R.string.action_save),
                    onClick = onSaveEditingRoutineSetName,
                )
            },
            dismissButton = {
                SteppieButton(
                    label = stringResource(R.string.action_cancel),
                    onClick = onCancelEditRoutineSetName,
                    style = SteppieButtonStyle.Secondary,
                )
            },
        )
    }

    if (state.pendingRestorePreview != null) {
        AlertDialog(
            onDismissRequest = onCancelRestore,
            title = { Text(stringResource(R.string.guardian_restore_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small)) {
                    Text(
                        text = stringResource(
                            R.string.guardian_restore_confirm_body,
                            state.pendingRestorePreview.routineSetCount,
                            state.pendingRestorePreview.routineCount,
                            state.pendingRestorePreview.dailyLogCount,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.guardian_restore_pin_prompt),
                        style = SteppieTheme.typography.guardianBody,
                    )
                    val restorePinProgressDescription = stringResource(
                        R.string.a11y_pin_progress,
                        state.restorePinDigits.length,
                    )
                    Row(
                        modifier = Modifier.semantics {
                            contentDescription = restorePinProgressDescription
                        },
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        repeat(4) { index ->
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index < state.restorePinDigits.length) {
                                            SteppieTheme.colors.progressComplete
                                        } else {
                                            SteppieTheme.colors.progressPending
                                        },
                                    ),
                            )
                        }
                    }
                    CompactPinKeypad(onDigit = onRestorePinDigit, onDelete = onDeleteRestorePinDigit)
                    state.backupError?.let { ErrorMessage(it) }
                }
            },
            confirmButton = {},
            dismissButton = {
                SteppieButton(
                    label = stringResource(R.string.action_cancel),
                    onClick = onCancelRestore,
                    style = SteppieButtonStyle.Secondary,
                )
            },
        )
    }

    state.notice?.let { notice ->
        AlertDialog(
            onDismissRequest = onClearNotice,
            title = { Text(stringResource(R.string.guardian_notice_title)) },
            text = { Text(notice) },
            confirmButton = {
                SteppieButton(label = stringResource(R.string.action_ok), onClick = onClearNotice)
            },
        )
    }
}

@Composable
private fun GuardianPinScreen(
    state: GuardianModeUiState,
    onDigit: (Int) -> Unit,
    onDeletePinDigit: () -> Unit,
) {
    val pinProgressDescription = stringResource(R.string.a11y_pin_progress, state.pinDigits.length)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("guardian_pin")
            .verticalScroll(rememberScrollState())
            .padding(
                start = SteppieLayout.ChildScreenPadding,
                top = SteppieLayout.GuardianScreenPadding,
                end = SteppieLayout.ChildScreenPadding,
                bottom = SteppieLayout.ChildScreenPadding,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GuardianTopBar(
            title = when (state.pinMode) {
                GuardianPinMode.Enter -> stringResource(R.string.guardian_pin_title)
                GuardianPinMode.Setup -> stringResource(R.string.guardian_pin_setup_title)
                GuardianPinMode.ChangeCurrent -> stringResource(R.string.guardian_pin_change_current_title)
                GuardianPinMode.ChangeNew -> stringResource(R.string.guardian_pin_change_new_title)
            },
            subtitle = when (state.pinMode) {
                GuardianPinMode.Enter -> stringResource(R.string.guardian_pin_subtitle)
                GuardianPinMode.Setup -> stringResource(R.string.guardian_pin_setup_subtitle)
                GuardianPinMode.ChangeCurrent -> stringResource(R.string.guardian_pin_change_current_subtitle)
                GuardianPinMode.ChangeNew -> stringResource(R.string.guardian_pin_change_new_subtitle)
            },
        )
        Spacer(Modifier.height(SteppieSpacing.Large))
        Row(
            modifier = Modifier.semantics {
                contentDescription = pinProgressDescription
            },
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < state.pinDigits.length) {
                                SteppieTheme.colors.progressComplete
                            } else {
                                SteppieTheme.colors.progressPending
                            },
                        ),
                )
            }
        }
        Spacer(Modifier.height(SteppieSpacing.Large))
        PinKeypad(onDigit = onDigit, onDelete = onDeletePinDigit)
        if (state.pinError != null) {
            Spacer(Modifier.height(28.dp))
            ErrorMessage(state.pinError)
        }
    }
}

@Composable
private fun PinKeypad(onDigit: (Int) -> Unit, onDelete: () -> Unit) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫"))
    val deleteDescription = stringResource(R.string.a11y_pin_delete)
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { label ->
                    if (label.isBlank()) {
                        Spacer(Modifier.size(width = 90.dp, height = SteppieLayout.ChildMinimumTouchTarget))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(width = 90.dp, height = SteppieLayout.ChildMinimumTouchTarget)
                                .clip(RoundedCornerShape(SteppieCornerRadius.Card))
                                .background(MaterialTheme.colorScheme.surface)
                                .clearAndSetSemantics {
                                    contentDescription = if (label == "⌫") deleteDescription else label
                                    role = Role.Button
                                    semanticOnClick {
                                        if (label == "⌫") onDelete() else onDigit(label.toInt())
                                        true
                                    }
                                }
                                .clickable(role = Role.Button) {
                                    if (label == "⌫") onDelete() else onDigit(label.toInt())
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = SteppieTheme.typography.childCardTitle,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactPinKeypad(onDigit: (Int) -> Unit, onDelete: () -> Unit) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫"))
    val deleteDescription = stringResource(R.string.a11y_pin_delete)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SteppieSpacing.ExtraSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.ExtraSmall)) {
                row.forEach { label ->
                    if (label.isBlank()) {
                        Spacer(Modifier.size(width = 64.dp, height = 56.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(width = 64.dp, height = 56.dp)
                                .clip(RoundedCornerShape(SteppieCornerRadius.Control))
                                .background(MaterialTheme.colorScheme.surface)
                                .clearAndSetSemantics {
                                    contentDescription = if (label == "⌫") deleteDescription else label
                                    role = Role.Button
                                    semanticOnClick {
                                        if (label == "⌫") onDelete() else onDigit(label.toInt())
                                        true
                                    }
                                }
                                .clickable(role = Role.Button) {
                                    if (label == "⌫") onDelete() else onDigit(label.toInt())
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = SteppieTheme.typography.guardianSection,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuardianHomeScreen(
    onCloseToChild: () -> Unit,
    onOpenRoutineSetCreate: () -> Unit,
    onOpenRoutineEdit: () -> Unit,
    onOpenTemplateSelect: () -> Unit,
    onOpenEnvironmentSettings: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenSecurity: () -> Unit,
    onShowOutOfScopeNotice: () -> Unit,
) {
    GuardianScaffold(
        title = stringResource(R.string.guardian_home_title),
        subtitle = stringResource(R.string.guardian_home_subtitle),
        bottom = {
            SteppieButton(
                label = stringResource(R.string.guardian_done),
                onClick = onCloseToChild,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        GuardianMenuCard(R.drawable.ic_guardian_menu_routine, stringResource(R.string.guardian_menu_create_routine_set), stringResource(R.string.guardian_menu_create_routine_set_desc), onOpenRoutineSetCreate)
        GuardianMenuCard(R.drawable.ic_guardian_menu_routine, stringResource(R.string.guardian_template_action), stringResource(R.string.guardian_template_home_desc), onOpenTemplateSelect)
        GuardianMenuCard(R.drawable.ic_guardian_menu_routine, stringResource(R.string.guardian_menu_routine), stringResource(R.string.guardian_menu_routine_desc), onOpenRoutineEdit)
        GuardianMenuCard(R.drawable.ic_guardian_menu_settings, stringResource(R.string.guardian_menu_feedback), stringResource(R.string.guardian_menu_feedback_desc), onOpenEnvironmentSettings)
        GuardianMenuCard(R.drawable.ic_guardian_menu_records, stringResource(R.string.guardian_menu_records), stringResource(R.string.guardian_menu_records_desc), onOpenRecords)
        GuardianMenuCard(R.drawable.ic_guardian_menu_security, stringResource(R.string.guardian_menu_security), stringResource(R.string.guardian_menu_security_desc), onOpenSecurity)
    }
}

@Composable
private fun GuardianEnvironmentSettingsScreen(
    settings: AppSettings,
    useWideLayout: Boolean,
    onOpenHome: () -> Unit,
    onFeedbackIntensityChange: (FeedbackIntensity) -> Unit,
    onTtsEnabledChange: (Boolean) -> Unit,
    onTtsRateChange: (Double) -> Unit,
    onTtsVolumeChange: (Double) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onHapticEnabledChange: (Boolean) -> Unit,
    onNotificationLeadTimeChange: (Int, Boolean) -> Unit,
    onQuietHoursEnabledChange: (Boolean) -> Unit,
    onQuietHoursStartChange: (String) -> Unit,
    onQuietHoursEndChange: (String) -> Unit,
) {
    GuardianScaffold(
        title = stringResource(R.string.guardian_environment_title),
        subtitle = stringResource(R.string.guardian_environment_subtitle),
        onBack = onOpenHome,
    ) {
        val firstColumn: @Composable ColumnScope.() -> Unit = {
            SettingBlock(title = stringResource(R.string.guardian_setting_feedback_intensity)) {
                SegmentedSetting(
                    options = listOf(
                        FeedbackIntensity.Strong to stringResource(R.string.guardian_feedback_strong),
                        FeedbackIntensity.Normal to stringResource(R.string.guardian_feedback_normal),
                        FeedbackIntensity.Quiet to stringResource(R.string.guardian_feedback_quiet),
                        FeedbackIntensity.Off to stringResource(R.string.guardian_feedback_off),
                    ),
                    selected = settings.feedbackIntensity,
                    onSelected = onFeedbackIntensityChange,
                )
            }
            SettingBlock(title = stringResource(R.string.guardian_setting_tts_enabled)) {
                BooleanSegmentedSetting(
                    enabled = settings.ttsEnabled,
                    onEnabledChange = onTtsEnabledChange,
                )
            }
            SettingBlock(title = stringResource(R.string.guardian_setting_tts_rate)) {
                SliderSetting(
                    value = settings.ttsRate.toFloat(),
                    range = 0.5f..1.5f,
                    steps = 9,
                    startLabel = "0.5",
                    endLabel = "1.5",
                    valueLabel = String.format(Locale.getDefault(), "%.1fx", settings.ttsRate),
                    onValueChange = { onTtsRateChange(it.toDouble()) },
                )
            }
            SettingBlock(title = stringResource(R.string.guardian_setting_tts_volume)) {
                SliderSetting(
                    value = settings.ttsVolume.toFloat(),
                    range = 0f..1f,
                    steps = 9,
                    startLabel = "0.0",
                    endLabel = "1.0",
                    valueLabel = "${(settings.ttsVolume * 100).toInt()}%",
                    onValueChange = { onTtsVolumeChange(it.toDouble()) },
                )
            }
            SettingBlock(title = stringResource(R.string.guardian_setting_sound_enabled)) {
                BooleanSegmentedSetting(
                    enabled = settings.soundEnabled,
                    onEnabledChange = onSoundEnabledChange,
                )
            }
            SettingBlock(title = stringResource(R.string.guardian_setting_haptic_enabled)) {
                BooleanSegmentedSetting(
                    enabled = settings.hapticEnabled,
                    onEnabledChange = onHapticEnabledChange,
                )
            }
        }
        val secondColumn: @Composable ColumnScope.() -> Unit = {
            SettingBlock(title = stringResource(R.string.guardian_setting_notification_lead_times)) {
                NotificationLeadTimeRow(
                    label = stringResource(R.string.guardian_notification_10_minutes),
                    enabled = 10 in settings.notificationLeadTimes,
                    onEnabledChange = { onNotificationLeadTimeChange(10, it) },
                )
                NotificationLeadTimeRow(
                    label = stringResource(R.string.guardian_notification_5_minutes),
                    enabled = 5 in settings.notificationLeadTimes,
                    onEnabledChange = { onNotificationLeadTimeChange(5, it) },
                )
            }
            SettingBlock(title = stringResource(R.string.guardian_setting_quiet_hours)) {
                val quietHoursEnabled = settings.quietHoursStart != null && settings.quietHoursEnd != null
                BooleanSegmentedSetting(
                    enabled = quietHoursEnabled,
                    onEnabledChange = onQuietHoursEnabledChange,
                )
                if (quietHoursEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
                    ) {
                        QuietHoursField(
                            label = stringResource(R.string.guardian_quiet_hours_start),
                            value = settings.quietHoursStart?.toString().orEmpty(),
                            onValueChange = onQuietHoursStartChange,
                            modifier = Modifier.weight(1f),
                        )
                        QuietHoursField(
                            label = stringResource(R.string.guardian_quiet_hours_end),
                            value = settings.quietHoursEnd?.toString().orEmpty(),
                            onValueChange = onQuietHoursEndChange,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        if (useWideLayout) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
                    content = firstColumn,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
                    content = secondColumn,
                )
            }
        } else {
            firstColumn()
            secondColumn()
        }
        QuietHoursNotice(settings)
    }
}

@Composable
private fun SettingBlock(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SteppieCornerRadius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(SteppieStroke.Divider, MaterialTheme.colorScheme.outline, RoundedCornerShape(SteppieCornerRadius.Card))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(SteppieSpacing.ExtraSmall),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = SteppieTheme.typography.button,
        )
        content()
    }
}

@Composable
private fun BooleanSegmentedSetting(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    SegmentedSetting(
        options = listOf(
            true to stringResource(R.string.guardian_setting_on),
            false to stringResource(R.string.guardian_setting_off),
        ),
        selected = enabled,
        onSelected = onEnabledChange,
    )
}

@Composable
private fun <T> SegmentedSetting(
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            val state = stringResource(if (isSelected) R.string.a11y_selected else R.string.a11y_not_selected)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) SteppieTheme.colors.progressComplete else MaterialTheme.colorScheme.surfaceVariant)
                    .clearAndSetSemantics {
                        contentDescription = label
                        stateDescription = state
                        role = Role.Button
                        semanticOnClick {
                            onSelected(value)
                            true
                        }
                    }
                    .clickable(role = Role.Button) { onSelected(value) }
                    .padding(horizontal = SteppieSpacing.ExtraSmall, vertical = SteppieSpacing.ExtraSmall),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                    style = SteppieTheme.typography.guardianCaption,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun SliderSetting(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    startLabel: String,
    endLabel: String,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
    ) {
        Text(
            text = startLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = SteppieTheme.typography.guardianBody,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = endLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = SteppieTheme.typography.guardianBody,
        )
        Text(
            text = valueLabel,
            modifier = Modifier.width(64.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = SteppieTheme.typography.button,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun NotificationLeadTimeRow(
    label: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            style = SteppieTheme.typography.guardianBody,
        )
        Row(
            modifier = Modifier.weight(1.4f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(true to R.string.guardian_setting_on, false to R.string.guardian_setting_off).forEach { (value, labelRes) ->
                val isSelected = value == enabled
                val segmentLabel = stringResource(labelRes)
                val state = stringResource(if (isSelected) R.string.a11y_selected else R.string.a11y_not_selected)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) SteppieTheme.colors.progressComplete else MaterialTheme.colorScheme.surfaceVariant)
                        .clearAndSetSemantics {
                            contentDescription = "$label $segmentLabel"
                            stateDescription = state
                            role = Role.Button
                            semanticOnClick {
                                onEnabledChange(value)
                                true
                            }
                        }
                        .clickable(role = Role.Button) { onEnabledChange(value) }
                        .padding(horizontal = SteppieSpacing.ExtraSmall, vertical = SteppieSpacing.ExtraSmall),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = segmentLabel,
                        color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                        style = SteppieTheme.typography.guardianCaption,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuietHoursField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { nextValue ->
            val sanitized = nextValue.filter { it.isDigit() || it == ':' }.take(5)
            text = sanitized
            if (sanitized.length == 5) {
                onValueChange(sanitized)
            }
        },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        textStyle = SteppieTheme.typography.guardianBody,
    )
}

@Composable
private fun QuietHoursNotice(settings: AppSettings) {
    val range = if (settings.quietHoursStart != null && settings.quietHoursEnd != null) {
        "${settings.quietHoursStart}-${settings.quietHoursEnd}"
    } else {
        stringResource(R.string.guardian_quiet_hours_disabled)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SteppieCornerRadius.Control))
            .background(SteppieTheme.colors.warning.copy(alpha = 0.18f))
            .border(SteppieStroke.Divider, SteppieTheme.colors.warning, RoundedCornerShape(SteppieCornerRadius.Control))
            .padding(horizontal = SteppieSpacing.Small, vertical = SteppieSpacing.ExtraSmall),
        verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall),
    ) {
        Text(
            text = stringResource(R.string.guardian_quiet_hours_notice_title, range),
            color = SteppieTheme.colors.warning,
            style = SteppieTheme.typography.button,
        )
        Text(
            text = stringResource(R.string.guardian_quiet_hours_notice_body),
            color = MaterialTheme.colorScheme.onSurface,
            style = SteppieTheme.typography.guardianCaption,
        )
    }
}

@Composable
private fun GuardianRoutineEditScreen(
    state: GuardianModeUiState,
    onOpenHome: () -> Unit,
    onOpenRoutineSetCreate: () -> Unit,
    onOpenTemplateSelect: () -> Unit,
    onOpenNewRoutineEditor: () -> Unit,
    onOpenRoutineEditor: (String) -> Unit,
    onToggleRoutineSetListEditing: () -> Unit,
    onSelectRoutineSet: (String) -> Unit,
    onRequestEditRoutineSetName: (String) -> Unit,
    onRequestDeleteRoutineSet: (String) -> Unit,
    onRequestDelete: (String) -> Unit,
    onMoveRoutine: (String, Int) -> Unit,
    onShowOutOfScopeNotice: () -> Unit,
) {
    GuardianScaffold(
        title = stringResource(R.string.guardian_menu_routine),
        subtitle = stringResource(R.string.guardian_routine_edit_subtitle),
        onBack = onOpenHome,
        topActionLabel = stringResource(
            if (state.routineSetListEditing) R.string.action_done_editing else R.string.action_edit,
        ),
        onTopAction = onToggleRoutineSetListEditing,
        bottom = {
            Column(verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small)) {
                if (state.activeRoutineSet == null) {
                    SteppieButton(
                        label = stringResource(R.string.guardian_menu_create_routine_set),
                        onClick = onOpenRoutineSetCreate,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    SteppieButton(
                        label = stringResource(R.string.guardian_template_action),
                        onClick = onOpenTemplateSelect,
                        modifier = Modifier.fillMaxWidth(),
                        style = SteppieButtonStyle.Secondary,
                    )
                    SteppieButton(
                        label = stringResource(R.string.guardian_add_routine),
                        onClick = onOpenNewRoutineEditor,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) {
        if (state.activeRoutineSet == null) {
            EmptyRoutineSetPanel(onOpenNewRoutineSet = onOpenRoutineSetCreate)
        }
        RoutineSetList(
            routineSets = state.routineSets,
            activeRoutineSetId = state.activeRoutineSet?.id,
            editing = state.routineSetListEditing,
            onSelectRoutineSet = onSelectRoutineSet,
            onRequestEditRoutineSetName = onRequestEditRoutineSetName,
            onRequestDeleteRoutineSet = onRequestDeleteRoutineSet,
        )
        if (state.activeRoutineSet != null) {
            Column(
                modifier = Modifier.padding(top = SteppieSpacing.Medium + SteppieSpacing.TwoExtraSmall),
                verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = SteppieLayout.GuardianMinimumTouchTarget),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.guardian_active_routine_steps_title, state.title),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = SteppieTheme.typography.guardianTitle,
                    )
                }
                Text(
                    text = stringResource(R.string.guardian_active_routine_steps_desc),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = SteppieTheme.typography.guardianCaption,
                )
            }
        }
        state.routines.forEach { routine ->
            EditableRoutineRow(
                routine = routine,
                onClick = { onOpenRoutineEditor(routine.id) },
                onDelete = { onRequestDelete(routine.id) },
                onMove = { direction -> onMoveRoutine(routine.id, direction) },
            )
        }
    }
}

@Composable
private fun RoutineSetList(
    routineSets: List<RoutineSet>,
    activeRoutineSetId: String?,
    editing: Boolean,
    onSelectRoutineSet: (String) -> Unit,
    onRequestEditRoutineSetName: (String) -> Unit,
    onRequestDeleteRoutineSet: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
    ) {
        routineSets.forEach { routineSet ->
            RoutineSetRow(
                routineSet = routineSet,
                active = routineSet.id == activeRoutineSetId,
                editing = editing,
                onSelect = { onSelectRoutineSet(routineSet.id) },
                onEdit = { onRequestEditRoutineSetName(routineSet.id) },
                onDelete = { onRequestDeleteRoutineSet(routineSet.id) },
            )
        }
    }
}

@Composable
private fun RoutineSetRow(
    routineSet: RoutineSet,
    active: Boolean,
    editing: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val borderColor = if (active) SteppieTheme.colors.warning else MaterialTheme.colorScheme.outline
    val routineSetName = routineSet.name.resolve(null, Locale.getDefault().toLanguageTag())
    val meta = stringResource(
        if (active) R.string.guardian_routine_set_active_meta else R.string.guardian_routine_set_meta,
        routineSet.routines.size,
    )
    val selectionState = stringResource(if (active) R.string.a11y_selected else R.string.a11y_not_selected)
    val accessibilityDescription = stringResource(R.string.a11y_routine_set_row, routineSetName, meta)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(SteppieCornerRadius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                if (active) 2.dp else SteppieStroke.Divider,
                borderColor,
                RoundedCornerShape(SteppieCornerRadius.Card),
            )
            .then(
                if (editing) {
                    Modifier.semantics {
                        contentDescription = accessibilityDescription
                        stateDescription = selectionState
                    }
                } else {
                    Modifier.clearAndSetSemantics {
                        contentDescription = accessibilityDescription
                        stateDescription = selectionState
                        role = Role.Button
                        semanticOnClick {
                            onSelect()
                            true
                        }
                    }
                },
            )
            .clickable(role = Role.Button, enabled = !editing, onClick = onSelect)
            .padding(horizontal = SteppieSpacing.Large, vertical = SteppieSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium),
    ) {
        RoutineSetSelectionMark(active = active)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall)) {
            Text(
                text = routineSetName,
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.button,
                maxLines = 2,
            )
            Text(
                text = meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = SteppieTheme.typography.guardianCaption,
            )
        }
        if (editing) {
            IconTextButton(
                label = stringResource(R.string.a11y_item_action, routineSetName, stringResource(R.string.action_edit)),
                text = "✎",
                onClick = onEdit,
                danger = false,
            )
            IconTextButton(
                label = stringResource(R.string.a11y_item_action, routineSetName, stringResource(R.string.action_delete)),
                text = "-",
                onClick = onDelete,
                danger = true,
            )
        }
    }
}

@Composable
private fun RoutineSetSelectionMark(active: Boolean) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (active) SteppieTheme.colors.warning else Color.Transparent)
            .border(3.dp, SteppieTheme.colors.warning, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (active) {
            Text(
                text = "✓",
                color = MaterialTheme.colorScheme.onError,
                style = SteppieTheme.typography.button,
            )
        }
    }
}

@Composable
private fun IconTextButton(
    label: String,
    text: String,
    onClick: () -> Unit,
    danger: Boolean,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surface)
            .border(
                SteppieStroke.Divider,
                if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                CircleShape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (danger) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface,
            style = SteppieTheme.typography.guardianSection,
        )
    }
}

@Composable
private fun GuardianRoutineSetCreateScreen(
    state: GuardianModeUiState,
    useSplitLayout: Boolean,
    onOpenHome: () -> Unit,
    onRoutineSetNameChange: (String) -> Unit,
    onRoutineSetStepTitleChange: (String) -> Unit,
    onRoutineSetStepIconChange: (String) -> Unit,
    onRoutineSetStepColorChange: (String) -> Unit,
    onRoutineSetStepScheduledTimeChange: (String) -> Unit,
    onAddRoutineSetStep: () -> Unit,
    onEditRoutineSetStep: (Int) -> Unit,
    onRemoveRoutineSetStep: (Int) -> Unit,
    onSaveRoutineSetDraft: () -> Unit,
) {
    val draft = state.routineSetDraft ?: return
    GuardianScaffold(
        title = stringResource(R.string.guardian_routine_set_create_title),
        subtitle = stringResource(R.string.guardian_routine_set_create_subtitle),
        onBack = onOpenHome,
        bottom = {
            Row(horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium)) {
                SteppieButton(
                    label = stringResource(R.string.action_cancel),
                    onClick = onOpenHome,
                    modifier = Modifier.weight(1f),
                    style = SteppieButtonStyle.Secondary,
                )
                SteppieButton(
                    label = stringResource(R.string.action_save),
                    onClick = onSaveRoutineSetDraft,
                    modifier = Modifier.weight(1f),
                    state = if (draft.steps.isEmpty()) SteppieButtonState.Disabled else SteppieButtonState.Enabled,
                )
            }
        },
    ) {
        if (useSplitLayout) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("guardian_routine_set_create_split"),
                horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Large),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
                ) {
                    RoutineSetNameField(draft.name, onRoutineSetNameChange)
                    RoutineSetStepList(draft.steps, onEditRoutineSetStep, onRemoveRoutineSetStep)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
                ) {
                    RoutineSetStepEditor(
                        draft = draft.stepDraft,
                        onTitleChange = onRoutineSetStepTitleChange,
                        onIconChange = onRoutineSetStepIconChange,
                        onColorChange = onRoutineSetStepColorChange,
                        onScheduledTimeChange = onRoutineSetStepScheduledTimeChange,
                        onAddStep = onAddRoutineSetStep,
                        isEditing = draft.editingStepIndex != null,
                    )
                    state.draftError?.let { ErrorMessage(it) }
                    WarningMessage(stringResource(R.string.guardian_unsaved_warning))
                }
            }
        } else {
            RoutineSetNameField(draft.name, onRoutineSetNameChange)
            RoutineSetStepList(draft.steps, onEditRoutineSetStep, onRemoveRoutineSetStep)
            RoutineSetStepEditor(
                draft = draft.stepDraft,
                onTitleChange = onRoutineSetStepTitleChange,
                onIconChange = onRoutineSetStepIconChange,
                onColorChange = onRoutineSetStepColorChange,
                onScheduledTimeChange = onRoutineSetStepScheduledTimeChange,
                onAddStep = onAddRoutineSetStep,
                isEditing = draft.editingStepIndex != null,
            )
            state.draftError?.let { ErrorMessage(it) }
            WarningMessage(stringResource(R.string.guardian_unsaved_warning))
        }
    }
}

@Composable
private fun RoutineSetNameField(
    name: String,
    onNameChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.guardian_field_routine_set_name)) },
        singleLine = true,
    )
}

@Composable
private fun RoutineSetStepEditor(
    draft: RoutineDraft,
    onTitleChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onScheduledTimeChange: (String) -> Unit,
    onAddStep: () -> Unit,
    isEditing: Boolean,
) {
    GuardianPanel(title = stringResource(R.string.guardian_step_editor_title)) {
        OutlinedTextField(
            value = draft.title,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.guardian_field_step_title)) },
            singleLine = true,
        )
        IconPicker(selectedIcon = draft.iconName, onSelected = onIconChange)
        ColorPicker(selectedColorToken = draft.colorToken, onSelected = onColorChange)
        OutlinedTextField(
            value = draft.scheduledTime,
            onValueChange = onScheduledTimeChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.guardian_field_time)) },
            placeholder = { Text(stringResource(R.string.guardian_time_placeholder)) },
            singleLine = true,
        )
        SteppieButton(
            label = stringResource(if (isEditing) R.string.guardian_update_step else R.string.guardian_add_step),
            onClick = onAddStep,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RoutineSetStepList(
    steps: List<RoutineDraft>,
    onEditStep: (Int) -> Unit,
    onRemoveStep: (Int) -> Unit,
) {
    GuardianPanel(title = stringResource(R.string.guardian_step_list_title, steps.size)) {
        if (steps.isEmpty()) {
            Text(
                text = stringResource(R.string.guardian_step_list_empty),
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.guardianCaption,
            )
        } else {
            steps.forEachIndexed { index, step ->
                RoutineSetStepRow(
                    index = index,
                    step = step,
                    onEdit = { onEditStep(index) },
                    onRemove = { onRemoveStep(index) },
                )
            }
        }
    }
}

@Composable
private fun RoutineSetStepRow(
    index: Int,
    step: RoutineDraft,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(RoundedCornerShape(SteppieCornerRadius.Control))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(SteppieSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            RoutineIcon(IconRef.Builtin(step.iconName), focus = false, modifier = Modifier.size(40.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.guardian_step_order_title, index + 1, step.title),
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.button,
                maxLines = 2,
            )
            Text(
                text = step.scheduledTime.ifBlank { stringResource(R.string.guardian_time_none) },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = SteppieTheme.typography.guardianCaption,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(SteppieSpacing.ExtraSmall)) {
            SteppieButton(
                label = stringResource(R.string.action_edit),
                onClick = onEdit,
                style = SteppieButtonStyle.Secondary,
            )
            SteppieButton(
                label = stringResource(R.string.action_delete),
                onClick = onRemove,
                style = SteppieButtonStyle.Danger,
            )
        }
    }
}

@Composable
private fun EmptyRoutineSetPanel(onOpenNewRoutineSet: () -> Unit) {
    GuardianPanel(
        title = stringResource(R.string.guardian_empty_routine_set_title),
        body = stringResource(R.string.guardian_empty_routine_set_body),
    ) {
        SteppieButton(
            label = stringResource(R.string.guardian_menu_create_routine_set),
            onClick = onOpenNewRoutineSet,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun GuardianRoutineSplitScreen(
    state: GuardianModeUiState,
    onOpenHome: () -> Unit,
    onOpenRoutineSetCreate: () -> Unit,
    onOpenTemplateSelect: () -> Unit,
    onOpenNewRoutineEditor: () -> Unit,
    onOpenRoutineEditor: (String) -> Unit,
    onToggleRoutineSetListEditing: () -> Unit,
    onSelectRoutineSet: (String) -> Unit,
    onRequestEditRoutineSetName: (String) -> Unit,
    onRequestDeleteRoutineSet: (String) -> Unit,
    onRequestDelete: (String) -> Unit,
    onMoveRoutine: (String, Int) -> Unit,
    onShowOutOfScopeNotice: () -> Unit,
) {
    Row(Modifier.fillMaxSize().testTag("guardian_split")) {
        Box(
            modifier = Modifier
                .width(SteppieLayout.SplitListWidth)
                .fillMaxHeight(),
        ) {
            GuardianRoutineEditScreen(
                state = state,
                onOpenHome = onOpenHome,
                onOpenRoutineSetCreate = onOpenRoutineSetCreate,
                onOpenTemplateSelect = onOpenTemplateSelect,
                onOpenNewRoutineEditor = onOpenNewRoutineEditor,
                onOpenRoutineEditor = onOpenRoutineEditor,
                onToggleRoutineSetListEditing = onToggleRoutineSetListEditing,
                onSelectRoutineSet = onSelectRoutineSet,
                onRequestEditRoutineSetName = onRequestEditRoutineSetName,
                onRequestDeleteRoutineSet = onRequestDeleteRoutineSet,
                onRequestDelete = onRequestDelete,
                onMoveRoutine = onMoveRoutine,
                onShowOutOfScopeNotice = onShowOutOfScopeNotice,
            )
        }
        Box(
            modifier = Modifier
                .width(SteppieStroke.Divider)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outline),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(SteppieSpacing.TwoExtraLarge),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.guardian_tablet_edit_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = SteppieTheme.typography.guardianTitle,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun GuardianTemplateSelectScreen(
    state: GuardianModeUiState,
    onBack: () -> Unit,
    onPreviewTemplate: (RoutineTemplateId) -> Unit,
    onSaveTemplatePreview: () -> Unit,
    useWideLayout: Boolean,
) {
    val selectedTemplate = state.selectedTemplate ?: RoutineTemplates.all.first()
    GuardianScaffold(
        title = stringResource(R.string.guardian_template_action),
        subtitle = stringResource(R.string.guardian_template_select_subtitle),
        onBack = onBack,
        bottom = {
            Row(horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium)) {
                SteppieButton(
                    label = stringResource(R.string.action_back),
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    style = SteppieButtonStyle.Secondary,
                )
                SteppieButton(
                    label = stringResource(R.string.guardian_template_save_action),
                    onClick = onSaveTemplatePreview,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        if (useWideLayout) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("guardian_template_select_split"),
                horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Large),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
                ) {
                    RoutineTemplates.all.forEach { template ->
                        TemplateSelectionRow(
                            template = template,
                            selected = template.id == selectedTemplate.id,
                            onClick = { onPreviewTemplate(template.id) },
                        )
                    }
                }
                TemplatePreviewSteps(template = selectedTemplate, modifier = Modifier.weight(1f))
            }
        } else {
            RoutineTemplates.all.forEach { template ->
                TemplateSelectionRow(
                    template = template,
                    selected = template.id == selectedTemplate.id,
                    onClick = { onPreviewTemplate(template.id) },
                )
            }
            TemplatePreviewSummary(template = selectedTemplate)
            TemplatePreviewSteps(template = selectedTemplate)
        }
        state.draftError?.let { ErrorMessage(it) }
    }
}

@Composable
private fun TemplateSelectionRow(
    template: RoutineTemplate,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val templateName = template.name.resolve(null, Locale.getDefault().toLanguageTag())
    val stepCount = stringResource(R.string.guardian_template_step_count, template.steps.size)
    val accessibilityDescription = stringResource(R.string.a11y_template_row, templateName, stepCount)
    val selectionState = stringResource(if (selected) R.string.a11y_selected else R.string.a11y_not_selected)
    val borderColor = if (selected) SteppieTheme.colors.warning else MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(SteppieCornerRadius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                if (selected) 2.dp else SteppieStroke.Divider,
                borderColor,
                RoundedCornerShape(SteppieCornerRadius.Card),
            )
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = accessibilityDescription
                role = Role.Button
                stateDescription = selectionState
            }
            .padding(SteppieSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium),
    ) {
        TemplateSelectionMark(selected = selected)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall)) {
            Text(
                text = templateName,
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.guardianTitle,
            )
            Text(
                text = stepCount,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = SteppieTheme.typography.guardianCaption,
            )
        }
    }
}

@Composable
private fun TemplateSelectionMark(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (selected) SteppieTheme.colors.warning else Color.Transparent)
            .border(3.dp, SteppieTheme.colors.warning, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Text(
                text = "✓",
                color = MaterialTheme.colorScheme.onError,
                style = SteppieTheme.typography.button,
            )
        }
    }
}

@Composable
private fun TemplatePreviewSummary(
    template: RoutineTemplate,
    modifier: Modifier = Modifier,
) {
    val templateName = template.name.resolve(null, Locale.getDefault().toLanguageTag())
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall),
    ) {
        Text(
            text = templateName,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = SteppieTheme.typography.guardianTitle,
        )
        Text(
            text = stringResource(R.string.guardian_template_preview_summary, template.steps.size),
            color = MaterialTheme.colorScheme.onSurface,
            style = SteppieTheme.typography.guardianBody,
        )
    }
}

@Composable
private fun TemplatePreviewSteps(
    template: RoutineTemplate,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
    ) {
        template.steps.forEachIndexed { index, step ->
            TemplatePreviewStepRow(index = index, step = step)
        }
    }
}

@Composable
private fun TemplatePreviewStepRow(
    index: Int,
    step: RoutineTemplateStep,
) {
    val title = step.title.resolve(null, Locale.getDefault().toLanguageTag())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .clip(RoundedCornerShape(SteppieCornerRadius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(SteppieStroke.Divider, MaterialTheme.colorScheme.outline, RoundedCornerShape(SteppieCornerRadius.Card))
            .padding(SteppieSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (index + 1).toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = SteppieTheme.typography.button,
            )
        }
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(SteppieCornerRadius.Control))
                .background(colorForToken(step.colorToken)),
            contentAlignment = Alignment.Center,
        ) {
            RoutineIcon(IconRef.Builtin(step.iconName), focus = false, modifier = Modifier.size(44.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.button,
            )
            Text(
                text = stringResource(R.string.guardian_time_none),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = SteppieTheme.typography.guardianCaption,
            )
        }
    }
}

@Composable
private fun GuardianCardEditScreen(
    state: GuardianModeUiState,
    onOpenRoutineEdit: () -> Unit,
    onRequestDelete: (String) -> Unit,
    onDraftTitleChange: (String) -> Unit,
    onDraftIconChange: (String) -> Unit,
    onDraftColorChange: (String) -> Unit,
    onDraftScheduledTimeChange: (String) -> Unit,
    onSaveDraft: () -> Unit,
) {
    val draft = state.draft ?: return
    GuardianScaffold(
        title = stringResource(R.string.guardian_card_edit_title),
        subtitle = stringResource(R.string.guardian_card_edit_subtitle),
        onBack = onOpenRoutineEdit,
        bottom = {
            Column(verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small)) {
                if (!draft.isNew && draft.routineId != null) {
                    SteppieButton(
                        label = stringResource(R.string.action_delete),
                        onClick = { onRequestDelete(draft.routineId) },
                        modifier = Modifier.fillMaxWidth(),
                        style = SteppieButtonStyle.Danger,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium)) {
                    SteppieButton(
                        label = stringResource(R.string.action_cancel),
                        onClick = onOpenRoutineEdit,
                        modifier = Modifier.weight(1f),
                        style = SteppieButtonStyle.Secondary,
                    )
                    SteppieButton(
                        label = stringResource(R.string.action_save),
                        onClick = onSaveDraft,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    ) {
        OutlinedTextField(
            value = draft.title,
            onValueChange = onDraftTitleChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.guardian_field_title)) },
            singleLine = true,
        )
        IconPicker(selectedIcon = draft.iconName, onSelected = onDraftIconChange)
        ColorPicker(selectedColorToken = draft.colorToken, onSelected = onDraftColorChange)
        OutlinedTextField(
            value = draft.scheduledTime,
            onValueChange = onDraftScheduledTimeChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.guardian_field_time)) },
            placeholder = { Text(stringResource(R.string.guardian_time_placeholder)) },
            singleLine = true,
        )
        WarningMessage(stringResource(R.string.guardian_unsaved_warning))
        state.draftError?.let { ErrorMessage(it) }
    }
}

@Composable
private fun GuardianRecordsScreen(
    state: GuardianModeUiState,
    useWideLayout: Boolean,
    onOpenHome: () -> Unit,
    onSelectRecordsDate: (LocalDate) -> Unit,
) {
    GuardianScaffold(
        title = stringResource(R.string.guardian_records_title),
        subtitle = stringResource(R.string.guardian_records_subtitle),
        onBack = onOpenHome,
    ) {
        if (useWideLayout) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("guardian_records_split"),
                horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Large),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
                ) {
                    GuardianRecordsDayList(state, onSelectRecordsDate)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
                ) {
                    GuardianRecordsDetail(state)
                }
            }
        } else {
            GuardianRecordsDayList(state, onSelectRecordsDate)
            GuardianRecordsDetail(state)
        }
    }
}

@Composable
private fun GuardianRecordsDayList(
    state: GuardianModeUiState,
    onSelectRecordsDate: (LocalDate) -> Unit,
) {
    GuardianPanel(title = stringResource(R.string.guardian_records_recent_title)) {
        state.recordDays.forEach { day ->
            GuardianRecordDayRow(
                day = day,
                selected = day.date == state.selectedRecordsDate,
                onClick = { onSelectRecordsDate(day.date) },
            )
        }
    }
}

@Composable
private fun GuardianRecordDayRow(
    day: GuardianRecordDay,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val dateText = recordDateText(day.date)
    val statusText = recordDayStatusText(day)
    val description = stringResource(
        R.string.a11y_guardian_record_day,
        dateText,
        day.completedCount,
        day.totalCount,
        day.completionPercent,
        statusText,
    )
    val selectionState = stringResource(if (selected) R.string.a11y_selected else R.string.a11y_not_selected)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 74.dp)
            .clip(RoundedCornerShape(SteppieCornerRadius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                if (selected) 2.dp else SteppieStroke.Divider,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(SteppieCornerRadius.Card),
            )
            .clearAndSetSemantics {
                contentDescription = description
                stateDescription = selectionState
                role = Role.Button
                semanticOnClick {
                    onClick()
                    true
                }
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = SteppieSpacing.Medium, vertical = SteppieSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
    ) {
        Column(
            modifier = Modifier.width(72.dp),
            verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall),
        ) {
            Text(
                text = weekdayText(day.date),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = SteppieTheme.typography.guardianSection,
            )
            Text(
                text = recordDateText(day.date),
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.guardianBody,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (day.hasRecords) {
                GuardianRecordProgressIndicator(
                    completedCount = day.completedCount,
                    totalCount = day.totalCount,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Text(
            text = statusText,
            color = MaterialTheme.colorScheme.onSurface,
            style = SteppieTheme.typography.guardianBody,
            maxLines = 2,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 84.dp, max = 112.dp),
        )
    }
}

@Composable
private fun GuardianRecordProgressIndicator(
    completedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    if (totalCount >= 9) {
        GuardianRecordContinuousProgressBar(
            completedCount = completedCount,
            totalCount = totalCount,
            modifier = modifier,
        )
    } else {
        GuardianRecordSegmentedProgressBar(
            completedCount = completedCount,
            totalCount = totalCount,
            modifier = modifier,
        )
    }
}

@Composable
private fun GuardianRecordSegmentedProgressBar(
    completedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    val segmentCount = totalCount.coerceAtLeast(1)
    val completedSegments = completedCount.coerceIn(0, segmentCount)
    Row(
        modifier = modifier.heightIn(min = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall),
    ) {
        repeat(segmentCount) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        if (index < completedSegments) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    ),
            )
        }
    }
}

@Composable
private fun GuardianRecordContinuousProgressBar(
    completedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    val progress = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()
    Box(
        modifier = modifier
            .heightIn(min = 20.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun GuardianRecordsDetail(state: GuardianModeUiState) {
    val summary = state.selectedRecordSummary
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium),
    ) {
        Text(
            text = fullRecordDateText(summary.date),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = SteppieTheme.typography.guardianTitle,
        )
        if (!summary.hasRecords) {
            Text(
                text = stringResource(R.string.guardian_records_empty_date),
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.guardianBody,
            )
        } else {
            val summaryText = stringResource(
                R.string.guardian_records_summary,
                summary.completedCount,
                summary.totalCount,
                summary.completionPercent,
            )
            val summaryDescription = stringResource(
                R.string.a11y_guardian_records_summary,
                summary.completedCount,
                summary.totalCount,
                summary.completionPercent,
            )
            Text(
                text = summaryText,
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.guardianBody,
                modifier = Modifier.semantics {
                    contentDescription = summaryDescription
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Small)) {
                GuardianRecordStatCard(
                    label = stringResource(R.string.guardian_records_completed_count),
                    value = summary.completedCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                GuardianRecordStatCard(
                    label = stringResource(R.string.guardian_records_total_count),
                    value = summary.totalCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                GuardianRecordStatCard(
                    label = stringResource(R.string.guardian_records_completion_rate),
                    value = stringResource(R.string.guardian_records_percent_value, summary.completionPercent),
                    modifier = Modifier.weight(1f),
                )
            }
            GuardianRecordProgressIndicator(
                completedCount = summary.completedCount,
                totalCount = summary.totalCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
            )
            Text(
                text = stringResource(R.string.guardian_records_routine_status_title),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = SteppieTheme.typography.guardianTitle,
            )
            state.selectedRecordRoutines.forEach { routine ->
                GuardianRecordRoutineRow(routine)
            }
        }
    }
}

@Composable
private fun GuardianRecordStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(SteppieCornerRadius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(SteppieStroke.Divider, MaterialTheme.colorScheme.outline, RoundedCornerShape(SteppieCornerRadius.Card))
            .padding(SteppieSpacing.Medium),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = SteppieTheme.typography.guardianBody,
            maxLines = 2,
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = SteppieTheme.typography.guardianTitle,
            maxLines = 1,
        )
    }
}

@Composable
private fun GuardianRecordRoutineRow(routine: GuardianRecordRoutine) {
    val title = routine.title ?: stringResource(R.string.guardian_records_deleted_routine)
    val status = stringResource(if (routine.isCompleted) R.string.guardian_records_completed else R.string.guardian_records_not_completed)
    val completedTime = routine.completedAt?.let(::recordCompletedTimeText)
    val statusDetail = if (routine.isCompleted && completedTime != null) {
        stringResource(R.string.guardian_records_completed_at, completedTime)
    } else {
        status
    }
    val lifecycle = when {
        routine.isMissing || routine.isDeleted -> stringResource(R.string.guardian_records_deleted_routine)
        routine.isInactive -> stringResource(R.string.guardian_records_inactive_routine)
        else -> null
    }
    val description = if (lifecycle == null) {
        stringResource(R.string.a11y_guardian_record_routine, title, status)
    } else {
        stringResource(R.string.a11y_guardian_record_routine_with_lifecycle, title, status, lifecycle)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(SteppieCornerRadius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(SteppieStroke.Divider, MaterialTheme.colorScheme.outline, RoundedCornerShape(SteppieCornerRadius.Card))
            .clearAndSetSemantics {
                contentDescription = description
            }
            .padding(horizontal = SteppieSpacing.Medium, vertical = SteppieSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (routine.isCompleted) SteppieTheme.colors.progressComplete else Color.Transparent)
                .border(
                    3.dp,
                    if (routine.isCompleted) SteppieTheme.colors.progressComplete else MaterialTheme.colorScheme.onSurfaceVariant,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (routine.isCompleted) {
                Text(
                    text = "✓",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = SteppieTheme.typography.button,
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.button,
                maxLines = 2,
            )
            Text(
                text = listOfNotNull(statusDetail, lifecycle).joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = SteppieTheme.typography.guardianCaption,
            )
        }
        Text(
            text = status,
            color = if (routine.isCompleted) SteppieTheme.colors.progressComplete else MaterialTheme.colorScheme.onSurface,
            style = SteppieTheme.typography.guardianBody,
        )
    }
}

@Composable
private fun recordDayStatusText(day: GuardianRecordDay): String = when {
    !day.hasRecords -> stringResource(R.string.guardian_records_no_record)
    day.remainingCount == 0 -> stringResource(R.string.guardian_records_all_done)
    else -> stringResource(R.string.guardian_records_remaining_count, day.remainingCount)
}

private fun weekdayText(date: LocalDate): String = date.dayOfWeek
    .getDisplayName(TextStyle.SHORT, Locale.getDefault())

private fun recordDateText(date: LocalDate): String = "${date.monthValue}/${date.dayOfMonth}"

private fun fullRecordDateText(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.getDefault()))

private fun recordCompletedTimeText(completedAt: java.time.Instant): String =
    completedAt.atZone(ZoneId.systemDefault()).format(
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()),
    )

@Composable
private fun GuardianSecurityScreen(
    onOpenHome: () -> Unit,
    onOpenPinChange: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onShowOutOfScopeNotice: () -> Unit,
) {
    GuardianScaffold(
        title = stringResource(R.string.guardian_security_title),
        subtitle = stringResource(R.string.guardian_security_subtitle),
        onBack = onOpenHome,
    ) {
        GuardianMenuCard(R.drawable.ic_guardian_security_warning, stringResource(R.string.guardian_pin_change), stringResource(R.string.guardian_pin_change_desc), onOpenPinChange)
        GuardianMenuCard(R.drawable.ic_guardian_security_warning, stringResource(R.string.guardian_recovery_code), stringResource(R.string.guardian_recovery_code_desc), onShowOutOfScopeNotice)
        GuardianMenuCard(R.drawable.ic_guardian_security_backup, stringResource(R.string.guardian_backup), stringResource(R.string.guardian_backup_desc), onOpenBackupRestore)
        GuardianPrivacyNote()
    }
}

@Composable
private fun GuardianBackupRestoreScreen(
    state: GuardianModeUiState,
    onOpenSecurity: () -> Unit,
    onCreateBackupFile: () -> Unit,
    onOpenRestoreFile: () -> Unit,
) {
    GuardianScaffold(
        title = stringResource(R.string.guardian_backup_title),
        subtitle = stringResource(R.string.guardian_backup_subtitle),
        onBack = onOpenSecurity,
    ) {
        WarningMessage(stringResource(R.string.guardian_backup_privacy_notice))
        SteppieButton(
            label = stringResource(R.string.guardian_backup_create),
            onClick = onCreateBackupFile,
            modifier = Modifier.fillMaxWidth(),
            state = if (state.backupInProgress) SteppieButtonState.Loading else SteppieButtonState.Enabled,
        )
        SteppieButton(
            label = stringResource(R.string.guardian_restore_select),
            onClick = onOpenRestoreFile,
            modifier = Modifier.fillMaxWidth(),
            style = SteppieButtonStyle.Secondary,
            state = if (state.backupInProgress) SteppieButtonState.Loading else SteppieButtonState.Enabled,
        )
        WarningMessage(stringResource(R.string.guardian_restore_replace_warning))
        state.backupMessage?.let { SuccessMessage(it) }
        state.backupError?.takeIf { state.pendingRestorePreview == null }?.let { ErrorMessage(it) }
    }
}

@Composable
private fun GuardianScaffold(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    topActionLabel: String? = null,
    onTopAction: (() -> Unit)? = null,
    bottom: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SteppieLayout.GuardianScreenPadding),
        verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium),
    ) {
        GuardianTopBar(
            title = title,
            subtitle = subtitle,
            onBack = onBack,
            topActionLabel = topActionLabel,
            onTopAction = onTopAction,
        )
        Column(verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small), content = content)
        Spacer(Modifier.height(SteppieSpacing.Large))
        bottom?.invoke()
    }
}

@Composable
private fun GuardianTopBar(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    topActionLabel: String? = null,
    onTopAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SteppieSpacing.Medium, bottom = SteppieSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall),
    ) {
        if (onBack != null) {
            Text(
                text = stringResource(R.string.action_back),
                modifier = Modifier
                    .heightIn(min = SteppieLayout.GuardianMinimumTouchTarget)
                    .clickable(role = Role.Button, onClick = onBack),
                color = MaterialTheme.colorScheme.primary,
                style = SteppieTheme.typography.button,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = SteppieTheme.typography.guardianTitle,
            )
            if (topActionLabel != null && onTopAction != null) {
                Text(
                    text = topActionLabel,
                    modifier = Modifier
                        .heightIn(min = SteppieLayout.GuardianMinimumTouchTarget)
                        .clickable(role = Role.Button, onClick = onTopAction)
                        .padding(horizontal = SteppieSpacing.ExtraSmall),
                    color = MaterialTheme.colorScheme.primary,
                    style = SteppieTheme.typography.button,
                )
            }
        }
        Text(subtitle, color = MaterialTheme.colorScheme.onSurface, style = SteppieTheme.typography.guardianCaption)
    }
}

@Composable
private fun GuardianMenuCard(
    @DrawableRes icon: Int,
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    val description = stringResource(R.string.a11y_guardian_menu_card, title, body)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(SteppieCornerRadius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(SteppieStroke.Divider, MaterialTheme.colorScheme.outline, RoundedCornerShape(SteppieCornerRadius.Card))
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                semanticOnClick {
                    onClick()
                    true
                }
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(SteppieSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(SteppieCornerRadius.Control))
                .background(Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall),
        ) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = SteppieTheme.typography.guardianSection)
            Text(body, color = MaterialTheme.colorScheme.onSurface, style = SteppieTheme.typography.guardianCaption)
        }
    }
}

@Composable
private fun EditableRoutineRow(
    routine: Routine,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMove: (Int) -> Unit,
) {
    val maxRevealPx = with(LocalDensity.current) { 76.dp.toPx() }
    var swipeOffsetPx by remember(routine.id) { mutableFloatStateOf(0f) }
    val routineTitle = routine.title.resolve(null, Locale.getDefault().toLanguageTag())
    val routineMeta = routine.scheduledTime?.toString() ?: stringResource(R.string.guardian_time_none)
    val rowDescription = stringResource(R.string.a11y_routine_edit_row, routineTitle, routineMeta)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 92.dp),
    ) {
        if (swipeOffsetPx < -1f) {
            SwipeDeleteAction(
                label = stringResource(R.string.a11y_item_action, routineTitle, stringResource(R.string.action_delete)),
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = SteppieSpacing.ExtraSmall),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 92.dp)
                .graphicsLayer { translationX = swipeOffsetPx }
                .clip(RoundedCornerShape(SteppieCornerRadius.Card))
                .background(MaterialTheme.colorScheme.surface)
                .border(SteppieStroke.Divider, MaterialTheme.colorScheme.outline, RoundedCornerShape(SteppieCornerRadius.Card))
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        swipeOffsetPx = (swipeOffsetPx + delta).coerceIn(-maxRevealPx, 0f)
                    },
                    onDragStopped = {
                        swipeOffsetPx = if (swipeOffsetPx <= -maxRevealPx / 2f) -maxRevealPx else 0f
                    },
                )
                .semantics {
                    contentDescription = rowDescription
                }
                .clickable(role = Role.Button, onClick = onClick)
                .padding(SteppieSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
        ) {
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colorForToken(routine.colorToken)),
            )
            Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                RoutineIcon(routine.icon, focus = false, modifier = Modifier.size(48.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall)) {
                Text(
                    text = routineTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = SteppieTheme.typography.button,
                    maxLines = 2,
                )
                Text(
                    text = routineMeta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = SteppieTheme.typography.guardianCaption,
                )
            }
            DragHandle(
                onMove = onMove,
                contentDescription = stringResource(
                    R.string.a11y_item_action,
                    routineTitle,
                    stringResource(R.string.a11y_reorder_routine),
                ),
                modifier = Modifier.testTag("routine_drag_${routine.id}"),
            )
        }
    }
}

@Composable
private fun SwipeDeleteAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visualLabel = stringResource(R.string.action_delete)
    Column(
        modifier = modifier
            .width(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
                .clearAndSetSemantics {
                    contentDescription = label
                    role = Role.Button
                    semanticOnClick {
                        onClick()
                        true
                    }
                }
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_guardian_delete_trash),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(SteppieSpacing.TwoExtraSmall))
        Text(
            text = visualLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = SteppieTheme.typography.guardianCaption,
        )
    }
}

@Composable
private fun DragHandle(
    onMove: (Int) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val thresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(RoundedCornerShape(SteppieCornerRadius.Control))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clearAndSetSemantics { this.contentDescription = contentDescription }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    dragDistance += delta
                    when {
                        dragDistance <= -thresholdPx -> {
                            onMove(-1)
                            dragDistance = 0f
                        }
                        dragDistance >= thresholdPx -> {
                            onMove(1)
                            dragDistance = 0f
                        }
                    }
                },
                onDragStopped = { dragDistance = 0f },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text("☰", color = MaterialTheme.colorScheme.onSurfaceVariant, style = SteppieTheme.typography.guardianSection)
    }
}

@Composable
private fun IconPicker(selectedIcon: String, onSelected: (String) -> Unit) {
    GuardianPanel(title = stringResource(R.string.guardian_icon_picker_title), body = stringResource(R.string.guardian_icon_picker_body)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.ExtraSmall)) {
            items(BuiltinIconNames.all.toList()) { iconName ->
                val selectionState = stringResource(
                    if (iconName == selectedIcon) R.string.a11y_selected else R.string.a11y_not_selected,
                )
                val description = stringResource(R.string.a11y_icon_option, iconName, selectionState)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(SteppieCornerRadius.Control))
                        .border(
                            if (iconName == selectedIcon) SteppieStroke.Focus else SteppieStroke.Divider,
                            if (iconName == selectedIcon) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(SteppieCornerRadius.Control),
                        )
                        .clearAndSetSemantics {
                            contentDescription = description
                            stateDescription = selectionState
                            role = Role.Button
                            semanticOnClick {
                                onSelected(iconName)
                                true
                            }
                        }
                        .clickable(role = Role.Button) { onSelected(iconName) }
                        .padding(SteppieSpacing.ExtraSmall),
                    contentAlignment = Alignment.Center,
                ) {
                    RoutineIcon(IconRef.Builtin(iconName), focus = false, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun ColorPicker(selectedColorToken: String, onSelected: (String) -> Unit) {
    GuardianPanel(title = stringResource(R.string.guardian_color_picker_title)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.ExtraSmall)) {
            items(RoutineColorTokens.all.toList()) { colorToken ->
                val selectionState = stringResource(
                    if (selectedColorToken == colorToken) R.string.a11y_selected else R.string.a11y_not_selected,
                )
                val colorLabel = colorAccessibilityLabel(colorToken)
                val description = stringResource(
                    R.string.a11y_color_option,
                    colorLabel,
                    selectionState,
                )
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .border(
                            if (selectedColorToken == colorToken) SteppieStroke.Focus else SteppieStroke.Divider,
                            if (selectedColorToken == colorToken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            CircleShape,
                        )
                        .clearAndSetSemantics {
                            contentDescription = description
                            stateDescription = selectionState
                            role = Role.Button
                            semanticOnClick {
                                onSelected(colorToken)
                                true
                            }
                        }
                        .clickable(role = Role.Button) { onSelected(colorToken) }
                        .padding(7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(colorForToken(colorToken)),
                    )
                }
            }
        }
    }
}

@Composable
private fun GuardianPanel(
    title: String,
    body: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SteppieCornerRadius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(SteppieStroke.Divider, MaterialTheme.colorScheme.outline, RoundedCornerShape(SteppieCornerRadius.Card))
            .padding(SteppieSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(SteppieSpacing.ExtraSmall),
    ) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = SteppieTheme.typography.guardianSection)
        body?.let { Text(it, color = MaterialTheme.colorScheme.onSurface, style = SteppieTheme.typography.guardianCaption) }
        content()
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Text(
        text = "!  $message",
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SteppieCornerRadius.Control))
            .background(SteppieTheme.colors.cardRose)
            .border(SteppieStroke.Divider, MaterialTheme.colorScheme.error, RoundedCornerShape(SteppieCornerRadius.Control))
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(SteppieSpacing.Medium),
        color = MaterialTheme.colorScheme.error,
        style = SteppieTheme.typography.guardianCaption,
    )
}

@Composable
private fun WarningMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SteppieCornerRadius.Control))
            .background(SteppieTheme.colors.cardLemon)
            .border(SteppieStroke.Divider, SteppieTheme.colors.warning, RoundedCornerShape(SteppieCornerRadius.Control))
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(SteppieSpacing.Medium),
        color = MaterialTheme.colorScheme.onSurface,
        style = SteppieTheme.typography.guardianCaption,
    )
}

@Composable
private fun SuccessMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SteppieCornerRadius.Control))
            .background(SteppieTheme.colors.cardMint)
            .border(SteppieStroke.Divider, SteppieTheme.colors.success, RoundedCornerShape(SteppieCornerRadius.Control))
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(SteppieSpacing.Medium),
        color = MaterialTheme.colorScheme.onSurface,
        style = SteppieTheme.typography.guardianCaption,
    )
}

@Composable
private fun GuardianPrivacyNote() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 148.dp)
            .clip(RoundedCornerShape(SteppieCornerRadius.Card))
            .background(SteppieTheme.colors.cardLemon)
            .border(SteppieStroke.Divider, SteppieTheme.colors.warning, RoundedCornerShape(SteppieCornerRadius.Card))
            .padding(SteppieSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.guardian_privacy_title),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = SteppieTheme.typography.guardianSection,
        )
        Text(
            text = stringResource(R.string.guardian_privacy_body),
            color = MaterialTheme.colorScheme.onSurface,
            style = SteppieTheme.typography.guardianCaption,
        )
    }
}

@Composable
private fun colorAccessibilityLabel(token: String): String = when (token) {
    "color.card.mint" -> stringResource(R.string.a11y_color_mint)
    "color.card.lemon" -> stringResource(R.string.a11y_color_lemon)
    "color.card.peach" -> stringResource(R.string.a11y_color_peach)
    "color.card.lavender" -> stringResource(R.string.a11y_color_lavender)
    "color.card.rose" -> stringResource(R.string.a11y_color_rose)
    else -> stringResource(R.string.a11y_color_sky)
}

@Composable
private fun colorForToken(token: String): Color = when (token) {
    "color.card.mint" -> SteppieTheme.colors.cardMint
    "color.card.lemon" -> SteppieTheme.colors.cardLemon
    "color.card.peach" -> SteppieTheme.colors.cardPeach
    "color.card.lavender" -> SteppieTheme.colors.cardLavender
    "color.card.rose" -> SteppieTheme.colors.cardRose
    else -> SteppieTheme.colors.cardSky
}
