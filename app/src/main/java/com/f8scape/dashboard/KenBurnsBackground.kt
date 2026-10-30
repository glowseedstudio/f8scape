package com.f8scape.dashboard

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.f8scape.app.theme.PureBlack

/**
 * Ambient Ken Burns Artwork Background.
 * Renders album artwork with slow, subtle pan and zoom animation (45-60s loop),
 * heavy dimming, and deep vignette to guarantee OLED black dominance and zero burn-in.
 */
@Composable
fun KenBurnsBackground(
    artwork: Bitmap?,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isEnabled || artwork == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(PureBlack)
        )
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "KenBurnsTransition")

    // Slow 45-second scale oscillation
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.06f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 45000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "KenBurnsScale"
    )

    // Slow 55-second X translation
    val translationX by infiniteTransition.animateFloat(
        initialValue = -25f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 55000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "KenBurnsTranslationX"
    )

    // Slow 38-second Y translation
    val translationY by infiniteTransition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 38000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "KenBurnsTranslationY"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // Animated artwork layer with enhanced ambient visibility (55% opacity)
        Image(
            bitmap = artwork.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.translationX = translationX
                    this.translationY = translationY
                }
                .alpha(0.55f)
        )

        // Soft ambient vignette to smoothly fade extreme outer edges while keeping artwork clearly visible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            PureBlack.copy(alpha = 0.40f),
                            PureBlack.copy(alpha = 0.80f)
                        )
                    )
                )
        )
    }
}
