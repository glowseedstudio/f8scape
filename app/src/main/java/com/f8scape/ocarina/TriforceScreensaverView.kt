package com.f8scape.ocarina

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f8scape.R
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Authentic Ambient Triforce Screensaver with the Three Sacred Spiritual Stones.
 * Features:
 * - Authentic in-game 3D models of Kokiri's Emerald, Goron's Ruby, and Zora's Sapphire.
 * - Perfectly centered in both landscape and portrait orientations.
 * - Multi-wall super bouncy physics: tap or fling them to send them bouncing across the screen,
 *   and when they slow down, they slide smoothly back to their canonical altar positions!
 * - Super Premium Door of Time Activation Sequence:
 *   When all three stones settle back home, a majestic 3D spinning Triforce materializes above them,
 *   illuminating with soft sweeping God Rays, elemental convergence beams, and divine golden stardust!
 * - Cleaned design without dark shadow dots.
 * - User setting toggle to show/hide spiritual stones on the screensaver.
 * - Dynamic 3D perspective projection, harmonic floating bobbing, and multi-axis 3D tilt.
 * - Specular sheen tightly wrapped to the stone's exact alpha silhouette (no circular cutoff).
 * - Multi-layer elemental radiant auras (Forest Emerald green, Fire Ruby red, Water Sapphire cyan).
 * - Continuous 8-point minute-boundary OLED orbital shifting ensuring 100% burn-in protection.
 * - 10-second inactivity dimming to save battery on AMOLED/OLED displays.
 * - Double tap anywhere to exit, single tap to wake & inspect stone lore.
 */
