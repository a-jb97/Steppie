package com.example.steppie.ui.guardian

import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.FeedbackIntensity
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardianSettingsReducerTest {
    @Test
    fun `settings change updates the single ui state owner and interaction token`() {
        val state = GuardianModeUiState(
            destination = GuardianDestination.EnvironmentSettings,
            interactionToken = 11L,
        )
        val settings = AppSettings(feedbackIntensity = FeedbackIntensity.Strong)

        val updated = GuardianSettingsReducer.settingsChanged(state, settings)

        assertSame(settings, updated.appSettings)
        assertEquals(state.destination, updated.destination)
        assertEquals(12L, updated.interactionToken)
    }

    @Test
    fun `feedback sound tts and haptic changes preserve unrelated settings`() {
        val initial = AppSettings()

        val feedback = GuardianSettingsReducer.updateFeedbackIntensity(
            initial,
            FeedbackIntensity.Quiet,
        )
        val ttsEnabled = GuardianSettingsReducer.updateTtsEnabled(feedback, false)
        val ttsRate = GuardianSettingsReducer.updateTtsRate(ttsEnabled, 0.75)
        val ttsVolume = GuardianSettingsReducer.updateTtsVolume(ttsRate, 0.4)
        val sound = GuardianSettingsReducer.updateSoundEnabled(ttsVolume, false)
        val haptic = GuardianSettingsReducer.updateHapticEnabled(sound, false)

        assertEquals(FeedbackIntensity.Quiet, haptic.feedbackIntensity)
        assertFalse(haptic.ttsEnabled)
        assertEquals(0.75, haptic.ttsRate, 0.0)
        assertEquals(0.4, haptic.ttsVolume, 0.0)
        assertFalse(haptic.soundEnabled)
        assertFalse(haptic.hapticEnabled)
        assertEquals(initial.notificationLeadTimes, haptic.notificationLeadTimes)
    }

    @Test
    fun `notification lead changes remain unique and descending`() {
        val initial = AppSettings(notificationLeadTimes = listOf(5))

        val enabled = GuardianSettingsReducer.updateNotificationLeadTime(initial, 10, true)
        val duplicate = GuardianSettingsReducer.updateNotificationLeadTime(enabled, 5, true)
        val disabled = GuardianSettingsReducer.updateNotificationLeadTime(duplicate, 10, false)

        assertEquals(listOf(10, 5), enabled.notificationLeadTimes)
        assertEquals(listOf(10, 5), duplicate.notificationLeadTimes)
        assertEquals(listOf(5), disabled.notificationLeadTimes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `notification lead rejects unsupported values`() {
        GuardianSettingsReducer.updateNotificationLeadTime(AppSettings(), 15, true)
    }

    @Test
    fun `enabling quiet hours supplies defaults only for missing boundaries`() {
        val initial = AppSettings(quietHoursStart = LocalTime.of(22, 30))

        val enabled = GuardianSettingsReducer.updateQuietHoursEnabled(initial, true)

        assertEquals(LocalTime.of(22, 30), enabled.quietHoursStart)
        assertEquals(LocalTime.of(7, 0), enabled.quietHoursEnd)
    }

    @Test
    fun `disabling quiet hours clears both boundaries`() {
        val initial = AppSettings(
            quietHoursStart = LocalTime.of(21, 0),
            quietHoursEnd = LocalTime.of(7, 0),
        )

        val disabled = GuardianSettingsReducer.updateQuietHoursEnabled(initial, false)

        assertNull(disabled.quietHoursStart)
        assertNull(disabled.quietHoursEnd)
    }

    @Test
    fun `quiet hour boundary changes preserve the other boundary`() {
        val initial = AppSettings(
            quietHoursStart = LocalTime.of(21, 0),
            quietHoursEnd = LocalTime.of(7, 0),
        )

        val start = GuardianSettingsReducer.updateQuietHoursStart(initial, LocalTime.of(22, 15))
        val end = GuardianSettingsReducer.updateQuietHoursEnd(start, LocalTime.of(6, 30))

        assertEquals(LocalTime.of(22, 15), end.quietHoursStart)
        assertEquals(LocalTime.of(6, 30), end.quietHoursEnd)
        assertTrue(end.ttsEnabled)
    }
}
