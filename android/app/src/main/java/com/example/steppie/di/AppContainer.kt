package com.example.steppie.di

import android.content.Context
import com.example.steppie.core.environment.ClockProvider
import com.example.steppie.core.environment.LocaleProvider
import com.example.steppie.core.environment.SystemClockProvider
import com.example.steppie.core.environment.SystemLocaleProvider
import com.example.steppie.data.backup.AndroidBackupRepository
import com.example.steppie.data.backup.BackupDataSource
import com.example.steppie.data.backup.BackupProvider
import com.example.steppie.data.local.SteppieDatabase
import com.example.steppie.data.photo.RoutinePhotoStore
import com.example.steppie.data.repository.DataStoreAppSettingsRepository
import com.example.steppie.data.repository.RoomRoutineRepository
import com.example.steppie.domain.repository.AppSettingsRepository
import com.example.steppie.domain.repository.RoutineRepository
import com.example.steppie.notifications.AndroidRoutineNotificationScheduler
import com.example.steppie.notifications.RoutineNotificationScheduler
import com.example.steppie.ui.tutorial.TutorialProgressRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database: SteppieDatabase by lazy {
        SteppieDatabase.getInstance(appContext)
    }

    val clockProvider: ClockProvider = SystemClockProvider
    val localeProvider: LocaleProvider = SystemLocaleProvider

    val routineRepository: RoutineRepository by lazy {
        RoomRoutineRepository(database)
    }
    val appSettingsRepository: AppSettingsRepository by lazy {
        DataStoreAppSettingsRepository(appContext)
    }
    val tutorialProgressRepository: TutorialProgressRepository by lazy {
        TutorialProgressRepository(appContext)
    }
    val routinePhotoStore: RoutinePhotoStore by lazy {
        RoutinePhotoStore(appContext)
    }
    val backupProvider: BackupProvider by lazy {
        AndroidBackupRepository(
            context = appContext,
            dataSource = BackupDataSource(database, appSettingsRepository, routinePhotoStore),
        )
    }
    val notificationScheduler: RoutineNotificationScheduler by lazy {
        AndroidRoutineNotificationScheduler(appContext)
    }
}
