package com.imn.iman.ui.screens.settings

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri

import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import com.imn.iman.ui.theme.PlusJakartaSans
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.imn.iman.MainActivity
import com.imn.iman.R
import com.imn.iman.i18n.LocaleHelper

import com.imn.iman.i18n.madhabLabelRes
import com.imn.iman.i18n.methodLabelRes
import com.imn.iman.i18n.prayerLabel
import com.imn.iman.i18n.soundOptionLabelRes
import com.imn.iman.i18n.sourceTypeLabelRes
import com.imn.iman.data.repository.EXTRA_PRAYER_TIMINGS
import com.imn.iman.data.repository.conventionsForCountry
import com.imn.iman.data.repository.isRecognizedCountryCode
import com.imn.iman.receiver.PrayerAlarmReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun prayerSettingLabel(context: Context, value: String, @StringRes labelRes: Int): String {
    return if (labelRes == 0) value else LocaleHelper.wrap(context).getString(labelRes)
}

private val prayerSoundOptions = listOf("DEFAULT", "ADHAN", "TAKBIR", "SILENT")

@Composable
private fun PrayerSoundSelector(
    prayerLabel: String,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    SettingsDropdown(
        title = stringResource(R.string.settings_prayer_reminder_sound),
        subtitle = stringResource(R.string.settings_prayer_reminder_sound_summary, prayerLabel),
        options = prayerSoundOptions,
        selectedOption = selectedOption,
        optionLabel = { prayerSettingLabel(context, it, soundOptionLabelRes(it)) },
        onOptionSelected = onOptionSelected
    )
}

private data class PrayerNotificationPreference(
    val prayerName: String,
    val prayerEnabled: Boolean,
    val prePrayerEnabled: Boolean,
    val prePrayerMinutes: Int,
    val prayerSound: String,
    val prePrayerSound: String,
)

private data class ExtraTimingPreference(
    val id: String,
    val label: String,
    val selected: Boolean,
    val notificationEnabled: Boolean,
    val sound: String,
)

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

