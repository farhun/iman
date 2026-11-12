package com.kaizen.khushu.i18n

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kaizen.khushu.R

@StringRes
fun prayerLabelRes(id: String): Int = when (id) {
    "Fajr" -> R.string.prayer_fajr
    "Dhuhr" -> R.string.prayer_dhuhr
    "Asr" -> R.string.prayer_asr
    "Maghrib" -> R.string.prayer_maghrib
    "Isha" -> R.string.prayer_isha
    "IMSAK" -> R.string.prayer_imsak
    "SUNRISE" -> R.string.prayer_sunrise
    "SUNSET" -> R.string.prayer_sunset
    "FIRST_THIRD" -> R.string.prayer_first_third
    "MIDNIGHT" -> R.string.prayer_midnight
    "LAST_THIRD" -> R.string.prayer_last_third
    "ZAWAL" -> R.string.prayer_zawal
    else -> 0
}

@Composable
fun prayerLabel(id: String): String {
    val labelRes = prayerLabelRes(id)
    return if (labelRes == 0) id else stringResource(labelRes)
}

fun prayerLabel(context: Context, id: String): String {
    val labelRes = prayerLabelRes(id)
    return if (labelRes == 0) id else LocaleHelper.wrap(context).getString(labelRes)
}

@StringRes
fun tasbihCanvasPresetLabelRes(id: String): Int = when (id) {
    "traditional" -> R.string.tasbih_canvas_preset_traditional
    "minimal" -> R.string.tasbih_canvas_preset_minimal
    "classic" -> R.string.tasbih_canvas_preset_classic
    else -> 0
}

@Composable
fun tasbihCanvasPresetLabel(id: String, fallback: String): String {
    val labelRes = tasbihCanvasPresetLabelRes(id)
    return if (labelRes == 0) fallback else stringResource(labelRes)
}
