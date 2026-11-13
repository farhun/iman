package com.kaizen.khushu.i18n

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kaizen.khushu.R

@StringRes
fun revelationPlaceLabelRes(id: String): Int = when (id) {
    "makkah" -> R.string.quran_revelation_makkah
    "madinah" -> R.string.quran_revelation_madinah
    else -> 0
}

@Composable
fun revelationPlaceLabel(id: String): String {
    val labelRes = revelationPlaceLabelRes(id)
    return if (labelRes == 0) id else stringResource(labelRes)
}

fun revelationPlaceLabel(context: Context, id: String): String {
    val labelRes = revelationPlaceLabelRes(id)
    return if (labelRes == 0) id else LocaleHelper.wrap(context).getString(labelRes)
}

@StringRes
fun quranScriptLabelRes(id: String): Int = when (id) {
    "uthmani" -> R.string.quran_script_uthmani
    "indopak" -> R.string.quran_script_indopak
    else -> 0
}

@Composable
fun quranScriptLabel(id: String): String {
    val labelRes = quranScriptLabelRes(id)
    return if (labelRes == 0) id else stringResource(labelRes)
}

@StringRes
fun quranReadingModeLabelRes(id: String): Int = when (id) {
    "verse_by_verse" -> R.string.quran_mode_verse_by_verse
    "reading" -> R.string.quran_mode_reading
    else -> 0
}

@Composable
fun quranReadingModeLabel(id: String): String {
    val labelRes = quranReadingModeLabelRes(id)
    return if (labelRes == 0) id else stringResource(labelRes)
}

@StringRes
fun quranReadingThemeLabelRes(id: String): Int = when (id) {
    "SYSTEM" -> R.string.quran_theme_auto
    "DARK" -> R.string.settings_theme_dark
    "PAPER" -> R.string.quran_theme_paper
    "LIGHT" -> R.string.settings_theme_light
    else -> 0
}

@Composable
fun quranReadingThemeLabel(id: String): String {
    val labelRes = quranReadingThemeLabelRes(id)
    return if (labelRes == 0) id else stringResource(labelRes)
}
