package com.imn.iman.ui.screens.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.imn.iman.R
import com.imn.iman.ui.navigation.AppDestinations
import androidx.compose.ui.text.font.FontWeight
import com.imn.iman.ui.theme.PlusJakartaSans
import com.imn.iman.ui.theme.LocalThemeTransitionController
import com.imn.iman.ui.theme.colorSeeds

private fun themeLabelRes(mode: String): Int = when (mode) {
    "System" -> R.string.settings_theme_system
    "Light" -> R.string.settings_theme_light
    "Dark" -> R.string.settings_theme_dark
    else -> 0
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    var showAccentColorPicker by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val transitionController = LocalThemeTransitionController.current
    val buttonCoordinates = remember { mutableStateListOf<LayoutCoordinates?>(null, null, null) }
    val themeOptions = listOf("System", "Light", "Dark")
    val themeIcons = listOf(
        Icons.Default.Settings,
        Icons.Default.WbSunny,
        Icons.Default.DarkMode
    )
    val isDarkTheme = when (settings.themeMode) {
        "Light" -> false
        "Dark" -> true
        else -> isSystemInDarkTheme()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { SettingsTopBarTitle(stringResource(R.string.settings_appearance), scrollBehavior) },
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
                title = stringResource(R.string.settings_theme),
//                description = "Control the overall look and startup behavior of Iman."
            ) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        themeOptions.forEachIndexed { index, label ->
                            SegmentedButton(
                                modifier = Modifier.onGloballyPositioned { buttonCoordinates[index] = it },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size),
                                onClick = {
                                    val center = buttonCoordinates[index]?.boundsInRoot()?.center ?: Offset.Zero
                                    transitionController.captureAndChange(center) {
                                        viewModel.setThemeMode(label)
                                    }
                                },
                                selected = settings.themeMode == label,
                                icon = {
                                    Icon(
                                        imageVector = themeIcons[index],
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(themeLabelRes(label)),
                                        fontFamily = PlusJakartaSans,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            )
                        }
                    }
                }


                SettingsToggleItem(
                    title = stringResource(R.string.settings_keep_screen_awake),
                    subtitle = stringResource(R.string.settings_keep_screen_awake_summary),
                    checked = settings.keepScreenAwake,
                    onCheckedChange = viewModel::toggleKeepScreenAwake
                )

                SettingsToggleItem(
                    title = stringResource(R.string.settings_show_quran_exit_reminder),
                    checked = !settings.quranExitReminderDisabled,
                    onCheckedChange = { enabled ->
                        viewModel.setQuranExitReminderDisabled(!enabled)
                    }
                )

                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_startup_screen),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_startup_screen_summary),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PlusJakartaSans),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            R.string.nav_home to AppDestinations.HOME.route,
                            R.string.nav_tasbih to AppDestinations.TASBEEH.route,
                            R.string.nav_quran to AppDestinations.QURAN.route
                        ).forEach { (labelRes, route) ->
                            FilterChip(
                                selected = settings.startupTab == route,
                                onClick = { viewModel.setStartupTab(route) },
                                label = { Text(stringResource(labelRes), fontFamily = PlusJakartaSans) }
                            )
                        }
                    }
                }
            }

            SettingsGroup(
                title = stringResource(R.string.settings_color),
//                description = "Use wallpaper-aware colors or choose a manual accent."
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsToggleItem(
                        title = stringResource(R.string.settings_dynamic_color),
                        subtitle = stringResource(R.string.settings_dynamic_color_summary),
                        checked = settings.dynamicColor,
                        onCheckedChange = viewModel::toggleDynamicColor
                    )
                }

                if (isDarkTheme) {
                    SettingsToggleItem(
                        title = stringResource(R.string.settings_pure_amoled_black),
                        subtitle = stringResource(R.string.settings_pure_amoled_black_summary),
                        checked = settings.pureBlack,
                        onCheckedChange = viewModel::togglePureBlack,
                        showDivider = !settings.dynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                    )
                }

                AnimatedVisibility(
                    visible = !settings.dynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(modifier = Modifier.padding(20.dp)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            colorSeeds.forEach { (key, color) ->
                                AccentColorChip(
                                    color = color,
                                    selected = settings.customAccentColor == null && settings.colorSeed == key,
                                    onClick = { viewModel.setColorSeed(key) }
                                )
                            }
                            CustomAccentColorChip(
                                color = settings.customAccentColor?.let { Color(it) },
                                selected = settings.customAccentColor != null,
                                onClick = { showAccentColorPicker = true }
                            )
                        }
                    }
                }

            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showAccentColorPicker) {
        AdvancedColorPickerSheet(
            initialColor = settings.customAccentColor?.let { Color(it) }
                ?: MaterialTheme.colorScheme.primary,
            onColorSelected = { color ->
                viewModel.setCustomAccentColor(color.toArgb())
                showAccentColorPicker = false
            },
            onDismiss = { showAccentColorPicker = false }
        )
    }
}

@Composable
private fun AccentColorChip(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (color == colorSeeds["default"]) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White.copy(alpha = 0.8f)
            )
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}

@Composable
private fun CustomAccentColorChip(
    color: Color?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val swatchColor = color ?: MaterialTheme.colorScheme.surfaceContainer
    val iconTint = if (color == null) MaterialTheme.colorScheme.primary
    else if (color.luminance() < 0.5f) Color.White else Color.Black

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(swatchColor)
            .then(
                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = stringResource(R.string.settings_custom_color),
            modifier = Modifier.size(20.dp),
            tint = iconTint
        )
    }
}
