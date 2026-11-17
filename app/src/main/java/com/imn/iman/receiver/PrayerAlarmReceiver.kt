package com.imn.iman.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.imn.iman.data.repository.SettingsRepository
import com.imn.iman.data.repository.UserSettings
import com.imn.iman.notifications.PrayerAlarmType
import com.imn.iman.notifications.PrayerNotificationPublisher
import com.imn.iman.notifications.PrayerNotificationScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ImanNotif", "PrayerAlarmReceiver started action=${intent.action}")
        if (intent.action != ACTION_FIRE_PRAYER_NOTIFICATION) {
            Log.e("ImanNotif", "Ignoring unexpected prayer notification action", IllegalArgumentException("Unexpected action"))
            return
        }

        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME)
        if (prayerName == null) {
            Log.e("ImanNotif", "Ignoring prayer notification without an id", IllegalArgumentException("Missing prayer id"))
            return
        }
        val type = try {
            PrayerAlarmType.valueOf(intent.getStringExtra(EXTRA_NOTIFICATION_TYPE).orEmpty())
        } catch (error: Exception) {
            Log.e("ImanNotif", "Invalid prayer notification type; using PRAYER", error)
            PrayerAlarmType.PRAYER
        }
        val appContext = context.applicationContext
        val prePrayerMinutes = intent.getIntExtra(EXTRA_PRE_PRAYER_MINUTES, 10)
        val triggerAtMillis = intent.getLongExtra(EXTRA_TRIGGER_AT_MILLIS, System.currentTimeMillis())
        Log.d("ImanNotif", "PrayerAlarmReceiver processing prayerId=$prayerName type=${type.name}")

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = try {
                    SettingsRepository(appContext).settingsFlow.first()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.e("ImanNotif", "Failed to load notification settings; using defaults", error)
                    UserSettings(
                        hapticsEnabled = true, dynamicColor = false, pureBlack = false, keepScreenAwake = true,
                        themeMode = "System", showStepTimer = true, fluidTransitions = true,
                        vibrationOnCount = true, showLapCounter = true, colorSeed = "default",
                        tasbeehListMode = true, startupTab = "home"
                    )
                }
                try {
                    PrayerNotificationScheduler(appContext).syncNotifications(settings)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.e("ImanNotif", "Failed to resync prayer alarms before notification", error)
                }
                try {
                    PrayerNotificationScheduler.createNotificationChannels(
                        appContext,
                        settings.prayerNotificationCustomSoundUri
                    )
                } catch (error: Exception) {
                    Log.e("ImanNotif", "Failed to prepare prayer notification channels", error)
                }
                try {
                    PrayerNotificationPublisher.publish(
                        context = appContext,
                        settings = settings,
                        prayerName = prayerName,
                        type = type,
                        triggerAtMillis = triggerAtMillis,
                        prePrayerMinutes = prePrayerMinutes,
                        alertStyle = settings.prayerNotificationAlertStyle
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.e("ImanNotif", "Unexpected failure while publishing prayer notification", error)
                    return@launch
                }
                try {
                    SettingsRepository(appContext).updateLastDeliveredPrayerNotificationEventId(
                        PrayerNotificationScheduler.eventIdFor(prayerName, type, triggerAtMillis)
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.e("ImanNotif", "Failed to persist delivered prayer notification id", error)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE_PRAYER_NOTIFICATION = "com.imn.iman.action.FIRE_PRAYER_NOTIFICATION"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
        const val EXTRA_PRE_PRAYER_MINUTES = "extra_pre_prayer_minutes"
        const val EXTRA_TRIGGER_AT_MILLIS = "extra_trigger_at_millis"
    }
}
