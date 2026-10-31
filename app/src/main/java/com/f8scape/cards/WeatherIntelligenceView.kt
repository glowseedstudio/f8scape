package com.f8scape.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f8scape.app.theme.SamsungBlue
import com.f8scape.app.theme.TextSecondary
import com.f8scape.app.theme.TextTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Premium Full-Screen Weather Intelligence Page for F8scape StandBy.
 * Displays live weather, hourly rain probabilities, and a 7-day outlook
 * formatted for the Galaxy Z Fold 8 landscape cover screen.
 */
@Composable
fun WeatherIntelligenceView(
    cityName: String,
    weatherTempUnit: String = "SYSTEM",
    expansionProgress: Float = 1f,
    onDoubleTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val weatherProvider = remember { WeatherDataProvider(context) }
    var intelligence by remember { mutableStateOf<WeatherIntelligence?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(cityName) {
        isLoading = true
        while (isActive) {
            val data = weatherProvider.getWeatherIntelligence(cityName)
            intelligence = data
            isLoading = false
            // Refresh every 15 minutes
            delay(15 * 60 * 1000L)
        }
    }

    val currentData = intelligence

    val heroGlideOffsetX = -45f * (1f - expansionProgress)
    val heroScale = 0.92f + (0.08f * expansionProgress)
    val heroAlpha = (expansionProgress * 1.8f - 0.2f).coerceIn(0f, 1f)

    val outlookGlideOffsetX = 45f * (1f - expansionProgress)
    val outlookScale = 0.92f + (0.08f * expansionProgress)
    val outlookAlpha = (expansionProgress * 1.8f - 0.2f).coerceIn(0f, 1f)

    val dividerScaleY = 0.7f + (0.3f * expansionProgress)
    val dividerAlpha = (expansionProgress * 2.0f - 1.0f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap?.invoke() }
                )
            }
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        if (currentData == null && isLoading) {
            // Ambient loading indicator
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = heroAlpha
                        scaleX = heroScale
                        scaleY = heroScale
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        color = SamsungBlue,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Loading weather intelligence for $cityName...",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        } else if (currentData != null) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // --- LEFT HERO COLUMN (~38% width) ---
                Column(
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            translationX = heroGlideOffsetX.dp.toPx()
                            scaleX = heroScale
                            scaleY = heroScale
                            alpha = heroAlpha
                        },
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Location & Live Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = SamsungBlue,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = currentData.cityName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentData.isCached) {
                            Text(
                                text = "· Cached",
                                fontSize = 11.sp,
                                color = TextTertiary
                            )
                        }
                    }

                    // Main Temperature Hero Block
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val heroIcon = mapWeatherIcon(currentData.weatherCode, currentData.condition)
                            val iconColor = mapWeatherColor(currentData.weatherCode, currentData.condition)
                            Icon(
                                imageVector = heroIcon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = TemperatureUnit.formatTemp(currentData.currentTempC, weatherTempUnit),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = (-1).sp
                            )
                        }

                        Text(
                            text = currentData.condition,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "H: ${TemperatureUnit.formatTemp(currentData.maxTempC, weatherTempUnit)}  ·  L: ${TemperatureUnit.formatTemp(currentData.minTempC, weatherTempUnit)}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "·  Feels like ${TemperatureUnit.formatTemp(currentData.apparentTempC, weatherTempUnit)}",
                                fontSize = 11.5.sp,
                                color = TextTertiary
                            )
                        }
                    }

                    // 2x2 Intelligence Metrics Grid
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MetricTile(
                                icon = Icons.Default.WaterDrop,
                                label = "Humidity",
                                value = "${currentData.humidityPercent}%",
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                icon = Icons.Default.Air,
                                label = "Wind",
                                value = "${currentData.windSpeedKmH.toInt()} km/h",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MetricTile(
                                icon = Icons.Default.WbSunny,
                                label = "UV Index",
                                value = String.format("%.1f", currentData.uvIndex),
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                icon = Icons.Default.Umbrella,
                                label = "Rain Chance",
                                value = "${currentData.rainProbability}%",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Sunrise / Sunset Strip
                    if (currentData.sunrise.isNotBlank() && currentData.sunset.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("🌅", fontSize = 12.sp)
                                Text(
                                    text = currentData.sunrise,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("🌇", fontSize = 12.sp)
                                Text(
                                    text = currentData.sunset,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // Vertical Frosted Divider
                Box(
                    modifier = Modifier
                        .width(0.5.dp)
                        .fillMaxHeight(0.85f)
                        .graphicsLayer {
                            scaleY = dividerScaleY
                            alpha = dividerAlpha
                        }
                        .background(Color.White.copy(alpha = 0.10f))
                )

                // --- RIGHT OUTLOOK COLUMN (~62% width) ---
                Column(
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            translationX = outlookGlideOffsetX.dp.toPx()
                            scaleX = outlookScale
                            scaleY = outlookScale
                            alpha = outlookAlpha
                        },
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. Hourly Forecast Bar
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "HOURLY FORECAST",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(currentData.hourlyForecast) { item ->
                                HourlyForecastCapsule(
                                    item = item,
                                    weatherTempUnit = weatherTempUnit
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. 7-Day Forecast
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Text(
                            text = "7-DAY OUTLOOK",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.10f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                items(currentData.dailyForecast) { day ->
                                    DailyForecastRow(
                                        day = day,
                                        weatherTempUnit = weatherTempUnit,
                                        weekMin = currentData.dailyForecast.minOfOrNull { it.minTempC } ?: 0,
                                        weekMax = currentData.dailyForecast.maxOfOrNull { it.maxTempC } ?: 30
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
 * Single frosted intelligence tile for metrics like Humidity, Wind, UV, Rain.
 */
@Composable
private fun MetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.09f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(13.dp)
            )
            Column {
                Text(
                    text = label,
                    fontSize = 9.5.sp,
                    color = TextTertiary,
                    maxLines = 1
                )
                Text(
                    text = value,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Hourly Forecast Capsule in the horizontal strip.
 */
@Composable
private fun HourlyForecastCapsule(
    item: HourlyForecast,
    weatherTempUnit: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.11f)),
        modifier = Modifier.width(58.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.timeLabel,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1
            )

            val icon = mapWeatherIcon(item.weatherCode)
            val iconColor = mapWeatherColor(item.weatherCode)
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )

            if (item.rainProbability > 10) {
                Text(
                    text = "${item.rainProbability}%",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4FC3F7)
                )
            } else {
                Spacer(modifier = Modifier.height(11.dp))
            }

            Text(
                text = TemperatureUnit.formatTemp(item.tempC, weatherTempUnit),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Daily Forecast Row in the 7-day outlook.
 */
@Composable
private fun DailyForecastRow(
    day: DailyForecast,
    weatherTempUnit: String,
    weekMin: Int,
    weekMax: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day Label (e.g. "Today", "Sun")
        Text(
            text = day.dayLabel,
            fontSize = 12.sp,
            fontWeight = if (day.dayLabel == "Today") FontWeight.Bold else FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.width(48.dp)
        )

        // Weather Icon
        val icon = mapWeatherIcon(day.weatherCode, day.condition)
        val iconColor = mapWeatherColor(day.weatherCode, day.condition)
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Rain Probability Pill
        if (day.rainProbability > 15) {
            Text(
                text = "💧 ${day.rainProbability}%",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4FC3F7),
                modifier = Modifier.width(46.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(46.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // Min Temp
        Text(
            text = TemperatureUnit.formatTemp(day.minTempC, weatherTempUnit),
            fontSize = 11.5.sp,
            color = TextTertiary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(36.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Temperature range gradient indicator
        val span = (weekMax - weekMin).coerceAtLeast(1)
        val startFraction = ((day.minTempC - weekMin).toFloat() / span).coerceIn(0f, 1f)
        val endFraction = ((day.maxTempC - weekMin).toFloat() / span).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .width(64.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (endFraction - startFraction).coerceAtLeast(0.15f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF4FC3F7),
                                Color(0xFFFFB74D)
                            )
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Max Temp
        Text(
            text = TemperatureUnit.formatTemp(day.maxTempC, weatherTempUnit),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.End,
            modifier = Modifier.width(36.dp)
        )
    }
}

private fun mapWeatherIcon(code: Int, condition: String = ""): ImageVector {
    return when (code) {
        0 -> Icons.Default.WbSunny
        1, 2, 3 -> Icons.Default.Cloud
        45, 48 -> Icons.Default.Cloud
        51, 53, 55, 61, 63, 65 -> Icons.Default.WaterDrop
        71, 73, 75, 77 -> Icons.Default.AcUnit
        80, 81, 82 -> Icons.Default.WaterDrop
        95, 96, 99 -> Icons.Default.Bolt
        else -> if (condition.contains("Rain", ignoreCase = true)) Icons.Default.WaterDrop else Icons.Default.WbSunny
    }
}

private fun mapWeatherColor(code: Int, condition: String = ""): Color {
    return when (code) {
        0 -> Color(0xFFFFD54F)
        1, 2, 3 -> Color(0xFFE0E0E0)
        45, 48 -> Color(0xFFB0BEC5)
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> Color(0xFF4FC3F7)
        71, 73, 75, 77 -> Color(0xFFB3E5FC)
        95, 96, 99 -> Color(0xFFFFE082)
        else -> Color(0xFFFFD54F)
    }
}
