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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.f8scape.app.theme.*
import com.f8scape.data.PreferencesRepository
import com.f8scape.data.UserPreferences
import com.f8scape.media.F8MediaNotificationListener
import com.f8scape.service.CoverOverlayService
import com.f8scape.util.WallpaperHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferencesRepository: PreferencesRepository,
    onNavigateBack: () -> Unit
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

    val oneUiSwitchColors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = SamsungBlue,
        uncheckedThumbColor = Color(0xFF9E9EA4),
        uncheckedTrackColor = Color(0xFF38383C),
        uncheckedBorderColor = Color.Transparent
    )

    val oneUiSliderColors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = SamsungBlue,
        inactiveTrackColor = Color(0xFF323236)
    )

    Scaffold(
        topBar = {
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
        },
        containerColor = PureBlack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Samsung One UI Large Header Banner
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "F8scape",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ambient StandBy for Galaxy Z Fold 8",
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
                            colors = oneUiSwitchColors
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
                            colors = oneUiSwitchColors
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

                    Text("Tent Dwell Threshold: ${preferences.dwellTimeMs}ms", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    Slider(
                        value = preferences.dwellTimeMs.toFloat(),
                        onValueChange = { coroutineScope.launch { preferencesRepository.setDwellTimeMs(it.toLong()) } },
                        valueRange = 800f..2000f,
                        steps = 5,
                        colors = oneUiSliderColors
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
                        colors = oneUiSliderColors
                    )
                    Text("Adjusts brightness of the white time numerals, text, and cards independently of the background. Also adjustable by dragging up/down along the left screen edge.", style = MaterialTheme.typography.labelSmall, color = TextTertiary)

                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 14.dp))

                    // Window Dim Level
                    Text("Ambient Window Dim Level: ${(preferences.screenDimLevel * 100).toInt()}%", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    Slider(
                        value = preferences.screenDimLevel,
                        onValueChange = { coroutineScope.launch { preferencesRepository.setScreenDimLevel(it) } },
                        valueRange = 0.05f..1.0f,
                        colors = oneUiSliderColors
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
                            colors = oneUiSwitchColors
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
                            colors = oneUiSwitchColors
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
                            colors = oneUiSwitchColors
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
                            colors = oneUiSwitchColors
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
                            colors = oneUiSwitchColors
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
