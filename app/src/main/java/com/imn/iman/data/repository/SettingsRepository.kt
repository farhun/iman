package com.imn.iman.data.repository

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.imn.iman.R
import com.imn.iman.i18n.LocaleHelper
import com.imn.iman.i18n.SUPPORTED_LANGUAGES

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

import java.io.IOException
import java.util.Locale

object WidgetVisibilityItems {
    const val GREGORIAN_DATE = "gregorian_date"
    const val HIJRI_DATE = "hijri_date"
    const val LOCATION = "location"
    const val FAJR = "fajr"
    const val DHUHR = "dhuhr"
    const val ASR = "asr"
    const val MAGHRIB = "maghrib"
    const val ISHA = "isha"
    const val COUNTDOWN = "countdown"
    const val NEXT_PRAYER = "next_prayer"
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

const val DEFAULT_WIDGET_BACKGROUND_COLOR = "#293041"
const val DEFAULT_WIDGET_BACKGROUND_OPACITY = 0.8f
const val DEFAULT_WIDGET_PANEL_COLOR = "#000000"
const val DEFAULT_WIDGET_PANEL_OPACITY = 0.3f
const val DEFAULT_WIDGET_FONT_COLOR = "#FFFFFF"
const val DEFAULT_WIDGET_INDICATOR_COLOR = "#B0C6FE"

const val DEFAULT_TASBIH_BACKGROUND_COLOR = "#000000"
const val DEFAULT_TASBIH_BEAD_COLOR = "#3192F5"
const val DEFAULT_TASBIH_TEXT_COLOR = "#FFFFFF"
const val DEFAULT_TASBIH_BEAD_MODE = "slide"
private val PRAYER_SOUND_OPTIONS = setOf("DEFAULT", "ADHAN", "TAKBIR", "SILENT")

internal fun supportsLocalCalculationMethod(methodStr: String): Boolean = when (methodStr) {
    "MUSLIM_WORLD_LEAGUE",
    "EGYPTIAN",
    "KARACHI",
    "UMM_AL_QURA",
    "DUBAI",
    "MOON_SIGHTING_COMMITTEE",
    "NORTH_AMERICA",
    "KUWAIT",
    "QATAR",
    "SINGAPORE",
    "ALGERIA",
    "TUNISIA",
    "FRANCE_UOIF",
    "FRANCE_15",
    "FRANCE_18" -> true

    else -> false
}

class SettingsRepository(private val context: Context) {
    internal val applicationContext: Context
        get() = context.applicationContext

    private fun legacyTranslationChoice(legacy: String?): String? {
        if (legacy == null) return ""
        if (legacy.isBlank()) return null
        val normalized = legacy.lowercase(Locale.ROOT)
        val parts = normalized.split('_', limit = 2)
        val isOldDefault = normalized == "20" || normalized == "54" ||
                (parts.size == 2 && ((parts[0] == "en" && parts[1] == "20") ||
                        (parts[0] == "ur" && parts[1] == "54")))
        return if (isOldDefault) "" else legacy
    }

    private fun defaultTranslationForAppLanguage(): String {
        val savedTag = LocaleHelper.savedTag(context)
        val language = (if (savedTag == "system") {
            LocaleHelper.wrap(context).resources.configuration.locales[0].language
        } else {
            savedTag.substringBefore('-')
        }).lowercase(Locale.ROOT)
        return when (language) {
            "en" -> TranslationRepository.QURANENC_ENGLISH_ID
            "in", "id" -> TranslationRepository.QURANENC_INDONESIAN_ID
            else -> ""
        }
    }

    private object PreferencesKeys {
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val PURE_BLACK = booleanPreferencesKey("pure_black")
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")

        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHOW_STEP_TIMER = booleanPreferencesKey("show_step_timer")
        val FLUID_TRANSITIONS = booleanPreferencesKey("fluid_transitions")
        val VIBRATION_ON_COUNT = booleanPreferencesKey("vibration_on_count")
        val SHOW_LAP_COUNTER = booleanPreferencesKey("show_lap_counter")

        val COLOR_SEED = stringPreferencesKey("color_seed")
        val CUSTOM_ACCENT_COLOR = intPreferencesKey("custom_accent_color")
        val TASBEEH_LIST_MODE = booleanPreferencesKey("tasbeeh_list_mode")
        val STARTUP_TAB = stringPreferencesKey("startup_tab")
        val TASBIH_BACKGROUND_COLOR = stringPreferencesKey("tasbih_background_color")
        val TASBIH_BEAD_COLOR = stringPreferencesKey("tasbih_bead_color")
        val TASBIH_TEXT_COLOR = stringPreferencesKey("tasbih_text_color")
        val TASBIH_BEAD_MODE = stringPreferencesKey("tasbih_bead_mode")

        val READING_THEME = stringPreferencesKey("reading_theme")
        val ARABIC_SIZE_SP = floatPreferencesKey("arabic_size_sp")
        val TRANSLATION_SIZE_SP = floatPreferencesKey("translation_size_sp")
        val SHOW_TRANSLATION = booleanPreferencesKey("show_translation")
        val READING_KEEP_SCREEN_ON = booleanPreferencesKey("reading_keep_screen_on")
        val LAST_READ_TOPIC_ID = stringPreferencesKey("last_read_topic_id")
        val BOOKMARKED_TOPIC_IDS = stringSetPreferencesKey("bookmarked_topic_ids")
        val BOOKMARK_SAVED_AT = stringSetPreferencesKey("bookmark_saved_at")
        val MASTERED_TOPIC_IDS = stringSetPreferencesKey("mastered_topic_ids")

        val QURAN_EXIT_REMINDER_DISABLED = booleanPreferencesKey("quran_exit_reminder_disabled")

        val SELECTED_TRANSLATION_LANG = stringPreferencesKey("selected_translation_lang")
        val TRANSLATION_CHOICE = stringPreferencesKey("translation_choice")
        val TRANSLATION_DEFAULT_MIGRATED = booleanPreferencesKey("translation_default_migrated")

        val SELECTED_RECITER_ID = stringPreferencesKey("selected_reciter_id")
        val SELECTED_SCRIPT = stringPreferencesKey("selected_script")


        // Tasbeeh Interaction

        val TASBEEH_VOLUME_ENABLED = booleanPreferencesKey("tasbeeh_volume_enabled")
        val TASBEEH_VOLUME_ANIMATION = booleanPreferencesKey("tasbeeh_volume_animation")
        val TASBIH_SOUND_ENABLED = booleanPreferencesKey("tasbih_sound_enabled")
        val TASBIH_SOUND_ID = stringPreferencesKey("tasbih_sound_id")

        val DEVELOPER_WELCOME_DISMISSED = booleanPreferencesKey("developer_welcome_dismissed")
        val STUDY_NOTE_DISMISSED = booleanPreferencesKey("study_note_dismissed")
        val LAST_SEEN_APP_VERSION_CODE = intPreferencesKey("last_seen_app_version_code")

