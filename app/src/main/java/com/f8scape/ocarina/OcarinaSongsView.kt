package com.f8scape.ocarina

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val TriforceGold = Color(0xFFFFD700)
private val LightGold = Color(0xFFFFF9C4)
private val DarkGold = Color(0xFFB78103)
private val CardBackdrop = Color(0xEE0B101E)

private enum class OcarinaTab {
    SONGBOOK,
    FREE_PLAY
}

@Composable
fun OcarinaSongsView(
    expansionProgress: Float = 1f,
    onDoubleTap: () -> Unit,
    onTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val audioEngine = remember(context) { OcarinaAudioEngine(context) }

    DisposableEffect(audioEngine) {
        onDispose {
            audioEngine.release()
        }
    }

    val isPlaying by audioEngine.isPlaying.collectAsState()
    val currentSongId by audioEngine.currentSongId.collectAsState()
    val currentNoteIndex by audioEngine.currentNoteIndex.collectAsState()
    val isMicMode by audioEngine.isMicInputMode.collectAsState()

    var activeTab by remember { mutableStateOf(OcarinaTab.SONGBOOK) }
    var isJukeboxMode by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<OcarinaSong?>(OcarinaSongs.ZELDAS_LULLABY) }
    var selectedJukeboxSong by remember { mutableStateOf<OcarinaSong>(OcarinaSongs.JUKEBOX_TITLE_THEME) }
    var recognizedSongText by remember { mutableStateOf<String?>(null) }
    val freePlayHistory = remember { mutableStateListOf<OcarinaButton>() }
    var lastSongClickTimeMs by remember { mutableLongStateOf(0L) }

    // System Volume Controller with live Hardware Volume Rocker synchronization
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    var isDraggingVolume by remember { mutableStateOf(false) }
    var volumeFraction by remember {
        val max = (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)
        val cur = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (max / 2)
        mutableStateOf((cur.toFloat() / max.toFloat()).coerceIn(0f, 1f))
    }

    // Listen to Android hardware volume rocker broadcasts in real time
    DisposableEffect(context, audioManager) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (!isDraggingVolume && audioManager != null) {
                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                    val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    volumeFraction = (cur.toFloat() / max.toFloat()).coerceIn(0f, 1f)
                }
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (_: Exception) {}

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    // 10-Second Idle Dimming: Dims screen down to 30% brightness (70% black scrim) after 10s of inactivity
    var lastInteractionTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var currentClockTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(250L)
            currentClockTimeMs = System.currentTimeMillis()
            if (!isDraggingVolume && audioManager != null) {
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                volumeFraction = (cur.toFloat() / max.toFloat()).coerceIn(0f, 1f)
            }
        }
    }

    fun registerInteraction() {
        lastInteractionTimeMs = System.currentTimeMillis()
        onTap()
    }

    // Keep interaction timestamp refreshed throughout song playback ONLY for Ocarina Songs screen.
    // On Hyrule Jukebox, playback does NOT prevent the 10s idle dimming, allowing the screen to dim safely.
    LaunchedEffect(isPlaying, isJukeboxMode) {
        if (!isJukeboxMode) {
            lastInteractionTimeMs = System.currentTimeMillis()
            onTap()
            while (isPlaying && !isJukeboxMode) {
                delay(1000L)
                lastInteractionTimeMs = System.currentTimeMillis()
                onTap()
            }
        }
    }

    // On Hyrule Jukebox: screen dims after 10s of inactivity even while music is playing.
    // On Ocarina Songs screen: screen stays fully bright while a song is playing, and dims after 10s when idle.
    val isIdleDimmed = if (isJukeboxMode) {
        (currentClockTimeMs - lastInteractionTimeMs) >= 10_000L
    } else {
        !isPlaying && ((currentClockTimeMs - lastInteractionTimeMs) >= 10_000L)
    }
    val idleDimAlpha by animateFloatAsState(
        targetValue = if (isIdleDimmed) 0.70f else 0.0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "OcarinaIdleDimAlpha"
    )

    // Pulsing transition for active playing aura
    val infiniteTransition = rememberInfiniteTransition(label = "ocarina_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val activePlayingSong = remember(currentSongId, selectedSong, selectedJukeboxSong, isJukeboxMode) {
        OcarinaSongs.findById(currentSongId ?: "") ?: if (isJukeboxMode) selectedJukeboxSong else selectedSong
    }

    val activeBackgroundKey = remember(currentSongId, selectedSong, selectedJukeboxSong, isJukeboxMode) {
        val current = currentSongId
        when {
            current != null && current.startsWith("jukebox_") -> current
            isJukeboxMode -> selectedJukeboxSong.id
            current != null -> current
            else -> selectedSong?.id ?: "zeldas_lullaby"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF04060C))
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap() },
                    onTap = { registerInteraction() }
                )
            }
            .graphicsLayer {
                alpha = expansionProgress
                scaleX = 0.95f + (0.05f * expansionProgress)
                scaleY = 0.95f + (0.05f * expansionProgress)
            }
    ) {
        // 1. Permanent Sacred 3D Spinning Triforces Idle Wallpaper:
        // Always actively spinning underneath as the persistent idle background.
        // When clicking any song, this 3D Triforce wallpaper remains seamlessly visible
        // while the song animation blossoms and fades in over it, with zero blank gaps or setting bleeds.
        OcarinaSongBackgroundEffect(
            isPlaying = false,
            songId = "zeldas_lullaby",
            currentNoteIndex = -1,
            isDimmed = isIdleDimmed,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Dynamic Full-Screen Background:
        //    - In Hyrule Jukebox Mode: 3 Rotating In-Game Screenshots per Track with Smooth Ken Burns Pan & Zoom
        //    - In Ocarina Songbook Mode: Bespoke In-Game Particle & Musical Staff Animations
        Crossfade(
            targetState = activeBackgroundKey,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            label = "OcarinaAnimationCrossfade",
            modifier = Modifier.fillMaxSize()
        ) { bgSongId ->
            if (bgSongId.startsWith("jukebox_")) {
                HyruleJukeboxKenBurnsBackground(
                    songId = bgSongId,
                    isPlaying = isPlaying,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                OcarinaSongBackgroundEffect(
                    isPlaying = isPlaying,
                    songId = bgSongId,
                    currentNoteIndex = currentNoteIndex,
                    isDimmed = isIdleDimmed,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 2. Foreground UI Controls & Medallion Matrix
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Bar: Title, Interactive Volume Slider, Mode Toggles, and Stop button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Crest & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TriforceEmblem(modifier = Modifier.size(24.dp))
                    Text(
                        text = "OCARINA OF TIME",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = TriforceGold,
                        letterSpacing = 1.4.sp
                    )
                }

                // Center: Collapsible Media Volume Control Pill
                OcarinaVolumeControlPill(
                    volumeFraction = volumeFraction,
                    onVolumeChanged = { newFraction ->
                        registerInteraction()
                        volumeFraction = newFraction
                        if (audioManager != null) {
                            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                            val target = (newFraction * max).toInt().coerceIn(0, max)
                            try {
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                            } catch (ignored: Exception) {}
                        }
                    },
                    onDragStateChanged = { isDraggingVolume = it },
                    onInteraction = { registerInteraction() }
                )

                // Right: Mode Toggles (Game Mic vs Full Melody) & Stop Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Controller Mic Mode Toggle
                    Surface(
                        onClick = {
                            registerInteraction()
                            audioEngine.setMicInputMode(!isMicMode)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isMicMode) Color(0xFF1B3A2A) else Color(0xFF141824).copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, if (isMicMode) Color(0xFF00E676).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isMicMode) Icons.Default.Mic else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (isMicMode) Color(0xFF00E676) else Color(0xFF81D4FA),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isMicMode) "GAME MIC MODE" else "FULL MELODY",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    if (isPlaying) {
                        Surface(
                            onClick = {
                                registerInteraction()
                                audioEngine.stopPlayback()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEF5350).copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop",
                                    tint = Color(0xFFFF8A80),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text("Stop", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Mode Toggle Pills
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10141E),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            TabPill(
                                label = "Songbook",
                                isSelected = activeTab == OcarinaTab.SONGBOOK,
                                onClick = {
                                    registerInteraction()
                                    activeTab = OcarinaTab.SONGBOOK
                                }
                            )
                            TabPill(
                                label = "Free Play",
                                isSelected = activeTab == OcarinaTab.FREE_PLAY,
                                onClick = {
                                    registerInteraction()
                                    activeTab = OcarinaTab.FREE_PLAY
                                }
                            )
                        }
                    }
                }
            }

            // 2. Main Body: Songbook Grid (12 songs) vs Hyrule Jukebox Grid (6 tracks, 3 top / 3 bottom) vs Free Play
            AnimatedContent(
                targetState = Pair(activeTab, isJukeboxMode),
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                label = "ocarina_mode_content",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp)
            ) { (mode, jukeboxActive) ->
                when {
                    mode == OcarinaTab.FREE_PLAY -> {
                        InteractiveKeypad(
                            recognizedText = recognizedSongText,
                            onButtonPressed = { button ->
                                registerInteraction()
                                audioEngine.playButtonNote(button)
                                freePlayHistory.add(button)
                                if (freePlayHistory.size > 14) {
                                    freePlayHistory.removeAt(0)
                                }

                                val match = checkSongRecognition(freePlayHistory)
                                if (match != null) {
                                    audioEngine.playSecretSolvedChime()
                                    recognizedSongText = "✨ Recognized ${match.title}! ✨"
                                    coroutineScope.launch {
                                        delay(3500L)
                                        if (recognizedSongText?.contains(match.title) == true) {
                                            recognizedSongText = null
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    jukeboxActive -> {
                        HyruleJukeboxGrid(
                            tracks = OcarinaSongs.jukeboxTracks,
                            currentSongId = currentSongId,
                            selectedSongId = selectedJukeboxSong?.id,
                            isPlaying = isPlaying,
                            pulseScale = pulseScale,
                            onTrackClick = { track ->
                                val now = System.currentTimeMillis()
                                if (now - lastSongClickTimeMs >= 350L) {
                                    lastSongClickTimeMs = now
                                    registerInteraction()
                                    selectedJukeboxSong = track
                                    if (isPlaying && currentSongId == track.id) {
                                        audioEngine.stopPlayback()
                                    } else {
                                        audioEngine.playSong(track)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        TwoRowSongbookGrid(
                            songs = OcarinaSongs.allSongs,
                            currentSongId = currentSongId,
                            selectedSongId = selectedSong?.id,
                            isPlaying = isPlaying,
                            pulseScale = pulseScale,
                            onSongClick = { song ->
                                val now = System.currentTimeMillis()
                                if (now - lastSongClickTimeMs >= 350L) {
                                    lastSongClickTimeMs = now
                                    registerInteraction()
                                    selectedSong = song
                                    if (isPlaying && currentSongId == song.id) {
                                        audioEngine.stopPlayback()
                                    } else {
                                        audioEngine.playSong(song)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // 3. Bottom Status / Hint Bar with Perfectly Centered Hyrule Jukebox Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            ) {
                // Left Hint
                Text(
                    text = "Double tap anywhere to exit",
                    fontSize = 9.5.sp,
                    color = Color.White.copy(alpha = 0.40f),
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                // Perfectly Centered Hyrule Jukebox Interactive Pill Button
                Surface(
                    onClick = {
                        registerInteraction()
                        isJukeboxMode = !isJukeboxMode
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isJukeboxMode) TriforceGold else Color(0xFF141824).copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, if (isJukeboxMode) LightGold else TriforceGold.copy(alpha = 0.6f)),
                    shadowElevation = if (isJukeboxMode) 8.dp else 2.dp,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (isJukeboxMode) Color(0xFF1B1605) else TriforceGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isJukeboxMode) "HYRULE JUKEBOX (ACTIVE)" else "HYRULE JUKEBOX",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isJukeboxMode) Color(0xFF1B1605) else Color.White,
                            letterSpacing = 0.6.sp
                        )
                    }
                }


            }
        }

        // 3. 10-Second Inactivity Dimming Scrim (dims down to 30% brightness = 70% black scrim)
        if (idleDimAlpha > 0.005f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = idleDimAlpha))
            )
        }
    }
}

/**
 * Interactive Collapsible Volume Control Pill.
 * Defaults to a sleek circular icon pill (with active level percentage).
 * When touched, smoothly expands into the interactive drag slider with animated width and fade-in track.
 * Automatically collapses after 4 seconds of inactivity or when tapped again.
 */
@Composable
private fun OcarinaVolumeControlPill(
    volumeFraction: Float,
    onVolumeChanged: (Float) -> Unit,
    onDragStateChanged: (Boolean) -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val volumePercent = (volumeFraction * 100f).toInt().coerceIn(0, 100)
    val currentOnVolumeChanged by rememberUpdatedState(onVolumeChanged)
    val currentOnDragStateChanged by rememberUpdatedState(onDragStateChanged)

    // Auto-collapse after 4 seconds of inactivity if not dragging
    var lastInteractionTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isExpanded, lastInteractionTime) {
        if (isExpanded) {
            delay(4000L)
            isExpanded = false
        }
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFF141824).copy(alpha = 0.88f),
        border = BorderStroke(1.dp, if (isExpanded) TriforceGold.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.15f)),
        shadowElevation = if (isExpanded) 8.dp else 2.dp,
        modifier = modifier.animateContentSize(
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (isExpanded) 12.dp else 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tappable Volume Icon Button
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        onInteraction()
                        lastInteractionTime = System.currentTimeMillis()
                        isExpanded = !isExpanded
                    }
                    .padding(2.dp)
            ) {
                Icon(
                    imageVector = when {
                        volumePercent == 0 -> Icons.Default.VolumeOff
                        volumePercent < 50 -> Icons.Default.VolumeDown
                        else -> Icons.Default.VolumeUp
                    },
                    contentDescription = "Volume",
                    tint = if (isExpanded) TriforceGold else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Expanded Smooth Drag-to-Adjust Volume Bar
            if (isExpanded) {
                Box(
                    modifier = Modifier
                        .width(105.dp)
                        .height(30.dp)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()
                                currentOnDragStateChanged(true)
                                onInteraction()
                                lastInteractionTime = System.currentTimeMillis()
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                currentOnVolumeChanged((down.position.x / w).coerceIn(0f, 1f))

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (change.pressed) {
                                        change.consume()
                                        lastInteractionTime = System.currentTimeMillis()
                                        currentOnVolumeChanged((change.position.x / w).coerceIn(0f, 1f))
                                    } else {
                                        break
                                    }
                                }
                                currentOnDragStateChanged(false)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Background Track (6.dp tall)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.20f))
                    ) {
                        // Active Level Gradient
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(volumeFraction.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(TriforceGold, LightGold)
                                    )
                                )
                        )
                    }
                }
            }

            // Volume Percentage Badge
            Text(
                text = "$volumePercent%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isExpanded) Color.White else Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .clickable {
                        onInteraction()
                        lastInteractionTime = System.currentTimeMillis()
                        isExpanded = !isExpanded
                    }
            )
        }
    }
}

