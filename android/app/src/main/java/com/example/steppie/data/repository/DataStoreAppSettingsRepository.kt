package com.example.steppie.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.FeedbackIntensity
import com.example.steppie.domain.repository.AppSettingsRepository
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.time.LocalTime
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")
private val pinPattern = Regex("^\\d{4}$")
private val recoveryCodePattern = Regex("^\\d{6}$")

class DataStoreAppSettingsRepository(
    context: Context,
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

    override suspend fun setGuardianPin(pin: String) {
        require(pin.matches(pinPattern)) { "Guardian PIN must be exactly 4 digits." }
        val recoveryCode = generateRecoveryCode()
        dataStore.edit { preferences ->
            preferences[Keys.GuardianPinHash] = PinHashing.hash(pin)
            preferences[Keys.RecoveryCodeHash] = PinHashing.hash(recoveryCode)
        }
    }

    override suspend fun verifyGuardianPin(pin: String): Boolean {
        if (!pin.matches(pinPattern)) return false
        val storedHash = dataStore.data.first()[Keys.GuardianPinHash] ?: return false
        return PinHashing.verify(pin, storedHash)
    }

    override suspend fun changeGuardianPin(currentPin: String, newPin: String): Boolean {
        require(newPin.matches(pinPattern)) { "Guardian PIN must be exactly 4 digits." }
        if (!verifyGuardianPin(currentPin)) return false
        setGuardianPin(newPin)
        return true
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

object PinHashing {
    private const val Algorithm = "PBKDF2WithHmacSHA256"
    private const val Iterations = 120_000
    private const val KeyLengthBits = 256
    private val secureRandom = SecureRandom()

    fun hash(value: String): String {
        require(value.matches(pinPattern) || value.matches(recoveryCodePattern)) {
            "PIN values must be 4 digits and recovery codes must be 6 digits."
        }
        val salt = ByteArray(16).also(secureRandom::nextBytes)
        val encoded = derive(value, salt, Iterations)
        return listOf(
            "pbkdf2-sha256",
            Iterations.toString(),
            Base64.getEncoder().encodeToString(salt),
            Base64.getEncoder().encodeToString(encoded),
        ).joinToString("$")
    }

    fun verify(value: String, storedHash: String): Boolean {
        val parts = storedHash.split('$')
        if (parts.size != 4 || parts[0] != "pbkdf2-sha256") return false
        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = runCatching { Base64.getDecoder().decode(parts[2]) }.getOrNull() ?: return false
        val expected = runCatching { Base64.getDecoder().decode(parts[3]) }.getOrNull() ?: return false
        val actual = derive(value, salt, iterations)
        return constantTimeEquals(expected, actual)
    }

    private fun derive(value: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec: KeySpec = PBEKeySpec(value.toCharArray(), salt, iterations, KeyLengthBits)
        return SecretKeyFactory.getInstance(Algorithm).generateSecret(spec).encoded
    }

    private fun constantTimeEquals(left: ByteArray, right: ByteArray): Boolean {
        if (left.size != right.size) return false
        var result = 0
        left.indices.forEach { index -> result = result or (left[index].toInt() xor right[index].toInt()) }
        return result == 0
    }
}

private fun generateRecoveryCode(): String = (SecureRandom().nextInt(900_000) + 100_000).toString()

private fun String.toLeadTimes(): List<Int> = split(',')
    .mapNotNull { it.trim().takeIf(String::isNotEmpty)?.toIntOrNull() }
    .distinct()
    .filter { it > 0 }
    .takeIf(List<Int>::isNotEmpty)
    ?: AppSettings().notificationLeadTimes