private fun openAppLocationPermissionSettings(context: Context) {
    val permissionIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent("android.intent.action.MANAGE_APP_PERMISSION").apply {
            putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
            putExtra("android.provider.extra.PERMISSION_NAME", Manifest.permission.ACCESS_FINE_LOCATION)
        }.takeIf { it.resolveActivity(context.packageManager) != null }
    } else null
    val appDetailsIntent = Intent(
        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}")
    )
    val activity = context.findActivity()
    fun launch(intent: Intent) {
        if (activity != null) activity.startActivity(intent)
        else context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    try {
        launch(permissionIntent ?: appDetailsIntent)
    } catch (_: Exception) {
        try {
            launch(appDetailsIntent)
        } catch (_: Exception) {
            Toast.makeText(
                context.applicationContext,
                LocaleHelper.wrap(context.applicationContext)
                    .getString(R.string.settings_open_app_settings_for_location),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PrayerSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    focusId: String? = null,
) {
    val settings by viewModel.settings.collectAsState()
    val latestTimesStatus by viewModel.prayerTimeRepository.latestStatus.collectAsState()
    val lastOnlineUpdateMillis by viewModel.prayerTimeRepository.lastOnlineUpdate.collectAsState(initial = null)
    val persistedPrayerError by viewModel.prayerTimeRepository.lastError.collectAsState(initial = null)
    val context = LocalContext.current
    val calculationSourceLabel = stringResource(
        when (latestTimesStatus.source) {
            com.imn.iman.data.repository.TimesSource.ONLINE -> R.string.settings_diagnostic_source_online
            com.imn.iman.data.repository.TimesSource.LOCAL -> R.string.settings_diagnostic_source_local
            com.imn.iman.data.repository.TimesSource.OFFLINE_ESTIMATE -> R.string.settings_diagnostic_source_offline_estimate
        }
    )
    val lastOnlineUpdateLabel = lastOnlineUpdateMillis?.let {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
    } ?: stringResource(R.string.settings_diagnostic_never)
    val lastPrayerErrorLabel = persistedPrayerError?.takeIf { it.isNotBlank() }
        ?: latestTimesStatus.message?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.settings_diagnostic_none)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionRefresh by remember { mutableStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) permissionRefresh++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val notificationsAllowed =
        remember(context, permissionRefresh) { NotificationManagerCompat.from(context).areNotificationsEnabled() }

    val lastRefreshed = if (settings.lastPrayerRefreshEpochMs > 0L) {
        SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(settings.lastPrayerRefreshEpochMs))
    } else {
        stringResource(R.string.settings_not_refreshed_yet)
    }
    var showLocationServicesDialog by remember { mutableStateOf(false) }
    var showUnsupportedSourceDialog by remember { mutableStateOf(false) }
    var unsupportedSourceMethod by remember { mutableStateOf(settings.prayerCalculationMethod) }
    var pendingLocalSwitch by remember { mutableStateOf(false) }
    var openConventionPicker by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        viewModel.unsupportedLocalSourceEvents.collect { method ->
            unsupportedSourceMethod = method
            showUnsupportedSourceDialog = true
        }
    }
    val locationPrefs = remember(context) {
        context.getSharedPreferences(MainActivity.LOCATION_PREFS, Context.MODE_PRIVATE)
    }
    val notificationPrefs = remember(context) {
        context.getSharedPreferences(MainActivity.NOTIFICATION_PREFS, Context.MODE_PRIVATE)
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.toggleUseGpsLocation(true)
            if (viewModel.isLocationServicesEnabled()) viewModel.refreshLocation()
            else showLocationServicesDialog = true
        } else {
            viewModel.toggleUseGpsLocation(false)
        }
    }
    val requestGpsAccess = {
        val hasLocationPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasLocationPermission) {
            viewModel.toggleUseGpsLocation(true)
            if (viewModel.isLocationServicesEnabled()) viewModel.refreshLocation()
            else showLocationServicesDialog = true
        } else {
            val activity = context.findActivity()
            val canShowRationale =
                activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) == true ||
                        activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) == true
            if (locationPrefs.getBoolean(MainActivity.LOCATION_REQUESTED, false) && !canShowRationale) {
                locationPrefs.edit().putBoolean(MainActivity.PENDING_GPS_ENABLE, true).apply()
                openAppLocationPermissionSettings(context)
            } else {
                locationPrefs.edit().putBoolean(MainActivity.LOCATION_REQUESTED, true).apply()
                locationPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                )
            }
        }
    }
    var pendingReminderEnable by remember { mutableStateOf<(() -> Unit)?>(null) }
    val hasExactAlarmAccess = {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || context.getSystemService(AlarmManager::class.java)
            ?.canScheduleExactAlarms() == true
    }
    val completePendingReminder = {
        val action = pendingReminderEnable
        pendingReminderEnable = null
        if (hasExactAlarmAccess()) action?.invoke()
    }
    val exactAlarmSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { permissionRefresh++; completePendingReminder() }
    val continueReminderSetup = {
        permissionRefresh++
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            if (hasExactAlarmAccess()) completePendingReminder()
            else exactAlarmSettingsLauncher.launch(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:${context.packageName}")
                )
            )
        } else pendingReminderEnable = null
    }
    val notificationsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) continueReminderSetup() else {
            permissionRefresh++; pendingReminderEnable = null
        }
    }
    val notificationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { continueReminderSetup() }
    val withReminderPermissions: (Boolean, () -> Unit) -> Unit = { enabling, action ->
        if (!enabling) action() else {
            pendingReminderEnable = action
            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionGranted) {
                val alreadyRequested = notificationPrefs.getBoolean(MainActivity.NOTIFICATION_REQUESTED, false)
                val canShowRationale = context.findActivity()
                    ?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true
                if (alreadyRequested && !canShowRationale) notificationSettingsLauncher.launch(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(
                        Settings.EXTRA_APP_PACKAGE,
                        context.packageName
                    )
                )
                else {
                    notificationPrefs.edit().putBoolean(MainActivity.NOTIFICATION_REQUESTED, true).apply()
                    notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else if (!notificationsAllowed) {
                notificationSettingsLauncher.launch(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(
                        Settings.EXTRA_APP_PACKAGE,
                        context.packageName
                    )
                )
            } else continueReminderSetup()
        }
    }

    val pickerCountryCode = settings.locationCountryCode?.trim()?.uppercase(Locale.ROOT)
    val countryName = pickerCountryCode
        ?.takeIf { isRecognizedCountryCode(it) }
        ?.let { Locale("", it).getDisplayCountry(Locale.getDefault()) }
        ?.takeIf { it.isNotBlank() }
    val conventionPickerHeader = if (countryName == null) null else {
        stringResource(R.string.settings_conventions_for_country, countryName)
    }
    val countryMethods = conventionsForCountry(pickerCountryCode)
    val calculationMethods = if (settings.prayerCalculationMethod in countryMethods) {
        countryMethods
    } else {
        listOf("MUSLIM_WORLD_LEAGUE", settings.prayerCalculationMethod) + countryMethods.drop(1)
    }
    val madhabs = listOf("SHAFI", "HANAFI")
    val sources = listOf("LOCAL", "API")



    if (showLocationServicesDialog) {
        AlertDialog(
            onDismissRequest = { showLocationServicesDialog = false },
            title = { Text(stringResource(R.string.settings_enable_device_location)) },
            text = { Text(stringResource(R.string.settings_enable_device_location_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showLocationServicesDialog = false
                    context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) { Text(stringResource(R.string.settings_open_location_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { showLocationServicesDialog = false }) {
                    Text(stringResource(R.string.settings_not_now))
                }
            }
        )
    }

    if (showUnsupportedSourceDialog) {
        AlertDialog(
            onDismissRequest = { showUnsupportedSourceDialog = false },
            title = { Text(stringResource(R.string.settings_offline_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.settings_offline_convention_message,
                        prayerSettingLabel(context, unsupportedSourceMethod, methodLabelRes(unsupportedSourceMethod))
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showUnsupportedSourceDialog = false
                    pendingLocalSwitch = true
                    openConventionPicker = true
                }) { Text(stringResource(R.string.settings_choose_convention)) }
            },
            dismissButton = {
                TextButton(onClick = { showUnsupportedSourceDialog = false }) {
                    Text(stringResource(R.string.settings_keep_aladhan))
                }
            }
        )
    }

    val prayerNotificationPreferences = listOf(
        PrayerNotificationPreference(
            "Fajr",
            settings.fajrPrayerNotificationEnabled,
            settings.fajrPrePrayerNotificationEnabled,
            settings.fajrPrePrayerMinutes,
            settings.fajrPrayerNotificationSound,
            settings.fajrPrePrayerNotificationSound
        ),
        PrayerNotificationPreference(
            "Dhuhr",
            settings.dhuhrPrayerNotificationEnabled,
            settings.dhuhrPrePrayerNotificationEnabled,
            settings.dhuhrPrePrayerMinutes,
            settings.dhuhrPrayerNotificationSound,
            settings.dhuhrPrePrayerNotificationSound
        ),
        PrayerNotificationPreference(
            "Asr",
            settings.asrPrayerNotificationEnabled,
            settings.asrPrePrayerNotificationEnabled,
            settings.asrPrePrayerMinutes,
            settings.asrPrayerNotificationSound,
            settings.asrPrePrayerNotificationSound
        ),
        PrayerNotificationPreference(
            "Maghrib",
            settings.maghribPrayerNotificationEnabled,
            settings.maghribPrePrayerNotificationEnabled,
            settings.maghribPrePrayerMinutes,
            settings.maghribPrayerNotificationSound,
            settings.maghribPrePrayerNotificationSound
        ),
        PrayerNotificationPreference(
            "Isha",
            settings.ishaPrayerNotificationEnabled,
            settings.ishaPrePrayerNotificationEnabled,
            settings.ishaPrePrayerMinutes,
            settings.ishaPrayerNotificationSound,
            settings.ishaPrePrayerNotificationSound
        )
    )
    val extraTimingPreferences = EXTRA_PRAYER_TIMINGS.map {
        ExtraTimingPreference(
            id = it.id,
            label = prayerLabel(it.id),
            selected = settings.selectedExtraPrayerTimings.contains(it.id),
            notificationEnabled = settings.extraPrayerNotifications.contains(it.id),
            sound = settings.extraPrayerSounds[it.id] ?: "DEFAULT"
        )
    }
    val focusedReminderName = prayerNotificationPreferences.firstOrNull {
        focusId == "reminder_${it.prayerName.lowercase(Locale.ROOT)}"
    }?.prayerName
    val focusedExtraTimingId = extraTimingPreferences.firstOrNull {
        focusId == "extra_${it.id}"
    }?.id
    var focusHandled by rememberSaveable(focusId) { mutableStateOf(false) }
    val focusRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(focusId) {
        if (!focusHandled && (focusedReminderName != null || focusedExtraTimingId != null)) {
            focusRequester.bringIntoView()
            focusHandled = true
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { SettingsTopBarTitle(stringResource(R.string.settings_prayer_times), scrollBehavior) },
                navigationIcon = { SettingsBackButton(onBack) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))

            SettingsGroup(
                title = stringResource(R.string.settings_calculation),
//                description = "How Iman computes the daily prayer times."
            ) {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SettingsDropdown(
                        title = stringResource(R.string.settings_convention),
                        subtitle = stringResource(R.string.settings_convention_summary),
                        options = calculationMethods,
                        selectedOption = settings.prayerCalculationMethod,
                        dropdownHeader = conventionPickerHeader,
                        optionLabel = { prayerSettingLabel(context, it, methodLabelRes(it)) },
                        onOptionSelected = { method ->
                            val switchToLocal = pendingLocalSwitch
                            pendingLocalSwitch = false
                            openConventionPicker = false
                            viewModel.setPrayerCalculationMethod(method, switchToLocalIfSupported = switchToLocal)
                        },
                        openDropdown = openConventionPicker,
                        onDismiss = {
                            pendingLocalSwitch = false
                            openConventionPicker = false
                        }
                    )
                }

                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SettingsDropdown(
                        title = stringResource(R.string.settings_madhab),
                        subtitle = stringResource(R.string.settings_madhab_summary),
                        options = madhabs,
                        selectedOption = settings.prayerMadhab,
                        optionLabel = { prayerSettingLabel(context, it, madhabLabelRes(it)) },
                        onOptionSelected = viewModel::setPrayerMadhab
                    )
                }

                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SettingsDropdown(
                        title = stringResource(R.string.settings_source),
                        subtitle = stringResource(R.string.settings_source_summary),
                        options = sources,
                        selectedOption = settings.prayerSourceType,
                        optionLabel = { prayerSettingLabel(context, it, sourceTypeLabelRes(it)) },
                        onOptionSelected = viewModel::setPrayerSourceType
                    )
                }
            }

            SettingsGroup(
                title = stringResource(R.string.settings_location),
//                description = "Use GPS or your saved coordinates for local prayer times."
            ) {
                SettingsToggleItem(
                    title = stringResource(R.string.settings_use_device_gps),
                    subtitle = stringResource(R.string.settings_gps_summary),
                    checked = settings.useGpsLocation,
                    onCheckedChange = { enabled ->
                        if (enabled) requestGpsAccess() else viewModel.toggleUseGpsLocation(false)
                    }
                )

                if (!settings.useGpsLocation) {
                    FilledTonalButton(
                        onClick = requestGpsAccess,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_gps_access),
                            fontFamily = PlusJakartaSans,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }

                if (settings.useGpsLocation) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = requestGpsAccess,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.settings_gps_access),
                                    fontFamily = PlusJakartaSans,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            OutlinedButton(
                                onClick = requestGpsAccess,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.settings_refresh),
                                    fontFamily = PlusJakartaSans,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(
                                    R.string.settings_current_coordinates,
                                    "%.4f".format(Locale.US, settings.locationLat),
                                    "%.4f".format(Locale.US, settings.locationLng)
                                ),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlusJakartaSans),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.settings_last_refreshed, lastRefreshed),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlusJakartaSans),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    ManualLocationInput(
                        lat = settings.locationLat,
                        lng = settings.locationLng,
                        onLocationSave = { lat, lng ->
                            viewModel.setLocation(lat, lng)
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingsGroup(
                title = stringResource(R.string.settings_reminders)
            ) {


                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    prayerNotificationPreferences.forEach { preference ->
                        val focused = focusedReminderName == preference.prayerName
                        PrayerReminderCard(
                            preference = preference,
                            initiallyExpanded = focused && !focusHandled,
                            modifier = if (focused && !focusHandled) {
                                Modifier.bringIntoViewRequester(focusRequester)
                            } else Modifier,
                            onPrayerToggle = { enabled ->
                                withReminderPermissions(enabled) {
                                    viewModel.setPrayerNotificationEnabled(preference.prayerName, enabled)
                                }
                            },
                            onPrePrayerToggle = { enabled ->
                                withReminderPermissions(enabled) {
                                    viewModel.setPrePrayerNotificationEnabled(preference.prayerName, enabled)
                                }
                            },
                            onPrePrayerMinutesChange = { minutes ->
                                viewModel.setPrePrayerMinutes(preference.prayerName, minutes)
                            },
                            onPrayerSoundChange = { sound ->
                                viewModel.setPrayerNotificationSound(preference.prayerName, sound)
                            },
                            onPrePrayerSoundChange = { sound ->
                                viewModel.setPrePrayerNotificationSound(preference.prayerName, sound)
                            }
                        )
                    }
                }
            }

            SettingsGroup(
                title = stringResource(R.string.settings_extra_timings)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    extraTimingPreferences.forEach { preference ->
                        val focused = focusedExtraTimingId == preference.id
                        ExtraTimingCard(
                            preference = preference,
                            initiallyExpanded = focused && !focusHandled,
                            modifier = if (focused && !focusHandled) {
                                Modifier.bringIntoViewRequester(focusRequester)
                            } else Modifier,
                            onSelectedToggle = { enabled -> viewModel.toggleExtraPrayerTiming(preference.id, enabled) },
                            onNotificationToggle = { enabled ->
                                withReminderPermissions(enabled) {
                                    viewModel.toggleExtraPrayerNotification(preference.id, enabled)
                                }
                            },
                            onSoundChange = { sound ->
                                viewModel.setExtraPrayerNotificationSound(preference.id, sound)
                            }
                        )
                    }
                }


                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Text(
                        text = stringResource(R.string.settings_localization_groundwork),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlusJakartaSans),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SettingsGroup(
                title = stringResource(R.string.settings_advanced),
//                description = "Offsets, diagnostics, and debugging tools."
            ) {
                ExpandableSettingsCard(
                    title = stringResource(R.string.settings_prayer_offsets),
                    summary = stringResource(R.string.settings_prayer_offsets_summary),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrayerOffsetRow("Fajr", settings.fajrOffsetMinutes) { viewModel.setPrayerOffset("Fajr", it) }
                        PrayerOffsetRow("Dhuhr", settings.dhuhrOffsetMinutes) { viewModel.setPrayerOffset("Dhuhr", it) }
                        PrayerOffsetRow("Asr", settings.asrOffsetMinutes) { viewModel.setPrayerOffset("Asr", it) }
                        PrayerOffsetRow("Maghrib", settings.maghribOffsetMinutes) {
                            viewModel.setPrayerOffset(
                                "Maghrib",
                                it
                            )
                        }
                        PrayerOffsetRow("Isha", settings.ishaOffsetMinutes) { viewModel.setPrayerOffset("Isha", it) }
                    }
                }

                ExpandableSettingsCard(
                    title = stringResource(R.string.settings_diagnostics),
                    summary = stringResource(R.string.settings_diagnostics_summary),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    PrayerDiagnosticsCard(
                        source = prayerSettingLabel(
                            context,
                            settings.prayerSourceType,
                            sourceTypeLabelRes(settings.prayerSourceType)
                        ),
                        calculationSource = calculationSourceLabel,
                        lastOnlineUpdate = lastOnlineUpdateLabel,
                        lastError = lastPrayerErrorLabel,
                        method = prayerSettingLabel(
                            context,
                            settings.prayerCalculationMethod,
                            methodLabelRes(settings.prayerCalculationMethod)
                        ),
                        madhab = prayerSettingLabel(
                            context,
                            settings.prayerMadhab,
                            madhabLabelRes(settings.prayerMadhab)
                        ),
                        latitude = settings.locationLat,
                        longitude = settings.locationLng,
                        lastRefreshed = lastRefreshed,
                        gpsEnabled = settings.useGpsLocation,
                        notificationsAllowed = notificationsAllowed
                    )
                }

                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.settings_debug_controls),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            onClick = {
                                Log.d("ImanNotif", "Notify debug button pressed")
                                val appContext = context.applicationContext
                                appContext.sendBroadcast(
                                    Intent(appContext, PrayerAlarmReceiver::class.java).apply {
                                        action = PrayerAlarmReceiver.ACTION_FIRE_PRAYER_NOTIFICATION
                                        putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, "Dhuhr")
                                        putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_TYPE, "PRAYER")
                                        putExtra(PrayerAlarmReceiver.EXTRA_PRE_PRAYER_MINUTES, 0)
                                        putExtra(
                                            PrayerAlarmReceiver.EXTRA_TRIGGER_AT_MILLIS,
                                            System.currentTimeMillis()
                                        )
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.settings_notify),
                                fontFamily = PlusJakartaSans,
                                fontSize = 14.sp,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                Log.d("ImanNotif", "Pre-notify debug button pressed")
                                val appContext = context.applicationContext
                                appContext.sendBroadcast(
                                    Intent(appContext, PrayerAlarmReceiver::class.java).apply {
                                        action = PrayerAlarmReceiver.ACTION_FIRE_PRAYER_NOTIFICATION
                                        putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, "Dhuhr")
                                        putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_TYPE, "PRE_PRAYER")
                                        putExtra(PrayerAlarmReceiver.EXTRA_PRE_PRAYER_MINUTES, 10)
                                        putExtra(
                                            PrayerAlarmReceiver.EXTRA_TRIGGER_AT_MILLIS,
                                            System.currentTimeMillis()
                                        )
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.settings_pre_notify),
                                fontFamily = PlusJakartaSans,
                                fontSize = 14.sp,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PrayerOffsetRow(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = prayerLabel(title),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = stringResource(R.string.settings_shift_by_minutes),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlusJakartaSans),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = { onValueChange(value - 1) }) {
                Text("-", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = if (value > 0) {
                    stringResource(R.string.settings_offset_positive, value)
                } else {
                    stringResource(R.string.settings_minutes_short, value)
                },
                modifier = Modifier.width(40.dp),
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlusJakartaSans),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(onClick = { onValueChange(value + 1) }) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun ExtraTimingCard(
    preference: ExtraTimingPreference,
    onSelectedToggle: (Boolean) -> Unit,
    onNotificationToggle: (Boolean) -> Unit,
    onSoundChange: (String) -> Unit,
    initiallyExpanded: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val visibilityLabel = stringResource(if (preference.selected) R.string.settings_shown else R.string.settings_hidden)
    val alertLabel =
        stringResource(if (preference.notificationEnabled) R.string.settings_alert_on else R.string.settings_alert_off)
    val summary = stringResource(R.string.settings_extra_timing_summary, visibilityLabel, alertLabel)
    ExpandableSettingsCard(
        title = preference.label,
        summary = summary,
        modifier = modifier,
        initiallyExpanded = initiallyExpanded,
        shape = RoundedCornerShape(20.dp),
        actions = {
            SettingsInlineAction(
                selected = preference.selected,
                icon = if (preference.selected) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = stringResource(R.string.settings_show_timing, preference.label),
                onClick = { onSelectedToggle(!preference.selected) }
            )
            SettingsInlineAction(
                selected = preference.notificationEnabled,
                icon = Icons.Default.Notifications,
                contentDescription = stringResource(R.string.settings_notify_for_timing, preference.label),
                onClick = { onNotificationToggle(!preference.notificationEnabled) }
            )
        }
    ) {
        SettingsToggleItem(
            title = stringResource(R.string.settings_show_on_surfaces),
            subtitle = stringResource(R.string.settings_keep_timing_visible, preference.label),
            checked = preference.selected,
            onCheckedChange = onSelectedToggle
        )
        SettingsToggleItem(
            title = stringResource(R.string.settings_notification_alert),
            subtitle = stringResource(R.string.settings_send_timing_reminder, preference.label),
            checked = preference.notificationEnabled,
            onCheckedChange = onNotificationToggle,
            showDivider = !preference.notificationEnabled
        )
        if (preference.notificationEnabled) {
            PrayerSoundSelector(
                prayerLabel = preference.label,
                selectedOption = preference.sound,
                onOptionSelected = onSoundChange
            )
        }
    }
}

@Composable
private fun PrayerReminderCard(
    preference: PrayerNotificationPreference,
    onPrayerToggle: (Boolean) -> Unit,
    onPrePrayerToggle: (Boolean) -> Unit,
    onPrePrayerMinutesChange: (Int) -> Unit,
    onPrayerSoundChange: (String) -> Unit,
    onPrePrayerSoundChange: (String) -> Unit,
    initiallyExpanded: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val prayerName = prayerLabel(preference.prayerName)
    val prayerStatus =
        stringResource(if (preference.prayerEnabled) R.string.settings_prayer_on else R.string.settings_prayer_off)
    val prePrayerStatus = if (preference.prePrayerEnabled) {
        stringResource(R.string.settings_pre_prayer_summary, preference.prePrayerMinutes)
    } else {
        stringResource(R.string.settings_pre_off)
    }
    val summary = stringResource(R.string.settings_extra_timing_summary, prayerStatus, prePrayerStatus)
    ExpandableSettingsCard(
        title = prayerName,
        summary = summary,
        modifier = modifier,
        initiallyExpanded = initiallyExpanded,
        shape = RoundedCornerShape(20.dp),
        actions = {
            SettingsInlineAction(
                selected = preference.prayerEnabled,
                icon = Icons.Default.Notifications,
                contentDescription = stringResource(R.string.settings_prayer_reminder_action_description, prayerName),
                onClick = { onPrayerToggle(!preference.prayerEnabled) }
            )
            SettingsInlineAction(
                selected = preference.prePrayerEnabled,
                icon = Icons.Default.Schedule,
                contentDescription = stringResource(
                    R.string.settings_pre_prayer_reminder_action_description,
                    prayerName
                ),
                onClick = { onPrePrayerToggle(!preference.prePrayerEnabled) }
            )
        }
    ) {
        SettingsToggleItem(
            title = stringResource(R.string.settings_prayer_notification),
            subtitle = stringResource(R.string.settings_prayer_notification_summary, prayerName),
            checked = preference.prayerEnabled,
            onCheckedChange = onPrayerToggle
        )
        PrayerSoundSelector(
            prayerLabel = prayerName,
            selectedOption = preference.prayerSound,
            onOptionSelected = onPrayerSoundChange
        )
        SettingsToggleItem(
            title = stringResource(R.string.settings_pre_prayer_notification),
            subtitle = stringResource(R.string.settings_remind_before_prayer, prayerName),
            checked = preference.prePrayerEnabled,
            onCheckedChange = onPrePrayerToggle,
            showDivider = preference.prePrayerEnabled
        )
        if (preference.prePrayerEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_lead_time),
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlusJakartaSans)
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.settings_minutes_before,
                            preference.prePrayerMinutes,
                            preference.prePrayerMinutes
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlusJakartaSans),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { onPrePrayerMinutesChange(preference.prePrayerMinutes - 1) }) {
                        Text("-", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        text = stringResource(R.string.settings_minutes_short, preference.prePrayerMinutes),
                        modifier = Modifier.width(40.dp),
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlusJakartaSans),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    IconButton(onClick = { onPrePrayerMinutesChange(preference.prePrayerMinutes + 1) }) {
                        Text("+", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            PrayerSoundSelector(
                prayerLabel = prayerName,
                selectedOption = preference.prePrayerSound,
                onOptionSelected = onPrePrayerSoundChange
            )
        }
    }
}

