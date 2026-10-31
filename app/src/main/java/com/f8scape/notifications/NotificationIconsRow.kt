package com.f8scape.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember

/**
 * Premium frosted ambient status bar notification cluster displaying monochrome silhouette
 * icons for active incoming notifications (e.g. Messages, WhatsApp, Mail, Calendar).
 * If isInteractive is enabled in settings, tapping the cluster triggers onClick to open
 * the split notification cards view.
 */
@Composable
fun NotificationIconsRow(
    icons: List<StatusBarNotificationIcon>,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    if (icons.isEmpty()) return

    val interactionSource = remember { MutableInteractionSource() }
    val baseModifier = modifier
        .clip(RoundedCornerShape(12.dp))
        .then(
            if (isInteractive && onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                    onClick = onClick
                )
            } else Modifier
        )
        .background(
            if (isInteractive) Color.White.copy(alpha = 0.09f)
            else Color.White.copy(alpha = 0.07f)
        )
        .border(
            BorderStroke(
                0.5.dp,
                if (isInteractive) Color.White.copy(alpha = 0.20f)
                else Color.White.copy(alpha = 0.12f)
            ),
            RoundedCornerShape(12.dp)
        )
        .padding(horizontal = 9.dp, vertical = 4.dp)

    Row(
        modifier = baseModifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icons.forEach { item ->
            item.iconBitmap?.let { bmp ->
                Box(
                    modifier = Modifier.size(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.85f), BlendMode.SrcIn),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
