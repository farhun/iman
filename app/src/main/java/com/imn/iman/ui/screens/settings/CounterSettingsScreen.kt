package com.imn.iman.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.imn.iman.R
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { SettingsTopBarTitle(stringResource(R.string.settings_counter), scrollBehavior) },
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
                title = stringResource(R.string.settings_haptics),
//                description = "Physical feedback that applies across Iman counters."
            ) {
                SettingsToggleItem(
                    title = stringResource(R.string.settings_haptic_feedback),
                    subtitle = stringResource(R.string.settings_haptic_feedback_summary),
                    checked = settings.hapticsEnabled,
                    onCheckedChange = { viewModel.toggleHaptics(it) },
                    showDivider = false
                )
            }

            SettingsGroup(
                title = stringResource(R.string.settings_hardware),
//                description = "Optional device-level controls for counting."
            ) {
                SettingsToggleItem(
                    title = stringResource(R.string.settings_volume_key_counting),
                    subtitle = stringResource(R.string.settings_volume_key_counting_summary),
                    checked = settings.volumeCounting,
                    onCheckedChange = { viewModel.toggleVolumeCounting(it) },
                    showDivider = false
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
