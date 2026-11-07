package com.f8scape.ocarina

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Museum-grade Zelda Medallions & Spiritual Emblems rendered directly in vector Canvas.
 * Ultra-sharp at any display density on Dynamic AMOLED 2X.
 */
@Composable
fun ZeldaMedallionEmblem(
    songId: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w / 2f, h / 2f)
            val r = (w / 2f) - 1.5f

            // 1. Ornate Golden Outer Medallion Bezel with Metallic Luster
            val goldBezelBrush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFFFD54F),
                    Color(0xFFFFF9C4),
                    Color(0xFFFFB300),
                    Color(0xFFFFE082),
                    Color(0xFFFF8F00),
                    Color(0xFFFFD54F)
                ),
                center = center
            )
            drawCircle(brush = goldBezelBrush, radius = r, center = center)

            // Inner dark gemstone chamber
            val innerR = r * 0.82f
            val gemBackdrop = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.55f),
                    Color(0xFF0A0E18),
                    Color(0xFF04060C)
                ),
                center = Offset(w * 0.40f, h * 0.38f),
                radius = innerR * 1.2f
            )
            drawCircle(brush = gemBackdrop, radius = innerR, center = center)

            // Gold inner rim wire
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = 0.8f),
                radius = innerR,
                center = center,
                style = Stroke(width = 1.2f)
            )

            // 2. Specific Sacred Emblem in the Core
            when (songId) {
                // 1. Forest Medallion / Minuet of Forest
                "minuet_of_forest" -> drawForestMedallion(center, innerR)

                // 2. Fire Medallion / Bolero of Fire
                "bolero_of_fire" -> drawFireMedallion(center, innerR)

                // 3. Water Medallion / Serenade of Water
                "serenade_of_water" -> drawWaterMedallion(center, innerR)

                // 4. Spirit Medallion / Requiem of Spirit
                "requiem_of_spirit" -> drawSpiritMedallion(center, innerR)

                // 5. Shadow Medallion / Nocturne of Shadow
                "nocturne_of_shadow" -> drawShadowMedallion(center, innerR)

                // 6. Light Medallion / Prelude of Light
                "prelude_of_light" -> drawLightMedallion(center, innerR)

                // 7. Zelda's Lullaby (Royal Crest of Hyrule & Loftwing)
                "zeldas_lullaby" -> drawRoyalCrest(center, innerR)

                // 8. Epona's Song (Lon Lon Ranch Horseshoe & Steed)
                "eponas_song" -> drawLonLonHorseshoe(center, innerR)

                // 9. Saria's Song (Kokiri's Emerald Spiritual Stone)
                "sarias_song" -> drawKokiriEmerald(center, innerR)

                // 10. Sun's Song (Sun & Crescent Moon)
                "suns_song" -> drawSunAndMoon(center, innerR)

                // 11. Song of Time (Ocarina of Time)
                "song_of_time" -> drawOcarinaOfTime(center, innerR)

                // 12. Song of Storms (Tempest Cyclone & Lightning)
                "song_of_storms" -> drawSongOfStorms(center, innerR)

                else -> drawTriforceCore(center, innerR)
            }

            // Top specular shine
            val specularBrush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.40f), Color.Transparent),
                center = Offset(w * 0.32f, h * 0.28f),
                radius = r * 0.7f
            )
            drawCircle(brush = specularBrush, radius = innerR * 0.9f, center = center)
        }
    }
}

