package com.f8scape.automation

/**
 * Standby lifecycle modes as defined in spec section 4.2.
 */
enum class StandbyMode {
    DISABLED,
    MONITORING,
    CANDIDATE_TENT,
    ACTIVATING,
    ACTIVE_DASHBOARD,
    SUSPENDED,
    EXITING,
    COOLDOWN
}

/**
 * Immutable StandBy UI state driving Compose rendering.
 */
data class StandbyUiState(
    val mode: StandbyMode = StandbyMode.MONITORING,
    val isAutoStandbyEnabled: Boolean = true,
    val isCoverDisplayActive: Boolean = true,
    val stabilityConfidence: Float = 0f,
    val candidateDwellElapsedMs: Long = 0L,
    val activePreset: String = "Dashboard", // Minimal, Dashboard, Media
    val isMediaPlaying: Boolean = false,
    val isBatterySaverActive: Boolean = false,
    val isCharging: Boolean = false,
    val isDeviceLocked: Boolean = false,
    val lastStateChangeReason: String = "Initial state",
    val timestampMs: Long = System.currentTimeMillis()
)
