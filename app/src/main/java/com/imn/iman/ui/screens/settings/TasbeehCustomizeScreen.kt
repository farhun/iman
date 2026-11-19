package com.imn.iman.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.imn.iman.R
import com.imn.iman.i18n.LocaleHelper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imn.iman.data.repository.DEFAULT_TASBIH_BACKGROUND_COLOR
import com.imn.iman.data.repository.DEFAULT_TASBIH_BEAD_COLOR
import com.imn.iman.data.repository.DEFAULT_TASBIH_TEXT_COLOR
import com.imn.iman.ui.screens.tasbeeh.TasbihSoundCatalog
import com.imn.iman.ui.screens.tasbeeh.TasbihSoundPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbeehCustomizeScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val localizedContext = remember(context) { LocaleHelper.wrap(context) }
    val soundPlayer = remember { TasbihSoundPlayer(context) }
    var colorPickerTarget by remember { mutableStateOf<String?>(null) }

    DisposableEffect(soundPlayer) {
        onDispose { soundPlayer.release() }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { SettingsTopBarTitle(stringResource(R.string.settings_tasbih_screen), scrollBehavior) },
                navigationIcon = { SettingsBackButton(onBack) },
                scrollBehavior = scrollBehavior,
                colors = SettingsTopAppBarColors()
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))

            SettingsGroup(title = stringResource(R.string.tasbih_settings_colors)) {
                SettingsMenuItem(
                    title = stringResource(R.string.tasbih_background_color),
                    subtitle = settings.tasbihBackgroundColor,
                    onClick = { colorPickerTarget = "background" },
                    showDivider = false
                )

                SettingsMenuItem(
                    title = stringResource(R.string.tasbih_bead_color),
                    subtitle = settings.tasbihBeadColor,
                    onClick = { colorPickerTarget = "bead" },
                    showDivider = false
                )

                SettingsMenuItem(
                    title = stringResource(R.string.tasbih_text_color),
                    subtitle = settings.tasbihTextColor,
                    onClick = { colorPickerTarget = "text" },
                    showDivider = false
                )

            }


            SettingsGroup {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SettingsDropdown(
                        title = stringResource(R.string.tasbih_bead_style),
                        subtitle = stringResource(R.string.tasbih_bead_mode_summary),
                        options = listOf("slide", "press"),
                        selectedOption = settings.tasbihBeadMode,
                        optionLabel = { mode ->
                            localizedContext.getString(
                                if (mode == "press") R.string.tasbih_bead_mode_press
                                else R.string.tasbih_bead_mode_slide
                            )
                        },
                        onOptionSelected = viewModel::setTasbihBeadMode
                    )
                }
            }



            SettingsGroup(
                title = stringResource(R.string.tasbih_settings_behavior),
//                description = "How the Tasbih screen reacts while you count."
            ) {
                SettingsToggleItem(
                    title = stringResource(R.string.tasbih_settings_volume_buttons),
                    subtitle = stringResource(R.string.tasbih_settings_volume_buttons_summary),
                    checked = settings.tasbeehVolumeEnabled,
                    onCheckedChange = { viewModel.toggleTasbeehVolumeEnabled(it) },
                    showDivider = settings.tasbeehVolumeEnabled
                )
                if (settings.tasbeehVolumeEnabled) {
                    SettingsToggleItem(
                        title = stringResource(R.string.tasbih_settings_animate_volume_keys),
                        subtitle = stringResource(R.string.tasbih_settings_animate_volume_keys_summary),
                        checked = settings.tasbeehVolumeAnimation,
                        onCheckedChange = { viewModel.toggleTasbeehVolumeAnimation(it) }
                    )
                }
                SettingsToggleItem(
                    title = stringResource(R.string.settings_haptic_feedback),
                    subtitle = stringResource(R.string.settings_haptic_feedback_summary),
                    checked = settings.hapticsEnabled,
                    onCheckedChange = { viewModel.toggleHaptics(it) },
                    showDivider = false
                )
            }

            SettingsGroup(
                title = stringResource(R.string.tasbih_settings_sound),
//                description = "Bundled Tasbih sounds that play when a count lands."
            ) {
                SettingsToggleItem(
                    title = stringResource(R.string.tasbih_settings_sound_enabled),
                    subtitle = stringResource(R.string.tasbih_settings_sound_enabled_summary),
                    checked = settings.tasbihSoundEnabled,
                    onCheckedChange = { viewModel.toggleTasbihSoundEnabled(it) },
                    showDivider = settings.tasbihSoundEnabled
                )
                if (settings.tasbihSoundEnabled) {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SettingsDropdown(
                            title = stringResource(R.string.tasbih_settings_sound_style),
                            subtitle = stringResource(R.string.tasbih_settings_sound_style_summary),
                            options = TasbihSoundCatalog.options.map { it.id },
                            selectedOption = settings.tasbihSoundId,
                            optionLabel = { id -> localizedContext.getString(TasbihSoundCatalog.optionFor(id).labelResId) },
                            onOptionSelected = viewModel::setTasbihSoundId
                        )
                    }
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        OutlinedButton(
                            onClick = { soundPlayer.play(settings.tasbihSoundId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.tasbih_settings_preview_sound))
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    colorPickerTarget?.let { target ->
        val initialHex = when (target) {
            "background" -> settings.tasbihBackgroundColor
            "bead" -> settings.tasbihBeadColor
            else -> settings.tasbihTextColor
        }
        AdvancedColorPickerSheet(
            initialColor = Color(android.graphics.Color.parseColor(initialHex)),
            onColorSelected = { color ->
                val hex = String.format("#%06X", 0xFFFFFF and color.toArgb())
                when (target) {
                    "background" -> viewModel.setTasbihBackgroundColor(hex)
                    "bead" -> viewModel.setTasbihBeadColor(hex)
                    else -> viewModel.setTasbihTextColor(hex)
                }
                colorPickerTarget = null
            },
            onResetToDefault = {
                when (target) {
                    "background" -> viewModel.setTasbihBackgroundColor(DEFAULT_TASBIH_BACKGROUND_COLOR)
                    "bead" -> viewModel.setTasbihBeadColor(DEFAULT_TASBIH_BEAD_COLOR)
                    else -> viewModel.setTasbihTextColor(DEFAULT_TASBIH_TEXT_COLOR)
                }
                colorPickerTarget = null
            },
            onDismiss = { colorPickerTarget = null }
        )
    }
}
