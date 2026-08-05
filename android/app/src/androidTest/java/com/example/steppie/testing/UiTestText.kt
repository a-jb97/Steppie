package com.example.steppie.testing

import androidx.test.platform.app.InstrumentationRegistry
import com.example.steppie.domain.model.LocalizedText

internal fun testString(resourceId: Int, vararg formatArgs: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId, *formatArgs)

internal fun LocalizedText.testText(): String {
    val configuration = InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration
    val languageTag = configuration.locales[0]?.toLanguageTag().orEmpty()
    return resolve(appLocale = null, systemLocale = languageTag)
}
