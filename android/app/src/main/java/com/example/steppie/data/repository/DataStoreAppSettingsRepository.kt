package com.example.steppie.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.steppie.data.security.PinCredentialService
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.FeedbackIntensity
import com.example.steppie.domain.repository.AppSettingsRepository
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class DataStoreAppSettingsRepository(
    context: Context,
    private val pinCredentials: PinCredentialService,
) : AppSettingsRepository {
    private val dataStore = context.applicationContext.appSettingsDataStore

    override fun observeAppSettings(): Flow<AppSettings> = dataStore.data.map { preferences ->
        val default = AppSettings()
        AppSettings(
            guardianPinHash = preferences[Keys.GuardianPinHash],
            recoveryCodeHash = preferences[Keys.RecoveryCodeHash],
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

    suspend fun getAppSettings(): AppSettings = observeAppSettings().first()

    override suspend fun updateAppSettings(settings: AppSettings) {
        replaceAppSettings(settings)
    }

    suspend fun replaceAppSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences.clear()
            settings.guardianPinHash?.let { preferences[Keys.GuardianPinHash] = it }
            settings.recoveryCodeHash?.let { preferences[Keys.RecoveryCodeHash] = it }
            preferences[Keys.FeedbackIntensity] = settings.feedbackIntensity.storageValue
            preferences[Keys.SoundEnabled] = settings.soundEnabled
            preferences[Keys.TtsEnabled] = settings.ttsEnabled
            preferences[Keys.TtsRate] = settings.ttsRate
            preferences[Keys.TtsVolume] = settings.ttsVolume
            preferences[Keys.HapticEnabled] = settings.hapticEnabled
            preferences[Keys.UndoDurationSeconds] = settings.undoDurationSeconds
            preferences[Keys.NotificationLeadTimes] = settings.notificationLeadTimes.joinToString(",")
            settings.quietHoursStart?.let { preferences[Keys.QuietHoursStart] = it.toString() }
            settings.quietHoursEnd?.let { preferences[Keys.QuietHoursEnd] = it.toString() }
            settings.locale?.let { preferences[Keys.Locale] = it }
        }
    }

    override suspend fun setGuardianPin(pin: String): String {
        require(pinCredentials.isValidPin(pin)) { "Guardian PIN must be exactly 4 digits." }
        val recoveryCode = pinCredentials.generateRecoveryCode()
        dataStore.edit { preferences ->
            preferences[Keys.GuardianPinHash] = pinCredentials.hash(pin)
            preferences[Keys.RecoveryCodeHash] = pinCredentials.hash(recoveryCode)
        }
        return recoveryCode
    }

    override suspend fun verifyGuardianPin(pin: String): Boolean {
        if (!pinCredentials.isValidPin(pin)) return false
        val storedHash = dataStore.data.first()[Keys.GuardianPinHash] ?: return false
        return pinCredentials.verify(pin, storedHash)
    }

    override suspend fun verifyRecoveryCode(recoveryCode: String): Boolean {
        if (!pinCredentials.isValidRecoveryCode(recoveryCode)) return false
        val storedHash = dataStore.data.first()[Keys.RecoveryCodeHash] ?: return false
        return pinCredentials.verify(recoveryCode, storedHash)
    }

    override suspend fun changeGuardianPin(currentPin: String, newPin: String): String? {
        require(pinCredentials.isValidPin(newPin)) { "Guardian PIN must be exactly 4 digits." }
        if (!verifyGuardianPin(currentPin)) return null
        return setGuardianPin(newPin)
    }

    override suspend fun regenerateRecoveryCode(currentPin: String): String? {
        if (!verifyGuardianPin(currentPin)) return null
        val recoveryCode = pinCredentials.generateRecoveryCode()
        dataStore.edit { preferences ->
            preferences[Keys.RecoveryCodeHash] = pinCredentials.hash(recoveryCode)
        }
        return recoveryCode
    }

    override suspend fun resetGuardianPinWithRecoveryCode(recoveryCode: String, newPin: String): String? {
        require(pinCredentials.isValidPin(newPin)) { "Guardian PIN must be exactly 4 digits." }
        if (!pinCredentials.isValidRecoveryCode(recoveryCode)) return null
        if (!verifyRecoveryCode(recoveryCode)) return null
        return setGuardianPin(newPin)
    }

    private object Keys {
        val GuardianPinHash = stringPreferencesKey("guardian_pin_hash")
        val RecoveryCodeHash = stringPreferencesKey("recovery_code_hash")
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

private fun String.toLeadTimes(): List<Int> {
    if (isBlank()) return emptyList()
    return split(',')
        .mapNotNull { it.trim().takeIf(String::isNotEmpty)?.toIntOrNull() }
        .distinct()
        .filter { it > 0 }
}