        // Prayer Times & Location
        val PRAYER_CALCULATION_METHOD = stringPreferencesKey("prayer_calculation_method")
        val PRAYER_MADHAB = stringPreferencesKey("prayer_madhab")
        val LOCATION_LAT = floatPreferencesKey("location_lat")
        val LOCATION_LNG = floatPreferencesKey("location_lng")
        val USE_GPS_LOCATION = booleanPreferencesKey("use_gps_location")
        val PRAYER_SOURCE_TYPE = stringPreferencesKey("prayer_source_type")
        val SOURCE_AUTO_SWITCHED = booleanPreferencesKey("source_auto_switched")
        val PRAYER_OFFSET_FAJR = intPreferencesKey("prayer_offset_fajr")
        val PRAYER_OFFSET_DHUHR = intPreferencesKey("prayer_offset_dhuhr")
        val PRAYER_OFFSET_ASR = intPreferencesKey("prayer_offset_asr")
        val PRAYER_OFFSET_MAGHRIB = intPreferencesKey("prayer_offset_maghrib")
        val PRAYER_OFFSET_ISHA = intPreferencesKey("prayer_offset_isha")
        val LAST_PRAYER_REFRESH_EPOCH_MS = longPreferencesKey("last_prayer_refresh_epoch_ms")
        val PRAYER_NOTIFICATION_FAJR = booleanPreferencesKey("prayer_notification_fajr")
        val PRAYER_NOTIFICATION_DHUHR = booleanPreferencesKey("prayer_notification_dhuhr")
        val PRAYER_NOTIFICATION_ASR = booleanPreferencesKey("prayer_notification_asr")
        val PRAYER_NOTIFICATION_MAGHRIB = booleanPreferencesKey("prayer_notification_maghrib")
        val PRAYER_NOTIFICATION_ISHA = booleanPreferencesKey("prayer_notification_isha")
        val PRE_PRAYER_NOTIFICATION_FAJR = booleanPreferencesKey("pre_prayer_notification_fajr")
        val PRE_PRAYER_NOTIFICATION_DHUHR = booleanPreferencesKey("pre_prayer_notification_dhuhr")
        val PRE_PRAYER_NOTIFICATION_ASR = booleanPreferencesKey("pre_prayer_notification_asr")
        val PRE_PRAYER_NOTIFICATION_MAGHRIB = booleanPreferencesKey("pre_prayer_notification_maghrib")
        val PRE_PRAYER_NOTIFICATION_ISHA = booleanPreferencesKey("pre_prayer_notification_isha")
        val PRE_PRAYER_MINUTES_FAJR = intPreferencesKey("pre_prayer_minutes_fajr")
        val PRE_PRAYER_MINUTES_DHUHR = intPreferencesKey("pre_prayer_minutes_dhuhr")
        val PRE_PRAYER_MINUTES_ASR = intPreferencesKey("pre_prayer_minutes_asr")
        val PRE_PRAYER_MINUTES_MAGHRIB = intPreferencesKey("pre_prayer_minutes_maghrib")
        val PRE_PRAYER_MINUTES_ISHA = intPreferencesKey("pre_prayer_minutes_isha")

        val PRAYER_SOUND_FAJR = stringPreferencesKey("prayer_sound_fajr")
        val PRAYER_SOUND_DHUHR = stringPreferencesKey("prayer_sound_dhuhr")
        val PRAYER_SOUND_ASR = stringPreferencesKey("prayer_sound_asr")
        val PRAYER_SOUND_MAGHRIB = stringPreferencesKey("prayer_sound_maghrib")
        val PRAYER_SOUND_ISHA = stringPreferencesKey("prayer_sound_isha")
        val PRE_PRAYER_SOUND_FAJR = stringPreferencesKey("pre_prayer_sound_fajr")
        val PRE_PRAYER_SOUND_DHUHR = stringPreferencesKey("pre_prayer_sound_dhuhr")
        val PRE_PRAYER_SOUND_ASR = stringPreferencesKey("pre_prayer_sound_asr")
        val PRE_PRAYER_SOUND_MAGHRIB = stringPreferencesKey("pre_prayer_sound_maghrib")
        val PRE_PRAYER_SOUND_ISHA = stringPreferencesKey("pre_prayer_sound_isha")

        val LAST_DELIVERED_PRAYER_NOTIFICATION_EVENT_ID =
            stringPreferencesKey("last_delivered_prayer_notification_event_id")
        val SELECTED_EXTRA_PRAYER_TIMINGS = stringSetPreferencesKey("selected_extra_prayer_timings")
        val EXTRA_PRAYER_NOTIFICATIONS = stringSetPreferencesKey("extra_prayer_notifications")
        val REMINDER_SETTINGS_V2_MIGRATED = booleanPreferencesKey("reminder_settings_v2_migrated")
        fun extraPrayerSound(id: String) = stringPreferencesKey("extra_sound_$id")


        val LOCATION_LABEL = stringPreferencesKey("location_label")
        val LOCATION_COUNTRY_CODE = stringPreferencesKey("location_country_code")
        val LOCATION_LABEL_LAT = floatPreferencesKey("location_label_lat")
        val LOCATION_LABEL_LNG = floatPreferencesKey("location_label_lng")
        val LOCATION_LABEL_LANG = stringPreferencesKey("location_label_lang")
        val WIDGET_BACKGROUND_COLOR = stringPreferencesKey("widget_background_color")
        val WIDGET_BACKGROUND_OPACITY = floatPreferencesKey("widget_background_opacity")
        val WIDGET_PANEL_COLOR = stringPreferencesKey("widget_panel_color")
        val WIDGET_PANEL_OPACITY = floatPreferencesKey("widget_panel_opacity")
        val WIDGET_FONT_COLOR = stringPreferencesKey("widget_font_color")
        val WIDGET_HIDDEN_ITEMS = stringSetPreferencesKey("widget_hidden_items")
        val WIDGET_INDICATOR_COLOR = stringPreferencesKey("widget_indicator_color")
        val WIDGET_BACKGROUND_CORNER_RADIUS = floatPreferencesKey("widget_background_corner_radius")
        val WIDGET_PANEL_CORNER_RADIUS = floatPreferencesKey("widget_panel_corner_radius")
        val WIDGET_INDICATOR_CORNER_RADIUS = floatPreferencesKey("widget_indicator_corner_radius")
    }

    private fun normalizeCountryCode(code: String?): String? = code
        ?.trim()
        ?.uppercase(Locale.ROOT)
        ?.takeIf { normalized ->
            normalized.length == 2 && Locale.getISOCountries().any { country -> country == normalized }
        }

    private fun locationCountryCode(preferences: Preferences): String? {
        normalizeCountryCode(preferences[PreferencesKeys.LOCATION_COUNTRY_CODE])?.let { return it }
        val latitude = preferences[PreferencesKeys.LOCATION_LAT] ?: 21.4225f
        val longitude = preferences[PreferencesKeys.LOCATION_LNG] ?: 39.8262f
        return if (kotlin.math.abs(latitude - 21.4225f) < 0.00001f &&
            kotlin.math.abs(longitude - 39.8262f) < 0.00001f
        ) "SA" else null
    }

    suspend fun getLocationCountryCode(): String? =
        locationCountryCode(context.dataStore.data.first())

    private fun defaultLocationLabel(): String =
        LocaleHelper.wrap(context.applicationContext).getString(R.string.quran_revelation_makkah)

