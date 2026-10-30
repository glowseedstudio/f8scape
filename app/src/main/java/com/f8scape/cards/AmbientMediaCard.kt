package com.f8scape.cards

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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

/**
 * Ambient native media card showing active playback (Spotify, YouTube Music, local players)
 * with album artwork, track/artist details, and transport controls.
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
        if (!isAccessGranted) {
            // Permission needed hint
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
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
                        text = "Now Playing",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enable Media Access in Settings",
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
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
                            text = "Play audio on Spotify or YouTube Music",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.50f)
                        )
                    }
                }
            }
        } else {
            // Active media state
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = !isEditMode && onExpandMusic != null,
                            onClick = { onExpandMusic?.invoke() }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Album Artwork
                    if (trackInfo.artwork != null) {
                        Image(
                            bitmap = trackInfo.artwork!!.asImageBitmap(),
                            contentDescription = "Album Art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.80f),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Metadata
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = trackInfo.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (trackInfo.artist.isNotBlank()) trackInfo.artist else (trackInfo.album.ifBlank { "Playing" }),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.70f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Transport Controls: Clean One UI white glyphs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Track
                    IconButton(
                        onClick = { MediaPlaybackRepository.skipToPrevious() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous track",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Play/Pause: Pure white button with pitch-black icon
                    Surface(
                        onClick = { MediaPlaybackRepository.togglePlayPause() },
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (trackInfo.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (trackInfo.isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Next Track
                    IconButton(
                        onClick = { MediaPlaybackRepository.skipToNext() },
                        modifier = Modifier.size(40.dp)
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
