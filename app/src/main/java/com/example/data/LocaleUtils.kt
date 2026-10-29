package com.example.data

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * Lightweight per-app language handling that works on every supported API level
 * (the framework LocaleManager only exists on API 33+).
 */
object LocaleUtils {
    private const val PREFS = "catbyte_locale"
    private const val KEY_LANGUAGE = "language"

    fun persistLanguage(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.name)
            .apply()
    }

    fun readLanguage(context: Context): AppLanguage {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null)
        return AppLanguage.entries.firstOrNull { it.name == stored } ?: AppLanguage.SYSTEM
    }

    /** Returns a context wrapped with the chosen locale, if one is selected. */
    fun wrap(context: Context): Context {
        val language = readLanguage(context)
        val tag = language.tag
        if (tag == null) {
            val systemLocale = context.resources.configuration.locales[0]
            Locale.setDefault(systemLocale)
            return context
        }

        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLocales(LocaleList(locale))
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(configuration)
    }
}