    private fun fallbackLocationLabels(): Set<String> {
        val appContext = context.applicationContext
        return buildSet {
            add(LocaleHelper.wrap(appContext).getString(R.string.home_your_area))
            SUPPORTED_LANGUAGES.forEach { language ->
                val configuration = Configuration(appContext.resources.configuration).apply {
                    setLocale(Locale.forLanguageTag(language.tag))
                }
                add(appContext.createConfigurationContext(configuration).getString(R.string.home_your_area))
            }
        }
    }

    suspend fun cleanupLegacyLocationLabel() {
        val fallbackLabels = fallbackLocationLabels()
        val defaultLabel = defaultLocationLabel()
        context.dataStore.edit { preferences ->
            val storedLabel = preferences[PreferencesKeys.LOCATION_LABEL]
            if (storedLabel != null && storedLabel in fallbackLabels && storedLabel != defaultLabel) {
                preferences.remove(PreferencesKeys.LOCATION_LABEL)
                preferences.remove(PreferencesKeys.LOCATION_LABEL_LAT)
                preferences.remove(PreferencesKeys.LOCATION_LABEL_LNG)
                preferences.remove(PreferencesKeys.LOCATION_LABEL_LANG)
            }
        }
        Log.d("ImanLocation", "One-time cleanup done")
    }

