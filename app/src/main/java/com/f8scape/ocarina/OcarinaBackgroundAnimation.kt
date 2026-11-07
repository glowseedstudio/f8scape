package com.f8scape.ocarina

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.*
import kotlin.random.Random

/**
 * Authentic Cinematic Ocarina Background Animation Engine.
 * Recreates the distinct magical in-game animations from The Legend of Zelda: Ocarina of Time:
 * - Epona's Song: Luminous golden musical notes (♪, ♫) floating upward with undulating waves and amber ranch aura.
 * - Saria's Song: Swirling Kokiri emerald leaves tumbling in the forest breeze with playful dancing notes.
 * - Song of Storms: Tempest rain streaks falling across the screen, wind gusts, and thunder flashes.
 * - Bolero of Fire: Death Mountain volcanic embers and fire sparks rising in heat convection waves.
 * - Serenade of Water: Translucent aquatic bubbles floating upward with gentle wobbles and expanding ripples.
 * - Sun's Song: Radiant golden solar rays sweeping and pulsing outward from a warm daylight orb.
 * - Song of Time: Sacred blue Temple of Time ripples expanding outward like temporal waves.
 * - Zelda's Lullaby: Royal purple aurora with luminous sacred Triforce pulses and golden melody notes.
 * - Minuet of Forest: Emerald warp whirlwind with spiraling sacred leaves and Kokiri fairy spores.
 * - Requiem of Spirit: Desert sandstorm particles blowing across the screen with golden dunes heat haze.
 * - Nocturne of Shadow: Eerie violet phantom flames and shadow wisps drifting upward.
 * - Prelude of Light: Holy celestial pillars of golden light descending from the heavens with star dust.
 *
 * Also renders an ambient translucent 5-line musical staff in the background with the song's notes
 * lighting up and emitting particles in real-time as each note is played!
 */
@Composable
fun OcarinaSongBackgroundEffect(
    isPlaying: Boolean,
    songId: String?,
    currentNoteIndex: Int,
    isDimmed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val song = remember(songId) { OcarinaSongs.findById(songId ?: "") }

    // High performance frame ticker for smooth 60-120fps physics;
    // dynamically throttles down to 30fps when dimmed/idle for maximum battery savings.
    var frameTimeNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isDimmed) {
        if (isDimmed) {
            while (true) {
                frameTimeNanos = System.nanoTime()
                kotlinx.coroutines.delay(33L)
            }
        } else {
            while (true) {
                withInfiniteAnimationFrameMillis { millis ->
                    frameTimeNanos = millis * 1_000_000L
                }
            }
        }
    }

    val timeSeconds = (frameTimeNanos / 1_000_000_000.0).toFloat()

    // Silky-smooth playback fade: 1.0f when playing, gently easing down to 0.0f when idle with zero jitter
    val playbackAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = if (isPlaying) 700 else 1400,
            easing = androidx.compose.animation.core.CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
        ),
        label = "playback_alpha"
    )

    // Lightning flash timer for Song of Storms
    var lightningAlpha by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying, songId) {
        if (isPlaying && songId == "song_of_storms") {
            while (true) {
                kotlinx.coroutines.delay(Random.nextLong(2200L, 4500L))
                lightningAlpha = 0.55f
                kotlinx.coroutines.delay(70L)
                lightningAlpha = 0.15f
                kotlinx.coroutines.delay(50L)
                lightningAlpha = 0.40f
                kotlinx.coroutines.delay(90L)
                lightningAlpha = 0f
            }
        } else {
            lightningAlpha = 0f
        }
    }

    // Persistent procedural particles initialized once per song category
    val particles = remember(songId) {
        generateParticlesForSong(songId ?: "zeldas_lullaby")
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Deep Atmospheric Gradient Base & Idle 3D Spinning Triforces
        val baseColor = song?.accentColor ?: Color(0xFFFFD700)
        drawSongAtmosphericBackdrop(w, h, baseColor, playbackAlpha, songId, timeSeconds)

        // Idle Ambient Wallpaper: Sacred 3D Spinning Triforces
        // Prominent and elegant while idle, smoothly fading away as song animations awaken
        val idleWallpaperAlpha = (1f - playbackAlpha).coerceIn(0f, 1f)
        if (idleWallpaperAlpha > 0.02f) {
            drawSpinning3DTriforceWallpaper(
                w = w,
                h = h,
                time = timeSeconds,
                alpha = idleWallpaperAlpha
            )
        }

        // 2. Slanted Lightning flash for Song of Storms
        if (lightningAlpha > 0f) {
            drawRect(
                color = Color(0xFFE1F5FE).copy(alpha = lightningAlpha),
                size = size
            )
            drawLightningBolt(w, h, lightningAlpha)
        }

        // 3. Translucent Musical Staff with Active Notes (behind medallions)
        if (song != null && playbackAlpha > 0.1f) {
            drawMusicalStaffRibbon(
                w = w,
                h = h,
                song = song,
                currentNoteIndex = currentNoteIndex,
                time = timeSeconds,
                alpha = playbackAlpha
            )
        }

        // 4. Soft Outward Light & Sound Ripples (soft glowing wavefronts, not sharp lines)
        if (playbackAlpha > 0.05f) {
            drawSoftOutwardSongRipples(
                w = w,
                h = h,
                time = timeSeconds,
                accentColor = baseColor,
                alpha = playbackAlpha,
                isPlaying = isPlaying
            )
        }

        // 5. Song-Specific Magical Particle Animations
        drawSongSpecificParticles(
            particles = particles,
            songId = songId ?: "eponas_song",
            w = w,
            h = h,
            time = timeSeconds,
            alpha = playbackAlpha,
            accentColor = baseColor
        )
    }
}

