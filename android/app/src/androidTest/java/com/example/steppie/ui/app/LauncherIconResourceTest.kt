package com.example.steppie.ui.app

import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.steppie.R
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherIconResourceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun launcherResourcesResolveToAdaptiveIcons() {
        listOf(R.mipmap.ic_launcher, R.mipmap.ic_launcher_round).forEach { resourceId ->
            val icon = context.getDrawable(resourceId)

            assertTrue(icon is AdaptiveIconDrawable)
            require(icon is AdaptiveIconDrawable)
            assertNotNull(icon.background)
            assertNotNull(icon.foreground)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                assertNotNull(icon.monochrome)
            }
        }
    }
}
