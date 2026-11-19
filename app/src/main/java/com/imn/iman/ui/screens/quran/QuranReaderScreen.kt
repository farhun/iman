package com.imn.iman.ui.screens.quran

import android.widget.Toast
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.session.MediaController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imn.iman.R
import com.imn.iman.data.model.AyahBlock
import com.imn.iman.data.model.ContentBlock
import com.imn.iman.data.model.ContentSource
import com.imn.iman.i18n.LocaleHelper
import com.imn.iman.i18n.quranReadingModeLabel
import com.imn.iman.i18n.revelationPlaceLabel

import com.imn.iman.ui.components.BlockActionSheet
import com.imn.iman.ui.components.ReadingSettingsSheet
import com.imn.iman.ui.components.TranslationPickerSheet

import com.imn.iman.data.repository.TranslationRepository

import com.imn.iman.ui.screens.learn.BlockRenderer
import com.imn.iman.ui.screens.settings.SettingsViewModel
import com.imn.iman.ui.theme.PlusJakartaSans
import com.imn.iman.ui.theme.NotoSansArabic
import com.imn.iman.ui.theme.rememberArabicScriptFontFamily
import com.imn.iman.ui.theme.colorSeeds
import com.imn.iman.ui.theme.DefaultSeedColor
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── Theme helpers ──────────────────────────────────────────────────────────────

private val ThemeDark = Color.Black
private val ThemePaper = Color(0xFFF5E6C8)
private val ThemeLight = Color.White

@Composable
private fun bgColor(theme: String): Color {
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    return when (theme) {
        "DARK" -> ThemeDark
        "PAPER" -> ThemePaper
        "LIGHT" -> ThemeLight
        else -> if (isSystemDark) ThemeDark else ThemeLight
    }
}

@Composable
private fun contentColor(theme: String): Color {
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    return when (theme) {
        "DARK" -> Color.White
        "PAPER", "LIGHT" -> Color.Black
        else -> if (isSystemDark) Color.White else Color.Black
    }
}

@Composable
private fun readingColorScheme(
    readingTheme: String,
    dynamicColor: Boolean,
    colorSeed: String,
    customAccentColor: Int?,
): ColorScheme {
    val context = LocalContext.current
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = when (readingTheme) {
        "DARK" -> true
        "LIGHT" -> false
        "PAPER" -> false
        else -> isSystemDark
    }

    val neutralScheme = if (isDark) darkColorScheme() else lightColorScheme()
    val generatedColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        else -> dynamicColorScheme(
            seedColor = customAccentColor?.let { Color(it) }
                ?: colorSeeds[colorSeed]
                ?: DefaultSeedColor,
            isDark = isDark,
            isAmoled = false,
            style = PaletteStyle.TonalSpot,
        )
    }
    return neutralScheme.copy(
        primary = generatedColorScheme.primary,
        onPrimary = generatedColorScheme.onPrimary,
        primaryContainer = generatedColorScheme.primaryContainer,
        onPrimaryContainer = generatedColorScheme.onPrimaryContainer,
        inversePrimary = generatedColorScheme.inversePrimary,
        secondary = generatedColorScheme.secondary,
        onSecondary = generatedColorScheme.onSecondary,
        secondaryContainer = generatedColorScheme.secondaryContainer,
        onSecondaryContainer = generatedColorScheme.onSecondaryContainer,
        tertiary = generatedColorScheme.tertiary,
        onTertiary = generatedColorScheme.onTertiary,
        tertiaryContainer = generatedColorScheme.tertiaryContainer,
        onTertiaryContainer = generatedColorScheme.onTertiaryContainer,
        surfaceTint = neutralScheme.surface,
    )
}

@Composable
private fun JuzHeader(juzNumber: Int, contentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = contentColor.copy(alpha = 0.15f),
        )
        Text(
            text = stringResource(R.string.quran_juz_number, juzNumber),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.4f),
            modifier = Modifier.padding(horizontal = 12.dp),
            fontFamily = PlusJakartaSans,
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = contentColor.copy(alpha = 0.15f),
        )
    }
}