/**
 * 6 Circular Track Medallions for Hyrule Jukebox:
 * 3 on the top row and 3 on the bottom row, perfectly proportioned.
 */
@Composable
private fun HyruleJukeboxGrid(
    tracks: List<OcarinaSong>,
    currentSongId: String?,
    selectedSongId: String?,
    isPlaying: Boolean,
    pulseScale: Float,
    onTrackClick: (OcarinaSong) -> Unit,
    modifier: Modifier = Modifier
) {
    val topRow = tracks.take(3)
    val bottomRow = tracks.drop(3).take(3)

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Row 1: Title Theme, Hyrule Field, Gerudo Valley
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            topRow.forEach { track ->
                SongCircleButton(
                    song = track,
                    isCurrentPlaying = isPlaying && currentSongId == track.id,
                    isSelected = selectedSongId == track.id,
                    pulseScale = pulseScale,
                    onClick = { onTrackClick(track) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 2: Lost Woods, Kakariko Village, Zora's Domain
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomRow.forEach { track ->
                SongCircleButton(
                    song = track,
                    isCurrentPlaying = isPlaying && currentSongId == track.id,
                    isSelected = selectedSongId == track.id,
                    pulseScale = pulseScale,
                    onClick = { onTrackClick(track) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Ultra-premium Ken Burns Effect background for Hyrule Jukebox songs.
 * Cycles through 3 authentic in-game screenshots for the song with smooth slow-motion pan & zoom.
 */
@Composable
private fun HyruleJukeboxKenBurnsBackground(
    songId: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val screenshots = remember(songId) { OcarinaSongs.sceneScreenshotsFor(songId) }
    var previousIndex by remember(songId) { mutableIntStateOf(0) }
    var targetIndex by remember(songId) { mutableIntStateOf(0) }
    val transitionAlpha = remember { androidx.compose.animation.core.Animatable(1f) }

    // Auto-advance screenshots every 7.5 seconds
    LaunchedEffect(songId) {
        previousIndex = 0
        targetIndex = 0
        transitionAlpha.snapTo(1f)
        while (true) {
            delay(7500L)
            previousIndex = targetIndex
            targetIndex = (targetIndex + 1) % screenshots.size
            transitionAlpha.snapTo(0f)
            transitionAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing)
            )
        }
    }

    // Continuous slow-motion pan & zoom animation
    val infiniteTransition = rememberInfiniteTransition(label = "ken_burns_loop")
    val panX by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "kb_pan_x"
    )
    val panY by infiniteTransition.animateFloat(
        initialValue = 14f,
        targetValue = -14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "kb_pan_y"
    )
    val zoom by infiniteTransition.animateFloat(
        initialValue = 1.08f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "kb_zoom"
    )

    // Robust dual-layer compositing that NEVER removes or tears down layouts from the tree
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Base Layer (Previous image)
        Image(
            painter = painterResource(id = screenshots[previousIndex % screenshots.size]),
            contentDescription = "Hyrule Scene",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = panX
                    translationY = panY
                }
        )

        // Top Layer (New target image smoothly fading in)
        if (targetIndex != previousIndex || transitionAlpha.value < 1f) {
            Image(
                painter = painterResource(id = screenshots[targetIndex % screenshots.size]),
                contentDescription = "Hyrule Scene",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = transitionAlpha.value
                        scaleX = zoom
                        scaleY = zoom
                        translationX = panX
                        translationY = panY
                    }
            )
        }



        // Subtle overall vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.40f),
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )
    }
}