@Composable
private fun PrayerDiagnosticsCard(
    source: String,
    calculationSource: String,
    lastOnlineUpdate: String,
    lastError: String,
    method: String,
    madhab: String,
    latitude: Float,
    longitude: Float,
    lastRefreshed: String,
    gpsEnabled: Boolean,
    notificationsAllowed: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.settings_diagnostic_setup),
            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary)
        )
        DiagnosticRow(stringResource(R.string.settings_diagnostic_selected_source), source)
        DiagnosticRow(stringResource(R.string.settings_diagnostic_source), calculationSource)
        DiagnosticRow(stringResource(R.string.settings_diagnostic_last_online_update), lastOnlineUpdate)
        DiagnosticRow(stringResource(R.string.settings_diagnostic_last_error), lastError)
        DiagnosticRow(stringResource(R.string.settings_diagnostic_method), method)
        DiagnosticRow(stringResource(R.string.settings_madhab), madhab)

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Text(
            stringResource(R.string.settings_location),
            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary)
        )
        DiagnosticRow(
            stringResource(R.string.settings_diagnostic_coordinates),
            stringResource(
                R.string.settings_diagnostic_coordinates_value,
                "%.4f".format(Locale.US, latitude),
                "%.4f".format(Locale.US, longitude)
            )
        )
        DiagnosticRow(
            stringResource(R.string.settings_diagnostic_gps_status),
            stringResource(if (gpsEnabled) R.string.settings_status_enabled else R.string.settings_status_disabled)
        )
        DiagnosticRow(stringResource(R.string.settings_diagnostic_last_refresh), lastRefreshed)

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Text(
            stringResource(R.string.settings_diagnostic_status),
            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary)
        )
        DiagnosticRow(
            stringResource(R.string.settings_diagnostic_notifications),
            stringResource(if (notificationsAllowed) R.string.settings_status_allowed else R.string.settings_status_not_allowed)
        )
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PlusJakartaSans),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun ManualLocationInput(
    lat: Float,
    lng: Float,
    onLocationSave: (Float, Float) -> Unit
) {
    var latText by remember(lat) { mutableStateOf(lat.toString()) }
    var lngText by remember(lng) { mutableStateOf(lng.toString()) }

    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = latText,
                onValueChange = { latText = it },
                label = { Text(stringResource(R.string.settings_latitude), fontFamily = PlusJakartaSans) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlusJakartaSans)
            )
            OutlinedTextField(
                value = lngText,
                onValueChange = { lngText = it },
                label = { Text(stringResource(R.string.settings_longitude), fontFamily = PlusJakartaSans) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlusJakartaSans)
            )
        }
        FilledTonalButton(
            onClick = {
                val parsedLat = latText.toFloatOrNull()
                val parsedLng = lngText.toFloatOrNull()
                if (parsedLat != null && parsedLng != null) {
                    onLocationSave(parsedLat, parsedLng)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings_update_coordinates), fontFamily = PlusJakartaSans)
        }
    }
}
