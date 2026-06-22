package com.example.steppie.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal val LightBackgroundPrimary = Color(0xFFFFFFFF)
internal val LightBackgroundSecondary = Color(0xFFF5F7FA)
internal val LightTextPrimary = Color(0xFF1F2933)
internal val LightTextSecondary = Color(0xFF52606D)
internal val LightBorderSubtle = Color(0xFFD9E2EC)
internal val LightFocusRing = Color(0xFF2563EB)
internal val LightSuccess = Color(0xFF2F855A)
internal val LightWarning = Color(0xFFB7791F)
internal val LightDanger = Color(0xFFC53030)
internal val LightCardSky = Color(0xFFD8ECFF)
internal val LightCardMint = Color(0xFFDDF7E8)
internal val LightCardLemon = Color(0xFFFFF3B8)
internal val LightCardPeach = Color(0xFFFFE0CC)
internal val LightCardLavender = Color(0xFFE8DEFF)
internal val LightCardRose = Color(0xFFFFDCE5)

internal val DarkBackgroundPrimary = Color(0xFF121212)
internal val DarkBackgroundSecondary = Color(0xFF1D1F22)
internal val DarkTextPrimary = Color(0xFFF4F6F8)
internal val DarkTextSecondary = Color(0xFFC9D1D9)
internal val DarkBorderSubtle = Color(0xFF3A3F45)
internal val DarkFocusRing = Color(0xFF8AB4F8)
internal val DarkSuccess = Color(0xFF68D391)
internal val DarkWarning = Color(0xFFF6AD55)
internal val DarkDanger = Color(0xFFFC8181)
internal val DarkCardSky = Color(0xFF17456B)
internal val DarkCardMint = Color(0xFF1F5A3D)
internal val DarkCardLemon = Color(0xFF665200)
internal val DarkCardPeach = Color(0xFF6B3B24)
internal val DarkCardLavender = Color(0xFF47306B)
internal val DarkCardRose = Color(0xFF6B263A)

@Immutable
data class SteppieColors(
    val success: Color,
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
    warning = DarkWarning,
    cardSky = DarkCardSky,
    cardMint = DarkCardMint,
    cardLemon = DarkCardLemon,
    cardPeach = DarkCardPeach,
    cardLavender = DarkCardLavender,
    cardRose = DarkCardRose,
)

internal val LocalSteppieColors = staticCompositionLocalOf { LightSteppieColors }
