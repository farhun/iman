package com.imn.iman.ui.navigation

import androidx.annotation.StringRes
import com.imn.iman.R


const val SETTINGS_ROUTE = "settings"

const val SETTINGS_APPEARANCE_ROUTE = "settings/appearance"
const val SETTINGS_PRAYER_ROUTE = "settings/prayer"
const val SETTINGS_PRAYER_FOCUS_ROUTE = "$SETTINGS_PRAYER_ROUTE?focus={focus}"

fun prayerSettingsRoute(focusId: String? = null): String =
    focusId?.let { "$SETTINGS_PRAYER_ROUTE?focus=$it" } ?: SETTINGS_PRAYER_ROUTE

const val SETTINGS_WIDGET_ROUTE = "settings/widget"
const val SETTINGS_ABOUT_ROUTE = "settings/about"
const val SETTINGS_CREDITS_ROUTE = "settings/credits"


const val SETTINGS_TASBEEH_ROUTE = "settings/tasbeeh"

// Immersive route
const val TASBEEH_IMMERSIVE_ROUTE = "tasbeeh/immersive/{collectionId}"


enum class AppDestinations(@StringRes val labelRes: Int, val icon: Int, val route: String) {
    HOME(R.string.nav_home, R.drawable.ic_home, "home"),
    TASBEEH(R.string.nav_tasbih, R.drawable.ic_tasbeeh, "tasbeeh"),
    QURAN(R.string.nav_quran, R.drawable.ic_learn, "quran"),
    ;

    companion object {
        fun fromRoute(route: String?) = entries.find { it.route == route }
    }
}
