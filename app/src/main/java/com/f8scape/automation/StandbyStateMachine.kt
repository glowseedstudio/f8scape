package com.f8scape.automation

import com.f8scape.fold.FoldPosture
import com.f8scape.fold.PostureData
import com.f8scape.util.F8Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Deterministic Standby State Machine driving F8scape cover screen activation.
 */
class StandbyStateMachine(
    private var policy: TriggerPolicy = TriggerPolicy()
) {
    sealed class Action {
        object LaunchCoverDashboard : Action()
        object DismissCoverDashboard : Action()
        object ScheduleDwellTimer : Action()
        object CancelDwellTimer : Action()
        object ScheduleCooldownTimer : Action()
    }

    private val _uiState = MutableStateFlow(StandbyUiState())
    val uiState: StateFlow<StandbyUiState> = _uiState.asStateFlow()

    private var candidateStartTimeMs: Long = 0L
    private var lastExitTimeMs: Long = 0L
    private var exitDebounceStartTimeMs: Long = 0L

    fun updatePolicy(newPolicy: TriggerPolicy) {
        policy = newPolicy
        if (!policy.isAutoStandbyEnabled && _uiState.value.mode != StandbyMode.DISABLED) {
            transitionTo(StandbyMode.DISABLED, "Disabled by user policy", Action.DismissCoverDashboard)
        } else if (policy.isAutoStandbyEnabled && _uiState.value.mode == StandbyMode.DISABLED) {
            transitionTo(StandbyMode.MONITORING, "Enabled by user policy", null)
        }
    }

    fun onPostureUpdate(
        postureData: PostureData,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Action? {
        val currentMode = _uiState.value.mode

        if (!policy.isAutoStandbyEnabled) {
            if (currentMode != StandbyMode.DISABLED) {
                transitionTo(StandbyMode.DISABLED, "Disabled in policy", Action.DismissCoverDashboard)
                return Action.DismissCoverDashboard
            }
            return null
        }

        // Check cooldown
        if (currentMode == StandbyMode.COOLDOWN) {
            if (currentTimeMs - lastExitTimeMs >= policy.cooldownDurationMs) {
                transitionTo(StandbyMode.MONITORING, "Cooldown expired", null)
            } else {
                return null
            }
        }

        val isTent = postureData.posture == FoldPosture.TENT_CANDIDATE
        val isStable = postureData.stabilityConfidence >= 0.70f
        val isCoverActive = postureData.isCoverScreenActive

        when (currentMode) {
            StandbyMode.MONITORING -> {
                if (isTent && isStable && isCoverActive) {
                    candidateStartTimeMs = currentTimeMs
                    transitionTo(
                        StandbyMode.CANDIDATE_TENT,
                        "Stable tent detected, starting dwell",
                        Action.ScheduleDwellTimer
                    )
                    return Action.ScheduleDwellTimer
                }
            }

            StandbyMode.CANDIDATE_TENT -> {
                if (!isTent || !isStable || !isCoverActive) {
                    candidateStartTimeMs = 0L
                    transitionTo(StandbyMode.MONITORING, "Stability lost during dwell", Action.CancelDwellTimer)
                    return Action.CancelDwellTimer
                } else {
                    val dwellElapsed = currentTimeMs - candidateStartTimeMs
                    _uiState.value = _uiState.value.copy(
                        candidateDwellElapsedMs = dwellElapsed,
                        stabilityConfidence = postureData.stabilityConfidence
                    )
                    if (dwellElapsed >= policy.dwellTimeMs) {
                        transitionTo(StandbyMode.ACTIVATING, "Dwell threshold reached", Action.LaunchCoverDashboard)
                        return Action.LaunchCoverDashboard
                    }
                }
            }

            StandbyMode.ACTIVATING -> {
                transitionTo(StandbyMode.ACTIVE_DASHBOARD, "Dashboard ready", null)
            }

            StandbyMode.ACTIVE_DASHBOARD -> {
                // Posture retention with hysteresis: phone remains in horizontal/landscape tent orientation
                // within hinge angle envelope (30°–155°), and not flat or closed.
                // Screen touches, widget taps, and cushion motion do not dismiss unless turned to portrait or folded.
                val isAngleInEnvelope = postureData.hingeAngleDeg?.let { it in 30f..155f }
                    ?: (postureData.posture == FoldPosture.TENT_CANDIDATE || postureData.posture == FoldPosture.HALF_OPENED)
                val isNotFlatOrClosed = postureData.posture != FoldPosture.CLOSED && postureData.posture != FoldPosture.FLAT
                val isNotPortrait = !postureData.isPortrait

                val isRetainingTent = isCoverActive && isAngleInEnvelope && isNotFlatOrClosed && isNotPortrait

                if (isRetainingTent) {
                    // Posture is within hysteresis envelope; cancel any pending exit debounce
                    if (exitDebounceStartTimeMs != 0L) {
                        F8Log.d("StateMachine", "Tent posture recovered, cancelled exit debounce")
                        exitDebounceStartTimeMs = 0L
                    }
                } else {
                    // Out of active tent envelope (e.g. turned back up to portrait mode, folded flat, or closed)
                    if (exitDebounceStartTimeMs == 0L) {
                        exitDebounceStartTimeMs = currentTimeMs
                        F8Log.d("StateMachine", "Exit trigger candidate (isPortrait=${postureData.isPortrait}, posture=${postureData.posture}), started exit debounce timer (${policy.exitDebounceMs}ms)")
                    }

                    val debounceElapsed = currentTimeMs - exitDebounceStartTimeMs
                    if (debounceElapsed >= policy.exitDebounceMs) {
                        lastExitTimeMs = currentTimeMs
                        exitDebounceStartTimeMs = 0L
                        val exitReason = when {
                            !isCoverActive -> "Cover screen inactive"
                            postureData.isPortrait -> "Device turned up to portrait mode"
                            postureData.posture == FoldPosture.CLOSED -> "Device closed"
                            postureData.posture == FoldPosture.FLAT -> "Device unfolded flat"
                            postureData.hingeAngleDeg != null && postureData.hingeAngleDeg !in 30f..155f ->
                                "Hinge angle out of hysteresis envelope (${postureData.hingeAngleDeg}°)"
                            else -> "Fold posture changed (${postureData.posture.name})"
                        }
                        transitionTo(StandbyMode.EXITING, exitReason, Action.DismissCoverDashboard)
                        return Action.DismissCoverDashboard
                    }
                }
            }

            StandbyMode.EXITING -> {
                transitionTo(StandbyMode.COOLDOWN, "Entering cooldown", Action.ScheduleCooldownTimer)
                return Action.ScheduleCooldownTimer
            }

            StandbyMode.SUSPENDED -> {
                if (isTent && isStable && isCoverActive) {
                    transitionTo(StandbyMode.MONITORING, "Resumed from suspended", null)
                }
            }

            StandbyMode.DISABLED, StandbyMode.COOLDOWN -> {
                // Handled above
            }
        }

        return null
    }

    fun onDashboardLaunched() {
        if (_uiState.value.mode == StandbyMode.ACTIVATING) {
            transitionTo(StandbyMode.ACTIVE_DASHBOARD, "Dashboard activity displayed", null)
        }
    }

    fun onDashboardDismissed(currentTimeMs: Long = System.currentTimeMillis()) {
        lastExitTimeMs = currentTimeMs
        transitionTo(StandbyMode.COOLDOWN, "Dashboard dismissed", Action.ScheduleCooldownTimer)
    }

    fun onPowerSuspension(reason: String) {
        if (_uiState.value.mode == StandbyMode.ACTIVE_DASHBOARD || _uiState.value.mode == StandbyMode.ACTIVATING) {
            transitionTo(StandbyMode.SUSPENDED, reason, Action.DismissCoverDashboard)
        } else {
            transitionTo(StandbyMode.SUSPENDED, reason, null)
        }
    }

    private fun transitionTo(newMode: StandbyMode, reason: String, sideEffect: Action?) {
        val oldMode = _uiState.value.mode
        if (oldMode == newMode) return

        if (newMode != StandbyMode.ACTIVE_DASHBOARD) {
            exitDebounceStartTimeMs = 0L
        }

        F8Log.i("StateMachine", "Transition: $oldMode -> $newMode ($reason)")
        _uiState.value = _uiState.value.copy(
            mode = newMode,
            lastStateChangeReason = reason,
            timestampMs = System.currentTimeMillis()
        )
    }
}
