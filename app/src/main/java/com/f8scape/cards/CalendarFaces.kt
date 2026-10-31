package com.f8scape.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f8scape.app.theme.PureBlack

private val CenteredNumberTextStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

/**
 * Renders a single day cell inside either the compact AmbientCalendarCard or the full-screen
 * ExpandedMonthCalendarView according to the user's selected [CalendarStyle], [accentColor],
 * and [fullColor] mode.
 *
 * Uses a single-center Box architecture with [PlatformTextStyle(includeFontPadding = false)]
 * so every day numeral sits on the exact same horizontal baseline and is mathematically centered
 * inside circles, rings, and tiles without being pushed off-center by event dots.
 */
@Composable
fun StyledCalendarDayCell(
    dayNum: Int,
    isToday: Boolean,
    hasEvents: Boolean,
    style: CalendarStyle,
    accentColor: Color,
    isCompact: Boolean,
    fullColor: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cellHeight: Dp = if (isCompact) 28.dp else 44.dp
    val badgeSize: Dp = if (isCompact) 24.dp else 36.dp
    val fontSize: TextUnit = if (isCompact) 11.sp else 15.sp
    val isLightAccent = accentColor == Color.White ||
        (accentColor.red * 0.299f + accentColor.green * 0.587f + accentColor.blue * 0.114f) > 0.65f
    val onAccentTextColor = if (isLightAccent) PureBlack else Color.White
    val regularTextColor = if (fullColor) accentColor.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.90f)
    val clickModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier

    when (style) {
        CalendarStyle.DEFAULT -> {
            Box(
                modifier = modifier
                    .height(cellHeight)
                    .clip(CircleShape)
                    .then(clickModifier),
                contentAlignment = Alignment.Center
            ) {
                if (isToday) {
                    Box(
                        modifier = Modifier
                            .size(badgeSize)
                            .background(accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayNum.toString(),
                            style = CenteredNumberTextStyle,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            color = onAccentTextColor,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = dayNum.toString(),
                        style = CenteredNumberTextStyle,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Medium,
                        color = regularTextColor,
                        textAlign = TextAlign.Center
                    )
                    if (hasEvents) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = if (isCompact) 1.dp else 3.dp)
                                .size(if (isCompact) 3.5.dp else 4.5.dp)
                                .background(accentColor, CircleShape)
                        )
                    }
                }
            }
        }

        CalendarStyle.ROUNDED_TILES -> {
            val tileShape = RoundedCornerShape(if (isCompact) 7.dp else 11.dp)
            val tileSize = if (isCompact) 25.dp else 38.dp
            Box(
                modifier = modifier
                    .height(cellHeight)
                    .then(clickModifier),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = if (isCompact) 28.dp else 42.dp, height = tileSize)
                        .clip(tileShape)
                        .background(
                            when {
                                isToday -> accentColor
                                hasEvents -> accentColor.copy(alpha = 0.16f)
                                fullColor -> accentColor.copy(alpha = 0.08f)
                                else -> Color.White.copy(alpha = 0.06f)
                            }
                        )
                        .then(
                            if (hasEvents && !isToday) {
                                Modifier.border(1.dp, accentColor.copy(alpha = 0.50f), tileShape)
                            } else if (fullColor && !isToday) {
                                Modifier.border(0.5.dp, accentColor.copy(alpha = 0.22f), tileShape)
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayNum.toString(),
                        style = CenteredNumberTextStyle,
                        fontSize = fontSize,
                        fontWeight = if (isToday || hasEvents) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isToday -> onAccentTextColor
                            hasEvents -> accentColor
                            else -> regularTextColor
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        CalendarStyle.MINIMAL_RING -> {
            Box(
                modifier = modifier
                    .height(cellHeight)
                    .clip(CircleShape)
                    .then(clickModifier),
                contentAlignment = Alignment.Center
            ) {
                if (isToday) {
                    Box(
                        modifier = Modifier
                            .size(badgeSize)
                            .then(
                                if (fullColor) Modifier.background(accentColor.copy(alpha = 0.22f), CircleShape) else Modifier
                            )
                            .border(if (isCompact) 1.5.dp else 2.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayNum.toString(),
                            style = CenteredNumberTextStyle,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = dayNum.toString(),
                        style = CenteredNumberTextStyle,
                        fontSize = fontSize,
                        fontWeight = if (hasEvents) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (hasEvents) accentColor else regularTextColor,
                        textAlign = TextAlign.Center
                    )
                    if (hasEvents) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = if (isCompact) 1.5.dp else 3.dp)
                                .width(if (isCompact) 10.dp else 14.dp)
                                .height(if (isCompact) 1.5.dp else 2.dp)
                                .background(accentColor, RoundedCornerShape(1.dp))
                        )
                    }
                }
            }
        }

        CalendarStyle.RETRO_MATRIX -> {
            val matrixShape = RoundedCornerShape(if (isCompact) 5.dp else 8.dp)
            val boxW = if (isCompact) 28.dp else 42.dp
            val boxH = if (isCompact) 24.dp else 36.dp
            Box(
                modifier = modifier
                    .height(cellHeight)
                    .then(clickModifier),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = boxW, height = boxH)
                        .clip(matrixShape)
                        .then(
                            if (isToday) {
                                Modifier
                                    .background(accentColor.copy(alpha = 0.20f))
                                    .border(if (isCompact) 1.2.dp else 1.8.dp, accentColor, matrixShape)
                            } else if (hasEvents) {
                                Modifier.border(1.dp, accentColor.copy(alpha = 0.45f), matrixShape)
                            } else if (fullColor) {
                                Modifier.border(0.5.dp, accentColor.copy(alpha = 0.20f), matrixShape)
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", dayNum),
                        style = CenteredNumberTextStyle,
                        fontSize = if (isCompact) 10.sp else 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isToday || hasEvents) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isToday -> accentColor
                            hasEvents -> accentColor
                            else -> regularTextColor
                        },
                        textAlign = TextAlign.Center
                    )
                    if (hasEvents && !isToday) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = if (isCompact) 1.5.dp else 2.5.dp)
                                .size(if (isCompact) 3.dp else 4.dp)
                                .background(accentColor)
                        )
                    }
                }
            }
        }

        CalendarStyle.EDITORIAL_BOLD -> {
            Box(
                modifier = modifier
                    .height(cellHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .then(clickModifier),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayNum.toString(),
                    style = CenteredNumberTextStyle,
                    fontSize = if (isCompact) 12.sp else 17.sp,
                    fontWeight = if (isToday) FontWeight.Black else FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = when {
                        isToday -> accentColor
                        hasEvents -> if (fullColor) accentColor else Color.White
                        else -> regularTextColor
                    },
                    textAlign = TextAlign.Center
                )
                if (isToday || hasEvents) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (isCompact) 1.5.dp else 3.dp)
                            .width(if (isToday) (if (isCompact) 14.dp else 22.dp) else (if (isCompact) 6.dp else 10.dp))
                            .height(if (isCompact) 2.dp else 3.dp)
                            .background(
                                if (isToday) accentColor else accentColor.copy(alpha = 0.75f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}
