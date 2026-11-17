@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.imn.iman.data.repository

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.CalculationParameters
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.HighLatitudeRule
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerTimes
import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.batoulapps.adhan2.SunnahTimes
import com.batoulapps.adhan2.data.DateComponents
import com.imn.iman.receiver.PrayerRescheduleReceiver
import com.imn.iman.notifications.PrayerNotificationScheduler
import com.imn.iman.widget.PrayerWidgetProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val Context.prayerTimesDataStore by preferencesDataStore(name = "prayer_times")

fun Instant.toDate(): Date = Date(this.toEpochMilliseconds())

enum class TimesSource { ONLINE, LOCAL, OFFLINE_ESTIMATE }

data class TimesResult(
    val times: Map<String, Date> = emptyMap(),
    val source: TimesSource = TimesSource.ONLINE,
    val provisional: Boolean = false,
    val message: String? = null,
    val nextRetryAt: Long? = null
)

data class ApiTimesValidationResult(
    val isValid: Boolean,
    val parsedTimes: Map<String, Date> = emptyMap(),
    val reasonCode: String? = null
)

private val PROCESS_LATEST_TIMES_STATUS = MutableStateFlow(TimesResult(source = TimesSource.LOCAL))
private var lastCompletedPrayerTimesRequestAt = 0L
private var lastCompletedPrayerTimesRequestSignature: String? = null
private val API_TIME_REGEX = Regex("""^([01][0-9]|2[0-3]):([0-5][0-9])(?: .*)?$""")
private val PRAYER_TIMES_API_MUTEX = Mutex()