// ---------------- Emblems implementation ----------------

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawForestMedallion(center: Offset, r: Float) {
    val emerald = Color(0xFF00E676)
    val gold = Color(0xFFFFD54F)
    // 6 outer radial points
    for (i in 0 until 6) {
        val angle = (i * 60.0) * (PI / 180.0)
        val p1 = Offset(center.x + (r * 0.85f * cos(angle)).toFloat(), center.y + (r * 0.85f * sin(angle)).toFloat())
        val p2 = Offset(center.x + (r * 0.52f * cos(angle)).toFloat(), center.y + (r * 0.52f * sin(angle)).toFloat())
        drawLine(gold, p1, p2, strokeWidth = 1.5f, cap = StrokeCap.Round)
    }
    // Forest leaf spiral
    val path = Path().apply {
        moveTo(center.x, center.y - (r * 0.55f))
        cubicTo(center.x + (r * 0.6f), center.y - (r * 0.2f), center.x + (r * 0.4f), center.y + (r * 0.55f), center.x, center.y + (r * 0.3f))
        cubicTo(center.x - (r * 0.4f), center.y + (r * 0.55f), center.x - (r * 0.6f), center.y - (r * 0.2f), center.x, center.y - (r * 0.55f))
        close()
    }
    drawPath(path, emerald, style = Fill)
    drawCircle(gold, radius = r * 0.18f, center = center)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFireMedallion(center: Offset, r: Float) {
    val flameRed = Color(0xFFFF1744)
    val flameGold = Color(0xFFFF9100)
    // Goron Flame emblem
    val path = Path().apply {
        moveTo(center.x, center.y - (r * 0.65f))
        cubicTo(center.x + (r * 0.2f), center.y - (r * 0.2f), center.x + (r * 0.6f), center.y - (r * 0.1f), center.x + (r * 0.45f), center.y + (r * 0.45f))
        cubicTo(center.x + (r * 0.2f), center.y + (r * 0.65f), center.x - (r * 0.2f), center.y + (r * 0.65f), center.x - (r * 0.45f), center.y + (r * 0.45f))
        cubicTo(center.x - (r * 0.6f), center.y - (r * 0.1f), center.x - (r * 0.2f), center.y - (r * 0.2f), center.x, center.y - (r * 0.65f))
        close()
    }
    drawPath(path, flameRed, style = Fill)
    // Inner golden flame core
    val corePath = Path().apply {
        moveTo(center.x, center.y - (r * 0.38f))
        cubicTo(center.x + (r * 0.25f), center.y, center.x + (r * 0.15f), center.y + (r * 0.35f), center.x, center.y + (r * 0.35f))
        cubicTo(center.x - (r * 0.15f), center.y + (r * 0.35f), center.x - (r * 0.25f), center.y, center.x, center.y - (r * 0.38f))
        close()
    }
    drawPath(corePath, flameGold, style = Fill)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWaterMedallion(center: Offset, r: Float) {
    val waterBlue = Color(0xFF00B0FF)
    val rippleCyan = Color(0xFF80D8FF)
    // 3 water ripple rings
    drawCircle(waterBlue.copy(alpha = 0.3f), radius = r * 0.72f, center = center, style = Stroke(width = 1.5f))
    drawCircle(waterBlue.copy(alpha = 0.6f), radius = r * 0.48f, center = center, style = Stroke(width = 2f))
    // Center water droplet
    val dropPath = Path().apply {
        moveTo(center.x, center.y - (r * 0.52f))
        cubicTo(center.x + (r * 0.42f), center.y, center.x + (r * 0.35f), center.y + (r * 0.42f), center.x, center.y + (r * 0.42f))
        cubicTo(center.x - (r * 0.35f), center.y + (r * 0.42f), center.x - (r * 0.42f), center.y, center.x, center.y - (r * 0.52f))
        close()
    }
    drawPath(dropPath, rippleCyan, style = Fill)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpiritMedallion(center: Offset, r: Float) {
    val topaz = Color(0xFFFF9100)
    val gold = Color(0xFFFFD54F)
    // Desert mirror oval & twin cobra crest
    drawOval(color = topaz, topLeft = Offset(center.x - (r * 0.35f), center.y - (r * 0.55f)), size = androidx.compose.ui.geometry.Size(r * 0.7f, r * 1.1f), style = Stroke(width = 2f))
    drawCircle(gold, radius = r * 0.22f, center = center)
    drawLine(gold, Offset(center.x - (r * 0.6f), center.y), Offset(center.x + (r * 0.6f), center.y), strokeWidth = 2f, cap = StrokeCap.Round)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawShadowMedallion(center: Offset, r: Float) {
    val amethyst = Color(0xFFE040FB)
    val white = Color.White
    // Sheikah Eye of Truth
    val eyePath = Path().apply {
        moveTo(center.x - (r * 0.6f), center.y - (r * 0.05f))
        cubicTo(center.x - (r * 0.2f), center.y - (r * 0.42f), center.x + (r * 0.2f), center.y - (r * 0.42f), center.x + (r * 0.6f), center.y - (r * 0.05f))
        cubicTo(center.x + (r * 0.2f), center.y + (r * 0.32f), center.x - (r * 0.2f), center.y + (r * 0.32f), center.x - (r * 0.6f), center.y - (r * 0.05f))
        close()
    }
    drawPath(eyePath, white, style = Stroke(width = 1.8f))
    drawCircle(amethyst, radius = r * 0.22f, center = Offset(center.x, center.y - (r * 0.05f)))
    // Sheikah Tear
    val tearPath = Path().apply {
        moveTo(center.x, center.y + (r * 0.15f))
        lineTo(center.x + (r * 0.12f), center.y + (r * 0.42f))
        cubicTo(center.x + (r * 0.12f), center.y + (r * 0.6f), center.x - (r * 0.12f), center.y + (r * 0.6f), center.x - (r * 0.12f), center.y + (r * 0.42f))
        close()
    }
    drawPath(tearPath, amethyst, style = Fill)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLightMedallion(center: Offset, r: Float) {
    val gold = Color(0xFFFFD700)
    // 6 celestial light beams
    for (i in 0 until 6) {
        val angle = (i * 60.0) * (PI / 180.0)
        val p1 = Offset(center.x, center.y)
        val p2 = Offset(center.x + (r * 0.85f * cos(angle)).toFloat(), center.y + (r * 0.85f * sin(angle)).toFloat())
        drawLine(gold.copy(alpha = 0.7f), p1, p2, strokeWidth = 2f, cap = StrokeCap.Round)
    }
    drawTriforceCore(center, r * 0.75f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoyalCrest(center: Offset, r: Float) {
    val gold = Color(0xFFFFD700)
    // Triforce at top
    drawTriforceCore(Offset(center.x, center.y - (r * 0.28f)), r * 0.45f)
    // Loftwing spread wings
    val wingPath = Path().apply {
        moveTo(center.x, center.y + (r * 0.45f))
        cubicTo(center.x + (r * 0.3f), center.y + (r * 0.35f), center.x + (r * 0.65f), center.y, center.x + (r * 0.68f), center.y - (r * 0.2f))
        cubicTo(center.x + (r * 0.45f), center.y + (r * 0.05f), center.x + (r * 0.25f), center.y + (r * 0.15f), center.x, center.y + (r * 0.25f))
        cubicTo(center.x - (r * 0.25f), center.y + (r * 0.15f), center.x - (r * 0.45f), center.y + (r * 0.05f), center.x - (r * 0.68f), center.y - (r * 0.2f))
        cubicTo(center.x - (r * 0.65f), center.y, center.x - (r * 0.3f), center.y + (r * 0.35f), center.x, center.y + (r * 0.45f))
        close()
    }
    drawPath(wingPath, gold, style = Fill)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLonLonHorseshoe(center: Offset, r: Float) {
    val gold = Color(0xFFFFB300)
    val amber = Color(0xFFFFE082)
    // Horseshoe
    drawArc(
        color = gold,
        startAngle = 140f,
        sweepAngle = 260f,
        useCenter = false,
        topLeft = Offset(center.x - (r * 0.58f), center.y - (r * 0.52f)),
        size = androidx.compose.ui.geometry.Size(r * 1.16f, r * 1.16f),
        style = Stroke(width = r * 0.22f, cap = StrokeCap.Round)
    )
    // Galloping Steed star
    drawCircle(amber, radius = r * 0.18f, center = Offset(center.x, center.y - (r * 0.05f)))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawKokiriEmerald(center: Offset, r: Float) {
    val emerald = Color(0xFF00E676)
    val gold = Color(0xFFFFD54F)
    // Three swirling spheres forming the Spiritual Stone of the Forest
    val dist = r * 0.36f
    for (i in 0 until 3) {
        val angle = (i * 120.0 - 90.0) * (PI / 180.0)
        val cx = center.x + (dist * cos(angle)).toFloat()
        val cy = center.y + (dist * sin(angle)).toFloat()
        drawCircle(emerald, radius = r * 0.26f, center = Offset(cx, cy))
        drawCircle(gold, radius = r * 0.26f, center = Offset(cx, cy), style = Stroke(width = 1.5f))
    }
    drawCircle(gold, radius = r * 0.12f, center = center)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSunAndMoon(center: Offset, r: Float) {
    val sunGold = Color(0xFFFFC107)
    val moonSilver = Color(0xFFE0E0E0)
    // Sun rays
    for (i in 0 until 8) {
        val angle = (i * 45.0) * (PI / 180.0)
        val p1 = Offset(center.x + (r * 0.42f * cos(angle)).toFloat(), center.y + (r * 0.42f * sin(angle)).toFloat())
        val p2 = Offset(center.x + (r * 0.65f * cos(angle)).toFloat(), center.y + (r * 0.65f * sin(angle)).toFloat())
        drawLine(sunGold, p1, p2, strokeWidth = 2f, cap = StrokeCap.Round)
    }
    drawCircle(sunGold, radius = r * 0.32f, center = center)
    // Crescent Moon
    val moonPath = Path().apply {
        val cr = r * 0.34f
        addOval(Rect(center.x - cr, center.y - cr, center.x + cr, center.y + cr))
    }
    drawPath(moonPath, moonSilver.copy(alpha = 0.6f), style = Stroke(width = 2.5f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOcarinaOfTime(center: Offset, r: Float) {
    val ceramicBlue = Color(0xFF1E88E5)
    val silver = Color(0xFFE0E0E0)
    // Ceramic chamber body
    val bodyPath = Path().apply {
        moveTo(center.x - (r * 0.55f), center.y)
        cubicTo(center.x - (r * 0.4f), center.y - (r * 0.45f), center.x + (r * 0.45f), center.y - (r * 0.35f), center.x + (r * 0.62f), center.y)
        cubicTo(center.x + (r * 0.45f), center.y + (r * 0.45f), center.x - (r * 0.3f), center.y + (r * 0.35f), center.x - (r * 0.55f), center.y)
        close()
    }
    drawPath(bodyPath, ceramicBlue, style = Fill)
    // Silver band
    drawLine(silver, Offset(center.x - (r * 0.2f), center.y - (r * 0.35f)), Offset(center.x - (r * 0.2f), center.y + (r * 0.32f)), strokeWidth = 3f)
    // Finger holes
    val holes = listOf(Offset(center.x + (r * 0.1f), center.y - (r * 0.1f)), Offset(center.x + (r * 0.28f), center.y - (r * 0.12f)), Offset(center.x + (r * 0.14f), center.y + (r * 0.12f)), Offset(center.x + (r * 0.32f), center.y + (r * 0.1f)))
    holes.forEach { drawCircle(Color(0xFF0D47A1), radius = r * 0.07f, center = it) }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSongOfStorms(center: Offset, r: Float) {
    val stormCyan = Color(0xFF00E5FF)
    val lightning = Color(0xFFFFEA00)
    // Swirling vortex circles
    drawArc(stormCyan, startAngle = 0f, sweepAngle = 240f, useCenter = false, topLeft = Offset(center.x - (r * 0.55f), center.y - (r * 0.55f)), size = androidx.compose.ui.geometry.Size(r * 1.1f, r * 1.1f), style = Stroke(width = 2f, cap = StrokeCap.Round))
    // Lightning bolt
    val bolt = Path().apply {
        moveTo(center.x + (r * 0.08f), center.y - (r * 0.5f))
        lineTo(center.x - (r * 0.18f), center.y + (r * 0.02f))
        lineTo(center.x + (r * 0.05f), center.y + (r * 0.02f))
        lineTo(center.x - (r * 0.12f), center.y + (r * 0.52f))
        lineTo(center.x + (r * 0.22f), center.y - (r * 0.06f))
        lineTo(center.x, center.y - (r * 0.06f))
        close()
    }
    drawPath(bolt, lightning, style = Fill)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTriforceCore(center: Offset, r: Float) {
    val gold = Color(0xFFFFD700)
    val h = r * 0.85f
    val w = h * 1.15f
    val top = Offset(center.x, center.y - (h * 0.5f))
    val botLeft = Offset(center.x - (w * 0.5f), center.y + (h * 0.5f))
    val botRight = Offset(center.x + (w * 0.5f), center.y + (h * 0.5f))
    val midLeft = Offset((top.x + botLeft.x) / 2f, (top.y + botLeft.y) / 2f)
    val midRight = Offset((top.x + botRight.x) / 2f, (top.y + botRight.y) / 2f)
    val midBot = Offset((botLeft.x + botRight.x) / 2f, (botLeft.y + botRight.y) / 2f)

    // Top triangle
    val t1 = Path().apply { moveTo(top.x, top.y); lineTo(midRight.x, midRight.y); lineTo(midLeft.x, midLeft.y); close() }
    // Bottom left triangle
    val t2 = Path().apply { moveTo(midLeft.x, midLeft.y); lineTo(midBot.x, midBot.y); lineTo(botLeft.x, botLeft.y); close() }
    // Bottom right triangle
    val t3 = Path().apply { moveTo(midRight.x, midRight.y); lineTo(botRight.x, botRight.y); lineTo(midBot.x, midBot.y); close() }

    drawPath(t1, gold, style = Fill)
    drawPath(t2, gold, style = Fill)
    drawPath(t3, gold, style = Fill)
}
