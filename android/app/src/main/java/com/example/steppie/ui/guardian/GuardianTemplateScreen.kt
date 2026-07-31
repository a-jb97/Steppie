package com.example.steppie.ui.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.steppie.R
import com.example.steppie.domain.model.IconRef
import com.example.steppie.ui.child.RoutineIcon
import com.example.steppie.ui.components.SteppieButton
import com.example.steppie.ui.components.SteppieButtonStyle
import com.example.steppie.ui.theme.SteppieCornerRadius
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.theme.SteppieStroke
import com.example.steppie.ui.theme.SteppieTheme
import java.util.Locale

@Composable
internal fun GuardianTemplateSelectScreen(
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
                style = SteppieTheme.typography.guardianTitle.copy(
                    fontSize = SteppieTheme.typography.guardianTitle.fontSize * 0.7f,
                    lineHeight = SteppieTheme.typography.guardianTitle.lineHeight * 0.7f,
                ),
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