    suspend fun migrateReminderSettingsV2(): Boolean {
        val legacyPrePrayerSoundKey = stringPreferencesKey("pre_prayer_sound")
        val legacyStyleKey = stringPreferencesKey("prayer_notification_alert_style")
        val legacyPickerUriKey = stringPreferencesKey("prayer_notification_custom_sound_uri")
        var migrated = false
        context.dataStore.edit { preferences ->
            if (preferences[PreferencesKeys.REMINDER_SETTINGS_V2_MIGRATED] == true) return@edit

            val legacyPrePrayerSound = (preferences[legacyPrePrayerSoundKey] ?: "DEFAULT")
                .takeIf { it in PRAYER_SOUND_OPTIONS } ?: "DEFAULT"
            listOf(
                PreferencesKeys.PRE_PRAYER_SOUND_FAJR,
                PreferencesKeys.PRE_PRAYER_SOUND_DHUHR,
                PreferencesKeys.PRE_PRAYER_SOUND_ASR,
                PreferencesKeys.PRE_PRAYER_SOUND_MAGHRIB,
                PreferencesKeys.PRE_PRAYER_SOUND_ISHA
            ).forEach { key ->
                if (preferences[key] == null) preferences[key] = legacyPrePrayerSound
            }

            val legacyStyle = preferences[legacyStyleKey]
            val migratedExtraSound = if (legacyStyle == "SILENT" || legacyStyle == "VIBRATION") {
                "SILENT"
            } else {
                "DEFAULT"
            }
            EXTRA_PRAYER_TIMINGS.forEach { timing ->
                val key = PreferencesKeys.extraPrayerSound(timing.id)
                if (preferences[key] == null) preferences[key] = migratedExtraSound
            }

            preferences.remove(legacyPrePrayerSoundKey)
            preferences.remove(legacyStyleKey)
            preferences.remove(legacyPickerUriKey)
            preferences[PreferencesKeys.REMINDER_SETTINGS_V2_MIGRATED] = true
            migrated = true
        }
        if (migrated) Log.d("ImanNotif", "Migrated reminder settings to v2")
        return migrated
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val locationLat = preferences[PreferencesKeys.LOCATION_LAT] ?: 21.4225f
            val locationLng = preferences[PreferencesKeys.LOCATION_LNG] ?: 39.8262f
            val locationLabel = preferences[PreferencesKeys.LOCATION_LABEL]
                ?: if (isDefaultLocation(locationLat.toDouble(), locationLng.toDouble())) defaultLocationLabel() else ""
            val translationChoice = when {
                preferences.contains(PreferencesKeys.TRANSLATION_CHOICE) ->
                    preferences[PreferencesKeys.TRANSLATION_CHOICE]

                preferences[PreferencesKeys.TRANSLATION_DEFAULT_MIGRATED] == true -> null
                else -> legacyTranslationChoice(preferences[PreferencesKeys.SELECTED_TRANSLATION_LANG])
            }
            val selectedTranslationLang = when (translationChoice) {
                null -> ""
                "" -> defaultTranslationForAppLanguage()
                else -> translationChoice
            }

            UserSettings(
                hapticsEnabled = preferences[PreferencesKeys.HAPTICS_ENABLED] ?: true,
                dynamicColor = preferences[PreferencesKeys.DYNAMIC_COLOR] ?: false,
                pureBlack = preferences[PreferencesKeys.PURE_BLACK] ?: false,
                keepScreenAwake = preferences[PreferencesKeys.KEEP_SCREEN_AWAKE] ?: true,

                themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "System",
                showStepTimer = preferences[PreferencesKeys.SHOW_STEP_TIMER] ?: true,
                fluidTransitions = preferences[PreferencesKeys.FLUID_TRANSITIONS] ?: true,
                vibrationOnCount = preferences[PreferencesKeys.VIBRATION_ON_COUNT] ?: true,
                showLapCounter = preferences[PreferencesKeys.SHOW_LAP_COUNTER] ?: true,

                colorSeed = preferences[PreferencesKeys.COLOR_SEED] ?: "default",
                customAccentColor = preferences[PreferencesKeys.CUSTOM_ACCENT_COLOR],
                tasbeehListMode = preferences[PreferencesKeys.TASBEEH_LIST_MODE] ?: true,
                startupTab = preferences[PreferencesKeys.STARTUP_TAB] ?: "home",
                tasbihBackgroundColor = preferences[PreferencesKeys.TASBIH_BACKGROUND_COLOR]
                    ?: DEFAULT_TASBIH_BACKGROUND_COLOR,
                tasbihBeadColor = preferences[PreferencesKeys.TASBIH_BEAD_COLOR]
                    ?: DEFAULT_TASBIH_BEAD_COLOR,
                tasbihTextColor = preferences[PreferencesKeys.TASBIH_TEXT_COLOR]
                    ?: DEFAULT_TASBIH_TEXT_COLOR,
                tasbihBeadMode = preferences[PreferencesKeys.TASBIH_BEAD_MODE]
                    ?: DEFAULT_TASBIH_BEAD_MODE,

                readingTheme = preferences[PreferencesKeys.READING_THEME] ?: "AUTO",
                arabicSizeSp = preferences[PreferencesKeys.ARABIC_SIZE_SP] ?: 32f,
                translationSizeSp = preferences[PreferencesKeys.TRANSLATION_SIZE_SP] ?: 16f,
                showTranslation = preferences[PreferencesKeys.SHOW_TRANSLATION] ?: true,
                readingKeepScreenOn = preferences[PreferencesKeys.READING_KEEP_SCREEN_ON] ?: true,
                lastReadTopicId = preferences[PreferencesKeys.LAST_READ_TOPIC_ID],
                bookmarkedAyahs = preferences[PreferencesKeys.BOOKMARKED_TOPIC_IDS] ?: emptySet(),
                bookmarkedAyahSavedAt = preferences[PreferencesKeys.BOOKMARK_SAVED_AT].orEmpty()
                    .mapNotNull { entry ->
                        val separator = entry.lastIndexOf('|')
                        if (separator <= 0) return@mapNotNull null
                        val savedAt = entry.substring(separator + 1).toLongOrNull() ?: return@mapNotNull null
                        entry.substring(0, separator) to savedAt
                    }.toMap(),
                masteredTopicIds = preferences[PreferencesKeys.MASTERED_TOPIC_IDS] ?: emptySet(),

                quranExitReminderDisabled = preferences[PreferencesKeys.QURAN_EXIT_REMINDER_DISABLED] ?: false,

                translationChoice = translationChoice,
                selectedTranslationLang = selectedTranslationLang,
                selectedReciterId = preferences[PreferencesKeys.SELECTED_RECITER_ID] ?: "everyayah_mishari",
                selectedScript = (preferences[PreferencesKeys.SELECTED_SCRIPT] ?: "indopak")
                    .let {
                        when (it) {
                            "uthmanic_hafs" -> "uthmani"
                            "uthmani_simple", "imlaei" -> "indopak"
                            else -> it
                        }
                    },


                tasbeehVolumeEnabled = preferences[PreferencesKeys.TASBEEH_VOLUME_ENABLED] ?: true,
                tasbeehVolumeAnimation = preferences[PreferencesKeys.TASBEEH_VOLUME_ANIMATION] ?: true,
                tasbihSoundEnabled = preferences[PreferencesKeys.TASBIH_SOUND_ENABLED] ?: true,
                tasbihSoundId = preferences[PreferencesKeys.TASBIH_SOUND_ID] ?: "1",

                studyNoteDismissed = preferences[PreferencesKeys.STUDY_NOTE_DISMISSED] ?: false,
                lastSeenAppVersionCode = preferences[PreferencesKeys.LAST_SEEN_APP_VERSION_CODE] ?: 0,
                prayerCalculationMethod = preferences[PreferencesKeys.PRAYER_CALCULATION_METHOD]
                    ?: "MUSLIM_WORLD_LEAGUE",
                prayerMadhab = preferences[PreferencesKeys.PRAYER_MADHAB] ?: "SHAFI",
                locationLat = locationLat,
                locationLng = locationLng,
                locationCountryCode = locationCountryCode(preferences),
                useGpsLocation = preferences[PreferencesKeys.USE_GPS_LOCATION] ?: false,
                prayerSourceType = preferences[PreferencesKeys.PRAYER_SOURCE_TYPE] ?: "LOCAL",
                fajrOffsetMinutes = preferences[PreferencesKeys.PRAYER_OFFSET_FAJR] ?: 0,
                dhuhrOffsetMinutes = preferences[PreferencesKeys.PRAYER_OFFSET_DHUHR] ?: 0,
                asrOffsetMinutes = preferences[PreferencesKeys.PRAYER_OFFSET_ASR] ?: 0,
                maghribOffsetMinutes = preferences[PreferencesKeys.PRAYER_OFFSET_MAGHRIB] ?: 0,
                ishaOffsetMinutes = preferences[PreferencesKeys.PRAYER_OFFSET_ISHA] ?: 0,
                lastPrayerRefreshEpochMs = preferences[PreferencesKeys.LAST_PRAYER_REFRESH_EPOCH_MS] ?: 0L,
                fajrPrayerNotificationEnabled = preferences[PreferencesKeys.PRAYER_NOTIFICATION_FAJR] ?: false,
                dhuhrPrayerNotificationEnabled = preferences[PreferencesKeys.PRAYER_NOTIFICATION_DHUHR] ?: false,
                asrPrayerNotificationEnabled = preferences[PreferencesKeys.PRAYER_NOTIFICATION_ASR] ?: false,
                maghribPrayerNotificationEnabled = preferences[PreferencesKeys.PRAYER_NOTIFICATION_MAGHRIB] ?: false,
                ishaPrayerNotificationEnabled = preferences[PreferencesKeys.PRAYER_NOTIFICATION_ISHA] ?: false,
                fajrPrePrayerNotificationEnabled = preferences[PreferencesKeys.PRE_PRAYER_NOTIFICATION_FAJR] ?: false,
                dhuhrPrePrayerNotificationEnabled = preferences[PreferencesKeys.PRE_PRAYER_NOTIFICATION_DHUHR] ?: false,
                asrPrePrayerNotificationEnabled = preferences[PreferencesKeys.PRE_PRAYER_NOTIFICATION_ASR] ?: false,
                maghribPrePrayerNotificationEnabled = preferences[PreferencesKeys.PRE_PRAYER_NOTIFICATION_MAGHRIB]
                    ?: false,
                ishaPrePrayerNotificationEnabled = preferences[PreferencesKeys.PRE_PRAYER_NOTIFICATION_ISHA] ?: false,
                fajrPrePrayerMinutes = preferences[PreferencesKeys.PRE_PRAYER_MINUTES_FAJR] ?: 10,
                dhuhrPrePrayerMinutes = preferences[PreferencesKeys.PRE_PRAYER_MINUTES_DHUHR] ?: 10,
                asrPrePrayerMinutes = preferences[PreferencesKeys.PRE_PRAYER_MINUTES_ASR] ?: 10,
                maghribPrePrayerMinutes = preferences[PreferencesKeys.PRE_PRAYER_MINUTES_MAGHRIB] ?: 10,
                ishaPrePrayerMinutes = preferences[PreferencesKeys.PRE_PRAYER_MINUTES_ISHA] ?: 10,

                fajrPrayerNotificationSound = preferences[PreferencesKeys.PRAYER_SOUND_FAJR] ?: "DEFAULT",
                dhuhrPrayerNotificationSound = preferences[PreferencesKeys.PRAYER_SOUND_DHUHR] ?: "DEFAULT",
                asrPrayerNotificationSound = preferences[PreferencesKeys.PRAYER_SOUND_ASR] ?: "DEFAULT",
                maghribPrayerNotificationSound = preferences[PreferencesKeys.PRAYER_SOUND_MAGHRIB] ?: "DEFAULT",
                ishaPrayerNotificationSound = preferences[PreferencesKeys.PRAYER_SOUND_ISHA] ?: "DEFAULT",
                fajrPrePrayerNotificationSound = preferences[PreferencesKeys.PRE_PRAYER_SOUND_FAJR] ?: "DEFAULT",
                dhuhrPrePrayerNotificationSound = preferences[PreferencesKeys.PRE_PRAYER_SOUND_DHUHR] ?: "DEFAULT",
                asrPrePrayerNotificationSound = preferences[PreferencesKeys.PRE_PRAYER_SOUND_ASR] ?: "DEFAULT",
                maghribPrePrayerNotificationSound = preferences[PreferencesKeys.PRE_PRAYER_SOUND_MAGHRIB] ?: "DEFAULT",
                ishaPrePrayerNotificationSound = preferences[PreferencesKeys.PRE_PRAYER_SOUND_ISHA] ?: "DEFAULT",

                lastDeliveredPrayerNotificationEventId = preferences[PreferencesKeys.LAST_DELIVERED_PRAYER_NOTIFICATION_EVENT_ID].orEmpty(),
                selectedExtraPrayerTimings = preferences[PreferencesKeys.SELECTED_EXTRA_PRAYER_TIMINGS] ?: emptySet(),
                extraPrayerNotifications = preferences[PreferencesKeys.EXTRA_PRAYER_NOTIFICATIONS] ?: emptySet(),
                extraPrayerSounds = EXTRA_PRAYER_TIMINGS.associate { timing ->
                    timing.id to (preferences[PreferencesKeys.extraPrayerSound(timing.id)] ?: "DEFAULT")
                },


                locationLabel = locationLabel,
                locationLabelLat = preferences[PreferencesKeys.LOCATION_LABEL_LAT],
                locationLabelLng = preferences[PreferencesKeys.LOCATION_LABEL_LNG],
                locationLabelLang = preferences[PreferencesKeys.LOCATION_LABEL_LANG],
                widgetBackgroundColor = preferences[PreferencesKeys.WIDGET_BACKGROUND_COLOR]
                    ?: DEFAULT_WIDGET_BACKGROUND_COLOR,
                widgetBackgroundOpacity = preferences[PreferencesKeys.WIDGET_BACKGROUND_OPACITY]
                    ?: DEFAULT_WIDGET_BACKGROUND_OPACITY,
                widgetPanelColor = preferences[PreferencesKeys.WIDGET_PANEL_COLOR] ?: DEFAULT_WIDGET_PANEL_COLOR,
                widgetPanelOpacity = preferences[PreferencesKeys.WIDGET_PANEL_OPACITY] ?: DEFAULT_WIDGET_PANEL_OPACITY,
                widgetFontColor = preferences[PreferencesKeys.WIDGET_FONT_COLOR] ?: DEFAULT_WIDGET_FONT_COLOR,
                hiddenWidgetItems = preferences[PreferencesKeys.WIDGET_HIDDEN_ITEMS] ?: emptySet(),
                widgetIndicatorColor = preferences[PreferencesKeys.WIDGET_INDICATOR_COLOR]
                    ?: DEFAULT_WIDGET_INDICATOR_COLOR,
                widgetBackgroundCornerRadius = preferences[PreferencesKeys.WIDGET_BACKGROUND_CORNER_RADIUS] ?: 24f,
                widgetPanelCornerRadius = preferences[PreferencesKeys.WIDGET_PANEL_CORNER_RADIUS] ?: 24f,
                widgetIndicatorCornerRadius = preferences[PreferencesKeys.WIDGET_INDICATOR_CORNER_RADIUS] ?: 6f
            )
        }