/**
 * Ambient Wallpaper: Sacred 3D Spinning Triforces drifting peacefully in the golden ether.
 * Renders elegant, rotating golden Triforces with perspective depth, subtle 3D facet shading,
 * and warm ambient particle motes. Smoothly fades into the song animations when audio starts.
 */
private fun DrawScope.drawSpinning3DTriforceWallpaper(
    w: Float,
    h: Float,
    time: Float,
    alpha: Float
) {
    // 1. Ambient Golden Radiance in the center
    val centerGlow = Brush.radialGradient(
        colors = listOf(
            Color(0xFFFFD700).copy(alpha = 0.10f * alpha),
            Color(0xFFB78103).copy(alpha = 0.04f * alpha),
            Color.Transparent
        ),
        center = Offset(w * 0.5f, h * 0.5f),
        radius = max(w, h) * 0.6f
    )
    drawRect(brush = centerGlow, size = Size(w, h))

    // 2. Large Central Sacred 3D Spinning Triforce
    drawSingle3DTriforce(
        cx = w * 0.5f,
        cy = h * 0.48f,
        scale = 135f,
        rotY = time * 0.45f,
        bobY = sin(time * 0.8f) * 14f,
        alpha = 0.32f * alpha
    )

    // 3. Background Orbital 3D Spinning Triforces drifting peacefully
    val orbitalTriforces = listOf(
        Triple(w * 0.18f, h * 0.30f, 0.55f),
        Triple(w * 0.82f, h * 0.32f, 0.60f),
        Triple(w * 0.14f, h * 0.72f, 0.48f),
        Triple(w * 0.86f, h * 0.70f, 0.52f),
        Triple(w * 0.50f, h * 0.18f, 0.42f)
    )

    for ((index, orb) in orbitalTriforces.withIndex()) {
        val (ox, oy, sz) = orb
        val phase = index * 1.25f
        drawSingle3DTriforce(
            cx = ox + sin(time * 0.35f + phase) * 16f,
            cy = oy + cos(time * 0.40f + phase) * 12f,
            scale = 65f * sz,
            rotY = time * 0.35f + phase,
            bobY = sin(time * 0.6f + phase) * 8f,
            alpha = 0.14f * alpha
        )
    }
}

/**
 * Draws an individual 3D perspective Triforce with smooth continuous Y-axis rotation
 * and realistic face shading (gold illuminated vs shadowed faces as it spins).
 */
private fun DrawScope.drawSingle3DTriforce(
    cx: Float,
    cy: Float,
    scale: Float,
    rotY: Float,
    bobY: Float,
    alpha: Float
) {
    if (alpha <= 0.005f) return

    val centerY = cy + bobY
    // 3D rotation projection factor: cos(rotY) compresses horizontal width from +1 to -1
    val cosAngle = cos(rotY)
    val sinAngle = sin(rotY)

    // Realistic front vs back face lighting intensity
    val lightIntensity = (sinAngle * 0.35f + 0.65f).coerceIn(0.25f, 1.0f)
    val faceColor = Color(
        red = 1.0f,
        green = (0.78f + 0.15f * lightIntensity).coerceIn(0f, 1f),
        blue = (0.10f + 0.25f * lightIntensity).coerceIn(0f, 1f),
        alpha = alpha
    )
    val shadowColor = Color(0xFF8D6E00).copy(alpha = alpha * 0.75f)

    // A Triforce consists of 3 smaller identical equilateral triangles:
    // Top triangle, Bottom-Left triangle, Bottom-Right triangle
    val subTriangles = listOf(
        // (xOffset, yOffset) relative to center, normalized to scale
        Pair(0f, -0.50f),      // Top
        Pair(-0.50f, 0.366f),  // Bottom Left
        Pair(0.50f, 0.366f)    // Bottom Right
    )

    val halfH = scale * 0.433f // sqrt(3)/4

    for ((offX, offY) in subTriangles) {
        val subCenterX = cx + (offX * scale * cosAngle)
        val subCenterY = centerY + (offY * scale)

        // 3 Vertices of this mini-triangle
        val pTop = Offset(subCenterX, subCenterY - halfH)
        val pLeft = Offset(subCenterX - (scale * 0.50f * cosAngle), subCenterY + halfH)
        val pRight = Offset(subCenterX + (scale * 0.50f * cosAngle), subCenterY + halfH)

        val trianglePath = Path().apply {
            moveTo(pTop.x, pTop.y)
            lineTo(pRight.x, pRight.y)
            lineTo(pLeft.x, pLeft.y)
            close()
        }

        // Fill face with golden shaded lighting
        drawPath(trianglePath, color = faceColor, style = Fill)

        // Delicate luminous bevel edge
        drawPath(
            path = trianglePath,
            color = Color(0xFFFFF9C4).copy(alpha = alpha * 0.90f),
            style = Stroke(width = max(1.2f, scale * 0.025f), cap = StrokeCap.Round)
        )

        // Depth shadow line on receding edge
        drawLine(
            color = shadowColor,
            start = pLeft,
            end = pRight,
            strokeWidth = max(1f, scale * 0.02f)
        )
    }
}

