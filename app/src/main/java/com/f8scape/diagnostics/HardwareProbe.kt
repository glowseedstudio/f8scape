package com.f8scape.diagnostics

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.provider.Settings
import com.f8scape.display.ScreenStateRepository
import com.f8scape.fold.FoldPosture
import com.f8scape.service.CoverOverlayService
import com.f8scape.shizuku.SafeShizukuBridge
import com.f8scape.util.F8Log

class HardwareProbe(
    private val context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val screenStateRepository = ScreenStateRepository(context)
    private val shizukuBridge = SafeShizukuBridge(context)

    fun probeDevice(): DeviceHardwareReport {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val device = Build.DEVICE
        val sdkVersion = Build.VERSION.SDK_INT
        val release = Build.VERSION.RELEASE

        // Standard Fold 8 checks (Samsung, foldable lineage, passport form factor)
        // Standard Fold 8 is distinct from Fold 8 Ultra
        val isSamsung = manufacturer.equals("samsung", ignoreCase = true)
        val isNotUltra = !model.contains("Ultra", ignoreCase = true) && !device.contains("ultra", ignoreCase = true)
        val isFoldCandidate = isSamsung && (model.startsWith("SM-F") || device.contains("fold", ignoreCase = true))

        return DeviceHardwareReport(
            manufacturer = manufacturer,
            model = model,
            device = device,
            sdkVersion = sdkVersion,
            androidRelease = release,
            isStandardFold8Candidate = isFoldCandidate && isNotUltra,
            board = Build.BOARD,
            fingerprint = Build.FINGERPRINT
        )
    }

    fun probeSensors(): List<SensorInfoItem> {
        val sensors = sensorManager?.getSensorList(Sensor.TYPE_ALL) ?: emptyList()
        return sensors.map { sensor ->
            SensorInfoItem(
                id = sensor.id,
                name = sensor.name,
                vendor = sensor.vendor,
                type = sensor.type,
                typeString = sensor.stringType ?: "type_${sensor.type}",
                maximumRange = sensor.maximumRange,
                resolution = sensor.resolution,
                powerMa = sensor.power
            )
        }.sortedBy { it.name }
    }

    fun generateFullReport(
        currentDisplayId: Int,
        currentPosture: FoldPosture,
        stabilityScore: Float,
        liveHingeAngle: Float?,
        isCoverActive: Boolean
    ): DiagnosticsReport {
        val hw = probeDevice()
        val displays = screenStateRepository.getDisplaysReport(currentDisplayId)
        val sensors = probeSensors()
        val shizuku = shizukuBridge.checkStatus()
        val isA11yRunning = CoverOverlayService.isServiceRunning
        val canOverlay = Settings.canDrawOverlays(context)
        val logs = F8Log.getEntries().takeLast(30).map { it.formatted() }

        return DiagnosticsReport(
            hardware = hw,
            displays = displays,
            sensors = sensors,
            currentPosture = currentPosture,
            stabilityScore = stabilityScore,
            liveHingeAngleDeg = liveHingeAngle,
            isCoverScreenActive = isCoverActive,
            isAccessibilityServiceConnected = isA11yRunning,
            isOverlayPermissionGranted = canOverlay,
            shizukuStatus = shizuku,
            recentLogs = logs
        )
    }
}
