package com.example.steppie.localization

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.steppie.R
import java.util.Locale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalizationResourcesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun privacyStrings_preserveLiteralPercentInKoreanAndEnglish() {
        listOf("ko-KR", "en-US").forEach { languageTag ->
            listOf(R.string.guardian_privacy_note, R.string.guardian_privacy_body).forEach { resourceId ->
                val value = localizedString(languageTag, resourceId)

                assertTrue(value.contains("100%"))
                assertFalse(value.contains("100%%"))
            }
        }
    }

    private fun localizedString(
        languageTag: String,
        @StringRes resourceId: Int,
    ): String {
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(languageTag))
        }
        return context.createConfigurationContext(configuration).getString(resourceId)
    }
}
