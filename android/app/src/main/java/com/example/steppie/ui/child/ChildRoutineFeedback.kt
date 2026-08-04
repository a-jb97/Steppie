package com.example.steppie.ui.child

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick as semanticOnClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.steppie.R
import com.example.steppie.domain.model.FeedbackIntensity
import com.example.steppie.domain.model.Routine
import com.example.steppie.ui.components.RoutineCardColor
import com.example.steppie.ui.components.rememberAnimationsEnabled
import com.example.steppie.ui.theme.SteppieCornerRadius
import com.example.steppie.ui.theme.SteppieLayout
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.theme.SteppieTheme
import kotlinx.coroutines.delay

@Composable
internal fun UndoFeedbackButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(280.dp)
            .height(SteppieLayout.ChildMinimumTouchTarget)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, Color(0xFF5D4037), RoundedCornerShape(50))
                .padding(horizontal = SteppieSpacing.Medium),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_undo_arrow),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.size(SteppieSpacing.ExtraSmall))
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = SteppieTheme.typography.button,
            )
        }
    }
}

@Composable
internal fun NextRoutinePreview(
    routine: Routine,
    onClick: () -> Unit,
) {
    val description = stringResource(R.string.child_next_routine, routine.localizedTitle())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SteppieLayout.ChildMinimumTouchTarget)
            .clip(RoundedCornerShape(SteppieCornerRadius.Card))
            .background(routine.previewColor())
            .clearAndSetSemantics {
                contentDescription = description
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
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center,
        ) {
            RoutineIcon(icon = routine.icon, focus = false, modifier = Modifier.size(48.dp))
        }
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurface,
            style = SteppieTheme.typography.childListTitle,
            maxLines = 2,
        )
    }
}

@Composable
internal fun AllCompletePreview(onClick: () -> Unit) {
    val description = stringResource(R.string.a11y_all_complete)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SteppieLayout.ChildMinimumTouchTarget)
            .clip(RoundedCornerShape(SteppieCornerRadius.Card))
            .background(SteppieTheme.colors.cardSky)
            .clearAndSetSemantics {
                contentDescription = description
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
        Image(
            painter = painterResource(R.drawable.complete_stamp),
            contentDescription = null,
            modifier = Modifier.size(52.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall),
        ) {
            Text(
                text = stringResource(R.string.child_all_complete_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.button,
            )
            Text(
                text = stringResource(R.string.child_all_complete_subtitle),
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.guardianCaption,
                maxLines = 2,
            )
        }
    }
}

@Composable
internal fun ContinueSchedulePreview(onClick: () -> Unit) {
    val description = stringResource(R.string.child_continue_schedule)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SteppieLayout.ChildMinimumTouchTarget)
            .clip(RoundedCornerShape(SteppieCornerRadius.Card))
            .background(SteppieTheme.colors.cardSky)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .padding(SteppieSpacing.Medium),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurface,
            style = SteppieTheme.typography.button,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun AllCompleteContent(
    feedbackIntensity: FeedbackIntensity,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.a11y_all_complete)
    val animationsEnabled = rememberAnimationsEnabled()
    val shouldPulseStamp =
        animationsEnabled &&
        feedbackIntensity in setOf(FeedbackIntensity.Strong, FeedbackIntensity.Normal)
    val stampPulse = remember { Animatable(0f) }
    LaunchedEffect(shouldPulseStamp) {
        if (shouldPulseStamp) {
            stampPulse.snapTo(0f)
            stampPulse.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
            )
        } else {
            stampPulse.snapTo(0f)
        }
    }
    val stampScale = if (shouldPulseStamp) {
        1f + 0.10f * kotlin.math.sin(stampPulse.value * Math.PI).toFloat()
    } else {
        1f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 620.dp)
            .clip(RoundedCornerShape(SteppieCornerRadius.Sheet))
            .background(SteppieTheme.colors.cardSky)
            .semantics(mergeDescendants = true) {
                contentDescription = description
            }
            .padding(SteppieSpacing.ExtraLarge),
        contentAlignment = Alignment.Center,
    ) {
        if (animationsEnabled && feedbackIntensity == FeedbackIntensity.Strong) {
            BirthdayFireworksLayer(
                modifier = Modifier
                    .matchParentSize()
                    .testTag("birthday_fireworks_layer"),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.complete_stamp),
                contentDescription = null,
                modifier = Modifier
                    .size(width = 168.dp, height = 164.dp)
                    .scale(stampScale),
            )
            Spacer(Modifier.size(SteppieSpacing.Large))
            Text(
                text = stringResource(R.string.child_all_complete_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.childCardTitle,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(SteppieSpacing.Medium))
            Text(
                text = stringResource(R.string.child_all_complete_subtitle),
                color = MaterialTheme.colorScheme.primary,
                style = SteppieTheme.typography.guardianSection,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun CheckParticleBurstLayer(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 860, easing = FastOutSlowInEasing),
        )
    }
    val colors = listOf(
        Color(0xFFFFD54F),
        Color(0xFF80CBC4),
        Color(0xFFF48FB1),
        Color(0xFF64B5F6),
        Color(0xFFFF8A65),
        Color(0xFFA5D6A7),
        Color(0xFFC77A4A),
        Color(0xFFFFF176),
    )

    Canvas(modifier = modifier) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val eased = 1f - (1f - progress.value) * (1f - progress.value)
        val alpha = (1f - progress.value).coerceIn(0f, 1f)
        val primaryRadius = size.minDimension * 0.52f
        val secondaryRadius = size.minDimension * 0.34f
        repeat(24) { particle ->
            val angle = Math.PI * 2.0 * particle / 24.0
            val stagger = if (particle % 2 == 0) 1f else 0.78f
            val distance = primaryRadius * eased * stagger
            val particleCenter = androidx.compose.ui.geometry.Offset(
                x = center.x + kotlin.math.cos(angle).toFloat() * distance,
                y = center.y + kotlin.math.sin(angle).toFloat() * distance,
            )
            val trailStart = androidx.compose.ui.geometry.Offset(
                x = center.x + kotlin.math.cos(angle).toFloat() * distance * 0.58f,
                y = center.y + kotlin.math.sin(angle).toFloat() * distance * 0.58f,
            )
            val color = colors[particle % colors.size].copy(alpha = alpha)
            drawLine(
                color = color.copy(alpha = alpha * 0.65f),
                start = trailStart,
                end = particleCenter,
                strokeWidth = 3.5f * (1f - progress.value) + 1f,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = color,
                radius = 3.5f + 5.5f * (1f - progress.value),
                center = particleCenter,
            )
        }
        repeat(12) { particle ->
            val angle = (Math.PI * 2.0 * particle / 12.0) + 0.26
            val distance = secondaryRadius * eased
            drawCircle(
                color = colors[(particle + 3) % colors.size].copy(alpha = alpha * 0.80f),
                radius = 2.5f + 3.5f * (1f - progress.value),
                center = androidx.compose.ui.geometry.Offset(
                    x = center.x + kotlin.math.cos(angle).toFloat() * distance,
                    y = center.y + kotlin.math.sin(angle).toFloat() * distance,
                ),
            )
        }
    }
}

