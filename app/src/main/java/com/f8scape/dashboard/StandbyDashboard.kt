package com.f8scape.dashboard

import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.BatteryManager
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f8scape.app.theme.*
import com.f8scape.cards.CalendarDataProvider
import com.f8scape.cards.CalendarEventInfo
import com.f8scape.cards.CalendarStyle
import com.f8scape.cards.CardItem
import com.f8scape.cards.CardType
import com.f8scape.cards.ClockColorOption
import com.f8scape.cards.ClockStyle
import com.f8scape.cards.DefaultCardLayouts
import com.f8scape.cards.F8ClockFace
import com.f8scape.cards.LiveCalendarEditorPanel
import com.f8scape.cards.LiveClockEditorPanel
import com.f8scape.cards.StyledCalendarDayCell
import com.f8scape.cards.WeatherDataProvider
import com.f8scape.cards.WeatherInfo
import com.f8scape.data.PreferencesRepository
import com.f8scape.data.UserPreferences
import com.f8scape.media.MediaPlaybackRepository
import com.f8scape.media.MediaTrackInfo
import com.f8scape.media.SquigglyProgressBar
import com.f8scape.util.TimeUtils
import com.f8scape.util.WallpaperHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CalendarDaySelection(val year: Int, val month: Int, val day: Int)

enum class StandbyPage {
    CLOCK,
    CALENDAR,
    MUSIC
}

