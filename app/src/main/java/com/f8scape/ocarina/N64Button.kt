package com.f8scape.ocarina

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Nintendo Switch 2 Controller Face Button Badge.
 * Features the signature Nintendo Switch 2 matte obsidian/graphite concave disc,
 * crisp engraved Switch letter glyphs (A, B, X, Y, L/R), subtle edge chamfers, and glowing active rings.
 */
@Composable
fun Switch2ButtonBadge(
    button: OcarinaButton? = null,
    customLabel: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    isActive: Boolean = false
) {
    val displayLetter = customLabel ?: when (button) {
        OcarinaButton.A -> "A"
        OcarinaButton.C_DOWN -> "B"
        OcarinaButton.C_LEFT -> "Y"
        OcarinaButton.C_UP -> "X"
        OcarinaButton.C_RIGHT -> "R"
        null -> "A"
    }

    val accentColor = when (displayLetter) {
        "A" -> Color(0xFFEF4444)     // Switch Red (A)
        "B" -> Color(0xFFF59E0B)     // Switch Yellow (B)
        "X" -> Color(0xFF3B82F6)     // Switch Blue (X)
        "Y" -> Color(0xFF10B981)     // Switch Green (Y)
        "L" -> Color(0xFF06B6D4)     // Switch Cyan (L)
        "R" -> Color(0xFFA855F7)     // Switch Purple (R)
        else -> Color(0xFFFFD700)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w / 2f, h / 2f)
            val radius = (w / 2f) - 1.5f

            // Switch 2 Modern Matte Dark Disc with subtle top-down lighting
            val discBrush = Brush.radialGradient(
                colors = if (isActive) {
                    listOf(Color(0xFF374151), Color(0xFF1F2937), Color(0xFF111827))
                } else {
                    listOf(Color(0xFF2B3340), Color(0xFF1B212D), Color(0xFF0F131C))
                },
                center = Offset(w * 0.40f, h * 0.35f),
                radius = radius * 1.2f
            )
            drawCircle(brush = discBrush, radius = radius, center = center)

            // Switch 2 Precision Chamfer Outer Rim
            drawCircle(
                color = if (isActive) Color(0xFF60A5FA).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.22f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.2f)
            )

            // Distinctive Nintendo Switch 2 Accent Ring
            drawCircle(
                color = accentColor.copy(alpha = if (isActive) 0.95f else 0.45f),
                radius = radius * 0.78f,
                center = center,
                style = Stroke(width = 1.4f)
            )

            // Draw crisp, proportional typographic letters
            val glyphH = h * 0.48f
            val glyphW = w * 0.38f
            val topY = (h - glyphH) / 2f
            val botY = topY + glyphH
            val leftX = (w - glyphW) / 2f
            val rightX = leftX + glyphW
            val midX = w / 2f

            when (displayLetter) {
                "A" -> {
                    val aPath = Path().apply {
                        moveTo(midX, topY)
                        lineTo(rightX, botY)
                        lineTo(rightX - (glyphW * 0.22f), botY)
                        lineTo(midX + (glyphW * 0.12f), botY - (glyphH * 0.38f))
                        lineTo(midX - (glyphW * 0.12f), botY - (glyphH * 0.38f))
                        lineTo(leftX + (glyphW * 0.22f), botY)
                        lineTo(leftX, botY)
                        close()
                    }
                    drawPath(path = aPath, color = Color.White, style = Fill)
                    val holePath = Path().apply {
                        moveTo(midX, topY + (glyphH * 0.22f))
                        lineTo(midX + (glyphW * 0.14f), botY - (glyphH * 0.50f))
                        lineTo(midX - (glyphW * 0.14f), botY - (glyphH * 0.50f))
                        close()
                    }
                    drawPath(path = holePath, color = Color(0xFF1B212D), style = Fill)
                }
                "B" -> {
                    // Proper, clean geometric B with a solid vertical spine and two distinct rounded loops
                    val spineW = glyphW * 0.22f
                    val loopW = glyphW * 0.78f
                    val midY = center.y

                    // Vertical left spine
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(leftX, topY),
                        size = androidx.compose.ui.geometry.Size(spineW, glyphH)
                    )

                    // Top loop
                    val topLoopPath = Path().apply {
                        moveTo(leftX + spineW - 0.5f, topY)
                        lineTo(leftX + spineW + loopW * 0.55f, topY)
                        cubicTo(rightX, topY, rightX, midY + 0.5f, leftX + spineW + loopW * 0.45f, midY + 0.5f)
                        lineTo(leftX + spineW - 0.5f, midY + 0.5f)
                        close()
                    }
                    drawPath(path = topLoopPath, color = Color.White, style = Fill)

                    // Top inner cutout
                    val topCutout = Path().apply {
                        val inLeft = leftX + spineW
                        val inTop = topY + glyphH * 0.11f
                        val inBot = midY - glyphH * 0.05f
                        moveTo(inLeft, inTop)
                        lineTo(inLeft + loopW * 0.32f, inTop)
                        cubicTo(rightX - spineW, inTop, rightX - spineW, inBot, inLeft + loopW * 0.32f, inBot)
                        lineTo(inLeft, inBot)
                        close()
                    }
                    drawPath(path = topCutout, color = Color(0xFF1B212D), style = Fill)

                    // Bottom loop (slightly wider, standard typographic B)
                    val botLoopPath = Path().apply {
                        moveTo(leftX + spineW - 0.5f, midY - 0.5f)
                        lineTo(leftX + spineW + loopW * 0.60f, midY - 0.5f)
                        cubicTo(rightX + glyphW * 0.06f, midY - 0.5f, rightX + glyphW * 0.06f, botY, leftX + spineW + loopW * 0.50f, botY)
                        lineTo(leftX + spineW - 0.5f, botY)
                        close()
                    }
                    drawPath(path = botLoopPath, color = Color.White, style = Fill)

                    // Bottom inner cutout
                    val botCutout = Path().apply {
                        val inLeft = leftX + spineW
                        val inTop = midY + glyphH * 0.05f
                        val inBot = botY - glyphH * 0.11f
                        moveTo(inLeft, inTop)
                        lineTo(inLeft + loopW * 0.35f, inTop)
                        cubicTo(rightX - spineW + 1f, inTop, rightX - spineW + 1f, inBot, inLeft + loopW * 0.35f, inBot)
                        lineTo(inLeft, inBot)
                        close()
                    }
                    drawPath(path = botCutout, color = Color(0xFF1B212D), style = Fill)
                }
                "X" -> {
                    val strokeW = glyphW * 0.26f
                    drawLine(color = Color.White, start = Offset(leftX, topY), end = Offset(rightX, botY), strokeWidth = strokeW, cap = StrokeCap.Round)
                    drawLine(color = Color.White, start = Offset(rightX, topY), end = Offset(leftX, botY), strokeWidth = strokeW, cap = StrokeCap.Round)
                }
                "Y" -> {
                    val strokeW = glyphW * 0.26f
                    drawLine(color = Color.White, start = Offset(leftX, topY), end = Offset(midX, center.y + 1f), strokeWidth = strokeW, cap = StrokeCap.Round)
                    drawLine(color = Color.White, start = Offset(rightX, topY), end = Offset(midX, center.y + 1f), strokeWidth = strokeW, cap = StrokeCap.Round)
                    drawLine(color = Color.White, start = Offset(midX, center.y), end = Offset(midX, botY), strokeWidth = strokeW, cap = StrokeCap.Round)
                }
                "L" -> {
                    val strokeW = glyphW * 0.24f
                    // Left vertical leg
                    drawLine(color = Color.White, start = Offset(leftX + strokeW * 0.5f, topY), end = Offset(leftX + strokeW * 0.5f, botY), strokeWidth = strokeW, cap = StrokeCap.Square)
                    // Bottom horizontal bar
                    drawLine(color = Color.White, start = Offset(leftX, botY - strokeW * 0.5f), end = Offset(rightX - 1f, botY - strokeW * 0.5f), strokeWidth = strokeW, cap = StrokeCap.Square)
                }
                "R" -> {
                    val spineW = glyphW * 0.22f
                    val loopW = glyphW * 0.78f
                    val midY = center.y + 1f

                    // Vertical left spine
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(leftX, topY),
                        size = androidx.compose.ui.geometry.Size(spineW, glyphH)
                    )

                    // Top loop
                    val rLoop = Path().apply {
                        moveTo(leftX + spineW - 0.5f, topY)
                        lineTo(leftX + spineW + loopW * 0.55f, topY)
                        cubicTo(rightX, topY, rightX, midY, leftX + spineW + loopW * 0.45f, midY)
                        lineTo(leftX + spineW - 0.5f, midY)
                        close()
                    }
                    drawPath(rLoop, color = Color.White, style = Fill)

                    // Top inner cutout
                    val rCutout = Path().apply {
                        val inLeft = leftX + spineW
                        val inTop = topY + glyphH * 0.10f
                        val inBot = midY - glyphH * 0.08f
                        moveTo(inLeft, inTop)
                        lineTo(inLeft + loopW * 0.30f, inTop)
                        cubicTo(rightX - spineW, inTop, rightX - spineW, inBot, inLeft + loopW * 0.30f, inBot)
                        lineTo(inLeft, inBot)
                        close()
                    }
                    drawPath(rCutout, color = Color(0xFF1B212D), style = Fill)

                    // Diagonal right leg
                    val legStroke = glyphW * 0.24f
                    drawLine(
                        color = Color.White,
                        start = Offset(leftX + spineW * 0.8f, midY - 2f),
                        end = Offset(rightX, botY),
                        strokeWidth = legStroke,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Glow aura when pressed/active
            if (isActive) {
                drawCircle(
                    color = accentColor,
                    radius = radius + 2.5f,
                    center = center,
                    style = Stroke(width = 2.2f)
                )
            }
        }
    }
}

