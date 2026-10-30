package com.f8scape.cards

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f8scape.app.theme.*
import com.f8scape.media.F8MediaNotificationListener
import com.f8scape.media.MediaPlaybackRepository
import com.f8scape.util.TimeUtils

/**
 * Ambient native media card styled after the Samsung One UI system player,
 * featuring squircle artwork, seek progress, and clean monochrome transport controls.
 */
@Composable
fun AmbientMediaCard(
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    onExpandMusic: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val trackInfo by MediaPlaybackRepository.mediaTrackState.collectAsState()
    val isAccessGranted = remember(context) {
        F8MediaNotificationListener.isNotificationAccessGranted(context)
    }

    CardContainer(
        modifier = modifier,
        isEditMode = isEditMode,
        onMoveUp = onMoveUp,
        onMoveDown = onMoveDown,
        onRemove = onRemove
    ) {
        Surface(
            color = Color(0x331C1C1E),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = !isEditMode && onExpandMusic != null,
                    onClick = { onExpandMusic?.invoke() }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isAccessGranted) {
                    // Permission needed hint
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.80f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Media Controller",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enable Notification Access in Settings",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.50f)
                            )
                        }
                    }
                } else if (!trackInfo.hasActiveSession) {
                    // Idle state: no session active
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.50f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "No Active Media",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Play audio on Spotify, YouTube Music, or Samsung Music",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.50f)
                            )
                        }
                    }
                } else {
                    // Active media state: Samsung One UI system player layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Album Artwork (One UI squircle)
                        if (trackInfo.artwork != null) {
                            Image(
                                bitmap = trackInfo.artwork!!.asImageBitmap(),
                                contentDescription = "Album Art",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                        RoundedCornerShape(14.dp)
                                    )
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.80f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Track details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = trackInfo.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (trackInfo.artist.isNotBlank()) trackInfo.artist else (trackInfo.album.ifBlank { "Playing" }),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.70f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Progress Bar (if duration available)
                    if (trackInfo.durationMs > 0) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LinearProgressIndicator(
                                progress = { (trackInfo.positionMs.toFloat() / trackInfo.durationMs).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.20f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = TimeUtils.formatDuration(trackInfo.positionMs),
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.50f)
                                )
                                Text(
                                    text = TimeUtils.formatDuration(trackInfo.durationMs),
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.50f)
                                )
                            }
                        }
                    }

                    // Transport Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Track
                        IconButton(
                            onClick = { MediaPlaybackRepository.skipToPrevious() },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous track",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(18.dp))

                        // Play/Pause
                        Surface(
                            onClick = { MediaPlaybackRepository.togglePlayPause() },
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (trackInfo.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (trackInfo.isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(18.dp))

                        // Next Track
                        IconButton(
                            onClick = { MediaPlaybackRepository.skipToNext() },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next track",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
