package com.f8scape.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.f8scape.app.theme.F8scapeTheme
import com.f8scape.dashboard.DashboardActivity
import com.f8scape.dashboard.StandbyDashboard
import com.f8scape.util.F8Log

/**
 * User-enabled Accessibility Service.
 * Provides lawful background posture detection and full-screen cover overlay host.
 */
class CoverOverlayService : AccessibilityService(), LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        var isServiceRunning = false
            private set
        var activeInstance: CoverOverlayService? = null
            private set
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        F8Log.i("CoverOverlayService", "Accessibility service created")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        activeInstance = this
        F8Log.i("CoverOverlayService", "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Monitored for window transitions
    }

    override fun onInterrupt() {
        F8Log.w("CoverOverlayService", "Accessibility service interrupted")
    }

    fun showStandbyOverlay() {
        if (overlayView != null) return

        try {
            val composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@CoverOverlayService)
                setViewTreeSavedStateRegistryOwner(this@CoverOverlayService)
                setContent {
                    F8scapeTheme {
                        StandbyDashboard(
                            onDismissRequest = {
                                removeStandbyOverlay()
                            }
                        )
                    }
                }
            }

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

            windowManager?.addView(composeView, params)
            overlayView = composeView
            F8Log.i("CoverOverlayService", "Presented Standby overlay via Accessibility window")
        } catch (e: Exception) {
            F8Log.e("CoverOverlayService", "Failed to add overlay window, falling back to DashboardActivity", e)
            val intent = DashboardActivity.createIntent(this)
            startActivity(intent)
        }
    }

    fun removeStandbyOverlay() {
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
                F8Log.i("CoverOverlayService", "Removed Standby overlay")
            } catch (e: Exception) {
                F8Log.e("CoverOverlayService", "Failed to remove overlay window", e)
            }
        }
        overlayView = null
    }

    override fun onDestroy() {
        removeStandbyOverlay()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        isServiceRunning = false
        activeInstance = null
        F8Log.i("CoverOverlayService", "Accessibility service destroyed")
        super.onDestroy()
    }
}
