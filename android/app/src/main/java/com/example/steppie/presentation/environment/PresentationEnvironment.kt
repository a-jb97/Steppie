package com.example.steppie.presentation.environment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import com.example.steppie.core.environment.ClockProvider
import com.example.steppie.core.environment.LocaleProvider
import com.example.steppie.core.environment.SystemClockProvider
import java.time.ZoneId
import java.util.Locale

internal val LocalPresentationClockProvider = staticCompositionLocalOf<ClockProvider> { SystemClockProvider }
internal val LocalPresentationLocaleProvider = staticCompositionLocalOf<LocaleProvider?> { null }

@Composable
internal fun currentPresentationLocale(): Locale {
    val languageTag = LocalPresentationLocaleProvider.current?.languageTag()
        ?: LocalConfiguration.current.locales[0]?.toLanguageTag()
        ?: Locale.getDefault().toLanguageTag()
    return Locale.forLanguageTag(languageTag)
}

@Composable
internal fun currentPresentationZoneId(): ZoneId = LocalPresentationClockProvider.current.zoneId
