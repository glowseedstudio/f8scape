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
        try {
            com.f8scape.media.F8MediaNotificationListener.activeInstance?.refreshActiveNotifications()
        } catch (_: Exception) {}
    }

    fun updateNotifications(icons: List<StatusBarNotificationIcon>) {
        _activeNotifications.value = icons
    }

    /**
     * Extracts the status bar silhouette icon for a notification.
     * Uses multiple resilient loaders (direct icon, package context, legacy resource ID, or app icon fallback),
     * and rasterizes the vector/drawable into a crisp 48x48 ARGB bitmap.
     */
    fun extractSmallIconBitmap(context: Context, sbn: StatusBarNotification): Bitmap? {
        val notif = sbn.notification ?: return null
        val smallIcon = notif.smallIcon

        val drawable: Drawable = (try {
            // 1. Direct Icon.loadDrawable with app context (supported via QUERY_ALL_PACKAGES)
            smallIcon?.loadDrawable(context)
        } catch (e: Exception) {
            null
        }) ?: (try {
            // 2. Package context with CONTEXT_IGNORE_SECURITY
            val packageContext = context.createPackageContext(sbn.packageName, Context.CONTEXT_IGNORE_SECURITY)
            smallIcon?.loadDrawable(packageContext)
        } catch (e: Exception) {
            null
        }) ?: (try {
            // 3. Legacy notification.icon integer resource ID
            if (notif.icon != 0) {
                val packageContext = context.createPackageContext(sbn.packageName, Context.CONTEXT_IGNORE_SECURITY)
                packageContext.resources.getDrawable(notif.icon, packageContext.theme)
            } else null
        } catch (e: Exception) {
            null
        }) ?: (try {
            // 4. Fallback to application launcher icon
            context.packageManager.getApplicationIcon(sbn.packageName)
        } catch (e: Exception) {
            null
        }) ?: return null

        return try {
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
