package com.imn.iman.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.imn.iman.data.repository.SettingsRepository
import com.imn.iman.i18n.LocaleHelper
import com.imn.iman.notifications.PrayerAlarmType
import com.imn.iman.notifications.PrayerNotificationPublisher
import com.imn.iman.notifications.PrayerNotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE_PRAYER_NOTIFICATION) return

        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: return
        val type = runCatching {
            PrayerAlarmType.valueOf(intent.getStringExtra(EXTRA_NOTIFICATION_TYPE).orEmpty())
        }.getOrDefault(PrayerAlarmType.PRAYER)
        val localizedContext = LocaleHelper.wrap(context)
        val prePrayerMinutes = intent.getIntExtra(EXTRA_PRE_PRAYER_MINUTES, 10)
        val triggerAtMillis = intent.getLongExtra(EXTRA_TRIGGER_AT_MILLIS, System.currentTimeMillis())

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(localizedContext.applicationContext).settingsFlow.first()
                PrayerNotificationScheduler(localizedContext).syncNotifications(settings)
                PrayerNotificationScheduler.createNotificationChannels(
                    localizedContext,
                    settings.prayerNotificationCustomSoundUri
                )
                PrayerNotificationPublisher.publish(
                    context = localizedContext,
                    settings = settings,
                    prayerName = prayerName,
                    type = type,
                    triggerAtMillis = triggerAtMillis,
                    prePrayerMinutes = prePrayerMinutes,
                    alertStyle = settings.prayerNotificationAlertStyle
                )
                SettingsRepository(localizedContext.applicationContext).updateLastDeliveredPrayerNotificationEventId(
                    PrayerNotificationScheduler.eventIdFor(
                        prayerName = prayerName,
                        type = type,
                        triggerAtMillis = triggerAtMillis
                    )
                )
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
