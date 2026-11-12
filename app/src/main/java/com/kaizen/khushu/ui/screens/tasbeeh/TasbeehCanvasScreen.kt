package com.kaizen.khushu.ui.screens.tasbeeh

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AlignHorizontalLeft
import androidx.compose.material.icons.automirrored.filled.AlignHorizontalRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.kaizen.khushu.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaizen.khushu.data.model.TasbeehCanvasPresetDomain
import com.kaizen.khushu.i18n.tasbihCanvasPresetLabel
import com.kaizen.khushu.ui.screens.settings.SettingsViewModel
import com.kaizen.khushu.ui.theme.PlusJakartaSansBold
import com.kaizen.khushu.ui.theme.PlusJakartaSans
import com.kaizen.khushu.ui.theme.NotoSansArabic
import com.kaizen.khushu.ui.theme.fontForText
import com.kaizen.khushu.ui.theme.KhushuColors

@Composable
fun TasbeehCanvasScreen(
    viewModel: TasbeehCanvasViewModel,
    settingsViewModel: SettingsViewModel,
    onExit: () -> Unit,
) {
    BackHandler(onBack = onExit)
    val view = LocalView.current
    val window = (LocalContext.current as Activity).window
    val context = LocalContext.current
    val beadStringExistsMessage = stringResource(R.string.tasbih_bead_string_exists)
    val layoutSavedMessage = stringResource(R.string.tasbih_layout_saved)

    DisposableEffect(Unit) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }

    val workingWidgets by viewModel.workingWidgets.collectAsStateWithLifecycle()
    val workingBackground by viewModel.workingBackgroundColor.collectAsStateWithLifecycle()
    val selectedWidgetId by viewModel.selectedWidgetId.collectAsStateWithLifecycle()
    val isUiVisible by viewModel.isUiVisible.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val customBeadStyle = settings.customBeadStyles.find { it.id == settings.activeBeadStyleId }
    val legacyBeadStyle = if (settings.tasbihBeadStyle == "DARK_ONYX") BeadStyle.DARK_ONYX else BeadStyle.CLASSIC_AMBER

    var showAddMenu by remember { mutableStateOf(false) }
    var showPresetsMenu by remember { mutableStateOf(false) }
    var showBackgroundMenu by remember { mutableStateOf(false) }
    var guidanceToShow by remember { mutableStateOf<TasbeehCanvasViewModel.GuidanceType?>(null) }
    var showColorPickerForWidget by remember { mutableStateOf<TasbihWidget?>(null) }

    // Listen for guidance events
    LaunchedEffect(Unit) {
        viewModel.guidanceEvent.collect { event ->
            if (event == TasbeehCanvasViewModel.GuidanceType.STRING_ALREADY_EXISTS) {
                Toast.makeText(context, beadStringExistsMessage, Toast.LENGTH_SHORT).show()
            } else {
                guidanceToShow = event
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(workingBackground.toLong()))
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    if (selectedWidgetId != null) {
                        viewModel.selectWidget(null)
                        viewModel.showUi()
                    } else {
                        viewModel.toggleUiVisibility()
                    }
                })
            },
        contentAlignment = Alignment.TopStart
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        LaunchedEffect(screenWidth, screenHeight) {
            viewModel.setCanvasSize(screenWidth, screenHeight)
        }

        workingWidgets.sortedBy { it.zIndex }.forEach { widget ->
            TasbeehCanvasWidgetItem(
                widget = widget,
                isSelected = widget.id == selectedWidgetId,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                legacyBeadStyle = legacyBeadStyle,
                customBeadStyle = customBeadStyle,
                onUpdate = { viewModel.updateWidget(it) },
                onTap = {
                    viewModel.selectWidget(widget.id)
                    viewModel.showUi()
                },
                onSizeMeasured = { id, w, h -> viewModel.updateWidgetSize(id, w, h) }
            )
        }

        // Action Buttons
        AnimatedVisibility(
            visible = isUiVisible && selectedWidgetId == null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onExit, colors = ButtonDefaults.textButtonColors(contentColor = if (Color(workingBackground.toLong()) == Color.White) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f))) {
                    Text(stringResource(R.string.tasbih_exit))
                }
                Button(
                    onClick = {
                        viewModel.saveLayout()
                        Toast.makeText(context, layoutSavedMessage, Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.common_save))
                }
            }
        }

        // Bottom Menu
        AnimatedVisibility(
            visible = isUiVisible && selectedWidgetId == null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            val isWhite = Color(workingBackground.toLong()) == Color.White
            Surface(
                color = if (isWhite) Color.Black.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, if (isWhite) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EditorMenuAction(icon = Icons.Default.Add, label = stringResource(R.string.tasbih_add_action), dark = isWhite, onClick = { showAddMenu = true })
                    EditorMenuAction(icon = Icons.Default.Style, label = stringResource(R.string.tasbih_presets_action), dark = isWhite, onClick = { showPresetsMenu = true })
                    EditorMenuAction(icon = Icons.Default.Palette, label = stringResource(R.string.tasbih_background_action), dark = isWhite, onClick = { showBackgroundMenu = true })
                    EditorMenuAction(icon = Icons.Default.RestartAlt, label = stringResource(R.string.common_reset), dark = isWhite, onClick = { viewModel.resetToDefault() })
                }
            }
        }

        if (showAddMenu) {
            AddTasbihWidgetSheet(
                onAdd = { widget ->
                    viewModel.addNewWidgetFromMenu(widget)
                    showAddMenu = false
                },
                onDismiss = { showAddMenu = false }
            )
        }

        if (showPresetsMenu) {
            TasbihPresetsSheet(
                presets = presets,
                onLoad = { viewModel.loadPreset(it) },
                onSave = { viewModel.saveAsPreset(it) },
                onDelete = { viewModel.deletePreset(it) },
                onDismiss = { showPresetsMenu = false }
            )
        }

        if (showBackgroundMenu) {
            TasbihBackgroundSheet(
                currentColor = Color(workingBackground.toLong()),
                onSelect = { viewModel.updateBackgroundColor(it.toArgb()) },
                onDismiss = { showBackgroundMenu = false }
            )
        }

        // Guidance Dialogs
        guidanceToShow?.let { type ->
            AlertDialog(
                onDismissRequest = { guidanceToShow = null },
                confirmButton = { Button(onClick = { guidanceToShow = null }) { Text(stringResource(R.string.common_got_it)) } },
                title = {
                                    Text(
                                        when (type) {
                                            TasbeehCanvasViewModel.GuidanceType.STRING_REMOVED -> stringResource(R.string.tasbih_string_removed_title)
                                            TasbeehCanvasViewModel.GuidanceType.BLIND_MODE_WARNING -> stringResource(R.string.tasbih_trackers_removed_title)
                                            else -> ""
                                        }
                                    )
                                },
                text = {
                    Text(
                        when (type) {
                            TasbeehCanvasViewModel.GuidanceType.STRING_REMOVED ->
                                stringResource(R.string.tasbih_string_removed_message)
                            TasbeehCanvasViewModel.GuidanceType.BLIND_MODE_WARNING ->
                                stringResource(R.string.tasbih_blind_mode_warning)
                            else -> ""
                        }
                    )
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        }

        // Selection / Config
        selectedWidgetId?.let { id ->
            workingWidgets.find { it.id == id }?.let { selectedWidget ->
                TasbihWidgetConfigSheet(
                    widget = selectedWidget,
                    onUpdate = { viewModel.updateWidget(it) },
                    onDelete = { viewModel.removeWidget(id) },
                    onDismiss = { viewModel.selectWidget(null) },
                    onAlign = { h, v -> viewModel.alignSelectedWidget(h, v) },
                    onPickColor = { showColorPickerForWidget = selectedWidget }
                )
            }
        }

        showColorPickerForWidget?.let { widget ->
            val currentColorLong = widget.color
            SimpleColorPickerSheet(
                initialColor = Color(currentColorLong),
                onColorSelected = { color ->
                    val updated = when(widget) {
                        is TasbihWidget.DhikrNameWidget -> widget.copy(color = color.toArgb().toLong())
                        is TasbihWidget.CounterWidget -> widget.copy(color = color.toArgb().toLong())
                        is TasbihWidget.MeaningWidget -> widget.copy(color = color.toArgb().toLong())
                        is TasbihWidget.CustomText -> widget.copy(color = color.toArgb().toLong())
                        is TasbihWidget.ProgressCircleWidget -> widget.copy(color = color.toArgb().toLong())
                        is TasbihWidget.StringBeadWidget -> widget.copy(color = color.toArgb().toLong())
                    }
                    viewModel.updateWidget(updated)
                },
                onDismiss = { showColorPickerForWidget = null }
            )
        }
    }
}

