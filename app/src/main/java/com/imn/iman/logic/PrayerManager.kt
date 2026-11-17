@file:OptIn(kotlin.time.ExperimentalTime::class)
package com.imn.iman.logic

import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Prayer
import com.batoulapps.adhan2.PrayerTimes
import com.imn.iman.data.repository.PrayerTimeRepository
import com.imn.iman.data.repository.SettingsRepository
import android.util.Log
import kotlinx.coroutines.CancellationException
import com.imn.iman.data.repository.toDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.datetime.Instant
import java.util.Date

class PrayerManager(
    private val settingsRepository: SettingsRepository,
    private val prayerRepository: PrayerTimeRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    private fun normalizePrayerName(name: String): String {
        return name.lowercase().replaceFirstChar { it.titlecase() }
    }

    data class PrayerState(
        val current: String,
        val next: String,
        val nextPrayerTime: Instant,
        val allPrayers: PrayerTimes
    )

    val prayerState: StateFlow<PrayerState?> = settingsRepository.settingsFlow
        .flatMapLatest { settings ->
            flow {
                while (currentCoroutineContext().isActive) {
                    val nowMs = System.currentTimeMillis()
                    val now = Instant.fromEpochMilliseconds(nowMs)
                    val date = Date(nowMs)

                    val timings = try {
                        prayerRepository.getSafeLocalPrayerTimes(
                            date = date,
                            lat = settings.locationLat.toDouble(),
                            lng = settings.locationLng.toDouble(),
                            methodStr = settings.prayerCalculationMethod,
                            madhabStr = settings.prayerMadhab
                        )
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Log.w("ImanTimes", "Prayer manager local calculation failed safely", error)
                        null
                    }
                    if (timings == null) {
                        emit(null)
                        delay(60_000L)
                        continue
                    }
                    val effectiveTimes = try {
                        prayerRepository.getEffectivePrayerDateTimes(date, settings)
                            .mapValues { (_, value) -> Instant.fromEpochMilliseconds(value.time) }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Log.w("ImanTimes", "Prayer manager timings unavailable", error)
                        emptyMap()
                    }
                    val orderedNames = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
                    if (effectiveTimes.size != orderedNames.size) {
                        emit(null)
                        delay(60_000L)
                        continue
                    }
                    val currentName = orderedNames.lastOrNull { name ->
                        effectiveTimes[name]?.toEpochMilliseconds()?.let { it <= nowMs } == true
                    } ?: "Isha"
                    val nextTodayName = orderedNames.firstOrNull { name ->
                        effectiveTimes[name]?.toEpochMilliseconds()?.let { it > nowMs } == true
                    }
                    val nextTime = if (nextTodayName != null) {
                        effectiveTimes[nextTodayName]
                    } else {
                        val tomorrow = Date(date.time + 86400000L)
                        val tomorrowTimes = prayerRepository.getEffectivePrayerDateTimes(tomorrow, settings)
                        tomorrowTimes["Fajr"]?.let { Instant.fromEpochMilliseconds(it.time) }
                    }
                    if (nextTime == null) {
                        emit(null)
                        delay(60_000L)
                        continue
                    }
                    val nextPrayer = nextTodayName ?: "Fajr"

                    emit(PrayerState(
                        current = normalizePrayerName(currentName),
                        next = normalizePrayerName(nextPrayer),
                        nextPrayerTime = nextTime,
                        allPrayers = timings
                    ))

                    val delayDurationMs = nextTime.toEpochMilliseconds() - nowMs
                    if (delayDurationMs > 0L) {
                        delay(delayDurationMs)
                    } else {
                        delay(1000L)
                    }
                }
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