fun parseApiTime(value: String, requestedDate: Date): Date? {
    val match = API_TIME_REGEX.matchEntire(value) ?: return null
    val hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues[2].toIntOrNull() ?: return null
    return Calendar.getInstance().apply {
        time = requestedDate
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
}

fun validateApiTimes(
    timings: Map<String, String>,
    responseDate: String,
    requestedDate: Date,
    localTimes: Map<String, Date>,
    preciseLocalCalculation: Boolean
): ApiTimesValidationResult {
    val expectedDate = SimpleDateFormat("dd-MM-yyyy", Locale.ROOT).format(requestedDate)
    if (responseDate != expectedDate) return ApiTimesValidationResult(false, reasonCode = "wrong_date")

    val required = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    val parsed = linkedMapOf<String, Date>()
    required.forEach { name ->
        val value = timings[name] ?: return ApiTimesValidationResult(false, reasonCode = "missing_${name.lowercase()}")
        val time = parseApiTime(value, requestedDate)
            ?: return ApiTimesValidationResult(false, reasonCode = "invalid_${name.lowercase()}")
        parsed[name] = time
    }
    val ordered = required.map { parsed.getValue(it).time }
    if (ordered.zipWithNext().any { (first, second) -> first >= second }) {
        return ApiTimesValidationResult(false, reasonCode = "out_of_order")
    }
    timings["Sunrise"]?.let { value ->
        val sunrise = parseApiTime(value, requestedDate)
            ?: return ApiTimesValidationResult(false, reasonCode = "invalid_sunrise")
        if (sunrise.time <= ordered.first() || sunrise.time >= ordered[1]) {
            return ApiTimesValidationResult(false, reasonCode = "sunrise_out_of_order")
        }
    }

    val maxDeviationMillis = TimeUnit.MINUTES.toMillis(if (preciseLocalCalculation) 20L else 60L)
    for (name in required) {
        val local = localTimes[name] ?: return ApiTimesValidationResult(false, reasonCode = "local_missing_${name.lowercase()}")
        if (kotlin.math.abs(parsed.getValue(name).time - local.time) > maxDeviationMillis) {
            return ApiTimesValidationResult(false, reasonCode = "deviation_${name.lowercase()}")
        }
    }
    return ApiTimesValidationResult(true, parsedTimes = parsed)
}

@Serializable
private data class AlAdhanResponse(
    val code: Int? = null,
    val status: String? = null,
    val data: AlAdhanData? = null
)

@Serializable
private data class AlAdhanData(
    val timings: Map<String, String>? = null,
    val date: AlAdhanDate? = null
)

@Serializable
private data class AlAdhanDate(val gregorian: AlAdhanGregorian? = null)

@Serializable
private data class AlAdhanGregorian(val date: String? = null)

class PrayerTimeRepository(
    private val settingsRepository: SettingsRepository
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val context = settingsRepository.applicationContext
    private val _latestStatus = PROCESS_LATEST_TIMES_STATUS
    val latestStatus: StateFlow<TimesResult> = _latestStatus.asStateFlow()
    val lastOnlineUpdate: Flow<Long?> = context.prayerTimesDataStore.data
        .map { it[longPreferencesKey(KEY_LAST_ONLINE_UPDATE)] }
        .catch { error ->
            Log.w(TAG, "Could not read last online update", error)
            emit(null)
        }
    val lastError: Flow<String?> = context.prayerTimesDataStore.data
        .map { it[stringPreferencesKey(KEY_LAST_ERROR)] }
        .catch { error ->
            Log.w(TAG, "Could not read last prayer-time error", error)
            emit(null)
        }

    @Volatile
    private var lastValidCoordinates = DEFAULT_COORDINATES

    fun usesApiSource(settings: UserSettings): Boolean {
        return settings.prayerSourceType == "API" ||
            !supportsLocalCalculationMethod(settings.prayerCalculationMethod)
    }

    private fun validCoordinates(lat: Double, lng: Double): Boolean =
        lat.isFinite() && lng.isFinite() && lat in -90.0..90.0 && lng in -180.0..180.0

    private data class LocalCalculation(val prayerTimes: PrayerTimes, val coordinates: Pair<Double, Double>)

    fun getLocalPrayerTimes(
        date: Date,
        lat: Double,
        lng: Double,
        methodStr: String,
        madhabStr: String
    ): PrayerTimes {
        val candidates = buildList {
            if (validCoordinates(lat, lng)) add(lat to lng)
            add(lastValidCoordinates)
            add(DEFAULT_COORDINATES)
        }.distinct()
        var lastFailure: Exception? = null
        candidates.forEach { coordinates ->
            try {
                return calculateLocalPrayerTimes(date, coordinates.first, coordinates.second, methodStr, madhabStr)
                    .also { lastValidCoordinates = coordinates }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                lastFailure = error
                Log.w(TAG, "Local calculation failed at coordinates=$coordinates", error)
            }
        }
        throw lastFailure ?: IllegalStateException("No valid prayer-time coordinates")
    }

    private fun calculateLocalPrayerTimes(
        date: Date,
        lat: Double,
        lng: Double,
        methodStr: String,
        madhabStr: String
    ): PrayerTimes {
        val coordinates = Coordinates(lat, lng)
        val calendar = Calendar.getInstance().apply { time = date }
        val dateComponents = DateComponents(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
        val parameters = getCalculationParameters(methodStr).copy(
            madhab = if (madhabStr == "HANAFI") Madhab.HANAFI else Madhab.SHAFI,
            highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT
        )
        return PrayerTimes(coordinates, dateComponents, parameters)
    }

    suspend fun getSafeLocalPrayerTimes(
        date: Date,
        lat: Double,
        lng: Double,
        methodStr: String,
        madhabStr: String
    ): PrayerTimes? = calculateSafeLocalPrayerTimes(date, lat, lng, methodStr, madhabStr)?.prayerTimes

    private suspend fun calculateSafeLocalPrayerTimes(
        date: Date,
        lat: Double,
        lng: Double,
        methodStr: String,
        madhabStr: String
    ): LocalCalculation? {
        return try {
            val preferences = try {
                context.prayerTimesDataStore.data.first()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(TAG, "Could not read saved coordinates; using validated/default coordinates", error)
                null
            }
            val savedLat = preferences?.get(floatPreferencesKey(KEY_LAST_VALID_LAT))?.toDouble()
            val savedLng = preferences?.get(floatPreferencesKey(KEY_LAST_VALID_LNG))?.toDouble()
            val candidates = buildList {
                if (validCoordinates(lat, lng)) add(lat to lng)
                if (savedLat != null && savedLng != null && validCoordinates(savedLat, savedLng)) add(savedLat to savedLng)
                add(DEFAULT_COORDINATES)
            }.distinct()
            candidates.forEach { coordinates ->
                try {
                    val calculated = calculateLocalPrayerTimes(date, coordinates.first, coordinates.second, methodStr, madhabStr)
                    rememberValidCoordinates(coordinates)
                    return LocalCalculation(calculated, coordinates)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.w(TAG, "Local calculation failed at coordinates=$coordinates", error)
                }
            }
            null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(TAG, "Local calculation failed after coordinate fallbacks", error)
            null
        }
    }

    private suspend fun rememberValidCoordinates(coordinates: Pair<Double, Double>) {
        lastValidCoordinates = coordinates
        try {
            val preferences = context.prayerTimesDataStore.data.first()
            if (preferences[floatPreferencesKey(KEY_LAST_VALID_LAT)] != coordinates.first.toFloat() ||
                preferences[floatPreferencesKey(KEY_LAST_VALID_LNG)] != coordinates.second.toFloat()
            ) {
                context.prayerTimesDataStore.edit { values ->
                    values[floatPreferencesKey(KEY_LAST_VALID_LAT)] = coordinates.first.toFloat()
                    values[floatPreferencesKey(KEY_LAST_VALID_LNG)] = coordinates.second.toFloat()
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(TAG, "Could not persist last valid coordinates", error)
        }
    }

    private suspend fun resolveCoordinates(lat: Double, lng: Double): Pair<Double, Double> {
        if (validCoordinates(lat, lng)) return (lat to lng).also { lastValidCoordinates = it }
        val preferences = context.prayerTimesDataStore.data.first()
        val savedLat = preferences[floatPreferencesKey(KEY_LAST_VALID_LAT)]?.toDouble()
        val savedLng = preferences[floatPreferencesKey(KEY_LAST_VALID_LNG)]?.toDouble()
        return if (savedLat != null && savedLng != null && validCoordinates(savedLat, savedLng)) {
            (savedLat to savedLng).also { lastValidCoordinates = it }
        } else DEFAULT_COORDINATES
    }

    fun getSunnahTimes(prayerTimes: PrayerTimes): SunnahTimes = SunnahTimes(prayerTimes)

    suspend fun getEffectivePrayerDateTimes(
        date: Date,
        settings: UserSettings
    ): Map<String, Date> = getTimesResult(date, settings).times

    suspend fun getTimesResult(
        date: Date,
        settings: UserSettings,
        retryNow: Boolean = false
    ): TimesResult {
        return try {
            getTimesResultSafely(date, settings, retryNow)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(TAG, "Prayer time lookup failed safely", error)
            TimesResult(
                source = fallbackSource(settings),
                provisional = usesApiSource(settings),
                message = shortReason(error)
            ).also { _latestStatus.value = it }
        }
    }

    private suspend fun getTimesResultSafely(date: Date, settings: UserSettings, retryNow: Boolean): TimesResult {
        val isApiSource = usesApiSource(settings)
        val signature = prayerSettingsSignature(settings)
        getSavedPrayerDateTimes(date, settings)?.let { cached ->
            Log.d(TAG, "Cache hit date=${dateKey(date)} source=${cached.source}")
            return cached.also { _latestStatus.value = it }
        }
        Log.d(TAG, "Cache miss date=${dateKey(date)}")
        if (!retryNow && isApiSource) {
            val preferences = context.prayerTimesDataStore.data.first()
            getProvisionalPrayerDateTimes(date, settings, preferences)?.let { provisional ->
                Log.d(TAG, "Provisional cache hit date=${dateKey(date)} retryAt=${provisional.nextRetryAt}")
                scheduleRetryAlarm(provisional.nextRetryAt)
                return provisional.also { _latestStatus.value = it }
            }
        }

        val localCalculation = calculateSafeLocalPrayerTimes(
            date = date,
            lat = settings.locationLat.toDouble(),
            lng = settings.locationLng.toDouble(),
            methodStr = settings.prayerCalculationMethod,
            madhabStr = settings.prayerMadhab
        )
        val localPrayerTimes = localCalculation?.prayerTimes
        val localTimes = localPrayerTimes?.let(::toPrayerDateTimes).orEmpty()
        val localResultTimes = applyOffsets(localTimes, settings)

        if (!isApiSource) {
            if (localResultTimes.size != 5) {
                return emptyResult(settings, "local_calculation_failed", false)
            }
            savePrayerDateTimes(date, settings, localResultTimes, TimesSource.LOCAL)
            return TimesResult(localResultTimes, TimesSource.LOCAL).also {
                Log.d(TAG, "Source chosen=LOCAL date=${dateKey(date)}")
                _latestStatus.value = it
            }
        }

        val preciseLocal = supportsLocalCalculationMethod(settings.prayerCalculationMethod)
        val retryPreferences = context.prayerTimesDataStore.data.first()
        val storedRetrySignature = retryPreferences[stringPreferencesKey(KEY_RETRY_SIGNATURE)]
        val storedRetryAt = retryPreferences[longPreferencesKey(KEY_RETRY_AT)]
        val storedAttempt = if (storedRetrySignature == signature) {
            retryPreferences[longPreferencesKey(KEY_RETRY_ATTEMPT)]?.toInt() ?: 0
        } else 0

        if (!retryNow && storedRetrySignature == signature && storedRetryAt != null && storedRetryAt > System.currentTimeMillis()) {
            return saveProvisionalResult(
                date, settings, localResultTimes, fallbackSource(settings),
                retryPreferences[stringPreferencesKey(KEY_LAST_ERROR)] ?: "retry_backoff",
                storedRetryAt, storedAttempt, updateGlobalState = false
            )
        }

        val previousProvisional = getProvisionalPrayerDateTimes(date, settings, retryPreferences, ignoreExpiry = true)
        return PRAYER_TIMES_API_MUTEX.withLock {
            getSavedPrayerDateTimes(date, settings)?.let { cached ->
                Log.d(TAG, "Cache hit after request lock date=${dateKey(date)} source=${cached.source}")
                return@withLock cached.also { _latestStatus.value = it }
            }
            val currentPreferences = context.prayerTimesDataStore.data.first()
            if (retryNow && lastCompletedPrayerTimesRequestSignature == signature &&
                System.currentTimeMillis() - lastCompletedPrayerTimesRequestAt < FORCED_RETRY_DEBOUNCE_MS
            ) {
                getProvisionalPrayerDateTimes(date, settings, currentPreferences, ignoreExpiry = true)?.let { provisional ->
                    Log.d(TAG, "Skipping duplicate forced retry date=${dateKey(date)}")
                    return@withLock provisional.also { _latestStatus.value = it }
                }
            }
            if (!retryNow) {
                val currentRetryAt = currentPreferences[longPreferencesKey(KEY_RETRY_AT)]
                if (currentPreferences[stringPreferencesKey(KEY_RETRY_SIGNATURE)] == signature &&
                    currentRetryAt != null && currentRetryAt > System.currentTimeMillis()
                ) {
                    val attempt = currentPreferences[longPreferencesKey(KEY_RETRY_ATTEMPT)]?.toInt() ?: 0
                    return@withLock saveProvisionalResult(
                        date, settings, localResultTimes, fallbackSource(settings), "retry_backoff",
                        currentRetryAt, attempt, updateGlobalState = false
                    )
                }
            }

            val apiCoordinates = localCalculation?.coordinates
                ?: resolveCoordinates(settings.locationLat.toDouble(), settings.locationLng.toDouble())
            val outcome = requestAlAdhanTimes(
                date, settings.prayerMadhab, apiCoordinates.first, apiCoordinates.second,
                settings.prayerCalculationMethod
            )
            lastCompletedPrayerTimesRequestAt = System.currentTimeMillis()
            lastCompletedPrayerTimesRequestSignature = signature
            val validation = if (outcome is ApiRequestResult.Success && localTimes.size == 5) {
                validateApiTimes(
                    timings = outcome.timings,
                    responseDate = outcome.responseDate,
                    requestedDate = date,
                    localTimes = localTimes,
                    preciseLocalCalculation = preciseLocal
                )
            } else null

            val failureReason = when {
                outcome is ApiRequestResult.Failure -> outcome.reason
                outcome is ApiRequestResult.Success && localTimes.size != 5 -> "local_calculation_failed"
                validation?.isValid == false -> "validation_${validation.reasonCode}"
                else -> null
            }
            if (failureReason != null) {
                if (failureReason.startsWith("validation_")) {
                    Log.w(TAG, "Validation failure reason=${failureReason.removePrefix("validation_")}")
                }
                val latestPreferences = context.prayerTimesDataStore.data.first()
                val latestAttempt = if (latestPreferences[stringPreferencesKey(KEY_RETRY_SIGNATURE)] == signature) {
                    latestPreferences[longPreferencesKey(KEY_RETRY_ATTEMPT)]?.toInt() ?: 0
                } else 0
                val nextAttempt = latestAttempt + 1
                val retryAt = System.currentTimeMillis() + retryDelayMillis(latestAttempt)
                saveProvisionalResult(
                    date = date,
                    settings = settings,
                    times = localResultTimes,
                    source = fallbackSource(settings),
                    message = failureReason,
                    nextRetryAt = retryAt,
                    attempt = nextAttempt,
                    updateGlobalState = true
                )
            } else {
                val rawOnlineTimes = validation!!.parsedTimes
                val onlineTimes = applyOffsets(rawOnlineTimes, settings)
                savePrayerDateTimes(date, settings, onlineTimes, TimesSource.ONLINE)
                clearProvisionalState(date, signature)
                context.prayerTimesDataStore.edit { preferences ->
                    preferences[longPreferencesKey(KEY_LAST_ONLINE_UPDATE)] = System.currentTimeMillis()
                    preferences.remove(stringPreferencesKey(KEY_LAST_ERROR))
                }
                val result = TimesResult(onlineTimes, TimesSource.ONLINE).also {
                    _latestStatus.value = it
                    Log.d(TAG, "Source chosen=ONLINE date=${dateKey(date)}")
                }
                if (previousProvisional != null && prayerTimesDiffer(previousProvisional.times, onlineTimes)) {
                    Log.d(TAG, "Online timings replaced provisional values; refreshing reminders and widget")
                    try {
                        PrayerNotificationScheduler.syncInApplicationScope(context)
                        PrayerWidgetProvider.forceWidgetRefresh(context)
                    } catch (error: Exception) {
                        Log.w(TAG, "Could not refresh prayer surfaces after online replacement", error)
                    }
                }
                result
            }
        }
    }

    private fun fallbackSource(settings: UserSettings): TimesSource =
        if (supportsLocalCalculationMethod(settings.prayerCalculationMethod)) TimesSource.LOCAL else TimesSource.OFFLINE_ESTIMATE

    private suspend fun emptyResult(settings: UserSettings, reason: String, provisional: Boolean): TimesResult {
        Log.d(TAG, "Source chosen=${fallbackSource(settings)}")
        Log.w(TAG, "No prayer times available reason=$reason")
        context.prayerTimesDataStore.edit { it[stringPreferencesKey(KEY_LAST_ERROR)] = reason }
        return TimesResult(source = fallbackSource(settings), provisional = provisional, message = reason)
            .also { _latestStatus.value = it }
    }

    private fun applyOffsets(times: Map<String, Date>, settings: UserSettings): Map<String, Date> {
        val offsets = mapOf(
            "Fajr" to settings.fajrOffsetMinutes,
            "Dhuhr" to settings.dhuhrOffsetMinutes,
            "Asr" to settings.asrOffsetMinutes,
            "Maghrib" to settings.maghribOffsetMinutes,
            "Isha" to settings.ishaOffsetMinutes
        )
        if (times.keys.containsAll(offsets.keys).not()) return emptyMap()
        return offsets.mapValues { (name, minutes) -> Date(times.getValue(name).time + minutes * 60_000L) }
    }

    private fun toPrayerDateTimes(prayerTimes: PrayerTimes): Map<String, Date> = linkedMapOf(
        "Fajr" to normalizeLocalTime(prayerTimes.fajr.toDate()),
        "Dhuhr" to normalizeLocalTime(prayerTimes.dhuhr.toDate()),
        "Asr" to normalizeLocalTime(prayerTimes.asr.toDate()),
        "Maghrib" to normalizeLocalTime(prayerTimes.maghrib.toDate()),
        "Isha" to normalizeLocalTime(prayerTimes.isha.toDate())
    )

    private fun normalizeLocalTime(date: Date): Date = Calendar.getInstance().apply {
        time = date
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    suspend fun getPrayerDateTimesForScheduling(
        date: Date,
        settings: UserSettings,
        retryNow: Boolean = false
    ): Map<String, Date> = getTimesResult(date, settings, retryNow).times

    private suspend fun getSavedPrayerDateTimes(date: Date, settings: UserSettings): TimesResult? {
        val key = dateKey(date)
        val preferences = context.prayerTimesDataStore.data.first()
        if (preferences[stringPreferencesKey("date_$key")] != key ||
            preferences[stringPreferencesKey("settings_$key")] != prayerSettingsSignature(settings)
        ) return null
        val source = preferences[stringPreferencesKey("source_$key")]?.let { stored ->
            TimesSource.values().firstOrNull { it.name == stored }
        } ?: return null
        if (source == TimesSource.OFFLINE_ESTIMATE) return null
        val times = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").mapNotNull { name ->
            val millis = preferences[longPreferencesKey("${name.lowercase()}_$key")] ?: return@mapNotNull null
            name to Date(millis)
        }.toMap()
        return times.takeIf { it.size == 5 }?.let { TimesResult(it, source) }
    }

    private suspend fun savePrayerDateTimes(
        date: Date,
        settings: UserSettings,
        times: Map<String, Date>,
        source: TimesSource
    ) {
        val key = dateKey(date)
        context.prayerTimesDataStore.edit { preferences ->
            preferences[stringPreferencesKey("date_$key")] = key
            preferences[stringPreferencesKey("settings_$key")] = prayerSettingsSignature(settings)
            preferences[stringPreferencesKey("source_$key")] = source.name
            times.forEach { (name, time) -> preferences[longPreferencesKey("${name.lowercase()}_$key")] = time.time }
        }
    }

    private suspend fun getProvisionalPrayerDateTimes(
        date: Date,
        settings: UserSettings,
        preferences: androidx.datastore.preferences.core.Preferences,
        ignoreExpiry: Boolean = false
    ): TimesResult? {
        val signature = prayerSettingsSignature(settings)
        val retryAt = preferences[longPreferencesKey(KEY_RETRY_AT)] ?: return null
        if (preferences[stringPreferencesKey(KEY_RETRY_SIGNATURE)] != signature ||
            (!ignoreExpiry && retryAt <= System.currentTimeMillis())
        ) return null
        val key = dateKey(date)
        if (preferences[stringPreferencesKey("provisional_settings_$key")] != signature ||
            preferences[booleanPreferencesKey("provisional_present_$key")] != true
        ) return null
        val times = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").mapNotNull { name ->
            val millis = preferences[longPreferencesKey("provisional_${name.lowercase()}_$key")] ?: return@mapNotNull null
            name to Date(millis)
        }.toMap()
        if (times.isNotEmpty() && times.size != 5) return null
        val source = fallbackSource(settings)
        return TimesResult(
            times = times,
            source = source,
            provisional = true,
            message = preferences[stringPreferencesKey(KEY_LAST_ERROR)],
            nextRetryAt = retryAt
        )
    }

    private suspend fun saveProvisionalResult(
        date: Date,
        settings: UserSettings,
        times: Map<String, Date>,
        source: TimesSource,
        message: String,
        nextRetryAt: Long,
        attempt: Int,
        updateGlobalState: Boolean
    ): TimesResult {
        val signature = prayerSettingsSignature(settings)
        val key = dateKey(date)
        context.prayerTimesDataStore.edit { preferences ->
            preferences[stringPreferencesKey("provisional_settings_$key")] = signature
            preferences[booleanPreferencesKey("provisional_present_$key")] = true
            listOf("fajr", "dhuhr", "asr", "maghrib", "isha").forEach {
                preferences.remove(longPreferencesKey("provisional_${it}_$key"))
            }
            times.forEach { (name, time) -> preferences[longPreferencesKey("provisional_${name.lowercase()}_$key")] = time.time }
            if (updateGlobalState) {
                preferences.remove(stringPreferencesKey("date_$key"))
                preferences.remove(stringPreferencesKey("settings_$key"))
                preferences.remove(stringPreferencesKey("source_$key"))
                listOf("fajr", "dhuhr", "asr", "maghrib", "isha").forEach {
                    preferences.remove(longPreferencesKey("${it}_$key"))
                }
                preferences[stringPreferencesKey(KEY_RETRY_SIGNATURE)] = signature
                preferences[longPreferencesKey(KEY_RETRY_AT)] = nextRetryAt
                preferences[longPreferencesKey(KEY_RETRY_ATTEMPT)] = attempt.toLong()
                preferences[stringPreferencesKey(KEY_LAST_ERROR)] = message
            }
        }
        val result = TimesResult(times, source, provisional = true, message = message, nextRetryAt = nextRetryAt)
        _latestStatus.value = result
        Log.d(TAG, "Source chosen=$source date=$key")
        Log.d(TAG, "Fallback used source=$source date=$key")
        Log.d(TAG, "Provisional state nextRetryAt=${Date(nextRetryAt)} attempt=$attempt")
        scheduleRetryAlarm(nextRetryAt)
        return result
    }

    private suspend fun clearProvisionalState(date: Date, signature: String) {
        val key = dateKey(date)
        context.prayerTimesDataStore.edit { preferences ->
            preferences.remove(stringPreferencesKey("provisional_settings_$key"))
            preferences.remove(booleanPreferencesKey("provisional_present_$key"))
            listOf("fajr", "dhuhr", "asr", "maghrib", "isha").forEach {
                preferences.remove(longPreferencesKey("provisional_${it}_$key"))
            }
            if (preferences[stringPreferencesKey(KEY_RETRY_SIGNATURE)] == signature) {
                preferences.remove(stringPreferencesKey(KEY_RETRY_SIGNATURE))
                preferences.remove(longPreferencesKey(KEY_RETRY_AT))
                preferences.remove(longPreferencesKey(KEY_RETRY_ATTEMPT))
                preferences.remove(stringPreferencesKey(KEY_LAST_ERROR))
            }
        }
        cancelRetryAlarm()
    }

    private fun retryDelayMillis(attempt: Int): Long = when (attempt) {
        0 -> TimeUnit.MINUTES.toMillis(15)
        1 -> TimeUnit.MINUTES.toMillis(30)
        2 -> TimeUnit.MINUTES.toMillis(60)
        else -> TimeUnit.HOURS.toMillis(2)
    }

    private fun prayerTimesDiffer(previous: Map<String, Date>, online: Map<String, Date>): Boolean =
        listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").any { name ->
            val old = previous[name]?.time ?: return@any true
            val new = online[name]?.time ?: return@any true
            kotlin.math.abs(old - new) >= TimeUnit.MINUTES.toMillis(1)
        }

    private suspend fun scheduleRetryAlarm(retryAt: Long?) {
        if (retryAt == null) return
        try {
            val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
            val intent = Intent(context, PrayerRescheduleReceiver::class.java).apply {
                action = ACTION_PRAYER_TIMES_RETRY
                data = Uri.parse("iman://prayer-times/retry")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, RETRY_RESCHEDULE_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, retryAt, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, retryAt, pendingIntent)
            }
            Log.d(TAG, "Scheduled retry alarm at ${Date(retryAt)}")
        } catch (error: Exception) {
            Log.w(TAG, "Could not schedule prayer-time retry alarm", error)
        }
    }

    private fun cancelRetryAlarm() {
        try {
            val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
            val intent = Intent(context, PrayerRescheduleReceiver::class.java).apply {
                action = ACTION_PRAYER_TIMES_RETRY
                data = Uri.parse("iman://prayer-times/retry")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, RETRY_RESCHEDULE_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (error: Exception) {
            Log.w(TAG, "Could not cancel prayer-time retry alarm", error)
        }
    }

    private fun dateKey(date: Date): String = SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(date)

    private fun prayerSettingsSignature(settings: UserSettings): String = listOf(
        settings.locationLat, settings.locationLng, settings.useGpsLocation,
        settings.prayerCalculationMethod, settings.prayerMadhab, settings.prayerSourceType,
        settings.fajrOffsetMinutes, settings.dhuhrOffsetMinutes, settings.asrOffsetMinutes,
        settings.maghribOffsetMinutes, settings.ishaOffsetMinutes,
        settings.fajrPrePrayerMinutes, settings.dhuhrPrePrayerMinutes, settings.asrPrePrayerMinutes,
        settings.maghribPrePrayerMinutes, settings.ishaPrePrayerMinutes,
        Calendar.getInstance().timeZone.id
    ).joinToString("|")

    suspend fun getExtraPrayerDateTimes(
        date: Date,
        settings: UserSettings
    ): Map<String, Date> {
        return try {
            val localPrayerTimes = getSafeLocalPrayerTimes(
                date, settings.locationLat.toDouble(), settings.locationLng.toDouble(),
                settings.prayerCalculationMethod, settings.prayerMadhab
            ) ?: return emptyMap()
            val nextDayPrayerTimes = getSafeLocalPrayerTimes(
                Date(date.time + TimeUnit.DAYS.toMillis(1)), settings.locationLat.toDouble(),
                settings.locationLng.toDouble(), settings.prayerCalculationMethod, settings.prayerMadhab
            ) ?: return emptyMap()
            val localFajr = normalizeLocalTime(localPrayerTimes.fajr.toDate())
            val localMaghrib = normalizeLocalTime(localPrayerTimes.maghrib.toDate())
            val sunnahTimes = getSunnahTimes(localPrayerTimes)
            val firstThirdMillis = localMaghrib.time +
                ((nextDayPrayerTimes.fajr.toDate().time - localMaghrib.time) / 3L)
            mapOf(
                "IMSAK" to Date(localFajr.time - TimeUnit.MINUTES.toMillis(10)),
                "SUNRISE" to normalizeLocalTime(localPrayerTimes.sunrise.toDate()),
                "SUNSET" to localMaghrib,
                "FIRST_THIRD" to normalizeLocalTime(Date(firstThirdMillis)),
                "MIDNIGHT" to normalizeLocalTime(sunnahTimes.middleOfTheNight.toDate()),
                "LAST_THIRD" to normalizeLocalTime(sunnahTimes.lastThirdOfTheNight.toDate())
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(TAG, "Extra prayer timings unavailable", error)
            emptyMap()
        }
    }

    suspend fun getFallbackPrayerTimes(
        date: Date,
        lat: Double,
        lng: Double,
        methodStr: String,
        madhabStr: String
    ): Map<String, String>? {
        return try {
            when (val response = requestAlAdhanTimes(date, madhabStr, lat, lng, methodStr)) {
                is ApiRequestResult.Success -> response.timings
                is ApiRequestResult.Failure -> null
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(TAG, "AlAdhan request failed safely: ${shortReason(error)}")
            null
        }
    }

    private sealed interface ApiRequestResult {
        data class Success(val timings: Map<String, String>, val responseDate: String) : ApiRequestResult
        data class Failure(val reason: String, val retryable: Boolean) : ApiRequestResult
    }

    private suspend fun requestAlAdhanTimes(
        date: Date,
        madhabStr: String,
        lat: Double,
        lng: Double,
        methodStr: String
    ): ApiRequestResult = withContext(Dispatchers.IO) {
        val resolvedCoordinates = if (validCoordinates(lat, lng)) lat to lng else resolveCoordinates(lat, lng)
        val requestLat = resolvedCoordinates.first
        val requestLng = resolvedCoordinates.second
        val calendar = Calendar.getInstance().apply { time = date }
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        val methodId = getApiMethodId(methodStr)
        val school = if (madhabStr == "HANAFI") 1 else 0
        val timeZoneString = URLEncoder.encode(calendar.timeZone.id, Charsets.UTF_8.name())
        val requestUrl = buildString {
            append("https://api.aladhan.com/v1/timings/$day-$month-$year")
            append("?latitude=$requestLat&longitude=$requestLng&method=$methodId")
            if (methodId == 99) {
                val params = getCalculationParameters(methodStr)
                append("&methodSettings=${params.fajrAngle},null,${params.ishaAngle}")
            }
            append("&school=$school&timezonestring=$timeZoneString")
            if (methodStr == "MOON_SIGHTING_COMMITTEE") append("&shafaq=general")
        }

        for (attempt in 0..2) {
            Log.d(TAG, "Request start attempt=${attempt + 1} date=$day-$month-$year method=$methodId school=$school")
            val outcome = try {
                withTimeout(15_000L) { requestAlAdhanOnce(requestUrl) }
            } catch (timeout: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                ApiRequestResult.Failure("timeout", true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (timeout: SocketTimeoutException) {
                ApiRequestResult.Failure("timeout", true)
            } catch (network: IOException) {
                ApiRequestResult.Failure("network_error", true)
            } catch (error: Exception) {
                ApiRequestResult.Failure(shortReason(error), false)
            }
            val status = when (outcome) {
                is ApiRequestResult.Success -> "200"
                is ApiRequestResult.Failure -> outcome.reason
            }
            Log.d(TAG, "Request end attempt=${attempt + 1} status=$status")
            if (outcome !is ApiRequestResult.Failure || !outcome.retryable || attempt == 2) {
                return@withContext outcome
            }
            delay(if (attempt == 0) 1_000L else 3_000L)
        }
        ApiRequestResult.Failure("request_failed", false)
    }

    private fun requestAlAdhanOnce(requestUrl: String): ApiRequestResult {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL(requestUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 8_000
            connection.readTimeout = 10_000
            connection.requestMethod = "GET"
            val httpStatus = connection.responseCode
            Log.d(TAG, "HTTP response status=$httpStatus")
            val stream = if (httpStatus in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.use { input -> input.bufferedReader(Charsets.UTF_8).readText() }
            if (httpStatus != HttpURLConnection.HTTP_OK) {
                return ApiRequestResult.Failure("http_$httpStatus", httpStatus == 429 || httpStatus in 500..599)
            }
            if (responseBody.isNullOrBlank()) return ApiRequestResult.Failure("empty_response", false)
            val response = try {
                json.decodeFromString<AlAdhanResponse>(responseBody)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return ApiRequestResult.Failure("invalid_json", false)
            }
            if (response.code != 200) return ApiRequestResult.Failure("api_code_${response.code ?: "missing"}", false)
            if (response.status != "OK") return ApiRequestResult.Failure("api_status_${response.status ?: "missing"}", false)
            val timings = response.data?.timings ?: return ApiRequestResult.Failure("missing_timings", false)
            val responseDate = response.data.date?.gregorian?.date
                ?: return ApiRequestResult.Failure("missing_date", false)
            ApiRequestResult.Success(timings, responseDate)
        } finally {
            connection?.disconnect()
        }
    }

    private fun shortReason(error: Throwable): String =
        error.message?.take(80)?.takeIf { it.isNotBlank() } ?: "calculation_failed"

    private fun getCalculationParameters(methodStr: String): CalculationParameters {
        return when (methodStr) {
            "SHIA_ITHNA_ASHARI" -> CalculationParameters(fajrAngle = 16.0, ishaAngle = 14.0)
            "MUSLIM_WORLD_LEAGUE" -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
            "EGYPTIAN" -> CalculationMethod.EGYPTIAN.parameters
            "KARACHI" -> CalculationMethod.KARACHI.parameters
            "UMM_AL_QURA" -> CalculationMethod.UMM_AL_QURA.parameters
            "DUBAI" -> CalculationMethod.DUBAI.parameters
            "MOON_SIGHTING_COMMITTEE" -> CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters
            "NORTH_AMERICA" -> CalculationMethod.NORTH_AMERICA.parameters
            "KUWAIT" -> CalculationMethod.KUWAIT.parameters
            "QATAR" -> CalculationMethod.QATAR.parameters
            "SINGAPORE" -> CalculationMethod.SINGAPORE.parameters
            "ALGERIA" -> CalculationParameters(fajrAngle = 18.0, ishaAngle = 17.0)
            "TUNISIA" -> CalculationParameters(fajrAngle = 18.0, ishaAngle = 18.0)
            "FRANCE_UOIF" -> CalculationParameters(fajrAngle = 12.0, ishaAngle = 12.0)
            "FRANCE_15" -> CalculationParameters(fajrAngle = 15.0, ishaAngle = 15.0)
            "FRANCE_18" -> CalculationParameters(fajrAngle = 18.0, ishaAngle = 17.0)
            "TEHRAN" -> CalculationMethod.OTHER.parameters
            "TURKEY" -> CalculationMethod.OTHER.parameters
            "RUSSIA" -> CalculationParameters(fajrAngle = 16.0, ishaAngle = 15.0)
            "MALAYSIA" -> CalculationParameters(fajrAngle = 18.0, ishaAngle = 18.0)
            "INDONESIA" -> CalculationParameters(fajrAngle = 20.0, ishaAngle = 18.0)
            "MOROCCO" -> CalculationParameters(fajrAngle = 19.0, ishaAngle = 17.0)
            "JORDAN" -> CalculationParameters(fajrAngle = 18.0, ishaAngle = 18.0)
            "GULF_REGION" -> CalculationParameters(fajrAngle = 19.5, ishaAngle = 0.0, ishaInterval = 90)
            "PORTUGAL" -> CalculationParameters(fajrAngle = 18.0, ishaAngle = 15.0)
            else -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
        }
    }

    private fun getApiMethodId(methodStr: String): Int {
        return when (methodStr) {
            "SHIA_ITHNA_ASHARI" -> 0
            "KARACHI" -> 1
            "NORTH_AMERICA" -> 2
            "MUSLIM_WORLD_LEAGUE" -> 3
            "UMM_AL_QURA" -> 4
            "EGYPTIAN" -> 5
            "TEHRAN" -> 7
            "GULF_REGION" -> 8
            "KUWAIT" -> 9
            "QATAR" -> 10
            "SINGAPORE" -> 11
            "FRANCE_UOIF" -> 12
            "TURKEY" -> 13
            "RUSSIA" -> 14
            "MOON_SIGHTING_COMMITTEE" -> 15
            "DUBAI" -> 16
            "MALAYSIA" -> 17
            "TUNISIA" -> 18
            "ALGERIA" -> 19
            "INDONESIA" -> 20
            "MOROCCO" -> 21
            "PORTUGAL" -> 22
            "JORDAN" -> 23
            else -> 99 // Fallback to custom for others or default
        }
    }

    private companion object {
        const val TAG = "ImanTimes"
        const val ACTION_PRAYER_TIMES_RETRY = "com.imn.iman.action.PRAYER_TIMES_RETRY"
        const val RETRY_RESCHEDULE_REQUEST_CODE = 0x50525452
        const val FORCED_RETRY_DEBOUNCE_MS = 10_000L
        const val KEY_LAST_VALID_LAT = "last_valid_lat"
        const val KEY_LAST_VALID_LNG = "last_valid_lng"
        const val KEY_LAST_ONLINE_UPDATE = "last_online_update"
        const val KEY_LAST_ERROR = "last_error"
        const val KEY_RETRY_SIGNATURE = "retry_settings_signature"
        const val KEY_RETRY_AT = "retry_at"
        const val KEY_RETRY_ATTEMPT = "retry_attempt"
        val DEFAULT_COORDINATES = 21.4225 to 39.8262
    }
}
