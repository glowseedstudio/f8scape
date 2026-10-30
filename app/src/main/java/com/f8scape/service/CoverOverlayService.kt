package com.f8scape.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.f8scape.util.F8Log

/**
 * Optional user-enabled Accessibility Service.
 * Used to investigate overlay capability and background posture detection on the Fold 8 cover screen.
 */
class CoverOverlayService : AccessibilityService() {

    companion object {
        var isServiceRunning = false
            private set
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        F8Log.i("CoverOverlayService", "Accessibility service created")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        F8Log.i("CoverOverlayService", "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Monitored for window transition diagnostics
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: "unknown"
            F8Log.d("CoverOverlayService", "Window state changed: $pkg")
        }
    }

    override fun onInterrupt() {
        F8Log.w("CoverOverlayService", "Accessibility service interrupted")
    }

    override fun onDestroy() {
        removeOverlay()
        isServiceRunning = false
        F8Log.i("CoverOverlayService", "Accessibility service destroyed")
        super.onDestroy()
    }

    fun showOverlay(view: View) {
        removeOverlay()
        overlayView = view

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            windowManager?.addView(overlayView, params)
            F8Log.i("CoverOverlayService", "Overlay window added")
        } catch (e: Exception) {
            F8Log.e("CoverOverlayService", "Failed to add overlay window", e)
        }
    }

    fun removeOverlay() {
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
                F8Log.i("CoverOverlayService", "Overlay window removed")
            } catch (e: Exception) {
                F8Log.e("CoverOverlayService", "Failed to remove overlay window", e)
            }
        }
        overlayView = null
    }
}
