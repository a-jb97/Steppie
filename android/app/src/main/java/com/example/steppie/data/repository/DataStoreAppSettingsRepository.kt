package com.example.steppie.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.FeedbackIntensity
import com.example.steppie.domain.repository.AppSettingsRepository
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class DataStoreAppSettingsRepository(
    context: Context,
) : AppSettingsRepository {
    private val dataStore = context.applicationContext.appSettingsDataStore

    override fun observeAppSettings(): Flow<AppSettings> = dataStore.data.map { preferences ->
        val default = AppSettings()
        AppSettings(
            feedbackIntensity = preferences[Keys.FeedbackIntensity]
                ?.let(FeedbackIntensity::fromStorageValue)
                ?: default.feedbackIntensity,
            soundEnabled = preferences[Keys.SoundEnabled] ?: default.soundEnabled,
            ttsEnabled = preferences[Keys.TtsEnabled] ?: default.ttsEnabled,
            ttsRate = preferences[Keys.TtsRate] ?: default.ttsRate,
            ttsVolume = preferences[Keys.TtsVolume] ?: default.ttsVolume,
            hapticEnabled = preferences[Keys.HapticEnabled] ?: default.hapticEnabled,
            undoDurationSeconds = preferences[Keys.UndoDurationSeconds] ?: default.undoDurationSeconds,
            notificationLeadTimes = preferences[Keys.NotificationLeadTimes]
                ?.toLeadTimes()
                ?: default.notificationLeadTimes,
            quietHoursStart = preferences[Keys.QuietHoursStart]?.let(LocalTime::parse),
            quietHoursEnd = preferences[Keys.QuietHoursEnd]?.let(LocalTime::parse),
            locale = preferences[Keys.Locale],
        )
    }

    private object Keys {
        val FeedbackIntensity = stringPreferencesKey("feedback_intensity")
        val SoundEnabled = booleanPreferencesKey("sound_enabled")
        val TtsEnabled = booleanPreferencesKey("tts_enabled")
        val TtsRate = doublePreferencesKey("tts_rate")
        val TtsVolume = doublePreferencesKey("tts_volume")
        val HapticEnabled = booleanPreferencesKey("haptic_enabled")
        val UndoDurationSeconds = intPreferencesKey("undo_duration_seconds")
        val NotificationLeadTimes = stringPreferencesKey("notification_lead_times")
        val QuietHoursStart = stringPreferencesKey("quiet_hours_start")
        val QuietHoursEnd = stringPreferencesKey("quiet_hours_end")
        val Locale = stringPreferencesKey("locale")
    }
}

private fun String.toLeadTimes(): List<Int> = split(',')
    .mapNotNull { it.trim().takeIf(String::isNotEmpty)?.toIntOrNull() }
    .distinct()
    .filter { it > 0 }
    .takeIf(List<Int>::isNotEmpty)
    ?: AppSettings().notificationLeadTimes
