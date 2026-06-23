package com.example.steppie.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val LightColorScheme = lightColorScheme(
    primary = LightFocusRing,
    onPrimary = LightBackgroundPrimary,
    primaryContainer = LightCardSky,
    onPrimaryContainer = LightTextPrimary,
    secondary = LightTextSecondary,
    onSecondary = LightBackgroundPrimary,
    background = LightBackgroundPrimary,
    onBackground = LightTextPrimary,
    surface = LightBackgroundPrimary,
    onSurface = LightTextPrimary,
    surfaceVariant = LightBackgroundSecondary,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorderSubtle,
    error = LightDanger,
    onError = LightBackgroundPrimary,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkFocusRing,
    onPrimary = DarkBackgroundPrimary,
    primaryContainer = DarkCardSky,
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkTextSecondary,
    onSecondary = DarkBackgroundPrimary,
    background = DarkBackgroundPrimary,
    onBackground = DarkTextPrimary,
    surface = DarkBackgroundPrimary,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkBackgroundSecondary,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorderSubtle,
    error = DarkDanger,
    onError = DarkBackgroundPrimary,
)

object SteppieTheme {
    val colors: SteppieColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSteppieColors.current

    val typography: SteppieTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalSteppieTypography.current
}

@Composable
fun SteppieTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val extendedColors = if (darkTheme) DarkSteppieColors else LightSteppieColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalSteppieColors provides extendedColors,
        LocalSteppieTypography provides SteppieType,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTypography,
            content = content,
        )
    }
}
