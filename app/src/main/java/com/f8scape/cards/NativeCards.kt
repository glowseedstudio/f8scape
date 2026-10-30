package com.f8scape.cards

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f8scape.app.theme.*
import com.f8scape.util.TimeUtils
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CardContainer(
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (isEditMode) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = Color(0x381E1E20)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (onMoveUp != null) {
                            IconButton(
                                onClick = onMoveUp,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Move up",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (onMoveDown != null) {
                            IconButton(
                                onClick = onMoveDown,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Move down",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (onRemove != null) {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FF453A))
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove card",
                                tint = Color(0xFFFF6961),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                content()
            }
        }
    } else {
        // Pure ambient: No card backgrounds, no borders, no boxes.
        // Elements float seamlessly on the OLED pure black background.
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            content()
        }
    }
}

@Composable
fun AmbientClockCard(
    currentTimeMs: Long,
    weatherCity: String = "Canberra",
    isLarge: Boolean = true,
    isEditMode: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var weatherInfo by remember { mutableStateOf<WeatherInfo?>(null) }
    val weatherProvider = remember { WeatherDataProvider(context) }

    var batteryPercent by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }

    LaunchedEffect(weatherCity) {
        weatherInfo = weatherProvider.getWeather(weatherCity)
    }

    LaunchedEffect(Unit) {
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

    CardContainer(
        isEditMode = isEditMode,
        onMoveUp = onMoveUp,
        onMoveDown = onMoveDown,
        onRemove = onRemove
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Samsung One UI Clock: Crisp, large, pure white, elegant spacing
            Text(
                text = TimeUtils.formatClockTime(currentTimeMs, is24Hour = false),
                fontSize = if (isLarge) 84.sp else 62.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2.0).sp,
                color = Color.White
            )

            // Date directly under time
            Text(
                text = TimeUtils.formatDate(currentTimeMs),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 2.dp)
            )

            // Premium Ambient Weather & Battery Row directly under the clock
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Weather
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
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${weatherInfo?.tempC ?: 21}°C · ${weatherInfo?.cityName ?: weatherCity}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.75f)
                )

                // Refined dot separator
                Text(
                    text = "·",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.40f),
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                // Battery
                Icon(
                    imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                    contentDescription = null,
                    tint = if (isCharging) Color(0xFF00E676) else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${batteryPercent}%${if (isCharging) " · Charging" else ""}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
fun AmbientWeatherCard(
    cityName: String = "Canberra",
    isEditMode: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var weatherInfo by remember { mutableStateOf<WeatherInfo?>(null) }
    val weatherProvider = remember { WeatherDataProvider(context) }

    LaunchedEffect(cityName) {
        weatherInfo = weatherProvider.getWeather(cityName)
    }

    CardContainer(
        isEditMode = isEditMode,
        onMoveUp = onMoveUp,
        onMoveDown = onMoveDown,
        onRemove = onRemove
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                        tint = AccentBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${weatherInfo?.tempC ?: 21}°C",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Light,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${weatherInfo?.condition ?: "Clear"} · ${weatherInfo?.cityName ?: cityName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            if (weatherInfo?.isCached == true) {
                Text(
                    text = "Cached",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun AmbientBatteryCard(
    isEditMode: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var batteryPercent by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

            batteryPercent = if (level >= 0 && scale > 0) (level * 100) / scale else 100
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            delay(15000L) // Refresh every 15s
        }
    }

    CardContainer(
        isEditMode = isEditMode,
        onMoveUp = onMoveUp,
        onMoveDown = onMoveDown,
        onRemove = onRemove
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                        contentDescription = null,
                        tint = if (isCharging) AccentBlue else if (batteryPercent > 20) StatusSuccess else StatusWarning,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$batteryPercent%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Light,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isCharging) "Charging on Fast Wireless" else "Battery Health Normal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            LinearProgressIndicator(
                progress = { (batteryPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .width(60.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isCharging) AccentBlue else if (batteryPercent > 20) StatusSuccess else StatusWarning,
                trackColor = SurfaceVariantDark
            )
        }
    }
}

@Composable
fun AmbientCalendarCard(
    maskTitle: Boolean = false,
    isEditMode: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val calendarProvider = remember { CalendarDataProvider(context) }
    var upcomingEvent by remember { mutableStateOf<CalendarEventInfo?>(null) }
    var hasPermission by remember { mutableStateOf(calendarProvider.hasCalendarPermission()) }

    LaunchedEffect(Unit) {
        while (true) {
            hasPermission = calendarProvider.hasCalendarPermission()
            if (hasPermission) {
                upcomingEvent = calendarProvider.getNextUpcomingEvent(maskTitleForPrivacy = maskTitle)
            }
            delay(15_000L)
        }
    }

    CardContainer(
        isEditMode = isEditMode,
        onMoveUp = onMoveUp,
        onMoveDown = onMoveDown,
        onRemove = onRemove
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (!hasPermission) {
                    Text(
                        text = "Calendar Access",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Grant permission in Settings to show events",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.50f)
                    )
                } else if (upcomingEvent == null) {
                    Text(
                        text = "No Upcoming Events",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Your schedule is clear for today",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.60f)
                    )
                } else {
                    val event = upcomingEvent!!
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val formattedTime = if (event.isAllDay) "All day" else "${timeFormat.format(Date(event.startTimeMs))} - ${timeFormat.format(Date(event.endTimeMs))}"
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.70f)
                    )
                }
            }
        }
    }
}
