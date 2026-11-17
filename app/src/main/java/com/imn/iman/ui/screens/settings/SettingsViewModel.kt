package com.imn.iman.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.imn.iman.R
import com.imn.iman.i18n.LocaleHelper

import com.imn.iman.data.repository.QuranScriptFontRepository
import com.imn.iman.data.repository.LocationNameResolver
import com.imn.iman.data.repository.distanceMeters
import com.imn.iman.data.repository.isDefaultLocation
import com.imn.iman.data.repository.PrayerTimeRepository
import com.imn.iman.data.repository.SettingsRepository
import com.imn.iman.data.repository.UserSettings

import com.imn.iman.widget.PrayerWidgetProvider
import com.imn.iman.notifications.PrayerNotificationScheduler
import kotlinx.coroutines.Job

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val LOCATION_TAG = "ImanLocation"

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val appContext: Context,
    val prayerTimeRepository: PrayerTimeRepository = PrayerTimeRepository(repository),
) : ViewModel() {
    private val locationNameResolver = LocationNameResolver(appContext)
    private var hasAttemptedGpsRefresh = false
    private val _unsupportedLocalSourceEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val unsupportedLocalSourceEvents = _unsupportedLocalSourceEvents.asSharedFlow()
    val availableQuranScripts = MutableStateFlow(QuranScriptFontRepository.availableScripts(appContext))

    val settings: StateFlow<UserSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserSettings(
            hapticsEnabled = true,
            dynamicColor = false,
            pureBlack = false,
            keepScreenAwake = true,

            themeMode = "System",
            showStepTimer = true,
            fluidTransitions = true,
            vibrationOnCount = true,
            showLapCounter = true,

            colorSeed = "default",
            tasbeehListMode = true,
            startupTab = "home",
            tasbihSoundEnabled = true,
            tasbihSoundId = "1",
            lastSeenAppVersionCode = 0,
        )
    )

    val isSettingsLoaded: StateFlow<Boolean> = repository.settingsFlow
        .map { true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    init {
        viewModelScope.launch { repository.repairUnsupportedLocalPrayerSource() }
        viewModelScope.launch {
            settings.collect { current ->
                if (current.useGpsLocation && !hasAttemptedGpsRefresh) {
                    hasAttemptedGpsRefresh = true
                    refreshLocation()
                }
                if (!current.useGpsLocation) {
                    hasAttemptedGpsRefresh = false
                }
            }
        }

        PrayerNotificationScheduler.launchInApplicationScope {
            repository.cleanupLegacyLocationLabel()
            repository.settingsFlow.first()
            ensureLocationName()
        }
    }

    fun ensureLocationName() {
        synchronized(locationNameJobLock) {
            locationNameJob?.cancel()
            locationNameJob = PrayerNotificationScheduler.launchInApplicationScope {
                val current = repository.settingsFlow.first()
                val latitude = current.locationLat.toDouble()
                val longitude = current.locationLng.toDouble()
                val languageTag = locationNameResolver.currentLanguageTag()

                if (isDefaultLocation(latitude, longitude) && current.locationLabel.isNotBlank()) {
                    Log.d(LOCATION_TAG, "Lookup skipped: default Makkah coordinates have a valid label")
                    return@launchInApplicationScope
                }

                val skipReason = when {
                    current.locationLabel.isBlank() -> "label is blank"
                    current.locationCountryCode.isNullOrBlank() -> "country code is blank"
                    current.locationLabelLat == null || current.locationLabelLng == null -> "label was never resolved"
                    distanceMeters(
                        latitude,
                        longitude,
                        current.locationLabelLat.toDouble(),
                        current.locationLabelLng.toDouble()
                    ) > 500.0 -> "saved coordinates moved more than 500 m"
                    current.locationLabelLang != languageTag -> "app language changed"
                    else -> "label is current"
                }
                if (skipReason == "label is current") {
                    Log.d(LOCATION_TAG, "Lookup skipped: $skipReason")
                    return@launchInApplicationScope
                }

                Log.d(LOCATION_TAG, "Lookup requested for $latitude, $longitude ($skipReason)")
                val place = locationNameResolver.resolve(latitude, longitude)
                if (place == null) {
                    Log.w(LOCATION_TAG, "Lookup failed for $latitude, $longitude")
                    return@launchInApplicationScope
                }

                val latest = repository.settingsFlow.first()
                if (kotlin.math.abs(latest.locationLat - current.locationLat) > 0.0001f ||
                    kotlin.math.abs(latest.locationLng - current.locationLng) > 0.0001f
                ) {
                    Log.d(LOCATION_TAG, "Result discarded as stale for $latitude, $longitude")
                    return@launchInApplicationScope
                }
                repository.updateResolvedLocation(
                    current.locationLat,
                    current.locationLng,
                    place,
                    languageTag
                )
                PrayerWidgetProvider.forceWidgetRefresh(appContext)
            }
        }
    }

    fun toggleHaptics(enabled: Boolean) {
        viewModelScope.launch { repository.updateHaptics(enabled) }
    }

    fun toggleDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.updateDynamicColor(enabled) }
    }

    fun togglePureBlack(enabled: Boolean) {
        viewModelScope.launch { repository.updatePureBlack(enabled) }
    }

    fun toggleKeepScreenAwake(enabled: Boolean) {
        viewModelScope.launch { repository.updateKeepScreenAwake(enabled) }
    }


    fun setThemeMode(mode: String) {
        viewModelScope.launch { repository.updateThemeMode(mode) }
    }

    fun toggleShowStepTimer(show: Boolean) {
        viewModelScope.launch { repository.updateShowStepTimer(show) }
    }

    fun toggleFluidTransitions(enabled: Boolean) {
        viewModelScope.launch { repository.updateFluidTransitions(enabled) }
    }

    fun toggleVibrationOnCount(enabled: Boolean) {
        viewModelScope.launch { repository.updateVibrationOnCount(enabled) }
    }

    fun toggleShowLapCounter(show: Boolean) {
        viewModelScope.launch { repository.updateShowLapCounter(show) }
    }


    fun setColorSeed(seed: String) {
        viewModelScope.launch { repository.updateColorSeed(seed) }
    }

    fun setTasbihBackgroundColor(color: String) {
        viewModelScope.launch { repository.updateTasbihBackgroundColor(color) }
    }

    fun setTasbihBeadColor(color: String) {
        viewModelScope.launch { repository.updateTasbihBeadColor(color) }
    }

    fun setTasbihBeadMode(mode: String) {
        viewModelScope.launch { repository.updateTasbihBeadMode(mode) }
    }

    fun setTasbihTextColor(color: String) {
        viewModelScope.launch { repository.updateTasbihTextColor(color) }
    }

    fun toggleTasbeehListMode(isList: Boolean) {
        viewModelScope.launch { repository.updateTasbeehListMode(isList) }
    }


    fun toggleTasbeehStealthModeAllowed(enabled: Boolean) {
        viewModelScope.launch { repository.updateTasbeehStealthModeAllowed(enabled) }
    }

    fun toggleTasbeehVolumeEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setTasbeehVolumeEnabled(enabled) }
    }

    fun toggleTasbeehVolumeAnimation(enabled: Boolean) {
        viewModelScope.launch { repository.setTasbeehVolumeAnimation(enabled) }
    }

    fun toggleTasbihSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setTasbihSoundEnabled(enabled) }
    }

    fun setTasbihSoundId(id: String) {
        viewModelScope.launch { repository.setTasbihSoundId(id) }
    }


    fun setStudyNoteDismissed(dismissed: Boolean) {
        viewModelScope.launch { repository.setStudyNoteDismissed(dismissed) }
    }

    fun markReleaseNotesSeen(versionCode: Int) {
        viewModelScope.launch { repository.updateLastSeenAppVersionCode(versionCode) }
    }




    fun setStartupTab(route: String) {
        viewModelScope.launch { repository.updateStartupTab(route) }
    }


    fun setReadingTheme(theme: String) {
        viewModelScope.launch { repository.updateReadingTheme(theme) }
    }

    fun setArabicSizeSp(size: Float) {
        viewModelScope.launch { repository.updateArabicSizeSp(size) }
    }

    fun setTranslationSizeSp(size: Float) {
        viewModelScope.launch { repository.updateTranslationSizeSp(size) }
    }

    fun toggleShowTranslation(show: Boolean) {
        viewModelScope.launch { repository.updateShowTranslation(show) }
    }


    fun toggleReadingKeepScreenOn(keep: Boolean) {
        viewModelScope.launch { repository.updateReadingKeepScreenOn(keep) }
    }

    fun toggleShowContinueReading(show: Boolean) {
        viewModelScope.launch { repository.updateShowContinueReading(show) }
    }

    fun setQuranExitReminderDisabled(disabled: Boolean) {
        viewModelScope.launch { repository.updateQuranExitReminderDisabled(disabled) }
    }


    fun setSelectedReciterId(id: String) {
        viewModelScope.launch { repository.updateSelectedReciterId(id) }
    }

    fun setSelectedScript(script: String) {
        viewModelScope.launch { repository.updateSelectedScript(script) }
    }

    fun setSelectedTranslationLang(lang: String) {
        viewModelScope.launch { repository.updateSelectedTranslationLang(lang) }
    }




    fun updateLastReadTopicId(id: String) {
        viewModelScope.launch { repository.updateLastReadTopicId(id) }
    }

    fun clearLastReadTopicId() {
        viewModelScope.launch { repository.updateLastReadTopicId("") }
    }

    private var pendingBookmarkJob: Job? = null

    fun toggleBookmark(topicId: String, ayahIndex: Int = 0) {
        val previousJob = pendingBookmarkJob
        pendingBookmarkJob = viewModelScope.launch {
            previousJob?.join()
            repository.toggleBookmarkedTopicId("$topicId:$ayahIndex")
        }
    }

    fun removeBookmark(topicId: String, ayahIndex: Int = 0) {
        viewModelScope.launch {
            repository.removeBookmarkedTopicId("$topicId:$ayahIndex")
        }
    }

    suspend fun getLatestSettings(): UserSettings {
        pendingBookmarkJob?.join()
        return repository.settingsFlow.first()
    }

    fun toggleMastered(topicId: String) {
        viewModelScope.launch {
            val current = settings.value.masteredTopicIds
            val updated = if (current.contains(topicId)) current - topicId else current + topicId
            repository.updateMasteredTopicIds(updated)
        }
    }

    fun setPrayerCalculationMethod(method: String, switchToLocalIfSupported: Boolean = false) {
        viewModelScope.launch {
            repository.updatePrayerCalculationMethod(method, switchToLocalIfSupported)
        }
    }

    fun setPrayerMadhab(madhab: String) {
        viewModelScope.launch { repository.updatePrayerMadhab(madhab) }
    }

    fun setLocation(lat: Float, lng: Float) {
        viewModelScope.launch {
            repository.updateLocation(lat, lng)
            ensureLocationName()
        }
    }

    fun toggleUseGpsLocation(enabled: Boolean) {
        viewModelScope.launch { repository.updateUseGpsLocation(enabled) }
    }

    fun setPrayerSourceType(source: String) {
        viewModelScope.launch {
            repository.updatePrayerSourceType(source)?.let { _unsupportedLocalSourceEvents.emit(it) }
        }
    }

    fun setPrayerOffset(prayerName: String, minutes: Int) {
        viewModelScope.launch { repository.updatePrayerOffset(prayerName, minutes.coerceIn(-15, 15)) }
    }

    fun setPrayerNotificationEnabled(prayerName: String, enabled: Boolean) {
        viewModelScope.launch { repository.updatePrayerNotificationEnabled(prayerName, enabled) }
    }

    fun setPrePrayerNotificationEnabled(prayerName: String, enabled: Boolean) {
        viewModelScope.launch { repository.updatePrePrayerNotificationEnabled(prayerName, enabled) }
    }

    fun setPrePrayerMinutes(prayerName: String, minutes: Int) {
        viewModelScope.launch { repository.updatePrePrayerMinutes(prayerName, minutes.coerceIn(1, 60)) }
    }

    fun setPrayerNotificationSound(prayerName: String, sound: String) {
        viewModelScope.launch { repository.updatePrayerNotificationSound(prayerName, sound) }
    }

    fun setPrePrayerNotificationSound(sound: String) {
        viewModelScope.launch { repository.updatePrePrayerNotificationSound(sound) }
    }

    fun setPrayerNotificationAlertStyle(style: String) {
        viewModelScope.launch { repository.updatePrayerNotificationAlertStyle(style) }
    }

    fun setPrayerNotificationCustomSoundUri(uri: String) {
        viewModelScope.launch { repository.updatePrayerNotificationCustomSoundUri(uri) }
    }

    fun toggleExtraPrayerTiming(id: String, enabled: Boolean) {
        val updated = settings.value.selectedExtraPrayerTimings.toMutableSet().apply {
            if (enabled) add(id) else remove(id)
        }
        viewModelScope.launch { repository.updateSelectedExtraPrayerTimings(updated) }
    }

    fun toggleExtraPrayerNotification(id: String, enabled: Boolean) {
        val updated = settings.value.extraPrayerNotifications.toMutableSet().apply {
            if (enabled) add(id) else remove(id)
        }
        viewModelScope.launch { repository.updateExtraPrayerNotifications(updated) }
    }

    fun toggleShowExtraPrayerTimingsOnHome(enabled: Boolean) {
        viewModelScope.launch { repository.updateShowExtraPrayerTimingsOnHome(enabled) }
    }


    fun updateWidgetCustomization(
        backgroundColor: String,
        backgroundOpacity: Float,
        panelColor: String,
        panelOpacity: Float,
        fontColor: String? = null
    ) {
        viewModelScope.launch {
            repository.updateWidgetCustomization(
                backgroundColor,
                backgroundOpacity,
                panelColor,
                panelOpacity
            )
            fontColor?.let { repository.updateWidgetFontColor(it) }
            PrayerWidgetProvider.forceWidgetRefresh(appContext)
        }
    }

    fun updateWidgetCornerRadii(background: Float, panel: Float, indicator: Float) {
        viewModelScope.launch {
            repository.updateWidgetCornerRadii(background, panel, indicator)
            PrayerWidgetProvider.forceWidgetRefresh(appContext)
        }
    }

    fun isLocationServicesEnabled(): Boolean {
        val locationManager = appContext.getSystemService(LocationManager::class.java) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }
    }

    fun refreshLocation() {
        refreshLocation(showProviderError = true) { location ->
            location?.let { (lat, lng) -> setLocation(lat, lng) }
        }
    }

    fun refreshLocation(onLocationFound: (Pair<Float, Float>?) -> Unit) {
        refreshLocation(showProviderError = false, onLocationFound = onLocationFound)
    }

    private fun refreshLocation(
        showProviderError: Boolean,
        onLocationFound: (Pair<Float, Float>?) -> Unit
    ) {
        val hasFineLocation =
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation =
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            onLocationFound(null)
            return
        }

        val locationManager = appContext.getSystemService(LocationManager::class.java)
        if (locationManager == null) {
            onLocationFound(null)
            return
        }
        val enabledProviders = buildList {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                add(LocationManager.GPS_PROVIDER)
            }
        }
        if (enabledProviders.isEmpty()) {
            if (showProviderError) {
                Toast.makeText(
                    appContext,
                    LocaleHelper.wrap(appContext).getString(R.string.settings_location_providers_disabled),
                    Toast.LENGTH_SHORT
                ).show()
            }
            onLocationFound(null)
            return
        }

        try {
            val provider = when {
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                else -> enabledProviders.first()
            }

            locationManager.getCurrentLocation(provider, null, appContext.mainExecutor) { currentLocation ->
                val location = currentLocation ?: runCatching {
                    enabledProviders
                        .asSequence()
                        .mapNotNull { p -> locationManager.getLastKnownLocation(p) }
                        .maxByOrNull(Location::getTime)
                }.getOrNull()
                onLocationFound(location?.let { it.latitude.toFloat() to it.longitude.toFloat() })
            }
        } catch (_: SecurityException) {
            onLocationFound(null)
        } catch (_: IllegalArgumentException) {
            onLocationFound(null)
        }
    }

    companion object {
        private val locationNameJobLock = Any()
        private var locationNameJob: Job? = null

        fun factory(repository: SettingsRepository, appContext: Context) =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return SettingsViewModel(repository, appContext) as T
                }
            }
    }
}
