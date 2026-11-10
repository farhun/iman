@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.kaizen.khushu.ui.screens.home

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaizen.khushu.R
import com.kaizen.khushu.ui.components.KhushuAppBar
import com.kaizen.khushu.ui.theme.PlusJakartaSans
import dev.chrisbanes.haze.HazeState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.drawscope.rotate

private fun findNextPrayer(prayers: List<PrayerInfo>, now: Long): PrayerInfo? {
    if (prayers.isEmpty()) return null

    prayers.firstOrNull { it.rawTime.toEpochMilliseconds() > now }?.let {
        return it
    }

    val fajr = prayers.first()
    return fajr.copy(rawTime = kotlinx.datetime.Instant.fromEpochMilliseconds(fajr.rawTime.toEpochMilliseconds() + 86400000L))
}

private fun findCurrentPrayer(prayers: List<PrayerInfo>, now: Long): PrayerInfo? {
    if (prayers.isEmpty()) return null
    return prayers.lastOrNull { it.rawTime.toEpochMilliseconds() <= now } ?: prayers.last()
}


private const val KAABA_LATITUDE = 21.4225
private const val KAABA_LONGITUDE = 39.8262

private fun qiblaBearingDegrees(latitude: Double, longitude: Double): Double {
    val latRad = Math.toRadians(latitude)
    val lngRad = Math.toRadians(longitude)
    val kaabaLatRad = Math.toRadians(KAABA_LATITUDE)
    val kaabaLngRad = Math.toRadians(KAABA_LONGITUDE)
    val deltaLng = kaabaLngRad - lngRad
    val y = sin(deltaLng) * cos(kaabaLatRad)
    val x = cos(latRad) * sin(kaabaLatRad) - sin(latRad) * cos(kaabaLatRad) * cos(deltaLng)
    return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
}

private fun compassPointLabel(bearing: Double): String {
    val points =
        listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    val index = (((bearing + 11.25) % 360) / 22.5).toInt()
    return points[index]
}

