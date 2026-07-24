package com.hasanege.materialtv.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LanguageManager {

    fun applyLanguage(code: String) {
        val locales = when (code) {
            "en" -> LocaleListCompat.forLanguageTags("en")
            "tr" -> LocaleListCompat.forLanguageTags("tr")
            "es" -> LocaleListCompat.forLanguageTags("es")
            "de" -> LocaleListCompat.forLanguageTags("de")
            "fr" -> LocaleListCompat.forLanguageTags("fr")
            "pt" -> LocaleListCompat.forLanguageTags("pt")
            "ru" -> LocaleListCompat.forLanguageTags("ru")
            "zh" -> LocaleListCompat.forLanguageTags("zh")
            "ur" -> LocaleListCompat.forLanguageTags("ur")
            "ja" -> LocaleListCompat.forLanguageTags("ja")
            "ar" -> LocaleListCompat.forLanguageTags("ar")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}


