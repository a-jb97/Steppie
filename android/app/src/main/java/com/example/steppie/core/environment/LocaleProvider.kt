package com.example.steppie.core.environment

import java.util.Locale

fun interface LocaleProvider {
    fun languageTag(): String
}

object SystemLocaleProvider : LocaleProvider {
    override fun languageTag(): String = Locale.getDefault().toLanguageTag()
}
