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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f8scape.app.theme.PureBlack

/**
 * Renders a single day cell inside either the compact AmbientCalendarCard or the full-screen
 * ExpandedMonthCalendarView according to the user's selected [CalendarStyle] and [accentColor].
 */
@Composable
fun StyledCalendarDayCell(
    dayNum: Int,
    isToday: Boolean,
    hasEvents: Boolean,
    style: CalendarStyle,
    accentColor: Color,
    isCompact: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cellHeight: Dp = if (isCompact) 28.dp else 44.dp
    val badgeSize: Dp = if (isCompact) 21.dp else 34.dp
    val fontSize: TextUnit = if (isCompact) 11.sp else 16.sp
    val isLightAccent = accentColor == Color.White ||
        (accentColor.red * 0.299f + accentColor.green * 0.587f + accentColor.blue * 0.114f) > 0.65f
    val onAccentTextColor = if (isLightAccent) PureBlack else Color.White
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
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            color = onAccentTextColor,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = dayNum.toString(),
                            fontSize = fontSize,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.88f),
                            textAlign = TextAlign.Center
                        )
                        if (hasEvents) {
                            Box(
                                modifier = Modifier
                                    .padding(top = if (isCompact) 1.5.dp else 3.dp)
                                    .size(if (isCompact) 3.dp else 4.5.dp)
                                    .background(accentColor, CircleShape)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 7.dp))
                        }
                    }
                }
            }
        }

        CalendarStyle.ROUNDED_TILES -> {
            val tileShape = RoundedCornerShape(if (isCompact) 7.dp else 11.dp)
            Box(
                modifier = modifier
                    .padding(horizontal = if (isCompact) 1.5.dp else 3.dp, vertical = 1.dp)
                    .height(cellHeight)
                    .clip(tileShape)
                    .background(
                        when {
                            isToday -> accentColor
                            hasEvents -> accentColor.copy(alpha = 0.14f)
                            else -> Color.White.copy(alpha = 0.05f)
                        }
                    )
                    .then(
                        if (hasEvents && !isToday) {
                            Modifier.border(1.dp, accentColor.copy(alpha = 0.45f), tileShape)
                        } else Modifier
                    )
                    .then(clickModifier),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayNum.toString(),
                    fontSize = fontSize,
                    fontWeight = if (isToday || hasEvents) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        isToday -> onAccentTextColor
                        hasEvents -> accentColor
                        else -> Color.White.copy(alpha = 0.85f)
                    },
                    textAlign = TextAlign.Center
                )
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
                            .border(if (isCompact) 1.5.dp else 2.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayNum.toString(),
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = dayNum.toString(),
                            fontSize = fontSize,
                            fontWeight = FontWeight.Light,
                            color = if (hasEvents) accentColor else Color.White.copy(alpha = 0.80f),
                            textAlign = TextAlign.Center
                        )
                        if (hasEvents) {
                            Box(
                                modifier = Modifier
                                    .padding(top = if (isCompact) 1.5.dp else 2.dp)
                                    .width(if (isCompact) 10.dp else 16.dp)
                                    .height(if (isCompact) 1.5.dp else 2.dp)
                                    .background(accentColor, RoundedCornerShape(1.dp))
                            )
                        } else {
                            Spacer(modifier = Modifier.height(if (isCompact) 3.dp else 5.dp))
                        }
                    }
                }
            }
        }

        CalendarStyle.RETRO_MATRIX -> {
            val matrixShape = RoundedCornerShape(if (isCompact) 4.dp else 6.dp)
            Box(
                modifier = modifier
                    .padding(1.dp)
                    .height(cellHeight)
                    .clip(matrixShape)
                    .then(
                        if (isToday) {
                            Modifier
                                .background(accentColor.copy(alpha = 0.18f))
                                .border(if (isCompact) 1.2.dp else 1.8.dp, accentColor, matrixShape)
                        } else Modifier
                    )
                    .then(clickModifier),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = String.format("%02d", dayNum),
                        fontSize = if (isCompact) 10.sp else 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                        color = if (isToday) accentColor else accentColor.copy(alpha = 0.80f),
                        textAlign = TextAlign.Center
                    )
                    if (hasEvents) {
                        Box(
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(if (isCompact) 3.dp else 4.dp)
                                .background(accentColor)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 5.dp))
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayNum.toString(),
                        fontSize = if (isCompact) 12.sp else 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = when {
                            isToday -> accentColor
                            hasEvents -> Color.White
                            else -> Color.White.copy(alpha = 0.65f)
                        },
                        textAlign = TextAlign.Center
                    )
                    if (isToday || hasEvents) {
                        Box(
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .width(if (isToday) (if (isCompact) 14.dp else 22.dp) else (if (isCompact) 6.dp else 10.dp))
                                .height(if (isCompact) 2.5.dp else 3.5.dp)
                                .background(
                                    if (isToday) accentColor else accentColor.copy(alpha = 0.70f),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    } else {
                        Spacer(modifier = Modifier.height(if (isCompact) 3.5.dp else 4.5.dp))
                    }
                }
            }
        }
    }
}
