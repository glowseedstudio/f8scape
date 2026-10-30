package com.f8scape.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

    // Periodic minute time tick
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // Ken Burns Ambient Artwork Background (dimmed edge-to-edge behind floating elements)
        KenBurnsBackground(
            artwork = trackInfo.artwork,
            isEnabled = preferences.kenBurnsEnabled && trackInfo.isPlaying
        )

        // Ambient dimming scrim (edge-to-edge fullscreen, eliminates darker inset box)
        if (preferences.screenDimLevel < 0.8f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PureBlack.copy(alpha = (1.0f - preferences.screenDimLevel) * 0.45f))
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
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
