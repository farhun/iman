package com.imn.iman.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.imn.iman.i18n.LocaleHelper
import com.imn.iman.notifications.PrayerNotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PrayerRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val localizedContext = LocaleHelper.wrap(context)
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                PrayerNotificationScheduler(localizedContext).syncNotifications()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
