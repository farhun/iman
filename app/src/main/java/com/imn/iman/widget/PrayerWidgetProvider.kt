package com.imn.iman.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF


import android.util.Log
import android.widget.RemoteViews
import com.imn.iman.MainActivity
import com.imn.iman.R
import com.imn.iman.data.repository.PrayerTimeRepository
import com.imn.iman.data.repository.SettingsRepository
import com.imn.iman.data.repository.UserSettings
import com.imn.iman.data.repository.WidgetVisibilityItems
import com.imn.iman.data.repository.displayLocationName
import com.imn.iman.i18n.LocaleHelper
import com.imn.iman.i18n.prayerLabel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.*


class PrayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refreshAsync(context, "appwidget_update")
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        refreshAsync(context, "appwidget_enabled")
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefresher.cancelAlarms(context.applicationContext)
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        super.onRestored(context, oldWidgetIds, newWidgetIds)
        refreshAsync(context, "appwidget_restored")
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == WidgetRefresher.ACTION_COUNTDOWN_REFRESH ||
            action == WidgetRefresher.ACTION_BOUNDARY_REFRESH ||
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED
        ) {
            Log.d("ImanWidget", "Receiver action handled=$action")
            refreshAsync(context, action ?: "unknown")
        } else {
            super.onReceive(context, intent)
        }
    }

    private fun refreshAsync(context: Context, reason: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(8_000L) { WidgetRefresher.refreshAll(context, reason) }
            } catch (timeout: TimeoutCancellationException) {
                Log.w("ImanWidget", "Refresh timed out reason=$reason", timeout)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w("ImanWidget", "Refresh failed reason=$reason", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "PrayerWidgetProvider"

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

        suspend fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int): Long? {
            val localizedContext = LocaleHelper.wrap(context)
            var nextPrayerBoundary: Long? = null
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
                    displayLocationName(
                        latitude = settings.locationLat.toDouble(),
                        longitude = settings.locationLng.toDouble(),
                        label = settings.locationLabel,
                        labelLatitude = settings.locationLabelLat?.toDouble(),
                        labelLongitude = settings.locationLabelLng?.toDouble(),
                        defaultLabel = localizedContext.getString(R.string.quran_revelation_makkah)
                    )
                } else {
                    localizedContext.getString(R.string.widget_location_unavailable)
                }
                views.setTextViewText(R.id.text_location, locationName)

                val effectiveTimes = if (validLocation) {
                    try {
                        prayerTimeRepository.getPrayerDateTimesNoNetwork(nowDate, settings)
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
                    val zone = java.time.ZoneId.systemDefault()
                    val tomorrow = Date.from(
                        java.time.LocalDate.ofInstant(nowDate.toInstant(), zone)
                            .plusDays(1).atStartOfDay(zone).toInstant()
                    )
                    try {
                        prayerTimeRepository.getPrayerDateTimesNoNetwork(tomorrow, settings)["Fajr"]?.let {
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
                nextPrayerBoundary = nextPrayer?.second
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
                val widgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val widgetWidthDp = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 240)
                applyCustomization(
                    views,
                    settings,
                    localizedContext,
                    widgetWidthDp,
                    widgetContentHeightDp(settings)
                )
                resetPrayerStyles(views, settings)
                currentPrayer?.let { highlightActivePrayer(views, it, settings, localizedContext) }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(TAG, "Error updating app widget", error)
                try {
                    setUnavailableContent(views, localizedContext)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (fallbackError: Exception) {
                    Log.w(TAG, "Could not render unavailable widget state", fallbackError)
                }
            }
            return nextPrayerBoundary
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

        private fun widgetContentHeightDp(settings: UserSettings): Int {
            val hidden = settings.hiddenWidgetItems
            val showDates =
                WidgetVisibilityItems.GREGORIAN_DATE !in hidden || WidgetVisibilityItems.HIJRI_DATE !in hidden
            val showDetails = WidgetVisibilityItems.LOCATION !in hidden ||
                    WidgetVisibilityItems.COUNTDOWN !in hidden || WidgetVisibilityItems.NEXT_PRAYER !in hidden
            val showPrayers = prayerItems.any { it.visibilityKey !in hidden }
            return 16 + (if (showDates) 18 else 0) + (if (showDetails) 18 else 0) + (if (showPrayers) 48 else 0)
        }


        private fun applyCustomization(
            views: RemoteViews,
            settings: UserSettings,
            context: Context,
            widgetWidthDp: Int,
            widgetHeightDp: Int
        ) {
            views.setImageViewBitmap(
                R.id.img_widget_bg,
                roundedRectangleBitmap(
                    context,
                    widgetWidthDp,
                    widgetHeightDp,
                    settings.widgetBackgroundCornerRadius,
                    Color.parseColor(settings.widgetBackgroundColor),
                    settings.widgetBackgroundOpacity
                )
            )
            views.setImageViewBitmap(
                R.id.img_panel_bg,
                roundedRectangleBitmap(
                    context,
                    widgetWidthDp - 16,
                    42,
                    settings.widgetPanelCornerRadius,
                    Color.parseColor(settings.widgetPanelColor),
                    settings.widgetPanelOpacity
                )
            )

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

        private fun highlightActivePrayer(
            views: RemoteViews,
            prayer: String,
            settings: UserSettings,
            context: Context
        ) {
            val item = prayerItems.firstOrNull { it.name == prayer } ?: return
            val fontColor = Color.parseColor(settings.widgetFontColor)
            views.setImageViewBitmap(
                item.pillId,
                roundedRectangleBitmap(context, 48, 18, settings.widgetIndicatorCornerRadius)
            )
            views.setTextColor(item.labelId, fontColor)
            views.setInt(item.pillId, "setColorFilter", Color.parseColor(settings.widgetIndicatorColor))
            views.setViewVisibility(item.pillId, android.view.View.VISIBLE)
            views.setTextColor(item.timeId, Color.BLACK)
        }

        private fun roundedRectangleBitmap(
            context: Context,
            widthDp: Int,
            heightDp: Int,
            radiusDp: Float,
            color: Int = Color.WHITE,
            opacity: Float = 1f
        ): Bitmap {
            val density = context.resources.displayMetrics.density
            val width = (widthDp.coerceAtLeast(1) * density).toInt().coerceAtLeast(1)
            val height = (heightDp.coerceAtLeast(1) * density).toInt().coerceAtLeast(1)
            val radius = (radiusDp.coerceAtLeast(0f) * density).coerceAtMost(minOf(width, height) / 2f)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).drawRoundRect(
                RectF(0f, 0f, width.toFloat(), height.toFloat()),
                radius,
                radius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = color
                    alpha = (opacity.coerceIn(0f, 1f) * 255).toInt()
                }
            )
            return bitmap
        }

    }
}
