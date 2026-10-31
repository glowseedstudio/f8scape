package com.f8scape.notifications

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.service.notification.StatusBarNotification
import com.f8scape.util.F8Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StatusBarNotificationIcon(
    val key: String,
    val packageName: String,
    val iconBitmap: Bitmap?,
    val postTime: Long = 0L,
    val count: Int = 1
)

object ActiveNotificationRepository {
    private val _activeNotifications = MutableStateFlow<List<StatusBarNotificationIcon>>(emptyList())
    val activeNotifications: StateFlow<List<StatusBarNotificationIcon>> = _activeNotifications.asStateFlow()

    private var refreshCallback: (() -> Unit)? = null

    fun setRefreshCallback(callback: (() -> Unit)?) {
        refreshCallback = callback
    }

    fun requestRefresh() {
        refreshCallback?.invoke()
    }

    fun updateNotifications(icons: List<StatusBarNotificationIcon>) {
        _activeNotifications.value = icons
    }

    /**
     * Extracts the status bar silhouette icon for a notification.
     * Uses package context so icon resource IDs resolve correctly across applications,
     * and rasterizes the vector/drawable into a crisp 48x48 ARGB bitmap.
     */
    fun extractSmallIconBitmap(context: Context, sbn: StatusBarNotification): Bitmap? {
        val drawable: Drawable = (try {
            val packageContext = context.createPackageContext(sbn.packageName, 0)
            sbn.notification?.smallIcon?.loadDrawable(packageContext)
        } catch (e: Exception) {
            try {
                sbn.notification?.smallIcon?.loadDrawable(context)
            } catch (e2: Exception) {
                try {
                    context.packageManager.getApplicationIcon(sbn.packageName)
                } catch (e3: Exception) {
                    null
                }
            }
        }) ?: return null

        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                return drawable.bitmap
            }
            val sizePx = 48
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            F8Log.w("NotificationRepository", "Could not render notification icon for ${sbn.packageName}: ${e.message}")
            null
        }
    }
}
