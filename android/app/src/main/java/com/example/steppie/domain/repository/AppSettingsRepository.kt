package com.example.steppie.domain.repository

import com.example.steppie.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    fun observeAppSettings(): Flow<AppSettings>
}
