package com.imn.iman.ui.screens.tasbeeh


import androidx.compose.foundation.Canvas

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.imn.iman.ui.theme.PlusJakartaSans
import com.imn.iman.ui.theme.containsArabic
import com.imn.iman.ui.theme.fontForText
import kotlin.math.abs
import kotlin.math.exp



sealed interface TasbihWidget {
    val offsetX: Float
    val offsetY: Float
    val scale: Float
    val zIndex: Float
    val alpha: Float

    data class StringBeadWidget(
        override val offsetX: Float = 0.88f,
        override val offsetY: Float = 0.5f,
        override val scale: Float = 1f,
        override val zIndex: Float = 0f,
        override val alpha: Float = 1f,
        val topStackLimit: Int = 3,
        val bottomStackLimit: Int = 7,
        val beadSizeScale: Float = 1f,

        val beadMicroScale: Float = 1.5f,
    ) : TasbihWidget

}

val DefaultTasbihWidgets = listOf(
    TasbihWidget.StringBeadWidget(
        offsetX = 0.69905907f,
        offsetY = 0.5f,
        scale = 1f,
        alpha = 1f,
        topStackLimit = 3,
        bottomStackLimit = 5,
        beadSizeScale = 1f,
        beadMicroScale = 1.5f,
    ),
)

val ClassicTasbihWidgets = emptyList<TasbihWidget>()

internal const val BEAD_RADIUS_BASE = 18f