/**
 * Organic Water Surface Ripples.
 * Simulates gentle, silky-smooth water disturbance expanding outward like drops on a tranquil pool:
 * - Fluid undulating perimeter (sine/cosine harmonic water wobble)
 * - Ultra-soft radial gradients that look like light refracting through water, not drawn lines
 * - Natural capillary deceleration and gentle dissolution
 */
private fun DrawScope.drawSoftOutwardSongRipples(
    w: Float,
    h: Float,
    time: Float,
    accentColor: Color,
    alpha: Float,
    isPlaying: Boolean
) {
    val cx = w * 0.5f
    val cy = h * 0.50f
    val maxRadius = max(w, h) * 0.68f

    // 3 organic water ripple fronts
    val numRipples = 3
    val speed = if (isPlaying) 0.28f else 0.14f

    for (i in 0 until numRipples) {
        val phaseOffset = i.toFloat() / numRipples.toFloat()
        val rawProgress = (time * speed + phaseOffset) % 1.0f
        // Fluid quadratic deceleration (water ripple decelerates naturally as energy disperses)
        val progress = 1f - (1f - rawProgress) * (1f - rawProgress)

        val baseRadius = progress * maxRadius
        if (baseRadius < 16f) continue

        // Water ripple visibility: very soft onset, silky sustain, and gentle feathering out
        val fade = sin(rawProgress * PI.toFloat()).coerceIn(0f, 1f)
        val rippleAlpha = (fade * fade) * (if (isPlaying) 0.18f else 0.08f) * alpha

        // Harmonic organic perturbation: slight 6-lobed gentle wave oscillation around the circle
        val segments = 48
        val path = Path()
        val waveAmplitude = (2.5f + progress * 7f)

        for (s in 0..segments) {
            val angle = (s.toFloat() / segments.toFloat()) * (2f * PI.toFloat())
            val wobble = sin(angle * 5f + time * 1.8f + i) * waveAmplitude +
                         cos(angle * 3f - time * 1.2f) * (waveAmplitude * 0.5f)
            val r = baseRadius + wobble
            val px = cx + cos(angle) * r
            val py = cy + sin(angle) * r

            if (s == 0) {
                path.moveTo(px, py)
            } else {
                path.lineTo(px, py)
            }
        }
        path.close()

        // 1. Wide diffused water swell (soft refraction glow)
        drawPath(
            path = path,
            color = accentColor.copy(alpha = rippleAlpha * 0.40f),
            style = Stroke(width = 36f + (progress * 28f), cap = StrokeCap.Round)
        )

        // 2. Main gentle undulating crest
        drawPath(
            path = path,
            color = accentColor.copy(alpha = rippleAlpha),
            style = Stroke(width = 16f + (progress * 12f), cap = StrokeCap.Round)
        )

        // 3. Delicate water highlight shimmer (translucent white-gold specular sheen)
        drawPath(
            path = path,
            color = Color(0xFFFFF9C4).copy(alpha = rippleAlpha * 0.55f),
            style = Stroke(width = 4f + (progress * 3f), cap = StrokeCap.Round)
        )
    }
}

// ---------------- Background Base Shader ----------------

private fun DrawScope.drawSongAtmosphericBackdrop(
    w: Float,
    h: Float,
    accent: Color,
    alpha: Float,
    songId: String?,
    time: Float
) {
    // Top-to-bottom dark cosmic gradient with ambient color wash
    val darkBackdrop = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF04060C),
            Color(0xFF080D18),
            Color(0xFF0B1220)
        )
    )
    drawRect(brush = darkBackdrop, size = Size(w, h))

    // Radial color blooms based on song theme
    val bloomAlpha = 0.22f * alpha
    val centerOffset = when (songId) {
        "song_of_storms" -> Offset(w * 0.5f, h * 0.2f)
        "suns_song" -> Offset(w * 0.5f, h * 0.45f)
        "prelude_of_light" -> Offset(w * 0.5f, 0f)
        else -> Offset(w * 0.5f, h * 0.5f)
    }

    val radialBloom = Brush.radialGradient(
        colors = listOf(
            accent.copy(alpha = bloomAlpha),
            accent.copy(alpha = bloomAlpha * 0.4f),
            Color.Transparent
        ),
        center = centerOffset,
        radius = max(w, h) * 0.75f
    )
    drawRect(brush = radialBloom, size = Size(w, h))

    // Soft Diffracted Sacred Light Beams for Prelude of Light (Volumetric God Rays)
    if (songId == "prelude_of_light" && alpha > 0.05f) {
        drawDiffractedLightBeams(w, h, time, alpha)
    }

    // Sacred Triforce Aura for Zelda's Lullaby
    if (songId == "zeldas_lullaby" && alpha > 0.1f) {
        val triforcePath = Path().apply {
            val cx = w * 0.5f
            val cy = h * 0.48f
            val r = 110f
            moveTo(cx, cy - r)
            lineTo(cx + r, cy + r * 0.8f)
            lineTo(cx - r, cy + r * 0.8f)
            close()
        }
        drawPath(
            path = triforcePath,
            color = Color(0xFFFFD700).copy(alpha = 0.08f * alpha),
            style = Stroke(width = 3f)
        )
    }
}

