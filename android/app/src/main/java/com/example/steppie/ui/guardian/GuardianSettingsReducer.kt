package com.example.steppie.ui.guardian

import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.FeedbackIntensity
import java.time.LocalTime

internal object GuardianSettingsReducer {
    fun settingsChanged(
        state: GuardianModeUiState,
        settings: AppSettings,
    ): GuardianModeUiState = state.copy(
        appSettings = settings,
        interactionToken = state.interactionToken + 1,
    )

    fun updateFeedbackIntensity(
        settings: AppSettings,
        intensity: FeedbackIntensity,
    ): AppSettings = settings.copy(feedbackIntensity = intensity)

    fun updateTtsEnabled(
        settings: AppSettings,
        enabled: Boolean,
    ): AppSettings = settings.copy(ttsEnabled = enabled)

    fun updateTtsRate(
        settings: AppSettings,
        rate: Double,
    ): AppSettings = settings.copy(ttsRate = rate)

    fun updateTtsVolume(
        settings: AppSettings,
        volume: Double,
    ): AppSettings = settings.copy(ttsVolume = volume)

    fun updateSoundEnabled(
        settings: AppSettings,
        enabled: Boolean,
    ): AppSettings = settings.copy(soundEnabled = enabled)

    fun updateHapticEnabled(
        settings: AppSettings,
        enabled: Boolean,
    ): AppSettings = settings.copy(hapticEnabled = enabled)

    fun updateNotificationLeadTime(
        settings: AppSettings,
        leadMinutes: Int,
        enabled: Boolean,
    ): AppSettings {
        require(leadMinutes in setOf(10, 5))
        val leadTimes = if (enabled) {
            (settings.notificationLeadTimes + leadMinutes).distinct()
        } else {
            settings.notificationLeadTimes.filterNot { it == leadMinutes }
        }.sortedDescending()
        return settings.copy(notificationLeadTimes = leadTimes)
    }

    fun updateQuietHoursEnabled(
        settings: AppSettings,
        enabled: Boolean,
    ): AppSettings = if (enabled) {
        settings.copy(
            quietHoursStart = settings.quietHoursStart ?: LocalTime.of(21, 0),
            quietHoursEnd = settings.quietHoursEnd ?: LocalTime.of(7, 0),
        )
    } else {
        settings.copy(quietHoursStart = null, quietHoursEnd = null)
    }

    fun updateQuietHoursStart(
        settings: AppSettings,
        time: LocalTime,
    ): AppSettings = settings.copy(quietHoursStart = time)

    fun updateQuietHoursEnd(
        settings: AppSettings,
        time: LocalTime,
    ): AppSettings = settings.copy(quietHoursEnd = time)
}
