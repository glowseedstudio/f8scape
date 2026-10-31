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
    fun activeDashboard_briefWobble_absorbsWobbleAndRemainsActive() {
        val tentData = PostureData(
            posture = FoldPosture.TENT_CANDIDATE,
            isCoverScreenActive = true,
            stabilityConfidence = 0.95f,
            hingeAngleDeg = 85f,
            source = "test"
        )
        stateMachine.onPostureUpdate(tentData, currentTimeMs = baseTimestamp)
        stateMachine.onPostureUpdate(tentData, currentTimeMs = baseTimestamp + 1050L)
        stateMachine.onDashboardLaunched()

        assertEquals(StandbyMode.ACTIVE_DASHBOARD, stateMachine.uiState.value.mode)

        // Desk bump / couch cushion wobble (stability drops to 0.25 for 500ms)
        val wobbleData = tentData.copy(stabilityConfidence = 0.25f)
        val wobbleAction = stateMachine.onPostureUpdate(wobbleData, currentTimeMs = baseTimestamp + 2000L)

        // Must absorb the wobble without tearing down the UI
        assertEquals(StandbyMode.ACTIVE_DASHBOARD, stateMachine.uiState.value.mode)
        assertNull(wobbleAction)

        // Device settles back into stable tent posture within debounce window
        val settledData = tentData.copy(stabilityConfidence = 0.92f)
        val settledAction = stateMachine.onPostureUpdate(settledData, currentTimeMs = baseTimestamp + 2500L)

        assertEquals(StandbyMode.ACTIVE_DASHBOARD, stateMachine.uiState.value.mode)
        assertNull(settledAction)

        // Verify it remains active long past the initial debounce window
        val stableAction = stateMachine.onPostureUpdate(settledData, currentTimeMs = baseTimestamp + 4000L)
        assertEquals(StandbyMode.ACTIVE_DASHBOARD, stateMachine.uiState.value.mode)
        assertNull(stableAction)
    }

    @Test
    fun activeDashboard_movementInLandscape_remainsActiveWithoutDebounce() {
        val tentData = PostureData(
            posture = FoldPosture.TENT_CANDIDATE,
            isCoverScreenActive = true,
            stabilityConfidence = 0.95f,
            hingeAngleDeg = 90f,
            isPortrait = false,
            source = "test"
        )
        stateMachine.onPostureUpdate(tentData, currentTimeMs = baseTimestamp)
        stateMachine.onPostureUpdate(tentData, currentTimeMs = baseTimestamp + 1050L)
        stateMachine.onDashboardLaunched()

        assertEquals(StandbyMode.ACTIVE_DASHBOARD, stateMachine.uiState.value.mode)

        // User touches screen or moves phone while keeping it in landscape tent posture (stability drops to 0.0)
        val inMotionLandscapeData = tentData.copy(
            stabilityConfidence = 0.0f,
            isPortrait = false
        )
        val action = stateMachine.onPostureUpdate(inMotionLandscapeData, currentTimeMs = baseTimestamp + 2000L)

        // Remains fully active without triggering exit debounce
        assertEquals(StandbyMode.ACTIVE_DASHBOARD, stateMachine.uiState.value.mode)
        assertNull(action)

        // Continues to remain active over sustained touches in landscape
        val action2 = stateMachine.onPostureUpdate(inMotionLandscapeData, currentTimeMs = baseTimestamp + 4000L)
        assertEquals(StandbyMode.ACTIVE_DASHBOARD, stateMachine.uiState.value.mode)
        assertNull(action2)
    }

    @Test
    fun activeDashboard_turnedToPortrait_exitsAfterDebounce() {
        val tentData = PostureData(
            posture = FoldPosture.TENT_CANDIDATE,
            isCoverScreenActive = true,
            stabilityConfidence = 0.95f,
            hingeAngleDeg = 90f,
            isPortrait = false,
            source = "test"
        )
        stateMachine.onPostureUpdate(tentData, currentTimeMs = baseTimestamp)
        stateMachine.onPostureUpdate(tentData, currentTimeMs = baseTimestamp + 1050L)
        stateMachine.onDashboardLaunched()

        assertEquals(StandbyMode.ACTIVE_DASHBOARD, stateMachine.uiState.value.mode)

        // User turns the phone back up to portrait mode
        val portraitData = tentData.copy(
            isPortrait = true,
            stabilityConfidence = 0.50f
        )
        // First tick starts the snappy debounce timer (300ms)
        val action1 = stateMachine.onPostureUpdate(portraitData, currentTimeMs = baseTimestamp + 2000L)
        assertEquals(StandbyMode.ACTIVE_DASHBOARD, stateMachine.uiState.value.mode)
        assertNull(action1)

        // Halfway through debounce (150ms), still active
        val action2 = stateMachine.onPostureUpdate(portraitData, currentTimeMs = baseTimestamp + 2150L)
        assertEquals(StandbyMode.ACTIVE_DASHBOARD, stateMachine.uiState.value.mode)
        assertNull(action2)

        // User turns it back to landscape before debounce expires -> cancels exit
        val returnedToLandscape = tentData.copy(isPortrait = false)
        val cancelAction = stateMachine.onPostureUpdate(returnedToLandscape, currentTimeMs = baseTimestamp + 2250L)
        assertEquals(StandbyMode.ACTIVE_DASHBOARD, stateMachine.uiState.value.mode)
        assertNull(cancelAction)

        // Now user turns it to portrait and leaves it in portrait beyond 300ms debounce threshold
        stateMachine.onPostureUpdate(portraitData, currentTimeMs = baseTimestamp + 2500L)
        val exitAction = stateMachine.onPostureUpdate(portraitData, currentTimeMs = baseTimestamp + 2850L)
        assertEquals(StandbyMode.EXITING, stateMachine.uiState.value.mode)
        assertEquals(StandbyStateMachine.Action.DismissCoverDashboard, exitAction)
    }

    @Test
    fun activeDashboard_hysteresisAngleTolerance_keepsDashboardActive() {
        val tentData = PostureData(
            posture = FoldPosture.TENT_CANDIDATE,
            isCoverScreenActive = true,
            stabilityConfidence = 0.95f,
            hingeAngleDeg = 90f,
            source = "test"
        )
        stateMachine.onPostureUpdate(tentData, currentTimeMs = baseTimestamp)
        stateMachine.onPostureUpdate(tentData, currentTimeMs = baseTimestamp + 1050L)
        stateMachine.onDashboardLaunched()

        assertEquals(StandbyMode.ACTIVE_DASHBOARD, stateMachine.uiState.value.mode)

        // Angle shifts to 140° (outside strict 45°..135° entry, but inside 35°..145° hysteresis)
        val wideAngleData = tentData.copy(hingeAngleDeg = 140f, stabilityConfidence = 0.50f)
        val action = stateMachine.onPostureUpdate(wideAngleData, currentTimeMs = baseTimestamp + 2000L)

        // Stays active without triggering exit debounce
        assertEquals(StandbyMode.ACTIVE_DASHBOARD, stateMachine.uiState.value.mode)
        assertNull(action)
    }

    @Test
    fun disabledPolicy_transitionsToDisabled() {
        stateMachine.updatePolicy(TriggerPolicy(isAutoStandbyEnabled = false))
        assertEquals(StandbyMode.DISABLED, stateMachine.uiState.value.mode)
    }
}
