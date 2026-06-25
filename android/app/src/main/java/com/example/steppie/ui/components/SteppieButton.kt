package com.example.steppie.ui.components

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.steppie.R
import com.example.steppie.ui.theme.SteppieCornerRadius
import com.example.steppie.ui.theme.SteppieLayout
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.theme.SteppieStroke
import com.example.steppie.ui.theme.SteppieTheme

enum class SteppieButtonStyle { Primary, Secondary, Danger }

enum class SteppieButtonSize { Regular, ChildLarge }

enum class SteppieButtonState { Enabled, Pressed, Disabled, Loading }

@Composable
fun SteppieButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SteppieButtonStyle = SteppieButtonStyle.Primary,
    size: SteppieButtonSize = SteppieButtonSize.Regular,
    state: SteppieButtonState = SteppieButtonState.Enabled,
    accessibilityLabel: String? = null,
    loadingStateDescription: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val resolvedLoadingStateDescription = loadingStateDescription
        ?: stringResource(R.string.state_loading)
    val isPointerPressed by interactionSource.collectIsPressedAsState()
    val isPressed = state == SteppieButtonState.Pressed ||
        (state == SteppieButtonState.Enabled && isPointerPressed)
    val acceptsInput = state == SteppieButtonState.Enabled || state == SteppieButtonState.Pressed
    val context = LocalContext.current
    val animationsEnabled = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) > 0f
        }.getOrDefault(true)
    }
    val targetScale = if (isPressed && animationsEnabled) 0.96f else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 200),
        label = "SteppieButtonPressScale",
    )
    val shape = RoundedCornerShape(SteppieCornerRadius.Control)
    val colors = buttonColors(style)
    val opacity = when {
        state == SteppieButtonState.Disabled -> 0.40f
        isPressed -> 0.86f
        else -> 1f
    }
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .scale(scale)
            .alpha(opacity)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) {
                    Modifier.border(SteppieStroke.Focus, MaterialTheme.colorScheme.primary, shape)
                } else if (style == SteppieButtonStyle.Secondary) {
                    Modifier.border(SteppieStroke.Divider, MaterialTheme.colorScheme.outline, shape)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(colors.container)
            .heightIn(
                min = if (size == SteppieButtonSize.ChildLarge) {
                    SteppieLayout.ChildMinimumTouchTarget
                } else {
                    54.dp
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = acceptsInput,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                accessibilityLabel?.let { contentDescription = it }
                if (state == SteppieButtonState.Loading) {
                    stateDescription = resolvedLoadingStateDescription
                }
            }
            .padding(horizontal = SteppieSpacing.Large, vertical = SteppieSpacing.Medium),
        contentAlignment = Alignment.Center,
    ) {
        if (state == SteppieButtonState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = colors.content,
                strokeWidth = 3.dp,
            )
        } else {
            Text(
                text = label,
                color = colors.content,
                style = SteppieTheme.typography.button,
            )
        }
    }
}

private data class ButtonColors(val container: Color, val content: Color)

@Composable
private fun buttonColors(style: SteppieButtonStyle): ButtonColors = when (style) {
    SteppieButtonStyle.Primary -> ButtonColors(
        container = MaterialTheme.colorScheme.primary,
        content = MaterialTheme.colorScheme.onPrimary,
    )
    SteppieButtonStyle.Secondary -> ButtonColors(
        container = MaterialTheme.colorScheme.surface,
        content = MaterialTheme.colorScheme.onSurface,
    )
    SteppieButtonStyle.Danger -> ButtonColors(
        container = MaterialTheme.colorScheme.error,
        content = MaterialTheme.colorScheme.onError,
    )
}

@Preview(name = "Button states", showBackground = true, widthDp = 620)
@Composable
private fun SteppieButtonPreview() {
    SteppieTheme {
        Row(
            modifier = Modifier.padding(SteppieSpacing.Medium),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                SteppieSpacing.Medium,
            ),
        ) {
            SteppieButton("저장", {}, style = SteppieButtonStyle.Primary)
            SteppieButton("취소", {}, style = SteppieButtonStyle.Secondary, state = SteppieButtonState.Pressed)
            SteppieButton("삭제", {}, style = SteppieButtonStyle.Danger, state = SteppieButtonState.Disabled)
            SteppieButton("저장", {}, state = SteppieButtonState.Loading)
        }
    }
}

@Preview(name = "Button dark", showBackground = true)
@Composable
private fun SteppieButtonDarkPreview() {
    SteppieTheme(darkTheme = true) {
        SteppieButton(
            label = "다음",
            onClick = {},
            modifier = Modifier.padding(SteppieSpacing.Medium),
            size = SteppieButtonSize.ChildLarge,
        )
    }
}
