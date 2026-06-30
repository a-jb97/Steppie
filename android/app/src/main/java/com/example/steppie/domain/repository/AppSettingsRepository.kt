package com.example.steppie.domain.repository

import com.example.steppie.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    fun observeAppSettings(): Flow<AppSettings>
    suspend fun updateAppSettings(settings: AppSettings)
    suspend fun setGuardianPin(pin: String)
    suspend fun verifyGuardianPin(pin: String): Boolean
    suspend fun changeGuardianPin(currentPin: String, newPin: String): Boolean
}