/**
 * Pixel-perfect Nintendo 64 controller buttons.
 * Renders convex 3D button disc with specular edge highlights and
 * mathematically centered geometric polygon arrows/A glyphs.
 */
@Composable
fun N64ButtonBadge(
    button: OcarinaButton,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    isActive: Boolean = false
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w / 2f, h / 2f)
            val radius = (w / 2f) - 1.5f

            if (button == OcarinaButton.A) {
                // 3D Blue A Button with radial sheen
                val baseBrush = Brush.radialGradient(
                    colors = if (isActive) {
                        listOf(Color(0xFF82B1FF), Color(0xFF2979FF), Color(0xFF0D47A1))
                    } else {
                        listOf(Color(0xFF448AFF), Color(0xFF1E88E5), Color(0xFF0D47A1))
                    },
                    center = Offset(w * 0.38f, h * 0.35f),
                    radius = radius * 1.3f
                )
                drawCircle(brush = baseBrush, radius = radius, center = center)

                // Bevel ring: light top-left, dark bottom-right
                drawCircle(
                    color = Color.White.copy(alpha = 0.55f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.2f)
                )

                // Draw bold centered 'A' glyph using geometric paths for zero font-padding error
                val glyphH = h * 0.52f
                val glyphW = w * 0.44f
                val topY = (h - glyphH) / 2f
                val botY = topY + glyphH
                val leftX = (w - glyphW) / 2f
                val rightX = leftX + glyphW
                val midX = w / 2f

                // Outer legs
                val aPath = Path().apply {
                    moveTo(midX, topY)
                    lineTo(rightX, botY)
                    lineTo(rightX - (glyphW * 0.22f), botY)
                    lineTo(midX + (glyphW * 0.12f), botY - (glyphH * 0.40f))
                    lineTo(midX - (glyphW * 0.12f), botY - (glyphH * 0.40f))
                    lineTo(leftX + (glyphW * 0.22f), botY)
                    lineTo(leftX, botY)
                    close()
                }
                drawPath(path = aPath, color = Color.White, style = Fill)

                // Inner triangle hole of the A
                val holePath = Path().apply {
                    val holeTop = topY + (glyphH * 0.22f)
                    val holeBot = botY - (glyphH * 0.52f)
                    moveTo(midX, holeTop)
                    lineTo(midX + (glyphW * 0.14f), holeBot)
                    lineTo(midX - (glyphW * 0.14f), holeBot)
                    close()
                }
                drawPath(path = holePath, color = Color(0xFF1565C0), style = Fill)
            } else {
                // 3D Yellow C Button with warm metallic golden sheen
                val baseBrush = Brush.radialGradient(
                    colors = if (isActive) {
                        listOf(Color(0xFFFFF9C4), Color(0xFFFFD54F), Color(0xFFF57F17))
                    } else {
                        listOf(Color(0xFFFFEE58), Color(0xFFFFCA28), Color(0xFFE65100))
                    },
                    center = Offset(w * 0.35f, h * 0.35f),
                    radius = radius * 1.3f
                )
                drawCircle(brush = baseBrush, radius = radius, center = center)

                // Bevel ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.65f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.2f)
                )

                // Geometric Equilateral Triangle strictly centered at (center.x, center.y)
                val arrowR = radius * 0.44f
                val arrowPath = Path()
                val cx = center.x
                val cy = center.y

                when (button) {
                    OcarinaButton.C_UP -> {
                        arrowPath.moveTo(cx, cy - arrowR)
                        arrowPath.lineTo(cx + (arrowR * 0.866f), cy + (arrowR * 0.55f))
                        arrowPath.lineTo(cx - (arrowR * 0.866f), cy + (arrowR * 0.55f))
                        arrowPath.close()
                    }
                    OcarinaButton.C_DOWN -> {
                        arrowPath.moveTo(cx, cy + arrowR)
                        arrowPath.lineTo(cx + (arrowR * 0.866f), cy - (arrowR * 0.55f))
                        arrowPath.lineTo(cx - (arrowR * 0.866f), cy - (arrowR * 0.55f))
                        arrowPath.close()
                    }
                    OcarinaButton.C_LEFT -> {
                        arrowPath.moveTo(cx - arrowR, cy)
                        arrowPath.lineTo(cx + (arrowR * 0.55f), cy - (arrowR * 0.866f))
                        arrowPath.lineTo(cx + (arrowR * 0.55f), cy + (arrowR * 0.866f))
                        arrowPath.close()
                    }
                    OcarinaButton.C_RIGHT -> {
                        arrowPath.moveTo(cx + arrowR, cy)
                        arrowPath.lineTo(cx - (arrowR * 0.55f), cy - (arrowR * 0.866f))
                        arrowPath.lineTo(cx - (arrowR * 0.55f), cy + (arrowR * 0.866f))
                        arrowPath.close()
                    }
                    else -> {}
                }

                // Engraved dark carbon triangle
                drawPath(path = arrowPath, color = Color(0xFF1E1700), style = Fill)

                // Subtle inner shadow/highlight edge
                drawPath(
                    path = arrowPath,
                    color = Color.Black.copy(alpha = 0.4f),
                    style = Stroke(width = 0.8f, cap = StrokeCap.Round)
                )
            }

            // If active, draw radiant gold pulse ring
            if (isActive) {
                drawCircle(
                    color = Color(0xFFFFD700),
                    radius = radius + 2.5f,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}