/**
 * Renders celestial, soft, diffracted volumetric light beams for Prelude of Light.
 * Eliminates harsh vertical rectangle edges by utilizing expanding conical light shafts,
 * multi-layered atmospheric diffraction haze, and smooth horizontal and vertical falloffs.
 */
private fun DrawScope.drawDiffractedLightBeams(
    w: Float,
    h: Float,
    time: Float,
    alpha: Float
) {
    if (alpha <= 0.05f) return

    val numBeams = 6
    for (i in 0 until numBeams) {
        val sway = sin(time * 0.7f + i * 1.1f) * 18f
        val swaySlow = cos(time * 0.45f + i * 0.9f) * 12f

        val topCenterX = w * (0.26f + i * 0.095f) + swaySlow
        val bottomCenterX = w * (0.04f + i * 0.185f) + sway

        val topHalfW = 14f + (i % 2) * 5f
        val bottomHalfW = 85f + (i % 3) * 28f

        // 1. Outer Diffused Haze Layer (Extremely soft edge scattering dissolving into air)
        val outerPath = Path().apply {
            moveTo(topCenterX - topHalfW * 1.8f, 0f)
            lineTo(bottomCenterX - bottomHalfW * 1.6f, h)
            lineTo(bottomCenterX + bottomHalfW * 1.6f, h)
            lineTo(topCenterX + topHalfW * 1.8f, 0f)
            close()
        }
        val outerBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFF9C4).copy(alpha = 0.09f * alpha),
                Color(0xFFFFD54F).copy(alpha = 0.04f * alpha),
                Color.Transparent
            ),
            startY = 0f,
            endY = h * 0.95f
        )
        drawPath(outerPath, brush = outerBrush)

        // 2. Main Soft Volumetric Shaft
        val mainPath = Path().apply {
            moveTo(topCenterX - topHalfW, 0f)
            lineTo(bottomCenterX - bottomHalfW, h)
            lineTo(bottomCenterX + bottomHalfW, h)
            lineTo(topCenterX + topHalfW, 0f)
            close()
        }
        val mainBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFDE7).copy(alpha = 0.20f * alpha),
                Color(0xFFFFE082).copy(alpha = 0.11f * alpha),
                Color(0xFFFFD54F).copy(alpha = 0.03f * alpha),
                Color.Transparent
            ),
            startY = 0f,
            endY = h * 0.88f
        )
        drawPath(mainPath, brush = mainBrush)

        // 3. Delicate Luminous Core Ray
        val corePath = Path().apply {
            moveTo(topCenterX - topHalfW * 0.35f, 0f)
            lineTo(bottomCenterX - bottomHalfW * 0.35f, h * 0.8f)
            lineTo(bottomCenterX + bottomHalfW * 0.35f, h * 0.8f)
            lineTo(topCenterX + topHalfW * 0.35f, 0f)
            close()
        }
        val coreBrush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.26f * alpha),
                Color(0xFFFFF9C4).copy(alpha = 0.12f * alpha),
                Color.Transparent
            ),
            startY = 0f,
            endY = h * 0.72f
        )
        drawPath(corePath, brush = coreBrush)
    }
}

// ---------------- Musical Staff Ribbon ----------------

