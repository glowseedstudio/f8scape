package com.f8scape.dashboard

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f8scape.app.theme.*
import com.f8scape.util.TimeUtils
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun StandbyDashboard(
    activePreset: String = "Dashboard", // Minimal, Dashboard, Media
    dimLevel: Float = 0.2f,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var batteryPercent by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }

    // Periodic time tick on minute boundary
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMs = System.currentTimeMillis()
            // Read battery state
            val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            batteryPercent = if (level >= 0 && scale > 0) (level * 100) / scale else 100
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            // Delay until next minute tick or 10s check
            val millisUntilNextMinute = 60000L - (System.currentTimeMillis() % 60000L)
            delay(millisUntilNextMinute.coerceAtLeast(1000L))
        }
    }

    // OLED Burn-in mitigation: subtle microscopic shift (1-2px) every 2 minutes
    val burnInShiftX by remember(currentTimeMs / 120000L) {
        mutableStateOf(((currentTimeMs / 120000L) % 3 - 1).toInt())
    }
    val burnInShiftY by remember(currentTimeMs / 120000L) {
        mutableStateOf((((currentTimeMs / 120000L) + 1) % 3 - 1).toInt())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(24.dp)
            .offset { IntOffset(burnInShiftX, burnInShiftY) }
    ) {
        // Exit tap target (subtle X button)
        IconButton(
            onClick = onDismissRequest,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .alpha(0.6f)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close StandBy", tint = TextTertiary)
        }

        // Center Ambient Content
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Oversized Ambient Clock
            Text(
                text = TimeUtils.formatClockTime(currentTimeMs, is24Hour = true),
                style = MaterialTheme.typography.displayLarge,
                fontSize = if (activePreset == "Minimal") 88.sp else 74.sp,
                fontWeight = FontWeight.ExtraLight,
                color = TextPrimary
            )

            // Date
            Text(
                text = TimeUtils.formatDate(currentTimeMs),
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (activePreset != "Minimal") {
                Spacer(modifier = Modifier.height(28.dp))

                // Native Card Chips Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Weather Chip
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.alpha(0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "21° · Clear",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }

                    // Battery Chip
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.alpha(0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                                contentDescription = null,
                                tint = if (isCharging) AccentBlue else if (batteryPercent > 20) StatusSuccess else StatusWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCharging) "$batteryPercent% ⚡" else "$batteryPercent%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Bottom ambient hint
        Text(
            text = "F8scape StandBy · Double tap to exit",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .alpha(0.5f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                )
        )

        // Dimming overlay if user configured reduced brightness scrim
        if (dimLevel < 0.8f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PureBlack.copy(alpha = (1.0f - dimLevel) * 0.45f))
            )
        }
    }
}
