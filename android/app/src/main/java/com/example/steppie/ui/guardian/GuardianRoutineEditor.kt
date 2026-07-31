package com.example.steppie.ui.guardian

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.steppie.R
import com.example.steppie.domain.model.BuiltinIconNames
import com.example.steppie.domain.model.IconRef
import com.example.steppie.domain.model.RoutineColorTokens
import com.example.steppie.presentation.formatting.formatLocalizedTime
import com.example.steppie.ui.child.RoutineIcon
import com.example.steppie.ui.components.SteppieButton
import com.example.steppie.ui.components.SteppieButtonStyle
import com.example.steppie.ui.theme.SteppieCornerRadius
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.theme.SteppieStroke
import com.example.steppie.ui.theme.SteppieTheme
import java.time.LocalTime
import java.util.Locale

@Composable
internal fun GuardianCardEditScreen(
    state: GuardianModeUiState,
    onOpenRoutineEdit: () -> Unit,
    onRequestDelete: (String) -> Unit,
    onDraftTitleChange: (String) -> Unit,
    onDraftIconChange: (String) -> Unit,
    onDraftPhotoPick: () -> Unit,
    onDraftCameraCapture: () -> Unit,
    onDraftPhotoRemove: () -> Unit,
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
        IconPicker(
            selectedIcon = draft.icon,
            onSelected = onDraftIconChange,
            onPhotoPick = onDraftPhotoPick,
            onCameraCapture = onDraftCameraCapture,
            onPhotoRemove = onDraftPhotoRemove,
            showPhotoRemoveAction = true,
        )
        ColorPicker(selectedColorToken = draft.colorToken, onSelected = onDraftColorChange)
        ScheduledTimePicker(
            scheduledTime = draft.scheduledTime,
            onScheduledTimeChange = onDraftScheduledTimeChange,
        )
        WarningMessage(stringResource(R.string.guardian_unsaved_warning))
        state.draftError?.let { ErrorMessage(it) }
    }
}

@Composable
internal fun ScheduledTimePicker(
    scheduledTime: String,
    onScheduledTimeChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val selectedTime = remember(scheduledTime) { scheduledTime.toLocalTimeOrNull() }
    val hasScheduledTime = scheduledTime.isNotBlank()
    val fallbackTime = selectedTime ?: LocalTime.NOON
    val normalizedTime = selectedTime?.toStorageString().orEmpty()
    val selectedText = normalizedTime.ifBlank { stringResource(R.string.guardian_time_none) }
    val displayText = selectedTime?.let { formatLocalizedTime(it, Locale.getDefault()) }
        ?: scheduledTime.ifBlank { stringResource(R.string.guardian_time_none) }

    fun showTimePicker() {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                onScheduledTimeChange(LocalTime.of(hour, minute).toStorageString())
            },
            fallbackTime.hour,
            fallbackTime.minute,
            false,
        ).show()
    }

    GuardianPanel(title = stringResource(R.string.guardian_field_time)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
        ) {
            ScheduledTimeSegment(
                label = stringResource(R.string.guardian_time_set),
                selected = hasScheduledTime,
                onClick = {
                    if (!hasScheduledTime) onScheduledTimeChange(LocalTime.NOON.toStorageString())
                    showTimePicker()
                },
                modifier = Modifier.weight(1f),
            )
            ScheduledTimeSegment(
                label = stringResource(R.string.guardian_time_none_short),
                selected = !hasScheduledTime,
                onClick = { onScheduledTimeChange("") },
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = stringResource(R.string.guardian_time_current_selection, selectedText),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = SteppieTheme.typography.guardianBody,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.guardian_field_time),
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.guardianSection,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(SteppieCornerRadius.Control))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .heightIn(min = 52.dp)
                    .clickable(
                        enabled = hasScheduledTime,
                        role = Role.Button,
                        onClick = ::showTimePicker,
                    )
                    .semantics {
                        contentDescription = displayText
                        role = Role.Button
                    }
                    .padding(horizontal = SteppieSpacing.Medium, vertical = SteppieSpacing.ExtraSmall),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = displayText,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = SteppieTheme.typography.guardianBody,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ScheduledTimeSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectionState = stringResource(if (selected) R.string.a11y_selected else R.string.a11y_not_selected)
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(SteppieCornerRadius.Control))
            .background(containerColor)
            .heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = label
                stateDescription = selectionState
                role = Role.Button
            }
            .padding(horizontal = SteppieSpacing.Medium, vertical = SteppieSpacing.Small),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = contentColor,
            style = SteppieTheme.typography.guardianBody,
            textAlign = TextAlign.Center,
        )
    }
}

