package com.kaizen.khushu.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kaizen.khushu.R
import com.kaizen.khushu.i18n.LocaleHelper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.kaizen.khushu.ui.screens.tasbeeh.TasbihBeadCustomizerSheet
import com.kaizen.khushu.ui.screens.tasbeeh.TasbihSoundCatalog
import com.kaizen.khushu.ui.screens.tasbeeh.TasbihSoundPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbeehCustomizeScreen(
    viewModel: SettingsViewModel,
    onPreview: () -> Unit,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val localizedContext = remember(context) { LocaleHelper.wrap(context) }
    val soundPlayer = remember { TasbihSoundPlayer(context) }
    var showBeadSheet by remember { mutableStateOf(false) }

    DisposableEffect(soundPlayer) {
        onDispose { soundPlayer.release() }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { SettingsTopBarTitle(stringResource(R.string.settings_tasbih_screen), scrollBehavior) },
                navigationIcon = { SettingsBackButton(onBack) },
                scrollBehavior = scrollBehavior
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

            SettingsGroup(
                title = stringResource(R.string.tasbih_settings_layout),
//                description = "Open the editor or change how beads are styled."
            ) {
                SettingsMenuItem(
                    title = stringResource(R.string.tasbih_settings_edit_screen),
                    subtitle = stringResource(R.string.tasbih_settings_edit_screen_summary),
                    onClick = onPreview
                )
                SettingsMenuItem(
                    title = stringResource(R.string.tasbih_settings_design_beads),
                    subtitle = stringResource(R.string.tasbih_settings_design_beads_summary),
                    onClick = { showBeadSheet = true },
                    showDivider = false
                )
            }

            SettingsGroup(
                title = stringResource(R.string.tasbih_settings_behavior),
//                description = "How the Tasbih screen reacts while you count."
            ) {
                SettingsToggleItem(
                    title = stringResource(R.string.tasbih_settings_dynamic_colors),
                    subtitle = stringResource(R.string.tasbih_settings_dynamic_colors_summary),
                    checked = settings.tasbeehDynamicColors,
                    onCheckedChange = { viewModel.toggleTasbeehDynamicColors(it) }
                )
                SettingsToggleItem(
                    title = stringResource(R.string.tasbih_settings_stealth_mode),
                    subtitle = stringResource(R.string.tasbih_settings_stealth_mode_summary),
                    checked = settings.tasbeehStealthModeAllowed,
                    onCheckedChange = { viewModel.toggleTasbeehStealthModeAllowed(it) }
                )
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
                        onCheckedChange = { viewModel.toggleTasbeehVolumeAnimation(it) },
                        showDivider = false
                    )
                }
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

    if (showBeadSheet) {
        TasbihBeadCustomizerSheet(
            settingsViewModel = viewModel,
            onDismiss = { showBeadSheet = false }
        )
    }
}
