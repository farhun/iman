package com.imn.iman.notifications

import android.app.PendingIntent
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.imn.iman.MainActivity
import com.imn.iman.R
import com.imn.iman.data.repository.UserSettings
import com.imn.iman.i18n.LocaleHelper

object PrayerNotificationPublisher {

    fun publish(
        context: Context,
        settings: UserSettings,
        prayerName: String,
        type: PrayerAlarmType,
        triggerAtMillis: Long,
        prePrayerMinutes: Int,
    ) {
        val appContext = context.applicationContext
        val notificationManager = NotificationManagerCompat.from(appContext)

        val launchIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = try {
            PendingIntent.getActivity(
                appContext,
                "$prayerName-${type.name}".hashCode(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } catch (error: Exception) {
            Log.e("ImanNotif", "Failed to build notification content intent", error)
            null
        }

        val selectedSound = when {
            type == PrayerAlarmType.PRE_PRAYER -> when (prayerName) {
                "Fajr" -> settings.fajrPrePrayerNotificationSound
                "Dhuhr" -> settings.dhuhrPrePrayerNotificationSound
                "Asr" -> settings.asrPrePrayerNotificationSound
                "Maghrib" -> settings.maghribPrePrayerNotificationSound
                "Isha" -> settings.ishaPrePrayerNotificationSound
                else -> "DEFAULT"
            }

            settings.extraPrayerSounds.containsKey(prayerName) -> settings.extraPrayerSounds[prayerName] ?: "DEFAULT"
            else -> when (prayerName) {
                "Fajr" -> settings.fajrPrayerNotificationSound
                "Dhuhr" -> settings.dhuhrPrayerNotificationSound
                "Asr" -> settings.asrPrayerNotificationSound
                "Maghrib" -> settings.maghribPrayerNotificationSound
                "Isha" -> settings.ishaPrayerNotificationSound
                else -> "DEFAULT"
            }
        }

        val content = try {
            PrayerNotificationCopy.build(
                context = appContext,
                settings = settings,
                prayerName = prayerName,
                type = type,
                triggerAtMillis = triggerAtMillis,
                prePrayerMinutes = prePrayerMinutes
            )
        } catch (error: Exception) {
            Log.e("ImanNotif", "Failed to resolve notification text", error)
            PrayerNotificationContent("Prayer reminder", "Prayer reminder", "Prayer reminder")
        }
        val localeTag = try {
            val savedTag = LocaleHelper.savedTag(appContext)
            if (savedTag == "system") {
                appContext.resources.configuration.locales[0]?.toLanguageTag()
                    ?: java.util.Locale.getDefault().toLanguageTag()
            } else savedTag
        } catch (error: Exception) {
            Log.e("ImanNotif", "Failed to read notification locale", error)
            java.util.Locale.getDefault().toLanguageTag()
        }
        Log.d("ImanNotif", "Resolved notification locale=$localeTag title=${content.title} body=${content.utilityLine}")

        val requestedChannelId = PrayerNotificationScheduler.channelIdForSound(selectedSound)
        when {
            type == PrayerAlarmType.PRE_PRAYER -> Log.d(
                "ImanNotif",
                "Pre-prayer channel selected prayer=$prayerName sound=$selectedSound channel=$requestedChannelId"
            )

            settings.extraPrayerSounds.containsKey(prayerName) -> Log.d(
                "ImanNotif",
                "Extra timing channel selected timing=$prayerName sound=$selectedSound channel=$requestedChannelId"
            )

            else -> Log.d(
                "ImanNotif",
                "Prayer channel selected prayer=$prayerName sound=$selectedSound channel=$requestedChannelId"
            )
        }
        var channelId = requestedChannelId
        val notificationsEnabled = try {
            notificationManager.areNotificationsEnabled()
        } catch (error: Exception) {
            Log.e("ImanNotif", "Failed to check notification permission", error)
            false
        }
        val platformNotificationManager = appContext.getSystemService(NotificationManager::class.java)
        var channelImportance = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                platformNotificationManager?.getNotificationChannel(channelId)?.importance
            } else null
        } catch (error: Exception) {
            Log.e("ImanNotif", "Failed to read notification channel importance for $channelId", error)
            null
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && channelImportance == null) {
            Log.e("ImanNotif", "Notification channel $channelId is missing", IllegalStateException("Channel not found"))
            PrayerNotificationScheduler.createFallbackNotificationChannel(appContext)
            channelId = PrayerNotificationScheduler.CHANNEL_PRAYER_DEFAULT
            channelImportance = try {
                platformNotificationManager?.getNotificationChannel(channelId)?.importance
            } catch (error: Exception) {
                Log.e("ImanNotif", "Failed to read fallback channel importance", error)
                null
            }
        }
        Log.d(
            "ImanNotif",
            "areNotificationsEnabled=$notificationsEnabled channelId=$channelId importance=$channelImportance"
        )
        Log.d("ImanNotif", "Using notification channel id=$channelId")

        try {
            val builder = NotificationCompat.Builder(
                appContext,
                channelId
            )
                .setSmallIcon(R.drawable.ic_notification_glyph)
                .setColor(ContextCompat.getColor(appContext, R.color.notification_icon_background))
                .setContentTitle(content.title)
                .setContentText(content.utilityLine)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content.expandedText))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(
                    if (selectedSound == "SILENT") {
                        NotificationCompat.PRIORITY_LOW
                    } else {
                        NotificationCompat.PRIORITY_HIGH
                    }
                )
                .setAutoCancel(true)
            contentIntent?.let(builder::setContentIntent)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                if (selectedSound == "SILENT") {
                    builder.setSilent(true)
                } else {
                    PrayerNotificationScheduler.soundUriForSelection(appContext, selectedSound)?.let(builder::setSound)
                    builder.setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                    builder.setVibrate(longArrayOf(0L, 180L, 120L, 180L))
                }
            } else if (selectedSound == "SILENT") {
                builder.setSilent(true)
            }

            val notificationId = "$prayerName-${type.name}".hashCode()
            Log.d("ImanNotif", "Calling notify() for $prayerName ${type.name}")
            notificationManager.notify(notificationId, builder.build())
            Log.d("ImanNotif", "notify() completed for $prayerName ${type.name}")
        } catch (error: Exception) {
            Log.e("ImanNotif", "Failed to build or post notification; retrying with fallback text", error)
            try {
                PrayerNotificationScheduler.createFallbackNotificationChannel(appContext)
                val fallbackNotification = NotificationCompat.Builder(appContext, channelId)
                    .setSmallIcon(R.drawable.ic_notification_glyph)
                    .setContentTitle("Prayer reminder")
                    .setContentText("Prayer reminder")
                    .setStyle(NotificationCompat.BigTextStyle().bigText("Prayer reminder"))
                    .setAutoCancel(true)
                    .build()
                Log.d("ImanNotif", "Calling notify() with fallback text for $prayerName ${type.name}")
                notificationManager.notify("$prayerName-${type.name}".hashCode(), fallbackNotification)
            } catch (fallbackError: Exception) {
                Log.e("ImanNotif", "Failed to post fallback prayer notification", fallbackError)
            }
        }
    }
}