    suspend fun updateWidgetFontColor(color: String) {
        context.dataStore.edit { it[PreferencesKeys.WIDGET_FONT_COLOR] = color }
    }

    suspend fun updateWidgetItemVisibility(item: String, visible: Boolean) {
        context.dataStore.edit { preferences ->
            val hiddenItems = preferences[PreferencesKeys.WIDGET_HIDDEN_ITEMS].orEmpty().toMutableSet()
            if (visible) hiddenItems.remove(item) else hiddenItems.add(item)
            preferences[PreferencesKeys.WIDGET_HIDDEN_ITEMS] = hiddenItems
        }
    }

    suspend fun updateWidgetIndicatorColor(color: String) {
        context.dataStore.edit { it[PreferencesKeys.WIDGET_INDICATOR_COLOR] = color }
    }

    suspend fun updateWidgetCustomization(
        backgroundColor: String,
        backgroundOpacity: Float,
        panelColor: String,
        panelOpacity: Float
    ) {
        context.dataStore.edit {
            it[PreferencesKeys.WIDGET_BACKGROUND_COLOR] = backgroundColor
            it[PreferencesKeys.WIDGET_BACKGROUND_OPACITY] = backgroundOpacity
            it[PreferencesKeys.WIDGET_PANEL_COLOR] = panelColor
            it[PreferencesKeys.WIDGET_PANEL_OPACITY] = panelOpacity
        }
    }

    suspend fun updateWidgetCornerRadii(background: Float, panel: Float, indicator: Float) {
        context.dataStore.edit {
            it[PreferencesKeys.WIDGET_BACKGROUND_CORNER_RADIUS] = background
            it[PreferencesKeys.WIDGET_PANEL_CORNER_RADIUS] = panel
            it[PreferencesKeys.WIDGET_INDICATOR_CORNER_RADIUS] = indicator
        }
    }


    suspend fun updateResolvedLocation(
        latitude: Float,
        longitude: Float,
        place: ResolvedPlace,
        languageTag: String
    ) {
        if (place.name.isBlank() || place.name in fallbackLocationLabels() || languageTag.isBlank()) return
        val normalizedCountryCode = normalizeCountryCode(place.countryCode) ?: return
        context.dataStore.edit { preferences ->
            val savedLatitude = preferences[PreferencesKeys.LOCATION_LAT] ?: 21.4225f
            val savedLongitude = preferences[PreferencesKeys.LOCATION_LNG] ?: 39.8262f
            if (kotlin.math.abs(savedLatitude - latitude) <= 0.0001f &&
                kotlin.math.abs(savedLongitude - longitude) <= 0.0001f
            ) {
                preferences[PreferencesKeys.LOCATION_LABEL] = place.name
                preferences[PreferencesKeys.LOCATION_COUNTRY_CODE] = normalizedCountryCode
                preferences[PreferencesKeys.LOCATION_LABEL_LAT] = latitude
                preferences[PreferencesKeys.LOCATION_LABEL_LNG] = longitude
                preferences[PreferencesKeys.LOCATION_LABEL_LANG] = languageTag
            }
        }
    }


    suspend fun updatePrayerCalculationMethod(
        method: String,
        switchToLocalIfSupported: Boolean = false
    ) {
        context.dataStore.edit { preferences ->
            val source = preferences[PreferencesKeys.PRAYER_SOURCE_TYPE] ?: "LOCAL"
            val autoSwitched = preferences[PreferencesKeys.SOURCE_AUTO_SWITCHED] ?: false
            val supportedLocally = supportsLocalCalculationMethod(method)
            preferences[PreferencesKeys.PRAYER_CALCULATION_METHOD] = method

            when {
                !supportedLocally && source == "LOCAL" -> {
                    preferences[PreferencesKeys.PRAYER_SOURCE_TYPE] = "API"
                    preferences[PreferencesKeys.SOURCE_AUTO_SWITCHED] = true
                }

                supportedLocally && source == "API" && (autoSwitched || switchToLocalIfSupported) -> {
                    preferences[PreferencesKeys.PRAYER_SOURCE_TYPE] = "LOCAL"
                    preferences[PreferencesKeys.SOURCE_AUTO_SWITCHED] = false
                }
            }
        }
    }

    suspend fun updatePrayerMadhab(madhab: String) {
        context.dataStore.edit { it[PreferencesKeys.PRAYER_MADHAB] = madhab }
    }

    suspend fun updateLocation(lat: Float, lng: Float) {
        context.dataStore.edit {
            it[PreferencesKeys.LOCATION_LAT] = lat
            it[PreferencesKeys.LOCATION_LNG] = lng
        }
    }

