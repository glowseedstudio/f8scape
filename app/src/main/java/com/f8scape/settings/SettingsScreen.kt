package com.f8scape.settings

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.f8scape.app.theme.*
import com.f8scape.cards.CalendarStyle
import com.f8scape.cards.ClockColorOption
import com.f8scape.cards.ClockStyle
import com.f8scape.cards.F8ClockFace
import com.f8scape.data.PreferencesRepository
import com.f8scape.data.UserPreferences
import com.f8scape.media.F8MediaNotificationListener
import com.f8scape.service.CoverOverlayService
import com.f8scape.util.WallpaperHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferencesRepository: PreferencesRepository,
    onNavigateBack: (() -> Unit)? = null,
    onOpenDiagnostics: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences by preferencesRepository.userPreferencesFlow.collectAsState(initial = UserPreferences())

    var hasCalendarPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        )
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCalendarPermission = isGranted
    }

    var hasStoragePermission by remember {
        mutableStateOf(WallpaperHelper.hasStoragePermission(context))
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted
        if (isGranted) {
            WallpaperHelper.clearCache()
        }
    }

    val defaultSwitchColors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = SamsungBlue,
        uncheckedThumbColor = Color(0xFF9E9EA4),
        uncheckedTrackColor = Color(0xFF38383C),
        uncheckedBorderColor = Color.Transparent
    )

    val defaultSliderColors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = SamsungBlue,
        inactiveTrackColor = Color(0xFF323236)
    )

    Scaffold(
        topBar = {
            if (onNavigateBack != null) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PureBlack,
                        titleContentColor = Color.White
                    )
                )
            }
        },
        containerColor = PureBlack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 18.dp, bottom = 110.dp)
        ) {
            // Large Header Banner
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Settings",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Configure StandBy behavior, studios & power",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }

            // Standby Automation Section
            item {
                SectionCard(title = "Auto Standby") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic Tent Activation", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text("Show dashboard automatically when propped in tent posture", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Switch(
                            checked = preferences.isAutoStandbyEnabled,
                            onCheckedChange = { coroutineScope.launch { preferencesRepository.setAutoStandbyEnabled(it) } },
                            colors = defaultSwitchColors
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Only While Charging", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text("Activate StandBy solely when connected to AC/wireless power", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Switch(
                            checked = preferences.onlyWhileCharging,
                            onCheckedChange = { coroutineScope.launch { preferencesRepository.setOnlyWhileCharging(it) } },
                            colors = defaultSwitchColors
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

                    Text("Tent Dwell Threshold: ${preferences.dwellTimeMs}ms", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    Slider(
                        value = preferences.dwellTimeMs.toFloat(),
                        onValueChange = { coroutineScope.launch { preferencesRepository.setDwellTimeMs(it.toLong()) } },
                        valueRange = 800f..2000f,
                        steps = 5,
                        colors = defaultSliderColors
                    )
                }
            }

            // Dashboard Presets Section
            item {
                SectionCard(title = "Dashboard Preset") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PresetChip(
                            label = "Dashboard",
                            isSelected = preferences.activePreset == "Dashboard",
                            onClick = { coroutineScope.launch { preferencesRepository.setActivePreset("Dashboard") } },
                            modifier = Modifier.weight(1f)
                        )
                        PresetChip(
                            label = "Minimal",
                            isSelected = preferences.activePreset == "Minimal",
                            onClick = { coroutineScope.launch { preferencesRepository.setActivePreset("Minimal") } },
                            modifier = Modifier.weight(1f)
                        )
                        PresetChip(
                            label = "Media",
                            isSelected = preferences.activePreset == "Media",
                            onClick = { coroutineScope.launch { preferencesRepository.setActivePreset("Media") } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Power & Ambient Display Section
            item {
                SectionCard(title = "Power & Ambient Display") {
                    // Content Luminance Slider
                    Text("Content Brightness (White Numerals & Cards): ${(preferences.contentBrightness * 100).toInt()}%", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    Slider(
                        value = preferences.contentBrightness,
                        onValueChange = { coroutineScope.launch { preferencesRepository.setContentBrightness(it) } },
                        valueRange = 0.15f..1.0f,
                        colors = defaultSliderColors
                    )
                    Text("Adjusts brightness of the white time numerals, text, and cards independently of the background. Also adjustable by dragging up/down along the left screen edge.", style = MaterialTheme.typography.labelSmall, color = TextTertiary)

                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

                    // Window Dim Level
                    Text("Ambient Window Dim Level: ${(preferences.screenDimLevel * 100).toInt()}%", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    Slider(
                        value = preferences.screenDimLevel,
                        onValueChange = { coroutineScope.launch { preferencesRepository.setScreenDimLevel(it) } },
                        valueRange = 0.05f..1.0f,
                        colors = defaultSliderColors
                    )
                    Text("Controls the F8scape window brightness without altering your system brightness settings.", style = MaterialTheme.typography.labelSmall, color = TextTertiary)

                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

                    // Ken Burns Animation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ambient Ken Burns Motion", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text("Slow, subtle zoom and pan of active album cover art with OLED protection", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Switch(
                            checked = preferences.kenBurnsEnabled,
                            onCheckedChange = { coroutineScope.launch { preferencesRepository.setKenBurnsEnabled(it) } },
                            colors = defaultSwitchColors
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

                    // System Wallpaper Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("System Wallpaper as Background", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text("Display your phone wallpaper with Ken Burns motion when music is idle", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Switch(
                            checked = preferences.showWallpaperBackground,
                            onCheckedChange = { coroutineScope.launch { preferencesRepository.setShowWallpaperBackground(it) } },
                            colors = defaultSwitchColors
                        )
                    }

                    if (!hasStoragePermission) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Storage access needed to read system wallpaper",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE) },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SamsungBlue),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Grant", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Clock Studio & Styles Section (100% synced with Dashboard & Big Clock View)
            item {
                var localStyleId by remember(preferences.clockStyle) { mutableStateOf(preferences.clockStyle) }
                var localColorId by remember(preferences.clockColorId) { mutableStateOf(preferences.clockColorId) }
                var localFullColor by remember(preferences.clockFullColor) { mutableStateOf(preferences.clockFullColor) }
                val activeClockStyle = remember(localStyleId) { ClockStyle.fromId(localStyleId) }
                val activeClockColor = remember(localColorId) { ClockColorOption.fromId(localColorId) }
                var previewTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
                LaunchedEffect(Unit) {
                    while (true) {
                        previewTimeMs = System.currentTimeMillis()
                        delay(1000L)
                    }
                }

                SectionCard(title = "Clock Studio & Styles") {
                    Text(
                        text = "Synced across Ambient Dashboard & Full-Screen Big Clock. Tip: Touch and hold the Big Clock in StandBy to open the live editor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Live OLED Clock Preview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black)
                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
                            .padding(vertical = 22.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        F8ClockFace(
                            currentTimeMs = previewTimeMs,
                            style = activeClockStyle,
                            accentColor = activeClockColor.color,
                            isLarge = false,
                            isFullScreen = false,
                            fullColor = localFullColor
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Clock Face Style", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    Text(activeClockStyle.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ClockStyle.entries, key = { it.id }) { style ->
                            val isSelected = style == activeClockStyle
                            Surface(
                                onClick = {
                                    localStyleId = style.id
                                    coroutineScope.launch { preferencesRepository.setClockStyle(style.id) }
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) SamsungBlue else Color(0xFF26262A),
                                border = if (isSelected) null else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Text(
                                    text = style.displayName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Clock Colour", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text(activeClockColor.displayName, style = MaterialTheme.typography.bodySmall, color = activeClockColor.color, fontWeight = FontWeight.SemiBold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(false to "Highlights", true to "Full Colour").forEach { (isFull, label) ->
                                val selected = localFullColor == isFull
                                Surface(
                                    onClick = {
                                        localFullColor = isFull
                                        coroutineScope.launch { preferencesRepository.setClockFullColor(isFull) }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) SamsungBlue else Color(0xFF26262A),
                                    border = if (selected) null else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(ClockColorOption.entries, key = { it.id }) { option ->
                            val isSelected = option == activeClockColor
                            Surface(
                                onClick = {
                                    localColorId = option.id
                                    coroutineScope.launch { preferencesRepository.setClockColorId(option.id) }
                                },
                                shape = CircleShape,
                                color = option.color,
                                border = BorderStroke(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = option.displayName,
                                            tint = if (option == ClockColorOption.PURE_WHITE || option.isPastel) Color.Black else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Calendar Studio & Styles Section (100% synced with Dashboard & Full-Screen Calendar)
            item {
                var localCalStyleId by remember(preferences.calendarStyle) { mutableStateOf(preferences.calendarStyle) }
                var localCalColorId by remember(preferences.calendarColorId) { mutableStateOf(preferences.calendarColorId) }
                var localCalFullColor by remember(preferences.calendarFullColor) { mutableStateOf(preferences.calendarFullColor) }
                val activeCalStyle = remember(localCalStyleId) { CalendarStyle.fromId(localCalStyleId) }
                val activeCalColor = remember(localCalColorId) { ClockColorOption.fromCalendarDefault(localCalColorId) }

                SectionCard(title = "Calendar Studio & Styles") {
                    Text(
                        text = "Synced across Home Screen Calendar Card & Full-Screen Calendar. Tip: Touch and hold the Full-Screen Calendar in StandBy to open the live editor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Calendar Layout Style", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    Text(activeCalStyle.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(CalendarStyle.entries, key = { it.id }) { style ->
                            val isSelected = style == activeCalStyle
                            Surface(
                                onClick = {
                                    localCalStyleId = style.id
                                    coroutineScope.launch { preferencesRepository.setCalendarStyle(style.id) }
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) SamsungBlue else Color(0xFF26262A),
                                border = if (isSelected) null else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Text(
                                    text = style.displayName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Calendar Colour", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text(activeCalColor.displayName, style = MaterialTheme.typography.bodySmall, color = activeCalColor.color, fontWeight = FontWeight.SemiBold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(false to "Highlights", true to "Full Colour").forEach { (isFull, label) ->
                                val selected = localCalFullColor == isFull
                                Surface(
                                    onClick = {
                                        localCalFullColor = isFull
                                        coroutineScope.launch { preferencesRepository.setCalendarFullColor(isFull) }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) SamsungBlue else Color(0xFF26262A),
                                    border = if (selected) null else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(ClockColorOption.entries, key = { it.id }) { option ->
                            val isSelected = option == activeCalColor
                            Surface(
                                onClick = {
                                    localCalColorId = option.id
                                    coroutineScope.launch { preferencesRepository.setCalendarColorId(option.id) }
                                },
                                shape = CircleShape,
                                color = option.color,
                                border = BorderStroke(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = option.displayName,
                                            tint = if (option == ClockColorOption.PURE_WHITE || option.isPastel) Color.Black else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Native Cards & Integrations Section
            item {
                SectionCard(title = "Native Cards & Integrations") {
                    // Weather City
                    var cityInput by remember(preferences.weatherCity) { mutableStateOf(preferences.weatherCity) }
                    Text("Weather City Location", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    Text("City name used for privacy-preserving weather lookup (Open-Meteo, zero tracking)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cityInput,
                        onValueChange = {
                            cityInput = it
                            coroutineScope.launch { preferencesRepository.setWeatherCity(it) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SamsungBlue,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = SamsungBlue
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

                    // Show Weather Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show Weather", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text("Display temperature and forecast icon under the ambient clock", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Switch(
                            checked = preferences.showWeather,
                            onCheckedChange = { coroutineScope.launch { preferencesRepository.setShowWeather(it) } },
                            colors = defaultSwitchColors
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

                    // Show Battery Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show Battery", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text("Display battery percentage and charging status under the ambient clock", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Switch(
                            checked = preferences.showBattery,
                            onCheckedChange = { coroutineScope.launch { preferencesRepository.setShowBattery(it) } },
                            colors = defaultSwitchColors
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

                    // Calendar Permission
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Calendar Access", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text(
                                text = if (hasCalendarPermission) "Permission granted · Month & agenda active" else "Not granted (Required to show calendar events)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (hasCalendarPermission) StatusSuccess else StatusWarning
                            )
                        }
                        Button(
                            onClick = {
                                if (!hasCalendarPermission) {
                                    calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                                } else {
                                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    })
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26262A)),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(if (hasCalendarPermission) "Config" else "Grant", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(13.dp))
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

                    // Calendar Privacy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mask Calendar When Locked", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text("Replaces event titles with 'Upcoming Event' on lockscreen for privacy", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Switch(
                            checked = preferences.calendarMaskLocked,
                            onCheckedChange = { coroutineScope.launch { preferencesRepository.setCalendarMaskLocked(it) } },
                            colors = defaultSwitchColors
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

                    // Reset Grid Layout
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                preferencesRepository.setCustomLayout("")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26262A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text("Reset Card Layout to Default", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Power, Battery & Resilience Section (Milestone 4)
            item {
                var localPowerProfile by remember(preferences.powerProfile) { mutableStateOf(preferences.powerProfile) }
                var localAutoOffMinutes by remember(preferences.autoOffMinutes) { mutableIntStateOf(preferences.autoOffMinutes) }
                val activeProfile = remember(localPowerProfile) { com.f8scape.power.PowerProfile.fromId(localPowerProfile) }

                SectionCard(title = "Power, Battery & Resilience") {
                    Text("Power Profile", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    Text(activeProfile.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.f8scape.power.PowerProfile.entries.forEach { profile ->
                            val isSelected = profile == activeProfile
                            Surface(
                                onClick = {
                                    localPowerProfile = profile.id
                                    coroutineScope.launch { preferencesRepository.setPowerProfile(profile.id) }
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) SamsungBlue else Color(0xFF26262A),
                                border = if (isSelected) null else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = profile.displayName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

                    // Auto-Eco on Battery Saver
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Eco on Battery Saver", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text("Pause GPU background motion when Android Battery Saver or thermal throttling is active", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Switch(
                            checked = preferences.autoEcoOnBatterySaver,
                            onCheckedChange = { coroutineScope.launch { preferencesRepository.setAutoEcoOnBatterySaver(it) } },
                            colors = defaultSwitchColors
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

                    // Auto-Off Idle Timeout
                    Text("Auto-Off Screen Timeout", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    Text("Automatically exit StandBy after inactivity so the cover screen can turn off", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to "Never", 15 to "15 min", 30 to "30 min", 60 to "60 min").forEach { (mins, label) ->
                            val isSelected = localAutoOffMinutes == mins
                            Surface(
                                onClick = {
                                    localAutoOffMinutes = mins
                                    coroutineScope.launch { preferencesRepository.setAutoOffMinutes(mins) }
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) SamsungBlue else Color(0xFF26262A),
                                border = if (isSelected) null else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = 9.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

                    // Suppress on Incoming / Active Calls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Suppress on Phone Calls", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text("Automatically dismiss StandBy if a phone call or ringtone is active", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Switch(
                            checked = preferences.suppressOnCalls,
                            onCheckedChange = { coroutineScope.launch { preferencesRepository.setSuppressOnCalls(it) } },
                            colors = defaultSwitchColors
                        )
                    }
                }
            }

            // System Integration & Services Section
            item {
                SectionCard(title = "System Integration & Services") {
                    val isA11yRunning = CoverOverlayService.isServiceRunning
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Accessibility Service", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text(
                                text = if (isA11yRunning) "Connected & active" else "Not connected (Required for lawful background overlay launch)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isA11yRunning) StatusSuccess else StatusWarning
                            )
                        }
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26262A)),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(if (isA11yRunning) "Config" else "Open", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(13.dp))
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

                    val isOverlayGranted = Settings.canDrawOverlays(context)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Appear on top (System Overlay)", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text(
                                text = if (isOverlayGranted) "Granted" else "Recommended for instant cover screen overlay rendering",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isOverlayGranted) StatusSuccess else StatusWarning
                            )
                        }
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26262A)),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(if (isOverlayGranted) "Config" else "Grant", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(13.dp))
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

                    val isMediaAccessGranted = F8MediaNotificationListener.isNotificationAccessGranted(context)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Media Session Access", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text(
                                text = if (isMediaAccessGranted) "Granted & active" else "Not granted (Required to read Spotify / YouTube Music playback & artwork)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isMediaAccessGranted) StatusSuccess else StatusWarning
                            )
                        }
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26262A)),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(if (isMediaAccessGranted) "Config" else "Grant", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(13.dp))
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

                    // Quick Settings Tile Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Quick Settings Tile ('Tent Mode')", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text(
                                text = "Swipe down your Samsung Quick Panel and tap edit buttons to add the 'Tent Mode' tile for 1-tap StandBy toggling.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

                    // Hardware & Posture Diagnostics (Moved into Settings for testing)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hardware & Posture Diagnostics", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text(
                                text = "Inspect live hinge sensors, cover screen display IDs, and state machine telemetry for testing",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onOpenDiagnostics,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26262A)),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Open", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 14.dp, bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF161618),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFF24242A) else Color(0xFF141416),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isSelected) SamsungBlue else Color.White.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = SamsungBlue, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = label,
                color = if (isSelected) Color.White else TextSecondary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