private fun DrawScope.drawMusicalStaffRibbon(
    w: Float,
    h: Float,
    song: OcarinaSong,
    currentNoteIndex: Int,
    time: Float,
    alpha: Float
) {
    val staffCenterY = h * 0.50f
    val lineSpacing = 16f
    val totalHeight = lineSpacing * 4f
    val startY = staffCenterY - (totalHeight / 2f)

    val staffGold = Color(0xFFFFD54F).copy(alpha = 0.18f * alpha)

    // 5 Horizontal Golden Musical Staff Lines
    for (i in 0..4) {
        val y = startY + (i * lineSpacing)
        // Gentle undulating sine wave for magical fluidity
        val path = Path()
        val segments = 24
        val step = w / segments
        path.moveTo(0f, y + sin(time * 1.5f) * 2f)
        for (s in 1..segments) {
            val x = s * step
            val waveY = y + sin(time * 1.8f + (s * 0.25f)) * 3.5f
            path.lineTo(x, waveY)
        }
        drawPath(path, staffGold, style = Stroke(width = 1.2f))
    }

    // Note markers laid out horizontally along the staff
    val notes = song.buttonSequence
    val numNotes = notes.size
    if (numNotes == 0) return

    val margin = w * 0.12f
    val usableWidth = w - (margin * 2f)
    val noteSpacing = usableWidth / (numNotes + 1)

    for ((idx, button) in notes.withIndex()) {
        val nx = margin + ((idx + 1) * noteSpacing)
        // Staff vertical placement based on pitch
        val lineIndex = when (button) {
            OcarinaButton.C_UP -> 0.0f     // D5: Top line
            OcarinaButton.C_LEFT -> 1.0f   // B4: 4th line
            OcarinaButton.C_RIGHT -> 2.0f  // A4: 3rd line
            OcarinaButton.C_DOWN -> 3.0f   // F4: 2nd line
            OcarinaButton.A -> 4.0f        // D4: Bottom line
        }
        val waveOffset = sin(time * 1.8f + (((nx / w) * 24f) * 0.25f)) * 3.5f
        val ny = startY + (lineIndex * lineSpacing) + waveOffset

        val isActive = idx == currentNoteIndex
        val noteColor = if (isActive) Color(0xFFFFF9C4) else Color(0xFFFFD54F).copy(alpha = 0.35f * alpha)

        // Note head (tilted ellipse)
        val headRadius = if (isActive) 8.5f else 6.0f
        drawCircle(
            color = noteColor,
            radius = headRadius,
            center = Offset(nx, ny)
        )

        // Note Stem
        drawLine(
            color = noteColor,
            start = Offset(nx + headRadius - 1f, ny),
            end = Offset(nx + headRadius - 1f, ny - 24f),
            strokeWidth = 1.5f
        )

        // Active note glowing aura
        if (isActive) {
            drawCircle(
                color = song.accentColor.copy(alpha = 0.45f * alpha),
                radius = 24f,
                center = Offset(nx, ny)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f * alpha),
                radius = 4f,
                center = Offset(nx, ny)
            )
        }
    }
}

// ---------------- Procedural Particle System ----------------

private enum class ParticleShape {
    MUSIC_NOTE_SINGLE, // ♪
    MUSIC_NOTE_DOUBLE, // ♫
    KOKIRI_LEAF,       // Leaf with stem and vein
    RAIN_STREAK,       // Fast slanted rainfall
    FIRE_EMBER,        // Flickering rising volcanic spark
    WATER_BUBBLE,      // Translucent bubble with specular shine
    SUN_SPARK,         // Radiant solar flare mote
    SAND_GRAIN,        // Amber desert particle
    SHADOW_WISP,       // Ghostly purple wisp
    STAR_DUST          // Golden celestial sparkle
}

private data class SongParticle(
    var x: Float,
    var y: Float,
    val speedY: Float,
    val speedX: Float,
    val size: Float,
    val shape: ParticleShape,
    val color: Color,
    val phase: Float,
    val rotationSpeed: Float
)

private fun generateParticlesForSong(songId: String): List<SongParticle> {
    val count = when (songId) {
        "song_of_storms" -> 90 // Rich tempest rain
        "bolero_of_fire" -> 60 // Abundant volcanic embers
        "sarias_song", "minuet_of_forest" -> 50 // Swirling foliage
        else -> 40
    }

    val list = mutableListOf<SongParticle>()
    for (i in 0 until count) {
        val shape = when (songId) {
            "eponas_song" -> if (i % 2 == 0) ParticleShape.MUSIC_NOTE_SINGLE else ParticleShape.MUSIC_NOTE_DOUBLE
            "sarias_song", "minuet_of_forest" -> if (i % 3 == 0) ParticleShape.MUSIC_NOTE_SINGLE else ParticleShape.KOKIRI_LEAF
            "song_of_storms" -> ParticleShape.RAIN_STREAK
            "bolero_of_fire" -> ParticleShape.FIRE_EMBER
            "serenade_of_water" -> ParticleShape.WATER_BUBBLE
            "suns_song" -> ParticleShape.SUN_SPARK
            "requiem_of_spirit" -> ParticleShape.SAND_GRAIN
            "nocturne_of_shadow" -> ParticleShape.SHADOW_WISP
            "prelude_of_light", "zeldas_lullaby" -> if (i % 3 == 0) ParticleShape.MUSIC_NOTE_SINGLE else ParticleShape.STAR_DUST
            else -> ParticleShape.MUSIC_NOTE_SINGLE
        }

        val color = when (songId) {
            "eponas_song" -> Color(0xFFFFD54F) // Golden Amber
            "sarias_song", "minuet_of_forest" -> if (i % 2 == 0) Color(0xFF00E676) else Color(0xFF76FF03)
            "song_of_storms" -> Color(0xFF81D4FA) // Rain Azure
            "bolero_of_fire" -> if (i % 2 == 0) Color(0xFFFF5252) else Color(0xFFFF9100)
            "serenade_of_water" -> Color(0xFF00E5FF) // Aqua Cyan
            "suns_song" -> Color(0xFFFFEA00) // Solar Yellow
            "song_of_time" -> Color(0xFF40C4FF) // Temple Sapphire
            "requiem_of_spirit" -> Color(0xFFFFAB40) // Desert Topaz
            "nocturne_of_shadow" -> Color(0xFFE040FB) // Shadow Violet
            "prelude_of_light" -> Color(0xFFFFF59D) // Celestial White-Gold
            "zeldas_lullaby" -> Color(0xFFB388FF) // Royal Lilac
            else -> Color(0xFFFFD700)
        }

        list.add(
            SongParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speedY = Random.nextFloat() * 0.08f + 0.03f,
                speedX = (Random.nextFloat() - 0.5f) * 0.03f,
                size = Random.nextFloat() * 12f + 8f,
                shape = shape,
                color = color,
                phase = Random.nextFloat() * (PI.toFloat() * 2f),
                rotationSpeed = (Random.nextFloat() - 0.5f) * 4f
            )
        )
    }
    return list
}

