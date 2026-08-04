package com.example.steppie.ui.guardian

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick as semanticOnClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.steppie.R
import com.example.steppie.ui.theme.SteppieCornerRadius
import com.example.steppie.ui.theme.SteppieLayout
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.theme.SteppieStroke
import com.example.steppie.ui.theme.SteppieTheme
import com.example.steppie.ui.tutorial.TutorialTarget
import com.example.steppie.ui.tutorial.tutorialAnchor

@Composable
internal fun GuardianScaffold(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    topActionLabel: String? = null,
    @DrawableRes topActionIcon: Int? = null,
    topActionContentDescription: String? = null,
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
            topActionIcon = topActionIcon,
            topActionContentDescription = topActionContentDescription,
            onTopAction = onTopAction,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .tutorialAnchor(TutorialTarget.GuardianMain),
            verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Small),
            content = content,
        )
        Spacer(Modifier.height(SteppieSpacing.Large))
        if (bottom != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .tutorialAnchor(TutorialTarget.GuardianBottom),
            ) { bottom() }
        }
    }
}

@Composable
internal fun GuardianTopBar(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    topActionLabel: String? = null,
    @DrawableRes topActionIcon: Int? = null,
    topActionContentDescription: String? = null,
    onTopAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SteppieSpacing.Medium, bottom = SteppieSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall),
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(SteppieLayout.GuardianMinimumTouchTarget),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.action_back),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
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
            } else if (topActionIcon != null && topActionContentDescription != null && onTopAction != null) {
                IconButton(
                    onClick = onTopAction,
                    modifier = Modifier.size(SteppieLayout.GuardianMinimumTouchTarget),
                ) {
                    Icon(
                        painter = painterResource(topActionIcon),
                        contentDescription = topActionContentDescription,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
        Text(subtitle, color = MaterialTheme.colorScheme.onSurface, style = SteppieTheme.typography.guardianCaption)
    }
}

@Composable
internal fun GuardianMenuCard(
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
internal fun GuardianPanel(
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
internal fun ErrorMessage(message: String) {
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
internal fun WarningMessage(message: String) {
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
