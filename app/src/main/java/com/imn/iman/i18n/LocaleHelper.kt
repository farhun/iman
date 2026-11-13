package com.imn.iman.i18n

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {
    private const val PREFERENCES_NAME = "iman_language"
    private const val LANGUAGE_TAG_KEY = "tag"
    private const val SYSTEM_TAG = "system"

    fun savedTag(context: Context): String = context
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        .getString(LANGUAGE_TAG_KEY, SYSTEM_TAG) ?: SYSTEM_TAG

    fun save(context: Context, tag: String) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(LANGUAGE_TAG_KEY, tag)
            .apply()
    }

    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base

        val tag = savedTag(base)
        if (tag == SYSTEM_TAG || SUPPORTED_LANGUAGES.none { it.tag == tag }) return base

        val locale = Locale.forLanguageTag(tag)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLayoutDirection(locale)
        configuration.setLocales(LocaleList(locale))
        return base.createConfigurationContext(configuration)
    }

    fun apply(activity: Activity, tag: String) {
        save(activity, tag)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = activity.getSystemService(LocaleManager::class.java)
            localeManager.applicationLocales = if (tag == SYSTEM_TAG) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(tag)
            }
        } else {
            activity.recreate()
        }
    }
}