// ---------------- Particle Drawing Engine ----------------

private fun DrawScope.drawSongSpecificParticles(
    particles: List<SongParticle>,
    songId: String,
    w: Float,
    h: Float,
    time: Float,
    alpha: Float,
    accentColor: Color
) {
    if (alpha <= 0.05f) return

    for (p in particles) {
        when (p.shape) {
            // 1. Epona's Song: Musical Notes floating upward in sine waves
            ParticleShape.MUSIC_NOTE_SINGLE -> {
                val progressY = (p.y - (time * p.speedY)) % 1.0f
                val py = if (progressY < 0f) progressY + 1.0f else progressY
                val waveX = sin(time * 2.2f + p.phase) * 32f
                val px = (p.x * w + waveX).mod(w)
                val noteAlpha = (sin(py * PI.toFloat()).coerceIn(0f, 1f)) * 0.75f * alpha

                drawSingleEighthNote(px, py * h, p.size, p.color.copy(alpha = noteAlpha), time * p.rotationSpeed)
            }

            ParticleShape.MUSIC_NOTE_DOUBLE -> {
                val progressY = (p.y - (time * p.speedY * 0.9f)) % 1.0f
                val py = if (progressY < 0f) progressY + 1.0f else progressY
                val waveX = cos(time * 2.0f + p.phase) * 38f
                val px = (p.x * w + waveX).mod(w)
                val noteAlpha = (sin(py * PI.toFloat()).coerceIn(0f, 1f)) * 0.80f * alpha

                drawDoubleEighthNote(px, py * h, p.size * 1.2f, p.color.copy(alpha = noteAlpha), time * p.rotationSpeed)
            }

            // 2. Saria's Song & Minuet of Forest: Kokiri leaves swirling in breeze
            ParticleShape.KOKIRI_LEAF -> {
                val progressY = (p.y - (time * p.speedY * 0.8f)) % 1.0f
                val py = if (progressY < 0f) progressY + 1.0f else progressY
                val waveX = sin(time * 2.5f + p.phase) * 55f + (time * 15f)
                val px = (p.x * w + waveX).mod(w)
                val leafAlpha = (sin(py * PI.toFloat()).coerceIn(0f, 1f)) * 0.85f * alpha
                val tilt = sin(time * 3f + p.phase) // 3D tumbling effect

                drawKokiriLeaf(px, py * h, p.size * 1.1f, p.color.copy(alpha = leafAlpha), tilt)
            }

            // 3. Song of Storms: Diagonal rain streaks
            ParticleShape.RAIN_STREAK -> {
                val rainSpeed = p.speedY * 8f
                val progressY = (p.y + (time * rainSpeed)) % 1.0f
                val py = progressY * h
                val px = ((p.x * w) + (progressY * w * 0.35f)).mod(w) // 20-degree slant
                val rainAlpha = 0.45f * alpha

                drawLine(
                    color = p.color.copy(alpha = rainAlpha),
                    start = Offset(px, py),
                    end = Offset(px + 9f, py + 26f),
                    strokeWidth = 1.6f,
                    cap = StrokeCap.Round
                )
            }

            // 4. Bolero of Fire: Volcanic rising sparks & embers
            ParticleShape.FIRE_EMBER -> {
                val progressY = (p.y - (time * p.speedY * 1.5f)) % 1.0f
                val py = if (progressY < 0f) progressY + 1.0f else progressY
                val wobbleX = sin(time * 4f + p.phase) * 22f
                val px = (p.x * w + wobbleX).mod(w)
                val flicker = (sin(time * 12f + p.phase) * 0.3f + 0.7f).coerceIn(0f, 1f)
                val emberAlpha = (sin(py * PI.toFloat()).coerceIn(0f, 1f)) * flicker * alpha

                drawCircle(
                    color = p.color.copy(alpha = emberAlpha),
                    radius = p.size * 0.28f,
                    center = Offset(px, py * h)
                )
                // Hot core
                drawCircle(
                    color = Color(0xFFFFF9C4).copy(alpha = emberAlpha * 0.8f),
                    radius = p.size * 0.12f,
                    center = Offset(px, py * h)
                )
            }

            // 5. Serenade of Water: Translucent aquatic bubbles
            ParticleShape.WATER_BUBBLE -> {
                val progressY = (p.y - (time * p.speedY * 0.7f)) % 1.0f
                val py = if (progressY < 0f) progressY + 1.0f else progressY
                val wobbleX = sin(time * 1.8f + p.phase) * 26f
                val px = (p.x * w + wobbleX).mod(w)
                val bubbleAlpha = (sin(py * PI.toFloat()).coerceIn(0f, 1f)) * 0.65f * alpha

                drawWaterBubble(px, py * h, p.size * 0.8f, p.color.copy(alpha = bubbleAlpha))
            }

            // 6. Sun's Song: Radiant solar flares
            ParticleShape.SUN_SPARK -> {
                val progressY = (p.y - (time * p.speedY * 0.6f)) % 1.0f
                val py = if (progressY < 0f) progressY + 1.0f else progressY
                val px = (p.x * w + sin(time * 1.5f + p.phase) * 30f).mod(w)
                val sparkAlpha = (sin(py * PI.toFloat()).coerceIn(0f, 1f)) * 0.85f * alpha

                drawSolarSparkle(px, py * h, p.size, p.color.copy(alpha = sparkAlpha), time * 2f + p.phase)
            }

            // 7. Requiem of Spirit: Desert sandstorm particles
            ParticleShape.SAND_GRAIN -> {
                val sandSpeedX = p.speedY * 3.5f
                val px = ((p.x * w) + (time * sandSpeedX * w)).mod(w)
                val py = (p.y * h + sin(time * 3f + p.phase) * 15f).mod(h)
                val sandAlpha = 0.50f * alpha

                drawCircle(
                    color = p.color.copy(alpha = sandAlpha),
                    radius = p.size * 0.18f,
                    center = Offset(px, py)
                )
            }

            // 8. Nocturne of Shadow: Eerie phantom shadow wisps
            ParticleShape.SHADOW_WISP -> {
                val progressY = (p.y - (time * p.speedY * 0.9f)) % 1.0f
                val py = if (progressY < 0f) progressY + 1.0f else progressY
                val wispX = sin(time * 2.8f + p.phase) * 45f
                val px = (p.x * w + wispX).mod(w)
                val wispAlpha = (sin(py * PI.toFloat()).coerceIn(0f, 1f)) * 0.60f * alpha

                drawCircle(
                    color = p.color.copy(alpha = wispAlpha * 0.4f),
                    radius = p.size * 0.8f,
                    center = Offset(px, py * h)
                )
                drawCircle(
                    color = Color.White.copy(alpha = wispAlpha * 0.7f),
                    radius = p.size * 0.22f,
                    center = Offset(px, py * h)
                )
            }

            // 9. Prelude of Light & Zelda's Lullaby: Holy star dust
            ParticleShape.STAR_DUST -> {
                val progressY = (p.y - (time * p.speedY * 0.7f)) % 1.0f
                val py = if (progressY < 0f) progressY + 1.0f else progressY
                val px = (p.x * w + sin(time * 1.8f + p.phase) * 20f).mod(w)
                val starAlpha = (sin(py * PI.toFloat()).coerceIn(0f, 1f)) * 0.85f * alpha

                drawSolarSparkle(px, py * h, p.size * 0.7f, p.color.copy(alpha = starAlpha), time * 3f + p.phase)
            }
        }
    }
}

