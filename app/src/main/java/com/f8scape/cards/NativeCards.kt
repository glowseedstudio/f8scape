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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f8scape.app.theme.*
import com.f8scape.util.TimeUtils
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
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
    showWeather: Boolean = true,
    showBattery: Boolean = true,
    clockStyleId: String = "DEFAULT",
    clockColorId: String = "PURE_WHITE",
    isLarge: Boolean = true,
    isEditMode: Boolean = false,
    onClockClick: () -> Unit = {},
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
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

    val resolvedStyle = remember(clockStyleId) { ClockStyle.fromId(clockStyleId) }
    val resolvedColor = remember(clockColorId) { ClockColorOption.fromId(clockColorId).color }

    CardContainer(
        isEditMode = isEditMode,
        onMoveUp = onMoveUp,
        onMoveDown = onMoveDown,
        onRemove = onRemove
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = !isEditMode) { onClockClick() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Unified Flagship Clock Face (synced with Big Clock & Settings)
            F8ClockFace(
                currentTimeMs = currentTimeMs,
                style = resolvedStyle,
                accentColor = resolvedColor,
                isLarge = isLarge,
                isFullScreen = false
            )

            // Date directly under time
            Text(
                text = TimeUtils.formatDate(currentTimeMs),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 2.dp)
            )

            // Premium Ambient Weather & Battery Row directly under the clock (if enabled in Settings)
            if (showWeather || showBattery) {
                Row(
                    modifier = Modifier.padding(top = 10.dp),
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
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${weatherInfo?.tempC ?: 21}°C · ${weatherInfo?.cityName ?: weatherCity}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }

                    if (showWeather && showBattery) {
                        Text(
                            text = "·",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.40f),
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                    }

                    if (showBattery) {
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
    calendarStyleId: String = "DEFAULT",
    calendarColorId: String = "SIGNATURE_BLUE",
    isEditMode: Boolean = false,
    onDayClick: (year: Int, month: Int, day: Int) -> Unit = { _, _, _ -> },
    onMonthClick: () -> Unit = {},
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val calendarProvider = remember { CalendarDataProvider(context) }
    var hasPermission by remember { mutableStateOf(calendarProvider.hasCalendarPermission()) }

    val now = remember { Calendar.getInstance() }
    val currentYear = now.get(Calendar.YEAR)
    val currentMonth = now.get(Calendar.MONTH)
    val todayDay = now.get(Calendar.DAY_OF_MONTH)

    var eventsByDay by remember { mutableStateOf<Map<Int, List<CalendarEventInfo>>>(emptyMap()) }

    LaunchedEffect(Unit) {
        while (true) {
            hasPermission = calendarProvider.hasCalendarPermission()
            if (hasPermission) {
                eventsByDay = calendarProvider.getEventsForMonth(currentYear, currentMonth, maskTitle)
            }
            delay(20_000L)
        }
    }

    val monthName = remember(currentMonth) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(now.time).uppercase()
    }

    val resolvedStyle = remember(calendarStyleId) { CalendarStyle.fromId(calendarStyleId) }
    val resolvedColor = remember(calendarColorId) { ClockColorOption.fromCalendarDefault(calendarColorId).color }

    val weekDays = listOf("M", "T", "W", "T", "F", "S", "S")

    val daysInMonth = remember(currentYear, currentMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val leadingEmptyDays = remember(currentYear, currentMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val dow = cal.get(Calendar.DAY_OF_WEEK) // Sunday = 1, Monday = 2
        (dow - Calendar.MONDAY + 7) % 7
    }

    CardContainer(
        isEditMode = isEditMode,
        onMoveUp = onMoveUp,
        onMoveDown = onMoveDown,
        onRemove = onRemove
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            // Month Header (touching smoothly expands the whole overlay into full month calendar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp, start = 2.dp, end = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !isEditMode) { onMonthClick() }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monthName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = resolvedColor,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Expand Calendar",
                        tint = resolvedColor.copy(alpha = 0.65f),
                        modifier = Modifier.size(14.dp)
                    )
                }
                if (!hasPermission) {
                    Text(
                        text = "No Permission",
                        fontSize = 10.sp,
                        color = StatusWarning
                    )
                }
            }

            // Days of week header (M T W T F S S)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEachIndexed { index, dayName ->
                    val isWeekend = index >= 5
                    Text(
                        text = dayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isWeekend) resolvedColor.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.45f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Calendar Days Grid
            val totalCells = leadingEmptyDays + daysInMonth
            val numRows = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (r in 0 until numRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (c in 0..6) {
                            val cellIndex = r * 7 + c
                            val dayNum = cellIndex - leadingEmptyDays + 1
                            if (dayNum in 1..daysInMonth) {
                                val isToday = dayNum == todayDay
                                val dayEvents = eventsByDay[dayNum]
                                val hasEvents = !dayEvents.isNullOrEmpty()

                                StyledCalendarDayCell(
                                    dayNum = dayNum,
                                    isToday = isToday,
                                    hasEvents = hasEvents,
                                    style = resolvedStyle,
                                    accentColor = resolvedColor,
                                    isCompact = true,
                                    onClick = { onDayClick(currentYear, currentMonth, dayNum) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
