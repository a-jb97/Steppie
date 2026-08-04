package com.example.steppie.ui.guardian

import android.widget.NumberPicker
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick as semanticOnClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.steppie.R
import com.example.steppie.presentation.environment.currentPresentationLocale
import com.example.steppie.presentation.environment.currentPresentationZoneId
import com.example.steppie.presentation.formatting.formatCalendarMonth
import com.example.steppie.presentation.formatting.formatCompletedTime
import com.example.steppie.presentation.formatting.formatFullRecordDate
import com.example.steppie.presentation.formatting.formatRecordDate
import com.example.steppie.presentation.formatting.formatWeekday
import com.example.steppie.presentation.formatting.orderedWeekdays
import com.example.steppie.ui.components.SteppieButton
import com.example.steppie.ui.components.SteppieButtonStyle
import com.example.steppie.ui.theme.SteppieCornerRadius
import com.example.steppie.ui.theme.SteppieLayout
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.theme.SteppieStroke
import com.example.steppie.ui.theme.SteppieTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@Composable
internal fun GuardianRecordsScreen(
    state: GuardianModeUiState,
    useWideLayout: Boolean,
    onNavigateBack: () -> Unit,
    onSelectRecordsDate: (LocalDate) -> Unit,
    onOpenRecordsCalendar: () -> Unit,
) {
    GuardianScaffold(
        title = stringResource(R.string.guardian_records_title),
        subtitle = stringResource(R.string.guardian_records_subtitle),
        onBack = onNavigateBack,
        topActionIcon = R.drawable.ic_guardian_calendar,
        topActionContentDescription = stringResource(R.string.a11y_guardian_records_calendar),
        onTopAction = onOpenRecordsCalendar,
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
                    GuardianRecordsDetail(
                        summary = state.selectedRecordSummary,
                        routines = state.selectedRecordRoutines,
                    )
                }
            }
        } else {
            GuardianRecordsDayList(state, onSelectRecordsDate)
            GuardianRecordsDetail(
                summary = state.selectedRecordSummary,
                routines = state.selectedRecordRoutines,
            )
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
    val dateText = formatRecordDate(day.date)
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
                text = formatWeekday(day.date, currentPresentationLocale()),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = SteppieTheme.typography.guardianSection,
            )
            Text(
                text = formatRecordDate(day.date),
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
internal fun GuardianRecordsCalendarScreen(
    state: GuardianModeUiState,
    useWideLayout: Boolean,
    onNavigateBack: () -> Unit,
    onSelectRecordsCalendarDate: (LocalDate) -> Unit,
    onMoveRecordsCalendarMonth: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SteppieLayout.GuardianScreenPadding),
    ) {
        GuardianTopBar(
            title = stringResource(R.string.guardian_records_calendar_title),
            subtitle = stringResource(R.string.guardian_records_calendar_subtitle),
            onBack = onNavigateBack,
        )
        if (useWideLayout) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Large),
            ) {
                GuardianRecordsCalendarPanel(
                    state = state,
                    onSelectDate = onSelectRecordsCalendarDate,
                    onMoveMonth = onMoveRecordsCalendarMonth,
                    modifier = Modifier.weight(1f),
                )
                GuardianRecordsDetail(
                    summary = state.selectedCalendarRecordSummary,
                    routines = state.selectedCalendarRecordRoutines,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            GuardianRecordsCalendarPanel(
                state = state,
                onSelectDate = onSelectRecordsCalendarDate,
                onMoveMonth = onMoveRecordsCalendarMonth,
            )
            Spacer(Modifier.height(SteppieSpacing.ExtraLarge + SteppieSpacing.Medium))
            GuardianRecordsDetail(
                summary = state.selectedCalendarRecordSummary,
                routines = state.selectedCalendarRecordRoutines,
            )
        }
        Spacer(Modifier.height(SteppieSpacing.Large))
    }
}
@Composable
private fun GuardianRecordsCalendarPanel(
    state: GuardianModeUiState,
    onSelectDate: (LocalDate) -> Unit,
    onMoveMonth: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var monthPickerOpen by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SteppieCornerRadius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(SteppieStroke.Divider, MaterialTheme.colorScheme.outline, RoundedCornerShape(SteppieCornerRadius.Card))
            .padding(SteppieSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium),
    ) {
        GuardianRecordsCalendarHeader(
            month = state.recordsCalendarMonth,
            onPreviousMonth = { onMoveMonth(-1L) },
            onNextMonth = { onMoveMonth(1L) },
            onOpenMonthPicker = { monthPickerOpen = true },
        )
        GuardianRecordsCalendarGrid(
            month = state.recordsCalendarMonth,
            recordDates = state.calendarRecordDates,
            selectedDate = state.selectedCalendarRecordsDate,
            onSelectDate = onSelectDate,
        )
    }
    if (monthPickerOpen) {
        GuardianRecordsMonthPickerDialog(
            currentMonth = state.recordsCalendarMonth,
            recordDates = state.calendarRecordDates,
            onDismiss = { monthPickerOpen = false },
            onConfirm = { selectedMonth ->
                val delta = ChronoUnit.MONTHS.between(state.recordsCalendarMonth, selectedMonth)
                if (delta != 0L) onMoveMonth(delta)
                monthPickerOpen = false
            },
        )
    }
}

