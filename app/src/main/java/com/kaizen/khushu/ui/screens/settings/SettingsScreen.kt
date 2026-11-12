package com.kaizen.khushu.ui.screens.settings

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
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.kaizen.khushu.R
import com.kaizen.khushu.i18n.LocaleHelper
import com.kaizen.khushu.i18n.SUPPORTED_LANGUAGES
import com.kaizen.khushu.notifications.PrayerNotificationScheduler
import com.kaizen.khushu.widget.PrayerWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateCounter: () -> Unit,
    onNavigateAppearance: () -> Unit,
    onNavigateWidget: () -> Unit,
    onNavigatePrayer: () -> Unit,
    onNavigateTasbeeh: () -> Unit,
    onNavigateAbout: () -> Unit,
    onBack: () -> Unit
) {

    val context = LocalContext.current
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
        val appContext = context.applicationContext
        PrayerWidgetProvider.forceWidgetRefresh(appContext)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            PrayerNotificationScheduler(appContext).syncNotifications()
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { SettingsTopBarTitle("Settings", scrollBehavior) },
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
                title = "Preferences",
//                description = "Prayer, appearance, interaction, and app information."
            ) {
                SettingsMenuItem(
                    title = "Prayer Times",
                    subtitle = "Calculation, reminders, and diagnostics",
                    imageVector = Icons.Default.AccessTime,
                    onClick = onNavigatePrayer
                )

                SettingsMenuItem(
                    title = "Appearance",
                    subtitle = "Theme, colors, and startup screen",
                    imageVector = Icons.Default.ColorLens,
                    onClick = onNavigateAppearance
                )

                SettingsMenuItem(
                    title = "Widget",
                    subtitle = "Custom backgrounds and transparency",
                    imageVector = Icons.Default.Layers,
                    onClick = onNavigateWidget
                )

                SettingsMenuItem(
                    title = "Counter",
                    subtitle = "Haptics and hardware behavior",
                    imageVector = Icons.Default.TouchApp,
                    onClick = onNavigateCounter
                )

                SettingsMenuItem(
                    title = "Tasbih Screen",
                    subtitle = "Customize the tasbih screen and canvas",
                    imageVector = Icons.Default.Tune,
                    onClick = onNavigateTasbeeh
                )

                SettingsMenuItem(
                    title = stringResource(R.string.language_title),
                    subtitle = selectedLanguageName,
                    imageVector = Icons.Default.Language,
                    onClick = { showLanguageDialog.value = true }
                )

                SettingsMenuItem(
                    title = "About Iman",
                    subtitle = "Story, project links, and reporting",
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
                    Text("Cancel")
                }
            }
        )
    }
}
