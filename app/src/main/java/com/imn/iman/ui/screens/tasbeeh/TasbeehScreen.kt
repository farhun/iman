package com.imn.iman.ui.screens.tasbeeh



import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.ExperimentalMaterial3Api


import androidx.compose.ui.geometry.Offset


import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.compose.collectAsStateWithLifecycle



import com.imn.iman.data.model.TasbeehCollection

import com.imn.iman.ui.navigation.AppDestinations


import com.imn.iman.ui.theme.fontForText

import com.imn.iman.ui.theme.PlusJakartaSans
import com.imn.iman.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

import com.imn.iman.ui.util.rememberMorphShape
import com.imn.iman.ui.theme.PlusJakartaSansBold


import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TasbeehScreen(
    viewModel: TasbeehViewModel,
    beadColor: Color,
    onCollectionTap: (TasbeehCollection) -> Unit,
    onEditCollection: () -> Unit,
    onSettingsClick: () -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    val collections by viewModel.collections.collectAsStateWithLifecycle(initialValue = emptyList())
    var searchActive by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var selectedCollection by remember { mutableStateOf<TasbeehCollection?>(null) }

    val filtered = remember(collections, query) {
        if (query.isBlank()) collections
        else collections.filter {
            it.title?.contains(query, ignoreCase = true) == true ||
                    it.items.any { item -> item.name.contains(query, ignoreCase = true) }
        }
    }

    val expressiveShape = MaterialShapes.Cookie9Sided.toShape()
    val listProgress by animateFloatAsState(targetValue = 1f, label = "list_morph_progress")
    val listMorphShape = rememberMorphShape(
        start = MaterialShapes.Circle,
        end = MaterialShapes.Cookie9Sided,
        progress = listProgress,
    )
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val headerBackgroundColor = MaterialTheme.colorScheme.background
    val headerSurfaceColor = MaterialTheme.colorScheme.surface

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                            Text(
                                text = stringResource(AppDestinations.TASBEEH.labelRes),
                                fontFamily = PlusJakartaSansBold,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    searchActive = !searchActive
                                    if (!searchActive) query = ""
                                }
                            ) {
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.tasbih_search_description))
                            }
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.common_settings))
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                        ),
                    )
                }
            },
        ) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding()),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 150.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (searchActive) {
                        item(key = "tasbih-search") {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = fontForText(query, PlusJakartaSans)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                placeholder = { Text(stringResource(R.string.tasbih_search_description)) },
                                singleLine = true,
                            )
                        }
                    }
                    items(filtered, key = { "real_${it.id}" }) { collection ->
                        TasbihCollectionListItem(
                            collection = collection,
                            beadColor = beadColor,
                            onClick = { selectedCollection = collection },
                        )
                    }
                }
            }
        }
    }

    // Safely unwrap to prevent NullPointerExceptions on Dismiss/Delete
    selectedCollection?.let { collection ->
        CollectionDetailSheet(
            collection = collection,

            onDismiss = { selectedCollection = null },
            onStart = {
                onCollectionTap(collection)
                selectedCollection = null
            },

            onEdit = {
                viewModel.loadCollectionForEdit(collection)
                selectedCollection = null
                onEditCollection()
            },
            onDelete = {
                viewModel.delete(collection)
                selectedCollection = null
            }
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionDetailSheet(
    collection: TasbeehCollection,

    onDismiss: () -> Unit,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding(),
        ) {
            // Header (Sticky)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 28.dp, top = 30.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.tasbih_edit),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEdit() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.common_close),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                scope.launch {
                                    sheetState.hide()
                                    onDismiss()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 28.dp, end = 28.dp, bottom = 8.dp),
            ) {
                item {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = collection.title?.takeIf { it.isNotBlank() }
                                    ?: stringResource(R.string.tasbih_collection_fallback),
                                style = MaterialTheme.typography.headlineMedium,
                                fontFamily = fontForText(
                                    collection.title?.takeIf { it.isNotBlank() }
                                        ?: stringResource(R.string.tasbih_collection_fallback),
                                    PlusJakartaSans,
                                ),
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = stringResource(R.string.common_delete),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { showDeleteConfirm = true }
                                    .padding(8.dp)
                                    .padding(top = 20.dp, end = 10.dp, bottom = 0.dp)
                            )
                        }
                        Text(
                            text = pluralStringResource(
                                R.plurals.tasbih_dhikr_item_count,
                                collection.items.size,
                                collection.items.size,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.tasbih_dhikr_list),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(collection.items) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = fontForText(item.name, PlusJakartaSans),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "×${item.targetCount}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp),
                            )
                        }
                    }
            }

            // Footer (Sticky)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 24.dp)
            ) {
                if (showDeleteConfirm) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = false },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text(stringResource(R.string.common_cancel), style = MaterialTheme.typography.titleSmall) }
                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text(stringResource(R.string.tasbih_confirm_delete), style = MaterialTheme.typography.titleSmall) }
                    }
                } else {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.tasbih_start),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }


}




@Composable
private fun TasbihCollectionListItem(
    collection: TasbeehCollection,
    beadColor: Color,
    onClick: () -> Unit,
) {
    val collectionTitle = collection.title?.takeIf { it.isNotBlank() }
        ?: collection.items.firstOrNull()?.name
        ?: stringResource(R.string.nav_tasbih)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.size(42.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(22.dp)) {
                val radius = size.minDimension / 2f
                drawTasbihBead(Offset(radius, radius), radius, baseColor = beadColor)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = collectionTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = fontForText(collectionTitle, PlusJakartaSans),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = pluralStringResource(
                    R.plurals.tasbih_dhikr_count,
                    collection.items.size,
                    collection.items.size,
                ),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlusJakartaSans),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 20.dp),
    )
}
