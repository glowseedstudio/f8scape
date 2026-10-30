package com.f8scape.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.*
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
    private val mainHandler = Handler(Looper.getMainLooper())
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        F8Log.w("CoverOverlayService", "Accessibility service interrupted")
        removeStandbyOverlay()
    }

    fun showStandbyOverlay() {
        mainHandler.post {
            if (overlayView != null) return@post

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

                // Fullscreen immersive: completely hide status and navigation bars
                composeView.fitsSystemWindows = false
                composeView.isFocusable = true
                composeView.isFocusableInTouchMode = true
                composeView.setOnKeyListener { _, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                        removeStandbyOverlay()
                        true
                    } else {
                        false
                    }
                }

                composeView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        @Suppress("DEPRECATION")
                        v.systemUiVisibility = (
                            View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            v.windowInsetsController?.let { controller ->
                                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            }
                        }
                    }

                    override fun onViewDetachedFromWindow(v: View) {}
                })

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
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
                    PixelFormat.OPAQUE
                ).apply {
                    gravity = Gravity.FILL
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    }
                }

                composeView.alpha = 0f
                windowManager?.addView(composeView, params)
                overlayView = composeView

                composeView.animate()
                    .alpha(1f)
                    .setDuration(950)
                    .setInterpolator(android.view.animation.DecelerateInterpolator(1.8f))
                    .start()

                F8Log.i("CoverOverlayService", "Presented full-screen Standby overlay with smooth fade-in")
            } catch (e: Exception) {
                F8Log.e("CoverOverlayService", "Failed to add overlay window, falling back to DashboardActivity", e)
                val intent = DashboardActivity.createIntent(this)
                startActivity(intent)
            }
        }
    }

    fun removeStandbyOverlay() {
        mainHandler.post {
            val view = overlayView ?: return@post
            overlayView = null
            try {
                view.animate()
                    .alpha(0f)
                    .setDuration(350)
                    .setInterpolator(android.view.animation.AccelerateInterpolator(1.5f))
                    .withEndAction {
                        try {
                            windowManager?.removeView(view)
                            F8Log.i("CoverOverlayService", "Removed Standby overlay")
                        } catch (e: Exception) {
                            F8Log.e("CoverOverlayService", "Failed to remove overlay window", e)
                        }
                    }
                    .start()
            } catch (e: Exception) {
                try {
                    windowManager?.removeView(view)
                } catch (_: Exception) {}
            }
        }
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
