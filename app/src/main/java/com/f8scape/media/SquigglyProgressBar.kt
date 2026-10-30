package com.f8scape.media

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Google Media Player-style squiggly/wavy progress bar.
 * The played portion renders as an animated sine wave while audio is playing,
 * smoothly flattening into a straight line when paused.
 * Terminates at the scrubber playhead thumb, while the unplayed portion continues
 * as a subtle straight track line.
 * Supports tap and drag seeking.
 */
@Composable
fun SquigglyProgressBar(
    progress: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onSeek: ((Float) -> Unit)? = null
) {
    val phase by if (isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "SquigglyWaveLoop")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "SquigglyPhase"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val waveAmplitude by animateDpAsState(
        targetValue = if (isPlaying) 3.5.dp else 0.dp,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "SquigglyAmp"
    )

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val effectiveProgress = if (isDragging) dragProgress else progress.coerceIn(0f, 1f)

    val wavePath = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(onSeek) {
                if (onSeek != null) {
                    detectTapGestures { offset ->
                        val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek(newProgress)
                    }
                }
            }
            .pointerInput(onSeek) {
                if (onSeek != null) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeek(dragProgress)
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        }
                    )
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val midY = height / 2f
        val progressX = (width * effectiveProgress).coerceIn(0f, width)

        val strokeWidthPx = 3.dp.toPx()
        val amplitudePx = waveAmplitude.toPx()
        val wavelengthPx = 28.dp.toPx()

        // 1. Unplayed track portion (straight horizontal line from progressX to width)
        if (progressX < width) {
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(progressX, midY),
                end = Offset(width, midY),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )
        }

        // 2. Played track portion (sine wave from 0 to progressX)
        if (progressX > 0f) {
            wavePath.reset()
            val dx = 2.dp.toPx()
            var x = 0f
            wavePath.moveTo(0f, midY)

            val taperEdgePx = 6.dp.toPx()
            while (x < progressX) {
                val taperStart = (x / taperEdgePx).coerceIn(0f, 1f)
                val taperEnd = ((progressX - x) / taperEdgePx).coerceIn(0f, 1f)
                val amp = amplitudePx * taperStart * taperEnd

                val y = midY + amp * Math.sin(2.0 * Math.PI * (x / wavelengthPx) - phase).toFloat()
                wavePath.lineTo(x, y)
                x += dx
            }
            wavePath.lineTo(progressX, midY)

            drawPath(
                path = wavePath,
                color = Color.White,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // Circular playhead scrubber thumb at current position
            drawCircle(
                color = Color.White,
                radius = if (isDragging) 7.dp.toPx() else 5.dp.toPx(),
                center = Offset(progressX, midY)
            )
        } else {
            // Thumb at the initial position
            drawCircle(
                color = Color.White,
                radius = if (isDragging) 7.dp.toPx() else 5.dp.toPx(),
                center = Offset(0f, midY)
            )
        }
    }
}
