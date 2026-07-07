package com.example.steppie.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal val LightBackgroundPrimary = Color(0xFFF6EFEE)
internal val LightBackgroundSecondary = Color(0xFFEFE6E1)
internal val LightTextPrimary = Color(0xFF190E0B)
internal val LightTextSecondary = Color(0xFF5D4037)
internal val LightBorderSubtle = Color(0xFFD7CCC8)
internal val LightFocusRing = Color(0xFF832C11)
internal val LightSuccess = Color(0xFF2F855A)
internal val LightProgressComplete = Color(0xFF832C11)
internal val LightProgressPending = Color(0xFFD7CCC8)
internal val LightWarning = Color(0xFFB7791F)
internal val LightDanger = Color(0xFFC53030)
internal val LightCardSky = Color(0xFFDDEBF2)
internal val LightCardMint = Color(0xFFDDEBDD)
internal val LightCardLemon = Color(0xFFFFF3B8)
internal val LightCardPeach = Color(0xFFF3D6C7)
internal val LightCardLavender = Color(0xFFF4E3D6)
internal val LightCardRose = Color(0xFFFFDCE5)

internal val DarkBackgroundPrimary = Color(0xFF121212)
internal val DarkBackgroundSecondary = Color(0xFF1D1F22)
internal val DarkTextPrimary = Color(0xFFF4F6F8)
internal val DarkTextSecondary = Color(0xFFC9D1D9)
internal val DarkBorderSubtle = Color(0xFF3A3F45)
internal val DarkFocusRing = Color(0xFFE2A279)
internal val DarkSuccess = Color(0xFF68D391)
internal val DarkProgressComplete = Color(0xFFB85F3A)
internal val DarkProgressPending = Color(0xFF3A3F45)
internal val DarkWarning = Color(0xFFF6AD55)
internal val DarkDanger = Color(0xFFFC8181)
internal val DarkCardSky = Color(0xFF17456B)
internal val DarkCardMint = Color(0xFF1F5A3D)
internal val DarkCardLemon = Color(0xFF665200)
internal val DarkCardPeach = Color(0xFF6B3B24)
internal val DarkCardLavender = Color(0xFF5A2F1C)
internal val DarkCardRose = Color(0xFF6B263A)

@Immutable
data class SteppieColors(
    val success: Color,
    val progressComplete: Color,
    val progressPending: Color,
    val warning: Color,
    val cardSky: Color,
    val cardMint: Color,
    val cardLemon: Color,
    val cardPeach: Color,
    val cardLavender: Color,
    val cardRose: Color,
)

internal val LightSteppieColors = SteppieColors(
    success = LightSuccess,
    progressComplete = LightProgressComplete,
    progressPending = LightProgressPending,
    warning = LightWarning,
    cardSky = LightCardSky,
    cardMint = LightCardMint,
    cardLemon = LightCardLemon,
    cardPeach = LightCardPeach,
    cardLavender = LightCardLavender,
    cardRose = LightCardRose,
)

internal val DarkSteppieColors = SteppieColors(
    success = DarkSuccess,
    progressComplete = DarkProgressComplete,
    progressPending = DarkProgressPending,
    warning = DarkWarning,
    cardSky = DarkCardSky,
    cardMint = DarkCardMint,
    cardLemon = DarkCardLemon,
    cardPeach = DarkCardPeach,
    cardLavender = DarkCardLavender,
    cardRose = DarkCardRose,
)

internal val LocalSteppieColors = staticCompositionLocalOf { LightSteppieColors }
