package com.f8scape.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.f8scape.util.F8Log
import java.io.File

object AppUpdateInstaller {
    private const val TAG = "AppUpdateInstaller"

    /**
     * Checks if the app currently has permission to prompt for package installation.
     * On Android 8.0+ (API 26+), requires [Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES].
     */
    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Launches the system settings screen allowing the user to grant permission
     * for F8scape to install updates directly.
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                F8Log.e(TAG, "Failed to open install permission settings: ${e.message}", e)
                val fallbackIntent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            }
        }
    }

    /**
     * Launches Android's native package installer to prompt the user to update the app.
     */
    fun installApk(context: Context, apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                return Result.failure(IllegalStateException("APK file does not exist or is empty"))
            }

            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            F8Log.i(TAG, "Launched package installer for ${apkFile.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            F8Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
            Result.failure(e)
        }
    }
}
