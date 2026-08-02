package com.example.steppie.ui.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick as semanticOnClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.steppie.R
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.FeedbackIntensity
import com.example.steppie.presentation.environment.currentPresentationLocale
import com.example.steppie.ui.components.SteppieButton
import com.example.steppie.ui.theme.SteppieCornerRadius
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.theme.SteppieStroke
import com.example.steppie.ui.theme.SteppieTheme

@Composable
internal fun GuardianEnvironmentSettingsScreen(
    settings: AppSettings,
    useWideLayout: Boolean,
    onNavigateBack: () -> Unit,
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
    onReplayTutorials: () -> Unit,
) {
    GuardianScaffold(
        title = stringResource(R.string.guardian_environment_title),
        subtitle = stringResource(R.string.guardian_environment_subtitle),
        onBack = onNavigateBack,
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
                    valueLabel = String.format(currentPresentationLocale(), "%.1fx", settings.ttsRate),
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
            SettingBlock(title = stringResource(R.string.tutorial_replay_title)) {
                Text(
                    text = stringResource(R.string.tutorial_replay_body),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = SteppieTheme.typography.guardianBody,
                )
                SteppieButton(
                    label = stringResource(R.string.tutorial_replay_title),
                    onClick = onReplayTutorials,
                    modifier = Modifier.fillMaxWidth(),
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
