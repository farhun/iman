package com.imn.iman.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import com.imn.iman.worker.PrayerRefreshWork
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

object WidgetRefresher {
    const val ACTION_COUNTDOWN_REFRESH = "com.imn.iman.widget.REFRESH_COUNTDOWN"
    const val ACTION_BOUNDARY_REFRESH = "com.imn.iman.widget.REFRESH_BOUNDARY"

    private const val TAG = "ImanWidget"
    private const val COUNTDOWN_REQUEST_CODE = 0
    private const val BOUNDARY_REQUEST_CODE = 1
    private val refreshMutex = Mutex()
    private val dirty = AtomicBoolean(false)

    suspend fun refreshAll(context: Context, reason: String) {
        val appContext = context.applicationContext
        withContext(Dispatchers.IO) { refreshAllOnIo(appContext, reason) }
        PrayerRefreshWork.enqueueIfNeeded(appContext)
    }

    private suspend fun refreshAllOnIo(appContext: Context, reason: String) {
        Log.d(TAG, "Refresh requested reason=$reason")
        if (!refreshMutex.tryLock()) {
            dirty.set(true)
            Log.d(TAG, "Refresh coalesced reason=$reason")
            return
        }

        try {
            do {
                dirty.set(false)
                val manager = AppWidgetManager.getInstance(appContext)
                val ids = manager.getAppWidgetIds(ComponentName(appContext, PrayerWidgetProvider::class.java))
                if (ids.isEmpty()) {
                    cancelAlarms(appContext)
                    Log.d(TAG, "Refresh skipped: no widget instances reason=$reason")
                } else {
                    var nextPrayerBoundary: Long? = null
                    for (id in ids) {
                        val boundary = PrayerWidgetProvider.updateAppWidget(appContext, manager, id)
                        if (boundary != null) {
                            nextPrayerBoundary = nextPrayerBoundary?.let { minOf(it, boundary) } ?: boundary
                        }
                    }
                    if (manager.getAppWidgetIds(ComponentName(appContext, PrayerWidgetProvider::class.java))
                            .isEmpty()
                    ) {
                        cancelAlarms(appContext)
                        Log.d(TAG, "Refresh skipped: widget removed during render reason=$reason")
                    } else {
                        scheduleNextCountdownRefresh(appContext)
                        scheduleNextBoundary(appContext, nextPrayerBoundary)
                    }
                }
            } while (dirty.getAndSet(false))
        } finally {
            refreshMutex.unlock()
        }

        if (dirty.getAndSet(false)) refreshAllOnIo(appContext, reason)
    }

    fun cancelAlarms(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        cancelAlarm(alarmManager, countdownPendingIntent(appContext), "countdown")
        cancelAlarm(alarmManager, boundaryPendingIntent(appContext), "boundary")
    }

    private fun scheduleNextCountdownRefresh(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val nextMinute = (System.currentTimeMillis() / 60_000L + 1L) * 60_000L + 500L
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC,
            nextMinute,
            countdownPendingIntent(context)
        )
        Log.d(TAG, "Alarm armed type=countdown trigger=${Date(nextMinute)}")
    }

    private fun scheduleNextBoundary(context: Context, nextPrayerBoundary: Long?) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val zone = java.time.ZoneId.systemDefault()
        val midnight = java.time.LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val nextPrayerTrigger = nextPrayerBoundary?.let { maxOf(it, System.currentTimeMillis() + 1_000L) }
            ?: Long.MAX_VALUE
        val triggerAt = minOf(nextPrayerTrigger, midnight)
        val pendingIntent = boundaryPendingIntent(context)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } catch (error: SecurityException) {
                Log.w(TAG, "Exact boundary alarm unavailable; using inexact alarm", error)
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
        Log.d(TAG, "Alarm armed type=boundary trigger=${Date(triggerAt)}")
    }

    private fun cancelAlarm(alarmManager: AlarmManager, pendingIntent: PendingIntent, type: String) {
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Alarm cancelled type=$type")
    }

    private fun countdownPendingIntent(context: Context) = PendingIntent.getBroadcast(
        context,
        COUNTDOWN_REQUEST_CODE,
        Intent(context, PrayerWidgetProvider::class.java).setAction(ACTION_COUNTDOWN_REFRESH),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun boundaryPendingIntent(context: Context) = PendingIntent.getBroadcast(
        context,
        BOUNDARY_REQUEST_CODE,
        Intent(context, PrayerWidgetProvider::class.java).setAction(ACTION_BOUNDARY_REFRESH),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
