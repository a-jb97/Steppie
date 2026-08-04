package com.example.steppie.presentation.environment

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.steppie.core.environment.ClockProvider
import com.example.steppie.core.environment.LocaleProvider
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PresentationEnvironmentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun presentationEnvironment_usesInjectedLocaleAndZoneId() {
        val zoneId = ZoneId.of("America/Los_Angeles")
        val clockProvider = FixedClockProvider(zoneId)
        var actualLanguageTag = ""
        var actualZoneId = ZoneId.of("UTC")

        composeRule.setContent {
            CompositionLocalProvider(
                LocalPresentationClockProvider provides clockProvider,
                LocalPresentationLocaleProvider provides LocaleProvider { "ko-KR" },
            ) {
                actualLanguageTag = currentPresentationLocale().toLanguageTag()
                actualZoneId = currentPresentationZoneId()
            }
        }

        composeRule.runOnIdle {
            assertEquals("ko-KR", actualLanguageTag)
            assertEquals(zoneId, actualZoneId)
        }
    }

    @Test
    fun presentationEnvironment_withoutLocaleProviderUsesConfigurationLocale() {
        var configurationLanguageTag = ""
        var actualLanguageTag = ""

        composeRule.setContent {
            configurationLanguageTag = LocalConfiguration.current.locales[0]?.toLanguageTag().orEmpty()
            actualLanguageTag = currentPresentationLocale().toLanguageTag()
        }

        composeRule.runOnIdle {
            assertEquals(configurationLanguageTag, actualLanguageTag)
        }
    }
}

private class FixedClockProvider(
    override val zoneId: ZoneId,
) : ClockProvider {
    override fun now(): Instant = Instant.EPOCH

    override fun today(): LocalDate = LocalDate.of(1970, 1, 1)

    override fun currentTime(): LocalTime = LocalTime.MIDNIGHT

    override fun currentYearMonth(): YearMonth = YearMonth.of(1970, 1)
}