@Composable
fun TriforceScreensaverView(
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier,
    showStones: Boolean = true
) {
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var lastInteractionTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showExitHint by remember { mutableStateOf(false) }
    var selectedStoneName by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current.density

    // Update internal clock ticker for orbital shift and idle dimming
    LaunchedEffect(Unit) {
        while (true) {
            delay(500L)
            currentTimeMs = System.currentTimeMillis()
        }
    }

    // Auto-hide the exit hint and stone title after 3.5 seconds
    LaunchedEffect(showExitHint, selectedStoneName, lastInteractionTimeMs) {
        if (showExitHint || selectedStoneName != null) {
            delay(3500L)
            showExitHint = false
            selectedStoneName = null
        }
    }

    // OLED Burn-In Orbital Drift: 8-point cycle advancing every 60 seconds
    val burnInStep = remember(currentTimeMs / 60_000L) {
        ((currentTimeMs / 60_000L) % 8).toInt()
    }
    val burnInOffset = remember(burnInStep) {
        when (burnInStep) {
            0 -> IntOffset(0, -10)
            1 -> IntOffset(8, -6)
            2 -> IntOffset(12, 0)
            3 -> IntOffset(8, 6)
            4 -> IntOffset(0, 10)
            5 -> IntOffset(-8, 6)
            6 -> IntOffset(-12, 0)
            7 -> IntOffset(-8, -6)
            else -> IntOffset.Zero
        }
    }

    // 10-Second Idle Dimming for deep OLED battery savings and protection
    val isIdleDimmed = (currentTimeMs - lastInteractionTimeMs) >= 10_000L
    val idleDimAlpha by animateFloatAsState(
        targetValue = if (isIdleDimmed) 0.70f else 0.0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "TriforceScreensaverDim"
    )

    // Smooth continuous animation transitions for the 3D spiritual stones
    val infiniteTransition = rememberInfiniteTransition(label = "SpiritualStones3D")
    val timeSeconds by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "screensaver_time"
    )
    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stone_aura"
    )

    // Independent 2D Bounce Physics for each of the three spiritual stones
    val stonesPhysics = remember {
        listOf(StonePhysicsState(), StonePhysicsState(), StonePhysicsState())
    }

    // Sacred Triforce Awakening Activation State
    var isActivationActive by remember { mutableStateOf(false) }
    var activationStartTimeMs by remember { mutableLongStateOf(0L) }
    var wasFlung by remember { mutableStateOf(false) }

    // Interactive launch impulse that scatters the stones across the screen with super bouncy physics
    fun triggerBounceAll() {
        lastInteractionTimeMs = System.currentTimeMillis()
        wasFlung = true
        isActivationActive = false

        // Emerald shoots diagonally up-left
        stonesPhysics[0].launch(
            impulseX = -2300f - Random.nextFloat() * 800f,
            impulseY = -2500f - Random.nextFloat() * 700f
        )
        // Ruby shoots diagonally up-right
        stonesPhysics[1].launch(
            impulseX = 2100f + Random.nextFloat() * 900f,
            impulseY = -2700f - Random.nextFloat() * 800f
        )
        // Sapphire shoots diagonally down-right
        stonesPhysics[2].launch(
            impulseX = 2400f + Random.nextFloat() * 800f,
            impulseY = 2300f + Random.nextFloat() * 800f
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020305)) // Deep OLED black
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap() },
                    onTap = {
                        lastInteractionTimeMs = System.currentTimeMillis()
                        showExitHint = true
                    }
                )
            }
    ) {
        // Floating layer with OLED orbital burn-in drift
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { burnInOffset }
        ) {
            // 1. Persistent Sacred 3D Spinning Triforces
            OcarinaSongBackgroundEffect(
                isPlaying = false,
                songId = "zeldas_lullaby",
                currentNoteIndex = -1,
                isDimmed = isIdleDimmed,
                modifier = Modifier.fillMaxSize()
            )

            // 2. The Three Sacred Spiritual Stones: Centered in Landscape and Portrait with Bounce Physics
            if (showStones) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val minDim = minOf(maxWidth, maxHeight)
                    val stoneSize = when {
                        minDim < 360.dp -> 76.dp
                        minDim < 500.dp -> 88.dp
                        minDim < 700.dp -> 100.dp
                        else -> 118.dp
                    }
                    val stoneSpacing = when {
                        minDim < 360.dp -> 12.dp
                        minDim < 500.dp -> 18.dp
                        else -> 28.dp
                    }

                    val screenW = maxWidth.value * density
                    val screenH = maxHeight.value * density
                    val stonePx = stoneSize.value * density
                    val spacingPx = stoneSpacing.value * density

                    // Slot offsets relative to screen center
                    val slotOffsetsX = remember(stonePx, spacingPx) {
                        listOf(-(stonePx + spacingPx), 0f, +(stonePx + spacingPx))
                    }

                    // Keep stones inside screen boundaries
                    val pad = stonePx * 0.52f
                    val minScreenX = -screenW * 0.5f + pad
                    val maxScreenX = screenW * 0.5f - pad
                    val minScreenY = -screenH * 0.5f + pad
                    val maxScreenY = screenH * 0.5f - pad

                    // Continuous physics ticker for fluid 60-120fps bounce, smooth return, and activation trigger
                    var lastFrameTimeMs by remember { mutableLongStateOf(0L) }
                    LaunchedEffect(screenW, screenH) {
                        while (true) {
                            withInfiniteAnimationFrameMillis { frameTime ->
                                if (lastFrameTimeMs != 0L) {
                                    val dt = ((frameTime - lastFrameTimeMs) / 1000f).coerceIn(0.001f, 0.040f)
                                    for (i in 0..2) {
                                        val minOffsetX = minScreenX - slotOffsetsX[i]
                                        val maxOffsetX = maxScreenX - slotOffsetsX[i]
                                        stonesPhysics[i].update(
                                            dt = dt,
                                            minOffsetX = minOffsetX,
                                            maxOffsetX = maxOffsetX,
                                            minOffsetY = minScreenY,
                                            maxOffsetY = maxScreenY
                                        )
                                    }

                                    // Check if all three stones were flung and have now all settled back home!
                                    val allSettled = stonesPhysics.all { !it.isBouncing && !it.isReturning && !it.isDragging }
                                    if (wasFlung && allSettled && !isActivationActive) {
                                        wasFlung = false
                                        isActivationActive = true
                                        activationStartTimeMs = frameTime
                                    }
                                }
                                lastFrameTimeMs = frameTime
                            }
                        }
                    }

                    // 3. Super Premium Sacred Awakening: 3D Triforce with soft light beams above the stones
                    if (isActivationActive) {
                        val activationTimeSec = ((lastFrameTimeMs - activationStartTimeMs) / 1000f).coerceAtLeast(0f)
                        val totalDuration = 6.2f
                        val progress = (activationTimeSec / totalDuration).coerceIn(0f, 1f)
                        if (progress >= 1.0f) {
                            isActivationActive = false
                        }

                        // Intensity envelope: smooth ease-in, radiant peak with gentle pulse, graceful dissolve
                        val intensity = when {
                            activationTimeSec < 1.1f -> {
                                val t = activationTimeSec / 1.1f
                                t * t * (3f - 2f * t)
                            }
                            activationTimeSec < 4.7f -> {
                                val pulse = 1.0f + 0.06f * sin((activationTimeSec - 1.1f) * 3.5f)
                                pulse.coerceIn(0.9f, 1.08f)
                            }
                            else -> {
                                val t = (activationTimeSec - 4.7f) / (totalDuration - 4.7f)
                                ((1f - t) * (1f - t)).coerceIn(0f, 1f)
                            }
                        }

                        if (intensity > 0.005f) {
                            val triforceCx = screenW * 0.5f
                            val triforceCy = screenH * 0.5f - (stonePx * 1.15f)
                            val triforceScale = stonePx * 0.88f

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawSacredActivationSequence(
                                    cx = triforceCx,
                                    cy = triforceCy,
                                    scale = triforceScale,
                                    rotY = activationTimeSec * 0.70f,
                                    intensity = intensity,
                                    timeSeconds = activationTimeSec,
                                    stonePositions = listOf(
                                        Offset(screenW * 0.5f + slotOffsetsX[0], screenH * 0.5f),
                                        Offset(screenW * 0.5f + slotOffsetsX[1], screenH * 0.5f),
                                        Offset(screenW * 0.5f + slotOffsetsX[2], screenH * 0.5f)
                                    )
                                )
                            }
                        }
                    }

                    // 4. Centered Row of the Three Spiritual Stones
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(stoneSpacing, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Left: Kokiri's Emerald (Forest)
                        SpiritualStone3D(
                            stoneRes = R.drawable.spiritual_stone_kokiri_emerald,
                            name = "Kokiri's Emerald",
                            element = "Spiritual Stone of the Forest",
                            accentColor = Color(0xFF00E676),
                            stoneSize = stoneSize,
                            phase = 0.0f,
                            timeSeconds = timeSeconds,
                            auraScale = auraPulse,
                            density = density,
                            physics = stonesPhysics[0],
                            isSelected = selectedStoneName?.contains("Kokiri") == true,
                            onClick = {
                                triggerBounceAll()
                                selectedStoneName = "Kokiri's Emerald · Spiritual Stone of the Forest"
                            },
                            onFling = { wasFlung = true }
                        )

                        // 2. Center: Goron's Ruby (Fire)
                        SpiritualStone3D(
                            stoneRes = R.drawable.spiritual_stone_goron_ruby,
                            name = "Goron's Ruby",
                            element = "Spiritual Stone of Fire",
                            accentColor = Color(0xFFFF2A4B),
                            stoneSize = stoneSize,
                            phase = 2.094f,
                            timeSeconds = timeSeconds,
                            auraScale = auraPulse,
                            density = density,
                            physics = stonesPhysics[1],
                            isSelected = selectedStoneName?.contains("Goron") == true,
                            onClick = {
                                triggerBounceAll()
                                selectedStoneName = "Goron's Ruby · Spiritual Stone of Fire"
                            },
                            onFling = { wasFlung = true }
                        )

                        // 3. Right: Zora's Sapphire (Water)
                        SpiritualStone3D(
                            stoneRes = R.drawable.spiritual_stone_zora_sapphire,
                            name = "Zora's Sapphire",
                            element = "Spiritual Stone of Water",
                            accentColor = Color(0xFF00B0FF),
                            stoneSize = stoneSize,
                            phase = 4.188f,
                            timeSeconds = timeSeconds,
                            auraScale = auraPulse,
                            density = density,
                            physics = stonesPhysics[2],
                            isSelected = selectedStoneName?.contains("Zora") == true,
                            onClick = {
                                triggerBounceAll()
                                selectedStoneName = "Zora's Sapphire · Spiritual Stone of Water"
                            },
                            onFling = { wasFlung = true }
                        )
                    }
                }
            }
        }

        // 10-second Inactivity Dimming Scrim (drops display draw to 30% brightness)
        if (idleDimAlpha > 0.005f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = idleDimAlpha))
            )
        }

        // Selected Stone Title Banner (displays upon tapping any stone)
        AnimatedVisibility(
            visible = selectedStoneName != null && !isIdleDimmed,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(400)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 44.dp)
        ) {
            Surface(
                color = Color(0xFF0A0E18).copy(alpha = 0.92f),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = selectedStoneName ?: "",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }

        // Minimal exit hint (shows upon tapping, auto-fades after 3.5s)
        AnimatedVisibility(
            visible = showExitHint && selectedStoneName == null && !isIdleDimmed,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(500)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        ) {
            Surface(
                color = Color(0xFF141824).copy(alpha = 0.85f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Double tap anywhere to exit",
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * High-performance 2D bounce physics state with restitution collision, friction, and smooth return.
 */
private class StonePhysicsState {
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)
    var vx by mutableFloatStateOf(0f)
    var vy by mutableFloatStateOf(0f)
    var isBouncing by mutableStateOf(false)
    var isReturning by mutableStateOf(false)
    var isDragging by mutableStateOf(false)
    var bounceCount by mutableStateOf(0)

    fun launch(impulseX: Float, impulseY: Float) {
        vx = impulseX
        vy = impulseY
        isBouncing = true
        isReturning = false
        isDragging = false
        bounceCount = 0
    }

    fun update(
        dt: Float,
        minOffsetX: Float,
        maxOffsetX: Float,
        minOffsetY: Float,
        maxOffsetY: Float
    ) {
        if (isDragging) return

        if (isBouncing) {
            offsetX += vx * dt
            offsetY += vy * dt

            // Super-elastic wall bounce off left and right boundaries
            if (offsetX < minOffsetX) {
                offsetX = minOffsetX
                vx = abs(vx) * 0.94f // High elasticity
                vy += (Random.nextFloat() - 0.5f) * 220f // Deflection kick
                bounceCount++
            } else if (offsetX > maxOffsetX) {
                offsetX = maxOffsetX
                vx = -abs(vx) * 0.94f
                vy += (Random.nextFloat() - 0.5f) * 220f
                bounceCount++
            }

            // Super-elastic wall bounce off top and bottom boundaries
            if (offsetY < minOffsetY) {
                offsetY = minOffsetY
                vy = abs(vy) * 0.94f
                vx += (Random.nextFloat() - 0.5f) * 220f
                bounceCount++
            } else if (offsetY > maxOffsetY) {
                offsetY = maxOffsetY
                vy = -abs(vy) * 0.94f
                vx += (Random.nextFloat() - 0.5f) * 220f
                bounceCount++
            }

            // Low air resistance so they ricochet across many walls
            val drag = exp(-0.18f * dt)
            vx *= drag
            vy *= drag

            // When kinetic energy drops below threshold after multiple wall ricochets, smoothly slide back home
            val speed = hypot(vx.toDouble(), vy.toDouble()).toFloat()
            if (bounceCount >= 6 && speed < 80f) {
                isBouncing = false
                isReturning = true
            }
        } else if (isReturning) {
            // Smooth magnetic slide back to home starting position (0, 0)
            val returnFactor = (4.5f * dt).coerceIn(0f, 1f)
            offsetX += (0f - offsetX) * returnFactor
            offsetY += (0f - offsetY) * returnFactor

            if (abs(offsetX) < 1.2f && abs(offsetY) < 1.2f) {
                offsetX = 0f
                offsetY = 0f
                isReturning = false
                bounceCount = 0
            }
        }
    }
}

/**
 * Authentic 3D Floating Spiritual Stone with perspective projection,
 * harmonic bobbing, multi-axis 3D tilt, tightly wrapped specular sheen, and dynamic bounce physics.
 * Note: Dark shadow dots have been completely removed as requested!
 */
@Composable
private fun SpiritualStone3D(
    stoneRes: Int,
    name: String,
    element: String,
    accentColor: Color,
    stoneSize: Dp,
    phase: Float,
    timeSeconds: Float,
    auraScale: Float,
    density: Float,
    physics: StonePhysicsState,
    isSelected: Boolean,
    onClick: () -> Unit,
    onFling: () -> Unit
) {
    // 3D Harmonic floating physics
    val bobY = (sin(timeSeconds * 1.5 + phase) * 8.0).toFloat()
    val rotY = (sin(timeSeconds * 0.95 + phase) * 22.0).toFloat()
    val rotX = (cos(timeSeconds * 1.15 + phase) * 11.0).toFloat()
    val rotZ = (sin(timeSeconds * 0.65 + phase * 0.5) * 5.0).toFloat()
    val depthScale = 1.0f + (0.04f * cos(timeSeconds * 0.95 + phase)).toFloat()

    // Dynamic 3D specular light position across surface: catches facets as it tilts
    val gleamOffset = ((sin(timeSeconds * 0.95 + phase) * 0.5 + 0.5)).toFloat().coerceIn(0.05f, 0.95f)

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = physics.offsetX.roundToInt(),
                    y = (physics.offsetY + bobY * density).roundToInt()
                )
            }
            .size(stoneSize + 16.dp)
            .pointerInput(name) {
                detectTapGestures(onTap = { onClick() })
            }
            .pointerInput(name) {
                var lastDeltaX = 0f
                var lastDeltaY = 0f
                detectDragGestures(
                    onDragStart = {
                        physics.isDragging = true
                        physics.isBouncing = false
                        physics.isReturning = false
                        lastDeltaX = 0f
                        lastDeltaY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        physics.offsetX += dragAmount.x
                        physics.offsetY += dragAmount.y
                        lastDeltaX = dragAmount.x
                        lastDeltaY = dragAmount.y
                    },
                    onDragEnd = {
                        physics.isDragging = false
                        onFling()
                        // Launch with high-speed fling into the multi-wall bounce simulation
                        val speedX = lastDeltaX * 85f
                        val speedY = lastDeltaY * 85f
                        val launchVx = if (abs(speedX) > 150f) speedX else (if (physics.offsetX >= 0) 1800f else -1800f)
                        val launchVy = if (abs(speedY) > 150f) speedY else (if (physics.offsetY >= 0) 1800f else -1800f)
                        physics.launch(
                            impulseX = launchVx.coerceIn(-3400f, 3400f),
                            impulseY = launchVy.coerceIn(-3400f, 3400f)
                        )
                    },
                    onDragCancel = {
                        physics.isDragging = false
                        physics.isReturning = true
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Volumetric Elemental Radial Bloom Aura (No dark shadow dots!)
        Box(
            modifier = Modifier
                .size(stoneSize * 1.18f * auraScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.38f * auraScale),
                            accentColor.copy(alpha = 0.12f * auraScale),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // 2. Selection Shimmering Halo when inspected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(stoneSize * 1.08f)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFFFFD700).copy(alpha = 0.8f),
                                Color(0xFFFFF9C4),
                                Color(0xFFFFD700).copy(alpha = 0.8f)
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // 3. Authentic 3D Game Model with Tightly-Wrapped Specular Sheen (No circle edge)
        // Uses offscreen compositing with BlendMode.SrcAtop so the sheen is strictly clipped to the stone's alpha pixels!
        Box(
            modifier = Modifier
                .size(stoneSize)
                .graphicsLayer {
                    this.cameraDistance = 8f * density // Strong 3D foreshortening
                    this.rotationY = rotY
                    this.rotationX = rotX
                    this.rotationZ = rotZ
                    this.scaleX = depthScale
                    this.scaleY = depthScale
                    this.compositingStrategy = CompositingStrategy.Offscreen
                }
        ) {
            // Authentic In-Game 3D Model
            Image(
                painter = painterResource(id = stoneRes),
                contentDescription = name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic Specular Light Gleam: Strictly masked to the exact stone silhouette
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val startX = (gleamOffset - 0.22f) * w * 1.6f
                val endX = (gleamOffset + 0.22f) * w * 1.6f

                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        start = Offset(startX, 0f),
                        end = Offset(endX, h)
                    ),
                    blendMode = BlendMode.SrcAtop
                )
            }
        }
    }
}

/**
 * Super Premium Cinematic Ocarina of Time Sacred Activation Sequence.
 * Renders the divine awakening of the sacred 3D Triforce above the spiritual stones:
 * - Three elemental convergence rays (Emerald green, Ruby fire, Sapphire water) feeding into the apex.
 * - 12 soft, volumetric heavenly God Rays rotating in 360 degrees.
 * - Luminous sacred sunburst flare with multi-tier corona bloom.
 * - Pristine 3D spinning golden Triforce with beveled edges and specular facet shines.
 * - Upward ascending holy fairy stardust particles.
 */
private fun DrawScope.drawSacredActivationSequence(
    cx: Float,
    cy: Float,
    scale: Float,
    rotY: Float,
    intensity: Float,
    timeSeconds: Float,
    stonePositions: List<Offset>
) {
    val maxRadius = max(size.width, size.height) * 0.75f

    // 1. Three Elemental Convergence Beams from the three stones up to the sacred Triforce
    val elementalColors = listOf(
        Pair(Color(0xFF00E676), Color(0xFFB9F6CA)), // Emerald (Forest)
        Pair(Color(0xFFFF1744), Color(0xFFFF8A80)), // Ruby (Fire)
        Pair(Color(0xFF00B0FF), Color(0xFF80D8FF))  // Sapphire (Water)
    )

    for (i in 0..2) {
        val stonePos = stonePositions[i]
        val (baseColor, glowColor) = elementalColors[i]

        // Outer soft glow line
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    baseColor.copy(alpha = 0.55f * intensity),
                    glowColor.copy(alpha = 0.75f * intensity),
                    Color(0xFFFFF9C4).copy(alpha = 0.85f * intensity)
                ),
                start = stonePos,
                end = Offset(cx, cy)
            ),
            start = stonePos,
            end = Offset(cx, cy),
            strokeWidth = 6.5f,
            cap = StrokeCap.Round
        )

        // Inner bright laser core
        drawLine(
            color = Color.White.copy(alpha = 0.90f * intensity),
            start = stonePos,
            end = Offset(cx, cy),
            strokeWidth = 2.0f,
            cap = StrokeCap.Round
        )

        // Resonant energy ring around stone
        drawCircle(
            color = glowColor.copy(alpha = 0.40f * intensity),
            radius = 28f + sin(timeSeconds * 6f + i * 2f) * 6f,
            center = stonePos,
            style = Stroke(width = 2.5f)
        )
    }

    // 2. 12 Volumetric Heavenly God Rays (Soft beams of light sweeping outward)
    val numBeams = 12
    val beamRotation = timeSeconds * 18.0 // 18 deg/sec smooth rotation
    val beamLength = maxRadius * 0.95f
    val beamHalfAngle = 8.5 // degrees width per beam

    for (b in 0 until numBeams) {
        val beamCenterAngle = (b * (360.0 / numBeams) + beamRotation) % 360.0
        val rad1 = Math.toRadians(beamCenterAngle - beamHalfAngle)
        val rad2 = Math.toRadians(beamCenterAngle + beamHalfAngle)

        val p1 = Offset(cx + (cos(rad1) * beamLength).toFloat(), cy + (sin(rad1) * beamLength).toFloat())
        val p2 = Offset(cx + (cos(rad2) * beamLength).toFloat(), cy + (sin(rad2) * beamLength).toFloat())

        val beamPath = Path().apply {
            moveTo(cx, cy)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            close()
        }

        // Soft undulating beam intensity
        val wave = (sin(timeSeconds * 2.5 + b * 0.8) * 0.25f + 0.75f).toFloat().coerceIn(0.4f, 1.0f)
        drawPath(
            path = beamPath,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFDE7).copy(alpha = 0.42f * intensity * wave),
                    Color(0xFFFFD700).copy(alpha = 0.22f * intensity * wave),
                    Color(0xFFFFA000).copy(alpha = 0.08f * intensity * wave),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = beamLength
            )
        )
    }

    // 3. Central Sacred Sunburst Flare (Multi-tiered radiant corona)
    val flareRadius = scale * 2.4f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.92f * intensity),
                Color(0xFFFFF9C4).copy(alpha = 0.65f * intensity),
                Color(0xFFFFD700).copy(alpha = 0.35f * intensity),
                Color(0xFFFF8F00).copy(alpha = 0.12f * intensity),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = flareRadius
        ),
        radius = flareRadius,
        center = Offset(cx, cy)
    )

    // 4. The Sacred 3D Golden Spinning Triforce
    val cosAngle = cos(rotY)
    val sinAngle = sin(rotY)
    val lightIntensity = (sinAngle * 0.35f + 0.65f).coerceIn(0.25f, 1.0f)

    val faceColor = Color(
        red = 1.0f,
        green = (0.84f + 0.14f * lightIntensity).coerceIn(0f, 1f),
        blue = (0.15f + 0.35f * lightIntensity).coerceIn(0f, 1f),
        alpha = intensity
    )
    val bevelColor = Color(0xFFFFFFFF).copy(alpha = 0.95f * intensity)
    val shadowColor = Color(0xFF8D6E00).copy(alpha = 0.75f * intensity)

    val subTriangles = listOf(
        Pair(0f, -0.50f),      // Top apex triangle
        Pair(-0.50f, 0.366f),  // Bottom-Left triangle
        Pair(0.50f, 0.366f)    // Bottom-Right triangle
    )
    val halfH = scale * 0.433f

    for ((offX, offY) in subTriangles) {
        val subCenterX = cx + (offX * scale * cosAngle)
        val subCenterY = cy + (offY * scale)

        val pTop = Offset(subCenterX, subCenterY - halfH)
        val pLeft = Offset(subCenterX - (scale * 0.50f * cosAngle), subCenterY + halfH)
        val pRight = Offset(subCenterX + (scale * 0.50f * cosAngle), subCenterY + halfH)

        val trianglePath = Path().apply {
            moveTo(pTop.x, pTop.y)
            lineTo(pRight.x, pRight.y)
            lineTo(pLeft.x, pLeft.y)
            close()
        }

        // Fill solid golden illuminated face
        drawPath(trianglePath, color = faceColor, style = Fill)

        // Delicate brilliant white-gold bevel edge
        drawPath(
            path = trianglePath,
            color = bevelColor,
            style = Stroke(width = max(1.8f, scale * 0.032f), cap = StrokeCap.Round)
        )

        // Depth shadow line on bottom base edge
        drawLine(
            color = shadowColor,
            start = pLeft,
            end = pRight,
            strokeWidth = max(1.2f, scale * 0.024f)
        )
    }

    // 5. Ascending Holy Fairy Stardust Particles
    for (s in 0 until 18) {
        val pPhase = s * 0.35f
        val pTime = (timeSeconds * 0.6f + pPhase) % 1.0f
        val pRadius = scale * (0.6f + (s % 5) * 0.35f)
        val pAngle = (s * 47f) + (timeSeconds * 12f)
        val rad = Math.toRadians(pAngle.toDouble())

        val px = cx + (cos(rad) * pRadius).toFloat() + sin(timeSeconds * 3f + s) * 12f
        val py = cy - (pTime * scale * 2.2f) + (s % 3) * 15f
        val pAlpha = ((1f - pTime) * intensity).coerceIn(0f, 1f)

        if (pAlpha > 0.05f) {
            drawCircle(
                color = Color(0xFFFFFDE7).copy(alpha = pAlpha * 0.85f),
                radius = 2.2f + (s % 3) * 1.0f,
                center = Offset(px, py)
            )
        }
    }
}
