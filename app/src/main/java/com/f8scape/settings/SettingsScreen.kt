package com.f8scape.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f8scape.app.theme.*
import com.f8scape.data.PreferencesRepository
import com.f8scape.data.UserPreferences
import com.f8scape.service.CoverOverlayService
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "F8scape Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureBlack,
                    titleContentColor = TextPrimary
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Standby Automation Section
            item {
                SectionCard(title = "AUTO STANDBY") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic Tent Activation", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text("Show dashboard automatically when propped in tent posture", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Switch(
                            checked = preferences.isAutoStandbyEnabled,
                            onCheckedChange = { coroutineScope.launch { preferencesRepository.setAutoStandbyEnabled(it) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureBlack, checkedTrackColor = AccentBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Only While Charging", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text("Activate StandBy solely when connected to AC/wireless power", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Switch(
                            checked = preferences.onlyWhileCharging,
                            onCheckedChange = { coroutineScope.launch { preferencesRepository.setOnlyWhileCharging(it) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureBlack, checkedTrackColor = AccentBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Tent Dwell Threshold: ${preferences.dwellTimeMs}ms", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Slider(
                        value = preferences.dwellTimeMs.toFloat(),
                        onValueChange = { coroutineScope.launch { preferencesRepository.setDwellTimeMs(it.toLong()) } },
                        valueRange = 800f..2000f,
                        steps = 5,
                        colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue)
                    )
                }
            }

            // Dashboard Presets Section
            item {
                SectionCard(title = "DASHBOARD PRESET") {
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
                    }
                }
            }

            // Power & Display Section
            item {
                SectionCard(title = "POWER & AMBIENT DISPLAY") {
                    Text("Ambient Window Dim Level: ${(preferences.screenDimLevel * 100).toInt()}%", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Slider(
                        value = preferences.screenDimLevel,
                        onValueChange = { coroutineScope.launch { preferencesRepository.setScreenDimLevel(it) } },
                        valueRange = 0.05f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue)
                    )
                    Text("Controls the F8scape window brightness without altering your system brightness settings.", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }
            }

            // Accessibility & System Services Section
            item {
                SectionCard(title = "SYSTEM INTEGRATION & SERVICES") {
                    val isA11yRunning = CoverOverlayService.isServiceRunning
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Accessibility Service", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
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
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isA11yRunning) "Config" else "Open", color = AccentBlue, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val isOverlayGranted = Settings.canDrawOverlays(context)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Appear on top (System Overlay)", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text(
                                text = if (isOverlayGranted) "Granted & active" else "Not granted (Enables full-screen standby overlay)",
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
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isOverlayGranted) "Config" else "Grant", color = AccentBlue, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            Spacer(modifier = Modifier.height(12.dp))
            content()
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
        color = if (isSelected) SurfaceVariantDark else PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AccentBlue else SurfaceBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(label, color = if (isSelected) TextPrimary else TextSecondary, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}
