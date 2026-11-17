package com.imn.iman.notifications

import android.content.Context
import androidx.annotation.StringRes
import com.imn.iman.R
import com.imn.iman.i18n.LocaleHelper
import com.imn.iman.i18n.prayerLabel
import com.imn.iman.data.repository.UserSettings
import com.imn.iman.data.repository.displayLocationName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


data class PrayerNotificationContent(
    val title: String,
    val utilityLine: String,
    val expandedText: String,
)

object PrayerNotificationCopy {
    private val prayerTitles: Map<String, List<Int>> = mapOf(
        "Fajr" to listOf(R.string.notif_title_fajr_1, R.string.notif_title_fajr_2, R.string.notif_title_fajr_3, R.string.notif_title_fajr_4),
        "Dhuhr" to listOf(R.string.notif_title_dhuhr_1, R.string.notif_title_dhuhr_2, R.string.notif_title_dhuhr_3, R.string.notif_title_dhuhr_4),
        "Asr" to listOf(R.string.notif_title_asr_1, R.string.notif_title_asr_2, R.string.notif_title_asr_3, R.string.notif_title_asr_4),
        "Maghrib" to listOf(R.string.notif_title_maghrib_1, R.string.notif_title_maghrib_2, R.string.notif_title_maghrib_3, R.string.notif_title_maghrib_4),
        "Isha" to listOf(R.string.notif_title_isha_1, R.string.notif_title_isha_2, R.string.notif_title_isha_3, R.string.notif_title_isha_4)
    )

    private val prePrayerTitles: Map<String, List<Int>> = mapOf(
        "Fajr" to listOf(R.string.notif_pre_title_fajr_1, R.string.notif_pre_title_fajr_2, R.string.notif_pre_title_fajr_3),
        "Dhuhr" to listOf(R.string.notif_pre_title_dhuhr_1, R.string.notif_pre_title_dhuhr_2, R.string.notif_pre_title_dhuhr_3),
        "Asr" to listOf(R.string.notif_pre_title_asr_1, R.string.notif_pre_title_asr_2, R.string.notif_pre_title_asr_3),
        "Maghrib" to listOf(R.string.notif_pre_title_maghrib_1, R.string.notif_pre_title_maghrib_2, R.string.notif_pre_title_maghrib_3),
        "Isha" to listOf(R.string.notif_pre_title_isha_1, R.string.notif_pre_title_isha_2, R.string.notif_pre_title_isha_3)
    )

    private val reflections: Map<String, List<Int>> = mapOf(
        "Fajr" to listOf(R.string.notif_reflection_fajr_1, R.string.notif_reflection_fajr_2, R.string.notif_reflection_fajr_3),
        "Dhuhr" to listOf(R.string.notif_reflection_dhuhr_1, R.string.notif_reflection_dhuhr_2, R.string.notif_reflection_dhuhr_3),
        "Asr" to listOf(R.string.notif_reflection_asr_1, R.string.notif_reflection_asr_2, R.string.notif_reflection_asr_3),
        "Maghrib" to listOf(R.string.notif_reflection_maghrib_1, R.string.notif_reflection_maghrib_2, R.string.notif_reflection_maghrib_3),
        "Isha" to listOf(R.string.notif_reflection_isha_1, R.string.notif_reflection_isha_2, R.string.notif_reflection_isha_3)
    )

    private val prePrayerReflections: List<Int> = listOf(
        R.string.notif_pre_reflection_1,
        R.string.notif_pre_reflection_2,
        R.string.notif_pre_reflection_3
    )

    private val extraTimingReflections = mapOf(
        "IMSAK" to R.string.notif_extra_reflection_imsak,
        "SUNRISE" to R.string.notif_extra_reflection_sunrise,
        "SUNSET" to R.string.notif_extra_reflection_sunset,
        "FIRST_THIRD" to R.string.notif_extra_reflection_first_third,
        "MIDNIGHT" to R.string.notif_extra_reflection_midnight,
        "LAST_THIRD" to R.string.notif_extra_reflection_last_third
    )