private fun String.toLocalTimeOrNull(): LocalTime? =
    runCatching { LocalTime.parse(this) }.getOrNull()
        ?.takeIf { it.second == 0 && it.nano == 0 }

internal fun LocalTime.toStorageString(): String =
    String.format(Locale.US, "%02d:%02d", hour, minute)

@Composable
internal fun IconPicker(
    selectedIcon: IconRef,
    onSelected: (String) -> Unit,
    onPhotoPick: () -> Unit,
    onCameraCapture: () -> Unit,
    onPhotoRemove: () -> Unit,
    showPhotoRemoveAction: Boolean = true,
) {
    GuardianPanel(title = stringResource(R.string.guardian_icon_picker_title), body = stringResource(R.string.guardian_icon_picker_body)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(SteppieCornerRadius.Control))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        SteppieStroke.Divider,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(SteppieCornerRadius.Control),
                    )
                    .padding(SteppieSpacing.ExtraSmall),
                contentAlignment = Alignment.Center,
            ) {
                RoutineIcon(selectedIcon, focus = false, modifier = Modifier.fillMaxSize())
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SteppieSpacing.ExtraSmall),
            ) {
                SteppieButton(
                    label = stringResource(R.string.guardian_photo_pick),
                    onClick = onPhotoPick,
                    modifier = Modifier.fillMaxWidth(),
                    style = SteppieButtonStyle.Secondary,
                )
                SteppieButton(
                    label = stringResource(R.string.guardian_photo_camera),
                    onClick = onCameraCapture,
                    modifier = Modifier.fillMaxWidth(),
                    style = SteppieButtonStyle.Secondary,
                )
                if (showPhotoRemoveAction && selectedIcon is IconRef.Photo) {
                    SteppieButton(
                        label = stringResource(R.string.guardian_photo_remove),
                        onClick = onPhotoRemove,
                        modifier = Modifier.fillMaxWidth(),
                        style = SteppieButtonStyle.Secondary,
                        contentColorOverride = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.ExtraSmall)) {
            val selectedBuiltin = (selectedIcon as? IconRef.Builtin)?.name
            items(BuiltinIconNames.all.toList()) { iconName ->
                val selectionState = stringResource(
                    if (iconName == selectedBuiltin) R.string.a11y_selected else R.string.a11y_not_selected,
                )
                val description = stringResource(R.string.a11y_icon_option, iconName, selectionState)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(SteppieCornerRadius.Control))
                        .border(
                            if (iconName == selectedBuiltin) SteppieStroke.Focus else SteppieStroke.Divider,
                            if (iconName == selectedBuiltin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
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
internal fun ColorPicker(selectedColorToken: String, onSelected: (String) -> Unit) {
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
private fun colorAccessibilityLabel(token: String): String = when (token) {
    "color.card.mint" -> stringResource(R.string.a11y_color_mint)
    "color.card.lemon" -> stringResource(R.string.a11y_color_lemon)
    "color.card.peach" -> stringResource(R.string.a11y_color_peach)
    "color.card.lavender" -> stringResource(R.string.a11y_color_lavender)
    "color.card.rose" -> stringResource(R.string.a11y_color_rose)
    else -> stringResource(R.string.a11y_color_sky)
}

@Composable
internal fun colorForToken(token: String): Color = when (token) {
    "color.card.mint" -> SteppieTheme.colors.cardMint
    "color.card.lemon" -> SteppieTheme.colors.cardLemon
    "color.card.peach" -> SteppieTheme.colors.cardPeach
    "color.card.lavender" -> SteppieTheme.colors.cardLavender
    "color.card.rose" -> SteppieTheme.colors.cardRose
    else -> SteppieTheme.colors.cardSky
}
