package com.imn.iman.ui.screens.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imn.iman.R
import com.imn.iman.data.model.SurahMeta
import com.imn.iman.i18n.revelationPlaceLabel
import com.imn.iman.ui.theme.PlusJakartaSansBold
import com.imn.iman.ui.theme.PlusJakartaSans
import com.imn.iman.ui.theme.NotoSansArabic
import com.imn.iman.ui.theme.fontForText
import com.imn.iman.ui.screens.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranSurahListScreen(
    onSurahTap: (Int) -> Unit,
    onBookmarkedAyahTap: (Int, Int) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: QuranViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val chapters by viewModel.chapters
    val settings by settingsViewModel.settings.collectAsState()
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showBookmarks by remember { mutableStateOf(false) }
    val filteredChapters = remember(chapters, searchQuery) {
        chapters.filter {
            it.nameSimple.contains(searchQuery, ignoreCase = true) ||
                    it.nameArabic.contains(searchQuery) ||
                    it.id.toString().contains(searchQuery)
        }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val headerBackgroundColor = MaterialTheme.colorScheme.background
    val headerSurfaceColor = MaterialTheme.colorScheme.surface

    LaunchedEffect(Unit) {
        viewModel.loadChapters()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            val collapseFraction = scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
                            drawRect(lerp(headerBackgroundColor, headerSurfaceColor, collapseFraction))
                        }
                ) {
                    LargeTopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = stringResource(R.string.nav_quran),
                                    fontFamily = PlusJakartaSansBold,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    searchActive = !searchActive
                                    if (!searchActive) searchQuery = ""
                                }
                            ) {
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.quran_search_content_description))
                            }
                            IconButton(onClick = { showBookmarks = true }) {
                                Icon(Icons.Default.Bookmark, contentDescription = stringResource(R.string.nav_bookmarks))
                            }
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.common_settings))
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            }
        ) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding()),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 150.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (searchActive) {
                        item(key = "quran-search") {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = fontForText(searchQuery, PlusJakartaSans)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                placeholder = { Text(stringResource(R.string.quran_search_placeholder)) },
                                singleLine = true
                            )
                        }
                    }
                    items(filteredChapters) { surah ->
                        SurahListItem(
                            surah = surah,
                            onClick = { onSurahTap(surah.id) }
                        )
                    }
                }
            }
        }
        if (showBookmarks) {
            QuranBookmarksSheet(
                bookmarkedAyahs = settings.bookmarkedAyahs,
                bookmarkSavedAt = settings.bookmarkedAyahSavedAt,
                chapters = chapters,
                onDismiss = { showBookmarks = false },
                onBookmarkTap = { surahNumber, ayahIndex ->
                    showBookmarks = false
                    onBookmarkedAyahTap(surahNumber, ayahIndex)
                },
                onBookmarkDelete = { topicId, ayahIndex ->
                    settingsViewModel.removeBookmark(topicId, ayahIndex)
                }
            )
        }
    }
}

private data class QuranBookmark(
    val topicId: String,
    val ayahIndex: Int,
    val surah: SurahMeta,
    val savedAt: Long?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuranBookmarksSheet(
    bookmarkedAyahs: Set<String>,
    bookmarkSavedAt: Map<String, Long>,
    chapters: List<SurahMeta>,
    onDismiss: () -> Unit,
    onBookmarkTap: (Int, Int) -> Unit,
    onBookmarkDelete: (String, Int) -> Unit
) {
    var bookmarkPendingDelete by remember { mutableStateOf<QuranBookmark?>(null) }
    var latestFirst by remember { mutableStateOf(true) }
    val bookmarks = remember(bookmarkedAyahs, bookmarkSavedAt, chapters, latestFirst) {
        val parsed = bookmarkedAyahs.mapNotNull { key ->
            val parts = key.split(":", limit = 2)
            val topicId = parts.getOrNull(0) ?: return@mapNotNull null
            if (!topicId.startsWith("quran_surah_")) return@mapNotNull null
            val surahNumber = topicId.removePrefix("quran_surah_").toIntOrNull()
                ?: return@mapNotNull null
            val ayahIndex = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            val surah = chapters.firstOrNull { it.id == surahNumber }
                ?: return@mapNotNull null
            if (ayahIndex !in 0 until surah.versesCount) return@mapNotNull null
            QuranBookmark(topicId, ayahIndex, surah, bookmarkSavedAt[key])
        }
        if (latestFirst) {
            parsed.sortedByDescending { it.savedAt ?: Long.MIN_VALUE }
        } else {
            parsed.sortedBy { it.savedAt ?: Long.MIN_VALUE }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                stringResource(R.string.nav_bookmarks),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                FilterChip(
                    selected = latestFirst,
                    onClick = { latestFirst = true },
                    label = { Text(stringResource(R.string.quran_bookmarks_sort_latest)) }
                )
                FilterChip(
                    selected = !latestFirst,
                    onClick = { latestFirst = false },
                    label = { Text(stringResource(R.string.quran_bookmarks_sort_oldest)) }
                )
            }
            if (bookmarks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.quran_bookmarks_empty),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(bookmarks) { bookmark ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .clickable {
                                    onBookmarkTap(bookmark.surah.id, bookmark.ayahIndex)
                                }
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { bookmarkPendingDelete = bookmark }
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = stringResource(R.string.quran_bookmark_delete_content_description),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            Icon(
                                Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                stringResource(R.string.quran_bookmark_ayah, bookmark.surah.nameSimple, bookmark.ayahIndex + 1),
                                modifier = Modifier.padding(start = 12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    bookmarkPendingDelete?.let { bookmark ->
        AlertDialog(
            onDismissRequest = { bookmarkPendingDelete = null },
            title = { Text(stringResource(R.string.quran_bookmark_delete_title)) },
            confirmButton = {
                TextButton(onClick = {
                    onBookmarkDelete(bookmark.topicId, bookmark.ayahIndex)
                    bookmarkPendingDelete = null
                }) {
                    Text(stringResource(R.string.common_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { bookmarkPendingDelete = null }) {
                    Text(stringResource(R.string.common_no))
                }
            }
        )
    }
}

@Composable
private fun SurahListItem(
    surah: SurahMeta,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Surah number diamond/badge
        Box(
            modifier = Modifier.size(42.dp),
            contentAlignment = Alignment.Center
        ) {
            // Rotated square (diamond shape like Quran.com)
            Surface(
                modifier = Modifier.size(34.dp),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            ) {}
            Text(
                text = surah.id.toString(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }

        // Surah name info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = surah.nameSimple,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${revelationPlaceLabel(surah.revelationPlace)} · ${pluralStringResource(R.plurals.quran_verse_count, surah.versesCount, surah.versesCount)}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlusJakartaSans),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        // Arabic name right-aligned
        Text(
            text = surah.nameArabic,
            fontFamily = NotoSansArabic,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End
        )
    }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}