@Composable
fun TasbihWidgetRenderer(
        widget: TasbihWidget,
        countedBeads: Int = 0,
        totalBeads: Int = 33,
        beadColor: Color = Color(0xFF3192F5),
        textColor: Color = Color.White,
        thumbPositionProvider: () -> Offset? = { null },
        // Fisheye strength (0..1): multiply peak scale and glow to fade smoothly on lift.
        fisheyeStrengthProvider: () -> Float = { 1f },
        // Transit bead: canvas-local Y of the bead being dragged between stacks, null = none.
        transitBeadProvider: () -> Float? = { null },
        // 0..1 lift amount for the transit bead. 1 = fully picked up, 0 = landed.
        transitLiftProvider: () -> Float = { 0f },
        transitFromBottom: Boolean = true,
        // Conveyor-belt scroll offset in canvas-local px: applied to all bead Y positions.
        // Positive = beads shifted downward; animates to 0 for slide-up effect on increment.
        scrollOffsetProvider: () -> Float = { 0f },
        modifier: Modifier = Modifier,
) {
    val baseModifier = modifier.graphicsLayer {
        alpha = widget.alpha
        clip = false
    }

    when (widget) {
        is TasbihWidget.StringBeadWidget -> {
            val topLimit = widget.topStackLimit
            val bottomLimit = widget.bottomStackLimit
            val beadRadiusBase = BEAD_RADIUS_BASE * widget.beadSizeScale

            Canvas(modifier = baseModifier.fillMaxHeight(0.9f).width(120.dp)) {
                val beadRadius = beadRadiusBase * density
                // ─── TUNABLE: Base gap between beads ────────────────────────────────
                //   Higher multiplier (e.g. 0.8f) → beads spaced further apart
                //   Lower  multiplier (e.g. 0.2f) → beads tightly packed
                val gap = beadRadius * 0.4f
                val cx = size.width / 2f

                // Read all providers at draw-phase — only the canvas draw layer is invalidated.
                val fingerY: Float? = thumbPositionProvider()?.let { it.y + size.height / 2f }
                // fisheyeStrength fades 1→0 on finger lift — drives glow and scale fade-out.
                val fisheyeStrength = fisheyeStrengthProvider()
                // scrollOffset shifts all bead positions for the conveyor-belt slide animation.
                val scrollOffset = scrollOffsetProvider()

                // ─── TUNABLE: Gaussian wave width ───────────────────────────────────
                // sigma controls how far the magnification spreads from the finger.
                //   Higher (e.g. 5f) → wave covers more beads, softer peak
                //   Lower  (e.g. 2f) → tight spotlight, only 1-2 beads affected
                val sigma = beadRadius * 4f

                // Base string — thin so the glow dominates.
                drawLine(
                        color = Color.White.copy(alpha = 0.25f * widget.alpha),
                        start = Offset(cx, 0f),
                        end = Offset(cx, size.height),
                        strokeWidth = 1.5f * density,
                        cap = StrokeCap.Round,
                )

                // Double-layer string glow: tight bright core + wide soft outer halo.
                fingerY?.let { fy ->
                    // ─── TUNABLE: Outer halo (wide, soft) ─────────────────────────
                    //   outerHalf multiplier (4f) → how long the fade extends (higher = longer)
                    //   strokeWidth (28f)          → how wide the halo is (higher = fatter bloom)
                    //   alpha (0.25f)              → brightness of outer glow
                    val outerHalf = sigma * 4f
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.25f * widget.alpha * fisheyeStrength),
                                Color.Transparent,
                            ),
                            startY = (fy - outerHalf).coerceAtLeast(0f),
                            endY = (fy + outerHalf).coerceAtMost(size.height),
                        ),
                        start = Offset(cx, (fy - outerHalf).coerceAtLeast(0f)),
                        end = Offset(cx, (fy + outerHalf).coerceAtMost(size.height)),
                        strokeWidth = 28f * density,
                        cap = StrokeCap.Round,
                    )
                    // ─── TUNABLE: Inner core (narrow, bright) ───────────────────
                    //   innerHalf multiplier (2f) → length of inner glow (lower = tighter)
                    //   strokeWidth (10f)          → width of the bright streak
                    //   alpha (0.9f)               → peak brightness at centre
                    val innerHalf = sigma * 2f
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.9f * widget.alpha * fisheyeStrength),
                                Color.Transparent,
                            ),
                            startY = (fy - innerHalf).coerceAtLeast(0f),
                            endY = (fy + innerHalf).coerceAtMost(size.height),
                        ),
                        start = Offset(cx, (fy - innerHalf).coerceAtLeast(0f)),
                        end = Offset(cx, (fy + innerHalf).coerceAtMost(size.height)),
                        strokeWidth = 10f * density,
                        cap = StrokeCap.Round,
                    )
                }

                val topCount = minOf(countedBeads, topLimit).coerceAtLeast(0)
                val bottomCount = minOf(totalBeads - countedBeads, bottomLimit).coerceAtLeast(0)

                // During transit, source stack shows one fewer bead (the one is in transit).
                // Read at draw-phase — no recomposition.
                val transitCanvasY: Float? = transitBeadProvider()
                val effectiveTopCount    = if (!transitFromBottom && transitCanvasY != null) (topCount    - 1).coerceAtLeast(0) else topCount
                val effectiveBottomCount = if ( transitFromBottom && transitCanvasY != null) (bottomCount - 1).coerceAtLeast(0) else bottomCount

                // ─── TUNABLE: Peak zoom per bead ─────────────────────────────────
                // beadMicroScale is the fixed maximum magnification at the touch point.
                //   Higher (e.g. 3.0f) → extreme zoom, bead much larger under finger
                //   Lower  (e.g. 1.4f) → subtle magnification
                // The exponent divisor (1.5f below) sharpens/softens the falloff:
                //   Higher divisor (e.g. 3.0f) → softer, more gradual falloff from peak
                //   Lower  divisor (e.g. 0.8f) → very sharp peak, falls off quickly
                fun beadScale(naturalY: Float): Float {
                    fingerY ?: return 1f
                    val dist = kotlin.math.abs(naturalY - fingerY)
                    val gaussian = kotlin.math.exp(-(dist * dist) / (1.5f * sigma * sigma)).toFloat()
                    return 1f + (widget.beadMicroScale - 1f) * gaussian * fisheyeStrength
                }

                // ─── TUNABLE: Gap stretch at peak ───────────────────────────────
                // The 0.5f multiplier controls how much extra space opens between beads near the finger.
                //   Higher (e.g. 1.0f) → beads push further apart, very dock-like spread
                //   0.0f               → gap stays fixed regardless of zoom
                fun scaledGap(scaleA: Float, scaleB: Float) = gap * (1f + 0.5f * ((scaleA + scaleB) / 2f - 1f))

                // ── Top stack ──────────────────────────────────────────────────────
                val topNaturalYs = FloatArray(effectiveTopCount) { i ->
                    beadRadius + i * (beadRadius * 2f + gap)
                }
                val topScales = FloatArray(effectiveTopCount) { i -> beadScale(topNaturalYs[i]) }
                val topCenterYs = FloatArray(effectiveTopCount)
                if (effectiveTopCount > 0) {
                    topCenterYs[0] = beadRadius * topScales[0]
                    for (i in 1 until effectiveTopCount) {
                        topCenterYs[i] = topCenterYs[i - 1] + beadRadius * topScales[i - 1] +
                            scaledGap(topScales[i - 1], topScales[i]) + beadRadius * topScales[i]
                    }
                }
                for (i in 0 until effectiveTopCount) {
                    val r = beadRadius * topScales[i]
                    val cy = topCenterYs[i] + scrollOffset  // scroll offset shifts all top-stack beads
                    if (topScales[i] > 1.15f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.18f * widget.alpha * (topScales[i] - 1f)), Color.Transparent),
                                center = Offset(cx, cy),
                                radius = r * 1.8f,
                            ),
                            radius = r * 1.8f,
                            center = Offset(cx, cy),
                        )
                    }
                    drawTasbihBead(Offset(cx, cy), r, widget.alpha, beadColor)
                }

                // ── Bottom stack ────────────────────────────────────────────────────
                val bottomNaturalYs = FloatArray(effectiveBottomCount) { i ->
                    size.height - beadRadius - (effectiveBottomCount - 1 - i) * (beadRadius * 2f + gap)
                }
                val bottomScales = FloatArray(effectiveBottomCount) { i -> beadScale(bottomNaturalYs[i]) }
                val bottomCenterYs = FloatArray(effectiveBottomCount)
                if (effectiveBottomCount > 0) {
                    bottomCenterYs[effectiveBottomCount - 1] =
                        size.height - beadRadius * bottomScales[effectiveBottomCount - 1]
                    for (i in effectiveBottomCount - 2 downTo 0) {
                        bottomCenterYs[i] = bottomCenterYs[i + 1] - beadRadius * bottomScales[i + 1] -
                            scaledGap(bottomScales[i + 1], bottomScales[i]) - beadRadius * bottomScales[i]
                    }
                }
                for (i in 0 until effectiveBottomCount) {
                    val r = beadRadius * bottomScales[i]
                    val cy = bottomCenterYs[i] + scrollOffset  // scroll offset shifts all bottom-stack beads
                    if (bottomScales[i] > 1.15f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.18f * widget.alpha * (bottomScales[i] - 1f)), Color.Transparent),
                                center = Offset(cx, cy),
                                radius = r * 1.8f,
                            ),
                            radius = r * 1.8f,
                            center = Offset(cx, cy),
                        )
                    }
                    drawTasbihBead(Offset(cx, cy), r, widget.alpha, beadColor)
                }

                // ── Transit bead: drawn on top, follows finger across the gap ───────
                // Visible only while a bead is being dragged between stacks.
                transitCanvasY?.let { ty ->
                    val transitLift = transitLiftProvider().coerceIn(0f, 1f)
                    val minTransitScale = 1f + (0.5f * transitLift)
                    val tScale = beadScale(ty).coerceAtLeast(minTransitScale)
                    val tr = beadRadius * tScale
                    // Large glowing halo so the held bead stands out clearly.
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.45f * widget.alpha * transitLift), Color.Transparent),
                            center = Offset(cx, ty),
                            radius = tr * 2.5f,
                        ),
                        radius = tr * 2.5f,
                        center = Offset(cx, ty),
                    )
                    drawTasbihBead(Offset(cx, ty), tr, widget.alpha, beadColor)
                }
            }
        }

    }
}

