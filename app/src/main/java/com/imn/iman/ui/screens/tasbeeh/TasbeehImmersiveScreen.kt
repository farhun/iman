package com.imn.iman.ui.screens.tasbeeh

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues

import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.imn.iman.R
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

import com.imn.iman.data.model.TasbeehCollection
import com.imn.iman.data.repository.UserSettings
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private val PRESS_BEAD_SIZE = 72.dp
private val PRESS_BEAD_BOTTOM_PADDING = 120.dp

@Composable
fun TasbeehImmersiveScreen(
    viewModel: TasbeehViewModel,
    collection: TasbeehCollection,
    settings: UserSettings,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val window = (context as? Activity)?.window
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current.density
    val focusRequester = remember { FocusRequester() }
    val isPressMode = settings.tasbihBeadMode == "press"
    val widgets = if (isPressMode) ClassicTasbihWidgets else DefaultTasbihWidgets
    val stringWidget = widgets.filterIsInstance<TasbihWidget.StringBeadWidget>().firstOrNull()
    val backgroundColor = Color(android.graphics.Color.parseColor(settings.tasbihBackgroundColor))
    val beadColor = Color(android.graphics.Color.parseColor(settings.tasbihBeadColor))
    val textColor = Color(android.graphics.Color.parseColor(settings.tasbihTextColor))
    val pressBeadDescription = stringResource(R.string.tasbih_press_bead_description)
    val soundPlayer = remember { TasbihSoundPlayer(context) }

    DisposableEffect(Unit) {
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            soundPlayer.release()
        }
    }

    var currentItemIndex by remember { mutableIntStateOf(0) }
    var currentCount by remember { mutableIntStateOf(0) }
    val items = collection.items
    val currentItem = items.getOrNull(currentItemIndex)
    val currentTarget = currentItem?.targetCount ?: 33
    val totalRequiredCount = items.sumOf { it.targetCount.coerceAtLeast(0) }
    var sessionCompleted by remember { mutableStateOf(false) }
    var hasExited by remember { mutableStateOf(false) }

    fun exitToList() {
        if (!hasExited) {
            hasExited = true
            onExit()
        }
    }

    BackHandler(onBack = { exitToList() })

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
        WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val navigationBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomContentPadding = if (isPressMode) {
        maxOf(PRESS_BEAD_BOTTOM_PADDING + PRESS_BEAD_SIZE, 96.dp + navigationBottomInset)
    } else {
        96.dp + navigationBottomInset
    }

    var screenWidth by remember { mutableFloatStateOf(0f) }
    var screenHeight by remember { mutableFloatStateOf(0f) }
    val slideDhikrTextWidth = stringWidget?.takeIf { !isPressMode && screenWidth > 0f }?.let { beadWidget ->
        val lineCenterDp = beadWidget.offsetX * screenWidth / density
        val maximumBeadRadiusDp = BEAD_RADIUS_BASE * beadWidget.beadSizeScale * beadWidget.beadMicroScale
        (lineCenterDp - 24f - maximumBeadRadiusDp - 16f).dp.coerceAtLeast(0.dp)
    }

    val scope = rememberCoroutineScope()
    val pressBeadScale = remember { Animatable(1f) }
    var pressBeadPressed by remember { mutableStateOf(false) }

    // Finger Y on the string canvas — drives the live fisheye wave.
    // Written directly from pointer events; read at Canvas draw-phase via the provider lambda.
    var fingerYOnString by remember { mutableStateOf<Float?>(null) }
    // fisheyeAlpha fades 0→1 on touch-down and 1→0 on lift — smooth fisheye release.
    val fisheyeAlpha = remember { Animatable(0f) }
    // Transit bead state — tracks a bead being dragged across the gap between stacks.
    val transitYAnim      = remember { Animatable(0f) }
    val transitLiftAnim   = remember { Animatable(0f) }
    var isInTransit       by remember { mutableStateOf(false) }
    var transitFromBottom by remember { mutableStateOf(true) }
    // Conveyor-belt scroll: snapped to ±beadStep then animated to 0 on each successful transit.
    val scrollOffsetAnim  = remember { Animatable(0f) }
    var widgetsVisible by remember { mutableStateOf(true) }
    var resetProgress by remember { mutableFloatStateOf(0f) }
    var resetArmed by remember { mutableStateOf(false) }
    var showResetOverlay by remember { mutableStateOf(false) }

    fun playTasbihCollisionSound() {
        if (settings.tasbihSoundEnabled) {
            soundPlayer.play(settings.tasbihSoundId)
        }
    }

    fun registerIncrement(playSound: Boolean = true) {
        if (sessionCompleted || hasExited) return
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        currentCount++
        if (playSound) {
            playTasbihCollisionSound()
        }
        if (currentCount >= currentTarget) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (currentItemIndex < items.lastIndex) {
                currentItemIndex++
                currentCount = 0
            } else {
                val completedCount = items.take(currentItemIndex)
                    .sumOf { it.targetCount.coerceAtLeast(0) } + currentCount
                if (items.isNotEmpty() && completedCount >= totalRequiredCount) {
                    sessionCompleted = true
                }
            }
        }
    }

    fun registerDecrement() {
        if (sessionCompleted || hasExited) return
        if (currentCount > 0 || currentItemIndex > 0) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            if (currentCount > 0) currentCount--
            else { currentItemIndex--; currentCount = items[currentItemIndex].targetCount - 1 }
        }
    }

    suspend fun setPressBeadPressed(pressed: Boolean) {
        pressBeadPressed = pressed
        pressBeadScale.animateTo(
            targetValue = if (pressed) 0.92f else 1f,
            animationSpec = spring(stiffness = 520f, dampingRatio = 0.62f),
        )
    }

    suspend fun animatePressBead() {
        setPressBeadPressed(true)
        delay(90)
        setPressBeadPressed(false)
    }

    suspend fun animateSlideTransit(
        fromBottom: Boolean,
        destinationY: Float,
        beadRadius: Float,
    ) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        coroutineScope {
            val moveJob = launch {
                if (fromBottom) {
                    val impactY = destinationY - (beadRadius * 0.22f)
                    transitYAnim.animateTo(
                        impactY,
                        tween(durationMillis = 135, easing = FastOutSlowInEasing),
                    )
                    playTasbihCollisionSound()
                    transitYAnim.animateTo(destinationY, tween(durationMillis = 70))
                } else {
                    transitYAnim.animateTo(
                        destinationY,
                        spring(stiffness = 500f, dampingRatio = 0.85f),
                    )
                }
            }
            val liftJob = launch { transitLiftAnim.animateTo(0f, tween(180)) }
            moveJob.join()
            liftJob.join()
        }

        val beadStep = beadRadius * 2.4f
        scrollOffsetAnim.snapTo(if (fromBottom) beadStep else -beadStep)
        if (fromBottom) registerIncrement(playSound = false) else registerDecrement()
        isInTransit = false
        scrollOffsetAnim.animateTo(0f, spring(stiffness = 220f, dampingRatio = 0.88f))
    }

    suspend fun animateSlideStep(fromBottom: Boolean): Boolean {
        val sw = stringWidget ?: return false
        if (screenWidth <= 0f || screenHeight <= 0f || isInTransit) return false

        val beadRadius = BEAD_RADIUS_BASE * sw.beadSizeScale * density
        val beadGap = beadRadius * 0.4f
        val canvasHeight = screenHeight * 0.9f
        val canvasTop = sw.offsetY * screenHeight - canvasHeight / 2f
        val topCount = minOf(currentCount, sw.topStackLimit).coerceAtLeast(0)
        val bottomCount = minOf(currentTarget - currentCount, sw.bottomStackLimit).coerceAtLeast(0)
        if ((fromBottom && bottomCount == 0) || (!fromBottom && topCount == 0)) return false

        val topBoundaryY = canvasTop + beadRadius +
            (topCount - 1).coerceAtLeast(0) * (2f * beadRadius + beadGap)
        val bottomBoundaryY = canvasTop + canvasHeight - beadRadius -
            (bottomCount - 1).coerceAtLeast(0) * (2f * beadRadius + beadGap)
        val destinationY = if (fromBottom) {
            topBoundaryY + 2f * beadRadius + beadGap
        } else {
            bottomBoundaryY - 2f * beadRadius - beadGap
        }

        isInTransit = true
        transitFromBottom = fromBottom
        transitLiftAnim.snapTo(1f)
        transitYAnim.snapTo(if (fromBottom) bottomBoundaryY else topBoundaryY)
        animateSlideTransit(fromBottom, destinationY, beadRadius)
        return true
    }

    suspend fun handleVolumeCount(increment: Boolean, animate: Boolean) {
        if (sessionCompleted || hasExited) return
        if (!animate) {
            if (increment) registerIncrement() else registerDecrement()
        } else if (isPressMode) {
            animatePressBead()
            if (increment) registerIncrement() else registerDecrement()
        } else if (!animateSlideStep(increment)) {
            if (increment) registerIncrement() else registerDecrement()
        }
    }

    LaunchedEffect(viewModel.countIncrementSignal, isPressMode) {
        viewModel.countIncrementSignal.collect { animate ->
            handleVolumeCount(increment = true, animate = animate)
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (settings.tasbeehVolumeEnabled && event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.VolumeUp -> {
                            scope.launch { handleVolumeCount(true, settings.tasbeehVolumeAnimation) }
                            true
                        }
                        Key.VolumeDown -> {
                            scope.launch { handleVolumeCount(false, settings.tasbeehVolumeAnimation) }
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .onSizeChanged { size ->
                screenWidth = size.width.toFloat()
                screenHeight = size.height.toFloat()
            }
    ) {
        if (screenWidth > 0f && screenHeight > 0f) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                AnimatedVisibility(visible = widgetsVisible, enter = fadeIn(), exit = fadeOut()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        widgets.sortedBy { it.zIndex }.forEach { widget ->
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    translationX = (widget.offsetX * screenWidth) - (size.width / 2f)
                                    translationY = (widget.offsetY * screenHeight) - (size.height / 2f)
                                    scaleX = widget.scale
                                    scaleY = widget.scale
                                    transformOrigin = TransformOrigin.Center
                                    clip = false
                                }
                            ) {
                                TasbihWidgetRenderer(
                                    widget = widget,
                                    countedBeads = currentCount,
                                    totalBeads = currentTarget,
                                    beadColor = beadColor,
                                    textColor = textColor,
                                    thumbPositionProvider = if (widget is TasbihWidget.StringBeadWidget) {
                                        val offsetY = widget.offsetY
                                        { fingerYOnString?.let { fy -> Offset(0f, fy - offsetY * screenHeight) } }
                                    } else { { null } },
                                    fisheyeStrengthProvider = if (widget is TasbihWidget.StringBeadWidget) {
                                        { fisheyeAlpha.value }
                                    } else { { 1f } },
                                    transitBeadProvider = if (widget is TasbihWidget.StringBeadWidget) {
                                        val offsetY  = widget.offsetY
                                        val canvasH  = screenHeight * 0.9f
                                        { if (isInTransit) transitYAnim.value - offsetY * screenHeight + canvasH / 2f else null }
                                    } else { { null } },
                                    transitLiftProvider = if (widget is TasbihWidget.StringBeadWidget) {
                                        { transitLiftAnim.value }
                                    } else { { 0f } },
                                    transitFromBottom = transitFromBottom,
                                    scrollOffsetProvider = if (widget is TasbihWidget.StringBeadWidget) {
                                        { scrollOffsetAnim.value }
                                    } else { { 0f } },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (screenWidth > 0f && screenHeight > 0f) {
            val hasString = stringWidget != null

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(screenWidth, screenHeight, widgets, widgetsVisible, settings.tasbeehStealthModeAllowed) {
                        awaitEachGesture {
                            val down = awaitFirstDown()

                            val startScreenX = down.position.x
                            val startScreenY = down.position.y
                            val stringScreenX = (stringWidget?.offsetX ?: 0.88f) * screenWidth
                            val pressBeadDiameter = PRESS_BEAD_SIZE.toPx()
                            val pressBeadCenterY = screenHeight - PRESS_BEAD_BOTTOM_PADDING.toPx() - pressBeadDiameter / 2f
                            val isOnPressBead = isPressMode &&
                                kotlin.math.abs(startScreenX - screenWidth / 2f) <= pressBeadDiameter / 2f &&
                                kotlin.math.abs(startScreenY - pressBeadCenterY) <= pressBeadDiameter / 2f
                            val isNearString = hasString && widgetsVisible &&
                                kotlin.math.abs(startScreenX - stringScreenX) < 70f * density && !showResetOverlay

                            var localResetArmed = false
                            var hasMoved = false

                            // Hold-to-reset: only fires when not near the string.
                            val holdJob = if (!isNearString && !isOnPressBead) {
                                scope.launch {
                                    kotlinx.coroutines.delay(500)
                                    showResetOverlay = true
                                    resetProgress = 0f; resetArmed = false
                                    for (i in 1..20) { kotlinx.coroutines.delay(40); resetProgress = i / 20f }
                                    resetArmed = true; localResetArmed = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            } else null

                            // Snap fisheye strength to full on touch-down near string.
                            if (isNearString) scope.launch { fisheyeAlpha.snapTo(1f) }

                            // ── Boundary-bead positions in screen space ──────────────
                            // These are natural (no-zoom) positions; close enough for trigger detection.
                            val sw = stringWidget
                            val beadR    = BEAD_RADIUS_BASE * (sw?.beadSizeScale ?: 1f) * density
                            val beadGap  = beadR * 0.4f
                            val canvasH  = screenHeight * 0.9f
                            val canvasTopScreen = (sw?.offsetY ?: 0.5f) * screenHeight - canvasH / 2f
                            val topCount    = minOf(currentCount,          sw?.topStackLimit    ?: 0).coerceAtLeast(0)
                            val bottomCount = minOf(currentTarget - currentCount, sw?.bottomStackLimit ?: 0).coerceAtLeast(0)

                            // Bottom-most bead of the TOP stack (screen Y)
                            val topBoundaryY = canvasTopScreen + beadR +
                                (topCount - 1).coerceAtLeast(0) * (2f * beadR + beadGap)
                            // Top-most bead of the BOTTOM stack (screen Y)
                            val bottomBoundaryY = canvasTopScreen + canvasH - beadR -
                                (bottomCount - 1).coerceAtLeast(0) * (2f * beadR + beadGap)
                            val gapDistance = (bottomBoundaryY - topBoundaryY).coerceAtLeast(0f)
                            // Commit once the bead has travelled a reasonable portion of the gap
                            // from its source side, so upward and downward swipes feel balanced.
                            val commitDistance = gapDistance * 0.25f
                            val triggerZone = beadR * 2.5f  // how close the finger must start
                            val triggerMove = beadR * 1.5f  // how far it must move to begin transit

                            val isNearTopBoundary    = topCount    > 0 && kotlin.math.abs(startScreenY - topBoundaryY)    < triggerZone
                            val isNearBottomBoundary = bottomCount > 0 && kotlin.math.abs(startScreenY - bottomBoundaryY) < triggerZone

                            // Track finger Y and handle transit activation.
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) break
                                val dy = change.position.y - startScreenY
                                if (isNearString) fingerYOnString = change.position.y
                                if (kotlin.math.abs(dy) > 20f) hasMoved = true

                                // Check proximity to boundary bead using CURRENT finger position
                                // (not startScreenY) so swiping through the stack naturally triggers transit.
                                if (isNearString && !isInTransit) {
                                    val curY = change.position.y
                                    val currentlyNearBottom = bottomCount > 0 && kotlin.math.abs(curY - bottomBoundaryY) < triggerZone
                                    val currentlyNearTop    = topCount    > 0 && kotlin.math.abs(curY - topBoundaryY)    < triggerZone
                                    val canGoUp   = dy < -triggerMove && currentlyNearBottom
                                    val canGoDown = dy >  triggerMove && currentlyNearTop
                                    if (canGoUp || canGoDown) {
                                        isInTransit    = true
                                        transitFromBottom = canGoUp
                                        scope.launch {
                                            transitLiftAnim.snapTo(1f)
                                            transitYAnim.snapTo(change.position.y)
                                        }
                                    }
                                }
                                if (isInTransit) scope.launch { transitYAnim.snapTo(change.position.y) }
                                if (isNearString || isOnPressBead) change.consume()
                            }

                            holdJob?.cancel()

                            // Fisheye fade-out: keep fingerYOnString set while strength fades to 0,
                            // then null it. Beads return to natural size smoothly.
                            if (isNearString) {
                                scope.launch {
                                    fisheyeAlpha.animateTo(0f, tween(200))
                                    fingerYOnString = null
                                }
                            } else {
                                fingerYOnString = null
                            }

                            // ── Transit resolution ───────────────────────────────────
                            if (isInTransit) {
                                val currentY = transitYAnim.value
                                val committed = if (transitFromBottom) {
                                    currentY < (bottomBoundaryY - commitDistance)
                                } else {
                                    currentY > (topBoundaryY + commitDistance)
                                }
                                if (committed) {
                                    val destY = if (transitFromBottom)
                                        topBoundaryY + 2f * beadR + beadGap
                                    else
                                        bottomBoundaryY - 2f * beadR - beadGap
                                    scope.launch {
                                        animateSlideTransit(transitFromBottom, destY, beadR)
                                    }
                                } else {
                                    // Didn't cross midpoint → spring back to source position.
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val snapTarget = if (transitFromBottom) bottomBoundaryY else topBoundaryY
                                    scope.launch {
                                        val moveJob = launch {
                                            transitYAnim.animateTo(snapTarget, spring(stiffness = 300f, dampingRatio = 0.7f))
                                        }
                                        val liftJob = launch {
                                            transitLiftAnim.animateTo(0f, tween(180))
                                        }
                                        moveJob.join()
                                        liftJob.join()
                                        isInTransit = false
                                    }
                                }
                            } else if (!hasMoved && !isOnPressBead) {
                                when {
                                    localResetArmed -> {
                                        if (!sessionCompleted && !hasExited) {
                                            currentCount = 0
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                    showResetOverlay -> {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    isPressMode -> registerIncrement()
                                    // No string or stealth hidden: single tap counts, double-tap toggles stealth.
                                    !hasString || !widgetsVisible -> {
                                        if (settings.tasbeehStealthModeAllowed) {
                                            val secondTap = withTimeoutOrNull(300L) { awaitFirstDown(requireUnconsumed = false) }
                                            if (secondTap != null) {
                                                secondTap.consume()
                                                widgetsVisible = !widgetsVisible
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            } else {
                                                registerIncrement()
                                            }
                                        } else {
                                            registerIncrement()
                                        }
                                    }
                                    // String visible, not near string: double-tap → stealth toggle.
                                    settings.tasbeehStealthModeAllowed && !isNearString -> {
                                        val secondTap = withTimeoutOrNull(300L) { awaitFirstDown(requireUnconsumed = false) }
                                        if (secondTap != null) {
                                            secondTap.consume()
                                            widgetsVisible = !widgetsVisible
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                    // Tap off-string with no stealth allowed → increment.
                                    !isNearString -> { registerIncrement() }
                                    // Tap on string with no move → do nothing (Step 3 handles swipe).
                                }
                            }

                            showResetOverlay = false
                        }
                    }
            ) {
                AnimatedVisibility(visible = widgetsVisible, enter = fadeIn(), exit = fadeOut()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 24.dp, end = 24.dp, top = topInset),
                    ) {
                        Column {
                            Text(
                                text = currentCount.toString(),
                                style = TextStyle(
                                    color = textColor,
                                    fontSize = 96.sp,
                                    fontWeight = FontWeight.Normal,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                ),
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                            )
                            Text(
                                text = stringResource(R.string.tasbih_out_of, currentItem?.targetCount ?: 0),
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor.copy(alpha = 0.5f),
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                            )
                        }
                        Box(
                            modifier = Modifier.weight(1f).then(
                                slideDhikrTextWidth?.let { Modifier.width(it) } ?: Modifier.fillMaxWidth()
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            CompositionLocalProvider(LocalContentColor provides textColor) {
                                TasbihDhikrText(
                                    text = currentItem?.name ?: "سُبْحَانَ اللَّهِ",
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                        Spacer(Modifier.height(bottomContentPadding))
                    }
                }

                if (isPressMode) {
                    val displayedBeadColor = if (pressBeadPressed) {
                        beadColor.copy(
                            red = beadColor.red * 0.84f,
                            green = beadColor.green * 0.84f,
                            blue = beadColor.blue * 0.84f,
                        )
                    } else beadColor

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = PRESS_BEAD_BOTTOM_PADDING)
                            .size(PRESS_BEAD_SIZE)
                            .graphicsLayer {
                                scaleX = pressBeadScale.value
                                scaleY = pressBeadScale.value
                            }
                            .semantics {
                                contentDescription = pressBeadDescription
                                role = Role.Button
                                onClick {
                                    scope.launch {
                                        animatePressBead()
                                        registerIncrement()
                                    }
                                    true
                                }
                            }
                            .pointerInput(isPressMode) {
                                detectTapGestures(
                                    onPress = {
                                        setPressBeadPressed(true)
                                        val released = tryAwaitRelease()
                                        setPressBeadPressed(false)
                                        if (released) registerIncrement()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val radius = size.minDimension / 2f
                            drawTasbihBead(
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = radius,
                                baseColor = displayedBeadColor,
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showResetOverlay,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        progress = { resetProgress },
                        modifier = Modifier.size(24.dp),
                        color = if (resetArmed) MaterialTheme.colorScheme.error else Color.White,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        strokeWidth = 3.dp
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = stringResource(
                                                    if (resetArmed) R.string.tasbih_release_to_reset_hint
                                                    else R.string.tasbih_holding_to_reset_hint,
                                                ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (resetArmed) MaterialTheme.colorScheme.error else Color.White
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !showResetOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 48.dp)
        ) {
            OutlinedButton(
                onClick = { exitToList() },
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.2f))
            ) {
                Text(text = stringResource(R.string.tasbih_exit), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    if (sessionCompleted) {
        AlertDialog(
            onDismissRequest = { exitToList() },
            title = { Text(stringResource(R.string.tasbih_completion_title)) },
            text = { Text(stringResource(R.string.tasbih_completion_message)) },
            confirmButton = {
                TextButton(onClick = { exitToList() }) {
                    Text(stringResource(R.string.tasbih_completion_ok))
                }
            },
        )
    }
}
