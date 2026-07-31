package com.example.steppie.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.steppie.core.environment.SystemClockProvider
import com.example.steppie.core.environment.SystemLocaleProvider
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppContainerTest {
    @Test
    fun containerCreatesAndRetainsApplicationDependencies() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AppContainer(context)

        assertSame(SystemClockProvider, container.clockProvider)
        assertSame(SystemLocaleProvider, container.localeProvider)
        assertSame(container.routineRepository, container.routineRepository)
        assertSame(container.appSettingsRepository, container.appSettingsRepository)
        assertSame(container.tutorialProgressRepository, container.tutorialProgressRepository)
        assertSame(container.routinePhotoStore, container.routinePhotoStore)
        assertSame(container.backupProvider, container.backupProvider)
        assertSame(container.notificationScheduler, container.notificationScheduler)
    }
}
