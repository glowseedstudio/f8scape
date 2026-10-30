package com.f8scape.updater

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.f8scape.app.theme.AccentBlue
import com.f8scape.app.theme.PureBlack
import com.f8scape.app.theme.TextSecondary
import com.f8scape.app.theme.TextTertiary
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun AppUpdateDialog(
    initialResult: UpdateCheckResult? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val checker = remember { AppUpdateChecker() }
    val downloader = remember { AppUpdateDownloader(context) }
    val currentVersion = remember(context) { AppUpdateChecker.getInstalledAppVersion(context) }

    var checkState by remember {
        mutableStateOf<UpdateCheckResult>(initialResult ?: UpdateCheckResult.Checking)
    }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<DownloadProgress?>(null) }
    var downloadedApkFile by remember { mutableStateOf<File?>(null) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    var hasInstallPermission by remember {
        mutableStateOf(AppUpdateInstaller.canInstallPackages(context))
    }

    fun startCheck() {
        checkState = UpdateCheckResult.Checking
        coroutineScope.launch {
            checkState = checker.checkForUpdate(currentVersion)
        }
    }

    LaunchedEffect(initialResult) {
        if (initialResult == null) {
            startCheck()
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = !isDownloading,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF161618),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row with Title and Close icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (checkState) {
                            is UpdateCheckResult.Checking -> "Checking for Updates"
                            is UpdateCheckResult.Available -> "Update Available"
                            is UpdateCheckResult.UpToDate -> "Up to Date"
                            is UpdateCheckResult.Error -> "Update Check Failed"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (!isDownloading) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                when (val state = checkState) {
                    is UpdateCheckResult.Checking -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = AccentBlue,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Querying GitHub for latest F8scape release...",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    is UpdateCheckResult.UpToDate -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF1B3828),
                                border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f)),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF81C784),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Text(
                                text = "F8scape is up to date",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "You have the latest version (${state.currentVersion}) installed.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24242A)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Done", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    is UpdateCheckResult.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF381B1B),
                                border = BorderStroke(1.dp, Color(0xFFE57373).copy(alpha = 0.4f)),
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFE57373),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Text(
                                text = state.message,
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Close")
                                }
                                Button(
                                    onClick = { startCheck() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Retry", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    is UpdateCheckResult.Available -> {
                        val release = state.releaseInfo
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Version comparison pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = AccentBlue.copy(alpha = 0.15f),
                                    border = BorderStroke(0.5.dp, AccentBlue.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "v$currentVersion  ➔  v${release.versionName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentBlue,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    text = formatFileSize(release.apkSizeBytes),
                                    fontSize = 12.sp,
                                    color = TextTertiary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Release notes box
                            if (release.releaseNotes.isNotBlank()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 180.dp),
                                    color = Color(0xFF101012),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        Text(
                                            text = release.releaseTitle.ifBlank { "What's New in v${release.versionName}" },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = release.releaseNotes.trim(),
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }

                            // Progress Indicator during download
                            if (isDownloading && downloadProgress != null) {
                                val progress = downloadProgress!!
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { progress.percent },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = AccentBlue,
                                        trackColor = Color.White.copy(alpha = 0.10f)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${(progress.percent * 100).toInt()}%",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "${formatFileSize(progress.bytesDownloaded)} / ${formatFileSize(progress.totalBytes)}",
                                            fontSize = 11.sp,
                                            color = TextTertiary
                                        )
                                    }
                                }
                            }

                            // Permission warning if downloaded but cannot install
                            AnimatedVisibility(visible = downloadedApkFile != null && !hasInstallPermission) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF282012),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.30f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Android requires permission to install app updates from F8scape.",
                                            fontSize = 12.sp,
                                            color = Color(0xFFFFE082),
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Action Buttons
                            when {
                                downloadedApkFile != null -> {
                                    if (!hasInstallPermission) {
                                        Button(
                                            onClick = {
                                                AppUpdateInstaller.openInstallPermissionSettings(context)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                "Grant Permission & Install",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                downloadedApkFile?.let { file ->
                                                    AppUpdateInstaller.installApk(context, file)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Install Now", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                isDownloading -> {
                                    OutlinedButton(
                                        onClick = {
                                            downloadJob?.cancel()
                                            isDownloading = false
                                            downloadProgress = null
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE57373).copy(alpha = 0.5f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Cancel Download")
                                    }
                                }

                                else -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = onDismiss,
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Later")
                                        }

                                        Button(
                                            onClick = {
                                                isDownloading = true
                                                downloadJob = coroutineScope.launch {
                                                    val result = downloader.downloadApk(release) { progress ->
                                                        downloadProgress = progress
                                                    }
                                                    isDownloading = false
                                                    result.onSuccess { file ->
                                                        downloadedApkFile = file
                                                        hasInstallPermission = AppUpdateInstaller.canInstallPackages(context)
                                                        if (hasInstallPermission) {
                                                            AppUpdateInstaller.installApk(context, file)
                                                        }
                                                    }.onFailure { err ->
                                                        checkState = UpdateCheckResult.Error(
                                                            err.message ?: "Failed to download update"
                                                        )
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.weight(1.3f)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Download & Install", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        val kb = bytes / 1024.0
        String.format(Locale.US, "%.0f KB", kb)
    }
}
