package com.f8scape.dashboard

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
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
 * smooth 1.2s crossfade between song transitions, and a deep ambient vignette
 * to guarantee OLED black dominance and zero burn-in.
 */
@Composable
fun KenBurnsBackground(
    artwork: Bitmap?,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    // Current and outgoing previous artwork for seamless crossfade
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previousBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val crossfadeProgress = remember { Animatable(1f) }

    val shouldDisplay = isEnabled && artwork != null
    val targetVisibility = if (shouldDisplay) 1f else 0f
    val masterAlpha = remember { Animatable(0f) }

    // Smooth master fade in/out when playback starts or stops
    LaunchedEffect(targetVisibility) {
        masterAlpha.animateTo(
            targetValue = targetVisibility,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    // Smooth crossfade between songs
    LaunchedEffect(artwork) {
        if (artwork != null && artwork != currentBitmap) {
            previousBitmap = currentBitmap
            currentBitmap = artwork
            crossfadeProgress.snapTo(0f)
            crossfadeProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1200, easing = LinearEasing)
            )
            previousBitmap = null
        } else if (artwork == null) {
            currentBitmap = null
            previousBitmap = null
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "KenBurnsTransition")

    // Dynamic scale oscillation
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.08f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "KenBurnsScale"
    )

    // Dynamic X translation
    val translationX by infiniteTransition.animateFloat(
        initialValue = -55f,
        targetValue = 55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 32000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "KenBurnsTranslationX"
    )

    // Dynamic Y translation
    val translationY by infiniteTransition.animateFloat(
        initialValue = -35f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "KenBurnsTranslationY"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (artwork != null && isEnabled) PureBlack.copy(alpha = masterAlpha.value) else Color.Transparent)
    ) {
        val vis = masterAlpha.value
        if (vis > 0.01f) {
            // Outgoing song artwork fading out
            val prev = previousBitmap
            val progress = crossfadeProgress.value
            if (prev != null && progress < 1f) {
                Image(
                    bitmap = prev.asImageBitmap(),
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
                        .alpha((1f - progress) * 0.70f * vis)
                )
            }

            // Incoming song artwork fading in
            val current = currentBitmap
            if (current != null) {
                Image(
                    bitmap = current.asImageBitmap(),
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
                        .alpha(progress * 0.70f * vis)
                )
            }

            // Fullscreen soft ambient vignette
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
}
