package com.f8scape.diagnostics

import com.f8scape.display.DisplaysReport
import com.f8scape.fold.FoldPosture
import com.f8scape.shizuku.ShizukuStatus

data class DeviceHardwareReport(
    val manufacturer: String,
    val model: String,
    val device: String,
    val sdkVersion: Int,
    val androidRelease: String,
    val isStandardFold8Candidate: Boolean,
    val board: String,
    val fingerprint: String
)

data class SensorInfoItem(
    val id: Int,
    val name: String,
    val vendor: String,
    val type: Int,
    val typeString: String,
    val maximumRange: Float,
    val resolution: Float,
    val powerMa: Float
)

data class DiagnosticsReport(
    val hardware: DeviceHardwareReport,
    val displays: DisplaysReport,
    val sensors: List<SensorInfoItem>,
    val currentPosture: FoldPosture,
    val stabilityScore: Float,
    val liveHingeAngleDeg: Float?,
    val isCoverScreenActive: Boolean,
    val isAccessibilityServiceConnected: Boolean,
    val isOverlayPermissionGranted: Boolean,
    val shizukuStatus: ShizukuStatus,
    val recentLogs: List<String>,
    val timestampMs: Long = System.currentTimeMillis()
)
