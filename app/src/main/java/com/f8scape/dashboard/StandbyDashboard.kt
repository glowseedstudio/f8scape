package com.f8scape.dashboard

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f8scape.app.theme.*
import com.f8scape.cards.CardItem
import com.f8scape.cards.DefaultCardLayouts
import com.f8scape.data.PreferencesRepository
import com.f8scape.data.UserPreferences
import com.f8scape.media.MediaPlaybackRepository
import com.f8scape.media.MediaTrackInfo
import com.f8scape.util.TimeUtils
import com.f8scape.util.WallpaperHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StandbyDashboard(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferencesRepo = remember { PreferencesRepository(context) }
    val preferences by preferencesRepo.userPreferencesFlow.collectAsState(initial = UserPreferences())

    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var lastInteractionTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isEditMode by remember { mutableStateOf(false) }
    var isFullMusicMode by remember { mutableStateOf(false) }

    // Cards state loaded from DataStore preferences
    val currentCards = remember(preferences.customLayoutSerialized, preferences.activePreset) {
        DefaultCardLayouts.deserialize(
            serialized = preferences.customLayoutSerialized,
            fallbackPreset = preferences.activePreset
        )
    }
    var localCards by remember(currentCards) { mutableStateOf(currentCards) }

    // Periodic second/minute tick
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    // Load system wallpaper for ambient background when music is idle
    var wallpaperBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(preferences.showWallpaperBackground) {
        if (preferences.showWallpaperBackground) {
            wallpaperBitmap = WallpaperHelper.getWallpaperBitmap(context)
        } else {
            wallpaperBitmap = null
        }
    }

    // 10-Second Idle Dimming: Smoothly dim screen to 30% after 10s of inactivity
    val isIdleDimmed = remember(currentTimeMs, lastInteractionTimeMs, isEditMode) {
        !isEditMode && ((currentTimeMs - lastInteractionTimeMs) >= 10_000L)
    }

    val idleDimAlpha by animateFloatAsState(
        targetValue = if (isIdleDimmed) 0.70f else 0.0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "IdleDimAlpha"
    )

    // OLED Burn-In Orbital Drift (8-point minute-boundary cycle)
    val burnInStep = remember(currentTimeMs / 60_000L) {
        ((currentTimeMs / 60_000L) % 8).toInt()
    }

    val targetBurnInOffset = remember(burnInStep) {
        when (burnInStep) {
            0 -> IntOffset(0, -10)
            1 -> IntOffset(8, -6)
            2 -> IntOffset(12, 0)
            3 -> IntOffset(8, 6)
            4 -> IntOffset(0, 10)
            5 -> IntOffset(-8, -6)
            6 -> IntOffset(-12, 0)
            else -> IntOffset(-8, 6)
        }
    }

    val animatedBurnInOffset by androidx.compose.animation.core.animateIntOffsetAsState(
        targetValue = targetBurnInOffset,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 2000,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "OledBurnInDrift"
    )

    val trackInfo by MediaPlaybackRepository.mediaTrackState.collectAsState()
    val showFullMusic = isFullMusicMode && trackInfo.hasActiveSession

    // Choose active background: Playing song artwork takes priority; falls back to system wallpaper
    val activeBackgroundArtwork = remember(trackInfo.isPlaying, trackInfo.artwork, wallpaperBitmap, preferences.showWallpaperBackground) {
        if (trackInfo.isPlaying && trackInfo.artwork != null) {
            trackInfo.artwork
        } else if (preferences.showWallpaperBackground) {
            wallpaperBitmap
        } else {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            // Initial pointer pass catches ANY user touch to wake up immediately and reset the 10s idle timer
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) {
                            lastInteractionTimeMs = System.currentTimeMillis()
                        }
                    }
                }
            }
            // Double tap anywhere on background exits; long press customizes
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        onDismissRequest()
                    },
                    onLongPress = {
                        if (!isFullMusicMode && !isEditMode) {
                            isEditMode = true
                        }
                    }
                )
            }
    ) {
        // Ken Burns Ambient Artwork / Wallpaper Background
        KenBurnsBackground(
            artwork = activeBackgroundArtwork,
            isEnabled = preferences.kenBurnsEnabled && activeBackgroundArtwork != null
        )

        // Master window dimming scrim
        if (preferences.screenDimLevel < 0.8f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PureBlack.copy(alpha = (1.0f - preferences.screenDimLevel) * 0.45f))
            )
        }

        // 10-second idle dimming scrim (dims screen down to 30% brightness, saving OLED power)
        if (idleDimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PureBlack.copy(alpha = idleDimAlpha))
            )
        }

        // Floating ambient content with 8-point orbital burn-in drift
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
                .offset { animatedBurnInOffset }
        ) {
            // Top-left back button when in Full Music Mode
            if (showFullMusic) {
                IconButton(
                    onClick = { isFullMusicMode = false },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to dashboard",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Exit tap target (subtle X button) in top right
            if (!isEditMode) {
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .alpha(0.6f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close StandBy", tint = TextTertiary)
                }
            }

            if (showFullMusic) {
                // Full Music View: Album Art where clock was, expanded controls on the side
                ExpandedMusicView(
                    trackInfo = trackInfo,
                    onDoubleTap = onDismissRequest,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 28.dp, bottom = 20.dp)
                )
            } else {
                // Snap-to-Grid Card Engine
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = if (isEditMode) 0.dp else 24.dp, bottom = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DashboardGridEngine(
                        cards = localCards,
                        currentTimeMs = currentTimeMs,
                        weatherCity = preferences.weatherCity,
                        maskCalendar = preferences.calendarMaskLocked,
                        isEditMode = isEditMode,
                        onEnterEditMode = { isEditMode = true },
                        onUpdateCards = { updated ->
                            localCards = updated
                            coroutineScope.launch {
                                preferencesRepo.setCustomLayout(DefaultCardLayouts.serialize(updated))
                            }
                        },
                        onDoneEdit = { isEditMode = false },
                        onResetDefaults = {
                            val defaultList = when (preferences.activePreset) {
                                "Minimal" -> DefaultCardLayouts.getMinimalLayout()
                                "Media" -> DefaultCardLayouts.getMediaLayout()
                                else -> DefaultCardLayouts.getDashboardLayout()
                            }
                            localCards = defaultList
                            coroutineScope.launch {
                                preferencesRepo.setCustomLayout("")
                            }
                        },
                        onDoubleTap = onDismissRequest,
                        onExpandMusic = { isFullMusicMode = true }
                    )
                }

                // Bottom ambient hint
                if (!isEditMode) {
                    Text(
                        text = "F8scape · Long press to customize · Double tap to exit",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .alpha(0.45f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismissRequest
                            )
                    )
                }
            }
        }
    }
}