// ---------------- Custom Glyph Vector Renderers ----------------

/**
 * Draws an authentic musical eighth note (♪).
 */
private fun DrawScope.drawSingleEighthNote(
    x: Float,
    y: Float,
    size: Float,
    color: Color,
    rotationDeg: Float
) {
    rotate(degrees = rotationDeg, pivot = Offset(x, y)) {
        // Oval note head tilted at ~25 degrees
        val headW = size * 0.65f
        val headH = size * 0.45f
        drawOval(
            color = color,
            topLeft = Offset(x - (headW / 2f), y - (headH / 2f)),
            size = Size(headW, headH)
        )
        // Vertical stem
        val stemX = x + (headW / 2f) - 1f
        val stemTopY = y - (size * 1.2f)
        drawLine(
            color = color,
            start = Offset(stemX, y),
            end = Offset(stemX, stemTopY),
            strokeWidth = 1.8f
        )
        // Curved note flag
        val flagPath = Path().apply {
            moveTo(stemX, stemTopY)
            cubicTo(
                stemX + (size * 0.6f), stemTopY + (size * 0.3f),
                stemX + (size * 0.5f), stemTopY + (size * 0.8f),
                stemX + (size * 0.8f), stemTopY + (size * 0.9f)
            )
        }
        drawPath(flagPath, color, style = Stroke(width = 2.0f, cap = StrokeCap.Round))
    }
}

/**
 * Draws an authentic beamed double musical eighth note (♫).
 */
