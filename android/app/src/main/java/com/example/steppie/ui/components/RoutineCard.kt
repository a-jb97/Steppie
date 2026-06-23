package com.example.steppie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.steppie.R
import com.example.steppie.ui.theme.SteppieCornerRadius
import com.example.steppie.ui.theme.SteppieLayout
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.theme.SteppieStroke
import com.example.steppie.ui.theme.SteppieTheme

enum class RoutineCardPresentation { Focus, List }

enum class RoutineCardState { Current, Completed, Upcoming }

enum class RoutineCardColor { Sky, Mint, Lemon, Peach, Lavender, Rose }

@Composable
fun RoutineCard(
    title: String,
    state: RoutineCardState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    presentation: RoutineCardPresentation = RoutineCardPresentation.List,
    cardColor: RoutineCardColor = RoutineCardColor.Sky,
    meta: String? = null,
    completedStateDescription: String? = null,
    notCompletedStateDescription: String? = null,
    focusMaxWidth: Dp = SteppieLayout.FocusCardPhoneMaxWidth,
    visual: @Composable BoxScope.() -> Unit = { RoutineVisualPlaceholder(title) },
) {
    val resolvedStateDescription = if (state == RoutineCardState.Completed) {
        completedStateDescription ?: stringResource(R.string.a11y_status_completed)
    } else {
        notCompletedStateDescription ?: stringResource(R.string.a11y_status_not_completed)
    }
    val resolvedMeta = meta ?: when (state) {
        RoutineCardState.Current -> stringResource(R.string.routine_state_current)
        RoutineCardState.Completed -> stringResource(R.string.routine_state_completed)
        RoutineCardState.Upcoming -> stringResource(R.string.routine_state_upcoming)
    }
    val canClick = presentation == RoutineCardPresentation.List || state == RoutineCardState.Current
    val background = routineCardColor(cardColor)

    if (presentation == RoutineCardPresentation.Focus) {
        FocusRoutineCard(
            title = title,
            meta = resolvedMeta,
            state = state,
            stateDescription = resolvedStateDescription,
            onClick = onClick,
            enabled = canClick,
            background = background,
            maxWidth = focusMaxWidth,
            modifier = modifier,
            visual = visual,
        )
    } else {
        ListRoutineCard(
            title = title,
            meta = resolvedMeta,
            state = state,
            stateDescription = resolvedStateDescription,
            onClick = onClick,
            background = background,
            modifier = modifier,
            visual = visual,
        )
    }
}

@Composable
private fun FocusRoutineCard(
    title: String,
    meta: String,
    state: RoutineCardState,
    stateDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    background: Color,
    maxWidth: Dp,
    modifier: Modifier,
    visual: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(SteppieCornerRadius.Card)
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = when {
        isFocused -> MaterialTheme.colorScheme.primary
        state == RoutineCardState.Completed -> SteppieTheme.colors.success
        else -> Color.Transparent
    }
    val borderWidth = when {
        isFocused -> SteppieStroke.Focus
        state == RoutineCardState.Completed -> 2.dp
        else -> 0.dp
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = maxWidth)
            .heightIn(min = 448.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .border(borderWidth, borderColor, shape)
            .clip(shape)
            .background(background)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { this.stateDescription = stateDescription }
            .padding(SteppieSpacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
            content = visual,
        )
        Spacer(Modifier.size(SteppieSpacing.Large))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = SteppieTheme.typography.childCardTitle,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(SteppieSpacing.Large))
        Text(
            text = meta,
            color = if (state == RoutineCardState.Completed) {
                SteppieTheme.colors.success
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            style = SteppieTheme.typography.guardianSection,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ListRoutineCard(
    title: String,
    meta: String,
    state: RoutineCardState,
    stateDescription: String,
    onClick: () -> Unit,
    background: Color,
    modifier: Modifier,
    visual: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(SteppieCornerRadius.Card)
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) {
                    Modifier.border(SteppieStroke.Focus, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { this.stateDescription = stateDescription }
            .padding(horizontal = SteppieSpacing.Medium, vertical = SteppieSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(SteppieCornerRadius.Control))
                .background(MaterialTheme.colorScheme.surface)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
            content = visual,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.childListTitle,
                maxLines = 2,
            )
            Text(
                text = meta,
                color = if (state == RoutineCardState.Completed) {
                    SteppieTheme.colors.success
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = SteppieTheme.typography.guardianCaption,
            )
        }
        Text(
            text = if (state == RoutineCardState.Completed) "✓" else "›",
            modifier = Modifier.clearAndSetSemantics {},
            color = if (state == RoutineCardState.Completed) {
                SteppieTheme.colors.success
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            style = SteppieTheme.typography.childProgress,
        )
    }
}

@Composable
private fun BoxScope.RoutineVisualPlaceholder(title: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(SteppieCornerRadius.Control))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title.take(1),
            color = MaterialTheme.colorScheme.onSurface,
            style = SteppieTheme.typography.guardianSection,
        )
    }
}

@Composable
private fun routineCardColor(color: RoutineCardColor): Color = when (color) {
    RoutineCardColor.Sky -> SteppieTheme.colors.cardSky
    RoutineCardColor.Mint -> SteppieTheme.colors.cardMint
    RoutineCardColor.Lemon -> SteppieTheme.colors.cardLemon
    RoutineCardColor.Peach -> SteppieTheme.colors.cardPeach
    RoutineCardColor.Lavender -> SteppieTheme.colors.cardLavender
    RoutineCardColor.Rose -> SteppieTheme.colors.cardRose
}

@Preview(name = "Focus current", showBackground = true, widthDp = 393, heightDp = 520)
@Composable
private fun FocusRoutineCardPreview() {
    SteppieTheme {
        RoutineCard(
            title = "양치하기",
            state = RoutineCardState.Current,
            onClick = {},
            modifier = Modifier.padding(SteppieSpacing.Large),
            presentation = RoutineCardPresentation.Focus,
            meta = "카드를 누르면 완료",
        )
    }
}

@Preview(name = "List states", showBackground = true, widthDp = 393, heightDp = 380)
@Composable
private fun ListRoutineCardPreview() {
    SteppieTheme {
        Column(
            modifier = Modifier.padding(SteppieSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium),
        ) {
            RoutineCard("양치하기", RoutineCardState.Current, {})
            RoutineCard("옷 입기", RoutineCardState.Completed, {}, cardColor = RoutineCardColor.Mint)
            RoutineCard("가방 챙기기", RoutineCardState.Upcoming, {}, cardColor = RoutineCardColor.Peach, meta = "3 · 8:00")
        }
    }
}