@Composable
private fun SajdaIndicator(type: String, contentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(
                    if (type == "obligatory") MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    androidx.compose.foundation.shape.CircleShape
                )
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(if (type == "obligatory") R.string.quran_sajdah_obligatory else R.string.quran_sajdah_mustahabb),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.5f),
            fontFamily = PlusJakartaSans,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    surahNumber: Int,
    initialAyahIndex: Int? = null,
    onBack: () -> Unit,
    onNextSurah: (Int) -> Unit,
    viewModel: QuranViewModel,
    settingsViewModel: SettingsViewModel,
    media3Controller: MediaController?,
    quranAudioViewModel: QuranAudioViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val ayahs by viewModel.currentAyahs
    val translations by viewModel.currentTranslation
    val scriptMap by viewModel.scriptMap
    val isLoading by viewModel.isLoading

    val chapters by viewModel.chapters

    val settings by settingsViewModel.settings.collectAsState()
    var bookmarkedAyahKeys by remember(surahNumber) { mutableStateOf(settings.bookmarkedAyahs) }
    LaunchedEffect(settings.bookmarkedAyahs) {
        bookmarkedAyahKeys = settings.bookmarkedAyahs
    }
    val availableQuranScripts by settingsViewModel.availableQuranScripts.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val scrollIndicatorScope = rememberCoroutineScope()
    var showScrollIndicator by remember { mutableStateOf(false) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collectLatest { scrolling ->
            if (scrolling) showScrollIndicator = true else {
                delay(750)
                showScrollIndicator = false
            }
        }
    }
    val scrollIndicatorAlpha by animateFloatAsState(
        targetValue = if (showScrollIndicator) 1f else 0f,
        animationSpec = tween(180),
        label = "quran_scroll_indicator_alpha"
    )
    val scrollMetrics by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val visible = info.visibleItemsInfo
            if (info.totalItemsCount == 0 || visible.isEmpty()) 0f to 1f else {
                val averageSize = visible.sumOf { it.size }.toFloat() / visible.size
                val contentSize = averageSize * info.totalItemsCount + with(density) { 32.dp.toPx() }
                val viewport = info.viewportSize.height.toFloat()
                val position =
                    ((listState.firstVisibleItemIndex * averageSize + listState.firstVisibleItemScrollOffset) /
                            (contentSize - viewport).coerceAtLeast(1f)).coerceIn(0f, 1f)
                position to (viewport / contentSize).coerceIn(0f, 1f)
            }
        }
    }
    val latestScrollMetrics = rememberUpdatedState(scrollMetrics)
    val currentAyahIndex by remember(listState, ayahs.size) {
        derivedStateOf {
            if (ayahs.isEmpty()) 0
            else (listState.firstVisibleItemIndex - 1).coerceIn(0, ayahs.lastIndex)
        }
    }
    var lastTappedAyahIndex by remember(surahNumber) { mutableStateOf<Int?>(null) }
    var showExitReminder by remember { mutableStateOf(false) }
    var dontShowExitReminderAgain by remember { mutableStateOf(false) }
    val exitScope = rememberCoroutineScope()
    val attemptExit = {
        val exitAyahIndex = lastTappedAyahIndex ?: currentAyahIndex
        val bookmarkKey = "quran_surah_$surahNumber:$exitAyahIndex"
        exitScope.launch {
            val latestSettings = settingsViewModel.getLatestSettings()
            if (latestSettings.quranExitReminderDisabled || latestSettings.bookmarkedAyahs.contains(bookmarkKey)) {
                onBack()
            } else {
                dontShowExitReminderAgain = false
                showExitReminder = true
            }
        }
    }
    BackHandler {
        if (showExitReminder) showExitReminder = false else attemptExit()
    }
    val translationDownloadScope = rememberCoroutineScope()
    val arabicScriptFontFamily = rememberArabicScriptFontFamily(settings.selectedScript, availableQuranScripts)

    val playingAyahIndex by quranAudioViewModel.playingAyahIndex
    val audioState by quranAudioViewModel.audioState
    val currentAyahKey by quranAudioViewModel.currentAyahKey

    // --- Overscroll / Next Surah Logic ---
    val thresholdPx = with(density) { 240.dp.toPx() }
    val overscrollState = remember { mutableFloatStateOf(0f) }
    val fillProgress = (overscrollState.floatValue / thresholdPx).coerceIn(0f, 1f)

    val nextSurah = remember(surahNumber, chapters) {
        if (surahNumber < 114) chapters.find { it.id == surahNumber + 1 } else null
    }

    val nestedScrollConnection = remember(listState, nextSurah) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (nextSurah == null) return androidx.compose.ui.geometry.Offset.Zero
                if (source != androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) return androidx.compose.ui.geometry.Offset.Zero

                // Only detect upward drag when at the very bottom
                if (available.y < 0f && !listState.canScrollForward) {
                    val prev = overscrollState.floatValue
                    overscrollState.floatValue = (prev - available.y).coerceAtMost(thresholdPx)

                    if (overscrollState.floatValue >= thresholdPx && prev < thresholdPx) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    }
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }

                // INSTANT ABORT: If user swipes down while bar is showing, reset to 0
                if (available.y > 0f && overscrollState.floatValue > 0f) {
                    overscrollState.floatValue = 0f
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }

                return androidx.compose.ui.geometry.Offset.Zero
            }

            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput && available.y > 0f && !listState.canScrollForward) {
                    // Safety reset
                    overscrollState.floatValue = 0f
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: androidx.compose.ui.unit.Velocity,
                available: androidx.compose.ui.unit.Velocity
            ): androidx.compose.ui.unit.Velocity {
                if (overscrollState.floatValue >= thresholdPx) {
                    onNextSurah(surahNumber + 1)
                }
                overscrollState.floatValue = 0f
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }
    // --------------------------------------

    LaunchedEffect(media3Controller) {
        quranAudioViewModel.setController(media3Controller)
    }

    val scheme = readingColorScheme(
        settings.readingTheme,
        settings.dynamicColor,
        settings.colorSeed,
        settings.customAccentColor,
    )

    var showSettings by remember { mutableStateOf(false) }
    var showTranslationPicker by remember { mutableStateOf(false) }

    var isTranslationDownloading by remember { mutableStateOf(false) }
    var translationDownloadProgress by remember { mutableFloatStateOf(0f) }
    // "verse_by_verse" or "reading"
    var readingMode by remember { mutableStateOf("verse_by_verse") }
    val bookmarkAddedMessage = stringResource(R.string.quran_bookmark_added)
    val bookmarkRemovedMessage = stringResource(R.string.quran_bookmark_removed)

    LaunchedEffect(surahNumber, settings.selectedTranslationLang) {
        viewModel.loadChapters()
        viewModel.loadSurah(surahNumber, settings.selectedTranslationLang)
        viewModel.loadVerseMeta(context)
    }


    // Load script when selected script changes
    LaunchedEffect(settings.selectedScript) {
        viewModel.loadScript(context, settings.selectedScript)
    }

    LaunchedEffect(surahNumber, initialAyahIndex, ayahs.size) {
        val targetAyah = initialAyahIndex ?: return@LaunchedEffect
        if (ayahs.isEmpty()) return@LaunchedEffect
        listState.scrollToItem(targetAyah.coerceIn(0, ayahs.lastIndex))
    }

    val surah = chapters.find { it.id == surahNumber }

    // Auto-scroll to playing ayah
    LaunchedEffect(playingAyahIndex) {
        playingAyahIndex?.let { index ->
            val offset = if (surahNumber != 1 && surahNumber != 9) 1 else 0
            listState.animateScrollToItem(index + offset)
        }
    }

    // Handle audio errors
    LaunchedEffect(audioState) {
        if (audioState is QuranAudioViewModel.AudioState.Error) {
            Toast.makeText(context, (audioState as QuranAudioViewModel.AudioState.Error).msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Convert ayahs to blocks for BlockRenderer
    val blocks = remember(ayahs, translations, surah) {
        ayahs.map { (num, text) ->
            AyahBlock(
                surah = surahNumber,
                ayah = num,
                display = LocaleHelper.wrap(context).getString(
                    R.string.quran_ayah_reference,
                    surah?.nameSimple ?: surahNumber.toString(),
                    num
                ),
                textUthmani = text,
                translationEn = translations[num],
                verified = true
            )
        }
    }

    MaterialTheme(colorScheme = scheme) {
        val bg = bgColor(settings.readingTheme)
        val fg = contentColor(settings.readingTheme)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
                .background(bg)
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                modifier = modifier
                    .nestedScroll(nestedScrollConnection),
                topBar = {
                    TopAppBar(
                        title = {
                            // Pill-style mode toggle — centred in the bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                ReadingModeToggle(
                                    mode = readingMode,
                                    onModeChange = { readingMode = it },
                                    fg = fg
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { attemptExit() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.common_back),
                                    tint = fg
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { showSettings = true }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.common_settings),
                                    tint = fg.copy(alpha = 0.7f)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = bg.copy(alpha = 0.9f),
                            titleContentColor = fg
                        )
                    )
                }
            ) { paddingValues ->
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = paddingValues.calculateTopPadding()),
                    color = if (settings.readingTheme == "DARK") Color.Black else if (settings.readingTheme == "PAPER") Color(
                        0xFFFBF4E9
                    ) else MaterialTheme.colorScheme.surface,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                ) {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        var activeBlock by remember { mutableStateOf<Pair<ContentBlock, Int>?>(null) }

                        Box(Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(bottom = 32.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        surah?.let {
                                            Text(
                                                text = it.nameArabic,
                                                style = MaterialTheme.typography.displaySmall.copy(
                                                    fontFamily = NotoSansArabic,
                                                    color = fg
                                                ),
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = it.nameSimple,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(top = 8.dp)
                                            )

                                            Row(
                                                modifier = Modifier.padding(top = 12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text(
                                                        text = revelationPlaceLabel(it.revelationPlace).uppercase(),
                                                        modifier = Modifier.padding(
                                                            horizontal = 12.dp,
                                                            vertical = 4.dp
                                                        ),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = PlusJakartaSans),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Text(
                                                    text = pluralStringResource(
                                                        R.plurals.quran_verse_count,
                                                        it.versesCount,
                                                        it.versesCount
                                                    ).uppercase(),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = PlusJakartaSans),
                                                    color = fg.copy(alpha = 0.6f)
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                HorizontalDivider(
                                                    modifier = Modifier.fillMaxWidth(0.5f),
                                                    color = fg.copy(alpha = 0.15f)
                                                )
                                            }
                                        }

                                        // Show Bismillah for all surahs except At-Tawbah (9)
                                        // and NOT for Al-Fatihah (1) since bismillah is its first verse
                                        if (surahNumber != 9 && surahNumber != 1) {
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                text = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ",
                                                textAlign = TextAlign.Center,
                                                fontFamily = arabicScriptFontFamily,
                                                fontSize = 26.sp,
                                                color = fg.copy(alpha = 0.85f),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                itemsIndexed(blocks) { index, block ->
                                    val ayahNum = block.ayah
                                    val meta = viewModel.verseMeta.value["$surahNumber:$ayahNum"]
                                    val prevMeta =
                                        if (ayahNum > 1) viewModel.verseMeta.value["$surahNumber:${ayahNum - 1}"] else null
                                    val showJuzHeader = meta != null && (prevMeta == null || prevMeta.juz != meta.juz)

                                    Column {
                                        if (showJuzHeader && ayahNum > 1) {
                                            JuzHeader(juzNumber = meta!!.juz, contentColor = fg)
                                        }
                                        if (meta?.sajda != null) {
                                            SajdaIndicator(type = meta.sajda, contentColor = fg)
                                        }

                                        val topicId = "quran_surah_$surahNumber"
                                        val bookmarkKey = "$topicId:$index"
                                        val isBookmarked = bookmarkKey in bookmarkedAyahKeys
                                        val translationMap = remember(translations) {
                                            translations.mapKeys { it.key.toString() }
                                        }

                                        BlockRenderer(
                                            block = block,
                                            settings = settings,
                                            fg = fg,
                                            bg = bg,
                                            translationMap = translationMap,
                                            scriptMap = scriptMap,
                                            arabicFontFamily = arabicScriptFontFamily,
                                            isHighlighted = playingAyahIndex == index,
                                            isBookmarked = isBookmarked,
                                            isAyahPlaying = currentAyahKey == "$surahNumber:${block.ayah}" &&
                                                    (audioState == QuranAudioViewModel.AudioState.Loading ||
                                                            audioState == QuranAudioViewModel.AudioState.Playing),
                                            readingMode = readingMode,
                                            source = ContentSource.QF,
                                            onBlockClick = { activeBlock = it to index },
                                            onPlayClick = {
                                                val ayahIsActive = currentAyahKey == "$surahNumber:${block.ayah}" &&
                                                        (audioState == QuranAudioViewModel.AudioState.Loading ||
                                                                audioState == QuranAudioViewModel.AudioState.Playing)
                                                if (ayahIsActive) {
                                                    quranAudioViewModel.stopAyah()
                                                } else {
                                                    quranAudioViewModel.playAyah(
                                                        surahNumber,
                                                        index,
                                                        blocks,
                                                        settings.selectedReciterId,
                                                        sequence = false
                                                    )
                                                }
                                            },
                                            onBookmarkClick = {
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                lastTappedAyahIndex = index
                                                bookmarkedAyahKeys = if (isBookmarked) {
                                                    bookmarkedAyahKeys - bookmarkKey
                                                } else {
                                                    bookmarkedAyahKeys + bookmarkKey
                                                }
                                                settingsViewModel.toggleBookmark(topicId, index)
                                                val msg =
                                                    if (isBookmarked) bookmarkRemovedMessage else bookmarkAddedMessage
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            },

                                        )
                                        val ayahAudioError = audioState as? QuranAudioViewModel.AudioState.Error
                                        if (playingAyahIndex == index && ayahAudioError != null) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    ayahAudioError.msg,
                                                    modifier = Modifier.weight(1f),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = fg.copy(alpha = 0.7f)
                                                )
                                                TextButton(onClick = {
                                                    quranAudioViewModel.playAyah(
                                                        surahNumber,
                                                        index,
                                                        blocks,
                                                        settings.selectedReciterId,
                                                        sequence = false
                                                    )
                                                }) {
                                                    Text(stringResource(R.string.common_retry))
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    val isArmed = fillProgress >= 1f
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 64.dp, top = 24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = stringResource(R.string.quran_end_of_surah),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = fg.copy(alpha = 0.15f)
                                        )

                                        if (nextSurah != null) {
                                            Spacer(Modifier.height(24.dp))

                                            // Circle hidden until swiping
                                            if (overscrollState.floatValue > 0f) {
                                                Box(
                                                    modifier = Modifier.size(36.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator(
                                                        progress = { fillProgress },
                                                        modifier = Modifier.fillMaxSize(),
                                                        color = if (isArmed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(
                                                            alpha = 0.2f
                                                        ),
                                                        strokeWidth = 3.dp,
                                                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                                    )
                                                    if (isArmed) {
                                                        Icon(
                                                            imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowUp,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(20.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                                Spacer(Modifier.height(12.dp))
                                            }

                                            Text(
                                                text = stringResource(if (isArmed) R.string.quran_next_surah_release else R.string.quran_next_surah_swipe),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isArmed) MaterialTheme.colorScheme.primary else fg.copy(
                                                    alpha = 0.3f
                                                )
                                            )

                                            // Only show next surah name after threshold is met
                                            if (isArmed) {
                                                Text(
                                                    text = nextSurah.nameSimple,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier.align(Alignment.CenterEnd).width(24.dp).fillMaxHeight()
                                    .padding(vertical = 8.dp)
                                    .pointerInput(listState) {
                                        detectVerticalDragGestures { change, dragAmount ->
                                            change.consume()
                                            val fraction = latestScrollMetrics.value.second
                                            val viewport = listState.layoutInfo.viewportSize.height.toFloat()
                                            if (fraction < 1f && fraction > 0f && viewport > 0f) {
                                                val thumbHeight = (size.height * fraction).coerceIn(
                                                    24.dp.toPx(),
                                                    size.height.toFloat()
                                                )
                                                val contentRange = (viewport / fraction - viewport).coerceAtLeast(1f)
                                                val trackRange = (size.height - thumbHeight).coerceAtLeast(1f)
                                                scrollIndicatorScope.launch { listState.scrollBy(dragAmount * contentRange / trackRange) }
                                            }
                                        }
                                    }
                            ) {
                                Canvas(
                                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 5.dp)
                                        .fillMaxHeight().width(2.dp)
                                        .graphicsLayer { alpha = scrollIndicatorAlpha }
                                ) {
                                    val (position, fraction) = scrollMetrics
                                    if (fraction < 1f && size.height > 0f) {
                                        val thumbHeight = (size.height * fraction).coerceIn(24.dp.toPx(), size.height)
                                        drawRoundRect(
                                            color = fg.copy(alpha = 0.4f),
                                            topLeft = Offset(0f, (size.height - thumbHeight) * position),
                                            size = Size(size.width, thumbHeight),
                                            cornerRadius = CornerRadius(size.width / 2f)
                                        )
                                    }
                                }
                            }
                        }
                        if (activeBlock != null) {
                            val (block, index) = activeBlock!!
                            val topicId = "quran_surah_$surahNumber"
                            val bookmarkKey = "$topicId:$index"
                            val isBookmarked = bookmarkKey in bookmarkedAyahKeys

                            BlockActionSheet(
                                block = block,
                                topicId = topicId,
                                settings = settings,
                                isBookmarked = isBookmarked,
                                onDismiss = { activeBlock = null },
                                onBookmark = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    lastTappedAyahIndex = index
                                    bookmarkedAyahKeys = if (isBookmarked) {
                                        bookmarkedAyahKeys - bookmarkKey
                                    } else {
                                        bookmarkedAyahKeys + bookmarkKey
                                    }
                                    settingsViewModel.toggleBookmark(topicId, index)
                                    val msg = if (isBookmarked) bookmarkRemovedMessage else bookmarkAddedMessage
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    activeBlock = null
                                },
                                onPlayAyah = {
                                    val ayahKey = (block as? AyahBlock)?.let { "$surahNumber:${it.ayah}" }
                                    val ayahIsActive = ayahKey != null && currentAyahKey == ayahKey &&
                                            (audioState == QuranAudioViewModel.AudioState.Loading ||
                                                    audioState == QuranAudioViewModel.AudioState.Playing)
                                    if (ayahIsActive) {
                                        quranAudioViewModel.stopAyah()
                                    } else {
                                        quranAudioViewModel.playAyah(
                                            surahNumber,
                                            index,
                                            blocks,
                                            settings.selectedReciterId,
                                            sequence = false
                                        )
                                    }
                                    activeBlock = null
                                },
                                onPlayFromHere = {
                                    quranAudioViewModel.playAyah(
                                        surahNumber,
                                        index,
                                        blocks,
                                        settings.selectedReciterId,
                                        sequence = true
                                    )
                                    activeBlock = null
                                }
                            )
                        }
                    }
                }
            }

            if (showSettings) {

                ReadingSettingsSheet(
                    settings = settings,

                    isQuranContext = true,

                    onDismiss = { showSettings = false },
                    onThemeChange = { settingsViewModel.setReadingTheme(it) },
                    onArabicSizeChange = { settingsViewModel.setArabicSizeSp(it) },
                    onTranslationSizeChange = { settingsViewModel.setTranslationSizeSp(it) },
                    onShowTranslationChange = { settingsViewModel.toggleShowTranslation(it) },
                    onKeepScreenOnChange = { settingsViewModel.toggleReadingKeepScreenOn(it) },

                    onTranslationLangChange = { settingsViewModel.setSelectedTranslationLang(it) },

                    onReciterChange = { settingsViewModel.setSelectedReciterId(it) },
                    onScriptChange = { settingsViewModel.setSelectedScript(it) },
                    availableQuranScripts = availableQuranScripts,
                    onOpenTranslationPicker = {
                        showSettings = false
                        showTranslationPicker = true
                    },

                )
            }

            if (showTranslationPicker) {
                TranslationPickerSheet(
                    selectedId = settings.selectedTranslationLang,
                    isDownloading = isTranslationDownloading,
                    progress = translationDownloadProgress,
                    onSelectNone = {
                        settingsViewModel.setSelectedTranslationLang("")
                        showTranslationPicker = false
                    },
                    onSelect = { meta ->
                        if (TranslationRepository.isDownloaded(context, meta.id)) {
                            settingsViewModel.setSelectedTranslationLang(meta.id)
                            showTranslationPicker = false
                        } else if (!isTranslationDownloading) {
                            translationDownloadScope.launch {
                                isTranslationDownloading = true
                                try {
                                    val downloaded =
                                        TranslationRepository.download(context, meta.id, meta.downloadUrl) { progress ->
                                            translationDownloadProgress = progress
                                        }
                                    if (downloaded) {
                                        withContext(Dispatchers.IO) { TranslationRepository.load(context, meta.id) }
                                        settingsViewModel.setSelectedTranslationLang(meta.id)
                                        showTranslationPicker = false
                                    } else {
                                        Toast.makeText(
                                            context,
                                            LocaleHelper.wrap(context)
                                                .getString(R.string.settings_update_download_failed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } finally {
                                    isTranslationDownloading = false
                                    translationDownloadProgress = 0f
                                }
                            }
                        }
                    },
                    onDismiss = { showTranslationPicker = false }
                )
            }

            if (showExitReminder) {
                AlertDialog(
                    onDismissRequest = { showExitReminder = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bookmark, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.quran_exit_reminder_title))
                        }
                    },
                    text = {
                        Column {
                            Text(stringResource(R.string.quran_exit_reminder_message))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Checkbox(
                                    checked = dontShowExitReminderAgain,
                                    onCheckedChange = { dontShowExitReminderAgain = it }
                                )
                                Text(stringResource(R.string.quran_exit_reminder_checkbox))
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (dontShowExitReminderAgain) {
                                settingsViewModel.setQuranExitReminderDisabled(true)
                            }
                            showExitReminder = false
                            onBack()
                        }) {
                            Text(stringResource(R.string.quran_exit_confirmation))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            if (dontShowExitReminderAgain) {
                                settingsViewModel.setQuranExitReminderDisabled(true)
                            }
                            showExitReminder = false
                        }) {
                            Text(stringResource(R.string.quran_exit_stay))
                        }
                    }
                )
            }
        }
    }
}


/**
 * Pill-style segmented toggle matching Quran.com's "Verse by Verse | Reading" switcher.
 */
@Composable
private fun ReadingModeToggle(
    mode: String,
    onModeChange: (String) -> Unit,
    fg: Color,
    modifier: Modifier = Modifier,
) {
    val isVerseByVerse = mode == "verse_by_verse"
    val activeColor = fg
    val inactiveColor = fg.copy(alpha = 0.45f)
    val pillBg = fg.copy(alpha = 0.1f)
    val activeBg = fg.copy(alpha = 0.18f)

    Row(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
            .background(pillBg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Verse by Verse tab
        Box(
            modifier = Modifier
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                .background(if (isVerseByVerse) activeBg else Color.Transparent)
                .clickable { onModeChange("verse_by_verse") }
                .padding(horizontal = 14.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = quranReadingModeLabel("verse_by_verse"),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = PlusJakartaSans),
                color = if (isVerseByVerse) activeColor else inactiveColor,
                fontWeight = if (isVerseByVerse) FontWeight.SemiBold else FontWeight.Normal
            )
        }

        // Reading tab
        Box(
            modifier = Modifier
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                .background(if (!isVerseByVerse) activeBg else Color.Transparent)
                .clickable { onModeChange("reading") }
                .padding(horizontal = 14.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = quranReadingModeLabel("reading"),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = PlusJakartaSans),
                color = if (!isVerseByVerse) activeColor else inactiveColor,
                fontWeight = if (!isVerseByVerse) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