@Composable
private fun GuardianRecordsCalendarHeader(
    month: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenMonthPicker: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarMonthIconButton(
            icon = R.drawable.ic_chevron_left,
            description = stringResource(R.string.guardian_records_previous_month),
            onClick = onPreviousMonth,
        )
        Box(
            modifier = Modifier
                .height(SteppieLayout.GuardianMinimumTouchTarget)
                .clip(RoundedCornerShape(SteppieCornerRadius.Control))
                .clickable(role = Role.Button, onClick = onOpenMonthPicker)
                .padding(horizontal = SteppieSpacing.ExtraSmall, vertical = SteppieSpacing.TwoExtraSmall),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = formatCalendarMonth(month, currentPresentationLocale()),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = SteppieTheme.typography.guardianSection,
                textAlign = TextAlign.Center,
            )
        }
        CalendarMonthIconButton(
            icon = R.drawable.ic_chevron_right,
            description = stringResource(R.string.guardian_records_next_month),
            onClick = onNextMonth,
        )
    }
}

@Composable
private fun CalendarMonthIconButton(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(SteppieLayout.GuardianMinimumTouchTarget),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun GuardianRecordsMonthPickerDialog(
    currentMonth: YearMonth,
    recordDates: Set<LocalDate>,
    onDismiss: () -> Unit,
    onConfirm: (YearMonth) -> Unit,
) {
    val recordYears = recordDates.map(LocalDate::getYear)
    val minYear = ((recordYears.minOrNull() ?: currentMonth.year) - 5).coerceAtLeast(1970)
    val maxYear = (recordYears.maxOrNull() ?: currentMonth.year) + 5
    var selectedYear by remember(currentMonth) { mutableStateOf(currentMonth.year.coerceIn(minYear, maxYear)) }
    var selectedMonth by remember(currentMonth) { mutableStateOf(currentMonth.monthValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.guardian_records_month_picker_title)) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AndroidView(
                    modifier = Modifier.weight(1f),
                    factory = { context ->
                        NumberPicker(context).apply {
                            minValue = minYear
                            maxValue = maxYear
                            value = selectedYear
                            wrapSelectorWheel = false
                            setOnValueChangedListener { _, _, newValue -> selectedYear = newValue }
                        }
                    },
                    update = { picker ->
                        picker.minValue = minYear
                        picker.maxValue = maxYear
                        if (picker.value != selectedYear) picker.value = selectedYear
                    },
                )
                AndroidView(
                    modifier = Modifier.weight(1f),
                    factory = { context ->
                        NumberPicker(context).apply {
                            minValue = 1
                            maxValue = 12
                            value = selectedMonth
                            displayedValues = (1..12).map { it.toString() }.toTypedArray()
                            wrapSelectorWheel = true
                            setOnValueChangedListener { _, _, newValue -> selectedMonth = newValue }
                        }
                    },
                    update = { picker ->
                        if (picker.value != selectedMonth) picker.value = selectedMonth
                    },
                )
            }
        },
        confirmButton = {
            SteppieButton(
                label = stringResource(R.string.action_ok),
                onClick = { onConfirm(YearMonth.of(selectedYear, selectedMonth)) },
            )
        },
        dismissButton = {
            SteppieButton(
                label = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                style = SteppieButtonStyle.Secondary,
            )
        },
    )
}

