package com.example.steppie.ui.app

import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.steppie.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppManifestContractTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun mainActivityInheritsApplicationLabel() {
        val packageManager = context.packageManager
        val activityInfo = packageManager.getActivityInfo(
            ComponentName(context, MainActivity::class.java),
            0,
        )

        assertEquals(0, activityInfo.labelRes)
        assertEquals(
            context.applicationInfo.loadLabel(packageManager).toString(),
            activityInfo.loadLabel(packageManager).toString(),
        )
    }
}
