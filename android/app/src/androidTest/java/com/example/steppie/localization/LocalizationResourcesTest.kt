package com.example.steppie.localization

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.steppie.R
import java.util.Locale
import org.junit.Assert.assertEquals
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

    @Test
    fun quantityStrings_useEnglishSingularAndPluralWhilePreservingKoreanCounters() {
        assertEquals("1 of 1 step completed", localizedQuantityString("en-US", R.plurals.a11y_progress, 1, 1, 1))
        assertEquals("1 of 2 steps completed", localizedQuantityString("en-US", R.plurals.a11y_progress, 2, 1, 2))
        assertEquals("1 step", localizedQuantityString("en-US", R.plurals.guardian_routine_set_meta, 1, 1))
        assertEquals("2 steps", localizedQuantityString("en-US", R.plurals.guardian_routine_set_meta, 2, 2))
        assertEquals(
            "1 activity will be saved as a new routine set",
            localizedQuantityString("en-US", R.plurals.guardian_template_preview_summary, 1, 1),
        )
        assertEquals(
            "2 activities will be saved as a new routine set",
            localizedQuantityString("en-US", R.plurals.guardian_template_preview_summary, 2, 2),
        )
        assertEquals("1 routine set", localizedQuantityString("en-US", R.plurals.guardian_restore_routine_set_count, 1, 1))
        assertEquals("2 routine sets", localizedQuantityString("en-US", R.plurals.guardian_restore_routine_set_count, 2, 2))
        assertEquals("1 routine", localizedQuantityString("en-US", R.plurals.guardian_restore_routine_count, 1, 1))
        assertEquals("2 routines", localizedQuantityString("en-US", R.plurals.guardian_restore_routine_count, 2, 2))
        assertEquals("1 log", localizedQuantityString("en-US", R.plurals.guardian_restore_log_count, 1, 1))
        assertEquals("2 logs", localizedQuantityString("en-US", R.plurals.guardian_restore_log_count, 2, 2))
        assertEquals("단계 1개", localizedQuantityString("ko-KR", R.plurals.guardian_routine_set_meta, 1, 1))
        assertEquals("단계 2개", localizedQuantityString("ko-KR", R.plurals.guardian_routine_set_meta, 2, 2))
    }

    private fun localizedString(
        languageTag: String,
        @StringRes resourceId: Int,
    ): String {
        return localizedResources(languageTag).getString(resourceId)
    }

    private fun localizedQuantityString(
        languageTag: String,
        @PluralsRes resourceId: Int,
        quantity: Int,
        vararg formatArgs: Any,
    ): String = localizedResources(languageTag).getQuantityString(resourceId, quantity, *formatArgs)

    private fun localizedResources(languageTag: String): Resources {
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(languageTag))
        }
        return context.createConfigurationContext(configuration).resources
    }
}
