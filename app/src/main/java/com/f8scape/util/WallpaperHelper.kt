package com.f8scape.util

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility to safely retrieve the user's actual system wallpaper bitmap for ambient background rendering.
 * Never falls back to getBuiltInDrawable() so the factory default wallpaper is never shown in place
 * of the user's real wallpaper.
 */
object WallpaperHelper {
    private var cachedWallpaper: Bitmap? = null
    private var lastFetchTimeMs = 0L

    fun hasWallpaperPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestWallpaperPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val fallback = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallback)
                } catch (e2: Exception) {
                    F8Log.e("WallpaperHelper", "Failed to launch all files access settings", e2)
                }
            }
        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    suspend fun getWallpaperBitmap(context: Context): Bitmap? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedWallpaper != null && (now - lastFetchTimeMs) < 60_000L) {
            return@withContext cachedWallpaper
        }

        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val drawable = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    // Prefer the current system wallpaper (or lock wallpaper if system is null)
                    wallpaperManager.getDrawable(WallpaperManager.FLAG_SYSTEM)
                        ?: wallpaperManager.drawable
                        ?: wallpaperManager.fastDrawable
                } else {
                    wallpaperManager.drawable ?: wallpaperManager.fastDrawable
                }
            } catch (e: SecurityException) {
                F8Log.w("WallpaperHelper", "SecurityException reading user wallpaper (using FLAG_SHOW_WALLPAPER fallback): ${e.message}")
                null
            }
            val bitmap = drawable?.toBitmap()
            if (bitmap != null) {
                cachedWallpaper = bitmap
                lastFetchTimeMs = now
                F8Log.i("WallpaperHelper", "Loaded user wallpaper: ${bitmap.width}x${bitmap.height}")
            }
            bitmap
        } catch (e: Exception) {
            F8Log.w("WallpaperHelper", "Failed to retrieve system wallpaper: ${e.message}")
            cachedWallpaper
        }
    }

    fun clearCache() {
        cachedWallpaper = null
        lastFetchTimeMs = 0L
    }
}
