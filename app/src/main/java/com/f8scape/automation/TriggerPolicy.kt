package com.f8scape.automation

data class TriggerPolicy(
    val isAutoStandbyEnabled: Boolean = true,
    val onlyWhileCharging: Boolean = false,
    val minBatteryPercent: Int = 10,
    val dwellTimeMs: Long = 1000L,
    val cooldownDurationMs: Long = 2000L,
    val exitDebounceMs: Long = 300L,
    val suppressDuringCalls: Boolean = true
) {
    fun canActivate(
        isCharging: Boolean,
        batteryPercent: Int,
        isCallActive: Boolean
    ): Boolean {
        if (!isAutoStandbyEnabled) return false
        if (onlyWhileCharging && !isCharging) return false
        if (batteryPercent < minBatteryPercent && !isCharging) return false
        if (suppressDuringCalls && isCallActive) return false
        return true
    }
}
