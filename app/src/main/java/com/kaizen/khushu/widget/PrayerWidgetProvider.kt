package com.kaizen.khushu.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color

import android.location.Geocoder
import android.os.Build

import android.util.Log
import android.widget.RemoteViews
import com.kaizen.khushu.MainActivity
import com.kaizen.khushu.R
import com.kaizen.khushu.data.repository.PrayerTimeRepository
import com.kaizen.khushu.data.repository.SettingsRepository
import com.kaizen.khushu.data.repository.UserSettings
import com.kaizen.khushu.data.repository.WidgetVisibilityItems
import com.kaizen.khushu.i18n.LocaleHelper
import com.kaizen.khushu.i18n.prayerLabel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch

class PrayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COUNTDOWN_REFRESH) {
            super.onReceive(context, intent)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, PrayerWidgetProvider::class.java))
                for (appWidgetId in ids) {
                    updateAppWidget(context, manager, appWidgetId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "PrayerWidgetProvider"
        private const val ACTION_COUNTDOWN_REFRESH = "com.kaizen.khushu.widget.REFRESH_COUNTDOWN"

        private data class PrayerItem(
            val name: String,
            val visibilityKey: String,
            val rowId: Int,
            val pillId: Int,
            val labelId: Int,
            val timeId: Int
        )

        private val prayerItems = listOf(
            PrayerItem(
                "Fajr", WidgetVisibilityItems.FAJR, R.id.row_fajr, R.id.pill_fajr,
                R.id.label_fajr, R.id.time_fajr
            ),
            PrayerItem(
                "Dhuhr", WidgetVisibilityItems.DHUHR, R.id.row_dhuhr, R.id.pill_dhuhr,
                R.id.label_dhuhr, R.id.time_dhuhr
            ),
            PrayerItem(
                "Asr", WidgetVisibilityItems.ASR, R.id.row_asr, R.id.pill_asr,
                R.id.label_asr, R.id.time_asr
            ),
            PrayerItem(
                "Maghrib",
                WidgetVisibilityItems.MAGHRIB,
                R.id.row_maghrib,
                R.id.pill_maghrib,
                R.id.label_maghrib,
                R.id.time_maghrib
            ),
            PrayerItem(
                "Isha", WidgetVisibilityItems.ISHA, R.id.row_isha, R.id.pill_isha,
                R.id.label_isha, R.id.time_isha
            )
        )

        suspend fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val localizedContext = LocaleHelper.wrap(context)
            val views = RemoteViews(localizedContext.packageName, R.layout.widget_prayer_times)
            val launchIntent = Intent(localizedContext, MainActivity::class.java)
            val contentIntent = PendingIntent.getActivity(
                localizedContext,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, contentIntent)

            try {
                val settings = SettingsRepository(localizedContext).settingsFlow.first()
                val prayerTimeRepository = PrayerTimeRepository(SettingsRepository(localizedContext))
                val nowMs = System.currentTimeMillis()
                val nowDate = Date(nowMs)
                val validLocation = settings.locationLat.isFinite() &&
                        settings.locationLng.isFinite() &&
                        settings.locationLat in -90f..90f &&
                        settings.locationLng in -180f..180f

                val hijriDate = runCatching {
                    val hijri = java.time.chrono.HijrahDate.now()
                    java.time.format.DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH).format(hijri)
                }.getOrDefault(localizedContext.getString(R.string.widget_date_unavailable))
                views.setTextViewText(
                    R.id.text_gregorian_date,
                    SimpleDateFormat("EEE, d MMMM", Locale.getDefault()).format(nowDate)
                )
                views.setTextViewText(R.id.text_hijri_date, hijriDate)

                val locationName = if (validLocation) {
                    settings.locationLabel.ifBlank { resolveLocationLabel(localizedContext, settings) }
                } else {
                    localizedContext.getString(R.string.widget_location_unavailable)
                }
                views.setTextViewText(R.id.text_location, locationName)

                val effectiveTimes = if (validLocation) {
                    try {
                        prayerTimeRepository.getEffectivePrayerDateTimes(nowDate, settings)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        emptyMap()
                    }
                } else {
                    emptyMap()
                }
                val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                prayerItems.forEach { item ->
                    views.setTextViewText(item.labelId, prayerLabel(localizedContext, item.name))
                    views.setTextViewText(
                        item.timeId,
                        effectiveTimes[item.name]?.let(timeFormatter::format)
                            ?: localizedContext.getString(R.string.widget_time_placeholder)
                    )
                }

                val orderedTimes = prayerItems.mapNotNull { item ->
                    effectiveTimes[item.name]?.let { item.name to it.time }
                }
                val currentPrayer = if (orderedTimes.isNotEmpty()) {
                    orderedTimes.lastOrNull { it.second <= nowMs }?.first ?: "Isha"
                } else {
                    null
                }
                val nextToday = orderedTimes.firstOrNull { it.second > nowMs }
                val nextPrayer = nextToday ?: if (validLocation && orderedTimes.isNotEmpty()) {
                    val tomorrow = Date(nowMs + 24L * 60L * 60L * 1000L)
                    try {
                        prayerTimeRepository.getEffectivePrayerDateTimes(tomorrow, settings)["Fajr"]?.let {
                            "Fajr" to it.time
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                } else {
                    null
                }
                views.setTextViewText(
                    R.id.text_next_prayer,
                    nextPrayer?.first?.let {
                        localizedContext.getString(R.string.widget_next_prayer_in, prayerLabel(localizedContext, it))
                    } ?: localizedContext.getString(R.string.home_next_prayer_fallback)
                )
                views.setTextViewText(
                    R.id.text_countdown,
                    nextPrayer?.second?.let { formatCountdown(it - nowMs) }
                        ?: localizedContext.getString(R.string.widget_time_placeholder)
                )

                applyVisibility(views, settings)
                applyCustomization(views, settings)
                resetPrayerStyles(views, settings)
                currentPrayer?.let { highlightActivePrayer(views, it, settings) }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating app widget", e)
                setUnavailableContent(views, localizedContext)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } finally {
                scheduleNextCountdownRefresh(localizedContext)
            }
        }

        private fun formatCountdown(remainingMillis: Long): String {
            val remainingMinutes = (remainingMillis.coerceAtLeast(0L) + 59_999L) / 60_000L
            return String.format(Locale.US, "%02d:%02d", remainingMinutes / 60L, remainingMinutes % 60L)
        }

        private fun setUnavailableContent(views: RemoteViews, context: Context) {
            views.setTextViewText(R.id.text_gregorian_date, context.getString(R.string.widget_date_unavailable))
            views.setTextViewText(R.id.text_hijri_date, context.getString(R.string.widget_date_unavailable))
            views.setTextViewText(R.id.text_location, context.getString(R.string.widget_location_unavailable))
            views.setTextViewText(R.id.text_next_prayer, context.getString(R.string.home_next_prayer_fallback))
            views.setTextViewText(R.id.text_countdown, context.getString(R.string.widget_time_placeholder))
            prayerItems.forEach { item ->
                views.setTextViewText(item.labelId, prayerLabel(context, item.name))
                views.setTextViewText(item.timeId, context.getString(R.string.widget_time_placeholder))
            }
        }

        private fun applyVisibility(views: RemoteViews, settings: UserSettings) {
            fun visible(item: String) = item !in settings.hiddenWidgetItems
            fun setVisible(viewId: Int, show: Boolean) {
                views.setViewVisibility(viewId, if (show) android.view.View.VISIBLE else android.view.View.GONE)
            }

            val showGregorian = visible(WidgetVisibilityItems.GREGORIAN_DATE)
            val showHijri = visible(WidgetVisibilityItems.HIJRI_DATE)
            setVisible(R.id.gregorian_date_container, showGregorian)
            setVisible(R.id.text_hijri_date, showHijri)
            setVisible(R.id.date_row, showGregorian || showHijri)

            val showLocation = visible(WidgetVisibilityItems.LOCATION)
            val showCountdown = visible(WidgetVisibilityItems.COUNTDOWN)
            val showNextPrayer = visible(WidgetVisibilityItems.NEXT_PRAYER)
            setVisible(R.id.location_container, showLocation)
            setVisible(R.id.text_countdown, showCountdown)
            setVisible(R.id.text_next_prayer, showNextPrayer)
            setVisible(R.id.next_prayer_container, showCountdown || showNextPrayer)
            setVisible(R.id.location_countdown_row, showLocation || showCountdown || showNextPrayer)

            var visiblePrayerCount = 0
            prayerItems.forEach { item ->
                val show = visible(item.visibilityKey)
                setVisible(item.rowId, show)
                if (show) visiblePrayerCount++
            }
            setVisible(R.id.prayer_panel, visiblePrayerCount > 0)
            if (visiblePrayerCount > 0) {
                views.setFloat(R.id.panel_container, "setWeightSum", visiblePrayerCount.toFloat())
            }
        }

        private fun scheduleNextCountdownRefresh(context: Context) {
            val widgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = widgetManager.getAppWidgetIds(ComponentName(context, PrayerWidgetProvider::class.java))
            val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, PrayerWidgetProvider::class.java).setAction(ACTION_COUNTDOWN_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (widgetIds.isEmpty()) {
                alarmManager.cancel(pendingIntent)
                return
            }
            val nextMinute = (System.currentTimeMillis() / 60_000L + 1L) * 60_000L
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, nextMinute, pendingIntent)
        }

        private fun applyCustomization(views: RemoteViews, settings: UserSettings) {
            // Layer 1: Total background
            val bgColor = Color.parseColor(settings.widgetBackgroundColor)
            views.setInt(R.id.img_widget_bg, "setColorFilter", bgColor)
            views.setInt(R.id.img_widget_bg, "setImageAlpha", (settings.widgetBackgroundOpacity * 255).toInt())

            // Layer 2: Inner panel background
            val panelColor = Color.parseColor(settings.widgetPanelColor)
            views.setInt(R.id.img_panel_bg, "setColorFilter", panelColor)
            views.setInt(R.id.img_panel_bg, "setImageAlpha", (settings.widgetPanelOpacity * 255).toInt())

            // Apply font color to static texts and icons
            val fontColor = Color.parseColor(settings.widgetFontColor)
            val subFontColor = (0x99 shl 24) or (fontColor and 0x00FFFFFF)

            views.setTextColor(R.id.text_gregorian_date, fontColor)
            views.setTextColor(R.id.text_hijri_date, fontColor)
            views.setTextColor(R.id.text_location, subFontColor)
            views.setTextColor(R.id.text_countdown, subFontColor)

            // Tint Icons
            views.setInt(R.id.icon_star, "setColorFilter", fontColor)
            views.setInt(R.id.icon_location, "setColorFilter", subFontColor)
        }

        private fun resetPrayerStyles(views: RemoteViews, settings: UserSettings) {
            val fontColor = Color.parseColor(settings.widgetFontColor)
            val semiFontColor = (0xB3 shl 24) or (fontColor and 0x00FFFFFF)
            prayerItems.forEach { item ->
                views.setViewVisibility(item.pillId, android.view.View.GONE)
                views.setTextColor(item.timeId, semiFontColor)
                views.setTextColor(item.labelId, semiFontColor)
            }
        }

        private fun highlightActivePrayer(views: RemoteViews, prayer: String, settings: UserSettings) {
            val item = prayerItems.firstOrNull { it.name == prayer } ?: return
            val fontColor = Color.parseColor(settings.widgetFontColor)
            views.setTextColor(item.labelId, fontColor)
            views.setInt(item.pillId, "setColorFilter", Color.parseColor(settings.widgetIndicatorColor))
            views.setViewVisibility(item.pillId, android.view.View.VISIBLE)
            views.setTextColor(item.timeId, Color.BLACK)
        }

        @Suppress("DEPRECATION")
        private fun resolveLocationLabel(context: Context, settings: UserSettings): String {
            return runCatching {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val results = mutableListOf<android.location.Address>()
                    val latch = CountDownLatch(1)
                    geocoder.getFromLocation(
                        settings.locationLat.toDouble(),
                        settings.locationLng.toDouble(),
                        1
                    ) { found ->
                        results += found
                        latch.countDown()
                    }
                    latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
                    results
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(
                        settings.locationLat.toDouble(),
                        settings.locationLng.toDouble(),
                        1
                    ).orEmpty()
                }

                val best = addresses.firstOrNull()
                listOfNotNull(
                    best?.locality?.takeIf { it.isNotBlank() },
                    best?.subAdminArea?.takeIf { it.isNotBlank() },
                    best?.adminArea?.takeIf { it.isNotBlank() }
                ).firstOrNull()
            }.getOrNull() ?: context.getString(R.string.widget_your_area)
        }

        fun forceWidgetRefresh(context: Context) {
            val intent = Intent(context, PrayerWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                    ComponentName(context, PrayerWidgetProvider::class.java)
                )
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
