package com.imn.iman.ui.screens.settings

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Language

import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.imn.iman.R
import com.imn.iman.i18n.LocaleHelper
import com.imn.iman.i18n.SUPPORTED_LANGUAGES
import com.imn.iman.notifications.PrayerNotificationScheduler
import com.imn.iman.widget.WidgetRefresher
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateAppearance: () -> Unit,
    onNavigateWidget: () -> Unit,
    onNavigatePrayer: () -> Unit,
    onNavigateTasbeeh: () -> Unit,
    onNavigateAbout: () -> Unit,
    onBack: () -> Unit,
    onLanguageChanged: () -> Unit = {}
) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = remember(context) {
        var current = context
        while (current !is Activity && current is ContextWrapper) current = current.baseContext
        current as? Activity
    }
    val selectedLanguageTag = remember(context) { mutableStateOf(LocaleHelper.savedTag(context)) }
    val showLanguageDialog = remember { mutableStateOf(false) }
    val systemDefaultLabel = stringResource(R.string.language_system_default)
    val languageOptions = listOf("system" to systemDefaultLabel) +
            SUPPORTED_LANGUAGES.map { it.tag to it.nativeName }
    val selectedLanguageName = languageOptions
        .firstOrNull { it.first == selectedLanguageTag.value }
        ?.second ?: systemDefaultLabel

    fun selectLanguage(tag: String) {
        selectedLanguageTag.value = tag
        showLanguageDialog.value = false
        activity?.let { LocaleHelper.apply(it, tag) }
        onLanguageChanged()
        val appContext = context.applicationContext
        coroutineScope.launch { WidgetRefresher.refreshAll(appContext, "language") }
        PrayerNotificationScheduler.syncInApplicationScope(appContext)
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { SettingsTopBarTitle(stringResource(R.string.common_settings), scrollBehavior) },
                navigationIcon = { SettingsBackButton(onBack) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
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
                title = stringResource(R.string.settings_preferences),
//                description = "Prayer, appearance, interaction, and app information."
            ) {
                SettingsMenuItem(
                    title = stringResource(R.string.settings_prayer_times),
                    subtitle = stringResource(R.string.settings_prayer_times_summary),
                    imageVector = Icons.Default.AccessTime,
                    onClick = onNavigatePrayer
                )

                SettingsMenuItem(
                    title = stringResource(R.string.settings_appearance),
                    subtitle = stringResource(R.string.settings_appearance_summary),
                    imageVector = Icons.Default.ColorLens,
                    onClick = onNavigateAppearance
                )

                SettingsMenuItem(
                    title = stringResource(R.string.settings_tasbih_screen),
                    subtitle = stringResource(R.string.settings_tasbih_screen_summary),
                    imageVector = Icons.Default.Tune,
                    onClick = onNavigateTasbeeh
                )


                SettingsMenuItem(
                    title = stringResource(R.string.widget_title),
                    subtitle = stringResource(R.string.widget_settings_summary),
                    imageVector = Icons.Default.Layers,
                    onClick = onNavigateWidget
                )

                SettingsMenuItem(
                    title = stringResource(R.string.language_title),
                    subtitle = selectedLanguageName,
                    imageVector = Icons.Default.Language,
                    onClick = { showLanguageDialog.value = true }
                )

                SettingsMenuItem(
                    title = stringResource(R.string.about_title),
                    subtitle = stringResource(R.string.settings_about_summary),
                    imageVector = Icons.Default.Info,
                    onClick = onNavigateAbout,
                    showDivider = false
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showLanguageDialog.value) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog.value = false },
            title = { Text(stringResource(R.string.language_dialog_title)) },
            text = {
                Column {
                    languageOptions.forEach { (tag, name) ->
                        val selected = selectedLanguageTag.value == tag
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = { selectLanguage(tag) }
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selected, onClick = null)
                            Text(name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog.value = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}
