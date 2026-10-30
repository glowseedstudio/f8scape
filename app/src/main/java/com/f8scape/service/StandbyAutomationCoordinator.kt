package com.f8scape.service

import android.content.Context
import com.f8scape.automation.StandbyStateMachine
import com.f8scape.dashboard.DashboardActivity
import com.f8scape.data.PreferencesRepository
import com.f8scape.display.ScreenStateRepository
import com.f8scape.fold.FoldPosture
import com.f8scape.fold.SensorPostureSource
import com.f8scape.util.F8Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

class StandbyAutomationCoordinator(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val sensorSource = SensorPostureSource(context)
    private val screenRepo = ScreenStateRepository(context)
    private val preferencesRepo = PreferencesRepository(context)
    val stateMachine = StandbyStateMachine()

    private val _livePosture = kotlinx.coroutines.flow.MutableStateFlow<com.f8scape.fold.PostureData?>(null)
    val livePosture: kotlinx.coroutines.flow.StateFlow<com.f8scape.fold.PostureData?> = _livePosture.asStateFlow()

    private var observationJob: Job? = null
    private var dwellTimerJob: Job? = null

    fun start() {
        if (observationJob != null) return
        F8Log.i("AutomationCoordinator", "Starting automation coordinator")

        observationJob = scope.launch {
            // Collect user policy updates
            launch {
                preferencesRepo.triggerPolicyFlow.collectLatest { policy ->
                    stateMachine.updatePolicy(policy)
                }
            }

            // Collect sensor posture stream
            launch {
                sensorSource.observePosture().collect { postureData ->
                    _livePosture.value = postureData
                    val action = stateMachine.onPostureUpdate(postureData)
                    handleAction(action)
                }
            }
        }
    }

    fun stop() {
        observationJob?.cancel()
        observationJob = null
        dwellTimerJob?.cancel()
        dwellTimerJob = null
        F8Log.i("AutomationCoordinator", "Stopped automation coordinator")
    }

    private fun handleAction(action: StandbyStateMachine.Action?) {
        when (action) {
            is StandbyStateMachine.Action.ScheduleDwellTimer -> {
                dwellTimerJob?.cancel()
                dwellTimerJob = scope.launch {
                    val dwellTime = preferencesRepo.userPreferencesFlow.first().dwellTimeMs
                    delay(dwellTime)
                    if (stateMachine.uiState.value.mode == com.f8scape.automation.StandbyMode.CANDIDATE_TENT) {
                        F8Log.i("AutomationCoordinator", "Dwell timer finished, activating dashboard")
                        launchDashboard()
                    }
                }
            }

            is StandbyStateMachine.Action.CancelDwellTimer -> {
                dwellTimerJob?.cancel()
                dwellTimerJob = null
            }

            is StandbyStateMachine.Action.LaunchCoverDashboard -> {
                dwellTimerJob?.cancel()
                dwellTimerJob = null
                launchDashboard()
            }

            is StandbyStateMachine.Action.DismissCoverDashboard -> {
                dismissDashboard()
            }

            is StandbyStateMachine.Action.ScheduleCooldownTimer -> {
                // Cooldown handled within state machine timestamp checks
            }

            null -> {}
        }
    }

    private fun launchDashboard() {
        F8Log.i("AutomationCoordinator", "Launching cover standby dashboard")

        // 1. If Accessibility Service is running, use it for overlay host
        if (CoverOverlayService.isServiceRunning) {
            CoverOverlayService.activeInstance?.showStandbyOverlay()
            stateMachine.onDashboardLaunched()
            return
        }

        // 2. Otherwise launch DashboardActivity
        try {
            val intent = DashboardActivity.createIntent(context)
            context.startActivity(intent)
            stateMachine.onDashboardLaunched()
        } catch (e: Exception) {
            F8Log.e("AutomationCoordinator", "Failed to launch DashboardActivity", e)
        }
    }

    private fun dismissDashboard() {
        F8Log.i("AutomationCoordinator", "Dismissing cover standby dashboard")
        if (CoverOverlayService.isServiceRunning) {
            CoverOverlayService.activeInstance?.removeStandbyOverlay()
        }
        stateMachine.onDashboardDismissed()
    }
}
