package com.imn.iman.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.imn.iman.data.repository.CatalogRepository
import com.imn.iman.R
import com.imn.iman.data.model.TranslationMeta
import com.imn.iman.data.model.ReciterMeta
import com.imn.iman.i18n.quranReadingThemeLabel
import com.imn.iman.i18n.quranScriptLabel

import com.imn.iman.data.repository.TranslationRepository
import com.imn.iman.data.repository.QuranScriptFontRepository
import com.imn.iman.data.repository.UserSettings

import com.imn.iman.ui.theme.PlusJakartaSans
import com.imn.iman.ui.theme.NotoSansArabic
import com.imn.iman.ui.theme.rememberArabicScriptFontFamily

@Composable
private fun SlidingSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp)
    ) {
        val itemWidth = maxWidth / items.size
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = tween(durationMillis = 220),
            label = "reading_settings_segment_offset"
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .fillMaxHeight()
            ) {}

            Row(modifier = Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelect(index) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = PlusJakartaSans,
                            color = if (selectedIndex == index) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsSheet(
    settings: UserSettings,

    isQuranContext: Boolean = true,
    onDismiss: () -> Unit,
    onThemeChange: (String) -> Unit,
    onArabicSizeChange: (Float) -> Unit,
    onTranslationSizeChange: (Float) -> Unit,
    onShowTranslationChange: (Boolean) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,

    onTranslationLangChange: (String) -> Unit,
    onReciterChange: (String) -> Unit,
    onScriptChange: (String) -> Unit,
    availableQuranScripts: Set<String> = QuranScriptFontRepository.bundledScripts(),
    onOpenTranslationPicker: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = if (isQuranContext) {
        listOf(
            stringResource(R.string.quran_settings_tab_display),
            stringResource(R.string.quran_settings_tab_text),
            stringResource(R.string.quran_settings_tab_audio)
        )
    } else {
        listOf(stringResource(R.string.quran_settings_tab_display))
    }

    LaunchedEffect(isQuranContext) {
        if (!isQuranContext) selectedTab = 0
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.quran_reading_settings_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.padding(horizontal = 28.dp)
                        .padding(bottom = 16.dp)
                )

                // LIVE PREVIEW BOX
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (settings.selectedScript == "indopak")
                                        "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِیْمِ "
                                    else "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                                    fontFamily = rememberArabicScriptFontFamily(settings.selectedScript, availableQuranScripts),
                                    fontSize = settings.arabicSizeSp.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                if (settings.selectedScript == "indopak") {
                                    Text(
                                        text = "1:1",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = PlusJakartaSans,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                    )
                                }
                            }
                        }
                        if (settings.showTranslation) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
                                fontFamily = PlusJakartaSans,
                                fontSize = settings.translationSizeSp.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                SlidingSegmentedControl(
                    items = tabs,
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )

                Spacer(Modifier.height(16.dp))

                // Using LazyColumn for the whole tab content to ensure full scrollability
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    when (selectedTab) {
                        0 -> { // Display
                            item {
                                SettingLabel(stringResource(R.string.quran_background_theme))
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val systemIsDark = isSystemInDarkTheme()
                                    listOf(
                                        "SYSTEM" to ((if (systemIsDark) Color.Black else Color.White) to (if (systemIsDark) Color.White else Color.Black)),
                                        "DARK" to (Color.Black to Color.White),
                                        "PAPER" to (Color(0xFFF5E6C8) to Color.Black),
                                        "LIGHT" to (Color.White to Color.Black)
                                    ).forEach { (value, colors) ->
                                        ThemePreviewCard(
                                            label = quranReadingThemeLabel(value),
                                            selected = settings.readingTheme == value,
                                            bgColor = colors.first,
                                            fgColor = colors.second,
                                            onClick = { onThemeChange(value) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            item {
                                SettingToggle(label = stringResource(R.string.quran_keep_screen_on), checked = settings.readingKeepScreenOn, onCheckedChange = onKeepScreenOnChange)
                            }
                        }
                        1 -> { // Text & Translation
                            if (!isQuranContext) {
                                item { Spacer(Modifier.height(1.dp)) }
                            } else {
                            item {
                                val context = LocalContext.current
                                val currentMeta = remember(settings.selectedTranslationLang) {
                                    CatalogRepository.translations(context).find { it.id == settings.selectedTranslationLang }
                                }
                                val langLabel = currentMeta?.let { "${it.langCode.uppercase()} • ${it.translatorName}" }
                                    ?: if (settings.selectedTranslationLang.isBlank()) stringResource(R.string.quran_translation_none)
                                    else stringResource(R.string.quran_translation_select)

                                SettingLabel(stringResource(R.string.quran_active_translation))
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = onOpenTranslationPicker,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Default.Language, null, modifier = Modifier.size(20.dp))
                                        Text(langLabel, style = MaterialTheme.typography.bodyLarge, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            item {
                                SettingLabel(stringResource(R.string.quran_script_style))
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf("uthmani", "indopak").forEach { value ->
                                        ScriptPreviewCard(
                                            label = quranScriptLabel(value),
                                            selected = settings.selectedScript == value,
                                            onClick = { onScriptChange(value) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            item {
                                SettingLabel(stringResource(R.string.quran_arabic_text_size, settings.arabicSizeSp.toInt()))
                                Slider(
                                    value = settings.arabicSizeSp,
                                    onValueChange = onArabicSizeChange,
                                    valueRange = 16f..64f,
                                    steps = 11 // 16,20,24,28,32,36,40,44,48,52,56,60,64
                                )
                            }

                            if (settings.showTranslation) {
                                item {
                                    SettingLabel(stringResource(R.string.quran_translation_size, settings.translationSizeSp.toInt()))
                                    Slider(
                                        value = settings.translationSizeSp,
                                        onValueChange = onTranslationSizeChange,
                                        valueRange = 12f..28f,
                                        steps = 7
                                    )
                                }
                            }

                            item {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Spacer(Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                                    SettingToggle(label = stringResource(R.string.quran_show_translation), checked = settings.showTranslation, onCheckedChange = onShowTranslationChange)
                                }
                            }
                            }
                        }
                        2 -> { // Audio
                            if (isQuranContext) item {
                                val reciters = remember { ReciterMeta.everyayahDefaults() }

                                Column {
                                    SettingLabel(stringResource(R.string.quran_select_reciter))
                                    Spacer(Modifier.height(8.dp))
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    reciters.forEach { reciter ->
                                        val isSelected = settings.selectedReciterId == reciter.id ||
                                            settings.selectedReciterId == reciter.id.removePrefix("everyayah_")

                                        Surface(
                                            onClick = { onReciterChange(reciter.id) },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
                                            border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                                    contentDescription = null,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                                Column {
                                                    Text(
                                                        text = reciter.name,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontFamily = PlusJakartaSans,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                    Text(
                                                        text = reciter.style,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontFamily = PlusJakartaSans,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontFamily = PlusJakartaSans,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun SettingToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontFamily = PlusJakartaSans, color = MaterialTheme.colorScheme.onSurface)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun ThemePreviewCard(
    label: String,
    selected: Boolean,
    bgColor: Color,
    fgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .border(2.dp, borderColor, RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp),
            color = bgColor,
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "بِسْمِ اللَّهِ",
                    color = fgColor,
                    fontFamily = NotoSansArabic,
                    fontSize = 18.sp
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = PlusJakartaSans,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ScriptPreviewCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val containerBg = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .border(2.dp, borderColor, RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp),
            color = containerBg,
            shadowElevation = 1.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = PlusJakartaSans,
                    color = contentColor,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val border = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontFamily = PlusJakartaSans,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationPickerSheet(
    selectedId: String,
    isDownloading: Boolean,
    progress: Float,
    onSelectNone: () -> Unit,
    onSelect: (TranslationMeta) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val otherLanguageLabel = stringResource(R.string.quran_translation_other)
    val downloadedLabel = stringResource(R.string.quran_translation_downloaded_section)
    val catalog = remember(context, otherLanguageLabel) {
        CatalogRepository.translations(context)
            .groupBy { it.langName.ifBlank { otherLanguageLabel } }
            .toSortedMap()
    }
    var searchText by remember { mutableStateOf("") }
    val visibleCatalog = remember(catalog, searchText, context, downloadedLabel) {
        val filtered = catalog.filterKeys { it.contains(searchText.trim(), ignoreCase = true) }
        val downloaded = filtered.values.flatten()
            .filter { TranslationRepository.isDownloaded(context, it.id) }
        buildList<Pair<String, List<TranslationMeta>>> {
            if (downloaded.isNotEmpty()) add(downloadedLabel to downloaded)
            filtered.forEach { (language, translations) ->
                val notDownloaded = translations.filterNot {
                    TranslationRepository.isDownloaded(context, it.id)
                }
                if (notDownloaded.isNotEmpty()) add(language to notDownloaded)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                Text(
                    stringResource(R.string.quran_translation_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PlusJakartaSans),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.quran_translation_search_language)) },
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))

                if (isDownloading) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(stringResource(R.string.settings_update_downloading), style = MaterialTheme.typography.bodyMedium,
                            fontFamily = PlusJakartaSans, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        val isNoneSelected = selectedId.isBlank()
                        Surface(
                            onClick = { if (!isDownloading) onSelectNone() },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isNoneSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else Color.Transparent,
                            border = if (isNoneSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(stringResource(R.string.quran_translation_none), style = MaterialTheme.typography.bodyLarge,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = if (isNoneSelected) FontWeight.Bold else FontWeight.Normal)
                                if (isNoneSelected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    visibleCatalog.forEach { (lang, translations) ->
                        item {
                            Text(lang, style = MaterialTheme.typography.labelLarge,
                                fontFamily = PlusJakartaSans, color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp))
                        }
                        items(translations) { meta ->
                            val isDownloaded = TranslationRepository.isDownloaded(context, meta.id)
                            val isSelected = selectedId == meta.id

                            Surface(
                                onClick = { if (!isDownloading) onSelect(meta) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else Color.Transparent,
                                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(meta.translatorName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        Text(
                                            stringResource(
                                                if (isDownloaded) R.string.quran_translation_downloaded_status
                                                else R.string.quran_translation_tap_to_download,
                                                meta.langCode.uppercase()
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = PlusJakartaSans,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    when {
                                        isSelected -> Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                        !isDownloaded -> Icon(Icons.Default.Download, null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