/**
 * Expanded full-music ambient view: large album artwork on the left,
 * track details and large transport controls on the right.
 */
@Composable
private fun ExpandedMusicView(
    trackInfo: MediaTrackInfo,
    onDoubleTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .pointerInput(Unit) {
                if (onDoubleTap != null) {
                    detectTapGestures(onDoubleTap = { onDoubleTap() })
                }
            },
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Pane: Large Album Artwork (where Clock normally sits)
        Box(
            modifier = Modifier
                .weight(1.05f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            if (trackInfo.artwork != null) {
                Image(
                    bitmap = trackInfo.artwork.asImageBitmap(),
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .border(
                            BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                            RoundedCornerShape(22.dp)
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.80f),
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
        }

        // Right Pane: Expanded Track Details & Large Transport Controls
        Column(
            modifier = Modifier
                .weight(0.95f)
                .fillMaxHeight()
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = trackInfo.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = trackInfo.artist.ifBlank { trackInfo.album.ifBlank { "Playing" } },
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Progress Bar (if duration available)
            if (trackInfo.durationMs > 0) {
                Spacer(modifier = Modifier.height(18.dp))
                LinearProgressIndicator(
                    progress = { (trackInfo.positionMs.toFloat() / trackInfo.durationMs).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.20f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = TimeUtils.formatDuration(trackInfo.positionMs),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                    Text(
                        text = TimeUtils.formatDuration(trackInfo.durationMs),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Large Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Track
                IconButton(
                    onClick = { MediaPlaybackRepository.skipToPrevious() },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous track",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play / Pause Button
                Surface(
                    onClick = { MediaPlaybackRepository.togglePlayPause() },
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(62.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (trackInfo.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (trackInfo.isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Next Track
                IconButton(
                    onClick = { MediaPlaybackRepository.skipToNext() },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next track",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
