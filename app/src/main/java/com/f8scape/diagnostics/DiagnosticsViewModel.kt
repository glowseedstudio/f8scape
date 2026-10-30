package com.f8scape.diagnostics

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.Display
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.f8scape.automation.StandbyStateMachine
import com.f8scape.display.ScreenStateRepository
import com.f8scape.fold.FoldPosture
import com.f8scape.fold.SensorPostureSource
import com.f8scape.util.F8Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DiagnosticsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val probe = HardwareProbe(application)
    private val sensorSource = SensorPostureSource(application)
    private val screenRepo = ScreenStateRepository(application)
    val stateMachine = StandbyStateMachine()

    private val _reportState = MutableStateFlow<DiagnosticsReport?>(null)
    val reportState: StateFlow<DiagnosticsReport?> = _reportState.asStateFlow()

    private var currentDisplayId: Int = Display.DEFAULT_DISPLAY

    init {
        refreshReport()
        startPostureObservation()
        startDisplayObservation()
    }

    fun updateCurrentDisplayId(displayId: Int) {
        currentDisplayId = displayId
        refreshReport()
    }

    fun refreshReport() {
        val currentPosture = _reportState.value?.currentPosture ?: FoldPosture.UNKNOWN
        val currentStability = _reportState.value?.stabilityScore ?: 0f
        val liveHinge = _reportState.value?.liveHingeAngleDeg
        val isCoverActive = _reportState.value?.isCoverScreenActive ?: true

        val report = probe.generateFullReport(
            currentDisplayId = currentDisplayId,
            currentPosture = currentPosture,
            stabilityScore = currentStability,
            liveHingeAngle = liveHinge,
            isCoverActive = isCoverActive
        )
        _reportState.value = report
    }

    private fun startPostureObservation() {
        viewModelScope.launch {
            sensorSource.observePosture().collect { postureData ->
                stateMachine.onPostureUpdate(postureData)

                val prev = _reportState.value
                if (prev != null) {
                    _reportState.value = prev.copy(
                        currentPosture = postureData.posture,
                        stabilityScore = postureData.stabilityConfidence,
                        liveHingeAngleDeg = postureData.hingeAngleDeg,
                        isCoverScreenActive = postureData.isCoverScreenActive,
                        recentLogs = F8Log.getEntries().takeLast(30).map { it.formatted() }
                    )
                }
            }
        }
    }

    private fun startDisplayObservation() {
        viewModelScope.launch {
            screenRepo.observeDisplayChanges().collect {
                refreshReport()
            }
        }
    }

    fun copyReportToClipboard(): Boolean {
        val report = _reportState.value ?: return false
        val markdown = DiagnosticExporter.formatMarkdown(report)
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("F8scape Diagnostics Report", markdown)
        clipboard?.setPrimaryClip(clip)
        F8Log.i("Diagnostics", "Copied diagnostic report to clipboard (${markdown.length} chars)")
        return true
    }
}
