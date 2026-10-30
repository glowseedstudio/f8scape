package com.f8scape.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.f8scape.R
import com.f8scape.app.theme.AccentBlue
import com.f8scape.app.theme.StatusSuccess
import com.f8scape.app.theme.TextSecondary
import com.f8scape.app.theme.TextTertiary
import com.f8scape.service.CoverOverlayService
import kotlinx.coroutines.launch

private val SetupBg = Color(0xFF09090B)
private val CardBg = Color(0xFF141417)
private val SuccessGreen = Color(0xFF4CAF50)

@Composable
fun FirstOpenSetupScreen(
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var isAccessibilityActive by remember { mutableStateOf(CoverOverlayService.isServiceRunning) }
    var isOverlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    // Auto-refresh states whenever returning from system settings
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

    val pagerState = rememberPagerState(pageCount = { 4 })

    // Auto-advance to completion page when both permissions are granted
    LaunchedEffect(isAccessibilityActive, isOverlayGranted) {
        if (isAccessibilityActive && isOverlayGranted && pagerState.currentPage in 1..2) {
            // Small pause so the user sees the animated tick mark
            kotlinx.coroutines.delay(650L)
            pagerState.animateScrollToPage(3)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SetupBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar: Back button when past page 0
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pagerState.currentPage > 0) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            // Skip to Home button if user prefers configuring later
            if (pagerState.currentPage < 3) {
                Surface(
                    onClick = onSetupComplete,
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent
                ) {
                    Text(
                        text = "Skip",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextTertiary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Main Multi-page content
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = true,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp, bottom = 100.dp)
        ) { page ->
            when (page) {
                0 -> WelcomePage(
                    onGetStarted = {
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    }
                )
                1 -> PermissionStepPage(
                    stepNumber = "1 OF 2",
                    title = "Tent Mode Detection",
                    subtitle = "Accessibility Service",
                    description = "F8scape needs lightweight Accessibility Service access to watch the Fold 8's hinge angle and wake the cover screen when tented.\n\nF8scape runs locally and never inspects screen text, passwords, or keystrokes.",
                    icon = Icons.Default.PhonelinkSetup,
                    isGranted = isAccessibilityActive,
                    actionLabel = if (isAccessibilityActive) "Enabled" else "Enable in Settings",
                    onActionClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    },
                    onNext = {
                        coroutineScope.launch { pagerState.animateScrollToPage(2) }
                    }
                )
                2 -> PermissionStepPage(
                    stepNumber = "2 OF 2",
                    title = "Appear on Top",
                    subtitle = "Cover Overlay Window",
                    description = "Allows F8scape to present the full-screen StandBy clock, calendar, and media controller directly over the outer cover screen.",
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
                    },
                    onNext = {
                        coroutineScope.launch { pagerState.animateScrollToPage(3) }
                    }
                )
                3 -> ReadyCompletionPage(
                    allReady = isAccessibilityActive && isOverlayGranted,
                    onFinish = onSetupComplete
                )
            }
        }

        // Bottom Navigation Bar with animated indicator pills & primary CTA
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Page indicator pills (Samsung-style smooth animated pills)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val isCurrent = pagerState.currentPage == index
                    val pillWidth by animateFloatAsState(
                        targetValue = if (isCurrent) 24f else 7f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "SetupPillWidth"
                    )
                    val pillAlpha by animateFloatAsState(
                        targetValue = if (isCurrent) 1.0f else 0.28f,
                        animationSpec = tween(300),
                        label = "SetupPillAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(pillWidth.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isCurrent) AccentBlue else Color.White.copy(alpha = pillAlpha)
                            )
                    )
                }
            }
        }
    }
}

/**
 * Page 0: Clean, calm, spacious Welcome
 */
@Composable
private fun WelcomePage(
    onGetStarted: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // App Icon Emblem with subtle elevation & border
            Surface(
                modifier = Modifier.size(96.dp),
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF101014),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                shadowElevation = 8.dp
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "F8scape Icon",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(26.dp))
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "F8scape",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "StandBy for Galaxy Z Fold 8",
                    fontSize = 15.sp,
                    color = AccentBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "Turn your phone into a calm, ambient desk display whenever it's propped in tent mode.\n\nTwo quick permissions are required to wake your cover screen automatically.",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Button(
            onClick = onGetStarted,
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Get Started",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Pages 1 & 2: Dedicated, focused permission step page with animated tick badge
 */
@Composable
private fun PermissionStepPage(
    stepNumber: String,
    title: String,
    subtitle: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    actionLabel: String,
    onActionClick: () -> Unit,
    onNext: () -> Unit
) {
    // Animated tick mark badge values
    val badgeScale by animateFloatAsState(
        targetValue = if (isGranted) 1.0f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "BadgeScale"
    )

    val badgeBgColor by animateColorAsState(
        targetValue = if (isGranted) Color(0xFF143320) else Color(0xFF181820),
        animationSpec = tween(400),
        label = "BadgeBgColor"
    )

    val badgeBorderColor by animateColorAsState(
        targetValue = if (isGranted) SuccessGreen.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.10f),
        animationSpec = tween(400),
        label = "BadgeBorderColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Step Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = (if (isGranted) SuccessGreen else AccentBlue).copy(alpha = 0.14f),
                border = BorderStroke(0.5.dp, (if (isGranted) SuccessGreen else AccentBlue).copy(alpha = 0.35f))
            ) {
                Text(
                    text = "STEP $stepNumber",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = if (isGranted) SuccessGreen else AccentBlue,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            // Big Animated State Icon Badge
            Surface(
                modifier = Modifier
                    .size(96.dp)
                    .scale(badgeScale),
                shape = CircleShape,
                color = badgeBgColor,
                border = BorderStroke(2.dp, badgeBorderColor),
                shadowElevation = if (isGranted) 10.dp else 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = isGranted,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(200))
                        },
                        label = "StepBadgeIcon"
                    ) { granted ->
                        if (granted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Granted",
                                tint = SuccessGreen,
                                modifier = Modifier.size(46.dp)
                            )
                        } else {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Description card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = CardBg,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
            ) {
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }

        // Action Buttons Row
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Main grant button
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) Color(0xFF1D3B27) else AccentBlue
                ),
                border = if (isGranted) BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)) else null,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 15.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isGranted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = actionLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGranted) SuccessGreen else Color.White
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

            // Next button to proceed to next page
            OutlinedButton(
                onClick = onNext,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isGranted) Color.White else TextTertiary
                ),
                border = BorderStroke(
                    1.dp,
                    if (isGranted) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f)
                ),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isGranted) "Next" else "Skip to Next Step",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

/**
 * Page 3: Completion / Ready Page
 */
@Composable
private fun ReadyCompletionPage(
    allReady: Boolean,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Surface(
                modifier = Modifier.size(96.dp),
                shape = CircleShape,
                color = Color(0xFF143320),
                border = BorderStroke(2.dp, SuccessGreen.copy(alpha = 0.5f)),
                shadowElevation = 12.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Ready",
                        tint = SuccessGreen,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "You're All Set",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "F8scape is ready for tent mode",
                    fontSize = 15.sp,
                    color = SuccessGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = CardBg,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "How to use StandBy:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "1. Fold your Galaxy Z Fold 8 into tent posture (~90°).\n2. Set it down on a flat surface.\n3. F8scape will slowly fade into ambient mode.\n4. Pick it up or unfold flat to dismiss instantly.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 21.sp
                    )
                }
            }
        }

        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Text(
                text = "Open F8scape Home",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
