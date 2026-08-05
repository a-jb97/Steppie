package com.example.steppie.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
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

    override fun observeAppSettings(): Flow<AppSettings> = dataStore.data.map(Preferences::toAppSettings)

    suspend fun getAppSettings(): AppSettings = observeAppSettings().first()

    override suspend fun updateAppSettings(settings: AppSettings) {
        replaceAppSettings(settings)
    }

    suspend fun replaceAppSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences.clear()
            settings.guardianPinHash?.let { preferences[AppSettingsKeys.GuardianPinHash] = it }
            settings.recoveryCodeHash?.let { preferences[AppSettingsKeys.RecoveryCodeHash] = it }
            preferences[AppSettingsKeys.FeedbackIntensity] = settings.feedbackIntensity.storageValue
            preferences[AppSettingsKeys.SoundEnabled] = settings.soundEnabled
            preferences[AppSettingsKeys.TtsEnabled] = settings.ttsEnabled
            preferences[AppSettingsKeys.TtsRate] = settings.ttsRate
            preferences[AppSettingsKeys.TtsVolume] = settings.ttsVolume
            preferences[AppSettingsKeys.HapticEnabled] = settings.hapticEnabled
            preferences[AppSettingsKeys.UndoDurationSeconds] = settings.undoDurationSeconds
            preferences[AppSettingsKeys.NotificationLeadTimes] = settings.notificationLeadTimes.joinToString(",")
            settings.quietHoursStart?.let { preferences[AppSettingsKeys.QuietHoursStart] = it.toString() }
            settings.quietHoursEnd?.let { preferences[AppSettingsKeys.QuietHoursEnd] = it.toString() }
            settings.locale?.let { preferences[AppSettingsKeys.Locale] = it }
        }
    }

    override suspend fun setGuardianPin(pin: String): String {
        require(pinCredentials.isValidPin(pin)) { "Guardian PIN must be exactly 4 digits." }
        val recoveryCode = pinCredentials.generateRecoveryCode()
        dataStore.edit { preferences ->
            preferences[AppSettingsKeys.GuardianPinHash] = pinCredentials.hash(pin)
            preferences[AppSettingsKeys.RecoveryCodeHash] = pinCredentials.hash(recoveryCode)
        }
        return recoveryCode
    }

    override suspend fun verifyGuardianPin(pin: String): Boolean {
        if (!pinCredentials.isValidPin(pin)) return false
        val storedHash = dataStore.data.first()[AppSettingsKeys.GuardianPinHash] ?: return false
        return pinCredentials.verify(pin, storedHash)
    }

    override suspend fun verifyRecoveryCode(recoveryCode: String): Boolean {
        if (!pinCredentials.isValidRecoveryCode(recoveryCode)) return false
        val storedHash = dataStore.data.first()[AppSettingsKeys.RecoveryCodeHash] ?: return false
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
            preferences[AppSettingsKeys.RecoveryCodeHash] = pinCredentials.hash(recoveryCode)
        }
        return recoveryCode
    }

    override suspend fun resetGuardianPinWithRecoveryCode(recoveryCode: String, newPin: String): String? {
        require(pinCredentials.isValidPin(newPin)) { "Guardian PIN must be exactly 4 digits." }
        if (!pinCredentials.isValidRecoveryCode(recoveryCode)) return null
        if (!verifyRecoveryCode(recoveryCode)) return null
        return setGuardianPin(newPin)
    }
}

private object AppSettingsKeys {
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

internal fun Preferences.toAppSettings(): AppSettings {
    val default = AppSettings()
    return AppSettings(
        guardianPinHash = this[AppSettingsKeys.GuardianPinHash]?.takeIf(String::isNotBlank),
        recoveryCodeHash = this[AppSettingsKeys.RecoveryCodeHash]?.takeIf(String::isNotBlank),
        feedbackIntensity = this[AppSettingsKeys.FeedbackIntensity]
            ?.let { stored -> FeedbackIntensity.entries.firstOrNull { it.storageValue == stored } }
            ?: default.feedbackIntensity,
        soundEnabled = this[AppSettingsKeys.SoundEnabled] ?: default.soundEnabled,
        ttsEnabled = this[AppSettingsKeys.TtsEnabled] ?: default.ttsEnabled,
        ttsRate = this[AppSettingsKeys.TtsRate]?.takeIf { it in 0.5..1.5 } ?: default.ttsRate,
        ttsVolume = this[AppSettingsKeys.TtsVolume]?.takeIf { it in 0.0..1.0 } ?: default.ttsVolume,
        hapticEnabled = this[AppSettingsKeys.HapticEnabled] ?: default.hapticEnabled,
        undoDurationSeconds = this[AppSettingsKeys.UndoDurationSeconds]
            ?.takeIf { it in setOf(3, 5, 10) }
            ?: default.undoDurationSeconds,
        notificationLeadTimes = this[AppSettingsKeys.NotificationLeadTimes]
            ?.toLeadTimesOrNull()
            ?: default.notificationLeadTimes,
        quietHoursStart = this[AppSettingsKeys.QuietHoursStart]?.toLocalTimeOrNull(),
        quietHoursEnd = this[AppSettingsKeys.QuietHoursEnd]?.toLocalTimeOrNull(),
        locale = this[AppSettingsKeys.Locale]?.takeIf(String::isNotBlank),
    )
}

private fun String.toLeadTimesOrNull(): List<Int>? {
    if (isBlank()) return emptyList()
    val values = split(',').map { token -> token.trim().toIntOrNull() ?: return null }
    return values.takeIf { leadTimes ->
        leadTimes.all { it > 0 } && leadTimes.distinct().size == leadTimes.size
    }
}

private fun String.toLocalTimeOrNull(): LocalTime? = runCatching { LocalTime.parse(this) }
    .getOrNull()
    ?.takeIf { it.second == 0 && it.nano == 0 }
