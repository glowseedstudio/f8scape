package com.f8scape.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.f8scape.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.f8scape.app.theme.AccentBlue
import com.f8scape.app.theme.StatusSuccess
import com.f8scape.app.theme.StatusWarning
import com.f8scape.app.theme.TextSecondary
import com.f8scape.app.theme.TextTertiary
import com.f8scape.cards.CalendarStyle
import com.f8scape.cards.ClockColorOption
import com.f8scape.cards.ClockStyle
import com.f8scape.data.PreferencesRepository
import com.f8scape.data.UserPreferences
import com.f8scape.dashboard.StandbyPage
import com.f8scape.media.F8MediaNotificationListener
import com.f8scape.service.CoverOverlayService
import com.f8scape.util.WallpaperHelper
import kotlinx.coroutines.launch

private val HomeBg = Color(0xFF0A0A0C)
private val HomeCardBg = Color(0xFF161618)

@Composable
fun AppHomeScreen(
    preferencesRepository: PreferencesRepository,
    onLaunchPreview: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSetup: () -> Unit,
    onOpenPhotoCollection: () -> Unit = {},
    onPreviewOcarina: () -> Unit = {},
    onPreviewScreensaver: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val preferences by preferencesRepository.userPreferencesFlow.collectAsState(initial = UserPreferences())

    var isAccessibilityActive by remember { mutableStateOf(CoverOverlayService.isServiceRunning) }
    var isOverlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isMediaGranted by remember { mutableStateOf(F8MediaNotificationListener.isNotificationAccessGranted(context)) }
    var isCalendarGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isWallpaperGranted by remember {
        mutableStateOf(WallpaperHelper.hasWallpaperPermission(context))
    }
    var isPhotosGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isCalendarGranted = granted
    }

    val photosPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isPhotosGranted = granted
    }

    // Automatically refresh permission states whenever user returns to the app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityActive = CoverOverlayService.isServiceRunning
                isOverlayGranted = Settings.canDrawOverlays(context)
                isMediaGranted = F8MediaNotificationListener.isNotificationAccessGranted(context)
                isCalendarGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CALENDAR
                ) == PackageManager.PERMISSION_GRANTED
                isWallpaperGranted = WallpaperHelper.hasWallpaperPermission(context)
                isPhotosGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                } else {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val corePermissionsReady = isAccessibilityActive && isOverlayGranted
    val activeClockStyle = remember(preferences.clockStyle) { ClockStyle.fromId(preferences.clockStyle) }
    val activeClockColor = remember(preferences.clockColorId) { ClockColorOption.fromId(preferences.clockColorId) }
    val activeCalStyle = remember(preferences.calendarStyle) { CalendarStyle.fromId(preferences.calendarStyle) }
    val activeCalColor = remember(preferences.calendarColorId) { ClockColorOption.fromCalendarDefault(preferences.calendarColorId) }

    val customSwitchColors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = AccentBlue,
        uncheckedThumbColor = Color(0xFFAEAEB2),
        uncheckedTrackColor = Color(0xFF323236),
        uncheckedBorderColor = Color.Transparent
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBg)
            .statusBarsPadding()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 110.dp)
    ) {
        // Top Header
        item {
            Column(modifier = Modifier.padding(horizontal = 6.dp)) {
                Text(
                    text = "F8scape",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Galaxy Z Fold 8 Cover Screen StandBy",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }

        // Master StandBy Status & Preview Card
        item {
            val statusColor = when {
                !preferences.isAutoStandbyEnabled -> TextTertiary
                corePermissionsReady -> StatusSuccess
                else -> StatusWarning
            }
            PremiumSectionCard(
                backgroundImageRes = R.drawable.bg_card_tent_standby,
                accentGlowColor = if (preferences.isAutoStandbyEnabled) statusColor else Color.Transparent,
                borderColor = if (preferences.isAutoStandbyEnabled) statusColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.10f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhonelinkSetup,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Auto Tent StandBy",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = when {
                                    !preferences.isAutoStandbyEnabled -> "Paused by user"
                                    corePermissionsReady -> "Active · Ready for tent posture"
                                    else -> "Core setup required for auto-launch"
                                },
                                fontSize = 12.sp,
                                color = when {
                                    !preferences.isAutoStandbyEnabled -> TextSecondary
                                    corePermissionsReady -> StatusSuccess
                                    else -> StatusWarning
                                }
                            )
                        }
                    }

                    Switch(
                        checked = preferences.isAutoStandbyEnabled,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch { preferencesRepository.setAutoStandbyEnabled(enabled) }
                        },
                        colors = customSwitchColors
                    )
                }

                if (!corePermissionsReady) {
                    Surface(
                        onClick = onOpenSetup,
                        shape = RoundedCornerShape(14.dp),
                        color = StatusWarning.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (!isAccessibilityActive && !isOverlayGranted) {
                                    "Enable Accessibility & Overlay permissions"
                                } else if (!isAccessibilityActive) {
                                    "Enable Accessibility Host service"
                                } else {
                                    "Grant Appear on Top permission"
                                },
                                color = StatusWarning,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Complete Setup →",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Launch Live Preview Button (Frosted Blue Glassmorphic with Specular Top Highlight)
                Surface(
                    onClick = onLaunchPreview,
                    shape = RoundedCornerShape(16.dp),
                    color = AccentBlue,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Open Full-Screen StandBy Preview",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section Label: Optional Features & Modules
        item {
            Text(
                text = "OPTIONAL FEATURES & MODULES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = TextTertiary,
                modifier = Modifier.padding(start = 10.dp, top = 4.dp)
            )
        }

        // 1. Optional Module: Music & Media
        item {
            OptionalFeatureCard(
                title = "Music & Ambient Artwork",
                subtitle = if (isMediaGranted) {
                    "Enabled · Active media sessions & squiggly seekbar connected"
                } else {
                    "Optional · Grant Notification Access to display Spotify, YouTube Music & controls"
                },
                icon = Icons.Default.MusicNote,
                isEnabled = isMediaGranted,
                statusText = if (isMediaGranted) "Active" else "Not Enabled",
                actionButtonLabel = if (isMediaGranted) "Manage Access" else "Enable Music Access",
                backgroundImageRes = R.drawable.bg_card_music,
                accentGlowColor = Color(0xFF8B5CF6),
                onActionClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            ) {
                if (isMediaGranted) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ken Burns Artwork Motion", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            Text("Slow ambient background zoom while music plays", fontSize = 12.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = preferences.kenBurnsEnabled,
                            onCheckedChange = { coroutineScope.launch { preferencesRepository.setKenBurnsEnabled(it) } },
                            colors = customSwitchColors
                        )
                    }
                }
            }
        }

        // 2. Optional Module: Calendar & Schedule
        item {
            OptionalFeatureCard(
                title = "Calendar & Daily Schedule",
                subtitle = if (isCalendarGranted) {
                    "Enabled · Full-month grid, event dots & tap-for-agenda active"
                } else {
                    "Optional · Allow Calendar access to show your upcoming events on the cover screen"
                },
                icon = Icons.Default.CalendarMonth,
                isEnabled = isCalendarGranted,
                statusText = if (isCalendarGranted) "Active" else "Not Enabled",
                actionButtonLabel = if (isCalendarGranted) "Permissions" else "Enable Calendar",
                backgroundImageRes = R.drawable.bg_card_calendar,
                accentGlowColor = Color(0xFFF59E0B),
                onActionClick = {
                    if (!isCalendarGranted) {
                        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                    } else {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    }
                }
            )
        }

        // 3. Optional Module: Weather & Battery Indicators
        item {
            val weatherOrBatteryOn = preferences.showWeather || preferences.showBattery
            PremiumSectionCard(
                backgroundImageRes = R.drawable.bg_card_weather,
                accentGlowColor = if (weatherOrBatteryOn) Color(0xFF38BDF8) else Color.Transparent,
                borderColor = if (weatherOrBatteryOn) Color(0xFF38BDF8).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.10f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text("Weather & Battery Status", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = buildString {
                                    if (preferences.showWeather) append("Weather (${preferences.weatherCity})")
                                    if (preferences.showWeather && preferences.showBattery) append(" · ")
                                    if (preferences.showBattery) append("Battery %")
                                    if (!preferences.showWeather && !preferences.showBattery) append("Both hidden")
                                },
                                fontSize = 12.sp,
                                color = if (weatherOrBatteryOn) StatusSuccess else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    StatusPill(
                        text = if (weatherOrBatteryOn) "Active" else "Hidden",
                        isPositive = weatherOrBatteryOn,
                        accentColor = Color(0xFF38BDF8)
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Weather (${preferences.weatherCity})", fontSize = 14.sp, color = Color.White)
                    Switch(
                        checked = preferences.showWeather,
                        onCheckedChange = { coroutineScope.launch { preferencesRepository.setShowWeather(it) } },
                        colors = customSwitchColors
                    )
                }

                if (preferences.showWeather) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Temperature Unit", fontSize = 13.sp, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            com.f8scape.cards.TemperatureUnit.entries.forEach { unit ->
                                val isSelected = preferences.weatherTempUnit == unit.id
                                Surface(
                                    onClick = {
                                        coroutineScope.launch { preferencesRepository.setWeatherTempUnit(unit.id) }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) AccentBlue else Color.White.copy(alpha = 0.07f),
                                    border = BorderStroke(1.dp, if (isSelected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.10f))
                                ) {
                                    Text(
                                        text = unit.shortLabel,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Battery Level", fontSize = 14.sp, color = Color.White)
                    Switch(
                        checked = preferences.showBattery,
                        onCheckedChange = { coroutineScope.launch { preferencesRepository.setShowBattery(it) } },
                        colors = customSwitchColors
                    )
                }
            }
        }

        // 4. Optional Module: System Wallpaper Background
        item {
            OptionalFeatureCard(
                title = "System Wallpaper as Background",
                subtitle = if (isWallpaperGranted) {
                    "Enabled · Active wallpaper extracted for Ken Burns ambient background"
                } else {
                    "Optional · Grant All Files Access so Android allows F8scape to extract your current wallpaper"
                },
                icon = Icons.Default.Image,
                isEnabled = isWallpaperGranted && preferences.showWallpaperBackground,
                statusText = if (isWallpaperGranted && preferences.showWallpaperBackground) "Active" else if (!isWallpaperGranted) "Access Needed" else "Off in Settings",
                actionButtonLabel = if (!isWallpaperGranted) "Enable Access" else "Settings",
                backgroundImageRes = R.drawable.bg_card_wallpaper,
                accentGlowColor = Color(0xFF10B981),
                onActionClick = {
                    if (!isWallpaperGranted) {
                        WallpaperHelper.requestWallpaperPermission(context)
                    } else {
                        onOpenSettings()
                    }
                }
            ) {
                if (isWallpaperGranted) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show Wallpaper as Background", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            Text("Renders under clock and widgets when music is idle", fontSize = 12.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = preferences.showWallpaperBackground,
                            onCheckedChange = { coroutineScope.launch { preferencesRepository.setShowWallpaperBackground(it) } },
                            colors = customSwitchColors
                        )
                    }
                }
            }
        }

        // Optional Enhancement 4: Ambient Photo Frame Full-Screen Page
        item {
            val photoCount = preferences.photoFrameSelectedUrisSerialized.split("\n").filter { it.isNotBlank() }.size
            val isPhotoPageActive = preferences.enablePhotoFramePage

            OptionalFeatureCard(
                title = "Ambient Photo Frame",
                subtitle = if (!isPhotosGranted) {
                    "Optional · Grant Photos access so F8scape can access your device photos for your ambient slideshow"
                } else if (preferences.photoFrameSourceType == "FOLDER") {
                    "Enabled · Dedicated folder slideshow available in full-screen StandBy"
                } else if (photoCount > 0) {
                    "Enabled · $photoCount photo(s) ready in your full-screen StandBy slideshow"
                } else {
                    "Enabled · No photos added yet — tap Manage Photos to select pictures"
                },
                icon = Icons.Default.PhotoLibrary,
                isEnabled = isPhotosGranted && isPhotoPageActive,
                statusText = if (!isPhotosGranted) "Access Needed" else if (isPhotoPageActive) "Active Page" else "Page Disabled",
                actionButtonLabel = if (!isPhotosGranted) "Enable Access" else "Manage Photos",
                backgroundImageRes = R.drawable.bg_card_photo_frame,
                accentGlowColor = Color(0xFFEC4899),
                onActionClick = {
                    if (!isPhotosGranted) {
                        photosPermissionLauncher.launch(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.READ_MEDIA_IMAGES
                            } else {
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            }
                        )
                    } else {
                        onOpenPhotoCollection()
                    }
                }
            ) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Full-Screen Photo Page", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Swipe to your photo slideshow when in full-screen StandBy mode", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isPhotoPageActive,
                        onCheckedChange = { active ->
                            coroutineScope.launch {
                                preferencesRepository.setEnablePhotoFramePage(active)
                            }
                        },
                        colors = customSwitchColors
                    )
                }

                if (isPhotosGranted) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        onClick = onOpenPhotoCollection,
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (photoCount > 0) "Manage Photo Collection ($photoCount photos) →" else "Select Photos or Folder →",
                                fontSize = 13.sp,
                                color = AccentBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Optional Enhancement 5: Ocarina of Time Songs Full-Screen Page
        item {
            val isOcarinaActive = preferences.enableOcarinaPage

            OptionalFeatureCard(
                title = "Ocarina of Time Songs",
                subtitle = if (isOcarinaActive) {
                    "Enabled · 12 magical melodies from Hyrule with authentic N64 acoustics ready in full-screen StandBy"
                } else {
                    "Disabled · Enable to swipe to your Ocarina songbook and instrument in StandBy"
                },
                icon = Icons.Default.MusicNote,
                isEnabled = isOcarinaActive,
                statusText = if (isOcarinaActive) "Active Page" else "Page Disabled",
                actionButtonLabel = if (CoverOverlayService.isServiceRunning) "Launch Overlay" else "Open Ocarina",
                backgroundImageRes = R.drawable.bg_card_ocarina,
                accentGlowColor = Color(0xFFF59E0B),
                onActionClick = {
                    if (CoverOverlayService.isServiceRunning) {
                        CoverOverlayService.activeInstance?.showStandbyOverlay(StandbyPage.OCARINA)
                    } else {
                        onPreviewOcarina()
                    }
                }
            ) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Full-Screen Ocarina Page", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Swipe to your Ocarina songs when in full-screen StandBy mode", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isOcarinaActive,
                        onCheckedChange = { active ->
                            coroutineScope.launch {
                                preferencesRepository.setEnableOcarinaPage(active)
                            }
                        },
                        colors = customSwitchColors
                    )
                }
            }
        }

        // Optional Enhancement 6: Pure Triforce Screensaver Mode
        item {
            val isScreensaverPageActive = preferences.enableScreensaverPage
            val isPresetScreensaver = preferences.activePreset in listOf("Triforce", "Screensaver", "Triforce Screensaver")

            OptionalFeatureCard(
                title = "Triforce Screensaver",
                subtitle = if (isPresetScreensaver) {
                    "Active Preset · Automatically enters the 3D spinning Triforce screensaver with safe OLED orbital shifting & idle dimming"
                } else if (isScreensaverPageActive) {
                    "Enabled · 3D spinning Triforces with safe OLED orbital shifting ready in full-screen StandBy"
                } else {
                    "Disabled · Enable to add the ambient 3D Triforce screensaver to StandBy"
                },
                icon = Icons.Default.Shield,
                isEnabled = isPresetScreensaver || isScreensaverPageActive,
                statusText = if (isPresetScreensaver) "Active Preset" else if (isScreensaverPageActive) "Active Page" else "Disabled",
                actionButtonLabel = if (CoverOverlayService.isServiceRunning) "Launch Screensaver" else "Preview",
                backgroundImageRes = R.drawable.bg_card_ocarina,
                accentGlowColor = Color(0xFFFFD700),
                onActionClick = {
                    if (CoverOverlayService.isServiceRunning) {
                        CoverOverlayService.activeInstance?.showStandbyOverlay(StandbyPage.SCREENSAVER)
                    } else {
                        onPreviewScreensaver()
                    }
                }
            ) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Full-Screen Screensaver Page", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Swipe to the 3D Triforce screensaver in StandBy mode", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isScreensaverPageActive,
                        onCheckedChange = { active ->
                            coroutineScope.launch {
                                preferencesRepository.setEnableScreensaverPage(active)
                            }
                        },
                        colors = customSwitchColors
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Set as Default Preset", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Opens directly into the Triforce screensaver in tent posture", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isPresetScreensaver,
                        onCheckedChange = { setAsPreset ->
                            coroutineScope.launch {
                                preferencesRepository.setActivePreset(if (setAsPreset) "Triforce" else "Dashboard")
                            }
                        },
                        colors = customSwitchColors
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Three Spiritual Stones", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Display authentic 3D Kokiri Emerald, Goron Ruby, and Zora Sapphire", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = preferences.screensaverShowStones,
                        onCheckedChange = { showStones ->
                            coroutineScope.launch {
                                preferencesRepository.setScreensaverShowStones(showStones)
                            }
                        },
                        colors = customSwitchColors
                    )
                }
            }
        }

        // Active Studio Styles Card (Quick Summary + Jump to Settings)
        item {
            PremiumSectionCard(
                backgroundImageRes = R.drawable.bg_card_clock_styles,
                accentGlowColor = Color(0xFF6366F1),
                borderColor = Color(0xFF6366F1).copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active Clock & Calendar Styles", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Surface(
                        onClick = onOpenSettings,
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                    ) {
                        Text(
                            text = "Customize",
                            color = AccentBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Clock summary pill (frosted glass)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.07f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(activeClockColor.color)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            )
                            Column {
                                Text("CLOCK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextTertiary, letterSpacing = 1.sp)
                                Text(activeClockStyle.displayName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }

                    // Calendar summary pill (frosted glass)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.07f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(activeCalColor.color)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            )
                            Column {
                                Text("CALENDAR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextTertiary, letterSpacing = 1.sp)
                                Text(activeCalStyle.displayName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumSectionCard(
    modifier: Modifier = Modifier,
    @DrawableRes backgroundImageRes: Int? = null,
    accentGlowColor: Color = Color.Transparent,
    borderColor: Color = Color.White.copy(alpha = 0.12f),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF101114))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        borderColor.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.04f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        if (backgroundImageRes != null) {
            Image(
                painter = painterResource(id = backgroundImageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }

        // High-visibility directional dark scrim:
        // Left side shields text contrast, right side lets the rich artwork shine vividly!
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xD908090C), // ~85% dark on the left behind text
                            Color(0x6008090C), // ~37% in center
                            Color(0x1808090C)  // ~10% on the right: background art is rich and vibrant!
                        )
                    )
                )
        )
        // Soft bottom gradient so buttons stay grounded
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x15000000),
                            Color(0x60000000)
                        )
                    )
                )
        )

        // Accent corner/horizontal subtle radial glow
        if (accentGlowColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentGlowColor.copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            center = Offset(Float.POSITIVE_INFINITY, 0f),
                            radius = 650f
                        )
                    )
            )
        }

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun OptionalFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isEnabled: Boolean,
    statusText: String,
    actionButtonLabel: String,
    onActionClick: () -> Unit,
    @DrawableRes backgroundImageRes: Int,
    accentGlowColor: Color = if (isEnabled) StatusSuccess else Color(0xFF6B7280),
    extraContent: @Composable ColumnScope.() -> Unit = {}
) {
    PremiumSectionCard(
        backgroundImageRes = backgroundImageRes,
        accentGlowColor = if (isEnabled) accentGlowColor else Color.Transparent,
        borderColor = if (isEnabled) accentGlowColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Flat premium icon directly on card surface - no backing box/circle
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
                Column {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(subtitle, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            StatusPill(
                text = statusText,
                isPositive = isEnabled,
                accentColor = if (accentGlowColor != Color.Transparent) accentGlowColor else StatusSuccess
            )
        }

        Surface(
            onClick = onActionClick,
            shape = RoundedCornerShape(14.dp),
            color = if (isEnabled) Color.White.copy(alpha = 0.08f) else AccentBlue,
            border = if (isEnabled) BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)) else null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionButtonLabel,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        extraContent()
    }
}

@Composable
private fun StatusPill(
    text: String,
    isPositive: Boolean,
    accentColor: Color = StatusSuccess
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isPositive) accentColor.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f),
        border = BorderStroke(
            0.5.dp,
            if (isPositive) accentColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (isPositive) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(accentColor, CircleShape)
                )
            }
            Text(
                text = text,
                color = if (isPositive) accentColor else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
