package com.f8scape.power

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.f8scape.util.F8Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PowerProfile(val id: String, val displayName: String, val description: String) {
    AMBIENT(
        id = "AMBIENT",
        displayName = "Ambient (Standard)",
        description = "Smooth animations, Ken Burns artwork motion, and full visual fidelity"
    ),
    ECO(
        id = "ECO",
        displayName = "Eco (Low Power)",
        description = "Static backgrounds, reduced GPU motion, and conservative refresh for maximum battery life"
    );

    companion object {
        fun fromId(id: String): PowerProfile = entries.find { it.id == id } ?: AMBIENT
    }
}

data class SystemResilienceState(
    val isBatterySaverActive: Boolean = false,
    val isThermalElevated: Boolean = false,
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val isCallOrRingtoneActive: Boolean = false
) {
    /**
     * Returns true if Eco power optimizations should be active due to user preference,
     * Android Battery Saver, or elevated device thermal status.
     */
    fun shouldEnforceEco(userProfile: PowerProfile, autoEcoOnBatterySaver: Boolean = true): Boolean {
        if (userProfile == PowerProfile.ECO) return true
        if (isThermalElevated) return true
        if (autoEcoOnBatterySaver && isBatterySaverActive) return true
        return false
    }
}

/**
 * Monitors battery saver, thermal status, charging state, and active call/ringtone audio modes
 * without requiring invasive runtime permissions.
 */
class PowerProfileController(private val context: Context) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _state = MutableStateFlow(readInitialState())
    val state: StateFlow<SystemResilienceState> = _state.asStateFlow()

    private var isRegistered = false

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshState()
        }
    }

    private val thermalListener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        PowerManager.OnThermalStatusChangedListener { status ->
            val elevated = status >= PowerManager.THERMAL_STATUS_MODERATE
            F8Log.i("PowerProfileController", "Thermal status changed: $status (elevated=$elevated)")
            refreshState()
        }
    } else null

    fun startMonitoring() {
        if (isRegistered) return
        try {
            val filter = IntentFilter().apply {
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            androidx.core.content.ContextCompat.registerReceiver(
                context,
                systemReceiver,
                filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && thermalListener != null) {
                powerManager?.addThermalStatusListener(context.mainExecutor, thermalListener)
            }
            isRegistered = true
            refreshState()
        } catch (e: Exception) {
            F8Log.e("PowerProfileController", "Failed to start system resilience monitoring", e)
        }
    }

    fun stopMonitoring() {
        if (!isRegistered) return
        try {
            context.unregisterReceiver(systemReceiver)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && thermalListener != null) {
                powerManager?.removeThermalStatusListener(thermalListener)
            }
        } catch (_: Exception) {
        } finally {
            isRegistered = false
        }
    }

    fun refreshState(): SystemResilienceState {
        val current = readInitialState()
        _state.value = current
        return current
    }

    private fun readInitialState(): SystemResilienceState {
        val batteryIntent = try {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: Exception) {
            null
        }
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 100
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val batterySaver = powerManager?.isPowerSaveMode ?: false
        val thermalElevated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (powerManager?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE) >=
                PowerManager.THERMAL_STATUS_MODERATE
        } else {
            false
        }

        val audioMode = audioManager?.mode ?: AudioManager.MODE_NORMAL
        val inCall = audioMode == AudioManager.MODE_RINGTONE ||
            audioMode == AudioManager.MODE_IN_CALL ||
            audioMode == AudioManager.MODE_IN_COMMUNICATION

        return SystemResilienceState(
            isBatterySaverActive = batterySaver,
            isThermalElevated = thermalElevated,
            batteryPercent = pct,
            isCharging = charging,
            isCallOrRingtoneActive = inCall
        )
    }
}
