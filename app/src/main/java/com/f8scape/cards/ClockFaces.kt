package com.f8scape.cards

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f8scape.util.TimeUtils
import java.util.Calendar
import java.util.Locale

/**
 * Unified clock face renderer supporting all 6 flagship styles and 12 colors,
 * scaling cleanly between the standard dashboard card and full-screen BigClockView,
 * and automatically respecting the user's system 12-hour / 24-hour time preference.
 */
@Composable
fun F8ClockFace(
    currentTimeMs: Long,
    style: ClockStyle,
    accentColor: Color,
    isLarge: Boolean,
    isFullScreen: Boolean = false,
    fullColor: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val is24Hour = remember(context, currentTimeMs / 5000L) {
        DateFormat.is24HourFormat(context)
    }

    when (style) {
        ClockStyle.DEFAULT -> {
            Text(
                text = TimeUtils.formatClockTime(currentTimeMs, is24Hour = is24Hour),
                fontSize = when {
                    isFullScreen -> 148.sp
                    isLarge -> 88.sp
                    else -> 64.sp
                },
                fontWeight = if (isFullScreen) FontWeight.Bold else FontWeight.Light,
                letterSpacing = if (isFullScreen) (-3.5).sp else (-2.0).sp,
                color = accentColor,
                modifier = modifier
            )
        }

        ClockStyle.RETRO_DIGITAL -> {
            val digitHeight = when {
                isFullScreen -> 136.dp
                isLarge -> 72.dp
                else -> 54.dp
            }
            RetroLedDigitalClock(
                currentTimeMs = currentTimeMs,
                accentColor = accentColor,
                digitHeight = digitHeight,
                is24Hour = is24Hour,
                modifier = modifier.padding(vertical = 6.dp)
            )
        }

        ClockStyle.CHRONO_ANALOG -> {
            val dialSize = when {
                isFullScreen -> 284.dp
                isLarge -> 196.dp
                else -> 152.dp
            }
            HorologyAnalogClock(
                currentTimeMs = currentTimeMs,
                accentColor = accentColor,
                dialSize = dialSize,
                fullColor = fullColor,
                modifier = modifier.padding(vertical = 4.dp)
            )
        }

        ClockStyle.FLIP_CLOCK -> {
            MechanicalFlipClock(
                currentTimeMs = currentTimeMs,
                accentColor = accentColor,
                isFullScreen = isFullScreen,
                isLarge = isLarge,
                is24Hour = is24Hour,
                modifier = modifier
            )
        }

        ClockStyle.STACKED_BOLD -> {
            StackedBoldClock(
                currentTimeMs = currentTimeMs,
                accentColor = accentColor,
                isFullScreen = isFullScreen,
                isLarge = isLarge,
                is24Hour = is24Hour,
                fullColor = fullColor,
                modifier = modifier
            )
        }

        ClockStyle.NEON_STENCIL -> {
            NeonStencilClock(
                currentTimeMs = currentTimeMs,
                accentColor = accentColor,
                isFullScreen = isFullScreen,
                isLarge = isLarge,
                is24Hour = is24Hour,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun MechanicalFlipClock(
    currentTimeMs: Long,
    accentColor: Color,
    isFullScreen: Boolean,
    isLarge: Boolean,
    is24Hour: Boolean,
    modifier: Modifier = Modifier
) {
    val cal = remember(currentTimeMs) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMs }
    }
    val displayHour = if (is24Hour) {
        cal.get(Calendar.HOUR_OF_DAY)
    } else {
        val raw = cal.get(Calendar.HOUR)
        if (raw == 0) 12 else raw
    }
    val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
    val minute = cal.get(Calendar.MINUTE)
    val hourStr = String.format(Locale.US, "%02d", displayHour)
    val minStr = String.format(Locale.US, "%02d", minute)

    val cardWidth = when {
        isFullScreen -> 156.dp
        isLarge -> 92.dp
        else -> 70.dp
    }
    val cardHeight = when {
        isFullScreen -> 146.dp
        isLarge -> 86.dp
        else -> 66.dp
    }
    val fontSize = when {
        isFullScreen -> 96.sp
        isLarge -> 52.sp
        else -> 40.sp
    }

    Row(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(if (isFullScreen) 18.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlipFlapCard(
            text = hourStr,
            badgeText = if (!is24Hour) amPm else null,
            color = accentColor,
            width = cardWidth,
            height = cardHeight,
            fontSize = fontSize,
            isFullScreen = isFullScreen
        )
        FlipFlapCard(
            text = minStr,
            badgeText = null,
            color = accentColor,
            width = cardWidth,
            height = cardHeight,
            fontSize = fontSize,
            isFullScreen = isFullScreen
        )
    }
}

@Composable
private fun FlipFlapCard(
    text: String,
    badgeText: String?,
    color: Color,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    isFullScreen: Boolean
) {
    val cornerRad = if (isFullScreen) 22.dp else 16.dp
    Box(
        modifier = Modifier
            .size(width, height)
            .clip(RoundedCornerShape(cornerRad))
            .background(Color(0xFF17171B))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(cornerRad)),
        contentAlignment = Alignment.Center
    ) {
        if (badgeText != null) {
            Text(
                text = badgeText,
                fontSize = if (isFullScreen) 12.sp else 9.sp,
                fontWeight = FontWeight.Bold,
                color = color.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = if (isFullScreen) 12.dp else 8.dp, top = if (isFullScreen) 10.dp else 6.dp)
            )
        }
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = (-1.5).sp,
            color = color
        )
        // Mechanical split-flap horizontal hinge line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFullScreen) 2.5.dp else 2.dp)
                .background(Color.Black.copy(alpha = 0.85f))
        )
    }
}