private enum class DashboardScreen {
    GRID,
    PAGED,
    DAY_CALENDAR
}

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
    var isClockEditorOpen by remember { mutableStateOf(false) }
    var isCalendarEditorOpen by remember { mutableStateOf(false) }
    var isPagedMode by remember { mutableStateOf(false) }
    var selectedCalendarDay by remember { mutableStateOf<CalendarDaySelection?>(null) }
    var localContentBrightness by remember {
        mutableFloatStateOf(preferences.contentBrightness)
    }
    var isDraggingBrightness by remember { mutableStateOf(false) }

    LaunchedEffect(preferences.contentBrightness) {
        if (!isDraggingBrightness) {
            localContentBrightness = preferences.contentBrightness
        }
    }

    // Cards state loaded from DataStore preferences
    val currentCards = remember(preferences.customLayoutSerialized, preferences.activePreset) {
        DefaultCardLayouts.deserialize(
            serialized = preferences.customLayoutSerialized,
            fallbackPreset = preferences.activePreset
        )
    }
    var localCards by remember(currentCards) { mutableStateOf(currentCards) }
    var localClockStyle by remember(preferences.clockStyle) { mutableStateOf(preferences.clockStyle) }
    var localClockColorId by remember(preferences.clockColorId) { mutableStateOf(preferences.clockColorId) }
    var localCalendarStyle by remember(preferences.calendarStyle) { mutableStateOf(preferences.calendarStyle) }
    var localCalendarColorId by remember(preferences.calendarColorId) { mutableStateOf(preferences.calendarColorId) }

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
            if (wallpaperBitmap == null) {
                delay(800L)
                wallpaperBitmap = WallpaperHelper.getWallpaperBitmap(context)
            }
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

    val availablePages = remember(localCards, trackInfo.hasActiveSession) {
        buildList {
            add(StandbyPage.CLOCK)
            if (localCards.any { it.type == CardType.CALENDAR }) {
                add(StandbyPage.CALENDAR)
            }
            if (localCards.any { it.type == CardType.MEDIA } || trackInfo.hasActiveSession) {
                add(StandbyPage.MUSIC)
            }
        }
    }

    val pagerState = rememberPagerState(pageCount = { availablePages.size })

    fun openPage(page: StandbyPage) {
        val index = availablePages.indexOf(page)
        if (index >= 0) {
            coroutineScope.launch {
                pagerState.scrollToPage(index)
            }
        }
        isPagedMode = true
    }

    val currentScreen = when {
        selectedCalendarDay != null -> DashboardScreen.DAY_CALENDAR
        isPagedMode -> DashboardScreen.PAGED
        else -> DashboardScreen.GRID
    }

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

    val rootBackgroundColor = PureBlack

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(rootBackgroundColor)
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
                        if (currentScreen == DashboardScreen.GRID && !isEditMode) {
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

        // Master window dimming scrim (active during music playback or when wallpaper is disabled)
        if (preferences.screenDimLevel < 0.8f && (activeBackgroundArtwork != null || !preferences.showWallpaperBackground)) {
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

        // Floating ambient content with 8-point orbital burn-in drift & content brightness
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
                .offset { animatedBurnInOffset }
                .graphicsLayer { alpha = localContentBrightness }
        ) {
            // Top-left back chevron when in Paged Mode or Calendar Overlay Mode
            if (currentScreen != DashboardScreen.GRID && !isEditMode) {
                IconButton(
                    onClick = {
                        when (currentScreen) {
                            DashboardScreen.DAY_CALENDAR -> {
                                selectedCalendarDay = null
                            }
                            DashboardScreen.PAGED -> {
                                isPagedMode = false
                            }
                            DashboardScreen.GRID -> {}
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .alpha(0.6f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextTertiary
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

            // Smooth crossfade transitions between Dashboard screens
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing)) togetherWith
                    fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
                },
                label = "DashboardScreenTransition"
            ) { screen ->
                when (screen) {
                    DashboardScreen.DAY_CALENDAR -> {
                        selectedCalendarDay?.let { selection ->
                            ExpandedCalendarView(
                                selection = selection,
                                maskTitle = preferences.calendarMaskLocked,
                                calendarColorId = localCalendarColorId,
                                onDoubleTap = onDismissRequest,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 28.dp, bottom = 20.dp)
                            )
                        }
                    }
                    DashboardScreen.PAGED -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 28.dp, bottom = 12.dp)
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                userScrollEnabled = !isClockEditorOpen && !isCalendarEditorOpen,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = if (availablePages.size > 1) 22.dp else 0.dp)
                            ) { pageIndex ->
                                when (availablePages.getOrNull(pageIndex)) {
                                    StandbyPage.CLOCK -> {
                                        BigClockView(
                                            currentTimeMs = currentTimeMs,
                                            weatherCity = preferences.weatherCity,
                                            showWeather = preferences.showWeather,
                                            showBattery = preferences.showBattery,
                                            clockStyleId = localClockStyle,
                                            clockColorId = localClockColorId,
                                            isClockEditorOpen = isClockEditorOpen,
                                            onOpenClockEditor = { isClockEditorOpen = true },
                                            onCloseClockEditor = { isClockEditorOpen = false },
                                            onSelectClockStyle = { style ->
                                                localClockStyle = style.id
                                                coroutineScope.launch { preferencesRepo.setClockStyle(style.id) }
                                            },
                                            onSelectClockColor = { color ->
                                                localClockColorId = color.id
                                                coroutineScope.launch { preferencesRepo.setClockColorId(color.id) }
                                            },
                                            onDoubleTap = onDismissRequest,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    StandbyPage.CALENDAR -> {
                                        ExpandedMonthCalendarView(
                                            onDayClick = { year, month, day ->
                                                selectedCalendarDay = CalendarDaySelection(year, month, day)
                                            },
                                            isDimmed = isIdleDimmed,
                                            maskTitle = preferences.calendarMaskLocked,
                                            calendarStyleId = localCalendarStyle,
                                            calendarColorId = localCalendarColorId,
                                            isCalendarEditorOpen = isCalendarEditorOpen,
                                            onOpenCalendarEditor = { isCalendarEditorOpen = true },
                                            onCloseCalendarEditor = { isCalendarEditorOpen = false },
                                            onSelectCalendarStyle = { style ->
                                                localCalendarStyle = style.id
                                                coroutineScope.launch { preferencesRepo.setCalendarStyle(style.id) }
                                            },
                                            onSelectCalendarColor = { color ->
                                                localCalendarColorId = color.id
                                                coroutineScope.launch { preferencesRepo.setCalendarColorId(color.id) }
                                            },
                                            onDoubleTap = onDismissRequest,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    StandbyPage.MUSIC -> {
                                        ExpandedMusicView(
                                            trackInfo = trackInfo,
                                            onDoubleTap = onDismissRequest,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    null -> {}
                                }
                            }

                            // Subtle pill page indicator at the bottom
                            if (availablePages.size > 1 && !isClockEditorOpen && !isCalendarEditorOpen) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(availablePages.size) { index ->
                                        val isCurrent = pagerState.currentPage == index
                                        Box(
                                            modifier = Modifier
                                                .height(4.dp)
                                                .width(if (isCurrent) 16.dp else 4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    if (isCurrent) Color.White.copy(alpha = 0.85f)
                                                    else Color.White.copy(alpha = 0.25f)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    DashboardScreen.GRID -> {
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
                                showWeather = preferences.showWeather,
                                showBattery = preferences.showBattery,
                                clockStyleId = localClockStyle,
                                clockColorId = localClockColorId,
                                calendarStyleId = localCalendarStyle,
                                calendarColorId = localCalendarColorId,
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
                                onExpandClock = { openPage(StandbyPage.CLOCK) },
                                onExpandMusic = { openPage(StandbyPage.MUSIC) },
                                onExpandMonthCalendar = { openPage(StandbyPage.CALENDAR) },
                                onSelectCalendarDay = { year, month, day ->
                                    selectedCalendarDay = CalendarDaySelection(year, month, day)
                                }
                            )

                            // Clean, understated bottom ambient hint (no app name)
                            if (!isEditMode) {
                                Text(
                                    text = "Hold to customize · Double tap to exit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .alpha(0.35f)
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
        }

        // Narrow left-edge gesture strip for vertical drag brightness control (disabled when editing)
        if (!isEditMode && !isClockEditorOpen && !isCalendarEditorOpen) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(64.dp)
                    .padding(top = 64.dp, bottom = 48.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                isDraggingBrightness = true
                                lastInteractionTimeMs = System.currentTimeMillis()
                            },
                            onDragEnd = {
                                isDraggingBrightness = false
                                coroutineScope.launch {
                                    preferencesRepo.setContentBrightness(localContentBrightness)
                                }
                            },
                            onDragCancel = {
                                isDraggingBrightness = false
                                coroutineScope.launch {
                                    preferencesRepo.setContentBrightness(localContentBrightness)
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                lastInteractionTimeMs = System.currentTimeMillis()
                                val delta = -dragAmount / 350f
                                localContentBrightness = (localContentBrightness + delta).coerceIn(0.15f, 1.0f)
                            }
                        )
                    }
            )
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
            // Title & Artist container with stable height so line wrapping never shifts controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = trackInfo.title.ifBlank { "Media Playing" },
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
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Squiggly Progress Bar (Google Media Player-style animated wavy seekbar)
            // Statically anchored so it never disappears or causes buttons to jump on track changes
            val hasValidDuration = trackInfo.durationMs > 0L
            val currentProgress = if (hasValidDuration) {
                (trackInfo.positionMs.toFloat() / trackInfo.durationMs).coerceIn(0f, 1f)
            } else {
                0f
            }
            SquigglyProgressBar(
                progress = currentProgress,
                isPlaying = trackInfo.isPlaying,
                onSeek = { fraction ->
                    if (hasValidDuration) {
                        MediaPlaybackRepository.seekTo((fraction * trackInfo.durationMs).toLong())
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
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
                    text = if (hasValidDuration) TimeUtils.formatDuration(trackInfo.durationMs) else "--:--",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

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

/**
 * Expanded full-calendar ambient view:
 * Large day of week, day number, and month on the left pane (where clock normally sits),
 * scrollable list of calendar entries for that day on the right pane.
 */
@Composable
private fun ExpandedCalendarView(
    selection: CalendarDaySelection,
    maskTitle: Boolean = false,
    calendarColorId: String = "SIGNATURE_BLUE",
    onDoubleTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendarProvider = remember { CalendarDataProvider(context) }
    var events by remember { mutableStateOf<List<CalendarEventInfo>>(emptyList()) }
    val resolvedColor = remember(calendarColorId) { ClockColorOption.fromCalendarDefault(calendarColorId).color }

    LaunchedEffect(selection) {
        events = calendarProvider.getEventsForDay(selection.year, selection.month, selection.day, maskTitle)
    }

    val dayCal = remember(selection) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, selection.year)
            set(Calendar.MONTH, selection.month)
            set(Calendar.DAY_OF_MONTH, selection.day)
        }
    }
    val dayOfWeek = remember(dayCal) {
        SimpleDateFormat("EEEE", Locale.getDefault()).format(dayCal.time).uppercase()
    }
    val monthYear = remember(dayCal) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(dayCal.time)
    }

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
        // Left Pane: Large Date & Day typography (where Clock/Album Art normally sits)
        Box(
            modifier = Modifier
                .weight(1.05f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = dayOfWeek,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = resolvedColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = selection.day.toString(),
                    fontSize = 92.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-4.0).sp,
                    color = Color.White
                )
                Text(
                    text = monthYear,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (events.isEmpty()) "No events scheduled" else "${events.size} event${if (events.size > 1) "s" else ""}",
                    fontSize = 13.sp,
                    color = TextTertiary
                )
            }
        }

        // Right Pane: Scrollable List of Calendar Entries for the Selected Day
        Box(
            modifier = Modifier
                .weight(0.95f)
                .fillMaxHeight()
                .padding(end = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (events.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EventAvailable,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No events scheduled for this day",
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.45f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(events) { event ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                            val timeStr = if (event.isAllDay) {
                                "All Day"
                            } else {
                                "${timeFormat.format(Date(event.startTimeMs))} – ${timeFormat.format(Date(event.endTimeMs))}"
                            }
                            Text(
                                text = timeStr,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = resolvedColor
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = event.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!event.location.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = TextTertiary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = event.location,
                                        fontSize = 12.sp,
                                        color = TextTertiary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Expanded full-month ambient calendar view:
 * Displays a spacious, customizable month grid with M T W T F S S,
 * styled today indicator, event dots, direct tap-to-agenda transitions,
 * and touch-and-hold Live Calendar Studio editor.
 */
@Composable
private fun ExpandedMonthCalendarView(
    onDayClick: (year: Int, month: Int, day: Int) -> Unit,
    isDimmed: Boolean = false,
    maskTitle: Boolean = false,
    calendarStyleId: String = "DEFAULT",
    calendarColorId: String = "SIGNATURE_BLUE",
    isCalendarEditorOpen: Boolean = false,
    onOpenCalendarEditor: () -> Unit = {},
    onCloseCalendarEditor: () -> Unit = {},
    onSelectCalendarStyle: (CalendarStyle) -> Unit = {},
    onSelectCalendarColor: (ClockColorOption) -> Unit = {},
    onDoubleTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendarProvider = remember { CalendarDataProvider(context) }
    val hasPermission = remember { calendarProvider.hasCalendarPermission() }

    val todayCal = remember { Calendar.getInstance() }
    val currentYear = remember { todayCal.get(Calendar.YEAR) }
    val currentMonth = remember { todayCal.get(Calendar.MONTH) }
    val todayDay = remember { todayCal.get(Calendar.DAY_OF_MONTH) }

    var eventsByDay by remember { mutableStateOf<Map<Int, List<CalendarEventInfo>>>(emptyMap()) }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            eventsByDay = calendarProvider.getEventsForMonth(currentYear, currentMonth, maskTitle)
        }
    }

    val monthCal = remember(currentYear, currentMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val monthName = remember(monthCal) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthCal.time).uppercase()
    }
    val daysInMonth = remember(monthCal) { monthCal.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val firstDayOfWeek = remember(monthCal) { monthCal.get(Calendar.DAY_OF_WEEK) }
    val leadingEmptyDays = (firstDayOfWeek - Calendar.MONDAY + 7) % 7
    val weekDays = listOf("M", "T", "W", "T", "F", "S", "S")

    val resolvedStyle = remember(calendarStyleId) { CalendarStyle.fromId(calendarStyleId) }
    val resolvedColorOption = remember(calendarColorId) { ClockColorOption.fromCalendarDefault(calendarColorId) }
    val resolvedColor = resolvedColorOption.color

    // Fade help text off with the 10-second idle dimming
    val helpTextAlpha by animateFloatAsState(
        targetValue = if (isDimmed || isCalendarEditorOpen) 0.0f else 0.55f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "CalendarHelpTextAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isCalendarEditorOpen) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!isCalendarEditorOpen) {
                            onDoubleTap?.invoke()
                        }
                    },
                    onLongPress = {
                        onOpenCalendarEditor()
                    },
                    onTap = {
                        if (isCalendarEditorOpen) {
                            onCloseCalendarEditor()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (isCalendarEditorOpen) Arrangement.SpaceBetween else Arrangement.Center
        ) {
            Column(
                modifier = if (isCalendarEditorOpen) Modifier.weight(1f) else Modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Month Title Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(bottom = if (isCalendarEditorOpen) 8.dp else 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monthName,
                        fontSize = if (isCalendarEditorOpen) 18.sp else 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = resolvedColor
                    )
                    if (!isCalendarEditorOpen) {
                        Text(
                            text = "Tap a day to view schedule · Hold for styles",
                            fontSize = 12.sp,
                            color = TextTertiary,
                            modifier = Modifier.alpha(helpTextAlpha)
                        )
                    }
                }

                // Days of week header (M T W T F S S)
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(bottom = if (isCalendarEditorOpen) 6.dp else 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekDays.forEachIndexed { idx, dayName ->
                        val isWeekend = idx >= 5
                        Text(
                            text = dayName,
                            fontSize = if (isCalendarEditorOpen) 12.sp else 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isWeekend) resolvedColor.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.45f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Spacious Month Days Grid
                val totalCells = leadingEmptyDays + daysInMonth
                val numRows = (totalCells + 6) / 7

                Column(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    verticalArrangement = Arrangement.spacedBy(if (isCalendarEditorOpen) 4.dp else 8.dp)
                ) {
                    for (r in 0 until numRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (c in 0..6) {
                                val cellIndex = r * 7 + c
                                val dayNum = cellIndex - leadingEmptyDays + 1
                                if (dayNum in 1..daysInMonth) {
                                    val isToday = dayNum == todayDay
                                    val dayEvents = eventsByDay[dayNum]
                                    val hasEvents = !dayEvents.isNullOrEmpty()

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(if (isCalendarEditorOpen) 32.dp else 44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .pointerInput(dayNum, isCalendarEditorOpen) {
                                                detectTapGestures(
                                                    onTap = {
                                                        if (isCalendarEditorOpen) {
                                                            onCloseCalendarEditor()
                                                        } else {
                                                            onDayClick(currentYear, currentMonth, dayNum)
                                                        }
                                                    },
                                                    onLongPress = {
                                                        onOpenCalendarEditor()
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        StyledCalendarDayCell(
                                            dayNum = dayNum,
                                            isToday = isToday,
                                            hasEvents = hasEvents,
                                            style = resolvedStyle,
                                            accentColor = resolvedColor,
                                            isCompact = isCalendarEditorOpen
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            if (isCalendarEditorOpen) {
                LiveCalendarEditorPanel(
                    selectedStyle = resolvedStyle,
                    selectedColor = resolvedColorOption,
                    onSelectStyle = onSelectCalendarStyle,
                    onSelectColor = onSelectCalendarColor,
                    onDone = onCloseCalendarEditor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

/**
 * Expanded full-page big clock ambient view:
 * Shows a centered, bold ambient clock with day and date underneath,
 * plus optional ambient weather and battery indicators.
 */
@Composable
private fun BigClockView(
    currentTimeMs: Long,
    weatherCity: String,
    showWeather: Boolean,
    showBattery: Boolean,
    clockStyleId: String = "DEFAULT",
    clockColorId: String = "PURE_WHITE",
    isClockEditorOpen: Boolean = false,
    onOpenClockEditor: () -> Unit = {},
    onCloseClockEditor: () -> Unit = {},
    onSelectClockStyle: (ClockStyle) -> Unit = {},
    onSelectClockColor: (ClockColorOption) -> Unit = {},
    onDoubleTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var weatherInfo by remember { mutableStateOf<WeatherInfo?>(null) }
    val weatherProvider = remember { WeatherDataProvider(context) }

    var batteryPercent by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }

    LaunchedEffect(weatherCity, showWeather) {
        if (showWeather) {
            weatherInfo = weatherProvider.getWeather(weatherCity)
        }
    }

    LaunchedEffect(showBattery) {
        if (showBattery) {
            while (true) {
                val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
                val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
                val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

                batteryPercent = if (level >= 0 && scale > 0) (level * 100) / scale else 100
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

                delay(15000L)
            }
        }
    }

    val dayOfWeek = remember(currentTimeMs) {
        SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(currentTimeMs)).uppercase()
    }
    val fullDate = remember(currentTimeMs) {
        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(currentTimeMs))
    }

    val resolvedStyle = remember(clockStyleId) { ClockStyle.fromId(clockStyleId) }
    val resolvedColorOption = remember(clockColorId) { ClockColorOption.fromId(clockColorId) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isClockEditorOpen) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!isClockEditorOpen) {
                            onDoubleTap?.invoke()
                        }
                    },
                    onLongPress = {
                        onOpenClockEditor()
                    },
                    onTap = {
                        if (isClockEditorOpen) {
                            onCloseClockEditor()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (isClockEditorOpen) Arrangement.SpaceBetween else Arrangement.Center
        ) {
            // Upper/Center Live Clock Preview Area
            Column(
                modifier = if (isClockEditorOpen) Modifier.weight(1f) else Modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                F8ClockFace(
                    currentTimeMs = currentTimeMs,
                    style = resolvedStyle,
                    accentColor = resolvedColorOption.color,
                    isLarge = true,
                    isFullScreen = !isClockEditorOpen
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Day and Date directly under time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayOfWeek,
                        fontSize = if (isClockEditorOpen) 15.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = if (resolvedColorOption == ClockColorOption.PURE_WHITE) AccentBlue else resolvedColorOption.color
                    )
                    Text(
                        text = "·",
                        fontSize = if (isClockEditorOpen) 15.sp else 18.sp,
                        color = Color.White.copy(alpha = 0.40f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Text(
                        text = fullDate,
                        fontSize = if (isClockEditorOpen) 15.sp else 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                // Weather and Battery (shown when not in compact editor mode)
                if (!isClockEditorOpen && (showWeather || showBattery)) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showWeather) {
                            val weatherIcon = when (weatherInfo?.condition) {
                                "Rain", "Showers" -> Icons.Default.WaterDrop
                                "Thunderstorm" -> Icons.Default.Bolt
                                "Snow" -> Icons.Default.AcUnit
                                "Partly Cloudy", "Fog" -> Icons.Default.Cloud
                                else -> Icons.Default.WbSunny
                            }

                            Icon(
                                imageVector = weatherIcon,
                                contentDescription = null,
                                tint = if (weatherInfo?.condition == "Clear" || weatherInfo?.condition == null) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${weatherInfo?.tempC ?: 21}°C · ${weatherInfo?.cityName ?: weatherCity}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }

                        if (showWeather && showBattery) {
                            Text(
                                text = "·",
                                fontSize = 15.sp,
                                color = Color.White.copy(alpha = 0.40f),
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                        }

                        if (showBattery) {
                            Icon(
                                imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                                contentDescription = null,
                                tint = if (isCharging) Color(0xFF00E676) else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${batteryPercent}%${if (isCharging) " · Charging" else ""}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }

            // Live Clock Studio Drawer when Touch & Hold is triggered
            if (isClockEditorOpen) {
                LiveClockEditorPanel(
                    selectedStyle = resolvedStyle,
                    selectedColor = resolvedColorOption,
                    onSelectStyle = onSelectClockStyle,
                    onSelectColor = onSelectClockColor,
                    onDone = onCloseClockEditor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}