@Composable
private fun GuardianRecordsCalendarGrid(
    month: YearMonth,
    recordDates: Set<LocalDate>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
) {
    val locale = currentPresentationLocale()
    val weekdays = orderedWeekdays(locale)
    val firstOfMonth = month.atDay(1)
    val leadingBlankCount = weekdays.indexOf(firstOfMonth.dayOfWeek).coerceAtLeast(0)
    val dates = List(leadingBlankCount) { null } + (1..month.lengthOfMonth()).map(month::atDay)
    val rows = dates.chunked(7)
    Column(verticalArrangement = Arrangement.spacedBy(SteppieSpacing.ExtraSmall)) {
        Row(horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.ExtraSmall)) {
            weekdays.forEach { weekday ->
                Text(
                    text = formatWeekday(weekday, locale),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = SteppieTheme.typography.guardianCaption,
                    textAlign = TextAlign.Center,
                )
            }
        }
        rows.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.ExtraSmall)) {
                (week + List(7 - week.size) { null }).forEach { date ->
                    GuardianRecordsCalendarDateCell(
                        date = date,
                        hasRecords = date != null && date in recordDates,
                        selected = date == selectedDate,
                        onClick = if (date != null && date in recordDates) {
                            { onSelectDate(date) }
                        } else {
                            null
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun GuardianRecordsCalendarDateCell(
    date: LocalDate?,
    hasRecords: Boolean,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (date == null) {
        Spacer(modifier.height(58.dp))
        return
    }
    val dateText = date.dayOfMonth.toString()
    val recordState = stringResource(
        if (hasRecords) R.string.guardian_records_calendar_date_has_records else R.string.guardian_records_calendar_date_no_records,
    )
    val selectionState = stringResource(if (selected) R.string.a11y_selected else R.string.a11y_not_selected)
    val cellModifier = modifier
        .testTag("guardian_records_calendar_date_${date}")
        .heightIn(min = 58.dp)
        .clip(RoundedCornerShape(SteppieCornerRadius.Control))
        .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
        .then(
            if (selected) {
                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(SteppieCornerRadius.Control))
            } else {
                Modifier
            },
        )
        .clearAndSetSemantics {
            contentDescription = "$dateText, $recordState"
            stateDescription = selectionState
            if (onClick != null) {
                role = Role.Button
                semanticOnClick {
                    onClick()
                    true
                }
            }
        }
        .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
        .padding(vertical = SteppieSpacing.ExtraSmall)
    Column(
        modifier = cellModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = dateText,
            color = if (hasRecords) MaterialTheme.colorScheme.onSurface else Color.Gray,
            style = SteppieTheme.typography.guardianBody,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(SteppieSpacing.TwoExtraSmall))
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (hasRecords) SteppieTheme.colors.progressComplete else Color.Transparent),
        )
    }
}

@Composable
private fun GuardianRecordsDetail(
    summary: GuardianRecordDay,
    routines: List<GuardianRecordRoutine>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium),
    ) {
        Text(
            text = formatFullRecordDate(summary.date, currentPresentationLocale()),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = SteppieTheme.typography.guardianTitle,
        )
        if (summary.totalCount == 0) {
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
            routines.forEach { routine ->
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
    val completedTime = routine.completedAt?.let {
        formatCompletedTime(it, currentPresentationLocale(), currentPresentationZoneId())
    }
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
