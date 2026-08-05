package com.example.steppie.data.repository

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.FeedbackIntensity
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class DataStoreAppSettingsCodecTest {
    @Test
    fun validStoredValuesDecodeWithoutChanges() {
        val preferences = mutablePreferencesOf(
            stringPreferencesKey("guardian_pin_hash") to "pin-hash",
            stringPreferencesKey("recovery_code_hash") to "recovery-hash",
            stringPreferencesKey("feedback_intensity") to "quiet",
            booleanPreferencesKey("sound_enabled") to false,
            booleanPreferencesKey("tts_enabled") to false,
            doublePreferencesKey("tts_rate") to 0.5,
            doublePreferencesKey("tts_volume") to 0.25,
            booleanPreferencesKey("haptic_enabled") to false,
            intPreferencesKey("undo_duration_seconds") to 10,
            stringPreferencesKey("notification_lead_times") to "15,5",
            stringPreferencesKey("quiet_hours_start") to "22:30",
            stringPreferencesKey("quiet_hours_end") to "07:00",
            stringPreferencesKey("locale") to "en",
        )

        val settings = preferences.toAppSettings()

        assertEquals("pin-hash", settings.guardianPinHash)
        assertEquals("recovery-hash", settings.recoveryCodeHash)
        assertEquals(FeedbackIntensity.Quiet, settings.feedbackIntensity)
        assertFalse(settings.soundEnabled)
        assertFalse(settings.ttsEnabled)
        assertEquals(0.5, settings.ttsRate, 0.0)
        assertEquals(0.25, settings.ttsVolume, 0.0)
        assertFalse(settings.hapticEnabled)
        assertEquals(10, settings.undoDurationSeconds)
        assertEquals(listOf(15, 5), settings.notificationLeadTimes)
        assertEquals(LocalTime.of(22, 30), settings.quietHoursStart)
        assertEquals(LocalTime.of(7, 0), settings.quietHoursEnd)
        assertEquals("en", settings.locale)
    }

    @Test
    fun invalidStoredValuesFallbackPerFieldWithoutDiscardingValidValues() {
        val default = AppSettings()
        val preferences = mutablePreferencesOf(
            stringPreferencesKey("guardian_pin_hash") to "",
            stringPreferencesKey("recovery_code_hash") to " ",
            stringPreferencesKey("feedback_intensity") to "unknown",
            booleanPreferencesKey("sound_enabled") to false,
            doublePreferencesKey("tts_rate") to 2.0,
            doublePreferencesKey("tts_volume") to Double.NaN,
            booleanPreferencesKey("haptic_enabled") to false,
            intPreferencesKey("undo_duration_seconds") to 7,
            stringPreferencesKey("notification_lead_times") to "10,bad,5",
            stringPreferencesKey("quiet_hours_start") to "25:00",
            stringPreferencesKey("quiet_hours_end") to "07:00:30",
            stringPreferencesKey("locale") to "",
        )

        val settings = preferences.toAppSettings()

        assertNull(settings.guardianPinHash)
        assertNull(settings.recoveryCodeHash)
        assertEquals(default.feedbackIntensity, settings.feedbackIntensity)
        assertFalse(settings.soundEnabled)
        assertEquals(default.ttsRate, settings.ttsRate, 0.0)
        assertEquals(default.ttsVolume, settings.ttsVolume, 0.0)
        assertFalse(settings.hapticEnabled)
        assertEquals(default.undoDurationSeconds, settings.undoDurationSeconds)
        assertEquals(default.notificationLeadTimes, settings.notificationLeadTimes)
        assertNull(settings.quietHoursStart)
        assertNull(settings.quietHoursEnd)
        assertNull(settings.locale)
    }
}
