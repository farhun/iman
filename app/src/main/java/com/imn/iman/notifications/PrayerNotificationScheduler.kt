package com.imn.iman.notifications

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build

import android.util.Log


import com.imn.iman.MainActivity
import com.imn.iman.R
import com.imn.iman.i18n.LocaleHelper
import com.imn.iman.data.repository.PrayerTimeRepository
import com.imn.iman.data.repository.SettingsRepository
import com.imn.iman.data.repository.UserSettings
import com.imn.iman.data.repository.EXTRA_PRAYER_TIMINGS
import com.imn.iman.receiver.PrayerAlarmReceiver
import com.imn.iman.receiver.PrayerRescheduleReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

data class PrayerNotificationScheduleConfig(
    val prayerCalculationMethod: String,
    val prayerMadhab: String,
    val locationLat: Float,
    val locationLng: Float,
    val useGpsLocation: Boolean,
    val prayerSourceType: String,
    val fajrOffsetMinutes: Int,
    val dhuhrOffsetMinutes: Int,
    val asrOffsetMinutes: Int,
    val maghribOffsetMinutes: Int,
    val ishaOffsetMinutes: Int,
    val fajrPrayerNotificationEnabled: Boolean,
    val dhuhrPrayerNotificationEnabled: Boolean,
    val asrPrayerNotificationEnabled: Boolean,
    val maghribPrayerNotificationEnabled: Boolean,
    val ishaPrayerNotificationEnabled: Boolean,
    val fajrPrePrayerNotificationEnabled: Boolean,
    val dhuhrPrePrayerNotificationEnabled: Boolean,
    val asrPrePrayerNotificationEnabled: Boolean,
    val maghribPrePrayerNotificationEnabled: Boolean,
    val ishaPrePrayerNotificationEnabled: Boolean,
    val fajrPrePrayerMinutes: Int,
    val dhuhrPrePrayerMinutes: Int,
    val asrPrePrayerMinutes: Int,
    val maghribPrePrayerMinutes: Int,
    val ishaPrePrayerMinutes: Int,
    val fajrPrayerNotificationSound: String,
    val dhuhrPrayerNotificationSound: String,
    val asrPrayerNotificationSound: String,
    val maghribPrayerNotificationSound: String,
    val ishaPrayerNotificationSound: String,
    val prePrayerNotificationSound: String,
    val prayerNotificationAlertStyle: String,
    val extraPrayerNotifications: Set<String>,
)

fun UserSettings.toPrayerNotificationScheduleConfig(): PrayerNotificationScheduleConfig {
    return PrayerNotificationScheduleConfig(
        prayerCalculationMethod = prayerCalculationMethod,
        prayerMadhab = prayerMadhab,
        locationLat = locationLat,
        locationLng = locationLng,
        useGpsLocation = useGpsLocation,
        prayerSourceType = prayerSourceType,
        fajrOffsetMinutes = fajrOffsetMinutes,
        dhuhrOffsetMinutes = dhuhrOffsetMinutes,
        asrOffsetMinutes = asrOffsetMinutes,
        maghribOffsetMinutes = maghribOffsetMinutes,
        ishaOffsetMinutes = ishaOffsetMinutes,
        fajrPrayerNotificationEnabled = fajrPrayerNotificationEnabled,
        dhuhrPrayerNotificationEnabled = dhuhrPrayerNotificationEnabled,
        asrPrayerNotificationEnabled = asrPrayerNotificationEnabled,
        maghribPrayerNotificationEnabled = maghribPrayerNotificationEnabled,
        ishaPrayerNotificationEnabled = ishaPrayerNotificationEnabled,
        fajrPrePrayerNotificationEnabled = fajrPrePrayerNotificationEnabled,
        dhuhrPrePrayerNotificationEnabled = dhuhrPrePrayerNotificationEnabled,
        asrPrePrayerNotificationEnabled = asrPrePrayerNotificationEnabled,
        maghribPrePrayerNotificationEnabled = maghribPrePrayerNotificationEnabled,
        ishaPrePrayerNotificationEnabled = ishaPrePrayerNotificationEnabled,
        fajrPrePrayerMinutes = fajrPrePrayerMinutes,
        dhuhrPrePrayerMinutes = dhuhrPrePrayerMinutes,
        asrPrePrayerMinutes = asrPrePrayerMinutes,
        maghribPrePrayerMinutes = maghribPrePrayerMinutes,
        ishaPrePrayerMinutes = ishaPrePrayerMinutes,
        fajrPrayerNotificationSound = fajrPrayerNotificationSound,
        dhuhrPrayerNotificationSound = dhuhrPrayerNotificationSound,
        asrPrayerNotificationSound = asrPrayerNotificationSound,
        maghribPrayerNotificationSound = maghribPrayerNotificationSound,
        ishaPrayerNotificationSound = ishaPrayerNotificationSound,
        prePrayerNotificationSound = prePrayerNotificationSound,
        prayerNotificationAlertStyle = prayerNotificationAlertStyle,
        extraPrayerNotifications = extraPrayerNotifications,
    )
}