@Composable
private fun BirthdayFireworksLayer(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(80)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1_360, easing = LinearEasing),
        )
    }
    val colors = listOf(
        Color(0xFFFFD54F),
        Color(0xFF80CBC4),
        Color(0xFFF48FB1),
        Color(0xFF64B5F6),
        Color(0xFFFFAB91),
    )
    val bursts = remember {
        listOf(
            FireworkBurst(x = 0.22f, y = 0.26f, delay = 0.00f, radius = 0.20f),
            FireworkBurst(x = 0.76f, y = 0.22f, delay = 0.10f, radius = 0.22f),
            FireworkBurst(x = 0.50f, y = 0.34f, delay = 0.22f, radius = 0.18f),
            FireworkBurst(x = 0.32f, y = 0.52f, delay = 0.34f, radius = 0.16f),
            FireworkBurst(x = 0.70f, y = 0.50f, delay = 0.42f, radius = 0.16f),
        )
    }

    Canvas(modifier = modifier) {
        bursts.forEachIndexed { burstIndex, burst ->
            val localProgress = ((progress.value - burst.delay) / 0.58f).coerceIn(0f, 1f)
            if (localProgress <= 0f || localProgress >= 1f) return@forEachIndexed
            val eased = 1f - (1f - localProgress) * (1f - localProgress)
            val alpha = (1f - localProgress).coerceIn(0f, 1f)
            val center = androidx.compose.ui.geometry.Offset(
                x = size.width * burst.x,
                y = size.height * burst.y,
            )
            val maxRadius = size.minDimension * burst.radius
            repeat(12) { particle ->
                val angle = (Math.PI * 2.0 * particle / 12.0) + (burstIndex * 0.18)
                val distance = maxRadius * eased
                val particleCenter = androidx.compose.ui.geometry.Offset(
                    x = center.x + kotlin.math.cos(angle).toFloat() * distance,
                    y = center.y + kotlin.math.sin(angle).toFloat() * distance,
                )
                drawCircle(
                    color = colors[(particle + burstIndex) % colors.size].copy(alpha = alpha),
                    radius = 5f + 4f * (1f - localProgress),
                    center = particleCenter,
                )
            }
        }
    }
}

private data class FireworkBurst(
    val x: Float,
    val y: Float,
    val delay: Float,
    val radius: Float,
)

@Composable
private fun Routine.previewColor(): Color = when (cardColor()) {
    RoutineCardColor.Sky -> SteppieTheme.colors.cardSky
    RoutineCardColor.Mint -> SteppieTheme.colors.cardMint
    RoutineCardColor.Lemon -> SteppieTheme.colors.cardLemon
    RoutineCardColor.Peach -> SteppieTheme.colors.cardPeach
    RoutineCardColor.Lavender -> SteppieTheme.colors.cardLavender
    RoutineCardColor.Rose -> SteppieTheme.colors.cardRose
}

