package com.imn.iman

import android.app.Activity
import android.app.AlarmManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.core.app.NotificationManagerCompat
import com.imn.iman.service.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument


import com.imn.iman.data.repository.ReleaseNotesRepository
import com.imn.iman.data.repository.SettingsRepository
import com.imn.iman.data.model.TasbeehCollection
import com.imn.iman.data.model.DhikrItem
import com.imn.iman.data.local.TasbeehDatabase
import com.imn.iman.i18n.LocaleHelper
import com.imn.iman.notifications.PrayerNotificationScheduler
import com.imn.iman.notifications.toPrayerNotificationScheduleConfig
import com.imn.iman.widget.WidgetRefresher
import com.imn.iman.worker.PrayerRefreshWork
import com.imn.iman.ui.components.PillNavBar
import com.imn.iman.ui.components.ReleaseNotesDialog
import com.imn.iman.ui.navigation.*

import com.imn.iman.ui.screens.home.HomeScreen
import com.imn.iman.ui.screens.home.HomeViewModel
import com.imn.iman.data.repository.PrayerTimeRepository

import com.imn.iman.ui.screens.settings.*
import com.imn.iman.ui.screens.tasbeeh.*
import com.imn.iman.ui.theme.ImanTheme
import com.imn.iman.ui.theme.ThemeTransitionProvider
import com.imn.iman.ui.util.add
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {

        const val LOCATION_PREFS = "location_permission_flow"
        const val LOCATION_REQUESTED = "location_requested"
        const val NOTIFICATION_PREFS = "notification_permission_flow"
        const val NOTIFICATION_REQUESTED = "notification_requested"
        const val FIRST_LOCATION_PROMPT_SHOWN = "first_location_prompt_shown_v2"
        const val PENDING_GPS_ENABLE = "pending_gps_enable"
    }

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var tasbeehViewModel: TasbeehViewModel


    private lateinit var quranViewModel: com.imn.iman.ui.screens.quran.QuranViewModel

    private lateinit var prayerTimeRepository: PrayerTimeRepository
    private lateinit var homeViewModel: com.imn.iman.ui.screens.home.HomeViewModel

    // Tracks whether the tasbeeh immersive screen is currently active so that
    // volume key interception only fires there and not app-wide.
    var isOnTasbeehImmersive: Boolean = false

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var hasPausedSinceResume = false
    private var notificationPermissionRequestInFlight = false
    private val media3Controller: MediaController?
        get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        notificationPermissionRequestInFlight = false
        if (::settingsRepository.isInitialized) disableReminderNotificationsIfPermissionsMissing()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
    }

    override fun onPause() {
        hasPausedSinceResume = true
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (!::settingsRepository.isInitialized) return
        if (!notificationPermissionRequestInFlight) disableReminderNotificationsIfPermissionsMissing()
        if (!hasPausedSinceResume || !::settingsViewModel.isInitialized) return
        hasPausedSinceResume = false
        val prefs = getSharedPreferences(LOCATION_PREFS, MODE_PRIVATE)
        val hasLocationPermission = hasLocationPermission()
        if (prefs.getBoolean(PENDING_GPS_ENABLE, false)) {
            prefs.edit().remove(PENDING_GPS_ENABLE).apply()
            if (hasLocationPermission) {
                enableGpsAndRefresh()
            } else {
                settingsViewModel.toggleUseGpsLocation(false)
            }
        } else if (hasLocationPermission && settingsViewModel.settings.value.useGpsLocation) {
            homeViewModel.refreshPrayerData()
        } else if (!hasLocationPermission && settingsViewModel.settings.value.useGpsLocation) {
            settingsViewModel.toggleUseGpsLocation(false)
        }
    }

    private fun disableReminderNotificationsIfPermissionsMissing() {
        val notificationsAllowed = NotificationManagerCompat.from(this).areNotificationsEnabled()
        val exactAlarmsAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
        if (!notificationsAllowed || !exactAlarmsAllowed) {
            lifecycleScope.launch { settingsRepository.disableAllReminderNotifications() }
        }
    }

    private fun hasLocationPermission(): Boolean =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun enableGpsAndRefresh() {
        settingsViewModel.toggleUseGpsLocation(true)
        lifecycleScope.launch {
            settingsRepository.settingsFlow.first { it.useGpsLocation }
            homeViewModel.refreshPrayerData()
        }
    }

    override fun onStop() {
        super.onStop()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        controllerFuture = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSharedPreferences(NOTIFICATION_PREFS, MODE_PRIVATE).edit()
                .putBoolean(NOTIFICATION_REQUESTED, true).apply()
            notificationPermissionRequestInFlight = true
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        settingsRepository = SettingsRepository(applicationContext)
        prayerTimeRepository = PrayerTimeRepository(settingsRepository)
        settingsViewModel = SettingsViewModel(settingsRepository, applicationContext, prayerTimeRepository)

        val prayerNotificationScheduler = PrayerNotificationScheduler(applicationContext, prayerTimeRepository)
        val prayerManager = com.imn.iman.logic.PrayerManager(settingsRepository, prayerTimeRepository)

        homeViewModel = ViewModelProvider(
            this as ViewModelStoreOwner,
            com.imn.iman.ui.screens.home.HomeViewModel.factory(
                settingsRepository,
                prayerTimeRepository,
                prayerManager,
                settingsViewModel,
                applicationContext
            )
        )[com.imn.iman.ui.screens.home.HomeViewModel::class.java]

        val dao = TasbeehDatabase.getInstance(applicationContext).tasbeehDao()
        tasbeehViewModel =
            ViewModelProvider(this as ViewModelStoreOwner, TasbeehViewModel.factory(dao))[
                TasbeehViewModel::class.java]


        quranViewModel = ViewModelProvider(this)[com.imn.iman.ui.screens.quran.QuranViewModel::class.java]


        lifecycleScope.launch {
            settingsRepository.migrateTranslationDefault()
            settingsRepository.migrateReminderSettingsV2()
            settingsRepository.settingsFlow
                .map { settings -> settings.toPrayerNotificationScheduleConfig() to settings }
                .distinctUntilChanged { old, new -> old.first == new.first }
                .collect { (_, settings) ->
                    PrayerRefreshWork.enqueueIfNeeded(applicationContext, settings)
                    prayerNotificationScheduler.syncNotifications(settings, retryNow = true)
                    WidgetRefresher.refreshAll(applicationContext, "settings")
                }
        }

        lifecycleScope.launch {
            while (true) {
                val settings = settingsRepository.settingsFlow.first()
                prayerNotificationScheduler.maybeDeliverCurrentPrayerNotification(settings)
                delay(30_000L)
            }
        }

        setContent {
            val isLoaded by settingsViewModel.isSettingsLoaded.collectAsState()

            if (!isLoaded) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                return@setContent
            }

            val settings by settingsViewModel.settings.collectAsState()
            val darkTheme =
                when (settings.themeMode) {
                    "Light" -> false
                    "Dark" -> true
                    else -> isSystemInDarkTheme()
                }
            var showFirstLocationExplanation by remember { mutableStateOf(false) }
            var showFirstLocationDeniedDialog by remember { mutableStateOf(false) }
            var showLocationServicesDialog by remember { mutableStateOf(false) }
            var firstLaunchLocationRequest by remember { mutableStateOf(false) }
            val locationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { grants ->
                if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                ) {
                    firstLaunchLocationRequest = false
                    enableGpsAndRefresh()
                    showLocationServicesDialog = !settingsViewModel.isLocationServicesEnabled()
                } else {
                    settingsViewModel.toggleUseGpsLocation(false)
                    if (firstLaunchLocationRequest) showFirstLocationDeniedDialog = true
                    firstLaunchLocationRequest = false
                }
            }
            LaunchedEffect(isLoaded) {
                val prefs = getSharedPreferences(LOCATION_PREFS, MODE_PRIVATE)
                if (isLoaded && hasLocationPermission()) {
                    prefs.edit().putBoolean(FIRST_LOCATION_PROMPT_SHOWN, true).apply()
                } else if (isLoaded) {
                    if (settings.useGpsLocation) settingsViewModel.toggleUseGpsLocation(false)
                    if (!prefs.getBoolean(FIRST_LOCATION_PROMPT_SHOWN, false)) {
                        prefs.edit().putBoolean(FIRST_LOCATION_PROMPT_SHOWN, true).apply()
                        showFirstLocationExplanation = true
                    }
                }
            }
            ImanTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColor,
                pureBlack = settings.pureBlack,
                colorSeed = settings.colorSeed,
                customAccentColor = settings.customAccentColor
            ) {
                if (showFirstLocationExplanation) {
                    AlertDialog(
                        onDismissRequest = {
                            showFirstLocationExplanation = false
                            showFirstLocationDeniedDialog = true
                        },
                        title = { Text(stringResource(R.string.settings_location_access_title)) },
                        text = { Text(stringResource(R.string.settings_location_access_message)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showFirstLocationExplanation = false
                                getSharedPreferences(LOCATION_PREFS, MODE_PRIVATE).edit()
                                    .putBoolean(LOCATION_REQUESTED, true).apply()
                                firstLaunchLocationRequest = true
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            }) { Text(stringResource(R.string.settings_update_continue)) }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showFirstLocationExplanation = false
                                showFirstLocationDeniedDialog = true
                            }) { Text(stringResource(R.string.settings_not_now)) }
                        }
                    )
                }
                if (showFirstLocationDeniedDialog) {
                    AlertDialog(
                        onDismissRequest = { showFirstLocationDeniedDialog = false },
                        title = { Text(stringResource(R.string.settings_location_permission_denied)) },
                        text = { Text(stringResource(R.string.settings_location_permission_denied_message)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showFirstLocationDeniedDialog = false
                            }) { Text(stringResource(R.string.common_got_it)) }
                        }
                    )
                }
                if (showLocationServicesDialog) {
                    AlertDialog(
                        onDismissRequest = { showLocationServicesDialog = false },
                        title = { Text(stringResource(R.string.settings_enable_device_location)) },
                        text = { Text(stringResource(R.string.settings_enable_device_location_message)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showLocationServicesDialog = false
                                startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            }) { Text(stringResource(R.string.settings_open_location_settings)) }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showLocationServicesDialog = false
                            }) { Text(stringResource(R.string.settings_not_now)) }
                        }
                    )
                }
            }

            val view = LocalView.current
            if (!view.isInEditMode) {
                val window = (view.context as Activity).window
                val insetsController = WindowCompat.getInsetsController(window, view)

                DisposableEffect(darkTheme) {
                    insetsController.isAppearanceLightStatusBars = !darkTheme
                    onDispose {}
                }
            }

            DisposableEffect(settings.keepScreenAwake) {
                if (settings.keepScreenAwake) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose {}
            }

            ThemeTransitionProvider {
                ImanTheme(
                    darkTheme = darkTheme,
                    dynamicColor = settings.dynamicColor,
                    pureBlack = settings.pureBlack,
                    colorSeed = settings.colorSeed,
                    customAccentColor = settings.customAccentColor
                ) {
                    ImanApp(
                        settingsViewModel = settingsViewModel,
                        tasbeehViewModel = tasbeehViewModel,

                        quranViewModel = quranViewModel,
                        homeViewModel = homeViewModel,
                        darkTheme = darkTheme,
                        media3Controller = media3Controller
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val settings = settingsViewModel.settings.value
        // Only intercept volume keys when the tasbeeh immersive screen is active
        // and the user has volume-counting enabled. Everywhere else, pass the
        // event to the OS so the system volume slider works normally.
        if (isOnTasbeehImmersive &&
            settings.tasbeehVolumeEnabled &&
            (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                    keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        ) {
            tasbeehViewModel.incrementActiveCount(animate = settings.tasbeehVolumeAnimation)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImanApp(
    settingsViewModel: SettingsViewModel,
    tasbeehViewModel: TasbeehViewModel,

    quranViewModel: com.imn.iman.ui.screens.quran.QuranViewModel,
    homeViewModel: HomeViewModel,
    darkTheme: Boolean,
    media3Controller: MediaController?
) {
    var showCreateSheet by remember { mutableStateOf(false) }

    val navController = rememberNavController()
    val settings by settingsViewModel.settings.collectAsState()
    val appContext = LocalContext.current.applicationContext
    val releaseNotesRepository = remember(appContext) { ReleaseNotesRepository(appContext) }
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    val screenContentPadding = systemBarsPadding.add(top = 64.dp, bottom = 86.dp)
    var pendingReleaseNotes by remember { mutableStateOf<com.imn.iman.data.model.ReleaseNote?>(null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val hazeState = remember { HazeState() }
    val showNavBar = AppDestinations.entries.any { it.route == currentRoute }

    // Keep MainActivity.isOnTasbeehImmersive in sync with the current route so that
    // volume key interception is scoped to the Tasbih immersive screen only.
    val activity = LocalContext.current as? MainActivity
    SideEffect {
        activity?.isOnTasbeehImmersive = currentRoute?.startsWith("tasbeeh/immersive") == true
    }
    val currentDestination = AppDestinations.fromRoute(currentRoute) ?: AppDestinations.HOME

    LaunchedEffect(currentRoute, settings.lastSeenAppVersionCode) {
        val currentVersionCode = BuildConfig.VERSION_CODE
        when {
            settings.lastSeenAppVersionCode == 0 -> {
                settingsViewModel.markReleaseNotesSeen(currentVersionCode)
                pendingReleaseNotes = null
            }

            settings.lastSeenAppVersionCode < currentVersionCode -> {
                pendingReleaseNotes = releaseNotesRepository.noteForVersion(currentVersionCode)
                if (pendingReleaseNotes == null) {
                    settingsViewModel.markReleaseNotesSeen(currentVersionCode)
                }
            }

            else -> pendingReleaseNotes = null
        }
    }

    val startRoute = remember {
        val saved = settingsViewModel.settings.value.startupTab
        AppDestinations.fromRoute(saved)?.route ?: AppDestinations.HOME.route
    }

    val onNavigateTab: (AppDestinations) -> Unit = { dest ->
        if (currentRoute != dest.route) {
            navController.navigate(dest.route) {
                popUpTo(startRoute) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxSize().hazeSource(hazeState),
            ) {
                NavHost(
                    navController = navController,
                    startDestination = startRoute,
                    modifier = Modifier.fillMaxSize(),
                ) {

                    composable(
                        route = AppDestinations.HOME.route,
                        enterTransition = { tabEnter() },
                        exitTransition = { tabExit() },
                        popEnterTransition = { tabEnter() },
                        popExitTransition = { tabExit() },
                    ) {
                        HomeScreen(
                            viewModel = homeViewModel,
                            hazeState = hazeState,
                            contentPadding = screenContentPadding,
                            onSettingsClick = {
                                navController.navigate(SETTINGS_ROUTE) { launchSingleTop = true }
                            },
                            onReminderClick = { focusId ->
                                navController.navigate(prayerSettingsRoute(focusId)) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }


                    composable(
                        route = AppDestinations.TASBEEH.route,
                        enterTransition = { tabEnter() },
                        exitTransition = { tabExit() },
                        popEnterTransition = {
                            if (initialState.destination.route == TASBEEH_IMMERSIVE_ROUTE) {
                                EnterTransition.None
                            } else {
                                tabEnter()
                            }
                        },
                        popExitTransition = { tabExit() },
                    ) {
                        TasbeehScreen(
                            viewModel = tasbeehViewModel,
                            beadColor = Color(android.graphics.Color.parseColor(settings.tasbihBeadColor)),
                            onCollectionTap = { collection ->
                                navController.navigate("tasbeeh/immersive/${collection.id}")
                            },
                            onEditCollection = { showCreateSheet = true },

                            onSettingsClick = {
                                navController.navigate(SETTINGS_ROUTE) { launchSingleTop = true }
                            },
                            hazeState = hazeState,
                            contentPadding = screenContentPadding
                        )
                    }


                    composable(
                        route = AppDestinations.QURAN.route,
                        enterTransition = { tabEnter() },
                        exitTransition = { tabExit() },
                        popEnterTransition = { tabEnter() },
                        popExitTransition = { tabExit() },
                    ) {
                        com.imn.iman.ui.screens.quran.QuranSurahListScreen(
                            onSurahTap = { num -> navController.navigate("quran/$num") },
                            onBookmarkedAyahTap = { surah, ayah ->
                                navController.navigate("quran/$surah?ayah=$ayah")
                            },
                            onSettingsClick = {
                                navController.navigate(SETTINGS_ROUTE) { launchSingleTop = true }
                            },
                            viewModel = quranViewModel,
                            settingsViewModel = settingsViewModel
                        )
                    }

                    composable(
                        route = "quran/{surahNumber}?ayah={ayah}",
                        arguments = listOf(
                            navArgument("surahNumber") { type = NavType.IntType },
                            navArgument("ayah") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        ),
                        enterTransition = { subScreenEnter() },
                        exitTransition = { subScreenExit() },
                        popEnterTransition = { subScreenPopEnter() },
                        popExitTransition = { subScreenPopExit() },
                    ) { backStackEntry ->
                        val surahNumber = backStackEntry.arguments?.getInt("surahNumber") ?: 1
                        val initialAyahIndex = backStackEntry.arguments?.getString("ayah")?.toIntOrNull()
                        com.imn.iman.ui.screens.quran.QuranReaderScreen(
                            surahNumber = surahNumber,
                            initialAyahIndex = initialAyahIndex,
                            onBack = { navController.popBackStack() },
                            onNextSurah = { next ->
                                navController.navigate("quran/$next") {
                                    popUpTo("quran/$surahNumber") { inclusive = true }
                                }
                            },
                            viewModel = quranViewModel,
                            settingsViewModel = settingsViewModel,
                            media3Controller = media3Controller
                        )
                    }



                    composable(
                        route = SETTINGS_ROUTE,
                        enterTransition = { subScreenEnter() },
                        exitTransition = { subScreenExit() },
                        popEnterTransition = { subScreenPopEnter() },
                        popExitTransition = { subScreenPopExit() },
                    ) {
                        SettingsScreen(

                            onNavigateAppearance = {
                                navController.navigate(SETTINGS_APPEARANCE_ROUTE)
                            },
                            onNavigateWidget = {
                                navController.navigate(SETTINGS_WIDGET_ROUTE)
                            },
                            onNavigatePrayer = {
                                navController.navigate(com.imn.iman.ui.navigation.SETTINGS_PRAYER_ROUTE)
                            },
                            onNavigateTasbeeh = {
                                navController.navigate(SETTINGS_TASBEEH_ROUTE)
                            },
                            onNavigateAbout = {
                                navController.navigate(com.imn.iman.ui.navigation.SETTINGS_ABOUT_ROUTE)
                            },
                            onBack = {
                                navController.popBackStack()
                            },
                            onLanguageChanged = settingsViewModel::ensureLocationName
                        )
                    }


                    composable(
                        route = SETTINGS_APPEARANCE_ROUTE,
                        enterTransition = { subScreenEnter() },
                        exitTransition = { subScreenExit() },
                        popEnterTransition = { subScreenPopEnter() },
                        popExitTransition = { subScreenPopExit() },
                    ) {
                        AppearanceSettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = SETTINGS_PRAYER_FOCUS_ROUTE,
                        arguments = listOf(
                            navArgument("focus") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        ),
                        enterTransition = { subScreenEnter() },
                        exitTransition = { subScreenExit() },
                        popEnterTransition = { subScreenPopEnter() },
                        popExitTransition = { subScreenPopExit() },
                    ) { backStackEntry ->
                        PrayerSettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() },
                            focusId = backStackEntry.arguments?.getString("focus")
                        )
                    }

                    composable(
                        route = SETTINGS_WIDGET_ROUTE,
                        enterTransition = { subScreenEnter() },
                        exitTransition = { subScreenExit() },
                        popEnterTransition = { subScreenPopEnter() },
                        popExitTransition = { subScreenPopExit() },
                    ) {
                        WidgetSettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = com.imn.iman.ui.navigation.SETTINGS_ABOUT_ROUTE,
                        enterTransition = { subScreenEnter() },
                        exitTransition = { subScreenExit() },
                        popEnterTransition = { subScreenPopEnter() },
                        popExitTransition = { subScreenPopExit() },
                    ) {
                        AboutSettingsScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateCredits = { navController.navigate(SETTINGS_CREDITS_ROUTE) }
                        )
                    }

                    composable(
                        route = SETTINGS_CREDITS_ROUTE,
                        enterTransition = { subScreenEnter() },
                        exitTransition = { subScreenExit() },
                        popEnterTransition = { subScreenPopEnter() },
                        popExitTransition = { subScreenPopExit() },
                    ) {
                        CreditsSettingsScreen(onBack = { navController.popBackStack() })
                    }

                    composable(
                        route = SETTINGS_TASBEEH_ROUTE,
                        enterTransition = { subScreenEnter() },
                        exitTransition = { subScreenExit() },
                        popEnterTransition = { subScreenPopEnter() },
                        popExitTransition = { subScreenPopExit() },
                    ) {
                        TasbeehCustomizeScreen(
                            viewModel = settingsViewModel,

                            onBack = { navController.popBackStack() }
                        )
                    }

                    // --- NEW IMMERSIVE & EDITOR ROUTES ---

                    composable(
                        route = TASBEEH_IMMERSIVE_ROUTE,
                        arguments = listOf(navArgument("collectionId") { type = NavType.StringType }),
                        enterTransition = { subScreenEnter() },
                        exitTransition = { subScreenExit() },
                        popEnterTransition = { subScreenPopEnter() },
                        popExitTransition = { ExitTransition.None },
                    ) { backStackEntry ->
                        val collectionId = backStackEntry.arguments?.getString("collectionId") ?: ""
                        val collections by tasbeehViewModel.collections.collectAsStateWithLifecycle()
                        val collection = collections.find { it.id.toString() == collectionId }
                        if (collection != null) {

                            TasbeehImmersiveScreen(
                                viewModel = tasbeehViewModel,

                                collection = collection,

                                settings = settings,
                                onExit = { navController.popBackStack() },
                            )
                        }
                    }


                }
            }
        }

        AnimatedVisibility(
            visible = showNavBar,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 30.dp),
        ) {
            PillNavBar(
                currentDestination = currentDestination,
                onDestinationSelected = onNavigateTab,
                hazeState = hazeState,
            )
        }

        AnimatedVisibility(
            visible = currentRoute == AppDestinations.TASBEEH.route,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier =
                Modifier.align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 122.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,

                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialShapes.Square.toShape(),
                elevation = FloatingActionButtonDefaults.elevation(),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.tasbih_create),
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        if (showCreateSheet) {
            CreateCollectionSheet(
                viewModel = tasbeehViewModel,
                settingsViewModel = settingsViewModel,
                onDismiss = { showCreateSheet = false },
            )
        }


        pendingReleaseNotes?.let { note ->
            ReleaseNotesDialog(
                note = note,
                onDismiss = {
                    settingsViewModel.markReleaseNotesSeen(BuildConfig.VERSION_CODE)
                    pendingReleaseNotes = null
                }
            )
        }
    }
}

private val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
private const val M3Duration = 500

// --- LATERAL TAB ANIMATIONS ---
private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabEnter(): EnterTransition =
    fadeIn(
        animationSpec = tween(
            durationMillis = 350,
            delayMillis = 150,
            easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
        )
    ) + scaleIn(
        initialScale = 0.92f,
        animationSpec = tween(M3Duration, easing = EmphasizedEasing)
    )

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabExit(): ExitTransition =
    fadeOut(
        animationSpec = tween(
            durationMillis = 150,
            easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
        )
    ) + scaleOut(
        targetScale = 0.92f,
        animationSpec = tween(M3Duration, easing = EmphasizedEasing)
    )

// --- VERTICAL SUB-SCREEN ANIMATIONS ---
private fun AnimatedContentTransitionScope<NavBackStackEntry>.subScreenEnter(): EnterTransition =
    fadeIn(
        animationSpec = tween(
            durationMillis = 350,
            delayMillis = 100,
            easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
        )
    ) + scaleIn(
        initialScale = 0.85f,
        animationSpec = tween(M3Duration, easing = EmphasizedEasing)
    )

private fun AnimatedContentTransitionScope<NavBackStackEntry>.subScreenExit(): ExitTransition =
    fadeOut(
        animationSpec = tween(
            durationMillis = 150,
            easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
        )
    ) + scaleOut(
        targetScale = 0.95f,
        animationSpec = tween(M3Duration, easing = EmphasizedEasing)
    )

private fun AnimatedContentTransitionScope<NavBackStackEntry>.subScreenPopEnter(): EnterTransition =
    fadeIn(
        animationSpec = tween(
            durationMillis = 350,
            delayMillis = 100,
            easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
        )
    ) + scaleIn(
        initialScale = 0.95f,
        animationSpec = tween(M3Duration, easing = EmphasizedEasing)
    )

private fun AnimatedContentTransitionScope<NavBackStackEntry>.subScreenPopExit(): ExitTransition =
    fadeOut(
        animationSpec = tween(
            durationMillis = 150,
            easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
        )
    ) + scaleOut(
        targetScale = 0.85f,
        animationSpec = tween(M3Duration, easing = EmphasizedEasing)
    )
