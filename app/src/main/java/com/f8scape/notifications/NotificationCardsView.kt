package com.f8scape.notifications

import android.app.PendingIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f8scape.app.theme.SamsungBlue
import com.f8scape.app.theme.TextSecondary
import com.f8scape.app.theme.TextTertiary
import com.f8scape.cards.ClockColorOption
import com.f8scape.cards.ClockStyle
import com.f8scape.cards.F8ClockFace
import com.f8scape.util.TimeUtils

/**
 * Premium split notification view displayed when the user opts in and taps the notification
 * silhouette icons on the StandBy overlay.
 *
 * Left side: The ambient clock face, current date, and quick return controls.
 * Right side: Frosted notification cards showing the app icon, title, message body,
 * and actionable controls.
 */
@Composable
fun NotificationCardsView(
    currentTimeMs: Long,
    clockStyleId: String,
    clockColorId: String,
    clockFullColor: Boolean,
    notifications: List<StatusBarNotificationIcon>,
    onDismissCard: (String) -> Unit,
    onOpenNotification: (PendingIntent) -> Unit,
    onClose: () -> Unit,
    onDoubleTap: () -> Unit,
    transitionProgress: Float = 1f,
    modifier: Modifier = Modifier
) {
    val resolvedStyle = remember(clockStyleId) { ClockStyle.fromId(clockStyleId) }
    val resolvedColorOption = remember(clockColorId) { ClockColorOption.fromId(clockColorId) }
    val accentColor = resolvedColorOption.color

    val leftGlideOffsetX = -45f * (1f - transitionProgress)
    val leftScale = 0.92f + (0.08f * transitionProgress)
    val leftAlpha = (transitionProgress * 1.8f - 0.2f).coerceIn(0f, 1f)

    val rightGlideOffsetX = 45f * (1f - transitionProgress)
    val rightScale = 0.92f + (0.08f * transitionProgress)
    val rightAlpha = (transitionProgress * 1.8f - 0.2f).coerceIn(0f, 1f)

    val dividerScaleY = 0.7f + (0.3f * transitionProgress)
    val dividerAlpha = (transitionProgress * 2.0f - 1.0f).coerceIn(0f, 1f)

    Row(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap() }
                )
            }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- LEFT SIDE: The Time & Ambient Clock ---
        Column(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight()
                .graphicsLayer {
                    translationX = leftGlideOffsetX.dp.toPx()
                    scaleX = leftScale
                    scaleY = leftScale
                    alpha = leftAlpha
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ambient Clock Face
            F8ClockFace(
                currentTimeMs = currentTimeMs,
                style = resolvedStyle,
                accentColor = accentColor,
                isLarge = false,
                isFullScreen = false,
                fullColor = clockFullColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Formatted Date
            Text(
                text = TimeUtils.formatDate(currentTimeMs),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.82f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Return to StandBy Pill Button
            Surface(
                onClick = onClose,
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.16f)),
                modifier = Modifier.height(34.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Return to StandBy",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Back",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // Subtle frosted vertical divider
        Box(
            modifier = Modifier
                .width(0.5.dp)
                .fillMaxHeight(0.72f)
                .graphicsLayer {
                    scaleY = dividerScaleY
                    alpha = dividerAlpha
                }
                .background(Color.White.copy(alpha = 0.10f))
        )

        // --- RIGHT SIDE: Notification Cards ---
        Column(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight()
                .graphicsLayer {
                    translationX = rightGlideOffsetX.dp.toPx()
                    scaleX = rightScale
                    scaleY = rightScale
                    alpha = rightAlpha
                }
        ) {
            // Top Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Notifications",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.95f),
                        letterSpacing = (-0.3).sp
                    )

                    if (notifications.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(SamsungBlue.copy(alpha = 0.25f))
                                .border(BorderStroke(0.5.dp, SamsungBlue.copy(alpha = 0.5f)), RoundedCornerShape(10.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${notifications.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                if (notifications.size > 1) {
                    TextButton(
                        onClick = {
                            notifications.forEach { onDismissCard(it.key) }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Clear All",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Cards Content
            if (notifications.isEmpty()) {
                // Frosted empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "All Caught Up",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "No active notifications in shade",
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 6.dp)
                ) {
                    items(notifications, key = { it.key }) { card ->
                        NotificationCardItem(
                            card = card,
                            onDismiss = { onDismissCard(card.key) },
                            onOpen = {
                                card.contentIntent?.let { onOpenNotification(it) }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual frosted notification card with app branding, timestamp, and message body.
 */
@Composable
private fun NotificationCardItem(
    card: StatusBarNotificationIcon,
    onDismiss: () -> Unit,
    onOpen: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        onClick = onOpen,
        enabled = card.contentIntent != null,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF16161A).copy(alpha = 0.88f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f)),
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            // Header Row: App Icon + App Name + Subtext + Relative Time + Dismiss Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon (Full-color launcher icon or monochrome silhouette fallback)
                if (card.appIconBitmap != null) {
                    Image(
                        bitmap = card.appIconBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )
                } else if (card.iconBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = card.iconBitmap.asImageBitmap(),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(Color.White, BlendMode.SrcIn),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // App Name
                Text(
                    text = card.appName.ifBlank { card.packageName },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Optional SubText (e.g. channel or group name)
                if (card.subText.isNotBlank()) {
                    Text(
                        text = " • ${card.subText}",
                        fontSize = 11.sp,
                        color = TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Relative Time
                val relativeTime = TimeUtils.formatRelativeTime(card.postTime)
                if (relativeTime.isNotBlank()) {
                    Text(
                        text = relativeTime,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Dismiss 'x' Button
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Notification Title (Sender, subject, or heading)
            if (card.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = card.title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Notification Text (Body preview)
            if (card.text.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = card.text,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.80f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