/**
 * 2 Rows of 6 Circular Medallion Buttons laid out on a single page with zero scrolling.
 */
@Composable
private fun TwoRowSongbookGrid(
    songs: List<OcarinaSong>,
    currentSongId: String?,
    selectedSongId: String?,
    isPlaying: Boolean,
    pulseScale: Float,
    onSongClick: (OcarinaSong) -> Unit,
    modifier: Modifier = Modifier
) {
    val topRow = songs.take(6)
    val bottomRow = songs.drop(6).take(6)

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Row 1: The 6 Melodies of Hyrule
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            topRow.forEach { song ->
                SongCircleButton(
                    song = song,
                    isCurrentPlaying = isPlaying && currentSongId == song.id,
                    isSelected = selectedSongId == song.id,
                    pulseScale = pulseScale,
                    onClick = { onSongClick(song) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 2: The 6 Sacred Warp Medallions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomRow.forEach { song ->
                SongCircleButton(
                    song = song,
                    isCurrentPlaying = isPlaying && currentSongId == song.id,
                    isSelected = selectedSongId == song.id,
                    pulseScale = pulseScale,
                    onClick = { onSongClick(song) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Individual Circular Medallion Button.
 */
@Composable
private fun SongCircleButton(
    song: OcarinaSong,
    isCurrentPlaying: Boolean,
    isSelected: Boolean,
    pulseScale: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isCurrentPlaying -> song.accentColor
            isSelected -> TriforceGold
            else -> TriforceGold.copy(alpha = 0.35f)
        },
        label = "circle_border"
    )

    val scale = if (isCurrentPlaying) pulseScale else 1.0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .padding(horizontal = 2.dp)
    ) {
        // Circular Medallion Frame
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (isCurrentPlaying) Color(0xFF141E33) else Color(0xFF0C1220),
            border = BorderStroke(
                width = if (isCurrentPlaying || isSelected) 2.5.dp else 1.5.dp,
                color = borderColor
            ),
            shadowElevation = if (isCurrentPlaying) 12.dp else 4.dp,
            modifier = Modifier
                .size(64.dp)
                .scale(scale)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // High-resolution Authentic Medallion/Icon filling the entire circular button
                if (song.iconRes != 0) {
                    Image(
                        painter = painterResource(id = song.iconRes),
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    ZeldaMedallionEmblem(
                        songId = song.id,
                        accentColor = song.accentColor,
                        size = 64.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        // Song Name under circle - wrapped to 2 lines so it never truncates
        Text(
            text = song.title,
            fontSize = 11.sp,
            fontWeight = if (isCurrentPlaying || isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isCurrentPlaying) song.accentColor else if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            lineHeight = 13.5.sp,
            maxLines = 2,
            minLines = 2,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        )
    }
}

@Composable
private fun InteractiveKeypad(
    recognizedText: String?,
    onButtonPressed: (OcarinaButton) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (recognizedText != null) {
            Text(
                text = recognizedText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E676),
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Controller Wing: L Trigger Button (Pitch Shift / Note Accent)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "LEFT JOY-CON",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8),
                    letterSpacing = 1.2.sp
                )

                // Realistic Switch 2 L Shoulder Trigger
                Surface(
                    onClick = { onButtonPressed(OcarinaButton.A) },
                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 16.dp, topEnd = 12.dp, bottomEnd = 12.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.5.dp, Color(0xFF06B6D4).copy(alpha = 0.7f)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Switch2ButtonBadge(customLabel = "L", size = 36.dp)
                        Text("L TRIGGER", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }

            // Center: Nintendo Switch 2 ABXY Diamond Face Buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "ACTION BUTTONS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TriforceGold.copy(alpha = 0.7f),
                    letterSpacing = 1.2.sp
                )

                // Top: X Button
                Switch2KeypadButton(button = OcarinaButton.C_UP, label = "X", onClick = { onButtonPressed(OcarinaButton.C_UP) })
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Y Button
                    Switch2KeypadButton(button = OcarinaButton.C_LEFT, label = "Y", onClick = { onButtonPressed(OcarinaButton.C_LEFT) })
                    // Center Switch 2 Hub Logo
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color(0xFF0F172A), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                    )
                    // Right: A Button
                    Switch2KeypadButton(button = OcarinaButton.A, label = "A", onClick = { onButtonPressed(OcarinaButton.A) })
                }
                // Bottom: B Button
                Switch2KeypadButton(button = OcarinaButton.C_DOWN, label = "B", onClick = { onButtonPressed(OcarinaButton.C_DOWN) })
            }

            // Right Controller Wing: R Trigger Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "RIGHT JOY-CON",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC084FC),
                    letterSpacing = 1.2.sp
                )

                // Realistic Switch 2 R Shoulder Trigger
                Surface(
                    onClick = { onButtonPressed(OcarinaButton.C_RIGHT) },
                    shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 16.dp, topStart = 12.dp, bottomStart = 12.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.5.dp, Color(0xFFA855F7).copy(alpha = 0.7f)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("R TRIGGER", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Switch2ButtonBadge(customLabel = "R", size = 36.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Switch2KeypadButton(
    button: OcarinaButton,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier.size(54.dp)
    ) {
        Switch2ButtonBadge(button = button, customLabel = label, size = 54.dp)
    }
}

private fun checkSongRecognition(history: List<OcarinaButton>): OcarinaSong? {
    for (song in OcarinaSongs.allSongs) {
        val seq = song.buttonSequence
        if (history.size >= seq.size) {
            val tail = history.takeLast(seq.size)
            if (tail == seq) return song
        }
    }
    return null
}

@Composable
private fun TabPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) TriforceGold else Color.Transparent
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF1B1605) else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun TriforceEmblem(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val pathTop = Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w * 0.75f, h * 0.5f)
            lineTo(w * 0.25f, h * 0.5f)
            close()
        }
        val pathBottomLeft = Path().apply {
            moveTo(w * 0.25f, h * 0.5f)
            lineTo(w * 0.5f, h)
            lineTo(0f, h)
            close()
        }
        val pathBottomRight = Path().apply {
            moveTo(w * 0.75f, h * 0.5f)
            lineTo(w, h)
            lineTo(w * 0.5f, h)
            close()
        }

        drawPath(pathTop, color = TriforceGold, style = Fill)
        drawPath(pathBottomLeft, color = TriforceGold, style = Fill)
        drawPath(pathBottomRight, color = TriforceGold, style = Fill)
    }
}
