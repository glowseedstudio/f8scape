package com.f8scape.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.f8scape.app.theme.AccentBlue
import com.f8scape.app.theme.StatusSuccess
import com.f8scape.app.theme.TextSecondary
import com.f8scape.app.theme.TextTertiary
import com.f8scape.service.CoverOverlayService

private val SetupBg = Color(0xFF0A0A0C)
private val SetupCardBg = Color(0xFF161618)

@Composable
fun FirstOpenSetupScreen(
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isAccessibilityActive by remember { mutableStateOf(CoverOverlayService.isServiceRunning) }
    var isOverlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    // Refresh permission states automatically whenever the user returns from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityActive = CoverOverlayService.isServiceRunning
                isOverlayGranted = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val allRequiredReady = isAccessibilityActive && isOverlayGranted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SetupBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(
                    text = "WELCOME TO F8SCAPE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = AccentBlue
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Set up your Fold 8 cover screen",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 34.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Grant the two core permissions below so F8scape can automatically detect tent posture and wake your cover display. Optional features like Music, Calendar, and Weather can be enabled anytime on the Home screen.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Step 1: Accessibility Service
            RequiredPermissionStepCard(
                stepNumber = "1",
                title = "StandBy Accessibility Host",
                description = "Required to detect when your Fold 8 is propped in tent mode and display the ambient cover screen without manual launching.",
                icon = Icons.Default.PhonelinkSetup,
                isGranted = isAccessibilityActive,
                actionLabel = if (isAccessibilityActive) "Enabled" else "Enable in Settings",
                onActionClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            )

            // Step 2: Appear on Top Overlay
            RequiredPermissionStepCard(
                stepNumber = "2",
                title = "Appear on Top (Cover Overlay)",
                description = "Required so the full-screen StandBy clock and dashboard can render cleanly over the outer display.",
                icon = Icons.Default.Layers,
                isGranted = isOverlayGranted,
                actionLabel = if (isOverlayGranted) "Granted" else "Grant Permission",
                onActionClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Bottom CTA area
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                onClick = onSetupComplete,
                shape = RoundedCornerShape(20.dp),
                color = if (allRequiredReady) AccentBlue else Color(0xFF26262A),
                border = if (allRequiredReady) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (allRequiredReady) "Finish Setup & Open Home" else "Continue to Home (Configure Later)",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Everything runs 100% locally on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun RequiredPermissionStepCard(
    stepNumber: String,
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SetupCardBg,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isGranted) StatusSuccess.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (isGranted) StatusSuccess.copy(alpha = 0.16f) else AccentBlue.copy(alpha = 0.16f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGranted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = StatusSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "REQUIRED STEP $stepNumber",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp,
                            color = if (isGranted) StatusSuccess else TextTertiary
                        )
                        Text(
                            text = title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            Text(
                text = description,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 19.sp
            )

            Surface(
                onClick = onActionClick,
                shape = RoundedCornerShape(14.dp),
                color = if (isGranted) StatusSuccess.copy(alpha = 0.14f) else AccentBlue,
                border = if (isGranted) BorderStroke(0.5.dp, StatusSuccess.copy(alpha = 0.4f)) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isGranted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = StatusSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = actionLabel,
                        color = if (isGranted) StatusSuccess else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isGranted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