    suspend fun updateUseGpsLocation(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.USE_GPS_LOCATION] = enabled }
    }

    suspend fun updatePrayerSourceType(source: String): String? {
        var unsupportedMethod: String? = null
        context.dataStore.edit { preferences ->
            if (source == "LOCAL") {
                val method = preferences[PreferencesKeys.PRAYER_CALCULATION_METHOD] ?: "MUSLIM_WORLD_LEAGUE"
                if (!supportsLocalCalculationMethod(method)) {
                    unsupportedMethod = method
                    return@edit
                }
            }
            preferences[PreferencesKeys.PRAYER_SOURCE_TYPE] = source
            preferences[PreferencesKeys.SOURCE_AUTO_SWITCHED] = false
        }
        return unsupportedMethod
    }

    suspend fun repairUnsupportedLocalPrayerSource() {
        context.dataStore.edit { preferences ->
            val source = preferences[PreferencesKeys.PRAYER_SOURCE_TYPE] ?: "LOCAL"
            val method = preferences[PreferencesKeys.PRAYER_CALCULATION_METHOD] ?: "MUSLIM_WORLD_LEAGUE"
            if (source == "LOCAL" && !supportsLocalCalculationMethod(method)) {
                preferences[PreferencesKeys.PRAYER_SOURCE_TYPE] = "API"
                preferences[PreferencesKeys.SOURCE_AUTO_SWITCHED] = true
            }
        }
    }

    suspend fun updatePrayerOffset(prayerKey: String, minutes: Int) {
        context.dataStore.edit {
            when (prayerKey) {
                "Fajr" -> it[PreferencesKeys.PRAYER_OFFSET_FAJR] = minutes
                "Dhuhr" -> it[PreferencesKeys.PRAYER_OFFSET_DHUHR] = minutes
                "Asr" -> it[PreferencesKeys.PRAYER_OFFSET_ASR] = minutes
                "Maghrib" -> it[PreferencesKeys.PRAYER_OFFSET_MAGHRIB] = minutes
                "Isha" -> it[PreferencesKeys.PRAYER_OFFSET_ISHA] = minutes
            }
        }
    }

    suspend fun updateLocationAndPrayerRefresh(
        epochMs: Long,
        location: Pair<Float, Float>? = null
    ) {
        context.dataStore.edit { preferences ->
            location?.let { (lat, lng) ->
                if (lat.isFinite() && lng.isFinite() && lat in -90f..90f && lng in -180f..180f) {
                    preferences[PreferencesKeys.LOCATION_LAT] = lat
                    preferences[PreferencesKeys.LOCATION_LNG] = lng
                }
            }
            preferences[PreferencesKeys.LAST_PRAYER_REFRESH_EPOCH_MS] = epochMs
        }
    }

    suspend fun updateLastPrayerRefresh(epochMs: Long) {
        context.dataStore.edit { it[PreferencesKeys.LAST_PRAYER_REFRESH_EPOCH_MS] = epochMs }
    }

    suspend fun updatePrayerNotificationEnabled(prayerKey: String, enabled: Boolean) {
        context.dataStore.edit {
            when (prayerKey) {
                "Fajr" -> it[PreferencesKeys.PRAYER_NOTIFICATION_FAJR] = enabled
                "Dhuhr" -> it[PreferencesKeys.PRAYER_NOTIFICATION_DHUHR] = enabled
                "Asr" -> it[PreferencesKeys.PRAYER_NOTIFICATION_ASR] = enabled
                "Maghrib" -> it[PreferencesKeys.PRAYER_NOTIFICATION_MAGHRIB] = enabled
                "Isha" -> it[PreferencesKeys.PRAYER_NOTIFICATION_ISHA] = enabled
            }
        }
    }

    suspend fun updatePrePrayerNotificationEnabled(prayerKey: String, enabled: Boolean) {
        context.dataStore.edit {
            when (prayerKey) {
                "Fajr" -> it[PreferencesKeys.PRE_PRAYER_NOTIFICATION_FAJR] = enabled
                "Dhuhr" -> it[PreferencesKeys.PRE_PRAYER_NOTIFICATION_DHUHR] = enabled
                "Asr" -> it[PreferencesKeys.PRE_PRAYER_NOTIFICATION_ASR] = enabled
                "Maghrib" -> it[PreferencesKeys.PRE_PRAYER_NOTIFICATION_MAGHRIB] = enabled
                "Isha" -> it[PreferencesKeys.PRE_PRAYER_NOTIFICATION_ISHA] = enabled
            }
        }
    }

    suspend fun updatePrePrayerMinutes(prayerKey: String, minutes: Int) {
        context.dataStore.edit {
            when (prayerKey) {
                "Fajr" -> it[PreferencesKeys.PRE_PRAYER_MINUTES_FAJR] = minutes
                "Dhuhr" -> it[PreferencesKeys.PRE_PRAYER_MINUTES_DHUHR] = minutes
                "Asr" -> it[PreferencesKeys.PRE_PRAYER_MINUTES_ASR] = minutes
                "Maghrib" -> it[PreferencesKeys.PRE_PRAYER_MINUTES_MAGHRIB] = minutes
                "Isha" -> it[PreferencesKeys.PRE_PRAYER_MINUTES_ISHA] = minutes
            }
        }
    }


    suspend fun updatePrayerNotificationSound(prayerName: String, sound: String) {
        val key = when (prayerName) {
            "Fajr" -> PreferencesKeys.PRAYER_SOUND_FAJR
            "Dhuhr" -> PreferencesKeys.PRAYER_SOUND_DHUHR
            "Asr" -> PreferencesKeys.PRAYER_SOUND_ASR
            "Maghrib" -> PreferencesKeys.PRAYER_SOUND_MAGHRIB
            "Isha" -> PreferencesKeys.PRAYER_SOUND_ISHA
            else -> return
        }
        val selectedSound = sound.takeIf { it in PRAYER_SOUND_OPTIONS } ?: "DEFAULT"
        context.dataStore.edit { it[key] = selectedSound }
    }

    suspend fun updatePrePrayerNotificationSound(prayerName: String, sound: String) {
        val key = when (prayerName) {
            "Fajr" -> PreferencesKeys.PRE_PRAYER_SOUND_FAJR
            "Dhuhr" -> PreferencesKeys.PRE_PRAYER_SOUND_DHUHR
            "Asr" -> PreferencesKeys.PRE_PRAYER_SOUND_ASR
            "Maghrib" -> PreferencesKeys.PRE_PRAYER_SOUND_MAGHRIB
            "Isha" -> PreferencesKeys.PRE_PRAYER_SOUND_ISHA
            else -> return
        }
        val selectedSound = sound.takeIf { it in PRAYER_SOUND_OPTIONS } ?: "DEFAULT"
        context.dataStore.edit { it[key] = selectedSound }
    }

    suspend fun updateLastDeliveredPrayerNotificationEventId(eventId: String) {
        context.dataStore.edit { it[PreferencesKeys.LAST_DELIVERED_PRAYER_NOTIFICATION_EVENT_ID] = eventId }
    }

    suspend fun updateSelectedExtraPrayerTimings(ids: Set<String>) {
        context.dataStore.edit { it[PreferencesKeys.SELECTED_EXTRA_PRAYER_TIMINGS] = ids }
    }

    suspend fun updateExtraPrayerNotifications(ids: Set<String>) {
        context.dataStore.edit { it[PreferencesKeys.EXTRA_PRAYER_NOTIFICATIONS] = ids }
    }

    suspend fun updateExtraPrayerNotificationSound(id: String, sound: String) {
        if (EXTRA_PRAYER_TIMINGS.none { it.id == id }) return
        val selectedSound = sound.takeIf { it in PRAYER_SOUND_OPTIONS } ?: "DEFAULT"
        context.dataStore.edit { it[PreferencesKeys.extraPrayerSound(id)] = selectedSound }
    }

    suspend fun disableAllReminderNotifications() {
        context.dataStore.edit {
            it[PreferencesKeys.PRAYER_NOTIFICATION_FAJR] = false
            it[PreferencesKeys.PRAYER_NOTIFICATION_DHUHR] = false
            it[PreferencesKeys.PRAYER_NOTIFICATION_ASR] = false
            it[PreferencesKeys.PRAYER_NOTIFICATION_MAGHRIB] = false
            it[PreferencesKeys.PRAYER_NOTIFICATION_ISHA] = false
            it[PreferencesKeys.PRE_PRAYER_NOTIFICATION_FAJR] = false
            it[PreferencesKeys.PRE_PRAYER_NOTIFICATION_DHUHR] = false
            it[PreferencesKeys.PRE_PRAYER_NOTIFICATION_ASR] = false
            it[PreferencesKeys.PRE_PRAYER_NOTIFICATION_MAGHRIB] = false
            it[PreferencesKeys.PRE_PRAYER_NOTIFICATION_ISHA] = false
            it[PreferencesKeys.EXTRA_PRAYER_NOTIFICATIONS] = emptySet()
        }
    }


    suspend fun setTasbeehVolumeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.TASBEEH_VOLUME_ENABLED] = enabled }
    }

    suspend fun setTasbeehVolumeAnimation(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.TASBEEH_VOLUME_ANIMATION] = enabled }
    }

    suspend fun setTasbihSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.TASBIH_SOUND_ENABLED] = enabled }
    }

    suspend fun setTasbihSoundId(id: String) {
        context.dataStore.edit { it[PreferencesKeys.TASBIH_SOUND_ID] = id }
    }


    suspend fun setStudyNoteDismissed(dismissed: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.STUDY_NOTE_DISMISSED] = dismissed }
    }

    suspend fun updateLastSeenAppVersionCode(versionCode: Int) {
        context.dataStore.edit { it[PreferencesKeys.LAST_SEEN_APP_VERSION_CODE] = versionCode }
    }


    suspend fun updateTasbihBackgroundColor(color: String) {
        context.dataStore.edit { it[PreferencesKeys.TASBIH_BACKGROUND_COLOR] = color }
    }

    suspend fun updateTasbihBeadColor(color: String) {
        context.dataStore.edit { it[PreferencesKeys.TASBIH_BEAD_COLOR] = color }
    }

    suspend fun updateTasbihTextColor(color: String) {
        context.dataStore.edit { it[PreferencesKeys.TASBIH_TEXT_COLOR] = color }
    }

    suspend fun updateTasbihBeadMode(mode: String) {
        context.dataStore.edit { it[PreferencesKeys.TASBIH_BEAD_MODE] = mode }
    }


    suspend fun updateHaptics(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun updateDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun updatePureBlack(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.PURE_BLACK] = enabled }
    }

    suspend fun updateKeepScreenAwake(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.KEEP_SCREEN_AWAKE] = enabled }
    }


    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode }
    }

    suspend fun updateShowStepTimer(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_STEP_TIMER] = show }
    }

    suspend fun updateFluidTransitions(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.FLUID_TRANSITIONS] = enabled }
    }

    suspend fun updateVibrationOnCount(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.VIBRATION_ON_COUNT] = enabled }
    }

    suspend fun updateShowLapCounter(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_LAP_COUNTER] = show }
    }


    suspend fun updateColorSeed(seed: String) {
        context.dataStore.edit {
            it[PreferencesKeys.COLOR_SEED] = seed
            it.remove(PreferencesKeys.CUSTOM_ACCENT_COLOR)
        }
    }

    suspend fun updateCustomAccentColor(color: Int) {
        context.dataStore.edit {
            it[PreferencesKeys.CUSTOM_ACCENT_COLOR] = color
            it[PreferencesKeys.DYNAMIC_COLOR] = false
        }
    }

    suspend fun updateTasbeehListMode(isList: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.TASBEEH_LIST_MODE] = isList }
    }

    suspend fun updateStartupTab(route: String) {
        context.dataStore.edit { it[PreferencesKeys.STARTUP_TAB] = route }
    }


    suspend fun updateReadingTheme(theme: String) {
        context.dataStore.edit { it[PreferencesKeys.READING_THEME] = theme }
    }

    suspend fun updateArabicSizeSp(size: Float) {
        context.dataStore.edit { it[PreferencesKeys.ARABIC_SIZE_SP] = size }
    }

    suspend fun updateTranslationSizeSp(size: Float) {
        context.dataStore.edit { it[PreferencesKeys.TRANSLATION_SIZE_SP] = size }
    }

    suspend fun updateShowTranslation(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_TRANSLATION] = show }
    }


    suspend fun updateReadingKeepScreenOn(keep: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.READING_KEEP_SCREEN_ON] = keep }
    }

    suspend fun updateLastReadTopicId(id: String) {
        context.dataStore.edit { it[PreferencesKeys.LAST_READ_TOPIC_ID] = id }
    }

    suspend fun toggleBookmarkedTopicId(key: String, savedAt: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            val bookmarks = preferences[PreferencesKeys.BOOKMARKED_TOPIC_IDS] ?: emptySet()
            val savedTimes = preferences[PreferencesKeys.BOOKMARK_SAVED_AT] ?: emptySet()
            if (key in bookmarks) {
                preferences[PreferencesKeys.BOOKMARKED_TOPIC_IDS] = bookmarks - key
                preferences[PreferencesKeys.BOOKMARK_SAVED_AT] = savedTimes.filterNot {
                    it.substringBeforeLast('|') == key
                }.toSet()
            } else {
                preferences[PreferencesKeys.BOOKMARKED_TOPIC_IDS] = bookmarks + key
                preferences[PreferencesKeys.BOOKMARK_SAVED_AT] = savedTimes + "$key|$savedAt"
            }
        }
    }

    suspend fun removeBookmarkedTopicId(key: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BOOKMARKED_TOPIC_IDS] =
                (preferences[PreferencesKeys.BOOKMARKED_TOPIC_IDS] ?: emptySet()) - key
            preferences[PreferencesKeys.BOOKMARK_SAVED_AT] =
                (preferences[PreferencesKeys.BOOKMARK_SAVED_AT] ?: emptySet()).filterNot {
                    it.substringBeforeLast('|') == key
                }.toSet()
        }
    }

    suspend fun updateBookmarkedTopicIds(ids: Set<String>) {
        context.dataStore.edit { it[PreferencesKeys.BOOKMARKED_TOPIC_IDS] = ids }
    }

    suspend fun updateMasteredTopicIds(ids: Set<String>) {
        context.dataStore.edit { it[PreferencesKeys.MASTERED_TOPIC_IDS] = ids }
    }


    suspend fun updateQuranExitReminderDisabled(disabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.QURAN_EXIT_REMINDER_DISABLED] = disabled }
    }


    suspend fun migrateTranslationDefault() {
        context.dataStore.edit { preferences ->
            if (preferences[PreferencesKeys.TRANSLATION_DEFAULT_MIGRATED] == true) return@edit
            if (!preferences.contains(PreferencesKeys.TRANSLATION_CHOICE)) {
                legacyTranslationChoice(preferences[PreferencesKeys.SELECTED_TRANSLATION_LANG])?.let {
                    preferences[PreferencesKeys.TRANSLATION_CHOICE] = it
                }
            }
            preferences[PreferencesKeys.TRANSLATION_DEFAULT_MIGRATED] = true
        }
    }

    suspend fun updateTranslationChoice(choice: String?) {
        context.dataStore.edit { preferences ->
            if (choice == null) preferences.remove(PreferencesKeys.TRANSLATION_CHOICE)
            else preferences[PreferencesKeys.TRANSLATION_CHOICE] = choice
        }
    }


    suspend fun updateSelectedReciterId(id: String) {
        context.dataStore.edit { it[PreferencesKeys.SELECTED_RECITER_ID] = id }
    }

    suspend fun updateSelectedScript(script: String) {
        context.dataStore.edit { it[PreferencesKeys.SELECTED_SCRIPT] = script }
    }
}