class PrayerNotificationScheduler(
    context: Context
) {
    private val appContext = context.applicationContext
    private val settingsRepository = SettingsRepository(appContext)
    private val prayerTimeRepository = PrayerTimeRepository(settingsRepository)
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    suspend fun syncNotifications(settings: UserSettings? = null) {
        val currentSettings = settings ?: settingsRepository.settingsFlow.first()
        createNotificationChannels(appContext, currentSettings.prayerNotificationCustomSoundUri)
        cancelAllScheduledNotifications()
        scheduleMidnightReschedule()
        if (!hasAnyNotificationEnabled(currentSettings)) return

        val now = System.currentTimeMillis()
        val datesToSchedule = listOf(
            DateAnchor.Today,
            DateAnchor.Tomorrow
        )

        for (anchor in datesToSchedule) {
            val targetDate = anchor.resolve()
            val prayerTimes = prayerTimeRepository.getPrayerDateTimesForScheduling(
                date = targetDate,
                settings = currentSettings
            )
            val extraPrayerTimes = prayerTimeRepository.getExtraPrayerDateTimes(
                date = targetDate,
                settings = currentSettings
            )

            prayerTimes.forEach { (prayerName, prayerDate) ->
                if (isPrayerNotificationEnabled(currentSettings, prayerName) && prayerDate.time > now) {
                    schedulePrayerAlarm(
                        prayerName = prayerName,
                        triggerAtMillis = prayerDate.time,
                        type = PrayerAlarmType.PRAYER,
                        prePrayerMinutes = 0
                    )
                }

                if (isPrePrayerNotificationEnabled(currentSettings, prayerName)) {
                    val leadMinutes = prePrayerMinutes(currentSettings, prayerName)
                    val prePrayerTime = prayerDate.time - leadMinutes * 60_000L
                    if (prePrayerTime > now) {
                        schedulePrayerAlarm(
                            prayerName = prayerName,
                            triggerAtMillis = prePrayerTime,
                            type = PrayerAlarmType.PRE_PRAYER,
                            prePrayerMinutes = leadMinutes
                        )
                    }
                }
            }

            extraPrayerTimes.forEach { (timingId, timingDate) ->
                if (currentSettings.extraPrayerNotifications.contains(timingId) && timingDate.time > now) {
                    schedulePrayerAlarm(
                        prayerName = timingId,
                        triggerAtMillis = timingDate.time,
                        type = PrayerAlarmType.PRAYER,
                        prePrayerMinutes = 0
                    )
                }
            }
        }
    }

    suspend fun maybeDeliverCurrentPrayerNotification(
        settings: UserSettings? = null,
        nowMillis: Long = System.currentTimeMillis(),
        windowMillis: Long = 75_000L,
    ) {
        val currentSettings = settings ?: settingsRepository.settingsFlow.first()
        if (!hasAnyNotificationEnabled(currentSettings)) return

        val prayerTimes = prayerTimeRepository.getPrayerDateTimesForScheduling(
            date = Date(nowMillis),
            settings = currentSettings
        )
        val extraPrayerTimes = prayerTimeRepository.getExtraPrayerDateTimes(
            date = Date(nowMillis),
            settings = currentSettings
        )

        prayerTimes.forEach { (prayerName, prayerDate) ->
            val prayerTimeMillis = prayerDate.time
            if (isPrayerNotificationEnabled(currentSettings, prayerName) &&
                nowMillis >= prayerTimeMillis &&
                nowMillis - prayerTimeMillis <= windowMillis
            ) {
                deliverIfNeeded(
                    prayerName = prayerName,
                    type = PrayerAlarmType.PRAYER,
                    triggerAtMillis = prayerTimeMillis,
                    prePrayerMinutes = 0,
                    settings = currentSettings
                )
            }

            if (isPrePrayerNotificationEnabled(currentSettings, prayerName)) {
                val leadMinutes = prePrayerMinutes(currentSettings, prayerName)
                val prePrayerTimeMillis = prayerTimeMillis - leadMinutes * 60_000L
                if (nowMillis >= prePrayerTimeMillis &&
                    nowMillis - prePrayerTimeMillis <= windowMillis
                ) {
                    deliverIfNeeded(
                        prayerName = prayerName,
                        type = PrayerAlarmType.PRE_PRAYER,
                        triggerAtMillis = prePrayerTimeMillis,
                        prePrayerMinutes = leadMinutes,
                        settings = currentSettings
                    )
                }
            }
        }

        extraPrayerTimes.forEach { (timingId, timingDate) ->
            val timingMillis = timingDate.time
            if (currentSettings.extraPrayerNotifications.contains(timingId) &&
                nowMillis >= timingMillis &&
                nowMillis - timingMillis <= windowMillis
            ) {
                deliverIfNeeded(
                    prayerName = timingId,
                    type = PrayerAlarmType.PRAYER,
                    triggerAtMillis = timingMillis,
                    prePrayerMinutes = 0,
                    settings = currentSettings
                )
            }
        }
    }

    fun cancelAllScheduledNotifications() {
        val dayOffsets = -1..2
        val prayerNames = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha") + EXTRA_PRAYER_TIMINGS.map { it.id }
        val alarmTypes = PrayerAlarmType.entries

        for (offset in dayOffsets) {
            val requestDay = dayStamp(offset)
            for (prayerName in prayerNames) {
                for (type in alarmTypes) {
                    val pendingIntent = buildAlarmPendingIntent(
                        prayerName = prayerName,
                        requestDay = requestDay,
                        type = type,
                        prePrayerMinutes = 0,
                        triggerAtMillis = 0L,
                        flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    if (pendingIntent != null) {
                        alarmManager.cancel(pendingIntent)
                        pendingIntent.cancel()
                    }
                }
            }
        }
    }

    private fun scheduleMidnightReschedule() {
        val midnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val intent = Intent(appContext, PrayerRescheduleReceiver::class.java).apply {
            action = ACTION_RESCHEDULE_AT_MIDNIGHT
            data = Uri.parse("iman://prayer-reminders/midnight")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            MIDNIGHT_RESCHEDULE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) -> {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, midnight, pendingIntent)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, midnight, pendingIntent)
            }
            else -> alarmManager.setExact(AlarmManager.RTC_WAKEUP, midnight, pendingIntent)
        }
        Log.d(TAG, "Scheduled midnight reschedule at ${Date(midnight)} ($midnight)")
    }

    private fun schedulePrayerAlarm(
        prayerName: String,
        triggerAtMillis: Long,
        type: PrayerAlarmType,
        prePrayerMinutes: Int,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) return

        val requestDay = dayStamp(triggerAtMillis)
        val pendingIntent = buildAlarmPendingIntent(
            prayerName = prayerName,
            requestDay = requestDay,
            type = type,
            prePrayerMinutes = prePrayerMinutes,
            triggerAtMillis = triggerAtMillis,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        if (type == PrayerAlarmType.PRAYER) {
            val showIntent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                data = Uri.parse("iman://prayer-alarm/$requestDay/$prayerName")
            }
            val showPendingIntent = PendingIntent.getActivity(
                appContext,
                requestCode(prayerName, requestDay, type) xor Int.MIN_VALUE,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent),
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
        Log.d(TAG, "Scheduled ${type.name} reminder for $prayerName at ${Date(triggerAtMillis)} ($triggerAtMillis)")
    }


    private fun buildAlarmPendingIntent(
        prayerName: String,
        requestDay: String,
        type: PrayerAlarmType,
        prePrayerMinutes: Int,
        triggerAtMillis: Long,
        flags: Int,
    ): PendingIntent? {
        val intent = Intent(appContext, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_FIRE_PRAYER_NOTIFICATION
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerName)
            putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_TYPE, type.name)
            putExtra(PrayerAlarmReceiver.EXTRA_PRE_PRAYER_MINUTES, prePrayerMinutes)
            putExtra(PrayerAlarmReceiver.EXTRA_TRIGGER_AT_MILLIS, triggerAtMillis)
        }
        return PendingIntent.getBroadcast(
            appContext,
            requestCode(prayerName, requestDay, type),
            intent,
            flags
        )
    }

    private fun hasAnyNotificationEnabled(settings: UserSettings): Boolean {
        return listOf(
            settings.fajrPrayerNotificationEnabled,
            settings.dhuhrPrayerNotificationEnabled,
            settings.asrPrayerNotificationEnabled,
            settings.maghribPrayerNotificationEnabled,
            settings.ishaPrayerNotificationEnabled,
            settings.fajrPrePrayerNotificationEnabled,
            settings.dhuhrPrePrayerNotificationEnabled,
            settings.asrPrePrayerNotificationEnabled,
            settings.maghribPrePrayerNotificationEnabled,
            settings.ishaPrePrayerNotificationEnabled
        ).any { it }
            || settings.extraPrayerNotifications.isNotEmpty()
    }

    private fun isPrayerNotificationEnabled(settings: UserSettings, prayerName: String): Boolean {
        return when (prayerName) {
            "Fajr" -> settings.fajrPrayerNotificationEnabled
            "Dhuhr" -> settings.dhuhrPrayerNotificationEnabled
            "Asr" -> settings.asrPrayerNotificationEnabled
            "Maghrib" -> settings.maghribPrayerNotificationEnabled
            "Isha" -> settings.ishaPrayerNotificationEnabled
            else -> false
        }
    }

    private fun isPrePrayerNotificationEnabled(settings: UserSettings, prayerName: String): Boolean {
        return when (prayerName) {
            "Fajr" -> settings.fajrPrePrayerNotificationEnabled
            "Dhuhr" -> settings.dhuhrPrePrayerNotificationEnabled
            "Asr" -> settings.asrPrePrayerNotificationEnabled
            "Maghrib" -> settings.maghribPrePrayerNotificationEnabled
            "Isha" -> settings.ishaPrePrayerNotificationEnabled
            else -> false
        }
    }

    private fun prePrayerMinutes(settings: UserSettings, prayerName: String): Int {
        return when (prayerName) {
            "Fajr" -> settings.fajrPrePrayerMinutes
            "Dhuhr" -> settings.dhuhrPrePrayerMinutes
            "Asr" -> settings.asrPrePrayerMinutes
            "Maghrib" -> settings.maghribPrePrayerMinutes
            "Isha" -> settings.ishaPrePrayerMinutes
            else -> 10
        }
    }

    private fun requestCode(prayerName: String, requestDay: String, type: PrayerAlarmType): Int {
        val day = requestDay.toInt()
        val year = day / 10_000
        val month = day / 100 % 100
        val monthDay = day % 100
        val prayerIndex = (listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha") + EXTRA_PRAYER_TIMINGS.map { it.id })
            .indexOf(prayerName)
            .let { if (it >= 0) it else 15 }
        return (((year * 13 + month) * 32 + monthDay) * 32) + prayerIndex * 2 + type.ordinal
    }

    private suspend fun deliverIfNeeded(
        prayerName: String,
        type: PrayerAlarmType,
        triggerAtMillis: Long,
        prePrayerMinutes: Int,
        settings: UserSettings,
    ) {
        val eventId = eventIdFor(prayerName, type, triggerAtMillis)
        if (settings.lastDeliveredPrayerNotificationEventId == eventId) return

        PrayerNotificationPublisher.publish(
            context = appContext,
            settings = settings,
            prayerName = prayerName,
            type = type,
            triggerAtMillis = triggerAtMillis,
            prePrayerMinutes = prePrayerMinutes,
            alertStyle = settings.prayerNotificationAlertStyle
        )
        settingsRepository.updateLastDeliveredPrayerNotificationEventId(eventId)
    }

    private fun dayStamp(offset: Int): String {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, offset)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return dayStamp(calendar.timeInMillis)
    }

    private fun dayStamp(epochMillis: Long): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = epochMillis
        }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return "%04d%02d%02d".format(year, month, day)
    }

    private enum class DateAnchor {
        Today,
        Tomorrow;

        fun resolve(): Date {
            val calendar = Calendar.getInstance().apply {
                if (this@DateAnchor == Tomorrow) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return calendar.time
        }
    }

    companion object {
        const val CHANNEL_SYSTEM_SOUND = "prayer_reminders_system_sound_v2"
        const val CHANNEL_CUSTOM_SOUND_PREFIX = "prayer_reminders_custom_sound_v3_"
        const val CHANNEL_VIBRATION = "prayer_reminders_vibration_v2"
        const val CHANNEL_SILENT = "prayer_reminders_silent_v3"
        const val CHANNEL_PRAYER_DEFAULT = "prayer_default"
        const val CHANNEL_PRAYER_ADHAN = "prayer_adhan"
        const val CHANNEL_PRAYER_TAKBIR = "prayer_takbir"
        const val CHANNEL_PRAYER_SILENT = "prayer_silent"

        private const val MIDNIGHT_RESCHEDULE_REQUEST_CODE = 0x4D49444E
        private const val ACTION_RESCHEDULE_AT_MIDNIGHT = "com.imn.iman.action.RESCHEDULE_AT_MIDNIGHT"

        private const val TAG = "PrayerAlarmScheduler"
        private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun syncInApplicationScope(context: Context) {
            val appContext = context.applicationContext
            applicationScope.launch {
                try {
                    PrayerNotificationScheduler(appContext).syncNotifications()
                } catch (error: Exception) {
                    Log.e("ImanNotif", "Failed to resync prayer notifications after language change", error)
                }
            }
        }

        fun createFallbackNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            try {
                val appContext = context.applicationContext
                val attributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                val channel = NotificationChannel(
                    CHANNEL_PRAYER_DEFAULT,
                    "Prayer reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), attributes)
                    enableVibration(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                appContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            } catch (error: Exception) {
                Log.e("ImanNotif", "Failed to create fallback prayer notification channel", error)
            }
        }

        fun createNotificationChannels(
            context: Context,
            customSoundUriString: String = "",
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

            val appContext = context.applicationContext
            try {
                val localizedContext = LocaleHelper.wrap(appContext)
                val notificationManager = appContext.getSystemService(NotificationManager::class.java)
                val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            val customSoundUri = customSoundUriString
                .takeIf { it.isNotBlank() }
                ?.let(Uri::parse)
                ?: defaultCustomSoundUri()
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_PRAYER_DEFAULT,
                    localizedContext.getString(R.string.notif_channel_prayer_sound_default),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes)
                    enableVibration(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
                NotificationChannel(
                    CHANNEL_PRAYER_ADHAN,
                    localizedContext.getString(R.string.notif_channel_prayer_sound_adhan),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(soundUriForSelection(appContext, "ADHAN"), audioAttributes)
                    enableVibration(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
                NotificationChannel(
                    CHANNEL_PRAYER_TAKBIR,
                    localizedContext.getString(R.string.notif_channel_prayer_sound_takbir),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(soundUriForSelection(appContext, "TAKBIR"), audioAttributes)
                    enableVibration(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
                NotificationChannel(
                    CHANNEL_PRAYER_SILENT,
                    localizedContext.getString(R.string.notif_channel_prayer_sound_silent),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(null, null)
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
                NotificationChannel(
                    CHANNEL_SYSTEM_SOUND,
                    localizedContext.getString(R.string.notif_channel_prayer_reminders),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = localizedContext.getString(R.string.notif_channel_system_description)
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes)
                    enableVibration(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
                NotificationChannel(
                    customSoundChannelId(customSoundUriString),
                    localizedContext.getString(R.string.notif_channel_prayer_reminders_custom),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = localizedContext.getString(R.string.notif_channel_custom_description)
                    setSound(customSoundUri, audioAttributes)
                    enableVibration(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
                NotificationChannel(
                    CHANNEL_VIBRATION,
                    localizedContext.getString(R.string.notif_channel_prayer_reminders_vibration),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = localizedContext.getString(R.string.notif_channel_vibration_description)
                    setSound(null, null)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0L, 220L, 140L, 220L)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
                NotificationChannel(
                    CHANNEL_SILENT,
                    localizedContext.getString(R.string.notif_channel_prayer_reminders_silent),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = localizedContext.getString(R.string.notif_channel_silent_description)
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
            } catch (error: Exception) {
                Log.e("ImanNotif", "Failed to build or create prayer notification channels", error)
                createFallbackNotificationChannel(appContext)
            }
        }

        fun channelIdForSound(sound: String): String = when (sound) {
            "ADHAN" -> CHANNEL_PRAYER_ADHAN
            "TAKBIR" -> CHANNEL_PRAYER_TAKBIR
            "SILENT" -> CHANNEL_PRAYER_SILENT
            else -> CHANNEL_PRAYER_DEFAULT
        }

        fun soundUriForSelection(context: Context, sound: String): Uri? = when (sound) {
            "ADHAN" -> Uri.parse("android.resource://${context.packageName}/${com.imn.iman.R.raw.adhan}")
            "TAKBIR" -> Uri.parse("android.resource://${context.packageName}/${com.imn.iman.R.raw.takbir}")
            "SILENT" -> null
            else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        fun channelIdForStyle(
            style: String,
            customSoundUriString: String = "",
        ): String {
            return when (style) {
                "CUSTOM_SOUND" -> customSoundChannelId(customSoundUriString)
                "VIBRATION" -> CHANNEL_VIBRATION
                "SILENT" -> CHANNEL_SILENT
                else -> CHANNEL_SYSTEM_SOUND
            }
        }

        fun defaultSoundUriForStyle(
            style: String,
            customSoundUriString: String = "",
        ): Uri? {
            return when (style) {
                "CUSTOM_SOUND" -> customSoundUriString
                    .takeIf { it.isNotBlank() }
                    ?.let(Uri::parse)
                    ?: defaultCustomSoundUri()
                "SYSTEM_SOUND" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                else -> null
            }
        }

        fun eventIdFor(
            prayerName: String,
            type: PrayerAlarmType,
            triggerAtMillis: Long,
        ): String {
            val calendar = Calendar.getInstance().apply { timeInMillis = triggerAtMillis }
            return "%04d%02d%02d|%s|%s".format(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                prayerName,
                type.name
            )
        }

        private fun defaultCustomSoundUri(): Uri {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }

        private fun customSoundChannelId(customSoundUriString: String): String {
            val channelSuffix = customSoundUriString.ifBlank { "default" }.hashCode().toString().replace("-", "n")
            return "$CHANNEL_CUSTOM_SOUND_PREFIX$channelSuffix"
        }
    }
}

enum class PrayerAlarmType {
    PRAYER,
    PRE_PRAYER
}
