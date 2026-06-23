package com.example.steppie.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val SteppieFontFamily = FontFamily.SansSerif

@Immutable
data class SteppieTypography(
    val childCardTitle: TextStyle,
    val childListTitle: TextStyle,
    val childProgress: TextStyle,
    val guardianTitle: TextStyle,
    val guardianSection: TextStyle,
    val guardianBody: TextStyle,
    val guardianCaption: TextStyle,
    val button: TextStyle,
)

val SteppieType = SteppieTypography(
    childCardTitle = TextStyle(
        fontFamily = SteppieFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    childListTitle = TextStyle(
        fontFamily = SteppieFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    childProgress = TextStyle(
        fontFamily = SteppieFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
    ),
    guardianTitle = TextStyle(
        fontFamily = SteppieFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    guardianSection = TextStyle(
        fontFamily = SteppieFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    guardianBody = TextStyle(
        fontFamily = SteppieFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    guardianCaption = TextStyle(
        fontFamily = SteppieFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    button = TextStyle(
        fontFamily = SteppieFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
)

internal val LocalSteppieTypography = staticCompositionLocalOf { SteppieType }

internal val MaterialTypography = Typography(
    displaySmall = SteppieType.childCardTitle,
    headlineMedium = SteppieType.guardianTitle,
    headlineSmall = SteppieType.childListTitle,
    titleLarge = SteppieType.childProgress,
    titleMedium = SteppieType.guardianSection,
    bodyLarge = SteppieType.guardianBody,
    bodySmall = SteppieType.guardianCaption,
    labelLarge = SteppieType.button,
)
