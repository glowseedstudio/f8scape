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

        fun createIntent(context: Context): Intent {
            return Intent(context, DashboardActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        // Read preferences for brightness & keepScreenOn
        lifecycleScope.launch {
            val prefs = preferencesRepository.userPreferencesFlow.first()
            if (prefs.keepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            // Window-specific brightness
            window.attributes = window.attributes.apply {
                screenBrightness = prefs.screenDimLevel.coerceIn(0.01f, 1.0f)
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
            sensorPostureSource.observePosture().collect { postureData ->
                // Grace window for sensor stabilization on launch
                if (System.currentTimeMillis() - launchTimeMs < 1500L) return@collect

                // Exit condition: picked up (stability < 0.55) or posture folded flat, closed, portrait, etc.
                val isNotTent = postureData.posture != FoldPosture.TENT_CANDIDATE
                val isMoving = postureData.stabilityConfidence < 0.55f

                if (isNotTent || isMoving) {
                    F8Log.i("DashboardActivity", "Exit trigger detected (posture=${postureData.posture}, stability=${postureData.stabilityConfidence})")
                    finishAndRemoveTask()
                }
            }
        }
    }

    override fun onDestroy() {
        isActivityActive = false
        F8Log.i("DashboardActivity", "Dashboard activity destroyed")
        super.onDestroy()
    }
}
