package com.f8scape.dashboard

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.f8scape.app.theme.F8scapeTheme
import com.f8scape.data.PreferencesRepository
import com.f8scape.fold.FoldPosture
import com.f8scape.fold.SensorPostureSource
import com.f8scape.util.F8Log
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DashboardActivity : ComponentActivity() {

    private lateinit var sensorPostureSource: SensorPostureSource
    private lateinit var preferencesRepository: PreferencesRepository

    companion object {
        var isActivityActive: Boolean = false
            private set
        private var activeInstance: DashboardActivity? = null

        fun dismissIfActive() {
            activeInstance?.let { activity ->
                activity.runOnUiThread {
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        F8Log.i("DashboardActivity", "Dismissing DashboardActivity via dismissIfActive")
                        activity.finishAndRemoveTask()
                    }
                }
            }
        }

        fun createIntent(context: Context): Intent {
            return Intent(context, DashboardActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeInstance = this
        isActivityActive = true
        preferencesRepository = PreferencesRepository(this)
        sensorPostureSource = SensorPostureSource(this)

        // Show when locked & turn screen on (Android O_MR1+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // Full immersive edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        // Observe preferences for brightness, keepScreenOn & wallpaper flag
        lifecycleScope.launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                if (prefs.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                window.setBackgroundDrawableResource(android.R.color.black)
                // Window-specific brightness
                window.attributes = window.attributes.apply {
                    screenBrightness = prefs.screenDimLevel.coerceIn(0.01f, 1.0f)
                }
            }
        }

        // Auto-dismiss on pickup, unfold flat, or closed fold
        startPostureExitObservation()

        setContent {
            F8scapeTheme {
                StandbyDashboard(
                    onDismissRequest = {
                        F8Log.i("DashboardActivity", "User requested dismissal")
                        finishAndRemoveTask()
                    }
                )
            }
        }
    }

    private fun startPostureExitObservation() {
        lifecycleScope.launch {
            val launchTimeMs = System.currentTimeMillis()
            var exitDebounceStartTimeMs = 0L
            val debounceDurationMs = 300L

            sensorPostureSource.observePosture().collect { postureData ->
                val now = System.currentTimeMillis()
                // Grace window for sensor stabilization on launch
                if (now - launchTimeMs < 1500L) return@collect

                // Standby is retained if in horizontal orientation (not portrait), not closed, and not flat.
                // Screen touches and cushion motion do not dismiss unless turned to portrait or folded.
                val isNotFlatOrClosed = postureData.posture != FoldPosture.CLOSED && postureData.posture != FoldPosture.FLAT
                val isNotPortrait = !postureData.isPortrait
                val isRetainingTent = isNotFlatOrClosed && isNotPortrait

                if (isRetainingTent) {
                    exitDebounceStartTimeMs = 0L
                } else {
                    if (exitDebounceStartTimeMs == 0L) {
                        exitDebounceStartTimeMs = now
                        F8Log.d("DashboardActivity", "Exit trigger candidate (isPortrait=${postureData.isPortrait}, posture=${postureData.posture}), started debounce timer (${debounceDurationMs}ms)")
                    } else if (now - exitDebounceStartTimeMs >= debounceDurationMs) {
                        F8Log.i("DashboardActivity", "Exit trigger confirmed (isPortrait=${postureData.isPortrait}, posture=${postureData.posture})")
                        finishAndRemoveTask()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (activeInstance == this) {
            activeInstance = null
        }
        isActivityActive = false
        F8Log.i("DashboardActivity", "Dashboard activity destroyed")
        super.onDestroy()
    }
}
