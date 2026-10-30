package com.f8scape.cards

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

/**
 * Precision Horology Analog Watch Face:
 * - 60 perimeter tick lines with live second-by-second ticker illumination
 * - Bold 12, 3, 6, 9 numerals
 * - Tapered hour and minute baton hands + counterweight second hand
 */
@Composable
fun HorologyAnalogClock(
    currentTimeMs: Long,
    accentColor: Color,
    dialSize: Dp,
    modifier: Modifier = Modifier
) {
    val cal = remember(currentTimeMs) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMs }
    }
    val hours = cal.get(Calendar.HOUR)
    val minutes = cal.get(Calendar.MINUTE)
    val seconds = cal.get(Calendar.SECOND)

    val textPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    Canvas(modifier = modifier.size(dialSize)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = (size.minDimension / 2f) * 0.94f

        // 1. Draw 60 perimeter tick lines (with live second ticker highlight)
        for (i in 0 until 60) {
            val angleRad = Math.toRadians((i * 6.0) - 90.0)
            val isCardinal = i % 15 == 0
            val isHourMark = i % 5 == 0
            val isCurrentSecond = i == seconds
            val isPassedSecond = i < seconds

            val outerR = radius
            val innerR = when {
                isCardinal -> radius * 0.85f
                isHourMark -> radius * 0.89f
                isCurrentSecond -> radius * 0.88f
                else -> radius * 0.93f
            }

            val tickColor = when {
                isCurrentSecond -> accentColor
                isCardinal -> accentColor
                isHourMark -> accentColor.copy(alpha = 0.85f)
                isPassedSecond -> accentColor.copy(alpha = 0.45f)
                else -> accentColor.copy(alpha = 0.22f)
            }

            val strokeW = when {
                isCurrentSecond -> 3.dp.toPx()
                isCardinal -> 3.dp.toPx()
                isHourMark -> 2.dp.toPx()
                else -> 1.2.dp.toPx()
            }

            val start = Offset(
                x = center.x + (innerR * cos(angleRad)).toFloat(),
                y = center.y + (innerR * sin(angleRad)).toFloat()
            )
            val end = Offset(
                x = center.x + (outerR * cos(angleRad)).toFloat(),
                y = center.y + (outerR * sin(angleRad)).toFloat()
            )

            drawLine(
                color = tickColor,
                start = start,
                end = end,
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }

        // 2. Draw 12, 3, 6, 9 numerals in the selected accentColor
        val numeralRadius = radius * 0.69f
        val fontSizePx = radius * 0.21f
        textPaint.textSize = fontSizePx
        textPaint.color = accentColor.toArgb()
        val fontMetrics = textPaint.fontMetrics
        val textCenterOffset = (fontMetrics.descent + fontMetrics.ascent) / 2f

        val numerals = listOf(0 to "12", 15 to "3", 30 to "6", 45 to "9")
        for ((tickIndex, label) in numerals) {
            val angleRad = Math.toRadians((tickIndex * 6.0) - 90.0)
            val nx = center.x + (numeralRadius * cos(angleRad)).toFloat()
            val ny = center.y + (numeralRadius * sin(angleRad)).toFloat() - textCenterOffset
            drawContext.canvas.nativeCanvas.drawText(label, nx, ny, textPaint)
        }

        // 3. Hour Hand
        val hourAngleRad = Math.toRadians(((hours + minutes / 60.0) * 30.0) - 90.0)
        val hourLength = radius * 0.50f
        drawLine(
            color = accentColor,
            start = center,
            end = Offset(
                center.x + (hourLength * cos(hourAngleRad)).toFloat(),
                center.y + (hourLength * sin(hourAngleRad)).toFloat()
            ),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 4. Minute Hand
        val minuteAngleRad = Math.toRadians(((minutes + seconds / 60.0) * 6.0) - 90.0)
        val minuteLength = radius * 0.74f
        drawLine(
            color = accentColor.copy(alpha = 0.90f),
            start = center,
            end = Offset(
                center.x + (minuteLength * cos(minuteAngleRad)).toFloat(),
                center.y + (minuteLength * sin(minuteAngleRad)).toFloat()
            ),
            strokeWidth = 3.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 5. Second Hand with tail counterweight
        val secondAngleRad = Math.toRadians((seconds * 6.0) - 90.0)
        val secondLength = radius * 0.84f
        val tailLength = radius * 0.18f
        drawLine(
            color = accentColor,
            start = Offset(
                center.x - (tailLength * cos(secondAngleRad)).toFloat(),
                center.y - (tailLength * sin(secondAngleRad)).toFloat()
            ),
            end = Offset(
                center.x + (secondLength * cos(secondAngleRad)).toFloat(),
                center.y + (secondLength * sin(secondAngleRad)).toFloat()
            ),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 6. Center Cap Jewel
        drawCircle(color = accentColor, radius = 5.dp.toPx(), center = center)
        drawCircle(color = Color.Black, radius = 2.dp.toPx(), center = center)
    }
}

/**
 * Old-School 7-Segment LED Digital Clock:
 * Renders classic 7-segment digits with subtle unlit ghost segments and bright lit segments.
 */
@Composable
fun RetroLedDigitalClock(
    currentTimeMs: Long,
    accentColor: Color,
    digitHeight: Dp,
    modifier: Modifier = Modifier
) {
    val cal = remember(currentTimeMs) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMs }
    }
    val rawHour = cal.get(Calendar.HOUR)
    val hour12 = if (rawHour == 0) 12 else rawHour
    val minute = cal.get(Calendar.MINUTE)
    val second = cal.get(Calendar.SECOND)

    val h1 = hour12 / 10
    val h2 = hour12 % 10
    val m1 = minute / 10
    val m2 = minute % 10
    val colonVisible = (second % 2) == 0

    val digitWidth = digitHeight * 0.56f
    val colonWidth = digitHeight * 0.18f
    val spacing = digitHeight * 0.10f
    val totalWidth = (digitWidth * 4) + colonWidth + (spacing * 4)

    Canvas(
        modifier = modifier
            .width(totalWidth)
            .height(digitHeight)
    ) {
        val dw = digitWidth.toPx()
        val dh = digitHeight.toPx()
        val cw = colonWidth.toPx()
        val sp = spacing.toPx()
        val segThick = (dh * 0.115f).coerceAtLeast(3f)

        var xCursor = 0f

        // Hour Tens (if 0, draw ghost segments only like a real bedside clock)
        drawSevenSegmentDigit(
            digit = if (h1 == 0) -1 else h1,
            topLeft = Offset(xCursor, 0f),
            width = dw,
            height = dh,
            thickness = segThick,
            activeColor = accentColor,
            ghostColor = accentColor.copy(alpha = 0.09f)
        )
        xCursor += dw + sp

        // Hour Ones
        drawSevenSegmentDigit(
            digit = h2,
            topLeft = Offset(xCursor, 0f),
            width = dw,
            height = dh,
            thickness = segThick,
            activeColor = accentColor,
            ghostColor = accentColor.copy(alpha = 0.09f)
        )
        xCursor += dw + sp

        // Blinking LED Colon
        val colonColor = if (colonVisible) accentColor else accentColor.copy(alpha = 0.12f)
        val dotRadius = segThick * 0.55f
        drawCircle(
            color = colonColor,
            radius = dotRadius,
            center = Offset(xCursor + cw / 2f, dh * 0.34f)
        )
        drawCircle(
            color = colonColor,
            radius = dotRadius,
            center = Offset(xCursor + cw / 2f, dh * 0.66f)
        )
        xCursor += cw + sp

        // Minute Tens
        drawSevenSegmentDigit(
            digit = m1,
            topLeft = Offset(xCursor, 0f),
            width = dw,
            height = dh,
            thickness = segThick,
            activeColor = accentColor,
            ghostColor = accentColor.copy(alpha = 0.09f)
        )
        xCursor += dw + sp

        // Minute Ones
        drawSevenSegmentDigit(
            digit = m2,
            topLeft = Offset(xCursor, 0f),
            width = dw,
            height = dh,
            thickness = segThick,
            activeColor = accentColor,
            ghostColor = accentColor.copy(alpha = 0.09f)
        )
    }
}

private val SEVEN_SEGMENT_MAP = arrayOf(
    booleanArrayOf(true, true, true, true, true, true, false),     // 0
    booleanArrayOf(false, true, true, false, false, false, false), // 1
    booleanArrayOf(true, true, false, true, true, false, true),    // 2
    booleanArrayOf(true, true, true, true, false, false, true),    // 3
    booleanArrayOf(false, true, true, false, false, true, true),   // 4
    booleanArrayOf(true, false, true, true, false, true, true),    // 5
    booleanArrayOf(true, false, true, true, true, true, true),     // 6
    booleanArrayOf(true, true, true, false, false, false, false),  // 7
    booleanArrayOf(true, true, true, true, true, true, true),      // 8
    booleanArrayOf(true, true, true, true, false, true, true)      // 9
)

private fun DrawScope.drawSevenSegmentDigit(
    digit: Int,
    topLeft: Offset,
    width: Float,
    height: Float,
    thickness: Float,
    activeColor: Color,
    ghostColor: Color
) {
    val active = if (digit in 0..9) SEVEN_SEGMENT_MAP[digit] else BooleanArray(7) { false }
    val pad = thickness * 0.25f
    val halfH = height / 2f
    val cr = CornerRadius(thickness / 2f, thickness / 2f)

    // Segment A (Top horizontal)
    drawRoundRect(
        color = if (active[0]) activeColor else ghostColor,
        topLeft = Offset(topLeft.x + thickness * 0.7f, topLeft.y),
        size = Size(width - thickness * 1.4f, thickness),
        cornerRadius = cr
    )
    // Segment B (Top-right vertical)
    drawRoundRect(
        color = if (active[1]) activeColor else ghostColor,
        topLeft = Offset(topLeft.x + width - thickness, topLeft.y + thickness * 0.6f),
        size = Size(thickness, halfH - thickness * 0.9f),
        cornerRadius = cr
    )
    // Segment C (Bottom-right vertical)
    drawRoundRect(
        color = if (active[2]) activeColor else ghostColor,
        topLeft = Offset(topLeft.x + width - thickness, topLeft.y + halfH + pad),
        size = Size(thickness, halfH - thickness * 0.9f),
        cornerRadius = cr
    )
    // Segment D (Bottom horizontal)
    drawRoundRect(
        color = if (active[3]) activeColor else ghostColor,
        topLeft = Offset(topLeft.x + thickness * 0.7f, topLeft.y + height - thickness),
        size = Size(width - thickness * 1.4f, thickness),
        cornerRadius = cr
    )
    // Segment E (Bottom-left vertical)
    drawRoundRect(
        color = if (active[4]) activeColor else ghostColor,
        topLeft = Offset(topLeft.x, topLeft.y + halfH + pad),
        size = Size(thickness, halfH - thickness * 0.9f),
        cornerRadius = cr
    )
    // Segment F (Top-left vertical)
    drawRoundRect(
        color = if (active[5]) activeColor else ghostColor,
        topLeft = Offset(topLeft.x, topLeft.y + thickness * 0.6f),
        size = Size(thickness, halfH - thickness * 0.9f),
        cornerRadius = cr
    )
    // Segment G (Middle horizontal)
    drawRoundRect(
        color = if (active[6]) activeColor else ghostColor,
        topLeft = Offset(topLeft.x + thickness * 0.7f, topLeft.y + halfH - thickness / 2f),
        size = Size(width - thickness * 1.4f, thickness),
        cornerRadius = cr
    )
}
