package com.imn.iman.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.imn.iman.notifications.PrayerNotificationScheduler
import com.imn.iman.widget.WidgetRefresher
import com.imn.iman.worker.PrayerRefreshWork
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PrayerRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                PrayerRefreshWork.enqueueIfNeeded(appContext)
                PrayerNotificationScheduler(appContext).syncNotifications(noNetwork = true)
                try {
                    WidgetRefresher.refreshAll(appContext, "reschedule:${intent.action}")
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.w("ImanWidget", "Could not refresh widget after reschedule", error)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("ImanNotif", "Failed to reschedule prayer notifications", error)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