private data class CompassState(val heading: Float?, val accuracy: Int)
@Composable
private fun rememberDeviceHeading(latitude: Float, longitude: Float): CompassState {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val headingState = remember { mutableStateOf<Float?>(null) }
    val accuracyState = remember { mutableStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }
    val declination = remember(latitude, longitude) {
        android.hardware.GeomagneticField(
            latitude, longitude, 0f, System.currentTimeMillis()
        ).declination
    }

    DisposableEffect(sensorManager, declination) {
        val manager = sensorManager
        val rotationSensor = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: manager?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
        val magnetSensor = manager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (manager == null || rotationSensor == null) {
            onDispose { }
        } else {
            val rotationMatrix = FloatArray(9)
            var sinAvg = 0.0
            var cosAvg = 0.0
            var initialized = false
            var lastShown = Float.NaN
            val alpha = 0.12

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val r = rotationMatrix

                    // 1.0 = flat (use top edge), 0.0 = upright (use back of phone)
                    val up = Math.abs(r[8].toDouble())
                    val wTop = ((up - 0.5) / 0.35).coerceIn(0.0, 1.0)

                    // Horizontal (east, north) parts of the top edge and the back
                    val topE = r[1].toDouble()
                    val topN = r[4].toDouble()
                    val backE = -r[2].toDouble()
                    val backN = -r[5].toDouble()
                    val topLen = Math.hypot(topE, topN)
                    val backLen = Math.hypot(backE, backN)

                    var vE = 0.0
                    var vN = 0.0
                    if (topLen > 0.2 && wTop > 0.0) {
                        vE += wTop * topE / topLen
                        vN += wTop * topN / topLen
                    }
                    if (backLen > 0.2 && wTop < 1.0) {
                        vE += (1.0 - wTop) * backE / backLen
                        vN += (1.0 - wTop) * backN / backLen
                    }

                    // Direction is ambiguous here: keep the last heading instead of jumping
                    if (Math.hypot(vE, vN) < 0.3) return

                    val magneticDeg = Math.toDegrees(atan2(vE, vN))
                    val trueDeg = (magneticDeg + declination + 720.0) % 360.0

                    val rad = Math.toRadians(trueDeg)
                    if (!initialized) {
                        sinAvg = sin(rad)
                        cosAvg = cos(rad)
                        initialized = true
                    } else {
                        sinAvg = sinAvg * (1 - alpha) + sin(rad) * alpha
                        cosAvg = cosAvg * (1 - alpha) + cos(rad) * alpha
                    }
                    val smoothed =
                        ((Math.toDegrees(atan2(sinAvg, cosAvg)) + 360.0) % 360.0).toFloat()

                    val diff = if (lastShown.isNaN()) 360f
                    else Math.abs(((smoothed - lastShown + 540f) % 360f) - 180f)
                    if (diff >= 1f) {
                        lastShown = smoothed
                        headingState.value = smoothed
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }

            val accuracyListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) = Unit
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    accuracyState.value = accuracy
                }
            }

            manager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
            if (magnetSensor != null) {
                manager.registerListener(
                    accuracyListener, magnetSensor, SensorManager.SENSOR_DELAY_UI
                )
            }
            onDispose {
                manager.unregisterListener(listener)
                manager.unregisterListener(accuracyListener)
            }
        }
    }

    return CompassState(headingState.value, accuracyState.value)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QiblaCompassDialog(
    bearingDegrees: Double,
    locationLabel: String,
    latitude: Float,
    longitude: Float,
    onDismiss: () -> Unit,
) {
    val compass = rememberDeviceHeading(latitude, longitude)
    val heading = compass.heading
    val needsCalibration =
        heading != null && compass.accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
    val relativeBearing = heading?.let { ((bearingDegrees - it + 360.0) % 360.0).toFloat() }
    val ringColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val northTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val needleColor = MaterialTheme.colorScheme.primary
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Qibla Direction",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = PlusJakartaSans),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = locationLabel.ifBlank { "Current location" },
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlusJakartaSans),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(220.dp)) {
                        val stroke = 8.dp.toPx()
                        val radius = min(size.width, size.height) / 2f - stroke
                        drawCircle(
                            color = ringColor,
                            radius = radius,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                        )
                        rotate(degrees = -(heading ?: 0f), pivot = center) {
                            drawLine(
                                color = northTickColor,
                                start = center.copy(y = center.y - radius),
                                end = center.copy(y = center.y - radius + 22.dp.toPx()),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                        if (relativeBearing != null) {
                            drawLine(
                                color = needleColor,
                                start = center,
                                end = androidx.compose.ui.geometry.Offset(
                                    x = center.x + sin(
                                        Math.toRadians(
                                            (relativeBearing ?: 0f).toDouble()
                                        )
                                    ).toFloat() * radius * 0.78f,
                                    y = center.y - cos(
                                        Math.toRadians(
                                            (relativeBearing ?: 0f).toDouble()
                                        )
                                    ).toFloat() * radius * 0.78f
                                ),
                                strokeWidth = 6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                        drawCircle(
                            color = needleColor,
                            radius = 7.dp.toPx(),
                            center = center
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .graphicsLayer { rotationZ = -(heading ?: 0f) }
                    ) {
                        Text(
                            text = "N",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "${bearingDegrees.roundToInt()}° ${compassPointLabel(bearingDegrees)} to Makkah",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = PlusJakartaSans),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = heading?.let { "Facing ${it.roundToInt()}° now" }
                        ?: "Compass sensor unavailable on this device",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlusJakartaSans),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
                if (needsCalibration) {
                    Text(
                        text = "Compass needs calibration. Move your phone in a figure-8 motion.",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlusJakartaSans),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Text(
                    text = "Coordinates: ${"%.4f".format(Locale.US, latitude)}, ${"%.4f".format(Locale.US, longitude)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlusJakartaSans),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    }
}

private data class HomeHijriBadgeParts(
    val dayMonth: String,
    val year: String,
)

private fun splitHijriBadgeParts(raw: String): HomeHijriBadgeParts {
    val compact = raw.trim().replace(Regex("\\s+"), " ")
    if (compact.isBlank()) {
        return HomeHijriBadgeParts(dayMonth = "Hijri Date", year = "")
    }
    val pieces = compact.split(" ")
    return if (pieces.size >= 2) {
        HomeHijriBadgeParts(dayMonth = pieces.dropLast(1).joinToString(" "), year = pieces.last())
    } else {
        HomeHijriBadgeParts(dayMonth = compact, year = "")
    }
}

@Composable
private fun HomeHijriBadge(
    badge: HomeHijriBadgeParts,
    modifier: Modifier = Modifier,
) {
    val baseColor = MaterialTheme.colorScheme.onSurface
    val label = remember(badge, baseColor) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = baseColor)) { append(badge.dayMonth) }
            if (badge.year.isNotBlank()) {
                append("  ")
                withStyle(SpanStyle(color = baseColor.copy(alpha = 0.4f))) { append(badge.year) }
            }
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Text(
            text = label,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun KhushuPullRefreshIndicator(
    progress: Float,
    isRefreshing: Boolean,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val logoRes = if (darkTheme) R.drawable.ic_iman_logo else R.drawable.ic_iman_logo_black
    val targetProgress = if (isRefreshing) 1f else progress.coerceIn(0f, 1f)
    val visibleProgress by
    animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = if (isRefreshing) 240 else 120),
        label = "home_refresh_indicator_progress"
    )
    val indicatorAlpha by
    animateFloatAsState(
        targetValue = if (isRefreshing || progress > 0f) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "home_refresh_indicator_alpha"
    )
    val pulseScaleBase =
        rememberInfiniteTransition(label = "home_refresh_logo_pulse")
            .animateFloat(
                initialValue = 1f,
                targetValue = 1.10f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(
                            durationMillis = 900,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                label = "home_refresh_logo_scale"
            )
            .value
    val pulseScale = if (isRefreshing) pulseScaleBase else 1f

    Box(
        modifier =
            modifier.graphicsLayer {
                alpha = indicatorAlpha
                scaleX = pulseScale
                scaleY = pulseScale
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = logoRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
            modifier = Modifier.fillMaxSize()
        )
        Icon(
            painter = painterResource(id = logoRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier.fillMaxSize().drawWithContent {
                    val clipTop = size.height * (1f - visibleProgress)
                    clipRect(top = clipTop) { this@drawWithContent.drawContent() }
                }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    hazeState: HazeState,
    contentPadding: PaddingValues,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    val darkTheme = isSystemInDarkTheme()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()


    var currentInstant by remember { mutableStateOf(Instant.fromEpochMilliseconds(System.currentTimeMillis())) }
    LaunchedEffect(Unit) {
        while (true) {
            currentInstant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            kotlinx.coroutines.delay(1000L)
        }
    }

    val currentTimeMillis = uiState.previewTime?.toEpochMilliseconds() ?: currentInstant.toEpochMilliseconds()
    val refreshThresholdPx = with(density) { 118.dp.toPx() }
    val refreshHoldPx = with(density) { 74.dp.toPx() }
    val maxPullPx = with(density) { 164.dp.toPx() }

    var cachedPrayers by remember { mutableStateOf<List<PrayerInfo>>(emptyList()) }
    var cachedExtraTimings by remember { mutableStateOf<List<PrayerInfo>>(emptyList()) }

    var cachedHijriDate by remember { mutableStateOf("") }


    var showTimeOverrideDialog by remember { mutableStateOf(false) }
    var selectedQuickAction by remember { mutableStateOf<HomeQuickAction?>(null) }
    var previewHourText by remember { mutableStateOf("") }
    var previewMinuteText by remember { mutableStateOf("") }
    var pullOffsetPx by remember { mutableFloatStateOf(0f) }
    var thresholdReached by remember { mutableStateOf(false) }

    LaunchedEffect(
        uiState.prayers,
        uiState.extraTimings,
        uiState.hijriDate
    ) {
        if (uiState.prayers.isNotEmpty()) {
            cachedPrayers = uiState.prayers
        }
        if (uiState.extraTimings.isNotEmpty() || cachedExtraTimings.isEmpty()) {
            cachedExtraTimings = uiState.extraTimings
        }

        if (uiState.hijriDate.isNotBlank()) {
            cachedHijriDate = uiState.hijriDate
        }

    }


    val displayPrayers = uiState.prayers.ifEmpty { cachedPrayers }
    val displayExtraTimings = uiState.extraTimings.ifEmpty { cachedExtraTimings }

    val displayHijriDate = uiState.hijriDate.ifBlank { cachedHijriDate }

    val hijriBadge = remember(displayHijriDate) { splitHijriBadgeParts(displayHijriDate) }
    val qiblaBearing = remember(uiState.locationLat, uiState.locationLng) {
        qiblaBearingDegrees(uiState.locationLat.toDouble(), uiState.locationLng.toDouble())
    }

    val currentPrayer = findCurrentPrayer(displayPrayers, currentTimeMillis)
    val homeVisibleTimings =
        if (uiState.showExtraPrayerTimingsOnHome) {
            (displayPrayers + displayExtraTimings).sortedBy { it.rawTime.toEpochMilliseconds() }
        } else {
            displayPrayers
        }
    val nextPrayer = findNextPrayer(homeVisibleTimings, currentTimeMillis)

    val sunArcT by derivedStateOf {
        val fajrMs = displayPrayers.firstOrNull { it.name == "Fajr" }?.rawTime?.toEpochMilliseconds() ?: 0L
        val ishaMs = displayPrayers.firstOrNull { it.name == "Isha" }?.rawTime?.toEpochMilliseconds() ?: 1L
        val total = (ishaMs - fajrMs).toFloat()
        if (total <= 0) 0.5f else {
            val ratio = (currentTimeMillis - fajrMs).toFloat() / total
            (0.08f + ratio * (0.93f - 0.08f)).coerceIn(-0.1f, 1.1f)
        }
    }


    val locationLabel = uiState.locationLabel.ifBlank { "Your area" }
    val pullProgress = (pullOffsetPx / refreshThresholdPx).coerceIn(0f, 1f)


    val animatedPullOffsetPx by
    animateFloatAsState(
        targetValue =
            when {
                uiState.isRefreshing -> refreshHoldPx
                else -> pullOffsetPx
            },
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.88f),
        label = "home_pull_offset"
    )

    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing) {
            pullOffsetPx = 0f
            thresholdReached = false
        }
    }

    val pullRefreshConnection =
        remember(listState, uiState.isRefreshing) {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: androidx.compose.ui.geometry.Offset,
                    source: NestedScrollSource
                ): androidx.compose.ui.geometry.Offset {
                    if (source != NestedScrollSource.UserInput)
                        return androidx.compose.ui.geometry.Offset.Zero

                    if (available.y < 0f && pullOffsetPx > 0f) {
                        val previous = pullOffsetPx
                        pullOffsetPx = (pullOffsetPx + available.y).coerceAtLeast(0f)
                        if (pullOffsetPx < refreshThresholdPx) {
                            thresholdReached = false
                        }
                        return androidx.compose.ui.geometry.Offset(0f, pullOffsetPx - previous)
                    }

                    return androidx.compose.ui.geometry.Offset.Zero
                }

                override fun onPostScroll(
                    consumed: androidx.compose.ui.geometry.Offset,
                    available: androidx.compose.ui.geometry.Offset,
                    source: NestedScrollSource
                ): androidx.compose.ui.geometry.Offset {
                    if (source != NestedScrollSource.UserInput || uiState.isRefreshing) {
                        return androidx.compose.ui.geometry.Offset.Zero
                    }

                    if (available.y > 0f && !listState.canScrollBackward) {
                        val previous = pullOffsetPx
                        val resistance =
                            0.55f - ((previous / maxPullPx).coerceIn(0f, 1f) * 0.2f)
                        pullOffsetPx =
                            (previous + available.y * resistance).coerceAtMost(maxPullPx)

                        if (pullOffsetPx >= refreshThresholdPx && !thresholdReached) {
                            thresholdReached = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else if (pullOffsetPx < refreshThresholdPx) {
                            thresholdReached = false
                        }

                        return androidx.compose.ui.geometry.Offset(0f, available.y)
                    }

                    return androidx.compose.ui.geometry.Offset.Zero
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity
                ): Velocity {
                    if (!uiState.isRefreshing && pullOffsetPx >= refreshThresholdPx) {
                        pullOffsetPx = refreshHoldPx
                        viewModel.refreshPrayerData()
                    } else if (!uiState.isRefreshing) {
                        pullOffsetPx = 0f
                        thresholdReached = false
                    }

                    return Velocity.Zero
                }
            }
        }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(pullRefreshConnection),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    val sunriseTime = (displayPrayers + displayExtraTimings)
                        .firstOrNull {
                            it.name.contains("shuruq", ignoreCase = true) ||
                                    it.name.contains("sunrise", ignoreCase = true)
                        }?.time ?: ""
                    val sunsetTime = displayPrayers
                        .firstOrNull { it.name.equals("Maghrib", ignoreCase = true) }
                        ?.time ?: ""

                    PrayerSunMergedCard(
                        currentPrayer = currentPrayer,
                        nextPrayer = nextPrayer,
                        sunT = sunArcT,
                        allPrayers = displayPrayers,
                        makruhZones = uiState.makruhZones,
                        darkTheme = darkTheme,
                        sunriseTime = sunriseTime,
                        sunsetTime = sunsetTime,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 22.dp, start = 14.dp, end = 14.dp),
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(
                                    bottom = contentPadding.calculateBottomPadding() +
                                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp
                                ),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { translationY = animatedPullOffsetPx },
                            ) {
                                item {
                                    PrayerSlab(
                                        prayers = displayPrayers,
                                        extraTimings =
                                            if (uiState.showExtraPrayerTimingsOnHome) displayExtraTimings
                                            else emptyList(),
                                        activePrayerName = currentPrayer?.name,
                                        onQiblaClick = { selectedQuickAction = HomeQuickAction.QIBLA },
                                        ayahRef = uiState.ayahRef,
                                        darkTheme = darkTheme,
                                        locationLabel = uiState.locationLabel,
                                        source = uiState.calculationSource,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            KhushuPullRefreshIndicator(
                                progress = if (thresholdReached || uiState.isRefreshing) 1f else pullProgress,
                                isRefreshing = uiState.isRefreshing,
                                darkTheme = darkTheme,
                                modifier = Modifier.align(Alignment.TopCenter)
                                    .padding(top = 14.dp)
                                    .size(52.dp)
                                    .graphicsLayer {
                                        translationY = (animatedPullOffsetPx * 0.22f) - with(density) { 8.dp.toPx() }
                                    }
                            )
                        }
                    }
                }
            }
        }

        KhushuAppBar(
            title = "",
            onSettingsClick = onSettingsClick,
            centerOverlayContent = { HomeHijriBadge(badge = hijriBadge) },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    selectedQuickAction?.let {
        QiblaCompassDialog(
            bearingDegrees = qiblaBearing,
            locationLabel = locationLabel,
            latitude = uiState.locationLat,
            longitude = uiState.locationLng,
            onDismiss = { selectedQuickAction = null }
        )
    }

    if (showTimeOverrideDialog) {
        AlertDialog(
            onDismissRequest = { showTimeOverrideDialog = false },
            title = { Text("Preview Prayer Time") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Set a temporary 24-hour clock time to test next prayer and makruh transitions."
                    )
                    OutlinedTextField(
                        value = previewHourText,
                        onValueChange = {
                            previewHourText = it.filter(Char::isDigit).take(2)
                        },
                        label = { Text("Hour (0-23)") }
                    )
                    OutlinedTextField(
                        value = previewMinuteText,
                        onValueChange = {
                            previewMinuteText = it.filter(Char::isDigit).take(2)
                        },
                        label = { Text("Minute (0-59)") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val hour = previewHourText.toIntOrNull()
                        val minute = previewMinuteText.toIntOrNull()
                        if (hour != null && minute != null) {
                            viewModel.setPreviewTime(hour, minute)
                            showTimeOverrideDialog = false
                        }
                    }
                ) { Text("Apply") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.clearPreviewTime()
                            showTimeOverrideDialog = false
                        }
                    ) { Text("Reset") }
                    TextButton(onClick = { showTimeOverrideDialog = false }) { Text("Close") }
                }
            }
        )
    }
}
