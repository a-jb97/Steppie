package com.example.steppie.ui.child

import android.content.Context
import android.util.TypedValue
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.steppie.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompletionStampResourceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun completionStampIsPackagedWithoutDensityScaling() {
        val value = TypedValue()

        context.resources.getValue(R.drawable.complete_stamp, value, true)

        assertEquals(TypedValue.DENSITY_NONE, value.density)
    }
}
