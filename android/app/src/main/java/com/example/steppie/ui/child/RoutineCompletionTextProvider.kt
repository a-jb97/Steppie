package com.example.steppie.ui.child

import android.content.Context
import android.content.res.Configuration
import com.example.steppie.R
import java.util.Locale

fun interface RoutineCompletionTextProvider {
    fun completionText(routineTitle: String, languageTag: String): String
}

class AndroidRoutineCompletionTextProvider(context: Context) : RoutineCompletionTextProvider {
    private val appContext = context.applicationContext

    override fun completionText(routineTitle: String, languageTag: String): String {
        val configuration = Configuration(appContext.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(languageTag))
        }
        return appContext.createConfigurationContext(configuration)
            .getString(R.string.tts_routine_completed, routineTitle)
    }
}
