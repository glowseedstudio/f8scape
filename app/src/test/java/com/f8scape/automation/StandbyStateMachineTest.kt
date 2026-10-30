package com.f8scape.automation

import com.f8scape.fold.FoldPosture
import com.f8scape.fold.PostureData
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class StandbyStateMachineTest {

    private lateinit var stateMachine: StandbyStateMachine
    private val baseTimestamp = 1000000L

    @Before
    fun setUp() {
        stateMachine = StandbyStateMachine(
            TriggerPolicy(
                isAutoStandbyEnabled = true,
                dwellTimeMs = 1000L,
                cooldownDurationMs = 2000L
            )
        )
    }

    @Test
    fun initialState_isMonitoring() {
        assertEquals(StandbyMode.MONITORING, stateMachine.uiState.value.mode)
    }

    @Test
    fun stableTentPosture_triggersCandidateTentAndSchedulesDwell() {
        val tentData = PostureData(
            posture = FoldPosture.TENT_CANDIDATE,
            isCoverScreenActive = true,
            stabilityConfidence = 0.95f,
            source = "test"
        )

        val action = stateMachine.onPostureUpdate(tentData, currentTimeMs = baseTimestamp)

        assertEquals(StandbyMode.CANDIDATE_TENT, stateMachine.uiState.value.mode)
        assertEquals(StandbyStateMachine.Action.ScheduleDwellTimer, action)
    }

    @Test
    fun unstablePostureDuringDwell_abortsBackToMonitoring() {
        val tentData = PostureData(
            posture = FoldPosture.TENT_CANDIDATE,
            isCoverScreenActive = true,
            stabilityConfidence = 0.95f,
            source = "test"
        )
        stateMachine.onPostureUpdate(tentData, currentTimeMs = baseTimestamp)

        // Device shakes/moves (stability drops)
        val movingData = tentData.copy(stabilityConfidence = 0.20f)
        val action = stateMachine.onPostureUpdate(movingData, currentTimeMs = baseTimestamp + 300L)

        assertEquals(StandbyMode.MONITORING, stateMachine.uiState.value.mode)
        assertEquals(StandbyStateMachine.Action.CancelDwellTimer, action)
    }

    @Test
    fun stableTent_heldBeyondDwell_activatesDashboard() {
        val tentData = PostureData(
            posture = FoldPosture.TENT_CANDIDATE,
            isCoverScreenActive = true,
            stabilityConfidence = 0.95f,
            source = "test"
        )
        stateMachine.onPostureUpdate(tentData, currentTimeMs = baseTimestamp)

        // Advance beyond 1000ms dwell threshold
        val action = stateMachine.onPostureUpdate(tentData, currentTimeMs = baseTimestamp + 1050L)

        assertEquals(StandbyMode.ACTIVATING, stateMachine.uiState.value.mode)
        assertEquals(StandbyStateMachine.Action.LaunchCoverDashboard, action)
    }

    @Test
    fun activeDashboard_devicePickedUp_exitsToCooldown() {
        val tentData = PostureData(
            posture = FoldPosture.TENT_CANDIDATE,
            isCoverScreenActive = true,
            stabilityConfidence = 0.95f,
            source = "test"
        )
        stateMachine.onPostureUpdate(tentData, currentTimeMs = baseTimestamp)
        stateMachine.onPostureUpdate(tentData, currentTimeMs = baseTimestamp + 1050L)
        stateMachine.onDashboardLaunched()

        assertEquals(StandbyMode.ACTIVE_DASHBOARD, stateMachine.uiState.value.mode)

        // Device picked up or unfolded flat
        val unfoldedData = tentData.copy(
            posture = FoldPosture.FLAT,
            stabilityConfidence = 0.20f
        )
        val exitAction = stateMachine.onPostureUpdate(unfoldedData, currentTimeMs = baseTimestamp + 2000L)

        assertEquals(StandbyMode.EXITING, stateMachine.uiState.value.mode)
        assertEquals(StandbyStateMachine.Action.DismissCoverDashboard, exitAction)
    }

    @Test
    fun disabledPolicy_transitionsToDisabled() {
        stateMachine.updatePolicy(TriggerPolicy(isAutoStandbyEnabled = false))
        assertEquals(StandbyMode.DISABLED, stateMachine.uiState.value.mode)
    }
}
