package com.f8scape.app

import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f8scape.R
import com.f8scape.app.theme.AccentBlue
import com.f8scape.app.theme.TextSecondary
import com.f8scape.app.theme.TextTertiary
import com.f8scape.updater.AppUpdateDialog

private val AboutBg = Color(0xFF0A0A0C)
private val AboutCardBg = Color(0xFF161618)

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    var showUpdateDialog by remember { mutableStateOf(false) }

    val versionName = remember {
        try {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pkgInfo.versionName ?: "1.0.0"
        } catch (_: PackageManager.NameNotFoundException) {
            "1.0.0"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AboutBg)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 100.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AboutCardBg,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // App Icon Emblem
                Surface(
                    modifier = Modifier.size(88.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0E0E11),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = "F8scape App Icon",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "F8scape",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AccentBlue.copy(alpha = 0.14f),
                    border = BorderStroke(0.5.dp, AccentBlue.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "Version $versionName",
                        color = AccentBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "A quiet bedside clock and cover screen dashboard made for the Galaxy Z Fold 8. Prop your phone in tent mode and it turns on; pick it up and it gets out of your way.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Check for Updates Button
                Button(
                    onClick = { showUpdateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222228)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdateAlt,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Check for Updates",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "No ads, no analytics, and no cloud accounts.",
                    fontSize = 12.sp,
                    color = TextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showUpdateDialog) {
        AppUpdateDialog(
            onDismiss = { showUpdateDialog = false }
        )
    }
}
