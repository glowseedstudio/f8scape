package com.f8scape.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import com.f8scape.media.F8MediaNotificationListener
import com.f8scape.service.CoverOverlayService
import kotlinx.coroutines.launch

private val HomeBg = Color(0xFF0A0A0C)
private val HomeCardBg = Color(0xFF161618)

@Composable
fun AppHomeScreen(
    preferencesRepository: PreferencesRepository,
    onLaunchPreview: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSetup: () -> Unit
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

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isCalendarGranted = granted
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = HomeCardBg,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        color = when {
                                            !preferences.isAutoStandbyEnabled -> TextTertiary
                                            corePermissionsReady -> StatusSuccess
                                            else -> StatusWarning
                                        },
                                        shape = CircleShape
                                    )
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
                            color = StatusWarning.copy(alpha = 0.14f),
                            border = BorderStroke(0.5.dp, StatusWarning.copy(alpha = 0.45f)),
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

                    // Launch Live Preview Button
                    Surface(
                        onClick = onLaunchPreview,
                        shape = RoundedCornerShape(16.dp),
                        color = AccentBlue,
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = HomeCardBg,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        color = if (weatherOrBatteryOn) AccentBlue.copy(alpha = 0.16f) else Color(0xFF26262A),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = if (weatherOrBatteryOn) AccentBlue else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text("Weather & Battery Status", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
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

                        StatusPill(
                            text = if (weatherOrBatteryOn) "Active" else "Hidden",
                            isPositive = weatherOrBatteryOn
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
                                        color = if (isSelected) AccentBlue else Color(0xFF26262A),
                                        border = if (isSelected) null else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
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
        }

        // Active Studio Styles Card (Quick Summary + Jump to Settings)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = HomeCardBg,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            color = Color(0xFF26262A)
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
                        // Clock summary pill
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1F1F23),
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

                        // Calendar summary pill
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1F1F23),
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
    extraContent: @Composable ColumnScope.() -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HomeCardBg,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isEnabled) StatusSuccess.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = if (isEnabled) StatusSuccess.copy(alpha = 0.16f) else Color(0xFF26262A),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isEnabled) StatusSuccess else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(subtitle, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                StatusPill(text = statusText, isPositive = isEnabled)
            }

            Surface(
                onClick = onActionClick,
                shape = RoundedCornerShape(14.dp),
                color = if (isEnabled) Color(0xFF26262A) else AccentBlue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
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
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            extraContent()
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    isPositive: Boolean
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isPositive) StatusSuccess.copy(alpha = 0.15f) else Color(0xFF26262A),
        border = if (isPositive) BorderStroke(0.5.dp, StatusSuccess.copy(alpha = 0.4f)) else null
    ) {
        Text(
            text = text,
            color = if (isPositive) StatusSuccess else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
