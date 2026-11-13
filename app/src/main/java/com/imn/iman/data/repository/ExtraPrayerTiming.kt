package com.imn.iman.data.repository

import android.content.Context
import androidx.annotation.StringRes
import com.imn.iman.R
import com.imn.iman.i18n.LocaleHelper

data class ExtraPrayerTimingDefinition(
    val id: String,
    @StringRes val labelRes: Int,
    @StringRes val shortLabelRes: Int,
    val arabicLabel: String,
)

val EXTRA_PRAYER_TIMINGS = listOf(
    ExtraPrayerTimingDefinition(
        id = "IMSAK",
        labelRes = R.string.prayer_imsak,
        shortLabelRes = R.string.prayer_imsak,
        arabicLabel = "إمساك",
    ),
    ExtraPrayerTimingDefinition(
        id = "SUNRISE",
        labelRes = R.string.prayer_sunrise,
        shortLabelRes = R.string.prayer_sunrise,
        arabicLabel = "شروق",
    ),
    ExtraPrayerTimingDefinition(
        id = "SUNSET",
        labelRes = R.string.prayer_sunset,
        shortLabelRes = R.string.prayer_sunset,
        arabicLabel = "غروب",
    ),
    ExtraPrayerTimingDefinition(
        id = "FIRST_THIRD",
        labelRes = R.string.prayer_first_third,
        shortLabelRes = R.string.prayer_first_third_short,
        arabicLabel = "ثلث أول",
    ),
    ExtraPrayerTimingDefinition(
        id = "MIDNIGHT",
        labelRes = R.string.prayer_midnight,
        shortLabelRes = R.string.prayer_midnight,
        arabicLabel = "منتصف الليل",
    ),
    ExtraPrayerTimingDefinition(
        id = "LAST_THIRD",
        labelRes = R.string.prayer_last_third,
        shortLabelRes = R.string.prayer_last_third,
        arabicLabel = "الثلث الأخير",
    ),
)

fun extraPrayerTimingLabel(context: Context, id: String): String {
    val labelRes = EXTRA_PRAYER_TIMINGS.firstOrNull { it.id == id }?.labelRes ?: return id
    return LocaleHelper.wrap(context).getString(labelRes)
}

fun extraPrayerTimingShortLabel(context: Context, id: String): String {
    val labelRes = EXTRA_PRAYER_TIMINGS.firstOrNull { it.id == id }?.shortLabelRes ?: return id
    return LocaleHelper.wrap(context).getString(labelRes)
}

fun extraPrayerTimingArabicLabel(id: String): String {
    return EXTRA_PRAYER_TIMINGS.firstOrNull { it.id == id }?.arabicLabel.orEmpty()
}