private fun DrawScope.drawDoubleEighthNote(
    x: Float,
    y: Float,
    size: Float,
    color: Color,
    rotationDeg: Float
) {
    rotate(degrees = rotationDeg, pivot = Offset(x, y)) {
        val headW = size * 0.45f
        val headH = size * 0.32f
        val noteSpacing = size * 0.75f
        val stemLen = size * 1.1f

        // Head 1
        drawOval(
            color = color,
            topLeft = Offset(x - noteSpacing / 2f - headW / 2f, y),
            size = Size(headW, headH)
        )
        // Head 2
        drawOval(
            color = color,
            topLeft = Offset(x + noteSpacing / 2f - headW / 2f, y - (size * 0.15f)),
            size = Size(headW, headH)
        )

        // Stem 1 & 2
        val s1X = x - noteSpacing / 2f + headW / 2f - 1f
        val s2X = x + noteSpacing / 2f + headW / 2f - 1f
        drawLine(color, Offset(s1X, y), Offset(s1X, y - stemLen), strokeWidth = 1.6f)
        drawLine(color, Offset(s2X, y - (size * 0.15f)), Offset(s2X, y - stemLen - (size * 0.15f)), strokeWidth = 1.6f)

        // Crossbeam at top
        drawLine(
            color = color,
            start = Offset(s1X, y - stemLen),
            end = Offset(s2X, y - stemLen - (size * 0.15f)),
            strokeWidth = 3.0f,
            cap = StrokeCap.Square
        )
    }
}

/**
 * Draws a Kokiri forest leaf with 3D wind tilt.
 */
private fun DrawScope.drawKokiriLeaf(
    x: Float,
    y: Float,
    size: Float,
    color: Color,
    tiltFactor: Float
) {
    val leafW = size * 0.8f * abs(tiltFactor).coerceAtLeast(0.15f)
    val leafH = size * 1.3f

    val leafPath = Path().apply {
        moveTo(x, y - (leafH / 2f)) // Tip
        cubicTo(x + (leafW * 0.8f), y - (leafH * 0.15f), x + (leafW * 0.7f), y + (leafH * 0.35f), x, y + (leafH / 2f)) // Right curve to stem
        cubicTo(x - (leafW * 0.7f), y + (leafH * 0.35f), x - (leafW * 0.8f), y - (leafH * 0.15f), x, y - (leafH / 2f)) // Left curve
        close()
    }
    drawPath(leafPath, color, style = Fill)
    // Central leaf vein
    drawLine(
        color = Color(0xFFFFF9C4).copy(alpha = color.alpha * 0.8f),
        start = Offset(x, y - (leafH * 0.45f)),
        end = Offset(x, y + (leafH * 0.45f)),
        strokeWidth = 1.0f
    )
}

/**
 * Draws a translucent aquatic water bubble with specular highlight.
 */
private fun DrawScope.drawWaterBubble(
    x: Float,
    y: Float,
    radius: Float,
    color: Color
) {
    // Bubble perimeter ring
    drawCircle(color = color, radius = radius, center = Offset(x, y), style = Stroke(width = 1.5f))
    // Soft transparent interior
    drawCircle(color = color.copy(alpha = color.alpha * 0.25f), radius = radius, center = Offset(x, y))
    // Top-left curved specular shine
    drawCircle(
        color = Color.White.copy(alpha = color.alpha * 0.85f),
        radius = radius * 0.25f,
        center = Offset(x - (radius * 0.38f), y - (radius * 0.38f))
    )
}

/**
 * Draws a four-pointed radiant solar sparkle star.
 */
private fun DrawScope.drawSolarSparkle(
    x: Float,
    y: Float,
    size: Float,
    color: Color,
    rotationDeg: Float
) {
    rotate(degrees = rotationDeg, pivot = Offset(x, y)) {
        val path = Path().apply {
            moveTo(x, y - size)
            cubicTo(x + (size * 0.15f), y - (size * 0.15f), x + (size * 0.15f), y - (size * 0.15f), x + size, y)
            cubicTo(x + (size * 0.15f), y + (size * 0.15f), x + (size * 0.15f), y + (size * 0.15f), x, y + size)
            cubicTo(x - (size * 0.15f), y + (size * 0.15f), x - (size * 0.15f), y + (size * 0.15f), x - size, y)
            cubicTo(x - (size * 0.15f), y - (size * 0.15f), x - (size * 0.15f), y - (size * 0.15f), x, y - size)
            close()
        }
        drawPath(path, color, style = Fill)
    }
}

/**
 * Draws a jagged branched lightning bolt for Song of Storms.
 */
private fun DrawScope.drawLightningBolt(w: Float, h: Float, alpha: Float) {
    val lightningColor = Color(0xFFE1F5FE).copy(alpha = alpha)
    val path = Path().apply {
        var lx = w * 0.65f
        var ly = 0f
        moveTo(lx, ly)
        val segments = 7
        val stepY = h * 0.75f / segments
        for (i in 0 until segments) {
            ly += stepY
            lx += (Random.nextFloat() - 0.5f) * 75f
            lineTo(lx, ly)
        }
    }
    drawPath(path, lightningColor, style = Stroke(width = 3.5f, cap = StrokeCap.Round))
    drawPath(path, Color.White.copy(alpha = alpha), style = Stroke(width = 1.5f, cap = StrokeCap.Round))
}
