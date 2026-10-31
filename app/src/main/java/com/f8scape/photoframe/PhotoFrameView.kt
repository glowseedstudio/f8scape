package com.f8scape.photoframe

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.f8scape.app.theme.PureBlack
import com.f8scape.app.theme.SamsungBlue
import com.f8scape.app.theme.TextSecondary
import com.f8scape.app.theme.TextTertiary
import com.f8scape.cards.*
import com.f8scape.data.UserPreferences
import com.f8scape.util.TimeUtils
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Visual StandBy Presentation for the Photo Frame Preset.
 * Renders full-screen photography with crossfades, optional subtle Ken Burns motion,
 * dark ambient vignettes for OLED contrast, customizable clock/calendar overlays,
 * and lock-screen privacy enforcement.
 */
@Composable
fun PhotoFrameView(
    preferences: UserPreferences,
    currentTimeMs: Long,
    isEcoModeActive: Boolean,
    onDoubleTap: (() -> Unit)? = null,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sourceRepo = remember { PhotoFrameSourceRepository(context) }
    val slideshowController = remember {
        PhotoFrameSlideshowController(context, coroutineScope)
    }

    // Keyguard lock detection for lock-screen privacy
    val keyguardManager = remember { context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager }
    var isDeviceLocked by remember {
        mutableStateOf(keyguardManager?.isKeyguardLocked == true)
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                isDeviceLocked = keyguardManager?.isKeyguardLocked == true
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
            slideshowController.stop()
        }
    }

    val isPrivacyShieldActive = isDeviceLocked && !preferences.photoFrameShowWhileLocked

    // Resolve active photo collection URIs
    var resolvedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    LaunchedEffect(
        preferences.photoFrameSourceType,
        preferences.photoFrameSelectedUrisSerialized,
        preferences.photoFrameFolderUri
    ) {
        resolvedUris = sourceRepo.resolveActiveUris(
            sourceType = preferences.photoFrameSourceType,
            selectedUrisSerialized = preferences.photoFrameSelectedUrisSerialized,
            folderUriString = preferences.photoFrameFolderUri
        )
    }

    // Feed URIs and playback settings to slideshow controller
    LaunchedEffect(
        resolvedUris,
        preferences.photoFrameShuffle,
        preferences.photoFrameIntervalSeconds,
        isPrivacyShieldActive
    ) {
        if (!isPrivacyShieldActive) {
            slideshowController.updateSource(
                uris = resolvedUris,
                shuffle = preferences.photoFrameShuffle,
                intervalSec = preferences.photoFrameIntervalSeconds
            )
        } else {
            slideshowController.stop()
        }
    }

    val slideshowState by slideshowController.state.collectAsState()

    // 900ms subtle crossfade between outgoing and incoming photograph
    val crossfadeProgress = remember { Animatable(1f) }
    LaunchedEffect(slideshowState.transitionTrigger) {
        if (slideshowState.transitionTrigger > 0L) {
            crossfadeProgress.snapTo(0f)
            crossfadeProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 900, easing = LinearEasing)
            )
        }
    }

    // Optional Ken Burns Photo Motion (disabled in Eco mode or when preference is off)
    val isMotionEnabled = preferences.photoFrameMotion && !isEcoModeActive && !slideshowState.isEmpty
    val infiniteTransition = rememberInfiniteTransition(label = "PhotoMotionTransition")

    val motionScale by if (isMotionEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 1.03f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 32000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PhotoMotionScale"
        )
    } else {
        remember { mutableFloatStateOf(1.0f) }
    }

    val motionTranslationX by if (isMotionEnabled) {
        infiniteTransition.animateFloat(
            initialValue = -20f,
            targetValue = 20f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 36000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PhotoMotionTranslationX"
        )
    } else {
        remember { mutableFloatStateOf(0.0f) }
    }

    val motionTranslationY by if (isMotionEnabled) {
        infiniteTransition.animateFloat(
            initialValue = -12f,
            targetValue = 12f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 28000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PhotoMotionTranslationY"
        )
    } else {
        remember { mutableFloatStateOf(0.0f) }
    }

    val contentScale = if (preferences.photoFrameFitMode == "FIT") {
        ContentScale.Fit
    } else {
        ContentScale.Crop
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap?.invoke() },
                    onTap = { onTap?.invoke() }
                )
            }
    ) {
        // --- 1. PRIVACY SHIELD FALLBACK ---
        if (isPrivacyShieldActive) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Privacy Shield",
                        tint = TextTertiary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Device Locked",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Unlock device to display selected photos",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
            return@Box
        }

        // --- 2. EMPTY STATE GUIDANCE ---
        if (slideshowState.isEmpty || resolvedUris.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Photo Frame Empty",
                        tint = SamsungBlue,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "Photo Frame",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "No photographs selected. Open F8scape Settings → Photo Frame to add photos or choose a folder.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.widthIn(max = 420.dp)
                    )
                }
            }
            return@Box
        }

        // --- 3. ACTIVE PHOTOGRAPH RENDERING ---
        val previousBmp = slideshowState.previousBitmap
        val currentBmp = slideshowState.currentBitmap
        val progress = crossfadeProgress.value

        // Outgoing previous bitmap during crossfade
        if (previousBmp != null && progress < 0.999f) {
            Image(
                bitmap = previousBmp.asImageBitmap(),
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 1f - progress
                    }
            )
        }

        // Incoming / current bitmap
        if (currentBmp != null) {
            Image(
                bitmap = currentBmp.asImageBitmap(),
                contentDescription = "Ambient Photograph",
                contentScale = contentScale,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = if (previousBmp != null) progress else 1f
                        if (isMotionEnabled) {
                            scaleX = motionScale
                            scaleY = motionScale
                            translationX = motionTranslationX
                            translationY = motionTranslationY
                        }
                    }
            )
        } else if (slideshowState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = SamsungBlue,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
