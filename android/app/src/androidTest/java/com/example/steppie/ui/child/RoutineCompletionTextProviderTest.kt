package com.example.steppie.ui.child

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutineCompletionTextProviderTest {
    private val provider = AndroidRoutineCompletionTextProvider(
        ApplicationProvider.getApplicationContext<Context>(),
    )

    @Test
    fun completionTextUsesRequestedSupportedLocale() {
        assertEquals(
            "일어나기 완료! 잘했어요!",
            provider.completionText("일어나기", "ko-KR"),
        )
        assertEquals(
            "Wake up done. Great job!",
            provider.completionText("Wake up", "en-US"),
        )
    }
}