@Composable
private fun StackedBoldClock(
    currentTimeMs: Long,
    accentColor: Color,
    isFullScreen: Boolean,
    isLarge: Boolean,
    is24Hour: Boolean,
    fullColor: Boolean,
    modifier: Modifier = Modifier
) {
    val cal = remember(currentTimeMs) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMs }
    }
    val displayHour = if (is24Hour) {
        cal.get(Calendar.HOUR_OF_DAY)
    } else {
        val raw = cal.get(Calendar.HOUR)
        if (raw == 0) 12 else raw
    }
    val minute = cal.get(Calendar.MINUTE)
    val second = cal.get(Calendar.SECOND)
    val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"

    val mainFontSize = when {
        isFullScreen -> 128.sp
        isLarge -> 72.sp
        else -> 54.sp
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isFullScreen) 16.dp else 12.dp)
    ) {
        Text(
            text = String.format(Locale.US, "%02d", displayHour),
            fontSize = mainFontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = (-3.0).sp,
            color = if (fullColor) accentColor else Color.White
        )
        Text(
            text = String.format(Locale.US, "%02d", minute),
            fontSize = mainFontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = (-3.0).sp,
            color = if (fullColor) accentColor.copy(alpha = 0.78f) else accentColor
        )
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            if (!is24Hour) {
                Text(
                    text = amPm,
                    fontSize = if (isFullScreen) 20.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = accentColor
                )
            }
            Text(
                text = String.format(Locale.US, ":%02d", second),
                fontSize = if (isFullScreen) 22.sp else 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = if (fullColor) accentColor.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun NeonStencilClock(
    currentTimeMs: Long,
    accentColor: Color,
    isFullScreen: Boolean,
    isLarge: Boolean,
    is24Hour: Boolean,
    modifier: Modifier = Modifier
) {
    val cal = remember(currentTimeMs) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMs }
    }
    val second = cal.get(Calendar.SECOND)
    val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
    val timeText = TimeUtils.formatClockTime(currentTimeMs, is24Hour = is24Hour)
    val fontSize = when {
        isFullScreen -> 136.sp
        isLarge -> 82.sp
        else -> 60.sp
    }
    // Generous positive letter-spacing so adjacent neon tube strokes never overlap
    val tubeSpacing = when {
        isFullScreen -> 9.sp
        isLarge -> 6.sp
        else -> 4.sp
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Soft ambient outer halo
            Text(
                text = timeText,
                style = TextStyle(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = tubeSpacing,
                    color = accentColor.copy(alpha = 0.20f),
                    drawStyle = Stroke(
                        width = if (isFullScreen) 11f else 8f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            )
            // Crisp geometric neon tube outline stroke
            Text(
                text = timeText,
                style = TextStyle(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = tubeSpacing,
                    color = accentColor,
                    drawStyle = Stroke(
                        width = if (isFullScreen) 4f else 3f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            )
        }
        Spacer(modifier = Modifier.width(if (isFullScreen) 10.dp else 6.dp))
        Column(
            modifier = Modifier.padding(bottom = if (isFullScreen) 22.dp else 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (!is24Hour) {
                Text(
                    text = amPm,
                    fontSize = if (isFullScreen) 16.sp else 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = accentColor.copy(alpha = 0.85f)
                )
            }
            Text(
                text = String.format(Locale.US, "%02d", second),
                fontSize = if (isFullScreen) 24.sp else 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = accentColor.copy(alpha = 0.85f)
            )
        }
    }
}

/**
 * Interactive Live Clock Studio Editor overlay triggered by touching & holding the Big Clock.
 * Changes made here immediately sync via PreferencesRepository across all screens and Settings.
 */
@Composable
fun LiveClockEditorPanel(
    selectedStyle: ClockStyle,
    selectedColor: ClockColorOption,
    fullColor: Boolean = false,
    onSelectStyle: (ClockStyle) -> Unit,
    onSelectColor: (ClockColorOption) -> Unit,
    onToggleFullColor: (Boolean) -> Unit = {},
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF141418).copy(alpha = 0.96f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CLOCK STUDIO · ${selectedStyle.displayName.uppercase()}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = Color.White.copy(alpha = 0.70f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { onToggleFullColor(false) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (!fullColor) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, if (!fullColor) selectedColor.color else Color.White.copy(alpha = 0.12f))
                ) {
                    Text(
                        text = "Highlights",
                        fontSize = 11.sp,
                        fontWeight = if (!fullColor) FontWeight.Bold else FontWeight.Medium,
                        color = if (!fullColor) Color.White else Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    onClick = { onToggleFullColor(true) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (fullColor) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, if (fullColor) selectedColor.color else Color.White.copy(alpha = 0.12f))
                ) {
                    Text(
                        text = "Full Colour",
                        fontSize = 11.sp,
                        fontWeight = if (fullColor) FontWeight.Bold else FontWeight.Medium,
                        color = if (fullColor) Color.White else Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    onClick = onDone,
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF2E75D3)
                ) {
                    Text(
                        text = "Done",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Style Selector Pills
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ClockStyle.entries, key = { it.id }) { style ->
                val isSelected = style == selectedStyle
                Surface(
                    onClick = { onSelectStyle(style) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) selectedColor.color else Color.White.copy(alpha = 0.12f)
                    )
                ) {
                    Text(
                        text = style.displayName,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Color Palette Swatches (Classic, LED Red/Amber, Soft Pastels)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(ClockColorOption.entries, key = { it.id }) { option ->
                val isSelected = option == selectedColor
                Surface(
                    onClick = { onSelectColor(option) },
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
                            androidx.compose.material3.Icon(
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

/**
 * Interactive Live Calendar Studio Editor overlay triggered by touching & holding the Full Calendar.
 * Changes made here immediately sync via PreferencesRepository across the Home Card, Full Calendar, and Settings.
 */
@Composable
fun LiveCalendarEditorPanel(
    selectedStyle: CalendarStyle,
    selectedColor: ClockColorOption,
    fullColor: Boolean = false,
    onSelectStyle: (CalendarStyle) -> Unit,
    onSelectColor: (ClockColorOption) -> Unit,
    onToggleFullColor: (Boolean) -> Unit = {},
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF141418).copy(alpha = 0.96f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CALENDAR STUDIO · ${selectedStyle.displayName.uppercase()}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = Color.White.copy(alpha = 0.70f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { onToggleFullColor(false) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (!fullColor) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, if (!fullColor) selectedColor.color else Color.White.copy(alpha = 0.12f))
                ) {
                    Text(
                        text = "Highlights",
                        fontSize = 11.sp,
                        fontWeight = if (!fullColor) FontWeight.Bold else FontWeight.Medium,
                        color = if (!fullColor) Color.White else Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    onClick = { onToggleFullColor(true) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (fullColor) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, if (fullColor) selectedColor.color else Color.White.copy(alpha = 0.12f))
                ) {
                    Text(
                        text = "Full Colour",
                        fontSize = 11.sp,
                        fontWeight = if (fullColor) FontWeight.Bold else FontWeight.Medium,
                        color = if (fullColor) Color.White else Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    onClick = onDone,
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF2E75D3)
                ) {
                    Text(
                        text = "Done",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar Style Selector Pills
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CalendarStyle.entries, key = { it.id }) { style ->
                val isSelected = style == selectedStyle
                Surface(
                    onClick = { onSelectStyle(style) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) selectedColor.color else Color.White.copy(alpha = 0.12f)
                    )
                ) {
                    Text(
                        text = style.displayName,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Color Palette Swatches
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(ClockColorOption.entries, key = { it.id }) { option ->
                val isSelected = option == selectedColor
                Surface(
                    onClick = { onSelectColor(option) },
                    shape = CircleShape,
                    color = option.color,
                    border = BorderStroke(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSelected) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = option.displayName,
                                tint = if (option == ClockColorOption.PURE_WHITE || option.isPastel) Color.Black else Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