@Composable
private fun EditorMenuAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, dark: Boolean = false, onClick: () -> Unit) {
    val contentColor = if (dark) Color.Black else Color.White
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(icon, null, tint = contentColor, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f))
    }
}

@Composable
fun TasbeehCanvasWidgetItem(
    widget: TasbihWidget,
    isSelected: Boolean,
    screenWidth: Float,
    screenHeight: Float,
    legacyBeadStyle: BeadStyle,
    customBeadStyle: com.kaizen.khushu.data.model.CustomBeadStyle?,
    onUpdate: (TasbihWidget) -> Unit,
    onTap: () -> Unit,
    onSizeMeasured: (String, Float, Float) -> Unit,
) {
    val currentWidget by rememberUpdatedState(widget)
    val currentOnUpdate by rememberUpdatedState(onUpdate)
    val currentOnSizeMeasured by rememberUpdatedState(onSizeMeasured)

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationX = (widget.offsetX * screenWidth) - (size.width / 2f)
                translationY = (widget.offsetY * screenHeight) - (size.height / 2f)
                scaleX = widget.scale
                scaleY = widget.scale
                alpha = widget.alpha
                transformOrigin = TransformOrigin.Center
            }
            .then(
                if (isSelected) Modifier.border(1.dp, if (widget is TasbihWidget.CustomText && Color(widget.color) == Color.White) Color.Black.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                else Modifier
            )
            .onGloballyPositioned { coordinates ->
                currentOnSizeMeasured(
                    widget.id,
                    coordinates.size.width.toFloat(),
                    coordinates.size.height.toFloat()
                )
            }
            .pointerInput(widget.id) {
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(widget.id) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val w = currentWidget
                    val newOffsetX = (w.offsetX + (pan.x / screenWidth)).coerceIn(0f, 1f)
                    val newOffsetY = if (w is TasbihWidget.StringBeadWidget) 0.5f
                                    else (w.offsetY + (pan.y / screenHeight)).coerceIn(0f, 1f)
                    val newScale = (w.scale * zoom).coerceIn(0.2f, 5f)

                    val updated = when (w) {
                        is TasbihWidget.StringBeadWidget -> w.copy(offsetX = newOffsetX, offsetY = newOffsetY, scale = newScale)
                        is TasbihWidget.DhikrNameWidget -> w.copy(offsetX = newOffsetX, offsetY = newOffsetY, scale = newScale)
                        is TasbihWidget.CounterWidget -> w.copy(offsetX = newOffsetX, offsetY = newOffsetY, scale = newScale)
                        is TasbihWidget.ProgressCircleWidget -> w.copy(offsetX = newOffsetX, offsetY = newOffsetY, scale = newScale)
                        is TasbihWidget.MeaningWidget -> w.copy(offsetX = newOffsetX, offsetY = newOffsetY, scale = newScale)
                        is TasbihWidget.CustomText -> w.copy(offsetX = newOffsetX, offsetY = newOffsetY, scale = newScale)
                    }
                    currentOnUpdate(updated)
                }
            }
    ) {
        val renderHeight = if (widget is TasbihWidget.StringBeadWidget) screenHeight else 0f

        Box(modifier = if (renderHeight > 0) Modifier.height(with(androidx.compose.ui.platform.LocalDensity.current) { renderHeight.toDp() }) else Modifier) {
            TasbihWidgetRenderer(
                widget = widget,
                currentCount = 33,
                currentItem = null,
                stringControlXOffset = 0f,
                stringControlYFraction = 0.5f,
                countedBeads = 12,
                totalBeads = 33,
                beadStyle = legacyBeadStyle,
                customBeadStyle = customBeadStyle,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTasbihWidgetSheet(
    onAdd: (TasbihWidget) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.tasbih_add_widget_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WidgetPreviewCard(
                    title = stringResource(R.string.settings_counter),
                    modifier = Modifier.weight(1f),
                    onClick = { onAdd(TasbihWidget.CounterWidget(id = "counter_${System.currentTimeMillis()}")) }
                ) {
                    Text("33", fontSize = 40.sp, color = Color.White, fontFamily = PlusJakartaSansBold)
                }
                WidgetPreviewCard(
                    title = stringResource(R.string.tasbih_dhikr_name),
                    modifier = Modifier.weight(1f),
                    onClick = { onAdd(TasbihWidget.DhikrNameWidget(id = "name_${System.currentTimeMillis()}")) }
                ) {
                    Text("سبحان الله", fontSize = 18.sp, color = Color.White, fontFamily = NotoSansArabic)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WidgetPreviewCard(
                    title = stringResource(R.string.tasbih_bead_string),
                    modifier = Modifier.weight(1f),
                    onClick = { onAdd(TasbihWidget.StringBeadWidget(id = "string_${System.currentTimeMillis()}")) }
                ) {
                    Box(Modifier.width(2.dp).fillMaxHeight(0.6f).background(Color.White.copy(alpha = 0.3f)))
                }
                WidgetPreviewCard(
                    title = stringResource(R.string.tasbih_custom_text),
                    modifier = Modifier.weight(1f),
                    onClick = { onAdd(TasbihWidget.CustomText(id = "text_${System.currentTimeMillis()}")) }
                ) {
                    Icon(Icons.Default.TextFields, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasbihPresetsSheet(
    presets: List<TasbeehCanvasPresetDomain>,
    onLoad: (TasbeehCanvasPresetDomain) -> Unit,
    onSave: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.tasbih_layout_presets),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { showSaveDialog = true }, shape = RoundedCornerShape(8.dp)) {
                    Text(stringResource(R.string.tasbih_save_new))
                }
            }
            Spacer(Modifier.height(24.dp))

            if (presets.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.tasbih_no_custom_presets), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(presets) { preset ->
                        val presetName = tasbihCanvasPresetLabel(preset.id, preset.name)
                        OutlinedCard(
                            onClick = { onLoad(preset); onDismiss() },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    presetName,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = fontForText(presetName, PlusJakartaSans)
                                    ),
                                    maxLines = 1,
                                )
                                Spacer(Modifier.height(8.dp))
                                Row {
                                    Text(
                                        pluralStringResource(R.plurals.tasbih_widget_count, preset.widgets.size, preset.widgets.size),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Icon(
                                        Icons.Default.Delete,
                                        null,
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp).clickable { onDelete(preset.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.tasbih_save_preset)) },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = fontForText(presetName, PlusJakartaSans)
                    ),
                    label = { Text(stringResource(R.string.tasbih_preset_name)) }
                )
            },
            confirmButton = {
                Button(onClick = { onSave(presetName); showSaveDialog = false }) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasbihBackgroundSheet(
    currentColor: Color,
    onSelect: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = listOf(Color.Black, Color.White, Color(0xFF1A1A1A), Color(0xFF0D1B2A), Color(0xFF1B4332), Color(0xFF432818))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 16.dp)) {
            Text(stringResource(R.string.tasbih_canvas_background), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(2.dp, if (currentColor == color) (if (color == Color.White) Color.Black else Color.White) else Color.Transparent, CircleShape)
                            .clickable { onSelect(color) }
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetPreviewCard(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
    previewContent: @Composable BoxScope.() -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
                content = previewContent
            )
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(text = title, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasbihWidgetConfigSheet(
    widget: TasbihWidget,
    onUpdate: (TasbihWidget) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onAlign: (Alignment.Horizontal?, Alignment.Vertical?) -> Unit,
    onPickColor: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.tasbih_configure_widget),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete, colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Delete, null)
                }
            }
            Spacer(Modifier.height(24.dp))

            // 1. Content (for CustomText)
            if (widget is TasbihWidget.CustomText) {
                OutlinedTextField(
                    value = widget.text,
                    onValueChange = { onUpdate(widget.copy(text = it)) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = fontForText(widget.text, PlusJakartaSans)
                    ),
                    label = { Text(stringResource(R.string.tasbih_text_content)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
            }

            // 2. Color Palette (Presets & Custom)
            Text(stringResource(R.string.tasbih_colors), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val palette = KhushuColors.Palette.take(5)
                palette.forEach { color ->
                    val isSelected = widget.color == color.toArgb().toLong()
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f), CircleShape)
                            .clickable { onUpdate(when(widget) {
                                is TasbihWidget.StringBeadWidget -> widget.copy(color = color.toArgb().toLong())
                                is TasbihWidget.DhikrNameWidget -> widget.copy(color = color.toArgb().toLong())
                                is TasbihWidget.CounterWidget -> widget.copy(color = color.toArgb().toLong())
                                is TasbihWidget.ProgressCircleWidget -> widget.copy(color = color.toArgb().toLong())
                                is TasbihWidget.MeaningWidget -> widget.copy(color = color.toArgb().toLong())
                                is TasbihWidget.CustomText -> widget.copy(color = color.toArgb().toLong())
                            }) }
                    )
                }
                // Custom Color Wheel
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .clickable { onPickColor() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Palette, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(24.dp))

            // 3. Style & Format
            Text(stringResource(R.string.tasbih_style_format), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (widget is TasbihWidget.CounterWidget || widget is TasbihWidget.DhikrNameWidget || widget is TasbihWidget.CustomText) {
                    val isBold = when(widget) {
                        is TasbihWidget.CounterWidget -> widget.isBold
                        is TasbihWidget.DhikrNameWidget -> widget.isBold
                        is TasbihWidget.CustomText -> widget.isBold
                        else -> false
                    }
                    val hasOutline = when(widget) {
                        is TasbihWidget.CounterWidget -> widget.hasOutline
                        is TasbihWidget.DhikrNameWidget -> widget.hasOutline
                        is TasbihWidget.CustomText -> widget.hasOutline
                        else -> false
                    }
                    FilterChip(
                        selected = isBold,
                        onClick = {
                            onUpdate(when(widget) {
                                is TasbihWidget.CounterWidget -> widget.copy(isBold = !isBold)
                                is TasbihWidget.DhikrNameWidget -> widget.copy(isBold = !isBold)
                                is TasbihWidget.CustomText -> widget.copy(isBold = !isBold)
                                else -> widget
                            })
                        },
                        label = { Text(stringResource(R.string.tasbih_bold)) }
                    )
                    FilterChip(
                        selected = hasOutline,
                        onClick = {
                            onUpdate(when(widget) {
                                is TasbihWidget.CounterWidget -> widget.copy(hasOutline = !hasOutline)
                                is TasbihWidget.DhikrNameWidget -> widget.copy(hasOutline = !hasOutline)
                                is TasbihWidget.CustomText -> widget.copy(hasOutline = !hasOutline)
                                else -> widget
                            })
                        },
                        label = { Text(stringResource(R.string.tasbih_outline)) }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            // 4. Sliders (Size, Scale, Opacity)
            if (widget is TasbihWidget.CustomText) {
                SliderControl(stringResource(R.string.tasbih_font_size), widget.fontSize, 12f, 120f) { onUpdate(widget.copy(fontSize = it)) }
                Spacer(Modifier.height(16.dp))
            }
            SliderControl(stringResource(R.string.tasbih_opacity, (widget.alpha * 100).toInt()), widget.alpha, 0.1f, 1.0f) { onUpdate(
                when(widget) {
                    is TasbihWidget.StringBeadWidget -> widget.copy(alpha = it)
                    is TasbihWidget.DhikrNameWidget -> widget.copy(alpha = it)
                    is TasbihWidget.CounterWidget -> widget.copy(alpha = it)
                    is TasbihWidget.ProgressCircleWidget -> widget.copy(alpha = it)
                    is TasbihWidget.MeaningWidget -> widget.copy(alpha = it)
                    is TasbihWidget.CustomText -> widget.copy(alpha = it)
                }
            )}
            Spacer(Modifier.height(16.dp))
            SliderControl(stringResource(R.string.tasbih_global_scale), widget.scale, 0.5f, 3f) { onUpdate(
                when(widget) {
                    is TasbihWidget.StringBeadWidget -> widget.copy(scale = it)
                    is TasbihWidget.DhikrNameWidget -> widget.copy(scale = it)
                    is TasbihWidget.CounterWidget -> widget.copy(scale = it)
                    is TasbihWidget.ProgressCircleWidget -> widget.copy(scale = it)
                    is TasbihWidget.MeaningWidget -> widget.copy(scale = it)
                    is TasbihWidget.CustomText -> widget.copy(scale = it)
                }
            )}
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(24.dp))

            // 5. String Specific
            if (widget is TasbihWidget.StringBeadWidget) {
                Text(stringResource(R.string.tasbih_string_physics), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                SliderControl(stringResource(R.string.tasbih_bead_size), widget.beadSizeScale, 0.5f, 2.5f) { onUpdate(widget.copy(beadSizeScale = it)) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SliderControl(stringResource(R.string.tasbih_top_stack), widget.topStackLimit.toFloat(), 0f, 10f, Modifier.weight(1f)) { onUpdate(widget.copy(topStackLimit = it.toInt())) }
                    SliderControl(stringResource(R.string.tasbih_bottom_pool), widget.bottomStackLimit.toFloat(), 0f, 15f, Modifier.weight(1f)) { onUpdate(widget.copy(bottomStackLimit = it.toInt())) }
                }
                Spacer(Modifier.height(16.dp))
                SliderControl(stringResource(R.string.tasbih_elasticity), widget.stringElasticity, 1.0f, 3.0f) { onUpdate(widget.copy(stringElasticity = it)) }
                SliderControl(stringResource(R.string.tasbih_stiffness), widget.wobbleStiffness, 50f, 500f) { onUpdate(widget.copy(wobbleStiffness = it)) }
                SliderControl(stringResource(R.string.tasbih_damping), widget.wobbleDampingRatio, 0.1f, 1.0f) { onUpdate(widget.copy(wobbleDampingRatio = it)) }
                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(24.dp))
            }

            // 6. Alignment
            Text(stringResource(R.string.tasbih_alignment), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Alignment.Start to Icons.AutoMirrored.Filled.AlignHorizontalLeft,
                    Alignment.CenterHorizontally to Icons.Default.AlignHorizontalCenter,
                    Alignment.End to Icons.AutoMirrored.Filled.AlignHorizontalRight
                ).forEach { (align, icon) ->
                    FilledTonalButton(onClick = { onAlign(align, null) }, modifier = Modifier.weight(1f)) {
                        Icon(icon, null)
                    }
                }
            }
            if (widget !is TasbihWidget.StringBeadWidget) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Alignment.Top to Icons.Default.VerticalAlignTop,
                        Alignment.CenterVertically to Icons.Default.VerticalAlignCenter,
                        Alignment.Bottom to Icons.Default.VerticalAlignBottom
                    ).forEach { (align, icon) ->
                        FilledTonalButton(onClick = { onAlign(null, align) }, modifier = Modifier.weight(1f)) {
                            Icon(icon, null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderControl(label: String, value: Float, min: Float, max: Float, modifier: Modifier = Modifier, onUpdate: (Float) -> Unit) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (value % 1 == 0f) value.toInt().toString() else String.format("%.2f", value), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onUpdate, valueRange = min..max)
    }
}