data class UserSettings(
    val hapticsEnabled: Boolean,
    val dynamicColor: Boolean,
    val pureBlack: Boolean,
    val keepScreenAwake: Boolean,

    val themeMode: String,
    val showStepTimer: Boolean,
    val fluidTransitions: Boolean,
    val vibrationOnCount: Boolean,
    val showLapCounter: Boolean,

    val colorSeed: String,
    val tasbeehListMode: Boolean,
    val startupTab: String,
    val tasbihBackgroundColor: String = DEFAULT_TASBIH_BACKGROUND_COLOR,
    val tasbihBeadColor: String = DEFAULT_TASBIH_BEAD_COLOR,
    val tasbihTextColor: String = DEFAULT_TASBIH_TEXT_COLOR,
    val tasbihBeadMode: String = DEFAULT_TASBIH_BEAD_MODE,

    val readingTheme: String = "DARK",
    val arabicSizeSp: Float = 32f,
    val translationSizeSp: Float = 16f,
    val showTranslation: Boolean = true,
    val readingKeepScreenOn: Boolean = true,
    val lastReadTopicId: String? = null,
    val bookmarkedAyahs: Set<String> = emptySet(),
    val bookmarkedAyahSavedAt: Map<String, Long> = emptyMap(),
    val masteredTopicIds: Set<String> = emptySet(),

    val quranExitReminderDisabled: Boolean = false,

    val translationChoice: String? = "",
    val selectedTranslationLang: String = "",
    val selectedReciterId: String = "everyayah_mishari",
    val selectedScript: String = "indopak",


    val tasbeehVolumeEnabled: Boolean = true,
    val tasbeehVolumeAnimation: Boolean = true,
    val tasbihSoundEnabled: Boolean = true,
    val tasbihSoundId: String = "1",

    val studyNoteDismissed: Boolean = false,
    val lastSeenAppVersionCode: Int = 0,
    val prayerCalculationMethod: String = "MUSLIM_WORLD_LEAGUE",
    val prayerMadhab: String = "SHAFI",
    val locationLat: Float = 21.4225f,
    val locationLng: Float = 39.8262f,
    val locationCountryCode: String? = null,
    val useGpsLocation: Boolean = false,
    val prayerSourceType: String = "LOCAL",
    val fajrOffsetMinutes: Int = 0,
    val dhuhrOffsetMinutes: Int = 0,
    val asrOffsetMinutes: Int = 0,
    val maghribOffsetMinutes: Int = 0,
    val ishaOffsetMinutes: Int = 0,
    val lastPrayerRefreshEpochMs: Long = 0L,
    val fajrPrayerNotificationEnabled: Boolean = false,
    val dhuhrPrayerNotificationEnabled: Boolean = false,
    val asrPrayerNotificationEnabled: Boolean = false,
    val maghribPrayerNotificationEnabled: Boolean = false,
    val ishaPrayerNotificationEnabled: Boolean = false,
    val fajrPrePrayerNotificationEnabled: Boolean = false,
    val dhuhrPrePrayerNotificationEnabled: Boolean = false,
    val asrPrePrayerNotificationEnabled: Boolean = false,
    val maghribPrePrayerNotificationEnabled: Boolean = false,
    val ishaPrePrayerNotificationEnabled: Boolean = false,
    val fajrPrePrayerMinutes: Int = 10,
    val dhuhrPrePrayerMinutes: Int = 10,
    val asrPrePrayerMinutes: Int = 10,
    val maghribPrePrayerMinutes: Int = 10,
    val ishaPrePrayerMinutes: Int = 10,

    val fajrPrayerNotificationSound: String = "DEFAULT",
    val dhuhrPrayerNotificationSound: String = "DEFAULT",
    val asrPrayerNotificationSound: String = "DEFAULT",
    val maghribPrayerNotificationSound: String = "DEFAULT",
    val ishaPrayerNotificationSound: String = "DEFAULT",
    val fajrPrePrayerNotificationSound: String = "DEFAULT",
    val dhuhrPrePrayerNotificationSound: String = "DEFAULT",
    val asrPrePrayerNotificationSound: String = "DEFAULT",
    val maghribPrePrayerNotificationSound: String = "DEFAULT",
    val ishaPrePrayerNotificationSound: String = "DEFAULT",

    val lastDeliveredPrayerNotificationEventId: String = "",
    val selectedExtraPrayerTimings: Set<String> = emptySet(),
    val extraPrayerNotifications: Set<String> = emptySet(),
    val extraPrayerSounds: Map<String, String> = emptyMap(),


    val locationLabel: String = "",
    val locationLabelLat: Float? = null,
    val locationLabelLng: Float? = null,
    val locationLabelLang: String? = null,
    val widgetBackgroundColor: String = DEFAULT_WIDGET_BACKGROUND_COLOR,
    val widgetBackgroundOpacity: Float = DEFAULT_WIDGET_BACKGROUND_OPACITY,
    val widgetPanelColor: String = DEFAULT_WIDGET_PANEL_COLOR,
    val widgetPanelOpacity: Float = DEFAULT_WIDGET_PANEL_OPACITY,
    val widgetFontColor: String = DEFAULT_WIDGET_FONT_COLOR,
    val hiddenWidgetItems: Set<String> = emptySet(),
    val widgetIndicatorColor: String = DEFAULT_WIDGET_INDICATOR_COLOR,
    val widgetBackgroundCornerRadius: Float = 24f,
    val widgetPanelCornerRadius: Float = 24f,
    val widgetIndicatorCornerRadius: Float = 6f,
    val customAccentColor: Int? = null
)
