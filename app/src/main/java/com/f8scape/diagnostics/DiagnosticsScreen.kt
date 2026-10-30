package com.f8scape.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f8scape.app.theme.*
import com.f8scape.automation.StandbyMode
import com.f8scape.fold.FoldPosture

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel,
    onNavigateToPreview: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val report by viewModel.reportState.collectAsState()
    val stateMachineState by viewModel.stateMachine.uiState.collectAsState()
    var copySuccessMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "F8scape Diagnostics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Hardware Probe · Standard Fold 8",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureBlack,
                    titleContentColor = TextPrimary
                ),
                actions = {
                    IconButton(onClick = { viewModel.refreshReport() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentBlue)
                    }
                }
            )
        },
        containerColor = PureBlack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Action buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (viewModel.copyReportToClipboard()) {
                                copySuccessMessage = "Report copied to clipboard!"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Report", color = TextPrimary, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onNavigateToPreview,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PureBlack, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Live Preview", color = PureBlack, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }

                copySuccessMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = StatusSuccess,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
            }

            // Live State Machine Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "STANDBY STATE MACHINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary
                            )
                            val stateColor = when (stateMachineState.mode) {
                                StandbyMode.ACTIVE_DASHBOARD -> StatusSuccess
                                StandbyMode.CANDIDATE_TENT -> StatusWarning
                                StandbyMode.MONITORING -> AccentBlue
                                else -> TextSecondary
                            }
                            Text(
                                text = stateMachineState.mode.name,
                                color = stateColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Last Transition: ${stateMachineState.lastStateChangeReason}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        if (stateMachineState.mode == StandbyMode.CANDIDATE_TENT) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (stateMachineState.candidateDwellElapsedMs / 1000f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = AccentBlue,
                                trackColor = SurfaceVariantDark,
                            )
                            Text(
                                text = "Dwell elapsed: ${stateMachineState.candidateDwellElapsedMs}ms / 1000ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Target Hardware Validation
            item {
                report?.let { rep ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isFold8 = rep.hardware.isStandardFold8Candidate
                                Icon(
                                    imageVector = if (isFold8) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isFold8) StatusSuccess else StatusWarning,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isFold8) "Standard Fold 8 Hardware Identified" else "Unverified Device Target",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isFold8) StatusSuccess else StatusWarning
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            MetricRow("Manufacturer", rep.hardware.manufacturer)
                            MetricRow("Model", rep.hardware.model)
                            MetricRow("Device Name", rep.hardware.device)
                            MetricRow("Android / API", "${rep.hardware.androidRelease} (API ${rep.hardware.sdkVersion})")
                            MetricRow("Board", rep.hardware.board)
                        }
                    }
                }
            }

            // Posture & Hinge Live Detection
            item {
                report?.let { rep ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "POSTURE & SENSOR LIVE FEED",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            MetricRow("Inferred Posture", rep.currentPosture.name, isHighlight = rep.currentPosture == FoldPosture.TENT_CANDIDATE)
                            MetricRow("Stability Confidence", "${(rep.stabilityScore * 100).toInt()}%")
                            MetricRow("Live Hinge Angle", rep.liveHingeAngleDeg?.let { "%.1f°".format(it) } ?: "Not Exposed (Discrete Stops)")
                            MetricRow("Cover Screen Active", if (rep.isCoverScreenActive) "YES (Targeted)" else "NO (Inner Screen)")
                        }
                    }
                }
            }

            // Displays & Resolution Report
            item {
                report?.let { rep ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "ACTIVE DISPLAYS (${rep.displays.activeDisplayCount})",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            rep.displays.items.forEach { display ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(SurfaceVariantDark, RoundedCornerShape(10.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(display.name, fontWeight = FontWeight.Medium, color = TextPrimary)
                                        Text("ID: ${display.displayId}", color = AccentBlue, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${display.widthPx}x${display.heightPx} px · ${display.densityDpi} dpi · ${display.refreshRateHz} Hz",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "State: ${display.state} · Cover candidate: ${if (display.isCoverCandidate) "YES" else "NO"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Capabilities & Optional Permissions
            item {
                report?.let { rep ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "CAPABILITIES & SYSTEM PERMISSIONS",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            CapabilityRow("Accessibility Service", rep.isAccessibilityServiceConnected)
                            CapabilityRow("System Overlay Permission", rep.isOverlayPermissionGranted)
                            CapabilityRow("Shizuku Installed", rep.shizukuStatus.isInstalled)
                            CapabilityRow("Shizuku Authorized", rep.shizukuStatus.isAuthorized)
                        }
                    }
                }
            }

            // Local Diagnostic Logs
            item {
                report?.let { rep ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "LOCAL DIAGNOSTIC LOGS",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PureBlack, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    rep.recentLogs.takeLast(10).forEach { line ->
                                        Text(
                                            text = line,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) AccentBlue else TextPrimary
        )
    }
}

@Composable
private fun CapabilityRow(label: String, isEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(
            text = if (isEnabled) "CONNECTED / GRANTED" else "NOT ENABLED",
            color = if (isEnabled) StatusSuccess else TextTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
