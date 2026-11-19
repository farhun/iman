package com.imn.iman.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.imn.iman.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val QURANWBW_URL = "https://www.QuranWBW.com"
private const val QUL_TEXT_URL = "https://qul.tarteel.ai/resources/quran-script/86"
private const val QUL_FONT_URL = "https://qul.tarteel.ai/resources/font/245"
private const val ARCHIVE_URL = "https://archive.org/details/adhan.recordings.from.doha.qatar"
private const val PUBLIC_DOMAIN_MARK_URL = "https://creativecommons.org/publicdomain/mark/1.0/"
private const val EVERYAYAH_URL = "https://everyayah.com"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var selectedDocument by remember { mutableStateOf<CreditDocument?>(null) }

    fun openUrl(url: String) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    val indoPakTitle = stringResource(R.string.about_credits_indopak_title)
    val uthmaniTitle = stringResource(R.string.about_credits_uthmani_title)
    val fontsTitle = stringResource(R.string.about_credits_fonts_title)
    val audioTitle = stringResource(R.string.about_credits_audio_title)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { SettingsTopBarTitle(stringResource(R.string.about_credits_title), scrollBehavior) },
                navigationIcon = { SettingsBackButton(onBack) },
                scrollBehavior = scrollBehavior,
                colors = SettingsTopAppBarColors()
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingsSectionCard(title = indoPakTitle) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CreditParagraph(R.string.about_credits_quranwbw_intro)
                        CreditParagraph(R.string.about_credits_quranwbw_permission)
                        CreditParagraph(R.string.about_credits_quranwbw_copyright)
                        CreditParagraph(R.string.about_credits_quranwbw_text_sources)
                        CreditParagraph(R.string.about_credits_quranwbw_font_development)
                        CreditParagraph(R.string.about_credits_quranwbw_verification)
                        CreditParagraph(R.string.about_credits_quranwbw_thanks)
                        CreditLinkButton(stringResource(R.string.about_credits_quranwbw_link)) {
                            openUrl(QURANWBW_URL)
                        }
                        CreditLinkButton(stringResource(R.string.about_credits_view_terms)) {
                            selectedDocument = CreditDocument(R.raw.notice_quranwbw, indoPakTitle)
                        }
                    }
                }
            }
            item {
                SettingsSectionCard(title = uthmaniTitle) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CreditParagraph(R.string.about_credits_qpc_hafs)
                        CreditLinkButton(stringResource(R.string.about_credits_qul_text_link)) {
                            openUrl(QUL_TEXT_URL)
                        }
                        CreditLinkButton(stringResource(R.string.about_credits_qul_font_link)) {
                            openUrl(QUL_FONT_URL)
                        }
                        CreditLinkButton(stringResource(R.string.about_credits_view_terms)) {
                            selectedDocument = CreditDocument(R.raw.notice_qpc_hafs, uthmaniTitle)
                        }
                    }
                }
            }
            item {
                SettingsSectionCard(title = fontsTitle) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CreditParagraph(R.string.about_credits_noto_sans_arabic)
                        CreditLinkButton(stringResource(R.string.about_credits_view_license)) {
                            selectedDocument = CreditDocument(R.raw.license_noto_sans_arabic, fontsTitle)
                        }
                        CreditParagraph(R.string.about_credits_plus_jakarta_sans)
                        CreditLinkButton(stringResource(R.string.about_credits_view_license)) {
                            selectedDocument = CreditDocument(R.raw.license_plus_jakarta_sans, fontsTitle)
                        }
                    }
                }
            }
            item {
                SettingsSectionCard(title = audioTitle) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CreditParagraph(R.string.about_credits_adhan)
                        CreditLinkButton(stringResource(R.string.about_credits_archive_link)) {
                            openUrl(ARCHIVE_URL)
                        }
                        CreditLinkButton(stringResource(R.string.about_credits_public_domain_link)) {
                            openUrl(PUBLIC_DOMAIN_MARK_URL)
                        }
                        CreditLinkButton(stringResource(R.string.about_credits_view_details)) {
                            selectedDocument = CreditDocument(R.raw.notice_adhan, audioTitle)
                        }
                        CreditParagraph(R.string.about_credits_everyayah)
                        CreditLinkButton(stringResource(R.string.about_credits_everyayah_link)) {
                            openUrl(EVERYAYAH_URL)
                        }
                    }
                }
            }
            item {
                SettingsSectionCard(title = stringResource(R.string.about_credits_data_libraries_title)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CreditParagraph(R.string.about_credits_prayer_times)
                        CreditParagraph(R.string.about_credits_translations)
                        CreditParagraph(R.string.about_credits_libraries)
                    }
                }
            }
        }
    }

    val document = selectedDocument
    if (document != null) {
        CreditTextDialog(
            resourceId = document.resourceId,
            title = document.title,
            onDismiss = { selectedDocument = null }
        )
    }
}

@Composable
private fun CreditParagraph(stringId: Int) {
    Text(
        text = stringResource(stringId),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun CreditLinkButton(label: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label)
    }
}

private data class CreditDocument(val resourceId: Int, val title: String)

@Composable
private fun CreditTextDialog(resourceId: Int, title: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var documentText by remember(resourceId) { mutableStateOf<String?>(null) }

    LaunchedEffect(resourceId) {
        documentText = withContext(Dispatchers.IO) {
            context.resources.openRawResource(resourceId).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )

                }
                if (documentText == null) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        text = documentText.orEmpty(),
                        modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                    FilledTonalButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.common_close))
                    }
                }
            }
        }
    }
}