@Composable
fun TasbihDhikrText(text: String, modifier: Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val maxFontSize = MaterialTheme.typography.headlineLarge.fontSize
    val minFontSize = 18.sp
    val fontFamily = fontForText(text, PlusJakartaSans)
    val lineHeightMultiplier = if (containsArabic(text)) 1.9f else 1.3f
    val density = androidx.compose.ui.platform.LocalDensity.current

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val availableWidth = constraints.maxWidth
        val availableHeight = constraints.maxHeight
        val measuredFont = remember(text, availableWidth, availableHeight, maxFontSize, fontFamily, density) {
            var candidate = maxFontSize
            var selected = minFontSize
            var fits = false
            while (true) {
                val style = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = candidate,
                    fontWeight = FontWeight.Bold,
                    lineHeight = candidate * lineHeightMultiplier,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                )
                val layout = textMeasurer.measure(
                    text = text,
                    style = style,
                    constraints = Constraints(maxWidth = availableWidth, maxHeight = availableHeight),
                )
                if (!layout.hasVisualOverflow && layout.size.width <= availableWidth && layout.size.height <= availableHeight) {
                    selected = candidate
                    fits = true
                    break
                }
                if (candidate <= minFontSize) break
                candidate = (candidate.value - 2f).coerceAtLeast(minFontSize.value).sp
            }
            selected to fits
        }
        val style = TextStyle(
            fontFamily = fontFamily,
            color = LocalContentColor.current,
            fontSize = measuredFont.first,
            fontWeight = FontWeight.Bold,
            lineHeight = measuredFont.first * lineHeightMultiplier,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        )

        if (measuredFont.second) {
            Text(text = text, style = style, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = text, style = style, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}


internal fun DrawScope.drawTasbihBead(
    center: Offset,
    radius: Float,
    alpha: Float = 1f,
    baseColor: Color = Color(0xFF3192F5),
) {
    val opacity = alpha * baseColor.alpha

    // Soft, subtle shadow
    drawCircle(
        color = Color.Black.copy(alpha = opacity * 0.28f),
        radius = radius,
        center = center + Offset(radius * 0.035f, radius * 0.055f),
    )

    // Diffused light from the upper-left
    val lightCenter = center + Offset(-radius * 0.44f, -radius * 0.56f)

    // Gentle golden highlight instead of a white reflection
    val highlight = Color(
        red = baseColor.red + (1f - baseColor.red) * 0.28f,
        green = baseColor.green + (1f - baseColor.green) * 0.28f,
        blue = baseColor.blue + (1f - baseColor.blue) * 0.28f,
        alpha = opacity,
    )

    val body = baseColor.copy(alpha = opacity)

    // Keep the edges golden, not dark brown
    val edge = Color(
        red = baseColor.red * 0.78f,
        green = baseColor.green * 0.78f,
        blue = baseColor.blue * 0.78f,
        alpha = opacity,
    )

    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to highlight,
                0.58f to body,
                1.0f to edge,
            ),
            center = lightCenter,
            radius = radius * 1.8f,
        ),
        radius = radius,
        center = center,
    )
}
