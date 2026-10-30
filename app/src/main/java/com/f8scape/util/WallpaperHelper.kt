package com.f8scape.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility to safely retrieve the system wallpaper bitmap for ambient background rendering.
 */
object WallpaperHelper {
    private var cachedWallpaper: Bitmap? = null
    private var lastFetchTimeMs = 0L

    suspend fun getWallpaperBitmap(context: Context): Bitmap? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedWallpaper != null && (now - lastFetchTimeMs) < 60_000L) {
            return@withContext cachedWallpaper
        }

        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val drawable = try {
                wallpaperManager.drawable ?: wallpaperManager.fastDrawable ?: wallpaperManager.getBuiltInDrawable()
            } catch (e: SecurityException) {
                F8Log.w("WallpaperHelper", "SecurityException reading wallpaper: ${e.message}")
                wallpaperManager.getBuiltInDrawable()
            }
            val bitmap = drawable?.toBitmap()
            if (bitmap != null) {
                cachedWallpaper = bitmap
                lastFetchTimeMs = now
                F8Log.i("WallpaperHelper", "Loaded wallpaper: ${bitmap.width}x${bitmap.height}")
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
