package com.kaizen.khushu.ui.navigation

import androidx.annotation.StringRes
import com.kaizen.khushu.R


const val SETTINGS_ROUTE = "settings"
const val SETTINGS_COUNTER_ROUTE = "settings/counter"
const val SETTINGS_APPEARANCE_ROUTE = "settings/appearance"
const val SETTINGS_PRAYER_ROUTE = "settings/prayer"
const val SETTINGS_WIDGET_ROUTE = "settings/widget"
const val SETTINGS_ABOUT_ROUTE = "settings/about"

const val ONBOARDING_ROUTE = "onboarding"


const val SETTINGS_TASBEEH_ROUTE = "settings/tasbeeh"

// Immersive & Editor Routes
const val TASBEEH_IMMERSIVE_ROUTE = "tasbeeh/immersive/{collectionId}"

const val TASBEEH_CANVAS_ROUTE = "tasbeeh/canvas"


enum class AppDestinations(@StringRes val labelRes: Int, val icon: Int, val route: String) {
    HOME(R.string.nav_home, R.drawable.ic_home, "home"),
    TASBEEH(R.string.nav_tasbih, R.drawable.ic_tasbeeh, "tasbeeh"),
    QURAN(R.string.nav_quran, R.drawable.ic_learn, "quran"),
    ;

    companion object {
        fun fromRoute(route: String?) = entries.find { it.route == route }
    }
}