    fun build(
        context: Context,
        settings: UserSettings,
        prayerName: String,
        type: PrayerAlarmType,
        triggerAtMillis: Long,
        prePrayerMinutes: Int,
    ): PrayerNotificationContent {
        val appContext = context.applicationContext
        val localizedContext = LocaleHelper.wrap(appContext)
        val locationLabel = displayLocationName(
            latitude = settings.locationLat.toDouble(),
            longitude = settings.locationLng.toDouble(),
            label = settings.locationLabel,
            labelLatitude = settings.locationLabelLat?.toDouble(),
            labelLongitude = settings.locationLabelLng?.toDouble(),
            defaultLabel = localizedContext.getString(R.string.quran_revelation_makkah)
        )
        val prayerDisplayName = prayerLabel(localizedContext, prayerName)
        val prayerUtilityLine = localizedContext.getString(
            R.string.notif_prayer_at_time_in_location,
            prayerDisplayName,
            formatTime(triggerAtMillis),
            locationLabel
        )
        val seed = "$prayerName|${type.name}|${dayStamp(triggerAtMillis)}".hashCode()
        val isExtraTiming = prayerTitles[prayerName] == null && prePrayerTitles[prayerName] == null

        return if (type == PrayerAlarmType.PRE_PRAYER) {
            val title = pick(prePrayerTitles[prayerName].orEmpty(), seed)?.let {
                localizedContext.getString(it, prayerDisplayName)
            } ?: localizedContext.getString(R.string.notif_pre_prayer_fallback_title, prayerDisplayName)
            val utilityLine = if (locationLabel.isNotBlank()) {
                localizedContext.getString(R.string.notif_pre_prayer_utility, prayerDisplayName, prePrayerMinutes, locationLabel)
            } else {
                localizedContext.getString(R.string.notif_pre_prayer_utility_no_location, prayerDisplayName, prePrayerMinutes)
            }
            val reflection = pick(prePrayerReflections, seed + prePrayerMinutes)?.let { localizedContext.getString(it) }
                ?: localizedContext.getString(R.string.notif_pre_prayer_fallback_reflection)
            PrayerNotificationContent(
                title = title,
                utilityLine = utilityLine,
                expandedText = localizedContext.getString(R.string.notif_expanded_text, utilityLine, reflection)
            )
        } else if (isExtraTiming) {
            val timingLabel = prayerLabel(localizedContext, prayerName)
            val utilityLine = localizedContext.getString(
                R.string.notif_prayer_at_time_in_location,
                timingLabel,
                formatTime(triggerAtMillis),
                locationLabel
            )
            val reflection = localizedContext.getString(
                extraTimingReflections[prayerName] ?: R.string.notif_extra_timing_fallback_reflection
            )
            PrayerNotificationContent(
                title = localizedContext.getString(R.string.notif_extra_timing_title, timingLabel),
                utilityLine = utilityLine,
                expandedText = localizedContext.getString(R.string.notif_expanded_text, utilityLine, reflection)
            )
        } else {
            val title = pick(prayerTitles[prayerName].orEmpty(), seed)?.let {
                localizedContext.getString(it, prayerDisplayName)
            } ?: localizedContext.getString(R.string.notif_prayer_fallback_title, prayerDisplayName)
            val reflection = pick(reflections[prayerName].orEmpty(), seed + 17)?.let { localizedContext.getString(it) }
                ?: localizedContext.getString(R.string.notif_prayer_fallback_reflection)
            PrayerNotificationContent(
                title = title,
                utilityLine = prayerUtilityLine,
                expandedText = localizedContext.getString(R.string.notif_expanded_text, prayerUtilityLine, reflection)
            )
        }
    }

    private fun formatTime(triggerAtMillis: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(triggerAtMillis))
    }


    @StringRes
    private fun pick(options: List<Int>, seed: Int): Int? {
        if (options.isEmpty()) return null
        val index = (seed % options.size + options.size) % options.size
        return options[index]
    }

    private fun dayStamp(triggerAtMillis: Long): String {
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(triggerAtMillis))
    }
}
